package com.cheatlearn.bgmiloader;

import android.util.Log;
import com.cheatlearn.bgmiloader.utils.NativeReader;
import com.cheatlearn.bgmiloader.utils.OffsetManager;
import com.cheatlearn.bgmiloader.utils.Vector3;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class EntityReader {

    private static final String TAG = "EntityReader";
    private static final int MAX_ENTITIES = 200;

    private final MemoryReader mem;
    private final OffsetManager offs;
    private long ue4Base;
    private long uworld;
    private int myTeamId = -1;
    private int screenW = 1080;
    private int screenH = 1920;
    private long localPawnAddr;

    public EntityReader(MemoryReader mem, OffsetManager offs) {
        this.mem = mem;
        this.offs = offs;
    }

    public void setScreenSize(int w, int h) {
        this.screenW = w;
        this.screenH = h;
    }

    // --------------------- INIT ---------------------
    public boolean init() {
        if (!mem.init()) return false;
        ue4Base = mem.getUe4Base();

        long uworldOffset = offs.get("UWorld");
        if (uworldOffset == 0) {
            Log.e(TAG, "UWorld offset is 0");
            return false;
        }

        long currentLevelOff = offs.get("CurrentLevel");
        if (currentLevelOff == 0) currentLevelOff = 0x30;

        uworld = mem.readPtr(ue4Base + uworldOffset);
        Log.i(TAG, "UWorld attempt1 (base+offset): 0x" + Long.toHexString(uworld));

        if (uworld > 0x10000) {
            long level = mem.readPtr(uworld + currentLevelOff);
            if (level > 0x10000) {
                Log.i(TAG, "UWorld verified via PersistentLevel");
                return true;
            }
        }

        uworld = uworldOffset;
        long level = mem.readPtr(uworld + currentLevelOff);
        if (level > 0x10000) {
            Log.i(TAG, "UWorld attempt2 (absolute): 0x" + Long.toHexString(uworld));
            return true;
        }

        uworld = mem.readPtr(mem.readPtr(ue4Base + uworldOffset));
        level = mem.readPtr(uworld + currentLevelOff);
        if (level > 0x10000) {
            Log.i(TAG, "UWorld attempt3 (double deref): 0x" + Long.toHexString(uworld));
            return true;
        }

        Log.e(TAG, "Could not find valid UWorld");
        uworld = 0;
        return false;
    }

    // --------------------- READ CAMERA ---------------------
    public float[] readViewMatrix() {
        try {
            long gameInstance = mem.readPtr(uworld + offs.get("GameInstance"));
            if (gameInstance == 0) return null;

            long localPlayers = mem.readPtr(gameInstance + offs.get("LocalPlayers"));
            if (localPlayers == 0) return null;

            long playerController = mem.readPtr(localPlayers + offs.get("PlayerController"));
            if (playerController == 0) return null;

            long cameraMgr = mem.readPtr(playerController + offs.get("PlayerCameraManager"));
            if (cameraMgr == 0) return null;

            long cameraCache = cameraMgr + offs.get("CameraCache");
            long vpMatrixAddr = cameraCache + offs.get("ViewProjectionMatrix");
            return mem.readMatrix(vpMatrixAddr, 16);
        } catch (Exception e) {
            Log.e(TAG, "readViewMatrix failed", e);
            return null;
        }
    }

    // --------------------- READ LOCAL PLAYER INFO ---------------------
    public Vector3 readLocalPos() {
        if (localPawnAddr == 0) return new Vector3(0, 0, 0);
        long root = mem.readPtr(localPawnAddr + offs.get("RootComponent"));
        if (root == 0) return new Vector3(0, 0, 0);
        return mem.readVector3(root + offs.get("RelativeLocation"));
    }

    public int getMyTeamId() { return myTeamId; }

    // --------------------- SCAN ENTITIES ---------------------
    public ArrayList<EntityData> scanEntities() {
        ArrayList<EntityData> result = new ArrayList<>();
        if (uworld == 0) return result;

        try {
            long persistentLevel = mem.readPtr(uworld + offs.get("CurrentLevel"));
            if (persistentLevel == 0) return result;

            long gameInstance = mem.readPtr(uworld + offs.get("GameInstance"));
            if (gameInstance == 0) return result;

            long localPlayers = mem.readPtr(gameInstance + offs.get("LocalPlayers"));
            if (localPlayers == 0) return result;

            long playerController = mem.readPtr(localPlayers + offs.get("PlayerController"));
            if (playerController == 0) return result;

            localPawnAddr = mem.readPtr(playerController + offs.get("AcknowledgedPawn"));

            long cameraMgr = mem.readPtr(playerController + offs.get("PlayerCameraManager"));

            long entityArray = mem.readPtr(persistentLevel + offs.get("EntityList"));
            int entityCount = mem.readInt(persistentLevel + offs.get("EntityListSize"));

            if (entityCount <= 0 || entityCount > MAX_ENTITIES) entityCount = MAX_ENTITIES;
            if (entityArray == 0) return result;

            byte[] ptrBuf = new byte[entityCount * 8];
            int read = mem.readBulk(entityArray, ptrBuf);
            if (read != ptrBuf.length) return result;

            ByteBuffer bb = ByteBuffer.wrap(ptrBuf);
            bb.order(ByteOrder.LITTLE_ENDIAN);

            Vector3 localPos = readLocalPos();

            for (int i = 0; i < entityCount; i++) {
                long actorPtr = bb.getLong();
                if (actorPtr == 0 || actorPtr == localPawnAddr) continue;

                long mesh = mem.readPtr(actorPtr + offs.get("Mesh"));
                if (mesh == 0) continue;

                int health = mem.readInt(actorPtr + offs.get("Health"));
                if (health <= 0 || health > 1000) continue;

                long root = mem.readPtr(actorPtr + offs.get("RootComponent"));
                if (root == 0) continue;

                Vector3 worldPos = mem.readVector3(root + offs.get("RelativeLocation"));

                long playerState = mem.readPtr(actorPtr + offs.get("PlayerState"));
                if (playerState == 0) continue;

                String name = mem.readStr(playerState + offs.get("PlayerName"), 32);
                int teamId = mem.readInt(playerState + offs.get("TeamID"));

                if (actorPtr == localPawnAddr) {
                    myTeamId = teamId;
                    continue;
                }

                EntityData e = new EntityData();
                e.address = actorPtr;
                e.position = worldPos;
                e.health = health;
                e.maxHealth = 100;
                e.teamId = teamId;
                e.name = name;
                e.isAlive = health > 0;
                e.isEnemy = teamId != myTeamId;

                if (localPos != null) {
                    float dx = worldPos.x - localPos.x;
                    float dy = worldPos.y - localPos.y;
                    float dz = worldPos.z - localPos.z;
                    e.distance = (float) Math.sqrt(dx*dx + dy*dy + dz*dz) / 100f;
                }

                result.add(e);
            }
        } catch (Exception e) {
            Log.e(TAG, "scanEntities error", e);
        }

        return result;
    }
}

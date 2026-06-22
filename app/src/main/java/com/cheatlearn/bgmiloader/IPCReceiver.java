package com.cheatlearn.bgmiloader;

import android.util.Log;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class IPCReceiver implements Runnable {
    private static final String TAG = "IPCReceiver";
    private static final int PORT = 9876;
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(PORT);
            Log.i(TAG, "IPC server on port " + PORT);

            while (running) {
                try {
                    Socket client = serverSocket.accept();
                    client.setSoTimeout(2000);
                    handleClient(client);
                } catch (Exception e) {
                    if (running) Log.w(TAG, "Accept error", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Server error", e);
        }
    }

    private void handleClient(Socket client) {
        try {
            DataInputStream dis = new DataInputStream(client.getInputStream());

            int count = dis.readInt();
            if (count <= 0 || count > 200) { dis.close(); return; }

            // Read view matrix (16 floats)
            float[] viewMatrix = new float[16];
            for (int i = 0; i < 16; i++) viewMatrix[i] = dis.readFloat();

            // Store matrix globally
            ViewMatrixStore.set(viewMatrix);

            // Read entities
            SharedEntityBuffer.clear();
            for (int i = 0; i < count; i++) {
                EntityData e = new EntityData();
                e.health = dis.readInt();
                e.maxHealth = dis.readInt();
                e.teamId = dis.readInt();
                e.isAlive = dis.readByte() != 0;
                e.isEnemy = dis.readByte() != 0;
                e.distance = dis.readFloat();
                e.position.x = dis.readFloat();
                e.position.y = dis.readFloat();
                e.position.z = dis.readFloat();
                int nameLen = dis.readInt();
                if (nameLen < 0 || nameLen > 4096) {
                    Log.w(TAG, "Invalid name length: " + nameLen);
                    return;
                }
                int readLen = Math.min(nameLen, 64);
                byte[] nameBytes = new byte[readLen];
                dis.readFully(nameBytes);
                skipFully(dis, nameLen - readLen);
                e.name = new String(nameBytes, StandardCharsets.UTF_8);
                SharedEntityBuffer.push(e);
            }
            dis.close();
        } catch (Exception e) {
            Log.w(TAG, "Client error", e);
        } finally {
            try { client.close(); } catch (Exception ignored) {}
        }
    }

    public void stop() {
        running = false;
        try { serverSocket.close(); } catch (Exception ignored) {}
    }

    private static void skipFully(DataInputStream dis, int bytes) throws IOException {
        while (bytes > 0) {
            int skipped = dis.skipBytes(bytes);
            if (skipped <= 0) throw new IOException("Failed to skip " + bytes + " bytes");
            bytes -= skipped;
        }
    }
}

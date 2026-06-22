package com.cheatlearn.bgmiloader;

import android.util.Log;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class IPCSender {
    private static final String TAG = "IPCSender";
    private static final int PORT = 9876;

    public static void sendEntities(ArrayList<EntityData> entities, float[] viewMatrix) {
        if (entities == null) return;

        try {
            Socket socket = new Socket("127.0.0.1", PORT);
            socket.setSoTimeout(2000);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            int count = Math.min(entities.size(), 200);
            dos.writeInt(count);

            if (viewMatrix != null && viewMatrix.length == 16) {
                for (int i = 0; i < 16; i++) dos.writeFloat(viewMatrix[i]);
            } else {
                for (int i = 0; i < 16; i++) dos.writeFloat(0f);
            }

            for (int i = 0; i < count; i++) {
                EntityData e = entities.get(i);
                dos.writeInt(e.health);
                dos.writeInt(e.maxHealth);
                dos.writeInt(e.teamId);
                dos.writeByte(e.isAlive ? 1 : 0);
                dos.writeByte(e.isEnemy ? 1 : 0);
                dos.writeFloat(e.distance);
                dos.writeFloat(e.position.x);
                dos.writeFloat(e.position.y);
                dos.writeFloat(e.position.z);
                byte[] nameBytes = e.name.getBytes("UTF-8");
                dos.writeInt(nameBytes.length);
                dos.write(nameBytes);
            }

            dos.flush();
            dos.close();
            socket.close();
        } catch (java.net.ConnectException e) {
        } catch (Exception e) {
            Log.w(TAG, "Send error", e);
        }
    }
}

package com.cheatlearn.bgmiloader;

import com.cheatlearn.bgmiloader.utils.NativeReader;
import com.cheatlearn.bgmiloader.utils.Vector3;

public class MemoryReader {
    private long ue4Base;
    private long uworldAddr;

    public boolean init() {
        int pid = NativeReader.getMyPid();
        NativeReader.attachProcess(pid);
        ue4Base = NativeReader.getModuleBase(pid, "libUE4.so");
        if (ue4Base == 0) {
            android.util.Log.e("MemReader", "libUE4.so not found");
            return false;
        }
        android.util.Log.i("MemReader", "libUE4 base: 0x" + Long.toHexString(ue4Base));
        return true;
    }

    public long readPtr(long addr) {
        return NativeReader.readLong(addr);
    }

    public int readInt(long addr) {
        return NativeReader.readInt(addr);
    }

    public float readFloat(long addr) {
        return NativeReader.readFloat(addr);
    }

    public String readStr(long addr, int max) {
        return NativeReader.readString(addr, max);
    }

    public float readVec3Component(long addr) {
        return NativeReader.readFloat(addr);
    }

    public Vector3 readVector3(long addr) {
        return new Vector3(
            NativeReader.readFloat(addr),
            NativeReader.readFloat(addr + 4),
            NativeReader.readFloat(addr + 8)
        );
    }

    public float[] readMatrix(long addr, int count) {
        float[] m = new float[count];
        byte[] buf = new byte[count * 4];
        if (NativeReader.readBuffer(addr, buf) == count * 4) {
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(buf);
            bb.order(java.nio.ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < count; i++) {
                m[i] = bb.getFloat();
            }
        }
        return m;
    }

    public boolean readVector3Bulk(long addr, Vector3 out) {
        byte[] buf = new byte[12];
        if (NativeReader.readBuffer(addr, buf) == 12) {
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(buf);
            bb.order(java.nio.ByteOrder.LITTLE_ENDIAN);
            out.x = bb.getFloat();
            out.y = bb.getFloat();
            out.z = bb.getFloat();
            return true;
        }
        return false;
    }

    public int readBulk(long addr, byte[] buf) {
        return NativeReader.readBuffer(addr, buf);
    }

    public long getUe4Base() { return ue4Base; }
    public long getUworld() { return uworldAddr; }
    public void setUworld(long addr) { this.uworldAddr = addr; }
}

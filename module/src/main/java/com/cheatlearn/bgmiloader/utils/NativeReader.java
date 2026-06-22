package com.cheatlearn.bgmiloader.utils;

public final class NativeReader {

    static {
        System.loadLibrary("bgmi_native");
    }

    public static native boolean attachProcess(int pid);
    public static native int getMyPid();
    public static native long readLong(long addr);
    public static native int readInt(long addr);
    public static native float readFloat(long addr);
    public static native int readBuffer(long addr, byte[] buffer);
    public static native String readString(long addr, int maxLen);
    public static native long getModuleBase(int pid, String moduleName);

    private NativeReader() {}
}

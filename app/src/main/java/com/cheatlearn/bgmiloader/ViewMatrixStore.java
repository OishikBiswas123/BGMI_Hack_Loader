package com.cheatlearn.bgmiloader;

public class ViewMatrixStore {
    private static float[] matrix;

    public static synchronized void set(float[] m) {
        matrix = m;
    }

    public static synchronized float[] get() {
        return matrix;
    }
}

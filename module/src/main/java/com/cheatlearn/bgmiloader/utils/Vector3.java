package com.cheatlearn.bgmiloader.utils;

public final class Vector3 {
    public float x, y, z;

    public Vector3() { this.x = 0; this.y = 0; this.z = 0; }
    public Vector3(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }

    public float distance(Vector3 other) {
        float dx = this.x - other.x;
        float dy = this.y - other.y;
        float dz = this.z - other.z;
        return (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
    }
}

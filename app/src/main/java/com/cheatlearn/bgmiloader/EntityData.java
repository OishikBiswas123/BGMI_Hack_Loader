package com.cheatlearn.bgmiloader;

import com.cheatlearn.bgmiloader.utils.Vector3;

public class EntityData {
    public long address;
    public Vector3 position;
    public Vector3 headPosition;
    public String name;
    public int health;
    public int maxHealth;
    public int teamId;
    public boolean isAlive;
    public boolean isEnemy;
    public float distance;

    public EntityData() {
        this.position = new Vector3();
        this.headPosition = new Vector3();
        this.name = "";
        this.health = 0;
        this.maxHealth = 100;
        this.teamId = -1;
        this.isAlive = false;
        this.isEnemy = false;
        this.distance = 0f;
    }
}

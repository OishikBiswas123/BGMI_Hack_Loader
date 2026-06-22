package com.cheatlearn.bgmiloader;

import java.util.concurrent.ConcurrentLinkedQueue;

public class SharedEntityBuffer {
    private static final ConcurrentLinkedQueue<EntityData> entities = new ConcurrentLinkedQueue<>();

    public static void push(EntityData e) {
        entities.offer(e);
    }

    public static EntityData[] drainAll() {
        Object[] arr = entities.toArray();
        entities.clear();
        if (arr.length == 0) return new EntityData[0];
        EntityData[] result = new EntityData[arr.length];
        for (int i = 0; i < arr.length; i++) result[i] = (EntityData) arr[i];
        return result;
    }

    public static int count() {
        return entities.size();
    }

    public static void clear() {
        entities.clear();
    }
}

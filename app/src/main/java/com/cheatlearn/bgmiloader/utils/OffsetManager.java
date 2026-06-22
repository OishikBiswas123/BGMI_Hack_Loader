package com.cheatlearn.bgmiloader.utils;

import android.content.Context;
import android.util.Log;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.InputStream;

public final class OffsetManager {

    private static final String TAG = "Offsets";
    private static OffsetManager instance;
    private final JSONObject offsets;

    private OffsetManager(Context ctx) {
        JSONObject loaded = new JSONObject();
        try {
            InputStream is = ctx.getAssets().open("offsets.json");
            byte[] buf = new byte[is.available()];
            is.read(buf);
            is.close();
            String json = new String(buf, "UTF-8");
            loaded = new JSONObject(new JSONTokener(json));
            Log.i(TAG, "Offsets loaded: " + loaded.length() + " keys");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load offsets.json", e);
        }
        this.offsets = loaded;
    }

    public static synchronized OffsetManager getInstance(Context ctx) {
        if (instance == null) instance = new OffsetManager(ctx.getApplicationContext());
        return instance;
    }

    public long get(String key) {
        try {
            if (offsets.has(key)) {
                String val = offsets.getString(key);
                if (val.startsWith("0x") || val.startsWith("0X")) {
                    return Long.parseLong(val.substring(2), 16);
                }
                return Long.parseLong(val);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public long getDefault(String key, long defaultVal) {
        long v = get(key);
        return v != 0 ? v : defaultVal;
    }
}

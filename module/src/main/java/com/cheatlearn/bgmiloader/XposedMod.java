package com.cheatlearn.bgmiloader;

import android.util.Log;
import com.cheatlearn.bgmiloader.utils.NativeReader;
import com.cheatlearn.bgmiloader.utils.OffsetManager;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.util.ArrayList;

public class XposedMod implements IXposedHookLoadPackage {

    private static final String TAG = "XposedMod";
    private OffsetManager offsetManager;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("com.pubg.imobile")) return;

        Log.i(TAG, "BGMI process detected!");
        XposedBridge.log("BGMI Loader: hooked into " + lpparam.packageName);

        hookGameActivity(lpparam.classLoader);
    }

    private void hookGameActivity(ClassLoader cl) {
        try {
            Class<?> gameActivity = cl.loadClass("com.epicgames.ue4.GameActivity");
            XposedHelpers.findAndHookMethod(gameActivity, "onCreate", android.os.Bundle.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Log.i(TAG, "GameActivity.onCreate - UE4 loaded");
                        android.content.Context ctx = (android.content.Context) param.thisObject;
                        offsetManager = OffsetManager.getInstance(ctx);
                        startReader();
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Failed to hook GameActivity", e);
        }
    }

    private void startReader() {
        new Thread(() -> {
            Log.i(TAG, "Initializing memory reader...");

            MemoryReader reader = new MemoryReader();
            if (!reader.init()) {
                Log.e(TAG, "Failed to init MemoryReader, retrying...");
                for (int i = 0; i < 30; i++) {
                    try { Thread.sleep(1000); } catch (InterruptedException e) { return; }
                    reader = new MemoryReader();
                    if (reader.init()) break;
                }
                if (reader.getUe4Base() == 0) {
                    Log.e(TAG, "Could not find libUE4.so after 30s");
                    return;
                }
            }

            EntityReader entityReader = new EntityReader(reader, offsetManager);
            if (!entityReader.init()) {
                Log.e(TAG, "EntityReader init failed");
                return;
            }

            Log.i(TAG, "Reader initialized! Starting scan loop.");

            while (true) {
                try {
                    float[] viewMatrix = entityReader.readViewMatrix();
                    ArrayList<EntityData> entities = entityReader.scanEntities();
                    IPCSender.sendEntities(entities, viewMatrix);
                    Thread.sleep(100);
                } catch (Exception e) {
                    Log.e(TAG, "Scan loop error", e);
                    try { Thread.sleep(1000); } catch (InterruptedException ie) { break; }
                }
            }
        }, "BgmiReader").start();
    }
}

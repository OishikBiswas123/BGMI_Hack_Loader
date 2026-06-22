package com.cheatlearn.bgmiloader;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import org.lsposed.patch.LSPatch;
import org.lsposed.patch.util.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PatcherHelper {

    private static final String TAG = "PatcherHelper";
    private static final String[] BGMI_PACKAGES = {
        "com.pubg.imobile",
        "com.tencent.ig",
        "com.pubg.krmobile",
        "com.vng.pubgmobile",
        "com.rekoo.pubgm"
    };
    private static final String MODULE_ASSET = "bgmi_module.apk";
    private static final String ORIGINAL_APK_NAME = "bgmi_original.apk";

    public interface PatchCallback {
        void onProgress(String message);
        void onSuccess(File patchedApk);
        void onError(String error);
    }

    public static void patchBgmi(Context context, PatchCallback callback) {
        new Thread(() -> {
            try {
                callback.onProgress("Finding BGMI installation...");
                var pm = context.getPackageManager();

                String bgmiPackage = null;
                File srcApk = null;
                for (String pkg : BGMI_PACKAGES) {
                    try {
                        var info = pm.getPackageInfo(pkg, 0);
                        File apk = new File(info.applicationInfo.sourceDir);
                        if (apk.exists()) {
                            bgmiPackage = pkg;
                            srcApk = apk;
                            break;
                        }
                    } catch (PackageManager.NameNotFoundException ignored) {}
                }

                if (srcApk == null) {
                    callback.onError("BGMI not found. Install BGMI or PUBG Mobile first.");
                    return;
                }
                callback.onProgress("Found " + bgmiPackage + ": " + srcApk.length() / 1048576 + " MB");

                File cacheDir = context.getCacheDir();

                File bgmiCopy = new File(cacheDir, ORIGINAL_APK_NAME);
                callback.onProgress("Copying BGMI APK...");
                copyFile(srcApk, bgmiCopy);

                File moduleFile = new File(cacheDir, MODULE_ASSET);
                callback.onProgress("Extracting module...");
                try (InputStream is = context.getAssets().open(MODULE_ASSET);
                     FileOutputStream os = new FileOutputStream(moduleFile)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = is.read(buf)) > 0) os.write(buf, 0, n);
                }

                File keystoreFile = new File(cacheDir, "runtime_keystore.p12");
                callback.onProgress("Generating signing keystore on device...");
                KeystoreGenerator.generate(keystoreFile, "123456", "key0");

                File outputDir = new File(cacheDir, "patched");
                if (!outputDir.exists() && !outputDir.mkdirs()) {
                    callback.onError("Failed to create output directory");
                    return;
                }
                File[] oldOutputs = outputDir.listFiles();
                if (oldOutputs != null) {
                    for (File f : oldOutputs) {
                        if (!f.delete()) Log.w(TAG, "Failed to delete old output: " + f.getAbsolutePath());
                    }
                }

                String[] args = {
                    bgmiCopy.getAbsolutePath(),
                    "-o", outputDir.getAbsolutePath(),
                    "-m", moduleFile.getAbsolutePath(),
                    "-k", keystoreFile.getAbsolutePath(),
                    "123456", "key0", "123456",
                    "-f",
                    "-l", "2",
                    "-d"
                };

                callback.onProgress("Patching BGMI with module...");
                Log.i(TAG, "LSPatch args: " + String.join(" ", args));

                LSPatch lsp = new LSPatch(new Logger() {
                    @Override public void i(String msg) { Log.i(TAG, msg); if (msg != null && msg.length() > 0) callback.onProgress(msg); }
                    @Override public void d(String msg) { Log.d(TAG, msg); }
                    @Override public void e(String msg) { Log.e(TAG, msg); callback.onProgress("ERROR: " + msg); }
                }, args);
                lsp.doCommandLine();

                File[] patchedFiles = outputDir.listFiles((dir, name) -> name.endsWith("-lspatched.apk"));
                if (patchedFiles == null || patchedFiles.length == 0) {
                    callback.onError("No patched APK found in output directory");
                    return;
                }

                File patched = patchedFiles[0];
                callback.onProgress("Patch complete: " + patched.getName() + " (" + patched.length() / 1048576 + " MB)");
                callback.onSuccess(patched);

            } catch (Throwable t) {
                Log.e(TAG, "Patching failed", t);
                StringBuilder sb = new StringBuilder("Patching failed: ");
                sb.append(t.getMessage());
                Throwable cause = t.getCause();
                int depth = 0;
                while (cause != null && depth < 3) {
                    sb.append(" \u2192 ").append(cause.getMessage());
                    cause = cause.getCause();
                    depth++;
                }
                callback.onError(sb.toString());
            }
        }).start();
    }

    private static void copyFile(File src, File dst) throws IOException {
        try (InputStream is = new FileInputStream(src);
             FileOutputStream os = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) os.write(buf, 0, n);
        }
    }
}

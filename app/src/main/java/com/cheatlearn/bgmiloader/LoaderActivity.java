package com.cheatlearn.bgmiloader;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.ContextCompat;

import java.io.File;

public class LoaderActivity extends AppCompatActivity {

    private TextView tvStatus;
    private Button btnPatch, btnLaunch, btnCheckKeystore, btnStartOverlay, btnStopOverlay;
    private File patchedApk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loader);

        tvStatus = findViewById(R.id.tvStatus);
        btnPatch = findViewById(R.id.btnPatch);
        btnLaunch = findViewById(R.id.btnLaunch);
        btnCheckKeystore = findViewById(R.id.btnCheckKeystore);
        btnStartOverlay = findViewById(R.id.btnStartOverlay);
        btnStopOverlay = findViewById(R.id.btnStopOverlay);

        btnPatch.setOnClickListener(v -> patchAndInstall());
        btnLaunch.setOnClickListener(v -> launchPatchedBgmi());
        btnCheckKeystore.setOnClickListener(v -> checkKeystore());
        btnStartOverlay.setOnClickListener(v -> startOverlay());
        btnStopOverlay.setOnClickListener(v -> stopOverlay());

        updateButtonStates();

        checkOverlayPermission();
        checkInstallPermission();
        checkNotificationPermission();
    }

    private void updateButtonStates() {
        btnLaunch.setEnabled(patchedApk != null && patchedApk.exists());
    }

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                new AlertDialog.Builder(this)
                    .setTitle("Overlay Permission Required")
                    .setMessage("Please grant overlay permission for ESP rendering.")
                    .setPositiveButton("Grant", (d, w) -> {
                        Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                        startActivity(i);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            }
        }
    }

    private void checkInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                new AlertDialog.Builder(this)
                    .setTitle("Install Permission Required")
                    .setMessage("Please allow app installation to install patched BGMI.")
                    .setPositiveButton("Grant", (d, w) -> {
                        Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:" + getPackageName()));
                        startActivity(i);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            }
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[] { Manifest.permission.POST_NOTIFICATIONS },
                1001);
        }
    }

    private void patchAndInstall() {
        tvStatus.setText("Status: patching BGMI...");
        btnPatch.setEnabled(false);

        PatcherHelper.patchBgmi(this, new PatcherHelper.PatchCallback() {
            @Override
            public void onProgress(String message) {
                runOnUiThread(() -> tvStatus.setText("Status: " + message));
            }

            @Override
            public void onSuccess(File apk) {
                runOnUiThread(() -> {
                    patchedApk = apk;
                    tvStatus.setText("Status: patch complete! (" + apk.length() / 1048576 + " MB)");
                    updateButtonStates();
                    installPatchedApk();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    tvStatus.setText("Status: " + error);
                    btnPatch.setEnabled(true);
                    Toast.makeText(LoaderActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void installPatchedApk() {
        if (patchedApk == null || !patchedApk.exists()) return;

        try {
            Uri apkUri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", patchedApk);

            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            intent.setData(apkUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            tvStatus.setText("Status: installer failed: " + e.getMessage());
            Toast.makeText(this, "Installer failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            btnPatch.setEnabled(true);
        }
    }

    private void launchPatchedBgmi() {
        if (patchedApk == null || !patchedApk.exists()) {
            Toast.makeText(this, "Patch BGMI first", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.pubg.imobile");
            if (launchIntent != null) {
                startActivity(launchIntent);
            } else {
                Toast.makeText(this, "Patched BGMI not installed", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to launch: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkKeystore() {
        tvStatus.setText("Status: checking keystore...");
        new Thread(() -> {
            String result = KeystoreDiagnostics.run(LoaderActivity.this);
            runOnUiThread(() -> {
                tvStatus.setText(result.replace("\n", " | "));
                Toast.makeText(LoaderActivity.this, "Check complete, see status", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void startOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            tvStatus.setText("Status: overlay permission missing");
            return;
        }
        Intent intent = new Intent(this, OverlayService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        } catch (Exception e) {
            tvStatus.setText("Status: overlay failed: " + e.getMessage());
            Toast.makeText(this, "Overlay failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        tvStatus.setText("Status: overlay running");
    }

    private void stopOverlay() {
        Intent intent = new Intent(this, OverlayService.class);
        stopService(intent);
        tvStatus.setText("Status: idle");
    }
}

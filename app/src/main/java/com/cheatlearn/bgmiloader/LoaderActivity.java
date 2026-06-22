package com.cheatlearn.bgmiloader;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class LoaderActivity extends AppCompatActivity {

    private TextView tvStatus;
    private boolean overlayRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loader);

        tvStatus = findViewById(R.id.tvStatus);
        Button btnStart = findViewById(R.id.btnStartOverlay);
        Button btnStop  = findViewById(R.id.btnStopOverlay);

        btnStart.setOnClickListener(v -> startOverlay());
        btnStop.setOnClickListener(v -> stopOverlay());

        checkOverlayPermission();
    }

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                new AlertDialog.Builder(this)
                    .setTitle("Overlay Permission Required")
                    .setMessage("Please grant overlay permission for ESP rendering.")
                    .setPositiveButton("Grant", (d, w) -> {
                        Intent i = new Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:" + getPackageName())
                        );
                        startActivity(i);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            }
        }
    }

    private void startOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            tvStatus.setText("Status: overlay permission missing");
            return;
        }
        Intent intent = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        overlayRunning = true;
        tvStatus.setText("Status: overlay running");
    }

    private void stopOverlay() {
        Intent intent = new Intent(this, OverlayService.class);
        stopService(intent);
        overlayRunning = false;
        tvStatus.setText("Status: idle");
    }
}

package com.cheatlearn.bgmiloader;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

public class OverlayService extends Service {

    private static final int NOTIFY_ID = 1;
    private WindowManager windowManager;
    private FrameLayout overlayContainer;
    private ESPRenderer espRenderer;
    private IPCReceiver ipcReceiver;
    private Thread ipcThread;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel ch = new NotificationChannel(
                    "bgmi_overlay", "BGMI Overlay",
                    NotificationManager.IMPORTANCE_LOW
                );
                ch.setDescription("Tap to open BGMI Loader and stop overlay");
                NotificationManager nm = getSystemService(NotificationManager.class);
                if (nm != null) nm.createNotificationChannel(ch);

                Intent tapIntent = new Intent(this, LoaderActivity.class);
                PendingIntent pi = PendingIntent.getActivity(this, 0, tapIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                Intent stopIntent = new Intent(this, OverlayService.class);
                stopIntent.setAction("STOP_OVERLAY");
                PendingIntent stopPi = PendingIntent.getService(this, 1, stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                Notification n = new Notification.Builder(this, "bgmi_overlay")
                    .setContentTitle("Harry Chutiya Overlay")
                    .setContentText("Running - tap to manage")
                    .setSmallIcon(android.R.drawable.ic_menu_manage)
                    .setContentIntent(pi)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPi)
                    .setOngoing(true)
                    .build();
                startForeground(NOTIFY_ID, n);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Overlay notification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }

        windowManager = getSystemService(WindowManager.class);
        if (windowManager == null) {
            Toast.makeText(this, "Window service unavailable", Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }

        Point size = new Point();
        windowManager.getDefaultDisplay().getSize(size);

        overlayContainer = new FrameLayout(this);
        overlayContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        espRenderer = new ESPRenderer(this);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 0;

        overlayContainer.addView(espRenderer, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        try {
            windowManager.addView(overlayContainer, params);
        } catch (Exception e) {
            Toast.makeText(this, "Overlay failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }

        ipcReceiver = new IPCReceiver();
        ipcThread = new Thread(ipcReceiver, "IPCReceiver");
        ipcThread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP_OVERLAY".equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (ipcReceiver != null) ipcReceiver.stop();
        if (espRenderer != null) espRenderer.stop();
        if (overlayContainer != null && windowManager != null) {
            try { windowManager.removeView(overlayContainer); } catch (Exception ignored) {}
        }
        super.onDestroy();
    }
}

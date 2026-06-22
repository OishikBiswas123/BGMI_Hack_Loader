package com.cheatlearn.bgmiloader;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class OverlayService extends Service {

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                "bgmi_overlay", "BGMI Overlay",
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(ch);
            Notification n = new Notification.Builder(this, "bgmi_overlay")
                .setContentTitle("BGMI Loader")
                .setContentText("Overlay is running")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .build();
            startForeground(1, n);
        }

        windowManager = getSystemService(WindowManager.class);

        Point size = new Point();
        windowManager.getDefaultDisplay().getSize(size);

        overlayContainer = new FrameLayout(this);
        espRenderer = new ESPRenderer(this);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 0;

        overlayContainer.addView(espRenderer, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        windowManager.addView(overlayContainer, params);

        ipcReceiver = new IPCReceiver();
        ipcThread = new Thread(ipcReceiver, "IPCReceiver");
        ipcThread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (ipcReceiver != null) ipcReceiver.stop();
        if (espRenderer != null) espRenderer.stop();
        if (overlayContainer != null && windowManager != null) {
            windowManager.removeView(overlayContainer);
        }
    }
}

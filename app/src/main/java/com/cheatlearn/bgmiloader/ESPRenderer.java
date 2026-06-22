package com.cheatlearn.bgmiloader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.cheatlearn.bgmiloader.utils.Vector2;
import com.cheatlearn.bgmiloader.utils.Vector3;
import com.cheatlearn.bgmiloader.utils.WorldToScreen;

import java.util.concurrent.atomic.AtomicBoolean;

public class ESPRenderer extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread renderThread;

    private final Paint boxPaint, boxEnemyPaint, linePaint, namePaint, healthBgPaint, healthFgPaint, snaplinePaint;

    public ESPRenderer(Context ctx) {
        super(ctx);
        getHolder().addCallback(this);

        boxPaint = new Paint();
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(2f);
        boxPaint.setColor(Color.argb(220, 0, 255, 0));

        boxEnemyPaint = new Paint();
        boxEnemyPaint.setStyle(Paint.Style.STROKE);
        boxEnemyPaint.setStrokeWidth(2f);
        boxEnemyPaint.setColor(Color.argb(220, 255, 0, 0));

        linePaint = new Paint();
        linePaint.setColor(Color.RED);
        linePaint.setStrokeWidth(2f);

        namePaint = new Paint();
        namePaint.setColor(Color.WHITE);
        namePaint.setTextSize(26f);
        namePaint.setTypeface(Typeface.MONOSPACE);
        namePaint.setAntiAlias(true);
        namePaint.setShadowLayer(2f, 1f, 1f, Color.BLACK);

        healthBgPaint = new Paint();
        healthBgPaint.setStyle(Paint.Style.FILL);
        healthBgPaint.setColor(Color.argb(180, 0, 0, 0));

        healthFgPaint = new Paint();
        healthFgPaint.setStyle(Paint.Style.FILL);

        snaplinePaint = new Paint();
        snaplinePaint.setStrokeWidth(1f);
        snaplinePaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running.set(true);
        renderThread = new Thread(this, "ESPRenderer");
        renderThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int fmt, int w, int h) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) { stop(); }

    public void stop() {
        running.set(false);
        try { if (renderThread != null) renderThread.join(1000); } catch (Exception ignored) {}
    }

    @Override
    public void run() {
        while (running.get()) {
            Canvas canvas = getHolder().lockCanvas();
            if (canvas != null) {
                try { drawFrame(canvas); }
                catch (Exception e) { Log.e("ESPRenderer", "Draw error", e); }
                finally { getHolder().unlockCanvasAndPost(canvas); }
            }
            try { Thread.sleep(33); } catch (InterruptedException e) { break; }
        }
    }

    private void drawFrame(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        canvas.drawColor(Color.argb(0, 0, 0, 0));

        // Crosshair
        linePaint.setColor(Color.argb(100, 255, 255, 255));
        canvas.drawLine(w / 2f - 15, h / 2f, w / 2f + 15, h / 2f, linePaint);
        canvas.drawLine(w / 2f, h / 2f - 15, w / 2f, h / 2f + 15, linePaint);

        float[] viewMatrix = ViewMatrixStore.get();
        EntityData[] entities = SharedEntityBuffer.drainAll();

        for (EntityData e : entities) {
            if (viewMatrix != null) {
                drawWithMatrix(canvas, e, viewMatrix, w, h);
            } else {
                drawFallback(canvas, e, w, h);
            }
        }
    }

    private void drawWithMatrix(Canvas c, EntityData e, float[] vm, int w, int h) {
        Vector2 screen = new Vector2();
        if (!WorldToScreen.project(vm, e.position, screen, w, h)) return;

        float boxH = Math.max(25, 6000f / e.distance);
        float boxW = boxH * 0.6f;
        float left = screen.x - boxW / 2f;
        float top = screen.y - boxH / 2f;

        Paint bp = e.isEnemy ? boxEnemyPaint : boxPaint;

        // Box
        c.drawRect(left, top, left + boxW, top + boxH, bp);

        // Snapline
        snaplinePaint.setColor(e.isEnemy ? Color.argb(80, 255, 0, 0) : Color.argb(80, 0, 255, 0));
        c.drawLine(w / 2f, h, screen.x, screen.y, snaplinePaint);

        // Health bar background
        c.drawRect(left - 5, top, left - 2, top + boxH, healthBgPaint);
        float healthPct = Math.max(0, e.health / (float) e.maxHealth);
        float healthBarH = boxH * healthPct;
        healthFgPaint.setColor(e.health > 50 ? Color.GREEN : e.health > 25 ? Color.YELLOW : Color.RED);
        c.drawRect(left - 5, top + boxH - healthBarH, left - 2, top + boxH, healthFgPaint);

        // Name + distance
        String label = e.name + " [" + (int) e.distance + "m]";
        namePaint.setColor(e.isEnemy ? Color.RED : Color.GREEN);
        float tw = namePaint.measureText(label);
        c.drawText(label, screen.x - tw / 2f, top - 8, namePaint);
    }

    private void drawFallback(Canvas c, EntityData e, int w, int h) {
        if (e.position.x == 0 && e.position.y == 0) return;

        float screenY = h * 0.3f;
        float screenX = w / 2f + (float) (Math.random() * 200 - 100);

        snaplinePaint.setColor(e.isEnemy ? Color.argb(80, 255, 0, 0) : Color.argb(80, 0, 255, 0));
        c.drawLine(w / 2f, h, screenX, screenY, snaplinePaint);

        String label = e.name + " [" + (int) e.distance + "m]";
        namePaint.setColor(e.isEnemy ? Color.RED : Color.GREEN);
        float tw = namePaint.measureText(label);
        c.drawText(label, screenX - tw / 2f, screenY - 10, namePaint);
    }
}

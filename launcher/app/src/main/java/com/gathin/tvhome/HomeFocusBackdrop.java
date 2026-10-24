package com.gathin.tvhome;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Full-bleed Home background behind the apps. Photos on: the screensaver photo store,
 * center-cropped, advancing on the Photos change interval. Photos off: one random gradient.
 */
final class HomeFocusBackdrop {
    private static final long EMPTY_RETRY_MS = 8_000L;
    private final Activity activity;
    private final ImageView photo;
    private final View scrim;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Runnable tick = this::advance;
    private int generation;
    private String currentFile = "";
    private boolean running;
    private boolean askedDownload;
    private Bitmap shown;

    HomeFocusBackdrop(Activity activity, ImageView photo, View scrim) {
        this.activity = activity;
        this.photo = photo;
        this.scrim = scrim;
        scrim.setBackgroundColor(HomeFocusPhoto.PHOTO_SCRIM);
    }

    /** Show the current background and, when photos are on, schedule the next change. */
    void start() {
        running = true;
        showNow(false);
    }

    /** Stop the rotation timer. The last frame stays until {@link #hide()}. */
    void stop() {
        running = false;
        handler.removeCallbacks(tick);
    }

    /** Reload after a photo download or a settings change while Home is in front. */
    void refresh() {
        if (running) showNow(false);
    }

    void hide() {
        stop();
        generation++;
        currentFile = "";
        photo.setImageDrawable(null);
        photo.setBackground(null);
        photo.setAlpha(0f);
        scrim.setAlpha(0f);
        recycleShown(null);
    }

    void destroy() {
        hide();
        worker.shutdownNow();
    }

    private void advance() {
        if (running) showNow(true);
    }

    private void showNow(boolean advance) {
        handler.removeCallbacks(tick);
        if (!PhotosMaster.on(activity)) {
            showGradient();
            return;
        }
        List<PhotoStore.Photo> photos = PhotoStore.list(activity, PhotoStore.chosenSource(activity));
        if (photos.isEmpty()) {
            showGradient();
            if (!askedDownload) {
                askedDownload = true;
                try { PhotoStore.scheduleIfMissingThrottled(activity); } catch (Throwable ignored) {}
                try { PhotoStore.schedule(activity, true); } catch (Throwable ignored) {}
            }
            if (running) handler.postDelayed(tick, EMPTY_RETRY_MS);
            return;
        }
        askedDownload = false;
        ArrayList<String> files = new ArrayList<>();
        for (PhotoStore.Photo item : photos) files.add(item.file);
        String next = advance || !files.contains(currentFile)
                ? PhotoBackgroundCycle.nextFile(files, currentFile)
                : currentFile;
        if (next.isEmpty()) {
            showGradient();
            return;
        }
        currentFile = next;
        final int request = ++generation;
        final String file = next;
        worker.execute(() -> {
            Bitmap bitmap = decode(file);
            activity.runOnUiThread(() -> {
                if (activity.isFinishing() || request != generation) {
                    if (bitmap != null) bitmap.recycle();
                    return;
                }
                if (bitmap == null) {
                    showGradient();
                    if (running) handler.postDelayed(tick, interval());
                    return;
                }
                photo.setBackground(null);
                photo.setImageBitmap(bitmap);
                recycleShown(bitmap);
                shown = bitmap;
                photo.setAlpha(1f);
                scrim.setAlpha(1f);
            });
        });
        if (running) handler.postDelayed(tick, interval());
    }

    private void showGradient() {
        generation++;
        currentFile = "";
        if (photo.getDrawable() != null || photo.getBackground() == null) {
            photo.setImageDrawable(null);
            recycleShown(null);
            photo.setBackground(PhotoGradients.drawable(new java.util.Random().nextInt()));
        }
        photo.setAlpha(1f);
        scrim.setAlpha(0f);
    }

    private long interval() {
        SharedPreferences prefs = activity.getSharedPreferences(Config.SETTINGS_NAME, Activity.MODE_PRIVATE);
        return HomeFocusPhoto.intervalMs(prefs.getLong("photo_interval_ms", HomeFocusPhoto.INTERVAL_DEFAULT_MS));
    }

    private void recycleShown(Bitmap keep) {
        if (shown != null && shown != keep && !shown.isRecycled()) shown.recycle();
        if (keep == null) shown = null;
    }

    private Bitmap decode(String file) {
        try {
            File path = new File(PhotoStore.dir(activity), file);
            android.util.DisplayMetrics dm = activity.getResources().getDisplayMetrics();
            int screenW = Math.max(1, dm.widthPixels);
            int screenH = Math.max(1, dm.heightPixels);
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path.getAbsolutePath(), bounds);
            int sample = 1;
            while (sample < 64 && (bounds.outWidth / (sample * 2) > screenW || bounds.outHeight / (sample * 2) > screenH)) sample *= 2;
            bounds.inJustDecodeBounds = false;
            bounds.inSampleSize = sample;
            bounds.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeFile(path.getAbsolutePath(), bounds);
        } catch (Throwable ignored) {
            return null;
        }
    }
}

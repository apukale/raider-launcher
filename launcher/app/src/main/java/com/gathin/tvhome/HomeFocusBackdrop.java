package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Screensaver photo behind Home while an app tile is focused.
 * The photo is decoded small, blurred once, and covered with the theme colour
 * so the shade matches the solid background. A live full-screen blur is skipped:
 * it is too expensive for a Mali-G31 on 2 GB.
 */
final class HomeFocusBackdrop {
    private final Activity activity;
    private final ImageView photo;
    private final View scrim;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private int index = -1;
    private int generation;
    private String shownFor = "";

    HomeFocusBackdrop(Activity activity, ImageView photo, View scrim) {
        this.activity = activity;
        this.photo = photo;
        this.scrim = scrim;
    }

    void setScrim(int background) { scrim.setBackgroundColor(HomeFocusPhoto.scrim(background)); }

    void onTile(String key) {
        if (key == null) key = "";
        if (!PhotosMaster.on(activity)) {
            shownFor = key;
            photo.setImageDrawable(null);
            if (photo.getBackground() == null) photo.setBackground(PhotoGradients.drawable(new java.util.Random().nextInt()));
            photo.setAlpha(1f);
            scrim.setAlpha(0f);
            return;
        }
        if (key.equals(shownFor) && photo.getDrawable() != null) {
            show();
            return;
        }
        List<PhotoStore.Photo> photos = PhotoStore.list(activity, PhotoStore.chosenSource(activity));
        if (photos.isEmpty()) { hide(); return; }
        index = HomeFocusPhoto.nextIndex(photos.size(), index);
        if (index < 0) { hide(); return; }
        String file = photos.get(index).file;
        shownFor = key;
        final int request = ++generation;
        worker.execute(() -> {
            Bitmap bitmap = decode(file);
            activity.runOnUiThread(() -> {
                if (activity.isFinishing() || request != generation) return;
                if (bitmap == null) { hide(); return; }
                android.graphics.drawable.Drawable previous = photo.getDrawable();
                photo.setImageBitmap(bitmap);
                if (previous instanceof android.graphics.drawable.BitmapDrawable) {
                    Bitmap old = ((android.graphics.drawable.BitmapDrawable) previous).getBitmap();
                    if (old != null && old != bitmap && !old.isRecycled()) old.recycle();
                }
                show();
            });
        });
    }

    void hide() {
        generation++;
        shownFor = "";
        photo.animate().cancel();
        scrim.animate().cancel();
        photo.setAlpha(0f);
        scrim.setAlpha(0f);
    }

    void destroy() {
        generation++;
        photo.setImageDrawable(null);
        worker.shutdownNow();
    }

    private void show() {
        photo.animate().cancel();
        scrim.animate().cancel();
        photo.setAlpha(1f);
        scrim.setAlpha(1f);
    }

    private Bitmap decode(String file) {
        try {
            File path = new File(PhotoStore.dir(activity), file);
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path.getAbsolutePath(), bounds);
            int sample = 1;
            while (sample < 64 && (bounds.outWidth / sample > 480 || bounds.outHeight / sample > 270)) sample *= 2;
            bounds.inJustDecodeBounds = false;
            bounds.inSampleSize = sample;
            bounds.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap raw = BitmapFactory.decodeFile(path.getAbsolutePath(), bounds);
            if (raw == null) return null;
            int w = raw.getWidth(), h = raw.getHeight();
            int[] pixels = new int[w * h];
            raw.getPixels(pixels, 0, w, 0, 0, w, h);
            BoxBlur.blur(pixels, w, h, 6, 1);
            raw.setPixels(pixels, 0, w, 0, 0, w, h);
            return raw;
        } catch (Throwable ignored) {
            return null;
        }
    }
}

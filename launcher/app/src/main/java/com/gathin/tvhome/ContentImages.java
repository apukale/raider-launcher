package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One decode thread and a small RGB_565 memory cache. {@link #trim()} drops the bitmaps
 * when Home stops so a 2 GB set is not holding posters in the background.
 */
final class ContentImages {
    private static final ExecutorService DECODE = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "tmdb-img");
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final AtomicInteger EPOCH = new AtomicInteger();
    private static final LruCache<String, Bitmap> MEMORY = new LruCache<String, Bitmap>(MemoryPolicy.IMAGE_MEMORY_CAP) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value == null ? 0 : value.getByteCount();
        }
    };
    private static DiskCache disk;
    private static int placeholder = 0xFF1A1B2E;

    private ContentImages() {}

    static void prepare(Context context) {
        if (disk == null && context != null) disk = new DiskCache(new File(context.getApplicationContext().getCacheDir(), "tmdb"));
    }

    static void load(Context context, ImageView view, String path, int size) {
        if (view == null) return;
        prepare(context);
        if (path == null || path.isEmpty()) {
            view.setImageDrawable(null);
            view.setTag(null);
            view.setBackgroundColor(placeholder);
            return;
        }
        int width = MemoryPolicy.imageSize(size);
        String key = width + path;
        view.setTag(key);
        view.setBackgroundColor(placeholder);
        Bitmap cached = MEMORY.get(key);
        if (cached != null && !cached.isRecycled()) {
            view.setImageBitmap(cached);
            return;
        }
        view.setImageDrawable(null);
        int epoch = EPOCH.get();
        Context app = context.getApplicationContext();
        DECODE.execute(() -> {
            if (epoch != EPOCH.get()) return;
            Bitmap bitmap = decode(app, key, path, width);
            if (bitmap == null || epoch != EPOCH.get()) return;
            MEMORY.put(key, bitmap);
            MAIN.post(() -> {
                if (epoch != EPOCH.get()) return;
                if (key.equals(view.getTag())) view.setImageBitmap(bitmap);
            });
        });
    }

    static void trim() {
        EPOCH.incrementAndGet();
        MEMORY.evictAll();
    }

    private static Bitmap decode(Context context, String key, String path, int width) {
        prepare(context);
        if (disk == null) return null;
        long now = System.currentTimeMillis();
        byte[] bytes = disk.getBytes(key, now, CacheTtl.IMAGES_MS);
        if (bytes == null) {
            try {
                bytes = TmdbClient.image(width, path);
                disk.putBytes(key, bytes, now);
                disk.trimBytes(MemoryPolicy.IMAGE_DISK_CAP);
            } catch (Exception e) {
                return null;
            }
        }
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        opts.inSampleSize = MemoryPolicy.sampleSize(bounds.outWidth, bounds.outHeight, width, width);
        try {
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
        } catch (OutOfMemoryError e) {
            MEMORY.evictAll();
            return null;
        }
    }
}

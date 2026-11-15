package com.gathin.tvhome;

/**
 * Image sizes for a 2 GB Android 11 TV. Posters stay on the small TMDB sizes.
 * The hero backdrop is w780 and decoded as RGB_565 by the loader. The disk cap
 * is the image cache only; list JSON is tiny next to it.
 */
final class MemoryPolicy {
    static final int POSTER_PX = 185;
    static final int POSTER_LARGE_PX = 342;
    static final int HERO_PX = 780;
    static final long IMAGE_DISK_CAP = 80L * 1024 * 1024;
    static final int IMAGE_MEMORY_CAP = 8 * 1024 * 1024;
    static final int TRENDING_CAP = 18;
    static final int TOP_CAP = 10;
    static final int CAST_CAP = 12;
    /** Below this, the band above the dock cannot hold a hero and a row. */
    static final int MIN_SHELF_DP = 96;

    private MemoryPolicy() {}

    /** TMDB width bucket for a view that is about {@code targetPx} wide. */
    static int imageSize(int targetPx) {
        if (targetPx <= 220) return POSTER_PX;
        if (targetPx <= 420) return POSTER_LARGE_PX;
        return HERO_PX;
    }

    /** Largest power-of-two sample that still covers the requested size. */
    static int sampleSize(int srcW, int srcH, int reqW, int reqH) {
        if (srcW <= 0 || srcH <= 0 || reqW <= 0 || reqH <= 0) return 1;
        int size = 1;
        while (srcW / (size * 2) >= reqW && srcH / (size * 2) >= reqH) size *= 2;
        return size;
    }
}

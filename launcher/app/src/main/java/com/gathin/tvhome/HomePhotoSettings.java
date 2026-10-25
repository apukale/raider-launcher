package com.gathin.tvhome;

/**
 * Home Layout photo rows and the Photos panel share one switch and one interval.
 * Home Focus backdrop reads the same keys.
 */
final class HomePhotoSettings {
    static final String MASTER = PhotosMaster.PREF;
    static final String INTERVAL = "photo_interval_ms";
    static final long[] INTERVALS_MS = {30_000L, 60_000L, 120_000L, 300_000L};
    static final String[] LABELS = {"30 s", "1 min", "2 min", "5 min"};
    /** Same dim as the Photos panel interval row. */
    static final float ENABLED_ALPHA = 1f;
    static final float DISABLED_ALPHA = 0.45f;

    private HomePhotoSettings() {}

    static int selectedIndex(long stored) {
        for (int i = 0; i < INTERVALS_MS.length; i++) if (INTERVALS_MS[i] == stored) return i;
        return 1;
    }

    static boolean intervalInteractive(boolean photosOn) { return photosOn; }

    static float intervalAlpha(boolean photosOn) {
        return photosOn ? ENABLED_ALPHA : DISABLED_ALPHA;
    }
}

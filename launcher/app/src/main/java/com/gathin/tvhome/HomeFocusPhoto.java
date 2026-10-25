package com.gathin.tvhome;

/** Backdrop index, the Photos change interval, and the shade over a Home photo. */
final class HomeFocusPhoto {
    static final long INTERVAL_FLOOR_MS = 30_000L;
    static final long INTERVAL_DEFAULT_MS = 60_000L;
    /** Light black shade so the clock stays readable and the picture still shows. */
    static final int PHOTO_SCRIM = 0x59000000;

    private HomeFocusPhoto() {}

    /** Same floor as the screensaver. A missing pref is passed in as {@link #INTERVAL_DEFAULT_MS}. */
    static long intervalMs(long stored) {
        return Math.max(INTERVAL_FLOOR_MS, stored);
    }

    /**
     * Delay before the next Home photo, or {@code 0} when Photos is off.
     * A zero delay means do not schedule: the Home background stays a gradient and does not rotate.
     */
    static long rotationDelayMs(boolean photosOn, long stored) {
        if (!photosOn) return 0L;
        return intervalMs(stored);
    }

    /** Row value for the Photos change interval. Matches the picker: {@code 30 s}, {@code 1 min}. */
    static String intervalLabel(long stored) {
        long ms = intervalMs(stored);
        if (ms % 60_000L == 0L) return (ms / 60_000L) + " min";
        return (ms / 1000L) + " s";
    }

    /** Theme colour at 70% over the photo, so the shade matches the solid home colour. */
    static int scrim(int background) { return 0xB3000000 | (background & 0x00FFFFFF); }

    static int nextIndex(int count, int current) {
        if (count <= 0) return -1;
        if (current < 0 || current >= count) return 0;
        return (current + 1) % count;
    }
}

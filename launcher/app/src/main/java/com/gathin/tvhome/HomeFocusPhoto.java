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

    /** Theme colour at 70% over the photo, so the shade matches the solid home colour. */
    static int scrim(int background) { return 0xB3000000 | (background & 0x00FFFFFF); }

    static int nextIndex(int count, int current) {
        if (count <= 0) return -1;
        if (current < 0 || current >= count) return 0;
        return (current + 1) % count;
    }
}

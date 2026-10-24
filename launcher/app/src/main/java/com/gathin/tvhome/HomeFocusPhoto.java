package com.gathin.tvhome;

/** Backdrop index and the shade laid over a screensaver photo on Home. */
final class HomeFocusPhoto {
    private HomeFocusPhoto() {}

    /** Theme colour at 70% over the photo, so the shade matches the solid home colour. */
    static int scrim(int background) { return 0xB3000000 | (background & 0x00FFFFFF); }

    static int nextIndex(int count, int current) {
        if (count <= 0) return -1;
        if (current < 0 || current >= count) return 0;
        return (current + 1) % count;
    }
}

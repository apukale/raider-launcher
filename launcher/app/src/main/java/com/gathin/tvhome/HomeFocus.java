package com.gathin.tvhome;

/**
 * Vertical focus on Home: top bar, hero, Trending, Top 10, dock.
 * Hidden zones are skipped. {@code -1} means the move leaves this chain.
 */
final class HomeFocus {
    static final int TOP = 0;
    static final int HERO = 1;
    static final int TRENDING = 2;
    static final int TOP10 = 3;
    static final int DOCK = 4;

    private HomeFocus() {}

    static int next(int zone, boolean up, boolean hero, boolean trending, boolean top) {
        int step = up ? -1 : 1;
        int z = zone;
        while (true) {
            z += step;
            if (z < TOP || z > DOCK) return -1;
            if (z == HERO && !hero) continue;
            if (z == TRENDING && !trending) continue;
            if (z == TOP10 && !top) continue;
            return z;
        }
    }
}

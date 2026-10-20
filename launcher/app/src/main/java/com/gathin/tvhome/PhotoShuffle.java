package com.gathin.tvhome;

import java.util.List;
import java.util.Random;

/** Pure shuffle order for screensaver / Panchang background photos. */
final class PhotoShuffle {
    private PhotoShuffle() {}

    /**
     * Random permutation of 0..n-1. When n > 1 and avoidFirst is a valid index, the
     * permutation never starts with avoidFirst (a new cycle never repeats the photo just
     * shown; a new start never repeats the previous start's first photo).
     */
    static int[] order(int n, int avoidFirst, Random random) {
        if (n <= 0) return new int[0];
        int[] out = new int[n];
        for (int i = 0; i < n; i++) out[i] = i;
        for (int i = n - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int t = out[i]; out[i] = out[j]; out[j] = t;
        }
        if (n > 1 && out[0] == avoidFirst) {
            int j = 1 + random.nextInt(n - 1);
            int t = out[0]; out[0] = out[j]; out[j] = t;
        }
        return out;
    }

    /** Index of key in keys, or -1. */
    static int indexOf(List<String> keys, String key) {
        if (key == null || key.isEmpty()) return -1;
        for (int i = 0; i < keys.size(); i++) if (key.equals(keys.get(i))) return i;
        return -1;
    }

    /** Random start index, avoiding avoid when there is a choice. */
    static int pick(int n, int avoid, Random random) {
        if (n <= 0) return -1;
        if (n == 1) return 0;
        int i = random.nextInt(n - (avoid >= 0 && avoid < n ? 1 : 0));
        if (avoid >= 0 && avoid < n && i >= avoid) i++;
        return i;
    }
}

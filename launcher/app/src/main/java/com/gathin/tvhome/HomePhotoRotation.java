package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/** Shuffled Home background order. A new session and each finished cycle get a new permutation. */
final class HomePhotoRotation {
    private HomePhotoRotation() {}

    /**
     * Random order of {@code files}. When more than one photo is available, {@code avoid}
     * is not placed first, so a new cycle does not open on the photo just shown.
     */
    static List<String> shuffle(List<String> files, String avoid, Random random) {
        if (files == null || files.isEmpty()) return Collections.emptyList();
        int[] order = PhotoShuffle.order(files.size(), PhotoShuffle.indexOf(files, avoid), random);
        ArrayList<String> out = new ArrayList<>(order.length);
        for (int index : order) out.add(files.get(index));
        return out;
    }

    /** True when {@code deck} is a permutation of {@code catalog}. */
    static boolean sameCatalog(List<String> deck, List<String> catalog) {
        if (deck == null || catalog == null || deck.size() != catalog.size()) return false;
        HashSet<String> names = new HashSet<>(catalog);
        if (names.size() != catalog.size()) return false;
        for (String file : deck) if (!names.contains(file)) return false;
        return true;
    }

    /**
     * Next index in the current deck. {@code -1} means this pass is finished and the caller
     * should build a new shuffle. {@code cursor} is the photo on screen, or {@code -1} before the first.
     */
    static int advance(int count, int cursor) {
        if (count <= 0) return -1;
        int next = cursor + 1;
        return next >= count ? -1 : next;
    }
}

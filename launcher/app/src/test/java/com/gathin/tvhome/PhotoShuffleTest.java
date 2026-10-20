package com.gathin.tvhome;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

import static org.junit.Assert.*;

public final class PhotoShuffleTest {
    @Test(timeout = 5000)
    public void orderIsPermutationAndAvoidsFirst() {
        Random r = new Random(42);
        for (int n = 1; n <= 30; n++) {
            for (int trial = 0; trial < 50; trial++) {
                int avoid = trial % n;
                int[] o = PhotoShuffle.order(n, avoid, r);
                assertEquals(n, o.length);
                HashSet<Integer> seen = new HashSet<>();
                for (int v : o) { assertTrue(v >= 0 && v < n); assertTrue(seen.add(v)); }
                if (n > 1) assertNotEquals("n=" + n, avoid, o[0]);
            }
        }
        assertEquals(0, PhotoShuffle.order(0, -1, r).length);
    }

    @Test(timeout = 5000)
    public void shuffleActuallyVariesFirstPhoto() {
        Random r = new Random(7);
        HashSet<Integer> firsts = new HashSet<>();
        for (int i = 0; i < 200; i++) firsts.add(PhotoShuffle.order(10, -1, r)[0]);
        assertTrue(firsts.size() >= 8);
    }

    @Test(timeout = 5000)
    public void pickAvoidsAndIndexOf() {
        Random r = new Random(3);
        for (int i = 0; i < 500; i++) {
            int p = PhotoShuffle.pick(5, 2, r);
            assertTrue(p >= 0 && p < 5);
            assertNotEquals(2, p);
        }
        assertEquals(0, PhotoShuffle.pick(1, 0, r));
        assertEquals(-1, PhotoShuffle.pick(0, -1, r));
        assertEquals(1, PhotoShuffle.indexOf(Arrays.asList("a.jpg", "b.jpg"), "b.jpg"));
        assertEquals(-1, PhotoShuffle.indexOf(Arrays.asList("a.jpg"), "zz.jpg"));
        assertEquals(-1, PhotoShuffle.indexOf(Arrays.asList("a.jpg"), ""));
    }
}

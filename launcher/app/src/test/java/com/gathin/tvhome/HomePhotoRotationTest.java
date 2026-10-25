package com.gathin.tvhome;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HomePhotoRotationTest {
    @Test(timeout = 5000)
    public void shufflePermutesAndAvoidsThePhotoJustShown() {
        List<String> files = Arrays.asList("a.jpg", "b.jpg", "c.jpg", "d.jpg");
        List<String> deck = HomePhotoRotation.shuffle(files, "a.jpg", new Random(1));
        assertEquals(new HashSet<>(files), new HashSet<>(deck));
        assertFalse(deck.get(0).equals("a.jpg"));
        assertTrue(HomePhotoRotation.shuffle(Arrays.asList(), "a.jpg", new Random(1)).isEmpty());
        assertEquals("only.jpg", HomePhotoRotation.shuffle(Arrays.asList("only.jpg"), "only.jpg", new Random(1)).get(0));
    }

    @Test(timeout = 5000)
    public void sessionsDoNotShareOneStartingImage() {
        List<String> files = Arrays.asList("a.jpg", "b.jpg", "c.jpg", "d.jpg");
        HashSet<String> firsts = new HashSet<>();
        HashSet<String> orders = new HashSet<>();
        for (int seed = 0; seed < 40; seed++) {
            List<String> deck = HomePhotoRotation.shuffle(files, "", new Random(seed));
            firsts.add(deck.get(0));
            orders.add(deck.toString());
        }
        assertTrue(firsts.size() >= 3);
        assertTrue(orders.size() > 1);
    }

    @Test(timeout = 5000)
    public void aCycleVisitsEachPhotoOnceThenAsksForANewShuffle() {
        List<String> deck = Arrays.asList("a.jpg", "b.jpg", "c.jpg", "d.jpg");
        ArrayList<String> seen = new ArrayList<>();
        int cursor = 0;
        seen.add(deck.get(cursor));
        for (int i = 0; i < deck.size() - 1; i++) {
            cursor = HomePhotoRotation.advance(deck.size(), cursor);
            assertTrue(cursor >= 0);
            seen.add(deck.get(cursor));
        }
        assertEquals(new HashSet<>(deck), new HashSet<>(seen));
        assertEquals(-1, HomePhotoRotation.advance(deck.size(), cursor));
        assertEquals(-1, HomePhotoRotation.advance(0, -1));
    }

    @Test(timeout = 5000)
    public void catalogChangeIsNotTheSameDeck() {
        assertTrue(HomePhotoRotation.sameCatalog(Arrays.asList("b.jpg", "a.jpg"), Arrays.asList("a.jpg", "b.jpg")));
        assertFalse(HomePhotoRotation.sameCatalog(Arrays.asList("a.jpg"), Arrays.asList("a.jpg", "b.jpg")));
        assertFalse(HomePhotoRotation.sameCatalog(null, Arrays.asList("a.jpg")));
    }
}

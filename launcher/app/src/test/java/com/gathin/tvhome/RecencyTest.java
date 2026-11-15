package com.gathin.tvhome;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RecencyTest {
    @Test(timeout = 5000)
    public void windowStepsWidenUpToFortyFiveDays() {
        assertEquals("2026-11-08", Recency.minusDays("2026-11-15", 7));
        assertEquals("2026-10-01", Recency.minusDays("2026-11-15", 45));
        assertArrayEquals(new int[]{3, 7, 14, 21, 30, 45}, Recency.steps(3));
        assertArrayEquals(new int[]{7, 14, 21, 30, 45}, Recency.steps(7));
        assertArrayEquals(new int[]{14, 21, 30, 45}, Recency.steps(14));
        assertEquals(45, Recency.steps(3)[Recency.steps(3).length - 1]);
        assertFalse(Recency.filled(7, 6, true, true));
        assertTrue(Recency.filled(8, 6, true, true));
        assertTrue(Recency.filled(0, 6, false, true));
        assertTrue(Recency.onOrAfter("2026-11-10", "2026-11-08"));
        assertFalse(Recency.onOrAfter("2026-11-01", "2026-11-08"));
    }

    @Test(timeout = 5000)
    public void languagesDefaultToTeluguHindiEnglishAndWindowDefaultsToAWeek() {
        assertEquals("te,hi,en", ContentLanguages.join(ContentLanguages.checked(null)));
        assertTrue(Arrays.equals(new String[]{"te", "hi", "en"}, ContentLanguages.selected("")));
        assertEquals("te,ta,kn", ContentLanguages.join(new boolean[]{true, false, false, true, false, true}));
        assertEquals("Telugu, Hindi", ContentLanguages.label("te,hi"));
        assertEquals(7, ContentWindow.normalize(0));
        assertEquals(3, ContentWindow.normalize(3));
        assertEquals(14, ContentWindow.normalize(14));
        assertTrue(ContentWindow.dailyTrending(3));
        assertFalse(ContentWindow.dailyTrending(7));
        assertFalse(ContentWindow.dailyTrending(14));
        assertEquals("1 week", ContentWindow.label(7));
        assertEquals("3 days", ContentWindow.label(3));
    }

    @Test(timeout = 5000)
    public void discoverUrlsStayOnSubscriptionAndTheChosenWindow() {
        String movies = TmdbUrls.discover(TmdbModels.MOVIE, "8|122", "te", "2026-11-08", 1);
        assertTrue(movies.contains("primary_release_date.gte=2026-11-08"));
        assertTrue(movies.contains("with_original_language=te"));
        assertTrue(movies.contains("flatrate"));
        assertTrue(movies.contains("free"));
        assertTrue(movies.contains("ads"));
        assertFalse(movies.contains("rent"));
        assertFalse(movies.contains("buy"));
        String shows = TmdbUrls.discover(TmdbModels.TV, "8", "hi", "2026-11-01", 2);
        assertTrue(shows.contains("air_date.gte=2026-11-01"));
        assertTrue(shows.contains("page=2"));
        assertTrue(TmdbUrls.trending(true).contains("/trending/all/day"));
        assertTrue(TmdbUrls.trending(false).contains("/trending/all/week"));
    }
}

package com.gathin.tvhome;

import org.junit.Test;

import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CacheTtlTest {
    @Test(timeout = 5000)
    public void listTtlSitsInsideTheThreeToSixHourWindow() {
        assertTrue(CacheTtl.LISTS_MS >= CacheTtl.LISTS_MIN_MS);
        assertTrue(CacheTtl.LISTS_MS <= CacheTtl.LISTS_MAX_MS);
        assertEquals(3L * 60 * 60 * 1000, CacheTtl.LISTS_MIN_MS);
        assertEquals(6L * 60 * 60 * 1000, CacheTtl.LISTS_MAX_MS);
        assertEquals(4L * 60 * 60 * 1000, CacheTtl.LISTS_MS);
    }

    @Test(timeout = 5000)
    public void detailExternalAndImageLifetimes() {
        assertEquals(24L * 60 * 60 * 1000, CacheTtl.DETAILS_MS);
        assertEquals(30L * 24 * 60 * 60 * 1000, CacheTtl.EXTERNAL_MS);
        assertEquals(7L * 24 * 60 * 60 * 1000, CacheTtl.IMAGES_MS);
    }

    @Test(timeout = 5000)
    public void freshUntilTheTtlThenStale() {
        long stored = 5_000L;
        long ttl = CacheTtl.LISTS_MS;
        assertTrue(CacheTtl.fresh(stored, stored, ttl));
        assertTrue(CacheTtl.fresh(stored, stored + ttl - 1, ttl));
        assertFalse(CacheTtl.fresh(stored, stored + ttl, ttl));
        assertFalse(CacheTtl.fresh(0, stored, ttl));
        assertFalse(CacheTtl.fresh(stored, stored - 1, ttl));
        assertFalse(CacheTtl.fresh(stored, stored + 1, 0));
        assertTrue(CacheTtl.fresh(stored, stored + CacheTtl.DETAILS_MS - 1, CacheTtl.DETAILS_MS));
        assertFalse(CacheTtl.fresh(stored, stored + CacheTtl.DETAILS_MS, CacheTtl.DETAILS_MS));
        assertTrue(CacheTtl.fresh(stored, stored + CacheTtl.EXTERNAL_MS - 1, CacheTtl.EXTERNAL_MS));
        assertFalse(CacheTtl.fresh(stored, stored + CacheTtl.IMAGES_MS, CacheTtl.IMAGES_MS));
    }

    @Test(timeout = 5000)
    public void diskCacheDropsExpiredTextAndOldestImages() throws Exception {
        DiskCache cache = new DiskCache(Files.createTempDirectory("tmdb-cache").toFile());
        long stored = 10_000L;
        cache.putText("lists", "{\"results\":[]}", stored);
        assertEquals("{\"results\":[]}", cache.getText("lists", stored + CacheTtl.LISTS_MS - 1, CacheTtl.LISTS_MS));
        assertNull(cache.getText("lists", stored + CacheTtl.LISTS_MS, CacheTtl.LISTS_MS));
        assertEquals("{\"results\":[]}", cache.getTextAnyAge("lists"));

        cache.putBytes("old", new byte[100], 10);
        cache.putBytes("mid", new byte[100], 20);
        cache.putBytes("new", new byte[100], 30);
        cache.trimBytes(150);
        assertNull(cache.getBytes("old", 30, CacheTtl.IMAGES_MS));
        assertNull(cache.getBytes("mid", 30, CacheTtl.IMAGES_MS));
        assertNotNull(cache.getBytes("new", 30, CacheTtl.IMAGES_MS));
        assertNull(cache.getBytes("new", 30 + CacheTtl.IMAGES_MS, CacheTtl.IMAGES_MS));
    }
}

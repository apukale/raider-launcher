package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HomePhotoSettingsTest {
    @Test(timeout = 5000)
    public void homeLayoutUsesThePhotosMasterAndTheSameIntervals() {
        assertEquals(PhotosMaster.PREF, HomePhotoSettings.MASTER);
        assertEquals("photo_interval_ms", HomePhotoSettings.INTERVAL);
        assertEquals(HomePhotoSettings.LABELS.length, HomePhotoSettings.INTERVALS_MS.length);
        for (int i = 0; i < HomePhotoSettings.INTERVALS_MS.length; i++) {
            long ms = HomePhotoSettings.INTERVALS_MS[i];
            assertEquals(HomePhotoSettings.LABELS[i], HomeFocusPhoto.intervalLabel(ms));
            assertEquals(i, HomePhotoSettings.selectedIndex(ms));
        }
        assertEquals(1, HomePhotoSettings.selectedIndex(0L));
        assertEquals(HomeFocusPhoto.INTERVAL_DEFAULT_MS, HomePhotoSettings.INTERVALS_MS[1]);
    }

    @Test(timeout = 5000)
    public void intervalRowDimsWhenPhotosAreOff() {
        assertTrue(HomePhotoSettings.intervalInteractive(true));
        assertFalse(HomePhotoSettings.intervalInteractive(false));
        assertEquals(1f, HomePhotoSettings.intervalAlpha(true), 0f);
        assertEquals(0.45f, HomePhotoSettings.intervalAlpha(false), 0f);
        assertEquals(0L, HomeFocusPhoto.rotationDelayMs(false, HomePhotoSettings.INTERVALS_MS[2]));
        assertEquals(HomePhotoSettings.INTERVALS_MS[2], HomeFocusPhoto.rotationDelayMs(true, HomePhotoSettings.INTERVALS_MS[2]));
    }
}

package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class RaiderThemeTest {
    @Test(timeout = 5000) public void eightThemesAndRetiredNames() {
        assertEquals(8, RaiderColors.themes().length);
        assertEquals(0xFFF6C453, RaiderColors.accentColor("Indigo"));
        assertEquals(0xFF9FD6FF, RaiderColors.accentColor("Midnight"));
        assertEquals(0xFFE8A04A, RaiderColors.accentColor("Espresso"));
        assertEquals(0xFF7DDCB0, RaiderColors.accentColor("Forest"));
        assertEquals(0xFFE29AD4, RaiderColors.accentColor("Plum"));
        assertEquals(0xFFE24B5A, RaiderColors.accentColor("Onyx"));
        assertEquals(0xFFD5D8DE, RaiderColors.accentColor("True Black"));
        assertEquals(0xFF4A2E22, RaiderColors.accentColor("Porcelain"));
        assertEquals("Indigo", RaiderColors.theme("Marigold").id);
        assertEquals("Forest", RaiderColors.theme("Teal").id);
        assertEquals("Plum", RaiderColors.theme("Rose").id);
        assertEquals("True Black", RaiderColors.theme("Silver").id);
        assertEquals("Indigo", RaiderColors.theme(null).id);
        assertEquals("Indigo", RaiderColors.theme("nope").id);
        assertTrue(RaiderColors.theme("Porcelain").light);
        assertFalse(RaiderColors.theme("Indigo").light);
        assertEquals("Indigo + Marigold", RaiderColors.labels()[0]);
        assertEquals("Porcelain + Espresso", RaiderColors.labels()[7]);
    }

    @Test(timeout = 5000) public void focusPhotoScrimAndCycle() {
        assertEquals(0xB312142B, HomeFocusPhoto.scrim(0xFF12142B));
        assertEquals(-1, HomeFocusPhoto.nextIndex(0, -1));
        assertEquals(0, HomeFocusPhoto.nextIndex(4, -1));
        assertEquals(1, HomeFocusPhoto.nextIndex(4, 0));
        assertEquals(0, HomeFocusPhoto.nextIndex(4, 3));
    }

    @Test(timeout = 5000) public void recommendationsDefaultOnAndCap() {
        assertTrue(HomeRecs.shown(false, false));
        assertTrue(HomeRecs.shown(true, true));
        assertFalse(HomeRecs.shown(true, false));
        java.util.ArrayList<HomeRecs.Item> items = new java.util.ArrayList<>();
        for (int i = 0; i < 8; i++) items.add(new HomeRecs.Item("Title " + i, null, null));
        items.add(new HomeRecs.Item("  ", null, null));
        assertEquals(HomeRecs.MAX, HomeRecs.cap(items).size());
        assertEquals("Title 0", HomeRecs.cap(items).get(0).title);
        assertTrue(HomeRecs.cap(null).isEmpty());
    }
}

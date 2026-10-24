package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class RaiderThemeTest {
    @Test(timeout = 5000) public void sevenThemesAndRetiredNames() {
        assertEquals(7, RaiderColors.themes().length);
        assertEquals(0xFFF2A93B, RaiderColors.accentColor("Indigo"));
        assertEquals(0xFFF5B942, RaiderColors.theme("Indigo").focus);
        assertEquals(0xFF0E1226, RaiderColors.theme("Indigo").background);
        assertEquals(0xFF1F2335, RaiderColors.theme("Indigo").surface);
        assertEquals(0xFFEDEFF7, RaiderColors.theme("Indigo").onSurface);
        assertEquals(0xFF9AA0B8, RaiderColors.theme("Indigo").secondary);
        assertEquals(0xFF7DD3FC, RaiderColors.accentColor("Midnight"));
        assertEquals(0xFFBAE6FD, RaiderColors.theme("Midnight").focus);
        assertEquals(0xFF0B1B2B, RaiderColors.theme("Midnight").background);
        assertEquals(0xFF12293D, RaiderColors.theme("Midnight").surface);
        assertEquals(0xFFF59E0B, RaiderColors.accentColor("Espresso"));
        assertEquals(0xFFFBBF24, RaiderColors.theme("Espresso").focus);
        assertEquals(0xFF171210, RaiderColors.theme("Espresso").background);
        assertEquals(0xFF34D399, RaiderColors.accentColor("Forest"));
        assertEquals(0xFF6EE7B7, RaiderColors.theme("Forest").focus);
        assertEquals(0xFF0F1A14, RaiderColors.theme("Forest").background);
        assertEquals(0xFFE879F9, RaiderColors.accentColor("Plum"));
        assertEquals(0xFFF0ABFC, RaiderColors.theme("Plum").focus);
        assertEquals(0xFF1A1026, RaiderColors.theme("Plum").background);
        assertEquals(0xFFEF4444, RaiderColors.accentColor("Onyx"));
        assertEquals(0xFFF87171, RaiderColors.theme("Onyx").focus);
        assertEquals(0xFF140D0F, RaiderColors.theme("Onyx").background);
        assertEquals(0xFFD4D4D4, RaiderColors.accentColor("True Black"));
        assertEquals(0xFFFFFFFF, RaiderColors.theme("True Black").focus);
        assertEquals(0xFF000000, RaiderColors.theme("True Black").background);
        assertEquals(0xFF141210, RaiderColors.inkOn(0xFFF5B942));
        assertEquals(0xFF141210, RaiderColors.inkOn(0xFFF87171));
        assertEquals(0xFFF6F3EE, RaiderColors.inkOn(0xFF3A241A));
        assertEquals("Indigo", RaiderColors.theme("Marigold").id);
        assertEquals("Indigo", RaiderColors.theme("Porcelain").id);
        assertEquals("Forest", RaiderColors.theme("Teal").id);
        assertEquals("Plum", RaiderColors.theme("Rose").id);
        assertEquals("True Black", RaiderColors.theme("Silver").id);
        assertEquals("Indigo", RaiderColors.theme(null).id);
        assertEquals("Indigo", RaiderColors.theme("nope").id);
        for (RaiderColors.Theme theme : RaiderColors.themes()) assertFalse(theme.light);
        assertFalse(RaiderColors.theme("Indigo").light);
        assertEquals("Indigo + Marigold", RaiderColors.labels()[0]);
        assertEquals("Midnight + Ice", RaiderColors.labels()[1]);
        assertEquals("Espresso + Amber", RaiderColors.labels()[2]);
        assertEquals("Forest + Mint", RaiderColors.labels()[3]);
        assertEquals("Plum + Orchid", RaiderColors.labels()[4]);
        assertEquals("Onyx + Crimson", RaiderColors.labels()[5]);
        assertEquals("True Black + Silver", RaiderColors.labels()[6]);
        for (String label : RaiderColors.labels()) assertFalse(Character.isDigit(label.charAt(0)));
        assertFalse(java.util.Arrays.asList(RaiderColors.labels()).contains("Graphite + Teal"));
        assertFalse(java.util.Arrays.asList(RaiderColors.labels()).contains("Porcelain + Espresso"));
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
        java.util.List<HomeRecs.Item> watch = java.util.Collections.singletonList(new HomeRecs.Item("Show", "content://p", "intent:#Intent;end"));
        java.util.ArrayList<HomeRecs.Item> photos = new java.util.ArrayList<>();
        photos.add(HomeRecs.photoCard("Dock", "Bing", "/tmp/a.jpg"));
        photos.add(HomeRecs.photoCard("  ", "Unsplash", "/tmp/b.jpg"));
        assertEquals("Show", HomeRecs.choose(watch, photos).get(0).title);
        java.util.List<HomeRecs.Item> fallback = HomeRecs.choose(java.util.Collections.emptyList(), photos);
        assertEquals(2, fallback.size());
        assertEquals("Dock", fallback.get(0).title);
        assertEquals("Unsplash", fallback.get(1).title);
        assertEquals(HomeRecs.SCREENSAVER, fallback.get(0).intent);
        java.util.List<HomeRecs.Item> none = HomeRecs.choose(null, java.util.Collections.emptyList());
        assertEquals(1, none.size());
        assertEquals(HomeRecs.PLACEHOLDER_TITLE, none.get(0).title);
        assertEquals(HomeRecs.PHOTOS, none.get(0).intent);
        assertEquals("Photo", HomeRecs.photoTitle(null, " "));
    }

    @Test(timeout = 5000) public void photosMasterDefaultsOn() {
        assertTrue(PhotosMaster.on(false, false));
        assertTrue(PhotosMaster.on(true, true));
        assertFalse(PhotosMaster.on(true, false));
    }

    @Test(timeout = 5000) public void photoGradientsStayInsideThePalette() {
        int[] first = PhotoGradients.pair(0);
        assertEquals(PhotoGradients.PAIRS[0][0], first[0]);
        assertEquals(PhotoGradients.PAIRS[0][1], first[1]);
        int[] wrapped = PhotoGradients.pair(PhotoGradients.PAIRS.length);
        assertEquals(first[0], wrapped[0]);
        int[] negative = PhotoGradients.pair(-1);
        assertEquals(PhotoGradients.PAIRS[PhotoGradients.PAIRS.length - 1][0], negative[0]);
    }

    @Test(timeout = 5000) public void advancedPanelFitsA1080pLeanbackScreen() {
        int viewport = CalendarPanelFit.viewportDp(540);
        assertEquals(426, viewport);
        AdvancedPanelFit.Spec spec = AdvancedPanelFit.forViewport(viewport, 24);
        assertTrue(spec.fits(viewport));
        assertTrue(spec.rowDp >= 32);
        assertEquals(7, AdvancedPanelFit.ROWS);
    }

    @Test(timeout = 5000) public void calendarPanelFitsA1080pLeanbackScreen() {
        int viewport = CalendarPanelFit.viewportDp(540);
        assertEquals(426, viewport);
        CalendarPanelFit.Spec spec = CalendarPanelFit.forViewport(viewport, 24);
        assertTrue(spec.fits(viewport));
        assertTrue(spec.singleDp >= 30);
        assertTrue(spec.twoDp >= 40);
        assertEquals(CalendarPanelFit.ROWS, 10);
    }

    @Test(timeout = 5000) public void freeRamSkipsForegroundSelfSystemAndHome() {
        java.util.ArrayList<RamFree.Proc> procs = new java.util.ArrayList<>();
        procs.add(new RamFree.Proc("com.gathin.tvhome", RamFree.CACHED, 80_000_000L, false, true, true));
        procs.add(new RamFree.Proc("android", RamFree.CACHED, 90_000_000L, true, false, false));
        procs.add(new RamFree.Proc("com.tcl.home", 100, 70_000_000L, false, false, true));
        procs.add(new RamFree.Proc("com.smarttube", 100, 200_000_000L, false, false, false));
        procs.add(new RamFree.Proc("com.cached.light", RamFree.CACHED, 20_000_000L, false, false, false));
        procs.add(new RamFree.Proc("com.cached.heavy", RamFree.CACHED, 180_000_000L, false, false, false));
        procs.add(new RamFree.Proc("com.service.app", RamFree.SERVICE, 150_000_000L, false, false, false));
        java.util.List<String> targets = RamFree.targets(procs);
        assertEquals(java.util.Arrays.asList("com.cached.heavy", "com.cached.light", "com.service.app"), targets);
        procs.add(new RamFree.Proc("com.cached.heavy", 100, 10_000L, false, false, false));
        assertFalse(RamFree.targets(procs).contains("com.cached.heavy"));
        assertTrue(RamFree.targets(null).isEmpty());
    }
}

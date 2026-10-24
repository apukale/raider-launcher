package com.gathin.tvhome;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FontsTest {
    private static final String[] EXPECTED = {
            "Outfit", "Sora", "Sofia Sans", "Nunito", "Work Sans", "Lexend",
            "Atkinson Hyperlegible", "Space Grotesk", "Inter", "Source Serif 4",
            "Roboto", "Poppins", "Roboto Flex", "Manrope"};

    @Test(timeout = 5000)
    public void chooserIsTheCuratedListAndOutfitIsDefault() {
        assertEquals(Arrays.asList(EXPECTED), Arrays.asList(Fonts.NAMES));
        assertEquals(0, Fonts.index(null));
        assertEquals(0, Fonts.index(""));
        assertEquals(0, Fonts.index("Google Sans"));
        assertEquals("Outfit", Fonts.DEFAULT);
        assertEquals("Outfit", Fonts.NAMES[0]);
        assertEquals(0, Fonts.familyIndex(null));
        assertEquals("Outfit", Fonts.NAMES[Fonts.familyIndex(null)]);
        assertEquals("Outfit", Fonts.NAMES[Fonts.familyIndex("")]);
        assertEquals("Outfit", Fonts.NAMES[Fonts.familyIndex("  ")]);
        assertEquals("Outfit", Fonts.NAMES[Fonts.familyIndex("not-a-font")]);
        assertEquals(Fonts.index("Sora"), Fonts.familyIndex("Sora"));
        assertEquals("font_home", Fonts.key(Fonts.HOME));
        assertEquals("font_settings", Fonts.key(Fonts.SETTINGS));
        assertEquals("font_dream", Fonts.key(Fonts.DREAM));
        assertEquals("font_panchang", Fonts.key(Fonts.PANCHANG));
        assertEquals("Outfit", Fonts.NAMES[Fonts.index("outfit")]);
        HashSet<String> banned = new HashSet<>(Arrays.asList("sf pro", "new york", "google sans"));
        for (String name : Fonts.NAMES) assertFalse(banned.contains(name.toLowerCase()));
    }

    @Test(timeout = 5000)
    public void savedNamesRoundTripAndOnlyRobotoIsUnbundled() {
        assertEquals(1, countUnbundled());
        assertFalse(Fonts.bundled(Fonts.index("Roboto")));
        for (int i = 0; i < Fonts.NAMES.length; i++) {
            assertEquals(i, Fonts.index(Fonts.NAMES[i]));
            if (!"Roboto".equals(Fonts.NAMES[i])) assertTrue(Fonts.bundled(i));
        }
    }

    @Test(timeout = 5000)
    public void weightPickFollowsUiRolesAndStaysPut() {
        int[] atkinson = Fonts.weights(Fonts.index("Atkinson Hyperlegible"));
        assertEquals(400, Fonts.pick(atkinson, 200));
        assertEquals(400, Fonts.pick(atkinson, 400));
        assertEquals(700, Fonts.pick(atkinson, 500));
        assertEquals(700, Fonts.pick(atkinson, 700));

        int[] inter = Fonts.weights(Fonts.index("Inter"));
        assertEquals(400, Fonts.pick(inter, 300));
        assertEquals(400, Fonts.pick(inter, 400));
        assertEquals(500, Fonts.pick(inter, 500));
        assertEquals(600, Fonts.pick(inter, 600));
        assertEquals(700, Fonts.pick(inter, 900));

        int[] sora = Fonts.weights(Fonts.index("Sora"));
        assertEquals(300, Fonts.pick(sora, 100));
        assertEquals(200, Fonts.pick(sora, 200));
        assertEquals(300, Fonts.pick(sora, 300));
        assertEquals(400, Fonts.pick(sora, 470));
        assertEquals(500, Fonts.pick(sora, 500));
        assertEquals(700, Fonts.pick(sora, 700));

        int[] space = Fonts.weights(Fonts.index("Space Grotesk"));
        assertEquals(300, Fonts.pick(space, 200));

        for (String name : Fonts.NAMES) {
            int[] weights = Fonts.weights(Fonts.index(name));
            assertTrue(weights.length >= 2);
            for (int want : new int[]{100, 200, 300, 400, 500, 600, 700, 900}) {
                int got = Fonts.pick(weights, want);
                assertEquals(name, got, Fonts.pick(weights, got));
            }
        }
    }

    private static int countUnbundled() {
        int n = 0;
        for (int i = 0; i < Fonts.NAMES.length; i++) if (!Fonts.bundled(i)) n++;
        return n;
    }
}

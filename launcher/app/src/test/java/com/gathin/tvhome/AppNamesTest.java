package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class AppNamesTest {
    @Test(timeout = 5000)
    public void customNameReplacesTheSystemLabel() {
        assertEquals("JioHotstar", AppNames.display("JioHotstar Morphe", "JioHotstar"));
        assertEquals("JioHotstar", AppNames.display("JioHotstar Morphe", "  JioHotstar  "));
        assertEquals("JioHotstar (JioHotstar Morphe)", AppNames.listLine("JioHotstar Morphe", "JioHotstar"));
    }

    @Test(timeout = 5000)
    public void blankOrMatchingNameKeepsTheSystemLabel() {
        assertEquals("JioHotstar Morphe", AppNames.display("JioHotstar Morphe", null));
        assertEquals("JioHotstar Morphe", AppNames.display("JioHotstar Morphe", ""));
        assertEquals("JioHotstar Morphe", AppNames.display("JioHotstar Morphe", "   "));
        assertEquals("JioHotstar Morphe", AppNames.display("JioHotstar Morphe", "JioHotstar Morphe"));
        assertEquals("JioHotstar Morphe", AppNames.listLine("JioHotstar Morphe", ""));
        assertEquals("JioHotstar Morphe", AppNames.listLine("JioHotstar Morphe", "JioHotstar Morphe"));
        assertEquals("", AppNames.storedValue("JioHotstar Morphe", "JioHotstar Morphe"));
        assertEquals("", AppNames.storedValue("  ", "JioHotstar Morphe"));
        assertEquals("", AppNames.storedValue(null, "JioHotstar Morphe"));
        assertEquals("JioHotstar", AppNames.storedValue("JioHotstar", "JioHotstar Morphe"));
    }

    @Test(timeout = 5000)
    public void storedNameDropsBidiAndStaysWithinTheTileCap() {
        assertEquals("Jio Hotstar", AppNames.normalize("Jio\u202EHotstar"));
        assertEquals("Jio Hotstar", AppNames.display("System", "Jio\u202EHotstar"));
        assertEquals("Jio Hotstar", AppNames.normalize("Jio\nHotstar"));
        assertEquals("", AppNames.normalize("\u202E\n\u0000"));
        assertEquals("", AppNames.storedValue("\u202E", "JioHotstar Morphe"));
        char[] big = new char[50];
        java.util.Arrays.fill(big, 'a');
        String capped = AppNames.storedValue(new String(big), "System");
        assertEquals(40, capped.length());
        assertEquals(capped, AppNames.display("System", capped));
    }
}

package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class RaiderThemeTest {
    @Test(timeout = 5000) public void accentNamesMapToPaletteAndFallback() {
        assertEquals(0xFFF2A93B, RaiderColors.accentColor("Marigold"));
        assertEquals(0xFF2EC4B6, RaiderColors.accentColor("Teal"));
        assertEquals(0xFFF06A8A, RaiderColors.accentColor("Rose"));
        assertEquals(0xFFC9CED6, RaiderColors.accentColor("Silver"));
        assertEquals(0xFFF2A93B, RaiderColors.accentColor("unknown"));
        assertEquals(0xFFF2A93B, RaiderColors.accentColor(""));
        assertEquals(0xFFF2A93B, RaiderColors.accentColor(null));
    }
}

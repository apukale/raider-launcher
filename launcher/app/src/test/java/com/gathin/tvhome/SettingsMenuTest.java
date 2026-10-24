package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class SettingsMenuTest {
    @Test(timeout = 5000)
    public void railHasTheApprovedNineLabels() {
        assertArrayEquals(new String[] {
                "Home Layout",
                "Appearance",
                "Screensaver",
                "Photos",
                "Calendar & Clocks",
                "Weather",
                "Night Mode",
                "Advanced",
                "About Raider Launcher"
        }, SettingsMenu.LABELS);
    }
}

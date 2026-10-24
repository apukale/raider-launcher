package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class LauncherPrefsTest {
    @Test(timeout=5000) public void iconsOnlyIsDefault() {
        assertFalse(LauncherPrefs.showPillLabels(false, true));
        assertTrue(LauncherPrefs.showPillLabels(true, true));
    }

    @Test(timeout=5000) public void hinduMasterGatesPanchangOnly() {
        assertTrue(LauncherPrefs.hinduOn(false, false));
        assertFalse(LauncherPrefs.hinduOn(true, false));
        assertTrue(LauncherPrefs.panchangOnHome(true, true));
        assertFalse(LauncherPrefs.panchangOnHome(false, true));
        assertFalse(LauncherPrefs.panchangOnDream(true, false));
        assertFalse(LauncherPrefs.panchangOnDream(false, true));
    }
}

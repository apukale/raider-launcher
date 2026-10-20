package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class LauncherPrefsTest {
    @Test(timeout=5000) public void iconsOnlyIsDefault() {
        assertFalse(LauncherPrefs.showPillLabels(false, true));
        assertTrue(LauncherPrefs.showPillLabels(true, true));
    }
}

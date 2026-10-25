package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SettingsPickerTest {
    @Test(timeout = 5000)
    public void panelStaysNarrowerThanAStockTvDialog() {
        // 1080p at xhdpi. A leanback AlertDialog uses most of the screen; this card does not.
        int width = SettingsPicker.panelWidthPx(1920, 2f);
        assertEquals(730, width);
        assertTrue(width <= (int) (1920 * 0.42f));
        assertTrue(width < 1920 / 2);
    }

    @Test(timeout = 5000)
    public void fontListShowsMoreRowsThanTheStockDialogAndStillScrolls() {
        // The stock Home font dialog showed about 6 of 14 faces. The card fits 8 and scrolls the rest.
        assertEquals(8, SettingsPicker.visibleRows(14, 1080, 2f));
        assertEquals(10, SettingsPicker.visibleRows(14, 1080, 1.5f));
        assertEquals(4, SettingsPicker.visibleRows(4, 1080, 2f));
        assertEquals(1, SettingsPicker.visibleRows(1, 800, 1f));
        assertEquals(0, SettingsPicker.visibleRows(0, 1080, 2f));
    }
}

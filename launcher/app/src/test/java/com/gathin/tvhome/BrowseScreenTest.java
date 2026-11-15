package com.gathin.tvhome;

import android.view.KeyEvent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BrowseScreenTest {
    @Test(timeout = 5000)
    public void postersFitBetweenFiltersAndTheBottomOnA960x540Screen() {
        BrowseLayout.Spec spec = BrowseLayout.at(1920, 1080, 2f);
        assertTrue(spec.posterInRange);
        assertEquals(280, spec.posterWidthPx);
        assertEquals(420, spec.posterHeightPx);
        assertEquals(spec.filterHeightPx, spec.gridTopPx);
        assertTrue(spec.oneCellFits);
        assertTrue(spec.separated);
        assertTrue(spec.gridTopPx + spec.cellHeightPx <= 1080);
        assertTrue(spec.columns >= 4);
        assertTrue(spec.columns <= 6);
    }

    @Test(timeout = 5000)
    public void filtersAndGridMoveWithoutLeavingTheScreen() {
        BrowseFocus.Step intoGrid = BrowseFocus.filter(3, 1, 6, 4, KeyEvent.KEYCODE_DPAD_DOWN);
        assertEquals(BrowseFocus.GRID, intoGrid.zone);
        assertFalse(intoGrid.stay);
        BrowseFocus.Step back = BrowseFocus.grid(0, 5, 20, 4, KeyEvent.KEYCODE_DPAD_UP);
        assertEquals(BrowseFocus.FILTER, back.zone);
        assertEquals(3, back.row);
        BrowseFocus.Step downInGrid = BrowseFocus.grid(2, 5, 20, 4, KeyEvent.KEYCODE_DPAD_DOWN);
        assertEquals(7, downInGrid.index);
        BrowseFocus.Step stayLeft = BrowseFocus.grid(5, 5, 20, 4, KeyEvent.KEYCODE_DPAD_LEFT);
        assertTrue(stayLeft.stay);
        BrowseFocus.Step stayRight = BrowseFocus.grid(4, 5, 20, 4, KeyEvent.KEYCODE_DPAD_RIGHT);
        assertTrue(stayRight.stay);
        BrowseFocus.Step stayBottom = BrowseFocus.grid(18, 5, 20, 4, KeyEvent.KEYCODE_DPAD_DOWN);
        assertTrue(stayBottom.stay);
        assertTrue(BrowseFocus.filter(0, 0, 3, 4, KeyEvent.KEYCODE_DPAD_UP).stay);
        assertTrue(BrowseFocus.filter(1, 0, 4, 4, KeyEvent.KEYCODE_DPAD_LEFT).stay);
        assertEquals(1, BrowseFocus.filter(1, 0, 4, 4, KeyEvent.KEYCODE_DPAD_RIGHT).index);
        assertEquals("Movie \u00B7 2026 \u00B7 8.0 TMDB", TmdbModels.cardMeta(new TmdbModels.Title(
                TmdbModels.MOVIE, 1, "Jailer", "", "/a.jpg", "", "2026-01-01", 8.0, 1, 0,
                null, null, null, null, "", "te", null)));
    }
}

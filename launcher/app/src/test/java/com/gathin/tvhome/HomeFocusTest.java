package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HomeFocusTest {
    @Test(timeout = 5000)
    public void downMovesThroughEveryVisibleZone() {
        assertEquals(HomeFocus.HERO, HomeFocus.next(HomeFocus.TOP, false, true, true, true));
        assertEquals(HomeFocus.TRENDING, HomeFocus.next(HomeFocus.HERO, false, true, true, true));
        assertEquals(HomeFocus.TOP10, HomeFocus.next(HomeFocus.TRENDING, false, true, true, true));
        assertEquals(HomeFocus.DOCK, HomeFocus.next(HomeFocus.TOP10, false, true, true, true));
        assertEquals(-1, HomeFocus.next(HomeFocus.DOCK, false, true, true, true));
    }

    @Test(timeout = 5000)
    public void upMovesBackAndSkipsHiddenRows() {
        assertEquals(HomeFocus.TOP10, HomeFocus.next(HomeFocus.DOCK, true, true, true, true));
        assertEquals(HomeFocus.TRENDING, HomeFocus.next(HomeFocus.TOP10, true, true, true, true));
        assertEquals(HomeFocus.HERO, HomeFocus.next(HomeFocus.TRENDING, true, true, true, true));
        assertEquals(HomeFocus.TOP, HomeFocus.next(HomeFocus.HERO, true, true, true, true));
        assertEquals(-1, HomeFocus.next(HomeFocus.TOP, true, true, true, true));
        assertEquals(HomeFocus.TOP10, HomeFocus.next(HomeFocus.HERO, false, true, false, true));
        assertEquals(HomeFocus.TOP, HomeFocus.next(HomeFocus.TRENDING, true, false, true, true));
        assertEquals(HomeFocus.DOCK, HomeFocus.next(HomeFocus.HERO, false, true, false, false));
    }
}

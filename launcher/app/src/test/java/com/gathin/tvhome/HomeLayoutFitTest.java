package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class HomeLayoutFitTest {
    @Test(timeout = 5000)
    public void homeLayoutFitsA1080pLeanbackScreenWithoutScrolling() {
        int viewport = CalendarPanelFit.viewportDp(540);
        HomeLayoutFit.Spec spec = HomeLayoutFit.forViewport(viewport, 24);
        assertTrue(spec.fits(viewport));
        assertTrue(spec.totalDp() <= viewport - 4);
        assertTrue(spec.moduleDp >= 36);
        assertTrue(spec.detailDp >= 42);
        assertTrue(spec.rowDp >= 32);
        assertTrue(spec.bandDp >= 32);
        assertTrue(spec.gapDp >= 1);
    }
}

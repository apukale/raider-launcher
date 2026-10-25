package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class HomeModulesTest {
    @Test(timeout = 5000)
    public void gridMovesInsideAndLeavesAtTheEdges() {
        assertEquals(HomeModules.CLOCKS, HomeModules.neighbor(HomeModules.WEATHER, HomeModules.RIGHT));
        assertEquals(HomeModules.PANCHANG, HomeModules.neighbor(HomeModules.WEATHER, HomeModules.DOWN));
        assertEquals(HomeModules.EXIT_LEFT, HomeModules.neighbor(HomeModules.WEATHER, HomeModules.LEFT));
        assertEquals(HomeModules.EXIT_UP, HomeModules.neighbor(HomeModules.WEATHER, HomeModules.UP));
        assertEquals(HomeModules.STAY, HomeModules.neighbor(HomeModules.CLOCKS, HomeModules.RIGHT));
        assertEquals(HomeModules.SCREENSAVER, HomeModules.neighbor(HomeModules.CLOCKS, HomeModules.DOWN));
        assertEquals(HomeModules.WEATHER, HomeModules.neighbor(HomeModules.PANCHANG, HomeModules.UP));
        assertEquals(HomeModules.EXIT_DOWN, HomeModules.neighbor(HomeModules.PANCHANG, HomeModules.DOWN));
        assertEquals(HomeModules.EXIT_DOWN, HomeModules.neighbor(HomeModules.SCREENSAVER, HomeModules.DOWN));
        assertEquals(HomeModules.PANCHANG, HomeModules.neighbor(HomeModules.SCREENSAVER, HomeModules.LEFT));
        assertEquals(HomeModules.STAY, HomeModules.neighbor(HomeModules.SCREENSAVER, HomeModules.RIGHT));
        assertEquals(HomeModules.STAY, HomeModules.neighbor(-1, HomeModules.DOWN));
    }
}

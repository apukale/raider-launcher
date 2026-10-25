package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class WeatherScreenLayoutTest {
    @Test(timeout = 5000)
    public void todayColumnFitsA1080pTvWithRoomUnderTheTemperatures() {
        WeatherScreenLayout.Spec spec = WeatherScreenLayout.fit(540);
        assertTrue(spec.todayFits(540));
        assertTrue(spec.body(540) - spec.todayColumn() >= 8);
        assertTrue(spec.bottom >= 32);
        assertTrue(spec.nowTempSp >= 40);
        assertTrue(spec.hourTempSp >= 18);
        assertTrue(spec.dayRow >= 44);
        assertTrue(spec.highLowWidth >= 120);
        assertTrue(spec.mapEdge >= 40);
    }

    @Test(timeout = 5000)
    public void shorterTvStillKeepsTheHourlyTemperatureOnScreen() {
        WeatherScreenLayout.Spec spec = WeatherScreenLayout.fit(480);
        assertTrue(spec.todayFits(480));
        assertTrue(spec.bottom >= 27);
        assertTrue(spec.nowTempSp >= 40);
        assertTrue(spec.hourTempSp >= 18);
        assertTrue(spec.dayRow >= 44);
    }
}

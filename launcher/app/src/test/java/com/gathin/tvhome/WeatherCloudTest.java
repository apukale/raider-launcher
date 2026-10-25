package com.gathin.tvhome;

import java.net.URL;
import org.junit.Test;
import static org.junit.Assert.*;

public class WeatherCloudTest {
    private static final String KEY = "0123456789abcdef0123456789abcdef";

    @Test(timeout = 5000) public void tileUrlIsCloudsNewOnTheFreeMapHost() {
        String url = CloudCover.tileUrl(6, 45, 28, KEY);
        assertEquals("https://tile.openweathermap.org/map/clouds_new/6/45/28.png?appid=" + KEY, url);
        assertTrue(CloudCover.allowsTileUrl(url));
        assertFalse(url.contains("precipitation_new"));
        assertFalse(url.contains("rainviewer"));
    }

    @Test(timeout = 5000) public void missingOrUnsafeKeyBuildsNoUrl() {
        assertFalse(CloudCover.keyUsable(null));
        assertFalse(CloudCover.keyUsable(""));
        assertFalse(CloudCover.keyUsable("   "));
        assertFalse(CloudCover.keyUsable("shortkey"));
        assertFalse(CloudCover.keyUsable("has space in key!!"));
        assertFalse(CloudCover.keyUsable(KEY + "&x=1"));
        assertNull(CloudCover.tileUrl(6, 1, 2, null));
        assertNull(CloudCover.tileUrl(6, 1, 2, ""));
        assertNull(CloudCover.tileUrl(6, 1, 2, "changeme"));
        assertFalse(CloudCover.allowsTileUrl(null));
        assertFalse(CloudCover.allowsTileUrl("https://tile.openweathermap.org/map/precipitation_new/6/1/2.png?appid=" + KEY));
        assertFalse(CloudCover.allowsTileUrl("http://tile.openweathermap.org/map/clouds_new/6/1/2.png?appid=" + KEY));
        assertFalse(CloudCover.allowsTileUrl("https://tile.openweathermap.org:444/map/clouds_new/6/1/2.png?appid=" + KEY));
        assertFalse(CloudCover.allowsTileUrl("https://api.rainviewer.com/public/weather-maps.json"));
        assertFalse(CloudCover.allowsTileUrl("https://evil.example/map/clouds_new/6/1/2.png?appid=" + KEY));
        assertFalse(CloudCover.allowsTileUrl("https://user:pw@tile.openweathermap.org/map/clouds_new/6/1/2.png?appid=" + KEY));
    }

    @Test(timeout = 5000) public void indiaFrameAndZoomMatchTheWeatherMap() {
        assertEquals(35.5, CloudCover.INDIA_NORTH, 0.0001);
        assertEquals(8.0, CloudCover.INDIA_SOUTH, 0.0001);
        assertEquals(68.1, CloudCover.INDIA_WEST, 0.0001);
        assertEquals(97.4, CloudCover.INDIA_EAST, 0.0001);
        assertArrayEquals(new int[] {44, 49, 25, 30}, CloudCover.tileBox(6));
        assertEquals(6, CloudCover.pickZoom(700));
        assertEquals(3, CloudCover.pickZoom(100));
        assertEquals(8, CloudCover.pickZoom(6000));
    }

    @Test(timeout = 5000) public void trimmedKeyIsAcceptedAndNotLoggedIntoAnotherHost() throws Exception {
        String url = CloudCover.tileUrl(6, 44, 25, "  " + KEY + "  ");
        assertNotNull(url);
        assertTrue(url.endsWith("appid=" + KEY));
        URL parsed = new URL(url);
        assertEquals(CloudCover.HOST, parsed.getHost());
        assertEquals("/map/clouds_new/6/44/25.png", parsed.getPath());
    }
}

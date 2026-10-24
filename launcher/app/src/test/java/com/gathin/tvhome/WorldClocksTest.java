package com.gathin.tvhome;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class WorldClocksTest {
    private static final long NOON_UTC = Instant.parse("2026-01-15T12:00:00Z").toEpochMilli();

    @Test(timeout = 5000)
    public void indiaTimeIsFixed() {
        assertEquals("5:30 PM", WorldClocks.formatTime(WorldClocks.INDIA_ZONE, NOON_UTC));
        assertEquals("12:00 PM", WorldClocks.formatTime("Europe/London", NOON_UTC));
        assertFalse(WorldClocks.validZone("Not/AZone"));
        assertTrue(WorldClocks.validZone("Asia/Kolkata"));
    }

    @Test(timeout = 5000)
    public void homeDetailAlwaysIncludesIndiaAndSkipsBlankCities() {
        String[] names = {"London", "", "  ", "Tokyo"};
        String[] zones = {"Europe/London", "Europe/Paris", "America/New_York", "Asia/Tokyo"};
        String line = WorldClocks.join(names, zones, NOON_UTC, "  \u00B7  ");
        assertEquals("India 5:30 PM  \u00B7  London 12:00 PM  \u00B7  Tokyo 9:00 PM", line);
    }

    @Test(timeout = 5000)
    public void dreamDetailIsOneCityPerLine() {
        String text = WorldClocks.dreamDetail(prefs("London", "Europe/London"), NOON_UTC);
        assertEquals("India 5:30 PM\nLondon 12:00 PM", text);
    }

    @Test(timeout = 5000)
    public void typedCityMatchesTheBuiltInList() {
        List<WorldClocks.Place> london = WorldClocks.matchLocal("lon");
        assertFalse(london.isEmpty());
        assertEquals("London", london.get(0).name);
        assertEquals("Europe/London", london.get(0).zone);
        assertTrue(WorldClocks.matchLocal("x").isEmpty());
        List<WorldClocks.Place> zone = WorldClocks.matchLocal("Pacific/Auckland");
        assertEquals("Pacific/Auckland", zone.get(0).zone);
    }

    private static java.util.function.BiFunction<String, String, String> prefs(String name, String zone) {
        Map<String, String> values = new HashMap<>();
        values.put(WorldClocks.nameKey(0), name);
        values.put(WorldClocks.zoneKey(0), zone);
        return (key, fallback) -> values.getOrDefault(key, fallback);
    }
}

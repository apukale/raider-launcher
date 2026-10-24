package com.gathin.tvhome;

import java.time.Instant;
import java.util.Arrays;
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
    public void blankNameUsesTheZoneIdAndSkipsAnEmptyZone() {
        String[] names = {"London", "", "  ", "Tokyo"};
        String[] zones = {"Europe/London", "Europe/Paris", "", "Asia/Tokyo"};
        String line = WorldClocks.join(names, zones, NOON_UTC, "  \u00B7  ");
        assertEquals("India 5:30 PM  \u00B7  London 12:00 PM  \u00B7  Europe/Paris 1:00 PM  \u00B7  Tokyo 9:00 PM", line);
    }

    @Test(timeout = 5000)
    public void renameKeepsTheZoneAndEntriesIncludeLocal() {
        assertEquals("NYC", WorldClocks.labelFor("NYC", "America/New_York"));
        assertEquals("America/New_York", WorldClocks.labelFor("  ", "America/New_York"));
        assertEquals("America/New_York", WorldClocks.labelFor(null, " America/New_York "));
        List<WorldClocks.Clock> clocks = WorldClocks.entries(prefs("NYC", "America/New_York"));
        assertEquals(2, clocks.size());
        assertEquals("India", clocks.get(0).label);
        assertEquals(WorldClocks.INDIA_ZONE, clocks.get(0).zone);
        assertEquals("NYC", clocks.get(1).label);
        assertEquals("America/New_York", clocks.get(1).zone);
        assertEquals("7:00 AM", WorldClocks.formatTime(clocks.get(1).zone, NOON_UTC));
    }

    @Test(timeout = 5000)
    public void filterZonesMatchesTheWholeIanaList() {
        String[] ids = {"Pacific/Auckland", "America/New_York", "Europe/London", "Asia/Kolkata"};
        List<String> all = WorldClocks.filterZones(ids, " ");
        assertEquals(Arrays.asList("America/New_York", "Asia/Kolkata", "Europe/London", "Pacific/Auckland"), all);
        assertEquals(Arrays.asList("America/New_York"), WorldClocks.filterZones(ids, "new york"));
        assertEquals("America/New_York", WorldClocks.filterZones(ids, "NEW_YORK").get(0));
        assertEquals("America/New_York", WorldClocks.filterZones(ids, "america/new").get(0));
        assertTrue(WorldClocks.filterZones(ids, "zzz").isEmpty());
        assertTrue(WorldClocks.filterZones(null, "a").isEmpty());
        String[] zones = WorldClocks.zones();
        assertTrue(zones.length > 100);
        assertTrue(Arrays.asList(zones).contains("America/New_York"));
        assertTrue(Arrays.asList(zones).contains("Asia/Kolkata"));
        for (int i = 1; i < zones.length; i++) assertTrue(zones[i - 1].compareToIgnoreCase(zones[i]) <= 0);
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

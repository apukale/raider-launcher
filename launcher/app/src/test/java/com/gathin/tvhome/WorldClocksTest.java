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
        assertEquals(5, clocks.size());
        assertEquals("India", clocks.get(0).label);
        assertEquals(WorldClocks.INDIA_ZONE, clocks.get(0).zone);
        assertEquals("NYC", clocks.get(1).label);
        assertEquals("America/New_York", clocks.get(1).zone);
        assertEquals("7:00 AM", WorldClocks.formatTime(clocks.get(1).zone, NOON_UTC));
        assertEquals("New York", clocks.get(2).label);
        assertEquals("London", clocks.get(3).label);
        assertEquals("Dubai", clocks.get(4).label);
    }

    @Test(timeout = 5000)
    public void unsetSlotsUseFactoryCitiesAndKeepASavedOne() {
        List<WorldClocks.Clock> clocks = WorldClocks.entries((key, fallback) -> fallback);
        assertEquals(5, clocks.size());
        assertEquals("Los Angeles", clocks.get(1).label);
        assertEquals("America/Los_Angeles", clocks.get(1).zone);
        assertEquals("4:00 AM", WorldClocks.formatTime(clocks.get(1).zone, NOON_UTC));
        assertEquals("New York", clocks.get(2).label);
        assertEquals("America/New_York", clocks.get(2).zone);
        assertEquals("London", clocks.get(3).label);
        assertEquals("Europe/London", clocks.get(3).zone);
        assertEquals("Dubai", clocks.get(4).label);
        assertEquals("Asia/Dubai", clocks.get(4).zone);
        assertEquals("4:00 PM", WorldClocks.formatTime(clocks.get(4).zone, NOON_UTC));
        java.util.Map<String, String> saved = new java.util.HashMap<>();
        saved.put(WorldClocks.zoneKey(2), "Europe/Paris");
        List<WorldClocks.Clock> mixed = WorldClocks.entries((key, fallback) -> saved.getOrDefault(key, fallback));
        assertEquals("Los Angeles", mixed.get(1).label);
        assertEquals("Europe/Paris", mixed.get(3).zone);
        assertEquals("Europe/Paris", mixed.get(3).label);
        assertEquals("Dubai", mixed.get(4).label);
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
        assertEquals("London 12:00 PM\nNew York 7:00 AM\nLondon 12:00 PM\nDubai 4:00 PM", text);
        assertFalse(text.contains("India"));
        List<WorldClocks.Clock> cities = WorldClocks.cities(prefs("London", "Europe/London"));
        assertEquals(4, cities.size());
        assertEquals("London", cities.get(0).label);
        assertFalse(cities.get(0).label.equals(WorldClocks.INDIA_LABEL) && cities.get(0).zone.equals(WorldClocks.INDIA_ZONE));
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

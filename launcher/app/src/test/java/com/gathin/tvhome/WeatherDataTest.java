package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class WeatherDataTest {
    private static final String SAMPLE="{\"current\":{\"time\":\"2026-10-20T12:00\",\"temperature_2m\":31.6,\"weather_code\":0,\"apparent_temperature\":34.4,\"relative_humidity_2m\":39},\"daily\":{\"time\":[\"2026-10-20\"],\"temperature_2m_max\":[32.6],\"temperature_2m_min\":[22.8],\"precipitation_probability_max\":[2]}}";
    @Test(timeout=5000) public void parsesSampleAndRoundsTemperatures(){WeatherData d=WeatherData.parse(SAMPLE);assertEquals(33,d.high);assertEquals(23,d.low);assertEquals(32,d.temperature);assertEquals("clear",WeatherData.iconGroup(d.code));}
    @Test(timeout=5000) public void mapsWmoGroups(){assertEquals("cloud",WeatherData.iconGroup(3));assertEquals("fog",WeatherData.iconGroup(45));assertEquals("drizzle",WeatherData.iconGroup(56));assertEquals("rain",WeatherData.iconGroup(82));assertEquals("snow",WeatherData.iconGroup(86));assertEquals("thunder",WeatherData.iconGroup(95));}
    @Test(timeout=5000) public void rainChanceMatchesHome(){assertEquals("",WeatherData.rainSuffix(0));assertEquals("",WeatherData.rainSuffix(39));assertEquals(" \u00B7 40%",WeatherData.rainSuffix(40));assertEquals(" \u00B7 80%",WeatherData.rainSuffix(80));}
    @Test(timeout=5000) public void weatherHostAllowListIsStrict(){assertTrue(WeatherData.allowsUrl("https://api.open-meteo.com/v1/forecast"));assertFalse(WeatherData.allowsUrl("http://api.open-meteo.com/v1/forecast"));assertFalse(WeatherData.allowsUrl("https://example.com/v1/forecast"));assertFalse(WeatherData.allowsUrl("https://api.open-meteo.com:444/v1/forecast"));}
}

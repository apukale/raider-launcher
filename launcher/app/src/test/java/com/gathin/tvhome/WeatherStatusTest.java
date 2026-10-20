package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class WeatherStatusTest {
    @Test(timeout=5000) public void formatsUpdatedFailureAndNeverStates() {
        assertEquals("Updated 12:40 PM \u00B7 32\u00B0", WeatherStatus.format(true, "", "12:40 PM", 32));
        assertEquals("Last try failed: timeout", WeatherStatus.format(true, "timeout", "", 0));
        assertEquals("Not fetched yet", WeatherStatus.format(false, "", "", 0));
    }
}

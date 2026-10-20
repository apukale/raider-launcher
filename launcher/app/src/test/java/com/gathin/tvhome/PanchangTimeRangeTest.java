package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class PanchangTimeRangeTest {
    @Test(timeout = 5000) public void omitsRepeatedMeridiem() {
        assertEquals("2:56 - 4:24 PM", TimeRangeFormatter.format("2:56 PM", "4:24 PM"));
        assertEquals("11:50 AM - 12:20 PM", TimeRangeFormatter.format("11:50 AM", "12:20 PM"));
    }
}

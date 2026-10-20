package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class PhotoContentFilterTest {
    @Test(timeout=5000) public void filtersPeopleAndPhoneWords() {
        assertTrue(PhotoContentFilter.isPeopleLike("a man holding a phone"));
        assertTrue(PhotoContentFilter.isPeopleLike("Woman in red dress"));
        assertFalse(PhotoContentFilter.isPeopleLike("snowy mountain landscape"));
        assertFalse(PhotoContentFilter.isPeopleLike("Manhattan skyline at night"));
        assertFalse(PhotoContentFilter.isPeopleLike("handrail on a bridge"));
        assertFalse(PhotoContentFilter.isPeopleLike((String) null, "", "  "));
    }
}

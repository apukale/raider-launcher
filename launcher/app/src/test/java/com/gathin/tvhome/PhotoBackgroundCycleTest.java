package com.gathin.tvhome;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class PhotoBackgroundCycleTest {
    @Test(timeout=5000) public void wrapsAndRecoversMissingRememberedFile() {
        assertEquals("a.jpg", PhotoBackgroundCycle.nextFile(Arrays.asList("a.jpg", "b.jpg"), "b.jpg"));
        assertEquals("a.jpg", PhotoBackgroundCycle.nextFile(Arrays.asList("a.jpg", "b.jpg"), "removed.jpg"));
    }
    @Test(timeout=5000) public void emptyListReturnsNoSelection() {
        assertEquals("", PhotoBackgroundCycle.nextFile(Collections.emptyList(), "old.jpg"));
    }
}

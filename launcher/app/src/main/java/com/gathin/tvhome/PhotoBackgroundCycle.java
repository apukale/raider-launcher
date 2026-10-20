package com.gathin.tvhome;

import java.util.List;

/** Pure filename cycling for the Panchang photo backdrop. */
final class PhotoBackgroundCycle {
    private PhotoBackgroundCycle() {}
    static String nextFile(List<String> files, String current) {
        if (files == null || files.isEmpty()) return "";
        int index = files.indexOf(current);
        return files.get(index < 0 ? 0 : (index + 1) % files.size());
    }
}

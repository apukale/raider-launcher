package com.gathin.tvhome;

/** Pure Java formatter for ranges with a shared AM or PM marker. */
final class TimeRangeFormatter {
    private TimeRangeFormatter() { }
    static String format(String start, String end) {
        String first=start.trim(), second=end.trim();
        int firstSpace=first.lastIndexOf(' '), secondSpace=second.lastIndexOf(' ');
        if(firstSpace>0&&secondSpace>0&&first.substring(firstSpace+1).equals(second.substring(secondSpace+1)))
            return first.substring(0,firstSpace)+" - "+second;
        return first+" - "+second;
    }
}

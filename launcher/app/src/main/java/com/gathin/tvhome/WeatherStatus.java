package com.gathin.tvhome;

/** Pure formatting for weather status shown in Launcher Settings. */
final class WeatherStatus {
    private WeatherStatus() {}
    static String format(boolean fetched, String error, String time, int temperature) {
        if (error != null && !error.isEmpty()) return "Last try failed: " + error;
        if (!fetched) return "Not fetched yet";
        return "Updated " + time + " \u00B7 " + temperature + "\u00B0";
    }
}

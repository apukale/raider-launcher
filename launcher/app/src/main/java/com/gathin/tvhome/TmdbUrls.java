package com.gathin.tvhome;

import java.net.URLEncoder;

/** TMDB paths. Monetization is flatrate, free, and ads only. */
final class TmdbUrls {
    private TmdbUrls() {}

    static String discover(String media, String providers, String language, String dateGte, int page) {
        boolean movie = TmdbModels.MOVIE.equals(media);
        String dateParam = movie ? "primary_release_date.gte" : "air_date.gte";
        String sort = movie ? "primary_release_date.desc" : "first_air_date.desc";
        return "https://api.themoviedb.org/3/discover/" + media
                + "?language=en-IN&region=IN&watch_region=IN&include_adult=false&include_video=false"
                + "&page=" + Math.max(1, page)
                + "&sort_by=" + sort
                + "&with_original_language=" + enc(language == null ? "" : language)
                + "&" + dateParam + "=" + enc(dateGte == null ? "" : dateGte)
                + "&with_watch_monetization_types=" + enc("flatrate|free|ads")
                + "&with_watch_providers=" + enc(providers == null ? "" : providers);
    }

    static String trending(boolean day) {
        return "https://api.themoviedb.org/3/trending/all/" + (day ? "day" : "week") + "?language=en-IN";
    }

    static String watchProviders(String media, int id) {
        return "https://api.themoviedb.org/3/" + media + "/" + id + "/watch/providers";
    }

    private static String enc(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}

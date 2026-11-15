package com.gathin.tvhome;

import java.net.URLEncoder;

/** TMDB paths. Monetization is flatrate, free, and ads only. */
final class TmdbUrls {
    private TmdbUrls() {}

    static String discover(String media, String providers, String language, String dateGte, int page) {
        return discover(media, providers, language, dateGte, page, null);
    }

    static String discover(String media, String providers, String language, String dateGte, int page, String sort) {
        boolean movie = TmdbModels.MOVIE.equals(media);
        String dateParam = movie ? "primary_release_date.gte" : "air_date.gte";
        String chosen = sort == null || sort.isEmpty()
                ? (movie ? "primary_release_date.desc" : "first_air_date.desc") : sort;
        StringBuilder url = new StringBuilder("https://api.themoviedb.org/3/discover/").append(media)
                .append("?language=en-IN&region=IN&watch_region=IN&include_adult=false&include_video=false")
                .append("&page=").append(Math.max(1, page))
                .append("&sort_by=").append(chosen)
                .append("&with_original_language=").append(enc(language == null ? "" : language));
        if (dateGte != null && !dateGte.isEmpty()) url.append('&').append(dateParam).append('=').append(enc(dateGte));
        if ("vote_average.desc".equals(chosen)) url.append("&vote_count.gte=50");
        url.append("&with_watch_monetization_types=").append(enc("flatrate|free|ads"))
                .append("&with_watch_providers=").append(enc(providers == null ? "" : providers));
        return url.toString();
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

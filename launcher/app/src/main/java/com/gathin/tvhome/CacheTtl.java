package com.gathin.tvhome;

/**
 * How long a TMDB response may be reused. Lists sit in the middle of the 3–6 hour window.
 * A stored time of 0, a non-positive ttl, or a clock that moved backwards is never fresh.
 */
final class CacheTtl {
    static final long LISTS_MIN_MS = 3L * 60 * 60 * 1000;
    static final long LISTS_MAX_MS = 6L * 60 * 60 * 1000;
    /** Chosen list lifetime: inside the 3–6 hour window. */
    static final long LISTS_MS = 4L * 60 * 60 * 1000;
    static final long DETAILS_MS = 24L * 60 * 60 * 1000;
    static final long EXTERNAL_MS = 30L * 24 * 60 * 60 * 1000;
    static final long IMAGES_MS = 7L * 24 * 60 * 60 * 1000;

    private CacheTtl() {}

    static boolean fresh(long storedAt, long now, long ttl) {
        if (ttl <= 0 || storedAt <= 0 || now < storedAt) return false;
        return now - storedAt < ttl;
    }
}

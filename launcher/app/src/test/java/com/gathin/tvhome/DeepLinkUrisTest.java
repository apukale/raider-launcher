package com.gathin.tvhome;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class DeepLinkUrisTest {
    @Test(timeout = 5000)
    public void buildsTheUrisCheckedOnTheTv() {
        assertEquals("https://www.netflix.com/title/81234567",
                DeepLinkUris.build(DeepLinkUris.NETFLIX, true, "Jailer 2", "81234567"));
        assertEquals("https://www.netflix.com/title/81234567",
                DeepLinkUris.build(DeepLinkUris.NETFLIX, true, "Jailer 2", "https://www.netflix.com/title/81234567"));

        assertEquals("https://www.hotstar.com/in/shows/jailer-2/1260099719",
                DeepLinkUris.build(DeepLinkUris.HOTSTAR, false, "Jailer 2", "1260099719"));
        assertEquals("https://www.hotstar.com/in/movies/jailer-2/1260025583",
                DeepLinkUris.build(DeepLinkUris.HOTSTAR, true, "Jailer 2", "1260025583"));

        String prime = DeepLinkUris.build(DeepLinkUris.PRIME, true, "Jailer 2", "B07ZPDN57Q");
        assertEquals("https://app.primevideo.com/detail/B07ZPDN57Q", prime);
        assertFalse(prime.contains("www.primevideo.com"));
        assertEquals(prime, DeepLinkUris.build(DeepLinkUris.PRIME, true, "Jailer 2",
                "https://www.primevideo.com/detail/B07ZPDN57Q"));

        assertEquals("https://www.sonyliv.com/shows/scam-1992-1700000292",
                DeepLinkUris.build(DeepLinkUris.SONYLIV, false, "Scam 1992", "1700000292"));
        assertEquals("https://www.sonyliv.com/movies/jailer-2-1700000292",
                DeepLinkUris.build(DeepLinkUris.SONYLIV, true, "Jailer 2", "1700000292"));

        assertEquals("https://www.zee5.com/movies/details/jailer/0-0-1z123",
                DeepLinkUris.build(DeepLinkUris.ZEE5, true, "Jailer", "0-0-1z123"));
        assertEquals("https://www.zee5.com/tv-shows/details/jailer/0-0-1z123",
                DeepLinkUris.build(DeepLinkUris.ZEE5, false, "Jailer", "0-0-1z123"));

        assertEquals("https://www.aha.video/webseries/unstoppable",
                DeepLinkUris.build(DeepLinkUris.AHA, false, "Unstoppable", "unstoppable"));
        assertEquals("https://www.aha.video/movie/jailer",
                DeepLinkUris.build(DeepLinkUris.AHA, true, "Jailer", "https://www.aha.video/movie/jailer"));

        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgVcQ",
                DeepLinkUris.build(DeepLinkUris.YOUTUBE, true, "Trailer", "dQw4w9WgVcQ"));
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgVcQ",
                DeepLinkUris.build(DeepLinkUris.YOUTUBE, true, "Trailer", "https://www.youtube.com/watch?v=dQw4w9WgVcQ"));
    }

    @Test(timeout = 5000)
    public void missingIdsAndJioTvOpenTheAppInstead() {
        assertNull(DeepLinkUris.build(DeepLinkUris.NETFLIX, true, "Jailer 2", null));
        assertNull(DeepLinkUris.build(DeepLinkUris.HOTSTAR, false, "Jailer 2", ""));
        assertNull(DeepLinkUris.build(DeepLinkUris.PRIME, true, "Jailer 2", "not-an-asin"));
        assertNull(DeepLinkUris.build(DeepLinkUris.SONYLIV, false, "Scam 1992", "slug-only"));
        assertNull(DeepLinkUris.build(DeepLinkUris.AHA, false, "Unstoppable", null));
        assertNull(DeepLinkUris.build(DeepLinkUris.YOUTUBE, true, "Trailer", "nope"));
        assertNull(DeepLinkUris.build(DeepLinkUris.APP, false, "JioTV+", "anything"));
        assertNull(DeepLinkUris.build(DeepLinkUris.NETFLIX, true, "Jailer 2", "12 34"));
        assertEquals("jailer-2", DeepLinkUris.slug("Jailer 2"));
    }

    @Test(timeout = 5000)
    public void wikidataNetflixClaimBecomesTheTitleUri() {
        String json = "{\"entities\":{\"Q1\":{\"claims\":{\"P1874\":[{\"mainsnak\":{\"datavalue\":{\"type\":\"string\",\"value\":\"81234567\"}}}]}}}}";
        Map<String, String> ids = WikidataIds.read(json, new String[]{"P1874", "P11049"});
        assertEquals("81234567", ids.get("P1874"));
        assertEquals("https://www.netflix.com/title/81234567",
                DeepLinkUris.build(DeepLinkUris.NETFLIX, true, "Jailer 2", ids.get("P1874")));
        assertNull(ids.get("P11049"));
    }
}

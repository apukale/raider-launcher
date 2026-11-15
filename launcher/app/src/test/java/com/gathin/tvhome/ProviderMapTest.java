package com.gathin.tvhome;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ProviderMapTest {
    @Test(timeout = 5000)
    public void mapsVerifiedPackagesAndHidesUnknownApps() throws Exception {
        ProviderMap map = ProviderMap.parse(asset());
        ProviderMap.Hit netflix = map.forPackage("com.netflix.ninja");
        assertEquals(8, netflix.id);
        assertEquals("netflix", netflix.kind);
        assertEquals("Netflix", netflix.name);
        assertEquals("com.netflix.ninja", netflix.packageName);
        assertEquals(8, map.forPackage("com.netflix.mediaclient").id);

        ProviderMap.Hit prime = map.forPackage("com.raider.primevideo");
        assertEquals(119, prime.id);
        assertEquals("prime", prime.kind);
        assertEquals("com.raider.primevideo", prime.packageName);
        assertEquals(119, map.forPackage("com.amazon.amazonvideo.livingroom").id);

        assertEquals(122, map.forPackage("in.startv.hotstar").id);
        assertEquals("hotstar", map.forPackage("in.startv.hotstar").kind);
        assertEquals(237, map.forPackage("com.sonyliv").id);
        assertEquals(232, map.forPackage("com.graymatrix.did").id);
        assertEquals(532, map.forPackage("ahaflix.tv").id);
        assertEquals(192, map.forPackage("org.smarttube.stable").id);
        assertEquals("youtube", map.forPackage("com.google.android.youtube.tv").kind);

        assertNull(map.forPackage("com.jio.media.jiotvplus"));
        assertNull(map.forPackage("com.example.missing"));
        assertNull(map.forPackage(null));
    }

    @Test(timeout = 5000)
    public void queryUsesOnlyInstalledProviders() throws Exception {
        ProviderMap map = ProviderMap.parse(asset());
        assertEquals("", map.providerQuery(Collections.emptySet()));
        assertEquals("8|119", map.providerQuery(set("com.netflix.ninja", "com.raider.primevideo")));
        assertEquals("8|119|122|220", map.providerQuery(set(
                "com.netflix.ninja", "com.amazon.amazonvideo.livingroom", "in.startv.hotstar")));
        assertEquals("", map.providerQuery(set("com.jio.media.jiotvplus")));
    }

    @Test(timeout = 5000)
    public void oneButtonPerInstalledAppPrefersTheRepack() throws Exception {
        ProviderMap map = ProviderMap.parse(asset());
        List<ProviderMap.Hit> hits = map.forProviders(new int[]{220, 122, 119, 8, 999},
                set("in.startv.hotstar", "com.raider.primevideo", "com.amazon.amazonvideo.livingroom"));
        assertEquals(2, hits.size());
        assertEquals("JioHotstar", hits.get(0).name);
        assertEquals("in.startv.hotstar", hits.get(0).packageName);
        assertEquals(122, hits.get(0).id);
        assertEquals("com.raider.primevideo", hits.get(1).packageName);
        assertTrue(hits.get(0).priority < hits.get(1).priority);
    }

    private static Set<String> set(String... packages) {
        return new HashSet<>(Arrays.asList(packages));
    }

    private static String asset() throws Exception {
        Path module = Paths.get("src/main/assets/tmdb_providers.json");
        Path root = Paths.get("app/src/main/assets/tmdb_providers.json");
        Path path = Files.exists(module) ? module : root;
        return new String(Files.readAllBytes(path), java.nio.charset.StandardCharsets.UTF_8);
    }
}

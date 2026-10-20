package com.gathin.tvhome;

import org.junit.Test;
import java.net.URL;
import java.util.Set;
import static org.junit.Assert.*;

public class UnsplashPhotoParserTest {
    @Test(timeout=5000) public void parsesRandomPhotoArray() throws Exception {
        String json="[{\"id\":\"abc123\",\"urls\":{\"raw\":\"https://images.unsplash.com/photo-1?ixlib=rb\",\"regular\":\"https://images.unsplash.com/photo-1?w=1080\"},\"user\":{\"name\":\"A Person\"},\"links\":{\"download_location\":\"https://api.unsplash.com/photos/abc123/download\"}}]";
        java.util.List<UnsplashPhotoParser.Entry> photos=UnsplashPhotoParser.parse(json);
        assertEquals(1,photos.size());
        UnsplashPhotoParser.Entry photo=photos.get(0);
        assertEquals("abc123",photo.id);assertEquals("A Person",photo.author);
        assertEquals("https://api.unsplash.com/photos/abc123/download",photo.downloadLocation);
        assertEquals("https://images.unsplash.com/photo-1?ixlib=rb&w=1920&h=1080&fit=crop&q=80&fm=jpg",UnsplashPhotoParser.sizedUrl(photo.rawUrl));
    }

    @Test(timeout=5000) public void buildsSizedUrlWithAndWithoutQuery() {
        assertEquals("https://images.unsplash.com/image?w=1920&h=1080&fit=crop&q=80&fm=jpg",UnsplashPhotoParser.sizedUrl("https://images.unsplash.com/image"));
        assertEquals("https://images.unsplash.com/image?x=1&w=1920&h=1080&fit=crop&q=80&fm=jpg",UnsplashPhotoParser.sizedUrl("https://images.unsplash.com/image?x=1"));
    }

    @Test(timeout=5000) public void sourceParsingAndLegacyMigration() {
        assertEquals(Set.of("bing","picsum","unsplash"),PhotoSources.parse("both"));
        assertEquals(Set.of("bing","picsum","unsplash"),PhotoSources.parse(""));
        assertEquals(Set.of("bing"),PhotoSources.parse("bing"));
        assertEquals(Set.of("picsum"),PhotoSources.parse("picsum"));
    }

    @Test(timeout=5000) public void allowListRequiresHttpsDefaultPortAndKnownHosts() throws Exception {
        assertTrue(PhotoUrlPolicy.isAllowed(new URL("https://api.unsplash.com/photos/random")));
        assertTrue(PhotoUrlPolicy.isAllowed(new URL("https://images.unsplash.com/photo.jpg")));
        assertFalse(PhotoUrlPolicy.isAllowed(new URL("http://api.unsplash.com/photos/random")));
        assertFalse(PhotoUrlPolicy.isAllowed(new URL("https://api.unsplash.com:444/photos/random")));
        assertFalse(PhotoUrlPolicy.isAllowed(new URL("https://example.com/photo.jpg")));
    }
}

package com.gathin.tvhome;

import org.junit.Test;
import java.net.URL;
import java.util.Set;
import static org.junit.Assert.*;

public class UnsplashPhotoParserTest {
    @Test(timeout=5000) public void parsesRandomPhotoArray() throws Exception {
        String json="[{\"id\":\"abc123\",\"description\":\"Snowy peaks\",\"alt_description\":\"mountain landscape\",\"tags\":[{\"title\":\"nature\"},{\"title\":\"winter\"}],\"tags_preview\":[{\"title\":\"alpine\"}],\"urls\":{\"raw\":\"https://images.unsplash.com/photo-1?ixlib=rb\",\"regular\":\"https://images.unsplash.com/photo-1?w=1080\"},\"user\":{\"name\":\"A Person\"},\"links\":{\"download_location\":\"https://api.unsplash.com/photos/abc123/download\"}}]";
        java.util.List<UnsplashPhotoParser.Entry> photos=UnsplashPhotoParser.parse(json);
        assertEquals(1,photos.size());
        UnsplashPhotoParser.Entry photo=photos.get(0);
        assertEquals("abc123",photo.id);assertEquals("A Person",photo.author);
        assertEquals("https://api.unsplash.com/photos/abc123/download",photo.downloadLocation);
        assertEquals("Snowy peaks",photo.description);assertEquals("mountain landscape",photo.altDescription);
        assertEquals("nature winter alpine",photo.tags);
        assertEquals("https://images.unsplash.com/photo-1?ixlib=rb&w=1920&h=1080&fit=crop&q=80&fm=jpg",UnsplashPhotoParser.sizedUrl(photo.rawUrl));
    }

    @Test(timeout=5000) public void buildsSizedUrlWithAndWithoutQuery() {
        assertEquals("https://images.unsplash.com/image?w=1920&h=1080&fit=crop&q=80&fm=jpg",UnsplashPhotoParser.sizedUrl("https://images.unsplash.com/image"));
        assertEquals("https://images.unsplash.com/image?x=1&w=1920&h=1080&fit=crop&q=80&fm=jpg",UnsplashPhotoParser.sizedUrl("https://images.unsplash.com/image?x=1"));
    }

    @Test(timeout=5000) public void sourceParsingAndLegacyMigration() {
        Set<String> all=Set.of("bing","unsplash","apod","nasa","wikimedia","pexels","pixabay","art");
        assertEquals(all,PhotoSources.parse("both"));
        assertEquals(all,PhotoSources.parse(""));
        assertEquals(Set.of("bing"),PhotoSources.parse("bing"));
        assertEquals(Set.of("bing","unsplash"),PhotoSources.parse("picsum"));
        assertEquals(Set.of("bing","nasa"),PhotoSources.parse("bing,picsum,nasa"));
        assertEquals("https://upload.wikimedia.org/wikipedia/commons/thumb/a/ab/Big_Lake.jpg/1920px-Big_Lake.jpg",ExtraPhotoSources.wikimediaThumb("https://upload.wikimedia.org/wikipedia/commons/a/ab/Big_Lake.jpg",1920));
        assertEquals("/th?id=OHR.Taj",ExtraPhotoSources.bingKey("/th?id=OHR.Taj_EN-IN1234567890"));
    }

    @Test(timeout=5000) public void allowListRequiresHttpsDefaultPortAndKnownHosts() throws Exception {
        assertTrue(PhotoUrlPolicy.isAllowed(new URL("https://api.unsplash.com/photos/random")));
        assertTrue(PhotoUrlPolicy.isAllowed(new URL("https://images.unsplash.com/photo.jpg")));
        assertFalse(PhotoUrlPolicy.isAllowed(new URL("http://api.unsplash.com/photos/random")));
        assertFalse(PhotoUrlPolicy.isAllowed(new URL("https://api.unsplash.com:444/photos/random")));
        assertFalse(PhotoUrlPolicy.isAllowed(new URL("https://example.com/photo.jpg")));
        assertFalse(PhotoUrlPolicy.isAllowed(new URL("https://picsum.photos/id/1/1920/1080.jpg")));
        assertTrue(PhotoUrlPolicy.isAllowed(new URL("https://images-assets.nasa.gov/image/x/x~orig.jpg")));
    }
}

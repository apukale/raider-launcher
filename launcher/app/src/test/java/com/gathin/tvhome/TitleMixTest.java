package com.gathin.tvhome;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TitleMixTest {
    @Test(timeout = 5000)
    public void teluguAndHindiAreTheMajorityAndRowsStayMixed() {
        Map<String, List<TmdbModels.Title>> langs = new LinkedHashMap<>();
        langs.put("te", many("te", 0, 12));
        langs.put("hi", many("hi", 100, 12));
        langs.put("en", many("en", 200, 12));
        List<TmdbModels.Title> mixed = TitleMix.mix(langs, new ArrayList<>(), new String[]{"te", "hi", "en"}, 18);
        assertEquals(18, mixed.size());
        assertEquals(7, count(mixed, "te"));
        assertEquals(6, count(mixed, "hi"));
        assertEquals(5, count(mixed, "en"));
        assertEquals("te", mixed.get(0).language);
        assertEquals("hi", mixed.get(1).language);
        assertEquals("en", mixed.get(2).language);
        assertTrue(count(mixed, "te") + count(mixed, "hi") > mixed.size() / 2);
    }

    @Test(timeout = 5000)
    public void moviesAndSeriesAlternateNewestFirst() {
        TmdbModels.Title olderMovie = item(TmdbModels.MOVIE, 1, "te", "2026-11-01", 1);
        TmdbModels.Title newerShow = item(TmdbModels.TV, 2, "te", "2026-11-10", 1);
        TmdbModels.Title newestMovie = item(TmdbModels.MOVIE, 3, "te", "2026-11-12", 1);
        List<TmdbModels.Title> row = TitleMix.interleaveTypes(Arrays.asList(olderMovie, newerShow, newestMovie), false);
        assertEquals(3, row.size());
        assertEquals(3, row.get(0).id);
        assertEquals(2, row.get(1).id);
        assertEquals(1, row.get(2).id);
        assertTrue(row.get(0).movie());
        assertTrue(!row.get(1).movie());
    }

    @Test(timeout = 5000)
    public void aShortLanguageSpillsIntoTheOthers() {
        Map<String, List<TmdbModels.Title>> langs = new LinkedHashMap<>();
        langs.put("te", many("te", 0, 2));
        langs.put("hi", many("hi", 100, 1));
        List<TmdbModels.Title> extras = many("ta", 300, 4);
        List<TmdbModels.Title> mixed = TitleMix.mix(langs, extras, new String[]{"te", "hi"}, 8);
        assertEquals(7, mixed.size());
        assertEquals(2, count(mixed, "te"));
        assertEquals(1, count(mixed, "hi"));
        assertEquals(4, count(mixed, "ta"));
    }

    private static int count(List<TmdbModels.Title> items, String language) {
        int n = 0;
        for (TmdbModels.Title title : items) if (language.equals(title.language)) n++;
        return n;
    }

    private static List<TmdbModels.Title> many(String language, int idBase, int count) {
        ArrayList<TmdbModels.Title> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            out.add(item(i % 2 == 0 ? TmdbModels.MOVIE : TmdbModels.TV, idBase + i, language, "2026-11-" + (10 + (i % 9)), 100 - i));
        }
        return out;
    }

    private static TmdbModels.Title item(String media, int id, String language, String date, double popularity) {
        return new TmdbModels.Title(media, id, "Title " + id, "", "/poster.jpg", "/back.jpg", date, 7, popularity,
                100, null, null, null, null, "", language, null);
    }
}

package com.gathin.tvhome;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TmdbModelsTest {
    @Test(timeout = 5000)
    public void discoverKeepsPostersAndDetailReadsIndiaProviders() {
        String discover = "{\"results\":["
                + "{\"id\":7,\"title\":\"Jailer 2\",\"overview\":\"A syndicate.\",\"poster_path\":\"/abc.jpg\","
                + "\"backdrop_path\":\"/wide.jpg\",\"vote_average\":8.04,\"popularity\":40,"
                + "\"release_date\":\"2026-08-14\",\"genre_ids\":[28,35,18]},"
                + "{\"id\":8,\"title\":\"No art\",\"poster_path\":null,\"popularity\":99,\"release_date\":\"2026-01-01\"}"
                + "]}";
        List<TmdbModels.Title> movies = TmdbModels.discover(discover, TmdbModels.MOVIE);
        assertEquals(2, movies.size());
        List<TmdbModels.Title> row = TmdbModels.merge(movies, null, 10);
        assertEquals(1, row.size());
        assertEquals("Jailer 2", row.get(0).name);
        assertEquals("2026", row.get(0).year());
        assertEquals("8.0", TmdbModels.ratingLabel(row.get(0).vote));
        assertEquals("Action \u00B7 Comedy \u00B7 Drama", TmdbModels.genreLine(row.get(0)));

        String detail = "{\"id\":7,\"title\":\"Jailer 2\",\"overview\":\"A syndicate.\",\"runtime\":163,"
                + "\"poster_path\":\"/abc.jpg\",\"backdrop_path\":\"/wide.jpg\",\"vote_average\":8,"
                + "\"release_date\":\"2026-08-14\",\"genres\":[{\"id\":28,\"name\":\"Action\"}],"
                + "\"external_ids\":{\"wikidata_id\":\"Q10\"},"
                + "\"credits\":{\"cast\":[{\"name\":\"Rajinikanth\",\"character\":\"Tiger\",\"profile_path\":\"/face.jpg\"}]},"
                + "\"watch/providers\":{\"results\":{\"US\":{\"flatrate\":[{\"provider_id\":8}]},"
                + "\"IN\":{\"flatrate\":[{\"provider_id\":119}],\"ads\":[{\"provider_id\":8}]}}}}";
        TmdbModels.Title full = TmdbModels.detail(detail, TmdbModels.MOVIE);
        assertEquals(163, full.runtime);
        assertEquals("2h 43m", TmdbModels.runtimeLabel(full.runtime));
        assertEquals(2, full.providerIds.length);
        assertEquals(119, full.providerIds[0]);
        assertEquals(8, full.providerIds[1]);
        assertEquals("Q10", full.wikidataId);
        assertEquals("Rajinikanth", full.cast[0].name);
        assertTrue(full.movie());
    }
}

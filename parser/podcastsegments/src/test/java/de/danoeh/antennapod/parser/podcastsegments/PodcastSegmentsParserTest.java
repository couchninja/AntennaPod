package de.danoeh.antennapod.parser.podcastsegments;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class PodcastSegmentsParserTest {

    @Test
    public void parseEpisode_summaryAndSegments() {
        String json = """
                {
                  "version": 1,
                  "episodes": {
                    "https://example.com/feed": {
                      "guid-1": {
                        "mp3_duration_sec": 120,
                        "summary": "<p>Episode summary</p>",
                        "segments": [
                          {"type": "intro", "start_sec": 0, "end_sec": 30, "label": "Opening"}
                        ]
                      }
                    }
                  }
                }
                """;
        PodcastSegmentsIndex index = PodcastSegmentsParser.parse(json);
        EpisodeSegments episode = index.getEpisode("https://example.com/feed", "guid-1");
        assertNotNull(episode);
        assertEquals("<p>Episode summary</p>", episode.getSummary());
        assertEquals(1, episode.getSegments().size());
        assertEquals("Opening", episode.getSegments().get(0).getLabel());
    }

    @Test
    public void parseEpisode_summaryOnly() {
        String json = """
                {
                  "version": 1,
                  "episodes": {
                    "https://example.com/feed": {
                      "guid-2": {
                        "summary": "<p>Only summary</p>"
                      }
                    }
                  }
                }
                """;
        PodcastSegmentsIndex index = PodcastSegmentsParser.parse(json);
        EpisodeSegments episode = index.getEpisode("https://example.com/feed", "guid-2");
        assertNotNull(episode);
        assertEquals("<p>Only summary</p>", episode.getSummary());
        assertEquals(0, episode.getSegments().size());
    }
}

package de.danoeh.antennapod.parser.podcastsegments;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.model.feed.Chapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PodcastSegmentChaptersTest {

    @Test
    public void toChapters_ordersByStartAndConvertsMs() {
        EpisodeSegments episode = new EpisodeSegments(3600, Arrays.asList(
                new PodcastSegment("part_b", 120, 240, null),
                new PodcastSegment("intro", 0, 60, "Hello")
        ), Collections.emptyList());
        List<Chapter> chapters = PodcastSegmentChapters.toChapters(episode);
        assertEquals(2, chapters.size());
        assertEquals(0, chapters.get(0).getStart());
        assertEquals("intro: Hello", chapters.get(0).getTitle());
        assertEquals(120_000, chapters.get(1).getStart());
        assertEquals("part b", chapters.get(1).getTitle());
    }

    @Test
    public void toChapters_titleTypeAndLabel() {
        EpisodeSegments episode = new EpisodeSegments(3600, Collections.singletonList(
                new PodcastSegment("ad", 0, 60, "Alienware RTX")
        ), Collections.emptyList());
        assertEquals("ad: Alienware RTX",
                PodcastSegmentChapters.toChapters(episode).get(0).getTitle());
    }

    @Test
    public void toChapters_emptyWhenNoSegments() {
        assertTrue(PodcastSegmentChapters.toChapters(null).isEmpty());
        assertTrue(PodcastSegmentChapters.toChapters(
                new EpisodeSegments(0, Collections.emptyList(), Collections.emptyList())).isEmpty());
    }
}

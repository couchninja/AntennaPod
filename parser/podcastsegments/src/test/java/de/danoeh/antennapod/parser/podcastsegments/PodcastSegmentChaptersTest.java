package de.danoeh.antennapod.parser.podcastsegments;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.model.feed.Chapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PodcastSegmentChaptersTest {

    @Test
    public void toChapters_ordersByStartAndConvertsMs() {
        EpisodeSegments episode = new EpisodeSegments(3600, Arrays.asList(
                new PodcastSegment("part_b", 120, 240, null),
                new PodcastSegment("intro", 0, 60, "Hello")
        ), Collections.emptyList());
        List<Chapter> chapters = PodcastSegmentChapters.toChapters(episode);
        assertEquals(4, chapters.size());
        assertEquals(0, chapters.get(0).getStart());
        assertEquals("intro: Hello", chapters.get(0).getTitle());
        assertEquals(60_000, chapters.get(1).getStart());
        assertEquals("unsegmented", chapters.get(1).getTitle());
        assertEquals(120_000, chapters.get(2).getStart());
        assertEquals("part b", chapters.get(2).getTitle());
        assertEquals(240_000, chapters.get(3).getStart());
        assertEquals("unsegmented", chapters.get(3).getTitle());
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

    @Test
    public void toChapters_addsUnsegmentedForGaps() {
        EpisodeSegments episode = new EpisodeSegments(3600, Arrays.asList(
                new PodcastSegment("intro", 60, 120, null),
                new PodcastSegment("ad", 200, 260, null)
        ), Collections.emptyList());
        List<Chapter> chapters = PodcastSegmentChapters.toChapters(episode);
        assertEquals(5, chapters.size());
        assertEquals(0, chapters.get(0).getStart());
        assertEquals("unsegmented", chapters.get(0).getTitle());
        assertEquals(60_000, chapters.get(1).getStart());
        assertEquals("intro", chapters.get(1).getTitle());
        assertEquals(120_000, chapters.get(2).getStart());
        assertEquals("unsegmented", chapters.get(2).getTitle());
        assertEquals(200_000, chapters.get(3).getStart());
        assertEquals("ad", chapters.get(3).getTitle());
        assertEquals(260_000, chapters.get(4).getStart());
        assertEquals("unsegmented", chapters.get(4).getTitle());
        boolean[] flags = PodcastSegmentChapters.autoSkipFlags(episode);
        assertEquals(5, flags.length);
        assertFalse(flags[0]);
        assertFalse(flags[1]);
        assertFalse(flags[2]);
        assertTrue(flags[3]);
        assertFalse(flags[4]);
    }
}

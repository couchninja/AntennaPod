package de.danoeh.antennapod.parser.podcastsegments;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class PodcastSegmentSkipHelperTest {

    @Test
    public void naturalPlaybackSkipsWhenEnteringSegment() {
        EpisodeSegments episode = new EpisodeSegments(3600,
                Collections.singletonList(new PodcastSegment("ad", 30, 60, null)),
                Collections.emptyList());
        long target = PodcastSegmentSkipHelper.applyNaturalPlaybackSkipMs(episode, 3600_000, 29_000, 30_500);
        assertEquals(60_000, target);
    }

    @Test
    public void naturalPlaybackDoesNotSkipWhenSeekingIntoSegment() {
        EpisodeSegments episode = new EpisodeSegments(3600,
                Collections.singletonList(new PodcastSegment("ad", 30, 60, null)),
                Collections.emptyList());
        long target = PodcastSegmentSkipHelper.applyNaturalPlaybackSkipMs(episode, 3600_000, 10_000, 35_000);
        assertEquals(35_000, target);
    }

    @Test
    public void continuousSkipWhenAlreadyInsideSegment() {
        EpisodeSegments episode = new EpisodeSegments(3600,
                Collections.singletonList(new PodcastSegment("ad", 30, 60, null)),
                Collections.emptyList());
        long target = PodcastSegmentSkipHelper.applyContinuousSkipMs(episode, 3600_000, 35_000);
        assertEquals(60_000, target);
    }

    @Test
    public void naturalPlaybackDoesNotSkipWhenAlreadyInsideSegment() {
        EpisodeSegments episode = new EpisodeSegments(3600,
                Collections.singletonList(new PodcastSegment("ad", 30, 60, null)),
                Collections.emptyList());
        long target = PodcastSegmentSkipHelper.applyNaturalPlaybackSkipMs(episode, 3600_000, 35_000, 36_000);
        assertEquals(36_000, target);
    }
}

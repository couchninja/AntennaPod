package de.danoeh.antennapod.parser.podcastsegments;

import org.junit.Test;

import de.danoeh.antennapod.model.feed.FeedItem;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PodcastSegmentAutoSkipTest {

    @Test
    public void isAutoSkipType_adAndPatreonVariants() {
        assertTrue(new PodcastSegment("ad", 0, 10, null).isAutoSkipType());
        assertTrue(new PodcastSegment("patreon_backers", 0, 10, null).isAutoSkipType());
        assertTrue(new PodcastSegment("patreon backers", 0, 10, null).isAutoSkipType());
        assertFalse(new PodcastSegment("intro", 0, 10, null).isAutoSkipType());
    }

    @Test
    public void segmentChapterAutoSkipColors_onlyWhenChaptersFromSegments() {
        FeedItem item = new FeedItem();
        assertNull(PodcastSegmentSkipHelper.segmentChapterAutoSkipColors(item));
        item.setChaptersFromPodcastSegments(true);
        assertNull(PodcastSegmentSkipHelper.segmentChapterAutoSkipColors(item));
    }
}

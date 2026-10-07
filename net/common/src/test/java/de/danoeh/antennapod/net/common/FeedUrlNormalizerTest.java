package de.danoeh.antennapod.net.common;

import junit.framework.TestCase;

public class FeedUrlNormalizerTest extends TestCase {

    public void testHttpAndHttpsSameKey() {
        String http = FeedUrlNormalizer.normalizeFeedUrlKey("http://example.com/feed.xml");
        String https = FeedUrlNormalizer.normalizeFeedUrlKey("https://example.com/feed.xml");
        assertEquals(http, https);
    }

    public void testPathSegmentCase() {
        String url = FeedUrlNormalizer.normalizeFeedUrlKey("https://Example.com/Feed/Item.xml");
        assertEquals("https://example.com/feed/item.xml", url);
    }
}

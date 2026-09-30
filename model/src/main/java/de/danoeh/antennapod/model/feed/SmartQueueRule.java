package de.danoeh.antennapod.model.feed;

public class SmartQueueRule {
    public static final int MIN_EPISODE_COUNT = 1;
    public static final int MAX_EPISODE_COUNT = 10;

    private long feedId;
    private boolean fromTop;
    private int episodeCount;

    public SmartQueueRule(long feedId, boolean fromTop, int episodeCount) {
        this.feedId = feedId;
        this.fromTop = fromTop;
        this.episodeCount = episodeCount;
    }

    public long getFeedId() {
        return feedId;
    }

    public void setFeedId(long feedId) {
        this.feedId = feedId;
    }

    public boolean isFromTop() {
        return fromTop;
    }

    public void setFromTop(boolean fromTop) {
        this.fromTop = fromTop;
    }

    public int getEpisodeCount() {
        return episodeCount;
    }

    public void setEpisodeCount(int episodeCount) {
        this.episodeCount = episodeCount;
    }
}

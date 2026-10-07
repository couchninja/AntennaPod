package de.danoeh.antennapod.parser.podcastsegments;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;

public final class PodcastSegmentSkipHelper {

    private PodcastSegmentSkipHelper() {
    }

    public static long applyContinuousSkipMs(FeedMedia media, long positionMs) {
        EpisodeSegments episode = getEpisodeSegments(media);
        if (episode == null) {
            return positionMs;
        }
        long durationMs = getDurationMs(media);
        long pos = positionMs;
        boolean changed;
        do {
            changed = false;
            for (PodcastSegment segment : episode.getSegments()) {
                if (!segment.isAutoSkipType()) {
                    continue;
                }
                long startMs = (long) (segment.getStartSec() * 1000);
                long endMs = (long) (segment.getEndSec() * 1000);
                if (durationMs > 0 && endMs > durationMs) {
                    endMs = durationMs;
                }
                if (pos >= startMs && pos < endMs) {
                    pos = endMs;
                    changed = true;
                }
            }
        } while (changed);
        return pos;
    }

    public static long applyForwardSkipMs(FeedMedia media, long positionMs) {
        return applyContinuousSkipMs(media, positionMs);
    }

    public static boolean shouldMarkPlayedAfterSkip(FeedMedia media, long positionMs) {
        long durationMs = getDurationMs(media);
        if (durationMs <= 0) {
            return false;
        }
        return positionMs >= durationMs - 500;
    }

    private static EpisodeSegments getEpisodeSegments(FeedMedia media) {
        if (media == null || media.getItem() == null) {
            return null;
        }
        return PodcastSegmentsRepository.getEpisodeSegments(media.getItem());
    }

    private static long getDurationMs(FeedMedia media) {
        if (media.getDuration() > 0) {
            return media.getDuration();
        }
        EpisodeSegments episode = getEpisodeSegments(media);
        if (episode != null && episode.getMp3DurationSec() > 0) {
            return (long) (episode.getMp3DurationSec() * 1000);
        }
        return 0;
    }

    public static EpisodeSegments getEpisodeSegments(FeedItem item) {
        return PodcastSegmentsRepository.getEpisodeSegments(item);
    }
}

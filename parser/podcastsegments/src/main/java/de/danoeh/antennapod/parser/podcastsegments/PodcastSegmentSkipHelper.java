package de.danoeh.antennapod.parser.podcastsegments;

import androidx.annotation.Nullable;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PodcastSegmentSkipHelper {

    private static final long MAX_NATURAL_PLAYBACK_STEP_MS = 5000;

    private PodcastSegmentSkipHelper() {
    }

    public static long getMaxNaturalPlaybackStepMs() {
        return MAX_NATURAL_PLAYBACK_STEP_MS;
    }

    public static long applyContinuousSkipMs(FeedMedia media, long positionMs) {
        EpisodeSegments episode = getEpisodeSegments(media);
        if (episode == null) {
            return positionMs;
        }
        return applyContinuousSkipMs(episode, getDurationMs(media), positionMs);
    }

    public static long applyContinuousSkipMs(EpisodeSegments episode, long durationMs, long positionMs) {
        long pos = positionMs;
        boolean changed;
        do {
            changed = false;
            for (PodcastSegment segment : getAutoSkipSegments(episode)) {
                long startMs = (long) (segment.getStartSec() * 1000);
                long endMs = segmentEndMs(segment, durationMs);
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

    public static long applyNaturalPlaybackSkipMs(FeedMedia media, long previousMs, long currentMs) {
        EpisodeSegments episode = getEpisodeSegments(media);
        if (episode == null) {
            return currentMs;
        }
        return applyNaturalPlaybackSkipMs(episode, getDurationMs(media), previousMs, currentMs);
    }

    public static long applyNaturalPlaybackSkipMs(EpisodeSegments episode, long durationMs,
                                                  long previousMs, long currentMs) {
        if (previousMs < 0 || currentMs <= previousMs) {
            return currentMs;
        }
        if (currentMs - previousMs > MAX_NATURAL_PLAYBACK_STEP_MS) {
            return currentMs;
        }
        for (PodcastSegment segment : getAutoSkipSegments(episode)) {
            long startMs = (long) (segment.getStartSec() * 1000);
            long endMs = segmentEndMs(segment, durationMs);
            if (previousMs < startMs && currentMs >= startMs && currentMs < endMs) {
                return endMs;
            }
        }
        return currentMs;
    }

    @Nullable
    public static boolean[] segmentChapterAutoSkipColors(FeedItem item) {
        if (item == null || !item.isChaptersFromPodcastSegments()) {
            return null;
        }
        EpisodeSegments episode = getEpisodeSegments(item);
        if (episode == null || !episode.hasSegments()) {
            return null;
        }
        List<PodcastSegment> segments = new ArrayList<>(episode.getSegments());
        segments.sort(Comparator.comparingDouble(PodcastSegment::getStartSec));
        boolean[] flags = new boolean[segments.size()];
        for (int i = 0; i < segments.size(); i++) {
            flags[i] = segments.get(i).isAutoSkipType();
        }
        return flags;
    }

    public static boolean isInsideAutoSkipSegment(FeedMedia media, long positionMs) {
        EpisodeSegments episode = getEpisodeSegments(media);
        if (episode == null) {
            return false;
        }
        long durationMs = getDurationMs(media);
        for (PodcastSegment segment : getAutoSkipSegments(episode)) {
            long startMs = (long) (segment.getStartSec() * 1000);
            long endMs = segmentEndMs(segment, durationMs);
            if (positionMs >= startMs && positionMs < endMs) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldMarkPlayedAfterSkip(FeedMedia media, long positionMs) {
        long durationMs = getDurationMs(media);
        if (durationMs <= 0) {
            return false;
        }
        return positionMs >= durationMs - 500;
    }

    private static List<PodcastSegment> getAutoSkipSegments(EpisodeSegments episode) {
        List<PodcastSegment> autoSkip = new ArrayList<>();
        for (PodcastSegment segment : episode.getSegments()) {
            if (segment.isAutoSkipType()) {
                autoSkip.add(segment);
            }
        }
        return autoSkip;
    }

    private static long segmentEndMs(PodcastSegment segment, long durationMs) {
        long endMs = (long) (segment.getEndSec() * 1000);
        if (durationMs > 0 && endMs > durationMs) {
            endMs = durationMs;
        }
        return endMs;
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

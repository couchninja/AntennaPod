package de.danoeh.antennapod.playback.service.internal;

import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.parser.podcastsegments.PodcastSegmentSkipHelper;

public final class PodcastSegmentPlayback {

    public interface SeekListener {
        void seekTo(long positionMs);
    }

    public interface SkipToNextListener {
        void skipToNextEpisode();
    }

    private PodcastSegmentPlayback() {
    }

    public static long adjustForwardSeekPosition(FeedMedia media, long positionMs) {
        return PodcastSegmentSkipHelper.applyForwardSkipMs(media, positionMs);
    }

    public static void skipDuringContinuousPlayback(FeedMedia media, long positionMs, long durationMs,
                                                      SeekListener seekListener, SkipToNextListener skipListener) {
        long target = PodcastSegmentSkipHelper.applyContinuousSkipMs(media, positionMs);
        if (target == positionMs) {
            return;
        }
        if (PodcastSegmentSkipHelper.shouldMarkPlayedAfterSkip(media, target)
                || (durationMs > 0 && target >= durationMs - 500)) {
            skipListener.skipToNextEpisode();
        } else {
            seekListener.seekTo(target);
        }
    }
}

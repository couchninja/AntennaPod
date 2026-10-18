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

    public static void skipDuringContinuousPlayback(FeedMedia media, PodcastSegmentSkipState state, long currentMs,
                                                      long durationMs, SeekListener seekListener,
                                                      SkipToNextListener skipListener) {
        long target = state.computeSkipTargetMs(media, currentMs);
        if (target == currentMs) {
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

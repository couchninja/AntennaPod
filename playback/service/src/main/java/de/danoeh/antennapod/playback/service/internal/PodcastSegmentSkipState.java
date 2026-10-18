package de.danoeh.antennapod.playback.service.internal;

import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.parser.podcastsegments.PodcastSegmentSkipHelper;

public final class PodcastSegmentSkipState {

    private long lastObservedPositionMs = Playable.INVALID_TIME;
    private boolean suppressSkipWhileInsideSegment;

    public void reset() {
        lastObservedPositionMs = Playable.INVALID_TIME;
        suppressSkipWhileInsideSegment = false;
    }

    public void onUserSeek(FeedMedia media, long positionMs) {
        lastObservedPositionMs = positionMs;
        suppressSkipWhileInsideSegment = PodcastSegmentSkipHelper.isInsideAutoSkipSegment(media, positionMs);
    }

    public void onAutomaticPositionSet(long positionMs) {
        lastObservedPositionMs = positionMs;
    }

    public long computeSkipTargetMs(FeedMedia media, long currentMs) {
        if (suppressSkipWhileInsideSegment) {
            if (!PodcastSegmentSkipHelper.isInsideAutoSkipSegment(media, currentMs)) {
                suppressSkipWhileInsideSegment = false;
            } else {
                lastObservedPositionMs = currentMs;
                return currentMs;
            }
        }

        long targetMs = currentMs;
        long previousMs = lastObservedPositionMs;
        if (previousMs == Playable.INVALID_TIME) {
            targetMs = PodcastSegmentSkipHelper.applyContinuousSkipMs(media, currentMs);
        } else if (currentMs > previousMs) {
            long stepMs = currentMs - previousMs;
            if (stepMs > PodcastSegmentSkipHelper.getMaxNaturalPlaybackStepMs()) {
                if (PodcastSegmentSkipHelper.isInsideAutoSkipSegment(media, currentMs)) {
                    suppressSkipWhileInsideSegment = true;
                }
            } else {
                targetMs = PodcastSegmentSkipHelper.applyNaturalPlaybackSkipMs(media, previousMs, currentMs);
                if (targetMs == currentMs) {
                    targetMs = PodcastSegmentSkipHelper.applyContinuousSkipMs(media, currentMs);
                }
            }
        }
        lastObservedPositionMs = targetMs;
        return targetMs;
    }
}

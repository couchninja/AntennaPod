package de.danoeh.antennapod.parser.podcastsegments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import de.danoeh.antennapod.model.feed.Chapter;

public final class PodcastSegmentChapters {

    private static final String UNSEGMENTED_TITLE = "unsegmented";

    private PodcastSegmentChapters() {
    }

    public static List<Chapter> toChapters(EpisodeSegments episode) {
        return buildChapters(episode).chapters;
    }

    public static boolean[] autoSkipFlags(EpisodeSegments episode) {
        ChapterBuild build = buildChapters(episode);
        if (build.autoSkipFlags.length == 0) {
            return null;
        }
        return build.autoSkipFlags;
    }

    private static ChapterBuild buildChapters(EpisodeSegments episode) {
        if (episode == null || !episode.hasSegments()) {
            return new ChapterBuild(Collections.emptyList(), new boolean[0]);
        }
        List<PodcastSegment> segments = new ArrayList<>(episode.getSegments());
        segments.sort(Comparator.comparingDouble(PodcastSegment::getStartSec));
        List<Chapter> chapters = new ArrayList<>();
        List<Boolean> autoSkip = new ArrayList<>();
        double prevEndSec = 0;
        for (PodcastSegment segment : segments) {
            if (segment.getStartSec() > prevEndSec) {
                chapters.add(new Chapter((long) (prevEndSec * 1000), UNSEGMENTED_TITLE, null, null));
                autoSkip.add(false);
            }
            chapters.add(new Chapter((long) (segment.getStartSec() * 1000),
                    formatChapterTitle(segment), null, null));
            autoSkip.add(segment.isAutoSkipType());
            prevEndSec = Math.max(prevEndSec, segment.getEndSec());
        }
        double durationSec = episode.getMp3DurationSec();
        if (durationSec > 0 && prevEndSec < durationSec) {
            chapters.add(new Chapter((long) (prevEndSec * 1000), UNSEGMENTED_TITLE, null, null));
            autoSkip.add(false);
        }
        boolean[] flags = new boolean[autoSkip.size()];
        for (int i = 0; i < autoSkip.size(); i++) {
            flags[i] = autoSkip.get(i);
        }
        return new ChapterBuild(chapters, flags);
    }

    private static final class ChapterBuild {
        private final List<Chapter> chapters;
        private final boolean[] autoSkipFlags;

        private ChapterBuild(List<Chapter> chapters, boolean[] autoSkipFlags) {
            this.chapters = chapters;
            this.autoSkipFlags = autoSkipFlags;
        }
    }

    private static String formatChapterTitle(PodcastSegment segment) {
        String type = humanizeType(segment.getType());
        String label = segment.getLabel();
        if (label == null || label.isEmpty()) {
            return type;
        }
        return type + ": " + label;
    }

    private static String humanizeType(String type) {
        if (type == null) {
            return "";
        }
        return type.replace('_', ' ');
    }
}

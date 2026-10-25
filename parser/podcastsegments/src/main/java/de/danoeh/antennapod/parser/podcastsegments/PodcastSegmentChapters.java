package de.danoeh.antennapod.parser.podcastsegments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import de.danoeh.antennapod.model.feed.Chapter;

public final class PodcastSegmentChapters {

    private PodcastSegmentChapters() {
    }

    public static List<Chapter> toChapters(EpisodeSegments episode) {
        if (episode == null || !episode.hasSegments()) {
            return Collections.emptyList();
        }
        List<Chapter> chapters = new ArrayList<>();
        for (PodcastSegment segment : episode.getSegments()) {
            chapters.add(new Chapter((long) (segment.getStartSec() * 1000),
                    formatChapterTitle(segment), null, null));
        }
        chapters.sort(Comparator.comparingLong(Chapter::getStart));
        return chapters;
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

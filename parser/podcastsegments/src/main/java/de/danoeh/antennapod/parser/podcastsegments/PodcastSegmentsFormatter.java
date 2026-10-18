package de.danoeh.antennapod.parser.podcastsegments;

import java.util.Locale;

import de.danoeh.antennapod.model.feed.FeedItem;

public final class PodcastSegmentsFormatter {

    private PodcastSegmentsFormatter() {
    }

    public static String appendToDescription(String description, FeedItem item) {
        EpisodeSegments episode = PodcastSegmentsRepository.getEpisodeSegments(item);
        if (episode == null || !episode.hasSegments()) {
            return description;
        }
        StringBuilder block = new StringBuilder();
        block.append("<p><b>Segments</b></p>");
        for (PodcastSegment segment : episode.getSegments()) {
            block.append("<p>")
                    .append(formatTime(segment.getStartSec()))
                    .append("–")
                    .append(formatTime(segment.getEndSec()))
                    .append(" ")
                    .append(escapeHtml(segment.getType()));
            if (segment.getLabel() != null) {
                block.append(" (").append(escapeHtml(segment.getLabel())).append(")");
            }
            block.append("</p>");
        }
        if (description == null || description.isEmpty()) {
            return block.toString();
        }
        return description + block;
    }

    private static String formatTime(double seconds) {
        int totalSec = (int) seconds;
        int minutes = totalSec / 60;
        int secs = totalSec % 60;
        return String.format(Locale.US, "%02d:%02d", minutes, secs);
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

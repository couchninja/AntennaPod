package de.danoeh.antennapod.parser.podcastsegments;

import java.util.Locale;

import de.danoeh.antennapod.model.feed.FeedItem;

public final class PodcastSegmentsFormatter {

    private PodcastSegmentsFormatter() {
    }

    public static String appendToDescription(String description, FeedItem item, String summaryHeading,
                                             String shownotesHeading) {
        EpisodeSegments episode = PodcastSegmentsRepository.getEpisodeSegments(item);
        if (episode == null) {
            return description;
        }
        if (episode.getSummary() == null && !episode.hasSegments()) {
            return description;
        }
        StringBuilder result = new StringBuilder();
        if (episode.getSummary() != null) {
            if (summaryHeading != null && !summaryHeading.isEmpty()) {
                result.append("<p><b>").append(escapeHtml(summaryHeading)).append("</b></p>");
            }
            result.append(formatSummaryForShownotes(episode.getSummary()));
        }
        if (description != null && !description.isEmpty()) {
            if (shownotesHeading != null && !shownotesHeading.isEmpty()) {
                result.append("<p><b>").append(escapeHtml(shownotesHeading)).append("</b></p>");
            }
            result.append(description);
        }
        if (episode.hasSegments()) {
            result.append("<p><b>Segments</b></p>");
            for (PodcastSegment segment : episode.getSegments()) {
                result.append("<p>")
                        .append(formatTime(segment.getStartSec()))
                        .append("–")
                        .append(formatTime(segment.getEndSec()))
                        .append(" ")
                        .append(escapeHtml(segment.getType()));
                if (segment.getLabel() != null) {
                    result.append(" (").append(escapeHtml(segment.getLabel())).append(")");
                }
                result.append("</p>");
            }
        }
        if (result.length() == 0) {
            return description;
        }
        return result.toString();
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

    private static String formatSummaryForShownotes(String summary) {
        String text = summary.replace("\r\n", "\n").replace('\r', '\n');
        if (!text.contains("<")) {
            return "<p>" + escapeHtml(text).replace("\n", "<br />") + "</p>";
        }
        return text.replace("\n", "<br />");
    }
}

package de.danoeh.antennapod.parser.podcastsegments;

import java.util.Collections;
import java.util.List;

public class EpisodeSegments {
    private final double mp3DurationSec;
    private final List<PodcastSegment> segments;
    private final List<String> episodeUrlAliases;
    private final String summary;

    public EpisodeSegments(double mp3DurationSec, List<PodcastSegment> segments, List<String> episodeUrlAliases) {
        this(mp3DurationSec, segments, episodeUrlAliases, null);
    }

    public EpisodeSegments(double mp3DurationSec, List<PodcastSegment> segments, List<String> episodeUrlAliases,
                           String summary) {
        this.mp3DurationSec = mp3DurationSec;
        this.segments = segments == null ? Collections.emptyList() : segments;
        this.episodeUrlAliases = episodeUrlAliases == null ? Collections.emptyList() : episodeUrlAliases;
        this.summary = summary;
    }

    public double getMp3DurationSec() {
        return mp3DurationSec;
    }

    public List<PodcastSegment> getSegments() {
        return segments;
    }

    public List<String> getEpisodeUrlAliases() {
        return episodeUrlAliases;
    }

    public boolean hasSegments() {
        return !segments.isEmpty();
    }

    public String getSummary() {
        return summary;
    }
}

package de.danoeh.antennapod.parser.podcastsegments;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PodcastSegmentsIndex {
    private final int version;
    private final Map<String, Map<String, EpisodeSegments>> episodesByFeed;

    public PodcastSegmentsIndex(int version, Map<String, Map<String, EpisodeSegments>> episodesByFeed) {
        this.version = version;
        this.episodesByFeed = episodesByFeed == null ? Collections.emptyMap() : episodesByFeed;
    }

    public int getVersion() {
        return version;
    }

    public EpisodeSegments getEpisode(String feedUrlNormalized, String guidNormalized) {
        Map<String, EpisodeSegments> feedEpisodes = episodesByFeed.get(feedUrlNormalized);
        if (feedEpisodes == null) {
            return null;
        }
        return feedEpisodes.get(guidNormalized);
    }

    public Collection<EpisodeSegments> getEpisodesForFeed(String feedUrlNormalized) {
        Map<String, EpisodeSegments> feedEpisodes = episodesByFeed.get(feedUrlNormalized);
        if (feedEpisodes == null) {
            return Collections.emptyList();
        }
        return feedEpisodes.values();
    }

    public static PodcastSegmentsIndex empty() {
        return new PodcastSegmentsIndex(0, new HashMap<>());
    }
}

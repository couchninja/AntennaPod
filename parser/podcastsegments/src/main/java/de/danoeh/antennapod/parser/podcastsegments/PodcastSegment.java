package de.danoeh.antennapod.parser.podcastsegments;

public class PodcastSegment {
    private final String type;
    private final double startSec;
    private final double endSec;
    private final String label;

    public PodcastSegment(String type, double startSec, double endSec, String label) {
        this.type = type;
        this.startSec = startSec;
        this.endSec = endSec;
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public double getStartSec() {
        return startSec;
    }

    public double getEndSec() {
        return endSec;
    }

    public String getLabel() {
        return label;
    }

    public boolean isAutoSkipType() {
        return "ad".equals(type) || "patreon_backers".equals(type);
    }
}

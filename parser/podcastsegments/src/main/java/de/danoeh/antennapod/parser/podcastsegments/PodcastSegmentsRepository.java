package de.danoeh.antennapod.parser.podcastsegments;

import android.content.Context;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.common.FeedUrlNormalizer;
import de.danoeh.antennapod.net.common.UrlChecker;

public class PodcastSegmentsRepository {
    public static final String FILE_NAME = "podcast_segments.json";
    private static final String TAG = "PodcastSegmentsRepo";

    private static PodcastSegmentsIndex cachedIndex;
    private static File segmentsFile;

    public static void init(Context context) {
        setSegmentsFile(new File(context.getFilesDir(), FILE_NAME));
    }

    public static void setSegmentsFile(File file) {
        segmentsFile = file;
        cachedIndex = null;
    }

    public static File getSegmentsFile() {
        return segmentsFile;
    }

    public static synchronized PodcastSegmentsIndex getIndex() {
        if (cachedIndex != null) {
            return cachedIndex;
        }
        if (segmentsFile == null || !segmentsFile.exists()) {
            cachedIndex = PodcastSegmentsIndex.empty();
            return cachedIndex;
        }
        try {
            String json = FileUtils.readFileToString(segmentsFile, StandardCharsets.UTF_8);
            cachedIndex = PodcastSegmentsParser.parse(json);
            return cachedIndex;
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            cachedIndex = PodcastSegmentsIndex.empty();
            return cachedIndex;
        }
    }

    public static synchronized void reloadFromDisk() {
        cachedIndex = null;
        getIndex();
    }

    public static synchronized void writeJsonAtomically(String json, File targetFile) throws IOException {
        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        File tempFile = new File(targetFile.getParentFile(), targetFile.getName() + ".tmp");
        FileUtils.writeStringToFile(tempFile, json, StandardCharsets.UTF_8);
        if (targetFile.exists() && !targetFile.delete()) {
            throw new IOException("Could not delete old segments file");
        }
        if (!tempFile.renameTo(targetFile)) {
            FileUtils.copyFile(tempFile, targetFile);
            tempFile.delete();
        }
        cachedIndex = null;
    }

    public static EpisodeSegments getEpisodeSegments(FeedItem item) {
        if (item == null || item.getFeed() == null) {
            return null;
        }
        Feed feed = item.getFeed();
        String feedKey = FeedUrlNormalizer.normalizeFeedUrlKey(feed.getDownloadUrl());
        PodcastSegmentsIndex index = getIndex();
        String guid = item.getItemIdentifier();
        if (PodcastSegmentsParser.isValidGuid(guid)) {
            EpisodeSegments episode = index.getEpisode(feedKey, PodcastSegmentsParser.normalizeGuid(guid));
            if (episode != null) {
                return clampToDuration(episode, item);
            }
        }
        FeedMedia media = item.getMedia();
        if (media == null || media.getDownloadUrl() == null) {
            return null;
        }
        String mediaUrl = media.getDownloadUrl();
        for (EpisodeSegments candidate : index.getEpisodesForFeed(feedKey)) {
            if (matchesEnclosureUrl(candidate, mediaUrl)) {
                return clampToDuration(candidate, item);
            }
        }
        return null;
    }

    private static boolean matchesEnclosureUrl(EpisodeSegments episode, String mediaUrl) {
        for (String alias : episode.getEpisodeUrlAliases()) {
            if (UrlChecker.urlEquals(alias, mediaUrl)) {
                return true;
            }
        }
        return false;
    }

    private static EpisodeSegments clampToDuration(EpisodeSegments episode, FeedItem item) {
        long durationMs = 0;
        FeedMedia media = item.getMedia();
        if (media != null && media.getDuration() > 0) {
            durationMs = media.getDuration();
        } else if (episode.getMp3DurationSec() > 0) {
            durationMs = (long) (episode.getMp3DurationSec() * 1000);
        }
        if (durationMs <= 0) {
            return episode;
        }
        double durationSec = durationMs / 1000.0;
        List<PodcastSegment> clamped = new ArrayList<>();
        for (PodcastSegment segment : episode.getSegments()) {
            double endSec = segment.getEndSec();
            if (endSec > durationSec) {
                endSec = durationSec;
            }
            if (endSec <= segment.getStartSec()) {
                continue;
            }
            clamped.add(new PodcastSegment(segment.getType(), segment.getStartSec(), endSec, segment.getLabel()));
        }
        return new EpisodeSegments(Math.min(episode.getMp3DurationSec(), durationSec), clamped,
                episode.getEpisodeUrlAliases());
    }

    public static boolean hasEpisodeSegments(FeedItem item) {
        EpisodeSegments episode = getEpisodeSegments(item);
        return episode != null && episode.hasSegments();
    }
}

package de.danoeh.antennapod.storage.database;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.feed.SortOrder;

public class FeedKeepDownloadHelper {
    private FeedKeepDownloadHelper() {
    }

    @NonNull
    public static List<FeedItem> getUndownloadedKeepCandidates() {
        List<FeedItem> undownloaded = new ArrayList<>();
        for (Feed feed : DBReader.getFeedList()) {
            if (feed.isLocalFeed()) {
                continue;
            }
            FeedPreferences prefs = feed.getPreferences();
            if (prefs.getKeepDownloadEpisodeCount() == FeedPreferences.KEEP_DOWNLOAD_DISABLED) {
                continue;
            }
            Feed fullFeed = DBReader.getFeed(feed.getId(), false, 0, Integer.MAX_VALUE);
            if (fullFeed == null || fullFeed.getItems() == null) {
                continue;
            }
            for (FeedItem item : pickEpisodes(fullFeed, prefs.getKeepDownloadEpisodeCount(),
                    prefs.isKeepDownloadFromTop())) {
                if (!item.isDownloaded() && item.hasMedia()) {
                    undownloaded.add(item);
                }
            }
        }
        return undownloaded;
    }

    @NonNull
    public static Set<Long> getKeptFeedItemIds() {
        Set<Long> ids = new HashSet<>();
        for (Feed feed : DBReader.getFeedList()) {
            if (feed.isLocalFeed()) {
                continue;
            }
            FeedPreferences prefs = feed.getPreferences();
            if (prefs.getKeepDownloadEpisodeCount() == FeedPreferences.KEEP_DOWNLOAD_DISABLED) {
                continue;
            }
            Feed fullFeed = DBReader.getFeed(feed.getId(), false, 0, Integer.MAX_VALUE);
            if (fullFeed == null || fullFeed.getItems() == null) {
                continue;
            }
            for (FeedItem item : pickEpisodes(fullFeed, prefs.getKeepDownloadEpisodeCount(),
                    prefs.isKeepDownloadFromTop())) {
                ids.add(item.getId());
            }
        }
        return ids;
    }

    @NonNull
    public static List<FeedItem> pickEpisodes(@NonNull Feed feed, int episodeCount, boolean fromTop) {
        if (episodeCount == FeedPreferences.KEEP_DOWNLOAD_DISABLED || feed.getItems() == null) {
            return Collections.emptyList();
        }
        List<FeedItem> candidates = new ArrayList<>();
        for (FeedItem item : feed.getItems()) {
            if (item.isPlayed() || !item.hasMedia()) {
                continue;
            }
            candidates.add(item);
        }
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }
        SortOrder sortOrder = feed.getSortOrder();
        if (sortOrder == null) {
            sortOrder = SortOrder.GLOBAL_DEFAULT;
        }
        FeedItemPermutors.getPermutor(sortOrder).reorder(candidates);
        int pickCount = episodeCount == FeedPreferences.KEEP_DOWNLOAD_ALL
                ? candidates.size() : Math.min(episodeCount, candidates.size());
        if (fromTop) {
            return new ArrayList<>(candidates.subList(0, pickCount));
        }
        List<FeedItem> picked = new ArrayList<>(candidates.subList(candidates.size() - pickCount, candidates.size()));
        Collections.reverse(picked);
        return picked;
    }
}

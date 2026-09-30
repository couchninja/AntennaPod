package de.danoeh.antennapod.storage.database;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.SmartQueueRule;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

public class SmartQueueRefiller {
    private static final String TAG = "SmartQueueRefiller";

    @FunctionalInterface
    private interface EpisodeCountProvider {
        int getCount(SmartQueueRule rule);
    }

    private SmartQueueRefiller() {
    }

    public static void refillAfterPlaybackEndedSynchronous(@NonNull Context context, @NonNull PodDBAdapter adapter) {
        if (!UserPreferences.isSmartQueueEnabled() || UserPreferences.isQueueLocked()
                || UserPreferences.getSmartQueueRules().isEmpty()) {
            return;
        }
        List<FeedItem> items = buildQueueItems(false);
        if (items.isEmpty()) {
            Log.d(TAG, "refillAfterPlaybackEnded: no items to add");
            return;
        }
        DBWriter.applySmartQueueFillSynchronous(context, adapter, items);
    }

    @NonNull
    public static List<FeedItem> buildQueueItems(boolean excludeCurrentlyPlaying) {
        return pickItemsForRules(SmartQueueRule::getEpisodeCount, excludeCurrentlyPlaying, true);
    }

    @NonNull
    public static List<FeedItem> getPredictiveDownloadCandidates() {
        if (!UserPreferences.isSmartQueueEnabled()
                || !UserPreferences.isSmartQueuePredictiveDownloadEnabled()
                || UserPreferences.getSmartQueueRules().isEmpty()) {
            return Collections.emptyList();
        }
        List<FeedItem> buffer = pickItemsForRules(rule -> rule.getEpisodeCount() * 2, false, false);
        List<FeedItem> undownloaded = new ArrayList<>();
        for (FeedItem item : buffer) {
            if (item.isDownloaded() || !item.hasMedia()) {
                continue;
            }
            Feed feed = item.getFeed();
            if (feed == null || feed.isLocalFeed()) {
                continue;
            }
            undownloaded.add(item);
        }
        return undownloaded;
    }

    @NonNull
    private static List<FeedItem> pickItemsForRules(@NonNull EpisodeCountProvider countProvider,
            boolean excludeCurrentlyPlaying, boolean applyDownloadedOnlyFilter) {
        long excludeItemId = -1;
        if (excludeCurrentlyPlaying) {
            FeedMedia playing = DBReader.getFeedMedia(PlaybackPreferences.getCurrentlyPlayingFeedMediaId());
            if (playing != null && playing.getItem() != null) {
                excludeItemId = playing.getItem().getId();
            }
        }

        List<FeedItem> result = new ArrayList<>();
        List<SmartQueueRule> rules = UserPreferences.getSmartQueueRules();
        for (SmartQueueRule rule : rules) {
            Feed feed = DBReader.getFeed(rule.getFeedId(), false, 0, Integer.MAX_VALUE);
            if (feed == null || feed.getItems() == null) {
                continue;
            }
            List<FeedItem> candidates = new ArrayList<>();
            for (FeedItem item : feed.getItems()) {
                if (item.isPlayed() || !item.hasMedia()) {
                    continue;
                }
                if (applyDownloadedOnlyFilter && UserPreferences.isSmartQueueDownloadedOnly()
                        && !item.isDownloaded()) {
                    continue;
                }
                if (item.getId() == excludeItemId) {
                    continue;
                }
                candidates.add(item);
            }
            if (candidates.isEmpty()) {
                continue;
            }
            SortOrder sortOrder = feed.getSortOrder();
            if (sortOrder == null) {
                sortOrder = SortOrder.GLOBAL_DEFAULT;
            }
            FeedItemPermutors.getPermutor(sortOrder).reorder(candidates);
            int count = Math.min(countProvider.getCount(rule), candidates.size());
            List<FeedItem> picked;
            if (rule.isFromTop()) {
                picked = candidates.subList(0, count);
            } else {
                picked = new ArrayList<>(candidates.subList(candidates.size() - count, candidates.size()));
                Collections.reverse(picked);
            }
            result.addAll(picked);
        }
        return result;
    }
}

package de.danoeh.antennapod.net.sync.service;

import androidx.collection.ArrayMap;
import androidx.core.util.Pair;

import java.util.Date;
import java.util.List;
import java.util.Map;

import de.danoeh.antennapod.net.sync.serviceinterface.EpisodeAction;

public class EpisodeActionFilter {

    public static Map<Pair<String, String>, EpisodeAction> getRemoteActionsOverridingLocalActions(
            List<EpisodeAction> remoteActions,
            List<EpisodeAction> queuedEpisodeActions) {
        Map<Pair<String, String>, EpisodeAction> remoteLatestRelevantActions =
                getLatestRelevantActionsPerEpisode(remoteActions);
        Map<Pair<String, String>, EpisodeAction> localMostRecentRelevantActions =
                getLatestRelevantActionsPerEpisode(queuedEpisodeActions);

        Map<Pair<String, String>, EpisodeAction> remoteActionsThatOverrideLocalActions = new ArrayMap<>();
        for (Map.Entry<Pair<String, String>, EpisodeAction> entry : remoteLatestRelevantActions.entrySet()) {
            EpisodeAction remoteAction = entry.getValue();
            EpisodeAction localMostRecent = localMostRecentRelevantActions.get(entry.getKey());
            if (secondActionOverridesFirstAction(remoteAction, localMostRecent)) {
                continue;
            }
            remoteActionsThatOverrideLocalActions.put(entry.getKey(), remoteAction);
        }

        return remoteActionsThatOverrideLocalActions;
    }

    private static Map<Pair<String, String>, EpisodeAction> getLatestRelevantActionsPerEpisode(
            List<EpisodeAction> actions) {
        Map<Pair<String, String>, EpisodeAction> latestRelevantAction = new ArrayMap<>();
        for (EpisodeAction action : actions) {
            if (!isRelevantForPlaybackState(action.getAction())) {
                continue;
            }
            Pair<String, String> key = new Pair<>(action.getPodcast(), action.getEpisode());
            EpisodeAction mostRecent = latestRelevantAction.get(key);
            if (shouldPreferAction(mostRecent, action)) {
                latestRelevantAction.put(key, action);
            }
        }
        return latestRelevantAction;
    }

    private static boolean shouldPreferAction(EpisodeAction current, EpisodeAction candidate) {
        if (current == null) {
            return true;
        }
        Date currentTimestamp = current.getTimestamp();
        Date candidateTimestamp = candidate.getTimestamp();
        if (candidateTimestamp == null && currentTimestamp != null) {
            return false;
        }
        if (candidateTimestamp != null && currentTimestamp == null) {
            return true;
        }
        if (candidateTimestamp != null && currentTimestamp != null) {
            int compare = candidateTimestamp.compareTo(currentTimestamp);
            if (compare > 0) {
                return true;
            }
            if (compare < 0) {
                return false;
            }
            return candidate.getAction() == EpisodeAction.Action.NEW
                    && current.getAction() == EpisodeAction.Action.PLAY;
        }
        return true;
    }

    private static boolean isRelevantForPlaybackState(EpisodeAction.Action action) {
        return action == EpisodeAction.Action.PLAY || action == EpisodeAction.Action.NEW;
    }

    private static boolean secondActionOverridesFirstAction(EpisodeAction firstAction,
                                                            EpisodeAction secondAction) {
        return secondAction != null
                && secondAction.getTimestamp() != null
                && (firstAction.getTimestamp() == null
                        || secondAction.getTimestamp().after(firstAction.getTimestamp()));
    }

}

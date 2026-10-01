package de.danoeh.antennapod.net.sync.service;


import androidx.core.util.Pair;

import junit.framework.TestCase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import de.danoeh.antennapod.net.sync.serviceinterface.EpisodeAction;


public class EpisodeActionFilterTest extends TestCase {

    public void testGetRemoteActionsHappeningAfterLocalActions() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date morning = format.parse("2021-01-01 08:00:00");
        Date lateMorning = format.parse("2021-01-01 09:00:00");

        List<EpisodeAction> episodeActions = new ArrayList<>();
        episodeActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.PLAY)
                .timestamp(morning)
                .position(10)
                .build()
        );
        episodeActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.PLAY)
                .timestamp(lateMorning)
                .position(20)
                .build()
        );
        episodeActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.2", EpisodeAction.Action.PLAY)
                .timestamp(morning)
                .position(5)
                .build()
        );

        Date morningFiveMinutesLater = format.parse("2021-01-01 08:05:00");
        List<EpisodeAction> remoteActions = new ArrayList<>();
        remoteActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.PLAY)
                .timestamp(morningFiveMinutesLater)
                .position(10)
                .build()
        );
        remoteActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.2", EpisodeAction.Action.PLAY)
                .timestamp(morningFiveMinutesLater)
                .position(5)
                .build()
        );

        Map<Pair<String, String>, EpisodeAction> uniqueList = EpisodeActionFilter
                .getRemoteActionsOverridingLocalActions(remoteActions, episodeActions);
        assertSame(1, uniqueList.size());
    }

    public void testGetRemoteActionsHappeningBeforeLocalActions() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date morning = format.parse("2021-01-01 08:00:00");
        Date lateMorning = format.parse("2021-01-01 09:00:00");

        List<EpisodeAction> episodeActions = new ArrayList<>();
        episodeActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.PLAY)
                .timestamp(morning)
                .position(10)
                .build()
        );
        episodeActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.PLAY)
                .timestamp(lateMorning)
                .position(20)
                .build()
        );
        episodeActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.2", EpisodeAction.Action.PLAY)
                .timestamp(morning)
                .position(5)
                .build()
        );

        Date morningFiveMinutesEarlier = format.parse("2021-01-01 07:55:00");
        List<EpisodeAction> remoteActions = new ArrayList<>();
        remoteActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.PLAY)
                .timestamp(morningFiveMinutesEarlier)
                .position(10)
                .build()
        );
        remoteActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.2", EpisodeAction.Action.PLAY)
                .timestamp(morningFiveMinutesEarlier)
                .position(5)
                .build()
        );

        Map<Pair<String, String>, EpisodeAction> uniqueList = EpisodeActionFilter
                .getRemoteActionsOverridingLocalActions(remoteActions, episodeActions);
        assertSame(0, uniqueList.size());
    }

    public void testGetMultipleRemoteActionsHappeningAfterLocalActions() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date morning = format.parse("2021-01-01 08:00:00");

        List<EpisodeAction> episodeActions = new ArrayList<>();
        episodeActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.PLAY)
                .timestamp(morning)
                .position(10)
                .build()
        );
        episodeActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.2", EpisodeAction.Action.PLAY)
                .timestamp(morning)
                .position(5)
                .build()
        );

        Date morningFiveMinutesLater = format.parse("2021-01-01 08:05:00");
        List<EpisodeAction> remoteActions = new ArrayList<>();
        remoteActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.PLAY)
                .timestamp(morningFiveMinutesLater)
                .position(10)
                .build()
        );
        remoteActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.2", EpisodeAction.Action.PLAY)
                .timestamp(morningFiveMinutesLater)
                .position(5)
                .build()
        );

        Map<Pair<String, String>, EpisodeAction> uniqueList = EpisodeActionFilter
                .getRemoteActionsOverridingLocalActions(remoteActions, episodeActions);
        assertEquals(2, uniqueList.size());
    }

    public void testGetMultipleRemoteActionsHappeningBeforeLocalActions() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date morning = format.parse("2021-01-01 08:00:00");

        List<EpisodeAction> episodeActions = new ArrayList<>();
        episodeActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.PLAY)
                .timestamp(morning)
                .position(10)
                .build()
        );
        episodeActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.2", EpisodeAction.Action.PLAY)
                .timestamp(morning)
                .position(5)
                .build()
        );

        Date morningFiveMinutesEarlier = format.parse("2021-01-01 07:55:00");
        List<EpisodeAction> remoteActions = new ArrayList<>();
        remoteActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.PLAY)
                .timestamp(morningFiveMinutesEarlier)
                .position(10)
                .build()
        );
        remoteActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.2", EpisodeAction.Action.PLAY)
                .timestamp(morningFiveMinutesEarlier)
                .position(5)
                .build()
        );

        Map<Pair<String, String>, EpisodeAction> uniqueList = EpisodeActionFilter
                .getRemoteActionsOverridingLocalActions(remoteActions, episodeActions);
        assertEquals(0, uniqueList.size());
    }

    public void testPresentRemoteTimestampOverridesMissingLocalTimestamp() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date arbitraryTime = format.parse("2021-01-01 08:00:00");

        List<EpisodeAction> episodeActions = new ArrayList<>();
        episodeActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.PLAY)
                // no timestamp
                .position(10)
                .build()
        );

        List<EpisodeAction> remoteActions = new ArrayList<>();
        remoteActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.PLAY)
                .timestamp(arbitraryTime)
                .position(10)
                .build()
        );

        Map<Pair<String, String>, EpisodeAction> uniqueList = EpisodeActionFilter
                .getRemoteActionsOverridingLocalActions(remoteActions, episodeActions);
        assertSame(1, uniqueList.size());
    }

    public void testLatestRemoteNewOverridesOlderRemotePlay() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date playedTime = format.parse("2021-01-01 08:00:00");
        Date unplayedTime = format.parse("2021-01-01 09:00:00");

        List<EpisodeAction> remoteActions = new ArrayList<>();
        remoteActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.PLAY)
                .timestamp(playedTime)
                .started(100)
                .position(100)
                .total(100)
                .build()
        );
        remoteActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.NEW)
                .timestamp(unplayedTime)
                .build()
        );

        Map<Pair<String, String>, EpisodeAction> uniqueList = EpisodeActionFilter
                .getRemoteActionsOverridingLocalActions(remoteActions, new ArrayList<>());
        assertEquals(1, uniqueList.size());
        assertEquals(EpisodeAction.Action.NEW, uniqueList.values().iterator().next().getAction());
    }

    public void testLatestRemotePlayOverridesOlderRemoteNew() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date unplayedTime = format.parse("2021-01-01 08:00:00");
        Date playedTime = format.parse("2021-01-01 09:00:00");

        List<EpisodeAction> remoteActions = new ArrayList<>();
        remoteActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.NEW)
                .timestamp(unplayedTime)
                .build()
        );
        remoteActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.PLAY)
                .timestamp(playedTime)
                .position(10)
                .build()
        );

        Map<Pair<String, String>, EpisodeAction> uniqueList = EpisodeActionFilter
                .getRemoteActionsOverridingLocalActions(remoteActions, new ArrayList<>());
        assertEquals(1, uniqueList.size());
        assertEquals(EpisodeAction.Action.PLAY, uniqueList.values().iterator().next().getAction());
    }

    public void testQueuedNewBlocksOlderRemotePlay() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date playedTime = format.parse("2021-01-01 08:00:00");
        Date unplayedTime = format.parse("2021-01-01 09:00:00");

        List<EpisodeAction> queuedEpisodeActions = new ArrayList<>();
        queuedEpisodeActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.NEW)
                .timestamp(unplayedTime)
                .build()
        );

        List<EpisodeAction> remoteActions = new ArrayList<>();
        remoteActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.PLAY)
                .timestamp(playedTime)
                .started(100)
                .position(100)
                .total(100)
                .build()
        );

        Map<Pair<String, String>, EpisodeAction> uniqueList = EpisodeActionFilter
                .getRemoteActionsOverridingLocalActions(remoteActions, queuedEpisodeActions);
        assertEquals(0, uniqueList.size());
    }

    public void testLatestRemoteNewOverridesRemotePlayWithSameTimestamp() throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date sameTime = format.parse("2021-01-01 08:00:00");

        List<EpisodeAction> remoteActions = new ArrayList<>();
        remoteActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.PLAY)
                .timestamp(sameTime)
                .position(100)
                .build()
        );
        remoteActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.NEW)
                .timestamp(sameTime)
                .build()
        );

        Map<Pair<String, String>, EpisodeAction> uniqueList = EpisodeActionFilter
                .getRemoteActionsOverridingLocalActions(remoteActions, new ArrayList<>());
        assertEquals(EpisodeAction.Action.NEW, uniqueList.values().iterator().next().getAction());
    }
}

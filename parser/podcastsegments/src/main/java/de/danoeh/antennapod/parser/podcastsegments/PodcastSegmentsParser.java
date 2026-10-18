package de.danoeh.antennapod.parser.podcastsegments;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PodcastSegmentsParser {
    private static final String TAG = "PodcastSegmentsParser";

    public static PodcastSegmentsIndex parse(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return PodcastSegmentsIndex.empty();
        }
        try {
            JSONObject root = new JSONObject(jsonStr);
            int version = root.optInt("version", 1);
            JSONObject episodes = root.optJSONObject("episodes");
            Map<String, Map<String, EpisodeSegments>> episodesByFeed = new HashMap<>();
            if (episodes != null) {
                Iterator<String> feedKeys = episodes.keys();
                while (feedKeys.hasNext()) {
                    String feedKey = feedKeys.next();
                    JSONObject feedEpisodes = episodes.optJSONObject(feedKey);
                    if (feedEpisodes == null) {
                        Log.w(TAG, "Skipping malformed feed entry: " + feedKey);
                        continue;
                    }
                    Map<String, EpisodeSegments> episodeMap = new HashMap<>();
                    Iterator<String> guidKeys = feedEpisodes.keys();
                    while (guidKeys.hasNext()) {
                        String guidKey = guidKeys.next();
                        JSONObject episodeObj = feedEpisodes.optJSONObject(guidKey);
                        if (episodeObj == null) {
                            Log.w(TAG, "Skipping malformed episode entry: " + feedKey + " / " + guidKey);
                            continue;
                        }
                        EpisodeSegments episodeSegments = parseEpisode(episodeObj);
                        if (episodeSegments != null) {
                            episodeMap.put(guidKey, episodeSegments);
                        }
                    }
                    if (!episodeMap.isEmpty()) {
                        episodesByFeed.put(feedKey, episodeMap);
                    }
                }
            }
            return new PodcastSegmentsIndex(version, episodesByFeed);
        } catch (JSONException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return PodcastSegmentsIndex.empty();
        }
    }

    private static EpisodeSegments parseEpisode(JSONObject episodeObj) {
        double mp3DurationSec = episodeObj.optDouble("mp3_duration_sec", 0);
        JSONArray segmentsArray = episodeObj.optJSONArray("segments");
        List<String> aliases = new ArrayList<>();
        JSONArray aliasesArray = episodeObj.optJSONArray("episode_url_aliases");
        if (aliasesArray != null) {
            for (int i = 0; i < aliasesArray.length(); i++) {
                String alias = aliasesArray.optString(i, null);
                if (alias != null && !alias.isEmpty()) {
                    aliases.add(alias);
                }
            }
        }
        if (segmentsArray == null) {
            return new EpisodeSegments(mp3DurationSec, List.of(), aliases);
        }
        List<PodcastSegment> segments = new ArrayList<>();
        for (int i = 0; i < segmentsArray.length(); i++) {
            JSONObject segmentObj = segmentsArray.optJSONObject(i);
            if (segmentObj == null) {
                continue;
            }
            String type = segmentObj.optString("type", "");
            double startSec = segmentObj.optDouble("start_sec", -1);
            double endSec = segmentObj.optDouble("end_sec", -1);
            if (endSec <= startSec || startSec < 0) {
                continue;
            }
            if (mp3DurationSec > 0 && endSec > mp3DurationSec) {
                endSec = mp3DurationSec;
            }
            if (endSec <= startSec) {
                continue;
            }
            String label = segmentObj.optString("label", null);
            if (label != null && label.isEmpty()) {
                label = null;
            }
            segments.add(new PodcastSegment(type, startSec, endSec, label));
        }
        return new EpisodeSegments(mp3DurationSec, segments, aliases);
    }

    public static String normalizeGuid(String guid) {
        if (guid == null) {
            return "";
        }
        return guid.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValidGuid(String guid) {
        return guid != null
                && !guid.trim().isEmpty()
                && !guid.equals("null");
    }
}

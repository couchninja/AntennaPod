package de.danoeh.antennapod.net.common;

import android.net.Uri;
import android.text.TextUtils;

import java.util.List;
import java.util.Locale;

public final class FeedUrlNormalizer {

    private FeedUrlNormalizer() {
    }

    public static String normalizeFeedUrlKey(String url) {
        url = UrlChecker.prepareUrl(url);
        Uri uri = Uri.parse(url);
        if (uri.getHost() == null) {
            return url;
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        List<String> pathSegments = normalizePathSegments(uri.getPathSegments());
        StringBuilder path = new StringBuilder();
        if (pathSegments.isEmpty()) {
            path.append("/");
        } else {
            for (String segment : pathSegments) {
                path.append("/").append(segment);
            }
        }
        String result = "https://" + host + path;
        if (!TextUtils.isEmpty(uri.getQuery())) {
            result += "?" + uri.getQuery();
        }
        return result;
    }

    private static List<String> normalizePathSegments(List<String> input) {
        List<String> result = new java.util.ArrayList<>();
        for (String string : input) {
            if (!TextUtils.isEmpty(string)) {
                result.add(string.toLowerCase(Locale.ROOT));
            }
        }
        return result;
    }
}

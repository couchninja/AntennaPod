package de.danoeh.antennapod.net.sync.service;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;

import de.danoeh.antennapod.net.common.AntennapodHttpClient;
import de.danoeh.antennapod.parser.podcastsegments.PodcastSegmentsRepository;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Downloads {@link PodcastSegmentsRepository#FILE_NAME} from the URL in settings.
 * <p>
 * Sync runs on gPodder refresh, app start (when stale), and when the URL changes. The JSON can be
 * tens of kilobytes and usually changes rarely, so we send If-None-Match / If-Modified-Since after
 * the first successful fetch. A 304 avoids re-downloading and re-parsing the same file.
 * Validators are stored in preferences because share URLs (e.g. Nextcloud) often send
 * {@code Cache-Control: no-store}, so the shared OkHttp disk cache is not enough.
 * <p>
 * If the server rejects conditional GET (412) or returns an error, we retry with a plain GET so
 * hosts without validator support still work; a 200 with a full body is always accepted.
 */
public class PodcastSegmentsDownloadWorker extends Worker {
    private static final String TAG = "PodcastSegmentsDl";
    private static final String WORK_ID = "PodcastSegmentsDownloadWorkId";
    private static final long STALE_MS = 24L * 60L * 60L * 1000L;

    public PodcastSegmentsDownloadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void enqueueImmediately(Context context) {
        String url = UserPreferences.getPodcastSegmentsUrl();
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(PodcastSegmentsDownloadWorker.class)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_ID, ExistingWorkPolicy.REPLACE, workRequest);
    }

    public static void enqueueIfStale(Context context) {
        String url = UserPreferences.getPodcastSegmentsUrl();
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        if (System.currentTimeMillis() - UserPreferences.getPodcastSegmentsLastDownload() < STALE_MS) {
            return;
        }
        enqueueImmediately(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        PodcastSegmentsRepository.init(getApplicationContext());
        String url = UserPreferences.getPodcastSegmentsUrl();
        if (url == null || url.trim().isEmpty()) {
            return Result.success();
        }
        String trimmedUrl = url.trim();
        File target = new File(getApplicationContext().getFilesDir(), PodcastSegmentsRepository.FILE_NAME);
        try {
            download(trimmedUrl, target);
            return Result.success();
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return Result.success();
        }
    }

    /**
     * Tries conditional GET only when we already have on-disk data and a validator from that URL;
     * otherwise a conditional request could not be validated and would be pointless.
     */
    private void download(String url, File target) throws IOException {
        String validator = UserPreferences.getPodcastSegmentsValidator(url);
        if (!validator.isEmpty() && target.exists()) {
            try (Response response = newCall(url, validator)) {
                if (handleResponse(response, url, target, true)) {
                    return;
                }
                if (response.code() == HttpURLConnection.HTTP_PRECON_FAILED) {
                    Log.d(TAG, "Conditional request not supported, retrying without validator");
                }
            }
        }
        try (Response response = newCall(url, null)) {
            if (!handleResponse(response, url, target, false)) {
                Log.w(TAG, "Download failed with code " + response.code());
            }
        }
    }

    private static Response newCall(String url, String validator) throws IOException {
        Request.Builder builder = new Request.Builder().url(url);
        if (validator != null && !validator.isEmpty()) {
            addConditionalHeader(builder, validator);
        }
        return AntennapodHttpClient.getHttpClient().newCall(builder.build()).execute();
    }

    /**
     * Same convention as {@code HttpDownloader}: RFC date → If-Modified-Since, else If-None-Match,
     * so one stored string works whether the server returned Last-Modified or ETag.
     */
    private static void addConditionalHeader(Request.Builder builder, String validator) {
        if (validator.length() > 2 && Character.isLetter(validator.charAt(0)) && validator.charAt(1) == ',') {
            builder.header("If-Modified-Since", validator);
        } else {
            builder.header("If-None-Match", validator);
        }
    }

    /**
     * @param conditional if true and response is 412, caller retries without validators
     * @return true when the sync attempt finished (304 or body written)
     */
    private boolean handleResponse(Response response, String url, File target, boolean conditional)
            throws IOException {
        int code = response.code();
        if (code == HttpURLConnection.HTTP_NOT_MODIFIED) {
            // Keep existing file; still refresh last-download so enqueueIfStale does not hammer the server.
            Log.d(TAG, "Segments JSON not modified");
            UserPreferences.setPodcastSegmentsLastDownload(System.currentTimeMillis());
            return true;
        }
        if (conditional && code == HttpURLConnection.HTTP_PRECON_FAILED) {
            return false;
        }
        if (!response.isSuccessful() || response.body() == null) {
            return false;
        }
        String json = response.body().string();
        PodcastSegmentsRepository.writeJsonAtomically(json, target);
        String newValidator = response.header("ETag");
        if (newValidator == null || newValidator.isEmpty()) {
            newValidator = response.header("Last-Modified");
        }
        if (newValidator != null && !newValidator.isEmpty()) {
            UserPreferences.setPodcastSegmentsValidator(url, newValidator);
        }
        UserPreferences.setPodcastSegmentsLastDownload(System.currentTimeMillis());
        PodcastSegmentsRepository.reloadFromDisk();
        return true;
    }
}

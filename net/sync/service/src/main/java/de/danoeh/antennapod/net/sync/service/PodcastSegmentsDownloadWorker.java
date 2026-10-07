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

import de.danoeh.antennapod.net.common.AntennapodHttpClient;
import de.danoeh.antennapod.parser.podcastsegments.PodcastSegmentsRepository;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import okhttp3.Request;
import okhttp3.Response;

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
        Request request = new Request.Builder().url(url.trim()).build();
        try (Response response = AntennapodHttpClient.getHttpClient().newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                Log.w(TAG, "Download failed with code " + response.code());
                return Result.success();
            }
            String json = response.body().string();
            File target = new File(getApplicationContext().getFilesDir(), PodcastSegmentsRepository.FILE_NAME);
            PodcastSegmentsRepository.writeJsonAtomically(json, target);
            UserPreferences.setPodcastSegmentsLastDownload(System.currentTimeMillis());
            PodcastSegmentsRepository.reloadFromDisk();
            return Result.success();
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return Result.success();
        }
    }
}

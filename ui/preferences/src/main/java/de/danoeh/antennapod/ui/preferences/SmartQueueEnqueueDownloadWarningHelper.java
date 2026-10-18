package de.danoeh.antennapod.ui.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.i18n.R;

public final class SmartQueueEnqueueDownloadWarningHelper {
    public static final String PREF_WARNING = "prefSmartQueuePredictiveDownloadEnqueueWarning";
    private static final String PREF_ENQUEUE_DOWNLOADED = "prefEnqueueDownloaded";

    private SmartQueueEnqueueDownloadWarningHelper() {
    }

    public static void setup(PreferenceFragmentCompat fragment) {
        Preference warning = fragment.findPreference(PREF_WARNING);
        if (warning != null) {
            warning.setIcon(de.danoeh.antennapod.ui.common.R.drawable.ic_warning);
        }
        updateWarnings(fragment);
        SharedPreferences prefs = fragment.getPreferenceManager().getSharedPreferences();
        SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> {
            if (UserPreferences.PREF_SMART_QUEUE_PREDICTIVE_DOWNLOAD.equals(key)
                    || PREF_ENQUEUE_DOWNLOADED.equals(key)) {
                updateWarnings(fragment);
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(listener);
        fragment.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                prefs.unregisterOnSharedPreferenceChangeListener(listener);
            }
        });
    }

    public static void updateWarnings(PreferenceFragmentCompat fragment) {
        boolean show = UserPreferences.isSmartQueuePredictiveDownloadEnabled()
                && UserPreferences.enqueueDownloadedEpisodes();
        Preference warning = fragment.findPreference(PREF_WARNING);
        if (warning != null) {
            warning.setSummary(buildWarningSummary(fragment.requireContext()));
            warning.setVisible(show);
        }
    }

    private static CharSequence buildWarningSummary(Context context) {
        return context.getString(R.string.pref_smart_queue_predictive_download_enqueue_warning,
                context.getString(R.string.pref_smart_queue_predictive_download_title),
                context.getString(R.string.pref_enqueue_downloaded_title));
    }
}

package de.danoeh.antennapod.ui.screen.feed.preferences;

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.FeedPreferences;

public abstract class FeedKeepDownloadDialog extends MaterialAlertDialogBuilder {

    public FeedKeepDownloadDialog(Context context, int episodeCount, boolean fromTop) {
        super(context);
        setTitle(R.string.pref_feed_keep_download_title);
        View rootView = View.inflate(context, R.layout.feed_pref_keep_download_dialog, null);
        setView(rootView);

        MaterialSwitch enabledSwitch = rootView.findViewById(R.id.enabledSwitch);
        LinearLayout optionsLayout = rootView.findViewById(R.id.optionsLayout);
        Spinner directionSpinner = rootView.findViewById(R.id.directionSpinner);
        Spinner episodeCountSpinner = rootView.findViewById(R.id.episodeCountSpinner);

        ArrayAdapter<String> directionAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item,
                new String[]{
                        context.getString(R.string.smart_queue_episode_direction_first),
                        context.getString(R.string.smart_queue_episode_direction_last)
                });
        directionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        directionSpinner.setAdapter(directionAdapter);

        List<String> countLabels = new ArrayList<>();
        for (int option : FeedPreferences.KEEP_DOWNLOAD_EPISODE_COUNT_OPTIONS) {
            if (option == FeedPreferences.KEEP_DOWNLOAD_ALL) {
                countLabels.add(context.getString(R.string.pref_feed_keep_download_all_episodes));
            } else {
                countLabels.add(context.getResources().getQuantityString(R.plurals.num_episodes, option, option));
            }
        }
        ArrayAdapter<String> countAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, countLabels);
        countAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        episodeCountSpinner.setAdapter(countAdapter);

        int defaultCountIndex = FeedPreferences.getKeepDownloadOptionIndex(FeedPreferences.KEEP_DOWNLOAD_DEFAULT);
        boolean enabled = episodeCount != FeedPreferences.KEEP_DOWNLOAD_DISABLED;
        enabledSwitch.setChecked(enabled);
        optionsLayout.setVisibility(enabled ? View.VISIBLE : View.GONE);
        enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                optionsLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        if (enabled) {
            directionSpinner.setSelection(fromTop ? 0 : 1);
            episodeCountSpinner.setSelection(FeedPreferences.getKeepDownloadOptionIndex(episodeCount));
        } else {
            directionSpinner.setSelection(1);
            episodeCountSpinner.setSelection(defaultCountIndex);
        }

        setNegativeButton(R.string.cancel_label, null);
        setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            if (!enabledSwitch.isChecked()) {
                onConfirmed(FeedPreferences.KEEP_DOWNLOAD_DISABLED, false);
                return;
            }
            boolean selectedFromTop = directionSpinner.getSelectedItemPosition() == 0;
            int selectedCount = FeedPreferences.KEEP_DOWNLOAD_EPISODE_COUNT_OPTIONS[
                    episodeCountSpinner.getSelectedItemPosition()];
            onConfirmed(selectedCount, selectedFromTop);
        });
    }

    protected abstract void onConfirmed(int episodeCount, boolean fromTop);
}

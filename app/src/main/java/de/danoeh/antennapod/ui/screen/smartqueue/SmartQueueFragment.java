package de.danoeh.antennapod.ui.screen.smartqueue;

import android.graphics.Canvas;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.collection.ArrayMap;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.ui.CoverLoader;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.SmartQueueRule;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import de.danoeh.antennapod.ui.screen.drawer.NavigationNames;
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class SmartQueueFragment extends Fragment {
    public static final String TAG = "SmartQueueFragment";

    private static final float DISABLED_ALPHA = 0.4f;

    private final List<SmartQueueRuleEntry> entries = new ArrayList<>();
    private SmartQueueRulesAdapter adapter;
    private Disposable disposable;
    private ItemTouchHelper itemTouchHelper;
    private RecyclerView recyclerView;
    private FloatingActionButton addButton;
    private View emptyView;
    private TextView emptyViewTitle;
    private TextView emptyViewMessage;
    private ImageView emptyViewIcon;
    private boolean smartQueueEnabled;
    private boolean hasSubscribedFeeds;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.smart_queue_settings_fragment, container, false);

        MaterialToolbar toolbar = root.findViewById(R.id.toolbar);
        ((MainActivity) requireActivity()).setupToolbarToggle(toolbar, false);

        MaterialSwitch enabledSwitch = root.findViewById(R.id.enabledSwitch);
        enabledSwitch.setChecked(UserPreferences.isSmartQueueEnabled());
        enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            UserPreferences.setSmartQueueEnabled(isChecked);
            updateEnabledState(isChecked);
        });

        recyclerView = root.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SmartQueueRulesAdapter();
        recyclerView.setAdapter(adapter);

        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder) {
                if (!smartQueueEnabled) {
                    return 0;
                }
                return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();
                if (from < 0 || to < 0 || from >= entries.size() || to >= entries.size()) {
                    return false;
                }
                entries.add(to, entries.remove(from));
                adapter.notifyItemMoved(from, to);
                persistRules();
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getBindingAdapterPosition();
                if (pos < 0 || pos >= entries.size()) {
                    return;
                }
                entries.remove(pos);
                adapter.notifyItemRemoved(pos);
                persistRules();
                updateUiState();
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    int actionColor = ThemeUtils.getColorFromAttr(recyclerView.getContext(), R.attr.icon_red);
                    int backgroundColor = ThemeUtils.getColorFromAttr(recyclerView.getContext(),
                            R.attr.background_elevated);
                    new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY,
                            actionState, isCurrentlyActive)
                            .addSwipeRightActionIcon(R.drawable.ic_delete)
                            .addSwipeLeftActionIcon(R.drawable.ic_delete)
                            .addSwipeRightBackgroundColor(backgroundColor)
                            .addSwipeLeftBackgroundColor(backgroundColor)
                            .setActionIconTint(actionColor)
                            .create()
                            .decorate();
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);

        addButton = root.findViewById(R.id.addPodcastButton);
        addButton.setOnClickListener(v -> showFeedPicker());

        emptyView = root.findViewById(R.id.emptyView);
        emptyViewTitle = emptyView.findViewById(de.danoeh.antennapod.ui.common.R.id.emptyViewTitle);
        emptyViewMessage = emptyView.findViewById(de.danoeh.antennapod.ui.common.R.id.emptyViewMessage);
        emptyViewIcon = emptyView.findViewById(de.danoeh.antennapod.ui.common.R.id.emptyViewIcon);

        updateEnabledState(UserPreferences.isSmartQueueEnabled());
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        loadEntries();
    }

    private void updateEnabledState(boolean enabled) {
        smartQueueEnabled = enabled;
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        updateUiState();
    }

    private void updateUiState() {
        if (recyclerView == null || emptyView == null) {
            return;
        }
        boolean showNoSubscriptions = !hasSubscribedFeeds;
        boolean showEmptySmartQueue = hasSubscribedFeeds && entries.isEmpty();
        boolean showSmartQueueOff = hasSubscribedFeeds && !smartQueueEnabled && !entries.isEmpty();
        if (showNoSubscriptions) {
            emptyView.setVisibility(View.VISIBLE);
            emptyViewTitle.setText(R.string.no_subscriptions_head_label);
            emptyViewMessage.setText(R.string.smart_queue_no_subscriptions_label);
            emptyViewMessage.setVisibility(View.VISIBLE);
            emptyViewIcon.setImageResource(R.drawable.ic_subscriptions);
            emptyViewIcon.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.INVISIBLE);
        } else if (showEmptySmartQueue) {
            emptyView.setVisibility(View.VISIBLE);
            setSmartQueueEmptyIcon();
            emptyViewMessage.setVisibility(View.VISIBLE);
            if (smartQueueEnabled) {
                emptyViewTitle.setText(R.string.smart_queue_empty_head_label);
                emptyViewMessage.setText(R.string.smart_queue_empty_on_label);
            } else {
                emptyViewTitle.setText(R.string.smart_queue_off_label);
                emptyViewMessage.setText(R.string.smart_queue_empty_off_label);
            }
            recyclerView.setVisibility(View.INVISIBLE);
        } else if (showSmartQueueOff) {
            emptyView.setVisibility(View.VISIBLE);
            emptyViewTitle.setText(R.string.smart_queue_off_label);
            emptyViewMessage.setVisibility(View.GONE);
            setSmartQueueEmptyIcon();
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setAlpha(DISABLED_ALPHA);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setAlpha(smartQueueEnabled ? 1f : DISABLED_ALPHA);
        }
        if (addButton != null) {
            boolean addEnabled = hasSubscribedFeeds && smartQueueEnabled;
            addButton.setEnabled(addEnabled);
            addButton.setAlpha(addEnabled ? 1f : DISABLED_ALPHA);
        }
    }

    private void setSmartQueueEmptyIcon() {
        emptyViewIcon.setImageResource(NavigationNames.getDrawable(TAG));
        emptyViewIcon.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    private void loadEntries() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = io.reactivex.rxjava3.core.Single.fromCallable(() -> {
            List<SmartQueueRule> rules = UserPreferences.getSmartQueueRules();
            Map<Long, Feed> feedById = new ArrayMap<>();
            boolean subscribedFeeds = false;
            for (Feed feed : DBReader.getFeedList()) {
                feedById.put(feed.getId(), feed);
                if (feed.getState() == Feed.STATE_SUBSCRIBED) {
                    subscribedFeeds = true;
                }
            }
            List<SmartQueueRuleEntry> loaded = new ArrayList<>();
            for (SmartQueueRule rule : rules) {
                loaded.add(new SmartQueueRuleEntry(rule, feedById.get(rule.getFeedId())));
            }
            return new LoadResult(loaded, subscribedFeeds);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    entries.clear();
                    entries.addAll(result.entries);
                    hasSubscribedFeeds = result.hasSubscribedFeeds;
                    adapter.notifyDataSetChanged();
                    updateUiState();
                }, Throwable::printStackTrace);
    }

    private void persistRules() {
        List<SmartQueueRule> rules = new ArrayList<>();
        for (SmartQueueRuleEntry entry : entries) {
            rules.add(entry.rule);
        }
        UserPreferences.setSmartQueueRules(rules);
    }

    private void showFeedPicker() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = io.reactivex.rxjava3.core.Single.fromCallable(() -> {
            Set<Long> usedFeedIds = new HashSet<>();
            for (SmartQueueRuleEntry entry : entries) {
                usedFeedIds.add(entry.rule.getFeedId());
            }
            List<Feed> pickable = new ArrayList<>();
            for (Feed feed : DBReader.getFeedList()) {
                if (feed.getState() != Feed.STATE_SUBSCRIBED) {
                    continue;
                }
                if (usedFeedIds.contains(feed.getId())) {
                    continue;
                }
                pickable.add(feed);
            }
            pickable.sort((f1, f2) -> {
                String t1 = f1.getTitle() != null ? f1.getTitle() : "";
                String t2 = f2.getTitle() != null ? f2.getTitle() : "";
                return t1.compareToIgnoreCase(t2);
            });
            return pickable;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pickable -> {
                    if (pickable.isEmpty()) {
                        return;
                    }
                    ArrayAdapter<Feed> feedAdapter = new ArrayAdapter<>(requireContext(),
                            R.layout.subscription_list_item, pickable) {
                        @NonNull
                        @Override
                        public View getView(int position, @Nullable View convertView,
                                            @NonNull ViewGroup parent) {
                            View view = convertView;
                            if (view == null) {
                                view = LayoutInflater.from(getContext())
                                        .inflate(R.layout.subscription_list_item, parent, false);
                            }
                            Feed feed = getItem(position);
                            TextView title = view.findViewById(R.id.titleLabel);
                            title.setText(feed.getTitle());
                            view.findViewById(R.id.countViewPill).setVisibility(View.GONE);
                            view.findViewById(R.id.errorIcon).setVisibility(View.GONE);
                            ImageView coverImage = view.findViewById(R.id.coverImage);
                            coverImage.setContentDescription(feed.getTitle());
                            TextView fallbackTitle = view.findViewById(R.id.fallbackTitleLabel);
                            fallbackTitle.setVisibility(View.GONE);
                            new CoverLoader()
                                    .withUri(feed.getImageUrl())
                                    .withCoverView(coverImage)
                                    .load();
                            return view;
                        }
                    };
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.smart_queue_pick_podcast)
                            .setAdapter(feedAdapter, (dialog, which) -> {
                                Feed feed = pickable.get(which);
                                entries.add(new SmartQueueRuleEntry(
                                        new SmartQueueRule(feed.getId(), false, 1), feed));
                                adapter.notifyItemInserted(entries.size() - 1);
                                persistRules();
                                updateUiState();
                            })
                            .show();
                }, Throwable::printStackTrace);
    }

    private static final class LoadResult {
        final List<SmartQueueRuleEntry> entries;
        final boolean hasSubscribedFeeds;

        LoadResult(List<SmartQueueRuleEntry> entries, boolean hasSubscribedFeeds) {
            this.entries = entries;
            this.hasSubscribedFeeds = hasSubscribedFeeds;
        }
    }

    private static final class SmartQueueRuleEntry {
        final SmartQueueRule rule;
        final Feed feed;

        SmartQueueRuleEntry(SmartQueueRule rule, Feed feed) {
            this.rule = rule;
            this.feed = feed;
        }
    }

    private class SmartQueueRulesAdapter extends RecyclerView.Adapter<SmartQueueRulesAdapter.RuleViewHolder> {
        @NonNull
        @Override
        public RuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.smart_queue_rule_item, parent, false);
            return new RuleViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull RuleViewHolder holder, int position) {
            holder.bind(entries.get(position), position);
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }

        class RuleViewHolder extends RecyclerView.ViewHolder {
            private final ImageView dragHandle;
            private final ImageView coverImage;
            private final TextView feedTitle;
            private final Spinner directionSpinner;
            private final Spinner episodeCountSpinner;
            private boolean bindingSpinners;

            RuleViewHolder(@NonNull View itemView) {
                super(itemView);
                dragHandle = itemView.findViewById(R.id.dragHandle);
                coverImage = itemView.findViewById(R.id.coverImage);
                feedTitle = itemView.findViewById(R.id.feedTitle);
                directionSpinner = itemView.findViewById(R.id.directionSpinner);
                episodeCountSpinner = itemView.findViewById(R.id.episodeCountSpinner);

                ArrayAdapter<String> directionAdapter = new ArrayAdapter<>(itemView.getContext(),
                        android.R.layout.simple_spinner_item,
                        new String[]{
                                itemView.getContext().getString(R.string.smart_queue_episode_direction_first),
                                itemView.getContext().getString(R.string.smart_queue_episode_direction_last)
                        });
                directionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                directionSpinner.setAdapter(directionAdapter);

                List<String> counts = new ArrayList<>();
                for (int i = SmartQueueRule.MIN_EPISODE_COUNT; i <= SmartQueueRule.MAX_EPISODE_COUNT; i++) {
                    counts.add(itemView.getResources().getQuantityString(R.plurals.num_episodes, i, i));
                }
                ArrayAdapter<String> countAdapter = new ArrayAdapter<>(itemView.getContext(),
                        android.R.layout.simple_spinner_item, counts);
                countAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                episodeCountSpinner.setAdapter(countAdapter);

                AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int index, long id) {
                        if (bindingSpinners) {
                            return;
                        }
                        int pos = getBindingAdapterPosition();
                        if (pos < 0 || pos >= entries.size()) {
                            return;
                        }
                        SmartQueueRuleEntry current = entries.get(pos);
                        current.rule.setFromTop(directionSpinner.getSelectedItemPosition() == 0);
                        current.rule.setEpisodeCount(episodeCountSpinner.getSelectedItemPosition() + 1);
                        persistRules();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                };
                directionSpinner.setOnItemSelectedListener(spinnerListener);
                episodeCountSpinner.setOnItemSelectedListener(spinnerListener);

                dragHandle.setOnTouchListener((v, event) -> {
                    if (!smartQueueEnabled) {
                        return false;
                    }
                    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        itemTouchHelper.startDrag(this);
                    }
                    return false;
                });

                itemView.setOnLongClickListener(v -> {
                    if (!smartQueueEnabled) {
                        return false;
                    }
                    int pos = getBindingAdapterPosition();
                    if (pos < 0 || pos >= entries.size()) {
                        return false;
                    }
                    PopupMenu menu = new PopupMenu(v.getContext(), v);
                    menu.inflate(R.menu.smart_queue_rule_context);
                    menu.setOnMenuItemClickListener(item -> onContextItem(item, pos));
                    menu.show();
                    return true;
                });
            }

            private boolean onContextItem(MenuItem item, int position) {
                int itemId = item.getItemId();
                if (itemId == R.id.move_to_top_item) {
                    if (position > 0) {
                        entries.add(0, entries.remove(position));
                        adapter.notifyItemMoved(position, 0);
                        persistRules();
                    }
                    return true;
                } else if (itemId == R.id.delete_item) {
                    deleteEntry(position);
                    return true;
                }
                return false;
            }

            private void deleteEntry(int position) {
                entries.remove(position);
                adapter.notifyItemRemoved(position);
                persistRules();
                updateUiState();
            }

            void bind(SmartQueueRuleEntry entry, int position) {
                String title = entry.feed != null ? entry.feed.getTitle() : String.valueOf(entry.rule.getFeedId());
                feedTitle.setText(title);
                if (entry.feed != null) {
                    coverImage.setContentDescription(entry.feed.getTitle());
                    new CoverLoader()
                            .withUri(entry.feed.getImageUrl())
                            .withCoverView(coverImage)
                            .load();
                } else {
                    coverImage.setImageResource(android.R.color.transparent);
                }

                bindingSpinners = true;
                directionSpinner.setSelection(entry.rule.isFromTop() ? 0 : 1);
                episodeCountSpinner.setSelection(entry.rule.getEpisodeCount() - 1);
                bindingSpinners = false;

                directionSpinner.setEnabled(smartQueueEnabled);
                episodeCountSpinner.setEnabled(smartQueueEnabled);
                dragHandle.setEnabled(smartQueueEnabled);
            }
        }
    }
}

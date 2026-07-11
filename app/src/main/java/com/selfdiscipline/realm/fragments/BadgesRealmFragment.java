package com.selfdiscipline.realm.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.selfdiscipline.realm.R;
import com.selfdiscipline.realm.RecordDetailActivity;
import com.selfdiscipline.realm.RecordListActivity;
import com.selfdiscipline.realm.data.*;
import com.selfdiscipline.realm.engine.*;
import com.selfdiscipline.realm.model.*;
import com.selfdiscipline.realm.util.ViewUtils;

public class BadgesRealmFragment extends BaseFragmentHelper {
    private AppRepository repo;
    private AppState state;
    private TextView realmText;
    private ProgressBar progress;
    private LinearLayout wall, logs;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup c, Bundle b) {
        View root = inflater.inflate(R.layout.fragment_badges_realm, c, false);
        repo = new AppRepository(getActivity());
        state = repo.load();
        realmText = root.findViewById(R.id.text_realm_detail);
        progress = root.findViewById(R.id.progress_realm);
        wall = root.findViewById(R.id.badge_wall);
        logs = root.findViewById(R.id.exp_log_list);
        root.findViewById(R.id.button_view_all_exp_logs).setOnClickListener(v -> RecordListActivity.open(getActivity(), RecordListActivity.TYPE_EXP_LOGS));
        render();
        return root;
    }

    @Override public void onResume() { super.onResume(); if (repo != null) { state = repo.load(); render(); } }

    private void render() {
        int xp = StatsEngine.totalXp(state);
        RealmLevel realm = RealmCatalog.current(xp);
        realmText.setBackgroundResource(realmBackground(realm));
        String next = realm.isCap() ? getString(R.string.text_realm_cap) : getString(R.string.format_next_xp, realm.nextXp - xp);
        realmText.setText(getString(R.string.format_realm_card, getString(realm.nameRes), getString(realm.descRes), xp, next));
        progress.setProgress(realm.isCap() ? 100 : (int)((xp - realm.minXp) * 100.0 / (realm.nextXp - realm.minXp)));
        wall.removeAllViews();
        int index = 0;
        for (Badge b : BadgeCatalog.all()) {
            boolean unlocked = state.isBadgeUnlocked(b.id) || RewardEngine.meets(state, b);
            int count = StatsEngine.badgeEarnCount(state, b);
            String text = getString(R.string.format_badge_card_v2,
                    getString(b.nameRes), getString(b.rankRes),
                    unlocked ? getString(R.string.text_unlocked) : getString(R.string.text_locked),
                    b.xpReward,
                    getString(R.string.label_badge_count, count));
            final int detailIndex = index;
            LinearLayout card = ViewUtils.iconCard(getActivity(), badgeIcon(b.rankRes), text + "\n" + getString(R.string.label_tap_detail), v -> RecordDetailActivity.open(getActivity(), RecordDetailActivity.TYPE_BADGE, detailIndex));
            card.setBackgroundResource(unlocked ? R.drawable.bg_badge_unlocked : R.drawable.bg_badge_locked);
            wall.addView(card);
            index++;
        }
        logs.removeAllViews();
        if (state.expLogs.isEmpty()) { logs.addView(ViewUtils.card(getActivity(), getString(R.string.empty_exp_logs))); return; }
        for (int i = 0; i < Math.min(10, state.expLogs.size()); i++) {
            ExperienceLog l = state.expLogs.get(i);
            final int detailIndex = i;
            logs.addView(ViewUtils.iconCard(getActivity(), R.drawable.ic_xp, getString(R.string.format_exp_log, l.date, l.points, l.source) + "\n" + getString(R.string.label_tap_detail), v -> RecordDetailActivity.open(getActivity(), RecordDetailActivity.TYPE_EXP, detailIndex)));
        }
    }

    private int badgeIcon(int rankRes) {
        if (rankRes == R.string.rank_silver) return R.drawable.ic_badge_silver;
        if (rankRes == R.string.rank_gold) return R.drawable.ic_badge_gold;
        if (rankRes == R.string.rank_platinum) return R.drawable.ic_badge_platinum;
        if (rankRes == R.string.rank_diamond) return R.drawable.ic_badge_diamond;
        return R.drawable.ic_badge_bronze;
    }
}

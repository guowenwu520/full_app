package com.selfdiscipline.realm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.selfdiscipline.realm.data.AppRepository;
import com.selfdiscipline.realm.data.BadgeCatalog;
import com.selfdiscipline.realm.engine.RewardEngine;
import com.selfdiscipline.realm.engine.StatsEngine;
import com.selfdiscipline.realm.model.*;

public class RecordDetailActivity extends Activity {
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_INDEX = "index";
    public static final String TYPE_BOOK = "book";
    public static final String TYPE_EXERCISE = "exercise";
    public static final String TYPE_WEIGHT = "weight";
    public static final String TYPE_SLEEP = "sleep";
    public static final String TYPE_WORD = "word";
    public static final String TYPE_DIARY = "diary";
    public static final String TYPE_EXP = "exp";
    public static final String TYPE_BADGE = "badge";

    public static void open(Context c, String type, int index) {
        Intent i = new Intent(c, RecordDetailActivity.class);
        i.putExtra(EXTRA_TYPE, type);
        i.putExtra(EXTRA_INDEX, index);
        c.startActivity(i);
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(getResources().getColor(R.color.color_surface));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(R.layout.activity_record_detail);
        String type = getIntent().getStringExtra(EXTRA_TYPE);
        int index = getIntent().getIntExtra(EXTRA_INDEX, -1);
        AppState state = new AppRepository(this).load();
        TextView title = findViewById(R.id.text_detail_title);
        TextView body = findViewById(R.id.text_detail_body);
        ImageView icon = findViewById(R.id.image_detail_icon);
        title.setText(titleFor(type));
        icon.setImageResource(iconFor(type, index));
        body.setText(detailFor(state, type, index));
    }

    private String titleFor(String type) {
        if (TYPE_BOOK.equals(type)) return getString(R.string.label_record_type_books);
        if (TYPE_EXERCISE.equals(type)) return getString(R.string.label_record_type_exercises);
        if (TYPE_WEIGHT.equals(type)) return getString(R.string.label_record_type_weights);
        if (TYPE_SLEEP.equals(type)) return getString(R.string.label_record_type_sleeps);
        if (TYPE_WORD.equals(type)) return getString(R.string.label_record_type_words);
        if (TYPE_DIARY.equals(type)) return getString(R.string.label_record_type_diaries);
        if (TYPE_EXP.equals(type)) return getString(R.string.label_record_type_exp_logs);
        if (TYPE_BADGE.equals(type)) return getString(R.string.title_badge_detail);
        return getString(R.string.title_record_detail);
    }

    private int iconFor(String type, int index) {
        if (TYPE_BOOK.equals(type)) return R.drawable.ic_nav_reading;
        if (TYPE_EXERCISE.equals(type) || TYPE_WEIGHT.equals(type) || TYPE_SLEEP.equals(type)) return R.drawable.ic_nav_sport;
        if (TYPE_WORD.equals(type)) return R.drawable.ic_nav_word;
        if (TYPE_DIARY.equals(type)) return R.drawable.ic_nav_diary;
        if (TYPE_EXP.equals(type)) return R.drawable.ic_xp;
        if (TYPE_BADGE.equals(type)) {
            Badge badge = safeBadge(index);
            if (badge != null) return badgeIcon(badge.rankRes);
        }
        return R.drawable.ic_nav_overview;
    }

    private String detailFor(AppState state, String type, int index) {
        try {
            if (TYPE_BOOK.equals(type) && index >= 0 && index < state.books.size()) {
                Book r = state.books.get(index);
                return getString(R.string.format_book_detail, r.title, empty(r.author), r.currentPage, empty(r.fullReview), pageNotes(r));
            }
            if (TYPE_EXERCISE.equals(type) && index >= 0 && index < state.exercises.size()) {
                ExerciseRecord r = state.exercises.get(index);
                return getString(R.string.format_exercise_detail, r.date, r.content, r.calories);
            }
            if (TYPE_WEIGHT.equals(type) && index >= 0 && index < state.weights.size()) {
                WeightRecord r = state.weights.get(index);
                return getString(R.string.format_weight_detail, r.date, r.weight);
            }
            if (TYPE_SLEEP.equals(type) && index >= 0 && index < state.sleeps.size()) {
                SleepRecord r = state.sleeps.get(index);
                return getString(R.string.format_sleep_detail, r.date, r.sleepTime, r.wakeTime, r.passed ? getString(R.string.text_sleep_pass) : getString(R.string.text_sleep_fail));
            }
            if (TYPE_WORD.equals(type) && index >= 0 && index < state.words.size()) {
                WordEntry r = state.words.get(index);
                return getString(R.string.format_word_detail, r.word, r.meaning, r.createdDate, r.correctCount, r.wrongCount, emptyDash(r.lastTestDate));
            }
            if (TYPE_DIARY.equals(type) && index >= 0 && index < state.diaries.size()) {
                DiaryRecord r = state.diaries.get(index);
                return getString(R.string.format_diary_detail, r.date, r.title, r.broken ? getString(R.string.text_broken) : getString(R.string.text_not_broken), r.body);
            }
            if (TYPE_EXP.equals(type) && index >= 0 && index < state.expLogs.size()) {
                ExperienceLog r = state.expLogs.get(index);
                return getString(R.string.format_exp_detail, r.date, r.source, r.points, r.key);
            }
            if (TYPE_BADGE.equals(type)) {
                Badge badge = safeBadge(index);
                if (badge != null) {
                    boolean unlocked = state.isBadgeUnlocked(badge.id) || RewardEngine.meets(state, badge);
                    int count = StatsEngine.badgeEarnCount(state, badge);
                    return getString(R.string.format_badge_detail,
                            getString(badge.nameRes), getString(badge.rankRes),
                            unlocked ? getString(R.string.text_unlocked) : getString(R.string.text_locked),
                            badge.target, count, badge.xpReward, getString(badge.descRes));
                }
            }
        } catch (Exception ignored) {}
        return getString(R.string.text_no_detail);
    }

    private Badge safeBadge(int index) {
        java.util.List<Badge> all = BadgeCatalog.all();
        if (index >= 0 && index < all.size()) return all.get(index);
        return null;
    }

    private int badgeIcon(int rankRes) {
        if (rankRes == R.string.rank_silver) return R.drawable.ic_badge_silver;
        if (rankRes == R.string.rank_gold) return R.drawable.ic_badge_gold;
        if (rankRes == R.string.rank_platinum) return R.drawable.ic_badge_platinum;
        if (rankRes == R.string.rank_diamond) return R.drawable.ic_badge_diamond;
        return R.drawable.ic_badge_bronze;
    }

    private String pageNotes(Book book) {
        if (book.pageNotes == null || book.pageNotes.isEmpty()) return getString(R.string.empty_history);
        StringBuilder sb = new StringBuilder();
        for (PageNote n : book.pageNotes) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append(getString(R.string.format_page_note, n.page, n.date, n.content));
        }
        return sb.toString();
    }

    private String empty(String s) { return s == null || s.trim().isEmpty() ? "-" : s; }
    private String emptyDash(String s) { return empty(s); }
}

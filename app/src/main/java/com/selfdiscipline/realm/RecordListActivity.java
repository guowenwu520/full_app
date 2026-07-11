package com.selfdiscipline.realm;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.selfdiscipline.realm.data.AppRepository;
import com.selfdiscipline.realm.model.*;
import com.selfdiscipline.realm.util.ViewUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecordListActivity extends Activity {
    private AppRepository repository;
    private AppState state;
    private String currentType;
    private LinearLayout currentList;
    public static final String EXTRA_TYPE = "list_type";
    public static final String TYPE_BOOKS = "books";
    public static final String TYPE_STUDY = "study";
    public static final String TYPE_EXERCISES = "exercises";
    public static final String TYPE_WORDS = "words";
    public static final String TYPE_DIARIES = "diaries";
    public static final String TYPE_EXP_LOGS = "exp_logs";
    public static final String TYPE_OVERVIEW = "overview";

    public static void open(Context c, String type) {
        Intent i = new Intent(c, RecordListActivity.class);
        i.putExtra(EXTRA_TYPE, type);
        c.startActivity(i);
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(getResources().getColor(R.color.color_surface));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(R.layout.activity_record_list);
        repository = new AppRepository(this);
        state = repository.load();
        currentType = getIntent().getStringExtra(EXTRA_TYPE);
        currentList = findViewById(R.id.record_list_container);
        TextView title = findViewById(R.id.text_list_title);
        title.setText(titleFor(currentType));
        renderList(state, currentType, currentList);
    }

    private String titleFor(String type) {
        if (TYPE_BOOKS.equals(type)) return getString(R.string.button_view_all_books);
        if (TYPE_STUDY.equals(type)) return getString(R.string.button_view_all_sport_sleep);
        if (TYPE_EXERCISES.equals(type)) return getString(R.string.label_record_type_exercises);
        if (TYPE_WORDS.equals(type)) return getString(R.string.button_view_all_words);
        if (TYPE_DIARIES.equals(type)) return getString(R.string.button_view_all_diaries);
        if (TYPE_EXP_LOGS.equals(type)) return getString(R.string.button_view_all_exp_logs);
        return getString(R.string.button_view_all_history);
    }

    private void renderList(AppState state, String type, LinearLayout list) {
        list.removeAllViews();
        List<Entry> entries = new ArrayList<>();
        if (TYPE_BOOKS.equals(type)) addBooks(state, entries);
        else if (TYPE_STUDY.equals(type)) addStudy(state, entries);
        else if (TYPE_EXERCISES.equals(type)) addExercises(state, entries);
        else if (TYPE_WORDS.equals(type)) addWords(state, entries);
        else if (TYPE_DIARIES.equals(type)) addDiaries(state, entries);
        else if (TYPE_EXP_LOGS.equals(type)) addExpLogs(state, entries);
        else addOverview(state, entries);
        Collections.sort(entries, (a, b) -> b.sortKey.compareTo(a.sortKey));
        if (entries.isEmpty()) { list.addView(ViewUtils.card(this, getString(R.string.empty_history))); return; }
        for (Entry e : entries) {
            LinearLayout card = ViewUtils.iconCard(this, e.iconRes, e.summary + "\n" + getString(R.string.label_tap_detail) + "｜" + getString(R.string.label_long_press_delete), v -> RecordDetailActivity.open(this, e.detailType, e.detailIndex));
            card.setOnLongClickListener(v -> {
                confirmDelete(e);
                return true;
            });
            list.addView(card);
        }
    }

    private void confirmDelete(Entry entry) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_record_title)
                .setMessage(getString(R.string.dialog_delete_record_message, deleteName(entry.detailType)))
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_delete, (d, w) -> deleteEntry(entry))
                .show();
    }

    private String deleteName(String type) {
        if (RecordDetailActivity.TYPE_BOOK.equals(type)) return getString(R.string.label_record_type_books);
        if (RecordDetailActivity.TYPE_EXERCISE.equals(type)) return getString(R.string.label_record_type_exercises);
        if (RecordDetailActivity.TYPE_WEIGHT.equals(type)) return getString(R.string.label_record_type_weights);
        if (RecordDetailActivity.TYPE_SLEEP.equals(type)) return getString(R.string.label_record_type_sleeps);
        if (RecordDetailActivity.TYPE_WORD.equals(type)) return getString(R.string.label_record_type_words);
        if (RecordDetailActivity.TYPE_DIARY.equals(type)) return getString(R.string.label_record_type_diaries);
        if (RecordDetailActivity.TYPE_EXP.equals(type)) return getString(R.string.label_record_type_exp_logs);
        return getString(R.string.title_record_detail);
    }

    private void deleteEntry(Entry entry) {
        boolean ok = false;
        try {
            String type = entry.detailType;
            int index = entry.detailIndex;
            if (RecordDetailActivity.TYPE_BOOK.equals(type) && valid(index, state.books.size())) {
                state.books.remove(index);
                ok = true;
            } else if (RecordDetailActivity.TYPE_EXERCISE.equals(type) && valid(index, state.exercises.size())) {
                ExerciseRecord removed = state.exercises.remove(index);
                cleanupDailyAwardIfNoMoreRecord(removed.date, "exercise");
                ok = true;
            } else if (RecordDetailActivity.TYPE_WEIGHT.equals(type) && valid(index, state.weights.size())) {
                state.weights.remove(index);
                ok = true;
            } else if (RecordDetailActivity.TYPE_SLEEP.equals(type) && valid(index, state.sleeps.size())) {
                SleepRecord removed = state.sleeps.remove(index);
                cleanupSleepAwardIfNoPassedRecord(removed.date);
                ok = true;
            } else if (RecordDetailActivity.TYPE_WORD.equals(type) && valid(index, state.words.size())) {
                WordEntry removed = state.words.remove(index);
                cleanupWordAwardIfNoMoreRecord(removed.createdDate);
                ok = true;
            } else if (RecordDetailActivity.TYPE_DIARY.equals(type) && valid(index, state.diaries.size())) {
                DiaryRecord removed = state.diaries.remove(index);
                cleanupDiaryAwardIfNoNoBreakRecord(removed.date);
                ok = true;
            } else if (RecordDetailActivity.TYPE_EXP.equals(type) && valid(index, state.expLogs.size())) {
                ExperienceLog removed = state.expLogs.remove(index);
                state.awardedKeys.remove(removed.key);
                ok = true;
            }
            if (ok) {
                cleanupAllDoneAwards();
                repository.save(state);
                Toast.makeText(this, R.string.toast_deleted, Toast.LENGTH_SHORT).show();
                renderList(state, currentType, currentList);
            } else {
                Toast.makeText(this, R.string.toast_delete_failed, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ex) {
            Toast.makeText(this, R.string.toast_delete_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean valid(int index, int size) { return index >= 0 && index < size; }

    private void cleanupDailyAwardIfNoMoreRecord(String date, String moduleKey) {
        if (date == null || date.isEmpty()) return;
        if ("exercise".equals(moduleKey)) {
            for (ExerciseRecord r : state.exercises) if (date.equals(r.date)) return;
        }
        removeAwardAndLogs(moduleKey + "_" + date);
    }

    private void cleanupSleepAwardIfNoPassedRecord(String date) {
        if (date == null || date.isEmpty()) return;
        for (SleepRecord r : state.sleeps) if (date.equals(r.date) && r.passed) return;
        removeAwardAndLogs("sleep_" + date);
    }

    private void cleanupWordAwardIfNoMoreRecord(String date) {
        if (date == null || date.isEmpty()) return;
        for (WordEntry r : state.words) if (date.equals(r.createdDate)) return;
        state.wordDates.remove(date);
        removeAwardAndLogs("word_" + date);
    }

    private void cleanupDiaryAwardIfNoNoBreakRecord(String date) {
        if (date == null || date.isEmpty()) return;
        for (DiaryRecord r : state.diaries) if (date.equals(r.date) && !r.broken) return;
        removeAwardAndLogs("diary_" + date);
    }

    private void cleanupAllDoneAwards() {
        List<String> remove = new ArrayList<>();
        for (String key : state.awardedKeys) {
            if (key != null && key.startsWith("all_done_")) {
                String date = key.substring("all_done_".length());
                if (!com.selfdiscipline.realm.engine.StatsEngine.allFiveDone(state, date)) remove.add(key);
            }
        }
        for (String key : remove) removeAwardAndLogs(key);
    }

    private void removeAwardAndLogs(String key) {
        if (key == null || key.isEmpty()) return;
        state.awardedKeys.remove(key);
        for (int i = state.expLogs.size() - 1; i >= 0; i--) {
            ExperienceLog log = state.expLogs.get(i);
            if (key.equals(log.key)) state.expLogs.remove(i);
        }
    }

    private void addBooks(AppState s, List<Entry> out) {
        for (int i = 0; i < s.books.size(); i++) {
            Book r = s.books.get(i);
            out.add(new Entry("9999-99-99-" + (100000 - i), R.drawable.ic_nav_reading,
                    getString(R.string.format_book_card, r.title, r.author == null ? "" : r.author, r.currentPage, r.fullReview == null || r.fullReview.isEmpty() ? getString(R.string.text_no) : getString(R.string.text_yes), r.pageNotes.size()),
                    RecordDetailActivity.TYPE_BOOK, i));
        }
    }

    /**
     * 仅添加运动记录。
     * 用于运动页面中的“查看全部”。
     */
    private void addExercises(AppState s, List<Entry> out) {
        for (int i = 0; i < s.exercises.size(); i++) {
            ExerciseRecord r = s.exercises.get(i);
            out.add(new Entry(
                    r.date + "-exercise-" + i,
                    R.drawable.ic_nav_sport,
                    getString(
                            R.string.format_exercise_record,
                            r.date,
                            r.content,
                            r.calories
                    ),
                    RecordDetailActivity.TYPE_EXERCISE,
                    i
            ));
        }
    }

    /**
     * 原来的综合学习/运动页面：
     * 运动 + 体重 + 作息。
     */
    private void addStudy(AppState s, List<Entry> out) {
        addExercises(s, out);

        for (int i = 0; i < s.weights.size(); i++) {
            WeightRecord r = s.weights.get(i);
            out.add(new Entry(r.date + "-weight-" + i, R.drawable.ic_nav_sport, getString(R.string.format_weight_record, r.date, r.weight), RecordDetailActivity.TYPE_WEIGHT, i));
        }
        for (int i = 0; i < s.sleeps.size(); i++) {
            SleepRecord r = s.sleeps.get(i);
            out.add(new Entry(r.date + "-sleep-" + i, R.drawable.ic_nav_sport, getString(R.string.format_sleep_record, r.date, r.sleepTime, r.wakeTime, r.passed ? getString(R.string.text_sleep_pass) : getString(R.string.text_sleep_fail)), RecordDetailActivity.TYPE_SLEEP, i));
        }
    }

    private void addWords(AppState s, List<Entry> out) {
        for (int i = 0; i < s.words.size(); i++) {
            WordEntry r = s.words.get(i);
            out.add(new Entry(r.createdDate + "-word-" + i, R.drawable.ic_nav_word, getString(R.string.format_word_record, r.word, r.meaning, r.correctCount, r.wrongCount, r.lastTestDate == null || r.lastTestDate.isEmpty() ? "-" : r.lastTestDate), RecordDetailActivity.TYPE_WORD, i));
        }
    }

    private void addDiaries(AppState s, List<Entry> out) {
        for (int i = 0; i < s.diaries.size(); i++) {
            DiaryRecord r = s.diaries.get(i);
            out.add(new Entry(r.date + "-diary-" + i, R.drawable.ic_nav_diary, getString(R.string.format_diary_record, r.date, r.title, r.broken ? getString(R.string.text_broken) : getString(R.string.text_not_broken), r.body), RecordDetailActivity.TYPE_DIARY, i));
        }
    }

    private void addExpLogs(AppState s, List<Entry> out) {
        for (int i = 0; i < s.expLogs.size(); i++) {
            ExperienceLog r = s.expLogs.get(i);
            out.add(new Entry(r.date + "-exp-" + i, R.drawable.ic_xp, getString(R.string.format_exp_log, r.date, r.points, r.source), RecordDetailActivity.TYPE_EXP, i));
        }
    }

    private void addOverview(AppState s, List<Entry> out) {
        addBooks(s, out);
        addStudy(s, out);
        addWords(s, out);
        addDiaries(s, out);
        addExpLogs(s, out);
    }

    private static class Entry {
        String sortKey;
        int iconRes;
        String summary;
        String detailType;
        int detailIndex;
        Entry(String sortKey, int iconRes, String summary, String detailType, int detailIndex) {
            this.sortKey = sortKey; this.iconRes = iconRes; this.summary = summary; this.detailType = detailType; this.detailIndex = detailIndex;
        }
    }
}
package com.selfdiscipline.realm.engine;

import com.selfdiscipline.realm.RecordDetailActivity;
import com.selfdiscipline.realm.model.AppState;
import com.selfdiscipline.realm.model.DiaryRecord;
import com.selfdiscipline.realm.model.ExerciseRecord;
import com.selfdiscipline.realm.model.ExperienceLog;
import com.selfdiscipline.realm.model.SleepRecord;
import com.selfdiscipline.realm.model.WordEntry;

import java.util.ArrayList;
import java.util.List;

public final class RecordDeleteEngine {
    private RecordDeleteEngine() {}

    public static boolean delete(AppState state, String type, int index) {
        if (state == null || type == null) return false;
        boolean deleted = false;
        if (RecordDetailActivity.TYPE_BOOK.equals(type) && valid(index, state.books.size())) {
            state.books.remove(index);
            deleted = true;
        } else if (RecordDetailActivity.TYPE_EXERCISE.equals(type) && valid(index, state.exercises.size())) {
            ExerciseRecord removed = state.exercises.remove(index);
            cleanupDailyAwardIfNoMoreExercise(state, removed.date);
            deleted = true;
        } else if (RecordDetailActivity.TYPE_WEIGHT.equals(type) && valid(index, state.weights.size())) {
            state.weights.remove(index);
            deleted = true;
        } else if (RecordDetailActivity.TYPE_SLEEP.equals(type) && valid(index, state.sleeps.size())) {
            SleepRecord removed = state.sleeps.remove(index);
            cleanupSleepAwardIfNoPassedRecord(state, removed.date);
            deleted = true;
        } else if (RecordDetailActivity.TYPE_WORD.equals(type) && valid(index, state.words.size())) {
            WordEntry removed = state.words.remove(index);
            cleanupWordAwardIfNoMoreRecord(state, removed.createdDate);
            deleted = true;
        } else if (RecordDetailActivity.TYPE_DIARY.equals(type) && valid(index, state.diaries.size())) {
            DiaryRecord removed = state.diaries.remove(index);
            cleanupDiaryAwardIfNoNoBreakRecord(state, removed.date);
            deleted = true;
        } else if (RecordDetailActivity.TYPE_EXP.equals(type) && valid(index, state.expLogs.size())) {
            ExperienceLog removed = state.expLogs.remove(index);
            state.awardedKeys.remove(removed.key);
            deleted = true;
        }
        if (deleted) cleanupAllDoneAwards(state);
        return deleted;
    }

    private static boolean valid(int index, int size) {
        return index >= 0 && index < size;
    }

    private static void cleanupDailyAwardIfNoMoreExercise(AppState state, String date) {
        if (date == null || date.isEmpty()) return;
        for (ExerciseRecord r : state.exercises) if (date.equals(r.date)) return;
        removeAwardAndLogs(state, "exercise_" + date);
    }

    private static void cleanupSleepAwardIfNoPassedRecord(AppState state, String date) {
        if (date == null || date.isEmpty()) return;
        for (SleepRecord r : state.sleeps) if (date.equals(r.date) && r.passed) return;
        removeAwardAndLogs(state, "sleep_" + date);
    }

    private static void cleanupWordAwardIfNoMoreRecord(AppState state, String date) {
        if (date == null || date.isEmpty()) return;
        for (WordEntry r : state.words) if (date.equals(r.createdDate)) return;
        state.wordDates.remove(date);
        removeAwardAndLogs(state, "word_" + date);
    }

    private static void cleanupDiaryAwardIfNoNoBreakRecord(AppState state, String date) {
        if (date == null || date.isEmpty()) return;
        for (DiaryRecord r : state.diaries) if (date.equals(r.date) && !r.broken) return;
        removeAwardAndLogs(state, "diary_" + date);
    }

    private static void cleanupAllDoneAwards(AppState state) {
        List<String> remove = new ArrayList<>();
        for (String key : state.awardedKeys) {
            if (key != null && key.startsWith("all_done_")) {
                String date = key.substring("all_done_".length());
                if (!StatsEngine.allFiveDone(state, date)) remove.add(key);
            }
        }
        for (String key : remove) removeAwardAndLogs(state, key);
    }

    private static void removeAwardAndLogs(AppState state, String key) {
        if (key == null || key.isEmpty()) return;
        state.awardedKeys.remove(key);
        for (int i = state.expLogs.size() - 1; i >= 0; i--) {
            ExperienceLog log = state.expLogs.get(i);
            if (key.equals(log.key)) state.expLogs.remove(i);
        }
    }
}

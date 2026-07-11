package com.selfdiscipline.realm.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.selfdiscipline.realm.R;
import com.selfdiscipline.realm.RecordDetailActivity;
import com.selfdiscipline.realm.RecordListActivity;
import com.selfdiscipline.realm.data.AppRepository;
import com.selfdiscipline.realm.engine.RewardEngine;
import com.selfdiscipline.realm.model.*;
import com.selfdiscipline.realm.util.DateUtils;
import com.selfdiscipline.realm.util.ViewUtils;
import com.selfdiscipline.realm.view.WeightChartView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class StudySportFragment extends BaseFragmentHelper {
    private AppRepository repo;
    private AppState state;
    private EditText exDate, exContent, calInput, wDate, wInput, sDate, sleep, wake;
    private EditText word, meaning;
    private LinearLayout history, wordList, panelSport, panelWords;
    private WeightChartView chart;
    private TextView tabSport, tabWords, title, subtitle;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup c, Bundle b) {
        View root = inflater.inflate(R.layout.fragment_study_sport, c, false);
        repo = new AppRepository(getActivity());
        state = repo.load();
        title = root.findViewById(R.id.title_study_sport);
        subtitle = root.findViewById(R.id.subtitle_study_sport);
        tabSport = root.findViewById(R.id.tab_sport);
        tabWords = root.findViewById(R.id.tab_words);
        panelSport = root.findViewById(R.id.panel_sport);
        panelWords = root.findViewById(R.id.panel_words);
        exDate = root.findViewById(R.id.input_exercise_date);
        exContent = root.findViewById(R.id.input_exercise_content);
        calInput = root.findViewById(R.id.input_calories);
        wDate = root.findViewById(R.id.input_weight_date);
        wInput = root.findViewById(R.id.input_weight);
        sDate = root.findViewById(R.id.input_sleep_date);
        sleep = root.findViewById(R.id.input_sleep_time);
        wake = root.findViewById(R.id.input_wake_time);
        history = root.findViewById(R.id.study_history_list);
        chart = root.findViewById(R.id.weight_chart);
        word = root.findViewById(R.id.input_word);
        meaning = root.findViewById(R.id.input_meaning);
        wordList = root.findViewById(R.id.word_list);
        String t = DateUtils.today();
        exDate.setText(t); wDate.setText(t); sDate.setText(t);
        root.findViewById(R.id.button_save_exercise).setOnClickListener(v -> saveExercise());
        root.findViewById(R.id.button_save_weight).setOnClickListener(v -> saveWeight());
        root.findViewById(R.id.button_save_sleep).setOnClickListener(v -> saveSleep());
        root.findViewById(R.id.button_view_all_study).setOnClickListener(v -> RecordListActivity.open(getActivity(), RecordListActivity.TYPE_STUDY));
        root.findViewById(R.id.button_add_word).setOnClickListener(v -> addWord());
        root.findViewById(R.id.button_random_test).setOnClickListener(v -> randomTest());
        root.findViewById(R.id.button_view_all_words).setOnClickListener(v -> RecordListActivity.open(getActivity(), RecordListActivity.TYPE_WORDS));
        tabSport.setOnClickListener(v -> showSport());
        tabWords.setOnClickListener(v -> showWords());
        showSport();
        render();
        return root;
    }

    @Override public void onResume() { super.onResume(); if (repo != null) { state = repo.load(); render(); } }

    private void showSport() {
        panelSport.setVisibility(View.VISIBLE);
        panelWords.setVisibility(View.GONE);
        tabSport.setBackgroundResource(R.drawable.bg_segment_selected);
        tabWords.setBackgroundColor(0x00000000);
        tabSport.setTypeface(null, 1);
        tabWords.setTypeface(null, 0);
        title.setText(R.string.title_study);
        subtitle.setText(R.string.sport_subtitle_pixel);
    }

    private void showWords() {
        panelSport.setVisibility(View.GONE);
        panelWords.setVisibility(View.VISIBLE);
        tabWords.setBackgroundResource(R.drawable.bg_segment_selected);
        tabSport.setBackgroundColor(0x00000000);
        tabWords.setTypeface(null, 1);
        tabSport.setTypeface(null, 0);
        title.setText(R.string.title_words);
        subtitle.setText(R.string.word_subtitle_pixel);
    }

    private void saveExercise() {
        String d = ViewUtils.text(exDate), c = ViewUtils.text(exContent);
        int cal = ViewUtils.parseInt(ViewUtils.text(calInput), -1);
        if (!DateUtils.isValidDate(d) || c.isEmpty() || cal < 0) { ViewUtils.toast(getActivity(), R.string.toast_invalid_input); return; }
        state.exercises.add(0, new ExerciseRecord(d, c, cal));
        RewardEngine.RewardResult rr = RewardEngine.awardExercise(getActivity(), state, d);
        repo.save(state);
        exContent.setText(""); calInput.setText("");
        showReward(rr);
        render();
    }

    private void saveWeight() {
        String d = ViewUtils.text(wDate);
        float val = ViewUtils.parseFloat(ViewUtils.text(wInput), -1);
        if (!DateUtils.isValidDate(d) || val <= 0) { ViewUtils.toast(getActivity(), R.string.toast_invalid_input); return; }
        for (int i = state.weights.size() - 1; i >= 0; i--) if (d.equals(state.weights.get(i).date)) state.weights.remove(i);
        state.weights.add(new WeightRecord(d, val));
        RewardEngine.RewardResult rr = RewardEngine.afterAction(getActivity(), state, d);
        repo.save(state);
        wInput.setText("");
        showReward(rr);
        render();
    }

    private void saveSleep() {
        String d = ViewUtils.text(sDate), sl = ViewUtils.text(sleep), wa = ViewUtils.text(wake);
        if (!DateUtils.isValidDate(d) || !DateUtils.isValidTime(sl) || !DateUtils.isValidTime(wa)) { ViewUtils.toast(getActivity(), R.string.toast_invalid_input); return; }
        boolean passed = DateUtils.sleepPassed(sl, wa);
        for (int i = state.sleeps.size() - 1; i >= 0; i--) if (d.equals(state.sleeps.get(i).date)) state.sleeps.remove(i);
        state.sleeps.add(0, new SleepRecord(d, sl, wa, passed));
        RewardEngine.RewardResult rr = passed ? RewardEngine.awardSleep(getActivity(), state, d) : RewardEngine.afterAction(getActivity(), state, d);
        repo.save(state);
        showReward(rr);
        render();
    }

    private void addWord() {
        String w = ViewUtils.text(word), m = ViewUtils.text(meaning);
        if (w.isEmpty() || m.isEmpty()) { ViewUtils.toast(getActivity(), R.string.toast_invalid_input); return; }
        String d = DateUtils.today();
        state.words.add(0, new WordEntry(w, m, d));
        state.addWordDate(d);
        RewardEngine.RewardResult rr = RewardEngine.awardWord(getActivity(), state, d);
        repo.save(state);
        word.setText(""); meaning.setText("");
        showReward(rr);
        render();
    }

    private void randomTest() {
        if (state.words.isEmpty()) { ViewUtils.toast(getActivity(), R.string.toast_no_words); return; }
        WordEntry e = state.words.get(new Random().nextInt(state.words.size()));
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.dialog_word_test_title)
                .setMessage(e.word + "\n\n" + e.meaning)
                .setPositiveButton(R.string.button_answer_correct, (d, w) -> {
                    e.correctCount++;
                    e.lastTestDate = DateUtils.today();
                    state.addWordDate(e.lastTestDate);
                    RewardEngine.RewardResult rr = RewardEngine.awardWord(getActivity(), state, e.lastTestDate);
                    repo.save(state);
                    showReward(rr);
                    render();
                })
                .setNegativeButton(R.string.button_answer_wrong, (d, w) -> {
                    e.wrongCount++;
                    e.lastTestDate = DateUtils.today();
                    state.addWordDate(e.lastTestDate);
                    RewardEngine.RewardResult rr = RewardEngine.awardWord(getActivity(), state, e.lastTestDate);
                    repo.save(state);
                    showReward(rr);
                    render();
                }).show();
    }

    private void render() {
        if (chart != null) chart.setRecords(state.weights);
        renderStudyHistory();
        renderWordList();
    }

    private void renderStudyHistory() {
        history.removeAllViews();
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < state.exercises.size(); i++) {
            ExerciseRecord r = state.exercises.get(i);
            entries.add(new Entry(r.date, getString(R.string.summary_exercise, r.content, r.calories), RecordDetailActivity.TYPE_EXERCISE, i));
        }
        for (int i = 0; i < state.weights.size(); i++) {
            WeightRecord r = state.weights.get(i);
            entries.add(new Entry(r.date, getString(R.string.summary_weight, r.date, r.weight), RecordDetailActivity.TYPE_WEIGHT, i));
        }
        for (int i = 0; i < state.sleeps.size(); i++) {
            SleepRecord r = state.sleeps.get(i);
            entries.add(new Entry(r.date, getString(R.string.summary_sleep, r.date, r.passed ? getString(R.string.text_sleep_pass) : getString(R.string.text_sleep_fail)), RecordDetailActivity.TYPE_SLEEP, i));
        }
        Collections.sort(entries, (a, b) -> b.date.compareTo(a.date));
        if (entries.isEmpty()) { history.addView(ViewUtils.card(getActivity(), getString(R.string.empty_history))); return; }
        for (int i = 0; i < Math.min(5, entries.size()); i++) {
            Entry e = entries.get(i);
            history.addView(ViewUtils.iconCard(getActivity(), R.drawable.ic_nav_sport, e.text + "\n" + getString(R.string.label_tap_detail), v -> RecordDetailActivity.open(getActivity(), e.type, e.index)));
        }
    }

    private void renderWordList() {
        wordList.removeAllViews();
        if (state.words.isEmpty()) { wordList.addView(ViewUtils.card(getActivity(), getString(R.string.toast_no_words))); return; }
        for (int i = 0; i < Math.min(5, state.words.size()); i++) {
            WordEntry e = state.words.get(i);
            final int index = i;
            wordList.addView(ViewUtils.iconCard(getActivity(), R.drawable.ic_nav_word, getString(R.string.format_word_record, e.word, e.meaning, e.correctCount, e.wrongCount, e.lastTestDate == null || e.lastTestDate.isEmpty() ? "-" : e.lastTestDate) + "\n" + getString(R.string.label_tap_detail), v -> RecordDetailActivity.open(getActivity(), RecordDetailActivity.TYPE_WORD, index)));
        }
    }

    private static class Entry {
        String date, text, type; int index;
        Entry(String date, String text, String type, int index) { this.date = date; this.text = text; this.type = type; this.index = index; }
    }
}

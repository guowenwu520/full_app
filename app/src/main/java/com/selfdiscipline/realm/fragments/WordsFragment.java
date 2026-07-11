package com.selfdiscipline.realm.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.selfdiscipline.realm.R;
import com.selfdiscipline.realm.RecordDetailActivity;
import com.selfdiscipline.realm.RecordListActivity;
import com.selfdiscipline.realm.data.AppRepository;
import com.selfdiscipline.realm.engine.RewardEngine;
import com.selfdiscipline.realm.model.AppState;
import com.selfdiscipline.realm.model.WordEntry;
import com.selfdiscipline.realm.util.DateUtils;
import com.selfdiscipline.realm.util.ViewUtils;

import java.util.Random;

public class WordsFragment extends BaseFragmentHelper {
    private AppRepository repo;
    private AppState state;
    private EditText word, meaning;
    private LinearLayout list;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup c, Bundle b) {
        View root = inflater.inflate(R.layout.fragment_words, c, false);
        repo = new AppRepository(getActivity());
        state = repo.load();
        word = root.findViewById(R.id.input_word);
        meaning = root.findViewById(R.id.input_meaning);
        list = root.findViewById(R.id.word_list);
        root.findViewById(R.id.button_add_word).setOnClickListener(v -> addWord());
        root.findViewById(R.id.button_random_test).setOnClickListener(v -> randomTest());
        root.findViewById(R.id.button_view_all_words).setOnClickListener(v -> RecordListActivity.open(getActivity(), RecordListActivity.TYPE_WORDS));
        render();
        return root;
    }

    @Override public void onResume() { super.onResume(); if (repo != null) { state = repo.load(); render(); } }

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
        list.removeAllViews();
        if (state.words.isEmpty()) { list.addView(ViewUtils.card(getActivity(), getString(R.string.toast_no_words))); return; }
        for (int i = 0; i < Math.min(8, state.words.size()); i++) {
            WordEntry e = state.words.get(i);
            final int index = i;
            list.addView(ViewUtils.iconCard(getActivity(), R.drawable.ic_nav_word, getString(R.string.format_word_record, e.word, e.meaning, e.correctCount, e.wrongCount, e.lastTestDate == null || e.lastTestDate.isEmpty() ? "-" : e.lastTestDate) + "\n" + getString(R.string.label_tap_detail), v -> RecordDetailActivity.open(getActivity(), RecordDetailActivity.TYPE_WORD, index)));
        }
    }
}

package com.selfdiscipline.realm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.selfdiscipline.realm.data.AppRepository;
import com.selfdiscipline.realm.engine.RewardEngine;
import com.selfdiscipline.realm.model.AppState;
import com.selfdiscipline.realm.model.DiaryRecord;
import com.selfdiscipline.realm.model.ExperienceLog;
import com.selfdiscipline.realm.util.DateUtils;

import java.util.ArrayList;

/**
 * 独立的今日日记编辑页。
 * 破戒状态仍在日记主页设置，本页只负责标题和正文，避免键盘遮挡主页内容。
 */
public class DiaryWriteActivity extends Activity {

    private static final String BREAK_PLACEHOLDER_TITLE = "破戒记录";
    private static final String EXTRA_EDITOR_MODE = "editor_mode";
    private static final String MODE_FUTURES_INCOME = "futures_income";

    private AppRepository repo;
    private AppState state;
    private EditText titleInput;
    private EditText bodyInput;
    private TextView dateView;
    private TextView breakStatusView;
    private TextView saveButton;
    private int todayDiaryIndex = -1;
    private boolean todayBroken;

    public static void open(Context context) {
        if (context == null) return;
        context.startActivity(new Intent(context, DiaryWriteActivity.class));
    }

    /** 使用已经注册的长文本编辑页宿主打开期货盈亏页面，不需要修改清单配置。 */
    public static void openFuturesIncome(Context context) {
        if (context == null) return;
        Intent intent = new Intent(context, DiaryWriteActivity.class);
        intent.putExtra(EXTRA_EDITOR_MODE, MODE_FUTURES_INCOME);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (MODE_FUTURES_INCOME.equals(
                getIntent().getStringExtra(EXTRA_EDITOR_MODE)
        )) {
            new FuturesIncomeWriteController(this).create();
            return;
        }
        setContentView(R.layout.activity_diary_write);

        repo = new AppRepository(this);
        state = repo.load();
        if (state == null) state = new AppState();
        normalizeState();

        bindViews();
        findViewById(R.id.buttonDiaryWriteBack).setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> saveTodayDiary());
        render();
    }

    private void bindViews() {
        titleInput = findViewById(R.id.inputDiaryWriteTitle);
        bodyInput = findViewById(R.id.inputDiaryWriteBody);
        dateView = findViewById(R.id.tvDiaryWriteDate);
        breakStatusView = findViewById(R.id.tvDiaryWriteBreakStatus);
        saveButton = findViewById(R.id.buttonSaveDiaryWrite);
    }

    private void render() {
        String today = DateUtils.today();
        todayDiaryIndex = findDiaryIndex(today);
        todayBroken = false;
        boolean hasWrittenDiary = false;

        if (todayDiaryIndex >= 0 && todayDiaryIndex < state.diaries.size()) {
            DiaryRecord diary = state.diaries.get(todayDiaryIndex);
            todayBroken = diary.broken;
            boolean placeholder = BREAK_PLACEHOLDER_TITLE.equals(safe(diary.title))
                    && safe(diary.body).trim().isEmpty();
            if (!placeholder) {
                titleInput.setText(safe(diary.title));
                bodyInput.setText(safe(diary.body));
                hasWrittenDiary = true;
            }
        }

        dateView.setText("日期：" + today);
        breakStatusView.setText(todayBroken
                ? "今日状态：已破戒（保存日记不会恢复不破戒状态）"
                : "今日状态：未破戒");
        breakStatusView.setTextColor(getResources().getColor(
                todayBroken ? R.color.color_danger : R.color.checkin_done
        ));
        saveButton.setText(hasWrittenDiary ? "更新今日日记" : "保存今日日记");
    }

    private void saveTodayDiary() {
        String title = titleInput.getText().toString().trim();
        String body = bodyInput.getText().toString().trim();

        if (title.isEmpty() || body.isEmpty()) {
            Toast.makeText(this, "标题和正文不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        String today = DateUtils.today();
        try {
            if (todayDiaryIndex >= 0 && todayDiaryIndex < state.diaries.size()) {
                DiaryRecord diary = state.diaries.get(todayDiaryIndex);
                diary.date = today;
                diary.title = title;
                diary.body = body;
                diary.broken = todayBroken;
            } else {
                state.diaries.add(0, new DiaryRecord(today, title, body, todayBroken));
            }

            if (todayBroken) {
                removeAwardAndLogs("diary_" + today);
                removeAwardAndLogs("all_done_" + today);
                RewardEngine.syncNoBreakBadges(state);
            } else {
                RewardEngine.awardDiary(this, state, today);
            }

            repo.save(state);
            Toast.makeText(this,
                    todayDiaryIndex >= 0 ? "今日日记已更新" : "今日日记已保存",
                    Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "保存失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private int findDiaryIndex(String date) {
        for (int i = 0; i < state.diaries.size(); i++) {
            DiaryRecord diary = state.diaries.get(i);
            if (diary != null && date.equals(safe(diary.date))) return i;
        }
        return -1;
    }

    private void removeAwardAndLogs(String key) {
        state.awardedKeys.remove(key);
        for (int i = state.expLogs.size() - 1; i >= 0; i--) {
            ExperienceLog log = state.expLogs.get(i);
            if (log != null && key.equals(log.key)) state.expLogs.remove(i);
        }
    }

    private void normalizeState() {
        if (state.diaries == null) state.diaries = new ArrayList<>();
        if (state.awardedKeys == null) state.awardedKeys = new ArrayList<>();
        if (state.expLogs == null) state.expLogs = new ArrayList<>();
        if (state.unlockedBadgeIds == null) state.unlockedBadgeIds = new ArrayList<>();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

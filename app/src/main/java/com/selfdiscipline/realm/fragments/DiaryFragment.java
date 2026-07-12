package com.selfdiscipline.realm.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TimePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.selfdiscipline.realm.R;
import com.selfdiscipline.realm.RecordDetailActivity;
import com.selfdiscipline.realm.RecordListActivity;
import com.selfdiscipline.realm.data.AppRepository;
import com.selfdiscipline.realm.engine.StatsEngine;
import com.selfdiscipline.realm.engine.RewardEngine;
import com.selfdiscipline.realm.model.AppState;
import com.selfdiscipline.realm.model.DiaryRecord;
import com.selfdiscipline.realm.ui.RealmDialog;
import com.selfdiscipline.realm.model.ExperienceLog;
import com.selfdiscipline.realm.model.SleepRecord;
import com.selfdiscipline.realm.util.DateUtils;

import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 日记自律主页。
 *
 * 功能：
 * 1. 今日破戒状态
 * 2. 每日一篇日记，重复保存时更新当天日记
 * 3. 最近日记 RecyclerView
 * 4. 当前连续不破戒、本月破戒、今日经验统计
 * 5. 与原有经验奖励、全部历史、详情页兼容
 */
public class DiaryFragment extends BaseFragmentHelper {

    private static final int RECENT_LIMIT = 3;

    private AppRepository repo;
    private AppState state;

    private View statCurrentNoBreak;
    private View statMonthlyBroken;
    private View statDiaryTotal;
    private View statTodayDiaryExp;

    private TextView buttonNotBroken;
    private TextView buttonBroken;
    private TextView todayStatusHint;

    private View pickSleepTimeButton;
    private View pickWakeTimeButton;
    private TextView sleepTimeValue;
    private TextView wakeTimeValue;
    private TextView sleepPassStatus;
    private TextView saveSleepButton;
    private int selectedSleepHour = 22;
    private int selectedSleepMinute = 30;
    private int selectedWakeHour = 7;
    private int selectedWakeMinute = 0;
    private int todaySleepIndex = -1;

    private EditText titleInput;
    private EditText bodyInput;
    private TextView saveButton;

    private RecyclerView recentRecycler;
    private TextView emptyDiaries;
    private DiaryAdapter diaryAdapter;


    private boolean pendingBroken;
    private int todayDiaryIndex = -1;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View root = inflater.inflate(
                R.layout.fragment_diary,
                container,
                false
        );

        repo = new AppRepository(getActivity());
        state = repo.load();

        if (state == null) {
            state = new AppState();
        }

        normalizeState();
        bindViews(root);
        setupRecyclerView();
        setupClicks(root);
        render();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (repo != null) {
            state = repo.load();

            if (state == null) {
                state = new AppState();
            }

            normalizeState();
            render();
        }
    }

    private void bindViews(View root) {
        statCurrentNoBreak = root.findViewById(
                R.id.statCurrentNoBreak
        );
        statMonthlyBroken = root.findViewById(
                R.id.statMonthlyBroken
        );
        statDiaryTotal = root.findViewById(
                R.id.statDiaryTotal
        );
        statTodayDiaryExp = root.findViewById(
                R.id.statTodayDiaryExp
        );

        buttonNotBroken = root.findViewById(
                R.id.buttonNotBroken
        );
        buttonBroken = root.findViewById(
                R.id.buttonBroken
        );
        todayStatusHint = root.findViewById(
                R.id.tvTodayStatusHint
        );

        pickSleepTimeButton = root.findViewById(
                R.id.buttonPickSleepTime
        );
        pickWakeTimeButton = root.findViewById(
                R.id.buttonPickWakeTime
        );
        sleepTimeValue = root.findViewById(
                R.id.tvSleepTimeValue
        );
        wakeTimeValue = root.findViewById(
                R.id.tvWakeTimeValue
        );
        sleepPassStatus = root.findViewById(
                R.id.tvSleepPassStatus
        );
        saveSleepButton = root.findViewById(
                R.id.buttonSaveSleepRecord
        );

        titleInput = root.findViewById(
                R.id.inputDiaryTitle
        );
        bodyInput = root.findViewById(
                R.id.inputDiaryBody
        );
        saveButton = root.findViewById(
                R.id.buttonSaveDiary
        );

        recentRecycler = root.findViewById(
                R.id.recyclerRecentDiaries
        );
        emptyDiaries = root.findViewById(
                R.id.tvEmptyDiaries
        );

    }

    private void setupRecyclerView() {
        diaryAdapter = new DiaryAdapter();

        recentRecycler.setLayoutManager(
                new LinearLayoutManager(getActivity())
        );
        recentRecycler.setAdapter(diaryAdapter);
        recentRecycler.setNestedScrollingEnabled(false);
        recentRecycler.setOverScrollMode(
                View.OVER_SCROLL_NEVER
        );
    }

    private void setupClicks(View root) {
        buttonNotBroken.setOnClickListener(v ->
                setPendingBroken(false)
        );

        buttonBroken.setOnClickListener(v ->
                setPendingBroken(true)
        );

        pickSleepTimeButton.setOnClickListener(v ->
                showTimePicker(
                        "选择入睡时间",
                        selectedSleepHour,
                        selectedSleepMinute,
                        (hour, minute) -> {
                            selectedSleepHour = hour;
                            selectedSleepMinute = minute;
                            updateSleepPreview();
                        }
                )
        );

        pickWakeTimeButton.setOnClickListener(v ->
                showTimePicker(
                        "选择起床时间",
                        selectedWakeHour,
                        selectedWakeMinute,
                        (hour, minute) -> {
                            selectedWakeHour = hour;
                            selectedWakeMinute = minute;
                            updateSleepPreview();
                        }
                )
        );

        saveSleepButton.setOnClickListener(v -> saveTodaySleep());

        saveButton.setOnClickListener(v -> saveTodayDiary());

        View.OnClickListener openAll = v ->
                RecordListActivity.open(
                        getActivity(),
                        RecordListActivity.TYPE_DIARIES
                );

        root.findViewById(R.id.buttonAllDiaries)
                .setOnClickListener(openAll);

        root.findViewById(R.id.buttonDiaryCalendar)
                .setOnClickListener(openAll);
    }

    private void render() {
        normalizeState();
        renderTodaySleep();
        renderTodayDiary();
        renderStats();
        renderRecentDiaries();
    }

    private void renderTodaySleep() {
        String today = DateUtils.today();
        todaySleepIndex = findSleepIndex(today);

        if (todaySleepIndex >= 0
                && todaySleepIndex < state.sleeps.size()) {
            SleepRecord record = state.sleeps.get(todaySleepIndex);
            int[] sleep = parseTime(record.sleepTime, 22, 30);
            int[] wake = parseTime(record.wakeTime, 7, 0);
            selectedSleepHour = sleep[0];
            selectedSleepMinute = sleep[1];
            selectedWakeHour = wake[0];
            selectedWakeMinute = wake[1];
            saveSleepButton.setText("更新作息");
        } else {
            selectedSleepHour = 22;
            selectedSleepMinute = 30;
            selectedWakeHour = 7;
            selectedWakeMinute = 0;
            saveSleepButton.setText("保存作息");
        }

        updateSleepPreview();
    }

    private void updateSleepPreview() {
        String sleepTime = formatTime(
                selectedSleepHour,
                selectedSleepMinute
        );
        String wakeTime = formatTime(
                selectedWakeHour,
                selectedWakeMinute
        );
        boolean passed = isSleepPassed(sleepTime, wakeTime);

        sleepTimeValue.setText(sleepTime);
        wakeTimeValue.setText(wakeTime);

        if (passed) {
            sleepPassStatus.setText("作息达标，可获得 +15 经验");
            sleepPassStatus.setTextColor(
                    getResources().getColor(R.color.checkin_done)
            );
        } else {
            sleepPassStatus.setText("未达标：需 23:00 前睡、8:30 前起");
            sleepPassStatus.setTextColor(
                    getResources().getColor(R.color.color_text_sub)
            );
        }
    }

    private void renderTodayDiary() {
        String today = DateUtils.today();
        todayDiaryIndex = findDiaryIndex(today);

        if (todayDiaryIndex >= 0) {
            DiaryRecord diary = state.diaries.get(todayDiaryIndex);
            pendingBroken = diary.broken;
            titleInput.setText(safe(diary.title));
            bodyInput.setText(safe(diary.body));
            saveButton.setText("更新今日日记");
        } else {
            pendingBroken = false;
            titleInput.setText("");
            bodyInput.setText("");
            saveButton.setText("保存今日日记");
        }

        updateStatusToggle();
    }

    private void renderStats() {
        String today = DateUtils.today();
        String monthPrefix = today.substring(0, 7);

        int monthlyBrokenCount = 0;

        for (DiaryRecord diary : state.diaries) {
            if (diary != null
                    && safe(diary.date).startsWith(monthPrefix)
                    && diary.broken) {
                monthlyBrokenCount++;
            }
        }

        int currentStreak =
                StatsEngine.currentNoBreakStreak(state);

        int todayExp = state.containsAwardKey(
                "diary_" + today
        ) ? 12 : 0;

        bindStat(
                statCurrentNoBreak,
                R.drawable.ic_core_no_break,
                "连续不破戒",
                String.valueOf(currentStreak),
                "天",
                false
        );

        bindStat(
                statMonthlyBroken,
                R.drawable.ic_core_streak,
                "本月破戒",
                String.valueOf(monthlyBrokenCount),
                "次",
                false
        );

        bindStat(
                statDiaryTotal,
                R.drawable.ic_exp_diary,
                "日记累计",
                String.valueOf(state.diaries.size()),
                "篇",
                false
        );

        bindStat(
                statTodayDiaryExp,
                R.drawable.ic_exp_all_complete,
                "今日经验",
                "+" + todayExp,
                "",
                true
        );
    }

    private void bindStat(
            View root,
            int iconRes,
            String label,
            String value,
            String unit,
            boolean accent
    ) {
        ImageView icon = root.findViewById(
                R.id.ivDiaryStatIcon
        );
        TextView labelView = root.findViewById(
                R.id.tvDiaryStatLabel
        );
        TextView valueView = root.findViewById(
                R.id.tvDiaryStatValue
        );
        TextView unitView = root.findViewById(
                R.id.tvDiaryStatUnit
        );

        icon.setImageResource(iconRes);
        labelView.setText(label);
        valueView.setText(value);
        unitView.setText(unit);

        if (accent) {
            valueView.setTextColor(
                    getResources().getColor(
                            R.color.checkin_done
                    )
            );
        }
    }

    private void renderRecentDiaries() {
        List<DiaryRow> rows = new ArrayList<>();

        for (int i = 0; i < state.diaries.size(); i++) {
            DiaryRecord diary = state.diaries.get(i);

            if (diary != null) {
                rows.add(new DiaryRow(i, diary));
            }
        }

        Collections.sort(
                rows,
                (left, right) -> safe(right.diary.date)
                        .compareTo(safe(left.diary.date))
        );

        if (rows.size() > RECENT_LIMIT) {
            rows = new ArrayList<>(
                    rows.subList(0, RECENT_LIMIT)
            );
        }

        diaryAdapter.submitList(rows);

        boolean empty = rows.isEmpty();
        emptyDiaries.setVisibility(
                empty ? View.VISIBLE : View.GONE
        );
        recentRecycler.setVisibility(
                empty ? View.GONE : View.VISIBLE
        );
    }

    private void setPendingBroken(boolean broken) {
        pendingBroken = broken;
        updateStatusToggle();
    }

    private void updateStatusToggle() {
        if (pendingBroken) {
            buttonNotBroken.setBackgroundResource(0);
            buttonBroken.setBackgroundResource(
                    R.drawable.bg_diary_toggle_selected
            );

            buttonNotBroken.setTextColor(
                    getResources().getColor(
                            R.color.color_text_main
                    )
            );
            buttonBroken.setTextColor(
                    getResources().getColor(
                            R.color.color_text_on_primary
                    )
            );

            todayStatusHint.setText(
                    "如实记录原因，反思后重新开始"
            );
        } else {
            buttonNotBroken.setBackgroundResource(
                    R.drawable.bg_diary_toggle_selected
            );
            buttonBroken.setBackgroundResource(0);

            buttonNotBroken.setTextColor(
                    getResources().getColor(
                            R.color.color_text_on_primary
                    )
            );
            buttonBroken.setTextColor(
                    getResources().getColor(
                            R.color.color_text_main
                    )
            );

            todayStatusHint.setText(
                    "保持本心，记录今日修行感悟"
            );
        }
    }

    private void saveTodaySleep() {
        String today = DateUtils.today();
        String sleepTime = formatTime(
                selectedSleepHour,
                selectedSleepMinute
        );
        String wakeTime = formatTime(
                selectedWakeHour,
                selectedWakeMinute
        );
        boolean passed = isSleepPassed(sleepTime, wakeTime);

        try {
            if (todaySleepIndex >= 0
                    && todaySleepIndex < state.sleeps.size()) {
                SleepRecord record = state.sleeps.get(todaySleepIndex);
                record.date = today;
                record.sleepTime = sleepTime;
                record.wakeTime = wakeTime;
                record.passed = passed;
            } else {
                state.sleeps.add(
                        0,
                        new SleepRecord(
                                today,
                                sleepTime,
                                wakeTime,
                                passed
                        )
                );
            }

            if (passed) {
                RewardEngine.awardSleep(
                        getActivity(),
                        state,
                        today
                );
            } else {
                removeAwardAndLogs("sleep_" + today);
                removeAwardAndLogs("all_done_" + today);
            }

            repo.save(state);
            state = repo.load();
            normalizeState();
            render();

            Toast.makeText(
                    getActivity(),
                    passed ? "作息已保存，今日达标" : "作息已保存，今日未达标",
                    Toast.LENGTH_SHORT
            ).show();
        } catch (Exception exception) {
            Toast.makeText(
                    getActivity(),
                    "保存作息失败：" + exception.getMessage(),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * 每个日期只保留一篇日记。
     * 当天已存在时更新原记录，不重复插入。
     */
    private void saveTodayDiary() {
        String title = titleInput.getText()
                .toString()
                .trim();

        String body = bodyInput.getText()
                .toString()
                .trim();

        if (title.isEmpty() || body.isEmpty()) {
            Toast.makeText(
                    getActivity(),
                    "标题和正文不能为空",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String today = DateUtils.today();

        try {
            if (todayDiaryIndex >= 0
                    && todayDiaryIndex < state.diaries.size()) {
                DiaryRecord diary =
                        state.diaries.get(todayDiaryIndex);

                diary.title = title;
                diary.body = body;
                diary.broken = pendingBroken;
                diary.date = today;

            } else {
                DiaryRecord diary = createDiaryRecord(
                        today,
                        title,
                        pendingBroken,
                        body
                );

                if (diary == null) {
                    Toast.makeText(
                            getActivity(),
                            "无法创建日记记录，请检查 DiaryRecord 模型",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                state.diaries.add(0, diary);
            }

            if (pendingBroken) {
                removeDiaryReward(today);
            } else {
                awardDiary(today);
            }

            repo.save(state);

            Toast.makeText(
                    getActivity(),
                    todayDiaryIndex >= 0
                            ? "今日日记已更新"
                            : "今日日记已保存",
                    Toast.LENGTH_SHORT
            ).show();

            state = repo.load();
            normalizeState();
            render();

        } catch (Exception exception) {
            Toast.makeText(
                    getActivity(),
                    "保存失败：" + exception.getMessage(),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * 优先使用现有 DiaryRecord.fromJson(JSONObject)，
     * 若模型没有该方法，再尝试常见构造函数。
     */
    private DiaryRecord createDiaryRecord(
            String date,
            String title,
            boolean broken,
            String body
    ) {
        try {
            JSONObject object = new JSONObject();
            object.put("id", UUID.randomUUID().toString());
            object.put("date", date);
            object.put("title", title);
            object.put("broken", broken);
            object.put("body", body);

            Method fromJson = DiaryRecord.class.getMethod(
                    "fromJson",
                    JSONObject.class
            );

            Object result = fromJson.invoke(null, object);

            if (result instanceof DiaryRecord) {
                return (DiaryRecord) result;
            }
        } catch (Throwable ignored) {
            // 继续尝试构造函数。
        }

        try {
            Constructor<DiaryRecord> constructor =
                    DiaryRecord.class.getDeclaredConstructor(
                            String.class,
                            String.class,
                            boolean.class,
                            String.class
                    );

            constructor.setAccessible(true);

            return constructor.newInstance(
                    date,
                    title,
                    broken,
                    body
            );
        } catch (Throwable ignored) {
        }

        try {
            Constructor<DiaryRecord> constructor =
                    DiaryRecord.class.getDeclaredConstructor();

            constructor.setAccessible(true);
            DiaryRecord diary = constructor.newInstance();

            diary.date = date;
            diary.title = title;
            diary.broken = broken;
            diary.body = body;

            trySetId(diary);

            return diary;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void trySetId(DiaryRecord diary) {
        try {
            Field field = DiaryRecord.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(
                    diary,
                    UUID.randomUUID().toString()
            );
        } catch (Throwable ignored) {
        }
    }

    private void awardDiary(String date) {
        try {
            Class<?> engine = Class.forName(
                    "com.selfdiscipline.realm.engine.RewardEngine"
            );

            for (Method method : engine.getMethods()) {
                if ("awardDiary".equals(method.getName())
                        && method.getParameterTypes().length == 3) {
                    method.invoke(
                            null,
                            getActivity(),
                            state,
                            date
                    );
                    break;
                }
            }

            // 如果项目中提供统一的“全完成”检查，则一并调用。
            for (Method method : engine.getMethods()) {
                if (("awardAllDone".equals(method.getName())
                        || "checkAllDone".equals(method.getName()))
                        && method.getParameterTypes().length == 3) {
                    method.invoke(
                            null,
                            getActivity(),
                            state,
                            date
                    );
                    break;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * 当日改为“破戒”时，撤销当天日记经验和全完成经验。
     */
    private void removeDiaryReward(String date) {
        removeAwardAndLogs("diary_" + date);
        removeAwardAndLogs("all_done_" + date);
    }

    private void removeAwardAndLogs(String key) {
        if (key == null || key.isEmpty()) {
            return;
        }

        state.awardedKeys.remove(key);

        for (int i = state.expLogs.size() - 1; i >= 0; i--) {
            ExperienceLog log = state.expLogs.get(i);

            if (log != null && key.equals(log.key)) {
                state.expLogs.remove(i);
            }
        }
    }

    private void showTimePicker(
            CharSequence title,
            int hour,
            int minute,
            TimePickAction action
    ) {
        TimePicker picker = new TimePicker(getActivity());
        picker.setIs24HourView(true);

        if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.M) {
            picker.setHour(hour);
            picker.setMinute(minute);
        } else {
            picker.setCurrentHour(hour);
            picker.setCurrentMinute(minute);
        }

        RealmDialog.showContent(
                getActivity(),
                title,
                picker,
                "确定",
                "取消",
                dialog -> {
                    int pickedHour;
                    int pickedMinute;

                    if (android.os.Build.VERSION.SDK_INT >=
                            android.os.Build.VERSION_CODES.M) {
                        pickedHour = picker.getHour();
                        pickedMinute = picker.getMinute();
                    } else {
                        pickedHour = picker.getCurrentHour();
                        pickedMinute = picker.getCurrentMinute();
                    }

                    if (action != null) {
                        action.onPicked(
                                pickedHour,
                                pickedMinute
                        );
                    }

                    return true;
                }
        );
    }

    private int findSleepIndex(String date) {
        for (int i = 0; i < state.sleeps.size(); i++) {
            SleepRecord record = state.sleeps.get(i);

            if (record != null
                    && date.equals(record.date)) {
                return i;
            }
        }

        return -1;
    }

    private boolean isSleepPassed(
            String sleepTime,
            String wakeTime
    ) {
        int sleepMinutes = timeToMinutes(sleepTime);
        int wakeMinutes = timeToMinutes(wakeTime);

        return sleepMinutes >= 0
                && wakeMinutes >= 0
                && sleepMinutes < 23 * 60
                && wakeMinutes <= 8 * 60 + 30;
    }

    private String formatTime(
            int hour,
            int minute
    ) {
        return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                hour,
                minute
        );
    }

    private int[] parseTime(
            String value,
            int defaultHour,
            int defaultMinute
    ) {
        if (value == null) {
            return new int[]{defaultHour, defaultMinute};
        }

        try {
            String[] parts = value.split(":");

            if (parts.length < 2) {
                return new int[]{defaultHour, defaultMinute};
            }

            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            if (hour < 0 || hour > 23
                    || minute < 0 || minute > 59) {
                return new int[]{defaultHour, defaultMinute};
            }

            return new int[]{hour, minute};
        } catch (Exception ignored) {
            return new int[]{defaultHour, defaultMinute};
        }
    }

    private int timeToMinutes(String value) {
        int[] result = parseTime(value, -1, -1);

        if (result[0] < 0 || result[1] < 0) {
            return -1;
        }

        return result[0] * 60 + result[1];
    }

    private int findDiaryIndex(String date) {
        for (int i = 0; i < state.diaries.size(); i++) {
            DiaryRecord diary = state.diaries.get(i);

            if (diary != null
                    && date.equals(diary.date)) {
                return i;
            }
        }

        return -1;
    }

    private void normalizeState() {
        if (state.diaries == null) {
            state.diaries = new ArrayList<>();
        }

        if (state.sleeps == null) {
            state.sleeps = new ArrayList<>();
        }

        if (state.awardedKeys == null) {
            state.awardedKeys = new ArrayList<>();
        }

        if (state.expLogs == null) {
            state.expLogs = new ArrayList<>();
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private interface TimePickAction {
        void onPicked(int hour, int minute);
    }

    private static class DiaryRow {
        final int stateIndex;
        final DiaryRecord diary;

        DiaryRow(int stateIndex, DiaryRecord diary) {
            this.stateIndex = stateIndex;
            this.diary = diary;
        }
    }

    private class DiaryAdapter
            extends RecyclerView.Adapter<DiaryAdapter.Holder> {

        private final List<DiaryRow> rows = new ArrayList<>();

        void submitList(List<DiaryRow> newRows) {
            rows.clear();

            if (newRows != null) {
                rows.addAll(newRows);
            }

            notifyDataSetChanged();
        }

        @Override
        public Holder onCreateViewHolder(
                ViewGroup parent,
                int viewType
        ) {
            View view = LayoutInflater
                    .from(parent.getContext())
                    .inflate(
                            R.layout.item_recent_diary,
                            parent,
                            false
                    );

            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(
                Holder holder,
                int position
        ) {
            DiaryRow row = rows.get(position);
            DiaryRecord diary = row.diary;

            holder.title.setText(
                    safe(diary.title).isEmpty()
                            ? "未命名日记"
                            : diary.title
            );

            holder.date.setText(
                    formatDiaryDate(diary.date)
            );

            holder.preview.setText(
                    safe(diary.body)
            );

            if (diary.broken) {
                holder.status.setText("破戒");
                holder.status.setTextColor(
                        Color.rgb(158, 68, 103)
                );
                holder.status.setBackgroundResource(
                        R.drawable.bg_diary_status_broken
                );
            } else {
                holder.status.setText("未破戒");
                holder.status.setTextColor(
                        getResources().getColor(
                                R.color.checkin_done
                        )
                );
                holder.status.setBackgroundResource(
                        R.drawable.bg_diary_status_clean
                );
            }

            holder.itemView.setOnClickListener(v ->
                    RecordDetailActivity.open(
                            getActivity(),
                            RecordDetailActivity.TYPE_DIARY,
                            row.stateIndex
                    )
            );

            holder.itemView.setOnLongClickListener(v -> {
                confirmDelete(row);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        class Holder extends RecyclerView.ViewHolder {

            final TextView title;
            final TextView date;
            final TextView preview;
            final TextView status;

            Holder(View itemView) {
                super(itemView);

                title = itemView.findViewById(
                        R.id.tvRecentDiaryTitle
                );
                date = itemView.findViewById(
                        R.id.tvRecentDiaryDate
                );
                preview = itemView.findViewById(
                        R.id.tvRecentDiaryPreview
                );
                status = itemView.findViewById(
                        R.id.tvRecentDiaryStatus
                );
            }
        }
    }

    private void confirmDelete(DiaryRow row) {
        RealmDialog.showConfirm(
                getActivity(),
                "删除日记",
                "确定删除这篇日记吗？",
                "删除",
                "取消",
                () -> deleteDiary(row)
        );
    }

    private void deleteDiary(DiaryRow row) {
        if (row.stateIndex < 0
                || row.stateIndex >= state.diaries.size()) {
            return;
        }

        DiaryRecord removed =
                state.diaries.remove(row.stateIndex);

        if (removed != null) {
            removeAwardAndLogs(
                    "diary_" + removed.date
            );
            removeAwardAndLogs(
                    "all_done_" + removed.date
            );
        }

        repo.save(state);
        render();

        Toast.makeText(
                getActivity(),
                "日记已删除",
                Toast.LENGTH_SHORT
        ).show();
    }

    private String formatDiaryDate(String date) {
        if (date == null || date.isEmpty()) {
            return "--";
        }

        if (DateUtils.today().equals(date)) {
            return "今天";
        }

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_MONTH, -1);

        String yesterdayText = new SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
        ).format(yesterday.getTime());

        if (yesterdayText.equals(date)) {
            return "昨天";
        }

        return date.length() >= 10
                ? date.substring(5, 10)
                : date;
    }
}

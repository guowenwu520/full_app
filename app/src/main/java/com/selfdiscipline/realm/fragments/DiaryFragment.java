package com.selfdiscipline.realm.fragments;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.selfdiscipline.realm.model.AppState;
import com.selfdiscipline.realm.model.DiaryRecord;
import com.selfdiscipline.realm.model.ExperienceLog;
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
 * 4. 当前/最长连续不破戒、本月破戒、本月完成统计
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

    private EditText titleInput;
    private EditText bodyInput;
    private TextView saveButton;

    private RecyclerView recentRecycler;
    private TextView emptyDiaries;
    private DiaryAdapter diaryAdapter;

    private TextView longestNoBreak;
    private TextView monthlyCompletion;

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

        longestNoBreak = root.findViewById(
                R.id.tvLongestNoBreak
        );
        monthlyCompletion = root.findViewById(
                R.id.tvMonthlyDiaryCompletion
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
        renderTodayDiary();
        renderStats();
        renderRecentDiaries();
        renderLongTermStats();
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

    private void renderLongTermStats() {
        int longest = calculateLongestNoBreakStreak();

        String monthPrefix =
                DateUtils.today().substring(0, 7);

        Set<String> diaryDates = new HashSet<>();

        for (DiaryRecord diary : state.diaries) {
            if (diary != null
                    && safe(diary.date).startsWith(monthPrefix)) {
                diaryDates.add(diary.date);
            }
        }

        Calendar calendar = Calendar.getInstance();
        int daysPassed = calendar.get(Calendar.DAY_OF_MONTH);

        longestNoBreak.setText(longest + " 天");
        monthlyCompletion.setText(
                diaryDates.size() + " / " + daysPassed
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

    private int calculateLongestNoBreakStreak() {
        Set<String> noBreakDates = new HashSet<>();

        for (DiaryRecord diary : state.diaries) {
            if (diary != null
                    && !diary.broken
                    && diary.date != null
                    && !diary.date.isEmpty()) {
                noBreakDates.add(diary.date);
            }
        }

        if (noBreakDates.isEmpty()) {
            return 0;
        }

        List<String> dates = new ArrayList<>(noBreakDates);
        Collections.sort(dates);

        SimpleDateFormat format = new SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
        );

        int longest = 1;
        int current = 1;

        for (int i = 1; i < dates.size(); i++) {
            try {
                Date previous = format.parse(dates.get(i - 1));
                Date now = format.parse(dates.get(i));

                if (previous == null || now == null) {
                    current = 1;
                    continue;
                }

                Calendar expected = Calendar.getInstance();
                expected.setTime(previous);
                expected.add(Calendar.DAY_OF_MONTH, 1);

                String expectedDate =
                        format.format(expected.getTime());

                if (expectedDate.equals(dates.get(i))) {
                    current++;
                } else {
                    current = 1;
                }

                longest = Math.max(longest, current);

            } catch (Exception ignored) {
                current = 1;
            }
        }

        return longest;
    }

    private void normalizeState() {
        if (state.diaries == null) {
            state.diaries = new ArrayList<>();
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
        new AlertDialog.Builder(getActivity())
                .setTitle("删除日记")
                .setMessage("确定删除这篇日记吗？")
                .setNegativeButton("取消", null)
                .setPositiveButton(
                        "删除",
                        (dialog, which) -> deleteDiary(row)
                )
                .show();
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

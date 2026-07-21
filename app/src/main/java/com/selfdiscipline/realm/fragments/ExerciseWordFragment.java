package com.selfdiscipline.realm.fragments;

import android.os.Bundle;
import android.text.InputType;
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
import com.selfdiscipline.realm.DiaryWriteActivity;
import com.selfdiscipline.realm.RecordListActivity;
import com.selfdiscipline.realm.data.AppRepository;
import com.selfdiscipline.realm.engine.RewardEngine;
import com.selfdiscipline.realm.engine.TrendDataBuilder;
import com.selfdiscipline.realm.model.AppState;
import com.selfdiscipline.realm.model.ExerciseRecord;
import com.selfdiscipline.realm.model.WeightRecord;
import com.selfdiscipline.realm.ui.RealmDialog;
import com.selfdiscipline.realm.util.DateUtils;
import com.selfdiscipline.realm.util.ExerciseFormat;
import com.selfdiscipline.realm.util.NumberFormatUtils;
import com.selfdiscipline.realm.view.IncomeTrendView;
import com.selfdiscipline.realm.view.WeightTrendView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 运动、体重与期货收入页面。
 * 单词模块已经从当前版本的界面和打卡逻辑中移除。
 */
public class ExerciseWordFragment extends BaseFragmentHelper {

    private AppRepository repo;
    private AppState state;

    private View statTodayCalories;
    private View statMonthlyCalories;
    private View statExerciseStreak;
    private View statCurrentWeight;

    private TextView todayExerciseMain;
    private TextView todayExerciseDuration;
    private TextView todayExerciseCalories;
    private TextView todayExerciseTime;

    private RecyclerView exerciseHistoryRecycler;
    private ExerciseAdapter exerciseAdapter;
    private WeightTrendView weightTrendView;
    private IncomeTrendView futuresIncomeTrendView;
    private TextView futuresIncomeSummary;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle stateBundle) {
        View root = inflater.inflate(R.layout.fragment_exercise_word, container, false);
        repo = new AppRepository(getActivity());
        state = repo.load();
        if (state == null) state = new AppState();

        bindViews(root);
        setupList();
        setupClicks(root);
        render();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (repo != null) {
            state = repo.load();
            if (state == null) state = new AppState();
            render();
        }
    }

    private void bindViews(View root) {
        statTodayCalories = root.findViewById(R.id.statTodayCalories);
        statMonthlyCalories = root.findViewById(R.id.statMonthlyCalories);
        statExerciseStreak = root.findViewById(R.id.statExerciseStreak);
        statCurrentWeight = root.findViewById(R.id.statCurrentWeight);

        todayExerciseMain = root.findViewById(R.id.tvTodayExerciseMain);
        todayExerciseDuration = root.findViewById(R.id.tvTodayExerciseDuration);
        todayExerciseCalories = root.findViewById(R.id.tvTodayExerciseCalories);
        todayExerciseTime = root.findViewById(R.id.tvTodayExerciseTime);

        exerciseHistoryRecycler = root.findViewById(R.id.recyclerExerciseHistory);
        weightTrendView = root.findViewById(R.id.weightTrendView);
        futuresIncomeTrendView = root.findViewById(R.id.futuresIncomeTrendView);
        futuresIncomeSummary = root.findViewById(R.id.tvFuturesIncomeSummary);
    }

    private void setupList() {
        exerciseAdapter = new ExerciseAdapter();
        exerciseHistoryRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        exerciseHistoryRecycler.setAdapter(exerciseAdapter);
        exerciseHistoryRecycler.setNestedScrollingEnabled(false);
    }

    private void setupClicks(View root) {
        root.findViewById(R.id.buttonAddExercise).setOnClickListener(v -> dialogAddExercise());
        root.findViewById(R.id.buttonAddWeight).setOnClickListener(v -> dialogAddWeight());
        root.findViewById(R.id.buttonAddFuturesIncome).setOnClickListener(v ->
                DiaryWriteActivity.openFuturesIncome(getActivity()));
        root.findViewById(R.id.buttonAllExercises).setOnClickListener(v ->
                RecordListActivity.open(getActivity(), RecordListActivity.TYPE_EXERCISES));
        root.findViewById(R.id.buttonAllWeights).setOnClickListener(v ->
                RecordListActivity.open(getActivity(), RecordListActivity.TYPE_WEIGHTS));
        root.findViewById(R.id.buttonAllFuturesIncome).setOnClickListener(v ->
                RecordListActivity.open(getActivity(), RecordListActivity.TYPE_FUTURES_INCOMES));
    }

    private void render() {
        if (state.exercises == null) state.exercises = new ArrayList<>();
        if (state.weights == null) state.weights = new ArrayList<>();
        if (state.futuresIncomes == null) state.futuresIncomes = new ArrayList<>();

        String today = DateUtils.today();
        String month = today.substring(0, 7);
        int todayCalories = 0;
        int monthCalories = 0;
        ExerciseRecord todayExercise = null;

        for (ExerciseRecord record : state.exercises) {
            if (record == null) continue;
            if (today.equals(record.date)) {
                todayCalories += record.calories;
                if (todayExercise == null) todayExercise = record;
            }
            if (record.date != null && record.date.startsWith(month)) monthCalories += record.calories;
        }

        bindStat(statTodayCalories, R.drawable.ic_ew_calories, "今日消耗", NumberFormatUtils.compact(todayCalories), "kcal");
        bindStat(statMonthlyCalories, R.drawable.ic_ew_calories, "本月累计", formatInt(monthCalories), "kcal");
        bindStat(statExerciseStreak, R.drawable.ic_ew_streak, "连续运动", NumberFormatUtils.compact(calculateExerciseStreak()), "天");

        WeightRecord latestWeight = latestWeight();
        bindStat(
                statCurrentWeight,
                R.drawable.ic_ew_weight,
                "当前体重",
                latestWeight == null ? "--" : String.format(Locale.getDefault(), "%.1f", latestWeight.weight),
                "kg"
        );

        renderTodayExercise(todayExercise);
        renderExerciseHistory();
        renderWeightTrend();
        renderFuturesTrend(today);
    }

    private void renderTodayExercise(ExerciseRecord record) {
        if (record == null) {
            todayExerciseMain.setText("今日尚未记录运动");
            todayExerciseDuration.setText("0 分钟");
            todayExerciseCalories.setText("0 kcal");
            todayExerciseTime.setText("时间：--");
            return;
        }

        String name = ExerciseFormat.name(record.content);
        int duration = ExerciseFormat.durationMinutes(record.content);
        todayExerciseMain.setText(name);
        todayExerciseDuration.setText(NumberFormatUtils.compact(duration) + " 分钟");
        todayExerciseCalories.setText(NumberFormatUtils.compact(record.calories) + " kcal");
        todayExerciseTime.setText("日期：" + safe(record.date));
    }

    private void renderExerciseHistory() {
        List<ExerciseRecord> rows = new ArrayList<>(state.exercises);
        Collections.sort(rows, (left, right) -> safe(right.date).compareTo(safe(left.date)));
        if (rows.size() > 3) rows = new ArrayList<>(rows.subList(0, 3));
        exerciseAdapter.submit(rows);
    }

    private void renderWeightTrend() {
        TrendDataBuilder.WeightTrendData trend =
                TrendDataBuilder.weightsNewestFirst(state.weights);
        weightTrendView.setData(trend.values, trend.labels);
    }

    private void renderFuturesTrend(String today) {
        TrendDataBuilder.IncomeTrendData trend =
                TrendDataBuilder.incomesNewestFirst(state.futuresIncomes, today);
        futuresIncomeTrendView.setData(trend.cumulativeValues, trend.labels);
        futuresIncomeSummary.setText(
                "累计 " + signedNumber(trend.totalIncome)
                        + " 元 · 今日 " + signedNumber(trend.todayIncome) + " 元"
        );
    }

    private void bindStat(View root, int iconRes, String label, String value, String unit) {
        ((ImageView) root.findViewById(R.id.ivEwStatIcon)).setImageResource(iconRes);
        ((TextView) root.findViewById(R.id.tvEwStatLabel)).setText(label);
        ((TextView) root.findViewById(R.id.tvEwStatValue)).setText(value);
        ((TextView) root.findViewById(R.id.tvEwStatUnit)).setText(unit);
    }

    private void dialogAddExercise() {
        LinearLayout box = dialogBox();
        EditText type = edit("运动类型，例如：跑步", InputType.TYPE_CLASS_TEXT);
        EditText duration = edit("时长（分钟）", InputType.TYPE_CLASS_NUMBER);
        EditText distance = edit("距离（公里，可留空）", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText calories = edit("消耗卡路里", InputType.TYPE_CLASS_NUMBER);
        box.addView(type);
        box.addView(duration);
        box.addView(distance);
        box.addView(calories);

        RealmDialog.showContent(
                getActivity(),
                "新增运动记录",
                box,
                "保存",
                "取消",
                dialog -> {
                    String typeValue = type.getText().toString().trim();
                    int durationValue = parseInt(duration, -1);
                    double distanceValue = parseDouble(distance, 0);
                    int caloriesValue = parseInt(calories, -1);
                    if (typeValue.isEmpty() || durationValue <= 0 || caloriesValue < 0) {
                        toast("请填写有效的运动类型、时长和卡路里");
                        return false;
                    }
                    try {
                        JSONObject object = new JSONObject();
                        object.put("date", DateUtils.today());
                        object.put("content", ExerciseFormat.content(typeValue, durationValue, distanceValue));
                        object.put("calories", caloriesValue);
                        state.exercises.add(0, ExerciseRecord.fromJson(object));
                        RewardEngine.RewardResult reward = RewardEngine.awardExercise(
                                getActivity(), state, DateUtils.today());
                        repo.save(state);
                        showReward(reward);
                        render();
                        return true;
                    } catch (Exception exception) {
                        toast("运动记录保存失败：" + exception.getMessage());
                        return false;
                    }
                }
        );
    }

    private void dialogAddWeight() {
        EditText input = edit("当前体重（kg）", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        RealmDialog.showContent(
                getActivity(),
                "记录体重",
                input,
                "保存",
                "取消",
                dialog -> {
                    double value = parseDouble(input, -1);
                    if (value <= 0) {
                        toast("请输入有效体重");
                        return false;
                    }
                    try {
                        JSONObject object = new JSONObject();
                        object.put("date", DateUtils.today());
                        object.put("weight", value);
                        state.weights.add(0, WeightRecord.fromJson(object));
                        RewardEngine.RewardResult reward = RewardEngine.afterAction(
                                getActivity(), state, DateUtils.today());
                        repo.save(state);
                        showReward(reward);
                        render();
                        return true;
                    } catch (Exception exception) {
                        toast("体重记录保存失败：" + exception.getMessage());
                        return false;
                    }
                }
        );
    }

    private WeightRecord latestWeight() {
        WeightRecord latest = null;
        for (WeightRecord record : state.weights) {
            if (record == null) continue;
            if (latest == null || safe(record.date).compareTo(safe(latest.date)) > 0) latest = record;
        }
        return latest;
    }

    private int calculateExerciseStreak() {
        Set<String> dates = new HashSet<>();
        for (ExerciseRecord record : state.exercises) {
            if (record != null && record.date != null && record.date.length() >= 10) {
                dates.add(record.date.substring(0, 10));
            }
        }
        String cursor = DateUtils.today();
        if (!dates.contains(cursor)) cursor = DateUtils.shift(cursor, -1);
        int streak = 0;
        while (dates.contains(cursor)) {
            streak++;
            cursor = DateUtils.shift(cursor, -1);
        }
        return streak;
    }

    private LinearLayout dialogBox() {
        LinearLayout box = new LinearLayout(getActivity());
        box.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(18);
        box.setPadding(padding, dp(4), padding, 0);
        return box;
    }

    private EditText edit(String hint, int inputType) {
        EditText editText = new EditText(getActivity());
        editText.setHint(hint);
        editText.setInputType(inputType);
        return editText;
    }

    private int parseInt(EditText editText, int defaultValue) {
        try {
            return Integer.parseInt(editText.getText().toString().trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private double parseDouble(EditText editText, double defaultValue) {
        try {
            return Double.parseDouble(editText.getText().toString().trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private String signedNumber(int value) {
        return NumberFormatUtils.compactSigned(value);
    }

    private String shortDate(String value) {
        if (value == null || value.isEmpty()) return "--";
        return value.length() >= 10 ? value.substring(5, 10) : value;
    }

    private String formatInt(int value) {
        return NumberFormatUtils.compact(value);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void toast(String text) {
        Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.Holder> {
        private final List<ExerciseRecord> items = new ArrayList<>();

        void submit(List<ExerciseRecord> data) {
            items.clear();
            if (data != null) items.addAll(data);
            notifyDataSetChanged();
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ew_exercise_history, parent, false));
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            ExerciseRecord record = items.get(position);
            String name = ExerciseFormat.name(record.content);
            int duration = ExerciseFormat.durationMinutes(record.content);
            double distance = ExerciseFormat.distanceKm(record.content);
            holder.type.setText(name);
            holder.date.setText(shortDate(record.date));
            holder.metric.setText(distance > 0
                    ? String.format(Locale.getDefault(), "%.1f km", distance)
                    : duration + " 分钟");
            holder.calories.setText(NumberFormatUtils.compact(record.calories) + " kcal");
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView type;
            final TextView date;
            final TextView metric;
            final TextView calories;

            Holder(View view) {
                super(view);
                type = view.findViewById(R.id.tvExerciseHistoryType);
                date = view.findViewById(R.id.tvExerciseHistoryDate);
                metric = view.findViewById(R.id.tvExerciseHistoryMetric);
                calories = view.findViewById(R.id.tvExerciseHistoryCalories);
            }
        }
    }
}

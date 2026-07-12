package com.selfdiscipline.realm.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.selfdiscipline.realm.R;
import com.selfdiscipline.realm.RecordListActivity;
import com.selfdiscipline.realm.data.AppRepository;
import com.selfdiscipline.realm.model.AppState;
import com.selfdiscipline.realm.model.ExerciseRecord;
import com.selfdiscipline.realm.model.WeightRecord;
import com.selfdiscipline.realm.ui.RealmDialog;
import com.selfdiscipline.realm.model.WordEntry;
import com.selfdiscipline.realm.util.DateUtils;
import com.selfdiscipline.realm.util.ExerciseFormat;
import com.selfdiscipline.realm.view.WeightTrendView;

import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 运动 / 单词合并页面。
 * <p>
 * 该版本通过反射兼容项目中已有的 Exercise、Weight、Word 模型字段，
 * 因此不会强制你替换原有数据模型。
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ExerciseWordFragment extends BaseFragmentHelper {

    private AppRepository repo;
    private AppState state;

    private LinearLayout exercisePage;
    private LinearLayout wordPage;
    private TextView title;
    private TextView subtitle;
    private TextView exerciseTab;
    private TextView wordTab;

    private View statTodayCalories;
    private View statMonthlyCalories;
    private View statExerciseStreak;
    private View statCurrentWeight;
    private View statWordTotal;
    private View statWordStreak;
    private View statWordToday;
    private View statWordReviewed;

    private TextView todayExerciseMain;
    private TextView todayExerciseDuration;
    private TextView todayExerciseCalories;
    private TextView todayExerciseTime;

    private RecyclerView exerciseHistoryRecycler;
    private ExerciseAdapter exerciseAdapter;
    private WeightTrendView weightTrendView;


    private RecyclerView todayWordsRecycler;
    private RecyclerView recentWordsRecycler;
    private TodayWordAdapter todayWordAdapter;
    private RecentWordAdapter recentWordAdapter;
    private TextView todayWordProgress;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle stateBundle) {
        View root = inflater.inflate(R.layout.fragment_exercise_word, container, false);
        repo = new AppRepository(getActivity());
        state = repo.load();
        if (state == null) state = new AppState();

        bindViews(root);
        setupLists();
        setupClicks(root);
        selectExerciseTab();
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
        exercisePage = root.findViewById(R.id.exercisePage);
        wordPage = root.findViewById(R.id.wordPage);
        title = root.findViewById(R.id.tvExerciseWordTitle);
        subtitle = root.findViewById(R.id.tvExerciseWordSubtitle);
        exerciseTab = root.findViewById(R.id.buttonExerciseTab);
        wordTab = root.findViewById(R.id.buttonWordTab);

        statTodayCalories = root.findViewById(R.id.statTodayCalories);
        statMonthlyCalories = root.findViewById(R.id.statMonthlyCalories);
        statExerciseStreak = root.findViewById(R.id.statExerciseStreak);
        statCurrentWeight = root.findViewById(R.id.statCurrentWeight);
        statWordTotal = root.findViewById(R.id.statWordTotal);
        statWordStreak = root.findViewById(R.id.statWordStreak);
        statWordToday = root.findViewById(R.id.statWordToday);
        statWordReviewed = root.findViewById(R.id.statWordReviewed);

        todayExerciseMain = root.findViewById(R.id.tvTodayExerciseMain);
        todayExerciseDuration = root.findViewById(R.id.tvTodayExerciseDuration);
        todayExerciseCalories = root.findViewById(R.id.tvTodayExerciseCalories);
        todayExerciseTime = root.findViewById(R.id.tvTodayExerciseTime);

        exerciseHistoryRecycler = root.findViewById(R.id.recyclerExerciseHistory);
        weightTrendView = root.findViewById(R.id.weightTrendView);


        todayWordsRecycler = root.findViewById(R.id.recyclerTodayWords);
        recentWordsRecycler = root.findViewById(R.id.recyclerRecentWords);
        todayWordProgress = root.findViewById(R.id.tvTodayWordProgress);

    }

    private void setupLists() {
        exerciseAdapter = new ExerciseAdapter();
        todayWordAdapter = new TodayWordAdapter();
        recentWordAdapter = new RecentWordAdapter();

        exerciseHistoryRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        exerciseHistoryRecycler.setAdapter(exerciseAdapter);
        exerciseHistoryRecycler.setNestedScrollingEnabled(false);

        todayWordsRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        todayWordsRecycler.setAdapter(todayWordAdapter);

        recentWordsRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        recentWordsRecycler.setAdapter(recentWordAdapter);
        recentWordsRecycler.setNestedScrollingEnabled(false);
    }

    private void setupClicks(View root) {
        exerciseTab.setOnClickListener(v -> selectExerciseTab());
        wordTab.setOnClickListener(v -> selectWordTab());

        root.findViewById(R.id.buttonAddExercise).setOnClickListener(v -> dialogAddExercise());
        root.findViewById(R.id.buttonAddWeight).setOnClickListener(v -> dialogAddWeight());
        root.findViewById(R.id.buttonAddWord).setOnClickListener(v -> dialogAddWord());

        root.findViewById(R.id.buttonAllExercises).setOnClickListener(v -> {
            try {
                RecordListActivity.open(getActivity(), RecordListActivity.TYPE_EXERCISES);
            } catch (Throwable ignored) {
            }
        });
        root.findViewById(R.id.buttonAllWords).setOnClickListener(v -> {
            try {
                RecordListActivity.open(getActivity(), RecordListActivity.TYPE_WORDS);
            } catch (Throwable ignored) {
            }
        });
    }

    private void selectExerciseTab() {
        exercisePage.setVisibility(View.VISIBLE);
        wordPage.setVisibility(View.GONE);
        title.setText("运动");
        subtitle.setText("动若灵风，体魄强健，方能御风而行。");
        exerciseTab.setBackgroundResource(R.drawable.bg_ew_tab_selected);
        wordTab.setBackgroundResource(0);
        exerciseTab.setTextColor(getResources().getColor(R.color.color_text_main));
        wordTab.setTextColor(getResources().getColor(R.color.color_text_sub));
    }

    private void selectWordTab() {
        exercisePage.setVisibility(View.GONE);
        wordPage.setVisibility(View.VISIBLE);
        title.setText("单词");
        subtitle.setText("日积月累，词海修行，日日精进。");
        wordTab.setBackgroundResource(R.drawable.bg_ew_tab_selected);
        exerciseTab.setBackgroundResource(0);
        wordTab.setTextColor(getResources().getColor(R.color.color_text_main));
        exerciseTab.setTextColor(getResources().getColor(R.color.color_text_sub));
    }

    private void render() {
        renderExercise();
        renderWords();
    }

    private void renderExercise() {
        List<Object> exercises = asObjects(readCollection(state, "exercises"));
        List<Object> weights = asObjects(readCollection(state, "weights"));
        String today = DateUtils.today();
        String month = today != null && today.length() >= 7 ? today.substring(0, 7) : "";

        int todayCalories = 0;
        int monthCalories = 0;
        Object todayExercise = null;

        for (Object item : exercises) {
            String date = readText(item, "date", "day", "recordDate", "createdDate", "time");
            int calories = readInt(item, 0, "calories", "calorie", "kcal", "burnedCalories");
            if (date != null && date.startsWith(today)) {
                todayCalories += calories;
                if (todayExercise == null) todayExercise = item;
            }
            if (date != null && date.startsWith(month)) monthCalories += calories;
        }

        int streak = calculateDateStreak(exercises, "date", "day", "recordDate", "createdDate");
        Float currentWeight = latestWeight(weights);

        bindStat(statTodayCalories, R.drawable.ic_ew_calories, "今日消耗", String.valueOf(todayCalories), "kcal");
        bindStat(statMonthlyCalories, R.drawable.ic_ew_calories, "本月累计", formatInt(monthCalories), "kcal");
        bindStat(statExerciseStreak, R.drawable.ic_ew_streak, "连续运动", String.valueOf(streak), "天");
        bindStat(statCurrentWeight, R.drawable.ic_ew_weight, "当前体重",
                currentWeight == null ? "--" : String.format(Locale.getDefault(), "%.1f", currentWeight), "kg");

        if (todayExercise == null) {
            todayExerciseMain.setText("今日尚未记录运动");
            todayExerciseDuration.setText("0 分钟");
            todayExerciseCalories.setText("0 kcal");
            todayExerciseTime.setText("时间：--");
        } else {
            String exerciseContent = safe(
                    readText(
                            todayExercise,
                            "content",
                            "type",
                            "name",
                            "exerciseType",
                            "title"
                    )
            );
            String type = ExerciseFormat.name(exerciseContent);
            double distance = readDouble(
                    todayExercise,
                    ExerciseFormat.distanceKm(exerciseContent),
                    "distance",
                    "km",
                    "mileage"
            );
            int duration = readInt(
                    todayExercise,
                    ExerciseFormat.durationMinutes(exerciseContent),
                    "duration",
                    "minutes",
                    "durationMinutes"
            );
            int calories = readInt(todayExercise, 0, "calories", "calorie", "kcal", "burnedCalories");
            String time = readText(todayExercise, "time", "dateTime", "createdAt", "date");
            todayExerciseMain.setText(type);
            todayExerciseDuration.setText(duration + " 分钟");
            todayExerciseCalories.setText(calories + " kcal");
            todayExerciseTime.setText("时间：" + shortTime(time));
        }

        List<Object> displayExercises = sortedByDateDesc(exercises, "date", "day", "recordDate", "createdDate", "time");
        if (displayExercises.size() > 3)
            displayExercises = new ArrayList<>(displayExercises.subList(0, 3));
        exerciseAdapter.submit(displayExercises);

        List<Object> weightRows = sortedByDateAsc(weights, "date", "day", "recordDate", "createdDate");
        if (weightRows.size() > 7) {
            weightRows = new ArrayList<>(weightRows.subList(weightRows.size() - 7, weightRows.size()));
        }
        List<Float> weightValues = new ArrayList<>();
        List<String> weightDates = new ArrayList<>();
        for (Object item : weightRows) {
            float value = (float) readDouble(item, 0, "weight", "value", "kg", "weightKg");
            if (value > 0) {
                weightValues.add(value);
                weightDates.add(shortDate(readText(item, "date", "day", "recordDate", "createdDate")));
            }
        }
        weightTrendView.setData(weightValues, weightDates);

    }

    private void renderWords() {
        List<Object> words = asObjects(readCollection(state, "words"));
        String today = DateUtils.today();
        String month = today != null && today.length() >= 7 ? today.substring(0, 7) : "";

        int todayCount = 0;
        int monthCount = 0;
        List<Object> todayWords = new ArrayList<>();

        for (Object item : words) {
            String date = readText(item, "date", "day", "createdDate", "createdAt", "addDate");
            if (date != null && date.startsWith(today)) {
                todayCount++;
                todayWords.add(item);
            }
            if (date != null && date.startsWith(month)) monthCount++;
        }

        int streak = calculateWordStreak();
        bindStat(statWordTotal, R.drawable.ic_ew_word_book, "单词总量", formatInt(words.size()), "个");
        bindStat(statWordStreak, R.drawable.ic_ew_streak, "连续背词", String.valueOf(streak), "天");
        bindStat(statWordToday, R.drawable.ic_ew_word_star, "今日新增", String.valueOf(todayCount), "个");
        bindStat(statWordReviewed, R.drawable.ic_ew_word_book, "本月新增", String.valueOf(monthCount), "个");

        if (todayWords.size() > 3) todayWords = new ArrayList<>(todayWords.subList(0, 3));
        todayWordAdapter.submit(todayWords);
        todayWordProgress.setText(todayCount + " 个");

        recentWordAdapter.submit(buildRecentWordRows(words));
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
                        JSONObject obj = new JSONObject();
                        obj.put("date", DateUtils.today());
                        obj.put(
                                "content",
                                buildExerciseContent(
                                        typeValue,
                                        durationValue,
                                        distanceValue
                                )
                        );
                        obj.put("calories", caloriesValue);

                        ExerciseRecord model = ExerciseRecord.fromJson(obj);
                        state.exercises.add(0, model);
                    } catch (Exception e) {
                        toast("运动记录保存失败：" + e.getMessage());
                        return false;
                    }
                    award("awardExercise");
                    saveAndRender();
                    return true;
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
                        JSONObject obj = new JSONObject();
                        obj.put("date", DateUtils.today());
                        obj.put("weight", value);

                        WeightRecord model = WeightRecord.fromJson(obj);
                        state.weights.add(0, model);
                    } catch (Exception e) {
                        toast("体重记录保存失败：" + e.getMessage());
                        return false;
                    }
                    saveAndRender();
                    return true;
                }
        );
    }

    private void dialogAddWord() {
        LinearLayout box = dialogBox();
        EditText word = edit("单词", InputType.TYPE_CLASS_TEXT);
        EditText meaning = edit("释义", InputType.TYPE_CLASS_TEXT);
        box.addView(word);
        box.addView(meaning);
        RealmDialog.showContent(
                getActivity(),
                "新增单词",
                box,
                "保存",
                "取消",
                dialog -> {
                    String w = word.getText().toString().trim();
                    String m = meaning.getText().toString().trim();
                    if (w.isEmpty() || m.isEmpty()) {
                        toast("单词和释义不能为空");
                        return false;
                    }
                    try {
                        JSONObject obj = new JSONObject();
                        obj.put("word", w);
                        obj.put("meaning", m);
                        obj.put("createdDate", DateUtils.today());
                        obj.put("correctCount", 0);
                        obj.put("wrongCount", 0);
                        obj.put("lastTestDate", "");

                        WordEntry model = WordEntry.fromJson(obj);
                        state.words.add(0, model);
                    } catch (Exception e) {
                        toast("单词保存失败：" + e.getMessage());
                        return false;
                    }
                    invokeStateDateMethod("addWordDate", DateUtils.today());
                    award("awardWord");
                    saveAndRender();
                    return true;
                }
        );
    }



    private void saveAndRender() {
        repo.save(state);
        render();
    }

    private void award(String methodName) {
        try {
            Class<?> engine = Class.forName("com.selfdiscipline.realm.engine.RewardEngine");
            for (Method method : engine.getMethods()) {
                if (method.getName().equals(methodName) && method.getParameterTypes().length == 3) {
                    method.invoke(null, getActivity(), state, DateUtils.today());
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void invokeStateDateMethod(String name, String date) {
        try {
            Method method = state.getClass().getMethod(name, String.class);
            method.invoke(state, date);
        } catch (Throwable ignored) {
        }
    }

    private Object createModel(String[] classNames) {
        for (String name : classNames) {
            try {
                Class<?> type = Class.forName(name);
                try {
                    Constructor<?> c = type.getDeclaredConstructor();
                    c.setAccessible(true);
                    return c.newInstance();
                } catch (Throwable ignored) {
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private List ensureList(String fieldName) {
        try {
            Field f = findField(state.getClass(), fieldName);
            f.setAccessible(true);
            Object value = f.get(state);
            if (value instanceof List) return (List) value;
            List list = new ArrayList();
            f.set(state, list);
            return list;
        } catch (Throwable e) {
            return new ArrayList();
        }
    }

    private void setMember(Object target, Object value, String... names) {
        for (String name : names) {
            try {
                Field f = findField(target.getClass(), name);
                f.setAccessible(true);
                Class<?> t = f.getType();
                if (t == int.class || t == Integer.class)
                    f.set(target, ((Number) value).intValue());
                else if (t == float.class || t == Float.class)
                    f.set(target, ((Number) value).floatValue());
                else if (t == double.class || t == Double.class)
                    f.set(target, ((Number) value).doubleValue());
                else if (t == boolean.class || t == Boolean.class) f.set(target, value);
                else f.set(target, value);
                return;
            } catch (Throwable ignored) {
            }
        }
    }

    private Collection<?> readCollection(Object target, String... names) {
        Object v = readMember(target, names);
        return v instanceof Collection ? (Collection<?>) v : null;
    }

    private Object readMember(Object target, String... names) {
        if (target == null) return null;
        for (String name : names) {
            try {
                Field f = findField(target.getClass(), name);
                f.setAccessible(true);
                Object v = f.get(target);
                if (v != null) return v;
            } catch (Throwable ignored) {
            }
            try {
                String suffix = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                Method m = target.getClass().getMethod("get" + suffix);
                Object v = m.invoke(target);
                if (v != null) return v;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> c = type;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private String readText(Object o, String... names) {
        Object v = readMember(o, names);
        return v == null ? null : String.valueOf(v);
    }

    private int readInt(Object o, int def, String... names) {
        Object v = readMember(o, names);
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return v == null ? def : (int) Double.parseDouble(String.valueOf(v));
        } catch (Throwable e) {
            return def;
        }
    }

    private double readDouble(Object o, double def, String... names) {
        Object v = readMember(o, names);
        if (v instanceof Number) return ((Number) v).doubleValue();
        try {
            return v == null ? def : Double.parseDouble(String.valueOf(v));
        } catch (Throwable e) {
            return def;
        }
    }

    private boolean readBoolean(Object o, boolean def, String... names) {
        Object v = readMember(o, names);
        if (v instanceof Boolean) return (Boolean) v;
        return v == null ? def : Boolean.parseBoolean(String.valueOf(v));
    }

    private List<Object> asObjects(Collection<?> c) {
        return c == null ? new ArrayList<>() : new ArrayList<Object>(c);
    }

    private List<Object> sortedByDateDesc(List<Object> items, String... dateFields) {
        List<Object> sorted = new ArrayList<>(items);
        Collections.sort(sorted, (left, right) -> safe(readText(right, dateFields)).compareTo(safe(readText(left, dateFields))));
        return sorted;
    }

    private List<Object> sortedByDateAsc(List<Object> items, String... dateFields) {
        List<Object> sorted = new ArrayList<>(items);
        Collections.sort(sorted, (left, right) -> safe(readText(left, dateFields)).compareTo(safe(readText(right, dateFields))));
        return sorted;
    }

    private Float latestWeight(List<Object> weights) {
        Object latest = null;
        String latestDate = "";
        for (Object item : weights) {
            String date = safe(readText(item, "date", "day", "recordDate", "createdDate"));
            if (latest == null || date.compareTo(latestDate) >= 0) {
                latest = item;
                latestDate = date;
            }
        }
        if (latest == null) return null;
        double value = readDouble(latest, 0, "weight", "value", "kg", "weightKg");
        return value > 0 ? (float) value : null;
    }

    private int calculateDateStreak(List<Object> items, String... dateFields) {
        Set<String> dates = new HashSet<>();
        for (Object item : items) {
            String d = readText(item, dateFields);
            if (d != null && d.length() >= 10) dates.add(d.substring(0, 10));
        }
        return streakFromDates(dates);
    }

    private int calculateWordStreak() {
        Collection<?> dates = readCollection(state, "wordDates", "wordStudyDates", "vocabularyDates");
        Set<String> set = new HashSet<>();
        if (dates != null) for (Object d : dates) if (d != null) set.add(String.valueOf(d));
        if (set.isEmpty()) {
            for (Object word : asObjects(readCollection(state, "words"))) {
                String d = readText(word, "date", "day", "createdDate", "addDate");
                if (d != null && d.length() >= 10) set.add(d.substring(0, 10));
            }
        }
        return streakFromDates(set);
    }

    private int streakFromDates(Set<String> set) {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        if (!set.contains(f.format(c.getTime()))) c.add(Calendar.DAY_OF_MONTH, -1);
        int streak = 0;
        while (set.contains(f.format(c.getTime()))) {
            streak++;
            c.add(Calendar.DAY_OF_MONTH, -1);
        }
        return streak;
    }




    private List<RecentWordRow> buildRecentWordRows(List<Object> words) {
        List<RecentWordRow> result = new ArrayList<>();
        List<Object> sorted = sortedByDateDesc(words, "date", "day", "createdDate", "addDate");
        int count = Math.min(5, sorted.size());
        for (int i = 0; i < count; i++) {
            Object w = sorted.get(i);
            String text = safe(readText(w, "word", "text", "name", "english"));
            String date = safe(readText(w, "date", "day", "createdDate", "addDate"));
            result.add(new RecentWordRow(text, "新增 1 个", shortDate(date)));
        }
        return result;
    }

    /**
     * 旧 ExerciseRecord 只有 date/content/calories。
     * 将新增页面的时长和距离编码进 content，保持旧 JSON 模型兼容。
     */
    private String buildExerciseContent(
            String type,
            int duration,
            double distance
    ) {
        return ExerciseFormat.content(type, duration, distance);
    }

    private int parseDurationFromContent(String content) {
        return ExerciseFormat.durationMinutes(content);
    }

    private double parseDistanceFromContent(String content) {
        return ExerciseFormat.distanceKm(content);
    }

    private LinearLayout dialogBox() {
        LinearLayout box = new LinearLayout(getActivity());
        box.setOrientation(LinearLayout.VERTICAL);
        int p = dp(18);
        box.setPadding(p, dp(4), p, 0);
        return box;
    }

    private EditText edit(String hint, int inputType) {
        EditText e = new EditText(getActivity());
        e.setHint(hint);
        e.setInputType(inputType);
        return e;
    }

    private int parseInt(EditText e, int def) {
        try {
            return Integer.parseInt(e.getText().toString().trim());
        } catch (Throwable x) {
            return def;
        }
    }

    private double parseDouble(EditText e, double def) {
        try {
            return Double.parseDouble(e.getText().toString().trim());
        } catch (Throwable x) {
            return def;
        }
    }

    private String nowText() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
    }

    private String shortTime(String value) {
        if (value == null || value.isEmpty()) return "--";
        int index = value.indexOf(' ');
        return index >= 0 && index + 1 < value.length() ? value.substring(index + 1) : value;
    }

    private String shortDate(String value) {
        if (value == null || value.isEmpty()) return "--";
        return value.length() >= 10 ? value.substring(5, 10) : value;
    }

    private String formatInt(int value) {
        return String.format(Locale.getDefault(), "%,d", value);
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
        private final List<Object> items = new ArrayList<>();

        void submit(List<Object> data) {
            items.clear();
            if (data != null) items.addAll(data);
            notifyDataSetChanged();
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup p, int t) {
            return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_ew_exercise_history, p, false));
        }

        @Override
        public void onBindViewHolder(Holder h, int pos) {
            Object item = items.get(pos);
            String date = safe(readText(item, "date", "day", "recordDate", "createdDate"));
            String exerciseContent = safe(
                    readText(
                            item,
                            "content",
                            "type",
                            "name",
                            "exerciseType",
                            "title"
                    )
            );
            String type = ExerciseFormat.name(exerciseContent);
            int duration = readInt(
                    item,
                    ExerciseFormat.durationMinutes(exerciseContent),
                    "duration",
                    "minutes",
                    "durationMinutes"
            );
            double distance = readDouble(
                    item,
                    ExerciseFormat.distanceKm(exerciseContent),
                    "distance",
                    "km",
                    "mileage"
            );
            int calories = readInt(item, 0, "calories", "calorie", "kcal", "burnedCalories");
            h.type.setText(type);
            h.date.setText(shortDate(date));
            h.metric.setText(distance > 0 ? String.format(Locale.getDefault(), "%.1f km", distance) : duration + " 分钟");
            h.calories.setText(calories + " kcal");
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView type, date, metric, calories;

            Holder(View v) {
                super(v);
                type = v.findViewById(R.id.tvExerciseHistoryType);
                date = v.findViewById(R.id.tvExerciseHistoryDate);
                metric = v.findViewById(R.id.tvExerciseHistoryMetric);
                calories = v.findViewById(R.id.tvExerciseHistoryCalories);
            }
        }
    }

    private class TodayWordAdapter extends RecyclerView.Adapter<TodayWordAdapter.Holder> {
        private final List<Object> items = new ArrayList<>();

        void submit(List<Object> data) {
            items.clear();
            if (data != null) items.addAll(data);
            notifyDataSetChanged();
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup p, int t) {
            return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_ew_today_word, p, false));
        }

        @Override
        public void onBindViewHolder(Holder h, int pos) {
            Object item = items.get(pos);
            h.word.setText(safe(readText(item, "word", "text", "name", "english")));
            h.meaning.setText(safe(readText(item, "meaning", "translation", "definition", "chinese")));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView word, meaning;

            Holder(View v) {
                super(v);
                word = v.findViewById(R.id.tvTodayWord);
                meaning = v.findViewById(R.id.tvTodayMeaning);
            }
        }
    }

    private static class RecentWordRow {
        final String source, count, date;

        RecentWordRow(String s, String c, String d) {
            source = s;
            count = c;
            date = d;
        }
    }

    private class RecentWordAdapter extends RecyclerView.Adapter<RecentWordAdapter.Holder> {
        private final List<RecentWordRow> items = new ArrayList<>();

        void submit(List<RecentWordRow> data) {
            items.clear();
            if (data != null) items.addAll(data);
            notifyDataSetChanged();
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup p, int t) {
            return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_ew_recent_word, p, false));
        }

        @Override
        public void onBindViewHolder(Holder h, int pos) {
            RecentWordRow r = items.get(pos);
            h.source.setText(r.source);
            h.count.setText(r.count);
            h.date.setText(r.date);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView source, count, date;

            Holder(View v) {
                super(v);
                source = v.findViewById(R.id.tvRecentWordSource);
                count = v.findViewById(R.id.tvRecentWordCount);
                date = v.findViewById(R.id.tvRecentWordDate);
            }
        }
    }
}
package com.selfdiscipline.realm.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.selfdiscipline.realm.R;
import com.selfdiscipline.realm.RecordDetailActivity;
import com.selfdiscipline.realm.RecordListActivity;
import com.selfdiscipline.realm.data.AppRepository;
import com.selfdiscipline.realm.data.RealmCatalog;
import com.selfdiscipline.realm.engine.StatsEngine;
import com.selfdiscipline.realm.model.AppState;
import com.selfdiscipline.realm.model.ExperienceLog;
import com.selfdiscipline.realm.model.RealmLevel;
import com.selfdiscipline.realm.adapter.RecentActivity;
import com.selfdiscipline.realm.adapter.RecentActivityAdapter;
import com.selfdiscipline.realm.ui.RealmDialog;
import com.selfdiscipline.realm.util.DateUtils;
import com.selfdiscipline.realm.util.NumberFormatUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 首页总览。
 *
 * 已重新绑定：
 * 1. 境界卡片
 * 2. 今日五项打卡卡片
 * 3. 今日可获取经验卡片
 * 4. 核心数据速览卡片
 * 5. 最近动态 RecyclerView
 * 6. 数据导入导出
 */
public class OverviewFragment extends BaseFragmentHelper {

    private static final int[] REALM_ICON_THRESHOLDS = {
            0, 3501, 15001, 40001, 82001, 130001, 228001, 374001,
            568001, 810001, 1100001, 1438001, 1824001
    };

    private static final int[] REALM_ICON_RES = {
            R.drawable.realm_stage_lianqi,
            R.drawable.realm_stage_zhuji,
            R.drawable.realm_stage_jindan,
            R.drawable.realm_stage_yuanying,
            R.drawable.realm_stage_huashen,
            R.drawable.realm_stage_lianxu,
            R.drawable.realm_stage_heti,
            R.drawable.realm_stage_dacheng,
            R.drawable.realm_stage_zhenxian,
            R.drawable.realm_stage_jinxian,
            R.drawable.realm_stage_taiyi,
            R.drawable.realm_stage_daluo,
            R.drawable.realm_stage_daozu
    };

    private static final int REQ_EXPORT_BACKUP = 501;
    private static final int REQ_IMPORT_BACKUP = 502;
    private static final int RECENT_ACTIVITY_LIMIT = 3;
    private static final String QUOTE_PREF = "daily_quote_cache";
    private static final String QUOTE_DATE_KEY = "quote_date";
    private static final String QUOTE_TEXT_KEY = "quote_text";
    private static final String QUOTE_AUTHOR_KEY = "quote_author";
    private static final String QUOTE_API = "https://v1.hitokoto.cn/?encode=json&max_length=26";

    private static final String[] FALLBACK_QUOTES = {
            "千里之行，始于足下。——老子",
            "真正的才智是刚毅的志向。——拿破仑",
            "天才是百分之一的灵感加百分之九十九的汗水。——爱迪生",
            "志不立，天下无可成之事。——王阳明",
            "不要等待时机，而要创造时机。——萧伯纳",
            "业精于勤，荒于嬉。——韩愈",
            "吾生也有涯，而知也无涯。——庄子"
    };

    private AppRepository repo;
    private AppState state;

    // 境界卡片
    private TextView realmName;
    private TextView realmRemainingExp;
    private TextView realmTotalExp;
    private TextView realmPercent;
    private ProgressBar realmProgress;
    private ImageView realmBadge;
    private TextView greetingTitle;
    private TextView greetingSubtitle;
    private final ExecutorService quoteExecutor = Executors.newSingleThreadExecutor();

    // 今日打卡
    private TextView checkinCount;

    private ImageView readingStatusIcon;
    private ImageView exerciseStatusIcon;
    private ImageView sleepStatusIcon;
    private ImageView diaryStatusIcon;

    private TextView readingStatusText;
    private TextView exerciseStatusText;
    private TextView sleepStatusText;
    private TextView diaryStatusText;

    // 今日经验
    private TextView readingExpValue;
    private TextView exerciseExpValue;
    private TextView sleepExpValue;
    private TextView diaryExpValue;
    private TextView allCompleteExpValue;

    // 核心数据
    private TextView disciplineStreakValue;
    private TextView futuresIncomeValue;
    private TextView noBreakStreakValue;
    private TextView todayCaloriesValue;
    private TextView currentWeightValue;
    private TextView monthlyReadingValue;

    // 最近动态
    private RecyclerView recentActivityRecycler;
    private RecentActivityAdapter recentActivityAdapter;

    // 备份
    private TextView backupStatus;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View root = inflater.inflate(R.layout.fragment_overview, container, false);

        repo = new AppRepository(getActivity());
        state = repo.load();

        bindViews(root);
        setupRecentActivity(root);
        setupClicks(root);
        render();

        return root;
    }

    private void bindViews(View root) {
        // 境界卡片
        realmName = root.findViewById(R.id.tvRealmName);
        realmRemainingExp = root.findViewById(R.id.tvRemainingExp);
        realmTotalExp = root.findViewById(R.id.tvTotalExp);
        realmPercent = root.findViewById(R.id.tvRealmPercent);
        realmProgress = root.findViewById(R.id.progressRealm);
        realmBadge = root.findViewById(R.id.ivRealmBadge);
        greetingTitle = root.findViewById(R.id.tvGreetingTitle);
        greetingSubtitle = root.findViewById(R.id.tvGreetingSubtitle);

        // 今日打卡卡片
        checkinCount = root.findViewById(R.id.tvCheckinCount);

        readingStatusIcon = root.findViewById(R.id.ivReadingStatus);
        exerciseStatusIcon = root.findViewById(R.id.ivExerciseStatus);
        sleepStatusIcon = root.findViewById(R.id.ivSleepStatus);
        diaryStatusIcon = root.findViewById(R.id.ivDiaryStatus);

        readingStatusText = root.findViewById(R.id.tvReadingStatus);
        exerciseStatusText = root.findViewById(R.id.tvExerciseStatus);
        sleepStatusText = root.findViewById(R.id.tvSleepStatus);
        diaryStatusText = root.findViewById(R.id.tvDiaryStatus);

        // 今日经验卡片
        readingExpValue = root.findViewById(R.id.tvReadingExpValue);
        exerciseExpValue = root.findViewById(R.id.tvExerciseExpValue);
        sleepExpValue = root.findViewById(R.id.tvSleepExpValue);
        diaryExpValue = root.findViewById(R.id.tvDiaryExpValue);
        allCompleteExpValue = root.findViewById(R.id.tvAllCompleteExpValue);

        // 核心数据卡片
        disciplineStreakValue = root.findViewById(R.id.tvDisciplineStreakValue);
        futuresIncomeValue = root.findViewById(R.id.tvFuturesIncomeValue);
        noBreakStreakValue = root.findViewById(R.id.tvNoBreakStreakValue);
        todayCaloriesValue = root.findViewById(R.id.tvTodayCaloriesValue);
        currentWeightValue = root.findViewById(R.id.tvCurrentWeightValue);
        monthlyReadingValue = root.findViewById(R.id.tvMonthlyReadingValue);

        // 最近动态
        recentActivityRecycler = root.findViewById(R.id.recyclerRecentActivity);
    }

    private void setupRecentActivity(View root) {
        recentActivityAdapter = new RecentActivityAdapter();

        recentActivityRecycler.setLayoutManager(
                new LinearLayoutManager(getActivity())
        );
        recentActivityRecycler.setAdapter(recentActivityAdapter);

        // RecyclerView 位于 ScrollView 内，关闭自身滚动，交给外层 ScrollView。
        recentActivityRecycler.setNestedScrollingEnabled(false);
        recentActivityRecycler.setOverScrollMode(View.OVER_SCROLL_NEVER);

        recentActivityAdapter.setOnItemClickListener((item, position) -> {
            if (position >= 0 && position < state.expLogs.size()) {
                RecordDetailActivity.open(
                        getActivity(),
                        RecordDetailActivity.TYPE_EXP,
                        position
                );
            }
        });

        root.findViewById(R.id.tvViewAllActivity).setOnClickListener(v ->
                RecordListActivity.open(
                        getActivity(),
                        RecordListActivity.TYPE_OVERVIEW
                )
        );
    }

    private void setupClicks(View root) {
        root.findViewById(R.id.button_export_data)
                .setOnClickListener(v -> exportData());

        root.findViewById(R.id.button_import_data)
                .setOnClickListener(v -> importData());
    }

    @Override
    public void onResume() {
        super.onResume();

        if (repo != null) {
            state = repo.load();
            render();
        }
    }

    private void render() {
        if (state == null) {
            return;
        }

        renderGreetingSection();
        renderRealm();
        renderTodayCheckin();
        renderTodayExperience();
        renderCoreStats();
        renderRecentActivity();
    }

    private void renderGreetingSection() {
        greetingTitle.setText(buildGreetingTitle());
        renderDailyQuote();
    }

    private String buildGreetingTitle() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String timeWord;
        if (hour < 5) {
            timeWord = "夜深了";
        } else if (hour < 9) {
            timeWord = "早上好";
        } else if (hour < 12) {
            timeWord = "上午好";
        } else if (hour < 14) {
            timeWord = "中午好";
        } else if (hour < 18) {
            timeWord = "下午好";
        } else if (hour < 22) {
            timeWord = "晚上好";
        } else {
            timeWord = "夜深了";
        }
        return "道友，" + timeWord + "！";
    }

    private void renderDailyQuote() {
        greetingSubtitle.setText(cachedQuoteOrFallback());

        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        final SharedPreferences sp = activity.getSharedPreferences(QUOTE_PREF, Activity.MODE_PRIVATE);
        final String today = DateUtils.today();
        if (today.equals(sp.getString(QUOTE_DATE_KEY, ""))
                && sp.contains(QUOTE_TEXT_KEY)) {
            return;
        }

        quoteExecutor.execute(() -> {
            String quote = fetchQuoteFromApi();
            if (quote == null || quote.trim().isEmpty()) {
                quote = fallbackQuoteForDate(today);
            }
            final String finalQuote = quote;
            sp.edit()
                    .putString(QUOTE_DATE_KEY, today)
                    .putString(QUOTE_TEXT_KEY, finalQuote)
                    .apply();

            Activity current = getActivity();
            if (current == null) {
                return;
            }
            current.runOnUiThread(() -> greetingSubtitle.setText(finalQuote));
        });
    }

    private String cachedQuoteOrFallback() {
        Activity activity = getActivity();
        if (activity == null) {
            return fallbackQuoteForDate(DateUtils.today());
        }

        SharedPreferences sp = activity.getSharedPreferences(QUOTE_PREF, Activity.MODE_PRIVATE);
        String today = DateUtils.today();
        String savedDate = sp.getString(QUOTE_DATE_KEY, "");
        String savedQuote = sp.getString(QUOTE_TEXT_KEY, "");

        if (today.equals(savedDate) && savedQuote != null && !savedQuote.trim().isEmpty()) {
            return savedQuote;
        }
        return fallbackQuoteForDate(today);
    }

    private String fetchQuoteFromApi() {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(QUOTE_API);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "SelfDisciplineRealm/1.0");

            if (connection.getResponseCode() != 200) {
                return null;
            }

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JSONObject json = new JSONObject(sb.toString());
            String hitokoto = json.optString("hitokoto", "").trim();
            String fromWho = json.optString("from_who", "").trim();
            String from = json.optString("from", "").trim();
            String author = !fromWho.isEmpty() ? fromWho : from;

            if (hitokoto.isEmpty()) {
                return null;
            }

            if (!author.isEmpty()) {
                return "“" + hitokoto + "” —— " + author;
            }
            return "“" + hitokoto + "”";
        } catch (Exception ignored) {
            return null;
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (Exception ignored) {
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String fallbackQuoteForDate(String date) {
        int index = Math.abs(date.hashCode()) % FALLBACK_QUOTES.length;
        return FALLBACK_QUOTES[index];
    }

    /**
     * 境界卡片。
     *
     * 这里的百分比使用“总经验 / 下一境界累计经验”，
     * 与你设计图中的 6280 / 8000 = 78.50% 一致。
     */
    private void renderRealm() {
        int xp = StatsEngine.totalXp(state);
        RealmLevel realm = RealmCatalog.current(xp);

        realmName.setText(realm.nameRes);
        realmTotalExp.setText(formatInteger(xp));
        realmBadge.setImageResource(realmIconByXp(xp));

        if (realm.isCap()) {
            realmRemainingExp.setText(R.string.text_realm_cap);
            realmPercent.setText("100.00%");
            realmProgress.setProgress(100);
            return;
        }

        int remaining = Math.max(0, realm.nextXp - xp);
        double percent = realm.nextXp <= 0
                ? 100.0
                : Math.max(0.0, Math.min(100.0, xp * 100.0 / realm.nextXp));

        realmRemainingExp.setText(formatInteger(remaining));
        realmPercent.setText(
                String.format(Locale.getDefault(), "%.2f%%", percent)
        );
        realmProgress.setProgress((int) Math.round(percent));
    }

    private int realmIconByXp(int xp) {
        int index = 0;
        for (int i = 0; i < REALM_ICON_THRESHOLDS.length; i++) {
            if (xp >= REALM_ICON_THRESHOLDS[i]) {
                index = i;
            }
        }
        return REALM_ICON_RES[index];
    }

    /**
     * 今日打卡卡片。
     */
    private void renderTodayCheckin() {
        String today = DateUtils.today();

        boolean readingDone = StatsEngine.hasReading(state, today);
        boolean exerciseDone = StatsEngine.hasExercise(state, today);
        boolean sleepDone = StatsEngine.hasSleepPassed(state, today);
        boolean noBreakDone = StatsEngine.hasDiaryNoBreak(state, today);

        int completedCount = 0;
        if (readingDone) completedCount++;
        if (exerciseDone) completedCount++;
        if (sleepDone) completedCount++;
        if (noBreakDone) completedCount++;

        checkinCount.setText(
                String.format(Locale.getDefault(), "%d/4", completedCount)
        );

        bindCheckinStatus(
                readingStatusIcon,
                readingStatusText,
                readingDone,
                R.drawable.ic_checkin_reading_done,
                R.drawable.ic_checkin_reading_pending
        );

        bindCheckinStatus(
                exerciseStatusIcon,
                exerciseStatusText,
                exerciseDone,
                R.drawable.ic_checkin_exercise_done,
                R.drawable.ic_checkin_exercise_pending
        );

        bindCheckinStatus(
                sleepStatusIcon,
                sleepStatusText,
                sleepDone,
                R.drawable.ic_checkin_sleep_done,
                R.drawable.ic_checkin_sleep_pending
        );

        diaryStatusIcon.setImageResource(
                noBreakDone
                        ? R.drawable.ic_checkin_diary_done
                        : R.drawable.ic_checkin_diary_pending
        );
        diaryStatusText.setText(noBreakDone ? "未破戒" : "已破戒");
        diaryStatusText.setTextColor(
                ContextCompat.getColor(
                        getActivity(),
                        noBreakDone ? R.color.checkin_done : R.color.checkin_pending
                )
        );
    }

    private void bindCheckinStatus(
            ImageView iconView,
            TextView statusView,
            boolean completed,
            int completedIcon,
            int pendingIcon
    ) {
        iconView.setImageResource(completed ? completedIcon : pendingIcon);

        statusView.setText(
                completed
                        ? R.string.checkin_completed
                        : R.string.checkin_pending
        );

        statusView.setTextColor(
                ContextCompat.getColor(
                        getActivity(),
                        completed
                                ? R.color.checkin_done
                                : R.color.checkin_pending
                )
        );
    }

    /**
     * 今日可获取经验卡片。
     *
     * 这些值是规则常量，不应再从旧的 format_today_progress 文本中拼接。
     */
    private void renderTodayExperience() {
        readingExpValue.setText("每页+1");
        exerciseExpValue.setText("+10");
        sleepExpValue.setText("+15");
        diaryExpValue.setText("+12");
        allCompleteExpValue.setText("+50");
    }

    /**
     * 核心数据速览卡片。
     */
    private void renderCoreStats() {
        String today = DateUtils.today();

        disciplineStreakValue.setText(
                NumberFormatUtils.compact(StatsEngine.currentSelfDisciplineStreak(state))
        );

        futuresIncomeValue.setText(
                NumberFormatUtils.compact(StatsEngine.totalFuturesIncome(state))
        );

        noBreakStreakValue.setText(
                NumberFormatUtils.compact(StatsEngine.currentNoBreakStreak(state))
        );

        todayCaloriesValue.setText(
                NumberFormatUtils.compact(calculateTodayCalories(today))
        );

        Double latestWeight = findLatestWeight();
        currentWeightValue.setText(
                latestWeight == null
                        ? "--"
                        : String.format(
                        Locale.getDefault(),
                        "%.1f",
                        latestWeight
                )
        );

        monthlyReadingValue.setText(
                NumberFormatUtils.compact(calculateMonthlyReadingPages(today))
        );
    }

    /**
     * 最近动态直接使用现有 ExperienceLog。
     * 默认取 state.expLogs 前三条，保持与你旧代码相同的顺序。
     */
    private void renderRecentActivity() {
        List<RecentActivity> activities = new ArrayList<>();

        int count = Math.min(
                RECENT_ACTIVITY_LIMIT,
                state.expLogs == null ? 0 : state.expLogs.size()
        );

        for (int i = 0; i < count; i++) {
            ExperienceLog log = state.expLogs.get(i);
            String source = log.source == null ? "" : log.source;

            activities.add(
                    new RecentActivity(
                            activityIcon(source),
                            activityType(source),
                            buildActivityContent(log),
                            formatActivityTime(log.date)
                    )
            );
        }

        recentActivityAdapter.submitList(activities);
        recentActivityRecycler.setVisibility(
                activities.isEmpty() ? View.GONE : View.VISIBLE
        );
    }

    private int activityIcon(String source) {
        if (containsAny(source, "阅读", "书籍", "读书")) {
            return R.drawable.ic_exp_reading;
        }
        if (containsAny(source, "运动", "卡路里", "健身")) {
            return R.drawable.ic_exp_exercise;
        }
        if (containsAny(source, "作息", "睡眠", "早起")) {
            return R.drawable.ic_exp_sleep;
        }
        if (containsAny(source, "单词", "背词", "抽查")) {
            return R.drawable.ic_exp_word;
        }
        if (containsAny(source, "日记", "破戒")) {
            return R.drawable.ic_exp_diary;
        }
        if (containsAny(source, "勋章", "奖励", "全完成")) {
            return R.drawable.ic_exp_all_complete;
        }
        return R.drawable.ic_xp;
    }

    private String activityType(String source) {
        if (containsAny(source, "阅读", "书籍", "读书")) {
            return "阅读打卡：";
        }
        if (containsAny(source, "期货", "盈利", "亏损")) {
            return "期货收入：";
        }
        if (containsAny(source, "运动", "卡路里", "健身")) {
            return "运动打卡：";
        }
        if (containsAny(source, "作息", "睡眠", "早起")) {
            return "作息打卡：";
        }
        if (containsAny(source, "单词", "背词", "抽查")) {
            return "单词学习：";
        }
        if (containsAny(source, "日记", "破戒")) {
            return "日记记录：";
        }
        if (containsAny(source, "勋章", "奖励", "全完成")) {
            return "获得奖励：";
        }
        return "经验记录：";
    }

    private String buildActivityContent(ExperienceLog log) {
        String source = log.source == null ? "" : log.source.trim();

        if (source.isEmpty()) {
            return NumberFormatUtils.compactSigned(log.points) + " 经验";
        }

        String signed = NumberFormatUtils.compactSigned(log.points);
        return source + "，" + signed + " 经验";
    }

    /**
     * ExperienceLog 当前只有日期时，先直接显示日期。
     * 后续若模型增加时间戳，可在这里改成“1小时前”等相对时间。
     */
    private String formatActivityTime(String date) {
        if (date == null || date.trim().isEmpty()) {
            return "";
        }

        if (DateUtils.today().equals(date)) {
            return "今天";
        }

        return date;
    }

    private boolean containsAny(String text, String... keys) {
        if (text == null) {
            return false;
        }

        for (String key : keys) {
            if (text.contains(key)) {
                return true;
            }
        }

        return false;
    }

    private String formatInteger(int value) {
        return NumberFormatUtils.compact(value);
    }

    /*
     * ============================================================
     * 以下三个统计方法使用反射读取模型字段。
     *
     * 原因：你这次只提供了 OverviewFragment，没有提供
     * ExerciseRecord、WeightRecord、ReadingLog 的实际字段名。
     *
     * 该写法能够兼容 date/calories/weight/fromPage/toPage 等常见命名，
     * 不需要修改当前模型即可编译。
     *
     * 等你确定模型字段后，建议把它们改成直接字段访问。
     * ============================================================
     */

    private int calculateTodayCalories(String today) {
        if (state.exercises == null) {
            return 0;
        }

        int total = 0;

        for (Object exercise : state.exercises) {
            String date = readText(
                    exercise,
                    "date",
                    "day",
                    "recordDate",
                    "createdDate"
            );

            if (!today.equals(date)) {
                continue;
            }

            Number calories = readNumber(
                    exercise,
                    "calories",
                    "calorie",
                    "kcal",
                    "burnedCalories",
                    "calorieBurned"
            );

            if (calories != null) {
                total += calories.intValue();
            }
        }

        return total;
    }

    private Double findLatestWeight() {
        if (state.weights == null || state.weights.isEmpty()) {
            return null;
        }

        Object latestRecord = null;
        String latestDate = "";

        for (Object record : state.weights) {
            String date = readText(
                    record,
                    "date",
                    "day",
                    "recordDate",
                    "createdDate"
            );

            if (latestRecord == null
                    || (date != null && date.compareTo(latestDate) >= 0)) {
                latestRecord = record;
                latestDate = date == null ? "" : date;
            }
        }

        Number weight = readNumber(
                latestRecord,
                "weight",
                "value",
                "kg",
                "weightKg"
        );

        return weight == null ? null : weight.doubleValue();
    }

    private int calculateMonthlyReadingPages(String today) {
        if (today == null || today.length() < 7) {
            return 0;
        }

        String monthPrefix = today.substring(0, 7);

        Collection<?> topLevelLogs = readCollection(
                state,
                "readingLogs",
                "readingRecords",
                "pageLogs",
                "readingHistory",
                "bookProgressLogs"
        );

        int total = sumReadingPages(topLevelLogs, monthPrefix);

        // 如果 AppState 没有独立阅读记录，则尝试读取每本书内部的历史。
        if (total == 0 && state.books != null) {
            for (Object book : state.books) {
                Collection<?> bookLogs = readCollection(
                        book,
                        "readingLogs",
                        "readingRecords",
                        "pageLogs",
                        "history",
                        "progressLogs"
                );

                total += sumReadingPages(bookLogs, monthPrefix);
            }
        }

        return total;
    }

    private int sumReadingPages(
            Collection<?> records,
            String monthPrefix
    ) {
        if (records == null) {
            return 0;
        }

        int total = 0;

        for (Object record : records) {
            String date = readText(
                    record,
                    "date",
                    "day",
                    "recordDate",
                    "createdDate"
            );

            if (date == null || !date.startsWith(monthPrefix)) {
                continue;
            }

            Number directPages = readNumber(
                    record,
                    "pages",
                    "pageCount",
                    "readPages",
                    "pagesRead",
                    "deltaPages",
                    "addedPages"
            );

            if (directPages != null) {
                total += Math.max(0, directPages.intValue());
                continue;
            }

            Number oldPage = readNumber(
                    record,
                    "oldPage",
                    "fromPage",
                    "startPage",
                    "previousPage"
            );

            Number newPage = readNumber(
                    record,
                    "newPage",
                    "toPage",
                    "endPage",
                    "currentPage"
            );

            if (oldPage != null && newPage != null) {
                total += Math.max(
                        0,
                        newPage.intValue() - oldPage.intValue()
                );
            }
        }

        return total;
    }

    private Collection<?> readCollection(
            Object target,
            String... names
    ) {
        Object value = readMember(target, names);

        if (value instanceof Collection<?>) {
            return (Collection<?>) value;
        }

        return null;
    }

    private String readText(
            Object target,
            String... names
    ) {
        Object value = readMember(target, names);
        return value == null ? null : String.valueOf(value);
    }

    private Number readNumber(
            Object target,
            String... names
    ) {
        Object value = readMember(target, names);

        if (value instanceof Number) {
            return (Number) value;
        }

        if (value != null) {
            try {
                return Double.parseDouble(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                // 不是数值，继续返回 null。
            }
        }

        return null;
    }

    private Object readMember(
            Object target,
            String... names
    ) {
        if (target == null) {
            return null;
        }

        for (String name : names) {
            Object fieldValue = readField(target, name);
            if (fieldValue != null) {
                return fieldValue;
            }

            Object getterValue = invokeGetter(target, name);
            if (getterValue != null) {
                return getterValue;
            }
        }

        return null;
    }

    private Object readField(Object target, String fieldName) {
        Class<?> type = target.getClass();

        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (Exception ignored) {
                type = type.getSuperclass();
            }
        }

        return null;
    }

    private Object invokeGetter(Object target, String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        String suffix = Character.toUpperCase(name.charAt(0))
                + name.substring(1);

        String[] methodNames = {
                "get" + suffix,
                "is" + suffix
        };

        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (Exception ignored) {
                // 继续尝试下一个 getter。
            }
        }

        return null;
    }

    private void exportData() {
        String fileName = "self_discipline_realm_backup_"
                + DateUtils.today()
                + ".zip";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        startActivityForResult(intent, REQ_EXPORT_BACKUP);
    }

    private void importData() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivityForResult(intent, REQ_IMPORT_BACKUP);
    }

    @Override
    public void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK
                || data == null
                || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();

        if (requestCode == REQ_EXPORT_BACKUP) {
            writeBackup(uri);
        } else if (requestCode == REQ_IMPORT_BACKUP) {
            readBackupAndConfirm(uri);
        }
    }

    private void writeBackup(Uri uri) {
        try (OutputStream out = getActivity()
                .getContentResolver()
                .openOutputStream(uri)) {

            if (out == null) {
                throw new IllegalStateException(
                        "Cannot open output stream"
                );
            }

            repo.exportBackupZip(getActivity(), out);
            out.flush();

            setBackupStatus(
                    getString(R.string.toast_export_success)
            );

            Toast.makeText(
                    getActivity(),
                    R.string.toast_export_success,
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {
            setBackupStatus(
                    getString(R.string.toast_export_failed)
            );

            Toast.makeText(
                    getActivity(),
                    R.string.toast_export_failed,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void readBackupAndConfirm(Uri uri) {
        RealmDialog.showConfirm(
                getActivity(),
                R.string.dialog_import_title,
                getString(R.string.dialog_import_message),
                R.string.button_import_confirm,
                R.string.dialog_cancel,
                () -> applyImport(uri)
        );
    }

    private void applyImport(Uri uri) {
        try (InputStream in = getActivity()
                .getContentResolver()
                .openInputStream(uri)) {

            if (in == null) {
                throw new IllegalStateException(
                        "Cannot open input stream"
                );
            }

            repo.importBackupAuto(getActivity(), in);
            state = repo.load();

            setBackupStatus(
                    getString(R.string.toast_import_success)
            );

            Toast.makeText(
                    getActivity(),
                    R.string.toast_import_success,
                    Toast.LENGTH_SHORT
            ).show();

            render();

        } catch (Exception e) {
            setBackupStatus(
                    getString(R.string.toast_import_failed)
            );

            Toast.makeText(
                    getActivity(),
                    R.string.toast_import_failed,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void setBackupStatus(String text) {
        if (backupStatus != null) {
            backupStatus.setText(text);
        }
    }


    @Override
    public void onDestroy() {
        quoteExecutor.shutdownNow();
        super.onDestroy();
    }
}

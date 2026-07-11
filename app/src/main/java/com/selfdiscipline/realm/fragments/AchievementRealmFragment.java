package com.selfdiscipline.realm.fragments;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.selfdiscipline.realm.R;
import com.selfdiscipline.realm.RecordDetailActivity;
import com.selfdiscipline.realm.RecordListActivity;
import com.selfdiscipline.realm.data.AppRepository;
import com.selfdiscipline.realm.data.RealmCatalog;
import com.selfdiscipline.realm.engine.StatsEngine;
import com.selfdiscipline.realm.model.AppState;
import com.selfdiscipline.realm.model.DiaryRecord;
import com.selfdiscipline.realm.model.ExperienceLog;
import com.selfdiscipline.realm.model.RealmLevel;
import com.selfdiscipline.realm.model.WeightRecord;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 勋章与境界主页。
 *
 * 对应布局：
 * res/layout/fragment_achievement_realm.xml
 *
 * 说明：
 * - 境界使用现有 RealmCatalog、RealmLevel、StatsEngine。
 * - 经验明细使用 state.expLogs。
 * - 勋章优先识别 state.awardedKeys 中已有的永久解锁记录；
 *   若旧项目没有统一的勋章 key，则使用历史最高指标作为显示兜底。
 */
public class AchievementRealmFragment extends BaseFragmentHelper {

    private static final int EXP_PREVIEW_LIMIT = 6;

    // 五个勋章品类。
    private static final String[] CATEGORY_KEYS = {
            "discipline", "weight", "calories", "nobreak", "words"
    };

    private static final String[] CATEGORY_NAMES = {
            "连续自律", "减脂体重", "卡路里累计", "连续不破戒", "累计背单词"
    };

    // 青铜、白银、黄金、铂金、钻石。
    private static final String[] TIER_KEYS = {
            "bronze", "silver", "gold", "platinum", "diamond"
    };

    private static final String[] TIER_NAMES = {
            "青铜", "白银", "黄金", "铂金", "钻石"
    };

    /*
     * 若原项目已有固定门槛，只需替换此数组。
     *
     * 行：五个品类。
     * 列：青铜、白银、黄金、铂金、钻石。
     */
    private static final double[][] THRESHOLDS = {
            {7, 30, 100, 180, 365},              // 连续自律：天
            {2, 5, 10, 15, 20},                  // 最大减重：kg
            {5000, 20000, 50000, 100000, 200000},// 累计卡路里
            {7, 30, 100, 180, 365},              // 最长不破戒：天
            {100, 500, 1000, 3000, 5000}         // 累计单词：个
    };

    private AppRepository repo;
    private AppState state;

    private ImageView realmEmblem;
    private TextView realmName;
    private TextView totalXp;
    private TextView remainingXp;
    private TextView realmPercent;
    private TextView realmDescription;
    private ProgressBar realmProgress;

    private RecyclerView realmRecycler;
    private RecyclerView medalRecycler;
    private RecyclerView experienceRecycler;
    private TextView unlockedCount;
    private TextView emptyExperience;

    private RealmStageAdapter realmAdapter;
    private MedalAdapter medalAdapter;
    private ExperienceAdapter experienceAdapter;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View root = inflater.inflate(
                R.layout.fragment_achievement_realm,
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
        setupLists();
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
        realmEmblem = root.findViewById(
                R.id.ivAchievementRealmEmblem
        );
        realmName = root.findViewById(
                R.id.tvAchievementRealmName
        );
        totalXp = root.findViewById(
                R.id.tvAchievementTotalXp
        );
        remainingXp = root.findViewById(
                R.id.tvAchievementRemainingXp
        );
        realmPercent = root.findViewById(
                R.id.tvAchievementRealmPercent
        );
        realmDescription = root.findViewById(
                R.id.tvAchievementRealmDescription
        );
        realmProgress = root.findViewById(
                R.id.progressAchievementRealm
        );

        realmRecycler = root.findViewById(
                R.id.recyclerRealmStages
        );
        medalRecycler = root.findViewById(
                R.id.recyclerMedalWall
        );
        experienceRecycler = root.findViewById(
                R.id.recyclerExperienceDetail
        );

        unlockedCount = root.findViewById(
                R.id.tvUnlockedMedalCount
        );
        emptyExperience = root.findViewById(
                R.id.tvEmptyExperience
        );
    }

    private void setupLists() {
        realmAdapter = new RealmStageAdapter();
        medalAdapter = new MedalAdapter();
        experienceAdapter = new ExperienceAdapter();

        LinearLayoutManager realmManager =
                new LinearLayoutManager(
                        getActivity(),
                        LinearLayoutManager.HORIZONTAL,
                        false
                );

        realmRecycler.setLayoutManager(realmManager);
        realmRecycler.setAdapter(realmAdapter);
        realmRecycler.setOverScrollMode(View.OVER_SCROLL_NEVER);

        medalRecycler.setLayoutManager(
                new GridLayoutManager(getActivity(), 5)
        );
        medalRecycler.setAdapter(medalAdapter);
        medalRecycler.setNestedScrollingEnabled(false);
        medalRecycler.setOverScrollMode(View.OVER_SCROLL_NEVER);

        experienceRecycler.setLayoutManager(
                new LinearLayoutManager(getActivity())
        );
        experienceRecycler.setAdapter(experienceAdapter);
        experienceRecycler.setNestedScrollingEnabled(false);
        experienceRecycler.setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    private void setupClicks(View root) {
        View.OnClickListener openExperience = v ->
                RecordListActivity.open(
                        getActivity(),
                        RecordListActivity.TYPE_EXP_LOGS
                );

        root.findViewById(R.id.buttonAllExperience)
                .setOnClickListener(openExperience);

        root.findViewById(R.id.buttonAchievementCalendar)
                .setOnClickListener(openExperience);
    }

    private void render() {
        normalizeState();
        int xp = StatsEngine.totalXp(state);

        renderCurrentRealm(xp);
        renderRealmStages(xp);
        renderMedals();
        renderExperience();
    }

    private void renderCurrentRealm(int xp) {
        RealmLevel realm = RealmCatalog.current(xp);
        int major = majorRealmIndex(xp);

        realmName.setText(realm.nameRes);
        totalXp.setText(formatNumber(xp));
        realmEmblem.setImageResource(realmIcon(major));
        realmDescription.setText(descriptionForXp(xp));

        if (realm.isCap()) {
            remainingXp.setText("已圆满");
            realmPercent.setText("100.00%");
            realmProgress.setProgress(100);
            return;
        }

        int remaining = Math.max(0, realm.nextXp - xp);
        double percent = realm.nextXp <= 0
                ? 100.0
                : Math.min(100.0, xp * 100.0 / realm.nextXp);

        remainingXp.setText(formatNumber(remaining));
        realmPercent.setText(
                String.format(
                        Locale.getDefault(),
                        "%.2f%%",
                        percent
                )
        );
        realmProgress.setProgress(
                (int) Math.round(percent)
        );
    }

    private void renderRealmStages(int xp) {
        int currentMajor = majorRealmIndex(xp);
        List<RealmStageItem> items = new ArrayList<>();

        String[] names = {
                "练气", "筑基", "金丹", "元婴", "化神"
        };

        for (int i = 0; i < names.length; i++) {
            items.add(
                    new RealmStageItem(
                            names[i],
                            realmIcon(i),
                            i,
                            i <= currentMajor,
                            i == currentMajor
                    )
            );
        }

        realmAdapter.submitList(items);
    }

    private void renderMedals() {
        double[] metrics = {
                StatsEngine.maxSelfDisciplineStreak(state),
                calculateMaxWeightLoss(),
                calculateTotalCalories(),
                calculateLongestNoBreakStreak(),
                state.words == null ? 0 : state.words.size()
        };

        List<MedalItem> medals = new ArrayList<>();
        int unlocked = 0;

        /*
         * 先按等级，再按品类添加：
         * 第一行五枚青铜，第二行五枚白银……
         * 视觉上形成五个纵向品类。
         */
        for (int tier = 0; tier < TIER_KEYS.length; tier++) {
            for (int category = 0;
                 category < CATEGORY_KEYS.length;
                 category++) {

                boolean isUnlocked = isMedalUnlocked(
                        category,
                        tier,
                        metrics[category]
                );

                if (isUnlocked) {
                    unlocked++;
                }

                medals.add(
                        new MedalItem(
                                category,
                                tier,
                                CATEGORY_NAMES[category],
                                TIER_NAMES[tier],
                                medalIcon(category, tier),
                                metrics[category],
                                THRESHOLDS[category][tier],
                                isUnlocked
                        )
                );
            }
        }

        unlockedCount.setText(
                "已获 " + unlocked + " 枚"
        );
        medalAdapter.submitList(medals);
    }

    private boolean isMedalUnlocked(
            int category,
            int tier,
            double metric
    ) {
        String categoryKey = CATEGORY_KEYS[category];
        String tierKey = TIER_KEYS[tier];
        String categoryName = CATEGORY_NAMES[category];
        String tierName = TIER_NAMES[tier];

        if (state.awardedKeys != null) {
            for (String key : state.awardedKeys) {
                if (key == null) {
                    continue;
                }

                String lower = key.toLowerCase(Locale.US);

                boolean categoryMatched =
                        lower.contains(categoryKey)
                                || key.contains(categoryName);

                boolean tierMatched =
                        lower.contains(tierKey)
                                || key.contains(tierName);

                if (categoryMatched && tierMatched) {
                    return true;
                }

                String canonical =
                        "medal_" + categoryKey + "_" + tierKey;

                if (canonical.equals(lower)) {
                    return true;
                }
            }
        }

        /*
         * 旧项目未保存统一勋章 key 时，用历史指标兜底。
         * 使用历史最高值，不采用当前连续值，避免普通断签导致已展示勋章消失。
         */
        return metric >= THRESHOLDS[category][tier];
    }

    private void renderExperience() {
        List<ExperienceRow> rows = new ArrayList<>();
        int count = Math.min(
                EXP_PREVIEW_LIMIT,
                state.expLogs.size()
        );

        for (int i = 0; i < count; i++) {
            ExperienceLog log = state.expLogs.get(i);

            if (log != null) {
                rows.add(
                        new ExperienceRow(i, log)
                );
            }
        }

        experienceAdapter.submitList(rows);

        boolean empty = rows.isEmpty();
        emptyExperience.setVisibility(
                empty ? View.VISIBLE : View.GONE
        );
        experienceRecycler.setVisibility(
                empty ? View.GONE : View.VISIBLE
        );
    }

    private int majorRealmIndex(int xp) {
        if (xp <= 3500) return 0;
        if (xp <= 15000) return 1;
        if (xp <= 40000) return 2;
        if (xp <= 82000) return 3;
        return 4;
    }

    private int realmIcon(int index) {
        switch (index) {
            case 0:
                return R.drawable.ic_realm_stage_qi;
            case 1:
                return R.drawable.ic_realm_stage_foundation;
            case 2:
                return R.drawable.ic_realm_stage_golden;
            case 3:
                return R.drawable.ic_realm_stage_nascent;
            default:
                return R.drawable.ic_realm_stage_spirit;
        }
    }

    private String descriptionForXp(int xp) {
        if (xp <= 500) {
            return "初入自律之路，偶尔懈怠，正在养成基础打卡习惯";
        } else if (xp <= 1200) {
            return "逐步固定每日任务，偶尔断签，但已有向上之心";
        } else if (xp <= 2200) {
            return "能稳定完成多项日常自律，自控力小幅提升";
        } else if (xp <= 3500) {
            return "极少破戒，作息、阅读基本稳定，初具恒心";
        } else if (xp <= 5500) {
            return "多数日子完成多模块打卡，连续自律天数稳步上涨";
        } else if (xp <= 8000) {
            return "体重、阅读、背词长期坚持，收获第一批白银勋章";
        } else if (xp <= 11000) {
            return "极少中断自律，自控力显著提升，很少破戒";
        } else if (xp <= 15000) {
            return "作息常年达标，运动阅读逐渐形成肌肉记忆";
        } else if (xp <= 20000) {
            return "解锁多类黄金勋章，长期保持多维度连续打卡";
        } else if (xp <= 26000) {
            return "体重稳步改善、阅读量持续积累，单词储备丰厚";
        } else if (xp <= 33000) {
            return "全年极少断签，自律成为日常本能";
        } else if (xp <= 40000) {
            return "集齐大半品类勋章，意志力远超常人";
        } else if (xp <= 49000) {
            return "解锁铂金勋章，长期无破戒记录，生活高度规律";
        } else if (xp <= 59000) {
            return "阅读、运动与学习全面丰收，自律内化于心";
        } else if (xp <= 70000) {
            return "全年连续自律天数时常破百，内心自律平和";
        } else if (xp <= 82000) {
            return "仅差少量钻石勋章，自我管理近乎完美";
        } else if (xp <= 95000) {
            return "解锁首枚钻石勋章，多维度长期坚持无断层";
        } else if (xp <= 110000) {
            return "集齐绝大多数勋章，数年稳定自律，彻底摆脱拖延";
        } else if (xp <= 130000) {
            return "所有自律习惯完全刻入生活，无需刻意约束自己";
        }

        return "自律大道圆满，知行合一，自我提升达到极致";
    }

    private int medalIcon(int category, int tier) {
        int[][] icons = {
                {
                        R.drawable.ic_medal_discipline_bronze,
                        R.drawable.ic_medal_discipline_silver,
                        R.drawable.ic_medal_discipline_gold,
                        R.drawable.ic_medal_discipline_platinum,
                        R.drawable.ic_medal_discipline_diamond
                },
                {
                        R.drawable.ic_medal_weight_bronze,
                        R.drawable.ic_medal_weight_silver,
                        R.drawable.ic_medal_weight_gold,
                        R.drawable.ic_medal_weight_platinum,
                        R.drawable.ic_medal_weight_diamond
                },
                {
                        R.drawable.ic_medal_calories_bronze,
                        R.drawable.ic_medal_calories_silver,
                        R.drawable.ic_medal_calories_gold,
                        R.drawable.ic_medal_calories_platinum,
                        R.drawable.ic_medal_calories_diamond
                },
                {
                        R.drawable.ic_medal_nobreak_bronze,
                        R.drawable.ic_medal_nobreak_silver,
                        R.drawable.ic_medal_nobreak_gold,
                        R.drawable.ic_medal_nobreak_platinum,
                        R.drawable.ic_medal_nobreak_diamond
                },
                {
                        R.drawable.ic_medal_words_bronze,
                        R.drawable.ic_medal_words_silver,
                        R.drawable.ic_medal_words_gold,
                        R.drawable.ic_medal_words_platinum,
                        R.drawable.ic_medal_words_diamond
                }
        };

        return icons[category][tier];
    }

    private int experienceIcon(String source) {
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

    private double calculateTotalCalories() {
        if (state.exercises == null) {
            return 0;
        }

        double total = 0;

        for (Object exercise : state.exercises) {
            Object value = readMember(
                    exercise,
                    "calories",
                    "calorie",
                    "kcal",
                    "burnedCalories"
            );

            if (value instanceof Number) {
                total += ((Number) value).doubleValue();
            }
        }

        return total;
    }

    /**
     * 以最早体重为基准，计算历史最低体重对应的最大减重。
     */
    private double calculateMaxWeightLoss() {
        if (state.weights == null
                || state.weights.isEmpty()) {
            return 0;
        }

        List<WeightPoint> points = new ArrayList<>();

        for (Object record : state.weights) {
            String date = stringMember(
                    record,
                    "date",
                    "day",
                    "recordDate",
                    "createdDate"
            );

            Object value = readMember(
                    record,
                    "weight",
                    "value",
                    "kg",
                    "weightKg"
            );

            if (value instanceof Number) {
                points.add(
                        new WeightPoint(
                                date == null ? "" : date,
                                ((Number) value).doubleValue()
                        )
                );
            }
        }

        if (points.isEmpty()) {
            return 0;
        }

        Collections.sort(
                points,
                Comparator.comparing(point -> point.date)
        );

        double initial = points.get(0).weight;
        double minimum = initial;

        for (WeightPoint point : points) {
            minimum = Math.min(minimum, point.weight);
        }

        return Math.max(0, initial - minimum);
    }

    private int calculateLongestNoBreakStreak() {
        if (state.diaries == null
                || state.diaries.isEmpty()) {
            return 0;
        }

        Set<String> dates = new HashSet<>();

        for (DiaryRecord diary : state.diaries) {
            if (diary != null
                    && !diary.broken
                    && diary.date != null
                    && !diary.date.isEmpty()) {
                dates.add(diary.date);
            }
        }

        if (dates.isEmpty()) {
            return 0;
        }

        List<String> sorted = new ArrayList<>(dates);
        Collections.sort(sorted);

        int longest = 1;
        int current = 1;

        for (int i = 1; i < sorted.size(); i++) {
            String previous = sorted.get(i - 1);
            String now = sorted.get(i);

            if (isNextDay(previous, now)) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 1;
            }
        }

        return longest;
    }

    private boolean isNextDay(String previous, String now) {
        try {
            java.text.SimpleDateFormat format =
                    new java.text.SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.getDefault()
                    );

            java.util.Date previousDate =
                    format.parse(previous);

            if (previousDate == null) {
                return false;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(previousDate);
            calendar.add(Calendar.DAY_OF_MONTH, 1);

            return format.format(
                    calendar.getTime()
            ).equals(now);

        } catch (Exception ignored) {
            return false;
        }
    }

    private Object readMember(
            Object target,
            String... names
    ) {
        if (target == null) {
            return null;
        }

        for (String name : names) {
            Class<?> type = target.getClass();

            while (type != null) {
                try {
                    java.lang.reflect.Field field =
                            type.getDeclaredField(name);

                    field.setAccessible(true);
                    Object value = field.get(target);

                    if (value != null) {
                        return value;
                    }

                    break;
                } catch (Exception ignored) {
                    type = type.getSuperclass();
                }
            }
        }

        return null;
    }

    private String stringMember(
            Object target,
            String... names
    ) {
        Object value = readMember(target, names);
        return value == null ? null : String.valueOf(value);
    }

    private boolean containsAny(
            String source,
            String... words
    ) {
        if (source == null) {
            return false;
        }

        for (String word : words) {
            if (source.contains(word)) {
                return true;
            }
        }

        return false;
    }

    private String conditionText(MedalItem medal) {
        String unit;

        switch (medal.category) {
            case 0:
            case 3:
                unit = "天";
                break;
            case 1:
                unit = "kg";
                break;
            case 2:
                unit = "kcal";
                break;
            default:
                unit = "个";
                break;
        }

        return formatMetric(medal.threshold, medal.category)
                + " " + unit;
    }

    private String progressText(MedalItem medal) {
        if (medal.unlocked) {
            return "已解锁";
        }

        String unit;

        switch (medal.category) {
            case 0:
            case 3:
                unit = "天";
                break;
            case 1:
                unit = "kg";
                break;
            case 2:
                unit = "kcal";
                break;
            default:
                unit = "个";
                break;
        }

        return formatMetric(medal.metric, medal.category)
                + "/"
                + formatMetric(medal.threshold, medal.category)
                + unit;
    }

    private String formatMetric(
            double value,
            int category
    ) {
        if (category == 1) {
            return String.format(
                    Locale.getDefault(),
                    "%.1f",
                    value
            );
        }

        return String.format(
                Locale.getDefault(),
                "%,.0f",
                value
        );
    }

    private String formatNumber(int value) {
        return String.format(
                Locale.getDefault(),
                "%,d",
                value
        );
    }

    private void showMedalDetail(MedalItem medal) {
        String message =
                "品类：" + medal.categoryName
                        + "\n等级：" + medal.tierName
                        + "\n解锁条件："
                        + conditionText(medal)
                        + "\n当前进度："
                        + progressText(medal)
                        + "\n状态："
                        + (medal.unlocked
                        ? "已永久收藏"
                        : "尚未解锁");

        new AlertDialog.Builder(getActivity())
                .setTitle(
                        medal.categoryName
                                + " · "
                                + medal.tierName
                )
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
    }

    private void showRealmDetail(RealmStageItem item) {
        String[] descriptions = {
                "练气境：新手自律入门，逐步建立每日习惯。",
                "筑基境：稳定自律阶段，多项习惯长期坚持。",
                "金丹境：深度自我提升，自律逐渐成为本能。",
                "元婴境：自律内化于心，生活高度规律。",
                "化神境：自律大成，知行合一。"
        };

        new AlertDialog.Builder(getActivity())
                .setTitle(item.name)
                .setMessage(descriptions[item.index])
                .setPositiveButton("确定", null)
                .show();
    }

    private void normalizeState() {
        if (state.awardedKeys == null) {
            state.awardedKeys = new ArrayList<>();
        }

        if (state.expLogs == null) {
            state.expLogs = new ArrayList<>();
        }

        if (state.words == null) {
            state.words = new ArrayList<>();
        }

        if (state.diaries == null) {
            state.diaries = new ArrayList<>();
        }
    }

    private static class WeightPoint {
        final String date;
        final double weight;

        WeightPoint(String date, double weight) {
            this.date = date;
            this.weight = weight;
        }
    }

    private static class RealmStageItem {
        final String name;
        final int iconRes;
        final int index;
        final boolean unlocked;
        final boolean current;

        RealmStageItem(
                String name,
                int iconRes,
                int index,
                boolean unlocked,
                boolean current
        ) {
            this.name = name;
            this.iconRes = iconRes;
            this.index = index;
            this.unlocked = unlocked;
            this.current = current;
        }
    }

    private static class MedalItem {
        final int category;
        final int tier;
        final String categoryName;
        final String tierName;
        final int iconRes;
        final double metric;
        final double threshold;
        final boolean unlocked;

        MedalItem(
                int category,
                int tier,
                String categoryName,
                String tierName,
                int iconRes,
                double metric,
                double threshold,
                boolean unlocked
        ) {
            this.category = category;
            this.tier = tier;
            this.categoryName = categoryName;
            this.tierName = tierName;
            this.iconRes = iconRes;
            this.metric = metric;
            this.threshold = threshold;
            this.unlocked = unlocked;
        }
    }

    private static class ExperienceRow {
        final int stateIndex;
        final ExperienceLog log;

        ExperienceRow(
                int stateIndex,
                ExperienceLog log
        ) {
            this.stateIndex = stateIndex;
            this.log = log;
        }
    }

    private class RealmStageAdapter
            extends RecyclerView.Adapter<RealmStageAdapter.Holder> {

        private final List<RealmStageItem> items =
                new ArrayList<>();

        void submitList(List<RealmStageItem> newItems) {
            items.clear();

            if (newItems != null) {
                items.addAll(newItems);
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
                            R.layout.item_realm_stage,
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
            RealmStageItem item = items.get(position);

            holder.icon.setImageResource(item.iconRes);
            holder.name.setText(item.name);
            holder.lock.setVisibility(
                    item.unlocked
                            ? View.GONE
                            : View.VISIBLE
            );
            holder.indicator.setVisibility(
                    item.current
                            ? View.VISIBLE
                            : View.INVISIBLE
            );
            holder.itemView.setBackgroundResource(
                    item.current
                            ? R.drawable.bg_realm_stage_selected
                            : R.drawable.bg_realm_stage_unselected
            );

            if (item.unlocked) {
                holder.icon.clearColorFilter();
                holder.icon.setAlpha(
                        item.current ? 1.0f : 0.75f
                );
            } else {
                holder.icon.setColorFilter(
                        Color.rgb(137, 153, 163),
                        PorterDuff.Mode.SRC_IN
                );
                holder.icon.setAlpha(0.42f);
            }

            holder.itemView.setOnClickListener(v ->
                    showRealmDetail(item)
            );
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final ImageView icon;
            final ImageView lock;
            final TextView name;
            final View indicator;

            Holder(View itemView) {
                super(itemView);

                icon = itemView.findViewById(
                        R.id.ivRealmStageIcon
                );
                lock = itemView.findViewById(
                        R.id.ivRealmStageLock
                );
                name = itemView.findViewById(
                        R.id.tvRealmStageName
                );
                indicator = itemView.findViewById(
                        R.id.realmStageIndicator
                );
            }
        }
    }

    private class MedalAdapter
            extends RecyclerView.Adapter<MedalAdapter.Holder> {

        private final List<MedalItem> items =
                new ArrayList<>();

        void submitList(List<MedalItem> newItems) {
            items.clear();

            if (newItems != null) {
                items.addAll(newItems);
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
                            R.layout.item_medal_wall,
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
            MedalItem medal = items.get(position);

            holder.category.setText(
                    medal.categoryName
            );
            holder.category.setVisibility(
                    medal.tier == 0
                            ? View.VISIBLE
                            : View.INVISIBLE
            );

            holder.icon.setImageResource(medal.iconRes);
            holder.tier.setText(medal.tierName);
            holder.progress.setText(
                    progressText(medal)
            );

            if (medal.unlocked) {
                holder.icon.clearColorFilter();
                holder.icon.setAlpha(1.0f);
                holder.lock.setVisibility(View.GONE);
            } else {
                holder.icon.setColorFilter(
                        Color.rgb(127, 143, 153),
                        PorterDuff.Mode.SRC_IN
                );
                holder.icon.setAlpha(0.40f);
                holder.lock.setVisibility(View.VISIBLE);
            }

            holder.itemView.setOnClickListener(v ->
                    showMedalDetail(medal)
            );
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView category;
            final ImageView icon;
            final ImageView lock;
            final TextView tier;
            final TextView progress;

            Holder(View itemView) {
                super(itemView);

                category = itemView.findViewById(
                        R.id.tvMedalCategory
                );
                icon = itemView.findViewById(
                        R.id.ivMedalIcon
                );
                lock = itemView.findViewById(
                        R.id.ivMedalLock
                );
                tier = itemView.findViewById(
                        R.id.tvMedalTier
                );
                progress = itemView.findViewById(
                        R.id.tvMedalProgress
                );
            }
        }
    }

    private class ExperienceAdapter
            extends RecyclerView.Adapter<ExperienceAdapter.Holder> {

        private final List<ExperienceRow> items =
                new ArrayList<>();

        void submitList(List<ExperienceRow> newItems) {
            items.clear();

            if (newItems != null) {
                items.addAll(newItems);
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
                            R.layout.item_experience_detail,
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
            ExperienceRow row = items.get(position);
            ExperienceLog log = row.log;

            holder.date.setText(
                    log.date == null ? "" : log.date
            );
            holder.icon.setImageResource(
                    experienceIcon(log.source)
            );
            holder.source.setText(
                    log.source == null
                            ? "经验记录"
                            : log.source
            );
            holder.points.setText(
                    "+" + log.points
            );

            holder.itemView.setOnClickListener(v ->
                    RecordDetailActivity.open(
                            getActivity(),
                            RecordDetailActivity.TYPE_EXP,
                            row.stateIndex
                    )
            );
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView date;
            final ImageView icon;
            final TextView source;
            final TextView points;

            Holder(View itemView) {
                super(itemView);

                date = itemView.findViewById(
                        R.id.tvExperienceDate
                );
                icon = itemView.findViewById(
                        R.id.ivExperienceSourceIcon
                );
                source = itemView.findViewById(
                        R.id.tvExperienceSource
                );
                points = itemView.findViewById(
                        R.id.tvExperiencePoints
                );
            }
        }
    }
}

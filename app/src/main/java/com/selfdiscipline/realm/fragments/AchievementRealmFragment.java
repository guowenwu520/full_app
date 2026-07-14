package com.selfdiscipline.realm.fragments;

import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.TypedValue;
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
import com.selfdiscipline.realm.data.BadgeCatalog;
import com.selfdiscipline.realm.data.RealmCatalog;
import com.selfdiscipline.realm.engine.RewardEngine;
import com.selfdiscipline.realm.engine.StatsEngine;
import com.selfdiscipline.realm.model.AppState;
import com.selfdiscipline.realm.model.Badge;
import com.selfdiscipline.realm.model.DiaryRecord;
import com.selfdiscipline.realm.model.ExperienceLog;
import com.selfdiscipline.realm.model.RealmLevel;
import com.selfdiscipline.realm.model.WeightRecord;
import com.selfdiscipline.realm.ui.BadgeIconResolver;
import com.selfdiscipline.realm.ui.RealmDialog;
import com.selfdiscipline.realm.util.NumberFormatUtils;

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
 * - 卡路里勋章永久保留；其余勋章按当前指标动态锁定和收回经验。
 * - 所有勋章始终显示当前值/目标值。
 */
public class AchievementRealmFragment extends BaseFragmentHelper {

    private static final int EXP_PREVIEW_LIMIT = 6;
    private static final String[] REALM_STAGE_NAMES = {
            "练气", "筑基", "金丹", "元婴", "化神",
            "炼虚", "合体", "大乘", "真仙境", "金仙境", "太乙境", "大罗境", "道祖境"
    };

    private static final String[] REALM_STAGE_DESCRIPTIONS = {
            "练气境：新手自律入门，逐步建立每日习惯。",
            "筑基境：稳定自律阶段，多项习惯长期坚持。",
            "金丹境：深度自我提升，自律逐渐成为本能。",
            "元婴境：自律内化于心，生活高度规律。",
            "化神境：自律大成，知行合一。",
            "炼虚境：开始由外在约束走向内在沉淀，节奏更加稳定。",
            "合体境：学习、运动、作息与情绪管理逐步统一。",
            "大乘境：长期主义成熟，持续突破舒适区。",
            "真仙境：高阶稳定输出，自律体系高度协同。",
            "金仙境：长期管理能力显著提升，目标推进清晰稳定。",
            "太乙境：极强的长期稳定性与掌控力，行动质量兼具。",
            "大罗境：从更高维度统筹修炼与生活，自我系统成熟。",
            "道祖境：知行合一，自律成道，身心秩序臻于完满。"
    };

    private static final int[] REALM_STAGE_ICONS = {
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

    private static final int[] REALM_STAGE_MIN_XP = {
            0, 3501, 15001, 40001, 82001, 130001, 228001, 374001,
            568001, 810001, 1100001, 1438001, 1824001
    };


    // 五个勋章品类。
    private static final String[] CATEGORY_KEYS = {
            "discipline", "weight", "calories", "nobreak", "futures"
    };

    private static final String[] CATEGORY_NAMES = {
            "连续自律", "减脂体重", "卡路里累计", "连续不破戒", "期货累计收入"
    };

    /*
     * 旧版本持久化键继续保留，确保已有用户的解锁记录不失效。
     * 显示等级已升级为：白银、黄金、铂金、钻石、王者。
     */
    private static final String[] TIER_KEYS = {
            "bronze", "silver", "gold", "platinum", "diamond"
    };

    private static final String[] TIER_NAMES = {
            "白银", "黄金", "铂金", "钻石", "王者"
    };

    private static final String[] BADGE_TYPE_KEYS = {
            "self", "weight", "calorie", "nobreak", "futures"
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
                R.id.ivRealmBadge
        );
        realmName = root.findViewById(
                R.id.tvRealmName
        );
        totalXp = root.findViewById(
                R.id.tvTotalExp
        );
        remainingXp = root.findViewById(
                R.id.tvRemainingExp
        );
        realmPercent = root.findViewById(
                R.id.tvRealmPercent
        );
        realmDescription = root.findViewById(
                R.id.tvRealmDescription
        );
        realmProgress = root.findViewById(
                R.id.progressRealm
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
        realmDescription.setText(realm.descRes);

        if (realm.isCap()) {
            remainingXp.setText("已圆满");
            realmPercent.setText("100.00%");
            realmProgress.setProgress(100);
            return;
        }

        int remaining = Math.max(0, realm.nextXp - xp);
        double percent = realm.nextXp <= 0
                ? 100.0
                : Math.max(0.0, Math.min(100.0, xp * 100.0 / realm.nextXp));

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

        for (int i = 0; i < REALM_STAGE_NAMES.length; i++) {
            items.add(new RealmStageItem(
                    REALM_STAGE_NAMES[i],
                    realmIcon(i),
                    i,
                    i <= currentMajor,
                    i == currentMajor
            ));
        }

        realmAdapter.submitList(items);
    }

    private void renderMedals() {
        double[] metrics = {
                Math.min(30, StatsEngine.currentSelfDisciplineStreak(state)),
                StatsEngine.weightLoss(state),
                StatsEngine.totalCalories(state),
                Math.min(30, StatsEngine.currentNoBreakStreak(state)),
                Math.max(0, StatsEngine.totalFuturesIncome(state))
        };

        List<Badge> catalog = BadgeCatalog.all();
        List<MedalItem> medals = new ArrayList<>();
        long totalEarned = 0;

        // 先按等级、再按品类：第一行五枚白银，第二行五枚黄金……
        for (int tier = 0; tier < TIER_KEYS.length; tier++) {
            for (int category = 0; category < CATEGORY_KEYS.length; category++) {
                Badge badge = catalog.get(category * TIER_KEYS.length + tier);
                double metric = metrics[category];
                boolean isUnlocked = isMedalUnlocked(category, badge, metric);
                int unlockCount = RewardEngine.badgeUnlockCount(state, badge);
                totalEarned += unlockCount;

                medals.add(new MedalItem(
                        category,
                        tier,
                        CATEGORY_NAMES[category],
                        TIER_NAMES[tier],
                        medalIcon(category, tier),
                        metric,
                        badge.target,
                        isUnlocked,
                        unlockCount
                ));
            }
        }

        unlockedCount.setText("已获 " + NumberFormatUtils.compact(totalEarned) + " 枚");
        medalAdapter.submitList(medals);
    }

    private boolean isMedalUnlocked(int category, Badge badge, double metric) {
        if (badge == null) return false;

        return state.isBadgeUnlocked(badge.id);
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
        int index = 0;
        for (int i = 0; i < REALM_STAGE_MIN_XP.length; i++) {
            if (xp >= REALM_STAGE_MIN_XP[i]) {
                index = i;
            }
        }
        return index;
    }

    private int realmIcon(int index) {
        if (index < 0) {
            return REALM_STAGE_ICONS[0];
        }
        if (index >= REALM_STAGE_ICONS.length) {
            return REALM_STAGE_ICONS[REALM_STAGE_ICONS.length - 1];
        }
        return REALM_STAGE_ICONS[index];
    }

    private int medalIcon(int category, int tier) {
        return BadgeIconResolver.medalIcon(category, tier);
    }

    private int experienceIcon(String source) {
        if (containsAny(source, "阅读", "书籍", "读书")) {
            return R.drawable.ic_exp_reading;
        }
        if (containsAny(source, "期货", "盈利", "亏损")) {
            return R.drawable.ic_xp;
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
                unit = "天";
                break;
            case 3:
                unit = "天";
                break;
            case 1:
                unit = "kg";
                break;
            case 2:
                unit = "kcal";
                break;
            case 4:
                unit = "元";
                break;
            default:
                unit = "";
                break;
        }

        return formatMetric(medal.threshold, medal.category)
                + " " + unit;
    }

    private String progressText(MedalItem medal) {
        String unit;

        switch (medal.category) {
            case 0:
                unit = "天";
                break;
            case 3:
                unit = "天";
                break;
            case 1:
                unit = "kg";
                break;
            case 2:
                unit = "kcal";
                break;
            case 4:
                unit = "元";
                break;
            default:
                unit = "";
                break;
        }

        return formatProgressMetric(medal.metric, medal.category)
                + "/"
                + formatProgressMetric(medal.threshold, medal.category)
                + unit;
    }

    private String formatProgressMetric(double value, int category) {
        return category == 1
                ? NumberFormatUtils.compact(value, 1)
                : NumberFormatUtils.compact(value);
    }

    private String formatMetric(
            double value,
            int category
    ) {
        return category == 1
                ? NumberFormatUtils.compact(value, 1)
                : NumberFormatUtils.compact(value);
    }

    private String formatNumber(int value) {
        return NumberFormatUtils.compact(value);
    }

    private int unlockCountBadgeRes(int count) {
        switch (count) {
            case 1:
                return R.drawable.ic_badge_count_1;
            case 2:
                return R.drawable.ic_badge_count_2;
            case 3:
                return R.drawable.ic_badge_count_3;
            case 4:
                return R.drawable.ic_badge_count_4;
            case 5:
                return R.drawable.ic_badge_count_5;
            case 6:
                return R.drawable.ic_badge_count_6;
            case 7:
                return R.drawable.ic_badge_count_7;
            default:
                return R.drawable.ic_badge_count_up;
        }
    }

    private String unlockCountText(int count) {
        return count >= 8 ? NumberFormatUtils.compact(count) : "";
    }

    private float unlockCountTextSizeSp(int count) {
        if (count >= 10000) return 5.2f;
        if (count >= 1000) return 6.0f;
        if (count >= 100) return 6.8f;
        return 7.8f;
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
                        + (medal.unlocked ? "已解锁" : "尚未解锁")
                        + "\n累计解锁次数："
                        + medal.unlockCount
                        + " 次";

        RealmDialog.showInfo(
                getActivity(),
                medal.categoryName + " · " + medal.tierName,
                message
        );
    }

    private void showRealmDetail(RealmStageItem item) {
        String description = REALM_STAGE_DESCRIPTIONS[item.index];
        RealmDialog.showInfo(
                getActivity(),
                item.name,
                description
        );
    }

    private void normalizeState() {
        if (state.awardedKeys == null) {
            state.awardedKeys = new ArrayList<>();
        }

        if (state.expLogs == null) {
            state.expLogs = new ArrayList<>();
        }

        if (state.futuresIncomes == null) {
            state.futuresIncomes = new ArrayList<>();
        }

        if (state.diaries == null) {
            state.diaries = new ArrayList<>();
        }

        if (state.badgeUnlockCounts == null) {
            state.badgeUnlockCounts = new java.util.HashMap<>();
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
        final int unlockCount;

        MedalItem(
                int category,
                int tier,
                String categoryName,
                String tierName,
                int iconRes,
                double metric,
                double threshold,
                boolean unlocked,
                int unlockCount
        ) {
            this.category = category;
            this.tier = tier;
            this.categoryName = categoryName;
            this.tierName = tierName;
            this.iconRes = iconRes;
            this.metric = metric;
            this.threshold = threshold;
            this.unlocked = unlocked;
            this.unlockCount = unlockCount;
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
            bindUnlockBadge(holder, medal.unlockCount);

            if (medal.unlocked) {
                holder.icon.clearColorFilter();
                holder.icon.setAlpha(1.0f);
                holder.lock.setVisibility(View.GONE);
            } else {
                ColorMatrix grayscale = new ColorMatrix();
                grayscale.setSaturation(0.0f);
                holder.icon.setColorFilter(
                        new ColorMatrixColorFilter(grayscale)
                );
                holder.icon.setAlpha(0.46f);
                holder.lock.setVisibility(View.VISIBLE);
            }

            holder.itemView.setOnClickListener(v ->
                    showMedalDetail(medal)
            );
        }

        private void bindUnlockBadge(Holder holder, int unlockCount) {
            if (unlockCount <= 0) {
                holder.unlockBadgeContainer.setVisibility(View.GONE);
                holder.unlockBadgeText.setVisibility(View.GONE);
                return;
            }

            holder.unlockBadgeContainer.setVisibility(View.VISIBLE);
            holder.unlockBadgeIcon.setImageResource(
                    unlockCountBadgeRes(unlockCount)
            );

            if (unlockCount >= 8) {
                holder.unlockBadgeText.setVisibility(View.VISIBLE);
                holder.unlockBadgeText.setText(
                        unlockCountText(unlockCount)
                );
                holder.unlockBadgeText.setTextSize(
                        TypedValue.COMPLEX_UNIT_SP,
                        unlockCountTextSizeSp(unlockCount)
                );
            } else {
                holder.unlockBadgeText.setVisibility(View.GONE);
                holder.unlockBadgeText.setText("");
            }
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
            final View unlockBadgeContainer;
            final ImageView unlockBadgeIcon;
            final TextView unlockBadgeText;

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
                unlockBadgeContainer = itemView.findViewById(
                        R.id.layoutMedalUnlockBadge
                );
                unlockBadgeIcon = itemView.findViewById(
                        R.id.ivMedalUnlockBadge
                );
                unlockBadgeText = itemView.findViewById(
                        R.id.tvMedalUnlockCount
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
                    NumberFormatUtils.compactSigned(log.points)
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

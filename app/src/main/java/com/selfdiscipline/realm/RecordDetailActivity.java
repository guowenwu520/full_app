package com.selfdiscipline.realm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.selfdiscipline.realm.data.AppRepository;
import com.selfdiscipline.realm.data.BadgeCatalog;
import com.selfdiscipline.realm.engine.RecordDeleteEngine;
import com.selfdiscipline.realm.engine.RewardEngine;
import com.selfdiscipline.realm.engine.StatsEngine;
import com.selfdiscipline.realm.model.AppState;
import com.selfdiscipline.realm.model.Badge;
import com.selfdiscipline.realm.model.DiaryRecord;
import com.selfdiscipline.realm.model.ExerciseRecord;
import com.selfdiscipline.realm.model.ExperienceLog;
import com.selfdiscipline.realm.model.FuturesIncomeRecord;
import com.selfdiscipline.realm.model.SleepRecord;
import com.selfdiscipline.realm.model.WeightRecord;
import com.selfdiscipline.realm.model.WordEntry;
import com.selfdiscipline.realm.util.ExerciseFormat;
import com.selfdiscipline.realm.util.NumberFormatUtils;
import com.selfdiscipline.realm.ui.RealmDialog;

import java.util.List;
import java.util.Locale;

public class RecordDetailActivity extends Activity {
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_INDEX = "index";
    public static final String TYPE_BOOK = "book";
    public static final String TYPE_EXERCISE = "exercise";
    public static final String TYPE_WEIGHT = "weight";
    public static final String TYPE_FUTURES_INCOME = "futures_income";
    public static final String TYPE_SLEEP = "sleep";
    public static final String TYPE_WORD = "word";
    public static final String TYPE_DIARY = "diary";
    public static final String TYPE_EXP = "exp";
    public static final String TYPE_BADGE = "badge";

    private TextView pageTitle;
    private TextView pageSubtitle;
    private TextView title;
    private TextView summary;
    private ImageView icon;
    private LinearLayout contentContainer;
    private TextView deleteButton;
    private AppRepository repository;
    private AppState state;
    private String recordType;
    private int recordIndex;

    public static void open(Context c, String type, int index) {
        Intent i = new Intent(c, RecordDetailActivity.class);
        i.putExtra(EXTRA_TYPE, type);
        i.putExtra(EXTRA_INDEX, index);
        c.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(getResources().getColor(R.color.color_surface));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(R.layout.activity_record_detail);

        bindViews();

        recordType = getIntent().getStringExtra(EXTRA_TYPE);
        recordIndex = getIntent().getIntExtra(EXTRA_INDEX, -1);
        repository = new AppRepository(this);
        state = repository.load();
        if (state == null) state = new AppState();

        setupDeleteAction();
        render(state, recordType, recordIndex);
    }

    private void bindViews() {
        pageTitle = findViewById(R.id.text_detail_page_title);
        pageSubtitle = findViewById(R.id.text_detail_page_subtitle);
        title = findViewById(R.id.text_detail_title);
        summary = findViewById(R.id.text_detail_summary);
        icon = findViewById(R.id.image_detail_icon);
        contentContainer = findViewById(R.id.detail_content_container);
        deleteButton = findViewById(R.id.button_delete_record);
    }

    private void setupDeleteAction() {
        boolean deletable = !TYPE_BADGE.equals(recordType) && !TYPE_BOOK.equals(recordType);
        deleteButton.setVisibility(deletable ? View.VISIBLE : View.GONE);
        if (!deletable) return;
        deleteButton.setOnClickListener(v -> RealmDialog.showConfirm(
                this,
                R.string.dialog_delete_record_title,
                getString(R.string.dialog_delete_record_message, titleFor(recordType)),
                R.string.dialog_delete,
                R.string.dialog_cancel,
                this::deleteCurrentRecord
        ));
    }

    private void deleteCurrentRecord() {
        if (RecordDeleteEngine.delete(state, recordType, recordIndex)) {
            repository.save(state);
            android.widget.Toast.makeText(this, R.string.toast_deleted, android.widget.Toast.LENGTH_SHORT).show();
            finish();
        } else {
            android.widget.Toast.makeText(this, R.string.toast_delete_failed, android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void render(AppState state, String type, int index) {
        pageTitle.setText(titleFor(type));
        icon.setImageResource(iconFor(type, index));
        contentContainer.removeAllViews();

        try {
            if (TYPE_EXERCISE.equals(type) && valid(index, state.exercises)) {
                renderExercise(state.exercises.get(index));
                return;
            }
            if (TYPE_WEIGHT.equals(type) && valid(index, state.weights)) {
                renderWeight(state.weights.get(index));
                return;
            }
            if (TYPE_FUTURES_INCOME.equals(type) && valid(index, state.futuresIncomes)) {
                renderFuturesIncome(state.futuresIncomes.get(index));
                return;
            }
            if (TYPE_SLEEP.equals(type) && valid(index, state.sleeps)) {
                renderSleep(state.sleeps.get(index));
                return;
            }
            if (TYPE_WORD.equals(type) && valid(index, state.words)) {
                renderWord(state.words.get(index));
                return;
            }
            if (TYPE_DIARY.equals(type) && valid(index, state.diaries)) {
                renderDiary(state.diaries.get(index));
                return;
            }
            if (TYPE_EXP.equals(type) && valid(index, state.expLogs)) {
                renderExp(state.expLogs.get(index));
                return;
            }
            if (TYPE_BADGE.equals(type)) {
                Badge badge = safeBadge(index);
                if (badge != null) {
                    renderBadge(state, badge);
                    return;
                }
            }
        } catch (Exception ignored) {
        }

        title.setText(R.string.title_record_detail);
        summary.setText(R.string.text_no_detail);
        addSection(getString(R.string.title_record_detail), getString(R.string.text_no_detail));
    }

    private void renderExercise(ExerciseRecord record) {
        String name = ExerciseFormat.name(record.content);
        int duration = ExerciseFormat.durationMinutes(record.content);
        double distance = ExerciseFormat.distanceKm(record.content);
        title.setText(name);
        summary.setText(empty(record.date) + " · " + NumberFormatUtils.compact(record.calories) + " kcal");
        pageSubtitle.setText("本次运动消耗与记录详情");

        addMetricGrid(
                new String[]{"运动名称", "消耗热量"},
                new String[]{name, NumberFormatUtils.compact(record.calories) + " kcal"}
        );
        if (duration > 0 || distance > 0) {
            addMetricGrid(
                    new String[]{"运动时长", "运动距离"},
                    new String[]{duration > 0 ? NumberFormatUtils.compact(duration) + " 分钟" : "--", distance > 0 ? NumberFormatUtils.compact(distance, 2) + " 公里" : "--"}
            );
        }
        addSection("记录日期", empty(record.date));
    }

    private void renderWeight(WeightRecord record) {
        title.setText(String.format(Locale.getDefault(), "%.1f kg", record.weight));
        summary.setText("体重记录 · " + empty(record.date));
        pageSubtitle.setText("体重变化记录详情");

        addMetricGrid(
                new String[]{"体重", "记录日期"},
                new String[]{String.format(Locale.getDefault(), "%.1f kg", record.weight), empty(record.date)}
        );
        addSection("趋势说明", "体重曲线在运动作息页面集中查看，此处展示单次体重记录。 ");
    }


    private void renderFuturesIncome(FuturesIncomeRecord record) {
        String signed = NumberFormatUtils.compactSigned(record.amount);
        String status = record.amount > 0 ? "盈利" : "亏损";
        title.setText(signed + " 元");
        summary.setText("期货" + status + " · " + empty(record.dateTime));
        pageSubtitle.setText("期货收入与经验变化详情");

        addMetricGrid(
                new String[]{"本次收入", "经验变化", "记录时间"},
                new String[]{signed + " 元", signed + " 经验", empty(record.dateTime)}
        );
        addSection(
                "心得体会",
                record.reflection == null || record.reflection.trim().isEmpty()
                        ? "暂无心得体会"
                        : record.reflection.trim()
        );
        addSection("计算规则", "期货收入每 1 元对应 1 点经验；盈利增加经验，亏损扣除经验。");
    }

    private void renderSleep(SleepRecord record) {
        String status = record.passed ? getString(R.string.text_sleep_pass) : getString(R.string.text_sleep_fail);
        title.setText(status);
        summary.setText(empty(record.date));
        pageSubtitle.setText("作息打卡判定详情");

        addMetricGrid(
                new String[]{"入睡时间", "起床时间", "判定结果"},
                new String[]{empty(record.sleepTime), empty(record.wakeTime), status}
        );
        addSection("达标标准", "23:30 前入睡，并且 8:30 前起床。 ");
    }

    private void renderWord(WordEntry record) {
        title.setText(empty(record.word));
        summary.setText(empty(record.meaning));
        pageSubtitle.setText("单词记录详情");

        addMetricGrid(
                new String[]{"单词", "录入日期"},
                new String[]{empty(record.word), empty(record.createdDate)}
        );
        addSection("释义", empty(record.meaning));
        addSection("学习说明", "当前版本已简化为只新增单词，不再保留抽查与背词日历入口。 ");
    }

    private void renderDiary(DiaryRecord record) {
        String diaryTitle = empty(record.title).equals("-") ? "日记" : record.title;
        String status = record.broken ? getString(R.string.text_broken) : getString(R.string.text_not_broken);
        title.setText(diaryTitle);
        summary.setText(empty(record.date) + " · " + status);
        pageSubtitle.setText("日记与自律记录详情");

        addMetricGrid(
                new String[]{"日期", "自律状态"},
                new String[]{empty(record.date), status}
        );
        addSection("日记标题", diaryTitle);
        addSection("正文", empty(record.body));
    }

    private void renderExp(ExperienceLog record) {
        String signed = NumberFormatUtils.compactSigned(record.points);
        title.setText(signed + " 经验");
        summary.setText(empty(record.source));
        pageSubtitle.setText("经验来源详情");

        addMetricGrid(
                new String[]{"获得经验", "日期"},
                new String[]{signed, empty(record.date)}
        );
        addSection("来源", empty(record.source));
        addSection("唯一键", empty(record.key));
    }

    private void renderBadge(AppState state, Badge badge) {
        boolean unlocked = state.isBadgeUnlocked(badge.id);
        double metric = StatsEngine.badgeMetric(state, badge);
        String unit = badgeUnit(badge);

        title.setText(badge.nameRes);
        summary.setText(getString(badge.rankRes) + " · "
                + (unlocked ? getString(R.string.text_unlocked) : getString(R.string.text_locked)));
        pageSubtitle.setText("勋章当前进度与状态详情");

        addMetricGrid(
                new String[]{"品级", "当前进度", "目标", "累计解锁"},
                new String[]{
                        getString(badge.rankRes),
                        formatBadgeMetric(metric, badge) + unit,
                        formatBadgeMetric(badge.target, badge) + unit,
                        NumberFormatUtils.compact(RewardEngine.badgeUnlockCount(state, badge)) + " 次"
                }
        );
        addSection("勋章状态", unlocked ? getString(R.string.text_unlocked) : getString(R.string.text_locked));
        addSection("奖励经验", NumberFormatUtils.compactSigned(badge.xpReward));
        addSection("说明", getString(badge.descRes));
    }

    private String badgeUnit(Badge badge) {
        if (Badge.TYPE_SELF.equals(badge.type) || Badge.TYPE_NOBREAK.equals(badge.type)) return "天";
        if (Badge.TYPE_WEIGHT.equals(badge.type)) return "kg";
        if (Badge.TYPE_CALORIE.equals(badge.type)) return "kcal";
        if (Badge.TYPE_FUTURES.equals(badge.type)) return "元";
        return "";
    }

    private String formatBadgeMetric(double value, Badge badge) {
        if (Badge.TYPE_WEIGHT.equals(badge.type)) {
            return NumberFormatUtils.compact(value, 1);
        }
        return NumberFormatUtils.compact(value);
    }

    private void addMetricGrid(String[] labels, String[] values) {
        LinearLayout card = newCard();
        card.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < labels.length; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, i == 0 ? 0 : dp(10), 0, 0);

            TextView label = labelText(labels[i]);
            TextView value = valueText(values[i]);
            value.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);

            row.addView(label, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(value, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f));
            card.addView(row);
        }

        contentContainer.addView(card);
    }

    private void addSection(String heading, String body) {
        LinearLayout card = newCard();
        card.setOrientation(LinearLayout.VERTICAL);

        TextView h = new TextView(this);
        h.setText(heading);
        h.setTextColor(getResources().getColor(R.color.checkin_text_primary));
        h.setTextSize(15);
        h.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView b = new TextView(this);
        b.setText(empty(body));
        b.setTextColor(getResources().getColor(R.color.color_text_main));
        b.setTextSize(15);
        b.setLineSpacing(dp(4), 1f);
        b.setPadding(0, dp(8), 0, 0);

        card.addView(h);
        card.addView(b);
        contentContainer.addView(card);
    }

    private LinearLayout newCard() {
        LinearLayout card = new LinearLayout(this);
        card.setBackgroundResource(R.drawable.bg_realm_status_card);
        card.setClipToOutline(true);
        card.setElevation(dp(3));
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lp);
        return card;
    }

    private TextView labelText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(getResources().getColor(R.color.color_text_sub));
        view.setTextSize(13);
        return view;
    }

    private TextView valueText(String text) {
        TextView view = new TextView(this);
        view.setText(empty(text));
        view.setTextColor(getResources().getColor(R.color.color_text_main));
        view.setTextSize(16);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        return view;
    }

    private String titleFor(String type) {
        if (TYPE_EXERCISE.equals(type)) return getString(R.string.label_record_type_exercises);
        if (TYPE_WEIGHT.equals(type)) return getString(R.string.label_record_type_weights);
        if (TYPE_FUTURES_INCOME.equals(type)) return getString(R.string.label_record_type_futures_income);
        if (TYPE_SLEEP.equals(type)) return getString(R.string.label_record_type_sleeps);
        if (TYPE_WORD.equals(type)) return getString(R.string.label_record_type_words);
        if (TYPE_DIARY.equals(type)) return getString(R.string.label_record_type_diaries);
        if (TYPE_EXP.equals(type)) return getString(R.string.label_record_type_exp_logs);
        if (TYPE_BADGE.equals(type)) return getString(R.string.title_badge_detail);
        return getString(R.string.title_record_detail);
    }

    private int iconFor(String type, int index) {
        if (TYPE_EXERCISE.equals(type)) return R.drawable.ic_nav_sport;
        if (TYPE_WEIGHT.equals(type)) return R.drawable.ic_ew_weight;
        if (TYPE_FUTURES_INCOME.equals(type)) return R.drawable.ic_xp;
        if (TYPE_SLEEP.equals(type)) return R.drawable.ic_exp_sleep;
        if (TYPE_WORD.equals(type)) return R.drawable.ic_nav_word;
        if (TYPE_DIARY.equals(type)) return R.drawable.ic_nav_diary;
        if (TYPE_EXP.equals(type)) return R.drawable.ic_xp;
        if (TYPE_BADGE.equals(type)) {
            Badge badge = safeBadge(index);
            if (badge != null) return badgeIcon(badge.rankRes);
        }
        return R.drawable.ic_nav_overview;
    }

    private Badge safeBadge(int index) {
        List<Badge> all = BadgeCatalog.all();
        if (index >= 0 && index < all.size()) return all.get(index);
        return null;
    }

    private int badgeIcon(int rankRes) {
        if (rankRes == R.string.rank_gold) return R.drawable.medal_tier_gold;
        if (rankRes == R.string.rank_platinum) return R.drawable.medal_tier_platinum;
        if (rankRes == R.string.rank_diamond) return R.drawable.medal_tier_diamond;
        if (rankRes == R.string.rank_king) return R.drawable.medal_tier_king;
        return R.drawable.medal_tier_silver;
    }

    private boolean valid(int index, List<?> list) {
        return list != null && index >= 0 && index < list.size();
    }

    private String empty(String s) {
        return s == null || s.trim().isEmpty() ? "-" : s.trim();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

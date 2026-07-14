package com.selfdiscipline.realm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.selfdiscipline.realm.data.AppRepository;
import com.selfdiscipline.realm.data.RealmCatalog;
import com.selfdiscipline.realm.engine.RecordDeleteEngine;
import com.selfdiscipline.realm.engine.StatsEngine;
import com.selfdiscipline.realm.model.AppState;
import com.selfdiscipline.realm.model.Book;
import com.selfdiscipline.realm.model.DiaryRecord;
import com.selfdiscipline.realm.model.ExerciseRecord;
import com.selfdiscipline.realm.model.ExperienceLog;
import com.selfdiscipline.realm.model.FuturesIncomeRecord;
import com.selfdiscipline.realm.model.RealmLevel;
import com.selfdiscipline.realm.model.SleepRecord;
import com.selfdiscipline.realm.model.WeightRecord;
import com.selfdiscipline.realm.model.WordEntry;
import com.selfdiscipline.realm.ui.RealmDialog;
import com.selfdiscipline.realm.util.ExerciseFormat;
import com.selfdiscipline.realm.util.NumberFormatUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class RecordListActivity extends Activity {

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
    private AppRepository repository;
    private AppState state;
    private String currentType;
    private RecyclerView recordRecycler;
    private TextView emptyView;
    private RecordEntryAdapter adapter;

    private ImageView realmBadge;
    private TextView realmName;
    private TextView realmRemainingExp;
    private TextView realmTotalExp;
    private TextView realmPercent;
    private TextView realmDescription;
    private ProgressBar realmProgress;

    public static final String EXTRA_TYPE = "list_type";
    public static final String TYPE_BOOKS = "books";
    public static final String TYPE_STUDY = "study";
    public static final String TYPE_EXERCISES = "exercises";
    public static final String TYPE_WORDS = "words";
    public static final String TYPE_DIARIES = "diaries";
    public static final String TYPE_EXP_LOGS = "exp_logs";
    public static final String TYPE_OVERVIEW = "overview";

    public static void open(Context c, String type) {
        Intent i = new Intent(c, RecordListActivity.class);
        i.putExtra(EXTRA_TYPE, type);
        c.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(getResources().getColor(R.color.color_surface));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(R.layout.activity_record_list);

        repository = new AppRepository(this);
        state = repository.load();
        if (state == null) state = new AppState();
        currentType = getIntent().getStringExtra(EXTRA_TYPE);
        recordRecycler = findViewById(R.id.record_recycler);
        emptyView = findViewById(R.id.text_empty_records);
        adapter = new RecordEntryAdapter();
        adapter.setListener(new RecordEntryAdapter.Listener() {
            @Override
            public void onClick(Entry entry) {
                if (RecordDetailActivity.TYPE_BOOK.equals(entry.detailType)) {
                    BookDetailActivity.open(RecordListActivity.this, entry.detailIndex);
                    return;
                }
                RecordDetailActivity.open(RecordListActivity.this, entry.detailType, entry.detailIndex);
            }

            @Override
            public void onLongClick(Entry entry) {
                confirmDelete(entry);
            }
        });
        recordRecycler.setLayoutManager(new LinearLayoutManager(this));
        recordRecycler.setAdapter(adapter);

        bindRealmCard();

        TextView title = findViewById(R.id.text_list_title);
        title.setText(titleFor(currentType));

        renderRealmCard();
        renderList(state, currentType);
    }

    private void bindRealmCard() {
        realmBadge = findViewById(R.id.ivRealmBadge);
        realmName = findViewById(R.id.tvRealmName);
        realmRemainingExp = findViewById(R.id.tvRemainingExp);
        realmTotalExp = findViewById(R.id.tvTotalExp);
        realmPercent = findViewById(R.id.tvRealmPercent);
        realmDescription = findViewById(R.id.tvRealmDescription);
        realmProgress = findViewById(R.id.progressRealm);
    }

    private void renderRealmCard() {
        if (state == null) {
            return;
        }

        int xp = StatsEngine.totalXp(state);
        RealmLevel realm = RealmCatalog.current(xp);

        realmName.setText(realm.nameRes);
        realmTotalExp.setText(formatInteger(xp));
        realmDescription.setText(realm.descRes);
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
        realmPercent.setText(String.format(Locale.getDefault(), "%.2f%%", percent));
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

    private String formatInteger(int value) {
        return NumberFormatUtils.compact(value);
    }

    private String titleFor(String type) {
        if (TYPE_BOOKS.equals(type)) return getString(R.string.button_view_all_books);
        if (TYPE_STUDY.equals(type)) return getString(R.string.button_view_all_sport_sleep);
        if (TYPE_EXERCISES.equals(type)) return getString(R.string.label_record_type_exercises);
        if (TYPE_WORDS.equals(type)) return getString(R.string.button_view_all_words);
        if (TYPE_DIARIES.equals(type)) return getString(R.string.button_view_all_diaries);
        if (TYPE_EXP_LOGS.equals(type)) return getString(R.string.button_view_all_exp_logs);
        return getString(R.string.button_view_all_history);
    }

    private void renderList(AppState state, String type) {
        List<Entry> entries = new ArrayList<>();
        if (TYPE_BOOKS.equals(type)) addBooks(state, entries);
        else if (TYPE_STUDY.equals(type)) addStudy(state, entries);
        else if (TYPE_EXERCISES.equals(type)) addExercises(state, entries);
        else if (TYPE_WORDS.equals(type)) addWords(state, entries);
        else if (TYPE_DIARIES.equals(type)) addDiaries(state, entries);
        else if (TYPE_EXP_LOGS.equals(type)) addExpLogs(state, entries);
        else addOverview(state, entries);

        Collections.sort(entries, (a, b) -> b.sortKey.compareTo(a.sortKey));

        boolean empty = entries.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        recordRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        adapter.submit(entries);
    }

    private void confirmDelete(Entry entry) {
        RealmDialog.showConfirm(
                this,
                R.string.dialog_delete_record_title,
                getString(R.string.dialog_delete_record_message, deleteName(entry.detailType)),
                R.string.dialog_delete,
                R.string.dialog_cancel,
                () -> deleteEntry(entry)
        );
    }

    private String deleteName(String type) {
        if (RecordDetailActivity.TYPE_BOOK.equals(type)) return getString(R.string.label_record_type_books);
        if (RecordDetailActivity.TYPE_EXERCISE.equals(type)) return getString(R.string.label_record_type_exercises);
        if (RecordDetailActivity.TYPE_WEIGHT.equals(type)) return getString(R.string.label_record_type_weights);
        if (RecordDetailActivity.TYPE_SLEEP.equals(type)) return getString(R.string.label_record_type_sleeps);
        if (RecordDetailActivity.TYPE_WORD.equals(type)) return getString(R.string.label_record_type_words);
        if (RecordDetailActivity.TYPE_DIARY.equals(type)) return getString(R.string.label_record_type_diaries);
        if (RecordDetailActivity.TYPE_EXP.equals(type)) return getString(R.string.label_record_type_exp_logs);
        return getString(R.string.title_record_detail);
    }

    private void deleteEntry(Entry entry) {
        try {
            if (RecordDeleteEngine.delete(state, entry.detailType, entry.detailIndex)) {
                repository.save(state);
                Toast.makeText(this, R.string.toast_deleted, Toast.LENGTH_SHORT).show();
                renderRealmCard();
                renderList(state, currentType);
            } else {
                Toast.makeText(this, R.string.toast_delete_failed, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ex) {
            Toast.makeText(this, R.string.toast_delete_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void addBooks(AppState s, List<Entry> out) {
        for (int i = 0; i < s.books.size(); i++) {
            Book r = s.books.get(i);
            String title = safe(r.title).isEmpty() ? getString(R.string.label_record_type_books) : r.title;
            String author = safe(r.author);
            String review = safe(r.fullReview).isEmpty() ? getString(R.string.text_no) : getString(R.string.text_yes);
            String content = "《" + title + "》" + (author.isEmpty() ? "" : " · " + author)
                    + "｜当前 " + NumberFormatUtils.compact(r.currentPage) + " 页｜读后感 " + review;
            out.add(new Entry("9999-99-99-book-" + (100000 - i), R.drawable.ic_nav_reading, "阅读：", content, "书籍", RecordDetailActivity.TYPE_BOOK, i));
        }
    }

    private void addExercises(AppState s, List<Entry> out) {
        for (int i = 0; i < s.exercises.size(); i++) {
            ExerciseRecord r = s.exercises.get(i);
            String name = ExerciseFormat.name(r.content);
            String metric = ExerciseFormat.metricText(r.content);
            String calories = NumberFormatUtils.compact(r.calories) + " 千卡";
            String content = metric.equals("--")
                    ? name + "｜" + calories
                    : name + "｜" + metric + "｜" + calories;
            out.add(new Entry(r.date + "-exercise-" + i, R.drawable.ic_nav_sport, "运动：", content, safeDate(r.date), RecordDetailActivity.TYPE_EXERCISE, i));
        }
    }

    private void addStudy(AppState s, List<Entry> out) {
        addExercises(s, out);

        for (int i = 0; i < s.weights.size(); i++) {
            WeightRecord r = s.weights.get(i);
            String content = String.format(Locale.getDefault(), "体重 %.1f kg", r.weight);
            out.add(new Entry(r.date + "-weight-" + i, R.drawable.ic_ew_weight, "体重：", content, safeDate(r.date), RecordDetailActivity.TYPE_WEIGHT, i));
        }
        for (int i = 0; i < s.futuresIncomes.size(); i++) {
            FuturesIncomeRecord r = s.futuresIncomes.get(i);
            String signed = NumberFormatUtils.compactSigned(r.amount);
            String content = "期货收入 " + signed + " 元｜经验 " + signed;
            out.add(new Entry(
                    safeDate(r.dateTime) + "-futures-" + i,
                    R.drawable.ic_xp,
                    "期货：",
                    content,
                    safeDate(r.dateTime),
                    RecordDetailActivity.TYPE_FUTURES_INCOME,
                    i
            ));
        }
        for (int i = 0; i < s.sleeps.size(); i++) {
            SleepRecord r = s.sleeps.get(i);
            String status = r.passed ? getString(R.string.text_sleep_pass) : getString(R.string.text_sleep_fail);
            String content = String.format(Locale.getDefault(), "%s - %s｜%s", safe(r.sleepTime), safe(r.wakeTime), status);
            out.add(new Entry(r.date + "-sleep-" + i, R.drawable.ic_exp_sleep, "作息：", content, safeDate(r.date), RecordDetailActivity.TYPE_SLEEP, i));
        }
    }

    private void addWords(AppState s, List<Entry> out) {
        for (int i = 0; i < s.words.size(); i++) {
            WordEntry r = s.words.get(i);
            String content = String.format(Locale.getDefault(), "%s：%s", safe(r.word), safe(r.meaning));
            out.add(new Entry(safeDate(r.createdDate) + "-word-" + i, R.drawable.ic_nav_word, "单词：", content, safeDate(r.createdDate), RecordDetailActivity.TYPE_WORD, i));
        }
    }

    private void addDiaries(AppState s, List<Entry> out) {
        for (int i = 0; i < s.diaries.size(); i++) {
            DiaryRecord r = s.diaries.get(i);
            String title = safe(r.title).isEmpty() ? getString(R.string.label_record_type_diaries) : r.title;
            String status = r.broken ? getString(R.string.text_broken) : getString(R.string.text_not_broken);
            out.add(new Entry(r.date + "-diary-" + i, R.drawable.ic_nav_diary, "日记：", title + "｜" + status, safeDate(r.date), RecordDetailActivity.TYPE_DIARY, i));
        }
    }

    private void addExpLogs(AppState s, List<Entry> out) {
        for (int i = 0; i < s.expLogs.size(); i++) {
            ExperienceLog r = s.expLogs.get(i);
            String signed = NumberFormatUtils.compactSigned(r.points);
            String content = String.format(Locale.getDefault(), "%s 经验｜%s", signed, safe(r.source));
            out.add(new Entry(r.date + "-exp-" + i, R.drawable.ic_xp, "经验：", content, safeDate(r.date), RecordDetailActivity.TYPE_EXP, i));
        }
    }

    private void addOverview(AppState s, List<Entry> out) {
        addBooks(s, out);
        addStudy(s, out);
        addDiaries(s, out);
        addExpLogs(s, out);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeDate(String value) {
        String date = safe(value);
        return date.isEmpty() ? "-" : date;
    }


    private static class RecordEntryAdapter extends RecyclerView.Adapter<RecordEntryAdapter.Holder> {
        interface Listener {
            void onClick(Entry entry);

            void onLongClick(Entry entry);
        }

        private final List<Entry> items = new ArrayList<>();
        private Listener listener;

        void setListener(Listener listener) {
            this.listener = listener;
        }

        void submit(List<Entry> newItems) {
            items.clear();
            if (newItems != null) {
                items.addAll(newItems);
            }
            notifyDataSetChanged();
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_recent_activity, parent, false);
            return new Holder(row);
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            Entry entry = items.get(position);
            holder.icon.setImageResource(entry.iconRes);
            holder.itemType.setText(entry.typeText);
            holder.content.setText(entry.contentText);
            holder.time.setText(entry.timeText);
            holder.divider.setVisibility(position == items.size() - 1 ? View.INVISIBLE : View.VISIBLE);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(entry);
                }
            });
            holder.itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onLongClick(entry);
                }
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView itemType;
            final TextView content;
            final TextView time;
            final View divider;

            Holder(View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.ivActivityIcon);
                itemType = itemView.findViewById(R.id.tvActivityType);
                content = itemView.findViewById(R.id.tvActivityContent);
                time = itemView.findViewById(R.id.tvActivityTime);
                divider = itemView.findViewById(R.id.activityDivider);
            }
        }
    }

    private static class Entry {
        final String sortKey;
        final int iconRes;
        final String typeText;
        final String contentText;
        final String timeText;
        final String detailType;
        final int detailIndex;

        Entry(String sortKey, int iconRes, String typeText, String contentText, String timeText, String detailType, int detailIndex) {
            this.sortKey = sortKey;
            this.iconRes = iconRes;
            this.typeText = typeText;
            this.contentText = contentText;
            this.timeText = timeText;
            this.detailType = detailType;
            this.detailIndex = detailIndex;
        }
    }
}

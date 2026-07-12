package com.selfdiscipline.realm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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

import com.selfdiscipline.realm.data.AppRepository;
import com.selfdiscipline.realm.engine.RewardEngine;
import com.selfdiscipline.realm.model.AppState;
import com.selfdiscipline.realm.model.Book;
import com.selfdiscipline.realm.model.PageNote;
import com.selfdiscipline.realm.model.ReadingHistory;
import com.selfdiscipline.realm.ui.RealmDialog;
import com.selfdiscipline.realm.util.DateUtils;
import com.selfdiscipline.realm.util.ViewUtils;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class BookDetailActivity extends Activity {

    private static final String EXTRA_BOOK_INDEX = "book_index";
    private static final int PREVIEW_LIMIT = 3;

    private AppRepository repo;
    private AppState state;
    private Book book;
    private int bookIndex = -1;

    private ImageView cover;
    private TextView title;
    private TextView author;
    private TextView genre;
    private View statCurrentPage;
    private View statTotalPages;
    private View statReadingPercent;
    private ProgressBar progress;
    private TextView percentBelow;
    private TextView lastUpdated;
    private TextView reviewHeading;
    private TextView reviewPreview;
    private TextView writeReviewButton;
    private TextView emptyNotes;
    private TextView emptyHistory;
    private TextView toggleNotes;
    private TextView toggleHistory;

    private RecyclerView notesRecycler;
    private RecyclerView historyRecycler;
    private NoteAdapter noteAdapter;
    private HistoryAdapter historyAdapter;

    private boolean showAllNotes;
    private boolean showAllHistory;

    public static void open(Context context, int bookIndex) {
        Intent intent = new Intent(context, BookDetailActivity.class);
        intent.putExtra(EXTRA_BOOK_INDEX, bookIndex);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);

        repo = new AppRepository(this);
        state = repo.load();
        bookIndex = getIntent().getIntExtra(EXTRA_BOOK_INDEX, -1);

        if (!loadBook()) {
            Toast.makeText(this, "书籍不存在或已被删除", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        setupLists();
        setupClicks();
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (repo != null && bookIndex >= 0) {
            state = repo.load();

            if (loadBook()) {
                render();
            }
        }
    }

    private boolean loadBook() {
        if (state == null
                || state.books == null
                || bookIndex < 0
                || bookIndex >= state.books.size()) {
            return false;
        }

        book = state.books.get(bookIndex);

        if (book == null) {
            return false;
        }

        normalizeBook(book);
        return true;
    }

    private void bindViews() {
        cover = findViewById(R.id.ivDetailBookCover);
        title = findViewById(R.id.tvDetailBookTitle);
        author = findViewById(R.id.tvDetailBookAuthor);
        genre = findViewById(R.id.tvDetailBookGenre);

        statCurrentPage = findViewById(R.id.statCurrentPage);
        statTotalPages = findViewById(R.id.statTotalPages);
        statReadingPercent = findViewById(R.id.statReadingPercent);

        progress = findViewById(R.id.progressBookDetail);
        percentBelow = findViewById(R.id.tvBookDetailPercentBelow);
        lastUpdated = findViewById(R.id.tvBookLastUpdated);

        reviewHeading = findViewById(R.id.tvBookReviewHeading);
        reviewPreview = findViewById(R.id.tvBookReviewPreview);
        writeReviewButton = findViewById(R.id.buttonWriteBookReview);

        emptyNotes = findViewById(R.id.tvEmptyPageNotes);
        emptyHistory = findViewById(R.id.tvEmptyReadingHistory);
        toggleNotes = findViewById(R.id.buttonToggleAllNotes);
        toggleHistory = findViewById(R.id.buttonToggleAllHistory);

        notesRecycler = findViewById(R.id.recyclerBookPageNotes);
        historyRecycler = findViewById(R.id.recyclerBookReadingHistory);
    }

    private void setupLists() {
        noteAdapter = new NoteAdapter();
        historyAdapter = new HistoryAdapter();

        notesRecycler.setLayoutManager(new LinearLayoutManager(this));
        notesRecycler.setAdapter(noteAdapter);
        notesRecycler.setNestedScrollingEnabled(false);

        historyRecycler.setLayoutManager(new LinearLayoutManager(this));
        historyRecycler.setAdapter(historyAdapter);
        historyRecycler.setNestedScrollingEnabled(false);
    }

    private void setupClicks() {
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
        findViewById(R.id.buttonShareBook).setOnClickListener(v -> shareBook());

        findViewById(R.id.buttonUpdateBookPage)
                .setOnClickListener(v -> dialogUpdatePage());

        View pageNoteButton = findViewById(R.id.buttonNewPageNote);
        if (pageNoteButton != null) pageNoteButton.setVisibility(View.GONE);

        writeReviewButton.setOnClickListener(v -> dialogFullReview());

        findViewById(R.id.buttonViewFullReview)
                .setOnClickListener(v -> showFullReview());

        toggleNotes.setOnClickListener(v -> {
            showAllNotes = !showAllNotes;
            renderNotes();
        });

        toggleHistory.setOnClickListener(v -> {
            showAllHistory = !showAllHistory;
            renderHistory();
        });

        title.setOnClickListener(v -> dialogEditBookInfo());
        author.setOnClickListener(v -> dialogEditBookInfo());
        genre.setOnClickListener(v -> dialogEditBookInfo());
        statTotalPages.setOnClickListener(v -> dialogEditBookInfo());
    }

    private void render() {
        normalizeBook(book);

        loadCover(cover, book.coverUri);
        title.setText(safe(book.title));
        author.setText(safe(book.author));
        genre.setText(book.genre.isEmpty() ? "未分类" : book.genre);

        int current = Math.max(0, book.currentPage);
        int total = Math.max(0, book.totalPages);
        double percent = total <= 0
                ? 0.0
                : Math.min(100.0, current * 100.0 / total);

        bindStat(statCurrentPage, "当前页码", String.valueOf(current));
        bindStat(statTotalPages, "全书页数", total > 0 ? String.valueOf(total) : "--");
        bindStat(
                statReadingPercent,
                "阅读进度",
                total > 0
                        ? String.format(Locale.getDefault(), "%.1f%%", percent)
                        : "--"
        );

        progress.setProgress((int) Math.round(percent));
        percentBelow.setText(
                total > 0
                        ? String.format(Locale.getDefault(), "%.1f%%", percent)
                        : "--"
        );

        lastUpdated.setText("最后更新：" + getLastUpdatedText());

        reviewHeading.setText(
                "第一次读《" + safe(book.title) + "》的感受"
        );
        writeReviewButton.setEnabled(true);
        writeReviewButton.setAlpha(1.0f);

        reviewPreview.setText(
                book.fullReview.isEmpty()
                        ? "暂无全书读后感，点击“写读后感”开始记录。"
                        : book.fullReview
        );

        renderNotes();
        renderHistory();
    }

    private void bindStat(View root, String label, String value) {
        TextView labelView = root.findViewById(R.id.tvBookDetailStatLabel);
        TextView valueView = root.findViewById(R.id.tvBookDetailStatValue);
        labelView.setText(label);
        valueView.setText(value);
    }

    private void renderNotes() {
        if (emptyNotes != null) emptyNotes.setVisibility(View.GONE);
        if (notesRecycler != null) notesRecycler.setVisibility(View.GONE);
        if (toggleNotes != null) toggleNotes.setVisibility(View.GONE);
    }

    private void renderHistory() {
        List<ReadingHistory> source = book.readingHistory;
        int count = showAllHistory
                ? source.size()
                : Math.min(PREVIEW_LIMIT, source.size());

        historyAdapter.submitList(new ArrayList<>(source.subList(0, count)));

        boolean empty = source.isEmpty();
        emptyHistory.setVisibility(empty ? View.VISIBLE : View.GONE);
        historyRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);

        toggleHistory.setVisibility(
                source.size() > PREVIEW_LIMIT ? View.VISIBLE : View.INVISIBLE
        );
        toggleHistory.setText(
                showAllHistory ? R.string.collapse : R.string.all_history
        );
    }

    private void dialogUpdatePage() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint(R.string.hint_current_page);
        input.setText(String.valueOf(Math.max(0, book.currentPage)));
        input.setSelectAllOnFocus(true);

        RealmDialog.showContent(
                this,
                R.string.button_update_page,
                input,
                R.string.dialog_ok,
                R.string.dialog_cancel,
                dialog -> {
                    int newPage = ViewUtils.parseInt(
                            input.getText().toString(),
                            -1
                    );

                    if (newPage < 0) {
                        ViewUtils.toast(this, R.string.toast_invalid_input);
                        return false;
                    }

                    if (book.totalPages <= 0 || newPage > book.totalPages) {
                        Toast.makeText(
                                this,
                                R.string.invalid_book_page_range,
                                Toast.LENGTH_SHORT
                        ).show();
                        return false;
                    }

                    int oldPage = book.currentPage;
                    if (newPage != oldPage) {
                        book.currentPage = newPage;
                        book.readingHistory.add(
                                0,
                                new ReadingHistory(
                                        UUID.randomUUID().toString(),
                                        nowText(),
                                        oldPage,
                                        newPage
                                )
                        );
                        finishReadingAction();
                    }
                    return true;
                }
        );
    }

    private void dialogPageNote() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        box.setPadding(padding, 0, padding, 0);

        EditText pageInput = new EditText(this);
        pageInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        pageInput.setHint(R.string.hint_note_page);
        pageInput.setText(String.valueOf(Math.max(0, book.currentPage)));

        EditText noteInput = new EditText(this);
        noteInput.setMinLines(4);
        noteInput.setGravity(48);
        noteInput.setHint(R.string.hint_page_note);

        box.addView(pageInput);
        box.addView(noteInput);

        RealmDialog.showContent(
                this,
                R.string.button_page_note,
                box,
                R.string.dialog_ok,
                R.string.dialog_cancel,
                dialog -> {
                    int page = ViewUtils.parseInt(
                            pageInput.getText().toString(),
                            -1
                    );
                    String body = noteInput.getText().toString().trim();

                    if (page <= 0 || body.isEmpty()) {
                        ViewUtils.toast(this, R.string.toast_invalid_input);
                        return false;
                    }

                    if (page > book.currentPage) {
                        Toast.makeText(
                                this,
                                R.string.page_note_exceeds_reading_progress,
                                Toast.LENGTH_SHORT
                        ).show();
                        return false;
                    }

                    book.pageNotes.add(
                            0,
                            new PageNote(
                                    UUID.randomUUID().toString(),
                                    DateUtils.today(),
                                    page,
                                    body
                            )
                    );
                    finishReadingAction();
                    return true;
                }
        );
    }

    private void dialogFullReview() {
        EditText input = new EditText(this);
        input.setMinLines(7);
        input.setGravity(48);
        input.setHint(R.string.hint_full_review);
        input.setText(book.fullReview);

        RealmDialog.showContent(
                this,
                R.string.button_full_review,
                input,
                R.string.dialog_ok,
                R.string.dialog_cancel,
                dialog -> {
                    book.fullReview = input.getText().toString().trim();
                    finishReadingAction();
                    return true;
                }
        );
    }

    private void dialogEditBookInfo() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        box.setPadding(padding, 0, padding, 0);

        EditText titleInput = new EditText(this);
        titleInput.setHint(R.string.hint_book_title);
        titleInput.setText(book.title);

        EditText authorInput = new EditText(this);
        authorInput.setHint(R.string.hint_book_author);
        authorInput.setText(book.author);

        EditText genreInput = new EditText(this);
        genreInput.setHint(R.string.hint_book_genre);
        genreInput.setText(book.genre);

        EditText totalInput = new EditText(this);
        totalInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        totalInput.setHint(R.string.hint_total_pages);
        totalInput.setText(book.totalPages > 0 ? String.valueOf(book.totalPages) : "");

        box.addView(titleInput);
        box.addView(authorInput);
        box.addView(genreInput);
        box.addView(totalInput);

        RealmDialog.showContent(
                this,
                R.string.edit_book_info,
                box,
                R.string.dialog_ok,
                R.string.dialog_cancel,
                dialog -> {
                    String newTitle = titleInput.getText().toString().trim();
                    int totalPages = ViewUtils.parseInt(
                            totalInput.getText().toString(),
                            0
                    );

                    if (newTitle.isEmpty()
                            || totalPages <= 0
                            || totalPages < book.currentPage) {
                        Toast.makeText(
                                this,
                                R.string.invalid_total_pages,
                                Toast.LENGTH_SHORT
                        ).show();
                        return false;
                    }

                    book.title = newTitle;
                    book.author = authorInput.getText().toString().trim();
                    book.genre = genreInput.getText().toString().trim();
                    book.totalPages = totalPages;
                    repo.save(state);
                    render();

                    Toast.makeText(
                            this,
                            R.string.book_info_saved,
                            Toast.LENGTH_SHORT
                    ).show();
                    return true;
                }
        );
    }

    private boolean isBookFinished() {
        return book.totalPages > 0
                && book.currentPage >= book.totalPages;
    }

    private void showFullReview() {
        RealmDialog.showInfo(
                this,
                reviewHeading.getText(),
                book.fullReview.isEmpty() ? "暂无全书读后感" : book.fullReview
        );
    }

    private void showNote(PageNote note) {
        RealmDialog.showInfo(
                this,
                "P" + note.page + " · " + safe(note.date),
                safe(note.content)
        );
    }

    private void finishReadingAction() {
        String today = DateUtils.today();
        state.addReadingDate(today);
        RewardEngine.awardReading(this, state, today);
        repo.save(state);
        render();
    }

    private void shareBook() {
        String progressText = book.totalPages > 0
                ? book.currentPage + " / " + book.totalPages + " 页"
                : "当前第 " + book.currentPage + " 页";

        String text = "《"
                + safe(book.title)
                + "》\n作者："
                + safe(book.author)
                + "\n阅读进度："
                + progressText;

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, book.title);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, "分享书籍"));
    }

    private String getLastUpdatedText() {
        if (!book.readingHistory.isEmpty()) {
            return safe(book.readingHistory.get(0).dateTime);
        }

        return "暂无记录";
    }

    private void normalizeBook(Book target) {
        if (target.id == null || target.id.trim().isEmpty()) {
            target.id = UUID.randomUUID().toString();
        }
        if (target.title == null) target.title = "";
        if (target.author == null) target.author = "";
        if (target.coverUri == null) target.coverUri = "";
        if (target.genre == null) target.genre = "";
        if (target.fullReview == null) target.fullReview = "";
        if (target.pageNotes == null) target.pageNotes = new ArrayList<>();
        if (target.readingHistory == null) target.readingHistory = new ArrayList<>();
        if (target.currentPage < 0) target.currentPage = 0;
        if (target.totalPages < 0) target.totalPages = 0;
    }

    private void loadCover(ImageView imageView, String coverUri) {
        imageView.setImageDrawable(null);
        imageView.setBackgroundResource(R.drawable.bg_book_cover_placeholder);

        if (coverUri == null || coverUri.trim().isEmpty()) {
            return;
        }

        try (InputStream inputStream = getContentResolver()
                .openInputStream(Uri.parse(coverUri))) {

            if (inputStream == null) {
                return;
            }

            Drawable drawable = Drawable.createFromStream(
                    inputStream,
                    coverUri
            );

            if (drawable != null) {
                imageView.setImageDrawable(drawable);
            }
        } catch (Throwable ignored) {
            imageView.setImageDrawable(null);
        }
    }

    private String nowText() {
        return new SimpleDateFormat(
                "yyyy-MM-dd HH:mm",
                Locale.getDefault()
        ).format(new Date());
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private class NoteAdapter
            extends RecyclerView.Adapter<NoteAdapter.Holder> {

        private final List<PageNote> items = new ArrayList<>();

        void submitList(List<PageNote> newItems) {
            items.clear();
            if (newItems != null) items.addAll(newItems);
            notifyDataSetChanged();
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(
                            R.layout.item_book_detail_note,
                            parent,
                            false
                    );
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            PageNote note = items.get(position);
            holder.page.setText("P" + note.page);
            holder.content.setText(
                    safe(note.date) + "： " + safe(note.content)
            );
            holder.itemView.setOnClickListener(v -> showNote(note));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView page;
            final TextView content;

            Holder(View itemView) {
                super(itemView);
                page = itemView.findViewById(R.id.tvDetailNotePage);
                content = itemView.findViewById(R.id.tvDetailNoteContent);
            }
        }
    }

    private class HistoryAdapter
            extends RecyclerView.Adapter<HistoryAdapter.Holder> {

        private final List<ReadingHistory> items = new ArrayList<>();

        void submitList(List<ReadingHistory> newItems) {
            items.clear();
            if (newItems != null) items.addAll(newItems);
            notifyDataSetChanged();
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(
                            R.layout.item_book_reading_history,
                            parent,
                            false
                    );
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            ReadingHistory history = items.get(position);
            holder.date.setText(safe(history.dateTime));
            holder.pages.setText(
                    history.oldPage + "  →  " + history.newPage
            );
            holder.added.setText(
                    "+" + history.addedPages() + " 页"
            );
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView date;
            final TextView pages;
            final TextView added;

            Holder(View itemView) {
                super(itemView);
                date = itemView.findViewById(R.id.tvHistoryDate);
                pages = itemView.findViewById(R.id.tvHistoryPages);
                added = itemView.findViewById(R.id.tvHistoryAdded);
            }
        }
    }
}
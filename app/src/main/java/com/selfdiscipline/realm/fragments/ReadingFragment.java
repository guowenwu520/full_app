package com.selfdiscipline.realm.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.graphics.drawable.Drawable;
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
import com.selfdiscipline.realm.BookDetailActivity;
import com.selfdiscipline.realm.RecordListActivity;
import com.selfdiscipline.realm.data.AppRepository;
import com.selfdiscipline.realm.engine.RewardEngine;
import com.selfdiscipline.realm.model.AppState;
import com.selfdiscipline.realm.model.Book;
import com.selfdiscipline.realm.model.PageNote;
import com.selfdiscipline.realm.util.DateUtils;
import com.selfdiscipline.realm.util.ViewUtils;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 新阅读主页的数据绑定。
 *
 * 对应布局：
 * res/layout/fragment_reading_new.xml
 *
 * 页面包含：
 * 1. 四项阅读统计
 * 2. 最近阅读
 * 3. 我的书架 RecyclerView
 * 4. 最近笔记 RecyclerView
 * 5. 添加书籍、更新页码、全书读后感、分页批注
 */
public class ReadingFragment extends BaseFragmentHelper {

    private static final int REQ_COVER = 101;
    private static final int HOME_BOOK_LIMIT = 3;
    private static final int HOME_NOTE_LIMIT = 3;

    private AppRepository repo;
    private AppState state;

    // 四项统计的 include 根布局
    private View statMonthlyPages;
    private View statReadingBooks;
    private View statFinishedBooks;
    private View statReadingStreak;

    // 最近阅读
    private LinearLayout recentReadingCard;
    private ImageView currentBookCover;
    private TextView currentBookTitle;
    private TextView currentBookAuthor;
    private TextView currentReadingProgress;
    private TextView currentReadingPercent;
    private ProgressBar currentBookProgress;
    private TextView continueReadingButton;
    private TextView bookReflectionButton;
    private TextView pageNotesButton;

    // 书架与笔记
    private RecyclerView bookshelfRecycler;
    private RecyclerView recentNotesRecycler;
    private BookAdapter bookAdapter;
    private NoteAdapter noteAdapter;

    // 当前最近阅读书籍
    private Book currentBook;
    private int currentBookIndex = -1;

    // 添加书籍时暂存的封面
    private String pendingCover = "";
    private TextView pendingCoverStatusView;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        // 如果你把新布局重命名成 fragment_reading.xml，
        // 这里改为 R.layout.fragment_reading。
        View root = inflater.inflate(
                R.layout.fragment_reading,
                container,
                false
        );

        try {
            repo = new AppRepository(requireSafeContext());
            state = repo.load();

            if (state == null) {
                state = new AppState();
            }

            normalizeState();
            bindViews(root);
            setupRecyclerViews();
            setupClicks(root);
            render();

        } catch (Throwable throwable) {
            showReadingError(throwable);
        }

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (repo != null) {
            try {
                state = repo.load();

                if (state == null) {
                    state = new AppState();
                }

                normalizeState();
                render();
            } catch (Throwable throwable) {
                showReadingError(throwable);
            }
        }
    }

    private Context requireSafeContext() {
        Activity activity = getActivity();

        if (activity == null) {
            throw new IllegalStateException(
                    "ReadingFragment 尚未连接到 Activity"
            );
        }

        return activity;
    }

    private void bindViews(View root) {
        // 统计卡片
        statMonthlyPages = root.findViewById(R.id.statMonthlyPages);
        statReadingBooks = root.findViewById(R.id.statReadingBooks);
        statFinishedBooks = root.findViewById(R.id.statFinishedBooks);
        statReadingStreak = root.findViewById(R.id.statReadingStreak);

        // 最近阅读
        recentReadingCard = root.findViewById(R.id.recentReadingCard);
        currentBookCover = root.findViewById(R.id.ivCurrentBookCover);
        currentBookTitle = root.findViewById(R.id.tvCurrentBookTitle);
        currentBookAuthor = root.findViewById(R.id.tvCurrentBookAuthor);
        currentReadingProgress = root.findViewById(
                R.id.tvCurrentReadingProgress
        );
        currentReadingPercent = root.findViewById(
                R.id.tvCurrentReadingPercent
        );
        currentBookProgress = root.findViewById(
                R.id.progressCurrentBook
        );
        continueReadingButton = root.findViewById(
                R.id.buttonContinueReading
        );
        bookReflectionButton = root.findViewById(
                R.id.buttonBookReflection
        );
        pageNotesButton = root.findViewById(
                R.id.buttonPageNotes
        );

        // RecyclerView
        bookshelfRecycler = root.findViewById(R.id.recyclerBookshelf);
        recentNotesRecycler = root.findViewById(
                R.id.recyclerRecentNotes
        );
    }

    private void setupRecyclerViews() {
        bookAdapter = new BookAdapter();
        noteAdapter = new NoteAdapter();

        bookshelfRecycler.setLayoutManager(
                new LinearLayoutManager(requireSafeContext())
        );
        bookshelfRecycler.setAdapter(bookAdapter);
        bookshelfRecycler.setNestedScrollingEnabled(false);
        bookshelfRecycler.setOverScrollMode(View.OVER_SCROLL_NEVER);

        recentNotesRecycler.setLayoutManager(
                new LinearLayoutManager(requireSafeContext())
        );
        recentNotesRecycler.setAdapter(noteAdapter);
        recentNotesRecycler.setNestedScrollingEnabled(false);
        recentNotesRecycler.setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    private void setupClicks(View root) {
        // “管理”同时保留添加书籍和查看全部书籍。
        root.findViewById(R.id.buttonManageBooks)
                .setOnClickListener(v -> showBookManageMenu());

        root.findViewById(R.id.buttonAllNotes)
                .setOnClickListener(v ->
                        RecordListActivity.open(
                                getActivity(),
                                RecordListActivity.TYPE_BOOKS
                        )
                );

        root.findViewById(R.id.ivReadingCalendar)
                .setOnClickListener(v ->
                        RecordListActivity.open(
                                getActivity(),
                                RecordListActivity.TYPE_BOOKS
                        )
                );

        continueReadingButton.setOnClickListener(v -> {
            if (currentBook != null) {
                dialogUpdatePage(currentBook);
            }
        });

        bookReflectionButton.setOnClickListener(v -> {
            if (currentBook != null) {
                dialogFullReview(currentBook);
            }
        });

        pageNotesButton.setOnClickListener(v -> {
            if (currentBook != null) {
                dialogPageNote(currentBook);
            }
        });

        currentBookCover.setOnClickListener(v -> openCurrentBookDetail());
        currentBookTitle.setOnClickListener(v -> openCurrentBookDetail());
    }

    private void render() {
        normalizeState();
        renderSummary();
        renderRecentBook();
        renderBookshelf();
        renderRecentNotes();
    }

    /**
     * 顶部四项统计。
     */
    private void renderSummary() {
        int monthlyPages = calculateMonthlyReadingPages();
        int readingCount = 0;
        int finishedCount = 0;

        for (Book book : state.books) {
            if (book == null) {
                continue;
            }

            normalizeBook(book);

            if (isBookFinished(book)) {
                finishedCount++;
            } else {
                readingCount++;
            }
        }

        int readingStreak = calculateReadingStreak();

        bindReadingStat(
                statMonthlyPages,
                R.drawable.ic_exp_reading,
                "本月阅读",
                monthlyPages < 0
                        ? "--"
                        : String.valueOf(monthlyPages),
                monthlyPages < 0 ? "" : "页"
        );

        bindReadingStat(
                statReadingBooks,
                R.drawable.ic_exp_reading,
                "在读",
                String.valueOf(readingCount),
                "本"
        );

        bindReadingStat(
                statFinishedBooks,
                R.drawable.ic_core_no_break,
                "已读完",
                String.valueOf(finishedCount),
                "本"
        );

        bindReadingStat(
                statReadingStreak,
                R.drawable.ic_core_streak,
                "连续阅读",
                String.valueOf(readingStreak),
                "天"
        );
    }

    private void bindReadingStat(
            View statRoot,
            int iconRes,
            String label,
            String value,
            String unit
    ) {
        if (statRoot == null) {
            return;
        }

        ImageView icon = statRoot.findViewById(
                R.id.ivReadingStatIcon
        );
        TextView labelView = statRoot.findViewById(
                R.id.tvReadingStatLabel
        );
        TextView valueView = statRoot.findViewById(
                R.id.tvReadingStatValue
        );
        TextView unitView = statRoot.findViewById(
                R.id.tvReadingStatUnit
        );

        icon.setImageResource(iconRes);
        labelView.setText(label);
        valueView.setText(value);
        unitView.setText(unit);
    }

    /**
     * 最近阅读默认取书籍列表第一本。
     * 旧代码添加新书时使用 add(0, book)，因此第一本就是最近添加或最近操作书籍。
     */
    private void renderRecentBook() {
        currentBook = null;
        currentBookIndex = -1;

        for (int i = 0; i < state.books.size(); i++) {
            Book book = state.books.get(i);

            if (book != null) {
                currentBook = book;
                currentBookIndex = i;
                break;
            }
        }

        if (currentBook == null) {
            renderEmptyRecentBook();
            return;
        }

        normalizeBook(currentBook);
        setCurrentActionsEnabled(true);
        setFullReviewEnabled(isBookFinished(currentBook));
        loadCover(currentBookCover, currentBook.coverUri);

        currentBookTitle.setText(
                "《" + safe(currentBook.title) + "》"
        );
        currentBookAuthor.setText(safe(currentBook.author));

        int totalPages = getTotalPages(currentBook);

        if (totalPages > 0) {
            int currentPage = Math.min(
                    currentBook.currentPage,
                    totalPages
            );
            double percent = currentPage * 100.0 / totalPages;

            currentReadingProgress.setText(
                    String.format(
                            Locale.getDefault(),
                            "进度：%d / %d 页",
                            currentPage,
                            totalPages
                    )
            );
            currentReadingPercent.setText(
                    String.format(
                            Locale.getDefault(),
                            "%.1f%%",
                            percent
                    )
            );
            currentReadingPercent.setVisibility(View.VISIBLE);
            currentBookProgress.setVisibility(View.VISIBLE);
            currentBookProgress.setProgress(
                    (int) Math.round(percent)
            );
        } else {
            currentReadingProgress.setText(
                    String.format(
                            Locale.getDefault(),
                            "进度：当前第 %d 页",
                            currentBook.currentPage
                    )
            );
            currentReadingPercent.setText("--");
            currentReadingPercent.setVisibility(View.VISIBLE);
            currentBookProgress.setVisibility(View.VISIBLE);
            currentBookProgress.setProgress(0);
        }
    }

    private void renderEmptyRecentBook() {
        currentBookCover.setImageResource(
                R.drawable.bg_book_cover_placeholder
        );
        currentBookTitle.setText("暂无书籍");
        currentBookAuthor.setText("点击“管理”添加书籍");
        currentReadingProgress.setText("进度：--");
        currentReadingPercent.setText("--");
        currentBookProgress.setProgress(0);
        setCurrentActionsEnabled(false);
        setFullReviewEnabled(false);
    }

    private void setCurrentActionsEnabled(boolean enabled) {
        continueReadingButton.setEnabled(enabled);
        bookReflectionButton.setEnabled(enabled);
        pageNotesButton.setEnabled(enabled);

        float alpha = enabled ? 1.0f : 0.45f;
        continueReadingButton.setAlpha(alpha);
        bookReflectionButton.setAlpha(alpha);
        pageNotesButton.setAlpha(alpha);
    }

    private void setFullReviewEnabled(boolean enabled) {
        bookReflectionButton.setEnabled(enabled);
        bookReflectionButton.setAlpha(enabled ? 1.0f : 0.45f);
    }

    private void openCurrentBookDetail() {
        if (currentBookIndex >= 0) {
            BookDetailActivity.open(getActivity(), currentBookIndex);
        }
    }

    /**
     * 首页书架显示前三本，完整书架由“管理”进入。
     */
    private void renderBookshelf() {
        List<BookRow> rows = new ArrayList<>();

        int count = Math.min(HOME_BOOK_LIMIT, state.books.size());

        for (int i = 0; i < count; i++) {
            Book book = state.books.get(i);

            if (book != null) {
                normalizeBook(book);
                rows.add(new BookRow(i, book));
            }
        }

        bookAdapter.submitList(rows);
        bookshelfRecycler.setVisibility(
                rows.isEmpty() ? View.GONE : View.VISIBLE
        );
    }

    /**
     * 汇总所有书籍的批注，按日期倒序显示前三条。
     */
    private void renderRecentNotes() {
        List<NoteRow> notes = new ArrayList<>();

        for (int bookIndex = 0;
             bookIndex < state.books.size();
             bookIndex++) {

            Book book = state.books.get(bookIndex);

            if (book == null) {
                continue;
            }

            normalizeBook(book);

            for (PageNote note : book.pageNotes) {
                if (note != null) {
                    notes.add(
                            new NoteRow(
                                    bookIndex,
                                    safe(book.title),
                                    note
                            )
                    );
                }
            }
        }

        Collections.sort(
                notes,
                (left, right) -> safe(right.note.date)
                        .compareTo(safe(left.note.date))
        );

        if (notes.size() > HOME_NOTE_LIMIT) {
            notes = new ArrayList<>(
                    notes.subList(0, HOME_NOTE_LIMIT)
            );
        }

        noteAdapter.submitList(notes);
        recentNotesRecycler.setVisibility(
                notes.isEmpty() ? View.GONE : View.VISIBLE
        );
    }

    private void showBookManageMenu() {
        String[] actions = {
                getString(R.string.button_add_book),
                getString(R.string.button_view_all_books)
        };

        new AlertDialog.Builder(requireSafeContext())
                .setTitle(R.string.my_bookshelf)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        showAddBookDialog();
                    } else {
                        RecordListActivity.open(
                                getActivity(),
                                RecordListActivity.TYPE_BOOKS
                        );
                    }
                })
                .show();
    }

    /**
     * 新页面没有旧版的输入区域，因此改成弹窗添加书籍。
     */
    private void showAddBookDialog() {
        pendingCover = "";

        LinearLayout box = new LinearLayout(requireSafeContext());
        box.setOrientation(LinearLayout.VERTICAL);

        int padding = ViewUtils.dp(requireSafeContext(), 20);
        box.setPadding(padding, padding / 2, padding, 0);

        EditText titleInput = new EditText(requireSafeContext());
        titleInput.setHint(R.string.hint_book_title);

        EditText authorInput = new EditText(requireSafeContext());
        authorInput.setHint(R.string.hint_book_author);

        EditText genreInput = new EditText(requireSafeContext());
        genreInput.setHint(R.string.hint_book_genre);

        EditText totalPagesInput = new EditText(requireSafeContext());
        totalPagesInput.setHint(R.string.hint_total_pages);
        totalPagesInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        EditText pageInput = new EditText(requireSafeContext());
        pageInput.setHint(R.string.hint_current_page);
        pageInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        TextView pickCoverButton = ViewUtils.softButton(
                requireSafeContext(),
                getString(R.string.button_pick_cover)
        );

        pendingCoverStatusView = new TextView(requireSafeContext());
        pendingCoverStatusView.setText(
                R.string.text_cover_not_selected
        );
        pendingCoverStatusView.setTextColor(
                getResources().getColor(R.color.color_text_sub)
        );
        pendingCoverStatusView.setPadding(
                0,
                ViewUtils.dp(requireSafeContext(), 6),
                0,
                ViewUtils.dp(requireSafeContext(), 8)
        );

        pickCoverButton.setOnClickListener(v -> pickCover());

        box.addView(titleInput);
        box.addView(authorInput);
        box.addView(genreInput);
        box.addView(totalPagesInput);
        box.addView(pageInput);
        box.addView(pickCoverButton);
        box.addView(pendingCoverStatusView);

        AlertDialog dialog = new AlertDialog.Builder(
                requireSafeContext()
        )
                .setTitle(R.string.button_add_book)
                .setView(box)
                .setPositiveButton(R.string.dialog_ok, null)
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();

        dialog.setOnShowListener(ignored ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(v -> {
                            String title = ViewUtils.text(titleInput);
                            String author = ViewUtils.text(authorInput);
                            String genre = ViewUtils.text(genreInput);
                            int totalPages = ViewUtils.parseInt(
                                    ViewUtils.text(totalPagesInput),
                                    -1
                            );
                            int currentPage = ViewUtils.parseInt(
                                    ViewUtils.text(pageInput),
                                    0
                            );

                            if (title.isEmpty()
                                    || totalPages <= 0
                                    || currentPage < 0
                                    || currentPage > totalPages) {
                                Toast.makeText(
                                        getActivity(),
                                        R.string.invalid_book_page_range,
                                        Toast.LENGTH_SHORT
                                ).show();
                                return;
                            }

                            addBook(
                                    title,
                                    author,
                                    genre,
                                    currentPage,
                                    totalPages
                            );
                            dialog.dismiss();
                        })
        );

        dialog.setOnDismissListener(ignored ->
                pendingCoverStatusView = null
        );

        dialog.show();
    }

    private void pickCover() {
        try {
            Intent intent = new Intent(
                    Intent.ACTION_OPEN_DOCUMENT
            );
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            );
            startActivityForResult(intent, REQ_COVER);
        } catch (Throwable throwable) {
            ViewUtils.toast(
                    getActivity(),
                    R.string.toast_operation_failed
            );
        }
    }

    @Override
    public void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQ_COVER
                || resultCode != Activity.RESULT_OK
                || data == null
                || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();
        pendingCover = uri.toString();

        try {
            getActivity()
                    .getContentResolver()
                    .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
        } catch (Throwable ignored) {
            // 部分图库不支持持久权限，不影响本次选择。
        }

        if (pendingCoverStatusView != null) {
            pendingCoverStatusView.setText(
                    R.string.text_cover_selected
            );
        }
    }

    private void addBook(
            String title,
            String author,
            String genre,
            int currentPage,
            int totalPages
    ) {
        try {
            Book book = new Book(
                    UUID.randomUUID().toString(),
                    title,
                    author,
                    pendingCover,
                    currentPage,
                    totalPages,
                    genre
            );

            state.books.add(0, book);

            String today = DateUtils.today();
            state.addReadingDate(today);

            RewardEngine.RewardResult reward =
                    RewardEngine.awardReading(
                            getActivity(),
                            state,
                            today
                    );

            repo.save(state);
            pendingCover = "";
            showReward(reward);
            render();

        } catch (Throwable throwable) {
            ViewUtils.toast(
                    getActivity(),
                    R.string.toast_operation_failed
            );
        }
    }

    private void showBookActions(
            Book book,
            int bookIndex
    ) {
        String[] actions = {
                getString(R.string.button_update_page),
                getString(R.string.button_full_review),
                getString(R.string.button_page_note),
                getString(R.string.label_tap_detail)
        };

        new AlertDialog.Builder(requireSafeContext())
                .setTitle(safe(book.title))
                .setItems(actions, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            dialogUpdatePage(book);
                            break;

                        case 1:
                            dialogFullReview(book);
                            break;

                        case 2:
                            dialogPageNote(book);
                            break;

                        default:
                            BookDetailActivity.open(getActivity(), bookIndex);
                            break;
                    }
                })
                .show();
    }

    private void dialogUpdatePage(Book book) {
        EditText input = new EditText(requireSafeContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint(R.string.hint_current_page);
        input.setText(
                String.valueOf(
                        Math.max(0, book.currentPage)
                )
        );

        new AlertDialog.Builder(requireSafeContext())
                .setTitle(R.string.button_update_page)
                .setView(input)
                .setPositiveButton(
                        R.string.dialog_ok,
                        (dialog, which) -> {
                            int page = ViewUtils.parseInt(
                                    input.getText().toString(),
                                    -1
                            );

                            int totalPages = getTotalPages(book);

                            if (page < 0
                                    || totalPages <= 0
                                    || page > totalPages) {
                                Toast.makeText(
                                        getActivity(),
                                        R.string.invalid_book_page_range,
                                        Toast.LENGTH_SHORT
                                ).show();
                                return;
                            }

                            book.currentPage = page;
                            moveBookToFront(book);
                            finishReadingAction();
                        }
                )
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void dialogFullReview(Book book) {
        if (!isBookFinished(book)) {
            Toast.makeText(
                    getActivity(),
                    R.string.full_review_requires_finished_book,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        EditText input = new EditText(requireSafeContext());
        input.setMinLines(5);
        input.setGravity(48);
        input.setHint(R.string.hint_full_review);
        input.setText(
                book.fullReview == null
                        ? ""
                        : book.fullReview
        );

        new AlertDialog.Builder(requireSafeContext())
                .setTitle(R.string.button_full_review)
                .setView(input)
                .setPositiveButton(
                        R.string.dialog_ok,
                        (dialog, which) -> {
                            book.fullReview =
                                    input.getText().toString();
                            moveBookToFront(book);
                            finishReadingAction();
                        }
                )
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void dialogPageNote(Book book) {
        LinearLayout box = new LinearLayout(
                requireSafeContext()
        );
        box.setOrientation(LinearLayout.VERTICAL);

        EditText pageInput = new EditText(
                requireSafeContext()
        );
        pageInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        pageInput.setHint(R.string.hint_note_page);
        pageInput.setText(
                String.valueOf(
                        Math.max(0, book.currentPage)
                )
        );

        EditText noteInput = new EditText(
                requireSafeContext()
        );
        noteInput.setMinLines(4);
        noteInput.setGravity(48);
        noteInput.setHint(R.string.hint_page_note);

        box.addView(pageInput);
        box.addView(noteInput);

        new AlertDialog.Builder(requireSafeContext())
                .setTitle(R.string.button_page_note)
                .setView(box)
                .setPositiveButton(
                        R.string.dialog_ok,
                        (dialog, which) -> {
                            int page = ViewUtils.parseInt(
                                    pageInput.getText().toString(),
                                    -1
                            );
                            String body = noteInput
                                    .getText()
                                    .toString()
                                    .trim();

                            if (page <= 0 || body.isEmpty()) {
                                ViewUtils.toast(
                                        getActivity(),
                                        R.string.toast_invalid_input
                                );
                                return;
                            }

                            if (page > book.currentPage) {
                                Toast.makeText(
                                        getActivity(),
                                        R.string.page_note_exceeds_reading_progress,
                                        Toast.LENGTH_SHORT
                                ).show();
                                return;
                            }

                            normalizeBook(book);

                            book.pageNotes.add(
                                    0,
                                    new PageNote(
                                            UUID.randomUUID()
                                                    .toString(),
                                            DateUtils.today(),
                                            page,
                                            body
                                    )
                            );

                            moveBookToFront(book);
                            finishReadingAction();
                        }
                )
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void moveBookToFront(Book book) {
        int oldIndex = state.books.indexOf(book);

        if (oldIndex > 0) {
            state.books.remove(oldIndex);
            state.books.add(0, book);
        }
    }

    private void finishReadingAction() {
        try {
            String today = DateUtils.today();
            state.addReadingDate(today);

            RewardEngine.RewardResult reward =
                    RewardEngine.awardReading(
                            getActivity(),
                            state,
                            today
                    );

            repo.save(state);
            showReward(reward);
            render();

        } catch (Throwable throwable) {
            ViewUtils.toast(
                    getActivity(),
                    R.string.toast_operation_failed
            );
        }
    }

    private boolean isBookFinished(Book book) {
        int totalPages = getTotalPages(book);
        return totalPages > 0 && book.currentPage >= totalPages;
    }

    /**
     * 兼容未来给 Book 增加 totalPages/pageCount 字段的情况。
     * 当前旧模型没有总页数字段时返回 0。
     */
    private int getTotalPages(Book book) {
        Number totalPages = readNumber(
                book,
                "totalPages",
                "pageCount",
                "pages",
                "bookPages"
        );

        return totalPages == null
                ? 0
                : Math.max(0, totalPages.intValue());
    }

    /**
     * 从 readingDates 计算连续阅读天数。
     */
    private int calculateReadingStreak() {
        Collection<?> dates = readCollection(
                state,
                "readingDates",
                "readDates"
        );

        if (dates == null || dates.isEmpty()) {
            return 0;
        }

        Set<String> dateSet = new HashSet<>();

        for (Object date : dates) {
            if (date != null) {
                dateSet.add(String.valueOf(date));
            }
        }

        SimpleDateFormat format = new SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
        );
        Calendar calendar = Calendar.getInstance();

        // 今天尚未打卡时，允许从昨天开始计算当前连续记录。
        String today = format.format(calendar.getTime());

        if (!dateSet.contains(today)) {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
        }

        int streak = 0;

        while (dateSet.contains(
                format.format(calendar.getTime())
        )) {
            streak++;
            calendar.add(Calendar.DAY_OF_MONTH, -1);
        }

        return streak;
    }

    /**
     * 旧 Book 模型只保存 currentPage，无法精确计算本月新增页数。
     *
     * 如果数据模型存在 readingLogs/readingHistory 等历史记录，
     * 本方法会自动读取；否则返回 -1，界面显示“--”。
     */
    private int calculateMonthlyReadingPages() {
        String today = DateUtils.today();

        if (today == null || today.length() < 7) {
            return -1;
        }

        String monthPrefix = today.substring(0, 7);
        boolean historyFound = false;
        int total = 0;

        Collection<?> stateLogs = readCollection(
                state,
                "readingLogs",
                "readingHistory",
                "pageLogs",
                "readingRecords"
        );

        if (stateLogs != null) {
            historyFound = true;
            total += sumReadingPages(
                    stateLogs,
                    monthPrefix
            );
        }

        for (Book book : state.books) {
            Collection<?> bookLogs = readCollection(
                    book,
                    "readingLogs",
                    "readingHistory",
                    "pageLogs",
                    "progressLogs"
            );

            if (bookLogs != null) {
                historyFound = true;
                total += sumReadingPages(
                        bookLogs,
                        monthPrefix
                );
            }
        }

        return historyFound ? total : -1;
    }

    private int sumReadingPages(
            Collection<?> records,
            String monthPrefix
    ) {
        int total = 0;

        for (Object record : records) {
            String date = readText(
                    record,
                    "date",
                    "day",
                    "recordDate"
            );

            if (date == null
                    || !date.startsWith(monthPrefix)) {
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
                total += Math.max(
                        0,
                        directPages.intValue()
                );
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
                        newPage.intValue()
                                - oldPage.intValue()
                );
            }
        }

        return total;
    }

    /**
     * 安全加载封面。
     *
     * 不再使用 ImageView.setImageURI()：该方法可能延迟到 onMeasure()
     * 才真正读取 URI，导致 SecurityException 无法在这里捕获并使页面崩溃。
     * 现在先通过 ContentResolver 同步读取，再把 Drawable 交给 ImageView。
     */
    private void loadCover(
            ImageView imageView,
            String coverUri
    ) {
        imageView.setImageDrawable(null);
        imageView.setBackgroundResource(
                R.drawable.bg_book_cover_placeholder
        );

        if (coverUri == null
                || coverUri.trim().isEmpty()) {
            return;
        }

        Uri uri;

        try {
            uri = Uri.parse(coverUri);
        } catch (Throwable ignored) {
            return;
        }

        try (InputStream inputStream = requireSafeContext()
                .getContentResolver()
                .openInputStream(uri)) {

            if (inputStream == null) {
                return;
            }

            Drawable drawable = Drawable.createFromStream(
                    inputStream,
                    uri.toString()
            );

            if (drawable != null) {
                imageView.setImageDrawable(drawable);
            }

        } catch (SecurityException securityException) {
            // 常见于重装应用或导入备份后：旧 content:// URI 的授权已失效。
            // 保留默认封面，不让页面崩溃；用户重新选择封面后即可恢复。
            imageView.setImageDrawable(null);
        } catch (Throwable ignored) {
            // 文件被删除、URI 无效或图片损坏时同样显示默认封面。
            imageView.setImageDrawable(null);
        }
    }

    private void normalizeState() {
        if (state == null) {
            state = new AppState();
        }

        if (state.books == null) {
            state.books = new ArrayList<>();
        }

        for (Book book : state.books) {
            if (book != null) {
                normalizeBook(book);
            }
        }
    }

    private void normalizeBook(Book book) {
        if (book.id == null || book.id.trim().isEmpty()) {
            book.id = UUID.randomUUID().toString();
        }

        if (book.title == null) {
            book.title = "";
        }

        if (book.author == null) {
            book.author = "";
        }

        if (book.coverUri == null) {
            book.coverUri = "";
        }

        if (book.fullReview == null) {
            book.fullReview = "";
        }

        if (book.pageNotes == null) {
            book.pageNotes = new ArrayList<>();
        }

        if (book.currentPage < 0) {
            book.currentPage = 0;
        }
    }

    private void showReadingError(Throwable throwable) {
        if (getActivity() != null) {
            ViewUtils.toast(
                    getActivity(),
                    R.string.toast_operation_failed
            );
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    /*
     * 反射读取仅用于兼容旧模型和未来扩展字段，
     * 不改变当前 Book/AppState 的实际结构。
     */

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
                return Double.parseDouble(
                        String.valueOf(value)
                );
            } catch (NumberFormatException ignored) {
                // 不是数值。
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

    private Object readField(
            Object target,
            String fieldName
    ) {
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

    private Object invokeGetter(
            Object target,
            String name
    ) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        String suffix =
                Character.toUpperCase(name.charAt(0))
                        + name.substring(1);

        String[] methodNames = {
                "get" + suffix,
                "is" + suffix
        };

        for (String methodName : methodNames) {
            try {
                Method method = target
                        .getClass()
                        .getMethod(methodName);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (Exception ignored) {
                // 继续尝试。
            }
        }

        return null;
    }

    private static class BookRow {
        final int stateIndex;
        final Book book;

        BookRow(int stateIndex, Book book) {
            this.stateIndex = stateIndex;
            this.book = book;
        }
    }

    private class BookAdapter
            extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {

        private final List<BookRow> rows = new ArrayList<>();

        void submitList(List<BookRow> newRows) {
            rows.clear();

            if (newRows != null) {
                rows.addAll(newRows);
            }

            notifyDataSetChanged();
        }

        @Override
        public BookViewHolder onCreateViewHolder(
                ViewGroup parent,
                int viewType
        ) {
            View view = LayoutInflater
                    .from(parent.getContext())
                    .inflate(
                            R.layout.item_reading_book,
                            parent,
                            false
                    );

            return new BookViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                BookViewHolder holder,
                int position
        ) {
            BookRow row = rows.get(position);
            Book book = row.book;

            loadCover(holder.cover, book.coverUri);
            holder.title.setText(
                    "《" + safe(book.title) + "》"
            );
            holder.author.setText(safe(book.author));

            int totalPages = getTotalPages(book);
            boolean finished = isBookFinished(book);

            if (totalPages > 0) {
                int currentPage = Math.min(
                        book.currentPage,
                        totalPages
                );
                double percent = currentPage
                        * 100.0
                        / totalPages;

                holder.progressText.setText(
                        String.format(
                                Locale.getDefault(),
                                "%d / %d 页",
                                currentPage,
                                totalPages
                        )
                );
                holder.progress.setProgress(
                        (int) Math.round(percent)
                );
                holder.progress.setVisibility(View.VISIBLE);
            } else {
                holder.progressText.setText(
                        String.format(
                                Locale.getDefault(),
                                "第 %d 页",
                                book.currentPage
                        )
                );
                holder.progress.setProgress(0);
                holder.progress.setVisibility(View.VISIBLE);
            }

            holder.status.setText(
                    finished ? "读完" : "在读"
            );
            holder.status.setBackgroundResource(
                    finished
                            ? R.drawable.bg_reading_chip_finished
                            : R.drawable.bg_reading_chip_active
            );

            holder.itemView.setOnClickListener(v ->
                    BookDetailActivity.open(
                            getActivity(),
                            row.stateIndex
                    )
            );

            holder.more.setOnClickListener(v ->
                    showBookActions(
                            book,
                            row.stateIndex
                    )
            );
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        class BookViewHolder extends RecyclerView.ViewHolder {

            final ImageView cover;
            final TextView title;
            final TextView author;
            final TextView progressText;
            final ProgressBar progress;
            final TextView status;
            final ImageView more;

            BookViewHolder(View itemView) {
                super(itemView);

                cover = itemView.findViewById(
                        R.id.ivBookCover
                );
                title = itemView.findViewById(
                        R.id.tvBookTitle
                );
                author = itemView.findViewById(
                        R.id.tvBookAuthor
                );
                progressText = itemView.findViewById(
                        R.id.tvBookProgress
                );
                progress = itemView.findViewById(
                        R.id.progressBook
                );
                status = itemView.findViewById(
                        R.id.tvBookStatus
                );
                more = itemView.findViewById(
                        R.id.ivBookMore
                );
            }
        }
    }

    private static class NoteRow {
        final int bookIndex;
        final String bookTitle;
        final PageNote note;

        NoteRow(
                int bookIndex,
                String bookTitle,
                PageNote note
        ) {
            this.bookIndex = bookIndex;
            this.bookTitle = bookTitle;
            this.note = note;
        }
    }

    private class NoteAdapter
            extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

        private final List<NoteRow> rows = new ArrayList<>();

        void submitList(List<NoteRow> newRows) {
            rows.clear();

            if (newRows != null) {
                rows.addAll(newRows);
            }

            notifyDataSetChanged();
        }

        @Override
        public NoteViewHolder onCreateViewHolder(
                ViewGroup parent,
                int viewType
        ) {
            View view = LayoutInflater
                    .from(parent.getContext())
                    .inflate(
                            R.layout.item_reading_note,
                            parent,
                            false
                    );

            return new NoteViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                NoteViewHolder holder,
                int position
        ) {
            NoteRow row = rows.get(position);

            holder.content.setText(
                    String.format(
                            Locale.getDefault(),
                            "P%d：%s",
                            row.note.page,
                            safe(row.note.content)
                    )
            );
            holder.date.setText(safe(row.note.date));

            holder.itemView.setOnClickListener(v ->
                    BookDetailActivity.open(
                            getActivity(),
                            row.bookIndex
                    )
            );

            holder.more.setOnClickListener(v ->
                    BookDetailActivity.open(
                            getActivity(),
                            row.bookIndex
                    )
            );
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        class NoteViewHolder extends RecyclerView.ViewHolder {

            final TextView content;
            final TextView date;
            final ImageView more;

            NoteViewHolder(View itemView) {
                super(itemView);

                content = itemView.findViewById(
                        R.id.tvNoteContent
                );
                date = itemView.findViewById(
                        R.id.tvNoteDate
                );
                more = itemView.findViewById(
                        R.id.ivNoteMore
                );
            }
        }
    }
}

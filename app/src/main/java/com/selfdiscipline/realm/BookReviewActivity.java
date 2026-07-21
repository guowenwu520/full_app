package com.selfdiscipline.realm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.selfdiscipline.realm.data.AppRepository;
import com.selfdiscipline.realm.engine.RewardEngine;
import com.selfdiscipline.realm.model.AppState;
import com.selfdiscipline.realm.model.Book;
import com.selfdiscipline.realm.util.DateUtils;

/** 独立的全书读后感编辑页，界面和日记写作保持一致。 */
public class BookReviewActivity extends Activity {
    private static final String EXTRA_BOOK_INDEX = "book_index";

    private AppRepository repository;
    private AppState state;
    private Book book;
    private int bookIndex;
    private EditText titleInput;
    private EditText bodyInput;
    private TextView bookView;
    private TextView saveButton;

    public static void open(Context context, int bookIndex) {
        if (context == null) return;
        Intent intent = new Intent(context, BookReviewActivity.class);
        intent.putExtra(EXTRA_BOOK_INDEX, bookIndex);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_review);

        repository = new AppRepository(this);
        state = repository.load();
        bookIndex = getIntent().getIntExtra(EXTRA_BOOK_INDEX, -1);
        if (!loadBook()) {
            Toast.makeText(this, "书籍不存在或已被删除", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        titleInput = findViewById(R.id.inputBookReviewTitle);
        bodyInput = findViewById(R.id.inputBookReviewBody);
        bookView = findViewById(R.id.tvBookReviewBook);
        saveButton = findViewById(R.id.buttonSaveBookReview);

        findViewById(R.id.buttonBookReviewBack).setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> saveReview());
        render();
    }

    private boolean loadBook() {
        if (state == null || state.books == null
                || bookIndex < 0 || bookIndex >= state.books.size()) return false;
        book = state.books.get(bookIndex);
        if (book == null) return false;
        if (book.fullReviewTitle == null) book.fullReviewTitle = "";
        if (book.fullReview == null) book.fullReview = "";
        return true;
    }

    private void render() {
        bookView.setText("《" + safe(book.title) + "》 · " + DateUtils.today());
        titleInput.setText(safe(book.fullReviewTitle));
        bodyInput.setText(safe(book.fullReview));
        saveButton.setText(safe(book.fullReview).isEmpty() ? "保存读后感" : "更新读后感");
    }

    private void saveReview() {
        String title = titleInput.getText().toString().trim();
        String body = bodyInput.getText().toString().trim();
        if (body.isEmpty()) {
            Toast.makeText(this, "请先写下读后感正文", Toast.LENGTH_SHORT).show();
            return;
        }

        book.fullReviewTitle = title;
        book.fullReview = body;
        String today = DateUtils.today();
        state.addReadingDate(today);
        RewardEngine.awardReadingPages(
                this,
                state,
                today,
                0,
                "book_review_" + safe(book.id) + "_" + System.currentTimeMillis()
        );
        repository.save(state);
        Toast.makeText(this, "读后感已保存", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

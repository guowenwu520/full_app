package com.selfdiscipline.realm.engine;

import com.selfdiscipline.realm.model.AppState;
import com.selfdiscipline.realm.model.Book;
import com.selfdiscipline.realm.model.ReadingHistory;

import java.util.ArrayList;

/**
 * 统一维护阅读进度历史，避免“当前页已变化、统计记录却缺失”。
 */
public final class ReadingProgressEngine {

    private ReadingProgressEngine() {
    }

    /**
     * 为旧版本漏记的阅读进度补一条历史。
     *
     * 补录只覆盖历史中尚未出现的页码区间，不补发经验；补录完成后
     * 最高历史页码已经等于当前页，因此后续加载不会重复补录。
     */
    public static boolean backfillMissingHistory(AppState state, String now) {
        if (state == null || state.books == null) {
            return false;
        }

        String recordTime = resolveRecordTime(state, now);
        boolean changed = false;

        for (int index = 0; index < state.books.size(); index++) {
            Book book = state.books.get(index);
            if (book == null || book.currentPage <= 0) {
                continue;
            }

            if (book.readingHistory == null) {
                book.readingHistory = new ArrayList<>();
            }

            int highestRecordedPage = 0;
            for (ReadingHistory history : book.readingHistory) {
                if (history != null) {
                    highestRecordedPage = Math.max(
                            highestRecordedPage,
                            Math.max(history.oldPage, history.newPage)
                    );
                }
            }

            if (book.currentPage > highestRecordedPage) {
                String bookKey = safe(book.id).isEmpty()
                        ? String.valueOf(index)
                        : safe(book.id);
                book.readingHistory.add(
                        0,
                        new ReadingHistory(
                                "reading_backfill_" + bookKey + "_" + book.currentPage,
                                recordTime,
                                highestRecordedPage,
                                book.currentPage,
                                0
                        )
                );
                changed = true;
            }
        }

        return changed;
    }

    private static String resolveRecordTime(AppState state, String now) {
        String latestReadingDate = "";
        if (state.readingDates != null) {
            for (String date : state.readingDates) {
                if (date != null
                        && date.matches("\\d{4}-\\d{2}-\\d{2}")
                        && date.compareTo(latestReadingDate) > 0) {
                    latestReadingDate = date;
                }
            }
        }

        if (!latestReadingDate.isEmpty()) {
            if (now != null && now.startsWith(latestReadingDate)) {
                return now;
            }
            return latestReadingDate + " 23:59:59";
        }
        return safe(now);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

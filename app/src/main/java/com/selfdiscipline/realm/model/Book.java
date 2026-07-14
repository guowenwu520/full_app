package com.selfdiscipline.realm.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 兼容旧数据与新版书籍详情页的 Book 模型。
 *
 * 新增字段：
 * - totalPages
 * - genre
 * - readingHistory
 *
 * 同时保留 AppState 依赖的：
 * - Book.fromJson(JSONObject)
 * - book.toJson()
 */
public class Book {

    public String id;
    public String title;
    public String author;
    public String coverUri;
    public int currentPage;
    /** 已经发放过阅读经验的最高页码，防止页码回退后重复刷经验。 */
    public int rewardedPage;

    public int totalPages;
    public String genre;

    public String fullReview;
    public List<PageNote> pageNotes;
    public List<ReadingHistory> readingHistory;

    public Book() {
        id = "";
        title = "";
        author = "";
        coverUri = "";
        currentPage = 0;
        rewardedPage = 0;
        totalPages = 0;
        genre = "";
        fullReview = "";
        pageNotes = new ArrayList<>();
        readingHistory = new ArrayList<>();
    }

    /**
     * 保留旧版构造函数，旧代码无需修改。
     */
    public Book(
            String id,
            String title,
            String author,
            String coverUri,
            int currentPage
    ) {
        this(
                id,
                title,
                author,
                coverUri,
                currentPage,
                0,
                ""
        );
    }

    public Book(
            String id,
            String title,
            String author,
            String coverUri,
            int currentPage,
            int totalPages,
            String genre
    ) {
        this.id = safe(id);
        this.title = safe(title);
        this.author = safe(author);
        this.coverUri = safe(coverUri);
        this.currentPage = Math.max(0, currentPage);
        this.rewardedPage = 0;
        this.totalPages = Math.max(0, totalPages);
        this.genre = safe(genre);
        this.fullReview = "";
        this.pageNotes = new ArrayList<>();
        this.readingHistory = new ArrayList<>();
    }

    /**
     * 从本地 JSON 恢复书籍。
     *
     * 对旧备份兼容：
     * - 不存在 totalPages 时默认为 0
     * - 不存在 genre 时默认为空字符串
     * - 不存在 readingHistory 时默认为空列表
     */
    public static Book fromJson(JSONObject obj) {
        Book book = new Book();

        if (obj == null) {
            return book;
        }

        book.id = obj.optString("id", "");
        book.title = obj.optString("title", "");
        book.author = obj.optString("author", "");

        // 兼容可能存在的旧字段名。
        book.coverUri = obj.optString(
                "coverUri",
                obj.optString("cover", "")
        );

        book.currentPage = Math.max(
                0,
                obj.optInt(
                        "currentPage",
                        obj.optInt("page", 0)
                )
        );
        // 旧备份没有 rewardedPage 时，以当前页作为已发放上限，避免升级后重复补发。
        book.rewardedPage = Math.max(
                0,
                obj.optInt("rewardedPage", book.currentPage)
        );

        book.totalPages = Math.max(
                0,
                obj.optInt(
                        "totalPages",
                        obj.optInt("pageCount", 0)
                )
        );

        book.genre = obj.optString("genre", "");
        book.fullReview = obj.optString("fullReview", "");

        book.pageNotes = new ArrayList<>();
        JSONArray notesArray = obj.optJSONArray("pageNotes");

        if (notesArray != null) {
            for (int i = 0; i < notesArray.length(); i++) {
                JSONObject noteObject = notesArray.optJSONObject(i);

                if (noteObject == null) {
                    continue;
                }

                PageNote note = new PageNote(
                        noteObject.optString("id", ""),
                        noteObject.optString("date", ""),
                        Math.max(0, noteObject.optInt("page", 0)),
                        noteObject.optString("content", "")
                );

                book.pageNotes.add(note);
            }
        }

        book.readingHistory = new ArrayList<>();
        JSONArray historyArray = obj.optJSONArray("readingHistory");

        if (historyArray != null) {
            for (int i = 0; i < historyArray.length(); i++) {
                JSONObject historyObject =
                        historyArray.optJSONObject(i);

                if (historyObject != null) {
                    book.readingHistory.add(
                            ReadingHistory.fromJson(historyObject)
                    );
                }
            }
        }

        return book;
    }

    /**
     * 保存为本地 JSON。
     *
     * 方法内部处理 JSON 异常，调用端不需要额外 try/catch。
     */
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();

        try {
            obj.put("id", safe(id));
            obj.put("title", safe(title));
            obj.put("author", safe(author));
            obj.put("coverUri", safe(coverUri));
            obj.put("currentPage", Math.max(0, currentPage));
            obj.put("rewardedPage", Math.max(0, rewardedPage));

            obj.put("totalPages", Math.max(0, totalPages));
            obj.put("genre", safe(genre));
            obj.put("fullReview", safe(fullReview));

            JSONArray notesArray = new JSONArray();

            if (pageNotes != null) {
                for (PageNote note : pageNotes) {
                    if (note == null) {
                        continue;
                    }

                    JSONObject noteObject = new JSONObject();
                    noteObject.put("id", safe(note.id));
                    noteObject.put("date", safe(note.date));
                    noteObject.put("page", Math.max(0, note.page));
                    noteObject.put("content", safe(note.content));
                    notesArray.put(noteObject);
                }
            }

            obj.put("pageNotes", notesArray);

            JSONArray historyArray = new JSONArray();

            if (readingHistory != null) {
                for (ReadingHistory history : readingHistory) {
                    if (history != null) {
                        historyArray.put(history.toJson());
                    }
                }
            }

            obj.put("readingHistory", historyArray);

        } catch (Exception ignored) {
            // JSONObject 写入普通字段通常不会失败。
            // 即使某条异常，也返回当前已构建的 JSON，避免数据保存崩溃。
        }

        return obj;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

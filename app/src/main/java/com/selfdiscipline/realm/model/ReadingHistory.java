package com.selfdiscipline.realm.model;

import org.json.JSONObject;

/**
 * 单次页码更新历史。
 */
public class ReadingHistory {

    public String id;
    public String dateTime;
    public int oldPage;
    public int newPage;
    /** 本次计入经验的页数，避免页码回退后重复获得经验。 */
    public int creditedPages;

    public ReadingHistory() {
        id = "";
        dateTime = "";
        oldPage = 0;
        newPage = 0;
        creditedPages = 0;
    }

    public ReadingHistory(
            String id,
            String dateTime,
            int oldPage,
            int newPage
    ) {
        this(id, dateTime, oldPage, newPage, Math.max(0, newPage - oldPage));
    }

    public ReadingHistory(
            String id,
            String dateTime,
            int oldPage,
            int newPage,
            int creditedPages
    ) {
        this.id = safe(id);
        this.dateTime = safe(dateTime);
        this.oldPage = Math.max(0, oldPage);
        this.newPage = Math.max(0, newPage);
        this.creditedPages = Math.max(0, creditedPages);
    }

    public int addedPages() {
        return Math.max(0, creditedPages);
    }

    /**
     * 本条阅读记录实际增加的页数。
     * 它用于阅读量统计，与防止重复发放经验的 creditedPages 分开计算。
     */
    public int recordedPages() {
        return Math.max(0, newPage - oldPage);
    }

    public static ReadingHistory fromJson(JSONObject obj) {
        if (obj == null) {
            return new ReadingHistory();
        }

        int oldPage = obj.optInt("oldPage", 0);
        int newPage = obj.optInt("newPage", 0);
        return new ReadingHistory(
                obj.optString("id", ""),
                obj.optString(
                        "dateTime",
                        obj.optString("date", "")
                ),
                oldPage,
                newPage,
                obj.optInt("creditedPages", Math.max(0, newPage - oldPage))
        );
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();

        try {
            obj.put("id", safe(id));
            obj.put("dateTime", safe(dateTime));
            obj.put("oldPage", Math.max(0, oldPage));
            obj.put("newPage", Math.max(0, newPage));
            obj.put("creditedPages", Math.max(0, creditedPages));
        } catch (Exception ignored) {
        }

        return obj;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

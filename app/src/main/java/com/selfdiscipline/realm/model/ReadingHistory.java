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

    public ReadingHistory() {
        id = "";
        dateTime = "";
        oldPage = 0;
        newPage = 0;
    }

    public ReadingHistory(
            String id,
            String dateTime,
            int oldPage,
            int newPage
    ) {
        this.id = safe(id);
        this.dateTime = safe(dateTime);
        this.oldPage = Math.max(0, oldPage);
        this.newPage = Math.max(0, newPage);
    }

    public int addedPages() {
        return Math.max(0, newPage - oldPage);
    }

    public static ReadingHistory fromJson(JSONObject obj) {
        if (obj == null) {
            return new ReadingHistory();
        }

        return new ReadingHistory(
                obj.optString("id", ""),
                obj.optString(
                        "dateTime",
                        obj.optString("date", "")
                ),
                obj.optInt("oldPage", 0),
                obj.optInt("newPage", 0)
        );
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();

        try {
            obj.put("id", safe(id));
            obj.put("dateTime", safe(dateTime));
            obj.put("oldPage", Math.max(0, oldPage));
            obj.put("newPage", Math.max(0, newPage));
        } catch (Exception ignored) {
        }

        return obj;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

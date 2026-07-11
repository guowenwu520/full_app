package com.selfdiscipline.realm.model;

import org.json.JSONObject;
import org.json.JSONException;

public class PageNote {
    public String id;
    public String date;
    public int page;
    public String content;

    public PageNote(String id, String date, int page, String content) {
        this.id = id; this.date = date; this.page = page; this.content = content;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id); o.put("date", date); o.put("page", page); o.put("content", content);
        return o;
    }

    public static PageNote fromJson(JSONObject o) {
        return new PageNote(o.optString("id"), o.optString("date"), o.optInt("page"), o.optString("content"));
    }
}

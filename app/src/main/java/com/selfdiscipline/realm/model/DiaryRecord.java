package com.selfdiscipline.realm.model;
import org.json.JSONObject;
import org.json.JSONException;
public class DiaryRecord {
    public String date; public String title; public String body; public boolean broken;
    public DiaryRecord(String date, String title, String body, boolean broken) { this.date=date; this.title=title; this.body=body; this.broken=broken; }
    public JSONObject toJson() throws JSONException { JSONObject o = new JSONObject(); o.put("date",date); o.put("title",title); o.put("body",body); o.put("broken",broken); return o; }
    public static DiaryRecord fromJson(JSONObject o) { return new DiaryRecord(o.optString("date"), o.optString("title"), o.optString("body"), o.optBoolean("broken")); }
}

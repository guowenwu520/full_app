package com.selfdiscipline.realm.model;
import org.json.JSONObject;
import org.json.JSONException;
public class ExerciseRecord {
    public String date; public String content; public int calories;
    public ExerciseRecord(String date, String content, int calories) { this.date=date; this.content=content; this.calories=calories; }
    public JSONObject toJson() throws JSONException { JSONObject o = new JSONObject(); o.put("date",date); o.put("content",content); o.put("calories",calories); return o; }
    public static ExerciseRecord fromJson(JSONObject o) { return new ExerciseRecord(o.optString("date"), o.optString("content"), o.optInt("calories")); }
}

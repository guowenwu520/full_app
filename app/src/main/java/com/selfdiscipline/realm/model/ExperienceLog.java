package com.selfdiscipline.realm.model;
import org.json.JSONObject;
import org.json.JSONException;
public class ExperienceLog {
    public String date; public String key; public String source; public int points;
    public ExperienceLog(String date, String key, String source, int points) { this.date=date; this.key=key; this.source=source; this.points=points; }
    public JSONObject toJson() throws JSONException { JSONObject o = new JSONObject(); o.put("date",date); o.put("key",key); o.put("source",source); o.put("points",points); return o; }
    public static ExperienceLog fromJson(JSONObject o) { return new ExperienceLog(o.optString("date"), o.optString("key"), o.optString("source"), o.optInt("points")); }
}

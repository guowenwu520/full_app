package com.selfdiscipline.realm.model;
import org.json.JSONObject;
import org.json.JSONException;
public class SleepRecord {
    public String date; public String sleepTime; public String wakeTime; public boolean passed;
    public SleepRecord(String date, String sleepTime, String wakeTime, boolean passed) { this.date=date; this.sleepTime=sleepTime; this.wakeTime=wakeTime; this.passed=passed; }
    public JSONObject toJson() throws JSONException { JSONObject o = new JSONObject(); o.put("date",date); o.put("sleepTime",sleepTime); o.put("wakeTime",wakeTime); o.put("passed",passed); return o; }
    public static SleepRecord fromJson(JSONObject o) { return new SleepRecord(o.optString("date"), o.optString("sleepTime"), o.optString("wakeTime"), o.optBoolean("passed")); }
}

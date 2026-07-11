package com.selfdiscipline.realm.model;
import org.json.JSONObject;
import org.json.JSONException;
public class WeightRecord {
    public String date; public float weight;
    public WeightRecord(String date, float weight) { this.date=date; this.weight=weight; }
    public JSONObject toJson() throws JSONException { JSONObject o = new JSONObject(); o.put("date",date); o.put("weight",weight); return o; }
    public static WeightRecord fromJson(JSONObject o) { return new WeightRecord(o.optString("date"), (float)o.optDouble("weight")); }
}

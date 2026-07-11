package com.selfdiscipline.realm.model;
import org.json.JSONObject;
import org.json.JSONException;
public class WordEntry {
    public String word; public String meaning; public String createdDate; public int correctCount; public int wrongCount; public String lastTestDate;
    public WordEntry(String word, String meaning, String createdDate) { this.word=word; this.meaning=meaning; this.createdDate=createdDate; this.correctCount=correctCount; this.wrongCount=wrongCount; this.lastTestDate=""; }
    public JSONObject toJson() throws JSONException { JSONObject o = new JSONObject(); o.put("word",word); o.put("meaning",meaning); o.put("createdDate",createdDate); o.put("correctCount",correctCount); o.put("wrongCount",wrongCount); o.put("lastTestDate",lastTestDate); return o; }
    public static WordEntry fromJson(JSONObject o) { WordEntry e = new WordEntry(o.optString("word"), o.optString("meaning"), o.optString("createdDate")); e.correctCount=o.optInt("correctCount"); e.wrongCount=o.optInt("wrongCount"); e.lastTestDate=o.optString("lastTestDate"); return e; }
}

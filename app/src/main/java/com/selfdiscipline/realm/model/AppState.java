package com.selfdiscipline.realm.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AppState {
    public List<Book> books = new ArrayList<>();
    public List<ExerciseRecord> exercises = new ArrayList<>();
    public List<WeightRecord> weights = new ArrayList<>();
    public List<SleepRecord> sleeps = new ArrayList<>();
    public List<DiaryRecord> diaries = new ArrayList<>();
    public List<WordEntry> words = new ArrayList<>();
    public List<ExperienceLog> expLogs = new ArrayList<>();
    public List<String> readingDates = new ArrayList<>();
    public List<String> wordDates = new ArrayList<>();
    public List<String> unlockedBadgeIds = new ArrayList<>();
    public List<String> awardedKeys = new ArrayList<>();

    public boolean containsAwardKey(String key) { return awardedKeys.contains(key); }
    public void addAwardKey(String key) { if (!awardedKeys.contains(key)) awardedKeys.add(key); }
    public void addReadingDate(String date) { if (!readingDates.contains(date)) readingDates.add(date); }
    public void addWordDate(String date) { if (!wordDates.contains(date)) wordDates.add(date); }
    public boolean isBadgeUnlocked(String id) { return unlockedBadgeIds.contains(id); }
    public void unlockBadge(String id) { if (!unlockedBadgeIds.contains(id)) unlockedBadgeIds.add(id); }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        putArray(obj, "books", books); putArray(obj, "exercises", exercises); putArray(obj, "weights", weights); putArray(obj, "sleeps", sleeps); putArray(obj, "diaries", diaries); putArray(obj, "words", words); putArray(obj, "expLogs", expLogs);
        obj.put("readingDates", stringArray(readingDates)); obj.put("wordDates", stringArray(wordDates)); obj.put("unlockedBadgeIds", stringArray(unlockedBadgeIds)); obj.put("awardedKeys", stringArray(awardedKeys));
        return obj;
    }

    private void putArray(JSONObject obj, String key, List<?> list) throws JSONException {
        JSONArray arr = new JSONArray();
        for (Object item : list) {
            if (item instanceof Book) arr.put(((Book)item).toJson());
            else if (item instanceof ExerciseRecord) arr.put(((ExerciseRecord)item).toJson());
            else if (item instanceof WeightRecord) arr.put(((WeightRecord)item).toJson());
            else if (item instanceof SleepRecord) arr.put(((SleepRecord)item).toJson());
            else if (item instanceof DiaryRecord) arr.put(((DiaryRecord)item).toJson());
            else if (item instanceof WordEntry) arr.put(((WordEntry)item).toJson());
            else if (item instanceof ExperienceLog) arr.put(((ExperienceLog)item).toJson());
        }
        obj.put(key, arr);
    }
    private JSONArray stringArray(List<String> src) { JSONArray arr = new JSONArray(); for (String s : src) arr.put(s); return arr; }
    private static List<String> readStringArray(JSONObject obj, String key) { List<String> list = new ArrayList<>(); JSONArray arr = obj.optJSONArray(key); if (arr != null) for (int i=0;i<arr.length();i++) list.add(arr.optString(i)); return list; }

    public static AppState fromJson(String raw) {
        AppState state = new AppState();
        if (raw == null || raw.trim().isEmpty()) return state;
        try {
            JSONObject obj = new JSONObject(raw);
            JSONArray books = obj.optJSONArray("books"); if (books != null) for (int i=0;i<books.length();i++) { JSONObject item=books.optJSONObject(i); if (item!=null) state.books.add(Book.fromJson(item)); }
            JSONArray ex = obj.optJSONArray("exercises"); if (ex != null) for (int i=0;i<ex.length();i++) { JSONObject item=ex.optJSONObject(i); if (item!=null) state.exercises.add(ExerciseRecord.fromJson(item)); }
            JSONArray w = obj.optJSONArray("weights"); if (w != null) for (int i=0;i<w.length();i++) { JSONObject item=w.optJSONObject(i); if (item!=null) state.weights.add(WeightRecord.fromJson(item)); }
            JSONArray s = obj.optJSONArray("sleeps"); if (s != null) for (int i=0;i<s.length();i++) { JSONObject item=s.optJSONObject(i); if (item!=null) state.sleeps.add(SleepRecord.fromJson(item)); }
            JSONArray d = obj.optJSONArray("diaries"); if (d != null) for (int i=0;i<d.length();i++) { JSONObject item=d.optJSONObject(i); if (item!=null) state.diaries.add(DiaryRecord.fromJson(item)); }
            JSONArray words = obj.optJSONArray("words"); if (words != null) for (int i=0;i<words.length();i++) { JSONObject item=words.optJSONObject(i); if (item!=null) state.words.add(WordEntry.fromJson(item)); }
            JSONArray logs = obj.optJSONArray("expLogs"); if (logs != null) for (int i=0;i<logs.length();i++) { JSONObject item=logs.optJSONObject(i); if (item!=null) state.expLogs.add(ExperienceLog.fromJson(item)); }
            state.readingDates = readStringArray(obj, "readingDates"); state.wordDates = readStringArray(obj, "wordDates"); state.unlockedBadgeIds = readStringArray(obj, "unlockedBadgeIds"); state.awardedKeys = readStringArray(obj, "awardedKeys");
        } catch (Exception ignored) { return new AppState(); }
        return state;
    }
}

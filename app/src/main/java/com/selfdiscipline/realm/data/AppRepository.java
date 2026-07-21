package com.selfdiscipline.realm.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.selfdiscipline.realm.model.AppState;
import com.selfdiscipline.realm.model.Book;
import com.selfdiscipline.realm.model.DiaryRecord;
import com.selfdiscipline.realm.model.ExerciseRecord;
import com.selfdiscipline.realm.model.ExperienceLog;
import com.selfdiscipline.realm.model.FuturesIncomeRecord;
import com.selfdiscipline.realm.model.SleepRecord;
import com.selfdiscipline.realm.model.WeightRecord;
import com.selfdiscipline.realm.engine.ReadingProgressEngine;
import com.selfdiscipline.realm.engine.RewardEngine;
import com.selfdiscipline.realm.util.DateUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class AppRepository {
    private static final String PREF_NAME = "self_discipline_realm_store";
    private static final String KEY_STATE = "app_state_v2";
    private static final String BACKUP_APP = "SelfDisciplineRealm";
    private static final int ZIP_BACKUP_VERSION = 6;
    private final SharedPreferences prefs;
    private final Context appContext;
    private AppState cachedState;

    public AppRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public AppState load() {
        // 每次从 SharedPreferences 重新读取，确保独立编辑页或详情页保存后，
        // 返回原页面能够立即拿到最新数据，而不是命中当前 Repository 的旧缓存。
        cachedState = AppState.fromJson(prefs.getString(KEY_STATE, ""));

        boolean changed = ReadingProgressEngine.backfillMissingHistory(
                cachedState,
                DateUtils.now()
        );
        if (RewardEngine.ensureDefaultNoBreak(
                appContext,
                cachedState,
                DateUtils.today()
        )) {
            changed = true;
        }
        if (RewardEngine.reconcileBadges(
                appContext,
                cachedState,
                DateUtils.today()
        )) {
            changed = true;
        }

        if (changed) {
            persist(cachedState);
        }
        return cachedState;
    }

    public void save(AppState state) {
        if (state == null) return;
        RewardEngine.reconcileBadges(appContext, state, DateUtils.today());
        persist(state);
    }

    private void persist(AppState state) {
        try {
            cachedState = state;
            prefs.edit().putString(KEY_STATE, state.toJson().toString()).apply();
        } catch (Exception ignored) {
        }
    }

    /**
     * v0.6: export a ZIP backup package instead of one giant JSON file.
     * The package is split by module, and cover images are copied into covers/ when readable.
     */
    public void exportBackupZip(Context context, OutputStream outputStream) throws Exception {
        AppState state = load();
        JSONObject full = state.toJson();
        full.remove("words");
        full.remove("wordDates");
        JSONObject metadata = new JSONObject();
        metadata.put("readingDates", full.optJSONArray("readingDates"));
        metadata.put("unlockedBadgeIds", full.optJSONArray("unlockedBadgeIds"));
        metadata.put("awardedKeys", full.optJSONArray("awardedKeys"));
        metadata.put("badgeUnlockCounts", full.optJSONObject("badgeUnlockCounts"));

        JSONObject counts = new JSONObject();
        counts.put("books", state.books.size());
        counts.put("exercises", state.exercises.size());
        counts.put("weights", state.weights.size());
        counts.put("futuresIncomes", state.futuresIncomes.size());
        counts.put("sleeps", state.sleeps.size());
        counts.put("diaries", state.diaries.size());
        counts.put("expLogs", state.expLogs.size());

        JSONObject coverMap = new JSONObject();
        JSONObject manifest = new JSONObject();
        manifest.put("app", BACKUP_APP);
        manifest.put("backupVersion", ZIP_BACKUP_VERSION);
        manifest.put("format", "zip_modules");
        manifest.put("exportedAt", DateUtils.now());
        manifest.put("counts", counts);

        ZipOutputStream zip = new ZipOutputStream(outputStream);
        writeJsonEntry(zip, "books.json", full.optJSONArray("books"));
        writeJsonEntry(zip, "exercises.json", full.optJSONArray("exercises"));
        writeJsonEntry(zip, "weights.json", full.optJSONArray("weights"));
        writeJsonEntry(zip, "futures_incomes.json", full.optJSONArray("futuresIncomes"));
        writeJsonEntry(zip, "sleeps.json", full.optJSONArray("sleeps"));
        writeJsonEntry(zip, "diaries.json", full.optJSONArray("diaries"));
        writeJsonEntry(zip, "exp_logs.json", full.optJSONArray("expLogs"));
        writeJsonEntry(zip, "metadata.json", metadata);
        writeJsonEntry(zip, "all_state.json", full); // compatibility fallback for future migrations

        for (Book book : state.books) {
            if (book.coverUri == null || book.coverUri.trim().isEmpty()) continue;
            String path = "covers/" + safeName(book.id) + ".cover";
            if (writeBinaryFromUri(context, zip, path, book.coverUri)) coverMap.put(book.id, path);
        }
        manifest.put("covers", coverMap);
        writeJsonEntry(zip, "manifest.json", manifest);
        zip.finish();
        zip.flush();
    }


    public void importBackupAuto(Context context, InputStream inputStream) throws Exception {
        byte[] data = readAllBytes(inputStream);
        if (data.length >= 2 && data[0] == 'P' && data[1] == 'K') {
            importBackupZip(context, new ByteArrayInputStream(data));
        } else {
            importBackupJson(new String(data, StandardCharsets.UTF_8));
        }
    }

    public void importBackupZip(Context context, InputStream inputStream) throws Exception {
        Map<String, byte[]> entries = readZipEntries(inputStream);
        if (!entries.containsKey("manifest.json") && !entries.containsKey("all_state.json")) {
            throw new IllegalArgumentException("Invalid zip backup package");
        }

        AppState imported;
        if (entries.containsKey("all_state.json")) {
            imported = AppState.fromJson(text(entries.get("all_state.json")));
        } else {
            imported = buildStateFromModules(entries);
        }
        imported.words.clear();
        imported.wordDates.clear();
        if (!looksLikeState(imported.toJson())) throw new IllegalArgumentException("Invalid backup state");
        restoreCoverFiles(context, imported, entries);
        RewardEngine.reconcileBadges(appContext, imported, DateUtils.today());
        persist(imported);
    }

    /** Backward-compatible JSON import kept for old v0.5 backup files. */
    public void importBackupJson(String raw) throws Exception {
        JSONObject root = new JSONObject(raw);
        JSONObject stateObj = root.optJSONObject("state");
        if (stateObj == null) stateObj = root;
        if (!looksLikeState(stateObj)) throw new IllegalArgumentException("Invalid backup file");
        AppState imported = AppState.fromJson(stateObj.toString());
        imported.words.clear();
        imported.wordDates.clear();
        RewardEngine.reconcileBadges(appContext, imported, DateUtils.today());
        persist(imported);
    }

    private void writeJsonEntry(ZipOutputStream zip, String name, Object json) throws Exception {
        ZipEntry entry = new ZipEntry(name);
        zip.putNextEntry(entry);
        String content;
        if (json instanceof JSONObject) content = ((JSONObject) json).toString(2);
        else if (json instanceof JSONArray) content = ((JSONArray) json).toString(2);
        else content = "{}";
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private boolean writeBinaryFromUri(Context context, ZipOutputStream zip, String entryName, String uriText) {
        try {
            InputStream in = context.getContentResolver().openInputStream(Uri.parse(uriText));
            if (in == null) return false;
            zip.putNextEntry(new ZipEntry(entryName));
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) zip.write(buf, 0, n);
            in.close();
            zip.closeEntry();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private byte[] readAllBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = inputStream.read(chunk)) != -1) out.write(chunk, 0, n);
        inputStream.close();
        return out.toByteArray();
    }

    private Map<String, byte[]> readZipEntries(InputStream inputStream) throws Exception {
        Map<String, byte[]> map = new HashMap<>();
        ZipInputStream zip = new ZipInputStream(inputStream);
        ZipEntry entry;
        byte[] chunk = new byte[8192];
        while ((entry = zip.getNextEntry()) != null) {
            if (entry.isDirectory()) continue;
            String name = entry.getName();
            if (name.contains("..") || name.startsWith("/") || name.startsWith("\\")) {
                throw new IllegalArgumentException("Unsafe zip entry");
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int n;
            while ((n = zip.read(chunk)) != -1) out.write(chunk, 0, n);
            map.put(name, out.toByteArray());
            zip.closeEntry();
        }
        zip.close();
        return map;
    }

    private AppState buildStateFromModules(Map<String, byte[]> entries) throws Exception {
        AppState state = new AppState();
        JSONArray books = array(entries, "books.json");
        for (int i = 0; i < books.length(); i++) { JSONObject item = books.optJSONObject(i); if (item != null) state.books.add(Book.fromJson(item)); }
        JSONArray exercises = array(entries, "exercises.json");
        for (int i = 0; i < exercises.length(); i++) { JSONObject item = exercises.optJSONObject(i); if (item != null) state.exercises.add(ExerciseRecord.fromJson(item)); }
        JSONArray weights = array(entries, "weights.json");
        for (int i = 0; i < weights.length(); i++) { JSONObject item = weights.optJSONObject(i); if (item != null) state.weights.add(WeightRecord.fromJson(item)); }
        JSONArray futures = array(entries, "futures_incomes.json");
        for (int i = 0; i < futures.length(); i++) { JSONObject item = futures.optJSONObject(i); if (item != null) state.futuresIncomes.add(FuturesIncomeRecord.fromJson(item)); }
        JSONArray sleeps = array(entries, "sleeps.json");
        for (int i = 0; i < sleeps.length(); i++) { JSONObject item = sleeps.optJSONObject(i); if (item != null) state.sleeps.add(SleepRecord.fromJson(item)); }
        JSONArray diaries = array(entries, "diaries.json");
        for (int i = 0; i < diaries.length(); i++) { JSONObject item = diaries.optJSONObject(i); if (item != null) state.diaries.add(DiaryRecord.fromJson(item)); }
        JSONArray logs = array(entries, "exp_logs.json");
        for (int i = 0; i < logs.length(); i++) { JSONObject item = logs.optJSONObject(i); if (item != null) state.expLogs.add(ExperienceLog.fromJson(item)); }
        JSONObject metadata = object(entries, "metadata.json");
        readStringArray(metadata, "readingDates", state.readingDates);
        readStringArray(metadata, "unlockedBadgeIds", state.unlockedBadgeIds);
        readStringArray(metadata, "awardedKeys", state.awardedKeys);
        JSONObject badgeCounts = metadata.optJSONObject("badgeUnlockCounts");
        if (badgeCounts != null) {
            java.util.Iterator<String> keys = badgeCounts.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                state.setBadgeUnlockCount(key, badgeCounts.optInt(key, 0));
            }
        }
        return state;
    }

    private void restoreCoverFiles(Context context, AppState state, Map<String, byte[]> entries) {
        File dir = new File(context.getFilesDir(), "covers");
        if (!dir.exists()) dir.mkdirs();
        for (Book book : state.books) {
            String name = "covers/" + safeName(book.id) + ".cover";
            byte[] data = entries.get(name);
            if (data == null || data.length == 0) continue;
            try {
                File out = new File(dir, safeName(book.id) + ".cover");
                FileOutputStream fos = new FileOutputStream(out, false);
                fos.write(data);
                fos.flush();
                fos.close();
                book.coverUri = Uri.fromFile(out).toString();
            } catch (Exception ignored) {}
        }
    }

    private JSONArray array(Map<String, byte[]> entries, String name) throws Exception {
        byte[] data = entries.get(name);
        if (data == null) return new JSONArray();
        return new JSONArray(text(data));
    }

    private JSONObject object(Map<String, byte[]> entries, String name) throws Exception {
        byte[] data = entries.get(name);
        if (data == null) return new JSONObject();
        return new JSONObject(text(data));
    }

    private void readStringArray(JSONObject obj, String key, java.util.List<String> target) {
        JSONArray arr = obj.optJSONArray(key);
        if (arr == null) return;
        for (int i = 0; i < arr.length(); i++) {
            String value = arr.optString(i);
            if (value != null && value.length() > 0 && !target.contains(value)) target.add(value);
        }
    }

    private String text(byte[] data) { return new String(data, StandardCharsets.UTF_8); }

    private String safeName(String raw) {
        if (raw == null || raw.length() == 0) return "empty";
        return raw.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private boolean looksLikeState(JSONObject obj) {
        return obj.has("books") || obj.has("exercises") || obj.has("weights") || obj.has("futuresIncomes") || obj.has("sleeps")
                || obj.has("diaries") || obj.has("expLogs")
                || obj.has("readingDates") || obj.has("unlockedBadgeIds")
                || obj.has("awardedKeys") || obj.has("badgeUnlockCounts");
    }
}

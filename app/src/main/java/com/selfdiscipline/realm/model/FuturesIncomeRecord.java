package com.selfdiscipline.realm.model;

import org.json.JSONException;
import org.json.JSONObject;

public class FuturesIncomeRecord {
    public String id;
    public String dateTime;
    public int amount;
    public String experienceKey;
    public String reflection;

    public FuturesIncomeRecord(String id, String dateTime, int amount, String experienceKey) {
        this(id, dateTime, amount, experienceKey, "");
    }

    public FuturesIncomeRecord(
            String id,
            String dateTime,
            int amount,
            String experienceKey,
            String reflection
    ) {
        this.id = id;
        this.dateTime = dateTime;
        this.amount = amount;
        this.experienceKey = experienceKey;
        this.reflection = reflection == null ? "" : reflection;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("dateTime", dateTime);
        object.put("amount", amount);
        object.put("experienceKey", experienceKey);
        object.put("reflection", reflection == null ? "" : reflection);
        return object;
    }

    public static FuturesIncomeRecord fromJson(JSONObject object) {
        String id = object.optString("id");
        String dateTime = object.optString("dateTime", object.optString("date"));
        int amount = object.optInt("amount");
        String experienceKey = object.optString("experienceKey");
        String reflection = object.optString(
                "reflection",
                object.optString("note", object.optString("thoughts", ""))
        );
        if (experienceKey == null || experienceKey.trim().isEmpty()) {
            experienceKey = "futures_income_" + id;
        }
        return new FuturesIncomeRecord(id, dateTime, amount, experienceKey, reflection);
    }
}

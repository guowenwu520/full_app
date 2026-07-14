package com.selfdiscipline.realm.model;

import org.json.JSONException;
import org.json.JSONObject;

public class FuturesIncomeRecord {
    public String id;
    public String dateTime;
    public int amount;
    public String experienceKey;

    public FuturesIncomeRecord(String id, String dateTime, int amount, String experienceKey) {
        this.id = id;
        this.dateTime = dateTime;
        this.amount = amount;
        this.experienceKey = experienceKey;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("dateTime", dateTime);
        object.put("amount", amount);
        object.put("experienceKey", experienceKey);
        return object;
    }

    public static FuturesIncomeRecord fromJson(JSONObject object) {
        String id = object.optString("id");
        String dateTime = object.optString("dateTime", object.optString("date"));
        int amount = object.optInt("amount");
        String experienceKey = object.optString("experienceKey");
        if (experienceKey == null || experienceKey.trim().isEmpty()) {
            experienceKey = "futures_income_" + id;
        }
        return new FuturesIncomeRecord(id, dateTime, amount, experienceKey);
    }
}

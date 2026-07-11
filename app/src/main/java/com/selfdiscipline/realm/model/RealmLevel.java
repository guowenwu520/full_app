package com.selfdiscipline.realm.model;

public class RealmLevel {
    public static final String MAJOR_QI = "qi";
    public static final String MAJOR_FOUNDATION = "foundation";
    public static final String MAJOR_CORE = "core";
    public static final String MAJOR_SOUL = "soul";
    public static final String MAJOR_SPIRIT = "spirit";
    public final int nameRes;
    public final int descRes;
    public final int minXp;
    public final int nextXp;
    public final String major;
    public RealmLevel(int nameRes, int descRes, int minXp, int nextXp, String major) {
        this.nameRes=nameRes; this.descRes=descRes; this.minXp=minXp; this.nextXp=nextXp; this.major=major;
    }
    public boolean isCap() { return nextXp <= minXp; }
}

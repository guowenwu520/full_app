package com.selfdiscipline.realm.model;

public class RealmLevel {
    public static final String MAJOR_QI = "qi";
    public static final String MAJOR_FOUNDATION = "foundation";
    public static final String MAJOR_CORE = "core";
    public static final String MAJOR_SOUL = "soul";
    public static final String MAJOR_SPIRIT = "spirit";
    public static final String MAJOR_VOID = "void";
    public static final String MAJOR_UNITY = "unity";
    public static final String MAJOR_ASCENSION = "ascension";
    public static final String MAJOR_TRUE_IMMORTAL = "true_immortal";
    public static final String MAJOR_GOLDEN_IMMORTAL = "golden_immortal";
    public static final String MAJOR_TAIYI = "taiyi";
    public static final String MAJOR_DALUO = "daluo";
    public static final String MAJOR_DAOZU = "daozu";

    public final int nameRes;
    public final int descRes;
    public final int minXp;
    public final int nextXp;
    public final String major;

    public RealmLevel(int nameRes, int descRes, int minXp, int nextXp, String major) {
        this.nameRes = nameRes;
        this.descRes = descRes;
        this.minXp = minXp;
        this.nextXp = nextXp;
        this.major = major;
    }

    public boolean isCap() {
        return nextXp <= minXp;
    }
}

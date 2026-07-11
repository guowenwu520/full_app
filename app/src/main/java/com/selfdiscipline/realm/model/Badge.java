package com.selfdiscipline.realm.model;

public class Badge {
    public static final String TYPE_SELF = "self";
    public static final String TYPE_WEIGHT = "weight";
    public static final String TYPE_CALORIE = "calorie";
    public static final String TYPE_NOBREAK = "nobreak";
    public static final String TYPE_WORD = "word";

    public final String id;
    public final int nameRes;
    public final int descRes;
    public final String type;
    public final int rankRes;
    public final int target;
    public final int xpReward;

    public Badge(String id, int nameRes, int descRes, String type, int rankRes, int target, int xpReward) {
        this.id=id; this.nameRes=nameRes; this.descRes=descRes; this.type=type; this.rankRes=rankRes; this.target=target; this.xpReward=xpReward;
    }
}

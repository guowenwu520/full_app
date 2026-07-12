package com.selfdiscipline.realm.data;

import com.selfdiscipline.realm.R;
import com.selfdiscipline.realm.model.Badge;
import java.util.ArrayList;
import java.util.List;

public class BadgeCatalog {
    private static final int BRONZE = 80;
    private static final int SILVER = 150;
    private static final int GOLD = 300;
    private static final int PLATINUM = 600;
    private static final int DIAMOND = 1200;

    public static List<Badge> all() {
        List<Badge> b = new ArrayList<>();
        addSet(b, Badge.TYPE_SELF, R.string.badge_desc_self, new int[]{1,7,30,90,180}, new int[]{R.string.badge_self_tier_silver,R.string.badge_self_tier_gold,R.string.badge_self_tier_platinum,R.string.badge_self_tier_diamond,R.string.badge_self_tier_king});
        addSet(b, Badge.TYPE_WEIGHT, R.string.badge_desc_weight, new int[]{1,3,5,8,10}, new int[]{R.string.badge_weight_tier_silver,R.string.badge_weight_tier_gold,R.string.badge_weight_tier_platinum,R.string.badge_weight_tier_diamond,R.string.badge_weight_tier_king});
        addSet(b, Badge.TYPE_CALORIE, R.string.badge_desc_calorie, new int[]{500,2000,10000,50000,100000}, new int[]{R.string.badge_cal_tier_silver,R.string.badge_cal_tier_gold,R.string.badge_cal_tier_platinum,R.string.badge_cal_tier_diamond,R.string.badge_cal_tier_king});
        addSet(b, Badge.TYPE_NOBREAK, R.string.badge_desc_nobreak, new int[]{1,7,30,90,180}, new int[]{R.string.badge_nobreak_tier_silver,R.string.badge_nobreak_tier_gold,R.string.badge_nobreak_tier_platinum,R.string.badge_nobreak_tier_diamond,R.string.badge_nobreak_tier_king});
        addSet(b, Badge.TYPE_WORD, R.string.badge_desc_word, new int[]{10,50,200,500,1000}, new int[]{R.string.badge_word_tier_silver,R.string.badge_word_tier_gold,R.string.badge_word_tier_platinum,R.string.badge_word_tier_diamond,R.string.badge_word_tier_king});
        return b;
    }
    private static void addSet(List<Badge> list, String type, int desc, int[] targets, int[] names) {
        int[] ranks = {R.string.rank_silver, R.string.rank_gold, R.string.rank_platinum, R.string.rank_diamond, R.string.rank_king}; int[] rewards = {BRONZE,SILVER,GOLD,PLATINUM,DIAMOND};
        for (int i=0;i<targets.length;i++) list.add(new Badge(type+"_"+i, names[i], desc, type, ranks[i], targets[i], rewards[i]));
    }
}

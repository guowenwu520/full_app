package com.selfdiscipline.realm.data;

import com.selfdiscipline.realm.R;
import com.selfdiscipline.realm.model.Badge;

import java.util.ArrayList;
import java.util.List;

public class BadgeCatalog {
    private static final int SILVER_REWARD = 80;
    private static final int GOLD_REWARD = 150;
    private static final int PLATINUM_REWARD = 300;
    private static final int DIAMOND_REWARD = 600;
    private static final int KING_REWARD = 1200;

    public static List<Badge> all() {
        List<Badge> badges = new ArrayList<>();
        addSet(badges, Badge.TYPE_SELF, R.string.badge_desc_self,
                new int[]{3, 7, 15, 21, 30},
                new int[]{R.string.badge_self_tier_silver, R.string.badge_self_tier_gold, R.string.badge_self_tier_platinum, R.string.badge_self_tier_diamond, R.string.badge_self_tier_king});
        addSet(badges, Badge.TYPE_WEIGHT, R.string.badge_desc_weight,
                new int[]{2, 5, 10, 15, 20},
                new int[]{R.string.badge_weight_tier_silver, R.string.badge_weight_tier_gold, R.string.badge_weight_tier_platinum, R.string.badge_weight_tier_diamond, R.string.badge_weight_tier_king});
        addSet(badges, Badge.TYPE_CALORIE, R.string.badge_desc_calorie,
                new int[]{5000, 20000, 50000, 100000, 10000000},
                new int[]{R.string.badge_cal_tier_silver, R.string.badge_cal_tier_gold, R.string.badge_cal_tier_platinum, R.string.badge_cal_tier_diamond, R.string.badge_cal_tier_king});
        addSet(badges, Badge.TYPE_NOBREAK, R.string.badge_desc_nobreak,
                new int[]{3, 7, 15, 21, 30},
                new int[]{R.string.badge_nobreak_tier_silver, R.string.badge_nobreak_tier_gold, R.string.badge_nobreak_tier_platinum, R.string.badge_nobreak_tier_diamond, R.string.badge_nobreak_tier_king});
        addSet(badges, Badge.TYPE_FUTURES, R.string.badge_desc_futures,
                new int[]{10000, 100000, 500000, 1000000, 5000000},
                new int[]{R.string.badge_futures_tier_silver, R.string.badge_futures_tier_gold, R.string.badge_futures_tier_platinum, R.string.badge_futures_tier_diamond, R.string.badge_futures_tier_king});
        return badges;
    }

    private static void addSet(List<Badge> list, String type, int desc, int[] targets, int[] names) {
        int[] ranks = {R.string.rank_silver, R.string.rank_gold, R.string.rank_platinum, R.string.rank_diamond, R.string.rank_king};
        int[] rewards = {SILVER_REWARD, GOLD_REWARD, PLATINUM_REWARD, DIAMOND_REWARD, KING_REWARD};
        for (int i = 0; i < targets.length; i++) {
            list.add(new Badge(type + "_" + i, names[i], desc, type, ranks[i], targets[i], rewards[i]));
        }
    }
}

package com.selfdiscipline.realm.ui;

import com.selfdiscipline.realm.R;

public final class BadgeIconResolver {
    public static final int CATEGORY_DISCIPLINE = 0;
    public static final int CATEGORY_WEIGHT = 1;
    public static final int CATEGORY_CALORIES = 2;
    public static final int CATEGORY_NOBREAK = 3;
    public static final int CATEGORY_FUTURES = 4;
    public static final int CATEGORY_WORDS = CATEGORY_FUTURES;

    /**
     * 五个等级统一使用用户提供的勋章素材：
     * 白银、黄金、铂金、钻石、王者。
     *
     * category 参数继续保留，避免影响现有调用和业务结构。
     */
    private static final int[] TIER_ICONS = {
            R.drawable.medal_tier_silver,
            R.drawable.medal_tier_gold,
            R.drawable.medal_tier_platinum,
            R.drawable.medal_tier_diamond,
            R.drawable.medal_tier_king
    };

    private BadgeIconResolver() {}

    public static int medalIcon(int category, int tier) {
        int safeTier = clamp(tier, 0, TIER_ICONS.length - 1);
        return TIER_ICONS[safeTier];
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) return min;
        return Math.min(value, max);
    }
}

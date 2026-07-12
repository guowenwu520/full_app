package com.selfdiscipline.realm.ui;

import com.selfdiscipline.realm.R;

public final class BadgeIconResolver {
    public static final int CATEGORY_DISCIPLINE = 0;
    public static final int CATEGORY_WEIGHT = 1;
    public static final int CATEGORY_CALORIES = 2;
    public static final int CATEGORY_NOBREAK = 3;
    public static final int CATEGORY_WORDS = 4;

    private static final int[][] ICONS = {
            {R.drawable.ic_medal_discipline_bronze, R.drawable.ic_medal_discipline_silver, R.drawable.ic_medal_discipline_gold, R.drawable.ic_medal_discipline_platinum, R.drawable.ic_medal_discipline_diamond},
            {R.drawable.ic_medal_weight_bronze, R.drawable.ic_medal_weight_silver, R.drawable.ic_medal_weight_gold, R.drawable.ic_medal_weight_platinum, R.drawable.ic_medal_weight_diamond},
            {R.drawable.ic_medal_calories_bronze, R.drawable.ic_medal_calories_silver, R.drawable.ic_medal_calories_gold, R.drawable.ic_medal_calories_platinum, R.drawable.ic_medal_calories_diamond},
            {R.drawable.ic_medal_nobreak_bronze, R.drawable.ic_medal_nobreak_silver, R.drawable.ic_medal_nobreak_gold, R.drawable.ic_medal_nobreak_platinum, R.drawable.ic_medal_nobreak_diamond},
            {R.drawable.ic_medal_words_bronze, R.drawable.ic_medal_words_silver, R.drawable.ic_medal_words_gold, R.drawable.ic_medal_words_platinum, R.drawable.ic_medal_words_diamond}
    };

    private BadgeIconResolver() {}

    public static int medalIcon(int category, int tier) {
        int safeCategory = clamp(category, 0, ICONS.length - 1);
        int safeTier = clamp(tier, 0, ICONS[safeCategory].length - 1);
        return ICONS[safeCategory][safeTier];
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) return min;
        return Math.min(value, max);
    }
}

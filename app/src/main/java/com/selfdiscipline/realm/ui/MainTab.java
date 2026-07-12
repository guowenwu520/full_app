package com.selfdiscipline.realm.ui;

import android.app.Fragment;

import com.selfdiscipline.realm.R;
import com.selfdiscipline.realm.fragments.AchievementRealmFragment;
import com.selfdiscipline.realm.fragments.DiaryFragment;
import com.selfdiscipline.realm.fragments.ExerciseWordFragment;
import com.selfdiscipline.realm.fragments.OverviewFragment;
import com.selfdiscipline.realm.fragments.ReadingFragment;

public enum MainTab {
    OVERVIEW(0, R.id.nav_overview, R.id.nav_overview_image, R.id.nav_overview_text, R.drawable.ic_nav_home),
    READING(1, R.id.nav_reading, R.id.nav_reading_image, R.id.nav_reading_text, R.drawable.ic_nav_reading),
    STUDY(2, R.id.nav_study, R.id.nav_study_image, R.id.nav_study_text, R.drawable.ic_nav_exercise_word),
    DIARY(3, R.id.nav_diary, R.id.nav_diary_image, R.id.nav_diary_text, R.drawable.ic_nav_diary),
    BADGES(4, R.id.nav_badges, R.id.nav_badges_image, R.id.nav_badges_text, R.drawable.ic_nav_medal_realm);

    public final int index;
    public final int containerId;
    public final int iconId;
    public final int textId;
    public final int iconRes;

    MainTab(int index, int containerId, int iconId, int textId, int iconRes) {
        this.index = index;
        this.containerId = containerId;
        this.iconId = iconId;
        this.textId = textId;
        this.iconRes = iconRes;
    }

    public Fragment newFragment() {
        switch (this) {
            case READING:
                return new ReadingFragment();
            case STUDY:
                return new ExerciseWordFragment();
            case DIARY:
                return new DiaryFragment();
            case BADGES:
                return new AchievementRealmFragment();
            case OVERVIEW:
            default:
                return new OverviewFragment();
        }
    }
}

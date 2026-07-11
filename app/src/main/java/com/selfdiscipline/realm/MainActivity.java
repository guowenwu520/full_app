package com.selfdiscipline.realm;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.selfdiscipline.realm.fragments.BadgesRealmFragment;
import com.selfdiscipline.realm.fragments.DiaryFragment;
import com.selfdiscipline.realm.fragments.OverviewFragment;
import com.selfdiscipline.realm.fragments.ReadingFragment;
import com.selfdiscipline.realm.fragments.AchievementRealmFragment;
import com.selfdiscipline.realm.fragments.ExerciseWordFragment;

public class MainActivity extends Activity {
    private LinearLayout navOverview, navReading, navStudy, navDiary, navBadges;
    private ImageView navOverviewImage, navReadingImage, navStudyImage, navDiaryImage, navBadgesImage;
    private TextView navOverviewText, navReadingText, navStudyText, navDiaryText, navBadgesText;
    private int [] image_ids_sel ={R.drawable.ic_nav_home,R.drawable.ic_nav_reading,R.drawable.ic_nav_exercise_word,R.drawable.ic_nav_diary,R.drawable.ic_nav_medal_realm};
    private int [] image_ids_no_sel ={R.drawable.ic_nav_home_inactive,R.drawable.ic_nav_reading_inactive,R.drawable.ic_nav_exercise_word_inactive,R.drawable.ic_nav_diary_inactive,R.drawable.ic_nav_medal_realm_inactive};
    private int currentTab = -1;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(getResources().getColor(R.color.color_surface));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(R.layout.activity_main);
        navOverview = findViewById(R.id.nav_overview);
        navReading = findViewById(R.id.nav_reading);
        navStudy = findViewById(R.id.nav_study);
        navDiary = findViewById(R.id.nav_diary);
        navBadges = findViewById(R.id.nav_badges);

        navOverviewImage = findViewById(R.id.nav_overview_image);
        navReadingImage = findViewById(R.id.nav_reading_image);
        navStudyImage = findViewById(R.id.nav_study_image);
        navDiaryImage = findViewById(R.id.nav_diary_image);
        navBadgesImage = findViewById(R.id.nav_badges_image);

        navOverviewText = findViewById(R.id.nav_overview_text);
        navReadingText = findViewById(R.id.nav_reading_text);
        navStudyText = findViewById(R.id.nav_study_text);
        navDiaryText = findViewById(R.id.nav_diary_text);
        navBadgesText = findViewById(R.id.nav_badges_text);

        navOverview.setOnClickListener(v -> switchTo(0));
        navReading.setOnClickListener(v -> switchTo(1));
        navStudy.setOnClickListener(v -> switchTo(2));
        navDiary.setOnClickListener(v -> switchTo(3));
        navBadges.setOnClickListener(v -> switchTo(4));
        switchTo(0);
    }

    private void switchTo(int tab) {
        if (currentTab == tab) return;
        currentTab = tab;
        Fragment f;
        if (tab == 1) f = new ReadingFragment();
        else if (tab == 2) f = new ExerciseWordFragment();
        else if (tab == 3) f = new DiaryFragment();
        else if (tab == 4) f = new AchievementRealmFragment();
        else f = new OverviewFragment();
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, f).commit();
        updateNav();
    }

    private void updateNav() {
        set(navOverviewImage,navOverviewText, currentTab == 0,0);
        set(navReadingImage, navReadingText,currentTab == 1,1);
        set(navStudyImage,navStudyText, currentTab == 2,2);
        set(navDiaryImage,navDiaryText, currentTab == 3,3);
        set(navBadgesImage,navBadgesText, currentTab == 4,4);
    }

    private void set(ImageView imageView,TextView textView, boolean selected,int index) {
        imageView.setImageResource(selected ? image_ids_sel[index] : image_ids_no_sel[index] );
        textView.setTextColor(getResources().getColor(selected ? R.color.color_primary_dark : R.color.color_text_sub));
    }
}

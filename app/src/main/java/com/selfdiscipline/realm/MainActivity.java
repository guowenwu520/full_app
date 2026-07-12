package com.selfdiscipline.realm;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.selfdiscipline.realm.ui.MainTab;

public class MainActivity extends Activity {
    private MainTab currentTab;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(getResources().getColor(R.color.color_surface));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(R.layout.activity_main);
        bindNavigation();
        switchTo(MainTab.OVERVIEW);
    }

    private void bindNavigation() {
        for (MainTab tab : MainTab.values()) {
            View item = findViewById(tab.containerId);
            ImageView icon = findViewById(tab.iconId);
            icon.setImageResource(tab.iconRes);
            item.setOnClickListener(v -> switchTo(tab));
        }
    }

    private void switchTo(MainTab tab) {
        if (currentTab == tab) return;
        currentTab = tab;
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, tab.newFragment())
                .commit();
        updateNavigationState();
    }

    private void updateNavigationState() {
        int selectedColor = getResources().getColor(R.color.nav_icon_selected);
        int normalColor = getResources().getColor(R.color.nav_icon_normal);
        for (MainTab tab : MainTab.values()) {
            boolean selected = currentTab == tab;
            ImageView icon = findViewById(tab.iconId);
            TextView label = findViewById(tab.textId);
            icon.setColorFilter(selected ? selectedColor : normalColor);
            label.setTextColor(selected ? selectedColor : normalColor);
            findViewById(tab.containerId).setSelected(selected);
        }
    }
}

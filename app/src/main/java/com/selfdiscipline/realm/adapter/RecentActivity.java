package com.selfdiscipline.realm.adapter;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

public class RecentActivity {

    @DrawableRes
    private final int iconRes;
    @NonNull
    private final String type;
    @NonNull
    private final String content;
    @NonNull
    private final String time;

    public RecentActivity(
            @DrawableRes int iconRes,
            @NonNull String type,
            @NonNull String content,
            @NonNull String time
    ) {
        this.iconRes = iconRes;
        this.type = type;
        this.content = content;
        this.time = time;
    }

    public int getIconRes() {
        return iconRes;
    }

    @NonNull
    public String getType() {
        return type;
    }

    @NonNull
    public String getContent() {
        return content;
    }

    @NonNull
    public String getTime() {
        return time;
    }
}

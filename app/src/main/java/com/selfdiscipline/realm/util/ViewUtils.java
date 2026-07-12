package com.selfdiscipline.realm.util;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.selfdiscipline.realm.ui.RealmDialog;

import com.selfdiscipline.realm.R;

import java.io.InputStream;

public class ViewUtils {
    public static String text(EditText e) { return e.getText().toString().trim(); }
    public static int parseInt(String s, int def) { try { return Integer.parseInt(s); } catch (Exception e) { return def; } }
    public static float parseFloat(String s, float def) { try { return Float.parseFloat(s); } catch (Exception e) { return def; } }
    public static void toast(Context c, int res) { Toast.makeText(c, res, Toast.LENGTH_SHORT).show(); }
    public static void toast(Context c, String text) { Toast.makeText(c, text, Toast.LENGTH_SHORT).show(); }

    public static TextView card(Context c, String text) {
        TextView v = baseTextCard(c, text);
        v.setBackgroundResource(R.drawable.bg_card);
        return v;
    }

    public static TextView clickableCard(Context c, String text, View.OnClickListener listener) {
        TextView v = baseTextCard(c, text);
        v.setBackgroundResource(R.drawable.bg_card);
        v.setOnClickListener(listener);
        return v;
    }

    public static TextView softButton(Context c, String text) {
        TextView v = new TextView(c);
        v.setText(text); v.setGravity(17);
        v.setTextColor(c.getResources().getColor(R.color.color_text_main));
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setBackgroundResource(R.drawable.bg_button_soft);
        v.setPadding(dp(c,12), dp(c,10), dp(c,12), dp(c,10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0,0,0,8); v.setLayoutParams(lp);
        return v;
    }

    public static LinearLayout iconCard(Context c, int iconRes, String text, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundResource(R.drawable.bg_card);
        row.setPadding(dp(c,12), dp(c,12), dp(c,12), dp(c,12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0,0,0,10); row.setLayoutParams(lp);
        ImageView icon = new ImageView(c);
        icon.setImageResource(iconRes);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(c,46), dp(c,46));
        ilp.setMargins(0,0,12,0); icon.setLayoutParams(ilp);
        row.addView(icon);
        TextView tv = new TextView(c);
        tv.setText(text); tv.setTextColor(c.getResources().getColor(R.color.color_text_main)); tv.setTextSize(15); tv.setLineSpacing(0,1.15f);
        row.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        if (listener != null) row.setOnClickListener(listener);
        return row;
    }

    private static TextView baseTextCard(Context c, String text) {
        TextView v = new TextView(c);
        v.setText(text);
        v.setTextColor(c.getResources().getColor(R.color.color_text_main));
        v.setTextSize(15);
        v.setLineSpacing(0, 1.15f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0,0,0,10); v.setLayoutParams(lp);
        v.setPadding(dp(c,14), dp(c,12), dp(c,14), dp(c,12));
        return v;
    }

    public static ImageView cover(Context c, String uri) {
        ImageView iv = new ImageView(c);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(c,72), dp(c,96));
        lp.setMargins(0,0,12,0); iv.setLayoutParams(lp);
        iv.setBackgroundResource(R.drawable.bg_card_inner);
        try { iv.setImageResource(R.drawable.cover_default_book); } catch (Throwable ignored) {}

        // v0.6.1: never let an invalid / huge / permission-lost cover URI crash the Reading page.
        // Large cover photos are decoded as small thumbnails; if anything fails we keep the soft placeholder.
        if (uri == null || uri.trim().isEmpty()) return iv;
        try {
            Uri parsed = Uri.parse(uri);
            Bitmap bmp = decodeSmallBitmap(c, parsed, dp(c, 72), dp(c, 96));
            if (bmp != null) iv.setImageBitmap(bmp);
        } catch (Throwable ignored) {
            iv.setBackgroundResource(R.drawable.bg_card_inner);
            try { iv.setImageResource(R.drawable.cover_default_book); } catch (Throwable ignored2) {}
        }
        return iv;
    }

    private static Bitmap decodeSmallBitmap(Context c, Uri uri, int reqW, int reqH) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            InputStream in1 = c.getContentResolver().openInputStream(uri);
            if (in1 == null) return null;
            BitmapFactory.decodeStream(in1, null, bounds);
            in1.close();
            int sample = 1;
            while ((bounds.outWidth / sample) > reqW * 2 || (bounds.outHeight / sample) > reqH * 2) {
                sample *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = Math.max(1, sample);
            InputStream in2 = c.getContentResolver().openInputStream(uri);
            if (in2 == null) return null;
            Bitmap bmp = BitmapFactory.decodeStream(in2, null, opts);
            in2.close();
            return bmp;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static int dp(Context c, int v) { return (int)(v * c.getResources().getDisplayMetrics().density + 0.5f); }
    public static void info(Context c, String title, String msg) { RealmDialog.showInfo(c, title, msg); }
}

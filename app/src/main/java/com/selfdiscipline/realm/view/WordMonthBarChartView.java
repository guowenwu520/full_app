package com.selfdiscipline.realm.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class WordMonthBarChartView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Integer> values = new ArrayList<>();

    public WordMonthBarChartView(Context c) { super(c); }
    public WordMonthBarChartView(Context c, AttributeSet a) { super(c, a); }
    public WordMonthBarChartView(Context c, AttributeSet a, int s) { super(c, a, s); }

    public void setValues(List<Integer> data) {
        values.clear();
        if (data != null) values.addAll(data);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        float bottom = h - dp(14);
        paint.setColor(0x3380A7B8);
        paint.setStrokeWidth(dp(1));
        canvas.drawLine(0, bottom, w, bottom, paint);

        if (values.isEmpty()) return;
        int max = 1;
        for (int v : values) max = Math.max(max, v);
        float gap = dp(2);
        float barW = Math.max(dp(2), (w - gap * (values.size() - 1)) / values.size());

        for (int i = 0; i < values.size(); i++) {
            float x = i * (barW + gap);
            float bh = (bottom - dp(4)) * values.get(i) / (float) max;
            paint.setColor(0xFF68BBD2);
            canvas.drawRoundRect(x, bottom - bh, x + barW, bottom, dp(1.5f), dp(1.5f), paint);
        }
    }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }
}

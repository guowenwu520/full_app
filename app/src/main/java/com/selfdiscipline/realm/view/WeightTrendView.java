package com.selfdiscipline.realm.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WeightTrendView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Float> values = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();

    public WeightTrendView(Context context) { super(context); }
    public WeightTrendView(Context context, AttributeSet attrs) { super(context, attrs); }
    public WeightTrendView(Context context, AttributeSet attrs, int style) { super(context, attrs, style); }

    public void setData(List<Float> weights, List<String> dates) {
        values.clear();
        labels.clear();
        if (weights != null) values.addAll(weights);
        if (dates != null) labels.addAll(dates);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        float left = dp(34), right = w - dp(8), top = dp(12), bottom = h - dp(24);

        paint.setStrokeWidth(dp(1));
        paint.setColor(0x3380A7B8);
        for (int i = 0; i < 4; i++) {
            float y = top + (bottom - top) * i / 3f;
            canvas.drawLine(left, y, right, y, paint);
        }

        if (values.isEmpty()) {
            paint.setColor(0xFF718797);
            paint.setTextSize(dp(11));
            canvas.drawText("暂无体重记录", left, (top + bottom) / 2f, paint);
            return;
        }

        float min = values.get(0), max = values.get(0);
        for (float v : values) { min = Math.min(min, v); max = Math.max(max, v); }
        if (max - min < 1f) { max += 0.5f; min -= 0.5f; }
        min -= 0.5f; max += 0.5f;

        paint.setTextSize(dp(9));
        paint.setColor(0xFF718797);
        for (int i = 0; i < 4; i++) {
            float value = max - (max - min) * i / 3f;
            float y = top + (bottom - top) * i / 3f;
            canvas.drawText(String.format(Locale.getDefault(), "%.1f", value), 0, y + dp(3), paint);
        }

        float step = values.size() <= 1 ? 0 : (right - left) / (values.size() - 1f);
        paint.setColor(0xFF1198B6);
        paint.setStrokeWidth(dp(2));

        float oldX = left, oldY = mapY(values.get(0), min, max, top, bottom);
        for (int i = 1; i < values.size(); i++) {
            float x = left + step * i;
            float y = mapY(values.get(i), min, max, top, bottom);
            canvas.drawLine(oldX, oldY, x, y, paint);
            oldX = x; oldY = y;
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(dp(8));
        for (int i = 0; i < values.size(); i++) {
            float x = left + step * i;
            float y = mapY(values.get(i), min, max, top, bottom);
            paint.setColor(0xFFFFFFFF);
            canvas.drawCircle(x, y, dp(3), paint);
            paint.setColor(0xFF1198B6);
            canvas.drawCircle(x, y, dp(2), paint);
            paint.setColor(0xFF17394F);
            canvas.drawText(String.format(Locale.getDefault(), "%.1f", values.get(i)), x - dp(7), y - dp(7), paint);
            if (i < labels.size()) {
                paint.setColor(0xFF718797);
                canvas.drawText(labels.get(i), x - dp(10), bottom + dp(16), paint);
            }
        }
    }

    private float mapY(float value, float min, float max, float top, float bottom) {
        return bottom - (value - min) / (max - min) * (bottom - top);
    }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }
}

package com.selfdiscipline.realm.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.selfdiscipline.realm.util.NumberFormatUtils;

import java.util.ArrayList;
import java.util.List;

public class IncomeTrendView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Integer> values = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();

    public IncomeTrendView(Context context) { super(context); }
    public IncomeTrendView(Context context, AttributeSet attrs) { super(context, attrs); }
    public IncomeTrendView(Context context, AttributeSet attrs, int style) { super(context, attrs, style); }

    public void setData(List<Integer> cumulativeIncome, List<String> dates) {
        values.clear();
        labels.clear();
        if (cumulativeIncome != null) values.addAll(cumulativeIncome);
        if (dates != null) labels.addAll(dates);
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int viewportWidth = getResources().getDisplayMetrics().widthPixels - Math.round(dp(44));
        int desiredWidth = Math.max(
                viewportWidth,
                Math.round(dp(64 + Math.max(1, values.size() - 1) * 72))
        );
        int measuredWidth = resolveSize(desiredWidth, widthMeasureSpec);
        int measuredHeight = resolveSize(Math.round(dp(145)), heightMeasureSpec);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float left = dp(42);
        float right = width - dp(8);
        float top = dp(12);
        float bottom = height - dp(34);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(0x3380A7B8);
        for (int i = 0; i < 4; i++) {
            float y = top + (bottom - top) * i / 3f;
            canvas.drawLine(left, y, right, y, paint);
        }

        if (values.isEmpty()) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF718797);
            paint.setTextSize(dp(11));
            canvas.drawText("暂无期货收入记录", left, (top + bottom) / 2f, paint);
            return;
        }

        int min = 0;
        int max = 0;
        for (int value : values) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        if (max == min) {
            max += 10;
            min -= 10;
        }
        int padding = Math.max(5, (max - min) / 10);
        max += padding;
        min -= padding;

        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(dp(8));
        paint.setColor(0xFF718797);
        for (int i = 0; i < 4; i++) {
            float value = max - (max - min) * i / 3f;
            float y = top + (bottom - top) * i / 3f;
            canvas.drawText(NumberFormatUtils.compact(value), 0, y + dp(3), paint);
        }

        if (min < 0 && max > 0) {
            float zeroY = mapY(0, min, max, top, bottom);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1.2f));
            paint.setColor(0x66839AA9);
            canvas.drawLine(left, zeroY, right, zeroY, paint);
        }

        float step = values.size() <= 1 ? 0 : (right - left) / (values.size() - 1f);
        float oldX = left;
        float oldY = mapY(values.get(0), min, max, top, bottom);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        for (int i = 1; i < values.size(); i++) {
            float x = left + step * i;
            float y = mapY(values.get(i), min, max, top, bottom);
            // 横轴从左到右是新到旧；比较时用左侧新值减右侧旧值判断盈亏方向。
            paint.setColor(values.get(i - 1) >= values.get(i) ? 0xFF1198B6 : 0xFFD45C5C);
            canvas.drawLine(oldX, oldY, x, y, paint);
            oldX = x;
            oldY = y;
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(dp(8));
        for (int i = 0; i < values.size(); i++) {
            float x = left + step * i;
            float y = mapY(values.get(i), min, max, top, bottom);
            int value = values.get(i);
            paint.setColor(0xFFFFFFFF);
            canvas.drawCircle(x, y, dp(3), paint);
            paint.setColor(value >= 0 ? 0xFF1198B6 : 0xFFD45C5C);
            canvas.drawCircle(x, y, dp(2), paint);
            paint.setColor(0xFF17394F);
            String valueText = NumberFormatUtils.compactSigned(value);
            canvas.drawText(valueText, x - dp(9), y - dp(7), paint);
            if (i < labels.size()) {
                paint.setColor(0xFF718797);
                String dateLabel = labels.get(i);
                float labelWidth = paint.measureText(dateLabel);
                float labelX = x - labelWidth / 2f;
                labelX = Math.max(0, Math.min(labelX, width - labelWidth));
                canvas.drawText(dateLabel, labelX, bottom + dp(18), paint);
            }
        }
    }

    private float mapY(float value, float min, float max, float top, float bottom) {
        return bottom - (value - min) / (max - min) * (bottom - top);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}

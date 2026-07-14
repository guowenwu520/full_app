package com.selfdiscipline.realm.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * UI 数值格式化工具。
 * 小数值保留常规显示，较大数值自动切换为“万 / 亿 / 万亿”，
 * 避免 TextView 因超长数字发生挤压或换行。
 */
public final class NumberFormatUtils {
    private NumberFormatUtils() {
    }

    public static String compact(int value) {
        return compact((double) value, 0);
    }

    public static String compact(long value) {
        return compact((double) value, 0);
    }

    public static String compact(double value) {
        return compact(value, isWhole(value) ? 0 : 1);
    }

    /** smallDecimals 只用于绝对值小于一万的数值。 */
    public static String compact(double value, int smallDecimals) {
        double abs = Math.abs(value);
        if (abs < 10000.0) {
            return formatSmall(value, smallDecimals);
        }
        if (abs < 100000000.0) {
            return formatScaled(value / 10000.0) + "万";
        }
        if (abs < 1000000000000.0) {
            return formatScaled(value / 100000000.0) + "亿";
        }
        return formatScaled(value / 1000000000000.0) + "万亿";
    }

    public static String compactSigned(int value) {
        if (value > 0) {
            return "+" + compact(value);
        }
        return compact(value);
    }

    public static String compactSigned(long value) {
        if (value > 0) {
            return "+" + compact(value);
        }
        return compact(value);
    }

    public static String compactSigned(double value) {
        if (value > 0) {
            return "+" + compact(value);
        }
        return compact(value);
    }

    private static String formatSmall(double value, int decimals) {
        if (decimals <= 0 || isWhole(value)) {
            return String.format(Locale.getDefault(), "%,.0f", value);
        }
        StringBuilder pattern = new StringBuilder("#,##0");
        pattern.append('.');
        for (int i = 0; i < decimals; i++) {
            pattern.append('#');
        }
        DecimalFormat format = new DecimalFormat(
                pattern.toString(),
                DecimalFormatSymbols.getInstance(Locale.getDefault())
        );
        return format.format(value);
    }

    private static String formatScaled(double value) {
        double abs = Math.abs(value);
        String pattern;
        if (abs >= 100) {
            pattern = "0";
        } else if (abs >= 10) {
            pattern = "0.#";
        } else {
            pattern = "0.##";
        }
        DecimalFormat format = new DecimalFormat(
                pattern,
                DecimalFormatSymbols.getInstance(Locale.getDefault())
        );
        return format.format(value);
    }

    private static boolean isWhole(double value) {
        return Math.abs(value - Math.rint(value)) < 0.0000001;
    }
}

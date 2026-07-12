package com.selfdiscipline.realm.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 运动记录格式化工具。
 *
 * 旧模型 ExerciseRecord 只有 date/content/calories 三个字段，
 * 因此时长和距离会被编码在 content 中。页面展示时统一从这里解析，
 * 避免不同页面把“跑步｜用时 30 分钟｜距离 5.00 公里”全部当成运动名称。
 */
public final class ExerciseFormat {
    private static final Pattern DURATION_PATTERN = Pattern.compile("用时\\s*(\\d+)\\s*分钟");
    private static final Pattern DISTANCE_PATTERN = Pattern.compile("距离\\s*([0-9]+(?:\\.[0-9]+)?)\\s*公里");

    private ExerciseFormat() {}

    public static String content(String name, int durationMinutes, double distanceKm) {
        StringBuilder builder = new StringBuilder();
        builder.append(name == null ? "" : name.trim());
        builder.append("｜用时 ").append(durationMinutes).append(" 分钟");
        if (distanceKm > 0) {
            builder.append("｜距离 ")
                    .append(String.format(Locale.getDefault(), "%.2f", distanceKm))
                    .append(" 公里");
        }
        return builder.toString();
    }

    public static String name(String content) {
        if (content == null) return "运动记录";
        String value = content.trim();
        if (value.isEmpty()) return "运动记录";
        int split = value.indexOf('｜');
        if (split < 0) split = value.indexOf('|');
        String name = split >= 0 ? value.substring(0, split).trim() : value;
        return name.isEmpty() ? "运动记录" : name;
    }

    public static int durationMinutes(String content) {
        if (content == null) return 0;
        Matcher matcher = DURATION_PATTERN.matcher(content);
        if (!matcher.find()) return 0;
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (Exception ignored) {
            return 0;
        }
    }

    public static double distanceKm(String content) {
        if (content == null) return 0;
        Matcher matcher = DISTANCE_PATTERN.matcher(content);
        if (!matcher.find()) return 0;
        try {
            return Double.parseDouble(matcher.group(1));
        } catch (Exception ignored) {
            return 0;
        }
    }

    public static String metricText(String content) {
        double distance = distanceKm(content);
        if (distance > 0) return String.format(Locale.getDefault(), "%.1f km", distance);
        int duration = durationMinutes(content);
        return duration > 0 ? duration + " 分钟" : "--";
    }
}

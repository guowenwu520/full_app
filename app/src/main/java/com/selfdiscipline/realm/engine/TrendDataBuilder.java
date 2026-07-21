package com.selfdiscipline.realm.engine;

import com.selfdiscipline.realm.model.FuturesIncomeRecord;
import com.selfdiscipline.realm.model.WeightRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 将业务记录转换为图表需要的数据。
 * 页面只负责展示，排序、累计值和日期截取统一放在这里维护。
 */
public final class TrendDataBuilder {
    private TrendDataBuilder() {}

    public static final class WeightTrendData {
        public final List<Float> values = new ArrayList<>();
        public final List<String> labels = new ArrayList<>();
    }

    public static final class IncomeTrendData {
        public final List<Integer> cumulativeValues = new ArrayList<>();
        public final List<String> labels = new ArrayList<>();
        public int totalIncome;
        public int todayIncome;
    }

    /** 最新记录放在第一个点，即图表最左侧。 */
    public static WeightTrendData weightsNewestFirst(List<WeightRecord> source) {
        WeightTrendData result = new WeightTrendData();
        if (source == null) return result;

        List<WeightRecord> rows = new ArrayList<>();
        for (WeightRecord record : source) {
            if (record != null && record.weight > 0) rows.add(record);
        }
        Collections.sort(rows, (left, right) -> safe(right.date).compareTo(safe(left.date)));

        for (WeightRecord record : rows) {
            result.values.add(record.weight);
            result.labels.add(shortDate(record.date));
        }
        return result;
    }

    /**
     * 先按时间计算历史累计值，再反转成“最新在左、旧记录向右滑动”的顺序。
     */
    public static IncomeTrendData incomesNewestFirst(
            List<FuturesIncomeRecord> source,
            String today
    ) {
        IncomeTrendData result = new IncomeTrendData();
        if (source == null) return result;

        List<FuturesIncomeRecord> rows = new ArrayList<>();
        for (FuturesIncomeRecord record : source) if (record != null) rows.add(record);
        // state 中同一时间的记录本来就是新记录在前；稳定的降序排序会保留该顺序。
        Collections.sort(rows, (left, right) ->
                safe(right.dateTime).compareTo(safe(left.dateTime)));

        int[] cumulativeAtPoint = new int[rows.size()];
        int cumulative = 0;
        for (int i = rows.size() - 1; i >= 0; i--) {
            FuturesIncomeRecord record = rows.get(i);
            cumulative += record.amount;
            cumulativeAtPoint[i] = cumulative;
            if (safe(record.dateTime).startsWith(safe(today))) {
                result.todayIncome += record.amount;
            }
        }
        result.totalIncome = cumulative;

        for (int i = 0; i < rows.size(); i++) {
            result.cumulativeValues.add(cumulativeAtPoint[i]);
            result.labels.add(shortDate(rows.get(i).dateTime));
        }
        return result;
    }

    private static String shortDate(String value) {
        String safe = safe(value);
        return safe.length() >= 10 ? safe.substring(5, 10) : (safe.isEmpty() ? "--" : safe);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

package com.selfdiscipline.realm.engine;

import com.selfdiscipline.realm.model.AppState;
import com.selfdiscipline.realm.model.Badge;
import com.selfdiscipline.realm.model.Book;
import com.selfdiscipline.realm.model.DiaryRecord;
import com.selfdiscipline.realm.model.ExerciseRecord;
import com.selfdiscipline.realm.model.ExperienceLog;
import com.selfdiscipline.realm.model.FuturesIncomeRecord;
import com.selfdiscipline.realm.model.ReadingHistory;
import com.selfdiscipline.realm.model.SleepRecord;
import com.selfdiscipline.realm.model.WeightRecord;
import com.selfdiscipline.realm.model.WordEntry;
import com.selfdiscipline.realm.util.DateUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StatsEngine {
    public static int totalXp(AppState s) {
        int sum = 0;
        if (s == null || s.expLogs == null) return 0;
        for (ExperienceLog log : s.expLogs) {
            if (log != null) sum += log.points;
        }
        return sum;
    }

    public static int totalCalories(AppState s) {
        int sum = 0;
        if (s == null || s.exercises == null) return 0;
        for (ExerciseRecord record : s.exercises) {
            if (record != null) sum += record.calories;
        }
        return sum;
    }

    public static int caloriesOnDate(AppState s, String date) {
        int sum = 0;
        if (s == null || s.exercises == null || date == null) return 0;
        for (ExerciseRecord record : s.exercises) {
            if (record != null && date.equals(record.date)) sum += record.calories;
        }
        return sum;
    }

    public static Float latestWeightKg(AppState s) {
        if (s == null || s.weights == null) return null;
        WeightRecord latest = null;
        for (WeightRecord record : s.weights) {
            if (record == null) continue;
            if (latest == null || safeDate(record.date).compareTo(safeDate(latest.date)) >= 0) {
                latest = record;
            }
        }
        return latest == null ? null : latest.weight;
    }

    public static int totalFuturesIncome(AppState s) {
        int sum = 0;
        if (s == null || s.futuresIncomes == null) return 0;
        for (FuturesIncomeRecord record : s.futuresIncomes) {
            if (record != null) sum += record.amount;
        }
        return sum;
    }

    public static int totalNotes(AppState s) {
        int sum = 0;
        if (s == null || s.books == null) return 0;
        for (Book book : s.books) {
            if (book != null && book.fullReview != null && !book.fullReview.trim().isEmpty()) sum++;
        }
        return sum;
    }

    /**
     * 先汇总指定月份每天的阅读页数，再得到整个月的阅读总量。
     * 阅读量按每条记录的 newPage - oldPage 计算，只累计正数；
     * 它与“本次实际发放多少阅读经验”是两个独立概念。
     */
    public static int monthlyReadingPages(AppState s, String dateInMonth) {
        if (s == null || s.books == null || dateInMonth == null || dateInMonth.length() < 7) {
            return 0;
        }
        String monthPrefix = dateInMonth.substring(0, 7);
        Map<String, Integer> dailyPages = new HashMap<>();
        for (Book book : s.books) {
            if (book == null || book.readingHistory == null) continue;
            for (ReadingHistory history : book.readingHistory) {
                if (history == null || history.dateTime == null) continue;
                if (history.dateTime.startsWith(monthPrefix)) {
                    String day = history.dateTime.length() >= 10
                            ? history.dateTime.substring(0, 10)
                            : history.dateTime;
                    int pages = history.recordedPages();
                    dailyPages.put(day, dailyPages.getOrDefault(day, 0) + pages);
                }
            }
        }

        int total = 0;
        for (int pages : dailyPages.values()) total += pages;
        return total;
    }

    public static boolean hasExercise(AppState s, String date) {
        if (s == null || s.exercises == null) return false;
        for (ExerciseRecord record : s.exercises) {
            if (record != null && date.equals(record.date)) return true;
        }
        return false;
    }

    public static boolean hasSleepPassed(AppState s, String date) {
        if (s == null || s.sleeps == null) return false;
        for (SleepRecord record : s.sleeps) {
            if (record != null && date.equals(record.date) && record.passed) return true;
        }
        return false;
    }

    public static boolean hasSleepRecord(AppState s, String date) {
        if (s == null || s.sleeps == null) return false;
        for (SleepRecord record : s.sleeps) {
            if (record != null && date.equals(record.date)) return true;
        }
        return false;
    }

    public static boolean hasBroken(AppState s, String date) {
        if (s == null || s.diaries == null) return false;
        for (DiaryRecord record : s.diaries) {
            if (record != null && date.equals(record.date) && record.broken) return true;
        }
        return false;
    }

    /**
     * 未记录破戒即视为未破戒，因此当天默认完成。
     */
    public static boolean hasDiaryNoBreak(AppState s, String date) {
        return !hasBroken(s, date);
    }

    public static boolean hasDiary(AppState s, String date) {
        if (s == null || s.diaries == null) return false;
        for (DiaryRecord record : s.diaries) {
            if (record != null && date.equals(record.date)) return true;
        }
        return false;
    }

    public static boolean hasReading(AppState s, String date) {
        return s != null && s.readingDates != null && s.readingDates.contains(date);
    }

    /** 保留旧接口兼容历史代码，当前版本不再将单词计入打卡。 */
    public static boolean hasWord(AppState s, String date) {
        return s != null && s.wordDates != null && s.wordDates.contains(date);
    }

    public static boolean allFourDone(AppState s, String date) {
        return hasReading(s, date)
                && hasExercise(s, date)
                && hasSleepPassed(s, date)
                && hasDiaryNoBreak(s, date);
    }

    /** 兼容旧调用，实际已改为四项完成。 */
    public static boolean allFiveDone(AppState s, String date) {
        return allFourDone(s, date);
    }

    public static int maxSelfDisciplineStreak(AppState s) {
        Set<String> candidates = new HashSet<>();
        if (s == null) return 0;
        if (s.readingDates != null) candidates.addAll(s.readingDates);
        if (s.exercises != null) {
            for (ExerciseRecord record : s.exercises) if (record != null) addDate(candidates, record.date);
        }
        if (s.sleeps != null) {
            for (SleepRecord record : s.sleeps) if (record != null) addDate(candidates, record.date);
        }

        List<String> completed = new ArrayList<>();
        for (String date : candidates) {
            if (allFourDone(s, date)) completed.add(date);
        }
        return DateUtils.maxStreak(completed);
    }

    /**
     * 当前连续自律天数。今天尚未完成全部项目时保留截至昨天的连续记录；
     * 若今天已经明确破戒或保存了未达标作息，则立即视为中断。
     */
    public static int currentSelfDisciplineStreak(AppState s) {
        if (s == null) return 0;
        String today = DateUtils.today();
        if (hasBroken(s, today)) return 0;
        if (hasSleepRecord(s, today) && !hasSleepPassed(s, today)) return 0;

        Set<String> candidates = new HashSet<>();
        if (s.readingDates != null) candidates.addAll(s.readingDates);
        if (s.exercises != null) {
            for (ExerciseRecord record : s.exercises) {
                if (record != null) addDate(candidates, record.date);
            }
        }
        if (s.sleeps != null) {
            for (SleepRecord record : s.sleeps) {
                if (record != null) addDate(candidates, record.date);
            }
        }

        List<String> completed = new ArrayList<>();
        for (String date : candidates) {
            if (allFourDone(s, date)) completed.add(date);
        }
        return DateUtils.currentStreak(completed);
    }

    /**
     * 历史累计“不破戒”打卡次数，仅保留给旧数据和旧界面兼容。
     * 当前不破戒勋章使用 currentNoBreakStreak()，破戒后从 0 重新累计。
     */
    public static int totalNoBreakCheckins(AppState s) {
        if (s == null || s.awardedKeys == null) return 0;
        Set<String> dates = new HashSet<>();
        for (String key : s.awardedKeys) {
            if (key == null || !key.startsWith("diary_")) continue;
            String date = key.substring("diary_".length());
            if (DateUtils.isValidDate(date)) dates.add(date);
        }
        return dates.size();
    }

    public static int currentNoBreakStreak(AppState s) {
        String start = earliestTrackedDate(s);
        String cursor = DateUtils.today();
        int streak = 0;
        while (cursor.compareTo(start) >= 0) {
            if (hasBroken(s, cursor)) break;
            streak++;
            cursor = DateUtils.shift(cursor, -1);
        }
        return streak;
    }

    public static int maxNoBreakStreak(AppState s) {
        String start = earliestTrackedDate(s);
        String today = DateUtils.today();
        String cursor = start;
        int current = 0;
        int best = 0;
        while (cursor.compareTo(today) <= 0) {
            if (hasBroken(s, cursor)) {
                current = 0;
            } else {
                current++;
                best = Math.max(best, current);
            }
            cursor = DateUtils.shift(cursor, 1);
        }
        return best;
    }

    public static int currentWordStreak(AppState s) {
        return s == null || s.wordDates == null ? 0 : DateUtils.currentStreak(s.wordDates);
    }

    public static int maxWordStreak(AppState s) {
        return s == null || s.wordDates == null ? 0 : DateUtils.maxStreak(s.wordDates);
    }

    public static float weightLoss(AppState s) {
        if (s == null || s.weights == null || s.weights.size() < 2) return 0;
        float max = 0;
        float latest = 0;
        String latestDate = "";
        for (WeightRecord record : s.weights) {
            if (record == null) continue;
            if (record.weight > max) max = record.weight;
            if (latestDate.isEmpty() || safeDate(record.date).compareTo(latestDate) > 0) {
                latestDate = safeDate(record.date);
                latest = record.weight;
            }
        }
        return Math.max(0, max - latest);
    }

    public static int correctRate(AppState s) {
        int correct = 0;
        int wrong = 0;
        if (s == null || s.words == null) return 0;
        for (WordEntry entry : s.words) {
            if (entry == null) continue;
            correct += entry.correctCount;
            wrong += entry.wrongCount;
        }
        int total = correct + wrong;
        return total == 0 ? 0 : (int) Math.round(correct * 100.0 / total);
    }

    public static double badgeMetric(AppState s, Badge badge) {
        if (badge == null) return 0;
        if (Badge.TYPE_SELF.equals(badge.type)) return Math.min(30, currentSelfDisciplineStreak(s));
        if (Badge.TYPE_WEIGHT.equals(badge.type)) return weightLoss(s);
        if (Badge.TYPE_CALORIE.equals(badge.type)) return totalCalories(s);
        if (Badge.TYPE_NOBREAK.equals(badge.type)) return Math.min(30, currentNoBreakStreak(s));
        if (Badge.TYPE_FUTURES.equals(badge.type)) return Math.max(0, totalFuturesIncome(s));
        if (Badge.TYPE_WORD.equals(badge.type)) {
            return s == null || s.words == null ? 0 : s.words.size();
        }
        return 0;
    }

    public static int badgeEarnCount(AppState s, Badge badge) {
        return RewardEngine.badgeUnlockCount(s, badge);
    }

    private static String earliestTrackedDate(AppState s) {
        String earliest = DateUtils.today();
        if (s == null) return earliest;

        if (s.readingDates != null) {
            for (String date : s.readingDates) earliest = earlier(earliest, date);
        }
        if (s.wordDates != null) {
            for (String date : s.wordDates) earliest = earlier(earliest, date);
        }
        if (s.exercises != null) {
            for (ExerciseRecord record : s.exercises) if (record != null) earliest = earlier(earliest, record.date);
        }
        if (s.sleeps != null) {
            for (SleepRecord record : s.sleeps) if (record != null) earliest = earlier(earliest, record.date);
        }
        if (s.diaries != null) {
            for (DiaryRecord record : s.diaries) if (record != null) earliest = earlier(earliest, record.date);
        }
        if (s.weights != null) {
            for (WeightRecord record : s.weights) if (record != null) earliest = earlier(earliest, record.date);
        }
        if (s.futuresIncomes != null) {
            for (FuturesIncomeRecord record : s.futuresIncomes) if (record != null) earliest = earlier(earliest, record.dateTime);
        }
        if (s.expLogs != null) {
            for (ExperienceLog log : s.expLogs) if (log != null) earliest = earlier(earliest, log.date);
        }
        return earliest;
    }

    private static String earlier(String current, String candidate) {
        String normalized = safeDate(candidate);
        if (!DateUtils.isValidDate(normalized)) return current;
        if (normalized.compareTo(DateUtils.today()) > 0) return current;
        return normalized.compareTo(current) < 0 ? normalized : current;
    }

    private static String safeDate(String value) {
        if (value == null) return "";
        return value.length() >= 10 ? value.substring(0, 10) : value;
    }

    private static void addDate(Set<String> out, String value) {
        String date = safeDate(value);
        if (DateUtils.isValidDate(date)) out.add(date);
    }
}

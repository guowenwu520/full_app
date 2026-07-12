package com.selfdiscipline.realm.engine;

import com.selfdiscipline.realm.model.*;
import com.selfdiscipline.realm.util.DateUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StatsEngine {
    public static int totalXp(AppState s) { int sum=0; for (ExperienceLog l:s.expLogs) sum += l.points; return sum; }
    public static int totalCalories(AppState s) { int sum=0; for (ExerciseRecord r:s.exercises) sum += r.calories; return sum; }
    public static int totalNotes(AppState s) { int sum=0; for (Book b:s.books) { if (b.fullReview != null && b.fullReview.trim().length()>0) sum++; } return sum; }
    public static boolean hasExercise(AppState s, String date) { for (ExerciseRecord r:s.exercises) if (date.equals(r.date)) return true; return false; }
    public static boolean hasSleepPassed(AppState s, String date) { for (SleepRecord r:s.sleeps) if (date.equals(r.date) && r.passed) return true; return false; }
    public static boolean hasDiaryNoBreak(AppState s, String date) { for (DiaryRecord r:s.diaries) if (date.equals(r.date) && !r.broken) return true; return false; }
    public static boolean hasDiary(AppState s, String date) { for (DiaryRecord r:s.diaries) if (date.equals(r.date)) return true; return false; }
    public static boolean hasReading(AppState s, String date) { return s.readingDates.contains(date); }
    public static boolean hasWord(AppState s, String date) { return s.wordDates.contains(date); }
    public static boolean allFiveDone(AppState s, String date) { return hasReading(s,date) && hasExercise(s,date) && hasSleepPassed(s,date) && hasWord(s,date) && hasDiaryNoBreak(s,date); }
    public static int maxSelfDisciplineStreak(AppState s) { List<String> dates = new ArrayList<>(); Set<String> candidates = new HashSet<>(); candidates.addAll(s.readingDates); for (ExerciseRecord r:s.exercises) candidates.add(r.date); for (SleepRecord r:s.sleeps) candidates.add(r.date); s.wordDates.addAll(new ArrayList<String>()); for (String d:candidates) if (allFiveDone(s,d)) dates.add(d); return DateUtils.maxStreak(dates); }
    public static int currentNoBreakStreak(AppState s) { List<String> dates = new ArrayList<>(); for (DiaryRecord r:s.diaries) if (!r.broken) dates.add(r.date); return DateUtils.currentStreak(dates); }
    public static int maxNoBreakStreak(AppState s) { List<String> dates = new ArrayList<>(); for (DiaryRecord r:s.diaries) if (!r.broken) dates.add(r.date); return DateUtils.maxStreak(dates); }
    public static int currentWordStreak(AppState s) { return DateUtils.currentStreak(s.wordDates); }
    public static int maxWordStreak(AppState s) { return DateUtils.maxStreak(s.wordDates); }
    public static float weightLoss(AppState s) { if (s.weights.size()<2) return 0; float max=0; float latest=0; String latestDate=""; for (WeightRecord r:s.weights){ if(r.weight>max) max=r.weight; if(latestDate.length()==0 || r.date.compareTo(latestDate)>0){latestDate=r.date; latest=r.weight;} } return Math.max(0, max-latest); }
    public static int correctRate(AppState s) { int c=0,w=0; for(WordEntry e:s.words){c+=e.correctCount; w+=e.wrongCount;} int total=c+w; return total==0?0:(int)Math.round(c*100.0/total); }
    public static int badgeEarnCount(AppState s, Badge b) {
        int metric = 0;
        if (Badge.TYPE_SELF.equals(b.type)) metric = maxSelfDisciplineStreak(s);
        else if (Badge.TYPE_WEIGHT.equals(b.type)) metric = (int)Math.floor(weightLoss(s));
        else if (Badge.TYPE_CALORIE.equals(b.type)) metric = totalCalories(s);
        else if (Badge.TYPE_NOBREAK.equals(b.type)) metric = maxNoBreakStreak(s);
        else if (Badge.TYPE_WORD.equals(b.type)) metric = s.words.size();
        int count = b.target <= 0 ? 0 : metric / b.target;
        if (s.isBadgeUnlocked(b.id) && count < 1) count = 1;
        return Math.max(0, count);
    }

}

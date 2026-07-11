package com.selfdiscipline.realm.engine;

import android.content.Context;
import com.selfdiscipline.realm.R;
import com.selfdiscipline.realm.data.BadgeCatalog;
import com.selfdiscipline.realm.data.RealmCatalog;
import com.selfdiscipline.realm.model.*;
import java.util.ArrayList;
import java.util.List;

public class RewardEngine {
    public static class RewardResult {
        public int gainedXp;
        public boolean realmUp;
        public RealmLevel newRealm;
        public List<Badge> newBadges = new ArrayList<>();
    }

    public static RewardResult afterAction(Context c, AppState s, String date) {
        int beforeXp = StatsEngine.totalXp(s);
        RewardResult result = new RewardResult();
        finalizeRewards(c, s, date, beforeXp, result);
        return result;
    }

    public static RewardResult awardReading(Context c, AppState s, String date) {
        int beforeXp = StatsEngine.totalXp(s);
        RewardResult r = new RewardResult();
        awardDaily(c, s, date, "reading", 10, R.string.exp_reading, r);
        finalizeRewards(c, s, date, beforeXp, r);
        return r;
    }

    public static RewardResult awardExercise(Context c, AppState s, String date) {
        int beforeXp = StatsEngine.totalXp(s);
        RewardResult r = new RewardResult();
        awardDaily(c, s, date, "exercise", 10, R.string.exp_exercise, r);
        finalizeRewards(c, s, date, beforeXp, r);
        return r;
    }

    public static RewardResult awardSleep(Context c, AppState s, String date) {
        int beforeXp = StatsEngine.totalXp(s);
        RewardResult r = new RewardResult();
        awardDaily(c, s, date, "sleep", 15, R.string.exp_sleep, r);
        finalizeRewards(c, s, date, beforeXp, r);
        return r;
    }

    public static RewardResult awardWord(Context c, AppState s, String date) {
        int beforeXp = StatsEngine.totalXp(s);
        RewardResult r = new RewardResult();
        awardDaily(c, s, date, "word", 8, R.string.exp_word, r);
        finalizeRewards(c, s, date, beforeXp, r);
        return r;
    }

    public static RewardResult awardDiary(Context c, AppState s, String date) {
        int beforeXp = StatsEngine.totalXp(s);
        RewardResult r = new RewardResult();
        awardDaily(c, s, date, "diary", 12, R.string.exp_diary, r);
        finalizeRewards(c, s, date, beforeXp, r);
        return r;
    }

    public static void awardDaily(Context c, AppState s, String date, String moduleKey, int points, int sourceRes, RewardResult result) {
        String key = moduleKey + "_" + date;
        if (!s.containsAwardKey(key)) {
            s.addAwardKey(key);
            s.expLogs.add(0, new ExperienceLog(date, key, c.getString(sourceRes), points));
            result.gainedXp += points;
        }
    }

    private static void finalizeRewards(Context c, AppState s, String date, int beforeXp, RewardResult result) {
        maybeAwardAllDone(c, s, date, result);
        checkBadges(c, s, date, result);
        int afterXp = StatsEngine.totalXp(s);
        RealmLevel before = RealmCatalog.current(beforeXp);
        RealmLevel after = RealmCatalog.current(afterXp);
        if (before.nameRes != after.nameRes) {
            result.realmUp = true;
            result.newRealm = after;
        }
    }

    private static void maybeAwardAllDone(Context c, AppState s, String date, RewardResult result) {
        String key = "all_done_" + date;
        if (StatsEngine.allFiveDone(s, date) && !s.containsAwardKey(key)) {
            s.addAwardKey(key);
            s.expLogs.add(0, new ExperienceLog(date, key, c.getString(R.string.exp_all_done), 50));
            result.gainedXp += 50;
        }
    }

    private static void checkBadges(Context c, AppState s, String date, RewardResult result) {
        for (Badge b : BadgeCatalog.all()) {
            if (s.isBadgeUnlocked(b.id)) continue;
            if (meets(s, b)) {
                s.unlockBadge(b.id);
                String key = "badge_" + b.id;
                if (!s.containsAwardKey(key)) {
                    s.addAwardKey(key);
                    s.expLogs.add(0, new ExperienceLog(date, key, c.getString(R.string.format_badge_exp_source, c.getString(R.string.exp_badge), c.getString(b.nameRes)), b.xpReward));
                    result.gainedXp += b.xpReward;
                }
                result.newBadges.add(b);
            }
        }
    }

    public static boolean meets(AppState s, Badge b) {
        if (Badge.TYPE_SELF.equals(b.type)) return StatsEngine.maxSelfDisciplineStreak(s) >= b.target;
        if (Badge.TYPE_WEIGHT.equals(b.type)) return StatsEngine.weightLoss(s) >= b.target;
        if (Badge.TYPE_CALORIE.equals(b.type)) return StatsEngine.totalCalories(s) >= b.target;
        if (Badge.TYPE_NOBREAK.equals(b.type)) return StatsEngine.maxNoBreakStreak(s) >= b.target;
        if (Badge.TYPE_WORD.equals(b.type)) return s.words.size() >= b.target;
        return false;
    }
}

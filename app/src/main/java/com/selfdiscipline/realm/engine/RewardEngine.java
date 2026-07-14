package com.selfdiscipline.realm.engine;

import android.content.Context;

import com.selfdiscipline.realm.R;
import com.selfdiscipline.realm.data.BadgeCatalog;
import com.selfdiscipline.realm.data.RealmCatalog;
import com.selfdiscipline.realm.model.AppState;
import com.selfdiscipline.realm.model.Badge;
import com.selfdiscipline.realm.model.ExperienceLog;
import com.selfdiscipline.realm.model.RealmLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一处理每日经验、阅读页数经验、勋章经验和境界变化。
 *
 * 规则：
 * 1. 阅读页数经验与卡路里勋章经验永久保留，不因数据回落自动收回。
 * 2. 体重、期货勋章按当前值动态锁定；跌破门槛时收回本次解锁经验。
 * 3. 连续自律和连续不破戒为周期勋章：
 *    - 未完成 30 天便中断，收回本周期勋章经验；
 *    - 完成 30 天后中断，只锁回勋章，已经获得的本周期经验永久保留。
 * 4. 每次从锁定变为解锁都会记录一次解锁次数。
 */
public class RewardEngine {
    private static final String BADGE_EVENT_PREFIX = "badge_unlock_event_";
    private static final String BADGE_REWARD_PREFIX = "badge_reward_";
    private static final String BADGE_ACTIVE_PREFIX = "badge_active_";
    private static final String LEGACY_BADGE_PREFIX = "badge_";

    public static class RewardResult {
        public int gainedXp;
        public boolean realmUp;
        public boolean realmDown;
        public RealmLevel newRealm;
        public List<Badge> newBadges = new ArrayList<>();
        public List<Badge> lostBadges = new ArrayList<>();
    }

    public static RewardResult afterAction(Context c, AppState s, String date) {
        int beforeXp = StatsEngine.totalXp(s);
        RewardResult result = new RewardResult();
        finalizeRewards(c, s, date, beforeXp, result);
        return result;
    }

    /**
     * 兼容旧调用：只把当天标记为阅读完成，不再固定奖励 10 经验。
     * 阅读经验必须通过 awardReadingPages() 按实际新增页数发放。
     */
    public static RewardResult awardReading(Context c, AppState s, String date) {
        int beforeXp = StatsEngine.totalXp(s);
        RewardResult result = new RewardResult();
        finalizeRewards(c, s, date, beforeXp, result);
        return result;
    }

    /** 每新增一页奖励一经验；页码回退时不会扣回已经获得的阅读经验。 */
    public static RewardResult awardReadingPages(
            Context c,
            AppState s,
            String date,
            int addedPages,
            String actionId
    ) {
        int beforeXp = StatsEngine.totalXp(s);
        RewardResult result = new RewardResult();
        normalizeState(s);

        int pages = Math.max(0, addedPages);
        if (pages > 0) {
            String safeAction = sanitizeKeyPart(actionId);
            if (safeAction.isEmpty()) {
                safeAction = String.valueOf(System.currentTimeMillis());
            }
            String key = "reading_pages_" + safeAction;
            if (!s.containsAwardKey(key)) {
                s.addAwardKey(key);
                s.expLogs.add(0, new ExperienceLog(
                        date == null ? "" : date,
                        key,
                        "阅读页数：" + pages + " 页",
                        pages
                ));
            }
        }

        finalizeRewards(c, s, date, beforeXp, result);
        return result;
    }

    public static RewardResult awardExercise(Context c, AppState s, String date) {
        int beforeXp = StatsEngine.totalXp(s);
        RewardResult result = new RewardResult();
        awardDaily(c, s, date, "exercise", 10, R.string.exp_exercise, result);
        finalizeRewards(c, s, date, beforeXp, result);
        return result;
    }

    public static RewardResult awardSleep(Context c, AppState s, String date) {
        int beforeXp = StatsEngine.totalXp(s);
        RewardResult result = new RewardResult();
        awardDaily(c, s, date, "sleep", 15, R.string.exp_sleep, result);
        finalizeRewards(c, s, date, beforeXp, result);
        return result;
    }

    /** 仅保留旧代码兼容，当前产品界面不再使用单词模块。 */
    public static RewardResult awardWord(Context c, AppState s, String date) {
        int beforeXp = StatsEngine.totalXp(s);
        RewardResult result = new RewardResult();
        awardDaily(c, s, date, "word", 8, R.string.exp_word, result);
        finalizeRewards(c, s, date, beforeXp, result);
        return result;
    }

    public static RewardResult awardDiary(Context c, AppState s, String date) {
        int beforeXp = StatsEngine.totalXp(s);
        RewardResult result = new RewardResult();
        awardDaily(c, s, date, "diary", 12, R.string.exp_diary, result);
        finalizeRewards(c, s, date, beforeXp, result);
        return result;
    }

    public static void awardDaily(
            Context c,
            AppState s,
            String date,
            String moduleKey,
            int points,
            int sourceRes,
            RewardResult result
    ) {
        normalizeState(s);
        String key = moduleKey + "_" + date;
        if (!s.containsAwardKey(key)) {
            s.addAwardKey(key);
            s.expLogs.add(0, new ExperienceLog(date, key, c.getString(sourceRes), points));
            result.gainedXp += points;
        }
    }

    private static void finalizeRewards(
            Context c,
            AppState s,
            String date,
            int beforeXp,
            RewardResult result
    ) {
        maybeAwardAllDone(c, s, date);
        reconcileBadges(c, s, date, result);

        int afterXp = StatsEngine.totalXp(s);
        result.gainedXp = afterXp - beforeXp;

        RealmLevel before = RealmCatalog.current(beforeXp);
        RealmLevel after = RealmCatalog.current(afterXp);
        if (before.nameRes != after.nameRes) {
            result.newRealm = after;
            result.realmUp = after.minXp > before.minXp;
            result.realmDown = after.minXp < before.minXp;
        }
    }

    private static void maybeAwardAllDone(Context c, AppState s, String date) {
        normalizeState(s);
        String key = "all_done_" + date;
        if (StatsEngine.allFourDone(s, date) && !s.containsAwardKey(key)) {
            s.addAwardKey(key);
            s.expLogs.add(0, new ExperienceLog(
                    date,
                    key,
                    c.getString(R.string.exp_all_done),
                    50
            ));
        }
    }

    public static boolean reconcileBadges(Context c, AppState s, String date) {
        return reconcileBadges(c, s, date, null);
    }

    private static boolean reconcileBadges(
            Context c,
            AppState s,
            String date,
            RewardResult result
    ) {
        if (s == null || c == null) return false;
        normalizeState(s);

        boolean changed = migrateUnlockEventCounts(s);
        if (migrateLegacyBadgeRecords(c, s, date)) changed = true;
        String safeDate = date == null ? "" : date;
        List<Badge> badges = BadgeCatalog.all();

        for (Badge badge : badges) {
            boolean permanent = Badge.TYPE_CALORIE.equals(badge.type);
            boolean meetsNow = meets(s, badge);
            boolean unlocked = s.isBadgeUnlocked(badge.id);
            boolean shouldBeUnlocked = meetsNow || (permanent && hasBadgeHistory(s, badge.id));

            if (shouldBeUnlocked) {
                if (!unlocked) {
                    unlockBadge(c, s, safeDate, badge);
                    changed = true;
                    if (result != null) result.newBadges.add(badge);
                }
            } else if (!permanent) {
                if (removeUnlockedBadge(s, badge.id)) {
                    changed = true;
                    if (result != null) result.lostBadges.add(badge);
                }

                // 动态勋章仅收回“当前未完成周期/当前一次解锁”的经验。
                // 已完成 30 天的两个连续类勋章，其 active 标记已经封存移除，
                // 因而这里只会锁回图标，不会扣掉已经永久保留的经验。
                if (removeActiveRewardsForBadge(s, badge.id)) {
                    changed = true;
                }
            }
        }

        // 两个连续类只有完整达到 30 天后，当前周期的奖励才永久封存。
        if (StatsEngine.currentSelfDisciplineStreak(s) >= maxTarget(Badge.TYPE_SELF)) {
            if (sealActiveRewardsForType(s, Badge.TYPE_SELF)) changed = true;
        }
        if (StatsEngine.currentNoBreakStreak(s) >= maxTarget(Badge.TYPE_NOBREAK)) {
            if (sealActiveRewardsForType(s, Badge.TYPE_NOBREAK)) changed = true;
        }

        return changed;
    }


    /**
     * 把旧版“每次解锁保存一个 event key”的结构迁移成：
     * 每一枚勋章只保存一个整数计数。旧 event key 在迁移后移除。
     */
    private static boolean migrateUnlockEventCounts(AppState s) {
        if (s == null) return false;
        normalizeState(s);
        boolean changed = false;

        for (Badge badge : BadgeCatalog.all()) {
            String prefix = BADGE_EVENT_PREFIX + badge.id + "_";
            int highest = s.getBadgeUnlockCount(badge.id);

            for (int i = s.awardedKeys.size() - 1; i >= 0; i--) {
                String key = s.awardedKeys.get(i);
                if (key == null || !key.startsWith(prefix)) continue;
                try {
                    highest = Math.max(highest, Integer.parseInt(key.substring(prefix.length())));
                } catch (Exception ignored) {
                    highest = Math.max(highest, 1);
                }
                s.awardedKeys.remove(i);
                changed = true;
            }

            if (highest != s.getBadgeUnlockCount(badge.id)) {
                s.setBadgeUnlockCount(badge.id, highest);
                changed = true;
            }
        }
        return changed;
    }

    /**
     * 兼容旧版本 badge_xxx 形式的记录，并迁移成可累计次数的新格式。
     */
    private static boolean migrateLegacyBadgeRecords(
            Context c,
            AppState s,
            String date
    ) {
        boolean changed = false;
        for (Badge badge : BadgeCatalog.all()) {
            String legacyKey = LEGACY_BADGE_PREFIX + badge.id;
            boolean legacyKeyExists = s.containsAwardKey(legacyKey);
            boolean legacyLogExists = hasExperienceLog(s, legacyKey);
            boolean legacyEvidence = legacyKeyExists || legacyLogExists || s.isBadgeUnlocked(badge.id);

            if (!legacyEvidence || badgeUnlockCount(s, badge.id) > 0) {
                continue;
            }

            int count = 1;
            String rewardKey = rewardKey(badge.id, count);
            s.setBadgeUnlockCount(badge.id, count);

            boolean migratedLog = false;
            for (ExperienceLog log : s.expLogs) {
                if (log != null && legacyKey.equals(log.key)) {
                    log.key = rewardKey;
                    migratedLog = true;
                }
            }
            while (s.awardedKeys.remove(legacyKey)) changed = true;
            s.addAwardKey(rewardKey);

            if (!migratedLog) {
                s.expLogs.add(0, new ExperienceLog(
                        date == null ? "" : date,
                        rewardKey,
                        badgeSource(c, badge),
                        badge.xpReward
                ));
            }

            if (!Badge.TYPE_CALORIE.equals(badge.type) && s.isBadgeUnlocked(badge.id)) {
                s.addAwardKey(activeKey(badge.id, count));
            }
            changed = true;
        }
        return changed;
    }

    private static void unlockBadge(
            Context c,
            AppState s,
            String date,
            Badge badge
    ) {
        int unlockNumber = s.incrementBadgeUnlockCount(badge.id);
        String rewardKey = rewardKey(badge.id, unlockNumber);

        s.unlockBadge(badge.id);
        s.addAwardKey(rewardKey);
        s.expLogs.add(0, new ExperienceLog(
                date,
                rewardKey,
                badgeSource(c, badge) + "（第 " + unlockNumber + " 次解锁）",
                badge.xpReward
        ));

        if (!Badge.TYPE_CALORIE.equals(badge.type)) {
            s.addAwardKey(activeKey(badge.id, unlockNumber));
        }
    }

    private static String badgeSource(Context c, Badge badge) {
        return c.getString(
                R.string.format_badge_exp_source,
                c.getString(R.string.exp_badge),
                c.getString(badge.nameRes)
        );
    }

    /** 返回某一枚勋章历史上从锁定切换为解锁的总次数。 */
    public static int badgeUnlockCount(AppState s, String badgeId) {
        if (s == null || badgeId == null) return 0;
        return s.getBadgeUnlockCount(badgeId);
    }

    public static int badgeUnlockCount(AppState s, Badge badge) {
        return badge == null ? 0 : badgeUnlockCount(s, badge.id);
    }

    private static boolean hasBadgeHistory(AppState s, String badgeId) {
        return badgeUnlockCount(s, badgeId) > 0
                || s.isBadgeUnlocked(badgeId)
                || s.containsAwardKey(LEGACY_BADGE_PREFIX + badgeId);
    }

    private static int maxTarget(String type) {
        int max = 0;
        for (Badge badge : BadgeCatalog.all()) {
            if (type.equals(badge.type)) max = Math.max(max, badge.target);
        }
        return max;
    }

    /** 完整达到 30 天后，移除 active 标记但保留奖励日志，使经验永久化。 */
    private static boolean sealActiveRewardsForType(AppState s, String type) {
        boolean changed = false;
        for (Badge badge : BadgeCatalog.all()) {
            if (!type.equals(badge.type)) continue;
            String prefix = BADGE_ACTIVE_PREFIX + badge.id + "_";
            for (int i = s.awardedKeys.size() - 1; i >= 0; i--) {
                String key = s.awardedKeys.get(i);
                if (key != null && key.startsWith(prefix)) {
                    s.awardedKeys.remove(i);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static boolean removeActiveRewardsForBadge(AppState s, String badgeId) {
        boolean changed = false;
        String prefix = BADGE_ACTIVE_PREFIX + badgeId + "_";
        List<Integer> activeNumbers = new ArrayList<>();

        for (int i = s.awardedKeys.size() - 1; i >= 0; i--) {
            String key = s.awardedKeys.get(i);
            if (key == null || !key.startsWith(prefix)) continue;
            try {
                activeNumbers.add(Integer.parseInt(key.substring(prefix.length())));
            } catch (Exception ignored) {
            }
            s.awardedKeys.remove(i);
            changed = true;
        }

        for (Integer number : activeNumbers) {
            if (number != null && removeAwardAndLogs(s, rewardKey(badgeId, number))) {
                changed = true;
            }
        }
        return changed;
    }

    /**
     * 兼容旧调用。现在会按新规则撤销所有未达标动态勋章。
     */
    public static boolean syncNoBreakBadges(AppState s) {
        return revokeUnmetDynamicBadges(s);
    }

    public static boolean revokeUnmetDynamicBadges(AppState s) {
        if (s == null) return false;
        normalizeState(s);
        boolean changed = false;

        for (Badge badge : BadgeCatalog.all()) {
            if (Badge.TYPE_CALORIE.equals(badge.type) || meets(s, badge)) continue;
            if (removeUnlockedBadge(s, badge.id)) changed = true;
            if (removeActiveRewardsForBadge(s, badge.id)) changed = true;
        }
        return changed;
    }

    public static boolean meets(AppState s, Badge badge) {
        if (s == null || badge == null) return false;
        return StatsEngine.badgeMetric(s, badge) >= badge.target;
    }

    /**
     * 当前版本把“未破戒”视为每日默认完成。只要当天没有破戒记录，
     * 就自动补发对应的每日经验，并继续检查全完成与勋章。
     */
    public static boolean ensureDefaultNoBreak(Context c, AppState s, String date) {
        if (c == null || s == null || date == null || StatsEngine.hasBroken(s, date)) {
            return false;
        }
        normalizeState(s);
        int beforeXp = StatsEngine.totalXp(s);
        int beforeKeys = s.awardedKeys.size();
        int beforeBadges = s.unlockedBadgeIds.size();
        awardDiary(c, s, date);
        return beforeXp != StatsEngine.totalXp(s)
                || beforeKeys != s.awardedKeys.size()
                || beforeBadges != s.unlockedBadgeIds.size();
    }

    private static boolean removeUnlockedBadge(AppState s, String badgeId) {
        boolean changed = false;
        while (s.unlockedBadgeIds.remove(badgeId)) changed = true;
        return changed;
    }

    private static boolean removeAwardAndLogs(AppState s, String key) {
        boolean changed = false;
        while (s.awardedKeys.remove(key)) changed = true;
        for (int i = s.expLogs.size() - 1; i >= 0; i--) {
            ExperienceLog log = s.expLogs.get(i);
            if (log != null && key.equals(log.key)) {
                s.expLogs.remove(i);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean hasExperienceLog(AppState s, String key) {
        if (s == null || s.expLogs == null) return false;
        for (ExperienceLog log : s.expLogs) {
            if (log != null && key.equals(log.key)) return true;
        }
        return false;
    }

    private static String unlockEventKey(String badgeId, int count) {
        return BADGE_EVENT_PREFIX + badgeId + "_" + count;
    }

    private static String rewardKey(String badgeId, int count) {
        return BADGE_REWARD_PREFIX + badgeId + "_" + count;
    }

    private static String activeKey(String badgeId, int count) {
        return BADGE_ACTIVE_PREFIX + badgeId + "_" + count;
    }

    private static String sanitizeKeyPart(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static void normalizeState(AppState s) {
        if (s.unlockedBadgeIds == null) s.unlockedBadgeIds = new ArrayList<>();
        if (s.awardedKeys == null) s.awardedKeys = new ArrayList<>();
        if (s.expLogs == null) s.expLogs = new ArrayList<>();
        if (s.words == null) s.words = new ArrayList<>();
        if (s.badgeUnlockCounts == null) s.badgeUnlockCounts = new java.util.HashMap<>();
    }
}

package com.selfdiscipline.realm.data;

import com.selfdiscipline.realm.R;
import com.selfdiscipline.realm.model.RealmLevel;
import java.util.ArrayList;
import java.util.List;

public class RealmCatalog {
    public static List<RealmLevel> all() {
        List<RealmLevel> list = new ArrayList<>();

        // 练气
        list.add(new RealmLevel(R.string.realm_qi_early, R.string.desc_qi_early, 0, 501, RealmLevel.MAJOR_QI));
        list.add(new RealmLevel(R.string.realm_qi_mid, R.string.desc_qi_mid, 501, 1201, RealmLevel.MAJOR_QI));
        list.add(new RealmLevel(R.string.realm_qi_late, R.string.desc_qi_late, 1201, 2201, RealmLevel.MAJOR_QI));
        list.add(new RealmLevel(R.string.realm_qi_peak, R.string.desc_qi_peak, 2201, 3501, RealmLevel.MAJOR_QI));

        // 筑基
        list.add(new RealmLevel(R.string.realm_foundation_early, R.string.desc_foundation_early, 3501, 5501, RealmLevel.MAJOR_FOUNDATION));
        list.add(new RealmLevel(R.string.realm_foundation_mid, R.string.desc_foundation_mid, 5501, 8001, RealmLevel.MAJOR_FOUNDATION));
        list.add(new RealmLevel(R.string.realm_foundation_late, R.string.desc_foundation_late, 8001, 11001, RealmLevel.MAJOR_FOUNDATION));
        list.add(new RealmLevel(R.string.realm_foundation_peak, R.string.desc_foundation_peak, 11001, 15001, RealmLevel.MAJOR_FOUNDATION));

        // 金丹
        list.add(new RealmLevel(R.string.realm_core_early, R.string.desc_core_early, 15001, 20001, RealmLevel.MAJOR_CORE));
        list.add(new RealmLevel(R.string.realm_core_mid, R.string.desc_core_mid, 20001, 26001, RealmLevel.MAJOR_CORE));
        list.add(new RealmLevel(R.string.realm_core_late, R.string.desc_core_late, 26001, 33001, RealmLevel.MAJOR_CORE));
        list.add(new RealmLevel(R.string.realm_core_peak, R.string.desc_core_peak, 33001, 40001, RealmLevel.MAJOR_CORE));

        // 元婴
        list.add(new RealmLevel(R.string.realm_soul_early, R.string.desc_soul_early, 40001, 49001, RealmLevel.MAJOR_SOUL));
        list.add(new RealmLevel(R.string.realm_soul_mid, R.string.desc_soul_mid, 49001, 59001, RealmLevel.MAJOR_SOUL));
        list.add(new RealmLevel(R.string.realm_soul_late, R.string.desc_soul_late, 59001, 70001, RealmLevel.MAJOR_SOUL));
        list.add(new RealmLevel(R.string.realm_soul_peak, R.string.desc_soul_peak, 70001, 82001, RealmLevel.MAJOR_SOUL));

        // 化神
        list.add(new RealmLevel(R.string.realm_spirit_early, R.string.desc_spirit_early, 82001, 95001, RealmLevel.MAJOR_SPIRIT));
        list.add(new RealmLevel(R.string.realm_spirit_mid, R.string.desc_spirit_mid, 95001, 110001, RealmLevel.MAJOR_SPIRIT));
        list.add(new RealmLevel(R.string.realm_spirit_late, R.string.desc_spirit_late, 110001, 130001, RealmLevel.MAJOR_SPIRIT));
        list.add(new RealmLevel(R.string.realm_spirit_peak, R.string.desc_spirit_peak, 130001, 130001, RealmLevel.MAJOR_SPIRIT));

        // 炼虚
        list.add(new RealmLevel(R.string.realm_void_early, R.string.desc_void_early, 130001, 150001, RealmLevel.MAJOR_VOID));
        list.add(new RealmLevel(R.string.realm_void_mid, R.string.desc_void_mid, 150001, 173001, RealmLevel.MAJOR_VOID));
        list.add(new RealmLevel(R.string.realm_void_late, R.string.desc_void_late, 173001, 199001, RealmLevel.MAJOR_VOID));
        list.add(new RealmLevel(R.string.realm_void_peak, R.string.desc_void_peak, 199001, 228001, RealmLevel.MAJOR_VOID));

        // 合体
        list.add(new RealmLevel(R.string.realm_unity_early, R.string.desc_unity_early, 228001, 260001, RealmLevel.MAJOR_UNITY));
        list.add(new RealmLevel(R.string.realm_unity_mid, R.string.desc_unity_mid, 260001, 295001, RealmLevel.MAJOR_UNITY));
        list.add(new RealmLevel(R.string.realm_unity_late, R.string.desc_unity_late, 295001, 333001, RealmLevel.MAJOR_UNITY));
        list.add(new RealmLevel(R.string.realm_unity_peak, R.string.desc_unity_peak, 333001, 374001, RealmLevel.MAJOR_UNITY));

        // 大乘
        list.add(new RealmLevel(R.string.realm_ascension_early, R.string.desc_ascension_early, 374001, 418001, RealmLevel.MAJOR_ASCENSION));
        list.add(new RealmLevel(R.string.realm_ascension_mid, R.string.desc_ascension_mid, 418001, 465001, RealmLevel.MAJOR_ASCENSION));
        list.add(new RealmLevel(R.string.realm_ascension_late, R.string.desc_ascension_late, 465001, 515001, RealmLevel.MAJOR_ASCENSION));
        list.add(new RealmLevel(R.string.realm_ascension_peak, R.string.desc_ascension_peak, 515001, 568001, RealmLevel.MAJOR_ASCENSION));

        // 真仙境
        list.add(new RealmLevel(R.string.realm_true_immortal_early, R.string.desc_true_immortal_early, 568001, 624001, RealmLevel.MAJOR_TRUE_IMMORTAL));
        list.add(new RealmLevel(R.string.realm_true_immortal_mid, R.string.desc_true_immortal_mid, 624001, 683001, RealmLevel.MAJOR_TRUE_IMMORTAL));
        list.add(new RealmLevel(R.string.realm_true_immortal_late, R.string.desc_true_immortal_late, 683001, 745001, RealmLevel.MAJOR_TRUE_IMMORTAL));
        list.add(new RealmLevel(R.string.realm_true_immortal_peak, R.string.desc_true_immortal_peak, 745001, 810001, RealmLevel.MAJOR_TRUE_IMMORTAL));

        // 金仙境
        list.add(new RealmLevel(R.string.realm_golden_immortal_early, R.string.desc_golden_immortal_early, 810001, 878001, RealmLevel.MAJOR_GOLDEN_IMMORTAL));
        list.add(new RealmLevel(R.string.realm_golden_immortal_mid, R.string.desc_golden_immortal_mid, 878001, 949001, RealmLevel.MAJOR_GOLDEN_IMMORTAL));
        list.add(new RealmLevel(R.string.realm_golden_immortal_late, R.string.desc_golden_immortal_late, 949001, 1023001, RealmLevel.MAJOR_GOLDEN_IMMORTAL));
        list.add(new RealmLevel(R.string.realm_golden_immortal_peak, R.string.desc_golden_immortal_peak, 1023001, 1100001, RealmLevel.MAJOR_GOLDEN_IMMORTAL));

        // 太乙境
        list.add(new RealmLevel(R.string.realm_taiyi_early, R.string.desc_taiyi_early, 1100001, 1180001, RealmLevel.MAJOR_TAIYI));
        list.add(new RealmLevel(R.string.realm_taiyi_mid, R.string.desc_taiyi_mid, 1180001, 1263001, RealmLevel.MAJOR_TAIYI));
        list.add(new RealmLevel(R.string.realm_taiyi_late, R.string.desc_taiyi_late, 1263001, 1349001, RealmLevel.MAJOR_TAIYI));
        list.add(new RealmLevel(R.string.realm_taiyi_peak, R.string.desc_taiyi_peak, 1349001, 1438001, RealmLevel.MAJOR_TAIYI));

        // 大罗境
        list.add(new RealmLevel(R.string.realm_daluo_early, R.string.desc_daluo_early, 1438001, 1530001, RealmLevel.MAJOR_DALUO));
        list.add(new RealmLevel(R.string.realm_daluo_mid, R.string.desc_daluo_mid, 1530001, 1625001, RealmLevel.MAJOR_DALUO));
        list.add(new RealmLevel(R.string.realm_daluo_late, R.string.desc_daluo_late, 1625001, 1723001, RealmLevel.MAJOR_DALUO));
        list.add(new RealmLevel(R.string.realm_daluo_peak, R.string.desc_daluo_peak, 1723001, 1824001, RealmLevel.MAJOR_DALUO));

        // 道祖境
        list.add(new RealmLevel(R.string.realm_daozu_early, R.string.desc_daozu_early, 1824001, 1928001, RealmLevel.MAJOR_DAOZU));
        list.add(new RealmLevel(R.string.realm_daozu_mid, R.string.desc_daozu_mid, 1928001, 2035001, RealmLevel.MAJOR_DAOZU));
        list.add(new RealmLevel(R.string.realm_daozu_late, R.string.desc_daozu_late, 2035001, 2145001, RealmLevel.MAJOR_DAOZU));
        list.add(new RealmLevel(R.string.realm_daozu_peak, R.string.desc_daozu_peak, 2145001, 2145001, RealmLevel.MAJOR_DAOZU));

        return list;
    }

    public static RealmLevel current(int xp) {
        List<RealmLevel> all = all();
        RealmLevel cur = all.get(0);
        for (RealmLevel r : all) {
            if (xp >= r.minXp) {
                cur = r;
            }
        }
        return cur;
    }

    public static int indexOf(RealmLevel level) {
        List<RealmLevel> all = all();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).nameRes == level.nameRes) {
                return i;
            }
        }
        return 0;
    }
}

package com.selfdiscipline.realm.data;

import com.selfdiscipline.realm.R;
import com.selfdiscipline.realm.model.RealmLevel;
import java.util.ArrayList;
import java.util.List;

public class RealmCatalog {
    public static List<RealmLevel> all() {
        List<RealmLevel> list = new ArrayList<>();
        list.add(new RealmLevel(R.string.realm_qi_early, R.string.desc_qi_early, 0, 501, RealmLevel.MAJOR_QI));
        list.add(new RealmLevel(R.string.realm_qi_mid, R.string.desc_qi_mid, 501, 1201, RealmLevel.MAJOR_QI));
        list.add(new RealmLevel(R.string.realm_qi_late, R.string.desc_qi_late, 1201, 2201, RealmLevel.MAJOR_QI));
        list.add(new RealmLevel(R.string.realm_qi_peak, R.string.desc_qi_peak, 2201, 3501, RealmLevel.MAJOR_QI));
        list.add(new RealmLevel(R.string.realm_foundation_early, R.string.desc_foundation_early, 3501, 5501, RealmLevel.MAJOR_FOUNDATION));
        list.add(new RealmLevel(R.string.realm_foundation_mid, R.string.desc_foundation_mid, 5501, 8001, RealmLevel.MAJOR_FOUNDATION));
        list.add(new RealmLevel(R.string.realm_foundation_late, R.string.desc_foundation_late, 8001, 11001, RealmLevel.MAJOR_FOUNDATION));
        list.add(new RealmLevel(R.string.realm_foundation_peak, R.string.desc_foundation_peak, 11001, 15001, RealmLevel.MAJOR_FOUNDATION));
        list.add(new RealmLevel(R.string.realm_core_early, R.string.desc_core_early, 15001, 20001, RealmLevel.MAJOR_CORE));
        list.add(new RealmLevel(R.string.realm_core_mid, R.string.desc_core_mid, 20001, 26001, RealmLevel.MAJOR_CORE));
        list.add(new RealmLevel(R.string.realm_core_late, R.string.desc_core_late, 26001, 33001, RealmLevel.MAJOR_CORE));
        list.add(new RealmLevel(R.string.realm_core_peak, R.string.desc_core_peak, 33001, 40001, RealmLevel.MAJOR_CORE));
        list.add(new RealmLevel(R.string.realm_soul_early, R.string.desc_soul_early, 40001, 49001, RealmLevel.MAJOR_SOUL));
        list.add(new RealmLevel(R.string.realm_soul_mid, R.string.desc_soul_mid, 49001, 59001, RealmLevel.MAJOR_SOUL));
        list.add(new RealmLevel(R.string.realm_soul_late, R.string.desc_soul_late, 59001, 70001, RealmLevel.MAJOR_SOUL));
        list.add(new RealmLevel(R.string.realm_soul_peak, R.string.desc_soul_peak, 70001, 82001, RealmLevel.MAJOR_SOUL));
        list.add(new RealmLevel(R.string.realm_spirit_early, R.string.desc_spirit_early, 82001, 95001, RealmLevel.MAJOR_SPIRIT));
        list.add(new RealmLevel(R.string.realm_spirit_mid, R.string.desc_spirit_mid, 95001, 110001, RealmLevel.MAJOR_SPIRIT));
        list.add(new RealmLevel(R.string.realm_spirit_late, R.string.desc_spirit_late, 110001, 130001, RealmLevel.MAJOR_SPIRIT));
        list.add(new RealmLevel(R.string.realm_spirit_peak, R.string.desc_spirit_peak, 130001, 130001, RealmLevel.MAJOR_SPIRIT));
        return list;
    }

    public static RealmLevel current(int xp) {
        List<RealmLevel> all = all();
        RealmLevel cur = all.get(0);
        for (RealmLevel r : all) if (xp >= r.minXp) cur = r;
        return cur;
    }

    public static int indexOf(RealmLevel level) {
        List<RealmLevel> all = all();
        for (int i=0;i<all.size();i++) if (all.get(i).nameRes == level.nameRes) return i;
        return 0;
    }
}

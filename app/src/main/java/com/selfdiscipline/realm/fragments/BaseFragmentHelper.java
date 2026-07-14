package com.selfdiscipline.realm.fragments;

import android.app.Fragment;
import android.content.Context;
import com.selfdiscipline.realm.R;
import com.selfdiscipline.realm.data.RealmCatalog;
import com.selfdiscipline.realm.engine.RewardEngine;
import com.selfdiscipline.realm.model.RealmLevel;
import com.selfdiscipline.realm.ui.RealmDialog;
import com.selfdiscipline.realm.util.ViewUtils;

public class BaseFragmentHelper extends Fragment {
    protected void showReward(RewardEngine.RewardResult r) {
        if (getActivity() == null || r == null) return;
        if (r.gainedXp > 0) {
            ViewUtils.toast(getActivity(), getString(R.string.toast_xp_gained, r.gainedXp));
        } else if (r.gainedXp < 0) {
            ViewUtils.toast(getActivity(), "已收回 " + Math.abs(r.gainedXp) + " 经验");
        }

        if (r.realmUp && r.newRealm != null) {
            RealmDialog.showInfo(
                    getActivity(),
                    getString(R.string.dialog_realm_up_title),
                    getString(
                            R.string.format_realm_up_message,
                            getString(r.newRealm.nameRes),
                            getString(r.newRealm.descRes)
                    )
            );
        } else if (r.realmDown && r.newRealm != null) {
            RealmDialog.showInfo(
                    getActivity(),
                    "境界回落",
                    "当前境界调整为：" + getString(r.newRealm.nameRes)
                            + "\n" + getString(r.newRealm.descRes)
            );
        }
    }
    protected int realmBackground(RealmLevel realm) {
        if (RealmLevel.MAJOR_FOUNDATION.equals(realm.major)) return R.drawable.bg_realm_foundation;
        if (RealmLevel.MAJOR_CORE.equals(realm.major)) return R.drawable.bg_realm_core;
        if (RealmLevel.MAJOR_SOUL.equals(realm.major)) return R.drawable.bg_realm_soul;
        if (RealmLevel.MAJOR_SPIRIT.equals(realm.major)
                || RealmLevel.MAJOR_VOID.equals(realm.major)
                || RealmLevel.MAJOR_UNITY.equals(realm.major)
                || RealmLevel.MAJOR_ASCENSION.equals(realm.major)
                || RealmLevel.MAJOR_TRUE_IMMORTAL.equals(realm.major)
                || RealmLevel.MAJOR_GOLDEN_IMMORTAL.equals(realm.major)
                || RealmLevel.MAJOR_TAIYI.equals(realm.major)
                || RealmLevel.MAJOR_DALUO.equals(realm.major)
                || RealmLevel.MAJOR_DAOZU.equals(realm.major)) return R.drawable.bg_realm_spirit;
        return R.drawable.bg_realm_qi;
    }
}

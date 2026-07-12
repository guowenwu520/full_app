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
        if (r.gainedXp > 0) ViewUtils.toast(getActivity(), getString(R.string.toast_xp_gained, r.gainedXp));
        if (r.realmUp && r.newRealm != null) {
            RealmDialog.showInfo(
                    getActivity(),
                    getString(R.string.dialog_realm_up_title),
                    getString(R.string.format_realm_up_message, getString(r.newRealm.nameRes), getString(r.newRealm.descRes))
            );
        }
    }
    protected int realmBackground(RealmLevel realm) {
        if (RealmLevel.MAJOR_FOUNDATION.equals(realm.major)) return R.drawable.bg_realm_foundation;
        if (RealmLevel.MAJOR_CORE.equals(realm.major)) return R.drawable.bg_realm_core;
        if (RealmLevel.MAJOR_SOUL.equals(realm.major)) return R.drawable.bg_realm_soul;
        if (RealmLevel.MAJOR_SPIRIT.equals(realm.major)) return R.drawable.bg_realm_spirit;
        return R.drawable.bg_realm_qi;
    }
}

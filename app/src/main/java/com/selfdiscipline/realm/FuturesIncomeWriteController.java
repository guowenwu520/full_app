package com.selfdiscipline.realm;

import android.app.Activity;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.selfdiscipline.realm.data.AppRepository;
import com.selfdiscipline.realm.engine.RewardEngine;
import com.selfdiscipline.realm.model.AppState;
import com.selfdiscipline.realm.model.ExperienceLog;
import com.selfdiscipline.realm.model.FuturesIncomeRecord;
import com.selfdiscipline.realm.util.DateUtils;

import java.util.ArrayList;
import java.util.UUID;

/**
 * 期货盈亏独立编辑页的页面控制器。
 *
 * Activity 只负责承载页面，本类独立负责输入校验、数据保存和经验变化，
 * 避免把期货业务混入日记的保存逻辑。
 */
public final class FuturesIncomeWriteController {

    private final Activity activity;
    private final AppRepository repository;
    private final AppState state;

    private EditText amountInput;
    private EditText reflectionInput;

    public FuturesIncomeWriteController(Activity activity) {
        this.activity = activity;
        repository = new AppRepository(activity);
        AppState loadedState = repository.load();
        state = loadedState == null ? new AppState() : loadedState;
        normalizeState();
    }

    public void create() {
        activity.setContentView(R.layout.activity_futures_income_write);

        amountInput = activity.findViewById(R.id.inputFuturesIncomeAmount);
        reflectionInput = activity.findViewById(R.id.inputFuturesIncomeReflection);
        TextView dateView = activity.findViewById(R.id.tvFuturesIncomeWriteDate);

        dateView.setText("记录时间：" + DateUtils.now());
        activity.findViewById(R.id.buttonFuturesIncomeWriteBack)
                .setOnClickListener(v -> activity.finish());
        activity.findViewById(R.id.buttonSaveFuturesIncome)
                .setOnClickListener(v -> save());
    }

    private void save() {
        String rawAmount = amountInput.getText().toString().trim();
        if (TextUtils.isEmpty(rawAmount)) {
            toast("请输入本次盈亏金额");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(rawAmount);
        } catch (NumberFormatException exception) {
            toast("请输入有效的整数金额");
            return;
        }

        if (amount == 0) {
            toast("盈亏金额不能为 0；亏损请填写负数");
            return;
        }

        String reflection = reflectionInput.getText().toString().trim();
        String id = UUID.randomUUID().toString();
        String experienceKey = "futures_income_" + id;
        String dateTime = DateUtils.now();

        state.futuresIncomes.add(
                0,
                new FuturesIncomeRecord(
                        id,
                        dateTime,
                        amount,
                        experienceKey,
                        reflection
                )
        );

        String source = amount > 0
                ? "期货盈利 " + amount + " 元"
                : "期货亏损 " + Math.abs(amount) + " 元";
        state.expLogs.add(
                0,
                new ExperienceLog(
                        DateUtils.today(),
                        experienceKey,
                        source,
                        amount
                )
        );

        RewardEngine.RewardResult reward = RewardEngine.afterAction(
                activity,
                state,
                DateUtils.today()
        );
        repository.save(state);

        String message = amount > 0
                ? "已记录盈利 " + amount + " 元，增加 " + amount + " 经验"
                : "已记录亏损 " + Math.abs(amount) + " 元，扣除 "
                        + Math.abs(amount) + " 经验";
        if (reward != null && reward.gainedXp != 0) {
            message += "；勋章经验变化 " + signed(reward.gainedXp);
        }
        toast(message);
        activity.finish();
    }

    private void normalizeState() {
        if (state.futuresIncomes == null) {
            state.futuresIncomes = new ArrayList<>();
        }
        if (state.expLogs == null) {
            state.expLogs = new ArrayList<>();
        }
    }

    private String signed(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }

    private void toast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }
}

package com.selfdiscipline.realm.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.selfdiscipline.realm.R;
import com.selfdiscipline.realm.util.ViewUtils;

public final class RealmDialog {
    public interface ConfirmAction {
        boolean onConfirm(Dialog dialog);
    }

    public interface ChoiceAction {
        void onChoice(int which);
    }

    private RealmDialog() {
    }

    public static Dialog showInfo(Context context, int titleRes, int messageRes) {
        return showInfo(context, context.getString(titleRes), context.getString(messageRes));
    }

    public static Dialog showInfo(Context context, CharSequence title, CharSequence message) {
        return showContent(
                context,
                title,
                messageView(context, message),
                context.getString(R.string.dialog_ok),
                null,
                dialog -> true
        );
    }

    public static Dialog showConfirm(
            Context context,
            int titleRes,
            CharSequence message,
            int positiveRes,
            int negativeRes,
            final Runnable onPositive
    ) {
        return showConfirm(
                context,
                context.getString(titleRes),
                message,
                context.getString(positiveRes),
                context.getString(negativeRes),
                onPositive
        );
    }

    public static Dialog showConfirm(
            Context context,
            CharSequence title,
            CharSequence message,
            CharSequence positive,
            CharSequence negative,
            final Runnable onPositive
    ) {
        return showContent(
                context,
                title,
                messageView(context, message),
                positive,
                negative,
                dialog -> {
                    if (onPositive != null) {
                        onPositive.run();
                    }
                    return true;
                }
        );
    }

    public static Dialog showChoices(
            Context context,
            CharSequence title,
            String[] choices,
            final ChoiceAction action
    ) {
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        int gap = dp(context, 8);
        list.setPadding(0, dp(context, 4), 0, 0);

        Dialog[] holder = new Dialog[1];
        for (int i = 0; i < choices.length; i++) {
            final int index = i;
            TextView item = new TextView(context);
            item.setText(choices[i]);
            item.setTextSize(15);
            item.setTextColor(context.getResources().getColor(R.color.color_text_main));
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setMinHeight(dp(context, 48));
            item.setPadding(dp(context, 16), 0, dp(context, 16), 0);
            item.setBackgroundResource(R.drawable.bg_dialog_choice_item);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            if (i > 0) {
                lp.topMargin = gap;
            }
            list.addView(item, lp);
            item.setOnClickListener(v -> {
                if (holder[0] != null) {
                    holder[0].dismiss();
                }
                if (action != null) {
                    action.onChoice(index);
                }
            });
        }

        holder[0] = showContent(context, title, list, null, context.getString(R.string.dialog_cancel), null);
        return holder[0];
    }

    public static Dialog showContent(
            Context context,
            int titleRes,
            View content,
            int positiveRes,
            int negativeRes,
            ConfirmAction onConfirm
    ) {
        return showContent(
                context,
                context.getString(titleRes),
                content,
                context.getString(positiveRes),
                context.getString(negativeRes),
                onConfirm
        );
    }

    public static Dialog showContent(
            Context context,
            CharSequence title,
            View content,
            CharSequence positive,
            CharSequence negative,
            ConfirmAction onConfirm
    ) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundResource(R.drawable.bg_dialog_panel);
        int panelPadding = dp(context, 18);
        panel.setPadding(panelPadding, panelPadding, panelPadding, panelPadding);

        TextView titleView = new TextView(context);
        titleView.setText(title == null ? "" : title);
        titleView.setTextColor(context.getResources().getColor(R.color.color_text_main));
        titleView.setTextSize(20);
        titleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER_VERTICAL);
        panel.addView(titleView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        if (content != null) {
            applyDialogInputStyle(context, content);

            ScrollView scroll = new ScrollView(context);
            scroll.setFillViewport(false);
            scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
            scroll.addView(content, new ScrollView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            scrollLp.topMargin = dp(context, 14);
            panel.addView(scroll, scrollLp);
        }

        LinearLayout buttons = new LinearLayout(context);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.END);
        LinearLayout.LayoutParams buttonsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        buttonsLp.topMargin = dp(context, 18);

        if (negative != null) {
            TextView cancel = button(context, negative, false);
            buttons.addView(cancel, buttonLp(context));
            cancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (positive != null) {
            TextView ok = button(context, positive, true);
            LinearLayout.LayoutParams okLp = buttonLp(context);
            okLp.leftMargin = dp(context, 10);
            buttons.addView(ok, okLp);
            ok.setOnClickListener(v -> {
                boolean shouldDismiss = true;
                if (onConfirm != null) {
                    shouldDismiss = onConfirm.onConfirm(dialog);
                }
                if (shouldDismiss) {
                    dialog.dismiss();
                }
            });
        }

        if (positive != null || negative != null) {
            panel.addView(buttons, buttonsLp);
        }

        dialog.setContentView(panel);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setOnShowListener(d -> {
            Window w = dialog.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                w.setLayout(
                        (int) (context.getResources().getDisplayMetrics().widthPixels * 0.88f),
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
            }
        });
        dialog.show();
        return dialog;
    }


    /**
     * Dialogs in the project often build EditText directly in Java. Styling them here keeps
     * all custom dialog input boxes readable and visually consistent without duplicating code.
     */
    private static void applyDialogInputStyle(Context context, View view) {
        if (view == null) {
            return;
        }

        if (view instanceof EditText) {
            EditText input = (EditText) view;
            input.setTextColor(context.getResources().getColor(R.color.color_text_main));
            input.setHintTextColor(context.getResources().getColor(R.color.color_dialog_input_hint));
            input.setTextSize(15);
            input.setBackgroundResource(R.drawable.bg_dialog_input);
            input.setMinHeight(dp(context, 48));
            input.setPadding(dp(context, 14), dp(context, 8), dp(context, 14), dp(context, 8));
            input.setLineSpacing(dp(context, 3), 1.0f);
            input.setIncludeFontPadding(true);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                input.setBackgroundTintList(null);
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyDialogInputStyle(context, group.getChildAt(i));
            }
        }
    }

    private static TextView messageView(Context context, CharSequence message) {
        TextView view = new TextView(context);
        view.setText(message == null ? "" : message);
        view.setTextColor(context.getResources().getColor(R.color.color_text_sub));
        view.setTextSize(15);
        view.setLineSpacing(dp(context, 3), 1.0f);
        return view;
    }

    private static TextView button(Context context, CharSequence text, boolean primary) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(14);
        view.setGravity(Gravity.CENTER);
        view.setMinHeight(dp(context, 42));
        view.setPadding(dp(context, 18), 0, dp(context, 18), 0);
        view.setTextColor(context.getResources().getColor(
                primary ? R.color.color_text_on_primary : R.color.color_text_sub
        ));
        view.setBackgroundResource(primary ? R.drawable.bg_button_primary : R.drawable.bg_dialog_button_cancel);
        return view;
    }

    private static LinearLayout.LayoutParams buttonLp(Context context) {
        return new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
    }

    private static int dp(Context context, int value) {
        return ViewUtils.dp(context, value);
    }
}

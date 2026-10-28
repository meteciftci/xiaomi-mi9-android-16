package com.mi9rom.extrakey;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

// Ayar ekranlarinin ortak gorunumu (Material You renkleri, kart satirlari)
final class Ui {
    final boolean night;
    int cBg, cCard, cAccent, cAccentBg, cText, cText2;
    private final Activity act;

    Ui(Activity a) {
        act = a;
        night = (a.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        Context c = a;
        cBg = c.getColor(night ? android.R.color.system_neutral1_900 : android.R.color.system_neutral1_50);
        cCard = c.getColor(night ? android.R.color.system_neutral1_800 : android.R.color.system_neutral1_0);
        cAccent = c.getColor(night ? android.R.color.system_accent1_200 : android.R.color.system_accent1_700);
        cAccentBg = c.getColor(night ? android.R.color.system_accent2_700 : android.R.color.system_accent2_100);
        cText = c.getColor(night ? android.R.color.system_neutral1_50 : android.R.color.system_neutral1_900);
        cText2 = c.getColor(night ? android.R.color.system_neutral2_300 : android.R.color.system_neutral2_600);
    }

    TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(act);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        return t;
    }

    View card(int icon, String title, String summary, boolean warn) {
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(20), dp(18), dp(20), dp(18));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(28));
        bg.setColor(warn ? cAccentBg : cCard);
        row.setBackground(new android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf((cAccent & 0x00FFFFFF) | 0x33000000), bg, null));
        row.setClickable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(10);
        row.setLayoutParams(lp);

        FrameLayout badge = new FrameLayout(act);
        GradientDrawable bb = new GradientDrawable();
        bb.setShape(GradientDrawable.OVAL);
        bb.setColor(cAccentBg);
        badge.setBackground(bb);
        ImageView iv = new ImageView(act);
        iv.setImageResource(icon);
        iv.setColorFilter(cAccent);
        badge.addView(iv, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
        row.addView(badge, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), 0, 0, 0);
        col.addView(text(title, 17, cText, true));
        TextView s = text(summary, 14, warn ? cText : cAccent, false);
        col.addView(s);
        row.addView(col, new LinearLayout.LayoutParams(0, -2, 1f));
        return row;
    }

    int dp(int v) { return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, act.getResources().getDisplayMetrics()); }
}

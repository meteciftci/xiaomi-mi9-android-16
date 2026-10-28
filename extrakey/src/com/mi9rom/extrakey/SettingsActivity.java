package com.mi9rom.extrakey;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SettingsActivity extends Activity {
    private static final String[] ACTIONS = {
        Prefs.ASSISTANT, Prefs.CAMERA, Prefs.FLASHLIGHT, Prefs.SCREENSHOT, Prefs.RECENTS,
        Prefs.NOTIFICATIONS, Prefs.QUICK_SETTINGS, Prefs.LOCK, Prefs.APP, Prefs.NONE
    };
    private static final int[] LABELS = {
        R.string.act_assistant, R.string.act_camera, R.string.act_flashlight, R.string.act_screenshot,
        R.string.act_recents, R.string.act_notifications, R.string.act_quick_settings, R.string.act_lock,
        R.string.act_app, R.string.act_none
    };
    private static final String[] GESTURES = { Prefs.SINGLE, Prefs.DOUBLE, Prefs.LONG };
    private static final int[] G_TITLES = { R.string.g_single, R.string.g_double, R.string.g_long };
    private static final int[] G_ICONS = { R.drawable.ic_tap1, R.drawable.ic_tap2, R.drawable.ic_hold };

    private boolean night;
    private int cBg, cCard, cAccent, cAccentBg, cText, cText2;
    private LinearLayout list;
    private View warnCard;
    private final TextView[] summaries = new TextView[3];

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setTitle(R.string.title);
        night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        pickColors();
        getWindow().setNavigationBarContrastEnforced(false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(cBg);

        TextView title = text(getString(R.string.title), 34, cText, true);
        title.setPadding(dp(24), dp(40), dp(24), dp(4));
        root.addView(title);
        TextView sub = text(getString(R.string.subtitle), 15, cText2, false);
        sub.setPadding(dp(24), 0, dp(24), dp(20));
        root.addView(sub);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(16), 0, dp(16), dp(24));
        root.addView(list);

        warnCard = card(R.drawable.ic_tap1, getString(R.string.svc_off_title), getString(R.string.svc_off), true);
        warnCard.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        list.addView(warnCard);
        for (int i = 0; i < GESTURES.length; i++) {
            final int idx = i;
            View c = card(G_ICONS[i], getString(G_TITLES[i]), "", false);
            summaries[i] = (TextView) ((LinearLayout) ((LinearLayout) c).getChildAt(1)).getChildAt(1);
            c.setOnClickListener(v -> choose(idx));
            list.addView(c);
        }

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setBackgroundColor(cBg);
        sv.addView(root);
        setContentView(sv);
        WindowInsetsController wic = getWindow().getInsetsController();
        if (wic != null) {
            int light = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            wic.setSystemBarsAppearance(night ? 0 : light, light);
        }
        sv.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            v.setPadding(0, bars.top, 0, bars.bottom);
            return WindowInsets.CONSUMED;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Prefs.serviceEnabled(this)) Prefs.enableService(this);
        Prefs.applyDefaults(this);
        warnCard.setVisibility(Prefs.serviceEnabled(this) ? View.GONE : View.VISIBLE);
        for (int i = 0; i < GESTURES.length; i++) summaries[i].setText(summary(GESTURES[i]));
    }

    private String summary(String gesture) {
        String a = Prefs.action(this, gesture);
        if (Prefs.APP.equals(a)) {
            String pkg = Prefs.app(this, gesture);
            return pkg == null ? getString(R.string.act_app) : getString(R.string.app_chosen, label(pkg));
        }
        for (int i = 0; i < ACTIONS.length; i++) if (ACTIONS[i].equals(a)) return getString(LABELS[i]);
        return getString(R.string.act_none);
    }

    private void choose(int gi) {
        String gesture = GESTURES[gi];
        String[] names = new String[ACTIONS.length];
        int checked = 0;
        String cur = Prefs.action(this, gesture);
        for (int i = 0; i < ACTIONS.length; i++) {
            names[i] = getString(LABELS[i]);
            if (ACTIONS[i].equals(cur)) checked = i;
        }
        new AlertDialog.Builder(this)
            .setTitle(G_TITLES[gi])
            .setSingleChoiceItems(names, checked, (d, which) -> {
                d.dismiss();
                if (Prefs.APP.equals(ACTIONS[which])) pickApp(gesture);
                else {
                    Prefs.setAction(this, gesture, ACTIONS[which]);
                    onResume();
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private void pickApp(String gesture) {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = new ArrayList<>(pm.queryIntentActivities(main, 0));
        Collections.sort(apps, (a, c) -> a.loadLabel(pm).toString().compareToIgnoreCase(c.loadLabel(pm).toString()));
        String[] names = new String[apps.size()];
        for (int i = 0; i < names.length; i++) names[i] = apps.get(i).loadLabel(pm).toString();
        new AlertDialog.Builder(this)
            .setTitle(R.string.pick_app)
            .setItems(names, (d, which) -> {
                Prefs.setApp(this, gesture, apps.get(which).activityInfo.packageName);
                onResume();
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private String label(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    // --- gorunum yardimcilari ---

    private void pickColors() {
        Context c = this;
        cBg = c.getColor(night ? android.R.color.system_neutral1_900 : android.R.color.system_neutral1_50);
        cCard = c.getColor(night ? android.R.color.system_neutral1_800 : android.R.color.system_neutral1_0);
        cAccent = c.getColor(night ? android.R.color.system_accent1_200 : android.R.color.system_accent1_700);
        cAccentBg = c.getColor(night ? android.R.color.system_accent2_700 : android.R.color.system_accent2_100);
        cText = c.getColor(night ? android.R.color.system_neutral1_50 : android.R.color.system_neutral1_900);
        cText2 = c.getColor(night ? android.R.color.system_neutral2_300 : android.R.color.system_neutral2_600);
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        return t;
    }

    private View card(int icon, String title, String summary, boolean warn) {
        LinearLayout row = new LinearLayout(this);
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

        FrameLayout badge = new FrameLayout(this);
        GradientDrawable bb = new GradientDrawable();
        bb.setShape(GradientDrawable.OVAL);
        bb.setColor(cAccentBg);
        badge.setBackground(bb);
        ImageView iv = new ImageView(this);
        iv.setImageResource(icon);
        iv.setColorFilter(cAccent);
        badge.addView(iv, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
        row.addView(badge, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), 0, 0, 0);
        col.addView(text(title, 17, cText, true));
        TextView s = text(summary, 14, warn ? cText : cAccent, false);
        col.addView(s);
        row.addView(col, new LinearLayout.LayoutParams(0, -2, 1f));
        return row;
    }

    private int dp(int v) { return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()); }
}

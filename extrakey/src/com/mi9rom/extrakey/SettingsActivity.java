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

    private Ui ui;
    private LinearLayout list;
    private View warnCard;
    private final TextView[] summaries = new TextView[3];

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setTitle(R.string.title);
        ui = new Ui(this);
        getWindow().setNavigationBarContrastEnforced(false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ui.cBg);

        TextView title = ui.text(getString(R.string.title), 34, ui.cText, true);
        title.setPadding(ui.dp(24), ui.dp(40), ui.dp(24), ui.dp(4));
        root.addView(title);
        TextView sub = ui.text(getString(R.string.subtitle), 15, ui.cText2, false);
        sub.setPadding(ui.dp(24), 0, ui.dp(24), ui.dp(20));
        root.addView(sub);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(ui.dp(16), 0, ui.dp(16), ui.dp(24));
        root.addView(list);

        warnCard = ui.card(R.drawable.ic_tap1, getString(R.string.svc_off_title), getString(R.string.svc_off), true);
        warnCard.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        list.addView(warnCard);
        for (int i = 0; i < GESTURES.length; i++) {
            final int idx = i;
            View c = ui.card(G_ICONS[i], getString(G_TITLES[i]), "", false);
            summaries[i] = (TextView) ((LinearLayout) ((LinearLayout) c).getChildAt(1)).getChildAt(1);
            c.setOnClickListener(v -> choose(idx));
            list.addView(c);
        }

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setBackgroundColor(ui.cBg);
        sv.addView(root);
        setContentView(sv);
        WindowInsetsController wic = getWindow().getInsetsController();
        if (wic != null) {
            int light = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            wic.setSystemBarsAppearance(ui.night ? 0 : light, light);
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
}

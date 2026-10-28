package com.mi9rom.extrakey;

import android.app.Activity;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

// Tek bir sistem ayarini secenek kartlariyla gosterir. Hangi ayar oldugu, acilan activity-alias'in adindan anlasilir.
public class ChoiceActivity extends Activity {
    private static final class Setting {
        final int title, subtitle, icon;
        final int[] labels;
        final int[] values;
        Setting(int title, int subtitle, int icon, int[] labels, int[] values) {
            this.title = title; this.subtitle = subtitle; this.icon = icon; this.labels = labels; this.values = values;
        }
    }

    private Ui ui;
    private String key;
    private Setting st;
    private LinearLayout list;

    private static Setting setting(String key) {
        switch (key) {
            case "VolumeSlider":
                return new Setting(R.string.vol_title, R.string.vol_sub, R.drawable.ic_volume,
                    new int[] { R.string.hide, R.string.show }, new int[] { 0, 1 });
            case "DynamicBar":
                return new Setting(R.string.dyn_title, R.string.dyn_sub, R.drawable.ic_bar,
                    new int[] { R.string.on, R.string.off }, new int[] { 1, 0 });
            case "BatteryPercent":
                return new Setting(R.string.bat_title, R.string.bat_sub, R.drawable.ic_battery,
                    new int[] { R.string.bat_inside, R.string.bat_next, R.string.hide }, new int[] { 1, 2, 0 });
            default:
                return new Setting(R.string.swipe_title, R.string.swipe_sub, R.drawable.ic_swipe,
                    new int[] { R.string.act_screenshot, R.string.act_none }, new int[] { 12, 0 });
        }
    }

    private int read() {
        switch (key) {
            case "VolumeSlider": return Settings.System.getInt(getContentResolver(), "qs_show_volume_slider", 1);
            case "DynamicBar": return Settings.Secure.getInt(getContentResolver(), "ax_dynamic_bar_enabled", 0);
            case "BatteryPercent": return Prefs.getLineage(this, "status_bar_show_battery_percent", 0);
            default: return Prefs.getLineage(this, "key_three_fingers_swipe_action", 0);
        }
    }

    private void write(int v) {
        try {
            switch (key) {
                case "VolumeSlider": Settings.System.putInt(getContentResolver(), "qs_show_volume_slider", v); break;
                case "DynamicBar": Settings.Secure.putInt(getContentResolver(), "ax_dynamic_bar_enabled", v); break;
                case "BatteryPercent": Prefs.putLineagePublic(this, "status_bar_show_battery_percent", v); break;
                default: Prefs.putLineagePublic(this, "key_three_fingers_swipe_action", v);
            }
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String cls = getIntent().getComponent() == null ? "" : getIntent().getComponent().getClassName();
        key = cls.substring(cls.lastIndexOf('.') + 1).replace("Alias", "");
        st = setting(key);
        setTitle(st.title);
        ui = new Ui(this);
        getWindow().setNavigationBarContrastEnforced(false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ui.cBg);
        TextView title = ui.text(getString(st.title), 34, ui.cText, true);
        title.setPadding(ui.dp(24), ui.dp(40), ui.dp(24), ui.dp(4));
        root.addView(title);
        TextView sub = ui.text(getString(st.subtitle), 15, ui.cText2, false);
        sub.setPadding(ui.dp(24), 0, ui.dp(24), ui.dp(20));
        root.addView(sub);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(ui.dp(16), 0, ui.dp(16), ui.dp(24));
        root.addView(list);

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
        render();
    }

    private void render() {
        list.removeAllViews();
        int cur = read();
        for (int i = 0; i < st.labels.length; i++) {
            final int v = st.values[i];
            boolean sel = v == cur;
            View c = ui.card(st.icon, getString(st.labels[i]), sel ? getString(R.string.selected) : "", sel);
            c.setOnClickListener(x -> {
                write(v);
                render();
            });
            list.addView(c);
        }
    }
}

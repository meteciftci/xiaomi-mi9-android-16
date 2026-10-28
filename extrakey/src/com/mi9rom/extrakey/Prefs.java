package com.mi9rom.extrakey;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.text.TextUtils;

final class Prefs {
    static final String NONE = "none";
    static final String ASSISTANT = "assistant";
    static final String CAMERA = "camera";
    static final String FLASHLIGHT = "flashlight";
    static final String SCREENSHOT = "screenshot";
    static final String RECENTS = "recents";
    static final String NOTIFICATIONS = "notifications";
    static final String QUICK_SETTINGS = "quick_settings";
    static final String LOCK = "lock";
    static final String APP = "app";

    private static final String FILE = "extrakey";
    static final String SINGLE = "single";
    static final String DOUBLE = "double";
    static final String LONG = "long";

    private Prefs() {}

    // Cihaz sifrelenmis depolama acilmadan once de okunabilsin diye
    private static SharedPreferences sp(Context c) {
        return c.createDeviceProtectedStorageContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static String action(Context c, String gesture) {
        return sp(c).getString("action_" + gesture, SINGLE.equals(gesture) ? ASSISTANT : NONE);
    }

    static String app(Context c, String gesture) { return sp(c).getString("app_" + gesture, null); }

    static void setAction(Context c, String gesture, String action) {
        sp(c).edit().putString("action_" + gesture, action).apply();
    }

    static void setApp(Context c, String gesture, String pkg) {
        sp(c).edit().putString("app_" + gesture, pkg).putString("action_" + gesture, APP).apply();
    }

    // ROM varsayilanlari: ilk acilista bir kez uygulanir, sonra kullanicinin secimine dokunmaz.
    static void applyDefaults(Context c) {
        SharedPreferences p = sp(c);
        if (p.getBoolean("defaults_v2", false)) return;
        try {
            // Hizli ayarlarda ses kaydiricisini gizle (parlaklik tam genislikte kalir)
            Settings.System.putInt(c.getContentResolver(), "qs_show_volume_slider", 0);
            // Dinamik cubuk acik
            Settings.Secure.putInt(c.getContentResolver(), "ax_dynamic_bar_enabled", 1);
            // crDroid ayarlari: pil yuzdesi simgenin icinde, uc parmak kaydirma = ekran goruntusu
            putLineage(c, "status_bar_show_battery_percent", 1);
            putLineage(c, "key_three_fingers_swipe_action", 12);
            p.edit().putBoolean("defaults_v2", true).apply();
        } catch (RuntimeException ignored) {
        }
    }

    // Kurulum sihirbazi bitince hareketle gezinme overlay'ini bir kez acar (sihirbaz 3 tusa geri aliyor).
    static boolean setupComplete(Context c) {
        return Settings.Secure.getInt(c.getContentResolver(), "user_setup_complete", 0) == 1;
    }

    static void applyGestureNav(Context c) {
        SharedPreferences p = sp(c);
        if (p.getBoolean("gesture_nav_v1", false) || !setupComplete(c)) return;
        try {
            Object om = c.getSystemService("overlay");
            om.getClass().getMethod("setEnabledExclusiveInCategory", String.class, android.os.UserHandle.class)
                .invoke(om, "com.android.internal.systemui.navbar.gestural", android.os.Process.myUserHandle());
            p.edit().putBoolean("gesture_nav_v1", true).apply();
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    static int getLineage(Context c, String name, int def) {
        try (android.database.Cursor q = c.getContentResolver().query(
                android.net.Uri.parse("content://lineagesettings/system"), new String[] { "value" }, "name=?", new String[] { name }, null)) {
            if (q != null && q.moveToFirst()) return Integer.parseInt(q.getString(0));
        } catch (RuntimeException ignored) {
        }
        return def;
    }

    static void putLineagePublic(Context c, String name, int value) { putLineage(c, name, value); }

    private static void putLineage(Context c, String name, int value) {
        android.content.ContentValues v = new android.content.ContentValues();
        v.put("name", name);
        v.put("value", String.valueOf(value));
        c.getContentResolver().insert(android.net.Uri.parse("content://lineagesettings/system"), v);
    }

    static ComponentName service(Context c) { return new ComponentName(c, KeyService.class); }

    static boolean serviceEnabled(Context c) {
        String s = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return s != null && s.contains(service(c).flattenToString());
    }

    // Ayrikalikli uygulama olarak WRITE_SECURE_SETTINGS izni varsa hizmeti kendimiz aciyoruz.
    static boolean enableService(Context c) {
        try {
            if (serviceEnabled(c)) return true;
            String name = service(c).flattenToString();
            String cur = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            String next = TextUtils.isEmpty(cur) ? name : cur + ":" + name;
            Settings.Secure.putString(c.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, next);
            Settings.Secure.putInt(c.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 1);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }
}

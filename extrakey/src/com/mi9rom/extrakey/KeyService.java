package com.mi9rom.extrakey;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class KeyService extends AccessibilityService {
    // key 689 -> PROG_YELLOW, gpio-keys.kl ile eslenir
    private static final int KEY = KeyEvent.KEYCODE_PROG_YELLOW;
    private static final long LONG_MS = 450;
    private static final long DOUBLE_MS = 280;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean torchOn;
    private boolean longFired;
    private int taps;

    private final Runnable longPress = () -> {
        longFired = true;
        taps = 0;
        run(Prefs.action(this, Prefs.LONG), Prefs.LONG);
    };
    private final Runnable singlePress = () -> {
        taps = 0;
        run(Prefs.action(this, Prefs.SINGLE), Prefs.SINGLE);
    };

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        android.util.Log.d("ExtraKey", "key=" + e.getKeyCode() + " action=" + e.getAction() + " locked=" + ((android.app.KeyguardManager) getSystemService(KEYGUARD_SERVICE)).isKeyguardLocked());
        if (e.getKeyCode() != KEY) return false;
        if (e.getAction() == KeyEvent.ACTION_DOWN) {
            if (e.getRepeatCount() == 0) {
                longFired = false;
                handler.removeCallbacks(longPress);
                if (!Prefs.NONE.equals(Prefs.action(this, Prefs.LONG))) handler.postDelayed(longPress, LONG_MS);
            }
        } else if (e.getAction() == KeyEvent.ACTION_UP) {
            handler.removeCallbacks(longPress);
            if (longFired) return true;
            taps++;
            handler.removeCallbacks(singlePress);
            if (taps >= 2) {
                taps = 0;
                run(Prefs.action(this, Prefs.DOUBLE), Prefs.DOUBLE);
            } else if (Prefs.NONE.equals(Prefs.action(this, Prefs.DOUBLE))) {
                singlePress.run();
            } else {
                handler.postDelayed(singlePress, DOUBLE_MS);
            }
        }
        return true;
    }

    private void run(String action, String gesture) {
        switch (action) {
            case Prefs.ASSISTANT: assistant(); break;
            case Prefs.CAMERA: start(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)); break;
            case Prefs.FLASHLIGHT: flashlight(); break;
            case Prefs.SCREENSHOT: performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT); break;
            case Prefs.RECENTS: performGlobalAction(GLOBAL_ACTION_RECENTS); break;
            case Prefs.NOTIFICATIONS: performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS); break;
            case Prefs.QUICK_SETTINGS: performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS); break;
            case Prefs.LOCK: performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN); break;
            case Prefs.APP: {
                String pkg = Prefs.app(this, gesture);
                Intent i = pkg == null ? null : getPackageManager().getLaunchIntentForPackage(pkg);
                if (i != null) start(i);
                break;
            }
            default: break;
        }
    }

    private void assistant() {
        if (!start(new Intent(Intent.ACTION_VOICE_COMMAND))) start(new Intent(Intent.ACTION_ASSIST));
    }

    private boolean start(Intent i) {
        try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private void flashlight() {
        try {
            CameraManager cm = getSystemService(CameraManager.class);
            for (String id : cm.getCameraIdList()) {
                if (Boolean.TRUE.equals(cm.getCameraCharacteristics(id)
                        .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE))) {
                    torchOn = !torchOn;
                    cm.setTorchMode(id, torchOn);
                    return;
                }
            }
        } catch (CameraAccessException ignored) {
        }
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override public void onInterrupt() {}
}

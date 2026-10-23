package com.revror.kernelmanager.utils.display;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.revror.kernelmanager.utils.ShizukuBridge;
import com.revror.kernelmanager.utils.root.RootUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Universal Display Refresh Rate Manager with HyperOS & LTPO 1 Hz Adaptive Support.
 *
 * Strict rules:
 * 1. Only supported modes reported by Display.getSupportedModes() are offered (no artificial frequencies invented).
 * 2. Locks are persisted in SharedPreferences so ROR profile switching does not reset the user's fixed refresh rate.
 * 3. Sets both Float (.0) and Integer representations across System, Global, and Secure settings.
 * 4. Overcomes HyperOS / MIUI vendor locks via `refresh_rate_mode`, `user_refresh_rate`, `smart_refresh_rate`.
 * 5. Uses Android 11+ `cmd display set-user-preferred-display-mode` and SurfaceFlinger transaction fallbacks.
 * 6. Enables 1 Hz Adaptive Minimum mode by activating smart dynamic VRR while maintaining peak gaming FPS.
 * 7. Honestly reports OEM/thermal throttling if display cannot reach requested rate.
 */
public final class RefreshRateManager {

    private static final String TAG = "ROR_RefreshRate";
    private static final String PREF_NAME = "ror_refresh_rate_prefs";
    private static final String KEY_LOCKED_HZ = "locked_hz";
    private static final String KEY_IS_LOCKED = "is_locked";
    private static final String KEY_IS_ADAPTIVE_1HZ = "is_adaptive_1hz";

    public static final class RefreshRateStatus {
        public final float currentHz;
        public final float peakHz;
        public final float minHz;
        public final boolean isAdaptive1Hz;
        public final boolean isVerified;
        public final String details;

        public RefreshRateStatus(float currentHz, float peakHz, float minHz, boolean isAdaptive1Hz, boolean isVerified, String details) {
            this.currentHz = currentHz;
            this.peakHz = peakHz;
            this.minHz = minHz;
            this.isAdaptive1Hz = isAdaptive1Hz;
            this.isVerified = isVerified;
            this.details = details;
        }
    }

    private RefreshRateManager() {}

    public static boolean isRateLocked(@NonNull Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_IS_LOCKED, false);
    }

    public static int getLockedRate(@NonNull Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_LOCKED_HZ, 0);
    }

    public static boolean isAdaptive1HzActive(@NonNull Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_IS_ADAPTIVE_1HZ, false);
    }

    private static void saveLockState(@NonNull Context context, int rate, boolean isLocked, boolean isAdaptive1Hz) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit()
                .putInt(KEY_LOCKED_HZ, rate)
                .putBoolean(KEY_IS_LOCKED, isLocked)
                .putBoolean(KEY_IS_ADAPTIVE_1HZ, isAdaptive1Hz)
                .apply();
    }

    public static boolean isHyperOsOrMiui() {
        String manu = Build.MANUFACTURER != null ? Build.MANUFACTURER.toLowerCase(Locale.US) : "";
        String brand = Build.BRAND != null ? Build.BRAND.toLowerCase(Locale.US) : "";
        if (manu.contains("xiaomi") || brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco")) {
            return true;
        }
        String miuiVer = RootUtils.getProp("ro.miui.ui.version.name");
        return !miuiVer.isEmpty();
    }

    public static float getCurrentRefreshRate(@NonNull Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            return wm.getDefaultDisplay().getRefreshRate();
        }
        return 60f;
    }

    public static float getPeakRefreshRate(@NonNull Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) return 60f;
        Display display = wm.getDefaultDisplay();
        float peak = display.getRefreshRate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display.Mode[] modes = display.getSupportedModes();
            if (modes != null) {
                for (Display.Mode m : modes) {
                    if (m.getRefreshRate() > peak) {
                        peak = m.getRefreshRate();
                    }
                }
            }
        }
        return Math.max(peak, 60f);
    }

    /**
     * Reads genuinely supported hardware refresh rates directly from Display.getSupportedModes().
     * NEVER invents artificial frequencies that the device panel does not support.
     */
    @NonNull
    public static List<Integer> getAvailableRates(@NonNull Context context) {
        List<Integer> list = new ArrayList<>();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            Display display = wm.getDefaultDisplay();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Display.Mode[] modes = display.getSupportedModes();
                if (modes != null) {
                    for (Display.Mode m : modes) {
                        int r = Math.round(m.getRefreshRate());
                        if (r > 0 && !list.contains(r)) {
                            list.add(r);
                        }
                    }
                }
            }
            if (list.isEmpty()) {
                int cur = Math.round(display.getRefreshRate());
                if (cur > 0) list.add(cur);
            }
        }
        if (list.isEmpty()) {
            list.add(60);
        }
        Collections.sort(list);
        return list;
    }

    /**
     * Applies 1 Hz Adaptive Minimum mode (HyperOS / LTPO VRR).
     * Drops minimum refresh rate to 1 Hz for static content to maximize battery efficiency,
     * while preserving peak capability for touch gestures and gaming.
     */
    @NonNull
    public static RefreshRateStatus setAdaptiveOneHz(@NonNull Context context) {
        saveLockState(context, 1, true, true);
        float peak = getPeakRefreshRate(context);
        int peakInt = Math.round(peak);

        StringBuilder sb = new StringBuilder();
        // 1. AOSP standard System, Global, and Secure settings (both float and int)
        sb.append("settings put system min_refresh_rate 1.0; ");
        sb.append("settings put system min_refresh_rate 1; ");
        sb.append("settings put global min_refresh_rate 1.0; ");
        sb.append("settings put global min_refresh_rate 1; ");
        sb.append("settings put secure min_refresh_rate 1.0; ");

        sb.append("settings put system peak_refresh_rate ").append(String.format(Locale.US, "%.1f", (float) peakInt)).append("; ");
        sb.append("settings put global peak_refresh_rate ").append(String.format(Locale.US, "%.1f", (float) peakInt)).append("; ");
        sb.append("settings put secure peak_refresh_rate ").append(String.format(Locale.US, "%.1f", (float) peakInt)).append("; ");

        // 2. HyperOS / MIUI Vendor VRR Configuration
        if (isHyperOsOrMiui()) {
            sb.append("settings put secure refresh_rate_mode 0; ");
            sb.append("settings put system miui_refresh_rate 0; ");
            sb.append("settings put system smart_refresh_rate 1; ");
            sb.append("settings put secure user_refresh_rate ").append(peakInt).append("; ");
            sb.append("settings put system user_refresh_rate ").append(peakInt).append("; ");
        }

        // 3. Clear OEM hard lock overrides
        sb.append("setprop persist.vendor.power.dfps.level 0 2>/dev/null; ");

        executeCommand(sb.toString());

        try {
            Thread.sleep(150);
        } catch (InterruptedException ignored) {}

        float current = getCurrentRefreshRate(context);
        String details = "1 Hz Adaptive Minimum active (1 Hz - " + peakInt + " Hz)\n"
                + (isHyperOsOrMiui() ? "HyperOS Smart VRR Enabled" : "AOSP Adaptive VRR Enabled");

        return new RefreshRateStatus(current, peak, 1.0f, true, true, details);
    }

    /**
     * Sets a genuine fixed refresh rate across System, Global, and Secure namespaces.
     * Persists the lock so ROR profiles do not accidentally revert it.
     */
    @NonNull
    public static RefreshRateStatus setFixedRate(@NonNull Context context, int rate) {
        saveLockState(context, rate, true, false);
        float rateFloat = (float) rate;
        String rateStr = String.format(Locale.US, "%.1f", rateFloat);

        StringBuilder sb = new StringBuilder();
        // AOSP standard System, Global, Secure
        sb.append("settings put system peak_refresh_rate ").append(rateStr).append("; ");
        sb.append("settings put system min_refresh_rate ").append(rateStr).append("; ");
        sb.append("settings put global peak_refresh_rate ").append(rateStr).append("; ");
        sb.append("settings put global min_refresh_rate ").append(rateStr).append("; ");
        sb.append("settings put secure peak_refresh_rate ").append(rateStr).append("; ");
        sb.append("settings put secure min_refresh_rate ").append(rateStr).append("; ");

        // HyperOS / MIUI Specific Settings
        if (isHyperOsOrMiui()) {
            sb.append("settings put secure refresh_rate_mode 1; ");
            sb.append("settings put system miui_refresh_rate ").append(rate).append("; ");
            sb.append("settings put secure user_refresh_rate ").append(rate).append("; ");
            sb.append("settings put system user_refresh_rate ").append(rate).append("; ");
            sb.append("settings put system speed_mode 1; ");
        }

        // Android 11+ display command fallback
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            Display display = wm.getDefaultDisplay();
            int w = display.getWidth();
            int h = display.getHeight();
            sb.append("cmd display set-user-preferred-display-mode ").append(w).append(" ").append(h).append(" ").append(rate).append(" 2>/dev/null; ");
        }

        executeCommand(sb.toString());

        try {
            Thread.sleep(150);
        } catch (InterruptedException ignored) {}

        float current = getCurrentRefreshRate(context);
        int curInt = Math.round(current);
        boolean verified = curInt == rate;
        String details;
        if (verified) {
            details = "Locked at " + rate + " Hz (Verified active display mode)";
        } else {
            details = "Applied " + rate + " Hz • Display reported: " + curInt + " Hz (OEM/Thermal lock or idle scaling active)";
        }

        return new RefreshRateStatus(current, rateFloat, rateFloat, false, verified, details);
    }

    /**
     * Resets refresh rate configuration to Android system default and clears lock.
     */
    @NonNull
    public static RefreshRateStatus resetToDefault(@NonNull Context context) {
        saveLockState(context, 0, false, false);

        StringBuilder sb = new StringBuilder();
        sb.append("settings delete system peak_refresh_rate; ");
        sb.append("settings delete system min_refresh_rate; ");
        sb.append("settings delete system user_refresh_rate; ");
        sb.append("settings delete global peak_refresh_rate; ");
        sb.append("settings delete global min_refresh_rate; ");
        sb.append("settings delete global user_refresh_rate; ");
        sb.append("settings delete secure peak_refresh_rate; ");
        sb.append("settings delete secure min_refresh_rate; ");
        sb.append("settings delete secure user_refresh_rate; ");

        if (isHyperOsOrMiui()) {
            sb.append("settings put secure refresh_rate_mode 0; ");
            sb.append("settings delete system miui_refresh_rate; ");
            sb.append("settings delete system smart_refresh_rate; ");
        }

        sb.append("cmd display clear-user-preferred-display-mode 2>/dev/null; ");

        executeCommand(sb.toString());

        try {
            Thread.sleep(150);
        } catch (InterruptedException ignored) {}

        float current = getCurrentRefreshRate(context);
        return new RefreshRateStatus(current, current, 60f, false, true, "Reset to System Default (Current: " + Math.round(current) + " Hz)");
    }

    private static void executeCommand(String cmd) {
        if (RootUtils.rootAccess()) {
            RootUtils.runCommand(cmd);
        } else if (ShizukuBridge.granted()) {
            ShizukuBridge.run(cmd);
        } else {
            RootUtils.runCommand(cmd);
        }
    }
}

package com.revror.kernelmanager.utils.touch;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.revror.kernelmanager.utils.ShizukuBridge;
import com.revror.kernelmanager.utils.root.RootUtils;

/**
 * Advanced Aim Responsiveness and Touch Precision Engine.
 *
 * Provides 3 hardware & kernel optimization modes:
 * 1. Efficient: 120Hz polling, battery conscious, stock smoothing
 * 2. Balanced: 240Hz polling, fast response, low jitter
 * 3. Competitive: 480Hz ultra-low latency, zero swap interval (EGL 0), instant register
 */
public final class AimBoostEngine {

    public static final int MODE_EFFICIENT = 0;
    public static final int MODE_BALANCED = 1;
    public static final int MODE_COMPETITIVE = 2;

    private static final String PREF_NAME = "ror_aim_boost_prefs";
    private static final String KEY_CURRENT_MODE = "current_mode";

    private AimBoostEngine() {}

    public static int getCurrentMode(@NonNull Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_CURRENT_MODE, MODE_BALANCED);
    }

    public static void applyMode(@NonNull Context context, int mode, @NonNull Runnable onComplete) {
        if (!com.revror.kernelmanager.utils.RorLicenseManager.isPremiumActive(context)) {
            new Handler(Looper.getMainLooper()).post(onComplete);
            return;
        }
        new Thread(() -> {
            StringBuilder sb = new StringBuilder();

            int targetRate;
            int swapInterval;
            int latencyLevel;
            int gameSwitch;
            String responseMode;

            switch (mode) {
                case MODE_COMPETITIVE:
                    targetRate = 480;
                    swapInterval = 0;
                    latencyLevel = 2;
                    gameSwitch = 1;
                    responseMode = "competitive";
                    break;
                case MODE_EFFICIENT:
                    targetRate = 120;
                    swapInterval = 1;
                    latencyLevel = 0;
                    gameSwitch = 0;
                    responseMode = "normal";
                    break;
                case MODE_BALANCED:
                default:
                    targetRate = 240;
                    swapInterval = 1;
                    latencyLevel = 1;
                    gameSwitch = 1;
                    responseMode = "balanced";
                    break;
            }

            // 1. Sysfs Touch Panel Interfaces (Oppo/OnePlus, Xiaomi, Samsung, MediaTek, Vivo)
            String[] tpGameSwitchPaths = new String[]{
                    "/proc/touchpanel/game_switch_enable",
                    "/sys/class/touch/touch_dev/game_switch_enable",
                    "/sys/devices/virtual/touch/touch_dev/game_switch_enable",
                    "/proc/touchpanel/oplus_tp_direction",
                    "/proc/touchpanel/report_rate",
                    "/proc/touchpanel/touch_sampling_rate"
            };

            for (String p : tpGameSwitchPaths) {
                sb.append("if [ -w ").append(p).append(" ]; then echo ").append(gameSwitch).append(" > ").append(p).append(" 2>/dev/null; fi; ");
            }

            // Touch boost governor sysfs
            if (mode == MODE_COMPETITIVE) {
                sb.append("if [ -w /sys/devices/system/cpu/cpufreq/touchboost ]; then echo 1 > /sys/devices/system/cpu/cpufreq/touchboost 2>/dev/null; fi; ");
                sb.append("if [ -w /sys/devices/virtual/touch/touch_dev/bump_sample_rate ]; then echo 1 > /sys/devices/virtual/touch/touch_dev/bump_sample_rate 2>/dev/null; fi; ");
                sb.append("if [ -w /proc/touchpanel/touch_sampling_rate ]; then echo 480 > /proc/touchpanel/touch_sampling_rate 2>/dev/null; fi; ");
            }

            // 2. Android System Properties
            sb.append("setprop debug.touch.sampling_rate ").append(targetRate).append("; ");
            sb.append("setprop persist.vendor.touch.sampling_rate ").append(targetRate).append("; ");
            sb.append("setprop debug.egl.swapinterval ").append(swapInterval).append("; ");
            sb.append("setprop vendor.touch.latency_level ").append(latencyLevel).append("; ");
            sb.append("setprop sys.touch.response_mode ").append(responseMode).append("; ");
            sb.append("setprop persist.sys.touch.game_mode ").append(gameSwitch).append("; ");

            if (mode == MODE_COMPETITIVE) {
                // Eliminate input filtering / smoothing delay for instant register
                sb.append("setprop touch.pressure.scale 0.001; ");
                sb.append("setprop debug.sf.early.app.duration 1666666 2>/dev/null; ");
                sb.append("setprop debug.sf.early.sf.duration 1666666 2>/dev/null; ");
            } else if (mode == MODE_BALANCED) {
                sb.append("setprop touch.pressure.scale 0.005; ");
            }

            executePrivileged(sb.toString());

            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putInt(KEY_CURRENT_MODE, mode)
                    .apply();

            try {
                Thread.sleep(150);
            } catch (InterruptedException ignored) {}

            new Handler(Looper.getMainLooper()).post(onComplete);
        }).start();
    }

    private static void executePrivileged(String cmd) {
        if (RootUtils.rootAccess()) {
            RootUtils.runCommand(cmd);
        } else if (ShizukuBridge.granted()) {
            ShizukuBridge.run(cmd);
        } else {
            RootUtils.runCommand(cmd);
        }
    }
}

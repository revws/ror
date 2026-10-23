/*
 * Reincarnation of Revolutionary profile integration.
 * Distributed under GPL-3.0-or-later.
 */
package com.revror.kernelmanager.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.revror.kernelmanager.utils.root.RootUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class GsmProfileManager {
    public static final String REVROR_SIGNATURE = "revror";

    public interface Callback {
        void onResult(boolean success, String detail);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final String PREFS = "ror_profiles";
    private static final String PROFILE_REGULAR = "selected_profile_regular";
    private static final String PROFILE_PHOENIX = "selected_profile_phoenix";
    private static final String ENGINE = "profile_engine";
    public static final String ENGINE_REGULAR = "regular";
    public static final String ENGINE_PHOENIX = "phoenix";
    private static final String ROOT_GRANTED = "root_granted";

    private GsmProfileManager() {}

    public static String getProfileName(int profile) {
        switch (profile) {
            case 0: return "Daily Use";
            case 1: return "Balance";
            case 2: return "Battery";
            case 3: return "CoolDown";
            case 4: return "Gaming";
            case 5: return "Ultra Gaming";
            default: return "Profile " + profile;
        }
    }

    public static int selectedProfile(Context context) {
        String key = isPhoenixSelected(context) ? PROFILE_PHOENIX : PROFILE_REGULAR;
        Object stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getAll().get(key);
        try {
            int profile = stored instanceof Number ? ((Number) stored).intValue() : Integer.parseInt(String.valueOf(stored));
            int maxProf = isPhoenixSelected(context) ? 7 : 5;
            return Math.max(0, Math.min(maxProf, profile));
        } catch (NumberFormatException ignored) { return 0; }
    }

    public static void saveSelectedProfile(Context context, int profile) {
        String key = isPhoenixSelected(context) ? PROFILE_PHOENIX : PROFILE_REGULAR;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(key, profile).apply();
    }

    public static boolean isPhoenixSelected(Context context) {
        return ENGINE_PHOENIX.equals(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(ENGINE, ENGINE_REGULAR));
    }

    public static void setEngine(Context context, boolean phoenix) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(ENGINE, phoenix ? ENGINE_PHOENIX : ENGINE_REGULAR).apply();
    }

    public static String getProfileName(Context context, int profile) {
        if (!isPhoenixSelected(context)) return getProfileName(profile);
        switch (profile) {
            case 0: return "Battery+";
            case 1: return "Balance+";
            case 2: return "Thermal Cooldown";
            case 3: return "Gaming Max";
            case 4: return "Smooth Gaming+";
            case 5: return "Ultra+";
            case 6: return "Esports";
            case 7: return "Apex";
            default: return "Phoenix Profile " + profile;
        }
    }

    public static boolean requiresLicense(Context context, int profile) {
        return usesPhoenixWorker(context, profile) || profile == 5;
    }

    public static boolean usesPhoenixWorker(Context context, int profile) {
        return isPhoenixSelected(context) && profile >= 0 && profile <= 7;
    }

    public static boolean isRootEnabled(Context context) {
        boolean stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(ROOT_GRANTED, false);
        if (!stored && Looper.myLooper() == Looper.getMainLooper()) return false;
        if (!stored) {
            try {
                if (RootUtils.rootAccess()) {
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                            .edit().putBoolean(ROOT_GRANTED, true).apply();
                    return true;
                }
            } catch (Throwable ignored) {}
        }
        return stored;
    }

    public static void requestRoot(Context context, Callback callback) {
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            boolean granted = false;
            String detail;
            try {
                granted = RootUtils.rootAccess();
                detail = granted ? "Root access granted" : "Root access denied";
            } catch (Exception e) {
                detail = "Root request failed: " + e.getMessage();
            }
            appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putBoolean(ROOT_GRANTED, granted)
                    .putBoolean("apply_on_boot", granted).apply();
            if (callback != null) {
                boolean result = granted;
                String message = detail;
                MAIN.post(() -> callback.onResult(result, message));
            }
        });
    }

    public static void prepare(Context context, Callback callback) {
        int profile = selectedProfile(context);
        if (requiresLicense(context, profile) && !RorLicenseManager.isPremiumActive(context)) {
            post(callback, false, "Activate Phoenix to start the profile engine");
            return;
        }
        if (!isRootEnabled(context)) {
            post(callback, false, "Root access is required for GSM profiles");
            return;
        }
        applyInternal(context.getApplicationContext(), profile, true, callback);
    }

    public static void apply(Context context, int profile, Callback callback) {
        boolean phoenix = usesPhoenixWorker(context, profile);
        if (requiresLicense(context, profile) && !RorLicenseManager.isPremiumActive(context)) {
            post(callback, false, "Phoenix license required");
            return;
        }
        saveSelectedProfile(context, profile);
        String pName = getProfileName(context, profile);
        String logName = phoenix ? "phx.log" : "ror.log";
        appendSharedProfileLog(context, logName, pName + " profile applying");

        // Centralized execution coordinator with finite state machine & deadlock prevention
        com.revror.kernelmanager.utils.kernel.compat.ProfileExecutionCoordinator.getInstance()
                .requestProfile(context, profile, phoenix, (success, message, opId) -> {
                    if (callback != null) {
                        callback.onResult(success, message);
                    }
                });
    }

    public static void restoreOnBoot(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (preferences.getBoolean(ROOT_GRANTED, false)
                && preferences.getBoolean("apply_on_boot", true)) {
            apply(context.getApplicationContext(), selectedProfile(context), null);
        }
    }

    public static void disableRoot(Context context, Callback callback) {
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                RootUtils.runCommand("p=$(cat /data/local/tmp/phx.pid 2>/dev/null); "
                        + "[ -n \"$p\" ] && kill \"$p\" 2>/dev/null; "
                        + "pkill -f /data/local/tmp/phx 2>/dev/null; "
                        + "rm -f /data/local/tmp/phx.pid /data/local/tmp/phx; "
                        + "setprop revror.revws.support 0");
            } catch (Exception ignored) {}
            appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putBoolean(ROOT_GRANTED, false)
                    .putBoolean("apply_on_boot", false).apply();
            post(callback, true, "Root features disabled inside REVROR");
        });
    }

    private static void applyInternal(Context context, int profile, boolean changeProfile, Callback callback) {
        apply(context, profile, callback);
    }

    private static void post(Callback callback, boolean success, String detail) {
        if (callback != null) {
            MAIN.post(() -> callback.onResult(success, detail));
        }
    }

    /**
     * Appends through multiple storage paths (shared storage and app internal dirs)
     * cleanly and non-blockingly without shell freezes.
     */
    public static void appendSharedProfileLog(Context context, String fileName, String message) {
        new Thread(() -> {
            String time = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(new java.util.Date());
            String fullLine = time + " " + message;

            File[] targetDirs = {
                new File("/sdcard"),
                new File("/storage/emulated/0"),
                Environment.getExternalStorageDirectory(),
                (context != null ? context.getExternalFilesDir(null) : null),
                (context != null ? context.getFilesDir() : null)
            };
            for (File dir : targetDirs) {
                if (dir == null) continue;
                try {
                    if (!dir.exists()) dir.mkdirs();
                    writeCleanLog(new File(dir, fileName), fullLine);
                    writeCleanLog(new File(dir, "revws.log"), fullLine);
                } catch (Throwable ignored) {}
            }
        }, "RorSharedProfileLogger").start();
    }

    private static void writeCleanLog(File file, String line) {
        try {
            if (file.exists() && file.length() <= 1024 * 1024) {
                boolean containedZeros = false;
                ByteArrayOutputStream clean = new ByteArrayOutputStream((int) file.length());
                try (FileInputStream input = new FileInputStream(file)) {
                    int value;
                    while ((value = input.read()) != -1) {
                        if (value == 0) containedZeros = true;
                        else clean.write(value);
                    }
                }
                if (containedZeros) {
                    try (FileOutputStream output = new FileOutputStream(file, false)) {
                        clean.writeTo(output);
                    }
                }
            }
            try (FileOutputStream output = new FileOutputStream(file, true)) {
                output.write((line + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                output.flush();
            }
        } catch (Throwable ignored) {}
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    static int toPhoenixProfile(int profile) {
        if (profile == 0) return 1; // Battery+ (codex 1)
        if (profile == 1) return 3; // Balance+ uses the smooth worker.
        if (profile == 2) return 0; // Thermal Cooldown must never select Ultra Gaming.
        if (profile == 3) return 3; // Gaming Max uses high throughput worker.
        if (profile == 4) return 3; // Smooth Gaming+ uses the smooth worker.
        if (profile == 5) return 2; // Ultra+ uses ultra gaming worker.
        if (profile == 6) return 3; // Esports uses low latency gaming worker.
        if (profile == 7) return 2; // Apex uses high performance worker.
        return 3;
    }

    private static String buildProfilePropertyCommand(boolean phoenix, int profile) {
        int engineProfile = phoenix ? toPhoenixProfile(profile) : toRegularProfile(profile);
        int spectrumProfile = (profile == 5 ? 1 : profile == 2 ? 2 : profile == 4 ? 3 : 0);
        return "setprop persist.revror.manual_cpu 0; setprop persist.revror.manual_gpu 0; setprop persist.revror.app.profile " + profile
                + "; setprop persist.revror.profile " + profile
                + "; setprop persist.revror.worker.profile " + engineProfile
                + (phoenix ? "; setprop persist.revror.phoenix.profile " : "; setprop persist.revror.revws.profile ") + engineProfile
                + "; setprop persist.revror.mode " + engineProfile
                + "; setprop persist.revror.active_engine " + (phoenix ? "phoenix" : "regular")
                + "; setprop persist.spectrum.profile " + spectrumProfile
                + "; setprop revror.phoenix.support " + (phoenix ? "1" : "0")
                + "; setprop revror.revws.support 1"
                + "; setprop revror.support 1";
    }

    public static void applyImmediateCpuConfig(int profile) {
        try {
            com.revror.kernelmanager.utils.kernel.compat.KernelCompatEngine.applyProfileUniversal(profile, false);
        } catch (Throwable ignored) {}
    }

    static int toRegularProfile(int profile) {
        // First 4 profiles for regular users connect directly with gsmws / library:
        // 0 = Daily Use and Battery, 1 = Balance Smooth, 2 = Ultra Battery, 3 = Gaming
        if (profile == 0) return 0;
        if (profile == 1) return 1;
        if (profile == 2) return 2;
        if (profile == 3) return 4; // CoolDown
        if (profile == 4) return 3; // Gaming
        if (profile == 5) return 6; // Ultra Gaming
        return profile;
    }

    private static void applyPhoenixEnhancements(int profile) throws Exception {
        RootUtils.runCommand("setprop persist.revror.phoenix.boost "
                + (profile == 5 ? "nova" : profile == 2 ? "cryo" : profile == 0 ? "horizon" : "apocalypse"));
    }
}

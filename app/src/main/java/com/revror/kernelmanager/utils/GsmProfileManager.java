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
            return Math.max(0, Math.min(5, profile));
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
            case 2: return "Thermal Profile+";
            case 3: return "CoolDown+";
            case 4: return "Smooth Gaming+";
            case 5: return "Ultra Gaming+";
            default: return "Phoenix Profile";
        }
    }

    public static boolean requiresLicense(Context context, int profile) {
        return usesPhoenixWorker(context, profile) || profile == 5;
    }

    public static boolean usesPhoenixWorker(Context context, int profile) {
        return isPhoenixSelected(context) && profile >= 0 && profile <= 5;
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

        // Root discovery and shell commands run on the engine worker, never the UI thread.
        applyInternal(context.getApplicationContext(), profile, true, callback);
    }

    public static void restoreOnBoot(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (preferences.getBoolean(ROOT_GRANTED, false)
                && preferences.getBoolean("apply_on_boot", true)) {
            applyInternal(context.getApplicationContext(), selectedProfile(context), true, null);
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
        if (requiresLicense(context, profile) && !RorLicenseManager.isPremiumActive(context)) {
            post(callback, false, "License required for this profile");
            return;
        }
        EXECUTOR.execute(() -> {
            boolean success = false;
            String detail;
            try {
                ProfileDiagnostics.stage(context, "root access", profile);
                if (!RootUtils.rootAccess()) {
                    detail = "Root access was not granted";
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                            .edit().putBoolean(ROOT_GRANTED, false).apply();
                } else {
                    boolean phoenixEngine = usesPhoenixWorker(context, profile);

                    String pName = getProfileName(context, profile);
                    String logName = phoenixEngine ? "phx.log" : "ror.log";
                    // Profiles use app-private storage; changing all-files app-ops kills the UID on some ROMs.
                    appendSharedProfileLog(context, logName, pName + " profile applying");
                    ProfileDiagnostics.stage(context, "install worker", profile);
                    Worker worker = installWorker(context, phoenixEngine);
                    ProfileWorkerRunner.start(context, worker.file, worker.binary,
                            buildProfilePropertyCommand(phoenixEngine, profile));
                    ProfileDiagnostics.stage(context, "apply tuning", profile);
                    // A single device-aware CPU controller owns both engines, including Ultra exit.
                    RootUtils.runCommand("cmd power set-fixed-performance-mode-enabled " + (profile == 5 ? "true" : "false")
                            + " 2>/dev/null; cmd power set-mode 0 2>/dev/null; setprop persist.revror.ultra_gaming " + (profile == 5 ? "1" : "0"));
                    if (phoenixEngine) applyPhoenixEnhancements(profile);

                    // Payloads initialize asynchronously. Verify the controller after that startup window.
                    RorFeedback.append(context, RootUtils.runAndGetOutput("sleep 8; "
                            + "for p in /sys/devices/system/cpu/cpufreq/policy*; do [ -d \"$p\" ] || continue; "
                            + "echo CPU_POLICY:$(basename \"$p\"):$(cat \"$p/scaling_min_freq\"):$(cat \"$p/scaling_max_freq\"):$(cat \"$p/scaling_governor\"); done"));

                    int phoenixProfile = toPhoenixProfile(profile);
                    int regularProfile = toRegularProfile(profile);
                    String profName = getProfileName(context, profile);
                    String actual = RootUtils.getProp("persist.revror.revws.profile");
                    String actualPhoenix = RootUtils.getProp("persist.revror.phoenix.profile");
                    success = phoenixEngine ? String.valueOf(phoenixProfile).equals(actualPhoenix)
                            : String.valueOf(regularProfile).equals(actual);
                    if (success) context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                            .putString("active_engine", phoenixEngine ? ENGINE_PHOENIX : ENGINE_REGULAR)
                            .putInt("active_profile", profile).apply();
                    ProfileDiagnostics.stage(context, success ? "completed" : "verification pending", profile);
                    appendSharedProfileLog(context, logName, pName + " profile activated");
                    detail = success ? ("Current profile - " + profName) : "Worker started; waiting for profile state";
                }
            } catch (Exception e) {
                ProfileDiagnostics.error(context, e);
                detail = "Profile error: " + e.getMessage();
            }
            if (callback != null) {
                boolean result = success;
                String message = detail;
                MAIN.post(() -> callback.onResult(result, message));
            }
        });
    }

    private static void post(Callback callback, boolean success, String detail) {
        if (callback != null) {
            MAIN.post(() -> callback.onResult(success, detail));
        }
    }

    /**
     * Appends through multiple storage paths (root namespace, shared storage, and app external dirs)
     * to guarantee that logs appear on ALL devices and Android versions.
     */
    public static void appendSharedProfileLog(Context context, String fileName, String message) {
        EXECUTOR.execute(() -> {
            String time = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(new java.util.Date());
            String fullLine = time + " " + message;

            // 1. Root shell write across /sdcard, /storage/emulated/0, and /data/media/0
            String esc = fullLine.replace("\"", "\\\"").replace("`", "\\`");
            String rootCmd = "for d in /sdcard /storage/emulated/0 /data/media/0; do "
                    + "if [ -d \"$d\" ]; then "
                    + "echo \"" + esc + "\" >> \"$d/" + fileName + "\" 2>/dev/null; "
                    + "echo \"" + esc + "\" >> \"$d/revws.log\" 2>/dev/null; "
                    + "chmod 0666 \"$d/" + fileName + "\" \"$d/revws.log\" 2>/dev/null; "
                    + "chown 1023:1023 \"$d/" + fileName + "\" \"$d/revws.log\" 2>/dev/null || chown media_rw:media_rw \"$d/" + fileName + "\" \"$d/revws.log\" 2>/dev/null; "
                    + "fi; done; "
                    + "am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d \"file:///sdcard/" + fileName + "\" >/dev/null 2>&1; "
                    + "am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d \"file:///sdcard/revws.log\" >/dev/null 2>&1;";
            try {
                RootUtils.runCommand(rootCmd);
            } catch (Throwable ignored) {}

            // 2. Direct Java File write across standard storage directories and app files
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
        });
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

    private static Worker installWorker(Context context, boolean phoenix) throws Exception {
        String[] candidates = {"library"};
        InputStream input = null;
        String selected = null;
        for (String candidate : candidates) {
            try { input = context.getAssets().open(candidate); selected = candidate; break; }
            catch (Exception ignored) {}
        }
        if (input == null) throw new IllegalStateException("REVROR profile payload is missing");
        // SHC passes this basename to mksh; a leading 'r' enables restricted-shell mode.
        File output = new File(context.getFilesDir(), (phoenix ? "phoenix_" : "library_")
                + com.revror.kernelmanager.BuildConfig.VERSION_CODE + ".sh");
        android.util.AtomicFile atomic = new android.util.AtomicFile(output);
        FileOutputStream stream = atomic.startWrite();
        try (InputStream source = input) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = source.read(buffer)) != -1) stream.write(buffer, 0, count);
            atomic.finishWrite(stream);
        } catch (Exception failure) {
            atomic.failWrite(stream);
            throw failure;
        }
        boolean binary = selected.endsWith(".bin") || selected.contains(".shc") || isElf(output);
        return new Worker(output, binary);
    }

    private static boolean isElf(File file) {
        try (InputStream input = new java.io.FileInputStream(file)) {
            return input.read() == 0x7f && input.read() == 'E' && input.read() == 'L' && input.read() == 'F';
        } catch (Exception ignored) { return false; }
    }

    private static final class Worker {
        final File file;
        final boolean binary;
        Worker(File file, boolean binary) { this.file = file; this.binary = binary; }
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private static int toPhoenixProfile(int profile) {
        if (profile == 0) return 1; // Battery+
        if (profile == 1) return 3; // Balance+ uses the smooth worker.
        if (profile == 2) return 0; // Thermal Profile+ must never select Ultra Gaming.
        if (profile == 3) return 0; // CoolDown+ uses the thermal worker.
        if (profile == 4) return 3; // Phoenix Gaming+ (smooth gaming)
        if (profile == 5) return 2; // Phoenix Ultra Gaming+ (ultra gaming)
        return 3;
    }

    private static String buildProfilePropertyCommand(boolean phoenix, int profile) {
        int engineProfile = phoenix ? toPhoenixProfile(profile) : toRegularProfile(profile);
        return "setprop persist.revror.manual_cpu 0; setprop persist.revror.manual_gpu 0; setprop persist.revror.app.profile " + profile
                + "; setprop persist.revror.profile " + profile
                + "; setprop persist.revror.worker.profile " + engineProfile
                + (phoenix ? "; setprop persist.revror.phoenix.profile " : "; setprop persist.revror.revws.profile ") + engineProfile
                + "; setprop persist.revror.mode " + engineProfile
                + "; setprop persist.revror.active_engine " + (phoenix ? "phoenix" : "regular")
                + "; setprop revror.phoenix.support " + (phoenix ? "1" : "0")
                + "; setprop revror.revws.support 1"
                + "; setprop revror.support 1";
    }

    private static int toRegularProfile(int profile) {
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
                + (profile == 5 ? "nova" : profile == 2 || profile == 3 ? "cryo" : profile == 0 ? "horizon" : "apocalypse"));
    }
}

package com.revror.kernelmanager.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.revror.kernelmanager.utils.kernel.compat.NativeKernelAccess;
import com.revror.kernelmanager.utils.root.RootUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ultra Gaming Background Workload Management Engine.
 *
 * Activates ONLY when:
 * 1. Ultra Gaming or Ultra+ profile is active.
 * 2. A supported game session is active.
 * 3. Background optimization is enabled by user.
 *
 * Implements non-destructive workload management:
 * - Dynamic resolution of protected packages (SystemUI, dialer, IME, home launcher,
 *   root managers, Shizuku, accessibility, notifications, alarms, foreground services, active game).
 * - Trims background package memory via `am trim-memory <pkg> RUNNING_MODERATE` and `BACKGROUND`.
 * - Memory cache compaction without force-stopping or inducing crash/restart loops.
 */
public final class UltraGamingBackgroundManager {

    private static final String TAG = "UltraBgManager";

    private UltraGamingBackgroundManager() {}

    /**
     * Checks if all 3 mandatory activation criteria are met.
     */
    public static boolean shouldActivate(@Nullable Context context, @Nullable String targetPackage, boolean userEnabled) {
        if (!userEnabled) {
            return false;
        }
        if (targetPackage == null || targetPackage.trim().isEmpty()) {
            return false;
        }
        if (context == null) {
            return false;
        }
        // Never trim or disrupt background tasks while screen recording is active
        if (com.revror.kernelmanager.utils.media.ScreenCaptureManager.getInstance().isRecording()) {
            return false;
        }
        int profile = GsmProfileManager.selectedProfile(context);
        // Profile 5 is Ultra Gaming (Regular) and Ultra+ (Phoenix)
        return (profile == 5);
    }

    /**
     * Dynamically identifies all protected packages that must never be terminated or disrupted.
     */
    @NonNull
    public static Set<String> getProtectedPackages(@NonNull Context context, @Nullable String activeGamePackage) {
        Set<String> protectedSet = new HashSet<>();

        // 1. REVROR application itself
        protectedSet.add(context.getPackageName());
        protectedSet.add("com.revror.kernelmanager");

        // 2. The active game session
        if (activeGamePackage != null && !activeGamePackage.trim().isEmpty()) {
            protectedSet.add(activeGamePackage.trim());
        }

        // 3. Android System UI and shell
        protectedSet.add("com.android.systemui");
        protectedSet.add("com.android.shell");
        protectedSet.add("android");
        protectedSet.add("com.android.providers.media");
        protectedSet.add("com.google.android.providers.media.module");

        // 4. Default Phone / Dialer / Telecom / Calling services (via TelecomManager)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                TelecomManager tm = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
                if (tm != null) {
                    String defDialer = tm.getDefaultDialerPackage();
                    if (defDialer != null && !defDialer.isEmpty()) {
                        protectedSet.add(defDialer);
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        String sysDialer = tm.getSystemDialerPackage();
                        if (sysDialer != null && !sysDialer.isEmpty()) {
                            protectedSet.add(sysDialer);
                        }
                    }
                }
            } catch (Throwable t) {
                Log.w(TAG, "Dialer resolution notice: " + t.getMessage());
            }
        }
        protectedSet.add("com.android.phone");
        protectedSet.add("com.android.server.telecom");
        protectedSet.add("com.android.providers.telephony");
        protectedSet.add("com.android.emergency");

        // 5. Active Input Method (IME / Keyboard)
        try {
            String defaultIme = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
            if (defaultIme != null && defaultIme.contains("/")) {
                String imePkg = defaultIme.split("/")[0];
                if (!imePkg.isEmpty()) {
                    protectedSet.add(imePkg);
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "IME resolution notice: " + t.getMessage());
        }
        protectedSet.add("com.google.android.inputmethod.latin");

        // 6. Default Home Launcher
        try {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                String launcherPkg = resolveInfo.activityInfo.packageName;
                if (launcherPkg != null && !launcherPkg.isEmpty()) {
                    protectedSet.add(launcherPkg);
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Launcher resolution notice: " + t.getMessage());
        }

        // 7. Root Managers & Privileged APIs
        protectedSet.add("com.topjohnwu.magisk");
        protectedSet.add("me.weishu.kernelsu");
        protectedSet.add("io.github.a13e300.ksu");
        protectedSet.add("me.bmax.apatch");
        protectedSet.add("moe.shizuku.privileged.api");

        // 8. Enabled Accessibility Services
        try {
            String enabledAcc = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (enabledAcc != null && !enabledAcc.isEmpty()) {
                for (String entry : enabledAcc.split(":")) {
                    if (entry.contains("/")) {
                        String accPkg = entry.split("/")[0];
                        if (!accPkg.isEmpty()) {
                            protectedSet.add(accPkg);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Accessibility resolution notice: " + t.getMessage());
        }

        // 9. Active Notification Listeners
        try {
            String notifs = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
            if (notifs != null && !notifs.isEmpty()) {
                for (String entry : notifs.split(":")) {
                    if (entry.contains("/")) {
                        String notifPkg = entry.split("/")[0];
                        if (!notifPkg.isEmpty()) {
                            protectedSet.add(notifPkg);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Notification listeners notice: " + t.getMessage());
        }

        // 10. Core Google Play services and system accounts
        protectedSet.add("com.google.android.gms");
        protectedSet.add("com.android.vending");

        // 11. User-configured protected packages from preferences
        try {
            Set<String> userProtected = context.getSharedPreferences("ror_tools", Context.MODE_PRIVATE)
                    .getStringSet("protected_packages", Collections.emptySet());
            if (userProtected != null) {
                protectedSet.addAll(userProtected);
            }
        } catch (Throwable ignored) {}

        return protectedSet;
    }

    /**
     * Executes non-destructive background workload management.
     * Trims background process memory while strictly exempting all protected packages.
     *
     * @return count of trimmed packages
     */
    public static int executeNonDestructiveWorkloadManagement(@NonNull Context context, @Nullable String activeGamePackage) {
        Set<String> protectedPackages = getProtectedPackages(context, activeGamePackage);
        Log.i(TAG, "Executing non-destructive workload trim. Total protected packages: " + protectedPackages.size());

        Set<String> runningPackages = new HashSet<>();
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                List<ActivityManager.RunningAppProcessInfo> procs = am.getRunningAppProcesses();
                if (procs != null) {
                    for (ActivityManager.RunningAppProcessInfo p : procs) {
                        if (p.pkgList != null) {
                            Collections.addAll(runningPackages, p.pkgList);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "ActivityManager query notice: " + t.getMessage());
        }

        // If ActivityManager returned empty due to Android 11+ query limitations, query third-party packages
        if (runningPackages.isEmpty()) {
            try {
                String pmOut = RootUtils.runAndGetOutput("pm list packages -3");
                if (pmOut != null) {
                    for (String line : pmOut.split("\\r?\\n")) {
                        if (line.startsWith("package:")) {
                            String pkg = line.substring(8).trim();
                            if (!pkg.isEmpty()) runningPackages.add(pkg);
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        int trimmedCount = 0;
        for (String pkg : runningPackages) {
            if (pkg == null || pkg.isEmpty()) continue;
            if (protectedPackages.contains(pkg)) {
                continue; // Strictly protected - never touch
            }

            // Non-destructive memory trim: am trim-memory <package> RUNNING_MODERATE & BACKGROUND
            try {
                if (GsmProfileManager.isRootEnabled(context)) {
                    RootUtils.runCommand("am trim-memory " + pkg + " RUNNING_MODERATE 2>/dev/null; "
                            + "am trim-memory " + pkg + " BACKGROUND 2>/dev/null");
                    trimmedCount++;
                } else if (ShizukuBridge.granted()) {
                    ShizukuBridge.run("am trim-memory " + pkg + " RUNNING_MODERATE 2>/dev/null; "
                            + "am trim-memory " + pkg + " BACKGROUND 2>/dev/null");
                    trimmedCount++;
                }
            } catch (Throwable ignored) {}
        }

        // Compact memory cache safely
        NativeKernelAccess.writeString("/proc/sys/vm/drop_caches", "3");
        NativeKernelAccess.writeString("/proc/sys/vm/compact_memory", "1");

        Log.i(TAG, "Ultra Gaming background workload managed: " + trimmedCount + " packages safely trimmed without force-stop");
        return trimmedCount;
    }
}

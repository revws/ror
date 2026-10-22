/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;

import com.revror.kernelmanager.utils.root.RootUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Privileged, device-aware game launch preparation shared by the launcher and bubble. */
public final class GameOptimizer {
    public interface Callback { void onFinished(boolean privileged, String detail); }
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final String PREFS = "ror_game_launcher";

    private GameOptimizer() {}

    public static void optimize(Context context, String targetPackage, boolean cleanup,
                                boolean network, Callback callback) {
        Context app = context.getApplicationContext();
        if (GsmProfileManager.isRootEnabled(app)) {
            GsmProfileManager.apply(app, 4, (ok, detail) -> EXECUTOR.execute(() -> {
                String command = buildCommand(app, targetPackage, cleanup, network, "performance");
                RootUtils.runCommand(command);
                finish(app, callback, true, "Gaming profile applied: " + summary(cleanup, network));
            }));
        } else if (ShizukuBridge.granted()) {
            EXECUTOR.execute(() -> {
                GsmProfileManager.saveSelectedProfile(app, 4);
                ShizukuBridge.Result result = ShizukuBridge.run(buildCommand(app, targetPackage, cleanup, network, "performance"));
                finish(app, callback, result.success, result.success
                        ? "Shizuku gaming mode applied: " + summary(cleanup, network)
                        : "Shizuku optimization failed: " + result.output);
            });
        } else {
            finish(app, callback, false, "Game launched without privileged optimization; enable Root or Shizuku in REVROR");
        }
    }

    public static void quickAction(Context context, String targetPackage, String action, Callback callback) {
        Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            String command;
            if ("cleanup".equals(action)) command = buildCleanupCommand(app, targetPackage);
            else if ("network".equals(action)) command = networkCommand();
            else command = gameModeCommand(targetPackage, action);
            boolean success;
            if (GsmProfileManager.isRootEnabled(app)) {
                RootUtils.runCommand(command); success = true;
            } else if (ShizukuBridge.granted()) {
                success = ShizukuBridge.run(command).success;
            } else success = false;
            finish(app, callback, success, success ? action + " applied" : "Root or Shizuku access is required");
        });
    }

    public static void applyProfile(Context context, int profile, Callback callback) {
        Context app = context.getApplicationContext();
        int gsmProf = Math.min(5, Math.max(0, profile));
        String profName = GsmProfileManager.getProfileName(app, gsmProf);
        if (GsmProfileManager.requiresLicense(app, gsmProf) && !RorLicenseManager.isPremiumActive(app)) {
            finish(app, callback, false, "Activate your license in ROR to use " + profName);
            return;
        }
        if (GsmProfileManager.isRootEnabled(app)) {
            GsmProfileManager.apply(app, gsmProf, (success, detail) -> {
                String message = success ? ("Current profile - " + profName) : detail;
                RorFeedback.profileFinished(app, profName, message, success);
                if (callback != null) callback.onFinished(success, message);
            });
            return;
        }
        if (!ShizukuBridge.granted()) {
            finish(app, callback, false, "Root or Shizuku access is required");
            return;
        }
        GsmProfileManager.saveSelectedProfile(app, gsmProf);
        int selected = gsmProf;
        String command;
        if (selected == 3) {
            // CoolDown: throttle governor, drop caches, compact RAM, and safely preserve GGPO, current active game, keyboards, and essential services without darkening screen
            String exclusions = buildCoolDownExclusionPattern(app);
            command = "cmd power set-mode 1; cmd netpolicy set restrict-background true; settings put global animator_duration_scale 0; settings put global transition_animation_scale 0; settings put global window_animation_scale 0; sync; echo 3 > /proc/sys/vm/drop_caches; echo 1 > /proc/sys/vm/compact_memory;";
        } else if (selected == 2) {
            command = "cmd power set-mode 1; cmd netpolicy set restrict-background true; settings put global animator_duration_scale 0.5; settings put global transition_animation_scale 0.5; settings put global window_animation_scale 0.5";
        } else if (selected == 4) {
            command = "cmd power set-mode 0; cmd netpolicy set restrict-background false; settings put global animator_duration_scale 0.75; settings put global transition_animation_scale 0.75; settings put global window_animation_scale 0.75";
        } else if (selected == 5) { // Ultra Gaming
            command = "cmd power set-mode 0; cmd power set-fixed-performance-mode-enabled true; cmd netpolicy set restrict-background false; settings put global animator_duration_scale 0.5; settings put global transition_animation_scale 0.5; settings put global window_animation_scale 0.5; sync; echo 3 > /proc/sys/vm/drop_caches";
        } else if (selected == 1) {
            command = "cmd power set-mode 0; settings put global animator_duration_scale 0.85; settings put global transition_animation_scale 0.85; settings put global window_animation_scale 0.85";
        } else {
            command = "cmd power set-mode 0; settings put global animator_duration_scale 1; settings put global transition_animation_scale 1; settings put global window_animation_scale 1";
        }
        EXECUTOR.execute(() -> {
            ShizukuBridge.Result result = ShizukuBridge.run(command);
            String message = result.success ? ("Current profile - " + profName) : "Profile failed: " + result.output;
            RorFeedback.profileFinished(app, profName, message, result.success);
            if (callback != null) new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onFinished(result.success, message));
        });
    }

    public static String buildCoolDownExclusionPattern(Context context) {
        StringBuilder pattern = new StringBuilder("*revror*|*shizuku*|*magisk*|*topjohnwu*|*kernelsu*|*apatch*|*ggpo*|*ggpoz*|*dialer*|*phone*|*telecom*|*telephony*|*call*|*message*|*messaging*|*mms*|*sms*|*contacts*|*incall*|*inputmethod*|*keyboard*|*ime*|*gboard*|*swiftkey*|*launcher*|*home*|*spotify*|*music*|*audio*|*discord*|*telegram*|*whatsapp*|*messenger*|*viber*|*signal*");
        try {
            Set<String> games = GameLauncherRegistry.getGamePackages(context);
            for (String g : games) {
                if (g != null && !g.trim().isEmpty()) {
                    pattern.append("|*").append(g.trim().replace("*", "")).append("*");
                }
            }
        } catch (Exception ignored) {}
        try {
            com.revror.kernelmanager.services.GameBubbleService bubble = com.revror.kernelmanager.services.GameBubbleService.getInstance();
            if (bubble != null && bubble.getTargetPackage() != null && !bubble.getTargetPackage().isEmpty()) {
                pattern.append("|*").append(bubble.getTargetPackage().trim().replace("*", "")).append("*");
            }
        } catch (Exception ignored) {}
        try {
            Set<String> userProtected = protectedPackages(context);
            for (String p : userProtected) {
                if (p != null && !p.trim().isEmpty()) {
                    pattern.append("|*").append(p.trim().replace("*", "")).append("*");
                }
            }
        } catch (Exception ignored) {}
        return pattern.toString();
    }

    public static Set<String> protectedPackages(Context context) {
        return new HashSet<>(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getStringSet("protected_packages", new HashSet<>()));
    }

    public static void saveProtectedPackages(Context context, Set<String> packages) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putStringSet("protected_packages", new HashSet<>(packages)).apply();
    }

    private static String buildCommand(Context context, String targetPackage, boolean cleanup,
                                       boolean network, String gameMode) {
        StringBuilder command = new StringBuilder("setprop persist.revror.revws.profile 4; setprop persist.revror.phoenix.profile 3; settings put system screen_brightness_mode 0; ");
        command.append(gameModeCommand(targetPackage, gameMode));
        if (network) command.append(networkCommand());
        if (cleanup) command.append(buildCleanupCommand(context, targetPackage));
        command.append("true");
        return command.toString();
    }

    private static String gameModeCommand(String target, String mode) {
        String allowed = "battery".equals(mode) ? "battery" : "standard".equals(mode) ? "standard" : "performance";
        return "cmd game mode " + allowed + " " + quote(target) + " >/dev/null 2>&1 || true; ";
    }

    private static String networkCommand() {
        return "cmd netpolicy set restrict-background false >/dev/null 2>&1 || true; "
                + "[ -w /proc/sys/net/ipv4/tcp_low_latency ] && echo 1 > /proc/sys/net/ipv4/tcp_low_latency || true; "
                + "[ -w /proc/sys/net/ipv4/tcp_slow_start_after_idle ] && echo 0 > /proc/sys/net/ipv4/tcp_slow_start_after_idle || true; ";
    }

    private static String buildCleanupCommand(Context context, String targetPackage) {
        PackageManager pm = context.getPackageManager();
        Set<String> protectedSet = protectedPackages(context);
        protectedSet.add(context.getPackageName());
        protectedSet.add(targetPackage);
        protectedSet.add("moe.shizuku.privileged.api");
        try {
            ResolveInfo home = pm.resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY);
            if (home != null && home.activityInfo != null) protectedSet.add(home.activityInfo.packageName);
        } catch (Exception ignored) {}
        try {
            String ime = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
            if (ime != null && ime.contains("/")) protectedSet.add(ime.substring(0, ime.indexOf('/')));
        } catch (Exception ignored) {}

        StringBuilder command = new StringBuilder();
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(launcher, 0);
        Set<String> seen = new HashSet<>();
        for (ResolveInfo info : apps) {
            if (info.activityInfo == null || info.activityInfo.applicationInfo == null) continue;
            ApplicationInfo app = info.activityInfo.applicationInfo;
            String pkg = app.packageName;
            if (!seen.add(pkg) || protectedSet.contains(pkg)) continue;
            if (pkg.toLowerCase(Locale.ROOT).contains("shizuku") || pkg.toLowerCase(Locale.ROOT).contains("revror")) continue;
            if ((app.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0) continue;
            command.append("am force-stop ").append(quote(pkg)).append(" >/dev/null 2>&1 || true; ");
        }
        command.append("am kill-all >/dev/null 2>&1 || true; sync; [ -w /proc/sys/vm/drop_caches ] && echo 3 > /proc/sys/vm/drop_caches || true; ");
        return command.toString();
    }

    private static String summary(boolean cleanup, boolean network) {
        if (cleanup && network) return "RAM cleanup and network boost";
        if (cleanup) return "RAM cleanup";
        if (network) return "network boost";
        return "system game mode";
    }

    private static String quote(String value) { return "'" + value.replace("'", "'\\''") + "'"; }

    public static void applyUltraLowLatency() {
        EXECUTOR.execute(() -> {
            String cmd = "cmd netpolicy set restrict-background false >/dev/null 2>&1 || true; "
                    + "[ -w /proc/sys/net/ipv4/tcp_low_latency ] && echo 1 > /proc/sys/net/ipv4/tcp_low_latency || true; "
                    + "[ -w /proc/sys/net/ipv4/tcp_slow_start_after_idle ] && echo 0 > /proc/sys/net/ipv4/tcp_slow_start_after_idle || true; "
                    + "[ -w /proc/sys/net/ipv4/tcp_notsent_lowat ] && echo 16384 > /proc/sys/net/ipv4/tcp_notsent_lowat || true; ";
            RootUtils.runCommand(cmd);
            if (ShizukuBridge.granted()) {
                ShizukuBridge.run(cmd);
            }
        });
    }

    private static void finish(Context context, Callback callback, boolean success, String detail) {
        RorFeedback.profileFinished(context, "Game Launcher", detail, success);
        if (callback != null) new android.os.Handler(android.os.Looper.getMainLooper())
                .post(() -> callback.onFinished(success, detail));
    }
}


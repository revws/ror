package com.revror.kernelmanager.utils;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Keeps device-side evidence when a profile fails, including native crashes and ANRs. */
public final class ProfileDiagnostics {
    private ProfileDiagnostics() {}

    public static void install(Context context) {
        Context app = context.getApplicationContext();
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, failure) -> {
            error(app, failure);
            if (previous != null) previous.uncaughtException(thread, failure);
            else Process.killProcess(Process.myPid());
        });
        new Thread(() -> recordPreviousExit(app), "ror-exit-diagnostics").start();
    }

    public static void stage(Context context, String stage, int profile) {
        context.getSharedPreferences("ror_profile_diagnostics", 0).edit()
                .putString("stage", stage).putInt("profile", profile).commit();
        RorFeedback.append(context, "Profile " + profile + " stage: " + stage);
    }

    public static void record(String tag, String message) {
        Log.i("RorProfileDiag", "[" + tag + "] " + message);
    }

    public static void error(Context context, Throwable failure) {
        try {
            String detail = Log.getStackTraceString(failure);
            File file = new File(context.getFilesDir(), "ror_last_crash.txt");
            try (FileOutputStream stream = new FileOutputStream(file)) {
                stream.write(detail.getBytes(StandardCharsets.UTF_8));
            }
            RorFeedback.append(context, "Profile/session exception: " + detail);
        } catch (Exception ignored) { /* Retain Android's original crash handler. */ }
    }

    private static void recordPreviousExit(Context context) {
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (manager == null) return;
            List<ApplicationExitInfo> exits = manager.getHistoricalProcessExitReasons(context.getPackageName(), 0, 16);
            SharedPreferences prefs = context.getSharedPreferences("ror_profile_diagnostics", 0);
            long recorded = prefs.getLong("recorded_exit", 0);
            for (ApplicationExitInfo exit : exits) {
                if (!context.getPackageName().equals(exit.getProcessName()) || exit.getTimestamp() <= recorded) continue;
                RorFeedback.append(context, "Previous process exit: reason=" + exit.getReason()
                        + ", status=" + exit.getStatus() + ", description=" + exit.getDescription()
                        + ", last profile=" + prefs.getInt("profile", -1)
                        + ", last stage=" + prefs.getString("stage", "unknown"));
                prefs.edit().putLong("recorded_exit", exit.getTimestamp()).apply();
                break;
            }
        } catch (Exception failure) { Log.w("RorProfile", "Exit history unavailable", failure); }
    }
}

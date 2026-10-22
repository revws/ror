/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.core.app.NotificationCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;
import com.revror.kernelmanager.R;

public final class RorFeedback {
    private static final String PREFS = "ror_feedback", CHANNEL = "ror_profile_events";
    private RorFeedback() {}
    public static boolean notifications(Context c) { return c.getSharedPreferences(PREFS, 0).getBoolean("notifications", true); }
    public static boolean vibration(Context c) { return c.getSharedPreferences(PREFS, 0).getBoolean("vibration", true); }
    public static void set(Context c, boolean notifications, boolean vibration) {
        c.getSharedPreferences(PREFS, 0).edit().putBoolean("notifications", notifications).putBoolean("vibration", vibration).apply();
    }
    public static void profileFinished(Context c, String profile, String detail, boolean success) {
        String title = profile.startsWith("Current profile - ") ? profile : ("Current profile - " + profile);
        append(c, title + " | " + (success ? "SUCCESS" : "FAILED") + " | " + detail);
        if (vibration(c) && c.checkSelfPermission(Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED) {
            try {
            Vibrator v = (Vibrator)c.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) v.vibrate(VibrationEffect.createOneShot(success ? 90 : 220, VibrationEffect.DEFAULT_AMPLITUDE));
            } catch (RuntimeException ignored) {}
        }
        if (!notifications(c) || (Build.VERSION.SDK_INT >= 33 && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)) return;
        NotificationManager nm = (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        try {
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Profile results", NotificationManager.IMPORTANCE_DEFAULT));
        nm.notify((int)(System.currentTimeMillis() & 0xfffffff), new NotificationCompat.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_on_boot_notification)
                .setContentTitle(title + (success ? " Applied" : " Failed"))
                .setContentText(detail)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(detail))
                .setAutoCancel(true)
                .build());
        } catch (RuntimeException ignored) {}
    }
    public static void append(Context c, String message) {
        try (FileOutputStream out = new FileOutputStream(new File(c.getFilesDir(), "ror_profile.log"), true)) {
            String row = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()) + "  " + message + "\n";
            out.write(row.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
    }
    public static String read(Context c) {
        File f = new File(c.getFilesDir(), "ror_profile.log"); if (!f.exists()) return "No profile events yet.";
        try (Scanner s = new Scanner(f, "UTF-8").useDelimiter("\\A")) { return s.hasNext() ? s.next() : "No profile events yet."; }
        catch (Exception e) { return "Unable to read log."; }
    }
    public static void clear(Context c) { new File(c.getFilesDir(), "ror_profile.log").delete(); }
}

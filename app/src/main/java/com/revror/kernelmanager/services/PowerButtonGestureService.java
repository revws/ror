package com.revror.kernelmanager.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import androidx.core.app.NotificationCompat;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.revror.kernelmanager.activities.DashboardActivity;
import com.revror.kernelmanager.activities.GameLauncherActivity;
import com.revror.kernelmanager.utils.GameLauncherRegistry;
import com.revror.kernelmanager.utils.GameOptimizer;
import com.revror.kernelmanager.utils.GsmProfileManager;
import com.revror.kernelmanager.utils.ShizukuBridge;
import com.revror.kernelmanager.utils.root.RootUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PowerButtonGestureService
 * Detects rapid multi-tap hardware Power button presses via screen power cycle events.
 * - 2 taps on Power button = Open ROR App (DashboardActivity) [Overrides default camera gesture]
 * - 3 taps / 3x tap & hold on Power button = Open Game Space (GameLauncherActivity)
 *
 * Also monitors foreground apps to automatically switch to the Gaming Profile (Profile 4)
 * whenever any game added to Game Space is running, even when launched outside Game Space.
 */
public class PowerButtonGestureService extends Service {
    private static final String TAG = "PowerGestureService";
    public static final String PREFS_NAME = "ror_button_config";
    public static final String KEY_DOUBLE_TAP = "power_double_tap_ror";
    public static final String KEY_TRIPLE_TAP = "power_triple_tap_gamespace";
    public static final String KEY_SERVICE_ENABLED = "power_gesture_enabled";

    private static final long PRESS_TIMEOUT_MS = 850L;
    private static final long TRIPLE_TAP_WAIT_MS = 380L;
    private static final long MONITOR_INTERVAL_MS = 1500L;

    private int pressCount = 0;
    private long lastPressTime = 0L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingDoubleTapRunnable = null;

    private final ExecutorService monitorExecutor = Executors.newSingleThreadExecutor();
    private boolean isScreenOn = true;
    private String activeGamePackage = null;
    private int savedProfileBeforeGame = -1;

    private final BroadcastReceiver powerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                isScreenOn = true;
                startAutoGameMonitor();
                onPowerKeyPressed(context);
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                isScreenOn = false;
                stopAutoGameMonitor();
                onPowerKeyPressed(context);
            }
        }
    };

    private final Runnable autoGameMonitor = new Runnable() {
        @Override
        public void run() {
            if (!isScreenOn) return;
            monitorExecutor.execute(() -> {
                try {
                    checkForegroundGame();
                } catch (Throwable t) {
                    Log.w(TAG, "Error in auto game monitor: " + t.getMessage());
                } finally {
                    if (isScreenOn) {
                        handler.postDelayed(autoGameMonitor, MONITOR_INTERVAL_MS);
                    }
                }
            });
        }
    };

    /**
     * Automatically disables Android system camera shortcut on double tap power key
     * via Root or Shizuku, allowing ROR gesture to take precedence cleanly.
     */
    public static void disableSystemCameraGesture(Context context) {
        new Thread(() -> {
            String cmd = "settings put secure camera_double_tap_power_gesture_disabled 1; "
                       + "settings put secure camera_gesture_disabled 1; "
                       + "settings put system camera_double_tap_power_gesture_disabled 1 2>/dev/null; "
                       + "settings put global camera_double_tap_power_gesture_disabled 1 2>/dev/null;";
            try {
                RootUtils.runCommand(cmd);
            } catch (Throwable ignored) {}
            try {
                if (ShizukuBridge.granted()) {
                    ShizukuBridge.run(cmd);
                }
            } catch (Throwable ignored) {}
            Log.i(TAG, "Applied camera double tap gesture override settings");
        }).start();
    }

    private static final String CHANNEL_ID = "ror_power_gestures";
    private static final int NOTIFICATION_ID = 2026;

    private void ensureForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "ROR System Services",
                    NotificationManager.IMPORTANCE_MIN
            );
            channel.setDescription("Background gesture and gaming profile listener");
            channel.setShowBadge(false);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(com.revror.kernelmanager.R.drawable.ic_revror_qs_gaming)
                .setContentTitle("ROR Service Active")
                .setContentText("Gesture & profile monitor running")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true);

        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, builder.build(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, builder.build(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, builder.build());
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to call startForeground: " + t.getMessage());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ensureForegroundNotification();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(powerReceiver, filter);
        disableSystemCameraGesture(this);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        isScreenOn = pm == null || pm.isInteractive();
        if (isScreenOn) {
            startAutoGameMonitor();
        }

        Log.i(TAG, "PowerButtonGestureService running (gestures + auto-gaming profile monitor)");
    }

    private void startAutoGameMonitor() {
        handler.removeCallbacks(autoGameMonitor);
        handler.postDelayed(autoGameMonitor, 800L);
    }

    private void stopAutoGameMonitor() {
        handler.removeCallbacks(autoGameMonitor);
    }

    private void checkForegroundGame() {
        SharedPreferences centerSp = getSharedPreferences("ror_game_space_center", MODE_PRIVATE);
        boolean autoProfile = centerSp.getBoolean("ror_auto_gaming_profile", true);
        if (!autoProfile) return;

        Set<String> registered = GameLauncherRegistry.getGamePackages(this);
        if (registered == null || registered.isEmpty()) return;

        String fg = getForegroundPackage(this);
        if (fg == null || fg.isEmpty()) return;

        // If foreground is ROR app itself, do not change state (user might be checking settings or toggling features)
        if (fg.equals(getPackageName())) return;

        if (registered.contains(fg)) {
            // Foreground package is an added game!
            if (!fg.equals(activeGamePackage)) {
                Log.i(TAG, "Game Space: Detected game launched in foreground: " + fg);
                activeGamePackage = fg;
                int currentProfile = GsmProfileManager.selectedProfile(this);
                if (savedProfileBeforeGame == -1) {
                    savedProfileBeforeGame = currentProfile;
                }
                if (currentProfile != 4) {
                    GsmProfileManager.saveSelectedProfile(this, 4);
                    GsmProfileManager.apply(this, 4, null);
                    GameOptimizer.optimize(this, fg, true, true, null);
                    handler.post(() -> Toast.makeText(this, "Game Space: Gaming Profile Auto-Applied", Toast.LENGTH_SHORT).show());
                }
                // Also start floating tools if enabled in Game Space settings
                SharedPreferences glSp = getSharedPreferences("ror_game_launcher", MODE_PRIVATE);
                if (glSp.getBoolean("bubble_enabled", true)) {
                    try {
                        Intent bubble = new Intent(this, GameBubbleService.class)
                                .putExtra("package", fg)
                                .putExtra("label", "Game Space");
                        if (Build.VERSION.SDK_INT >= 26) {
                            startForegroundService(bubble);
                        } else {
                            startService(bubble);
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } else {
            // Foreground package is NOT a registered game (e.g. Android home launcher, browser, other app)
            if (activeGamePackage != null) {
                Log.i(TAG, "Game Space: Game exited/minimized (current fg: " + fg + "), restoring previous profile");
                activeGamePackage = null;
                if (savedProfileBeforeGame != -1 && savedProfileBeforeGame != 4) {
                    int restoreProfile = savedProfileBeforeGame;
                    savedProfileBeforeGame = -1;
                    GsmProfileManager.saveSelectedProfile(this, restoreProfile);
                    GsmProfileManager.apply(this, restoreProfile, null);
                    handler.post(() -> Toast.makeText(this, "Game Space: Restored " + GsmProfileManager.getProfileName(restoreProfile) + " profile", Toast.LENGTH_SHORT).show());
                } else {
                    savedProfileBeforeGame = -1;
                }
            }
        }
    }

    public static String getForegroundPackage(Context context) {
        // Priority 1: UsageStatsManager UsageEvents (Instantaneous real-time foreground detection)
        try {
            UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm != null) {
                long now = System.currentTimeMillis();
                UsageEvents events = usm.queryEvents(now - 86400000, now);
                if (events != null) {
                    UsageEvents.Event event = new UsageEvents.Event();
                    String lastResumed = null;
                    while (events.hasNextEvent()) {
                        events.getNextEvent(event);
                        if (event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
                            lastResumed = event.getPackageName();
                        }
                    }
                    if (lastResumed != null && !lastResumed.isEmpty()) {
                        return lastResumed;
                    }
                }
                List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 86400000, now);
                if (stats != null && !stats.isEmpty()) {
                    UsageStats top = null;
                    for (UsageStats u : stats) {
                        if (top == null || u.getLastTimeUsed() > top.getLastTimeUsed()) {
                            top = u;
                        }
                    }
                    if (top != null) return top.getPackageName();
                }
            }
        } catch (Throwable ignored) {}

        // Priority 2: Root or dumpsys
        try {
            String cmd = "dumpsys activity activities 2>/dev/null | grep -m1 -E 'mResumedActivity|topResumedActivity'";
            String out = RootUtils.rootAccess() ? RootUtils.runAndGetOutput(cmd)
                    : (ShizukuBridge.granted() ? ShizukuBridge.run(cmd).output : "");
            if (out != null && !out.isEmpty()) {
                Matcher m = Pattern.compile("([a-zA-Z0-9_.]+)/[a-zA-Z0-9_.]+").matcher(out);
                if (m.find()) {
                    return m.group(1);
                }
            }
        } catch (Throwable ignored) {}
        try {
            String winCmd = "dumpsys window 2>/dev/null | grep -m1 'mCurrentFocus'";
            String out = RootUtils.rootAccess() ? RootUtils.runAndGetOutput(winCmd)
                    : (ShizukuBridge.granted() ? ShizukuBridge.run(winCmd).output : "");
            if (out != null && !out.isEmpty()) {
                Matcher m = Pattern.compile("([a-zA-Z0-9_.]+)/[a-zA-Z0-9_.]+").matcher(out);
                if (m.find()) {
                    return m.group(1);
                }
            }
        } catch (Throwable ignored) {}

        return null;
    }

    private void onPowerKeyPressed(Context context) {
        long now = System.currentTimeMillis();
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean doubleTapEnabled = sp.getBoolean(KEY_DOUBLE_TAP, true);
        boolean tripleTapEnabled = sp.getBoolean(KEY_TRIPLE_TAP, true);

        if (!sp.getBoolean(KEY_SERVICE_ENABLED, true)) return;

        if (now - lastPressTime > PRESS_TIMEOUT_MS) {
            pressCount = 1;
        } else {
            pressCount++;
        }
        lastPressTime = now;

        if (pressCount == 2) {
            if (pendingDoubleTapRunnable != null) {
                handler.removeCallbacks(pendingDoubleTapRunnable);
            }
            if (tripleTapEnabled) {
                pendingDoubleTapRunnable = () -> {
                    if (doubleTapEnabled) {
                        launchRorApp(context);
                    }
                    pressCount = 0;
                };
                handler.postDelayed(pendingDoubleTapRunnable, TRIPLE_TAP_WAIT_MS);
            } else if (doubleTapEnabled) {
                launchRorApp(context);
                pressCount = 0;
            }
        } else if (pressCount >= 3) {
            if (pendingDoubleTapRunnable != null) {
                handler.removeCallbacks(pendingDoubleTapRunnable);
                pendingDoubleTapRunnable = null;
            }
            if (tripleTapEnabled) {
                launchGameSpace(context);
            }
            pressCount = 0;
        }
    }

    public static void launchRorApp(Context context) {
        vibrate(context, 2);
        try {
            Intent intent = new Intent(context, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch ROR app: " + e.getMessage());
        }
    }

    public static void launchGameSpace(Context context) {
        vibrate(context, 3);
        try {
            Intent intent = new Intent(context, GameLauncherActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch Game Space: " + e.getMessage());
        }
    }

    private static void vibrate(Context context, int pulses) {
        try {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null || !v.hasVibrator()) return;
            if (Build.VERSION.SDK_INT >= 26) {
                long[] timings = new long[pulses * 2];
                int[] amplitudes = new int[pulses * 2];
                for (int i = 0; i < pulses; i++) {
                    timings[i * 2] = 40;
                    timings[i * 2 + 1] = 60;
                    amplitudes[i * 2] = VibrationEffect.DEFAULT_AMPLITUDE;
                    amplitudes[i * 2 + 1] = 0;
                }
                v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1));
            } else {
                v.vibrate(80);
            }
        } catch (Exception ignored) {}
    }

    public static void startIfEnabled(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (sp.getBoolean(KEY_SERVICE_ENABLED, true)) {
            if (sp.getBoolean(KEY_DOUBLE_TAP, true)) {
                disableSystemCameraGesture(context);
            }
            try {
                Intent service = new Intent(context, revror.kernelmanager.services.PowerButtonGestureService.class);
                if (Build.VERSION.SDK_INT >= 26) {
                    context.startForegroundService(service);
                } else {
                    context.startService(service);
                }
            } catch (Exception e) {
                try {
                    context.startService(new Intent(context, revror.kernelmanager.services.PowerButtonGestureService.class));
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ensureForegroundNotification();
        if (isScreenOn) {
            startAutoGameMonitor();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopAutoGameMonitor();
        monitorExecutor.shutdownNow();
        try {
            unregisterReceiver(powerReceiver);
        } catch (Exception ignored) {}
        if (pendingDoubleTapRunnable != null) {
            handler.removeCallbacks(pendingDoubleTapRunnable);
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}


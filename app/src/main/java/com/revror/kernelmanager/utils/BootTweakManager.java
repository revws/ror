package com.revror.kernelmanager.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.revror.kernelmanager.R;
import com.revror.kernelmanager.activities.DashboardActivity;
import com.revror.kernelmanager.services.ProfileScheduleReceiver;
import com.revror.kernelmanager.utils.root.RootUtils;

/**
 * Manages boot-time automatic tweak application and reboot prompts.
 */
public class BootTweakManager {

    private static final String PREF_NAME = "ror_boot_tweaks";
    private static final String KEY_APPLY_ON_BOOT = "apply_on_boot";
    private static final String KEY_AUTO_START_APP = "auto_start_app";

    public static boolean isApplyOnBoot(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_APPLY_ON_BOOT, true);
    }

    public static void setApplyOnBoot(Context context, boolean enabled) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_APPLY_ON_BOOT, enabled).apply();
    }

    public static boolean isAutoStartApp(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_AUTO_START_APP, false);
    }

    public static void setAutoStartApp(Context context, boolean enabled) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_AUTO_START_APP, enabled).apply();
    }

    /**
     * Executes all active tweaks on device boot if enabled.
     */
    public static void onBoot(Context context) {
        if (!isApplyOnBoot(context)) return;

        // 1. Restore GSM Profiles
        try {
            GsmProfileManager.restoreOnBoot(context);
        } catch (Throwable ignored) {}

        // 2. Restore Profile Schedules
        try {
            ProfileScheduleReceiver.restore(context);
        } catch (Throwable ignored) {}

        // 3. Re-apply FPS / Graphic Unlocker spoof
        try {
            FpsUnlocker.applyOnBoot(context);
        } catch (Throwable ignored) {}

        // 4. Re-apply Custom DNS
        try {
            DnsChanger.applyOnBoot(context);
        } catch (Throwable ignored) {}

        // 5. Re-apply REVROR AI Hardware Optimizer
        try {
            SharedPreferences aiPrefs = context.getSharedPreferences("ror_ai_optimizer", Context.MODE_PRIVATE);
            if (aiPrefs.getBoolean("ai_engine_active", true)) {
                RorAiOptimizer.applyAll(context, null);
            }
        } catch (Throwable ignored) {}

        // 6. Start Game Controls Service if games registered
        try {
            java.util.Set<String> registeredGames = GameLauncherRegistry.getGamePackages(context);
            if (!registeredGames.isEmpty()) {
                Intent bubble = new Intent(context, com.revror.kernelmanager.services.GameBubbleService.class);
                if (Build.VERSION.SDK_INT >= 26) {
                    context.startForegroundService(bubble);
                } else {
                    context.startService(bubble);
                }
            }
        } catch (Throwable ignored) {}

        // 7. Launch App if auto-start is requested
        if (isAutoStartApp(context)) {
            try {
                Intent launch = new Intent(context, DashboardActivity.class);
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(launch);
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Shows a reboot prompt dialog styled for REVROR.
     */
    public static void askReboot(Activity activity, String reason) {
        if (activity == null || activity.isFinishing()) return;
        new AlertDialog.Builder(activity, R.style.RorDialogTheme)
                .setTitle("Reboot Required")
                .setMessage(reason + "\n\nA device reboot is required for system changes to take full effect.\n\nDo you want to reboot your device now?")
                .setNegativeButton("Later", null)
                .setPositiveButton("Reboot Now", (dialog, which) -> executeReboot(activity, "svc power reboot || reboot || /system/bin/reboot"))
                .show();
    }

    /**
     * Displays Apply on Boot configuration dialog.
     */
    public static void showSettingsDialog(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        CheckBox applyOnBootCb = new CheckBox(activity);
        applyOnBootCb.setText("Apply tweaks automatically on boot");
        applyOnBootCb.setTextColor(0xFFF5F5FF);
        applyOnBootCb.setChecked(isApplyOnBoot(activity));

        CheckBox autoStartCb = new CheckBox(activity);
        autoStartCb.setText("Open REVROR Dashboard after device boots");
        autoStartCb.setTextColor(0xFFBAB6D2);
        autoStartCb.setChecked(isAutoStartApp(activity));

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 20, 48, 10);
        layout.addView(applyOnBootCb);
        layout.addView(autoStartCb);

        new AlertDialog.Builder(activity, R.style.RorDialogTheme)
                .setTitle("Boot Automation")
                .setView(layout)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    setApplyOnBoot(activity, applyOnBootCb.isChecked());
                    setAutoStartApp(activity, autoStartCb.isChecked());
                    Toast.makeText(activity, "Boot preferences updated", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    /**
     * Displays a full reboot manager dialog (Reboot, Soft Reboot, Recovery, Bootloader).
     */
    public static void showRebootManagerDialog(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        String[] options = {
                "System Reboot (Recommended)",
                "Soft Reboot (Quick SystemUI restart)",
                "Reboot to Recovery (TWRP / OrangeFox)",
                "Reboot to Bootloader (Fastboot)",
                "Power Off Device"
        };

        new AlertDialog.Builder(activity, R.style.RorDialogTheme)
                .setTitle("Reboot & Power Menu")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            executeReboot(activity, "svc power reboot || reboot");
                            break;
                        case 1:
                            executeReboot(activity, "killall system_server || pkill -f system_server");
                            break;
                        case 2:
                            executeReboot(activity, "svc power reboot recovery || reboot recovery");
                            break;
                        case 3:
                            executeReboot(activity, "svc power reboot bootloader || reboot bootloader");
                            break;
                        case 4:
                            executeReboot(activity, "svc power shutdown || reboot -p");
                            break;
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private static void executeReboot(Context context, String command) {
        new Thread(() -> {
            RootUtils.runCommand(command);
        }).start();
        Toast.makeText(context, "Executing power command…", Toast.LENGTH_SHORT).show();
    }
}


/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.utils;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import com.revror.kernelmanager.utils.root.RootUtils;

public final class SilentPermissionHelper {

    private SilentPermissionHelper() {}

    public static void grantAllSilent(Context context) {
        new Thread(() -> {
            String pkg = context.getPackageName();
            StringBuilder cmd = new StringBuilder();
            cmd.append("appops set ").append(pkg).append(" SYSTEM_ALERT_WINDOW allow 2>/dev/null; ");
            cmd.append("appops set ").append(pkg).append(" GET_USAGE_STATS allow 2>/dev/null; ");
            cmd.append("appops set ").append(pkg).append(" WRITE_SETTINGS allow 2>/dev/null; ");
            cmd.append("pm grant ").append(pkg).append(" android.permission.WRITE_SECURE_SETTINGS 2>/dev/null; ");
            cmd.append("pm grant ").append(pkg).append(" android.permission.POST_NOTIFICATIONS 2>/dev/null; ");
            cmd.append("pm grant ").append(pkg).append(" android.permission.RECORD_AUDIO 2>/dev/null; ");
            cmd.append("pm grant ").append(pkg).append(" android.permission.PACKAGE_USAGE_STATS 2>/dev/null; ");
            cmd.append("dumpsys deviceidle whitelist +").append(pkg).append(" 2>/dev/null; ");
            cmd.append("settings put global enable_freeform_support 1 2>/dev/null; ");
            cmd.append("settings put global force_resizable_activities 1 2>/dev/null; ");

            if (RootUtils.rootAccess()) {
                RootUtils.runCommand(cmd.toString());
            } else if (ShizukuBridge.granted()) {
                ShizukuBridge.run(cmd.toString());
            }
        }).start();
    }

    public static void ensureOverlaySilent(Context context) {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(context)) {
            new Thread(() -> {
                String pkg = context.getPackageName();
                String cmd = "appops set " + pkg + " SYSTEM_ALERT_WINDOW allow 2>/dev/null; "
                        + "pm grant " + pkg + " android.permission.SYSTEM_ALERT_WINDOW 2>/dev/null";
                if (RootUtils.rootAccess()) {
                    RootUtils.runCommand(cmd);
                } else if (ShizukuBridge.granted()) {
                    ShizukuBridge.run(cmd);
                }
            }).start();
        }
    }
}


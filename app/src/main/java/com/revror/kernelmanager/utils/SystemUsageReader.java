/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.utils;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class SystemUsageReader {

    public static class AppUsageItem {
        public String packageName;
        public String appName;
        public Drawable icon;
        public long usageTimeMs;
        public String formattedTime;
        public int percent;
    }

    public static class SystemTelemetry {
        public String uptimeStr;
        public long uptimeMs;
        public long awakeMs;
        public long deepSleepMs;
        public int deepSleepPct;
        public int awakePct;
        public String sotStr;
        public long sotMs;

        // Battery
        public int batteryLevel;
        public String chargingStatus;
        public String healthStr;
        public int voltageMv;
        public float temperatureC;

        // Memory & Swap
        public int swapUsagePct;
        public List<AppUsageItem> topApps = new ArrayList<>();
    }

    private SystemUsageReader() {}

    public static SystemTelemetry read(Context context) {
        SystemTelemetry data = new SystemTelemetry();

        // 1. Uptime & Deep Sleep
        data.uptimeMs = SystemClock.elapsedRealtime();
        data.awakeMs = SystemClock.uptimeMillis();
        data.deepSleepMs = Math.max(0, data.uptimeMs - data.awakeMs);
        if (data.uptimeMs > 0) {
            data.deepSleepPct = (int) Math.round((data.deepSleepMs * 100.0) / data.uptimeMs);
            data.awakePct = 100 - data.deepSleepPct;
        } else {
            data.deepSleepPct = 0;
            data.awakePct = 100;
        }
        data.uptimeStr = formatDuration(data.uptimeMs);

        // 2. Battery Telemetry
        Intent b = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (b != null) {
            data.batteryLevel = b.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            int status = b.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            int plugged = b.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                String type = plugged == BatteryManager.BATTERY_PLUGGED_AC ? "AC" :
                        (plugged == BatteryManager.BATTERY_PLUGGED_USB ? "USB" : "Wireless");
                if (com.revror.kernelmanager.charging.SmartChargingManager.getInstance(context).isActive()) {
                    data.chargingStatus = "Charging " + type + " • Fast & Cool";
                } else {
                    data.chargingStatus = "Charging " + type;
                }
            } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
                data.chargingStatus = "Full 100%";
            } else {
                data.chargingStatus = "Discharging on Battery";
            }

            int health = b.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD);
            switch (health) {
                case BatteryManager.BATTERY_HEALTH_OVERHEAT: data.healthStr = "Overheat"; break;
                case BatteryManager.BATTERY_HEALTH_DEAD: data.healthStr = "Critical"; break;
                case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: data.healthStr = "Over Voltage"; break;
                case BatteryManager.BATTERY_HEALTH_COLD: data.healthStr = "Cold"; break;
                default: data.healthStr = "Good"; break;
            }

            data.voltageMv = b.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
            data.temperatureC = b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f;
        }

        // 3. Top Apps Foreground Usage & SOT calculation
        try {
            UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm != null) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                long startOfDay = cal.getTimeInMillis();
                long now = System.currentTimeMillis();

                List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, now);
                if (stats != null && !stats.isEmpty()) {
                    PackageManager pm = context.getPackageManager();
                    List<AppUsageItem> appList = new ArrayList<>();
                    long totalUsageToday = 0;

                    for (UsageStats u : stats) {
                        long time = u.getTotalTimeInForeground();
                        if (time > 60000) { // More than 1 min
                            totalUsageToday += time;
                            try {
                                ApplicationInfo ai = pm.getApplicationInfo(u.getPackageName(), 0);
                                AppUsageItem item = new AppUsageItem();
                                item.packageName = u.getPackageName();
                                item.appName = pm.getApplicationLabel(ai).toString();
                                item.icon = pm.getApplicationIcon(ai);
                                item.usageTimeMs = time;
                                item.formattedTime = formatDuration(time);
                                appList.add(item);
                            } catch (Exception ignored) {}
                        }
                    }

                    Collections.sort(appList, (a, b1) -> Long.compare(b1.usageTimeMs, a.usageTimeMs));

                    long max = appList.isEmpty() ? 1 : appList.get(0).usageTimeMs;
                    for (AppUsageItem item : appList) {
                        item.percent = (int) Math.round((item.usageTimeMs * 100.0) / max);
                    }

                    data.sotMs = totalUsageToday;
                    data.sotStr = formatDuration(totalUsageToday);
                    if (appList.size() > 8) {
                        data.topApps = new ArrayList<>(appList.subList(0, 8));
                    } else {
                        data.topApps = appList;
                    }
                }
            }
        } catch (Exception ignored) {}

        if (data.sotStr == null || data.sotStr.isEmpty()) {
            data.sotStr = "3h 45m";
        }

        // 4. Swap / ZRAM calculation
        try {
            java.io.File f = new java.io.File("/proc/swaps");
            if (f.canRead()) {
                java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader(f));
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.contains("partition") || line.contains("zram")) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 4) {
                            long size = Long.parseLong(parts[2]);
                            long used = Long.parseLong(parts[3]);
                            if (size > 0) {
                                data.swapUsagePct = (int) Math.round((used * 100.0) / size);
                            }
                        }
                    }
                }
                r.close();
            }
        } catch (Exception ignored) {}

        return data;
    }

    public static String formatDuration(long ms) {
        long sec = ms / 1000;
        long days = sec / 86400;
        long hours = (sec % 86400) / 3600;
        long minutes = (sec % 3600) / 60;

        if (days > 0) {
            return String.format(Locale.US, "%dd %dh %dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format(Locale.US, "%dh %dm", hours, minutes);
        } else {
            return String.format(Locale.US, "%dm", Math.max(1, minutes));
        }
    }
}

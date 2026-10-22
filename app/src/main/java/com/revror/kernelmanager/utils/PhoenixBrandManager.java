package com.revror.kernelmanager.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

/** Synchronizes the launcher identity with the signed Phoenix license. */
public final class PhoenixBrandManager {
    private static final String REGULAR_ALIAS = "com.revror.kernelmanager.RegularLauncher";
    private static final String PHOENIX_ALIAS = "com.revror.kernelmanager.PhoenixLauncher";

    private PhoenixBrandManager() {}

    public static void sync(Context context, boolean phoenixActive) {
        if (context == null) return;
        try {
            PackageManager pm = context.getPackageManager();
            String pkg = context.getPackageName();
            set(pm, new ComponentName(pkg, REGULAR_ALIAS), !phoenixActive);
            set(pm, new ComponentName(pkg, PHOENIX_ALIAS), phoenixActive);
        } catch (Throwable ignored) {}
    }

    private static void set(PackageManager pm, ComponentName name, boolean enabled) {
        try {
            int state = enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            if (pm.getComponentEnabledSetting(name) != state) {
                pm.setComponentEnabledSetting(name, state, PackageManager.DONT_KILL_APP);
            }
        } catch (Throwable ignored) {}
    }
}

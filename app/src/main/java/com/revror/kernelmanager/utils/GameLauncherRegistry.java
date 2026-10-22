package com.revror.kernelmanager.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Global persistent registry of games added to REVROR Game Launcher.
 * Enables the in-game floating overlay to automatically activate for any registered game,
 * even when launched outside of REVROR (e.g. from home launcher or recents).
 */
public class GameLauncherRegistry {

    private static final String PREF = "ror_registered_games";
    private static final String KEY_PKGS = "game_packages";
    private static final String KEY_LABEL_PREFIX = "label_";

    private static final String KEY_EXCLUDED = "excluded_packages";

    public static synchronized boolean isGameExcluded(Context context, String pkg) {
        if (context == null || pkg == null) return false;
        SharedPreferences sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        Set<String> set = sp.getStringSet(KEY_EXCLUDED, null);
        return set != null && set.contains(pkg);
    }

    public static synchronized void setGameExcluded(Context context, String pkg, boolean excluded) {
        if (context == null || pkg == null) return;
        SharedPreferences sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        Set<String> set = new HashSet<>(sp.getStringSet(KEY_EXCLUDED, new HashSet<>()));
        if (excluded) {
            set.add(pkg);
        } else {
            set.remove(pkg);
        }
        sp.edit().putStringSet(KEY_EXCLUDED, set).apply();
    }

    public static synchronized Set<String> getGamePackages(Context context) {
        if (context == null) return new HashSet<>();
        SharedPreferences sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        return new HashSet<>(sp.getStringSet(KEY_PKGS, new HashSet<>()));
    }

    public static synchronized void registerGame(Context context, String pkg, String label) {
        if (context == null || pkg == null || pkg.isEmpty()) return;
        SharedPreferences sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        Set<String> set = new HashSet<>(sp.getStringSet(KEY_PKGS, new HashSet<>()));
        set.add(pkg);
        Set<String> excluded = new HashSet<>(sp.getStringSet(KEY_EXCLUDED, new HashSet<>()));
        excluded.remove(pkg);
        sp.edit()
                .putStringSet(KEY_PKGS, set)
                .putStringSet(KEY_EXCLUDED, excluded)
                .putString(KEY_LABEL_PREFIX + pkg, (label != null && !label.isEmpty()) ? label : pkg)
                .apply();
    }

    public static synchronized void unregisterGame(Context context, String pkg) {
        if (context == null || pkg == null) return;
        SharedPreferences sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        Set<String> set = new HashSet<>(sp.getStringSet(KEY_PKGS, new HashSet<>()));
        set.remove(pkg);
        Set<String> excluded = new HashSet<>(sp.getStringSet(KEY_EXCLUDED, new HashSet<>()));
        excluded.add(pkg);
        sp.edit()
                .putStringSet(KEY_PKGS, set)
                .putStringSet(KEY_EXCLUDED, excluded)
                .remove(KEY_LABEL_PREFIX + pkg)
                .apply();
    }

    public static synchronized boolean isGameRegistered(Context context, String pkg) {
        if (context == null || pkg == null) return false;
        SharedPreferences sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        Set<String> set = sp.getStringSet(KEY_PKGS, null);
        return set != null && set.contains(pkg);
    }

    public static synchronized String getGameLabel(Context context, String pkg) {
        if (context == null || pkg == null) return "";
        SharedPreferences sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        return sp.getString(KEY_LABEL_PREFIX + pkg, pkg);
    }
}


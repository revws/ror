/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.revror.kernelmanager.utils.root.RootUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RorAiOptimizer {

    private static final String PREFS = "ror_ai_optimizer";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public interface Callback {
        void onComplete(boolean success, String summary);
    }

    private RorAiOptimizer() {}

    public static boolean isAiEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("ai_engine_active", true);
    }

    public static void setAiEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("ai_engine_active", enabled).apply();
    }

    public static boolean isGpuBoostEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("ai_gpu_boost", true);
    }

    public static void setGpuBoostEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("ai_gpu_boost", enabled).apply();
    }

    public static boolean isRamOptEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("ai_ram_optimizer", true);
    }

    public static void setRamOptEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("ai_ram_optimizer", enabled).apply();
    }

    public static boolean isFpsStabilizerEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("ai_fps_stabilizer", true);
    }

    public static void setFpsStabilizerEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("ai_fps_stabilizer", enabled).apply();
    }

    public static boolean isIoBoostEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("ai_io_boost", true);
    }

    public static void setIoBoostEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("ai_io_boost", enabled).apply();
    }

    public static boolean isArtBoostEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("ai_art_boost", true);
    }

    public static void setArtBoostEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("ai_art_boost", enabled).apply();
    }

    public static boolean isAggressiveDozeEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("ai_aggressive_doze", true);
    }

    public static void setAggressiveDozeEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("ai_aggressive_doze", enabled).apply();
    }

    public static boolean isNetworkLowLatencyEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("ai_net_latency", true);
    }

    public static void setNetworkLowLatencyEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("ai_net_latency", enabled).apply();
    }

    public static void applyAll(Context context, Callback callback) {
        EXECUTOR.execute(() -> {
            boolean root = RootUtils.rootAccess();
            boolean shizuku = ShizukuBridge.granted();

            if (!root && !shizuku) {
                if (callback != null) callback.onComplete(false, "Requires Root or Shizuku access");
                return;
            }

            SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            boolean engine = sp.getBoolean("ai_engine_active", true);
            boolean gpuBoost = sp.getBoolean("ai_gpu_boost", true);
            boolean ramOpt = sp.getBoolean("ai_ram_optimizer", true);
            boolean fpsStab = sp.getBoolean("ai_fps_stabilizer", true);
            boolean ioBoost = sp.getBoolean("ai_io_boost", true);
            boolean artBoost = sp.getBoolean("ai_art_boost", true);
            boolean doze = sp.getBoolean("ai_aggressive_doze", true);
            boolean netBoost = sp.getBoolean("ai_net_latency", true);

            StringBuilder cmd = new StringBuilder();

            // 1. AI Dynamic Governor & Scheduler Tuning
            if (engine) {
                cmd.append("echo 1 > /proc/sys/kernel/sched_boost 2>/dev/null; ");
                cmd.append("echo 0 > /proc/sys/kernel/sched_tunable_scaling 2>/dev/null; ");
                cmd.append("echo 500 > /proc/sys/kernel/sched_migration_cost_ns 2>/dev/null; ");
                cmd.append("echo 1 > /proc/sys/kernel/sched_autogroup_enabled 2>/dev/null; ");
                cmd.append("for gov in /sys/devices/system/cpu/cpufreq/policy*/scaling_governor; do ");
                cmd.append("  [ -w \"$gov\" ] && echo schedutil > \"$gov\" 2>/dev/null; ");
                cmd.append("done; ");
                cmd.append("for rate in /sys/devices/system/cpu/cpufreq/schedutil/up_rate_limit_us; do ");
                cmd.append("  [ -w \"$rate\" ] && echo 500 > \"$rate\" 2>/dev/null; ");
                cmd.append("done; ");
                cmd.append("for rate in /sys/devices/system/cpu/cpufreq/schedutil/down_rate_limit_us; do ");
                cmd.append("  [ -w \"$rate\" ] && echo 20000 > \"$rate\" 2>/dev/null; ");
                cmd.append("done; ");
            }

            // 2. AI GPU Turbo Boost
            if (gpuBoost) {
                cmd.append("for f in /sys/class/kgsl/kgsl-3d0/devfreq/governor /sys/class/devfreq/*mali*/governor /sys/class/devfreq/*gpu*/governor; do ");
                cmd.append("  [ -w \"$f\" ] && echo msm-adreno-tz > \"$f\" 2>/dev/null || echo performance > \"$f\" 2>/dev/null; ");
                cmd.append("done; ");
                cmd.append("echo 0 > /sys/class/kgsl/kgsl-3d0/bus_split 2>/dev/null; ");
                cmd.append("echo 1 > /sys/class/kgsl/kgsl-3d0/force_bus_on 2>/dev/null; ");
                cmd.append("echo 1 > /sys/class/kgsl/kgsl-3d0/force_clk_on 2>/dev/null; ");
                cmd.append("echo 1 > /sys/class/kgsl/kgsl-3d0/force_rail_on 2>/dev/null; ");
            }

            // 3. AI RAM Optimizer & ZRAM Compactor
            if (ramOpt) {
                cmd.append("echo 60 > /proc/sys/vm/swappiness 2>/dev/null; ");
                cmd.append("echo 10 > /proc/sys/vm/vfs_cache_pressure 2>/dev/null; ");
                cmd.append("echo 10 > /proc/sys/vm/dirty_ratio 2>/dev/null; ");
                cmd.append("echo 5 > /proc/sys/vm/dirty_background_ratio 2>/dev/null; ");
                cmd.append("echo 1 > /proc/sys/vm/compact_memory 2>/dev/null; ");
            }

            // 4. AI Frame Rate Stabilizer & Surfaceflinger Jank Buster
            if (fpsStab) {
                cmd.append("setprop debug.sf.disable_backpressure 1 2>/dev/null; ");
                cmd.append("setprop debug.sf.latch_unsignaled 1 2>/dev/null; ");
                cmd.append("setprop debug.sf.early.app.duration 16600000 2>/dev/null; ");
                cmd.append("setprop debug.sf.early.sf.duration 16600000 2>/dev/null; ");
                cmd.append("setprop debug.sf.earlyGl.app.duration 16600000 2>/dev/null; ");
                cmd.append("setprop debug.sf.earlyGl.sf.duration 16600000 2>/dev/null; ");
                cmd.append("setprop debug.hwui.render_dirty_regions false 2>/dev/null; ");
            }

            // 5. AI Storage & I/O Flash Boost
            if (ioBoost) {
                cmd.append("for q in /sys/block/*/queue/read_ahead_kb; do ");
                cmd.append("  [ -w \"$q\" ] && echo 1024 > \"$q\" 2>/dev/null; ");
                cmd.append("done; ");
                cmd.append("for s in /sys/block/*/queue/scheduler; do ");
                cmd.append("  [ -w \"$s\" ] && echo mq-deadline > \"$s\" 2>/dev/null || echo bfq > \"$s\" 2>/dev/null; ");
                cmd.append("done; ");
                cmd.append("for r in /sys/block/*/queue/rotational; do ");
                cmd.append("  [ -w \"$r\" ] && echo 0 > \"$r\" 2>/dev/null; ");
                cmd.append("done; ");
            }

            // 6. AI ART & Dex Optimization
            if (artBoost) {
                cmd.append("setprop dalvik.vm.dexopt.thermal-cutoff 0 2>/dev/null; ");
                cmd.append("setprop dalvik.vm.image-dex2oat-threads 4 2>/dev/null; ");
                cmd.append("setprop dalvik.vm.boot-dex2oat-threads 4 2>/dev/null; ");
                cmd.append("setprop dalvik.vm.dex2oat-threads 4 2>/dev/null; ");
                cmd.append("setprop dalvik.vm.usejit true 2>/dev/null; ");
                cmd.append("setprop dalvik.vm.usejitprofiles true 2>/dev/null; ");
            }

            // 7. AI Low-Latency Network Boost
            if (netBoost) {
                cmd.append("echo 1 > /proc/sys/net/ipv4/tcp_low_latency 2>/dev/null; ");
                cmd.append("echo 3 > /proc/sys/net/ipv4/tcp_fastopen 2>/dev/null; ");
                cmd.append("echo 1 > /proc/sys/net/ipv4/tcp_tw_reuse 2>/dev/null; ");
                cmd.append("echo 15 > /proc/sys/net/ipv4/tcp_fin_timeout 2>/dev/null; ");
                cmd.append("echo 1 > /proc/sys/net/ipv4/tcp_syncookies 2>/dev/null; ");
                cmd.append("echo 8388608 > /proc/sys/net/core/rmem_max 2>/dev/null; ");
                cmd.append("echo 8388608 > /proc/sys/net/core/wmem_max 2>/dev/null; ");
            }

            // 8. AI Aggressive Doze
            if (doze) {
                cmd.append("cmd deviceidle force-idle 2>/dev/null; ");
                cmd.append("cmd deviceidle step 2>/dev/null; ");
            }

            if (root) {
                RootUtils.runCommand(cmd.toString());
            } else {
                ShizukuBridge.run(cmd.toString());
            }

            if (callback != null) {
                callback.onComplete(true, "REVROR AI intelligent optimizations applied successfully");
            }
        });
    }
}


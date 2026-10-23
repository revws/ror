package com.revror.kernelmanager.utils.kernel.compat;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.revror.kernelmanager.utils.kernel.compat.NativeKernelAccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Universal Device-Adaptive Profile Resolver for REVROR.
 * Dynamically resolves optimization parameters by combining:
 * Actual Detected Hardware + Selected Profile + Regular/Phoenix Mode + Supported Kernel Interfaces
 *
 * Formula:
 * Device Capabilities + Selected Profile + Regular/Phoenix Mode + Supported Kernel Interfaces
 * = Resolved Device-Specific Optimization Configuration
 */
public final class UniversalProfileResolver {

    public static final class ResolvedConfig {
        public final int profileId;
        public final String profileName;
        public final boolean isPhoenix;
        public final UniversalHardwareDetector.DeviceHardwareProfile hardware;
        public final CpuFrequencyProfileEngine.ProfileApplicationReport cpuReport;
        public final List<String> gpuLogs;
        public final List<String> ioLogs;
        public final List<String> vmLogs;
        public final List<String> schedLogs;
        public final List<String> netLogs;
        public final List<String> platformLogs;
        public final boolean success;

        public ResolvedConfig(int profileId, String profileName, boolean isPhoenix,
                              UniversalHardwareDetector.DeviceHardwareProfile hardware,
                              CpuFrequencyProfileEngine.ProfileApplicationReport cpuReport,
                              List<String> gpuLogs, List<String> ioLogs, List<String> vmLogs,
                              List<String> schedLogs, List<String> netLogs,
                              List<String> platformLogs, boolean success) {
            this.profileId = profileId;
            this.profileName = profileName;
            this.isPhoenix = isPhoenix;
            this.hardware = hardware;
            this.cpuReport = cpuReport;
            this.gpuLogs = Collections.unmodifiableList(new ArrayList<>(gpuLogs));
            this.ioLogs = Collections.unmodifiableList(new ArrayList<>(ioLogs));
            this.vmLogs = Collections.unmodifiableList(new ArrayList<>(vmLogs));
            this.schedLogs = Collections.unmodifiableList(new ArrayList<>(schedLogs));
            this.netLogs = Collections.unmodifiableList(new ArrayList<>(netLogs));
            this.platformLogs = Collections.unmodifiableList(new ArrayList<>(platformLogs));
            this.success = success;
        }

        public List<String> getAllLogs() {
            List<String> all = new ArrayList<>();
            all.add("Universal Profile: " + profileName + " [" + (isPhoenix ? "Phoenix+" : "Regular") + "] on "
                    + hardware.manufacturer + " " + hardware.model + " (" + hardware.socPlatform + ")");
            if (cpuReport != null) all.addAll(cpuReport.logMessages);
            all.addAll(gpuLogs);
            all.addAll(ioLogs);
            all.addAll(vmLogs);
            all.addAll(schedLogs);
            all.addAll(netLogs);
            all.addAll(platformLogs);
            return all;
        }
    }

    private UniversalProfileResolver() {}

    /**
     * Resolves and applies a device-specific optimization profile.
     */
    @NonNull
    public static ResolvedConfig resolveAndApply(@Nullable Context context, int profileId, boolean isPhoenix) {
        UniversalHardwareDetector.DeviceHardwareProfile hw = UniversalHardwareDetector.detect(context);
        CpuFrequencyProfileEngine.ProfileType pType = CpuFrequencyProfileEngine.ProfileType.fromId(profileId, isPhoenix);
        String name = pType.getDisplayName();

        List<String> ioLogs = new ArrayList<>();
        List<String> vmLogs = new ArrayList<>();
        List<String> schedLogs = new ArrayList<>();
        List<String> netLogs = new ArrayList<>();
        List<String> platformLogs = new ArrayList<>();

        // If KCAL display preset (8-22 in regular mode), apply and return early
        if (!isPhoenix && profileId >= 8 && profileId <= 22) {
            boolean kcalOk = KernelCompatEngine.applyKcalPreset(profileId);
            return new ResolvedConfig(profileId, "KCAL Preset " + profileId, isPhoenix, hw,
                    null, Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                    Collections.singletonList("KCAL applied: " + kcalOk), kcalOk);
        }

        // 1. Dynamic CPU Frequency and Governor Resolution
        CpuFrequencyProfileEngine.ProfileApplicationReport cpuReport =
                CpuFrequencyProfileEngine.applyProfile(pType, isPhoenix);

        boolean isUltra = (profileId == 5);
        if (isUltra) {
            // Lock sysfs permissions to 0444 ONLY on Ultra Gaming to prevent OEM service downclocking
            for (UniversalHardwareDetector.DetectedCluster cluster : hw.clusters) {
                NativeKernelAccess.setPermissions(cluster.path + "/scaling_min_freq", 444);
                NativeKernelAccess.setPermissions(cluster.path + "/scaling_max_freq", 444);
            }
        }

        // 2. Universal GPU Optimization
        List<String> gpuLogs = UniversalGpuOptimizer.applyProfile(profileId, isPhoenix);

        // 3. Dynamic I/O Schedulers across real physical storage devices
        applyIoOptimizations(hw, profileId, isPhoenix, ioLogs);

        // 4. Memory & Virtual Memory scaled to actual device RAM
        applyVmOptimizations(hw, profileId, isPhoenix, vmLogs);

        // 5. Scheduler, Latency, and Task Packing
        applySchedulerOptimizations(hw, profileId, isPhoenix, schedLogs);

        // 6. Network Low-Latency & TCP
        applyNetworkOptimizations(profileId, isPhoenix, netLogs);

        // 7. Touch Sampling Rate & Kernel Panic Suppression
        applyTouchAndKernelOptimizations(profileId, platformLogs);

        // 8. Platform-Specific Vendor Backends (Snapdragon, MediaTek, Exynos, Tensor, Unisoc)
        PlatformBackend backend = KernelCompatEngine.getPlatformBackend();
        boolean isGamingProfile = (profileId == 4 || profileId == 5 || profileId == 6 || profileId == 7 || (isPhoenix && profileId == 3));
        if (isUltra) {
            platformLogs.addAll(backend.applyGamingOptimizations(true));
        } else if (isGamingProfile) {
            platformLogs.addAll(backend.applyGamingOptimizations(false));
        } else if ((!isPhoenix && (profileId == 2 || profileId == 3)) || (isPhoenix && (profileId == 0 || profileId == 2))) {
            platformLogs.addAll(backend.applyBatteryOptimizations());
        } else {
            platformLogs.addAll(backend.applyBalancedOptimizations());
        }

        boolean overallOk = cpuReport.overallSuccess;
        return new ResolvedConfig(profileId, name, isPhoenix, hw, cpuReport, gpuLogs,
                ioLogs, vmLogs, schedLogs, netLogs, platformLogs, overallOk);
    }

    private static void applyIoOptimizations(UniversalHardwareDetector.DeviceHardwareProfile hw,
                                            int profileId, boolean isPhoenix, List<String> logs) {
        String preferredSched;
        int raKb;

        boolean isGamingProfile = (profileId == 4 || profileId == 5 || profileId == 6 || profileId == 7 || (isPhoenix && profileId == 3));
        if (profileId == 5) {
            // Ultra Gaming: lowest latency queue
            preferredSched = "deadline";
            raKb = isPhoenix ? 1024 : 512;
        } else if (isGamingProfile) {
            // Gaming / Smooth / Esports / Apex
            preferredSched = "deadline";
            raKb = isPhoenix ? 512 : 256;
        } else if (profileId == 2 || (!isPhoenix && profileId == 3)) {
            // Battery Saver / CoolDown: power efficiency
            preferredSched = "bfq";
            raKb = 128;
        } else {
            // Balanced / Daily: smooth throughput
            preferredSched = "mq-deadline";
            raKb = 256;
        }

        int count = 0;
        for (KernelCompatEngine.BlockDevice bdev : hw.blockDevices) {
            String chosen = KernelCompatEngine.resolveIoScheduler(preferredSched, bdev.availableSchedulers);
            NativeKernelAccess.writeString(bdev.schedulerPath, chosen);
            NativeKernelAccess.writeString(bdev.queuePath + "/read_ahead_kb", String.valueOf(raKb));
            NativeKernelAccess.writeString(bdev.queuePath + "/iostats", isGamingProfile ? "0" : "1");
            NativeKernelAccess.writeString(bdev.queuePath + "/rq_affinity", "2");
            NativeKernelAccess.writeString(bdev.queuePath + "/add_random", "0");
            NativeKernelAccess.writeString(bdev.queuePath + "/rotational", "0");
            NativeKernelAccess.writeString(bdev.queuePath + "/nomerges", isGamingProfile ? "2" : "0");
            count++;
        }
        logs.add("Optimized " + count + " block storage devices (Scheduler=" + preferredSched + ", ReadAhead=" + raKb + "KB)");
    }

    private static void applyVmOptimizations(UniversalHardwareDetector.DeviceHardwareProfile hw,
                                            int profileId, boolean isPhoenix, List<String> logs) {
        long ramMb = hw.totalRamMb;

        // Dynamic Swappiness and Cache Pressure based on device RAM size
        String swappiness;
        String vfsCachePressure;
        String dirtyRatio;
        String dirtyBgRatio;

        boolean isGamingProfile = (profileId == 4 || profileId == 5 || profileId == 6 || profileId == 7 || (isPhoenix && profileId == 3));
        if ((!isPhoenix && (profileId == 2 || profileId == 3)) || (isPhoenix && (profileId == 0 || profileId == 2))) {
            // Battery / CoolDown
            swappiness = (ramMb > 6000) ? "20" : "40";
            vfsCachePressure = "50";
            dirtyRatio = "15";
            dirtyBgRatio = "5";
        } else if (profileId == 5) {
            // Ultra Gaming
            swappiness = (ramMb > 8000) ? "60" : "80";
            vfsCachePressure = isPhoenix ? "120" : "100";
            dirtyRatio = "10";
            dirtyBgRatio = "4";
        } else if (isGamingProfile) {
            // Gaming / Smooth / Esports / Apex
            swappiness = (ramMb > 8000) ? "55" : "75";
            vfsCachePressure = "100";
            dirtyRatio = "12";
            dirtyBgRatio = "6";
        } else {
            // Balanced / Daily
            swappiness = "50";
            vfsCachePressure = "80";
            dirtyRatio = "20";
            dirtyBgRatio = "10";
        }

        NativeKernelAccess.writeString("/proc/sys/vm/swappiness", swappiness);
        NativeKernelAccess.writeString("/proc/sys/vm/vfs_cache_pressure", vfsCachePressure);
        NativeKernelAccess.writeString("/proc/sys/vm/dirty_ratio", dirtyRatio);
        NativeKernelAccess.writeString("/proc/sys/vm/dirty_background_ratio", dirtyBgRatio);
        NativeKernelAccess.writeString("/proc/sys/vm/dirty_expire_centisecs", isGamingProfile ? "500" : "1500");
        NativeKernelAccess.writeString("/proc/sys/vm/dirty_writeback_centisecs", isGamingProfile ? "250" : "1000");
        NativeKernelAccess.writeString("/proc/sys/vm/laptop_mode", "0");
        NativeKernelAccess.writeString("/proc/sys/vm/oom_dump_tasks", "0");

        if (profileId == 3 || profileId == 5) {
            NativeKernelAccess.writeString("/proc/sys/vm/drop_caches", "3");
            NativeKernelAccess.writeString("/proc/sys/vm/compact_memory", "1");
        }

        if (NativeKernelAccess.exists("/proc/sys/vm/oom_kill_allocating_task")) {
            NativeKernelAccess.writeString("/proc/sys/vm/oom_kill_allocating_task", "0");
        }

        logs.add("VM tuned for " + ramMb + "MB RAM (Swappiness=" + swappiness + ", CachePressure=" + vfsCachePressure + ")");
    }

    private static void applySchedulerOptimizations(UniversalHardwareDetector.DeviceHardwareProfile hw,
                                                   int profileId, boolean isPhoenix, List<String> logs) {
        boolean isGamingProfile = (profileId == 4 || profileId == 5 || profileId == 6 || profileId == 7 || (isPhoenix && profileId == 3));
        NativeKernelAccess.writeString("/proc/sys/kernel/sched_boost", isGamingProfile ? "1" : "0");
        NativeKernelAccess.writeString("/proc/sys/kernel/sched_tunable_scaling", "0");
        NativeKernelAccess.writeString("/proc/sys/kernel/sched_latency_ns", isGamingProfile ? "4000000" : "10000000");
        NativeKernelAccess.writeString("/proc/sys/kernel/sched_min_granularity_ns", isGamingProfile ? "500000" : "2000000");
        NativeKernelAccess.writeString("/proc/sys/kernel/sched_wakeup_granularity_ns", isGamingProfile ? "1000000" : "2500000");
        NativeKernelAccess.writeString("/proc/sys/kernel/sched_child_runs_first", "0");
        NativeKernelAccess.writeString("/proc/sys/kernel/sched_autogroup_enabled", isGamingProfile ? "0" : "1");

        // Android 12+ UCLAMP vs Android 11- SchedTune
        if (hw.hasUclamp) {
            String uclampMin = (profileId == 5) ? (isPhoenix ? "60" : "50") : (isGamingProfile ? "30" : "0");
            NativeKernelAccess.writeString("/dev/cpuctl/top-app/cpu.uclamp.min", uclampMin);
            if (NativeKernelAccess.exists("/dev/cpuctl/top-app/cpu.uclamp.latency_sensitive")) {
                NativeKernelAccess.writeString("/dev/cpuctl/top-app/cpu.uclamp.latency_sensitive", isGamingProfile ? "1" : "0");
            }
            logs.add("Applied Android 12+ UCLAMP boost (Min=" + uclampMin + "%)");
        } else if (hw.hasSchedtune) {
            String stuneBoost = (profileId == 5) ? (isPhoenix ? "50" : "40") : (isGamingProfile ? "20" : "0");
            NativeKernelAccess.writeString("/dev/stune/top-app/schedtune.boost", stuneBoost);
            NativeKernelAccess.writeString("/dev/stune/top-app/schedtune.prefer_idle", isGamingProfile ? "1" : "0");
            logs.add("Applied EAS SchedTune boost (Boost=" + stuneBoost + "%)");
        }

        // Sched features
        if (NativeKernelAccess.exists("/sys/kernel/debug/sched_features")) {
            NativeKernelAccess.writeString("/sys/kernel/debug/sched_features", isGamingProfile ? "NO_GENTLE_FAIR_SLEEPERS" : "GENTLE_FAIR_SLEEPERS");
            NativeKernelAccess.writeString("/sys/kernel/debug/sched_features", "NO_START_DEBIT");
            NativeKernelAccess.writeString("/sys/kernel/debug/sched_features", "NEXT_BUDDY");
        }
    }

    private static void applyNetworkOptimizations(int profileId, boolean isPhoenix, List<String> logs) {
        NativeKernelAccess.writeString("/proc/sys/net/ipv4/tcp_low_latency", (profileId >= 4) ? "1" : "0");
        NativeKernelAccess.writeString("/proc/sys/net/ipv4/tcp_fastopen", "3");
        NativeKernelAccess.writeString("/proc/sys/net/ipv4/tcp_tw_reuse", "1");
        NativeKernelAccess.writeString("/proc/sys/net/ipv4/tcp_slow_start_after_idle", "0");
        if (NativeKernelAccess.exists("/proc/sys/net/ipv4/tcp_notsent_lowat")) {
            NativeKernelAccess.writeString("/proc/sys/net/ipv4/tcp_notsent_lowat", (profileId >= 4) ? "16384" : "4294967295");
        }
        NativeKernelAccess.writeString("/proc/sys/net/core/netdev_max_backlog", (profileId >= 4) ? "10000" : "2500");
        logs.add("TCP/IP tuned for " + (profileId >= 4 ? "low latency gaming" : "energy efficiency"));
    }

    private static void applyTouchAndKernelOptimizations(int profileId, List<String> logs) {
        NativeKernelAccess.writeString("/proc/touchpanel/game_switch_enable", (profileId >= 4) ? "1" : "0");
        NativeKernelAccess.writeString("/sys/class/touch/switch/set_touchscreen", (profileId >= 4) ? "1" : "0");
        NativeKernelAccess.writeString("/sys/devices/virtual/touch/touch_dev/bump_sample_rate", (profileId >= 4) ? "1" : "0");

        NativeKernelAccess.writeString("/proc/sys/kernel/panic", "0");
        NativeKernelAccess.writeString("/proc/sys/kernel/panic_on_oops", "0");
        NativeKernelAccess.writeString("/sys/module/kernel/parameters/initcall_debug", "0");
        logs.add("Touch sampling and kernel panic suppression configured");
    }
}

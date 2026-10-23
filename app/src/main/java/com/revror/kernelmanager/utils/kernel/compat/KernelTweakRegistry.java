package com.revror.kernelmanager.utils.kernel.compat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Universal Registry of Kernel Optimization Tweaks for ROR.
 * Maps every kernel tweak to its purpose, primary paths, verified equivalents,
 * valid value ranges, and verification methods across Snapdragon, MediaTek, Exynos,
 * Tensor, Unisoc, and generic Android/Linux kernels.
 */
public final class KernelTweakRegistry {

    public enum Category {
        CPU,
        GPU,
        IO,
        VM,
        SCHEDULER,
        NETWORK,
        THERMAL,
        DISPLAY,
        WAKELOCK,
        SYSTEM
    }

    public enum VerificationType {
        READ_BACK,
        NODE_EXISTS,
        PROPERTY_CHECK,
        RETURN_CODE,
        NONE
    }

    public static final class TweakDefinition {
        private final String id;
        private final String purpose;
        private final Category category;
        private final List<String> primaryPaths;
        private final List<String> fallbackPaths;
        private final List<String> validOptions;
        private final Long minValue;
        private final Long maxValue;
        private final String defaultValue;
        private final VerificationType verificationType;
        private final boolean requiresRoot;
        private final boolean isReadOnlyInfo;

        private TweakDefinition(Builder builder) {
            this.id = builder.id;
            this.purpose = builder.purpose;
            this.category = builder.category;
            this.primaryPaths = Collections.unmodifiableList(new ArrayList<>(builder.primaryPaths));
            this.fallbackPaths = Collections.unmodifiableList(new ArrayList<>(builder.fallbackPaths));
            this.validOptions = Collections.unmodifiableList(new ArrayList<>(builder.validOptions));
            this.minValue = builder.minValue;
            this.maxValue = builder.maxValue;
            this.defaultValue = builder.defaultValue;
            this.verificationType = builder.verificationType;
            this.requiresRoot = builder.requiresRoot;
            this.isReadOnlyInfo = builder.isReadOnlyInfo;
        }

        public String getId() { return id; }
        public String getPurpose() { return purpose; }
        public Category getCategory() { return category; }
        public List<String> getPrimaryPaths() { return primaryPaths; }
        public List<String> getFallbackPaths() { return fallbackPaths; }
        public List<String> getValidOptions() { return validOptions; }
        public Long getMinValue() { return minValue; }
        public Long getMaxValue() { return maxValue; }
        public String getDefaultValue() { return defaultValue; }
        public VerificationType getVerificationType() { return verificationType; }
        public boolean requiresRoot() { return requiresRoot; }
        public boolean isReadOnlyInfo() { return isReadOnlyInfo; }

        /**
         * Returns all candidate paths in order of preference.
         */
        public List<String> getAllCandidatePaths() {
            List<String> all = new ArrayList<>(primaryPaths.size() + fallbackPaths.size());
            all.addAll(primaryPaths);
            all.addAll(fallbackPaths);
            return Collections.unmodifiableList(all);
        }

        public static class Builder {
            private final String id;
            private final String purpose;
            private final Category category;
            private final List<String> primaryPaths = new ArrayList<>();
            private final List<String> fallbackPaths = new ArrayList<>();
            private final List<String> validOptions = new ArrayList<>();
            private Long minValue = null;
            private Long maxValue = null;
            private String defaultValue = null;
            private VerificationType verificationType = VerificationType.READ_BACK;
            private boolean requiresRoot = true;
            private boolean isReadOnlyInfo = false;

            public Builder(String id, String purpose, Category category) {
                this.id = id;
                this.purpose = purpose;
                this.category = category;
            }

            public Builder addPrimaryPath(String path) {
                if (path != null && !path.trim().isEmpty()) primaryPaths.add(path.trim());
                return this;
            }

            public Builder addFallbackPath(String path) {
                if (path != null && !path.trim().isEmpty()) fallbackPaths.add(path.trim());
                return this;
            }

            public Builder setOptions(String... options) {
                if (options != null) validOptions.addAll(Arrays.asList(options));
                return this;
            }

            public Builder setRange(long min, long max) {
                this.minValue = min;
                this.maxValue = max;
                return this;
            }

            public Builder setDefaultValue(String defaultValue) {
                this.defaultValue = defaultValue;
                return this;
            }

            public Builder setVerificationType(VerificationType type) {
                this.verificationType = type;
                return this;
            }

            public Builder setRequiresRoot(boolean requiresRoot) {
                this.requiresRoot = requiresRoot;
                return this;
            }

            public Builder setReadOnlyInfo(boolean readOnlyInfo) {
                this.isReadOnlyInfo = readOnlyInfo;
                return this;
            }

            public TweakDefinition build() {
                return new TweakDefinition(this);
            }
        }
    }

    private static final Map<String, TweakDefinition> REGISTRY = new HashMap<>();

    static {
        registerAllTweaks();
    }

    private KernelTweakRegistry() {}

    private static void register(TweakDefinition tweak) {
        REGISTRY.put(tweak.getId(), tweak);
    }

    private static void registerAllTweaks() {
        // ==========================================
        // 1. CPU FREQUENCY & GOVERNOR TWEAKS
        // ==========================================

        register(new TweakDefinition.Builder("CPU_SCALING_GOVERNOR",
                "CPU Frequency Scaling Governor", Category.CPU)
                .addPrimaryPath("/sys/devices/system/cpu/cpufreq/policy%d/scaling_governor")
                .addFallbackPath("/sys/devices/system/cpu/cpu%d/cpufreq/scaling_governor")
                .setOptions("schedutil", "performance", "powersave", "interactive", "blu_schedutil",
                        "ondemand", "conservative", "userspace", "pixutil", "energy_dcfc")
                .setDefaultValue("schedutil")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("CPU_SCALING_MIN_FREQ",
                "CPU Policy Minimum Frequency (kHz)", Category.CPU)
                .addPrimaryPath("/sys/devices/system/cpu/cpufreq/policy%d/scaling_min_freq")
                .addFallbackPath("/sys/devices/system/cpu/cpu%d/cpufreq/scaling_min_freq")
                .addFallbackPath("/sys/kernel/cpufreq_hardlimit/scaling_min_freq")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("CPU_SCALING_MAX_FREQ",
                "CPU Policy Maximum Frequency (kHz)", Category.CPU)
                .addPrimaryPath("/sys/devices/system/cpu/cpufreq/policy%d/scaling_max_freq")
                .addFallbackPath("/sys/devices/system/cpu/cpu%d/cpufreq/scaling_max_freq")
                .addFallbackPath("/sys/kernel/cpufreq_hardlimit/scaling_max_freq")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("CPU_INFO_MIN_FREQ",
                "CPU Hardware Minimum Frequency - READ ONLY", Category.CPU)
                .addPrimaryPath("/sys/devices/system/cpu/cpufreq/policy%d/cpuinfo_min_freq")
                .addFallbackPath("/sys/devices/system/cpu/cpu%d/cpufreq/cpuinfo_min_freq")
                .setReadOnlyInfo(true)
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("CPU_INFO_MAX_FREQ",
                "CPU Hardware Maximum Frequency - READ ONLY", Category.CPU)
                .addPrimaryPath("/sys/devices/system/cpu/cpufreq/policy%d/cpuinfo_max_freq")
                .addFallbackPath("/sys/devices/system/cpu/cpu%d/cpufreq/cpuinfo_max_freq")
                .setReadOnlyInfo(true)
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("CPU_CUR_FREQ",
                "CPU Instantaneous Frequency (kHz) - READ ONLY", Category.CPU)
                .addPrimaryPath("/sys/devices/system/cpu/cpufreq/policy%d/scaling_cur_freq")
                .addFallbackPath("/sys/devices/system/cpu/cpu%d/cpufreq/scaling_cur_freq")
                .setReadOnlyInfo(true)
                .setVerificationType(VerificationType.NONE)
                .build());

        register(new TweakDefinition.Builder("CPU_ONLINE",
                "CPU Core Online Control", Category.CPU)
                .addPrimaryPath("/sys/devices/system/cpu/cpu%d/online")
                .setOptions("0", "1")
                .setDefaultValue("1")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        // Qualcomm msm_performance QoS frequency clamping
        register(new TweakDefinition.Builder("MSM_PERF_MIN_FREQ",
                "Qualcomm MSM Performance Min Frequency Clamp", Category.CPU)
                .addPrimaryPath("/sys/module/msm_performance/parameters/cpu_min_freq")
                // Equivalent fallback is cpufreq policy scaling_min_freq directly
                .addFallbackPath("/sys/devices/system/cpu/cpufreq/policy%d/scaling_min_freq")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("MSM_PERF_MAX_FREQ",
                "Qualcomm MSM Performance Max Frequency Clamp", Category.CPU)
                .addPrimaryPath("/sys/module/msm_performance/parameters/cpu_max_freq")
                .addFallbackPath("/sys/devices/system/cpu/cpufreq/policy%d/scaling_max_freq")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("TOUCH_BOOST",
                "CPU Touch Boost Activation", Category.CPU)
                .addPrimaryPath("/sys/module/msm_performance/parameters/touchboost")
                .addFallbackPath("/sys/power/pnpmgr/touch_boost")
                .addFallbackPath("/sys/module/cpu_boost/parameters/input_boost_ms")
                .addFallbackPath("/sys/module/cpu_input_boost/parameters/input_boost_duration")
                .setOptions("0", "1")
                .setDefaultValue("0")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("INPUT_BOOST_MS",
                "CPU Input Boost Duration (ms)", Category.CPU)
                .addPrimaryPath("/sys/module/cpu_boost/parameters/input_boost_ms")
                .addFallbackPath("/sys/module/cpu_input_boost/parameters/input_boost_duration")
                .addFallbackPath("/sys/module/dsboost/parameters/input_boost_duration")
                .setRange(0, 1000)
                .setDefaultValue("40")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        // ==========================================
        // 2. SCHEDULER & SCHEDTUNE / UCLAMP TWEAKS
        // ==========================================

        register(new TweakDefinition.Builder("SCHED_BOOST",
                "Kernel CPU Scheduler Boost Level", Category.SCHEDULER)
                .addPrimaryPath("/proc/sys/kernel/sched_boost")
                .addFallbackPath("/proc/sys/kernel/sched_walt_init_task_load_pct")
                .setRange(0, 3)
                .setDefaultValue("0")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("SCHEDTUNE_TOP_APP_BOOST",
                "Top-App SchedTune / UCLAMP Boost Percentage", Category.SCHEDULER)
                .addPrimaryPath("/dev/stune/top-app/schedtune.boost")
                // Android 12+ equivalent interface:
                .addFallbackPath("/dev/cpuctl/top-app/cpu.uclamp.min")
                .addFallbackPath("/dev/stune/schedtune.boost")
                .addFallbackPath("/proc/sys/kernel/sched_boost")
                .setRange(0, 100)
                .setDefaultValue("0")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("SCHEDTUNE_FOREGROUND_BOOST",
                "Foreground SchedTune / UCLAMP Boost", Category.SCHEDULER)
                .addPrimaryPath("/dev/stune/foreground/schedtune.boost")
                .addFallbackPath("/dev/cpuctl/foreground/cpu.uclamp.min")
                .addFallbackPath("/dev/stune/schedtune.boost")
                .setRange(0, 100)
                .setDefaultValue("0")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("SCHED_PREFER_IDLE",
                "Top-App SchedTune Prefer Idle", Category.SCHEDULER)
                .addPrimaryPath("/dev/stune/top-app/schedtune.prefer_idle")
                .addFallbackPath("/dev/cpuctl/top-app/cpu.uclamp.latency_sensitive")
                .setOptions("0", "1")
                .setDefaultValue("0")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("SCHED_TUNABLE_SCALING",
                "CPU Scheduler Tunable Scaling", Category.SCHEDULER)
                .addPrimaryPath("/proc/sys/kernel/sched_tunable_scaling")
                .setOptions("0", "1")
                .setDefaultValue("0")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("SCHED_MIGRATION_COST",
                "Scheduler Migration Cost (ns)", Category.SCHEDULER)
                .addPrimaryPath("/proc/sys/kernel/sched_migration_cost_ns")
                .setRange(100, 5000000)
                .setDefaultValue("500000")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("SCHED_AUTOGROUP",
                "Scheduler Autogroup Enabled", Category.SCHEDULER)
                .addPrimaryPath("/proc/sys/kernel/sched_autogroup_enabled")
                .setOptions("0", "1")
                .setDefaultValue("1")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("SCHED_RATE_LIMIT_UP",
                "Schedutil Governor Rate Limit Up (us)", Category.SCHEDULER)
                .addPrimaryPath("/sys/devices/system/cpu/cpufreq/policy%d/schedutil/up_rate_limit_us")
                .addFallbackPath("/sys/devices/system/cpu/cpufreq/schedutil/up_rate_limit_us")
                .setRange(0, 100000)
                .setDefaultValue("500")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("SCHED_RATE_LIMIT_DOWN",
                "Schedutil Governor Rate Limit Down (us)", Category.SCHEDULER)
                .addPrimaryPath("/sys/devices/system/cpu/cpufreq/policy%d/schedutil/down_rate_limit_us")
                .addFallbackPath("/sys/devices/system/cpu/cpufreq/schedutil/down_rate_limit_us")
                .setRange(0, 100000)
                .setDefaultValue("20000")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        // ==========================================
        // 3. GPU TWEAKS (ADRENO, MALI, DEVFREQ)
        // ==========================================

        register(new TweakDefinition.Builder("GPU_GOVERNOR",
                "GPU Frequency Governor", Category.GPU)
                .addPrimaryPath("/sys/class/kgsl/kgsl-3d0/devfreq/governor")
                .addFallbackPath("/sys/class/kgsl/kgsl-3d0/pwrscale/trustzone/governor")
                .addFallbackPath("/sys/class/devfreq/1c00000.qcom,kgsl-3d0/governor")
                .addFallbackPath("/sys/class/devfreq/3d00000.qcom,kgsl-3d0/governor")
                .addFallbackPath("/sys/class/devfreq/gpufreq/governor")
                .addFallbackPath("/sys/class/devfreq/mali0/governor")
                .setOptions("msm-adreno-tz", "simple_ondemand", "performance", "powersave", "userspace")
                .setDefaultValue("msm-adreno-tz")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("GPU_MAX_FREQ",
                "GPU Maximum Clock Frequency (Hz)", Category.GPU)
                .addPrimaryPath("/sys/class/kgsl/kgsl-3d0/max_gpuclk")
                .addFallbackPath("/sys/class/kgsl/kgsl-3d0/devfreq/max_freq")
                .addFallbackPath("/sys/devices/platform/kgsl-3d0.0/kgsl/kgsl-3d0/max_gpuclk")
                .addFallbackPath("/sys/class/devfreq/gpufreq/max_freq")
                .addFallbackPath("/sys/class/devfreq/mali0/max_freq")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("GPU_MIN_FREQ",
                "GPU Minimum Clock Frequency (Hz)", Category.GPU)
                .addPrimaryPath("/sys/class/kgsl/kgsl-3d0/devfreq/min_freq")
                .addFallbackPath("/sys/class/kgsl/kgsl-3d0/min_clock_mhz")
                .addFallbackPath("/sys/class/devfreq/gpufreq/min_freq")
                .addFallbackPath("/sys/class/devfreq/mali0/min_freq")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("GPU_FORCE_BUS_ON",
                "GPU Force Bus Always Active", Category.GPU)
                .addPrimaryPath("/sys/class/kgsl/kgsl-3d0/force_bus_on")
                .setOptions("0", "1")
                .setDefaultValue("0")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("GPU_FORCE_CLK_ON",
                "GPU Force Clock Always Active", Category.GPU)
                .addPrimaryPath("/sys/class/kgsl/kgsl-3d0/force_clk_on")
                .setOptions("0", "1")
                .setDefaultValue("0")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("GPU_FORCE_RAIL_ON",
                "GPU Force Power Rail Always Active", Category.GPU)
                .addPrimaryPath("/sys/class/kgsl/kgsl-3d0/force_rail_on")
                .setOptions("0", "1")
                .setDefaultValue("0")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("GPU_THROTTLING",
                "Adreno GPU Hardware Throttling Control", Category.GPU)
                .addPrimaryPath("/sys/class/kgsl/kgsl-3d0/throttling")
                .setOptions("0", "1")
                .setDefaultValue("1")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("ADRENO_BOOST",
                "Adreno Boost Acceleration Level", Category.GPU)
                .addPrimaryPath("/sys/class/kgsl/kgsl-3d0/devfreq/adrenoboost")
                .setOptions("0", "1", "2", "3")
                .setDefaultValue("0")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("ADRENO_IDLER",
                "Adreno Idler Power Saving Algorithm", Category.GPU)
                .addPrimaryPath("/sys/module/adreno_idler/parameters/adreno_idler_active")
                .setOptions("Y", "N", "1", "0")
                .setDefaultValue("N")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        // ==========================================
        // 4. STORAGE & I/O SCHEDULER TWEAKS
        // ==========================================

        register(new TweakDefinition.Builder("IO_SCHEDULER",
                "Block Device I/O Elevator / Scheduler", Category.IO)
                .addPrimaryPath("/sys/block/%s/queue/scheduler")
                .setOptions("mq-deadline", "deadline", "bfq", "cfq", "none", "kyber", "anxiety", "noop")
                .setDefaultValue("mq-deadline")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("IO_READ_AHEAD_KB",
                "Block Device Read-Ahead Buffer (KiB)", Category.IO)
                .addPrimaryPath("/sys/block/%s/queue/read_ahead_kb")
                .setRange(64, 4096)
                .setDefaultValue("512")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("IO_IOSTATS",
                "Block Device I/O Accounting Overhead", Category.IO)
                .addPrimaryPath("/sys/block/%s/queue/iostats")
                .setOptions("0", "1")
                .setDefaultValue("0")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("IO_ADD_RANDOM",
                "Block Queue Random Entropy Contribution", Category.IO)
                .addPrimaryPath("/sys/block/%s/queue/add_random")
                .setOptions("0", "1")
                .setDefaultValue("0")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("IO_RQ_AFFINITY",
                "Block Queue Request Affinity to CPU Core", Category.IO)
                .addPrimaryPath("/sys/block/%s/queue/rq_affinity")
                .setOptions("0", "1", "2")
                .setDefaultValue("2")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("FS_DYNAMIC_FSYNC",
                "Dynamic Fsync Control", Category.IO)
                .addPrimaryPath("/sys/kernel/dyn_fsync/Dyn_fsync_active")
                .addFallbackPath("/sys/module/sync/parameters/fsync_enabled")
                .setOptions("0", "1", "Y", "N")
                .setDefaultValue("0")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        // ==========================================
        // 5. VIRTUAL MEMORY & RAM TUNING
        // ==========================================

        register(new TweakDefinition.Builder("VM_SWAPPINESS",
                "Virtual Memory Swappiness Ratio (%)", Category.VM)
                .addPrimaryPath("/proc/sys/vm/swappiness")
                .setRange(0, 200)
                .setDefaultValue("60")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("VM_VFS_CACHE_PRESSURE",
                "VFS Dentry & Inode Cache Eviction Pressure", Category.VM)
                .addPrimaryPath("/proc/sys/vm/vfs_cache_pressure")
                .setRange(0, 500)
                .setDefaultValue("100")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("VM_DIRTY_RATIO",
                "Dirty Memory Writeback Trigger Ratio (%)", Category.VM)
                .addPrimaryPath("/proc/sys/vm/dirty_ratio")
                .setRange(1, 90)
                .setDefaultValue("20")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("VM_DIRTY_BACKGROUND_RATIO",
                "Background Dirty Memory Flusher Ratio (%)", Category.VM)
                .addPrimaryPath("/proc/sys/vm/dirty_background_ratio")
                .setRange(1, 80)
                .setDefaultValue("10")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("VM_DIRTY_EXPIRE_CENTISECS",
                "Dirty Data Expiration Time (1/100s)", Category.VM)
                .addPrimaryPath("/proc/sys/vm/dirty_expire_centisecs")
                .setRange(50, 6000)
                .setDefaultValue("500")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("VM_DIRTY_WRITEBACK_CENTISECS",
                "Dirty Flusher Wakeup Period (1/100s)", Category.VM)
                .addPrimaryPath("/proc/sys/vm/dirty_writeback_centisecs")
                .setRange(50, 6000)
                .setDefaultValue("1500")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("VM_DROP_CACHES",
                "Drop Pagecache, Dentries, and Inodes", Category.VM)
                .addPrimaryPath("/proc/sys/vm/drop_caches")
                .setOptions("1", "2", "3")
                .setVerificationType(VerificationType.RETURN_CODE)
                .build());

        register(new TweakDefinition.Builder("VM_COMPACT_MEMORY",
                "Compact Fragmented Free Memory", Category.VM)
                .addPrimaryPath("/proc/sys/vm/compact_memory")
                .setOptions("1")
                .setVerificationType(VerificationType.RETURN_CODE)
                .build());

        register(new TweakDefinition.Builder("VM_MIN_FREE_KBYTES",
                "Minimum Reserved Free Memory (KiB)", Category.VM)
                .addPrimaryPath("/proc/sys/vm/min_free_kbytes")
                .setRange(1024, 65536)
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("VM_EXTRA_FREE_KBYTES",
                "Extra Reserved Free Memory (KiB)", Category.VM)
                .addPrimaryPath("/proc/sys/vm/extra_free_kbytes")
                .setRange(0, 131072)
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        // CRITICAL: oom_kill_allocating_task has NO equivalent on modern GKI kernels.
        // It must be checked before write, and if missing, safely recorded as unsupported.
        register(new TweakDefinition.Builder("VM_OOM_KILL_ALLOCATING_TASK",
                "OOM Killer Sacrifices Allocating Task (Legacy Kernel)", Category.VM)
                .addPrimaryPath("/proc/sys/vm/oom_kill_allocating_task")
                .setOptions("0", "1")
                .setDefaultValue("0")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("LMK_MINFREE",
                "Low Memory Killer Free Pages Thresholds", Category.VM)
                .addPrimaryPath("/sys/module/lowmemorykiller/parameters/minfree")
                // Android 10+ userspace equivalent:
                .addFallbackPath("sys.lmk.minfree_levels")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        // ==========================================
        // 6. NETWORKING & TCP OPTIMIZATION
        // ==========================================

        register(new TweakDefinition.Builder("TCP_CONGESTION",
                "TCP Congestion Control Algorithm", Category.NETWORK)
                .addPrimaryPath("/proc/sys/net/ipv4/tcp_congestion_control")
                .setOptions("bbr", "cubic", "westwood", "reno", "bic", "fq_codel")
                .setDefaultValue("cubic")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("TCP_LOW_LATENCY",
                "TCP Low-Latency Immediate Processing", Category.NETWORK)
                .addPrimaryPath("/proc/sys/net/ipv4/tcp_low_latency")
                .setOptions("0", "1")
                .setDefaultValue("1")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("TCP_FASTOPEN",
                "TCP Fast Open Bitmask", Category.NETWORK)
                .addPrimaryPath("/proc/sys/net/ipv4/tcp_fastopen")
                .setOptions("0", "1", "2", "3")
                .setDefaultValue("3")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("TCP_TW_REUSE",
                "TCP TIME-WAIT Socket Recycling", Category.NETWORK)
                .addPrimaryPath("/proc/sys/net/ipv4/tcp_tw_reuse")
                .setOptions("0", "1")
                .setDefaultValue("1")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("TCP_SLOW_START_AFTER_IDLE",
                "TCP Slow Start After Idle Window Reduction", Category.NETWORK)
                .addPrimaryPath("/proc/sys/net/ipv4/tcp_slow_start_after_idle")
                .setOptions("0", "1")
                .setDefaultValue("0")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("NET_CORE_RMEM_MAX",
                "Network Core Receive Buffer Maximum (Bytes)", Category.NETWORK)
                .addPrimaryPath("/proc/sys/net/core/rmem_max")
                .setRange(65536, 16777216)
                .setDefaultValue("8388608")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("NET_CORE_WMEM_MAX",
                "Network Core Transmit Buffer Maximum (Bytes)", Category.NETWORK)
                .addPrimaryPath("/proc/sys/net/core/wmem_max")
                .setRange(65536, 16777216)
                .setDefaultValue("8388608")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        // ==========================================
        // 7. DISPLAY & KCAL CALIBRATION
        // ==========================================

        register(new TweakDefinition.Builder("KCAL_RGB",
                "Qualcomm KCAL RGB Color Calibration", Category.DISPLAY)
                .addPrimaryPath("/sys/devices/platform/kcal_ctrl.0/kcal")
                .addFallbackPath("/sys/module/msm_fb/parameters/kcal")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("KCAL_SAT",
                "Qualcomm KCAL Color Saturation", Category.DISPLAY)
                .addPrimaryPath("/sys/devices/platform/kcal_ctrl.0/kcal_sat")
                .setRange(128, 384)
                .setDefaultValue("255")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("KCAL_VAL",
                "Qualcomm KCAL Color Value", Category.DISPLAY)
                .addPrimaryPath("/sys/devices/platform/kcal_ctrl.0/kcal_val")
                .setRange(128, 384)
                .setDefaultValue("255")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("KCAL_CONT",
                "Qualcomm KCAL Color Contrast", Category.DISPLAY)
                .addPrimaryPath("/sys/devices/platform/kcal_ctrl.0/kcal_cont")
                .setRange(128, 384)
                .setDefaultValue("255")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("KCAL_MIN",
                "Qualcomm KCAL Display Minimum Brightness", Category.DISPLAY)
                .addPrimaryPath("/sys/devices/platform/kcal_ctrl.0/kcal_min")
                .setRange(0, 100)
                .setDefaultValue("35")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        // ==========================================
        // 8. WAKELOCKS & BATTERY SAVER
        // ==========================================

        register(new TweakDefinition.Builder("BOEFFLA_WAKELOCK_BLOCKER",
                "Boeffla Custom Kernel Wakelock Blocker", Category.WAKELOCK)
                .addPrimaryPath("/sys/class/misc/boeffla_wakelock_blocker/wakelock_blocker")
                .addFallbackPath("/sys/devices/virtual/misc/boeffla_wakelock_blocker/wakelock_blocker")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("WORKQUEUE_POWER_EFFICIENT",
                "Kernel Workqueue Power Efficient Mode", Category.SYSTEM)
                .addPrimaryPath("/sys/module/workqueue/parameters/power_efficient")
                .setOptions("Y", "N", "1", "0")
                .setDefaultValue("Y")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        // ==========================================
        // 9. THERMAL MITIGATION
        // ==========================================

        register(new TweakDefinition.Builder("MSM_THERMAL_CORE_CONTROL",
                "Qualcomm MSM Thermal Core Control", Category.THERMAL)
                .addPrimaryPath("/sys/module/msm_thermal/core_control/enabled")
                .addFallbackPath("/sys/module/msm_thermal/parameters/enabled")
                .setOptions("0", "1")
                .setDefaultValue("1")
                .setVerificationType(VerificationType.READ_BACK)
                .build());

        register(new TweakDefinition.Builder("MSM_THERMAL_VDD",
                "Qualcomm MSM Thermal VDD Restriction", Category.THERMAL)
                .addPrimaryPath("/sys/module/msm_thermal/vdd_restriction/enabled")
                .setOptions("0", "1")
                .setDefaultValue("1")
                .setVerificationType(VerificationType.READ_BACK)
                .build());
    }

    @Nullable
    public static TweakDefinition get(@NonNull String id) {
        return REGISTRY.get(id);
    }

    @NonNull
    public static Map<String, TweakDefinition> getAll() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    /**
     * Resolves the best available equivalent path for a given tweak ID on the device.
     */
    @Nullable
    public static String resolveCompatiblePath(@NonNull String tweakId, @NonNull NodeExistenceChecker checker) {
        TweakDefinition def = get(tweakId);
        if (def == null) return null;

        for (String path : def.getPrimaryPaths()) {
            if (checker.exists(path)) return path;
        }
        for (String fallback : def.getFallbackPaths()) {
            if (checker.exists(fallback)) return fallback;
        }
        return null;
    }

    public interface NodeExistenceChecker {
        boolean exists(String path);
    }
}

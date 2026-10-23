package com.revror.kernelmanager.utils.kernel.compat;

import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.revror.kernelmanager.utils.root.RootUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Universal Dynamic Kernel Compatibility Engine for REVROR.
 * Centralized detection, validation, equivalent tweak mapping, and execution engine
 * used by ALL built-in profiles, custom profiles, game optimization, and background services.
 */
public final class KernelCompatEngine {

    private static final String TAG = "KernelCompatEngine";

    // Data Models
    public static final class CpuPolicy {
        public final int id;
        public final String path;
        public final List<Integer> relatedCpus;
        public final long minFreq;
        public final long maxFreq;
        public final List<Long> availableFreqs;
        public final List<String> availableGovernors;
        public final String currentGovernor;
        public final boolean isWritable;

        public CpuPolicy(int id, String path, List<Integer> relatedCpus, long minFreq, long maxFreq,
                         List<Long> availableFreqs, List<String> availableGovernors,
                         String currentGovernor, boolean isWritable) {
            this.id = id;
            this.path = path;
            this.relatedCpus = Collections.unmodifiableList(relatedCpus);
            this.minFreq = minFreq;
            this.maxFreq = maxFreq;
            this.availableFreqs = Collections.unmodifiableList(availableFreqs);
            this.availableGovernors = Collections.unmodifiableList(availableGovernors);
            this.currentGovernor = currentGovernor;
            this.isWritable = isWritable;
        }

        @Override
        public String toString() {
            return "Policy" + id + "[cores=" + relatedCpus + ", min=" + minFreq + ", max=" + maxFreq
                    + ", gov=" + currentGovernor + "]";
        }
    }

    public static final class GpuInfo {
        public enum DriverType { ADRENO, MALI, POWERVR, GENERIC_DEVFREQ, UNKNOWN }
        public final DriverType driverType;
        public final String basePath;
        public final String governorPath;
        public final String minFreqPath;
        public final String maxFreqPath;
        public final String curFreqPath;
        public final List<Long> availableFreqs;
        public final List<String> availableGovernors;
        public final String currentGovernor;

        public GpuInfo(DriverType driverType, String basePath, String governorPath,
                       String minFreqPath, String maxFreqPath, String curFreqPath,
                       List<Long> availableFreqs, List<String> availableGovernors,
                       String currentGovernor) {
            this.driverType = driverType;
            this.basePath = basePath;
            this.governorPath = governorPath;
            this.minFreqPath = minFreqPath;
            this.maxFreqPath = maxFreqPath;
            this.curFreqPath = curFreqPath;
            this.availableFreqs = Collections.unmodifiableList(availableFreqs);
            this.availableGovernors = Collections.unmodifiableList(availableGovernors);
            this.currentGovernor = currentGovernor;
        }
    }

    public static final class BlockDevice {
        public final String name;
        public final String queuePath;
        public final String schedulerPath;
        public final List<String> availableSchedulers;
        public final String currentScheduler;
        public final int readAheadKb;

        public BlockDevice(String name, String queuePath, String schedulerPath,
                           List<String> availableSchedulers, String currentScheduler, int readAheadKb) {
            this.name = name;
            this.queuePath = queuePath;
            this.schedulerPath = schedulerPath;
            this.availableSchedulers = Collections.unmodifiableList(availableSchedulers);
            this.currentScheduler = currentScheduler;
            this.readAheadKb = readAheadKb;
        }
    }

    public static final class PlatformInfo {
        public final PlatformBackend.PlatformVendor vendor;
        public final String platformName;
        public final String hardware;
        public final String kernelVersion;
        public final int coreCount;
        public final boolean isGki;
        public final boolean hasSchedtune;
        public final boolean hasUclamp;

        public PlatformInfo(PlatformBackend.PlatformVendor vendor, String platformName,
                            String hardware, String kernelVersion, int coreCount,
                            boolean isGki, boolean hasSchedtune, boolean hasUclamp) {
            this.vendor = vendor;
            this.platformName = platformName;
            this.hardware = hardware;
            this.kernelVersion = kernelVersion;
            this.coreCount = coreCount;
            this.isGki = isGki;
            this.hasSchedtune = hasSchedtune;
            this.hasUclamp = hasUclamp;
        }
    }

    public static final class WriteResult {
        public final boolean success;
        public final String pathUsed;
        public final String valueWritten;
        public final String detail;

        public WriteResult(boolean success, String pathUsed, String valueWritten, String detail) {
            this.success = success;
            this.pathUsed = pathUsed;
            this.valueWritten = valueWritten;
            this.detail = detail;
        }
    }

    // Cached discovery state
    private static PlatformInfo sPlatformInfo;
    private static PlatformBackend sPlatformBackend;
    private static List<CpuPolicy> sCpuPolicies;
    private static GpuInfo sGpuInfo;
    private static List<BlockDevice> sBlockDevices;

    private KernelCompatEngine() {}

    /**
     * Refreshes all hardware detections.
     */
    public static synchronized void refresh() {
        sPlatformInfo = null;
        sPlatformBackend = null;
        sCpuPolicies = null;
        sGpuInfo = null;
        sBlockDevices = null;
        getPlatformInfo();
        getCpuPolicies();
        getGpuInfo();
        getBlockDevices();
    }

    // ==========================================
    // 1. HARDWARE DETECTION & PLATFORM BACKEND
    // ==========================================

    @NonNull
    public static synchronized PlatformInfo getPlatformInfo() {
        if (sPlatformInfo == null) {
            String hardware = RootUtils.getProp("ro.hardware");
            if (hardware.isEmpty()) hardware = RootUtils.getProp("ro.board.platform");
            String kernelVer = RootUtils.runAndGetOutput("uname -r 2>/dev/null");
            if (kernelVer.isEmpty()) kernelVer = System.getProperty("os.version", "unknown");

            int cores = discoverCoreCount();
            boolean isGki = kernelVer.contains("-android") || kernelVer.startsWith("5.") || kernelVer.startsWith("6.");
            boolean hasSchedtune = exists("/dev/stune");
            boolean hasUclamp = exists("/dev/cpuctl/top-app/cpu.uclamp.min");

            PlatformBackend backend = getPlatformBackend();
            sPlatformInfo = new PlatformInfo(backend.getVendor(), backend.getPlatformName(),
                    hardware, kernelVer, cores, isGki, hasSchedtune, hasUclamp);
        }
        return sPlatformInfo;
    }

    @NonNull
    public static synchronized PlatformBackend getPlatformBackend() {
        if (sPlatformBackend == null) {
            PlatformBackend[] backends = new PlatformBackend[]{
                    new QualcommBackend(),
                    new MediaTekBackend(),
                    new ExynosBackend(),
                    new TensorBackend(),
                    new UnisocBackend()
            };
            for (PlatformBackend b : backends) {
                if (b.isSupported()) {
                    sPlatformBackend = b;
                    return sPlatformBackend;
                }
            }
            sPlatformBackend = new GenericBackend();
        }
        return sPlatformBackend;
    }

    public static int discoverCoreCount() {
        // Try reading /sys/devices/system/cpu/present
        String present = safeRead("/sys/devices/system/cpu/present");
        if (present != null && present.contains("-")) {
            try {
                String[] parts = present.trim().split("-");
                if (parts.length == 2) {
                    return Integer.parseInt(parts[1]) - Integer.parseInt(parts[0]) + 1;
                }
            } catch (Exception ignored) {}
        }
        // Fallback: count cpu[0-9]* folders natively
        List<String> entries = NativeKernelAccess.listDirectory("/sys/devices/system/cpu");
        int count = 0;
        for (String entry : entries) {
            String name = new File(entry).getName();
            if (name.matches("cpu[0-9]+")) count++;
        }
        if (count > 0) return count;

        return Runtime.getRuntime().availableProcessors();
    }

    // ==========================================
    // 2. DYNAMIC CPU POLICY & FREQUENCY DETECTION
    // ==========================================

    @NonNull
    public static synchronized List<CpuPolicy> getCpuPolicies() {
        if (sCpuPolicies == null) {
            sCpuPolicies = new ArrayList<>();
            // Scan /sys/devices/system/cpu/cpufreq/policy0 ... policy15 natively
            for (int i = 0; i < 16; i++) {
                String p = "/sys/devices/system/cpu/cpufreq/policy" + i;
                if (exists(p)) {
                    sCpuPolicies.add(parseCpuPolicy(i, p));
                }
            }

            // Fallback: if no policy folders, scan /sys/devices/system/cpu/cpu*/cpufreq
            if (sCpuPolicies.isEmpty()) {
                int cores = discoverCoreCount();
                Set<String> seenFreqPaths = new HashSet<>();
                for (int c = 0; c < cores; c++) {
                    String path = "/sys/devices/system/cpu/cpu" + c + "/cpufreq";
                    if (exists(path)) {
                        sCpuPolicies.add(parseCpuPolicy(c, path));
                    }
                }
            }
        }
        return Collections.unmodifiableList(sCpuPolicies);
    }

    private static CpuPolicy parseCpuPolicy(int id, String path) {
        CpuPolicy policy = CpuFrequencyProfileEngine.readSinglePolicy(id, path, null);
        if (policy != null) return policy;
        return new CpuPolicy(id, path, Collections.singletonList(id), -1L, -1L,
                Collections.emptyList(), Collections.emptyList(), "unknown", false);
    }

    // ==========================================
    // 3. DYNAMIC GPU DETECTION
    // ==========================================

    @NonNull
    public static synchronized GpuInfo getGpuInfo() {
        if (sGpuInfo == null) {
            // Check Adreno kgsl
            if (exists("/sys/class/kgsl/kgsl-3d0")) {
                String base = "/sys/class/kgsl/kgsl-3d0";
                String govPath = exists(base + "/devfreq/governor") ? (base + "/devfreq/governor")
                        : (base + "/pwrscale/trustzone/governor");
                String maxPath = exists(base + "/max_gpuclk") ? (base + "/max_gpuclk")
                        : (base + "/devfreq/max_freq");
                String minPath = exists(base + "/devfreq/min_freq") ? (base + "/devfreq/min_freq")
                        : (base + "/min_clock_mhz");
                String curPath = exists(base + "/gpuclk") ? (base + "/gpuclk")
                        : (base + "/devfreq/cur_freq");

                List<Long> freqs = new ArrayList<>();
                String freqsStr = safeRead(base + "/gpu_available_frequencies");
                if (freqsStr == null) freqsStr = safeRead(base + "/devfreq/available_frequencies");
                if (freqsStr != null) {
                    for (String tok : freqsStr.trim().split("\\s+")) {
                        long f = parseLong(tok, -1L);
                        if (f > 0) freqs.add(f);
                    }
                }
                Collections.sort(freqs);

                List<String> govs = new ArrayList<>();
                String govsStr = safeRead(base + "/devfreq/available_governors");
                if (govsStr != null) {
                    for (String g : govsStr.trim().split("\\s+")) {
                        if (!g.isEmpty()) govs.add(g);
                    }
                }

                String curGov = safeRead(govPath);
                sGpuInfo = new GpuInfo(GpuInfo.DriverType.ADRENO, base, govPath, minPath, maxPath,
                        curPath, freqs, govs, curGov != null ? curGov : "msm-adreno-tz");
                return sGpuInfo;
            }

            // Check Mali or devfreq GPU
            for (String df : findMatchingNodes("/sys/class/devfreq", "governor")) {
                String dfDir = df.substring(0, df.lastIndexOf('/'));
                String name = safeRead(dfDir + "/name");
                if (name == null) name = dfDir;
                String lower = (dfDir + " " + name).toLowerCase();
                if (lower.contains("mali") || lower.contains("gpu") || lower.contains("3d")) {
                    GpuInfo.DriverType type = lower.contains("mali") ? GpuInfo.DriverType.MALI
                            : GpuInfo.DriverType.GENERIC_DEVFREQ;

                    List<Long> freqs = new ArrayList<>();
                    String freqsStr = safeRead(dfDir + "/available_frequencies");
                    if (freqsStr != null) {
                        for (String tok : freqsStr.trim().split("\\s+")) {
                            long f = parseLong(tok, -1L);
                            if (f > 0) freqs.add(f);
                        }
                    }
                    Collections.sort(freqs);

                    List<String> govs = new ArrayList<>();
                    String govsStr = safeRead(dfDir + "/available_governors");
                    if (govsStr != null) {
                        for (String g : govsStr.trim().split("\\s+")) {
                            if (!g.isEmpty()) govs.add(g);
                        }
                    }

                    sGpuInfo = new GpuInfo(type, dfDir, df, dfDir + "/min_freq", dfDir + "/max_freq",
                            dfDir + "/cur_freq", freqs, govs, safeRead(df));
                    return sGpuInfo;
                }
            }

            // Unknown GPU fallback
            sGpuInfo = new GpuInfo(GpuInfo.DriverType.UNKNOWN, "", "", "", "", "",
                    Collections.emptyList(), Collections.emptyList(), "");
        }
        return sGpuInfo;
    }

    // ==========================================
    // 4. DYNAMIC I/O BLOCK DEVICES DETECTION
    // ==========================================

    @NonNull
    public static synchronized List<BlockDevice> getBlockDevices() {
        if (sBlockDevices == null) {
            sBlockDevices = new ArrayList<>();
            List<String> blockDirs = NativeKernelAccess.listDirectory("/sys/block");
            for (String b : blockDirs) {
                String devName = new File(b).getName();
                // Skip loop, ram, and dm virtual devices
                if (devName.startsWith("loop") || devName.startsWith("ram") || devName.startsWith("zram")) continue;
                String q = b + "/queue";
                if (!exists(q)) continue;

                String schedPath = q + "/scheduler";
                String schedStr = safeRead(schedPath);
                List<String> available = new ArrayList<>();
                String current = "";
                if (schedStr != null) {
                    for (String tok : schedStr.trim().split("\\s+")) {
                        if (tok.startsWith("[") && tok.endsWith("]")) {
                            String clean = tok.substring(1, tok.length() - 1);
                            current = clean;
                            available.add(clean);
                        } else if (!tok.isEmpty()) {
                            available.add(tok);
                        }
                    }
                }

                int ra = (int) parseLong(safeRead(q + "/read_ahead_kb"), 512L);
                sBlockDevices.add(new BlockDevice(devName, q, schedPath, available, current, ra));
            }
        }
        return Collections.unmodifiableList(sBlockDevices);
    }

    // ==========================================
    // 5. SAFE WRITE & READ WITH VERIFICATION
    // ==========================================

    /**
     * Safely writes a value to a kernel node using direct stream I/O.
     */
    @NonNull
    public static WriteResult safeWrite(@NonNull String path, @NonNull String value) {
        if (!exists(path)) {
            return new WriteResult(false, path, value, "Node does not exist on this kernel");
        }

        boolean ok = NativeKernelAccess.writeString(path, value);

        // Verify read-back if feasible
        String readBack = safeRead(path);
        boolean matches = readBack != null && (readBack.contains(value) || value.contains(readBack));

        return new WriteResult(ok || matches, path, value,
                matches ? "Successfully written and verified" : (ok ? "Written natively" : "Failed write to " + path));
    }

    /**
     * Tries writing to candidate paths in order until one succeeds.
     */
    @NonNull
    public static WriteResult safeWriteWithFallbacks(@NonNull List<String> paths, @NonNull String value) {
        for (String p : paths) {
            if (exists(p)) {
                WriteResult res = safeWrite(p, value);
                if (res.success) return res;
            }
        }
        return new WriteResult(false, "", value, "No compatible implementation found among " + paths.size() + " candidates");
    }

    @Nullable
    public static String safeRead(@NonNull String path) {
        return NativeKernelAccess.readString(path);
    }

    public static boolean exists(@Nullable String path) {
        return NativeKernelAccess.exists(path);
    }

    public static boolean isWritable(@Nullable String path) {
        return NativeKernelAccess.isWritable(path);
    }

    @NonNull
    public static List<String> findMatchingNodes(@NonNull String baseDir, @NonNull String leafName) {
        List<String> list = new ArrayList<>();
        if (!exists(baseDir)) return list;
        List<String> subDirs = NativeKernelAccess.listDirectory(baseDir);
        for (String sub : subDirs) {
            String candidate = sub + "/" + leafName;
            if (exists(candidate)) {
                list.add(candidate);
            }
        }
        return list;
    }

    // ==========================================
    // 6. RESOLUTION HELPERS FOR GOVERNORS & FREQS
    // ==========================================

    /**
     * Resolves the best supported governor for a CPU policy.
     * Fallback order: preferred -> schedutil -> interactive -> ondemand -> conservative -> powersave.
     * Strict rule: Non-Ultra profiles must never resolve to "performance" governor.
     */
    @NonNull
    public static String resolveGovernor(@NonNull String preferred, @NonNull List<String> available) {
        return resolveGovernor(preferred, available, "performance".equalsIgnoreCase(preferred));
    }

    @NonNull
    public static String resolveGovernor(@NonNull String preferred, @NonNull List<String> available, boolean allowPerformance) {
        if (!allowPerformance && "performance".equalsIgnoreCase(preferred)) {
            preferred = "schedutil";
        }
        if (available.contains(preferred)) return preferred;

        String[] fallbacks;
        if ("performance".equalsIgnoreCase(preferred) && allowPerformance) {
            fallbacks = new String[]{"performance", "schedutil", "blu_schedutil", "interactive", "ondemand"};
        } else if ("powersave".equalsIgnoreCase(preferred)) {
            fallbacks = new String[]{"powersave", "conservative", "schedutil", "interactive", "ondemand"};
        } else {
            fallbacks = new String[]{"schedutil", "blu_schedutil", "interactive", "ondemand", "conservative", "powersave"};
        }

        for (String fb : fallbacks) {
            for (String av : available) {
                if (av.equalsIgnoreCase(fb)) return av;
            }
        }
        for (String av : available) {
            if (allowPerformance || !"performance".equalsIgnoreCase(av)) {
                return av;
            }
        }
        return available.isEmpty() ? "schedutil" : available.get(0);
    }

    /**
     * Resolves the best supported I/O scheduler.
     * Fallback order: preferred -> mq-deadline -> bfq -> deadline -> cfq -> none.
     */
    @NonNull
    public static String resolveIoScheduler(@NonNull String preferred, @NonNull List<String> available) {
        if (available.contains(preferred)) return preferred;

        String[] fallbacks = new String[]{"mq-deadline", "bfq", "deadline", "cfq", "kyber", "none", "noop"};
        for (String fb : fallbacks) {
            for (String av : available) {
                if (av.equalsIgnoreCase(fb)) return av;
            }
        }
        return available.isEmpty() ? "none" : available.get(0);
    }

    public static long resolveFrequency(long target, @NonNull List<Long> available, boolean roundUp) {
        if (available.isEmpty()) return target;
        if (available.contains(target)) return target;

        long closest = available.get(0);
        for (long f : available) {
            if (roundUp && f >= target) return f;
            if (!roundUp && f <= target) closest = f;
            if (Math.abs(f - target) < Math.abs(closest - target)) closest = f;
        }
        return closest;
    }

    // ==========================================
    // 7. UNIVERSAL PROFILE APPLICATION ENGINE
    // ==========================================

    /**
     * Universal profile application used by ALL profiles (Daily Use, Balance, Battery,
     * CoolDown, Gaming, Ultra Gaming, Phoenix profiles, Custom profiles).
     */
    @NonNull
    public static List<String> applyProfileUniversal(int profileId, boolean isPhoenix) {
        return UniversalProfileResolver.resolveAndApply(null, profileId, isPhoenix).getAllLogs();
    }

    // ==========================================
    // 8. KCAL DISPLAY PRESETS & GAME PROCESS BOOST
    // ==========================================

    /**
     * Applies KCAL display color presets natively (GSMWS presets 7-22).
     */
    public static boolean applyKcalPreset(int presetId) {
        String kcalBase = "/sys/devices/platform/kcal_ctrl.0";
        if (!exists(kcalBase)) {
            if (exists("/sys/module/msm_fb/parameters/kcal")) {
                kcalBase = "/sys/module/msm_fb/parameters";
            } else {
                return false;
            }
        }

        // Enable KCAL
        safeWrite(kcalBase + "/kcal_enable", "1");
        safeWrite(kcalBase + "/kcal_ctrl", "1");

        switch (presetId) {
            case 7: // Deep Black
                safeWrite(kcalBase + "/kcal", "245 245 250");
                safeWrite(kcalBase + "/kcal_sat", "265");
                safeWrite(kcalBase + "/kcal_val", "250");
                safeWrite(kcalBase + "/kcal_cont", "260");
                safeWrite(kcalBase + "/kcal_min", "25");
                break;
            case 8: // AMOLED
                safeWrite(kcalBase + "/kcal", "255 255 255");
                safeWrite(kcalBase + "/kcal_sat", "280");
                safeWrite(kcalBase + "/kcal_val", "256");
                safeWrite(kcalBase + "/kcal_cont", "265");
                break;
            case 9: // Cool Colors
                safeWrite(kcalBase + "/kcal", "240 250 255");
                safeWrite(kcalBase + "/kcal_sat", "255");
                break;
            case 10: // Warm Colors
                safeWrite(kcalBase + "/kcal", "255 245 235");
                safeWrite(kcalBase + "/kcal_sat", "255");
                break;
            case 11: // High Saturation
                safeWrite(kcalBase + "/kcal", "255 255 255");
                safeWrite(kcalBase + "/kcal_sat", "295");
                break;
            case 12: // Movie Mode
                safeWrite(kcalBase + "/kcal", "255 250 245");
                safeWrite(kcalBase + "/kcal_sat", "270");
                safeWrite(kcalBase + "/kcal_cont", "260");
                break;
            case 13: // Gaming Visuals
                safeWrite(kcalBase + "/kcal", "255 255 255");
                safeWrite(kcalBase + "/kcal_sat", "285");
                safeWrite(kcalBase + "/kcal_val", "260");
                safeWrite(kcalBase + "/kcal_cont", "270");
                break;
            case 14: // Reading / Eye Protection
                safeWrite(kcalBase + "/kcal", "255 240 215");
                safeWrite(kcalBase + "/kcal_sat", "240");
                break;
            case 15: // Vibrant Display
                safeWrite(kcalBase + "/kcal", "255 255 255");
                safeWrite(kcalBase + "/kcal_sat", "290");
                safeWrite(kcalBase + "/kcal_val", "258");
                break;
            case 16: // True Color
                safeWrite(kcalBase + "/kcal", "255 255 255");
                safeWrite(kcalBase + "/kcal_sat", "256");
                safeWrite(kcalBase + "/kcal_val", "256");
                safeWrite(kcalBase + "/kcal_cont", "256");
                break;
            case 17: // High Contrast
                safeWrite(kcalBase + "/kcal", "255 255 255");
                safeWrite(kcalBase + "/kcal_cont", "280");
                break;
            case 18: // Outdoor Sunlight Boost
                safeWrite(kcalBase + "/kcal", "255 255 255");
                safeWrite(kcalBase + "/kcal_sat", "275");
                safeWrite(kcalBase + "/kcal_val", "275");
                safeWrite(kcalBase + "/kcal_cont", "270");
                break;
            case 19: // Night Dimmer
                safeWrite(kcalBase + "/kcal", "250 235 210");
                safeWrite(kcalBase + "/kcal_val", "230");
                safeWrite(kcalBase + "/kcal_min", "10");
                break;
            case 20: // Dynamic Color
                safeWrite(kcalBase + "/kcal", "255 252 250");
                safeWrite(kcalBase + "/kcal_sat", "275");
                break;
            case 21: // Natural Sharp
                safeWrite(kcalBase + "/kcal", "255 255 255");
                safeWrite(kcalBase + "/kcal_cont", "265");
                break;
            case 22: // Display Reset (Stock)
            default:
                safeWrite(kcalBase + "/kcal", "255 255 255");
                safeWrite(kcalBase + "/kcal_sat", "255");
                safeWrite(kcalBase + "/kcal_val", "255");
                safeWrite(kcalBase + "/kcal_cont", "255");
                safeWrite(kcalBase + "/kcal_min", "35");
                break;
        }
        return true;
    }

    /**
     * Prioritizes game rendering and engine threads (UnityMain, UnityGfxDeviceW, Apollo, RenderThread)
     * by pinning them to Big/Prime cores and elevating scheduling priority.
     */
    @NonNull
    public static List<String> optimizeGameThreads(@NonNull String targetPackage) {
        List<String> report = new ArrayList<>();
        if (targetPackage.trim().isEmpty()) return report;

        String pidStr = RootUtils.runAndGetOutput("pidof " + quote(targetPackage) + " 2>/dev/null");
        if (pidStr.isEmpty()) {
            pidStr = RootUtils.runAndGetOutput("pgrep -f " + quote(targetPackage) + " 2>/dev/null");
        }
        if (pidStr.isEmpty()) {
            report.add("Game process " + targetPackage + " not currently running");
            return report;
        }

        String[] pids = pidStr.trim().split("\\s+");
        String mainPid = pids[0];

        List<CpuPolicy> policies = getCpuPolicies();
        int bigCoreMask = 0;
        long maxFreqOverall = 0;
        for (CpuPolicy p : policies) {
            if (p.maxFreq > maxFreqOverall) maxFreqOverall = p.maxFreq;
        }
        for (CpuPolicy p : policies) {
            if (p.maxFreq >= maxFreqOverall * 0.70) {
                for (int cpu : p.relatedCpus) {
                    bigCoreMask |= (1 << cpu);
                }
            }
        }
        if (bigCoreMask == 0) {
            int total = discoverCoreCount();
            for (int i = Math.max(1, total / 2); i < total; i++) {
                bigCoreMask |= (1 << i);
            }
        }
        String hexMask = Integer.toHexString(bigCoreMask);

        List<String> threadDirs = NativeKernelAccess.listDirectory("/proc/" + mainPid + "/task");
        int pinned = 0;
        for (String tDir : threadDirs) {
            String tid = new File(tDir).getName();
            String comm = safeRead(tDir + "/comm");
            if (comm == null) continue;
            comm = comm.trim();

            boolean isCritical = comm.equalsIgnoreCase("UnityMain")
                    || comm.equalsIgnoreCase("UnityGfxDeviceW")
                    || comm.contains("Apollo")
                    || comm.equalsIgnoreCase("MainThread")
                    || comm.equalsIgnoreCase("RenderThread")
                    || comm.equalsIgnoreCase("GLThread")
                    || comm.startsWith("Job.Worker")
                    || comm.equals(targetPackage);

            if (isCritical) {
                RootUtils.runCommand("taskset -p " + hexMask + " " + tid + " 2>/dev/null");
                RootUtils.runCommand("renice -n -20 -p " + tid + " 2>/dev/null");
                pinned++;
            }
        }

        report.add("Optimized " + pinned + " critical threads for " + targetPackage + " (mask: 0x" + hexMask + ")");
        return report;
    }

    private static String quote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private static long parseLong(String val, long fallback) {
        if (val == null) return fallback;
        try { return Long.parseLong(val.trim()); } catch (Exception ignored) { return fallback; }
    }
}

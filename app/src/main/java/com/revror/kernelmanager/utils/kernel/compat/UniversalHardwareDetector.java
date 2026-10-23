package com.revror.kernelmanager.utils.kernel.compat;

import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.revror.kernelmanager.utils.kernel.compat.NativeKernelAccess;
import com.revror.kernelmanager.utils.root.RootUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Universal Hardware, Chipset, CPU Cluster, and Kernel Detection System.
 * Inspects the actual physical silicon and kernel interfaces dynamically at runtime.
 * Supports Qualcomm Snapdragon, MediaTek, Samsung Exynos, Google Tensor, Unisoc,
 * HiSilicon Kirin, and generic Android architectures without assuming fixed paths.
 */
public final class UniversalHardwareDetector {

    public enum ClusterRole {
        LITTLE,
        MID,
        BIG,
        PRIME,
        UNIFIED
    }

    public enum AccessLevel {
        AUTHORIZED_ROOT,
        SHIZUKU_PRIVILEGED,
        NON_ROOT_RESTRICTED
    }

    public static final class DetectedCluster {
        public final int policyId;
        public final String path;
        public final ClusterRole role;
        public final List<Integer> coreIds;
        public final long minFrequencyKhz;
        public final long maxFrequencyKhz;
        public final List<Long> availableFrequencies;
        public final List<String> availableGovernors;
        public final String currentGovernor;
        public final boolean isWritable;

        public DetectedCluster(int policyId, String path, ClusterRole role, List<Integer> coreIds,
                               long minFrequencyKhz, long maxFrequencyKhz,
                               List<Long> availableFrequencies, List<String> availableGovernors,
                               String currentGovernor, boolean isWritable) {
            this.policyId = policyId;
            this.path = path;
            this.role = role;
            this.coreIds = Collections.unmodifiableList(new ArrayList<>(coreIds));
            this.minFrequencyKhz = minFrequencyKhz;
            this.maxFrequencyKhz = maxFrequencyKhz;
            this.availableFrequencies = Collections.unmodifiableList(new ArrayList<>(availableFrequencies));
            this.availableGovernors = Collections.unmodifiableList(new ArrayList<>(availableGovernors));
            this.currentGovernor = currentGovernor != null ? currentGovernor : "schedutil";
            this.isWritable = isWritable;
        }

        public long getPercentileFrequency(float percentile, boolean roundUp) {
            if (availableFrequencies.isEmpty()) {
                long span = maxFrequencyKhz - minFrequencyKhz;
                return minFrequencyKhz + (long) (span * Math.max(0f, Math.min(1f, percentile)));
            }
            if (percentile <= 0f) return availableFrequencies.get(0);
            if (percentile >= 1f) return availableFrequencies.get(availableFrequencies.size() - 1);

            int targetIndex = (int) Math.round(percentile * (availableFrequencies.size() - 1));
            targetIndex = Math.max(0, Math.min(availableFrequencies.size() - 1, targetIndex));
            return availableFrequencies.get(targetIndex);
        }

        @Override
        public String toString() {
            return "Cluster[" + role + " (Policy " + policyId + "), Cores=" + coreIds
                    + ", " + (minFrequencyKhz / 1000) + "MHz-" + (maxFrequencyKhz / 1000) + "MHz"
                    + ", Gov=" + currentGovernor + "]";
        }
    }

    public static final class DeviceHardwareProfile {
        public final String manufacturer;
        public final String model;
        public final String board;
        public final String socPlatform;
        public final PlatformBackend.PlatformVendor vendor;
        public final String cpuArchitecture;
        public final int totalCoreCount;
        public final List<DetectedCluster> clusters;
        public final KernelCompatEngine.GpuInfo gpuInfo;
        public final List<KernelCompatEngine.BlockDevice> blockDevices;
        public final long totalRamMb;
        public final String kernelVersion;
        public final boolean isGki;
        public final boolean hasSchedtune;
        public final boolean hasUclamp;
        public final boolean hasCoreControl;
        public final AccessLevel accessLevel;

        public DeviceHardwareProfile(String manufacturer, String model, String board,
                                     String socPlatform, PlatformBackend.PlatformVendor vendor,
                                     String cpuArchitecture, int totalCoreCount,
                                     List<DetectedCluster> clusters,
                                     KernelCompatEngine.GpuInfo gpuInfo,
                                     List<KernelCompatEngine.BlockDevice> blockDevices,
                                     long totalRamMb, String kernelVersion, boolean isGki,
                                     boolean hasSchedtune, boolean hasUclamp, boolean hasCoreControl,
                                     AccessLevel accessLevel) {
            this.manufacturer = manufacturer;
            this.model = model;
            this.board = board;
            this.socPlatform = socPlatform;
            this.vendor = vendor;
            this.cpuArchitecture = cpuArchitecture;
            this.totalCoreCount = totalCoreCount;
            this.clusters = Collections.unmodifiableList(new ArrayList<>(clusters));
            this.gpuInfo = gpuInfo;
            this.blockDevices = Collections.unmodifiableList(new ArrayList<>(blockDevices));
            this.totalRamMb = totalRamMb;
            this.kernelVersion = kernelVersion;
            this.isGki = isGki;
            this.hasSchedtune = hasSchedtune;
            this.hasUclamp = hasUclamp;
            this.hasCoreControl = hasCoreControl;
            this.accessLevel = accessLevel;
        }

        public boolean isRooted() {
            return accessLevel == AccessLevel.AUTHORIZED_ROOT;
        }

        @Nullable
        public DetectedCluster getClusterByRole(ClusterRole role) {
            for (DetectedCluster c : clusters) {
                if (c.role == role) return c;
            }
            return null;
        }

        @Nullable
        public DetectedCluster getClusterForCore(int coreId) {
            for (DetectedCluster c : clusters) {
                if (c.coreIds.contains(coreId)) return c;
            }
            return null;
        }
    }

    private static volatile DeviceHardwareProfile sCachedProfile;
    private static final Object sLock = new Object();

    private UniversalHardwareDetector() {}

    /**
     * Discovers and resolves the comprehensive hardware profile for the current device.
     */
    @NonNull
    public static DeviceHardwareProfile detect(@Nullable Context context) {
        DeviceHardwareProfile cached = sCachedProfile;
        if (cached != null) return cached;

        synchronized (sLock) {
            if (sCachedProfile != null) return sCachedProfile;

            String manufacturer = Build.MANUFACTURER;
            String model = Build.MODEL;
            String board = Build.BOARD;

            String soc = RootUtils.getProp("ro.board.platform");
            if (soc.isEmpty()) soc = RootUtils.getProp("ro.hardware");
            if (soc.isEmpty()) soc = Build.HARDWARE;

            PlatformBackend backend = KernelCompatEngine.getPlatformBackend();
            PlatformBackend.PlatformVendor vendor = backend.getVendor();

            String abi = Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0
                    ? Build.SUPPORTED_ABIS[0] : System.getProperty("os.arch", "arm64-v8a");

            int totalCores = KernelCompatEngine.discoverCoreCount();
            String kernelVer = safeReadKernelVersion();
            boolean isGki = kernelVer.contains("-android") || kernelVer.startsWith("5.") || kernelVer.startsWith("6.");
            boolean hasSchedtune = NativeKernelAccess.exists("/dev/stune");
            boolean hasUclamp = NativeKernelAccess.exists("/dev/cpuctl/top-app/cpu.uclamp.min");
            boolean hasCoreControl = NativeKernelAccess.exists("/sys/devices/system/cpu/cpu0/core_ctl")
                    || NativeKernelAccess.exists("/sys/devices/system/cpu/cpu4/core_ctl");

            AccessLevel access = resolveAccessLevel();

            // Detect CPU Policies and categorize Cluster Roles
            List<KernelCompatEngine.CpuPolicy> rawPolicies = KernelCompatEngine.getCpuPolicies();
            List<DetectedCluster> clusters = categorizeClusters(rawPolicies);

            KernelCompatEngine.GpuInfo gpuInfo = KernelCompatEngine.getGpuInfo();
            List<KernelCompatEngine.BlockDevice> blockDevices = KernelCompatEngine.getBlockDevices();
            long ramMb = readTotalRamMb();

            sCachedProfile = new DeviceHardwareProfile(
                    manufacturer, model, board, soc, vendor, abi, totalCores,
                    clusters, gpuInfo, blockDevices, ramMb, kernelVer,
                    isGki, hasSchedtune, hasUclamp, hasCoreControl, access
            );
            return sCachedProfile;
        }
    }

    /**
     * Forces invalidation of cached hardware detections (e.g. after system, kernel, or ROM change).
     */
    public static void invalidate() {
        synchronized (sLock) {
            sCachedProfile = null;
            KernelCompatEngine.refresh();
        }
    }

    private static AccessLevel resolveAccessLevel() {
        try {
            if (RootUtils.rootAccess()) {
                return AccessLevel.AUTHORIZED_ROOT;
            }
        } catch (Throwable ignored) {}
        return AccessLevel.NON_ROOT_RESTRICTED;
    }

    /**
     * Dynamically categorizes CPU policies into Little, Mid, Big, Prime, or Unified clusters
     * by analyzing relative core counts, affected CPUs, and maximum frequencies.
     */
    @NonNull
    public static List<DetectedCluster> categorizeClusters(@NonNull List<KernelCompatEngine.CpuPolicy> rawPolicies) {
        List<DetectedCluster> result = new ArrayList<>();
        if (rawPolicies.isEmpty()) {
            return result;
        }

        if (rawPolicies.size() == 1) {
            KernelCompatEngine.CpuPolicy p = rawPolicies.get(0);
            result.add(new DetectedCluster(p.id, p.path, ClusterRole.UNIFIED, p.relatedCpus,
                    p.minFreq, p.maxFreq, p.availableFreqs, p.availableGovernors, p.currentGovernor, p.isWritable));
            return result;
        }

        // Sort policies by max frequency ascending
        List<KernelCompatEngine.CpuPolicy> sorted = new ArrayList<>(rawPolicies);
        Collections.sort(sorted, Comparator.comparingLong(a -> a.maxFreq));

        int count = sorted.size();
        if (count == 2) {
            // Dual-cluster (e.g., 4 Little + 4 Big, or 6 Little + 2 Big)
            KernelCompatEngine.CpuPolicy little = sorted.get(0);
            KernelCompatEngine.CpuPolicy big = sorted.get(1);

            result.add(new DetectedCluster(little.id, little.path, ClusterRole.LITTLE, little.relatedCpus,
                    little.minFreq, little.maxFreq, little.availableFreqs, little.availableGovernors, little.currentGovernor, little.isWritable));
            result.add(new DetectedCluster(big.id, big.path, ClusterRole.BIG, big.relatedCpus,
                    big.minFreq, big.maxFreq, big.availableFreqs, big.availableGovernors, big.currentGovernor, big.isWritable));
        } else if (count == 3) {
            // Tri-cluster (e.g. Snapdragon 8 Gen 1/2/3: 4 Little + 3 Mid + 1 Prime, or 2+2+4)
            KernelCompatEngine.CpuPolicy little = sorted.get(0);
            KernelCompatEngine.CpuPolicy mid = sorted.get(1);
            KernelCompatEngine.CpuPolicy prime = sorted.get(2);

            result.add(new DetectedCluster(little.id, little.path, ClusterRole.LITTLE, little.relatedCpus,
                    little.minFreq, little.maxFreq, little.availableFreqs, little.availableGovernors, little.currentGovernor, little.isWritable));
            result.add(new DetectedCluster(mid.id, mid.path, ClusterRole.MID, mid.relatedCpus,
                    mid.minFreq, mid.maxFreq, mid.availableFreqs, mid.availableGovernors, mid.currentGovernor, mid.isWritable));
            result.add(new DetectedCluster(prime.id, prime.path, ClusterRole.PRIME, prime.relatedCpus,
                    prime.minFreq, prime.maxFreq, prime.availableFreqs, prime.availableGovernors, prime.currentGovernor, prime.isWritable));
        } else {
            // 4+ clusters (e.g., MediaTek 10-core or complex heterogeneous big.LITTLE architectures)
            for (int i = 0; i < count; i++) {
                KernelCompatEngine.CpuPolicy p = sorted.get(i);
                ClusterRole role;
                if (i == 0) {
                    role = ClusterRole.LITTLE;
                } else if (i == count - 1) {
                    role = ClusterRole.PRIME;
                } else if (i == count - 2) {
                    role = ClusterRole.BIG;
                } else {
                    role = ClusterRole.MID;
                }
                result.add(new DetectedCluster(p.id, p.path, role, p.relatedCpus,
                        p.minFreq, p.maxFreq, p.availableFreqs, p.availableGovernors, p.currentGovernor, p.isWritable));
            }
        }

        // Return sorted by policy ID for consistent indexing
        Collections.sort(result, Comparator.comparingInt(a -> a.policyId));
        return result;
    }

    private static String safeReadKernelVersion() {
        String ver = NativeKernelAccess.readString("/proc/version");
        if (ver != null && !ver.trim().isEmpty()) {
            return ver.trim();
        }
        ver = RootUtils.runAndGetOutput("uname -r 2>/dev/null");
        if (!ver.isEmpty()) return ver.trim();
        return System.getProperty("os.version", "unknown");
    }

    private static long readTotalRamMb() {
        String meminfo = NativeKernelAccess.readString("/proc/meminfo");
        if (meminfo != null) {
            for (String line : meminfo.split("\n")) {
                if (line.startsWith("MemTotal:")) {
                    String clean = line.replace("MemTotal:", "").replace("kB", "").trim();
                    try {
                        long kb = Long.parseLong(clean);
                        return kb / 1024L;
                    } catch (Exception ignored) {}
                }
            }
        }
        return Runtime.getRuntime().maxMemory() / (1024L * 1024L);
    }
}

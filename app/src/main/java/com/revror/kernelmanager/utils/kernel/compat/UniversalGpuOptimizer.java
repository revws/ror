package com.revror.kernelmanager.utils.kernel.compat;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.revror.kernelmanager.utils.kernel.compat.NativeKernelAccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Universal Device-Adaptive GPU Optimization System.
 * Supports Qualcomm Adreno (KGSL), ARM Mali, PowerVR / IMG Rogue,
 * and generic Linux Devfreq GPU interfaces across all Android SoCs.
 */
public final class UniversalGpuOptimizer {

    private static final String TAG = "UniversalGpuOptimizer";

    public static final class GpuPlan {
        public final KernelCompatEngine.GpuInfo.DriverType driverType;
        public final long targetMinFreq;
        public final long targetMaxFreq;
        public final String targetGovernor;
        public final List<String> appliedTweaks;

        public GpuPlan(KernelCompatEngine.GpuInfo.DriverType driverType,
                       long targetMinFreq, long targetMaxFreq, String targetGovernor,
                       List<String> appliedTweaks) {
            this.driverType = driverType;
            this.targetMinFreq = targetMinFreq;
            this.targetMaxFreq = targetMaxFreq;
            this.targetGovernor = targetGovernor;
            this.appliedTweaks = Collections.unmodifiableList(new ArrayList<>(appliedTweaks));
        }
    }

    private static String sBaselineGovernor = "";
    private static long sBaselineMinFreq = -1;
    private static long sBaselineMaxFreq = -1;
    private static boolean sGpuBaselineCaptured = false;

    private UniversalGpuOptimizer() {}

    /**
     * Captures baseline GPU state for clean restoration.
     */
    public static synchronized void captureBaselineIfEmpty() {
        if (sGpuBaselineCaptured) return;
        KernelCompatEngine.GpuInfo info = KernelCompatEngine.getGpuInfo();
        if (info.driverType == KernelCompatEngine.GpuInfo.DriverType.UNKNOWN) return;

        sBaselineGovernor = info.currentGovernor;
        if (!info.minFreqPath.isEmpty()) {
            sBaselineMinFreq = parseLong(NativeKernelAccess.readString(info.minFreqPath), -1);
        }
        if (!info.maxFreqPath.isEmpty()) {
            sBaselineMaxFreq = parseLong(NativeKernelAccess.readString(info.maxFreqPath), -1);
        }
        sGpuBaselineCaptured = true;
    }

    /**
     * Restores stock GPU baseline settings.
     */
    @NonNull
    public static synchronized List<String> restoreBaseline() {
        List<String> report = new ArrayList<>();
        if (!sGpuBaselineCaptured) {
            report.add("No GPU baseline captured to restore");
            return report;
        }

        KernelCompatEngine.GpuInfo info = KernelCompatEngine.getGpuInfo();
        if (info.driverType == KernelCompatEngine.GpuInfo.DriverType.UNKNOWN) return report;

        if (!info.governorPath.isEmpty() && !sBaselineGovernor.isEmpty()) {
            NativeKernelAccess.writeString(info.governorPath, sBaselineGovernor);
            report.add("Restored GPU governor: " + sBaselineGovernor);
        }
        if (!info.minFreqPath.isEmpty() && sBaselineMinFreq > 0) {
            NativeKernelAccess.writeString(info.minFreqPath, String.valueOf(sBaselineMinFreq));
        }
        if (!info.maxFreqPath.isEmpty() && sBaselineMaxFreq > 0) {
            NativeKernelAccess.writeString(info.maxFreqPath, String.valueOf(sBaselineMaxFreq));
        }
        report.add("Restored GPU frequencies to baseline");
        return report;
    }

    /**
     * Universal profile application for detected GPU hardware.
     */
    @NonNull
    public static List<String> applyProfile(int profileId, boolean isPhoenix) {
        captureBaselineIfEmpty();
        List<String> logs = new ArrayList<>();

        KernelCompatEngine.GpuInfo info = KernelCompatEngine.getGpuInfo();
        if (info.driverType == KernelCompatEngine.GpuInfo.DriverType.UNKNOWN) {
            logs.add("No controllable GPU devfreq interface exposed on this kernel");
            return logs;
        }

        List<Long> freqs = info.availableFreqs;
        long minAvailable = freqs.isEmpty() ? 0 : freqs.get(0);
        long maxAvailable = freqs.isEmpty() ? 0 : freqs.get(freqs.size() - 1);

        long targetMin;
        long targetMax;
        String preferredGov;

        // Profile Mapping
        if ((!isPhoenix && (profileId == 2 || profileId == 3)) || (isPhoenix && (profileId == 0 || profileId == 2))) {
            // Battery Saver (Regular 2 Battery, Regular 3 CoolDown, Phoenix 0 Battery+, Phoenix 2 Thermal Cooldown)
            targetMin = minAvailable;
            targetMax = getPercentileFreq(freqs, isPhoenix ? 0.35f : 0.45f);
            preferredGov = "powersave";
        } else if ((!isPhoenix && profileId == 0) || (isPhoenix && profileId == 1)) {
            // Daily Usage (Regular 0) / Balanced+ (Phoenix 1)
            targetMin = minAvailable;
            targetMax = getPercentileFreq(freqs, 0.70f);
            preferredGov = info.driverType == KernelCompatEngine.GpuInfo.DriverType.ADRENO
                    ? "msm-adreno-tz" : "simple_ondemand";
        } else if (!isPhoenix && profileId == 1) {
            // Balanced (Regular 1)
            targetMin = getPercentileFreq(freqs, 0.20f);
            targetMax = maxAvailable;
            preferredGov = info.driverType == KernelCompatEngine.GpuInfo.DriverType.ADRENO
                    ? "msm-adreno-tz" : "simple_ondemand";
        } else if ((!isPhoenix && profileId == 4) || (isPhoenix && (profileId == 3 || profileId == 4))) {
            // Gaming (Regular 4 Gaming, Phoenix 3 Gaming Max, Phoenix 4 Smooth Gaming+)
            targetMin = getPercentileFreq(freqs, isPhoenix ? 0.60f : 0.45f);
            targetMax = maxAvailable;
            preferredGov = isPhoenix ? "performance"
                    : (info.driverType == KernelCompatEngine.GpuInfo.DriverType.ADRENO ? "msm-adreno-tz" : "performance");
        } else if (profileId == 6 || (isPhoenix && profileId == 7)) {
            // Esports / Apex
            targetMin = getPercentileFreq(freqs, isPhoenix ? 0.75f : 0.60f);
            targetMax = maxAvailable;
            preferredGov = "performance";
        } else {
            // Ultra Gaming (5) / Ultra+
            targetMin = isPhoenix ? maxAvailable : getPercentileFreq(freqs, 0.85f);
            targetMax = maxAvailable;
            preferredGov = "performance";
        }

        // Resolve governor against available list
        String resolvedGov = resolveGpuGovernor(preferredGov, info.availableGovernors);
        if (!info.governorPath.isEmpty() && !resolvedGov.isEmpty()) {
            NativeKernelAccess.writeString(info.governorPath, resolvedGov);
            logs.add("GPU Governor set to: " + resolvedGov);
        }

        // Apply Frequency Limits with Safe Write Ordering
        if (targetMin > 0 && targetMax > 0) {
            // When raising, write max first; when lowering, write min first
            long curMin = parseLong(NativeKernelAccess.readString(info.minFreqPath), 0);
            if (targetMax >= curMin) {
                if (!info.maxFreqPath.isEmpty()) NativeKernelAccess.writeString(info.maxFreqPath, String.valueOf(targetMax));
                if (!info.minFreqPath.isEmpty()) NativeKernelAccess.writeString(info.minFreqPath, String.valueOf(targetMin));
            } else {
                if (!info.minFreqPath.isEmpty()) NativeKernelAccess.writeString(info.minFreqPath, String.valueOf(targetMin));
                if (!info.maxFreqPath.isEmpty()) NativeKernelAccess.writeString(info.maxFreqPath, String.valueOf(targetMax));
            }
            logs.add("GPU Frequency bounds set: " + (targetMin / 1_000_000) + "MHz - " + (targetMax / 1_000_000) + "MHz");
        }

        // Hardware-specific fine tuning
        if (info.driverType == KernelCompatEngine.GpuInfo.DriverType.ADRENO) {
            applyAdrenoTweaks(info.basePath, profileId, isPhoenix, logs);
        } else if (info.driverType == KernelCompatEngine.GpuInfo.DriverType.MALI) {
            applyMaliTweaks(info.basePath, profileId, isPhoenix, logs);
        }

        return logs;
    }

    private static void applyAdrenoTweaks(String base, int profileId, boolean isPhoenix, List<String> logs) {
        if (profileId == 5 || (isPhoenix && profileId == 4)) {
            // Gaming / Ultra Gaming
            NativeKernelAccess.writeString(base + "/force_bus_on", "1");
            NativeKernelAccess.writeString(base + "/force_clk_on", "1");
            NativeKernelAccess.writeString(base + "/force_rail_on", "1");
            NativeKernelAccess.writeString(base + "/throttling", "0");
            NativeKernelAccess.writeString(base + "/idle_timer", "100000");
            NativeKernelAccess.writeString(base + "/devfreq/adrenoboost", isPhoenix ? "3" : "2");
            NativeKernelAccess.writeString("/sys/module/adreno_idler/parameters/adreno_idler_active", "N");
            logs.add("Adreno power rails, adrenoboost, and frame stabilization active");
        } else if (profileId == 2 || profileId == 3) {
            // Battery Saver
            NativeKernelAccess.writeString(base + "/force_bus_on", "0");
            NativeKernelAccess.writeString(base + "/force_clk_on", "0");
            NativeKernelAccess.writeString(base + "/force_rail_on", "0");
            NativeKernelAccess.writeString(base + "/throttling", "1");
            NativeKernelAccess.writeString(base + "/idle_timer", "64");
            NativeKernelAccess.writeString(base + "/devfreq/adrenoboost", "0");
            NativeKernelAccess.writeString("/sys/module/adreno_idler/parameters/adreno_idler_active", "Y");
            logs.add("Adreno power conservation and idler active");
        } else {
            // Balanced / Daily
            NativeKernelAccess.writeString(base + "/force_bus_on", "0");
            NativeKernelAccess.writeString(base + "/force_clk_on", "0");
            NativeKernelAccess.writeString(base + "/force_rail_on", "0");
            NativeKernelAccess.writeString(base + "/throttling", "1");
            NativeKernelAccess.writeString(base + "/idle_timer", "80");
            NativeKernelAccess.writeString(base + "/devfreq/adrenoboost", "1");
        }
    }

    private static void applyMaliTweaks(String base, int profileId, boolean isPhoenix, List<String> logs) {
        if (profileId >= 4) {
            NativeKernelAccess.writeString(base + "/power_policy", "coarse_demand");
            NativeKernelAccess.writeString(base + "/dvfs_enable", "1");
            NativeKernelAccess.writeString("/sys/module/mali/parameters/mali_gpu_utilization_timeout", "250");
            logs.add("ARM Mali DVFS tuned for high performance");
        } else if (profileId == 2 || profileId == 3) {
            NativeKernelAccess.writeString(base + "/power_policy", "always_on");
            NativeKernelAccess.writeString("/sys/module/mali/parameters/mali_gpu_utilization_timeout", "1000");
            logs.add("ARM Mali power policy set to energy conservation");
        }
    }

    private static long getPercentileFreq(List<Long> freqs, float pct) {
        if (freqs.isEmpty()) return 0;
        if (pct <= 0f) return freqs.get(0);
        if (pct >= 1f) return freqs.get(freqs.size() - 1);
        int idx = Math.round(pct * (freqs.size() - 1));
        return freqs.get(Math.max(0, Math.min(freqs.size() - 1, idx)));
    }

    private static String resolveGpuGovernor(String preferred, List<String> available) {
        if (available.isEmpty()) return preferred;
        if (available.contains(preferred)) return preferred;

        String[] fallbacks;
        if ("performance".equalsIgnoreCase(preferred)) {
            fallbacks = new String[]{"performance", "msm-adreno-tz", "simple_ondemand", "mali_ondemand"};
        } else if ("powersave".equalsIgnoreCase(preferred)) {
            fallbacks = new String[]{"powersave", "simple_ondemand", "msm-adreno-tz"};
        } else {
            fallbacks = new String[]{"msm-adreno-tz", "simple_ondemand", "performance", "powersave"};
        }

        for (String fb : fallbacks) {
            for (String av : available) {
                if (av.equalsIgnoreCase(fb)) return av;
            }
        }
        return available.get(0);
    }

    private static long parseLong(String val, long fallback) {
        if (val == null) return fallback;
        try { return Long.parseLong(val.trim()); } catch (Exception ignored) { return fallback; }
    }
}

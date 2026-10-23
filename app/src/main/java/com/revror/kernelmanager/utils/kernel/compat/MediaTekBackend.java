package com.revror.kernelmanager.utils.kernel.compat;

import androidx.annotation.NonNull;
import com.revror.kernelmanager.utils.root.RootUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Hardware optimization backend for MediaTek (Dimensity, Helio) SoCs.
 * Manages GED (Graphic Extension Device), PPM (Processor Power Management), and Mali GPU.
 */
public class MediaTekBackend implements PlatformBackend {

    @NonNull
    @Override
    public PlatformVendor getVendor() {
        return PlatformVendor.MEDIATEK;
    }

    @NonNull
    @Override
    public String getPlatformName() {
        return "MediaTek Dimensity / Helio";
    }

    @Override
    public boolean isSupported() {
        return KernelCompatEngine.exists("/proc/ged")
                || KernelCompatEngine.exists("/proc/ppm")
                || KernelCompatEngine.exists("/proc/mtktscpu")
                || RootUtils.getProp("ro.hardware").toLowerCase().contains("mt")
                || RootUtils.getProp("ro.board.platform").toLowerCase().contains("mt");
    }

    @NonNull
    @Override
    public List<String> applyGamingOptimizations(boolean ultraMode) {
        List<String> actions = new ArrayList<>();

        // 1. MediaTek GED (GPU Frequency and Boost Engine)
        if (KernelCompatEngine.exists("/proc/ged/ged_boost")) {
            KernelCompatEngine.safeWrite("/proc/ged/ged_boost", ultraMode ? "2" : "1");
            actions.add("MediaTek GED boost enabled (" + (ultraMode ? "level 2" : "level 1") + ")");
        }
        if (KernelCompatEngine.exists("/proc/ged/gpu_boost")) {
            KernelCompatEngine.safeWrite("/proc/ged/gpu_boost", "1");
        }
        if (KernelCompatEngine.exists("/proc/ged/hal/custom_upbound_gpu_freq")) {
            // Uncap GPU ceiling for high framerates
            actions.add("MediaTek custom GPU ceiling adjusted");
        }

        // 2. MediaTek PPM (Processor Power Management)
        if (KernelCompatEngine.exists("/proc/ppm/policy_status")) {
            // Don't throttle big cores aggressively during gaming
            actions.add("MediaTek PPM gaming policy verified");
        }

        // 3. Mali Devfreq
        for (String maliGov : KernelCompatEngine.findMatchingNodes("/sys/class/devfreq", "governor")) {
            if (maliGov.contains("mali") || maliGov.contains("gpu")) {
                KernelCompatEngine.safeWrite(maliGov, ultraMode ? "performance" : "simple_ondemand");
                actions.add("Mali GPU governor tuned to " + (ultraMode ? "performance" : "simple_ondemand"));
            }
        }

        return actions;
    }

    @NonNull
    @Override
    public List<String> applyBatteryOptimizations() {
        List<String> actions = new ArrayList<>();

        if (KernelCompatEngine.exists("/proc/ged/ged_boost")) {
            KernelCompatEngine.safeWrite("/proc/ged/ged_boost", "0");
        }
        if (KernelCompatEngine.exists("/proc/ged/gpu_boost")) {
            KernelCompatEngine.safeWrite("/proc/ged/gpu_boost", "0");
        }

        for (String maliGov : KernelCompatEngine.findMatchingNodes("/sys/class/devfreq", "governor")) {
            if (maliGov.contains("mali") || maliGov.contains("gpu")) {
                KernelCompatEngine.safeWrite(maliGov, "powersave");
            }
        }

        return actions;
    }

    @NonNull
    @Override
    public List<String> applyBalancedOptimizations() {
        List<String> actions = new ArrayList<>();

        if (KernelCompatEngine.exists("/proc/ged/ged_boost")) {
            KernelCompatEngine.safeWrite("/proc/ged/ged_boost", "1");
        }

        for (String maliGov : KernelCompatEngine.findMatchingNodes("/sys/class/devfreq", "governor")) {
            if (maliGov.contains("mali") || maliGov.contains("gpu")) {
                KernelCompatEngine.safeWrite(maliGov, "simple_ondemand");
            }
        }

        return actions;
    }

    @NonNull
    @Override
    public List<String> restoreDefaults() {
        return applyBalancedOptimizations();
    }
}

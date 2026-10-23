package com.revror.kernelmanager.utils.kernel.compat;

import androidx.annotation.NonNull;
import com.revror.kernelmanager.utils.root.RootUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Hardware optimization backend for Samsung Exynos SoCs.
 * Manages Samsung ECT, Mali GPU, and big.LITTLE / tri-cluster energy models.
 */
public class ExynosBackend implements PlatformBackend {

    @NonNull
    @Override
    public PlatformVendor getVendor() {
        return PlatformVendor.EXYNOS;
    }

    @NonNull
    @Override
    public String getPlatformName() {
        return "Samsung Exynos";
    }

    @Override
    public boolean isSupported() {
        return KernelCompatEngine.exists("/sys/devices/platform/11400000.mali")
                || KernelCompatEngine.exists("/sys/module/exynos_thermal")
                || RootUtils.getProp("ro.hardware").toLowerCase().contains("exynos")
                || RootUtils.getProp("ro.board.platform").toLowerCase().contains("exynos");
    }

    @NonNull
    @Override
    public List<String> applyGamingOptimizations(boolean ultraMode) {
        List<String> actions = new ArrayList<>();

        // Exynos Mali devfreq
        for (String gov : KernelCompatEngine.findMatchingNodes("/sys/class/devfreq", "governor")) {
            if (gov.contains("mali") || gov.contains("11400000")) {
                KernelCompatEngine.safeWrite(gov, ultraMode ? "performance" : "simple_ondemand");
                actions.add("Exynos Mali GPU governor tuned to " + (ultraMode ? "performance" : "simple_ondemand"));
            }
        }

        // Samsung interactive/schedutil tunables
        for (String rate : KernelCompatEngine.findMatchingNodes("/sys/devices/system/cpu/cpufreq", "up_rate_limit_us")) {
            KernelCompatEngine.safeWrite(rate, ultraMode ? "200" : "500");
        }

        return actions;
    }

    @NonNull
    @Override
    public List<String> applyBatteryOptimizations() {
        List<String> actions = new ArrayList<>();
        for (String gov : KernelCompatEngine.findMatchingNodes("/sys/class/devfreq", "governor")) {
            if (gov.contains("mali") || gov.contains("11400000")) {
                KernelCompatEngine.safeWrite(gov, "powersave");
            }
        }
        return actions;
    }

    @NonNull
    @Override
    public List<String> applyBalancedOptimizations() {
        List<String> actions = new ArrayList<>();
        for (String gov : KernelCompatEngine.findMatchingNodes("/sys/class/devfreq", "governor")) {
            if (gov.contains("mali") || gov.contains("11400000")) {
                KernelCompatEngine.safeWrite(gov, "simple_ondemand");
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

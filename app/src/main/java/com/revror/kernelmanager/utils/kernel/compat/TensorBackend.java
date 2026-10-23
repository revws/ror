package com.revror.kernelmanager.utils.kernel.compat;

import androidx.annotation.NonNull;
import com.revror.kernelmanager.utils.root.RootUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Hardware optimization backend for Google Tensor SoCs (Pixel 6, 7, 8, 9, Fold).
 * Uses modern GKI / UCLAMP schedutil tunables, Mali G78/G715 GPU, and Pixel power hal.
 */
public class TensorBackend implements PlatformBackend {

    @NonNull
    @Override
    public PlatformVendor getVendor() {
        return PlatformVendor.TENSOR;
    }

    @NonNull
    @Override
    public String getPlatformName() {
        return "Google Tensor";
    }

    @Override
    public boolean isSupported() {
        String hw = RootUtils.getProp("ro.hardware").toLowerCase();
        String soc = RootUtils.getProp("ro.soc.model").toLowerCase();
        return hw.contains("gs101") || hw.contains("gs201") || hw.contains("zuma")
                || soc.contains("tensor") || hw.contains("tensor");
    }

    @NonNull
    @Override
    public List<String> applyGamingOptimizations(boolean ultraMode) {
        List<String> actions = new ArrayList<>();

        // 1. Google Tensor UCLAMP Top-App boost (Android 12+)
        if (KernelCompatEngine.exists("/dev/cpuctl/top-app/cpu.uclamp.min")) {
            KernelCompatEngine.safeWrite("/dev/cpuctl/top-app/cpu.uclamp.min", ultraMode ? "60" : "30");
            KernelCompatEngine.safeWrite("/dev/cpuctl/top-app/cpu.uclamp.latency_sensitive", "1");
            actions.add("Google Tensor UCLAMP latency boost enabled (" + (ultraMode ? "60%" : "30%") + ")");
        }

        // 2. Mali GPU devfreq
        for (String gov : KernelCompatEngine.findMatchingNodes("/sys/class/devfreq", "governor")) {
            if (gov.contains("mali") || gov.contains("gpu")) {
                KernelCompatEngine.safeWrite(gov, ultraMode ? "performance" : "simple_ondemand");
                actions.add("Tensor Mali GPU governor tuned");
            }
        }

        // 3. Pixel thermal throttling headroom via cmd power
        if (ultraMode) {
            RootUtils.runCommand("cmd power set-fixed-performance-mode-enabled true 2>/dev/null");
            actions.add("Pixel fixed performance mode engaged");
        }

        return actions;
    }

    @NonNull
    @Override
    public List<String> applyBatteryOptimizations() {
        List<String> actions = new ArrayList<>();
        if (KernelCompatEngine.exists("/dev/cpuctl/top-app/cpu.uclamp.min")) {
            KernelCompatEngine.safeWrite("/dev/cpuctl/top-app/cpu.uclamp.min", "0");
            KernelCompatEngine.safeWrite("/dev/cpuctl/top-app/cpu.uclamp.latency_sensitive", "0");
        }
        RootUtils.runCommand("cmd power set-fixed-performance-mode-enabled false 2>/dev/null");
        return actions;
    }

    @NonNull
    @Override
    public List<String> applyBalancedOptimizations() {
        List<String> actions = new ArrayList<>();
        if (KernelCompatEngine.exists("/dev/cpuctl/top-app/cpu.uclamp.min")) {
            KernelCompatEngine.safeWrite("/dev/cpuctl/top-app/cpu.uclamp.min", "10");
            KernelCompatEngine.safeWrite("/dev/cpuctl/top-app/cpu.uclamp.latency_sensitive", "0");
        }
        RootUtils.runCommand("cmd power set-fixed-performance-mode-enabled false 2>/dev/null");
        return actions;
    }

    @NonNull
    @Override
    public List<String> restoreDefaults() {
        return applyBalancedOptimizations();
    }
}

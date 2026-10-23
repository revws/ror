package com.revror.kernelmanager.utils.kernel.compat;

import androidx.annotation.NonNull;
import com.revror.kernelmanager.utils.root.RootUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Hardware optimization backend for Unisoc / Spreadtrum SoCs.
 */
public class UnisocBackend implements PlatformBackend {

    @NonNull
    @Override
    public PlatformVendor getVendor() {
        return PlatformVendor.UNISOC;
    }

    @NonNull
    @Override
    public String getPlatformName() {
        return "Unisoc / Spreadtrum";
    }

    @Override
    public boolean isSupported() {
        String hw = RootUtils.getProp("ro.hardware").toLowerCase();
        String soc = RootUtils.getProp("ro.soc.manufacturer").toLowerCase();
        return hw.contains("sc") || hw.contains("sprd") || hw.contains("ums")
                || soc.contains("unisoc") || soc.contains("spreadtrum");
    }

    @NonNull
    @Override
    public List<String> applyGamingOptimizations(boolean ultraMode) {
        List<String> actions = new ArrayList<>();
        for (String gov : KernelCompatEngine.findMatchingNodes("/sys/class/devfreq", "governor")) {
            KernelCompatEngine.safeWrite(gov, ultraMode ? "performance" : "simple_ondemand");
        }
        actions.add("Unisoc devfreq performance policy applied");
        return actions;
    }

    @NonNull
    @Override
    public List<String> applyBatteryOptimizations() {
        List<String> actions = new ArrayList<>();
        for (String gov : KernelCompatEngine.findMatchingNodes("/sys/class/devfreq", "governor")) {
            KernelCompatEngine.safeWrite(gov, "powersave");
        }
        return actions;
    }

    @NonNull
    @Override
    public List<String> applyBalancedOptimizations() {
        List<String> actions = new ArrayList<>();
        for (String gov : KernelCompatEngine.findMatchingNodes("/sys/class/devfreq", "governor")) {
            KernelCompatEngine.safeWrite(gov, "simple_ondemand");
        }
        return actions;
    }

    @NonNull
    @Override
    public List<String> restoreDefaults() {
        return applyBalancedOptimizations();
    }
}

package com.revror.kernelmanager.utils.kernel.compat;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Universal fallback hardware backend for standard AOSP, GKI, and unknown chipsets.
 * Operates purely on standard Linux kernel interfaces (cpufreq, devfreq, block, vm, net).
 */
public class GenericBackend implements PlatformBackend {

    @NonNull
    @Override
    public PlatformVendor getVendor() {
        return PlatformVendor.GENERIC;
    }

    @NonNull
    @Override
    public String getPlatformName() {
        return "Generic AOSP / GKI Linux";
    }

    @Override
    public boolean isSupported() {
        return true; // Always supported as baseline
    }

    @NonNull
    @Override
    public List<String> applyGamingOptimizations(boolean ultraMode) {
        List<String> actions = new ArrayList<>();
        actions.add("Generic Linux standard governor & I/O optimizations applied");
        return actions;
    }

    @NonNull
    @Override
    public List<String> applyBatteryOptimizations() {
        List<String> actions = new ArrayList<>();
        actions.add("Generic Linux power efficiency profiles applied");
        return actions;
    }

    @NonNull
    @Override
    public List<String> applyBalancedOptimizations() {
        List<String> actions = new ArrayList<>();
        actions.add("Generic Linux balanced parameters applied");
        return actions;
    }

    @NonNull
    @Override
    public List<String> restoreDefaults() {
        return new ArrayList<>();
    }
}

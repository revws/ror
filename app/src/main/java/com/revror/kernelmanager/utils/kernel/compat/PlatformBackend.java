package com.revror.kernelmanager.utils.kernel.compat;

import androidx.annotation.NonNull;
import java.util.List;

/**
 * Common contract for device-specific optimization backends in ROR.
 * Provides platform-tailored tweaks for CPU, GPU, memory, scheduler, and power management.
 */
public interface PlatformBackend {

    enum PlatformVendor {
        QUALCOMM,
        MEDIATEK,
        EXYNOS,
        TENSOR,
        UNISOC,
        GENERIC
    }

    @NonNull
    PlatformVendor getVendor();

    @NonNull
    String getPlatformName();

    boolean isSupported();

    /**
     * Applies platform-specific performance/gaming optimizations.
     * @param ultraMode true if Ultra Gaming mode requested
     * @return List of executed commands or status notes
     */
    @NonNull
    List<String> applyGamingOptimizations(boolean ultraMode);

    /**
     * Applies platform-specific battery/power-saving optimizations.
     * @return List of executed commands or status notes
     */
    @NonNull
    List<String> applyBatteryOptimizations();

    /**
     * Applies platform-specific balanced optimizations.
     * @return List of executed commands or status notes
     */
    @NonNull
    List<String> applyBalancedOptimizations();

    /**
     * Restores default hardware parameters safely.
     * @return List of restoration commands
     */
    @NonNull
    List<String> restoreDefaults();
}

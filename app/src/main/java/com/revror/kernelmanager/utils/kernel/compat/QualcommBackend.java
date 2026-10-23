package com.revror.kernelmanager.utils.kernel.compat;

import androidx.annotation.NonNull;
import com.revror.kernelmanager.utils.root.RootUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Hardware optimization backend for Qualcomm Snapdragon SoCs.
 * Manages Adreno kgsl GPU, msm_performance QoS, cpu_boost, Adreno idler, and thermals.
 */
public class QualcommBackend implements PlatformBackend {

    @NonNull
    @Override
    public PlatformVendor getVendor() {
        return PlatformVendor.QUALCOMM;
    }

    @NonNull
    @Override
    public String getPlatformName() {
        return "Qualcomm Snapdragon";
    }

    @Override
    public boolean isSupported() {
        // Detect via kgsl or msm modules or hardware properties
        return KernelCompatEngine.exists("/sys/class/kgsl/kgsl-3d0")
                || KernelCompatEngine.exists("/sys/module/msm_performance")
                || KernelCompatEngine.exists("/sys/module/cpu_boost")
                || RootUtils.getProp("ro.hardware").toLowerCase().contains("qcom")
                || RootUtils.getProp("ro.soc.manufacturer").toLowerCase().contains("qti");
    }

    @NonNull
    @Override
    public List<String> applyGamingOptimizations(boolean ultraMode) {
        List<String> actions = new ArrayList<>();

        // 1. Adreno GPU boost
        if (KernelCompatEngine.exists("/sys/class/kgsl/kgsl-3d0")) {
            if (ultraMode) {
                KernelCompatEngine.safeWrite("/sys/class/kgsl/kgsl-3d0/force_bus_on", "1");
                KernelCompatEngine.safeWrite("/sys/class/kgsl/kgsl-3d0/force_clk_on", "1");
                KernelCompatEngine.safeWrite("/sys/class/kgsl/kgsl-3d0/force_rail_on", "1");
                KernelCompatEngine.safeWrite("/sys/class/kgsl/kgsl-3d0/throttling", "0");
                actions.add("Adreno GPU power rails forced active");
            }
            if (KernelCompatEngine.exists("/sys/class/kgsl/kgsl-3d0/devfreq/adrenoboost")) {
                KernelCompatEngine.safeWrite("/sys/class/kgsl/kgsl-3d0/devfreq/adrenoboost", ultraMode ? "3" : "2");
                actions.add("Adrenoboost enabled (" + (ultraMode ? "level 3" : "level 2") + ")");
            }
            if (KernelCompatEngine.exists("/sys/module/adreno_idler/parameters/adreno_idler_active")) {
                KernelCompatEngine.safeWrite("/sys/module/adreno_idler/parameters/adreno_idler_active", "N");
                actions.add("Adreno idler disabled for sustained framerate");
            }
            KernelCompatEngine.safeWrite("/sys/class/kgsl/kgsl-3d0/devfreq/governor", "msm-adreno-tz");
        }

        // 2. CPU input boost
        if (KernelCompatEngine.exists("/sys/module/cpu_boost/parameters/input_boost_ms")) {
            KernelCompatEngine.safeWrite("/sys/module/cpu_boost/parameters/input_boost_ms", ultraMode ? "120" : "60");
            actions.add("Qualcomm cpu_boost duration tuned");
        }

        // 3. Touchboost
        if (KernelCompatEngine.exists("/sys/module/msm_performance/parameters/touchboost")) {
            KernelCompatEngine.safeWrite("/sys/module/msm_performance/parameters/touchboost", "1");
            actions.add("Qualcomm touchboost enabled");
        }

        // 4. Devfreq memory latency governors
        String[] qcomDevfreqs = {
                "/sys/class/devfreq/soc:qcom,cpubw/governor",
                "/sys/class/devfreq/soc:qcom,gpubw/governor",
                "/sys/class/devfreq/soc:qcom,kgsl-busmon/governor",
                "/sys/class/devfreq/soc:qcom,mincpubw/governor"
        };
        for (String df : qcomDevfreqs) {
            if (KernelCompatEngine.exists(df)) {
                KernelCompatEngine.safeWrite(df, ultraMode ? "performance" : "bw_hwmon");
            }
        }

        return actions;
    }

    @NonNull
    @Override
    public List<String> applyBatteryOptimizations() {
        List<String> actions = new ArrayList<>();

        if (KernelCompatEngine.exists("/sys/class/kgsl/kgsl-3d0")) {
            KernelCompatEngine.safeWrite("/sys/class/kgsl/kgsl-3d0/force_bus_on", "0");
            KernelCompatEngine.safeWrite("/sys/class/kgsl/kgsl-3d0/force_clk_on", "0");
            KernelCompatEngine.safeWrite("/sys/class/kgsl/kgsl-3d0/force_rail_on", "0");
            KernelCompatEngine.safeWrite("/sys/class/kgsl/kgsl-3d0/throttling", "1");
            if (KernelCompatEngine.exists("/sys/class/kgsl/kgsl-3d0/devfreq/adrenoboost")) {
                KernelCompatEngine.safeWrite("/sys/class/kgsl/kgsl-3d0/devfreq/adrenoboost", "0");
            }
            if (KernelCompatEngine.exists("/sys/module/adreno_idler/parameters/adreno_idler_active")) {
                KernelCompatEngine.safeWrite("/sys/module/adreno_idler/parameters/adreno_idler_active", "Y");
                KernelCompatEngine.safeWrite("/sys/module/adreno_idler/parameters/adreno_idler_downdifferential", "35");
                KernelCompatEngine.safeWrite("/sys/module/adreno_idler/parameters/adreno_idler_idlewait", "25");
                actions.add("Adreno idler aggressive power saving enabled");
            }
        }

        if (KernelCompatEngine.exists("/sys/module/msm_performance/parameters/touchboost")) {
            KernelCompatEngine.safeWrite("/sys/module/msm_performance/parameters/touchboost", "0");
        }

        if (KernelCompatEngine.exists("/sys/module/cpu_boost/parameters/input_boost_ms")) {
            KernelCompatEngine.safeWrite("/sys/module/cpu_boost/parameters/input_boost_ms", "20");
        }

        return actions;
    }

    @NonNull
    @Override
    public List<String> applyBalancedOptimizations() {
        List<String> actions = new ArrayList<>();

        if (KernelCompatEngine.exists("/sys/class/kgsl/kgsl-3d0")) {
            KernelCompatEngine.safeWrite("/sys/class/kgsl/kgsl-3d0/force_bus_on", "0");
            KernelCompatEngine.safeWrite("/sys/class/kgsl/kgsl-3d0/force_clk_on", "0");
            KernelCompatEngine.safeWrite("/sys/class/kgsl/kgsl-3d0/force_rail_on", "0");
            KernelCompatEngine.safeWrite("/sys/class/kgsl/kgsl-3d0/throttling", "1");
            if (KernelCompatEngine.exists("/sys/class/kgsl/kgsl-3d0/devfreq/adrenoboost")) {
                KernelCompatEngine.safeWrite("/sys/class/kgsl/kgsl-3d0/devfreq/adrenoboost", "0");
            }
            if (KernelCompatEngine.exists("/sys/module/adreno_idler/parameters/adreno_idler_active")) {
                KernelCompatEngine.safeWrite("/sys/module/adreno_idler/parameters/adreno_idler_active", "Y");
            }
        }

        if (KernelCompatEngine.exists("/sys/module/msm_performance/parameters/touchboost")) {
            KernelCompatEngine.safeWrite("/sys/module/msm_performance/parameters/touchboost", "1");
        }

        if (KernelCompatEngine.exists("/sys/module/cpu_boost/parameters/input_boost_ms")) {
            KernelCompatEngine.safeWrite("/sys/module/cpu_boost/parameters/input_boost_ms", "40");
        }

        return actions;
    }

    @NonNull
    @Override
    public List<String> restoreDefaults() {
        List<String> actions = new ArrayList<>();
        applyBalancedOptimizations();
        // Unclamp QoS min and max freq
        if (KernelCompatEngine.exists("/sys/module/msm_performance/parameters/cpu_min_freq")) {
            KernelCompatEngine.safeWrite("/sys/module/msm_performance/parameters/cpu_min_freq", "0");
            actions.add("Qualcomm msm_performance min freq clamp reset");
        }
        return actions;
    }
}

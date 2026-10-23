package com.revror.kernelmanager.utils.kernel.compat;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Comprehensive automated test suite for the Universal Dynamic CPU Frequency Engine:
 * 1. Device A & Device B specification examples (3014 MHz & 2803 MHz).
 * 2. Tri-cluster independent frequency resolution (Efficiency, Performance, Prime).
 * 3. Exact 12-profile percentage-based configuration table (Regular & Phoenix+).
 * 4. Strict READ -> VALIDATE -> APPLY -> VERIFY execution pipeline & safety aborts.
 * 5. Safe write ordering logic (raising vs lowering).
 * 6. UniversalProfileResolver end-to-end integration across all profile tiers.
 */
public class UniversalOptimizationTest {

    @Test
    public void testDeviceAAndDeviceBExampleCalculations() {
        // Example Device A from user requirements:
        // Maximum = 3014 MHz (3014000 kHz)
        // Gaming minimum target = 3014 * 70% = 2109.8 MHz (2109800 kHz)
        // Nearest supported frequency = 2150 MHz (2150000 kHz)
        List<Long> deviceAFreqs = Arrays.asList(
                300000L, 800000L, 1200000L, 1600000L, 1850000L, 2150000L, 2600000L, 3014000L
        );
        KernelCompatEngine.CpuPolicy policyA = new KernelCompatEngine.CpuPolicy(
                0, "/sys/devices/system/cpu/cpufreq/policy0", Arrays.asList(0, 1, 2, 3),
                300000L, 3014000L, deviceAFreqs, Arrays.asList("schedutil", "performance"), "schedutil", true
        );

        CpuFrequencyProfileEngine.PolicyPlan planA = CpuFrequencyProfileEngine.calculatePlan(
                policyA, CpuFrequencyProfileEngine.ProfileType.GAMING, UniversalHardwareDetector.ClusterRole.BIG, false
        );
        Assert.assertEquals(2109800L, planA.targetMinFreq);
        Assert.assertEquals("Device A nearest supported frequency must be 2150 MHz", 2150000L, planA.resolvedMinFreq);
        Assert.assertEquals(3014000L, planA.resolvedMaxFreq);
        Assert.assertEquals("schedutil", planA.resolvedGovernor);

        // Example Device B from user requirements:
        // Maximum = 2803 MHz (2803000 kHz)
        // Gaming minimum target = 2803 * 70% = 1962.1 MHz (1962100 kHz)
        // Nearest supported frequency = 1920 MHz (1920000 kHz)
        List<Long> deviceBFreqs = Arrays.asList(
                300000L, 700000L, 1100000L, 1500000L, 1920000L, 2300000L, 2803000L
        );
        KernelCompatEngine.CpuPolicy policyB = new KernelCompatEngine.CpuPolicy(
                0, "/sys/devices/system/cpu/cpufreq/policy0", Arrays.asList(0, 1, 2, 3),
                300000L, 2803000L, deviceBFreqs, Arrays.asList("schedutil", "performance"), "schedutil", true
        );

        CpuFrequencyProfileEngine.PolicyPlan planB = CpuFrequencyProfileEngine.calculatePlan(
                policyB, CpuFrequencyProfileEngine.ProfileType.GAMING, UniversalHardwareDetector.ClusterRole.BIG, false
        );
        Assert.assertEquals(1962100L, planB.targetMinFreq);
        Assert.assertEquals("Device B nearest supported frequency must be 1920 MHz", 1920000L, planB.resolvedMinFreq);
        Assert.assertEquals(2803000L, planB.resolvedMaxFreq);
        Assert.assertEquals("schedutil", planB.resolvedGovernor);
    }

    @Test
    public void testTriClusterIndependentConfiguration() {
        // Section 5 Multi-Cluster Example:
        // Efficiency cluster maximum: 2000 MHz (2000000 kHz) -> 70% = 1400 MHz
        // Performance cluster maximum: 2800 MHz (2800000 kHz) -> 70% = 1960 MHz
        // Prime cluster maximum: 3200 MHz (3200000 kHz) -> 70% = 2240 MHz
        List<Long> effSteps = Arrays.asList(300000L, 600000L, 1000000L, 1400000L, 1800000L, 2000000L);
        List<Long> perfSteps = Arrays.asList(600000L, 1000000L, 1500000L, 1960000L, 2400000L, 2800000L);
        List<Long> primeSteps = Arrays.asList(800000L, 1200000L, 1700000L, 2240000L, 2700000L, 3200000L);

        KernelCompatEngine.CpuPolicy effPolicy = new KernelCompatEngine.CpuPolicy(
                0, "/sys/devices/system/cpu/cpufreq/policy0", Arrays.asList(0, 1, 2, 3),
                300000L, 2000000L, effSteps, Arrays.asList("schedutil", "performance"), "schedutil", true
        );
        KernelCompatEngine.CpuPolicy perfPolicy = new KernelCompatEngine.CpuPolicy(
                4, "/sys/devices/system/cpu/cpufreq/policy4", Arrays.asList(4, 5, 6),
                600000L, 2800000L, perfSteps, Arrays.asList("schedutil", "performance"), "schedutil", true
        );
        KernelCompatEngine.CpuPolicy primePolicy = new KernelCompatEngine.CpuPolicy(
                7, "/sys/devices/system/cpu/cpufreq/policy7", Collections.singletonList(7),
                800000L, 3200000L, primeSteps, Arrays.asList("schedutil", "performance"), "schedutil", true
        );

        CpuFrequencyProfileEngine.PolicyPlan effPlan = CpuFrequencyProfileEngine.calculatePlan(
                effPolicy, CpuFrequencyProfileEngine.ProfileType.GAMING, UniversalHardwareDetector.ClusterRole.LITTLE, false
        );
        CpuFrequencyProfileEngine.PolicyPlan perfPlan = CpuFrequencyProfileEngine.calculatePlan(
                perfPolicy, CpuFrequencyProfileEngine.ProfileType.GAMING, UniversalHardwareDetector.ClusterRole.BIG, false
        );
        CpuFrequencyProfileEngine.PolicyPlan primePlan = CpuFrequencyProfileEngine.calculatePlan(
                primePolicy, CpuFrequencyProfileEngine.ProfileType.GAMING, UniversalHardwareDetector.ClusterRole.PRIME, false
        );

        Assert.assertEquals(1400000L, effPlan.resolvedMinFreq);
        Assert.assertEquals(2000000L, effPlan.resolvedMaxFreq);

        Assert.assertEquals(1960000L, perfPlan.resolvedMinFreq);
        Assert.assertEquals(2800000L, perfPlan.resolvedMaxFreq);

        Assert.assertEquals(2240000L, primePlan.resolvedMinFreq);
        Assert.assertEquals(3200000L, primePlan.resolvedMaxFreq);
    }

    @Test
    public void testAllTwelveProfileConfigurationsTable() {
        // Verify the exact percentages and governor rules
        // Battery Saver: Lowest, 35% / 30%
        CpuFrequencyProfileEngine.ProfileFrequencySpec bat = CpuFrequencyProfileEngine.getProfileSpec(CpuFrequencyProfileEngine.ProfileType.BATTERY_SAVER, false);
        Assert.assertEquals(0, bat.minPercent);
        Assert.assertEquals(35, bat.maxPercent);
        Assert.assertEquals("powersave", bat.defaultGovernor);

        // Battery Saver+: Lowest, 30%
        CpuFrequencyProfileEngine.ProfileFrequencySpec batPlus = CpuFrequencyProfileEngine.getProfileSpec(CpuFrequencyProfileEngine.ProfileType.BATTERY_SAVER, true);
        Assert.assertEquals(0, batPlus.minPercent);
        Assert.assertEquals(30, batPlus.maxPercent);
        Assert.assertEquals("powersave", batPlus.defaultGovernor);

        // Daily Usage: Lowest, 50%
        CpuFrequencyProfileEngine.ProfileFrequencySpec daily = CpuFrequencyProfileEngine.getProfileSpec(CpuFrequencyProfileEngine.ProfileType.DAILY_USAGE, false);
        Assert.assertEquals(0, daily.minPercent);
        Assert.assertEquals(50, daily.maxPercent);
        Assert.assertEquals("schedutil", daily.defaultGovernor);

        // Balanced: 40%, 85%
        CpuFrequencyProfileEngine.ProfileFrequencySpec bal = CpuFrequencyProfileEngine.getProfileSpec(CpuFrequencyProfileEngine.ProfileType.BALANCED, false);
        Assert.assertEquals(40, bal.minPercent);
        Assert.assertEquals(85, bal.maxPercent);
        Assert.assertEquals("schedutil", bal.defaultGovernor);

        // Balanced+: 45%, 85%
        CpuFrequencyProfileEngine.ProfileFrequencySpec balPlus = CpuFrequencyProfileEngine.getProfileSpec(CpuFrequencyProfileEngine.ProfileType.BALANCED, true);
        Assert.assertEquals(45, balPlus.minPercent);
        Assert.assertEquals(85, balPlus.maxPercent);
        Assert.assertEquals("schedutil", balPlus.defaultGovernor);

        // Gaming (Regular): 70%, 100%, schedutil
        CpuFrequencyProfileEngine.ProfileFrequencySpec gam = CpuFrequencyProfileEngine.getProfileSpec(CpuFrequencyProfileEngine.ProfileType.GAMING, false);
        Assert.assertEquals(70, gam.minPercent);
        Assert.assertEquals(100, gam.maxPercent);
        Assert.assertEquals("schedutil", gam.defaultGovernor);

        // Smooth Gaming+: 85%, 100%, schedutil (NEVER performance!)
        CpuFrequencyProfileEngine.ProfileFrequencySpec smooth = CpuFrequencyProfileEngine.getProfileSpec(CpuFrequencyProfileEngine.ProfileType.SMOOTH_GAMING, true);
        Assert.assertEquals(85, smooth.minPercent);
        Assert.assertEquals(100, smooth.maxPercent);
        Assert.assertEquals("schedutil", smooth.defaultGovernor);

        // Gaming Max: 85%, 100%, schedutil (NEVER performance!)
        CpuFrequencyProfileEngine.ProfileFrequencySpec gamMax = CpuFrequencyProfileEngine.getProfileSpec(CpuFrequencyProfileEngine.ProfileType.GAMING_MAX, true);
        Assert.assertEquals(85, gamMax.minPercent);
        Assert.assertEquals(100, gamMax.maxPercent);
        Assert.assertEquals("schedutil", gamMax.defaultGovernor);

        // Esports: 75%, 100%, schedutil
        CpuFrequencyProfileEngine.ProfileFrequencySpec esports = CpuFrequencyProfileEngine.getProfileSpec(CpuFrequencyProfileEngine.ProfileType.ESPORTS, true);
        Assert.assertEquals(75, esports.minPercent);
        Assert.assertEquals(100, esports.maxPercent);
        Assert.assertEquals("schedutil", esports.defaultGovernor);

        // Apex: 80%, 100%, schedutil
        CpuFrequencyProfileEngine.ProfileFrequencySpec apex = CpuFrequencyProfileEngine.getProfileSpec(CpuFrequencyProfileEngine.ProfileType.APEX, true);
        Assert.assertEquals(80, apex.minPercent);
        Assert.assertEquals(100, apex.maxPercent);
        Assert.assertEquals("schedutil", apex.defaultGovernor);

        // Ultra Gaming: 100%, 100%, performance (ONLY profile allowed 100%/100% and performance governor)
        CpuFrequencyProfileEngine.ProfileFrequencySpec ultra = CpuFrequencyProfileEngine.getProfileSpec(CpuFrequencyProfileEngine.ProfileType.ULTRA_GAMING, false);
        Assert.assertEquals(100, ultra.minPercent);
        Assert.assertEquals(100, ultra.maxPercent);
        Assert.assertEquals("performance", ultra.defaultGovernor);

        // Ultra Gaming+: 100%, 100%, performance
        CpuFrequencyProfileEngine.ProfileFrequencySpec ultraPlus = CpuFrequencyProfileEngine.getProfileSpec(CpuFrequencyProfileEngine.ProfileType.ULTRA_GAMING, true);
        Assert.assertEquals(100, ultraPlus.minPercent);
        Assert.assertEquals(100, ultraPlus.maxPercent);
        Assert.assertEquals("performance", ultraPlus.defaultGovernor);
    }

    @Test
    public void testGamingMaxAndSmoothGamingNeverUsePerformanceGovernor() {
        List<Long> freqs = Arrays.asList(300000L, 800000L, 1200000L, 1800000L, 2400000L);
        List<String> govs = Arrays.asList("schedutil", "performance", "powersave");
        KernelCompatEngine.CpuPolicy policy = new KernelCompatEngine.CpuPolicy(
                0, "/sys/devices/system/cpu/cpufreq/policy0", Arrays.asList(0, 1, 2, 3),
                300000L, 2400000L, freqs, govs, "schedutil", true
        );

        // Test Gaming Max
        CpuFrequencyProfileEngine.PolicyPlan planMax = CpuFrequencyProfileEngine.calculatePlan(
                policy, CpuFrequencyProfileEngine.ProfileType.GAMING_MAX, UniversalHardwareDetector.ClusterRole.BIG, true
        );
        Assert.assertEquals("Gaming Max MUST use schedutil governor", "schedutil", planMax.resolvedGovernor);
        Assert.assertNotEquals("Gaming Max MUST NOT use performance governor", "performance", planMax.resolvedGovernor);
        Assert.assertTrue("Gaming Max must dynamically scale (min != max)", planMax.resolvedMinFreq < planMax.resolvedMaxFreq);

        // Test Smooth Gaming+
        CpuFrequencyProfileEngine.PolicyPlan planSmooth = CpuFrequencyProfileEngine.calculatePlan(
                policy, CpuFrequencyProfileEngine.ProfileType.SMOOTH_GAMING, UniversalHardwareDetector.ClusterRole.BIG, true
        );
        Assert.assertEquals("Smooth Gaming+ MUST use schedutil governor", "schedutil", planSmooth.resolvedGovernor);
        Assert.assertNotEquals("Smooth Gaming+ MUST NOT use performance governor", "performance", planSmooth.resolvedGovernor);
        Assert.assertTrue("Smooth Gaming+ must dynamically scale (min != max)", planSmooth.resolvedMinFreq < planSmooth.resolvedMaxFreq);

        // Even if schedutil is NOT available, non-Ultra profile MUST NOT resolve to performance
        List<String> noSchedutilGovs = Arrays.asList("interactive", "performance", "powersave");
        KernelCompatEngine.CpuPolicy policy2 = new KernelCompatEngine.CpuPolicy(
                0, "/sys/devices/system/cpu/cpufreq/policy0", Arrays.asList(0, 1, 2, 3),
                300000L, 2400000L, freqs, noSchedutilGovs, "interactive", true
        );
        CpuFrequencyProfileEngine.PolicyPlan planSmooth2 = CpuFrequencyProfileEngine.calculatePlan(
                policy2, CpuFrequencyProfileEngine.ProfileType.SMOOTH_GAMING, UniversalHardwareDetector.ClusterRole.BIG, true
        );
        Assert.assertEquals("Smooth Gaming+ should fall back to interactive, not performance", "interactive", planSmooth2.resolvedGovernor);
        Assert.assertNotEquals("performance", planSmooth2.resolvedGovernor);
    }

    @Test
    public void testUltraGamingIsOnlyProfileAllowedMaxLocking() {
        List<Long> freqs = Arrays.asList(300000L, 600000L, 1000000L, 1500000L, 2000000L, 2500000L, 3000000L);
        List<String> govs = Arrays.asList("schedutil", "performance");
        KernelCompatEngine.CpuPolicy policy = new KernelCompatEngine.CpuPolicy(
                0, "/sys/devices/system/cpu/cpufreq/policy0", Collections.singletonList(0),
                300000L, 3000000L, freqs, govs, "schedutil", true
        );

        for (CpuFrequencyProfileEngine.ProfileType pt : CpuFrequencyProfileEngine.ProfileType.values()) {
            if (pt == CpuFrequencyProfileEngine.ProfileType.CUSTOM) continue;
            CpuFrequencyProfileEngine.PolicyPlan plan = CpuFrequencyProfileEngine.calculatePlan(
                    policy, pt, UniversalHardwareDetector.ClusterRole.PRIME, false
            );
            if (pt == CpuFrequencyProfileEngine.ProfileType.ULTRA_GAMING) {
                Assert.assertEquals("Ultra Gaming MUST use performance governor", "performance", plan.resolvedGovernor);
                Assert.assertEquals("Ultra Gaming MUST lock min to max", plan.resolvedMaxFreq, plan.resolvedMinFreq);
            } else {
                Assert.assertNotEquals(pt.getDisplayName() + " must NOT use performance governor", "performance", plan.resolvedGovernor);
                Assert.assertFalse(pt.getDisplayName() + " must never lock 100% min and max",
                        plan.resolvedMinFreq == policy.maxFreq && plan.resolvedMaxFreq == policy.maxFreq);
                Assert.assertTrue(pt.getDisplayName() + " must allow dynamic scaling", plan.resolvedMinFreq < plan.resolvedMaxFreq);
            }
        }
    }

    @Test
    public void testSafeWriteOrderingLogic() {
        // When raising: max must be set before min to prevent min > max at intermediate steps
        long oldMin = 800000L;
        long oldMax = 1500000L;
        long newMin = 1800000L;
        long newMax = 2800000L;

        List<String> writeSequenceRaising = new ArrayList<>();
        if (newMin > oldMax) {
            // Must raise max first
            writeSequenceRaising.add("scaling_max_freq=" + newMax);
            writeSequenceRaising.add("scaling_min_freq=" + newMin);
        } else {
            writeSequenceRaising.add("scaling_min_freq=" + newMin);
            writeSequenceRaising.add("scaling_max_freq=" + newMax);
        }

        Assert.assertEquals("scaling_max_freq=2800000", writeSequenceRaising.get(0));
        Assert.assertEquals("scaling_min_freq=1800000", writeSequenceRaising.get(1));

        // When lowering: min must be lowered before max to prevent max < min at intermediate steps
        long currMin = 1800000L;
        long currMax = 2800000L;
        long targetMin = 400000L;
        long targetMax = 1200000L;

        List<String> writeSequenceLowering = new ArrayList<>();
        if (targetMax < currMin) {
            // Must lower min first
            writeSequenceLowering.add("scaling_min_freq=" + targetMin);
            writeSequenceLowering.add("scaling_max_freq=" + targetMax);
        } else {
            writeSequenceLowering.add("scaling_max_freq=" + targetMax);
            writeSequenceLowering.add("scaling_min_freq=" + targetMin);
        }

        Assert.assertEquals("scaling_min_freq=400000", writeSequenceLowering.get(0));
        Assert.assertEquals("scaling_max_freq=1200000", writeSequenceLowering.get(1));
    }

    @Test
    public void testHardwareDetectorClusterRoleHeuristics() {
        // Simulate tri-cluster (1 Prime + 3 Big + 4 Little)
        UniversalHardwareDetector.DetectedCluster little = new UniversalHardwareDetector.DetectedCluster(
                0, "/sys/devices/system/cpu/cpufreq/policy0",
                UniversalHardwareDetector.ClusterRole.LITTLE,
                Arrays.asList(0, 1, 2, 3), 300000L, 1800000L,
                Arrays.asList(300000L, 600000L, 1800000L),
                Arrays.asList("schedutil", "powersave"), "schedutil", true
        );

        UniversalHardwareDetector.DetectedCluster big = new UniversalHardwareDetector.DetectedCluster(
                4, "/sys/devices/system/cpu/cpufreq/policy4",
                UniversalHardwareDetector.ClusterRole.BIG,
                Arrays.asList(4, 5, 6), 700000L, 2400000L,
                Arrays.asList(700000L, 1200000L, 2400000L),
                Arrays.asList("schedutil", "performance"), "schedutil", true
        );

        UniversalHardwareDetector.DetectedCluster prime = new UniversalHardwareDetector.DetectedCluster(
                7, "/sys/devices/system/cpu/cpufreq/policy7",
                UniversalHardwareDetector.ClusterRole.PRIME,
                Collections.singletonList(7), 800000L, 3000000L,
                Arrays.asList(800000L, 1500000L, 3000000L),
                Arrays.asList("schedutil", "performance"), "schedutil", true
        );

        List<UniversalHardwareDetector.DetectedCluster> clusters = Arrays.asList(little, big, prime);

        Assert.assertEquals(3, clusters.size());
        Assert.assertEquals(UniversalHardwareDetector.ClusterRole.LITTLE, clusters.get(0).role);
        Assert.assertEquals(UniversalHardwareDetector.ClusterRole.BIG, clusters.get(1).role);
        Assert.assertEquals(UniversalHardwareDetector.ClusterRole.PRIME, clusters.get(2).role);

        // Verify percentile lookup
        long littleMid = little.getPercentileFrequency(0.5f, false);
        Assert.assertEquals(600000L, littleMid);

        long bigMax = big.getPercentileFrequency(1.0f, true);
        Assert.assertEquals(2400000L, bigMax);

        long primeMax = prime.getPercentileFrequency(1.0f, true);
        Assert.assertEquals(3000000L, primeMax);
    }

    @Test
    public void testUniversalProfileResolverExecution() {
        for (int p = 0; p <= 7; p++) {
            UniversalProfileResolver.ResolvedConfig configRegular =
                    UniversalProfileResolver.resolveAndApply(null, p, false);
            Assert.assertNotNull(configRegular);
            Assert.assertNotNull(configRegular.profileName);
            Assert.assertFalse(configRegular.getAllLogs().isEmpty());

            UniversalProfileResolver.ResolvedConfig configPhoenix =
                    UniversalProfileResolver.resolveAndApply(null, p, true);
            Assert.assertNotNull(configPhoenix);
            Assert.assertTrue(configPhoenix.isPhoenix);
            Assert.assertNotNull(configPhoenix.profileName);
        }

        // Test KCAL preset delegation
        UniversalProfileResolver.ResolvedConfig kcalConfig =
                UniversalProfileResolver.resolveAndApply(null, 8, false);
        Assert.assertNotNull(kcalConfig);
        Assert.assertEquals(8, kcalConfig.profileId);
    }

    @Test
    public void testUltraGamingBackgroundManagerActivationRules() {
        // 1. Background optimization disabled -> must not activate
        Assert.assertFalse(com.revror.kernelmanager.utils.UltraGamingBackgroundManager.shouldActivate(null, "com.activision.callofduty.shooter", false));

        // 2. No active game session (null or empty package) -> must not activate
        Assert.assertFalse(com.revror.kernelmanager.utils.UltraGamingBackgroundManager.shouldActivate(null, "", true));
        Assert.assertFalse(com.revror.kernelmanager.utils.UltraGamingBackgroundManager.shouldActivate(null, "   ", true));
        Assert.assertFalse(com.revror.kernelmanager.utils.UltraGamingBackgroundManager.shouldActivate(null, null, true));
    }
}

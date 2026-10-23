package com.revror.kernelmanager;

import com.revror.kernelmanager.utils.kernel.compat.KernelCompatEngine;
import com.revror.kernelmanager.utils.kernel.compat.KernelTweakRegistry;
import com.revror.kernelmanager.utils.kernel.compat.NativeKernelAccess;
import com.revror.kernelmanager.utils.kernel.compat.NativeProfileExecutor;
import com.revror.kernelmanager.utils.security.RorSecurityManager;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Unit test suite validating:
 * 1. Native kernel access direct file I/O (without shell commands).
 * 2. Centralized kernel tweak registry mappings, bounds, and equivalents.
 * 3. Dynamic governor and fallback path resolutions.
 * 4. Native profile executor mappings for all standard & Phoenix profiles.
 * 5. Security & remote config validation (blocking shell injections).
 */
public class NativeKernelControlTest {

    @Test
    public void testKernelTweakRegistryMappings() {
        Map<String, KernelTweakRegistry.TweakDefinition> tweaks = KernelTweakRegistry.getAll();
        Assert.assertFalse("Registry must not be empty", tweaks.isEmpty());

        // Verify critical categories exist
        Assert.assertTrue("CPU governor definition missing", tweaks.containsKey("CPU_SCALING_GOVERNOR"));
        Assert.assertTrue("VM swappiness definition missing", tweaks.containsKey("VM_SWAPPINESS"));
        Assert.assertTrue("IO scheduler definition missing", tweaks.containsKey("IO_SCHEDULER"));
        Assert.assertTrue("TCP congestion definition missing", tweaks.containsKey("TCP_CONGESTION"));

        // Verify oom_kill_allocating_task has verified safe fallback
        KernelTweakRegistry.TweakDefinition oomDef = tweaks.get("VM_OOM_KILL_ALLOCATING_TASK");
        Assert.assertNotNull("VM_OOM_KILL_ALLOCATING_TASK definition must exist", oomDef);
        Assert.assertFalse("oom candidates must have fallbacks", oomDef.getAllCandidatePaths().isEmpty());
    }

    @Test
    public void testNativeKernelAccessDirectFileIo() throws Exception {
        File temp = File.createTempFile("ror_native_test", ".txt");
        temp.deleteOnExit();

        // Direct write without shell
        String testVal = "1024";
        try (FileWriter fw = new FileWriter(temp)) {
            fw.write(testVal + "\n");
        }

        Assert.assertTrue("Native exists check must succeed", NativeKernelAccess.exists(temp.getAbsolutePath()));
        String readBack = NativeKernelAccess.readString(temp.getAbsolutePath());
        Assert.assertEquals("Direct readback must match written value", testVal, readBack);

        // Test non-existent path
        Assert.assertFalse("Non-existent path must return false", NativeKernelAccess.exists("/non/existent/path/for/ror/testing"));
        Assert.assertNull("Reading non-existent path must return null", NativeKernelAccess.readString("/non/existent/path/for/ror/testing"));
    }

    @Test
    public void testGovernorResolution() {
        List<String> available = Arrays.asList("schedutil", "powersave", "performance");

        String govPerf = KernelCompatEngine.resolveGovernor("performance", available);
        Assert.assertEquals("Governor resolution for performance failed", "performance", govPerf);

        String govSched = KernelCompatEngine.resolveGovernor("interactive", available);
        Assert.assertEquals("Governor resolution fallback to schedutil failed", "schedutil", govSched);

        String govPower = KernelCompatEngine.resolveGovernor("powersave", available);
        Assert.assertEquals("Governor resolution for powersave failed", "powersave", govPower);
    }

    @Test
    public void testNativeProfileExecutorNaming() {
        // Regular profiles
        Assert.assertEquals("Daily Use", NativeProfileExecutor.getProfileName(0, false));
        Assert.assertEquals("Balance", NativeProfileExecutor.getProfileName(1, false));
        Assert.assertEquals("Battery", NativeProfileExecutor.getProfileName(2, false));
        Assert.assertEquals("CoolDown", NativeProfileExecutor.getProfileName(3, false));
        Assert.assertEquals("Gaming", NativeProfileExecutor.getProfileName(4, false));
        Assert.assertEquals("Ultra Gaming", NativeProfileExecutor.getProfileName(5, false));

        // Phoenix profiles
        Assert.assertEquals("Battery+", NativeProfileExecutor.getProfileName(0, true));
        Assert.assertEquals("Balance+", NativeProfileExecutor.getProfileName(1, true));
        Assert.assertEquals("Thermal Cooldown", NativeProfileExecutor.getProfileName(2, true));
        Assert.assertEquals("Gaming Max", NativeProfileExecutor.getProfileName(3, true));
        Assert.assertEquals("Smooth Gaming+", NativeProfileExecutor.getProfileName(4, true));
        Assert.assertEquals("Ultra+", NativeProfileExecutor.getProfileName(5, true));
    }

    @Test
    public void testCpuFrequencyNearestFrequencySnapping() {
        List<Long> available = Arrays.asList(
                300_000L, 576_000L, 748_800L, 1_017_600L, 1_324_800L, 1_804_800L, 2_208_000L
        );

        // Snap ~500 MHz
        long snap500Min = com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.snapToNearestAvailable(
                500_000L, available, false);
        Assert.assertEquals(300_000L, snap500Min);

        long snap500Max = com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.snapToNearestAvailable(
                500_000L, available, true);
        Assert.assertEquals(576_000L, snap500Max);

        // Snap ~700 MHz
        long snap700Max = com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.snapToNearestAvailable(
                700_000L, available, true);
        Assert.assertEquals(748_800L, snap700Max);

        // Snap ~1300 MHz
        long snap1300Max = com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.snapToNearestAvailable(
                1_300_000L, available, true);
        Assert.assertEquals(1_324_800L, snap1300Max);

        // Extreme clamp beyond available maximum
        long snapOverMax = com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.snapToNearestAvailable(
                3_500_000L, available, true);
        Assert.assertEquals(2_208_000L, snapOverMax);
    }

    @Test
    public void testCpuFrequencyProfilePlanningAllProfiles() {
        List<Long> freqs = Arrays.asList(
                300_000L, 576_000L, 748_800L, 1_017_600L, 1_324_800L, 1_804_800L, 2_208_000L
        );
        List<String> govs = Arrays.asList("schedutil", "powersave", "performance");
        KernelCompatEngine.CpuPolicy policy = new KernelCompatEngine.CpuPolicy(
                0, "/sys/devices/system/cpu/cpufreq/policy0", Arrays.asList(0, 1, 2, 3),
                300_000L, 2_208_000L, freqs, govs, "schedutil", true
        );

        // 1. Battery Saver: ~500MHz min, ~700MHz max, powersave
        com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.PolicyPlan planBattery =
                com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.calculatePlan(
                        policy, com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.ProfileType.BATTERY_SAVER);
        Assert.assertEquals("powersave", planBattery.resolvedGovernor);
        Assert.assertTrue("Battery min must be <= max", planBattery.resolvedMinFreq <= planBattery.resolvedMaxFreq);
        Assert.assertEquals(748_800L, planBattery.resolvedMaxFreq);

        // 2. Daily Usage: lowest min, 50% max (1104 MHz target -> snaps to 1017.6 MHz), schedutil
        com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.PolicyPlan planDaily =
                com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.calculatePlan(
                        policy, com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.ProfileType.DAILY_USAGE);
        Assert.assertEquals("schedutil", planDaily.resolvedGovernor);
        Assert.assertTrue("Daily min must be <= max", planDaily.resolvedMinFreq <= planDaily.resolvedMaxFreq);
        Assert.assertEquals(1_017_600L, planDaily.resolvedMaxFreq);

        // 3. Balanced: ~1300MHz min, 85% max (snaps to 1804.8 MHz), schedutil
        com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.PolicyPlan planBalanced =
                com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.calculatePlan(
                        policy, com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.ProfileType.BALANCED);
        Assert.assertEquals("schedutil", planBalanced.resolvedGovernor);
        Assert.assertEquals(1_804_800L, planBalanced.resolvedMaxFreq);
        Assert.assertTrue("Balanced min must be <= max", planBalanced.resolvedMinFreq <= planBalanced.resolvedMaxFreq);

        // 4. Gaming: elevated min (~70% of max), max available, schedutil (never performance)
        com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.PolicyPlan planGaming =
                com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.calculatePlan(
                        policy, com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.ProfileType.GAMING);
        Assert.assertEquals("schedutil", planGaming.resolvedGovernor);
        Assert.assertEquals(2_208_000L, planGaming.resolvedMaxFreq);
        Assert.assertTrue("Gaming min must be elevated", planGaming.resolvedMinFreq >= 1_300_000L);

        // 5. High Performance: high min (~85% of max), max available, schedutil
        com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.PolicyPlan planHighPerf =
                com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.calculatePlan(
                        policy, com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.ProfileType.HIGH_PERFORMANCE);
        Assert.assertEquals("schedutil", planHighPerf.resolvedGovernor);
        Assert.assertEquals(2_208_000L, planHighPerf.resolvedMaxFreq);
        Assert.assertTrue("High perf min must be >= 1.8GHz", planHighPerf.resolvedMinFreq >= 1_800_000L);

        // 6. Ultra Gaming: Min = Max = Highest supported frequency strictly clamped, performance
        com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.PolicyPlan planUltra =
                com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.calculatePlan(
                        policy, com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.ProfileType.ULTRA_GAMING);
        Assert.assertEquals("performance", planUltra.resolvedGovernor);
        Assert.assertEquals(2_208_000L, planUltra.resolvedMinFreq);
        Assert.assertEquals(2_208_000L, planUltra.resolvedMaxFreq);
        Assert.assertEquals("Ultra Gaming must clamp min and max to same peak value",
                planUltra.resolvedMinFreq, planUltra.resolvedMaxFreq);
    }

    @Test
    public void testProfileCoordinatorFiniteStateMachine() {
        com.revror.kernelmanager.utils.kernel.compat.ProfileExecutionCoordinator coordinator =
                com.revror.kernelmanager.utils.kernel.compat.ProfileExecutionCoordinator.getInstance();
        Assert.assertNotNull("Coordinator instance must not be null", coordinator);
        Assert.assertFalse("Initial state must not be busy", coordinator.isBusy());

        // Validate FSM states
        for (com.revror.kernelmanager.utils.kernel.compat.ProfileExecutionCoordinator.State s :
                com.revror.kernelmanager.utils.kernel.compat.ProfileExecutionCoordinator.State.values()) {
            Assert.assertNotNull(s.getDisplayName());
            Assert.assertFalse(s.getDisplayName().isEmpty());
        }

        // Active profile and applying profile must be tracked separately
        Assert.assertEquals(-1, coordinator.getCurrentApplyingProfile());
    }

    @Test
    public void testProfileSwitchingSequenceSimulation() {
        List<Long> freqs = Arrays.asList(
                300_000L, 576_000L, 748_800L, 1_017_600L, 1_324_800L, 1_804_800L, 2_208_000L
        );
        List<String> govs = Arrays.asList("schedutil", "powersave", "performance");
        KernelCompatEngine.CpuPolicy policy = new KernelCompatEngine.CpuPolicy(
                0, "/sys/devices/system/cpu/cpufreq/policy0", Arrays.asList(0, 1, 2, 3),
                300_000L, 2_208_000L, freqs, govs, "schedutil", true
        );

        // 1. Battery Saver
        com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.PolicyPlan planBattery =
                com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.calculatePlan(
                        policy, com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.ProfileType.BATTERY_SAVER);
        Assert.assertEquals("powersave", planBattery.resolvedGovernor);
        Assert.assertTrue(planBattery.resolvedMinFreq <= planBattery.resolvedMaxFreq);

        // 2. Switch Battery -> Balanced
        com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.PolicyPlan planBalanced =
                com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.calculatePlan(
                        policy, com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.ProfileType.BALANCED);
        Assert.assertEquals("schedutil", planBalanced.resolvedGovernor);
        Assert.assertTrue(planBalanced.resolvedMinFreq > planBattery.resolvedMinFreq);
        Assert.assertEquals(1_804_800L, planBalanced.resolvedMaxFreq);

        // 3. Switch Balanced -> Ultra Gaming
        com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.PolicyPlan planUltra =
                com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.calculatePlan(
                        policy, com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine.ProfileType.ULTRA_GAMING);
        Assert.assertEquals("performance", planUltra.resolvedGovernor);
        Assert.assertEquals(2_208_000L, planUltra.resolvedMinFreq);
        Assert.assertEquals(2_208_000L, planUltra.resolvedMaxFreq);
    }

    @Test
    public void testSafeFrequencyWriteOrderingLogic() throws Exception {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "ror_freq_test_" + System.currentTimeMillis());
        tempDir.mkdirs();
        File minFile = new File(tempDir, "scaling_min_freq");
        File maxFile = new File(tempDir, "scaling_max_freq");

        try {
            // Initial state: Balanced (min 1300000, max 2208000)
            try (FileWriter fw = new FileWriter(minFile)) { fw.write("1300000\n"); }
            try (FileWriter fw = new FileWriter(maxFile)) { fw.write("2208000\n"); }

            // Switch to Battery Saver (target min 300000, target max 748800)
            // targetMax (748800) < curMin (1300000): must lower min first!
            boolean okLower = com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine
                    .applyFrequencyLimits(tempDir.getAbsolutePath(), 300_000L, 748_800L, null);
            Assert.assertTrue(okLower);

            String readMin = NativeKernelAccess.readString(minFile.getAbsolutePath());
            String readMax = NativeKernelAccess.readString(maxFile.getAbsolutePath());
            Assert.assertEquals("300000", readMin);
            Assert.assertEquals("748800", readMax);

            // Switch from Battery Saver to Ultra Gaming (target min 2208000, target max 2208000)
            // targetMax (2208000) >= curMin (300000): must raise max first!
            boolean okRaise = com.revror.kernelmanager.utils.kernel.compat.CpuFrequencyProfileEngine
                    .applyFrequencyLimits(tempDir.getAbsolutePath(), 2_208_000L, 2_208_000L, null);
            Assert.assertTrue(okRaise);

            readMin = NativeKernelAccess.readString(minFile.getAbsolutePath());
            readMax = NativeKernelAccess.readString(maxFile.getAbsolutePath());
            Assert.assertEquals("2208000", readMin);
            Assert.assertEquals("2208000", readMax);

        } finally {
            minFile.delete();
            maxFile.delete();
            tempDir.delete();
        }
    }

    @Test
    public void testSecuritySha256AndRemoteConfigValidation() {
        byte[] data = "ROR_SECURE_PAYLOAD_TEST".getBytes();
        String sha = RorSecurityManager.calculateSha256(data);
        Assert.assertNotNull(sha);
        Assert.assertEquals("SHA-256 string length must be 64 characters", 64, sha.length());

        // Valid JSON config
        String validJson = "{\"version\": 514, \"channel\": \"stable\", \"features\": [\"native_kernel_control\"]}";
        Assert.assertTrue("Valid config must be considered safe", RorSecurityManager.isSafeRemoteConfig(validJson));

        // Malicious shell injection attempt
        String maliciousJson = "{\"version\": 514, \"cmd\": \"/system/bin/sh -c 'rm -rf /data'\"}";
        Assert.assertFalse("Unsafe JSON containing shell injection must be rejected", RorSecurityManager.isSafeRemoteConfig(maliciousJson));
    }
}

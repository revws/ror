package com.revror.kernelmanager;

import com.revror.kernelmanager.utils.display.RefreshRateManager;
import com.revror.kernelmanager.utils.doze.ForceDozeManager;
import com.revror.kernelmanager.utils.kernel.cpu.CpuTopologyDetector;
import com.revror.kernelmanager.utils.profile.CustomProfileManager;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class UniversalFeatureUpgradeTest {

    @Test
    public void testCustomProfileSerialization() throws Exception {
        CustomProfileManager.CustomProfile p = new CustomProfileManager.CustomProfile();
        p.name = "Ultimate Apex Gaming";
        p.iconResName = "ic_game";
        p.accentColor = "#FF5252";
        p.swappiness = 80;
        p.vfsCachePressure = 120;
        p.dropCaches = true;
        p.compactMemory = true;

        p.policySettings.put(0, new CustomProfileManager.PolicySetting(0, 300000, 1800000, "schedutil"));
        p.policySettings.put(4, new CustomProfileManager.PolicySetting(4, 710000, 2420000, "schedutil"));
        p.policySettings.put(7, new CustomProfileManager.PolicySetting(7, 844000, 3200000, "schedutil"));

        p.coreOnlineMap.put(0, true);
        p.coreOnlineMap.put(1, true);
        p.coreOnlineMap.put(2, true);
        p.coreOnlineMap.put(3, true);
        p.coreOnlineMap.put(4, true);
        p.coreOnlineMap.put(5, true);
        p.coreOnlineMap.put(6, true);
        p.coreOnlineMap.put(7, true);

        JSONObject json = p.toJson();
        Assert.assertNotNull(json);

        CustomProfileManager.CustomProfile restored = CustomProfileManager.CustomProfile.fromJson(json);
        Assert.assertNotNull(restored);
        Assert.assertEquals("Ultimate Apex Gaming", restored.name);
        Assert.assertEquals("ic_game", restored.iconResName);
        Assert.assertEquals("#FF5252", restored.accentColor);
        Assert.assertEquals(80, restored.swappiness);
        Assert.assertEquals(120, restored.vfsCachePressure);
        Assert.assertTrue(restored.dropCaches);
        Assert.assertTrue(restored.compactMemory);
        Assert.assertEquals(3, restored.policySettings.size());
        Assert.assertEquals(8, restored.coreOnlineMap.size());
        Assert.assertEquals(3200000L, restored.policySettings.get(7).maxFreqKhz);
        Assert.assertEquals("schedutil", restored.policySettings.get(7).governor);
    }

    @Test
    public void testForceDozeModesAndReport() {
        Assert.assertEquals("Battery Mode", ForceDozeManager.DozeMode.BATTERY.getTitle());
        Assert.assertEquals("Balance Mode", ForceDozeManager.DozeMode.BALANCE.getTitle());
        Assert.assertEquals("Gaming Mode", ForceDozeManager.DozeMode.GAMING.getTitle());

        List<String> protectedApps = Arrays.asList("com.revror.revws", "com.android.systemui", "com.mobile.legends");
        List<String> stoppedApps = Arrays.asList("com.some.bloatware", "com.idle.app");

        ForceDozeManager.DozeReport report = new ForceDozeManager.DozeReport(
                ForceDozeManager.DozeMode.GAMING,
                5,
                protectedApps.size(),
                stoppedApps.size(),
                0,
                protectedApps,
                stoppedApps,
                "Gaming Mode applied"
        );

        Assert.assertEquals(ForceDozeManager.DozeMode.GAMING, report.mode);
        Assert.assertEquals(5, report.scannedCount);
        Assert.assertEquals(3, report.protectedCount);
        Assert.assertEquals(2, report.stoppedCount);
        Assert.assertEquals(0, report.failedCount);
        Assert.assertTrue(report.protectedApps.contains("com.mobile.legends"));
    }

    @Test
    public void testCpuTopologyDataStructures() {
        CpuTopologyDetector.CpuCore core0 = new CpuTopologyDetector.CpuCore(0, true, false, 0, 1200000);
        Assert.assertEquals(0, core0.coreId);
        Assert.assertTrue(core0.isOnline);
        Assert.assertFalse(core0.canHotplug); // CPU0 is boot locked

        // Verify setCpuOnline rejects core 0
        Assert.assertFalse(CpuTopologyDetector.setCpuOnline(0, false));

        CpuTopologyDetector.CpuPolicy pol0 = new CpuTopologyDetector.CpuPolicy(
                0,
                "/sys/devices/system/cpu/cpufreq/policy0",
                Arrays.asList(0, 1, 2, 3),
                1200000,
                300000,
                1800000,
                300000,
                1800000,
                "schedutil",
                Arrays.asList("schedutil", "performance"),
                Arrays.asList(300000L, 600000L, 1200000L, 1800000L),
                "LITTLE Cluster"
        );

        Assert.assertEquals(0, pol0.policyId);
        Assert.assertEquals("LITTLE Cluster", pol0.clusterLabel);
        Assert.assertEquals(4, pol0.affectedCpus.size());
        Assert.assertEquals(4, pol0.availableFrequenciesKhz.size());

        CpuTopologyDetector.DeviceCpuTopology topo = new CpuTopologyDetector.DeviceCpuTopology(
                Collections.singletonList(core0),
                Collections.singletonList(pol0),
                1,
                "1 Core: 1 LITTLE (0)"
        );
        Assert.assertEquals(1, topo.totalCores);
        Assert.assertEquals("1 Core: 1 LITTLE (0)", topo.summary);
    }

    @Test
    public void testRefreshRateStatusModel() {
        RefreshRateManager.RefreshRateStatus status = new RefreshRateManager.RefreshRateStatus(
                120.0f, 120.0f, 120.0f, false, true, "Locked at 120 Hz"
        );
        Assert.assertEquals(120.0f, status.currentHz, 0.01f);
        Assert.assertTrue(status.isVerified);
        Assert.assertFalse(status.isAdaptive1Hz);

        RefreshRateManager.RefreshRateStatus adaptiveStatus = new RefreshRateManager.RefreshRateStatus(
                120.0f, 120.0f, 1.0f, true, true, "1 Hz Adaptive active"
        );
        Assert.assertTrue(adaptiveStatus.isAdaptive1Hz);
        Assert.assertEquals(1.0f, adaptiveStatus.minHz, 0.01f);
    }
}

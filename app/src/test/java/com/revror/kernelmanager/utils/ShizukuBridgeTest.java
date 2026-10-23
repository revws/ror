/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

public class ShizukuBridgeTest {

    @Test
    public void testResultCreation() {
        ShizukuBridge.Result okResult = new ShizukuBridge.Result(true, "uid=2000(shell) gid=2000(shell)", 0);
        Assert.assertTrue(okResult.success);
        Assert.assertEquals(0, okResult.exitCode);
        Assert.assertEquals("uid=2000(shell) gid=2000(shell)", okResult.output);

        ShizukuBridge.Result failResult = new ShizukuBridge.Result(false, "Permission denied", 1);
        Assert.assertFalse(failResult.success);
        Assert.assertEquals(1, failResult.exitCode);
        Assert.assertEquals("Permission denied", failResult.output);
    }

    @Test
    public void testGracefulFallbackWhenServiceUnavailable() {
        // In local JVM test runner, Shizuku binder is absent
        boolean available = ShizukuBridge.available();
        Assert.assertFalse("Binder should safely report false on desktop JVM", available);

        boolean granted = ShizukuBridge.granted();
        Assert.assertFalse("Granted should be false when service is unavailable", granted);

        String summary = ShizukuBridge.getStatusSummary();
        Assert.assertNotNull(summary);
        Assert.assertTrue(summary.contains("Disconnected") || summary.contains("Not Running"));

        ShizukuBridge.Result runRes = ShizukuBridge.run("id");
        Assert.assertNotNull(runRes);
        Assert.assertFalse(runRes.success);
        Assert.assertTrue(runRes.output.contains("not available"));
    }

    @Test
    public void testConnectionListenerLifecycle() {
        final AtomicBoolean disconnectedCalled = new AtomicBoolean(false);
        ShizukuBridge.OnConnectionListener listener = new ShizukuBridge.OnConnectionListener() {
            @Override
            public void onShizukuConnected(boolean hasPermission) {
            }

            @Override
            public void onShizukuDisconnected() {
                disconnectedCalled.set(true);
            }
        };

        ShizukuBridge.addConnectionListener(listener);
        Assert.assertTrue("Sticky listener should immediately receive disconnected event when service offline",
                disconnectedCalled.get());

        // Safe removal
        ShizukuBridge.removeConnectionListener(listener);
    }

    @Test
    public void testHardwareDetectorAccessLevelIntegration() {
        com.revror.kernelmanager.utils.kernel.compat.UniversalHardwareDetector.AccessLevel level =
                com.revror.kernelmanager.utils.kernel.compat.UniversalHardwareDetector.AccessLevel.SHIZUKU_PRIVILEGED;
        Assert.assertEquals("SHIZUKU_PRIVILEGED", level.name());
    }
}

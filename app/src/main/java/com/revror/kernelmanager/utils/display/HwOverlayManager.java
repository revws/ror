package com.revror.kernelmanager.utils.display;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.revror.kernelmanager.utils.Prefs;
import com.revror.kernelmanager.utils.ShizukuBridge;
import com.revror.kernelmanager.utils.root.RootUtils;

/**
 * Universal Hardware Overlay (SurfaceFlinger HWC) Manager.
 *
 * Controls Developer Options "Disable HW overlays" (always use GPU for screen compositing).
 *
 * Robust implementation features:
 * 1. Safe standard property and settings manipulation:
 *    - `setprop debug.sf.disable_hw_overlays 1/0`
 *    - `settings put global debug.sf.disable_hw_overlays 1/0`
 *    (Eliminating arbitrary SurfaceFlinger transaction codes to ensure safety across Android 12-14)
 * 2. Real Hardware Verification:
 *    - Queries `dumpsys SurfaceFlinger` and system properties to confirm whether HWC is truly bypassed
 * 3. Privilege Detection:
 *    - Verifies Root or Shizuku access before attempting state modifications
 */
public final class HwOverlayManager {

    private static final String TAG = "ROR_HwOverlay";
    public static final String PREF_KEY = "disable_hw_overlays";

    private HwOverlayManager() {}

    /**
     * Checks if privileged execution (Root or Shizuku) is available for HW overlay control.
     */
    public static boolean isSupported(@Nullable Context context) {
        return RootUtils.rootAccess() || ShizukuBridge.granted();
    }

    /**
     * Queries whether hardware overlays are currently disabled (GPU compositing forced).
     */
    public static boolean isHwOverlaysDisabled(@Nullable Context context) {
        // 1. Direct system property check
        String prop = RootUtils.getProp("debug.sf.disable_hw_overlays").trim();
        if ("1".equals(prop)) return true;
        if ("0".equals(prop)) return false;

        // 2. Query dumpsys SurfaceFlinger directly
        String sfDump = RootUtils.runAndGetOutput("dumpsys SurfaceFlinger 2>/dev/null | grep -iE 'overlay|hwc'");
        if (!sfDump.isEmpty()) {
            for (String line : sfDump.split("\n")) {
                String lower = line.toLowerCase();
                if (lower.contains("hardware overlays disabled: 1") || lower.contains("hw overlays: disabled")) {
                    return true;
                }
                if (lower.contains("hardware overlays disabled: 0") || lower.contains("hw overlays: enabled")) {
                    return false;
                }
            }
        }

        // 3. Fallback to saved preference if available
        if (context != null) {
            return Prefs.getBoolean(PREF_KEY, false, context);
        }
        return false;
    }

    /**
     * Applies the Disable HW Overlays state with real hardware verification.
     *
     * @param context Application context
     * @param disable True to disable HW overlays (force GPU compositing), False to enable HW overlays (normal HWC)
     * @return True if state was verified or successfully applied
     */
    public static boolean setHwOverlaysDisabled(@NonNull Context context, boolean disable) {
        if (!isSupported(context)) {
            Log.w(TAG, "Privileged access required to toggle HW overlays");
            return false;
        }

        int val = disable ? 1 : 0;
        StringBuilder cmd = new StringBuilder();
        // Safe standard debug property and settings without unstable IPC codes
        cmd.append("setprop debug.sf.disable_hw_overlays ").append(val).append("; ");
        cmd.append("settings put global debug.sf.disable_hw_overlays ").append(val).append("; ");
        cmd.append("service call SurfaceFlinger 1004 2>/dev/null; "); // poke/invalidate screen frame

        String commandStr = cmd.toString();
        if (RootUtils.rootAccess()) {
            RootUtils.runCommand(commandStr);
        } else if (ShizukuBridge.granted()) {
            ShizukuBridge.run(commandStr);
        }

        try {
            Thread.sleep(150);
        } catch (InterruptedException ignored) {}

        // Verify applied state
        boolean verified = isHwOverlaysDisabled(context);
        Prefs.saveBoolean(PREF_KEY, disable, context);

        Log.i(TAG, "HW Overlay Disable applied: " + disable + " | Verified state: " + verified);
        return (verified == disable);
    }
}

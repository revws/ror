package com.revror.kernelmanager.utils;

import android.content.Context;
import androidx.annotation.NonNull;

import com.revror.kernelmanager.utils.kernel.compat.NativeProfileExecutor;

import java.io.File;

/**
 * Legacy compatibility wrapper for ProfileWorkerRunner.
 * Completely routes execution to NativeProfileExecutor, eliminating background
 * shell workers and script execution.
 */
public final class ProfileWorkerRunner {
    private ProfileWorkerRunner() {}

    public static void start(@NonNull Context context, File payload, boolean binary, String properties) throws Exception {
        int appProf = 0;
        try {
            String pStr = com.revror.kernelmanager.utils.root.RootUtils.getProp("persist.revror.app.profile");
            if (!pStr.isEmpty()) appProf = Integer.parseInt(pStr.trim());
        } catch (Exception ignored) {}

        boolean isPhoenix = payload != null && payload.getName().contains("codex");
        NativeProfileExecutor.executeProfile(context, appProf, isPhoenix);
    }
}

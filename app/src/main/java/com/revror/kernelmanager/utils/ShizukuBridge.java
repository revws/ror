/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.utils;

import android.content.pm.PackageManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import rikka.shizuku.Shizuku;

public final class ShizukuBridge {
    private ShizukuBridge() {}

    public static boolean available() {
        try { return Shizuku.pingBinder(); } catch (Throwable ignored) { return false; }
    }

    public static boolean granted() {
        try { return available() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED; }
        catch (Throwable ignored) { return false; }
    }

    public static void request(int requestCode) {
        if (available()) Shizuku.requestPermission(requestCode);
    }

    public static Result run(String command) {
        if (!granted()) return new Result(false, "Shizuku permission is not available");
        Process process = null;
        try {
            // API 13 keeps the legacy remote-process bridge for compatibility but no longer
            // exposes it publicly. Centralizing the reflective call here makes failure explicit.
            Method method = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
            method.setAccessible(true);
            process = (Process)method.invoke(null, new String[]{"sh", "-c", command}, null, null);
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line; while ((line = reader.readLine()) != null && output.length() < 32768) output.append(line).append('\n');
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line; while ((line = reader.readLine()) != null && output.length() < 32768) output.append(line).append('\n');
            }
            int exit = process.waitFor();
            return new Result(exit == 0, output.toString().trim());
        } catch (Throwable error) {
            return new Result(false, error.getClass().getSimpleName() + ": " + error.getMessage());
        } finally { if (process != null) process.destroy(); }
    }

    public static final class Result {
        public final boolean success; public final String output;
        Result(boolean success, String output) { this.success = success; this.output = output == null ? "" : output; }
    }
}

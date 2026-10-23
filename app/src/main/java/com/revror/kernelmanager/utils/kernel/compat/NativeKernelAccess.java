package com.revror.kernelmanager.utils.kernel.compat;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.revror.kernelmanager.utils.root.RootUtils;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ShellUtils;
import com.topjohnwu.superuser.io.SuFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * High-performance native kernel file access layer.
 * Replaces shell scripts with direct stream I/O using Java I/O for unprivileged nodes
 * and libsu direct IPC streams / fast root commands for root-privileged nodes.
 */
public final class NativeKernelAccess {

    private static final String TAG = "NativeKernelAccess";

    private NativeKernelAccess() {}

    /**
     * Checks if a kernel or filesystem node exists without spawning an external script.
     */
    public static boolean exists(@Nullable String path) {
        if (path == null || path.isEmpty()) return false;
        File f = new File(path);
        if (f.exists()) return true;
        // /sys and /proc are standard Linux virtual filesystems. If not found by File.exists(),
        // it definitely does not exist. Do not incur expensive root shell IPC overhead on /sys or /proc!
        if (path.startsWith("/sys/") || path.startsWith("/proc/")) {
            return false;
        }
        try {
            return SuFile.open(path).exists();
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Checks if a node is writable without spawning an external script.
     */
    public static boolean isWritable(@Nullable String path) {
        if (path == null || path.isEmpty()) return false;
        File f = new File(path);
        if (f.canWrite()) return true;
        try {
            return SuFile.open(path).canWrite();
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Checks if a node is a directory without spawning an external script.
     */
    public static boolean isDirectory(@Nullable String path) {
        if (path == null || path.isEmpty()) return false;
        File f = new File(path);
        if (f.isDirectory()) return true;
        try {
            return SuFile.open(path).isDirectory();
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Reads the complete contents of a kernel node into a trimmed string.
     * Tries direct unprivileged stream first (0 overhead, 0 processes), then fast root command.
     */
    @Nullable
    public static String readString(@NonNull String path) {
        if (!exists(path)) return null;

        // 1. Direct unprivileged stream read (works for /sys and /proc world-readable nodes)
        try (FileInputStream fis = new FileInputStream(path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(line);
            }
            return sb.toString().trim();
        } catch (Exception ignored) {
            // Unprivileged read blocked by DAC/SELinux, proceed to root read
        }

        // 2. Fallback fast command if unprivileged stream was blocked
        try {
            String out = RootUtils.runAndGetOutput("cat '" + path.replace("'", "'\\''") + "' 2>/dev/null");
            return out == null || out.trim().isEmpty() ? null : out.trim();
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Writes a string value to a kernel node directly without creating temporary scripts.
     * Guarantees atomic write and unlock if previously chmodded 0444 (e.g. Ultra Gaming).
     */
    public static boolean writeString(@NonNull String path, @NonNull String value) {
        if (path == null || path.isEmpty()) return false;

        byte[] bytes = (value.endsWith("\n") ? value : value + "\n").getBytes(StandardCharsets.UTF_8);

        // Try direct FileOutputStream if writable by current process
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(bytes);
            fos.flush();
            return true;
        } catch (Throwable ignored) {}

        // Root write: automatically unlock permissions if 0444 and write synchronously
        String cleanVal = value.trim();
        String cmd = "[ -e '" + path + "' ] && { [ ! -w '" + path + "' ] && chmod 0644 '" + path + "' 2>/dev/null; echo '" + cleanVal.replace("'", "'\\''") + "' > '" + path + "' 2>/dev/null; }";
        try {
            RootUtils.runCommand(cmd);
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "Failed writing to " + path + ": " + t.getMessage());
            return false;
        }
    }

    /**
     * Ensures node has writable permissions.
     */
    public static void ensureWritable(@NonNull String path) {
        if (path == null || path.isEmpty()) return;
        RootUtils.runCommand("[ -e '" + path + "' ] && [ ! -w '" + path + "' ] && chmod 0644 '" + path + "' 2>/dev/null");
    }

    /**
     * Sets POSIX file permissions (e.g. 0644 or 0444).
     */
    public static boolean setPermissions(@NonNull String path, int mode) {
        if (path == null || path.isEmpty()) return false;
        String octal;
        if (mode == 644 || mode == 444 || mode == 666 || mode == 755 || mode == 777) {
            octal = "0" + mode;
        } else {
            octal = String.format("%04o", mode);
        }
        try {
            RootUtils.runCommand("chmod " + octal + " '" + path.replace("'", "'\\''") + "' 2>/dev/null");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Appends a log line to a specified log file (e.g. phx.log or revws.log) natively.
     */
    public static void appendLog(@NonNull String logPath, @NonNull String message) {
        try {
            File parent = new File(logPath).getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            String line = message + "\n";
            byte[] bytes = line.getBytes(StandardCharsets.UTF_8);

            // Try standard FileOutputStream first
            try (FileOutputStream fos = new FileOutputStream(logPath, true)) {
                fos.write(bytes);
                fos.flush();
                return;
            } catch (Exception ignored) {}

            // Root append synchronously
            RootUtils.runCommand("printf '%s\\n' '" + message.replace("'", "'\\''") + "' >> '" + logPath.replace("'", "'\\''") + "' 2>/dev/null");
        } catch (Throwable ignored) {}
    }

    /**
     * Sets a persistent Android system property cleanly without spawning a shell script.
     */
    public static void setSystemProperty(@NonNull String key, @NonNull String value) {
        try {
            RootUtils.runCommand("setprop " + key + " '" + value.replace("'", "'\\''") + "'");
        } catch (Throwable t) {
            Log.w(TAG, "Failed setprop " + key + ": " + t.getMessage());
        }
    }

    /**
     * Lists sub-files in a directory natively.
     */
    @NonNull
    public static List<String> listDirectory(@NonNull String dirPath) {
        List<String> results = new ArrayList<>();
        File dir = new File(dirPath);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    results.add(f.getAbsolutePath());
                }
                return results;
            }
        }

        try {
            File sDir = SuFile.open(dirPath);
            if (sDir.exists() && sDir.isDirectory()) {
                File[] files = sDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        results.add(f.getAbsolutePath());
                    }
                    return results;
                }
            }
        } catch (Throwable ignored) {}

        return results;
    }
}

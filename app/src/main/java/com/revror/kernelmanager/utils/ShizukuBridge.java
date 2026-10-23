/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.utils;

import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import rikka.shizuku.Shizuku;

/**
 * Enterprise-grade Shizuku connectivity, lifecycle supervision, and remote shell bridge.
 * Features:
 * - Thread-safe reflective binding to Shizuku IPC service
 * - Sticky binder event listeners with reactive callbacks
 * - Bidirectional output streaming with pipe deadlock prevention
 * - Configurable execution timeouts preventing hung operations
 * - Diagnostic telemetry (UID, SELinux domain, version, binder state)
 */
public final class ShizukuBridge {

    private static final String TAG = "ShizukuBridge";
    private static final int DEFAULT_TIMEOUT_SECONDS = 15;

    public interface OnConnectionListener {
        void onShizukuConnected(boolean hasPermission);
        void onShizukuDisconnected();
    }

    private static final List<OnConnectionListener> sListeners = new CopyOnWriteArrayList<>();
    private static final AtomicBoolean sInitialized = new AtomicBoolean(false);
    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());

    private static volatile Method sNewProcessMethod = null;
    private static volatile boolean sMethodLookupFailed = false;

    private static final Shizuku.OnBinderReceivedListener sBinderReceivedListener = () -> {
        Log.i(TAG, "Shizuku binder received");
        dispatchConnectionEvent(true);
    };

    private static final Shizuku.OnBinderDeadListener sBinderDeadListener = () -> {
        Log.w(TAG, "Shizuku binder died");
        dispatchConnectionEvent(false);
    };

    private static final Shizuku.OnRequestPermissionResultListener sPermissionListener = (requestCode, grantResult) -> {
        boolean granted = grantResult == PackageManager.PERMISSION_GRANTED;
        Log.i(TAG, "Shizuku permission result for request " + requestCode + ": " + (granted ? "GRANTED" : "DENIED"));
        dispatchConnectionEvent(pingBinderSafe());
    };

    static {
        ensureInitialized();
    }

    private ShizukuBridge() {}

    /**
     * Initializes sticky lifecycle listeners. Safe to call multiple times.
     */
    public static void ensureInitialized() {
        if (sInitialized.compareAndSet(false, true)) {
            try {
                Shizuku.addBinderReceivedListenerSticky(sBinderReceivedListener);
                Shizuku.addBinderDeadListener(sBinderDeadListener);
                Shizuku.addRequestPermissionResultListener(sPermissionListener);
                Log.d(TAG, "Shizuku lifecycle listeners successfully registered");
            } catch (Throwable t) {
                Log.w(TAG, "Failed to register Shizuku lifecycle listeners: " + t.getMessage());
            }
        }
    }

    public static void addConnectionListener(@NonNull OnConnectionListener listener) {
        ensureInitialized();
        if (!sListeners.contains(listener)) {
            sListeners.add(listener);
        }
        // Sticky trigger: immediately notify current state
        boolean alive = available();
        if (alive) {
            listener.onShizukuConnected(granted());
        } else {
            listener.onShizukuDisconnected();
        }
    }

    public static void removeConnectionListener(@NonNull OnConnectionListener listener) {
        sListeners.remove(listener);
    }

    private static void dispatchConnectionEvent(boolean alive) {
        boolean hasPerm = alive && granted();
        sMainHandler.post(() -> {
            for (OnConnectionListener l : sListeners) {
                try {
                    if (alive) {
                        l.onShizukuConnected(hasPerm);
                    } else {
                        l.onShizukuDisconnected();
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "Error in connection listener callback", t);
                }
            }
        });
    }

    private static boolean pingBinderSafe() {
        try {
            return Shizuku.pingBinder();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Returns true if Shizuku binder is currently alive and responsive.
     */
    public static boolean available() {
        return pingBinderSafe();
    }

    /**
     * Returns true if Shizuku binder is alive AND permission has been granted to this application.
     */
    public static boolean granted() {
        try {
            return available() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Requests Shizuku authorization.
     */
    public static void request(int requestCode) {
        ensureInitialized();
        if (available()) {
            try {
                Shizuku.requestPermission(requestCode);
            } catch (Throwable t) {
                Log.e(TAG, "Failed to request Shizuku permission", t);
            }
        }
    }

    /**
     * Returns true if request permission rationale should be displayed.
     */
    public static boolean shouldShowRationale() {
        try {
            return available() && Shizuku.shouldShowRequestPermissionRationale();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Retrieves the UID under which the Shizuku server operates (typically 2000 for shell, 0 for root).
     */
    public static int getUid() {
        try {
            return available() ? Shizuku.getUid() : -1;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    /**
     * Returns true if Shizuku server runs as root (UID 0).
     */
    public static boolean isRootServer() {
        return getUid() == 0;
    }

    /**
     * Retrieves the Shizuku server API version.
     */
    public static int getVersion() {
        try {
            return available() ? Shizuku.getVersion() : -1;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    /**
     * Retrieves the SELinux security context string of the Shizuku process.
     */
    @NonNull
    public static String getSELinuxContext() {
        try {
            if (available()) {
                String ctx = Shizuku.getSELinuxContext();
                return ctx != null ? ctx : "unknown";
            }
        } catch (Throwable ignored) {}
        return "disconnected";
    }

    /**
     * Formats a human-readable summary of Shizuku's connectivity and authorization state.
     */
    @NonNull
    public static String getStatusSummary() {
        if (!available()) {
            return "Shizuku Service Disconnected (Not Running)";
        }
        if (!granted()) {
            return "Shizuku Connected (Permission Required)";
        }
        int uid = getUid();
        String role = (uid == 0) ? "Root (UID 0)" : (uid == 2000) ? "Shell (UID 2000)" : ("UID " + uid);
        return "Connected (" + role + ", API v" + getVersion() + ", " + getSELinuxContext() + ")";
    }

    private static Method getNewProcessMethod() throws NoSuchMethodException {
        if (sNewProcessMethod != null) return sNewProcessMethod;
        if (sMethodLookupFailed) throw new NoSuchMethodException("Shizuku.newProcess method not available");
        synchronized (ShizukuBridge.class) {
            if (sNewProcessMethod == null) {
                try {
                    Method m = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
                    m.setAccessible(true);
                    sNewProcessMethod = m;
                } catch (NoSuchMethodException e) {
                    sMethodLookupFailed = true;
                    throw e;
                }
            }
            return sNewProcessMethod;
        }
    }

    /**
     * Executes a command through the Shizuku remote process bridge with standard timeout.
     */
    public static Result run(String command) {
        return run(command, DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Executes a command through Shizuku with a specified timeout.
     */
    public static Result run(String command, long timeout, TimeUnit unit) {
        if (!granted()) {
            return new Result(false, "Shizuku permission is not available", -1);
        }

        Process process = null;
        try {
            String cmdTrimmed = command != null ? command.trim() : "";
            String wrappedCommand = cmdTrimmed.endsWith(";") ? "{ " + cmdTrimmed + " } 2>&1" : "{ " + cmdTrimmed + "; } 2>&1";
            Method method = getNewProcessMethod();
            process = (Process) method.invoke(null, new String[]{"sh", "-c", wrappedCommand}, null, null);
            if (process == null) {
                return new Result(false, "Failed to instantiate remote process", -1);
            }

            final StringBuilder output = new StringBuilder();
            final Process finalProcess = process;
            final CountDownLatch readLatch = new CountDownLatch(1);

            Thread readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(finalProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null && output.length() < 65536) {
                        if (output.length() > 0) output.append('\n');
                        output.append(line);
                    }
                } catch (Throwable ignored) {
                } finally {
                    readLatch.countDown();
                }
            }, "ror-shizuku-io");
            readerThread.start();

            boolean finished = false;
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                finished = process.waitFor(timeout, unit);
            } else {
                // Pre-Oreo timeout emulation
                long start = System.currentTimeMillis();
                long maxWaitMs = unit.toMillis(timeout);
                while (System.currentTimeMillis() - start < maxWaitMs) {
                    try {
                        process.exitValue();
                        finished = true;
                        break;
                    } catch (IllegalThreadStateException notExited) {
                        Thread.sleep(50);
                    }
                }
            }

            if (!finished) {
                try { process.destroy(); } catch (Throwable ignored) {}
                return new Result(false, "Command timed out after " + timeout + " " + unit.name().toLowerCase(), -1);
            }

            // Await reader stream drain
            readLatch.await(1200, TimeUnit.MILLISECONDS);

            int exitCode = process.exitValue();
            return new Result(exitCode == 0, output.toString().trim(), exitCode);

        } catch (Throwable error) {
            Log.e(TAG, "Shizuku command execution error: " + command, error);
            return new Result(false, error.getClass().getSimpleName() + ": " + error.getMessage(), -1);
        } finally {
            if (process != null) {
                try {
                    process.destroy();
                } catch (Throwable ignored) {}
            }
        }
    }

    public static final class Result {
        public final boolean success;
        public final String output;
        public final int exitCode;

        public Result(boolean success, String output) {
            this(success, output, success ? 0 : 1);
        }

        public Result(boolean success, String output, int exitCode) {
            this.success = success;
            this.output = output == null ? "" : output;
            this.exitCode = exitCode;
        }

        @NonNull
        @Override
        public String toString() {
            return "Result{success=" + success + ", exitCode=" + exitCode + ", output='" + output + "'}";
        }
    }
}

/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.utils;

import android.os.Build;

import com.revror.kernelmanager.utils.root.RootUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads whole-device CPU state. It never substitutes the REVROR process CPU time. */
public final class CpuStatsReader {
    private final java.util.Map<Integer, Long> prevTotalMap = new java.util.HashMap<>();
    private final java.util.Map<Integer, Long> prevIdleMap = new java.util.HashMap<>();
    private long previousTotal = -1;
    private long previousIdle = -1;

    public int readPercent(boolean allowRootFallback) {
        String line = readFirstLine("/proc/stat");
        if ((line == null || !line.startsWith("cpu ")) && allowRootFallback) {
            line = firstLine(RootUtils.runAndGetOutput("head -n 1 /proc/stat 2>/dev/null"));
        }
        int statResult = parseProcStat(line);
        if (statResult >= 0) return statResult;

        String top = null;
        try {
            Process process = new ProcessBuilder("/system/bin/top", "-b", "-n", "1").start();
            try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String current;
                while ((current = reader.readLine()) != null) {
                    if (current.toLowerCase(Locale.US).contains("cpu")) { top = current; break; }
                }
            }
        } catch (Exception ignored) {}
        if ((top == null || top.isEmpty()) && allowRootFallback) {
            top = RootUtils.runAndGetOutput("top -b -n 1 2>/dev/null | grep -m1 -i cpu");
        }
        return parseTop(top);
    }

    public long readAverageCurrentKHz(boolean allowRootFallback) {
        List<Long> values = new ArrayList<>();
        collectFrequencies(new File("/sys/devices/system/cpu/cpufreq"), "scaling_cur_freq", values);
        if (values.isEmpty()) collectCpuFrequencies("scaling_cur_freq", values);
        if (values.isEmpty() && allowRootFallback) {
            return parseAverage(RootUtils.runAndGetOutput(
                    "for f in /sys/devices/system/cpu/cpufreq/policy*/scaling_cur_freq "
                            + "/sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq; do "
                            + "[ -r \"$f\" ] && cat \"$f\"; done"));
        }
        return average(values);
    }

    public long readMaxKHz(boolean allowRootFallback) {
        List<Long> values = new ArrayList<>();
        collectFrequencies(new File("/sys/devices/system/cpu/cpufreq"), "cpuinfo_max_freq", values);
        if (values.isEmpty()) collectCpuFrequencies("cpuinfo_max_freq", values);
        long result = 0;
        for (long value : values) result = Math.max(result, value);
        if (result <= 0 && allowRootFallback) {
            String output = RootUtils.runAndGetOutput(
                    "for f in /sys/devices/system/cpu/cpufreq/policy*/cpuinfo_max_freq "
                            + "/sys/devices/system/cpu/cpu*/cpufreq/cpuinfo_max_freq; do "
                            + "[ -r \"$f\" ] && cat \"$f\"; done");
            for (String value : output.split("\\s+")) {
                try { result = Math.max(result, Long.parseLong(value)); } catch (Exception ignored) {}
            }
        }
        return result;
    }

    public String deviceDescription(boolean allowRootFallback) {
        String soc = "";
        if (Build.VERSION.SDK_INT >= 31) {
            soc = join(Build.SOC_MANUFACTURER, Build.SOC_MODEL);
        }
        if (soc.isEmpty() && allowRootFallback) {
            soc = RootUtils.runAndGetOutput("getprop ro.soc.manufacturer; getprop ro.soc.model").trim()
                    .replace('\n', ' ');
        }
        if (soc.isEmpty()) soc = Build.HARDWARE;
        long max = readMaxKHz(allowRootFallback);
        String maxText = max > 0 ? String.format(Locale.US, "%.2f GHz max", max / 1_000_000d)
                : "maximum frequency unavailable";
        return soc + "\n" + Runtime.getRuntime().availableProcessors() + " cores • " + maxText
                + "\nABI: " + String.join(", ", Build.SUPPORTED_ABIS);
    }

    private int parseProcStat(String line) {
        // existing method for overall CPU usage
        if (line == null || !line.startsWith("cpu ")) return -1;
        try {
            String[] values = line.trim().split("\\s+");
            if (values.length < 5) return -1;
            long total = 0;
            for (int i = 1; i < values.length; i++) total += Long.parseLong(values[i]);
            long idle = Long.parseLong(values[4]);
            if (values.length > 5) idle += Long.parseLong(values[5]);
            int result = -1;
            if (previousTotal >= 0 && total > previousTotal) {
                long deltaTotal = total - previousTotal;
                result = (int) Math.round((deltaTotal - (idle - previousIdle)) * 100d / deltaTotal);
            }
            previousTotal = total;
            previousIdle = idle;
            return clamp(result);
        } catch (Exception ignored) { return -1; }
    }

    private int parseTop(String line) {
        if (line == null) return -1;
        String normalized = line.toLowerCase(Locale.US);
        Matcher idleMatcher = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)%?\\s*(?:idle|id)\\b").matcher(normalized);
        if (!idleMatcher.find()) return -1;
        double idle = Double.parseDouble(idleMatcher.group(1));
        Matcher totalMatcher = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)%cpu").matcher(normalized);
        if (totalMatcher.find()) {
            double total = Double.parseDouble(totalMatcher.group(1));
            if (total > 0) return clamp((int) Math.round((total - idle) * 100d / total));
        }
        return clamp((int) Math.round(100d - idle));
    }

    private void collectFrequencies(File parent, String name, List<Long> output) {
        File[] policies = parent.listFiles();
        if (policies == null) return;
        for (File policy : policies) {
            long value = readLong(new File(policy, name));
            if (value > 0) output.add(value);
        }
    }

    private void collectCpuFrequencies(String name, List<Long> output) {
        File[] cpus = new File("/sys/devices/system/cpu").listFiles(
                file -> file.isDirectory() && file.getName().matches("cpu[0-9]+"));
        if (cpus == null) return;
        for (File cpu : cpus) {
            long value = readLong(new File(new File(cpu, "cpufreq"), name));
            if (value > 0 && !output.contains(value)) output.add(value);
        }
    }

    private long readLong(File file) {
        String value = readFirstLine(file.getAbsolutePath());
        try { return value == null ? -1 : Long.parseLong(value.trim()); }
        catch (Exception ignored) { return -1; }
    }

    private String readFirstLine(String path) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) { return reader.readLine(); }
        catch (Exception ignored) { return null; }
    }

    private String firstLine(String value) {
        if (value == null) return null;
        int newline = value.indexOf('\n');
        return newline < 0 ? value : value.substring(0, newline);
    }

    private long parseAverage(String output) {
        List<Long> values = new ArrayList<>();
        if (output != null) for (String value : output.split("\\s+")) {
            try { long parsed = Long.parseLong(value); if (parsed > 0) values.add(parsed); }
            catch (Exception ignored) {}
        }
        return average(values);
    }

    private long average(List<Long> values) {
        if (values.isEmpty()) return -1;
        long total = 0;
        for (long value : values) total += value;
        return total / values.size();
    }

    private int clamp(int value) { return value < 0 ? -1 : Math.min(100, value); }

    private String join(String first, String second) {
        String a = first == null ? "" : first.trim();
        String b = second == null ? "" : second.trim();
        return (a + " " + b).trim();
    }

    /** Reads per‑core CPU usage percentages. */
    public java.util.List<Integer> readCorePercents(boolean allowRootFallback) {
        java.util.List<Integer> result = new java.util.ArrayList<>();
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader("/proc/stat"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("cpu")) continue;
                if (line.startsWith("cpu ")) continue; // skip aggregate line
                String[] parts = line.trim().split("\\s+");
                int coreId;
                try { coreId = Integer.parseInt(parts[0].substring(3)); } catch (Exception e) { continue; }
                long user = parseLongSafe(parts, 1);
                long nice = parseLongSafe(parts, 2);
                long system = parseLongSafe(parts, 3);
                long idle = parseLongSafe(parts, 4);
                long iowait = parts.length > 5 ? parseLongSafe(parts, 5) : 0;
                long total = user + nice + system + idle + iowait;
                long prevTotal = prevTotalMap.getOrDefault(coreId, -1L);
                long prevIdle = prevIdleMap.getOrDefault(coreId, -1L);
                if (prevTotal >= 0 && total > prevTotal) {
                    long deltaTotal = total - prevTotal;
                    long deltaIdle = idle - prevIdle;
                    int usage = (int) Math.round((deltaTotal - deltaIdle) * 100d / deltaTotal);
                    result.add(clamp(usage));
                } else {
                    result.add(-1);
                }
                prevTotalMap.put(coreId, total);
                prevIdleMap.put(coreId, idle);
            }
        } catch (Exception ignored) {}
        return result;
    }

    private long parseLongSafe(String[] arr, int idx) {
        try { return Long.parseLong(arr[idx]); } catch (Exception e) { return 0L; }
    }
}

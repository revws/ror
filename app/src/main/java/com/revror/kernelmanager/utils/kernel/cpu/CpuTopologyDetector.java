package com.revror.kernelmanager.utils.kernel.cpu;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.revror.kernelmanager.utils.kernel.compat.NativeKernelAccess;
import com.revror.kernelmanager.utils.root.RootUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Universal Dynamic CPU Core and Policy Topology Detector.
 *
 * Capabilities:
 * 1. Dynamically detects ALL CPU cores (e.g. 4, 8, 10+ cores) across any Android device.
 * 2. Identifies writable CPU hotplug nodes for safe per-core online/offline control (CPU0 is protected).
 * 3. Discovers all CPU frequency policies and maps cores to their shared hardware policies.
 * 4. Labels clusters accurately (LITTLE, BIG, PRIME) based on actual detected hardware peak frequencies.
 * 5. Provides safe frequency and governor setters with strict safe write ordering.
 */
public final class CpuTopologyDetector {

    private static final String TAG = "CpuTopologyDetector";

    public static final class CpuCore {
        public final int coreId;
        public final boolean isOnline;
        public final boolean canHotplug;
        public final int policyId;
        public final long curFreqKhz;

        public CpuCore(int coreId, boolean isOnline, boolean canHotplug, int policyId, long curFreqKhz) {
            this.coreId = coreId;
            this.isOnline = isOnline;
            this.canHotplug = canHotplug;
            this.policyId = policyId;
            this.curFreqKhz = curFreqKhz;
        }
    }

    public static final class CpuPolicy {
        public final int policyId;
        public final String path;
        public final List<Integer> affectedCpus;
        public final long curFreqKhz;
        public final long minFreqKhz;
        public final long maxFreqKhz;
        public final long hwMinFreqKhz;
        public final long hwMaxFreqKhz;
        public final String currentGovernor;
        public final List<String> availableGovernors;
        public final List<Long> availableFrequenciesKhz;
        public final String clusterLabel;

        public CpuPolicy(int policyId, String path, List<Integer> affectedCpus,
                         long curFreqKhz, long minFreqKhz, long maxFreqKhz,
                         long hwMinFreqKhz, long hwMaxFreqKhz, String currentGovernor,
                         List<String> availableGovernors, List<Long> availableFrequenciesKhz,
                         String clusterLabel) {
            this.policyId = policyId;
            this.path = path;
            this.affectedCpus = Collections.unmodifiableList(new ArrayList<>(affectedCpus));
            this.curFreqKhz = curFreqKhz;
            this.minFreqKhz = minFreqKhz;
            this.maxFreqKhz = maxFreqKhz;
            this.hwMinFreqKhz = hwMinFreqKhz;
            this.hwMaxFreqKhz = hwMaxFreqKhz;
            this.currentGovernor = currentGovernor;
            this.availableGovernors = Collections.unmodifiableList(new ArrayList<>(availableGovernors));
            this.availableFrequenciesKhz = Collections.unmodifiableList(new ArrayList<>(availableFrequenciesKhz));
            this.clusterLabel = clusterLabel;
        }
    }

    public static final class DeviceCpuTopology {
        public final List<CpuCore> cores;
        public final List<CpuPolicy> policies;
        public final int totalCores;
        public final String summary;

        public DeviceCpuTopology(List<CpuCore> cores, List<CpuPolicy> policies, int totalCores, String summary) {
            this.cores = Collections.unmodifiableList(new ArrayList<>(cores));
            this.policies = Collections.unmodifiableList(new ArrayList<>(policies));
            this.totalCores = totalCores;
            this.summary = summary;
        }
    }

    private CpuTopologyDetector() {}

    /**
     * Dynamically detects the full CPU topology of the device.
     */
    @NonNull
    public static DeviceCpuTopology detect() {
        // 1. Discover all CPU cores
        List<Integer> coreIds = new ArrayList<>();
        File cpuSysDir = new File("/sys/devices/system/cpu");
        if (cpuSysDir.exists() && cpuSysDir.isDirectory()) {
            File[] files = cpuSysDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    String name = f.getName();
                    if (name.startsWith("cpu") && name.length() > 3) {
                        try {
                            int id = Integer.parseInt(name.substring(3));
                            coreIds.add(id);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }

        if (coreIds.isEmpty()) {
            // Fallback via shell
            String rawCores = RootUtils.runAndGetOutput("ls -d /sys/devices/system/cpu/cpu[0-9]* 2>/dev/null").trim();
            for (String line : rawCores.split("\\s+")) {
                if (line.contains("cpu")) {
                    try {
                        String idStr = line.substring(line.lastIndexOf("cpu") + 3);
                        int id = Integer.parseInt(idStr);
                        if (!coreIds.contains(id)) coreIds.add(id);
                    } catch (Exception ignored) {}
                }
            }
        }

        if (coreIds.isEmpty()) {
            // Default minimum fallback
            int coresCount = Runtime.getRuntime().availableProcessors();
            for (int i = 0; i < coresCount; i++) coreIds.add(i);
        }

        Collections.sort(coreIds);
        int totalCores = coreIds.size();

        // 2. Discover all CPU policies
        Map<Integer, RawPolicyData> rawPolicies = new HashMap<>();
        String policyCmd = "for p in /sys/devices/system/cpu/cpufreq/policy*; do "
                + "[ -d \"$p\" ] && id=${p##*/policy} && echo \"POL|$id|$p|$(cat $p/affected_cpus 2>/dev/null)|$(cat $p/scaling_cur_freq 2>/dev/null)|$(cat $p/scaling_min_freq 2>/dev/null)|$(cat $p/scaling_max_freq 2>/dev/null)|$(cat $p/cpuinfo_min_freq 2>/dev/null)|$(cat $p/cpuinfo_max_freq 2>/dev/null)|$(cat $p/scaling_governor 2>/dev/null)|$(cat $p/scaling_available_governors 2>/dev/null)|$(cat $p/scaling_available_frequencies 2>/dev/null)\"; done";
        String polOut = RootUtils.runAndGetOutput(policyCmd);

        for (String line : polOut.split("\\n")) {
            line = line.trim();
            if (line.startsWith("POL|")) {
                String[] parts = line.split("\\|", -1);
                if (parts.length >= 11) {
                    try {
                        int polId = Integer.parseInt(parts[1].trim());
                        RawPolicyData data = new RawPolicyData();
                        data.policyId = polId;
                        data.path = parts[2].trim();
                        data.affectedCpusStr = parts[3].trim();
                        data.curFreq = parseLong(parts[4]);
                        data.minFreq = parseLong(parts[5]);
                        data.maxFreq = parseLong(parts[6]);
                        data.hwMinFreq = parseLong(parts[7]);
                        data.hwMaxFreq = parseLong(parts[8]);
                        data.governor = parts[9].trim();
                        data.govListStr = parts[10].trim();
                        if (parts.length > 11) data.freqListStr = parts[11].trim();
                        rawPolicies.put(polId, data);
                    } catch (Exception ignored) {}
                }
            }
        }

        // If no policies found via /sys/devices/system/cpu/cpufreq/policy*, inspect individual cores
        if (rawPolicies.isEmpty()) {
            for (int c : coreIds) {
                String pPath = "/sys/devices/system/cpu/cpu" + c + "/cpufreq";
                if (NativeKernelAccess.exists(pPath + "/scaling_max_freq") || new File(pPath).exists()) {
                    RawPolicyData data = new RawPolicyData();
                    data.policyId = c;
                    data.path = pPath;
                    data.affectedCpusStr = String.valueOf(c);
                    data.curFreq = parseLong(NativeKernelAccess.readString(pPath + "/scaling_cur_freq"));
                    data.minFreq = parseLong(NativeKernelAccess.readString(pPath + "/scaling_min_freq"));
                    data.maxFreq = parseLong(NativeKernelAccess.readString(pPath + "/scaling_max_freq"));
                    data.hwMinFreq = parseLong(NativeKernelAccess.readString(pPath + "/cpuinfo_min_freq"));
                    data.hwMaxFreq = parseLong(NativeKernelAccess.readString(pPath + "/cpuinfo_max_freq"));
                    data.governor = NativeKernelAccess.readString(pPath + "/scaling_governor");
                    data.govListStr = NativeKernelAccess.readString(pPath + "/scaling_available_governors");
                    data.freqListStr = NativeKernelAccess.readString(pPath + "/scaling_available_frequencies");
                    rawPolicies.put(c, data);
                }
            }
        }

        // Sort policies by policy ID
        List<RawPolicyData> sortedRaw = new ArrayList<>(rawPolicies.values());
        Collections.sort(sortedRaw, Comparator.comparingInt(a -> a.policyId));

        // Determine cluster labels based on hwMaxFreq (or maxFreq)
        List<RawPolicyData> byMaxFreq = new ArrayList<>(sortedRaw);
        Collections.sort(byMaxFreq, (a, b) -> Long.compare(
                a.hwMaxFreq > 0 ? a.hwMaxFreq : a.maxFreq,
                b.hwMaxFreq > 0 ? b.hwMaxFreq : b.maxFreq
        ));

        Map<Integer, String> clusterLabels = new HashMap<>();
        if (byMaxFreq.size() == 1) {
            clusterLabels.put(byMaxFreq.get(0).policyId, "Unified Cluster");
        } else if (byMaxFreq.size() == 2) {
            clusterLabels.put(byMaxFreq.get(0).policyId, "LITTLE Cluster");
            clusterLabels.put(byMaxFreq.get(1).policyId, "BIG Cluster");
        } else if (byMaxFreq.size() >= 3) {
            for (int i = 0; i < byMaxFreq.size(); i++) {
                int pid = byMaxFreq.get(i).policyId;
                if (i == 0) {
                    clusterLabels.put(pid, "LITTLE Cluster");
                } else if (i == byMaxFreq.size() - 1) {
                    clusterLabels.put(pid, "PRIME Cluster");
                } else {
                    clusterLabels.put(pid, "BIG Cluster " + i);
                }
            }
        }

        // Build CpuPolicy objects
        List<CpuPolicy> policies = new ArrayList<>();
        Map<Integer, Integer> cpuToPolicyMap = new HashMap<>();

        for (RawPolicyData raw : sortedRaw) {
            List<Integer> aff = new ArrayList<>();
            if (!raw.affectedCpusStr.isEmpty()) {
                for (String s : raw.affectedCpusStr.split("\\s+")) {
                    try {
                        int cId = Integer.parseInt(s.trim());
                        aff.add(cId);
                        cpuToPolicyMap.put(cId, raw.policyId);
                    } catch (Exception ignored) {}
                }
            }
            if (aff.isEmpty()) {
                aff.add(raw.policyId);
                cpuToPolicyMap.put(raw.policyId, raw.policyId);
            }

            List<String> govs = new ArrayList<>();
            if (!raw.govListStr.isEmpty()) {
                for (String g : raw.govListStr.split("\\s+")) {
                    if (!g.trim().isEmpty() && !govs.contains(g.trim())) govs.add(g.trim());
                }
            }
            if (govs.isEmpty()) {
                govs.addAll(Arrays.asList("schedutil", "performance", "powersave", "interactive"));
            }

            Set<Long> freqSet = new LinkedHashSet<>();
            if (!raw.freqListStr.isEmpty()) {
                for (String f : raw.freqListStr.split("\\s+")) {
                    try {
                        long val = Long.parseLong(f.trim());
                        if (val > 50000) freqSet.add(val);
                    } catch (Exception ignored) {}
                }
            }
            if (freqSet.isEmpty() && raw.hwMinFreq > 0 && raw.hwMaxFreq > 0) {
                freqSet.add(raw.hwMinFreq);
                freqSet.add(raw.hwMaxFreq);
            }
            List<Long> freqList = new ArrayList<>(freqSet);
            Collections.sort(freqList);

            String label = clusterLabels.containsKey(raw.policyId) ? clusterLabels.get(raw.policyId) : ("Cluster " + raw.policyId);

            CpuPolicy pol = new CpuPolicy(
                    raw.policyId,
                    raw.path,
                    aff,
                    raw.curFreq,
                    raw.minFreq,
                    raw.maxFreq,
                    raw.hwMinFreq > 0 ? raw.hwMinFreq : raw.minFreq,
                    raw.hwMaxFreq > 0 ? raw.hwMaxFreq : raw.maxFreq,
                    raw.governor.isEmpty() ? "schedutil" : raw.governor,
                    govs,
                    freqList,
                    label
            );
            policies.add(pol);
        }

        // 3. Detect Core online states and hotplug capability
        String coreCmd = "for c in /sys/devices/system/cpu/cpu[0-9]*; do "
                + "[ -d \"$c\" ] && id=${c##*/cpu} && [ -n \"$id\" ] && "
                + "on=$(cat $c/online 2>/dev/null || echo 1) && "
                + "hp=$([ -w $c/online ] && echo 1 || echo 0) && "
                + "cur=$(cat $c/cpufreq/scaling_cur_freq 2>/dev/null || echo 0) && "
                + "echo \"CORE|$id|$on|$hp|$cur\"; done";
        String coreOut = RootUtils.runAndGetOutput(coreCmd);

        Map<Integer, RawCoreData> rawCores = new HashMap<>();
        for (String line : coreOut.split("\\n")) {
            line = line.trim();
            if (line.startsWith("CORE|")) {
                String[] p = line.split("\\|", -1);
                if (p.length >= 5) {
                    try {
                        int cId = Integer.parseInt(p[1].trim());
                        RawCoreData d = new RawCoreData();
                        d.coreId = cId;
                        d.isOnline = !"0".equals(p[2].trim());
                        d.canHotplug = "1".equals(p[3].trim()) && cId != 0; // CPU0 cannot be hotplugged
                        d.curFreq = parseLong(p[4]);
                        rawCores.put(cId, d);
                    } catch (Exception ignored) {}
                }
            }
        }

        List<CpuCore> cores = new ArrayList<>();
        for (int cId : coreIds) {
            RawCoreData r = rawCores.get(cId);
            boolean on = (r != null) ? r.isOnline : true;
            boolean canHp = (r != null) ? r.canHotplug : (cId != 0);
            long curF = (r != null) ? r.curFreq : 0;
            int polId = cpuToPolicyMap.containsKey(cId) ? cpuToPolicyMap.get(cId) : 0;
            cores.add(new CpuCore(cId, on, canHp, polId, curF));
        }

        // Summary builder
        StringBuilder summary = new StringBuilder();
        summary.append(totalCores).append(" Cores: ");
        for (int i = 0; i < policies.size(); i++) {
            CpuPolicy p = policies.get(i);
            if (i > 0) summary.append(" • ");
            summary.append(p.affectedCpus.size()).append(" ")
                    .append(p.clusterLabel.replace(" Cluster", ""));
            if (p.affectedCpus.size() > 1) {
                summary.append(" (").append(p.affectedCpus.get(0)).append("-").append(p.affectedCpus.get(p.affectedCpus.size() - 1)).append(")");
            } else if (!p.affectedCpus.isEmpty()) {
                summary.append(" (").append(p.affectedCpus.get(0)).append(")");
            }
        }

        return new DeviceCpuTopology(cores, policies, totalCores, summary.toString());
    }

    /**
     * Toggles a CPU core online or offline where supported.
     * CPU0 is strictly protected and will never be taken offline.
     */
    public static boolean setCpuOnline(int coreId, boolean online) {
        if (coreId == 0) {
            Log.w(TAG, "Cannot take CPU0 offline (Boot critical core)");
            return false;
        }
        String path = "/sys/devices/system/cpu/cpu" + coreId + "/online";
        String val = online ? "1" : "0";
        String cmd = String.format(Locale.US, "[ -w %s ] && echo %s > %s", path, val, path);
        RootUtils.runCommand(cmd);

        // Verify
        String check = RootUtils.runAndGetOutput("cat " + path + " 2>/dev/null").trim();
        return val.equals(check);
    }

    /**
     * Sets frequency limits and governor for a specific CPU policy cluster.
     * Uses strict safe write ordering (raising: max first then min; lowering: min first then max).
     */
    public static void setPolicyFrequency(int policyId, long minFreqKhz, long maxFreqKhz, @Nullable String governor) {
        String p = "/sys/devices/system/cpu/cpufreq/policy" + policyId;
        if (!NativeKernelAccess.exists(p)) {
            p = "/sys/devices/system/cpu/cpu" + policyId + "/cpufreq";
        }

        String curMinStr = NativeKernelAccess.readString(p + "/scaling_min_freq");
        long curMin = parseLong(curMinStr);

        StringBuilder sb = new StringBuilder();
        sb.append("setprop persist.revror.manual_cpu 1; ");

        if (governor != null && !governor.trim().isEmpty()) {
            sb.append("[ -w ").append(p).append("/scaling_governor ] && echo ").append(governor.trim()).append(" > ").append(p).append("/scaling_governor; ");
        }

        if (maxFreqKhz >= curMin) {
            // Raising: Set max first, then min
            sb.append("[ -w ").append(p).append("/scaling_max_freq ] && echo ").append(maxFreqKhz).append(" > ").append(p).append("/scaling_max_freq; ");
            sb.append("[ -w ").append(p).append("/scaling_min_freq ] && echo ").append(minFreqKhz).append(" > ").append(p).append("/scaling_min_freq; ");
        } else {
            // Lowering: Set min first, then max
            sb.append("[ -w ").append(p).append("/scaling_min_freq ] && echo ").append(minFreqKhz).append(" > ").append(p).append("/scaling_min_freq; ");
            sb.append("[ -w ").append(p).append("/scaling_max_freq ] && echo ").append(maxFreqKhz).append(" > ").append(p).append("/scaling_max_freq; ");
        }

        RootUtils.runCommand(sb.toString());
    }

    public static String formatFreq(long freqKhz) {
        if (freqKhz >= 1000000) {
            return String.format(Locale.US, "%.2f GHz", freqKhz / 1000000.0);
        } else if (freqKhz > 0) {
            return String.format(Locale.US, "%d MHz", freqKhz / 1000);
        } else {
            return "Auto";
        }
    }

    private static long parseLong(@Nullable String val) {
        if (val == null) return 0;
        try {
            return Long.parseLong(val.trim());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static class RawPolicyData {
        int policyId;
        String path = "";
        String affectedCpusStr = "";
        long curFreq;
        long minFreq;
        long maxFreq;
        long hwMinFreq;
        long hwMaxFreq;
        String governor = "";
        String govListStr = "";
        String freqListStr = "";
    }

    private static class RawCoreData {
        int coreId;
        boolean isOnline = true;
        boolean canHotplug = false;
        long curFreq;
    }
}

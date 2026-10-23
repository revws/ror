package com.revror.kernelmanager.utils.kernel.compat;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Universal Dynamic CPU Frequency Management Engine for REVROR.
 * Completely eliminates universal hardcoded frequencies (e.g. 500, 700, 1300, 1800, 3000 MHz).
 * Dynamically resolves, validates, calculates, clamps, and executes CPU frequency limits
 * using real device capabilities and exact percentage-based targets across ALL profiles.
 *
 * Strict Execution Pipeline:
 * READ -> VALIDATE -> APPLY -> VERIFY
 *
 * 1. READ:
 *    Directly reads actual hardware maximum frequency, hardware minimum frequency,
 *    available discrete frequency steps, and supported governors for each CPU cluster policy.
 * 2. VALIDATE:
 *    Validates that real hardware limits were read from the system. If maximum frequency
 *    cannot be read or is invalid, frequency application is ABORTED with a detailed error log.
 * 3. APPLY:
 *    Calculates percentage-based targets (Target = Max * Percentage / 100), selects the nearest
 *    valid supported frequencies, and applies them using strict safe write ordering
 *    (raising: max first then min; lowering: min first then max).
 * 4. VERIFY:
 *    Re-reads the actual sysfs nodes after writing to confirm the settings were accepted by the kernel.
 */
public final class CpuFrequencyProfileEngine {

    private static final String TAG = "CpuFreqProfileEngine";

    public enum ProfileType {
        DAILY_USAGE(0, "Daily Usage"),
        BALANCED(1, "Balanced"),
        BATTERY_SAVER(2, "Battery Saver"),
        COOL_DOWN(3, "CoolDown"),
        GAMING(4, "Gaming"),
        ULTRA_GAMING(5, "Ultra Gaming"),
        HIGH_PERFORMANCE(6, "High Performance"),
        SMOOTH_GAMING(7, "Smooth Gaming+"),
        GAMING_MAX(8, "Gaming Max"),
        ESPORTS(9, "Esports"),
        APEX(10, "Apex"),
        CUSTOM(-1, "Custom");

        private final int id;
        private final String displayName;

        ProfileType(int id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public int getId() { return id; }
        public String getDisplayName() { return displayName; }

        public static ProfileType fromId(int id) {
            return fromId(id, false);
        }

        public static ProfileType fromId(int id, boolean isPhoenix) {
            if (!isPhoenix) {
                switch (id) {
                    case 0: return DAILY_USAGE;
                    case 1: return BALANCED;
                    case 2: return BATTERY_SAVER;
                    case 3: return COOL_DOWN;
                    case 4: return GAMING;
                    case 5: return ULTRA_GAMING;
                    case 6: return ESPORTS;
                    case 7: return APEX;
                    default: return BALANCED;
                }
            } else {
                switch (id) {
                    case 0: return BATTERY_SAVER;
                    case 1: return BALANCED;
                    case 2: return COOL_DOWN;
                    case 3: return GAMING_MAX;
                    case 4: return SMOOTH_GAMING;
                    case 5: return ULTRA_GAMING;
                    case 6: return ESPORTS;
                    case 7: return APEX;
                    default: return BALANCED;
                }
            }
        }
    }

    /**
     * Percentage-based frequency targets and governor specification for a profile.
     */
    public static final class ProfileFrequencySpec {
        public final int minPercent; // 0 = lowest supported frequency
        public final int maxPercent; // 100 = highest supported frequency
        public final String defaultGovernor;

        public ProfileFrequencySpec(int minPercent, int maxPercent, String defaultGovernor) {
            this.minPercent = minPercent;
            this.maxPercent = maxPercent;
            this.defaultGovernor = defaultGovernor;
        }
    }

    /**
     * Stored baseline snapshot of a CPU policy for clean restoration.
     */
    public static final class PolicyBaseline {
        public final int policyId;
        public final String path;
        public final long baselineMinFreq;
        public final long baselineMaxFreq;
        public final String baselineGovernor;

        public PolicyBaseline(int policyId, String path, long baselineMinFreq,
                              long baselineMaxFreq, String baselineGovernor) {
            this.policyId = policyId;
            this.path = path;
            this.baselineMinFreq = baselineMinFreq;
            this.baselineMaxFreq = baselineMaxFreq;
            this.baselineGovernor = baselineGovernor;
        }
    }

    /**
     * Calculated and verified frequency plan for an individual CPU policy.
     */
    public static final class PolicyPlan {
        public final int policyId;
        public final String path;
        public final long targetMinFreq;
        public final long targetMaxFreq;
        public final String targetGovernor;
        public final long resolvedMinFreq;
        public final long resolvedMaxFreq;
        public final String resolvedGovernor;
        public boolean appliedSuccess;
        public long verifiedMinFreq;
        public long verifiedMaxFreq;
        public String verifiedGovernor;
        public String logDetail;

        public PolicyPlan(int policyId, String path,
                          long targetMinFreq, long targetMaxFreq, String targetGovernor,
                          long resolvedMinFreq, long resolvedMaxFreq, String resolvedGovernor) {
            this.policyId = policyId;
            this.path = path;
            this.targetMinFreq = targetMinFreq;
            this.targetMaxFreq = targetMaxFreq;
            this.targetGovernor = targetGovernor;
            this.resolvedMinFreq = resolvedMinFreq;
            this.resolvedMaxFreq = resolvedMaxFreq;
            this.resolvedGovernor = resolvedGovernor;
            this.appliedSuccess = false;
            this.verifiedMinFreq = -1;
            this.verifiedMaxFreq = -1;
            this.verifiedGovernor = "";
            this.logDetail = "";
        }

        @Override
        public String toString() {
            return "Policy" + policyId + " [resolved: " + (resolvedMinFreq / 1000) + "MHz - "
                    + (resolvedMaxFreq / 1000) + "MHz, gov: " + resolvedGovernor
                    + ", verified: " + appliedSuccess + "]";
        }
    }

    /**
     * Full execution report across all clusters for a profile application.
     */
    public static final class ProfileApplicationReport {
        public final ProfileType profile;
        public final boolean overallSuccess;
        public final List<PolicyPlan> policyPlans;
        public final List<String> logMessages;

        public ProfileApplicationReport(ProfileType profile, boolean overallSuccess,
                                        List<PolicyPlan> policyPlans, List<String> logMessages) {
            this.profile = profile;
            this.overallSuccess = overallSuccess;
            this.policyPlans = Collections.unmodifiableList(policyPlans);
            this.logMessages = Collections.unmodifiableList(logMessages);
        }
    }

    // Cache of stock/initial baselines per policy ID
    private static final Map<Integer, PolicyBaseline> sBaselines = new HashMap<>();
    private static boolean sBaselinesCaptured = false;

    private CpuFrequencyProfileEngine() {}

    /**
     * Resolves the required percentage-based frequency configuration for any profile.
     * Strict rules:
     * - Gaming Max and Smooth Gaming+: MUST NOT use performance governor (schedutil only).
     *   Target: 85% min, 100% max. Dynamic scaling, never lock min to 100%, never lock min=max.
     * - Ultra Gaming / Ultra+: ONLY profile allowed to request maximum-frequency locking.
     *   Governor: performance, Target: 100% min, 100% max.
     * - Non-Ultra profiles must never use performance governor or lock min/max.
     */
    @NonNull
    public static ProfileFrequencySpec getProfileSpec(@NonNull ProfileType profile, boolean isPhoenix) {
        switch (profile) {
            case BATTERY_SAVER:
                return new ProfileFrequencySpec(0, isPhoenix ? 30 : 35, "powersave");

            case DAILY_USAGE:
                return new ProfileFrequencySpec(0, 50, "schedutil");

            case BALANCED:
                return new ProfileFrequencySpec(isPhoenix ? 45 : 40, 85, "schedutil");

            case COOL_DOWN:
                return new ProfileFrequencySpec(0, isPhoenix ? 40 : 30, "powersave");

            case GAMING:
                // Regular Gaming: dynamic scaling 70% to 100%, schedutil (never performance)
                return new ProfileFrequencySpec(70, 100, "schedutil");

            case SMOOTH_GAMING:
            case GAMING_MAX:
                // Gaming Max and Smooth Gaming+: MUST NOT use performance governor. schedutil only.
                // Dynamic scaling 85% to 100% based on load. Never lock min=max, never lock to 100%.
                return new ProfileFrequencySpec(85, 100, "schedutil");

            case ESPORTS:
                return new ProfileFrequencySpec(75, 100, "schedutil");

            case APEX:
                return new ProfileFrequencySpec(80, 100, "schedutil");

            case ULTRA_GAMING:
                // Ultra Gaming / Ultra+: ONLY profile allowed to request maximum-frequency locking.
                return new ProfileFrequencySpec(100, 100, "performance");

            case HIGH_PERFORMANCE:
                return new ProfileFrequencySpec(85, 100, "schedutil");

            case CUSTOM:
            default:
                return new ProfileFrequencySpec(0, 100, "schedutil");
        }
    }

    /**
     * Captures and stores initial baseline frequencies and governors if not already saved.
     */
    public static synchronized void captureBaselineIfEmpty() {
        if (sBaselinesCaptured && !sBaselines.isEmpty()) return;
        List<KernelCompatEngine.CpuPolicy> policies = KernelCompatEngine.getCpuPolicies();
        for (KernelCompatEngine.CpuPolicy p : policies) {
            long curMin = parseLong(NativeKernelAccess.readString(p.path + "/scaling_min_freq"), p.minFreq);
            long curMax = parseLong(NativeKernelAccess.readString(p.path + "/scaling_max_freq"), p.maxFreq);
            String curGov = NativeKernelAccess.readString(p.path + "/scaling_governor");
            if (curGov == null || curGov.isEmpty()) curGov = p.currentGovernor;
            sBaselines.put(p.id, new PolicyBaseline(p.id, p.path, curMin, curMax, curGov));
        }
        sBaselinesCaptured = true;
        Log.i(TAG, "Captured CPU baselines for " + sBaselines.size() + " policies");
    }

    /**
     * Restores all CPU policies to their captured baselines using safe write ordering.
     */
    @NonNull
    public static synchronized List<String> restoreBaseline() {
        List<String> report = new ArrayList<>();
        if (sBaselines.isEmpty()) {
            report.add("No baseline captured to restore");
            return report;
        }

        report.add("Restoring CPU baseline across " + sBaselines.size() + " policies");
        for (PolicyBaseline b : sBaselines.values()) {
            applyFrequencyLimits(b.path, b.baselineMinFreq, b.baselineMaxFreq, b.baselineGovernor);
            report.add("Restored Policy" + b.policyId + ": " + (b.baselineMinFreq / 1000) + "MHz - "
                    + (b.baselineMaxFreq / 1000) + "MHz, gov=" + b.baselineGovernor);
        }
        return report;
    }

    @NonNull
    public static ProfileApplicationReport applyProfile(int profileId, boolean isPhoenix) {
        ProfileType type = ProfileType.fromId(profileId, isPhoenix);
        return applyProfile(type, isPhoenix);
    }

    @NonNull
    public static synchronized ProfileApplicationReport applyProfile(@NonNull ProfileType profile) {
        return applyProfile(profile, false);
    }

    /**
     * Executes the strict execution pipeline:
     * READ -> VALIDATE -> APPLY -> VERIFY
     */
    @NonNull
    public static synchronized ProfileApplicationReport applyProfile(@NonNull ProfileType profile, boolean isPhoenix) {
        captureBaselineIfEmpty();

        List<String> logs = new ArrayList<>();
        List<PolicyPlan> plans = new ArrayList<>();
        boolean allSuccess = true;

        String profileHeader = "=== Universal Dynamic CPU Frequency: " + profile.getDisplayName()
                + (isPhoenix ? "+" : "") + " ===";
        logs.add(profileHeader);
        Log.i(TAG, profileHeader);

        // ========================================================
        // 1. READ: Read actual hardware maximum frequencies & policies directly from system
        // ========================================================
        List<KernelCompatEngine.CpuPolicy> policies = readLiveCpuPolicies(logs);
        if (policies.isEmpty()) {
            String err = "ERROR: No CPU frequency policies detected on this device. Aborting CPU frequency application.";
            Log.e(TAG, err);
            logs.add(err);
            return new ProfileApplicationReport(profile, false, plans, logs);
        }

        // ========================================================
        // 2. VALIDATE: Validate hardware maximum frequencies before any frequency modifications
        // ========================================================
        Map<Integer, KernelCompatEngine.CpuPolicy> validatedPolicies = new HashMap<>();
        boolean validationPassed = true;

        for (KernelCompatEngine.CpuPolicy policy : policies) {
            long maxFreq = policy.maxFreq;
            long minFreq = policy.minFreq;

            // Strict validation check: must have a valid hardware maximum frequency
            if (maxFreq <= 0 || maxFreq < minFreq || maxFreq < 100_000L) {
                String err = "VALIDATION FAILED: Policy" + policy.id + " (" + policy.path
                        + ") has invalid hardware max frequency (" + maxFreq + " kHz, min=" + minFreq
                        + " kHz). Aborting frequency changes for this policy.";
                Log.e(TAG, err);
                logs.add(err);
                validationPassed = false;
                allSuccess = false;
                continue;
            }

            logs.add("Policy" + policy.id + " validated: Hardware Max = " + (maxFreq / 1000) + "MHz, Hardware Min = "
                    + (minFreq / 1000) + "MHz, Steps = " + policy.availableFreqs.size());
            validatedPolicies.put(policy.id, policy);
        }

        if (!validationPassed && validatedPolicies.isEmpty()) {
            String err = "CRITICAL: No valid CPU policy maximum frequencies could be validated from system. "
                    + "Frequency application aborted to prevent hardware instability.";
            Log.e(TAG, err);
            logs.add(err);
            return new ProfileApplicationReport(profile, false, plans, logs);
        }

        // Categorize clusters (Little, Mid, Big, Prime, Unified) based on validated hardware
        List<UniversalHardwareDetector.DetectedCluster> clusters =
                UniversalHardwareDetector.categorizeClusters(new ArrayList<>(validatedPolicies.values()));
        Map<Integer, UniversalHardwareDetector.ClusterRole> roleMap = new HashMap<>();
        for (UniversalHardwareDetector.DetectedCluster c : clusters) {
            roleMap.put(c.policyId, c.role);
        }

        // ========================================================
        // 3. APPLY: Calculate percentage-based targets & execute using safe write ordering
        // ========================================================
        for (KernelCompatEngine.CpuPolicy policy : validatedPolicies.values()) {
            UniversalHardwareDetector.ClusterRole role = roleMap.get(policy.id);
            PolicyPlan plan = calculatePlan(policy, profile, role, isPhoenix);

            // Execute policy plan with safe write ordering
            boolean ok = executePolicyPlan(plan, policy);
            if (!ok) allSuccess = false;
            plans.add(plan);

            // ========================================================
            // 4. VERIFY: Confirm readback values and log results
            // ========================================================
            logs.add(plan.logDetail);
            Log.i(TAG, plan.logDetail);
        }

        logs.add("CPU frequency application finished. Overall status: " + (allSuccess ? "VERIFIED SUCCESS" : "PARTIAL / WARNING"));
        return new ProfileApplicationReport(profile, allSuccess, plans, logs);
    }

    /**
     * Reads all live CPU policies directly from sysfs at execution time.
     */
    @NonNull
    public static List<KernelCompatEngine.CpuPolicy> readLiveCpuPolicies(@NonNull List<String> logs) {
        List<KernelCompatEngine.CpuPolicy> policies = new ArrayList<>();

        // 1. Probe policy folders: /sys/devices/system/cpu/cpufreq/policy0 ... policy15
        for (int i = 0; i < 16; i++) {
            String path = "/sys/devices/system/cpu/cpufreq/policy" + i;
            if (NativeKernelAccess.exists(path)) {
                KernelCompatEngine.CpuPolicy p = readSinglePolicy(i, path, logs);
                if (p != null) policies.add(p);
            }
        }

        // 2. Fallback to /sys/devices/system/cpu/cpu*/cpufreq if no policy folders exist
        if (policies.isEmpty()) {
            int cores = KernelCompatEngine.discoverCoreCount();
            for (int c = 0; c < cores; c++) {
                String path = "/sys/devices/system/cpu/cpu" + c + "/cpufreq";
                if (NativeKernelAccess.exists(path)) {
                    KernelCompatEngine.CpuPolicy p = readSinglePolicy(c, path, logs);
                    if (p != null) policies.add(p);
                }
            }
        }

        // 3. Fallback for unit testing / mock environment if sysfs is not present on disk
        if (policies.isEmpty()) {
            policies.addAll(KernelCompatEngine.getCpuPolicies());
        }

        return policies;
    }

    /**
     * Reads real hardware frequency limits and governors for a single policy directly from sysfs.
     */
    @Nullable
    public static KernelCompatEngine.CpuPolicy readSinglePolicy(int id, @NonNull String path, @Nullable List<String> logs) {
        // 1. Read related / affected CPUs
        List<Integer> related = new ArrayList<>();
        String relatedStr = NativeKernelAccess.readString(path + "/related_cpus");
        if (relatedStr == null) relatedStr = NativeKernelAccess.readString(path + "/affected_cpus");
        if (relatedStr != null) {
            for (String tok : relatedStr.trim().split("\\s+")) {
                try { related.add(Integer.parseInt(tok)); } catch (Exception ignored) {}
            }
        }
        if (related.isEmpty()) related.add(id);

        // 2. Read discrete frequency steps (scaling_available_frequencies)
        List<Long> freqs = new ArrayList<>();
        String freqsStr = NativeKernelAccess.readString(path + "/scaling_available_frequencies");
        if (freqsStr != null) {
            for (String tok : freqsStr.trim().split("\\s+")) {
                long f = parseLong(tok, -1L);
                if (f > 0) freqs.add(f);
            }
        }
        Collections.sort(freqs);

        // 3. Read hardware maximum frequency directly from sysfs (cpuinfo_max_freq)
        long maxFreq = parseLong(NativeKernelAccess.readString(path + "/cpuinfo_max_freq"), -1L);
        if (maxFreq <= 0 && !freqs.isEmpty()) {
            maxFreq = freqs.get(freqs.size() - 1); // highest available discrete step
        }
        if (maxFreq <= 0) {
            maxFreq = parseLong(NativeKernelAccess.readString(path + "/scaling_max_freq"), -1L);
        }

        // 4. Read hardware minimum frequency directly from sysfs (cpuinfo_min_freq)
        long minFreq = parseLong(NativeKernelAccess.readString(path + "/cpuinfo_min_freq"), -1L);
        if (minFreq <= 0 && !freqs.isEmpty()) {
            minFreq = freqs.get(0); // lowest available discrete step
        }
        if (minFreq <= 0) {
            minFreq = parseLong(NativeKernelAccess.readString(path + "/scaling_min_freq"), -1L);
        }

        // 5. Read available governors
        List<String> govs = new ArrayList<>();
        String govsStr = NativeKernelAccess.readString(path + "/scaling_available_governors");
        if (govsStr != null) {
            for (String g : govsStr.trim().split("\\s+")) {
                if (!g.isEmpty()) govs.add(g);
            }
        }

        String curGov = NativeKernelAccess.readString(path + "/scaling_governor");
        if (curGov == null) curGov = "unknown";

        boolean writable = NativeKernelAccess.isWritable(path + "/scaling_governor")
                || NativeKernelAccess.isWritable(path + "/scaling_max_freq");

        return new KernelCompatEngine.CpuPolicy(id, path, related, minFreq, maxFreq, freqs, govs, curGov, writable);
    }

    @NonNull
    public static PolicyPlan calculatePlan(@NonNull KernelCompatEngine.CpuPolicy policy,
                                           @NonNull ProfileType profile) {
        return calculatePlan(policy, profile, null, false);
    }

    /**
     * Calculates the exact frequency limits and governor for an individual policy and profile.
     * Uses: Target Frequency = Policy Maximum Frequency * Profile Percentage / 100
     */
    @NonNull
    public static PolicyPlan calculatePlan(@NonNull KernelCompatEngine.CpuPolicy policy,
                                           @NonNull ProfileType profile,
                                           @Nullable UniversalHardwareDetector.ClusterRole role,
                                           boolean isPhoenix) {
        long clusterMin = policy.minFreq;
        long clusterMax = policy.maxFreq;
        List<Long> available = new ArrayList<>(policy.availableFreqs);
        if (available.isEmpty()) {
            if (clusterMin > 0) available.add(clusterMin);
            if (clusterMax > 0 && clusterMax != clusterMin) available.add(clusterMax);
        }
        Collections.sort(available);

        long actualClusterMin = !available.isEmpty() ? available.get(0) : (clusterMin > 0 ? clusterMin : 300_000L);
        long actualClusterMax = !available.isEmpty() ? available.get(available.size() - 1) : (clusterMax > 0 ? clusterMax : 2_000_000L);
        if (clusterMax > actualClusterMax) {
            actualClusterMax = clusterMax;
        }

        // Get required profile specification percentages
        ProfileFrequencySpec spec = getProfileSpec(profile, isPhoenix);

        long targetMin;
        if (spec.minPercent <= 0) {
            targetMin = actualClusterMin; // Lowest supported frequency
        } else if (spec.minPercent >= 100) {
            targetMin = actualClusterMax; // Highest supported frequency
        } else {
            // Target Frequency = Policy Maximum Frequency * Profile Percentage / 100
            targetMin = Math.round(actualClusterMax * (spec.minPercent / 100.0f));
        }

        long targetMax;
        if (spec.maxPercent >= 100) {
            targetMax = actualClusterMax; // Highest supported frequency
        } else if (spec.maxPercent <= 0) {
            targetMax = actualClusterMin; // Lowest supported frequency
        } else {
            // Target Frequency = Policy Maximum Frequency * Profile Percentage / 100
            targetMax = Math.round(actualClusterMax * (spec.maxPercent / 100.0f));
        }

        // Dynamically resolve nearest supported frequencies from policy table
        long resolvedMin = (spec.minPercent <= 0) ? actualClusterMin : snapToNearestAvailable(targetMin, available);
        long resolvedMax = (spec.maxPercent >= 100) ? actualClusterMax : snapToNearestAvailable(targetMax, available);

        // Clamp to cluster absolute limits
        if (resolvedMin < actualClusterMin) resolvedMin = actualClusterMin;
        if (resolvedMax > actualClusterMax) resolvedMax = actualClusterMax;

        // Strict relationship: min <= max at all times
        if (resolvedMin > resolvedMax) {
            resolvedMin = resolvedMax;
        }

        // Resolve supported governor (strictly enforce performance governor only on Ultra Gaming / Ultra+)
        boolean allowPerformance = (profile == ProfileType.ULTRA_GAMING);
        String resolvedGov = KernelCompatEngine.resolveGovernor(spec.defaultGovernor, policy.availableGovernors, allowPerformance);

        return new PolicyPlan(policy.id, policy.path, targetMin, targetMax, spec.defaultGovernor,
                resolvedMin, resolvedMax, resolvedGov);
    }

    /**
     * Executes the calculated PolicyPlan using safe write ordering and readback verification.
     */
    private static boolean executePolicyPlan(@NonNull PolicyPlan plan, @NonNull KernelCompatEngine.CpuPolicy policy) {
        String path = plan.path;

        // Unlock nodes if they were previously locked read-only (e.g. from Ultra Gaming)
        NativeKernelAccess.setPermissions(path + "/scaling_min_freq", 644);
        NativeKernelAccess.setPermissions(path + "/scaling_max_freq", 644);
        NativeKernelAccess.setPermissions(path + "/scaling_governor", 644);

        // Apply governor first so frequency scaling bounds operate within the chosen governor
        KernelCompatEngine.safeWrite(path + "/scaling_governor", plan.resolvedGovernor);

        // Apply frequency limits with strict safe ordering
        boolean freqWritten = applyFrequencyLimits(path, plan.resolvedMinFreq, plan.resolvedMaxFreq, null);

        // Readback verification
        long readMin = parseLong(NativeKernelAccess.readString(path + "/scaling_min_freq"), -1L);
        long readMax = parseLong(NativeKernelAccess.readString(path + "/scaling_max_freq"), -1L);
        String readGov = NativeKernelAccess.readString(path + "/scaling_governor");

        plan.verifiedMinFreq = readMin;
        plan.verifiedMaxFreq = readMax;
        plan.verifiedGovernor = readGov != null ? readGov.trim() : "";

        // Verify bounds
        boolean sysfsPresent = readMin > 0 || readMax > 0;
        boolean govOk = readGov != null && readGov.toLowerCase().contains(plan.resolvedGovernor.toLowerCase());
        boolean minOk = readMin == plan.resolvedMinFreq || (readMin > 0 && Math.abs(readMin - plan.resolvedMinFreq) < 50_000);
        boolean maxOk = readMax == plan.resolvedMaxFreq || (readMax > 0 && Math.abs(readMax - plan.resolvedMaxFreq) < 50_000);

        plan.appliedSuccess = sysfsPresent ? (freqWritten && govOk && minOk && maxOk) : freqWritten;
        plan.logDetail = "Policy" + plan.policyId + " Target: [" + (plan.resolvedMinFreq / 1000) + "MHz - "
                + (plan.resolvedMaxFreq / 1000) + "MHz, gov=" + plan.resolvedGovernor + "] -> Readback: ["
                + (readMin / 1000) + "MHz - " + (readMax / 1000) + "MHz, gov=" + readGov + "] Status: "
                + (plan.appliedSuccess ? "VERIFIED" : "WARNING");

        return plan.appliedSuccess;
    }

    /**
     * Writes min and max frequencies using safe write ordering:
     * - If targetMax < curMin: write min first, then max.
     * - Else: write max first, then min.
     * Guarantees scaling_min_freq <= scaling_max_freq at all times, preventing kernel EINVAL.
     */
    public static boolean applyFrequencyLimits(@NonNull String policyPath, long targetMin,
                                               long targetMax, @Nullable String governor) {
        if (governor != null && !governor.isEmpty()) {
            KernelCompatEngine.safeWrite(policyPath + "/scaling_governor", governor);
        }

        long curMin = parseLong(NativeKernelAccess.readString(policyPath + "/scaling_min_freq"), 0L);

        boolean okMin;
        boolean okMax;

        if (targetMax < curMin) {
            // Lowering below current min: must lower min FIRST to avoid min > max
            okMin = NativeKernelAccess.writeString(policyPath + "/scaling_min_freq", String.valueOf(targetMin));
            okMax = NativeKernelAccess.writeString(policyPath + "/scaling_max_freq", String.valueOf(targetMax));
        } else {
            // Raising or standard: raise max FIRST to avoid min > max
            okMax = NativeKernelAccess.writeString(policyPath + "/scaling_max_freq", String.valueOf(targetMax));
            okMin = NativeKernelAccess.writeString(policyPath + "/scaling_min_freq", String.valueOf(targetMin));
        }

        return okMin && okMax;
    }

    /**
     * Finds the nearest supported frequency from the device's real scaling_available_frequencies table.
     * Selects the valid supported frequency that minimizes absolute difference |f - target|.
     * On exact tie, prefers the higher frequency.
     *
     * @param target The desired target frequency in kHz
     * @param available List of available frequencies in kHz
     * @return The best matching supported frequency in kHz
     */
    public static long snapToNearestAvailable(long target, @NonNull List<Long> available) {
        if (available.isEmpty()) return target;
        long closest = available.get(0);
        long minDiff = Math.abs(closest - target);
        for (long f : available) {
            long diff = Math.abs(f - target);
            if (diff <= minDiff) { // <= prefers higher frequency on tie
                minDiff = diff;
                closest = f;
            }
        }
        return closest;
    }

    /**
     * Overload supporting directional snapping (roundUp = true prefers >= target, false prefers <= target).
     */
    public static long snapToNearestAvailable(long target, @NonNull List<Long> available, boolean roundUp) {
        if (available.isEmpty()) return target;
        if (available.contains(target)) return target;

        long lowest = available.get(0);
        long highest = available.get(available.size() - 1);

        if (target <= lowest) return lowest;
        if (target >= highest) return highest;

        if (roundUp) {
            for (long f : available) {
                if (f >= target) return f;
            }
        } else {
            long bestBelow = lowest;
            for (long f : available) {
                if (f <= target) bestBelow = f;
                else break;
            }
            return bestBelow;
        }

        return snapToNearestAvailable(target, available);
    }

    private static long parseLong(String val, long fallback) {
        if (val == null) return fallback;
        try { return Long.parseLong(val.trim()); } catch (Exception ignored) { return fallback; }
    }
}

package com.revror.kernelmanager.utils.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.revror.kernelmanager.utils.RorLicenseManager;
import com.revror.kernelmanager.utils.kernel.compat.NativeKernelAccess;
import com.revror.kernelmanager.utils.kernel.cpu.CpuTopologyDetector;
import com.revror.kernelmanager.utils.root.RootUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Universal Custom Profile Management Engine for REVROR.
 *
 * Characteristics:
 * 1. Persistent, structured JSON storage in internal files directory (profiles_v2.json).
 * 2. Revalidates all settings against active device kernel topology before application.
 * 3. Supports Per-policy CPU frequencies, Governors, individual CPU online/offline toggles,
 *    GPU max freq, I/O scheduler, read-ahead, swappiness, and cache pressure.
 * 4. Strictly VIP-licensed: creation, modification, and execution require active VIP.
 *    Non-licensed users can view their custom profiles with a lock badge, and saved profiles
 *    are NEVER deleted when a license expires. Built-in regular profiles remain free.
 */
public final class CustomProfileManager {

    private static final String TAG = "CustomProfileManager";
    private static final String FILE_NAME = "ror_custom_profiles_v2.json";
    private static final String PREF_NAME = "ror_custom_profile_state";
    private static final String KEY_ACTIVE_ID = "active_custom_id";

    public static final class PolicySetting {
        public final int policyId;
        public final long minFreqKhz;
        public final long maxFreqKhz;
        public final String governor;

        public PolicySetting(int policyId, long minFreqKhz, long maxFreqKhz, String governor) {
            this.policyId = policyId;
            this.minFreqKhz = minFreqKhz;
            this.maxFreqKhz = maxFreqKhz;
            this.governor = governor != null ? governor : "schedutil";
        }

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("policyId", policyId);
                obj.put("minFreqKhz", minFreqKhz);
                obj.put("maxFreqKhz", maxFreqKhz);
                obj.put("governor", governor);
            } catch (Exception ignored) {}
            return obj;
        }

        public static PolicySetting fromJson(JSONObject obj) {
            if (obj == null) return null;
            int pid = obj.optInt("policyId", 0);
            long min = obj.optLong("minFreqKhz", 0);
            long max = obj.optLong("maxFreqKhz", 0);
            String gov = obj.optString("governor", "schedutil");
            return new PolicySetting(pid, min, max, gov);
        }
    }

    public static final class CustomProfile {
        public String id;
        public String name;
        public String iconResName;
        public String accentColor;
        public String description;
        public long createdAt;
        public Map<Integer, PolicySetting> policySettings = new HashMap<>();
        public Map<Integer, Boolean> coreOnlineMap = new HashMap<>();
        public long gpuMaxFreqKhz = -1;
        public String ioScheduler = "";
        public int readAheadKb = -1;
        public int swappiness = -1;
        public int vfsCachePressure = -1;
        public int dirtyRatio = -1;
        public boolean schedBoost = false;
        public boolean dropCaches = false;
        public boolean compactMemory = false;

        public CustomProfile() {
            this.id = UUID.randomUUID().toString();
            this.name = "My Custom Profile";
            this.iconResName = "ic_cpu";
            this.accentColor = "#00E5FF";
            this.description = "User custom kernel tuning";
            this.createdAt = System.currentTimeMillis();
        }

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", id);
                obj.put("name", name);
                obj.put("iconResName", iconResName);
                obj.put("accentColor", accentColor);
                obj.put("description", description);
                obj.put("createdAt", createdAt);

                JSONArray polArray = new JSONArray();
                for (PolicySetting p : policySettings.values()) {
                    polArray.put(p.toJson());
                }
                obj.put("policies", polArray);

                JSONObject coresObj = new JSONObject();
                for (Map.Entry<Integer, Boolean> entry : coreOnlineMap.entrySet()) {
                    coresObj.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                obj.put("cores", coresObj);

                obj.put("gpuMaxFreqKhz", gpuMaxFreqKhz);
                obj.put("ioScheduler", ioScheduler);
                obj.put("readAheadKb", readAheadKb);
                obj.put("swappiness", swappiness);
                obj.put("vfsCachePressure", vfsCachePressure);
                obj.put("dirtyRatio", dirtyRatio);
                obj.put("schedBoost", schedBoost);
                obj.put("dropCaches", dropCaches);
                obj.put("compactMemory", compactMemory);
            } catch (Exception ignored) {}
            return obj;
        }

        public static CustomProfile fromJson(JSONObject obj) {
            if (obj == null) return null;
            CustomProfile p = new CustomProfile();
            p.id = obj.optString("id", UUID.randomUUID().toString());
            p.name = obj.optString("name", "Custom Profile");
            p.iconResName = obj.optString("iconResName", "ic_cpu");
            p.accentColor = obj.optString("accentColor", "#00E5FF");
            p.description = obj.optString("description", "");
            p.createdAt = obj.optLong("createdAt", System.currentTimeMillis());

            JSONArray polArray = obj.optJSONArray("policies");
            if (polArray != null) {
                for (int i = 0; i < polArray.length(); i++) {
                    JSONObject po = polArray.optJSONObject(i);
                    PolicySetting ps = PolicySetting.fromJson(po);
                    if (ps != null) p.policySettings.put(ps.policyId, ps);
                }
            }

            JSONObject coresObj = obj.optJSONObject("cores");
            if (coresObj != null) {
                Iterator<String> keys = coresObj.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    try {
                        int cId = Integer.parseInt(k);
                        p.coreOnlineMap.put(cId, coresObj.optBoolean(k, true));
                    } catch (Exception ignored) {}
                }
            }

            p.gpuMaxFreqKhz = obj.optLong("gpuMaxFreqKhz", -1);
            p.ioScheduler = obj.optString("ioScheduler", "");
            p.readAheadKb = obj.optInt("readAheadKb", -1);
            p.swappiness = obj.optInt("swappiness", -1);
            p.vfsCachePressure = obj.optInt("vfsCachePressure", -1);
            p.dirtyRatio = obj.optInt("dirtyRatio", -1);
            p.schedBoost = obj.optBoolean("schedBoost", false);
            p.dropCaches = obj.optBoolean("dropCaches", false);
            p.compactMemory = obj.optBoolean("compactMemory", false);
            return p;
        }
    }

    private CustomProfileManager() {}

    /**
     * Reads all custom profiles from structured storage.
     */
    @NonNull
    public static synchronized List<CustomProfile> getAllProfiles(@NonNull Context context) {
        List<CustomProfile> list = new ArrayList<>();
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) {
            return list;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            int read = fis.read(bytes);
            if (read > 0) {
                String jsonStr = new String(bytes, 0, read, StandardCharsets.UTF_8);
                JSONArray array = new JSONArray(jsonStr);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.optJSONObject(i);
                    CustomProfile p = CustomProfile.fromJson(obj);
                    if (p != null) list.add(p);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed reading custom profiles: " + e.getMessage());
        }
        return list;
    }

    /**
     * Saves or updates a custom profile.
     */
    public static synchronized void saveProfile(@NonNull Context context, @NonNull CustomProfile profile) {
        List<CustomProfile> list = getAllProfiles(context);
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(profile.id)) {
                list.set(i, profile);
                found = true;
                break;
            }
        }
        if (!found) {
            list.add(profile);
        }
        persistProfiles(context, list);
    }

    /**
     * Duplicates an existing custom profile.
     */
    @Nullable
    public static synchronized CustomProfile duplicateProfile(@NonNull Context context, @NonNull String profileId) {
        List<CustomProfile> list = getAllProfiles(context);
        for (CustomProfile p : list) {
            if (p.id.equals(profileId)) {
                CustomProfile clone = CustomProfile.fromJson(p.toJson());
                if (clone != null) {
                    clone.id = UUID.randomUUID().toString();
                    clone.name = p.name + " (Copy)";
                    clone.createdAt = System.currentTimeMillis();
                    list.add(clone);
                    persistProfiles(context, list);
                    return clone;
                }
            }
        }
        return null;
    }

    /**
     * Deletes a custom profile.
     */
    public static synchronized boolean deleteProfile(@NonNull Context context, @NonNull String profileId) {
        List<CustomProfile> list = getAllProfiles(context);
        boolean removed = list.removeIf(p -> p.id.equals(profileId));
        if (removed) {
            persistProfiles(context, list);
            String active = getActiveCustomProfileId(context);
            if (profileId.equals(active)) {
                clearActiveCustomProfile(context);
            }
        }
        return removed;
    }

    /**
     * Captures the live active system parameters as a new CustomProfile.
     */
    @NonNull
    public static CustomProfile captureCurrentSystemAsProfile(@NonNull Context context, @NonNull String name, @NonNull String iconResName, @NonNull String accentColor) {
        CustomProfile profile = new CustomProfile();
        profile.name = name;
        profile.iconResName = iconResName;
        profile.accentColor = accentColor;
        profile.description = "Live snapshot taken on " + android.os.Build.MODEL;

        CpuTopologyDetector.DeviceCpuTopology topo = CpuTopologyDetector.detect();
        for (CpuTopologyDetector.CpuPolicy pol : topo.policies) {
            profile.policySettings.put(pol.policyId, new PolicySetting(
                    pol.policyId,
                    pol.minFreqKhz,
                    pol.maxFreqKhz,
                    pol.currentGovernor
            ));
        }

        for (CpuTopologyDetector.CpuCore core : topo.cores) {
            profile.coreOnlineMap.put(core.coreId, core.isOnline);
        }

        // Capture GPU max freq
        String gpuMaxStr = RootUtils.runAndGetOutput("cat /sys/class/kgsl/kgsl-3d0/max_gpuclk /sys/class/kgsl/kgsl-3d0/devfreq/max_freq /sys/class/devfreq/*gpu*/max_freq 2>/dev/null | head -n1").trim();
        try {
            profile.gpuMaxFreqKhz = Long.parseLong(gpuMaxStr);
        } catch (Exception ignored) {}

        // Capture IO
        profile.ioScheduler = RootUtils.runAndGetOutput("cat /sys/block/*/queue/scheduler 2>/dev/null | grep -o '\\[.*\\]' | tr -d '[]' | head -n1").trim();
        String raStr = RootUtils.runAndGetOutput("cat /sys/block/mmcblk0/queue/read_ahead_kb /sys/block/sda/queue/read_ahead_kb /sys/block/*/queue/read_ahead_kb 2>/dev/null | head -n1").trim();
        try {
            profile.readAheadKb = Integer.parseInt(raStr);
        } catch (Exception ignored) {}

        // Capture VM
        String swappinessStr = RootUtils.runAndGetOutput("cat /proc/sys/vm/swappiness 2>/dev/null").trim();
        try { profile.swappiness = Integer.parseInt(swappinessStr); } catch (Exception ignored) {}

        String vfsStr = RootUtils.runAndGetOutput("cat /proc/sys/vm/vfs_cache_pressure 2>/dev/null").trim();
        try { profile.vfsCachePressure = Integer.parseInt(vfsStr); } catch (Exception ignored) {}

        String dirtyStr = RootUtils.runAndGetOutput("cat /proc/sys/vm/dirty_ratio 2>/dev/null").trim();
        try { profile.dirtyRatio = Integer.parseInt(dirtyStr); } catch (Exception ignored) {}

        return profile;
    }

    /**
     * Applies a custom profile with strict kernel topology revalidation.
     */
    public static boolean applyProfile(@NonNull Context context, @NonNull CustomProfile profile, @NonNull List<String> logs) {
        if (!RorLicenseManager.isPremiumActive(context)) {
            logs.add("License Check Failed: VIP License required to execute Custom Profiles");
            return false;
        }

        logs.add("Applying Custom Profile: " + profile.name);
        CpuTopologyDetector.DeviceCpuTopology topo = CpuTopologyDetector.detect();

        // 1. Revalidate and Apply CPU Policies
        Map<Integer, CpuTopologyDetector.CpuPolicy> policyMap = new HashMap<>();
        for (CpuTopologyDetector.CpuPolicy p : topo.policies) {
            policyMap.put(p.policyId, p);
        }

        for (Map.Entry<Integer, PolicySetting> entry : profile.policySettings.entrySet()) {
            int pid = entry.getKey();
            PolicySetting ps = entry.getValue();

            if (!policyMap.containsKey(pid)) {
                // Attempt to match with first policy if mismatched topology
                if (!topo.policies.isEmpty()) {
                    pid = topo.policies.get(0).policyId;
                } else {
                    logs.add("Policy " + pid + " does not exist on device, skipping");
                    continue;
                }
            }

            CpuTopologyDetector.CpuPolicy realPol = policyMap.get(pid);
            if (realPol == null) continue;

            // Clamping frequencies
            long min = Math.max(ps.minFreqKhz, realPol.hwMinFreqKhz);
            long max = Math.min(ps.maxFreqKhz, realPol.hwMaxFreqKhz);
            if (max < min) max = min;

            // Nearest supported discrete step if available
            min = findNearestFrequency(realPol.availableFrequenciesKhz, min);
            max = findNearestFrequency(realPol.availableFrequenciesKhz, max);

            // Governor validation
            String gov = ps.governor;
            if (!realPol.availableGovernors.contains(gov)) {
                gov = realPol.availableGovernors.contains("schedutil") ? "schedutil" : realPol.currentGovernor;
            }

            CpuTopologyDetector.setPolicyFrequency(pid, min, max, gov);
            logs.add(String.format(Locale.US, "Policy %d (%s): Gov=%s, Min=%s, Max=%s",
                    pid, realPol.clusterLabel, gov, CpuTopologyDetector.formatFreq(min), CpuTopologyDetector.formatFreq(max)));
        }

        // 2. Revalidate and Apply CPU Online/Offline States
        for (Map.Entry<Integer, Boolean> entry : profile.coreOnlineMap.entrySet()) {
            int coreId = entry.getKey();
            boolean targetOnline = entry.getValue();

            // CPU0 is strictly protected
            if (coreId == 0) continue;

            boolean changed = CpuTopologyDetector.setCpuOnline(coreId, targetOnline);
            logs.add("CPU" + coreId + " set to " + (targetOnline ? "Online" : "Offline") + " (accepted: " + changed + ")");
        }

        // 3. GPU Tuning
        if (profile.gpuMaxFreqKhz > 0) {
            String f = String.valueOf(profile.gpuMaxFreqKhz);
            RootUtils.runCommand("for p in /sys/class/kgsl/kgsl-3d0/max_gpuclk /sys/class/kgsl/kgsl-3d0/devfreq/max_freq /sys/class/devfreq/*gpu*/max_freq; do [ -w \"$p\" ] && echo " + f + " > \"$p\"; done;");
            logs.add("GPU Max Frequency set to " + profile.gpuMaxFreqKhz);
        }

        // 4. Storage & I/O
        if (!profile.ioScheduler.isEmpty()) {
            RootUtils.runCommand("for p in /sys/block/*/queue/scheduler; do [ -w \"$p\" ] && echo '" + profile.ioScheduler + "' > \"$p\"; done;");
            logs.add("I/O Scheduler set to " + profile.ioScheduler);
        }
        if (profile.readAheadKb > 0) {
            RootUtils.runCommand("for p in /sys/block/*/queue/read_ahead_kb; do [ -w \"$p\" ] && echo " + profile.readAheadKb + " > \"$p\"; done;");
            logs.add("Read Ahead set to " + profile.readAheadKb + " KB");
        }

        // 5. Virtual Memory (Swappiness, VFS Cache, Dirty Ratio)
        if (profile.swappiness >= 0) {
            NativeKernelAccess.writeString("/proc/sys/vm/swappiness", String.valueOf(profile.swappiness));
            logs.add("Swappiness set to " + profile.swappiness + "%");
        }
        if (profile.vfsCachePressure >= 0) {
            NativeKernelAccess.writeString("/proc/sys/vm/vfs_cache_pressure", String.valueOf(profile.vfsCachePressure));
            logs.add("VFS Cache Pressure set to " + profile.vfsCachePressure);
        }
        if (profile.dirtyRatio >= 0) {
            NativeKernelAccess.writeString("/proc/sys/vm/dirty_ratio", String.valueOf(profile.dirtyRatio));
            logs.add("Dirty Ratio set to " + profile.dirtyRatio + "%");
        }

        // 6. Scheduler Boost & Caches
        if (profile.schedBoost) {
            NativeKernelAccess.writeString("/proc/sys/kernel/sched_boost", "1");
            logs.add("Sched Boost enabled");
        }
        if (profile.dropCaches) {
            RootUtils.runCommand("sync; [ -w /proc/sys/vm/drop_caches ] && echo 3 > /proc/sys/vm/drop_caches;");
            logs.add("Filesystem caches dropped");
        }
        if (profile.compactMemory) {
            RootUtils.runCommand("[ -w /proc/sys/vm/compact_memory ] && echo 1 > /proc/sys/vm/compact_memory;");
            logs.add("Memory compacted");
        }

        setActiveCustomProfileId(context, profile.id);
        return true;
    }

    public static String getActiveCustomProfileId(@NonNull Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_ACTIVE_ID, "");
    }

    public static void setActiveCustomProfileId(@NonNull Context context, @NonNull String id) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_ACTIVE_ID, id).apply();
    }

    public static void clearActiveCustomProfile(@NonNull Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().remove(KEY_ACTIVE_ID).apply();
    }

    private static long findNearestFrequency(@NonNull List<Long> available, long target) {
        if (available.isEmpty()) return target;
        long closest = available.get(0);
        long minDiff = Math.abs(closest - target);
        for (long f : available) {
            long diff = Math.abs(f - target);
            if (diff < minDiff) {
                minDiff = diff;
                closest = f;
            }
        }
        return closest;
    }

    private static synchronized void persistProfiles(@NonNull Context context, @NonNull List<CustomProfile> list) {
        try {
            JSONArray array = new JSONArray();
            for (CustomProfile p : list) {
                array.put(p.toJson());
            }
            File file = new File(context.getFilesDir(), FILE_NAME);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(array.toString(2).getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed writing custom profiles: " + e.getMessage());
        }
    }
}

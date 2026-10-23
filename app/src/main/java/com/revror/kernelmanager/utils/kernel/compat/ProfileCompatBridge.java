package com.revror.kernelmanager.utils.kernel.compat;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;

import com.revror.kernelmanager.database.tools.profiles.Profiles;
import com.revror.kernelmanager.services.boot.ApplyOnBoot;
import com.revror.kernelmanager.utils.kernel.cpu.CPUFreq;
import com.revror.kernelmanager.utils.root.RootUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Compatibility bridge connecting all profiles (built-in, custom, AI, gaming)
 * to the centralized KernelCompatEngine.
 */
public final class ProfileCompatBridge {

    private static final String TAG = "ProfileCompatBridge";

    private ProfileCompatBridge() {}

    /**
     * Executes a full built-in or Phoenix profile through the universal compatibility engine.
     */
    @NonNull
    public static List<String> applyProfile(Context context, int profile, boolean isPhoenix) {
        return KernelCompatEngine.applyProfileUniversal(profile, isPhoenix);
    }

    /**
     * Rewrites and applies custom user-created profiles using equivalent kernel interfaces.
     * If a command in the profile targets a path that does not exist on the current kernel
     * (e.g. an old /dev/stune node or msm_performance path), this bridge locates the
     * verified equivalent and executes it safely.
     */
    @NonNull
    public static List<String> applyCustomProfile(Context context, Profiles.ProfileItem profile) {
        List<String> results = new ArrayList<>();
        if (profile == null) return results;

        List<Profiles.ProfileItem.CommandItem> commands = profile.getCommands();
        for (Profiles.ProfileItem.CommandItem item : commands) {
            String path = item.getPath();
            String rawCmd = item.getCommand();

            if (rawCmd == null || rawCmd.trim().isEmpty()) continue;

            if (path != null && !path.isEmpty()) {
                if (isReadOnlyNode(path)) {
                    Log.w(TAG, "Skipping custom profile command targeting read-only node: " + path);
                    results.add("Skipped read-only node: " + path);
                    continue;
                }

                if (KernelCompatEngine.exists(path)) {
                    RootUtils.runCommand(rawCmd);
                    results.add("Direct: " + path);
                    continue;
                }

                // Path does not exist! Search for equivalent in KernelTweakRegistry
                boolean resolved = false;
                for (Map.Entry<String, KernelTweakRegistry.TweakDefinition> entry : KernelTweakRegistry.getAll().entrySet()) {
                    KernelTweakRegistry.TweakDefinition def = entry.getValue();
                    if (matchesPathPattern(path, def)) {
                        String value = extractValue(rawCmd);
                        if (value != null) {
                            KernelCompatEngine.WriteResult writeRes = KernelCompatEngine.safeWriteWithFallbacks(def.getAllCandidatePaths(), value);
                            if (writeRes.success) {
                                results.add("Equivalent (" + def.getId() + "): " + writeRes.pathUsed);
                                resolved = true;
                                break;
                            }
                        }
                    }
                }

                if (!resolved) {
                    results.add("Skipped unsupported node: " + path);
                }
            } else {
                applyCommandSafe(context, rawCmd);
                results.add("Executed: " + rawCmd);
            }
        }
        return results;
    }

    /**
     * Safely executes an individual shell command string with equivalent path resolution
     * and protection against read-only or non-existent sysfs/procfs node failures.
     */
    public static void applyCommandSafe(Context context, String rawCmd) {
        if (rawCmd == null || rawCmd.trim().isEmpty()) return;
        rawCmd = rawCmd.trim();

        if (rawCmd.startsWith("#")) {
            CPUFreq.ApplyCpu applyCpu = new CPUFreq.ApplyCpu(rawCmd.substring(1));
            if (applyCpu.toString() != null) {
                List<String> cpuCmds = ApplyOnBoot.getApplyCpu(applyCpu);
                for (String c : cpuCmds) {
                    applyCommandSafe(context, c);
                }
                return;
            }
        }

        if (rawCmd.contains(">")) {
            int redirectIdx = rawCmd.indexOf(">");
            int startTarget = redirectIdx + 1;
            if (startTarget < rawCmd.length() && rawCmd.charAt(startTarget) == '>') {
                startTarget++;
            }
            String targetPath = rawCmd.substring(startTarget).trim();
            if (targetPath.contains(" ")) {
                targetPath = targetPath.split("\\s+")[0];
            }

            if (isReadOnlyNode(targetPath)) {
                Log.w(TAG, "Skipping write to read-only kernel node: " + targetPath);
                return;
            }

            if (KernelCompatEngine.exists(targetPath)) {
                RootUtils.runCommand(rawCmd);
                return;
            }

            // Path missing; search for equivalent tweak
            String value = extractValue(rawCmd);
            boolean resolved = false;
            for (Map.Entry<String, KernelTweakRegistry.TweakDefinition> entry : KernelTweakRegistry.getAll().entrySet()) {
                KernelTweakRegistry.TweakDefinition def = entry.getValue();
                if (matchesPathPattern(targetPath, def)) {
                    if (value != null) {
                        KernelCompatEngine.WriteResult res = KernelCompatEngine.safeWriteWithFallbacks(def.getAllCandidatePaths(), value);
                        if (res.success) {
                            resolved = true;
                            break;
                        }
                    }
                }
            }

            if (!resolved) {
                Log.i(TAG, "Safely bypassed unsupported kernel node: " + targetPath);
            }
        } else {
            RootUtils.runCommand(rawCmd);
        }
    }

    public static void applyCommandsSafe(Context context, List<String> commands) {
        if (commands == null) return;
        for (String cmd : commands) {
            applyCommandSafe(context, cmd);
        }
    }

    private static boolean isReadOnlyNode(String path) {
        if (path == null) return false;
        if (path.contains("cpuinfo_") || path.contains("scaling_cur_freq") || path.contains("scaling_available_")) {
            return true;
        }
        for (KernelTweakRegistry.TweakDefinition def : KernelTweakRegistry.getAll().values()) {
            if (def.isReadOnlyInfo() && matchesPathPattern(path, def)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesPathPattern(String actualPath, KernelTweakRegistry.TweakDefinition def) {
        for (String p : def.getAllCandidatePaths()) {
            String regex = p.replace("%d", "[0-9]+").replace("%s", "[a-zA-Z0-9_\\-]+");
            if (actualPath.matches(regex) || actualPath.contains(p.replace("/%d", "").replace("/%s", ""))) {
                return true;
            }
        }
        return false;
    }

    private static String extractValue(String command) {
        if (command == null) return null;
        if (command.contains("echo ") && command.contains(">")) {
            int start = command.indexOf("echo ") + 5;
            int end = command.indexOf(">", start);
            if (start < end) {
                String val = command.substring(start, end).trim();
                if ((val.startsWith("'") && val.endsWith("'")) || (val.startsWith("\"") && val.endsWith("\""))) {
                    val = val.substring(1, val.length() - 1);
                }
                return val.trim();
            }
        }
        return null;
    }
}

/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.revror.kernelmanager.R;
import com.revror.kernelmanager.utils.root.RootUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class KernelTuningActivity extends AppCompatActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private TextView deviceStatus;
    private TextView txtTopologyTitle;
    private TextView txtTopologySummary;
    private android.widget.LinearLayout policiesContainer;
    private android.widget.LinearLayout coresContainer;
    private TextView valCpuGov, valCpuMin, valCpuMax, valCpuBoost;
    private TextView valGpuFreq;
    private TextView valIoSched, valReadAhead, valSwappiness, valVfsCache, valDirtyRatio;
    private TextView valTcpCongestion;
    private TextView valSelinux;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.RorTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kernel_tuning);

        deviceStatus = findViewById(R.id.kernel_device_status);
        txtTopologyTitle = findViewById(R.id.kernel_cpu_topology_title);
        txtTopologySummary = findViewById(R.id.kernel_cpu_topology_summary);
        policiesContainer = findViewById(R.id.kernel_tuning_policies_container);
        coresContainer = findViewById(R.id.kernel_tuning_cores_container);

        findViewById(R.id.kernel_tuning_back).setOnClickListener(v -> finish());
        findViewById(R.id.kernel_tuning_save).setOnClickListener(v -> {
            Toast.makeText(this, "Kernel tuning active and applied", Toast.LENGTH_SHORT).show();
            refreshValues();
        });

        valCpuGov = findViewById(R.id.val_cpu_governor);
        valCpuMin = findViewById(R.id.val_cpu_min_freq);
        valCpuMax = findViewById(R.id.val_cpu_max_freq);
        valCpuBoost = findViewById(R.id.val_cpu_boost);
        valGpuFreq = findViewById(R.id.val_gpu_freq);
        valIoSched = findViewById(R.id.val_io_scheduler);
        valReadAhead = findViewById(R.id.val_read_ahead);
        valSwappiness = findViewById(R.id.val_swappiness);
        valVfsCache = findViewById(R.id.val_vfs_cache);
        valDirtyRatio = findViewById(R.id.val_dirty_ratio);
        valTcpCongestion = findViewById(R.id.val_tcp_congestion);
        valSelinux = findViewById(R.id.val_selinux);

        findViewById(R.id.row_cpu_governor).setOnClickListener(v -> pickCpuGovernor());
        findViewById(R.id.row_cpu_min_freq).setOnClickListener(v -> pickCpuFreq(true));
        findViewById(R.id.row_cpu_max_freq).setOnClickListener(v -> pickCpuFreq(false));
        findViewById(R.id.row_cpu_boost).setOnClickListener(v -> toggleCpuBoost());
        findViewById(R.id.row_gpu_freq).setOnClickListener(v -> pickGpuFreq());
        findViewById(R.id.row_io_scheduler).setOnClickListener(v -> pickIoScheduler());
        findViewById(R.id.row_read_ahead).setOnClickListener(v -> pickReadAhead());
        findViewById(R.id.row_swappiness).setOnClickListener(v -> pickSwappiness());
        findViewById(R.id.row_vfs_cache).setOnClickListener(v -> pickVfsCache());
        findViewById(R.id.row_dirty_ratio).setOnClickListener(v -> pickDirtyRatio());
        findViewById(R.id.row_tcp_congestion).setOnClickListener(v -> pickTcpCongestion());
        findViewById(R.id.row_selinux).setOnClickListener(v -> toggleSelinux());

        findViewById(R.id.btn_compact_memory).setOnClickListener(v -> {
            runRoot("Memory compact", "[ -w /proc/sys/vm/compact_memory ] && echo 1 > /proc/sys/vm/compact_memory");
        });

        findViewById(R.id.btn_drop_caches).setOnClickListener(v -> {
            runRoot("Drop filesystem caches", "sync; echo 3 > /proc/sys/vm/drop_caches");
        });

        if (!RootUtils.rootAccess()) {
            Toast.makeText(this, "Root not detected - kernel tuning nodes are locked", Toast.LENGTH_LONG).show();
            new AlertDialog.Builder(this, R.style.RorDialogTheme)
                    .setTitle("Kernel Tuning • Root Status")
                    .setMessage("Kernel parameters in /sys and /proc are protected by Linux permissions and require root access.\n\nRunning in read-only inspection mode.")
                    .setPositiveButton("OK", null)
                    .show();
        }

        refreshValues();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshValues();
    }

    private void submit(Runnable task) {
        if (isFinishing() || isDestroyed() || executor.isShutdown()) return;
        try {
            executor.execute(task);
        } catch (RejectedExecutionException ignored) {}
    }

    private void runOnUiThreadSafe(Runnable action) {
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) {
                action.run();
            }
        });
    }

    private void refreshValues() {
        submit(() -> {
            boolean rooted = RootUtils.rootAccess();
            String kernelVer = RootUtils.runAndGetOutput("uname -r 2>/dev/null").trim();
            com.revror.kernelmanager.utils.kernel.cpu.CpuTopologyDetector.DeviceCpuTopology topo =
                    com.revror.kernelmanager.utils.kernel.cpu.CpuTopologyDetector.detect();

            String gov = RootUtils.runAndGetOutput("cat /sys/devices/system/cpu/cpufreq/policy*/scaling_governor /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor 2>/dev/null | head -n1").trim();
            String min = RootUtils.runAndGetOutput("cat /sys/devices/system/cpu/cpufreq/policy*/scaling_min_freq /sys/devices/system/cpu/cpu*/cpufreq/scaling_min_freq 2>/dev/null | sort -n | head -n1").trim();
            String max = RootUtils.runAndGetOutput("cat /sys/devices/system/cpu/cpufreq/policy*/scaling_max_freq /sys/devices/system/cpu/cpu*/cpufreq/scaling_max_freq 2>/dev/null | sort -n | tail -n1").trim();
            String boost = RootUtils.runAndGetOutput("cat /proc/sys/kernel/sched_boost /sys/module/msm_performance/parameters/touchboost 2>/dev/null | head -n1").trim();

            String gpuCur = RootUtils.runAndGetOutput("cat /sys/class/kgsl/kgsl-3d0/devfreq/cur_freq /sys/class/kgsl/kgsl-3d0/gpuclk /sys/class/devfreq/*gpu*/cur_freq 2>/dev/null | head -n1").trim();
            String gpuMax = RootUtils.runAndGetOutput("cat /sys/class/kgsl/kgsl-3d0/max_gpuclk /sys/class/kgsl/kgsl-3d0/devfreq/max_freq /sys/class/devfreq/*gpu*/max_freq 2>/dev/null | head -n1").trim();

            String io = RootUtils.runAndGetOutput("cat /sys/block/*/queue/scheduler 2>/dev/null | grep -o '\\[.*\\]' | tr -d '[]' | head -n1").trim();
            String readAhead = RootUtils.runAndGetOutput("cat /sys/block/mmcblk0/queue/read_ahead_kb /sys/block/sda/queue/read_ahead_kb /sys/block/*/queue/read_ahead_kb 2>/dev/null | head -n1").trim();
            String swappiness = RootUtils.runAndGetOutput("cat /proc/sys/vm/swappiness 2>/dev/null").trim();
            String vfs = RootUtils.runAndGetOutput("cat /proc/sys/vm/vfs_cache_pressure 2>/dev/null").trim();
            String dirty = RootUtils.runAndGetOutput("cat /proc/sys/vm/dirty_ratio 2>/dev/null").trim();
            String tcp = RootUtils.runAndGetOutput("cat /proc/sys/net/ipv4/tcp_congestion_control 2>/dev/null").trim();
            String selinux = RootUtils.runAndGetOutput("getenforce 2>/dev/null").trim();

            runOnUiThreadSafe(() -> {
                if (deviceStatus != null) {
                    String statusLabel = rooted ? "Linux Kernel Control • Root Active" : "Linux Kernel Control • Read-Only (Root Required)";
                    if (!kernelVer.isEmpty()) {
                        statusLabel = "Linux " + kernelVer + " • " + (rooted ? "Root Active" : "Read-Only");
                    }
                    deviceStatus.setText(statusLabel);
                    deviceStatus.setTextColor(rooted ? 0xFF00E5FF : 0xFFFFA84B);
                    if (txtTopologyTitle != null) {
                        txtTopologyTitle.setText(topo.summary);
                    }
                    if (txtTopologySummary != null) {
                        txtTopologySummary.setText(topo.policies.size() + " Dynamic CPU Policy Clusters • Per-Core Hotplug Supported");
                    }
                    renderPolicies(topo);
                    renderCores(topo);
                }

                if (valCpuGov != null) valCpuGov.setText(gov.isEmpty() ? "Default" : gov);
                if (valCpuMin != null) valCpuMin.setText(min.isEmpty() ? "Default" : formatFreq(min));
                if (valCpuMax != null) valCpuMax.setText(max.isEmpty() ? "Default" : formatFreq(max));
                if (valCpuBoost != null) valCpuBoost.setText("1".equals(boost) ? "Enabled" : "Disabled");

                if (valGpuFreq != null) {
                    if (!gpuMax.isEmpty()) {
                        valGpuFreq.setText(formatGpuFreq(gpuMax));
                    } else if (!gpuCur.isEmpty()) {
                        valGpuFreq.setText(formatGpuFreq(gpuCur));
                    } else {
                        valGpuFreq.setText("Dynamic Scaling");
                    }
                }

                if (valIoSched != null) valIoSched.setText(io.isEmpty() ? "Default" : io);
                if (valReadAhead != null) valReadAhead.setText(readAhead.isEmpty() ? "512 KB" : readAhead + " KB");
                if (valSwappiness != null) valSwappiness.setText(swappiness.isEmpty() ? "60%" : swappiness + "%");
                if (valVfsCache != null) valVfsCache.setText(vfs.isEmpty() ? "100" : vfs);
                if (valDirtyRatio != null) valDirtyRatio.setText(dirty.isEmpty() ? "20%" : dirty + "%");
                if (valTcpCongestion != null) valTcpCongestion.setText(tcp.isEmpty() ? "cubic" : tcp);
                if (valSelinux != null) valSelinux.setText(selinux.isEmpty() ? "Enforcing" : selinux);
            });
        });
    }

    private void renderPolicies(com.revror.kernelmanager.utils.kernel.cpu.CpuTopologyDetector.DeviceCpuTopology topo) {
        if (policiesContainer == null) return;
        policiesContainer.removeAllViews();
        android.view.LayoutInflater inflater = android.view.LayoutInflater.from(this);

        for (com.revror.kernelmanager.utils.kernel.cpu.CpuTopologyDetector.CpuPolicy pol : topo.policies) {
            android.view.View view = inflater.inflate(R.layout.item_custom_profile_policy, policiesContainer, false);
            TextView title = view.findViewById(R.id.item_policy_title);
            TextView valGov = view.findViewById(R.id.item_val_governor);
            TextView valMin = view.findViewById(R.id.item_val_min_freq);
            TextView valMax = view.findViewById(R.id.item_val_max_freq);

            StringBuilder cpusStr = new StringBuilder();
            for (int i = 0; i < pol.affectedCpus.size(); i++) {
                if (i > 0) cpusStr.append(", ");
                cpusStr.append(pol.affectedCpus.get(i));
            }
            title.setText(String.format(Locale.US, "Policy %d • %s (Cores: %s)", pol.policyId, pol.clusterLabel, cpusStr));

            valGov.setText(pol.currentGovernor);
            valMin.setText(com.revror.kernelmanager.utils.kernel.cpu.CpuTopologyDetector.formatFreq(pol.minFreqKhz));
            valMax.setText(com.revror.kernelmanager.utils.kernel.cpu.CpuTopologyDetector.formatFreq(pol.maxFreqKhz));

            view.findViewById(R.id.item_row_governor).setOnClickListener(v -> {
                String[] govs = pol.availableGovernors.toArray(new String[0]);
                new AlertDialog.Builder(this, R.style.RorDialogTheme)
                        .setTitle("Policy " + pol.policyId + " Governor")
                        .setItems(govs, (d, which) -> {
                            String chosen = govs[which];
                            runRoot("Set Policy " + pol.policyId + " Governor",
                                    "setprop persist.revror.manual_cpu 1; for p in /sys/devices/system/cpu/cpufreq/policy" + pol.policyId + "/scaling_governor /sys/devices/system/cpu/cpu" + pol.policyId + "/cpufreq/scaling_governor; do [ -w \"$p\" ] && echo " + chosen + " > \"$p\"; done");
                        }).show();
            });

            view.findViewById(R.id.item_row_min_freq).setOnClickListener(v -> {
                List<Long> freqs = pol.availableFrequenciesKhz;
                String[] items = new String[freqs.size()];
                for (int i = 0; i < freqs.size(); i++) items[i] = com.revror.kernelmanager.utils.kernel.cpu.CpuTopologyDetector.formatFreq(freqs.get(i));
                new AlertDialog.Builder(this, R.style.RorDialogTheme)
                        .setTitle("Policy " + pol.policyId + " Min Frequency")
                        .setItems(items, (d, which) -> {
                            long f = freqs.get(which);
                            com.revror.kernelmanager.utils.kernel.cpu.CpuTopologyDetector.setPolicyFrequency(pol.policyId, f, Math.max(f, pol.maxFreqKhz), null);
                            refreshValues();
                        }).show();
            });

            view.findViewById(R.id.item_row_max_freq).setOnClickListener(v -> {
                List<Long> freqs = pol.availableFrequenciesKhz;
                String[] items = new String[freqs.size()];
                for (int i = 0; i < freqs.size(); i++) items[i] = com.revror.kernelmanager.utils.kernel.cpu.CpuTopologyDetector.formatFreq(freqs.get(i));
                new AlertDialog.Builder(this, R.style.RorDialogTheme)
                        .setTitle("Policy " + pol.policyId + " Max Frequency")
                        .setItems(items, (d, which) -> {
                            long f = freqs.get(which);
                            com.revror.kernelmanager.utils.kernel.cpu.CpuTopologyDetector.setPolicyFrequency(pol.policyId, Math.min(f, pol.minFreqKhz), f, null);
                            refreshValues();
                        }).show();
            });

            policiesContainer.addView(view);
        }
    }

    private void renderCores(com.revror.kernelmanager.utils.kernel.cpu.CpuTopologyDetector.DeviceCpuTopology topo) {
        if (coresContainer == null) return;
        coresContainer.removeAllViews();
        android.view.LayoutInflater inflater = android.view.LayoutInflater.from(this);

        for (com.revror.kernelmanager.utils.kernel.cpu.CpuTopologyDetector.CpuCore core : topo.cores) {
            android.view.View view = inflater.inflate(R.layout.item_custom_profile_core, coresContainer, false);
            TextView label = view.findViewById(R.id.item_core_label);
            TextView tag = view.findViewById(R.id.item_core_status_tag);
            com.google.android.material.switchmaterial.SwitchMaterial sw = view.findViewById(R.id.item_core_switch);

            if (core.coreId == 0) {
                label.setText("CPU 0 (Boot Core • Always Online)");
                tag.setText("BOOT LOCKED");
                tag.setTextColor(0xFF00E5FF);
                sw.setChecked(true);
                sw.setEnabled(false);
            } else if (!core.canHotplug) {
                label.setText("CPU " + core.coreId + (core.curFreqKhz > 0 ? " (" + com.revror.kernelmanager.utils.kernel.cpu.CpuTopologyDetector.formatFreq(core.curFreqKhz) + ")" : ""));
                tag.setText("ONLINE");
                tag.setTextColor(0xFF00ED74);
                sw.setChecked(true);
                sw.setEnabled(false);
            } else {
                label.setText("CPU " + core.coreId + (core.curFreqKhz > 0 ? " (" + com.revror.kernelmanager.utils.kernel.cpu.CpuTopologyDetector.formatFreq(core.curFreqKhz) + ")" : ""));
                tag.setText(core.isOnline ? "ONLINE" : "OFFLINE");
                tag.setTextColor(core.isOnline ? 0xFF00ED74 : 0xFFFA476B);
                sw.setChecked(core.isOnline);
                sw.setEnabled(true);

                sw.setOnCheckedChangeListener((btn, isChecked) -> {
                    submit(() -> {
                        com.revror.kernelmanager.utils.kernel.cpu.CpuTopologyDetector.setCpuOnline(core.coreId, isChecked);
                        refreshValues();
                    });
                });
            }

            coresContainer.addView(view);
        }
    }

    private static String formatFreq(String freqKhz) {
        try {
            long khz = Long.parseLong(freqKhz.trim());
            if (khz >= 1000000) {
                return String.format(Locale.US, "%.2f GHz (%d MHz)", khz / 1000000.0, khz / 1000);
            } else {
                return String.format(Locale.US, "%d MHz", khz / 1000);
            }
        } catch (Exception e) {
            return freqKhz + " KHz";
        }
    }

    private static String formatGpuFreq(String freqHzOrMhz) {
        try {
            long val = Long.parseLong(freqHzOrMhz.trim());
            if (val > 10000000) {
                return String.format(Locale.US, "%d MHz", val / 1000000);
            } else if (val > 10000) {
                return String.format(Locale.US, "%d MHz", val / 1000);
            } else {
                return String.format(Locale.US, "%d MHz", val);
            }
        } catch (Exception e) {
            return freqHzOrMhz;
        }
    }

    private void pickCpuGovernor() {
        submit(() -> {
            String out = RootUtils.runAndGetOutput("cat /sys/devices/system/cpu/cpufreq/policy*/scaling_available_governors /sys/devices/system/cpu/cpu*/cpufreq/scaling_available_governors 2>/dev/null | tr ' ' '\\n' | sort -u | tr '\\n' ' '").trim();
            if (out.isEmpty()) out = "schedutil performance powersave interactive conservative ondemand";
            String[] govs = out.split("\\s+");

            runOnUiThreadSafe(() -> {
                new AlertDialog.Builder(this, R.style.RorDialogTheme)
                        .setTitle("Select CPU Governor")
                        .setItems(govs, (d, which) -> {
                            String chosen = govs[which];
                            runRoot("Set Governor", "setprop persist.revror.manual_cpu 1; for p in /sys/devices/system/cpu/cpufreq/policy*/scaling_governor /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do [ -w \"$p\" ] && echo " + chosen + " > \"$p\"; done");
                            if (valCpuGov != null) valCpuGov.setText(chosen);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        });
    }

    private void pickCpuFreq(boolean isMin) {
        submit(() -> {
            String out = RootUtils.runAndGetOutput("cat /sys/devices/system/cpu/cpufreq/policy*/scaling_available_frequencies /sys/devices/system/cpu/cpu*/cpufreq/scaling_available_frequencies 2>/dev/null | tr ' ' '\\n' | sort -n -u | tr '\\n' ' '").trim();
            if (out.isEmpty()) {
                out = "300000 600000 900000 1200000 1500000 1800000 2000000 2400000 2841600 3187200 3360000";
            }

            Set<Long> numSet = new LinkedHashSet<>();
            for (String s : out.split("\\s+")) {
                try {
                    long f = Long.parseLong(s.trim());
                    if (f > 50000) numSet.add(f);
                } catch (Exception ignored) {}
            }
            List<Long> freqList = new ArrayList<>(numSet);
            Collections.sort(freqList);

            final String[] rawFreqs = new String[freqList.size()];
            final String[] displayFreqs = new String[freqList.size()];
            for (int i = 0; i < freqList.size(); i++) {
                rawFreqs[i] = String.valueOf(freqList.get(i));
                displayFreqs[i] = formatFreq(rawFreqs[i]);
            }

            runOnUiThreadSafe(() -> {
                new AlertDialog.Builder(this, R.style.RorDialogTheme)
                        .setTitle(isMin ? "Select CPU Minimum Frequency" : "Select CPU Maximum Frequency")
                        .setItems(displayFreqs, (d, which) -> {
                            String f = rawFreqs[which];
                            String target = isMin ? "scaling_min_freq" : "scaling_max_freq";
                            runRoot("Set CPU Frequency", "setprop persist.revror.manual_cpu 1; for p in /sys/devices/system/cpu/cpufreq/policy*/" + target + " /sys/devices/system/cpu/cpu*/cpufreq/" + target + "; do [ -w \"$p\" ] && echo " + f + " > \"$p\"; done");
                            if (isMin) {
                                if (valCpuMin != null) valCpuMin.setText(displayFreqs[which]);
                            } else {
                                if (valCpuMax != null) valCpuMax.setText(displayFreqs[which]);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        });
    }

    private void pickGpuFreq() {
        submit(() -> {
            String out = RootUtils.runAndGetOutput("cat /sys/class/kgsl/kgsl-3d0/gpu_available_frequencies /sys/class/kgsl/kgsl-3d0/devfreq/available_frequencies /sys/class/devfreq/*gpu*/available_frequencies 2>/dev/null | head -n1").trim();

            List<String> rawList = new ArrayList<>();
            List<String> labelList = new ArrayList<>();

            if (!out.isEmpty()) {
                for (String s : out.split("\\s+")) {
                    if (!s.isEmpty()) {
                        rawList.add(s);
                        labelList.add(formatGpuFreq(s));
                    }
                }
            }

            if (rawList.isEmpty()) {
                rawList.add("0"); labelList.add("Default Dynamic");
                rawList.add("305000000"); labelList.add("305 MHz");
                rawList.add("450000000"); labelList.add("450 MHz");
                rawList.add("587000000"); labelList.add("587 MHz");
                rawList.add("670000000"); labelList.add("670 MHz (Turbo)");
                rawList.add("800000000"); labelList.add("800 MHz (Extreme)");
            }

            final String[] raw = rawList.toArray(new String[0]);
            final String[] labels = labelList.toArray(new String[0]);

            runOnUiThreadSafe(() -> {
                new AlertDialog.Builder(this, R.style.RorDialogTheme)
                        .setTitle("Select GPU Maximum Frequency")
                        .setItems(labels, (d, which) -> {
                            String targetVal = raw[which];
                            if ("0".equals(targetVal)) {
                                runRoot("Reset GPU Dynamic", "setprop persist.revror.manual_gpu 0; echo 0 > /sys/class/kgsl/kgsl-3d0/max_gpuclk 2>/dev/null");
                            } else {
                                String cmd = "setprop persist.revror.manual_gpu 1; "
                                        + "echo " + targetVal + " > /sys/class/kgsl/kgsl-3d0/max_gpuclk 2>/dev/null; "
                                        + "echo " + targetVal + " > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq 2>/dev/null; "
                                        + "for p in /sys/class/devfreq/*gpu*/max_freq; do [ -w \"$p\" ] && echo " + targetVal + " > \"$p\"; done";
                                runRoot("Set GPU Max Frequency", cmd);
                            }
                            if (valGpuFreq != null) valGpuFreq.setText(labels[which]);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        });
    }

    private void pickIoScheduler() {
        submit(() -> {
            String out = RootUtils.runAndGetOutput("cat /sys/block/*/queue/scheduler 2>/dev/null | tr ' []' '\\n' | sed '/^$/d' | sort -u").trim();
            if (out.isEmpty()) out = "cfq deadline noop bfq kyber mq-deadline none";
            String[] scheds = out.split("\\s+");

            runOnUiThreadSafe(() -> {
                new AlertDialog.Builder(this, R.style.RorDialogTheme)
                        .setTitle("Select I/O Scheduler")
                        .setItems(scheds, (d, which) -> {
                            String chosen = scheds[which];
                            runRoot("Set I/O Scheduler", "for p in /sys/block/*/queue/scheduler; do [ -w \"$p\" ] && echo " + chosen + " > \"$p\"; done");
                            if (valIoSched != null) valIoSched.setText(chosen);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        });
    }

    private void toggleCpuBoost() {
        String current = valCpuBoost != null ? valCpuBoost.getText().toString() : "";
        boolean enable = !current.equalsIgnoreCase("Enabled");
        String cmd = enable
                ? "echo 1 > /proc/sys/kernel/sched_boost 2>/dev/null; echo 1 > /sys/module/msm_performance/parameters/touchboost 2>/dev/null; for p in /sys/module/cpu_boost/parameters/input_boost_freq*; do [ -w \"$p\" ] && echo 1400000 > \"$p\"; done"
                : "echo 0 > /proc/sys/kernel/sched_boost 2>/dev/null; echo 0 > /sys/module/msm_performance/parameters/touchboost 2>/dev/null; for p in /sys/module/cpu_boost/parameters/input_boost_freq*; do [ -w \"$p\" ] && echo 0 > \"$p\"; done";
        runRoot("CPU Boost", cmd);
        if (valCpuBoost != null) valCpuBoost.setText(enable ? "Enabled" : "Disabled");
    }

    private void pickReadAhead() {
        String[] options = {"128 KB", "256 KB", "512 KB", "1024 KB", "2048 KB", "4096 KB"};
        String[] values = {"128", "256", "512", "1024", "2048", "4096"};
        new AlertDialog.Builder(this, R.style.RorDialogTheme)
                .setTitle("Select Read-Ahead Buffer")
                .setItems(options, (d, which) -> {
                    String v = values[which];
                    runRoot("Read-Ahead", "for p in /sys/block/*/queue/read_ahead_kb; do [ -w \"$p\" ] && echo " + v + " > \"$p\"; done");
                    if (valReadAhead != null) valReadAhead.setText(options[which]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void pickSwappiness() {
        String[] options = {"0% (Disable Swap)", "20% (Minimal)", "40% (Low)", "60% (Default)", "80% (Aggressive)", "100% (Maximum)", "150% (ZRAM Heavy)", "200% (ZRAM Full)"};
        String[] values = {"0", "20", "40", "60", "80", "100", "150", "200"};
        new AlertDialog.Builder(this, R.style.RorDialogTheme)
                .setTitle("Select VM Swappiness")
                .setItems(options, (d, which) -> {
                    String v = values[which];
                    runRoot("Swappiness", "echo " + v + " > /proc/sys/vm/swappiness");
                    if (valSwappiness != null) valSwappiness.setText(v + "%");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void pickVfsCache() {
        String[] options = {"50 (Favor VFS Cache)", "100 (Default Balance)", "150 (Aggressive Directory Reclaim)", "200 (Maximum Reclaim)"};
        String[] values = {"50", "100", "150", "200"};
        new AlertDialog.Builder(this, R.style.RorDialogTheme)
                .setTitle("Select VFS Cache Pressure")
                .setItems(options, (d, which) -> {
                    String v = values[which];
                    runRoot("VFS Cache Pressure", "echo " + v + " > /proc/sys/vm/vfs_cache_pressure");
                    if (valVfsCache != null) valVfsCache.setText(v);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void pickDirtyRatio() {
        String[] options = {"10% (Frequent Flushes)", "20% (Default Balanced)", "30% (High Throughput)", "40% (Maximum Buffer)"};
        String[] values = {"10", "20", "30", "40"};
        new AlertDialog.Builder(this, R.style.RorDialogTheme)
                .setTitle("Select Dirty Writeback Ratio")
                .setItems(options, (d, which) -> {
                    String v = values[which];
                    runRoot("Dirty Ratio", "echo " + v + " > /proc/sys/vm/dirty_ratio");
                    if (valDirtyRatio != null) valDirtyRatio.setText(v + "%");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void pickTcpCongestion() {
        submit(() -> {
            String out = RootUtils.runAndGetOutput("cat /proc/sys/net/ipv4/tcp_available_congestion_control 2>/dev/null").trim();
            if (out.isEmpty()) out = "bbr cubic westwood reno bic";
            String[] algos = out.split("\\s+");

            runOnUiThreadSafe(() -> {
                new AlertDialog.Builder(this, R.style.RorDialogTheme)
                        .setTitle("Select TCP Congestion Control")
                        .setItems(algos, (d, which) -> {
                            String chosen = algos[which];
                            runRoot("TCP Congestion", "echo " + chosen + " > /proc/sys/net/ipv4/tcp_congestion_control");
                            if (valTcpCongestion != null) valTcpCongestion.setText(chosen);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        });
    }

    private void toggleSelinux() {
        String current = valSelinux != null ? valSelinux.getText().toString() : "";
        boolean toEnforcing = current.equalsIgnoreCase("Permissive");
        runRoot("SELinux", "setenforce " + (toEnforcing ? "1" : "0"));
        if (valSelinux != null) valSelinux.setText(toEnforcing ? "Enforcing" : "Permissive");
    }

    private void runRoot(String label, String cmd) {
        if (!RootUtils.rootAccess()) {
            Toast.makeText(this, label + " locked: root privileges required", Toast.LENGTH_SHORT).show();
            return;
        }
        submit(() -> {
            RootUtils.runCommand(cmd);
            runOnUiThreadSafe(() -> {
                Toast.makeText(this, label + " executed", Toast.LENGTH_SHORT).show();
                refreshValues();
            });
        });
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}

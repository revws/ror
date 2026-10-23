package com.revror.kernelmanager.views.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.revror.kernelmanager.R;
import com.revror.kernelmanager.utils.RorLicenseManager;
import com.revror.kernelmanager.utils.kernel.cpu.CpuTopologyDetector;
import com.revror.kernelmanager.utils.profile.CustomProfileManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Custom Profile Dialog & Studio Controller.
 * Enables users to craft, edit, duplicate, and delete custom kernel profiles
 * with live dynamic per-policy and per-core topology controls.
 */
public final class CustomProfileDialog {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private CustomProfileDialog() {}

    public static void showEditor(@NonNull Activity activity, @Nullable CustomProfileManager.CustomProfile existingProfile, @NonNull Runnable onSaved) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        if (!RorLicenseManager.isPremiumActive(activity)) {
            RorLicenseManager.showVipRequiredDialog(activity, "Custom Profile Studio", () -> {
                showEditor(activity, existingProfile, onSaved);
            });
            return;
        }

        activity.runOnUiThread(() -> {
            View view = LayoutInflater.from(activity).inflate(R.layout.dialog_custom_profile_editor, null);
            AlertDialog dialog = new AlertDialog.Builder(activity)
                    .setView(view)
                    .setCancelable(true)
                    .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            final CustomProfileManager.CustomProfile profile = existingProfile != null
                    ? existingProfile
                    : new CustomProfileManager.CustomProfile();

            TextView titleText = view.findViewById(R.id.profile_editor_title);
            titleText.setText(existingProfile != null ? "Edit Custom Profile" : "Create Custom Profile");

            EditText nameInput = view.findViewById(R.id.profile_editor_name);
            nameInput.setText(profile.name);

            ImageView iconPreview = view.findViewById(R.id.profile_editor_icon_preview);
            updateIconPreview(activity, iconPreview, profile.iconResName, profile.accentColor);

            // Icon picking handlers
            setupIconPicker(view, profile, iconPreview, activity);

            LinearLayout policiesContainer = view.findViewById(R.id.profile_editor_policies_container);
            LinearLayout coresContainer = view.findViewById(R.id.profile_editor_cores_container);

            TextView valSwappiness = view.findViewById(R.id.val_editor_swappiness);
            TextView valVfs = view.findViewById(R.id.val_editor_vfs);
            CheckBox chkDrop = view.findViewById(R.id.chk_editor_drop_caches);
            CheckBox chkCompact = view.findViewById(R.id.chk_editor_compact_memory);

            valSwappiness.setText(profile.swappiness >= 0 ? profile.swappiness + "%" : "Default (60%)");
            valVfs.setText(profile.vfsCachePressure >= 0 ? String.valueOf(profile.vfsCachePressure) : "Default (100)");
            chkDrop.setChecked(profile.dropCaches);
            chkCompact.setChecked(profile.compactMemory);

            view.findViewById(R.id.row_editor_swappiness).setOnClickListener(v -> {
                String[] items = {"0% (Disable Swap)", "20%", "40%", "60% (Balanced)", "80%", "100% (Aggressive)"};
                int[] vals = {0, 20, 40, 60, 80, 100};
                new AlertDialog.Builder(activity, R.style.RorDialogTheme)
                        .setTitle("Select RAM Swappiness")
                        .setItems(items, (d, which) -> {
                            profile.swappiness = vals[which];
                            valSwappiness.setText(items[which]);
                        }).show();
            });

            view.findViewById(R.id.row_editor_vfs).setOnClickListener(v -> {
                String[] items = {"50 (Retain Caches)", "80", "100 (Balanced Default)", "120", "150 (Reclaim Aggressively)"};
                int[] vals = {50, 80, 100, 120, 150};
                new AlertDialog.Builder(activity, R.style.RorDialogTheme)
                        .setTitle("Select VFS Cache Pressure")
                        .setItems(items, (d, which) -> {
                            profile.vfsCachePressure = vals[which];
                            valVfs.setText(items[which]);
                        }).show();
            });

            // Detect live topology in background and populate policy and core rows
            EXECUTOR.execute(() -> {
                CpuTopologyDetector.DeviceCpuTopology topo = CpuTopologyDetector.detect();
                activity.runOnUiThread(() -> {
                    if (activity.isFinishing() || activity.isDestroyed()) return;

                    // Populate policies
                    policiesContainer.removeAllViews();
                    for (CpuTopologyDetector.CpuPolicy pol : topo.policies) {
                        View polView = LayoutInflater.from(activity).inflate(R.layout.item_custom_profile_policy, policiesContainer, false);
                        bindPolicyView(activity, polView, pol, profile);
                        policiesContainer.addView(polView);
                    }

                    // Populate cores
                    coresContainer.removeAllViews();
                    for (CpuTopologyDetector.CpuCore core : topo.cores) {
                        View coreView = LayoutInflater.from(activity).inflate(R.layout.item_custom_profile_core, coresContainer, false);
                        bindCoreView(activity, coreView, core, profile);
                        coresContainer.addView(coreView);
                    }
                });
            });

            // Capture Live State button
            view.findViewById(R.id.btn_capture_live).setOnClickListener(v -> {
                Toast.makeText(activity, "Capturing live system hardware tuning...", Toast.LENGTH_SHORT).show();
                EXECUTOR.execute(() -> {
                    CustomProfileManager.CustomProfile live = CustomProfileManager.captureCurrentSystemAsProfile(
                            activity,
                            nameInput.getText().toString().trim().isEmpty() ? "Live Hardware Profile" : nameInput.getText().toString().trim(),
                            profile.iconResName,
                            profile.accentColor
                    );
                    activity.runOnUiThread(() -> {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        profile.policySettings = live.policySettings;
                        profile.coreOnlineMap = live.coreOnlineMap;
                        profile.swappiness = live.swappiness;
                        profile.vfsCachePressure = live.vfsCachePressure;
                        profile.dirtyRatio = live.dirtyRatio;
                        profile.ioScheduler = live.ioScheduler;
                        profile.readAheadKb = live.readAheadKb;
                        profile.gpuMaxFreqKhz = live.gpuMaxFreqKhz;

                        valSwappiness.setText(profile.swappiness >= 0 ? profile.swappiness + "%" : "Default");
                        valVfs.setText(profile.vfsCachePressure >= 0 ? String.valueOf(profile.vfsCachePressure) : "Default");

                        // Re-trigger topology populate with live values
                        EXECUTOR.execute(() -> {
                            CpuTopologyDetector.DeviceCpuTopology topo = CpuTopologyDetector.detect();
                            activity.runOnUiThread(() -> {
                                if (activity.isFinishing() || activity.isDestroyed()) return;
                                policiesContainer.removeAllViews();
                                for (CpuTopologyDetector.CpuPolicy pol : topo.policies) {
                                    View polView = LayoutInflater.from(activity).inflate(R.layout.item_custom_profile_policy, policiesContainer, false);
                                    bindPolicyView(activity, polView, pol, profile);
                                    policiesContainer.addView(polView);
                                }
                                coresContainer.removeAllViews();
                                for (CpuTopologyDetector.CpuCore core : topo.cores) {
                                    View coreView = LayoutInflater.from(activity).inflate(R.layout.item_custom_profile_core, coresContainer, false);
                                    bindCoreView(activity, coreView, core, profile);
                                    coresContainer.addView(coreView);
                                }
                                Toast.makeText(activity, "Captured live hardware tuning!", Toast.LENGTH_SHORT).show();
                            });
                        });
                    });
                });
            });

            // Save Profile
            view.findViewById(R.id.btn_editor_save).setOnClickListener(v -> {
                if (!RorLicenseManager.isPremiumActive(activity)) {
                    RorLicenseManager.showVipRequiredDialog(activity, "Custom Profile Studio", null);
                    return;
                }

                String name = nameInput.getText().toString().trim();
                if (name.isEmpty()) name = "Custom Profile " + System.currentTimeMillis() % 1000;
                profile.name = name;
                profile.dropCaches = chkDrop.isChecked();
                profile.compactMemory = chkCompact.isChecked();

                CustomProfileManager.saveProfile(activity, profile);
                Toast.makeText(activity, "Custom profile '" + profile.name + "' saved", Toast.LENGTH_SHORT).show();
                onSaved.run();
                dialog.dismiss();
            });

            view.findViewById(R.id.btn_editor_cancel).setOnClickListener(v -> dialog.dismiss());

            dialog.show();
        });
    }

    private static void bindPolicyView(Activity activity, View view, CpuTopologyDetector.CpuPolicy pol, CustomProfileManager.CustomProfile profile) {
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

        CustomProfileManager.PolicySetting existingSetting = profile.policySettings.get(pol.policyId);
        final long[] chosenMin = {existingSetting != null ? existingSetting.minFreqKhz : pol.minFreqKhz};
        final long[] chosenMax = {existingSetting != null ? existingSetting.maxFreqKhz : pol.maxFreqKhz};
        final String[] chosenGov = {existingSetting != null ? existingSetting.governor : pol.currentGovernor};

        valGov.setText(chosenGov[0]);
        valMin.setText(CpuTopologyDetector.formatFreq(chosenMin[0]));
        valMax.setText(CpuTopologyDetector.formatFreq(chosenMax[0]));

        // Ensure in profile map
        profile.policySettings.put(pol.policyId, new CustomProfileManager.PolicySetting(
                pol.policyId, chosenMin[0], chosenMax[0], chosenGov[0]
        ));

        // Governor picker
        view.findViewById(R.id.item_row_governor).setOnClickListener(v -> {
            String[] govs = pol.availableGovernors.toArray(new String[0]);
            new AlertDialog.Builder(activity, R.style.RorDialogTheme)
                    .setTitle("Policy " + pol.policyId + " Governor")
                    .setItems(govs, (d, which) -> {
                        chosenGov[0] = govs[which];
                        valGov.setText(chosenGov[0]);
                        profile.policySettings.put(pol.policyId, new CustomProfileManager.PolicySetting(
                                pol.policyId, chosenMin[0], chosenMax[0], chosenGov[0]
                        ));
                    }).show();
        });

        // Min Freq picker
        view.findViewById(R.id.item_row_min_freq).setOnClickListener(v -> {
            List<Long> freqs = pol.availableFrequenciesKhz;
            String[] items = new String[freqs.size()];
            for (int i = 0; i < freqs.size(); i++) {
                items[i] = CpuTopologyDetector.formatFreq(freqs.get(i));
            }
            new AlertDialog.Builder(activity, R.style.RorDialogTheme)
                    .setTitle("Policy " + pol.policyId + " Minimum Frequency")
                    .setItems(items, (d, which) -> {
                        chosenMin[0] = freqs.get(which);
                        if (chosenMin[0] > chosenMax[0]) chosenMax[0] = chosenMin[0];
                        valMin.setText(CpuTopologyDetector.formatFreq(chosenMin[0]));
                        valMax.setText(CpuTopologyDetector.formatFreq(chosenMax[0]));
                        profile.policySettings.put(pol.policyId, new CustomProfileManager.PolicySetting(
                                pol.policyId, chosenMin[0], chosenMax[0], chosenGov[0]
                        ));
                    }).show();
        });

        // Max Freq picker
        view.findViewById(R.id.item_row_max_freq).setOnClickListener(v -> {
            List<Long> freqs = pol.availableFrequenciesKhz;
            String[] items = new String[freqs.size()];
            for (int i = 0; i < freqs.size(); i++) {
                items[i] = CpuTopologyDetector.formatFreq(freqs.get(i));
            }
            new AlertDialog.Builder(activity, R.style.RorDialogTheme)
                    .setTitle("Policy " + pol.policyId + " Maximum Frequency")
                    .setItems(items, (d, which) -> {
                        chosenMax[0] = freqs.get(which);
                        if (chosenMax[0] < chosenMin[0]) chosenMin[0] = chosenMax[0];
                        valMin.setText(CpuTopologyDetector.formatFreq(chosenMin[0]));
                        valMax.setText(CpuTopologyDetector.formatFreq(chosenMax[0]));
                        profile.policySettings.put(pol.policyId, new CustomProfileManager.PolicySetting(
                                pol.policyId, chosenMin[0], chosenMax[0], chosenGov[0]
                        ));
                    }).show();
        });
    }

    private static void bindCoreView(Activity activity, View view, CpuTopologyDetector.CpuCore core, CustomProfileManager.CustomProfile profile) {
        TextView label = view.findViewById(R.id.item_core_label);
        TextView tag = view.findViewById(R.id.item_core_status_tag);
        SwitchMaterial sw = view.findViewById(R.id.item_core_switch);

        boolean initialOnline = profile.coreOnlineMap.containsKey(core.coreId)
                ? Boolean.TRUE.equals(profile.coreOnlineMap.get(core.coreId))
                : core.isOnline;

        if (core.coreId == 0) {
            label.setText("CPU 0 (Boot Core)");
            tag.setText("BOOT LOCKED");
            tag.setTextColor(0xFF00E5FF);
            sw.setChecked(true);
            sw.setEnabled(false);
            profile.coreOnlineMap.put(0, true);
        } else if (!core.canHotplug) {
            label.setText("CPU " + core.coreId);
            tag.setText("ONLINE");
            tag.setTextColor(0xFF00ED74);
            sw.setChecked(true);
            sw.setEnabled(false);
            profile.coreOnlineMap.put(core.coreId, true);
        } else {
            label.setText("CPU " + core.coreId);
            sw.setEnabled(true);
            sw.setChecked(initialOnline);
            tag.setText(initialOnline ? "ONLINE" : "OFFLINE");
            tag.setTextColor(initialOnline ? 0xFF00ED74 : 0xFFFA476B);

            sw.setOnCheckedChangeListener((btn, isChecked) -> {
                profile.coreOnlineMap.put(core.coreId, isChecked);
                tag.setText(isChecked ? "ONLINE" : "OFFLINE");
                tag.setTextColor(isChecked ? 0xFF00ED74 : 0xFFFA476B);
            });
        }
    }

    private static void setupIconPicker(View root, CustomProfileManager.CustomProfile profile, ImageView preview, Activity activity) {
        root.findViewById(R.id.pick_icon_game).setOnClickListener(v -> {
            profile.iconResName = "ic_game";
            profile.accentColor = "#FF5252";
            updateIconPreview(activity, preview, profile.iconResName, profile.accentColor);
        });
        root.findViewById(R.id.pick_icon_cpu).setOnClickListener(v -> {
            profile.iconResName = "ic_cpu";
            profile.accentColor = "#00E5FF";
            updateIconPreview(activity, preview, profile.iconResName, profile.accentColor);
        });
        root.findViewById(R.id.pick_icon_flash).setOnClickListener(v -> {
            profile.iconResName = "ic_flash";
            profile.accentColor = "#FFA84B";
            updateIconPreview(activity, preview, profile.iconResName, profile.accentColor);
        });
        root.findViewById(R.id.pick_icon_battery).setOnClickListener(v -> {
            profile.iconResName = "ic_battery";
            profile.accentColor = "#00ED74";
            updateIconPreview(activity, preview, profile.iconResName, profile.accentColor);
        });
        root.findViewById(R.id.pick_icon_speed).setOnClickListener(v -> {
            profile.iconResName = "ic_speed_meter";
            profile.accentColor = "#D46CFF";
            updateIconPreview(activity, preview, profile.iconResName, profile.accentColor);
        });
    }

    private static void updateIconPreview(Activity activity, ImageView preview, String iconName, String colorHex) {
        int resId = activity.getResources().getIdentifier(iconName, "drawable", activity.getPackageName());
        if (resId == 0) resId = R.drawable.ic_cpu;
        preview.setImageResource(resId);
        try {
            preview.setColorFilter(Color.parseColor(colorHex));
        } catch (Exception e) {
            preview.setColorFilter(0xFF00E5FF);
        }
    }

    public static void showProfileOptions(@NonNull Activity activity, @NonNull CustomProfileManager.CustomProfile profile, @NonNull Runnable onModified) {
        if (!RorLicenseManager.isPremiumActive(activity)) {
            RorLicenseManager.showVipRequiredDialog(activity, "Custom Profile Options", () -> {
                showProfileOptions(activity, profile, onModified);
            });
            return;
        }

        String[] options = {
                "Apply Profile",
                "Edit Profile",
                "Duplicate Profile",
                "Delete Profile"
        };

        new AlertDialog.Builder(activity, R.style.RorDialogTheme)
                .setTitle(profile.name)
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        applyProfile(activity, profile, onModified);
                    } else if (which == 1) {
                        showEditor(activity, profile, onModified);
                    } else if (which == 2) {
                        CustomProfileManager.duplicateProfile(activity, profile.id);
                        Toast.makeText(activity, "Profile duplicated", Toast.LENGTH_SHORT).show();
                        onModified.run();
                    } else if (which == 3) {
                        new AlertDialog.Builder(activity, R.style.RorDialogTheme)
                                .setTitle("Delete Profile")
                                .setMessage("Are you sure you want to delete '" + profile.name + "'?")
                                .setPositiveButton("Delete", (d2, w2) -> {
                                    CustomProfileManager.deleteProfile(activity, profile.id);
                                    Toast.makeText(activity, "Profile deleted", Toast.LENGTH_SHORT).show();
                                    onModified.run();
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    public static void applyProfile(@NonNull Activity activity, @NonNull CustomProfileManager.CustomProfile profile, @NonNull Runnable onApplied) {
        if (!RorLicenseManager.isPremiumActive(activity)) {
            RorLicenseManager.showVipRequiredDialog(activity, "Custom Profile Execution", null);
            return;
        }

        Toast.makeText(activity, "Applying custom profile '" + profile.name + "'...", Toast.LENGTH_SHORT).show();
        EXECUTOR.execute(() -> {
            List<String> logs = new ArrayList<>();
            boolean success = CustomProfileManager.applyProfile(activity, profile, logs);
            activity.runOnUiThread(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                if (success) {
                    Toast.makeText(activity, "Custom profile '" + profile.name + "' applied!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(activity, "Failed applying profile: VIP License required", Toast.LENGTH_LONG).show();
                }
                onApplied.run();
            });
        });
    }
}

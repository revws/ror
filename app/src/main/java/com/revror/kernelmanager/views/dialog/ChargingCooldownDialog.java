/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.views.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.revror.kernelmanager.R;
import com.revror.kernelmanager.charging.ChargingProtocolDetector;
import com.revror.kernelmanager.charging.SmartChargingManager;
import com.revror.kernelmanager.fragments.kernel.BatteryFragment;
import com.revror.kernelmanager.utils.GsmProfileManager;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Modern Real-Time Charging Cooldown Dashboard Dialog.
 * Displays live battery temperature, fast charging protocol, electrical wattage/current/voltage,
 * Android thermal severity, dynamic mode selection, session history, and verified controls.
 *
 * Performance Guarantee:
 * All sysfs and hardware detection runs on a dedicated background executor to prevent UI thread
 * blocking, ensuring immediate, smooth touch response.
 */
public final class ChargingCooldownDialog {

    private ChargingCooldownDialog() {}

    public static void show(@NonNull Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_charging_cooldown, null);
        AlertDialog dialog = new AlertDialog.Builder(context, R.style.RorDialogTheme)
                .setView(view)
                .create();

        SmartChargingManager manager = SmartChargingManager.getInstance(context);

        // 1. Views binding
        SwitchMaterial autoSwitch = view.findViewById(R.id.charging_auto_switch);
        TextView tempText = view.findViewById(R.id.charging_temp_text);
        TextView tempStatus = view.findViewById(R.id.charging_temp_status);
        TextView statusText = view.findViewById(R.id.charging_status_text);
        TextView protocolBadge = view.findViewById(R.id.charging_protocol_badge);
        TextView trendText = view.findViewById(R.id.charging_trend_text);

        View cardEco = view.findViewById(R.id.mode_card_eco);
        View cardBalanced = view.findViewById(R.id.mode_card_balanced);
        View cardFastCool = view.findViewById(R.id.mode_card_fast_cool);
        View cardMaximum = view.findViewById(R.id.mode_card_maximum);

        RadioButton radioEco = view.findViewById(R.id.radio_mode_eco);
        RadioButton radioBalanced = view.findViewById(R.id.radio_mode_balanced);
        RadioButton radioFastCool = view.findViewById(R.id.radio_mode_fast_cool);
        RadioButton radioMaximum = view.findViewById(R.id.radio_mode_maximum);

        TextView currentText = view.findViewById(R.id.telemetry_current_text);
        TextView voltageText = view.findViewById(R.id.telemetry_voltage_text);
        TextView wattageText = view.findViewById(R.id.telemetry_wattage_text);
        TextView thermalText = view.findViewById(R.id.telemetry_thermal_status_text);

        TextView activeOptText = view.findViewById(R.id.charging_active_opt_text);
        TextView kernelControlText = view.findViewById(R.id.charging_kernel_control_text);
        TextView sessionDeltaText = view.findViewById(R.id.charging_session_delta_text);
        LinearLayout historyContainer = view.findViewById(R.id.charging_history_container);

        Button btnApply = view.findViewById(R.id.btn_charging_apply);
        Button btnStop = view.findViewById(R.id.btn_charging_stop);
        Button btnRestore = view.findViewById(R.id.btn_charging_restore);
        Button btnKernel = view.findViewById(R.id.btn_charging_kernel);
        Button btnClose = view.findViewById(R.id.btn_charging_close);

        // 2. Setup initial selection
        final int[] selectedMode = new int[]{manager.getSelectedMode()};
        if (autoSwitch != null) {
            autoSwitch.setChecked(manager.isAutoFastCoolEnabled());
            autoSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                manager.setAutoFastCoolEnabled(isChecked);
                Toast.makeText(context, isChecked ? "Auto Fast & Cool enabled" : "Auto Fast & Cool disabled", Toast.LENGTH_SHORT).show();
            });
        }

        Runnable updateRadios = () -> {
            int m = selectedMode[0];
            if (radioEco != null) radioEco.setChecked(m == SmartChargingManager.MODE_ECO);
            if (radioBalanced != null) radioBalanced.setChecked(m == SmartChargingManager.MODE_BALANCED);
            if (radioFastCool != null) radioFastCool.setChecked(m == SmartChargingManager.MODE_FAST_COOL);
            if (radioMaximum != null) radioMaximum.setChecked(m == SmartChargingManager.MODE_MAXIMUM);

            highlightCard(cardEco, m == SmartChargingManager.MODE_ECO);
            highlightCard(cardBalanced, m == SmartChargingManager.MODE_BALANCED);
            highlightCard(cardFastCool, m == SmartChargingManager.MODE_FAST_COOL);
            highlightCard(cardMaximum, m == SmartChargingManager.MODE_MAXIMUM);
        };

        if (cardEco != null) cardEco.setOnClickListener(v -> { selectedMode[0] = SmartChargingManager.MODE_ECO; updateRadios.run(); });
        if (cardBalanced != null) cardBalanced.setOnClickListener(v -> { selectedMode[0] = SmartChargingManager.MODE_BALANCED; updateRadios.run(); });
        if (cardFastCool != null) cardFastCool.setOnClickListener(v -> { selectedMode[0] = SmartChargingManager.MODE_FAST_COOL; updateRadios.run(); });
        if (cardMaximum != null) cardMaximum.setOnClickListener(v -> { selectedMode[0] = SmartChargingManager.MODE_MAXIMUM; updateRadios.run(); });

        updateRadios.run();

        // 3. Dedicated Background Poller for Telemetry (Off Main Thread)
        final boolean[] isAlive = new boolean[]{true};
        final ExecutorService bgExecutor = Executors.newSingleThreadExecutor();
        final Handler uiHandler = new Handler(Looper.getMainLooper());

        Runnable pollerTask = new Runnable() {
            @Override
            public void run() {
                if (!isAlive[0]) return;
                // Query hardware on background thread
                ChargingProtocolDetector.ChargingInfo info = ChargingProtocolDetector.detect(context);
                boolean isActive = manager.isActive();
                float tempBefore = manager.getTempBefore();
                String lastOpt = manager.getLastOptimizationSummary();

                // Post updates cleanly to UI thread
                uiHandler.post(() -> {
                    if (!isAlive[0]) return;

                    // Temperature
                    if (tempText != null) {
                        tempText.setText(String.format(Locale.US, "%.1f°C", info.temperatureC));
                    }

                    // Temperature badge
                    if (tempStatus != null) {
                        if (!info.isCharging && !info.isFull) {
                            tempStatus.setText("DISCHARGING");
                            tempStatus.setTextColor(0xFF8E95B2);
                        } else if (info.temperatureC < 38.5f) {
                            tempStatus.setText("COOL & OPTIMAL");
                            tempStatus.setTextColor(0xFF00F5D4);
                        } else if (info.temperatureC < 42.0f) {
                            tempStatus.setText("WARM • FAST CHARGE");
                            tempStatus.setTextColor(0xFFFFD166);
                        } else {
                            tempStatus.setText("ELEVATED HEAT");
                            tempStatus.setTextColor(0xFFFF5252);
                        }
                    }

                    // Status line
                    if (statusText != null) {
                        String st = info.isCharging ? "Charging" : (info.isFull ? "Full" : "Discharging");
                        statusText.setText(String.format(Locale.US, "%s %d%% • %s", st, info.batteryLevel, info.plugType));
                    }

                    // Protocol badge
                    if (protocolBadge != null) {
                        protocolBadge.setText("⚡ " + info.protocolName);
                    }

                    // Telemetry Current
                    if (currentText != null) {
                        if (info.hasCurrentReading && info.currentMa > 0) {
                            currentText.setText(String.format(Locale.US, "%s%,d mA", info.isCharging ? "+" : "-", info.currentMa));
                            currentText.setTextColor(info.isCharging ? 0xFF00ED74 : 0xFFFF8A80);
                        } else {
                            currentText.setText("N/A");
                            currentText.setTextColor(0xFF7A809E);
                        }
                    }

                    // Telemetry Voltage
                    if (voltageText != null) {
                        if (info.hasVoltageReading && info.voltageMv > 0) {
                            voltageText.setText(String.format(Locale.US, "%.2f V", info.voltageMv / 1000.0f));
                            voltageText.setTextColor(0xFF53B2FF);
                        } else {
                            voltageText.setText("N/A");
                            voltageText.setTextColor(0xFF7A809E);
                        }
                    }

                    // Telemetry Wattage
                    if (wattageText != null) {
                        if (info.hasWattageReading && info.wattageW > 0f) {
                            wattageText.setText(String.format(Locale.US, "%.1f W (Battery)", info.wattageW));
                            wattageText.setTextColor(0xFFFFD166);
                        } else {
                            wattageText.setText("N/A");
                            wattageText.setTextColor(0xFF7A809E);
                        }
                    }

                    // Telemetry Thermal
                    if (thermalText != null) {
                        thermalText.setText(info.thermalStatusName);
                        if ("NORMAL".equalsIgnoreCase(info.thermalStatusName)) {
                            thermalText.setTextColor(0xFF00F5D4);
                        } else if ("LIGHT".equalsIgnoreCase(info.thermalStatusName) || "MODERATE".equalsIgnoreCase(info.thermalStatusName)) {
                            thermalText.setTextColor(0xFFFFD166);
                        } else {
                            thermalText.setTextColor(0xFFFF5252);
                        }
                    }

                    // Active Optimization Text
                    if (activeOptText != null) {
                        activeOptText.setText(lastOpt);
                    }

                    // Kernel Control Summary
                    if (kernelControlText != null) {
                        kernelControlText.setText(info.kernelControlSummary);
                    }

                    // Trend and Delta
                    if (trendText != null) {
                        if (isActive) {
                            float diff = tempBefore > 0f ? (info.temperatureC - tempBefore) : 0f;
                            trendText.setText(String.format(Locale.US, "⚡ Active: %s (Δ %+.1f°C)", SmartChargingManager.getModeName(manager.getSelectedMode()), diff));
                            trendText.setTextColor(0xFF00F5D4);
                        } else {
                            trendText.setText(String.format(Locale.US, "⚡ Standby • Ready to optimize", info.temperatureC));
                            trendText.setTextColor(0xFF9DA3BD);
                        }
                    }

                    if (sessionDeltaText != null) {
                        if (isActive && tempBefore > 0f) {
                            float diff = tempBefore - info.temperatureC;
                            sessionDeltaText.setText(String.format(Locale.US, "Start: %.1f°C → Current: %.1f°C (%+.1f°C Cooled)", tempBefore, info.temperatureC, diff));
                        } else {
                            sessionDeltaText.setText(String.format(Locale.US, "Current Battery Temperature: %.1f°C", info.temperatureC));
                        }
                    }

                    if (btnStop != null) {
                        btnStop.setEnabled(isActive);
                        btnStop.setAlpha(isActive ? 1.0f : 0.45f);
                    }

                    if (btnApply != null) {
                        if (isActive && manager.getSelectedMode() == selectedMode[0]) {
                            btnApply.setText("Active");
                        } else {
                            btnApply.setText("Activate Mode");
                        }
                    }
                });

                // Schedule next query if dialog is still active
                if (isAlive[0]) {
                    uiHandler.postDelayed(() -> {
                        if (isAlive[0]) bgExecutor.execute(this);
                    }, 1200);
                }
            }
        };

        // 4. Render Session History
        renderHistory(context, historyContainer, manager.getSessionHistory());

        // 5. Actions: Apply
        if (btnApply != null) {
            btnApply.setOnClickListener(v -> {
                int mode = selectedMode[0];
                btnApply.setEnabled(false);
                btnApply.setText("Applying...");
                manager.setSelectedMode(mode);
                manager.activateMode(mode, true, (success, msg) -> {
                    btnApply.setEnabled(true);
                    btnApply.setText(success ? "Active" : "Activate Mode");
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                    // Refresh poller immediately
                    bgExecutor.execute(pollerTask);
                });
            });
        }

        // 6. Actions: Stop & Revert
        if (btnStop != null) {
            btnStop.setOnClickListener(v -> {
                btnStop.setEnabled(false);
                btnStop.setText("Stopping...");
                manager.stopAndRestore((success, msg) -> {
                    btnStop.setEnabled(true);
                    btnStop.setText("Stop & Revert");
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                    // Refresh history and poller
                    renderHistory(context, historyContainer, manager.getSessionHistory());
                    bgExecutor.execute(pollerTask);
                });
            });
        }

        // 7. Actions: Restore Defaults
        if (btnRestore != null) {
            btnRestore.setOnClickListener(v -> {
                btnRestore.setEnabled(false);
                btnRestore.setText("Restoring...");
                manager.restoreDefaults((success, msg) -> {
                    btnRestore.setEnabled(true);
                    btnRestore.setText("Restore Defaults");
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                    updateRadios.run();
                    bgExecutor.execute(pollerTask);
                });
            });
        }

        // 8. Actions: Kernel Tuning (Visible for rooted users)
        boolean isRoot = GsmProfileManager.isRootEnabled(context);
        if (btnKernel != null && isRoot) {
            btnKernel.setVisibility(View.VISIBLE);
            btnKernel.setOnClickListener(v -> {
                dialog.dismiss();
                try {
                    Intent intent = new Intent(context, com.revror.kernelmanager.activities.KernelTuningActivity.class);
                    intent.putExtra("fragment", BatteryFragment.class.getCanonicalName());
                    context.startActivity(intent);
                } catch (Throwable t) {
                    Toast.makeText(context, "Could not open Kernel Battery Tuning: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        // 9. Actions: Close
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.setOnDismissListener(d -> {
            isAlive[0] = false;
            bgExecutor.shutdown();
        });

        // 10. Show & configure responsive window geometry
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            int screenWidth = dm.widthPixels;
            int screenHeight = dm.heightPixels;
            boolean isLandscape = screenWidth > screenHeight;

            int targetWidth = isLandscape ? Math.min((int)(screenWidth * 0.70f), dp(context, 520))
                    : Math.min((int)(screenWidth * 0.94f), dp(context, 480));

            window.setLayout(targetWidth, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
        }

        // Trigger first background poll
        bgExecutor.execute(pollerTask);
    }

    private static void highlightCard(View card, boolean isSelected) {
        if (card == null) return;
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(isSelected ? 0x2200F5D4 : 0x1A141A30);
        gd.setCornerRadius(dp(card.getContext(), 10));
        gd.setStroke(dp(card.getContext(), 1), isSelected ? 0xFF00F5D4 : 0x334E5D94);
        card.setBackground(gd);
    }

    private static void renderHistory(@NonNull Context context, LinearLayout container, @NonNull List<SmartChargingManager.SessionRecord> history) {
        if (container == null) return;
        container.removeAllViews();

        if (history.isEmpty()) {
            TextView emptyView = new TextView(context);
            emptyView.setText("No previous charging sessions recorded yet.\nConnect charger to log real temperature curves.");
            emptyView.setTextColor(0xFF7A809E);
            emptyView.setTextSize(10f);
            emptyView.setPadding(0, dp(context, 4), 0, dp(context, 4));
            container.addView(emptyView);
            return;
        }

        for (int i = 0; i < Math.min(3, history.size()); i++) {
            SmartChargingManager.SessionRecord r = history.get(i);
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(dp(context, 8), dp(context, 6), dp(context, 8), dp(context, 6));

            GradientDrawable rowBg = new GradientDrawable();
            rowBg.setColor(0x10151C35);
            rowBg.setCornerRadius(dp(context, 6));
            row.setBackground(rowBg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = dp(context, 4);
            row.setLayoutParams(lp);

            TextView title = new TextView(context);
            title.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            title.setText(r.dateFormatted + " • " + r.modeName);
            title.setTextColor(0xFFFFFFFF);
            title.setTextSize(10f);
            title.setTypeface(null, Typeface.BOLD);

            TextView delta = new TextView(context);
            delta.setText(String.format(Locale.US, "%.1f°C → %.1f°C (-%.1f°C)", r.startTemp, r.endTemp, r.deltaTemp));
            delta.setTextColor(r.deltaTemp >= 0 ? 0xFF00F5D4 : 0xFFFF9E00);
            delta.setTextSize(9.5f);
            delta.setTypeface(null, Typeface.BOLD);

            row.addView(title);
            row.addView(delta);
            container.addView(row);
        }
    }

    private static int dp(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}

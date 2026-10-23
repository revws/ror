package com.revror.kernelmanager.views.dialog;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.revror.kernelmanager.R;
import com.revror.kernelmanager.services.CpuOverlayService;
import com.revror.kernelmanager.utils.SilentPermissionHelper;
import com.revror.kernelmanager.utils.display.HwOverlayManager;

import java.util.concurrent.Executors;

/**
 * Dedicated Interactive Dialog for Hardware Overlays (SurfaceFlinger HWC) and Live Telemetry HUD.
 */
public final class HwOverlayDialog {

    private static final String PREF_TOOLS = "ror_tools";

    private HwOverlayDialog() {}

    public static void show(@NonNull Activity activity) {
        if (activity.isFinishing()) return;

        View root = LayoutInflater.from(activity).inflate(R.layout.dialog_hw_overlay, null);
        AlertDialog dialog = new AlertDialog.Builder(activity, R.style.RorDialogTheme)
                .setView(root)
                .create();

        SharedPreferences prefs = activity.getSharedPreferences(PREF_TOOLS, Context.MODE_PRIVATE);

        // Section 1: SurfaceFlinger HW Overlays
        TextView hwBadge = root.findViewById(R.id.hw_overlay_badge);
        TextView hwDesc = root.findViewById(R.id.hw_overlay_desc);
        SwitchMaterial hwSwitch = root.findViewById(R.id.hw_overlay_switch);

        boolean isSupported = HwOverlayManager.isSupported(activity);
        boolean isHwDisabled = HwOverlayManager.isHwOverlaysDisabled(activity);

        if (!isSupported) {
            hwSwitch.setEnabled(false);
            hwBadge.setText("UNSUPPORTED (Requires Root or Shizuku)");
            hwBadge.setTextColor(0xFFFF5277);
            hwDesc.setText("Root or Shizuku permission is required to manipulate SurfaceFlinger hardware overlay properties.");
        } else {
            hwSwitch.setChecked(isHwDisabled);
            updateHwBadge(hwBadge, isHwDisabled);

            hwSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
                btn.setEnabled(false);
                Executors.newSingleThreadExecutor().execute(() -> {
                    boolean success = HwOverlayManager.setHwOverlaysDisabled(activity, isChecked);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        btn.setEnabled(true);
                        updateHwBadge(hwBadge, isChecked);
                        Toast.makeText(activity, isChecked
                                ? (success ? "HW Overlays disabled • GPU composition forced" : "Applied • verifying with SurfaceFlinger")
                                : (success ? "HW Overlays enabled • Normal hardware planes" : "Applied • verifying state"),
                                Toast.LENGTH_SHORT).show();
                    });
                });
            });
        }

        // Section 2: Live Telemetry Floating HUD
        TextView hudBadge = root.findViewById(R.id.hud_badge);
        SwitchMaterial hudSwitch = root.findViewById(R.id.hud_switch);
        SwitchMaterial gameOnlySwitch = root.findViewById(R.id.hud_game_only_switch);
        TextView alphaLabel = root.findViewById(R.id.hud_alpha_label);
        SeekBar alphaSeekbar = root.findViewById(R.id.hud_alpha_seekbar);
        TextView intervalLabel = root.findViewById(R.id.hud_interval_label);

        Button btnFast = root.findViewById(R.id.btn_interval_fast);
        Button btnNormal = root.findViewById(R.id.btn_interval_normal);
        Button btnEco = root.findViewById(R.id.btn_interval_eco);

        boolean isHudRunning = prefs.getBoolean("overlay", false);
        hudSwitch.setChecked(isHudRunning);
        updateHudBadge(hudBadge, isHudRunning);

        hudSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                SilentPermissionHelper.ensureOverlaySilent(activity);
                Intent service = new Intent(activity, CpuOverlayService.class);
                if (Build.VERSION.SDK_INT >= 26) {
                    activity.startForegroundService(service);
                } else {
                    activity.startService(service);
                }
                prefs.edit().putBoolean("overlay", true).apply();
                updateHudBadge(hudBadge, true);
                Toast.makeText(activity, "Live Telemetry HUD started • Draggable anywhere", Toast.LENGTH_SHORT).show();
            } else {
                activity.stopService(new Intent(activity, CpuOverlayService.class));
                prefs.edit().putBoolean("overlay", false).apply();
                updateHudBadge(hudBadge, false);
                Toast.makeText(activity, "Telemetry HUD stopped", Toast.LENGTH_SHORT).show();
            }
        });

        // Game Only Mode
        boolean gameOnly = prefs.getBoolean("overlay_game_only", false);
        gameOnlySwitch.setChecked(gameOnly);
        gameOnlySwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean("overlay_game_only", isChecked).apply();
        });

        // Opacity Slider (20% to 100%)
        int currentAlpha = prefs.getInt("overlay_alpha", 90);
        alphaSeekbar.setProgress(Math.max(0, Math.min(80, currentAlpha - 20)));
        alphaLabel.setText("HUD Opacity: " + currentAlpha + "%");
        alphaSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int alphaVal = progress + 20;
                alphaLabel.setText("HUD Opacity: " + alphaVal + "%");
                prefs.edit().putInt("overlay_alpha", alphaVal).apply();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Interval selection
        int currentInterval = prefs.getInt("overlay_interval_ms", 1000);
        updateIntervalUi(currentInterval, intervalLabel, btnFast, btnNormal, btnEco);

        btnFast.setOnClickListener(v -> {
            prefs.edit().putInt("overlay_interval_ms", 500).apply();
            updateIntervalUi(500, intervalLabel, btnFast, btnNormal, btnEco);
        });

        btnNormal.setOnClickListener(v -> {
            prefs.edit().putInt("overlay_interval_ms", 1000).apply();
            updateIntervalUi(1000, intervalLabel, btnFast, btnNormal, btnEco);
        });

        btnEco.setOnClickListener(v -> {
            prefs.edit().putInt("overlay_interval_ms", 2000).apply();
            updateIntervalUi(2000, intervalLabel, btnFast, btnNormal, btnEco);
        });

        // Footer
        root.findViewById(R.id.btn_hw_close).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private static void updateHwBadge(TextView badge, boolean disabled) {
        if (disabled) {
            badge.setText("ACTIVE (GPU Compositing Forced)");
            badge.setTextColor(0xFF00F5D4);
        } else {
            badge.setText("NORMAL (Hardware Planes Enabled)");
            badge.setTextColor(0xFF2DE49A);
        }
    }

    private static void updateHudBadge(TextView badge, boolean running) {
        if (running) {
            badge.setText("RUNNING (Active On Screen)");
            badge.setTextColor(0xFF00F5D4);
        } else {
            badge.setText("STOPPED");
            badge.setTextColor(0xFF7A85A8);
        }
    }

    private static void updateIntervalUi(int intervalMs, TextView label, Button fast, Button normal, Button eco) {
        if (intervalMs <= 500) {
            label.setText("Update Frequency: 0.5s (Fast / Pro)");
            fast.setTextColor(0xFF00F5D4);
            normal.setTextColor(0xFFFFFFFF);
            eco.setTextColor(0xFFFFFFFF);
        } else if (intervalMs >= 2000) {
            label.setText("Update Frequency: 2.0s (Eco Saver)");
            fast.setTextColor(0xFFFFFFFF);
            normal.setTextColor(0xFFFFFFFF);
            eco.setTextColor(0xFF00F5D4);
        } else {
            label.setText("Update Frequency: 1.0s (Normal)");
            fast.setTextColor(0xFFFFFFFF);
            normal.setTextColor(0xFF00F5D4);
            eco.setTextColor(0xFFFFFFFF);
        }
    }
}

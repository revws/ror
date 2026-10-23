package com.revror.kernelmanager.views.dialog;

import android.app.Activity;
import android.hardware.Sensor;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.revror.kernelmanager.R;
import com.revror.kernelmanager.utils.sensor.GyroDelayEngine;

import java.util.Locale;

/**
 * Interactive Gyroscope Latency Diagnostic & Optimization Dialog.
 */
public final class GyroDelayDialog {

    private GyroDelayDialog() {}

    public static void show(@NonNull Activity activity) {
        if (activity.isFinishing()) return;

        if (!com.revror.kernelmanager.utils.RorLicenseManager.isPremiumActive(activity)) {
            com.revror.kernelmanager.utils.RorLicenseManager.showVipRequiredDialog(activity, "Gyro Delay Fix (Phoenix VIP)", () -> {
                show(activity);
            });
            return;
        }

        View root = LayoutInflater.from(activity).inflate(R.layout.dialog_gyro_delay, null);

        TextView hwSummary = root.findViewById(R.id.gyro_hardware_summary);
        LinearLayout progressLayout = root.findViewById(R.id.gyro_progress_layout);
        TextView progressText = root.findViewById(R.id.gyro_progress_text);
        ProgressBar progressBar = root.findViewById(R.id.gyro_progress_bar);

        TextView badgeStatus = root.findViewById(R.id.gyro_badge_status);
        TextView valDelay = root.findViewById(R.id.gyro_val_delay);
        TextView valP95 = root.findViewById(R.id.gyro_val_p95);
        TextView valHz = root.findViewById(R.id.gyro_val_hz);
        TextView valInterval = root.findViewById(R.id.gyro_val_interval);
        TextView valJitter = root.findViewById(R.id.gyro_val_jitter);
        TextView valIrregular = root.findViewById(R.id.gyro_val_irregular);

        LinearLayout comparisonCard = root.findViewById(R.id.gyro_comparison_card);
        TextView optDelta = root.findViewById(R.id.gyro_opt_delta);
        TextView optSummary = root.findViewById(R.id.gyro_opt_summary);

        Button btnTest = root.findViewById(R.id.btn_gyro_test);
        Button btnOptimize = root.findViewById(R.id.btn_gyro_optimize);
        Button btnClose = root.findViewById(R.id.btn_gyro_close);

        Sensor gyro = GyroDelayEngine.getGyroscopeSensor(activity);
        if (gyro != null) {
            double maxRate = gyro.getMinDelay() > 0 ? (1_000_000.0 / gyro.getMinDelay()) : 200.0;
            hwSummary.setText(String.format(Locale.US, "%s\n%s • Max Hardware: %.0f Hz",
                    gyro.getName(), gyro.getVendor(), maxRate));
        } else {
            hwSummary.setText("No hardware gyroscope detected on this device");
            btnTest.setEnabled(false);
            btnOptimize.setEnabled(false);
        }

        AlertDialog dialog = new AlertDialog.Builder(activity, R.style.RorDialogTheme)
                .setView(root)
                .create();

        dialog.setOnDismissListener(d -> GyroDelayEngine.cancelActiveMeasurement());

        final GyroDelayEngine.GyroMetrics[] baselineMetrics = new GyroDelayEngine.GyroMetrics[1];

        Runnable runDiagnostic = () -> {
            progressLayout.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);
            badgeStatus.setText("MEASURING...");
            badgeStatus.setTextColor(0xFF00F5D4);
            btnTest.setEnabled(false);
            btnOptimize.setEnabled(false);

            GyroDelayEngine.measureLatency(activity, 100, new GyroDelayEngine.DiagnosticCallback() {
                @Override
                public void onProgress(int currentSamples, int totalSamples, double currentDelayMs) {
                    progressBar.setProgress((int) ((currentSamples * 100.0) / totalSamples));
                    progressText.setText(String.format(Locale.US, "Sampling sensor: %d/%d (Live: %.1f ms)",
                            currentSamples, totalSamples, currentDelayMs));
                }

                @Override
                public void onComplete(@NonNull GyroDelayEngine.GyroMetrics metrics) {
                    progressLayout.setVisibility(View.GONE);
                    btnTest.setEnabled(true);
                    btnOptimize.setEnabled(true);

                    valDelay.setText(String.format(Locale.US, "%.1f ms", metrics.deliveryDelayAvgMs));
                    valP95.setText(String.format(Locale.US, "p95: %.1f ms", metrics.deliveryDelayP95Ms));
                    valHz.setText(String.format(Locale.US, "%.0f Hz", metrics.actualHz));
                    valInterval.setText(String.format(Locale.US, "Interval: %.2f ms", metrics.avgIntervalMs));
                    valJitter.setText(String.format(Locale.US, "%.2f ms", metrics.jitterMs));
                    valIrregular.setText(String.format(Locale.US, "Drops: %d", metrics.irregularEvents));

                    if (baselineMetrics[0] == null) {
                        baselineMetrics[0] = metrics;
                        badgeStatus.setText("BASELINE");
                        badgeStatus.setTextColor(0xFF2DE49A);
                    } else {
                        badgeStatus.setText("ACTIVE");
                        badgeStatus.setTextColor(0xFF00F5D4);

                        // Compare against baseline
                        double delayDiff = baselineMetrics[0].deliveryDelayAvgMs - metrics.deliveryDelayAvgMs;
                        double jitterDiff = baselineMetrics[0].jitterMs - metrics.jitterMs;
                        comparisonCard.setVisibility(View.VISIBLE);

                        if (delayDiff > 0.1 || jitterDiff > 0.05) {
                            optDelta.setText("LATENCY IMPROVED");
                            optDelta.setTextColor(0xFF00F5D4);
                            optSummary.setText(String.format(Locale.US,
                                    "Delivery delay reduced by -%.1f ms • Jitter decreased by -%.2f ms • Active sampling: %.0f Hz",
                                    delayDiff, Math.max(0, jitterDiff), metrics.actualHz));
                        } else {
                            optDelta.setText("HARDWARE PEAK");
                            optDelta.setTextColor(0xFF2DE49A);
                            optSummary.setText(String.format(Locale.US,
                                    "Sensor is already operating at hardware peak (%.0f Hz, %.1f ms latency, %.2f ms jitter).",
                                    metrics.actualHz, metrics.deliveryDelayAvgMs, metrics.jitterMs));
                        }
                    }
                }

                @Override
                public void onError(String message) {
                    progressLayout.setVisibility(View.GONE);
                    btnTest.setEnabled(true);
                    btnOptimize.setEnabled(true);
                    badgeStatus.setText("ERROR");
                    badgeStatus.setTextColor(0xFFFF5252);
                    Toast.makeText(activity, "Gyro Diagnostic: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        };

        btnTest.setOnClickListener(v -> runDiagnostic.run());

        btnOptimize.setOnClickListener(v -> {
            btnOptimize.setEnabled(false);
            btnTest.setEnabled(false);
            progressLayout.setVisibility(View.VISIBLE);
            progressText.setText("Applying kernel sensor poll rate & priority tuning...");
            progressBar.setIndeterminate(true);

            GyroDelayEngine.applyOptimization(activity, () -> {
                progressBar.setIndeterminate(false);
                Toast.makeText(activity, "Gyro delay optimization applied! Verifying...", Toast.LENGTH_SHORT).show();
                // Run post-optimization measurement
                runDiagnostic.run();
            });
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            DisplayMetrics dm = activity.getResources().getDisplayMetrics();
            int width = (int) (dm.widthPixels * 0.92);
            dialog.getWindow().setLayout(width, -2);
        }

        // Run baseline test immediately when opened
        if (gyro != null) {
            runDiagnostic.run();
        }
    }
}

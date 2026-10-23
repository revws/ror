package com.revror.kernelmanager.views.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.revror.kernelmanager.R;
import com.revror.kernelmanager.utils.doze.ForceDozeManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Universal Ad-Free Force Doze Controller Dialog.
 *
 * Provides real execution of Battery, Balance, and Gaming background management
 * with zero advertisements and detailed real-time execution feedback.
 */
public final class ForceDozeDialog {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private ForceDozeDialog() {}

    public static void show(@NonNull Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        activity.runOnUiThread(() -> {
            View view = LayoutInflater.from(activity).inflate(R.layout.dialog_force_doze, null);

            AlertDialog dialog = new AlertDialog.Builder(activity)
                    .setView(view)
                    .setCancelable(true)
                    .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            TextView statusTitle = view.findViewById(R.id.doze_status_title);
            TextView statusDetails = view.findViewById(R.id.doze_status_details);

            View btnBattery = view.findViewById(R.id.btn_doze_battery);
            View btnBalance = view.findViewById(R.id.btn_doze_balance);
            View btnGaming = view.findViewById(R.id.btn_doze_gaming);
            View btnExit = view.findViewById(R.id.btn_doze_exit);
            View btnDismiss = view.findViewById(R.id.btn_doze_dismiss);

            View.OnClickListener applyListener = v -> {
                ForceDozeManager.DozeMode mode;
                if (v == btnBattery || v == view.findViewById(R.id.doze_card_battery)) {
                    mode = ForceDozeManager.DozeMode.BATTERY;
                } else if (v == btnBalance || v == view.findViewById(R.id.doze_card_balance)) {
                    mode = ForceDozeManager.DozeMode.BALANCE;
                } else {
                    mode = ForceDozeManager.DozeMode.GAMING;
                }

                statusTitle.setText("Optimizing...");
                statusDetails.setText("Scanning background apps and applying " + mode.getTitle() + "...");
                btnBattery.setEnabled(false);
                btnBalance.setEnabled(false);
                btnGaming.setEnabled(false);

                EXECUTOR.execute(() -> {
                    ForceDozeManager.DozeReport report = ForceDozeManager.executeDoze(activity, mode);
                    activity.runOnUiThread(() -> {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        btnBattery.setEnabled(true);
                        btnBalance.setEnabled(true);
                        btnGaming.setEnabled(true);

                        statusTitle.setText(report.mode.getTitle() + " Active");
                        statusDetails.setText(String.format("Closed %d apps • Protected %d apps • Total scanned %d",
                                report.stoppedCount, report.protectedCount, report.scannedCount));

                        Toast.makeText(activity, report.statusSummary, Toast.LENGTH_LONG).show();
                    });
                });
            };

            btnBattery.setOnClickListener(applyListener);
            btnBalance.setOnClickListener(applyListener);
            btnGaming.setOnClickListener(applyListener);
            view.findViewById(R.id.doze_card_battery).setOnClickListener(applyListener);
            view.findViewById(R.id.doze_card_balance).setOnClickListener(applyListener);
            view.findViewById(R.id.doze_card_gaming).setOnClickListener(applyListener);

            btnExit.setOnClickListener(v -> {
                statusTitle.setText("Resuming...");
                statusDetails.setText("Unforcing device-idle and restoring normal scheduling...");
                EXECUTOR.execute(() -> {
                    ForceDozeManager.exitDoze(activity);
                    activity.runOnUiThread(() -> {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        statusTitle.setText("Normal Scheduling Restored");
                        statusDetails.setText("Device returned to standard Android background execution.");
                        Toast.makeText(activity, "Normal background scheduling restored", Toast.LENGTH_SHORT).show();
                    });
                });
            });

            btnDismiss.setOnClickListener(v -> dialog.dismiss());

            dialog.show();
        });
    }
}

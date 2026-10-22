/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.revror.kernelmanager.R;
import com.revror.kernelmanager.utils.root.RootUtils;

import java.util.concurrent.Executors;

public class EulaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eula);

        Button btnSeeFull = findViewById(R.id.btn_see_full_eula);
        Button btnAccept = findViewById(R.id.btn_accept_eula);

        btnSeeFull.setOnClickListener(v -> showFullEulaDialog());

        btnAccept.setOnClickListener(v -> {
            getSharedPreferences("ror_privacy", MODE_PRIVATE)
                    .edit()
                    .putBoolean("ror_eula_accepted", true)
                    .apply();

            // Root silent permission auto-grants (no extra prompts needed)
            grantPrivilegesInBackground();

            Intent intent = new Intent(this, DashboardActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void grantPrivilegesInBackground() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                if (RootUtils.rootAccess()) {
                    String pkg = getPackageName();
                    RootUtils.runCommand("pm grant " + pkg + " android.permission.SYSTEM_ALERT_WINDOW 2>/dev/null; "
                            + "appops set " + pkg + " SYSTEM_ALERT_WINDOW allow 2>/dev/null; "
                            + "pm grant " + pkg + " android.permission.WRITE_SECURE_SETTINGS 2>/dev/null; "
                            + "pm grant " + pkg + " android.permission.PACKAGE_USAGE_STATS 2>/dev/null; "
                            + "appops set " + pkg + " GET_USAGE_STATS allow 2>/dev/null; "
                            + "pm grant " + pkg + " android.permission.POST_NOTIFICATIONS 2>/dev/null");
                }
            } catch (Exception ignored) {}
        });
    }

    private void showFullEulaDialog() {
        new AlertDialog.Builder(this, R.style.RorDialogTheme)
                .setTitle("End User License Agreement")
                .setMessage("Reincarnation of Revolutionary\n\n"
                        + "1. Disclaimer of Warranty & Liability:\n"
                        + "This software is distributed under the GNU General Public License v3.0 or later without any warranty. "
                        + "By proceeding, you acknowledge that modifying kernel parameters, CPU and GPU clock frequencies, and sysfs nodes involves inherent risk. "
                        + "The developers are not liable for any system instability, bootloops, data corruption, or hardware damage.\n\n"
                        + "2. Data Safety:\n"
                        + "Always maintain full Nandroid backups or file backups before applying kernel governor overrides.\n\n"
                        + "3. Mod Compatibility:\n"
                        + "Do not stack conflicting third-party performance daemons concurrently with REVROR profiles.\n\n"
                        + "4. Root & Privacy:\n"
                        + "REVROR operates 100% on-device and does not transmit private telemetry to any third party.\n\n"
                        + "5. On-Device Execution & Privacy:\n"
                        + "Root and Shizuku run 100% on-device with zero telemetry collected or shared.\n\n"
                        + "6. Thermal & Power Limits:\n"
                        + "Gaming and overclock modes increase device thermal load and power consumption.\n\n"
                        + "7. Factory Defaults:\n"
                        + "All kernel and system parameters can be restored to factory defaults anytime.")
                .setPositiveButton("Close", null)
                .show();
    }
}


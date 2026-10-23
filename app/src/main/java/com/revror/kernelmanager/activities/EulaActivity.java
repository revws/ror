/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.activities;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.revror.kernelmanager.R;
import com.revror.kernelmanager.utils.root.RootUtils;

import java.util.concurrent.Executors;

public class EulaActivity extends AppCompatActivity {

    private View containerWelcome;
    private View containerFullEula;
    private ObjectAnimator logoPulse;

    private static final String FULL_EULA_TEXT =
            "REINCARNATION OF REVOLUTIONARY (REVROR)\n"
            + "END USER LICENSE AGREEMENT & PLATFORM DISCLOSURE\n"
            + "Version 50.4 Hyper Edition | GNU General Public License v3.0\n\n"
            + "================================================================================\n"
            + "§ 1. PREAMBLE & OPEN-SOURCE LICENSING GRANT\n"
            + "================================================================================\n"
            + "Reincarnation of Revolutionary (\"REVROR\" or \"the Software\") is free and open-source software licensed under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.\n\n"
            + "This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for full details.\n\n"
            + "================================================================================\n"
            + "§ 2. HARDWARE OVERCLOCKING & GOVERNOR DISCLAIMER\n"
            + "================================================================================\n"
            + "REVROR provides precision interfaces allowing direct interaction with Android kernel governors, CPU cluster frequency scaling, GPU devfreq clocks, I/O schedulers, virtual memory compaction nodes, and thermal parameters.\n\n"
            + "By proceeding and activating performance profiles:\n"
            + "a) You acknowledge that operating hardware at peak frequencies or modified voltages carries inherent thermodynamic load.\n"
            + "b) While REVROR incorporates multi-layered safety guardrails (including automatic thermal cooldown throttles), the software authors and contributors assume no liability for system crashes, spontaneous kernel panics, bootloops, battery degradation, or hardware component failure resulting from deliberate hardware overclocking or unsupported device parameters.\n\n"
            + "================================================================================\n"
            + "§ 3. ZERO-TELEMETRY & 100% ON-DEVICE PRIVACY GUARANTEE\n"
            + "================================================================================\n"
            + "REVROR is engineered strictly under a Zero-Knowledge, Zero-Telemetry architecture:\n"
            + "a) 100% On-Device Execution: All kernel parameter reads, sysfs manipulations, Shizuku IPC binder transactions, and root shell commands are executed entirely locally within your device environment.\n"
            + "b) No Data Collection: REVROR contains zero analytical tracking beacons, zero advertising SDKs, zero telemetry loggers, and zero crash data uploaders.\n"
            + "c) Network Integrity: Network access is utilized exclusively for user-initiated actions (such as checking for software updates or opening project repository pages).\n\n"
            + "================================================================================\n"
            + "§ 4. PRIVILEGED SUBSYSTEMS: ROOT & SHIZUKU PROTOCOLS\n"
            + "================================================================================\n"
            + "To execute privileged kernel and system operations without persistent su popups or system friction, REVROR interacts with established system interfaces:\n"
            + "a) Root Access: Operates via libsu / su daemon with strict shell sanitization and automated privilege verification.\n"
            + "b) Shizuku Privilege Bridge: Facilitates non-root system operations via Android IPC binder tokens.\n"
            + "c) User Control: You retain complete authority to revoke, re-authenticate, or restrict access permissions at any time through your superuser manager or Shizuku application.\n\n"
            + "================================================================================\n"
            + "§ 5. THIRD-PARTY MODIFICATION STACKING POLICY\n"
            + "================================================================================\n"
            + "REVROR applies comprehensive low-level system governors. Concurrently running third-party performance daemons, conflicting Magisk modules, or aggressive battery killers may result in governor desynchronization, micro-stutters, or conflicting sysfs writes. It is strongly recommended to run REVROR as your unified performance tuning environment.\n\n"
            + "================================================================================\n"
            + "§ 6. THERMAL ELEVATION & HIGH-DRAW LOAD DISCLOSURE\n"
            + "================================================================================\n"
            + "Sustained usage of tournament gaming profiles (including Ultra Gaming, Apex, and Esports modes) intentionally maximizes core frequencies and disables conservative idle states. Consequently:\n"
            + "a) Device surface temperature may elevate during heavy rendering.\n"
            + "b) Battery consumption will accelerate proportionally with compute throughput.\n"
            + "c) REVROR provides real-time thermal telemetry, automated thermal guard warnings, and an instant Charging Cooldown tool to protect battery health during intensive gaming sessions.\n\n"
            + "================================================================================\n"
            + "§ 7. INSTANT FACTORY RESTORATION GUARANTEE\n"
            + "================================================================================\n"
            + "All kernel sysfs nodes, CPU/GPU clock limits, virtual memory thresholds, and Android system properties modified by REVROR are fully reversible. Selecting the Stock/Balance profile or utilizing the built-in Restore Defaults tool immediately resets hardware parameters to OEM system defaults.\n\n"
            + "================================================================================\n"
            + "§ 8. USER ACCEPTANCE & CONSENT\n"
            + "================================================================================\n"
            + "By tapping \"Accept & Launch\" or \"I Agree & Continue\", you certify that you have read, understood, and consented to these terms, and assume full operational agency over your device hardware modifications.";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eula);

        containerWelcome = findViewById(R.id.container_welcome);
        containerFullEula = findViewById(R.id.container_full_eula);

        ImageView welcomeLogo = findViewById(R.id.welcome_logo);
        if (welcomeLogo != null) {
            logoPulse = ObjectAnimator.ofFloat(welcomeLogo, "alpha", 0.85f, 1.0f);
            logoPulse.setDuration(1200);
            logoPulse.setRepeatMode(ValueAnimator.REVERSE);
            logoPulse.setRepeatCount(ValueAnimator.INFINITE);
            logoPulse.start();
        }

        TextView tvFullEula = findViewById(R.id.tv_full_eula_text);
        if (tvFullEula != null) {
            tvFullEula.setText(FULL_EULA_TEXT);
        }

        // Welcome page actions
        Button btnSeeFull = findViewById(R.id.btn_see_full_eula);
        Button btnAccept = findViewById(R.id.btn_accept_eula);

        if (btnSeeFull != null) {
            btnSeeFull.setOnClickListener(v -> showFullEulaPage());
        }

        if (btnAccept != null) {
            btnAccept.setOnClickListener(v -> acceptAndProceed());
        }

        // Full EULA reader actions
        ImageView btnBack = findViewById(R.id.btn_back_to_welcome);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> showWelcomePage());
        }

        Button btnReaderBack = findViewById(R.id.btn_reader_back);
        if (btnReaderBack != null) {
            btnReaderBack.setOnClickListener(v -> showWelcomePage());
        }

        Button btnReaderAccept = findViewById(R.id.btn_reader_accept);
        if (btnReaderAccept != null) {
            btnReaderAccept.setOnClickListener(v -> acceptAndProceed());
        }
    }

    private void showFullEulaPage() {
        if (containerWelcome != null) containerWelcome.setVisibility(View.GONE);
        if (containerFullEula != null) containerFullEula.setVisibility(View.VISIBLE);
    }

    private void showWelcomePage() {
        if (containerFullEula != null) containerFullEula.setVisibility(View.GONE);
        if (containerWelcome != null) containerWelcome.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        if (containerFullEula != null && containerFullEula.getVisibility() == View.VISIBLE) {
            showWelcomePage();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (logoPulse != null) {
            logoPulse.cancel();
            logoPulse = null;
        }
        super.onDestroy();
    }

    private void acceptAndProceed() {
        getSharedPreferences("ror_privacy", MODE_PRIVATE)
                .edit()
                .putBoolean("ror_eula_accepted", true)
                .apply();

        // Silent root privilege auto-grants
        grantPrivilegesInBackground();

        Intent intent = new Intent(this, DashboardActivity.class);
        startActivity(intent);
        finish();
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
}

/* Copyright (C) 2026 REVROR contributors - Commercial License */
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
            + "END USER LICENSE AGREEMENT & COMMERCIAL PLATFORM DISCLOSURE\n"
            + "Version 50.4 Hyper Edition | Commercial License • Paid Access Model\n\n"
            + "================================================================================\n"
            + "§ 1. COMMERCIAL SOFTWARE & PAID LICENSE REQUIREMENT\n"
            + "================================================================================\n"
            + "Reincarnation of Revolutionary (\"REVROR\" or \"the Software\") is proprietary commercial software developed and maintained by revWhiteShadow. THIS APPLICATION IS NOT FREE SOFTWARE.\n\n"
            + "To obtain, unlock, and maintain full access to all features—including Ultra Gaming profiles, Phoenix Kernel Overclocking, Game Space floating HUD tools, Aim Boost, and hardware governors—you must purchase an authentic commercial license.\n\n"
            + "================================================================================\n"
            + "§ 2. COMMERCIAL TERMS & LICENSE TIERS\n"
            + "================================================================================\n"
            + "A purchased license grants the non-exclusive, non-transferable right to activate and run the full suite of REVROR features on your designated device hardware token.\n\n"
            + "a) Unlicensed Evaluation: Installations without an active purchased license operate in a strictly restricted evaluation mode with core performance overclocking locked.\n"
            + "b) Paywall Integrity: Any attempt to bypass, tamper with, crack, reverse-engineer, or spoof the cryptographic license validation subsystem is strictly prohibited and terminates your entitlement immediately.\n"
            + "c) Non-Transferability: Device-locked licenses are tied to your specific hardware fingerprint and cannot be transferred without developer authorization.\n\n"
            + "================================================================================\n"
            + "§ 3. OFFICIAL LICENSE PURCHASE & ACQUISITION CHANNELS\n"
            + "================================================================================\n"
            + "Authentic REVROR commercial licenses may be purchased exclusively through official verified developer channels:\n\n"
            + "• Official Developer: revWhiteShadow\n"
            + "• Official Telegram Channel: https://t.me/revWhiteShadow\n"
            + "• Official Ko-fi Support: https://ko-fi.com/revWS\n"
            + "• Official Community: @godTspeed / @godspeedmode\n\n"
            + "Purchased license tokens are cryptographically signed using ECC P-256 keys and can be applied directly via the \"Enter Key\" interface or the More page.\n\n"
            + "================================================================================\n"
            + "§ 4. OFFICIAL SOURCE CODE REPOSITORY & OPEN DESIGN ACCESS\n"
            + "================================================================================\n"
            + "The official public source repository is hosted at:\n"
            + "https://github.com/revws/rorexp\n\n"
            + "In the spirit of transparent Android development and community UI customization:\n"
            + "a) The presentation layer, layouts, vector assets, themes, and open-source UI design stubs are published on GitHub under source-available terms.\n"
            + "b) All proprietary kernel tuning governors, su root execution scripts, Aim Boost latency calibrators, and cryptographic licensing systems remain proprietary intellectual property.\n"
            + "c) Compiling, distributing, or publishing commercial clones or builds intended to circumvent the purchased license requirement is strictly prohibited.\n\n"
            + "================================================================================\n"
            + "§ 5. HARDWARE OVERCLOCKING & GOVERNOR DISCLAIMER\n"
            + "================================================================================\n"
            + "REVROR provides precision interfaces allowing direct interaction with Android kernel governors, CPU cluster frequency scaling, GPU devfreq clocks, I/O schedulers, virtual memory compaction nodes, and thermal parameters.\n\n"
            + "By proceeding and activating performance profiles:\n"
            + "a) You acknowledge that operating hardware at peak frequencies or modified voltages carries inherent thermodynamic load.\n"
            + "b) While REVROR incorporates multi-layered safety guardrails (including automatic thermal cooldown throttles), the software authors assume no liability for system crashes, spontaneous kernel panics, bootloops, battery degradation, or hardware component failure resulting from deliberate hardware overclocking or unsupported device parameters.\n\n"
            + "================================================================================\n"
            + "§ 6. ZERO-TELEMETRY & 100% ON-DEVICE PRIVACY GUARANTEE\n"
            + "================================================================================\n"
            + "REVROR is engineered strictly under a Zero-Knowledge, Zero-Telemetry architecture:\n"
            + "a) 100% On-Device Execution: All kernel parameter reads, sysfs manipulations, Shizuku IPC binder transactions, and root shell commands are executed entirely locally within your device environment.\n"
            + "b) No Data Collection: REVROR contains zero analytical tracking beacons, zero advertising SDKs, zero telemetry loggers, and zero crash data uploaders.\n"
            + "c) Network Integrity: Network access is utilized exclusively for user-initiated actions (such as purchasing a license, checking for software updates, or opening project repository pages).\n\n"
            + "================================================================================\n"
            + "§ 7. PRIVILEGED SUBSYSTEMS: ROOT & SHIZUKU PROTOCOLS\n"
            + "================================================================================\n"
            + "To execute privileged kernel and system operations without persistent su popups or system friction, REVROR interacts with established system interfaces:\n"
            + "a) Root Access: Operates via libsu / su daemon with strict shell sanitization and automated privilege verification.\n"
            + "b) Shizuku Privilege Bridge: Facilitates non-root system operations via Android IPC binder tokens.\n"
            + "c) User Control: You retain complete authority to revoke, re-authenticate, or restrict access permissions at any time through your superuser manager or Shizuku application.\n\n"
            + "================================================================================\n"
            + "§ 8. FACTORY RESET & USER ACCEPTANCE\n"
            + "================================================================================\n"
            + "All kernel sysfs nodes, CPU/GPU clock limits, virtual memory thresholds, and Android system properties modified by REVROR are fully reversible to OEM factory stock anytime.\n\n"
            + "By tapping \"Accept & Launch\" or \"I Agree & Continue\", you certify that you have read, understood, and consented to these commercial terms, acknowledge that full feature access requires purchasing a valid license, and assume full operational agency over your device hardware modifications.";

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

        // Commercial license & source actions
        View btnBuyLicense = findViewById(R.id.btn_welcome_purchase_license);
        if (btnBuyLicense != null) {
            btnBuyLicense.setOnClickListener(v -> openUrl("https://t.me/revWhiteShadow"));
        }

        View btnActivateKey = findViewById(R.id.btn_welcome_activate_key);
        if (btnActivateKey != null) {
            btnActivateKey.setOnClickListener(v -> {
                com.revror.kernelmanager.utils.RorLicenseManager.showVipRequiredDialog(
                        this,
                        "REVROR Commercial",
                        () -> android.widget.Toast.makeText(this, "License Activated! Full access unlocked.", android.widget.Toast.LENGTH_SHORT).show()
                );
            });
        }

        View btnViewSource = findViewById(R.id.btn_welcome_view_source);
        if (btnViewSource != null) {
            btnViewSource.setOnClickListener(v -> openUrl("https://github.com/revws/rorexp"));
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

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Throwable t) {
            android.widget.Toast.makeText(this, "Opening: " + url, android.widget.Toast.LENGTH_SHORT).show();
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

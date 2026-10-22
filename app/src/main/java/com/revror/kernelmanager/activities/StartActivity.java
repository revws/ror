/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.revror.kernelmanager.utils.AppIntegrity;
import com.revror.kernelmanager.utils.root.RootUtils;

import java.util.concurrent.Executors;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppIntegrity.enforce(this);

        // Silent root auto-grant background privileges (no intrusive popups)
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

        boolean accepted = getSharedPreferences("ror_privacy", MODE_PRIVATE)
                .getBoolean("ror_eula_accepted", false);

        Intent intent = new Intent(this, accepted ? DashboardActivity.class : EulaActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (getIntent().getExtras() != null) {
            intent.putExtras(getIntent().getExtras());
        }
        startActivity(intent);
        finish();
    }
}

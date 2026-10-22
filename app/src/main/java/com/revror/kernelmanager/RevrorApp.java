/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager;

import android.app.Application;
import com.revror.kernelmanager.utils.AppIntegrity;

/**
 * REVROR Core Application entry point.
 * Verifies build identity and enforces signature integrity before any component launches.
 */
public class RevrorApp extends Application {
    public static final String REVROR_SIGNATURE = "revror";

    @Override
    public void onCreate() {
        super.onCreate();
        com.revror.kernelmanager.utils.ProfileDiagnostics.install(this);
        AppIntegrity.enforce(this);
    }
}


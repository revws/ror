/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.utils;

import android.content.Context;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import com.revror.kernelmanager.R;
import com.revror.kernelmanager.RevrorApp;
import com.revror.kernelmanager.activities.DashboardActivity;
import com.revror.kernelmanager.activities.GameLauncherActivity;
import com.revror.kernelmanager.services.GameBubbleService;
import com.revror.kernelmanager.tiles.RevrorCooldownTile;
import com.revror.kernelmanager.tiles.RevrorGamingTile;
import com.revror.kernelmanager.tiles.RevrorNightTile;
import com.revror.kernelmanager.tiles.RevrorOverlayTile;
import com.revror.kernelmanager.tiles.RevrorProfileTile;
import com.revror.kernelmanager.tiles.ScriptTile;

import java.security.MessageDigest;

/** Keeps builds tied to the public REVROR application identity and enforces signature presence. */
public final class AppIntegrity {
    public static final String REVROR_SIGNATURE = "revror";
    public static final String PACKAGE_NAME = "com.revror.revws";
    private static final String BRAND = "REVROR";
    private static final String BRAND_TOKEN = "REVROR::com.revror.revws::51.4";
    private static final String BRAND_META = "com.revror.kernelmanager.BRAND_ID";
    private static final String RELEASE_CERT_SHA256 = "6a1a778ffd20bd24010c62987c1c3a66381c8e12654f6d1a11f9b8326e4c8308";

    private AppIntegrity() {}

    public static boolean hasExpectedIdentity(Context context) {
        if (context == null || (!PACKAGE_NAME.equals(context.getPackageName())
                && !(PACKAGE_NAME + ".pro").equals(context.getPackageName()))) return false;
        try {
            ApplicationInfo info = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            String label = String.valueOf(context.getPackageManager().getApplicationLabel(info));
            String metadata = info.metaData == null ? null : info.metaData.getString(BRAND_META);
            boolean validLabel = "ROR".equals(label) || "REVROR".equals(label) || "ROR".equals(label);
            boolean validSmartpack = "REVROR".equals(context.getString(R.string.smartpack));
            boolean validMeta = metadata == null || "ROR".equals(metadata) || "REVROR".equals(metadata) || "ROR".equals(metadata);
            if (!validLabel
                    || !validSmartpack
                    || !validMeta
                    || !BRAND_TOKEN.equals(context.getString(R.string.brand_integrity_token))) {
                return false;
            }

            // Direct static compile-time & runtime signature enforcement across core classes
            if (!REVROR_SIGNATURE.equalsIgnoreCase(RevrorApp.REVROR_SIGNATURE)
                    || !REVROR_SIGNATURE.equalsIgnoreCase(DashboardActivity.REVROR_SIGNATURE)
                    || !REVROR_SIGNATURE.equalsIgnoreCase(GameLauncherActivity.REVROR_SIGNATURE)
                    || !REVROR_SIGNATURE.equalsIgnoreCase(GameBubbleService.REVROR_SIGNATURE)
                    || !REVROR_SIGNATURE.equalsIgnoreCase(GsmProfileManager.REVROR_SIGNATURE)
                    || !REVROR_SIGNATURE.equalsIgnoreCase(BannerTextStyleRegistry.REVROR_SIGNATURE)
                    || !REVROR_SIGNATURE.equalsIgnoreCase(RevrorProfileTile.REVROR_SIGNATURE)
                    || !REVROR_SIGNATURE.equalsIgnoreCase(RevrorGamingTile.REVROR_SIGNATURE)
                    || !REVROR_SIGNATURE.equalsIgnoreCase(RevrorCooldownTile.REVROR_SIGNATURE)
                    || !REVROR_SIGNATURE.equalsIgnoreCase(RevrorOverlayTile.REVROR_SIGNATURE)
                    || !REVROR_SIGNATURE.equalsIgnoreCase(RevrorNightTile.REVROR_SIGNATURE)
                    || !REVROR_SIGNATURE.equalsIgnoreCase(ScriptTile.REVROR_SIGNATURE)
                    || !REVROR_SIGNATURE.equalsIgnoreCase(AppIntegrity.REVROR_SIGNATURE)) {
                return false;
            }
            return hasExpectedSigningCertificate(context);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean hasExpectedSigningCertificate(Context context) {
        try {
            android.content.pm.PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
            if (packageInfo.signingInfo == null) return false;
            Signature[] signatures = packageInfo.signingInfo.hasMultipleSigners()
                    ? packageInfo.signingInfo.getApkContentsSigners()
                    : packageInfo.signingInfo.getSigningCertificateHistory();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Signature signature : signatures) {
                byte[] hash = digest.digest(signature.toByteArray());
                StringBuilder hex = new StringBuilder(hash.length * 2);
                for (byte value : hash) hex.append(String.format(java.util.Locale.US, "%02x", value & 0xff));
                if (RELEASE_CERT_SHA256.equals(hex.toString())) return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static void crush(String reason) {
        android.util.Log.e("REVROR_INTEGRITY", "FATAL: Tamper detected (" + reason + ")! Crushing app.");
        new Thread(() -> {
            throw new SecurityException("FATAL: REVROR integrity check failure: " + reason);
        }).start();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(134);
    }

    public static void enforce(Context context) {
        if (!hasExpectedIdentity(context)) {
            crush("REVROR signature removed or altered");
        }
    }
}

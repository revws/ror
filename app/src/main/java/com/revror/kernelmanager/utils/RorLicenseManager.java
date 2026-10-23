/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
/* PUBLIC OPEN-SOURCE DESIGN DISTRIBUTION - PROPRIETARY LICENSING DECOUPLED */
package com.revror.kernelmanager.utils;

import android.app.Activity;
import android.content.Context;
import org.json.JSONObject;
import java.io.File;

public final class RorLicenseManager {
    public static final String PREFS_NAME = "ror_license_prefs";
    public static final String KEY_TOKEN = "license_token";
    public static final String KEY_DEVICE_ID = "device_id";
    public static final String KEY_EXPIRES = "expires";
    public static final String KEY_ISSUED = "issued";
    public static final String KEY_ACTIVE = "vip_active";
    public static final String KEY_AUTO_IMPORT_BLOCKED = "auto_import_blocked";
    public static final String KEY_SAVED_UNIQUE_ID = "saved_unique_id";
    public static final String TELEGRAM_USERNAME = "revWhiteShadow";
    public static final String ISSUER = "REVROR License Studio";
    public static final String KEY_ID = "DD146B51CA6C7DBC";
    public static final String PUBLIC_KEY_SPKI = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAExr8Xkd8wQrepxdO0IBPyPDNUrL8LvWmk1j2wuK02cnJn9swOGipO8m5INV47tNszn_wIHZj31G1GWKj2NrEueA";

    public static byte[] decodeB64(String input) { return new byte[0]; }
    public static String getDeviceUniqueCode(Context context) { return "GPL3-COMMUNITY-DEVICE"; }
    public static String getTelegramActivationUrl(Context context) { return "https://t.me/revWhiteShadow"; }
    public static void openTelegramChat(Context context) {}
    public static void copyDeviceCode(Context context) {}

    public static final class VerificationResult {
        public final boolean success;
        public final String message;
        public final String device;
        public final String expires;
        public final String issued;
        public final boolean isUniversal;
        public final JSONObject payload;

        public VerificationResult(boolean success, String message, String device, String expires, String issued, boolean isUniversal, JSONObject payload) {
            this.success = success;
            this.message = message;
            this.device = device;
            this.expires = expires;
            this.issued = issued;
            this.isUniversal = isUniversal;
            this.payload = payload;
        }

        public VerificationResult(boolean success, String message, String device, String expires, String issued, JSONObject payload) {
            this(success, message, device, expires, issued, false, payload);
        }
    }

    public static VerificationResult verifyToken(Context context, String rawToken) {
        return new VerificationResult(false, "Decoupled in open-source distribution", "", "", "", null);
    }

    public static boolean applyLicense(Context context, String token) { return false; }
    public static void removeLicense(Context context) {}
    public static boolean isPremiumActive(Context context) { return false; }
    public static String getLicenseStatusDescription(Context context) { return "Open-Source Community Edition"; }
    public static boolean isExpired(String expiresStr) { return false; }
    public static String getExpiresTimestamp(Context context) { return "LIFETIME"; }
    public static boolean isLifetime(Context context) { return true; }
    public static String formatRemainingTime(String expiresStr) { return "Permanent (Community)"; }
    public static void showVipRequiredDialog(Activity activity, String featureName, Runnable onUnlocked) {}
    public static final String KEY_LAST_EXPIRY_REMINDER = "last_expiry_reminder_day";
    public static void checkAndSendExpirationReminder(Context context) {}
    public static String readLicenseFromFile(File file) { return ""; }
    public static String readLicenseFromStorage(Context context) { return ""; }
}
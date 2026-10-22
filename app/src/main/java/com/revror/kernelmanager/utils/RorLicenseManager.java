package com.revror.kernelmanager.utils;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;

import com.revror.kernelmanager.R;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public final class RorLicenseManager {
    private static final String TAG = "RorLicense";
    private static final java.util.concurrent.ExecutorService IMPORT_EXECUTOR = java.util.concurrent.Executors.newSingleThreadExecutor();
    private static volatile boolean rootImportPending;
    private static volatile long rootImportChecked;
    private static volatile String rootImportedToken;
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

    private RorLicenseManager() {}

    public static byte[] decodeB64(String input) {
        if (input == null) return new byte[0];
        String normalized = input.trim().replace('-', '+').replace('_', '/');
        int mod = normalized.length() % 4;
        if (mod == 2) normalized += "==";
        else if (mod == 3) normalized += "=";
        return Base64.decode(normalized, Base64.DEFAULT);
    }

    public static String getDeviceUniqueCode(Context context) {
        if (context == null) return "UNKNOWN";
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String saved = prefs.getString(KEY_SAVED_UNIQUE_ID, null);
        if (saved != null && !saved.trim().isEmpty()) {
            return saved.trim().toUpperCase(Locale.US);
        }

        String androidId = null;
        try {
            androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Throwable ignored) {}

        if (androidId == null || androidId.trim().isEmpty() || "9774d56d682e549c".equalsIgnoreCase(androidId)) {
            try {
                String hw = Build.BOARD + ":" + Build.BRAND + ":" + Build.DEVICE + ":" + Build.MODEL + ":" + UUID.randomUUID();
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] digest = md.digest(hw.getBytes(StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 8; i++) {
                    sb.append(String.format(Locale.US, "%02X", digest[i]));
                }
                androidId = sb.toString();
            } catch (Throwable e) {
                androidId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            }
        }

        String finalCode = androidId.trim().toUpperCase(Locale.US);
        prefs.edit().putString(KEY_SAVED_UNIQUE_ID, finalCode).apply();
        return finalCode;
    }

    public static String getTelegramActivationUrl(Context context) {
        String code = getDeviceUniqueCode(context);
        String msg = "Hello revWhiteShadow, here is my ROR device code for  Phoenix activation:\n" + code;
        return "https://t.me/" + TELEGRAM_USERNAME + "?text=" + Uri.encode(msg);
    }

    public static void openTelegramChat(Context context) {
        if (context == null) return;
        try {
            String code = getDeviceUniqueCode(context);
            String msg = "Hello revWhiteShadow, here is my ROR device code for  Phoenix activation:\n" + code;
            try {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    ClipData clip = ClipData.newPlainText("Device Code", msg);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, "Code copied! Opening Telegram...", Toast.LENGTH_SHORT).show();
                }
            } catch (Throwable ignored) {}

            Intent tgIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=" + TELEGRAM_USERNAME + "&text=" + Uri.encode(msg)));
            tgIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(tgIntent);
                return;
            } catch (Throwable ignored) {}

            String url = getTelegramActivationUrl(context);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Throwable t) {
            try {
                Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/" + TELEGRAM_USERNAME));
                web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(web);
            } catch (Throwable ignored) {
                Toast.makeText(context, "Telegram not available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static void copyDeviceCode(Context context) {
        if (context == null) return;
        String code = getDeviceUniqueCode(context);
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("REVROR Device Code", code);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Device Code copied: " + code, Toast.LENGTH_SHORT).show();
        }
    }

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
        if (rawToken == null || rawToken.trim().isEmpty()) {
            return new VerificationResult(false, "Enter valid license", null, null, null, false, null);
        }
        String compact = rawToken.trim()
                .replaceAll("^[\"'\"]+|[\"'\"]+$", "")
                .replaceAll("[`]", "")
                .replaceAll("\\s+", "");

        // Find token start if text contains surrounding labels
        int rorIdx = compact.toUpperCase(Locale.US).indexOf("ROR.");
        if (rorIdx != -1) {
            compact = compact.substring(rorIdx);
        } else {
            int gsmIdx = compact.toUpperCase(Locale.US).indexOf("GSM.");
            if (gsmIdx != -1) {
                compact = compact.substring(gsmIdx);
            } else {
                if (compact.split("\\.", -1).length == 2) {
                    compact = "ROR." + compact;
                }
            }
        }

        String[] parts = compact.split("\\.", -1);
        if (parts.length != 3) {
            return new VerificationResult(false, "Enter valid license", null, null, null, false, null);
        }

        String prefix = parts[0].toUpperCase(Locale.US);
        if (!prefix.equals("ROR") && !prefix.equals("GSM")) {
            return new VerificationResult(false, "Invalid license", null, null, null, false, null);
        }

        byte[] payloadBytes;
        byte[] sigBytes;
        try {
            payloadBytes = decodeB64(parts[1]);
            sigBytes = decodeB64(parts[2]);
        } catch (Throwable t) {
            return new VerificationResult(false, "Invalid license", null, null, null, false, null);
        }

        try {
            byte[] pubKeyBytes = decodeB64(PUBLIC_KEY_SPKI);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(pubKeyBytes);
            KeyFactory kf = KeyFactory.getInstance("EC");
            PublicKey publicKey = kf.generatePublic(keySpec);

            String sigAlgo = new String(new byte[]{0x53, 0x48, 0x41, 0x32, 0x35, 0x36, 0x77, 0x69, 0x74, 0x68, 0x45, 0x43, 0x44, 0x53, 0x41}); // SHA256withEC
            Signature sig = Signature.getInstance(sigAlgo);
            sig.initVerify(publicKey);
            sig.update(payloadBytes);
            if (!sig.verify(sigBytes)) {
                return new VerificationResult(false, "Invalid license", null, null, null, false, null);
            }

            JSONObject json = new JSONObject(new String(payloadBytes, StandardCharsets.UTF_8));
            if (json.optInt("v", 1) != 1) {
                return new VerificationResult(false, "Invalid license", null, null, null, false, json);
            }

            String kid = json.optString("kid", "");
            if (!kid.isEmpty() && !KEY_ID.equalsIgnoreCase(kid)) {
                return new VerificationResult(false, "Invalid license", null, null, null, false, json);
            }

            String licDevice = json.optString("device", "").trim().toUpperCase(Locale.US);
            String currentDevice = getDeviceUniqueCode(context);
            boolean isUniversal = "UNIVERSAL".equals(licDevice) || "ALL".equals(licDevice) || "*".equals(licDevice) || licDevice.isEmpty();

            if (!isUniversal && !licDevice.equals(currentDevice)) {
                return new VerificationResult(false, "Invalid license", licDevice, null, null, false, json);
            }

            String expires = json.optString("expires", null);
            String issued = json.optString("issued", null);

            if (isExpired(expires)) {
                return new VerificationResult(false, "Invalid license (Expired)", licDevice, expires, issued, isUniversal, json);
            }

            return new VerificationResult(true, "License is valid", licDevice, expires, issued, isUniversal, json);
        } catch (Throwable t) {
            Log.e(TAG, "License verification error", t);
            return new VerificationResult(false, "Invalid license", null, null, null, false, null);
        }
    }

    public static boolean applyLicense(Context context, String token) {
        if (context == null) return false;
        VerificationResult res = verifyToken(context, token);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (res.success) {
            prefs.edit()
                    .putString(KEY_TOKEN, token.trim())
                    .putString(KEY_DEVICE_ID, res.device)
                    .putString(KEY_EXPIRES, res.expires)
                    .putString(KEY_ISSUED, res.issued)
                    .putBoolean(KEY_ACTIVE, true)
                    .putBoolean(KEY_AUTO_IMPORT_BLOCKED, false)
                    .apply();
            PhoenixBrandManager.sync(context, true);
            return true;
        } else {
            prefs.edit().putBoolean(KEY_ACTIVE, false).apply();
            return false;
        }
    }

    public static void removeLicense(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_DEVICE_ID)
                .remove(KEY_EXPIRES)
                .remove(KEY_ISSUED)
                .putBoolean(KEY_ACTIVE, false)
                .putBoolean(KEY_AUTO_IMPORT_BLOCKED, true)
                .apply();
        PhoenixBrandManager.sync(context, false);
    }

    public static boolean isPremiumActive(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String token = prefs.getString(KEY_TOKEN, null);
        if ((token == null || token.trim().isEmpty())
                && !prefs.getBoolean(KEY_AUTO_IMPORT_BLOCKED, false)) {
            token = readLicenseFromStorage(context);
            if (token != null && !token.trim().isEmpty()) {
                VerificationResult autoCheck = verifyToken(context, token);
                if (autoCheck.success) {
                    applyLicense(context, token);
                    return true;
                }
            }
            return false;
        }
        if (token == null || token.trim().isEmpty()) return false;
        VerificationResult res = verifyToken(context, token);
        boolean active = res.success;
        if (prefs.getBoolean(KEY_ACTIVE, false) != active) {
            prefs.edit().putBoolean(KEY_ACTIVE, active).apply();
        }
        PhoenixBrandManager.sync(context, active);
        return active;
    }

    public static String getLicenseStatusDescription(Context context) {
        if (context == null) return "Free Tier";
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String token = prefs.getString(KEY_TOKEN, null);
        if (token == null || token.trim().isEmpty()) {
            return "Free Tier • License Not Activated";
        }
        VerificationResult res = verifyToken(context, token);
        if (!res.success) {
            return "Invalid license";
        }
        String scope = res.isUniversal ? "Universal" : "Device Locked";
        if (res.expires == null || res.expires.isEmpty() || "lifetime".equalsIgnoreCase(res.expires) || "permanent".equalsIgnoreCase(res.expires)) {
            return "Phoenix Lifetime Active (" + scope + ")";
        }
        return "Phoenix Active (" + scope + ") • " + formatRemainingTime(res.expires);
    }

    public static boolean isExpired(String expiresStr) {
        if (expiresStr == null || expiresStr.trim().isEmpty()
                || "lifetime".equalsIgnoreCase(expiresStr.trim())
                || "permanent".equalsIgnoreCase(expiresStr.trim())
                || "null".equalsIgnoreCase(expiresStr.trim())) {
            return false;
        }
        long now = System.currentTimeMillis();

        // 1. Numeric epoch (seconds or milliseconds)
        try {
            long ts = Long.parseLong(expiresStr.trim());
            if (ts < 10000000000L) ts *= 1000L;
            return now > ts;
        } catch (NumberFormatException ignored) {}

        // 2. ISO-8601 with time (e.g. 2026-10-19T18:05:00Z)
        if (expiresStr.contains("T")) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Instant inst = Instant.parse(expiresStr);
                    return Instant.now().isAfter(inst);
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    Date d = sdf.parse(expiresStr.replace("Z", ""));
                    return d != null && now > d.getTime();
                }
            } catch (Throwable ignored) {}
        }

        // 3. Simple date YYYY-MM-DD
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String today = LocalDate.now().toString();
                return expiresStr.compareTo(today) < 0;
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                Date d = sdf.parse(expiresStr);
                return d != null && now > (d.getTime() + 86400000L);
            }
        } catch (Throwable ignored) {}

        return false;
    }

    public static String getExpiresTimestamp(Context context) {
        if (context == null) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_EXPIRES, null);
    }

    public static boolean isLifetime(Context context) {
        String expires = getExpiresTimestamp(context);
        return expires == null || expires.isEmpty() || "lifetime".equalsIgnoreCase(expires) || "permanent".equalsIgnoreCase(expires);
    }

    public static String formatRemainingTime(String expiresStr) {
        if (expiresStr == null || expiresStr.trim().isEmpty() || "lifetime".equalsIgnoreCase(expiresStr) || "permanent".equalsIgnoreCase(expiresStr)) {
            return "Lifetime / Permanent";
        }
        long targetMs = parseTargetTimeMs(expiresStr);
        if (targetMs <= 0) return "Valid until " + expiresStr;

        long diffMs = targetMs - System.currentTimeMillis();
        if (diffMs <= 0) return "Expired";

        long totalSeconds = diffMs / 1000;
        long seconds = totalSeconds % 60;
        long totalMinutes = totalSeconds / 60;
        long minutes = totalMinutes % 60;
        long totalHours = totalMinutes / 60;
        long hours = totalHours % 24;
        long days = totalHours / 24;

        if (days > 0) {
            return String.format(Locale.US, "%dd %02dh %02dm %02ds remaining", days, hours, minutes, seconds);
        } else {
            return String.format(Locale.US, "%02dh %02dm %02ds remaining", hours, minutes, seconds);
        }
    }

    private static long parseTargetTimeMs(String expiresStr) {
        try {
            long ts = Long.parseLong(expiresStr.trim());
            if (ts < 10000000000L) ts *= 1000L;
            return ts;
        } catch (NumberFormatException ignored) {}

        if (expiresStr.contains("T")) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    return Instant.parse(expiresStr).toEpochMilli();
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    Date d = sdf.parse(expiresStr.replace("Z", ""));
                    if (d != null) return d.getTime();
                }
            } catch (Throwable ignored) {}
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date d = sdf.parse(expiresStr);
            if (d != null) return d.getTime() + 86400000L;
        } catch (Throwable ignored) {}

        return 0;
    }

    public static void showVipRequiredDialog(Activity activity, String featureName, Runnable onUnlocked) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        activity.runOnUiThread(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            View dialogView = LayoutInflater.from(activity).inflate(R.layout.ror_dialog_vip_lock, null);
            AlertDialog dialog = new AlertDialog.Builder(activity)
                    .setView(dialogView)
                    .setCancelable(true)
                    .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            TextView featureTitle = dialogView.findViewById(R.id.dialog_vip_feature_name);
            TextView deviceCodeText = dialogView.findViewById(R.id.dialog_vip_device_code);
            Button btnCopy = dialogView.findViewById(R.id.dialog_vip_btn_copy);
            Button btnTelegram = dialogView.findViewById(R.id.dialog_vip_btn_telegram);
            EditText editToken = dialogView.findViewById(R.id.dialog_vip_input_token);
            Button btnActivate = dialogView.findViewById(R.id.dialog_vip_btn_activate);
            View btnClose = dialogView.findViewById(R.id.dialog_vip_btn_close);

            if (featureTitle != null && featureName != null) {
                featureTitle.setText(featureName.toUpperCase(Locale.US) + " • PHOENIX LOCKED");
            }

            String code = getDeviceUniqueCode(activity);
            if (deviceCodeText != null) {
                deviceCodeText.setText(code);
            }

            if (btnCopy != null) {
                btnCopy.setOnClickListener(v -> copyDeviceCode(activity));
            }

            if (btnTelegram != null) {
                btnTelegram.setOnClickListener(v -> openTelegramChat(activity));
            }

            if (btnActivate != null && editToken != null) {
                btnActivate.setOnClickListener(v -> {
                    String token = editToken.getText().toString().trim();
                    if (token.isEmpty()) {
                        token = readLicenseFromStorage(activity);
                        if (token != null) token = token.trim();
                    }
                    if (token == null || token.isEmpty()) {
                        Toast.makeText(activity, "Enter valid license", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    VerificationResult result = verifyToken(activity, token);
                    if (result.success) {
                        applyLicense(activity, token);
                        Toast.makeText(activity, "License activated!", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                        if (onUnlocked != null) {
                            onUnlocked.run();
                        }
                    } else {
                        Toast.makeText(activity, "Invalid license", Toast.LENGTH_LONG).show();
                    }
                });
            }

            if (btnClose != null) {
                btnClose.setOnClickListener(v -> dialog.dismiss());
            }

            dialog.show();
        });
    }

    public static final String KEY_LAST_EXPIRY_REMINDER = "last_expiry_reminder_day";

    public static void checkAndSendExpirationReminder(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String token = prefs.getString(KEY_TOKEN, null);
        if (token == null || token.trim().isEmpty()) return;

        VerificationResult res = verifyToken(context, token);
        if (!res.success || res.expires == null || res.expires.isEmpty()
                || "lifetime".equalsIgnoreCase(res.expires) || "permanent".equalsIgnoreCase(res.expires)) {
            return;
        }

        long targetMs = parseTargetTimeMs(res.expires);
        if (targetMs <= 0) return;
        long now = System.currentTimeMillis();
        long remainingMs = targetMs - now;
        long twoDaysMs = 2L * 24 * 60 * 60 * 1000L;

        // If expiring within 2 days (48 hours) and still valid
        if (remainingMs > 0 && remainingMs <= twoDaysMs) {
            long todayDay = now / (24 * 60 * 60 * 1000L);
            long lastSentDay = prefs.getLong(KEY_LAST_EXPIRY_REMINDER, 0L);
            if (todayDay != lastSentDay) {
                prefs.edit().putLong(KEY_LAST_EXPIRY_REMINDER, todayDay).apply();
                sendExpiryNotification(context, res.expires);
            }
        }
    }

    private static void sendExpiryNotification(Context context, String expires) {
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            String channelId = "ror_vip_reminders";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel chan = new NotificationChannel(
                        channelId,
                        "REVROR Phoenix Status",
                        NotificationManager.IMPORTANCE_HIGH
                );
                chan.setDescription("Alerts for Phoenix License expiration and renewal");
                nm.createNotificationChannel(chan);
            }

            Intent intent = new Intent(context, com.revror.kernelmanager.activities.DashboardActivity.class);
            intent.putExtra("open_page", 4);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(
                    context,
                    991,
                    intent,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT : PendingIntent.FLAG_UPDATE_CURRENT
            );

            String timeDesc = formatRemainingTime(expires);
            NotificationCompat.Builder b = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_vip_active_badge)
                    .setContentTitle("REVROR Phoenix Expiring Soon (" + timeDesc + ")")
                    .setContentText("Your Phoenix access expires in " + timeDesc + ". Tap to renew and keep Ultra Gaming.")
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("Your REVROR Phoenix License will expire in " + timeDesc + ". Tap here to renew your license with @revWhiteShadow on Telegram to prevent interruption of Ultra Gaming and ad-free experience."))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pi);

            nm.notify(8801, b.build());
        } catch (Throwable t) {
            Log.e(TAG, "Failed to send expiry notification", t);
        }
    }

    public static String readLicenseFromFile(File file) {
        if (file == null || !file.exists() || !file.canRead()) return null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue;
                sb.append(line);
            }
            return sb.toString().trim();
        } catch (Throwable t) {
            Log.e(TAG, "Error reading license file", t);
            return null;
        }
    }

    public static String readLicenseFromStorage(Context context) {
        String[] fileNames = new String[]{"pro_license.txt", "vip_license.txt"};

        // 1. Check Download directory
        try {
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (downloadDir != null) {
                for (String name : fileNames) {
                    File licFile = new File(downloadDir, name);
                    if (licFile.exists()) {
                        String token = readLicenseFromFile(licFile);
                        if (token != null && !token.isEmpty()) return token;
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 2. Check /sdcard root
        try {
            File sdcard = Environment.getExternalStorageDirectory();
            if (sdcard != null) {
                for (String name : fileNames) {
                    File licFile = new File(sdcard, name);
                    if (licFile.exists()) {
                        String token = readLicenseFromFile(licFile);
                        if (token != null && !token.isEmpty()) return token;
                    }
                }
            }
        } catch (Throwable ignored) {}

        // Never wait for the shared root shell from a license check on the UI thread.
        // A profile job can hold that shell for several seconds on slower devices.
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            synchronized (RorLicenseManager.class) {
                if (!rootImportPending && (rootImportChecked == 0 || android.os.SystemClock.uptimeMillis() - rootImportChecked > 30000)) {
                    rootImportPending = true;
                    IMPORT_EXECUTOR.execute(() -> {
                        try { rootImportedToken = readRootLicense(); }
                        finally { rootImportChecked = android.os.SystemClock.uptimeMillis(); rootImportPending = false; }
                    });
                }
            }
            return rootImportedToken;
        }
        return readRootLicense();
    }

    private static String readRootLicense() {
        try {
            if (com.revror.kernelmanager.utils.root.RootUtils.rootAccess()) {
                for (String p : new String[]{"/sdcard/pro_license.txt", "/sdcard/Download/pro_license.txt", "/storage/emulated/0/pro_license.txt"}) {
                    String out = com.revror.kernelmanager.utils.root.RootUtils.runAndGetOutput("cat " + p + " 2>/dev/null").trim();
                    if (!out.isEmpty() && (out.startsWith("ROR.") || out.startsWith("GSM."))) return out;
                }
            }
        } catch (RuntimeException ignored) {}
        return null;
    }
}


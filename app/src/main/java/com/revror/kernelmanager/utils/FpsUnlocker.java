package com.revror.kernelmanager.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.revror.kernelmanager.R;
import com.revror.kernelmanager.utils.root.RootUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * FPS & Graphic Unlocker for games via ro.product.* system identity spoofing.
 * Displays clean marketing device names (e.g. "OnePlus 7", "Sony Xperia 5 II")
 * and hides internal technical model codes (e.g. GM1903, SO-52A).
 */
public class FpsUnlocker {

    private static final String PREF_NAME = "ror_fps_unlocker";
    private static final String KEY_DEFAULT_BRAND = "default_brand";
    private static final String KEY_DEFAULT_MANUFACTURER = "default_manufacturer";
    private static final String KEY_DEFAULT_MODEL = "default_model";
    private static final String KEY_DEFAULT_DEVICE = "default_device";
    private static final String KEY_DEFAULT_NAME = "default_name";
    private static final String KEY_ACTIVE_DEVICE_NAME = "active_device_name";

    public static class DevicePreset {
        public final String deviceName;
        public final String gameTarget;
        public final String fpsTarget;
        public final String brand;
        public final String manufacturer;
        public final String model;
        public final String device;
        public final String name;

        public DevicePreset(String deviceName, String gameTarget, String fpsTarget,
                            String brand, String manufacturer, String model,
                            String device, String name) {
            this.deviceName = deviceName;
            this.gameTarget = gameTarget;
            this.fpsTarget = fpsTarget;
            this.brand = brand;
            this.manufacturer = manufacturer;
            this.model = model;
            this.device = device;
            this.name = name;
        }
    }

    public static List<DevicePreset> getPresets() {
        List<DevicePreset> list = new ArrayList<>();
        // 1. Sony Xperia 5 II - 120 FPS Ultra
        list.add(new DevicePreset(
                "Sony Xperia 5 II",
                "Call of Duty Mobile",
                "120 FPS Ultra",
                "Sony", "Sony", "SO-52A", "SO-52A", "SO-52A"
        ));
        // 2. OnePlus 7 - 90 FPS Extreme (User Requested)
        list.add(new DevicePreset(
                "OnePlus 7",
                "PUBG Mobile / BGMI / Apex",
                "90 FPS Extreme",
                "OnePlus", "OnePlus", "GM1903", "OnePlus7", "OnePlus7"
        ));
        // 3. OnePlus 8 Pro - 90 FPS Smooth
        list.add(new DevicePreset(
                "OnePlus 8 Pro",
                "PUBG Mobile / BGMI",
                "90 FPS Smooth",
                "OnePlus", "OnePlus", "IN2023", "OnePlus8Pro", "OnePlus8Pro"
        ));
        // 4. Xiaomi Mi 10T Pro - 60 FPS HDR Extreme
        list.add(new DevicePreset(
                "Xiaomi Mi 10T Pro",
                "PUBG Mobile HDR Extreme",
                "60 FPS HDR Extreme",
                "Xiaomi", "Xiaomi", "M2006J10C", "apollo", "apollo"
        ));
        // 5. ASUS ROG Phone 5 - 120/144 FPS Global
        list.add(new DevicePreset(
                "ASUS ROG Phone 5",
                "Free Fire, Genshin, All Games",
                "120 / 144 FPS Global",
                "asus", "asus", "ASUS_I005D", "ASUS_I005_1", "WW_I005D"
        ));
        // 6. Samsung Galaxy S9+ - 60/90 FPS
        list.add(new DevicePreset(
                "Samsung Galaxy S9+",
                "Call of Duty Mobile Extreme",
                "60 / 90 FPS",
                "samsung", "samsung", "SM-G965F", "star2lte", "star2ltexx"
        ));
        // 7. Xiaomi Mi 10 Pro - Ultra High Graphics
        list.add(new DevicePreset(
                "Xiaomi Mi 10 Pro",
                "Mobile Legends Ultra",
                "Ultra High FPS & Graphics",
                "Xiaomi", "Xiaomi", "Mi 10 Pro", "cmi", "cmi"
        ));
        // 8. Samsung Galaxy S20 Ultra - 120 FPS Mode
        list.add(new DevicePreset(
                "Samsung Galaxy S20 Ultra",
                "Wild Rift 120 FPS",
                "120 FPS Mode",
                "samsung", "samsung", "SM-G9880", "z3q", "z3qzh"
        ));
        // 9. OPPO R11 Plus - High Frame Rate
        list.add(new DevicePreset(
                "OPPO R11 Plus",
                "Arena of Valor High FPS",
                "High Frame Rate",
                "OPPO", "OPPO", "R11 Plus", "R11Plus", "R11Plus"
        ));
        return list;
    }

    public static void ensureDefaultBackup(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (!sp.contains(KEY_DEFAULT_MODEL)) {
            sp.edit()
                    .putString(KEY_DEFAULT_BRAND, Build.BRAND)
                    .putString(KEY_DEFAULT_MANUFACTURER, Build.MANUFACTURER)
                    .putString(KEY_DEFAULT_MODEL, Build.MODEL)
                    .putString(KEY_DEFAULT_DEVICE, Build.DEVICE)
                    .putString(KEY_DEFAULT_NAME, Build.PRODUCT)
                    .apply();
        }
    }

    public static boolean isSpoofed(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .contains(KEY_ACTIVE_DEVICE_NAME);
    }

    /**
     * Resolves the friendly user-facing device name.
     * If spoofed, returns the spoofed device name.
     * If default, maps known models (e.g. GM1903 -> OnePlus 7, SO-52A -> Sony Xperia 5 II)
     * and strips any technical model codes.
     */
    public static String getDisplayDeviceName(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String active = sp.getString(KEY_ACTIVE_DEVICE_NAME, null);
        if (active != null && !active.trim().isEmpty()) {
            return active;
        }
        return resolveNativeMarketingName(context);
    }

    public static String resolveNativeMarketingName(Context context) {
        String model = Build.MODEL != null ? Build.MODEL.trim() : "";
        String brand = Build.BRAND != null ? Build.BRAND.trim() : "";
        String mfg = Build.MANUFACTURER != null ? Build.MANUFACTURER.trim() : "";
        String dev = Build.DEVICE != null ? Build.DEVICE.trim() : "";

        // 1. Direct Known Model Code Mapping
        String mapped = mapKnownModel(model);
        if (mapped != null) return mapped;
        mapped = mapKnownModel(dev);
        if (mapped != null) return mapped;

        // 2. Query System Settings device_name (OnePlus, Samsung, Xiaomi, Pixel store market names here)
        try {
            String sysName = Settings.Global.getString(context.getContentResolver(), "device_name");
            if (!TextUtils.isEmpty(sysName) && !sysName.equalsIgnoreCase(model) && !sysName.equalsIgnoreCase(dev)) {
                return cleanMarketingString(sysName);
            }
        } catch (Throwable ignored) {}

        // 3. Fallback: Clean up Build.MODEL
        return cleanMarketingString(model.isEmpty() ? mfg + " " + brand : model);
    }

    private static String mapKnownModel(String code) {
        if (code == null) return null;
        String c = code.toUpperCase(Locale.US).replaceAll("[_\\- ]", "");

        // OnePlus 7 Series
        if (c.startsWith("GM190") || c.equals("ONEPLUS7")) return "OnePlus 7";
        if (c.startsWith("GM191") || c.equals("ONEPLUS7PRO")) return "OnePlus 7 Pro";
        if (c.startsWith("HD190") || c.equals("ONEPLUS7T")) return "OnePlus 7T";
        if (c.startsWith("HD191") || c.equals("ONEPLUS7TPRO")) return "OnePlus 7T Pro";
        // OnePlus 8 Series
        if (c.startsWith("IN201") || c.equals("ONEPLUS8")) return "OnePlus 8";
        if (c.startsWith("IN202") || c.equals("ONEPLUS8PRO")) return "OnePlus 8 Pro";
        if (c.startsWith("KB200") || c.equals("ONEPLUS8T")) return "OnePlus 8T";
        // OnePlus 9 Series
        if (c.startsWith("LE211") || c.equals("ONEPLUS9")) return "OnePlus 9";
        if (c.startsWith("LE212") || c.equals("ONEPLUS9PRO")) return "OnePlus 9 Pro";

        // Sony Xperia
        if (c.contains("SO52A") || c.contains("SOG02") || c.contains("XQAS52") || c.contains("XQAS62") || c.contains("XQAS72"))
            return "Sony Xperia 5 II";
        if (c.contains("SO51A") || c.contains("SOG01") || c.contains("XQAT51") || c.contains("XQAT52"))
            return "Sony Xperia 1 II";
        if (c.contains("SO51B") || c.contains("SOG03") || c.contains("XQBC52"))
            return "Sony Xperia 1 III";
        if (c.contains("SO53B") || c.contains("SOG05") || c.contains("XQBQ52"))
            return "Sony Xperia 5 III";

        // Xiaomi / POCO
        if (c.startsWith("M2006J10") || c.startsWith("M2007J3")) return "Xiaomi Mi 10T Pro";
        if (c.equals("CMI") || c.contains("MI10PRO")) return "Xiaomi Mi 10 Pro";
        if (c.equals("VENUS") || c.contains("MI11")) return "Xiaomi Mi 11";
        if (c.equals("APOLLO")) return "Xiaomi Mi 10T";

        // Samsung
        if (c.startsWith("SMG965")) return "Samsung Galaxy S9+";
        if (c.startsWith("SMG960")) return "Samsung Galaxy S9";
        if (c.startsWith("SMG973")) return "Samsung Galaxy S10";
        if (c.startsWith("SMG975")) return "Samsung Galaxy S10+";
        if (c.startsWith("SMG988")) return "Samsung Galaxy S20 Ultra";
        if (c.startsWith("SMG998")) return "Samsung Galaxy S21 Ultra";
        if (c.startsWith("SMS908")) return "Samsung Galaxy S22 Ultra";
        if (c.startsWith("SMS918")) return "Samsung Galaxy S23 Ultra";
        if (c.startsWith("SMS928")) return "Samsung Galaxy S24 Ultra";

        // ASUS ROG
        if (c.startsWith("ASUSI005")) return "ASUS ROG Phone 5";
        if (c.startsWith("ASUSI003")) return "ASUS ROG Phone 3";
        if (c.startsWith("ASUSAI2201")) return "ASUS ROG Phone 6";

        // OPPO
        if (c.contains("R11PLUS")) return "OPPO R11 Plus";

        // Emulators / Generic
        if (c.equals("G576D") || c.contains("GOLDFISH") || c.contains("RANCHU") || c.contains("ANDROIDSDK"))
            return "Android Virtual Device";

        return null;
    }

    private static String cleanMarketingString(String raw) {
        if (raw == null) return "Android Device";
        // Remove parenthesized technical strings e.g. "Sony Xperia 5 II (SO-52A)" -> "Sony Xperia 5 II"
        String cleaned = raw.replaceAll("\\s*\\([^)]*\\)", "").trim();
        // Remove raw codes if present at end
        cleaned = cleaned.replaceAll("\\s*-[A-Z0-9]+$", "").trim();
        return cleaned.isEmpty() ? raw : cleaned;
    }

    /**
     * Displays the custom FPS Unlocker Dialog.
     */
    public static void showDialog(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        ensureDefaultBackup(activity);

        View root = LayoutInflater.from(activity).inflate(R.layout.dialog_fps_unlocker, null);
        TextView activeStatus = root.findViewById(R.id.fps_active_status);
        LinearLayout container = root.findViewById(R.id.fps_presets_container);
        Button btnRestore = root.findViewById(R.id.btn_fps_restore);
        Button btnClose = root.findViewById(R.id.btn_fps_close);

        String activeName = getDisplayDeviceName(activity);
        boolean spoofed = isSpoofed(activity);
        activeStatus.setText("Active: " + activeName + (spoofed ? " [Spoofed]" : " [Default]"));

        AlertDialog dialog = new AlertDialog.Builder(activity, R.style.RorDialogTheme)
                .setView(root)
                .create();

        List<DevicePreset> presets = getPresets();
        for (DevicePreset p : presets) {
            boolean isCur = spoofed && p.deviceName.equalsIgnoreCase(activeName);

            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setBackgroundResource(R.drawable.ror_tool_card_bg);
            row.setPadding(28, 20, 28, 20);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 14);
            row.setLayoutParams(lp);

            LinearLayout headerLine = new LinearLayout(activity);
            headerLine.setOrientation(LinearLayout.HORIZONTAL);

            TextView titleTv = new TextView(activity);
            titleTv.setText((isCur ? " " : " ") + p.deviceName);
            titleTv.setTextColor(isCur ? 0xFF00F5D4 : 0xFFFFFFFF);
            titleTv.setTextSize(13.5f);
            titleTv.getPaint().setFakeBoldText(true);
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            titleTv.setLayoutParams(titleLp);
            headerLine.addView(titleTv);

            if (isCur) {
                TextView activeBadge = new TextView(activity);
                activeBadge.setText("ACTIVE");
                activeBadge.setTextColor(0xFF00F5D4);
                activeBadge.setTextSize(9f);
                activeBadge.getPaint().setFakeBoldText(true);
                headerLine.addView(activeBadge);
            }
            row.addView(headerLine);

            TextView targetTv = new TextView(activity);
            targetTv.setText(p.gameTarget + " • " + p.fpsTarget);
            targetTv.setTextColor(0xFF2DE49A);
            targetTv.setTextSize(10.5f);
            targetTv.setPadding(0, 4, 0, 0);
            row.addView(targetTv);

            row.setOnClickListener(v -> {
                dialog.dismiss();
                applyPreset(activity, p);
            });

            container.addView(row);
        }

        btnRestore.setOnClickListener(v -> {
            dialog.dismiss();
            restoreDefault(activity);
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            int width = (int) (metrics.widthPixels * 0.90);
            int height = (int) (metrics.heightPixels * 0.78);
            dialog.getWindow().setLayout(width, height);
        }
    }

    public static void applyPreset(Activity activity, DevicePreset preset) {
        ensureDefaultBackup(activity);

        new Thread(() -> {
            String script = buildSpoofScript(preset.brand, preset.manufacturer, preset.model, preset.device, preset.name);
            RootUtils.runCommand(script);

            // Write to Magisk/KernelSU service.d & post-fs-data.d for complete boot persistence
            String persistScript = "mkdir -p /data/adb/service.d /data/adb/post-fs-data.d; "
                    + "echo '#!/system/bin/sh' > /data/adb/service.d/ror_fps_spoof.sh; "
                    + "echo '" + script.replace("'", "'\"'\"'") + "' >> /data/adb/service.d/ror_fps_spoof.sh; "
                    + "chmod 0755 /data/adb/service.d/ror_fps_spoof.sh; "
                    + "echo '#!/system/bin/sh' > /data/adb/post-fs-data.d/ror_fps_spoof.sh; "
                    + "echo '" + script.replace("'", "'\"'\"'") + "' >> /data/adb/post-fs-data.d/ror_fps_spoof.sh; "
                    + "chmod 0755 /data/adb/post-fs-data.d/ror_fps_spoof.sh";
            RootUtils.runCommand(persistScript);

            activity.runOnUiThread(() -> {
                activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                        .edit().putString(KEY_ACTIVE_DEVICE_NAME, preset.deviceName).apply();

                TextView heroDevice = activity.findViewById(R.id.hero_device_text);
                if (heroDevice != null) {
                    heroDevice.setText(preset.deviceName);
                }

                Toast.makeText(activity, "Spoofed: " + preset.deviceName, Toast.LENGTH_SHORT).show();
                BootTweakManager.askReboot(activity, "Device spoofed to " + preset.deviceName + ".");
            });
        }).start();
    }

    public static void restoreDefault(Activity activity) {
        SharedPreferences sp = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String brand = sp.getString(KEY_DEFAULT_BRAND, Build.BRAND);
        String manufacturer = sp.getString(KEY_DEFAULT_MANUFACTURER, Build.MANUFACTURER);
        String model = sp.getString(KEY_DEFAULT_MODEL, Build.MODEL);
        String device = sp.getString(KEY_DEFAULT_DEVICE, Build.DEVICE);
        String name = sp.getString(KEY_DEFAULT_NAME, Build.PRODUCT);

        new Thread(() -> {
            String script = buildSpoofScript(brand, manufacturer, model, device, name);
            RootUtils.runCommand(script);
            RootUtils.runCommand("rm -f /data/adb/service.d/ror_fps_spoof.sh /data/adb/post-fs-data.d/ror_fps_spoof.sh; settings delete global ror_spoofed_device 2>/dev/null");

            activity.runOnUiThread(() -> {
                sp.edit().remove(KEY_ACTIVE_DEVICE_NAME).apply();
                String defaultMarketing = resolveNativeMarketingName(activity);
                TextView heroDevice = activity.findViewById(R.id.hero_device_text);
                if (heroDevice != null) {
                    heroDevice.setText(defaultMarketing);
                }
                Toast.makeText(activity, "Default device profile restored", Toast.LENGTH_SHORT).show();
                BootTweakManager.askReboot(activity, "Default device profile restored.");
            });
        }).start();
    }

    public static void applyOnBoot(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String active = sp.getString(KEY_ACTIVE_DEVICE_NAME, null);
        if (active == null) return;

        for (DevicePreset p : getPresets()) {
            if (p.deviceName.equalsIgnoreCase(active)) {
                String script = buildSpoofScript(p.brand, p.manufacturer, p.model, p.device, p.name);
                RootUtils.runCommand(script);
                break;
            }
        }
    }

    public static String buildSpoofScript(String brand, String manufacturer, String model, String device, String name) {
        return "RP=\"\"; "
                + "if command -v resetprop >/dev/null 2>&1; then RP=\"resetprop\"; "
                + "elif command -v magisk >/dev/null 2>&1; then RP=\"magisk resetprop\"; "
                + "elif [ -x /data/adb/magisk/magisk ]; then RP=\"/data/adb/magisk/magisk resetprop\"; "
                + "elif [ -x /data/adb/ksu/bin/resetprop ]; then RP=\"/data/adb/ksu/bin/resetprop\"; "
                + "elif [ -x /data/adb/ap/bin/resetprop ]; then RP=\"/data/adb/ap/bin/resetprop\"; "
                + "elif [ -x /system/bin/resetprop ]; then RP=\"/system/bin/resetprop\"; "
                + "elif [ -x /system/xbin/resetprop ]; then RP=\"/system/xbin/resetprop\"; "
                + "fi; "
                + "sp() { "
                + "  k=\"$1\"; v=\"$2\"; "
                + "  if [ -n \"$RP\" ]; then "
                + "    $RP -n \"$k\" \"$v\" 2>/dev/null || $RP \"$k\" \"$v\" 2>/dev/null; "
                + "  fi; "
                + "  setprop \"$k\" \"$v\" 2>/dev/null; "
                + "}; "
                + "for p in ro.product.brand ro.product.system.brand ro.product.vendor.brand ro.product.odm.brand ro.product.product.brand ro.product.system_ext.brand ro.product.bootimage.brand; do sp \"$p\" \"" + brand + "\"; done; "
                + "for p in ro.product.manufacturer ro.product.system.manufacturer ro.product.vendor.manufacturer ro.product.odm.manufacturer ro.product.product.manufacturer ro.product.system_ext.manufacturer ro.product.bootimage.manufacturer; do sp \"$p\" \"" + manufacturer + "\"; done; "
                + "for p in ro.product.model ro.product.system.model ro.product.vendor.model ro.product.odm.model ro.product.product.model ro.product.system_ext.model ro.product.bootimage.model ro.vendor.product.model; do sp \"$p\" \"" + model + "\"; done; "
                + "for p in ro.product.device ro.product.system.device ro.product.vendor.device ro.product.odm.device ro.product.product.device ro.product.system_ext.device ro.product.bootimage.device ro.vendor.product.device ro.build.product; do sp \"$p\" \"" + device + "\"; done; "
                + "for p in ro.product.name ro.product.system.name ro.product.vendor.name ro.product.odm.name ro.product.product.name ro.product.system_ext.name ro.product.bootimage.name; do sp \"$p\" \"" + name + "\"; done; "
                + "settings put global ror_spoofed_device \"" + model + "\" 2>/dev/null";
    }
}

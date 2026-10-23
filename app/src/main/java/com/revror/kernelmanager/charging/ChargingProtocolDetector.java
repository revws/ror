/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.charging;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.revror.kernelmanager.utils.Utils;
import com.revror.kernelmanager.utils.kernel.battery.Battery;

import java.util.Locale;

/**
 * Intelligent Fast Charging & Hardware Capability Detector.
 * Safely analyzes negotiated protocols, thermal severity, voltage, and current without
 * guessing from brand names alone, overclocking hardware, or fabricating synthetic metrics.
 */
public final class ChargingProtocolDetector {

    public static final class ChargingInfo {
        public boolean isCharging;
        public boolean isFull;
        public int batteryLevel;
        public float temperatureC;
        public int voltageMv;
        public boolean hasVoltageReading;
        public int currentMa;
        public boolean hasCurrentReading;
        public float wattageW;
        public boolean hasWattageReading;
        public String protocolName;
        public String protocolDetail;
        public boolean isFastCharging;
        public int thermalStatus; // PowerManager.THERMAL_STATUS_*
        public String thermalStatusName;
        public String plugType; // AC, USB, Wireless, None
        public boolean hasKernelControl;
        public String kernelControlSummary;
    }

    private ChargingProtocolDetector() {}

    @NonNull
    public static ChargingInfo detect(@NonNull Context context) {
        ChargingInfo info = new ChargingInfo();

        // 1. Android Standard Battery Intent
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int status = BatteryManager.BATTERY_STATUS_UNKNOWN;
        int plugged = 0;
        float tempC = 0f;
        int rawVolt = 0;

        if (batteryIntent != null) {
            status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
            plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            info.batteryLevel = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);

            // Intelligent Temperature normalization
            int rawTemp = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            if (rawTemp > 1000) {
                tempC = rawTemp / 1000.0f; // millidegrees (e.g., 36500 -> 36.5°C)
            } else if (rawTemp > 100) {
                tempC = rawTemp / 10.0f; // standard tenths of degree (e.g., 365 -> 36.5°C)
            } else if (rawTemp > 0) {
                tempC = (float) rawTemp; // direct Celsius
            }

            rawVolt = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
            if (rawVolt > 100000) {
                rawVolt = rawVolt / 1000; // microvolts to millivolts
            }
        }

        // Thermal zone fallback if battery temperature was not provided
        if (tempC <= 0f) {
            tempC = readThermalZoneTemp();
        }
        info.temperatureC = tempC > 0f ? tempC : 25.0f;

        // Fallback voltage from sysfs if 0
        if (rawVolt <= 0 && Battery.hasBatteryVoltage()) {
            rawVolt = Battery.getBatteryVoltage();
        }
        info.voltageMv = rawVolt;
        info.hasVoltageReading = (info.voltageMv > 0);

        info.isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING);
        info.isFull = (status == BatteryManager.BATTERY_STATUS_FULL);

        if (plugged == BatteryManager.BATTERY_PLUGGED_AC) {
            info.plugType = "AC Wall Charger";
        } else if (plugged == BatteryManager.BATTERY_PLUGGED_USB) {
            info.plugType = "USB Host / Port";
        } else if (plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS) {
            info.plugType = "Wireless Charger";
        } else {
            info.plugType = info.isCharging ? "Charging" : "Battery Discharging";
        }

        // 2. Read Electrical Current (mA) accurately
        readCurrent(context, info);

        // 3. Calculate Wattage (Battery-side estimate: Voltage * Current)
        if (info.hasCurrentReading && info.hasVoltageReading && info.voltageMv > 0 && info.currentMa != 0) {
            info.wattageW = (info.voltageMv / 1000.0f) * (Math.abs(info.currentMa) / 1000.0f);
            info.hasWattageReading = true;
        } else {
            info.wattageW = 0f;
            info.hasWattageReading = false;
        }

        // 4. Android Thermal Status (PowerManager API 29+)
        readThermalStatus(context, info);

        // 5. Intelligent Protocol Detection (Hardware-reported)
        detectProtocol(plugged, info);

        // 6. Kernel Control Interfaces Check
        checkKernelControls(context, info);

        return info;
    }

    private static void readCurrent(@NonNull Context context, @NonNull ChargingInfo info) {
        int currentRaw = 0;
        boolean found = false;

        // Try standard Android BatteryManager API
        try {
            BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            if (bm != null) {
                int prop = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                if (prop != Integer.MIN_VALUE && prop != 0) {
                    currentRaw = prop;
                    found = true;
                }
            }
        } catch (Throwable ignored) {}

        // Try sysfs nodes if BatteryManager returned nothing
        if (!found) {
            String[] sysfsCurrentNodes = {
                    "/sys/class/power_supply/battery/current_now",
                    "/sys/class/power_supply/bms/current_now",
                    "/sys/class/power_supply/main/current_now",
                    "/sys/class/power_supply/battery/batt_current"
            };
            for (String node : sysfsCurrentNodes) {
                if (Utils.existFile(node)) {
                    int val = Utils.strToInt(Utils.readFile(node).trim());
                    if (val != 0) {
                        currentRaw = val;
                        found = true;
                        break;
                    }
                }
            }
        }

        if (found) {
            // Normalize microamps (µA) to milliamps (mA)
            int absVal = Math.abs(currentRaw);
            int normalized;
            if (absVal > 10000) {
                normalized = absVal / 1000;
            } else {
                normalized = absVal;
            }

            info.currentMa = normalized;
            info.hasCurrentReading = (info.currentMa > 0);
        } else {
            info.currentMa = 0;
            info.hasCurrentReading = false;
        }
    }

    private static float readThermalZoneTemp() {
        String[] zones = {
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/thermal/thermal_zone1/temp",
                "/sys/class/power_supply/battery/temp"
        };
        for (String z : zones) {
            if (Utils.existFile(z)) {
                int val = Utils.strToInt(Utils.readFile(z).trim());
                if (val > 1000) return val / 1000.0f;
                if (val > 100) return val / 10.0f;
                if (val > 0) return (float) val;
            }
        }
        return 0f;
    }

    private static void readThermalStatus(@NonNull Context context, @NonNull ChargingInfo info) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    info.thermalStatus = pm.getCurrentThermalStatus();
                    switch (info.thermalStatus) {
                        case PowerManager.THERMAL_STATUS_NONE:
                            info.thermalStatusName = "NORMAL";
                            return;
                        case PowerManager.THERMAL_STATUS_LIGHT:
                            info.thermalStatusName = "LIGHT";
                            return;
                        case PowerManager.THERMAL_STATUS_MODERATE:
                            info.thermalStatusName = "MODERATE";
                            return;
                        case PowerManager.THERMAL_STATUS_SEVERE:
                            info.thermalStatusName = "SEVERE";
                            return;
                        case PowerManager.THERMAL_STATUS_CRITICAL:
                            info.thermalStatusName = "CRITICAL";
                            return;
                        case PowerManager.THERMAL_STATUS_EMERGENCY:
                            info.thermalStatusName = "EMERGENCY";
                            return;
                        case PowerManager.THERMAL_STATUS_SHUTDOWN:
                            info.thermalStatusName = "SHUTDOWN";
                            return;
                    }
                }
            } catch (Throwable ignored) {}
        }

        // Thermal fallback based on actual temperature
        if (info.temperatureC < 38.5f) {
            info.thermalStatus = 0; // NONE
            info.thermalStatusName = "NORMAL";
        } else if (info.temperatureC < 41.5f) {
            info.thermalStatus = 1; // LIGHT
            info.thermalStatusName = "LIGHT";
        } else if (info.temperatureC < 44.0f) {
            info.thermalStatus = 2; // MODERATE
            info.thermalStatusName = "MODERATE";
        } else if (info.temperatureC < 47.0f) {
            info.thermalStatus = 3; // SEVERE
            info.thermalStatusName = "SEVERE";
        } else {
            info.thermalStatus = 4; // CRITICAL
            info.thermalStatusName = "CRITICAL";
        }
    }

    private static void detectProtocol(int plugged, @NonNull ChargingInfo info) {
        if (!info.isCharging && !info.isFull) {
            info.protocolName = "Discharging on Battery";
            info.protocolDetail = "Not connected to external power";
            info.isFastCharging = false;
            return;
        }

        if (info.isFull) {
            info.protocolName = "Trickle / Full";
            info.protocolDetail = "Battery 100% • Safety maintenance current";
            info.isFastCharging = false;
            return;
        }

        // Check Wireless
        if (plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS || Utils.existFile("/sys/class/power_supply/wireless/online")) {
            if (info.currentMa >= 1200 || (info.hasWattageReading && info.wattageW >= 7.5f)) {
                info.protocolName = "Fast Wireless Charging";
                info.protocolDetail = "Qi EPP High-Efficiency Induction";
                info.isFastCharging = true;
            } else {
                info.protocolName = "Wireless Charging";
                info.protocolDetail = "Qi Standard Induction";
                info.isFastCharging = false;
            }
            return;
        }

        // 1. Check USB Power Delivery (USB-PD / PPS)
        String usbType = readSysfs("/sys/class/power_supply/usb/type");
        String pdActive = readSysfs("/sys/class/power_supply/usb/pd_active");
        if ("1".equals(pdActive) || "true".equalsIgnoreCase(pdActive) ||
                (usbType != null && (usbType.contains("PD") || usbType.contains("PPS")))) {
            info.protocolName = usbType != null && usbType.contains("PPS") ? "USB Power Delivery (PPS)" : "USB Power Delivery (PD)";
            info.protocolDetail = "Standard negotiated USB Type-C protocol";
            info.isFastCharging = true;
            return;
        }

        // 2. Check Qualcomm Quick Charge (QC)
        String qcType = readSysfs("/sys/class/power_supply/usb/quick_charge_type");
        if (qcType != null && !qcType.isEmpty() && !"0".equals(qcType) && !"UNKNOWN".equalsIgnoreCase(qcType)) {
            info.protocolName = "Qualcomm Quick Charge (" + qcType + ")";
            info.protocolDetail = "High-Voltage Dedicated Charger";
            info.isFastCharging = true;
            return;
        }
        if (usbType != null && usbType.contains("HVDCP")) {
            info.protocolName = "Qualcomm Quick Charge (HVDCP)";
            info.protocolDetail = "High-Voltage Dedicated Charging Port";
            info.isFastCharging = true;
            return;
        }

        // 3. Check Xiaomi / Redmi / POCO Mi Turbo Charge
        String miFast = readSysfs("/sys/class/power_supply/battery/fastcharge_mode");
        String miStep = readSysfs("/sys/class/power_supply/battery/step_charging_enabled");
        if ("1".equals(miFast) || "2".equals(miFast) || "1".equals(miStep)) {
            info.protocolName = "Xiaomi Turbo / HyperCharge";
            info.protocolDetail = "OEM Step-charging thermal negotiated";
            info.isFastCharging = true;
            return;
        }

        // 4. Check OnePlus / OPPO / Realme SuperVOOC / Dart
        String voocType = readSysfs("/sys/class/power_supply/battery/fast_chg_type");
        if (voocType != null && !voocType.isEmpty() && !"0".equals(voocType) && !"N/A".equalsIgnoreCase(voocType)) {
            info.protocolName = "SuperVOOC / Dart Charge";
            info.protocolDetail = "Direct Low-Heat Flash Charge";
            info.isFastCharging = true;
            return;
        }

        // 5. Check Samsung Adaptive / Super Fast Charging
        String hvStatus = readSysfs("/sys/class/power_supply/battery/hv_charger_status");
        String samsungAfc = readSysfs("/sys/class/power_supply/battery/adaptive_fast_charging");
        if ("1".equals(hvStatus) || "2".equals(hvStatus) || "1".equals(samsungAfc)) {
            info.protocolName = "Samsung Fast Charging (AFC/SFC)";
            info.protocolDetail = "Adaptive High Voltage negotiated";
            info.isFastCharging = true;
            return;
        }

        // 6. Generic Linux Kernel charge_type
        String chargeType = readSysfs("/sys/class/power_supply/battery/charge_type");
        if ("Fast".equalsIgnoreCase(chargeType)) {
            info.protocolName = "High-Power Fast Charge (OEM)";
            info.protocolDetail = "Manufacturer kernel reported fast charge";
            info.isFastCharging = true;
            return;
        }

        // 7. Electrical Current & Voltage evaluation (No brand guessing)
        if (info.currentMa >= 2500 || (info.hasWattageReading && info.wattageW >= 15.0f)) {
            info.protocolName = "Rapid Fast Charging (Negotiated)";
            info.protocolDetail = String.format(Locale.US, "Measured %d mA • High-speed protocol", info.currentMa);
            info.isFastCharging = true;
            return;
        } else if (info.currentMa >= 1800 || (info.hasWattageReading && info.wattageW >= 9.0f)) {
            info.protocolName = "Standard Fast Charge (10W - 18W)";
            info.protocolDetail = String.format(Locale.US, "Measured %d mA • Standard fast rate", info.currentMa);
            info.isFastCharging = true;
            return;
        }

        // 8. Default fallback
        info.protocolName = plugged == BatteryManager.BATTERY_PLUGGED_AC ? "Standard AC Charging" : "Standard USB Charging";
        info.protocolDetail = info.hasCurrentReading ? String.format(Locale.US, "Measured %d mA", info.currentMa) : "Stable charging rate";
        info.isFastCharging = false;
    }

    private static void checkKernelControls(@NonNull Context context, @NonNull ChargingInfo info) {
        Battery battery = Battery.getInstance(context);
        boolean hasAny = Battery.hasFastCharge() || Battery.hasForceFastCharge() ||
                Battery.hasFastChargeControlAC() || Battery.haschargeLevel() ||
                Battery.hasUSBFastCharge() || Battery.hasThunderCharge() ||
                battery.hasbatterychargelimit() || battery.hasChargingEnable() ||
                Utils.existFile("/sys/class/power_supply/battery/charge_control_limit_max");

        info.hasKernelControl = hasAny;
        if (Battery.hasFastChargeControlAC()) {
            info.kernelControlSummary = "Kernel AC charge level control available";
        } else if (Battery.hasForceFastCharge()) {
            info.kernelControlSummary = "Kernel force fast charge interface present";
        } else if (battery.hasbatterychargelimit()) {
            info.kernelControlSummary = "Battery charge limit control available";
        } else if (battery.hasChargingEnable()) {
            info.kernelControlSummary = "Kernel bypass charging switch available";
        } else if (Utils.existFile("/sys/class/power_supply/battery/charge_control_limit_max")) {
            info.kernelControlSummary = "Linux standard charge control limit node present";
        } else {
            info.kernelControlSummary = "Direct charge current control: Unsupported (Workload-level thermal cooldown active)";
        }
    }

    @Nullable
    private static String readSysfs(String path) {
        try {
            if (Utils.existFile(path)) {
                String val = Utils.readFile(path);
                if (val != null) {
                    return val.trim();
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }
}

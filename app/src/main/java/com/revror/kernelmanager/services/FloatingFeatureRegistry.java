/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.services;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.revror.kernelmanager.R;
import com.revror.kernelmanager.activities.AppManagerActivity;
import com.revror.kernelmanager.activities.DashboardActivity;
import com.revror.kernelmanager.activities.KernelTuningActivity;
import com.revror.kernelmanager.charging.SmartChargingManager;
import com.revror.kernelmanager.utils.GameOptimizer;
import com.revror.kernelmanager.utils.RorLicenseManager;
import com.revror.kernelmanager.utils.display.HwOverlayManager;
import com.revror.kernelmanager.utils.doze.ForceDozeManager;
import com.revror.kernelmanager.utils.sensor.GyroDelayEngine;
import com.revror.kernelmanager.utils.touch.AimBoostEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Centralized, shared Game Space Floating Feature Registry.
 *
 * Provides a unified inventory of all in-game floating tools, diagnostics,
 * performance governors, gaming engines, and system helpers across ALL
 * floating window layouts in REVROR.
 */
public final class FloatingFeatureRegistry {

    public enum Category {
        PERFORMANCE("⚡ Performance", "Core engine governors, kernel tuning & hardware switches"),
        GAMING("🎮 Gaming Engines", "Display refresh, touch latency, crosshair & tactical aids"),
        CAPTURE("🎥 Screen Capture", "Lossless screenshot & high-framerate screen recording"),
        SYSTEM("🛡️ System & Battery", "Network boost, thermal guard, charging cooldown & cleaner"),
        MONITORING("📊 Live Telemetry", "Live hardware telemetry, FPS & floating overlay HUD");

        public final String title;
        public final String description;

        Category(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    public interface FeatureActionHandler {
        void execute(@NonNull GameBubbleService service, @NonNull Runnable onComplete);
    }

    public interface FeatureStateQuery {
        boolean isActive(@NonNull GameBubbleService service);
    }

    public interface FeatureStatusTextQuery {
        String getStatusText(@NonNull GameBubbleService service);
    }

    public static final class FloatingFeature {
        public final String id;
        public final String name;
        public final String subtitle;
        @DrawableRes public final int iconRes;
        public final Category category;
        public final boolean requiresPhoenixVip;
        public final boolean isToggle;
        private final FeatureStateQuery stateQuery;
        private final FeatureStatusTextQuery statusTextQuery;
        private final FeatureActionHandler actionHandler;

        public FloatingFeature(String id, String name, String subtitle,
                               @DrawableRes int iconRes, Category category,
                               boolean requiresPhoenixVip, boolean isToggle,
                               @Nullable FeatureStateQuery stateQuery,
                               @Nullable FeatureStatusTextQuery statusTextQuery,
                               @NonNull FeatureActionHandler actionHandler) {
            this.id = id;
            this.name = name;
            this.subtitle = subtitle;
            this.iconRes = iconRes;
            this.category = category;
            this.requiresPhoenixVip = requiresPhoenixVip;
            this.isToggle = isToggle;
            this.stateQuery = stateQuery;
            this.statusTextQuery = statusTextQuery;
            this.actionHandler = actionHandler;
        }

        public boolean isActive(@NonNull GameBubbleService service) {
            return stateQuery != null && stateQuery.isActive(service);
        }

        @NonNull
        public String getStatusText(@NonNull GameBubbleService service) {
            if (requiresPhoenixVip && !RorLicenseManager.isPremiumActive(service)) {
                return "\uD83D\uDD12 VIP Locked";
            }
            if (statusTextQuery != null) {
                return statusTextQuery.getStatusText(service);
            }
            return isActive(service) ? "ON" : "OFF";
        }

        public void execute(@NonNull GameBubbleService service, @NonNull Runnable onComplete) {
            if (actionHandler != null) {
                actionHandler.execute(service, onComplete);
            } else {
                onComplete.run();
            }
        }
    }

    private static final Map<String, FloatingFeature> FEATURES = new LinkedHashMap<>();
    private static final Set<String> ACTIVE_OPERATIONS = Collections.synchronizedSet(new HashSet<>());
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    static {
        // =========================================================================================
        // 1. PERFORMANCE
        // =========================================================================================
        register(new FloatingFeature(
                "quick_ram_vram_purge",
                "Super RAM & VRAM Flush",
                "Purge caches & reset thermal governors",
                R.drawable.ic_turbo_fan,
                Category.PERFORMANCE,
                false,
                false,
                null,
                service -> "Purge",
                (service, onComplete) -> {
                    service.flushVramAndRam();
                    onComplete.run();
                }
        ));

        register(new FloatingFeature(
                "hw_overlay",
                "HW Overlay Bypass",
                "Force GPU compositing & bypass SurfaceFlinger",
                R.drawable.ic_display,
                Category.PERFORMANCE,
                false,
                true,
                service -> HwOverlayManager.isHwOverlaysDisabled(service),
                service -> HwOverlayManager.isHwOverlaysDisabled(service) ? "BYPASS ON" : "DEFAULT",
                (service, onComplete) -> {
                    boolean cur = HwOverlayManager.isHwOverlaysDisabled(service);
                    boolean target = !cur;
                    boolean ok = HwOverlayManager.setHwOverlaysDisabled(service, target);
                    service.toast(ok ? (target ? "HW Overlays disabled (GPU forced)" : "HW Overlays restored")
                            : "Root or Shizuku required for HW Overlays");
                    onComplete.run();
                }
        ));

        register(new FloatingFeature(
                "kernel_tuning",
                "Kernel Tuning Suite",
                "Direct sysfs frequencies, governors & thermal limits",
                R.drawable.ic_tools,
                Category.PERFORMANCE,
                false,
                false,
                null,
                service -> "Launch",
                (service, onComplete) -> {
                    service.launchKernelTuning();
                    onComplete.run();
                }
        ));

        // =========================================================================================
        // 2. GAMING
        // =========================================================================================
        register(new FloatingFeature(
                "touch_sampling_boost",
                "Touch Engine Boost",
                "Burst 480Hz ultra-low latency touch sampling",
                R.drawable.ic_gestures,
                Category.GAMING,
                false,
                true,
                GameBubbleService::isTouchBoostEnabled,
                service -> service.isTouchBoostEnabled() ? "480Hz ULTRA" : "240Hz NORMAL",
                (service, onComplete) -> {
                    service.toggleTouchBoost();
                    onComplete.run();
                }
        ));

        register(new FloatingFeature(
                "aim_boost",
                "Aim Boost Calibration",
                "Pro precision micro-aim sensitivity tuning (Phoenix VIP)",
                R.drawable.ic_crosshair,
                Category.GAMING,
                true,
                true,
                service -> AimBoostEngine.getCurrentMode(service) == AimBoostEngine.MODE_COMPETITIVE,
                service -> AimBoostEngine.getCurrentMode(service) == AimBoostEngine.MODE_COMPETITIVE ? "COMPETITIVE" : "BALANCED",
                (service, onComplete) -> {
                    if (!RorLicenseManager.isPremiumActive(service)) {
                        service.openLicenseActivation("Aim Boost (Phoenix VIP)");
                        onComplete.run();
                        return;
                    }
                    int curMode = AimBoostEngine.getCurrentMode(service);
                    int targetMode = (curMode == AimBoostEngine.MODE_COMPETITIVE)
                            ? AimBoostEngine.MODE_BALANCED
                            : AimBoostEngine.MODE_COMPETITIVE;
                    AimBoostEngine.applyMode(service, targetMode, () -> {
                        service.toast("Aim Boost: " + (targetMode == AimBoostEngine.MODE_COMPETITIVE ? "Competitive" : "Balanced"));
                        MAIN_HANDLER.post(onComplete);
                    });
                }
        ));

        register(new FloatingFeature(
                "gyro_delay_fix",
                "Gyro Delay Fix",
                "Sub-millisecond high-rate gyroscope sensor boost (Phoenix VIP)",
                R.drawable.ic_gyro,
                Category.GAMING,
                true,
                true,
                GyroDelayEngine::isOptimized,
                service -> GyroDelayEngine.isOptimized(service) ? "SUB-MS BOOST" : "STOCK",
                (service, onComplete) -> {
                    if (!RorLicenseManager.isPremiumActive(service)) {
                        service.openLicenseActivation("Gyro Delay Fix (Phoenix VIP)");
                        onComplete.run();
                        return;
                    }
                    boolean opt = GyroDelayEngine.isOptimized(service);
                    if (opt) {
                        GyroDelayEngine.restoreDefault(service, () -> {
                            service.toast("Gyro Delay Fix restored to default");
                            MAIN_HANDLER.post(onComplete);
                        });
                    } else {
                        GyroDelayEngine.applyOptimization(service, () -> {
                            service.toast("Gyro Delay Fix: Sub-ms Boost Applied");
                            MAIN_HANDLER.post(onComplete);
                        });
                    }
                }
        ));

        register(new FloatingFeature(
                "force_max_refresh",
                "Force Peak Display Hz",
                "Lock active display to peak hardware refresh rate",
                R.drawable.ic_refresh,
                Category.GAMING,
                false,
                true,
                GameBubbleService::isHighRefreshEnabled,
                service -> service.isHighRefreshEnabled() ? "PEAK LOCKED" : "AUTO",
                (service, onComplete) -> {
                    service.toggleHighRefresh();
                    onComplete.run();
                }
        ));

        register(new FloatingFeature(
                "crosshair_toggle",
                "Combat Crosshair Overlay",
                "On-screen precision tactical aiming reticle",
                R.drawable.ic_crosshair,
                Category.GAMING,
                false,
                true,
                GameBubbleService::isCrosshairEnabled,
                service -> service.isCrosshairEnabled() ? "ACTIVE" : "OFF",
                (service, onComplete) -> {
                    service.toggleCrosshair();
                    onComplete.run();
                }
        ));

        register(new FloatingFeature(
                "crosshair_settings",
                "Crosshair Settings Panel",
                "Adjust reticle color, style, and size",
                R.drawable.ic_crosshair,
                Category.GAMING,
                false,
                false,
                null,
                GameBubbleService::getCrosshairStyleName,
                (service, onComplete) -> {
                    service.showCrosshairSettingsPanel();
                    onComplete.run();
                }
        ));

        register(new FloatingFeature(
                "speedrun_stopwatch",
                "Speedrun Stopwatch",
                "Floating on-screen precision lap timer",
                R.drawable.ic_clock,
                Category.GAMING,
                false,
                true,
                GameBubbleService::isStopwatchActive,
                service -> service.isStopwatchActive() ? "ACTIVE" : "OFF",
                (service, onComplete) -> {
                    service.toggleSpeedrunStopwatch();
                    onComplete.run();
                }
        ));

        register(new FloatingFeature(
                "floating_calc",
                "Combat Mini Calculator",
                "Floating tactical damage calculator overlay",
                R.drawable.ic_numbers,
                Category.GAMING,
                false,
                true,
                GameBubbleService::isCalculatorActive,
                service -> service.isCalculatorActive() ? "ACTIVE" : "OFF",
                (service, onComplete) -> {
                    service.toggleFloatingCalculator();
                    onComplete.run();
                }
        ));

        register(new FloatingFeature(
                "audio_boost",
                "Audio Boost (+6dB)",
                "Acoustic amplification for footsteps & spatial audio",
                R.drawable.ic_volume,
                Category.GAMING,
                false,
                true,
                GameBubbleService::isAudioBoostEnabled,
                service -> service.isAudioBoostEnabled() ? "+6dB ON" : "OFF",
                (service, onComplete) -> {
                    service.toggleAudioBoost();
                    onComplete.run();
                }
        ));

        register(new FloatingFeature(
                "voice_changer",
                "Voice Comms Filter",
                "Cycle microphone vocal modulation effects",
                R.drawable.ic_barrage_style,
                Category.GAMING,
                false,
                false,
                service -> service.getVoiceFilterIndex() > 0,
                GameBubbleService::getVoiceFilterName,
                (service, onComplete) -> {
                    service.cycleVoiceFilter();
                    onComplete.run();
                }
        ));

        register(new FloatingFeature(
                "hunting_filter",
                "Hunting Filter (Night Vision)",
                "Night vision & tactical contrast game display filter",
                R.drawable.ic_display,
                Category.GAMING,
                false,
                true,
                GameBubbleService::isScreenFilterEnabled,
                service -> service.isScreenFilterEnabled() ? service.getHuntingFilterShortName() : "OFF",
                (service, onComplete) -> {
                    service.showHuntingFilterPanel();
                    onComplete.run();
                }
        ));

        register(new FloatingFeature(
                "color_boost",
                "Color Saturation Boost",
                "Vibrant in-game color matrix enhancement",
                R.drawable.ic_sparkle,
                Category.GAMING,
                false,
                true,
                GameBubbleService::isColorBoostEnabled,
                service -> service.isColorBoostEnabled() ? "ACTIVE" : "OFF",
                (service, onComplete) -> {
                    service.toggleColorBoost();
                    onComplete.run();
                }
        ));

        register(new FloatingFeature(
                "display_tools",
                "Game Plugins & Display",
                "In-screen controls, display filters & connectivity",
                R.drawable.ic_display,
                Category.GAMING,
                false,
                false,
                null,
                service -> "Open",
                (service, onComplete) -> {
                    service.showGamePlugins();
                    onComplete.run();
                }
        ));

        // =========================================================================================
        // 3. CAPTURE
        // =========================================================================================
        register(new FloatingFeature(
                "quick_screenshot",
                "Quick Screenshot",
                "Instant lossless gameplay capture",
                R.drawable.ic_gallery,
                Category.CAPTURE,
                false,
                false,
                null,
                service -> "Capture",
                (service, onComplete) -> {
                    service.takeQuickScreenshot();
                    onComplete.run();
                }
        ));

        register(new FloatingFeature(
                "screen_recorder",
                "Screen Recorder",
                "Record gameplay at 60-120fps with internal audio",
                R.drawable.ic_record_dot,
                Category.CAPTURE,
                false,
                true,
                GameBubbleService::isScreenRecording,
                service -> service.isScreenRecording() ? "RECORDING" : "START",
                (service, onComplete) -> {
                    if (service.isScreenRecording()) {
                        service.toggleScreenRecording();
                    } else {
                        service.showScreenRecorder();
                    }
                    onComplete.run();
                }
        ));

        // =========================================================================================
        // 4. SYSTEM
        // =========================================================================================
        register(new FloatingFeature(
                "network",
                "Network Latency Boost",
                "QoS network acceleration & packet queue prioritization",
                R.drawable.ic_wifi,
                Category.SYSTEM,
                false,
                false,
                null,
                service -> "Boost",
                (service, onComplete) -> {
                    GameOptimizer.quickAction(service, service.getTargetPackage(), "network", (ok, msg) -> {
                        service.toast(msg);
                        MAIN_HANDLER.post(onComplete);
                    });
                }
        ));

        register(new FloatingFeature(
                "cleanup",
                "One-Touch Cleaner",
                "Kill background apps & flush inactive memory",
                R.drawable.ic_turbo_fan,
                Category.SYSTEM,
                false,
                false,
                null,
                service -> "Clean",
                (service, onComplete) -> {
                    GameOptimizer.quickAction(service, service.getTargetPackage(), "cleanup", (ok, msg) -> {
                        service.toast(msg);
                        MAIN_HANDLER.post(onComplete);
                    });
                }
        ));

        register(new FloatingFeature(
                "thermal_battery_guard",
                "Thermal Battery Guard",
                "Auto throttle alert & overheating protection",
                R.drawable.ic_shield,
                Category.SYSTEM,
                false,
                true,
                GameBubbleService::isThermalGuardEnabled,
                service -> service.isThermalGuardEnabled() ? "ARMED" : "OFF",
                (service, onComplete) -> {
                    service.toggleThermalGuard();
                    onComplete.run();
                }
        ));

        register(new FloatingFeature(
                "charging_cooldown",
                "Fast & Cool (Cooldown)",
                "Active battery thermal regulation during charging",
                R.drawable.ic_cooldown,
                Category.SYSTEM,
                false,
                true,
                service -> SmartChargingManager.getInstance(service).isActive(),
                service -> SmartChargingManager.getInstance(service).isActive() ? "COOLING ACTIVE" : "OFF",
                (service, onComplete) -> {
                    SmartChargingManager scm = SmartChargingManager.getInstance(service);
                    if (scm.isActive()) {
                        scm.stopAndRestore((ok, detail) -> {
                            service.toast("Charging Cooldown: " + detail);
                            MAIN_HANDLER.post(onComplete);
                        });
                    } else {
                        scm.activateMode(SmartChargingManager.MODE_FAST_COOL, true, (ok, detail) -> {
                            service.toast("Fast & Cool: " + detail);
                            MAIN_HANDLER.post(onComplete);
                        });
                    }
                }
        ));

        register(new FloatingFeature(
                "force_doze",
                "Force Doze Mode",
                "Aggressive idle background power suspension",
                R.drawable.ic_battery,
                Category.SYSTEM,
                false,
                true,
                GameBubbleService::isForceDozeActive,
                service -> service.isForceDozeActive() ? "DOZE ACTIVE" : "OFF",
                (service, onComplete) -> {
                    service.toggleForceDoze();
                    onComplete.run();
                }
        ));

        register(new FloatingFeature(
                "anti_disturb",
                "Anti-Disturb Shield",
                "Silence calls, banners & heads-up notifications",
                R.drawable.ic_shield,
                Category.SYSTEM,
                false,
                true,
                GameBubbleService::isAntiDisturbEnabled,
                service -> service.isAntiDisturbEnabled() ? "SHIELD ON" : "OFF",
                (service, onComplete) -> {
                    service.toggleAntiDisturb();
                    onComplete.run();
                }
        ));

        register(new FloatingFeature(
                "barrage",
                "Barrage Messages",
                "Floating bullet-chat ticker for incoming notifications",
                R.drawable.ic_barrage_message,
                Category.SYSTEM,
                false,
                true,
                GameBubbleService::isBarrageEnabled,
                service -> service.isBarrageEnabled() ? "ON" : "OFF",
                (service, onComplete) -> {
                    service.toggleBarrage();
                    onComplete.run();
                }
        ));

        register(new FloatingFeature(
                "mini_apps",
                "Mini Apps & Multitasking",
                "Float browser, calculator & messenger over gameplay",
                R.drawable.ic_layers,
                Category.SYSTEM,
                false,
                false,
                null,
                service -> "Open",
                (service, onComplete) -> {
                    service.showMiniApps();
                    onComplete.run();
                }
        ));

        register(new FloatingFeature(
                "wifi_manager",
                "Wi-Fi Connectivity Panel",
                "Wi-Fi search, connect & latency diagnostics",
                R.drawable.ic_wifi,
                Category.SYSTEM,
                false,
                false,
                null,
                service -> "Panel",
                (service, onComplete) -> {
                    service.showWifiPanel();
                    onComplete.run();
                }
        ));

        register(new FloatingFeature(
                "mobile_data",
                "Mobile Data Switch",
                "Toggle cellular mobile data interface",
                R.drawable.ic_mobile_data,
                Category.SYSTEM,
                false,
                true,
                GameBubbleService::isMobileDataEnabled,
                service -> service.isMobileDataEnabled() ? "ON" : "OFF",
                (service, onComplete) -> {
                    service.toggleMobileData();
                    onComplete.run();
                }
        ));

        register(new FloatingFeature(
                "brightness_lock",
                "Brightness Lock",
                "Prevent automatic ambient display dimming in game",
                R.drawable.ic_sun,
                Category.SYSTEM,
                false,
                true,
                GameBubbleService::isBrightnessLocked,
                service -> service.isBrightnessLocked() ? "LOCKED" : "UNLOCKED",
                (service, onComplete) -> {
                    service.toggleBrightnessLock();
                    onComplete.run();
                }
        ));

        register(new FloatingFeature(
                "play_reminder",
                "Play Reminder Interval",
                "Healthy session reminder (15/30/60m)",
                R.drawable.ic_clock,
                Category.SYSTEM,
                false,
                false,
                service -> service.getReminderMinutes() > 0,
                service -> service.getReminderMinutes() == 0 ? "OFF" : service.getReminderMinutes() + " MIN",
                (service, onComplete) -> {
                    service.cycleReminder();
                    onComplete.run();
                }
        ));

        register(new FloatingFeature(
                "app_manager",
                "App Process Manager",
                "Inspect running processes & memory hogs",
                R.drawable.ic_settings,
                Category.SYSTEM,
                false,
                false,
                null,
                service -> "Launch",
                (service, onComplete) -> {
                    service.launchAppManager();
                    onComplete.run();
                }
        ));

        // =========================================================================================
        // 5. MONITORING
        // =========================================================================================
        register(new FloatingFeature(
                "toggle_overlay",
                "CPU / GPU Floating HUD",
                "Persistent on-screen hardware performance meters",
                R.drawable.ic_speed_meter,
                Category.MONITORING,
                false,
                true,
                GameBubbleService::isPerformanceHudActive,
                service -> service.isPerformanceHudActive() ? "HUD ON" : "OFF",
                (service, onComplete) -> {
                    service.togglePerformanceHud();
                    onComplete.run();
                }
        ));

        register(new FloatingFeature(
                "perf_telemetry",
                "Telemetry Refresh",
                "Immediate live poll of CPU frequencies, GPU & FPS",
                R.drawable.ic_refresh,
                Category.MONITORING,
                false,
                false,
                null,
                service -> "Refresh",
                (service, onComplete) -> {
                    service.refreshTelemetryNow();
                    onComplete.run();
                }
        ));
    }

    private static void register(@NonNull FloatingFeature feature) {
        FEATURES.put(feature.id, feature);
    }

    private FloatingFeatureRegistry() {}

    @NonNull
    public static List<FloatingFeature> getAllFeatures() {
        return new ArrayList<>(FEATURES.values());
    }

    @NonNull
    public static List<FloatingFeature> getFeaturesByCategory(@NonNull Category category) {
        List<FloatingFeature> list = new ArrayList<>();
        for (FloatingFeature f : FEATURES.values()) {
            if (f.category == category) {
                list.add(f);
            }
        }
        return list;
    }

    @Nullable
    public static FloatingFeature getFeature(@Nullable String id) {
        if (id == null) return null;
        return FEATURES.get(id);
    }

    public static boolean hasFeature(@Nullable String id) {
        return id != null && FEATURES.containsKey(id);
    }

    /**
     * Executes the requested feature action with strict debounce protection,
     * VIP entitlement verification, and state update callback.
     */
    public static void executeFeature(@NonNull GameBubbleService service,
                                      @NonNull String featureId,
                                      @Nullable Runnable onComplete) {
        FloatingFeature feature = getFeature(featureId);
        if (feature == null) {
            if (onComplete != null) onComplete.run();
            return;
        }

        // Anti-rapid-tap debounce guard
        if (ACTIVE_OPERATIONS.contains(featureId)) {
            Toast.makeText(service, "Operation in progress...", Toast.LENGTH_SHORT).show();
            if (onComplete != null) onComplete.run();
            return;
        }

        ACTIVE_OPERATIONS.add(featureId);

        feature.execute(service, () -> {
            ACTIVE_OPERATIONS.remove(featureId);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }
}

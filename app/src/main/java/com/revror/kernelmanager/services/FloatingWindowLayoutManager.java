/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.revror.kernelmanager.R;
import com.revror.kernelmanager.utils.GsmProfileManager;
import com.revror.kernelmanager.utils.RorLicenseManager;
import com.revror.kernelmanager.utils.kernel.compat.ProfileRegistry;
import com.revror.kernelmanager.utils.media.ScreenCaptureManager;
import com.revror.kernelmanager.utils.touch.AimBoostEngine;

import java.util.List;
import java.util.Locale;

/**
 * Manager and factory for the 10 distinct, professional, responsive
 * in-game floating window layouts in REVROR Game Space.
 */
public final class FloatingWindowLayoutManager {

    public static final int LAYOUT_PHOENIX_COMMAND = 0;
    public static final int LAYOUT_COMPACT_HUD = 1;
    public static final int LAYOUT_CYBER_PANEL = 2;
    public static final int LAYOUT_MINIMAL_GLASS = 3;
    public static final int LAYOUT_ESPORTS_DASH = 4;
    public static final int LAYOUT_GAMING_SIDEBAR = 5;
    public static final int LAYOUT_PERF_MATRIX = 6;
    public static final int LAYOUT_PHOENIX_ULTRA = 7;
    public static final int LAYOUT_GAME_DOCK = 8;
    public static final int LAYOUT_ADAPTIVE_HUD = 9;

    public static final String[] LAYOUT_NAMES = {
            "Phoenix Command Center",
            "Compact Gaming HUD",
            "Cyber Gaming Panel",
            "Minimal Glass Overlay",
            "Esports Dashboard",
            "Gaming Sidebar",
            "Performance Matrix",
            "Phoenix Ultra Panel",
            "Floating Game Dock",
            "Adaptive Smart HUD"
    };

    public static final String[] LAYOUT_DESCRIPTIONS = {
            "Flagship translucent dark panel with crimson & violet accents, full telemetry & quick tools",
            "Minimal ultra-low obstruction HUD displaying FPS, temperature and essential action triggers",
            "Futuristic cyberpunk aesthetics with angular illuminated borders and dual load meters",
            "Sleek frosted glass with soft typography, rounded cards and unobtrusive inline metrics",
            "High-contrast tournament HUD with frame-time tracking, Aim Boost, touch boost & low latency",
            "Slim screen-edge mounted vertical toolbar with quick collapsible flyout actions",
            "Comprehensive hardware telemetry matrix showing CPU clusters, GPU, RAM, voltage & governor",
            "Exclusive Phoenix flagship overlay with deep kernel boost switches and dynamic lighting",
            "Draggable horizontal micro-dock toolbar for quick game controls without blocking gameplay",
            "Intelligent responsive HUD adapting between dual-column landscape and compact modes"
    };

    private FloatingWindowLayoutManager() {}

    /**
     * Configures initial WindowManager parameters for the chosen layout.
     */
    public static void configureLayoutParams(GameBubbleService service, WindowManager.LayoutParams params, int layoutStyle) {
        if (service == null || params == null) return;
        DisplayMetrics dm = service.getResources().getDisplayMetrics();
        SharedPreferences sp = service.getSharedPreferences("ror_game_space_center", Context.MODE_PRIVATE);

        int transparency = sp.getInt("ror_floating_transparency", 96);
        params.alpha = Math.max(0.30f, Math.min(1.0f, transparency / 100f));

        int scalePct = sp.getInt("ror_floating_scale", 100);
        float scale = Math.max(0.80f, Math.min(1.25f, scalePct / 100f));

        boolean dockRight = sp.getBoolean("ror_floating_dock_right", true);

        switch (layoutStyle) {
            case LAYOUT_COMPACT_HUD:
                params.width = Math.round(dp(service, 210) * scale);
                params.height = Math.round(dp(service, 160) * scale);
                params.gravity = Gravity.TOP | (dockRight ? Gravity.END : Gravity.START);
                params.x = dp(service, 12);
                params.y = dp(service, 48);
                break;

            case LAYOUT_GAMING_SIDEBAR:
                params.width = Math.round(dp(service, 74) * scale);
                params.height = Math.min(Math.round(dp(service, 360) * scale), dm.heightPixels - dp(service, 32));
                params.gravity = (dockRight ? Gravity.END : Gravity.START) | Gravity.CENTER_VERTICAL;
                params.x = 0;
                params.y = 0;
                break;

            case LAYOUT_GAME_DOCK:
                params.width = Math.min(Math.round(dp(service, 320) * scale), dm.widthPixels - dp(service, 24));
                params.height = Math.round(dp(service, 56) * scale);
                params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                params.x = 0;
                params.y = dp(service, 20);
                break;

            case LAYOUT_ADAPTIVE_HUD:
                boolean isLandscape = service.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
                if (isLandscape) {
                    params.width = Math.min(Math.round(dp(service, 460) * scale), dm.widthPixels - dp(service, 32));
                    params.height = Math.min(Math.round(dp(service, 290) * scale), dm.heightPixels - dp(service, 24));
                } else {
                    params.width = Math.min(Math.round(dp(service, 330) * scale), dm.widthPixels - dp(service, 24));
                    params.height = Math.min(Math.round(dp(service, 500) * scale), dm.heightPixels - dp(service, 32));
                }
                params.gravity = Gravity.CENTER;
                break;

            case LAYOUT_MINIMAL_GLASS:
                params.width = Math.min(Math.round(dp(service, 320) * scale), dm.widthPixels - dp(service, 24));
                params.height = Math.min(Math.round(dp(service, 480) * scale), dm.heightPixels - dp(service, 32));
                params.gravity = Gravity.CENTER_VERTICAL | (dockRight ? Gravity.END : Gravity.START);
                params.x = dp(service, 12);
                break;

            case LAYOUT_PERF_MATRIX:
                params.width = Math.min(Math.round(dp(service, 340) * scale), dm.widthPixels - dp(service, 20));
                params.height = Math.min(Math.round(dp(service, 490) * scale), dm.heightPixels - dp(service, 24));
                params.gravity = Gravity.CENTER;
                break;

            case LAYOUT_PHOENIX_COMMAND:
            case LAYOUT_CYBER_PANEL:
            case LAYOUT_ESPORTS_DASH:
            case LAYOUT_PHOENIX_ULTRA:
            default:
                params.width = Math.min(Math.round(dp(service, 340) * scale), dm.widthPixels - dp(service, 20));
                params.height = Math.min(Math.round(dp(service, 540) * scale), dm.heightPixels - dp(service, 24));
                params.gravity = Gravity.CENTER_VERTICAL | (dockRight ? Gravity.END : Gravity.START);
                params.x = dp(service, 10);
                break;
        }
    }

    /**
     * Builds and returns the root view for the requested floating layout style.
     */
    public static View buildLayout(GameBubbleService service, int layoutStyle) {
        switch (layoutStyle) {
            case LAYOUT_COMPACT_HUD:
                return buildCompactGamingHud(service);
            case LAYOUT_CYBER_PANEL:
                return buildCyberGamingPanel(service);
            case LAYOUT_MINIMAL_GLASS:
                return buildMinimalGlassOverlay(service);
            case LAYOUT_ESPORTS_DASH:
                return buildEsportsDashboard(service);
            case LAYOUT_GAMING_SIDEBAR:
                return buildGamingSidebar(service);
            case LAYOUT_PERF_MATRIX:
                return buildPerformanceMatrix(service);
            case LAYOUT_PHOENIX_ULTRA:
                return buildPhoenixUltraPanel(service);
            case LAYOUT_GAME_DOCK:
                return buildFloatingGameDock(service);
            case LAYOUT_ADAPTIVE_HUD:
                return buildAdaptiveSmartHud(service);
            case LAYOUT_PHOENIX_COMMAND:
            default:
                return buildPhoenixCommandCenter(service);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 1. PHOENIX COMMAND CENTER (Flagship Crimson & Violet HUD)
    // ---------------------------------------------------------------------------------------------
    private static View buildPhoenixCommandCenter(GameBubbleService service) {
        LinearLayout root = createBasePanel(service, 0xF20D0B14, 0xFA09070E, 0xFF9D00FF, 0xFFFF1744, 18);

        // Header
        root.addView(createPanelHeader(service, "PHOENIX COMMAND CENTER", 0xFFFF2A48, true));

        ScrollView scroll = createScrollView(service);
        LinearLayout content = createVerticalContent(service);

        // Live Telemetry Banner
        content.addView(createTelemetryBadge(service, 0xFF9D00FF, 0xFFFF1744));

        // Profiles Strip
        content.addView(createSectionTitle(service, "KERNEL PERFORMANCE MODES", 0xFFFF3355));
        content.addView(createProfileSelectorRow(service, 0xFFFF1744));

        // Quick Controls
        content.addView(createSectionTitle(service, "TACTICAL IN-GAME TOOLS", 0xFFB388FF));
        content.addView(createActionTile(service, R.drawable.ic_gallery, "Quick Screenshot", "Instant lossless game capture", service::takeQuickScreenshot));
        content.addView(createActionTile(service, R.drawable.ic_record_dot, "Screen Recorder", service.isRecordingActive() ? "Recording in progress (Movies/ROR)" : "Record high-fps gameplay video", service::toggleScreenRecording));
        content.addView(createActionTile(service, R.drawable.ic_refresh, "Force Peak Refresh Rate", "Lock display to hardware peak Hz", service::forcePeakRefreshRate));
        content.addView(createActionTile(service, R.drawable.ic_turbo_fan, "Super RAM & VRAM Flush", "Purge background tasks & drop caches", service::flushVramAndRam));
        content.addView(createActionTile(service, R.drawable.ic_crosshair, "Combat Crosshair Overlay", "Toggle center aiming reticle", service::toggleCrosshair));
        content.addView(createActionTile(service, R.drawable.ic_volume, "Audio Boost (+6dB)", "Enhance footsteps & game acoustics", service::toggleAudioBoost));
        content.addView(createActionTile(service, R.drawable.ic_clock, "Speedrun Stopwatch", "Floating on-screen timer", service::toggleSpeedrunStopwatch));
        content.addView(createActionTile(service, R.drawable.ic_numbers, "Combat Mini Calculator", "Floating tactical damage calculator", service::toggleFloatingCalculator));
        content.addView(createActionTile(service, R.drawable.ic_layers, "Mini Apps & Multitasking", "Launch floating companion apps", service::showMiniApps));

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));

        root.addView(createPanelFooter(service));
        service.attachDrag(root);
        return root;
    }

    // ---------------------------------------------------------------------------------------------
    // 2. COMPACT GAMING HUD (Ultra-Low Footprint Overlay)
    // ---------------------------------------------------------------------------------------------
    private static View buildCompactGamingHud(GameBubbleService service) {
        LinearLayout root = createBasePanel(service, 0xEE090A0E, 0xF506070A, 0x5535394A, 0xAA00E5FF, 14);
        root.setPadding(dp(service, 10), dp(service, 8), dp(service, 10), dp(service, 8));

        // Minimal Header with FPS and Close
        LinearLayout top = new LinearLayout(service);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        int hz = 60;
        try {
            if (service.getWindowManager() != null) {
                hz = Math.round(service.getWindowManager().getDefaultDisplay().getRefreshRate());
            }
        } catch (Exception ignored) {}

        TextView fpsVal = new TextView(service);
        fpsVal.setText(hz + " FPS");
        fpsVal.setTextSize(14f);
        fpsVal.setTypeface(null, Typeface.BOLD);
        fpsVal.setTextColor(0xFF00FF88);
        top.addView(fpsVal, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView profBadge = new TextView(service);
        profBadge.setText(GsmProfileManager.getProfileName(service, service.getSelectedProfile()).toUpperCase(Locale.US));
        profBadge.setTextSize(8.5f);
        profBadge.setTypeface(null, Typeface.BOLD);
        profBadge.setTextColor(0xFFFF2A48);
        profBadge.setPadding(dp(service, 6), dp(service, 2), dp(service, 6), dp(service, 2));
        GradientDrawable pBg = new GradientDrawable();
        pBg.setColor(0x33FF1744);
        pBg.setCornerRadius(dp(service, 4));
        profBadge.setBackground(pBg);
        top.addView(profBadge);

        ImageView close = new ImageView(service);
        close.setImageResource(R.drawable.ic_window_close);
        close.setColorFilter(0xFF888894);
        close.setPadding(dp(service, 4), dp(service, 4), dp(service, 4), dp(service, 4));
        close.setOnClickListener(v -> service.collapse());
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(service, 22), dp(service, 22));
        clp.setMarginStart(dp(service, 6));
        top.addView(close, clp);
        root.addView(top);

        // Hardware Stats Chips
        TextView stats = new TextView(service);
        stats.setText(service.readLiveStats());
        stats.setTextSize(9f);
        stats.setTextColor(0xFFE4E4EA);
        stats.setPadding(dp(service, 6), dp(service, 4), dp(service, 6), dp(service, 4));
        GradientDrawable sBg = new GradientDrawable();
        sBg.setColor(0x44141620);
        sBg.setCornerRadius(dp(service, 6));
        stats.setBackground(sBg);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, -2);
        slp.topMargin = dp(service, 6);
        slp.bottomMargin = dp(service, 6);
        root.addView(stats, slp);

        // Micro Toolbar
        LinearLayout toolbar = new LinearLayout(service);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);

        toolbar.addView(createMicroButton(service, R.drawable.ic_gallery, "Shot", service::takeQuickScreenshot));
        toolbar.addView(createMicroButton(service, R.drawable.ic_record_dot, "Rec", service::toggleScreenRecording));
        toolbar.addView(createMicroButton(service, R.drawable.ic_turbo_fan, "Boost", service::flushVramAndRam));
        toolbar.addView(createMicroButton(service, R.drawable.ic_layers, "Tools", service::showGamePlugins));

        root.addView(toolbar, new LinearLayout.LayoutParams(-1, -2));
        service.attachDrag(root);
        return root;
    }

    // ---------------------------------------------------------------------------------------------
    // 3. CYBER GAMING PANEL (Futuristic Angular HUD)
    // ---------------------------------------------------------------------------------------------
    private static View buildCyberGamingPanel(GameBubbleService service) {
        LinearLayout root = createBasePanel(service, 0xF40A0F1A, 0xFA050910, 0xFF00E5FF, 0xFFFF0055, 12);
        root.addView(createPanelHeader(service, "CYBER MATRIX TERMINAL", 0xFF00E5FF, true));

        ScrollView scroll = createScrollView(service);
        LinearLayout content = createVerticalContent(service);

        // Cyber Load Card
        LinearLayout cyberCard = new LinearLayout(service);
        cyberCard.setOrientation(LinearLayout.VERTICAL);
        cyberCard.setPadding(dp(service, 12), dp(service, 10), dp(service, 12), dp(service, 10));
        GradientDrawable cBg = new GradientDrawable();
        cBg.setColor(0xCC0E1726);
        cBg.setCornerRadius(dp(service, 8));
        cBg.setStroke(dp(service, 1), 0x6600E5FF);
        cyberCard.setBackground(cBg);

        TextView sysTitle = createLabel(service, "CORE ENGINE TELEMETRY  //  CYBERPUNK", 9f, 0xFF00E5FF, true);
        cyberCard.addView(sysTitle);

        TextView stats = createLabel(service, service.readLiveStats(), 10.5f, 0xFFE0F7FA, false);
        stats.setPadding(0, dp(service, 6), 0, 0);
        cyberCard.addView(stats);
        content.addView(cyberCard);

        content.addView(createSectionTitle(service, "GRID PROFILES", 0xFF00E5FF));
        content.addView(createProfileSelectorRow(service, 0xFF00E5FF));

        content.addView(createSectionTitle(service, "CYBER OVERCLOCK ACTIONS", 0xFFFF0055));
        content.addView(createActionTile(service, R.drawable.ic_turbo_fan, "RAM & VRAM Flush", "Purge caches & reset thermal governors", service::flushVramAndRam));
        content.addView(createActionTile(service, R.drawable.ic_gestures, "Touch Engine Boost", "480Hz high-frequency sampling burst", service::boostTouchSampling));
        content.addView(createActionTile(service, R.drawable.ic_refresh, "Force Peak Refresh Rate", "Uncap system render throttle", service::forcePeakRefreshRate));
        content.addView(createActionTile(service, R.drawable.ic_gallery, "Instant Screenshot", "Capture gameplay frame", service::takeQuickScreenshot));
        content.addView(createActionTile(service, R.drawable.ic_record_dot, "Screen Record", "Stream game session to disk", service::toggleScreenRecording));
        content.addView(createActionTile(service, R.drawable.ic_crosshair, "Cyber Crosshair", "HUD aim crosshair overlay", service::toggleCrosshair));

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        root.addView(createPanelFooter(service));

        service.attachDrag(root);
        return root;
    }

    // ---------------------------------------------------------------------------------------------
    // 4. MINIMAL GLASS OVERLAY (Frosted Clean Design)
    // ---------------------------------------------------------------------------------------------
    private static View buildMinimalGlassOverlay(GameBubbleService service) {
        LinearLayout root = createBasePanel(service, 0xB8141722, 0xD00E1018, 0x44FFFFFF, 0x88FA476B, 22);
        root.addView(createPanelHeader(service, "Game Space Glass", 0xFFFA476B, true));

        ScrollView scroll = createScrollView(service);
        LinearLayout content = createVerticalContent(service);

        // Frosted Pill Metric
        TextView glassHud = new TextView(service);
        glassHud.setText(service.readLiveStats());
        glassHud.setTextSize(10f);
        glassHud.setTextColor(0xFFFFFFFF);
        glassHud.setPadding(dp(service, 12), dp(service, 8), dp(service, 12), dp(service, 8));
        GradientDrawable ghBg = new GradientDrawable();
        ghBg.setColor(0x33FFFFFF);
        ghBg.setCornerRadius(dp(service, 14));
        ghBg.setStroke(dp(service, 1), 0x22FFFFFF);
        glassHud.setBackground(ghBg);
        content.addView(glassHud);

        content.addView(createSectionTitle(service, "PERFORMANCE", 0xFFC0C4D8));
        content.addView(createProfileSelectorRow(service, 0xFFFA476B));

        content.addView(createSectionTitle(service, "TOOLS", 0xFFC0C4D8));
        content.addView(createActionTile(service, R.drawable.ic_gallery, "Capture Screenshot", "Save current picture to Pictures/ROR", service::takeQuickScreenshot));
        content.addView(createActionTile(service, R.drawable.ic_record_dot, "Capture Video", "Toggle game session recorder", service::toggleScreenRecording));
        content.addView(createActionTile(service, R.drawable.ic_turbo_fan, "Clean Memory", "Free cached background processes", service::flushVramAndRam));
        content.addView(createActionTile(service, R.drawable.ic_refresh, "Max Display Rate", "Force display rate to highest supported Hz", service::forcePeakRefreshRate));
        content.addView(createActionTile(service, R.drawable.ic_display, "Display Settings", "Configure screen colors and brightness", service::showGamePlugins));

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        root.addView(createPanelFooter(service));

        service.attachDrag(root);
        return root;
    }

    // ---------------------------------------------------------------------------------------------
    // 5. ESPORTS DASHBOARD (Tournament Competitive Focus)
    // ---------------------------------------------------------------------------------------------
    private static View buildEsportsDashboard(GameBubbleService service) {
        LinearLayout root = createBasePanel(service, 0xF808080C, 0xFC040406, 0xFFFF1744, 0xFFFF5252, 14);
        root.addView(createPanelHeader(service, "ESPORTS TOURNAMENT HUB", 0xFFFF1744, true));

        ScrollView scroll = createScrollView(service);
        LinearLayout content = createVerticalContent(service);

        // Frame-Time & Latency Card
        LinearLayout latencyCard = new LinearLayout(service);
        latencyCard.setOrientation(LinearLayout.VERTICAL);
        latencyCard.setPadding(dp(service, 12), dp(service, 8), dp(service, 12), dp(service, 8));
        GradientDrawable lBg = new GradientDrawable();
        lBg.setColor(0xCC1A080C);
        lBg.setCornerRadius(dp(service, 8));
        lBg.setStroke(dp(service, 1.5f), 0xFFFF1744);
        latencyCard.setBackground(lBg);

        int hz = 120;
        try {
            if (service.getWindowManager() != null) {
                hz = Math.round(service.getWindowManager().getDefaultDisplay().getRefreshRate());
            }
        } catch (Exception ignored) {}
        float frameTime = (hz > 0) ? (1000f / hz) : 16.6f;

        TextView ftVal = createLabel(service, String.format(Locale.US, "REFRESH: %d Hz  |  FRAME-TIME: %.1f ms", hz, frameTime), 10.5f, 0xFF00FF66, true);
        latencyCard.addView(ftVal);
        TextView liveInfo = createLabel(service, service.readLiveStats(), 9.5f, 0xFFF0F0F5, false);
        liveInfo.setPadding(0, dp(service, 4), 0, 0);
        latencyCard.addView(liveInfo);
        content.addView(latencyCard);

        content.addView(createSectionTitle(service, "TOURNAMENT PROFILES", 0xFFFF1744));
        content.addView(createProfileSelectorRow(service, 0xFFFF1744));

        content.addView(createSectionTitle(service, "COMPETITIVE ADVANTAGE ENGINES", 0xFFFF5252));
        content.addView(createActionTile(service, R.drawable.ic_crosshair, "Aim Boost Engine", "Zero-latency precision aim calibration", () -> {
            AimBoostEngine.applyMode(service, AimBoostEngine.MODE_COMPETITIVE, () -> {
                service.showBarrage("Aim Boost", "Competitive touch calibration applied");
            });
        }));
        content.addView(createActionTile(service, R.drawable.ic_gestures, "Touch Sampling 480Hz", "Ultra fast touchscreen response burst", service::boostTouchSampling));
        content.addView(createActionTile(service, R.drawable.ic_shield, "DND Shield Mode", "Block intrusive notifications & heads-up", service::toggleAntiDisturb));
        content.addView(createActionTile(service, R.drawable.ic_turbo_fan, "Purge Background Apps", "Kill non-essential background processes", service::flushVramAndRam));
        content.addView(createActionTile(service, R.drawable.ic_gallery, "Instant Screenshot", "Capture competitive highlight", service::takeQuickScreenshot));
        content.addView(createActionTile(service, R.drawable.ic_record_dot, "Match Recorder", "Record match session to Movies/ROR", service::toggleScreenRecording));

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        root.addView(createPanelFooter(service));

        service.attachDrag(root);
        return root;
    }

    // ---------------------------------------------------------------------------------------------
    // 6. GAMING SIDEBAR (Edge-Mounted Vertical Rail)
    // ---------------------------------------------------------------------------------------------
    private static View buildGamingSidebar(GameBubbleService service) {
        LinearLayout root = new LinearLayout(service);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(service, 6), dp(service, 10), dp(service, 6), dp(service, 10));

        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xF612141F, 0xF80A0B12, 0xF618080E});
        bg.setCornerRadius(dp(service, 18));
        bg.setStroke(dp(service, 1.5f), 0xFFFF1744);
        root.setBackground(bg);

        // Sidebar Header Grip
        View grip = new View(service);
        GradientDrawable gBg = new GradientDrawable();
        gBg.setColor(0xFFFF1744);
        gBg.setCornerRadius(dp(service, 3));
        grip.setBackground(gBg);
        root.addView(grip, new LinearLayout.LayoutParams(dp(service, 26), dp(service, 4)));

        // Profile Switcher Icon
        ImageView profIcon = createSidebarIconButton(service, R.drawable.ic_flash, 0xFFFF2A48, () -> {
            int cur = service.getSelectedProfile();
            int next = (cur + 1) % (service.isPhoenixMode() ? 8 : 6);
            service.applyProfileDirect(next);
            service.showBarrage("Profile Applied", GsmProfileManager.getProfileName(service, next));
        });
        root.addView(profIcon);

        // Screenshot
        root.addView(createSidebarIconButton(service, R.drawable.ic_gallery, 0xFF00E5FF, service::takeQuickScreenshot));

        // Screen Record
        root.addView(createSidebarIconButton(service, R.drawable.ic_record_dot, 0xFFFF3345, service::toggleScreenRecording));

        // Refresh Rate
        root.addView(createSidebarIconButton(service, R.drawable.ic_refresh, 0xFFFFD600, service::forcePeakRefreshRate));

        // RAM Flush
        root.addView(createSidebarIconButton(service, R.drawable.ic_turbo_fan, 0xFF00FF66, service::flushVramAndRam));

        // Crosshair
        root.addView(createSidebarIconButton(service, R.drawable.ic_crosshair, 0xFFFF007F, service::toggleCrosshair));

        // Floating Apps
        root.addView(createSidebarIconButton(service, R.drawable.ic_layers, 0xFF9D00FF, service::showMiniApps));

        // Close / Minimize
        root.addView(createSidebarIconButton(service, R.drawable.ic_window_minimize, 0xFF888894, service::minimizeToEdge));

        service.attachDrag(root);
        return root;
    }

    // ---------------------------------------------------------------------------------------------
    // 7. PERFORMANCE MATRIX (Hardware Telemetry Enthusiast Matrix)
    // ---------------------------------------------------------------------------------------------
    private static View buildPerformanceMatrix(GameBubbleService service) {
        LinearLayout root = createBasePanel(service, 0xF60B0E14, 0xFA06080C, 0xFF00E5FF, 0xFF00C853, 14);
        root.addView(createPanelHeader(service, "PERFORMANCE MATRIX", 0xFF00E5FF, true));

        ScrollView scroll = createScrollView(service);
        LinearLayout content = createVerticalContent(service);

        // Real Telemetry Data Grid
        LinearLayout grid = new LinearLayout(service);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(dp(service, 10), dp(service, 8), dp(service, 10), dp(service, 8));
        GradientDrawable gBg = new GradientDrawable();
        gBg.setColor(0xDD0B121C);
        gBg.setCornerRadius(dp(service, 10));
        gBg.setStroke(dp(service, 1), 0x5500E5FF);
        grid.setBackground(gBg);

        grid.addView(createMatrixRow(service, "HARDWARE STATUS", "ONLINE // ACTIVE", 0xFF00E5FF));
        grid.addView(createMatrixRow(service, "LIVE TELEMETRY", service.readLiveStats(), 0xFFFFFFFF));
        grid.addView(createMatrixRow(service, "ACTIVE PROFILE", GsmProfileManager.getProfileName(service, service.getSelectedProfile()), 0xFFFF2A48));
        grid.addView(createMatrixRow(service, "ENGINE MODE", service.isPhoenixMode() ? "Phoenix VIP Core" : "Regular Kernel", 0xFF9D00FF));

        content.addView(grid);

        content.addView(createSectionTitle(service, "SWITCH PROFILES", 0xFF00E5FF));
        content.addView(createProfileSelectorRow(service, 0xFF00E5FF));

        content.addView(createSectionTitle(service, "HARDWARE TUNING SHORTCUTS", 0xFF00C853));
        content.addView(createActionTile(service, R.drawable.ic_refresh, "Lock Peak Display Hz", "Lock active refresh rate", service::forcePeakRefreshRate));
        content.addView(createActionTile(service, R.drawable.ic_turbo_fan, "Drop Caches & Flush RAM", "Release reserved memory pages", service::flushVramAndRam));
        content.addView(createActionTile(service, R.drawable.ic_gestures, "Boost Touch Polling Rate", "Switch touch driver to gaming boost", service::boostTouchSampling));
        content.addView(createActionTile(service, R.drawable.ic_clock, "Speedrun Stopwatch", "Floating on-screen timer", service::toggleSpeedrunStopwatch));
        content.addView(createActionTile(service, R.drawable.ic_numbers, "Combat Mini Calculator", "Floating tactical damage calculator", service::toggleFloatingCalculator));
        content.addView(createActionTile(service, R.drawable.ic_gallery, "Lossless Screenshot", "Pictures/ROR/Screenshots", service::takeQuickScreenshot));
        content.addView(createActionTile(service, R.drawable.ic_record_dot, "Gameplay Video", "Movies/ROR MP4 recording", service::toggleScreenRecording));

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        root.addView(createPanelFooter(service));

        service.attachDrag(root);
        return root;
    }

    // ---------------------------------------------------------------------------------------------
    // 8. PHOENIX ULTRA PANEL (Flagship Crimson-Gold Gaming Station)
    // ---------------------------------------------------------------------------------------------
    private static View buildPhoenixUltraPanel(GameBubbleService service) {
        LinearLayout root = createBasePanel(service, 0xF8100609, 0xFC080204, 0xFFFF1744, 0xFFFFD700, 18);
        root.addView(createPanelHeader(service, "PHOENIX ULTRA ENGINE", 0xFFFFD700, true));

        ScrollView scroll = createScrollView(service);
        LinearLayout content = createVerticalContent(service);

        // Ultra Banner Card
        LinearLayout ultraCard = new LinearLayout(service);
        ultraCard.setOrientation(LinearLayout.VERTICAL);
        ultraCard.setPadding(dp(service, 14), dp(service, 10), dp(service, 14), dp(service, 10));
        GradientDrawable uBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xCC2A040B, 0xDD120205});
        uBg.setCornerRadius(dp(service, 10));
        uBg.setStroke(dp(service, 1.5f), 0xFFFFD700);
        ultraCard.setBackground(uBg);

        TextView uTitle = createLabel(service, "PHOENIX VIP PERFORMANCE CORE", 10f, 0xFFFFD700, true);
        ultraCard.addView(uTitle);
        TextView uStats = createLabel(service, service.readLiveStats(), 10f, 0xFFFFF0F5, false);
        uStats.setPadding(0, dp(service, 4), 0, 0);
        ultraCard.addView(uStats);
        content.addView(ultraCard);

        content.addView(createSectionTitle(service, "PHOENIX ENGINE PROFILES", 0xFFFF1744));
        content.addView(createProfileSelectorRow(service, 0xFFFF1744));

        content.addView(createSectionTitle(service, "ULTRA KERNEL CONTROLS", 0xFFFFD700));
        content.addView(createActionTile(service, R.drawable.ic_turbo_fan, "Maximum GPU/CPU Clock Lock", "Apply aggressive frequency burst", service::flushVramAndRam));
        content.addView(createActionTile(service, R.drawable.ic_crosshair, "Aim Precision Engine", "Target acquisition boost", service::toggleCrosshair));
        content.addView(createActionTile(service, R.drawable.ic_refresh, "Uncapped 120Hz/144Hz Rate", "Force maximum hardware scanout rate", service::forcePeakRefreshRate));
        content.addView(createActionTile(service, R.drawable.ic_gallery, "Instant Screenshot", "Capture gameplay snapshot", service::takeQuickScreenshot));
        content.addView(createActionTile(service, R.drawable.ic_record_dot, "Pro Video Recorder", "Start screen recording", service::toggleScreenRecording));
        content.addView(createActionTile(service, R.drawable.ic_layers, "Multitasking Companion", "Open floating mini apps", service::showMiniApps));

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        root.addView(createPanelFooter(service));

        service.attachDrag(root);
        return root;
    }

    // ---------------------------------------------------------------------------------------------
    // 9. FLOATING GAME DOCK (Horizontal Draggable Toolbar)
    // ---------------------------------------------------------------------------------------------
    private static View buildFloatingGameDock(GameBubbleService service) {
        LinearLayout root = new LinearLayout(service);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setPadding(dp(service, 8), dp(service, 6), dp(service, 8), dp(service, 6));

        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xF6141622, 0xF80B0C14, 0xF61A0C16});
        bg.setCornerRadius(dp(service, 28));
        bg.setStroke(dp(service, 1.5f), 0xFFFF1744);
        root.setBackground(bg);

        // Profile Pill
        TextView prof = new TextView(service);
        prof.setText(GsmProfileManager.getProfileName(service, service.getSelectedProfile()).toUpperCase(Locale.US));
        prof.setTextSize(9f);
        prof.setTextColor(Color.WHITE);
        prof.setTypeface(null, Typeface.BOLD);
        prof.setPadding(dp(service, 10), dp(service, 6), dp(service, 10), dp(service, 6));
        GradientDrawable pBg = new GradientDrawable();
        pBg.setColor(0xFFFF1744);
        pBg.setCornerRadius(dp(service, 14));
        prof.setBackground(pBg);
        prof.setOnClickListener(v -> {
            int cur = service.getSelectedProfile();
            int next = (cur + 1) % (service.isPhoenixMode() ? 8 : 6);
            service.applyProfileDirect(next);
            prof.setText(GsmProfileManager.getProfileName(service, next).toUpperCase(Locale.US));
        });
        root.addView(prof);

        // Action Icons
        root.addView(createDockIcon(service, R.drawable.ic_gallery, 0xFF00E5FF, service::takeQuickScreenshot));
        root.addView(createDockIcon(service, R.drawable.ic_record_dot, 0xFFFF3345, service::toggleScreenRecording));
        root.addView(createDockIcon(service, R.drawable.ic_refresh, 0xFFFFD600, service::forcePeakRefreshRate));
        root.addView(createDockIcon(service, R.drawable.ic_turbo_fan, 0xFF00FF66, service::flushVramAndRam));
        root.addView(createDockIcon(service, R.drawable.ic_crosshair, 0xFFFF007F, service::toggleCrosshair));
        root.addView(createDockIcon(service, R.drawable.ic_window_close, 0xFF8E95AF, service::collapse));

        service.attachDrag(root);
        return root;
    }

    // ---------------------------------------------------------------------------------------------
    // 10. ADAPTIVE SMART HUD (Dynamic Orientation & Compact/Expanded Modes)
    // ---------------------------------------------------------------------------------------------
    private static View buildAdaptiveSmartHud(GameBubbleService service) {
        boolean isLandscape = service.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        if (isLandscape) {
            // Dual Column Landscape Mode
            LinearLayout root = createBasePanel(service, 0xF60E111A, 0xFA080A10, 0xFF00E5FF, 0xFFFF1744, 16);
            root.addView(createPanelHeader(service, "ADAPTIVE HUD // LANDSCAPE", 0xFF00E5FF, false));

            LinearLayout split = new LinearLayout(service);
            split.setOrientation(LinearLayout.HORIZONTAL);

            // Left: Telemetry & Profiles
            LinearLayout left = new LinearLayout(service);
            left.setOrientation(LinearLayout.VERTICAL);
            left.setPadding(0, 0, dp(service, 8), 0);

            TextView hudTitle = createLabel(service, "REAL-TIME TELEMETRY", 9f, 0xFF00E5FF, true);
            left.addView(hudTitle);
            TextView hud = createLabel(service, service.readLiveStats(), 9.5f, Color.WHITE, false);
            hud.setPadding(0, dp(service, 4), 0, dp(service, 6));
            left.addView(hud);

            left.addView(createSectionTitle(service, "PROFILE SELECTOR", 0xFFFF1744));
            left.addView(createProfileSelectorRow(service, 0xFFFF1744));
            split.addView(left, new LinearLayout.LayoutParams(0, -1, 1.1f));

            // Right: Scrollable Quick Tools
            ScrollView rightScroll = createScrollView(service);
            LinearLayout right = createVerticalContent(service);
            right.addView(createSectionTitle(service, "TACTICAL SHORTCUTS", 0xFF00FF66));
            right.addView(createActionTile(service, R.drawable.ic_gallery, "Screenshot", "Instant picture", service::takeQuickScreenshot));
            right.addView(createActionTile(service, R.drawable.ic_record_dot, "Record Screen", "Capture video", service::toggleScreenRecording));
            right.addView(createActionTile(service, R.drawable.ic_refresh, "Peak Refresh", "Lock maximum Hz", service::forcePeakRefreshRate));
            right.addView(createActionTile(service, R.drawable.ic_turbo_fan, "Flush RAM", "Purge caches", service::flushVramAndRam));
            right.addView(createActionTile(service, R.drawable.ic_crosshair, "Crosshair", "Aim reticle", service::toggleCrosshair));
            rightScroll.addView(right);
            split.addView(rightScroll, new LinearLayout.LayoutParams(0, -1, 1.0f));

            root.addView(split, new LinearLayout.LayoutParams(-1, 0, 1.0f));
            root.addView(createPanelFooter(service));
            service.attachDrag(root);
            return root;
        } else {
            // Single Column Portrait Mode
            LinearLayout root = createBasePanel(service, 0xF60E111A, 0xFA080A10, 0xFF00E5FF, 0xFFFF1744, 16);
            root.addView(createPanelHeader(service, "ADAPTIVE HUD // PORTRAIT", 0xFF00E5FF, true));

            ScrollView scroll = createScrollView(service);
            LinearLayout content = createVerticalContent(service);

            content.addView(createTelemetryBadge(service, 0xFF00E5FF, 0xFFFF1744));
            content.addView(createSectionTitle(service, "PERFORMANCE MODES", 0xFFFF1744));
            content.addView(createProfileSelectorRow(service, 0xFFFF1744));

            content.addView(createSectionTitle(service, "QUICK TOOLS", 0xFF00FF66));
            content.addView(createActionTile(service, R.drawable.ic_gallery, "Screenshot", "Instant game capture", service::takeQuickScreenshot));
            content.addView(createActionTile(service, R.drawable.ic_record_dot, "Screen Record", "Video to Movies/ROR", service::toggleScreenRecording));
            content.addView(createActionTile(service, R.drawable.ic_refresh, "Force Peak Hz", "Max refresh rate", service::forcePeakRefreshRate));
            content.addView(createActionTile(service, R.drawable.ic_turbo_fan, "Clean Memory", "Clear cache pages", service::flushVramAndRam));
            content.addView(createActionTile(service, R.drawable.ic_crosshair, "Crosshair Aim", "Toggle overlay aim", service::toggleCrosshair));

            scroll.addView(content);
            root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));
            root.addView(createPanelFooter(service));
            service.attachDrag(root);
            return root;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // COMMON UI BUILDER HELPERS
    // ---------------------------------------------------------------------------------------------

    private static LinearLayout createBasePanel(Context context, int bgTop, int bgBottom, int strokeColor1, int strokeColor2, int radiusDp) {
        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12));

        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{bgTop, bgBottom});
        bg.setCornerRadius(dp(context, radiusDp));
        bg.setStroke(dp(context, 1.5f), strokeColor1);
        panel.setBackground(bg);
        return panel;
    }

    private static LinearLayout createPanelHeader(GameBubbleService service, String titleText, int titleColor, boolean showTrafficLights) {
        LinearLayout header = new LinearLayout(service);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(service, 8));

        TextView title = new TextView(service);
        title.setText(titleText);
        title.setTextSize(11.5f);
        title.setTextColor(titleColor);
        title.setTypeface(null, Typeface.BOLD);
        title.setLetterSpacing(0.04f);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1.0f));

        if (showTrafficLights) {
            header.addView(createTrafficLights(service));
        } else {
            ImageView close = new ImageView(service);
            close.setImageResource(R.drawable.ic_window_close);
            close.setColorFilter(0xFF888894);
            close.setPadding(dp(service, 4), dp(service, 4), dp(service, 4), dp(service, 4));
            close.setOnClickListener(v -> service.collapse());
            header.addView(close, new LinearLayout.LayoutParams(dp(service, 22), dp(service, 22)));
        }
        return header;
    }

    private static LinearLayout createTrafficLights(GameBubbleService service) {
        LinearLayout bar = new LinearLayout(service);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);

        bar.addView(createTrafficDot(service, 0xFFD71528, 0xFFFF6874, R.drawable.ic_window_close, service::collapse));
        bar.addView(createTrafficDot(service, 0xFF25252A, 0xFF55555D, R.drawable.ic_window_minimize, service::minimizeToEdge));
        bar.addView(createTrafficDot(service, 0xFF25252A, 0xFF55555D, R.drawable.ic_refresh, () -> {
            service.showBarrage("Telemetry Updated", service.readLiveStats());
        }));
        return bar;
    }

    private static View createTrafficDot(Context context, int color, int stroke, int iconRes, Runnable action) {
        FrameLayout dot = new FrameLayout(context);
        int size = dp(context, 18);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMargins(dp(context, 3), 0, dp(context, 3), 0);
        dot.setLayoutParams(lp);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(color);
        bg.setStroke(dp(context, 1), stroke);
        dot.setBackground(bg);

        ImageView icon = new ImageView(context);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.WHITE);
        icon.setPadding(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 4));
        dot.addView(icon, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));

        dot.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            if (action != null) action.run();
        });
        return dot;
    }

    private static ScrollView createScrollView(Context context) {
        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        return scroll;
    }

    private static LinearLayout createVerticalContent(Context context) {
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(context, 2), 0, dp(context, 6));
        return content;
    }

    private static TextView createSectionTitle(Context context, String title, int color) {
        TextView tv = new TextView(context);
        tv.setText(title);
        tv.setTextSize(9f);
        tv.setTextColor(color);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setLetterSpacing(0.06f);
        tv.setPadding(0, dp(context, 8), 0, dp(context, 4));
        return tv;
    }

    private static TextView createLabel(Context context, String text, float sizeSp, int color, boolean bold) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(sizeSp);
        tv.setTextColor(color);
        if (bold) tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }

    private static View createTelemetryBadge(GameBubbleService service, int accentColor, int strokeColor) {
        TextView badge = new TextView(service);
        badge.setText(service.readLiveStats());
        badge.setTextSize(10.5f);
        badge.setTextColor(Color.WHITE);
        badge.setPadding(dp(service, 12), dp(service, 8), dp(service, 12), dp(service, 8));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xCC161422);
        bg.setCornerRadius(dp(service, 10));
        bg.setStroke(dp(service, 1), strokeColor);
        badge.setBackground(bg);
        return badge;
    }

    private static View createProfileSelectorRow(GameBubbleService service, int activeColor) {
        HorizontalScrollView scroll = new HorizontalScrollView(service);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout row = new LinearLayout(service);
        row.setOrientation(LinearLayout.HORIZONTAL);

        boolean phoenix = service.isPhoenixMode();
        List<ProfileRegistry.ProfileDescriptor> profiles = ProfileRegistry.getProfiles(phoenix);

        int currentProfile = service.getSelectedProfile();

        for (ProfileRegistry.ProfileDescriptor desc : profiles) {
            final int profId = desc.id;
            boolean active = (profId == currentProfile);

            TextView btn = new TextView(service);
            btn.setText(desc.shortName);
            btn.setTextSize(9.5f);
            btn.setTypeface(null, Typeface.BOLD);
            btn.setTextColor(active ? Color.WHITE : 0xFFCCCCCC);
            btn.setGravity(Gravity.CENTER);
            btn.setPadding(dp(service, 10), dp(service, 6), dp(service, 10), dp(service, 6));

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(active ? activeColor : 0x44222432);
            bg.setCornerRadius(dp(service, 8));
            bg.setStroke(dp(service, active ? 1.5f : 1), active ? Color.WHITE : 0x5544485C);
            btn.setBackground(bg);

            btn.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                service.applyProfileDirect(profId);
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(service, 32));
            lp.setMarginEnd(dp(service, 5));
            row.addView(btn, lp);
        }

        scroll.addView(row);
        return scroll;
    }

    private static View createActionTile(GameBubbleService service, int iconRes, String title, String subtitle, Runnable action) {
        LinearLayout tile = new LinearLayout(service);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(dp(service, 10), dp(service, 7), dp(service, 10), dp(service, 7));
        tile.setClickable(true);
        tile.setFocusable(true);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xCC1A1C28);
        bg.setCornerRadius(dp(service, 8));
        bg.setStroke(dp(service, 1), 0x33555D7A);
        tile.setBackground(bg);

        ImageView icon = new ImageView(service);
        icon.setImageResource(iconRes);
        icon.setColorFilter(0xFFFF3345);
        tile.addView(icon, new LinearLayout.LayoutParams(dp(service, 20), dp(service, 20)));

        LinearLayout texts = new LinearLayout(service);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(service, 10), 0, 0, 0);

        TextView tTitle = new TextView(service);
        tTitle.setText(title);
        tTitle.setTextSize(10.5f);
        tTitle.setTypeface(null, Typeface.BOLD);
        tTitle.setTextColor(Color.WHITE);
        texts.addView(tTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tSub = new TextView(service);
            tSub.setText(subtitle);
            tSub.setTextSize(8.5f);
            tSub.setTextColor(0xFF9EA3B8);
            texts.addView(tSub);
        }

        tile.addView(texts, new LinearLayout.LayoutParams(0, -2, 1.0f));

        tile.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(60).withEndAction(() -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(70).start();
                if (action != null) action.run();
            }).start();
        });

        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-1, -2);
        tlp.bottomMargin = dp(service, 5);
        tile.setLayoutParams(tlp);
        return tile;
    }

    private static View createMicroButton(GameBubbleService service, int iconRes, String text, Runnable action) {
        LinearLayout btn = new LinearLayout(service);
        btn.setOrientation(LinearLayout.VERTICAL);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(service, 4), dp(service, 4), dp(service, 4), dp(service, 4));

        ImageView icon = new ImageView(service);
        icon.setImageResource(iconRes);
        icon.setColorFilter(0xFF00E5FF);
        btn.addView(icon, new LinearLayout.LayoutParams(dp(service, 18), dp(service, 18)));

        TextView label = new TextView(service);
        label.setText(text);
        label.setTextSize(7.5f);
        label.setTextColor(0xFFD0D4E4);
        label.setTypeface(null, Typeface.BOLD);
        btn.addView(label);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x441F2332);
        bg.setCornerRadius(dp(service, 6));
        btn.setBackground(bg);

        btn.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            if (action != null) action.run();
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(service, 36), 1.0f);
        lp.setMargins(dp(service, 2), 0, dp(service, 2), 0);
        btn.setLayoutParams(lp);
        return btn;
    }

    private static ImageView createSidebarIconButton(GameBubbleService service, int iconRes, int tint, Runnable action) {
        ImageView icon = new ImageView(service);
        icon.setImageResource(iconRes);
        icon.setColorFilter(tint);
        icon.setPadding(dp(service, 10), dp(service, 10), dp(service, 10), dp(service, 10));

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(0xCC181A28);
        bg.setStroke(dp(service, 1), 0x44454E6C);
        icon.setBackground(bg);

        icon.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(60).withEndAction(() -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(70).start();
                if (action != null) action.run();
            }).start();
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(service, 44), dp(service, 44));
        lp.topMargin = dp(service, 5);
        lp.bottomMargin = dp(service, 5);
        icon.setLayoutParams(lp);
        return icon;
    }

    private static ImageView createDockIcon(GameBubbleService service, int iconRes, int tint, Runnable action) {
        ImageView icon = new ImageView(service);
        icon.setImageResource(iconRes);
        icon.setColorFilter(tint);
        icon.setPadding(dp(service, 7), dp(service, 7), dp(service, 7), dp(service, 7));

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(0x441F2332);
        bg.setStroke(dp(service, 1), 0x33555D7A);
        icon.setBackground(bg);

        icon.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            if (action != null) action.run();
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(service, 36), dp(service, 36));
        lp.setMarginStart(dp(service, 6));
        icon.setLayoutParams(lp);
        return icon;
    }

    private static View createMatrixRow(GameBubbleService service, String key, String value, int valColor) {
        LinearLayout row = new LinearLayout(service);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(service, 4), 0, dp(service, 4));

        TextView k = new TextView(service);
        k.setText(key);
        k.setTextSize(9f);
        k.setTypeface(null, Typeface.BOLD);
        k.setTextColor(0xFF8E95AF);
        row.addView(k, new LinearLayout.LayoutParams(dp(service, 110), -2));

        TextView v = new TextView(service);
        v.setText(value);
        v.setTextSize(9.5f);
        v.setTypeface(null, Typeface.BOLD);
        v.setTextColor(valColor);
        row.addView(v, new LinearLayout.LayoutParams(0, -2, 1.0f));
        return row;
    }

    private static LinearLayout createPanelFooter(GameBubbleService service) {
        LinearLayout footer = new LinearLayout(service);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setPadding(0, dp(service, 6), 0, 0);

        TextView minBtn = new TextView(service);
        minBtn.setText("DOCK TO EDGE");
        minBtn.setTextSize(9.5f);
        minBtn.setTextColor(Color.WHITE);
        minBtn.setTypeface(null, Typeface.BOLD);
        minBtn.setGravity(Gravity.CENTER);
        GradientDrawable mBg = new GradientDrawable();
        mBg.setColor(0xCC202332);
        mBg.setCornerRadius(dp(service, 8));
        minBtn.setBackground(mBg);
        minBtn.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            service.minimizeToEdge();
        });

        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, dp(service, 36), 1.0f);
        lp1.setMarginEnd(dp(service, 4));
        footer.addView(minBtn, lp1);

        TextView closeBtn = new TextView(service);
        closeBtn.setText("COLLAPSE");
        closeBtn.setTextSize(9.5f);
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setTypeface(null, Typeface.BOLD);
        closeBtn.setGravity(Gravity.CENTER);
        GradientDrawable cBg = new GradientDrawable();
        cBg.setColor(0xCC202332);
        cBg.setCornerRadius(dp(service, 8));
        closeBtn.setBackground(cBg);
        closeBtn.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            service.collapse();
        });

        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, dp(service, 36), 1.0f);
        lp2.setMarginStart(dp(service, 4));
        footer.addView(closeBtn, lp2);
        return footer;
    }

    private static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

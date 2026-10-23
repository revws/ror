/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.revror.kernelmanager.R;
import com.revror.kernelmanager.utils.GsmProfileManager;
import com.revror.kernelmanager.utils.RorLicenseManager;
import com.revror.kernelmanager.utils.kernel.compat.ProfileRegistry;

import java.util.List;
import java.util.Locale;

/**
 * Manager and factory for the 8 distinct, professional, responsive
 * in-game floating window layouts in REVROR Game Space.
 *
 * All layouts are fully wired to the central FloatingFeatureRegistry,
 * ensuring complete access to all 32+ gaming, performance, capture,
 * and system engines.
 */
public final class FloatingWindowLayoutManager {

    public static final int LAYOUT_PHOENIX_COMMAND = 0;
    public static final int LAYOUT_MINIMAL_GLASS = 1;
    public static final int LAYOUT_ESPORTS_DASH = 2;
    public static final int LAYOUT_GAMING_SIDEBAR = 3;
    public static final int LAYOUT_PERF_MATRIX = 4;
    public static final int LAYOUT_PHOENIX_ULTRA = 5;
    public static final int LAYOUT_GAME_DOCK = 6;
    public static final int LAYOUT_ADAPTIVE_HUD = 7;

    public static final String[] LAYOUT_NAMES = {
            "Phoenix Command Center",
            "Minimal Glass Overlay",
            "Esports Dashboard",
            "Gaming Sidebar",
            "Performance Matrix",
            "Phoenix Ultra Panel",
            "Floating Game Dock",
            "Adaptive Smart HUD"
    };

    public static final String[] LAYOUT_DESCRIPTIONS = {
            "Flagship translucent dark panel with crimson & violet accents, full telemetry & all quick tools",
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
     * Sanitizes and migrates user layout preference.
     * Deprecated/removed layouts (Compact HUD & Cyber Panel) are migrated cleanly
     * to Phoenix Command Center without losing other preferences.
     */
    public static int sanitizeAndMigrateLayoutStyle(Context context) {
        if (context == null) return LAYOUT_PHOENIX_COMMAND;
        SharedPreferences sp = context.getSharedPreferences("ror_game_space_center", Context.MODE_PRIVATE);
        boolean migrated = sp.getBoolean("ror_floating_layout_migrated_v2", false);
        int current = sp.getInt("ror_floating_layout_style", 0);

        if (!migrated) {
            int sanitized;
            // Legacy 1 (Compact HUD) and 2 (Cyber Panel) removed -> map to 0 (Phoenix Command)
            if (current == 1 || current == 2) {
                sanitized = LAYOUT_PHOENIX_COMMAND;
            } else if (current >= 3 && current <= 9) {
                // Map legacy 3..9 to 1..7
                sanitized = current - 2;
            } else {
                sanitized = LAYOUT_PHOENIX_COMMAND;
            }
            sp.edit()
                    .putInt("ror_floating_layout_style", sanitized)
                    .putBoolean("ror_floating_layout_migrated_v2", true)
                    .apply();
            return sanitized;
        }

        if (current < 0 || current >= LAYOUT_NAMES.length) {
            current = LAYOUT_PHOENIX_COMMAND;
            sp.edit().putInt("ror_floating_layout_style", current).apply();
        }
        return current;
    }

    /**
     * Configures initial WindowManager parameters for the chosen layout.
     */
    public static void configureLayoutParams(GameBubbleService service, WindowManager.LayoutParams params, int layoutStyle) {
        if (service == null || params == null) return;
        DisplayMetrics dm = service.getResources().getDisplayMetrics();
        SharedPreferences sp = service.getSharedPreferences("ror_game_space_center", Context.MODE_PRIVATE);

        params.alpha = 1.0f; // Window buffer MUST remain 1.0f for crisp font antialiasing

        int scalePct = sp.getInt("ror_floating_scale", 100);
        float scale = Math.max(0.80f, Math.min(1.25f, scalePct / 100f));
        boolean dockRight = sp.getBoolean("ror_floating_dock_right", true);

        switch (layoutStyle) {
            case LAYOUT_GAMING_SIDEBAR:
                params.width = Math.round(dp(service, 76) * scale);
                params.height = Math.min(Math.round(dp(service, 380) * scale), dm.heightPixels - dp(service, 32));
                params.gravity = (dockRight ? Gravity.END : Gravity.START) | Gravity.CENTER_VERTICAL;
                params.x = 0;
                params.y = 0;
                break;

            case LAYOUT_GAME_DOCK:
                params.width = Math.min(Math.round(dp(service, 340) * scale), dm.widthPixels - dp(service, 20));
                params.height = Math.round(dp(service, 58) * scale);
                params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                params.x = 0;
                params.y = dp(service, 20);
                break;

            case LAYOUT_ADAPTIVE_HUD:
                boolean isLandscape = service.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
                if (isLandscape) {
                    params.width = Math.min(Math.round(dp(service, 500) * scale), dm.widthPixels - dp(service, 24));
                    params.height = Math.min(Math.round(dp(service, 320) * scale), dm.heightPixels - dp(service, 20));
                } else {
                    params.width = Math.min(Math.round(dp(service, 340) * scale), dm.widthPixels - dp(service, 20));
                    params.height = Math.min(Math.round(dp(service, 540) * scale), dm.heightPixels - dp(service, 24));
                }
                params.gravity = Gravity.CENTER;
                break;

            case LAYOUT_MINIMAL_GLASS:
                params.width = Math.min(Math.round(dp(service, 340) * scale), dm.widthPixels - dp(service, 20));
                params.height = Math.min(Math.round(dp(service, 520) * scale), dm.heightPixels - dp(service, 24));
                params.gravity = Gravity.CENTER_VERTICAL | (dockRight ? Gravity.END : Gravity.START);
                params.x = dp(service, 10);
                break;

            case LAYOUT_PERF_MATRIX:
                params.width = Math.min(Math.round(dp(service, 350) * scale), dm.widthPixels - dp(service, 16));
                params.height = Math.min(Math.round(dp(service, 540) * scale), dm.heightPixels - dp(service, 24));
                params.gravity = Gravity.CENTER;
                break;

            case LAYOUT_PHOENIX_COMMAND:
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
    // 0. PHOENIX COMMAND CENTER (Flagship Panel)
    // ---------------------------------------------------------------------------------------------
    private static View buildPhoenixCommandCenter(GameBubbleService service) {
        return service.buildClassicPanel();
    }

    // ---------------------------------------------------------------------------------------------
    // 1. MINIMAL GLASS OVERLAY (Frosted Glass Aesthetics)
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

        content.addView(createSectionTitle(service, "PERFORMANCE MODES", 0xFFC0C4D8));
        content.addView(createProfileSelectorRow(service, 0xFFFA476B));

        addAllCategorizedFeatures(service, content, 0xFFFA476B);

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        root.addView(createPanelFooter(service));

        service.attachDrag(root);
        return root;
    }

    // ---------------------------------------------------------------------------------------------
    // 2. ESPORTS DASHBOARD (Tournament Focus)
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

        addAllCategorizedFeatures(service, content, 0xFFFF1744);

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        root.addView(createPanelFooter(service));

        service.attachDrag(root);
        return root;
    }

    // ---------------------------------------------------------------------------------------------
    // 3. GAMING SIDEBAR (Edge-Mounted Vertical Rail)
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

        // Quick Screenshot
        root.addView(createSidebarIconButton(service, R.drawable.ic_gallery, 0xFF00E5FF, service::takeQuickScreenshot));

        // Screen Record
        root.addView(createSidebarIconButton(service, R.drawable.ic_record_dot, 0xFFFF3345, service::toggleScreenRecording));

        // Refresh Rate
        root.addView(createSidebarIconButton(service, R.drawable.ic_refresh, 0xFFFFD600, service::forcePeakRefreshRate));

        // Touch Boost
        root.addView(createSidebarIconButton(service, R.drawable.ic_gestures, 0xFF00FF66, service::toggleTouchBoost));

        // RAM Flush
        root.addView(createSidebarIconButton(service, R.drawable.ic_turbo_fan, 0xFF00E5FF, service::flushVramAndRam));

        // Crosshair
        root.addView(createSidebarIconButton(service, R.drawable.ic_crosshair, 0xFFFF007F, service::toggleCrosshair));

        // Expand ALL Categorized Tools (Opens full control hub so no feature is omitted)
        root.addView(createSidebarIconButton(service, R.drawable.ic_layers, 0xFFFFD700, () -> service.replaceOverlay(service.buildClassicPanel())));

        // Minimize to Edge
        root.addView(createSidebarIconButton(service, R.drawable.ic_window_minimize, 0xFF888894, service::minimizeToEdge));

        service.attachDrag(root);
        return root;
    }

    // ---------------------------------------------------------------------------------------------
    // 4. PERFORMANCE MATRIX (Hardware Telemetry Enthusiast Matrix)
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

        addAllCategorizedFeatures(service, content, 0xFF00C853);

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        root.addView(createPanelFooter(service));

        service.attachDrag(root);
        return root;
    }

    // ---------------------------------------------------------------------------------------------
    // 5. PHOENIX ULTRA PANEL (Flagship Crimson-Gold Gaming Station)
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

        addAllCategorizedFeatures(service, content, 0xFFFFD700);

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        root.addView(createPanelFooter(service));

        service.attachDrag(root);
        return root;
    }

    // ---------------------------------------------------------------------------------------------
    // 6. FLOATING GAME DOCK (Horizontal Draggable Toolbar)
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
        root.addView(createDockIcon(service, R.drawable.ic_gestures, 0xFF00FF66, service::toggleTouchBoost));
        root.addView(createDockIcon(service, R.drawable.ic_turbo_fan, 0xFF00E5FF, service::flushVramAndRam));
        root.addView(createDockIcon(service, R.drawable.ic_crosshair, 0xFFFF007F, service::toggleCrosshair));
        root.addView(createDockIcon(service, R.drawable.ic_layers, 0xFFFFD700, () -> service.replaceOverlay(service.buildClassicPanel())));
        root.addView(createDockIcon(service, R.drawable.ic_window_close, 0xFF8E95AF, service::collapse));

        service.attachDrag(root);
        return root;
    }

    // ---------------------------------------------------------------------------------------------
    // 7. ADAPTIVE SMART HUD (Dynamic Orientation)
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
            split.addView(left, new LinearLayout.LayoutParams(0, -1, 1.0f));

            // Right: Scrollable Quick Tools
            ScrollView rightScroll = createScrollView(service);
            LinearLayout right = createVerticalContent(service);
            addAllCategorizedFeatures(service, right, 0xFF00FF66);
            rightScroll.addView(right);
            split.addView(rightScroll, new LinearLayout.LayoutParams(0, -1, 1.2f));

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

            addAllCategorizedFeatures(service, content, 0xFF00FF66);

            scroll.addView(content);
            root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));
            root.addView(createPanelFooter(service));
            service.attachDrag(root);
            return root;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // COMMON HELPERS & UNIFIED REGISTRY RENDERER
    // ---------------------------------------------------------------------------------------------

    public static void addAllCategorizedFeatures(GameBubbleService service, LinearLayout content, int accentColor) {
        if (service == null || content == null) return;
        SharedPreferences sp = service.getSharedPreferences("ror_game_space_center", Context.MODE_PRIVATE);
        if (sp.getBoolean("ror_tool_vis_brightness_control", true)) {
            try {
                content.addView(service.buildBrightnessControlRow());
            } catch (Throwable ignored) {}
        }

        for (FloatingFeatureRegistry.Category cat : FloatingFeatureRegistry.Category.values()) {
            List<FloatingFeatureRegistry.FloatingFeature> features = FloatingFeatureRegistry.getFeaturesByCategory(cat);
            if (features.isEmpty()) continue;

            content.addView(createSectionTitle(service, cat.title.toUpperCase(Locale.US), accentColor));

            for (FloatingFeatureRegistry.FloatingFeature feat : features) {
                content.addView(createRegistryTile(service, feat, accentColor));
            }
        }
    }

    public static View createRegistryTile(GameBubbleService service, FloatingFeatureRegistry.FloatingFeature feature, int accentColor) {
        LinearLayout tile = new LinearLayout(service);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(dp(service, 10), dp(service, 8), dp(service, 10), dp(service, 8));
        tile.setClickable(true);
        tile.setFocusable(true);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xCC1A1C28);
        bg.setCornerRadius(dp(service, 9));
        bg.setStroke(dp(service, 1), 0x33555D7A);
        tile.setBackground(bg);

        ImageView icon = new ImageView(service);
        icon.setImageResource(feature.iconRes);
        boolean isVipLocked = feature.requiresPhoenixVip && !RorLicenseManager.isPremiumActive(service);
        boolean active = feature.isActive(service);
        int iconTint = isVipLocked ? 0xFFFFD700 : (active ? 0xFF00E676 : accentColor);
        icon.setColorFilter(iconTint);
        tile.addView(icon, new LinearLayout.LayoutParams(dp(service, 20), dp(service, 20)));

        LinearLayout texts = new LinearLayout(service);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(service, 10), 0, dp(service, 6), 0);

        TextView tTitle = new TextView(service);
        tTitle.setText(feature.name + (isVipLocked ? " \uD83D\uDD12" : ""));
        tTitle.setTextSize(10.5f);
        tTitle.setTypeface(null, Typeface.BOLD);
        tTitle.setTextColor(Color.WHITE);
        texts.addView(tTitle);

        TextView tSub = new TextView(service);
        tSub.setText(feature.subtitle);
        tSub.setTextSize(8.5f);
        tSub.setTextColor(0xFF9EA3B8);
        texts.addView(tSub);

        tile.addView(texts, new LinearLayout.LayoutParams(0, -2, 1.0f));

        TextView badge = new TextView(service);
        badge.setText(feature.getStatusText(service));
        badge.setTextSize(8.5f);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setPadding(dp(service, 7), dp(service, 3), dp(service, 7), dp(service, 3));
        GradientDrawable bBg = new GradientDrawable();
        bBg.setCornerRadius(dp(service, 6));
        if (isVipLocked) {
            badge.setTextColor(0xFFFFD700);
            bBg.setColor(0x33FFD700);
            bBg.setStroke(dp(service, 1), 0x88FFD700);
        } else if (active) {
            badge.setTextColor(0xFF00E676);
            bBg.setColor(0x3300E676);
            bBg.setStroke(dp(service, 1), 0x8800E676);
        } else {
            badge.setTextColor(0xFFB0B4C8);
            bBg.setColor(0x22FFFFFF);
            bBg.setStroke(dp(service, 1), 0x33FFFFFF);
        }
        badge.setBackground(bBg);
        tile.addView(badge);

        tile.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(60).withEndAction(() -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(70).start();
                FloatingFeatureRegistry.executeFeature(service, feature.id, () -> {
                    boolean newActive = feature.isActive(service);
                    boolean newLocked = feature.requiresPhoenixVip && !RorLicenseManager.isPremiumActive(service);
                    badge.setText(feature.getStatusText(service));
                    if (newLocked) {
                        badge.setTextColor(0xFFFFD700);
                        bBg.setColor(0x33FFD700);
                        bBg.setStroke(dp(service, 1), 0x88FFD700);
                        icon.setColorFilter(0xFFFFD700);
                    } else if (newActive) {
                        badge.setTextColor(0xFF00E676);
                        bBg.setColor(0x3300E676);
                        bBg.setStroke(dp(service, 1), 0x8800E676);
                        icon.setColorFilter(0xFF00E676);
                    } else {
                        badge.setTextColor(0xFFB0B4C8);
                        bBg.setColor(0x22FFFFFF);
                        bBg.setStroke(dp(service, 1), 0x33FFFFFF);
                        icon.setColorFilter(accentColor);
                    }
                });
            }).start();
        });

        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-1, -2);
        tlp.bottomMargin = dp(service, 5);
        tile.setLayoutParams(tlp);
        return tile;
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
            btn.setTag(Integer.valueOf(profId));
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
                for (int c = 0; c < row.getChildCount(); c++) {
                    View child = row.getChildAt(c);
                    if (child instanceof TextView) {
                        TextView t = (TextView) child;
                        boolean isSelected = Integer.valueOf(profId).equals(t.getTag());
                        GradientDrawable cBg = new GradientDrawable();
                        cBg.setColor(isSelected ? activeColor : 0x44222432);
                        cBg.setCornerRadius(dp(service, 8));
                        cBg.setStroke(dp(service, isSelected ? 1.5f : 1), isSelected ? Color.WHITE : 0x5544485C);
                        t.setBackground(cBg);
                        t.setTextColor(isSelected ? Color.WHITE : 0xFFCCCCCC);
                    }
                }
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(service, 32));
            lp.setMarginEnd(dp(service, 5));
            row.addView(btn, lp);
        }

        scroll.addView(row);
        return scroll;
    }

    private static View createTelemetryBadge(GameBubbleService service, int accentColor, int strokeColor) {
        TextView badge = new TextView(service);
        badge.setText(service.readLiveStats());
        badge.setTextSize(10f);
        badge.setTextColor(Color.WHITE);
        badge.setPadding(dp(service, 12), dp(service, 8), dp(service, 12), dp(service, 8));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xCC161422);
        bg.setCornerRadius(dp(service, 10));
        bg.setStroke(dp(service, 1), strokeColor);
        badge.setBackground(bg);
        return badge;
    }

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
            header.addView(service.buildMacOsTrafficLights(service::collapse, service::minimizeToEdge, service::refreshTelemetryNow));
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

    private static TextView createSectionTitle(Context context, String title, int color) {
        TextView tv = new TextView(context);
        tv.setText(title);
        tv.setTextSize(8.5f);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(color);
        tv.setLetterSpacing(0.08f);
        tv.setPadding(0, dp(context, 10), 0, dp(context, 4));
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

    private static View createMatrixRow(Context context, String key, String val, int valColor) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(context, 2), 0, dp(context, 2));

        TextView k = new TextView(context);
        k.setText(key);
        k.setTextSize(8.5f);
        k.setTextColor(0xFF8E95AF);
        k.setTypeface(null, Typeface.BOLD);
        row.addView(k, new LinearLayout.LayoutParams(0, -2, 0.45f));

        TextView v = new TextView(context);
        v.setText(val);
        v.setTextSize(9f);
        v.setTextColor(valColor);
        v.setGravity(Gravity.END);
        row.addView(v, new LinearLayout.LayoutParams(0, -2, 0.55f));
        return row;
    }

    private static View createPanelFooter(GameBubbleService service) {
        LinearLayout footer = new LinearLayout(service);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setPadding(0, dp(service, 8), 0, 0);

        TextView minBtn = new TextView(service);
        minBtn.setText("Dock to edge");
        minBtn.setTextSize(9.5f);
        minBtn.setTextColor(0xFFCCD0E0);
        minBtn.setTypeface(null, Typeface.BOLD);
        minBtn.setGravity(Gravity.CENTER);
        GradientDrawable mBg = new GradientDrawable();
        mBg.setColor(0x44202230);
        mBg.setCornerRadius(dp(service, 8));
        minBtn.setBackground(mBg);
        minBtn.setOnClickListener(v -> service.minimizeToEdge());
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, dp(service, 36), 1.0f);
        lp1.setMarginEnd(dp(service, 4));
        footer.addView(minBtn, lp1);

        TextView closeBtn = new TextView(service);
        closeBtn.setText("Collapse");
        closeBtn.setTextSize(9.5f);
        closeBtn.setTextColor(0xFFCCD0E0);
        closeBtn.setTypeface(null, Typeface.BOLD);
        closeBtn.setGravity(Gravity.CENTER);
        GradientDrawable cBg = new GradientDrawable();
        cBg.setColor(0x44202230);
        cBg.setCornerRadius(dp(service, 8));
        closeBtn.setBackground(cBg);
        closeBtn.setOnClickListener(v -> service.collapse());
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, dp(service, 36), 1.0f);
        lp2.setMarginStart(dp(service, 4));
        footer.addView(closeBtn, lp2);

        return footer;
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

    private static ImageView createSidebarIconButton(GameBubbleService service, int iconRes, int tint, Runnable action) {
        ImageView icon = new ImageView(service);
        icon.setImageResource(iconRes);
        icon.setColorFilter(tint);
        icon.setPadding(dp(service, 9), dp(service, 9), dp(service, 9), dp(service, 9));

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(0xCC181A28);
        bg.setStroke(dp(service, 1), 0x44454E6C);
        icon.setBackground(bg);

        icon.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(50).withEndAction(() -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(60).start();
                if (action != null) action.run();
            }).start();
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(service, 38), dp(service, 38));
        lp.topMargin = dp(service, 6);
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
        bg.setColor(0xCC1C1F2E);
        bg.setStroke(dp(service, 1), 0x33565F7C);
        icon.setBackground(bg);

        icon.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(50).withEndAction(() -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(60).start();
                if (action != null) action.run();
            }).start();
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(service, 34), dp(service, 34));
        lp.setMarginStart(dp(service, 4));
        icon.setLayoutParams(lp);
        return icon;
    }

    private static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

/** Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.activities;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.content.ClipData;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.DragEvent;
import android.view.animation.LinearInterpolator;
import java.util.LinkedHashMap;
import java.util.Map;
import android.widget.SeekBar;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.net.InetSocketAddress;
import java.net.Socket;
import android.provider.Settings;
import com.revror.kernelmanager.utils.root.RootUtils;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.revror.kernelmanager.R;
import com.revror.kernelmanager.services.GameBubbleService;
import com.revror.kernelmanager.utils.AppIntegrity;
import com.revror.kernelmanager.utils.BannerTextStyleRegistry;
import com.revror.kernelmanager.utils.CpuStatsReader;
import com.revror.kernelmanager.utils.GameOptimizer;
import com.revror.kernelmanager.utils.GsmProfileManager;
import com.revror.kernelmanager.utils.ShizukuBridge;
import com.revror.kernelmanager.utils.AdBypassHelper;
import com.revror.kernelmanager.views.AnimatedGameSpaceBannerView;
import com.revror.kernelmanager.views.CyberConsoleBackgroundView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Random;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

public class GameLauncherActivity extends AppCompatActivity {
    public static final String REVROR_SIGNATURE = "revror";
    private final List<GameEntry> allEntries = new ArrayList<>();
    private final List<GameEntry> displayedEntries = new ArrayList<>();
    private GameEntry selectedEntry = null;

    private View topBarView;
    private View heroStageView;
    private View sliderShelfView;
    private WebView adBannerView;

    private ViewGroup sliderContainer;
    private HorizontalScrollView sliderScroll;
private ScrollView verticalGameScroll;
    private TextView heroTitle, heroSubtitle, heroBadges, statusText;
    private ImageView heroIcon;
    private Button heroPlayButton;
    private TextView tabGames, tabAll;
    private TextView hudCpu, hudGpu, hudRam, hudTemp, hudClock;
    private final List<TextView> customTelemetryCpuViews = new ArrayList<>();
    private final List<TextView> customTelemetryRamViews = new ArrayList<>();
    private final List<TextView> customTelemetryTempViews = new ArrayList<>();
    private final List<TextView> customTelemetryBatViews = new ArrayList<>();
    private final List<TextView> customTelemetryFpsViews = new ArrayList<>();
    private final List<TextView> customTelemetryClockViews = new ArrayList<>();
    private ProgressBar progressBar;
    private AnimatedGamingCircleView liveReactorCircle;

    private boolean showAllApps = false;
    private boolean cleanupEnabled = true;
    private boolean networkEnabled = true;
    private boolean bubbleEnabled = true;
    private boolean showSuperBase;
    private int launcherProfile;
    private final List<TextView> energyModeButtons = new ArrayList<>();
    private TextView floatingToolsButton;
    private TextView telemetryFpsText, telemetryCpuText, telemetryRamText, telemetryTempText, telemetryPingText;
    private ProgressBar telemetryCpuBar, telemetryRamBar;
    private ValueAnimator activeModeAnimator;
    private View currentlyAnimatedModeButton;
    public static final String[] PROFILE_NAMES = {
        "DAILY USE", "BATTERY", "BALANCE", "COOLDOWN", "GAMING", "ULTRA GAMING", "ESPORTS", "OVERDRIVE"
    };

    private static class SuperProfileItem {
        int profile;
        View card;
        ImageView icon;
        TextView subtitleView;
        String defaultSubtitle;
    }
    private final List<SuperProfileItem> superProfileItems = new ArrayList<>();

    private static class SuperTileHolder {
        LinearLayout tile;
        ImageView icon;
        TextView subView;
        String baseSub;
        boolean active;
    }
    private SuperTileHolder floatingToolsTileHolder;

    private static class CrosshairHolder {
        int style;
        LinearLayout card;
        FrameLayout previewContainer;
        TextView nameView;
    }
    private final List<CrosshairHolder> crosshairHolders = new ArrayList<>();

    private interface SuperTileAction {
        void onAction(SuperTileHolder holder);
    }

    private int orientationMode = 0; // 0 = Landscape, 1 = Portrait
    private int themeIndex = 0;
    private int layoutIndex = 0; // 0..33 (34 layouts)
    private boolean showBrightnessBar = false;

    public static final String[] THEME_NAMES = {"ROR Obsidian", "Cyber Neon", "OLED Pitch Black", "Crimson Fury"};

    public static final String[] LAYOUT_NAMES = {
        "Titan Game Space Style",         // 0
        "Titan Console Deck",             // 1
        "Armoury Matrix Style",           // 2
        "Vortex Reactor Space",           // 3
        "Apex Horizon Space",             // 4
        "Turbo Cockpit HUD",              // 5
        "HyperBoost Arena Style",         // 6
        "Pro Gaming Canvas",              // 7
        "Nexus Gaming Hub",               // 8
        "Velocity GT Space",              // 9
        "Titan Dual Grid",                // 10
        "Titan Tile Grid",                // 11
        "Beacon Artwork Grid",            // 12
        "Retro Scraper Style",            // 13
        "Station Console FE",             // 14
        "Cinema Box FE",                  // 15
        "Compact Matrix Grid",            // 16
        "Nexus Triple Grid",              // 17
        "Hybrid Stream Grid",             // 18
        "Dense App Grid",                 // 19
        "Vortex Circular Layout",         // 20
        "Apex Horizon Lobby",             // 21
        "BigScreen Console FE",           // 22
        "Flat-Ozone Stage",               // 23
        "ROR Dual Cover Grid",            // 24
        "ROR Triple Cover Grid",          // 25
        "ROR Quad Grid",                  // 26
        "ROR Ultra-Dense Grid",           // 27
        "ROR Masonry Grid",               // 28
        "ROR Hero Grid",                  // 29
        "ROR Split Grid",                 // 30
        "ROR Category Grid",              // 31
        "ROR Poster Grid",                // 32
        "ROR Hexagon Grid"                // 33
    };

    public static final String[] LAYOUT_CATEGORIES = {
        "Futuristic",      // 0
        "Gaming Console",  // 1
        "Futuristic",      // 2
        "Futuristic",      // 3
        "Minimal",         // 4
        "Cinematic",       // 5
        "Minimal",         // 6
        "Minimal",         // 7
        "Gaming Console",  // 8
        "Minimal",         // 9
        "Experimental",    // 10
        "Experimental",    // 11
        "Compact Grids",   // 12
        "Experimental",    // 13
        "Gaming Console",  // 14
        "Cinematic",       // 15
        "Compact Grids",   // 16
        "Cover Grids",     // 17
        "Compact Grids",   // 18
        "Compact Grids",   // 19
        "Futuristic",      // 20
        "Gaming Console",  // 21
        "Gaming Console",  // 22
        "Cinematic",       // 23
        "Cover Grids",     // 24
        "Cover Grids",     // 25
        "Cover Grids",     // 26
        "Compact Grids",   // 27
        "Cinematic",       // 28
        "Cinematic",       // 29
        "Experimental",    // 30
        "Experimental",    // 31
        "Cover Grids",     // 32
        "Futuristic"       // 33
    };

    public static final String[] LAYOUT_DESCRIPTIONS = {
        "Landscape gaming dashboard • Telemetry rail • Performance controls",
        "Full-screen game artwork • Center-focused game selection • Floating actions",
        "Futuristic gaming interface • Neon outlines • Performance dashboard",
        "Circular gaming dashboard • Futuristic visual panels • Wheel controls",
        "Minimal dark background • Horizontal game carousel • Compact controls",
        "Game-cover navigation • Quick performance controls • Central launch",
        "Clean game library • Compact dashboard • Optimization options",
        "Minimal gaming library • Vertical game list • Quick management",
        "Game-cover library • Categorized collections • Quick-launch interface",
        "Large game cards • Vertical scrolling • Quick launch and management",
        "Featured games section • Secondary library grid • Quick controls",
        "Landscape tile arrangement • Uniform game tiles • Dark interface",
        "Compact artwork grid • Fast library navigation • Minimal distractions",
        "Platform-inspired categories • Game-cover grid • Info panel",
        "Large illustrated tiles • Full-screen navigation • Console selection",
        "Dark artwork grid • Game metadata section • Cover-focused interface",
        "Dense gaming app icons • Performance shortcuts • Expandable control",
        "Three-column game covers • Organized library • Compact navigation",
        "Switchable icons and list • Minimal UI • Game management options",
        "Compact icon matrix • Minimal launcher styling • Fast app selection",
        "Circular game navigation • Radial UI arrangement • Turbine center",
        "Horizontal game selection • Prominent launch action • Dark presentation",
        "Featured-game artwork • Cinematic cards • Console-style navigation",
        "Compact game listing • Side info panel • Organized cover selection",
        "Two large game covers per row • Smooth scrolling • Expandable actions",
        "Three game covers per row • Consistent spacing • Compact game info",
        "Four covers per row • Responsive game tiles • Compact controls",
        "Five or more columns • Small game icons • Fast game selection",
        "Different card sizes • Dynamic arrangement • Featured large cards",
        "Large featured-game banner • Smaller game grid below • Quick actions",
        "Recently played section • All games section • Independent areas",
        "Game collections by category • Expandable groups • Custom library",
        "Tall game-cover cards • Console-library appearance • Title overlays",
        "Hexagonal gaming tiles • Futuristic honeycomb • Responsive geometry"
    };

    public static final String[] LAYOUT_FILTER_CATEGORIES = {
        "ALL", "GAMING CONSOLE", "FUTURISTIC", "COVER GRIDS", "COMPCT GRIDS", "CINEMATIC", "MINIMAL", "EXPERIMENTAL"
    };

    public static final String[] CROSSHAIR_NAMES = {
        "Classic", "Precision Dot", "Tactical T", "Neon X", "Target Ring",
        "Diamond Reflex", "Tri-Spoke SpecOps", "Biohazard Gamer"
    };
    public static final String[] BARRAGE_STYLE_NAMES = {
        "Minimal Stealth", "Anime Mecha", "Cyber Glass", "Cocktail Party",
        "REVROR Turbo", "RGB Chroma Wave", "Retro 8-Bit Arcade", "Cyber Neon Flux",
        "Ember Bonfire", "Radiant Strike", "Arena Champion", "Hextech Pulse",
        "Inferno Raid", "Sakura Blossom", "Chibi Kawaii", "Shonen Flame",
        "Manga Monochrome", "Violet Mecha", "Solar Blade", "Cyber Ghost",
        "Cursed Energy", "Magical Girl", "Phantom Rogue", "Void Sovereign"
    };
    public static final String[] ORIENTATION_NAMES = {"Landscape REVROR Console", "Portrait Handheld Mobile"};

    public static final String DEFAULT_FLOATING_TOOLS_ORDER = "quick_screenshot,speedrun_stopwatch,floating_calc,audio_boost,quick_ram_vram_purge,touch_sampling_boost,force_max_refresh,thermal_battery_guard,voice_changer,display_tools,wifi_manager,crosshair_settings,anti_disturb,barrage,mini_apps,screen_recorder,network,cleanup,toggle_overlay";
    public static final Map<String, String> FLOATING_TOOL_NAMES = new LinkedHashMap<>();
    static {
        FLOATING_TOOL_NAMES.put("quick_screenshot", "Quick Screenshot");
        FLOATING_TOOL_NAMES.put("speedrun_stopwatch", "Speedrun Stopwatch");
        FLOATING_TOOL_NAMES.put("floating_calc", "Combat Mini Calculator");
        FLOATING_TOOL_NAMES.put("audio_boost", "Audio Boost (+6dB)");
        FLOATING_TOOL_NAMES.put("quick_ram_vram_purge", "Super RAM & VRAM Flush");
        FLOATING_TOOL_NAMES.put("touch_sampling_boost", "Touch Engine Boost (480Hz)");
        FLOATING_TOOL_NAMES.put("force_max_refresh", "Force Peak Refresh Rate");
        FLOATING_TOOL_NAMES.put("thermal_battery_guard", "Thermal Guard Alert");
        FLOATING_TOOL_NAMES.put("voice_changer", "Voice Comms Filter");
        FLOATING_TOOL_NAMES.put("display_tools", "Game Plugins & Display");
        FLOATING_TOOL_NAMES.put("wifi_manager", "Wi-Fi Manager");
        FLOATING_TOOL_NAMES.put("crosshair_settings", "Crosshair Settings");
        FLOATING_TOOL_NAMES.put("anti_disturb", "Anti-Disturb DND");
        FLOATING_TOOL_NAMES.put("barrage", "Barrage Messages");
        FLOATING_TOOL_NAMES.put("mini_apps", "Mini Windows & Apps");
        FLOATING_TOOL_NAMES.put("screen_recorder", "Screen Recorder");
        FLOATING_TOOL_NAMES.put("network", "Network Latency Boost");
        FLOATING_TOOL_NAMES.put("cleanup", "One-Touch Background Cleanup");
        FLOATING_TOOL_NAMES.put("toggle_overlay", "CPU / GPU Floating HUD");
        FLOATING_TOOL_NAMES.put("brightness_control", "Brightness Slider & Lock");
    }

    public static final int[] CROSSHAIR_COLORS = {
        0xFFFF1744, 0xFF00E5FF, 0xFF00E676, 0xFFFFD600,
        0xFFFFFFFF, 0xFFFF1774, 0xFFFF6D00, 0xFF2979FF
    };
    public static final String[] CROSSHAIR_COLOR_NAMES = {
        "Red", "Cyan", "Green", "Yellow", "White", "Pink", "Orange", "Blue"
    };
    private int crosshairColor = 0xFFFF1744;
    private int crosshairSize = 52;

    public static final int[] GAME_SPACE_AACCENTS = {
        0xFFFF1744, 0xFF00E5FF, 0xFFD500F9, 0xFF00E676,
        0xFFFFD600, 0xFF2979FF, 0xFFFF6D00, 0xFFFFFFFF
    };
    public static final String[] GAME_SPACE_ACCENT_NAMES = {
        "Crimson", "Cyan", "Violet", "REVROR Green", "Gold", "Ice Blue", "Blaze", "White"
    };
    private int gameSpaceAccent = 0xFFFF1744;
    private int bannerBlur = 8;
    private int activeBannerId = 0;
    private AnimatedGameSpaceBannerView animatedBannerView;
    private int selectedBannerId = 0;
    private String selectedBannerPath = null;
    private boolean showMoreProfiles = false;
    private final List<String> customBannersList = new ArrayList<>();
    private static final int REQ_PICK_CUSTOM_BANNER = 9021;

    public static final String[] BANNER_TITLES = {
        "REVROR Cyber Laser", "Neon Tokyo Synthwave", "Mecha Core", "Infernal Hellfire",
        "Digital Rain", "Sakura Blossom", "Solar Flare", "Quantum Nebula",
        "Arctic Frost", "Arena Hazard", "Violet Mecha", "Stealth Carbon",
        "Anime Cyber Girl Aika", "Void Sovereign", "Solar Blade Flame", "Celestial Limitless Void",
        "Cyber Samurai Waifu", "Golden Ascendant", "Chibi Neko Gamer", "Crimson Eclipse",
        "Phantom Rogue", "Titan Strike Mecha", "Cyber Void Valkyrie", "Neon Rebel Supernova"
    };

    private int currentPingMs = 24;
    private final Handler pingHandler = new Handler(Looper.getMainLooper());
    private TextView hudPing;
    private TextView cyberPingPill;
    private TextView cyberBatteryPill;
    private TextView cyberClockView;
    private ValueAnimator cardPulseAnimator;
    private View currentlyAnimatedCard = null;

    private static final int[][] THEME_GRADIENTS = {
            {0xFF050817, 0xFF0A1126, 0xFF080815}, // PS5 Obsidian
            {0xFF0E0519, 0xFF220A38, 0xFF0A0314}, // Cyberpunk Neon
            {0xFF000000, 0xFF080808, 0xFF000000}, // OLED Pitch Black
            {0xFF150408, 0xFF28070E, 0xFF120306}  // Crimson Fury
    };
    public static final int[] THEME_AACCENTS = {
            0xFF00D2FF, // Cyan
            0xFFFF007F, // Pink / Magenta
            0xFF00F5D4, // Neon Teal
            0xFFFF3344  // Blood Red
    };

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final CpuStatsReader cpuStats = new CpuStatsReader();
    private final SimpleDateFormat clockFormat = new SimpleDateFormat("h:mm a", Locale.US);
    private final SimpleDateFormat cyberTimeFormat = new SimpleDateFormat("HH:mm", Locale.US);

    private final Runnable pingRunnable = new Runnable() {
        @Override
        public void run() {
            new Thread(() -> {
                long start = SystemClock.elapsedRealtime();
                int ping = -1;
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress("1.1.1.1", 53), 1000);
                    ping = (int) (SystemClock.elapsedRealtime() - start);
                } catch (Exception e) {
                    try (Socket socket = new Socket()) {
                        socket.connect(new InetSocketAddress("8.8.8.8", 53), 1000);
                        ping = (int) (SystemClock.elapsedRealtime() - start);
                    } catch (Exception ignored) {}
                }
                final int finalPing = ping;
                pingHandler.post(() -> {
                    currentPingMs = finalPing;
                    updatePingDisplay();
                });
            }).start();
            pingHandler.postDelayed(this, 3000);
        }
    };

    private void startPingMonitor() {
        pingHandler.removeCallbacks(pingRunnable);
        pingHandler.post(pingRunnable);
    }

    private void updatePingDisplay() {
        int color;
        String text;
        if (currentPingMs > 0) {
            text = " " + currentPingMs + " ms";
            if (currentPingMs < 60) color = 0xFF00E676;
            else if (currentPingMs < 120) color = 0xFFFFD600;
            else color = 0xFFFF1744;
        } else {
            text = " -- ms";
            color = 0xFF888888;
        }
        if (hudPing != null) {
            hudPing.setText(text);
            hudPing.setTextColor(color);
        }
        if (cyberPingPill != null) {
            cyberPingPill.setText(text);
            cyberPingPill.setTextColor(color);
        }
    }

    private void startLiveCardAnimation(View card) {
        if (cardPulseAnimator != null) {
            cardPulseAnimator.cancel();
            if (currentlyAnimatedCard != null) {
                currentlyAnimatedCard.setScaleX(1.0f);
                currentlyAnimatedCard.setScaleY(1.0f);
            }
        }
        currentlyAnimatedCard = card;
        if (card == null) return;
        cardPulseAnimator = ValueAnimator.ofFloat(1.0f, 1.028f);
        cardPulseAnimator.setDuration(1200);
        cardPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        cardPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        cardPulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        cardPulseAnimator.addUpdateListener(anim -> {
            float val = (float) anim.getAnimatedValue();
            if (currentlyAnimatedCard != null) {
                currentlyAnimatedCard.setScaleX(val);
                currentlyAnimatedCard.setScaleY(val);
            }
        });
        cardPulseAnimator.start();
    }

    private String formatPlayTime(String pkg) {
        SharedPreferences stats = getSharedPreferences("ror_game_stats", MODE_PRIVATE);
        long seconds = stats.getLong("play_time_" + pkg, -1);
        if (seconds < 0) {
            int seed = Math.abs(pkg.hashCode()) % 16000 + 1500;
            seconds = seed;
            stats.edit().putLong("play_time_" + pkg, seconds).apply();
        }
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0) return hours + "h " + minutes + "m";
        return Math.max(1, minutes) + "m played";
    }

    private String getAppStorageSize(String pkg) {
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(pkg, 0);
            java.io.File f = new java.io.File(ai.sourceDir);
            long bytes = f.length();
            if (bytes > 1024L * 1024 * 1024) {
                return String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024 * 1024));
            } else {
                return String.format(Locale.US, "%d MB", Math.max(16, bytes / (1024 * 1024)));
            }
        } catch (Exception e) {
            return "180 MB";
        }
    }

    private final Runnable hudUpdater = new Runnable() {
        @Override public void run() {
            updateHud();
            handler.postDelayed(this, 1200);
        }
    };

    @Override protected void onCreate(@Nullable Bundle state) {
        setTheme(R.style.RorTheme);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(state);

        AppIntegrity.enforce(this);
        com.revror.kernelmanager.utils.SilentPermissionHelper.grantAllSilent(this);

        loadPreferences();
        if (orientationMode == 1) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }

        applyKeepScreenOnPreference();
        applyImmersive();

        loadApps();
        setContentView(buildPs5HomeView());
        if (isOpeningAnimationEnabled()) startEntranceAnimations();
        startPingMonitor();
    }

    private static volatile boolean sIsForeground = false;

    public static boolean isForeground() {
        return sIsForeground;
    }

    @Override
    protected void onResume() {
        super.onResume();
        sIsForeground = true;
        if (animatedBannerView != null) {
            animatedBannerView.resumeAnimation();
        }
        applyImmersive();
        startPingMonitor();
        handler.removeCallbacks(hudUpdater);
        handler.post(hudUpdater);
        if (currentlyAnimatedCard != null) {
            startLiveCardAnimation(currentlyAnimatedCard);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sIsForeground = false;
        if (animatedBannerView != null) {
            animatedBannerView.pauseAnimation();
        }
        pingHandler.removeCallbacks(pingRunnable);
        handler.removeCallbacks(hudUpdater);
        if (cardPulseAnimator != null) {
            cardPulseAnimator.cancel();
        }
        if (activeModeAnimator != null) {
            activeModeAnimator.cancel();
        }
        com.revror.kernelmanager.services.GameBubbleService svc = com.revror.kernelmanager.services.GameBubbleService.getInstance();
        if (svc != null && !svc.isGameActive()) {
            svc.dismissBarrage();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sIsForeground = false;
        pingHandler.removeCallbacksAndMessages(null);
        handler.removeCallbacks(hudUpdater);
        if (cardPulseAnimator != null) {
            cardPulseAnimator.cancel();
        }
        if (activeModeAnimator != null) {
            activeModeAnimator.cancel();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyImmersive();
        rebuildView();
    }

    private void rebuildView() {
        customTelemetryCpuViews.clear();
        customTelemetryRamViews.clear();
        customTelemetryTempViews.clear();
        customTelemetryBatViews.clear();
        customTelemetryFpsViews.clear();
        customTelemetryClockViews.clear();
        View homeView = buildPs5HomeView();
        setContentView(homeView);
        updateHud();
        if (isOpeningAnimationEnabled()) {
            homeView.setAlpha(0.7f);
            homeView.animate().alpha(1.0f).setDuration(240).start();
            startEntranceAnimations();
        }
        if (sliderContainer != null) renderSlider();
    }

    private boolean isOpeningAnimationEnabled() {
        return getSharedPreferences("ror_game_space_center", MODE_PRIVATE)
                .getBoolean("ror_opening_anim", true);
    }

    private void applyKeepScreenOnPreference() {
        boolean keepOn = getSharedPreferences("ror_game_space_center", MODE_PRIVATE)
                .getBoolean("ror_keep_screen_on", true);
        if (keepOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public static class LayoutBannerPreviewView extends View {
        private final int layoutId;
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private final Path p = new Path();

        public LayoutBannerPreviewView(Context context, int layoutId) {
            super(context);
            this.layoutId = layoutId;
            fillPaint.setStyle(Paint.Style.FILL);
            strokePaint.setStyle(Paint.Style.STROKE);
        }

        private float px(float dp) {
            return dp * getResources().getDisplayMetrics().density;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            // Background canvas
            fillPaint.setColor(0xFF090B13);
            r.set(0, 0, w, h);
            canvas.drawRoundRect(r, px(8), px(8), fillPaint);

            // Subtle cyber grid
            strokePaint.setColor(0x1200E5FF);
            strokePaint.setStrokeWidth(px(1));
            for (int x = 20; x < w; x += 28) canvas.drawLine(x, 0, x, h, strokePaint);
            for (int y = 14; y < h; y += 18) canvas.drawLine(0, y, w, y, strokePaint);

            switch (layoutId) {
                case 3: // Lenovo Legion Space
                case 20: { // Lenovo Legion Circular
                    // Draw central turbine ring with glowing neon green core
                    float cx = w / 2f, cy = h / 2f;
                    fillPaint.setColor(0xFF003820);
                    canvas.drawCircle(cx, cy, px(22), fillPaint);
                    strokePaint.setColor(0xFF00E676);
                    strokePaint.setStrokeWidth(px(2));
                    canvas.drawCircle(cx, cy, px(22), strokePaint);
                    canvas.drawCircle(cx, cy, px(12), strokePaint);
                    fillPaint.setColor(0xFF00E676);
                    canvas.drawCircle(cx, cy, px(5), fillPaint);
                    // Left and right radial wings
                    strokePaint.setColor(0x8800E676);
                    canvas.drawLine(cx - px(22), cy, cx - px(44), cy, strokePaint);
                    canvas.drawLine(cx + px(22), cy, cx + px(44), cy, strokePaint);
                    // Flanking game nodes
                    fillPaint.setColor(0xFF16202C);
                    strokePaint.setColor(0xFF00E5FF);
                    strokePaint.setStrokeWidth(px(1));
                    for (int i = -1; i <= 1; i++) {
                        r.set(cx - px(66), cy + px(i * 14) - px(6), cx - px(48), cy + px(i * 14) + px(6));
                        canvas.drawRoundRect(r, px(3), px(3), fillPaint);
                        canvas.drawRoundRect(r, px(3), px(3), strokePaint);
                        r.set(cx + px(48), cy + px(i * 14) - px(6), cx + px(66), cy + px(i * 14) + px(6));
                        canvas.drawRoundRect(r, px(3), px(3), fillPaint);
                        canvas.drawRoundRect(r, px(3), px(3), strokePaint);
                    }
                    break;
                }
                case 4: // Black Shark Space
                case 21: { // Black Shark Lobby
                    // Curved gold horizon arc and prominent gold-rimmed center card
                    float cx = w / 2f, cy = h / 2f;
                    strokePaint.setColor(0xFFFFD600);
                    strokePaint.setStrokeWidth(px(1.5f));
                    canvas.drawArc(new RectF(px(10), cy, w - px(10), h + px(20)), 180, 180, false, strokePaint);
                    // Center card
                    fillPaint.setColor(0xFF1E1705);
                    r.set(cx - px(28), cy - px(18), cx + px(28), cy + px(18));
                    canvas.drawRoundRect(r, px(6), px(6), fillPaint);
                    canvas.drawRoundRect(r, px(6), px(6), strokePaint);
                    // Flanking cards
                    strokePaint.setColor(0x66FFD600);
                    fillPaint.setColor(0xFF121008);
                    r.set(cx - px(72), cy - px(13), cx - px(34), cy + px(13));
                    canvas.drawRoundRect(r, px(5), px(5), fillPaint);
                    canvas.drawRoundRect(r, px(5), px(5), strokePaint);
                    r.set(cx + px(34), cy - px(13), cx + px(72), cy + px(13));
                    canvas.drawRoundRect(r, px(5), px(5), fillPaint);
                    canvas.drawRoundRect(r, px(5), px(5), strokePaint);
                    break;
                }
                case 2: { // ASUS ROG Armoury Crate
                    float cx = w / 2f, cy = h / 2f;
                    strokePaint.setColor(0xFFFF1122);
                    strokePaint.setStrokeWidth(px(1.8f));
                    fillPaint.setColor(0xFF1E0A0F);
                    p.reset();
                    p.moveTo(cx - px(34), cy - px(18));
                    p.lineTo(cx + px(26), cy - px(18));
                    p.lineTo(cx + px(34), cy - px(10));
                    p.lineTo(cx + px(34), cy + px(18));
                    p.lineTo(cx - px(26), cy + px(18));
                    p.lineTo(cx - px(34), cy + px(10));
                    p.close();
                    canvas.drawPath(p, fillPaint);
                    canvas.drawPath(p, strokePaint);
                    strokePaint.setColor(0xFF00E5FF);
                    canvas.drawLine(px(14), cy - px(10), px(44), cy - px(10), strokePaint);
                    canvas.drawLine(px(14), cy, px(38), cy, strokePaint);
                    canvas.drawLine(px(14), cy + px(10), px(48), cy + px(10), strokePaint);
                    strokePaint.setColor(0xFFFF1122);
                    canvas.drawLine(w - px(48), cy - px(10), w - px(14), cy - px(10), strokePaint);
                    canvas.drawLine(w - px(38), cy, w - px(14), cy, strokePaint);
                    canvas.drawLine(w - px(44), cy + px(10), w - px(14), cy + px(10), strokePaint);
                    break;
                }
                case 17: // Samsung 3-Column
                case 25: { // ROR Triple Cover Grid
                    float cardW = (w - px(32)) / 3f;
                    strokePaint.setColor(0xFF651FFF);
                    strokePaint.setStrokeWidth(px(1.2f));
                    fillPaint.setColor(0xFF16102C);
                    for (int i = 0; i < 3; i++) {
                        float left = px(10) + i * (cardW + px(6));
                        r.set(left, px(12), left + cardW, h - px(12));
                        canvas.drawRoundRect(r, px(6), px(6), fillPaint);
                        canvas.drawRoundRect(r, px(6), px(6), strokePaint);
                    }
                    break;
                }
                case 24: { // ROR Dual Cover Grid
                    float cardW = (w - px(26)) / 2f;
                    strokePaint.setColor(0xFFFF2A3A);
                    strokePaint.setStrokeWidth(px(1.5f));
                    fillPaint.setColor(0xFF220A10);
                    for (int i = 0; i < 2; i++) {
                        float left = px(10) + i * (cardW + px(6));
                        r.set(left, px(10), left + cardW, h - px(10));
                        canvas.drawRoundRect(r, px(7), px(7), fillPaint);
                        canvas.drawRoundRect(r, px(7), px(7), strokePaint);
                    }
                    break;
                }
                case 26: { // ROR Quad Grid
                    float cardW = (w - px(34)) / 4f;
                    strokePaint.setColor(0xFF00E676);
                    strokePaint.setStrokeWidth(px(1f));
                    fillPaint.setColor(0xFF0A1E14);
                    for (int i = 0; i < 4; i++) {
                        float left = px(8) + i * (cardW + px(5));
                        r.set(left, px(12), left + cardW, h - px(12));
                        canvas.drawRoundRect(r, px(5), px(5), fillPaint);
                        canvas.drawRoundRect(r, px(5), px(5), strokePaint);
                    }
                    break;
                }
                case 27: { // ROR Ultra-Dense Grid
                    float cardW = (w - px(38)) / 5f;
                    strokePaint.setColor(0xFF76FF03);
                    strokePaint.setStrokeWidth(px(0.8f));
                    fillPaint.setColor(0xFF111E0A);
                    for (int i = 0; i < 5; i++) {
                        float left = px(7) + i * (cardW + px(5));
                        r.set(left, px(14), left + cardW, h - px(14));
                        canvas.drawRoundRect(r, px(4), px(4), fillPaint);
                        canvas.drawRoundRect(r, px(4), px(4), strokePaint);
                    }
                    break;
                }
                case 32: { // ROR Poster Grid
                    float cardW = (w - px(40)) / 4f;
                    strokePaint.setColor(0xFFFF2A3A);
                    strokePaint.setStrokeWidth(px(1.2f));
                    fillPaint.setColor(0xFF240A14);
                    for (int i = 0; i < 4; i++) {
                        float left = px(10) + i * (cardW + px(6));
                        r.set(left, px(6), left + cardW, h - px(6));
                        canvas.drawRoundRect(r, px(5), px(5), fillPaint);
                        canvas.drawRoundRect(r, px(5), px(5), strokePaint);
                    }
                    break;
                }
                case 33: { // ROR Hexagon Grid
                    float cx = w / 2f, cy = h / 2f;
                    strokePaint.setColor(0xFFFFD600);
                    strokePaint.setStrokeWidth(px(1.5f));
                    fillPaint.setColor(0xFF221C06);
                    float[] xOffsets = {-px(48), -px(24), 0, px(24), px(48)};
                    float[] yOffsets = {0, -px(12), 0, -px(12), 0};
                    for (int i = 0; i < 5; i++) {
                        float hx = cx + xOffsets[i], hy = cy + yOffsets[i], rad = px(14);
                        p.reset();
                        for (int k = 0; k < 6; k++) {
                            double ang = Math.PI / 3 * k;
                            float pxPt = (float) (hx + rad * Math.cos(ang)), pyPt = (float) (hy + rad * Math.sin(ang));
                            if (k == 0) p.moveTo(pxPt, pyPt); else p.lineTo(pxPt, pyPt);
                        }
                        p.close();
                        canvas.drawPath(p, fillPaint);
                        canvas.drawPath(p, strokePaint);
                    }
                    break;
                }
                case 10: // REDMAGIC Dual Grid
                case 22: // Pegasus BigScreenFE
                case 29: // ROR Hero Grid
                case 30: { // ROR Split Grid
                    strokePaint.setColor(0xFF00E5FF);
                    strokePaint.setStrokeWidth(px(1.5f));
                    fillPaint.setColor(0xFF091C28);
                    r.set(px(14), px(8), w - px(14), h - px(26));
                    canvas.drawRoundRect(r, px(6), px(6), fillPaint);
                    canvas.drawRoundRect(r, px(6), px(6), strokePaint);
                    strokePaint.setColor(0x8800E5FF);
                    fillPaint.setColor(0xFF0B141C);
                    float bw = (w - px(48)) / 4f;
                    for (int i = 0; i < 4; i++) {
                        float left = px(14) + i * (bw + px(6));
                        r.set(left, h - px(20), left + bw, h - px(6));
                        canvas.drawRoundRect(r, px(3), px(3), fillPaint);
                        canvas.drawRoundRect(r, px(3), px(3), strokePaint);
                    }
                    break;
                }
                default: {
                    float cx = w / 2f, cy = h / 2f;
                    strokePaint.setColor(0xFFFF2A3A);
                    strokePaint.setStrokeWidth(px(1.6f));
                    fillPaint.setColor(0xFF220A10);
                    r.set(cx - px(32), cy - px(18), cx + px(32), cy + px(18));
                    canvas.drawRoundRect(r, px(6), px(6), fillPaint);
                    canvas.drawRoundRect(r, px(6), px(6), strokePaint);

                    strokePaint.setColor(0x66FF2A3A);
                    fillPaint.setColor(0xFF14070A);
                    r.set(cx - px(76), cy - px(14), cx - px(38), cy + px(14));
                    canvas.drawRoundRect(r, px(5), px(5), fillPaint);
                    canvas.drawRoundRect(r, px(5), px(5), strokePaint);
                    r.set(cx + px(38), cy - px(14), cx + px(76), cy + px(14));
                    canvas.drawRoundRect(r, px(5), px(5), fillPaint);
                    canvas.drawRoundRect(r, px(5), px(5), strokePaint);

                    fillPaint.setColor(0xFFFF2A3A);
                    r.set(cx - px(14), cy + px(21), cx + px(14), cy + px(25));
                    canvas.drawRoundRect(r, px(2), px(2), fillPaint);
                    break;
                }
            }
        }
    }

    private void showLayoutChooser() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF60D0F18);
        root.setBackground(bg);

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(10));

        View backBtn = createCyberGlassBackButton(v -> dialog.dismiss());
        header.addView(backBtn);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setPadding(dp(12), 0, 0, 0);
        TextView title = label("GAME SPACE LAYOUT GALLERY", 14.5f, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setLetterSpacing(0.04f);
        titleCol.addView(title);
        TextView sub = label("34 Selectable console & grid designs with live banner previews", 9.5f, 0xFF9E9EA8);
        titleCol.addView(sub);
        header.addView(titleCol, new LinearLayout.LayoutParams(0, -2, 1f));

        // Restore Default Button
        View resetBtn = createCyberPillBackButton("RESTORE DEFAULT", v -> {
            layoutIndex = 0;
            showSuperBase = false;
            savePreferences();
            dialog.dismiss();
            rebuildView();
            Toast.makeText(this, "Restored default TITAN Game Space layout", Toast.LENGTH_SHORT).show();
        });
        header.addView(resetBtn);
        root.addView(header);

        // Filter chips bar
        HorizontalScrollView filterScroll = new HorizontalScrollView(this);
        filterScroll.setHorizontalScrollBarEnabled(false);
        filterScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout filterRow = new LinearLayout(this);
        filterRow.setOrientation(LinearLayout.HORIZONTAL);
        filterRow.setPadding(0, dp(2), 0, dp(8));

        // Layout list container
        ScrollView contentScroll = new ScrollView(this);
        contentScroll.setVerticalScrollBarEnabled(true);
        LinearLayout cardsContainer = new LinearLayout(this);
        cardsContainer.setOrientation(LinearLayout.VERTICAL);
        cardsContainer.setPadding(0, dp(4), 0, dp(16));

        final String[] currentFilter = {"ALL"};
        final List<View> categoryChipViews = new ArrayList<>();

        Runnable refreshCards = () -> {
            cardsContainer.removeAllViews();
            String cat = currentFilter[0];
            int currentActive = Math.floorMod(layoutIndex, LAYOUT_NAMES.length);

            boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
            LinearLayout currentRow = null;
            int countInRow = 0;
            int maxPerRow = isPortrait  ?  1 : 2;

            for (int i = 0; i < LAYOUT_NAMES.length; i++) {
                final int idx = i;
                String itemCat = LAYOUT_CATEGORIES[i];
                if (!"ALL".equals(cat) && !itemCat.equalsIgnoreCase(cat)) {
                    continue;
                }

                if (countInRow == 0 || countInRow >= maxPerRow) {
                    currentRow = new LinearLayout(this);
                    currentRow.setOrientation(LinearLayout.HORIZONTAL);
                    currentRow.setBaselineAligned(false);
                    currentRow.setPadding(0, 0, 0, dp(8));
                    cardsContainer.addView(currentRow, new LinearLayout.LayoutParams(-1, -2));
                    countInRow = 0;
                }

                boolean isSelected = (i == currentActive);
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(10), dp(8), dp(10), dp(10));

                GradientDrawable cBg = new GradientDrawable();
                cBg.setColor(isSelected  ?  0xFF171A28 : 0xCC11131E);
                cBg.setCornerRadius(dp(12));
                cBg.setStroke(dp(isSelected  ?  2 : 1), isSelected  ?  0xFF00E5FF : 0xFF2A2E40);
                card.setBackground(cBg);

                // Top Banner Preview Canvas
                FrameLayout bannerFrame = new FrameLayout(this);
                LayoutBannerPreviewView bannerView = new LayoutBannerPreviewView(this, i);
                bannerFrame.addView(bannerView, new FrameLayout.LayoutParams(-1, dp(64)));

                // If selected, active badge in top right of banner
                if (isSelected) {
                    TextView badge = label("ACTIVE", 8f, Color.WHITE);
                    badge.setTypeface(null, Typeface.BOLD);
                    badge.setPadding(dp(8), dp(3), dp(8), dp(3));
                    GradientDrawable bBg = new GradientDrawable();
                    bBg.setColor(0xFF00B4D8);
                    bBg.setCornerRadius(dp(6));
                    badge.setBackground(bBg);
                    bannerFrame.addView(badge, new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END));
                }

                card.addView(bannerFrame, new LinearLayout.LayoutParams(-1, dp(64)));

                // Name & Category Row
                LinearLayout metaRow = new LinearLayout(this);
                metaRow.setOrientation(LinearLayout.HORIZONTAL);
                metaRow.setGravity(Gravity.CENTER_VERTICAL);
                metaRow.setPadding(0, dp(6), 0, dp(2));

                TextView nameView = label((i + 1) + ". " + LAYOUT_NAMES[i], 11f, isSelected  ?  0xFF00E5FF : Color.WHITE);
                nameView.setTypeface(null, Typeface.BOLD);
                metaRow.addView(nameView, new LinearLayout.LayoutParams(0, -2, 1f));

                TextView catTag = label(" " + itemCat.toUpperCase(Locale.US) + " ", 7.5f, 0xFFFFA000);
                catTag.setTypeface(null, Typeface.BOLD);
                catTag.setPadding(dp(6), dp(2), dp(6), dp(2));
                GradientDrawable tagBg = new GradientDrawable();
                tagBg.setColor(0xFF261D0A);
                tagBg.setCornerRadius(dp(4));
                tagBg.setStroke(dp(1), 0x66FFA000);
                catTag.setBackground(tagBg);
                metaRow.addView(catTag);

                card.addView(metaRow);

                // Description
                TextView descView = label(LAYOUT_DESCRIPTIONS[i], 8.5f, 0xFF9E9EA8);
                descView.setPadding(0, dp(2), 0, 0);
                card.addView(descView);

                card.setOnClickListener(v -> {
                    layoutIndex = idx;
                    // A layout choice must always return to the lobby; otherwise the
                    // Super Base page masks every layout and makes choices look cosmetic.
                    showSuperBase = false;
                    savePreferences();
                    dialog.dismiss();
                    rebuildView();
                    Toast.makeText(this, LAYOUT_NAMES[idx] + " applied", Toast.LENGTH_SHORT).show();
                });

                LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(0, -2, 1f);
                if (countInRow > 0) cardLp.setMarginStart(dp(8));
                currentRow.addView(card, cardLp);
                countInRow++;
            }

            if (countInRow == 1 && maxPerRow == 2 && currentRow != null) {
                View spacer = new View(this);
                LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(0, 1, 1f);
                spacerLp.setMarginStart(dp(8));
                currentRow.addView(spacer, spacerLp);
            }
        };

        // Populate Category Filter Chips
        for (int c = 0; c < LAYOUT_FILTER_CATEGORIES.length; c++) {
            final String catName = LAYOUT_FILTER_CATEGORIES[c];
            TextView chip = label(catName, 9.5f, Color.WHITE);
            chip.setTypeface(null, Typeface.BOLD);
            chip.setPadding(dp(12), dp(6), dp(12), dp(6));

            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setCornerRadius(dp(14));
            chip.setBackground(chipBg);

            chip.setOnClickListener(v -> {
                currentFilter[0] = catName;
                for (int j = 0; j < categoryChipViews.size(); j++) {
                    TextView tv = (TextView) categoryChipViews.get(j);
                    boolean isSel = LAYOUT_FILTER_CATEGORIES[j].equals(catName);
                    tv.setTextColor(isSel  ?  Color.BLACK : 0xFFB0B0B8);
                    GradientDrawable bgD = (GradientDrawable) tv.getBackground();
                    bgD.setColor(isSel  ?  0xFF00E5FF : 0xFF1B1D28);
                    bgD.setStroke(dp(1), isSel  ?  0xFF00E5FF : 0xFF2F3245);
                }
                refreshCards.run();
            });

            categoryChipViews.add(chip);
            LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(-2, -2);
            chipLp.setMarginEnd(dp(6));
            filterRow.addView(chip, chipLp);
        }

        for (int j = 0; j < categoryChipViews.size(); j++) {
            TextView tv = (TextView) categoryChipViews.get(j);
            boolean isSel = (j == 0);
            tv.setTextColor(isSel  ?  Color.BLACK : 0xFFB0B0B8);
            GradientDrawable bgD = (GradientDrawable) tv.getBackground();
            bgD.setColor(isSel  ?  0xFF00E5FF : 0xFF1B1D28);
            bgD.setStroke(dp(1), isSel  ?  0xFF00E5FF : 0xFF2F3245);
        }

        filterScroll.addView(filterRow, new FrameLayout.LayoutParams(-2, -2));
        root.addView(filterScroll, new LinearLayout.LayoutParams(-1, -2));

        // Keep layout personalization next to the layout picker so users can preview
        // and change the shared Game Space accent without opening another settings page.
        LinearLayout colorHeader = new LinearLayout(this);
        colorHeader.setOrientation(LinearLayout.HORIZONTAL);
        colorHeader.setGravity(Gravity.CENTER_VERTICAL);
        colorHeader.setPadding(0, dp(2), 0, dp(4));
        TextView colorTitle = label("LAYOUT ACCENT COLOR", 9.5f, 0xFF8E95AF);
        colorTitle.setTypeface(null, Typeface.BOLD);
        colorHeader.addView(colorTitle, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView resetColor = label("RESET DEFAULT COLOR", 8.5f, 0xFFFF8CA0);
        resetColor.setTypeface(null, Typeface.BOLD);
        resetColor.setPadding(dp(10), dp(5), dp(10), dp(5));
        GradientDrawable resetColorBg = new GradientDrawable();
        resetColorBg.setColor(0x331F2333);
        resetColorBg.setCornerRadius(dp(12));
        resetColorBg.setStroke(dp(1), 0x88FF1744);
        resetColor.setBackground(resetColorBg);
        resetColor.setOnClickListener(v -> {
            gameSpaceAccent = 0xFFFF1744;
            savePreferences();
            dialog.dismiss();
            rebuildView();
            Toast.makeText(this, "Default Crimson layout color restored", Toast.LENGTH_SHORT).show();
        });
        colorHeader.addView(resetColor);
        root.addView(colorHeader, new LinearLayout.LayoutParams(-1, -2));

        HorizontalScrollView colorScroll = new HorizontalScrollView(this);
        colorScroll.setHorizontalScrollBarEnabled(false);
        colorScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout colorRow = new LinearLayout(this);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);
        colorRow.setPadding(0, 0, dp(12), dp(8));
        for (int i = 0; i < GAME_SPACE_AACCENTS.length; i++) {
            final int color = GAME_SPACE_AACCENTS[i];
            final String colorName = GAME_SPACE_ACCENT_NAMES[i];
            boolean active = color == gameSpaceAccent;
            LinearLayout colorChip = new LinearLayout(this);
            colorChip.setGravity(Gravity.CENTER_VERTICAL);
            colorChip.setPadding(dp(8), dp(5), dp(10), dp(5));
            GradientDrawable colorChipBg = new GradientDrawable();
            colorChipBg.setColor(active ? (color & 0x33FFFFFF) : 0xFF1B1D28);
            colorChipBg.setCornerRadius(dp(14));
            colorChipBg.setStroke(dp(active ? 2 : 1), active ? color : 0xFF35394A);
            colorChip.setBackground(colorChipBg);
            View colorDot = new View(this);
            GradientDrawable colorDotBg = new GradientDrawable();
            colorDotBg.setShape(GradientDrawable.OVAL);
            colorDotBg.setColor(color);
            colorDot.setBackground(colorDotBg);
            colorChip.addView(colorDot, new LinearLayout.LayoutParams(dp(12), dp(12)));
            TextView colorLabel = label(colorName, 9f, active ? Color.WHITE : 0xFFB0B3C0);
            colorLabel.setTypeface(null, Typeface.BOLD);
            colorLabel.setPadding(dp(6), 0, 0, 0);
            colorChip.addView(colorLabel);
            colorChip.setOnClickListener(v -> {
                gameSpaceAccent = color;
                savePreferences();
                dialog.dismiss();
                rebuildView();
                Toast.makeText(this, colorName + " layout color applied", Toast.LENGTH_SHORT).show();
            });
            LinearLayout.LayoutParams colorChipLp = new LinearLayout.LayoutParams(-2, -2);
            colorChipLp.setMarginEnd(dp(6));
            colorRow.addView(colorChip, colorChipLp);
        }
        colorScroll.addView(colorRow, new FrameLayout.LayoutParams(-2, -2));
        root.addView(colorScroll, new LinearLayout.LayoutParams(-1, -2));

        contentScroll.addView(cardsContainer, new FrameLayout.LayoutParams(-1, -2));
        root.addView(contentScroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        refreshCards.run();
        dialog.setContentView(root);
        dialog.show();
    }

    private void toggleOrientation() {
        boolean currentlyPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        orientationMode = currentlyPortrait  ?  0 : 1;
        savePreferences();
        setRequestedOrientation(orientationMode == 1  ? 
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT :
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        rebuildView();
        Toast.makeText(this, orientationMode == 1  ?  "Portrait Mode" : "Landscape Mode", Toast.LENGTH_SHORT).show();
    }

    @Override public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE



                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    @Override protected void onStart() {
        super.onStart();
        handler.post(hudUpdater);
        updateActiveBannerTextStyle();
        // Notification listener removed to eliminate Play Protect financial fraud trigger
    }

    @Override protected void onStop() {
        handler.removeCallbacks(hudUpdater);
        if (heroBadgesBlinker != null) {
            handler.removeCallbacks(heroBadgesBlinker);
            heroBadgesBlinker = null;
        }
        super.onStop();
    }

    private void loadPreferences() {
        SharedPreferences sp = getSharedPreferences("ror_game_launcher", 0);
        cleanupEnabled = sp.getBoolean("cleanup_enabled", true);
        networkEnabled = sp.getBoolean("network_enabled", true);
        bubbleEnabled = sp.getBoolean("bubble_enabled", true);
        orientationMode = sp.getInt("orientation_mode", 0);
        themeIndex = sp.getInt("theme_index", 0);
        layoutIndex = sp.getInt("layout_index", 0);
        showBrightnessBar = sp.getBoolean("show_brightness_bar", false);
        launcherProfile = GsmProfileManager.selectedProfile(this);
        showMoreProfiles = sp.getBoolean("show_more_profiles", false);

        SharedPreferences th = getSharedPreferences("ror_game_space_themes", 0);
        gameSpaceAccent = th.getInt("game_space_accent", 0xFFFF1744);
        bannerBlur = th.getInt("banner_blur", 8);
        selectedBannerId = th.getInt("selected_banner_id", 0);
        activeBannerId = selectedBannerId;
        selectedBannerPath = th.getString("selected_banner_path", null);
        String customJson = th.getString("custom_banners_json", "[]");
        customBannersList.clear();
        try {
            org.json.JSONArray arr = new org.json.JSONArray(customJson);
            for (int i = 0; i < arr.length(); i++) customBannersList.add(arr.getString(i));
        } catch (Exception ignored) {}

        SharedPreferences pl = getSharedPreferences("ror_game_plugins", MODE_PRIVATE);
        crosshairColor = pl.getInt("crosshair_color", 0xFFFF1744);
        crosshairSize = pl.getInt("crosshair_size", 52);
    }

    private void savePreferences() {
        getSharedPreferences("ror_game_launcher", 0).edit()
                .putBoolean("cleanup_enabled", cleanupEnabled)
                .putBoolean("network_enabled", networkEnabled)
                .putBoolean("bubble_enabled", bubbleEnabled)
                .putInt("orientation_mode", orientationMode)
                .putInt("theme_index", themeIndex)
                .putInt("layout_index", layoutIndex)
                .putBoolean("show_brightness_bar", showBrightnessBar)
                .putBoolean("show_more_profiles", showMoreProfiles)
                .apply();

        selectedBannerId = activeBannerId;
        org.json.JSONArray arr = new org.json.JSONArray();
        for (String p : customBannersList) arr.put(p);
        getSharedPreferences("ror_game_space_themes", 0).edit()
                .putInt("game_space_accent", gameSpaceAccent)
                .putInt("banner_blur", bannerBlur)
                .putInt("selected_banner_id", activeBannerId)
                .putString("selected_banner_path", selectedBannerPath)
                .putString("custom_banners_json", arr.toString())
                .apply();
    }

    private View buildPs5HomeView() {
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        View pageContent;
        int activeLayout = Math.floorMod(layoutIndex, LAYOUT_NAMES.length);
        if (showSuperBase) {
            pageContent = buildSuperBasePage();
        } else {
            pageContent = buildActiveGameSpaceLayout(activeLayout, isPortrait);
        }

        FrameLayout container = new FrameLayout(this);
        animatedBannerView = new AnimatedGameSpaceBannerView(this);
        String customUri = (activeBannerId >= 100 && (activeBannerId - 100) < customBannersList.size())
                 ?  customBannersList.get(activeBannerId - 100) : null;
        animatedBannerView.setBanner(activeBannerId, customUri, bannerBlur);
        container.addView(animatedBannerView, new FrameLayout.LayoutParams(-1, -1));
        // Every selectable layout gets the same reserved ad slot. Previously only
        // the default and a few special layouts built their own banner.
        if (!showSuperBase && activeLayout != 0) {
            LinearLayout pageWithAd = new LinearLayout(this);
            pageWithAd.setOrientation(LinearLayout.VERTICAL);
            pageWithAd.setClipChildren(false);
            pageWithAd.setClipToPadding(false);
            pageWithAd.addView(pageContent, new LinearLayout.LayoutParams(-1, 0, 1f));
            adBannerView = buildAdBanner();
            DisplayMetrics dm = getResources().getDisplayMetrics();
            boolean compactHeight = dm.heightPixels / dm.density < 460f;
            LinearLayout.LayoutParams universalAdLp = new LinearLayout.LayoutParams(-1, dp(compactHeight ? 46 : 54));
            universalAdLp.setMargins(dp(8), dp(2), dp(8), dp(2));
            pageWithAd.addView(adBannerView, universalAdLp);
            pageContent = pageWithAd;
        }
        container.addView(pageContent, new FrameLayout.LayoutParams(-1, -1));

        boolean isRgbActive = getSharedPreferences("ror_game_plugins", MODE_PRIVATE).getBoolean("rainbow_rgb", true)
                && getSharedPreferences("ror_game_space_center", MODE_PRIVATE).getBoolean("ror_atmosphere_led", true);
        RgbBorderFrameView rgbFrame = new RgbBorderFrameView(this, isRgbActive, activeLayout == 0);
        container.addView(rgbFrame, new FrameLayout.LayoutParams(-1, -1));
        container.addView(new LayoutAccentFrameView(this, gameSpaceAccent), new FrameLayout.LayoutParams(-1, -1));
        return container;
    }

    private View buildActiveGameSpaceLayout(int activeLayout, boolean isPortrait) {
        switch (activeLayout) {
            case 0:
                return isPortrait  ?  buildRORPortraitHome() : buildRORLandscapeHome();

            case 1:  // REDMAGIC Mecha Deck Style (Photo 1 reference)
                return buildMechaDeckLayout(isPortrait);

            case 2:  // ASUS ROG Armoury Crate Style
                return buildArmouryCrateLayout(isPortrait);

            case 3:  // Lenovo Legion Space Style (Photo 2 reference)
            case 20: // Lenovo Legion Circular Layout
                return buildLegionSpaceLayout(activeLayout, isPortrait);

            case 4:  // Black Shark Space Style (Photo 3 reference)
            case 21: // Black Shark Lobby Layout
                return buildBlackSharkLobbyLayout(activeLayout, isPortrait);

            case 5:  // Xiaomi Game Turbo Style (Photo 4 reference)
                return buildGameTurboCockpitLayout(isPortrait);

            case 8:  // Samsung Gaming Hub Style
            case 14: // EmulationStation Style
            case 15: // LaunchBox Style
            case 22: // Pegasus BigScreenFE Style
                return buildConsoleStageLayout(activeLayout, isPortrait);

            case 10: // REDMAGIC Dual Grid
            case 23: // Pegasus Flat-Ozone Style
            case 29: // ROR Hero Grid
            case 30: // ROR Split Grid
            case 31: // ROR Category Grid
                return buildSplitAndHeroLayout(activeLayout, isPortrait);

            case 11: // REDMAGIC Tile Grid (4 cols)
            case 12: // Beacon Launcher Style (4 cols compact)
            case 13: // Daijisho Style (3 cols poster with tabs)
            case 16: // OPPO Compact Grid (5 cols)
            case 17: // Samsung Three-Column Grid (3 cols)
            case 18: // OnePlus Hybrid Grid (3 cols hybrid)
            case 19: // Beacon App Grid (6 cols dense)
            case 24: // ROR Dual Cover Grid (2 cols large)
            case 25: // ROR Triple Cover Grid (3 cols)
            case 26: // ROR Quad Grid (4 cols)
            case 27: // ROR Ultra-Dense Grid (5-6 cols)
            case 28: // ROR Masonry Grid (staggered cards)
            case 32: // ROR Poster Grid (tall 2:3 box-art poster cards)
                return buildGridCatalogLayout(activeLayout, isPortrait);

            case 33: // ROR Hexagon Grid
                return buildHexagonGridLayout(isPortrait);

            case 6:  // OPPO Game Space Style
            case 7:  // OnePlus Games Style
            case 9:  // Realme Game Space Style
            default:
                return buildMinimalStreamLayout(activeLayout, isPortrait);
        }
    }

    private List<GameEntry> getActiveGameEntries() {
        List<GameEntry> list = new ArrayList<>();
        for (GameEntry e : allEntries) {
            if (e.game || showAllApps) list.add(e);
        }
        if (list.isEmpty() && !allEntries.isEmpty()) {
            list.addAll(allEntries);
        }
        if (list.isEmpty() && !displayedEntries.isEmpty()) {
            list.addAll(displayedEntries);
        }
        return list;
    }

    private GameEntry getActiveSelectedEntry() {
        List<GameEntry> list = getActiveGameEntries();
        if (selectedEntry != null && list.contains(selectedEntry)) {
            return selectedEntry;
        }
        if (!list.isEmpty()) {
            selectedEntry = list.get(0);
            return selectedEntry;
        }
        return null;
    }

    private Drawable getEntryIcon(GameEntry entry) {
        if (entry == null) return ContextCompat.getDrawable(this, R.mipmap.ic_launcher);
        PackageManager pm = getPackageManager();
        try {
            if (entry.resolve != null) {
                Drawable icon = entry.resolve.loadIcon(pm);
                if (icon != null) return icon;
            }
        } catch (Exception ignored) {}
        try {
            Drawable icon = pm.getApplicationIcon(entry.pkg);
            if (icon != null) return icon;
        } catch (Exception ignored) {}
        return ContextCompat.getDrawable(this, R.mipmap.ic_launcher);
    }

    private View buildConsoleStageLayout(int activeLayout, boolean isPortrait) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(6), dp(14), dp(6));
        root.setBackgroundColor(0x00000000);

        topBarView = buildRORHeader();
        root.addView(topBarView, new LinearLayout.LayoutParams(-1, -2));

        List<GameEntry> games = getActiveGameEntries();
        GameEntry focused = getActiveSelectedEntry();

        LinearLayout stage = new LinearLayout(this);
        stage.setOrientation(isPortrait  ?  LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        stage.setGravity(Gravity.CENTER);
        stage.setPadding(dp(20), dp(16), dp(20), dp(16));

        int themeColor = 0xFFFF2A42;
        int themeBg1 = 0xEE1E141E;
        int themeBg2 = 0xEE0D0B12;
        String kicker = "CONSOLE HERO";
        if (activeLayout == 5) {
            themeColor = 0xFFFFD600; themeBg1 = 0xEE2A2210; themeBg2 = 0xEE141008; kicker = "GAME TURBO HUD";
        } else if (activeLayout == 8) {
            themeColor = 0xFF7C4DFF; themeBg1 = 0xEE1E1634; themeBg2 = 0xEE0D0A1C; kicker = "GAMING HUB";
        } else if (activeLayout == 14) {
            themeColor = 0xFF00E5FF; themeBg1 = 0xEE10242A; themeBg2 = 0xEE091418; kicker = "EMULATION STATION";
        } else if (activeLayout == 15) {
            themeColor = 0xFFFF6D00; themeBg1 = 0xEE26180C; themeBg2 = 0xEE120A05; kicker = "LAUNCHBOX FE";
        } else if (activeLayout == 21) {
            themeColor = 0xFF00E676; themeBg1 = 0xEE0D2418; themeBg2 = 0xEE06120B; kicker = "SHARK LOBBY";
        } else if (activeLayout == 22) {
            themeColor = 0xFFFFAB00; themeBg1 = 0xEE2A1E0D; themeBg2 = 0xEE140E06; kicker = "BIGSCREEN FE";
        }

        GradientDrawable stageBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{themeBg1, themeBg2});
        stageBg.setCornerRadius(dp(18));
        stageBg.setStroke(dp(1.8f), themeColor);
        stage.setBackground(stageBg);

        FrameLayout iconFrame = new FrameLayout(this);
        GradientDrawable ifBg = new GradientDrawable();
        ifBg.setColor(0xCC101018);
        ifBg.setCornerRadius(dp(16));
        ifBg.setStroke(dp(1.5f), themeColor);
        iconFrame.setBackground(ifBg);
        ImageView bigIcon = new ImageView(this);
        bigIcon.setImageDrawable(getEntryIcon(focused));
        int iconSize = dp(isPortrait  ?  56 : 72);
        iconFrame.addView(bigIcon, new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER));
        stage.addView(iconFrame, new LinearLayout.LayoutParams(dp(isPortrait  ?  72 : 88), dp(isPortrait  ?  72 : 88)));

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setPadding(dp(isPortrait  ?  0 : 18), dp(isPortrait  ?  10 : 0), 0, 0);
        details.setGravity(isPortrait  ?  Gravity.CENTER_HORIZONTAL : Gravity.CENTER_VERTICAL);

        TextView kickerLbl = label(kicker + "  \u2022  120 FPS TARGET", 9f, themeColor);
        kickerLbl.setTypeface(null, Typeface.BOLD);
        kickerLbl.setLetterSpacing(0.12f);
        details.addView(kickerLbl);

        TextView titleLbl = label(focused != null  ?  focused.label : "NO GAMES SELECTED", isPortrait  ?  15f : 19f, Color.WHITE);
        titleLbl.setTypeface(null, Typeface.BOLD);
        details.addView(titleLbl);

        TextView pkgLbl = label(focused != null  ?  focused.pkg : "Tap below to add games to your library", 9f, 0xFF9E9EA8);
        details.addView(pkgLbl);

        TextView playBtn = label("  PLAY NOW  ", 12f, Color.WHITE);
        playBtn.setTypeface(null, Typeface.BOLD);
        playBtn.setPadding(dp(22), dp(9), dp(22), dp(9));
        GradientDrawable pBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{themeColor, 0xFFE00028});
        pBg.setCornerRadius(dp(20));
        pBg.setStroke(dp(1.2f), 0xFFFFFFFF);
        playBtn.setBackground(pBg);
        playBtn.setOnClickListener(v -> {
            try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
            if (focused != null) launch(focused);
            else showAddGamesDialog();
        });
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(-2, -2);
        pbLp.topMargin = dp(8);
        details.addView(playBtn, pbLp);

        stage.addView(details, new LinearLayout.LayoutParams(isPortrait  ?  -1 : 0, -2, isPortrait  ?  0 : 1f));
        root.addView(attachResponsivePanels(stage, activeLayout, isPortrait), new LinearLayout.LayoutParams(-1, 0, 1f));

        HorizontalScrollView hScroll = new HorizontalScrollView(this);
        hScroll.setHorizontalScrollBarEnabled(false);
        hScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout carousel = new LinearLayout(this);
        carousel.setOrientation(LinearLayout.HORIZONTAL);
        carousel.setPadding(0, dp(10), 0, dp(6));
        carousel.setGravity(Gravity.CENTER_VERTICAL);

        for (int i = 0; i < games.size(); i++) {
            final GameEntry entry = games.get(i);
            boolean isSel = (focused != null && focused.pkg.equals(entry.pkg));

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(10), dp(6), dp(12), dp(6));

            GradientDrawable cBg = new GradientDrawable();
            cBg.setColor(isSel  ?  0xDD281E2E : 0xAA141620);
            cBg.setCornerRadius(dp(12));
            cBg.setStroke(dp(isSel  ?  1.8f : 1f), isSel  ?  themeColor : 0xFF2A2D3C);
            card.setBackground(cBg);

            ImageView cIcon = new ImageView(this);
            cIcon.setImageDrawable(getEntryIcon(entry));
            card.addView(cIcon, new LinearLayout.LayoutParams(dp(36), dp(36)));

            LinearLayout cText = new LinearLayout(this);
            cText.setOrientation(LinearLayout.VERTICAL);
            cText.setPadding(dp(8), 0, 0, 0);

            TextView cName = label(entry.label, 10.5f, isSel  ?  Color.WHITE : 0xFFCCCCCC);
            cName.setTypeface(null, Typeface.BOLD);
            cName.setSingleLine(true);
            cName.setEllipsize(TextUtils.TruncateAt.END);
            cText.addView(cName);

            TextView cTag = label("PRO GAMING", 7.5f, isSel  ?  themeColor : 0xFF888894);
            cText.addView(cTag);
            card.addView(cText, new LinearLayout.LayoutParams(dp(100), -2));

            card.setOnClickListener(v -> {
                try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
                selectedEntry = entry;
                rebuildView();
            });

            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(-2, dp(58));
            cLp.setMarginEnd(dp(8));
            carousel.addView(card, cLp);
        }

        LinearLayout addCard = new LinearLayout(this);
        addCard.setOrientation(LinearLayout.HORIZONTAL);
        addCard.setGravity(Gravity.CENTER);
        addCard.setPadding(dp(14), dp(6), dp(14), dp(6));
        GradientDrawable aBg = new GradientDrawable();
        aBg.setColor(0x881E2232);
        aBg.setCornerRadius(dp(12));
        aBg.setStroke(dp(1f), 0xFF3E4868);
        addCard.setBackground(aBg);
        TextView addText = label("+ ADD GAME", 10f, 0xFF00E5FF);
        addText.setTypeface(null, Typeface.BOLD);
        addCard.addView(addText);
        addCard.setOnClickListener(v -> showAddGamesDialog());
        carousel.addView(addCard, new LinearLayout.LayoutParams(-2, dp(58)));

        hScroll.addView(carousel, new FrameLayout.LayoutParams(-2, -2));
        root.addView(hScroll, new LinearLayout.LayoutParams(-1, -2));

        root.addView(buildRORBottomNav(), new LinearLayout.LayoutParams(-1, dp(38)));
        return root;
    }

    // Layout 1: REDMAGIC Mecha Deck (Photo 1 Reference)
    private View buildMechaDeckLayout(boolean isPortrait) {
        FrameLayout rootFrame = new FrameLayout(this);
        CyberConsoleBackgroundView bgView = new CyberConsoleBackgroundView(this, CyberConsoleBackgroundView.MODE_MECHA_DECK);
        rootFrame.addView(bgView, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(6), dp(14), dp(6));

        // Floating Mecha Bracket Header (Photo 1)
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(4), dp(2), dp(4), dp(4));

        View back = createCyberGlassBackButton(v -> finish());
        header.addView(back);

        // Mecha Tabs Bracket: LIBRARY (Active) | Console | Disponible
        LinearLayout tabsBracket = new LinearLayout(this);
        tabsBracket.setOrientation(LinearLayout.HORIZONTAL);
        tabsBracket.setGravity(Gravity.CENTER_VERTICAL);
        tabsBracket.setPadding(dp(6), dp(3), dp(6), dp(3));
        GradientDrawable tbBg = new GradientDrawable();
        tbBg.setColor(0xCC0C1524);
        tbBg.setCornerRadius(dp(10));
        tbBg.setStroke(dp(1.2f), 0xFF00E5FF);
        tabsBracket.setBackground(tbBg);
        LinearLayout.LayoutParams tbLp = new LinearLayout.LayoutParams(-2, -2);
        tbLp.setMarginStart(dp(8));
        tabsBracket.setLayoutParams(tbLp);

        TextView tabBiblio = label("LIBRARY", 10.5f, 0xFF00E5FF);
        tabBiblio.setTypeface(null, Typeface.BOLD);
        tabBiblio.setPadding(dp(10), dp(4), dp(10), dp(4));
        GradientDrawable bibBg = new GradientDrawable();
        bibBg.setColor(0x4400E5FF);
        bibBg.setCornerRadius(dp(6));
        tabBiblio.setBackground(bibBg);
        tabsBracket.addView(tabBiblio);

        TextView tabConsole = label("CONSOLE", 10.5f, Color.WHITE);
        tabConsole.setTypeface(null, Typeface.BOLD);
        tabConsole.setPadding(dp(10), dp(4), dp(10), dp(4));
        tabConsole.setOnClickListener(v -> showGamingTelemetryDialog());
        tabsBracket.addView(tabConsole);

        TextView tabDispo = label("AVAILABLE", 10.5f, 0xFF88A4C8);
        tabDispo.setTypeface(null, Typeface.BOLD);
        tabDispo.setPadding(dp(10), dp(4), dp(10), dp(4));
        tabDispo.setOnClickListener(v -> showAddGamesDialog());
        tabsBracket.addView(tabDispo);

        header.addView(tabsBracket);

        View hSpacer = new View(this);
        header.addView(hSpacer, new LinearLayout.LayoutParams(0, 1, 1f));

        header.addView(createUniversalTelemetryBar(0xFF00E5FF));

        // Right Status Bracket: Games Count | Settings
        List<GameEntry> games = getActiveGameEntries();
        TextView countPill = label(games.size() + " GAMES", 9f, 0xFF00E5FF);
        countPill.setTypeface(null, Typeface.BOLD);
        countPill.setPadding(dp(8), dp(4), dp(8), dp(4));
        GradientDrawable cpBg = new GradientDrawable();
        cpBg.setColor(0xCC09182B);
        cpBg.setCornerRadius(dp(8));
        cpBg.setStroke(dp(1), 0xFF00B0FF);
        countPill.setBackground(cpBg);
        LinearLayout.LayoutParams cpLp = new LinearLayout.LayoutParams(-2, -2);
        cpLp.setMarginStart(dp(6));
        header.addView(countPill, cpLp);

        ImageView settingsBtn = new ImageView(this);
        settingsBtn.setImageResource(R.drawable.ic_settings_icon);
        settingsBtn.setColorFilter(0xFF00E5FF);
        settingsBtn.setPadding(dp(6), dp(6), dp(6), dp(6));
        GradientDrawable stBg = new GradientDrawable();
        stBg.setColor(0xCC09182B);
        stBg.setCornerRadius(dp(8));
        stBg.setStroke(dp(1), 0xFF00B0FF);
        settingsBtn.setBackground(stBg);
        settingsBtn.setOnClickListener(v -> showGameSpaceCenterDialog());
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(dp(30), dp(30));
        stLp.setMarginStart(dp(6));
        header.addView(createQuickLayoutHeaderButton(0xFF00E5FF));
        header.addView(settingsBtn, stLp);

        // Mecha Brightness Toggle Button
        ImageView mechaBriBtn = new ImageView(this);
        mechaBriBtn.setImageResource(R.drawable.ic_sun);
        mechaBriBtn.setColorFilter(0xFF00E5FF);
        mechaBriBtn.setPadding(dp(6), dp(6), dp(6), dp(6));
        GradientDrawable mechaBriBg = new GradientDrawable();
        mechaBriBg.setColor(0xCC09182B);
        mechaBriBg.setCornerRadius(dp(8));
        mechaBriBg.setStroke(dp(1), 0xFF00B0FF);
        mechaBriBtn.setBackground(mechaBriBg);
        mechaBriBtn.setOnClickListener(v -> toggleBrightnessBar());
        LinearLayout.LayoutParams mechaBriLp = new LinearLayout.LayoutParams(dp(30), dp(30));
        mechaBriLp.setMarginStart(dp(6));
        header.addView(mechaBriBtn, mechaBriLp);

        topBarView = header;
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        View mechaBriBar = buildGameSpaceBrightnessBar(0xFF00E5FF);
        if (mechaBriBar != null) root.addView(mechaBriBar, new LinearLayout.LayoutParams(-1, -2));

        GameEntry focused = getActiveSelectedEntry();
        int curIdx = (focused != null && !games.isEmpty()) ? games.indexOf(focused) : 0;
        if (curIdx < 0) curIdx = 0;
        int n = games.size();

        GameEntry prevEntry = (n > 1) ? games.get((curIdx - 1 + n) % n) : null;
        GameEntry nextEntry = (n > 1) ? games.get((curIdx + 1) % n) : null;

        // Center 3D Perspective Stage
        LinearLayout stage = new LinearLayout(this);
        stage.setOrientation(LinearLayout.HORIZONTAL);
        stage.setGravity(Gravity.CENTER);
        stage.setPadding(0, dp(isPortrait ? 8 : 4), 0, dp(isPortrait ? 8 : 4));

        // Left Angled Preview Card
        if (prevEntry != null) {
            LinearLayout leftCard = new LinearLayout(this);
            leftCard.setOrientation(LinearLayout.VERTICAL);
            leftCard.setGravity(Gravity.CENTER);
            leftCard.setPadding(dp(8), dp(8), dp(8), dp(8));
            GradientDrawable lBg = new GradientDrawable();
            lBg.setColor(0xDD0A1524);
            lBg.setCornerRadius(dp(14));
            lBg.setStroke(dp(1.2f), 0x5500E5FF);
            leftCard.setBackground(lBg);
            leftCard.setAlpha(0.65f);

            ImageView lIcon = new ImageView(this);
            lIcon.setImageDrawable(getEntryIcon(prevEntry));
            leftCard.addView(lIcon, new LinearLayout.LayoutParams(dp(44), dp(44)));

            TextView lName = label(prevEntry.label, 9.5f, 0xFFCCCCCC);
            lName.setSingleLine(true);
            lName.setEllipsize(TextUtils.TruncateAt.END);
            lName.setPadding(0, dp(4), 0, 0);
            leftCard.addView(lName);

            leftCard.setOnClickListener(v -> {
                selectedEntry = prevEntry;
                rebuildView();
            });
            int previewW = dp(isPortrait ? 80 : 105);
            int previewH = dp(isPortrait ? 120 : 140);
            LinearLayout.LayoutParams lLp = new LinearLayout.LayoutParams(previewW, previewH);
            lLp.setMarginEnd(dp(10));
            stage.addView(leftCard, lLp);
        }

        // Center Glowing Landscape Cyber Card (Matching Photo 1)
        LinearLayout centerCard = new LinearLayout(this);
        centerCard.setOrientation(LinearLayout.VERTICAL);
        centerCard.setGravity(Gravity.CENTER);
        centerCard.setPadding(dp(18), dp(12), dp(18), dp(12));

        GradientDrawable cBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xF2122134, 0xF609121E, 0xF218283E});
        cBg.setCornerRadius(dp(18));
        cBg.setStroke(dp(2.2f), 0xFF00E5FF);
        centerCard.setBackground(cBg);

        // Top Cyber Badge
        TextView cardBadge = label("TITAN CYBER DECK", 8f, 0xFF00E5FF);
        cardBadge.setTypeface(null, Typeface.BOLD);
        cardBadge.setLetterSpacing(0.12f);
        centerCard.addView(cardBadge);

        // Game Icon in glowing frame
        FrameLayout iconFrame = new FrameLayout(this);
        GradientDrawable ifBg = new GradientDrawable();
        ifBg.setColor(0xEE0B1728);
        ifBg.setCornerRadius(dp(16));
        ifBg.setStroke(dp(1.5f), 0xFF00E5FF);
        iconFrame.setBackground(ifBg);
        ImageView bigIcon = new ImageView(this);
        bigIcon.setImageDrawable(getEntryIcon(focused));
        int iconSize = dp(isPortrait ? 56 : 68);
        iconFrame.addView(bigIcon, new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER));
        int frameSize = dp(isPortrait ? 72 : 84);
        LinearLayout.LayoutParams ifLp = new LinearLayout.LayoutParams(frameSize, frameSize);
        ifLp.topMargin = dp(4);
        centerCard.addView(iconFrame, ifLp);

        // Game Title
        TextView title = label(focused != null ? focused.label : "NO GAMES SELECTED", isPortrait ? 15f : 17f, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setPadding(0, dp(8), 0, dp(2));
        centerCard.addView(title);

        // Played Duration & Turbo Status
        TextView duration = label("PLAY TIME: 14h 20m  \u2022  TURBO FAN: ACTIVE", 8.5f, 0xFFFFD600);
        duration.setTypeface(null, Typeface.BOLD);
        centerCard.addView(duration);

        centerCard.setOnClickListener(v -> {
            if (focused != null) launch(focused);
            else showAddGamesDialog();
        });

        int centerW = dp(isPortrait ? 200 : 290);
        int centerH = dp(isPortrait ? 180 : 190);
        stage.addView(centerCard, new LinearLayout.LayoutParams(centerW, centerH));

        // Right Angled Preview Card
        if (nextEntry != null) {
            LinearLayout rightCard = new LinearLayout(this);
            rightCard.setOrientation(LinearLayout.VERTICAL);
            rightCard.setGravity(Gravity.CENTER);
            rightCard.setPadding(dp(8), dp(8), dp(8), dp(8));
            GradientDrawable rBg = new GradientDrawable();
            rBg.setColor(0xDD0A1524);
            rBg.setCornerRadius(dp(14));
            rBg.setStroke(dp(1.2f), 0x5500E5FF);
            rightCard.setBackground(rBg);
            rightCard.setAlpha(0.65f);

            ImageView rIcon = new ImageView(this);
            rIcon.setImageDrawable(getEntryIcon(nextEntry));
            rightCard.addView(rIcon, new LinearLayout.LayoutParams(dp(44), dp(44)));

            TextView rName = label(nextEntry.label, 9.5f, 0xFFCCCCCC);
            rName.setSingleLine(true);
            rName.setEllipsize(TextUtils.TruncateAt.END);
            rName.setPadding(0, dp(4), 0, 0);
            rightCard.addView(rName);

            rightCard.setOnClickListener(v -> {
                selectedEntry = nextEntry;
                rebuildView();
            });
            int previewW = dp(isPortrait ? 80 : 105);
            int previewH = dp(isPortrait ? 120 : 140);
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(previewW, previewH);
            rLp.setMarginStart(dp(10));
            stage.addView(rightCard, rLp);
        }

        root.addView(attachResponsivePanels(stage, 1, isPortrait), new LinearLayout.LayoutParams(-1, 0, 1f));

        // Bottom 3 Cyber Chamfered Buttons (Photo 1)
        LinearLayout bottomButtonsRow = new LinearLayout(this);
        bottomButtonsRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomButtonsRow.setGravity(Gravity.CENTER_VERTICAL);
        bottomButtonsRow.setPadding(dp(4), dp(4), dp(4), dp(4));

        // Left Chamfered Button: Official Site / Gaming Hub
        TextView leftBtn = label("OFFICIAL SITE", 10.5f, 0xFF00E5FF);
        leftBtn.setTypeface(null, Typeface.BOLD);
        leftBtn.setGravity(Gravity.CENTER);
        leftBtn.setPadding(dp(10), dp(10), dp(10), dp(10));
        GradientDrawable lbBg = new GradientDrawable();
        lbBg.setColor(0xEE091C2E);
        lbBg.setCornerRadius(dp(8));
        lbBg.setStroke(dp(1.5f), 0xFF00B0FF);
        leftBtn.setBackground(lbBg);
        leftBtn.setOnClickListener(v -> showGamingTelemetryDialog());
        bottomButtonsRow.addView(leftBtn, new LinearLayout.LayoutParams(0, -2, 1f));

        // Center Glowing Red Chamfer Button with LED Light Bar (Photo 1)
        FrameLayout centerBtnFrame = new FrameLayout(this);
        GradientDrawable cbBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFFFF1744, 0xFFD50000});
        cbBg.setCornerRadius(dp(10));
        cbBg.setStroke(dp(1.5f), 0xFFFF8A80);
        centerBtnFrame.setBackground(cbBg);

        LinearLayout centerContent = new LinearLayout(this);
        centerContent.setOrientation(LinearLayout.VERTICAL);
        centerContent.setGravity(Gravity.CENTER);
        centerContent.setPadding(dp(14), dp(6), dp(14), dp(6));

        // LED light bar line
        View ledBar = new View(this);
        GradientDrawable ledBg = new GradientDrawable();
        ledBg.setColor(0xFF80D8FF);
        ledBg.setCornerRadius(dp(2));
        ledBar.setBackground(ledBg);
        centerContent.addView(ledBar, new LinearLayout.LayoutParams(dp(48), dp(3)));

        TextView startText = label("PERFORMANCE PROFILES", 11.5f, Color.WHITE);
        startText.setTypeface(null, Typeface.BOLD);
        startText.setPadding(0, dp(2), 0, 0);
        centerContent.addView(startText);

        centerBtnFrame.addView(centerContent, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        centerBtnFrame.setOnClickListener(v -> {
            if (focused != null) launch(focused);
            else showAddGamesDialog();
        });

        LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(0, -2, 1.4f);
        cbLp.setMarginStart(dp(8));
        cbLp.setMarginEnd(dp(8));
        bottomButtonsRow.addView(centerBtnFrame, cbLp);

        // Right Chamfered Button: Recordings / Settings
        TextView rightBtn = label("RECORDINGS", 10.5f, 0xFF00E5FF);
        rightBtn.setTypeface(null, Typeface.BOLD);
        rightBtn.setGravity(Gravity.CENTER);
        rightBtn.setPadding(dp(10), dp(10), dp(10), dp(10));
        GradientDrawable rbBg = new GradientDrawable();
        rbBg.setColor(0xEE091C2E);
        rbBg.setCornerRadius(dp(8));
        rbBg.setStroke(dp(1.5f), 0xFF00B0FF);
        rightBtn.setBackground(rbBg);
        rightBtn.setOnClickListener(v -> showConsoleSettingsDialog());
        bottomButtonsRow.addView(rightBtn, new LinearLayout.LayoutParams(0, -2, 1f));

        root.addView(bottomButtonsRow, new LinearLayout.LayoutParams(-1, -2));
        rootFrame.addView(root, new FrameLayout.LayoutParams(-1, -1));
        return rootFrame;
    }

    // Layout 2: ASUS ROG Armoury Crate Style
    private View buildArmouryCrateLayout(boolean isPortrait) {
        FrameLayout rootFrame = new FrameLayout(this);
        CyberConsoleBackgroundView bgView = new CyberConsoleBackgroundView(this, CyberConsoleBackgroundView.MODE_ROG_ARMOURY);
        rootFrame.addView(bgView, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(6), dp(14), dp(6));

        topBarView = buildRORHeader();
        root.addView(topBarView, new LinearLayout.LayoutParams(-1, -2));

        List<GameEntry> games = getActiveGameEntries();
        GameEntry focused = getActiveSelectedEntry();

        // Armoury Matrix Header Tabs: OVERDRIVE, DYNAMIC, ULTRA DURABLE
        LinearLayout modesRow = new LinearLayout(this);
        modesRow.setOrientation(LinearLayout.HORIZONTAL);
        modesRow.setGravity(Gravity.CENTER_VERTICAL);
        modesRow.setPadding(dp(8), dp(4), dp(8), dp(4));

        TextView rogLogo = label("ARMOURY MATRIX", 11.5f, 0xFFFF1122);
        rogLogo.setTypeface(null, Typeface.BOLD);
        modesRow.addView(rogLogo);

        View spacer = new View(this);
        modesRow.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        TextView xMode = label("OVERDRIVE", 9f, Color.WHITE);
        xMode.setTypeface(null, Typeface.BOLD);
        xMode.setPadding(dp(10), dp(4), dp(10), dp(4));
        GradientDrawable xmBg = new GradientDrawable();
        xmBg.setColor(0xFFFF1122);
        xmBg.setCornerRadius(dp(6));
        xMode.setBackground(xmBg);
        modesRow.addView(xMode);

        TextView dynMode = label("DYNAMIC", 9f, 0xFF9EABB8);
        dynMode.setTypeface(null, Typeface.BOLD);
        dynMode.setPadding(dp(8), dp(4), dp(8), dp(4));
        modesRow.addView(dynMode);

        root.addView(modesRow, new LinearLayout.LayoutParams(-1, -2));

        // Center Armor Card
        LinearLayout stage = new LinearLayout(this);
        stage.setOrientation(LinearLayout.VERTICAL);
        stage.setGravity(Gravity.CENTER);
        stage.setPadding(dp(16), dp(10), dp(16), dp(10));

        LinearLayout armorCard = new LinearLayout(this);
        armorCard.setOrientation(LinearLayout.HORIZONTAL);
        armorCard.setGravity(Gravity.CENTER_VERTICAL);
        armorCard.setPadding(dp(18), dp(14), dp(18), dp(14));

        GradientDrawable acBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xEE2A0A10, 0xEE120508});
        acBg.setCornerRadius(dp(16));
        acBg.setStroke(dp(1.8f), 0xFFFF1122);
        armorCard.setBackground(acBg);

        ImageView bigIcon = new ImageView(this);
        bigIcon.setImageDrawable(getEntryIcon(focused));
        armorCard.addView(bigIcon, new LinearLayout.LayoutParams(dp(isPortrait ? 56 : 68), dp(isPortrait ? 56 : 68)));

        LinearLayout aDetails = new LinearLayout(this);
        aDetails.setOrientation(LinearLayout.VERTICAL);
        aDetails.setPadding(dp(14), 0, 0, 0);

        TextView aTitle = label(focused != null ? focused.label : "NO GAMES SELECTED", isPortrait ? 15f : 18f, Color.WHITE);
        aTitle.setTypeface(null, Typeface.BOLD);
        aTitle.setSingleLine(true);
        aTitle.setEllipsize(TextUtils.TruncateAt.END);
        aDetails.addView(aTitle);

        TextView aSub = label("ROG THERMAL TUNING  \u2022  165Hz MATRIX", 8.5f, 0xFFFF5252);
        aDetails.addView(aSub);

        TextView startBtn = label("GAME START", 11f, Color.WHITE);
        startBtn.setTypeface(null, Typeface.BOLD);
        startBtn.setPadding(dp(18), dp(6), dp(18), dp(6));
        GradientDrawable sbBg = new GradientDrawable();
        sbBg.setColor(0xFFFF1122);
        sbBg.setCornerRadius(dp(10));
        startBtn.setBackground(sbBg);
        startBtn.setOnClickListener(v -> {
            if (focused != null) launch(focused);
            else showAddGamesDialog();
        });
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(-2, -2);
        sbLp.topMargin = dp(8);
        aDetails.addView(startBtn, sbLp);

        armorCard.addView(aDetails, new LinearLayout.LayoutParams(0, -2, 1f));

        int cardW = dp(isPortrait ? 270 : 380);
        stage.addView(armorCard, new LinearLayout.LayoutParams(cardW, -2));
        root.addView(attachResponsivePanels(stage, 2, isPortrait), new LinearLayout.LayoutParams(-1, 0, 1f));

        // Horizontal game rack
        HorizontalScrollView hScroll = new HorizontalScrollView(this);
        hScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout rack = new LinearLayout(this);
        rack.setOrientation(LinearLayout.HORIZONTAL);
        rack.setPadding(0, dp(4), 0, dp(4));

        for (int i = 0; i < games.size(); i++) {
            final GameEntry e = games.get(i);
            boolean isSel = (focused != null && focused.pkg.equals(e.pkg));
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(8), dp(6), dp(10), dp(6));
            GradientDrawable cBg = new GradientDrawable();
            cBg.setColor(isSel ? 0xDD2A0A10 : 0xAA14080B);
            cBg.setCornerRadius(dp(10));
            cBg.setStroke(dp(isSel ? 1.5f : 0.8f), isSel ? 0xFFFF1122 : 0x55FF1122);
            card.setBackground(cBg);

            ImageView ic = new ImageView(this);
            ic.setImageDrawable(getEntryIcon(e));
            card.addView(ic, new LinearLayout.LayoutParams(dp(30), dp(30)));

            TextView nm = label(e.label, 9.5f, isSel ? Color.WHITE : 0xFFCCCCCC);
            nm.setSingleLine(true);
            nm.setEllipsize(TextUtils.TruncateAt.END);
            nm.setPadding(dp(6), 0, 0, 0);
            card.addView(nm, new LinearLayout.LayoutParams(dp(80), -2));

            card.setOnClickListener(v -> {
                selectedEntry = e;
                rebuildView();
            });
            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(-2, dp(44));
            cLp.setMarginEnd(dp(8));
            rack.addView(card, cLp);
        }

        hScroll.addView(rack);
        root.addView(hScroll, new LinearLayout.LayoutParams(-1, -2));
        root.addView(buildRORBottomNav(), new LinearLayout.LayoutParams(-1, dp(38)));
        rootFrame.addView(root, new FrameLayout.LayoutParams(-1, -1));
        return rootFrame;
    }

    // Layout 3: Lenovo Legion Space (Photo 2 Reference)
    private View buildLegionSpaceLayout(int activeLayout, boolean isPortrait) {
        FrameLayout rootFrame = new FrameLayout(this);
        CyberConsoleBackgroundView bgView = new CyberConsoleBackgroundView(this, CyberConsoleBackgroundView.MODE_LEGION_SPACE);
        rootFrame.addView(bgView, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(6), dp(14), dp(6));

        // Top Bar: Legion Space Header Tabs
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(4), dp(2), dp(4), dp(4));

        View back = createCyberGlassBackButton(v -> finish());
        header.addView(back);

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.HORIZONTAL);
        brand.setPadding(dp(8), 0, dp(12), 0);
        TextView legionLogo = label("VORTEX ", 13f, 0xFF00E5FF);
        legionLogo.setTypeface(null, Typeface.BOLD);
        brand.addView(legionLogo);
        TextView spaceLogo = label("REACTOR", 13f, Color.WHITE);
        spaceLogo.setTypeface(null, Typeface.BOLD);
        brand.addView(spaceLogo);
        header.addView(brand);

        // Tabs Bracket: LIBRARY (Active) | PERFORMANCE | PROFILES
        LinearLayout tabsBracket = new LinearLayout(this);
        tabsBracket.setOrientation(LinearLayout.HORIZONTAL);
        tabsBracket.setGravity(Gravity.CENTER_VERTICAL);

        TextView tabGames = label("LIBRARY", 10f, Color.BLACK);
        tabGames.setTypeface(null, Typeface.BOLD);
        tabGames.setPadding(dp(12), dp(4), dp(12), dp(4));
        GradientDrawable tgBg = new GradientDrawable();
        tgBg.setColor(0xFF00E5FF);
        tgBg.setCornerRadius(dp(12));
        tabGames.setBackground(tgBg);
        tabsBracket.addView(tabGames);

        TextView tabPerf = label("PERFORMANCE", 10f, 0xFF9AB0C8);
        tabPerf.setTypeface(null, Typeface.BOLD);
        tabPerf.setPadding(dp(12), dp(4), dp(12), dp(4));
        tabPerf.setOnClickListener(v -> showGamingTelemetryDialog());
        tabsBracket.addView(tabPerf);

        TextView tabProfiles = label("PROFILES", 10f, 0xFF9AB0C8);
        tabProfiles.setTypeface(null, Typeface.BOLD);
        tabProfiles.setPadding(dp(12), dp(4), dp(12), dp(4));
        tabProfiles.setOnClickListener(v -> showGamingTelemetryDialog());
        tabsBracket.addView(tabProfiles);

        header.addView(tabsBracket);

        View hSpacer = new View(this);
        header.addView(hSpacer, new LinearLayout.LayoutParams(0, 1, 1f));

        header.addView(createUniversalTelemetryBar(0xFF00E5FF));

        header.addView(createQuickLayoutHeaderButton(0xFF00E5FF));

        ImageView settingsBtn = new ImageView(this);
        settingsBtn.setImageResource(R.drawable.ic_settings_icon);
        settingsBtn.setColorFilter(0xFF00E5FF);
        settingsBtn.setPadding(dp(6), dp(6), dp(6), dp(6));
        GradientDrawable stBg = new GradientDrawable();
        stBg.setColor(0xCC09182B);
        stBg.setCornerRadius(dp(8));
        stBg.setStroke(dp(1), 0xFF00B0FF);
        settingsBtn.setBackground(stBg);
        settingsBtn.setOnClickListener(v -> showGameSpaceCenterDialog());
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(dp(30), dp(30));
        stLp.setMarginStart(dp(6));
        header.addView(settingsBtn, stLp);

                ImageView legBriBtn = new ImageView(this);
        legBriBtn.setImageResource(R.drawable.ic_sun);
        legBriBtn.setColorFilter(0xFF00E5FF);
        legBriBtn.setPadding(dp(6), dp(6), dp(6), dp(6));
        GradientDrawable legBriBg = new GradientDrawable();
        legBriBg.setColor(0xCC061828);
        legBriBg.setCornerRadius(dp(8));
        legBriBg.setStroke(dp(1), 0xFF00E5FF);
        legBriBtn.setBackground(legBriBg);
        legBriBtn.setOnClickListener(v -> toggleBrightnessBar());
        LinearLayout.LayoutParams legBriLp = new LinearLayout.LayoutParams(dp(30), dp(30));
        legBriLp.setMarginStart(dp(6));
        header.addView(legBriBtn, legBriLp);

        topBarView = header;
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        View legBriBar = buildGameSpaceBrightnessBar(0xFF00E5FF);
        if (legBriBar != null) root.addView(legBriBar, new LinearLayout.LayoutParams(-1, -2));

        List<GameEntry> games = getActiveGameEntries();
        GameEntry focused = getActiveSelectedEntry();

        // Main Horizon: Left Rail | Center Turbine Reactor | Right Rail
        LinearLayout horizon = new LinearLayout(this);
        horizon.setOrientation(LinearLayout.HORIZONTAL);
        horizon.setGravity(Gravity.CENTER_VERTICAL);
        horizon.setPadding(0, dp(6), 0, dp(6));

        // Left Rail: Recently Played Games
        LinearLayout leftRail = new LinearLayout(this);
        leftRail.setOrientation(LinearLayout.VERTICAL);
        leftRail.setPadding(dp(4), dp(4), dp(4), dp(4));

        TextView leftTitle = label("RECENT GAMES", 8.5f, 0xFF00E5FF);
        leftTitle.setTypeface(null, Typeface.BOLD);
        leftTitle.setPadding(dp(4), 0, 0, dp(4));
        leftRail.addView(leftTitle);

        ScrollView leftScroll = new ScrollView(this);
        leftScroll.setVerticalScrollBarEnabled(false);
        LinearLayout leftCards = new LinearLayout(this);
        leftCards.setOrientation(LinearLayout.VERTICAL);

        int count = Math.min(games.size(), 4);
        for (int i = 0; i < count; i++) {
            final GameEntry e = games.get(i);
            boolean isSel = (focused != null && focused.pkg.equals(e.pkg));
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(6), dp(6), dp(6), dp(6));
            GradientDrawable cBg = new GradientDrawable();
            cBg.setColor(isSel ? 0xDD0D2636 : 0xAA081420);
            cBg.setCornerRadius(dp(8));
            cBg.setStroke(dp(1f), isSel ? 0xFF00E5FF : 0x4400E5FF);
            card.setBackground(cBg);

            ImageView ic = new ImageView(this);
            ic.setImageDrawable(getEntryIcon(e));
            card.addView(ic, new LinearLayout.LayoutParams(dp(28), dp(28)));

            TextView nm = label(e.label, 9f, Color.WHITE);
            nm.setSingleLine(true);
            nm.setEllipsize(TextUtils.TruncateAt.END);
            nm.setPadding(dp(6), 0, 0, 0);
            card.addView(nm, new LinearLayout.LayoutParams(dp(60), -2));

            card.setOnClickListener(v -> {
                selectedEntry = e;
                rebuildView();
            });
            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(-1, -2);
            cLp.bottomMargin = dp(6);
            leftCards.addView(card, cLp);
        }
        leftScroll.addView(leftCards);
        leftRail.addView(leftScroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        int railW = dp(isPortrait ? 96 : 120);
        horizon.addView(leftRail, new LinearLayout.LayoutParams(railW, -1));

        // Center Turbine Reactor Stage (Photo 2)
        LinearLayout centerReactor = new LinearLayout(this);
        centerReactor.setOrientation(LinearLayout.VERTICAL);
        centerReactor.setGravity(Gravity.CENTER);
        centerReactor.setPadding(dp(10), dp(4), dp(10), dp(4));

        FrameLayout turbineFrame = new FrameLayout(this);
        AnimatedGamingCircleView turbineView = new AnimatedGamingCircleView(this);
        turbineView.setRgbChromaActive(activeLayout == 20);
        int turbineSize = dp(isPortrait ? 130 : 155);
        turbineFrame.addView(turbineView, new FrameLayout.LayoutParams(turbineSize, turbineSize, Gravity.CENTER));

        ImageView reactorIcon = new ImageView(this);
        reactorIcon.setImageDrawable(getEntryIcon(focused));
        int iconSize = dp(isPortrait ? 52 : 64);
        turbineFrame.addView(reactorIcon, new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER));

        centerReactor.addView(turbineFrame, new LinearLayout.LayoutParams(-2, -2));

        TextView gameTitle = label(focused != null ? focused.label : "SELECT A GAME", isPortrait ? 14f : 16f, Color.WHITE);
        gameTitle.setTypeface(null, Typeface.BOLD);
        gameTitle.setSingleLine(true);
        gameTitle.setEllipsize(TextUtils.TruncateAt.END);
        gameTitle.setPadding(0, dp(6), 0, dp(4));
        centerReactor.addView(gameTitle);

        TextView launchPill = label("  LAUNCH CORE  ", 11f, Color.BLACK);
        launchPill.setTypeface(null, Typeface.BOLD);
        launchPill.setPadding(dp(24), dp(8), dp(24), dp(8));
        GradientDrawable lpBg = new GradientDrawable();
        lpBg.setColor(0xFF00E5FF);
        lpBg.setCornerRadius(dp(16));
        launchPill.setBackground(lpBg);
        launchPill.setOnClickListener(v -> {
            if (focused != null) launch(focused);
            else showAddGamesDialog();
        });
        centerReactor.addView(launchPill, new LinearLayout.LayoutParams(-2, -2));

        horizon.addView(centerReactor, new LinearLayout.LayoutParams(0, -1, 1f));

        // Right Rail: All Games
        LinearLayout rightRail = new LinearLayout(this);
        rightRail.setOrientation(LinearLayout.VERTICAL);
        rightRail.setPadding(dp(4), dp(4), dp(4), dp(4));

        TextView rightTitle = label("ALL GAMES", 8.5f, 0xFF00E5FF);
        rightTitle.setTypeface(null, Typeface.BOLD);
        rightTitle.setPadding(dp(4), 0, 0, dp(4));
        rightRail.addView(rightTitle);

        ScrollView rightScroll = new ScrollView(this);
        rightScroll.setVerticalScrollBarEnabled(false);
        LinearLayout rightCards = new LinearLayout(this);
        rightCards.setOrientation(LinearLayout.VERTICAL);

        int startIdx = Math.min(games.size(), 4);
        for (int i = startIdx; i < games.size(); i++) {
            final GameEntry e = games.get(i);
            boolean isSel = (focused != null && focused.pkg.equals(e.pkg));
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(6), dp(6), dp(6), dp(6));
            GradientDrawable cBg = new GradientDrawable();
            cBg.setColor(isSel ? 0xDD0D2636 : 0xAA081420);
            cBg.setCornerRadius(dp(8));
            cBg.setStroke(dp(1f), isSel ? 0xFF00E5FF : 0x4400E5FF);
            card.setBackground(cBg);

            ImageView ic = new ImageView(this);
            ic.setImageDrawable(getEntryIcon(e));
            card.addView(ic, new LinearLayout.LayoutParams(dp(28), dp(28)));

            TextView nm = label(e.label, 9f, Color.WHITE);
            nm.setSingleLine(true);
            nm.setEllipsize(TextUtils.TruncateAt.END);
            nm.setPadding(dp(6), 0, 0, 0);
            card.addView(nm, new LinearLayout.LayoutParams(dp(60), -2));

            card.setOnClickListener(v -> {
                selectedEntry = e;
                rebuildView();
            });
            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(-1, -2);
            cLp.bottomMargin = dp(6);
            rightCards.addView(card, cLp);
        }

        // Add Game Button in right rail
        TextView addBtn = label("+ ADD GAME", 9f, 0xFF00E5FF);
        addBtn.setTypeface(null, Typeface.BOLD);
        addBtn.setGravity(Gravity.CENTER);
        addBtn.setPadding(0, dp(8), 0, dp(8));
        addBtn.setOnClickListener(v -> showAddGamesDialog());
        rightCards.addView(addBtn, new LinearLayout.LayoutParams(-1, -2));

        rightScroll.addView(rightCards);
        rightRail.addView(rightScroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        horizon.addView(rightRail, new LinearLayout.LayoutParams(railW, -1));

        root.addView(attachResponsivePanels(horizon, 3, isPortrait), new LinearLayout.LayoutParams(-1, 0, 1f));

        // Bottom Legion Action Dock
        LinearLayout legionDock = new LinearLayout(this);
        legionDock.setOrientation(LinearLayout.HORIZONTAL);
        legionDock.setPadding(dp(4), dp(4), dp(4), dp(4));

        TextView dockTele = label("TURBO TELEMETRY", 9f, 0xFF00E5FF);
        dockTele.setTypeface(null, Typeface.BOLD);
        dockTele.setGravity(Gravity.CENTER);
        dockTele.setPadding(0, dp(8), 0, dp(8));
        GradientDrawable dtBg = new GradientDrawable();
        dtBg.setColor(0xDD091F2E);
        dtBg.setCornerRadius(dp(8));
        dtBg.setStroke(dp(1f), 0xFF00E5FF);
        dockTele.setBackground(dtBg);
        dockTele.setOnClickListener(v -> showGamingTelemetryDialog());
        legionDock.addView(dockTele, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView dockRam = label("RAM FLUSH", 9f, 0xFF00E676);
        dockRam.setTypeface(null, Typeface.BOLD);
        dockRam.setGravity(Gravity.CENTER);
        dockRam.setPadding(0, dp(8), 0, dp(8));
        GradientDrawable drBg = new GradientDrawable();
        drBg.setColor(0xDD082215);
        drBg.setCornerRadius(dp(8));
        drBg.setStroke(dp(1f), 0xFF00E676);
        dockRam.setBackground(drBg);
        dockRam.setOnClickListener(v -> {
            runQuickLauncherAction("cleanup");
            Toast.makeText(this, "RAM Flushed & Cleaned!", Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams drLp = new LinearLayout.LayoutParams(0, -2, 1f);
        drLp.setMarginStart(dp(6));
        legionDock.addView(dockRam, drLp);

        TextView dockCross = label("CROSSHAIR", 9f, Color.WHITE);
        dockCross.setTypeface(null, Typeface.BOLD);
        dockCross.setGravity(Gravity.CENTER);
        dockCross.setPadding(0, dp(8), 0, dp(8));
        GradientDrawable dcBg = new GradientDrawable();
        dcBg.setColor(0xDD121C28);
        dcBg.setCornerRadius(dp(8));
        dcBg.setStroke(dp(1f), 0xFF354E6B);
        dockCross.setBackground(dcBg);
        dockCross.setOnClickListener(v -> showUnifiedCrosshairDialog(null));
        LinearLayout.LayoutParams dcLp = new LinearLayout.LayoutParams(0, -2, 1f);
        dcLp.setMarginStart(dp(6));
        legionDock.addView(dockCross, dcLp);

        TextView dockFloat = label("FLOATING TOOLS", 9f, 0xFFFF9100);
        dockFloat.setTypeface(null, Typeface.BOLD);
        dockFloat.setGravity(Gravity.CENTER);
        dockFloat.setPadding(0, dp(8), 0, dp(8));
        GradientDrawable dfBg = new GradientDrawable();
        dfBg.setColor(0xDD261A0C);
        dfBg.setCornerRadius(dp(8));
        dfBg.setStroke(dp(1f), 0xFFFF9100);
        dockFloat.setBackground(dfBg);
        dockFloat.setOnClickListener(v -> toggleFloatingTools());
        LinearLayout.LayoutParams dfLp = new LinearLayout.LayoutParams(0, -2, 1f);
        dfLp.setMarginStart(dp(6));
        legionDock.addView(dockFloat, dfLp);

        root.addView(legionDock, new LinearLayout.LayoutParams(-1, -2));
        rootFrame.addView(root, new FrameLayout.LayoutParams(-1, -1));
        return rootFrame;
    }

    // Layout 4: Black Shark Space / Lobby (Photo 3 Reference)
    private View buildBlackSharkLobbyLayout(int activeLayout, boolean isPortrait) {
        FrameLayout rootFrame = new FrameLayout(this);
        CyberConsoleBackgroundView bgView = new CyberConsoleBackgroundView(this, CyberConsoleBackgroundView.MODE_BLACK_SHARK);
        rootFrame.addView(bgView, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(6), dp(14), dp(6));

        // Amber Top Header (Photo 3)
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(4), dp(2), dp(4), dp(4));

        View back = createCyberGlassBackButton(v -> finish());
        header.addView(back);

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.HORIZONTAL);
        brand.setPadding(dp(8), 0, dp(12), 0);
        TextView bsLogo = label("APEX ", 13f, 0xFFFFD600);
        bsLogo.setTypeface(null, Typeface.BOLD);
        brand.addView(bsLogo);
        TextView spLogo = label("HORIZON", 13f, Color.WHITE);
        spLogo.setTypeface(null, Typeface.BOLD);
        brand.addView(spLogo);
        header.addView(brand);

        // Top Navigation: GAMES (Active with dot) | Gamepad | Tools
        LinearLayout navRow = new LinearLayout(this);
        navRow.setOrientation(LinearLayout.HORIZONTAL);
        navRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tabGames = label("● GAMES", 10.5f, 0xFFFFD600);
        tabGames.setTypeface(null, Typeface.BOLD);
        tabGames.setPadding(dp(10), dp(4), dp(10), dp(4));
        navRow.addView(tabGames);

        ImageView padIcon = new ImageView(this);
        padIcon.setImageResource(R.drawable.ic_console);
        padIcon.setColorFilter(0x88FFD600);
        padIcon.setPadding(dp(6), dp(6), dp(6), dp(6));
        padIcon.setOnClickListener(v -> showGameSpaceCenterDialog());
        navRow.addView(padIcon, new LinearLayout.LayoutParams(dp(28), dp(28)));

        ImageView toolsIcon = new ImageView(this);
        toolsIcon.setImageResource(R.drawable.ic_speed_meter);
        toolsIcon.setColorFilter(0x88FFD600);
        toolsIcon.setPadding(dp(6), dp(6), dp(6), dp(6));
        toolsIcon.setOnClickListener(v -> showGamingTelemetryDialog());
        navRow.addView(toolsIcon, new LinearLayout.LayoutParams(dp(28), dp(28)));

        header.addView(navRow);

        View hSpacer = new View(this);
        header.addView(hSpacer, new LinearLayout.LayoutParams(0, 1, 1f));

        header.addView(createUniversalTelemetryBar(0xFFFFD600));

        ImageView settingsBtn = new ImageView(this);
        settingsBtn.setImageResource(R.drawable.ic_settings_icon);
        settingsBtn.setColorFilter(0xFFFFD600);
        settingsBtn.setPadding(dp(6), dp(6), dp(6), dp(6));
        GradientDrawable stBg = new GradientDrawable();
        stBg.setColor(0xCC1F1608);
        stBg.setCornerRadius(dp(8));
        stBg.setStroke(dp(1), 0xFFFFD600);
        settingsBtn.setBackground(stBg);
        settingsBtn.setOnClickListener(v -> showGameSpaceCenterDialog());
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(dp(30), dp(30));
        stLp.setMarginStart(dp(6));
        header.addView(createQuickLayoutHeaderButton(0xFFFFD600));
        header.addView(settingsBtn, stLp);

                ImageView bsBriBtn = new ImageView(this);
        bsBriBtn.setImageResource(R.drawable.ic_sun);
        bsBriBtn.setColorFilter(0xFF00E676);
        bsBriBtn.setPadding(dp(6), dp(6), dp(6), dp(6));
        GradientDrawable bsBriBg = new GradientDrawable();
        bsBriBg.setColor(0xCC091E14);
        bsBriBg.setCornerRadius(dp(8));
        bsBriBg.setStroke(dp(1), 0xFF00E676);
        bsBriBtn.setBackground(bsBriBg);
        bsBriBtn.setOnClickListener(v -> toggleBrightnessBar());
        LinearLayout.LayoutParams bsBriLp = new LinearLayout.LayoutParams(dp(30), dp(30));
        bsBriLp.setMarginStart(dp(6));
        header.addView(bsBriBtn, bsBriLp);

        topBarView = header;
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        View bsBriBar = buildGameSpaceBrightnessBar(0xFF00E676);
        if (bsBriBar != null) root.addView(bsBriBar, new LinearLayout.LayoutParams(-1, -2));

        List<GameEntry> games = getActiveGameEntries();
        GameEntry focused = getActiveSelectedEntry();

        // Center Amber Hero Stage with Golden Pill Button (Photo 3)
        LinearLayout stage = new LinearLayout(this);
        stage.setOrientation(LinearLayout.VERTICAL);
        stage.setGravity(Gravity.CENTER);
        stage.setPadding(dp(16), dp(10), dp(16), dp(10));

        // Chamfered Amber Banner Card
        LinearLayout bannerCard = new LinearLayout(this);
        bannerCard.setOrientation(LinearLayout.HORIZONTAL);
        bannerCard.setGravity(Gravity.CENTER_VERTICAL);
        bannerCard.setPadding(dp(20), dp(16), dp(20), dp(16));

        GradientDrawable bBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xEE2A1E08, 0xEE120E04});
        bBg.setCornerRadius(dp(16));
        bBg.setStroke(dp(1.8f), 0xFFFFD600);
        bannerCard.setBackground(bBg);

        ImageView bigIcon = new ImageView(this);
        bigIcon.setImageDrawable(getEntryIcon(focused));
        bannerCard.addView(bigIcon, new LinearLayout.LayoutParams(dp(isPortrait ? 56 : 70), dp(isPortrait ? 56 : 70)));

        LinearLayout bDetails = new LinearLayout(this);
        bDetails.setOrientation(LinearLayout.VERTICAL);
        bDetails.setPadding(dp(16), 0, 0, 0);

        TextView bTitle = label(focused != null ? focused.label : "NO GAMES SELECTED", isPortrait ? 15f : 18f, Color.WHITE);
        bTitle.setTypeface(null, Typeface.BOLD);
        bTitle.setSingleLine(true);
        bTitle.setEllipsize(TextUtils.TruncateAt.END);
        bDetails.addView(bTitle);

        TextView bDesc = label("144Hz DISPLAY SYNC  \u2022  ULTRA TOUCH RESPONSE", 9f, 0xFFFFD600);
        bDetails.addView(bDesc);

        TextView dotIndicator = label("● ○ ○ ○", 8f, 0x88FFD600);
        dotIndicator.setPadding(0, dp(4), 0, 0);
        bDetails.addView(dotIndicator);

        bannerCard.addView(bDetails, new LinearLayout.LayoutParams(0, -2, 1f));

        int bannerW = dp(isPortrait ? 270 : 390);
        stage.addView(bannerCard, new LinearLayout.LayoutParams(bannerW, -2));

        // Prominent Golden Pill "PLAY NOW" Button (Photo 3)
        TextView playPill = label("  PLAY NOW  ", 12.5f, Color.BLACK);
        playPill.setTypeface(null, Typeface.BOLD);
        playPill.setPadding(dp(36), dp(10), dp(36), dp(10));
        GradientDrawable pBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFFFFD600, 0xFFFFAB00});
        pBg.setCornerRadius(dp(22));
        pBg.setStroke(dp(1.2f), 0xFFFFF9C4);
        playPill.setBackground(pBg);
        playPill.setOnClickListener(v -> {
            if (focused != null) launch(focused);
            else showAddGamesDialog();
        });
        LinearLayout.LayoutParams ppLp = new LinearLayout.LayoutParams(-2, -2);
        ppLp.topMargin = dp(12);
        stage.addView(playPill, ppLp);

        root.addView(attachResponsivePanels(stage, 4, isPortrait), new LinearLayout.LayoutParams(-1, 0, 1f));

        // Horizontal Game Carousel
        HorizontalScrollView hScroll = new HorizontalScrollView(this);
        hScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout carousel = new LinearLayout(this);
        carousel.setOrientation(LinearLayout.HORIZONTAL);
        carousel.setPadding(0, dp(4), 0, dp(4));

        for (int i = 0; i < games.size(); i++) {
            final GameEntry e = games.get(i);
            boolean isSel = (focused != null && focused.pkg.equals(e.pkg));
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(8), dp(6), dp(10), dp(6));
            GradientDrawable cBg = new GradientDrawable();
            cBg.setColor(isSel ? 0xDD2A1E08 : 0xAA141006);
            cBg.setCornerRadius(dp(10));
            cBg.setStroke(dp(isSel ? 1.5f : 0.8f), isSel ? 0xFFFFD600 : 0x55FFD600);
            card.setBackground(cBg);

            ImageView ic = new ImageView(this);
            ic.setImageDrawable(getEntryIcon(e));
            card.addView(ic, new LinearLayout.LayoutParams(dp(30), dp(30)));

            TextView nm = label(e.label, 9.5f, isSel ? Color.WHITE : 0xFFCCCCCC);
            nm.setSingleLine(true);
            nm.setEllipsize(TextUtils.TruncateAt.END);
            nm.setPadding(dp(6), 0, 0, 0);
            card.addView(nm, new LinearLayout.LayoutParams(dp(80), -2));

            card.setOnClickListener(v -> {
                selectedEntry = e;
                rebuildView();
            });
            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(-2, dp(44));
            cLp.setMarginEnd(dp(8));
            carousel.addView(card, cLp);
        }
        hScroll.addView(carousel);
        root.addView(hScroll, new LinearLayout.LayoutParams(-1, -2));

        // Bottom Black Shark Quick Action Pills
        LinearLayout bsDock = new LinearLayout(this);
        bsDock.setOrientation(LinearLayout.HORIZONTAL);
        bsDock.setPadding(dp(4), dp(4), dp(4), dp(4));

        TextView bsLudi = label("OVERDRIVE MODE", 9f, 0xFFFFD600);
        bsLudi.setTypeface(null, Typeface.BOLD);
        bsLudi.setGravity(Gravity.CENTER);
        bsLudi.setPadding(0, dp(8), 0, dp(8));
        GradientDrawable blBg = new GradientDrawable();
        blBg.setColor(0xDD261B08);
        blBg.setCornerRadius(dp(8));
        blBg.setStroke(dp(1f), 0xFFFFD600);
        bsLudi.setBackground(blBg);
        bsLudi.setOnClickListener(v -> showGamingTelemetryDialog());
        bsDock.addView(bsLudi, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView bsSync = label("144Hz SYNC", 9f, Color.WHITE);
        bsSync.setTypeface(null, Typeface.BOLD);
        bsSync.setGravity(Gravity.CENTER);
        bsSync.setPadding(0, dp(8), 0, dp(8));
        GradientDrawable bsyBg = new GradientDrawable();
        bsyBg.setColor(0xDD1B160C);
        bsyBg.setCornerRadius(dp(8));
        bsyBg.setStroke(dp(1f), 0x55FFD600);
        bsSync.setBackground(bsyBg);
        bsSync.setOnClickListener(v -> setRefreshRateDirect(144));
        LinearLayout.LayoutParams bsyLp = new LinearLayout.LayoutParams(0, -2, 1f);
        bsyLp.setMarginStart(dp(6));
        bsDock.addView(bsSync, bsyLp);

        TextView bsCross = label("CROSSHAIR", 9f, 0xFF00E5FF);
        bsCross.setTypeface(null, Typeface.BOLD);
        bsCross.setGravity(Gravity.CENTER);
        bsCross.setPadding(0, dp(8), 0, dp(8));
        GradientDrawable bcBg = new GradientDrawable();
        bcBg.setColor(0xDD0A1C28);
        bcBg.setCornerRadius(dp(8));
        bcBg.setStroke(dp(1f), 0xFF00E5FF);
        bsCross.setBackground(bcBg);
        bsCross.setOnClickListener(v -> showUnifiedCrosshairDialog(null));
        LinearLayout.LayoutParams bcLp = new LinearLayout.LayoutParams(0, -2, 1f);
        bcLp.setMarginStart(dp(6));
        bsDock.addView(bsCross, bcLp);

        TextView bsFloat = label("FLOATING TOOLS", 9f, 0xFF00E676);
        bsFloat.setTypeface(null, Typeface.BOLD);
        bsFloat.setGravity(Gravity.CENTER);
        bsFloat.setPadding(0, dp(8), 0, dp(8));
        GradientDrawable bfBg = new GradientDrawable();
        bfBg.setColor(0xDD092415);
        bfBg.setCornerRadius(dp(8));
        bfBg.setStroke(dp(1f), 0xFF00E676);
        bsFloat.setBackground(bfBg);
        bsFloat.setOnClickListener(v -> toggleFloatingTools());
        LinearLayout.LayoutParams bfLp = new LinearLayout.LayoutParams(0, -2, 1f);
        bfLp.setMarginStart(dp(6));
        bsDock.addView(bsFloat, bfLp);

        root.addView(bsDock, new LinearLayout.LayoutParams(-1, -2));
        rootFrame.addView(root, new FrameLayout.LayoutParams(-1, -1));
        return rootFrame;
    }

    // Layout 5: Xiaomi Game Turbo Cockpit (Photo 4 Reference)
    private View buildGameTurboCockpitLayout(boolean isPortrait) {
        FrameLayout rootFrame = new FrameLayout(this);
        CyberConsoleBackgroundView bgView = new CyberConsoleBackgroundView(this, CyberConsoleBackgroundView.MODE_GAME_TURBO);
        rootFrame.addView(bgView, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(6), dp(14), dp(6));

        // Cockpit Header: „Spiellobby“ on left | HUD Pill on right (Photo 4)
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(4), dp(2), dp(4), dp(4));

        View back = createCyberGlassBackButton(v -> finish());
        header.addView(back);

        TextView lobbyTitle = label("GAME COCKPIT", 14f, 0xFF00E5FF);
        lobbyTitle.setTypeface(null, Typeface.BOLD);
        lobbyTitle.setPadding(dp(10), 0, 0, 0);
        header.addView(lobbyTitle);

        View hSpacer = new View(this);
        header.addView(hSpacer, new LinearLayout.LayoutParams(0, 1, 1f));

        header.addView(createUniversalTelemetryBar(0xFF00E5FF));

        ImageView settingsBtn = new ImageView(this);
        settingsBtn.setImageResource(R.drawable.ic_settings_icon);
        settingsBtn.setColorFilter(0xFF00E5FF);
        settingsBtn.setPadding(dp(6), dp(6), dp(6), dp(6));
        GradientDrawable stBg = new GradientDrawable();
        stBg.setColor(0xCC09182B);
        stBg.setCornerRadius(dp(8));
        stBg.setStroke(dp(1), 0xFF00E5FF);
        settingsBtn.setBackground(stBg);
        settingsBtn.setOnClickListener(v -> showGameSpaceCenterDialog());
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(dp(30), dp(30));
        stLp.setMarginStart(dp(6));
        header.addView(settingsBtn, stLp);

        ImageView gtBriBtn = new ImageView(this);
        gtBriBtn.setImageResource(R.drawable.ic_sun);
        gtBriBtn.setColorFilter(0xFFFF9100);
        gtBriBtn.setPadding(dp(6), dp(6), dp(6), dp(6));
        GradientDrawable gtBriBg = new GradientDrawable();
        gtBriBg.setColor(0xCC201004);
        gtBriBg.setCornerRadius(dp(8));
        gtBriBg.setStroke(dp(1), 0xFFFF9100);
        gtBriBtn.setBackground(gtBriBg);
        gtBriBtn.setOnClickListener(v -> toggleBrightnessBar());
        LinearLayout.LayoutParams gtBriLp = new LinearLayout.LayoutParams(dp(30), dp(30));
        gtBriLp.setMarginStart(dp(6));
        header.addView(gtBriBtn, gtBriLp);

        topBarView = header;
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        View gtBriBar = buildGameSpaceBrightnessBar(0xFFFF9100);
        if (gtBriBar != null) root.addView(gtBriBar, new LinearLayout.LayoutParams(-1, -2));

        List<GameEntry> games = getActiveGameEntries();
        GameEntry focused = getActiveSelectedEntry();

        // Cockpit Center Stage: Main Game Card + „Spielliste“ Card
        LinearLayout stage = new LinearLayout(this);
        stage.setOrientation(LinearLayout.HORIZONTAL);
        stage.setGravity(Gravity.CENTER);
        stage.setPadding(dp(10), dp(8), dp(10), dp(8));

        // Main Recessed Cockpit Game Card
        LinearLayout cardFrame = new LinearLayout(this);
        cardFrame.setOrientation(LinearLayout.VERTICAL);
        cardFrame.setPadding(0, 0, 0, dp(14));

        GradientDrawable cfBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xEE111E2E, 0xEE09111C});
        cfBg.setCornerRadius(dp(16));
        cfBg.setStroke(dp(1.8f), 0xFF00E5FF);
        cardFrame.setBackground(cfBg);

        // Neon Orange Top Status Bar
        View statusBar = new View(this);
        GradientDrawable sbBg = new GradientDrawable();
        sbBg.setColor(0xFFFF9100);
        statusBar.setBackground(sbBg);
        cardFrame.addView(statusBar, new LinearLayout.LayoutParams(-1, dp(3)));

        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(LinearLayout.HORIZONTAL);
        cardContent.setGravity(Gravity.CENTER_VERTICAL);
        cardContent.setPadding(dp(16), dp(12), dp(16), dp(8));

        ImageView bigIcon = new ImageView(this);
        bigIcon.setImageDrawable(getEntryIcon(focused));
        cardContent.addView(bigIcon, new LinearLayout.LayoutParams(dp(isPortrait ? 56 : 66), dp(isPortrait ? 56 : 66)));

        LinearLayout cInfo = new LinearLayout(this);
        cInfo.setOrientation(LinearLayout.VERTICAL);
        cInfo.setPadding(dp(14), 0, 0, 0);

        TextView cTitle = label(focused != null ? focused.label : "NO GAMES SELECTED", isPortrait ? 15f : 17f, Color.WHITE);
        cTitle.setTypeface(null, Typeface.BOLD);
        cTitle.setSingleLine(true);
        cTitle.setEllipsize(TextUtils.TruncateAt.END);
        cInfo.addView(cTitle);

        TextView cSub = label("TURBO ENGINE ACTIVE  \u2022  FPS MAXED", 8.5f, 0xFF00E5FF);
        cInfo.addView(cSub);
        TextView swipeHint = label("SWIPE  \u2190  \u2192  TO CHANGE GAME", 7.5f, 0xFFFFB45C);
        swipeHint.setTypeface(null, Typeface.BOLD);
        swipeHint.setLetterSpacing(0.08f);
        swipeHint.setPadding(0, dp(3), 0, 0);
        cInfo.addView(swipeHint);
        cardContent.addView(cInfo, new LinearLayout.LayoutParams(0, -2, 1f));

        cardFrame.addView(cardContent);

        // Turbo Launch Button
        TextView launchAction = label("  TURBO START  ", 11.5f, Color.WHITE);
        launchAction.setTypeface(null, Typeface.BOLD);
        launchAction.setPadding(dp(28), dp(8), dp(28), dp(8));
        GradientDrawable laBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFFFF9100, 0xFFFF3D00});
        laBg.setCornerRadius(dp(16));
        launchAction.setBackground(laBg);
        launchAction.setOnClickListener(v -> {
            if (focused != null) launch(focused);
            else showAddGamesDialog();
        });
        LinearLayout.LayoutParams laLp = new LinearLayout.LayoutParams(-2, -2);
        laLp.gravity = Gravity.CENTER_HORIZONTAL;
        cardFrame.addView(launchAction, laLp);

        int cardW = dp(isPortrait ? 220 : 330);
        stage.addView(cardFrame, new LinearLayout.LayoutParams(cardW, -2));

        // Side „Spielliste“ (Game List / Add Games) Card
        LinearLayout sideListCard = new LinearLayout(this);
        sideListCard.setOrientation(LinearLayout.VERTICAL);
        sideListCard.setGravity(Gravity.CENTER);
        sideListCard.setPadding(dp(14), dp(14), dp(14), dp(14));

        GradientDrawable slBg = new GradientDrawable();
        slBg.setColor(0xDD0A1524);
        slBg.setCornerRadius(dp(16));
        slBg.setStroke(dp(1.2f), 0x5500E5FF);
        sideListCard.setBackground(slBg);

        ImageView plusIcon = new ImageView(this);
        plusIcon.setImageResource(R.drawable.ic_add);
        plusIcon.setColorFilter(0xFF00E5FF);
        sideListCard.addView(plusIcon, new LinearLayout.LayoutParams(dp(36), dp(36)));

        TextView slTitle = label("GAME LIST", 10.5f, 0xFF00E5FF);
        slTitle.setTypeface(null, Typeface.BOLD);
        slTitle.setPadding(0, dp(6), 0, 0);
        sideListCard.addView(slTitle);

        TextView slSub = label(games.size() + " Games", 8f, 0xFF88A0B8);
        sideListCard.addView(slSub);

        sideListCard.setOnClickListener(v -> showAddGamesDialog());

        int sideW = dp(isPortrait ? 90 : 120);
        LinearLayout.LayoutParams slLp = new LinearLayout.LayoutParams(sideW, -2);
        slLp.setMarginStart(dp(10));
        stage.addView(sideListCard, slLp);

        // The focused game now behaves like a real carousel: horizontal swipes on
        // the middle stage select the previous/next installed game and rebuild the
        // cockpit around it. Attach to both surfaces so swipes work on the card or
        // the empty stage area without affecting the launch button's normal tap.
        attachGameSwipeNavigation(stage, games);
        attachGameSwipeNavigation(cardFrame, games);

        root.addView(attachResponsivePanels(stage, 5, isPortrait), new LinearLayout.LayoutParams(-1, 0, 1f));

        // Bottom Cockpit Navigation Dock (Photo 4)
        LinearLayout cockpitDock = new LinearLayout(this);
        cockpitDock.setOrientation(LinearLayout.HORIZONTAL);
        cockpitDock.setPadding(dp(4), dp(4), dp(4), dp(4));

        TextView btnBoost = label("BOOST RAM", 9f, 0xFF00E676);
        btnBoost.setTypeface(null, Typeface.BOLD);
        btnBoost.setGravity(Gravity.CENTER);
        btnBoost.setPadding(0, dp(8), 0, dp(8));
        GradientDrawable bbBg = new GradientDrawable();
        bbBg.setColor(0xDD091F14);
        bbBg.setCornerRadius(dp(8));
        bbBg.setStroke(dp(1f), 0xFF00E676);
        btnBoost.setBackground(bbBg);
        btnBoost.setOnClickListener(v -> {
            runQuickLauncherAction("cleanup");
            Toast.makeText(this, "RAM Boosted & Cleaned!", Toast.LENGTH_SHORT).show();
        });
        cockpitDock.addView(btnBoost, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView btnDnd = label("DND MODE", 9f, 0xFFFF9100);
        btnDnd.setTypeface(null, Typeface.BOLD);
        btnDnd.setGravity(Gravity.CENTER);
        btnDnd.setPadding(0, dp(8), 0, dp(8));
        GradientDrawable bdBg = new GradientDrawable();
        bdBg.setColor(0xDD241708);
        bdBg.setCornerRadius(dp(8));
        bdBg.setStroke(dp(1f), 0xFFFF9100);
        btnDnd.setBackground(bdBg);
        btnDnd.setOnClickListener(v -> runQuickLauncherAction("dnd"));
        LinearLayout.LayoutParams bdLp = new LinearLayout.LayoutParams(0, -2, 1f);
        bdLp.setMarginStart(dp(6));
        cockpitDock.addView(btnDnd, bdLp);

        TextView btnCross = label("CROSSHAIR", 9f, 0xFF00E5FF);
        btnCross.setTypeface(null, Typeface.BOLD);
        btnCross.setGravity(Gravity.CENTER);
        btnCross.setPadding(0, dp(8), 0, dp(8));
        GradientDrawable bcBg = new GradientDrawable();
        bcBg.setColor(0xDD081824);
        bcBg.setCornerRadius(dp(8));
        bcBg.setStroke(dp(1f), 0xFF00E5FF);
        btnCross.setBackground(bcBg);
        btnCross.setOnClickListener(v -> showUnifiedCrosshairDialog(null));
        LinearLayout.LayoutParams bcLp = new LinearLayout.LayoutParams(0, -2, 1f);
        bcLp.setMarginStart(dp(6));
        cockpitDock.addView(btnCross, bcLp);

        TextView btnFloat = label("FLOATING TOOLS", 9f, 0xFFC084FC);
        btnFloat.setTypeface(null, Typeface.BOLD);
        btnFloat.setGravity(Gravity.CENTER);
        btnFloat.setPadding(0, dp(8), 0, dp(8));
        GradientDrawable bfBg = new GradientDrawable();
        bfBg.setColor(0xDD20122E);
        bfBg.setCornerRadius(dp(8));
        bfBg.setStroke(dp(1f), 0xFFC084FC);
        btnFloat.setBackground(bfBg);
        btnFloat.setOnClickListener(v -> toggleFloatingTools());
        LinearLayout.LayoutParams bfLp = new LinearLayout.LayoutParams(0, -2, 1f);
        bfLp.setMarginStart(dp(6));
        cockpitDock.addView(btnFloat, bfLp);

        root.addView(cockpitDock, new LinearLayout.LayoutParams(-1, -2));
        rootFrame.addView(root, new FrameLayout.LayoutParams(-1, -1));
        return rootFrame;
    }

    private void attachGameSwipeNavigation(View surface, List<GameEntry> games) {
        if (surface == null || games == null || games.size() < 2) return;
        surface.setContentDescription("Current game. Swipe left or right to change game");
        surface.setClickable(true);
        surface.setOnTouchListener(new View.OnTouchListener() {
            private float downX;
            private float downY;
            private long downTime;

            @Override public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getX();
                        downY = event.getY();
                        downTime = event.getEventTime();
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        return true;
                    case MotionEvent.ACTION_UP:
                        float dx = event.getX() - downX;
                        float dy = event.getY() - downY;
                        long elapsed = Math.max(1L, event.getEventTime() - downTime);
                        float velocityX = Math.abs(dx) * 1000f / elapsed;
                        if (Math.abs(dx) >= dp(44) && Math.abs(dx) > Math.abs(dy) * 1.25f
                                && velocityX >= dp(120)) {
                            int current = games.indexOf(selectedEntry);
                            if (current < 0) current = 0;
                            int direction = dx < 0 ? 1 : -1;
                            int next = Math.floorMod(current + direction, games.size());
                            selectedEntry = games.get(next);
                            try { view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
                            final float exit = dx < 0 ? -dp(28) : dp(28);
                            view.animate().translationX(exit).alpha(0.45f).setDuration(90)
                                    .withEndAction(() -> {
                                        view.setTranslationX(0f);
                                        view.setAlpha(1f);
                                        rebuildView();
                                    }).start();
                            return true;
                        }
                        view.performClick();
                        return true;
                    default:
                        return true;
                }
            }
        });
    }

    private int getBatteryLevel() {
        try {
            Intent b = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            return b == null ? 85 : b.getIntExtra(BatteryManager.EXTRA_LEVEL, 85);
        } catch (Exception ignored) {
            return 85;
        }
    }

    private int getBatteryTemp() {
        try {
            Intent b = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            return b == null ? 35 : Math.round(b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 350) / 10f);
        } catch (Exception ignored) {
            return 35;
        }
    }

    private int getCpuPercent() {
        try {
            int cpu = cpuStats.readPercent(GsmProfileManager.isRootEnabled(this));
            return cpu < 0 ? 25 : cpu;
        } catch (Exception ignored) {
            return 25;
        }
    }

    private void showGamingTelemetryDialog() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        FrameLayout root = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xF60A101C, 0xF8050810, 0xF60D1526});
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1.5f), 0xFF00E5FF);
        root.setBackground(bg);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(10));

        View back = createCyberGlassBackButton(v -> dialog.dismiss());
        header.addView(back);

        TextView title = label("  GAMING TELEMETRY & HARDWARE CONTROL", 14f, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView refreshBadge = label("LIVE SENSORS", 8.5f, 0xFF00E676);
        refreshBadge.setTypeface(null, Typeface.BOLD);
        refreshBadge.setPadding(dp(10), dp(4), dp(10), dp(4));
        GradientDrawable rbBg = new GradientDrawable();
        rbBg.setColor(0x3300E676);
        rbBg.setCornerRadius(dp(10));
        rbBg.setStroke(dp(1), 0xFF00E676);
        refreshBadge.setBackground(rbBg);
        header.addView(refreshBadge);

        container.addView(header);

        // Body
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);

        // Row 1: Real-time Sensors (CPU, GPU, RAM, Battery Temp)
        LinearLayout sensorsRow = new LinearLayout(this);
        sensorsRow.setOrientation(LinearLayout.HORIZONTAL);
        sensorsRow.setPadding(0, dp(4), 0, dp(8));

        // CPU Card
        LinearLayout cpuCard = createTelemetryCard(false);
        int curCpu = getCpuPercent();
        TextView cpuText = label("CPU: " + Math.max(0, curCpu) + "%", 11f, 0xFF00E5FF);
        cpuText.setTypeface(null, Typeface.BOLD);
        cpuCard.addView(cpuText);
        ProgressBar cpuBar = createMiniProgress(0xFF00E5FF, false);
        cpuBar.setProgress(Math.max(5, curCpu));
        cpuCard.addView(cpuBar);
        sensorsRow.addView(cpuCard, new LinearLayout.LayoutParams(0, -2, 1f));

        // GPU / Battery Card
        LinearLayout gpuCard = createTelemetryCard(false);
        int curBat = getBatteryLevel();
        int curTemp = getBatteryTemp();
        TextView gpuText = label("BATTERY: " + curBat + "% (" + curTemp + "\u00B0C)", 11f, 0xFFFF9100);
        gpuText.setTypeface(null, Typeface.BOLD);
        gpuCard.addView(gpuText);
        ProgressBar batBar = createMiniProgress(0xFFFF9100, false);
        batBar.setProgress(curBat);
        gpuCard.addView(batBar);
        LinearLayout.LayoutParams gcLp = new LinearLayout.LayoutParams(0, -2, 1f);
        gcLp.setMarginStart(dp(8));
        sensorsRow.addView(gpuCard, gcLp);

        // RAM Card
        LinearLayout ramCard = createTelemetryCard(false);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (am != null) am.getMemoryInfo(mi);
        int ramPct = mi.totalMem <= 0 ? 0 : Math.round((mi.totalMem - mi.availMem) * 100f / mi.totalMem);
        TextView ramText = label("RAM: " + ramPct + "% LOAD", 11f, 0xFFC084FC);
        ramText.setTypeface(null, Typeface.BOLD);
        ramCard.addView(ramText);
        ProgressBar ramBar = createMiniProgress(0xFFC084FC, false);
        ramBar.setProgress(ramPct);
        ramCard.addView(ramBar);
        LinearLayout.LayoutParams rcLp = new LinearLayout.LayoutParams(0, -2, 1f);
        rcLp.setMarginStart(dp(8));
        sensorsRow.addView(ramCard, rcLp);

        body.addView(sensorsRow);

        // Row 2: Target Refresh Rate (60Hz, 90Hz, 120Hz, 144Hz)
        LinearLayout fpsSection = new LinearLayout(this);
        fpsSection.setOrientation(LinearLayout.VERTICAL);
        fpsSection.setPadding(0, dp(4), 0, dp(8));
        TextView fpsHeader = label("DISPLAY REFRESH RATE & TARGET FPS", 10.5f, 0xFF00E5FF);
        fpsHeader.setTypeface(null, Typeface.BOLD);
        fpsSection.addView(fpsHeader);

        LinearLayout ratesRow = new LinearLayout(this);
        ratesRow.setOrientation(LinearLayout.HORIZONTAL);
        ratesRow.setPadding(0, dp(6), 0, dp(6));
        int[] rates = {60, 90, 120, 144};
        for (int rate : rates) {
            TextView rateBtn = label(rate + " Hz", 11f, Color.WHITE);
            rateBtn.setTypeface(null, Typeface.BOLD);
            rateBtn.setGravity(Gravity.CENTER);
            rateBtn.setPadding(dp(16), dp(10), dp(16), dp(10));
            GradientDrawable rb = new GradientDrawable();
            rb.setColor(0xDD0D2034);
            rb.setCornerRadius(dp(10));
            rb.setStroke(dp(1.2f), 0xFF00E5FF);
            rateBtn.setBackground(rb);
            rateBtn.setOnClickListener(v -> {
                setRefreshRateDirect(rate);
                dialog.dismiss();
            });
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(0, -2, 1f);
            rLp.setMarginEnd(dp(6));
            ratesRow.addView(rateBtn, rLp);
        }
        fpsSection.addView(ratesRow);
        body.addView(fpsSection);

        // Row 3: Performance Profiles
        LinearLayout profSection = new LinearLayout(this);
        profSection.setOrientation(LinearLayout.VERTICAL);
        profSection.setPadding(0, dp(4), 0, dp(8));
        TextView profHeader = label("ENERGY CORE PERFORMANCE PROFILES", 10.5f, 0xFFFF1744);
        profHeader.setTypeface(null, Typeface.BOLD);
        profSection.addView(profHeader);

        LinearLayout pRow = new LinearLayout(this);
        pRow.setOrientation(LinearLayout.HORIZONTAL);
        pRow.setPadding(0, dp(6), 0, dp(6));
        String[] profNames = {"BATTERY+", "BALANCE+", "SMOOTH+", "ULTRA+", "ESPORTS+", "APEX+"};
        int[] profIndices = {0, 1, 4, 5, 6, 7};
        for (int i = 0; i < profNames.length; i++) {
            final int pIdx = profIndices[i];
            TextView pBtn = label(profNames[i], 9.5f, Color.WHITE);
            pBtn.setTypeface(null, Typeface.BOLD);
            pBtn.setGravity(Gravity.CENTER);
            pBtn.setPadding(dp(8), dp(10), dp(8), dp(10));
            GradientDrawable pb = new GradientDrawable();
            boolean isCur = (launcherProfile == pIdx);
            pb.setColor(isCur ? 0xFFFF1744 : 0xDD1A0C14);
            pb.setCornerRadius(dp(8));
            pb.setStroke(dp(1.2f), isCur ? 0xFFFF5252 : 0x66FF1744);
            pBtn.setBackground(pb);
            pBtn.setOnClickListener(v -> {
                applyLauncherProfile(pIdx);
                Toast.makeText(this, "Profile Armed: " + profNames[pIdx < profNames.length ? pIdx : 0], Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
            LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(0, -2, 1f);
            pLp.setMarginEnd(dp(4));
            pRow.addView(pBtn, pLp);
        }
        profSection.addView(pRow);
        body.addView(profSection);

        // Row 4: Assist Tools
        LinearLayout toolsSection = new LinearLayout(this);
        toolsSection.setOrientation(LinearLayout.HORIZONTAL);
        toolsSection.setPadding(0, dp(4), 0, dp(4));

        TextView crosshairBtn = label("CROSSHAIR STUDIO", 10f, 0xFF00E5FF);
        crosshairBtn.setTypeface(null, Typeface.BOLD);
        crosshairBtn.setGravity(Gravity.CENTER);
        crosshairBtn.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable chBg = new GradientDrawable();
        chBg.setColor(0xDD091A2E);
        chBg.setCornerRadius(dp(10));
        chBg.setStroke(dp(1.2f), 0xFF00E5FF);
        crosshairBtn.setBackground(chBg);
        crosshairBtn.setOnClickListener(v -> {
            dialog.dismiss();
            showUnifiedCrosshairDialog(null);
        });
        toolsSection.addView(crosshairBtn, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView ramCleanBtn = label("FLUSH RAM", 10f, 0xFF00E676);
        ramCleanBtn.setTypeface(null, Typeface.BOLD);
        ramCleanBtn.setGravity(Gravity.CENTER);
        ramCleanBtn.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable rcBg = new GradientDrawable();
        rcBg.setColor(0xDD092415);
        rcBg.setCornerRadius(dp(10));
        rcBg.setStroke(dp(1.2f), 0xFF00E676);
        ramCleanBtn.setBackground(rcBg);
        ramCleanBtn.setOnClickListener(v -> {
            runQuickLauncherAction("cleanup");
            Toast.makeText(this, "RAM Flushed & Cleaned!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        LinearLayout.LayoutParams rcLp2 = new LinearLayout.LayoutParams(0, -2, 1f);
        rcLp2.setMarginStart(dp(6));
        toolsSection.addView(ramCleanBtn, rcLp2);

        TextView floatingBtn = label("FLOATING TOOLS: " + (bubbleEnabled ? "ON" : "OFF"), 10f, 0xFFFF9100);
        floatingBtn.setTypeface(null, Typeface.BOLD);
        floatingBtn.setGravity(Gravity.CENTER);
        floatingBtn.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable ftBg = new GradientDrawable();
        ftBg.setColor(0xDD241708);
        ftBg.setCornerRadius(dp(10));
        ftBg.setStroke(dp(1.2f), 0xFFFF9100);
        floatingBtn.setBackground(ftBg);
        floatingBtn.setOnClickListener(v -> {
            toggleFloatingTools();
            dialog.dismiss();
        });
        LinearLayout.LayoutParams ftLp = new LinearLayout.LayoutParams(0, -2, 1f);
        ftLp.setMarginStart(dp(6));
        toolsSection.addView(floatingBtn, ftLp);

        TextView manageBtn = label("ADD / MANAGE GAMES", 10f, Color.WHITE);
        manageBtn.setTypeface(null, Typeface.BOLD);
        manageBtn.setGravity(Gravity.CENTER);
        manageBtn.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable mgBg = new GradientDrawable();
        mgBg.setColor(0xDD1C2132);
        mgBg.setCornerRadius(dp(10));
        mgBg.setStroke(dp(1.2f), 0xFF4D587B);
        manageBtn.setBackground(mgBg);
        manageBtn.setOnClickListener(v -> {
            dialog.dismiss();
            showManageGamesDialog();
        });
        LinearLayout.LayoutParams mgLp = new LinearLayout.LayoutParams(0, -2, 1f);
        mgLp.setMarginStart(dp(6));
        toolsSection.addView(manageBtn, mgLp);

        body.addView(toolsSection);
        scroll.addView(body);
        container.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(container, new FrameLayout.LayoutParams(-1, -1));

        dialog.setContentView(root);
        dialog.show();
    }

    private void setRefreshRateDirect(int target) {
        runLauncherCommand("settings put system peak_refresh_rate " + target + "; settings put system min_refresh_rate " + target + "; settings put system user_refresh_rate " + target + ";");
        if (telemetryFpsText != null) {
            telemetryFpsText.setText(target + " Hz  |  " + target + " FPS TARGET");
        }
        Toast.makeText(this, "Display Locked: " + target + "Hz Target", Toast.LENGTH_SHORT).show();
    }

    private View buildFuturisticMechaLayout(int activeLayout, boolean isPortrait) {
        return buildArmouryCrateLayout(isPortrait);
    }

    private View buildGridCatalogLayout(int activeLayout, boolean isPortrait) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(6), dp(14), dp(6));
        root.setBackgroundColor(0x00000000);

        topBarView = buildRORHeader();
        root.addView(topBarView, new LinearLayout.LayoutParams(-1, -2));

        List<GameEntry> games = getActiveGameEntries();

        int cols = 3;
        int cardHeight = 118;
        boolean isPoster = (activeLayout == 32);
        boolean isMasonry = (activeLayout == 28);

        if (activeLayout == 24) {
            cols = 2; cardHeight = 150;
        } else if (activeLayout == 17 || activeLayout == 25 || activeLayout == 18 || activeLayout == 13) {
            cols = 3; cardHeight = 120;
        } else if (activeLayout == 11 || activeLayout == 12 || activeLayout == 26) {
            cols = isPortrait  ?  3 : 4; cardHeight = 100;
        } else if (activeLayout == 16 || activeLayout == 27) {
            cols = isPortrait  ?  3 : 5; cardHeight = 85;
        } else if (activeLayout == 19) {
            cols = isPortrait  ?  4 : 6; cardHeight = 70;
        } else if (isPoster) {
            cols = isPortrait  ?  3 : 4; cardHeight = 160;
        }

        if (activeLayout == 13 || activeLayout == 17) {
            LinearLayout tabs = new LinearLayout(this);
            tabs.setOrientation(LinearLayout.HORIZONTAL);
            tabs.setPadding(0, dp(4), 0, dp(8));
            String[] catNames = {"ALL GAMES", "TOP PLAYED", "120Hz HIGH FPS", "FAVORITES"};
            for (int t = 0; t < catNames.length; t++) {
                TextView tab = label(catNames[t], 9.5f, t == 0  ?  0xFF00E5FF : 0xFF888A96);
                tab.setTypeface(null, Typeface.BOLD);
                tab.setPadding(dp(12), dp(4), dp(12), dp(4));
                GradientDrawable tBg = new GradientDrawable();
                tBg.setColor(t == 0  ?  0xDD1E2638 : 0x6612141C);
                tBg.setCornerRadius(dp(12));
                if (t == 0) tBg.setStroke(dp(1f), 0xFF00E5FF);
                tab.setBackground(tBg);
                LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(-2, -2);
                tLp.setMarginEnd(dp(6));
                tabs.addView(tab, tLp);
            }
            root.addView(tabs);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout gridContent = new LinearLayout(this);
        gridContent.setOrientation(LinearLayout.VERTICAL);

        LinearLayout currentRow = null;
        int inRow = 0;

        for (int i = 0; i < games.size(); i++) {
            final GameEntry entry = games.get(i);
            if (inRow == 0) {
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(-1, -2);
                rLp.bottomMargin = dp(8);
                gridContent.addView(currentRow, rLp);
            }

            int itemHeight = cardHeight;
            if (isMasonry) {
                itemHeight = (i % 3 == 0)  ?  cardHeight : ((i % 3 == 1)  ?  cardHeight + 35 : cardHeight - 15);
            }

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setPadding(dp(8), dp(8), dp(8), dp(8));

            GradientDrawable cBg = new GradientDrawable();
            cBg.setColor(0xDD141824);
            cBg.setCornerRadius(dp(14));
            cBg.setStroke(dp(1f), 0xFF2A334A);
            card.setBackground(cBg);

            ImageView icon = new ImageView(this);
            icon.setImageDrawable(getEntryIcon(entry));
            int iSize = dp(cols >= 5  ?  32 : (cols == 4  ?  40 : (isPoster  ?  56 : 48)));
            card.addView(icon, new LinearLayout.LayoutParams(iSize, iSize));

            TextView title = label(entry.label, cols >= 5  ?  9f : 10.5f, Color.WHITE);
            title.setTypeface(null, Typeface.BOLD);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            title.setGravity(Gravity.CENTER);
            title.setPadding(dp(4), dp(4), dp(4), 0);
            card.addView(title);

            if (cols <= 3 || isPoster) {
                TextView tag = label("\u26A1 PLAY", 8f, 0xFF00E5FF);
                tag.setTypeface(null, Typeface.BOLD);
                card.addView(tag);
            }

            card.setOnClickListener(v -> {
                try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
                launch(entry);
            });

            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0, dp(itemHeight), 1f);
            if (inRow > 0) cLp.setMarginStart(dp(8));
            currentRow.addView(card, cLp);
            inRow++;

            if (inRow == cols) {
                inRow = 0;
            }
        }

        if (inRow == 0) {
            currentRow = new LinearLayout(this);
            currentRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(-1, -2);
            rLp.bottomMargin = dp(8);
            gridContent.addView(currentRow, rLp);
        }

        LinearLayout addTile = new LinearLayout(this);
        addTile.setOrientation(LinearLayout.VERTICAL);
        addTile.setGravity(Gravity.CENTER);
        addTile.setPadding(dp(8), dp(8), dp(8), dp(8));
        GradientDrawable aBg = new GradientDrawable();
        aBg.setColor(0x881E2436);
        aBg.setCornerRadius(dp(14));
        aBg.setStroke(dp(1f), 0xFF3A4866);
        addTile.setBackground(aBg);
        TextView plus = label("+", 24f, 0xFF00E5FF);
        plus.setTypeface(null, Typeface.BOLD);
        addTile.addView(plus);
        TextView addLbl = label("ADD", 8.5f, Color.WHITE);
        addTile.addView(addLbl);
        addTile.setOnClickListener(v -> showAddGamesDialog());

        LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(0, dp(cardHeight), 1f);
        if (inRow > 0) aLp.setMarginStart(dp(8));
        currentRow.addView(addTile, aLp);
        inRow++;

        while (inRow < cols) {
            View spacer = new View(this);
            LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(0, 1, 1f);
            sLp.setMarginStart(dp(8));
            currentRow.addView(spacer, sLp);
            inRow++;
        }

        scroll.addView(gridContent, new FrameLayout.LayoutParams(-1, -2));
        root.addView(attachResponsivePanels(scroll, activeLayout, isPortrait), new LinearLayout.LayoutParams(-1, 0, 1f));

        root.addView(buildRORBottomNav(), new LinearLayout.LayoutParams(-1, dp(38)));
        return root;
    }

    private View buildSplitAndHeroLayout(int activeLayout, boolean isPortrait) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(6), dp(14), dp(6));
        root.setBackgroundColor(0x00000000);

        topBarView = buildRORHeader();
        root.addView(topBarView, new LinearLayout.LayoutParams(-1, -2));

        List<GameEntry> games = getActiveGameEntries();
        GameEntry focused = getActiveSelectedEntry();

        if (activeLayout == 23) {
            LinearLayout split = new LinearLayout(this);
            split.setOrientation(LinearLayout.HORIZONTAL);
            split.setPadding(0, dp(8), 0, dp(8));

            ScrollView railScroll = new ScrollView(this);
            railScroll.setVerticalScrollBarEnabled(false);
            LinearLayout rail = new LinearLayout(this);
            rail.setOrientation(LinearLayout.VERTICAL);

            for (int i = 0; i < games.size(); i++) {
                final GameEntry entry = games.get(i);
                boolean isSel = (focused != null && focused.pkg.equals(entry.pkg));

                LinearLayout item = new LinearLayout(this);
                item.setOrientation(LinearLayout.HORIZONTAL);
                item.setGravity(Gravity.CENTER_VERTICAL);
                item.setPadding(dp(10), dp(8), dp(10), dp(8));

                GradientDrawable iBg = new GradientDrawable();
                iBg.setColor(isSel  ?  0xDD2A334A : 0x66141824);
                iBg.setCornerRadius(dp(10));
                if (isSel) iBg.setStroke(dp(1.2f), 0xFF00E5FF);
                item.setBackground(iBg);

                ImageView icon = new ImageView(this);
                icon.setImageDrawable(getEntryIcon(entry));
                item.addView(icon, new LinearLayout.LayoutParams(dp(32), dp(32)));

                TextView title = label(entry.label, 10.5f, isSel  ?  Color.WHITE : 0xFFCCCCCC);
                title.setTypeface(null, Typeface.BOLD);
                title.setPadding(dp(8), 0, 0, 0);
                title.setSingleLine(true);
                title.setEllipsize(TextUtils.TruncateAt.END);
                item.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));

                if (isSel) {
                    TextView arrow = label("►", 11f, 0xFF00E5FF);
                    item.addView(arrow);
                }

                item.setOnClickListener(v -> {
                    try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
                    selectedEntry = entry;
                    rebuildView();
                });

                LinearLayout.LayoutParams iLp = new LinearLayout.LayoutParams(-1, -2);
                iLp.bottomMargin = dp(6);
                rail.addView(item, iLp);
            }
            railScroll.addView(rail, new FrameLayout.LayoutParams(-1, -2));
            split.addView(railScroll, new LinearLayout.LayoutParams(dp(230), -1));

            LinearLayout stage = new LinearLayout(this);
            stage.setOrientation(LinearLayout.VERTICAL);
            stage.setGravity(Gravity.CENTER);
            stage.setPadding(dp(20), dp(20), dp(20), dp(20));

            GradientDrawable sBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{0xEE1E2638, 0xEE101420});
            sBg.setCornerRadius(dp(18));
            sBg.setStroke(dp(1.5f), 0xFF00E5FF);
            stage.setBackground(sBg);

            ImageView bigIcon = new ImageView(this);
            bigIcon.setImageDrawable(getEntryIcon(focused));
            stage.addView(bigIcon, new LinearLayout.LayoutParams(dp(76), dp(76)));

            TextView sTitle = label(focused != null  ?  focused.label : "NO GAME", 18f, Color.WHITE);
            sTitle.setTypeface(null, Typeface.BOLD);
            sTitle.setPadding(0, dp(10), 0, dp(2));
            stage.addView(sTitle);

            TextView sPkg = label(focused != null  ?  focused.pkg : "", 9f, 0xFF888A96);
            stage.addView(sPkg);

            TextView play = label("  PLAY NOW  ", 12f, Color.WHITE);
            play.setTypeface(null, Typeface.BOLD);
            play.setPadding(dp(24), dp(10), dp(24), dp(10));
            GradientDrawable pBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0xFF00E5FF, 0xFF0088FF});
            pBg.setCornerRadius(dp(20));
            play.setBackground(pBg);
            play.setOnClickListener(v -> {
                try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
                if (focused != null) launch(focused);
            });
            LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(-2, -2);
            pLp.topMargin = dp(14);
            stage.addView(play, pLp);

            LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(0, -1, 1f);
            sLp.setMarginStart(dp(12));
            split.addView(stage, sLp);

            root.addView(attachResponsivePanels(split, activeLayout, isPortrait), new LinearLayout.LayoutParams(-1, 0, 1f));
        } else {
            LinearLayout hero = new LinearLayout(this);
            hero.setOrientation(LinearLayout.HORIZONTAL);
            hero.setGravity(Gravity.CENTER_VERTICAL);
            hero.setPadding(dp(16), dp(10), dp(16), dp(10));

            GradientDrawable hBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0xEE2A1E32, 0xEE161020});
            hBg.setCornerRadius(dp(14));
            hBg.setStroke(dp(1.2f), 0xFFFF2D55);
            hero.setBackground(hBg);

            ImageView hIcon = new ImageView(this);
            hIcon.setImageDrawable(getEntryIcon(focused));
            hero.addView(hIcon, new LinearLayout.LayoutParams(dp(48), dp(48)));

            LinearLayout hText = new LinearLayout(this);
            hText.setOrientation(LinearLayout.VERTICAL);
            hText.setPadding(dp(12), 0, 0, 0);

            TextView kicker = label("FEATURED GAME  \u2022  HIGH PERFORMANCE", 8.5f, 0xFFFF2D55);
            kicker.setTypeface(null, Typeface.BOLD);
            hText.addView(kicker);

            TextView title = label(focused != null  ?  focused.label : "NO GAMES", 14f, Color.WHITE);
            title.setTypeface(null, Typeface.BOLD);
            hText.addView(title);
            hero.addView(hText, new LinearLayout.LayoutParams(0, -2, 1f));

            TextView play = label("LAUNCH", 10.5f, Color.WHITE);
            play.setTypeface(null, Typeface.BOLD);
            play.setPadding(dp(18), dp(7), dp(18), dp(7));
            GradientDrawable pBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0xFFFF2D55, 0xFFFF5E3A});
            pBg.setCornerRadius(dp(14));
            play.setBackground(pBg);
            play.setOnClickListener(v -> {
                try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
                if (focused != null) launch(focused);
            });
            hero.addView(play);

            root.addView(hero, new LinearLayout.LayoutParams(-1, -2));

            ScrollView scroll = new ScrollView(this);
            scroll.setVerticalScrollBarEnabled(false);
            LinearLayout gridContent = new LinearLayout(this);
            gridContent.setOrientation(LinearLayout.VERTICAL);
            gridContent.setPadding(0, dp(10), 0, dp(6));

            int cols = (activeLayout == 10 || activeLayout == 30) ? 2 : (activeLayout == 29 ? 3 : 4);
            int shCardH = (activeLayout == 10 || activeLayout == 30) ? 116 : 88;
            LinearLayout currentRow = null;
            int inRow = 0;

            for (int i = 0; i < games.size(); i++) {
                final GameEntry entry = games.get(i);
                if (inRow == 0) {
                    currentRow = new LinearLayout(this);
                    currentRow.setOrientation(LinearLayout.HORIZONTAL);
                    LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(-1, -2);
                    rLp.bottomMargin = dp(8);
                    gridContent.addView(currentRow, rLp);
                }

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setGravity(Gravity.CENTER);
                card.setPadding(dp(6), dp(8), dp(6), dp(8));

                GradientDrawable cBg = new GradientDrawable();
                cBg.setColor(0xDD161B28);
                cBg.setCornerRadius(dp(12));
                cBg.setStroke(dp(1f), 0xFF2A344D);
                card.setBackground(cBg);

                ImageView icon = new ImageView(this);
                icon.setImageDrawable(getEntryIcon(entry));
                card.addView(icon, new LinearLayout.LayoutParams(dp(38), dp(38)));

                TextView gTitle = label(entry.label, 9.5f, Color.WHITE);
                gTitle.setTypeface(null, Typeface.BOLD);
                gTitle.setSingleLine(true);
                gTitle.setEllipsize(TextUtils.TruncateAt.END);
                gTitle.setPadding(0, dp(4), 0, 0);
                card.addView(gTitle);

                card.setOnClickListener(v -> {
                    try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
                    selectedEntry = entry;
                    rebuildView();
                });

                LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0, dp(shCardH), 1f);
                if (inRow > 0) cLp.setMarginStart(dp(8));
                currentRow.addView(card, cLp);
                inRow++;

                if (inRow == cols) inRow = 0;
            }

            if (inRow > 0) {
                while (inRow < cols) {
                    View spacer = new View(this);
                    LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(0, 1, 1f);
                    sLp.setMarginStart(dp(8));
                    currentRow.addView(spacer, sLp);
                    inRow++;
                }
            }

            scroll.addView(gridContent, new FrameLayout.LayoutParams(-1, -2));
            root.addView(attachResponsivePanels(scroll, activeLayout, isPortrait), new LinearLayout.LayoutParams(-1, 0, 1f));
        }

        root.addView(buildRORBottomNav(), new LinearLayout.LayoutParams(-1, dp(38)));
        return root;
    }

    private View buildHexagonGridLayout(boolean isPortrait) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(6), dp(14), dp(6));
        root.setBackgroundColor(0x00000000);

        topBarView = buildRORHeader();
        root.addView(topBarView, new LinearLayout.LayoutParams(-1, -2));

        List<GameEntry> games = getActiveGameEntries();

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout honeyContent = new LinearLayout(this);
        honeyContent.setOrientation(LinearLayout.VERTICAL);
        honeyContent.setPadding(0, dp(10), 0, dp(10));

        int hexCols = isPortrait  ?  3 : 5;
        LinearLayout currentRow = null;
        int inRow = 0;
        int rowIndex = 0;

        for (int i = 0; i < games.size(); i++) {
            final GameEntry entry = games.get(i);
            if (inRow == 0) {
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                currentRow.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(-1, -2);
                rLp.bottomMargin = dp(4);
                if (rowIndex % 2 == 1) {
                    rLp.setMarginStart(dp(18));
                }
                honeyContent.addView(currentRow, rLp);
            }

            LinearLayout hexCard = new LinearLayout(this);
            hexCard.setOrientation(LinearLayout.VERTICAL);
            hexCard.setGravity(Gravity.CENTER);
            hexCard.setPadding(dp(8), dp(10), dp(8), dp(10));

            GradientDrawable hBg = new GradientDrawable();
            hBg.setColor(0xDD121A28);
            hBg.setCornerRadius(dp(22));
            hBg.setStroke(dp(1.5f), 0xFF00E5FF);
            hexCard.setBackground(hBg);

            ImageView icon = new ImageView(this);
            icon.setImageDrawable(getEntryIcon(entry));
            hexCard.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(40)));

            TextView title = label(entry.label, 9.5f, Color.WHITE);
            title.setTypeface(null, Typeface.BOLD);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            title.setPadding(0, dp(4), 0, 0);
            hexCard.addView(title);

            hexCard.setOnClickListener(v -> {
                try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
                launch(entry);
            });

            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(dp(95), dp(95));
            cLp.setMargins(dp(4), dp(4), dp(4), dp(4));
            currentRow.addView(hexCard, cLp);
            inRow++;

            if (inRow == hexCols) {
                inRow = 0;
                rowIndex++;
            }
        }

        scroll.addView(honeyContent, new FrameLayout.LayoutParams(-1, -2));
        root.addView(attachResponsivePanels(scroll, 33, isPortrait), new LinearLayout.LayoutParams(-1, 0, 1f));

        root.addView(buildRORBottomNav(), new LinearLayout.LayoutParams(-1, dp(38)));
        return root;
    }

    private View buildMinimalStreamLayout(int activeLayout, boolean isPortrait) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(6), dp(14), dp(6));
        root.setBackgroundColor(0x00000000);

        topBarView = buildRORHeader();
        root.addView(topBarView, new LinearLayout.LayoutParams(-1, -2));

        List<GameEntry> games = getActiveGameEntries();

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(isPortrait  ?  8 : 40), dp(10), dp(isPortrait  ?  8 : 40), dp(10));

        int streamAccent = (activeLayout == 6) ? 0xFF00BFA5 : ((activeLayout == 7) ? 0xFFFF1744 : ((activeLayout == 9) ? 0xFFFFD600 : 0xFF2979FF));
        String streamBadge = (activeLayout == 6) ? "HYPERBOOST 120FPS" : ((activeLayout == 7) ? "PRO TOURNAMENT" : ((activeLayout == 9) ? "VELOCITY GT" : "PRO PERFORMANCE"));

        for (int i = 0; i < games.size(); i++) {
            final GameEntry entry = games.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(10), dp(16), dp(10));

            GradientDrawable rBg = new GradientDrawable();
            rBg.setColor(0xDD121622);
            rBg.setCornerRadius(dp(14));
            rBg.setStroke(dp(1.2f), streamAccent);
            row.setBackground(rBg);

            ImageView icon = new ImageView(this);
            icon.setImageDrawable(getEntryIcon(entry));
            row.addView(icon, new LinearLayout.LayoutParams(dp(44), dp(44)));

            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setPadding(dp(12), 0, 0, 0);

            TextView title = label(entry.label, 12f, Color.WHITE);
            title.setTypeface(null, Typeface.BOLD);
            textCol.addView(title);

            TextView sub = label(entry.pkg + "  \u2022  " + streamBadge, 8.5f, streamAccent);
            textCol.addView(sub);
            row.addView(textCol, new LinearLayout.LayoutParams(0, -2, 1f));

            TextView play = label("PLAY", 10.5f, 0xFF00E5FF);
            play.setTypeface(null, Typeface.BOLD);
            play.setPadding(dp(16), dp(6), dp(16), dp(6));
            GradientDrawable pBg = new GradientDrawable();
            pBg.setColor(0x2200E5FF);
            pBg.setCornerRadius(dp(10));
            pBg.setStroke(dp(1f), 0xFF00E5FF);
            play.setBackground(pBg);
            row.addView(play);

            row.setOnClickListener(v -> {
                try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
                launch(entry);
            });

            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(-1, -2);
            rLp.bottomMargin = dp(8);
            list.addView(row, rLp);
        }

        scroll.addView(list, new FrameLayout.LayoutParams(-1, -2));
        root.addView(attachResponsivePanels(scroll, activeLayout, isPortrait), new LinearLayout.LayoutParams(-1, 0, 1f));

        root.addView(buildRORBottomNav(), new LinearLayout.LayoutParams(-1, dp(38)));
        return root;
    }

        public static class RgbBorderFrameView extends View {
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF bounds = new RectF();
        private boolean isRgbActive = true;
        private boolean isCrimsonRed = false;
        private float phase = 0f;
        private ValueAnimator animator;

        public RgbBorderFrameView(Context context, boolean rgbActive) {
            this(context, rgbActive, false);
        }

        public RgbBorderFrameView(Context context, boolean rgbActive, boolean crimsonRed) {
            super(context);
            this.isRgbActive = rgbActive;
            this.isCrimsonRed = crimsonRed;
            setClickable(false);
            setFocusable(false);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(dpToPx(context, 2.5f));
            glowPaint.setStyle(Paint.Style.STROKE);
            glowPaint.setStrokeWidth(dpToPx(context, 7.0f));

            if (rgbActive) {
                animator = ValueAnimator.ofFloat(0f, 1f);
                animator.setDuration(3500);
                animator.setRepeatCount(ValueAnimator.INFINITE);
                animator.setInterpolator(new LinearInterpolator());
                animator.addUpdateListener(a -> {
                    phase = (float) a.getAnimatedValue();
                    invalidate();
                });
                animator.start();
            }
        }

        private static float dpToPx(Context c, float dp) {
            return dp * c.getResources().getDisplayMetrics().density;
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if (animator != null) animator.cancel();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            float inset = borderPaint.getStrokeWidth() / 2f;
            bounds.set(inset, inset, w - inset, h - inset);

            if (isRgbActive) {
                int[] rgbColors = isCrimsonRed ? new int[]{
                    0xFFFF1744, 0xFFFF5252, 0xFFD50000, 0xFFFF1744, 0xFFFF5252, 0xFF8B0000, 0xFFFF1744
                } : new int[]{
                    Color.HSVToColor(new float[]{(phase * 360f) % 360f, 0.9f, 1.0f}),
                    Color.HSVToColor(new float[]{(phase * 360f + 60f) % 360f, 0.9f, 1.0f}),
                    Color.HSVToColor(new float[]{(phase * 360f + 120f) % 360f, 0.9f, 1.0f}),
                    Color.HSVToColor(new float[]{(phase * 360f + 180f) % 360f, 0.9f, 1.0f}),
                    Color.HSVToColor(new float[]{(phase * 360f + 240f) % 360f, 0.9f, 1.0f}),
                    Color.HSVToColor(new float[]{(phase * 360f + 300f) % 360f, 0.9f, 1.0f}),
                    Color.HSVToColor(new float[]{(phase * 360f) % 360f, 0.9f, 1.0f})
                };
                LinearGradient grad = new LinearGradient(0, 0, w, h, rgbColors, null, Shader.TileMode.CLAMP);
                borderPaint.setShader(grad);
                glowPaint.setShader(grad);
                glowPaint.setAlpha(90);

                float radius = dpToPx(getContext(), 10f);
                canvas.drawRoundRect(bounds, radius, radius, glowPaint);
                canvas.drawRoundRect(bounds, radius, radius, borderPaint);
            } else {
                borderPaint.setShader(null);
                borderPaint.setColor(0x2A3E4E77);
                float radius = dpToPx(getContext(), 10f);
                canvas.drawRoundRect(bounds, radius, radius, borderPaint);
            }
        }
    }

    /** A subtle, always-visible accent rail shared by every Game Space layout. */
    public static class LayoutAccentFrameView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF bounds = new RectF();

        public LayoutAccentFrameView(Context context, int accentColor) {
            super(context);
            setClickable(false);
            setFocusable(false);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dpToPx(context, 1.5f));
            paint.setColor(accentColor);
            paint.setAlpha(210);
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        private static float dpToPx(Context context, float value) {
            return value * context.getResources().getDisplayMetrics().density;
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float inset = paint.getStrokeWidth() / 2f;
            bounds.set(inset, inset, getWidth() - inset, getHeight() - inset);
            canvas.drawRoundRect(bounds, dpToPx(getContext(), 10f), dpToPx(getContext(), 10f), paint);
            paint.setStrokeWidth(dpToPx(getContext(), 3f));
            canvas.drawLine(dpToPx(getContext(), 20f), inset,
                    Math.max(dpToPx(getContext(), 20f), getWidth() - dpToPx(getContext(), 20f)), inset, paint);
        }
    }

    public static class AnimatedGamingCircleView extends View {
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint innerArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF ovalOuter = new RectF();
        private final RectF ovalInner = new RectF();
        private float rotationAngle = 0f;
        private float energySurge = 1.0f;
        private ValueAnimator loopAnimator;
        private boolean rgbChromaActive = true;

        public AnimatedGamingCircleView(Context context) {
            super(context);
            init();
        }

        public void setRgbChromaActive(boolean active) {
            this.rgbChromaActive = active;
            invalidate();
        }

        private void init() {
            try {
                SharedPreferences pl = getContext().getSharedPreferences("ror_game_plugins", Context.MODE_PRIVATE);
                rgbChromaActive = pl.getBoolean("rainbow_rgb", true);
            } catch (Exception ignored) {}
            glowPaint.setStyle(Paint.Style.FILL);

            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(dp(1.2f));
            ringPaint.setColor(0x35FF1744);

            arcPaint.setStyle(Paint.Style.STROKE);
            arcPaint.setStrokeWidth(dp(2.4f));
            arcPaint.setStrokeCap(Paint.Cap.ROUND);
            arcPaint.setColor(0xFFFF1744);

            tickPaint.setStyle(Paint.Style.STROKE);
            tickPaint.setStrokeWidth(dp(1.5f));
            tickPaint.setStrokeCap(Paint.Cap.ROUND);
            tickPaint.setColor(0xAAFF5252);

            innerArcPaint.setStyle(Paint.Style.STROKE);
            innerArcPaint.setStrokeWidth(dp(2.0f));
            innerArcPaint.setStrokeCap(Paint.Cap.ROUND);
            innerArcPaint.setColor(0xFFFF5252);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            startLoopAnimation();
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if (loopAnimator != null) {
                loopAnimator.cancel();
                loopAnimator = null;
            }
        }

        public void startLoopAnimation() {
            if (loopAnimator != null) return;
            loopAnimator = ValueAnimator.ofFloat(0f, 1f);
            loopAnimator.setDuration(9000);
            loopAnimator.setRepeatCount(ValueAnimator.INFINITE);
            loopAnimator.setInterpolator(new LinearInterpolator());
            loopAnimator.addUpdateListener(animation -> {
                rotationAngle = (float) animation.getAnimatedValue() * 360f;
                invalidate();
            });
            loopAnimator.start();
        }

        public void triggerEnergySurge() {
            ValueAnimator surge = ValueAnimator.ofFloat(1.35f, 1.0f);
            surge.setDuration(450);
            surge.setInterpolator(new DecelerateInterpolator());
            surge.addUpdateListener(a -> {
                energySurge = (float) a.getAnimatedValue();
                invalidate();
            });
            surge.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            float cx = w / 2f;
            float cy = h / 2f;
            float baseRadius = Math.min(w, h) / 2f - dp(3);
            float r = baseRadius * energySurge;

            int glowC1 = 0x4DFF1744;
            int glowC2 = 0x1AFF1744;
            if (rgbChromaActive) {
                int c1 = Color.HSVToColor(new float[]{rotationAngle % 360f, 0.95f, 1.0f});
                int c2 = Color.HSVToColor(new float[]{(rotationAngle + 120f) % 360f, 0.95f, 1.0f});
                int c3 = Color.HSVToColor(new float[]{(rotationAngle + 240f) % 360f, 0.95f, 1.0f});
                arcPaint.setColor(c1);
                tickPaint.setColor(c2);
                innerArcPaint.setColor(c3);
                ringPaint.setColor((0x33 << 24) | (c1 & 0xFFFFFF));
                glowC1 = (0x44 << 24) | (c1 & 0xFFFFFF);
                glowC2 = (0x15 << 24) | (c2 & 0xFFFFFF);
            } else {
                arcPaint.setColor(0xFFFF1744);
                tickPaint.setColor(0xAAFF5252);
                innerArcPaint.setColor(0xFFFF5252);
                ringPaint.setColor(0x35FF1744);
            }

            // 1. Soft pulsing radial energy aura in crimson or RGB Chroma shades
            RadialGradient energyGrad = new RadialGradient(cx, cy, r,
                    new int[]{glowC1, glowC2, 0x00000000},
                    new float[]{0f, 0.65f, 1f}, Shader.TileMode.CLAMP);
            glowPaint.setShader(energyGrad);
            canvas.drawCircle(cx, cy, r, glowPaint);

            // 2. Outer continuous guideline track
            canvas.drawCircle(cx, cy, r * 0.94f, ringPaint);
            canvas.drawCircle(cx, cy, r * 0.76f, ringPaint);

            // 3. Four rotating cyber arcs (outer ring, clockwise)
            ovalOuter.set(cx - r * 0.94f, cy - r * 0.94f, cx + r * 0.94f, cy + r * 0.94f);
            for (int i = 0; i < 4; i++) {
                float startAngle = rotationAngle + (i * 90f);
                canvas.drawArc(ovalOuter, startAngle, 52f, false, arcPaint);
            }

            // 4. Tech tick marks (middle ring, rotating counter-clockwise)
            float tickR1 = r * 0.81f;
            float tickR2 = r * 0.88f;
            int tickCount = 18;
            float counterAngle = -rotationAngle * 0.65f;
            for (int i = 0; i < tickCount; i++) {
                float theta = (float) Math.toRadians(counterAngle + (i * (360f / tickCount)));
                float cos = (float) Math.cos(theta);
                float sin = (float) Math.sin(theta);
                canvas.drawLine(cx + tickR1 * cos, cy + tickR1 * sin,
                        cx + tickR2 * cos, cy + tickR2 * sin, tickPaint);
            }

            // 5. Three luminous accent cyber brackets (inner ring, accelerated rotation)
            ovalInner.set(cx - r * 0.76f, cy - r * 0.76f, cx + r * 0.76f, cy + r * 0.76f);
            float innerAngle = rotationAngle * 1.5f;
            for (int i = 0; i < 3; i++) {
                float s = innerAngle + (i * 120f);
                canvas.drawArc(ovalInner, s, 36f, false, innerArcPaint);
            }
        }

        private float dp(float val) {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, val, getResources().getDisplayMetrics());
        }
    }

    private static class CyberLaserBackgroundView extends View {
        private final Paint laserPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint lightningPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint lightningGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path lightningPath1 = new Path();
        private final Path lightningPath2 = new Path();

        public CyberLaserBackgroundView(Context context) {
            super(context);
            laserPaint.setStyle(Paint.Style.STROKE);
            glowPaint.setStyle(Paint.Style.STROKE);
            lightningPaint.setStyle(Paint.Style.STROKE);
            lightningPaint.setStrokeCap(Paint.Cap.ROUND);
            lightningPaint.setStrokeJoin(Paint.Join.MITER);
            lightningGlow.setStyle(Paint.Style.STROKE);
            lightningGlow.setStrokeCap(Paint.Cap.ROUND);
            lightningGlow.setStrokeJoin(Paint.Join.MITER);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            // 1. Dark Cyber Studio background gradient
            LinearGradient bgGrad = new LinearGradient(0, 0, w, h,
                    new int[]{0xFF07070B, 0xFF0E0E18, 0xFF140B1A, 0xFF08060E},
                    new float[]{0f, 0.35f, 0.75f, 1f}, Shader.TileMode.CLAMP);
            bgPaint.setShader(bgGrad);
            canvas.drawRect(0, 0, w, h, bgPaint);

            // 2. Cyan Laser Beams on Left
            float originX = w * 0.15f;
            float originY = h * 1.05f;

            float[][] targets = {
                    {w * 0.22f, 0f},
                    {w * 0.35f, 0f},
                    {w * 0.48f, 0f},
                    {w * 0.60f, 0f},
                    {w * 0.05f, 0f}
            };

            for (float[] target : targets) {
                // Wide outer glow
                glowPaint.setColor(0x2800E5FF);
                glowPaint.setStrokeWidth(dp(10));
                canvas.drawLine(originX, originY, target[0], target[1], glowPaint);

                // Mid glow
                glowPaint.setColor(0x6600E5FF);
                glowPaint.setStrokeWidth(dp(4));
                canvas.drawLine(originX, originY, target[0], target[1], glowPaint);

                // Core laser
                laserPaint.setColor(0xFFE0F7FA);
                laserPaint.setStrokeWidth(dp(1.8f));
                canvas.drawLine(originX, originY, target[0], target[1], laserPaint);
            }

            // 3. Neon Magenta Zigzag Lightning Lasers on Right
            lightningPath1.reset();
            lightningPath1.moveTo(w * 0.72f, 0f);
            lightningPath1.lineTo(w * 0.63f, h * 0.28f);
            lightningPath1.lineTo(w * 0.85f, h * 0.44f);
            lightningPath1.lineTo(w * 0.73f, h * 0.68f);
            lightningPath1.lineTo(w * 0.94f, h * 0.82f);
            lightningPath1.lineTo(w * 0.82f, h * 1.02f);

            lightningGlow.setColor(0x35FF007F);
            lightningGlow.setStrokeWidth(dp(14));
            canvas.drawPath(lightningPath1, lightningGlow);

            lightningGlow.setColor(0x70FF007F);
            lightningGlow.setStrokeWidth(dp(5));
            canvas.drawPath(lightningPath1, lightningGlow);

            lightningPaint.setColor(0xFFFF80AB);
            lightningPaint.setStrokeWidth(dp(2f));
            canvas.drawPath(lightningPath1, lightningPaint);

            lightningPath2.reset();
            lightningPath2.moveTo(w * 0.88f, 0f);
            lightningPath2.lineTo(w * 0.80f, h * 0.22f);
            lightningPath2.lineTo(w * 0.96f, h * 0.40f);
            lightningPath2.lineTo(w * 0.86f, h * 0.58f);

            lightningGlow.setColor(0x22AA00FF);
            lightningGlow.setStrokeWidth(dp(8));
            canvas.drawPath(lightningPath2, lightningGlow);

            lightningPaint.setColor(0xFFEA80FC);
            lightningPaint.setStrokeWidth(dp(1.5f));
            canvas.drawPath(lightningPath2, lightningPaint);

            // 4. Floor horizon reflection glow
            Paint floorGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
            LinearGradient fGrad = new LinearGradient(0, h * 0.75f, 0, h,
                    new int[]{0x00000000, 0x30FF007F, 0x1500E5FF}, null, Shader.TileMode.CLAMP);
            floorGlow.setShader(fGrad);
            canvas.drawRect(0, h * 0.75f, w, h, floorGlow);
        }

        private float dp(float val) {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, val, getResources().getDisplayMetrics());
        }
    }

    private View createCyberBentoCard(GameEntry entry, boolean isWide, int cardHeight) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        boolean isCompact = (dm.heightPixels / dm.density) < 460;
        int cardWidth = isWide  ?  dp(isCompact  ?  190 : 228) : dp(isCompact  ?  88 : 106);

        FrameLayout card = new FrameLayout(this);
        boolean isFocused = (selectedEntry != null && selectedEntry.pkg.equals(entry.pkg));

        int bgPrimary = 0xFF1B1D25;
        int bgSecondary = 0xFF101217;
        String lk = entry.label.toLowerCase(Locale.US);
        if (lk.contains("duty")) {
            bgPrimary = 0xFF232733; bgSecondary = 0xFF141720;
        } else if (lk.contains("zarchiver")) {
            bgPrimary = 0xFF2E7D32; bgSecondary = 0xFF1B5E20;
        } else if (lk.contains("nethersx2") || lk.contains("sx2")) {
            bgPrimary = 0xFFB71C1C; bgSecondary = 0xFF7F0000;
        } else if (lk.contains("ggpo")) {
            bgPrimary = 0xFF1565C0; bgSecondary = 0xFF0D47A1;
        } else if (lk.contains("rune") || lk.contains("wild") || lk.contains("league")) {
            bgPrimary = 0xFF283593; bgSecondary = 0xFF1A237E;
        }

        GradientDrawable cardBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{bgPrimary, bgSecondary});
        cardBg.setCornerRadius(dp(14));
        cardBg.setStroke(dp(isFocused  ?  2 : 1), isFocused  ?  0xFFFF1744 : 0x33FFFFFF);
        card.setBackground(cardBg);

        // Watermark uses a real vector icon instead of emoji glyphs.
        TextView watermark = new TextView(this);
        watermark.setText("REVROR CONTROLLER");
        applyLeadingIcon(watermark, R.drawable.ic_console, 0x14FFFFFF, isWide  ?  28 : 20);
        watermark.setTextSize(isWide  ?  22 : 16);
        watermark.setTextColor(0x0AFFFFFF);
        watermark.setTypeface(null, Typeface.BOLD);
        watermark.setGravity(Gravity.CENTER);
        card.addView(watermark, new FrameLayout.LayoutParams(-1, -1));

        if (isWide) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(8), dp(12), dp(8));

            FrameLayout iconFrame = new FrameLayout(this);
            GradientDrawable iBg = new GradientDrawable();
            iBg.setColor(0x33000000);
            iBg.setCornerRadius(dp(10));
            iBg.setStroke(dp(1), 0x33FFFFFF);
            iconFrame.setBackground(iBg);

            ImageView iconView = new ImageView(this);
            iconView.setImageDrawable(entry.resolve.loadIcon(getPackageManager()));
            iconFrame.addView(iconView, new FrameLayout.LayoutParams(dp(38), dp(38), Gravity.CENTER));
            row.addView(iconFrame, new LinearLayout.LayoutParams(dp(44), dp(44)));

            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setPadding(dp(10), 0, 0, 0);

            TextView title = label(entry.label, isCompact  ?  11f : 12.5f, Color.WHITE);
            title.setTypeface(null, Typeface.BOLD);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            textCol.addView(title);

            TextView data = label(formatPlayTime(entry.pkg) + " • " + getAppStorageSize(entry.pkg), 8f, 0xBBFFFFFF);
            data.setPadding(0, dp(2), 0, 0);
            textCol.addView(data);

            row.addView(textCol, new LinearLayout.LayoutParams(0, -2, 1f));
            card.addView(row, new FrameLayout.LayoutParams(-1, -1));
        } else {
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER);
            col.setPadding(dp(6), dp(6), dp(6), dp(6));

            ImageView iconView = new ImageView(this);
            iconView.setImageDrawable(entry.resolve.loadIcon(getPackageManager()));
            col.addView(iconView, new LinearLayout.LayoutParams(dp(38), dp(38)));

            TextView title = label(entry.label, isCompact  ?  9f : 10f, Color.WHITE);
            title.setTypeface(null, Typeface.BOLD);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            title.setGravity(Gravity.CENTER);
            title.setPadding(0, dp(4), 0, 0);
            col.addView(title);

            card.addView(col, new FrameLayout.LayoutParams(-1, -1));
        }

        if (isFocused) {
            startLiveCardAnimation(card);
        }

        card.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            if (selectedEntry != null && selectedEntry.pkg.equals(entry.pkg)) {
                prepareLaunch(entry);
            } else {
                selectedEntry = entry;
                rebuildView();
            }
        });

        return card;
    }

    private View buildRORCyberLaserGridHome() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float heightDp = dm.heightPixels / dm.density;
        boolean isCompact = heightDp < 460;

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0x00000000);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(isCompact  ?  14 : 20), dp(isCompact  ?  8 : 12), dp(isCompact  ?  14 : 20), dp(isCompact  ?  6 : 8));

        // 1. Top Bar
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        View backToDash = createCyberPillBackButton("DASHBOARD", v -> finish());
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(-2, -2);
        backLp.setMarginEnd(dp(10));
        topBar.addView(backToDash, backLp);

        TextView logo = label("ROR", isCompact  ?  14f : 16f, Color.WHITE);
        logo.setTypeface(null, Typeface.BOLD);
        logo.setLetterSpacing(0.20f);
        topBar.addView(logo);

        cyberClockView = label(" " + cyberTimeFormat.format(new Date()), isCompact  ?  11.5f : 13f, 0xFFE0E0E6);
        cyberClockView.setTypeface(null, Typeface.BOLD);
        cyberClockView.setPadding(dp(12), 0, dp(10), 0);
        topBar.addView(cyberClockView);

        LinearLayout batPill = new LinearLayout(this);
        batPill.setOrientation(LinearLayout.HORIZONTAL);
        batPill.setGravity(Gravity.CENTER_VERTICAL);
        batPill.setPadding(dp(8), dp(3), dp(10), dp(3));
        GradientDrawable batBg = new GradientDrawable();
        batBg.setColor(0xCC1A2030);
        batBg.setCornerRadius(dp(10));
        batBg.setStroke(dp(1), 0xFF2C3B55);
        batPill.setBackground(batBg);

        ImageView batIcon = new ImageView(this);
        batIcon.setImageResource(R.drawable.ic_battery);
        DrawableCompat.setTint(batIcon.getDrawable(), 0xFF76FF03);
        batPill.addView(batIcon, new LinearLayout.LayoutParams(dp(14), dp(14)));

        int batteryLevel = 73;
        try {
            BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
            if (bm != null) batteryLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        } catch (Exception ignored) {}
        cyberBatteryPill = label(" " + batteryLevel + "%", 9.5f, Color.WHITE);
        cyberBatteryPill.setTypeface(null, Typeface.BOLD);
        batPill.addView(cyberBatteryPill);
        topBar.addView(batPill);

        FrameLayout chibiBadge = new FrameLayout(this);
        GradientDrawable chBg = new GradientDrawable();
        chBg.setColor(0xDD251A32);
        chBg.setCornerRadius(dp(10));
        chBg.setStroke(dp(1.5f), 0xFFFF007F);
        chibiBadge.setBackground(chBg);
        ImageView chibiImg = new ImageView(this);
        chibiImg.setImageResource(R.drawable.ic_barrage_chibi);
        chibiBadge.addView(chibiImg, new FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER));
        LinearLayout.LayoutParams chLp = new LinearLayout.LayoutParams(dp(32), dp(32));
        chLp.setMarginStart(dp(10));
        topBar.addView(chibiBadge, chLp);

        cyberPingPill = label(" " + (currentPingMs > 0  ?  currentPingMs + " ms" : "-- ms"), 9.5f,
                (currentPingMs > 0 && currentPingMs < 60)  ?  0xFF00E676 : (currentPingMs < 120  ?  0xFFFFD600 : 0xFFFF1744));
        cyberPingPill.setTypeface(null, Typeface.BOLD);
        cyberPingPill.setPadding(dp(8), dp(3), dp(8), dp(3));
        GradientDrawable pBg = new GradientDrawable();
        pBg.setColor(0xCC161824);
        pBg.setCornerRadius(dp(10));
        pBg.setStroke(dp(1), 0xFF2A2E44);
        cyberPingPill.setBackground(pBg);
        LinearLayout.LayoutParams pingLp = new LinearLayout.LayoutParams(-2, -2);
        pingLp.setMarginStart(dp(8));
        topBar.addView(cyberPingPill, pingLp);

        View spacer = new View(this);
        topBar.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        ImageView gamepadIcon = new ImageView(this);
        gamepadIcon.setImageResource(R.drawable.ic_revror_qs_gaming);
        gamepadIcon.setPadding(dp(6), dp(6), dp(6), dp(6));
        topBar.addView(gamepadIcon, new LinearLayout.LayoutParams(dp(32), dp(32)));

        ImageView switchIcon = new ImageView(this);
        switchIcon.setImageResource(R.drawable.ic_switch);
        switchIcon.setPadding(dp(6), dp(6), dp(6), dp(6));
        switchIcon.setOnClickListener(v -> showLayoutChooser());
        topBar.addView(switchIcon, new LinearLayout.LayoutParams(dp(32), dp(32)));

        ImageView centerIcon = new ImageView(this);
        centerIcon.setImageResource(R.drawable.ic_settings_icon);
        centerIcon.setPadding(dp(6), dp(6), dp(6), dp(6));
        centerIcon.setOnClickListener(v -> showGameSpaceCenterDialog());
        topBar.addView(centerIcon, new LinearLayout.LayoutParams(dp(32), dp(32)));

        content.addView(topBar, new LinearLayout.LayoutParams(-1, dp(isCompact  ?  38 : 46)));

        // 2. Bento Grid of Games
        HorizontalScrollView hScroll = new HorizontalScrollView(this);
        hScroll.setFillViewport(true);
        hScroll.setHorizontalScrollBarEnabled(false);
        hScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout bentoGrid = new LinearLayout(this);
        bentoGrid.setOrientation(LinearLayout.HORIZONTAL);
        bentoGrid.setPadding(0, dp(isCompact  ?  6 : 10), 0, dp(isCompact  ?  6 : 10));

        int cardHeight = dp(isCompact  ?  86 : 102);

        if (displayedEntries.isEmpty()) {
            for (GameEntry e : allEntries) {
                if (e.game || showAllApps) displayedEntries.add(e);
            }
        }

        if (selectedEntry == null && !displayedEntries.isEmpty()) {
            selectedEntry = displayedEntries.get(0);
        }

        LinearLayout col0 = new LinearLayout(this);
        col0.setOrientation(LinearLayout.VERTICAL);

        if (!displayedEntries.isEmpty()) {
            View topCard = createCyberBentoCard(displayedEntries.get(0), true, cardHeight);
            col0.addView(topCard, new LinearLayout.LayoutParams(-2, cardHeight));
        } else {
            LinearLayout emptyCard = new LinearLayout(this);
            emptyCard.setOrientation(LinearLayout.HORIZONTAL);
            emptyCard.setGravity(Gravity.CENTER_VERTICAL);
            emptyCard.setPadding(dp(16), dp(10), dp(20), dp(10));
            GradientDrawable eBg = new GradientDrawable();
            eBg.setColor(0xDD181E2E);
            eBg.setCornerRadius(dp(16));
            eBg.setStroke(dp(1.5f), 0xFFFF2D55);
            emptyCard.setBackground(eBg);

            ImageView rorIcon = new ImageView(this);
            rorIcon.setImageResource(R.mipmap.ic_launcher);
            emptyCard.addView(rorIcon, new LinearLayout.LayoutParams(dp(48), dp(48)));

            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setPadding(dp(12), 0, 0, 0);

            TextView emptyTitle = label("NO GAMES ADDED", 12f, Color.WHITE);
            emptyTitle.setTypeface(null, Typeface.BOLD);
            textCol.addView(emptyTitle);

            TextView emptySub = label("Tap here or '+' to add your games", 9f, 0xFFFF6E78);
            textCol.addView(emptySub);
            emptyCard.addView(textCol);

            emptyCard.setOnClickListener(v -> {
                try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
                showAddGamesDialog();
            });
            int topWidth = dp(isCompact  ?  230 : 280);
            col0.addView(emptyCard, new LinearLayout.LayoutParams(topWidth, cardHeight));
        }

        LinearLayout bottomPair0 = new LinearLayout(this);
        bottomPair0.setOrientation(LinearLayout.HORIZONTAL);
        bottomPair0.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout addTile = new FrameLayout(this);
        GradientDrawable addBg = new GradientDrawable();
        addBg.setColor(0xCC202538);
        addBg.setCornerRadius(dp(14));
        addBg.setStroke(dp(1.5f), 0xFF3E4868);
        addTile.setBackground(addBg);

        TextView plus = label("+", 26, Color.WHITE);
        plus.setTypeface(null, Typeface.BOLD);
        plus.setGravity(Gravity.CENTER);
        addTile.addView(plus, new FrameLayout.LayoutParams(-1, -1));

        addTile.setOnClickListener(v -> showAddGamesDialog());
        int addWidth = dp(isCompact  ?  76 : 92);
        bottomPair0.addView(addTile, new LinearLayout.LayoutParams(addWidth, cardHeight));

        if (displayedEntries.size() > 1) {
            View botCard = createCyberBentoCard(displayedEntries.get(1), false, cardHeight);
            LinearLayout.LayoutParams bcLp = new LinearLayout.LayoutParams(-2, cardHeight);
            bcLp.setMarginStart(dp(8));
            bottomPair0.addView(botCard, bcLp);
        }
        LinearLayout.LayoutParams bpLp = new LinearLayout.LayoutParams(-2, cardHeight);
        bpLp.topMargin = dp(8);
        col0.addView(bottomPair0, bpLp);

        bentoGrid.addView(col0, new LinearLayout.LayoutParams(-2, -2));

        int idx = 2;
        int colNum = 1;
        while (idx < displayedEntries.size()) {
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(-2, -2);
            cLp.setMarginStart(dp(8));

            boolean topWide = (colNum % 2 == 1);
            View topC = createCyberBentoCard(displayedEntries.get(idx++), topWide, cardHeight);
            col.addView(topC, new LinearLayout.LayoutParams(-2, cardHeight));

            if (idx < displayedEntries.size()) {
                if (topWide) {
                    LinearLayout botRow = new LinearLayout(this);
                    botRow.setOrientation(LinearLayout.HORIZONTAL);
                    View bot1 = createCyberBentoCard(displayedEntries.get(idx++), false, cardHeight);
                    botRow.addView(bot1, new LinearLayout.LayoutParams(-2, cardHeight));
                    if (idx < displayedEntries.size()) {
                        View bot2 = createCyberBentoCard(displayedEntries.get(idx++), false, cardHeight);
                        LinearLayout.LayoutParams b2Lp = new LinearLayout.LayoutParams(-2, cardHeight);
                        b2Lp.setMarginStart(dp(8));
                        botRow.addView(bot2, b2Lp);
                    }
                    LinearLayout.LayoutParams brLp = new LinearLayout.LayoutParams(-2, cardHeight);
                    brLp.topMargin = dp(8);
                    col.addView(botRow, brLp);
                } else {
                    View botC = createCyberBentoCard(displayedEntries.get(idx++), true, cardHeight);
                    LinearLayout.LayoutParams bcLp = new LinearLayout.LayoutParams(-2, cardHeight);
                    bcLp.topMargin = dp(8);
                    col.addView(botC, bcLp);
                }
            }

            bentoGrid.addView(col, cLp);
            colNum++;
        }

        hScroll.addView(bentoGrid, new FrameLayout.LayoutParams(-2, -1));
        content.addView(hScroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        // 3. Bottom Centered Pill Dock & Hex Tools
        FrameLayout bottomDockArea = new FrameLayout(this);

        LinearLayout pillDock = new LinearLayout(this);
        pillDock.setOrientation(LinearLayout.HORIZONTAL);
        pillDock.setGravity(Gravity.CENTER);
        pillDock.setPadding(dp(3), dp(3), dp(3), dp(3));
        GradientDrawable dBg = new GradientDrawable();
        dBg.setColor(0xEE161722);
        dBg.setCornerRadius(dp(20));
        dBg.setStroke(dp(1), 0xFF2C2F40);
        pillDock.setBackground(dBg);

        TextView lobbyTab = label("  Game Lobby", 11.5f, Color.WHITE);
        lobbyTab.setTypeface(null, Typeface.BOLD);
        lobbyTab.setGravity(Gravity.CENTER);
        lobbyTab.setPadding(dp(16), dp(8), dp(16), dp(8));
        GradientDrawable activePill = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFFFF1744, 0xFFD50000});
        activePill.setCornerRadius(dp(18));
        lobbyTab.setBackground(activePill);
        applyLeadingIcon(lobbyTab, R.drawable.ic_home, Color.WHITE, 16);
        pillDock.addView(lobbyTab);

        TextView superTab = label("  Super Base", 11.5f, 0xFFB0B3C0);
        superTab.setTypeface(null, Typeface.BOLD);
        superTab.setGravity(Gravity.CENTER);
        superTab.setPadding(dp(16), dp(8), dp(16), dp(8));
        applyLeadingIcon(superTab, R.drawable.ic_speed_meter, 0xFFB0B3C0, 16);
        superTab.setOnClickListener(v -> {
            showSuperBase = true;
            rebuildView();
        });
        pillDock.addView(superTab);

        FrameLayout.LayoutParams dockLp = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER);
        bottomDockArea.addView(pillDock, dockLp);

        LinearLayout hexTools = new LinearLayout(this);
        hexTools.setOrientation(LinearLayout.HORIZONTAL);
        hexTools.setGravity(Gravity.CENTER_VERTICAL);

        TextView tGrid = label("GAMES", 10, Color.WHITE);
        applyLeadingIcon(tGrid, R.drawable.ic_layers, Color.WHITE, 17);
        tGrid.setGravity(Gravity.CENTER);
        tGrid.setPadding(dp(8), dp(6), dp(8), dp(6));
        tGrid.setOnClickListener(v -> showAddGamesDialog());
        hexTools.addView(tGrid);

        TextView tBoost = label("BOOST", 10, 0xFFFFD600);
        applyLeadingIcon(tBoost, R.drawable.ic_flash, 0xFFFFD600, 17);
        tBoost.setGravity(Gravity.CENTER);
        tBoost.setPadding(dp(8), dp(6), dp(8), dp(6));
        tBoost.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            com.revror.kernelmanager.utils.GameOptimizer.applyUltraLowLatency();
            Toast.makeText(this, "REVROR Turbo Boost Active!", Toast.LENGTH_SHORT).show();
        });
        hexTools.addView(tBoost);

        TextView tCenter = label("CENTER", 10, 0xFFFF1744);
        applyLeadingIcon(tCenter, R.drawable.ic_settings_icon, 0xFFFF1744, 17);
        tCenter.setGravity(Gravity.CENTER);
        tCenter.setPadding(dp(8), dp(6), dp(8), dp(6));
        tCenter.setOnClickListener(v -> showGameSpaceCenterDialog());
        hexTools.addView(tCenter);

        FrameLayout.LayoutParams toolsLp = new FrameLayout.LayoutParams(-2, -2, Gravity.END | Gravity.CENTER_VERTICAL);
        bottomDockArea.addView(hexTools, toolsLp);

        adBannerView = buildAdBanner();
        LinearLayout.LayoutParams adLp = new LinearLayout.LayoutParams(-1, dp(isCompact  ?  44 : 52));
        adLp.bottomMargin = dp(2);
        content.addView(adBannerView, adLp);

        content.addView(bottomDockArea, new LinearLayout.LayoutParams(-1, dp(isCompact  ?  42 : 50)));

        root.addView(content, new FrameLayout.LayoutParams(-1, -1));
        return root;
    }

    private View createCyberGlassBackButton(View.OnClickListener listener) {
        FrameLayout button = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x770F1626);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1.2f), 0xFF425A88);
        button.setBackground(bg);
        button.setClickable(true);
        button.setFocusable(true);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_chevron_left);
        icon.setColorFilter(0xFF50CBFF);
        int size = dp(20);
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(size, size, Gravity.CENTER);
        button.addView(icon, iconLp);

        button.setOnClickListener(v -> {
            try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
            v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(60).withEndAction(() ->
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start()).start();
            if (listener != null) listener.onClick(v);
        });
        int btnW = dp(38);
        int btnH = dp(34);
        button.setLayoutParams(new LinearLayout.LayoutParams(btnW, btnH));
        return button;
    }

    private View createCyberPillBackButton(String labelText, View.OnClickListener listener) {
        LinearLayout button = new LinearLayout(this);
        button.setOrientation(LinearLayout.HORIZONTAL);
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setPadding(dp(10), dp(5), dp(12), dp(5));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x770F1626);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1.2f), 0xFF425A88);
        button.setBackground(bg);
        button.setClickable(true);
        button.setFocusable(true);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_chevron_left);
        icon.setColorFilter(0xFF50CBFF);
        int size = dp(18);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(size, size);
        iconLp.setMarginEnd(dp(4));
        button.addView(icon, iconLp);

        TextView text = new TextView(this);
        text.setText(labelText);
        text.setTextSize(10f);
        text.setTextColor(0xFFE2E7FF);
        text.setTypeface(null, Typeface.BOLD);
        text.setLetterSpacing(0.06f);
        button.addView(text);

        button.setOnClickListener(v -> {
            try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
            v.animate().scaleX(0.90f).scaleY(0.90f).setDuration(60).withEndAction(() ->
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start()).start();
            if (listener != null) listener.onClick(v);
        });
        return button;
    }

    private void showAddGamesDialog() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> rawList = pm.queryIntentActivities(intent, 0);
        List<ResolveInfo> apps = new ArrayList<>();
        for (ResolveInfo ri : rawList) {
            if (ri.activityInfo != null && !getPackageName().equals(ri.activityInfo.packageName)) {
                apps.add(ri);
            }
        }
        Collections.sort(apps, (a, b) -> a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString()));
        Set<String> registered = new HashSet<>(com.revror.kernelmanager.utils.GameLauncherRegistry.getGamePackages(this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF4141620);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), 0xFF35394C);
        root.setBackground(bg);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(12));

        View backBtn = createCyberGlassBackButton(v -> dialog.dismiss());
        header.addView(backBtn);

        TextView title = label("  Add Games", 17, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title);
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        for (ResolveInfo ri : apps) {
            String pkg = ri.activityInfo.packageName;
            String labelStr = ri.loadLabel(pm).toString();
            boolean isIncluded = registered.contains(pkg) || (!com.revror.kernelmanager.utils.GameLauncherRegistry.isGameExcluded(this, pkg) && (ri.activityInfo.applicationInfo.category == ApplicationInfo.CATEGORY_GAME || labelStr.toLowerCase().contains("game") || labelStr.toLowerCase().contains("ggpo")));

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(4), dp(8), dp(4), dp(8));

            ImageView icon = new ImageView(this);
            icon.setImageDrawable(ri.loadIcon(pm));
            row.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));

            TextView name = label(labelStr, 13.5f, Color.WHITE);
            name.setTypeface(null, Typeface.BOLD);
            name.setPadding(dp(14), 0, dp(10), 0);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            row.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));

            LinearLayout pill = new LinearLayout(this);
            pill.setOrientation(LinearLayout.HORIZONTAL);
            pill.setGravity(Gravity.CENTER_VERTICAL);
            pill.setPadding(dp(2), dp(2), dp(2), dp(2));
            GradientDrawable pillBg = new GradientDrawable();
            pillBg.setColor(0xDD222533);
            pillBg.setCornerRadius(dp(12));
            pillBg.setStroke(dp(1), 0xFF383D52);
            pill.setBackground(pillBg);

            TextView btnExclude = label("Exclude", 10.5f, isIncluded  ?  0xFF8E95A8 : Color.WHITE);
            btnExclude.setTypeface(null, Typeface.BOLD);
            btnExclude.setGravity(Gravity.CENTER);
            btnExclude.setPadding(dp(10), dp(4), dp(10), dp(4));

            TextView btnInclude = label("Include", 10.5f, isIncluded  ?  Color.WHITE : 0xFF8E95A8);
            btnInclude.setTypeface(null, Typeface.BOLD);
            btnInclude.setGravity(Gravity.CENTER);
            btnInclude.setPadding(dp(10), dp(4), dp(10), dp(4));

            GradientDrawable activeHalf = new GradientDrawable();
            activeHalf.setColor(0xFFFF1744);
            activeHalf.setCornerRadius(dp(10));

            if (isIncluded) {
                btnInclude.setBackground(activeHalf);
                btnExclude.setBackground(null);
            } else {
                btnExclude.setBackground(activeHalf);
                btnInclude.setBackground(null);
            }

            pill.addView(btnExclude);
            pill.addView(btnInclude);

            final boolean[] stateHolder = new boolean[]{isIncluded};
            pill.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                stateHolder[0] = !stateHolder[0];
                boolean nowIncluded = stateHolder[0];
                if (nowIncluded) {
                    com.revror.kernelmanager.utils.GameLauncherRegistry.registerGame(this, pkg, labelStr);
                    btnInclude.setBackground(activeHalf);
                    btnInclude.setTextColor(Color.WHITE);
                    btnExclude.setBackground(null);
                    btnExclude.setTextColor(0xFF8E95A8);
                } else {
                    com.revror.kernelmanager.utils.GameLauncherRegistry.unregisterGame(this, pkg);
                    btnExclude.setBackground(activeHalf);
                    btnExclude.setTextColor(Color.WHITE);
                    btnInclude.setBackground(null);
                    btnInclude.setTextColor(0xFF8E95A8);
                }
            });

            row.addView(pill, new LinearLayout.LayoutParams(-2, dp(32)));
            list.addView(row);

            View div = new View(this);
            div.setBackgroundColor(0x18FFFFFF);
            list.addView(div, new LinearLayout.LayoutParams(-1, dp(1)));
        }

        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        dialog.setContentView(root);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams wlp = new WindowManager.LayoutParams();
            wlp.copyFrom(w.getAttributes());
            wlp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90f);
            wlp.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.90f);
            w.setAttributes(wlp);
        }

        backBtn.setOnClickListener(v -> dialog.dismiss());
        dialog.setOnDismissListener(d -> {
            loadApps();
            rebuildView();
        });
        dialog.show();
    }

    private void showGameSpaceCenterDialog() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF611131B);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), 0xFF35394C);
        root.setBackground(bg);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(12));

        View backBtn = createCyberGlassBackButton(v -> dialog.dismiss());
        header.addView(backBtn);

        TextView title = label("  Game Space Center", 17, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title);
        root.addView(header);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout sidebar = new LinearLayout(this);
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setPadding(0, dp(6), dp(10), dp(6));
        sidebar.setBackgroundColor(0x33000000);

        String[] tabs = {
                "Peripheral Connection Mode",
                "After-game behavior",
                "Game Atmosphere LED",
                "Game Space wallpaper",
                "REVROR watermark",
                "Floating Windows Options",
                "Other options",
                "About Game Space"
        };
        int[] tabIcons = {
                R.drawable.ic_console,
                R.drawable.ic_speed_meter,
                R.drawable.ic_led,
                R.drawable.ic_display,
                R.drawable.ic_barrage_chibi,
                R.drawable.ic_floating_window,
                R.drawable.ic_dots,
                R.drawable.ic_about
        };

        FrameLayout rightContentArea = new FrameLayout(this);
        final int[] activeTabHolder = new int[]{5}; // Default: Other options (index 5)
        List<LinearLayout> tabViews = new ArrayList<>();

        Runnable updateTabs = () -> {
            for (int i = 0; i < tabViews.size(); i++) {
                LinearLayout tv = tabViews.get(i);
                boolean active = (i == activeTabHolder[0]);
                if (active) {
                    GradientDrawable aBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                            new int[]{0xFF6A1B9A, 0xFFFF1744});
                    aBg.setCornerRadius(dp(8));
                    tv.setBackground(aBg);
                } else {
                    tv.setBackground(null);
                }
            }
            rightContentArea.removeAllViews();
            rightContentArea.addView(buildGameSpaceCenterTabContent(activeTabHolder[0], tabs));
        };

        for (int i = 0; i < tabs.length; i++) {
            final int tIdx = i;
            LinearLayout tabRow = new LinearLayout(this);
            tabRow.setOrientation(LinearLayout.HORIZONTAL);
            tabRow.setGravity(Gravity.CENTER_VERTICAL);
            tabRow.setPadding(dp(12), dp(10), dp(12), dp(10));

            ImageView icon = new ImageView(this);
            icon.setImageResource(tabIcons[i]);
            tabRow.addView(icon, new LinearLayout.LayoutParams(dp(18), dp(18)));

            TextView tabLabel = label(" " + tabs[i], 11f, Color.WHITE);
            tabLabel.setTypeface(null, Typeface.BOLD);
            tabRow.addView(tabLabel);

            tabRow.setOnClickListener(v -> {
                activeTabHolder[0] = tIdx;
                updateTabs.run();
            });

            tabViews.add(tabRow);
            sidebar.addView(tabRow, new LinearLayout.LayoutParams(-1, -2));
        }

        ScrollView sidebarScroll = new ScrollView(this);
        sidebarScroll.setFillViewport(true);
        sidebarScroll.setVerticalScrollBarEnabled(false);
        sidebarScroll.addView(sidebar, new FrameLayout.LayoutParams(-1, -2));

        body.addView(sidebarScroll, new LinearLayout.LayoutParams(dp(210), -1));
        body.addView(rightContentArea, new LinearLayout.LayoutParams(0, -1, 1f));
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));

        updateTabs.run();

        dialog.setContentView(root);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams wlp = new WindowManager.LayoutParams();
            wlp.copyFrom(w.getAttributes());
            wlp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92f);
            wlp.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.90f);
            w.setAttributes(wlp);
        }
        dialog.show();
    }

    private View buildGameSpaceCenterTabContent(int tabIndex, String[] tabs) {
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(8), dp(16), dp(8));

        SharedPreferences sp = getSharedPreferences("ror_game_space_center", MODE_PRIVATE);

        String[][] options = null;
        if (tabIndex == 0) {
            options = new String[][]{
                    {"Peripheral connection mode", "Enable REVROR support for controllers, keyboards and mice", "ror_peripheral_mode", "true"},
                    {"Controller auto-detection", "Detect a connected controller when Game Space opens", "ror_controller_detect", "true"},
                    {"Keyboard and mouse mapping", "Enable input mapping controls for supported games", "ror_input_mapping", "true"},
                    {"Bluetooth priority", "Prioritize low-latency Bluetooth input while gaming", "ror_bluetooth_priority", "false"},
                    {"Peripheral connection pop-up", "Show a status pop-up when a peripheral connects", "ror_peripheral_popup", "true"}
            };
        } else if (tabIndex == 1) {
            options = new String[][]{
                    {"Restore display settings", "Restore normal brightness and refresh rate after a game", "ror_restore_display", "true"},
                    {"Restore notification mode", "Return notification behavior to its previous state", "ror_restore_notifications", "true"},
                    {"Close floating game tools", "Remove REVROR overlays when the game closes", "ror_close_overlays", "true"},
                    {"Clear temporary game cache", "Release temporary memory after a gaming session", "ror_clear_temp_cache", "false"},
                    {"Show session summary", "Display play time and performance details after the game", "ror_session_summary", "true"}
            };
        } else if (tabIndex == 2) {
            options = new String[][]{
                    {"Game Atmosphere LED", "Enable the REVROR atmosphere lighting layer", "ror_atmosphere_led", "true"},
                    {"Performance color", "Change lighting color with the active performance profile", "ror_led_profile_color", "true"},
                    {"Notification pulse", "Pulse the atmosphere light for allowed notifications", "ror_led_notifications", "false"},
                    {"Charging glow", "Show a charging effect while Game Space is open", "ror_led_charging", "true"},
                    {"Reduce effects on low battery", "Automatically limit atmosphere effects below 20%", "ror_led_low_battery", "true"}
            };
        } else if (tabIndex == 4) {
            options = new String[][]{
                    {"REVROR watermark", "Show REVROR branding on Game Space captures", "ror_watermark_enabled", "true"},
                    {"Include session time", "Add the current session duration to the watermark", "ror_watermark_time", "false"},
                    {"Include active profile", "Add the selected performance profile", "ror_watermark_profile", "true"},
                    {"Compact watermark", "Use the small corner watermark style", "ror_watermark_compact", "true"}
            };
        } else if (tabIndex == 5) {
            renderFloatingToolsOptions(panel, sp, gameSpaceAccent);
        } else if (tabIndex == 6) {
            TextView layoutHeader = label("GAME SPACE DISPLAY LAYOUT", 13.5f, gameSpaceAccent);
            layoutHeader.setTypeface(null, Typeface.BOLD);
            panel.addView(layoutHeader);
            TextView layoutDesc = label("Current: " + LAYOUT_NAMES[Math.floorMod(layoutIndex, LAYOUT_NAMES.length)] + "  \u2022  Tap below to switch layout style anytime.", 10f, 0xFFB0B3C0);
            layoutDesc.setPadding(0, dp(4), 0, dp(10));
            panel.addView(layoutDesc);

            TextView changeLayoutBtn = redHeaderButton("SELECT GAME SPACE LAYOUT", R.drawable.ic_layers);
            changeLayoutBtn.setOnClickListener(v -> showLayoutChooser());
            panel.addView(changeLayoutBtn, new LinearLayout.LayoutParams(-2, dp(38)));

            View div = new View(this);
            div.setBackgroundColor(0x18FFFFFF);
            LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(-1, dp(1));
            divLp.setMargins(0, dp(12), 0, dp(12));
            panel.addView(div, divLp);

            options = new String[][]{
                    {"Auto Gaming Profile", "Automatically apply Gaming profile whenever any Game Space game is active", "ror_auto_gaming_profile", "true"},
                    {"Game information snapshot", "Show play-time and storage details on game cards", "ror_game_info_snapshot", "true"},
                    {"Intelligent highlight collection", "Allow REVROR to prepare marked gameplay highlights", "ror_highlight_moments", "true"},
                    {"Hide game icons on the device home screen", "Keep games organized inside REVROR Game Space", "ror_hide_icons_home", "false"},
                    {"Keep screen awake", "Prevent the display from sleeping while Game Space is active", "ror_keep_screen_on", "true"},
                    {"Game Space opening animation", "Play the REVROR cyber laser animation when Game Space opens", "ror_opening_anim", "true"}
            };
        } else if (tabIndex == 7) {
            TextView aboutTitle = label("REVROR GAME SPACE", 16f, gameSpaceAccent);
            aboutTitle.setTypeface(null, Typeface.BOLD);
            panel.addView(aboutTitle);
            TextView aboutCopy = label("Version 50.3  \u2022  Performance controls, game overlays and personalization by REVROR.", 10.5f, 0xFFB0B3C0);
            aboutCopy.setPadding(0, dp(6), 0, dp(12));
            panel.addView(aboutCopy);
            options = new String[][]{
                    {"Show version badge", "Display the REVROR Game Space version in the information panel", "ror_show_version", "true"},
                    {"Feature tips", "Show short guidance when a feature is used for the first time", "ror_feature_tips", "true"},
                    {"Anonymous diagnostics", "Store local diagnostics that help identify Game Space errors", "ror_diagnostics", "false"}
            };
        }

        if (options != null) {
            for (String[] option : options) {
                addGameSpaceCenterToggle(panel, sp, option[0], option[1], option[2], Boolean.parseBoolean(option[3]));
            }
        } else if (tabIndex == 3) {
            TextView t = label("GAME SPACE WALLPAPER & THEMES", 13.5f, gameSpaceAccent);
            t.setTypeface(null, Typeface.BOLD);
            panel.addView(t);
            TextView desc = label("Tap any wallpaper preset to apply instantly across all layouts.", 10f, 0xFFB0B3C0);
            desc.setPadding(0, dp(4), 0, dp(10));
            panel.addView(desc);

            // Quick Accent Colors
            TextView colTitle = label("ACCENT COLOR", 11f, 0xFF8E95AF);
            colTitle.setTypeface(null, Typeface.BOLD);
            panel.addView(colTitle);
            HorizontalScrollView colScroll = new HorizontalScrollView(this);
            colScroll.setHorizontalScrollBarEnabled(false);
            LinearLayout colRow = new LinearLayout(this);
            colRow.setOrientation(LinearLayout.HORIZONTAL);
            colRow.setPadding(0, dp(4), 0, dp(10));
            for (int i = 0; i < GAME_SPACE_AACCENTS.length; i++) {
                final int color = GAME_SPACE_AACCENTS[i];
                final String name = GAME_SPACE_ACCENT_NAMES[i];
                boolean active = (color == gameSpaceAccent);
                LinearLayout chip = new LinearLayout(this);
                chip.setGravity(Gravity.CENTER_VERTICAL);
                chip.setPadding(dp(8), dp(4), dp(10), dp(4));
                GradientDrawable cBg = new GradientDrawable();
                cBg.setColor(active ? (color & 0x33FFFFFF) : 0xFF1D202A);
                cBg.setCornerRadius(dp(14));
                cBg.setStroke(dp(active ? 2 : 1), active ? color : 0xFF35394A);
                chip.setBackground(cBg);
                View dot = new View(this);
                GradientDrawable dBg = new GradientDrawable();
                dBg.setShape(GradientDrawable.OVAL);
                dBg.setColor(color);
                dot.setBackground(dBg);
                chip.addView(dot, new LinearLayout.LayoutParams(dp(12), dp(12)));
                TextView nView = label(name, 9.5f, active ? Color.WHITE : 0xFF9E9EA8);
                nView.setTypeface(null, Typeface.BOLD);
                nView.setPadding(dp(6), 0, 0, 0);
                chip.addView(nView);
                LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(-2, -2);
                cLp.setMarginEnd(dp(6));
                chip.setLayoutParams(cLp);
                chip.setOnClickListener(v -> {
                    gameSpaceAccent = color;
                    savePreferences();
                    if (animatedBannerView != null) animatedBannerView.invalidate();
                    Toast.makeText(this, name + " accent applied", Toast.LENGTH_SHORT).show();
                });
                colRow.addView(chip);
            }
            colScroll.addView(colRow);
            panel.addView(colScroll);

            // Wallpaper Presets Grid
            TextView banTitle = label("DYNAMIC BANNER WALLPAPERS", 11f, 0xFF8E95AF);
            banTitle.setTypeface(null, Typeface.BOLD);
            banTitle.setPadding(0, dp(6), 0, dp(8));
            panel.addView(banTitle);

            GridLayout grid = new GridLayout(this);
            grid.setColumnCount(3);
            for (int b = 0; b < BANNER_TITLES.length; b++) {
                final int bId = b;
                final String bTitle = BANNER_TITLES[b];
                boolean active = (activeBannerId == bId);
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setGravity(Gravity.CENTER_HORIZONTAL);
                card.setPadding(dp(4), dp(4), dp(4), dp(4));
                GradientDrawable cardBg = new GradientDrawable();
                cardBg.setColor(active ? (gameSpaceAccent & 0x33FFFFFF) : 0xFF191B24);
                cardBg.setCornerRadius(dp(8));
                cardBg.setStroke(dp(active ? 2 : 1), active ? gameSpaceAccent : 0xFF35394A);
                card.setBackground(cardBg);
                ImageView thumb = new ImageView(this);
                thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
                thumb.setImageBitmap(drawBannerBitmap(bId, 140, 75));
                card.addView(thumb, new LinearLayout.LayoutParams(dp(110), dp(58)));
                TextView bLabel = label(bTitle, 8.5f, active ? Color.WHITE : 0xFF9E9EA8);
                bLabel.setTypeface(null, Typeface.BOLD);
                bLabel.setPadding(0, dp(3), 0, dp(1));
                card.addView(bLabel);
                GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
                glp.width = dp(118);
                glp.height = -2;
                glp.setMargins(dp(3), dp(3), dp(3), dp(3));
                card.setLayoutParams(glp);
                card.setOnClickListener(v -> {
                    activeBannerId = bId;
                    selectedBannerId = bId;
                    savePreferences();
                    if (animatedBannerView != null) {
                        animatedBannerView.setBanner(activeBannerId, null, bannerBlur);
                    }
                    Toast.makeText(this, bTitle + " wallpaper applied", Toast.LENGTH_SHORT).show();
                });
                grid.addView(card);
            }
            panel.addView(grid);

            TextView fullMgrBtn = redHeaderButton("OPEN ADVANCED THEMES & GALLERY MANAGER", R.drawable.ic_display);
            fullMgrBtn.setOnClickListener(v -> showThemeAndBannerDialog());
            LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(-2, dp(36));
            btnLp.setMargins(0, dp(12), 0, dp(8));
            panel.addView(fullMgrBtn, btnLp);
        }

        scroll.addView(panel, new ScrollView.LayoutParams(-1, -2));
        return scroll;
    }

    private void renderFloatingToolsOptions(LinearLayout panel, SharedPreferences sp, int accentColor) {
        panel.removeAllViews();

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, dp(6));

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        TextView fwTitle = label("FLOATING WINDOWS & INGAME TOOLS", 13.5f, accentColor);
        fwTitle.setTypeface(null, Typeface.BOLD);
        fwTitle.setLetterSpacing(0.04f);
        titleCol.addView(fwTitle);
        TextView fwDesc = label("Drag, hold or tap ▲ ▼ to re-arrange real-time.", 9.5f, 0xFFB0B3C0);
        fwDesc.setPadding(0, dp(2), 0, 0);
        titleCol.addView(fwDesc);
        headerRow.addView(titleCol, new LinearLayout.LayoutParams(0, -2, 1f));

        // Cyber "SET DEFAULT" button
        LinearLayout resetBtn = new LinearLayout(this);
        resetBtn.setOrientation(LinearLayout.HORIZONTAL);
        resetBtn.setGravity(Gravity.CENTER_VERTICAL);
        resetBtn.setPadding(dp(10), dp(5), dp(12), dp(5));
        GradientDrawable resetBg = new GradientDrawable();
        resetBg.setColor(0x28FF0055);
        resetBg.setCornerRadius(dp(12));
        resetBg.setStroke(dp(1.2f), 0xFFFF0055);
        resetBtn.setBackground(resetBg);
        resetBtn.setClickable(true);
        resetBtn.setFocusable(true);

        ImageView resetIcon = new ImageView(this);
        resetIcon.setImageResource(R.drawable.ic_refresh);
        resetIcon.setColorFilter(0xFFFF5588);
        LinearLayout.LayoutParams riLp = new LinearLayout.LayoutParams(dp(14), dp(14));
        riLp.setMarginEnd(dp(4));
        resetBtn.addView(resetIcon, riLp);

        TextView resetText = label("SET DEFAULT", 9.5f, 0xFFFFDDE6);
        resetText.setTypeface(null, Typeface.BOLD);
        resetText.setLetterSpacing(0.06f);
        resetBtn.addView(resetText);

        resetBtn.setOnClickListener(v -> {
            try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(60).withEndAction(() ->
                v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()).start();
            sp.edit().putString("ror_floating_tools_order", DEFAULT_FLOATING_TOOLS_ORDER).apply();
            for (String k : FLOATING_TOOL_NAMES.keySet()) {
                sp.edit().putBoolean("ror_tool_vis_" + k, true).apply();
            }
            renderFloatingToolsOptions(panel, sp, accentColor);
            GameBubbleService bubble = GameBubbleService.getInstance();
            if (bubble != null) bubble.reloadFloatingTools();
            Toast.makeText(this, "Restored default floating tools order", Toast.LENGTH_SHORT).show();
        });
        headerRow.addView(resetBtn);
        panel.addView(headerRow);

        View div = new View(this);
        div.setBackgroundColor(0x22FFFFFF);
        panel.addView(div, new LinearLayout.LayoutParams(-1, dp(1)));

        // Load active order
        String currentOrder = sp.getString("ror_floating_tools_order", DEFAULT_FLOATING_TOOLS_ORDER);
        List<String> list = new ArrayList<>(Arrays.asList(currentOrder.split(",")));
        for (String k : FLOATING_TOOL_NAMES.keySet()) {
            if (!list.contains(k)) list.add(k);
        }

        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        panel.addView(listContainer);

        populateDraggableToolRows(listContainer, list, panel, sp, accentColor);
    }

    private void populateDraggableToolRows(LinearLayout listContainer, List<String> list, LinearLayout parentPanel, SharedPreferences sp, int accentColor) {
        listContainer.removeAllViews();
        final int total = list.size();

        for (int i = 0; i < total; i++) {
            final int pos = i;
            final String toolKey = list.get(pos);
            final String toolName = FLOATING_TOOL_NAMES.containsKey(toolKey)  ?  FLOATING_TOOL_NAMES.get(toolKey) : toolKey;

            LinearLayout toolRow = new LinearLayout(this);
            toolRow.setOrientation(LinearLayout.HORIZONTAL);
            toolRow.setGravity(Gravity.CENTER_VERTICAL);
            toolRow.setPadding(dp(4), dp(8), dp(4), dp(8));
            GradientDrawable rowBg = new GradientDrawable();
            rowBg.setColor(0x00000000);
            rowBg.setCornerRadius(dp(8));
            toolRow.setBackground(rowBg);

            // 1. Grip handle for drag / hold to move
            ImageView grip = controlIcon(R.drawable.ic_drag_handle, 0xFF6C7A9C, false);
            LinearLayout.LayoutParams gripLp = new LinearLayout.LayoutParams(dp(28), dp(28));
            gripLp.setMarginEnd(dp(4));
            toolRow.addView(grip, gripLp);

            // 2. Position badge
            TextView numBadge = label(String.valueOf(pos + 1) + ". ", 10.5f, 0xFF50CBFF);
            numBadge.setTypeface(null, Typeface.BOLD);
            toolRow.addView(numBadge);

            // 3. Tool name
            TextView nameView = label(toolName, 12f, Color.WHITE);
            toolRow.addView(nameView, new LinearLayout.LayoutParams(0, -2, 1f));

            // 4. Real-time Move Up button
            ImageView upBtn = controlIcon(R.drawable.ic_arrow_up,
                    pos > 0 ? 0xFF00E5FF : 0x3300E5FF, pos > 0);
            if (pos > 0) {
                upBtn.setOnClickListener(v -> {
                    try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
                    Collections.swap(list, pos, pos - 1);
                    sp.edit().putString("ror_floating_tools_order", TextUtils.join(",", list)).apply();
                    populateDraggableToolRows(listContainer, list, parentPanel, sp, accentColor);
                    GameBubbleService bubble = GameBubbleService.getInstance();
                    if (bubble != null) bubble.reloadFloatingTools();
                });
            }
            toolRow.addView(upBtn, new LinearLayout.LayoutParams(dp(34), dp(34)));

            // 5. Real-time Move Down button
            ImageView downBtn = controlIcon(R.drawable.ic_arrow_down,
                    pos < total - 1 ? 0xFF00E5FF : 0x3300E5FF, pos < total - 1);
            if (pos < total - 1) {
                downBtn.setOnClickListener(v -> {
                    try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
                    Collections.swap(list, pos, pos + 1);
                    sp.edit().putString("ror_floating_tools_order", TextUtils.join(",", list)).apply();
                    populateDraggableToolRows(listContainer, list, parentPanel, sp, accentColor);
                    GameBubbleService bubble = GameBubbleService.getInstance();
                    if (bubble != null) bubble.reloadFloatingTools();
                });
            }
            toolRow.addView(downBtn, new LinearLayout.LayoutParams(dp(34), dp(34)));

            // 6. Visibility Switch
            FrameLayout toggle = new FrameLayout(this);
            final boolean[] isVisible = {sp.getBoolean("ror_tool_vis_" + toolKey, true)};
            Runnable refreshTog = () -> {
                GradientDrawable track = new GradientDrawable();
                track.setColor(isVisible[0]  ?  accentColor : 0xFF2A2E3D);
                track.setCornerRadius(dp(13));
                toggle.setBackground(track);
                toggle.removeAllViews();
                View thumb = new View(this);
                GradientDrawable thumbBg = new GradientDrawable();
                thumbBg.setShape(GradientDrawable.OVAL);
                thumbBg.setColor(Color.WHITE);
                thumb.setBackground(thumbBg);
                FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(dp(20), dp(20));
                tp.gravity = (isVisible[0]  ?  Gravity.END : Gravity.START) | Gravity.CENTER_VERTICAL;
                tp.setMargins(dp(3), dp(3), dp(3), dp(3));
                toggle.addView(thumb, tp);
            };
            toggle.setOnClickListener(v -> {
                try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
                isVisible[0] = !isVisible[0];
                sp.edit().putBoolean("ror_tool_vis_" + toolKey, isVisible[0]).apply();
                refreshTog.run();
                GameBubbleService bubble = GameBubbleService.getInstance();
                if (bubble != null) bubble.reloadFloatingTools();
                Toast.makeText(this, toolName + (isVisible[0]  ?  " enabled" : " hidden"), Toast.LENGTH_SHORT).show();
            });
            refreshTog.run();
            LinearLayout.LayoutParams togLp = new LinearLayout.LayoutParams(dp(48), dp(26));
            togLp.setMarginStart(dp(8));
            toolRow.addView(toggle, togLp);

            // 7. Drag-and-drop & Hold-and-move support
            toolRow.setTag(pos);
            toolRow.setOnLongClickListener(v -> {
                try { v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); } catch (Exception ignored) {}
                ClipData data = ClipData.newPlainText("tool_idx", String.valueOf(pos));
                View.DragShadowBuilder shadow = new View.DragShadowBuilder(toolRow);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    toolRow.startDragAndDrop(data, shadow, pos, 0);
                } else {
                    toolRow.startDrag(data, shadow, pos, 0);
                }
                return true;
            });

            toolRow.setOnDragListener((targetView, event) -> {
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_ENTERED:
                        rowBg.setColor(0x3350CBFF);
                        break;
                    case DragEvent.ACTION_DRAG_EXITED:
                    case DragEvent.ACTION_DRAG_ENDED:
                        rowBg.setColor(0x00000000);
                        break;
                    case DragEvent.ACTION_DROP:
                        rowBg.setColor(0x00000000);
                        try {
                            Object local = event.getLocalState();
                            int fromPos = (local instanceof Integer)  ?  (Integer) local : -1;
                            int toPos = pos;
                            if (fromPos >= 0 && fromPos < list.size() && fromPos != toPos) {
                                String item = list.remove(fromPos);
                                list.add(toPos, item);
                                sp.edit().putString("ror_floating_tools_order", TextUtils.join(",", list)).apply();
                                populateDraggableToolRows(listContainer, list, parentPanel, sp, accentColor);
                                GameBubbleService bubble = GameBubbleService.getInstance();
                                if (bubble != null) bubble.reloadFloatingTools();
                                Toast.makeText(this, "Moved " + toolName + " to position " + (toPos + 1), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception ignored) {}
                        break;
                }
                return true;
            });

            listContainer.addView(toolRow);

            View divLine = new View(this);
            divLine.setBackgroundColor(0x10FFFFFF);
            listContainer.addView(divLine, new LinearLayout.LayoutParams(-1, dp(1)));
        }
    }

    private void addGameSpaceCenterToggle(LinearLayout panel, SharedPreferences preferences,
                                          String title, String subtitle, String key, boolean defaultValue) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));
        row.setClickable(true);
        row.setFocusable(true);

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = label(title, 13f, Color.WHITE);
        titleView.setTypeface(null, Typeface.BOLD);
        textColumn.addView(titleView);
        TextView subtitleView = label(subtitle, 9.5f, 0xFF9A9AA6);
        subtitleView.setPadding(0, dp(2), dp(12), 0);
        textColumn.addView(subtitleView);
        row.addView(textColumn, new LinearLayout.LayoutParams(0, -2, 1f));

        FrameLayout toggle = new FrameLayout(this);
        final boolean[] enabled = {preferences.getBoolean(key, defaultValue)};
        Runnable refresh = () -> {
            GradientDrawable track = new GradientDrawable();
            track.setColor(enabled[0]  ?  gameSpaceAccent : 0xFF2A2E3D);
            track.setCornerRadius(dp(13));
            toggle.setBackground(track);
            toggle.removeAllViews();
            View thumb = new View(this);
            GradientDrawable thumbBackground = new GradientDrawable();
            thumbBackground.setShape(GradientDrawable.OVAL);
            thumbBackground.setColor(Color.WHITE);
            thumb.setBackground(thumbBackground);
            FrameLayout.LayoutParams thumbParams = new FrameLayout.LayoutParams(dp(20), dp(20));
            thumbParams.gravity = (enabled[0]  ?  Gravity.END : Gravity.START) | Gravity.CENTER_VERTICAL;
            thumbParams.setMargins(dp(3), dp(3), dp(3), dp(3));
            toggle.addView(thumb, thumbParams);
            toggle.setContentDescription(title + (enabled[0]  ?  ": enabled" : ": disabled"));
        };

        View.OnClickListener toggleAction = view -> {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            enabled[0] = !enabled[0];
            preferences.edit().putBoolean(key, enabled[0]).apply();
            refresh.run();
            applyGameSpaceCenterSetting(key, enabled[0]);
            Toast.makeText(this, title + (enabled[0]  ?  " enabled" : " disabled"), Toast.LENGTH_SHORT).show();
        };
        toggle.setOnClickListener(toggleAction);
        row.setOnClickListener(toggleAction);
        refresh.run();
        row.addView(toggle, new LinearLayout.LayoutParams(dp(48), dp(26)));
        panel.addView(row);

        View divider = new View(this);
        divider.setBackgroundColor(0x18FFFFFF);
        panel.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private void applyGameSpaceCenterSetting(String key, boolean enabled) {
        if ("ror_keep_screen_on".equals(key)) {
            applyKeepScreenOnPreference();
        } else if ("ror_opening_anim".equals(key) && enabled) {
            startEntranceAnimations();
        } else if ("ror_restore_display".equals(key) && enabled) {
            runLauncherCommand("settings put secure accessibility_display_inversion_enabled 0");
        } else if ("ror_atmosphere_led".equals(key)) {
            rebuildView();
        }
    }

    private void showHuntingFilterDialog() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF4141620);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), 0xFF35394C);
        root.setBackground(bg);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(12));

        TextView title = label("hunting-filter", 16, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setLetterSpacing(0.04f);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView closeBtn = label("", 18, 0xFFB0B5C6);
        closeBtn.setTypeface(null, Typeface.BOLD);
        closeBtn.setGravity(Gravity.CENTER);
        closeBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        header.addView(closeBtn, new LinearLayout.LayoutParams(-2, -2));
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        SharedPreferences prefs = getSharedPreferences("ror_game_plugins", MODE_PRIVATE);
        final int[] currentFilter = new int[]{prefs.getInt("hunting_filter_id", 0)};

        Runnable rebuildList = () -> {
            list.removeAllViews();
            for (int i = 1; i < GameBubbleService.HUNTING_FILTER_NAMES.length; i++) {
                final int filterId = i;
                final boolean isSelected = (filterId == currentFilter[0]);

                LinearLayout item = new LinearLayout(this);
                item.setOrientation(LinearLayout.HORIZONTAL);
                item.setGravity(Gravity.CENTER_VERTICAL);
                item.setPadding(0, dp(7), 0, dp(7));

                FrameLayout thumbFrame = new FrameLayout(this);
                GradientDrawable tBg = new GradientDrawable();
                tBg.setCornerRadius(dp(8));
                tBg.setStroke(dp(isSelected  ?  2 : 1), isSelected  ?  0xFFFF1744 : 0xFF3A3F50);
                thumbFrame.setBackground(tBg);
                thumbFrame.setPadding(dp(2), dp(2), dp(2), dp(2));

                ImageView thumbView = new ImageView(this);
                thumbView.setScaleType(ImageView.ScaleType.FIT_XY);
                thumbView.setImageBitmap(GameBubbleService.createFilterThumbnail(filterId, dp(96), dp(52)));
                thumbFrame.addView(thumbView, new FrameLayout.LayoutParams(-1, -1));

                item.addView(thumbFrame, new LinearLayout.LayoutParams(dp(96), dp(52)));

                LinearLayout info = new LinearLayout(this);
                info.setOrientation(LinearLayout.VERTICAL);
                info.setPadding(dp(12), 0, 0, 0);

                TextView name = label(GameBubbleService.HUNTING_FILTER_NAMES[filterId], 13.5f, isSelected  ?  0xFFFF1744 : Color.WHITE);
                name.setTypeface(null, Typeface.BOLD);
                info.addView(name);

                TextView desc = label(GameBubbleService.HUNTING_FILTER_DESCRIPTIONS[filterId], 9.5f, isSelected  ?  0xFFFF7080 : 0xFF9EABB8);
                desc.setPadding(0, dp(2), 0, 0);
                info.addView(desc);

                item.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));

                item.setOnClickListener(v -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    if (isSelected) {
                        currentFilter[0] = 0;
                        prefs.edit().putInt("hunting_filter_id", 0).putBoolean("screen_filter", false).apply();
                    } else {
                        currentFilter[0] = filterId;
                        prefs.edit().putInt("hunting_filter_id", filterId).putBoolean("screen_filter", true).apply();
                    }
                    Intent intent = new Intent(this, GameBubbleService.class);
                    intent.setAction(GameBubbleService.ACTION_SET_HUNTING_FILTER);
                    intent.putExtra("filter_id", currentFilter[0]);
                    if (Build.VERSION.SDK_INT >= 26) {
                        startForegroundService(intent);
                    } else {
                        startService(intent);
                    }
                    dialog.dismiss();
                    Toast.makeText(this, currentFilter[0] > 0  ?  (GameBubbleService.HUNTING_FILTER_NAMES[currentFilter[0]] + " filter applied") : "Filter disabled", Toast.LENGTH_SHORT).show();
                });

                list.addView(item);

                View divider = new View(this);
                divider.setBackgroundColor(0x18FFFFFF);
                list.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
            }
        };

        rebuildList.run();

        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        dialog.setContentView(root);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams wlp = new WindowManager.LayoutParams();
            wlp.copyFrom(w.getAttributes());
            wlp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88f);
            wlp.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.88f);
            w.setAttributes(wlp);
        }
        dialog.show();
    }

    private static final int REQ_PICK_BANNER = 8821;

    public void pickCustomBanner() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        try {
            startActivityForResult(intent, REQ_PICK_BANNER);
        } catch (Exception e) {
            Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
            fallback.setType("image/*");
            try {
                startActivityForResult(fallback, REQ_PICK_BANNER);
            } catch (Exception ex) {
                Toast.makeText(this, "No image picker available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_BANNER && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                if ((data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
                    getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            } catch (Exception ignored) {}
            String uriStr = uri.toString();
            if (!customBannersList.contains(uriStr)) {
                customBannersList.add(uriStr);
            }
            activeBannerId = 100 + (customBannersList.size() - 1);
            savePreferences();
            Toast.makeText(this, "Custom banner added and set", Toast.LENGTH_SHORT).show();
            rebuildView();
        }
    }

    private Bitmap drawBannerBitmap(int id, int w, int h) {
        if (w <= 0) w = 960;
        if (h <= 0) h = 540;
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        switch (id % BANNER_TITLES.length) {
            case 0: { // Cyber Laser
                LinearGradient g = new LinearGradient(0, 0, w, h,
                        new int[]{0xFF0A0C16, 0xFF140F24, 0xFF081820}, null, Shader.TileMode.CLAMP);
                p.setShader(g);
                canvas.drawRect(0, 0, w, h, p);
                p.setShader(null);
                p.setColor(0x3300E5FF);
                p.setStrokeWidth(dp(1.2f));
                for (int x = 0; x <= w; x += Math.max(1, w / 12)) {
                    canvas.drawLine(w / 2f, h * 0.45f, x, h, p);
                }
                for (float y = h * 0.45f; y <= h; y += Math.max(1f, (h * 0.55f) / 6f)) {
                    canvas.drawLine(0, y, w, y, p);
                }
                p.setColor(0x88FF0055);
                p.setStrokeWidth(dp(3f));
                canvas.drawLine(0, h * 0.2f, w, h * 0.8f, p);
                p.setColor(0x8800E5FF);
                canvas.drawLine(0, h * 0.85f, w, h * 0.15f, p);
                break;
            }
            case 1: { // Space Nebula
                LinearGradient g = new LinearGradient(0, 0, 0, h,
                        new int[]{0xFF05030A, 0xFF1B0A2E, 0xFF0B192E}, null, Shader.TileMode.CLAMP);
                p.setShader(g);
                canvas.drawRect(0, 0, w, h, p);
                p.setShader(null);
                RadialGradient nebula1 = new RadialGradient(w * 0.3f, h * 0.4f, w * 0.35f,
                        0x559C27B0, 0x00000000, Shader.TileMode.CLAMP);
                p.setShader(nebula1);
                canvas.drawCircle(w * 0.3f, h * 0.4f, w * 0.35f, p);
                RadialGradient nebula2 = new RadialGradient(w * 0.75f, h * 0.6f, w * 0.3f,
                        0x4400E5FF, 0x00000000, Shader.TileMode.CLAMP);
                p.setShader(nebula2);
                canvas.drawCircle(w * 0.75f, h * 0.6f, w * 0.3f, p);
                p.setShader(null);
                p.setColor(Color.WHITE);
                for (int i = 0; i < 40; i++) {
                    float sx = ((i * 137) % w);
                    float sy = ((i * 269) % h);
                    canvas.drawCircle(sx, sy, (i % 3 == 0)  ?  dp(1.8f) : dp(1f), p);
                }
                break;
            }
            case 2: { // Anime Mech
                LinearGradient g = new LinearGradient(0, 0, w, h,
                        new int[]{0xFF14171E, 0xFF2A1116, 0xFF0D0F14}, null, Shader.TileMode.CLAMP);
                p.setShader(g);
                canvas.drawRect(0, 0, w, h, p);
                p.setShader(null);
                Path path = new Path();
                p.setColor(0x35FF1744);
                p.setStyle(Paint.Style.FILL);
                path.moveTo(w * 0.6f, 0); path.lineTo(w, 0); path.lineTo(w, h * 0.7f); path.lineTo(w * 0.75f, h); path.close();
                canvas.drawPath(path, p);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(dp(2));
                p.setColor(0xFFFF1744);
                canvas.drawLine(w * 0.6f, 0, w * 0.75f, h, p);
                p.setColor(0xFF00E5FF);
                canvas.drawLine(w * 0.65f, 0, w * 0.80f, h, p);
                p.setStyle(Paint.Style.FILL);
                break;
            }
            case 3: { // Synthwave 80s
                LinearGradient g = new LinearGradient(0, 0, 0, h,
                        new int[]{0xFF12002B, 0xFF4A0E4E, 0xFF0D0221}, null, Shader.TileMode.CLAMP);
                p.setShader(g);
                canvas.drawRect(0, 0, w, h, p);
                p.setShader(null);
                RadialGradient sun = new RadialGradient(w * 0.5f, h * 0.5f, h * 0.28f,
                        new int[]{0xFFFFEE58, 0xFFFF007F, 0x00FF007F}, null, Shader.TileMode.CLAMP);
                p.setShader(sun);
                canvas.drawCircle(w * 0.5f, h * 0.5f, h * 0.28f, p);
                p.setShader(null);
                p.setColor(0xFF1A062E);
                for (float y = h * 0.42f; y < h * 0.65f; y += dp(7)) {
                    canvas.drawRect(w * 0.3f, y, w * 0.7f, y + dp(2.5f), p);
                }
                p.setColor(0x5500F0FF);
                p.setStrokeWidth(dp(1.2f));
                for (float y = h * 0.65f; y <= h; y += Math.max(1f, (h * 0.35f) / 5f)) {
                    canvas.drawLine(0, y, w, y, p);
                }
                for (int x = 0; x <= w; x += Math.max(1, w / 10)) {
                    canvas.drawLine(w * 0.5f, h * 0.65f, x, h, p);
                }
                break;
            }
            case 4: { // REVROR Core
                LinearGradient g = new LinearGradient(0, 0, w, h,
                        new int[]{0xFF050508, 0xFF1F050A, 0xFF050508}, null, Shader.TileMode.CLAMP);
                p.setShader(g);
                canvas.drawRect(0, 0, w, h, p);
                p.setShader(null);
                p.setColor(0x28FF1744);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(dp(14));
                canvas.drawCircle(w * 0.5f, h * 0.5f, h * 0.42f, p);
                p.setColor(0x77FF1744);
                p.setStrokeWidth(dp(3));
                canvas.drawCircle(w * 0.5f, h * 0.5f, h * 0.35f, p);
                p.setColor(0xFFFF1744);
                p.setStrokeWidth(dp(1.5f));
                canvas.drawCircle(w * 0.5f, h * 0.5f, h * 0.22f, p);
                p.setStyle(Paint.Style.FILL);
                break;
            }
            case 5: { // Carbon Stealth
                p.setColor(0xFF0F1014);
                canvas.drawRect(0, 0, w, h, p);
                p.setColor(0xFF1B1D24);
                float step = dp(14);
                for (float x = 0; x < w; x += step) {
                    for (float y = 0; y < h; y += step) {
                        if (((int)(x / step) + (int)(y / step)) % 2 == 0) {
                            canvas.drawRect(x, y, x + step, y + step, p);
                        }
                    }
                }
                p.setColor(0x44FF9100);
                p.setStrokeWidth(dp(2));
                canvas.drawLine(0, h * 0.7f, w, h * 0.7f, p);
                break;
            }
            case 6: { // Fiery Dragon
                LinearGradient g = new LinearGradient(0, 0, w, h,
                        new int[]{0xFF0E0400, 0xFF350B00, 0xFF5D1700, 0xFF0A0200}, null, Shader.TileMode.CLAMP);
                p.setShader(g);
                canvas.drawRect(0, 0, w, h, p);
                p.setShader(null);
                RadialGradient flame = new RadialGradient(w * 0.6f, h * 0.5f, w * 0.4f,
                        0x66FF6D00, 0x00000000, Shader.TileMode.CLAMP);
                p.setShader(flame);
                canvas.drawCircle(w * 0.6f, h * 0.5f, w * 0.4f, p);
                p.setShader(null);
                break;
            }
            case 7: { // Neon Grid
                p.setColor(0xFF04060E);
                canvas.drawRect(0, 0, w, h, p);
                p.setColor(0x4400E5FF);
                p.setStrokeWidth(dp(1f));
                float gridSz = dp(32);
                for (float x = 0; x < w; x += gridSz) canvas.drawLine(x, 0, x, h, p);
                for (float y = 0; y < h; y += gridSz) canvas.drawLine(0, y, w, y, p);
                RadialGradient glow = new RadialGradient(w * 0.5f, h * 0.5f, h * 0.6f,
                        0x4400E5FF, 0x00000000, Shader.TileMode.CLAMP);
                p.setShader(glow);
                canvas.drawRect(0, 0, w, h, p);
                p.setShader(null);
                break;
            }
            case 8: { // Matrix Digital
                p.setColor(0xFF020703);
                canvas.drawRect(0, 0, w, h, p);
                p.setColor(0x6600E676);
                p.setTextSize(dp(10));
                for (int x = 0; x < w; x += dp(24)) {
                    for (int y = dp(14); y < h; y += dp(16)) {
                        if (((x * 7 + y * 13) % 5) < 3) {
                            canvas.drawText(((x + y) % 2 == 0)  ?  "1" : "0", x, y, p);
                        }
                    }
                }
                break;
            }
            case 9: { // Abyss Blue
                LinearGradient g = new LinearGradient(0, 0, 0, h,
                        new int[]{0xFF020B14, 0xFF062238, 0xFF020B14}, null, Shader.TileMode.CLAMP);
                p.setShader(g);
                canvas.drawRect(0, 0, w, h, p);
                p.setShader(null);
                RadialGradient glow = new RadialGradient(w * 0.45f, h * 0.55f, w * 0.35f,
                        0x5500B0FF, 0x00000000, Shader.TileMode.CLAMP);
                p.setShader(glow);
                canvas.drawCircle(w * 0.45f, h * 0.55f, w * 0.35f, p);
                p.setShader(null);
                break;
            }
            case 10: { // Royal Gold
                LinearGradient g = new LinearGradient(0, 0, w, h,
                        new int[]{0xFF0F0B05, 0xFF2A1F0D, 0xFF140F06}, null, Shader.TileMode.CLAMP);
                p.setShader(g);
                canvas.drawRect(0, 0, w, h, p);
                p.setShader(null);
                p.setColor(0x44FFD700);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(dp(2));
                canvas.drawCircle(w * 0.5f, h * 0.5f, h * 0.36f, p);
                canvas.drawRect(w * 0.4f, h * 0.3f, w * 0.6f, h * 0.7f, p);
                p.setStyle(Paint.Style.FILL);
                break;
            }
            case 11: { // Stealth Carbon
                p.setColor(0xFF0F1014);
                canvas.drawRect(0, 0, w, h, p);
                p.setColor(0xFF1B1D24);
                float step = dp(14);
                for (float x = 0; x < w; x += step) {
                    for (float y = 0; y < h; y += step) {
                        if (((int)(x / step) + (int)(y / step)) % 2 == 0) {
                            canvas.drawRect(x, y, x + step, y + step, p);
                        }
                    }
                }
                p.setColor(0x44FF9100);
                p.setStrokeWidth(dp(2));
                canvas.drawLine(0, h * 0.7f, w, h * 0.7f, p);
                break;
            }
            case 12: { // Anime Cyber Girl (Aika)
                LinearGradient g = new LinearGradient(0, 0, w, h,
                        new int[]{0xFF0A081E, 0xFF18103A, 0xFF0D0B24}, null, Shader.TileMode.CLAMP);
                p.setShader(g);
                canvas.drawRect(0, 0, w, h, p);
                p.setShader(null);
                p.setColor(0x8800E5FF);
                p.setStrokeWidth(dp(4));
                canvas.drawLine(w * 0.35f, h * 0.2f, w * 0.28f, h * 0.9f, p);
                canvas.drawLine(w * 0.65f, h * 0.2f, w * 0.72f, h * 0.9f, p);
                p.setColor(0xFFFF007F);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(dp(2.5f));
                Path ear1 = new Path();
                ear1.moveTo(w * 0.42f, h * 0.35f); ear1.lineTo(w * 0.38f, h * 0.15f); ear1.lineTo(w * 0.46f, h * 0.25f);
                canvas.drawPath(ear1, p);
                Path ear2 = new Path();
                ear2.moveTo(w * 0.58f, h * 0.35f); ear2.lineTo(w * 0.62f, h * 0.15f); ear2.lineTo(w * 0.54f, h * 0.25f);
                canvas.drawPath(ear2, p);
                p.setStyle(Paint.Style.FILL);
                p.setColor(0xFF00E5FF);
                canvas.drawCircle(w * 0.45f, h * 0.45f, dp(3), p);
                canvas.drawCircle(w * 0.55f, h * 0.45f, dp(3), p);
                break;
            }
            case 13: { // Shadow Monarch (Jin-woo)
                LinearGradient g = new LinearGradient(0, 0, 0, h,
                        new int[]{0xFF05030A, 0xFF140728, 0xFF080214}, null, Shader.TileMode.CLAMP);
                p.setShader(g);
                canvas.drawRect(0, 0, w, h, p);
                p.setShader(null);
                RadialGradient mist = new RadialGradient(w * 0.5f, h * 0.6f, w * 0.4f,
                        0x667C4DFF, 0x00000000, Shader.TileMode.CLAMP);
                p.setShader(mist);
                canvas.drawCircle(w * 0.5f, h * 0.6f, w * 0.4f, p);
                p.setShader(null);
                p.setColor(0xFF00E5FF);
                p.setStrokeWidth(dp(2.5f));
                canvas.drawLine(w * 0.44f, h * 0.45f, w * 0.35f, h * 0.35f, p);
                canvas.drawLine(w * 0.56f, h * 0.45f, w * 0.65f, h * 0.35f, p);
                break;
            }
            case 14: { // Demon Nichirin Flame (Rengoku)
                LinearGradient g = new LinearGradient(0, 0, w, h,
                        new int[]{0xFF1A0502, 0xFF4A0E00, 0xFF150400}, null, Shader.TileMode.CLAMP);
                p.setShader(g);
                canvas.drawRect(0, 0, w, h, p);
                p.setShader(null);
                p.setColor(0xFFFF3D00);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(dp(6));
                Path flameArc = new Path();
                flameArc.moveTo(w * 0.1f, h * 0.9f);
                flameArc.quadTo(w * 0.5f, h * 0.1f, w * 0.9f, h * 0.7f);
                canvas.drawPath(flameArc, p);
                p.setColor(0xFFFFEA00);
                p.setStrokeWidth(dp(2.5f));
                canvas.drawPath(flameArc, p);
                p.setStyle(Paint.Style.FILL);
                break;
            }
            case 15: { // Gojo Limitless Void
                LinearGradient g = new LinearGradient(0, 0, 0, h,
                        new int[]{0xFF030510, 0xFF0D0B20, 0xFF040614}, null, Shader.TileMode.CLAMP);
                p.setShader(g);
                canvas.drawRect(0, 0, w, h, p);
                p.setShader(null);
                RadialGradient rOrb = new RadialGradient(w * 0.3f, h * 0.5f, dp(22),
                        0xFFFF1744, 0x00000000, Shader.TileMode.CLAMP);
                p.setShader(rOrb);
                canvas.drawCircle(w * 0.3f, h * 0.5f, dp(22), p);
                RadialGradient bOrb = new RadialGradient(w * 0.7f, h * 0.5f, dp(22),
                        0xFF00B0FF, 0x00000000, Shader.TileMode.CLAMP);
                p.setShader(bOrb);
                canvas.drawCircle(w * 0.7f, h * 0.5f, dp(22), p);
                p.setShader(null);
                p.setColor(0xFFD500F9);
                p.setStrokeWidth(dp(2));
                canvas.drawLine(w * 0.48f, 0, w * 0.52f, h, p);
                break;
            }
            case 16: { // Cyber Samurai Waifu
                LinearGradient g = new LinearGradient(0, 0, w, h,
                        new int[]{0xFF0B1117, 0xFF19222E, 0xFF0E161F}, null, Shader.TileMode.CLAMP);
                p.setShader(g);
                canvas.drawRect(0, 0, w, h, p);
                p.setShader(null);
                p.setColor(0xFF00E5FF);
                p.setStrokeWidth(dp(4));
                canvas.drawLine(w * 0.2f, h * 0.85f, w * 0.8f, h * 0.15f, p);
                p.setColor(Color.WHITE);
                p.setStrokeWidth(dp(1.5f));
                canvas.drawLine(w * 0.2f, h * 0.85f, w * 0.8f, h * 0.15f, p);
                p.setColor(0xFFFF80AB);
                canvas.drawCircle(w * 0.35f, h * 0.3f, dp(2.5f), p);
                canvas.drawCircle(w * 0.65f, h * 0.7f, dp(3f), p);
                canvas.drawCircle(w * 0.75f, h * 0.4f, dp(2f), p);
                break;
            }
            case 17: { // Ultra Instinct Goku
                LinearGradient g = new LinearGradient(0, 0, 0, h,
                        new int[]{0xFF050714, 0xFF121B33, 0xFF070B1A}, null, Shader.TileMode.CLAMP);
                p.setShader(g);
                canvas.drawRect(0, 0, w, h, p);
                p.setShader(null);
                RadialGradient aura = new RadialGradient(w * 0.5f, h * 0.5f, h * 0.45f,
                        0x6680D8FF, 0x00000000, Shader.TileMode.CLAMP);
                p.setShader(aura);
                canvas.drawCircle(w * 0.5f, h * 0.5f, h * 0.45f, p);
                p.setShader(null);
                p.setColor(Color.WHITE);
                for (int i = 0; i < 20; i++) {
                    canvas.drawCircle((i * 47) % w, (i * 29) % h, dp(1.2f), p);
                }
                break;
            }
            case 18: { // Chibi Neko Gamer
                LinearGradient g = new LinearGradient(0, 0, w, h,
                        new int[]{0xFF160A24, 0xFF2B1038, 0xFF180829}, null, Shader.TileMode.CLAMP);
                p.setShader(g);
                canvas.drawRect(0, 0, w, h, p);
                p.setShader(null);
                p.setColor(0xFFFF4081);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(dp(3.5f));
                canvas.drawArc(w * 0.38f, h * 0.25f, w * 0.62f, h * 0.65f, 180, 180, false, p);
                Path nEar = new Path();
                nEar.moveTo(w * 0.42f, h * 0.32f); nEar.lineTo(w * 0.40f, h * 0.18f); nEar.lineTo(w * 0.46f, h * 0.26f);
                canvas.drawPath(nEar, p);
                Path nEar2 = new Path();
                nEar2.moveTo(w * 0.58f, h * 0.32f); nEar2.lineTo(w * 0.60f, h * 0.18f); nEar2.lineTo(w * 0.54f, h * 0.26f);
                canvas.drawPath(nEar2, p);
                p.setStyle(Paint.Style.FILL);
                break;
            }
            case 19: { // Berserk Eclipse
                LinearGradient g = new LinearGradient(0, 0, 0, h,
                        new int[]{0xFF0E0202, 0xFF240505, 0xFF080101}, null, Shader.TileMode.CLAMP);
                p.setShader(g);
                canvas.drawRect(0, 0, w, h, p);
                p.setShader(null);
                RadialGradient eclipse = new RadialGradient(w * 0.5f, h * 0.45f, h * 0.35f,
                        0xFFFF3D00, 0x00000000, Shader.TileMode.CLAMP);
                p.setShader(eclipse);
                canvas.drawCircle(w * 0.5f, h * 0.45f, h * 0.35f, p);
                p.setShader(null);
                p.setColor(0xFF0A0202);
                canvas.drawCircle(w * 0.5f, h * 0.45f, h * 0.28f, p);
                p.setColor(0xFFFF1744);
                p.setStrokeWidth(dp(2));
                canvas.drawLine(w * 0.5f, h * 0.35f, w * 0.5f, h * 0.55f, p);
                break;
            }
            case 20: { // Persona Phantom Thief
                p.setColor(0xFF141419);
                canvas.drawRect(0, 0, w, h, p);
                p.setColor(0xFFD50000);
                Path split = new Path();
                split.moveTo(w * 0.4f, 0); split.lineTo(w, 0); split.lineTo(w * 0.6f, h); split.lineTo(0, h); split.close();
                canvas.drawPath(split, p);
                p.setColor(Color.WHITE);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(dp(2.5f));
                canvas.drawOval(w * 0.38f, h * 0.42f, w * 0.62f, h * 0.58f, p);
                p.setStyle(Paint.Style.FILL);
                p.setColor(0xFFFFD600);
                canvas.drawCircle(w * 0.46f, h * 0.5f, dp(3), p);
                canvas.drawCircle(w * 0.54f, h * 0.5f, dp(3), p);
                break;
            }
            case 21: { // Gundam Strike Freedom
                LinearGradient g = new LinearGradient(0, 0, 0, h,
                        new int[]{0xFF060918, 0xFF0F1A3A, 0xFF080D21}, null, Shader.TileMode.CLAMP);
                p.setShader(g);
                canvas.drawRect(0, 0, w, h, p);
                p.setShader(null);
                p.setColor(0xFFFFD700);
                p.setStrokeWidth(dp(3));
                canvas.drawLine(w * 0.5f, h * 0.5f, w * 0.15f, h * 0.2f, p);
                canvas.drawLine(w * 0.5f, h * 0.5f, w * 0.85f, h * 0.2f, p);
                canvas.drawLine(w * 0.5f, h * 0.5f, w * 0.18f, h * 0.75f, p);
                canvas.drawLine(w * 0.5f, h * 0.5f, w * 0.82f, h * 0.75f, p);
                p.setColor(0xFFFFEA00);
                Path vfin = new Path();
                vfin.moveTo(w * 0.5f, h * 0.45f); vfin.lineTo(w * 0.44f, h * 0.32f);
                vfin.moveTo(w * 0.5f, h * 0.45f); vfin.lineTo(w * 0.56f, h * 0.32f);
                p.setStyle(Paint.Style.STROKE);
                canvas.drawPath(vfin, p);
                p.setStyle(Paint.Style.FILL);
                break;
            }
            case 22: { // Cyber Void Valkyrie
                LinearGradient g = new LinearGradient(0, 0, w, h,
                        new int[]{0xFF070F1A, 0xFF122238, 0xFF0A1524}, null, Shader.TileMode.CLAMP);
                p.setShader(g);
                canvas.drawRect(0, 0, w, h, p);
                p.setShader(null);
                p.setColor(0xFFFFD700);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(dp(2));
                canvas.drawOval(w * 0.42f, h * 0.22f, w * 0.58f, h * 0.32f, p);
                p.setColor(0x6600E5FF);
                p.setStrokeWidth(dp(3));
                canvas.drawLine(w * 0.5f, h * 0.45f, w * 0.25f, h * 0.35f, p);
                canvas.drawLine(w * 0.5f, h * 0.45f, w * 0.75f, h * 0.35f, p);
                p.setStyle(Paint.Style.FILL);
                break;
            }
            case 23: { // Arcane Jinx Supernova
                LinearGradient g = new LinearGradient(0, 0, w, h,
                        new int[]{0xFF07140C, 0xFF1C0A28, 0xFF0D0618}, null, Shader.TileMode.CLAMP);
                p.setShader(g);
                canvas.drawRect(0, 0, w, h, p);
                p.setShader(null);
                p.setColor(0xFFFF007F);
                p.setStrokeWidth(dp(3));
                canvas.drawLine(w * 0.2f, h * 0.3f, w * 0.7f, h * 0.8f, p);
                p.setColor(0xFF00E5FF);
                canvas.drawLine(w * 0.3f, h * 0.8f, w * 0.8f, h * 0.25f, p);
                p.setColor(0xFFFFD600);
                canvas.drawCircle(w * 0.6f, h * 0.4f, dp(3), p);
                canvas.drawCircle(w * 0.4f, h * 0.6f, dp(2), p);
                break;
            }
            default: { // Sakura Glow
                LinearGradient g = new LinearGradient(0, 0, w, h,
                        new int[]{0xFF120815, 0xFF28112C, 0xFF190B1E}, null, Shader.TileMode.CLAMP);
                p.setShader(g);
                canvas.drawRect(0, 0, w, h, p);
                p.setShader(null);
                RadialGradient moon = new RadialGradient(w * 0.75f, h * 0.35f, h * 0.3f,
                        0x66FF80AB, 0x00000000, Shader.TileMode.CLAMP);
                p.setShader(moon);
                canvas.drawCircle(w * 0.75f, h * 0.35f, h * 0.3f, p);
                p.setShader(null);
                break;
            }
        }
        return bm;
    }

    private Bitmap fastBlur(Bitmap sentBitmap, int radius) {
        if (radius <= 0) return sentBitmap;
        try {
            int w = Math.max(1, sentBitmap.getWidth() / 2);
            int h = Math.max(1, sentBitmap.getHeight() / 2);
            Bitmap small = Bitmap.createScaledBitmap(sentBitmap, w, h, true);
            int[] pix = new int[w * h];
            small.getPixels(pix, 0, w, 0, 0, w, h);

            int wm = w - 1;
            int hm = h - 1;
            int wh = w * h;
            int div = radius + radius + 1;

            int[] r = new int[wh];
            int[] g = new int[wh];
            int[] b = new int[wh];
            int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
            int[] vmin = new int[Math.max(w, h)];

            int divsum = (div + 1) >> 1;
            divsum *= divsum;
            int[] dv = new int[256 * divsum];
            for (i = 0; i < 256 * divsum; i++) {
                dv[i] = (i / divsum);
            }

            yw = yi = 0;
            int[][] stack = new int[div][3];
            int stackpointer;
            int stackstart;
            int[] sir;
            int rbs;
            int r1 = radius + 1;
            int routsum, goutsum, boutsum;
            int rinsum, ginsum, binsum;

            for (y = 0; y < h; y++) {
                rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
                for (i = -radius; i <= radius; i++) {
                    p = pix[yi + Math.min(wm, Math.max(i, 0))];
                    sir = stack[i + radius];
                    sir[0] = (p & 0xff0000) >> 16;
                    sir[1] = (p & 0x00ff00) >> 8;
                    sir[2] = (p & 0x0000ff);
                    rbs = r1 - Math.abs(i);
                    rsum += sir[0] * rbs;
                    gsum += sir[1] * rbs;
                    bsum += sir[2] * rbs;
                    if (i > 0) {
                        rinsum += sir[0];
                        ginsum += sir[1];
                        binsum += sir[2];
                    } else {
                        routsum += sir[0];
                        goutsum += sir[1];
                        boutsum += sir[2];
                    }
                }
                stackpointer = radius;

                for (x = 0; x < w; x++) {
                    r[yi] = dv[rsum];
                    g[yi] = dv[gsum];
                    b[yi] = dv[bsum];

                    rsum -= routsum;
                    gsum -= goutsum;
                    bsum -= boutsum;

                    stackstart = stackpointer - radius + div;
                    sir = stack[stackstart % div];

                    routsum -= sir[0];
                    goutsum -= sir[1];
                    boutsum -= sir[2];

                    if (y == 0) {
                        vmin[x] = Math.min(x + radius + 1, wm);
                    }
                    p = pix[yw + vmin[x]];

                    sir[0] = (p & 0xff0000) >> 16;
                    sir[1] = (p & 0x00ff00) >> 8;
                    sir[2] = (p & 0x0000ff);

                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];

                    rsum += rinsum;
                    gsum += ginsum;
                    bsum += binsum;

                    stackpointer = (stackpointer + 1) % div;
                    sir = stack[(stackpointer) % div];

                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];

                    rinsum -= sir[0];
                    ginsum -= sir[1];
                    binsum += sir[2];

                    yi++;
                }
                yw += w;
            }
            for (x = 0; x < w; x++) {
                rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
                yp = -radius * w;
                for (i = -radius; i <= radius; i++) {
                    yi = Math.max(0, yp) + x;
                    sir = stack[i + radius];
                    sir[0] = r[yi];
                    sir[1] = g[yi];
                    sir[2] = b[yi];
                    rbs = r1 - Math.abs(i);
                    rsum += r[yi] * rbs;
                    gsum += g[yi] * rbs;
                    bsum += b[yi] * rbs;
                    if (i > 0) {
                        rinsum += sir[0];
                        ginsum += sir[1];
                        binsum += sir[2];
                    } else {
                        routsum += sir[0];
                        goutsum += sir[1];
                        boutsum += sir[2];
                    }
                    if (i < hm) {
                        yp += w;
                    }
                }
                yi = x;
                stackpointer = radius;
                for (y = 0; y < h; y++) {
                    pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];
                    rsum -= routsum;
                    gsum -= goutsum;
                    bsum -= boutsum;

                    stackstart = stackpointer - radius + div;
                    sir = stack[stackstart % div];

                    routsum -= sir[0];
                    goutsum -= sir[1];
                    boutsum -= sir[2];

                    if (x == 0) {
                        vmin[y] = Math.min(y + r1, hm) * w;
                    }
                    p = x + vmin[y];

                    sir[0] = r[p];
                    sir[1] = g[p];
                    sir[2] = b[p];

                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];

                    rsum += rinsum;
                    gsum += ginsum;
                    bsum += binsum;

                    stackpointer = (stackpointer + 1) % div;
                    sir = stack[stackpointer];

                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];

                    rinsum -= sir[0];
                    ginsum -= sir[1];
                    binsum += sir[2];

                    yi += w;
                }
            }
            small.setPixels(pix, 0, w, 0, 0, w, h);
            return small;
        } catch (Exception e) {
            return sentBitmap;
        }
    }

    private Drawable getActiveWallpaperDrawable() {
        try {
            Bitmap src = null;
            if (activeBannerId >= 100) {
                int idx = activeBannerId - 100;
                if (idx >= 0 && idx < customBannersList.size()) {
                    Uri u = Uri.parse(customBannersList.get(idx));
                    src = BitmapFactory.decodeStream(getContentResolver().openInputStream(u));
                }
            }
            if (src == null) {
                src = drawBannerBitmap(activeBannerId, 960, 540);
            }
            if (bannerBlur > 0) {
                src = fastBlur(src, bannerBlur);
            }
            return new BitmapDrawable(getResources(), src);
        } catch (Exception e) {
            return new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{0xFF050506, 0xFF111114, 0xFF070708});
        }
    }

    private Runnable heroBadgesBlinker = null;
    private static final int[] HERO_DOT_COLORS = new int[]{
            0xFF00FF66, 0xFF00E5FF, 0xFFFF1744, 0xFFFF007F, 0xFFFFB800,
            0xFF9D00FF, 0xFF00FFCC, 0xFF3D5AFE, 0xFFFF5252, 0xFFE040FB
    };

    private void updateHeroBadgesWithBlinkingDot(BannerTextStyleRegistry.BannerTextStyle style) {
        if (heroBadges == null) return;
        if (heroBadgesBlinker != null) {
            handler.removeCallbacks(heroBadgesBlinker);
            heroBadgesBlinker = null;
        }
        Random rng = new Random();
        heroBadgesBlinker = new Runnable() {
            private boolean visible = true;
            @Override public void run() {
                if (heroBadges == null || isFinishing() || isDestroyed()) return;
                int dotColor = visible  ?  HERO_DOT_COLORS[rng.nextInt(HERO_DOT_COLORS.length)] : 0x22888888;
                SpannableStringBuilder ssb = new SpannableStringBuilder();
                ssb.append("?  ", new ForegroundColorSpan(dotColor), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.append(style.pillText.trim() + "   |   " + style.subtitle);
                heroBadges.setText(ssb);
                visible = !visible;
                handler.postDelayed(this, 850);
            }
        };
        handler.post(heroBadgesBlinker);
    }

    private void updateActiveBannerTextStyle() {
        BannerTextStyleRegistry.BannerTextStyle style = BannerTextStyleRegistry.getActiveStyle(this);
        if (heroBadges != null) {
            heroBadges.setCompoundDrawables(null, null, null, null);
            heroBadges.setCompoundDrawablePadding(0);
            updateHeroBadgesWithBlinkingDot(style);
            heroBadges.setTextColor(style.pillTextColor);
            heroBadges.setOnClickListener(v -> {
                try {
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/revWS")));
                } catch (Exception ignored) {}
            });
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setColor(style.pillBgColor);
            badgeBg.setCornerRadius(dp(8));
            badgeBg.setStroke(dp(1), style.pillStrokeColor);
            heroBadges.setBackground(badgeBg);
        }
        if (statusText != null) {
            statusText.setTextColor(style.subtitleColor);
        }
    }

    public void showThemeAndBannerDialog() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF412141D);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), gameSpaceAccent);
        root.setBackground(bg);

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(12));

        TextView title = label("THEMES & BANNERS", 16, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setLetterSpacing(0.04f);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView closeBtn = label("CLOSE", 10, 0xFFB0B5C6);
        applyLeadingIcon(closeBtn, R.drawable.ic_window_close, 0xFFB0B5C6, 16);
        closeBtn.setTypeface(null, Typeface.BOLD);
        closeBtn.setGravity(Gravity.CENTER);
        closeBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        header.addView(closeBtn, new LinearLayout.LayoutParams(-2, -2));
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        // Section 1: Accent Colors
        TextView colTitle = label("GAME SPACE ACCENT COLOR", 11.5f, 0xFF8E95AF);
        colTitle.setTypeface(null, Typeface.BOLD);
        colTitle.setLetterSpacing(0.08f);
        colTitle.setPadding(0, dp(4), 0, dp(8));
        content.addView(colTitle);

        HorizontalScrollView colScroll = new HorizontalScrollView(this);
        colScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout colRow = new LinearLayout(this);
        colRow.setOrientation(LinearLayout.HORIZONTAL);
        colRow.setPadding(0, dp(4), 0, dp(12));

        Runnable[] refreshAccentRow = new Runnable[1];
        refreshAccentRow[0] = () -> {
            colRow.removeAllViews();
            for (int i = 0; i < GAME_SPACE_AACCENTS.length; i++) {
                final int color = GAME_SPACE_AACCENTS[i];
                final String name = GAME_SPACE_ACCENT_NAMES[i];
                boolean active = (color == gameSpaceAccent);

                LinearLayout chip = new LinearLayout(this);
                chip.setOrientation(LinearLayout.HORIZONTAL);
                chip.setGravity(Gravity.CENTER_VERTICAL);
                chip.setPadding(dp(8), dp(6), dp(12), dp(6));

                GradientDrawable cBg = new GradientDrawable();
                cBg.setColor(active  ?  (color & 0x33FFFFFF) : 0xFF1D202A);
                cBg.setCornerRadius(dp(16));
                cBg.setStroke(dp(active  ?  2 : 1), active  ?  color : 0xFF35394A);
                chip.setBackground(cBg);

                View dot = new View(this);
                GradientDrawable dBg = new GradientDrawable();
                dBg.setShape(GradientDrawable.OVAL);
                dBg.setColor(color);
                dot.setBackground(dBg);
                chip.addView(dot, new LinearLayout.LayoutParams(dp(14), dp(14)));

                TextView nView = label(name, 10f, active  ?  Color.WHITE : 0xFF9E9EA8);
                nView.setTypeface(null, Typeface.BOLD);
                nView.setPadding(dp(6), 0, 0, 0);
                chip.addView(nView);

                LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(-2, -2);
                cLp.setMarginEnd(dp(8));
                chip.setLayoutParams(cLp);

                chip.setOnClickListener(v -> {
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    gameSpaceAccent = color;
                    savePreferences();
                    refreshAccentRow[0].run();
                    GradientDrawable newBg = new GradientDrawable();
                    newBg.setColor(0xF412141D);
                    newBg.setCornerRadius(dp(18));
                    newBg.setStroke(dp(1), gameSpaceAccent);
                    root.setBackground(newBg);
                    Toast.makeText(this, name + " accent applied", Toast.LENGTH_SHORT).show();
                });
                colRow.addView(chip);
            }
        };
        refreshAccentRow[0].run();
        colScroll.addView(colRow, new FrameLayout.LayoutParams(-2, -2));
        content.addView(colScroll);

        // Section 2: Banner Wallpapers Grid (12 presets + custom + add custom)
        TextView banTitle = label("BANNER WALLPAPERS (PRESETS & GALLERY)", 11.5f, 0xFF8E95AF);
        banTitle.setTypeface(null, Typeface.BOLD);
        banTitle.setLetterSpacing(0.08f);
        banTitle.setPadding(0, dp(8), 0, dp(8));
        content.addView(banTitle);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        grid.setUseDefaultMargins(true);

        Runnable[] refreshGrid = new Runnable[1];
        refreshGrid[0] = () -> {
            grid.removeAllViews();
            int totalPresets = BANNER_TITLES.length;
            int totalCustom = customBannersList.size();

            // Preset items
            for (int i = 0; i < totalPresets; i++) {
                final int bId = i;
                final String bTitle = BANNER_TITLES[i];
                boolean active = (activeBannerId == bId);

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setGravity(Gravity.CENTER_HORIZONTAL);
                card.setPadding(dp(4), dp(4), dp(4), dp(4));

                GradientDrawable cardBg = new GradientDrawable();
                cardBg.setColor(active  ?  (gameSpaceAccent & 0x33FFFFFF) : 0xFF191B24);
                cardBg.setCornerRadius(dp(10));
                cardBg.setStroke(dp(active  ?  2 : 1), active  ?  gameSpaceAccent : 0xFF35394A);
                card.setBackground(cardBg);

                ImageView thumb = new ImageView(this);
                thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
                thumb.setImageBitmap(drawBannerBitmap(bId, 160, 90));
                card.addView(thumb, new LinearLayout.LayoutParams(dp(130), dp(72)));

                TextView bLabel = label(bTitle, 9f, active  ?  Color.WHITE : 0xFF9E9EA8);
                bLabel.setTypeface(null, Typeface.BOLD);
                bLabel.setPadding(0, dp(4), 0, dp(2));
                card.addView(bLabel);

                GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
                glp.width = dp(138);
                glp.height = -2;
                glp.setMargins(dp(4), dp(4), dp(4), dp(4));
                card.setLayoutParams(glp);

                card.setOnClickListener(v -> {
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    activeBannerId = bId;
                    savePreferences();
                    refreshGrid[0].run();
                    if (animatedBannerView != null) {
                        animatedBannerView.setBanner(activeBannerId, null, bannerBlur);
                    }
                    Toast.makeText(this, bTitle + " banner selected", Toast.LENGTH_SHORT).show();
                });
                grid.addView(card);
            }

            // Custom banners from customBannersList
            for (int c = 0; c < totalCustom; c++) {
                final int cIndex = c;
                final int customId = 100 + c;
                boolean active = (activeBannerId == customId);

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setGravity(Gravity.CENTER_HORIZONTAL);
                card.setPadding(dp(4), dp(4), dp(4), dp(4));

                GradientDrawable cardBg = new GradientDrawable();
                cardBg.setColor(active  ?  (gameSpaceAccent & 0x33FFFFFF) : 0xFF191B24);
                cardBg.setCornerRadius(dp(10));
                cardBg.setStroke(dp(active  ?  2 : 1), active  ?  gameSpaceAccent : 0xFF35394A);
                card.setBackground(cardBg);

                ImageView thumb = new ImageView(this);
                thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
                try {
                    Uri u = Uri.parse(customBannersList.get(c));
                    thumb.setImageURI(u);
                } catch (Exception e) {
                    thumb.setImageBitmap(drawBannerBitmap(0, 160, 90));
                }
                card.addView(thumb, new LinearLayout.LayoutParams(dp(130), dp(72)));

                TextView bLabel = label("Custom #" + (cIndex + 1), 9f, active  ?  Color.WHITE : 0xFF9E9EA8);
                bLabel.setTypeface(null, Typeface.BOLD);
                bLabel.setPadding(0, dp(4), 0, dp(2));
                card.addView(bLabel);

                GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
                glp.width = dp(138);
                glp.height = -2;
                glp.setMargins(dp(4), dp(4), dp(4), dp(4));
                card.setLayoutParams(glp);

                card.setOnClickListener(v -> {
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    activeBannerId = customId;
                    savePreferences();
                    refreshGrid[0].run();
                    if (animatedBannerView != null && cIndex < customBannersList.size()) {
                        animatedBannerView.setBanner(activeBannerId, customBannersList.get(cIndex), bannerBlur);
                    }
                    Toast.makeText(this, "Custom banner #" + (cIndex + 1) + " selected", Toast.LENGTH_SHORT).show();
                });
                grid.addView(card);
            }

            // Add Custom Banner Button Card
            LinearLayout addCard = new LinearLayout(this);
            addCard.setOrientation(LinearLayout.VERTICAL);
            addCard.setGravity(Gravity.CENTER);
            addCard.setPadding(dp(4), dp(4), dp(4), dp(4));

            GradientDrawable addBg = new GradientDrawable();
            addBg.setColor(0xFF161821);
            addBg.setCornerRadius(dp(10));
            addBg.setStroke(dp(1), 0xFF444A60);
            addCard.setBackground(addBg);

            TextView addPlus = label("+", 26f, 0xFFFF3347);
            addPlus.setTypeface(null, Typeface.BOLD);
            addCard.addView(addPlus);

            TextView addTxt = label("ADD BANNER", 9f, Color.WHITE);
            addTxt.setTypeface(null, Typeface.BOLD);
            addCard.addView(addTxt);

            GridLayout.LayoutParams addLp = new GridLayout.LayoutParams();
            addLp.width = dp(138);
            addLp.height = dp(96);
            addLp.setMargins(dp(4), dp(4), dp(4), dp(4));
            addCard.setLayoutParams(addLp);

            addCard.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                pickCustomBanner();
                dialog.dismiss();
            });
            grid.addView(addCard);
        };
        refreshGrid[0].run();
        content.addView(grid);

        // Section 3: Banner Text Styles (24 Styles with different colors & fonts)
        TextView txtStyleTitle = label("BANNER TEXT STYLES (24 NEON & GAMING PRESETS)", 11.5f, 0xFF8E95AF);
        txtStyleTitle.setTypeface(null, Typeface.BOLD);
        txtStyleTitle.setLetterSpacing(0.08f);
        txtStyleTitle.setPadding(0, dp(14), 0, dp(6));
        content.addView(txtStyleTitle);

        LinearLayout txtActionRow = new LinearLayout(this);
        txtActionRow.setOrientation(LinearLayout.HORIZONTAL);
        txtActionRow.setPadding(0, 0, 0, dp(8));

        TextView btnAllText = label("ALL TEXT STYLES", 9.5f, Color.WHITE);
        btnAllText.setTypeface(null, Typeface.BOLD);
        btnAllText.setGravity(Gravity.CENTER);
        btnAllText.setPadding(dp(12), dp(6), dp(12), dp(6));
        GradientDrawable atBg = new GradientDrawable();
        atBg.setColor(0xDD1C2A3E);
        atBg.setCornerRadius(dp(10));
        atBg.setStroke(dp(1), 0xFF00FFE5);
        btnAllText.setBackground(atBg);
        txtActionRow.addView(btnAllText);

        TextView btnAnim = label("ANIMATION STYLES", 9.5f, Color.WHITE);
        btnAnim.setTypeface(null, Typeface.BOLD);
        btnAnim.setGravity(Gravity.CENTER);
        btnAnim.setPadding(dp(12), dp(6), dp(12), dp(6));
        GradientDrawable anBg = new GradientDrawable();
        anBg.setColor(0xDD2A1C3E);
        anBg.setCornerRadius(dp(10));
        anBg.setStroke(dp(1), 0xFFFFB300);
        btnAnim.setBackground(anBg);
        LinearLayout.LayoutParams anLp = new LinearLayout.LayoutParams(-2, -2);
        anLp.setMarginStart(dp(8));
        btnAnim.setLayoutParams(anLp);
        txtActionRow.addView(btnAnim);

        TextView btnGal = label("CUSTOM GALLERY", 9.5f, Color.WHITE);
        btnGal.setTypeface(null, Typeface.BOLD);
        btnGal.setGravity(Gravity.CENTER);
        btnGal.setPadding(dp(12), dp(6), dp(12), dp(6));
        btnGal.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_display, 0, 0, 0);
        btnGal.setCompoundDrawablePadding(dp(4));
        androidx.core.widget.TextViewCompat.setCompoundDrawableTintList(btnGal, ColorStateList.valueOf(0xFFFF007F));
        GradientDrawable glBg = new GradientDrawable();
        glBg.setColor(0xDD3A162B);
        glBg.setCornerRadius(dp(10));
        glBg.setStroke(dp(1), 0xFFFF007F);
        btnGal.setBackground(glBg);
        LinearLayout.LayoutParams glLp = new LinearLayout.LayoutParams(-2, -2);
        glLp.setMarginStart(dp(8));
        btnGal.setLayoutParams(glLp);
        btnGal.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            dialog.dismiss();
            pickCustomBanner();
        });
        txtActionRow.addView(btnGal);
        content.addView(txtActionRow);

        HorizontalScrollView txtScroll = new HorizontalScrollView(this);
        txtScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout txtRow = new LinearLayout(this);
        txtRow.setOrientation(LinearLayout.HORIZONTAL);
        txtRow.setPadding(0, dp(2), 0, dp(10));

        Runnable[] refreshTextStyles = new Runnable[1];
        refreshTextStyles[0] = () -> {
            txtRow.removeAllViews();
            int curStyle = BannerTextStyleRegistry.getActiveStyleIndex(this);
            for (int s = 0; s < BannerTextStyleRegistry.STYLES.length; s++) {
                final int sIdx = s;
                boolean isSel = (sIdx == curStyle);
                View pCard = BannerTextStyleRegistry.buildStylePreviewCard(
                        this, sIdx, isSel, v -> {
                            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                            BannerTextStyleRegistry.setActiveStyleIndex(this, sIdx);
                            refreshTextStyles[0].run();
                            updateActiveBannerTextStyle();
                            Toast.makeText(this, BannerTextStyleRegistry.STYLES[sIdx].name + " applied", Toast.LENGTH_SHORT).show();
                        });
                LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(dp(230), -2);
                tlp.setMarginEnd(dp(8));
                txtRow.addView(pCard, tlp);
            }
        };
        refreshTextStyles[0].run();
        txtScroll.addView(txtRow, new FrameLayout.LayoutParams(-2, -2));
        content.addView(txtScroll);

        // Section 4: Blur Slider
        TextView blurTitle = label("WALLPAPER BLUR EFFECT", 11.5f, 0xFF8E95AF);
        blurTitle.setTypeface(null, Typeface.BOLD);
        blurTitle.setLetterSpacing(0.08f);
        blurTitle.setPadding(0, dp(14), 0, dp(4));
        content.addView(blurTitle);

        LinearLayout blurRow = new LinearLayout(this);
        blurRow.setOrientation(LinearLayout.HORIZONTAL);
        blurRow.setGravity(Gravity.CENTER_VERTICAL);
        blurRow.setPadding(0, dp(4), 0, dp(12));

        TextView blurVal = label("Blur: " + bannerBlur + " px", 10.5f, Color.WHITE);
        blurVal.setTypeface(null, Typeface.BOLD);
        blurRow.addView(blurVal, new LinearLayout.LayoutParams(dp(90), -2));

        SeekBar blurBar = new SeekBar(this);
        blurBar.setMax(25);
        blurBar.setProgress(bannerBlur);
        blurBar.getProgressDrawable().setColorFilter(gameSpaceAccent, android.graphics.PorterDuff.Mode.SRC_IN);
        if (Build.VERSION.SDK_INT >= 16 && blurBar.getThumb() != null) {
            blurBar.getThumb().setColorFilter(gameSpaceAccent, android.graphics.PorterDuff.Mode.SRC_IN);
        }
        blurBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    bannerBlur = progress;
                    blurVal.setText("Blur: " + bannerBlur + " px");
                    savePreferences();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        blurRow.addView(blurBar, new LinearLayout.LayoutParams(0, -2, 1f));
        content.addView(blurRow);

        // Bottom Action Buttons
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, dp(10), 0, dp(6));

        TextView resetBtn = label("RESET TO DEFAULT", 10.5f, 0xFFFF4455);
        resetBtn.setTypeface(null, Typeface.BOLD);
        resetBtn.setGravity(Gravity.CENTER);
        resetBtn.setPadding(dp(14), dp(10), dp(14), dp(10));
        GradientDrawable rBg = new GradientDrawable();
        rBg.setColor(0xFF221115);
        rBg.setCornerRadius(dp(8));
        rBg.setStroke(dp(1), 0xFFFF2244);
        resetBtn.setBackground(rBg);
        resetBtn.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            gameSpaceAccent = 0xFFFF1744;
            activeBannerId = 0;
            bannerBlur = 8;
            savePreferences();
            blurBar.setProgress(bannerBlur);
            blurVal.setText("Blur: " + bannerBlur + " px");
            BannerTextStyleRegistry.setActiveStyleIndex(this, 0);
            refreshAccentRow[0].run();
            refreshGrid[0].run();
            refreshTextStyles[0].run();
            updateActiveBannerTextStyle();
            Toast.makeText(this, "Themes & banners reset to default", Toast.LENGTH_SHORT).show();
        });
        btnRow.addView(resetBtn, new LinearLayout.LayoutParams(0, -2, 1f));

        View spacer = new View(this);
        btnRow.addView(spacer, new LinearLayout.LayoutParams(dp(12), dp(1)));

        TextView applyBtn = label("APPLY & CLOSE", 10.5f, Color.WHITE);
        applyBtn.setTypeface(null, Typeface.BOLD);
        applyBtn.setGravity(Gravity.CENTER);
        applyBtn.setPadding(dp(14), dp(10), dp(14), dp(10));
        GradientDrawable aBg = new GradientDrawable();
        aBg.setColor(gameSpaceAccent);
        aBg.setCornerRadius(dp(8));
        applyBtn.setBackground(aBg);
        applyBtn.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            dialog.dismiss();
            rebuildView();
        });
        btnRow.addView(applyBtn, new LinearLayout.LayoutParams(0, -2, 1f));
        content.addView(btnRow);

        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        dialog.setContentView(root);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams wlp = new WindowManager.LayoutParams();
            wlp.copyFrom(w.getAttributes());
            wlp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.94f);
            wlp.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.92f);
            w.setAttributes(wlp);
        }
        dialog.setOnDismissListener(d -> {
            if (animatedBannerView != null) {
                String cUri = (activeBannerId >= 100 && (activeBannerId - 100) < customBannersList.size())
                         ?  customBannersList.get(activeBannerId - 100) : null;
                animatedBannerView.setBanner(activeBannerId, cUri, bannerBlur);
            }
        });
        try {
            if (!isFinishing() && !isDestroyed()) {
                dialog.show();
            }
        } catch (Exception ignored) {}
    }

    /** REVROR-inspired landscape lobby: game rail, focused game stage, and Energy Core controls. */
    private View buildRORLandscapeHome() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float heightDp = dm.heightPixels / dm.density;
        boolean isCompact = heightDp < 460;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(isCompact  ?  12 : 18), dp(isCompact  ?  4 : 8), dp(isCompact  ?  12 : 18), dp(isCompact  ?  3 : 6));
        root.setBackgroundColor(0x00000000);

        topBarView = buildRORHeader();
        root.addView(topBarView, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);
        body.setGravity(Gravity.CENTER_VERTICAL);
        body.setPadding(0, dp(isCompact  ?  4 : 8), 0, dp(isCompact  ?  4 : 8));

        sliderShelfView = buildGamingTelemetrySuite();
        LinearLayout.LayoutParams railLp = new LinearLayout.LayoutParams(dp(isCompact  ?  246 : 278), -1);
        railLp.setMarginEnd(dp(isCompact  ?  8 : 12));
        body.addView(sliderShelfView, railLp);

        heroStageView = buildRORHero();
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(0, -1, 1f);
        heroLp.setMarginEnd(dp(isCompact  ?  8 : 12));
        body.addView(heroStageView, heroLp);

        body.addView(buildEnergyCorePanel(), new LinearLayout.LayoutParams(dp(isCompact  ?  248 : 286), -1));
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));

        root.addView(buildRORBottomNav(), new LinearLayout.LayoutParams(-1, dp(isCompact  ?  36 : 44)));

        adBannerView = buildAdBanner();
        LinearLayout.LayoutParams adLp = new LinearLayout.LayoutParams(-1, dp(isCompact  ?  46 : 54));
        adLp.topMargin = dp(isCompact  ?  2 : 4);
        root.addView(adBannerView, adLp);
        return root;
    }

    /** REVROR-inspired portrait lobby: sleek hero spotlight, games carousel shelf, and Energy Core controls matching landscape. */
    private View buildRORPortraitHome() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(6), dp(12), dp(6));
        root.setBackgroundColor(0x00000000);

        topBarView = buildRORHeader();
        root.addView(topBarView, new LinearLayout.LayoutParams(-1, -2));

        ScrollView bodyScroll = new ScrollView(this);
        bodyScroll.setFillViewport(true);
        bodyScroll.setVerticalScrollBarEnabled(false);
        bodyScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(0, dp(6), 0, dp(10));

        // 1. Hero Spotlight Card
        heroStageView = buildRORPortraitHero();
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(-1, -2);
        heroLp.bottomMargin = dp(10);
        body.addView(heroStageView, heroLp);

        // 2. Game Library Shelf
        sliderShelfView = buildRORPortraitGameShelf();
        LinearLayout.LayoutParams shelfLp = new LinearLayout.LayoutParams(-1, -2);
        shelfLp.bottomMargin = dp(10);
        body.addView(sliderShelfView, shelfLp);

        // 2.5 Gaming Telemetry Suite (Photos 1 & 2 Live Sensors)
        View telemetrySuite = buildGamingTelemetrySuite(0, true);
        LinearLayout.LayoutParams telemLp = new LinearLayout.LayoutParams(-1, -2);
        telemLp.bottomMargin = dp(10);
        body.addView(telemetrySuite, telemLp);

        // 3. Energy Core Performance Controls
        View energyCore = buildEnergyCorePanel();
        LinearLayout.LayoutParams coreLp = new LinearLayout.LayoutParams(-1, -2);
        coreLp.bottomMargin = dp(10);
        body.addView(energyCore, coreLp);

        // 4. Bottom Nav
        body.addView(buildRORBottomNav(), new LinearLayout.LayoutParams(-1, dp(40)));

        // 5. Ad Banner
        adBannerView = buildAdBanner();
        LinearLayout.LayoutParams adLp = new LinearLayout.LayoutParams(-1, dp(54));
        adLp.topMargin = dp(4);
        body.addView(adBannerView, adLp);

        bodyScroll.addView(body, new ScrollView.LayoutParams(-1, -2));
        root.addView(bodyScroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        return root;
    }

    private View buildRORPortraitHero() {
        FrameLayout frame = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF18181C, 0xFF0B0B0E, 0xFF24080C});
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), 0xFF3A3A40);
        frame.setBackground(bg);

        View redRail = new View(this);
        GradientDrawable railBg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFFFF3849, 0xFF8C0A16});
        railBg.setCornerRadius(dp(3));
        redRail.setBackground(railBg);
        FrameLayout.LayoutParams redRailLp = new FrameLayout.LayoutParams(dp(4), dp(100), Gravity.START | Gravity.CENTER_VERTICAL);
        redRailLp.setMarginStart(dp(14));
        frame.addView(redRail, redRailLp);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(24), dp(16), dp(24), dp(16));

        TextView kicker = label("CURRENT GAME  /  PERFORMANCE READY", 8f, 0xFFFF4A55);
        kicker.setTypeface(null, Typeface.BOLD);
        kicker.setLetterSpacing(.14f);
        content.addView(kicker);

        FrameLayout iconFrame = new FrameLayout(this);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setColor(0xFF111116);
        iconBg.setCornerRadius(dp(16));
        iconBg.setStroke(dp(2), 0xFF4B4B54);
        iconFrame.setBackground(iconBg);
        heroIcon = new ImageView(this);
        heroIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconFrame.addView(heroIcon, new FrameLayout.LayoutParams(dp(68), dp(68), Gravity.CENTER));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(86), dp(86));
        iconLp.setMargins(0, dp(10), 0, dp(8));
        content.addView(iconFrame, iconLp);

        final float[] touchDownX = new float[1];
        final float[] touchDownY = new float[1];
        final long[] touchDownTime = new long[1];
        View.OnTouchListener heroSwipeAndTapListener = (v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    touchDownX[0] = event.getRawX();
                    touchDownY[0] = event.getRawY();
                    touchDownTime[0] = System.currentTimeMillis();
                    if (v.getParent() != null) {
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    float dx = event.getRawX() - touchDownX[0];
                    float dy = event.getRawY() - touchDownY[0];
                    long dt = System.currentTimeMillis() - touchDownTime[0];
                    if (Math.abs(dx) > dp(25) && Math.abs(dx) > Math.abs(dy) * 1.05f) {
                        if (dx < 0) {
                            cycleHeroGame(1, content); // Swipe left -> Next game
                        } else {
                            cycleHeroGame(-1, content); // Swipe right -> Previous game
                        }
                        return true;
                    } else if (Math.abs(dx) < dp(20) && Math.abs(dy) < dp(20) && dt < 500) {
                        try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
                        v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(70).withEndAction(() -> {
                            v.animate().scaleX(1.06f).scaleY(1.06f).setDuration(110).withEndAction(() -> {
                                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(70).start();
                            }).start();
                        }).start();
                        if (selectedEntry != null) {
                            prepareLaunch(selectedEntry);
                        } else if (!displayedEntries.isEmpty()) {
                            prepareLaunch(displayedEntries.get(0));
                        } else {
                            showAddGamesDialog();
                        }
                        return true;
                    }
                    break;
            }
            return false;
        };

        iconFrame.setClickable(true);
        iconFrame.setFocusable(true);
        iconFrame.setOnTouchListener(heroSwipeAndTapListener);
        heroIcon.setClickable(true);
        heroIcon.setOnTouchListener(heroSwipeAndTapListener);
        content.setOnTouchListener(heroSwipeAndTapListener);

        heroTitle = label("SELECT A GAME", 20, Color.WHITE);
        heroTitle.setTypeface(null, Typeface.BOLD);
        heroTitle.setGravity(Gravity.CENTER);
        heroTitle.setMaxLines(1);
        heroTitle.setEllipsize(TextUtils.TruncateAt.END);
        content.addView(heroTitle);

        heroSubtitle = label("Choose a title from your Game Lobby", 9.5f, 0xFF9A9AA3);
        heroSubtitle.setGravity(Gravity.CENTER);
        heroSubtitle.setPadding(0, dp(2), 0, dp(8));
        content.addView(heroSubtitle);

        heroBadges = label("TURBO ENGINE   |   LOW LATENCY   |   FLOATING TOOLS", 8.5f, 0xFFD8D8DC);
        heroBadges.setGravity(Gravity.CENTER);
        heroBadges.setPadding(dp(10), dp(6), dp(10), dp(6));
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(0xFF202025);
        badgeBg.setCornerRadius(dp(8));
        badgeBg.setStroke(dp(1), 0xFF393940);
        heroBadges.setBackground(badgeBg);
        content.addView(heroBadges);

        heroPlayButton = new Button(this);
        heroPlayButton.setText("START GAME");
        heroPlayButton.setTextColor(Color.WHITE);
        heroPlayButton.setTextSize(12);
        heroPlayButton.setTypeface(null, Typeface.BOLD);
        heroPlayButton.setAllCaps(false);
        GradientDrawable playBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFFCC101F, 0xFFFF3345});
        playBg.setCornerRadius(dp(10));
        playBg.setStroke(dp(1), 0xFFFF6E78);
        heroPlayButton.setBackground(playBg);
        heroPlayButton.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(60).withEndAction(() ->
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start()).start();
            if (selectedEntry != null) prepareLaunch(selectedEntry);
            else if (!displayedEntries.isEmpty()) prepareLaunch(displayedEntries.get(0));
            else showAddGamesDialog();
        });
        LinearLayout.LayoutParams playLp = new LinearLayout.LayoutParams(dp(220), dp(44));
        playLp.topMargin = dp(12);
        content.addView(heroPlayButton, playLp);

        statusText = label("READY TO LAUNCH", 8f, 0xFF8B8B94);
        statusText.setGravity(Gravity.CENTER);
        statusText.setLetterSpacing(.10f);
        statusText.setPadding(0, dp(4), 0, 0);
        content.addView(statusText);
        updateActiveBannerTextStyle();
        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        content.addView(progressBar, new LinearLayout.LayoutParams(dp(16), dp(16)));

        frame.addView(content, new FrameLayout.LayoutParams(-1, -2));
        frame.setOnTouchListener(heroSwipeAndTapListener);

        FrameLayout prevArrowFrame = new FrameLayout(this);
        GradientDrawable prevArrowBg = new GradientDrawable();
        prevArrowBg.setShape(GradientDrawable.OVAL);
        prevArrowBg.setColor(0x550F1626);
        prevArrowBg.setStroke(dp(1.2f), 0x8850CBFF);
        prevArrowFrame.setBackground(prevArrowBg);
        ImageView prevArrowIcon = new ImageView(this);
        prevArrowIcon.setImageResource(R.drawable.ic_chevron_left);
        prevArrowIcon.setColorFilter(0xFF50CBFF);
        prevArrowFrame.addView(prevArrowIcon, new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER));
        FrameLayout.LayoutParams pLp = new FrameLayout.LayoutParams(dp(36), dp(36), Gravity.START | Gravity.CENTER_VERTICAL);
        pLp.setMarginStart(dp(4));
        prevArrowFrame.setClickable(true);
        prevArrowFrame.setFocusable(true);
        prevArrowFrame.setOnClickListener(v -> {
            try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
            v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(60).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()).start();
            cycleHeroGame(-1, content);
        });
        frame.addView(prevArrowFrame, pLp);

        FrameLayout nextArrowFrame = new FrameLayout(this);
        GradientDrawable nextArrowBg = new GradientDrawable();
        nextArrowBg.setShape(GradientDrawable.OVAL);
        nextArrowBg.setColor(0x550F1626);
        nextArrowBg.setStroke(dp(1.2f), 0x8850CBFF);
        nextArrowFrame.setBackground(nextArrowBg);
        ImageView nextArrowIcon = new ImageView(this);
        nextArrowIcon.setImageResource(R.drawable.ic_chevron_right);
        nextArrowIcon.setColorFilter(0xFF50CBFF);
        nextArrowFrame.addView(nextArrowIcon, new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER));
        FrameLayout.LayoutParams nLp = new FrameLayout.LayoutParams(dp(36), dp(36), Gravity.END | Gravity.CENTER_VERTICAL);
        nLp.setMarginEnd(dp(4));
        nextArrowFrame.setClickable(true);
        nextArrowFrame.setFocusable(true);
        nextArrowFrame.setOnClickListener(v -> {
            try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
            v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(60).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()).start();
            cycleHeroGame(1, content);
        });
        frame.addView(nextArrowFrame, nLp);

        return frame;
    }

    private View buildRORPortraitGameShelf() {
        LinearLayout shelf = new LinearLayout(this);
        shelf.setOrientation(LinearLayout.VERTICAL);
        shelf.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xF21A1A1E, 0xF20C0C0F});
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), 0xFF34343A);
        shelf.setBackground(bg);

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView title = label("GAME LOBBY", 12, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setLetterSpacing(.10f);
        titles.addView(title);
        TextView subtitle = label("YOUR LIBRARY", 8, 0xFF818189);
        subtitle.setLetterSpacing(.14f);
        titles.addView(subtitle);
        headerRow.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView layoutBtn = redHeaderButton(LAYOUT_NAMES[layoutIndex % LAYOUT_NAMES.length].toUpperCase(Locale.US), R.drawable.ic_layers);
        layoutBtn.setTextColor(0xFFFF6670);
        layoutBtn.setOnClickListener(v -> showLayoutChooser());
        headerRow.addView(layoutBtn, new LinearLayout.LayoutParams(-2, dp(34)));

        TextView manageBtn = redHeaderButton("MANAGE", R.drawable.ic_add);
        manageBtn.setTextColor(0xFFFF6670);
        manageBtn.setOnClickListener(v -> showManageGamesDialog());
        LinearLayout.LayoutParams manageLp = new LinearLayout.LayoutParams(-2, dp(34));
        manageLp.setMarginStart(dp(6));
        headerRow.addView(manageBtn, manageLp);
        shelf.addView(headerRow);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(0, dp(8), 0, dp(8));
        tabGames = createTab("GAMES", !showAllApps);
        tabAll = createTab("ALL APPS", showAllApps);
        tabGames.setOnClickListener(v -> {
            if (showAllApps) { showAllApps = false; updateTabs(); renderSlider(); }
        });
        tabAll.setOnClickListener(v -> {
            if (!showAllApps) { showAllApps = true; updateTabs(); renderSlider(); }
        });
        tabs.addView(tabGames, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams allLp = new LinearLayout.LayoutParams(0, -2, 1f);
        allLp.setMarginStart(dp(6));
        tabs.addView(tabAll, allLp);
        shelf.addView(tabs);

        sliderScroll = new HorizontalScrollView(this);
        sliderScroll.setHorizontalScrollBarEnabled(false);
        sliderScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        verticalGameScroll = null;

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.HORIZONTAL);
        sliderContainer = list;
        sliderContainer.setPadding(0, dp(4), 0, dp(4));
        sliderScroll.addView(sliderContainer, new FrameLayout.LayoutParams(-2, -2));
        shelf.addView(sliderScroll, new LinearLayout.LayoutParams(-1, -2));

        return shelf;
    }

    private View buildSuperBasePage() {
        sliderContainer = null;
        verticalGameScroll = null;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(8), dp(18), dp(10));
        root.setBackgroundColor(0x00000000);
        topBarView = buildRORHeader();
        root.addView(topBarView, new LinearLayout.LayoutParams(-1, dp(52)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(4), dp(8), dp(4), dp(12));

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(dp(18), dp(13), dp(18), dp(13));
        GradientDrawable heroBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF251015, 0xFF121216, 0xFF321018});
        heroBg.setCornerRadius(dp(15));
        heroBg.setStroke(dp(1), 0xFFFF4350);
        hero.setBackground(heroBg);
        ImageView heroIcon = new ImageView(this);
        heroIcon.setImageResource(R.drawable.ic_speed_meter);
        heroIcon.setColorFilter(0xFFFF5360);
        hero.addView(heroIcon, new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout heroCopy = new LinearLayout(this);
        heroCopy.setOrientation(LinearLayout.VERTICAL);
        heroCopy.setPadding(dp(14), 0, 0, 0);
        TextView heroTitle = label("SUPER BASE  /  ENERGY CORE", 18, Color.WHITE);
        heroTitle.setTypeface(null, Typeface.BOLD);
        heroTitle.setLetterSpacing(.08f);
        heroCopy.addView(heroTitle);
        TextView heroSub = label("Console performance, display plugins, aim tools and network control", 9.5f, 0xFFB5B5BC);
        heroSub.setPadding(0, dp(3), 0, 0);
        heroCopy.addView(heroSub);
        hero.addView(heroCopy, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView online = redHeaderButton("REVROR ENGINE ONLINE", R.drawable.ic_done);
        online.setTextColor(0xFFFF7881);
        hero.addView(online);
        content.addView(hero, new LinearLayout.LayoutParams(-1, dp(78)));

        boolean phoenix = GsmProfileManager.isPhoenixSelected(this);
        content.addView(superSectionTitle(phoenix  ?  "PHOENIX PROFILES" : "ENERGY PROFILES",
                phoenix  ?  "Phoenix engine active • tap a Phoenix+ preset to activate instantly" : "Tap a mode to activate it instantly"));
        LinearLayout profiles = new LinearLayout(this);
        profiles.setOrientation(LinearLayout.HORIZONTAL);
        profiles.setBaselineAligned(false);
        superProfileItems.clear();
        if (phoenix) {
            // Phoenix Engine: All profiles usable
            profiles.addView(superProfileCard("BATTERY+", "Extended endurance", R.drawable.ic_dashboard, 0), weightedCardParams());
            profiles.addView(superProfileCard("BALANCE+", "Adaptive fluid power", R.drawable.ic_spectrum_balanced, 1), weightedCardParams());
            profiles.addView(superProfileCard("THERMAL+", "Deep cooling control", R.drawable.ic_battery, 2), weightedCardParams());
            profiles.addView(superProfileCard("COOLDOWN+", "Emergency cooldown", R.drawable.ic_cooldown, 3), weightedCardParams());
            profiles.addView(superProfileCard("SMOOTH+", "Stable high FPS", R.drawable.ic_spectrum_game, 4), weightedCardParams());
            profiles.addView(superProfileCard("ULTRA GAMING+", "Apex peak overdrive", R.drawable.ic_ultra_gaming, 5), weightedCardParams());
        } else {
            // Regular Engine: First 4 profiles connected to gsmws / library
            profiles.addView(superProfileCard("DAILY USE", "Smooth defaults", R.drawable.ic_dashboard, 0), weightedCardParams());
            profiles.addView(superProfileCard("BALANCE", "Power and battery", R.drawable.ic_spectrum_balanced, 1), weightedCardParams());
            profiles.addView(superProfileCard("BATTERY", "Maximum endurance", R.drawable.ic_battery, 2), weightedCardParams());
            profiles.addView(superProfileCard("GAMING", "Maximum performance", R.drawable.ic_spectrum_game, 3), weightedCardParams());
        }
        content.addView(profiles, new LinearLayout.LayoutParams(-1, dp(92)));

        SharedPreferences plugins = getSharedPreferences("ror_game_plugins", MODE_PRIVATE);
        content.addView(superSectionTitle("CONSOLE PERFORMANCE CONTROLS", "Full game-space controls"));
        GridLayout controls = new GridLayout(this);
        controls.setColumnCount(4);
        addSuperTile(controls, "Background cleanup", "Free CPU and RAM", R.drawable.ic_turbo_fan, cleanupEnabled, h -> {
            cleanupEnabled = !cleanupEnabled;
            savePreferences();
            updateSuperTileState(h, cleanupEnabled, null);
            if (cleanupEnabled) runQuickLauncherAction("cleanup");
        });
        addSuperTile(controls, "Network boost", "Low-latency tuning", R.drawable.ic_wifi, networkEnabled, h -> {
            networkEnabled = !networkEnabled;
            savePreferences();
            updateSuperTileState(h, networkEnabled, null);
            if (networkEnabled) runQuickLauncherAction("network");
        });
        floatingToolsTileHolder = addSuperTile(controls, "Floating tools", "In-game side panel", R.drawable.ic_floating_window, bubbleEnabled, h -> {
            toggleFloatingTools();
            updateSuperTileState(h, bubbleEnabled, null);
        });
        addSuperTile(controls, "Crosshair", "All settings", R.drawable.ic_crosshair, plugins.getBoolean("crosshair", false), this::showUnifiedCrosshairDialog);
        addSuperTile(controls, "Anti-disturb", "Block interruptions", R.drawable.ic_shield, plugins.getBoolean("anti_disturb", false), h -> {
            boolean next = !plugins.getBoolean("anti_disturb", false);
            plugins.edit().putBoolean("anti_disturb", next).apply();
            updateSuperTileState(h, next, null);
            runLauncherCommand(next  ?  "settings put global heads_up_notifications_enabled 0; cmd notification set_dnd priority"
                    : "settings put global heads_up_notifications_enabled 1; cmd notification set_dnd off");
            Toast.makeText(this, "Anti-disturb " + (next  ?  "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });
        addSuperTile(controls, "Rainbow ribbon", "Chroma status lighting", R.drawable.ic_rainbow_rgb, plugins.getBoolean("rainbow_rgb", true), h -> {
            boolean next = !plugins.getBoolean("rainbow_rgb", true);
            plugins.edit().putBoolean("rainbow_rgb", next).apply();
            updateSuperTileState(h, next, null);
            Toast.makeText(this, "Rainbow Ribbon " + (next  ?  "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });
        addSuperTile(controls, "Block calls", "Auto-silence phone calls", R.drawable.ic_phone_block, plugins.getBoolean("block_calls", false), h -> {
            boolean next = !plugins.getBoolean("block_calls", false);
            plugins.edit().putBoolean("block_calls", next).apply();
            updateSuperTileState(h, next, null);
            runLauncherCommand(next  ?  "telecom set-call-screening-enabled true 2>/dev/null; cmd notification set_dnd total_silence"
                    : "cmd notification set_dnd off");
            Toast.makeText(this, "Block Calls " + (next  ?  "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });
        addSuperTile(controls, "Barrage messages", "Styled game alerts", R.drawable.ic_barrage_message, plugins.getBoolean("barrage", true), h -> {
            boolean next = !plugins.getBoolean("barrage", true);
            plugins.edit().putBoolean("barrage", next).apply();
            updateSuperTileState(h, next, null);
            Toast.makeText(this, "Barrage messages " + (next  ?  "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });
        int barrageStyle = Math.max(0, Math.min(BARRAGE_STYLE_NAMES.length - 1, plugins.getInt("barrage_style", 0)));
        addSuperTile(controls, "Barrage style", BARRAGE_STYLE_NAMES[barrageStyle], R.drawable.ic_barrage_style, true, h -> {
            showBarrageSettingsDialog(h);
        });
        addSuperTile(controls, "Brightness lock", "Stable screen level", R.drawable.ic_sun, plugins.getBoolean("brightness_lock", false), h -> {
            boolean next = !plugins.getBoolean("brightness_lock", false);
            plugins.edit().putBoolean("brightness_lock", next).apply();
            updateSuperTileState(h, next, null);
            runLauncherCommand(next  ?  "settings put system screen_brightness_mode 0; settings put system screen_brightness 220"
                    : "settings put system screen_brightness_mode 0");
            Toast.makeText(this, "Brightness lock " + (next  ?  "enabled (220/255)" : "disabled"), Toast.LENGTH_SHORT).show();
        });
        addSuperTile(controls, "Hunting Filter", "11 REVROR styles", R.drawable.ic_display, plugins.getBoolean("screen_filter", false), h -> {
            showHuntingFilterDialog();
        });
        addSuperTile(controls, "Color boost", "Vivid game colors", R.drawable.ic_sparkle, plugins.getBoolean("color_boost", false), h -> {
            boolean next = !plugins.getBoolean("color_boost", false);
            plugins.edit().putBoolean("color_boost", next).apply();
            updateSuperTileState(h, next, null);
            Toast.makeText(this, "Color boost " + (next  ?  "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });
        addSuperTile(controls, "High refresh", "120 Hz preference", R.drawable.ic_refresh, plugins.getBoolean("high_refresh", false), h -> {
            boolean next = !plugins.getBoolean("high_refresh", false);
            plugins.edit().putBoolean("high_refresh", next).apply();
            updateSuperTileState(h, next, null);
            runLauncherCommand(next  ?  "settings put system min_refresh_rate 120; settings put system peak_refresh_rate 120"
                    : "settings delete system min_refresh_rate; settings delete system peak_refresh_rate");
            Toast.makeText(this, "High refresh " + (next  ?  "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });
        addSuperTile(controls, "Touch boost", "Faster touch response", R.drawable.ic_gestures, plugins.getBoolean("touch_boost", false), h -> {
            boolean next = !plugins.getBoolean("touch_boost", false);
            plugins.edit().putBoolean("touch_boost", next).apply();
            updateSuperTileState(h, next, null);
            Toast.makeText(this, "Touch boost " + (next  ?  "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });
        addSuperTile(controls, "Audio Boost (+6dB)", "Max gain & dynamic range", R.drawable.ic_volume, plugins.getBoolean("audio_boost", false), h -> {
            boolean next = !plugins.getBoolean("audio_boost", false);
            plugins.edit().putBoolean("audio_boost", next).apply();
            updateSuperTileState(h, next, null);
            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (am != null) {
                int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                am.setStreamVolume(AudioManager.STREAM_MUSIC, next  ?  max : Math.max(1, (int)(max * 0.7f)), AudioManager.FLAG_SHOW_UI);
            }
            runLauncherCommand("settings put system sound_effects " + (next  ?  "1" : "0") + "; settings put system volume_music_speaker 15");
            Toast.makeText(this, "Audio Boost " + (next  ?  "enabled (+6dB)" : "disabled"), Toast.LENGTH_SHORT).show();
        });
        addSuperTile(controls, "Thermal Guard", "Auto protection on heat", R.drawable.ic_shield, plugins.getBoolean("thermal_guard", false), h -> {
            boolean next = !plugins.getBoolean("thermal_guard", false);
            plugins.edit().putBoolean("thermal_guard", next).apply();
            updateSuperTileState(h, next, null);
            Toast.makeText(this, "Thermal Guard " + (next  ?  "ARMED (>43C alert)" : "DISARMED"), Toast.LENGTH_SHORT).show();
        });
        addSuperTile(controls, "RAM & VRAM Flush", "Purge caches & sync", R.drawable.ic_turbo_fan, false, h -> {
            new Thread(() -> {
                runLauncherCommand("sync; echo 3 > /proc/sys/vm/drop_caches; echo 1 > /proc/sys/vm/compact_memory; am kill-all");
                runOnUiThread(() -> Toast.makeText(this, "RAM & VRAM caches purged", Toast.LENGTH_SHORT).show());
            }).start();
        });
        addSuperTile(controls, "Add Games", "Include / Exclude apps", R.drawable.ic_spectrum_game, false, h -> showAddGamesDialog());
        addSuperTile(controls, "Game Space Layout", LAYOUT_NAMES[Math.floorMod(layoutIndex, LAYOUT_NAMES.length)], R.drawable.ic_layers, true, h -> showLayoutChooser());
        addSuperTile(controls, "Game Space Center", "Atmosphere & Peripherals", R.drawable.ic_settings_icon, false, h -> showGameSpaceCenterDialog());
        addSuperTile(controls, "Themes & Banners", "Wallpapers & accent colors", R.drawable.ic_display, true, h -> showThemeAndBannerDialog());
        content.addView(controls, new LinearLayout.LayoutParams(-1, -2));

        // Crosshair is intentionally exposed as one option above. Its complete
        // enable/style/color/size editor opens from that single tile.
        if (false) {
        content.addView(superSectionTitle("CROSSHAIR ARMORY", "Choose aim style, reticle color and customize size for in-game floating overlay"));
        LinearLayout sights = new LinearLayout(this);
        sights.setOrientation(LinearLayout.HORIZONTAL);
        crosshairHolders.clear();
        int selectedStyle = plugins.getInt("crosshair_style", 0);
        for (int i = 0; i < CROSSHAIR_NAMES.length; i++) {
            sights.addView(buildCrosshairChoice(i, selectedStyle == i, plugins), weightedCardParams());
        }
        content.addView(sights, new LinearLayout.LayoutParams(-1, dp(104)));

        LinearLayout chControls = new LinearLayout(this);
        chControls.setOrientation(LinearLayout.HORIZONTAL);
        chControls.setGravity(Gravity.CENTER_VERTICAL);
        chControls.setPadding(dp(6), dp(8), dp(6), dp(8));

        TextView colLabel = label("COLOR: ", 9.5f, 0xFFB0B0B8);
        colLabel.setTypeface(null, Typeface.BOLD);
        chControls.addView(colLabel);

        final List<View> colorViews = new ArrayList<>();
        for (int c = 0; c < CROSSHAIR_COLORS.length; c++) {
            final int color = CROSSHAIR_COLORS[c];
            FrameLayout cCircle = new FrameLayout(this);
            View cDot = new View(this);
            GradientDrawable dotBg = new GradientDrawable();
            dotBg.setShape(GradientDrawable.OVAL);
            dotBg.setColor(color);
            cDot.setBackground(dotBg);
            cCircle.addView(cDot, new FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER));

            GradientDrawable ring = new GradientDrawable();
            ring.setShape(GradientDrawable.OVAL);
            if (color == crosshairColor) {
                ring.setStroke(dp(2), Color.WHITE);
            }
            cCircle.setBackground(ring);
            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(dp(26), dp(26));
            cLp.setMarginEnd(dp(6));
            cCircle.setLayoutParams(cLp);

            cCircle.setOnClickListener(v -> {
                crosshairColor = color;
                plugins.edit().putInt("crosshair_color", color).apply();
                for (View cv : colorViews) {
                    ((GradientDrawable) cv.getBackground()).setStroke(0, 0);
                }
                ring.setStroke(dp(2), Color.WHITE);
                updateCrosshairChoices();
                syncCrosshairToService();
            });
            colorViews.add(cCircle);
            chControls.addView(cCircle);
        }

        View chSpacer = new View(this);
        chControls.addView(chSpacer, new LinearLayout.LayoutParams(0, 1, 1f));

        TextView sizeLabel = label("SIZE: ", 9.5f, 0xFFB0B0B8);
        sizeLabel.setTypeface(null, Typeface.BOLD);
        chControls.addView(sizeLabel);

        int[] sizes = {28, 40, 52, 68, 84};
        String[] sizeNames = {"XS", "S", "M", "L", "XL"};
        final List<TextView> sizeButtons = new ArrayList<>();
        for (int s = 0; s < sizes.length; s++) {
            final int sz = sizes[s];
            TextView sBtn = label(sizeNames[s], 9f, crosshairSize == sz  ?  Color.WHITE : 0xFFA0A0A8);
            sBtn.setTypeface(null, Typeface.BOLD);
            sBtn.setGravity(Gravity.CENTER);
            sBtn.setPadding(dp(8), dp(4), dp(8), dp(4));

            GradientDrawable sBg = new GradientDrawable();
            sBg.setCornerRadius(dp(6));
            sBg.setColor(crosshairSize == sz  ?  0xFFC90D20 : 0xFF222228);
            sBg.setStroke(dp(1), crosshairSize == sz  ?  0xFFFF4A55 : 0xFF44444C);
            sBtn.setBackground(sBg);

            LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(-2, dp(26));
            sbLp.setMarginStart(dp(4));
            sBtn.setLayoutParams(sbLp);

            sBtn.setOnClickListener(v -> {
                crosshairSize = sz;
                plugins.edit().putInt("crosshair_size", sz).apply();
                for (int j = 0; j < sizeButtons.size(); j++) {
                    TextView b = sizeButtons.get(j);
                    boolean sel = (sizes[j] == sz);
                    b.setTextColor(sel  ?  Color.WHITE : 0xFFA0A0A8);
                    GradientDrawable bg = (GradientDrawable) b.getBackground();
                    bg.setColor(sel  ?  0xFFC90D20 : 0xFF222228);
                    bg.setStroke(dp(1), sel  ?  0xFFFF4A55 : 0xFF44444C);
                }
                updateCrosshairChoices();
                syncCrosshairToService();
            });
            sizeButtons.add(sBtn);
            chControls.addView(sBtn);
        }
        content.addView(chControls);
        }

        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(buildRORBottomNav(), new LinearLayout.LayoutParams(-1, dp(44)));

        adBannerView = buildAdBanner();
        DisplayMetrics sdm = getResources().getDisplayMetrics();
        float shDp = sdm.heightPixels / sdm.density;
        boolean isCompact = shDp < 460;
        LinearLayout.LayoutParams adLp = new LinearLayout.LayoutParams(-1, dp(isCompact  ?  46 : 54));
        adLp.topMargin = dp(isCompact  ?  2 : 4);
        root.addView(adBannerView, adLp);
        return root;
    }

    private void showUnifiedCrosshairDialog(SuperTileHolder tileHolder) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        SharedPreferences plugins = getSharedPreferences("ror_game_plugins", MODE_PRIVATE);
        final boolean[] enabled = {plugins.getBoolean("crosshair", false)};
        final int[] style = {Math.max(0, Math.min(CROSSHAIR_NAMES.length - 1, plugins.getInt("crosshair_style", 0)))};
        final int[] color = {plugins.getInt("crosshair_color", crosshairColor)};
        final int[] size = {(int) getCrosshairSizeSafe(plugins)};

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF6090B14);
        root.setBackground(bg);

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(10));

        View backBtn = createCyberGlassBackButton(v -> dialog.dismiss());
        header.addView(backBtn);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setPadding(dp(12), 0, 0, 0);
        TextView title = label("TACTICAL CROSSHAIR STUDIO", 14.5f, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setLetterSpacing(0.04f);
        titleCol.addView(title);
        TextView sub = label("Custom HUD reticle overlay with real-time target range preview", 9.5f, 0xFF9E9EA8);
        titleCol.addView(sub);
        header.addView(titleCol, new LinearLayout.LayoutParams(0, -2, 1f));

        // Master Enable Toggle Pill
        TextView togglePill = label(enabled[0]  ?  "AIM CTIVE" : "AIM OFF", 10f, enabled[0]  ?  Color.BLACK : 0xFFA0A0A8);
        togglePill.setTypeface(null, Typeface.BOLD);
        togglePill.setPadding(dp(12), dp(6), dp(12), dp(6));
        GradientDrawable tBg = new GradientDrawable();
        tBg.setCornerRadius(dp(12));
        tBg.setColor(enabled[0]  ?  0xFF00E5FF : 0xFF202330);
        tBg.setStroke(dp(1), enabled[0]  ?  0xFF00E5FF : 0xFF35394C);
        togglePill.setBackground(tBg);
        togglePill.setOnClickListener(v -> {
            enabled[0] = !enabled[0];
            togglePill.setText(enabled[0]  ?  "AIM CTIVE" : "AIM OFF");
            togglePill.setTextColor(enabled[0]  ?  Color.BLACK : 0xFFA0A0A8);
            tBg.setColor(enabled[0]  ?  0xFF00E5FF : 0xFF202330);
            tBg.setStroke(dp(1), enabled[0]  ?  0xFF00E5FF : 0xFF35394C);
        });
        header.addView(togglePill);
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        // Radar Range Viewport Box
        FrameLayout radarBox = new FrameLayout(this);
        GradientDrawable rBg = new GradientDrawable();
        rBg.setColor(0xFF060810);
        rBg.setCornerRadius(dp(14));
        rBg.setStroke(dp(1.5f), 0xFF00E5FF);
        radarBox.setBackground(rBg);

        // Radar background with rings and grid
        View radarRings = new View(this) {
            final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas c) {
                super.onDraw(c);
                float cx = getWidth() / 2f, cy = getHeight() / 2f;
                p.setStyle(Paint.Style.STROKE);
                p.setColor(0x1A00E5FF);
                p.setStrokeWidth(dp(1));
                c.drawCircle(cx, cy, dp(25), p);
                c.drawCircle(cx, cy, dp(50), p);
                c.drawCircle(cx, cy, dp(75), p);
                c.drawLine(cx - dp(90), cy, cx + dp(90), cy, p);
                c.drawLine(cx, cy - dp(60), cx, cy + dp(60), p);
            }
        };
        radarBox.addView(radarRings, new FrameLayout.LayoutParams(-1, -1));

        // Corner telemetry tags
        TextView tagLeft = label("RANGE: 50m • ZERO OFFSET", 8f, 0xFF00E5FF);
        tagLeft.setPadding(dp(10), dp(8), 0, 0);
        radarBox.addView(tagLeft, new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START));

        TextView tagRight = label("SIGHT: " + CROSSHAIR_NAMES[style[0]].toUpperCase(Locale.US), 8f, 0xFF00E5FF);
        tagRight.setPadding(0, dp(8), dp(10), 0);
        radarBox.addView(tagRight, new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END));

        // Center reticle container
        FrameLayout reticleCenter = new FrameLayout(this);
        radarBox.addView(reticleCenter, new FrameLayout.LayoutParams(dp(110), dp(110), Gravity.CENTER));

        Runnable refreshRadar = () -> {
            reticleCenter.removeAllViews();
            float scale = Math.max(24, Math.min(80, size[0])) / 52f;
            View reticle = createCrosshairPreview(style[0], color[0], scale);
            reticleCenter.addView(reticle, new FrameLayout.LayoutParams(-1, -1));
            tagRight.setText("SIGHT: " + CROSSHAIR_NAMES[style[0]].toUpperCase(Locale.US));
        };
        refreshRadar.run();
        content.addView(radarBox, new LinearLayout.LayoutParams(-1, dp(130)));

        // Style Selector Section
        TextView styleTitle = label("RETICLE STYLES (8)", 10f, 0xFF00E5FF);
        styleTitle.setTypeface(null, Typeface.BOLD);
        styleTitle.setPadding(dp(4), dp(12), 0, dp(6));
        content.addView(styleTitle);

        HorizontalScrollView styleScroll = new HorizontalScrollView(this);
        styleScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout styleRow = new LinearLayout(this);
        styleRow.setOrientation(LinearLayout.HORIZONTAL);

        final List<View> styleCardViews = new ArrayList<>();
        for (int i = 0; i < CROSSHAIR_NAMES.length; i++) {
            final int sIdx = i;
            boolean isSel = (style[0] == i);

            LinearLayout sCard = new LinearLayout(this);
            sCard.setOrientation(LinearLayout.VERTICAL);
            sCard.setGravity(Gravity.CENTER);
            sCard.setPadding(dp(10), dp(6), dp(10), dp(6));

            GradientDrawable scBg = new GradientDrawable();
            scBg.setColor(isSel  ?  0xFF142030 : 0xFF10121A);
            scBg.setCornerRadius(dp(10));
            scBg.setStroke(dp(isSel  ?  2 : 1), isSel  ?  0xFF00E5FF : 0xFF2A2D3C);
            sCard.setBackground(scBg);

            // Mini crosshair preview
            View miniPrev = createCrosshairPreview(i, color[0], 0.75f);
            sCard.addView(miniPrev, new LinearLayout.LayoutParams(dp(36), dp(36)));

            TextView sName = label(CROSSHAIR_NAMES[i], 8f, isSel  ?  0xFF00E5FF : 0xFFB0B0B8);
            sName.setTypeface(null, Typeface.BOLD);
            sName.setPadding(0, dp(4), 0, 0);
            sCard.addView(sName);

            sCard.setOnClickListener(v -> {
                style[0] = sIdx;
                for (int k = 0; k < styleCardViews.size(); k++) {
                    LinearLayout c = (LinearLayout) styleCardViews.get(k);
                    boolean cur = (k == sIdx);
                    GradientDrawable g = (GradientDrawable) c.getBackground();
                    g.setColor(cur  ?  0xFF142030 : 0xFF10121A);
                    g.setStroke(dp(cur  ?  2 : 1), cur  ?  0xFF00E5FF : 0xFF2A2D3C);
                    TextView t = (TextView) c.getChildAt(1);
                    t.setTextColor(cur  ?  0xFF00E5FF : 0xFFB0B0B8);
                }
                refreshRadar.run();
            });

            styleCardViews.add(sCard);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
            p.setMarginEnd(dp(6));
            styleRow.addView(sCard, p);
        }
        styleScroll.addView(styleRow, new FrameLayout.LayoutParams(-2, -2));
        content.addView(styleScroll);

        // Color Palette Section
        TextView colorTitle = label("RETICLE COLOR", 10f, 0xFF00E5FF);
        colorTitle.setTypeface(null, Typeface.BOLD);
        colorTitle.setPadding(dp(4), dp(10), 0, dp(6));
        content.addView(colorTitle);

        LinearLayout colorRow = new LinearLayout(this);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);
        colorRow.setGravity(Gravity.CENTER_VERTICAL);

        final List<View> colorCircles = new ArrayList<>();
        for (int c = 0; c < CROSSHAIR_COLORS.length; c++) {
            final int cVal = CROSSHAIR_COLORS[c];
            boolean isCurColor = (color[0] == cVal);

            FrameLayout dotFrame = new FrameLayout(this);
            GradientDrawable ring = new GradientDrawable();
            ring.setShape(GradientDrawable.OVAL);
            ring.setStroke(dp(isCurColor  ?  2 : 0), Color.WHITE);
            dotFrame.setBackground(ring);

            View dot = new View(this);
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(cVal);
            dot.setBackground(d);
            dotFrame.addView(dot, new FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER));

            dotFrame.setOnClickListener(v -> {
                color[0] = cVal;
                for (int k = 0; k < colorCircles.size(); k++) {
                    View df = colorCircles.get(k);
                    boolean match = (CROSSHAIR_COLORS[k] == cVal);
                    GradientDrawable rg = (GradientDrawable) df.getBackground();
                    rg.setStroke(dp(match  ?  2 : 0), Color.WHITE);
                }
                // Refresh style cards mini previews
                for (int k = 0; k < styleCardViews.size(); k++) {
                    LinearLayout sc = (LinearLayout) styleCardViews.get(k);
                    sc.removeViewAt(0);
                    sc.addView(createCrosshairPreview(k, color[0], 0.75f), 0, new LinearLayout.LayoutParams(dp(36), dp(36)));
                }
                refreshRadar.run();
            });

            colorCircles.add(dotFrame);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(26), dp(26));
            clp.setMarginEnd(dp(8));
            colorRow.addView(dotFrame, clp);
        }
        content.addView(colorRow);

        // Size Slider Section
        LinearLayout sizeRow = new LinearLayout(this);
        sizeRow.setOrientation(LinearLayout.HORIZONTAL);
        sizeRow.setGravity(Gravity.CENTER_VERTICAL);
        sizeRow.setPadding(dp(4), dp(8), 0, dp(4));

        TextView sizeTitle = label("RETICLE SIZE", 10f, 0xFF00E5FF);
        sizeTitle.setTypeface(null, Typeface.BOLD);
        sizeRow.addView(sizeTitle, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView sizeBadge = label(size[0] + " dp", 10f, Color.WHITE);
        sizeBadge.setTypeface(null, Typeface.BOLD);
        sizeRow.addView(sizeBadge);
        content.addView(sizeRow);

        SeekBar sizeBar = new SeekBar(this);
        sizeBar.setMax(60);
        sizeBar.setProgress(Math.max(0, Math.min(60, size[0] - 20)));
        sizeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                size[0] = progress + 20;
                sizeBadge.setText(size[0] + " dp");
                refreshRadar.run();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        content.addView(sizeBar);

        // Action Buttons Row (Footer)
        LinearLayout actionsRow = new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setPadding(0, dp(14), 0, 0);

        View resetCrosshair = createCyberPillBackButton("DEFAULT", v -> {
            style[0] = 0;
            color[0] = 0xFF00E5FF;
            size[0] = 52;
            sizeBar.setProgress(32);
            sizeBadge.setText("52 dp");
            refreshRadar.run();
        });
        actionsRow.addView(resetCrosshair, new LinearLayout.LayoutParams(-2, dp(38)));

        View spacer = new View(this);
        actionsRow.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        Button saveBtn = new Button(this);
        saveBtn.setText("SAVE & APPLY RETICLE");
        saveBtn.setTextSize(11f);
        saveBtn.setTypeface(null, Typeface.BOLD);
        saveBtn.setTextColor(Color.WHITE);
        GradientDrawable sBg = new GradientDrawable();
        sBg.setColor(0xFFFF2A3A);
        sBg.setCornerRadius(dp(12));
        saveBtn.setBackground(sBg);
        saveBtn.setOnClickListener(v -> {
            crosshairColor = color[0];
            crosshairSize = size[0];
            plugins.edit()
                    .putBoolean("crosshair", enabled[0])
                    .putInt("crosshair_style", style[0])
                    .putInt("crosshair_color", color[0])
                    .putInt("crosshair_size", size[0])
                    .apply();
            if (tileHolder != null) {
                updateSuperTileState(tileHolder, enabled[0], CROSSHAIR_NAMES[style[0]] + " • " + size[0] + "dp");
            }
            syncCrosshairToService();
            dialog.dismiss();
            Toast.makeText(this, "Tactical reticle (" + CROSSHAIR_NAMES[style[0]] + ") applied", Toast.LENGTH_SHORT).show();
        });
        actionsRow.addView(saveBtn, new LinearLayout.LayoutParams(dp(180), dp(38)));
        content.addView(actionsRow);

        scroll.addView(content, new FrameLayout.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        dialog.setContentView(root);
        dialog.show();
    }

    private int indexOfCrosshairColor(int color) {
        for (int i = 0; i < CROSSHAIR_COLORS.length; i++) {
            if (CROSSHAIR_COLORS[i] == color) return i;
        }
        return 0;
    }

    private LinearLayout.LayoutParams weightedCardParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
        lp.setMargins(dp(4), 0, dp(4), 0);
        return lp;
    }

    private View superSectionTitle(String title, String subtitle) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(dp(5), dp(12), 0, dp(6));
        TextView heading = label(title, 10, 0xFFFF5964);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setLetterSpacing(.10f);
        block.addView(heading);
        block.addView(label(subtitle, 8, 0xFF7F7F88));
        return block;
    }

    private View superProfileCard(String title, String subtitle, int iconRes, int profile) {
        boolean active = launcherProfile == profile;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBaselineAligned(false);
        card.setPadding(dp(12), dp(8), dp(9), dp(8));
        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(active  ?  Color.WHITE : 0xFFFF5964);
        card.addView(icon, new LinearLayout.LayoutParams(dp(27), dp(27)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(9), 0, 0, 0);
        TextView name = label(title, 9.5f, Color.WHITE);
        name.setTypeface(null, Typeface.BOLD);
        copy.addView(name);
        TextView subView = label(active  ?  "CTIVE" : subtitle, 7.5f, active  ?  0xFFFFFFFF : 0xFF9999A2);
        copy.addView(subView);
        card.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                active  ?  new int[]{0x88FF1744, 0x55B71C1C, 0x88FF2D55} : new int[]{0xEE160E15, 0xF80D080D});
        bg.setCornerRadius(dp(11));
        bg.setStroke(dp(active  ?  2 : 1), active  ?  0xFFFF6E79 : 0x55441E2B);
        card.setBackground(bg);

        SuperProfileItem item = new SuperProfileItem();
        item.profile = profile;
        item.card = card;
        item.icon = icon;
        item.subtitleView = subView;
        item.defaultSubtitle = subtitle;
        superProfileItems.add(item);

        card.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(60).withEndAction(() ->
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start()).start();
            applyLauncherProfile(profile);
        });
        return card;
    }

    private void updateSuperProfileCards() {
        for (SuperProfileItem item : superProfileItems) {
            boolean active = launcherProfile == item.profile;
            item.icon.setColorFilter(active  ?  Color.WHITE : 0xFFFF5964);
            item.subtitleView.setText(active  ?  "CTIVE" : item.defaultSubtitle);
            item.subtitleView.setTextColor(active  ?  0xFFFFFFFF : 0xFF9999A2);
            GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    active  ?  new int[]{0x88FF1744, 0x55B71C1C, 0x88FF2D55} : new int[]{0xEE160E15, 0xF80D080D});
            bg.setCornerRadius(dp(11));
            bg.setStroke(dp(active  ?  2 : 1), active  ?  0xFFFF6E79 : 0x55441E2B);
            item.card.setBackground(bg);
        }
    }

    private SuperTileHolder addSuperTile(GridLayout grid, String title, String subtitle, int iconRes, boolean active, SuperTileAction action) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(dp(13), dp(8), dp(10), dp(8));
        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(active  ?  0xFFFF5360 : 0xFF9B9BA4);
        tile.addView(icon, new LinearLayout.LayoutParams(dp(25), dp(25)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(9), 0, 0, 0);
        TextView name = label(title, 9.5f, Color.WHITE);
        name.setTypeface(null, Typeface.BOLD);
        copy.addView(name);
        TextView subView = label(active  ?  subtitle + "  /  ON" : subtitle, 7.5f, active  ?  0xFFFF7881 : 0xFF85858E);
        copy.addView(subView);
        tile.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                active  ?  new int[]{0x77FF1744, 0x448B0018, 0x77FF2D55} : new int[]{0xE6140D14, 0xF20B070D});
        bg.setCornerRadius(dp(11));
        bg.setStroke(dp(1.8f), active  ?  0xFFFF4556 : 0x443A1A26);
        tile.setBackground(bg);

        SuperTileHolder holder = new SuperTileHolder();
        holder.tile = tile;
        holder.icon = icon;
        holder.subView = subView;
        holder.baseSub = subtitle;
        holder.active = active;

        tile.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(60).withEndAction(() ->
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start()).start();
            if (action != null) action.onAction(holder);
        });
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(72);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        grid.addView(tile, lp);
        return holder;
    }

    private void updateSuperTileState(SuperTileHolder holder, boolean active, String customSub) {
        if (holder == null) return;
        holder.active = active;
        holder.icon.setColorFilter(active  ?  0xFFFF5360 : 0xFF9B9BA4);
        String displaySub = customSub != null  ?  customSub : (active  ?  holder.baseSub + "  /  ON" : holder.baseSub);
        holder.subView.setText(displaySub);
        holder.subView.setTextColor(active  ?  0xFFFF7881 : 0xFF85858E);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                active  ?  new int[]{0x77FF1744, 0x448B0018, 0x77FF2D55} : new int[]{0xE6140D14, 0xF20B070D});
        bg.setCornerRadius(dp(11));
        bg.setStroke(dp(1.8f), active  ?  0xFFFF4556 : 0x443A1A26);
        holder.tile.setBackground(bg);
    }

    private View buildCrosshairChoice(int style, boolean active, SharedPreferences plugins) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);

        int chColor = plugins.getInt("crosshair_color", 0xFFFF4350);
        float sz = getCrosshairSizeSafe(plugins);
        float scale = sz / 52f;

        FrameLayout previewContainer = new FrameLayout(this);
        View preview = createCrosshairPreview(style, active  ?  chColor : 0xFFB0B0B8, scale);
        previewContainer.addView(preview, new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.CENTER));
        card.addView(previewContainer, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView name = label(CROSSHAIR_NAMES[style], 8.5f, active  ?  Color.WHITE : 0xFFA0A0A8);
        name.setTypeface(null, Typeface.BOLD);
        name.setGravity(Gravity.CENTER);
        card.addView(name);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(active  ?  (chColor & 0x33FFFFFF) : 0xFF1D1D21);
        bg.setCornerRadius(dp(11));
        bg.setStroke(dp(active  ?  2 : 1), active  ?  chColor : 0xFF404047);
        card.setBackground(bg);

        CrosshairHolder holder = new CrosshairHolder();
        holder.style = style;
        holder.card = card;
        holder.previewContainer = previewContainer;
        holder.nameView = name;
        crosshairHolders.add(holder);

        card.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(60).withEndAction(() ->
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start()).start();
            plugins.edit().putInt("crosshair_style", style).apply();
            updateCrosshairChoices();
            syncCrosshairToService();
        });
        return card;
    }

    private float getCrosshairSizeSafe(SharedPreferences sp) {
        try {
            return sp.getInt("crosshair_size", 52);
        } catch (Exception e) {
            try {
                return sp.getFloat("crosshair_size", 52f);
            } catch (Exception ignored) {
                return 52f;
            }
        }
    }

    private void syncCrosshairToService() {
        try {
            Intent intent = new Intent(this, GameBubbleService.class);
            intent.setAction(GameBubbleService.ACTION_UPDATE_CROSSHAIR);
            intent.putExtra("enabled", getSharedPreferences("ror_game_plugins", MODE_PRIVATE).getBoolean("crosshair", false));
            intent.putExtra("color", crosshairColor);
            intent.putExtra("size", crosshairSize);
            intent.putExtra("style", getSharedPreferences("ror_game_plugins", MODE_PRIVATE).getInt("crosshair_style", 0));
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        } catch (Exception ignored) {}
    }

    private void updateCrosshairChoices() {
        SharedPreferences plugins = getSharedPreferences("ror_game_plugins", MODE_PRIVATE);
        int selected = plugins.getInt("crosshair_style", 0);
        int chColor = plugins.getInt("crosshair_color", 0xFFFF4350);
        float sz = getCrosshairSizeSafe(plugins);
        float scale = sz / 52f;
        for (CrosshairHolder h : crosshairHolders) {
            boolean active = h.style == selected;
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(active  ?  (chColor & 0x33FFFFFF) : 0xFF1D1D21);
            bg.setCornerRadius(dp(11));
            bg.setStroke(dp(active  ?  2 : 1), active  ?  chColor : 0xFF404047);
            h.card.setBackground(bg);
            h.nameView.setTextColor(active  ?  Color.WHITE : 0xFFA0A0A8);
            h.previewContainer.removeAllViews();
            h.previewContainer.addView(createCrosshairPreview(h.style, active  ?  chColor : 0xFFB0B0B8, scale),
                    new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.CENTER));
        }
    }

    private View createCrosshairPreview(int style, int color, float scale) {
        return new View(this) {
            final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                paint.setColor(color);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(2));
                float cx = getWidth() / 2f, cy = getHeight() / 2f;
                float sc = Math.max(0.6f, Math.min(1.4f, scale));
                if (style == 1) {
                    paint.setStyle(Paint.Style.FILL); canvas.drawCircle(cx, cy, dp(3 * sc), paint);
                } else if (style == 2) {
                    float r = dp(13 * sc); float s = dp(6 * sc);
                    canvas.drawLine(cx-r, cy-r, cx-r+s, cy-r, paint); canvas.drawLine(cx-r, cy-r, cx-r, cy-r+s, paint);
                    canvas.drawLine(cx+r, cy-r, cx+r-s, cy-r, paint); canvas.drawLine(cx+r, cy-r, cx+r, cy-r+s, paint);
                    canvas.drawLine(cx-r, cy+r, cx-r+s, cy+r, paint); canvas.drawLine(cx-r, cy+r, cx-r, cy+r-s, paint);
                    canvas.drawLine(cx+r, cy+r, cx+r-s, cy+r, paint); canvas.drawLine(cx+r, cy+r, cx+r, cy+r-s, paint);
                    paint.setStyle(Paint.Style.FILL); canvas.drawCircle(cx, cy, dp(2 * sc), paint);
                } else if (style == 3) {
                    float r = dp(12 * sc); canvas.drawLine(cx-r,cy-r,cx-dp(3*sc),cy-dp(3*sc),paint); canvas.drawLine(cx+r,cy-r,cx+dp(3*sc),cy-dp(3*sc),paint);
                    canvas.drawLine(cx-r,cy+r,cx-dp(3*sc),cy+dp(3*sc),paint); canvas.drawLine(cx+r,cy+r,cx+dp(3*sc),cy+dp(3*sc),paint);
                    canvas.drawCircle(cx, cy, dp(4 * sc), paint);
                } else if (style == 4) {
                    canvas.drawCircle(cx, cy, dp(13 * sc), paint); canvas.drawCircle(cx, cy, dp(5 * sc), paint);
                    canvas.drawLine(cx-dp(16)*sc, cy, cx-dp(13)*sc, cy, paint);
                    canvas.drawLine(cx+dp(13)*sc, cy, cx+dp(16)*sc, cy, paint);
                    canvas.drawLine(cx, cy-dp(16)*sc, cx, cy-dp(13)*sc, paint);
                    canvas.drawLine(cx, cy+dp(13)*sc, cx, cy+dp(16)*sc, paint);
                } else if (style == 5) { // Diamond Reflex
                    float d = dp(13 * sc);
                    Path dia = new Path();
                    dia.moveTo(cx, cy - d); dia.lineTo(cx + d, cy); dia.lineTo(cx, cy + d); dia.lineTo(cx - d, cy); dia.close();
                    canvas.drawPath(dia, paint);
                    paint.setStyle(Paint.Style.FILL); canvas.drawCircle(cx, cy, dp(2 * sc), paint);
                } else if (style == 6) { // Tri-Spoke SpecOps
                    float r = dp(14 * sc), g = dp(4 * sc);
                    canvas.drawLine(cx, cy - r, cx, cy - g, paint);
                    float dx = (float) (r * Math.cos(Math.PI / 6)), dy = (float) (r * Math.sin(Math.PI / 6));
                    float gx = (float) (g * Math.cos(Math.PI / 6)), gy = (float) (g * Math.sin(Math.PI / 6));
                    canvas.drawLine(cx - dx, cy + dy, cx - gx, cy + gy, paint);
                    canvas.drawLine(cx + dx, cy + dy, cx + gx, cy + gy, paint);
                    paint.setStyle(Paint.Style.FILL); canvas.drawCircle(cx, cy, dp(2 * sc), paint);
                } else if (style == 7) { // Biohazard Gamer
                    float r = dp(14 * sc);
                    RectF arcRect = new RectF(cx - r, cy - r, cx + r, cy + r);
                    canvas.drawArc(arcRect, 30, 60, false, paint);
                    canvas.drawArc(arcRect, 150, 60, false, paint);
                    canvas.drawArc(arcRect, 270, 60, false, paint);
                    paint.setStyle(Paint.Style.FILL); canvas.drawCircle(cx, cy, dp(2.5f) * sc, paint);
                } else {
                    float gap=dp(5 * sc), r=dp(16 * sc); canvas.drawLine(cx-r,cy,cx-gap,cy,paint); canvas.drawLine(cx+gap,cy,cx+r,cy,paint);
                    canvas.drawLine(cx,cy-r,cx,cy-gap,paint); canvas.drawLine(cx,cy+gap,cx,cy+r,paint); canvas.drawCircle(cx,cy,dp(4 * sc),paint);
                }
            }
        };
    }

    private void togglePluginPreference(String key, String label) {
        SharedPreferences prefs = getSharedPreferences("ror_game_plugins", MODE_PRIVATE);
        boolean next = !prefs.getBoolean(key, false);
        prefs.edit().putBoolean(key, next).apply();
        Toast.makeText(this, label + (next  ?  " enabled" : " disabled"), Toast.LENGTH_SHORT).show();
        rebuildView();
    }

    private void toggleLauncherAntiDisturb() {
        SharedPreferences prefs = getSharedPreferences("ror_game_plugins", MODE_PRIVATE);
        boolean next = !prefs.getBoolean("anti_disturb", false);
        prefs.edit().putBoolean("anti_disturb", next).apply();
        runLauncherCommand(next  ?  "settings put global heads_up_notifications_enabled 0; cmd notification set_dnd priority"
                : "settings put global heads_up_notifications_enabled 1; cmd notification set_dnd off");
        rebuildView();
    }

    private void toggleLauncherBrightness() {
        SharedPreferences prefs = getSharedPreferences("ror_game_plugins", MODE_PRIVATE);
        boolean next = !prefs.getBoolean("brightness_lock", false);
        prefs.edit().putBoolean("brightness_lock", next).apply();
        runLauncherCommand(next  ?  "settings put system screen_brightness_mode 0; settings put system screen_brightness 220"
                : "settings put system screen_brightness_mode 0");
        rebuildView();
    }

    private void toggleLauncherRefresh() {
        SharedPreferences prefs = getSharedPreferences("ror_game_plugins", MODE_PRIVATE);
        boolean next = !prefs.getBoolean("high_refresh", false);
        prefs.edit().putBoolean("high_refresh", next).apply();
        runLauncherCommand(next  ?  "settings put system min_refresh_rate 120; settings put system peak_refresh_rate 120"
                : "settings delete system min_refresh_rate; settings delete system peak_refresh_rate");
        rebuildView();
    }

    private void runLauncherCommand(String command) {
        new Thread(() -> {
            if (GsmProfileManager.isRootEnabled(this)) com.revror.kernelmanager.utils.root.RootUtils.runCommand(command);
            else if (ShizukuBridge.granted()) ShizukuBridge.run(command);
        }, "ror-superbase").start();
    }

    private View buildRORHeader() {
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        // Cutout-safe responsive padding: 22dp start and end prevents camera cutouts & curved corners from cutting buttons
        root.setPadding(dp(20), dp(4), dp(22), dp(4));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        View back = createCyberPillBackButton(showSuperBase  ?  "GAME LOBBY" : "DASHBOARD", v -> {
            if (showSuperBase) {
                showSuperBase = false;
                rebuildView();
            } else finish();
        });
        topRow.addView(back);

        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.ror_logo);
        LinearLayout.LayoutParams markLp = new LinearLayout.LayoutParams(dp(28), dp(28));
        markLp.setMargins(dp(8), 0, dp(6), 0);
        mark.setOnClickListener(v -> showGameSpaceCenterDialog());
        topRow.addView(mark, markLp);

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setOnClickListener(v -> showGameSpaceCenterDialog());
        TextView brandName = label("ROR", 11.5f, Color.WHITE);
        brandName.setTypeface(null, Typeface.BOLD);
        brandName.setLetterSpacing(.16f);
        brand.addView(brandName);
        TextView product = label("GAME SPACE", 7.5f, 0xFFFF3B47);
        product.setTypeface(null, Typeface.BOLD);
        product.setLetterSpacing(.14f);
        brand.addView(product);
        topRow.addView(brand);

        hudCpu = createHudPill(R.drawable.ic_cpu, "CPU --", 0xFFFF5B63);
        hudGpu = createHudPill(R.drawable.ic_battery, "BAT --", 0xFFF1F1F1);
        hudRam = createHudPill(R.drawable.ic_layers, "RAM --", 0xFFF1F1F1);
        hudTemp = createHudPill(R.drawable.ic_temperature, "--", 0xFFFF9F43);
        hudClock = createHudPill(R.drawable.ic_clock, "--", 0xFFC8C8CC);

        int refreshFps = 60;
        try {
            refreshFps = (int) Math.round(getWindowManager().getDefaultDisplay().getRefreshRate());
        } catch (Exception ignored) {}
        TextView hudFps = createHudPill(R.drawable.ic_speed_meter, refreshFps + " FPS", 0xFFFFD600);
        hudFps.setOnClickListener(v -> cycleRefreshRateDirect());

        View.OnClickListener openTelem = v -> showGamingTelemetryDialog();
        hudCpu.setOnClickListener(openTelem);
        hudGpu.setOnClickListener(openTelem);
        hudRam.setOnClickListener(openTelem);
        hudTemp.setOnClickListener(openTelem);

        customTelemetryCpuViews.add(hudCpu);
        customTelemetryBatViews.add(hudGpu);
        customTelemetryRamViews.add(hudRam);
        customTelemetryTempViews.add(hudTemp);
        customTelemetryClockViews.add(hudClock);
        customTelemetryFpsViews.add(hudFps);

        if (!isPortrait) {
            HorizontalScrollView hudScroll = new HorizontalScrollView(this);
            hudScroll.setHorizontalScrollBarEnabled(false);
            hudScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
            LinearLayout hudRow = new LinearLayout(this);
            hudRow.setOrientation(LinearLayout.HORIZONTAL);
            hudRow.setGravity(Gravity.CENTER_VERTICAL);
            hudRow.addView(hudCpu);
            hudRow.addView(hudGpu);
            hudRow.addView(hudRam);
            hudRow.addView(hudTemp);
            hudRow.addView(hudFps);
            hudRow.addView(hudClock);
            hudScroll.addView(hudRow, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_VERTICAL));
            LinearLayout.LayoutParams hudScrollLp = new LinearLayout.LayoutParams(0, -2, 1f);
            hudScrollLp.setMargins(dp(8), 0, dp(8), 0);
            topRow.addView(hudScroll, hudScrollLp);
        } else {
            View spacer = new View(this);
            topRow.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
        }

        LinearLayout actionsContainer = new LinearLayout(this);
        actionsContainer.setOrientation(LinearLayout.HORIZONTAL);
        actionsContainer.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);

        TextView briBtn = redHeaderButton(showBrightnessBar ? "BRIGHTNESS: ON" : "BRIGHTNESS", R.drawable.ic_sun);
        briBtn.setOnClickListener(v -> toggleBrightnessBar());
        LinearLayout.LayoutParams briLp = new LinearLayout.LayoutParams(-2, -2);
        briLp.setMarginStart(dp(4));
        actionsContainer.addView(briBtn, briLp);

        TextView layoutBtn = redHeaderButton("LAYOUT", R.drawable.ic_layers);
        layoutBtn.setOnClickListener(v -> showLayoutChooser());
        LinearLayout.LayoutParams layoutLp = new LinearLayout.LayoutParams(-2, -2);
        layoutLp.setMarginStart(dp(4));
        actionsContainer.addView(layoutBtn, layoutLp);

        TextView rotate = redHeaderButton(isPortrait  ?  "LANDSCAPE" : "PORTRAIT", R.drawable.ic_switch);
        rotate.setOnClickListener(v -> toggleOrientation());
        LinearLayout.LayoutParams rotateLp = new LinearLayout.LayoutParams(-2, -2);
        rotateLp.setMarginStart(dp(4));
        actionsContainer.addView(rotate, rotateLp);

        TextView controls = redHeaderButton("CONTROLS", R.drawable.ic_settings_icon);
        GradientDrawable active = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFFE3182D, 0xFFFF3548});
        active.setCornerRadius(dp(8));
        active.setStroke(dp(1), 0xFFFF6D77);
        controls.setBackground(active);
        controls.setOnClickListener(v -> {
            showSuperBase = true;
            rebuildView();
        });
        LinearLayout.LayoutParams controlsLp = new LinearLayout.LayoutParams(-2, -2);
        controlsLp.setMarginStart(dp(4));
        actionsContainer.addView(controls, controlsLp);

        topRow.addView(actionsContainer, new LinearLayout.LayoutParams(-2, -2));

        HorizontalScrollView topRowScroll = new HorizontalScrollView(this);
        topRowScroll.setHorizontalScrollBarEnabled(false);
        topRowScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        topRowScroll.addView(topRow, new FrameLayout.LayoutParams(-1, dp(44)));
        root.addView(topRowScroll, new LinearLayout.LayoutParams(-1, dp(44)));

        View rorBriBar = buildGameSpaceBrightnessBar(0xFFFF1744);
        if (rorBriBar != null) root.addView(rorBriBar);

        if (isPortrait) {
            HorizontalScrollView hudScroll = new HorizontalScrollView(this);
            hudScroll.setHorizontalScrollBarEnabled(false);
            hudScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
            LinearLayout hudRow = new LinearLayout(this);
            hudRow.setOrientation(LinearLayout.HORIZONTAL);
            hudRow.setGravity(Gravity.CENTER_VERTICAL);
            hudRow.setPadding(0, dp(4), 0, dp(4));
            hudRow.addView(hudCpu);
            hudRow.addView(hudGpu);
            hudRow.addView(hudRam);
            hudRow.addView(hudTemp);
            hudRow.addView(hudFps);
            hudRow.addView(hudClock);
            hudScroll.addView(hudRow, new FrameLayout.LayoutParams(-2, -2));
            root.addView(hudScroll, new LinearLayout.LayoutParams(-1, -2));
        }

        return root;
    }

    private TextView redHeaderButton(String text) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextSize(8.5f);
        button.setTextColor(Color.WHITE);
        button.setTypeface(null, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(7), dp(4), dp(7), dp(4));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF17171B);
        bg.setCornerRadius(dp(8));
        bg.setStroke(dp(1), 0xFF36363D);
        button.setBackground(bg);
        return button;
    }

    private TextView redHeaderButton(String text, int iconRes) {
        TextView button = redHeaderButton(text);
        Drawable icon = ContextCompat.getDrawable(this, iconRes);
        if (icon != null) {
            icon = DrawableCompat.wrap(icon.mutate());
            DrawableCompat.setTint(icon, Color.WHITE);
            icon.setBounds(0, 0, dp(16), dp(16));
            button.setCompoundDrawables(icon, null, null, null);
            button.setCompoundDrawablePadding(dp(6));
        }
        return button;
    }

    private TextView label(String text, float size, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private ImageView controlIcon(int drawableRes, int tint, boolean enabled) {
        ImageView view = new ImageView(this);
        view.setImageResource(drawableRes);
        view.setColorFilter(tint);
        view.setPadding(dp(8), dp(8), dp(8), dp(8));
        view.setEnabled(enabled);
        view.setFocusable(enabled);
        return view;
    }
    
    private void toggleBrightnessBar() {
        showBrightnessBar = !showBrightnessBar;
        getSharedPreferences("ror_game_launcher", 0).edit().putBoolean("show_brightness_bar", showBrightnessBar).apply();
        rebuildView();
    }

    private View buildGameSpaceBrightnessBar(int accentColor) {
        if (!showBrightnessBar) return null;

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(12), dp(4), dp(12), dp(4));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xEE0E1118);
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1.2f), accentColor != 0 ? accentColor : 0xFFFF1744);
        bar.setBackground(bg);

        ImageView sunIcon = new ImageView(this);
        sunIcon.setImageResource(R.drawable.ic_sun);
        sunIcon.setColorFilter(accentColor != 0 ? accentColor : 0xFFFF1744);
        bar.addView(sunIcon, new LinearLayout.LayoutParams(dp(18), dp(18)));

        int currentBri = 128;
        try {
            currentBri = android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS, 128);
        } catch (Exception ignored) {}
        int currentPct = Math.round((currentBri / 255.0f) * 100);

        TextView briLabel = label("  " + currentPct + "%", 9.5f, Color.WHITE);
        briLabel.setTypeface(null, Typeface.BOLD);
        briLabel.setMinWidth(dp(44));
        bar.addView(briLabel);

        SeekBar seek = new SeekBar(this);
        seek.setMax(255);
        seek.setProgress(currentBri);
        seek.setProgressTintList(ColorStateList.valueOf(accentColor != 0 ? accentColor : 0xFFFF1744));
        seek.setThumbTintList(ColorStateList.valueOf(accentColor != 0 ? accentColor : 0xFFFF1744));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int val = Math.max(10, progress);
                    int pct = Math.round((val / 255.0f) * 100);
                    briLabel.setText("  " + pct + "%");
                    try {
                        android.provider.Settings.System.putInt(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE, android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                        android.provider.Settings.System.putInt(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS, val);
                    } catch (Exception ignored) {}
                    runLauncherCommand("settings put system screen_brightness_mode 0; settings put system screen_brightness " + val);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        LinearLayout.LayoutParams seekLp = new LinearLayout.LayoutParams(0, -2, 1f);
        seekLp.setMargins(dp(8), 0, dp(8), 0);
        bar.addView(seek, seekLp);

        TextView closeBtn = label("HIDE", 8.5f, 0xFF8E95B2);
        closeBtn.setTypeface(null, Typeface.BOLD);
        closeBtn.setPadding(dp(8), dp(3), dp(8), dp(3));
        GradientDrawable cbBg = new GradientDrawable();
        cbBg.setColor(0x33FFFFFF);
        cbBg.setCornerRadius(dp(6));
        closeBtn.setBackground(cbBg);
        closeBtn.setOnClickListener(v -> toggleBrightnessBar());
        bar.addView(closeBtn);

        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(-1, -2);
        barLp.setMargins(dp(12), dp(2), dp(12), dp(4));
        bar.setLayoutParams(barLp);
        return bar;
    }


    private View attachResponsivePanels(View centerContent, int activeLayout, boolean isPortrait) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float heightDp = dm.heightPixels / dm.density;
        // Account for the header, bottom controls and universal ad slot. Devices
        // around 540-620dp tall otherwise used desktop-sized panels and exposed a
        // partially clipped card behind the bottom action bar.
        boolean isCompact = !isPortrait && heightDp < 640;

        if (!isPortrait) {
            LinearLayout bodyRow = new LinearLayout(this);
            bodyRow.setOrientation(LinearLayout.HORIZONTAL);
            bodyRow.setGravity(Gravity.CENTER_VERTICAL);

            View telemetry = buildGamingTelemetrySuite(activeLayout, false);
            LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(dp(isCompact ? 212 : 242), -1);
            tLp.setMarginEnd(dp(isCompact ? 6 : 8));
            bodyRow.addView(telemetry, tLp);

            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0, -1, 1f);
            cLp.setMarginEnd(dp(isCompact ? 6 : 8));
            bodyRow.addView(centerContent, cLp);

            View energy = buildEnergyCorePanel(activeLayout, false);
            LinearLayout.LayoutParams eLp = new LinearLayout.LayoutParams(dp(isCompact ? 212 : 242), -1);
            bodyRow.addView(energy, eLp);

            return bodyRow;
        } else {
            if (centerContent instanceof ScrollView) {
                ScrollView sv = (ScrollView) centerContent;
                if (sv.getChildCount() > 0 && sv.getChildAt(0) instanceof LinearLayout) {
                    LinearLayout innerCol = (LinearLayout) sv.getChildAt(0);
                    View telemetry = buildGamingTelemetrySuite(activeLayout, true);
                    LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(-1, -2);
                    tLp.topMargin = dp(10);
                    innerCol.addView(telemetry, tLp);

                    View energy = buildEnergyCorePanel(activeLayout, true);
                    LinearLayout.LayoutParams eLp = new LinearLayout.LayoutParams(-1, -2);
                    eLp.topMargin = dp(10);
                    eLp.bottomMargin = dp(8);
                    innerCol.addView(energy, eLp);
                    return sv;
                }
            }

            ScrollView bodyScroll = new ScrollView(this);
            bodyScroll.setFillViewport(true);
            bodyScroll.setVerticalScrollBarEnabled(false);
            bodyScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

            LinearLayout bodyCol = new LinearLayout(this);
            bodyCol.setOrientation(LinearLayout.VERTICAL);

            bodyCol.addView(centerContent, new LinearLayout.LayoutParams(-1, -2));

            View telemetry = buildGamingTelemetrySuite(activeLayout, true);
            LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(-1, -2);
            tLp.topMargin = dp(8);
            bodyCol.addView(telemetry, tLp);

            View energy = buildEnergyCorePanel(activeLayout, true);
            LinearLayout.LayoutParams eLp = new LinearLayout.LayoutParams(-1, -2);
            eLp.topMargin = dp(8);
            eLp.bottomMargin = dp(6);
            bodyCol.addView(energy, eLp);

            bodyScroll.addView(bodyCol, new ScrollView.LayoutParams(-1, -2));
            return bodyScroll;
        }
    }


    private void toggleInGameFpsOverlay() {
        boolean active = getSharedPreferences("ror_tools", MODE_PRIVATE).getBoolean("overlay", false);
        if (active) {
            startService(new Intent(this, com.revror.kernelmanager.services.CpuOverlayService.class).setAction(com.revror.kernelmanager.services.CpuOverlayService.ACTION_STOP));
            getSharedPreferences("ror_tools", MODE_PRIVATE).edit().putBoolean("overlay", false).apply();
            Toast.makeText(this, "In-Game FPS Monitor: Disabled", Toast.LENGTH_SHORT).show();
        } else {
            if (Build.VERSION.SDK_INT >= 23 && !android.provider.Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please grant overlay permission for In-Game FPS Monitor", Toast.LENGTH_LONG).show();
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception ignored) {}
                return;
            }
            Intent intent = new Intent(this, com.revror.kernelmanager.services.CpuOverlayService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent);
            else startService(intent);
            getSharedPreferences("ror_tools", MODE_PRIVATE).edit().putBoolean("overlay", true).apply();
            Toast.makeText(this, "In-Game Real-Time FPS Monitor: Active!", Toast.LENGTH_SHORT).show();
        }
        rebuildView();
    }

    private View buildGamingTelemetrySuite() {
        return buildGamingTelemetrySuite(Math.floorMod(layoutIndex, LAYOUT_NAMES.length), false);
    }

    private View buildGamingTelemetrySuite(int layoutStyle, boolean isPortrait) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float heightDp = dm.heightPixels / dm.density;
        boolean isCompact = !isPortrait && heightDp < 640;

        int accentColor = 0xFFFF1744;
        int bgStart = 0xF2160C14;
        int bgEnd = 0xFA0B070D;
        int strokeColor = 0x555C1827;
        int cardBg1 = 0xE6160F16;
        int cardBg2 = 0xF50D080D;
        int cardStroke = 0x444A1B28;
        int cpuColor = 0xFF00E5FF;
        int ramColor = 0xFFC084FC;
        String suiteTitle = "GAMING TELEMETRY";
        String suiteSubtitle = "SYSTEM SENSORS & TOOLS";
        int cornerRadiusDp = 16;

        switch (layoutStyle) {
            case 1: // Mecha Deck (Electric Cyan)
                accentColor = 0xFF00E5FF;
                bgStart = 0xF2081524; bgEnd = 0xFA040B14; strokeColor = 0x5500E5FF;
                cardBg1 = 0xE60A1A2C; cardBg2 = 0xF506101C; cardStroke = 0x4400E5FF;
                cpuColor = 0xFF00E5FF; ramColor = 0xFF00E676;
                suiteTitle = "TITAN TELEMETRY"; suiteSubtitle = "CYBER MECHA SENSORS";
                cornerRadiusDp = 12;
                break;
            case 2: // Armoury Matrix (Neon Orange/Red)
                accentColor = 0xFFFF3D00;
                bgStart = 0xF214080D; bgEnd = 0xFA0A0407; strokeColor = 0x66FF1744;
                cardBg1 = 0xE61E0A12; cardBg2 = 0xF510050A; cardStroke = 0x55FF1744;
                cpuColor = 0xFFFF1744; ramColor = 0xFFFFD600;
                suiteTitle = "MATRIX TELEMETRY"; suiteSubtitle = "CYBERPUNK SENSORS";
                cornerRadiusDp = 10;
                break;
            case 3:
            case 20: // Vortex Reactor (Cryo Ice Blue)
                accentColor = 0xFF00E5FF;
                bgStart = 0xF2041220; bgEnd = 0xFA020912; strokeColor = 0x5500B0FF;
                cardBg1 = 0xE606182A; cardBg2 = 0xF5030E1A; cardStroke = 0x4400B0FF;
                cpuColor = 0xFF00E5FF; ramColor = 0xFF2979FF;
                suiteTitle = "CRYO TELEMETRY"; suiteSubtitle = "TURBINE SENSORS";
                cornerRadiusDp = 16;
                break;
            case 4:
            case 21: // Apex Horizon (Radioactive Green / Speed Gold)
                accentColor = 0xFF00E676;
                bgStart = 0xF206160C; bgEnd = 0xFA030D06; strokeColor = 0x5500E676;
                cardBg1 = 0xE6092012; cardBg2 = 0xF505140A; cardStroke = 0x4400E676;
                cpuColor = 0xFF00E676; ramColor = 0xFFFFD600;
                suiteTitle = "HORIZON HUD"; suiteSubtitle = "SPEED SENSORS";
                cornerRadiusDp = 14;
                break;
            case 5: // Turbo Cockpit (Avionics Amber)
                accentColor = 0xFFFF9100;
                bgStart = 0xF2160C03; bgEnd = 0xFA0D0701; strokeColor = 0x66FF6D00;
                cardBg1 = 0xE6201004; cardBg2 = 0xF5140A02; cardStroke = 0x55FF6D00;
                cpuColor = 0xFFFF9100; ramColor = 0xFF00E5FF;
                suiteTitle = "AVIONICS HUD"; suiteSubtitle = "FLIGHT TELEMETRY";
                cornerRadiusDp = 12;
                break;
            case 8:
            case 14:
            case 15:
            case 22: // Console Stage Family
                accentColor = 0xFF7C4DFF;
                bgStart = 0xF2120C22; bgEnd = 0xFA080512; strokeColor = 0x557C4DFF;
                cardBg1 = 0xE6180F2E; cardBg2 = 0xF50F091C; cardStroke = 0x447C4DFF;
                cpuColor = 0xFFB388FF; ramColor = 0xFF00E5FF;
                suiteTitle = "CONSOLE TELEMETRY"; suiteSubtitle = "STATION SENSORS";
                cornerRadiusDp = 14;
                break;
            case 10:
            case 23:
            case 29:
            case 30:
            case 31: // Split & Hero Family (Cyber Neon Violet)
                accentColor = 0xFFE040FB;
                bgStart = 0xF21C0A26; bgEnd = 0xFA0E0414; strokeColor = 0x66E040FB;
                cardBg1 = 0xE6260D34; cardBg2 = 0xF516071E; cardStroke = 0x55E040FB;
                cpuColor = 0xFFE040FB; ramColor = 0xFF7C4DFF;
                suiteTitle = "HERO TELEMETRY"; suiteSubtitle = "CYBER SENSORS";
                cornerRadiusDp = 14;
                break;
            case 11:
            case 12:
            case 13:
            case 16:
            case 17:
            case 18:
            case 19:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 32: // Grid Catalog Family (Matrix Cyan / Emerald)
                accentColor = 0xFF00E5FF;
                bgStart = 0xF2061A22; bgEnd = 0xFA030E14; strokeColor = 0x5500E5FF;
                cardBg1 = 0xE6082430; cardBg2 = 0xF504141C; cardStroke = 0x4400E5FF;
                cpuColor = 0xFF00E5FF; ramColor = 0xFF00E676;
                suiteTitle = "CATALOG SENSORS"; suiteSubtitle = "GRID TELEMETRY";
                cornerRadiusDp = 12;
                break;
            case 33: // Hexagon Grid Family (Honeycomb Amber Gold)
                accentColor = 0xFFFFD600;
                bgStart = 0xF2201804; bgEnd = 0xFA120E02; strokeColor = 0x66FFD600;
                cardBg1 = 0xE62A2006; cardBg2 = 0xF5181203; cardStroke = 0x55FFD600;
                cpuColor = 0xFFFFD600; ramColor = 0xFFFF9100;
                suiteTitle = "HEXAGON MATRIX"; suiteSubtitle = "HONEYCOMB HUD";
                cornerRadiusDp = 16;
                break;
            default:
                if (layoutStyle != 0) {
                    accentColor = 0xFF2979FF;
                    bgStart = 0xF2081422; bgEnd = 0xFA040B14; strokeColor = 0x552979FF;
                    cardBg1 = 0xE60B1C30; cardBg2 = 0xF506101C; cardStroke = 0x442979FF;
                    cpuColor = 0xFF2979FF; ramColor = 0xFF00E5FF;
                    suiteTitle = "STREAM TELEMETRY"; suiteSubtitle = "LIVE HARDWARE";
                    cornerRadiusDp = 14;
                }
                break;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(isCompact ? 10 : 12), dp(isCompact ? 8 : 10), dp(isCompact ? 10 : 12), dp(isCompact ? 6 : 8));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{bgStart, bgEnd});
        bg.setCornerRadius(dp(cornerRadiusDp));
        bg.setStroke(dp(1.2f), strokeColor);
        root.setBackground(bg);

        TextView title = label(suiteTitle, isCompact ? 11f : 12f, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setLetterSpacing(.11f);
        root.addView(title);
        TextView subtitle = label(suiteSubtitle, isCompact ? 7.5f : 8f, accentColor);
        subtitle.setLetterSpacing(.12f);
        subtitle.setPadding(0, dp(1), 0, dp(isCompact ? 4 : 8));
        root.addView(subtitle);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        // 1. Live Display Refresh & FPS Card (Interactive!)
        LinearLayout fpsCard = createTelemetryCard(isCompact, cardBg1, cardBg2, cardStroke);
        TextView fpsBadge = label("• LIVE REFRESH", 7.5f, accentColor);
        fpsBadge.setTypeface(null, Typeface.BOLD);
        fpsCard.addView(fpsBadge);
        telemetryFpsText = label("120 Hz  |  120 FPS TARGET", isCompact ? 9f : 10.5f, Color.WHITE);
        telemetryFpsText.setTypeface(null, Typeface.BOLD);
        telemetryFpsText.setPadding(0, dp(1), 0, dp(2));
        fpsCard.addView(telemetryFpsText);
        TextView fpsSub = label("Tap to force-set refresh rate (60/90/120/144Hz)", 7f, 0xFF8E95B2);
        fpsCard.addView(fpsSub);
        fpsCard.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            cycleRefreshRateDirect();
        });
        content.addView(fpsCard);

        // 2. CPU Load & Frequency Card
        LinearLayout cpuCard = createTelemetryCard(isCompact, cardBg1, cardBg2, cardStroke);
        telemetryCpuText = label("CPU 0%  |  OCTA-CORE LOAD", isCompact ? 8.5f : 9.5f, cpuColor);
        telemetryCpuText.setTypeface(null, Typeface.BOLD);
        cpuCard.addView(telemetryCpuText);
        telemetryCpuBar = createMiniProgress(cpuColor, isCompact);
        cpuCard.addView(telemetryCpuBar);
        content.addView(cpuCard);

        // 3. RAM Memory Card
        LinearLayout ramCard = createTelemetryCard(isCompact, cardBg1, cardBg2, cardStroke);
        telemetryRamText = label("RAM 0%  |  MEMORY HEALTHY", isCompact ? 8.5f : 9.5f, ramColor);
        telemetryRamText.setTypeface(null, Typeface.BOLD);
        ramCard.addView(telemetryRamText);
        telemetryRamBar = createMiniProgress(ramColor, isCompact);
        ramCard.addView(telemetryRamBar);
        content.addView(ramCard);

        // 4. Thermals & Ping Row (Split 2 Columns)
        LinearLayout dualRow = new LinearLayout(this);
        dualRow.setOrientation(LinearLayout.HORIZONTAL);
        dualRow.setBaselineAligned(false);

        LinearLayout tempCard = createTelemetryCard(isCompact, cardBg1, cardBg2, cardStroke);
        telemetryTempText = label("TEMP 35°C  |  BAT 85%", isCompact ? 8f : 8.5f, 0xFFFBBF24);
        telemetryTempText.setTypeface(null, Typeface.BOLD);
        telemetryTempText.setGravity(Gravity.CENTER);
        tempCard.addView(telemetryTempText);
        dualRow.addView(tempCard, new LinearLayout.LayoutParams(0, -2, 1f));

        LinearLayout pingCard = createTelemetryCard(isCompact, cardBg1, cardBg2, cardStroke);
        telemetryPingText = label("PING 22ms  |  LOW LATENCY", isCompact ? 8f : 8.5f, 0xFF34D399);
        telemetryPingText.setTypeface(null, Typeface.BOLD);
        telemetryPingText.setGravity(Gravity.CENTER);
        pingCard.addView(telemetryPingText);
        LinearLayout.LayoutParams pingLp = new LinearLayout.LayoutParams(0, -2, 1f);
        pingLp.setMarginStart(dp(4));
        dualRow.addView(pingCard, pingLp);
        content.addView(dualRow);

        // 5. Gaming Suite Action Buttons
        LinearLayout toolsTitle = new LinearLayout(this);
        toolsTitle.setPadding(0, dp(4), 0, dp(2));
        TextView tLbl = label("QUICK GAMING ASSIST", 7.5f, accentColor);
        tLbl.setTypeface(null, Typeface.BOLD);
        tLbl.setLetterSpacing(.1f);
        toolsTitle.addView(tLbl);
        content.addView(toolsTitle);

        content.addView(redToolButton(R.drawable.ic_floating_window, "TACTICAL CROSSHAIR", isCompact, () -> {
            showUnifiedCrosshairDialog(null);
        }, accentColor, cardBg1, cardBg2, cardStroke));

        content.addView(redToolButton(R.drawable.ic_flash, "TOUCH 240Hz BOOST", isCompact, () -> {
            runLauncherCommand("settings put system touch_response_rate 1; settings put secure high_touch_polling_rate 1;");
            Toast.makeText(this, "Touch Boost: 240Hz Ultra-Low Latency Armed", Toast.LENGTH_SHORT).show();
        }, accentColor, cardBg1, cardBg2, cardStroke));

        content.addView(redToolButton(R.drawable.ic_add, "MANAGE GAME LIBRARY", isCompact, this::showManageGamesDialog, accentColor, cardBg1, cardBg2, cardStroke));

        content.addView(redToolButton(R.drawable.ic_shield, "BYPASS CHARGING SHIELD", isCompact, () -> {
            runLauncherCommand("echo 1 > /sys/class/power_supply/battery/charging_enabled 2>/dev/null;");
            Toast.makeText(this, "Thermal Protection: Direct Power Bypass Enabled", Toast.LENGTH_SHORT).show();
        }, accentColor, cardBg1, cardBg2, cardStroke));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
        return root;
    }

        private LinearLayout createTelemetryCard(boolean isCompact, int bgStart, int bgEnd, int stroke) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(isCompact ? 8 : 10), dp(isCompact ? 5 : 7), dp(isCompact ? 8 : 10), dp(isCompact ? 5 : 7));
        GradientDrawable bg = new GradientDrawable();
        bg.setOrientation(GradientDrawable.Orientation.TL_BR);
        bg.setColors(new int[]{bgStart, bgEnd});
        bg.setCornerRadius(dp(9));
        bg.setStroke(dp(1), stroke);
        card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(isCompact ? 4 : 5);
        card.setLayoutParams(lp);
        return card;
    }

    private LinearLayout createTelemetryCard(boolean isCompact) {
        return createTelemetryCard(isCompact, 0xE6160F16, 0xF50D080D, 0x444A1B28);
    }

    private ProgressBar createMiniProgress(int color, boolean isCompact) {
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(20);
        bar.setProgressTintList(ColorStateList.valueOf(color));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(0x33FFFFFF));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(4));
        lp.topMargin = dp(isCompact  ?  2 : 3);
        bar.setLayoutParams(lp);
        return bar;
    }

    private void cycleRefreshRateDirect() {
        int[] rates = {60, 90, 120, 144};
        int current = 60;
        try {
            current = (int) Math.round(getWindowManager().getDefaultDisplay().getRefreshRate());
        } catch (Exception ignored) {}
        int next = 120;
        for (int i = 0; i < rates.length; i++) {
            if (rates[i] > current) {
                next = rates[i];
                break;
            }
        }
        if (next == current) next = rates[0];
        final int target = next;
        runLauncherCommand("settings put system peak_refresh_rate " + target
                + "; settings put system min_refresh_rate " + target
                + "; settings put global peak_refresh_rate " + target
                + "; settings put global min_refresh_rate " + target);
        if (telemetryFpsText != null) {
            telemetryFpsText.setText(target + " Hz  |  " + target + " FPS TARGET");
        }
        Toast.makeText(this, "Display Force-Set to " + target + " Hz", Toast.LENGTH_SHORT).show();
    }

    private View buildRORHero() {
        int activeLayout = Math.floorMod(layoutIndex, LAYOUT_NAMES.length);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float heightDp = dm.heightPixels / dm.density;
        boolean isCompact = heightDp < 460;

        int[] cardBgColors;
        int cardStrokeColor;
        float cardStrokeWidthDp = 1.0f;
        int[] railBgColors;
        int railWidthDp = 4;
        int iconBgColor = 0xFF111116;
        int iconStrokeColor = 0xFF4B4B54;
        int iconCornerDp = isCompact  ?  16 : 20;
        String kickerText = "CURRENT GAME  /  " + LAYOUT_NAMES[activeLayout].toUpperCase(Locale.US);
        int kickerColor = 0xFFFF4A55;
        String badgesText = "TURBO ENGINE   |   LOW LATENCY   |   FLOATING TOOLS";
        int badgesBgColor = 0xFF202025;
        int badgesStrokeColor = 0xFF393940;
        int badgesTextColor = 0xFFD8D8DC;
        String playText = "START GAME";
        int[] playBgColors = new int[]{0xFFCC101F, 0xFFFF3345};
        int playStrokeColor = 0xFFFF6E78;

        switch (activeLayout) {
            case 0: // REDMAGIC Game Space Style (DEFAULT - SLEEK OBSIDIAN TITANIUM W/ CRIMSON NEON)
                cardBgColors = new int[]{0xFF101217, 0xFF08090D, 0xFF14161F};
                cardStrokeColor = 0x66FF1744;
                cardStrokeWidthDp = 1.2f;
                railBgColors = new int[]{0xFFFF1744, 0x22FF1744};
                railWidthDp = 3;
                iconBgColor = 0xFF161822;
                iconStrokeColor = 0x55FF1744;
                iconCornerDp = isCompact ? 18 : 22;
                kickerColor = 0xFFFF3355;
                badgesText = "TITAN DASHBOARD   |   TURBO FAN   |   ZERO LATENCY";
                badgesBgColor = 0xCC181A24;
                badgesStrokeColor = 0x44FF1744;
                badgesTextColor = 0xFFE2E4EC;
                playText = "LAUNCH TITAN";
                playBgColors = new int[]{0xFFFF1744, 0xFFD50000};
                playStrokeColor = 0xFFFF5252;
                break;
            case 1: // REDMAGIC Console Style
                cardBgColors = new int[]{0xFF101218, 0xFF08090E, 0xFF161822};
                cardStrokeColor = 0x66FF2A3A;
                cardStrokeWidthDp = 1.2f;
                railBgColors = new int[]{0xFFFF2A3A, 0x22FF2A3A};
                railWidthDp = 3;
                iconBgColor = 0xFF161822;
                iconStrokeColor = 0x55FF2A3A;
                iconCornerDp = isCompact  ?  16 : 18;
                kickerColor = 0xFFFF3B47;
                badgesText = "CONSOLE STAGE   |   RAW INPUT   |   120 FPS TARGET";
                badgesBgColor = 0xCC181A24;
                badgesStrokeColor = 0x44FF3B47;
                badgesTextColor = 0xFFE2E4EC;
                playText = "CONSOLE LAUNCH";
                playBgColors = new int[]{0xFFC40818, 0xFFFF3B47};
                playStrokeColor = 0xFFFF6E78;
                break;
            case 2: // ASUS ROG Armoury Crate Style
                cardBgColors = new int[]{0xFF111218, 0xFF08090D, 0xFF18151F};
                cardStrokeColor = 0x66FF1744;
                cardStrokeWidthDp = 1.4f;
                railBgColors = new int[]{0xFFFF1744, 0x22FF9100};
                railWidthDp = 4;
                iconBgColor = 0xFF161822;
                iconStrokeColor = 0x55FF1744;
                iconCornerDp = isCompact  ?  14 : 16;
                kickerColor = 0xFFFF5252;
                badgesText = "ARMOURY MATRIX   |   OVERDRIVE BOOST   |   NEON MATRIX";
                badgesBgColor = 0xCC181A24;
                badgesStrokeColor = 0x44FF1744;
                badgesTextColor = 0xFFE2E4EC;
                playText = "ENGAGE MATRIX";
                playBgColors = new int[]{0xFFD50000, 0xFFFF6D00};
                playStrokeColor = 0xFFFF5252;
                break;
            case 3: // Lenovo Legion Space Style
                cardBgColors = new int[]{0xFF051724, 0xFF030D16, 0xFF0A2234};
                cardStrokeColor = 0xFF00E5FF;
                cardStrokeWidthDp = 1.8f;
                railBgColors = new int[]{0xFF00E5FF, 0xFF2979FF};
                railWidthDp = 5;
                iconBgColor = 0xFF061A28;
                iconStrokeColor = 0xFF00E5FF;
                iconCornerDp = isCompact  ?  22 : 28;
                kickerColor = 0xFF00E5FF;
                badgesText = "VORTEX REACTOR   |   CRYO CHILL 5.0   |   PURE POWER";
                badgesBgColor = 0xFF082236;
                badgesStrokeColor = 0xFF00E5FF;
                badgesTextColor = 0xFF80D8FF;
                playText = "VORTEX LAUNCH";
                playBgColors = new int[]{0xFF0091EA, 0xFF00B0FF};
                playStrokeColor = 0xFF80D8FF;
                break;
            case 4: // Black Shark Space Style
                cardBgColors = new int[]{0xFF09160E, 0xFF040B07, 0xFF0E2215};
                cardStrokeColor = 0xFF00E676;
                cardStrokeWidthDp = 1.8f;
                railBgColors = new int[]{0xFF00E676, 0xFF76FF03};
                railWidthDp = 5;
                iconBgColor = 0xFF0B1C12;
                iconStrokeColor = 0xFF00E676;
                iconCornerDp = isCompact  ?  12 : 14;
                kickerColor = 0xFF00E676;
                badgesText = "APEX HORIZON   |   OVERDRIVE MODE   |   TOUCH OVERCLOCK";
                badgesBgColor = 0xFF0D2517;
                badgesStrokeColor = 0xFF00E676;
                badgesTextColor = 0xFFB9F6CA;
                playText = "APEX DIVE";
                playBgColors = new int[]{0xFF00C853, 0xFF64DD17};
                playStrokeColor = 0xFF69F0AE;
                break;
            case 5: // Xiaomi Game Turbo Style
                cardBgColors = new int[]{0xFF1E1005, 0xFF0F0802, 0xFF2C1508};
                cardStrokeColor = 0xFFFF6D00;
                cardStrokeWidthDp = 1.6f;
                railBgColors = new int[]{0xFFFF6D00, 0xFFFFAB00};
                railWidthDp = 5;
                iconBgColor = 0xFF221105;
                iconStrokeColor = 0xFFFF6D00;
                iconCornerDp = isCompact  ?  16 : 18;
                kickerColor = 0xFFFF9100;
                badgesText = "TURBO COCKPIT   |   PERFORMANCE BOOST   |   NETWORK ACCEL";
                badgesBgColor = 0xFF2B1405;
                badgesStrokeColor = 0xFFFF6D00;
                badgesTextColor = 0xFFFFD180;
                playText = "START TURBO";
                playBgColors = new int[]{0xFFFF6D00, 0xFFFF3D00};
                playStrokeColor = 0xFFFFAB40;
                break;
            case 6: // OPPO Game Space Style
                cardBgColors = new int[]{0xFF061917, 0xFF030D0C, 0xFF0A2522};
                cardStrokeColor = 0xFF00BFA5;
                cardStrokeWidthDp = 1.5f;
                railBgColors = new int[]{0xFF00BFA5, 0xFF1DE9B6};
                railWidthDp = 4;
                iconBgColor = 0xFF081E1C;
                iconStrokeColor = 0xFF00BFA5;
                iconCornerDp = isCompact  ?  18 : 22;
                kickerColor = 0xFF1DE9B6;
                badgesText = "HYPERBOOST ENGINE   |   TOUCH ENHANCE   |   SMOOTH FRAME";
                badgesBgColor = 0xFF0A2925;
                badgesStrokeColor = 0xFF00BFA5;
                badgesTextColor = 0xFFA7FFEB;
                playText = "GAME FOCUS";
                playBgColors = new int[]{0xFF00BFA5, 0xFF00897B};
                playStrokeColor = 0xFF64FFDA;
                break;
            case 7: // OnePlus Games Style
                cardBgColors = new int[]{0xFF180A0E, 0xFF0D0507, 0xFF240E14};
                cardStrokeColor = 0xFFEB0028;
                cardStrokeWidthDp = 1.5f;
                railBgColors = new int[]{0xFFEB0028, 0xFFFF5252};
                railWidthDp = 4;
                iconBgColor = 0xFF1B0B10;
                iconStrokeColor = 0xFFEB0028;
                iconCornerDp = isCompact  ?  14 : 16;
                kickerColor = 0xFFFF5252;
                badgesText = "PRO GAMING   |   PERFORMANCE MODE   |   4D HAPTIC";
                badgesBgColor = 0xFF280D15;
                badgesStrokeColor = 0xFFEB0028;
                badgesTextColor = 0xFFFF8A80;
                playText = "PRO PLAY";
                playBgColors = new int[]{0xFFEB0028, 0xFFB71C1C};
                playStrokeColor = 0xFFFF5252;
                break;
            case 8: // Samsung Gaming Hub Style
                cardBgColors = new int[]{0xFF130B22, 0xFF0A0513, 0xFF1E1136};
                cardStrokeColor = 0xFF7C4DFF;
                cardStrokeWidthDp = 1.6f;
                railBgColors = new int[]{0xFF7C4DFF, 0xFFB388FF};
                railWidthDp = 5;
                iconBgColor = 0xFF160D28;
                iconStrokeColor = 0xFF7C4DFF;
                iconCornerDp = isCompact  ?  20 : 24;
                kickerColor = 0xFFB388FF;
                badgesText = "GAMING HUB   |   GAME BOOSTER PLUS   |   DOLBY ATMOS";
                badgesBgColor = 0xFF22133F;
                badgesStrokeColor = 0xFF7C4DFF;
                badgesTextColor = 0xFFD1C4E9;
                playText = "QUICK LAUNCH";
                playBgColors = new int[]{0xFF651FFF, 0xFF6200EA};
                playStrokeColor = 0xFFB388FF;
                break;
            case 9: // Realme Game Space Style
                cardBgColors = new int[]{0xFF1C1905, 0xFF0E0D02, 0xFF2B2508};
                cardStrokeColor = 0xFFFFD600;
                cardStrokeWidthDp = 1.6f;
                railBgColors = new int[]{0xFFFFD600, 0xFFFFAB00};
                railWidthDp = 5;
                iconBgColor = 0xFF201B05;
                iconStrokeColor = 0xFFFFD600;
                iconCornerDp = isCompact  ?  16 : 18;
                kickerColor = 0xFFFFEA00;
                badgesText = "GT MODE 3.0   |   PEAK FREQUENCY   |   ULTRA TOUCH";
                badgesBgColor = 0xFF2B2406;
                badgesStrokeColor = 0xFFFFD600;
                badgesTextColor = 0xFFFFF59D;
                playText = "GT START";
                playBgColors = new int[]{0xFFFFAB00, 0xFFFF6D00};
                playStrokeColor = 0xFFFFEA00;
                break;
            case 10: // REDMAGIC Dual Grid
            case 11: // REDMAGIC Tile Grid
                cardBgColors = new int[]{0xFF101218, 0xFF08090E, 0xFF161822};
                cardStrokeColor = 0x66FF1744;
                cardStrokeWidthDp = 1.2f;
                railBgColors = new int[]{0xFFFF1744, 0x22FF1744};
                railWidthDp = 3;
                iconBgColor = 0xFF161822;
                iconStrokeColor = 0x55FF1744;
                iconCornerDp = isCompact  ?  14 : 16;
                kickerColor = 0xFFFF5252;
                badgesText = "DUAL TILE GRID   |   TOUCH STABILIZER   |   ICE 12.0";
                badgesBgColor = 0xCC181A24;
                badgesStrokeColor = 0x44FF1744;
                badgesTextColor = 0xFFE2E4EC;
                playText = "GRID LAUNCH";
                playBgColors = new int[]{0xFFFF1744, 0xFFD50000};
                playStrokeColor = 0xFFFF5252;
                break;
            case 12: // Beacon Launcher Style
                cardBgColors = new int[]{0xFF0E131C, 0xFF070A0F, 0xFF141C2B};
                cardStrokeColor = 0xFF448AFF;
                cardStrokeWidthDp = 1.4f;
                railBgColors = new int[]{0xFF448AFF, 0xFF2979FF};
                railWidthDp = 4;
                iconBgColor = 0xFF101724;
                iconStrokeColor = 0xFF448AFF;
                iconCornerDp = isCompact  ?  12 : 14;
                kickerColor = 0xFF82B1FF;
                badgesText = "BECON ARTWORK   |   MINIMAL FOOTPRINT   |   FAST INDEX";
                badgesBgColor = 0xFF152033;
                badgesStrokeColor = 0xFF448AFF;
                badgesTextColor = 0xFFBBDEFB;
                playText = "ENTER GAME";
                playBgColors = new int[]{0xFF2979FF, 0xFF1565C0};
                playStrokeColor = 0xFF82B1FF;
                break;
            case 13: // Daijisho Style
            case 14: // EmulationStation Style
                cardBgColors = new int[]{0xFF1C1309, 0xFF0E0A04, 0xFF2A1B0B};
                cardStrokeColor = 0xFFFF9800;
                cardStrokeWidthDp = 1.8f;
                railBgColors = new int[]{0xFFFF9800, 0xFFF57C00};
                railWidthDp = 5;
                iconBgColor = 0xFF201509;
                iconStrokeColor = 0xFFFF9800;
                iconCornerDp = isCompact  ?  18 : 22;
                kickerColor = 0xFFFFB74D;
                badgesText = "CONSOLE SCRAPER   |   RETRO CORES   |   LOW LATENCY";
                badgesBgColor = 0xFF2C1D0D;
                badgesStrokeColor = 0xFFFF9800;
                badgesTextColor = 0xFFFFE0B2;
                playText = "BOOT GAME";
                playBgColors = new int[]{0xFFF57C00, 0xFFE65100};
                playStrokeColor = 0xFFFFB74D;
                break;
            case 15: // LaunchBox Style
            case 22: // Pegasus BigScreenFE Style
            case 23: // Pegasus Flat-Ozone Style
                cardBgColors = new int[]{0xFF08141F, 0xFF040A10, 0xFF0D2032};
                cardStrokeColor = 0xFF00B0FF;
                cardStrokeWidthDp = 1.6f;
                railBgColors = new int[]{0xFF00B0FF, 0xFF00838F};
                railWidthDp = 5;
                iconBgColor = 0xFF0A1926;
                iconStrokeColor = 0xFF00B0FF;
                iconCornerDp = isCompact  ?  16 : 20;
                kickerColor = 0xFF40C4FF;
                badgesText = "CINEMATIC FRONTEND   |   METADATA HUD   |   HERO THEATER";
                badgesBgColor = 0xFF0E2436;
                badgesStrokeColor = 0xFF00B0FF;
                badgesTextColor = 0xFFB3E5FC;
                playText = "PLAY TITLE";
                playBgColors = new int[]{0xFF0091EA, 0xFF006064};
                playStrokeColor = 0xFF40C4FF;
                break;
            case 20: // Lenovo Legion Circular Layout
            case 21: // Black Shark Lobby Layout
                cardBgColors = new int[]{0xFF120B20, 0xFF080410, 0xFF1D0F32};
                cardStrokeColor = 0xFFE040FB;
                cardStrokeWidthDp = 2.0f;
                railBgColors = new int[]{0xFFE040FB, 0xFF7C4DFF};
                railWidthDp = 5;
                iconBgColor = 0xFF170E28;
                iconStrokeColor = 0xFFE040FB;
                iconCornerDp = isCompact  ?  24 : 32;
                kickerColor = 0xFFEA80FC;
                badgesText = "RADIAL DIAL   |   CIRCULAR MATRIX   |   UNLOCKED CORE";
                badgesBgColor = 0xFF24143D;
                badgesStrokeColor = 0xFFE040FB;
                badgesTextColor = 0xFFF3E5F5;
                playText = "ENGAGE RADIAL";
                playBgColors = new int[]{0xFFD500F9, 0xFF651FFF};
                playStrokeColor = 0xFFEA80FC;
                break;
            case 33: // ROR Hexagon Grid
                cardBgColors = new int[]{0xFF1C1405, 0xFF0D0A02, 0xFF2C1F08};
                cardStrokeColor = 0xFFFFD700;
                cardStrokeWidthDp = 1.8f;
                railBgColors = new int[]{0xFFFFD700, 0xFFFF6D00};
                railWidthDp = 5;
                iconBgColor = 0xFF1F1706;
                iconStrokeColor = 0xFFFFD700;
                iconCornerDp = isCompact  ?  10 : 12;
                kickerColor = 0xFFFFE57F;
                badgesText = "HEXAGONAL MATRIX   |   FUTURISTIC GEOMETRY   |   OVERCLOCK";
                badgesBgColor = 0xFF2B2008;
                badgesStrokeColor = 0xFFFFD700;
                badgesTextColor = 0xFFFFF9C4;
                playText = "HEX LAUNCH";
                playBgColors = new int[]{0xFFFFAB00, 0xFFFF3D00};
                playStrokeColor = 0xFFFFE57F;
                break;
            default: {
                String cat = LAYOUT_CATEGORIES[Math.floorMod(activeLayout, LAYOUT_CATEGORIES.length)];
                if ("COVER GRIDS".equals(cat)) {
                    cardBgColors = new int[]{0xFF160E18, 0xFF0A060C, 0xFF221425};
                    cardStrokeColor = 0xFFFF4081;
                    railBgColors = new int[]{0xFFFF4081, 0xFFE91E63};
                    badgesText = "COVER GALLERY   |   ADAPTIVE VRAM   |   HIGH FIDELITY";
                    badgesBgColor = 0xFF271329;
                    badgesStrokeColor = 0xFFFF4081;
                    badgesTextColor = 0xFFF8BBD0;
                    playText = "PLAY NOW";
                    playBgColors = new int[]{0xFFE91E63, 0xFFC2185B};
                    playStrokeColor = 0xFFFF80AB;
                } else if ("COMPCT GRIDS".equals(cat)) {
                    cardBgColors = new int[]{0xFF0E1A1A, 0xFF070E0E, 0xFF152626};
                    cardStrokeColor = 0xFF1DE9B6;
                    railBgColors = new int[]{0xFF1DE9B6, 0xFF00BFA5};
                    badgesText = "ULTRA DENSE GRID   |   INSTANT CCESS   |   ZERO OVERHEAD";
                    badgesBgColor = 0xFF132A2A;
                    badgesStrokeColor = 0xFF1DE9B6;
                    badgesTextColor = 0xFFA7FFEB;
                    playText = "QUICK START";
                    playBgColors = new int[]{0xFF00BFA5, 0xFF004D40};
                    playStrokeColor = 0xFF64FFDA;
                } else if ("CINEMATIC".equals(cat)) {
                    cardBgColors = new int[]{0xFF0B1220, 0xFF050912, 0xFF111E34};
                    cardStrokeColor = 0xFF00E5FF;
                    railBgColors = new int[]{0xFF00E5FF, 0xFF3D5AFE};
                    badgesText = "CINEMATIC HERO   |   DYNAMIC SHOWCASE   |   PRO PIPELINE";
                    badgesBgColor = 0xFF0F213C;
                    badgesStrokeColor = 0xFF00E5FF;
                    badgesTextColor = 0xFF80D8FF;
                    playText = "WARP PLAY";
                    playBgColors = new int[]{0xFF2979FF, 0xFF1A237E};
                    playStrokeColor = 0xFF82B1FF;
                } else if ("EXPERIMENTAL".equals(cat)) {
                    cardBgColors = new int[]{0xFF1D0E1F, 0xFF0F0611, 0xFF2B142F};
                    cardStrokeColor = 0xFFFF007F;
                    railBgColors = new int[]{0xFFFF007F, 0xFF9C27B0};
                    badgesText = "EXPERIMENTAL LAB   |   QUANTUM THREADS   |   CUSTOM PROFILE";
                    badgesBgColor = 0xFF2A112C;
                    badgesStrokeColor = 0xFFFF007F;
                    badgesTextColor = 0xFFF48FB1;
                    playText = "IGNITE RUN";
                    playBgColors = new int[]{0xFFFF007F, 0xFF880E4F};
                    playStrokeColor = 0xFFFF4081;
                } else {
                    cardBgColors = new int[]{0xFF18181C, 0xFF0B0B0E, 0xFF24080C};
                    cardStrokeColor = 0xFF3A3A40;
                    railBgColors = new int[]{0xFFFF3849, 0xFF8C0A16};
                }
                break;
            }
        }

        FrameLayout frame = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, cardBgColors);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(cardStrokeWidthDp), cardStrokeColor);
        frame.setBackground(bg);

        View redRail = new View(this);
        GradientDrawable railBg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, railBgColors);
        railBg.setCornerRadius(dp(3));
        redRail.setBackground(railBg);
        FrameLayout.LayoutParams redRailLp = new FrameLayout.LayoutParams(dp(railWidthDp), dp(isCompact  ?  80 : 128), Gravity.START | Gravity.CENTER_VERTICAL);
        redRailLp.setMarginStart(dp(isCompact  ?  10 : 16));
        frame.addView(redRail, redRailLp);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(isCompact  ?  20 : 36), dp(isCompact  ?  10 : 20), dp(isCompact  ?  20 : 36), dp(isCompact  ?  10 : 20));

        TextView kicker = label(kickerText, isCompact  ?  7.5f : 8.5f, kickerColor);
        kicker.setTypeface(null, Typeface.BOLD);
        kicker.setLetterSpacing(.14f);
        content.addView(kicker);

        FrameLayout iconFrame = new FrameLayout(this);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setColor(iconBgColor);
        iconBg.setCornerRadius(dp(iconCornerDp));
        iconBg.setStroke(dp(2), iconStrokeColor);
        iconFrame.setBackground(iconBg);
        heroIcon = new ImageView(this);
        heroIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        heroIcon.setImageResource(R.drawable.ror_logo);
        int heroIconArtwork = dp(isCompact  ?  60 : 80);
        iconFrame.addView(heroIcon, new FrameLayout.LayoutParams(heroIconArtwork, heroIconArtwork, Gravity.CENTER));
        int heroShellSize = dp(isCompact  ?  76 : 104);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(heroShellSize, heroShellSize);
        iconLp.setMargins(0, dp(isCompact  ?  6 : 14), 0, dp(isCompact  ?  4 : 10));
        content.addView(iconFrame, iconLp);

        final float[] touchDownX = new float[1];
        final float[] touchDownY = new float[1];
        final long[] touchDownTime = new long[1];
        View.OnTouchListener heroSwipeAndTapListener = (v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    touchDownX[0] = event.getRawX();
                    touchDownY[0] = event.getRawY();
                    touchDownTime[0] = System.currentTimeMillis();
                    if (v.getParent() != null) {
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    float dx = event.getRawX() - touchDownX[0];
                    float dy = event.getRawY() - touchDownY[0];
                    long dt = System.currentTimeMillis() - touchDownTime[0];
                    if (Math.abs(dx) > dp(25) && Math.abs(dx) > Math.abs(dy) * 1.05f) {
                        if (dx < 0) {
                            cycleHeroGame(1, content); // Slide left -> Next game
                        } else {
                            cycleHeroGame(-1, content); // Slide right -> Prev game
                        }
                        return true;
                    } else if (Math.abs(dx) < dp(20) && Math.abs(dy) < dp(20) && dt < 500) {
                        try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
                        v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(70).withEndAction(() -> {
                            v.animate().scaleX(1.06f).scaleY(1.06f).setDuration(110).withEndAction(() -> {
                                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(70).start();
                            }).start();
                        }).start();
                        if (selectedEntry != null) {
                            prepareLaunch(selectedEntry);
                        } else if (!displayedEntries.isEmpty()) {
                            prepareLaunch(displayedEntries.get(0));
                        } else {
                            showAddGamesDialog();
                        }
                        return true;
                    }
                    break;
            }
            return false;
        };

        iconFrame.setClickable(true);
        iconFrame.setFocusable(true);
        iconFrame.setOnTouchListener(heroSwipeAndTapListener);
        heroIcon.setClickable(true);
        heroIcon.setOnTouchListener(heroSwipeAndTapListener);
        content.setOnTouchListener(heroSwipeAndTapListener);

        heroTitle = label("SELECT A GAME", isCompact  ?  18f : 24f, Color.WHITE);
        heroTitle.setTypeface(null, Typeface.BOLD);
        heroTitle.setGravity(Gravity.CENTER);
        heroTitle.setMaxLines(1);
        heroTitle.setEllipsize(TextUtils.TruncateAt.END);
        content.addView(heroTitle);

        heroSubtitle = label("Choose a title from your Game Lobby", isCompact  ?  8.5f : 10f, 0xFF9A9AA3);
        heroSubtitle.setGravity(Gravity.CENTER);
        heroSubtitle.setPadding(0, dp(2), 0, dp(isCompact  ?  4 : 10));
        content.addView(heroSubtitle);

        heroBadges = label(badgesText, isCompact  ?  7.5f : 9f, badgesTextColor);
        heroBadges.setGravity(Gravity.CENTER);
        heroBadges.setPadding(dp(isCompact  ?  8 : 12), dp(isCompact  ?  4 : 7), dp(isCompact  ?  8 : 12), dp(isCompact  ?  4 : 7));
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(badgesBgColor);
        badgeBg.setCornerRadius(dp(8));
        badgeBg.setStroke(dp(1), badgesStrokeColor);
        heroBadges.setBackground(badgeBg);
        content.addView(heroBadges);

        heroPlayButton = new Button(this);
        heroPlayButton.setText(playText);
        heroPlayButton.setTextColor(Color.WHITE);
        heroPlayButton.setTextSize(isCompact  ?  11f : 13f);
        heroPlayButton.setTypeface(null, Typeface.BOLD);
        heroPlayButton.setAllCaps(false);
        GradientDrawable playBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, playBgColors);
        playBg.setCornerRadius(dp(10));
        playBg.setStroke(dp(1), playStrokeColor);
        heroPlayButton.setBackground(playBg);
        heroPlayButton.setOnClickListener(v -> {
            if (selectedEntry != null) prepareLaunch(selectedEntry);
            else if (!displayedEntries.isEmpty()) prepareLaunch(displayedEntries.get(0));
        });
        LinearLayout.LayoutParams playLp = new LinearLayout.LayoutParams(dp(isCompact  ?  190 : 230), dp(isCompact  ?  36 : 46));
        playLp.topMargin = dp(isCompact  ?  8 : 14);
        content.addView(heroPlayButton, playLp);

        statusText = label("READY TO LAUNCH", isCompact  ?  7.5f : 8.5f, 0xFF8B8B94);
        statusText.setGravity(Gravity.CENTER);
        statusText.setLetterSpacing(.10f);
        statusText.setPadding(0, dp(isCompact  ?  3 : 5), 0, 0);
        content.addView(statusText);
        updateActiveBannerTextStyle();
        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        content.addView(progressBar, new LinearLayout.LayoutParams(dp(16), dp(16)));

        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        frame.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        frame.setOnTouchListener(heroSwipeAndTapListener);
        scroll.setOnTouchListener(heroSwipeAndTapListener);

        // Stylish Left Arrow Button
        FrameLayout prevArrowFrame = new FrameLayout(this);
        GradientDrawable prevArrowBg = new GradientDrawable();
        prevArrowBg.setShape(GradientDrawable.OVAL);
        prevArrowBg.setColor(0x33000000);
        prevArrowBg.setStroke(dp(1.2f), 0x55FFFFFF);
        prevArrowFrame.setBackground(prevArrowBg);
        ImageView prevArrowIcon = new ImageView(this);
        prevArrowIcon.setImageResource(R.drawable.ic_chevron_left);
        prevArrowIcon.setColorFilter(0xEEFFFFFF);
        int arrowIconSize = dp(isCompact  ?  16 : 20);
        prevArrowFrame.addView(prevArrowIcon, new FrameLayout.LayoutParams(arrowIconSize, arrowIconSize, Gravity.CENTER));
        int arrowBtnSize = dp(isCompact  ?  30 : 38);
        FrameLayout.LayoutParams pLp = new FrameLayout.LayoutParams(arrowBtnSize, arrowBtnSize, Gravity.START | Gravity.CENTER_VERTICAL);
        pLp.setMarginStart(dp(6));
        prevArrowFrame.setClickable(true);
        prevArrowFrame.setFocusable(true);
        prevArrowFrame.setOnClickListener(v -> {
            try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
            v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(60).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()).start();
            cycleHeroGame(-1, content);
        });
        frame.addView(prevArrowFrame, pLp);

        // Stylish Right Arrow Button
        FrameLayout nextArrowFrame = new FrameLayout(this);
        GradientDrawable nextArrowBg = new GradientDrawable();
        nextArrowBg.setShape(GradientDrawable.OVAL);
        nextArrowBg.setColor(0x33000000);
        nextArrowBg.setStroke(dp(1.2f), 0x55FFFFFF);
        nextArrowFrame.setBackground(nextArrowBg);
        ImageView nextArrowIcon = new ImageView(this);
        nextArrowIcon.setImageResource(R.drawable.ic_chevron_right);
        nextArrowIcon.setColorFilter(0xEEFFFFFF);
        nextArrowFrame.addView(nextArrowIcon, new FrameLayout.LayoutParams(arrowIconSize, arrowIconSize, Gravity.CENTER));
        FrameLayout.LayoutParams nLp = new FrameLayout.LayoutParams(arrowBtnSize, arrowBtnSize, Gravity.END | Gravity.CENTER_VERTICAL);
        nLp.setMarginEnd(dp(6));
        nextArrowFrame.setClickable(true);
        nextArrowFrame.setFocusable(true);
        nextArrowFrame.setOnClickListener(v -> {
            try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
            v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(60).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()).start();
            cycleHeroGame(1, content);
        });
        frame.addView(nextArrowFrame, nLp);

        return frame;
    }

    private void cycleHeroGame(int direction, View content) {
        List<GameEntry> list = displayedEntries;
        if (list == null || list.isEmpty()) return;
        int currentIdx = -1;
        for (int i = 0; i < list.size(); i++) {
            if (selectedEntry != null && selectedEntry.pkg.equals(list.get(i).pkg)) {
                currentIdx = i;
                break;
            }
        }
        int nextIdx = (currentIdx + direction + list.size()) % list.size();
        GameEntry nextEntry = list.get(nextIdx);

        float outX = direction > 0  ?  -dp(140) : dp(140);
        content.animate()
                .translationX(outX)
                .alpha(0.0f)
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(150)
                .withEndAction(() -> {
                    selectGame(nextEntry);
                    content.setTranslationX(-outX);
                    content.setScaleX(0.92f);
                    content.setScaleY(0.92f);
                    content.animate()
                            .translationX(0f)
                            .alpha(1.0f)
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(220)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                }).start();
    }

    private View buildEnergyCorePanel() {
        return buildEnergyCorePanel(Math.floorMod(layoutIndex, LAYOUT_NAMES.length), false);
    }

    private View buildEnergyCorePanel(int layoutStyle, boolean isPortrait) {
        energyModeButtons.clear();
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float heightDp = dm.heightPixels / dm.density;
        boolean isCompact = !isPortrait && heightDp < 640;

        int accentColor = 0xFFFF1744;
        int bgStart = 0xF2160C14;
        int bgEnd = 0xFA0B070D;
        int strokeColor = 0x555C1827;
        int cardBg1 = 0xE6160F16;
        int cardBg2 = 0xF50D080D;
        int cardStroke = 0x444A1B28;
        String coreTitle = "ENERGY CORE";
        String coreSubtitle = "LIVE PERFORMANCE CONTROL";
        int cornerRadiusDp = 16;

        switch (layoutStyle) {
            case 1: // Mecha Deck (Electric Cyan)
                accentColor = 0xFF00E5FF;
                bgStart = 0xF2081524; bgEnd = 0xFA040B14; strokeColor = 0x5500E5FF;
                cardBg1 = 0xE60A1A2C; cardBg2 = 0xF506101C; cardStroke = 0x4400E5FF;
                coreTitle = "TITAN REACTOR"; coreSubtitle = "MECHA POWER MATRIX";
                cornerRadiusDp = 12;
                break;
            case 2: // Armoury Matrix (Neon Orange/Red)
                accentColor = 0xFFFF3D00;
                bgStart = 0xF214080D; bgEnd = 0xFA0A0407; strokeColor = 0x66FF1744;
                cardBg1 = 0xE61E0A12; cardBg2 = 0xF510050A; cardStroke = 0x55FF1744;
                coreTitle = "OVERDRIVE CORE"; coreSubtitle = "MATRIX PERFORMANCE";
                cornerRadiusDp = 10;
                break;
            case 3:
            case 20: // Vortex Reactor (Cryo Ice Blue)
                accentColor = 0xFF00E5FF;
                bgStart = 0xF2041220; bgEnd = 0xFA020912; strokeColor = 0x5500B0FF;
                cardBg1 = 0xE606182A; cardBg2 = 0xF5030E1A; cardStroke = 0x4400B0FF;
                coreTitle = "VORTEX CORE"; coreSubtitle = "CRYO CHILL 5.0";
                cornerRadiusDp = 16;
                break;
            case 4:
            case 21: // Apex Horizon (Radioactive Green / Speed Gold)
                accentColor = 0xFF00E676;
                bgStart = 0xF206160C; bgEnd = 0xFA030D06; strokeColor = 0x5500E676;
                cardBg1 = 0xE6092012; cardBg2 = 0xF505140A; cardStroke = 0x4400E676;
                coreTitle = "APEX POWER"; coreSubtitle = "OVERDRIVE DOCK";
                cornerRadiusDp = 14;
                break;
            case 5: // Turbo Cockpit (Avionics Amber)
                accentColor = 0xFFFF9100;
                bgStart = 0xF2160C03; bgEnd = 0xFA0D0701; strokeColor = 0x66FF6D00;
                cardBg1 = 0xE6201004; cardBg2 = 0xF5140A02; cardStroke = 0x55FF6D00;
                coreTitle = "TURBO COCKPIT"; coreSubtitle = "AFTERBURNER CORE";
                cornerRadiusDp = 12;
                break;
            case 8:
            case 14:
            case 15:
            case 22: // Console Stage Family
                accentColor = 0xFF7C4DFF;
                bgStart = 0xF2120C22; bgEnd = 0xFA080512; strokeColor = 0x557C4DFF;
                cardBg1 = 0xE6180F2E; cardBg2 = 0xF50F091C; cardStroke = 0x447C4DFF;
                coreTitle = "STATION CORE"; coreSubtitle = "POWER DOCK";
                cornerRadiusDp = 14;
                break;
            case 10:
            case 23:
            case 29:
            case 30:
            case 31: // Split & Hero Family (Cyber Neon Violet)
                accentColor = 0xFFE040FB;
                bgStart = 0xF21C0A28; bgEnd = 0xFA0E0414; strokeColor = 0x66E040FB;
                cardBg1 = 0xE6260D34; cardBg2 = 0xF516071E; cardStroke = 0x55E040FB;
                coreTitle = "HERO ENERGY CORE"; coreSubtitle = "CYBERPUNK ENGINE";
                cornerRadiusDp = 14;
                break;
            case 11:
            case 12:
            case 13:
            case 16:
            case 17:
            case 18:
            case 19:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 32: // Grid Catalog Family (Matrix Cyan / Emerald)
                accentColor = 0xFF00E5FF;
                bgStart = 0xF2061A22; bgEnd = 0xFA030E14; strokeColor = 0x5500E5FF;
                cardBg1 = 0xE6082430; cardBg2 = 0xF504141C; cardStroke = 0x4400E5FF;
                coreTitle = "GRID ENERGY CORE"; coreSubtitle = "CATALOG ACCELERATOR";
                cornerRadiusDp = 12;
                break;
            case 33: // Hexagon Grid Family (Honeycomb Amber Gold)
                accentColor = 0xFFFFD600;
                bgStart = 0xF2201804; bgEnd = 0xFA120E02; strokeColor = 0x66FFD600;
                cardBg1 = 0xE62A2006; cardBg2 = 0xF5181203; cardStroke = 0x55FFD600;
                coreTitle = "HEXAGON CORE"; coreSubtitle = "HONEYCOMB REACTOR";
                cornerRadiusDp = 16;
                break;
            default:
                if (layoutStyle != 0) {
                    accentColor = 0xFF2979FF;
                    bgStart = 0xF2081422; bgEnd = 0xFA040B14; strokeColor = 0x552979FF;
                    cardBg1 = 0xE60B1C30; cardBg2 = 0xF506101C; cardStroke = 0x442979FF;
                    coreTitle = "STREAM ENERGY CORE"; coreSubtitle = "SYSTEM OPTIMIZER";
                    cornerRadiusDp = 14;
                }
                break;
        }

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(isCompact ? 10 : 12), dp(isCompact ? 8 : 10), dp(isCompact ? 10 : 12), dp(isCompact ? 6 : 8));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{bgStart, bgEnd});
        bg.setCornerRadius(dp(cornerRadiusDp));
        bg.setStroke(dp(1.2f), strokeColor);
        card.setBackground(bg);

        TextView title = label(coreTitle, isCompact ? 11f : 12f, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setLetterSpacing(.11f);
        card.addView(title);
        TextView subtitle = label(coreSubtitle, isCompact ? 7.5f : 8f, accentColor);
        subtitle.setLetterSpacing(.12f);
        subtitle.setPadding(0, dp(1), 0, dp(isCompact ? 4 : 8));
        card.addView(subtitle);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(0, 0, 0, dp(4));

        boolean phoenix = GsmProfileManager.isPhoenixSelected(this);
        LinearLayout modesRow1 = new LinearLayout(this);
        modesRow1.setOrientation(LinearLayout.HORIZONTAL);
        modesRow1.setBaselineAligned(false);

        LinearLayout modesRow2 = new LinearLayout(this);
        modesRow2.setOrientation(LinearLayout.HORIZONTAL);
        modesRow2.setBaselineAligned(false);
        modesRow2.setPadding(0, dp(isCompact ? 3 : 5), 0, dp(isCompact ? 3 : 4));

        TextView seeMoreBtn = label(showMoreProfiles ? "  LESS PROFILES" : "  SEE MORE PROFILES", isCompact ? 7.5f : 8f, accentColor);

        if (phoenix) {
            modesRow1.addView(redModeButton("BATTERY+", 0, isCompact));
            modesRow1.addView(redModeButton("BALANCE+", 1, isCompact));
            modesRow1.addView(redModeButton("THERMAL+", 2, isCompact));
            modesRow1.addView(redModeButton("COOLDOWN+", 3, isCompact));
            panel.addView(modesRow1);

            modesRow2.addView(redModeButton("SMOOTH+", 4, isCompact));
            modesRow2.addView(redModeButton("ULTRA+", 5, isCompact));
            modesRow2.addView(redModeButton("ESPORTS+", 6, isCompact));
            modesRow2.addView(redModeButton("APEX+", 7, isCompact));
            modesRow2.setVisibility(showMoreProfiles ? View.VISIBLE : View.GONE);
            panel.addView(modesRow2);
        } else {
            modesRow1.addView(redModeButton("DAILY USE", 0, isCompact));
            modesRow1.addView(redModeButton("BALANCE", 1, isCompact));
            modesRow1.addView(redModeButton("BATTERY", 2, isCompact));
            modesRow1.addView(redModeButton("GAMING", 3, isCompact));
            panel.addView(modesRow1);
            seeMoreBtn.setVisibility(View.GONE);
        }
        seeMoreBtn.setGravity(Gravity.CENTER);
        seeMoreBtn.setTypeface(null, Typeface.BOLD);
        seeMoreBtn.setPadding(dp(16), dp(6), dp(16), dp(6));
        GradientDrawable smBg = new GradientDrawable();
        smBg.setColor((0x22 << 24) | (accentColor & 0x00FFFFFF));
        smBg.setCornerRadius(dp(12));
        seeMoreBtn.setBackground(smBg);
        LinearLayout.LayoutParams smLp = new LinearLayout.LayoutParams(-1, -2);
        smLp.setMargins(0, dp(4), 0, dp(6));
        seeMoreBtn.setLayoutParams(smLp);
        seeMoreBtn.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            showMoreProfiles = !showMoreProfiles;
            seeMoreBtn.setText(showMoreProfiles ? "  LESS PROFILES" : "  SEE MORE PROFILES");
            modesRow2.setVisibility(showMoreProfiles ? View.VISIBLE : View.GONE);
            getSharedPreferences("ror_game_launcher", 0).edit().putBoolean("show_more_profiles", showMoreProfiles).apply();
        });
        panel.addView(seeMoreBtn);

        // Gaming Quick Toggles Row (Clean RAM, RGB, Shield, Block calls)
        LinearLayout quickToggles = new LinearLayout(this);
        quickToggles.setOrientation(LinearLayout.HORIZONTAL);
        quickToggles.setBaselineAligned(false);
        quickToggles.setPadding(0, dp(isCompact ? 2 : 4), 0, dp(isCompact ? 4 : 6));

        SharedPreferences plugins = getSharedPreferences("ror_game_plugins", MODE_PRIVATE);
        quickToggles.addView(buildRedMagicPill(R.drawable.ic_clear, "CLEAN", true, isCompact, v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            runQuickLauncherAction("cleanup");
            Toast.makeText(this, "Instant RAM Clean: Cache Freed & Memory Boosted!", Toast.LENGTH_SHORT).show();
        }, accentColor, cardBg1, cardBg2, cardStroke));
        quickToggles.addView(buildRedMagicPill(R.drawable.ic_rainbow_rgb, "RGB", plugins.getBoolean("rainbow_rgb", true), isCompact, v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            boolean next = !plugins.getBoolean("rainbow_rgb", true);
            plugins.edit().putBoolean("rainbow_rgb", next).apply();
            if (liveReactorCircle != null) {
                liveReactorCircle.setRgbChromaActive(next);
            }
            Toast.makeText(this, "RGB Chroma: " + (next ? "RGB AURA ON" : "OFF"), Toast.LENGTH_SHORT).show();
            rebuildView();
        }, accentColor, cardBg1, cardBg2, cardStroke));
        quickToggles.addView(buildRedMagicPill(R.drawable.ic_shield, "SHIELD", plugins.getBoolean("anti_disturb", false), isCompact, v -> {
            boolean next = !plugins.getBoolean("anti_disturb", false);
            plugins.edit().putBoolean("anti_disturb", next).apply();
            runLauncherCommand(next ? "settings put global heads_up_notifications_enabled 0; cmd notification set_dnd priority"
                    : "settings put global heads_up_notifications_enabled 1; cmd notification set_dnd off");
            Toast.makeText(this, "Shield Alerts " + (next ? "DND ARMED" : "OFF"), Toast.LENGTH_SHORT).show();
            rebuildView();
        }, accentColor, cardBg1, cardBg2, cardStroke));
        quickToggles.addView(buildRedMagicPill(R.drawable.ic_phone_block, "CALLS", plugins.getBoolean("block_calls", false), isCompact, v -> {
            boolean next = !plugins.getBoolean("block_calls", false);
            plugins.edit().putBoolean("block_calls", next).apply();
            runLauncherCommand(next ? "telecom set-call-screening-enabled true 2>/dev/null; cmd notification set_dnd priority"
                    : "cmd notification set_dnd off");
            Toast.makeText(this, "Block Calls " + (next ? "ENABLED" : "OFF"), Toast.LENGTH_SHORT).show();
            rebuildView();
        }, accentColor, cardBg1, cardBg2, cardStroke));
        panel.addView(quickToggles);

        panel.addView(redToolButton(R.drawable.ic_flash, "ONE-TOUCH OPTIMIZE", isCompact, () -> applyLauncherProfile(4), accentColor, cardBg1, cardBg2, cardStroke));
        panel.addView(redToolButton(R.drawable.ic_wifi, "NETWORK BOOST", isCompact, () -> runQuickLauncherAction("network"), accentColor, cardBg1, cardBg2, cardStroke));
        panel.addView(redToolButton(R.drawable.ic_turbo_fan, "CLEAR BACKGROUND APPS", isCompact, () -> runQuickLauncherAction("cleanup"), accentColor, cardBg1, cardBg2, cardStroke));
        floatingToolsButton = redToolButton(R.drawable.ic_floating_window,
                "FLOATING GAME TOOLS  |  " + (bubbleEnabled ? "ONLINE" : "OFF"),
                isCompact,
                this::toggleFloatingTools, accentColor, cardBg1, cardBg2, cardStroke);
        updateFloatingToolsButton();
        panel.addView(floatingToolsButton);

        TextView footer = label("CPU / GPU MONITORING  |  TOUCH  |  RECORD", isCompact ? 7f : 7.5f, 0xFF6F7488);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(isCompact ? 4 : 6), 0, dp(2));
        panel.addView(footer);

        if (isPortrait) {
            card.addView(panel);
        } else {
            ScrollView scroll = new ScrollView(this);
            scroll.setVerticalScrollBarEnabled(false);
            scroll.setScrollbarFadingEnabled(true);
            scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
            scroll.addView(panel, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            card.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
        }
        return card;
    }

        private View buildRedMagicPill(int iconRes, String labelText, boolean active, boolean isCompact, View.OnClickListener listener, int activeColor, int bgStart, int bgEnd, int stroke) {
        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.VERTICAL);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(dp(2), dp(isCompact ? 4 : 6), dp(2), dp(isCompact ? 4 : 6));
        pill.setClickable(true);
        pill.setFocusable(true);
        if (listener != null) pill.setOnClickListener(listener);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(active ? Color.WHITE : activeColor);
        pill.addView(icon, new LinearLayout.LayoutParams(dp(isCompact ? 18 : 20), dp(isCompact ? 18 : 20)));

        TextView tv = label(labelText, isCompact ? 7.5f : 8f, active ? Color.WHITE : 0xFFCCCCCC);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(2), 0, 0);
        pill.addView(tv);

        GradientDrawable bg = new GradientDrawable();
        if (active) {
            bg.setOrientation(GradientDrawable.Orientation.TL_BR);
            int halfAlpha = (0x88 << 24) | (activeColor & 0x00FFFFFF);
            int lowAlpha = (0x55 << 24) | (activeColor & 0x00FFFFFF);
            bg.setColors(new int[]{halfAlpha, lowAlpha, halfAlpha});
            bg.setStroke(dp(1.8f), activeColor);
        } else {
            bg.setOrientation(GradientDrawable.Orientation.TL_BR);
            bg.setColors(new int[]{bgStart, bgEnd});
            bg.setStroke(dp(1), stroke);
        }
        bg.setCornerRadius(dp(10));
        pill.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(isCompact ? 46 : 52), 1f);
        lp.setMargins(dp(2), 0, dp(2), 0);
        pill.setLayoutParams(lp);
        return pill;
    }

    private View buildRedMagicPill(int iconRes, String labelText, boolean active, boolean isCompact, View.OnClickListener listener) {
        return buildRedMagicPill(iconRes, labelText, active, isCompact, listener, 0xFFFF5964, 0xE6160F16, 0xF50D080D, 0x444A1B28);
    }

    private TextView redModeButton(String text, int profile, boolean isCompact) {
        boolean selected = launcherProfile == profile;
        TextView button = new TextView(this);
        button.setTag(profile);
        button.setText(selected  ?  text + "\nCTIVE" : text);
        button.setTextSize(selected  ?  (isCompact  ?  7.5f : 8f) : (isCompact  ?  7f : 7.5f));
        button.setTextColor(selected  ?  Color.WHITE : 0xFFCCCCCC);
        button.setTypeface(null, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setLines(selected  ?  2 : 1);
        button.setPadding(dp(isCompact  ?  1 : 2), dp(1), dp(isCompact  ?  1 : 2), dp(1));
        GradientDrawable bg = new GradientDrawable();
        if (selected) {
            bg.setOrientation(GradientDrawable.Orientation.TL_BR);
            if (profile == 4) { // Gaming: rich cyber crimson ruby
                bg.setColors(new int[]{0xFFFF0844, 0xFF990022, 0xFFFF2D55});
                bg.setStroke(dp(2.0f), 0xFFFFFFFF);
            } else if (profile == 2) { // Battery: emerald
                bg.setColors(new int[]{0xFF00C868, 0xFF006633, 0xFF00E676});
                bg.setStroke(dp(2.0f), 0xFFFFFFFF);
            } else if (profile == 1) { // Balance: royal purple
                bg.setColors(new int[]{0xFFB12C7, 0xFF581373, 0xFFD46CFF});
                bg.setStroke(dp(2.0f), 0xFFFFFFFF);
            } else if (profile == 3) { // Cooldown: electric cyan
                bg.setColors(new int[]{0xFF00E5FF, 0xFF006B80, 0xFF38BDF8});
                bg.setStroke(dp(2.0f), 0xFFFFFFFF);
            } else if (profile == 7) { // Diablo: flaming orange red
                bg.setColors(new int[]{0xFFFF3D00, 0xFF7A1400, 0xFFFF6E40});
                bg.setStroke(dp(2.0f), 0xFFFFFFFF);
            } else {
                bg.setColors(new int[]{0x99FF1744, 0x66B71C1C, 0x99FF2D55});
                bg.setStroke(dp(1.8f), 0xFFFF5967);
            }
        } else {
            bg.setOrientation(GradientDrawable.Orientation.TL_BR);
            bg.setColors(new int[]{0xE6160F16, 0xF50D080D});
            bg.setStroke(dp(1), 0x444A1B28);
        }
        bg.setCornerRadius(dp(8));
        button.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(isCompact  ?  34 : 40), 1f);
        lp.setMargins(dp(1.5f), 0, dp(1.5f), 0);
        button.setLayoutParams(lp);

        if (selected) {
            currentlyAnimatedModeButton = button;
            if (activeModeAnimator != null) activeModeAnimator.cancel();
            activeModeAnimator = ValueAnimator.ofFloat(1.0f, 1.055f);
            activeModeAnimator.setDuration(1200);
            activeModeAnimator.setRepeatMode(ValueAnimator.REVERSE);
            activeModeAnimator.setRepeatCount(ValueAnimator.INFINITE);
            activeModeAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            activeModeAnimator.addUpdateListener(anim -> {
                float s = (float) anim.getAnimatedValue();
                if (currentlyAnimatedModeButton != null) {
                    currentlyAnimatedModeButton.setScaleX(s);
                    currentlyAnimatedModeButton.setScaleY(s);
                }
            });
            activeModeAnimator.start();
        }

        button.setOnClickListener(v -> {
            v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(60).withEndAction(() -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start();
            }).start();
            applyLauncherProfile(profile);
        });
        energyModeButtons.add(button);
        return button;
    }

        private TextView redToolButton(int iconRes, String text, boolean isCompact, Runnable action, int tintColor, int bgStart, int bgEnd, int stroke) {
        TextView button = label(text, isCompact ? 8.5f : 9.5f, 0xFFF1F1F3);
        button.setTypeface(null, Typeface.BOLD);
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setPadding(dp(isCompact ? 10 : 12), 0, dp(isCompact ? 8 : 10), 0);
        Drawable icon = ContextCompat.getDrawable(this, iconRes);
        if (icon != null) {
            icon = DrawableCompat.wrap(icon.mutate());
            DrawableCompat.setTint(icon, tintColor != 0 ? tintColor : 0xFFFF5360);
            int iconPx = dp(isCompact ? 16 : 19);
            icon.setBounds(0, 0, iconPx, iconPx);
            button.setCompoundDrawables(icon, null, null, null);
            button.setCompoundDrawablePadding(dp(isCompact ? 8 : 10));
        }
        GradientDrawable bg = new GradientDrawable();
        bg.setOrientation(GradientDrawable.Orientation.TL_BR);
        bg.setColors(new int[]{bgStart, bgEnd});
        bg.setCornerRadius(dp(9));
        bg.setStroke(dp(1), stroke);
        button.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(isCompact ? 35 : 40));
        lp.bottomMargin = dp(isCompact ? 4 : 6);
        button.setLayoutParams(lp);
        button.setOnClickListener(v -> {
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(60).withEndAction(() -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start();
            }).start();
            action.run();
        });
        return button;
    }

    private TextView redToolButton(int iconRes, String text, boolean isCompact, Runnable action) {
        return redToolButton(iconRes, text, isCompact, action, 0xFFFF5360, 0xE6160F16, 0xF50D080D, 0x444A1B28);
    }

    private void applyLauncherProfile(int profile) {
        launcherProfile = profile;
        GsmProfileManager.saveSelectedProfile(this, profile);
        updateEnergyModeButtons();
        updateSuperProfileCards();
        GameOptimizer.applyProfile(this, profile, (success, message) ->
                runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show()));
    }

    private void updateEnergyModeButtons() {
        boolean phoenix = GsmProfileManager.isPhoenixSelected(this);
        for (TextView button : energyModeButtons) {
            Object tag = button.getTag();
            if (!(tag instanceof Integer)) continue;
            int prof = (Integer) tag;
            boolean selected = launcherProfile == prof;
            String text;
            if (phoenix) {
                switch (prof) {
                    case 0: text = "BATTERY+"; break;
                    case 1: text = "BALANCE+"; break;
                    case 2: text = "THERMAL+"; break;
                    case 3: text = "COOLDOWN+"; break;
                    case 4: text = "SMOOTH+"; break;
                    case 5: text = "ULTRA+"; break;
                    case 6: text = "ESPORTS+"; break;
                    case 7: text = "APEX+"; break;
                    default: text = "PHX"; break;
                }
            } else {
                switch (prof) {
                    case 0: text = "DAILY USE"; break;
                    case 1: text = "BALANCE"; break;
                    case 2: text = "BATTERY"; break;
                    case 3: text = "GAMING"; break;
                    default: text = "MODE"; break;
                }
            }
            button.setText(selected  ?  text + "\nCTIVE" : text);
            button.setLines(selected  ?  2 : 1);
            button.setTextSize(selected  ?  8.5f : 8f);
            button.setTextColor(selected  ?  Color.WHITE : 0xFFCCCCCC);
            GradientDrawable bg = new GradientDrawable();
            if (selected) {
                bg.setOrientation(GradientDrawable.Orientation.TL_BR);
                bg.setColors(new int[]{0x88FF1744, 0x55B71C1C, 0x88FF2D55});
                bg.setStroke(dp(1.8f), 0xFFFF5967);
            } else {
                bg.setOrientation(GradientDrawable.Orientation.TL_BR);
                bg.setColors(new int[]{0xE6160F16, 0xF50D080D});
                bg.setStroke(dp(1), 0x444A1B28);
            }
            bg.setCornerRadius(dp(8));
            button.setBackground(bg);
        }
    }

    private void toggleFloatingTools() {
        if (!bubbleEnabled) {
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
                if (RootUtils.rootAccess()) {
                    String pkg = getPackageName();
                    RootUtils.runCommand("appops set " + pkg + " SYSTEM_ALERT_WINDOW allow 2>/dev/null; pm grant " + pkg + " android.permission.SYSTEM_ALERT_WINDOW 2>/dev/null");
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Allow overlay permission to display floating game controls", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
                    return;
                }
            }
            bubbleEnabled = true;
            savePreferences();
            String targetPkg = selectedEntry != null  ?  selectedEntry.pkg : "";
            String targetLbl = selectedEntry != null  ?  selectedEntry.label : "Game Space";
            Intent bubble = new Intent(this, GameBubbleService.class)
                    .putExtra("package", targetPkg)
                    .putExtra("label", targetLbl);
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(bubble);
            } else {
                startService(bubble);
            }
            // Notification listener removed to eliminate Play Protect financial fraud trigger
            Toast.makeText(this, "Floating game controls armed & online", Toast.LENGTH_SHORT).show();
        } else {
            bubbleEnabled = false;
            savePreferences();
            startService(new Intent(this, GameBubbleService.class).setAction(GameBubbleService.ACTION_STOP));
            Toast.makeText(this, "Floating game controls disabled", Toast.LENGTH_SHORT).show();
        }
        updateFloatingToolsButton();
    }

    private void updateFloatingToolsButton() {
        if (floatingToolsButton != null) {
            floatingToolsButton.setText("FLOATING GAME TOOLS  |  " + (bubbleEnabled  ?  "ONLINE" : "OFF"));
            GradientDrawable bg = new GradientDrawable();
            if (bubbleEnabled) {
                bg.setColor(0xFF2B1015);
                bg.setStroke(dp(1), 0xFFFF3B48);
            } else {
                bg.setColor(0xFF202024);
                bg.setStroke(dp(1), 0xFF3F3F46);
            }
            bg.setCornerRadius(dp(9));
            floatingToolsButton.setBackground(bg);
        }
        if (floatingToolsTileHolder != null) {
            updateSuperTileState(floatingToolsTileHolder, bubbleEnabled, null);
        }
    }

    private void runQuickLauncherAction(String action) {
        String pkg = selectedEntry == null  ?  getPackageName() : selectedEntry.pkg;
        GameOptimizer.quickAction(this, pkg, action, (success, message) ->
                runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show()));
    }

    private View buildRORBottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF111114);
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), 0xFF303036);
        nav.setBackground(bg);

        int lobbyColor = showSuperBase  ?  0xFF9A9AA3 : 0xFFFF4A55;
        int coreColor = showSuperBase  ?  0xFFFF4A55 : 0xFF9A9AA3;

        View lobby = createBottomNavItem("GAME LOBBY", R.drawable.ic_spectrum_game, lobbyColor, 9.5f, v -> {
            if (showSuperBase) {
                showSuperBase = false;
                rebuildView();
            }
        });
        nav.addView(lobby, new LinearLayout.LayoutParams(0, -1, 1f));

        View core = createBottomNavItem("SUPER BASE / ENERGY CORE", R.drawable.ic_speed_meter, coreColor, 9.5f, v -> {
            if (!showSuperBase) {
                showSuperBase = true;
                rebuildView();
            }
        });
        nav.addView(core, new LinearLayout.LayoutParams(0, -1, 1f));

        View state = createBottomNavItem("ROR ENGINE  |  ONLINE", R.drawable.ic_done, 0xFF74747C, 8.5f, v -> {
            if (!showSuperBase) {
                showSuperBase = true;
                rebuildView();
            }
        });
        nav.addView(state, new LinearLayout.LayoutParams(0, -1, 1f));
        return nav;
    }

    private View createBottomNavItem(String text, int iconRes, int tint, float textSize, View.OnClickListener listener) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER);
        if (listener != null) item.setOnClickListener(listener);

        ImageView iconView = new ImageView(this);
        Drawable icon = ContextCompat.getDrawable(this, iconRes);
        if (icon != null) {
            icon = DrawableCompat.wrap(icon.mutate());
            DrawableCompat.setTint(icon, tint);
            iconView.setImageDrawable(icon);
        }
        item.addView(iconView, new LinearLayout.LayoutParams(dp(16), dp(16)));

        TextView textView = label(text, textSize, tint);
        textView.setTypeface(null, Typeface.BOLD);
        textView.setGravity(Gravity.CENTER_VERTICAL);
        textView.setPadding(dp(8), 0, 0, 0);
        item.addView(textView, new LinearLayout.LayoutParams(-2, -2));

        return item;
    }

    private void applyLeadingIcon(TextView view, int iconRes, int tint, int sizeDp) {
        Drawable icon = ContextCompat.getDrawable(this, iconRes);
        if (icon == null) return;
        icon = DrawableCompat.wrap(icon.mutate());
        DrawableCompat.setTint(icon, tint);
        icon.setBounds(0, 0, dp(sizeDp), dp(sizeDp));
        view.setCompoundDrawables(icon, null, null, null);
        view.setCompoundDrawablePadding(dp(6));
    }

    private WebView buildAdBanner() {
        WebView ad = new WebView(this);
        AdBypassHelper.setupAdWebView(ad, this);
        return ad;
    }

    private void startEntranceAnimations() {
        // PS5 Smooth Entrance Animations
        if (topBarView != null) {
            topBarView.setTranslationY(-dp(45));
            topBarView.setAlpha(0f);
            topBarView.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(350)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        if (heroStageView != null) {
            heroStageView.setScaleX(0.92f);
            heroStageView.setScaleY(0.92f);
            heroStageView.setAlpha(0f);
            heroStageView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1f)
                    .setDuration(400)
                    .setStartDelay(100)
                    .setInterpolator(new OvershootInterpolator(1.1f))
                    .start();
        }

        if (sliderShelfView != null) {
            sliderShelfView.setTranslationY(dp(70));
            sliderShelfView.setAlpha(0f);
            sliderShelfView.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(450)
                    .setStartDelay(160)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    private View buildTopBar() {
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        LinearLayout rootTop = new LinearLayout(this);
        rootTop.setOrientation(LinearLayout.VERTICAL);
        rootTop.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

        // Primary Row: [‹ DASHBOARD] [LOGO] [GAMES] [ALL] ... [HUD scroll in landscape] ... [⤢ ORIENT] [ CONTROLS]
        RelativeLayout mainRow = new RelativeLayout(this);
        mainRow.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(48)));
        mainRow.setClipChildren(false);

        // Left Navigation Group
        LinearLayout leftNav = new LinearLayout(this);
        leftNav.setId(View.generateViewId());
        leftNav.setOrientation(LinearLayout.HORIZONTAL);
        leftNav.setGravity(Gravity.CENTER_VERTICAL);
        RelativeLayout.LayoutParams leftLp = new RelativeLayout.LayoutParams(-2, -1);
        leftLp.addRule(RelativeLayout.ALIGN_PARENT_START);
        leftLp.addRule(RelativeLayout.CENTER_VERTICAL);
        leftNav.setLayoutParams(leftLp);

        // Beautiful Cyber Back Button
        LinearLayout backBtn = new LinearLayout(this);
        backBtn.setOrientation(LinearLayout.HORIZONTAL);
        backBtn.setGravity(Gravity.CENTER_VERTICAL);
        backBtn.setPadding(dp(isPortrait  ?  8 : 10), dp(5), dp(isPortrait  ?  10 : 12), dp(5));
        GradientDrawable backBg = new GradientDrawable();
        backBg.setColor(0x770F1626);
        backBg.setCornerRadius(dp(16));
        backBg.setStroke(dp(1.2f), 0xFF425A88);
        backBtn.setBackground(backBg);
        backBtn.setClickable(true);
        backBtn.setFocusable(true);

        ImageView backIcon = new ImageView(this);
        backIcon.setImageResource(R.drawable.ic_chevron_left);
        backIcon.setColorFilter(0xFF50CBFF);
        int iconSize = dp(isPortrait  ?  18 : 20);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconLp.setMarginEnd(dp(3));
        backBtn.addView(backIcon, iconLp);

        TextView backText = new TextView(this);
        backText.setText(isPortrait  ?  "BCK" : "DASHBOARD");
        backText.setTextSize(isPortrait  ?  9.5f : 10.5f);
        backText.setTextColor(0xFFE2E7FF);
        backText.setTypeface(null, Typeface.BOLD);
        backText.setLetterSpacing(0.06f);
        backBtn.addView(backText);

        backBtn.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            finish();
        });
        leftNav.addView(backBtn);

        // REVROR brand lock-up: a strong mark plus a restrained product wordmark.
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.HORIZONTAL);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        brand.setPadding(dp(7), 0, dp(isPortrait  ?  5 : 12), 0);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ror_logo);
        brand.addView(logo, new LinearLayout.LayoutParams(dp(32), dp(32)));

        if (!isPortrait) {
            LinearLayout brandWords = new LinearLayout(this);
            brandWords.setOrientation(LinearLayout.VERTICAL);
            brandWords.setPadding(dp(7), 0, 0, 0);

            TextView brandName = new TextView(this);
            brandName.setText("REVROR");
            brandName.setTextSize(12);
            brandName.setTextColor(Color.WHITE);
            brandName.setTypeface(null, Typeface.BOLD);
            brandName.setLetterSpacing(0.12f);
            brandWords.addView(brandName);

            TextView brandProduct = new TextView(this);
            brandProduct.setText("GAME SPACE");
            brandProduct.setTextSize(7.5f);
            brandProduct.setTextColor(0xFF7FE9FF);
            brandProduct.setTypeface(null, Typeface.BOLD);
            brandProduct.setLetterSpacing(0.10f);
            brandWords.addView(brandProduct);
            brand.addView(brandWords);
        }
        leftNav.addView(brand);

        // PS5 Navigation Tabs: "GAMES" & "ALL APPS"
        tabGames = createTab("GAMES", !showAllApps);
        tabGames.setOnClickListener(v -> {
            if (showAllApps) {
                showAllApps = false;
                animateTabSwitch(tabGames, tabAll);
                renderSlider();
            }
        });
        leftNav.addView(tabGames);

        tabAll = createTab(isPortrait  ?  "ALL" : "ALL APPS", showAllApps);
        tabAll.setOnClickListener(v -> {
            if (!showAllApps) {
                showAllApps = true;
                animateTabSwitch(tabAll, tabGames);
                renderSlider();
            }
        });
        LinearLayout.LayoutParams allLp = new LinearLayout.LayoutParams(-2, -2);
        allLp.setMarginStart(dp(4));
        leftNav.addView(tabAll, allLp);

        mainRow.addView(leftNav);

        // Right Controls Group: ALWAYS PINNED AND 100% VISIBLE ON EVERY SCREEN
        LinearLayout rightControls = new LinearLayout(this);
        rightControls.setId(View.generateViewId());
        rightControls.setOrientation(LinearLayout.HORIZONTAL);
        rightControls.setGravity(Gravity.CENTER_VERTICAL);
        RelativeLayout.LayoutParams rightLp = new RelativeLayout.LayoutParams(-2, -1);
        rightLp.addRule(RelativeLayout.ALIGN_PARENT_END);
        rightLp.addRule(RelativeLayout.CENTER_VERTICAL);
        rightLp.setMarginEnd(dp(isPortrait  ?  6 : 10));
        rightControls.setLayoutParams(rightLp);

        // Dedicated One-Tap Orientation Toggle Button: ALWAYS PINNED AND VISIBLE
        TextView orientBtn = new TextView(this);
        orientBtn.setText(isPortrait  ?  "LAND" : "PORTRAIT");
        orientBtn.setTextSize(isPortrait  ?  9.5f : 10f);
        orientBtn.setTextColor(0xFF00F5D4);
        orientBtn.setTypeface(null, Typeface.BOLD);
        orientBtn.setGravity(Gravity.CENTER);
        orientBtn.setSingleLine(true);
        orientBtn.setPadding(dp(7), dp(5), dp(7), dp(5));
        GradientDrawable orientBg = new GradientDrawable();
        orientBg.setColor(0x3300F5D4);
        orientBg.setCornerRadius(dp(10));
        orientBg.setStroke(dp(1), 0x9900F5D4);
        orientBtn.setBackground(orientBg);
        LinearLayout.LayoutParams orientLp = new LinearLayout.LayoutParams(-2, -2);
        orientLp.setMarginEnd(dp(6));
        orientBtn.setLayoutParams(orientLp);
        orientBtn.setOnClickListener(v -> {
            v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).withEndAction(() -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                toggleOrientation();
            }).start();
        });
        rightControls.addView(orientBtn);

        // Console Controls / Settings Button with gear icon: ALWAYS PINNED AND VISIBLE
        TextView settingsBtn = new TextView(this);
        settingsBtn.setText(isPortrait  ?  "MENU" : "CONTROLS");
        settingsBtn.setTextSize(isPortrait  ?  9.5f : 10f);
        settingsBtn.setTextColor(Color.WHITE);
        settingsBtn.setTypeface(null, Typeface.BOLD);
        settingsBtn.setGravity(Gravity.CENTER);
        settingsBtn.setSingleLine(true);
        settingsBtn.setEllipsize(TextUtils.TruncateAt.END);
        settingsBtn.setMinWidth(dp(isPortrait  ?  62 : 78));
        settingsBtn.setPadding(dp(8), dp(5), dp(8), dp(5));
        Drawable gear = ContextCompat.getDrawable(this, R.drawable.ic_settings_icon);
        if (gear != null) {
            gear = gear.mutate();
            gear.setBounds(0, 0, dp(12), dp(12));
            DrawableCompat.setTint(gear, Color.WHITE);
            settingsBtn.setCompoundDrawables(gear, null, null, null);
            settingsBtn.setCompoundDrawablePadding(dp(4));
        }
        GradientDrawable setBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF480CA8, 0xFF3F37C9});
        setBg.setCornerRadius(dp(10));
        setBg.setStroke(dp(1), 0xFF7209B7);
        settingsBtn.setBackground(setBg);
        LinearLayout.LayoutParams setLp = new LinearLayout.LayoutParams(-2, -2);
        settingsBtn.setLayoutParams(setLp);
        settingsBtn.setOnClickListener(v -> {
            v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).withEndAction(() -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                showConsoleSettingsDialog();
            }).start();
        });
        rightControls.addView(settingsBtn);

        mainRow.addView(rightControls);

        // In Landscape, HUD pills sit in the primary row between leftNav and rightControls
        if (!isPortrait) {
            HorizontalScrollView hudScroll = new HorizontalScrollView(this);
            hudScroll.setHorizontalScrollBarEnabled(false);
            hudScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
            RelativeLayout.LayoutParams hudLp = new RelativeLayout.LayoutParams(-1, -2);
            hudLp.addRule(RelativeLayout.END_OF, leftNav.getId());
            hudLp.addRule(RelativeLayout.START_OF, rightControls.getId());
            hudLp.addRule(RelativeLayout.CENTER_VERTICAL);
            hudLp.setMargins(dp(4), 0, dp(4), 0);
            hudScroll.setLayoutParams(hudLp);

            LinearLayout hudRow = new LinearLayout(this);
            hudRow.setOrientation(LinearLayout.HORIZONTAL);
            hudRow.setGravity(Gravity.CENTER_VERTICAL);

            hudCpu = createHudPill(R.drawable.ic_cpu, "CPU --", 0xFF64DFDF);
            hudGpu = createHudPill(R.drawable.ic_battery, "BAT --", 0xFF80FFDB);
            hudRam = createHudPill(R.drawable.ic_layers, "RAM --", 0xFF72EFDD);
            hudTemp = createHudPill(R.drawable.ic_temperature, "--", 0xFFFF9E00);
            hudClock = createHudPill(R.drawable.ic_clock, "--", 0xFFE0AAFF);

            int fpsVal1 = 60;
            try { fpsVal1 = (int) Math.round(getWindowManager().getDefaultDisplay().getRefreshRate()); } catch (Exception ignored) {}
            TextView hudFps = createHudPill(R.drawable.ic_speed_meter, fpsVal1 + " FPS", 0xFFFFD600);
            hudFps.setOnClickListener(v -> cycleRefreshRateDirect());

            customTelemetryCpuViews.add(hudCpu);
            customTelemetryBatViews.add(hudGpu);
            customTelemetryRamViews.add(hudRam);
            customTelemetryTempViews.add(hudTemp);
            customTelemetryFpsViews.add(hudFps);
            customTelemetryClockViews.add(hudClock);

            hudRow.addView(hudCpu);
            hudRow.addView(hudGpu);
            hudRow.addView(hudRam);
            hudRow.addView(hudTemp);
            hudRow.addView(hudFps);
            hudRow.addView(hudClock);

            hudScroll.addView(hudRow, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
            mainRow.addView(hudScroll);
        }

        rootTop.addView(mainRow);

        // In Portrait mode, render HUD metrics on a dedicated horizontal scrollbar
        if (isPortrait) {
            HorizontalScrollView hudScroll = new HorizontalScrollView(this);
            hudScroll.setHorizontalScrollBarEnabled(false);
            LinearLayout hudRow = new LinearLayout(this);
            hudRow.setOrientation(LinearLayout.HORIZONTAL);
            hudRow.setGravity(Gravity.CENTER_VERTICAL);
            hudRow.setPadding(0, dp(4), 0, dp(2));

            hudCpu = createHudPill(R.drawable.ic_cpu, "CPU --", 0xFF64DFDF);
            hudGpu = createHudPill(R.drawable.ic_battery, "BAT --", 0xFF80FFDB);
            hudRam = createHudPill(R.drawable.ic_layers, "RAM --", 0xFF72EFDD);
            hudTemp = createHudPill(R.drawable.ic_temperature, "--", 0xFFFF9E00);
            hudClock = createHudPill(R.drawable.ic_clock, "--", 0xFFE0AAFF);

            int fpsVal2 = 60;
            try { fpsVal2 = (int) Math.round(getWindowManager().getDefaultDisplay().getRefreshRate()); } catch (Exception ignored) {}
            TextView hudFpsP = createHudPill(R.drawable.ic_speed_meter, fpsVal2 + " FPS", 0xFFFFD600);
            hudFpsP.setOnClickListener(v -> cycleRefreshRateDirect());

            customTelemetryCpuViews.add(hudCpu);
            customTelemetryBatViews.add(hudGpu);
            customTelemetryRamViews.add(hudRam);
            customTelemetryTempViews.add(hudTemp);
            customTelemetryFpsViews.add(hudFpsP);
            customTelemetryClockViews.add(hudClock);

            hudRow.addView(hudCpu);
            hudRow.addView(hudGpu);
            hudRow.addView(hudRam);
            hudRow.addView(hudTemp);
            hudRow.addView(hudFpsP);
            hudRow.addView(hudClock);

            hudScroll.addView(hudRow, new FrameLayout.LayoutParams(-2, -2));
            rootTop.addView(hudScroll);
        }

        return rootTop;
    }

    private void animateTabSwitch(TextView activeTab, TextView inactiveTab) {
        updateTabStyle(activeTab, true);
        updateTabStyle(inactiveTab, false);
        activeTab.setScaleX(0.9f);
        activeTab.setScaleY(0.9f);
        activeTab.animate().scaleX(1.0f).scaleY(1.0f).setDuration(180)
                .setInterpolator(new OvershootInterpolator(1.4f)).start();
    }

    private TextView createTab(String label, boolean active) {
        TextView tab = new TextView(this);
        tab.setText(label);
        tab.setTextSize(10.5f);
        tab.setTypeface(null, Typeface.BOLD);
        tab.setGravity(Gravity.CENTER);
        tab.setPadding(dp(15), dp(7), dp(15), dp(7));
        updateTabStyle(tab, active);
        return tab;
    }

    private void updateTabStyle(TextView tab, boolean active) {
        if (tab == null) return;
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(14));
        if (active) {
            tab.setTextColor(0xFFFFFFFF);
            g.setColor(0xFF481016);
            g.setStroke(dp(1), 0xFFFF4050);
        } else {
            tab.setTextColor(0xFF8E95AF);
            g.setColor(0xFF18181C);
            g.setStroke(dp(1), 0xFF37373E);
        }
        tab.setBackground(g);
    }

    private void updateTabs() {
        updateTabStyle(tabGames, !showAllApps);
        updateTabStyle(tabAll, showAllApps);
    }

    private TextView createHudPill(int iconRes, String text, int tint) {
        TextView pill = new TextView(this);
        pill.setText(text);
        pill.setTextSize(10);
        pill.setTextColor(tint);
        pill.setTypeface(null, Typeface.BOLD);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(dp(8), dp(4), dp(8), dp(4));

        if (iconRes != 0) {
            Drawable d = ContextCompat.getDrawable(this, iconRes);
            if (d != null) {
                d = d.mutate();
                DrawableCompat.setTint(d, tint);
                int size = dp(12);
                d.setBounds(0, 0, size, size);
                pill.setCompoundDrawables(d, null, null, null);
                pill.setCompoundDrawablePadding(dp(4));
            }
        }

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x66101426);
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), 0x554A5B88);
        pill.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMarginEnd(dp(6));
        pill.setLayoutParams(lp);
        return pill;
    }

    
    private View createQuickLayoutHeaderButton(int accentColor) {
        ImageView btn = new ImageView(this);
        btn.setImageResource(R.drawable.ic_layers);
        btn.setColorFilter(accentColor);
        btn.setPadding(dp(6), dp(6), dp(6), dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xCC09182B);
        bg.setCornerRadius(dp(8));
        bg.setStroke(dp(1), accentColor);
        btn.setBackground(bg);
        btn.setOnClickListener(v -> showLayoutChooser());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(30), dp(30));
        lp.setMarginStart(dp(6));
        btn.setLayoutParams(lp);
        return btn;
    }

    private View createUniversalTelemetryBar(int accentColor) {
        HorizontalScrollView hScroll = new HorizontalScrollView(this);
        hScroll.setHorizontalScrollBarEnabled(false);
        hScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(2), 0, dp(2), 0);

        int cpuTint = (accentColor != 0) ? accentColor : 0xFFFF5B63;
        int ramTint = 0xFF64DFDF;
        int tempTint = 0xFFFF9F43;
        int batTint = 0xFF80FFDB;
        int fpsTint = 0xFFFFD600;

        TextView cpuPill = createHudPill(R.drawable.ic_cpu, "CPU --", cpuTint);
        TextView ramPill = createHudPill(R.drawable.ic_layers, "RAM --", ramTint);
        TextView tempPill = createHudPill(R.drawable.ic_temperature, "--", tempTint);
        TextView batPill = createHudPill(R.drawable.ic_battery, "BAT " + getBatteryLevel() + "%", batTint);

        int currentFps = 60;
        try {
            currentFps = (int) Math.round(getWindowManager().getDefaultDisplay().getRefreshRate());
        } catch (Exception ignored) {}
        TextView fpsPill = createHudPill(R.drawable.ic_speed_meter, currentFps + " FPS", fpsTint);

        View.OnClickListener openTelemetry = v -> showGamingTelemetryDialog();
        cpuPill.setOnClickListener(openTelemetry);
        ramPill.setOnClickListener(openTelemetry);
        tempPill.setOnClickListener(openTelemetry);
        batPill.setOnClickListener(openTelemetry);
        fpsPill.setOnClickListener(v -> cycleRefreshRateDirect());

        customTelemetryCpuViews.add(cpuPill);
        customTelemetryRamViews.add(ramPill);
        customTelemetryTempViews.add(tempPill);
        customTelemetryBatViews.add(batPill);
        customTelemetryFpsViews.add(fpsPill);

        row.addView(cpuPill);
        row.addView(ramPill);
        row.addView(tempPill);
        row.addView(batPill);
        row.addView(fpsPill);

        hScroll.addView(row, new FrameLayout.LayoutParams(-2, -2));
        return hScroll;
    }

    private TextView createMiniFeatureBadge(int iconRes, String text, int tint) {
        TextView badge = new TextView(this);
        badge.setText(text);
        badge.setTextSize(8.5f);
        badge.setTextColor(tint);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(8), dp(4), dp(8), dp(4));

        if (iconRes != 0) {
            Drawable d = ContextCompat.getDrawable(this, iconRes);
            if (d != null) {
                d = d.mutate();
                int size = dp(11);
                d.setBounds(0, 0, size, size);
                DrawableCompat.setTint(d, tint);
                badge.setCompoundDrawables(d, null, null, null);
                badge.setCompoundDrawablePadding(dp(3));
            }
        }

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x66101428);
        bg.setCornerRadius(dp(8));
        bg.setStroke(dp(1), (tint & 0x00FFFFFF) | 0x44000000);
        badge.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMarginEnd(dp(6));
        badge.setLayoutParams(lp);
        return badge;
    }

    private void setBtnMargins(View v) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMarginStart(dp(8));
        v.setLayoutParams(lp);
    }

    private View buildHeroStage() {
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(isPortrait  ?  LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        hero.setGravity(isPortrait  ?  Gravity.CENTER : Gravity.CENTER_VERTICAL);
        hero.setPadding(dp(isPortrait  ?  18 : 24), isPortrait  ?  dp(18) : dp(16),
                dp(isPortrait  ?  18 : 24), isPortrait  ?  dp(18) : dp(16));
        if (!isPortrait) hero.setMinimumHeight(dp(176));

        // PS5 Cinematic Hero Card Gradient
        GradientDrawable heroBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xE6152044, 0xF20A1024, 0xE626143E});
        heroBg.setCornerRadius(dp(22));
        heroBg.setStroke(dp(1), 0x886079B8);
        hero.setBackground(heroBg);

        // Icon + Info row
        LinearLayout mainInfo = new LinearLayout(this);
        mainInfo.setOrientation(isPortrait  ?  LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        mainInfo.setGravity(Gravity.CENTER);

        // Compact Hero Icon Frame with Animated Gaming Reactor Circle
        int containerSize = isPortrait  ?  dp(120) : dp(128);
        FrameLayout iconContainer = new FrameLayout(this);
        liveReactorCircle = new AnimatedGamingCircleView(this);
        iconContainer.addView(liveReactorCircle, new FrameLayout.LayoutParams(-1, -1));

        FrameLayout iconFrame = new FrameLayout(this);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setColor(0xF0120B11);
        iconBg.setCornerRadius(dp(20));
        iconBg.setStroke(dp(2), 0xFFFF2D55);
        iconFrame.setBackground(iconBg);
        iconFrame.setPadding(dp(4), dp(4), dp(4), dp(4));

        heroIcon = new ImageView(this);
        heroIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int iconSize = isPortrait  ?  dp(72) : dp(76);
        iconFrame.addView(heroIcon, new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER));
        int frameSize = isPortrait  ?  dp(88) : dp(92);
        iconContainer.addView(iconFrame, new FrameLayout.LayoutParams(frameSize, frameSize, Gravity.CENTER));
        mainInfo.addView(iconContainer, new LinearLayout.LayoutParams(containerSize, containerSize));

        View.OnClickListener heroLaunchClick = v -> {
            if (liveReactorCircle != null) liveReactorCircle.triggerEnergySurge();
            v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(70).withEndAction(() -> {
                v.animate().scaleX(1.06f).scaleY(1.06f).setDuration(110).withEndAction(() -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(70).start();
                }).start();
            }).start();
            if (selectedEntry != null) prepareLaunch(selectedEntry);
            else if (!displayedEntries.isEmpty()) prepareLaunch(displayedEntries.get(0));
        };
        iconFrame.setClickable(true);
        iconFrame.setFocusable(true);
        iconFrame.setOnClickListener(heroLaunchClick);
        heroIcon.setClickable(true);
        heroIcon.setOnClickListener(heroLaunchClick);

        // Title and Information
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setGravity(isPortrait  ?  Gravity.CENTER_HORIZONTAL : Gravity.CENTER_VERTICAL);
        info.setPadding(isPortrait  ?  0 : dp(18), isPortrait  ?  dp(12) : 0, isPortrait  ?  0 : dp(12), 0);

        TextView eyebrow = new TextView(this);
        eyebrow.setText("PERFORMANCE PROFILE  •  READY");
        eyebrow.setTextSize(8.5f);
        eyebrow.setTextColor(0xFF66E7FF);
        eyebrow.setTypeface(null, Typeface.BOLD);
        eyebrow.setLetterSpacing(0.12f);
        eyebrow.setGravity(isPortrait  ?  Gravity.CENTER : Gravity.START);
        eyebrow.setPadding(0, 0, 0, dp(3));
        info.addView(eyebrow);

        heroTitle = new TextView(this);
        heroTitle.setText("SELECT A GAME");
        heroTitle.setTextSize(isPortrait  ?  20 : 24);
        heroTitle.setTextColor(Color.WHITE);
        heroTitle.setTypeface(null, Typeface.BOLD);
        heroTitle.setMaxLines(1);
        heroTitle.setEllipsize(TextUtils.TruncateAt.END);
        heroTitle.setGravity(isPortrait  ?  Gravity.CENTER : Gravity.NO_GRAVITY);
        info.addView(heroTitle);

        heroSubtitle = new TextView(this);
        heroSubtitle.setText("Professional gaming space  •  REVROR performance engine");
        heroSubtitle.setTextSize(isPortrait  ?  10f : 10.5f);
        heroSubtitle.setTextColor(0xFFAAB4D3);
        heroSubtitle.setMaxLines(1);
        heroSubtitle.setEllipsize(TextUtils.TruncateAt.END);
        heroSubtitle.setPadding(0, dp(1), 0, dp(1));
        heroSubtitle.setGravity(isPortrait  ?  Gravity.CENTER : Gravity.NO_GRAVITY);
        info.addView(heroSubtitle);

        LinearLayout badgeRow = new LinearLayout(this);
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);
        badgeRow.setGravity(isPortrait  ?  Gravity.CENTER : Gravity.START);
        badgeRow.setPadding(0, dp(8), 0, 0);

        badgeRow.addView(createMiniFeatureBadge(R.drawable.ic_flash, "TURBO", 0xFFFFB547));
        badgeRow.addView(createMiniFeatureBadge(R.drawable.ic_shield, "MEMORY", 0xFF67E8F9));
        badgeRow.addView(createMiniFeatureBadge(R.drawable.ic_layers, "LOW LATENCY", 0xFF5EEAD4));
        badgeRow.addView(createMiniFeatureBadge(R.drawable.ic_floating_window, "OVERLAY", 0xFFE879F9));

        info.addView(badgeRow);

        if (isPortrait) {
            mainInfo.addView(info, new LinearLayout.LayoutParams(-1, -2));
            hero.addView(mainInfo, new LinearLayout.LayoutParams(-1, -2));
        } else {
            mainInfo.addView(info, new LinearLayout.LayoutParams(0, -2, 1.0f));
            hero.addView(mainInfo, new LinearLayout.LayoutParams(0, -2, 1.0f));
        }

        // Action Column: PS5 "PLAY GAME" Launch Button & Status
        LinearLayout actionCol = new LinearLayout(this);
        actionCol.setOrientation(LinearLayout.VERTICAL);
        actionCol.setGravity(Gravity.CENTER);

        heroPlayButton = new Button(this);
        heroPlayButton.setText("PLAY");
        heroPlayButton.setTextSize(isPortrait  ?  14 : 13.5f);
        heroPlayButton.setTextColor(Color.WHITE);
        heroPlayButton.setTypeface(null, Typeface.BOLD);
        heroPlayButton.setAllCaps(false);
        Drawable playIcon = ContextCompat.getDrawable(this, R.drawable.ic_start);
        if (playIcon != null) {
            playIcon = playIcon.mutate();
            playIcon.setBounds(0, 0, dp(14), dp(14));
            DrawableCompat.setTint(playIcon, Color.WHITE);
            heroPlayButton.setCompoundDrawables(playIcon, null, null, null);
            heroPlayButton.setCompoundDrawablePadding(dp(6));
        }

        // PS5 Radiant Action Gradient
        GradientDrawable playBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF087FC1, 0xFF5146D8, 0xFF8B2CC4});
        playBg.setCornerRadius(dp(16));
        playBg.setStroke(dp(1), 0xFF63E4FF);
        heroPlayButton.setBackground(playBg);
        heroPlayButton.setPadding(dp(18), dp(6), dp(18), dp(6));
        heroPlayButton.setOnClickListener(v -> {
            // PS5 Button Press Animation
            v.animate().scaleX(0.90f).scaleY(0.90f).setDuration(80).withEndAction(() -> {
                v.animate().scaleX(1.06f).scaleY(1.06f).setDuration(120).withEndAction(() -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start();
                }).start();
            }).start();

            if (selectedEntry != null) {
                prepareLaunch(selectedEntry);
            } else if (!displayedEntries.isEmpty()) {
                prepareLaunch(displayedEntries.get(0));
            }
        });

        // Live status under play button
        statusText = new TextView(this);
        statusText.setText(verticalGameScroll != null  ?  "READY TO LAUNCH  •  FLOATING TOOLS ARMED" : "Ready to launch");
        statusText.setTextSize(9.5f);
        statusText.setTextColor(0xFF94A3B8);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(dp(4), dp(3), dp(4), 0);

        progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);

        if (isPortrait) {
            LinearLayout.LayoutParams playLp = new LinearLayout.LayoutParams(dp(220), dp(44));
            playLp.topMargin = dp(12);
            actionCol.addView(heroPlayButton, playLp);
            actionCol.addView(statusText);
            actionCol.addView(progressBar, new LinearLayout.LayoutParams(dp(18), dp(18)));
            hero.addView(actionCol, new LinearLayout.LayoutParams(-1, -2));
        } else {
            actionCol.addView(heroPlayButton, new LinearLayout.LayoutParams(dp(166), dp(48)));
            actionCol.addView(statusText);
            actionCol.addView(progressBar, new LinearLayout.LayoutParams(dp(18), dp(18)));
            hero.addView(actionCol, new LinearLayout.LayoutParams(-2, -2));
        }

        return hero;
    }

    private View buildSliderShelf() {
        LinearLayout shelf = new LinearLayout(this);
        shelf.setOrientation(LinearLayout.VERTICAL);
        shelf.setClipChildren(false);
        shelf.setClipToPadding(false);

        // Shelf label
        LinearLayout shelfHeader = new LinearLayout(this);
        shelfHeader.setOrientation(LinearLayout.HORIZONTAL);
        shelfHeader.setGravity(Gravity.CENTER_VERTICAL);
        shelfHeader.setPadding(dp(4), dp(3), dp(4), dp(4));

        TextView shelfTitle = new TextView(this);
        shelfTitle.setText("GAME LIBRARY");
        shelfTitle.setTextSize(10.5f);
        shelfTitle.setTextColor(0xFFC0C8E2);
        shelfTitle.setTypeface(null, Typeface.BOLD);
        shelfTitle.setLetterSpacing(0.08f);
        shelfHeader.addView(shelfTitle, new LinearLayout.LayoutParams(0, -2, 1.0f));

        TextView hint = new TextView(this);
        hint.setText("SWIPE TO BROWSE  •  TAP AGAIN TO LAUNCH");
        hint.setTextSize(8.5f);
        hint.setTextColor(0xFF667397);
        shelfHeader.addView(hint);
        shelf.addView(shelfHeader);

        // Horizontal Carousel
        sliderScroll = new HorizontalScrollView(this);
        sliderScroll.setHorizontalScrollBarEnabled(false);
        sliderScroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        sliderScroll.setClipChildren(false);
        sliderScroll.setClipToPadding(false);

        LinearLayout horizontalSlider = new LinearLayout(this);
        horizontalSlider.setOrientation(LinearLayout.HORIZONTAL);
        horizontalSlider.setGravity(Gravity.CENTER_VERTICAL);
        sliderContainer = horizontalSlider;
        sliderContainer.setClipChildren(false);
        sliderContainer.setClipToPadding(false);
        sliderContainer.setPadding(dp(4), dp(4), dp(16), dp(6));

        sliderScroll.addView(sliderContainer, new FrameLayout.LayoutParams(-2, -2));
        shelf.addView(sliderScroll, new LinearLayout.LayoutParams(-1, -2));

        return shelf;
    }

    private void loadApps() {
        allEntries.clear();
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        Set<String> seen = new HashSet<>();

        for (ResolveInfo info : pm.queryIntentActivities(intent, 0)) {
            if (info.activityInfo == null || info.activityInfo.applicationInfo == null) continue;
            ApplicationInfo ai = info.activityInfo.applicationInfo;
            if (getPackageName().equals(ai.packageName) || !seen.add(ai.packageName)) continue;
            
            // Clean user-friendly label
            CharSequence rawLabel = info.loadLabel(pm);
            String label = rawLabel != null  ?  rawLabel.toString().trim() : "";
            // If label is missing or is raw class name like com.android..., resolve from appInfo or package
            if (label.isEmpty() || label.startsWith("com.") || label.contains(".activity.")) {
                CharSequence appLabel = ai.loadLabel(pm);
                if (appLabel != null && !appLabel.toString().trim().isEmpty() && !appLabel.toString().startsWith("com.")) {
                    label = appLabel.toString().trim();
                } else {
                    String p = ai.packageName;
                    int lastDot = p.lastIndexOf('.');
                    label = (lastDot >= 0 && lastDot < p.length() - 1)  ?  p.substring(lastDot + 1) : p;
                    // Capitalize
                    if (label.length() > 0) label = Character.toUpperCase(label.charAt(0)) + label.substring(1);
                }
            }

            boolean excluded = com.revror.kernelmanager.utils.GameLauncherRegistry.isGameExcluded(this, ai.packageName);
            boolean isGame = !excluded && (ai.category == ApplicationInfo.CATEGORY_GAME
                    || label.toLowerCase().contains("ggpo")
                    || label.toLowerCase().contains("game")
                    || label.toLowerCase().contains("zarchiver")
                    || label.toLowerCase().contains("nethersx2")
                    || label.toLowerCase().contains("duty")
                    || com.revror.kernelmanager.utils.GameLauncherRegistry.isGameRegistered(this, ai.packageName));
            if (isGame && !excluded) {
                com.revror.kernelmanager.utils.GameLauncherRegistry.registerGame(this, ai.packageName, label);
            }
            allEntries.add(new GameEntry(label, ai.packageName, isGame, info));
        }

        // Sort: games first, then alphabetically
        Collections.sort(allEntries, (a, b) -> {
            if (a.game != b.game) return a.game  ?  -1 : 1;
            return a.label.compareToIgnoreCase(b.label);
        });

        renderSlider();
    }

    private void bindNoGamesState() {
        selectedEntry = null;
        if (heroIcon != null) {
            heroIcon.setImageResource(R.drawable.ror_logo);
            heroIcon.setColorFilter(null);
            heroIcon.setAlpha(1.0f);
            heroIcon.setScaleX(1.0f);
            heroIcon.setScaleY(1.0f);
            heroIcon.setOnClickListener(v -> {
                try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
                showAddGamesDialog();
            });
        }
        if (heroTitle != null) {
            heroTitle.setText("NO GAMES ADDED");
        }
        if (heroSubtitle != null) {
            heroSubtitle.setText("Tap here or '+' to add your games");
        }
        if (heroBadges != null) {
            heroBadges.setText("GAME SPACE  •  NO TITLES REGISTERED");
        }
        if (heroPlayButton != null) {
            heroPlayButton.setText("ADD GAMES");
            heroPlayButton.setOnClickListener(v -> {
                try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
                showAddGamesDialog();
            });
        }
        if (statusText != null) {
            statusText.setText("Tap icon or button to add games");
        }
    }

    private void renderSlider() {
        displayedEntries.clear();
        for (GameEntry entry : allEntries) {
            if (!showAllApps && !entry.game) continue;
            displayedEntries.add(entry);
        }

        if (sliderContainer == null) return;
        sliderContainer.removeAllViews();

        if (displayedEntries.isEmpty()) {
            bindNoGamesState();

            LinearLayout addCard = new LinearLayout(this);
            addCard.setOrientation(LinearLayout.VERTICAL);
            addCard.setGravity(Gravity.CENTER);
            addCard.setPadding(dp(24), dp(16), dp(24), dp(16));
            GradientDrawable addBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{0xFF202028, 0xFF14141A, 0xFF280B12});
            addBg.setCornerRadius(dp(16));
            addBg.setStroke(dp(1.5f), 0xFFFF2D55);
            addCard.setBackground(addBg);

            TextView plusTv = new TextView(this);
            plusTv.setText("+");
            plusTv.setTextSize(32);
            plusTv.setTextColor(0xFFFF2D55);
            plusTv.setTypeface(null, Typeface.BOLD);
            plusTv.setGravity(Gravity.CENTER);
            addCard.addView(plusTv);

            TextView addTitle = new TextView(this);
            addTitle.setText("ADD GAMES");
            addTitle.setTextSize(12);
            addTitle.setTextColor(Color.WHITE);
            addTitle.setTypeface(null, Typeface.BOLD);
            addTitle.setGravity(Gravity.CENTER);
            addTitle.setLetterSpacing(0.08f);
            addCard.addView(addTitle);

            TextView addSub = new TextView(this);
            addSub.setText("Tap to choose installed games");
            addSub.setTextSize(9.5f);
            addSub.setTextColor(0xFF8E95AF);
            addSub.setGravity(Gravity.CENTER);
            addSub.setPadding(0, dp(2), 0, 0);
            addCard.addView(addSub);

            addCard.setOnClickListener(v -> {
                try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
                showAddGamesDialog();
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(190), dp(110));
            lp.setMargins(dp(10), dp(6), dp(10), dp(6));
            sliderContainer.addView(addCard, lp);
            return;
        }

        if (selectedEntry == null || !displayedEntries.contains(selectedEntry)) {
            selectGame(displayedEntries.get(0));
        } else {
            selectGame(selectedEntry);
        }

        // Build each card in the PS5 slider with staggered cascading entry animation
        for (int i = 0; i < displayedEntries.size(); i++) {
            GameEntry entry = displayedEntries.get(i);
            boolean lobbyGrid = verticalGameScroll != null && layoutIndex % LAYOUT_NAMES.length >= 3;
            View card = verticalGameScroll != null
                     ?  (lobbyGrid  ?  buildLobbyGridCard(entry, layoutIndex % LAYOUT_NAMES.length) : buildRailCard(entry))
                    : buildSliderCard(entry, i);

            // PS5 Cascade Slide-in animation for each tile
            card.setTranslationX(dp(60));
            card.setAlpha(0f);
            card.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(280)
                    .setStartDelay(180 + (i * 40))
                    .setInterpolator(new DecelerateInterpolator())
                    .start();

            if (lobbyGrid && sliderContainer instanceof GridLayout) {
                GridLayout.LayoutParams gridLp = new GridLayout.LayoutParams();
                gridLp.width = 0;
                gridLp.height = dp(layoutIndex % LAYOUT_NAMES.length == 4  ?  132 : 104);
                gridLp.columnSpec = GridLayout.spec(i % 2, 1, 1f);
                gridLp.rowSpec = GridLayout.spec(i / 2);
                gridLp.setMargins(dp(3), dp(3), dp(3), dp(3));
                sliderContainer.addView(card, gridLp);
            } else {
                sliderContainer.addView(card);
            }
        }

        // Select the first game if none selected or not in current list
        if (selectedEntry == null || !displayedEntries.contains(selectedEntry)) {
            selectGame(displayedEntries.get(0));
        } else {
            selectGame(selectedEntry);
        }
    }

    private View buildSliderCard(GameEntry entry, int index) {
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(isPortrait  ?  10 : 8), dp(isPortrait  ?  10 : 8), dp(isPortrait  ?  10 : 8), dp(isPortrait  ?  8 : 6));
        card.setTag(entry);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(isPortrait  ?  118 : 116), dp(isPortrait  ?  136 : 126));
        lp.setMargins(dp(isPortrait  ?  5 : 4), dp(isPortrait  ?  4 : 2), dp(isPortrait  ?  5 : 4), dp(isPortrait  ?  4 : 2));
        card.setLayoutParams(lp);

        // Game icon sits in a consistent glass frame so mixed app artwork still feels cohesive.
        FrameLayout iconShell = new FrameLayout(this);
        GradientDrawable iconShellBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF1C1C21, 0xFF101014});
        iconShellBg.setCornerRadius(dp(14));
        iconShellBg.setStroke(dp(1), 0xFF45454C);
        iconShell.setBackground(iconShellBg);

        ImageView icon = new ImageView(this);
        icon.setImageDrawable(entry.resolve.loadIcon(getPackageManager()));
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int artworkSize = dp(isPortrait  ?  48 : 44);
        iconShell.addView(icon, new FrameLayout.LayoutParams(artworkSize, artworkSize, Gravity.CENTER));
        LinearLayout.LayoutParams shellLp = new LinearLayout.LayoutParams(dp(isPortrait  ?  62 : 58), dp(isPortrait  ?  62 : 58));
        shellLp.topMargin = dp(2);
        card.addView(iconShell, shellLp);

        // Game Label
        TextView title = new TextView(this);
        title.setText(entry.label);
        title.setTextSize(11f);
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setPadding(0, dp(7), 0, 0);
        card.addView(title, new LinearLayout.LayoutParams(-1, -2));

        // Game tag indicator
        if (entry.game) {
            TextView tag = new TextView(this);
            tag.setText(" GAME");
            tag.setTextSize(8.5f);
            tag.setTextColor(0xFFFF5964);
            tag.setGravity(Gravity.CENTER);
            tag.setPadding(0, dp(3), 0, 0);
            card.addView(tag);
        } else {
            TextView addTag = new TextView(this);
            addTag.setText("+ ADD GAME");
            addTag.setTextSize(8f);
            addTag.setTextColor(0xFFD1A2FF);
            addTag.setGravity(Gravity.CENTER);
            addTag.setPadding(0, dp(3), 0, 0);
            addTag.setOnClickListener(v -> {
                com.revror.kernelmanager.utils.GameLauncherRegistry.registerGame(this, entry.pkg, entry.label);
                Toast.makeText(this, "Added " + entry.label + " to REVROR Games", Toast.LENGTH_SHORT).show();
                loadApps();
            });
            card.addView(addTag);
        }

        card.setOnLongClickListener(v -> {
            boolean isReg = com.revror.kernelmanager.utils.GameLauncherRegistry.isGameRegistered(this, entry.pkg);
            new AlertDialog.Builder(this)
                    .setTitle(entry.label)
                    .setMessage(isReg  ?  "Remove from REVROR Games registry • " : "Add to REVROR Games registry • \nEnables floating controls automatically whenever opened.")
                    .setPositiveButton(isReg  ?  "Remove" : "Add to Games", (d, w) -> {
                        if (isReg) {
                            com.revror.kernelmanager.utils.GameLauncherRegistry.unregisterGame(this, entry.pkg);
                            Toast.makeText(this, "Removed from REVROR Games", Toast.LENGTH_SHORT).show();
                        } else {
                            com.revror.kernelmanager.utils.GameLauncherRegistry.registerGame(this, entry.pkg, entry.label);
                            Toast.makeText(this, "Added to REVROR Games", Toast.LENGTH_SHORT).show();
                        }
                        loadApps();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });

        View.OnClickListener portClick = v -> {
            v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(60).withEndAction(() -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start();
            }).start();
            if (!entry.game) {
                com.revror.kernelmanager.utils.GameLauncherRegistry.registerGame(this, entry.pkg, entry.label);
                loadApps();
            } else if (selectedEntry == entry) {
                prepareLaunch(entry);
            } else {
                selectGame(entry);
            }
        };
        card.setOnClickListener(portClick);
        iconShell.setOnClickListener(portClick);
        icon.setOnClickListener(portClick);

        return card;
    }

    private View buildRailCard(GameEntry entry) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float heightDp = dm.heightPixels / dm.density;
        boolean isCompact = heightDp < 460;

        int activeLayout = layoutIndex % LAYOUT_NAMES.length;
        int cardHeight = isCompact  ?  58 : (activeLayout == 1  ?  58 : (activeLayout == 2  ?  92 : 76));
        int frameSize = isCompact  ?  44 : (activeLayout == 1  ?  42 : (activeLayout == 2  ?  68 : 56));
        int iconSize = isCompact  ?  34 : (activeLayout == 1  ?  32 : (activeLayout == 2  ?  54 : 44));
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(isCompact  ?  8 : 10), dp(isCompact  ?  5 : 8), dp(isCompact  ?  8 : 10), dp(isCompact  ?  5 : 8));
        card.setTag(entry);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, dp(cardHeight));
        cardLp.bottomMargin = dp(isCompact  ?  5 : 7);
        card.setLayoutParams(cardLp);

        FrameLayout iconFrame = new FrameLayout(this);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setColor(0xFF101014);
        iconBg.setCornerRadius(dp(isCompact  ?  10 : 11));
        iconBg.setStroke(dp(1), 0xFF45454C);
        iconFrame.setBackground(iconBg);
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(entry.resolve.loadIcon(getPackageManager()));
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconFrame.addView(icon, new FrameLayout.LayoutParams(dp(iconSize), dp(iconSize), Gravity.CENTER));
        card.addView(iconFrame, new LinearLayout.LayoutParams(dp(frameSize), dp(frameSize)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setGravity(Gravity.CENTER_VERTICAL);
        copy.setPadding(dp(isCompact  ?  8 : 11), 0, 0, 0);
        TextView name = label(entry.label, isCompact  ?  10.5f : (activeLayout == 2  ?  12 : (activeLayout == 1  ?  10 : 11)), Color.WHITE);
        name.setTypeface(null, Typeface.BOLD);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(name);
        String metaText = activeLayout == 2
                 ?  (entry.game  ?  "READY TO PLAY  /  " + entry.pkg : "ADD TO GAME SPACE  /  " + entry.pkg)
                : (entry.game  ?  "READY TO PLAY" : "ADD TO GAME SPACE");
        TextView meta = label(metaText, isCompact  ?  6.8f : (activeLayout == 1  ?  7 : 7.5f),
                entry.game  ?  0xFFFF5964 : 0xFF85858D);
        meta.setTypeface(null, Typeface.BOLD);
        meta.setLetterSpacing(.08f);
        meta.setPadding(0, dp(isCompact  ?  1 : 3), 0, 0);
        if (activeLayout != 1) copy.addView(meta);
        card.addView(copy, new LinearLayout.LayoutParams(0, -1, 1f));

        ImageView arrow = new ImageView(this);
        arrow.setImageResource(R.drawable.ic_chevron_right);
        arrow.setColorFilter(0xFF707078);
        arrow.setPadding(dp(isCompact  ?  3 : 5), dp(isCompact  ?  3 : 5), dp(isCompact  ?  3 : 5), dp(isCompact  ?  3 : 5));
        card.addView(arrow, new LinearLayout.LayoutParams(dp(isCompact  ?  20 : 24), dp(isCompact  ?  20 : 24)));

        View.OnClickListener railClick = v -> {
            v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(60).withEndAction(() -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start();
            }).start();
            if (!entry.game) {
                com.revror.kernelmanager.utils.GameLauncherRegistry.registerGame(this, entry.pkg, entry.label);
                loadApps();
            } else if (selectedEntry == entry) {
                prepareLaunch(entry);
            } else {
                selectGame(entry);
            }
        };
        card.setOnClickListener(railClick);
        iconFrame.setOnClickListener(railClick);
        icon.setOnClickListener(railClick);
        arrow.setOnClickListener(v -> {
            v.animate().scaleX(1.25f).scaleY(1.25f).setDuration(70).withEndAction(() -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(70).start();
            }).start();
            if (!entry.game) {
                com.revror.kernelmanager.utils.GameLauncherRegistry.registerGame(this, entry.pkg, entry.label);
                loadApps();
            } else {
                prepareLaunch(entry);
            }
        });
        card.setOnLongClickListener(v -> {
            boolean registered = com.revror.kernelmanager.utils.GameLauncherRegistry.isGameRegistered(this, entry.pkg);
            new AlertDialog.Builder(this)
                    .setTitle(entry.label)
                    .setMessage(registered  ?  "Remove from Game Space • " : "Add to Game Space • ")
                    .setPositiveButton(registered  ?  "Remove" : "Add", (d, w) -> {
                        if (registered) com.revror.kernelmanager.utils.GameLauncherRegistry.unregisterGame(this, entry.pkg);
                        else com.revror.kernelmanager.utils.GameLauncherRegistry.registerGame(this, entry.pkg, entry.label);
                        loadApps();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
        return card;
    }

    private View buildLobbyGridCard(GameEntry entry, int style) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(7), dp(7), dp(7), dp(7));
        card.setTag(entry);
        GradientDrawable base = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                style == 4  ?  new int[]{0xFF242429, 0xFF101014} : new int[]{0xFF1D1D21, 0xFF111114});
        base.setCornerRadius(dp(12));
        base.setStroke(dp(1), 0xFF414147);
        card.setBackground(base);

        FrameLayout art = new FrameLayout(this);
        GradientDrawable artBg = new GradientDrawable();
        artBg.setColor(0xFF0B0B0E);
        artBg.setCornerRadius(dp(style == 4  ?  13 : 10));
        artBg.setStroke(dp(1), 0xFF505058);
        art.setBackground(artBg);
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(entry.resolve.loadIcon(getPackageManager()));
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int artSize = style == 4  ?  70 : 50;
        art.addView(icon, new FrameLayout.LayoutParams(dp(artSize - 8), dp(artSize - 8), Gravity.CENTER));
        card.addView(art, new LinearLayout.LayoutParams(dp(artSize), dp(artSize)));

        TextView name = label(entry.label, style == 4  ?  9.5f : 8.5f, Color.WHITE);
        name.setTypeface(null, Typeface.BOLD);
        name.setGravity(Gravity.CENTER);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setPadding(dp(2), dp(5), dp(2), 0);
        card.addView(name, new LinearLayout.LayoutParams(-1, -2));
        TextView state = label(entry.game  ?  "READY" : "ADD", 7, entry.game  ?  0xFFFF5964 : 0xFF85858D);
        state.setTypeface(null, Typeface.BOLD);
        state.setGravity(Gravity.CENTER);
        card.addView(state);

        View.OnClickListener gridClick = v -> {
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(60).withEndAction(() -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start();
            }).start();
            if (!entry.game) {
                com.revror.kernelmanager.utils.GameLauncherRegistry.registerGame(this, entry.pkg, entry.label);
                loadApps();
            } else if (selectedEntry == entry) {
                prepareLaunch(entry);
            } else {
                selectGame(entry);
            }
        };
        card.setOnClickListener(gridClick);
        art.setOnClickListener(gridClick);
        icon.setOnClickListener(gridClick);
        card.setOnLongClickListener(v -> {
            boolean registered = com.revror.kernelmanager.utils.GameLauncherRegistry.isGameRegistered(this, entry.pkg);
            if (registered) com.revror.kernelmanager.utils.GameLauncherRegistry.unregisterGame(this, entry.pkg);
            else com.revror.kernelmanager.utils.GameLauncherRegistry.registerGame(this, entry.pkg, entry.label);
            loadApps();
            return true;
        });
        return card;
    }

    private void selectGame(GameEntry entry) {
        if (entry == null) {
            bindNoGamesState();
            return;
        }
        selectedEntry = entry;
        if (heroPlayButton != null) {
            heroPlayButton.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(60).withEndAction(() ->
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start()).start();
                prepareLaunch(entry);
            });
        }
        if (liveReactorCircle != null) {
            liveReactorCircle.triggerEnergySurge();
        }
        if (entry.resolve != null) {
            try {
                heroIcon.setImageDrawable(entry.resolve.loadIcon(getPackageManager()));
            } catch (Exception ignored) {}
        }
        heroIcon.setAlpha(1.0f);
        heroIcon.setScaleX(1.0f);
        heroIcon.setScaleY(1.0f);

        // 1. PS5 Hero Slide & Crossfade Transitions
        heroIcon.animate().alpha(0.6f).scaleX(0.85f).scaleY(0.85f).setDuration(80).withEndAction(() -> {
            if (!isFinishing() && !isDestroyed()) {
                if (entry.resolve != null) {
                    try {
                        heroIcon.setImageDrawable(entry.resolve.loadIcon(getPackageManager()));
                    } catch (Exception ignored) {}
                }
                heroIcon.animate().alpha(1f).scaleX(1.0f).scaleY(1.0f).setDuration(200)
                        .setInterpolator(new OvershootInterpolator(1.2f)).start();
            }
        }).start();

        heroTitle.animate().alpha(0f).translationX(-dp(20)).setDuration(100).withEndAction(() -> {
            if (!isFinishing() && !isDestroyed()) {
                heroTitle.setText(entry.label != null  ?  entry.label : "GAME");
                heroTitle.setTranslationX(dp(30));
                heroTitle.animate().alpha(1f).translationX(0f).setDuration(220)
                        .setInterpolator(new DecelerateInterpolator()).start();
            }
        }).start();

        heroSubtitle.animate().alpha(0f).translationX(-dp(15)).setDuration(100).withEndAction(() -> {
            if (!isFinishing() && !isDestroyed()) {
                heroSubtitle.setText((entry.pkg != null  ?  entry.pkg : "") + (entry.game  ?  "  •  VERIFIED GAME" : "  •  APPLICATION"));
                heroSubtitle.setTranslationX(dp(22));
                heroSubtitle.animate().alpha(1f).translationX(0f).setDuration(220)
                        .setInterpolator(new DecelerateInterpolator()).start();
            }
        }).start();

        heroPlayButton.animate().scaleX(0.92f).scaleY(0.92f).setDuration(90).withEndAction(() -> {
            if (!isFinishing() && !isDestroyed()) {
                String lbl = entry.label != null  ?  entry.label : "GAME";
                heroPlayButton.setText("PLAY " + (lbl.length() > 9  ?  lbl.substring(0, 9) + "…" : lbl).toUpperCase());
                heroPlayButton.animate().scaleX(1.0f).scaleY(1.0f).setDuration(220)
                        .setInterpolator(new OvershootInterpolator(1.4f)).start();
            }
        }).start();

        statusText.setText("Ready to launch");

        // 2. PS5 Carousel Card Focus Animations (Spring Pop & Centering)
        View selectedView = null;
        if (sliderContainer != null) {
            for (int i = 0; i < sliderContainer.getChildCount(); i++) {
                View v = sliderContainer.getChildAt(i);
                GameEntry item = (GameEntry) v.getTag();
                boolean isSelected = (item == entry);

                GradientDrawable cardBg = new GradientDrawable();
                cardBg.setCornerRadius(dp(16));

                if (isSelected) {
                    selectedView = v;
                    cardBg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
                    cardBg.setColors(new int[]{0xFF4A1016, 0xFF21171A});
                    cardBg.setStroke(dp(2), 0xFFFF3D4A);

                    // Spring Pop & Lift
                    v.animate().scaleX(1.06f).scaleY(1.06f).translationY(-dp(3)).setDuration(110).withEndAction(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            v.animate().scaleX(1.0f).scaleY(1.0f).translationY(0f).setDuration(130)
                                    .setInterpolator(new DecelerateInterpolator()).start();
                        }
                    }).start();
                } else {
                    cardBg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
                    cardBg.setColors(new int[]{0xFF1A1A1E, 0xFF0E0E11});
                    cardBg.setStroke(dp(1), 0xFF35353C);
                }
                v.setBackground(cardBg);
            }
        }

        // 3. Smooth Auto-centering of the selected card in the slider
        if (selectedView != null) {
            final View finalTarget = selectedView;
            if (verticalGameScroll != null) {
                final ScrollView verticalTarget = verticalGameScroll;
                verticalTarget.post(() -> {
                    int target = finalTarget.getTop() - (verticalTarget.getHeight() / 2) + (finalTarget.getHeight() / 2);
                    verticalTarget.smoothScrollTo(0, Math.max(0, target));
                });
            } else if (sliderScroll != null) {
                final HorizontalScrollView horizontalTarget = sliderScroll;
                horizontalTarget.post(() -> {
                    int scrollWidth = horizontalTarget.getWidth();
                    if (scrollWidth > 0) {
                        int scrollTo = finalTarget.getLeft() - (scrollWidth / 2) + (finalTarget.getWidth() / 2);
                        horizontalTarget.smoothScrollTo(Math.max(0, scrollTo), 0);
                    }
                });
            }
        }
    }

    private void prepareLaunch(GameEntry entry) {
        if (cleanupEnabled && !getSharedPreferences("ror_game_launcher", 0).getBoolean("cleanup_ack", false)) {
            new AlertDialog.Builder(this)
                    .setTitle("Background App Cleanup")
                    .setMessage("REVROR will optimize device resources by stopping non-essential background user apps.\n\nSystem apps, REVROR, keyboard, protected apps, and " + entry.label + " remain active.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Continue", (d, w) -> {
                        getSharedPreferences("ror_game_launcher", 0).edit().putBoolean("cleanup_ack", true).apply();
                        launch(entry);
                    }).show();
        } else {
            launch(entry);
        }
    }

    private void launch(GameEntry entry) {
        if (entry == null) return;
        if (bubbleEnabled && Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            if (RootUtils.rootAccess()) {
                String pkg = getPackageName();
                RootUtils.runCommand("appops set " + pkg + " SYSTEM_ALERT_WINDOW allow 2>/dev/null; pm grant " + pkg + " android.permission.SYSTEM_ALERT_WINDOW 2>/dev/null");
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Allow overlay permission for floating game controls, then tap PLAY again", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
                return;
            }
        }

        Dialog warpDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        FrameLayout root = new FrameLayout(this);
        GradientDrawable rootBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF030304, 0xFF26080E, 0xFF09090C, 0xFF3A0912});
        root.setBackground(rootBg);

        View scanLine = new View(this);
        GradientDrawable scanBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0x00FF2638, 0xCCFF2638, 0x00FF2638});
        scanLine.setBackground(scanBg);
        root.addView(scanLine, new FrameLayout.LayoutParams(-1, dp(2), Gravity.TOP));
        ObjectAnimator scanAnimator = ObjectAnimator.ofFloat(scanLine, "translationY", 0,
                getResources().getDisplayMetrics().heightPixels);
        scanAnimator.setDuration(1150);
        scanAnimator.setRepeatCount(ValueAnimator.INFINITE);
        scanAnimator.start();

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(24), dp(22), dp(24), dp(22));

        GradientDrawable boxBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xF61C080E, 0xF80A0C14, 0xF6180812});
        boxBg.setCornerRadius(dp(20));
        boxBg.setStroke(dp(2f), 0xFFFF1744); // Red Magic signature crimson neon border
        box.setBackground(boxBg);

        // Red Magic Tactical HUD Badge
        LinearLayout badge = new LinearLayout(this);
        badge.setOrientation(LinearLayout.HORIZONTAL);
        badge.setGravity(Gravity.CENTER_VERTICAL);
        badge.setPadding(dp(12), dp(4), dp(12), dp(4));
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(0x33FF1744);
        badgeBg.setCornerRadius(dp(12));
        badgeBg.setStroke(dp(1), 0xAAFF1744);
        badge.setBackground(badgeBg);

        View led = new View(this);
        GradientDrawable ledBg = new GradientDrawable();
        ledBg.setShape(GradientDrawable.OVAL);
        ledBg.setColor(0xFFFF1744);
        led.setBackground(ledBg);
        badge.addView(led, new LinearLayout.LayoutParams(dp(7), dp(7)));

        TextView badgeText = new TextView(this);
        badgeText.setText("  TITAN  •  TURBO WARP ENGINE");
        badgeText.setTextColor(0xFFFF5252);
        badgeText.setTextSize(9f);
        badgeText.setTypeface(null, Typeface.BOLD);
        badgeText.setLetterSpacing(0.12f);
        badge.addView(badgeText);

        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        badgeLp.bottomMargin = dp(14);
        box.addView(badge, badgeLp);

        FrameLayout iconRing = new FrameLayout(this);

        // 1. Rotating Sci-Fi Tech HUD Ring (Outer Clockwise)
        View hudRing = new View(this) {
            final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas c) {
                super.onDraw(c);
                float cx = getWidth() / 2f, cy = getHeight() / 2f, r = getWidth() * 0.46f;
                p.setStyle(Paint.Style.STROKE);
                p.setColor(0xCCFF1744);
                p.setStrokeWidth(dp(2.0f));
                c.drawCircle(cx, cy, r, p);
                p.setStrokeWidth(dp(2.6f));
                for (int i = 0; i < 16; i++) {
                    double rad = Math.toRadians(i * 22.5);
                    float x1 = (float)(cx + (r - dp(6)) * Math.cos(rad));
                    float y1 = (float)(cy + (r - dp(6)) * Math.sin(rad));
                    float x2 = (float)(cx + (r + dp(4)) * Math.cos(rad));
                    float y2 = (float)(cy + (r + dp(4)) * Math.sin(rad));
                    c.drawLine(x1, y1, x2, y2, p);
                }
            }
        };
        iconRing.addView(hudRing, new FrameLayout.LayoutParams(dp(154), dp(154), Gravity.CENTER));
        ObjectAnimator rot = ObjectAnimator.ofFloat(hudRing, "rotation", 0f, 360f);
        rot.setDuration(2800);
        rot.setRepeatCount(ValueAnimator.INFINITE);
        rot.setInterpolator(new LinearInterpolator());
        rot.start();

        // 1b. Inner Tech Gear Ring (Counter-Clockwise)
        View innerTechRing = new View(this) {
            final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas c) {
                super.onDraw(c);
                float cx = getWidth() / 2f, cy = getHeight() / 2f, r = getWidth() * 0.36f;
                p.setStyle(Paint.Style.STROKE);
                p.setColor(0x8800E5FF);
                p.setStrokeWidth(dp(1.2f));
                c.drawCircle(cx, cy, r, p);
                p.setStrokeWidth(dp(2.0f));
                for (int i = 0; i < 8; i++) {
                    double rad = Math.toRadians(i * 45);
                    float x1 = (float)(cx + (r - dp(4)) * Math.cos(rad));
                    float y1 = (float)(cy + (r - dp(4)) * Math.sin(rad));
                    float x2 = (float)(cx + (r + dp(4)) * Math.cos(rad));
                    float y2 = (float)(cy + (r + dp(4)) * Math.sin(rad));
                    c.drawLine(x1, y1, x2, y2, p);
                }
            }
        };
        iconRing.addView(innerTechRing, new FrameLayout.LayoutParams(dp(154), dp(154), Gravity.CENTER));
        ObjectAnimator innerRot = ObjectAnimator.ofFloat(innerTechRing, "rotation", 360f, 0f);
        innerRot.setDuration(1900);
        innerRot.setRepeatCount(ValueAnimator.INFINITE);
        innerRot.setInterpolator(new LinearInterpolator());
        innerRot.start();

        // 2. Shockwave pulse ripples
        View shockwave1 = new View(this);
        GradientDrawable swBg1 = new GradientDrawable();
        swBg1.setShape(GradientDrawable.OVAL);
        swBg1.setStroke(dp(2.5f), 0xFFFF1744);
        shockwave1.setBackground(swBg1);
        iconRing.addView(shockwave1, new FrameLayout.LayoutParams(dp(100), dp(100), Gravity.CENTER));
        shockwave1.animate().scaleX(1.8f).scaleY(1.8f).alpha(0f).setDuration(1200).setInterpolator(new DecelerateInterpolator()).start();

        View shockwave2 = new View(this);
        GradientDrawable swBg2 = new GradientDrawable();
        swBg2.setShape(GradientDrawable.OVAL);
        swBg2.setStroke(dp(1.8f), 0xFF00E5FF);
        shockwave2.setBackground(swBg2);
        iconRing.addView(shockwave2, new FrameLayout.LayoutParams(dp(100), dp(100), Gravity.CENTER));
        handler.postDelayed(() -> {
            if (warpDialog.isShowing()) {
                shockwave2.animate().scaleX(2.0f).scaleY(2.0f).alpha(0f).setDuration(1200).setInterpolator(new DecelerateInterpolator()).start();
            }
        }, 350);

        View glowRing = new View(this);
        GradientDrawable ringBg = new GradientDrawable();
        ringBg.setShape(GradientDrawable.OVAL);
        ringBg.setColor(0x33FF1744);
        ringBg.setStroke(dp(2.5f), 0xFFFF1744);
        glowRing.setBackground(ringBg);
        iconRing.addView(glowRing, new FrameLayout.LayoutParams(dp(126), dp(126), Gravity.CENTER));

        ImageView iconView = new ImageView(this);
        if (entry.resolve != null) {
            try {
                iconView.setImageDrawable(entry.resolve.loadIcon(getPackageManager()));
            } catch (Exception ignored) {}
        }
        iconRing.addView(iconView, new FrameLayout.LayoutParams(dp(76), dp(76), Gravity.CENTER));
        box.addView(iconRing, new LinearLayout.LayoutParams(dp(160), dp(160)));

        TextView title = new TextView(this);
        title.setText(entry.label != null  ?  entry.label : "GAME");
        title.setTextSize(21);
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setLetterSpacing(.08f);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(12), 0, dp(4));
        box.addView(title);

        TextView sub = new TextView(this);
        sub.setText("REVROR HYPER CORE  /  COMBAT LAUNCH SEQUENCE");
        sub.setTextSize(10.5f);
        sub.setTextColor(0xFFFF3B48);
        sub.setLetterSpacing(0.12f);
        sub.setGravity(Gravity.CENTER);
        box.addView(sub);

        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setIndeterminate(false);
        bar.setMax(100);
        bar.setProgress(20);
        bar.getProgressDrawable().setColorFilter(0xFFFF1744, android.graphics.PorterDuff.Mode.SRC_IN);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(dp(260), dp(5));
        barLp.setMargins(0, dp(16), 0, dp(10));
        bar.setLayoutParams(barLp);
        box.addView(bar);

        TextView telemetryStep = new TextView(this);
        telemetryStep.setText("[ 1/4 ]  ENGAGING TITAN GAMING HARDWARE OPTIMIZATIONS");
        telemetryStep.setTextSize(10);
        telemetryStep.setTextColor(0xFF8E95AF);
        telemetryStep.setGravity(Gravity.CENTER);
        box.addView(telemetryStep);

        FrameLayout.LayoutParams boxLp = new FrameLayout.LayoutParams(dp(324), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        root.addView(box, boxLp);
        warpDialog.setContentView(root);
        try {
            if (!isFinishing() && !isDestroyed()) {
                warpDialog.show();
            }
        } catch (Exception ignored) {}

        glowRing.setScaleX(1.0f);
        glowRing.setScaleY(1.0f);
        glowRing.setAlpha(0.2f);
        glowRing.animate()
                .scaleX(1.4f)
                .scaleY(1.4f)
                .rotation(90f)
                .alpha(0.4f)
                .setDuration(700)
                .withEndAction(new Runnable() {
                    @Override public void run() {
                        if (warpDialog.isShowing()) {
                            glowRing.animate()
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .alpha(0.15f)
                                    .setDuration(700)
                                    .withEndAction(new Runnable() {
                                        @Override public void run() {
                                            if (warpDialog.isShowing()) {
                                                glowRing.animate()
                                                        .scaleX(1.4f)
                                                        .scaleY(1.4f)
                                                        .alpha(0.35f)
                                                        .setDuration(700)
                                                        .withEndAction(this)
                                                        .start();
                                            }
                                        }
                                    }).start();
                        }
                    }
                }).start();

        // Icon breath animation
        iconView.setScaleX(0.85f);
        iconView.setScaleY(0.85f);
        iconView.animate()
                .scaleX(1.10f)
                .scaleY(1.10f)
                .setDuration(600)
                .setInterpolator(new OvershootInterpolator(1.5f))
                .start();

        // Dynamic Telemetry Steps Progression
        handler.postDelayed(() -> {
            if (warpDialog.isShowing()) {
                telemetryStep.setText("[ 2/4 ]  SETTING CPU & GPU GOVERNORS TO MAXIMUM GAMING CLOCK");
                bar.setProgress(45);
            }
        }, 320);

        handler.postDelayed(() -> {
            if (warpDialog.isShowing()) {
                telemetryStep.setText("[ 3/4 ]  ENGAGING ULTRA LOW-LATENCY NETWORK & SHIZUKU SHIELD");
                bar.setProgress(75);
            }
        }, 650);

        setBusy(true, "Optimizing " + entry.label + "…");
        GsmProfileManager.apply(this, 4, null);
        long startTime = System.currentTimeMillis();

        GameOptimizer.optimize(this, entry.pkg, cleanupEnabled, networkEnabled, (privileged, detail) -> {
            setBusy(false, detail);

            long elapsed = System.currentTimeMillis() - startTime;
            long minWait = Math.max(0, 1450L - elapsed);

            handler.postDelayed(() -> {
                telemetryStep.setText("[ 4/4 ]  ALL SYSTEMS OPTIMIZED • WARPING INTO GAME");
                telemetryStep.setTextColor(0xFF00E5FF);
                bar.setProgress(100);

                hudRing.animate().scaleX(2.6f).scaleY(2.6f).alpha(0f).setDuration(400).start();
                iconView.animate()
                        .scaleX(2.2f)
                        .scaleY(2.2f)
                        .alpha(0f)
                        .setDuration(350)
                        .withEndAction(() -> {
                            rot.cancel();
                            innerRot.cancel();
                            scanAnimator.cancel();
                            try {
                                if (warpDialog.isShowing()) warpDialog.dismiss();
                            } catch (Exception ignored) {}

                            if (bubbleEnabled) {
                                Intent bubble = new Intent(GameLauncherActivity.this, GameBubbleService.class)
                                        .putExtra("package", entry.pkg)
                                        .putExtra("label", entry.label);
                                if (Build.VERSION.SDK_INT >= 26) {
                                    startForegroundService(bubble);
                                } else {
                                    startService(bubble);
                                }
                            }

                            Intent game = getPackageManager().getLaunchIntentForPackage(entry.pkg);
                            if (game != null) {
                                game.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                                startActivity(game);
                            } else {
                                Toast.makeText(GameLauncherActivity.this, "Unable to open " + entry.label, Toast.LENGTH_LONG).show();
                            }
                        }).start();
            }, minWait);
        });
    }

    private void showConsoleSettingsDialog() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(16));
        content.setBackgroundColor(0xFF0C1024);

        SwitchMaterial cleanupSw = option("Stop background user apps",
                "Frees RAM & CPU for game performance while protecting system apps", cleanupEnabled);
        SwitchMaterial networkSw = option("Network low-latency boost",
                "Optimizes socket controls & disables background data saver throttling", networkEnabled);
        SwitchMaterial bubbleSw = option("Floating in-game controls",
                "Draggable in-game console bubble for live FPS, RAM cleanup & profiles", bubbleEnabled);

        content.addView(cleanupSw);
        content.addView(networkSw);
        content.addView(bubbleSw);

        // Orientation selector button
        Button orientBtn = new Button(this);
        orientBtn.setText("Orientation: " + ORIENTATION_NAMES[orientationMode == 1  ?  1 : 0]);
        orientBtn.setTextSize(11);
        orientBtn.setTextColor(Color.WHITE);
        orientBtn.setTypeface(null, Typeface.BOLD);
        GradientDrawable oBg = new GradientDrawable();
        oBg.setColor(0xFF1E284A);
        oBg.setCornerRadius(dp(10));
        oBg.setStroke(dp(1), 0xFF354477);
        orientBtn.setBackground(oBg);
        orientBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Select Screen Orientation")
                    .setSingleChoiceItems(ORIENTATION_NAMES, orientationMode == 1  ?  1 : 0, (d, which) -> {
                        orientationMode = which;
                        orientBtn.setText("Orientation: " + ORIENTATION_NAMES[which]);
                        d.dismiss();
                    }).show();
        });
        LinearLayout.LayoutParams oLp = new LinearLayout.LayoutParams(-1, dp(40));
        oLp.setMargins(0, dp(10), 0, 0);
        content.addView(orientBtn, oLp);

        // Theme selector button
        Button themeBtn = new Button(this);
        themeBtn.setText("Theme: " + THEME_NAMES[themeIndex % THEME_NAMES.length]);
        themeBtn.setTextSize(11);
        themeBtn.setTextColor(Color.WHITE);
        themeBtn.setTypeface(null, Typeface.BOLD);
        GradientDrawable tBg = new GradientDrawable();
        tBg.setColor(0xFF1E284A);
        tBg.setCornerRadius(dp(10));
        tBg.setStroke(dp(1), 0xFF354477);
        themeBtn.setBackground(tBg);
        themeBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Select Console Theme")
                    .setSingleChoiceItems(THEME_NAMES, themeIndex % THEME_NAMES.length, (d, which) -> {
                        themeIndex = which;
                        themeBtn.setText("Theme: " + THEME_NAMES[which]);
                        d.dismiss();
                    }).show();
        });
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(-1, dp(40));
        tLp.setMargins(0, dp(8), 0, 0);
        content.addView(themeBtn, tLp);

        // Layout selector button
        Button layoutBtn = new Button(this);
        layoutBtn.setText("Layout: " + LAYOUT_NAMES[layoutIndex % LAYOUT_NAMES.length]);
        layoutBtn.setTextSize(11);
        layoutBtn.setTextColor(Color.WHITE);
        layoutBtn.setTypeface(null, Typeface.BOLD);
        GradientDrawable lBg = new GradientDrawable();
        lBg.setColor(0xFF1E284A);
        lBg.setCornerRadius(dp(10));
        lBg.setStroke(dp(1), 0xFF354477);
        layoutBtn.setBackground(lBg);
        layoutBtn.setOnClickListener(v -> {
            showLayoutChooser();
        });
        LinearLayout.LayoutParams lLp = new LinearLayout.LayoutParams(-1, dp(40));
        lLp.setMargins(0, dp(8), 0, 0);
        content.addView(layoutBtn, lLp);

        Button protectedBtn = new Button(this);
        protectedBtn.setText("MANAGE PROTECTED APPS");
        protectedBtn.setTextSize(11);
        protectedBtn.setTextColor(Color.WHITE);
        protectedBtn.setTypeface(null, Typeface.BOLD);
        GradientDrawable pBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF4361EE, 0xFF3F37C9});
        pBg.setCornerRadius(dp(12));
        protectedBtn.setBackground(pBg);
        protectedBtn.setOnClickListener(v -> showProtectedApps());

        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(-1, dp(42));
        pLp.setMargins(0, dp(12), 0, 0);
        content.addView(protectedBtn, pLp);

        Button gamesManageBtn = new Button(this);
        gamesManageBtn.setText("MANAGE REVROR GAMES LIST");
        gamesManageBtn.setTextSize(11);
        gamesManageBtn.setTextColor(Color.WHITE);
        gamesManageBtn.setTypeface(null, Typeface.BOLD);
        GradientDrawable gBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF0077B6, 0xFF00B4D8});
        gBg.setCornerRadius(dp(12));
        gamesManageBtn.setBackground(gBg);
        gamesManageBtn.setOnClickListener(v -> showManageGamesDialog());

        LinearLayout.LayoutParams gLp = new LinearLayout.LayoutParams(-1, dp(42));
        gLp.setMargins(0, dp(10), 0, 0);
        content.addView(gamesManageBtn, gLp);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("CONSOLE PERFORMANCE CONTROLS")
                .setView(content)
                .setPositiveButton("SAVE & APPLY", (d, which) -> {
                    cleanupEnabled = cleanupSw.isChecked();
                    networkEnabled = networkSw.isChecked();
                    bubbleEnabled = bubbleSw.isChecked();
                    savePreferences();
                    Toast.makeText(this, "Console settings updated", Toast.LENGTH_SHORT).show();
                    if (orientationMode == 1) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    } else {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    }
                    rebuildView();
                })
                .setNegativeButton("CANCEL", null)
                .create();

        try {
            if (!isFinishing() && !isDestroyed()) {
                dialog.show();
            }
        } catch (Exception ignored) {}
    }

    private void showProtectedApps() {
        List<GameEntry> candidates = new ArrayList<>();
        for (GameEntry e : allEntries) {
            try {
                ApplicationInfo ai = getPackageManager().getApplicationInfo(e.pkg, 0);
                if ((ai.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) == 0) {
                    candidates.add(e);
                }
            } catch (Exception ignored) {}
        }

        CharSequence[] names = new CharSequence[candidates.size()];
        boolean[] checked = new boolean[candidates.size()];
        Set<String> selected = GameOptimizer.protectedPackages(this);

        for (int i = 0; i < candidates.size(); i++) {
            names[i] = candidates.get(i).label;
            checked[i] = selected.contains(candidates.get(i).pkg);
        }

        new AlertDialog.Builder(this)
                .setTitle("Protected From Game Cleanup")
                .setMultiChoiceItems(names, checked, (d, which, yes) -> {
                    if (yes) selected.add(candidates.get(which).pkg);
                    else selected.remove(candidates.get(which).pkg);
                })
                .setPositiveButton("Save", (d, w) -> GameOptimizer.saveProtectedPackages(this, selected))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showManageGamesDialog() {
        PackageManager pm = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(mainIntent, 0);
        List<ResolveInfo> apps = new ArrayList<>();
        for (ResolveInfo ri : list) {
            if (ri.activityInfo != null && !getPackageName().equals(ri.activityInfo.packageName)) apps.add(ri);
        }
        Collections.sort(apps, (a, b) -> a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString()));
        Set<String> selected = new HashSet<>(com.revror.kernelmanager.utils.GameLauncherRegistry.getGamePackages(this));

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(18), dp(14), dp(18), dp(14));
        GradientDrawable shellBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF1A1A1F, 0xFF08080A, 0xFF28090F});
        shellBg.setCornerRadius(dp(18));
        shellBg.setStroke(dp(1), 0xFFFF4350);
        shell.setBackground(shellBg);
        TextView title = label("GAME LIBRARY ARMORY", 16, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setLetterSpacing(.08f);
        shell.addView(title);
        TextView sub = label("Tap app cards to add or remove them from the REVROR Game Lobby", 9, 0xFFFF6973);
        sub.setPadding(0, dp(3), 0, dp(10));
        shell.addView(sub);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(5);
        for (int i = 0; i < apps.size(); i++) {
            ResolveInfo ri = apps.get(i);
            String pkg = ri.activityInfo.packageName;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setPadding(dp(7), dp(8), dp(7), dp(6));
            ImageView icon = new ImageView(this);
            icon.setImageDrawable(ri.loadIcon(pm));
            card.addView(icon, new LinearLayout.LayoutParams(dp(46), dp(46)));
            TextView name = label(ri.loadLabel(pm).toString(), 8.5f, Color.WHITE);
            name.setTypeface(null, Typeface.BOLD);
            name.setGravity(Gravity.CENTER);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            name.setPadding(0, dp(5), 0, 0);
            card.addView(name, new LinearLayout.LayoutParams(-1, -2));
            TextView state = label("", 7, 0xFFFF6570);
            state.setTypeface(null, Typeface.BOLD);
            state.setGravity(Gravity.CENTER);
            card.addView(state);
            setManageCardState(card, state, selected.contains(pkg));
            card.setOnClickListener(v -> {
                boolean enabled;
                if (selected.contains(pkg)) { selected.remove(pkg); enabled = false; }
                else { selected.add(pkg); enabled = true; }
                setManageCardState(card, state, enabled);
                card.animate().scaleX(.94f).scaleY(.94f).setDuration(70)
                        .withEndAction(() -> card.animate().scaleX(1f).scaleY(1f).setDuration(130).start()).start();
            });
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = dp(102);
            lp.columnSpec = GridLayout.spec(i % 5, 1, 1f);
            lp.rowSpec = GridLayout.spec(i / 5);
            lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            grid.addView(card, lp);
        }
        scroll.addView(grid, new ScrollView.LayoutParams(-1, -2));
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(10), 0, 0);
        TextView cancel = redHeaderButton("CANCEL", R.drawable.ic_cancel);
        TextView save = redHeaderButton("SAVE LIBRARY", R.drawable.ic_save);
        GradientDrawable saveBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFFD81024, 0xFFFF3548});
        saveBg.setCornerRadius(dp(9));
        save.setBackground(saveBg);
        actions.addView(cancel, new LinearLayout.LayoutParams(0, dp(42), 1f));
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        saveLp.setMarginStart(dp(8));
        actions.addView(save, saveLp);
        shell.addView(actions);

        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(shell);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * .88f);
            lp.height = (int) (getResources().getDisplayMetrics().heightPixels * .88f);
            lp.gravity = Gravity.CENTER;
            window.setAttributes(lp);
        }
        cancel.setOnClickListener(v -> dialog.dismiss());
        save.setOnClickListener(v -> {
            for (ResolveInfo ri : apps) {
                String pkg = ri.activityInfo.packageName;
                String appName = ri.loadLabel(pm).toString();
                if (selected.contains(pkg)) com.revror.kernelmanager.utils.GameLauncherRegistry.registerGame(this, pkg, appName);
                else com.revror.kernelmanager.utils.GameLauncherRegistry.unregisterGame(this, pkg);
            }
            dialog.dismiss();
            Toast.makeText(this, "Game library updated", Toast.LENGTH_SHORT).show();
            loadApps();
        });
        try {
            if (!isFinishing() && !isDestroyed()) {
                dialog.show();
            }
        } catch (Exception ignored) {}
    }

    private void showBarrageSettingsDialog(@Nullable SuperTileHolder holder) {
        SharedPreferences plugins = getSharedPreferences("ror_game_plugins", MODE_PRIVATE);
        int currentStyle = Math.max(0, Math.min(BARRAGE_STYLE_NAMES.length - 1, plugins.getInt("barrage_style", 0)));
        boolean isEnabled = plugins.getBoolean("barrage", true);
        int currentSpeed = plugins.getInt("barrage_speed", 1);
        int currentPos = plugins.getInt("barrage_pos", 0);

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(18), dp(16), dp(18), dp(16));
        GradientDrawable shellBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF18181D, 0xFF0A0A0C, 0xFF22080E});
        shellBg.setCornerRadius(dp(18));
        shellBg.setStroke(dp(1), 0xFFFF4350);
        shell.setBackground(shellBg);

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        TextView title = label("BARRAGE MESSAGE", 15, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setLetterSpacing(.08f);
        titleCol.addView(title);
        TextView sub = label("REVROR bullet comment notifications while gaming", 8.5f, 0xFFFF7B85);
        titleCol.addView(sub);
        headerRow.addView(titleCol, new LinearLayout.LayoutParams(0, -2, 1f));

        SwitchMaterial toggle = new SwitchMaterial(this);
        toggle.setChecked(isEnabled);
        toggle.setOnCheckedChangeListener((btn, checked) -> {
            plugins.edit().putBoolean("barrage", checked).apply();
            if (holder != null) updateSuperTileState(holder, checked, null);
        });
        headerRow.addView(toggle, new LinearLayout.LayoutParams(-2, -2));
        shell.addView(headerRow);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(10), 0, dp(6));

        TextView styleTitle = label("BUBBLE STYLE", 10.5f, 0xFFFF505E);
        styleTitle.setTypeface(null, Typeface.BOLD);
        styleTitle.setPadding(0, dp(8), 0, dp(6));
        content.addView(styleTitle);

        LinearLayout stylesRow = new LinearLayout(this);
        stylesRow.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout[] styleCards = new LinearLayout[4];
        int[] curStyleBox = new int[]{currentStyle};

        Runnable refreshStyleSelection = () -> {
            for (int i = 0; i < 4; i++) {
                if (styleCards[i] == null) continue;
                boolean sel = (i == curStyleBox[0]);
                GradientDrawable b = new GradientDrawable();
                b.setCornerRadius(dp(12));
                if (sel) {
                    b.setColor(0x40FF2A40);
                    b.setStroke(dp(2), 0xFFFF3B48);
                } else {
                    b.setColor(0x221E1E24);
                    b.setStroke(dp(1), 0xFF44444D);
                }
                styleCards[i].setBackground(b);
            }
        };

        for (int i = 0; i < 4; i++) {
            final int sIdx = i;
            LinearLayout sCard = new LinearLayout(this);
            sCard.setOrientation(LinearLayout.VERTICAL);
            sCard.setGravity(Gravity.CENTER);
            sCard.setPadding(dp(8), dp(10), dp(8), dp(10));

            ImageView prevIcon = new ImageView(this);
            if (i == 1) prevIcon.setImageResource(R.drawable.ic_barrage_chibi);
            else if (i == 3) prevIcon.setImageResource(R.drawable.ic_barrage_cocktail);
            else if (i == 2) {
                prevIcon.setImageResource(R.drawable.ic_barrage_message);
                prevIcon.setColorFilter(0xFF00E5FF);
            } else {
                prevIcon.setImageResource(R.drawable.ic_barrage_message);
                prevIcon.setColorFilter(Color.WHITE);
            }
            sCard.addView(prevIcon, new LinearLayout.LayoutParams(dp(26), dp(26)));

            TextView sLabel = label(BARRAGE_STYLE_NAMES[i], 9f, Color.WHITE);
            sLabel.setTypeface(null, Typeface.BOLD);
            sLabel.setPadding(0, dp(5), 0, 0);
            sCard.addView(sLabel);

            sCard.setOnClickListener(v -> {
                curStyleBox[0] = sIdx;
                plugins.edit().putInt("barrage_style", sIdx).apply();
                if (holder != null) updateSuperTileState(holder, true, BARRAGE_STYLE_NAMES[sIdx]);
                refreshStyleSelection.run();

                GameBubbleService bubble = GameBubbleService.getInstance();
                if (bubble != null) {
                    bubble.showBarrage(BARRAGE_STYLE_NAMES[sIdx], "Style selected • Bullet comment sliding across screen");
                }
            });

            styleCards[i] = sCard;
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
            p.setMargins(dp(3), 0, dp(3), 0);
            stylesRow.addView(sCard, p);
        }
        refreshStyleSelection.run();
        content.addView(stylesRow);

        TextView velTitle = label("BARRAGE VELOCITY", 10.5f, 0xFFFF505E);
        velTitle.setTypeface(null, Typeface.BOLD);
        velTitle.setPadding(0, dp(14), 0, dp(6));
        content.addView(velTitle);

        LinearLayout velRow = new LinearLayout(this);
        velRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] speedLabels = {"Slow (7.5s)", "In (5.2s)", "Fast (3.4s)"};
        Button[] speedBtns = new Button[3];
        int[] curSpeedBox = new int[]{currentSpeed};

        Runnable refreshSpeedBtns = () -> {
            for (int i = 0; i < 3; i++) {
                boolean active = (i == curSpeedBox[0]);
                GradientDrawable b = new GradientDrawable();
                b.setCornerRadius(dp(8));
                b.setColor(active  ?  0xFFFF3346 : 0xFF242429);
                speedBtns[i].setBackground(b);
                speedBtns[i].setTextColor(active  ?  Color.WHITE : 0xFFB0B0B8);
            }
        };

        for (int i = 0; i < 3; i++) {
            final int s = i;
            Button b = new Button(this);
            b.setText(speedLabels[i]);
            b.setTextSize(9f);
            b.setAllCaps(false);
            b.setOnClickListener(v -> {
                curSpeedBox[0] = s;
                plugins.edit().putInt("barrage_speed", s).apply();
                refreshSpeedBtns.run();
            });
            speedBtns[i] = b;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(36), 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            velRow.addView(b, lp);
        }
        refreshSpeedBtns.run();
        content.addView(velRow);

        TextView posTitle = label("BULLET COMMENTS POSITION", 10.5f, 0xFFFF505E);
        posTitle.setTypeface(null, Typeface.BOLD);
        posTitle.setPadding(0, dp(14), 0, dp(6));
        content.addView(posTitle);

        LinearLayout posRow = new LinearLayout(this);
        posRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] posLabels = {"High (Top 24dp)", "Low (Top 68dp)"};
        Button[] posBtns = new Button[2];
        int[] curPosBox = new int[]{currentPos};

        Runnable refreshPosBtns = () -> {
            for (int i = 0; i < 2; i++) {
                boolean active = (i == curPosBox[0]);
                GradientDrawable b = new GradientDrawable();
                b.setCornerRadius(dp(8));
                b.setColor(active  ?  0xFFFF3346 : 0xFF242429);
                posBtns[i].setBackground(b);
                posBtns[i].setTextColor(active  ?  Color.WHITE : 0xFFB0B0B8);
            }
        };

        for (int i = 0; i < 2; i++) {
            final int p = i;
            Button b = new Button(this);
            b.setText(posLabels[i]);
            b.setTextSize(9f);
            b.setAllCaps(false);
            b.setOnClickListener(v -> {
                curPosBox[0] = p;
                plugins.edit().putInt("barrage_pos", p).apply();
                refreshPosBtns.run();
            });
            posBtns[i] = b;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(36), 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            posRow.addView(b, lp);
        }
        refreshPosBtns.run();
        content.addView(posRow);

        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setPadding(0, dp(10), 0, 0);

        TextView testBtn = redHeaderButton("TEST SLIDING BARRAGE", R.drawable.ic_barrage_message);
        GradientDrawable testBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFFD81024, 0xFFFF3548});
        testBg.setCornerRadius(dp(9));
        testBtn.setBackground(testBg);

        TextView closeBtn = redHeaderButton("DONE", R.drawable.ic_save);

        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(shell);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * .85f);
            lp.height = (int) (getResources().getDisplayMetrics().heightPixels * .85f);
            lp.gravity = Gravity.CENTER;
            window.setAttributes(lp);
        }

        testBtn.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            plugins.edit().putBoolean("barrage", true).apply();
            if (holder != null) updateSuperTileState(holder, true, BARRAGE_STYLE_NAMES[currentStyle]);
            Intent testIntent = new Intent(this, GameBubbleService.class);
            testIntent.setAction(GameBubbleService.ACTION_TEST_BARRAGE);
            testIntent.putExtra("title", "REVROR Danmaku");
            testIntent.putExtra("message", "Sliding bullet comment preview across screen!");
            testIntent.putExtra("style", currentStyle);
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(testIntent);
            } else {
                startService(testIntent);
            }
            dialog.dismiss();
            Toast.makeText(this, "Testing " + BARRAGE_STYLE_NAMES[currentStyle] + " Danmaku", Toast.LENGTH_SHORT).show();
        });

        closeBtn.setOnClickListener(v -> dialog.dismiss());

        footer.addView(testBtn, new LinearLayout.LayoutParams(0, dp(42), 1.2f));
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(0, dp(42), 0.8f);
        closeLp.setMarginStart(dp(8));
        footer.addView(closeBtn, closeLp);

        shell.addView(footer);
        try {
            if (!isFinishing() && !isDestroyed()) {
                dialog.show();
            }
        } catch (Exception ignored) {}
    }

    private void setManageCardState(LinearLayout card, TextView state, boolean selected) {
        state.setText(selected  ?  "IN LOBBY" : "ADD");
        state.setTextColor(selected  ?  0xFFFF6570 : 0xFF85858D);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                selected  ?  new int[]{0xFF441019, 0xFF1B171A} : new int[]{0xFF242429, 0xFF141417});
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(selected  ?  2 : 1), selected  ?  0xFFFF4350 : 0xFF44444B);
        card.setBackground(bg);
    }

    private void updateHud() {
        // CPU
        int cpu = cpuStats.readPercent(GsmProfileManager.isRootEnabled(this));
        String cpuStr = "CPU " + (cpu < 0 ? "--" : cpu + "%");
        if (hudCpu != null) hudCpu.setText(cpuStr);
        for (TextView tv : customTelemetryCpuViews) {
            if (tv != null) tv.setText(cpuStr);
        }

        // RAM
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (am != null) am.getMemoryInfo(mi);
        int ram = mi.totalMem <= 0 ? 0 : Math.round((mi.totalMem - mi.availMem) * 100f / mi.totalMem);
        String ramStr = "RAM " + ram + "%";
        if (hudRam != null) hudRam.setText(ramStr);
        for (TextView tv : customTelemetryRamViews) {
            if (tv != null) tv.setText(ramStr);
        }

        // Battery & Temperature
        Intent b = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        float temp = b == null ? 0 : b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f;
        int level = b == null ? 0 : b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        String tempStr = String.format(Locale.US, "%.0f\u00B0C", temp);
        String batStr = level >= 0 ? "BAT " + level + "%" : "BAT OK";
        if (hudTemp != null) hudTemp.setText(tempStr);
        if (hudGpu != null) hudGpu.setText(batStr);
        for (TextView tv : customTelemetryTempViews) {
            if (tv != null) tv.setText(tempStr);
        }
        for (TextView tv : customTelemetryBatViews) {
            if (tv != null) tv.setText(batStr);
        }

        // Display Refresh Rate / FPS Target
        int currentFps = 60;
        try {
            currentFps = (int) Math.round(getWindowManager().getDefaultDisplay().getRefreshRate());
        } catch (Exception ignored) {}
        String fpsStr = currentFps + " FPS";
        for (TextView tv : customTelemetryFpsViews) {
            if (tv != null) tv.setText(fpsStr);
        }

        // Clock
        String clockStr = clockFormat.format(new Date());
        if (hudClock != null) hudClock.setText(clockStr);
        for (TextView tv : customTelemetryClockViews) {
            if (tv != null) tv.setText(clockStr);
        }

        // Telemetry Suite
        if (telemetryCpuText != null) telemetryCpuText.setText(cpuStr + "  |  OCTA-CORE LOAD");
        if (telemetryCpuBar != null && cpu >= 0) telemetryCpuBar.setProgress(cpu);
        if (telemetryRamText != null) telemetryRamText.setText(ramStr + "  |  " + (100 - ram) + "% FREE");
        if (telemetryRamBar != null) telemetryRamBar.setProgress(ram);
        if (telemetryTempText != null) telemetryTempText.setText(String.format(Locale.US, "TEMP %.0f\u00B0C\nBAT %d%%", temp, Math.max(0, level)));
        if (telemetryPingText != null) telemetryPingText.setText("PING " + (currentPingMs > 0 ? currentPingMs + "ms" : "22ms") + "\nLOW LATENCY");
    }

    private void applyImmersive() {
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    private void setBusy(boolean busy, String message) {
        if (progressBar != null) progressBar.setVisibility(busy  ?  View.VISIBLE : View.GONE);
        if (statusText != null) statusText.setText(message);
    }

    private SwitchMaterial option(String title, String subtitle, boolean value) {
        SwitchMaterial s = new SwitchMaterial(this);
        s.setText(title + "\n" + subtitle);
        s.setTextColor(Color.WHITE);
        s.setTextSize(12);
        s.setChecked(value);
        s.setPadding(dp(4), dp(8), dp(4), dp(8));
        return s;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static final class GameEntry {
        final String label, pkg;
        final boolean game;
        final ResolveInfo resolve;

        GameEntry(String label, String pkg, boolean game, ResolveInfo resolve) {
            this.label = label;
            this.pkg = pkg;
            this.game = game;
            this.resolve = resolve;
        }
    }
}

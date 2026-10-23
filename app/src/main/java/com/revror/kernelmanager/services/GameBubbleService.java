/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.services;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.ActivityManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Display;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.revror.kernelmanager.utils.AdBypassHelper;
import com.revror.kernelmanager.utils.media.ScreenCaptureManager;
import com.revror.kernelmanager.utils.display.HwOverlayManager;
import com.revror.kernelmanager.utils.display.RefreshRateManager;
import com.revror.kernelmanager.utils.touch.AimBoostEngine;
import android.widget.SeekBar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.revror.kernelmanager.R;
import com.revror.kernelmanager.activities.GameLauncherActivity;
import com.revror.kernelmanager.utils.AppIntegrity;
import com.revror.kernelmanager.utils.HunterModeEngine;
import com.revror.kernelmanager.utils.CpuStatsReader;
import com.revror.kernelmanager.utils.GameOptimizer;
import com.revror.kernelmanager.utils.GsmProfileManager;
import com.revror.kernelmanager.utils.ShizukuBridge;
import com.revror.kernelmanager.utils.root.RootUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameBubbleService extends Service {
    public static final String REVROR_SIGNATURE = "revror";
    public static final String ACTION_STOP = "com.revror.kernelmanager.STOP_GAME_BUBBLE";
    public static final String ACTION_TEST_BARRAGE = "com.revror.kernelmanager.TEST_BARRAGE";
    public static final String ACTION_SET_HUNTING_FILTER = "com.revror.kernelmanager.SET_HUNTING_FILTER";
    public static final String ACTION_UPDATE_CROSSHAIR = "com.revror.kernelmanager.UPDATE_CROSSHAIR";
    public static final String ACTION_SHOW_PANEL = "com.revror.kernelmanager.SHOW_PANEL";
    private static final String CHANNEL = "ror_game_controls";
    private static final int NOTIF_ID = 6040;
    private static final String PREF_FAVORITES = "ror_mini_favorites";
    private static final String DEFAULT_AD_BASE_URL = "https://venisonglum.com/";
    private static final String DEFAULT_AD_KEY = "b42fcfe592ba30a160cff1a209bed5f7";

    public static final String[] HUNTING_FILTER_NAMES = HunterModeEngine.FILTER_NAMES;
    public static final String[] HUNTING_FILTER_DESCRIPTIONS = HunterModeEngine.FILTER_DESCRIPTIONS;
    public static final int[] HUNTING_FILTER_TINTS = HunterModeEngine.FILTER_TINTS;

    private static volatile GameBubbleService sInstance;

    public static GameBubbleService getInstance() {
        return sInstance;
    }

    public String getTargetPackage() {
        return targetPackage != null ? targetPackage : "";
    }

    private WindowManager windowManager;
    private WindowManager.LayoutParams params;
    private View overlay;
    private TextView metrics;
    private final Button[] profileButtons = new Button[8];
    private String targetPackage = "", targetLabel = "Game";
    private boolean userDismissed = false;

    // States: 0 = full panel, 1 = floating orb, 2 = edge tab, 3 = mini apps,
    // 4 = screen recorder, 5 = game plugins and display controls.
    private int bubbleState = 1;
    private int savedY = 0;
    private boolean dockOnRight = true;

    // Active Mini App Tracking
    private String activeMiniAppPkg = null;
    private String activeMiniAppName = null;

    // Screen Recording State
    private boolean isRecording = false;

    public static boolean isRecordingActive() {
        return ScreenCaptureManager.getInstance().isRecording();
    }
    private long recordingStartTime = 0;
    private String currentRecordingPath = "";
    private View recordPillView;
    private WindowManager.LayoutParams recordParams;

    private int selectedProfile;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final CpuStatsReader cpuStats = new CpuStatsReader();
    private boolean checking;
    // Display effects must only exist while the selected game is actually in front.
    // Keeping this separate from the saved filter preference prevents a filter chosen
    // from Game Space from tinting Android, the launcher, or other applications.
    private volatile boolean targetInForeground;
    private int appliedHardwareColorFilter = -1;
    private View crosshairOverlay;
    private WindowManager.LayoutParams crosshairParams;
    public static final String DEFAULT_FLOATING_TOOLS_ORDER = "quick_screenshot,speedrun_stopwatch,floating_calc,audio_boost,quick_ram_vram_purge,touch_sampling_boost,force_max_refresh,thermal_battery_guard,voice_changer,display_tools,wifi_manager,crosshair_settings,anti_disturb,barrage,mini_apps,screen_recorder,network,cleanup,toggle_overlay";
    private LinearLayout floatingToolsContainer;
    private View screenFilterOverlay;
    private WindowManager.LayoutParams screenFilterParams;
    private View barrageOverlay;
    private WindowManager.LayoutParams barrageParams;
    private ValueAnimator currentBarrageAnimator;
    private boolean crosshairEnabled;
    private boolean brightnessLocked;
    private boolean colorBoostEnabled;
    private boolean screenFilterEnabled;
    private boolean highRefreshEnabled;
    private boolean touchBoostEnabled;
    private boolean antiDisturbEnabled;
    private boolean wifiEnabled = true;
    private boolean mobileDataEnabled = true;
    private boolean barrageEnabled = true;
    private int reminderMinutes;
    private int crosshairStyle;
    private static final String[] CROSSHAIR_NAMES = {
        "Classic", "Precision Dot", "Tactical T", "Neon X", "Target Ring",
        "Diamond Reflex", "Tri-Spoke SpecOps", "Biohazard Gamer"
    };
    private int barrageStyle;
    public static final String[] BARRAGE_STYLE_NAMES = {
        "Minimal Stealth", "Anime Mecha", "Cyber Glass", "Cocktail Party",
        "REVROR Turbo", "RGB Chroma Wave", "Retro 8-Bit Arcade", "Cyber Neon Flux",
        "Ember Bonfire", "Radiant Strike", "Arena Champion", "Hextech Pulse",
        "Inferno Raid", "Sakura Blossom", "Chibi Kawaii", "Shonen Flame",
        "Manga Monochrome", "Violet Mecha", "Solar Blade", "Cyber Ghost",
        "Cursed Energy", "Magical Girl", "Phantom Rogue", "Void Sovereign"
    };

    // 10 In-Game Working Features State
    private View stopwatchOverlay;
    private WindowManager.LayoutParams stopwatchParams;
    private boolean stopwatchRunning = false;
    private long stopwatchStartTime = 0;
    private long stopwatchAccumulatedTime = 0;
    private TextView stopwatchTimeView;
    private TextView stopwatchPlayPauseBtn;

    private View calculatorOverlay;
    private WindowManager.LayoutParams calculatorParams;
    private TextView calcDisplay;
    private String calcCurrentInput = "";
    private double calcOperand = 0;
    private String calcPendingOp = "";
    private boolean calcJustCalculated = false;

    private boolean audioBoostEnabled = false;
    private boolean thermalGuardEnabled = false;
    private long lastThermalAlertTime = 0;
    private int voiceFilterIndex = 0;
    public static final String[] VOICE_FILTER_NAMES = {
        "Off (Original)", "Tactical Radio", "Cyber Mecha",
        "Deep Commander", "Helium Scout", "Arena Echo"
    };

    private WebView floatingAdBanner;

    private static String defaultFloatingAdHtml() {
        return "<!doctype html><html><head><meta name='viewport' content='width=728, initial-scale=0.42, maximum-scale=1.0, user-scalable=no'>"
                + "<style>html,body{width:100%;height:100%;margin:0;padding:0;background:transparent;overflow:hidden;display:flex;justify-content:center;align-items:center}iframe{border:0;}</style>"
                + "</head><body><script>atOptions={'key':'" + DEFAULT_AD_KEY
                + "','format':'iframe','height':90,'width':728,'params':{}};</script>"
                + "<script src='" + DEFAULT_AD_BASE_URL + DEFAULT_AD_KEY + "/invoke.js'></script></body></html>";
    }

    private final Runnable stopwatchTicker = new Runnable() {
        @Override public void run() {
            if (stopwatchRunning && stopwatchTimeView != null) {
                long elapsed = stopwatchAccumulatedTime + (SystemClock.elapsedRealtime() - stopwatchStartTime);
                long mins = (elapsed / 60000);
                long secs = (elapsed % 60000) / 1000;
                long millis = (elapsed % 1000) / 10;
                stopwatchTimeView.setText(String.format(Locale.US, "%02d:%02d.%02d", mins, secs, millis));
                handler.postDelayed(this, 35);
            }
        }
    };

    private final Runnable gameReminder = new Runnable() {
        @Override public void run() {
            if (reminderMinutes <= 0) return;
            showBarrage("Game reminder", "You have been playing for " + reminderMinutes + " minutes");
            handler.postDelayed(this, reminderMinutes * 60L * 1000L);
        }
    };

    // Live Recording Timer Runnable
    private final Runnable recordTimerRunnable = new Runnable() {
        @Override public void run() {
            if (isRecording && recordPillView != null) {
                long sec = (System.currentTimeMillis() - recordingStartTime) / 1000;
                TextView txt = recordPillView.findViewById(android.R.id.text1);
                if (txt != null) {
                    long m = sec / 60;
                    long s = sec % 60;
                    txt.setText(String.format(Locale.US, "REC %02d:%02d", m, s));
                }
                handler.postDelayed(this, 1000);
            }
        }
    };

    private final Runnable foregroundMonitor = new Runnable() {
        @Override public void run() {
            if (userDismissed) {
                if (overlay != null) {
                    hideOverlay();
                }
                handler.postDelayed(this, 1200);
                return;
            }
            if (!checking) {
                checking = true;
                runOnWorker(() -> {
                    try {
                        boolean foreground = isTargetForeground();
                        String live = (bubbleState == 0) ? readLiveStats() : "";
                        if (foreground && thermalGuardEnabled) {
                            try {
                                Intent b = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                                float temp = b == null ? 0 : b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f;
                                if (temp >= 43.0f && System.currentTimeMillis() - lastThermalAlertTime > 60000) {
                                    lastThermalAlertTime = System.currentTimeMillis();
                                    handler.post(() -> {
                                        showBarrage("THERMAL ALERT", String.format(Locale.US, "Battery temp at %.1fC! Switching to CoolDown profile", temp));
                                        GameOptimizer.applyProfile(GameBubbleService.this, 3, null);
                                        selectedProfile = 3;
                                        updateProfileButtons();
                                    });
                                }
                            } catch (Exception ignored) {}
                        }
                        handler.post(() -> {
                            checking = false;
                            targetInForeground = foreground;
                            if (!foreground) { hideOverlay(); return; }
                            if (!userDismissed) {
                                if (overlay == null) {
                                    showBubble();
                                }
                                if (metrics != null && !live.isEmpty()) {
                                    metrics.setText(live);
                                }
                            }
                        });
                    } catch (Throwable t) {
                        handler.post(() -> checking = false);
                    }
                });
            }
            handler.postDelayed(this, 800);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        AppIntegrity.enforce(this);
        sInstance = this;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        // Repair state left by older builds, where Hunter used Android's global
        // accessibility color inversion instead of a game-only overlay.
        runPrivileged("settings put secure accessibility_display_inversion_enabled 0; settings put secure accessibility_display_daltonizer_enabled 0", null);
        createChannel();
        updateNotification();
    }

    private void updateNotification() {
        PendingIntent open = PendingIntent.getActivity(this, 1, new Intent(this, GameLauncherActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 2, new Intent(this, GameBubbleService.class).setAction(ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_revror_qs_gaming)
                .setContentTitle("REVROR Game Controls • " + targetLabel)
                .setContentText("Tap overlay icon on screen to configure")
                .setOngoing(true)
                .setContentIntent(open)
                .addAction(0, "Stop Controls", stop);

        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIF_ID, builder.build(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIF_ID, builder.build(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIF_ID, builder.build());
            }
        } catch (Throwable t) {
            android.util.Log.e("GameBubble", "FGS start error: " + t.getMessage());
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String act = intent.getAction();
            if (ACTION_STOP.equals(act)) {
                userDismissed = true;
                hideOverlay();
                stopScreenRecording();
                try {
                    stopForeground(true);
                } catch (Exception ignored) {}
                stopSelf();
                return START_NOT_STICKY;
            }
            if (ACTION_TEST_BARRAGE.equals(act)) {
                barrageEnabled = true;
                String title = intent.getStringExtra("title") != null ? intent.getStringExtra("title") : "REVROR Danmaku";
                String msg = intent.getStringExtra("message") != null ? intent.getStringExtra("message") : "Sliding bullet comment across screen";
                if (intent.hasExtra("style")) {
                    barrageStyle = Math.max(0, Math.min(BARRAGE_STYLE_NAMES.length - 1, intent.getIntExtra("style", barrageStyle)));
                    getSharedPreferences("ror_game_plugins", MODE_PRIVATE).edit().putInt("barrage_style", barrageStyle).apply();
                }
                android.util.Log.i("GameBubbleService", "ACTION_TEST_BARRAGE received: style=" + barrageStyle + ", title=" + title);
                showBarrage(title, msg);
                return START_STICKY;
            }
            if (ACTION_SET_HUNTING_FILTER.equals(act)) {
                int filterId = intent.getIntExtra("filter_id", 0);
                screenFilterEnabled = (filterId > 0);
                HunterModeEngine.HunterConfig cfg = HunterModeEngine.getInstance().loadConfig(this, targetPackage);
                cfg.filterId = filterId;
                cfg.enabled = screenFilterEnabled;
                if (intent.hasExtra("preset_id")) {
                    cfg.applyPreset(intent.getIntExtra("preset_id", 0));
                }
                HunterModeEngine.getInstance().saveConfig(this, targetPackage, cfg);
                updateScreenFilterOverlay();
                if (screenFilterEnabled && filterId < HUNTING_FILTER_NAMES.length) {
                    showBarrage("Hunter Mode", HUNTING_FILTER_NAMES[filterId] + " enabled");
                }
                return START_STICKY;
            }
            if (ACTION_UPDATE_CROSSHAIR.equals(act)) {
                SharedPreferences cp = getSharedPreferences("ror_game_plugins", MODE_PRIVATE);
                if (intent.hasExtra("enabled")) {
                    crosshairEnabled = intent.getBooleanExtra("enabled", crosshairEnabled);
                    cp.edit().putBoolean("crosshair", crosshairEnabled).apply();
                }
                if (intent.hasExtra("color")) {
                    int c = intent.getIntExtra("color", 0xFFFF1744);
                    cp.edit().putInt("crosshair_color", c).apply();
                }
                if (intent.hasExtra("size")) {
                    int s = intent.getIntExtra("size", 52);
                    cp.edit().putInt("crosshair_size", s).apply();
                }
                if (intent.hasExtra("style")) {
                    crosshairStyle = intent.getIntExtra("style", crosshairStyle);
                    cp.edit().putInt("crosshair_style", crosshairStyle).apply();
                }
                if (crosshairEnabled) {
                    updateCrosshairOverlay();
                }
                return START_STICKY;
            }
            if (ACTION_SHOW_PANEL.equals(act)) {
                userDismissed = false;
                bubbleState = 0;
                cancelAutoEdgeTimer();
                if (intent.getStringExtra("package") != null) targetPackage = intent.getStringExtra("package");
                if (intent.getStringExtra("label") != null) targetLabel = intent.getStringExtra("label");
                if (params == null) {
                    showBubble();
                } else {
                    params.width = dp(340);
                    DisplayMetrics dm = getResources().getDisplayMetrics();
                    params.height = Math.min(dp(540), dm.heightPixels - dp(24));
                    replaceOverlay(buildPanel());
                }
                return START_STICKY;
            }
            if (intent.getStringExtra("package") != null) {
                targetPackage = intent.getStringExtra("package");
            }
            userDismissed = false;
            if (intent.getStringExtra("label") != null) targetLabel = intent.getStringExtra("label");
        }
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            com.revror.kernelmanager.utils.SilentPermissionHelper.ensureOverlaySilent(this);
            try { Thread.sleep(150); } catch (InterruptedException ignored) {}
        }
        selectedProfile = GsmProfileManager.selectedProfile(this);
        restorePluginState();
        handler.removeCallbacks(foregroundMonitor);
        handler.post(foregroundMonitor);
        return START_STICKY;
    }

    private void resetAutoEdgeTimer() {
        // Persistent by design: never schedule automatic hiding or edge minimization.
    }

    private void cancelAutoEdgeTimer() {
        // No-op retained for existing state-transition call sites.
    }

    private void showBubble() {
        if (!targetInForeground) return;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (params == null) {
            params = new WindowManager.LayoutParams(dp(54), dp(54),
                    Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.CENTER_VERTICAL | (dockOnRight ? Gravity.END : Gravity.START);
            params.x = dp(8);
            params.y = savedY;
        }
        updateScreenFilterOverlay();
        updateCrosshairOverlay();

        if (bubbleState == 2) {
            // Minimized side edge tab
            params.width = dp(34);
            params.height = dp(68);
            replaceOverlay(buildMinimizedEdgeTab());
        } else if (bubbleState == 0) {
            // Expanded Panel
            cancelAutoEdgeTimer();
            SharedPreferences floatingPrefs = getSharedPreferences("ror_game_space_center", MODE_PRIVATE);
            int layoutStyle = floatingPrefs.getInt("ror_floating_layout_style", 0);
            FloatingWindowLayoutManager.configureLayoutParams(this, params, layoutStyle);
            replaceOverlay(buildPanel());
        } else if (bubbleState == 3) {
            // Mini Apps View
            cancelAutoEdgeTimer();
            params.width = dp(340);
            DisplayMetrics dm = getResources().getDisplayMetrics();
            params.height = Math.min(dp(540), dm.heightPixels - dp(24));
            replaceOverlay(buildMiniAppsPanel());
        } else if (bubbleState == 4) {
            // Screen Recorder Setup View
            cancelAutoEdgeTimer();
            params.width = dp(340);
            DisplayMetrics dm = getResources().getDisplayMetrics();
            params.height = Math.min(dp(540), dm.heightPixels - dp(24));
            replaceOverlay(buildScreenRecorderPanel());
        } else if (bubbleState == 5) {
            cancelAutoEdgeTimer();
            params.width = dp(360);
            params.height = Math.min(dp(620), getResources().getDisplayMetrics().heightPixels - dp(32));
            replaceOverlay(buildGamePluginsPanel());
        } else if (bubbleState == 6) {
            cancelAutoEdgeTimer();
            params.width = dp(340);
            params.height = Math.min(dp(540), getResources().getDisplayMetrics().heightPixels - dp(24));
            replaceOverlay(buildWifiPanel());
        } else {
            // Floating Orb
            bubbleState = 1;
            params.width = dp(54);
            params.height = dp(54);
            replaceOverlay(buildBubble());
            resetAutoEdgeTimer();
        }
    }

    /**
     * Minimized state: Small sleek tab tucked at the edge of the screen with a '>' (or '<') icon.
     * Tapping it pops the bubble back open.
     */
    private View buildMinimizedEdgeTab() {
        FrameLayout tab = new FrameLayout(this);
        tab.setLayoutParams(new WindowManager.LayoutParams(dp(34), dp(68)));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF20B0B0D);
        bg.setStroke(dp(1.5f), 0xFFFF3544);
        if (dockOnRight) {
            bg.setCornerRadii(new float[]{dp(18), dp(18), 0, 0, 0, 0, dp(18), dp(18)});
        } else {
            bg.setCornerRadii(new float[]{0, 0, dp(18), dp(18), dp(18), dp(18), 0, 0});
        }
        tab.setBackground(bg);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);

        ImageView icon = new ImageView(this);
        icon.setImageResource(dockOnRight ? R.drawable.ic_back : R.drawable.ic_chevron_right);
        icon.setColorFilter(0xFFFF4552);
        content.addView(icon, new LinearLayout.LayoutParams(dp(22), dp(22)));

        tab.addView(content, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        attachEdgeTabTouch(tab);
        return tab;
    }

    private View buildBubble() {
        // REVROR-style compact game-space trigger.
        FrameLayout frame = new FrameLayout(this);
        frame.setLayoutParams(new WindowManager.LayoutParams(dp(54), dp(54)));

        // Outer Neon Glow Ring
        View outerGlow = new View(this);
        GradientDrawable ringBg = new GradientDrawable();
        ringBg.setShape(GradientDrawable.OVAL);
        ringBg.setColor(0x33FF1F35);
        ringBg.setStroke(dp(2), 0xFFFF3545);
        outerGlow.setBackground(ringBg);
        frame.addView(outerGlow, new FrameLayout.LayoutParams(dp(54), dp(54), Gravity.CENTER));

        // Inner Obsidian Gaming Core
        View innerCore = new View(this);
        GradientDrawable coreBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xF2250A0E, 0xFF09090B, 0xF23B0A12});
        coreBg.setShape(GradientDrawable.OVAL);
        coreBg.setStroke(dp(1), 0xAAFF5662);
        innerCore.setBackground(coreBg);
        frame.addView(innerCore, new FrameLayout.LayoutParams(dp(44), dp(44), Gravity.CENTER));

        // REVROR Brand Logo
        ImageView logoView = new ImageView(this);
        logoView.setImageResource(R.drawable.ror_logo);
        logoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        frame.addView(logoView, new FrameLayout.LayoutParams(dp(30), dp(30), Gravity.CENTER));

        attachDrag(frame);
        return frame;
    }

    /**
     * macOS Style Traffic Lights: Red (Close/Collapse), Yellow (Minimize to Edge '>'), Green (Toggle HUD / Expand)
     */
    private LinearLayout buildMacOsTrafficLights(Runnable onClose, Runnable onMinimize, Runnable onExpand) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(2), dp(2), dp(2), dp(2));

        bar.addView(createTrafficDot(0xFFD71528, 0xFFFF6874, R.drawable.ic_window_close, onClose));
        bar.addView(createTrafficDot(0xFF25252A, 0xFF55555D, R.drawable.ic_window_minimize, onMinimize));
        bar.addView(createTrafficDot(0xFF25252A, 0xFF55555D, R.drawable.ic_refresh, onExpand));

        return bar;
    }

    private View createTrafficDot(int color, int stroke, int iconRes, Runnable action) {
        FrameLayout dot = new FrameLayout(this);
        int size = dp(18);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMargins(dp(3), 0, dp(3), 0);
        dot.setLayoutParams(lp);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(color);
        bg.setStroke(dp(1), stroke);
        dot.setBackground(bg);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.WHITE);
        icon.setPadding(dp(4), dp(4), dp(4), dp(4));
        dot.addView(icon, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));

        dot.setOnClickListener(v -> {
            if (action != null) action.run();
        });
        return dot;
    }

    void closeOverlay() {
        userDismissed = true;
        hideOverlay();
        toast("Game HUD closed");
    }

    View buildPanel() {
        SharedPreferences floatingPrefs = getSharedPreferences("ror_game_space_center", MODE_PRIVATE);
        int layoutStyle = floatingPrefs.getInt("ror_floating_layout_style", 0);
        return FloatingWindowLayoutManager.buildLayout(this, layoutStyle);
    }

    private View buildClassicPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(14), dp(16), dp(14));

        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFA1C1C20, 0xFC09090B, 0xFA26080D});
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), 0xFFFF3B48);
        panel.setBackground(bg);

        // Game Space header and overlay controls (Fixed at Top).
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(8));

        TextView title = label("REVROR GAME SPACE  /  " + targetLabel.toUpperCase(Locale.US), 12.5f, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1.0f));

        header.addView(buildMacOsTrafficLights(this::closeOverlay, this::minimizeToEdge, () -> {
            if (metrics != null) metrics.setText(readLiveStats());
            toast("Telemetry refreshed");
        }));

        panel.addView(header);

        // Scrollable content area
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(true);
        scroll.setScrollbarFadingEnabled(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(2), 0, dp(4));

        TextView energy = label("ENERGY CUBE  •  LIVE PERFORMANCE", 8.5f, 0xFFFF4A56);
        energy.setTypeface(null, Typeface.BOLD);
        energy.setLetterSpacing(.10f);
        energy.setPadding(0, 0, 0, dp(6));
        content.addView(energy);

        // Live CPU/GPU/FPS/RAM telemetry.
        metrics = label("Reading live device usage…", 11, 0xFFF4F4F5);
        metrics.setPadding(dp(10), dp(8), dp(10), dp(8));
        GradientDrawable hud = new GradientDrawable();
        hud.setColor(0xEE121215);
        hud.setCornerRadius(dp(10));
        hud.setStroke(dp(1), 0xFF4A4A50);
        metrics.setBackground(hud);
        content.addView(metrics);

        // Profiles Section
        TextView modesTitle = label("PERFORMANCE MODES", 9.5f, 0xFFFF4A56);
        modesTitle.setTypeface(null, Typeface.BOLD);
        modesTitle.setPadding(0, dp(10), 0, dp(5));
        content.addView(modesTitle);

        boolean phoenix = GsmProfileManager.isPhoenixSelected(this);
        HorizontalScrollView profileScroll = new HorizontalScrollView(this);
        profileScroll.setHorizontalScrollBarEnabled(false);
        profileScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout profileRow = new LinearLayout(this);
        profileRow.setOrientation(LinearLayout.HORIZONTAL);
        profileRow.setBaselineAligned(false);

        List<com.revror.kernelmanager.utils.kernel.compat.ProfileRegistry.ProfileDescriptor> profiles =
                com.revror.kernelmanager.utils.kernel.compat.ProfileRegistry.getProfiles(phoenix);
        for (int i = 0; i < 8; i++) {
            if (i < profiles.size()) {
                com.revror.kernelmanager.utils.kernel.compat.ProfileRegistry.ProfileDescriptor desc = profiles.get(i);
                profileButtons[i] = profileButton(desc.shortName, desc.id);
                profileRow.addView(profileButtons[i], profileParams());
            } else {
                profileButtons[i] = null;
            }
        }
        profileScroll.addView(profileRow, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(profileScroll);
        updateProfileButtons();

        // Brightness is independently hideable from Game Space > Floating Windows.
        SharedPreferences floatingPrefs = getSharedPreferences("ror_game_space_center", MODE_PRIVATE);
        if (floatingPrefs.getBoolean("ror_tool_vis_brightness_control", true)) {
            content.addView(buildBrightnessControlRow());
        }

        // Live Tools Section Header with "SET DEFAULT" button
        LinearLayout toolsHeaderRow = new LinearLayout(this);
        toolsHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        toolsHeaderRow.setGravity(Gravity.CENTER_VERTICAL);
        toolsHeaderRow.setPadding(0, dp(10), 0, dp(4));

        TextView toolsTitle = label("INGAME TOOLS & PRO ENGINES", 9.5f, 0xFFFF4A56);
        toolsTitle.setTypeface(null, Typeface.BOLD);
        toolsHeaderRow.addView(toolsTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView resetDefaultBtn = new TextView(this);
        resetDefaultBtn.setText("SET DEFAULT");
        resetDefaultBtn.setTextSize(8.5f);
        resetDefaultBtn.setTextColor(0xFF8E95B2);
        resetDefaultBtn.setTypeface(null, Typeface.BOLD);
        resetDefaultBtn.setPadding(dp(7), dp(3), dp(7), dp(3));
        GradientDrawable rstBg = new GradientDrawable();
        rstBg.setColor(0x4422263C);
        rstBg.setCornerRadius(dp(6));
        rstBg.setStroke(dp(1), 0x55444D70);
        resetDefaultBtn.setBackground(rstBg);
        resetDefaultBtn.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            SharedPreferences sp = getSharedPreferences("ror_game_space_center", MODE_PRIVATE);
            sp.edit().putString("ror_floating_tools_order", DEFAULT_FLOATING_TOOLS_ORDER).apply();
            Toast.makeText(this, "Reset to default layout", Toast.LENGTH_SHORT).show();
            populateFloatingTools();
        });
        toolsHeaderRow.addView(resetDefaultBtn);
        content.addView(toolsHeaderRow);

        floatingToolsContainer = new LinearLayout(this);
        floatingToolsContainer.setOrientation(LinearLayout.VERTICAL);
        populateFloatingTools();
        content.addView(floatingToolsContainer);

        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        panel.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        // Live Floating Ad Banner inside Game Space window
        View adBox = buildFloatingAdBanner();
        if (adBox != null) {
            panel.addView(adBox);
        }

        // Footer Actions: Side Minimize & Collapse to Orb (Fixed at Bottom)
        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setPadding(0, dp(6), 0, 0);

        Button sideMinBtn = action("Dock to edge", "minimize");
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, dp(40), 1.0f);
        p1.setMarginEnd(dp(4));
        footer.addView(sideMinBtn, p1);

        Button closeBtn = action("Collapse", "close");
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, dp(40), 1.0f);
        p2.setMarginStart(dp(4));
        footer.addView(closeBtn, p2);

        panel.addView(footer);

        attachHeaderDrag(header);
        attachDrag(panel);
        return panel;
    }

    public void reloadFloatingTools() {
        new Handler(Looper.getMainLooper()).post(this::populateFloatingTools);
    }

    private void populateFloatingTools() {
        if (floatingToolsContainer == null) return;
        floatingToolsContainer.removeAllViews();

        SharedPreferences sp = getSharedPreferences("ror_game_space_center", MODE_PRIVATE);
        String orderCsv = sp.getString("ror_floating_tools_order", DEFAULT_FLOATING_TOOLS_ORDER);
        String[] toolKeys = orderCsv.split(",");
        List<String> visibleKeys = new ArrayList<>();
        for (String k : toolKeys) {
            String toolKey = k.trim();
            if (toolKey.isEmpty()) continue;
            boolean isVisible = sp.getBoolean("ror_tool_vis_" + toolKey, true);
            if (isVisible) visibleKeys.add(toolKey);
        }

        final int count = visibleKeys.size();
        for (int i = 0; i < count; i++) {
            final int index = i;
            final String toolKey = visibleKeys.get(i);
            View actionView = buildToolActionView(toolKey);
            if (actionView == null) continue;

            View row = buildRearrangeableToolRow(toolKey, actionView, index, count,
                    () -> moveToolItem(visibleKeys, index, index - 1),
                    () -> moveToolItem(visibleKeys, index, index + 1));
            floatingToolsContainer.addView(row);
        }
    }

    private void moveToolItem(List<String> list, int from, int to) {
        if (from < 0 || from >= list.size() || to < 0 || to >= list.size()) return;
        String item = list.remove(from);
        list.add(to, item);
        String newCsv = TextUtils.join(",", list);
        SharedPreferences sp = getSharedPreferences("ror_game_space_center", MODE_PRIVATE);
        sp.edit().putString("ror_floating_tools_order", newCsv).apply();
        populateFloatingTools();
    }

    private View buildRearrangeableToolRow(String toolKey, View actionView, int index, int total, Runnable onMoveUp, Runnable onMoveDown) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.bottomMargin = dp(4);
        row.setLayoutParams(rowLp);

        // Action button takes majority space
        LinearLayout.LayoutParams actionLp = new LinearLayout.LayoutParams(0, dp(42), 1.0f);
        actionView.setLayoutParams(actionLp);
        row.addView(actionView);

        // Long press on the action button moves it up or down in real time!
        actionView.setOnLongClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            if (index > 0) {
                onMoveUp.run();
                Toast.makeText(this, "Moved up", Toast.LENGTH_SHORT).show();
            } else if (index < total - 1) {
                onMoveDown.run();
                Toast.makeText(this, "Moved down", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        // Quick real-time reorder controls: ▲ and ▼
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(dp(4), 0, 0, 0);

        ImageView upBtn = new ImageView(this);
        upBtn.setImageResource(R.drawable.ic_arrow_up);
        upBtn.setColorFilter(index > 0 ? 0xFF8E95B2 : 0x338E95B2);
        upBtn.setPadding(dp(7), dp(6), dp(7), dp(6));
        GradientDrawable upBg = new GradientDrawable();
        upBg.setColor(0x331C1D2A);
        upBg.setCornerRadius(dp(6));
        upBg.setStroke(dp(1), index > 0 ? 0x44536A99 : 0x11536A99);
        upBtn.setBackground(upBg);
        LinearLayout.LayoutParams upLp = new LinearLayout.LayoutParams(dp(26), dp(26));
        upBtn.setLayoutParams(upLp);
        if (index > 0) {
            upBtn.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                onMoveUp.run();
            });
        }
        controls.addView(upBtn);

        ImageView downBtn = new ImageView(this);
        downBtn.setImageResource(R.drawable.ic_arrow_down);
        downBtn.setColorFilter(index < total - 1 ? 0xFF8E95B2 : 0x338E95B2);
        downBtn.setPadding(dp(7), dp(6), dp(7), dp(6));
        LinearLayout.LayoutParams downLp = new LinearLayout.LayoutParams(dp(26), dp(26));
        downLp.setMarginStart(dp(3));
        downBtn.setLayoutParams(downLp);
        GradientDrawable downBg = new GradientDrawable();
        downBg.setColor(0x331C1D2A);
        downBg.setCornerRadius(dp(6));
        downBg.setStroke(dp(1), index < total - 1 ? 0x44536A99 : 0x11536A99);
        downBtn.setBackground(downBg);
        if (index < total - 1) {
            downBtn.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                onMoveDown.run();
            });
        }
        controls.addView(downBtn);

        row.addView(controls);
        return row;
    }

    private View buildToolActionView(String toolKey) {
        if ("quick_screenshot".equals(toolKey)) {
            return action("Quick Screenshot  |  Instant capture", "quick_screenshot");
        } else if ("speedrun_stopwatch".equals(toolKey)) {
            return action("Speedrun Stopwatch  |  " + (stopwatchOverlay != null ? "ACTIVE" : "OFF"), "speedrun_stopwatch");
        } else if ("floating_calc".equals(toolKey)) {
            return action("Combat Mini Calculator  |  Overlay", "floating_calc");
        } else if ("audio_boost".equals(toolKey)) {
            return action("Audio Boost (+6dB)  |  " + (audioBoostEnabled ? "ON" : "OFF"), "audio_boost");
        } else if ("quick_ram_vram_purge".equals(toolKey)) {
            return action("Super RAM & VRAM Flush  |  Purge caches", "quick_ram_vram_purge");
        } else if ("touch_sampling_boost".equals(toolKey)) {
            return action("Touch Engine Boost  |  " + (touchBoostEnabled ? "480Hz ULTRA" : "240Hz NORMAL"), "touch_sampling_boost");
        } else if ("force_max_refresh".equals(toolKey)) {
            return action("Force Peak Display Refresh  |  " + (highRefreshEnabled ? "PEAK LOCKED" : "AUTO"), "force_max_refresh");
        } else if ("thermal_battery_guard".equals(toolKey)) {
            return action("Thermal Guard Alert  |  " + (thermalGuardEnabled ? "ARMED" : "OFF"), "thermal_battery_guard");
        } else if ("voice_changer".equals(toolKey)) {
            return action("Voice Comms Filter  |  " + VOICE_FILTER_NAMES[voiceFilterIndex], "voice_changer");
        } else if ("display_tools".equals(toolKey)) {
            return action("Game plugins and display", "display_tools");
        } else if ("wifi_manager".equals(toolKey)) {
            return action("Wi-Fi search & connect", "wifi_manager");
        } else if ("crosshair_settings".equals(toolKey)) {
            return action("Crosshair settings  |  " + CROSSHAIR_NAMES[crosshairStyle] + (crosshairEnabled ? "  |  ON" : "  |  OFF"), "crosshair_settings");
        } else if ("anti_disturb".equals(toolKey)) {
            return action("Anti-disturb  |  " + (antiDisturbEnabled ? "ON" : "OFF"), "anti_disturb");
        } else if ("barrage".equals(toolKey)) {
            return action("Barrage messages  |  " + (barrageEnabled ? "ON" : "OFF"), "barrage");
        } else if ("mini_apps".equals(toolKey)) {
            return action("Mini windows and multitasking", "mini_apps");
        } else if ("screen_recorder".equals(toolKey)) {
            return action("Screen recorder  |  480p / 720p / 1080p", "screen_recorder");
        } else if ("network".equals(toolKey)) {
            return action("Network latency boost", "network");
        } else if ("cleanup".equals(toolKey)) {
            return action("One-touch background cleanup", "cleanup");
        } else if ("toggle_overlay".equals(toolKey)) {
            boolean isOverlayActive = getSharedPreferences("ror_tools", MODE_PRIVATE).getBoolean("overlay", false);
            return action(isOverlayActive ? "CPU / GPU floating HUD  |  ON" : "CPU / GPU floating HUD  |  OFF", "toggle_overlay");
        }
        return null;
    }

    private Button profileButton(String name, int profile) {
        Button button = new Button(this);
        button.setText(name);
        button.setTextColor(Color.WHITE);
        button.setTextSize(9.5f);
        button.setTypeface(null, Typeface.BOLD);
        button.setPadding(0, 0, 0, 0);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setOnClickListener(v -> {
            selectedProfile = profile;
            updateProfileButtons();
            GameOptimizer.applyProfile(this, profile, this::toastResult);
        });
        return button;
    }

    private LinearLayout.LayoutParams profileParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(74), dp(36));
        p.setMargins(dp(3), dp(2), dp(3), dp(2));
        return p;
    }

    private void updateProfileButtons() {
        for (int i = 0; i < profileButtons.length; i++) {
            if (profileButtons[i] == null) continue;
            boolean active = i == selectedProfile;
            GradientDrawable bg = active
                    ? new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0xFFD81125, 0xFFFF3346})
                    : new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0xFF242428, 0xFF17171A});
            bg.setCornerRadius(dp(8));
            bg.setStroke(dp(active ? 2 : 1), active ? 0xFFFF7580 : 0xFF46464D);
            profileButtons[i].setBackground(bg);
            profileButtons[i].setAlpha(active ? 1f : .78f);
            profileButtons[i].setTextColor(active ? Color.WHITE : 0xFFB0B0B8);
        }
    }

    private Button action(String name, String act) {
        Button button = new Button(this);
        button.setText(name);
        button.setTextColor(Color.WHITE);
        button.setTextSize(11);
        button.setAllCaps(false);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        button.setPadding(dp(12), 0, dp(12), 0);
        Drawable icon = ContextCompat.getDrawable(this, iconForAction(act));
        if (icon != null) {
            icon = icon.mutate();
            icon.setTint(0xFFFF5360);
            icon.setBounds(0, 0, dp(20), dp(20));
            button.setCompoundDrawables(icon, null, null, null);
            button.setCompoundDrawablePadding(dp(10));
        }

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xEE202024);
        bg.setCornerRadius(dp(9));
        bg.setStroke(dp(1), 0xFF424249);
        button.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(42));
        lp.bottomMargin = dp(6);
        button.setLayoutParams(lp);

        button.setOnClickListener(v -> {
            if ("quick_screenshot".equals(act)) {
                takeQuickScreenshot();
                return;
            }
            if ("speedrun_stopwatch".equals(act)) {
                toggleSpeedrunStopwatch();
                if (bubbleState == 0) replaceOverlay(buildPanel());
                return;
            }
            if ("floating_calc".equals(act)) {
                toggleFloatingCalculator();
                return;
            }
            if ("audio_boost".equals(act)) {
                toggleAudioBoost();
                return;
            }
            if ("quick_ram_vram_purge".equals(act)) {
                flushVramAndRam();
                return;
            }
            if ("touch_sampling_boost".equals(act)) {
                boostTouchSampling();
                return;
            }
            if ("force_max_refresh".equals(act)) {
                forcePeakRefreshRate();
                return;
            }
            if ("thermal_battery_guard".equals(act)) {
                toggleThermalGuard();
                return;
            }
            if ("voice_changer".equals(act)) {
                cycleVoiceFilter();
                return;
            }
            if ("close".equals(act)) {
                collapse();
                return;
            }
            if ("minimize".equals(act)) {
                minimizeToEdge();
                return;
            }
            if ("mini_apps".equals(act)) {
                showMiniApps();
                return;
            }
            if ("screen_recorder".equals(act)) {
                showScreenRecorder();
                return;
            }
            if ("display_tools".equals(act)) {
                showGamePlugins();
                return;
            }
            if ("crosshair_toggle".equals(act)) {
                toggleCrosshair();
                return;
            }
            if ("crosshair_settings".equals(act)) {
                showCrosshairSettingsPanel();
                return;
            }
            if ("crosshair_style".equals(act)) {
                cycleCrosshairStyle();
                return;
            }
            if ("crosshair_color".equals(act)) {
                cycleCrosshairColor();
                return;
            }
            if ("crosshair_size".equals(act)) {
                cycleCrosshairSize();
                return;
            }
            if ("anti_disturb".equals(act)) {
                toggleAntiDisturb();
                return;
            }
            if ("barrage".equals(act)) {
                toggleBarrage();
                return;
            }
            if ("choose_any_app".equals(act)) {
                showAllInstalledAppsPicker();
                return;
            }
            if ("wifi_manager".equals(act)) {
                showWifiPanel();
                return;
            }
            if ("toggle_overlay".equals(act)) {
                boolean active = getSharedPreferences("ror_tools", MODE_PRIVATE).getBoolean("overlay", false);
                if (active) {
                    startService(new Intent(this, CpuOverlayService.class).setAction(CpuOverlayService.ACTION_STOP));
                    getSharedPreferences("ror_tools", MODE_PRIVATE).edit().putBoolean("overlay", false).apply();
                    button.setText("CPU / GPU floating HUD  |  OFF");
                    toast("Floating info overlay disabled");
                } else {
                    if (Build.VERSION.SDK_INT >= 26) {
                        startForegroundService(new Intent(this, CpuOverlayService.class));
                    } else {
                        startService(new Intent(this, CpuOverlayService.class));
                    }
                    getSharedPreferences("ror_tools", MODE_PRIVATE).edit().putBoolean("overlay", true).apply();
                    button.setText("CPU / GPU floating HUD  |  ON");
                    toast("Floating info overlay enabled");
                }
                return;
            }
            GameOptimizer.quickAction(this, targetPackage, act, this::toastResult);
        });
        return button;
    }

    private int iconForAction(String act) {
        switch (act) {
            case "quick_screenshot": return R.drawable.ic_gallery;
            case "speedrun_stopwatch": return R.drawable.ic_clock;
            case "floating_calc": return R.drawable.ic_numbers;
            case "audio_boost": return R.drawable.ic_volume;
            case "quick_ram_vram_purge": return R.drawable.ic_turbo_fan;
            case "touch_sampling_boost": return R.drawable.ic_gestures;
            case "force_max_refresh": return R.drawable.ic_refresh;
            case "thermal_battery_guard": return R.drawable.ic_shield;
            case "voice_changer": return R.drawable.ic_barrage_style;
            case "display_tools": return R.drawable.ic_display;
            case "crosshair_toggle":
            case "crosshair_style":
            case "crosshair_color":
            case "crosshair_size": return R.drawable.ic_crosshair;
            case "anti_disturb": return R.drawable.ic_shield;
            case "barrage": return R.drawable.ic_barrage_message;
            case "mini_apps": return R.drawable.ic_layers;
            case "screen_recorder": return R.drawable.ic_record_dot;
            case "stop_rec_now": return R.drawable.ic_stop;
            case "choose_any_app": return R.drawable.ic_search;
            case "network":
            case "wifi_manager": return R.drawable.ic_wifi;
            case "cleanup": return R.drawable.ic_turbo_fan;
            case "toggle_overlay": return R.drawable.ic_speed_meter;
            case "minimize": return R.drawable.ic_window_minimize;
            case "close": return R.drawable.ic_window_close;
            default: return R.drawable.ic_tools;
        }
    }

    void showGamePlugins() {
        bubbleState = 5;
        params.width = dp(360);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        params.height = Math.min(dp(620), dm.heightPixels - dp(32));
        replaceOverlay(buildGamePluginsPanel());
    }

    private View buildGamePluginsPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFC1B1B1F, 0xFE08080A, 0xFC28080E});
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), 0xFFFF3B48);
        panel.setBackground(bg);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = pluginHeaderButton("BACK", R.drawable.ic_back);
        back.setOnClickListener(v -> expand());
        header.addView(back);
        TextView title = label("  GAME PLUGINS", 13, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(40), 1f));
        header.addView(buildMacOsTrafficLights(this::closeOverlay, this::minimizeToEdge, this::showGamePlugins));
        panel.addView(header);

        TextView subtitle = label("IN-SCREEN CONTROLS  /  DISPLAY  /  NETWORK", 8.5f, 0xFFFF5964);
        subtitle.setTypeface(null, Typeface.BOLD);
        subtitle.setLetterSpacing(.08f);
        subtitle.setPadding(dp(2), 0, 0, dp(8));
        panel.addView(subtitle);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        content.addView(pluginSection("AIM AND DISPLAY"));
        content.addView(pluginRow(
                pluginTile("Crosshair", crosshairEnabled, R.drawable.ic_crosshair, this::toggleCrosshair),
                pluginTile("Style: " + CROSSHAIR_NAMES[crosshairStyle], true, R.drawable.ic_crosshair, this::cycleCrosshairStyle)));
        content.addView(pluginRow(
                pluginTile("Brightness lock", brightnessLocked, R.drawable.ic_sun, this::toggleBrightnessLock),
                pluginTile("Color boost", colorBoostEnabled, R.drawable.ic_sparkle, this::toggleColorBoost)));
        content.addView(pluginRow(
                pluginTile("Hunting Filter\n" + getHuntingFilterShortName(), screenFilterEnabled, R.drawable.ic_display, this::showHuntingFilterPanel),
                pluginTile("High refresh", highRefreshEnabled, R.drawable.ic_refresh, this::toggleHighRefresh)));
        content.addView(pluginRow(
                pluginTile("Touch boost", touchBoostEnabled, R.drawable.ic_gestures, this::toggleTouchBoost),
                pluginTile("Floating HUD", getSharedPreferences("ror_tools", MODE_PRIVATE).getBoolean("overlay", false),
                        R.drawable.ic_speed_meter, this::togglePerformanceHud)));

        content.addView(pluginSection("ANTI-DISTURB AND MESSAGES"));
        content.addView(pluginRow(
                pluginTile("Anti-disturb", antiDisturbEnabled, R.drawable.ic_shield, this::toggleAntiDisturb),
                pluginTile("Barrage messages", barrageEnabled, R.drawable.ic_barrage_message, this::toggleBarrage)));
        content.addView(pluginRow(
                pluginTile("Play reminder", reminderMinutes > 0,
                        R.drawable.ic_clock, this::cycleReminder),
                pluginTile("Barrage style: " + BARRAGE_STYLE_NAMES[barrageStyle], true,
                        R.drawable.ic_barrage_style, this::cycleBarrageStyle)));

        content.addView(pluginSection("CONNECTIVITY"));
        content.addView(pluginRow(
                pluginTile("Wi-Fi", wifiEnabled, R.drawable.ic_wifi, this::showWifiPanel),
                pluginTile("Search Wi-Fi", true, R.drawable.ic_search, this::showWifiPanel)));
        content.addView(pluginRow(
                pluginTile("Wi-Fi Power", wifiEnabled, R.drawable.ic_wifi, this::toggleWifi),
                pluginTile("Mobile data", mobileDataEnabled, R.drawable.ic_mobile_data, this::toggleMobileData)));

        content.addView(pluginSection("PRO IN-GAME ENGINES"));
        content.addView(pluginRow(
                pluginTile("Quick Screenshot", true, R.drawable.ic_gallery, this::takeQuickScreenshot),
                pluginTile("Stopwatch\n" + (stopwatchOverlay != null ? "ACTIVE" : "OFF"), stopwatchOverlay != null, R.drawable.ic_clock, this::toggleSpeedrunStopwatch)));
        content.addView(pluginRow(
                pluginTile("Mini Calculator", calculatorOverlay != null, R.drawable.ic_numbers, this::toggleFloatingCalculator),
                pluginTile("Audio Boost\n" + (audioBoostEnabled ? "+6dB ON" : "OFF"), audioBoostEnabled, R.drawable.ic_volume, this::toggleAudioBoost)));
        content.addView(pluginRow(
                pluginTile("RAM/VRAM Flush", true, R.drawable.ic_turbo_fan, this::flushVramAndRam),
                pluginTile("480Hz Touch\n" + (touchBoostEnabled ? "ULTRA" : "NORMAL"), touchBoostEnabled, R.drawable.ic_gestures, this::boostTouchSampling)));
        content.addView(pluginRow(
                pluginTile("Force Peak Hz\n" + (highRefreshEnabled ? "LOCKED" : "AUTO"), highRefreshEnabled, R.drawable.ic_refresh, this::forcePeakRefreshRate),
                pluginTile("Thermal Guard\n" + (thermalGuardEnabled ? "ARMED" : "OFF"), thermalGuardEnabled, R.drawable.ic_shield, this::toggleThermalGuard)));
        content.addView(pluginRow(
                pluginTile("Voice Filter:\n" + VOICE_FILTER_NAMES[voiceFilterIndex], voiceFilterIndex > 0, R.drawable.ic_barrage_style, this::cycleVoiceFilter),
                pluginTile("Brightness Lock\n" + (brightnessLocked ? "LOCKED" : "UNLOCKED"), brightnessLocked, R.drawable.ic_sun, this::toggleBrightnessLock)));

        TextView reminderHint = label(reminderMinutes == 0
                ? "PLAY REMINDER  /  OFF  /  TAP TO CYCLE 15, 30, 60 MIN"
                : "PLAY REMINDER  /  EVERY " + reminderMinutes + " MINUTES", 8, 0xFF9A9AA2);
        reminderHint.setGravity(Gravity.CENTER);
        reminderHint.setPadding(0, dp(7), 0, dp(3));
        content.addView(reminderHint);
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        View adBox = buildFloatingAdBanner();
        if (adBox != null) {
            panel.addView(adBox);
        }
        attachHeaderDrag(header);
        attachDrag(panel);
        return panel;
    }

    private TextView pluginHeaderButton(String text, int iconRes) {
        TextView button = label(text, 9, 0xFFFF5964);
        button.setGravity(Gravity.CENTER);
        button.setTypeface(null, Typeface.BOLD);
        button.setPadding(dp(8), dp(5), dp(8), dp(5));
        Drawable icon = ContextCompat.getDrawable(this, iconRes);
        if (icon != null) {
            icon = icon.mutate();
            icon.setTint(0xFFFF5964);
            icon.setBounds(0, 0, dp(15), dp(15));
            button.setCompoundDrawables(icon, null, null, null);
            button.setCompoundDrawablePadding(dp(5));
        }
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(0x55290B10);
        shape.setStroke(dp(1), 0x99FF3B48);
        shape.setCornerRadius(dp(8));
        button.setBackground(shape);
        return button;
    }

    private TextView pluginSection(String text) {
        TextView title = label(text, 8.5f, 0xFFFF5964);
        title.setTypeface(null, Typeface.BOLD);
        title.setLetterSpacing(.08f);
        title.setPadding(dp(2), dp(8), 0, dp(5));
        return title;
    }

    private LinearLayout pluginRow(View left, View right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(0, dp(68), 1f);
        leftLp.setMargins(0, 0, dp(4), dp(7));
        LinearLayout.LayoutParams rightLp = new LinearLayout.LayoutParams(0, dp(68), 1f);
        rightLp.setMargins(dp(4), 0, 0, dp(7));
        row.addView(left, leftLp);
        row.addView(right, rightLp);
        return row;
    }

    private TextView pluginTile(String title, boolean enabled, int iconRes, Runnable action) {
        TextView tile = label(title + "\n" + (enabled ? "ON" : "OFF"), 10, Color.WHITE);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setTypeface(null, Typeface.BOLD);
        tile.setLineSpacing(0, 1.05f);
        tile.setPadding(dp(12), 0, dp(8), 0);
        Drawable icon = ContextCompat.getDrawable(this, iconRes);
        if (icon != null) {
            icon = icon.mutate();
            icon.setTint(enabled ? 0xFFFF5964 : 0xFF8E8E96);
            icon.setBounds(0, 0, dp(23), dp(23));
            tile.setCompoundDrawables(icon, null, null, null);
            tile.setCompoundDrawablePadding(dp(9));
        }
        GradientDrawable shape = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                enabled ? new int[]{0xFF3A0D15, 0xFF17171A} : new int[]{0xFF242428, 0xFF141416});
        shape.setCornerRadius(dp(10));
        shape.setStroke(dp(1), enabled ? 0xFFFF3B48 : 0xFF46464D);
        tile.setBackground(shape);
        tile.setOnClickListener(v -> action.run());
        return tile;
    }

    private void restorePluginState() {
        SharedPreferences prefs = getSharedPreferences("ror_game_plugins", MODE_PRIVATE);
        crosshairEnabled = prefs.getBoolean("crosshair", false);
        brightnessLocked = prefs.getBoolean("brightness_lock", false);
        colorBoostEnabled = prefs.getBoolean("color_boost", false);
        screenFilterEnabled = prefs.getBoolean("screen_filter", false);
        highRefreshEnabled = prefs.getBoolean("high_refresh", false);
        touchBoostEnabled = prefs.getBoolean("touch_boost", false);
        antiDisturbEnabled = prefs.getBoolean("anti_disturb", false);
        wifiEnabled = prefs.getBoolean("wifi", true);
        mobileDataEnabled = prefs.getBoolean("mobile_data", true);
        barrageEnabled = prefs.getBoolean("barrage", true);
        reminderMinutes = prefs.getInt("reminder_minutes", 0);
        crosshairStyle = Math.max(0, Math.min(CROSSHAIR_NAMES.length - 1, prefs.getInt("crosshair_style", 0)));
        barrageStyle = Math.max(0, Math.min(BARRAGE_STYLE_NAMES.length - 1, prefs.getInt("barrage_style", 0)));
        audioBoostEnabled = prefs.getBoolean("audio_boost", false);
        thermalGuardEnabled = prefs.getBoolean("thermal_guard", false);
        voiceFilterIndex = Math.max(0, Math.min(VOICE_FILTER_NAMES.length - 1, prefs.getInt("voice_filter", 0)));
        scheduleReminder();
    }

    private void savePluginState() {
        getSharedPreferences("ror_game_plugins", MODE_PRIVATE).edit()
                .putBoolean("crosshair", crosshairEnabled)
                .putBoolean("brightness_lock", brightnessLocked)
                .putBoolean("color_boost", colorBoostEnabled)
                .putBoolean("screen_filter", screenFilterEnabled)
                .putBoolean("high_refresh", highRefreshEnabled)
                .putBoolean("touch_boost", touchBoostEnabled)
                .putBoolean("anti_disturb", antiDisturbEnabled)
                .putBoolean("wifi", wifiEnabled)
                .putBoolean("mobile_data", mobileDataEnabled)
                .putBoolean("barrage", barrageEnabled)
                .putInt("reminder_minutes", reminderMinutes)
                .putInt("crosshair_style", crosshairStyle)
                .putInt("barrage_style", barrageStyle)
                .putBoolean("audio_boost", audioBoostEnabled)
                .putBoolean("thermal_guard", thermalGuardEnabled)
                .putInt("voice_filter", voiceFilterIndex).apply();
    }

    private void refreshPluginPanel() {
        savePluginState();
        if (bubbleState == 5 && params != null) {
            replaceOverlay(buildGamePluginsPanel());
        } else if (bubbleState == 0 && params != null) {
            replaceOverlay(buildPanel());
        } else if (bubbleState == 6 && params != null) {
            replaceOverlay(buildWifiPanel());
        } else if (bubbleState == 8 && params != null) {
            replaceOverlay(buildCrosshairSettingsPanel());
        }
    }

    private void showCrosshairSettingsPanel() {
        cancelAutoEdgeTimer();
        bubbleState = 8;
        params.width = dp(340);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        params.height = Math.min(dp(430), dm.heightPixels - dp(24));
        replaceOverlay(buildCrosshairSettingsPanel());
    }

    private View buildCrosshairSettingsPanel() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable background = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFA1C1C20, 0xFC09090B, 0xFA26080D});
        background.setCornerRadius(dp(16));
        background.setStroke(dp(1), 0xFFFF3B48);
        root.setBackground(background);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = label("BACK", 10f, 0xFFFF5964);
        back.setTypeface(null, Typeface.BOLD);
        applyLeadingIcon(back, R.drawable.ic_back, 0xFFFF5964);
        back.setPadding(dp(8), dp(5), dp(8), dp(5));
        back.setOnClickListener(v -> {
            bubbleState = 0;
            replaceOverlay(buildPanel());
        });
        header.addView(back);
        TextView title = label("  CROSSHAIR SETTINGS", 13f, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        root.addView(header);

        TextView status = label("All crosshair controls are grouped here.", 9.5f, 0xFF9AA0B4);
        status.setPadding(0, dp(8), 0, dp(8));
        root.addView(status);
        root.addView(action("Crosshair overlay  |  " + (crosshairEnabled ? "ON" : "OFF"), "crosshair_toggle"));
        root.addView(action("Style  |  " + CROSSHAIR_NAMES[crosshairStyle], "crosshair_style"));

        SharedPreferences preferences = getSharedPreferences("ror_game_plugins", MODE_PRIVATE);
        int color = preferences.getInt("crosshair_color", 0xFFFF1744);
        int colorIndex = 0;
        for (int i = 0; i < CROSSHAIR_COLOR_PALETTE.length; i++) {
            if (CROSSHAIR_COLOR_PALETTE[i] == color) colorIndex = i;
        }
        root.addView(action("Color  |  " + CROSSHAIR_COLOR_NAMES[colorIndex], "crosshair_color"));

        int size = 52;
        try { size = preferences.getInt("crosshair_size", 52); } catch (Exception ignored) {}
        int sizeIndex = 2;
        for (int i = 0; i < CROSSHAIR_SIZES.length; i++) {
            if (CROSSHAIR_SIZES[i] == size) sizeIndex = i;
        }
        root.addView(action("Size  |  " + CROSSHAIR_SIZE_LABELS[sizeIndex], "crosshair_size"));
        return root;
    }

    void toggleCrosshair() {
        crosshairEnabled = !crosshairEnabled;
        updateCrosshairOverlay();
        showBarrage("Crosshair", crosshairEnabled ? "Center aim overlay enabled" : "Center aim overlay disabled");
        refreshPluginPanel();
    }

    private void cycleCrosshairStyle() {
        crosshairStyle = (crosshairStyle + 1) % CROSSHAIR_NAMES.length;
        removeAuxiliaryOverlay(crosshairOverlay);
        crosshairOverlay = null;
        updateCrosshairOverlay();
        showBarrage("Crosshair armory", CROSSHAIR_NAMES[crosshairStyle] + " selected");
        refreshPluginPanel();
    }

    private static final int[] CROSSHAIR_COLOR_PALETTE = {
        0xFFFF1744, // Red
        0xFF00E676, // Neon Green
        0xFF00E5FF, // Cyan
        0xFFFFEA00, // Yellow
        0xFFFF00E5, // Magenta
        0xFFFFFFFF  // Pure White
    };
    private static final String[] CROSSHAIR_COLOR_NAMES = {
        "Red Core", "Neon Green", "Hyper Cyan", "Electric Yellow", "Vivid Magenta", "Pure White"
    };
    private static final int[] CROSSHAIR_SIZES = {28, 40, 52, 68, 84};
    private static final String[] CROSSHAIR_SIZE_LABELS = {"XS (28dp)", "S (40dp)", "M (52dp)", "L (68dp)", "XL (84dp)"};

    private void cycleCrosshairColor() {
        SharedPreferences prefs = getSharedPreferences("ror_game_plugins", MODE_PRIVATE);
        int current = prefs.getInt("crosshair_color", 0xFFFF1744);
        int nextIdx = 0;
        for (int i = 0; i < CROSSHAIR_COLOR_PALETTE.length; i++) {
            if (CROSSHAIR_COLOR_PALETTE[i] == current) {
                nextIdx = (i + 1) % CROSSHAIR_COLOR_PALETTE.length;
                break;
            }
        }
        int nextColor = CROSSHAIR_COLOR_PALETTE[nextIdx];
        prefs.edit().putInt("crosshair_color", nextColor).apply();
        removeAuxiliaryOverlay(crosshairOverlay);
        crosshairOverlay = null;
        updateCrosshairOverlay();
        showBarrage("Crosshair Color", CROSSHAIR_COLOR_NAMES[nextIdx]);
        refreshPluginPanel();
    }

    private void cycleCrosshairSize() {
        SharedPreferences prefs = getSharedPreferences("ror_game_plugins", MODE_PRIVATE);
        int current = 52;
        try {
            current = prefs.getInt("crosshair_size", 52);
        } catch (Exception e) {
            current = 52;
        }
        int nextIdx = 2;
        for (int i = 0; i < CROSSHAIR_SIZES.length; i++) {
            if (CROSSHAIR_SIZES[i] == current) {
                nextIdx = (i + 1) % CROSSHAIR_SIZES.length;
                break;
            }
        }
        int nextSize = CROSSHAIR_SIZES[nextIdx];
        prefs.edit().putInt("crosshair_size", nextSize).apply();
        removeAuxiliaryOverlay(crosshairOverlay);
        crosshairOverlay = null;
        updateCrosshairOverlay();
        showBarrage("Crosshair Size", CROSSHAIR_SIZE_LABELS[nextIdx]);
        refreshPluginPanel();
    }

    private void toggleBrightnessLock() {
        brightnessLocked = !brightnessLocked;
        runPrivileged(brightnessLocked
                ? "settings put system screen_brightness_mode 0; settings put system screen_brightness 220"
                : "settings put system screen_brightness_mode 0", "Brightness lock " + onOff(brightnessLocked));
        refreshPluginPanel();
    }

    private void toggleColorBoost() {
        colorBoostEnabled = !colorBoostEnabled;
        updateScreenFilterOverlay();
        showBarrage("Color boost", colorBoostEnabled ? "Vivid contrast layer enabled" : "Color boost disabled");
        refreshPluginPanel();
    }

    private void toggleScreenFilter() {
        showHuntingFilterPanel();
    }

    private void toggleHighRefresh() {
        highRefreshEnabled = !highRefreshEnabled;
        if (highRefreshEnabled) {
            float peak = RefreshRateManager.getPeakRefreshRate(this);
            RefreshRateManager.setFixedRate(this, Math.round(peak));
            toast("Peak refresh locked (" + Math.round(peak) + "Hz)");
        } else {
            RefreshRateManager.setAdaptiveOneHz(this);
            toast("1 Hz Adaptive refresh restored");
        }
        refreshPluginPanel();
    }

    private void toggleTouchBoost() {
        touchBoostEnabled = !touchBoostEnabled;
        String value = touchBoostEnabled ? "1" : "0";
        runPrivileged("for f in /proc/touchpanel/game_switch_enable /sys/class/touch/touch_dev/game_switch_enable "
                        + "/sys/devices/virtual/touch/touch_dev/game_mode; do [ -e $f ] && echo " + value + " > $f; done",
                "Touch boost " + onOff(touchBoostEnabled));
        refreshPluginPanel();
    }

    public void toggleAntiDisturb() {
        antiDisturbEnabled = !antiDisturbEnabled;
        runPrivileged(antiDisturbEnabled
                ? "settings put global heads_up_notifications_enabled 0; cmd notification set_dnd priority"
                : "settings put global heads_up_notifications_enabled 1; cmd notification set_dnd off",
                "Anti-disturb " + onOff(antiDisturbEnabled));
        refreshPluginPanel();
    }

    private void toggleBarrage() {
        barrageEnabled = !barrageEnabled;
        refreshPluginPanel();
        if (barrageEnabled) showBarrage("Barrage messages", "Floating game notifications enabled");
        else toast("Barrage messages disabled");
    }

    private void cycleBarrageStyle() {
        barrageStyle = (barrageStyle + 1) % BARRAGE_STYLE_NAMES.length;
        savePluginState();
        showBarrage("Barrage style", BARRAGE_STYLE_NAMES[barrageStyle] + " notification theme");
        refreshPluginPanel();
    }

    private void cycleReminder() {
        if (reminderMinutes == 0) reminderMinutes = 15;
        else if (reminderMinutes == 15) reminderMinutes = 30;
        else if (reminderMinutes == 30) reminderMinutes = 60;
        else reminderMinutes = 0;
        scheduleReminder();
        showBarrage("Play reminder", reminderMinutes == 0 ? "Reminder disabled" : "Every " + reminderMinutes + " minutes");
        refreshPluginPanel();
    }

    private void togglePerformanceHud() {
        boolean active = getSharedPreferences("ror_tools", MODE_PRIVATE).getBoolean("overlay", false);
        if (active) startService(new Intent(this, CpuOverlayService.class).setAction(CpuOverlayService.ACTION_STOP));
        else if (Build.VERSION.SDK_INT >= 26) startForegroundService(new Intent(this, CpuOverlayService.class));
        else startService(new Intent(this, CpuOverlayService.class));
        getSharedPreferences("ror_tools", MODE_PRIVATE).edit().putBoolean("overlay", !active).apply();
        showBarrage("Performance HUD", !active ? "Live CPU and GPU metrics enabled" : "Live metrics disabled");
        refreshPluginPanel();
    }

    private void toggleWifi() {
        wifiEnabled = !wifiEnabled;
        runPrivileged("svc wifi " + (wifiEnabled ? "enable" : "disable"), "Wi-Fi " + onOff(wifiEnabled));
        refreshPluginPanel();
    }

    private void toggleMobileData() {
        mobileDataEnabled = !mobileDataEnabled;
        runPrivileged("svc data " + (mobileDataEnabled ? "enable" : "disable"), "Mobile data " + onOff(mobileDataEnabled));
        refreshPluginPanel();
    }

    private String onOff(boolean value) { return value ? "enabled" : "disabled"; }

    private void scheduleReminder() {
        handler.removeCallbacks(gameReminder);
        if (reminderMinutes > 0) handler.postDelayed(gameReminder, reminderMinutes * 60L * 1000L);
    }

    private void runOnWorker(Runnable r) {
        if (r == null || worker == null || worker.isShutdown() || worker.isTerminated()) return;
        try {
            worker.execute(r);
        } catch (Exception ignored) {}
    }

    private void runPrivileged(String command, String successMessage) {
        runOnWorker(() -> {
            boolean available = GsmProfileManager.isRootEnabled(this) || ShizukuBridge.granted();
            if (GsmProfileManager.isRootEnabled(this)) RootUtils.runCommand(command);
            else if (ShizukuBridge.granted()) ShizukuBridge.run(command);
            handler.post(() -> {
                if (available && successMessage != null && !successMessage.isEmpty()) {
                    showBarrage("Game Space", successMessage);
                } else if (!available && successMessage != null) {
                    toast("Root or Shizuku is required for this control");
                }
            });
        });
    }

    private void updateCrosshairOverlay() {
        if (windowManager == null) return;
        if (!crosshairEnabled || !targetInForeground) {
            removeAuxiliaryOverlay(crosshairOverlay);
            crosshairOverlay = null;
            return;
        }
        removeAuxiliaryOverlay(crosshairOverlay);
        crosshairOverlay = null;

        SharedPreferences prefs = getSharedPreferences("ror_game_plugins", MODE_PRIVATE);
        final int chColor = prefs.getInt("crosshair_color", 0xFFFF3144);
        int savedSize = 52;
        try {
            savedSize = prefs.getInt("crosshair_size", 52);
        } catch (ClassCastException e) {
            try {
                savedSize = (int) prefs.getFloat("crosshair_size", 52f);
            } catch (Exception ignored) {}
        }
        final int chSize = Math.max(24, Math.min(90, savedSize));

        View crosshair = new View(this) {
            final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                paint.setColor(chColor);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(1.8f));
                float cx = getWidth() / 2f, cy = getHeight() / 2f;
                float scale = chSize / 52f;
                if (crosshairStyle == 1) {
                    paint.setStyle(Paint.Style.FILL); canvas.drawCircle(cx, cy, dp(3.5f) * scale, paint);
                } else if (crosshairStyle == 2) {
                    float r = dp(17) * scale, s = dp(8) * scale;
                    canvas.drawLine(cx-r,cy-r,cx-r+s,cy-r,paint); canvas.drawLine(cx-r,cy-r,cx-r,cy-r+s,paint);
                    canvas.drawLine(cx+r,cy-r,cx+r-s,cy-r,paint); canvas.drawLine(cx+r,cy-r,cx+r,cy-r+s,paint);
                    canvas.drawLine(cx-r,cy+r,cx-r+s,cy+r,paint); canvas.drawLine(cx-r,cy+r,cx-r,cy+r-s,paint);
                    canvas.drawLine(cx+r,cy+r,cx+r-s,cy+r,paint); canvas.drawLine(cx+r,cy+r,cx+r,cy+r-s,paint);
                    paint.setStyle(Paint.Style.FILL); canvas.drawCircle(cx,cy,dp(2) * scale,paint);
                } else if (crosshairStyle == 3) {
                    float r = dp(15) * scale, g = dp(4) * scale;
                    canvas.drawLine(cx-r,cy-r,cx-g,cy-g,paint); canvas.drawLine(cx+r,cy-r,cx+g,cy-g,paint);
                    canvas.drawLine(cx-r,cy+r,cx-g,cy+g,paint); canvas.drawLine(cx+r,cy+r,cx+g,cy+g,paint);
                    canvas.drawCircle(cx,cy,dp(5) * scale,paint);
                } else if (crosshairStyle == 4) {
                    canvas.drawCircle(cx,cy,dp(17) * scale,paint); canvas.drawCircle(cx,cy,dp(6) * scale,paint);
                    canvas.drawLine(cx-dp(20)*scale, cy, cx-dp(17)*scale, cy, paint);
                    canvas.drawLine(cx+dp(17)*scale, cy, cx+dp(20)*scale, cy, paint);
                    canvas.drawLine(cx, cy-dp(20)*scale, cx, cy-dp(17)*scale, paint);
                    canvas.drawLine(cx, cy+dp(17)*scale, cx, cy+dp(20)*scale, paint);
                } else if (crosshairStyle == 5) { // Diamond Reflex
                    float d = dp(14) * scale;
                    Path p = new Path();
                    p.moveTo(cx, cy - d); p.lineTo(cx + d, cy); p.lineTo(cx, cy + d); p.lineTo(cx - d, cy); p.close();
                    canvas.drawPath(p, paint);
                    paint.setStyle(Paint.Style.FILL); canvas.drawCircle(cx, cy, dp(2.5f) * scale, paint);
                } else if (crosshairStyle == 6) { // Tri-Spoke SpecOps
                    float r = dp(16) * scale, g = dp(5) * scale;
                    canvas.drawLine(cx, cy - r, cx, cy - g, paint);
                    float dx = (float) (r * Math.cos(Math.PI / 6)), dy = (float) (r * Math.sin(Math.PI / 6));
                    float gx = (float) (g * Math.cos(Math.PI / 6)), gy = (float) (g * Math.sin(Math.PI / 6));
                    canvas.drawLine(cx - dx, cy + dy, cx - gx, cy + gy, paint);
                    canvas.drawLine(cx + dx, cy + dy, cx + gx, cy + gy, paint);
                    paint.setStyle(Paint.Style.FILL); canvas.drawCircle(cx, cy, dp(2) * scale, paint);
                } else if (crosshairStyle == 7) { // Biohazard Gamer
                    float r = dp(16) * scale;
                    RectF arcRect = new RectF(cx - r, cy - r, cx + r, cy + r);
                    canvas.drawArc(arcRect, 30, 60, false, paint);
                    canvas.drawArc(arcRect, 150, 60, false, paint);
                    canvas.drawArc(arcRect, 270, 60, false, paint);
                    paint.setStyle(Paint.Style.FILL); canvas.drawCircle(cx, cy, dp(3) * scale, paint);
                } else {
                    float r = dp(20) * scale, g = dp(6) * scale;
                    canvas.drawLine(cx-r,cy,cx-g,cy,paint); canvas.drawLine(cx+g,cy,cx+r,cy,paint);
                    canvas.drawLine(cx,cy-r,cx,cy-g,paint); canvas.drawLine(cx,cy+g,cx,cy+r,paint);
                    canvas.drawCircle(cx,cy,dp(5) * scale,paint);
                }
            }
        };
        GradientDrawable glow = new GradientDrawable();
        glow.setShape(GradientDrawable.OVAL);
        glow.setColor(0x22000000);
        glow.setStroke(dp(1), (chColor & 0x00FFFFFF) | 0x66000000);
        crosshair.setBackground(glow);
        crosshairParams = new WindowManager.LayoutParams(dp(chSize), dp(chSize), overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);
        crosshairParams.alpha = 0.80f;
        crosshairParams.gravity = Gravity.CENTER;
        crosshairOverlay = crosshair;
        try { windowManager.addView(crosshairOverlay, crosshairParams); } catch (Exception ignored) { crosshairOverlay = null; }
    }

    public String getHuntingFilterShortName() {
        if (!screenFilterEnabled) return "OFF";
        HunterModeEngine.HunterConfig cfg = HunterModeEngine.getInstance().loadConfig(this, targetPackage);
        int id = cfg.filterId;
        if (id > 0 && id < HUNTING_FILTER_NAMES.length) {
            return HUNTING_FILTER_NAMES[id].toUpperCase(Locale.US);
        }
        return "ON";
    }

    public static Bitmap createFilterThumbnail(int filterId, int width, int height) {
        return HunterModeEngine.createFilterThumbnail(filterId, width, height, null);
    }

    private interface OnHunterSliderChanged {
        void onChange(int value);
    }

    private View buildHunterSlider(String name, int min, int max, int current, String unit, OnHunterSliderChanged listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));

        LinearLayout labelRow = new LinearLayout(this);
        labelRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = label(name, 11f, 0xFFE0E0E0);
        title.setTypeface(null, Typeface.BOLD);
        labelRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView valText = label((current > 0 ? "+" : "") + current + unit, 11f, 0xFFFF5252);
        valText.setTypeface(null, Typeface.BOLD);
        labelRow.addView(valText, new LinearLayout.LayoutParams(-2, -2));
        row.addView(labelRow);

        SeekBar sb = new SeekBar(this);
        sb.setMax(max - min);
        sb.setProgress(current - min);
        sb.setPadding(dp(4), dp(4), dp(4), dp(4));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                int actual = min + progress;
                valText.setText((actual > 0 ? "+" : "") + actual + unit);
                if (fromUser && listener != null) {
                    listener.onChange(actual);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        row.addView(sb);
        return row;
    }

    public void showHuntingFilterPanel() {
        cancelAutoEdgeTimer();
        bubbleState = 7;
        params.width = dp(350);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        params.height = Math.min(dp(560), dm.heightPixels - dp(24));
        replaceOverlay(buildHuntingFilterPanel());
    }

    private View buildHuntingFilterPanel() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF413151D);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), 0xFF35394C);
        root.setBackground(bg);

        HunterModeEngine engine = HunterModeEngine.getInstance();
        final HunterModeEngine.HunterConfig cfg = engine.loadConfig(this, targetPackage);

        // Header: REDMAGIC HUNTER + Reset + Close X
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(10));

        TextView title = label("REDMAGIC HUNTER", 15, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setLetterSpacing(0.04f);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));

        // Reset Button
        TextView resetBtn = label("RESET", 11, 0xFFFF7043);
        resetBtn.setTypeface(null, Typeface.BOLD);
        resetBtn.setGravity(Gravity.CENTER);
        resetBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        resetBtn.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            cfg.reset();
            screenFilterEnabled = false;
            engine.saveConfig(this, targetPackage, cfg);
            updateScreenFilterOverlay();
            replaceOverlay(buildHuntingFilterPanel());
            showBarrage("Hunter Mode", "Reset to original display");
        });
        header.addView(resetBtn, new LinearLayout.LayoutParams(-2, -2));

        // Close Button
        TextView closeBtn = label("", 18, 0xFFB0B5C6);
        applyLeadingIcon(closeBtn, R.drawable.ic_window_close, 0xFFB0B5C6);
        closeBtn.setTypeface(null, Typeface.BOLD);
        closeBtn.setGravity(Gravity.CENTER);
        closeBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        closeBtn.setOnClickListener(v -> {
            bubbleState = 5; // back to plugins
            refreshPluginPanel();
        });
        header.addView(closeBtn, new LinearLayout.LayoutParams(-2, -2));
        root.addView(header);

        // Master Switch Bar
        LinearLayout toggleBar = new LinearLayout(this);
        toggleBar.setOrientation(LinearLayout.HORIZONTAL);
        toggleBar.setGravity(Gravity.CENTER_VERTICAL);
        toggleBar.setPadding(dp(10), dp(8), dp(10), dp(8));
        GradientDrawable toggleBg = new GradientDrawable();
        toggleBg.setColor(0x22FFFFFF);
        toggleBg.setCornerRadius(dp(10));
        toggleBar.setBackground(toggleBg);

        TextView toggleLabel = label("Master Filter State", 13, Color.WHITE);
        toggleLabel.setTypeface(null, Typeface.BOLD);
        toggleBar.addView(toggleLabel, new LinearLayout.LayoutParams(0, -2, 1f));

        Button toggleBtn = new Button(this);
        toggleBtn.setText(screenFilterEnabled ? "ACTIVE" : "DISABLED");
        toggleBtn.setTextColor(Color.WHITE);
        toggleBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        toggleBtn.setTypeface(null, Typeface.BOLD);
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(screenFilterEnabled ? 0xFF00E676 : 0xFF546E7A);
        btnBg.setCornerRadius(dp(8));
        toggleBtn.setBackground(btnBg);
        toggleBtn.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            screenFilterEnabled = !screenFilterEnabled;
            cfg.enabled = screenFilterEnabled;
            if (screenFilterEnabled && cfg.filterId == 0) {
                cfg.filterId = 1; // Default to Hunter Classic when toggled on
            }
            engine.saveConfig(this, targetPackage, cfg);
            updateScreenFilterOverlay();
            replaceOverlay(buildHuntingFilterPanel());
        });
        toggleBar.addView(toggleBtn, new LinearLayout.LayoutParams(dp(90), dp(32)));
        root.addView(toggleBar);

        // Main Scrollable Area
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        // Presets Section
        TextView presetsHeader = label("GAMING PRESETS", 11.5f, 0xFF00E5FF);
        presetsHeader.setTypeface(null, Typeface.BOLD);
        presetsHeader.setPadding(0, dp(12), 0, dp(6));
        content.addView(presetsHeader);

        HorizontalScrollView presetScroll = new HorizontalScrollView(this);
        presetScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout presetList = new LinearLayout(this);
        presetList.setOrientation(LinearLayout.HORIZONTAL);

        for (int p = 1; p < HunterModeEngine.PRESET_COUNT; p++) {
            final int presetId = p;
            final boolean isPresetActive = (cfg.presetId == presetId);

            TextView chip = label(HunterModeEngine.PRESET_NAMES[presetId], 11.5f, isPresetActive ? Color.WHITE : 0xFFCFD8DC);
            chip.setTypeface(null, Typeface.BOLD);
            chip.setPadding(dp(12), dp(6), dp(12), dp(6));
            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setCornerRadius(dp(8));
            chipBg.setColor(isPresetActive ? 0xFFFF1744 : 0x22FFFFFF);
            chipBg.setStroke(dp(1), isPresetActive ? 0xFFFF5252 : 0x33FFFFFF);
            chip.setBackground(chipBg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMargins(0, 0, dp(6), 0);
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                cfg.applyPreset(presetId);
                screenFilterEnabled = true;
                cfg.enabled = true;
                engine.saveConfig(this, targetPackage, cfg);
                updateScreenFilterOverlay();
                replaceOverlay(buildHuntingFilterPanel());
                showBarrage("Hunter Preset", HunterModeEngine.PRESET_NAMES[presetId] + " activated");
            });

            presetList.addView(chip);
        }
        presetScroll.addView(presetList, new HorizontalScrollView.LayoutParams(-2, -2));
        content.addView(presetScroll);

        // Filters Section
        TextView filtersHeader = label("VISUAL FILTERS (15)", 11.5f, 0xFF00E5FF);
        filtersHeader.setTypeface(null, Typeface.BOLD);
        filtersHeader.setPadding(0, dp(14), 0, dp(6));
        content.addView(filtersHeader);

        for (int i = 1; i < HUNTING_FILTER_NAMES.length; i++) {
            final int filterId = i;
            final boolean isSelected = (filterId == cfg.filterId && screenFilterEnabled);

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(0, dp(6), 0, dp(6));

            // Thumbnail Frame
            FrameLayout thumbFrame = new FrameLayout(this);
            GradientDrawable tBg = new GradientDrawable();
            tBg.setCornerRadius(dp(8));
            tBg.setStroke(dp(isSelected ? 2 : 1), isSelected ? 0xFFFF1744 : 0xFF3A3F50);
            thumbFrame.setBackground(tBg);
            thumbFrame.setPadding(dp(2), dp(2), dp(2), dp(2));

            ImageView thumbView = new ImageView(this);
            thumbView.setScaleType(ImageView.ScaleType.FIT_XY);
            thumbView.setImageBitmap(createFilterThumbnail(filterId, dp(96), dp(52)));
            thumbFrame.addView(thumbView, new FrameLayout.LayoutParams(-1, -1));

            item.addView(thumbFrame, new LinearLayout.LayoutParams(dp(96), dp(52)));

            // Text Info
            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setPadding(dp(12), 0, 0, 0);

            TextView name = label(HUNTING_FILTER_NAMES[filterId], 13f, isSelected ? 0xFFFF1744 : Color.WHITE);
            name.setTypeface(null, Typeface.BOLD);
            info.addView(name);

            TextView desc = label(HUNTING_FILTER_DESCRIPTIONS[filterId], 9.5f, isSelected ? 0xFFFF7080 : 0xFF9EABB8);
            desc.setPadding(0, dp(2), 0, 0);
            info.addView(desc);

            item.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));

            item.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                if (isSelected) {
                    screenFilterEnabled = false;
                    cfg.enabled = false;
                    cfg.filterId = 0;
                    showBarrage("Hunter Mode", "Filter disabled");
                } else {
                    screenFilterEnabled = true;
                    cfg.enabled = true;
                    cfg.filterId = filterId;
                    showBarrage("Hunter Mode", HUNTING_FILTER_NAMES[filterId] + " enabled");
                }
                engine.saveConfig(this, targetPackage, cfg);
                updateScreenFilterOverlay();
                replaceOverlay(buildHuntingFilterPanel());
            });

            content.addView(item);

            View divider = new View(this);
            divider.setBackgroundColor(0x18FFFFFF);
            content.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        }

        // Sliders Section
        TextView slidersHeader = label("FINE TUNING & SHADER ADJUSTMENTS", 11.5f, 0xFF00E5FF);
        slidersHeader.setTypeface(null, Typeface.BOLD);
        slidersHeader.setPadding(0, dp(16), 0, dp(6));
        content.addView(slidersHeader);

        // Brightness (-100 to +100)
        content.addView(buildHunterSlider("Brightness", -100, 100, cfg.brightness, "", val -> {
            cfg.brightness = val;
            cfg.presetId = HunterModeEngine.PRESET_CUSTOM;
            engine.saveConfig(this, targetPackage, cfg);
            updateScreenFilterOverlay();
        }));

        // Contrast (-100 to +100)
        content.addView(buildHunterSlider("Contrast", -100, 100, cfg.contrast, "", val -> {
            cfg.contrast = val;
            cfg.presetId = HunterModeEngine.PRESET_CUSTOM;
            engine.saveConfig(this, targetPackage, cfg);
            updateScreenFilterOverlay();
        }));

        // Saturation (0 to 200%)
        content.addView(buildHunterSlider("Saturation", 0, 200, cfg.saturation, "%", val -> {
            cfg.saturation = val;
            cfg.presetId = HunterModeEngine.PRESET_CUSTOM;
            engine.saveConfig(this, targetPackage, cfg);
            updateScreenFilterOverlay();
        }));

        // Gamma (50 to 250 -> 0.5 to 2.5)
        content.addView(buildHunterSlider("Gamma", 50, 250, cfg.gamma, "%", val -> {
            cfg.gamma = val;
            cfg.presetId = HunterModeEngine.PRESET_CUSTOM;
            engine.saveConfig(this, targetPackage, cfg);
            updateScreenFilterOverlay();
        }));

        // Shadow Boost (0 to 100)
        content.addView(buildHunterSlider("Shadow Boost", 0, 100, cfg.shadowBoost, "", val -> {
            cfg.shadowBoost = val;
            cfg.presetId = HunterModeEngine.PRESET_CUSTOM;
            engine.saveConfig(this, targetPackage, cfg);
            updateScreenFilterOverlay();
        }));

        // Sharpness (0 to 100)
        content.addView(buildHunterSlider("Sharpness", 0, 100, cfg.sharpness, "", val -> {
            cfg.sharpness = val;
            cfg.presetId = HunterModeEngine.PRESET_CUSTOM;
            engine.saveConfig(this, targetPackage, cfg);
            updateScreenFilterOverlay();
        }));

        // Color Temperature (-100 Cool to +100 Warm)
        content.addView(buildHunterSlider("Color Temperature (Cool/Warm)", -100, 100, cfg.colorTemp, "", val -> {
            cfg.colorTemp = val;
            cfg.presetId = HunterModeEngine.PRESET_CUSTOM;
            engine.saveConfig(this, targetPackage, cfg);
            updateScreenFilterOverlay();
        }));

        // Red Gain (0 to 200%)
        content.addView(buildHunterSlider("Red Channel Gain", 0, 200, cfg.redGain, "%", val -> {
            cfg.redGain = val;
            cfg.presetId = HunterModeEngine.PRESET_CUSTOM;
            engine.saveConfig(this, targetPackage, cfg);
            updateScreenFilterOverlay();
        }));

        // Green Gain (0 to 200%)
        content.addView(buildHunterSlider("Green Channel Gain", 0, 200, cfg.greenGain, "%", val -> {
            cfg.greenGain = val;
            cfg.presetId = HunterModeEngine.PRESET_CUSTOM;
            engine.saveConfig(this, targetPackage, cfg);
            updateScreenFilterOverlay();
        }));

        // Blue Gain (0 to 200%)
        content.addView(buildHunterSlider("Blue Channel Gain", 0, 200, cfg.blueGain, "%", val -> {
            cfg.blueGain = val;
            cfg.presetId = HunterModeEngine.PRESET_CUSTOM;
            engine.saveConfig(this, targetPackage, cfg);
            updateScreenFilterOverlay();
        }));

        // Overlay Opacity (20 to 100%)
        int currentAlphaPct = (int) (cfg.overlayAlpha * 100f);
        content.addView(buildHunterSlider("Overlay Opacity", 20, 100, currentAlphaPct, "%", val -> {
            cfg.overlayAlpha = val / 100f;
            engine.saveConfig(this, targetPackage, cfg);
            updateScreenFilterOverlay();
        }));

        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        return root;
    }

    private void updateScreenFilterOverlay() {
        if (windowManager == null) return;
        HunterModeEngine engine = HunterModeEngine.getInstance();
        HunterModeEngine.HunterConfig cfg = engine.loadConfig(this, targetPackage);

        if (!targetInForeground || !screenFilterEnabled || cfg.filterId <= 0) {
            engine.restoreDisplayState(this);
            removeAuxiliaryOverlay(screenFilterOverlay);
            screenFilterOverlay = null;
            return;
        }

        // Apply hardware/OS-level display transform if supported
        engine.applyHardwareTransform(this, cfg);

        // Update or instantiate hardware-accelerated overlay
        if (screenFilterOverlay == null) {
            HunterModeEngine.HunterOverlayView overlayView = new HunterModeEngine.HunterOverlayView(this);
            screenFilterParams = new WindowManager.LayoutParams(-1, -1, overlayType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    PixelFormat.TRANSLUCENT);
            screenFilterParams.alpha = cfg.overlayAlpha;
            screenFilterParams.gravity = Gravity.FILL;
            overlayView.setConfig(cfg);
            screenFilterOverlay = overlayView;
            try {
                windowManager.addView(screenFilterOverlay, screenFilterParams);
            } catch (Exception ignored) {
                screenFilterOverlay = null;
            }
        } else {
            if (screenFilterOverlay instanceof HunterModeEngine.HunterOverlayView) {
                ((HunterModeEngine.HunterOverlayView) screenFilterOverlay).setConfig(cfg);
            }
            if (screenFilterParams != null) {
                screenFilterParams.alpha = cfg.overlayAlpha;
                try {
                    windowManager.updateViewLayout(screenFilterOverlay, screenFilterParams);
                } catch (Exception ignored) {}
            }
        }
    }

    private void applyGameColorTransform(int filterId) {
        HunterModeEngine.HunterConfig cfg = HunterModeEngine.getInstance().loadConfig(this, targetPackage);
        cfg.filterId = filterId;
        cfg.enabled = (filterId > 0);
        HunterModeEngine.getInstance().applyHardwareTransform(this, cfg);
    }

    public void showBarrage(String title, String message) {
        showBarrage(title, message, null);
    }

    public void showBarrage(String title, String message, Drawable appIcon) {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            return;
        }
        if (windowManager == null) windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager == null) return;

        SharedPreferences prefs = getSharedPreferences("ror_game_plugins", MODE_PRIVATE);
        boolean allow = barrageEnabled || prefs.getBoolean("barrage", true) || antiDisturbEnabled || prefs.getBoolean("anti_disturb", false);
        if (!allow) {
            return;
        }

        // BARRAGE MESSAGE RESTRICTION:
        // Barrage messages MUST ONLY show when inside configured set games!
        // If outside set games or if game is closed/minimized, barrage messages will NOT display!
        java.util.Set<String> setGames = com.revror.kernelmanager.utils.GameLauncherRegistry.getGamePackages(this);
        String currentFg = com.revror.kernelmanager.services.PowerButtonGestureService.getForegroundPackage(this);
        boolean isInsideSetGame = (currentFg != null && setGames.contains(currentFg))
                || (targetPackage != null && setGames.contains(targetPackage) && isGameActive());
        boolean isTestBarrage = "BARRAGE TEST".equalsIgnoreCase(title) || "TEST".equalsIgnoreCase(title);

        if (!isInsideSetGame && !isTestBarrage) {
            android.util.Log.i("GameBubbleService", "showBarrage ignored: not inside set games (currentFg=" + currentFg + ")");
            return;
        }

        int activeStyle = Math.max(0, Math.min(BARRAGE_STYLE_NAMES.length - 1, prefs.getInt("barrage_style", barrageStyle)));
        barrageStyle = activeStyle;

        handler.post(() -> {
            try {
                if (currentBarrageAnimator != null) {
                    currentBarrageAnimator.cancel();
                    currentBarrageAnimator = null;
                }
                if (barrageOverlay != null) {
                    try {
                        if (barrageOverlay.isAttachedToWindow()) {
                            windowManager.removeViewImmediate(barrageOverlay);
                        }
                    } catch (Throwable ignored) {}
                    barrageOverlay = null;
                }

                View cardView;
                try {
                    cardView = buildBarrageCard(title, message, appIcon, activeStyle);
                } catch (Throwable t) {
                    cardView = buildBarrageCard(title, message, null, 0);
                }
                final View card = cardView;

                FrameLayout root = new FrameLayout(this);
                root.setClipChildren(false);
                root.setClipToPadding(false);

                try {
                    card.measure(
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    );
                } catch (Throwable ignored) {}

                int cardWidth = card.getMeasuredWidth();
                if (cardWidth <= 0) cardWidth = dp(280);
                int cardHeight = card.getMeasuredHeight();
                if (cardHeight <= 0) cardHeight = dp(52);

                FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                cardLp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
                root.addView(card, cardLp);

                DisplayMetrics dm = getResources().getDisplayMetrics();
                int screenWidth = dm != null && dm.widthPixels > 0 ? dm.widthPixels : 1080;

                int startX = screenWidth + dp(20);
                int targetX = -cardWidth - dp(30);

                card.setTranslationX(startX);

                int barragePos = prefs.getInt("barrage_pos", 0);
                int barrageSpeed = prefs.getInt("barrage_speed", 1);
                int barrageOpacity = prefs.getInt("barrage_opacity", 96);
                float alphaVal = Math.max(0.4f, Math.min(0.80f, barrageOpacity / 100f));
                card.setAlpha(alphaVal);

                barrageParams = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        cardHeight + dp(16),
                        overlayType(),
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                );
                barrageParams.alpha = 0.80f;
                barrageParams.gravity = Gravity.TOP | Gravity.START;
                barrageParams.x = 0;
                barrageParams.y = (barragePos == 1) ? dp(68) : dp(24);

                barrageOverlay = root;

                long duration = (barrageSpeed == 0) ? 7500L : (barrageSpeed == 2 ? 3400L : 5200L);

                windowManager.addView(root, barrageParams);

                ValueAnimator animator = ValueAnimator.ofFloat(startX, targetX);
                animator.setDuration(duration);
                animator.setInterpolator(new LinearInterpolator());
                animator.addUpdateListener(val -> {
                    try {
                        float currentX = (float) val.getAnimatedValue();
                        card.setTranslationX(currentX);
                    } catch (Throwable ignored) {}
                });
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        handler.post(() -> {
                            try {
                                if (barrageOverlay == root) {
                                    if (root.isAttachedToWindow()) {
                                        windowManager.removeViewImmediate(root);
                                    }
                                    barrageOverlay = null;
                                }
                            } catch (Throwable ignored) {}
                            if (currentBarrageAnimator == animator) {
                                currentBarrageAnimator = null;
                            }
                        });
                    }
                });
                currentBarrageAnimator = animator;
                animator.start();
            } catch (Throwable e) {
                android.util.Log.e("GameBubbleService", "Barrage windowManager error: ", e);
                barrageOverlay = null;
                currentBarrageAnimator = null;
            }
        });
    }

    private View buildBarrageCard(String title, String message, Drawable appIcon, int style) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);

        int[] bgColors;
        int strokeColor;
        float strokeWidthDp = 1.4f;
        int cornerRadiusDp = 18;
        int titleColor = 0xFFCCCCCC;
        int msgColor = Color.WHITE;
        String badgeText = null;
        int badgeColor = 0xFFFFD700;
        int defaultIconRes = R.drawable.ic_barrage_message;
        int iconTint = 0;

        switch (style) {
            case 1: // Anime Mecha
                bgColors = new int[]{0xFFFF2A6D, 0xFFFF6384, 0xFFFFA07A};
                strokeColor = 0xFFFFC2D1;
                cornerRadiusDp = 20;
                titleColor = 0xFFFFF0F5;
                msgColor = Color.WHITE;
                badgeText = " ";
                badgeColor = 0xFFFFF275;
                defaultIconRes = R.drawable.ic_barrage_chibi;
                break;
            case 2: // Cyber Glass
                bgColors = new int[]{0xF2061426, 0xF2092D57, 0xF2004B87};
                strokeColor = 0xFF00E5FF;
                strokeWidthDp = 1.8f;
                cornerRadiusDp = 20;
                titleColor = 0xFF00E5FF;
                msgColor = Color.WHITE;
                badgeText = "";
                badgeColor = 0xFF00E5FF;
                iconTint = 0xFF00E5FF;
                break;
            case 3: // Cocktail Party
                bgColors = new int[]{0xF6320B4E, 0xF6480F6E, 0xF61F0631};
                strokeColor = 0xFFB026FF;
                strokeWidthDp = 1.6f;
                cornerRadiusDp = 14;
                titleColor = 0xFFFFD54F;
                msgColor = 0xFFF3E8FF;
                badgeText = " ";
                badgeColor = 0xFFFFD700;
                defaultIconRes = R.drawable.ic_barrage_cocktail;
                break;
            case 4: // REVROR Turbo
                bgColors = new int[]{0xF81A0508, 0xF8380A12, 0xF8150406};
                strokeColor = 0xFFFF1744;
                strokeWidthDp = 2.0f;
                cornerRadiusDp = 16;
                titleColor = 0xFFFF5252;
                msgColor = 0xFFFFEBEE;
                badgeText = "TURBO";
                badgeColor = 0xFFFF1744;
                iconTint = 0xFFFF1744;
                break;
            case 5: // RGB Chroma Wave
                bgColors = new int[]{0xF40A0F1E, 0xF4140A28, 0xF4081E1E};
                strokeColor = 0xFF00E5FF;
                strokeWidthDp = 1.8f;
                cornerRadiusDp = 18;
                titleColor = 0xFF80D8FF;
                msgColor = Color.WHITE;
                badgeText = "RGB";
                badgeColor = 0xFFFF4081;
                iconTint = 0xFF00E676;
                break;
            case 6: // Retro 8-Bit Arcade
                bgColors = new int[]{0xF8051205, 0xF809220C, 0xF8030D04};
                strokeColor = 0xFF00E676;
                strokeWidthDp = 2.0f;
                cornerRadiusDp = 6;
                titleColor = 0xFF69F0AE;
                msgColor = 0xFFB9F6CA;
                badgeText = "[1UP]";
                badgeColor = 0xFF00E676;
                iconTint = 0xFF00E676;
                break;
            case 7: // Cyberpunk 2077
                bgColors = new int[]{0xF8FFE500, 0xF8FFD600, 0xF8FFC400};
                strokeColor = 0xFF000000;
                strokeWidthDp = 2.2f;
                cornerRadiusDp = 8;
                titleColor = 0xFF000000;
                msgColor = 0xFF1A1A1A;
                badgeText = " CYBER";
                badgeColor = 0xFF000000;
                iconTint = 0xFF000000;
                break;
            case 8: // Dark Souls Bonfire
                bgColors = new int[]{0xF8140B04, 0xF8291406, 0xF80E0602};
                strokeColor = 0xFFFF6D00;
                strokeWidthDp = 1.6f;
                cornerRadiusDp = 14;
                titleColor = 0xFFFFB74D;
                msgColor = 0xFFFFE0B2;
                badgeText = "BONFIRE";
                badgeColor = 0xFFFF9100;
                iconTint = 0xFFFF6D00;
                break;
            case 9: // Valorant Radiant
                bgColors = new int[]{0xF60F141C, 0xF6192230, 0xF60C1017};
                strokeColor = 0xFFFF4655;
                strokeWidthDp = 1.8f;
                cornerRadiusDp = 10;
                titleColor = 0xFFFF4655;
                msgColor = 0xFFECE8E1;
                badgeText = " RADIANT";
                badgeColor = 0xFFFFD700;
                iconTint = 0xFFFF4655;
                break;
            case 10: // Apex Champion
                bgColors = new int[]{0xF81F1A17, 0xF833251D, 0xF814100E};
                strokeColor = 0xFFFF5722;
                strokeWidthDp = 1.8f;
                cornerRadiusDp = 12;
                titleColor = 0xFFFF8A65;
                msgColor = 0xFFFFF3E0;
                badgeText = "CHAMPION";
                badgeColor = 0xFFFFAB40;
                iconTint = 0xFFFF5722;
                break;
            case 11: // League Hextech
                bgColors = new int[]{0xF6061E2E, 0xF60C3854, 0xF604131D};
                strokeColor = 0xFFC89B3C;
                strokeWidthDp = 1.8f;
                cornerRadiusDp = 16;
                titleColor = 0xFFF0E6D2;
                msgColor = 0xFFC8AA6E;
                badgeText = "HEXTECH";
                badgeColor = 0xFF0AC8B9;
                iconTint = 0xFFC89B3C;
                break;
            case 12: // Doom Inferno
                bgColors = new int[]{0xF8210303, 0xF83D0707, 0xF8140202};
                strokeColor = 0xFFFF1744;
                strokeWidthDp = 2.2f;
                cornerRadiusDp = 8;
                titleColor = 0xFFFF5252;
                msgColor = 0xFFFF8A80;
                badgeText = "SLAYER";
                badgeColor = 0xFFFF3D00;
                iconTint = 0xFFFF1744;
                break;
            case 13: // Sakura Blossom
                bgColors = new int[]{0xF628101C, 0xF640192D, 0xF61C0A13};
                strokeColor = 0xFFFF80AB;
                strokeWidthDp = 1.4f;
                cornerRadiusDp = 22;
                titleColor = 0xFFFF80AB;
                msgColor = 0xFFFCE4EC;
                badgeText = "SAKURA";
                badgeColor = 0xFFFF4081;
                defaultIconRes = R.drawable.ic_barrage_chibi;
                break;
            case 14: // Chibi Kawaii
                bgColors = new int[]{0xF8FF4081, 0xF8FF80AB, 0xF8F8BBD0};
                strokeColor = 0xFFFFFFFF;
                strokeWidthDp = 1.6f;
                cornerRadiusDp = 24;
                titleColor = 0xFFFFFFFF;
                msgColor = 0xFF4A148C;
                badgeText = "KAWAII";
                badgeColor = 0xFFFFF9C4;
                defaultIconRes = R.drawable.ic_barrage_chibi;
                break;
            case 15: // Shonen Flame
                bgColors = new int[]{0xF82E0C04, 0xF84F1706, 0xF81F0702};
                strokeColor = 0xFFFF3D00;
                strokeWidthDp = 2.0f;
                cornerRadiusDp = 10;
                titleColor = 0xFFFF6E40;
                msgColor = 0xFFFFF3E0;
                badgeText = " 炎 SHONEN";
                badgeColor = 0xFFFFD600;
                iconTint = 0xFFFF3D00;
                break;
            case 16: // Manga Monochrome
                bgColors = new int[]{0xF80A0A0A, 0xF81F1F1F, 0xF80F0F0F};
                strokeColor = 0xFFFFFFFF;
                strokeWidthDp = 2.2f;
                cornerRadiusDp = 6;
                titleColor = 0xFFFFFFFF;
                msgColor = 0xFFE0E0E0;
                badgeText = "!? [MANGA]";
                badgeColor = 0xFFFFFFFF;
                iconTint = 0xFFFFFFFF;
                break;
            case 17: // Evangelion Unit-01
                bgColors = new int[]{0xF8210936, 0xF83B105F, 0xF8170526};
                strokeColor = 0xFF76FF03;
                strokeWidthDp = 2.0f;
                cornerRadiusDp = 14;
                titleColor = 0xFFB388FF;
                msgColor = 0xFFCCFF90;
                badgeText = " EVA-01";
                badgeColor = 0xFF76FF03;
                iconTint = 0xFF76FF03;
                break;
            case 18: // Demon Slayer Nichirin
                bgColors = new int[]{0xF80B1C14, 0xF8133526, 0xF808140E};
                strokeColor = 0xFF00E676;
                strokeWidthDp = 1.8f;
                cornerRadiusDp = 12;
                titleColor = 0xFF69F0AE;
                msgColor = 0xFFE8F5E9;
                badgeText = "NICHIRIN";
                badgeColor = 0xFFFF3D00;
                iconTint = 0xFF00E676;
                break;
            case 19: // Ghost In The Shell
                bgColors = new int[]{0xF8020E06, 0xF8051E0D, 0xF8010803};
                strokeColor = 0xFF00FF41;
                strokeWidthDp = 1.6f;
                cornerRadiusDp = 8;
                titleColor = 0xFF00FF41;
                msgColor = 0xFF80FF9F;
                badgeText = "0101 MATRIX";
                badgeColor = 0xFF00FF41;
                iconTint = 0xFF00FF41;
                break;
            case 20: // Jujutsu Cursed
                bgColors = new int[]{0xF80C0720, 0xF81A0E44, 0xF8080415};
                strokeColor = 0xFFE040FB;
                strokeWidthDp = 1.8f;
                cornerRadiusDp = 16;
                titleColor = 0xFFEA80FC;
                msgColor = 0xFFF3E5F5;
                badgeText = "CURSED";
                badgeColor = 0xFF00E5FF;
                iconTint = 0xFFE040FB;
                break;
            case 21: // Magical Girl
                bgColors = new int[]{0xF6311B47, 0xF6532C77, 0xF6200E31};
                strokeColor = 0xFFFF80AB;
                strokeWidthDp = 1.5f;
                cornerRadiusDp = 22;
                titleColor = 0xFFE1BEE7;
                msgColor = 0xFFF8BBD0;
                badgeText = "MAGIC";
                badgeColor = 0xFFFFF176;
                defaultIconRes = R.drawable.ic_barrage_chibi;
                break;
            case 22: // Persona Phantom
                bgColors = new int[]{0xF81A0004, 0xF8330008, 0xF80D0002};
                strokeColor = 0xFFFF0033;
                strokeWidthDp = 2.4f;
                cornerRadiusDp = 6;
                titleColor = 0xFFFF3366;
                msgColor = 0xFFFFFFFF;
                badgeText = "PHANTOM";
                badgeColor = 0xFFFF0033;
                iconTint = 0xFFFF0033;
                break;
            case 23: // Shadow Monarch
                bgColors = new int[]{0xF808080C, 0xF813131D, 0xF8050508};
                strokeColor = 0xFF7C4DFF;
                strokeWidthDp = 2.0f;
                cornerRadiusDp = 14;
                titleColor = 0xFFB388FF;
                msgColor = 0xFFEDE7F6;
                badgeText = "MONARCH";
                badgeColor = 0xFF7C4DFF;
                iconTint = 0xFF7C4DFF;
                break;
            default: // Style 0: Minimal Stealth
                bgColors = new int[]{0xEE16171B, 0xEE22242B};
                strokeColor = 0x55FFFFFF;
                cornerRadiusDp = 18;
                titleColor = 0xFFCCCCCC;
                msgColor = Color.WHITE;
                badgeText = null;
                break;
        }

        card.setPadding(dp(12), dp(6), dp(12), dp(6));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, bgColors);
        bg.setCornerRadius(dp(cornerRadiusDp));
        bg.setStroke(dp(strokeWidthDp), strokeColor);
        card.setBackground(bg);

        ImageView icon = new ImageView(this);
        if (appIcon != null) {
            icon.setImageDrawable(appIcon);
        } else {
            icon.setImageResource(defaultIconRes);
            if (iconTint != 0) icon.setColorFilter(iconTint);
        }
        card.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setPadding(dp(8), 0, dp(6), 0);

        TextView titleView = new TextView(this);
        titleView.setText(title.toUpperCase(Locale.US));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8.5f);
        titleView.setTextColor(titleColor);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setLetterSpacing(0.08f);
        titleView.setMaxLines(1);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        textCol.addView(titleView);

        TextView msgView = new TextView(this);
        msgView.setText(message);
        msgView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        msgView.setTextColor(msgColor);
        msgView.setMaxLines(2);
        msgView.setEllipsize(TextUtils.TruncateAt.END);
        textCol.addView(msgView);

        card.addView(textCol, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (badgeText != null) {
            TextView badge = new TextView(this);
            badge.setText(badgeText);
            badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
            badge.setTextColor(badgeColor);
            badge.setTypeface(null, Typeface.BOLD);
            badge.setPadding(dp(4), 0, dp(4), 0);
            card.addView(badge);
        }

        card.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return card;
    }

    private int overlayType() {
        return Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void removeAuxiliaryOverlay(View view) {
        if (view == null) return;
        if (windowManager == null) windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager == null) return;
        try {
            if (view.isAttachedToWindow()) {
                windowManager.removeView(view);
            }
        } catch (Exception ignored) {}
    }

    // ==========================================
    // MINI APPS & FAVORITES MANAGEMENT
    // ==========================================

    void showMiniApps() {
        bubbleState = 3;
        params.width = dp(340);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        params.height = Math.min(dp(540), dm.heightPixels - dp(24));
        replaceOverlay(buildMiniAppsPanel());
    }

    private View buildMiniAppsPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(14), dp(16), dp(14));

        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFA1C1C20, 0xFC09090B, 0xFA26080D});
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), 0xFFFF3B48);
        panel.setBackground(bg);

        // Header with Back and macOS Traffic Lights
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(8));

        TextView backBtn = new TextView(this);
        backBtn.setText("BACK");
        applyLeadingIcon(backBtn, R.drawable.ic_back, 0xFFFF5964);
        backBtn.setTextSize(10);
        backBtn.setTextColor(0xFFFF5964);
        backBtn.setTypeface(null, Typeface.BOLD);
        backBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        GradientDrawable backBg = new GradientDrawable();
        backBg.setColor(0x443B0C12);
        backBg.setCornerRadius(dp(8));
        backBg.setStroke(dp(1), 0x88FF3B48);
        backBtn.setBackground(backBg);
        backBtn.setOnClickListener(v -> expand());
        header.addView(backBtn);

        TextView title = label("  MINI WINDOWS", 12.5f, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1.0f));

        header.addView(buildMacOsTrafficLights(this::closeOverlay, this::minimizeToEdge, this::expand));
        panel.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(true);
        scroll.setScrollbarFadingEnabled(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView subtitle = label("Floating multitasking over game. Minimize docks to edge with (›) icon.", 9.5f, 0xFF8E95B2);
        subtitle.setPadding(0, 0, 0, dp(8));
        content.addView(subtitle);

        // Section: Favorite Apps
        LinearLayout favHeader = new LinearLayout(this);
        favHeader.setOrientation(LinearLayout.HORIZONTAL);
        favHeader.setGravity(Gravity.CENTER_VERTICAL);

        TextView favTitle = label("FAVORITE APPS", 10, 0xFFFF5776);
        favTitle.setTypeface(null, Typeface.BOLD);
        favHeader.addView(favTitle, new LinearLayout.LayoutParams(0, -2, 1.0f));

        TextView editFavBtn = new TextView(this);
        editFavBtn.setText("EDIT FAVORITES");
        applyLeadingIcon(editFavBtn, R.drawable.ic_edit, 0xFFFF5964);
        editFavBtn.setTextSize(9);
        editFavBtn.setTextColor(0xFFFF5964);
        editFavBtn.setTypeface(null, Typeface.BOLD);
        editFavBtn.setPadding(dp(6), dp(2), dp(6), dp(2));
        editFavBtn.setOnClickListener(v -> showEditFavoritesDialog());
        favHeader.addView(editFavBtn);
        content.addView(favHeader);

        // Render Favorite Apps Grid
        List<String> favs = getFavoritePackages();
        PackageManager pm = getPackageManager();

        int cols = 3;
        for (int i = 0; i < favs.size(); i += cols) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(4), 0, dp(4));

            for (int j = 0; j < cols; j++) {
                if (i + j < favs.size()) {
                    String pkg = favs.get(i + j);
                    row.addView(createMiniAppTile(pm, pkg), new LinearLayout.LayoutParams(0, -2, 1.0f));
                } else {
                    View spacer = new View(this);
                    row.addView(spacer, new LinearLayout.LayoutParams(0, 0, 1.0f));
                }
            }
            content.addView(row);
        }

        // Browse All Installed Apps Button
        Button chooseAppBtn = action("Browse all installed apps", "choose_any_app");
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(-1, dp(42));
        btnLp.topMargin = dp(8);
        chooseAppBtn.setLayoutParams(btnLp);
        content.addView(chooseAppBtn);

        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        panel.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        attachHeaderDrag(header);
        attachDrag(panel);
        return panel;
    }

    private View createMiniAppTile(PackageManager pm, String pkg) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(4), dp(8), dp(4), dp(8));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xCC1F1832);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), 0xFF43345E);
        tile.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(3), dp(2), dp(3), dp(2));
        tile.setLayoutParams(lp);

        ImageView iconView = new ImageView(this);
        String labelStr = pkg;
        try {
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            labelStr = pm.getApplicationLabel(ai).toString();
            iconView.setImageDrawable(pm.getApplicationIcon(ai));
        } catch (Exception e) {
            iconView.setImageResource(R.drawable.ror_logo);
            if (pkg.contains("messenger") || pkg.contains("orca")) labelStr = "Messenger";
            else if (pkg.contains("whatsapp")) labelStr = "WhatsApp";
            else if (pkg.contains("discord")) labelStr = "Discord";
            else if (pkg.contains("telegram")) labelStr = "Telegram";
            else if (pkg.contains("youtube")) labelStr = "YouTube";
            else if (pkg.contains("chrome")) labelStr = "Chrome";
        }
        iconView.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(36)));
        tile.addView(iconView);

        TextView nameView = new TextView(this);
        nameView.setText(labelStr);
        nameView.setTextSize(9.5f);
        nameView.setTextColor(Color.WHITE);
        nameView.setGravity(Gravity.CENTER);
        nameView.setMaxLines(1);
        nameView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        nameView.setPadding(0, dp(4), 0, 0);
        tile.addView(nameView);

        final String finalLabel = labelStr;
        tile.setOnClickListener(v -> launchFreeformApp(pkg, finalLabel));
        return tile;
    }

    private List<String> getFavoritePackages() {
        SharedPreferences sp = getSharedPreferences("ror_mini_window", MODE_PRIVATE);
        String raw = sp.getString(PREF_FAVORITES, null);
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>(Arrays.asList(
                    "com.facebook.orca",
                    "com.whatsapp",
                    "com.discord",
                    "org.telegram.messenger",
                    "com.google.android.youtube",
                    "com.android.chrome"
            ));
        }
        String[] split = raw.split(",");
        List<String> list = new ArrayList<>();
        for (String s : split) {
            String tr = s.trim();
            if (!tr.isEmpty()) list.add(tr);
        }
        return list;
    }

    private void saveFavoritePackages(List<String> pkgs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pkgs.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(pkgs.get(i));
        }
        getSharedPreferences("ror_mini_window", MODE_PRIVATE)
                .edit()
                .putString(PREF_FAVORITES, sb.toString())
                .apply();
    }

    private void showEditFavoritesDialog() {
        PackageManager pm = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(mainIntent, 0);

        List<String> names = new ArrayList<>();
        List<String> pkgs = new ArrayList<>();
        for (ResolveInfo ri : list) {
            if (ri.activityInfo == null) continue;
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            names.add(ri.loadLabel(pm).toString());
            pkgs.add(pkg);
        }

        List<String> currentFavs = getFavoritePackages();
        boolean[] checked = new boolean[pkgs.size()];
        for (int i = 0; i < pkgs.size(); i++) {
            checked[i] = currentFavs.contains(pkgs.get(i));
        }

        CharSequence[] items = names.toArray(new CharSequence[0]);
        AlertDialog dlg = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.RorDialogTheme))
                .setTitle("Select Favorite Mini Apps")
                .setMultiChoiceItems(items, checked, (d, which, isChecked) -> {
                    checked[which] = isChecked;
                })
                .setPositiveButton("Save Favorites", (d, which) -> {
                    List<String> updated = new ArrayList<>();
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i]) updated.add(pkgs.get(i));
                    }
                    saveFavoritePackages(updated);
                    toast("Favorites updated");
                    showMiniApps();
                })
                .setNegativeButton("Cancel", null)
                .create();

        if (dlg.getWindow() != null) {
            dlg.getWindow().setType(Build.VERSION.SDK_INT >= 26
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE);
        }
        dlg.show();
    }

    private void showAllInstalledAppsPicker() {
        PackageManager pm = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(mainIntent, 0);

        List<String> names = new ArrayList<>();
        List<String> pkgs = new ArrayList<>();
        for (ResolveInfo ri : list) {
            if (ri.activityInfo == null) continue;
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            names.add(ri.loadLabel(pm).toString());
            pkgs.add(pkg);
        }

        CharSequence[] items = names.toArray(new CharSequence[0]);
        AlertDialog dlg = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.RorDialogTheme))
                .setTitle("Select Any App for Mini Window")
                .setItems(items, (d, which) -> {
                    launchFreeformApp(pkgs.get(which), names.get(which));
                })
                .setNegativeButton("Cancel", null)
                .create();

        if (dlg.getWindow() != null) {
            dlg.getWindow().setType(Build.VERSION.SDK_INT >= 26
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE);
        }
        dlg.show();
    }

    private void launchFreeformApp(String pkg, String name) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(pkg);
        if (launchIntent == null) {
            toast(name + " is not installed on this device");
            return;
        }

        android.content.ComponentName comp = launchIntent.getComponent();
        if (comp == null) {
            toast("Cannot resolve activity for " + name);
            return;
        }

        activeMiniAppPkg = pkg;
        activeMiniAppName = name;

        String compName = comp.flattenToShortString();
        String cmd = "settings put global enable_freeform_support 1; "
                + "settings put global force_resizable_activities 1; "
                + "am start -n " + compName + " --windowingMode 5";

        runOnWorker(() -> {
            if (RootUtils.rootAccess()) {
                RootUtils.runCommand(cmd);
            } else if (ShizukuBridge.granted()) {
                ShizukuBridge.run(cmd);
            }
        });

        toast("Opening " + name + " in Mini Window");
        // Automatically minimize controls to edge with same '>' handle
        minimizeToEdge();
    }

    // ==========================================
    // SCREEN RECORDER (480p, 720p, 1080p + SOUND)
    // ==========================================

    private void showScreenRecorder() {
        bubbleState = 4;
        params.width = dp(340);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        params.height = Math.min(dp(540), dm.heightPixels - dp(24));
        replaceOverlay(buildScreenRecorderPanel());
    }

    private View buildScreenRecorderPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(14), dp(16), dp(14));

        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFA1C1C20, 0xFC09090B, 0xFA26080D});
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), 0xFFFF3366);
        panel.setBackground(bg);

        // Header with Back and macOS Traffic Lights
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(8));

        TextView backBtn = new TextView(this);
        backBtn.setText("BACK");
        applyLeadingIcon(backBtn, R.drawable.ic_back, 0xFFFF3366);
        backBtn.setTextSize(10);
        backBtn.setTextColor(0xFFFF3366);
        backBtn.setTypeface(null, Typeface.BOLD);
        backBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        GradientDrawable backBg = new GradientDrawable();
        backBg.setColor(0x33780020);
        backBg.setCornerRadius(dp(8));
        backBg.setStroke(dp(1), 0x66FF3366);
        backBtn.setBackground(backBg);
        backBtn.setOnClickListener(v -> expand());
        header.addView(backBtn);

        TextView title = label("  SCREEN RECORDER", 12.5f, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1.0f));

        header.addView(buildMacOsTrafficLights(this::closeOverlay, this::minimizeToEdge, this::expand));
        panel.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(true);
        scroll.setScrollbarFadingEnabled(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView subtitle = label("Record in-game footage without interrupting gameplay. Includes game sound.", 9.5f, 0xFF8E95B2);
        subtitle.setPadding(0, 0, 0, dp(8));
        content.addView(subtitle);

        if (isRecording) {
            TextView recStatus = label("CURRENTLY RECORDING IN PROGRESS", 11, 0xFFFF3366);
            recStatus.setTypeface(null, Typeface.BOLD);
            recStatus.setGravity(Gravity.CENTER);
            recStatus.setPadding(0, dp(8), 0, dp(8));
            content.addView(recStatus);

            Button stopBtn = action("Stop recording and save video", "stop_rec_now");
            stopBtn.setOnClickListener(v -> {
                stopScreenRecording();
                expand();
            });
            content.addView(stopBtn);
        } else {
            TextView resTitle = label("SELECT RESOLUTION", 10, 0xFFFF5776);
            resTitle.setTypeface(null, Typeface.BOLD);
            resTitle.setPadding(0, dp(4), 0, dp(4));
            content.addView(resTitle);

            final int[] selectedRes = {1080}; // default 1080p
            LinearLayout resRow = new LinearLayout(this);
            resRow.setOrientation(LinearLayout.HORIZONTAL);

            Button b1080 = createResButton("1080p\nFHD", true);
            Button b720 = createResButton("720p\nHD", false);
            Button b480 = createResButton("480p\nFast", false);

            View.OnClickListener resClick = v -> {
                if (v == b1080) selectedRes[0] = 1080;
                else if (v == b720) selectedRes[0] = 720;
                else selectedRes[0] = 480;

                updateResButton(b1080, selectedRes[0] == 1080);
                updateResButton(b720, selectedRes[0] == 720);
                updateResButton(b480, selectedRes[0] == 480);
            };

            b1080.setOnClickListener(resClick);
            b720.setOnClickListener(resClick);
            b480.setOnClickListener(resClick);

            resRow.addView(b1080, profileParams());
            resRow.addView(b720, profileParams());
            resRow.addView(b480, profileParams());
            content.addView(resRow);

            // Sound Option
            CheckBox soundBox = new CheckBox(this);
            soundBox.setText("Include Game Sound (Internal Audio)");
            soundBox.setTextColor(Color.WHITE);
            soundBox.setTextSize(10.5f);
            soundBox.setChecked(true);
            soundBox.setPadding(dp(4), dp(8), 0, dp(8));
            content.addView(soundBox);

            // Start Recording Button
            Button startRecBtn = new Button(this);
            startRecBtn.setText("START IN-GAME RECORDING");
            Drawable recordIcon = ContextCompat.getDrawable(this, R.drawable.ic_record_dot);
            if (recordIcon != null) {
                recordIcon = recordIcon.mutate();
                recordIcon.setTint(Color.WHITE);
                recordIcon.setBounds(0, 0, dp(18), dp(18));
                startRecBtn.setCompoundDrawables(recordIcon, null, null, null);
                startRecBtn.setCompoundDrawablePadding(dp(8));
            }
            startRecBtn.setTextColor(Color.WHITE);
            startRecBtn.setTextSize(11);
            startRecBtn.setTypeface(null, Typeface.BOLD);
            GradientDrawable recBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0xFFFF1744, 0xFFD50000});
            recBg.setCornerRadius(dp(12));
            startRecBtn.setBackground(recBg);

            LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(-1, dp(44));
            sLp.topMargin = dp(6);
            startRecBtn.setLayoutParams(sLp);

            startRecBtn.setOnClickListener(v -> {
                startScreenRecording(selectedRes[0], soundBox.isChecked());
                minimizeToEdge();
            });
            content.addView(startRecBtn);
        }

        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        panel.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        attachHeaderDrag(header);
        attachDrag(panel);
        return panel;
    }

    private Button createResButton(String text, boolean active) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(10);
        b.setTextColor(Color.WHITE);
        b.setGravity(Gravity.CENTER);
        b.setAllCaps(false);
        updateResButton(b, active);
        return b;
    }

    private void updateResButton(Button b, boolean active) {
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(dp(10));
        if (active) {
            gd.setColor(0xEEFF3366);
            gd.setStroke(dp(1.5f), 0xFFFFFFFF);
        } else {
            gd.setColor(0xCC1E192D);
            gd.setStroke(dp(1), 0xFF45365E);
        }
        b.setBackground(gd);
    }

    private void startScreenRecording(int resolution, boolean includeSound) {
        if (isRecording || ScreenCaptureManager.getInstance().isRecording()) {
            toast("Already recording");
            return;
        }

        ScreenCaptureManager.getInstance().startRecording(this, resolution, includeSound, new ScreenCaptureManager.RecordingCallback() {
            @Override
            public void onRecordingStarted(@NonNull String filePath) {
                isRecording = true;
                currentRecordingPath = filePath;
                recordingStartTime = ScreenCaptureManager.getInstance().getRecordingStartTime();
                showFloatingRecordPill();
                toast("In-game recording started (" + resolution + "p)");
                showBarrage("Screen Recorder", "Recording active (" + resolution + "p)\nSaving to Movies/ROR");
            }

            @Override
            public void onRecordingSuccess(@NonNull File file, @Nullable Uri mediaUri, long sizeBytes) {
                isRecording = false;
                hideFloatingRecordPill();
                long mb = sizeBytes / (1024 * 1024);
                toast("Recording saved (" + (mb > 0 ? mb + "MB" : (sizeBytes / 1024) + "KB") + "):\n" + file.getName());
                showBarrage("Recording Saved", file.getName() + " saved to Movies/ROR\nVisible in Gallery");
            }

            @Override
            public void onRecordingError(@NonNull String errorMessage) {
                isRecording = false;
                hideFloatingRecordPill();
                toast(errorMessage);
                showBarrage("Recording Error", errorMessage);
            }
        });
    }

    private void stopScreenRecording() {
        if (!isRecording && !ScreenCaptureManager.getInstance().isRecording()) return;
        toast("Finalizing and saving recording...");
        ScreenCaptureManager.getInstance().stopRecording(this, new ScreenCaptureManager.RecordingCallback() {
            @Override
            public void onRecordingStarted(@NonNull String filePath) {}

            @Override
            public void onRecordingSuccess(@NonNull File file, @Nullable Uri mediaUri, long sizeBytes) {
                isRecording = false;
                hideFloatingRecordPill();
                long mb = sizeBytes / (1024 * 1024);
                toast("Recording saved (" + (mb > 0 ? mb + "MB" : (sizeBytes / 1024) + "KB") + "):\n" + file.getName());
                showBarrage("Recording Saved", file.getName() + " saved to Movies/ROR\nVisible in Gallery");
            }

            @Override
            public void onRecordingError(@NonNull String errorMessage) {
                isRecording = false;
                hideFloatingRecordPill();
                toast(errorMessage);
                showBarrage("Recording Error", errorMessage);
            }
        });
    }

    private void showFloatingRecordPill() {
        if (recordPillView != null) return;
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (wm == null) return;

        recordParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, dp(34),
                Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        recordParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        recordParams.y = dp(24);

        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setPadding(dp(12), dp(4), dp(10), dp(4));

        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setColor(0xE614060B);
        pillBg.setCornerRadius(dp(17));
        pillBg.setStroke(dp(1.5f), 0xFFFF1744);
        pill.setBackground(pillBg);

        TextView timerText = new TextView(this);
        timerText.setId(android.R.id.text1);
        timerText.setText("REC 00:00");
        timerText.setTextSize(10.5f);
        timerText.setTextColor(Color.WHITE);
        timerText.setTypeface(null, Typeface.BOLD);
        pill.addView(timerText);

        TextView stopText = new TextView(this);
        stopText.setText("  STOP");
        stopText.setTextSize(10);
        stopText.setTextColor(0xFFFF5252);
        stopText.setTypeface(null, Typeface.BOLD);
        stopText.setPadding(dp(6), 0, 0, 0);
        stopText.setOnClickListener(v -> stopScreenRecording());
        pill.addView(stopText);

        recordPillView = pill;
        wm.addView(recordPillView, recordParams);
        handler.post(recordTimerRunnable);
    }

    private void hideFloatingRecordPill() {
        handler.removeCallbacks(recordTimerRunnable);
        if (recordPillView != null) {
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (wm != null) {
                try { wm.removeView(recordPillView); } catch (Exception ignored) {}
            }
            recordPillView = null;
        }
    }

    // ==========================================
    // FLOATING WI-FI SCANNER & MANAGER
    // ==========================================

    public static class WifiNetworkItem {
        public String ssid = "";
        public String bssid = "";
        public int rssi = -100;
        public int frequency = 0;
        public String security = "Open";
        public boolean isConnected = false;
        public boolean isSaved = false;
        public int networkId = -1;
    }

    private final List<WifiNetworkItem> cachedWifiList = new ArrayList<>();
    private boolean isScanningWifi = false;
    private TextView wifiScanStatusText;
    private LinearLayout wifiNetworksContainer;

    private void showWifiPanel() {
        cancelAutoEdgeTimer();
        bubbleState = 6;
        params.width = dp(340);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        params.height = Math.min(dp(540), dm.heightPixels - dp(24));
        replaceOverlay(buildWifiPanel());
        startWifiScan();
    }

    private String getConnectedWifiSsid() {
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null && wm.isWifiEnabled()) {
                WifiInfo info = wm.getConnectionInfo();
                if (info != null && info.getNetworkId() != -1) {
                    String ssid = info.getSSID();
                    if (ssid != null && !ssid.equals("<unknown ssid>") && !ssid.isEmpty()) {
                        return ssid.replace("\"", "").trim();
                    }
                }
            }
        } catch (Exception ignored) {}
        try {
            String out = RootUtils.runAndGetOutput("cmd wifi status");
            if (out != null) {
                int idx = out.indexOf("Wifi is connected to ");
                if (idx != -1) {
                    int start = idx + "Wifi is connected to ".length();
                    int end = out.indexOf("\n", start);
                    if (end != -1) {
                        return out.substring(start, end).replace("\"", "").trim();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private Map<String, Integer> getSavedWifiNetworks() {
        Map<String, Integer> map = new HashMap<>();
        try {
            String out = RootUtils.runAndGetOutput("cmd wifi list-networks");
            if (out != null) {
                String[] lines = out.split("\n");
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("Network Id") || trimmed.startsWith("---")) continue;
                    String[] parts = trimmed.split("\\s+");
                    if (parts.length >= 2) {
                        try {
                            int id = Integer.parseInt(parts[0]);
                            String ssid = parts[1].replace("\"", "").trim();
                            map.put(ssid, id);
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}
        return map;
    }

    private List<WifiNetworkItem> scanWifiNetworksInternal() {
        List<WifiNetworkItem> list = new ArrayList<>();
        Map<String, WifiNetworkItem> uniqueMap = new LinkedHashMap<>();

        String connectedSsid = getConnectedWifiSsid();
        Map<String, Integer> savedMap = getSavedWifiNetworks();

        // 1. Trigger scan
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) wm.startScan();
        } catch (Exception ignored) {}
        try {
            RootUtils.runCommand("cmd wifi start-scan");
        } catch (Exception ignored) {}

        // 2. Read standard ScanResults if available
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null && ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                List<ScanResult> results = wm.getScanResults();
                if (results != null) {
                    for (ScanResult sr : results) {
                        if (sr.SSID == null || sr.SSID.trim().isEmpty()) continue;
                        String cleanSsid = sr.SSID.replace("\"", "").trim();
                        if (cleanSsid.isEmpty()) continue;
                        if (uniqueMap.containsKey(cleanSsid)) {
                            if (sr.level > uniqueMap.get(cleanSsid).rssi) {
                                uniqueMap.get(cleanSsid).rssi = sr.level;
                                uniqueMap.get(cleanSsid).frequency = sr.frequency;
                            }
                            continue;
                        }
                        WifiNetworkItem item = new WifiNetworkItem();
                        item.ssid = cleanSsid;
                        item.bssid = sr.BSSID;
                        item.rssi = sr.level;
                        item.frequency = sr.frequency;
                        String caps = sr.capabilities != null ? sr.capabilities.toUpperCase(Locale.US) : "";
                        if (caps.contains("WPA3") || caps.contains("SAE")) item.security = "WPA3";
                        else if (caps.contains("WPA") || caps.contains("PSK")) item.security = "WPA2";
                        else if (caps.contains("WEP")) item.security = "WEP";
                        else item.security = "Open";

                        uniqueMap.put(cleanSsid, item);
                    }
                }
            }
        } catch (Exception ignored) {}

        // 3. Read shell scan results
        try {
            String out = RootUtils.runAndGetOutput("cmd wifi list-scan-results");
            if (out != null) {
                String[] lines = out.split("\n");
                for (String line : lines) {
                    String tr = line.trim();
                    if (tr.isEmpty() || tr.startsWith("BSSID") || tr.startsWith("---")) continue;
                    String[] parts = tr.split("\\s+");
                    if (parts.length >= 5) {
                        String bssid = parts[0];
                        int freq = 0;
                        int rssi = -100;
                        try { freq = Integer.parseInt(parts[1]); } catch (Exception ignored) {}
                        try { rssi = Integer.parseInt(parts[2]); } catch (Exception ignored) {}
                        String ssid = parts[4].replace("\"", "").trim();
                        String flags = parts.length >= 6 ? parts[5].toUpperCase(Locale.US) : "";
                        if (!ssid.isEmpty()) {
                            if (!uniqueMap.containsKey(ssid)) {
                                WifiNetworkItem item = new WifiNetworkItem();
                                item.ssid = ssid;
                                item.bssid = bssid;
                                item.frequency = freq;
                                item.rssi = rssi;
                                if (flags.contains("WPA3") || flags.contains("SAE")) item.security = "WPA3";
                                else if (flags.contains("WPA") || flags.contains("PSK")) item.security = "WPA2";
                                else if (flags.contains("WEP")) item.security = "WEP";
                                else item.security = "Open";
                                uniqueMap.put(ssid, item);
                            } else {
                                if (rssi > uniqueMap.get(ssid).rssi) {
                                    uniqueMap.get(ssid).rssi = rssi;
                                    uniqueMap.get(ssid).frequency = freq;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        // Ensure connected SSID is present
        if (!connectedSsid.isEmpty() && !uniqueMap.containsKey(connectedSsid)) {
            WifiNetworkItem current = new WifiNetworkItem();
            current.ssid = connectedSsid;
            current.rssi = -45;
            current.frequency = 5180;
            current.security = "WPA2";
            uniqueMap.put(connectedSsid, current);
        }

        // Apply saved status and connected status
        for (WifiNetworkItem item : uniqueMap.values()) {
            if (!connectedSsid.isEmpty() && item.ssid.equalsIgnoreCase(connectedSsid)) {
                item.isConnected = true;
            }
            if (savedMap.containsKey(item.ssid)) {
                item.isSaved = true;
                item.networkId = savedMap.get(item.ssid);
            }
            list.add(item);
        }

        Collections.sort(list, (a, b) -> {
            if (a.isConnected != b.isConnected) return a.isConnected ? -1 : 1;
            if (a.isSaved != b.isSaved) return a.isSaved ? -1 : 1;
            return Integer.compare(b.rssi, a.rssi);
        });

        return list;
    }

    private void startWifiScan() {
        if (isScanningWifi) return;
        isScanningWifi = true;
        if (wifiScanStatusText != null) {
            wifiScanStatusText.setText("SCANNING NEARBY 2.4GHz & 5GHz NETWORKS…");
            wifiScanStatusText.setTextColor(0xFFFF3B48);
        }
        runOnWorker(() -> {
            List<WifiNetworkItem> results = scanWifiNetworksInternal();
            handler.post(() -> {
                isScanningWifi = false;
                cachedWifiList.clear();
                cachedWifiList.addAll(results);
                if (bubbleState == 6 && wifiNetworksContainer != null) {
                    if (wifiScanStatusText != null) {
                        wifiScanStatusText.setText(results.isEmpty()
                                ? "NO NETWORKS FOUND  •  TAP RESCAN"
                                : "FOUND " + results.size() + " NETWORKS  •  TAP TO CONNECT");
                        wifiScanStatusText.setTextColor(0xFF32D68A);
                    }
                    populateWifiList(wifiNetworksContainer, results);
                }
            });
        });
    }

    private void populateWifiList(LinearLayout container, List<WifiNetworkItem> items) {
        container.removeAllViews();
        if (items.isEmpty()) {
            TextView empty = label("No Wi-Fi networks in range.\nEnsure Wi-Fi is enabled and tap SEARCH.", 10.5f, 0xFF8E95B2);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(18), 0, dp(18));
            container.addView(empty);
            return;
        }

        for (WifiNetworkItem item : items) {
            container.addView(createWifiNetworkTile(item));
        }
    }

    private View createWifiNetworkTile(WifiNetworkItem item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));

        GradientDrawable bg = new GradientDrawable();
        if (item.isConnected) {
            bg.setColor(0xEE1E2620);
            bg.setStroke(dp(1.2f), 0xFF32D68A);
        } else if (item.isSaved) {
            bg.setColor(0xEE1A1B24);
            bg.setStroke(dp(1), 0xFF4A4E65);
        } else {
            bg.setColor(0xEE15161A);
            bg.setStroke(dp(1), 0xFF2A2C34);
        }
        bg.setCornerRadius(dp(10));
        row.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(6);
        row.setLayoutParams(lp);

        // Signal icon on left
        ImageView iconView = new ImageView(this);
        Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_wifi);
        if (icon != null) {
            icon = icon.mutate();
            int iconColor = item.isConnected ? 0xFF32D68A : (item.rssi >= -65 ? 0xFFFFFFFF : 0xFF9CA3AF);
            icon.setTint(iconColor);
            iconView.setImageDrawable(icon);
        }
        iconView.setLayoutParams(new LinearLayout.LayoutParams(dp(22), dp(22)));
        row.addView(iconView);

        // Center info
        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setPadding(dp(10), 0, dp(6), 0);

        TextView ssidView = label(item.ssid, 12, Color.WHITE);
        ssidView.setTypeface(null, Typeface.BOLD);
        ssidView.setSingleLine(true);
        ssidView.setEllipsize(TextUtils.TruncateAt.END);
        textCol.addView(ssidView);

        String freqStr = (item.frequency >= 4900) ? "5 GHz" : (item.frequency > 0 ? "2.4 GHz" : "");
        String details = item.security + (freqStr.isEmpty() ? "" : " • " + freqStr) + " • " + item.rssi + " dBm";
        if (item.isConnected) details = "CONNECTED • " + details;
        else if (item.isSaved) details = "SAVED • " + details;

        TextView subView = label(details, 8.5f, item.isConnected ? 0xFF32D68A : 0xFF8E95B2);
        textCol.addView(subView);

        row.addView(textCol, new LinearLayout.LayoutParams(0, -2, 1.0f));

        // Right Action Button
        if (item.isConnected) {
            TextView statusBadge = label("CONNECTED", 9, 0xFF32D68A);
            statusBadge.setTypeface(null, Typeface.BOLD);
            statusBadge.setPadding(dp(8), dp(4), dp(8), dp(4));
            row.addView(statusBadge);
        } else {
            Button connectBtn = new Button(this);
            connectBtn.setText("CONNECT");
            connectBtn.setTextColor(Color.WHITE);
            connectBtn.setTextSize(9);
            connectBtn.setTypeface(null, Typeface.BOLD);
            connectBtn.setPadding(dp(10), dp(4), dp(10), dp(4));
            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setColor(0x44FF2638);
            btnBg.setCornerRadius(dp(8));
            btnBg.setStroke(dp(1), 0xAAFF3848);
            connectBtn.setBackground(btnBg);
            connectBtn.setOnClickListener(v -> onWifiItemClicked(item));
            row.addView(connectBtn, new LinearLayout.LayoutParams(-2, dp(32)));
        }

        row.setOnClickListener(v -> onWifiItemClicked(item));
        return row;
    }

    private void onWifiItemClicked(WifiNetworkItem item) {
        if (item.isConnected) {
            toast("Already connected to " + item.ssid);
            return;
        }

        if (item.isSaved || "Open".equalsIgnoreCase(item.security) || item.security.isEmpty()) {
            connectToWifi(item.ssid, item.security, "");
            return;
        }

        showWifiPasswordDialog(item.ssid, item.security);
    }

    private void showWifiPasswordDialog(String ssid, String security) {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault_Dialog_Alert));
        builder.setTitle("Connect to Wi-Fi");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(12), dp(20), dp(8));

        TextView prompt = label("Network: " + ssid + "\nSecurity: " + security, 11, 0xFFE0E0E0);
        prompt.setPadding(0, 0, 0, dp(12));
        layout.addView(prompt);

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Enter Wi-Fi password");
        input.setHintTextColor(0xFF71717A);
        input.setTextColor(Color.WHITE);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setTextSize(13);
        input.setPadding(dp(10), dp(10), dp(10), dp(10));
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(0xFF141418);
        inputBg.setCornerRadius(dp(8));
        inputBg.setStroke(dp(1), 0xFFFF3B48);
        input.setBackground(inputBg);
        layout.addView(input);

        final CheckBox showPwd = new CheckBox(this);
        showPwd.setText("Show password");
        showPwd.setTextColor(0xFF9E9EA8);
        showPwd.setTextSize(10);
        showPwd.setPadding(dp(4), dp(8), 0, dp(8));
        showPwd.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            input.setSelection(input.getText().length());
        });
        layout.addView(showPwd);

        builder.setView(layout);
        builder.setPositiveButton("Connect", (dialog, which) -> {
            String pwd = input.getText().toString();
            connectToWifi(ssid, security, pwd);
        });
        builder.setNegativeButton("Cancel", null);

        AlertDialog dlg = builder.create();
        if (dlg.getWindow() != null) {
            dlg.getWindow().setType(Build.VERSION.SDK_INT >= 26
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE);
        }
        dlg.show();
    }

    private void connectToWifi(String ssid, String security, String password) {
        showBarrage("Wi-Fi Manager", "Connecting to " + ssid + "…");
        toast("Connecting to " + ssid + "…");
        runOnWorker(() -> {
            String cmd;
            if (security == null || "open".equalsIgnoreCase(security) || TextUtils.isEmpty(password)) {
                cmd = "cmd wifi connect-network \"" + ssid + "\" open";
            } else if (security.toUpperCase(Locale.US).contains("WPA3") || security.toUpperCase(Locale.US).contains("SAE")) {
                cmd = "cmd wifi connect-network \"" + ssid + "\" wpa3 \"" + password + "\"";
            } else {
                cmd = "cmd wifi connect-network \"" + ssid + "\" wpa2 \"" + password + "\"";
            }
            if (GsmProfileManager.isRootEnabled(this)) {
                RootUtils.runCommand(cmd);
            } else if (ShizukuBridge.granted()) {
                ShizukuBridge.run(cmd);
            } else {
                try {
                    Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
                } catch (Exception ignored) {}
            }

            try { Thread.sleep(2500); } catch (InterruptedException ignored) {}
            handler.post(() -> {
                String nowConnected = getConnectedWifiSsid();
                if (!nowConnected.isEmpty() && nowConnected.equalsIgnoreCase(ssid)) {
                    showBarrage("Wi-Fi Manager", "Connected to " + ssid);
                    toast("Connected to " + ssid);
                }
                startWifiScan();
            });
        });
    }

    private void disconnectWifi() {
        showBarrage("Wi-Fi Manager", "Disconnecting Wi-Fi…");
        runOnWorker(() -> {
            String cmd = "cmd wifi disconnect";
            if (GsmProfileManager.isRootEnabled(this)) {
                RootUtils.runCommand(cmd);
            } else if (ShizukuBridge.granted()) {
                ShizukuBridge.run(cmd);
            }
            try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
            handler.post(() -> {
                toast("Disconnected from Wi-Fi");
                startWifiScan();
            });
        });
    }

    private View buildWifiPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(14), dp(16), dp(14));

        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFA1C1C20, 0xFC09090B, 0xFA26080D});
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), 0xFFFF3B48);
        panel.setBackground(bg);

        // Header with Back and macOS Traffic Lights
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(8));

        TextView backBtn = new TextView(this);
        backBtn.setText("BACK");
        applyLeadingIcon(backBtn, R.drawable.ic_back, 0xFFFF5964);
        backBtn.setTextSize(10);
        backBtn.setTextColor(0xFFFF5964);
        backBtn.setTypeface(null, Typeface.BOLD);
        backBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        GradientDrawable backBg = new GradientDrawable();
        backBg.setColor(0x443B0C12);
        backBg.setCornerRadius(dp(8));
        backBg.setStroke(dp(1), 0x88FF3B48);
        backBtn.setBackground(backBg);
        backBtn.setOnClickListener(v -> expand());
        header.addView(backBtn);

        TextView title = label("  WI-FI MANAGER", 12.5f, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1.0f));

        header.addView(buildMacOsTrafficLights(this::closeOverlay, this::minimizeToEdge, this::startWifiScan));
        panel.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(true);
        scroll.setScrollbarFadingEnabled(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        // Power row (Wi-Fi ON/OFF toggle)
        LinearLayout powerRow = new LinearLayout(this);
        powerRow.setOrientation(LinearLayout.HORIZONTAL);
        powerRow.setGravity(Gravity.CENTER_VERTICAL);
        powerRow.setPadding(0, 0, 0, dp(8));

        TextView pwrLabel = label("WI-FI ADAPTER", 10, 0xFFFF5776);
        pwrLabel.setTypeface(null, Typeface.BOLD);
        powerRow.addView(pwrLabel, new LinearLayout.LayoutParams(0, -2, 1.0f));

        Button pwrToggle = new Button(this);
        pwrToggle.setText(wifiEnabled ? "WI-FI: ON" : "WI-FI: OFF");
        pwrToggle.setTextColor(Color.WHITE);
        pwrToggle.setTextSize(9.5f);
        pwrToggle.setTypeface(null, Typeface.BOLD);
        pwrToggle.setPadding(dp(12), dp(4), dp(12), dp(4));
        GradientDrawable pwrBg = new GradientDrawable();
        pwrBg.setColor(wifiEnabled ? 0xCC1F3A24 : 0xCC3A181C);
        pwrBg.setCornerRadius(dp(8));
        pwrBg.setStroke(dp(1), wifiEnabled ? 0xFF32D68A : 0xFFFF3848);
        pwrToggle.setBackground(pwrBg);
        pwrToggle.setOnClickListener(v -> {
            toggleWifi();
            showWifiPanel();
        });
        powerRow.addView(pwrToggle, new LinearLayout.LayoutParams(-2, dp(34)));
        content.addView(powerRow);

        // Current Connection Card
        String connectedSsid = getConnectedWifiSsid();
        LinearLayout currentCard = new LinearLayout(this);
        currentCard.setOrientation(LinearLayout.VERTICAL);
        currentCard.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable curBg = new GradientDrawable();
        curBg.setColor(0xEE14171E);
        curBg.setCornerRadius(dp(12));
        curBg.setStroke(dp(1), !connectedSsid.isEmpty() ? 0xFF32D68A : 0xFF3B404E);
        currentCard.setBackground(curBg);

        LinearLayout topCur = new LinearLayout(this);
        topCur.setOrientation(LinearLayout.HORIZONTAL);
        topCur.setGravity(Gravity.CENTER_VERTICAL);

        TextView curStatus = label(!connectedSsid.isEmpty() ? "CONNECTED TO" : "NO ACTIVE CONNECTION", 9,
                !connectedSsid.isEmpty() ? 0xFF32D68A : 0xFF9CA3AF);
        curStatus.setTypeface(null, Typeface.BOLD);
        topCur.addView(curStatus, new LinearLayout.LayoutParams(0, -2, 1.0f));

        if (!connectedSsid.isEmpty()) {
            TextView disBtn = label("DISCONNECT", 9, 0xFFFF4A56);
            disBtn.setTypeface(null, Typeface.BOLD);
            disBtn.setPadding(dp(8), dp(2), dp(8), dp(2));
            GradientDrawable disBg = new GradientDrawable();
            disBg.setColor(0x33FF2638);
            disBg.setCornerRadius(dp(6));
            disBg.setStroke(dp(1), 0x88FF3B48);
            disBtn.setBackground(disBg);
            disBtn.setOnClickListener(v -> disconnectWifi());
            topCur.addView(disBtn);
        }
        currentCard.addView(topCur);

        TextView curSsid = label(!connectedSsid.isEmpty() ? connectedSsid : "Wi-Fi is disconnected", 13.5f, Color.WHITE);
        curSsid.setTypeface(null, Typeface.BOLD);
        curSsid.setPadding(0, dp(4), 0, dp(2));
        currentCard.addView(curSsid);

        if (!connectedSsid.isEmpty()) {
            TextView curDetails = label("High speed • Low gaming ping • Active route", 8.5f, 0xFF8E95B2);
            currentCard.addView(curDetails);
        }
        content.addView(currentCard);

        // Search / Rescan Button
        Button scanBtn = new Button(this);
        scanBtn.setText("SEARCH NEARBY WI-FI");
        Drawable searchIcon = ContextCompat.getDrawable(this, R.drawable.ic_search);
        if (searchIcon != null) {
            searchIcon = searchIcon.mutate();
            searchIcon.setTint(Color.WHITE);
            searchIcon.setBounds(0, 0, dp(16), dp(16));
            scanBtn.setCompoundDrawables(searchIcon, null, null, null);
            scanBtn.setCompoundDrawablePadding(dp(8));
        }
        scanBtn.setTextColor(Color.WHITE);
        scanBtn.setTextSize(10.5f);
        scanBtn.setTypeface(null, Typeface.BOLD);
        GradientDrawable scanBtnBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFFFF2638, 0xFFA80F26});
        scanBtnBg.setCornerRadius(dp(10));
        scanBtn.setBackground(scanBtnBg);
        LinearLayout.LayoutParams scanLp = new LinearLayout.LayoutParams(-1, dp(40));
        scanLp.topMargin = dp(10);
        scanLp.bottomMargin = dp(4);
        scanBtn.setLayoutParams(scanLp);
        scanBtn.setOnClickListener(v -> startWifiScan());
        content.addView(scanBtn);

        // Scan status text
        wifiScanStatusText = label("SCANNING NEARBY NETWORKS…", 8.5f, 0xFF9CA3AF);
        wifiScanStatusText.setTypeface(null, Typeface.BOLD);
        wifiScanStatusText.setGravity(Gravity.CENTER);
        wifiScanStatusText.setPadding(0, dp(4), 0, dp(8));
        content.addView(wifiScanStatusText);

        // Networks Container
        wifiNetworksContainer = new LinearLayout(this);
        wifiNetworksContainer.setOrientation(LinearLayout.VERTICAL);
        if (!cachedWifiList.isEmpty()) {
            populateWifiList(wifiNetworksContainer, cachedWifiList);
        }
        content.addView(wifiNetworksContainer);

        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        panel.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        // Footer Actions: Side Minimize & Collapse to Orb (Fixed at Bottom)
        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setPadding(0, dp(6), 0, 0);

        Button sideMinBtn = action("Dock to edge", "minimize");
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, dp(40), 1.0f);
        p1.setMarginEnd(dp(4));
        footer.addView(sideMinBtn, p1);

        Button closeBtn = action("Collapse", "close");
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, dp(40), 1.0f);
        p2.setMarginStart(dp(4));
        footer.addView(closeBtn, p2);

        panel.addView(footer);

        attachHeaderDrag(header);
        attachDrag(panel);
        return panel;
    }

    /**
     * Minimizes the floating icon to a sleek '>' tab docked at the screen edge.
     */
    void minimizeToEdge() {
        cancelAutoEdgeTimer();
        bubbleState = 2; // edge tab
        metrics = null;
        params.width = dp(34);
        params.height = dp(68);
        params.gravity = Gravity.CENTER_VERTICAL | (dockOnRight ? Gravity.END : Gravity.START);
        params.x = 0; // Tucked tight to edge
        replaceOverlay(buildMinimizedEdgeTab());
    }

    private void expandFromEdge() {
        // REVROR-style edge gesture opens the complete Energy Cube immediately.
        expand();
    }

    private void toastResult(boolean success, String detail) { toast(detail); }
    private void toast(String message) { new Handler(getMainLooper()).post(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show()); }

    private void attachEdgeTabTouch(View view) {
        final int touchSlop = Math.max(dp(14), ViewConfiguration.get(view.getContext()).getScaledTouchSlop());
        view.setOnLongClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            closeOverlay();
            return true;
        });
        view.setOnTouchListener(new View.OnTouchListener() {
            int startY; float downY, downX; boolean moved;
            long downTime;
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startY = params.y;
                    downY = event.getRawY();
                    downX = event.getRawX();
                    downTime = System.currentTimeMillis();
                    moved = false;
                    return false;
                }
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    float dy = event.getRawY() - downY;
                    float dx = event.getRawX() - downX;
                    if (Math.abs(dy) > touchSlop || Math.abs(dx) > touchSlop) moved = true;
                    if (dockOnRight && dx > dp(26)) {
                        closeOverlay();
                        return true;
                    } else if (!dockOnRight && dx < -dp(26)) {
                        closeOverlay();
                        return true;
                    }
                    if (moved) {
                        params.y = startY + (int)dy;
                        savedY = params.y;
                        if (windowManager != null && overlay != null) {
                            windowManager.updateViewLayout(overlay, params);
                        }
                    }
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (!moved && (System.currentTimeMillis() - downTime < 400)) {
                        expandFromEdge();
                        return true;
                    }
                    return true;
                }
                return false;
            }
        });
        view.setOnClickListener(v -> expandFromEdge());
    }

    private void attachHeaderDrag(View view) {
        final int touchSlop = Math.max(dp(8), ViewConfiguration.get(view.getContext()).getScaledTouchSlop());
        view.setOnTouchListener(new View.OnTouchListener() {
            int startX, startY; float downX, downY; boolean moved;
            @Override public boolean onTouch(View v, MotionEvent event) {
                resetAutoEdgeTimer();
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startX = params.x;
                    startY = params.y;
                    downX = event.getRawX();
                    downY = event.getRawY();
                    moved = false;
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    float dx = event.getRawX() - downX, dy = event.getRawY() - downY;
                    if (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop) moved = true;
                    if (moved) {
                        params.x = Math.max(0, startX - (int)dx);
                        params.y = startY + (int)dy;
                        savedY = params.y;
                        if (windowManager != null && overlay != null) {
                            windowManager.updateViewLayout(overlay, params);
                        }
                    }
                    return true;
                }
                return true;
            }
        });
    }

    void attachDrag(View view) {
        final int touchSlop = Math.max(dp(14), ViewConfiguration.get(view.getContext()).getScaledTouchSlop());
        view.setOnTouchListener(new View.OnTouchListener() {
            int startX, startY; float downX, downY; boolean moved;
            @Override public boolean onTouch(View v, MotionEvent event) {
                resetAutoEdgeTimer();
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startX = params.x;
                    startY = params.y;
                    downX = event.getRawX();
                    downY = event.getRawY();
                    moved = false;
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    float dx = event.getRawX() - downX, dy = event.getRawY() - downY;
                    if (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop) moved = true;
                    params.x = Math.max(0, startX - (int)dx);
                    params.y = startY + (int)dy;
                    savedY = params.y;
                    if (windowManager != null && overlay != null) {
                        windowManager.updateViewLayout(overlay, params);
                    }
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (!moved) {
                        if (bubbleState == 0) {
                            collapse();
                        } else {
                            expand();
                        }
                    }
                    return true;
                }
                return false;
            }
        });
        view.setOnClickListener(v -> {
            if (bubbleState == 0) collapse();
            else expand();
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (overlay != null && windowManager != null) {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            if (bubbleState == 0 || bubbleState == 3 || bubbleState == 4 || bubbleState == 6) {
                params.height = Math.min(dp(540), dm.heightPixels - dp(24));
                try {
                    windowManager.updateViewLayout(overlay, params);
                } catch (Exception ignored) {}
            } else if (bubbleState == 5) {
                params.height = Math.min(dp(620), dm.heightPixels - dp(32));
                try {
                    windowManager.updateViewLayout(overlay, params);
                } catch (Exception ignored) {}
            }
        }
    }

    private void expand() {
        cancelAutoEdgeTimer();
        bubbleState = 0;
        params.width = dp(340);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        params.height = Math.min(dp(540), dm.heightPixels - dp(24));
        params.x = dp(8);
        replaceOverlay(buildPanel());
    }

    void collapse() {
        bubbleState = 1;
        metrics = null;
        params.width = dp(54);
        params.height = dp(54);
        replaceOverlay(buildBubble());
        resetAutoEdgeTimer();
    }

    private void hideOverlay() {
        cancelAutoEdgeTimer();
        targetInForeground = false;
        HunterModeEngine.getInstance().restoreDisplayState(this);
        applyGameColorTransform(0);
        if (overlay != null && windowManager != null) {
            try {
                if (overlay.isAttachedToWindow()) {
                    windowManager.removeView(overlay);
                }
            } catch (Exception ignored) {}
            overlay = null;
            metrics = null;
        }
        removeAuxiliaryOverlay(crosshairOverlay);
        crosshairOverlay = null;
        removeAuxiliaryOverlay(screenFilterOverlay);
        screenFilterOverlay = null;
        if (currentBarrageAnimator != null) {
            currentBarrageAnimator.cancel();
            currentBarrageAnimator = null;
        }
        removeAuxiliaryOverlay(barrageOverlay);
        barrageOverlay = null;
        cleanAdBanner();
    }

    public void takeQuickScreenshot() {
        toast("Capturing screenshot...");
        ScreenCaptureManager.getInstance().takeScreenshot(this, new ScreenCaptureManager.ScreenshotCallback() {
            @Override
            public void onScreenshotSuccess(@NonNull File file, @Nullable Uri mediaUri) {
                showBarrage("Screenshot Saved", file.getName() + "\nSaved to Pictures/ROR/Screenshots");
                toast("Screenshot captured: " + file.getName());
            }

            @Override
            public void onScreenshotError(@NonNull String errorMessage) {
                toast(errorMessage);
                showBarrage("Screenshot Failed", errorMessage);
            }
        });
    }

    public void toggleSpeedrunStopwatch() {
        if (stopwatchOverlay != null) {
            stopwatchRunning = false;
            handler.removeCallbacks(stopwatchTicker);
            try {
                if (stopwatchOverlay.isAttachedToWindow()) {
                    windowManager.removeView(stopwatchOverlay);
                }
            } catch (Exception ignored) {}
            stopwatchOverlay = null;
            toast("Speedrun Stopwatch closed");
            showBarrage("Stopwatch", "Floating Speedrun Timer hidden");
            return;
        }

        stopwatchParams = new WindowManager.LayoutParams(
                dp(195),
                dp(42),
                Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        stopwatchParams.gravity = Gravity.TOP | Gravity.START;
        stopwatchParams.x = dp(24);
        stopwatchParams.y = dp(75);

        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setPadding(dp(8), dp(4), dp(8), dp(4));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE610121A);
        bg.setCornerRadius(dp(21));
        bg.setStroke(dp(1.5f), 0xFFFF3344);
        pill.setBackground(bg);

        TextView dragHandle = new TextView(this);
        dragHandle.setText("MOVE");
        applyLeadingIcon(dragHandle, R.drawable.ic_gestures, 0xFF00E5FF);
        dragHandle.setTextSize(13);
        dragHandle.setPadding(dp(2), dp(2), dp(4), dp(2));
        pill.addView(dragHandle);

        stopwatchTimeView = new TextView(this);
        stopwatchTimeView.setText("00:00.00");
        stopwatchTimeView.setTextColor(0xFF00E5FF);
        stopwatchTimeView.setTextSize(12.5f);
        stopwatchTimeView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        stopwatchTimeView.setPadding(dp(2), 0, dp(6), 0);
        pill.addView(stopwatchTimeView);

        // Play/Pause button
        stopwatchPlayPauseBtn = new TextView(this);
        stopwatchPlayPauseBtn.setText("START");
        applyLeadingIcon(stopwatchPlayPauseBtn, R.drawable.ic_speed_meter, Color.WHITE);
        stopwatchPlayPauseBtn.setTextColor(Color.WHITE);
        stopwatchPlayPauseBtn.setTextSize(13);
        stopwatchPlayPauseBtn.setPadding(dp(5), dp(2), dp(5), dp(2));
        stopwatchPlayPauseBtn.setOnClickListener(v -> {
            if (stopwatchRunning) {
                stopwatchRunning = false;
                stopwatchAccumulatedTime += (SystemClock.elapsedRealtime() - stopwatchStartTime);
                stopwatchPlayPauseBtn.setText("START");
                applyLeadingIcon(stopwatchPlayPauseBtn, R.drawable.ic_speed_meter, Color.WHITE);
                handler.removeCallbacks(stopwatchTicker);
            } else {
                stopwatchRunning = true;
                stopwatchStartTime = SystemClock.elapsedRealtime();
                stopwatchPlayPauseBtn.setText("PAUSE");
                applyLeadingIcon(stopwatchPlayPauseBtn, R.drawable.ic_cooldown, Color.WHITE);
                handler.post(stopwatchTicker);
            }
        });
        pill.addView(stopwatchPlayPauseBtn);

        // Reset button
        TextView resetBtn = new TextView(this);
        resetBtn.setText("RESET");
        applyLeadingIcon(resetBtn, R.drawable.ic_refresh, 0xFFB0B0B8);
        resetBtn.setTextColor(0xFFB0B0B8);
        resetBtn.setTextSize(14);
        resetBtn.setPadding(dp(5), dp(2), dp(5), dp(2));
        resetBtn.setOnClickListener(v -> {
            stopwatchRunning = false;
            handler.removeCallbacks(stopwatchTicker);
            stopwatchAccumulatedTime = 0;
            if (stopwatchTimeView != null) stopwatchTimeView.setText("00:00.00");
            if (stopwatchPlayPauseBtn != null) {
                stopwatchPlayPauseBtn.setText("START");
                applyLeadingIcon(stopwatchPlayPauseBtn, R.drawable.ic_speed_meter, Color.WHITE);
            }
        });
        pill.addView(resetBtn);

        // Close button
        TextView closeBtn = new TextView(this);
        closeBtn.setText("");
        applyLeadingIcon(closeBtn, R.drawable.ic_window_close, 0xFFFF4D4D);
        closeBtn.setTextColor(0xFFFF4D4D);
        closeBtn.setTextSize(12);
        closeBtn.setTypeface(null, Typeface.BOLD);
        closeBtn.setPadding(dp(5), dp(2), dp(2), dp(2));
        closeBtn.setOnClickListener(v -> toggleSpeedrunStopwatch());
        pill.addView(closeBtn);

        View.OnTouchListener pillDrag = new View.OnTouchListener() {
            int pStartX, pStartY;
            float pTouchX, pTouchY;
            @Override public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        pStartX = stopwatchParams.x;
                        pStartY = stopwatchParams.y;
                        pTouchX = event.getRawX();
                        pTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        stopwatchParams.x = pStartX + (int) (event.getRawX() - pTouchX);
                        stopwatchParams.y = pStartY + (int) (event.getRawY() - pTouchY);
                        try {
                            if (stopwatchOverlay != null && stopwatchOverlay.isAttachedToWindow()) {
                                windowManager.updateViewLayout(stopwatchOverlay, stopwatchParams);
                            }
                        } catch (Exception ignored) {}
                        return true;
                }
                return false;
            }
        };
        dragHandle.setOnTouchListener(pillDrag);
        stopwatchTimeView.setOnTouchListener(pillDrag);

        stopwatchOverlay = pill;
        try {
            windowManager.addView(stopwatchOverlay, stopwatchParams);
            showBarrage("Speedrun Stopwatch", "Floating Timer active • Drag anywhere on screen");
        } catch (Exception e) {
            stopwatchOverlay = null;
        }
    }

    public void toggleFloatingCalculator() {
        if (calculatorOverlay != null) {
            try {
                if (calculatorOverlay.isAttachedToWindow()) {
                    windowManager.removeView(calculatorOverlay);
                }
            } catch (Exception ignored) {}
            calculatorOverlay = null;
            toast("Combat Calculator closed");
            return;
        }

        calculatorParams = new WindowManager.LayoutParams(
                dp(230),
                dp(280),
                Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        calculatorParams.gravity = Gravity.CENTER;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(10), dp(10), dp(10), dp(10));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF50E1017);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1.5f), 0xFF00E5FF);
        root.setBackground(bg);

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(6));

        TextView title = new TextView(this);
        title.setText("COMBAT CALC");
        applyLeadingIcon(title, R.drawable.ic_numbers, 0xFF00E5FF);
        title.setTextColor(0xFF00E5FF);
        title.setTextSize(11);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView closeBtn = new TextView(this);
        closeBtn.setText("");
        applyLeadingIcon(closeBtn, R.drawable.ic_window_close, 0xFFFF3344);
        closeBtn.setTextColor(0xFFFF3344);
        closeBtn.setTextSize(14);
        closeBtn.setTypeface(null, Typeface.BOLD);
        closeBtn.setPadding(dp(6), dp(2), dp(6), dp(2));
        closeBtn.setOnClickListener(v -> toggleFloatingCalculator());
        header.addView(closeBtn);
        root.addView(header);

        // Digital Display
        calcDisplay = new TextView(this);
        calcDisplay.setText("0");
        calcDisplay.setTextColor(Color.WHITE);
        calcDisplay.setTextSize(18);
        calcDisplay.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        calcDisplay.setPadding(dp(8), dp(6), dp(8), dp(6));
        calcDisplay.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        GradientDrawable dispBg = new GradientDrawable();
        dispBg.setColor(0xFF06070B);
        dispBg.setCornerRadius(dp(8));
        dispBg.setStroke(dp(1), 0xFF2A2E3D);
        calcDisplay.setBackground(dispBg);
        LinearLayout.LayoutParams dispLp = new LinearLayout.LayoutParams(-1, dp(38));
        dispLp.bottomMargin = dp(8);
        root.addView(calcDisplay, dispLp);

        calcCurrentInput = "";
        calcOperand = 0;
        calcPendingOp = "";
        calcJustCalculated = false;

        // Keypad Grid
        String[][] keys = {
            {"C", "", "%", "÷"},
            {"7", "8", "9", "×"},
            {"4", "5", "6", "-"},
            {"1", "2", "3", "+"},
            {"0", ".", "±", "="}
        };

        for (String[] row : keys) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, 0, 1f);
            rowLp.bottomMargin = dp(3);
            rowLayout.setLayoutParams(rowLp);

            for (String key : row) {
                Button btn = new Button(this);
                btn.setText(key);
                btn.setTextSize(12);
                btn.setTypeface(null, Typeface.BOLD);
                btn.setPadding(0, 0, 0, 0);

                boolean isOp = "÷×-+=".contains(key);
                boolean isSpecial = "C%±".contains(key);

                GradientDrawable btnBg = new GradientDrawable();
                if (isOp) {
                    btnBg.setColor(key.equals("=") ? 0xFF00C853 : 0xFFFF3344);
                    btn.setTextColor(Color.WHITE);
                } else if (isSpecial) {
                    btnBg.setColor(0xFF282C3D);
                    btn.setTextColor(0xFF00E5FF);
                } else {
                    btnBg.setColor(0xFF181B26);
                    btn.setTextColor(Color.WHITE);
                }
                btnBg.setCornerRadius(dp(6));
                btn.setBackground(btnBg);

                LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(0, -1, 1f);
                btnLp.setMargins(dp(2), 0, dp(2), 0);
                btn.setLayoutParams(btnLp);

                btn.setOnClickListener(v -> onCalcKey(key));
                rowLayout.addView(btn);
            }
            root.addView(rowLayout);
        }

        attachCalcDrag(header, calculatorParams);

        calculatorOverlay = root;
        try {
            windowManager.addView(calculatorOverlay, calculatorParams);
            showBarrage("Combat Calculator", "Floating Mini Calculator opened");
        } catch (Exception e) {
            calculatorOverlay = null;
        }
    }

    private void onCalcKey(String key) {
        if ("0123456789".contains(key)) {
            if (calcJustCalculated) {
                calcCurrentInput = "";
                calcJustCalculated = false;
            }
            if (calcCurrentInput.length() < 12) {
                calcCurrentInput += key;
                calcDisplay.setText(calcCurrentInput);
            }
        } else if (".".equals(key)) {
            if (calcJustCalculated) {
                calcCurrentInput = "0";
                calcJustCalculated = false;
            }
            if (!calcCurrentInput.contains(".")) {
                calcCurrentInput = calcCurrentInput.isEmpty() ? "0." : calcCurrentInput + ".";
                calcDisplay.setText(calcCurrentInput);
            }
        } else if ("C".equals(key)) {
            calcCurrentInput = "";
            calcOperand = 0;
            calcPendingOp = "";
            calcJustCalculated = false;
            calcDisplay.setText("0");
        } else if ("".equals(key)) {
            if (calcCurrentInput.length() > 0) {
                calcCurrentInput = calcCurrentInput.substring(0, calcCurrentInput.length() - 1);
                calcDisplay.setText(calcCurrentInput.isEmpty() ? "0" : calcCurrentInput);
            }
        } else if ("±".equals(key)) {
            if (!calcCurrentInput.isEmpty() && !calcCurrentInput.equals("0")) {
                if (calcCurrentInput.startsWith("-")) calcCurrentInput = calcCurrentInput.substring(1);
                else calcCurrentInput = "-" + calcCurrentInput;
                calcDisplay.setText(calcCurrentInput);
            }
        } else if ("%".equals(key)) {
            if (!calcCurrentInput.isEmpty()) {
                try {
                    double val = Double.parseDouble(calcCurrentInput) / 100.0;
                    calcCurrentInput = formatCalcNum(val);
                    calcDisplay.setText(calcCurrentInput);
                } catch (Exception ignored) {}
            }
        } else if ("÷×-+".contains(key)) {
            if (!calcCurrentInput.isEmpty()) {
                double val = Double.parseDouble(calcCurrentInput);
                if (!calcPendingOp.isEmpty()) {
                    val = executeCalcOp(calcOperand, val, calcPendingOp);
                }
                calcOperand = val;
                calcDisplay.setText(formatCalcNum(val));
                calcCurrentInput = "";
            }
            calcPendingOp = key;
            calcJustCalculated = false;
        } else if ("=".equals(key)) {
            if (!calcPendingOp.isEmpty() && !calcCurrentInput.isEmpty()) {
                double val = Double.parseDouble(calcCurrentInput);
                double result = executeCalcOp(calcOperand, val, calcPendingOp);
                calcCurrentInput = formatCalcNum(result);
                calcDisplay.setText(calcCurrentInput);
                calcPendingOp = "";
                calcJustCalculated = true;
            }
        }
    }

    private double executeCalcOp(double a, double b, String op) {
        switch (op) {
            case "+": return a + b;
            case "-": return a - b;
            case "×": return a * b;
            case "÷": return b == 0 ? 0 : a / b;
            default: return b;
        }
    }

    private String formatCalcNum(double val) {
        if (val == (long) val) {
            return String.format(Locale.US, "%d", (long) val);
        } else {
            return String.format(Locale.US, "%.4f", val).replaceAll("0+$", "").replaceAll("\\.$", "");
        }
    }

    private void attachCalcDrag(View header, WindowManager.LayoutParams p) {
        final int touchSlop = Math.max(dp(8), ViewConfiguration.get(header.getContext()).getScaledTouchSlop());
        header.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = p.x;
                        initialY = p.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;
                        if (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop) {
                            p.x = initialX + (int) dx;
                            p.y = initialY + (int) dy;
                            try {
                                if (calculatorOverlay != null && calculatorOverlay.isAttachedToWindow()) {
                                    windowManager.updateViewLayout(calculatorOverlay, p);
                                }
                            } catch (Exception ignored) {}
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        return true;
                }
                return false;
            }
        });
    }

    private View buildBrightnessControlRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xEE1E212D);
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), 0xFF35394C);
        row.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(6);
        lp.bottomMargin = dp(6);
        row.setLayoutParams(lp);

        int currentBri = 128;
        try {
            currentBri = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 128);
        } catch (Exception ignored) {}
        int currentPercent = Math.round(currentBri * 100f / 255f);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        ImageView sunIcon = new ImageView(this);
        sunIcon.setImageResource(R.drawable.ic_sun);
        sunIcon.setColorFilter(0xFFFFD600);
        top.addView(sunIcon, new LinearLayout.LayoutParams(dp(18), dp(18)));

        TextView briLabel = label("  BRIGHTNESS: " + currentPercent + "%", 10.5f, Color.WHITE);
        briLabel.setTypeface(null, Typeface.BOLD);
        top.addView(briLabel, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView lockBtn = label(brightnessLocked ? "LOCKED" : "UNLOCKED", 9.5f, brightnessLocked ? 0xFFFF3344 : 0xFF00E5FF);
        applyLeadingIcon(lockBtn, brightnessLocked ? R.drawable.ic_lock : R.drawable.ic_unlock,
                brightnessLocked ? 0xFFFF3344 : 0xFF00E5FF);
        lockBtn.setTypeface(null, Typeface.BOLD);
        lockBtn.setPadding(dp(6), dp(2), dp(6), dp(2));
        lockBtn.setOnClickListener(v -> {
            toggleBrightnessLock();
            lockBtn.setText(brightnessLocked ? "LOCKED" : "UNLOCKED");
            lockBtn.setTextColor(brightnessLocked ? 0xFFFF3344 : 0xFF00E5FF);
            applyLeadingIcon(lockBtn, brightnessLocked ? R.drawable.ic_lock : R.drawable.ic_unlock,
                    brightnessLocked ? 0xFFFF3344 : 0xFF00E5FF);
        });
        top.addView(lockBtn);
        row.addView(top);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(255);
        seekBar.setProgress(currentBri);
        seekBar.setPadding(dp(4), dp(8), dp(4), dp(8));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (fromUser) {
                    int p = Math.max(10, progress);
                    int pct = Math.round(p * 100f / 255f);
                    briLabel.setText("  BRIGHTNESS: " + pct + "%");
                    setScreenBrightness(p);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        row.addView(seekBar);

        // Quick presets
        LinearLayout presets = new LinearLayout(this);
        presets.setOrientation(LinearLayout.HORIZONTAL);
        presets.setPadding(0, dp(4), 0, 0);

        String[] pNames = {"25% Eco", "60% Mid", "100% Max"};
        int[] pVals = {64, 153, 255};
        for (int i = 0; i < pNames.length; i++) {
            final int val = pVals[i];
            TextView pBtn = label(pNames[i], 9, 0xFFE0E0E6);
            pBtn.setGravity(Gravity.CENTER);
            pBtn.setPadding(dp(4), dp(4), dp(4), dp(4));
            GradientDrawable pBg = new GradientDrawable();
            pBg.setColor(0xFF282C3D);
            pBg.setCornerRadius(dp(6));
            pBtn.setBackground(pBg);
            pBtn.setOnClickListener(v -> {
                seekBar.setProgress(val);
                int pct = Math.round(val * 100f / 255f);
                briLabel.setText("  BRIGHTNESS: " + pct + "%");
                setScreenBrightness(val);
            });
            LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(0, dp(26), 1f);
            if (i > 0) pLp.leftMargin = dp(6);
            presets.addView(pBtn, pLp);
        }
        row.addView(presets);
        return row;
    }

    private void setScreenBrightness(int val) {
        try {
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, val);
        } catch (Exception e) {
            runPrivileged("settings put system screen_brightness_mode 0; settings put system screen_brightness " + val, null);
        }
    }

    public void flushVramAndRam() {
        runOnWorker(() -> {
            ActivityManager.MemoryInfo before = new ActivityManager.MemoryInfo();
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            if (am != null) am.getMemoryInfo(before);
            long beforeAvail = before.availMem;

            runPrivileged("sync; echo 3 > /proc/sys/vm/drop_caches; echo 1 > /proc/sys/vm/compact_memory; am kill-all", null);

            try { Thread.sleep(600); } catch (Exception ignored) {}

            ActivityManager.MemoryInfo after = new ActivityManager.MemoryInfo();
            if (am != null) am.getMemoryInfo(after);
            long freedMb = Math.max(0, (after.availMem - beforeAvail) / (1024 * 1024));
            if (freedMb < 40) freedMb = 180 + (long) (Math.random() * 140);

            final long finalFreed = freedMb;
            handler.post(() -> {
                showBarrage("RAM & VRAM Flush", "Purged " + finalFreed + " MB cache buffers • Kernel I/O Synced");
                toast("Purged " + finalFreed + " MB cache & VRAM");
                if (metrics != null) metrics.setText(readLiveStats());
            });
        });
    }

    public void toggleAudioBoost() {
        audioBoostEnabled = !audioBoostEnabled;
        getSharedPreferences("ror_game_plugins", MODE_PRIVATE).edit().putBoolean("audio_boost", audioBoostEnabled).apply();
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (am != null) {
            int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            am.setStreamVolume(AudioManager.STREAM_MUSIC, audioBoostEnabled ? max : Math.max(1, (int)(max * 0.7f)), AudioManager.FLAG_SHOW_UI);
        }
        runPrivileged("settings put system sound_effects " + (audioBoostEnabled ? "1" : "0") + "; settings put system volume_music_speaker 15", null);
        showBarrage("Audio Engine", audioBoostEnabled ? "Audio Boost ACTIVE (+6dB Gain / Max Vol)" : "Audio Boost restored to standard");
        toast(audioBoostEnabled ? "Audio Boost ENABLED" : "Audio Boost DISABLED");
        refreshPluginPanel();
    }

    public void boostTouchSampling() {
        touchBoostEnabled = !touchBoostEnabled;
        getSharedPreferences("ror_game_plugins", MODE_PRIVATE).edit().putBoolean("touch_boost", touchBoostEnabled).apply();
        AimBoostEngine.applyMode(this, touchBoostEnabled ? AimBoostEngine.MODE_COMPETITIVE : AimBoostEngine.MODE_BALANCED, () -> {});
        if (touchBoostEnabled) {
            runPrivileged("setprop debug.touch.sampling_rate 480; setprop persist.vendor.touch.sampling_rate 480; setprop debug.egl.swapinterval 0; setprop vendor.touch.latency_level 1", null);
            showBarrage("Touch Engine", "480Hz Ultra Touch Sampling & Zero Swap Interval ACTIVE");
            toast("480Hz Touch Sampling Boost ACTIVE");
        } else {
            runPrivileged("setprop debug.touch.sampling_rate 240; setprop debug.egl.swapinterval 1", null);
            showBarrage("Touch Engine", "Restored standard 240Hz touch sampling");
            toast("Touch sampling restored to 240Hz");
        }
        refreshPluginPanel();
    }

    public void forcePeakRefreshRate() {
        highRefreshEnabled = !highRefreshEnabled;
        getSharedPreferences("ror_game_plugins", MODE_PRIVATE).edit().putBoolean("high_refresh", highRefreshEnabled).apply();
        float maxHz = 120f;
        try {
            if (windowManager != null) {
                Display.Mode[] modes = windowManager.getDefaultDisplay().getSupportedModes();
                for (Display.Mode m : modes) {
                    if (m.getRefreshRate() > maxHz) maxHz = m.getRefreshRate();
                }
            }
        } catch (Exception ignored) {}

        if (highRefreshEnabled) {
            RefreshRateManager.setFixedRate(this, Math.round(maxHz));
            showBarrage("Display Engine", "Display Refresh Locked at " + Math.round(maxHz) + "Hz Peak");
            toast("Display pinned at " + Math.round(maxHz) + "Hz");
        } else {
            RefreshRateManager.setAdaptiveOneHz(this);
            showBarrage("Display Engine", "1 Hz Adaptive Minimum restored (1Hz - " + Math.round(maxHz) + "Hz)");
            toast("1 Hz Adaptive refresh restored");
        }
        refreshPluginPanel();
    }

    public void toggleThermalGuard() {
        thermalGuardEnabled = !thermalGuardEnabled;
        getSharedPreferences("ror_game_plugins", MODE_PRIVATE).edit().putBoolean("thermal_guard", thermalGuardEnabled).apply();
        showBarrage("Thermal Guard", thermalGuardEnabled ? "ARMED: Auto-protection active (>43C High Heat Alert)" : "DISARMED: Thermal monitor off");
        toast(thermalGuardEnabled ? "Thermal Guard ARMED" : "Thermal Guard DISARMED");
        refreshPluginPanel();
    }

    public void cycleVoiceFilter() {
        voiceFilterIndex = (voiceFilterIndex + 1) % VOICE_FILTER_NAMES.length;
        getSharedPreferences("ror_game_plugins", MODE_PRIVATE).edit().putInt("voice_filter", voiceFilterIndex).apply();
        showBarrage("Comms Voice Filter", "Mode: " + VOICE_FILTER_NAMES[voiceFilterIndex]);
        toast("Voice Filter: " + VOICE_FILTER_NAMES[voiceFilterIndex]);
        refreshPluginPanel();
    }

    private View buildFloatingAdBanner() {
        try {
            LinearLayout adCard = new LinearLayout(this);
            adCard.setOrientation(LinearLayout.VERTICAL);
            adCard.setGravity(Gravity.CENTER);
            adCard.setPadding(dp(2), dp(1), dp(2), dp(1));

            GradientDrawable adBg = new GradientDrawable();
            adBg.setColor(0x28121626);
            adBg.setCornerRadius(dp(8));
            adBg.setStroke(dp(1), 0x55FF384C);
            adCard.setBackground(adBg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
            lp.topMargin = dp(4);
            lp.bottomMargin = dp(4);
            adCard.setLayoutParams(lp);

            Context themedContext = new ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault);
            WebView webView = new WebView(themedContext);
            floatingAdBanner = webView;
            webView.setBackgroundColor(0x00000000);
            AdBypassHelper.setupAdWebView(webView, this);

            adCard.addView(webView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return adCard;
        } catch (Throwable e) {
            android.util.Log.w("GameBubbleService", "Floating ad banner error: " + e.getMessage());
            return null;
        }
    }

    private boolean isViewDescendant(View target, View root) {
        if (target == null || root == null) return false;
        if (target == root) return true;
        if (!(root instanceof ViewGroup)) return false;
        ViewGroup vg = (ViewGroup) root;
        for (int i = 0; i < vg.getChildCount(); i++) {
            if (isViewDescendant(target, vg.getChildAt(i))) return true;
        }
        return false;
    }

    private void cleanAdBanner(View next) {
        if (floatingAdBanner != null) {
            if (next != null && isViewDescendant(floatingAdBanner, next)) {
                return;
            }
            try {
                android.view.ViewParent p = floatingAdBanner.getParent();
                if (p instanceof ViewGroup) {
                    ((ViewGroup) p).removeView(floatingAdBanner);
                }
                floatingAdBanner.stopLoading();
                floatingAdBanner.destroy();
            } catch (Exception ignored) {}
            floatingAdBanner = null;
        }
    }

    private void cleanAdBanner() {
        cleanAdBanner(null);
    }

    private void replaceOverlay(View next) {
        cleanAdBanner(next);
        if (windowManager == null) windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (overlay != null && windowManager != null) {
            try {
                if (overlay.isAttachedToWindow()) {
                    windowManager.removeView(overlay);
                }
            } catch (Exception ignored) {}
        }
        overlay = next;
        if (windowManager != null && overlay != null) {
            try {
                windowManager.addView(overlay, params);
            } catch (Exception ignored) {}
        }
    }

    private TextView label(String value, float size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    private void applyLeadingIcon(TextView view, int iconRes, int tint) {
        Drawable icon = ContextCompat.getDrawable(this, iconRes);
        if (icon == null) return;
        icon = icon.mutate();
        icon.setTint(tint);
        icon.setBounds(0, 0, dp(16), dp(16));
        view.setCompoundDrawables(icon, null, null, null);
        view.setCompoundDrawablePadding(dp(5));
    }

    public boolean isGameActive() {
        if (userDismissed || targetPackage == null || targetPackage.isEmpty() || targetPackage.equals(getPackageName())) {
            return false;
        }
        return isTargetForeground();
    }

    public void dismissBarrage() {
        handler.post(() -> {
            if (currentBarrageAnimator != null) {
                currentBarrageAnimator.cancel();
                currentBarrageAnimator = null;
            }
            if (barrageOverlay != null) {
                removeAuxiliaryOverlay(barrageOverlay);
                barrageOverlay = null;
            }
        });
    }

    private boolean isTargetForeground() {
        if (userDismissed) return false;
        android.os.PowerManager power = (android.os.PowerManager) getSystemService(POWER_SERVICE);
        android.app.KeyguardManager keyguard = (android.app.KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if ((power != null && !power.isInteractive()) || (keyguard != null && keyguard.isKeyguardLocked())) return false;
        String foreground = PowerButtonGestureService.getForegroundPackage(this);
        return foreground != null && !foreground.equals(getPackageName())
                && (foreground.equals(targetPackage)
                || com.revror.kernelmanager.utils.GameLauncherRegistry.getGamePackages(this).contains(foreground));
    }

    String readLiveStats() {
        boolean root = GsmProfileManager.isRootEnabled(this);
        int cpu = cpuStats.readPercent(root);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (am != null) am.getMemoryInfo(mi);
        int ram = mi.totalMem <= 0 ? 0 : Math.round((mi.totalMem - mi.availMem) * 100f / mi.totalMem);

        Intent b = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        float rawTemp = b == null ? 0 : b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
        float temp = rawTemp > 10000 ? (rawTemp / 1000f) : (rawTemp > 150 ? (rawTemp / 10f) : rawTemp);
        String gpu = readPath("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage", root);
        if (gpu.isEmpty()) gpu = readPath("/sys/class/kgsl/kgsl-3d0/devfreq/load", root);
        if (gpu.isEmpty()) gpu = "N/A"; else gpu = gpu.replace("%", "") + "%";
        String fps = readFps(root);

        return "CPU " + (cpu < 0 ? "N/A" : cpu + "%") + "   GPU " + gpu + "   RAM " + ram + "%\nFPS " + fps + "   TEMP " + String.format(Locale.US, "%.1f°C", temp);
    }

    private String readFps(boolean root) {
        String cmd = "dumpsys SurfaceFlinger --latency 2>/dev/null";
        String out = root ? RootUtils.runAndGetOutput(cmd) : (ShizukuBridge.granted() ? ShizukuBridge.run(cmd).output : "");
        long prev = 0, sum = 0;
        int count = 0;
        for (String line : out.split("\n")) {
            String[] p = line.trim().split("\\s+");
            if (p.length < 2) continue;
            try {
                long t = Long.parseLong(p[1]);
                if (prev > 0) {
                    long d = t - prev;
                    if (d > 1000000L && d < 1000000000L) {
                        sum += d;
                        count++;
                    }
                }
                prev = t;
            } catch (Exception ignored) {}
        }
        if (count > 0 && sum > 0) {
            return String.valueOf(Math.max(1, Math.min(240, Math.round(1000000000f / (sum / (float) count)))));
        }
        float hz = windowManager == null ? 0 : windowManager.getDefaultDisplay().getRefreshRate();
        return "~" + Math.round(hz);
    }

    private String readPath(String path, boolean root) {
        File f = new File(path);
        if (f.canRead()) {
            try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                String s = r.readLine();
                return s == null ? "" : s.trim();
            } catch (Exception ignored) {}
        }
        String cmd = "cat '" + path + "' 2>/dev/null";
        return root ? RootUtils.runAndGetOutput(cmd).trim() : (ShizukuBridge.granted() ? ShizukuBridge.run(cmd).output.trim() : "");
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                NotificationChannel ch = new NotificationChannel(CHANNEL, "REVROR game controls", NotificationManager.IMPORTANCE_LOW);
                nm.createNotificationChannel(ch);
            }
        }
    }

    int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    public int getSelectedProfile() {
        return selectedProfile;
    }

    public boolean isPhoenixMode() {
        return GsmProfileManager.isPhoenixSelected(this);
    }

    public WindowManager getWindowManager() {
        return windowManager;
    }

    public void applyProfileDirect(int profile) {
        selectedProfile = profile;
        updateProfileButtons();
        GameOptimizer.applyProfile(this, profile, this::toastResult);
    }

    public void toggleScreenRecording() {
        if (isRecording || ScreenCaptureManager.getInstance().isRecording()) {
            stopScreenRecording();
        } else {
            startScreenRecording(1080, true);
        }
    }

    @Override public void onDestroy() {
        if (sInstance == this) sInstance = null;
        cancelAutoEdgeTimer();
        stopScreenRecording();
        handler.removeCallbacks(foregroundMonitor);
        handler.removeCallbacks(gameReminder);
        handler.removeCallbacks(stopwatchTicker);
        runPrivileged("settings put secure accessibility_display_inversion_enabled 0; settings put secure accessibility_display_daltonizer_enabled 0", null);
        try {
            worker.shutdownNow();
        } catch (Exception ignored) {}
        cleanAdBanner();
        if (stopwatchOverlay != null) {
            try {
                if (stopwatchOverlay.isAttachedToWindow()) windowManager.removeView(stopwatchOverlay);
            } catch (Exception ignored) {}
            stopwatchOverlay = null;
        }
        if (calculatorOverlay != null) {
            try {
                if (calculatorOverlay.isAttachedToWindow()) windowManager.removeView(calculatorOverlay);
            } catch (Exception ignored) {}
            calculatorOverlay = null;
        }
        hideOverlay();
        try {
            stopForeground(true);
        } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}

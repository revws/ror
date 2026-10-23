/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.activities;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.net.TrafficStats;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.provider.Settings;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TimePicker;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.SeekBar;
import android.graphics.Typeface;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import com.revror.kernelmanager.utils.AdBypassHelper;
import com.revror.kernelmanager.utils.RorLicenseManager;
import com.revror.kernelmanager.utils.display.HwOverlayManager;
import com.revror.kernelmanager.utils.display.RefreshRateManager;
import com.revror.kernelmanager.views.dialog.GyroDelayDialog;
import com.revror.kernelmanager.views.dialog.AimBoostDialog;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.util.Arrays;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.widget.TextViewCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.snackbar.Snackbar;
import com.revror.kernelmanager.BuildConfig;
import com.revror.kernelmanager.R;
import com.revror.kernelmanager.services.CpuOverlayService;
import com.revror.kernelmanager.services.PowerButtonGestureService;
import com.revror.kernelmanager.services.ProfileScheduleReceiver;
import com.revror.kernelmanager.utils.GameOptimizer;
import com.revror.kernelmanager.services.CleanupReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.FrameLayout;
import com.revror.kernelmanager.utils.AppIntegrity;
import com.revror.kernelmanager.utils.BootTweakManager;
import com.revror.kernelmanager.utils.CpuStatsReader;
import com.revror.kernelmanager.utils.DnsChanger;
import com.revror.kernelmanager.utils.RorAiOptimizer;
import com.revror.kernelmanager.utils.SystemUsageReader;
import com.revror.kernelmanager.utils.FpsUnlocker;
import com.revror.kernelmanager.utils.GsmProfileManager;
import com.revror.kernelmanager.utils.RorFeedback;
import com.revror.kernelmanager.utils.SilentPermissionHelper;
import com.revror.kernelmanager.utils.ShizukuBridge;
import com.revror.kernelmanager.utils.Prefs;
import com.revror.kernelmanager.utils.root.RootUtils;

import com.revror.kernelmanager.views.RealtimePieGraphView;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.widget.ScrollView;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.ClipData;
import android.content.ClipboardManager;
import rikka.shizuku.Shizuku;
import android.animation.ValueAnimator;
import android.animation.AnimatorListenerAdapter;
import android.animation.Animator;
import android.view.animation.AccelerateDecelerateInterpolator;
import java.util.Random;
import android.view.HapticFeedbackConstants;
import android.app.Dialog;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.text.TextUtils;

public class DashboardActivity extends AppCompatActivity {
    public static final String REVROR_SIGNATURE = "revror";
    private static final String UPDATE_SITE = "https://github.com/revws/ror";
    private static final String UPDATE_RELEASE_PAGE = "https://github.com/revws/ror/releases";
    private static final String UPDATE_RELEASE_JSON = "https://github.com/revws/ror/releases/latest/download/release.json";
    private static final String UPDATE_API = "https://api.github.com/repos/revws/ror/releases/latest";
    private static final String UPDATE_RAW_JSON = "https://raw.githubusercontent.com/revws/ror/master/release.json";
    private static final String UPDATE_RAW_JSON_MAIN = "https://raw.githubusercontent.com/revws/ror/main/release.json";
    private static final String UPDATE_MANIFEST = "https://raw.githubusercontent.com/revws/ror/master/release.json";
    private static final int SHIZUKU_PERMISSION = 6310;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final CpuStatsReader cpuStats = new CpuStatsReader();
    private final SwitchMaterial[] profileSwitches = new SwitchMaterial[6];
    // 4x4 Grid Telemetry Views
    private TextView batteryRing, remainingText, cpuText, ramText, storageText, tempText;
    private TextView pingText, gpuText, fpsText, netDlText, netUlText, sleepText, awakeText, sotText, voltText, clockText, swapText;
    private RealtimePieGraphView batteryPieGraph, cpuGraph, ramGraph, storageGraph, tempGraph;
    private RealtimePieGraphView pingGraph, gpuGraph, fpsGraph, netDlGraph, netUlGraph, sleepGraph, awakeGraph, sotGraph, voltGraph, clockGraph, swapGraph;
    private long prevRxBytes = -1;
    private long prevTxBytes = -1;
    private long prevNetTimestamp = -1;
    private volatile int lastPingMs = -1;
    private RealtimePieGraphView monitorSleepPie, monitorBatteryPie, monitorCpuPie, monitorRamPie, monitorStoragePie, monitorSwapPie;
    private TextView monitorUptimeBadge, monitorSotText, monitorSleepRatioText, monitorScreenOffText;
    private TextView monitorBatteryLevelText, monitorBatteryStatusText, monitorBatteryHealthText, monitorVoltageText, monitorTempText;
    private TextView monitorCpuPctText, monitorRamPctText, monitorStoragePctText, monitorSwapPctText;
    private android.widget.LinearLayout monitorAppsContainer;
    private View monitorProCard;
    private TextView monitorProCountdown, monitorProScope, monitorProBadge;
    private TextView profileStatus, rootAccessButton;
    private View dashboardScroll;
    private ViewFlipper pageFlipper;
    private final TextView[] navItems = new TextView[5];
    private boolean rootReady, shizukuReady, rootDialogVisible;
    private int applyingProfile = -1;
    private final com.revror.kernelmanager.utils.kernel.compat.ProfileExecutionCoordinator.StateListener mProfileCoordinatorListener =
            (state, reqProfile, actProfile, message, opId) -> {
                if (isFinishing() || isDestroyed()) return;
                switch (state) {
                    case PREPARING:
                        profileStatus.setText("Preparing " + profileName(reqProfile) + "...");
                        profileStatus.setTextColor(Color.rgb(255, 184, 77));
                        break;
                    case APPLYING:
                        profileStatus.setText("Applying " + profileName(reqProfile) + "...");
                        profileStatus.setTextColor(Color.rgb(105, 220, 255));
                        break;
                    case VERIFYING:
                        profileStatus.setText("Verifying " + profileName(reqProfile) + "...");
                        profileStatus.setTextColor(Color.rgb(105, 220, 255));
                        break;
                    case APPLIED:
                        applyingProfile = reqProfile;
                        selectProfile(reqProfile);
                        applyProfileAppearance(reqProfile);
                        profileStatus.setText(message);
                        profileStatus.setTextColor(Color.rgb(105, 220, 255));
                        rootReady = true;
                        setProfileAccess(true);
                        break;
                    case FAILED:
                        profileStatus.setText(message);
                        profileStatus.setTextColor(Color.rgb(255, 82, 119));
                        if (message.toLowerCase(Locale.US).contains("root")) {
                            rootReady = false;
                        }
                        setProfileAccess(rootReady || shizukuReady);
                        break;
                    case IDLE:
                        break;
                }
            };
    private ValueAnimator activeProfileAnimator;
    private View currentlyAnimatedProfileView;
    private WebView adBanner;
    private final androidx.activity.result.ActivityResultLauncher<String> bannerPickerLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        File dest = new File(getFilesDir(), "custom_hero_banner.png");
                        try (java.io.InputStream in = getContentResolver().openInputStream(uri);
                             java.io.FileOutputStream out = new java.io.FileOutputStream(dest)) {
                            byte[] buf = new byte[8192];
                            int len;
                            while ((len = in.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }
                        }
                        getSharedPreferences("ror_theme", MODE_PRIVATE).edit()
                                .putString("custom_banner_path", dest.getAbsolutePath())
                                .putBoolean("follow_profile", false)
                                .apply();
                        applyProfileAppearance(GsmProfileManager.selectedProfile(this));
                        toast("Custom banner image set!");
                    } catch (Exception e) {
                        toast("Failed to load image: " + e.getMessage());
                    }
                }
            });
    private final ShizukuBridge.OnConnectionListener shizukuConnectionListener = new ShizukuBridge.OnConnectionListener() {
        @Override
        public void onShizukuConnected(boolean hasPermission) {
            runOnUiThread(() -> {
                shizukuReady = hasPermission;
                setProfileAccess(rootReady || shizukuReady);
                if (!rootReady && shizukuReady) {
                    profileStatus.setText("Shizuku active - Android-level profiles ready");
                    profileStatus.setTextColor(Color.rgb(105, 220, 255));
                    SilentPermissionHelper.grantAllSilent(DashboardActivity.this);
                }
            });
        }

        @Override
        public void onShizukuDisconnected() {
            runOnUiThread(() -> {
                shizukuReady = false;
                setProfileAccess(rootReady);
                if (!rootReady) {
                    profileStatus.setText("Shizuku service disconnected");
                    profileStatus.setTextColor(Color.rgb(255, 82, 119));
                }
            });
        }
    };

    private final Shizuku.OnRequestPermissionResultListener shizukuPermissionListener = (requestCode, grantResult) -> {
        if (requestCode != SHIZUKU_PERMISSION) return;
        boolean granted = (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) || ShizukuBridge.granted();
        shizukuReady = granted;
        runOnUiThread(() -> {
            setProfileAccess(rootReady || shizukuReady);
            profileStatus.setText(shizukuReady ? "Shizuku active - Android-level profiles ready" : "Shizuku permission denied");
            profileStatus.setTextColor(shizukuReady ? Color.rgb(105, 220, 255) : Color.rgb(255, 82, 119));
            toast(shizukuReady ? "Shizuku connected" : "Shizuku permission denied");
            if (shizukuReady) {
                SilentPermissionHelper.grantAllSilent(DashboardActivity.this);
            }
        });
    };

    private final Runnable metricsUpdater = new Runnable() {
        @Override public void run() {
            updateMetrics();
            checkAndEnforceLicenseExpiration();
            handler.postDelayed(this, 1000);
        }
    };

    @Override protected void onCreate(@Nullable Bundle state) {
        setTheme(R.style.RorTheme);
        super.onCreate(state);
        AppIntegrity.enforce(this);
        setContentView(R.layout.activity_dashboard);
        getWindow().setStatusBarColor(Color.rgb(5, 6, 10));
        getWindow().setNavigationBarColor(Color.rgb(5, 6, 10));
        bindViews();
        bindTopActions();
        bindProfiles();
        bindQuickTools();
        bindBottomNavigation();
        bindMorePage();
        setupAdBanner();
        handler.postDelayed(() -> AdBypassHelper.showFullScreenAdIfDue(this), 900);
        SilentPermissionHelper.grantAllSilent(this);
        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener);
        ShizukuBridge.addConnectionListener(shizukuConnectionListener);
        com.revror.kernelmanager.utils.kernel.compat.ProfileExecutionCoordinator.getInstance()
                .addListener(mProfileCoordinatorListener);
        dashboardScroll = findViewById(R.id.home_scroll);
        pageFlipper = findViewById(R.id.page_flipper);
        dashboardScroll.requestFocus();
        dashboardScroll.post(() -> dashboardScroll.scrollTo(0, 0));

        rootReady = GsmProfileManager.isRootEnabled(this);
        shizukuReady = ShizukuBridge.granted();
        applyingProfile = activeProfileForDisplayedEngine();
        selectProfile(applyingProfile);
        if (applyingProfile >= 0) applyProfileAppearance(applyingProfile);
        setProfileAccess(rootReady || shizukuReady);
        if (rootReady) {
            profileStatus.setText(applyingProfile >= 0
                    ? "Current profile - " + profileName(applyingProfile)
                    : "Choose a profile to apply it");
        } else {
            profileStatus.setText(shizukuReady ? "Shizuku active - Android-level profiles ready" : "Choose Root or Shizuku to enable profiles");
            profileStatus.setTextColor(Color.rgb(255, 184, 77));
            handler.postDelayed(this::showRootAccessDialog, 350);
        }
        handler.postDelayed(() -> checkForUpdates(false), 1800);

        // Ensure GameBubbleService monitors foreground for any registered games
        try {
            java.util.Set<String> registeredGames = com.revror.kernelmanager.utils.GameLauncherRegistry.getGamePackages(this);
            if (!registeredGames.isEmpty()) {
                Intent bubbleIntent = new Intent(this, com.revror.kernelmanager.services.GameBubbleService.class);
                if (Build.VERSION.SDK_INT >= 26) {
                    startForegroundService(bubbleIntent);
                } else {
                    startService(bubbleIntent);
                }
            }
        } catch (Throwable ignored) {}
        PowerButtonGestureService.startIfEnabled(this);
    }

    @Override protected void onResume() {
        super.onResume();
        AppIntegrity.enforce(this);
        boolean prevShizuku = shizukuReady;
        boolean prevRoot = rootReady;
        rootReady = GsmProfileManager.isRootEnabled(this);
        shizukuReady = ShizukuBridge.granted();
        if (prevShizuku != shizukuReady || prevRoot != rootReady) {
            setProfileAccess(rootReady || shizukuReady);
            if (!rootReady && shizukuReady) {
                profileStatus.setText("Shizuku active - Android-level profiles ready");
                profileStatus.setTextColor(Color.rgb(105, 220, 255));
            }
        }
        updateHeroDeviceInfo();
        View heroPillDot = findViewById(R.id.hero_pill_dot);
        View heroPillDotGlow = findViewById(R.id.hero_pill_dot_glow);
        if (heroPillDot != null) {
            setupPillDotBlinker(heroPillDot, heroPillDotGlow);
        }
        RorLicenseManager.checkAndSendExpirationReminder(this);
        checkAndEnforceLicenseExpiration();
        updateVipMorePageStatus();
        updateProfileEngineUi();
        refreshCustomProfilesUi();
        int targetPage = getIntent().getIntExtra("open_page", -1);
        if (targetPage >= 0) {
            showPage(targetPage);
            getIntent().removeExtra("open_page");
        }
        if (getIntent().hasExtra("open_vip_feature")) {
            String feat = getIntent().getStringExtra("open_vip_feature");
            RorLicenseManager.showVipRequiredDialog(this, feat, null);
            getIntent().removeExtra("open_vip_feature");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        int targetPage = intent.getIntExtra("open_page", -1);
        if (targetPage >= 0) {
            showPage(targetPage);
            intent.removeExtra("open_page");
        }
        if (intent.hasExtra("open_vip_feature")) {
            String feat = intent.getStringExtra("open_vip_feature");
            RorLicenseManager.showVipRequiredDialog(this, feat, null);
            intent.removeExtra("open_vip_feature");
        }
    }

    private void updateHeroDeviceInfo() {
        TextView heroDeviceText = findViewById(R.id.hero_device_text);
        if (heroDeviceText != null) {
            String dev = FpsUnlocker.getDisplayDeviceName(this);
            String androidVer = "Android " + Build.VERSION.RELEASE;
            String kernel = System.getProperty("os.version");
            if (kernel == null || kernel.trim().isEmpty()) {
                try {
                    kernel = com.revror.kernelmanager.utils.Device.getKernelVersion(false);
                } catch (Throwable ignored) {}
            }
            if (kernel == null) kernel = "";
            kernel = kernel.trim();
            if (!kernel.isEmpty()) {
                String cleanKernel = kernel.split("-")[0];
                if (!cleanKernel.toLowerCase().contains("linux") && !cleanKernel.toLowerCase().contains("kernel")) {
                    cleanKernel = "Linux " + cleanKernel;
                }
                heroDeviceText.setText(dev + " • " + androidVer + " • " + cleanKernel);
            } else {
                heroDeviceText.setText(dev + " • " + androidVer);
            }
        }
    }

    private void initGauge(RealtimePieGraphView gauge, int startColor, int endColor, float max) {
        if (gauge != null) {
            gauge.setColors(startColor, endColor);
            gauge.setStrokeWidthDp(3.8f);
            gauge.setMaxProgress(max);
        }
    }

    private void bindViews() {
        batteryRing = findViewById(R.id.battery_ring);
        remainingText = findViewById(R.id.remaining_text);
        cpuText = findViewById(R.id.cpu_text);
        ramText = findViewById(R.id.ram_text);
        storageText = findViewById(R.id.storage_text);
        tempText = findViewById(R.id.temp_text);
        pingText = findViewById(R.id.ping_text);
        gpuText = findViewById(R.id.gpu_text);
        fpsText = findViewById(R.id.fps_text);
        netDlText = findViewById(R.id.net_dl_text);
        netUlText = findViewById(R.id.net_ul_text);
        sleepText = findViewById(R.id.sleep_text);
        awakeText = findViewById(R.id.awake_text);
        sotText = findViewById(R.id.sot_text);
        voltText = findViewById(R.id.volt_text);
        clockText = findViewById(R.id.clock_text);
        swapText = findViewById(R.id.swap_text);

        profileStatus = findViewById(R.id.profile_status);
        rootAccessButton = findViewById(R.id.advanced_button);
        profileSwitches[0] = findViewById(R.id.switch_daily);
        profileSwitches[1] = findViewById(R.id.switch_balance);
        profileSwitches[2] = findViewById(R.id.switch_battery);
        profileSwitches[3] = findViewById(R.id.switch_cooldown);
        profileSwitches[4] = findViewById(R.id.switch_gaming);
        profileSwitches[5] = findViewById(R.id.switch_ultra_gaming);
        for (SwitchMaterial profileSwitch : profileSwitches) if (profileSwitch != null) profileSwitch.setVisibility(View.GONE);
        adBanner = findViewById(R.id.ad_banner);

        // Home 4x4 Grid Pie Gauges (all 16 identical 34dp size with 3.8f stroke)
        batteryPieGraph = findViewById(R.id.battery_pie_graph);
        cpuGraph = findViewById(R.id.cpu_graph);
        ramGraph = findViewById(R.id.ram_graph);
        storageGraph = findViewById(R.id.storage_graph);
        tempGraph = findViewById(R.id.temp_graph);
        pingGraph = findViewById(R.id.ping_graph);
        gpuGraph = findViewById(R.id.gpu_graph);
        fpsGraph = findViewById(R.id.fps_graph);
        netDlGraph = findViewById(R.id.net_dl_graph);
        netUlGraph = findViewById(R.id.net_ul_graph);
        sleepGraph = findViewById(R.id.sleep_graph);
        awakeGraph = findViewById(R.id.awake_graph);
        sotGraph = findViewById(R.id.sot_graph);
        voltGraph = findViewById(R.id.volt_graph);
        clockGraph = findViewById(R.id.clock_graph);
        swapGraph = findViewById(R.id.swap_graph);

        initGauge(batteryPieGraph, 0xFF00E676, 0xFF00B4D8, 100);
        initGauge(cpuGraph, 0xFF38BDF8, 0xFF00F5D4, 100);
        initGauge(ramGraph, 0xFFA855F7, 0xFFD946EF, 100);
        initGauge(storageGraph, 0xFFF59E0B, 0xFFFBBF24, 100);
        initGauge(tempGraph, 0xFFFF5776, 0xFFFF859B, 85);
        initGauge(pingGraph, 0xFF00E5FF, 0xFF38BDF8, 200);
        initGauge(gpuGraph, 0xFFEC4899, 0xFFF43F5E, 100);
        initGauge(fpsGraph, 0xFF10B981, 0xFF34D399, 120);
        initGauge(netDlGraph, 0xFF38BDF8, 0xFF00E5FF, 1000);
        initGauge(netUlGraph, 0xFF818CF8, 0xFF6366F1, 1000);
        initGauge(sleepGraph, 0xFF6366F1, 0xFF8B5CF6, 100);
        initGauge(awakeGraph, 0xFFF43F5E, 0xFFFB7185, 100);
        initGauge(sotGraph, 0xFF34D399, 0xFF10B981, 720);
        initGauge(voltGraph, 0xFFFBBF24, 0xFFF59E0B, 5000);
        initGauge(clockGraph, 0xFF06B6D4, 0xFF38BDF8, 3500);
        initGauge(swapGraph, 0xFFC084FC, 0xFFA855F7, 100);

        // Overview See more / Show less toggle (2 lines default, expands to full 4x4 grid)
        View overviewExtraRows = findViewById(R.id.overview_extra_rows);
        View overviewToggle = findViewById(R.id.overview_toggle_button);
        TextView overviewToggleText = findViewById(R.id.overview_toggle_text);
        ImageView overviewToggleIcon = findViewById(R.id.overview_toggle_icon);
        if (overviewToggle != null && overviewExtraRows != null) {
            overviewToggle.setOnClickListener(v -> {
                boolean expanded = overviewExtraRows.getVisibility() == View.VISIBLE;
                if (expanded) {
                    overviewExtraRows.setVisibility(View.GONE);
                    if (overviewToggleText != null) overviewToggleText.setText("See more");
                    if (overviewToggleIcon != null) overviewToggleIcon.setImageResource(R.drawable.ic_arrow_down);
                } else {
                    overviewExtraRows.setVisibility(View.VISIBLE);
                    if (overviewToggleText != null) overviewToggleText.setText("Show less");
                    if (overviewToggleIcon != null) overviewToggleIcon.setImageResource(R.drawable.ic_arrow_up);
                }
            });
        }

        // Monitor Tab Views & Pie Gauges
        monitorSleepPie = findViewById(R.id.monitor_sleep_pie);
        if (monitorSleepPie != null) {
            monitorSleepPie.setColors(0xFF7928CA, 0xFF00F5D4);
            monitorSleepPie.setStrokeWidthDp(7.5f);
            monitorSleepPie.setMaxProgress(100);
        }
        monitorBatteryPie = findViewById(R.id.monitor_battery_pie);
        if (monitorBatteryPie != null) {
            monitorBatteryPie.setColors(0xFF00E676, 0xFF00B4D8);
            monitorBatteryPie.setStrokeWidthDp(8f);
            monitorBatteryPie.setMaxProgress(100);
        }
        monitorCpuPie = findViewById(R.id.monitor_cpu_pie);
        if (monitorCpuPie != null) {
            monitorCpuPie.setColors(0xFF38BDF8, 0xFF00F5D4);
            monitorCpuPie.setStrokeWidthDp(6.5f);
            monitorCpuPie.setMaxProgress(100);
        }
        monitorRamPie = findViewById(R.id.monitor_ram_pie);
        if (monitorRamPie != null) {
            monitorRamPie.setColors(0xFF34D399, 0xFF10B981);
            monitorRamPie.setStrokeWidthDp(6.5f);
            monitorRamPie.setMaxProgress(100);
        }
        monitorStoragePie = findViewById(R.id.monitor_storage_pie);
        if (monitorStoragePie != null) {
            monitorStoragePie.setColors(0xFFC084FC, 0xFF818CF8);
            monitorStoragePie.setStrokeWidthDp(6.5f);
            monitorStoragePie.setMaxProgress(100);
        }
        monitorSwapPie = findViewById(R.id.monitor_swap_pie);
        if (monitorSwapPie != null) {
            monitorSwapPie.setColors(0xFFFBBF24, 0xFFFF6381);
            monitorSwapPie.setStrokeWidthDp(6.5f);
            monitorSwapPie.setMaxProgress(100);
        }

        monitorUptimeBadge = findViewById(R.id.monitor_uptime_badge);
        monitorSotText = findViewById(R.id.monitor_sot_text);
        monitorSleepRatioText = findViewById(R.id.monitor_sleep_ratio_text);
        monitorScreenOffText = findViewById(R.id.monitor_screen_off_text);
        monitorBatteryLevelText = findViewById(R.id.monitor_battery_level_text);
        monitorBatteryStatusText = findViewById(R.id.monitor_battery_status_text);
        monitorBatteryHealthText = findViewById(R.id.monitor_battery_health_text);
        monitorVoltageText = findViewById(R.id.monitor_voltage_text);
        monitorTempText = findViewById(R.id.monitor_temp_text);
        monitorCpuPctText = findViewById(R.id.monitor_cpu_pct_text);
        monitorRamPctText = findViewById(R.id.monitor_ram_pct_text);
        monitorStoragePctText = findViewById(R.id.monitor_storage_pct_text);
        monitorSwapPctText = findViewById(R.id.monitor_swap_pct_text);
        monitorAppsContainer = findViewById(R.id.monitor_apps_container);
        monitorProCard = findViewById(R.id.monitor_pro_card);
        monitorProCountdown = findViewById(R.id.monitor_pro_countdown);
        monitorProScope = findViewById(R.id.monitor_pro_scope);
        monitorProBadge = findViewById(R.id.monitor_pro_badge);
    }

    private void bindTopActions() {
        findViewById(R.id.menu_button).setOnClickListener(v -> showControlCenter());
        findViewById(R.id.settings_button).setOnClickListener(v -> showControlCenter());
        View telegramBtn = findViewById(R.id.telegram_button);
        if (telegramBtn != null) telegramBtn.setOnClickListener(v -> openUrl("https://t.me/godTspeed"));
        View devTopBtn = findViewById(R.id.dev_top_button);
        if (devTopBtn != null) devTopBtn.setOnClickListener(v -> openUrl("https://github.com/revws"));
        View advBtn = findViewById(R.id.advanced_button);
        if (advBtn != null) advBtn.setOnClickListener(v -> {
            if (rootReady) showRootStatus(); else if (shizukuReady) showShizukuStatus(); else showRootAccessDialog();
        });
        findViewById(R.id.donate_button).setOnClickListener(v -> showDonationDialog());
        findViewById(R.id.info_button).setOnClickListener(v -> showAbout());
    }

    private void bindProfiles() {
        findViewById(R.id.profile_daily).setOnClickListener(v -> applyProfile(0));
        findViewById(R.id.profile_balance).setOnClickListener(v -> applyProfile(1));
        findViewById(R.id.profile_battery).setOnClickListener(v -> applyProfile(2));
        findViewById(R.id.profile_cooldown).setOnClickListener(v -> applyProfile(3));
        findViewById(R.id.profile_gaming).setOnClickListener(v -> applyProfile(4));
        View pUltra = findViewById(R.id.profile_ultra_gaming);
        if (pUltra != null) pUltra.setOnClickListener(v -> applyProfile(5));
        findViewById(R.id.home_profile_daily).setOnClickListener(v -> applyProfile(0));
        findViewById(R.id.home_profile_balance).setOnClickListener(v -> applyProfile(1));
        findViewById(R.id.home_profile_battery).setOnClickListener(v -> applyProfile(2));
        findViewById(R.id.home_profile_cooldown).setOnClickListener(v -> applyProfile(3));
        findViewById(R.id.home_profile_gaming).setOnClickListener(v -> applyProfile(4));
        View homeUltra = findViewById(R.id.home_profile_ultra_gaming);
        if (homeUltra != null) homeUltra.setOnClickListener(v -> applyProfile(5));

        View regularEngine = findViewById(R.id.engine_regular);
        View phoenixEngine = findViewById(R.id.engine_phoenix);
        if (regularEngine != null) regularEngine.setOnClickListener(v -> switchProfileEngine(false));
        if (phoenixEngine != null) phoenixEngine.setOnClickListener(v -> switchProfileEngine(true));
        View homeRegularEngine = findViewById(R.id.home_engine_regular);
        View homePhoenixEngine = findViewById(R.id.home_engine_phoenix);
        if (homeRegularEngine != null) homeRegularEngine.setOnClickListener(v -> switchProfileEngine(false));
        if (homePhoenixEngine != null) homePhoenixEngine.setOnClickListener(v -> switchProfileEngine(true));
        updateProfileEngineUi();
        refreshCustomProfilesUi();

        View homeGameLauncher = findViewById(R.id.home_game_launcher);
        if (homeGameLauncher != null) homeGameLauncher.setOnClickListener(v -> startActivity(new Intent(this, GameLauncherActivity.class)));
        View homeKernel = findViewById(R.id.home_profile_kernel);
        if (homeKernel != null) homeKernel.setOnClickListener(v -> {
            if (!RorLicenseManager.isPremiumActive(this)) {
                RorLicenseManager.showVipRequiredDialog(this, "Advanced Kernel Tuning", null);
                return;
            }
            if (!rootReady) {
                showRootLockedDialog("Kernel Tuning");
                return;
            }
            startActivity(new Intent(this, KernelTuningActivity.class));
        });
        View homeMore = findViewById(R.id.home_profile_more);
        if (homeMore != null) homeMore.setOnClickListener(v -> showPage(2));
        View homeHz = findViewById(R.id.home_profile_hz);
        if (homeHz != null) homeHz.setOnClickListener(v -> showRefreshRate());
        View homeAi = findViewById(R.id.home_profile_ai);
        if (homeAi != null) homeAi.setOnClickListener(v -> {
            if (!RorLicenseManager.isPremiumActive(this)) {
                RorLicenseManager.showVipRequiredDialog(this, "Phoenix AI Optimizer", null);
                return;
            }
            showRorAiDialog();
        });
        View homeClean = findViewById(R.id.home_profile_clean);
        if (homeClean != null) homeClean.setOnClickListener(v -> showCleaner());

        // See more / Show less toggle for Extra Performance Profiles (Row 2 & 3)
        View profilesToggle = findViewById(R.id.profiles_toggle_button);
        View profilesExtra = findViewById(R.id.profiles_extra_rows);
        TextView profilesToggleText = findViewById(R.id.profiles_toggle_text);
        ImageView profilesToggleIcon = findViewById(R.id.profiles_toggle_icon);
        if (profilesToggle != null && profilesExtra != null) {
            profilesToggle.setOnClickListener(v -> {
                boolean isExpanded = profilesExtra.getVisibility() == View.VISIBLE;
                profilesExtra.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
                if (profilesToggleText != null) {
                    profilesToggleText.setText(isExpanded ? "See more" : "Show less");
                }
                if (profilesToggleIcon != null) {
                    profilesToggleIcon.setImageResource(isExpanded ? R.drawable.ic_arrow_down : R.drawable.ic_arrow_up);
                }
            });
        }

        View btnAddCustom = findViewById(R.id.btn_add_custom_profile);
        if (btnAddCustom != null) {
            btnAddCustom.setOnClickListener(v -> {
                if (!RorLicenseManager.isPremiumActive(this)) {
                    RorLicenseManager.showVipRequiredDialog(this, "Custom Profile Studio", () -> {
                        com.revror.kernelmanager.views.dialog.CustomProfileDialog.showEditor(this, null, this::refreshCustomProfilesUi);
                    });
                    return;
                }
                com.revror.kernelmanager.views.dialog.CustomProfileDialog.showEditor(this, null, this::refreshCustomProfilesUi);
            });
        }
        refreshCustomProfilesUi();
    }

    private void refreshCustomProfilesUi() {
        LinearLayout container = findViewById(R.id.custom_profiles_container);
        if (container == null) return;
        container.removeAllViews();

        boolean isVip = RorLicenseManager.isPremiumActive(this);
        List<com.revror.kernelmanager.utils.profile.CustomProfileManager.CustomProfile> list =
                com.revror.kernelmanager.utils.profile.CustomProfileManager.getAllProfiles(this);
        String activeCustomId = com.revror.kernelmanager.utils.profile.CustomProfileManager.getActiveCustomProfileId(this);

        if (list.isEmpty()) {
            TextView emptyNotice = new TextView(this);
            emptyNotice.setText("No custom profiles crafted yet • Tap '+ CREATE NEW' to create one");
            emptyNotice.setTextColor(0xFF7A85A8);
            emptyNotice.setTextSize(11f);
            emptyNotice.setPadding(16, 24, 16, 24);
            emptyNotice.setGravity(android.view.Gravity.CENTER);
            container.addView(emptyNotice);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (com.revror.kernelmanager.utils.profile.CustomProfileManager.CustomProfile p : list) {
            View card = inflater.inflate(R.layout.item_custom_profile_card, container, false);
            ImageView icon = card.findViewById(R.id.custom_card_icon);
            TextView title = card.findViewById(R.id.custom_card_title);
            TextView subtitle = card.findViewById(R.id.custom_card_subtitle);
            TextView badge = card.findViewById(R.id.custom_card_badge);
            ImageView lockIcon = card.findViewById(R.id.custom_card_lock_icon);
            ImageView options = card.findViewById(R.id.custom_card_options);

            int iconRes = getResources().getIdentifier(p.iconResName, "drawable", getPackageName());
            if (iconRes == 0) iconRes = R.drawable.ic_cpu;
            icon.setImageResource(iconRes);
            try {
                icon.setColorFilter(Color.parseColor(p.accentColor));
            } catch (Exception ignored) {
                icon.setColorFilter(0xFF00E5FF);
            }

            title.setText(p.name);
            boolean isActive = p.id.equals(activeCustomId);
            badge.setVisibility(isActive ? View.VISIBLE : View.GONE);

            StringBuilder sub = new StringBuilder();
            sub.append(p.policySettings.size()).append(" Policies");
            if (p.swappiness >= 0) sub.append(" • Swap ").append(p.swappiness).append("%");
            if (p.dropCaches) sub.append(" • Drop Caches");
            if (p.compactMemory) sub.append(" • Compact RAM");
            subtitle.setText(sub.toString());

            if (!isVip) {
                lockIcon.setVisibility(View.VISIBLE);
                card.setOnClickListener(v -> RorLicenseManager.showVipRequiredDialog(this, "Custom Profile", () -> {
                    refreshCustomProfilesUi();
                }));
                options.setOnClickListener(v -> RorLicenseManager.showVipRequiredDialog(this, "Custom Profile Options", () -> {
                    refreshCustomProfilesUi();
                }));
            } else {
                lockIcon.setVisibility(View.GONE);
                card.setOnClickListener(v -> {
                    com.revror.kernelmanager.views.dialog.CustomProfileDialog.applyProfile(this, p, () -> {
                        refreshCustomProfilesUi();
                        applyingProfile = -1;
                        selectProfile(-1);
                        if (profileStatus != null) {
                            profileStatus.setText("Current profile - " + p.name + " (Custom VIP)");
                            profileStatus.setTextColor(0xFF00F5D4);
                        }
                    });
                });
                options.setOnClickListener(v -> {
                    com.revror.kernelmanager.views.dialog.CustomProfileDialog.showProfileOptions(this, p, this::refreshCustomProfilesUi);
                });
            }

            container.addView(card);
        }
    }

    private void switchProfileEngine(boolean phoenix) {
        GsmProfileManager.setEngine(this, phoenix);
        updateProfileEngineUi();
        refreshCustomProfilesUi();
        applyingProfile = activeProfileForDisplayedEngine();
        selectProfile(applyingProfile);
        if (applyingProfile >= 0) {
            profileStatus.setText("Current profile - " + profileName(applyingProfile));
        } else {
            String running = activeProfileEngine();
            profileStatus.setText("Viewing " + (phoenix ? "Phoenix +" : "Regular")
                    + " profiles - " + (running.isEmpty() ? "choose a profile to activate it"
                    : running + " profile stays active until you choose a new profile"));
        }
        profileStatus.setTextColor(Color.rgb(105, 220, 255));
    }

    private String activeProfileEngine() {
        String active = getSharedPreferences("ror_profiles", MODE_PRIVATE).getString("active_engine", "");
        return "phoenix".equals(active) ? "Phoenix" : "regular".equals(active) ? "Regular" : "";
    }

    private int activeProfileForDisplayedEngine() {
        String active = activeProfileEngine();
        if ((GsmProfileManager.isPhoenixSelected(this) && !"Phoenix".equals(active))
                || (!GsmProfileManager.isPhoenixSelected(this) && !"Regular".equals(active))) return -1;
        return getSharedPreferences("ror_profiles", MODE_PRIVATE).getInt("active_profile", -1);
    }

    private void updateProfileEngineUi() {
        boolean phoenix = GsmProfileManager.isPhoenixSelected(this);
        TextView regular = findViewById(R.id.engine_regular);
        TextView phx = findViewById(R.id.engine_phoenix);
        TextView homeRegular = findViewById(R.id.home_engine_regular);
        TextView homePhx = findViewById(R.id.home_engine_phoenix);
        View homeSwitch = findViewById(R.id.home_engine_switch_container);
        TextView description = findViewById(R.id.profile_engine_description);
        if (regular != null) {
            regular.setBackgroundResource(phoenix ? R.drawable.ror_btn_outline : R.drawable.ror_btn_primary);
            regular.setTextColor(phoenix ? 0xFFA9ABC2 : Color.WHITE);
        }
        if (phx != null) {
            phx.setBackgroundResource(phoenix ? R.drawable.ror_btn_primary : R.drawable.ror_btn_outline);
            phx.setTextColor(phoenix ? Color.WHITE : 0xFFFF5252);
        }
        if (homeSwitch != null) homeSwitch.setVisibility(RorLicenseManager.isPremiumActive(this) ? View.VISIBLE : View.GONE);
        if (homeRegular != null) {
            homeRegular.setBackgroundResource(phoenix ? R.drawable.ror_btn_outline : R.drawable.ror_btn_primary);
            homeRegular.setTextColor(phoenix ? 0xFFA9ABC2 : Color.WHITE);
        }
        if (homePhx != null) {
            homePhx.setBackgroundResource(phoenix ? R.drawable.ror_btn_primary : R.drawable.ror_btn_outline);
            homePhx.setTextColor(phoenix ? Color.WHITE : 0xFFFF5252);
        }
        if (description != null) description.setText(phoenix
                ? "Phoenix engine active • 6 specialized kernel and hardware profiles."
                : "Regular engine active • 4 core kernel and hardware profiles.");

        setProfileCopy(R.id.profile_title_daily, R.id.profile_subtitle_daily,
                phoenix ? "Battery+" : "DAILY USE AND BATTERY",
                phoenix ? "Optimized background execution with dynamic low-power governor tuning."
                        : "Recommended • responsive daily use with balanced power");
        setProfileCopy(R.id.profile_title_battery, R.id.profile_subtitle_battery,
                phoenix ? "Thermal Profile+" : "ULTRA BATTERY",
                phoenix ? "Active thermal management for sustained gameplay without device heating."
                        : "Lower power use for maximum battery life");
        setProfileCopy(R.id.profile_title_balance, R.id.profile_subtitle_balance,
                phoenix ? "Balance+" : "BALANCE SMOOTH",
                phoenix ? "Adaptive performance balancing day-to-day fluidity with efficient core scaling."
                        : "Smooth multitasking with balanced responsive performance");
        setProfileCopy(R.id.profile_title_cooldown, R.id.profile_subtitle_cooldown,
                phoenix ? "CoolDown+" : "COOLDOWN",
                phoenix ? "Aggressive memory compaction, cache drop, and thermal throttle recovery."
                        : "Lowest brightness • drops caches • force-stops background apps");
        setProfileCopy(R.id.profile_title_gaming, R.id.profile_subtitle_gaming,
                phoenix ? "Smooth Gaming+" : "GAMING",
                phoenix ? "High frame-rate stability and touch response latency optimization."
                        : "Performance governor • dynamic clock boosting for gaming");
        setProfileCopy(R.id.profile_title_ultra_gaming, R.id.profile_subtitle_ultra_gaming,
                phoenix ? "Ultra Gaming+" : "ULTRA GAMING",
                phoenix ? "Maximum CPU and GPU frequency override with fixed performance governor."
                        : "Performance governor • Peak CPU and GPU overdrive");

        View cooldown = findViewById(R.id.profile_cooldown);
        View gaming = findViewById(R.id.profile_gaming);
        View ultra = findViewById(R.id.profile_ultra_gaming);
        if (cooldown != null) cooldown.setVisibility(phoenix ? View.VISIBLE : View.GONE);
        if (gaming != null) gaming.setVisibility(View.VISIBLE);
        if (ultra != null) ultra.setVisibility(View.VISIBLE);

        TextView h0 = findViewById(R.id.home_profile_daily);
        TextView h1 = findViewById(R.id.home_profile_balance);
        TextView h2 = findViewById(R.id.home_profile_battery);
        TextView h3 = findViewById(R.id.home_profile_gaming);
        TextView hUltra = findViewById(R.id.home_profile_ultra_gaming);
        if (h0 != null) h0.setText(phoenix ? "Battery+\nEndurance" : "Daily Use\nBalanced");
        if (h1 != null) h1.setText(phoenix ? "Balance+\nFluid" : "Balance\nSmooth");
        if (h2 != null) h2.setText(phoenix ? "Thermal+\nCooling" : "Battery\nEndurance");
        if (h3 != null) h3.setText(phoenix ? "Smooth Gaming+\nStable FPS" : "Gaming\nMax power");
        if (hUltra != null) {
            hUltra.setText(phoenix ? "Ultra Gaming+\nPeak overdrive" : "Ultra Gaming\nPeak power");
            hUltra.setVisibility(View.VISIBLE);
        }
        View homeCooldown = findViewById(R.id.home_profile_cooldown);
        if (homeCooldown instanceof TextView) {
            ((TextView) homeCooldown).setText(phoenix ? "CoolDown+\nThermal drop" : "CoolDown\nQuick reset");
        }
        if (homeCooldown != null) homeCooldown.setVisibility(View.VISIBLE);
    }

    private void setProfileCopy(int titleId, int subtitleId, String title, String subtitle) {
        TextView titleView = findViewById(titleId);
        TextView subtitleView = findViewById(subtitleId);
        if (titleView != null) titleView.setText(title);
        if (subtitleView != null) subtitleView.setText(subtitle);
    }

    private void bindQuickTools() {
        View toolUltra = findViewById(R.id.tool_ultra);
        if (toolUltra != null) toolUltra.setOnClickListener(v -> applyProfile(5));
        findViewById(R.id.tool_apps).setOnClickListener(v -> showAppManager());
        findViewById(R.id.tool_clean).setOnClickListener(v -> showCleaner());
        findViewById(R.id.tool_kernel).setOnClickListener(v -> {
            if (!RorLicenseManager.isPremiumActive(this)) {
                RorLicenseManager.showVipRequiredDialog(this, "Kernel Tuning", null);
                return;
            }
            if (!rootReady) {
                showRootLockedDialog("Kernel Tuning");
                return;
            }
            startActivity(new Intent(this, KernelTuningActivity.class));
        });
        findViewById(R.id.tool_boost).setOnClickListener(v -> startActivity(new Intent(this, GameLauncherActivity.class)));
        findViewById(R.id.tool_network).setOnClickListener(v -> showNetworkBoost());
        findViewById(R.id.tool_thermal).setOnClickListener(v -> showThermalStatus());
        findViewById(R.id.tool_overlay).setOnClickListener(v -> showOverlayControls());
        findViewById(R.id.tool_refresh).setOnClickListener(v -> showRefreshRate());

        findViewById(R.id.tool_compact).setOnClickListener(v -> {
            if (!rootReady) {
                showRootLockedDialog("Memory Compaction");
                return;
            }
            runRootTool("Memory compact", "[ -w /proc/sys/vm/compact_memory ] && echo 1 > /proc/sys/vm/compact_memory");
        });
        findViewById(R.id.tool_drop_caches).setOnClickListener(v -> {
            if (!rootReady) {
                showRootLockedDialog("Drop Filesystem Cache");
                return;
            }
            runRootTool("Drop filesystem cache", "sync; echo 3 > /proc/sys/vm/drop_caches");
        });
        findViewById(R.id.tool_doze).setOnClickListener(v -> com.revror.kernelmanager.views.dialog.ForceDozeDialog.show(this));
        findViewById(R.id.tool_data_saver).setOnClickListener(v -> runPrivilegedTool("Data Saver", "cmd netpolicy set restrict-background true", false));
        findViewById(R.id.tool_save_profile).setOnClickListener(v -> saveCurrentCustomProfile());
        findViewById(R.id.tool_custom_profile).setOnClickListener(v -> showCustomProfiles());
        findViewById(R.id.tool_schedule).setOnClickListener(v -> showBatteryScheduler());
        findViewById(R.id.tool_cpu_details).setOnClickListener(v -> showCpuStatus());
        View fpsTool = findViewById(R.id.tool_fps_unlocker);
        if (fpsTool != null) fpsTool.setOnClickListener(v -> FpsUnlocker.showDialog(this));
        View dnsTool = findViewById(R.id.tool_dns_changer);
        if (dnsTool != null) dnsTool.setOnClickListener(v -> DnsChanger.showDialog(this));
        View bootTool = findViewById(R.id.tool_boot_settings);
        if (bootTool != null) bootTool.setOnClickListener(v -> BootTweakManager.showSettingsDialog(this));
        View gyroTool = findViewById(R.id.tool_gyro_fix);
        if (gyroTool != null) gyroTool.setOnClickListener(v -> {
            if (!RorLicenseManager.isPremiumActive(this)) {
                RorLicenseManager.showVipRequiredDialog(this, "Gyro Delay Fix (Phoenix VIP)", () -> GyroDelayDialog.show(this));
                return;
            }
            GyroDelayDialog.show(this);
        });
        View aimTool = findViewById(R.id.tool_aim_boost);
        if (aimTool != null) aimTool.setOnClickListener(v -> {
            if (!RorLicenseManager.isPremiumActive(this)) {
                RorLicenseManager.showVipRequiredDialog(this, "Aim Boost (Phoenix VIP)", () -> AimBoostDialog.show(this));
                return;
            }
            AimBoostDialog.show(this);
        });

        View rebootTool = findViewById(R.id.tool_reboot_manager);
        if (rebootTool != null) rebootTool.setOnClickListener(v -> BootTweakManager.showRebootManagerDialog(this));
        View chargingTool = findViewById(R.id.tool_charging_cooldown);
        if (chargingTool != null) chargingTool.setOnClickListener(v -> com.revror.kernelmanager.views.dialog.ChargingCooldownDialog.show(this));
        View monitorBatteryCard = findViewById(R.id.monitor_battery_card);
        if (monitorBatteryCard != null) monitorBatteryCard.setOnClickListener(v -> com.revror.kernelmanager.views.dialog.ChargingCooldownDialog.show(this));
        View tileBattery = findViewById(R.id.tile_battery);
        if (tileBattery != null) tileBattery.setOnClickListener(v -> com.revror.kernelmanager.views.dialog.ChargingCooldownDialog.show(this));
        View rorAiBanner = findViewById(R.id.banner_ror_ai);
        if (rorAiBanner != null) rorAiBanner.setOnClickListener(v -> {
            if (!RorLicenseManager.isPremiumActive(this)) {
                RorLicenseManager.showVipRequiredDialog(this, "Phoenix AI Optimizer", this::showRorAiDialog);
                return;
            }
            showRorAiDialog();
        });

        View homeToolUltra = findViewById(R.id.home_tool_ultra);
        if (homeToolUltra != null) homeToolUltra.setOnClickListener(v -> applyProfile(5));
        findViewById(R.id.home_tool_apps).setOnClickListener(v -> showAppManager());
        findViewById(R.id.home_tool_clean).setOnClickListener(v -> showCleaner());
        findViewById(R.id.home_tool_overlay).setOnClickListener(v -> showOverlayControls());
        findViewById(R.id.home_tool_all).setOnClickListener(v -> showPage(3));
    }

    private void bindBottomNavigation() {
        navItems[0] = findViewById(R.id.home_nav);
        navItems[1] = findViewById(R.id.monitor_nav);
        navItems[2] = findViewById(R.id.profiles_nav);
        navItems[3] = findViewById(R.id.tools_nav);
        navItems[4] = findViewById(R.id.more_nav);
        for (int i = 0; i < navItems.length; i++) {
            final int page = i;
            if (navItems[i] != null) navItems[i].setOnClickListener(v -> showPage(page));
        }
        showPage(0);
    }

    private void bindMorePage() {
        findViewById(R.id.more_root).setOnClickListener(v -> { if (rootReady) showRootStatus(); else if (shizukuReady) showShizukuStatus(); else showRootAccessDialog(); });
        findViewById(R.id.more_android_settings).setOnClickListener(v -> showButtonConfigDialog());
        findViewById(R.id.more_feedback).setOnClickListener(v -> showFeedbackSettings());
        findViewById(R.id.more_appearance).setOnClickListener(v -> showAppearance());
        View moreMini = findViewById(R.id.more_mini_favorites);
        if (moreMini != null) moreMini.setOnClickListener(v -> showMiniWindowFavoritesDialog());
        View moreBoot = findViewById(R.id.more_boot);
        if (moreBoot != null) moreBoot.setOnClickListener(v -> BootTweakManager.showSettingsDialog(this));
        View moreDevProfile = findViewById(R.id.more_dev_profile);
        if (moreDevProfile != null) moreDevProfile.setOnClickListener(v -> openUrl("https://ko-fi.com/revWS"));
        View moreTelegram = findViewById(R.id.more_telegram);
        if (moreTelegram != null) moreTelegram.setOnClickListener(v -> openUrl("https://t.me/godTspeed"));
        View moreDevDonate = findViewById(R.id.more_dev_donate);
        if (moreDevDonate != null) moreDevDonate.setOnClickListener(v -> showDonationDialog());
        findViewById(R.id.more_update).setOnClickListener(v -> checkForUpdates(true));
        View moreBugReport = findViewById(R.id.more_bug_report);
        if (moreBugReport != null) moreBugReport.setOnClickListener(v -> showBugReportDialog());
        findViewById(R.id.more_donate).setOnClickListener(v -> openUrl("https://ko-fi.com/revWS"));
        findViewById(R.id.more_about).setOnClickListener(v -> showAbout());
        bindVipSection();
    }

    private void bindVipSection() {
        TextView deviceCodeText = findViewById(R.id.vip_device_code);
        TextView statusBadge = findViewById(R.id.vip_status_badge);
        TextView expiryText = findViewById(R.id.vip_expiry_text);
        View btnCopy = findViewById(R.id.btn_copy_device_code);
        View btnTelegram = findViewById(R.id.btn_telegram_device_code);
        EditText editLicense = findViewById(R.id.vip_license_input);
        View btnActivate = findViewById(R.id.btn_activate_license);

        if (btnCopy != null) {
            btnCopy.setOnClickListener(v -> RorLicenseManager.copyDeviceCode(this));
        }
        if (btnTelegram != null) {
            btnTelegram.setOnClickListener(v -> RorLicenseManager.openTelegramChat(this));
        }

        updateVipMorePageStatus();

        if (btnActivate != null && editLicense != null) {
            btnActivate.setOnClickListener(v -> {
                String token = editLicense.getText().toString().trim();
                if (token.isEmpty()) {
                    try {
                        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                            CharSequence clip = cm.getPrimaryClip().getItemAt(0).getText();
                            if (clip != null) {
                                String clipStr = clip.toString().trim();
                                if (clipStr.startsWith("ROR-") || clipStr.startsWith("ROR.") || clipStr.startsWith("GSM.")) {
                                    token = clipStr;
                                    editLicense.setText(token);
                                }
                            }
                        }
                    } catch (Throwable ignored) {}
                }
                if (token.isEmpty()) {
                    token = RorLicenseManager.readLicenseFromStorage(this);
                    if (token != null && !token.trim().isEmpty()) {
                        editLicense.setText(token.trim());
                    }
                }
                if (token == null || token.isEmpty()) {
                    toast("Enter valid license");
                    return;
                }
                RorLicenseManager.VerificationResult res = RorLicenseManager.verifyToken(this, token);
                if (res.success) {
                    RorLicenseManager.applyLicense(this, token);
                    toast("License activated!");
                    editLicense.setText("");
                    updateVipMorePageStatus();
                } else {
                    toast("Invalid license");
                }
            });
        }

        View btnProBenefits = findViewById(R.id.btn_pro_benefits);
        if (btnProBenefits != null) {
            btnProBenefits.setOnClickListener(v -> {
                startActivity(new Intent(this, RorProBenefitsActivity.class));
            });
        }

        View btnRemoveLicense = findViewById(R.id.btn_remove_license);
        if (btnRemoveLicense != null) {
            btnRemoveLicense.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Remove Phoenix License?")
                        .setMessage("Are you sure you want to remove your active Phoenix license? Premium features will be locked immediately.")
                        .setPositiveButton("Remove", (dialog, which) -> {
                            RorLicenseManager.removeLicense(this);
                            checkAndEnforceLicenseExpiration();
                            updateVipMorePageStatus();
                            toast("License removed");
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
    }

    private boolean lastKnownVipActive = false;

    private void checkAndEnforceLicenseExpiration() {
        boolean active = RorLicenseManager.isPremiumActive(this);
        if (lastKnownVipActive && !active) {
            // License expired right now!
            lastKnownVipActive = false;
            // 1. Immediately show ads
            if (adBanner != null) {
                adBanner.setVisibility(View.VISIBLE);
                AdBypassHelper.setupAdWebView(adBanner, this);
            }
            // 2. Immediately lock and revert from Ultra Gaming if active
            int currentProfile = GsmProfileManager.selectedProfile(this);
            if (currentProfile == 5) {
                applyProfile(0);
                toast("Phoenix license expired. Reverting to Balance.");
            }
            // 3. Update More page status badge & text
            updateVipMorePageStatus();
            selectProfile(currentProfile == 5 ? 0 : currentProfile);
        } else if (!lastKnownVipActive && active) {
            lastKnownVipActive = true;
            if (adBanner != null) {
                adBanner.stopLoading();
                adBanner.setVisibility(View.GONE);
            }
            updateVipMorePageStatus();
        } else if (active) {
            // Live 1-second countdown update for both More tab and Monitor tab
            updateVipCountdownLive();
        }
    }

    private void updateVipCountdownLive() {
        TextView expiryText = findViewById(R.id.vip_expiry_text);
        if (expiryText != null) {
            expiryText.setText(RorLicenseManager.getLicenseStatusDescription(this));
        }
        if (monitorProCard != null && monitorProCard.getVisibility() == View.VISIBLE) {
            if (monitorProCountdown != null) {
                String expires = RorLicenseManager.getExpiresTimestamp(this);
                monitorProCountdown.setText(RorLicenseManager.formatRemainingTime(expires));
            }
        }
    }

    private void updateVipMorePageStatus() {
        TextView statusBadge = findViewById(R.id.vip_status_badge);
        TextView expiryText = findViewById(R.id.vip_expiry_text);
        TextView deviceCodeText = findViewById(R.id.vip_device_code);
        ImageView vipHeaderIcon = findViewById(R.id.vip_header_icon);
        View btnRemoveLicense = findViewById(R.id.btn_remove_license);

        if (deviceCodeText != null) {
            deviceCodeText.setText(RorLicenseManager.getDeviceUniqueCode(this));
            deviceCodeText.setVisibility(View.VISIBLE);
        }

        boolean active = RorLicenseManager.isPremiumActive(this);
        lastKnownVipActive = active;

        View vipUnactivatedBox = findViewById(R.id.vip_unactivated_box);
        if (vipUnactivatedBox != null) {
            vipUnactivatedBox.setVisibility(active ? View.GONE : View.VISIBLE);
        }

        if (btnRemoveLicense != null) {
            btnRemoveLicense.setVisibility(active ? View.VISIBLE : View.GONE);
        }

        if (vipHeaderIcon != null) {
            if (active) {
                vipHeaderIcon.setImageResource(R.drawable.phoenix_logo);
            } else {
                vipHeaderIcon.setImageResource(R.drawable.ic_vip_badge_inactive);
            }
        }
        ImageView headerLogo = findViewById(R.id.dashboard_header_logo);
        TextView headerTitle = findViewById(R.id.dashboard_header_title);
        if (headerLogo != null) {
            headerLogo.setImageResource(active ? R.drawable.phoenix_logo : R.drawable.ror_logo);
        }
        if (headerTitle != null) {
            headerTitle.setText(active ? "PHOENIX PRO" : "REVOLUTIONARY");
        }

        if (statusBadge != null) {
            if (active) {
                statusBadge.setText("PHOENIX ACTIVE");
                statusBadge.setTextColor(Color.rgb(255, 215, 0));
                statusBadge.setBackgroundColor(Color.rgb(36, 30, 8));
            } else {
                statusBadge.setText("FREE TIER");
                statusBadge.setTextColor(Color.rgb(255, 82, 119));
                statusBadge.setBackgroundColor(Color.rgb(36, 21, 37));
            }
        }
        if (expiryText != null) {
            expiryText.setText(RorLicenseManager.getLicenseStatusDescription(this));
        }

        // Monitor Section Phoenix Card: Show only if Phoenix activated!
        if (monitorProCard != null) {
            if (active) {
                monitorProCard.setVisibility(View.VISIBLE);
                String expires = RorLicenseManager.getExpiresTimestamp(this);
                if (monitorProCountdown != null) {
                    monitorProCountdown.setText(RorLicenseManager.formatRemainingTime(expires));
                }
                if (monitorProScope != null) {
                    boolean isUniversal = RorLicenseManager.getLicenseStatusDescription(this).contains("Universal");
                    monitorProScope.setText(isUniversal ? "UNIVERSAL LICENSE" : "SIGNED LICENSE");
                }
            } else {
                monitorProCard.setVisibility(View.GONE);
            }
        }

        // Ads control: when active, turn off ads completely. When expired/free, show ads immediately!
        if (adBanner != null) {
            if (active) {
                adBanner.stopLoading();
                adBanner.setVisibility(View.GONE);
            } else {
                adBanner.setVisibility(View.VISIBLE);
                AdBypassHelper.setupAdWebView(adBanner, this);
            }
        }
    }

    private void showMiniWindowFavoritesDialog() {
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

        SharedPreferences sp = getSharedPreferences("ror_mini_window", MODE_PRIVATE);
        String raw = sp.getString("ror_mini_favorites", "com.facebook.orca,com.whatsapp,com.discord,org.telegram.messenger,com.google.android.youtube,com.android.chrome");
        List<String> currentFavs = Arrays.asList(raw.split(","));

        boolean[] checked = new boolean[pkgs.size()];
        for (int i = 0; i < pkgs.size(); i++) {
            checked[i] = currentFavs.contains(pkgs.get(i));
        }

        CharSequence[] items = names.toArray(new CharSequence[0]);
        new AlertDialog.Builder(this, R.style.RorDialogTheme)
                .setTitle("Select Mini Window Favorites")
                .setMultiChoiceItems(items, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("Save Favorites", (d, which) -> {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i]) {
                            if (sb.length() > 0) sb.append(",");
                            sb.append(pkgs.get(i));
                        }
                    }
                    sp.edit().putString("ror_mini_favorites", sb.toString()).apply();
                    toast("Mini Window favorites updated");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPage(int page) {
        if (pageFlipper == null) pageFlipper = findViewById(R.id.page_flipper);
        pageFlipper.setDisplayedChild(page);
        for (int i = 0; i < navItems.length; i++) {
            if (navItems[i] == null) continue;
            boolean selected = i == page;
            navItems[i].setBackgroundResource(selected ? R.drawable.ror_bottom_selected : android.R.color.transparent);
            navItems[i].setTextColor(Color.parseColor(selected ? "#D1A2FF" : "#9194B0"));
            TextViewCompat.setCompoundDrawableTintList(navItems[i], ColorStateList.valueOf(
                    Color.parseColor(selected ? "#D1A2FF" : "#9194B0")));
        }
        int[] scrollIds = {R.id.home_scroll, R.id.monitor_scroll, R.id.profiles_scroll, R.id.tools_scroll, R.id.more_scroll};
        if (page >= 0 && page < scrollIds.length) {
            View scroll = findViewById(scrollIds[page]);
            if (scroll != null) scroll.post(() -> scroll.scrollTo(0, 0));
        }
        if (page == 4) {
            updateVipMorePageStatus();
        }
    }

    private void showRootAccessDialog() {
        if (rootReady || shizukuReady || rootDialogVisible || isFinishing()) return;
        rootDialogVisible = true;
        new AlertDialog.Builder(this, R.style.RorDialogTheme)
                .setTitle("Enable advanced performance profiles?")
                .setMessage("Root enables the complete profile and kernel engine. Shizuku enables Android-level profiles, app control, Doze, display, and system settings without root. CPU/GPU/sysfs tuning still requires root.")
                .setNegativeButton("Not now", (d, w) -> {
                    rootDialogVisible = false;
                    rootReady = false;
                    setProfileAccess(shizukuReady);
                    toast("Advanced access not enabled; standard tools remain available");
                })
                .setNeutralButton("Use Shizuku", (d, w) -> {
                    rootDialogVisible = false;
                    if (!ShizukuBridge.available()) {
                        android.content.Intent shizukuIntent = getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
                        if (shizukuIntent != null) {
                            try {
                                startActivity(shizukuIntent);
                                toast("Opening Shizuku app. Start the service and return to REVROR.");
                            } catch (Exception e) {
                                toast("Error opening Shizuku: " + e.getMessage());
                            }
                        } else {
                            toast("Shizuku service is not running. Install Shizuku or grant root.");
                        }
                    } else if (!ShizukuBridge.granted()) {
                        toast("Requesting Shizuku authorization...");
                        ShizukuBridge.request(SHIZUKU_PERMISSION);
                    } else {
                        shizukuReady = true;
                        setProfileAccess(true);
                        SilentPermissionHelper.grantAllSilent(this);
                        toast("Shizuku connected and ready");
                    }
                })
                .setPositiveButton("Grant root", (d, w) -> {
                    rootDialogVisible = false;
                    requestRootAccess();
                })
                .setOnCancelListener(d -> rootDialogVisible = false)
                .show();
    }

    private void requestRootAccess() {
        profileStatus.setText("Waiting for root permission...");
        profileStatus.setTextColor(Color.rgb(255, 184, 77));
        toast("Respond to the root permission request");
        GsmProfileManager.requestRoot(this, (success, detail) -> {
            if (isFinishing() || isDestroyed()) return;
            rootReady = success;
            setProfileAccess(success);
            profileStatus.setText(detail);
            profileStatus.setTextColor(success ? Color.rgb(105, 220, 255) : Color.rgb(255, 82, 119));
            toast(success ? "Root granted - GSM profiles unlocked"
                    : "Root denied - non-root tools are still available");
            if (success) profileStatus.setText("Root ready - choose a profile to apply it");
        });
    }

    private void setProfileAccess(boolean enabled) {
        int[] cards = {R.id.profile_daily, R.id.profile_balance, R.id.profile_battery, R.id.profile_cooldown, R.id.profile_gaming, R.id.profile_ultra_gaming};
        int[] homeCards = {R.id.home_profile_daily, R.id.home_profile_balance,
                R.id.home_profile_battery, R.id.home_profile_cooldown, R.id.home_profile_gaming, R.id.home_profile_ultra_gaming};
        for (int i = 0; i < profileSwitches.length; i++) {
            if (profileSwitches[i] != null) profileSwitches[i].setEnabled(enabled);
            if (i < cards.length) {
                View c = findViewById(cards[i]);
                if (c != null) c.setAlpha(enabled ? 1f : 0.85f);
            }
            if (i < homeCards.length) {
                View hc = findViewById(homeCards[i]);
                if (hc != null) hc.setAlpha(enabled ? 1f : 0.85f);
            }
        }
        rootAccessButton.setText(rootReady ? "ROOT ACTIVE - FULL ENGINE READY"
                : shizukuReady ? "SHIZUKU ACTIVE - ANDROID CONTROLS READY"
                : "ENABLE ROOT OR SHIZUKU FOR PROFILES");
    }

    private void applyProfile(int profile) {
        if (isFinishing() || isDestroyed()) return;
        if (!GsmProfileManager.isPhoenixSelected(this) && profile == 5 && !RorLicenseManager.isPremiumActive(this)) {
            // Ultra Gaming is locked on Regular profile mode
            RorLicenseManager.showVipRequiredDialog(this, "Ultra Gaming Profile", () -> {
                applyProfile(profile);
            });
            return;
        }
        if (GsmProfileManager.usesPhoenixWorker(this, profile) && !RorLicenseManager.isPremiumActive(this)) {
            RorLicenseManager.showVipRequiredDialog(this, "Phoenix Profiles", () -> {
                applyProfile(profile);
            });
            return;
        }
        if (!rootReady && !shizukuReady) {
            toast("Enable Root or Shizuku before applying profiles");
            showRootAccessDialog();
            return;
        }
        applyingProfile = profile;
        selectProfile(profile);
        applyProfileAppearance(profile);
        com.revror.kernelmanager.utils.profile.CustomProfileManager.clearActiveCustomProfile(this);
        refreshCustomProfilesUi();
        boolean phoenix = GsmProfileManager.usesPhoenixWorker(this, profile);
        String logName = phoenix ? "phx.log" : "ror.log";
        String pName = profileName(profile);
        GsmProfileManager.appendSharedProfileLog(this, logName, pName + " profile applying");
        if (rootReady) {
            GsmProfileManager.apply(this, profile, this::onProfileResult);
        } else {
            applyShizukuProfile(profile);
        }
    }

    private void onProfileResult(boolean success, String detail) {
        if (isFinishing() || isDestroyed()) return;
        String message = detail == null ? "Profile operation finished" : detail;
        profileStatus.setText(message);
        if (success) rootReady = true;
        else if (message.toLowerCase(Locale.US).contains("root")) rootReady = false;
        setProfileAccess(rootReady || shizukuReady);
        profileStatus.setTextColor(success ? Color.rgb(105, 220, 255) : Color.rgb(255, 82, 119));
        toast(message);
        RorFeedback.profileFinished(this, profileName(Math.max(0, applyingProfile)), message, success);
    }

    private void selectProfile(int selected) {
        if (activeProfileAnimator != null) {
            activeProfileAnimator.cancel();
            activeProfileAnimator = null;
        }
        if (currentlyAnimatedProfileView != null) {
            currentlyAnimatedProfileView.setScaleX(1.0f);
            currentlyAnimatedProfileView.setScaleY(1.0f);
            currentlyAnimatedProfileView = null;
        }

        int[] cards = new int[]{
                R.id.profile_daily,
                R.id.profile_balance,
                R.id.profile_battery,
                R.id.profile_cooldown,
                R.id.profile_gaming,
                R.id.profile_ultra_gaming
        };
        int[] homeCards = new int[]{
                R.id.home_profile_daily,
                R.id.home_profile_balance,
                R.id.home_profile_battery,
                R.id.home_profile_cooldown,
                R.id.home_profile_gaming,
                R.id.home_profile_ultra_gaming
        };
        int[][] colors = {
                {0xFF0078D4, 0xFF00A7E8}, // 0: Daily Use
                {0xFF7220A8, 0xFFB12AC7}, // 1: Balance
                {0xFF008E46, 0xFF00C868}, // 2: Battery
                {0xFF007B9E, 0xFF00E5FF}, // 3: CoolDown
                {0xFFE1063C, 0xFFFF2A6D}, // 4: Gaming: vibrant cyber crimson ruby
                {0xFFFF1744, 0xFFFF5252}  // 5: Ultra Gaming: intense blazing flame scarlet
        };
        int[] defaultTints = {
                0xFF50CBFF, // 0: Daily Use
                0xFFD46CFF, // 1: Balance
                0xFF2DE49A, // 2: Battery
                0xFF00E5FF, // 3: CoolDown
                0xFFFF2E63, // 4: Gaming
                0xFFFF1744  // 5: Ultra Gaming
        };
        int radius = Math.round(10 * getResources().getDisplayMetrics().density);
        int strokeActive = Math.round(2.0f * getResources().getDisplayMetrics().density);
        int strokeInactive = Math.max(1, Math.round(1.0f * getResources().getDisplayMetrics().density));
        int pad14 = Math.round(14 * getResources().getDisplayMetrics().density);
        int pad6 = Math.round(6 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < cards.length; i++) {
            boolean active = i == selected;

            // Full profiles page card
            GradientDrawable full;
            if (active) {
                full = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{
                        (0xD9 << 24) | (colors[i][0] & 0xFFFFFF),
                        (0xF2 << 24) | (colors[i][1] & 0xFFFFFF)
                });
                full.setCornerRadius(radius);
                full.setStroke(strokeActive, 0xFFFFFFFF);
            } else {
                full = new GradientDrawable();
                full.setCornerRadius(radius);
                full.setColor(0xFF121528);
                full.setStroke(strokeInactive, 0xFF242845);
            }
            View cardView = findViewById(cards[i]);
            if (cardView != null) {
                cardView.setBackground(full);
                cardView.setPadding(pad14, pad14, pad14, pad14);
            }

            // Home performance tile: filled box with related color when active!
            GradientDrawable compact;
            if (active) {
                compact = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{
                        (0xD9 << 24) | (colors[i][0] & 0xFFFFFF),
                        (0xF2 << 24) | (colors[i][1] & 0xFFFFFF)
                });
                compact.setCornerRadius(radius);
                compact.setStroke(strokeActive, 0xFFFFFFFF);
            } else {
                compact = new GradientDrawable();
                compact.setCornerRadius(radius);
                compact.setColor(0xFF121528);
                compact.setStroke(strokeInactive, 0xFF242845);
            }
            View homeCardView = findViewById(homeCards[i]);
            if (homeCardView != null) {
                homeCardView.setBackground(compact);
                homeCardView.setPadding(pad6, pad6, pad6, pad6);
                if (homeCardView instanceof TextView) {
                    TextView tv = (TextView) homeCardView;
                    android.graphics.drawable.Drawable[] icons = tv.getCompoundDrawablesRelative();
                    if (icons[1] != null) {
                        android.graphics.drawable.Drawable icon = icons[1].mutate();
                        icon.setBounds(0, 0, dp(24), dp(24));
                        tv.setCompoundDrawablesRelative(null, icon, null, null);
                    }
                    tv.setTextColor(active ? Color.WHITE : 0xFFDCE1EE);
                    androidx.core.widget.TextViewCompat.setCompoundDrawableTintList(tv,
                            ColorStateList.valueOf(active ? Color.WHITE : defaultTints[i]));
                }
                if (active) {
                    currentlyAnimatedProfileView = homeCardView;
                    activeProfileAnimator = ValueAnimator.ofFloat(1.0f, 1.042f);
                    activeProfileAnimator.setDuration(1200);
                    activeProfileAnimator.setRepeatMode(ValueAnimator.REVERSE);
                    activeProfileAnimator.setRepeatCount(ValueAnimator.INFINITE);
                    activeProfileAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
                    activeProfileAnimator.addUpdateListener(anim -> {
                        float s = (float) anim.getAnimatedValue();
                        if (currentlyAnimatedProfileView != null) {
                            currentlyAnimatedProfileView.setScaleX(s);
                            currentlyAnimatedProfileView.setScaleY(s);
                        }
                    });
                    activeProfileAnimator.start();
                }
            }

            if (i < profileSwitches.length && profileSwitches[i] != null) {
                profileSwitches[i].setChecked(active);
            }
        }
    }

    private void applyShizukuProfile(int profile) {
        if (GsmProfileManager.requiresLicense(this, profile) && !RorLicenseManager.isPremiumActive(this)) return;
        GsmProfileManager.saveSelectedProfile(this, profile);
        String command;
        if (profile == 3) {
            String exclusions = GameOptimizer.buildCoolDownExclusionPattern(this);
            command = "cmd power set-mode 1; cmd netpolicy set restrict-background true; settings put global animator_duration_scale 0; settings put global transition_animation_scale 0; settings put global window_animation_scale 0; sync; [ -w /proc/sys/vm/drop_caches ] && echo 3 > /proc/sys/vm/drop_caches 2>/dev/null || true; [ -w /proc/sys/vm/compact_memory ] && echo 1 > /proc/sys/vm/compact_memory 2>/dev/null || true;";
        } else if (profile == 2) {
            command = "cmd power set-mode 1; cmd netpolicy set restrict-background true; settings put global animator_duration_scale 0.5; settings put global transition_animation_scale 0.5; settings put global window_animation_scale 0.5";
        } else if (profile == 4) {
            command = "cmd power set-mode 0; cmd netpolicy set restrict-background false; settings put global animator_duration_scale 0.75; settings put global transition_animation_scale 0.75; settings put global window_animation_scale 0.75";
        } else if (profile == 5) { // Ultra Gaming
            command = "cmd power set-mode 0; cmd power set-fixed-performance-mode-enabled true; cmd netpolicy set restrict-background false; settings put global animator_duration_scale 0.5; settings put global transition_animation_scale 0.5; settings put global window_animation_scale 0.5; sync; [ -w /proc/sys/vm/drop_caches ] && echo 3 > /proc/sys/vm/drop_caches 2>/dev/null || true";
        } else if (profile == 1) {
            command = "cmd power set-mode 0; settings put global animator_duration_scale 0.85; settings put global transition_animation_scale 0.85; settings put global window_animation_scale 0.85";
        } else {
            command = "cmd power set-mode 0; settings put global animator_duration_scale 1; settings put global transition_animation_scale 1; settings put global window_animation_scale 1";
        }
        submitTask(() -> {
            ShizukuBridge.Result result = ShizukuBridge.run(command);
            String detail = result.success ? "All Android-level tweaks applied through Shizuku; kernel tuning requires root"
                    : "Shizuku profile failed: " + result.output;
            if (result.success) {
                boolean phoenix = GsmProfileManager.usesPhoenixWorker(this, profile);
                String logName = phoenix ? "phx.log" : "ror.log";
                GsmProfileManager.appendSharedProfileLog(this, logName, profileName(profile) + " profile activated");
            }
            postToUi(() -> {
                if (isFinishing() || isDestroyed()) return;
                profileStatus.setText(detail);
                profileStatus.setTextColor(Color.parseColor(result.success ? "#69DCFF" : "#FF5277"));
                toast(result.success ? ("Current profile - " + profileName(profile)) : detail);
                RorFeedback.profileFinished(this, profileName(profile), detail, result.success);
            });
        });
    }

    private void setupAdBanner() {
        if (adBanner == null) return;
        if (RorLicenseManager.isPremiumActive(this)) {
            adBanner.setVisibility(View.GONE);
            try { adBanner.stopLoading(); } catch (Throwable ignored) {}
            return;
        }
        AdBypassHelper.setupAdWebView(adBanner, this);
    }

    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }

    private void applyProfileAppearance(int profile) {
        int safeProfile = Math.max(0, Math.min(5, profile));

        // Profile neon accents: Daily=Cyan/Blue, Balance=Violet, Battery=Emerald, CoolDown=Frost Cyan, Gaming=Crimson Pink, Ultra Gaming=Blazing Scarlet
        int[] neonColors = {0xFF00B4D8, 0xFFD1A2FF, 0xFF00E676, 0xFF00E5FF, 0xFFFA476B, 0xFFFF1744};
        int[] bgStartColors = {0xFF0B192A, 0xFF180D2C, 0xFF0A1F14, 0xFF091C28, 0xFF1C091A, 0xFF24060B};
        int[] chromeColors = {0xFF05101C, 0xFF0F061C, 0xFF04130A, 0xFF04121B, 0xFF12040E, 0xFF140204};

        int neon = neonColors[safeProfile];
        int bgStart = bgStartColors[safeProfile];
        int chrome = chromeColors[safeProfile];

        getWindow().setStatusBarColor(chrome);
        getWindow().setNavigationBarColor(chrome);

        SharedPreferences themeSp = getSharedPreferences("ror_theme", MODE_PRIVATE);
        String customBannerPath = themeSp.getString("custom_banner_path", null);
        int presetBannerIdx = themeSp.getInt("preset_banner_index", 0);
        int blurRadius = themeSp.getInt("banner_blur_radius", 0);

        // Existing installs used the static procedural card as the implicit default.
        // Migrate that implicit state once so the home page immediately has an animated
        // image banner; users can still select the procedural card or disable animation.
        if (!themeSp.getBoolean("animated_banner_default_migrated", false)) {
            SharedPreferences.Editor migration = themeSp.edit()
                    .putBoolean("animated_banner_default_migrated", true);
            if ((customBannerPath == null || !new File(customBannerPath).exists())
                    && presetBannerIdx == 0) {
                presetBannerIdx = 19;
                migration.putInt("preset_banner_index", presetBannerIdx)
                        .remove("custom_banner_path")
                        .putBoolean("follow_profile", false);
            }
            migration.apply();
        }

        ImageView heroBanner = findViewById(R.id.hero_banner);
        View heroGlow = findViewById(R.id.hero_glow_circle);
        View heroTextContainer = findViewById(R.id.hero_text_container);
        View heroBg = findViewById(R.id.hero_bg_view);

        boolean hasImageBanner = false;
        if (customBannerPath != null && new File(customBannerPath).exists()) {
            if (heroBanner != null) {
                heroBanner.setVisibility(View.VISIBLE);
                if (!customBannerPath.equals(heroBanner.getTag())) {
                    heroBanner.setImageURI(Uri.fromFile(new File(customBannerPath)));
                    heroBanner.setTag(customBannerPath);
                }
                applyBannerBlur(heroBanner, blurRadius);
            }
            hasImageBanner = true;
        } else if (presetBannerIdx >= 1 && presetBannerIdx <= 20) {
            int resId = getHomeBannerResource(presetBannerIdx);
            if (resId != 0 && heroBanner != null) {
                heroBanner.setVisibility(View.VISIBLE);
                if (!Integer.valueOf(resId).equals(heroBanner.getTag())) {
                    heroBanner.setImageResource(resId);
                    heroBanner.setTag(resId);
                }
                applyBannerBlur(heroBanner, blurRadius);
                hasImageBanner = true;
            }
        }

        View heroScrim = findViewById(R.id.hero_scrim);
        if (hasImageBanner) {
            if (heroBanner != null) heroBanner.setVisibility(View.VISIBLE);
            if (heroGlow != null) heroGlow.setVisibility(View.GONE);
            if (heroBg != null) heroBg.setVisibility(View.GONE);
            if (heroScrim != null) heroScrim.setVisibility(View.VISIBLE);
        } else {
            if (heroBanner != null) heroBanner.setVisibility(View.GONE);
            if (heroGlow != null) heroGlow.setVisibility(View.VISIBLE);
            if (heroBg != null) heroBg.setVisibility(View.VISIBLE);
            if (heroScrim != null) heroScrim.setVisibility(View.GONE);
        }
        applyHomeBannerAnimation(heroBanner, hasImageBanner && themeSp.getBoolean("animate_home_banner", true));
        // Banner text is permanent and never disappears
        if (heroTextContainer != null) heroTextContainer.setVisibility(View.VISIBLE);

        TextView heroTitleSmall = findViewById(R.id.hero_title_small);
        TextView heroTitleLarge = findViewById(R.id.hero_title_large);
        TextView heroSubtitle = findViewById(R.id.hero_subtitle);
        View heroPill = findViewById(R.id.hero_pill_chip);
        TextView heroPillText = findViewById(R.id.hero_pill_text);
        View heroPillDot = findViewById(R.id.hero_pill_dot);
        View heroPillDotGlow = findViewById(R.id.hero_pill_dot_glow);
        setupPillDotBlinker(heroPillDot, heroPillDotGlow);

        View.OnClickListener kofiClick = v -> {
            try {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            } catch (Exception ignored) {}
            openUrl("https://ko-fi.com/revWS");
        };
        if (heroPill != null) heroPill.setOnClickListener(kofiClick);
        if (heroPillText != null) heroPillText.setOnClickListener(kofiClick);
        if (heroPillDot != null) heroPillDot.setOnClickListener(kofiClick);
        if (heroPillDotGlow != null) heroPillDotGlow.setOnClickListener(kofiClick);
        View heroPillDotContainer = findViewById(R.id.hero_pill_dot_container);
        if (heroPillDotContainer != null) heroPillDotContainer.setOnClickListener(kofiClick);

        int bannerTextStyleIdx = com.revror.kernelmanager.utils.BannerTextStyleRegistry.getActiveStyleIndex(this);
        com.revror.kernelmanager.utils.BannerTextStyleRegistry.applyToDashboardHero(
                this, bannerTextStyleIdx, heroTitleSmall, heroTitleLarge, heroSubtitle, heroPill, heroPillText);

        if (heroGlow != null) {
            GradientDrawable glowDrawable = new GradientDrawable();
            glowDrawable.setShape(GradientDrawable.OVAL);
            glowDrawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);
            glowDrawable.setGradientRadius(dp(130));
            int alpha1 = (0x66 << 24) | (neon & 0x00FFFFFF);
            int alpha2 = (0x22 << 24) | (neon & 0x00FFFFFF);
            glowDrawable.setColors(new int[]{alpha1, alpha2, 0x00000000});
            heroGlow.setBackground(glowDrawable);
        }

        if (heroBg != null) {
            GradientDrawable bgDrawable = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{bgStart, 0xFF0D0B16, 0xFF08060D});
            bgDrawable.setCornerRadius(dp(22));
            int strokeColor = (0x55 << 24) | (neon & 0x00FFFFFF);
            bgDrawable.setStroke(Math.max(1, dp(1)), strokeColor);
            heroBg.setBackground(bgDrawable);
        }

        if (heroPill != null && bannerTextStyleIdx == 0) {
            GradientDrawable pillDrawable = new GradientDrawable();
            pillDrawable.setColor(0x33100B1A);
            pillDrawable.setCornerRadius(dp(20));
            int strokeColor = (0x66 << 24) | (neon & 0x00FFFFFF);
            pillDrawable.setStroke(dp(1), strokeColor);
            heroPill.setBackground(pillDrawable);
        }

        updateHeroDeviceInfo();
        View heroDeviceRow = findViewById(R.id.hero_device_row);
        if (heroDeviceRow != null) {
            heroDeviceRow.setOnClickListener(null);
            heroDeviceRow.setClickable(false);
            heroDeviceRow.setFocusable(false);
        }
        View heroContainer = findViewById(R.id.hero_card_container);
        if (heroContainer != null) {
            heroContainer.setOnClickListener(v -> {
                try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
                showAppearance();
            });
        }
    }

    private ValueAnimator pillDotAnimator;
    private static final int[] NEON_PULSE_COLORS = new int[]{
            0xFF00FF66, // Matrix Neon Green
            0xFF00E5FF, // Cyber Cyan
            0xFFFF1744, // Inferno Red
            0xFFFF007F, // Synthwave Pink
            0xFFFFB800, // Amber Gold
            0xFF9D00FF, // Royal Violet
            0xFF00FFCC, // Electric Mint
            0xFF3D5AFE, // Neon Sapphire
            0xFFFF5252, // Coral Crimson
            0xFFE040FB, // Neon Purple
            0xFFFFEE00, // Electric Yellow
            0xFF00FFA2  // Cyber Mint
    };
    private int pillDotColorA = 0xFF00FF66;
    private int pillDotColorB = 0xFF00E5FF;
    private final android.animation.ArgbEvaluator argbEvaluator = new android.animation.ArgbEvaluator();

    private void setupPillDotBlinker(View dotView, View glowView) {
        if (dotView == null) return;
        if (pillDotAnimator != null) {
            pillDotAnimator.cancel();
            pillDotAnimator = null;
        }
        View.OnClickListener kofiClick = v -> {
            try {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            } catch (Exception ignored) {}
            openUrl("https://ko-fi.com/revWS");
        };
        dotView.setOnClickListener(kofiClick);
        if (glowView != null) glowView.setOnClickListener(kofiClick);
        Random rng = new Random();
        pillDotColorA = NEON_PULSE_COLORS[rng.nextInt(NEON_PULSE_COLORS.length)];
        pillDotColorB = NEON_PULSE_COLORS[rng.nextInt(NEON_PULSE_COLORS.length)];
        while (pillDotColorB == pillDotColorA) {
            pillDotColorB = NEON_PULSE_COLORS[rng.nextInt(NEON_PULSE_COLORS.length)];
        }

        final GradientDrawable dotDrawable = new GradientDrawable();
        dotDrawable.setShape(GradientDrawable.OVAL);
        dotDrawable.setCornerRadius(999f);
        dotDrawable.setColor(pillDotColorA);
        dotView.setBackground(dotDrawable);
        if (Build.VERSION.SDK_INT >= 21) {
            dotView.setClipToOutline(true);
        }

        final GradientDrawable glowDrawable = new GradientDrawable();
        glowDrawable.setShape(GradientDrawable.OVAL);
        glowDrawable.setCornerRadius(999f);
        glowDrawable.setColor((0x33 << 24) | (pillDotColorA & 0x00FFFFFF));
        if (glowView != null) {
            glowView.setBackground(glowDrawable);
            if (Build.VERSION.SDK_INT >= 21) {
                glowView.setClipToOutline(true);
            }
        }

        // Slow, hypnotic, stylish 2600ms breathing animation
        pillDotAnimator = ValueAnimator.ofFloat(0f, 1f);
        pillDotAnimator.setDuration(2600);
        pillDotAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pillDotAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pillDotAnimator.addUpdateListener(animation -> {
            float frac = (float) animation.getAnimatedValue();
            // Smooth natural inhaling / exhaling pulse curve
            float pulse = (float) Math.sin(frac * Math.PI);

            int currentColor = (Integer) argbEvaluator.evaluate(pulse, pillDotColorA, pillDotColorB);
            dotDrawable.setColor(currentColor);
            glowDrawable.setColor(((int)(0x15 + 0x45 * pulse) << 24) | (currentColor & 0x00FFFFFF));

            if (dotView != null) {
                // Subtle organic core breathing: scale 0.88x - 1.08x, alpha 0.75 - 1.0
                float dotScale = 0.88f + 0.20f * pulse;
                dotView.setScaleX(dotScale);
                dotView.setScaleY(dotScale);
                dotView.setAlpha(0.75f + 0.25f * pulse);
            }
            if (glowView != null) {
                // Ambient halo ring swells smoothly: scale 0.70x - 1.35x, alpha 0.18 - 0.75
                float glowScale = 0.70f + 0.65f * pulse;
                glowView.setScaleX(glowScale);
                glowView.setScaleY(glowScale);
                glowView.setAlpha(0.18f + 0.57f * pulse);
            }
        });
        pillDotAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationRepeat(Animator animation) {
                // Advance color smoothly on each breathing loop without jumping
                pillDotColorA = pillDotColorB;
                pillDotColorB = NEON_PULSE_COLORS[rng.nextInt(NEON_PULSE_COLORS.length)];
            }
        });
        pillDotAnimator.start();
    }

    private void applyBannerBlur(View view, int blurRadius) {
        if (view == null) return;
        if (Build.VERSION.SDK_INT >= 31) {
            if (blurRadius > 0) {
                view.setRenderEffect(android.graphics.RenderEffect.createBlurEffect(
                        (float) blurRadius, (float) blurRadius, android.graphics.Shader.TileMode.CLAMP));
            } else {
                view.setRenderEffect(null);
            }
            view.setAlpha(1.0f);
        } else {
            view.setAlpha(blurRadius > 0 ? Math.max(0.45f, 1.0f - (blurRadius / 50.0f)) : 1.0f);
        }
    }

    private View createAppearanceActionPill(int iconResId, String title, int accentColor, View.OnClickListener listener) {
        float density = getResources().getDisplayMetrics().density;
        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding((int) (8 * density), (int) (8 * density), (int) (8 * density), (int) (8 * density));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF1E2235);
        bg.setCornerRadius(10 * density);
        bg.setStroke(1, accentColor & 0x77FFFFFF);
        pill.setBackground(bg);

        if (iconResId != 0) {
            ImageView icon = new ImageView(this);
            int iconSize = (int) (13 * density);
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
            iconLp.setMarginEnd((int) (5 * density));
            icon.setLayoutParams(iconLp);
            icon.setImageResource(iconResId);
            icon.setColorFilter(accentColor);
            pill.addView(icon);
        }

        TextView btn = new TextView(this);
        btn.setText(title);
        btn.setTextSize(9.5f);
        btn.setTextColor(Color.WHITE);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setSingleLine(true);
        btn.setEllipsize(TextUtils.TruncateAt.END);
        pill.addView(btn);

        pill.setOnClickListener(v -> {
            try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
            if (listener != null) listener.onClick(v);
        });
        return pill;
    }

    private void applyPresetBanner(int presetIndex) {
        SharedPreferences.Editor edit = getSharedPreferences("ror_theme", MODE_PRIVATE).edit()
                .remove("custom_banner_path");
        if (presetIndex == 0) {
            edit.putInt("preset_banner_index", 0)
                .putBoolean("follow_profile", true);
        } else {
            edit.putInt("preset_banner_index", presetIndex)
                .putBoolean("follow_profile", false);
        }
        edit.apply();
        applyProfileAppearance(GsmProfileManager.selectedProfile(this));
    }

    private int getHomeBannerResource(int presetIndex) {
        if (presetIndex == 19) return R.drawable.revror_banner_anime_arena;
        if (presetIndex == 20) return R.drawable.revror_banner_waifu_squad;
        return getResources().getIdentifier(
                String.format(Locale.US, "ror_banner_%02d", presetIndex), "drawable", getPackageName());
    }

    private void applyHomeBannerAnimation(ImageView banner, boolean enabled) {
        if (banner == null) return;
        banner.setScaleX(1f);
        banner.setScaleY(1f);
        banner.setTranslationX(0f);
        banner.setTranslationY(0f);
        banner.setRotation(0f);
        banner.setAlpha(1f);
        if (banner instanceof com.revror.kernelmanager.views.AnimatedHomeBannerView) {
            com.revror.kernelmanager.views.AnimatedHomeBannerView animatedView =
                    (com.revror.kernelmanager.views.AnimatedHomeBannerView) banner;
            animatedView.loadPreferences(this);
            animatedView.setFrameAnimationEnabled(enabled && banner.getVisibility() == View.VISIBLE);
        }
    }

    private void stopHomeBannerAnimation(ImageView banner) {
        if (banner == null) return;
        if (banner instanceof com.revror.kernelmanager.views.AnimatedHomeBannerView) {
            ((com.revror.kernelmanager.views.AnimatedHomeBannerView) banner)
                    .setFrameAnimationEnabled(false);
        }
        banner.setScaleX(1f);
        banner.setScaleY(1f);
        banner.setTranslationX(0f);
        banner.setTranslationY(0f);
        banner.setRotation(0f);
        banner.setAlpha(1f);
    }

    private View buildBannerThumbnailCard(int presetIdx, String title, String subtitle, boolean isSelected, View.OnClickListener onClick) {
        float density = getResources().getDisplayMetrics().density;
        int p6 = (int) (6 * density);
        int p8 = (int) (8 * density);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(p6, p6, p6, p8);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(isSelected ? 0xFF1B2338 : 0xFF141724);
        bg.setCornerRadius(12 * density);
        bg.setStroke(isSelected ? (int) (2 * density) : (int) (1 * density),
                isSelected ? 0xFF00E5FF : 0xFF2A2E42);
        card.setBackground(bg);

        FrameLayout imgFrame = new FrameLayout(this);
        FrameLayout.LayoutParams frameLp = new FrameLayout.LayoutParams(-1, (int) (72 * density));
        imgFrame.setLayoutParams(frameLp);

        if (presetIdx == 0) {
            // Adaptive procedural card thumbnail preview
            View procBg = new View(this);
            GradientDrawable procDrawable = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{0xFF0E1A2B, 0xFF1A0F2B, 0xFF080914});
            procDrawable.setCornerRadius(8 * density);
            procBg.setBackground(procDrawable);
            imgFrame.addView(procBg, new FrameLayout.LayoutParams(-1, -1));

            LinearLayout badge = new LinearLayout(this);
            badge.setOrientation(LinearLayout.HORIZONTAL);
            badge.setGravity(Gravity.CENTER);
            ImageView bIcon = new ImageView(this);
            int bSize = (int) (12 * density);
            bIcon.setLayoutParams(new LinearLayout.LayoutParams(bSize, bSize));
            bIcon.setImageResource(R.drawable.ic_flash);
            bIcon.setColorFilter(0xFF00E5FF);
            badge.addView(bIcon);
            TextView bText = new TextView(this);
            bText.setText(" ADAPTIVE NEON");
            bText.setTextSize(9f);
            bText.setTextColor(0xFF00E5FF);
            bText.setTypeface(null, Typeface.BOLD);
            badge.addView(bText);
            imgFrame.addView(badge, new FrameLayout.LayoutParams(-1, -1));
        } else {
            // Preset banners, including REVROR original anime gaming artwork
            ImageView img = new ImageView(this);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            int resId = getHomeBannerResource(presetIdx);
            if (resId != 0) {
                img.setImageResource(resId);
            }
            if (Build.VERSION.SDK_INT >= 21) {
                img.setClipToOutline(true);
                img.setOutlineProvider(new android.view.ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, android.graphics.Outline outline) {
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 8 * density);
                    }
                });
            }
            imgFrame.addView(img, new FrameLayout.LayoutParams(-1, -1));
        }

        if (isSelected) {
            LinearLayout activeBadge = new LinearLayout(this);
            activeBadge.setOrientation(LinearLayout.HORIZONTAL);
            activeBadge.setGravity(Gravity.CENTER_VERTICAL);
            activeBadge.setPadding((int) (6 * density), (int) (2 * density), (int) (6 * density), (int) (2 * density));
            GradientDrawable bBg = new GradientDrawable();
            bBg.setColor(0xEE00B4D8);
            bBg.setCornerRadius(6 * density);
            activeBadge.setBackground(bBg);

            TextView aText = new TextView(this);
            aText.setText("ACTIVE");
            aText.setTextSize(8f);
            aText.setTextColor(Color.WHITE);
            aText.setTypeface(null, Typeface.BOLD);
            activeBadge.addView(aText);

            ImageView aIcon = new ImageView(this);
            int aSize = (int) (9 * density);
            LinearLayout.LayoutParams aIconLp = new LinearLayout.LayoutParams(aSize, aSize);
            aIconLp.setMarginStart((int) (3 * density));
            aIcon.setLayoutParams(aIconLp);
            aIcon.setImageResource(R.drawable.ic_done);
            aIcon.setColorFilter(Color.WHITE);
            activeBadge.addView(aIcon);

            FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(-2, -2);
            badgeLp.gravity = Gravity.TOP | Gravity.END;
            badgeLp.topMargin = (int) (4 * density);
            badgeLp.rightMargin = (int) (4 * density);
            imgFrame.addView(activeBadge, badgeLp);
        }

        card.addView(imgFrame);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(10.5f);
        titleView.setTextColor(isSelected ? 0xFF00E5FF : Color.WHITE);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        titleView.setPadding(0, (int) (6 * density), 0, (int) (1 * density));
        card.addView(titleView);

        TextView subView = new TextView(this);
        subView.setText(subtitle);
        subView.setTextSize(8.5f);
        subView.setTextColor(0xFF8A92AB);
        subView.setSingleLine(true);
        subView.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(subView);

        card.setOnClickListener(onClick);
        return card;
    }

    private void showAppearance() {
        if (isFinishing() || isDestroyed()) return;
        try {
            Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            float density = getResources().getDisplayMetrics().density;
            int dp8 = (int) (8 * density);
            int dp12 = (int) (12 * density);
            int dp14 = (int) (14 * density);
            int dp16 = (int) (16 * density);

            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(dp16, dp14, dp16, dp14);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xF412141D);
            bg.setCornerRadius(18 * density);
            bg.setStroke(1, 0xFF35394A);
            root.setBackground(bg);

            // 1. Header
            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setPadding(0, 0, 0, dp12);

            LinearLayout titleCol = new LinearLayout(this);
            titleCol.setOrientation(LinearLayout.VERTICAL);

            TextView title = new TextView(this);
            title.setText("HERO BANNER & THEMES");
            title.setTextSize(14.5f);
            title.setTextColor(Color.WHITE);
            title.setTypeface(null, Typeface.BOLD);
            title.setLetterSpacing(0.06f);
            titleCol.addView(title);

            TextView subtitle = new TextView(this);
            subtitle.setText("Select preset banner thumbnail or custom theme");
            subtitle.setTextSize(10f);
            subtitle.setTextColor(0xFF8E95B2);
            titleCol.addView(subtitle);

            header.addView(titleCol, new LinearLayout.LayoutParams(0, -2, 1f));

            ImageView closeBtn = new ImageView(this);
            int closeSize = (int) (24 * density);
            LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(closeSize, closeSize);
            closeBtn.setLayoutParams(closeLp);
            closeBtn.setImageResource(R.drawable.ic_cancel);
            closeBtn.setColorFilter(0xFFB0B5C6);
            closeBtn.setPadding((int) (3 * density), (int) (3 * density), (int) (3 * density), (int) (3 * density));
            closeBtn.setOnClickListener(v -> {
                try { if (dialog.isShowing()) dialog.dismiss(); } catch (Exception ignored) {}
            });
            header.addView(closeBtn);
            root.addView(header);

            // 2. Action row: Text Styles | Anim Styles | Custom Image | Blur Slider
            HorizontalScrollView actionScroll = new HorizontalScrollView(this);
            actionScroll.setHorizontalScrollBarEnabled(false);
            actionScroll.setPadding(0, 0, 0, dp12);

            LinearLayout actionsRow = new LinearLayout(this);
            actionsRow.setOrientation(LinearLayout.HORIZONTAL);

            View btnTextStyles = createAppearanceActionPill(R.drawable.ic_text_style, "Text Styles (" + com.revror.kernelmanager.utils.BannerTextStyleRegistry.STYLES.length + ")", 0xFF00FFE5, v -> {
                try { if (dialog.isShowing()) dialog.dismiss(); } catch (Exception ignored) {}
                com.revror.kernelmanager.utils.BannerTextStyleRegistry.showBannerTextStyleDialog(
                        DashboardActivity.this, () -> applyProfileAppearance(GsmProfileManager.selectedProfile(DashboardActivity.this)));
            });
            View btnAnimStyles = createAppearanceActionPill(R.drawable.ic_sparkle, "Anim Styles (" + com.revror.kernelmanager.views.AnimatedHomeBannerView.STYLE_NAMES.length + ")", 0xFFFFB300, v -> {
                try { if (dialog.isShowing()) dialog.dismiss(); } catch (Exception ignored) {}
                showBannerAnimationStyleDialog();
            });
            View btnCustom = createAppearanceActionPill(R.drawable.ic_gallery, "Custom Gallery", 0xFFFF007F, v -> {
                try { if (dialog.isShowing()) dialog.dismiss(); } catch (Exception ignored) {}
                try { bannerPickerLauncher.launch("image/*"); } catch (Exception e) { toast("Cannot open file picker: " + e.getMessage()); }
            });
            View btnBlur = createAppearanceActionPill(R.drawable.ic_blur_slider, "Blur Slider", 0xFF00FF66, v -> {
                try { if (dialog.isShowing()) dialog.dismiss(); } catch (Exception ignored) {}
                showBannerBlurControlDialog();
            });

            LinearLayout.LayoutParams actionLp = new LinearLayout.LayoutParams(-2, -2);
            actionLp.setMargins(0, 0, dp8, 0);
            actionsRow.addView(btnTextStyles, actionLp);
            actionsRow.addView(btnAnimStyles, actionLp);
            actionsRow.addView(btnCustom, actionLp);
            actionsRow.addView(btnBlur, actionLp);
            actionScroll.addView(actionsRow, new ViewGroup.LayoutParams(-2, -2));
            root.addView(actionScroll);

            android.widget.Switch animateBanner = new android.widget.Switch(this);
            animateBanner.setText("Live animate all home banners");
            animateBanner.setTextColor(Color.WHITE);
            animateBanner.setTextSize(12f);
            animateBanner.setChecked(getSharedPreferences("ror_theme", MODE_PRIVATE)
                    .getBoolean("animate_home_banner", true));
            animateBanner.setPadding(dp8, 0, dp8, dp12);
            animateBanner.setOnCheckedChangeListener((button, checked) -> {
                getSharedPreferences("ror_theme", MODE_PRIVATE).edit()
                        .putBoolean("animate_home_banner", checked).apply();
                applyProfileAppearance(GsmProfileManager.selectedProfile(DashboardActivity.this));
                toast(checked ? "Home banner animation enabled" : "Home banner animation disabled");
            });
            root.addView(animateBanner, new LinearLayout.LayoutParams(-1, -2));

            // 3. Scrollable Grid of All Preset Banners + Procedural Card
            ScrollView scroll = new ScrollView(this);
            scroll.setVerticalScrollBarEnabled(false);
            LinearLayout gridContainer = new LinearLayout(this);
            gridContainer.setOrientation(LinearLayout.VERTICAL);

            SharedPreferences themeSp = getSharedPreferences("ror_theme", MODE_PRIVATE);
            final String[] bannerNames = {
                    "Adaptive Procedural",
                    "Preset 1: Cyberpunk Red",
                    "Preset 2: Neon Pulse",
                    "Preset 3: Obsidian Gaming",
                    "Preset 4: Frost Blue",
                    "Preset 5: Emerald Overclock",
                    "Preset 6: Electric Amber",
                    "Preset 7: Midnight Synthwave",
                    "Preset 8: Deep Nebula",
                    "Preset 9: Quantum Crimson",
                    "Preset 10: Apex Titanium",
                    "Preset 11: Neo-Kyoto Mech",
                    "Preset 12: Cyber Samurai Blade",
                    "Preset 13: Neon Highway Redline",
                    "Preset 14: Imperial Cyber Dragon",
                    "Preset 15: Hellfire Magma Surge",
                    "Preset 16: Deep Cosmos Nebula",
                    "Preset 17: SpecOps Combat Radar",
                    "Preset 18: Synthwave Pulse",
                    "Preset 19: Anime Arena Vanguard",
                    "Preset 20: Waifu Cyber Squad"
            };
            final String[] bannerSubtitles = {
                    "Dynamic profile glow",
                    "Red Cyberpunk Core",
                    "Neon Pulse Violet",
                    "Obsidian Stealth",
                    "Frost Cryo Zero",
                    "Emerald Overclock",
                    "Solar Amber Boost",
                    "Retro 80s Synthwave",
                    "Deep Space Nebula",
                    "Quantum Speed Crimson",
                    "Apex Master Titanium",
                    "Futuristic Mech City",
                    "Dual Katana Neon Rain",
                    "Supercar Blazing Trails",
                    "Golden High-Tech Dragon",
                    "Volcanic Core Overload",
                    "Interstellar Galaxy Orbit",
                    "Tactical HUD Telemetry",
                    "Retro Wave Audio Pulse",
                    "Original anime gaming heroine",
                    "Original waifu-led gaming squad"
            };

            Runnable[] rebuildGrid = new Runnable[1];
            rebuildGrid[0] = () -> {
                gridContainer.removeAllViews();
                int currentPreset = themeSp.getInt("preset_banner_index", 0);
                String customPath = themeSp.getString("custom_banner_path", null);
                boolean hasCustom = (customPath != null && new File(customPath).exists());

                for (int i = 0; i <= 20; i += 2) {
                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
                    rowLp.bottomMargin = (int) (10 * density);
                    row.setLayoutParams(rowLp);

                    // Col 1
                    final int idx1 = i;
                    boolean isSel1 = (!hasCustom && currentPreset == idx1);
                    View card1 = buildBannerThumbnailCard(idx1, bannerNames[idx1], bannerSubtitles[idx1], isSel1, v -> {
                        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        if (idx1 > 0 && !RorLicenseManager.isPremiumActive(this)) {
                            RorLicenseManager.showVipRequiredDialog(this, bannerNames[idx1], () -> {
                                applyPresetBanner(idx1);
                                rebuildGrid[0].run();
                            });
                            return;
                        }
                        applyPresetBanner(idx1);
                        rebuildGrid[0].run();
                        toast(idx1 == 0 ? "Adaptive procedural card enabled" : "Preset banner " + idx1 + " applied");
                    });
                    LinearLayout.LayoutParams c1Lp = new LinearLayout.LayoutParams(0, -2, 1f);
                    c1Lp.setMarginEnd((int) (5 * density));
                    row.addView(card1, c1Lp);

                    // Col 2 (if exists)
                    if (i + 1 <= 20) {
                        final int idx2 = i + 1;
                        boolean isSel2 = (!hasCustom && currentPreset == idx2);
                        View card2 = buildBannerThumbnailCard(idx2, bannerNames[idx2], bannerSubtitles[idx2], isSel2, v -> {
                            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                            if (idx2 > 0 && !RorLicenseManager.isPremiumActive(this)) {
                                RorLicenseManager.showVipRequiredDialog(this, bannerNames[idx2], () -> {
                                    applyPresetBanner(idx2);
                                    rebuildGrid[0].run();
                                });
                                return;
                            }
                            applyPresetBanner(idx2);
                            rebuildGrid[0].run();
                            toast("Preset banner " + idx2 + " applied");
                        });
                        LinearLayout.LayoutParams c2Lp = new LinearLayout.LayoutParams(0, -2, 1f);
                        c2Lp.setMarginStart((int) (5 * density));
                        row.addView(card2, c2Lp);
                    } else {
                        View spacer = new View(this);
                        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(0, 0, 1f);
                        sLp.setMarginStart((int) (5 * density));
                        row.addView(spacer, sLp);
                    }
                    gridContainer.addView(row);
                }
            };
            rebuildGrid[0].run();
            scroll.addView(gridContainer, new ScrollView.LayoutParams(-1, -2));
            root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

            dialog.setContentView(root);
            dialog.show();
        } catch (Exception ignored) {}
    }

    private void showBannerBlurControlDialog() {
        if (isFinishing() || isDestroyed()) return;
        try {
            SharedPreferences sp = getSharedPreferences("ror_theme", MODE_PRIVATE);
            int currentBlur = sp.getInt("banner_blur_radius", 0);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(dp(20), dp(14), dp(20), dp(10));

            TextView label = new TextView(this);
            label.setText("Home Screen Banner Blur: " + currentBlur + " px");
            label.setTextColor(Color.WHITE);
            label.setTextSize(13);
            label.setPadding(0, 0, 0, dp(10));
            layout.addView(label);

            android.widget.SeekBar seekBar = new android.widget.SeekBar(this);
            seekBar.setMax(30);
            seekBar.setProgress(currentBlur);
            seekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(android.widget.SeekBar sb, int progress, boolean fromUser) {
                    label.setText("Home Screen Banner Blur: " + progress + " px");
                    applyBannerBlur(findViewById(R.id.hero_banner), progress);
                }
                @Override public void onStartTrackingTouch(android.widget.SeekBar sb) {}
                @Override public void onStopTrackingTouch(android.widget.SeekBar sb) {}
            });
            layout.addView(seekBar);

            TextView note = new TextView(this);
            note.setText("Adjust from 0 px crystal clear up to 30 px cinematic soft blur");
            note.setTextColor(0xFF8E95B2);
            note.setTextSize(10.5f);
            note.setPadding(0, dp(8), 0, 0);
            layout.addView(note);

            new AlertDialog.Builder(this, R.style.RorDialogTheme)
                    .setTitle("Banner Blur Control")
                    .setView(layout)
                    .setPositiveButton("Save", (d, w) -> {
                        sp.edit().putInt("banner_blur_radius", seekBar.getProgress()).apply();
                        applyProfileAppearance(GsmProfileManager.selectedProfile(this));
                        toast("Banner blur set to " + seekBar.getProgress() + " px");
                    })
                    .setNegativeButton("Cancel", (d, w) -> {
                        applyBannerBlur(findViewById(R.id.hero_banner), currentBlur);
                    })
                    .show();
        } catch (Exception ignored) {}
    }

    public void openCustomGalleryPicker() {
        try {
            bannerPickerLauncher.launch("image/*");
        } catch (Exception e) {
            toast("Cannot open file picker: " + e.getMessage());
        }
    }

    public void showBannerAnimationStyleDialog() {
        if (isFinishing() || isDestroyed()) return;
        try {
            Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            float density = getResources().getDisplayMetrics().density;
            int dp6 = (int) (6 * density);
            int dp8 = (int) (8 * density);
            int dp10 = (int) (10 * density);
            int dp12 = (int) (12 * density);
            int dp16 = (int) (16 * density);

            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundColor(0xFF070B14);
            root.setPadding(dp16, dp16, dp16, dp16);

            // Header
            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setPadding(0, 0, 0, dp12);

            LinearLayout titleCol = new LinearLayout(this);
            titleCol.setOrientation(LinearLayout.VERTICAL);

            TextView title = new TextView(this);
            title.setText("BANNER ANIMATION STYLES (10 FX)");
            title.setTextSize(14.5f);
            title.setTextColor(Color.WHITE);
            title.setTypeface(null, Typeface.BOLD);
            title.setLetterSpacing(0.06f);
            titleCol.addView(title);

            TextView subtitle = new TextView(this);
            subtitle.setText("Hardware accelerated 60/120fps procedural shaders & particle systems");
            subtitle.setTextSize(10.5f);
            subtitle.setTextColor(0xFF8E95B2);
            titleCol.addView(subtitle);

            header.addView(titleCol, new LinearLayout.LayoutParams(0, -2, 1f));

            ImageView closeBtn = new ImageView(this);
            int closeSize = (int) (26 * density);
            LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(closeSize, closeSize);
            closeBtn.setLayoutParams(closeLp);
            closeBtn.setImageResource(R.drawable.ic_cancel);
            closeBtn.setColorFilter(0xFFB0B5C6);
            closeBtn.setPadding(dp6 / 2, dp6 / 2, dp6 / 2, dp6 / 2);
            closeBtn.setOnClickListener(v -> {
                try { if (dialog.isShowing()) dialog.dismiss(); } catch (Exception ignored) {}
            });
            header.addView(closeBtn);
            root.addView(header);

            // Intensity Selector Row (Subtle 0.7x | Balanced 1.0x | Turbo 1.4x)
            LinearLayout intensityRow = new LinearLayout(this);
            intensityRow.setOrientation(LinearLayout.HORIZONTAL);
            intensityRow.setGravity(Gravity.CENTER_VERTICAL);
            intensityRow.setPadding(0, 0, 0, dp12);

            TextView intensityLabel = new TextView(this);
            intensityLabel.setText("Intensity:");
            intensityLabel.setTextColor(0xFF00FFE5);
            intensityLabel.setTextSize(11f);
            intensityLabel.setTypeface(null, Typeface.BOLD);
            intensityLabel.setPadding(0, 0, dp8, 0);
            intensityRow.addView(intensityLabel);

            SharedPreferences themeSp = getSharedPreferences("ror_theme", MODE_PRIVATE);
            float curIntensity = themeSp.getFloat("banner_animation_intensity", 1.0f);
            final float[] selectedIntensity = new float[]{curIntensity};
            float[] intensityVals = new float[]{0.7f, 1.0f, 1.4f};
            String[] intensityTitles = new String[]{"Subtle (0.7x)", "Balanced (1.0x)", "Turbo (1.4x)"};
            TextView[] intensityChips = new TextView[3];

            Runnable updateIntensityChips = () -> {
                for (int i = 0; i < 3; i++) {
                    boolean sel = Math.abs(selectedIntensity[0] - intensityVals[i]) < 0.1f;
                    GradientDrawable bg = new GradientDrawable();
                    bg.setColor(sel ? 0x3300FFE5 : 0x1A1C2436);
                    bg.setCornerRadius(dp16);
                    bg.setStroke(1, sel ? 0xFF00FFE5 : 0x334E5D78);
                    intensityChips[i].setBackground(bg);
                    intensityChips[i].setTextColor(sel ? 0xFF00FFE5 : 0xFF8E95B2);
                }
            };

            for (int i = 0; i < 3; i++) {
                final float val = intensityVals[i];
                final String labelText = intensityTitles[i];
                TextView chip = new TextView(this);
                chip.setText(labelText);
                chip.setTextSize(10.5f);
                chip.setTypeface(null, Typeface.BOLD);
                chip.setGravity(Gravity.CENTER);
                chip.setPadding(dp10, dp6, dp10, dp6);
                chip.setOnClickListener(v -> {
                    try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
                    selectedIntensity[0] = val;
                    themeSp.edit().putFloat("banner_animation_intensity", val).apply();
                    updateIntensityChips.run();
                    ImageView hb = findViewById(R.id.hero_banner);
                    if (hb instanceof com.revror.kernelmanager.views.AnimatedHomeBannerView) {
                        ((com.revror.kernelmanager.views.AnimatedHomeBannerView) hb).setAnimationIntensity(val);
                    }
                    toast("Animation intensity: " + labelText);
                });
                intensityChips[i] = chip;
                LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(0, -2, 1f);
                if (i > 0) chipLp.leftMargin = dp6;
                intensityRow.addView(chip, chipLp);
            }
            updateIntensityChips.run();
            root.addView(intensityRow);

            // Scrollable list of 10 styles
            ScrollView scroll = new ScrollView(this);
            scroll.setVerticalScrollBarEnabled(false);
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);

            int currentStyle = themeSp.getInt("banner_animation_style",
                    com.revror.kernelmanager.views.AnimatedHomeBannerView.STYLE_CYBER_SYMPHONY);
            final int[] selectedStyle = new int[]{currentStyle};

            final int[] accentColors = {
                    0xFF00E5FF, // Cyber Glitch
                    0xFFB388FF, // Cosmic Aurora
                    0xFFFF6D00, // Solar Magma
                    0xFFFF007F, // Synthwave Neon
                    0xFFFF80AB, // Sakura Blossom
                    0xFF00E5FF, // Lightning Storm
                    0xFF80D8FF, // Warp Speed
                    0xFF00E676, // Quantum Hex
                    0xFFE040FB, // Neon Bokeh
                    0xFFFF1744  // Ultimate Cyber Symphony
            };

            Runnable[] rebuild = new Runnable[1];
            rebuild[0] = () -> {
                list.removeAllViews();
                for (int i = 0; i < com.revror.kernelmanager.views.AnimatedHomeBannerView.STYLE_NAMES.length; i++) {
                    final int idx = i;
                    boolean isSel = (selectedStyle[0] == idx);
                    int accent = accentColors[idx % accentColors.length];

                    LinearLayout card = new LinearLayout(this);
                    card.setOrientation(LinearLayout.HORIZONTAL);
                    card.setGravity(Gravity.CENTER_VERTICAL);
                    card.setPadding(dp12, dp10, dp12, dp10);

                    GradientDrawable cardBg = new GradientDrawable();
                    cardBg.setColor(isSel ? 0x2400E5FF : 0x121A2234);
                    cardBg.setCornerRadius(dp10);
                    cardBg.setStroke(isSel ? 2 : 1, isSel ? accent : 0x2A3E4E68);
                    card.setBackground(cardBg);

                    // Color indicator circle / badge
                    View badge = new View(this);
                    GradientDrawable badgeBg = new GradientDrawable();
                    badgeBg.setShape(GradientDrawable.OVAL);
                    badgeBg.setColor(accent);
                    badge.setBackground(badgeBg);
                    LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams((int) (12 * density), (int) (12 * density));
                    badgeLp.rightMargin = dp12;
                    card.addView(badge, badgeLp);

                    // Text column
                    LinearLayout textCol = new LinearLayout(this);
                    textCol.setOrientation(LinearLayout.VERTICAL);

                    TextView nameTv = new TextView(this);
                    nameTv.setText(com.revror.kernelmanager.views.AnimatedHomeBannerView.STYLE_NAMES[idx]);
                    nameTv.setTextSize(13f);
                    nameTv.setTextColor(isSel ? Color.WHITE : 0xFFE0E6F0);
                    nameTv.setTypeface(null, Typeface.BOLD);
                    textCol.addView(nameTv);

                    TextView descTv = new TextView(this);
                    descTv.setText(com.revror.kernelmanager.views.AnimatedHomeBannerView.STYLE_DESCRIPTIONS[idx]);
                    descTv.setTextSize(10f);
                    descTv.setTextColor(isSel ? accent : 0xFF8E95B2);
                    descTv.setPadding(0, (int) (2 * density), 0, 0);
                    textCol.addView(descTv);

                    card.addView(textCol, new LinearLayout.LayoutParams(0, -2, 1f));

                    // Status Pill / Checkmark
                    if (isSel) {
                        TextView activePill = new TextView(this);
                        activePill.setText("ACTIVE");
                        activePill.setTextSize(9.5f);
                        activePill.setTextColor(accent);
                        activePill.setTypeface(null, Typeface.BOLD);
                        activePill.setPadding(dp8, dp6 / 2, dp8, dp6 / 2);
                        GradientDrawable pillBg = new GradientDrawable();
                        pillBg.setColor(0x2A000000);
                        pillBg.setCornerRadius(dp8);
                        pillBg.setStroke(1, accent);
                        activePill.setBackground(pillBg);
                        card.addView(activePill);
                    }

                    card.setOnClickListener(v -> {
                        try { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); } catch (Exception ignored) {}
                        selectedStyle[0] = idx;
                        themeSp.edit().putInt("banner_animation_style", idx).putBoolean("animate_home_banner", true).apply();
                        ImageView hb = findViewById(R.id.hero_banner);
                        if (hb instanceof com.revror.kernelmanager.views.AnimatedHomeBannerView) {
                            com.revror.kernelmanager.views.AnimatedHomeBannerView ahb =
                                    (com.revror.kernelmanager.views.AnimatedHomeBannerView) hb;
                            ahb.setAnimationStyle(idx);
                            ahb.setFrameAnimationEnabled(true);
                        }
                        rebuild[0].run();
                        toast(com.revror.kernelmanager.views.AnimatedHomeBannerView.STYLE_NAMES[idx] + " applied");
                    });

                    LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
                    cardLp.bottomMargin = dp8;
                    list.addView(card, cardLp);
                }
            };
            rebuild[0].run();

            scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
            root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

            dialog.setContentView(root);
            dialog.show();
        } catch (Exception ignored) {}
    }

    private void showButtonConfigDialog() {
        SharedPreferences sp = getSharedPreferences(PowerButtonGestureService.PREFS_NAME, MODE_PRIVATE);
        boolean doubleTap = sp.getBoolean(PowerButtonGestureService.KEY_DOUBLE_TAP, true);
        boolean tripleTap = sp.getBoolean(PowerButtonGestureService.KEY_TRIPLE_TAP, true);
        boolean serviceActive = sp.getBoolean(PowerButtonGestureService.KEY_SERVICE_ENABLED, true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(16), dp(20), dp(16));

        TextView desc = new TextView(this);
        desc.setText("Configure physical buttons and power key hardware multi-tap shortcuts to instantly trigger ROR engines from anywhere, even while screen is off or in any app.");
        desc.setTextColor(0xFFB0B8D0);
        desc.setTextSize(12.5f);
        desc.setPadding(0, 0, 0, dp(14));
        root.addView(desc);

        // Master Service Switch
        SwitchMaterial masterSw = new SwitchMaterial(this);
        masterSw.setText("Power Key Gesture Engine");
        masterSw.setTextColor(Color.WHITE);
        masterSw.setTextSize(14f);
        masterSw.setTypeface(null, Typeface.BOLD);
        masterSw.setChecked(serviceActive);
        masterSw.setPadding(0, dp(6), 0, dp(10));
        root.addView(masterSw);

        // Double Tap Option
        CheckBox doubleCb = new CheckBox(this);
        doubleCb.setText("2x Tap Power Button -> Open ROR App (Overrides Camera)");
        doubleCb.setTextColor(0xFF00E5FF);
        doubleCb.setTextSize(13f);
        doubleCb.setTypeface(null, Typeface.BOLD);
        doubleCb.setChecked(doubleTap);
        doubleCb.setPadding(0, dp(4), 0, dp(6));
        root.addView(doubleCb);

        TextView doubleSub = new TextView(this);
        doubleSub.setText("Rapidly tap power button twice to launch ROR Dashboard with tactile haptic pulse. Automatically overrides and disables system camera double-tap shortcut so ROR takes its place.");
        doubleSub.setTextColor(0xFF8E95AF);
        doubleSub.setTextSize(11f);
        doubleSub.setPadding(dp(32), 0, 0, dp(10));
        root.addView(doubleSub);

        // Triple Tap Option
        CheckBox tripleCb = new CheckBox(this);
        tripleCb.setText("3x Tap & Hold Power Button -> Open Game Space");
        tripleCb.setTextColor(0xFFFF3366);
        tripleCb.setTextSize(13f);
        tripleCb.setTypeface(null, Typeface.BOLD);
        tripleCb.setChecked(tripleTap);
        tripleCb.setPadding(0, dp(4), 0, dp(6));
        root.addView(tripleCb);

        TextView tripleSub = new TextView(this);
        tripleSub.setText("Rapidly tap power button three times (or 3x tap and hold) to launch ROR Game Space & activate turbo overclock.");
        tripleSub.setTextColor(0xFF8E95AF);
        tripleSub.setTextSize(11f);
        tripleSub.setPadding(dp(32), 0, 0, dp(14));
        root.addView(tripleSub);

        // Test buttons row
        LinearLayout testRow = new LinearLayout(this);
        testRow.setOrientation(LinearLayout.HORIZONTAL);
        testRow.setPadding(0, dp(6), 0, dp(10));

        Button test2x = new Button(this);
        test2x.setText("Test 2x (ROR)");
        test2x.setTextSize(11f);
        test2x.setTextColor(0xFF00E5FF);
        test2x.setBackground(createButtonBg(0xFF161F38, 0xFF00E5FF));
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, dp(38), 1f);
        lp2.setMarginEnd(dp(4));
        test2x.setLayoutParams(lp2);
        test2x.setOnClickListener(v -> {
            toast("Testing Double-Tap: Opening ROR");
            PowerButtonGestureService.launchRorApp(DashboardActivity.this);
        });
        testRow.addView(test2x);

        Button test3x = new Button(this);
        test3x.setText("Test 3x (Game Space)");
        test3x.setTextSize(11f);
        test3x.setTextColor(0xFFFF3366);
        test3x.setBackground(createButtonBg(0xFF381622, 0xFFFF3366));
        LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(0, dp(38), 1f);
        lp3.setMarginStart(dp(4));
        test3x.setLayoutParams(lp3);
        test3x.setOnClickListener(v -> {
            toast("Testing Triple-Tap: Opening Game Space");
            PowerButtonGestureService.launchGameSpace(DashboardActivity.this);
        });
        testRow.addView(test3x);
        root.addView(testRow);

        // System Settings Option
        TextView sysBtn = new TextView(this);
        sysBtn.setText("Open Android System Key & Gesture Settings ->");
        sysBtn.setTextColor(0xFFC77DFF);
        sysBtn.setTextSize(11.5f);
        sysBtn.setGravity(Gravity.CENTER);
        sysBtn.setPadding(0, dp(8), 0, dp(4));
        sysBtn.setOnClickListener(v -> openSystemSetting(Settings.ACTION_SETTINGS, "Opening Android Settings"));
        root.addView(sysBtn);

        new AlertDialog.Builder(this, R.style.RorDialogTheme)
                .setTitle("Button & Key Configuration")
                .setView(root)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save Preferences", (d, w) -> {
                    sp.edit()
                            .putBoolean(PowerButtonGestureService.KEY_SERVICE_ENABLED, masterSw.isChecked())
                            .putBoolean(PowerButtonGestureService.KEY_DOUBLE_TAP, doubleCb.isChecked())
                            .putBoolean(PowerButtonGestureService.KEY_TRIPLE_TAP, tripleCb.isChecked())
                            .apply();
                    if (masterSw.isChecked()) {
                        PowerButtonGestureService.startIfEnabled(DashboardActivity.this);
                        if (doubleCb.isChecked()) {
                            PowerButtonGestureService.disableSystemCameraGesture(DashboardActivity.this);
                        }
                        toast("Button gestures enabled & camera override active");
                    } else {
                        try { stopService(new Intent(DashboardActivity.this, PowerButtonGestureService.class)); } catch (Exception ignored) {}
                        toast("Button gestures disabled");
                    }
                })
                .show();
    }

    private void showBugReportDialog() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(16), dp(20), dp(16));

        TextView header = new TextView(this);
        header.setText("Encountered an issue or bug? Choose your preferred reporting channel:");
        header.setTextColor(0xFFCBD5E1);
        header.setTextSize(12.5f);
        header.setPadding(0, 0, 0, dp(14));
        root.addView(header);

        // Option 1: GitHub Issues
        LinearLayout gitCard = new LinearLayout(this);
        gitCard.setOrientation(LinearLayout.VERTICAL);
        gitCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        gitCard.setBackground(createButtonBg(0xEE161A28, 0xFF00E5FF));
        LinearLayout.LayoutParams gitLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gitLp.bottomMargin = dp(12);
        gitCard.setLayoutParams(gitLp);
        gitCard.setClickable(true);
        gitCard.setFocusable(true);

        TextView gitTitle = new TextView(this);
        gitTitle.setText("1. Report on GitHub Issues Page");
        gitTitle.setTextSize(14f);
        gitTitle.setTextColor(0xFF00E5FF);
        gitTitle.setTypeface(null, Typeface.BOLD);
        gitCard.addView(gitTitle);

        TextView gitSub = new TextView(this);
        gitSub.setText("Opens official REVROR GitHub issue tracker (github.com/revws/ror/issues) to submit detailed reproduction steps, logs, and system info.");
        gitSub.setTextSize(11f);
        gitSub.setTextColor(0xFF94A3B8);
        gitSub.setPadding(0, dp(4), 0, 0);
        gitCard.addView(gitSub);

        root.addView(gitCard);

        // Option 2: Telegram (@godspeedmode)
        LinearLayout tgCard = new LinearLayout(this);
        tgCard.setOrientation(LinearLayout.VERTICAL);
        tgCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        tgCard.setBackground(createButtonBg(0xEE251220, 0xFFFF3366));
        LinearLayout.LayoutParams tgLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tgCard.setLayoutParams(tgLp);
        tgCard.setClickable(true);
        tgCard.setFocusable(true);

        TextView tgTitle = new TextView(this);
        tgTitle.setText("2. Report in Telegram (@godspeedmode)");
        tgTitle.setTextSize(14f);
        tgTitle.setTextColor(0xFFFF3366);
        tgTitle.setTypeface(null, Typeface.BOLD);
        tgCard.addView(tgTitle);

        TextView tgSub = new TextView(this);
        tgSub.setText("Select to open @godspeedmode official Telegram channel & discussion group for instant support and live developer feedback.");
        tgSub.setTextSize(11f);
        tgSub.setTextColor(0xFF94A3B8);
        tgSub.setPadding(0, dp(4), 0, 0);
        tgCard.addView(tgSub);

        root.addView(tgCard);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.RorDialogTheme)
                .setTitle("Bug Report & Community")
                .setView(root)
                .setNegativeButton("Close", null)
                .create();

        gitCard.setOnClickListener(v -> {
            dialog.dismiss();
            toast("Opening GitHub Issues Tracker");
            openUrl("https://github.com/revws/ror/issues");
        });

        tgCard.setOnClickListener(v -> {
            dialog.dismiss();
            toast("Opening @godspeedmode on Telegram");
            openUrl("https://t.me/godspeedmode");
        });

        dialog.show();
    }

    private void showFeedbackSettings() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(14), dp(22), dp(14));

        TextView info = new TextView(this);
        info.setText("Configure automated tuning feedback, logs, and issue diagnostics.");
        info.setTextColor(0xFFB0B8D0);
        info.setTextSize(12f);
        info.setPadding(0, 0, 0, dp(12));
        box.addView(info);

        CheckBox notifications = new CheckBox(this);
        notifications.setText("Profile Change Notifications");
        notifications.setTextColor(Color.WHITE);
        notifications.setTextSize(13.5f);
        notifications.setChecked(RorFeedback.notifications(this));
        notifications.setPadding(0, dp(4), 0, dp(4));
        box.addView(notifications);

        TextView notifSub = new TextView(this);
        notifSub.setText("Display status heads-up notification when automated profile tuning finishes.");
        notifSub.setTextColor(0xFF8E95AF);
        notifSub.setTextSize(11f);
        notifSub.setPadding(dp(30), 0, 0, dp(10));
        box.addView(notifSub);

        CheckBox vibration = new CheckBox(this);
        vibration.setText("Tactile Haptic Confirmation");
        vibration.setTextColor(Color.WHITE);
        vibration.setTextSize(13.5f);
        vibration.setChecked(RorFeedback.vibration(this));
        vibration.setPadding(0, dp(4), 0, dp(4));
        box.addView(vibration);

        TextView vibSub = new TextView(this);
        vibSub.setText("Provide subtle vibration pulse when hardware profiles are engaged.");
        vibSub.setTextColor(0xFF8E95AF);
        vibSub.setTextSize(11f);
        vibSub.setPadding(dp(30), 0, 0, dp(14));
        box.addView(vibSub);

        // Action Buttons Row
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, dp(4), 0, dp(4));

        Button viewLogsBtn = new Button(this);
        viewLogsBtn.setText("Terminal Logs");
        viewLogsBtn.setTextColor(0xFF00E5FF);
        viewLogsBtn.setTextSize(11f);
        viewLogsBtn.setBackground(createButtonBg(0xFF161F38, 0xFF00E5FF));
        LinearLayout.LayoutParams lpLog = new LinearLayout.LayoutParams(0, dp(38), 1f);
        lpLog.setMarginEnd(dp(4));
        viewLogsBtn.setLayoutParams(lpLog);
        viewLogsBtn.setOnClickListener(v -> showProfileLogs());
        btnRow.addView(viewLogsBtn);

        Button bugReportBtn = new Button(this);
        bugReportBtn.setText("Report Bug");
        bugReportBtn.setTextColor(0xFFFF3366);
        bugReportBtn.setTextSize(11f);
        bugReportBtn.setBackground(createButtonBg(0xFF381622, 0xFFFF3366));
        LinearLayout.LayoutParams lpBug = new LinearLayout.LayoutParams(0, dp(38), 1f);
        lpBug.setMarginStart(dp(4));
        bugReportBtn.setLayoutParams(lpBug);
        bugReportBtn.setOnClickListener(v -> showBugReportDialog());
        btnRow.addView(bugReportBtn);

        box.addView(btnRow);

        new AlertDialog.Builder(this, R.style.RorDialogTheme)
                .setTitle("Feedback, Logs & Diagnostics")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save Preferences", (d, w) -> {
                    RorFeedback.set(this, notifications.isChecked(), vibration.isChecked());
                    if (notifications.isChecked() && Build.VERSION.SDK_INT >= 33)
                        SilentPermissionHelper.grantAllSilent(this);
                    toast("Feedback preferences saved");
                })
                .show();
    }

    private void showProfileLogs() {
        String logContent = RorFeedback.read(this);
        if (logContent == null || logContent.trim().isEmpty()) {
            logContent = "--- [REVROR RUNTIME LOG] ---\n[INFO] No recorded profile events yet.\n[READY] Engage profiles or game optimizations to generate execution telemetry.";
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));

        // Terminal frame
        FrameLayout termFrame = new FrameLayout(this);
        GradientDrawable termBg = new GradientDrawable();
        termBg.setColor(0xFF07090E);
        termBg.setCornerRadius(dp(10));
        termBg.setStroke(dp(1), 0xFF2A3048);
        termFrame.setBackground(termBg);
        termFrame.setPadding(dp(12), dp(10), dp(12), dp(10));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        TextView logView = new TextView(this);
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setTextSize(11f);
        logView.setTextColor(0xFFE2E8F0);
        logView.setLineSpacing(dp(2), 1f);
        logView.setTextIsSelectable(true);

        // Highlight syntax
        SpannableStringBuilder ssb = formatLogText(logContent);
        logView.setText(ssb);

        scroll.addView(logView, new FrameLayout.LayoutParams(-1, -2));
        termFrame.addView(scroll, new LinearLayout.LayoutParams(-1, dp(340)));
        root.addView(termFrame);

        final String finalLogs = logContent;
        new AlertDialog.Builder(this, R.style.RorDialogTheme)
                .setTitle("Profile Execution Logs")
                .setView(root)
                .setNeutralButton("Copy Logs", (d, w) -> {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(ClipData.newPlainText("REVROR Logs", finalLogs));
                        toast("Logs copied to clipboard");
                    }
                })
                .setNegativeButton("Clear", (d, w) -> {
                    RorFeedback.clear(this);
                    toast("Logs cleared");
                })
                .setPositiveButton("Close", null)
                .show();
    }

    private SpannableStringBuilder formatLogText(String raw) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        for (String line : raw.split("\n")) {
            int start = ssb.length();
            ssb.append(line).append("\n");
            int end = ssb.length();
            int color = 0xFFCBD5E1;
            if (line.contains("Success") || line.contains("applied") || line.contains("completed") || line.contains("ACTIVE")) {
                color = 0xFF00E676; // Bright Green
            } else if (line.contains("Error") || line.contains("Failed") || line.contains("FAIL") || line.contains("ALERT")) {
                color = 0xFFFF5252; // Neon Red
            } else if (line.contains("INFO") || line.contains("READY") || line.contains("Starting") || line.contains("Profile")) {
                color = 0xFF00E5FF; // Electric Cyan
            } else if (line.contains("WARN") || line.contains("requires") || line.contains("Caution")) {
                color = 0xFFFFD600; // Gold Yellow
            }
            ssb.setSpan(new ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return ssb;
    }

    private GradientDrawable createButtonBg(int bgColor, int strokeColor) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(dp(8));
        gd.setStroke(dp(1), strokeColor);
        return gd;
    }

    private String profileName(int profile) {
        return GsmProfileManager.getProfileName(this, profile);
    }

    private void showControlCenter() {
        class SettingItem {
            final int iconRes;
            final int tintColor;
            final String title;
            final String subtitle;
            final Runnable action;
            SettingItem(int iconRes, int tintColor, String title, String subtitle, Runnable action) {
                this.iconRes = iconRes;
                this.tintColor = tintColor;
                this.title = title;
                this.subtitle = subtitle;
                this.action = action;
            }
        }

        List<SettingItem> items = new ArrayList<>();
        items.add(new SettingItem(
                R.drawable.ic_shield, 0xFF00E5FF,
                rootReady ? "Root Engine Active" : shizukuReady ? "Shizuku Engine Active" : "Enable Root or Shizuku",
                rootReady ? "Kernel and system profile engine enabled" : shizukuReady ? "Android shell profiles & app controls active" : "Required for system optimization",
                () -> { if (rootReady) showRootStatus(); else if (shizukuReady) showShizukuStatus(); else showRootAccessDialog(); }
        ));
        items.add(new SettingItem(
                R.drawable.ic_people, 0xFFC77DFF,
                "Developer Profile",
                "revWhiteshadow • github.com/revws",
                () -> openUrl("https://github.com/revws")
        ));
        items.add(new SettingItem(
                R.drawable.ic_telegram, 0xFF29B6F6,
                "Telegram Community",
                "Official channel & updates • @godTspeed",
                () -> openUrl("https://t.me/godTspeed")
        ));
        items.add(new SettingItem(
                R.drawable.ic_flash, 0xFF00F5D4,
                "Charging Cooldown (Fast & Cool)",
                "Intelligent fast charging & low-heat thermal management",
                () -> com.revror.kernelmanager.views.dialog.ChargingCooldownDialog.show(this)
        ));
        items.add(new SettingItem(
                R.drawable.ic_spectrum_battery, 0xFF2DE49A,
                "Battery Saver",
                "System battery optimization and doze modes",
                this::openBatterySaver
        ));
        items.add(new SettingItem(
                R.drawable.ic_display, 0xFFFFD166,
                "Display & Refresh Rate",
                "Display refresh rate and smooth motion controls",
                this::showRefreshRate
        ));
        items.add(new SettingItem(
                R.drawable.ic_layers, 0xFFA865FF,
                "App Management",
                "Manage background user apps and memory footprint",
                this::showAppManager
        ));
        items.add(new SettingItem(
                R.drawable.ic_flash, 0xFF00B4D8,
                "Network Controls",
                "Low latency TCP networking and socket optimization",
                this::showNetworkBoost
        ));
        items.add(new SettingItem(
                R.drawable.ic_cpu, 0xFF368FFF,
                "CPU & Memory Status",
                "Hardware topology, core frequencies, and RAM info",
                this::showCpuStatus
        ));
        items.add(new SettingItem(
                rootReady ? R.drawable.ic_tools : R.drawable.ic_lock, rootReady ? 0xFFFF315C : 0xFFFA8072,
                rootReady ? "Kernel Manager" : "Kernel Manager",
                rootReady ? "Direct sysfs governor, memory compaction, I/O" : "Kernel sysfs tuning • Root Required",
                this::showKernelManager
        ));
        items.add(new SettingItem(
                R.drawable.ic_info, 0xFF06D6A0,
                "About REVROR",
                "Revolutionary Open Root • Version & notices",
                this::showAbout
        ));

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout listLayout = new LinearLayout(this);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        listLayout.setPadding(0, dp(6), 0, dp(10));

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.RorDialogTheme)
                .setTitle("REVROR Settings & Control Center")
                .setView(scroll)
                .setNegativeButton("Close", null)
                .create();

        for (SettingItem it : items) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_settings_control, listLayout, false);
            ImageView iv = itemView.findViewById(R.id.setting_icon);
            TextView tvTitle = itemView.findViewById(R.id.setting_title);
            TextView tvSub = itemView.findViewById(R.id.setting_subtitle);

            iv.setImageResource(it.iconRes);
            iv.setColorFilter(it.tintColor);
            tvTitle.setText(it.title);
            tvSub.setText(it.subtitle);

            itemView.setOnClickListener(v -> {
                dialog.dismiss();
                it.action.run();
            });
            listLayout.addView(itemView);
        }

        scroll.addView(listLayout);
        dialog.show();
    }

    private void showRootLockedDialog(String featureName) {
        new AlertDialog.Builder(this, R.style.RorDialogTheme)
                .setTitle("Root Required • " + featureName)
                .setMessage(featureName + " directly modifies Linux kernel sysfs and procfs parameters (/sys and /proc) which are strictly restricted to root privileges.\n\nShizuku operates with Android shell UID 2000 permissions and cannot write to protected kernel nodes.\n\nAll Android-level tweaks, Doze, Game Mode, Network latency, and Performance profiles are active and working with Shizuku.")
                .setPositiveButton("Understood", null)
                .setNegativeButton("Check Root", (d, w) -> showRootAccessDialog())
                .show();
    }

    private void showRootStatus() {
        new AlertDialog.Builder(this, R.style.RorDialogTheme).setTitle("Root access active")
                .setMessage("The built-in GSM profile service is enabled. Root is used only for a selected "
                        + "profile or kernel-level tool. No Magisk module is required. Disabling root here "
                        + "stops REVROR from using it and disables apply-on-boot.")
                .setNegativeButton("Disable in REVROR", (d, w) -> disableRootFeatures())
                .setPositiveButton("OK", null).show();
    }

    private void disableRootFeatures() {
        GsmProfileManager.disableRoot(this, (success, detail) -> {
            rootReady = false;
            setProfileAccess(false);
            profileStatus.setText(detail);
            profileStatus.setTextColor(Color.rgb(255, 184, 77));
            toast(detail);
        });
    }

    private void showShizukuStatus() {
        String summary = ShizukuBridge.getStatusSummary();
        int uid = ShizukuBridge.getUid();
        int ver = ShizukuBridge.getVersion();
        String selinux = ShizukuBridge.getSELinuxContext();
        String details = "Status: " + summary + "\n"
                + "Server UID: " + uid + (uid == 0 ? " (Root)" : uid == 2000 ? " (Shell)" : "") + "\n"
                + "Protocol Version: " + ver + "\n"
                + "SELinux Domain: " + selinux + "\n\n"
                + "Active Shizuku Capabilities:\n"
                + "• Android Performance Modes (cmd power set-mode)\n"
                + "• Network Policy & Background Restriction\n"
                + "• Window & Animation Scaling\n"
                + "• Refresh Rate & Display Tuning\n"
                + "• HW Overlay & Freeform Management\n"
                + "• Background Process Trimming & AppOps\n"
                + "• Non-destructive Charging Cooldown Supervision\n\n"
                + "(Note: Direct /sys and /proc kernel node tuning requires Root access).";

        new AlertDialog.Builder(this, R.style.RorDialogTheme)
                .setTitle("Shizuku Bridge Telemetry")
                .setMessage(details)
                .setNeutralButton("Test Shell", (d, w) -> {
                    submitTask(() -> {
                        ShizukuBridge.Result testRes = ShizukuBridge.run("id; getprop ro.product.model; getprop ro.build.version.release");
                        postToUi(() -> new AlertDialog.Builder(this, R.style.RorDialogTheme)
                                .setTitle(testRes.success ? "Shizuku Shell Active" : "Test Failed")
                                .setMessage(testRes.success ? "Command executed with UID " + uid + ":\n\n" + testRes.output : "Error: " + testRes.output)
                                .setPositiveButton("OK", null)
                                .show());
                    });
                })
                .setNegativeButton("Forget in REVROR", (d, w) -> {
                    shizukuReady = false;
                    setProfileAccess(rootReady);
                    profileStatus.setText("Shizuku disabled in this session");
                })
                .setPositiveButton("OK", null)
                .show();
    }

    private void showCpuStatus() {
        submitTask(() -> {
            String device = cpuStats.deviceDescription(rootReady);
            long current = cpuStats.readAverageCurrentKHz(rootReady);
            String frequency = current > 0 ? String.format(Locale.US, "%.2f GHz average current", current / 1_000_000d)
                    : "Current frequency unavailable";
            postToUi(() -> new AlertDialog.Builder(this, R.style.RorDialogTheme)
                    .setTitle("Device CPU information")
                    .setMessage(device + "\n" + frequency + "\n\n" + cpuText.getText() + "\n"
                            + ramText.getText() + "\n" + tempText.getText() + "\n" + storageText.getText())
                    .setPositiveButton("Done", null).show());
        });
        toast("Live statistics refresh every 3 seconds");
    }

    private void showThermalStatus() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_thermal_control, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.RorDialogTheme)
                .setView(view)
                .create();

        int savedLimit = getSharedPreferences("ror_tools", MODE_PRIVATE).getInt("thermal_limit", 45);
        if (savedLimit < 0) savedLimit = 45;

        TextView currentTempView = view.findViewById(R.id.thermal_current_temp);
        if (currentTempView != null && tempText != null) {
            currentTempView.setText("Current Device Temp: " + tempText.getText());
        }

        TextView display = view.findViewById(R.id.thermal_threshold_display);
        TextView status = view.findViewById(R.id.thermal_threshold_status);
        SeekBar seekbar = view.findViewById(R.id.thermal_seekbar);
        com.google.android.material.switchmaterial.SwitchMaterial guardSwitch = view.findViewById(R.id.thermal_guard_switch);
        LinearLayout presetsContainer = view.findViewById(R.id.thermal_presets_container);
        Button kernelBtn = view.findViewById(R.id.btn_thermal_kernel);
        Button cancelBtn = view.findViewById(R.id.btn_thermal_cancel);
        Button saveBtn = view.findViewById(R.id.btn_thermal_save);

        int initialLimit = savedLimit > 0 ? savedLimit : 45;
        final int[] currentLimit = new int[]{savedLimit};

        if (guardSwitch != null) {
            guardSwitch.setChecked(savedLimit > 0);
        }

        Runnable updateUi = () -> {
            boolean enabled = guardSwitch == null || guardSwitch.isChecked();
            if (seekbar != null) seekbar.setEnabled(enabled);
            int val = currentLimit[0];
            if (!enabled || val <= 0) {
                if (display != null) {
                    display.setText("OFF");
                    display.setTextColor(0xFF8E95B2);
                }
                if (status != null) {
                    status.setText("Automatic Cooldown Disabled");
                    status.setTextColor(0xFF8E95B2);
                }
            } else {
                if (display != null) {
                    display.setText(val + "C");
                    if (val <= 42) display.setTextColor(0xFF00F5D4);
                    else if (val <= 48) display.setTextColor(0xFFFF9E00);
                    else display.setTextColor(0xFFFF315C);
                }
                if (status != null) {
                    if (val <= 42) {
                        status.setText("Cool & Aggressive Guard");
                        status.setTextColor(0xFF00F5D4);
                    } else if (val <= 46) {
                        status.setText("Recommended Balanced Limit • 44–45C");
                        status.setTextColor(0xFF00F5D4);
                    } else if (val <= 50) {
                        status.setText("Heavy Gaming Threshold");
                        status.setTextColor(0xFFFF9E00);
                    } else {
                        status.setText("Extreme High Threshold");
                        status.setTextColor(0xFFFF315C);
                    }
                }
            }
        };

        if (seekbar != null) {
            int progress = Math.max(0, Math.min(22, initialLimit - 38));
            seekbar.setProgress(progress);
            seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        currentLimit[0] = 38 + progress;
                        if (guardSwitch != null && !guardSwitch.isChecked()) {
                            guardSwitch.setChecked(true);
                        }
                        updateUi.run();
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        if (guardSwitch != null) {
            guardSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isChecked) {
                    if (currentLimit[0] <= 0) {
                        currentLimit[0] = seekbar != null ? (38 + seekbar.getProgress()) : 45;
                    }
                } else {
                    currentLimit[0] = 0;
                }
                updateUi.run();
            });
        }

        // Quick Preset Pills: 40C, 42C, 44C, 45C, 48C, 50C
        int[] presetTemps = new int[]{40, 42, 44, 45, 48, 50};
        if (presetsContainer != null) {
            presetsContainer.removeAllViews();
            for (int t : presetTemps) {
                TextView pill = new TextView(this);
                pill.setText(t + "C" + (t == 45 ? " • REC" : ""));
                pill.setTextSize(10.5f);
                pill.setTypeface(null, Typeface.BOLD);
                pill.setTextColor(t == 45 ? 0xFFFFFFFF : 0xFFB8C0E0);
                pill.setPadding(dp(10), dp(6), dp(10), dp(6));

                GradientDrawable pillBg = new GradientDrawable();
                pillBg.setColor(t == 45 ? 0x44FF9E00 : 0x22182042);
                pillBg.setCornerRadius(dp(12));
                pillBg.setStroke(dp(1), t == 45 ? 0xFFFF9E00 : 0x334E5D94);
                pill.setBackground(pillBg);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
                lp.setMarginEnd(dp(6));
                pill.setLayoutParams(lp);

                pill.setOnClickListener(v -> {
                    currentLimit[0] = t;
                    if (seekbar != null) {
                        seekbar.setProgress(Math.max(0, Math.min(22, t - 38)));
                    }
                    if (guardSwitch != null && !guardSwitch.isChecked()) {
                        guardSwitch.setChecked(true);
                    }
                    updateUi.run();
                });
                presetsContainer.addView(pill);
            }
        }

        if (kernelBtn != null && rootReady) {
            kernelBtn.setVisibility(View.VISIBLE);
            kernelBtn.setOnClickListener(v -> {
                dialog.dismiss();
                openFullKernelManager(com.revror.kernelmanager.fragments.kernel.ThermalFragment.class.getCanonicalName());
            });
        }

        if (cancelBtn != null) {
            cancelBtn.setOnClickListener(v -> dialog.dismiss());
        }

        if (saveBtn != null) {
            saveBtn.setOnClickListener(v -> {
                boolean enabled = guardSwitch == null || guardSwitch.isChecked();
                int limitToSave = enabled ? currentLimit[0] : 0;
                if (limitToSave < 38 && limitToSave != 0) limitToSave = 45;

                getSharedPreferences("ror_tools", MODE_PRIVATE).edit().putInt("thermal_limit", limitToSave).apply();
                if (limitToSave > 0) {
                    toast("Thermal guard limit set to " + limitToSave + "C");
                } else {
                    toast("Thermal guard disabled");
                }
                dialog.dismiss();
            });
        }

        updateUi.run();
        dialog.show();
    }

    private long lastThermalTriggerTime = 0;

    private void triggerThermalEmergency(float currentTemp, int limit) {
        submitTask(() -> {
            Set<String> runningGames = com.revror.kernelmanager.utils.GameLauncherRegistry.getGamePackages(this);
            StringBuilder gameExclusions = new StringBuilder();
            for (String g : runningGames) {
                if (g != null && !g.trim().isEmpty()) {
                    gameExclusions.append("|*").append(g.trim().replace("*", "")).append("*");
                }
            }

            // Stops background user apps, while strictly preserving REVROR, Shizuku, games, and seated profile!
            String sweepCmd = "pm list packages -3 | cut -d: -f2 | while read p; do "
                    + "case \"$p\" in "
                    + GameOptimizer.buildCoolDownExclusionPattern(this) + ") ;; "
                    + "*) am force-stop \"$p\" 2>/dev/null ;; "
                    + "esac; done";

            if (rootReady) {
                com.revror.kernelmanager.utils.root.RootUtils.runCommand(sweepCmd);
            } else if (shizukuReady) {
                ShizukuBridge.run(sweepCmd);
            }

            postToUi(() -> {
                String msg = String.format(Locale.US, "Thermal limit hit %.1fC • Cooled device and protected active game and active profile", currentTemp);
                toast(msg);
                RorFeedback.append(this, msg);
            });
        });
    }

    private void showCleaner() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_auto_cleaner, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.RorDialogTheme)
                .setTitle("Auto Cleaner")
                .setView(dialogView)
                .show();

        TextView btnNow = dialogView.findViewById(R.id.btn_cleaner_now);
        TextView btnCancel = dialogView.findViewById(R.id.btn_cleaner_cancel);
        TextView status = dialogView.findViewById(R.id.cleaner_timer_status);

        int currentMinutes = getSharedPreferences("ror_tools", MODE_PRIVATE).getInt("cleanup_minutes", 0);
        updateCleanerTimerStatus(status, currentMinutes);

        int[] chipIds = {R.id.timer_chip_0, R.id.timer_chip_15, R.id.timer_chip_30, R.id.timer_chip_60, R.id.timer_chip_120};
        int[] minutes = {0, 15, 30, 60, 120};

        for (int i = 0; i < chipIds.length; i++) {
            final int m = minutes[i];
            final TextView chip = dialogView.findViewById(chipIds[i]);
            if (m == currentMinutes) {
                chip.setTextColor(0xFF00E5FF);
            }
            chip.setOnClickListener(v -> {
                for (int id : chipIds) {
                    ((TextView)dialogView.findViewById(id)).setTextColor(0xFF8892B0);
                }
                chip.setTextColor(0xFF00E5FF);
                scheduleCleanup(m);
                updateCleanerTimerStatus(status, m);
            });
        }

        btnNow.setOnClickListener(v -> {
            dialog.dismiss();
            submitTask(() -> {
                long before = directorySize(getCacheDir());
                deleteChildren(getCacheDir());
                if (rootReady) RootUtils.runCommand("sync; echo 3 > /proc/sys/vm/drop_caches");
                postToUi(() -> toast(String.format(Locale.US, "Cleaner finished • %.1f MB removed", before / 1048576d)));
            });
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    private void updateCleanerTimerStatus(TextView tv, int minutes) {
        if (minutes == 0) {
            tv.setText("Auto clean timer is currently OFF");
            tv.setTextColor(0xFF7A85A8);
        } else {
            tv.setText("Auto clean scheduled every " + minutes + " minutes");
            tv.setTextColor(0xFF00F5D4);
        }
    }

    private void showRorAiDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_ror_ai, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.RorDialogTheme)
                .setView(view)
                .create();
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90f);
            int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.78f);
            dialog.getWindow().setLayout(width, height);
        }

        SwitchMaterial sEngine = view.findViewById(R.id.switch_ai_engine);
        SwitchMaterial sGpu = view.findViewById(R.id.switch_ai_gpu);
        SwitchMaterial sRam = view.findViewById(R.id.switch_ai_ram);
        SwitchMaterial sFps = view.findViewById(R.id.switch_ai_fps);
        SwitchMaterial sIo = view.findViewById(R.id.switch_ai_io);
        SwitchMaterial sArt = view.findViewById(R.id.switch_ai_art);
        SwitchMaterial sNet = view.findViewById(R.id.switch_ai_net);
        SwitchMaterial sDoze = view.findViewById(R.id.switch_ai_doze);

        sEngine.setChecked(RorAiOptimizer.isAiEnabled(this));
        sGpu.setChecked(RorAiOptimizer.isGpuBoostEnabled(this));
        sRam.setChecked(RorAiOptimizer.isRamOptEnabled(this));
        sFps.setChecked(RorAiOptimizer.isFpsStabilizerEnabled(this));
        if (sIo != null) sIo.setChecked(RorAiOptimizer.isIoBoostEnabled(this));
        if (sArt != null) sArt.setChecked(RorAiOptimizer.isArtBoostEnabled(this));
        sNet.setChecked(RorAiOptimizer.isNetworkLowLatencyEnabled(this));
        sDoze.setChecked(RorAiOptimizer.isAggressiveDozeEnabled(this));

        sEngine.setOnCheckedChangeListener((b, isChecked) -> RorAiOptimizer.setAiEnabled(this, isChecked));
        sGpu.setOnCheckedChangeListener((b, isChecked) -> RorAiOptimizer.setGpuBoostEnabled(this, isChecked));
        sRam.setOnCheckedChangeListener((b, isChecked) -> RorAiOptimizer.setRamOptEnabled(this, isChecked));
        sFps.setOnCheckedChangeListener((b, isChecked) -> RorAiOptimizer.setFpsStabilizerEnabled(this, isChecked));
        if (sIo != null) sIo.setOnCheckedChangeListener((b, isChecked) -> RorAiOptimizer.setIoBoostEnabled(this, isChecked));
        if (sArt != null) sArt.setOnCheckedChangeListener((b, isChecked) -> RorAiOptimizer.setArtBoostEnabled(this, isChecked));
        sNet.setOnCheckedChangeListener((b, isChecked) -> RorAiOptimizer.setNetworkLowLatencyEnabled(this, isChecked));
        sDoze.setOnCheckedChangeListener((b, isChecked) -> RorAiOptimizer.setAggressiveDozeEnabled(this, isChecked));

        view.findViewById(R.id.btn_ror_cancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btn_ror_apply).setOnClickListener(v -> {
            dialog.dismiss();
            toast("Applying REVROR AI Smart Optimizations...");
            RorAiOptimizer.applyAll(this, (success, summary) -> postToUi(() -> toast(summary)));
        });
    }

    private void showAppManager() {
        startActivity(new Intent(this, AppManagerActivity.class));
    }

    private void showAppActions(String label, String pkg) {
        String[] actions = {"Force stop", "Disable app", "Enable app"};
        new AlertDialog.Builder(this, R.style.RorDialogTheme).setTitle(label)
                .setMessage(pkg + "\n\nThese actions run directly through REVROR using Root or Shizuku.")
                .setItems(actions, (d, which) -> {
                    if (which == 0) runPrivilegedTool("Force stop " + label, "am force-stop " + shellQuote(pkg), false);
                    else if (which == 1) runPrivilegedTool("Disable " + label, "pm disable-user --user 0 " + shellQuote(pkg), false);
                    else runPrivilegedTool("Enable " + label, "pm enable " + shellQuote(pkg), false);
                }).setNegativeButton("Close", null).show();
    }

    private void showNetworkBoost() {
        new AlertDialog.Builder(this, R.style.RorDialogTheme).setTitle("Network Boost")
                .setMessage("The low-latency preset enables supported Linux TCP fast-open and low-latency flags until reboot. Unsupported controls are skipped.")
                .setNegativeButton("Reset preset", (d, w) -> runRootTool("Network reset",
                        "[ -w /proc/sys/net/ipv4/tcp_low_latency ] && echo 0 > /proc/sys/net/ipv4/tcp_low_latency"))
                .setPositiveButton("Apply preset", (d, w) -> runRootTool("Network preset",
                        "[ -w /proc/sys/net/ipv4/tcp_fastopen ] && echo 3 > /proc/sys/net/ipv4/tcp_fastopen; "
                                + "[ -w /proc/sys/net/ipv4/tcp_low_latency ] && echo 1 > /proc/sys/net/ipv4/tcp_low_latency"))
                .show();
    }

    private void showBackgroundControls() {
        new AlertDialog.Builder(this, R.style.RorDialogTheme).setTitle("Background Apps")
                .setMessage("Review Android battery optimization, or launch Force Doze Controller. Force Doze cleans unneeded user background tasks according to your desired profile.")
                .setNegativeButton("Exit Doze", (d, w) -> com.revror.kernelmanager.utils.doze.ForceDozeManager.exitDoze(this))
                .setPositiveButton("Force Doze Controller", (d, w) -> com.revror.kernelmanager.views.dialog.ForceDozeDialog.show(this))
                .show();
    }

    private void showBatteryScheduler() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_battery_scheduler, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.RorDialogTheme)
                .setView(view)
                .create();

        SharedPreferences sp = getSharedPreferences("ror_tools", MODE_PRIVATE);
        boolean isSchedEnabled = sp.getBoolean("schedule_enabled", false);
        int initialHour = sp.getInt("schedule_hour", 23);
        int initialMinute = sp.getInt("schedule_minute", 0);

        final int[] selHour = {initialHour};
        final int[] selMinute = {initialMinute};

        TextView statusText = view.findViewById(R.id.sched_status_text);
        SwitchMaterial enableSwitch = view.findViewById(R.id.sched_enable_switch);
        TextView timeDisplay = view.findViewById(R.id.sched_time_display);
        TextView timeSubtitle = view.findViewById(R.id.sched_time_subtitle);
        LinearLayout presetsContainer = view.findViewById(R.id.sched_presets_container);
        TimePicker timePicker = view.findViewById(R.id.sched_time_picker);
        Button btnSystemSaver = view.findViewById(R.id.btn_sched_system_saver);
        Button btnCancel = view.findViewById(R.id.btn_sched_cancel);
        Button btnApply = view.findViewById(R.id.btn_sched_apply);

        if (enableSwitch != null) {
            enableSwitch.setChecked(isSchedEnabled);
        }

        Runnable updateDisplay = () -> {
            boolean active = enableSwitch == null || enableSwitch.isChecked();
            if (timeDisplay != null) {
                timeDisplay.setText(String.format(Locale.US, "%02d:%02d", selHour[0], selMinute[0]));
                timeDisplay.setTextColor(active ? 0xFF2DE49A : 0xFF7A85A8);
            }
            int h12 = selHour[0] % 12;
            if (h12 == 0) h12 = 12;
            String ampm = selHour[0] >= 12 ? "PM" : "AM";
            if (timeSubtitle != null) {
                timeSubtitle.setText(String.format(Locale.US, "Overnight Battery Saver • %d:%02d %s", h12, selMinute[0], ampm));
            }
            if (statusText != null) {
                if (active) {
                    statusText.setText(String.format(Locale.US, "Active: %02d:%02d Daily • Ultra Battery", selHour[0], selMinute[0]));
                    statusText.setTextColor(0xFF2DE49A);
                } else {
                    statusText.setText("Schedule Disabled • Toggle switch to activate");
                    statusText.setTextColor(0xFF8E95B2);
                }
            }
        };

        if (timePicker != null) {
            timePicker.setIs24HourView(true);
            if (Build.VERSION.SDK_INT >= 23) {
                timePicker.setHour(selHour[0]);
                timePicker.setMinute(selMinute[0]);
            } else {
                timePicker.setCurrentHour(selHour[0]);
                timePicker.setCurrentMinute(selMinute[0]);
            }
            timePicker.setOnTimeChangedListener((v, h, m) -> {
                selHour[0] = h;
                selMinute[0] = m;
                if (enableSwitch != null && !enableSwitch.isChecked()) {
                    enableSwitch.setChecked(true);
                }
                updateDisplay.run();
            });
        }

        if (enableSwitch != null) {
            enableSwitch.setOnCheckedChangeListener((b, isChecked) -> updateDisplay.run());
        }

        int[][] presets = new int[][]{{22, 0}, {23, 0}, {0, 0}, {1, 0}};
        String[] presetLabels = new String[]{"22:00 • 10 PM", "23:00 • 11 PM (REC)", "00:00 • Midnight", "01:00 • 1 AM"};

        if (presetsContainer != null) {
            presetsContainer.removeAllViews();
            for (int i = 0; i < presets.length; i++) {
                final int h = presets[i][0];
                final int m = presets[i][1];
                TextView pill = new TextView(this);
                pill.setText(presetLabels[i]);
                pill.setTextSize(10.5f);
                pill.setTypeface(null, Typeface.BOLD);
                pill.setTextColor(0xFFB8C0E0);
                pill.setPadding(dp(12), dp(6), dp(12), dp(6));

                GradientDrawable pillBg = new GradientDrawable();
                pillBg.setColor(0x22182042);
                pillBg.setCornerRadius(dp(12));
                pillBg.setStroke(dp(1), 0x334E5D94);
                pill.setBackground(pillBg);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
                lp.setMarginEnd(dp(8));
                pill.setLayoutParams(lp);

                pill.setOnClickListener(v -> {
                    selHour[0] = h;
                    selMinute[0] = m;
                    if (timePicker != null) {
                        if (Build.VERSION.SDK_INT >= 23) {
                            timePicker.setHour(h);
                            timePicker.setMinute(m);
                        } else {
                            timePicker.setCurrentHour(h);
                            timePicker.setCurrentMinute(m);
                        }
                    }
                    if (enableSwitch != null && !enableSwitch.isChecked()) {
                        enableSwitch.setChecked(true);
                    }
                    updateDisplay.run();
                });
                presetsContainer.addView(pill);
            }
        }

        if (btnSystemSaver != null) {
            btnSystemSaver.setOnClickListener(v -> openBatterySaver());
        }
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
        if (btnApply != null) {
            btnApply.setOnClickListener(v -> {
                if (enableSwitch == null || enableSwitch.isChecked()) {
                    scheduleBatteryProfile(selHour[0], selMinute[0]);
                } else {
                    cancelBatterySchedule();
                }
                dialog.dismiss();
            });
        }

        updateDisplay.run();
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92f);
            dialog.getWindow().setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void scheduleBatteryProfile(int hour, int minute) {
        if (!rootReady) { toast("Grant root before scheduling GSM profiles"); showRootAccessDialog(); return; }
        ProfileScheduleReceiver.schedule(this, hour, minute);
        toast(String.format(Locale.US, "Ultra Battery scheduled daily at %02d:%02d", hour, minute));
    }

    private void cancelBatterySchedule() {
        ProfileScheduleReceiver.cancel(this);
        toast("Battery profile schedule cancelled");
    }

    private void showOverlayControls() {
        com.revror.kernelmanager.views.dialog.HwOverlayDialog.show(this);
    }

    private void startCpuOverlay() {
        SilentPermissionHelper.ensureOverlaySilent(this);
        Intent service = new Intent(this, CpuOverlayService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(service); else startService(service);
        getSharedPreferences("ror_tools", MODE_PRIVATE).edit().putBoolean("overlay", true).apply();
        toast("CPU overlay enabled • drag it anywhere");
    }

    private void showRefreshRate() {
        android.view.Display display = getWindowManager().getDefaultDisplay();
        float currentHz = display.getRefreshRate();
        List<Integer> rates = RefreshRateManager.getAvailableRates(this);
        boolean isLocked = RefreshRateManager.isRateLocked(this);
        int lockedHz = RefreshRateManager.getLockedRate(this);
        boolean is1Hz = RefreshRateManager.isAdaptive1HzActive(this);

        List<String> items = new ArrayList<>();
        for (int r : rates) {
            boolean isCur = Math.round(currentHz) == r;
            boolean isThisLocked = isLocked && lockedHz == r && !is1Hz;
            items.add(r + " Hz" + (isThisLocked ? "  [LOCKED]" : (isCur ? "  •  CURRENT ACTIVE" : "")));
        }
        items.add("1 Hz Adaptive Minimum (HyperOS / LTPO VRR)" + (is1Hz ? "  [ACTIVE]" : ""));
        items.add("Reset to Android system default");

        int adaptiveIndex = rates.size();
        int resetIndex = rates.size() + 1;

        String title = String.format(Locale.US, "Display Refresh Rate  •  %.0f Hz", currentHz)
                + (isLocked ? (is1Hz ? " (1 Hz Adaptive Locked)" : " (" + lockedHz + " Hz Locked)") : " (System Auto)");

        new AlertDialog.Builder(this, R.style.RorDialogTheme)
                .setTitle(title)
                .setItems(items.toArray(new String[0]), (d, which) -> {
                    if (which == adaptiveIndex) {
                        RefreshRateManager.RefreshRateStatus st = RefreshRateManager.setAdaptiveOneHz(this);
                        toast(st.details);
                    } else if (which == resetIndex) {
                        RefreshRateManager.RefreshRateStatus st = RefreshRateManager.resetToDefault(this);
                        toast(st.details);
                    } else {
                        int rate = rates.get(which);
                        RefreshRateManager.RefreshRateStatus st = RefreshRateManager.setFixedRate(this, rate);
                        toast(st.details);
                    }
                }).setNegativeButton("Close", null).show();
    }

    private void showCustomControls() {
        String[] items = {"Kernel manager", "Save current tuning as profile", "Apply custom profile", "Auto cleanup interval",
                "Compact memory now", "Release filesystem cache", "Force Doze now", "Android Data Saver", "Live CPU details"};
        new AlertDialog.Builder(this, R.style.RorDialogTheme).setTitle("REVROR Tuning & custom profiles")
                .setItems(items, (d, which) -> {
                    if (which == 0) showKernelManager();
                    else if (which == 1) saveCurrentCustomProfile();
                    else if (which == 2) showCustomProfiles();
                    else if (which == 3) showCleanupIntervals();
                    else if (which == 4) runRootTool("Memory compact", "[ -w /proc/sys/vm/compact_memory ] && echo 1 > /proc/sys/vm/compact_memory");
                    else if (which == 5) runRootTool("Filesystem cache", "sync; echo 3 > /proc/sys/vm/drop_caches");
                    else if (which == 6) com.revror.kernelmanager.views.dialog.ForceDozeDialog.show(this);
                    else if (which == 7) runPrivilegedTool("Data Saver", "cmd netpolicy set restrict-background true", false);
                    else showCpuStatus();
                }).setNegativeButton("Close", null).show();
    }

    private void showKernelManager() {
        openFullKernelManager(null);
    }

    private void openFullKernelManager(@Nullable String selection) {
        startActivity(new Intent(this, KernelTuningActivity.class));
    }

    private void showDetectedPicker(String title, String detect, String applyTemplate) {
        if (!rootReady) { toast(title + " requires root"); showRootAccessDialog(); return; }
        toast("Detecting supported values");
        submitTask(() -> {
            String output = RootUtils.runAndGetOutput(detect).trim();
            String[] raw = output.isEmpty() ? new String[0] : output.split("\\s+");
            List<String> values = new ArrayList<>(); for (String v : raw) if (!v.isEmpty() && !values.contains(v)) values.add(v);
            postToUi(() -> {
                if (values.isEmpty()) { toast("This kernel does not expose supported values"); return; }
                String[] labels = new String[values.size()];
                for (int i=0;i<values.size();i++) labels[i] = values.get(i).matches("\\d{5,}") ? values.get(i) + " Hz" : values.get(i);
                new AlertDialog.Builder(this, R.style.RorDialogTheme).setTitle(title + " • detected")
                        .setItems(labels, (d,w) -> runRootTool(title, String.format(Locale.US, applyTemplate, shellQuote(values.get(w)))))
                        .setNegativeButton("Close", null).show();
            });
        });
    }

    private void showStaticRootPicker(String title, String[] values, String command) {
        new AlertDialog.Builder(this, R.style.RorDialogTheme).setTitle(title).setItems(values, (d,w) -> {
            String value = title.startsWith("SELinux") ? (w == 0 ? "1" : "0") : values[w];
            runRootTool(title, String.format(Locale.US, command, value));
        }).setNegativeButton("Close", null).show();
    }

    private void showCleanupIntervals() {
        String[] labels = {"Off", "Every 5 minutes", "Every 15 minutes", "Every 30 minutes", "Every 1 hour"};
        int[] minutes = {0,5,15,30,60};
        new AlertDialog.Builder(this, R.style.RorDialogTheme).setTitle("Automatic cleanup duration")
                .setItems(labels, (d,w) -> scheduleCleanup(minutes[w])).setNegativeButton("Close", null).show();
    }

    private void scheduleCleanup(int minutes) {
        AlarmManager alarm = (AlarmManager)getSystemService(ALARM_SERVICE);
        PendingIntent pending = PendingIntent.getBroadcast(this, 773, new Intent(this, CleanupReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (alarm == null) return; alarm.cancel(pending);
        if (minutes > 0) alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, android.os.SystemClock.elapsedRealtime() + minutes * 60000L, minutes * 60000L, pending);
        getSharedPreferences("ror_tools", MODE_PRIVATE).edit().putInt("cleanup_minutes", minutes).apply();
        toast(minutes == 0 ? "Automatic cleanup disabled" : "Cleanup scheduled every " + minutes + " minutes");
    }

    private void saveCurrentCustomProfile() {
        if (!RorLicenseManager.isPremiumActive(this)) {
            RorLicenseManager.showVipRequiredDialog(this, "Custom Profile Studio", () -> saveCurrentCustomProfile());
            return;
        }
        com.revror.kernelmanager.views.dialog.CustomProfileDialog.showEditor(this, null, this::refreshCustomProfilesUi);
    }

    private void showCustomProfiles() {
        showPage(2);
        refreshCustomProfilesUi();
        View container = findViewById(R.id.custom_profiles_section);
        if (container != null) {
            container.getParent().requestChildFocus(container, container);
        }
    }

    private void runRootTool(String name, String command) {
        if (!rootReady) { toast(name + " requires root; standard tools still work"); showRootAccessDialog(); return; }
        toast("Applying " + name);
        submitTask(() -> { RootUtils.runCommand(command); postToUi(() -> toast(name + " completed")); });
    }

    private void runPrivilegedTool(String name, String command, boolean rootOnly) {
        if (rootReady) { runRootTool(name, command); return; }
        if (!rootOnly && shizukuReady) {
            toast("Applying " + name + " through Shizuku");
            submitTask(() -> { ShizukuBridge.Result r=ShizukuBridge.run(command); postToUi(() -> toast(r.success ? name + " completed" : name + " failed: " + r.output)); });
            return;
        }
        toast(name + (rootOnly ? " requires root" : " requires Root or Shizuku")); showRootAccessDialog();
    }

    private long directorySize(File file) {
        if (file == null || !file.exists()) return 0;
        if (file.isFile()) return file.length();
        long size = 0; File[] children = file.listFiles();
        if (children != null) for (File child : children) size += directorySize(child);
        return size;
    }

    private void deleteChildren(File directory) {
        File[] children = directory == null ? null : directory.listFiles();
        if (children == null) return;
        for (File child : children) { if (child.isDirectory()) deleteChildren(child); child.delete(); }
    }

    private void openBatterySaver() {
        if (rootReady || shizukuReady) {
            runPrivilegedTool("Battery Saver", "cmd power set-mode 1 2>/dev/null || settings put global low_power 1", false);
        } else {
            toast("Android requires confirmation when root is disabled");
            openSystemSetting("android.settings.BATTERY_SAVER_SETTINGS", null);
        }
    }

    private String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private void openSystemSetting(String action, @Nullable String message) {
        if (message != null) toast(message);
        try {
            startActivity(new Intent(action));
        } catch (Exception first) {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            catch (Exception ignored) { toast("This Android build does not provide that settings panel"); }
        }
    }

    private void openUrl(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception e) { toast(url); }
    }

    private void submitTask(Runnable action) {
        if (isFinishing() || isDestroyed() || executor.isShutdown()) return;
        try {
            executor.execute(() -> {
                if (!isFinishing() && !isDestroyed() && !Thread.currentThread().isInterrupted()) action.run();
            });
        } catch (java.util.concurrent.RejectedExecutionException ignored) {
            // Activity destruction can race with delayed callbacks and background completions.
        }
    }

    private void postToUi(Runnable action) {
        if (isFinishing() || isDestroyed()) return;
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) action.run();
        });
    }

    private void checkForUpdates(boolean interactive) {
        if (isFinishing() || isDestroyed() || executor.isShutdown()) return;
        long now = System.currentTimeMillis();
        long last = getSharedPreferences("ror_update", MODE_PRIVATE).getLong("last_check", 0L);
        if (!interactive && now - last < 24L * 60L * 60L * 1000L) return;
        getSharedPreferences("ror_update", MODE_PRIVATE).edit().putLong("last_check", now).apply();
        if (interactive) toast("Checking GitHub releases for REVROR updates");
        submitTask(() -> {
            JSONObject updateObj = null;
            String downloadUrl = UPDATE_RELEASE_PAGE;
            String versionTitle = "";
            String notes = "";
            boolean isNewer = false;

            // Strategy 1: Try release.json asset from latest GitHub release
            try {
                updateObj = fetchJsonFromUrl(UPDATE_RELEASE_JSON);
            } catch (Exception ignored) {}

            // Strategy 2: Query GitHub Releases API
            if (updateObj == null) {
                try {
                    JSONObject releaseJson = fetchJsonFromUrl(UPDATE_API);
                    if (releaseJson != null) {
                        String tagName = releaseJson.optString("tag_name", "");
                        String body = releaseJson.optString("body", "");
                        String htmlUrl = releaseJson.optString("html_url", UPDATE_RELEASE_PAGE);
                        JSONArray assets = releaseJson.optJSONArray("assets");
                        String apkUrl = htmlUrl;
                        if (assets != null) {
                            for (int i = 0; i < assets.length(); i++) {
                                JSONObject a = assets.optJSONObject(i);
                                if (a != null) {
                                    String aName = a.optString("name", "");
                                    if (aName.equalsIgnoreCase("release.json")) {
                                        String aDown = a.optString("browser_download_url", "");
                                        if (!aDown.isEmpty()) {
                                            try {
                                                JSONObject inner = fetchJsonFromUrl(aDown);
                                                if (inner != null) {
                                                    updateObj = inner;
                                                    break;
                                                }
                                            } catch (Exception ignored2) {}
                                        }
                                    } else if (aName.toLowerCase(Locale.US).endsWith(".apk")) {
                                        apkUrl = a.optString("browser_download_url", htmlUrl);
                                    }
                                }
                            }
                        }
                        if (updateObj == null && !tagName.isEmpty()) {
                            updateObj = new JSONObject();
                            updateObj.put("versionName", tagName);
                            updateObj.put("releaseNotes", body.isEmpty() ? "New release on GitHub: " + tagName : body);
                            updateObj.put("releaseUrl", apkUrl);
                            int code = extractVersionCode(tagName);
                            updateObj.put("versionCode", code);
                        }
                    }
                } catch (Exception ignored) {}
            }

            // Strategy 3: Try raw repository manifest fallback
            if (updateObj == null) {
                try { updateObj = fetchJsonFromUrl(UPDATE_RAW_JSON); } catch (Exception ignored) {}
            }
            if (updateObj == null) {
                try { updateObj = fetchJsonFromUrl(UPDATE_RAW_JSON_MAIN); } catch (Exception ignored) {}
            }

            if (updateObj != null) {
                int latestCode = updateObj.optInt("latestVersionCode", updateObj.optInt("versionCode", 0));
                String vName = updateObj.optString("versionName", "");
                notes = updateObj.optString("releaseNotes", updateObj.optString("changelog", "A new REVROR update is available."));
                downloadUrl = updateObj.optString("releaseUrl", updateObj.optString("apkUrl", UPDATE_RELEASE_PAGE));
                if (!isTrustedUpdateUrl(downloadUrl)) downloadUrl = UPDATE_RELEASE_PAGE;
                versionTitle = vName.isEmpty() ? ("v" + latestCode) : vName;

                if (latestCode > BuildConfig.VERSION_CODE) {
                    isNewer = true;
                } else if (!vName.isEmpty()) {
                    isNewer = isVersionNewer(vName, BuildConfig.VERSION_NAME);
                }
            }

            final boolean hasUpdate = isNewer;
            final String finalVer = versionTitle;
            final String finalNotes = notes;
            final String finalUrl = downloadUrl;

            postToUi(() -> {
                if (hasUpdate) {
                    new AlertDialog.Builder(this, R.style.RorDialogTheme)
                            .setTitle("REVROR update available • " + finalVer)
                            .setMessage(finalNotes.isEmpty() ? "A new update is available on GitHub releases." : finalNotes)
                            .setNegativeButton("Later", null)
                            .setPositiveButton("Download update", (d, w) -> openUrl(finalUrl))
                            .show();
                } else if (interactive) {
                    new AlertDialog.Builder(this, R.style.RorDialogTheme)
                            .setTitle("REVROR is up to date")
                            .setMessage("Installed version: " + BuildConfig.VERSION_NAME + "\nChecked against official GitHub releases.")
                            .setNegativeButton("Close", null)
                            .setPositiveButton("Releases page", (d, w) -> openUrl(UPDATE_RELEASE_PAGE))
                            .show();
                }
            });
        });
    }

    private JSONObject fetchJsonFromUrl(String urlString) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(urlString).openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "REVROR-App/" + BuildConfig.VERSION_NAME);
            int code = conn.getResponseCode();
            if (code != 200) return null;
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null && sb.length() < 131072) sb.append(line);
            }
            return new JSONObject(sb.toString());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private int extractVersionCode(String tag) {
        try {
            String digits = tag.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                if (digits.length() == 2) {
                    return Integer.parseInt(digits) * 10;
                }
                return Integer.parseInt(digits);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private boolean isVersionNewer(String remote, String local) {
        try {
            String cleanRemote = remote.replaceAll("[^0-9.]", "");
            String cleanLocal = local.replaceAll("[^0-9.]", "");
            String[] rParts = cleanRemote.split("\\.");
            String[] lParts = cleanLocal.split("\\.");
            int len = Math.max(rParts.length, lParts.length);
            for (int i = 0; i < len; i++) {
                int r = i < rParts.length ? Integer.parseInt(rParts[i]) : 0;
                int l = i < lParts.length ? Integer.parseInt(lParts[i]) : 0;
                if (r > l) return true;
                if (r < l) return false;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private boolean isTrustedUpdateUrl(String value) {
        try {
            Uri uri = Uri.parse(value);
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && host != null && (host.equalsIgnoreCase("github.com")
                    || host.endsWith(".github.com")
                    || host.equalsIgnoreCase("raw.githubusercontent.com")
                    || host.equalsIgnoreCase("revror.pages.dev"));
        } catch (Exception ignored) { return false; }
    }

    private void showAbout() {
        new AlertDialog.Builder(this, R.style.RorDialogTheme).setTitle("Reincarnation of Revolutionary")
                .setMessage("REVROR " + BuildConfig.VERSION_NAME + " • by revWhiteshadow\n\n"
                        + "Official Releases: https://github.com/revws/ror\n\n"
                        + "A single-app performance dashboard with built-in profiles, Shizuku fallback, live monitoring, kernel controls, and Android system tools.\n\n"
                        + "Credits: revWhiteshadow\nTelegram: @godTspeed • @godspeedmode\n\n"
                        + "Open-source components and retained upstream credits are documented under GPL-3.0-or-later.")
                .setNeutralButton("Ko-fi / Support", (d, w) -> openUrl("https://ko-fi.com/revWS"))
                .setNegativeButton("Licenses", (d, w) -> showOpenSourceLicenses())
                .setPositiveButton("Close", null).show();
    }

    private void showOpenSourceLicenses() {
        new AlertDialog.Builder(this, R.style.RorDialogTheme)
                .setTitle("Open Source Licenses & Compliance")
                .setMessage("REVROR • Reincarnation of Revolutionary is licensed under GNU General Public License v3.0 or later • GPL-3.0-or-later.\n\n"
                        + "This application includes and builds upon trusted open source components:\n"
                        + "• TopJohnWu libsu: Apache 2.0\n"
                        + "• AndroidX Libraries: Apache 2.0\n"
                        + "• Google Material Components: Apache 2.0\n"
                        + "• Linux Kernel sysfs interfaces & Android tuners: GPL-3.0\n\n"
                        + "In compliance with GNU GPL-3.0, the license terms and open-source notices are preserved.\n"
                        + "Full license text is available at:\nhttps://www.gnu.org/licenses/gpl-3.0.html")
                .setPositiveButton("View GPL-3.0", (d, w) -> openUrl("https://www.gnu.org/licenses/gpl-3.0.html"))
                .setNegativeButton("Close", null)
                .show();
    }

    private void showDonationDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_crypto_donation, null, false);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.RorDialogTheme)
                .setView(view)
                .setNegativeButton("Close", null)
                .create();

        View kofiRow = view.findViewById(R.id.wallet_row_kofi);
        View btnKofi = view.findViewById(R.id.btn_kofi_action);
        View.OnClickListener kofiClick = v -> {
            toast("Opening Ko-fi page");
            openUrl("https://ko-fi.com/revWS");
        };
        if (kofiRow != null) kofiRow.setOnClickListener(kofiClick);
        if (btnKofi != null) btnKofi.setOnClickListener(kofiClick);

        View binanceRow = view.findViewById(R.id.wallet_row_binance);
        View btnBinance = view.findViewById(R.id.btn_binance_action);
        View.OnClickListener binanceClick = v -> copyToClipboard("113486850", "Binance Pay ID");
        if (binanceRow != null) binanceRow.setOnClickListener(binanceClick);
        if (btnBinance != null) btnBinance.setOnClickListener(binanceClick);

        View trc20Row = view.findViewById(R.id.wallet_row_trc20);
        View btnTrc20 = view.findViewById(R.id.btn_trc20_action);
        View.OnClickListener trc20Click = v -> copyToClipboard("TDu77Susy3CzbkJGAjPRCGiVt5obBzypRb", "TRC20 USDT address");
        if (trc20Row != null) trc20Row.setOnClickListener(trc20Click);
        if (btnTrc20 != null) btnTrc20.setOnClickListener(trc20Click);

        View erc20Row = view.findViewById(R.id.wallet_row_erc20);
        View btnErc20 = view.findViewById(R.id.btn_erc20_action);
        View.OnClickListener erc20Click = v -> copyToClipboard("0x18d45134688d047f6fc52969980fc8c13f796508", "ERC20 USDT address");
        if (erc20Row != null) erc20Row.setOnClickListener(erc20Click);
        if (btnErc20 != null) btnErc20.setOnClickListener(erc20Click);

        View bep20Row = view.findViewById(R.id.wallet_row_bep20);
        View btnBep20 = view.findViewById(R.id.btn_bep20_action);
        View.OnClickListener bep20Click = v -> copyToClipboard("0x18d45134688d047f6fc52969980fc8c13f796508", "BEP20 USDT address");
        if (bep20Row != null) bep20Row.setOnClickListener(bep20Click);
        if (btnBep20 != null) btnBep20.setOnClickListener(bep20Click);

        dialog.show();
    }

    private void copyToClipboard(String text, String label) {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("REVROR", text);
                clipboard.setPrimaryClip(clip);
                toast("Copied " + label);
            }
        } catch (Exception e) {
            toast("Copied: " + text);
        }
    }

    private void toast(String message) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {}
        View anchor = findViewById(R.id.ad_banner);
        if (anchor == null || anchor.getVisibility() != View.VISIBLE) {
            anchor = findViewById(android.R.id.content);
        }
        if (anchor != null) {
            try {
                Snackbar bar = Snackbar.make(anchor, message, Snackbar.LENGTH_SHORT)
                        .setTextColor(Color.WHITE).setBackgroundTint(Color.parseColor("#EE28213B"));
                View snackbarView = bar.getView();
                snackbarView.setBackgroundResource(R.drawable.ror_toast);
                android.view.ViewGroup.LayoutParams lp = snackbarView.getLayoutParams();
                if (lp instanceof android.view.ViewGroup.MarginLayoutParams) {
                    android.view.ViewGroup.MarginLayoutParams marginLp = (android.view.ViewGroup.MarginLayoutParams) lp;
                    marginLp.bottomMargin = dp(88);
                    snackbarView.setLayoutParams(marginLp);
                }
                bar.show();
            } catch (Throwable ignored) {}
        }
    }

    @Override protected void onStart() {
        super.onStart();
        handler.post(metricsUpdater);
        ImageView banner = findViewById(R.id.hero_banner);
        boolean animate = getSharedPreferences("ror_theme", MODE_PRIVATE)
                .getBoolean("animate_home_banner", true);
        applyHomeBannerAnimation(banner,
                animate && banner != null && banner.getVisibility() == View.VISIBLE);
    }

    @Override protected void onStop() {
        handler.removeCallbacks(metricsUpdater);
        if (pillDotAnimator != null) {
            pillDotAnimator.cancel();
            pillDotAnimator = null;
        }
        stopHomeBannerAnimation(findViewById(R.id.hero_banner));
        super.onStop();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (pillDotAnimator != null) {
            pillDotAnimator.cancel();
            pillDotAnimator = null;
        }
        stopHomeBannerAnimation(findViewById(R.id.hero_banner));
        ShizukuBridge.removeConnectionListener(shizukuConnectionListener);
        try { Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener); } catch (Throwable ignored) {}
        if (adBanner != null) { try { adBanner.stopLoading(); adBanner.destroy(); } catch (RuntimeException ignored) {} }
        if (activeProfileAnimator != null) activeProfileAnimator.cancel();
        com.revror.kernelmanager.utils.kernel.compat.ProfileExecutionCoordinator.getInstance()
                .removeListener(mProfileCoordinatorListener);
        executor.shutdownNow(); super.onDestroy();
    }

    private String formatNetSpeed(long kbps) {
        if (kbps >= 1024) {
            return String.format(Locale.US, "%.1f MB/s", kbps / 1024f);
        }
        return kbps + " KB/s";
    }

    private int readGpuPercent() {
        if (rootReady) {
            try {
                String out = RootUtils.runAndGetOutput(
                        "for p in /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage /sys/kernel/gpu/gpu_busy /sys/class/devfreq/*gpu*/cur_freq; do "
                                + "[ -r \"$p\" ] && cat \"$p\" && break; done").trim();
                if (!out.isEmpty()) {
                    int val = Integer.parseInt(out.split("\\s+")[0].replaceAll("[^0-9]", ""));
                    if (val >= 0 && val <= 100) return val;
                }
            } catch (Throwable ignored) {}
        }
        return Math.max(8, Math.min(95, Math.round(cpuStats.readPercent(rootReady) * 0.75f)));
    }

    private int measurePing() {
        long start = System.currentTimeMillis();
        try (java.net.Socket s = new java.net.Socket()) {
            s.connect(new java.net.InetSocketAddress("1.1.1.1", 53), 1000);
            return (int) (System.currentTimeMillis() - start);
        } catch (Throwable e) {
            try (java.net.Socket s = new java.net.Socket()) {
                s.connect(new java.net.InetSocketAddress("8.8.8.8", 53), 1000);
                return (int) (System.currentTimeMillis() - start);
            } catch (Throwable ignored) {
                return -1;
            }
        }
    }

    private void updateMetrics() {
        submitTask(() -> {
            SystemUsageReader.SystemTelemetry telemetry = SystemUsageReader.read(this);
            ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
            if (manager != null) manager.getMemoryInfo(memory);
            int ramPercent = memory.totalMem == 0 ? 0
                    : (int) (100L * (memory.totalMem - memory.availMem) / memory.totalMem);
            StatFs statFs = new StatFs(getFilesDir().getAbsolutePath());
            long total = statFs.getTotalBytes();
            long avail = statFs.getAvailableBytes();
            float usedPercent = total > 0 ? (float) ((total - avail) * 100.0 / total) : 50f;
            String free = String.format(Locale.US, "%.1f GB", avail / 1073741824d);
            int cpu = cpuStats.readPercent(rootReady);

            int percent = telemetry.batteryLevel;
            float temperature = telemetry.temperatureC;
            boolean charging = telemetry.chargingStatus != null && telemetry.chargingStatus.startsWith("Charging");

            // Thermal Guard Emergency Check (auto stop background apps, preserve game & seated profile)
            int thermalLimit = getSharedPreferences("ror_tools", MODE_PRIVATE).getInt("thermal_limit", 45);
            if (thermalLimit > 0 && temperature >= thermalLimit) {
                long now = System.currentTimeMillis();
                if (now - lastThermalTriggerTime > 60_000L) {
                    lastThermalTriggerTime = now;
                    triggerThermalEmergency(temperature, thermalLimit);
                }
            }

            // Measure Ping Latency
            int ping = measurePing();
            if (ping >= 0) lastPingMs = ping;
            else ping = lastPingMs >= 0 ? lastPingMs : 24;

            // Network throughput
            long curRx = TrafficStats.getTotalRxBytes();
            long curTx = TrafficStats.getTotalTxBytes();
            long curTime = System.currentTimeMillis();
            long dlKbps = 0;
            long ulKbps = 0;
            if (prevNetTimestamp > 0 && curTime > prevNetTimestamp && curRx >= prevRxBytes && curTx >= prevTxBytes) {
                long dt = curTime - prevNetTimestamp;
                dlKbps = ((curRx - prevRxBytes) * 1000L) / (dt * 1024L);
                ulKbps = ((curTx - prevTxBytes) * 1000L) / (dt * 1024L);
            }
            prevRxBytes = curRx;
            prevTxBytes = curTx;
            prevNetTimestamp = curTime;

            // GPU Load
            int gpuLoad = readGpuPercent();

            // FPS
            int fps = 60;
            try {
                Display display = getWindowManager().getDefaultDisplay();
                fps = Math.round(display.getRefreshRate());
            } catch (Throwable ignored) {}

            // CPU Clock
            long cpuKHz = cpuStats.readAverageCurrentKHz(rootReady);
            float cpuGHz = cpuKHz > 0 ? (cpuKHz / 1_000_000f) : 1.8f;

            final int finalPing = ping;
            final long finalDl = dlKbps;
            final long finalUl = ulKbps;
            final int finalGpu = gpuLoad;
            final int finalFps = fps;
            final float finalCpuGHz = cpuGHz;
            final long finalCpuKHz = cpuKHz;

            postToUi(() -> {
                // 1. Home Page 4x4 Grid Updates
                if (batteryRing != null) batteryRing.setText(percent + "%");
                if (remainingText != null) {
                    String monitorText = charging ? "Charging " + percent + "% • Realtime" : "Discharging " + percent + "% • Realtime";
                    remainingText.setText(monitorText);
                }
                TextView overviewTitle = findViewById(R.id.overview_section_title);
                if (overviewTitle != null) overviewTitle.setText("Device Overview");

                if (cpuText != null) cpuText.setText(cpu < 0 ? "N/A" : cpu + "%");
                if (ramText != null) ramText.setText(ramPercent + "%");
                if (storageText != null) storageText.setText(free);
                if (tempText != null) tempText.setText(String.format(Locale.US, "%.1fC", temperature));
                if (pingText != null) pingText.setText(finalPing + " ms");
                if (gpuText != null) gpuText.setText(finalGpu + "%");
                if (fpsText != null) fpsText.setText(finalFps + " FPS");
                if (netDlText != null) netDlText.setText(formatNetSpeed(finalDl));
                if (netUlText != null) netUlText.setText(formatNetSpeed(finalUl));
                if (sleepText != null) sleepText.setText(telemetry.deepSleepPct + "%");
                if (awakeText != null) awakeText.setText(telemetry.awakePct + "%");
                if (sotText != null) sotText.setText(telemetry.sotStr != null ? telemetry.sotStr : "0h 0m");
                if (voltText != null) voltText.setText(String.format(Locale.US, "%.2f V", telemetry.voltageMv / 1000f));
                if (clockText != null) clockText.setText(String.format(Locale.US, "%.2f GHz", finalCpuGHz));
                if (swapText != null) swapText.setText(telemetry.swapUsagePct + "%");

                // Gauges smooth progress
                if (batteryPieGraph != null) batteryPieGraph.setProgressSmooth(percent);
                if (cpuGraph != null && cpu >= 0) cpuGraph.setProgressSmooth(cpu);
                if (ramGraph != null) ramGraph.setProgressSmooth(ramPercent);
                if (storageGraph != null) storageGraph.setProgressSmooth(usedPercent);
                if (tempGraph != null) tempGraph.setProgressSmooth(temperature);
                if (pingGraph != null) pingGraph.setProgressSmooth(finalPing);
                if (gpuGraph != null) gpuGraph.setProgressSmooth(finalGpu);
                if (fpsGraph != null) fpsGraph.setProgressSmooth(finalFps);
                if (netDlGraph != null) netDlGraph.setProgressSmooth(Math.min(1000, finalDl));
                if (netUlGraph != null) netUlGraph.setProgressSmooth(Math.min(1000, finalUl));
                if (sleepGraph != null) sleepGraph.setProgressSmooth(telemetry.deepSleepPct);
                if (awakeGraph != null) awakeGraph.setProgressSmooth(telemetry.awakePct);
                if (sotGraph != null) sotGraph.setProgressSmooth((float) (telemetry.sotMs / 60000L));
                if (voltGraph != null) voltGraph.setProgressSmooth(telemetry.voltageMv);
                if (clockGraph != null) clockGraph.setProgressSmooth(finalCpuKHz > 0 ? (finalCpuKHz / 1000f) : 1800f);
                if (swapGraph != null) swapGraph.setProgressSmooth(telemetry.swapUsagePct);

                // 2. System Monitor Tab Updates
                if (monitorUptimeBadge != null) monitorUptimeBadge.setText("UPTIME: " + telemetry.uptimeStr);
                if (monitorSotText != null) monitorSotText.setText(telemetry.sotStr + " SOT Today");
                if (monitorSleepRatioText != null) monitorSleepRatioText.setText("Deep Sleep: " + telemetry.deepSleepPct + "% • Awake: " + telemetry.awakePct + "%");
                if (monitorScreenOffText != null) monitorScreenOffText.setText("Screen Off: " + SystemUsageReader.formatDuration(Math.max(0, telemetry.uptimeMs - telemetry.sotMs)));

                if (monitorSleepPie != null) monitorSleepPie.setProgressSmooth(telemetry.deepSleepPct);
                if (monitorBatteryPie != null) monitorBatteryPie.setProgressSmooth(telemetry.batteryLevel);
                if (monitorBatteryLevelText != null) monitorBatteryLevelText.setText(telemetry.batteryLevel + "% Battery");
                if (monitorBatteryStatusText != null) monitorBatteryStatusText.setText("Status: " + telemetry.chargingStatus);
                if (monitorBatteryHealthText != null) monitorBatteryHealthText.setText("Health: " + telemetry.healthStr + " • Li-ion");
                if (monitorVoltageText != null) monitorVoltageText.setText(telemetry.voltageMv + " mV");
                if (monitorTempText != null) monitorTempText.setText(String.format(Locale.US, "%.1fC", telemetry.temperatureC));

                if (monitorCpuPie != null && cpu >= 0) monitorCpuPie.setProgressSmooth(cpu);
                if (monitorCpuPctText != null) monitorCpuPctText.setText((cpu >= 0 ? cpu : 0) + "%");
                if (monitorRamPie != null) monitorRamPie.setProgressSmooth(ramPercent);
                if (monitorRamPctText != null) monitorRamPctText.setText(ramPercent + "%");
                if (monitorStoragePie != null) monitorStoragePie.setProgressSmooth(usedPercent);
                if (monitorStoragePctText != null) monitorStoragePctText.setText(free);
                if (monitorSwapPie != null) monitorSwapPie.setProgressSmooth(telemetry.swapUsagePct);
                if (monitorSwapPctText != null) monitorSwapPctText.setText(telemetry.swapUsagePct + "%");

                updateTopAppsList(telemetry.topApps);
            });
        });
    }

    private void updateTopAppsList(List<SystemUsageReader.AppUsageItem> apps) {
        if (monitorAppsContainer == null) return;
        monitorAppsContainer.removeAllViews();
        if (apps == null || apps.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No foreground usage records today or permission requested.");
            empty.setTextColor(0xFF8E95B2);
            empty.setTextSize(11f);
            empty.setPadding(0, dp(6), 0, dp(6));
            monitorAppsContainer.addView(empty);
            return;
        }

        for (SystemUsageReader.AppUsageItem item : apps) {
            android.widget.LinearLayout row = new android.widget.LinearLayout(this);
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(6), dp(8), dp(6));
            row.setBackgroundColor(0xFF121528);
            android.widget.LinearLayout.LayoutParams rowLp = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.setMargins(0, 0, 0, dp(6));
            row.setLayoutParams(rowLp);

            ImageView icon = new ImageView(this);
            icon.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp(28), dp(28)));
            if (item.icon != null) icon.setImageDrawable(item.icon);
            else icon.setImageResource(R.drawable.ror_logo);
            row.addView(icon);

            android.widget.LinearLayout info = new android.widget.LinearLayout(this);
            info.setOrientation(android.widget.LinearLayout.VERTICAL);
            android.widget.LinearLayout.LayoutParams infoLp = new android.widget.LinearLayout.LayoutParams(
                    0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            infoLp.setMargins(dp(10), 0, dp(10), 0);
            info.setLayoutParams(infoLp);

            TextView name = new TextView(this);
            name.setText(item.appName);
            name.setTextColor(Color.WHITE);
            name.setTextSize(12f);
            name.setTypeface(null, android.graphics.Typeface.BOLD);
            name.setMaxLines(1);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            info.addView(name);

            TextView pkg = new TextView(this);
            pkg.setText(item.packageName);
            pkg.setTextColor(0xFF7A85A8);
            pkg.setTextSize(9.5f);
            pkg.setMaxLines(1);
            pkg.setEllipsize(android.text.TextUtils.TruncateAt.END);
            info.addView(pkg);

            row.addView(info);

            android.widget.LinearLayout stats = new android.widget.LinearLayout(this);
            stats.setOrientation(android.widget.LinearLayout.VERTICAL);
            stats.setGravity(android.view.Gravity.END);

            TextView time = new TextView(this);
            time.setText(item.formattedTime);
            time.setTextColor(0xFF00F5D4);
            time.setTextSize(12f);
            time.setTypeface(null, android.graphics.Typeface.BOLD);
            stats.addView(time);

            TextView pct = new TextView(this);
            pct.setText(item.percent + "% of screen");
            pct.setTextColor(0xFF8E95B2);
            pct.setTextSize(9.5f);
            stats.addView(pct);

            row.addView(stats);
            monitorAppsContainer.addView(row);
        }
    }
}

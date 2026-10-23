package com.revror.kernelmanager.services;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.revror.kernelmanager.R;
import com.revror.kernelmanager.activities.DashboardActivity;
import com.revror.kernelmanager.utils.CpuStatsReader;
import com.revror.kernelmanager.utils.GsmProfileManager;
import com.revror.kernelmanager.utils.ShizukuBridge;
import com.revror.kernelmanager.utils.kernel.compat.ProfileRegistry;
import com.revror.kernelmanager.utils.kernel.cpu.Temperature;
import com.revror.kernelmanager.utils.root.RootUtils;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class CpuOverlayService extends Service {
    public static final String ACTION_STOP = "com.revror.kernelmanager.STOP_CPU_OVERLAY";
    private static final String CHANNEL = "ror_live_overlay";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService reader = Executors.newSingleThreadExecutor();
    private final CpuStatsReader cpuStats = new CpuStatsReader();
    private WindowManager windowManager;
    private WindowManager.LayoutParams params;
    private TextView overlay;
    private String cpuName = "Device CPU", cachedFps = "N/A";
    private int refreshCount;
    private boolean compactMode = true;

    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            if (reader.isShutdown()) return;
            reader.execute(() -> {
                String foreground = PowerButtonGestureService.getForegroundPackage(CpuOverlayService.this);
                boolean inGame = foreground != null && com.revror.kernelmanager.utils.GameLauncherRegistry.getGamePackages(CpuOverlayService.this).contains(foreground);
                boolean gameOnly = getSharedPreferences("ror_tools", MODE_PRIVATE).getBoolean("overlay_game_only", false);
                android.os.PowerManager power = (android.os.PowerManager) getSystemService(POWER_SERVICE);
                android.app.KeyguardManager keyguard = (android.app.KeyguardManager) getSystemService(KEYGUARD_SERVICE);
                final boolean show = (!gameOnly || inGame) && (power == null || power.isInteractive()) && (keyguard == null || !keyguard.isKeyguardLocked());
                String text = show ? readLiveText() : "";
                handler.post(() -> {
                    if (overlay != null) {
                        overlay.setVisibility(show ? View.VISIBLE : View.GONE);
                        if (show) overlay.setText(text);
                    }
                });
            });
            int interval = getSharedPreferences("ror_tools", MODE_PRIVATE).getInt("overlay_interval_ms", 1000);
            handler.postDelayed(this, Math.max(300, interval));
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        String description = cpuStats.deviceDescription(GsmProfileManager.isRootEnabled(this));
        if (description != null && !description.trim().isEmpty()) cpuName = description.split("\n")[0];
        createChannel();
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, DashboardActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, CpuOverlayService.class).setAction(ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_cpu)
                .setContentTitle("REVROR hardware overlay").setContentText("CPU cores, governor, GPU, FPS, battery and RAM")
                .setOngoing(true).setContentIntent(open).addAction(0, "Stop", stop).build();
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(6020, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(6020, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(6020, notification);
            }
        } catch (Throwable t) {
            android.util.Log.e("CpuOverlay", "FGS start error: " + t.getMessage());
        }
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            com.revror.kernelmanager.utils.SilentPermissionHelper.ensureOverlaySilent(this);
            try { Thread.sleep(150); } catch (InterruptedException ignored) {}
        }
        showOverlay();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    private void showOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        overlay = new TextView(this);
        overlay.setVisibility(View.VISIBLE);
        overlay.setTextColor(Color.WHITE);
        overlay.setTextSize(11f);
        overlay.setPadding(dp(12), dp(8), dp(12), dp(8));
        overlay.setGravity(Gravity.CENTER_VERTICAL);
        overlay.setElevation(dp(8));

        int alphaPct = getSharedPreferences("ror_tools", MODE_PRIVATE).getInt("overlay_alpha", 90);
        overlay.setAlpha(Math.max(0.2f, Math.min(1.0f, alphaPct / 100f)));

        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xEE16102A, 0xEE051522});
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), 0xFF25C8FF);
        overlay.setBackground(bg);

        params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.END;

        int savedX = getSharedPreferences("ror_tools", MODE_PRIVATE).getInt("overlay_pos_x", dp(10));
        int savedY = getSharedPreferences("ror_tools", MODE_PRIVATE).getInt("overlay_pos_y", dp(90));
        params.x = savedX;
        params.y = savedY;

        overlay.setOnTouchListener(new View.OnTouchListener() {
            private int startX, startY;
            private float downX, downY;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startX = params.x;
                    startY = params.y;
                    downX = event.getRawX();
                    downY = event.getRawY();
                    isDragging = false;
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    float dx = Math.abs(event.getRawX() - downX);
                    float dy = Math.abs(event.getRawY() - downY);
                    if (dx > 10 || dy > 10) isDragging = true;
                    params.x = Math.max(0, startX - (int)(event.getRawX() - downX));
                    params.y = Math.max(0, startY + (int)(event.getRawY() - downY));
                    try { windowManager.updateViewLayout(overlay, params); } catch (Exception ignored) {}
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (isDragging) {
                        getSharedPreferences("ror_tools", MODE_PRIVATE).edit()
                                .putInt("overlay_pos_x", params.x)
                                .putInt("overlay_pos_y", params.y)
                                .apply();
                    } else {
                        compactMode = !compactMode;
                        handler.post(refresh);
                    }
                    return true;
                }
                return false;
            }
        });

        try {
            windowManager.addView(overlay, params);
        } catch (Throwable t) {
            android.util.Log.e("CpuOverlay", "Error adding overlay: " + t.getMessage());
        }
        getSharedPreferences("ror_tools", MODE_PRIVATE).edit().putBoolean("overlay", true).apply();
        handler.post(refresh);
    }

    private String readLiveText() {
        boolean root = GsmProfileManager.isRootEnabled(this);
        int cpu = cpuStats.readPercent(root);
        String governor = firstReadable(new String[]{"/sys/devices/system/cpu/cpufreq/policy0/scaling_governor","/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"}, root);
        List<Integer> online = parseCpuList(readPath("/sys/devices/system/cpu/online", root));
        if (online.isEmpty()) online = parseCpuList("0-" + Math.max(0, Runtime.getRuntime().availableProcessors()-1));

        StringBuilder cores = new StringBuilder();
        List<Integer> coreUsages = cpuStats.readCorePercents(root);
        int usageIdx = 0;
        for (int id : online) {
            long khz = parseLong(readPath("/sys/devices/system/cpu/cpu" + id + "/cpufreq/scaling_cur_freq", root));
            if (cores.length() > 0) cores.append("  ");
            cores.append('C').append(id).append(' ');
            cores.append(khz > 0 ? formatMHz(khz) : "on");
            if (coreUsages != null && usageIdx < coreUsages.size()) {
                int pct = coreUsages.get(usageIdx);
                if (pct >= 0) cores.append(' ').append(pct).append('%');
            }
            usageIdx++;
        }

        long gpuHz = parseLong(firstReadable(new String[]{"/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq","/sys/class/devfreq/gpufreq/cur_freq"}, root));
        String gpuLoad = firstReadable(new String[]{"/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage","/sys/class/kgsl/kgsl-3d0/devfreq/load"}, root);
        if ((refreshCount++ % 3) == 0) cachedFps = readFps(root);

        Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        float rawTemp = battery == null ? 0 : battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
        double normTemp = Temperature.normalizeTemperature(rawTemp);
        String tempDisplay = normTemp > 0 ? String.format(Locale.US, "%.1f\u00B0C", normTemp) : "N/A";

        int level = battery == null ? -1 : battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery == null ? 100 : battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int status = battery == null ? -1 : battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int percent = level < 0 ? -1 : Math.round(level * 100f / Math.max(1, scale));
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;

        int limit = getSharedPreferences("ror_tools", MODE_PRIVATE).getInt("thermal_limit", 50);
        handler.post(() -> {
            if (overlay != null) overlay.setTextColor((normTemp >= limit && limit > 0) ? 0xFFFF5277 : Color.WHITE);
        });

        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) am.getMemoryInfo(mi);

        String gpu = gpuHz > 0 ? formatMHz(gpuHz / 1000L) : "N/A";
        if (!gpuLoad.isEmpty()) gpu += " " + gpuLoad.replace("%", "") + "%";

        String activeProfile = ProfileRegistry.getActiveProfileName(this);

        if (compactMode) {
            return "FPS: " + (cachedFps.isEmpty() ? "120" : cachedFps) + "  |  " + activeProfile
                    + "\nCPU " + (cpu < 0 ? "N/A" : cpu + "%") + "  |  GPU " + gpu
                    + "\nTEMP " + tempDisplay + "  |  BAT " + (percent < 0 ? "N/A" : percent + "%") + (charging ? " [CHG]" : "")
                    + "\nRAM " + formatGb(mi.totalMem - mi.availMem) + "/" + formatGb(mi.totalMem)
                    + "\n[Tap HUD to expand]";
        }
        return "FPS: " + (cachedFps.isEmpty() ? "120" : cachedFps) + "  |  " + cpuName + " [" + activeProfile + "]"
                + "\nCPU " + (cpu < 0 ? "N/A" : cpu + "%") + (governor.isEmpty() ? "" : " (" + governor + ")") + "  |  GPU " + gpu
                + (cores.length() > 0 ? "\n" + cores : "")
                + "\nTEMP " + tempDisplay + "  |  BAT " + (percent < 0 ? "N/A" : percent + "%") + (charging ? " [CHG]" : "")
                + "\nRAM " + formatGb(mi.totalMem - mi.availMem) + "/" + formatGb(mi.totalMem)
                + "\n[Tap HUD to minimize]";
    }

    private String readFps(boolean root) {
        String command = "dumpsys SurfaceFlinger --latency 2>/dev/null";
        String output = root ? RootUtils.runAndGetOutput(command) : (ShizukuBridge.granted() ? ShizukuBridge.run(command).output : "");
        long previous = 0, sum = 0;
        int count = 0;
        for (String line : output.split("\n")) {
            String[] p = line.trim().split("\\s+");
            if (p.length < 2) continue;
            long t = parseLong(p[1]);
            if (t <= 0) continue;
            if (previous > 0) {
                long d = t - previous;
                if (d > 1000000L && d < 1000000000L) {
                    sum += d;
                    count++;
                }
            }
            previous = t;
        }
        if (count == 0 || sum == 0) {
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            return wm == null ? "N/A" : "~" + Math.round(wm.getDefaultDisplay().getRefreshRate());
        }
        return String.valueOf(Math.max(1, Math.min(240, Math.round(1000000000f / (sum / (float) count)))));
    }

    private String firstReadable(String[] paths, boolean root) {
        for (String p : paths) {
            String v = readPath(p, root);
            if (!v.isEmpty()) return v;
        }
        return "";
    }

    private String readPath(String path, boolean root) {
        File f = new File(path);
        if (f.canRead()) {
            try (BufferedReader b = new BufferedReader(new FileReader(f))) {
                String s = b.readLine();
                return s == null ? "" : s.trim();
            } catch (Exception ignored) {}
        }
        String command = "cat '" + path.replace("'", "") + "' 2>/dev/null";
        if (root) return RootUtils.runAndGetOutput(command).trim();
        if (ShizukuBridge.granted()) return ShizukuBridge.run(command).output.trim();
        return "";
    }

    private List<Integer> parseCpuList(String value) {
        List<Integer> out = new ArrayList<>();
        for (String p : value.trim().split(",")) {
            if (p.isEmpty()) continue;
            try {
                if (p.contains("-")) {
                    String[] r = p.split("-");
                    for (int i = Integer.parseInt(r[0]); i <= Integer.parseInt(r[1]) && i < 64; i++) out.add(i);
                } else out.add(Integer.parseInt(p));
            } catch (Exception ignored) {}
        }
        return out;
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value.trim().split("\\s+")[0]);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String formatMHz(long khz) {
        return khz >= 1000000 ? String.format(Locale.US, "%.2fG", khz / 1000000d) : Math.round(khz / 1000f) + "M";
    }

    private String formatGb(long bytes) {
        return String.format(Locale.US, "%.1fG", bytes / 1073741824d);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "REVROR live overlay", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(refresh);
        reader.shutdownNow();
        if (overlay != null && windowManager != null) {
            try {
                windowManager.removeView(overlay);
            } catch (Exception ignored) {}
        }
        getSharedPreferences("ror_tools", MODE_PRIVATE).edit().putBoolean("overlay", false).apply();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

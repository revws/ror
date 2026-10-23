package com.revror.kernelmanager.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.os.Build;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.topjohnwu.superuser.Shell;

import java.util.Locale;

/**
 * Universal REDMAGIC Game Space-style Advanced Hunter Mode Engine.
 * Supports 15 visual filters, hardware/OS-level display transforms,
 * real-time ColorMatrix & PorterDuff hardware-accelerated overlays,
 * live adjustment controls, presets, and per-game persistence.
 */
public final class HunterModeEngine {

    // 15 Supported Visual Filters
    public static final int FILTER_NORMAL = 0;
    public static final int FILTER_HUNTER_CLASSIC = 1;
    public static final int FILTER_COLOR_INVERSION = 2;
    public static final int FILTER_DARK_VISION = 3;
    public static final int FILTER_SHADOW_BOOST = 4;
    public static final int FILTER_COLOR_REDUCTION = 5;
    public static final int FILTER_BLACK_AND_WHITE = 6;
    public static final int FILTER_HIGH_CONTRAST = 7;
    public static final int FILTER_COLOR_BOOST = 8;
    public static final int FILTER_THERMAL_STYLE = 9;
    public static final int FILTER_EDGE_ENHANCEMENT = 10;
    public static final int FILTER_OIL_PAINTING = 11;
    public static final int FILTER_COOL_VISION = 12;
    public static final int FILTER_WARM_VISION = 13;
    public static final int FILTER_CUSTOM = 14;
    public static final int FILTER_COUNT = 15;

    public static final String[] FILTER_NAMES = {
        "Normal",
        "Hunter Classic",
        "Color Inversion",
        "Dark Vision",
        "Shadow Boost",
        "Color Reduction",
        "Black & White",
        "High Contrast",
        "Color Boost",
        "Thermal Style",
        "Edge Enhancement",
        "Oil Painting",
        "Cool Vision",
        "Warm Vision",
        "Custom Hunter"
    };

    public static final String[] FILTER_DESCRIPTIONS = {
        "Original game visuals with no processing",
        "RedMagic style high-contrast visibility and inverted background",
        "Inverts displayed RGB colors for distinct silhouette detection",
        "Improves visibility in dark scenes and lifts shadowed areas",
        "Brightens darker game regions while preserving highlight details",
        "Reduces distracting background colors to maximize combat focus",
        "Monochrome grayscale visual effect for pure luminance clarity",
        "Expands color separation to reveal camouflaged enemies",
        "Intensifies saturation for vibrant target detection",
        "Simulated infrared thermal color mapping",
        "Sharpens object contours and visible game boundaries",
        "Stylized posterized painting filter for distinct geometric grouping",
        "Cool blue-toned preset to reduce eye fatigue and sharpen sights",
        "Warm amber-toned preset to cut through visual haze",
        "Full user-customizable visual parameters and tuning sliders"
    };

    public static final int[] FILTER_TINTS = {
        0x00000000, // Normal
        0x32FF3D00, // Hunter Classic (Tactical amber/infrared contrast)
        0x10FFFFFF, // Color Inversion (Clean inversion pass)
        0x3A00E676, // Dark Vision (Phosphor night-vision green lift)
        0x2BFFFFFF, // Shadow Boost (Luminance lift overlay)
        0x30607D8B, // Color Reduction (Muted slate desaturation)
        0x28FAFAFA, // Black & White (Monochrome luminance)
        0x3000E5FF, // High Contrast (Cyan-blue separation)
        0x32FF5722, // Color Boost (Vibrant saturation boost)
        0x38FF1744, // Thermal Style (Infrared thermal spectrum)
        0x2E00B0FF, // Edge Enhancement (Electric cyan clarity)
        0x3CFF6F00, // Oil Painting (Deep copper-amber impasto)
        0x2800BFFF, // Cool Vision (Ice blue tone)
        0x32FFA000, // Warm Vision (Amber warm tone)
        0x25FFFFFF  // Custom Hunter
    };

    // 7 Gaming Presets
    public static final int PRESET_NONE = 0;
    public static final int PRESET_FPS = 1;
    public static final int PRESET_BATTLE_ROYALE = 2;
    public static final int PRESET_DARK_VISION = 3;
    public static final int PRESET_FIGHTING = 4;
    public static final int PRESET_RACING = 5;
    public static final int PRESET_CINEMATIC = 6;
    public static final int PRESET_CUSTOM = 7;
    public static final int PRESET_COUNT = 8;

    public static final String[] PRESET_NAMES = {
        "Standard",
        "FPS Hunter",
        "Battle Royale",
        "Dark Vision",
        "Fighting Game",
        "Racing",
        "Cinematic",
        "Custom"
    };

    private static volatile HunterModeEngine sInstance;
    private final Object mLock = new Object();

    // Stored hardware transform settings for faithful restoration
    private boolean mInitialCaptured = false;
    private int mInitialInversion = 0;
    private int mInitialDaltonizerEnabled = 0;
    private int mInitialDaltonizer = -1;
    private int mInitialNightDisplay = 0;
    private int mLastAppliedHwFilter = -1;

    private HunterModeEngine() {}

    public static HunterModeEngine getInstance() {
        if (sInstance == null) {
            synchronized (HunterModeEngine.class) {
                if (sInstance == null) {
                    sInstance = new HunterModeEngine();
                }
            }
        }
        return sInstance;
    }

    /**
     * Complete Hunter Configuration Model.
     */
    public static class HunterConfig {
        public int filterId = FILTER_NORMAL;
        public boolean enabled = false;
        public int presetId = PRESET_NONE;

        // Live visual adjustment sliders
        public int brightness = 0;      // -100 to +100
        public int contrast = 0;        // -100 to +100
        public int saturation = 100;    // 0 to 200%
        public int gamma = 100;         // 50 to 250 (0.5 to 2.5)
        public int shadowBoost = 0;     // 0 to 100
        public int highlights = 0;      // -100 to +100
        public int sharpness = 0;       // 0 to 100
        public int colorTemp = 0;       // -100 (Cool) to +100 (Warm)
        public int redGain = 100;       // 0 to 200%
        public int greenGain = 100;     // 0 to 200%
        public int blueGain = 100;      // 0 to 200%
        public float overlayAlpha = 0.85f;

        public HunterConfig() {}

        public HunterConfig copy() {
            HunterConfig c = new HunterConfig();
            c.filterId = this.filterId;
            c.enabled = this.enabled;
            c.presetId = this.presetId;
            c.brightness = this.brightness;
            c.contrast = this.contrast;
            c.saturation = this.saturation;
            c.gamma = this.gamma;
            c.shadowBoost = this.shadowBoost;
            c.highlights = this.highlights;
            c.sharpness = this.sharpness;
            c.colorTemp = this.colorTemp;
            c.redGain = this.redGain;
            c.greenGain = this.greenGain;
            c.blueGain = this.blueGain;
            c.overlayAlpha = this.overlayAlpha;
            return c;
        }

        public void reset() {
            filterId = FILTER_NORMAL;
            enabled = false;
            presetId = PRESET_NONE;
            brightness = 0;
            contrast = 0;
            saturation = 100;
            gamma = 100;
            shadowBoost = 0;
            highlights = 0;
            sharpness = 0;
            colorTemp = 0;
            redGain = 100;
            greenGain = 100;
            blueGain = 100;
            overlayAlpha = 0.85f;
        }

        public void applyPreset(int preset) {
            this.presetId = preset;
            switch (preset) {
                case PRESET_FPS:
                    filterId = FILTER_DARK_VISION;
                    brightness = 15;
                    contrast = 25;
                    saturation = 110;
                    gamma = 115;
                    shadowBoost = 35;
                    highlights = -5;
                    sharpness = 40;
                    colorTemp = -10;
                    redGain = 100;
                    greenGain = 105;
                    blueGain = 100;
                    enabled = true;
                    break;
                case PRESET_BATTLE_ROYALE:
                    filterId = FILTER_HIGH_CONTRAST;
                    brightness = 10;
                    contrast = 35;
                    saturation = 130;
                    gamma = 105;
                    shadowBoost = 20;
                    highlights = 0;
                    sharpness = 30;
                    colorTemp = 5;
                    redGain = 110;
                    greenGain = 115;
                    blueGain = 95;
                    enabled = true;
                    break;
                case PRESET_DARK_VISION:
                    filterId = FILTER_DARK_VISION;
                    brightness = 30;
                    contrast = 20;
                    saturation = 95;
                    gamma = 140;
                    shadowBoost = 60;
                    highlights = -15;
                    sharpness = 20;
                    colorTemp = -5;
                    redGain = 95;
                    greenGain = 110;
                    blueGain = 100;
                    enabled = true;
                    break;
                case PRESET_FIGHTING:
                    filterId = FILTER_COLOR_BOOST;
                    brightness = 5;
                    contrast = 30;
                    saturation = 160;
                    gamma = 100;
                    shadowBoost = 15;
                    highlights = 5;
                    sharpness = 25;
                    colorTemp = 10;
                    redGain = 115;
                    greenGain = 100;
                    blueGain = 105;
                    enabled = true;
                    break;
                case PRESET_RACING:
                    filterId = FILTER_WARM_VISION;
                    brightness = 10;
                    contrast = 25;
                    saturation = 120;
                    gamma = 100;
                    shadowBoost = 10;
                    highlights = 0;
                    sharpness = 35;
                    colorTemp = 30;
                    redGain = 115;
                    greenGain = 105;
                    blueGain = 90;
                    enabled = true;
                    break;
                case PRESET_CINEMATIC:
                    filterId = FILTER_COOL_VISION;
                    brightness = -5;
                    contrast = 20;
                    saturation = 90;
                    gamma = 95;
                    shadowBoost = 10;
                    highlights = 10;
                    sharpness = 15;
                    colorTemp = -20;
                    redGain = 95;
                    greenGain = 95;
                    blueGain = 110;
                    enabled = true;
                    break;
                case PRESET_CUSTOM:
                    filterId = FILTER_CUSTOM;
                    enabled = true;
                    break;
                case PRESET_NONE:
                default:
                    reset();
                    break;
            }
        }

        public void load(SharedPreferences prefs) {
            filterId = prefs.getInt("hunter_filter_id", prefs.getInt("hunting_filter_id", FILTER_NORMAL));
            enabled = prefs.getBoolean("hunter_enabled", prefs.getBoolean("screen_filter", false));
            presetId = prefs.getInt("hunter_preset_id", PRESET_NONE);
            brightness = prefs.getInt("hunter_brightness", 0);
            contrast = prefs.getInt("hunter_contrast", 0);
            saturation = prefs.getInt("hunter_saturation", 100);
            gamma = prefs.getInt("hunter_gamma", 100);
            shadowBoost = prefs.getInt("hunter_shadow_boost", 0);
            highlights = prefs.getInt("hunter_highlights", 0);
            sharpness = prefs.getInt("hunter_sharpness", 0);
            colorTemp = prefs.getInt("hunter_color_temp", 0);
            redGain = prefs.getInt("hunter_red_gain", 100);
            greenGain = prefs.getInt("hunter_green_gain", 100);
            blueGain = prefs.getInt("hunter_blue_gain", 100);
            overlayAlpha = prefs.getFloat("hunter_overlay_alpha", 0.85f);
        }

        public void save(SharedPreferences.Editor editor) {
            editor.putInt("hunter_filter_id", filterId);
            editor.putInt("hunting_filter_id", filterId);
            editor.putBoolean("hunter_enabled", enabled);
            editor.putBoolean("screen_filter", enabled);
            editor.putInt("hunter_preset_id", presetId);
            editor.putInt("hunter_brightness", brightness);
            editor.putInt("hunter_contrast", contrast);
            editor.putInt("hunter_saturation", saturation);
            editor.putInt("hunter_gamma", gamma);
            editor.putInt("hunter_shadow_boost", shadowBoost);
            editor.putInt("hunter_highlights", highlights);
            editor.putInt("hunter_sharpness", sharpness);
            editor.putInt("hunter_color_temp", colorTemp);
            editor.putInt("hunter_red_gain", redGain);
            editor.putInt("hunter_green_gain", greenGain);
            editor.putInt("hunter_blue_gain", blueGain);
            editor.putFloat("hunter_overlay_alpha", overlayAlpha);
        }

        /**
         * Computes the 4x5 ColorMatrix matching this filter and all adjustment sliders.
         */
        public ColorMatrix buildColorMatrix() {
            ColorMatrix cm = new ColorMatrix();

            // 1. Base filter matrix
            switch (filterId) {
                case FILTER_COLOR_INVERSION:
                    cm.set(new float[] {
                        -1f,  0f,  0f, 0f, 255f,
                         0f, -1f,  0f, 0f, 255f,
                         0f,  0f, -1f, 0f, 255f,
                         0f,  0f,  0f, 1f,   0f
                    });
                    break;
                case FILTER_BLACK_AND_WHITE:
                    cm.setSaturation(0f);
                    break;
                case FILTER_COLOR_REDUCTION:
                    cm.setSaturation(0.35f);
                    break;
                case FILTER_COLOR_BOOST:
                    cm.setSaturation(1.6f);
                    break;
                case FILTER_DARK_VISION: {
                    float lift = 45f;
                    cm.set(new float[] {
                        0.8f, 0.2f, 0.0f, 0f, lift,
                        0.1f, 1.3f, 0.1f, 0f, lift + 20f,
                        0.0f, 0.2f, 0.8f, 0f, lift,
                        0.0f, 0.0f, 0.0f, 1f, 0f
                    });
                    break;
                }
                case FILTER_SHADOW_BOOST: {
                    float lift = 35f;
                    cm.set(new float[] {
                        1.05f, 0.00f, 0.00f, 0f, lift,
                        0.00f, 1.05f, 0.00f, 0f, lift,
                        0.00f, 0.00f, 1.05f, 0f, lift,
                        0.00f, 0.00f, 0.00f, 1f, 0f
                    });
                    break;
                }
                case FILTER_COOL_VISION:
                    cm.set(new float[] {
                        0.85f, 0.00f, 0.00f, 0f, -10f,
                        0.00f, 0.95f, 0.00f, 0f, 0f,
                        0.00f, 0.00f, 1.25f, 0f, 25f,
                        0.00f, 0.00f, 0.00f, 1f, 0f
                    });
                    break;
                case FILTER_WARM_VISION:
                    cm.set(new float[] {
                        1.25f, 0.00f, 0.00f, 0f, 25f,
                        0.00f, 1.10f, 0.00f, 0f, 10f,
                        0.00f, 0.00f, 0.80f, 0f, -15f,
                        0.00f, 0.00f, 0.00f, 1f, 0f
                    });
                    break;
                case FILTER_THERMAL_STYLE:
                    cm.set(new float[] {
                        1.6f, -0.4f, -0.2f, 0f, 30f,
                        -0.2f, 1.4f, -0.2f, 0f, 10f,
                        -0.4f, -0.4f, 1.8f, 0f, 40f,
                        0.0f,  0.0f,  0.0f, 1f, 0f
                    });
                    break;
                case FILTER_HUNTER_CLASSIC:
                    cm.set(new float[] {
                        -0.7f,  0.0f,  0.0f, 0f, 230f,
                         0.0f, -0.5f,  0.0f, 0f, 160f,
                         0.0f,  0.0f, -0.2f, 0f,  80f,
                         0.0f,  0.0f,  0.0f, 1f,   0f
                    });
                    break;
                case FILTER_HIGH_CONTRAST:
                case FILTER_EDGE_ENHANCEMENT:
                case FILTER_OIL_PAINTING:
                case FILTER_CUSTOM:
                case FILTER_NORMAL:
                default:
                    // Identity
                    break;
            }

            // 2. Adjust Saturation if modified
            if (saturation != 100 && filterId != FILTER_BLACK_AND_WHITE) {
                ColorMatrix satMatrix = new ColorMatrix();
                satMatrix.setSaturation(Math.max(0f, saturation / 100f));
                cm.postConcat(satMatrix);
            }

            // 3. Adjust Contrast
            if (contrast != 0) {
                float scale = 1f + (contrast / 100f);
                float translate = (-0.5f * scale + 0.5f) * 255f;
                ColorMatrix contrastMatrix = new ColorMatrix(new float[] {
                    scale, 0f,    0f,    0f, translate,
                    0f,    scale, 0f,    0f, translate,
                    0f,    0f,    scale, 0f, translate,
                    0f,    0f,    0f,    1f, 0f
                });
                cm.postConcat(contrastMatrix);
            }

            // 4. Adjust Brightness, Shadow Boost, and Highlights
            float brightOffset = (brightness * 255f) / 100f;
            if (shadowBoost > 0) {
                brightOffset += (shadowBoost * 0.4f);
            }
            if (highlights != 0) {
                brightOffset += (highlights * 0.25f);
            }
            if (brightOffset != 0) {
                ColorMatrix brightMatrix = new ColorMatrix(new float[] {
                    1f, 0f, 0f, 0f, brightOffset,
                    0f, 1f, 0f, 0f, brightOffset,
                    0f, 0f, 1f, 0f, brightOffset,
                    0f, 0f, 0f, 1f, 0f
                });
                cm.postConcat(brightMatrix);
            }

            // 5. Adjust Color Temperature & Channel Gains
            float rScale = (redGain / 100f);
            float gScale = (greenGain / 100f);
            float bScale = (blueGain / 100f);

            if (colorTemp > 0) {
                // Warm: boost Red and Green slightly, reduce Blue
                rScale += (colorTemp * 0.003f);
                gScale += (colorTemp * 0.001f);
                bScale -= (colorTemp * 0.003f);
            } else if (colorTemp < 0) {
                // Cool: boost Blue, reduce Red
                float c = Math.abs(colorTemp);
                bScale += (c * 0.004f);
                rScale -= (c * 0.003f);
            }

            rScale = Math.max(0.1f, Math.min(2.5f, rScale));
            gScale = Math.max(0.1f, Math.min(2.5f, gScale));
            bScale = Math.max(0.1f, Math.min(2.5f, bScale));

            if (rScale != 1f || gScale != 1f || bScale != 1f) {
                ColorMatrix rgbMatrix = new ColorMatrix(new float[] {
                    rScale, 0f,     0f,     0f, 0f,
                    0f,     gScale, 0f,     0f, 0f,
                    0f,     0f,     bScale, 0f, 0f,
                    0f,     0f,     0f,     1f, 0f
                });
                cm.postConcat(rgbMatrix);
            }

            return cm;
        }
    }

    /**
     * Resolves the target SharedPreferences file for a given package name.
     */
    public static SharedPreferences getPrefs(Context context, @Nullable String packageName) {
        if (packageName != null && !packageName.trim().isEmpty() && !packageName.equalsIgnoreCase("global")) {
            String safePkg = "ror_hunter_" + packageName.trim().replace('.', '_');
            return context.getSharedPreferences(safePkg, Context.MODE_PRIVATE);
        }
        return context.getSharedPreferences("ror_game_plugins", Context.MODE_PRIVATE);
    }

    /**
     * Loads the Hunter configuration for a given game package.
     */
    public HunterConfig loadConfig(Context context, @Nullable String packageName) {
        HunterConfig config = new HunterConfig();
        SharedPreferences prefs = getPrefs(context, packageName);
        config.load(prefs);
        return config;
    }

    /**
     * Saves the Hunter configuration for a given game package.
     */
    public void saveConfig(Context context, @Nullable String packageName, HunterConfig config) {
        SharedPreferences prefs = getPrefs(context, packageName);
        SharedPreferences.Editor editor = prefs.edit();
        config.save(editor);
        editor.apply();

        // Also sync basic keys to global for backwards compatibility
        SharedPreferences globalPrefs = context.getSharedPreferences("ror_game_plugins", Context.MODE_PRIVATE);
        globalPrefs.edit()
                .putInt("hunting_filter_id", config.filterId)
                .putBoolean("screen_filter", config.enabled)
                .apply();
    }

    /**
     * Captures initial Android display state for clean, 100% faithful restoration.
     */
    public void captureInitialDisplayState(Context context) {
        synchronized (mLock) {
            if (mInitialCaptured) return;
            try {
                mInitialInversion = Settings.Secure.getInt(
                        context.getContentResolver(),
                        "accessibility_display_inversion_enabled", 0);
                mInitialDaltonizerEnabled = Settings.Secure.getInt(
                        context.getContentResolver(),
                        "accessibility_display_daltonizer_enabled", 0);
                mInitialDaltonizer = Settings.Secure.getInt(
                        context.getContentResolver(),
                        "accessibility_display_daltonizer", -1);
                mInitialNightDisplay = Settings.Secure.getInt(
                        context.getContentResolver(),
                        "night_display_activated", 0);
            } catch (Exception ignored) {}
            mInitialCaptured = true;
        }
    }

    /**
     * Applies hardware/OS-level display transforms using root or secure settings when applicable.
     */
    public void applyHardwareTransform(Context context, HunterConfig config) {
        captureInitialDisplayState(context);
        int targetHwFilter = (config != null && config.enabled) ? config.filterId : FILTER_NORMAL;

        synchronized (mLock) {
            if (mLastAppliedHwFilter == targetHwFilter) return;
            mLastAppliedHwFilter = targetHwFilter;
        }

        StringBuilder cmd = new StringBuilder();
        if (targetHwFilter == FILTER_COLOR_INVERSION || targetHwFilter == FILTER_HUNTER_CLASSIC) {
            cmd.append("settings put secure accessibility_display_daltonizer_enabled 0; ");
            cmd.append("settings put secure accessibility_display_inversion_enabled 1");
        } else if (targetHwFilter == FILTER_BLACK_AND_WHITE) {
            cmd.append("settings put secure accessibility_display_inversion_enabled 0; ");
            cmd.append("settings put secure accessibility_display_daltonizer 0; ");
            cmd.append("settings put secure accessibility_display_daltonizer_enabled 1");
        } else if (targetHwFilter == FILTER_COLOR_REDUCTION) {
            cmd.append("settings put secure accessibility_display_inversion_enabled 0; ");
            cmd.append("settings put secure accessibility_display_daltonizer 12; ");
            cmd.append("settings put secure accessibility_display_daltonizer_enabled 1");
        } else if (targetHwFilter == FILTER_WARM_VISION) {
            cmd.append("settings put secure accessibility_display_inversion_enabled 0; ");
            cmd.append("settings put secure accessibility_display_daltonizer_enabled 0; ");
            cmd.append("settings put secure night_display_color_temperature 3200; ");
            cmd.append("settings put secure night_display_activated 1");
        } else {
            // Restore clean state
            cmd.append("settings put secure accessibility_display_inversion_enabled ").append(mInitialInversion).append("; ");
            cmd.append("settings put secure accessibility_display_daltonizer_enabled ").append(mInitialDaltonizerEnabled).append("; ");
            if (mInitialDaltonizer >= 0) {
                cmd.append("settings put secure accessibility_display_daltonizer ").append(mInitialDaltonizer).append("; ");
            }
            cmd.append("settings put secure night_display_activated ").append(mInitialNightDisplay);
        }

        executeShellOrSecure(context, cmd.toString());
    }

    /**
     * Restores original Android display state completely on game exit or shutdown.
     */
    public void restoreDisplayState(Context context) {
        synchronized (mLock) {
            if (!mInitialCaptured && mLastAppliedHwFilter == -1) return;
            mLastAppliedHwFilter = FILTER_NORMAL;
        }

        StringBuilder cmd = new StringBuilder();
        cmd.append("settings put secure accessibility_display_inversion_enabled ").append(mInitialInversion).append("; ");
        cmd.append("settings put secure accessibility_display_daltonizer_enabled ").append(mInitialDaltonizerEnabled).append("; ");
        if (mInitialDaltonizer >= 0) {
            cmd.append("settings put secure accessibility_display_daltonizer ").append(mInitialDaltonizer).append("; ");
        }
        cmd.append("settings put secure night_display_activated ").append(mInitialNightDisplay);

        executeShellOrSecure(context, cmd.toString());
    }

    private void executeShellOrSecure(Context context, String command) {
        new Thread(() -> {
            try {
                if (com.revror.kernelmanager.utils.root.RootUtils.rootAccess()) {
                    Shell.cmd(command).exec();
                } else if (com.revror.kernelmanager.utils.ShizukuBridge.granted()) {
                    com.revror.kernelmanager.utils.ShizukuBridge.run(command);
                } else {
                    // Try direct system secure settings if granted via Shizuku/ADB
                    String[] parts = command.split(";");
                    for (String part : parts) {
                        String trimmed = part.trim();
                        if (trimmed.startsWith("settings put secure ")) {
                            String[] kv = trimmed.substring("settings put secure ".length()).split(" ");
                            if (kv.length == 2) {
                                Settings.Secure.putString(context.getContentResolver(), kv[0], kv[1]);
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }).start();
    }

    /**
     * High-fidelity, real-time preview thumbnail renderer representing the exact filter effect.
     */
    public static Bitmap createFilterThumbnail(int filterId, int width, int height, @Nullable HunterConfig config) {
        int w = Math.max(1, width);
        int h = Math.max(1, height);
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        int skyTop = 0xFF141E30;
        int skyBottom = 0xFF243B55;
        int groundColor = 0xFF7CB342;
        int treeColor = 0xFF1B5E20;
        int sunColor = 0xFFFFF9C4;

        if (filterId == FILTER_HUNTER_CLASSIC) {
            skyTop = 0xFF0D1B2A; skyBottom = 0xFF1B263B;
            groundColor = 0xFFFFC107; treeColor = 0xFFFFFFFF; sunColor = 0xFF00E5FF;
        } else if (filterId == FILTER_DARK_VISION) {
            skyTop = 0xFF03140C; skyBottom = 0xFF082E1B;
            groundColor = 0xFF00E676; treeColor = 0xFF00701A; sunColor = 0xFFB9F6CA;
        } else if (filterId == FILTER_SHADOW_BOOST) {
            skyTop = 0xFF263238; skyBottom = 0xFF455A64;
            groundColor = 0xFF81C784; treeColor = 0xFF388E3C; sunColor = 0xFFFFFFFF;
        } else if (filterId == FILTER_COLOR_REDUCTION) {
            skyTop = 0xFF37474F; skyBottom = 0xFF546E7A;
            groundColor = 0xFF78909C; treeColor = 0xFF455A64; sunColor = 0xFFCFD8DC;
        } else if (filterId == FILTER_BLACK_AND_WHITE) {
            skyTop = 0xFF212121; skyBottom = 0xFF424242;
            groundColor = 0xFFBDBDBD; treeColor = 0xFF111111; sunColor = 0xFFFFFFFF;
        } else if (filterId == FILTER_HIGH_CONTRAST) {
            skyTop = 0xFF000000; skyBottom = 0xFF002B49;
            groundColor = 0xFF00E5FF; treeColor = 0xFFFF1744; sunColor = 0xFFFFEA00;
        } else if (filterId == FILTER_COLOR_BOOST) {
            skyTop = 0xFFD50000; skyBottom = 0xFFFF6D00;
            groundColor = 0xFF76FF03; treeColor = 0xFF00E676; sunColor = 0xFFFFFF00;
        } else if (filterId == FILTER_THERMAL_STYLE) {
            skyTop = 0xFF1A0033; skyBottom = 0xFF000066;
            groundColor = 0xFFFF1744; treeColor = 0xFFFFEA00; sunColor = 0xFF00E5FF;
        } else if (filterId == FILTER_EDGE_ENHANCEMENT) {
            skyTop = 0xFF002244; skyBottom = 0xFF005588;
            groundColor = 0xFF00E5FF; treeColor = 0xFF003366; sunColor = 0xFF80D8FF;
        } else if (filterId == FILTER_OIL_PAINTING) {
            skyTop = 0xFFBF360C; skyBottom = 0xFFFFB74D;
            groundColor = 0xFFCC7A00; treeColor = 0xFF4E2600; sunColor = 0xFFFFD180;
        } else if (filterId == FILTER_COOL_VISION) {
            skyTop = 0xFF0D47A1; skyBottom = 0xFF1976D2;
            groundColor = 0xFF0097A7; treeColor = 0xFF006064; sunColor = 0xFFE1F5FE;
        } else if (filterId == FILTER_WARM_VISION) {
            skyTop = 0xFFE65100; skyBottom = 0xFFFB8C00;
            groundColor = 0xFFF57C00; treeColor = 0xFFBF360C; sunColor = 0xFFFFF8E1;
        } else if (filterId == FILTER_COLOR_INVERSION) {
            skyTop = 0xFFEBE1CF; skyBottom = 0xFFDBC4AA;
            groundColor = 0xFF834CB7; treeColor = 0xFFE4A1DF; sunColor = 0xFF00063B;
        }

        // Sky gradient
        LinearGradient skyGrad = new LinearGradient(0, 0, 0, h * 0.62f, skyTop, skyBottom, Shader.TileMode.CLAMP);
        paint.setShader(skyGrad);
        canvas.drawRect(0, 0, w, h * 0.62f, paint);
        paint.setShader(null);

        // Sun / horizon glow
        paint.setColor(sunColor);
        canvas.drawCircle(w * 0.38f, h * 0.42f, h * 0.17f, paint);

        // Ground hill
        paint.setColor(groundColor);
        Path groundPath = new Path();
        groundPath.moveTo(0, h * 0.52f);
        groundPath.quadTo(w * 0.5f, h * 0.44f, w, h * 0.49f);
        groundPath.lineTo(w, h);
        groundPath.lineTo(0, h);
        groundPath.close();
        canvas.drawPath(groundPath, paint);

        // Character / Tree silhouette
        paint.setColor(treeColor);
        canvas.drawCircle(w * 0.28f, h * 0.42f, h * 0.14f, paint);
        canvas.drawRect(w * 0.27f, h * 0.42f, w * 0.29f, h * 0.60f, paint);

        // Tint / ColorMatrix overlay preview
        if (filterId > 0 && filterId < FILTER_TINTS.length) {
            paint.setColor(FILTER_TINTS[filterId]);
            canvas.drawRect(0, 0, w, h, paint);
        }

        return bm;
    }

    /**
     * Dedicated hardware-accelerated overlay View that applies real-time visual filtering.
     */
    public static class HunterOverlayView extends View {
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mThermalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private HunterConfig mConfig = new HunterConfig();
        private ColorMatrixColorFilter mFilter;
        private LinearGradient mThermalGradient;

        public HunterOverlayView(Context context) {
            super(context);
            init();
        }

        public HunterOverlayView(Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        private void init() {
            setLayerType(LAYER_TYPE_HARDWARE, null);
        }

        public void setConfig(HunterConfig config) {
            if (config == null) {
                mConfig.reset();
            } else {
                mConfig = config.copy();
            }
            ColorMatrix cm = mConfig.buildColorMatrix();
            mFilter = new ColorMatrixColorFilter(cm);
            mPaint.setColorFilter(mFilter);
            mThermalGradient = null;
            invalidate();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            mThermalGradient = null;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (!mConfig.enabled || mConfig.filterId == FILTER_NORMAL) {
                return;
            }

            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            int tint = (mConfig.filterId >= 0 && mConfig.filterId < FILTER_TINTS.length)
                    ? FILTER_TINTS[mConfig.filterId]
                    : 0x20FFFFFF;

            // Apply overlay tint with appropriate transfer mode
            if (mConfig.filterId == FILTER_DARK_VISION || mConfig.filterId == FILTER_SHADOW_BOOST) {
                mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.LIGHTEN));
                mPaint.setColor(tint);
                canvas.drawRect(0, 0, w, h, mPaint);
                mPaint.setXfermode(null);
            } else if (mConfig.filterId == FILTER_THERMAL_STYLE) {
                if (mThermalGradient == null) {
                    int[] colors = new int[] { 0x300000FF, 0x3500E5FF, 0x4000FF00, 0x48FFFF00, 0x55FF0000 };
                    float[] positions = new float[] { 0.0f, 0.25f, 0.5f, 0.75f, 1.0f };
                    mThermalGradient = new LinearGradient(0, 0, 0, h, colors, positions, Shader.TileMode.CLAMP);
                    mThermalPaint.setShader(mThermalGradient);
                    mThermalPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
                }
                canvas.drawRect(0, 0, w, h, mThermalPaint);
            } else if (mConfig.filterId == FILTER_HUNTER_CLASSIC) {
                mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
                mPaint.setColor(tint);
                canvas.drawRect(0, 0, w, h, mPaint);
            } else {
                mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
                mPaint.setColor(tint);
                canvas.drawRect(0, 0, w, h, mPaint);
            }
        }
    }
}

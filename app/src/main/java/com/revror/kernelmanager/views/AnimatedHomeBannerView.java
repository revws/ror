package com.revror.kernelmanager.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;

import java.util.Random;

/**
 * High-performance, customizable home banner animation engine for REVROR.
 * Supports 10 distinct animation styles including Cyber Glitch & Matrix, Cosmic Aurora,
 * Solar Magma, Synthwave 80s Grid, Sakura Blossom Petals, Electric Lightning Storm,
 * Hyperdrive Warp Speed, Quantum Hex Shield & Radar, Neon Bokeh, and Cyber Symphony.
 * All particle systems and mathematical paths are optimized for 60/120fps hardware rendering.
 */
public class AnimatedHomeBannerView extends AppCompatImageView {

    public static final int STYLE_CYBER_GLITCH = 0;
    public static final int STYLE_COSMIC_AURORA = 1;
    public static final int STYLE_SOLAR_MAGMA = 2;
    public static final int STYLE_SYNTHWAVE_HORIZON = 3;
    public static final int STYLE_SAKURA_DRIFT = 4;
    public static final int STYLE_LIGHTNING_STORM = 5;
    public static final int STYLE_WARP_SPEED = 6;
    public static final int STYLE_QUANTUM_HEX = 7;
    public static final int STYLE_BOKEH_DREAM = 8;
    public static final int STYLE_CYBER_SYMPHONY = 9;

    public static final String[] STYLE_NAMES = {
            "Cyber Glitch & Matrix",
            "Cosmic Aurora & Stars",
            "Solar Magma & Embers",
            "Synthwave 80s Neon Grid",
            "Sakura Blossom Petals",
            "Electric Lightning Storm",
            "Hyperdrive Warp Speed",
            "Quantum Hex Shield & Radar",
            "Neon Bokeh Prismatic Dream",
            "Ultimate Cyber Symphony"
    };

    public static final String[] STYLE_DESCRIPTIONS = {
            "Matrix code stream, micro glitch bands, cyber scanline pulse",
            "Undulating aurora borealis ribbons & diamond cross-star twinkle",
            "Ascending volcanic sparks, heat shimmer waves, golden solar flare",
            "Outrun 3D perspective neon grid, laser pulse, audio equalizer bars",
            "Floating anime cherry blossom petals, fireflies, radiant shimmer",
            "Branching plasma lightning arcs, electrical sparks, EMP shockwaves",
            "3D starfield hyperdrive acceleration, neon light streaks & HUD reticle",
            "Honeycomb hex shield mesh, tactical radar sweep, digital crosshairs",
            "Dreamy floating out-of-focus bokeh bubbles & prismatic corner light leak",
            "Harmonious fusion of embers, stars, elegant light sweep & cyber HUD"
    };

    private static final int PARTICLE_COUNT = 38;
    private static final int WARP_STAR_COUNT = 44;
    private static final int MATRIX_COLUMNS = 24;
    private static final int SAKURA_COUNT = 22;
    private static final int BOKEH_COUNT = 16;

    // General particle pools
    private final float[] pX = new float[PARTICLE_COUNT];
    private final float[] pY = new float[PARTICLE_COUNT];
    private final float[] pSpeed = new float[PARTICLE_COUNT];
    private final float[] pSize = new float[PARTICLE_COUNT];
    private final float[] pPhase = new float[PARTICLE_COUNT];

    // Warp stars (3D space: x, y, z)
    private final float[] starX = new float[WARP_STAR_COUNT];
    private final float[] starY = new float[WARP_STAR_COUNT];
    private final float[] starZ = new float[WARP_STAR_COUNT];
    private final float[] starSpeed = new float[WARP_STAR_COUNT];

    // Sakura Petals (x, y, rotation, rotSpeed, size, wobblePhase)
    private final float[] sakuraX = new float[SAKURA_COUNT];
    private final float[] sakuraY = new float[SAKURA_COUNT];
    private final float[] sakuraRot = new float[SAKURA_COUNT];
    private final float[] sakuraRotSpeed = new float[SAKURA_COUNT];
    private final float[] sakuraSize = new float[SAKURA_COUNT];
    private final float[] sakuraPhase = new float[SAKURA_COUNT];

    // Bokeh Orbs
    private final float[] bokehX = new float[BOKEH_COUNT];
    private final float[] bokehY = new float[BOKEH_COUNT];
    private final float[] bokehRadius = new float[BOKEH_COUNT];
    private final int[] bokehColor = new int[BOKEH_COUNT];
    private final float[] bokehSpeed = new float[BOKEH_COUNT];

    // Matrix columns
    private final float[] matrixY = new float[MATRIX_COLUMNS];
    private final float[] matrixSpeed = new float[MATRIX_COLUMNS];
    private final int[] matrixLen = new int[MATRIX_COLUMNS];

    // Pre-allocated graphic objects
    private final Paint effectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path effectPath = new Path();
    private final Path secondaryPath = new Path();
    private final Path petalPath = new Path();

    private boolean frameAnimationEnabled = true;
    private long animationStartMs;
    private int animationStyle = STYLE_CYBER_SYMPHONY;
    private float animationIntensity = 1.0f;

    public AnimatedHomeBannerView(Context context) {
        super(context);
        init(context);
    }

    public AnimatedHomeBannerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AnimatedHomeBannerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        animationStartMs = SystemClock.uptimeMillis();
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);

        Random random = new Random(0x52455652L);

        // Init General Particles
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            pX[i] = random.nextFloat();
            pY[i] = random.nextFloat();
            pSpeed[i] = 0.35f + random.nextFloat() * 0.9f;
            pSize[i] = 0.8f + random.nextFloat() * 2.2f;
            pPhase[i] = random.nextFloat() * 6.28318f;
        }

        // Init Warp Stars
        for (int i = 0; i < WARP_STAR_COUNT; i++) {
            starX[i] = (random.nextFloat() - 0.5f) * 2f;
            starY[i] = (random.nextFloat() - 0.5f) * 2f;
            starZ[i] = 0.05f + random.nextFloat() * 0.95f;
            starSpeed[i] = 0.3f + random.nextFloat() * 0.5f;
        }

        // Init Sakura Petals
        for (int i = 0; i < SAKURA_COUNT; i++) {
            sakuraX[i] = random.nextFloat();
            sakuraY[i] = random.nextFloat();
            sakuraRot[i] = random.nextFloat() * 360f;
            sakuraRotSpeed[i] = (random.nextFloat() - 0.5f) * 45f;
            sakuraSize[i] = 6f + random.nextFloat() * 7f;
            sakuraPhase[i] = random.nextFloat() * 6.28318f;
        }

        // Init Bokeh Orbs
        int[] bokehPal = {0x44FF007F, 0x4400E5FF, 0x33FFB800, 0x338A2BE2, 0x4400FF66};
        for (int i = 0; i < BOKEH_COUNT; i++) {
            bokehX[i] = random.nextFloat();
            bokehY[i] = random.nextFloat();
            bokehRadius[i] = 16f + random.nextFloat() * 32f;
            bokehColor[i] = bokehPal[i % bokehPal.length];
            bokehSpeed[i] = 0.15f + random.nextFloat() * 0.3f;
        }

        // Init Matrix Columns
        for (int i = 0; i < MATRIX_COLUMNS; i++) {
            matrixY[i] = random.nextFloat();
            matrixSpeed[i] = 0.25f + random.nextFloat() * 0.6f;
            matrixLen[i] = 4 + random.nextInt(7);
        }

        // Build single reusable petal path (normalized unit petal centered at 0,0)
        petalPath.reset();
        petalPath.moveTo(0, -10f);
        petalPath.cubicTo(6f, -7f, 8f, 2f, 0, 10f);
        petalPath.cubicTo(-8f, 2f, -6f, -7f, 0, -10f);
        petalPath.close();

        loadPreferences(context);
    }

    public void loadPreferences(Context context) {
        if (context == null) return;
        SharedPreferences sp = context.getSharedPreferences("ror_theme", Context.MODE_PRIVATE);
        animationStyle = sp.getInt("banner_animation_style", STYLE_CYBER_SYMPHONY);
        animationIntensity = sp.getFloat("banner_animation_intensity", 1.0f);
        frameAnimationEnabled = sp.getBoolean("animate_home_banner", true);
        invalidate();
    }

    public void setAnimationStyle(int style) {
        if (style < 0 || style >= STYLE_NAMES.length) style = STYLE_CYBER_SYMPHONY;
        this.animationStyle = style;
        invalidate();
    }

    public int getAnimationStyle() {
        return animationStyle;
    }

    public void setAnimationIntensity(float intensity) {
        this.animationIntensity = Math.max(0.4f, Math.min(2.5f, intensity));
        invalidate();
    }

    public float getAnimationIntensity() {
        return animationIntensity;
    }

    public void setFrameAnimationEnabled(boolean enabled) {
        if (frameAnimationEnabled == enabled) return;
        frameAnimationEnabled = enabled;
        animationStartMs = SystemClock.uptimeMillis();
        invalidate();
        if (enabled && isShown()) postInvalidateOnAnimation();
    }

    public boolean isFrameAnimationEnabled() {
        return frameAnimationEnabled;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!frameAnimationEnabled || getDrawable() == null) return;

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        float time = ((SystemClock.uptimeMillis() - animationStartMs) / 1000f) * animationIntensity;

        switch (animationStyle) {
            case STYLE_CYBER_GLITCH:
                drawGlitchSlices(canvas, width, height, time);
                drawMatrixRain(canvas, width, height, time);
                drawScanLines(canvas, width, height, time);
                drawHudCornerBrackets(canvas, width, height, 0x7700E5FF);
                break;

            case STYLE_COSMIC_AURORA:
                drawAuroraBorealis(canvas, width, height, time);
                drawCosmicStars(canvas, width, height, time);
                drawLightSweep(canvas, width, height, time * 0.7f);
                break;

            case STYLE_SOLAR_MAGMA:
                drawMagmaEmbers(canvas, width, height, time);
                drawSolarFlare(canvas, width, height, time);
                drawHeatShimmer(canvas, width, height, time);
                break;

            case STYLE_SYNTHWAVE_HORIZON:
                drawSynthwaveGrid(canvas, width, height, time);
                drawLaserPulse(canvas, width, height, time);
                drawAudioEqualizerBars(canvas, width, height, time);
                break;

            case STYLE_SAKURA_DRIFT:
                drawSakuraPetals(canvas, width, height, time);
                drawSpiritFireflies(canvas, width, height, time);
                break;

            case STYLE_LIGHTNING_STORM:
                drawLightningStorm(canvas, width, height, time);
                drawScanLines(canvas, width, height, time * 0.8f);
                break;

            case STYLE_WARP_SPEED:
                drawWarpStarfield(canvas, width, height, time);
                drawSpeedTrailFlares(canvas, width, height, time);
                drawTacticalHudReticle(canvas, width, height, time);
                break;

            case STYLE_QUANTUM_HEX:
                drawQuantumHexGrid(canvas, width, height, time);
                drawTacticalRadar(canvas, width, height, time);
                drawHudCornerBrackets(canvas, width, height, 0x9900FFA2);
                break;

            case STYLE_BOKEH_DREAM:
                drawBokehOrbs(canvas, width, height, time);
                drawPrismaticLightLeak(canvas, width, height, time);
                break;

            case STYLE_CYBER_SYMPHONY:
            default:
                drawAuroraBorealis(canvas, width, height, time * 0.6f);
                drawMagmaEmbers(canvas, width, height, time * 0.85f);
                drawCosmicStars(canvas, width, height, time * 0.85f);
                drawLightSweep(canvas, width, height, time);
                drawHudCornerBrackets(canvas, width, height, 0x88FF1744);
                break;
        }

        if (isShown() && getWindowVisibility() == View.VISIBLE) {
            postInvalidateOnAnimation();
        }
    }

    // =========================================================================
    // STYLE 0: CYBER GLITCH & MATRIX RAIN
    // =========================================================================

    @SuppressLint("WrongCall")
    private void drawGlitchSlices(Canvas canvas, int width, int height, float time) {
        float burst = time % 3.6f;
        if (burst > 0.45f) return;

        float strength = (float) Math.sin((burst / 0.45f) * Math.PI);
        for (int i = 0; i < 5; i++) {
            float top = height * (0.10f + ((i * 0.18f + burst * 0.85f) % 0.76f));
            float bandHeight = dp(2.5f + (i % 3) * 2f);
            float shift = (float) Math.sin(time * 38f + i * 2.2f) * dp(10f) * strength;

            int save = canvas.save();
            canvas.clipRect(0, top, width, top + bandHeight);
            canvas.translate(shift, 0);
            super.onDraw(canvas);
            canvas.restoreToCount(save);

            effectPaint.setColor(i % 2 == 0 ? 0x5500E5FF : 0x55FF1744);
            canvas.drawRect(0, top, width, top + dp(1f), effectPaint);
        }
    }

    private void drawMatrixRain(Canvas canvas, int width, int height, float time) {
        float colWidth = width / (float) MATRIX_COLUMNS;
        linePaint.setStrokeWidth(dp(1.2f));
        for (int i = 0; i < MATRIX_COLUMNS; i++) {
            float yHead = ((matrixY[i] + time * matrixSpeed[i] * 0.35f) % 1.25f) * height;
            float x = i * colWidth + colWidth * 0.5f;

            for (int k = 0; k < matrixLen[i]; k++) {
                float y = yHead - k * dp(7.5f);
                if (y < 0 || y > height) continue;

                int alpha = (int) (220 * (1.0f - (float) k / matrixLen[i]));
                if (k == 0) {
                    effectPaint.setColor((alpha << 24) | 0xFFFFFF); // White leading head
                    canvas.drawCircle(x, y, dp(1.3f), effectPaint);
                } else {
                    int rgb = (i % 3 == 0) ? 0x00E5FF : 0x00FF66;
                    effectPaint.setColor((alpha << 24) | rgb);
                    canvas.drawRect(x - dp(0.8f), y - dp(2f), x + dp(0.8f), y + dp(2f), effectPaint);
                }
            }
        }
    }

    private void drawScanLines(Canvas canvas, int width, int height, float time) {
        float spacing = dp(6.5f);
        float offset = (time * dp(22f)) % spacing;
        effectPaint.setColor(0x1400E5FF);
        for (float y = -spacing + offset; y < height; y += spacing) {
            canvas.drawRect(0, y, width, y + Math.max(1f, dp(0.5f)), effectPaint);
        }

        float scannerY = (time % 2.8f) / 2.8f * height;
        effectPaint.setColor(0x4400FFB2);
        canvas.drawRect(0, scannerY, width, scannerY + dp(1.3f), effectPaint);
    }

    // =========================================================================
    // STYLE 1: COSMIC AURORA & STARS
    // =========================================================================

    private void drawAuroraBorealis(Canvas canvas, int width, int height, float time) {
        int[] auroraColors = {0x00A020F0, 0x4800E5FF, 0x36FF007F, 0x00000000};
        float[] positions = {0f, 0.45f, 0.8f, 1.0f};

        for (int layer = 0; layer < 2; layer++) {
            effectPath.reset();
            float baseY = height * (0.28f + layer * 0.32f + 0.06f * (float) Math.sin(time * 0.7f + layer * 2f));
            effectPath.moveTo(0, baseY);

            for (int x = 0; x <= width; x += Math.max(8, width / 45)) {
                float wave = (float) Math.sin(x * 0.008f + time * 1.4f + layer) * dp(12f)
                        + (float) Math.sin(x * 0.022f - time * 0.9f) * dp(6f);
                effectPath.lineTo(x, baseY + wave);
            }
            effectPath.lineTo(width, height);
            effectPath.lineTo(0, height);
            effectPath.close();

            LinearGradient grad = new LinearGradient(0, baseY - dp(15f), 0, height,
                    auroraColors, positions, Shader.TileMode.CLAMP);
            effectPaint.setShader(grad);
            canvas.drawPath(effectPath, effectPaint);
            effectPaint.setShader(null);
        }
    }

    private void drawCosmicStars(Canvas canvas, int width, int height, float time) {
        linePaint.setStrokeWidth(dp(1.1f));
        for (int i = 0; i < 28; i++) {
            float x = ((pX[i] + (float) Math.sin(time * 0.08f + pPhase[i]) * 0.02f) % 1.0f + 1.0f) % 1.0f * width;
            float y = ((pY[i] + (float) Math.cos(time * 0.06f + pPhase[i]) * 0.02f) % 1.0f + 1.0f) % 1.0f * height;

            float twinkle = 0.3f + 0.7f * (float) Math.abs(Math.sin(time * (1.8f + pSpeed[i]) + pPhase[i]));
            int alpha = (int) (twinkle * 220);
            int rgb = (i % 3 == 0) ? 0x00E5FF : ((i % 2 == 0) ? 0xFFE082 : 0xFFFFFF);

            // Diamond cross twinkle for prominent stars
            float arm = dp(2f + pSize[i] * 2.2f) * twinkle;
            linePaint.setColor(((alpha / 2) << 24) | rgb);
            canvas.drawLine(x - arm, y, x + arm, y, linePaint);
            canvas.drawLine(x, y - arm, x, y + arm, linePaint);

            // Center core
            effectPaint.setColor((alpha << 24) | 0xFFFFFF);
            canvas.drawCircle(x, y, dp(0.9f + pSize[i] * 0.5f) * twinkle, effectPaint);
        }
    }

    // =========================================================================
    // STYLE 2: SOLAR MAGMA & EMBERS
    // =========================================================================

    private void drawMagmaEmbers(Canvas canvas, int width, int height, float time) {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float rawY = pY[i] - time * (0.08f + pSpeed[i] * 0.12f);
            float y = ((rawY % 1.0f) + 1.0f) % 1.0f * height;
            float rawX = pX[i] + (float) Math.sin(time * (1.2f + pSpeed[i]) + pPhase[i]) * 0.045f;
            float x = ((rawX % 1.0f) + 1.0f) % 1.0f * width;

            float pulse = 0.4f + 0.6f * (float) Math.abs(Math.sin(time * 2.5f + pPhase[i]));
            int alpha = (int) (pulse * 215);

            int rgb = (i % 4 == 0) ? 0xFFFFD54F : ((i % 2 == 0) ? 0xFFFF5722 : 0xFFFF1744);
            effectPaint.setColor((alpha << 24) | rgb);
            float radius = dp(1.2f + pSize[i] * 1.5f) * pulse;
            canvas.drawCircle(x, y, radius, effectPaint);

            // Ascending heat tail
            linePaint.setColor(((alpha / 3) << 24) | rgb);
            linePaint.setStrokeWidth(Math.max(1f, radius * 0.6f));
            canvas.drawLine(x, y, x, Math.min(height, y + dp(7f)), linePaint);
        }
    }

    private void drawSolarFlare(Canvas canvas, int width, int height, float time) {
        float cycle = (time % 4.2f) / 4.2f;
        float centerX = -width * 0.35f + cycle * width * 1.7f;
        float halfWidth = width * 0.085f;

        effectPath.reset();
        effectPath.moveTo(centerX - halfWidth, 0);
        effectPath.lineTo(centerX + halfWidth, 0);
        effectPath.lineTo(centerX + halfWidth * 1.8f + height * 0.35f, height);
        effectPath.lineTo(centerX - halfWidth * 1.8f + height * 0.35f, height);
        effectPath.close();

        int[] flareColors = {0x00FF5722, 0x38FFA000, 0x48FFE082, 0x38FFA000, 0x00FF1744};
        LinearGradient flareGrad = new LinearGradient(centerX - halfWidth, 0,
                centerX + halfWidth, 0, flareColors, null, Shader.TileMode.CLAMP);
        effectPaint.setShader(flareGrad);
        canvas.drawPath(effectPath, effectPaint);
        effectPaint.setShader(null);
    }

    private void drawHeatShimmer(Canvas canvas, int width, int height, float time) {
        linePaint.setStrokeWidth(dp(1.2f));
        effectPath.reset();
        float baseY = height * 0.88f;
        for (int x = 0; x <= width; x += Math.max(6, width / 60)) {
            float y = baseY + (float) Math.sin(x * 0.04f + time * 4f) * dp(2.8f)
                    + (float) Math.sin(x * 0.015f - time * 2.5f) * dp(1.8f);
            if (x == 0) effectPath.moveTo(x, y);
            else effectPath.lineTo(x, y);
        }
        linePaint.setColor(0x55FF5722);
        canvas.drawPath(effectPath, linePaint);
    }

    // =========================================================================
    // STYLE 3: SYNTHWAVE 80S NEON GRID & EQUALIZER
    // =========================================================================

    private void drawSynthwaveGrid(Canvas canvas, int width, int height, float time) {
        float horizonY = height * 0.46f;
        float vanishX = width * 0.5f;

        linePaint.setStrokeWidth(dp(1.0f));
        linePaint.setColor(0x3500E5FF);

        // Radial perspective lines converging to horizon center
        int perspectiveCount = 18;
        for (int i = 0; i <= perspectiveCount; i++) {
            float bottomX = width * (i / (float) perspectiveCount);
            canvas.drawLine(vanishX, horizonY, bottomX, height, linePaint);
        }

        // Horizontal grid rungs moving toward viewer
        linePaint.setColor(0x50FF007F);
        float gridCycle = (time * 0.45f) % 1.0f;
        for (int rung = 0; rung < 7; rung++) {
            float t = ((rung / 7.0f) + gridCycle * (1.0f / 7.0f)) % 1.0f;
            float y = horizonY + (t * t) * (height - horizonY);
            canvas.drawLine(0, y, width, y, linePaint);
        }
    }

    private void drawLaserPulse(Canvas canvas, int width, int height, float time) {
        float laserProgress = (time * 1.1f) % 2.5f;
        if (laserProgress < 1.0f) {
            float x = laserProgress * width;
            float y = height * 0.46f;
            linePaint.setStrokeWidth(dp(1.8f));
            linePaint.setColor(0xAAFF007F);
            canvas.drawLine(Math.max(0, x - dp(70f)), y, Math.min(width, x + dp(20f)), y, linePaint);

            effectPaint.setColor(0xFFFFFFFF);
            canvas.drawCircle(x, y, dp(2.2f), effectPaint);
        }
    }

    private void drawAudioEqualizerBars(Canvas canvas, int width, int height, float time) {
        int barCount = 28;
        float barWidth = width / (float) barCount;
        for (int b = 0; b < barCount; b++) {
            float phase = b * 0.4f + time * 3.8f;
            float barHeight = height * 0.22f * (0.25f + 0.75f * (float) Math.abs(Math.sin(phase) * Math.cos(phase * 0.6f)));
            float x = b * barWidth + dp(1.5f);
            float y = height - barHeight;

            int color = (b % 2 == 0) ? 0x66FF007F : 0x6600E5FF;
            effectPaint.setColor(color);
            canvas.drawRect(x, y, x + barWidth - dp(2.5f), height, effectPaint);

            // Glowing top cap
            effectPaint.setColor(0xCCFFFFFF);
            canvas.drawRect(x, y, x + barWidth - dp(2.5f), y + dp(1.2f), effectPaint);
        }
    }

    // =========================================================================
    // STYLE 4: SAKURA BLOSSOM PETALS & SPIRIT FIREFLIES
    // =========================================================================

    private void drawSakuraPetals(Canvas canvas, int width, int height, float time) {
        for (int i = 0; i < SAKURA_COUNT; i++) {
            float rawY = sakuraY[i] + time * (0.045f + (i % 5) * 0.015f);
            float y = ((rawY % 1.0f) + 1.0f) % 1.0f * height;

            float rawX = sakuraX[i] + (float) Math.sin(time * 0.9f + sakuraPhase[i]) * 0.06f
                    + time * 0.02f; // Wind blowing slightly to the right
            float x = ((rawX % 1.0f) + 1.0f) % 1.0f * width;

            float rotation = sakuraRot[i] + time * sakuraRotSpeed[i];
            float scale = (dp(sakuraSize[i]) / 10f) * (0.8f + 0.2f * (float) Math.sin(time * 2f + sakuraPhase[i]));

            int save = canvas.save();
            canvas.translate(x, y);
            canvas.rotate(rotation);
            canvas.scale(scale, scale);

            // Shaded Sakura petal body
            effectPaint.setColor(i % 3 == 0 ? 0xD0FFB7C5 : 0xD5FFC0CB);
            canvas.drawPath(petalPath, effectPaint);

            // Petal interior highlight
            linePaint.setColor(0x80FFFFFF);
            linePaint.setStrokeWidth(1.2f);
            canvas.drawLine(0, -6f, 0, 5f, linePaint);

            canvas.restoreToCount(save);
        }
    }

    private void drawSpiritFireflies(Canvas canvas, int width, int height, float time) {
        for (int i = 0; i < 14; i++) {
            float x = ((pX[i] + (float) Math.sin(time * 0.7f + pPhase[i]) * 0.05f) % 1.0f + 1.0f) % 1.0f * width;
            float y = ((pY[i] + (float) Math.cos(time * 0.5f + pPhase[i]) * 0.05f) % 1.0f + 1.0f) % 1.0f * height;

            float breath = 0.35f + 0.65f * (float) Math.abs(Math.sin(time * 2.2f + pPhase[i]));
            int alpha = (int) (breath * 190);

            // Soft glowing aura
            effectPaint.setColor(((alpha / 3) << 24) | 0xFFE082);
            canvas.drawCircle(x, y, dp(6f) * breath, effectPaint);

            // Intense inner firefly spark
            effectPaint.setColor((alpha << 24) | 0xFFFFF9);
            canvas.drawCircle(x, y, dp(1.8f) * breath, effectPaint);
        }
    }

    // =========================================================================
    // STYLE 5: ELECTRIC LIGHTNING STORM
    // =========================================================================

    private void drawLightningStorm(Canvas canvas, int width, int height, float time) {
        float strikeCycle = time % 1.8f;
        if (strikeCycle < 0.28f) {
            float strikeSeed = (float) Math.floor(time / 1.8f);
            Random lRng = new Random((long) (strikeSeed * 100000L));

            float startX = width * (0.2f + lRng.nextFloat() * 0.6f);
            float startY = 0;
            float endX = startX + (lRng.nextFloat() - 0.5f) * width * 0.45f;
            float endY = height;

            effectPath.reset();
            secondaryPath.reset();
            effectPath.moveTo(startX, startY);

            int segments = 12;
            float curX = startX;
            float curY = startY;

            for (int s = 1; s <= segments; s++) {
                float targetY = (s / (float) segments) * endY;
                float targetX = startX + (s / (float) segments) * (endX - startX)
                        + (lRng.nextFloat() - 0.5f) * dp(26f);

                effectPath.lineTo(targetX, targetY);

                // Occasional branch fork
                if (s == 5 || s == 8) {
                    secondaryPath.moveTo(targetX, targetY);
                    float branchX = targetX + (lRng.nextFloat() - 0.5f) * dp(45f);
                    float branchY = targetY + dp(20f + lRng.nextFloat() * 25f);
                    secondaryPath.lineTo(branchX, branchY);
                }

                curX = targetX;
                curY = targetY;
            }

            float flashAlpha = (float) Math.sin((strikeCycle / 0.28f) * Math.PI);
            int alphaInt = (int) (flashAlpha * 240);

            // Outer plasma glow
            glowPaint.setStrokeWidth(dp(5.5f));
            glowPaint.setColor(((alphaInt / 3) << 24) | 0x9D00FF);
            canvas.drawPath(effectPath, glowPaint);
            canvas.drawPath(secondaryPath, glowPaint);

            // Cyan electric body
            linePaint.setStrokeWidth(dp(2.4f));
            linePaint.setColor((alphaInt << 24) | 0x00E5FF);
            canvas.drawPath(effectPath, linePaint);
            canvas.drawPath(secondaryPath, linePaint);

            // Blinding white core
            linePaint.setStrokeWidth(dp(1.0f));
            linePaint.setColor((alphaInt << 24) | 0xFFFFFF);
            canvas.drawPath(effectPath, linePaint);

            // Strike impact sparks
            for (int sp = 0; sp < 8; sp++) {
                float angle = lRng.nextFloat() * 6.28f;
                float dist = dp(8f + lRng.nextFloat() * 24f) * flashAlpha;
                float sx = curX + (float) Math.cos(angle) * dist;
                float sy = curY + (float) Math.sin(angle) * dist;
                effectPaint.setColor((alphaInt << 24) | 0x00FFE5);
                canvas.drawCircle(sx, sy, dp(1.2f), effectPaint);
            }
        }
    }

    // =========================================================================
    // STYLE 6: HYPERDRIVE WARP SPEED & HUD
    // =========================================================================

    private void drawWarpStarfield(Canvas canvas, int width, int height, float time) {
        float centerX = width * 0.5f;
        float centerY = height * 0.5f;
        linePaint.setStrokeWidth(dp(1.4f));

        for (int i = 0; i < WARP_STAR_COUNT; i++) {
            float z = starZ[i] - (time * starSpeed[i] * 0.45f);
            z = ((z % 1.0f) + 1.0f) % 1.0f;
            if (z < 0.02f) z = 0.98f;

            float prevZ = Math.min(1.0f, z + 0.045f);

            float x1 = centerX + (starX[i] / prevZ) * (width * 0.42f);
            float y1 = centerY + (starY[i] / prevZ) * (height * 0.42f);

            float x2 = centerX + (starX[i] / z) * (width * 0.42f);
            float y2 = centerY + (starY[i] / z) * (height * 0.42f);

            if (x2 < 0 || x2 > width || y2 < 0 || y2 > height) continue;

            float intensity = (1.0f - z);
            int alpha = (int) (intensity * 230);
            int rgb = (i % 3 == 0) ? 0x00E5FF : 0xFFFFFF;

            linePaint.setColor((alpha << 24) | rgb);
            canvas.drawLine(x1, y1, x2, y2, linePaint);

            effectPaint.setColor((alpha << 24) | 0xFFFFFF);
            canvas.drawCircle(x2, y2, dp(1.2f * intensity), effectPaint);
        }
    }

    private void drawSpeedTrailFlares(Canvas canvas, int width, int height, float time) {
        float speedProgress = (time * 1.4f) % 1.0f;
        float topX = speedProgress * width;
        linePaint.setStrokeWidth(dp(1.8f));
        linePaint.setColor(0x8800E5FF);
        canvas.drawLine(Math.max(0, topX - dp(90f)), dp(2f), topX, dp(2f), linePaint);

        float bottomX = width - (speedProgress * width);
        linePaint.setColor(0x88FF1744);
        canvas.drawLine(bottomX, height - dp(2f), Math.min(width, bottomX + dp(90f)), height - dp(2f), linePaint);
    }

    private void drawTacticalHudReticle(Canvas canvas, int width, int height, float time) {
        float cx = width * 0.5f;
        float cy = height * 0.5f;
        float r = dp(24f);

        linePaint.setStrokeWidth(dp(1f));
        linePaint.setColor(0x4000E5FF);
        canvas.drawCircle(cx, cy, r, linePaint);

        int save = canvas.save();
        canvas.rotate(time * 35f, cx, cy);
        for (int a = 0; a < 360; a += 90) {
            float rad = (float) Math.toRadians(a);
            canvas.drawLine(cx + (float) Math.cos(rad) * (r - dp(4f)),
                    cy + (float) Math.sin(rad) * (r - dp(4f)),
                    cx + (float) Math.cos(rad) * (r + dp(4f)),
                    cy + (float) Math.sin(rad) * (r + dp(4f)), linePaint);
        }
        canvas.restoreToCount(save);
    }

    // =========================================================================
    // STYLE 7: QUANTUM HEX SHIELD & RADAR
    // =========================================================================

    private void drawQuantumHexGrid(Canvas canvas, int width, int height, float time) {
        float radius = dp(16f);
        float hexWidth = radius * 1.732f;
        float hexHeight = radius * 2f;

        linePaint.setStrokeWidth(dp(0.9f));

        int cols = (int) (width / hexWidth) + 2;
        int rows = (int) (height / (hexHeight * 0.75f)) + 2;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float cx = c * hexWidth + ((r % 2 == 1) ? hexWidth * 0.5f : 0);
                float cy = r * (hexHeight * 0.75f);

                // Wave ripple pulse
                float distFromCenter = (float) Math.hypot(cx - width * 0.5f, cy - height * 0.5f);
                float wave = (float) Math.sin(distFromCenter * 0.02f - time * 2.5f);
                if (wave > 0.4f) {
                    int alpha = (int) ((wave - 0.4f) / 0.6f * 95);
                    linePaint.setColor((alpha << 24) | 0x00FFA2);
                    drawHexagon(canvas, cx, cy, radius * 0.85f, linePaint);
                }
            }
        }
    }

    private void drawHexagon(Canvas canvas, float cx, float cy, float r, Paint paint) {
        effectPath.reset();
        for (int i = 0; i < 6; i++) {
            float angle = (float) (Math.PI / 3.0 * i + Math.PI / 6.0);
            float x = cx + (float) Math.cos(angle) * r;
            float y = cy + (float) Math.sin(angle) * r;
            if (i == 0) effectPath.moveTo(x, y);
            else effectPath.lineTo(x, y);
        }
        effectPath.close();
        canvas.drawPath(effectPath, paint);
    }

    private void drawTacticalRadar(Canvas canvas, int width, int height, float time) {
        float cx = width * 0.84f;
        float cy = height * 0.50f;
        float r = dp(28f);

        linePaint.setStrokeWidth(dp(1.0f));
        linePaint.setColor(0x3500FFA2);
        canvas.drawCircle(cx, cy, r, linePaint);
        canvas.drawCircle(cx, cy, r * 0.5f, linePaint);

        // Radar rotating line
        float angle = (time * 120f) % 360f;
        float rad = (float) Math.toRadians(angle);
        linePaint.setStrokeWidth(dp(1.5f));
        linePaint.setColor(0x9900FFA2);
        canvas.drawLine(cx, cy, cx + (float) Math.cos(rad) * r, cy + (float) Math.sin(rad) * r, linePaint);
    }

    // =========================================================================
    // STYLE 8: NEON BOKEH PRISMATIC DREAM
    // =========================================================================

    private void drawBokehOrbs(Canvas canvas, int width, int height, float time) {
        for (int i = 0; i < BOKEH_COUNT; i++) {
            float rawY = bokehY[i] - time * bokehSpeed[i] * 0.08f;
            float y = ((rawY % 1.0f) + 1.0f) % 1.0f * height;
            float rawX = bokehX[i] + (float) Math.sin(time * 0.4f + i) * 0.035f;
            float x = ((rawX % 1.0f) + 1.0f) % 1.0f * width;

            float pulse = 0.8f + 0.2f * (float) Math.sin(time * 1.5f + i);
            float r = dp(bokehRadius[i]) * pulse;

            // Translucent bokeh fill
            effectPaint.setColor(bokehColor[i]);
            canvas.drawCircle(x, y, r, effectPaint);

            // Thin crisp rim
            linePaint.setColor((0x55 << 24) | (bokehColor[i] & 0x00FFFFFF));
            linePaint.setStrokeWidth(dp(1f));
            canvas.drawCircle(x, y, r, linePaint);
        }
    }

    private void drawPrismaticLightLeak(Canvas canvas, int width, int height, float time) {
        float sweep = (float) Math.sin(time * 0.6f);
        int[] leakColors = {0x45FF007F, 0x307F00FF, 0x3500E5FF, 0x00000000};
        RadialGradient grad = new RadialGradient(width * (0.85f + sweep * 0.15f), 0,
                width * 0.65f, leakColors, null, Shader.TileMode.CLAMP);
        effectPaint.setShader(grad);
        canvas.drawRect(0, 0, width, height, effectPaint);
        effectPaint.setShader(null);
    }

    // =========================================================================
    // SHARED UTILITIES & HUDS
    // =========================================================================

    private void drawLightSweep(Canvas canvas, int width, int height, float time) {
        float cycle = (time % 3.6f) / 3.6f;
        float centerX = -width * 0.30f + cycle * width * 1.6f;
        float halfWidth = width * 0.065f;

        effectPath.reset();
        effectPath.moveTo(centerX - halfWidth, 0);
        effectPath.lineTo(centerX + halfWidth, 0);
        effectPath.lineTo(centerX - halfWidth + height * 0.42f, height);
        effectPath.lineTo(centerX - halfWidth * 3f + height * 0.42f, height);
        effectPath.close();
        effectPaint.setColor(0x22FFFFFF);
        canvas.drawPath(effectPath, effectPaint);

        linePaint.setColor(0x6000E5FF);
        linePaint.setStrokeWidth(dp(1.1f));
        canvas.drawLine(centerX + halfWidth, 0,
                centerX - halfWidth + height * 0.42f, height, linePaint);
    }

    private void drawHudCornerBrackets(Canvas canvas, int width, int height, int color) {
        float len = dp(14f);
        float pad = dp(8f);
        linePaint.setColor(color);
        linePaint.setStrokeWidth(dp(1.4f));

        // Top-left
        canvas.drawLine(pad, pad, pad + len, pad, linePaint);
        canvas.drawLine(pad, pad, pad, pad + len, linePaint);

        // Top-right
        canvas.drawLine(width - pad, pad, width - pad - len, pad, linePaint);
        canvas.drawLine(width - pad, pad, width - pad, pad + len, linePaint);

        // Bottom-left
        canvas.drawLine(pad, height - pad, pad + len, height - pad, linePaint);
        canvas.drawLine(pad, height - pad, pad, height - pad - len, linePaint);

        // Bottom-right
        canvas.drawLine(width - pad, height - pad, width - pad - len, height - pad, linePaint);
        canvas.drawLine(width - pad, height - pad, width - pad, height - pad - len, linePaint);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == View.VISIBLE && frameAnimationEnabled) {
            animationStartMs = SystemClock.uptimeMillis();
            postInvalidateOnAnimation();
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}

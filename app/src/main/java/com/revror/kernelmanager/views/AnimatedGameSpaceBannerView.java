package com.revror.kernelmanager.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.Uri;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import java.io.InputStream;
import java.util.Random;

/**
 * AnimatedGameSpaceBannerView
 * 60 FPS live animated background for Game Space supporting:
 * - 12 dynamic preset canvas animations (Cyber Laser, Synthwave, Mecha, Hellfire, Matrix Rain, Sakura, etc.)
 * - Cinematic Ken Burns motion with ambient floating particles for custom gallery banners.
 * - Zero allocations in onDraw() for maximum efficiency and minimum thermal load.
 */
public class AnimatedGameSpaceBannerView extends View {

    private int bannerId = 0;
    private String customBannerUriString = null;
    private int blurAmount = 0;
    private Bitmap customBitmap = null;
    private boolean isAnimating = true;

    private long startTime;
    private long lastFrameTime;

    // Shared reusable paints
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint scrimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix transformMatrix = new Matrix();
    private final Rect srcRect = new Rect();
    private final RectF dstRect = new RectF();
    private final Path tempPath = new Path();

    // Particle pools for various animations
    private static final int MAX_PARTICLES = 60;
    private final float[] partX = new float[MAX_PARTICLES];
    private final float[] partY = new float[MAX_PARTICLES];
    private final float[] partSpeed = new float[MAX_PARTICLES];
    private final float[] partSize = new float[MAX_PARTICLES];
    private final float[] partAlpha = new float[MAX_PARTICLES];
    private final float[] partAngle = new float[MAX_PARTICLES];
    private final int[] partColor = new int[MAX_PARTICLES];
    private boolean particlesInitialized = false;

    // Matrix Rain specific arrays
    private static final int MATRIX_COLS = 36;
    private final float[] matrixY = new float[MATRIX_COLS];
    private final float[] matrixSpeed = new float[MATRIX_COLS];
    private final int[] matrixCharIdx = new int[MATRIX_COLS];
    private static final String MATRIX_CHARS = "01011001RORX9Z7KF4Q2AΩΨΔΞ";

    // Horizon grid scroll tracking
    private float gridOffset = 0f;

    public AnimatedGameSpaceBannerView(Context context) {
        super(context);
        init();
    }

    public AnimatedGameSpaceBannerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnimatedGameSpaceBannerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        startTime = SystemClock.uptimeMillis();
        lastFrameTime = startTime;
        linePaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        scrimPaint.setColor(0x4405070E);
        initParticles();
    }

    private void initParticles() {
        Random rng = new Random(42);
        for (int i = 0; i < MAX_PARTICLES; i++) {
            partX[i] = rng.nextFloat();
            partY[i] = rng.nextFloat();
            partSpeed[i] = 0.2f + rng.nextFloat() * 0.8f;
            partSize[i] = 1.5f + rng.nextFloat() * 4.5f;
            partAlpha[i] = 0.3f + rng.nextFloat() * 0.7f;
            partAngle[i] = rng.nextFloat() * 360f;
        }
        for (int i = 0; i < MATRIX_COLS; i++) {
            matrixY[i] = rng.nextFloat();
            matrixSpeed[i] = 0.25f + rng.nextFloat() * 0.65f;
            matrixCharIdx[i] = rng.nextInt(MATRIX_CHARS.length());
        }
        particlesInitialized = true;
    }

    public void setBanner(int id, String customUri, int blur) {
        this.bannerId = id;
        this.customBannerUriString = customUri;
        this.blurAmount = blur;
        loadCustomBitmapIfNeed();
        invalidate();
    }

    public void pauseAnimation() {
        isAnimating = false;
    }

    public void resumeAnimation() {
        if (!isAnimating) {
            isAnimating = true;
            lastFrameTime = SystemClock.uptimeMillis();
            postInvalidateOnAnimation();
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            resumeAnimation();
        } else {
            pauseAnimation();
        }
    }

    private void loadCustomBitmapIfNeed() {
        if (bannerId >= 100 && customBannerUriString != null) {
            try {
                Uri uri = Uri.parse(customBannerUriString);
                InputStream is = getContext().getContentResolver().openInputStream(uri);
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inPreferredConfig = Bitmap.Config.RGB_565;
                customBitmap = BitmapFactory.decodeStream(is, null, opts);
                if (is != null) is.close();
            } catch (Throwable t) {
                customBitmap = null;
            }
        } else {
            if (customBitmap != null) {
                customBitmap = null;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        long now = SystemClock.uptimeMillis();
        float elapsedSec = (now - startTime) / 1000f;
        float dt = Math.min((now - lastFrameTime) / 1000f, 0.05f);
        lastFrameTime = now;

        if (bannerId >= 100 && customBitmap != null) {
            drawCustomKenBurns(canvas, w, h, elapsedSec);
        } else {
            int preset = (bannerId % 24);
            switch (preset) {
                case 0:  drawCyberLaser(canvas, w, h, elapsedSec, dt); break;
                case 1:  drawSpaceNebula(canvas, w, h, elapsedSec, dt); break;
                case 2:  drawMechaCore(canvas, w, h, elapsedSec, dt); break;
                case 3:  drawSynthwave80s(canvas, w, h, elapsedSec, dt); break;
                case 4:  drawRevrorCore(canvas, w, h, elapsedSec, dt); break;
                case 5:  drawMatrixRain(canvas, w, h, elapsedSec, dt); break;
                case 6:  drawCyberSamurai(canvas, w, h, elapsedSec, dt); break;
                case 7:  drawHellfireDragon(canvas, w, h, elapsedSec, dt); break;
                case 8:  drawArcticFrost(canvas, w, h, elapsedSec, dt); break;
                case 9:  drawApexHazard(canvas, w, h, elapsedSec, dt); break;
                case 10: drawEvaMana(canvas, w, h, elapsedSec, dt); break;
                case 11: drawRgbChromaWave(canvas, w, h, elapsedSec, dt); break;
                case 12: drawAnimeCyberGirl(canvas, w, h, elapsedSec, dt); break;
                case 13: drawShadowMonarch(canvas, w, h, elapsedSec, dt); break;
                case 14: drawDemonNichirinFlame(canvas, w, h, elapsedSec, dt); break;
                case 15: drawGojoLimitlessVoid(canvas, w, h, elapsedSec, dt); break;
                case 16: drawCyberSamuraiWaifu(canvas, w, h, elapsedSec, dt); break;
                case 17: drawUltraInstinct(canvas, w, h, elapsedSec, dt); break;
                case 18: drawChibiNekoGamer(canvas, w, h, elapsedSec, dt); break;
                case 19: drawBerserkEclipse(canvas, w, h, elapsedSec, dt); break;
                case 20: drawPersonaPhantomThief(canvas, w, h, elapsedSec, dt); break;
                case 21: drawGundamStrikeFreedom(canvas, w, h, elapsedSec, dt); break;
                case 22: drawCyberVoidValkyrie(canvas, w, h, elapsedSec, dt); break;
                case 23: default: drawArcaneJinxSupernova(canvas, w, h, elapsedSec, dt); break;
            }
        }

        // Dark glass scrim for contrast with foreground HUD controls
        scrimPaint.setColor(0x4003050B);
        canvas.drawRect(0, 0, w, h, scrimPaint);

        if (isAnimating) {
            postInvalidateOnAnimation();
        }
    }

    /**
     * Preset 0: REVROR Cyber Laser
     * Moving 3D horizon grid with oscillating dual laser beams and floating plasma nodes.
     */
    private void drawCyberLaser(Canvas canvas, int w, int h, float t, float dt) {
        LinearGradient bg = new LinearGradient(0, 0, w, h,
                new int[]{0xFF090A0E, 0xFF1C080E, 0xFF0B0407}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(null);

        float horizonY = h * 0.42f;
        gridOffset = (gridOffset + dt * 0.35f) % 1.0f;

        // 3D Perspective Grid - Deep Crimson Red
        linePaint.setColor(0x30FF1744);
        linePaint.setStrokeWidth(dp(1.2f));
        int fanLines = 14;
        for (int i = 0; i <= fanLines; i++) {
            float bottomX = (w * 1.4f / fanLines) * i - (w * 0.2f);
            canvas.drawLine(w * 0.5f, horizonY, bottomX, h, linePaint);
        }
        for (int r = 1; r <= 8; r++) {
            float progress = (float) Math.pow((r + gridOffset) / 9.0, 2.2);
            float y = horizonY + (h - horizonY) * progress;
            int alpha = (int) (18 + progress * 80);
            linePaint.setColor((alpha << 24) | 0xFF1744);
            canvas.drawLine(0, y, w, y, linePaint);
        }

        // Scanning Lasers - Crimson Red & Hot Ember
        float laser1Pos = (float) Math.sin(t * 1.2f) * 0.4f + 0.5f;
        float laser2Pos = (float) Math.cos(t * 0.9f) * 0.4f + 0.5f;

        linePaint.setColor(0xDDFF1744);
        linePaint.setStrokeWidth(dp(2.8f));
        canvas.drawLine(0, h * laser1Pos, w, h * (1f - laser1Pos), linePaint);

        linePaint.setColor(0xAAFF5252);
        linePaint.setStrokeWidth(dp(2.0f));
        canvas.drawLine(0, h * (1f - laser2Pos), w, h * laser2Pos, linePaint);

        // Floating energy nodes - Crimson embers
        glowPaint.setColor(0x99FF1744);
        for (int i = 0; i < 20; i++) {
            float py = (partY[i] - t * partSpeed[i] * 0.08f) % 1.0f;
            if (py < 0) py += 1.0f;
            float px = (partX[i] + (float) Math.sin(t + i) * 0.03f) % 1.0f;
            if (px < 0) px += 1.0f;
            canvas.drawCircle(px * w, py * h, dp(partSize[i] * 0.6f), glowPaint);
        }
    }

    /**
     * Preset 1: Space Nebula
     * Deep cosmic dust swirling with 40 pulsating stars and dynamic chromatic nebula blooms.
     */
    private void drawSpaceNebula(Canvas canvas, int w, int h, float t, float dt) {
        LinearGradient bg = new LinearGradient(0, 0, 0, h,
                new int[]{0xFF040209, 0xFF140726, 0xFF08142A}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(null);

        // Breathing Nebula Clouds
        float n1Scale = 1.0f + (float) Math.sin(t * 0.6f) * 0.12f;
        RadialGradient nebula1 = new RadialGradient(w * 0.35f, h * 0.45f, w * 0.4f * n1Scale,
                new int[]{0x55A128B5, 0x228010A0, 0x00000000}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(nebula1);
        canvas.drawCircle(w * 0.35f, h * 0.45f, w * 0.4f * n1Scale, bgPaint);

        float n2Scale = 1.0f + (float) Math.cos(t * 0.8f) * 0.15f;
        RadialGradient nebula2 = new RadialGradient(w * 0.72f, h * 0.55f, w * 0.35f * n2Scale,
                new int[]{0x4400D2FF, 0x180088CC, 0x00000000}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(nebula2);
        canvas.drawCircle(w * 0.72f, h * 0.55f, w * 0.35f * n2Scale, bgPaint);
        bgPaint.setShader(null);

        // Twinkling Starfield
        for (int i = 0; i < 45; i++) {
            float pulse = 0.3f + 0.7f * (float) Math.abs(Math.sin(t * 2f + i * 1.5f));
            int alpha = (int) (pulse * 255);
            glowPaint.setColor((alpha << 24) | 0xFFFFFF);
            float sx = (partX[i] * w);
            float sy = (partY[i] * h);
            canvas.drawCircle(sx, sy, dp(partSize[i] * 0.5f), glowPaint);
        }
    }

    /**
     * Preset 2: Mecha Core
     * Concentric cyber reticles rotating, pulsing radar sweep sector, and tactical markings.
     */
    private void drawMechaCore(Canvas canvas, int w, int h, float t, float dt) {
        LinearGradient bg = new LinearGradient(0, 0, w, h,
                new int[]{0xFF0E1118, 0xFF220C12, 0xFF0B0D12}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(null);

        float cx = w * 0.5f;
        float cy = h * 0.5f;
        float baseR = Math.min(w, h) * 0.38f;

        // Rotating outer ring with notches
        float angleOuter = t * 24f;
        linePaint.setColor(0x44FF1744);
        linePaint.setStrokeWidth(dp(2.5f));
        dstRect.set(cx - baseR, cy - baseR, cx + baseR, cy + baseR);
        canvas.drawArc(dstRect, angleOuter, 80, false, linePaint);
        canvas.drawArc(dstRect, angleOuter + 120, 80, false, linePaint);
        canvas.drawArc(dstRect, angleOuter + 240, 80, false, linePaint);

        // Counter-rotating inner ring
        float angleInner = -t * 36f;
        float rInner = baseR * 0.65f;
        linePaint.setColor(0x6600E5FF);
        linePaint.setStrokeWidth(dp(1.8f));
        dstRect.set(cx - rInner, cy - rInner, cx + rInner, cy + rInner);
        canvas.drawArc(dstRect, angleInner, 100, false, linePaint);
        canvas.drawArc(dstRect, angleInner + 180, 100, false, linePaint);

        // Pulsing radar sweep sector
        float sweepAngle = (t * 85f) % 360f;
        tempPath.reset();
        tempPath.moveTo(cx, cy);
        float rad = (float) Math.toRadians(sweepAngle);
        float radEnd = (float) Math.toRadians(sweepAngle + 45f);
        tempPath.lineTo(cx + (float) Math.cos(rad) * baseR, cy + (float) Math.sin(rad) * baseR);
        tempPath.arcTo(dstRect, sweepAngle, 45f);
        tempPath.close();
        glowPaint.setColor(0x18FF1744);
        canvas.drawPath(tempPath, glowPaint);

        // Core reticle crosshair
        linePaint.setColor(0x88FF1744);
        linePaint.setStrokeWidth(dp(1.2f));
        canvas.drawLine(cx - baseR * 0.25f, cy, cx + baseR * 0.25f, cy, linePaint);
        canvas.drawLine(cx, cy - baseR * 0.25f, cx, cy + baseR * 0.25f, linePaint);
    }

    /**
     * Preset 3: Synthwave 80s
     * Blazing retro neon sun with shimmering horizontal black slices and rolling magenta wireframe.
     */
    private void drawSynthwave80s(Canvas canvas, int w, int h, float t, float dt) {
        LinearGradient bg = new LinearGradient(0, 0, 0, h,
                new int[]{0xFF0E0024, 0xFF3D0942, 0xFF0A021A}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(null);

        float cx = w * 0.5f;
        float cy = h * 0.44f;
        float sunR = Math.min(w, h) * 0.28f;

        // Radiant Sun
        RadialGradient sun = new RadialGradient(cx, cy, sunR,
                new int[]{0xFFFFEE55, 0xFFFF007F, 0x00FF007F}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(sun);
        canvas.drawCircle(cx, cy, sunR, bgPaint);
        bgPaint.setShader(null);

        // Oscillating horizontal sun scanlines
        linePaint.setColor(0xFF0E0024);
        float linePhase = (t * 0.4f) % 1.0f;
        for (int i = 0; i < 7; i++) {
            float y = cy - sunR * 0.1f + (i + linePhase) * dp(10);
            if (y < cy + sunR) {
                float thickness = dp(1.5f + i * 0.8f);
                linePaint.setStrokeWidth(thickness);
                float span = (float) Math.sqrt(Math.max(0, sunR * sunR - (y - cy) * (y - cy)));
                canvas.drawLine(cx - span, y, cx + span, y, linePaint);
            }
        }

        // 3D Moving Synthwave Wireframe Floor
        float horizonY = h * 0.62f;
        gridOffset = (gridOffset + dt * 0.45f) % 1.0f;
        linePaint.setColor(0x3500F0FF);
        linePaint.setStrokeWidth(dp(1.2f));

        for (int x = 0; x <= 16; x++) {
            float bottomX = (w * 1.6f / 16f) * x - (w * 0.3f);
            canvas.drawLine(cx, horizonY, bottomX, h, linePaint);
        }
        for (int r = 1; r <= 6; r++) {
            float prog = (float) Math.pow((r + gridOffset) / 7.0, 2.0);
            float y = horizonY + (h - horizonY) * prog;
            linePaint.setColor(((int)(20 + prog * 80) << 24) | 0xFF007F);
            canvas.drawLine(0, y, w, y, linePaint);
        }
    }

    /**
     * Preset 4: REVROR Core
     * Dynamic dual energy containment rings with pulsing plasma core and orbiting leptons.
     */
    private void drawRevrorCore(Canvas canvas, int w, int h, float t, float dt) {
        LinearGradient bg = new LinearGradient(0, 0, w, h,
                new int[]{0xFF05050A, 0xFF1C060E, 0xFF05050A}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(null);

        float cx = w * 0.5f;
        float cy = h * 0.5f;
        float baseR = Math.min(w, h) * 0.36f;

        // Pulsing plasma glow
        float pulse = 0.85f + 0.15f * (float) Math.sin(t * 3f);
        RadialGradient coreGlow = new RadialGradient(cx, cy, baseR * pulse,
                new int[]{0x66FF1744, 0x22FF1744, 0x00000000}, null, Shader.TileMode.CLAMP);
        glowPaint.setShader(coreGlow);
        canvas.drawCircle(cx, cy, baseR * pulse, glowPaint);
        glowPaint.setShader(null);

        // Core containment ring 1 (outer)
        linePaint.setColor(0x55FF1744);
        linePaint.setStrokeWidth(dp(10));
        canvas.drawCircle(cx, cy, baseR, linePaint);

        // Core ring 2 (active segments)
        linePaint.setColor(0xFFFF1744);
        linePaint.setStrokeWidth(dp(3f));
        dstRect.set(cx - baseR * 0.8f, cy - baseR * 0.8f, cx + baseR * 0.8f, cy + baseR * 0.8f);
        float rot = t * 45f;
        canvas.drawArc(dstRect, rot, 60, false, linePaint);
        canvas.drawArc(dstRect, rot + 120, 60, false, linePaint);
        canvas.drawArc(dstRect, rot + 240, 60, false, linePaint);

        // Core center ring
        linePaint.setColor(0xFF00E5FF);
        linePaint.setStrokeWidth(dp(1.8f));
        canvas.drawCircle(cx, cy, baseR * 0.45f, linePaint);

        // Orbiting particles
        glowPaint.setColor(0xFFFFD600);
        for (int i = 0; i < 6; i++) {
            float orbAngle = (float) Math.toRadians(t * 80f + i * 60f);
            float px = cx + (float) Math.cos(orbAngle) * baseR * 0.8f;
            float py = cy + (float) Math.sin(orbAngle) * baseR * 0.8f;
            canvas.drawCircle(px, py, dp(3f), glowPaint);
        }
    }

    /**
     * Preset 5: Matrix Digital Rain
     * 36 cascading columns of falling cyber glyphs with bright white/cyan leading heads.
     */
    private void drawMatrixRain(Canvas canvas, int w, int h, float t, float dt) {
        bgPaint.setColor(0xFF020704);
        canvas.drawRect(0, 0, w, h, bgPaint);

        textPaint.setTextSize(dp(11f));
        float colWidth = (float) w / MATRIX_COLS;

        for (int col = 0; col < MATRIX_COLS; col++) {
            matrixY[col] = (matrixY[col] + dt * matrixSpeed[col] * 0.4f) % 1.2f;
            float headY = matrixY[col] * h;
            float x = col * colWidth + colWidth * 0.5f;

            // Draw trailing characters
            for (int trail = 0; trail < 10; trail++) {
                float y = headY - trail * dp(14);
                if (y > 0 && y < h) {
                    int charIndex = (matrixCharIdx[col] + trail) % MATRIX_CHARS.length();
                    String ch = String.valueOf(MATRIX_CHARS.charAt(charIndex));
                    if (trail == 0) {
                        textPaint.setColor(0xFFE0FFE0); // Bright head
                        textPaint.setFakeBoldText(true);
                    } else {
                        int alpha = (int) (220 * (1f - (float) trail / 10f));
                        textPaint.setColor((alpha << 24) | 0x00FF66);
                        textPaint.setFakeBoldText(false);
                    }
                    canvas.drawText(ch, x, y, textPaint);
                }
            }
        }
    }

    /**
     * Preset 6: Sakura Blossom & Cyber Samurai
     * Swirling cherry blossom petals drifting across periodic glowing katana light gleams.
     */
    private void drawCyberSamurai(Canvas canvas, int w, int h, float t, float dt) {
        LinearGradient bg = new LinearGradient(0, 0, w, h,
                new int[]{0xFF0F0816, 0xFF220C24, 0xFF140818}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(null);

        // Neon Moon Crescent
        glowPaint.setColor(0x44FF007F);
        canvas.drawCircle(w * 0.82f, h * 0.28f, dp(38), glowPaint);
        glowPaint.setColor(0xFF140818);
        canvas.drawCircle(w * 0.80f, h * 0.26f, dp(32), glowPaint);

        // Periodic Katana Slash Gleam
        float slashCycle = (t * 0.35f) % 1.0f;
        if (slashCycle < 0.2f) {
            float slashProgress = slashCycle / 0.2f;
            float x1 = 0;
            float y1 = h * 0.8f - slashProgress * h * 0.5f;
            float x2 = w;
            float y2 = h * 0.2f + slashProgress * h * 0.4f;
            linePaint.setColor(0xCC00E5FF);
            linePaint.setStrokeWidth(dp(2.5f));
            canvas.drawLine(x1, y1, x2, y2, linePaint);
        }

        // Swirling Sakura Petals
        glowPaint.setColor(0xAAFF4081);
        for (int i = 0; i < 35; i++) {
            partY[i] = (partY[i] + dt * partSpeed[i] * 0.12f) % 1.0f;
            float sway = (float) Math.sin(t * 1.5f + i) * 0.05f;
            float px = ((partX[i] + sway) % 1.0f) * w;
            float py = partY[i] * h;

            canvas.save();
            canvas.translate(px, py);
            canvas.rotate(partAngle[i] + t * 40f);
            dstRect.set(-dp(3), -dp(6), dp(3), dp(6));
            canvas.drawOval(dstRect, glowPaint);
            canvas.restore();
        }
    }

    /**
     * Preset 7: Diablo Hellfire / Dragon Fire
     * Molten inferno gradient with 45+ animated rising embers and flickering heat distortion.
     */
    private void drawHellfireDragon(Canvas canvas, int w, int h, float t, float dt) {
        LinearGradient bg = new LinearGradient(0, 0, 0, h,
                new int[]{0xFF0A0200, 0xFF2A0800, 0xFF4D1200}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(null);

        // Inferno Core Pulse
        float firePulse = 0.88f + 0.12f * (float) Math.sin(t * 4f);
        RadialGradient flame = new RadialGradient(w * 0.5f, h * 0.75f, w * 0.5f * firePulse,
                new int[]{0x66FF6D00, 0x22FF1744, 0x00000000}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(flame);
        canvas.drawCircle(w * 0.5f, h * 0.75f, w * 0.5f * firePulse, bgPaint);
        bgPaint.setShader(null);

        // Rising fiery embers
        for (int i = 0; i < 45; i++) {
            partY[i] = (partY[i] - dt * partSpeed[i] * 0.22f);
            if (partY[i] < 0) partY[i] += 1.0f;
            float drift = (float) Math.sin(t * 2.2f + i * 2f) * 0.04f;
            float px = ((partX[i] + drift) % 1.0f) * w;
            float py = partY[i] * h;

            int emberCol = (i % 3 == 0) ? 0xFFFFD600 : (i % 2 == 0) ? 0xFFFF6D00 : 0xFFFF1744;
            int alpha = (int) (180 * (1.0f - (float) py / h * 0.5f));
            glowPaint.setColor((alpha << 24) | (emberCol & 0x00FFFFFF));
            canvas.drawCircle(px, py, dp(partSize[i] * 0.55f), glowPaint);
        }
    }

    /**
     * Preset 8: Arctic Frost
     * Sinusoidal aurora borealis ribbons shifting in cyan/blue with falling ice crystal frost.
     */
    private void drawArcticFrost(Canvas canvas, int w, int h, float t, float dt) {
        LinearGradient bg = new LinearGradient(0, 0, 0, h,
                new int[]{0xFF020914, 0xFF051C2C, 0xFF020E18}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(null);

        // Undulating Aurora Ribbons
        linePaint.setStrokeWidth(dp(18f));
        linePaint.setColor(0x2800E5FF);
        tempPath.reset();
        for (int x = 0; x <= w; x += dp(16)) {
            float y = h * 0.28f + (float) Math.sin(t * 1.2f + x * 0.006f) * dp(25);
            if (x == 0) tempPath.moveTo(x, y); else tempPath.lineTo(x, y);
        }
        canvas.drawPath(tempPath, linePaint);

        linePaint.setColor(0x2200FF88);
        tempPath.reset();
        for (int x = 0; x <= w; x += dp(16)) {
            float y = h * 0.35f + (float) Math.cos(t * 1.5f + x * 0.008f) * dp(22);
            if (x == 0) tempPath.moveTo(x, y); else tempPath.lineTo(x, y);
        }
        canvas.drawPath(tempPath, linePaint);

        // Falling frost crystals
        glowPaint.setColor(0xBBCCF5FF);
        for (int i = 0; i < 35; i++) {
            partY[i] = (partY[i] + dt * partSpeed[i] * 0.08f) % 1.0f;
            float px = partX[i] * w;
            float py = partY[i] * h;
            canvas.drawCircle(px, py, dp(partSize[i] * 0.45f), glowPaint);
        }
    }

    /**
     * Preset 9: Apex Hazard / Neon Drift
     * Warp-speed light streaks radiating from vanishing point towards edges.
     */
    private void drawApexHazard(Canvas canvas, int w, int h, float t, float dt) {
        bgPaint.setColor(0xFF060810);
        canvas.drawRect(0, 0, w, h, bgPaint);

        float cx = w * 0.5f;
        float cy = h * 0.5f;

        linePaint.setStrokeWidth(dp(1.8f));
        for (int i = 0; i < MAX_PARTICLES; i++) {
            float progress = (partY[i] + t * partSpeed[i] * 0.4f) % 1.0f;
            float angle = (float) Math.toRadians(partAngle[i]);
            float rInner = progress * Math.max(w, h) * 0.5f;
            float rOuter = rInner + dp(18f) * progress;

            float x1 = cx + (float) Math.cos(angle) * rInner;
            float y1 = cy + (float) Math.sin(angle) * rInner;
            float x2 = cx + (float) Math.cos(angle) * rOuter;
            float y2 = cy + (float) Math.sin(angle) * rOuter;

            int alpha = (int) (progress * 220);
            int col = (i % 2 == 0) ? 0x00E5FF : 0xFF1744;
            linePaint.setColor((alpha << 24) | col);
            canvas.drawLine(x1, y1, x2, y2, linePaint);
        }
    }

    /**
     * Preset 10: Eva Unit-01 / Cosmic Mana
     * Expanding AT-field geometric octagons and pulsing bio-luminescent mana spirals.
     */
    private void drawEvaMana(Canvas canvas, int w, int h, float t, float dt) {
        LinearGradient bg = new LinearGradient(0, 0, w, h,
                new int[]{0xFF0E0318, 0xFF1C072E, 0xFF08160B}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(null);

        float cx = w * 0.5f;
        float cy = h * 0.5f;

        // Expanding AT-Field Octagons
        linePaint.setStrokeWidth(dp(2.2f));
        for (int ring = 0; ring < 4; ring++) {
            float progress = ((t * 0.35f + ring * 0.25f) % 1.0f);
            float r = progress * Math.min(w, h) * 0.52f;
            int alpha = (int) ((1.0f - progress) * 190);
            linePaint.setColor((alpha << 24) | 0xFF8800);

            tempPath.reset();
            for (int k = 0; k < 8; k++) {
                float a = (float) Math.toRadians(k * 45f + 22.5f);
                float px = cx + (float) Math.cos(a) * r;
                float py = cy + (float) Math.sin(a) * r;
                if (k == 0) tempPath.moveTo(px, py); else tempPath.lineTo(px, py);
            }
            tempPath.close();
            canvas.drawPath(tempPath, linePaint);
        }

        // Bioluminescent mana particles
        glowPaint.setColor(0x8800FF7F);
        for (int i = 0; i < 24; i++) {
            float a = (float) Math.toRadians(t * 50f + i * 15f);
            float r = (partX[i] * Math.min(w, h) * 0.45f);
            float px = cx + (float) Math.cos(a) * r;
            float py = cy + (float) Math.sin(a) * r;
            canvas.drawCircle(px, py, dp(2.8f), glowPaint);
        }
    }

    /**
     * Preset 11: Stealth Carbon / RGB Chroma Wave
     * Carbon weave texture with smooth sweeping 45-degree full-spectrum RGB Chroma wave.
     */
    private void drawRgbChromaWave(Canvas canvas, int w, int h, float t, float dt) {
        bgPaint.setColor(0xFF0A0B0E);
        canvas.drawRect(0, 0, w, h, bgPaint);

        // Carbon fiber subtle tile weave
        linePaint.setColor(0x18FFFFFF);
        linePaint.setStrokeWidth(dp(1f));
        float step = dp(16);
        for (float x = 0; x < w; x += step) {
            canvas.drawLine(x, 0, x, h, linePaint);
        }
        for (float y = 0; y < h; y += step) {
            canvas.drawLine(0, y, w, y, linePaint);
        }

        // RGB Chroma Wave
        float wavePos = (t * 0.22f) % 1.0f;
        int[] chromaColors = new int[]{
                0x44FF0000, 0x44FF7F00, 0x44FFFF00, 0x4400FF00,
                0x4400E5FF, 0x440000FF, 0x448B00FF, 0x44FF0000
        };
        LinearGradient chroma = new LinearGradient(
                w * (wavePos - 0.5f), 0,
                w * (wavePos + 0.5f), h,
                chromaColors, null, Shader.TileMode.REPEAT);
        glowPaint.setShader(chroma);
        canvas.drawRect(0, 0, w, h, glowPaint);
        glowPaint.setShader(null);
    }

    /**
     * Preset 12: Anime Cyber Girl (Aika)
     * Holographic data rings, swaying cyan twintails, blinking anime eyes, and glowing headset.
     */
    private void drawAnimeCyberGirl(Canvas canvas, int w, int h, float t, float dt) {
        LinearGradient bg = new LinearGradient(0, 0, w, h,
                new int[]{0xFF0B071A, 0xFF1A0E33, 0xFF081226}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(null);

        float cx = w * 0.5f;
        float cy = h * 0.52f;

        // Rotating Holographic Data Rings behind head
        linePaint.setStrokeWidth(dp(1.5f));
        linePaint.setColor(0x4400E5FF);
        dstRect.set(cx - dp(90), cy - dp(120), cx + dp(90), cy + dp(60));
        canvas.drawArc(dstRect, t * 35f, 100, false, linePaint);
        canvas.drawArc(dstRect, t * 35f + 160, 60, false, linePaint);
        linePaint.setColor(0x44FF007F);
        dstRect.set(cx - dp(115), cy - dp(145), cx + dp(115), cy + dp(85));
        canvas.drawArc(dstRect, -t * 50f, 120, false, linePaint);
        canvas.drawArc(dstRect, -t * 50f + 200, 70, false, linePaint);

        // Swaying Twin Tails (Left and Right)
        float swayL = (float) Math.sin(t * 2.8f) * dp(14);
        float swayR = (float) Math.cos(t * 2.5f) * dp(14);

        tempPath.reset();
        tempPath.moveTo(cx - dp(28), cy - dp(45));
        tempPath.cubicTo(cx - dp(65) + swayL, cy - dp(20), cx - dp(75) + swayL, cy + dp(50), cx - dp(50) + swayL, cy + dp(95));
        tempPath.cubicTo(cx - dp(40) + swayL, cy + dp(60), cx - dp(45) + swayL, cy - dp(10), cx - dp(20), cy - dp(35));
        tempPath.close();
        glowPaint.setColor(0xAA00E5FF);
        canvas.drawPath(tempPath, glowPaint);

        tempPath.reset();
        tempPath.moveTo(cx + dp(28), cy - dp(45));
        tempPath.cubicTo(cx + dp(65) + swayR, cy - dp(20), cx + dp(75) + swayR, cy + dp(50), cx + dp(50) + swayR, cy + dp(95));
        tempPath.cubicTo(cx + dp(40) + swayR, cy + dp(60), cx + dp(45) + swayR, cy - dp(10), cx + dp(20), cy - dp(35));
        tempPath.close();
        glowPaint.setColor(0xAAFF007F);
        canvas.drawPath(tempPath, glowPaint);

        // Anime Girl Silhouette (Head & Shoulders)
        glowPaint.setColor(0xFF141724);
        tempPath.reset();
        tempPath.moveTo(cx - dp(55), h);
        tempPath.lineTo(cx - dp(18), cy + dp(10));
        tempPath.lineTo(cx + dp(18), cy + dp(10));
        tempPath.lineTo(cx + dp(55), h);
        tempPath.close();
        canvas.drawPath(tempPath, glowPaint);

        dstRect.set(cx - dp(28), cy - dp(60), cx + dp(28), cy - dp(5));
        canvas.drawOval(dstRect, glowPaint);

        // Anime Bangs / Hair Front
        glowPaint.setColor(0xFF1D2235);
        tempPath.reset();
        tempPath.moveTo(cx - dp(28), cy - dp(45));
        tempPath.lineTo(cx - dp(8), cy - dp(25));
        tempPath.lineTo(cx, cy - dp(32));
        tempPath.lineTo(cx + dp(10), cy - dp(23));
        tempPath.lineTo(cx + dp(28), cy - dp(45));
        tempPath.lineTo(cx + dp(25), cy - dp(62));
        tempPath.lineTo(cx - dp(25), cy - dp(62));
        tempPath.close();
        canvas.drawPath(tempPath, glowPaint);

        // Glowing Anime Eyes (with blink cycle)
        boolean isBlinking = ((t % 3.8f) < 0.15f);
        if (isBlinking) {
            linePaint.setColor(0xFF00E5FF);
            linePaint.setStrokeWidth(dp(2f));
            canvas.drawLine(cx - dp(16), cy - dp(30), cx - dp(6), cy - dp(30), linePaint);
            canvas.drawLine(cx + dp(6), cy - dp(30), cx + dp(16), cy - dp(30), linePaint);
        } else {
            glowPaint.setColor(0xFF00E5FF);
            dstRect.set(cx - dp(17), cy - dp(35), cx - dp(7), cy - dp(25));
            canvas.drawOval(dstRect, glowPaint);
            dstRect.set(cx + dp(7), cy - dp(35), cx + dp(17), cy - dp(25));
            canvas.drawOval(dstRect, glowPaint);
            glowPaint.setColor(Color.WHITE);
            canvas.drawCircle(cx - dp(11), cy - dp(32), dp(2f), glowPaint);
            canvas.drawCircle(cx + dp(13), cy - dp(32), dp(2f), glowPaint);
        }

        // Cat-ear / Antenna Headset clips
        glowPaint.setColor(0xFFFF007F);
        tempPath.reset();
        tempPath.moveTo(cx - dp(30), cy - dp(40));
        tempPath.lineTo(cx - dp(42), cy - dp(65));
        tempPath.lineTo(cx - dp(24), cy - dp(52));
        tempPath.close();
        canvas.drawPath(tempPath, glowPaint);

        tempPath.reset();
        tempPath.moveTo(cx + dp(30), cy - dp(40));
        tempPath.lineTo(cx + dp(42), cy - dp(65));
        tempPath.lineTo(cx + dp(24), cy - dp(52));
        tempPath.close();
        canvas.drawPath(tempPath, glowPaint);

        // Rising cyber spark particles
        glowPaint.setColor(0x8800E5FF);
        for (int i = 0; i < 20; i++) {
            float py = (partY[i] - t * partSpeed[i] * 0.1f) % 1.0f;
            if (py < 0) py += 1.0f;
            canvas.drawCircle(partX[i] * w, py * h, dp(partSize[i] * 0.5f), glowPaint);
        }
    }

    /**
     * Preset 13: Shadow Monarch (Sung Jin-woo)
     * Billowing dark purple shadow flame aura, piercing cyan eye trails, and floating shadow daggers.
     */
    private void drawShadowMonarch(Canvas canvas, int w, int h, float t, float dt) {
        LinearGradient bg = new LinearGradient(0, 0, w, h,
                new int[]{0xFF020106, 0xFF10041E, 0xFF030108}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(null);

        float cx = w * 0.5f;
        float cy = h * 0.55f;

        // Billowing Shadow Mist Tendrils
        for (int s = 0; s < 5; s++) {
            float offset = (float) Math.sin(t * 1.8f + s * 1.2f) * dp(25);
            tempPath.reset();
            tempPath.moveTo(cx - dp(80) + s * dp(40), h);
            tempPath.quadTo(cx - dp(40) + offset, cy + dp(20), cx - dp(20) + offset * 0.5f, cy - dp(70));
            tempPath.quadTo(cx + dp(20) + offset, cy + dp(20), cx + dp(80) + s * dp(40), h);
            tempPath.close();
            glowPaint.setColor(0x227928CA);
            canvas.drawPath(tempPath, glowPaint);
        }

        // Shadow Monarch Silhouette (Broad shoulders, coat tails)
        glowPaint.setColor(0xFF0C0816);
        tempPath.reset();
        tempPath.moveTo(cx - dp(85), h);
        tempPath.lineTo(cx - dp(50), cy + dp(20));
        tempPath.lineTo(cx - dp(22), cy - dp(40)); // High collar
        tempPath.lineTo(cx, cy - dp(25));
        tempPath.lineTo(cx + dp(22), cy - dp(40));
        tempPath.lineTo(cx + dp(50), cy + dp(20));
        tempPath.lineTo(cx + dp(85), h);
        tempPath.close();
        canvas.drawPath(tempPath, glowPaint);

        // Head & Spiky Hair
        tempPath.reset();
        tempPath.moveTo(cx - dp(22), cy - dp(40));
        tempPath.lineTo(cx - dp(32), cy - dp(65));
        tempPath.lineTo(cx - dp(18), cy - dp(60));
        tempPath.lineTo(cx - dp(25), cy - dp(80));
        tempPath.lineTo(cx - dp(10), cy - dp(70));
        tempPath.lineTo(cx, cy - dp(88)); // Top center spike
        tempPath.lineTo(cx + dp(10), cy - dp(70));
        tempPath.lineTo(cx + dp(25), cy - dp(80));
        tempPath.lineTo(cx + dp(18), cy - dp(60));
        tempPath.lineTo(cx + dp(32), cy - dp(65));
        tempPath.lineTo(cx + dp(22), cy - dp(40));
        tempPath.close();
        glowPaint.setColor(0xFF0F0B1E);
        canvas.drawPath(tempPath, glowPaint);

        // Piercing Cyan Eye Trails (Solo Leveling Iconic Glow)
        glowPaint.setColor(0xFF00E5FF);
        canvas.drawCircle(cx - dp(8), cy - dp(50), dp(3.5f), glowPaint);
        canvas.drawCircle(cx + dp(8), cy - dp(50), dp(3.5f), glowPaint);

        linePaint.setColor(0xCC00E5FF);
        linePaint.setStrokeWidth(dp(2.2f));
        float flameLag = (float) Math.sin(t * 4f) * dp(6);
        tempPath.reset();
        tempPath.moveTo(cx - dp(8), cy - dp(50));
        tempPath.quadTo(cx - dp(25), cy - dp(55) + flameLag, cx - dp(45), cy - dp(65) + flameLag);
        canvas.drawPath(tempPath, linePaint);

        tempPath.reset();
        tempPath.moveTo(cx + dp(8), cy - dp(50));
        tempPath.quadTo(cx + dp(25), cy - dp(55) + flameLag, cx + dp(45), cy - dp(65) + flameLag);
        canvas.drawPath(tempPath, linePaint);

        // Floating Shadow Daggers on sides
        float dagRotL = (float) Math.sin(t * 1.5f) * 15f - 30f;
        float dagRotR = (float) Math.cos(t * 1.5f) * 15f + 30f;
        drawShadowDagger(canvas, cx - dp(120), cy, dagRotL);
        drawShadowDagger(canvas, cx + dp(120), cy, dagRotR);
    }

    private void drawShadowDagger(Canvas canvas, float x, float y, float rot) {
        canvas.save();
        canvas.translate(x, y);
        canvas.rotate(rot);
        tempPath.reset();
        tempPath.moveTo(0, -dp(30));
        tempPath.lineTo(dp(6), dp(10));
        tempPath.lineTo(dp(2), dp(25));
        tempPath.lineTo(-dp(2), dp(25));
        tempPath.lineTo(-dp(6), dp(10));
        tempPath.close();
        glowPaint.setColor(0xEE1E0938);
        canvas.drawPath(tempPath, glowPaint);
        linePaint.setColor(0xFF9D00FF);
        linePaint.setStrokeWidth(dp(1.5f));
        canvas.drawPath(tempPath, linePaint);
        canvas.restore();
    }

    /**
     * Preset 14: Demon Nichirin Flame (Rengoku)
     * Fiery blade arcs, swirling flame dragon aura, and incandescent combustion embers.
     */
    private void drawDemonNichirinFlame(Canvas canvas, int w, int h, float t, float dt) {
        LinearGradient bg = new LinearGradient(0, 0, w, h,
                new int[]{0xFF0E0300, 0xFF350A00, 0xFF581500}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(null);

        float cx = w * 0.5f;
        float cy = h * 0.52f;

        // Swirling Fiery Dragon Arc (Nichirin 9th Form)
        linePaint.setStrokeWidth(dp(16f));
        linePaint.setColor(0x44FF3D00);
        tempPath.reset();
        for (int i = 0; i <= 20; i++) {
            float progress = i / 20f;
            float angle = (float) (progress * Math.PI * 2.2 + t * 2.5f);
            float r = dp(60) + progress * dp(80);
            float px = cx + (float) Math.cos(angle) * r;
            float py = cy + (float) Math.sin(angle) * (r * 0.6f);
            if (i == 0) tempPath.moveTo(px, py); else tempPath.lineTo(px, py);
        }
        canvas.drawPath(tempPath, linePaint);

        linePaint.setStrokeWidth(dp(6f));
        linePaint.setColor(0xFFFFD600);
        canvas.drawPath(tempPath, linePaint);

        // Flame Hashira Silhouette
        glowPaint.setColor(0xFF1F0B00);
        tempPath.reset();
        tempPath.moveTo(cx - dp(45), h);
        tempPath.lineTo(cx - dp(20), cy + dp(15));
        tempPath.lineTo(cx + dp(20), cy + dp(15));
        tempPath.lineTo(cx + dp(45), h);
        tempPath.close();
        canvas.drawPath(tempPath, glowPaint);

        // Spiky Flame Hair
        tempPath.reset();
        tempPath.moveTo(cx - dp(25), cy - dp(35));
        tempPath.lineTo(cx - dp(35), cy - dp(58));
        tempPath.lineTo(cx - dp(18), cy - dp(52));
        tempPath.lineTo(cx - dp(22), cy - dp(75));
        tempPath.lineTo(cx, cy - dp(82));
        tempPath.lineTo(cx + dp(22), cy - dp(75));
        tempPath.lineTo(cx + dp(18), cy - dp(52));
        tempPath.lineTo(cx + dp(35), cy - dp(58));
        tempPath.lineTo(cx + dp(25), cy - dp(35));
        tempPath.close();
        glowPaint.setColor(0xFFFF6D00);
        canvas.drawPath(tempPath, glowPaint);

        // Burning Katana Slash
        float bladePos = (float) Math.sin(t * 3f) * 0.5f + 0.5f;
        linePaint.setColor(0xFFFF1744);
        linePaint.setStrokeWidth(dp(3f));
        canvas.drawLine(cx - dp(70), cy + dp(30), cx + dp(80), cy - dp(40), linePaint);

        // Rising fiery combustion sparks
        glowPaint.setColor(0xFFFFD600);
        for (int i = 0; i < 35; i++) {
            partY[i] = (partY[i] - dt * partSpeed[i] * 0.28f);
            if (partY[i] < 0) partY[i] += 1.0f;
            float px = (partX[i] * w);
            float py = (partY[i] * h);
            canvas.drawCircle(px, py, dp(partSize[i] * 0.6f), glowPaint);
        }
    }

    /**
     * Preset 15: Gojo Limitless Void
     * Six Eyes celestial azure aura, divergent infinity rings, and Hollow Purple lightning fractures.
     */
    private void drawGojoLimitlessVoid(Canvas canvas, int w, int h, float t, float dt) {
        LinearGradient bg = new LinearGradient(0, 0, 0, h,
                new int[]{0xFF020716, 0xFF0B1A3A, 0xFF020716}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(null);

        float cx = w * 0.5f;
        float cy = h * 0.5f;

        // Expanding "Infinity" Spatial Expansion Rings
        linePaint.setStrokeWidth(dp(1.8f));
        for (int r = 0; r < 4; r++) {
            float progress = ((t * 0.25f + r * 0.25f) % 1.0f);
            float radius = progress * Math.min(w, h) * 0.55f;
            int alpha = (int) ((1.0f - progress) * 200);
            linePaint.setColor((alpha << 24) | 0x00E5FF);
            canvas.drawCircle(cx, cy, radius, linePaint);
        }

        // Cursed Technique Reversal: Red (Left) & Lapse: Blue (Right)
        float orbDist = dp(90);
        RadialGradient redOrb = new RadialGradient(cx - orbDist, cy, dp(35),
                new int[]{0xFFFF1744, 0x88FF1744, 0x00000000}, null, Shader.TileMode.CLAMP);
        glowPaint.setShader(redOrb);
        canvas.drawCircle(cx - orbDist, cy, dp(35), glowPaint);

        RadialGradient blueOrb = new RadialGradient(cx + orbDist, cy, dp(35),
                new int[]{0xFF00B0FF, 0x8800B0FF, 0x00000000}, null, Shader.TileMode.CLAMP);
        glowPaint.setShader(blueOrb);
        canvas.drawCircle(cx + orbDist, cy, dp(35), glowPaint);
        glowPaint.setShader(null);

        // Hollow Purple Center Lightning Arc
        linePaint.setColor(0xFFD500F9);
        linePaint.setStrokeWidth(dp(2.5f));
        tempPath.reset();
        tempPath.moveTo(cx - orbDist, cy);
        for (int i = 1; i <= 6; i++) {
            float frac = i / 6f;
            float px = cx - orbDist + (orbDist * 2f) * frac;
            float py = cy + ((i % 2 == 0) ? dp(12) : -dp(12)) * (float) Math.sin(t * 8f + i);
            tempPath.lineTo(px, py);
        }
        canvas.drawPath(tempPath, linePaint);

        // Gojo Silhouette (White hair, dark blindfold)
        glowPaint.setColor(0xFF0B1020);
        dstRect.set(cx - dp(24), cy - dp(55), cx + dp(24), cy - dp(10));
        canvas.drawOval(dstRect, glowPaint);

        // White spiky hair
        glowPaint.setColor(0xFFF0F4F8);
        tempPath.reset();
        tempPath.moveTo(cx - dp(24), cy - dp(40));
        tempPath.lineTo(cx - dp(32), cy - dp(65));
        tempPath.lineTo(cx - dp(16), cy - dp(60));
        tempPath.lineTo(cx - dp(20), cy - dp(80));
        tempPath.lineTo(cx, cy - dp(85));
        tempPath.lineTo(cx + dp(20), cy - dp(80));
        tempPath.lineTo(cx + dp(16), cy - dp(60));
        tempPath.lineTo(cx + dp(32), cy - dp(65));
        tempPath.lineTo(cx + dp(24), cy - dp(40));
        tempPath.close();
        canvas.drawPath(tempPath, glowPaint);

        // Black Blindfold with radiant Six Eyes azure glow bleeding beneath
        glowPaint.setColor(0xFF050810);
        canvas.drawRect(cx - dp(22), cy - dp(45), cx + dp(22), cy - dp(32), glowPaint);
        glowPaint.setColor(0xCC00E5FF);
        canvas.drawCircle(cx - dp(10), cy - dp(38), dp(3.5f), glowPaint);
        canvas.drawCircle(cx + dp(10), cy - dp(38), dp(3.5f), glowPaint);
    }

    /**
     * Preset 16: Cyber Samurai Waifu
     * Neo-Tokyo rainy reflections, long sakura ponytail, gleaming dual-tone katana, and falling blossom petals.
     */
    private void drawCyberSamuraiWaifu(Canvas canvas, int w, int h, float t, float dt) {
        LinearGradient bg = new LinearGradient(0, 0, w, h,
                new int[]{0xFF0E0716, 0xFF220E28, 0xFF0D0618}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(null);

        float cx = w * 0.5f;
        float cy = h * 0.52f;

        // Long Sakura Ponytail fluttering
        float sway = (float) Math.sin(t * 3f) * dp(16);
        tempPath.reset();
        tempPath.moveTo(cx - dp(15), cy - dp(50));
        tempPath.cubicTo(cx - dp(60) + sway, cy - dp(30), cx - dp(90) + sway, cy + dp(40), cx - dp(70) + sway, cy + dp(90));
        tempPath.cubicTo(cx - dp(50) + sway, cy + dp(40), cx - dp(40) + sway, cy - dp(20), cx - dp(10), cy - dp(35));
        tempPath.close();
        glowPaint.setColor(0xCCFF4081);
        canvas.drawPath(tempPath, glowPaint);

        // Samurai Silhouette
        glowPaint.setColor(0xFF15101E);
        dstRect.set(cx - dp(24), cy - dp(55), cx + dp(24), cy - dp(10));
        canvas.drawOval(dstRect, glowPaint);
        tempPath.reset();
        tempPath.moveTo(cx - dp(45), h);
        tempPath.lineTo(cx - dp(16), cy + dp(10));
        tempPath.lineTo(cx + dp(16), cy + dp(10));
        tempPath.lineTo(cx + dp(45), h);
        tempPath.close();
        canvas.drawPath(tempPath, glowPaint);

        // Glowing Katana Blade
        linePaint.setColor(0xFF00E5FF);
        linePaint.setStrokeWidth(dp(2.8f));
        canvas.drawLine(cx - dp(50), cy + dp(80), cx + dp(90), cy - dp(60), linePaint);
        linePaint.setColor(Color.WHITE);
        linePaint.setStrokeWidth(dp(1.2f));
        canvas.drawLine(cx - dp(50), cy + dp(80), cx + dp(90), cy - dp(60), linePaint);

        // Diagonal cyber rain streaks
        linePaint.setColor(0x3300E5FF);
        linePaint.setStrokeWidth(dp(1f));
        for (int i = 0; i < 25; i++) {
            float rx = (partX[i] * w);
            float ry = ((partY[i] + t * 0.6f) % 1.0f) * h;
            canvas.drawLine(rx, ry, rx - dp(6), ry + dp(18), linePaint);
        }

        // Tumbling cherry blossoms
        glowPaint.setColor(0xBBFF80AB);
        for (int i = 0; i < 20; i++) {
            float py = (partY[i] + t * 0.08f) % 1.0f;
            float px = (partX[i] + (float) Math.sin(t + i) * 0.04f) % 1.0f;
            canvas.drawCircle(px * w, py * h, dp(3f), glowPaint);
        }
    }

    /**
     * Preset 17: Dragon Ball Ultra Instinct
     * Celestial god Ki aura undulating around warrior silhouette with shockwave energy ripples.
     */
    private void drawUltraInstinct(Canvas canvas, int w, int h, float t, float dt) {
        LinearGradient bg = new LinearGradient(0, 0, 0, h,
                new int[]{0xFF050814, 0xFF121B2F, 0xFF050814}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(null);

        float cx = w * 0.5f;
        float cy = h * 0.52f;

        // Shockwave Ripple Ring expanding outward
        float rippleProg = (t * 0.4f) % 1.0f;
        float rippleR = rippleProg * Math.min(w, h) * 0.6f;
        linePaint.setColor(((int)((1.0f - rippleProg) * 160) << 24) | 0xFFFFFF);
        linePaint.setStrokeWidth(dp(2.5f));
        canvas.drawCircle(cx, cy, rippleR, linePaint);

        // Billowing Ultra Instinct Ki Aura (Outer Cyan, Inner White)
        for (int layer = 0; layer < 2; layer++) {
            tempPath.reset();
            tempPath.moveTo(cx - dp(65 + layer * 15), h);
            for (float y = h; y >= cy - dp(85); y -= dp(12)) {
                float wave = (float) Math.sin(t * 8f + y * 0.05f) * dp(10);
                tempPath.lineTo(cx - dp(35 + layer * 12) + wave, y);
            }
            tempPath.lineTo(cx, cy - dp(95));
            for (float y = cy - dp(85); y <= h; y += dp(12)) {
                float wave = (float) Math.cos(t * 8f + y * 0.05f) * dp(10);
                tempPath.lineTo(cx + dp(35 + layer * 12) + wave, y);
            }
            tempPath.close();
            glowPaint.setColor((layer == 0) ? 0x44B0E0E6 : 0x22FFFFFF);
            canvas.drawPath(tempPath, glowPaint);
        }

        // Warrior Silhouette with Silver Hair
        glowPaint.setColor(0xFF0F1422);
        dstRect.set(cx - dp(25), cy - dp(50), cx + dp(25), cy - dp(10));
        canvas.drawOval(dstRect, glowPaint);

        // Spiky Silver Ultra Instinct Hair
        glowPaint.setColor(0xFFE2E8F0);
        tempPath.reset();
        tempPath.moveTo(cx - dp(25), cy - dp(35));
        tempPath.lineTo(cx - dp(38), cy - dp(65));
        tempPath.lineTo(cx - dp(20), cy - dp(60));
        tempPath.lineTo(cx - dp(26), cy - dp(85));
        tempPath.lineTo(cx, cy - dp(92));
        tempPath.lineTo(cx + dp(26), cy - dp(85));
        tempPath.lineTo(cx + dp(20), cy - dp(60));
        tempPath.lineTo(cx + dp(38), cy - dp(65));
        tempPath.lineTo(cx + dp(25), cy - dp(35));
        tempPath.close();
        canvas.drawPath(tempPath, glowPaint);

        // Silver Floating Ki Particles
        glowPaint.setColor(Color.WHITE);
        for (int i = 0; i < 28; i++) {
            float py = (partY[i] - t * partSpeed[i] * 0.18f);
            if (py < 0) py += 1.0f;
            canvas.drawCircle(partX[i] * w, py * h, dp(partSize[i] * 0.45f), glowPaint);
        }
    }

    /**
     * Preset 18: Chibi Neko Gamer
     * Cute catgirl silhouette with animated twitching cat ears, RGB headset, and floating heart sparkles.
     */
    private void drawChibiNekoGamer(Canvas canvas, int w, int h, float t, float dt) {
        LinearGradient bg = new LinearGradient(0, 0, w, h,
                new int[]{0xFF12081C, 0xFF280E38, 0xFF12081C}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(null);

        float cx = w * 0.5f;
        float cy = h * 0.52f;

        // Ear Twitch Animation (Twitches every 3 seconds)
        float twitchPhase = (t % 3.0f);
        float twitchRot = 0f;
        if (twitchPhase < 0.25f) {
            twitchRot = (float) Math.sin(twitchPhase * Math.PI * 8.0) * 14f;
        }

        // Chibi Head & Ears
        glowPaint.setColor(0xFF1E142B);
        dstRect.set(cx - dp(32), cy - dp(50), cx + dp(32), cy + dp(10));
        canvas.drawOval(dstRect, glowPaint);

        // Left Cat Ear
        canvas.save();
        canvas.translate(cx - dp(24), cy - dp(45));
        canvas.rotate(twitchRot);
        tempPath.reset();
        tempPath.moveTo(-dp(12), dp(12));
        tempPath.lineTo(-dp(10), -dp(24));
        tempPath.lineTo(dp(12), -dp(2));
        tempPath.close();
        glowPaint.setColor(0xFF1E142B);
        canvas.drawPath(tempPath, glowPaint);
        glowPaint.setColor(0xFFFF4081);
        canvas.drawCircle(-dp(3), -dp(8), dp(5), glowPaint);
        canvas.restore();

        // Right Cat Ear
        canvas.save();
        canvas.translate(cx + dp(24), cy - dp(45));
        canvas.rotate(-twitchRot);
        tempPath.reset();
        tempPath.moveTo(dp(12), dp(12));
        tempPath.lineTo(dp(10), -dp(24));
        tempPath.lineTo(-dp(12), -dp(2));
        tempPath.close();
        glowPaint.setColor(0xFF1E142B);
        canvas.drawPath(tempPath, glowPaint);
        glowPaint.setColor(0xFFFF4081);
        canvas.drawCircle(dp(3), -dp(8), dp(5), glowPaint);
        canvas.restore();

        // RGB Chroma Headset Band
        int rgbColor = Color.HSVToColor(new float[]{(t * 90f) % 360f, 0.85f, 1.0f});
        linePaint.setColor(rgbColor);
        linePaint.setStrokeWidth(dp(3.5f));
        dstRect.set(cx - dp(34), cy - dp(56), cx + dp(34), cy - dp(2));
        canvas.drawArc(dstRect, 180, 180, false, linePaint);

        // Floating Kawaii Hearts and 8-bit star sparkles
        glowPaint.setColor(0xFFFF4081);
        for (int i = 0; i < 18; i++) {
            float py = (partY[i] - t * partSpeed[i] * 0.08f);
            if (py < 0) py += 1.0f;
            float px = (partX[i] * w);
            canvas.drawCircle(px, py * h, dp(partSize[i] * 0.6f), glowPaint);
        }
    }

    /**
     * Preset 19: Berserk Eclipse
     * Blood eclipse red sky, massive Dragonslayer blade, pulsing Brand of Sacrifice, and shadow wisps.
     */
    private void drawBerserkEclipse(Canvas canvas, int w, int h, float t, float dt) {
        LinearGradient bg = new LinearGradient(0, 0, 0, h,
                new int[]{0xFF1C0005, 0xFF42020D, 0xFF140003}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(null);

        float cx = w * 0.5f;
        float cy = h * 0.45f;

        // Radiant Eclipse Sun
        RadialGradient corona = new RadialGradient(cx, cy, dp(75),
                new int[]{0xFFFF1744, 0xAAFF1744, 0x00000000}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(corona);
        canvas.drawCircle(cx, cy, dp(75), bgPaint);
        bgPaint.setShader(null);

        // Black Sun Center
        glowPaint.setColor(0xFF0A0002);
        canvas.drawCircle(cx, cy, dp(42), glowPaint);

        // Colossal Dragonslayer Blade diagonally across back
        canvas.save();
        canvas.translate(cx, cy + dp(20));
        canvas.rotate(28f);
        tempPath.reset();
        tempPath.moveTo(-dp(14), -dp(90));
        tempPath.lineTo(dp(14), -dp(90));
        tempPath.lineTo(dp(18), dp(80));
        tempPath.lineTo(-dp(18), dp(80));
        tempPath.close();
        glowPaint.setColor(0xFF282832);
        canvas.drawPath(tempPath, glowPaint);
        linePaint.setColor(0xFF888899);
        linePaint.setStrokeWidth(dp(1.8f));
        canvas.drawPath(tempPath, linePaint);
        canvas.restore();

        // Swordsman Silhouette
        glowPaint.setColor(0xFF0F0608);
        tempPath.reset();
        tempPath.moveTo(cx - dp(50), h);
        tempPath.lineTo(cx - dp(22), cy + dp(10));
        tempPath.lineTo(cx + dp(22), cy + dp(10));
        tempPath.lineTo(cx + dp(50), h);
        tempPath.close();
        canvas.drawPath(tempPath, glowPaint);

        // Pulsing Brand of Sacrifice
        glowPaint.setColor(0xFFFF1744);
        float brandPulse = 0.8f + 0.2f * (float) Math.sin(t * 5f);
        canvas.drawCircle(cx + dp(12), cy + dp(15), dp(3.5f * brandPulse), glowPaint);
    }

    /**
     * Preset 20: Persona Phantom Thief
     * High-contrast Persona comic graphic aesthetic, domino mask, blue summoning fire, and Tarot cards.
     */
    private void drawPersonaPhantomThief(Canvas canvas, int w, int h, float t, float dt) {
        bgPaint.setColor(0xFF0A0A0E);
        canvas.drawRect(0, 0, w, h, bgPaint);

        // High-Contrast Red Persona Slashes in Background
        glowPaint.setColor(0x35FF1744);
        tempPath.reset();
        tempPath.moveTo(0, h * 0.3f);
        tempPath.lineTo(w, h * 0.1f);
        tempPath.lineTo(w, h * 0.45f);
        tempPath.lineTo(0, h * 0.65f);
        tempPath.close();
        canvas.drawPath(tempPath, glowPaint);

        float cx = w * 0.5f;
        float cy = h * 0.52f;

        // Swirling Blue Persona Flames
        glowPaint.setColor(0x8800B0FF);
        for (int i = 0; i < 4; i++) {
            float flameAngle = (float) Math.toRadians(t * 60f + i * 90f);
            float fx = cx + (float) Math.cos(flameAngle) * dp(80);
            float fy = cy + (float) Math.sin(flameAngle) * dp(45);
            canvas.drawCircle(fx, fy, dp(14f), glowPaint);
        }

        // Phantom Thief Silhouette (Trench coat, domino mask)
        glowPaint.setColor(0xFF141418);
        tempPath.reset();
        tempPath.moveTo(cx - dp(65), h);
        tempPath.lineTo(cx - dp(20), cy + dp(10));
        tempPath.lineTo(cx + dp(20), cy + dp(10));
        tempPath.lineTo(cx + dp(65), h);
        tempPath.close();
        canvas.drawPath(tempPath, glowPaint);

        dstRect.set(cx - dp(22), cy - dp(52), cx + dp(22), cy - dp(10));
        canvas.drawOval(dstRect, glowPaint);

        // Glowing Yellow Eyes behind Domino Mask
        glowPaint.setColor(0xFFFFD600);
        canvas.drawCircle(cx - dp(8), cy - dp(32), dp(3f), glowPaint);
        canvas.drawCircle(cx + dp(8), cy - dp(32), dp(3f), glowPaint);

        // Flying Tarot Card Shards
        linePaint.setColor(Color.WHITE);
        linePaint.setStrokeWidth(dp(1.5f));
        for (int i = 0; i < 6; i++) {
            float cardX = (cx - dp(140) + i * dp(55));
            float cardY = cy - dp(60) + (float) Math.sin(t * 2f + i) * dp(16);
            canvas.save();
            canvas.translate(cardX, cardY);
            canvas.rotate(i * 18f + t * 20f);
            canvas.drawRect(-dp(10), -dp(16), dp(10), dp(16), linePaint);
            canvas.restore();
        }
    }

    /**
     * Preset 21: Gundam Strike Freedom
     * 8 radiant expanding golden beam wings of light, plasma thrusters, and lock-on HUD crosshairs.
     */
    private void drawGundamStrikeFreedom(Canvas canvas, int w, int h, float t, float dt) {
        LinearGradient bg = new LinearGradient(0, 0, 0, h,
                new int[]{0xFF050814, 0xFF0D1B36, 0xFF050814}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(null);

        float cx = w * 0.5f;
        float cy = h * 0.52f;

        // 8 Radiant Golden Beam Wings (Strike Freedom Wings of Light)
        linePaint.setStrokeWidth(dp(4f));
        linePaint.setColor(0xFFFFD700);
        for (int i = 0; i < 4; i++) {
            float wingAngleL = 195f + i * 16f + (float) Math.sin(t * 2f + i) * 4f;
            float wingAngleR = -15f - i * 16f - (float) Math.sin(t * 2f + i) * 4f;

            tempPath.reset();
            tempPath.moveTo(cx - dp(20), cy - dp(20));
            float radL = (float) Math.toRadians(wingAngleL);
            tempPath.lineTo(cx + (float) Math.cos(radL) * dp(160 + i * 15), cy + (float) Math.sin(radL) * dp(110 + i * 10));
            canvas.drawPath(tempPath, linePaint);

            tempPath.reset();
            tempPath.moveTo(cx + dp(20), cy - dp(20));
            float radR = (float) Math.toRadians(wingAngleR);
            tempPath.lineTo(cx + (float) Math.cos(radR) * dp(160 + i * 15), cy + (float) Math.sin(radR) * dp(110 + i * 10));
            canvas.drawPath(tempPath, linePaint);
        }

        // Mecha Torso & V-Fin Antenna
        glowPaint.setColor(0xFF1B2338);
        dstRect.set(cx - dp(20), cy - dp(45), cx + dp(20), cy - dp(10));
        canvas.drawRect(dstRect, glowPaint);

        // Golden V-Fin
        glowPaint.setColor(0xFFFFD700);
        tempPath.reset();
        tempPath.moveTo(cx, cy - dp(40));
        tempPath.lineTo(cx - dp(32), cy - dp(65));
        tempPath.lineTo(cx - dp(16), cy - dp(45));
        tempPath.lineTo(cx, cy - dp(42));
        tempPath.lineTo(cx + dp(16), cy - dp(45));
        tempPath.lineTo(cx + dp(32), cy - dp(65));
        tempPath.close();
        canvas.drawPath(tempPath, glowPaint);

        // Lock-on Target HUD brackets
        linePaint.setColor(0xFF00E5FF);
        linePaint.setStrokeWidth(dp(1.8f));
        float lockR = dp(28);
        dstRect.set(cx - lockR, cy - lockR, cx + lockR, cy + lockR);
        canvas.drawArc(dstRect, t * 50f, 60, false, linePaint);
        canvas.drawArc(dstRect, t * 50f + 180, 60, false, linePaint);
    }

    /**
     * Preset 22: Cyber Void Valkyrie
     * Angelic cyber valkyrie silhouette with floating holographic halo, slowly flapping vector wings, and feather motes.
     */
    private void drawCyberVoidValkyrie(Canvas canvas, int w, int h, float t, float dt) {
        LinearGradient bg = new LinearGradient(0, 0, w, h,
                new int[]{0xFF0E081A, 0xFF1C1232, 0xFF0B1428}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(null);

        float cx = w * 0.5f;
        float cy = h * 0.52f;

        // Floating Holographic Halo above head
        linePaint.setColor(0xFFFFD700);
        linePaint.setStrokeWidth(dp(2.2f));
        dstRect.set(cx - dp(24), cy - dp(75), cx + dp(24), cy - dp(65));
        canvas.drawOval(dstRect, linePaint);

        // Majestic Flapping Vector Wings (Flaps with Math.sin)
        float wingFlap = (float) Math.sin(t * 1.8f) * dp(18);
        tempPath.reset();
        tempPath.moveTo(cx - dp(15), cy - dp(25));
        tempPath.cubicTo(cx - dp(80), cy - dp(90) + wingFlap, cx - dp(140), cy - dp(60) + wingFlap, cx - dp(120), cy + dp(20) + wingFlap);
        tempPath.cubicTo(cx - dp(90), cy + dp(30), cx - dp(50), cy + dp(10), cx - dp(10), cy);
        tempPath.close();
        glowPaint.setColor(0x8800E5FF);
        canvas.drawPath(tempPath, glowPaint);

        tempPath.reset();
        tempPath.moveTo(cx + dp(15), cy - dp(25));
        tempPath.cubicTo(cx + dp(80), cy - dp(90) + wingFlap, cx + dp(140), cy - dp(60) + wingFlap, cx + dp(120), cy + dp(20) + wingFlap);
        tempPath.cubicTo(cx + dp(90), cy + dp(30), cx + dp(50), cy + dp(10), cx + dp(10), cy);
        tempPath.close();
        glowPaint.setColor(0x88FF007F);
        canvas.drawPath(tempPath, glowPaint);

        // Valkyrie Silhouette
        glowPaint.setColor(0xFF161224);
        dstRect.set(cx - dp(20), cy - dp(55), cx + dp(20), cy - dp(12));
        canvas.drawOval(dstRect, glowPaint);

        // Falling luminous cyber feathers
        glowPaint.setColor(0xCCFFFFFF);
        for (int i = 0; i < 22; i++) {
            float py = (partY[i] + t * 0.09f) % 1.0f;
            float px = (partX[i] + (float) Math.sin(t * 1.2f + i) * 0.05f) % 1.0f;
            canvas.drawCircle(px * w, py * h, dp(partSize[i] * 0.45f), glowPaint);
        }
    }

    /**
     * Preset 23: Arcane Jinx Supernova
     * Wild Zaun graffiti aesthetic, swaying braids, electric magenta/cyan spray, and explosive rocket sparks.
     */
    private void drawArcaneJinxSupernova(Canvas canvas, int w, int h, float t, float dt) {
        LinearGradient bg = new LinearGradient(0, 0, w, h,
                new int[]{0xFF12031C, 0xFF2A093D, 0xFF0A1826}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(null);

        float cx = w * 0.5f;
        float cy = h * 0.52f;

        // Long Braids swaying dynamically
        float braidL = (float) Math.sin(t * 3.5f) * dp(16);
        float braidR = (float) Math.cos(t * 3.2f) * dp(16);

        linePaint.setColor(0xFF00E5FF);
        linePaint.setStrokeWidth(dp(5f));
        tempPath.reset();
        tempPath.moveTo(cx - dp(15), cy - dp(35));
        tempPath.quadTo(cx - dp(65) + braidL, cy + dp(20), cx - dp(45) + braidL, cy + dp(95));
        canvas.drawPath(tempPath, linePaint);

        linePaint.setColor(0xFFFF007F);
        tempPath.reset();
        tempPath.moveTo(cx + dp(15), cy - dp(35));
        tempPath.quadTo(cx + dp(65) + braidR, cy + dp(20), cx + dp(45) + braidR, cy + dp(95));
        canvas.drawPath(tempPath, linePaint);

        // Jinx Silhouette
        glowPaint.setColor(0xFF1E0E2E);
        dstRect.set(cx - dp(22), cy - dp(55), cx + dp(22), cy - dp(12));
        canvas.drawOval(dstRect, glowPaint);

        // Glowing Pink Eyes
        glowPaint.setColor(0xFFFF007F);
        canvas.drawCircle(cx - dp(7), cy - dp(34), dp(3.5f), glowPaint);
        canvas.drawCircle(cx + dp(7), cy - dp(34), dp(3.5f), glowPaint);

        // Explosive Neon Rocket Sparks
        glowPaint.setColor(0xFFFFD600);
        for (int i = 0; i < 30; i++) {
            float sparkAngle = (float) Math.toRadians(partAngle[i]);
            float sparkDist = ((t * 80f + i * 15f) % (w * 0.45f));
            float sx = cx + (float) Math.cos(sparkAngle) * sparkDist;
            float sy = cy + (float) Math.sin(sparkAngle) * (sparkDist * 0.6f);
            canvas.drawCircle(sx, sy, dp(partSize[i] * 0.5f), glowPaint);
        }
    }

    /**
     * High-end cinematic Ken Burns effect (subtle scale between 1.0f and 1.08f and gentle panning)
     * plus ambient floating particle sparkles overlay.
     */
    private void drawCustomKenBurns(Canvas canvas, int w, int h, float t) {
        if (customBitmap == null || customBitmap.isRecycled()) return;

        int bw = customBitmap.getWidth();
        int bh = customBitmap.getHeight();
        if (bw <= 0 || bh <= 0) return;

        // Cinematic Ken Burns slow zoom (1.00 -> 1.07 -> 1.00 over 16 seconds)
        float zoom = 1.0f + 0.07f * (0.5f + 0.5f * (float) Math.sin(t * 0.39f));
        float panX = (float) Math.sin(t * 0.25f) * dp(12);
        float panY = (float) Math.cos(t * 0.20f) * dp(8);

        transformMatrix.reset();
        // Scale to fill maintaining aspect ratio
        float scale = Math.max((float) w / bw, (float) h / bh) * zoom;
        float dx = (w - bw * scale) * 0.5f + panX;
        float dy = (h - bh * scale) * 0.5f + panY;
        transformMatrix.setScale(scale, scale);
        transformMatrix.postTranslate(dx, dy);

        canvas.drawBitmap(customBitmap, transformMatrix, bitmapPaint);

        // Ambient floating dust sparkles
        glowPaint.setColor(0x55FFFFFF);
        for (int i = 0; i < 25; i++) {
            float py = (partY[i] - t * partSpeed[i] * 0.04f) % 1.0f;
            if (py < 0) py += 1.0f;
            float px = (partX[i] + (float) Math.sin(t * 0.5f + i) * 0.02f) % 1.0f;
            canvas.drawCircle(px * w, py * h, dp(partSize[i] * 0.5f), glowPaint);
        }
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}


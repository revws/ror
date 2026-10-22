package com.revror.kernelmanager.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class CyberConsoleBackgroundView extends View {

    public static final int MODE_MECHA_DECK = 1;     // Photo 1: REDMAGIC Mecha Deck
    public static final int MODE_LEGION_SPACE = 2;   // Photo 2: Lenovo Legion Space Turbine
    public static final int MODE_BLACK_SHARK = 3;    // Photo 3: Black Shark Golden Horizon
    public static final int MODE_GAME_TURBO = 4;     // Photo 4: Xiaomi Game Turbo Cockpit
    public static final int MODE_ROG_ARMOURY = 5;    // ASUS ROG Armoury Crate Cyber Grid

    private int mode = MODE_MECHA_DECK;
    private float phase = 0f;
    private ValueAnimator animator;

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rectF = new RectF();

    public CyberConsoleBackgroundView(Context context) {
        super(context);
        init();
    }

    public CyberConsoleBackgroundView(Context context, int mode) {
        super(context);
        this.mode = mode;
        init();
    }

    public CyberConsoleBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setClickable(false);
        setFocusable(false);
        linePaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStyle(Paint.Style.FILL);

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(4000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            phase = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    public void setMode(int mode) {
        this.mode = mode;
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) animator.cancel();
    }

    private float dp(float val) {
        return val * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        switch (mode) {
            case MODE_MECHA_DECK:
                drawMechaDeck(canvas, w, h);
                break;
            case MODE_LEGION_SPACE:
                drawLegionSpace(canvas, w, h);
                break;
            case MODE_BLACK_SHARK:
                drawBlackShark(canvas, w, h);
                break;
            case MODE_GAME_TURBO:
                drawGameTurbo(canvas, w, h);
                break;
            case MODE_ROG_ARMOURY:
                drawRogArmoury(canvas, w, h);
                break;
            default:
                drawMechaDeck(canvas, w, h);
                break;
        }
    }

    // Photo 1: REDMAGIC Mecha Deck Background
    private void drawMechaDeck(Canvas canvas, int w, int h) {
        // Deep metallic cyber-blue gradient
        LinearGradient bg = new LinearGradient(0, 0, 0, h,
                new int[]{0x330B111D, 0x33080D18, 0x3305070E}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(null);

        // Top mecha bracket wings
        linePaint.setColor(0x3300E5FF);
        linePaint.setStrokeWidth(dp(1.5f));
        path.reset();
        path.moveTo(w * 0.15f, dp(4));
        path.lineTo(w * 0.28f, dp(24));
        path.lineTo(w * 0.72f, dp(24));
        path.lineTo(w * 0.85f, dp(4));
        canvas.drawPath(path, linePaint);

        // Angled side telemetry wings
        path.reset();
        path.moveTo(dp(16), h * 0.35f);
        path.lineTo(w * 0.18f, h * 0.25f);
        path.lineTo(w * 0.22f, h * 0.45f);
        canvas.drawPath(path, linePaint);

        path.reset();
        path.moveTo(w - dp(16), h * 0.35f);
        path.lineTo(w - w * 0.18f, h * 0.25f);
        path.lineTo(w - w * 0.22f, h * 0.45f);
        canvas.drawPath(path, linePaint);

        // Bottom cyber trails angling inward to center button
        path.reset();
        path.moveTo(dp(20), h - dp(45));
        path.lineTo(w * 0.28f, h - dp(45));
        path.lineTo(w * 0.36f, h - dp(12));
        canvas.drawPath(path, linePaint);

        path.reset();
        path.moveTo(w - dp(20), h - dp(45));
        path.lineTo(w - w * 0.28f, h - dp(45));
        path.lineTo(w - w * 0.36f, h - dp(12));
        canvas.drawPath(path, linePaint);

        // Glowing Cyan Hexagonal Circuits in bottom-left and bottom-right corners
        drawHexPads(canvas, dp(70), h - dp(50), 3, true);
        drawHexPads(canvas, w - dp(70), h - dp(50), 3, false);

        // Top mecha laser sweep pulse
        float laserX = w * (0.28f + 0.44f * phase);
        glowPaint.setColor(0x8800E5FF);
        canvas.drawCircle(laserX, dp(24), dp(3.5f), glowPaint);
        linePaint.setColor(0xCC00E5FF);
        linePaint.setStrokeWidth(dp(1f));
        canvas.drawLine(laserX - dp(18), dp(24), laserX + dp(18), dp(24), linePaint);

        drawChronographicTelemetryRing(canvas, w * 0.12f, h * 0.40f, dp(24), 0xFF00E5FF);
        drawChronographicTelemetryRing(canvas, w * 0.88f, h * 0.40f, dp(24), 0xFF00E5FF);
    }

    private void drawHexPads(Canvas canvas, float cx, float cy, int count, boolean isLeft) {
        float r = dp(14);
        for (int i = 0; i < count; i++) {
            float ox = cx + (isLeft ? i * dp(26) : -i * dp(26));
            float oy = cy + (i % 2 == 0 ? 0 : dp(16));
            
            // Traveling data pulse across hexagons
            float padPhase = (phase * 3f + (isLeft ? i : (count - 1 - i))) % 3f;
            boolean isActive = padPhase < 1.0f;
            int glowAlpha = isActive ? 0x66 : 0x22;
            int strokeAlpha = isActive ? 0xFF : 0x77;
            
            glowPaint.setStyle(Paint.Style.FILL);
            glowPaint.setColor((glowAlpha << 24) | 0x00E5FF);
            linePaint.setColor((strokeAlpha << 24) | 0x00E5FF);
            linePaint.setStrokeWidth(dp(isActive ? 1.8f : 1.2f));

            path.reset();
            for (int k = 0; k < 6; k++) {
                double angle = Math.toRadians(60 * k - 30);
                float x = ox + (float) (r * Math.cos(angle));
                float y = oy + (float) (r * Math.sin(angle));
                if (k == 0) path.moveTo(x, y);
                else path.lineTo(x, y);
            }
            path.close();
            canvas.drawPath(path, glowPaint);
            canvas.drawPath(path, linePaint);
        }

    }

    // Photo 2: Lenovo Legion Space Turbine Reactor Background
    private void drawLegionSpace(Canvas canvas, int w, int h) {
        // Deep space midnight background
        LinearGradient bg = new LinearGradient(0, 0, 0, h,
                new int[]{0x33070B14, 0x3304070D, 0x33080C16}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(null);

        float cx = w * 0.5f;
        float cy = h * 0.50f;
        float radius = Math.min(w, h) * 0.32f;

        // Horizontal mechanical rails extending left & right
        linePaint.setColor(0x66162338);
        linePaint.setStrokeWidth(dp(18f));
        canvas.drawLine(0, cy, cx - radius * 0.7f, cy, linePaint);
        canvas.drawLine(cx + radius * 0.7f, cy, w, cy, linePaint);

        linePaint.setColor(0xFF00E5FF);
        linePaint.setStrokeWidth(dp(2.2f));
        // Cyan inward-pointing arrows on rails
        path.reset();
        path.moveTo(cx - radius * 0.72f, cy - dp(9));
        path.lineTo(cx - radius * 0.60f, cy);
        path.lineTo(cx - radius * 0.72f, cy + dp(9));
        canvas.drawPath(path, linePaint);

        path.reset();
        path.moveTo(cx + radius * 0.72f, cy - dp(9));
        path.lineTo(cx + radius * 0.60f, cy);
        path.lineTo(cx + radius * 0.72f, cy + dp(9));
        canvas.drawPath(path, linePaint);

        // Inward kinetic energy pulses traveling along the rails
        float railPulse1 = (phase * 1.5f) % 1.0f;
        float pX1 = (cx - radius * 0.75f) * (1.0f - railPulse1);
        float pX2 = (cx + radius * 0.75f) + (w - (cx + radius * 0.75f)) * railPulse1;
        glowPaint.setColor(0xAA00E5FF);
        canvas.drawCircle(pX1, cy, dp(4), glowPaint);
        canvas.drawCircle(pX2, cy, dp(4), glowPaint);

        // Turbine Outer Bezel Ring
        linePaint.setColor(0xFF141C2A);
        linePaint.setStrokeWidth(dp(16f));
        canvas.drawCircle(cx, cy, radius, linePaint);

        // Concentric tick-mark track (animated rotation)
        linePaint.setColor(0x8800E5FF);
        linePaint.setStrokeWidth(dp(2f));
        int ticks = 36;
        float rot = phase * 360f;
        for (int i = 0; i < ticks; i++) {
            if (i % 3 != 0) continue;
            double a = Math.toRadians((i * (360f / ticks)) + rot);
            float x1 = cx + (float) Math.cos(a) * (radius - dp(6));
            float y1 = cy + (float) Math.sin(a) * (radius - dp(6));
            float x2 = cx + (float) Math.cos(a) * (radius + dp(6));
            float y2 = cy + (float) Math.sin(a) * (radius + dp(6));
            canvas.drawLine(x1, y1, x2, y2, linePaint);
        }

        // Inner glowing cyan turbine chamber (animated breathing pulse)
        float chamberPulse = (float) (0.35f + 0.25f * Math.sin(phase * 2 * Math.PI));
        int glowColor1 = Color.argb((int) (chamberPulse * 255), 0, 229, 255);
        int glowColor2 = Color.argb((int) (chamberPulse * 120), 0, 176, 255);
        RadialGradient chamber = new RadialGradient(cx, cy, radius * 0.82f,
                new int[]{glowColor1, glowColor2, 0x00000000}, null, Shader.TileMode.CLAMP);
        glowPaint.setShader(chamber);
        canvas.drawCircle(cx, cy, radius * 0.82f, glowPaint);
        glowPaint.setShader(null);

        // 3-blade turbine rotor (SMOOTH 60 FPS CONTINUOUS ROTATION)
        float bladeR = radius * 0.65f;
        linePaint.setColor(0xFF00E5FF);
        linePaint.setStrokeWidth(dp(4.2f));
        float spinAngle = phase * 360f;
        for (int b = 0; b < 3; b++) {
            double angle = Math.toRadians(b * 120 + spinAngle);
            float bx = cx + (float) Math.cos(angle) * bladeR;
            float by = cy + (float) Math.sin(angle) * bladeR;
            canvas.drawLine(cx, cy, bx, by, linePaint);

            // Aerodynamic blade fin
            double subAngle = Math.toRadians(b * 120 + spinAngle + 18);
            float sx = cx + (float) Math.cos(subAngle) * (bladeR * 0.68f);
            float sy = cy + (float) Math.sin(subAngle) * (bladeR * 0.68f);
            linePaint.setStrokeWidth(dp(2.2f));
            canvas.drawLine(bx, by, sx, sy, linePaint);
            linePaint.setStrokeWidth(dp(4.2f));
        }

        // Central metallic hub with spinning inner dial
        glowPaint.setColor(0xFF0F1522);
        canvas.drawCircle(cx, cy, dp(24), glowPaint);
        linePaint.setColor(0xFF00E5FF);
        linePaint.setStrokeWidth(dp(2.5f));
        canvas.drawCircle(cx, cy, dp(24), linePaint);
        linePaint.setColor(0xFFFF1744);
        linePaint.setStrokeWidth(dp(1.8f));
        double hubA = Math.toRadians(-rot * 2f);
        canvas.drawLine(cx, cy, cx + (float) Math.cos(hubA) * dp(16), cy + (float) Math.sin(hubA) * dp(16), linePaint);

        // Right side cyan vertical edge drawer handle (as seen in photo)
        linePaint.setColor(0xFF00E5FF);
        linePaint.setStrokeWidth(dp(3f));
        canvas.drawLine(w - dp(4), h * 0.22f, w - dp(4), h * 0.35f, linePaint);

        drawChronographicTelemetryRing(canvas, w * 0.12f, h * 0.50f, dp(26), 0xFF00E5FF);
        drawChronographicTelemetryRing(canvas, w * 0.88f, h * 0.50f, dp(26), 0xFF00E5FF);
    }

    // Photo 3: Black Shark Ambient Void & Glowing Golden Horizon Background
    private void drawBlackShark(Canvas canvas, int w, int h) {
        // Pure pitch-black top
        canvas.drawColor(0x22000000);

        // Glowing Golden / Amber Horizon Planetary Aura sweeping across bottom (Breathing pulse)
        float arcCenterY = h * 1.55f;
        float arcRadius = h * 1.05f;

        float horizonBreath = (float) (0.85f + 0.15f * Math.sin(phase * 2 * Math.PI));
        int goldA1 = Color.argb((int) (0x88 * horizonBreath), 0xFF, 0xB3, 0x00);
        int goldA2 = Color.argb((int) (0x3C * horizonBreath), 0xFF, 0x8F, 0x00);

        RadialGradient goldGlow = new RadialGradient(w * 0.5f, h * 0.85f, w * 0.58f * horizonBreath,
                new int[]{goldA1, goldA2, 0x00000000}, null, Shader.TileMode.CLAMP);
        glowPaint.setShader(goldGlow);
        canvas.drawCircle(w * 0.5f, h * 0.85f, w * 0.58f * horizonBreath, glowPaint);
        glowPaint.setShader(null);

        // Curved horizon arc line
        rectF.set(w * 0.5f - arcRadius * 1.4f, arcCenterY - arcRadius,
                w * 0.5f + arcRadius * 1.4f, arcCenterY + arcRadius);
        linePaint.setColor(0xFFFFD54F);
        linePaint.setStrokeWidth(dp(2.8f));
        canvas.drawArc(rectF, 195, 150, false, linePaint);

        // Ambient gold stardust particles (UPWARD DRIFT & TWINKLE)
        for (int i = 0; i < 22; i++) {
            float sx = (w * (0.08f + 0.84f * ((i * 37) % 100) / 100f));
            float driftY = (phase + (i * 0.045f)) % 1.0f;
            float sy = h * (0.95f - 0.38f * driftY);
            int pAlpha = (int) (90 + 130 * Math.abs(Math.sin(phase * 4 * Math.PI + i * 0.7f)));
            glowPaint.setColor((pAlpha << 24) | 0xFFD54F);
            canvas.drawCircle(sx, sy, dp(1.8f), glowPaint);
        }

        drawChronographicTelemetryRing(canvas, w * 0.12f, h * 0.45f, dp(26), 0xFFFFD600);
        drawChronographicTelemetryRing(canvas, w * 0.88f, h * 0.45f, dp(26), 0xFFFFD600);
    }

    // Photo 4: Xiaomi Game Turbo / POCO Sci-Fi Cockpit Background
    private void drawGameTurbo(Canvas canvas, int w, int h) {
        // Sci-Fi metallic chamber
        LinearGradient bg = new LinearGradient(0, 0, 0, h,
                new int[]{0x33121B2E, 0x330E1524, 0x3318233C}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(null);

        // Left wall angled cyan LED tubes with traveling kinetic pulses
        linePaint.setColor(0xFF00E5FF);
        linePaint.setStrokeWidth(dp(3f));

        // Tube 1 (top left)
        path.reset();
        path.moveTo(dp(12), h * 0.16f);
        path.lineTo(w * 0.14f, h * 0.22f);
        canvas.drawPath(path, linePaint);

        // Tube 2 (mid left)
        path.reset();
        path.moveTo(dp(14), h * 0.32f);
        path.lineTo(w * 0.18f, h * 0.38f);
        canvas.drawPath(path, linePaint);

        // Tube 3 (bottom left)
        path.reset();
        path.moveTo(dp(18), h * 0.72f);
        path.lineTo(w * 0.22f, h * 0.78f);
        path.lineTo(w * 0.28f, h * 0.78f);
        canvas.drawPath(path, linePaint);

        // Right wall angled cyan LED tubes
        // Tube 1 (top right)
        path.reset();
        path.moveTo(w - dp(12), h * 0.16f);
        path.lineTo(w - w * 0.14f, h * 0.22f);
        canvas.drawPath(path, linePaint);

        // Tube 2 (mid right)
        path.reset();
        path.moveTo(w - dp(14), h * 0.32f);
        path.lineTo(w - w * 0.18f, h * 0.38f);
        canvas.drawPath(path, linePaint);

        // Tube 3 (bottom right)
        path.reset();
        path.moveTo(w - dp(18), h * 0.72f);
        path.lineTo(w - w * 0.22f, h * 0.78f);
        path.lineTo(w - w * 0.28f, h * 0.78f);
        canvas.drawPath(path, linePaint);

        // Kinetic traveling pulses along wall tubes
        float tPulse = (phase * 2f) % 1.0f;
        float pulseLx = dp(12) + (w * 0.14f - dp(12)) * tPulse;
        float pulseLy = h * 0.16f + (h * 0.22f - h * 0.16f) * tPulse;
        glowPaint.setColor(0xCC00E5FF);
        canvas.drawCircle(pulseLx, pulseLy, dp(3.5f), glowPaint);

        float pulseRx = (w - dp(12)) + ((w - w * 0.14f) - (w - dp(12))) * tPulse;
        float pulseRy = h * 0.16f + (h * 0.22f - h * 0.16f) * tPulse;
        canvas.drawCircle(pulseRx, pulseRy, dp(3.5f), glowPaint);

        // Center Cockpit Recessed Display Chamber Outline (with neon breathing alpha)
        int chamberAlpha = (int) (0x33 + 0x24 * Math.sin(phase * 2 * Math.PI));
        linePaint.setColor((chamberAlpha << 24) | 0x00E5FF);
        linePaint.setStrokeWidth(dp(2.2f));
        rectF.set(w * 0.22f, h * 0.18f, w * 0.78f, h * 0.82f);
        canvas.drawRoundRect(rectF, dp(16), dp(16), linePaint);

        drawChronographicTelemetryRing(canvas, w * 0.11f, h * 0.52f, dp(24), 0xFF00E5FF);
        drawChronographicTelemetryRing(canvas, w * 0.89f, h * 0.52f, dp(24), 0xFF00E5FF);
    }

    // ASUS ROG Armoury Crate Cyber Grid Background
    private void drawRogArmoury(Canvas canvas, int w, int h) {
        // Deep obsidian titanium background with subtle crimson vignette
        LinearGradient bg = new LinearGradient(0, 0, w, h,
                new int[]{0x330E0B12, 0x3308060A, 0x2214080D}, null, Shader.TileMode.CLAMP);
        bgPaint.setShader(bg);
        canvas.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(null);

        // Breathing ROG X-Mode Central Cyber Glow
        float glowPulse = (float) (0.5f + 0.5f * Math.sin(phase * 2 * Math.PI));
        int glowColor = Color.argb((int) (28 + 26 * glowPulse), 255, 17, 34);
        RadialGradient xModeAura = new RadialGradient(w * 0.5f, h * 0.45f, Math.max(w, h) * 0.45f,
                glowColor, 0x00000000, Shader.TileMode.CLAMP);
        glowPaint.setShader(xModeAura);
        canvas.drawRect(0, 0, w, h, glowPaint);
        glowPaint.setShader(null);

        // Angled ROG Slash & Cyber Accent Lines
        linePaint.setColor(0x28FF1744);
        linePaint.setStrokeWidth(dp(1.2f));
        float slashSpacing = dp(36);
        float offset = (phase * slashSpacing);
        for (float x = -h; x < w + h; x += slashSpacing) {
            canvas.drawLine(x + offset, 0, x + offset + h * 0.6f, h, linePaint);
        }

        // Top & bottom mechanical neon brackets
        linePaint.setColor(0x55FF1744);
        linePaint.setStrokeWidth(dp(2.0f));
        path.reset();
        path.moveTo(dp(16), dp(12));
        path.lineTo(w * 0.28f, dp(12));
        path.lineTo(w * 0.32f, dp(24));
        canvas.drawPath(path, linePaint);

        path.reset();
        path.moveTo(w - dp(16), dp(12));
        path.lineTo(w - w * 0.28f, dp(12));
        path.lineTo(w - w * 0.32f, dp(24));
        canvas.drawPath(path, linePaint);

        // Dynamic kinetic energy node traveling along top bracket
        float t = phase;
        float nodeX = dp(16) + (w * 0.28f - dp(16)) * t;
        glowPaint.setColor(0xFFFF3355);
        canvas.drawCircle(nodeX, dp(12), dp(3.5f), glowPaint);

        drawChronographicTelemetryRing(canvas, w * 0.12f, h * 0.45f, dp(26), 0xFFFF1744);
        drawChronographicTelemetryRing(canvas, w * 0.88f, h * 0.45f, dp(26), 0xFFFF1744);
    }

    // Chronographic live telemetry sweep indicator
    private void drawChronographicTelemetryRing(Canvas canvas, float cx, float cy, float radius, int tintColor) {
        // Outer tick marks (every 15 degrees)
        linePaint.setColor((0x44 << 24) | (tintColor & 0x00FFFFFF));
        linePaint.setStrokeWidth(dp(1.2f));
        int totalTicks = 24;
        for (int i = 0; i < totalTicks; i++) {
            double angleRad = Math.toRadians(i * (360.0 / totalTicks));
            float innerR = (i % 6 == 0) ? radius - dp(7) : radius - dp(4);
            float startX = (float) (cx + Math.cos(angleRad) * innerR);
            float startY = (float) (cy + Math.sin(angleRad) * innerR);
            float endX = (float) (cx + Math.cos(angleRad) * radius);
            float endY = (float) (cy + Math.sin(angleRad) * radius);
            canvas.drawLine(startX, startY, endX, endY, linePaint);
        }

        // Live sweeping chronographic needle / radar hand
        float sweepAngle = phase * 360f;
        double rad = Math.toRadians(sweepAngle);
        linePaint.setColor((0xDD << 24) | (tintColor & 0x00FFFFFF));
        linePaint.setStrokeWidth(dp(1.8f));
        canvas.drawLine(cx, cy, (float) (cx + Math.cos(rad) * (radius - dp(4))),
                (float) (cy + Math.sin(rad) * (radius - dp(4))), linePaint);

        // Center jewel pin
        glowPaint.setColor(tintColor);
        canvas.drawCircle(cx, cy, dp(3f), glowPaint);

        // Chronographic vernier arc
        rectF.set(cx - radius + dp(2), cy - radius + dp(2), cx + radius - dp(2), cy + radius - dp(2));
        linePaint.setColor((0x88 << 24) | (tintColor & 0x00FFFFFF));
        linePaint.setStrokeWidth(dp(1.5f));
        canvas.drawArc(rectF, sweepAngle - 45f, 45f, false, linePaint);
    }
}


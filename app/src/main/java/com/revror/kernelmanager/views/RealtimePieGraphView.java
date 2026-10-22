/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

/**
 * High-tech Neon Pie / Donut Graph View for REVROR System Telemetry.
 * Renders anti-aliased glowing circular gauge arcs with customizable neon gradients,
 * track rings, percentage indicators, and center value readouts.
 */
public class RealtimePieGraphView extends View {

    private Paint trackPaint;
    private Paint progressPaint;
    private Paint glowPaint;
    private Paint textPaint;
    private Paint subtextPaint;
    private RectF arcBounds;

    private float progress = 0f; // 0.0 to 100.0
    private float animatedProgress = 0f;
    private float maxProgress = 100f;

    private int startColor = 0xFF00F5D4;
    private int endColor = 0xFF00B4D8;
    private int trackColor = 0x2424355A;
    private float strokeWidthDp = 7.5f;

    private boolean showCenterText = false;
    private String centerText = "";
    private String centerSubtext = "";
    private ValueAnimator animator;

    public RealtimePieGraphView(Context context) {
        super(context);
        init();
    }

    public RealtimePieGraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RealtimePieGraphView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        float stroke = strokeWidthDp * density;

        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(stroke);
        trackPaint.setColor(trackColor);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(stroke);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(stroke * 1.5f);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(14f * density);

        subtextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subtextPaint.setColor(0xFF9B9FB8);
        subtextPaint.setTextAlign(Paint.Align.CENTER);
        subtextPaint.setTextSize(9f * density);

        arcBounds = new RectF();
    }

    public void setStrokeWidthDp(float dp) {
        this.strokeWidthDp = dp;
        float stroke = dp * getResources().getDisplayMetrics().density;
        trackPaint.setStrokeWidth(stroke);
        progressPaint.setStrokeWidth(stroke);
        glowPaint.setStrokeWidth(stroke * 1.4f);
        invalidate();
    }

    public void setColors(int start, int end) {
        this.startColor = start;
        this.endColor = end;
        invalidate();
    }

    public void setTrackColor(int color) {
        this.trackColor = color;
        trackPaint.setColor(color);
        invalidate();
    }

    public void setShowCenterText(boolean show) {
        this.showCenterText = show;
        invalidate();
    }

    public void setCenterText(String text, String subtext) {
        this.centerText = text != null ? text : "";
        this.centerSubtext = subtext != null ? subtext : "";
        invalidate();
    }

    public void setMaxProgress(float max) {
        this.maxProgress = Math.max(1f, max);
        invalidate();
    }

    public void setProgress(float val) {
        this.progress = Math.max(0f, Math.min(val, maxProgress));
        this.animatedProgress = this.progress;
        postInvalidate();
    }

    public void setProgressSmooth(float target) {
        final float clamped = Math.max(0f, Math.min(target, maxProgress));
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
        this.progress = clamped;
        animator = ValueAnimator.ofFloat(animatedProgress, clamped);
        animator.setDuration(450);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            animatedProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                animatedProgress = clamped;
                invalidate();
            }
        });
        animator.start();
        // Fallback for devices/emulators with disabled animator scale
        if (Build.VERSION.SDK_INT >= 33 && ValueAnimator.getDurationScale() == 0f) {
            animatedProgress = clamped;
            invalidate();
        }
    }

    public float getProgress() {
        return progress;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float stroke = progressPaint.getStrokeWidth();
        float pad = stroke / 2f + 4f * getResources().getDisplayMetrics().density;

        // Keep square aspect ratio for circle
        float size = Math.min(w, h);
        float left = (w - size) / 2f + pad;
        float top = (h - size) / 2f + pad;
        float right = (w + size) / 2f - pad;
        float bottom = (h + size) / 2f - pad;

        arcBounds.set(left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (arcBounds.width() <= 0 || arcBounds.height() <= 0) return;

        float cx = arcBounds.centerX();
        float cy = arcBounds.centerY();

        // 1. Draw Full Background Track Ring (Pie background)
        canvas.drawArc(arcBounds, 0, 360, false, trackPaint);

        // 2. Calculate sweep angle
        float sweepAngle = (animatedProgress / maxProgress) * 360f;
        if (sweepAngle < 2.5f && animatedProgress > 0) sweepAngle = 2.5f;

        if (sweepAngle > 0.5f) {
            // 3. Draw Neon Glowing Shadow Arc
            int glowColor = (0x44FFFFFF & startColor) | 0x22000000;
            glowPaint.setColor(glowColor);
            canvas.drawArc(arcBounds, -90f, sweepAngle, false, glowPaint);

            // 4. Draw Active Arc with Gradient
            if (startColor == endColor) {
                progressPaint.setShader(null);
                progressPaint.setColor(startColor);
            } else {
                android.graphics.Matrix matrix = new android.graphics.Matrix();
                matrix.setRotate(-90f, cx, cy);
                SweepGradient gradient = new SweepGradient(cx, cy, new int[]{startColor, endColor}, new float[]{0f, 1f});
                gradient.setLocalMatrix(matrix);
                progressPaint.setShader(gradient);
            }
            canvas.drawArc(arcBounds, -90f, sweepAngle, false, progressPaint);
        }

        // 5. Center Text (Optional)
        if (showCenterText) {
            float textY = cy;
            if (!centerSubtext.isEmpty()) {
                textY = cy - 2f * getResources().getDisplayMetrics().density;
                canvas.drawText(centerText, cx, textY, textPaint);
                canvas.drawText(centerSubtext, cx, textY + 14f * getResources().getDisplayMetrics().density, subtextPaint);
            } else {
                // Center vertically
                Paint.FontMetrics fm = textPaint.getFontMetrics();
                textY = cy - (fm.ascent + fm.descent) / 2f;
                canvas.drawText(centerText, cx, textY, textPaint);
            }
        }
    }
}


/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.LinkedList;

/**
 * Lightweight real-time sparkline graph view with smooth bezier curves
 * and translucent neon gradient fills for CPU, RAM, Battery, Storage, and Temperature.
 */
public class RealtimeSparklineView extends View {
    private static final int MAX_POINTS = 20;
    private final LinkedList<Float> points = new LinkedList<>();

    private Paint linePaint;
    private Paint fillPaint;
    private Paint gridPaint;
    private Path linePath;
    private Path fillPath;

    private int lineColor = 0xFF00E5FF;
    private int fillStartColor = 0x5500E5FF;
    private int fillEndColor = 0x0500E5FF;
    private float minVal = 0f;
    private float maxVal = 100f;

    public RealtimeSparklineView(Context context) {
        super(context);
        init();
    }

    public RealtimeSparklineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RealtimeSparklineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(2));
        linePaint.setPathEffect(new CornerPathEffect(dp(6)));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setColor(lineColor);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dp(1));
        gridPaint.setColor(0x18FFFFFF);

        linePath = new Path();
        fillPath = new Path();

        // Seed initial zero/neutral baseline points
        for (int i = 0; i < MAX_POINTS; i++) {
            points.add(0f);
        }
    }

    public void setLineColor(int color) {
        this.lineColor = color;
        this.fillStartColor = (color & 0x00FFFFFF) | 0x55000000;
        this.fillEndColor = (color & 0x00FFFFFF) | 0x02000000;
        if (linePaint != null) linePaint.setColor(color);
        invalidate();
    }

    public void setRange(float min, float max) {
        this.minVal = min;
        this.maxVal = Math.max(min + 1f, max);
        invalidate();
    }

    public void addPoint(float value) {
        if (points.size() >= MAX_POINTS) {
            points.removeFirst();
        }
        points.add(value);
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0 || points.size() < 2) return;

        float padTop = dp(4);
        float padBottom = dp(4);
        float usableH = h - padTop - padBottom;

        // Subtle baseline and mid-grid lines
        canvas.drawLine(0, h - padBottom, w, h - padBottom, gridPaint);
        canvas.drawLine(0, padTop + usableH * 0.5f, w, padTop + usableH * 0.5f, gridPaint);

        linePath.reset();
        fillPath.reset();

        float stepX = (float) w / (MAX_POINTS - 1);
        float firstY = 0f;

        for (int i = 0; i < points.size(); i++) {
            float val = points.get(i);
            float clamped = Math.max(minVal, Math.min(maxVal, val));
            float normalized = (clamped - minVal) / (maxVal - minVal);
            float x = i * stepX;
            float y = padTop + (1.0f - normalized) * usableH;

            if (i == 0) {
                linePath.moveTo(x, y);
                fillPath.moveTo(x, h);
                fillPath.lineTo(x, y);
                firstY = y;
            } else {
                float prevX = (i - 1) * stepX;
                float prevVal = points.get(i - 1);
                float prevNorm = (Math.max(minVal, Math.min(maxVal, prevVal)) - minVal) / (maxVal - minVal);
                float prevY = padTop + (1.0f - prevNorm) * usableH;

                float cx = (prevX + x) / 2f;
                linePath.cubicTo(cx, prevY, cx, y, x, y);
                fillPath.cubicTo(cx, prevY, cx, y, x, y);
            }

            if (i == points.size() - 1) {
                fillPath.lineTo(x, h);
                fillPath.close();
            }
        }

        // Draw translucent gradient fill
        fillPaint.setShader(new LinearGradient(0, padTop, 0, h, fillStartColor, fillEndColor, Shader.TileMode.CLAMP));
        canvas.drawPath(fillPath, fillPaint);

        // Draw glowing neon sparkline
        canvas.drawPath(linePath, linePaint);

        // Draw glowing point at the newest tip
        float lastVal = points.getLast();
        float lastNorm = (Math.max(minVal, Math.min(maxVal, lastVal)) - minVal) / (maxVal - minVal);
        float tipX = (points.size() - 1) * stepX;
        float tipY = padTop + (1.0f - lastNorm) * usableH;

        Paint tipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tipPaint.setStyle(Paint.Style.FILL);
        tipPaint.setColor(lineColor);
        canvas.drawCircle(tipX - dp(2), tipY, dp(3), tipPaint);

        tipPaint.setStyle(Paint.Style.STROKE);
        tipPaint.setStrokeWidth(dp(1.5f));
        tipPaint.setColor(0xFFFFFFFF);
        canvas.drawCircle(tipX - dp(2), tipY, dp(4), tipPaint);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}


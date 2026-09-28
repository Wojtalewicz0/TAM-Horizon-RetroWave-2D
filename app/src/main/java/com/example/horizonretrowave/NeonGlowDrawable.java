package com.example.horizonretrowave;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

public class NeonGlowDrawable extends Drawable {

    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private int glowColor = 0xFFFF2DBB;
    private float glowRadius = 102f;
    private boolean shadowEnabled = true;

    public NeonGlowDrawable() {
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(4f);
    }

    public void setGlowColor(int color) {
        if (glowColor != color) {
            glowColor = color;
            invalidateSelf();
        }
    }

    public void setGlowRadius(float radius) {
        float safeRadius = Math.max(0f, radius);
        if (glowRadius != safeRadius) {
            glowRadius = safeRadius;
            invalidateSelf();
        }
    }

    public void setShadowEnabled(boolean enabled) {
        if (shadowEnabled != enabled) {
            shadowEnabled = enabled;
            invalidateSelf();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        bounds.set(getBounds());
        bounds.inset(5f, 5f);

        glowPaint.setColor(glowColor);
        glowPaint.setAlpha(Math.min(255, Math.round(Color.alpha(glowColor) * 0.9f)));
        if (shadowEnabled) {
            glowPaint.setShadowLayer(glowRadius, 0f, 0f, glowColor);
        } else {
            glowPaint.clearShadowLayer();
        }
        canvas.drawRoundRect(bounds, 12f, 12f, glowPaint);
        glowPaint.clearShadowLayer();
    }

    @Override
    public void setAlpha(int alpha) {
        glowPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        glowPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}

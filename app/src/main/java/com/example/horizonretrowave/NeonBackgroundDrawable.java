package com.example.horizonretrowave;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Rounded panel with a real blurred neon glow instead of a solid outer border.
 */
public class NeonBackgroundDrawable extends Drawable {

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF panelBounds = new RectF();
    private final int fillColor;
    private final int glowColor;
    private final float cornerRadius;
    private final float glowRadius;
    private int drawableAlpha = 255;

    public NeonBackgroundDrawable(
            int fillColor,
            int glowColor,
            float cornerRadius,
            float glowRadius
    ) {
        this.fillColor = fillColor;
        this.glowColor = glowColor;
        this.cornerRadius = cornerRadius;
        this.glowRadius = glowRadius;

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f);
        borderPaint.setColor(0xFFFFFFFF);
    }

    @Override
    public void draw(Canvas canvas) {
        panelBounds.set(getBounds());
        panelBounds.inset(3f, 3f);

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(fillColor);
        fillPaint.setAlpha(Color.alpha(fillColor) * drawableAlpha / 255);
        fillPaint.setShadowLayer(glowRadius, 0f, 0f, glowColor);
        canvas.drawRoundRect(panelBounds, cornerRadius, cornerRadius, fillPaint);
        fillPaint.clearShadowLayer();

        borderPaint.setAlpha(drawableAlpha);
        canvas.drawRoundRect(panelBounds, cornerRadius, cornerRadius, borderPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        drawableAlpha = alpha;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        fillPaint.setColorFilter(colorFilter);
        borderPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}

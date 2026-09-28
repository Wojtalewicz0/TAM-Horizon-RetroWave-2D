package com.example.horizonretrowave;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/** Thick left/right arrow with a white stroke and animated neon glow. */
public class NeonArrowButtonView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int glowColor = Color.rgb(255, 45, 187);
    private float glowRadius = 36f;
    private boolean shadowEnabled = true;
    private boolean pointsRight;

    public NeonArrowButtonView(Context context) {
        super(context);
        initialize();
    }

    public NeonArrowButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public NeonArrowButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        setWillNotDraw(false);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(18f);
    }

    public void setPointsRight(boolean pointsRight) {
        if (this.pointsRight != pointsRight) {
            this.pointsRight = pointsRight;
            invalidate();
        }
    }

    public void setGlowColor(int color) {
        if (glowColor != color) {
            glowColor = color;
            invalidate();
        }
    }

    public void setGlowRadius(float radius) {
        float safeRadius = Math.max(0f, radius);
        if (glowRadius != safeRadius) {
            glowRadius = safeRadius;
            invalidate();
        }
    }

    public void setShadowEnabled(boolean enabled) {
        if (shadowEnabled != enabled) {
            shadowEnabled = enabled;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float shaftHalfLength = Math.min(48f, getWidth() * 0.28f);
        float tipX = pointsRight
                ? centerX + shaftHalfLength
                : centerX - shaftHalfLength;
        float tailX = pointsRight
                ? centerX - shaftHalfLength
                : centerX + shaftHalfLength;
        float headBaseX = pointsRight
                ? tipX - 42f
                : tipX + 42f;

        paint.setColor(Color.WHITE);
        if (shadowEnabled) {
            paint.setShadowLayer(glowRadius, 0f, 0f, glowColor);
        } else {
            paint.clearShadowLayer();
        }

        canvas.drawLine(tailX, centerY, tipX, centerY, paint);
        canvas.drawLine(tipX, centerY, headBaseX, centerY - 38f, paint);
        canvas.drawLine(tipX, centerY, headBaseX, centerY + 38f, paint);

        paint.clearShadowLayer();
    }
}

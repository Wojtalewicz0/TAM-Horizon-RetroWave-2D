package com.example.horizonretrowave;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/** Circular back button with a thick, centered arrow and animated neon glow. */
public class NeonBackButtonView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int glowColor = Color.rgb(255, 45, 187);
    private float glowRadius = 36f;
    private boolean shadowEnabled = true;

    public NeonBackButtonView(Context context) {
        super(context);
        initialize();
    }

    public NeonBackButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public NeonBackButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        setWillNotDraw(false);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
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

    /** Enables or disables the blurred neon shadow for performance mode. */
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
        float circleRadius = Math.min(getWidth(), getHeight()) / 2f - 9f;

        // The circle itself follows the same pink-violet-blue pulse as the
        // NeonGlowDrawable borders used by the rest of the menu.
        paint.setColor(glowColor);
        paint.setStrokeWidth(6f);
        if (shadowEnabled) {
            paint.setShadowLayer(glowRadius, 0f, 0f, glowColor);
        } else {
            paint.clearShadowLayer();
        }
        canvas.drawCircle(centerX, centerY, circleRadius, paint);

        // Keep the arrow white for contrast while its neon shadow follows the
        // same animated border color.
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(10f);
        float arrowTipX = centerX - 48f;
        float arrowTailX = centerX + 48f;
        float arrowHeadLength = 38f;
        float arrowHeadHeight = 32f;
        canvas.drawLine(arrowTailX, centerY, arrowTipX, centerY, paint);
        canvas.drawLine(
                arrowTipX,
                centerY,
                arrowTipX + arrowHeadLength,
                centerY - arrowHeadHeight,
                paint
        );
        canvas.drawLine(
                arrowTipX,
                centerY,
                arrowTipX + arrowHeadLength,
                centerY + arrowHeadHeight,
                paint
        );

        paint.clearShadowLayer();
    }
}

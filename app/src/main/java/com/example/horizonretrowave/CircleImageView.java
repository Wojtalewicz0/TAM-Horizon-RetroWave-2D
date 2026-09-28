package com.example.horizonretrowave;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

/** ImageView that clips its content to a circle without adding a border. */
public class CircleImageView extends AppCompatImageView {

    private final Path circlePath = new Path();
    private final RectF imageBounds = new RectF();

    public CircleImageView(Context context) {
        super(context);
    }

    public CircleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CircleImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        imageBounds.set(0f, 0f, getWidth(), getHeight());
        circlePath.reset();
        circlePath.addOval(imageBounds, Path.Direction.CW);

        int saveCount = canvas.save();
        canvas.clipPath(circlePath);
        super.onDraw(canvas);
        canvas.restoreToCount(saveCount);
    }
}

package com.example.horizonretrowave;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * Root container for the game's logical 16:9 scene.
 *
 * The logical scene is always 1920 x 1080 units. The host scales it uniformly
 * to the available window and centers it, leaving black letterbox bars when
 * the window has another aspect ratio.
 *
 * Children use scene_* attributes rather than Android screen pixels:
 *
 * app:scene_width="480"
 * app:scene_height="120"
 * app:scene_center_x="480"
 * app:scene_center_y="540"
 */
public class SceneHost extends ViewGroup {

    public static final float SCENE_WIDTH = 1920f;
    public static final float SCENE_HEIGHT = 1080f;

    private float sceneScale = 1f;
    private float sceneLeft;
    private float sceneTop;

    public SceneHost(Context context) {
        super(context);
        initialize();
    }

    public SceneHost(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public SceneHost(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        setWillNotDraw(false);
        setBackgroundColor(0xFF000000);
        setClipChildren(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = resolveSize(0, widthMeasureSpec);
        int measuredHeight = resolveSize(0, heightMeasureSpec);
        setMeasuredDimension(measuredWidth, measuredHeight);

        updateSceneTransform(measuredWidth, measuredHeight);

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }

            SceneLayoutParams params = (SceneLayoutParams) child.getLayoutParams();
            int logicalWidth = Math.max(1, Math.round(params.sceneWidth));
            int logicalHeight = Math.max(1, Math.round(params.sceneHeight));

            // The child is measured in logical scene units. Its whole visual
            // tree is then uniformly scaled in onLayout(), including text.
            child.measure(
                    MeasureSpec.makeMeasureSpec(logicalWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(logicalHeight, MeasureSpec.EXACTLY)
            );
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        updateSceneTransform(getWidth(), getHeight());

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }

            SceneLayoutParams params = (SceneLayoutParams) child.getLayoutParams();

            int logicalWidth = Math.max(1, Math.round(params.sceneWidth));
            int logicalHeight = Math.max(1, Math.round(params.sceneHeight));

            // The unscaled layout rectangle is centered on the logical scene
            // center. The visual scale is applied around that rectangle's
            // center, so the element grows in every direction without moving.
            float childLeft = sceneLeft
                    + params.centerX * sceneScale
                    - logicalWidth / 2f;
            float childTop = sceneTop
                    + params.centerY * sceneScale
                    - logicalHeight / 2f;

            child.layout(
                    Math.round(childLeft),
                    Math.round(childTop),
                    Math.round(childLeft + logicalWidth),
                    Math.round(childTop + logicalHeight)
            );
            applyChildTransform(child, params);
        }
    }

    /**
     * Sets the local scale of a direct child without changing its layout
     * rectangle or moving any sibling views.
     */
    public void setElementScale(View child, float elementScale) {
        if (child.getParent() != this || !(child.getLayoutParams() instanceof SceneLayoutParams)) {
            throw new IllegalArgumentException(
                    "The view must be a direct child of SceneHost."
            );
        }

        SceneLayoutParams params = (SceneLayoutParams) child.getLayoutParams();
        params.elementScale = Math.max(0f, elementScale);
        applyChildTransform(child, params);
    }

    public float getElementScale(View child) {
        if (child.getLayoutParams() instanceof SceneLayoutParams) {
            return ((SceneLayoutParams) child.getLayoutParams()).elementScale;
        }
        return 1f;
    }

    private void applyChildTransform(View child, SceneLayoutParams params) {
        child.setPivotX(child.getMeasuredWidth() / 2f);
        child.setPivotY(child.getMeasuredHeight() / 2f);
        child.setScaleX(sceneScale * params.elementScale);
        child.setScaleY(sceneScale * params.elementScale);
    }

    private void updateSceneTransform(int width, int height) {
        int contentWidth = Math.max(0, width - getPaddingLeft() - getPaddingRight());
        int contentHeight = Math.max(0, height - getPaddingTop() - getPaddingBottom());

        if (contentWidth == 0 || contentHeight == 0) {
            sceneScale = 1f;
            sceneLeft = getPaddingLeft();
            sceneTop = getPaddingTop();
            return;
        }

        sceneScale = Math.min(
                contentWidth / SCENE_WIDTH,
                contentHeight / SCENE_HEIGHT
        );

        sceneLeft = getPaddingLeft()
                + (contentWidth - SCENE_WIDTH * sceneScale) / 2f;
        sceneTop = getPaddingTop()
                + (contentHeight - SCENE_HEIGHT * sceneScale) / 2f;
    }

    /**
     * Converts an X coordinate from the physical View into logical scene units.
     */
    public float toSceneX(float viewX) {
        return (viewX - sceneLeft) / sceneScale;
    }

    /**
     * Converts a Y coordinate from the physical View into logical scene units.
     */
    public float toSceneY(float viewY) {
        return (viewY - sceneTop) / sceneScale;
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams params) {
        return params instanceof SceneLayoutParams;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new SceneLayoutParams(480f, 120f, SCENE_WIDTH / 2f, SCENE_HEIGHT / 2f);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new SceneLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams params) {
        return new SceneLayoutParams(params);
    }

    /** Logical layout parameters for a child inside the 1920 x 1080 scene. */
    public static class SceneLayoutParams extends ViewGroup.LayoutParams {
        public float sceneWidth = 480f;
        public float sceneHeight = 120f;
        public float centerX = SCENE_WIDTH / 2f;
        public float centerY = SCENE_HEIGHT / 2f;
        public float elementScale = 1f;

        public SceneLayoutParams(float sceneWidth, float sceneHeight,
                                 float centerX, float centerY) {
            super(WRAP_CONTENT, WRAP_CONTENT);
            this.sceneWidth = sceneWidth;
            this.sceneHeight = sceneHeight;
            this.centerX = centerX;
            this.centerY = centerY;
        }

        public SceneLayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);

            TypedArray attributes = context.obtainStyledAttributes(
                    attrs,
                    R.styleable.SceneHost_Layout
            );
            sceneWidth = attributes.getFloat(
                    R.styleable.SceneHost_Layout_scene_width,
                    sceneWidth
            );
            sceneHeight = attributes.getFloat(
                    R.styleable.SceneHost_Layout_scene_height,
                    sceneHeight
            );
            centerX = attributes.getFloat(
                    R.styleable.SceneHost_Layout_scene_center_x,
                    centerX
            );
            centerY = attributes.getFloat(
                    R.styleable.SceneHost_Layout_scene_center_y,
                    centerY
            );
            attributes.recycle();
        }

        public SceneLayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            if (source instanceof SceneLayoutParams) {
                SceneLayoutParams sceneSource = (SceneLayoutParams) source;
                sceneWidth = sceneSource.sceneWidth;
                sceneHeight = sceneSource.sceneHeight;
                centerX = sceneSource.centerX;
                centerY = sceneSource.centerY;
                elementScale = sceneSource.elementScale;
            }
        }
    }
}

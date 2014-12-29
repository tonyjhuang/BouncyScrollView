package com.tonyjhuang.bouncyscrollview;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;

/**
 * Created by tony on 12/28/14.
 */
public class BouncyScrollView extends ScrollView {

    private final String TAG = getClass().getSimpleName();

    private LinearLayout container;
    private FrameLayout viewContainer;
    private Space topSpacer;
    private Space bottomSpacer;

    private View customView;

    private ObjectAnimator viewAnimator;
    private Interpolator viewAnimationInterpolator = new OvershootInterpolator();
    private int viewAnimationDuration;

    private float relativeStartingPosition;
    private int absoluteStartingPosition;

    private boolean isDraggingOutside = false;
    private boolean isDraggingInside = false;

    private EventListener eventListener;

    public BouncyScrollView(Context context) {
        this(context, null);
    }

    public BouncyScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BouncyScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        inflate(context, R.layout.view_bouncy_scrollview, this);

        container = (LinearLayout) findViewById(R.id.container);
        viewContainer = (FrameLayout) findViewById(R.id.view_container);
        topSpacer = (Space) findViewById(R.id.top_spacer);
        bottomSpacer = (Space) findViewById(R.id.bottom_spacer);

        // load the styled attributes and set their properties
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.BouncyScrollView, defStyleAttr, 0);
        relativeStartingPosition = attributes.getFloat(R.styleable.BouncyScrollView_starting_position, 0.66f);
        viewAnimationDuration = attributes.getInteger(R.styleable.BouncyScrollView_anim_duration, 500);
        attributes.recycle();
    }

    /* Blood & Guts */

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(TAG, "onSizeChanged. h: " + h);
        setAbsoluteStartingPosition(relativeStartingPosition, h);
        initViewAnimator();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            /**
             * Set the height of the top and bottom spacers to the same size as this container so
             * the card has space to scroll out of screen.
             */
            LinearLayout.LayoutParams topSpacerLayoutParams = (LinearLayout.LayoutParams) topSpacer.getLayoutParams();
            topSpacerLayoutParams.height = getHeight();
            topSpacer.setLayoutParams(topSpacerLayoutParams);

            LinearLayout.LayoutParams bottomSpacerLayoutParams = (LinearLayout.LayoutParams) bottomSpacer.getLayoutParams();
            bottomSpacerLayoutParams.height = getHeight();
            bottomSpacer.setLayoutParams(bottomSpacerLayoutParams);
        }
    }

    private void initViewAnimator() {
        Log.d(TAG, "absoluteStartingPosition: " + absoluteStartingPosition);
        viewAnimator = ObjectAnimator.ofInt(this, "scrollY", 1, absoluteStartingPosition);
        viewAnimator.setInterpolator(viewAnimationInterpolator);
        viewAnimator.setDuration(viewAnimationDuration);
    }

    /**
     * Let child Views consume the MotionEvent if they want to. Also, check if the user is pressing
     * and holding the area outside of our custom view bounds. If they are, then we can ignore future
     * onTouchEvents that land on our custom view, as long as isDraggingOutside is true. If the user
     * quickly taps the screen, both ACTION_DOWN & _UP are passed to this method so take that into
     * account.
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!isTouchingView(ev)) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isDraggingOutside = true;
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    isDraggingOutside = false;
                    break;
            }
        } else {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isDraggingInside = true;
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    isDraggingInside = false;
                    break;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        /**
         * Only consume the drag/scroll event if the MotionEvent is within the bounds of our view.
         */
        int action = ev.getAction();
        if (isDraggingInside || (isTouchingView(ev) && !isDraggingOutside)) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)
                isDraggingInside = false;
            return super.onTouchEvent(ev);
        } else {
            switch (action) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isDraggingInside = false;
                    isDraggingOutside = false;
            }
            return false;
        }
    }

    /**
     * Does this MotionEvent land on our CardView?
     */
    private boolean isTouchingView(MotionEvent ev) {
        if (customView == null)
            return false;

        int[] location = {0, 0};
        customView.getLocationOnScreen(location);
        int left = location[0];
        int top = location[1];
        int right = left + viewContainer.getWidth();
        int bottom = top + viewContainer.getHeight();

        float x = ev.getRawX();
        float y = ev.getRawY();
        return (x > left)
                && (x < right)
                && (y > top)
                && (y < bottom);
    }

    private int lastT, lastOldT;

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        if (getOverScrollMode() == OVER_SCROLL_NEVER) {
            // There's a bug where the last scroll event gets repeated if overscroll is off.
            if (t == lastT && oldt == lastOldT)
                return;
            lastT = t;
            lastOldT = oldt;
        }

        if (eventListener != null) eventListener.onScrollChanged(this, l, t, oldl, oldt);

        if (t == 0) {
            Log.d(TAG, "hit bottom. t: " + t + ", oldt: " + oldt);
            resetPosition();
            if (eventListener != null) eventListener.onViewHitBottom(customView);
        } else if (t == getMaxScrollHeight()) {
            resetPosition();
            if (eventListener != null) eventListener.onViewHitTop(customView);
        }
    }

    /* API */

    public static interface EventListener {
        public void onViewHitBottom(View view);

        public void onViewHitTop(View view);

        public void onScrollChanged(BouncyScrollView scrollView, int l, int t, int oldl, int oldt);
    }

    public void resetPosition() {
        Log.d(TAG, "resetPosition");
        viewContainer.scrollTo(0, 1);
    }

    public void animateToStartingPosition() {
        Log.d(TAG, "animateToStartingPosition");
        resetPosition();
        viewAnimator.start();
    }

    private int getMaxScrollHeight() {
        return getHeight() - bottomSpacer.getHeight();
    }

    /* Getters & Setters */

    public Interpolator getInterpolator() {
        return viewAnimationInterpolator;
    }

    public void setInterpolator(Interpolator viewAnimationInterpolator) {
        this.viewAnimationInterpolator = viewAnimationInterpolator;
        initViewAnimator();
    }

    public int getViewAnimationDuration() {
        return viewAnimationDuration;
    }

    public void setViewAnimationDuration(int viewAnimationDuration) {
        this.viewAnimationDuration = viewAnimationDuration;
        initViewAnimator();
    }

    public float getRelativeStartingPosition() {
        return relativeStartingPosition;
    }

    public void setRelativeStartingPosition(float relativeStartingPosition) {
        this.relativeStartingPosition = relativeStartingPosition;
        setAbsoluteStartingPosition(relativeStartingPosition, getHeight());
        initViewAnimator();
    }

    private void setAbsoluteStartingPosition(float relativeStartingPosition, int height) {
        absoluteStartingPosition = (int) ((1 - relativeStartingPosition) * height);
    }

    public View getCustomView() {
        return customView;
    }

    public void setCustomView(View customView) {
        this.customView = customView;
        viewContainer.removeAllViews();
        viewContainer.addView(customView);
    }

    public EventListener getEventListener() {
        return eventListener;
    }

    public void setEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
    }
}

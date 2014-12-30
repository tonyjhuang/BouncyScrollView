package com.tonyjhuang.bouncyscrollview;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
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

    private boolean scrollable;

    private EventListener eventListener;
    private OnScrollStopListener onScrollStopListener = new OnScrollStopListener();

    /**
     * Once a certain percentage of the view is off the screen, should we automatically scroll it
     * entirely off? The threshold is the percentage of the view you want scrolled off before we
     * take control of it. Note: we will never auto-scroll the view as long as the user is touching
     * it.
     * <p/>
     * Setting it to 0f is effectively turning off scrollAssist whereas setting it to 1f will scroll
     * the view as long as it touches the edge of the screen. Finally, 0.5f will scroll the view if
     * at least half of it is scrolled offscreen.
     */
    private boolean scrollAssist;
    private int scrollAssistDuration;
    private float scrollAssistThreshold;

    public BouncyScrollView(Context context) {
        this(context, null);
    }

    public BouncyScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BouncyScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        inflate(context, R.layout.view_bouncy_scrollview, this);
        setOverScrollMode(OVER_SCROLL_NEVER);

        viewContainer = (FrameLayout) findViewById(R.id.view_container);
        topSpacer = (Space) findViewById(R.id.top_spacer);
        bottomSpacer = (Space) findViewById(R.id.bottom_spacer);

        // load the styled attributes and set their properties
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.BouncyScrollView, defStyleAttr, 0);
        relativeStartingPosition = attributes.getFloat(R.styleable.BouncyScrollView_starting_position, 0.66f);
        viewAnimationDuration = attributes.getInteger(R.styleable.BouncyScrollView_anim_duration, 500);
        scrollAssist = attributes.getBoolean(R.styleable.BouncyScrollView_scroll_assist, false);
        scrollAssistDuration = attributes.getInteger(R.styleable.BouncyScrollView_scroll_assist_anim_duration, 200);
        scrollAssistThreshold = attributes.getFloat(R.styleable.BouncyScrollView_scroll_assist_threshold, 0.5f);
        scrollable = attributes.getBoolean(R.styleable.BouncyScrollView_scrollable, true);
        attributes.recycle();
    }

    /* Blood & Guts */

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
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
        viewAnimator = ObjectAnimator.ofInt(this, "scrollY", 1, absoluteStartingPosition);
        viewAnimator.setInterpolator(viewAnimationInterpolator);
        viewAnimator.setDuration(viewAnimationDuration);
        viewAnimator.addListener(new SimpleAnimationListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                onScrollStopListener.setPause(false);
            }
        });
    }

    /**
     * Let child Views consume the MotionEvent if they want to. Also, check if the user is pressing
     * and holding the area outside of our custom view bounds. If they are, then we can ignore future
     * onTouchEvents that land on our custom view, as long as isDraggingOutside is true. If the user
     * quickly taps the screen, both ACTION_DOWN & _UP are passed to this method so take that into
     * account.
     */
    @Override
    public boolean onInterceptTouchEvent(@NonNull MotionEvent ev) {
        if (!scrollable) return false;

        onScrollStopListener.onInterceptTouchEvent(ev);
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
        if (!scrollable) return false;
        onScrollStopListener.onTouchEvent(ev);
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
    protected boolean isTouchingView(MotionEvent ev) {
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
        if (t == lastT && oldt == lastOldT)
            return;
        lastT = t;
        lastOldT = oldt;

        if (eventListener != null) eventListener.onScrollChanged(this, l, t, oldl, oldt);

        if (t == 0) {
            if (eventListener != null) eventListener.onViewHitBottom(customView);
        } else if (t == getMaxScrollHeight()) {
            if (eventListener != null) eventListener.onViewHitTop(customView);
        } else {
            onScrollStopListener.onScrollChanged(l, t, oldl, oldt);
        }
    }

    /* API */

    public static interface EventListener {
        public void onViewHitBottom(View view);

        public void onViewHitTop(View view);

        public void onScrollChanged(BouncyScrollView scrollView, int l, int t, int oldl, int oldt);
    }

    public void resetPosition() {
        viewContainer.scrollTo(0, 1);
    }

    public void animateToStartingPosition() {
        onScrollStopListener.setPause(true);
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if(viewAnimator != null) viewAnimator.start();
            }
        });
    }

    private int getMaxScrollHeight() {
        return topSpacer.getHeight() + (customView == null ? 0 : customView.getHeight());
    }

    /* Scroll Assist */

    /**
     * Check if our cardview is beyond the thresholds of readability (eg if either the top edge
     * of the CardView is near the bottom edge of the screen and vice versa). If so, scroll the
     * CardView entirely off screen.
     */
    private void scrollOffScreenIfNecessary() {
        if (!scrollAssist)
            return;

        int scrollY = getScrollY();
        int bottomThreshold = (int) ((customView == null ? 0 : customView.getHeight()) * scrollAssistThreshold);
        int topThreshold = getHeight() - bottomThreshold;

        //
        if (scrollY <= bottomThreshold) { // Should scroll down
            scrollDownOffscreen();
        } else if (scrollY >= topThreshold) { // Should scroll up
            scrollUpOffscreen();
        }
    }

    protected void scrollUpOffscreen() {
        scrollToPosition(getMaxScrollHeight(), scrollAssistDuration).start();
    }

    protected void scrollDownOffscreen() {
        scrollToPosition(0, scrollAssistDuration).start();
    }

    protected ObjectAnimator scrollToPosition(int position, int duration) {
        final ObjectAnimator animator = ObjectAnimator.ofInt(this, "scrollY", getScrollY(), position);
        animator.setDuration(duration);
        animator.setInterpolator(new AccelerateInterpolator());
        return animator;
    }

    /* Getters & Setters */

    public boolean isScrollAssist() {
        return scrollAssist;
    }

    public void setScrollAssist(boolean scrollAssist) {
        this.scrollAssist = scrollAssist;
    }

    public int getScrollAssistDuration() {
        return scrollAssistDuration;
    }

    public void setScrollAssistDuration(int scrollAssistDuration) {
        this.scrollAssistDuration = scrollAssistDuration;
    }

    public float getScrollAssistThreshold() {
        return scrollAssistThreshold;
    }

    public void setScrollAssistThreshold(float scrollAssistThreshold) {
        this.scrollAssistThreshold = scrollAssistThreshold;
    }

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

    public void setCustomView(final View customView) {
        setCustomView(customView, false);
    }

    public void setCustomView(final View customView, boolean animate) {
        setCustomView(customView, animate, 200);
    }

    public void setCustomView(final View customView, boolean animate, int duration) {
        if (this.customView == null || !animate) {
            resetPosition();
            this.customView = customView;
            viewContainer.removeAllViews();
            viewContainer.addView(customView);
            animateToStartingPosition();
        } else {
            onScrollStopListener.setPause(true);
            ObjectAnimator scrollDown = scrollToPosition(1, duration);
            scrollDown.addListener(new SimpleAnimationListener() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    setCustomView(customView);
                }
            });
            scrollDown.start();
        }
    }

    public boolean isScrollable() {
        return scrollable;
    }

    public void setScrollable(boolean scrollable) {
        this.scrollable = scrollable;
    }

    public EventListener getEventListener() {
        return eventListener;
    }

    public void setEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
    }

    /* Utility */
    public class OnScrollStopListener {

        /**
         * How often we should check if the ScrollView has stopped scrolling in millis.
         * This Runnable is only started after the user has started scrolling.
         * 16 millis = 1 frame @ 60fps.
         */
        private static final int DELAY = 32;

        /**
         * The y position we received last check.
         */
        private int oldY = 0;

        /**
         * Does the user currently have a finger down (are they scrolling)?
         */
        private boolean userFingerDown = false;

        /**
         * Stop operations! Why? I was getting issues with onScrollStop events being called
         * when setting a new custom view or animating the view to starting position. So that's why.
         */
        private boolean pause = false;

        /**
         * Runnable to compare the current to past scroll states.
         */
        private Runnable checkScrollView = new Runnable() {
            @Override
            public void run() {
                int y = getScrollY();
                if (y == oldY && !userFingerDown) {
                    onScrollStopped();
                } else {
                    oldY = y;
                    postDelayedRunnableCheck(DELAY);
                }
            }
        };

        public void onScrollChanged(int l, int t, int oldl, int oldt) {
            //TODO: change this to t?
            oldY = oldt;
            postDelayedRunnableCheck(DELAY);
        }


        public void onInterceptTouchEvent(MotionEvent ev) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    userFingerDown = true;
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    userFingerDown = false;
                    postDelayedRunnableCheck(0);
                    break;
            }
        }

        public void onTouchEvent(MotionEvent ev) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    userFingerDown = true;
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    userFingerDown = false;
                    postDelayedRunnableCheck(0);
                    break;
            }
        }

        public void setPause(boolean pause) {
            this.pause = pause;
        }

        /**
         * Check for scroll stoppage again in the future.
         */
        private void postDelayedRunnableCheck(int delay) {
            removeCallbacks(checkScrollView);
            postDelayed(checkScrollView, delay);
        }

        private void onScrollStopped() {
            if (scrollAssist && !pause)
                scrollOffScreenIfNecessary();
        }
    }

    private static class SimpleAnimationListener implements Animator.AnimatorListener {
        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }
    }
}

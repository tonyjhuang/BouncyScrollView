package com.tonyjhuang.bouncyscrollview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;

/**
 * Created by tony on 12/30/14.
 */
public class FadingBouncyScrollView extends BouncyScrollView {
    private static final float FADE_ALPHA = 0.25f;
    private static final int FADE_ANIM_DURATION = 200;

    private View customView;

    private float previousAlpha = 1f;

    public FadingBouncyScrollView(Context context) {
        super(context);
    }

    public FadingBouncyScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FadingBouncyScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setCustomView(View customView, boolean animation, int duration) {
        super.setCustomView(customView);
        this.customView = customView;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (customView == null) return;
        /**
         * Fade card out if it is below the start position
         */
        if (t < getBottomScrollAssistThreshold()) {
            customView.setAlpha((float) Math.pow(((float) t) / getBottomScrollAssistThreshold(), 1.6f));
        } else {
            customView.setAlpha(1.0f);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!isTouchingView(ev)) {
            /**
             * We really only care about the MotionEvent if the user isn't touching the card view.
             */
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    fadeView(true);
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    fadeView(false);
                    break;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        if (((!isTouchingView(ev) && !draggingInside) || draggingOutside))
            switch (ev.getAction()) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    fadeView(false);
            }
        return super.onTouchEvent(ev);
    }

    private void fadeView(boolean fade) {
        if (customView == null) return;
        customView.clearAnimation();

        if (fade) previousAlpha = customView.getAlpha();
        float startAlpha = fade ? previousAlpha : FADE_ALPHA;
        float endAlpha = fade ? FADE_ALPHA : previousAlpha;

        // If returning from a fade and our new alpha is less than the alpha that we faded to,
        // don't do anything.
        if (!fade && endAlpha > previousAlpha) return;

        customView.startAnimation(createAlphaAnimation(startAlpha, endAlpha, FADE_ANIM_DURATION));
    }

    private AlphaAnimation createAlphaAnimation(float startAlpha, float endAlpha, int duration) {
        AlphaAnimation anim = new AlphaAnimation(startAlpha, endAlpha);
        anim.setDuration(duration);
        anim.setFillAfter(true);
        return anim;
    }
}

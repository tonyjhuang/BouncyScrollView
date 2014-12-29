package com.tonyjhuang.bouncyscrollview;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ProgressBar;


public class MainActivity extends ActionBarActivity implements BouncyScrollView.EventListener {

    private final String TAG = getClass().getSimpleName();

    BouncyScrollView bouncyScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bouncyScrollView = (BouncyScrollView) findViewById(R.id.scrollview);
        bouncyScrollView.setEventListener(this);
        bouncyScrollView.setCustomView(new ProgressBar(this));

        bouncyScrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                bouncyScrollView.animateToStartingPosition();
            }
        });
    }

    @Override
    public void onViewHitBottom(View view) {
        Log.d(TAG, "onViewHitBottom");
        bouncyScrollView.animateToStartingPosition();
    }

    @Override
    public void onViewHitTop(View view) {
        Log.d(TAG, "onViewHitTop");
        bouncyScrollView.animateToStartingPosition();
    }

    @Override
    public void onScrollChanged(BouncyScrollView scrollView, int l, int t, int oldl, int oldt) {
        //Log.d(TAG, "onScrollChanged");
    }
}

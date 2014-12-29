package com.tonyjhuang.bouncyscrollview;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ProgressBar;


public class MainActivity extends ActionBarActivity implements BouncyScrollView.EventListener{

    private final String TAG = getClass().getSimpleName();

    BouncyScrollView bouncyScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bouncyScrollView = (BouncyScrollView) findViewById(R.id.scrollview);
        bouncyScrollView.setCustomView(new ProgressBar(this));

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "running");
                bouncyScrollView.animateToStartingPosition();
            }
        }, 1000);
    }

    @Override
    public void onViewHitBottom(View view) {
        Log.d(TAG, "onViewHitBottom");
    }

    @Override
    public void onViewHitTop(View view) {
        Log.d(TAG, "onViewHitTop");

    }

    @Override
    public void onScrollChanged(BouncyScrollView scrollView, int l, int t, int oldl, int oldt) {
        Log.d(TAG, "onScrollChanged");

    }
}

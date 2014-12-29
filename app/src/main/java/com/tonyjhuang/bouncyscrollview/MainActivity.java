package com.tonyjhuang.bouncyscrollview;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ProgressBar;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity implements BouncyScrollView.EventListener {

    private final String TAG = getClass().getSimpleName();

    BouncyScrollView bouncyScrollView;
    boolean flip = true;
    ProgressBar progressBar;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = new ProgressBar(this);
        textView = new TextView(this);
        textView.setText("HELLO WORLD");

        bouncyScrollView = (BouncyScrollView) findViewById(R.id.scrollview);
        bouncyScrollView.setEventListener(this);
        setNewView();

        bouncyScrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                bouncyScrollView.animateToStartingPosition();
            }
        });
    }

    private void setNewView() {
        bouncyScrollView.setCustomView(flip ? progressBar : textView);
        flip = !flip;
    }

    @Override
    public void onViewHitBottom(View view) {
        Log.d(TAG, "onViewHitBottom");
        setNewView();
        bouncyScrollView.animateToStartingPosition();
    }

    @Override
    public void onViewHitTop(View view) {
        Log.d(TAG, "onViewHitTop");
        setNewView();
        bouncyScrollView.animateToStartingPosition();
    }

    @Override
    public void onScrollChanged(BouncyScrollView scrollView, int l, int t, int oldl, int oldt) {
        //Log.d(TAG, "onScrollChanged");
    }
}

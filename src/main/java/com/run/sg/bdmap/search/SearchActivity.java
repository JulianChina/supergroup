package com.run.sg.bdmap.search;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.widget.LinearLayout;

import com.run.sg.bdmap.R;

/**
 * Created by yq on 2017/5/20.
 */
public class SearchActivity extends Activity {
    LinearLayout mRootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.la_search_activity_content);
        mRootView = (LinearLayout)findViewById(R.id.search_activity_content_root);
        if (savedInstanceState == null){
            mRootView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mRootView.getViewTreeObserver().removeOnPreDrawListener(this);
                    startRootAnimation();
                    return true;
                }
            });
        }
    }

    private void startRootAnimation() {
        mRootView.setScaleY(0.1f);
        mRootView.setPivotY(mRootView.getY() + mRootView.getHeight() / 3);

        mRootView.animate()
                .scaleY(1)
                .setDuration(100)
                .setInterpolator(new AccelerateInterpolator())
                .start();
    }
}

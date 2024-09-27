package com.web;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;

import com.cab.app.R;
import com.google.android.material.appbar.AppBarLayout;

public class WebViewBottomSheetBehavior extends CoordinatorLayout.Behavior<WebView> {
    private ObjectAnimator objectAnimator;
    private boolean initialized,isAnimating;

    public WebViewBottomSheetBehavior(){
    }
    public WebViewBottomSheetBehavior(@NonNull Context context, @Nullable AttributeSet attrs){
        super(context,attrs);
    }
    @Override
    public boolean onDependentViewChanged(@NonNull CoordinatorLayout parent, @NonNull WebView child, @NonNull View dependency) {
        if (isAnimating||!initialized)
            return false ;
        isAnimating=true;
        int offset = -(dependency.getHeight()-(parent.getHeight()- child.getMeasuredHeight()))/2;
        child.animate().translationY(Math.min(0, offset)).setDuration(301).withEndAction(()->isAnimating=false).start();
        Log.i("behaviour", "onDependentViewChanged: "+offset);
        return true;
    }

    @Override
    public boolean layoutDependsOn(@NonNull CoordinatorLayout parent, @NonNull WebView child, @NonNull View dependency) {
        if ((!initialized)&&dependency.getId() == R.id.standard_bottom_sheet){
            parent.post(()->{
                ViewGroup.LayoutParams params = child.getLayoutParams();
                params.height = parent.getMeasuredHeight()-dependency.getMeasuredHeight();
                child.setLayoutParams(params);
                initialized=true;
            });
        }
        return dependency instanceof FrameLayout && dependency.getId() == R.id.standard_bottom_sheet;
    }
}

package com.cab.welcome;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.cab.app.R;

public class WelcomeFrag extends Fragment {
    private final String animationUrl, title ,description;

    private TextView titleView , descriptionView;
    private LottieAnimationView animationView;

    public WelcomeFrag(String animationUrl, String title, String description) {
        this.animationUrl = animationUrl;
        this.title = title;
        this.description = description;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.welcome_fragment, container);
        animationView = rootView.findViewById(R.id.lottieAnimationView);
        titleView = rootView.findViewById(R.id.title);
        descriptionView = rootView.findViewById(R.id.description);
        animationView.setAnimationFromUrl(animationUrl);
        animationView.playAnimation();
        titleView.setText(title);
        descriptionView.setText(description);
        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
       // animationView.pauseAnimation();
    }

    @Override
    public void onResume() {
       // animationView.resumeAnimation();
        super.onResume();
    }
}

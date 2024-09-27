package com.cab.app;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ApplicationLifecycle extends Application implements Application.ActivityLifecycleCallbacks {

    private static boolean isInForeground=false;

    @Override
    public void onCreate() {
        super.onCreate();
       registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        isInForeground = true;
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        isInForeground = false;
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }
    public static boolean isAppInForeground() {
        return isInForeground;
    }
}


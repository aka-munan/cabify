package com.web;

import android.webkit.JavascriptInterface;

import com.cab.app.MainActivity;
import com.ride.utils.LocationHelper;

public class WebInterface {
    private static String TAG = WebInterface.class.getName();
    MainActivity mainActivity;

    public WebInterface(MainActivity context) {
        this.mainActivity = context;
    }
    @JavascriptInterface
    public void locationPicked(String latitude, String longitude) {
        LocationHelper.displayAddress(mainActivity,latitude + "," + longitude);
    }
    @JavascriptInterface
    public void getOrigin(){
        mainActivity.setOriginToWebView();
    }
}

package com.ride.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.cab.app.BuildConfig;
import com.cab.app.MainActivity;
import com.cab.utils.HttpUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LocationHelper {
    private static String myLastLocation="";
    private static final String TAG = LocationHelper.class.getName();

    public static Task<JSONObject> getSuggestions(String location, String query) {
        Map<String, String> params = new HashMap<>();
        params.put("q", query);
        params.put("at", location);
        params.put("limit", "5");
        params.put("apiKey", BuildConfig.HERE_API_KEY);
        return HttpUtils.sendGetRequest("https://autosuggest.search.hereapi.com/v1/autosuggest", params);
    }

    public static Task<JSONObject> getAddressFromCoords(String location) {
        Map<String, String> params = new HashMap<>();
        params.put("at", location);
        params.put("limit", "1");
        params.put("lang", "en-US");
        params.put("types", "address");
        params.put("apiKey", BuildConfig.HERE_API_KEY);
        return HttpUtils.sendGetRequest("https://revgeocode.search.hereapi.com/v1/revgeocode", params);
    }

    public static void displayAddress(MainActivity activity, String coords) {
        getAddressFromCoords(coords).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "locationPicked: ", task.getException());
                return;
            }
            try {
                final JSONObject map = (JSONObject) ((JSONArray) task.getResult().get("items")).get(0);
                Log.i(TAG, "displayAddress: " + map);
                if (!map.has("title"))
                    throw new Exception("data is empty");
                Map<String, String> item = new HashMap<>();
                item.put("name", map.getString("title"));
                JSONObject pos = map.getJSONObject("position");
                item.put("coords", coords);
                activity.locationPicked(item);
            } catch (Exception e) {
                Log.e(TAG, "locationPicked: ", e);
            }
        });
    }

    @SuppressLint("MissingPermission")
    public static Task<Location> getFusedLastLocation(Activity context) {
        TaskCompletionSource<Location> taskCompletionSource = new TaskCompletionSource<>();
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS) {
            Location location = getLastKnownLocation(context);
            taskCompletionSource.setResult(location);
            myLastLocation = location.getLatitude() + "," + location.getLongitude();
            return taskCompletionSource.getTask();
        }
        FusedLocationProviderClient client =
                LocationServices.getFusedLocationProviderClient(context);
        taskCompletionSource.setResult(null);
        Task<Location> task = client.getLastLocation();
        task.addOnSuccessListener(location -> {
            myLastLocation = location.getLatitude() + "," + location.getLongitude();
        });
        return task;
    }

    @SuppressLint("MissingPermission")
    private static Location getLastKnownLocation(Context context) {
        final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Location bestLocation = locationManager.getLastKnownLocation(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? LocationManager.FUSED_PROVIDER : LocationManager.NETWORK_PROVIDER);
        for (String provider : locationManager.getProviders(true)) {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null && (bestLocation == null || bestLocation.getAccuracy() < location.getAccuracy()))
                bestLocation = location;
        }
        if (bestLocation != null)
            myLastLocation = bestLocation.getLatitude() + "," + bestLocation.getLongitude();
        return bestLocation;
    }

    @SuppressLint("MissingPermission")
    public static void getSingleUpdate(Activity activity, android.location.LocationListener listener) {
        final LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        if (!isLocationEnabled(locationManager))
            promptUserToEnableLocation(activity, false);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, new android.location.LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                locationManager.removeUpdates(this);
                myLastLocation = location.getLatitude() + "," + location.getLongitude();
                listener.onLocationChanged(location);
            }
        });
    }

    public static void promptUserToEnableLocation(Activity activity, boolean cancelable) {
        new MaterialAlertDialogBuilder(activity).setTitle("Location service")
                .setMessage("To continue, turn on device location.\n Press \"ok\" to open location settings")
                .setCancelable(cancelable)
                .setPositiveButton("Ok", (dialogInterface, i) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    activity.startActivity(intent);
                }).setNegativeButton("Cancel", (dialogInterface, i) -> {

                }).show();
    }

    public static boolean isLocationEnabled(LocationManager locationManager) {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public static boolean hasLocationPermission(Activity activity) {
        return ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean shouldShowUiForPermission(Activity activity) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    public static void showLocationPermissionUi(MainActivity activity, View anchor,ResultCallback<Boolean> callback) {
        Snackbar.make(anchor, "Location permission denied.", Snackbar.LENGTH_LONG)
                .setAnchorView(anchor)
                .setAction("Settings", v -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.getPackageName(), null));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    activity.startActivity(intent);
                })
                .show();
        activity.resultCallback=callback;
    }

    @SuppressLint("MissingPermission")
    public static void getLocationUpdates(Activity activity, LocationListener locationListener) {
        final LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        if (!isLocationEnabled(locationManager))
            promptUserToEnableLocation(activity, false);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 50, locationListener);
    }

    @SuppressLint("MissingPermission")
    public static void removeListener(Activity activity, LocationListener locationListener) {
        final LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(locationListener);
    }

    public static double distanceBetween(double lat1, double lat2, double lon1,
                                         double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }

    public static String getMyLastLocation() {
        return myLastLocation;
    }
}

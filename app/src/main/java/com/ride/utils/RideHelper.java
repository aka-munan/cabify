package com.ride.utils;

import android.content.Context;
import android.util.Log;

import com.auth.User;
import com.cab.app.BuildConfig;
import com.cab.utils.HttpUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.ride.RideInstance;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class RideHelper {
    public static Task<JSONObject> bookRide(Context context, String data) {
        TaskCompletionSource<JSONObject> taskCompletionSource = new TaskCompletionSource<>();
        FirebaseUser user = User.getInstance().getFirebaseUser();
        HttpUtils.sendPostRequest(context, taskCompletionSource, "bookRide", user, data);
        return taskCompletionSource.getTask();
    }

    public static Task<JSONObject> acceptRide(Context context, String uid, String tripId) {
        TaskCompletionSource<JSONObject> taskCompletionSource = new TaskCompletionSource<>();
        try {
            JSONObject data = new JSONObject();
            data.put("passengerId", uid);
            data.put("tripId", tripId);
            FirebaseUser user = User.getInstance().getFirebaseUser();
            HttpUtils.sendPostRequest(context, taskCompletionSource, "acceptRide", user, data.toString());
        } catch (Exception e) {
            taskCompletionSource.setException(e);
        }
        return taskCompletionSource.getTask();
    }

    public static Task<JSONObject> getRoute(String location, String destination) {
        Map<String, String> params = new HashMap<>();
        params.put("transportMode", "car");
        params.put("origin", location);
        params.put("destination", destination);
        params.put("return", "summary");
        params.put("apiKey", BuildConfig.HERE_API_KEY);
        return HttpUtils.sendGetRequest("https://router.hereapi.com/v8/routes", params);
    }

    public static void rideFinished(Context context, String tripId) {
        TaskCompletionSource<JSONObject> taskCompletionSource = new TaskCompletionSource<>();
        FirebaseUser user = User.getInstance().getFirebaseUser();
        try {
            HttpUtils.sendPostRequest(context, taskCompletionSource, "rideFinished", user, new JSONObject().put("tripId", tripId).toString());
        } catch (JSONException e) {
            Log.e("RideHelper", "rideFinished: ", e);
        }
    }

    public static Task<JSONObject> askConfirmation(Context context, String tripId) {
        TaskCompletionSource<JSONObject> taskCompletionSource = new TaskCompletionSource<>();
        FirebaseUser user = User.getInstance().getFirebaseUser();
        try {
            HttpUtils.sendPostRequest(context, taskCompletionSource, "rideFinished", user, new JSONObject().put("tripId", tripId).toString());
        } catch (JSONException e) {
            Log.e("RideHelper", "rideFinished: ", e);
        }
        return taskCompletionSource.getTask();
    }

    public static Task<Map<String, Object>> getUserRideInstance(String passengerUid, String tripId) {
        Executor executor = Executors.newSingleThreadExecutor();
        AtomicReference<RideInstance> rideInstance = new AtomicReference<>();
        TaskCompletionSource<Map<String, Object>> taskCompletionSource = new TaskCompletionSource<>();
        FirebaseDatabase.getInstance().getReference("trips").child(passengerUid).child(tripId)
                .get().addOnCompleteListener(executor, task -> {
                    if (task.isSuccessful()) {
                        rideInstance.set(task.getResult().getValue(RideInstance.class));
                        return;
                    }
                    taskCompletionSource.setException(task.getException());
                });
        FirebaseDatabase.getInstance().getReference("users").child(passengerUid).get().addOnCompleteListener(executor, task -> {
            if (task.isSuccessful()) {
                User passenger = task.getResult().getValue(User.class);
                Map<String, Object> result = new HashMap<>();
                result.put("passenger", passenger);
                result.put("rideInstance", rideInstance.get());
                taskCompletionSource.setResult(result);
                return;
            }
            taskCompletionSource.setException(task.getException());
        });
        return taskCompletionSource.getTask();
    }

    public static Task<JSONObject> cancelRide(Context context, String tripId) {
        TaskCompletionSource<JSONObject> taskCompletionSource = new TaskCompletionSource<>();
        FirebaseUser user = User.getInstance().getFirebaseUser();
        try {
            HttpUtils.sendPostRequest(context, taskCompletionSource, "cancelRide", user, new JSONObject().put("tripId", tripId).toString());
        } catch (JSONException e) {
            taskCompletionSource.setException(e);
        }
        return taskCompletionSource.getTask();
    }
}

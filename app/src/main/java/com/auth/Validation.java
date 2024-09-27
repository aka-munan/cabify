package com.auth;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import com.auth.utils.Account;
import com.cab.app.R;
import com.cab.utils.HttpUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class Validation {
    private static final String TAG = "Validation";

    public static Task<JSONObject> validateFromServer(Context context, DriverDetails driverDetails) {
        JSONObject jsonObject = new JSONObject(driverDetails);
        FirebaseUser user = User.getInstance().getFirebaseUser();
        ExecutorService executor = Executors.newFixedThreadPool(1);

        TaskCompletionSource<JSONObject> taskCompletionSource = new TaskCompletionSource<>();
        HttpUtils.sendPostRequest(context, taskCompletionSource,"updatepaymentdetails", user, jsonObject.toString());
        return taskCompletionSource.getTask();
    }

    public static Task<JSONObject> updatePaymentDetails(Context context, Account account) {
        FirebaseUser user = User.getInstance().getFirebaseUser();
        ExecutorService executor = Executors.newFixedThreadPool(1);
        TaskCompletionSource<JSONObject> taskCompletionSource = new TaskCompletionSource<>();
        String message = account.toJson();

        HttpUtils.sendPostRequest(context, taskCompletionSource, "updatepaymentdetails",user, message);
        return taskCompletionSource.getTask();
    }

}

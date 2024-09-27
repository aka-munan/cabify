package com.cab.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.cab.app.R;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.connection.RealCall;

public class HttpUtils {
    private static final int CONNECTION_TIMEOUT = 10000;
    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    public static void sendPostRequest(Context context, TaskCompletionSource<JSONObject> taskCompletionSource, String path, FirebaseUser user, String jsonData) {
        user.getIdToken(true).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                final String token = task.getResult().getToken();

                final OkHttpClient client = new OkHttpClient();
                Call postRequest = client.newCall(createPostRequest(context, path, token, jsonData));
                postRequest.timeout().timeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
                attachTaskToCall(taskCompletionSource, postRequest);
            } else {
                taskCompletionSource.setException(new Exception("Please login again to continue"));
            }
        });

    }

    public static Request createPostRequest(Context context, String path, String token, String jsonBody) {
        String url = context.getString(R.string.server_addr) + path;
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Log.i("data", "sendPostRequest: " + body);
        return new Request.Builder()
                .url(url)
                .addHeader("token", token)
                .post(body)
                .build();
    }
    private static void attachTaskToCall(TaskCompletionSource taskCompletionSource, Call call){
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                taskCompletionSource.setException(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody body = response.body()) {
                    final JSONObject responseJson = new JSONObject(body.string());
                    if (!response.isSuccessful()) {
                        taskCompletionSource.setException(new Exception(responseJson.has("error")?responseJson.get("error").toString():responseJson.toString()));
                        return;
                    }
                    taskCompletionSource.setResult(responseJson);
                } catch (Exception e) {
                    taskCompletionSource.setException(e);
                }
            }
        });
    }
    public static Request createGetRequest(String url, Map<String, String> params) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        params.forEach(urlBuilder::addQueryParameter);
        String queryUrl = urlBuilder.build().toString();
        return new Request.Builder().url(queryUrl)
                .build();
    }

    public static Task<JSONObject> sendGetRequest(String url, Map<String, String> params) {
        TaskCompletionSource<JSONObject> taskCompletionSource = new TaskCompletionSource<>();
        final OkHttpClient client = new OkHttpClient();
        Request request = createGetRequest(url, params);
        Call call = client.newCall(request);
        call.timeout().timeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
        attachTaskToCall(taskCompletionSource, call);
        return taskCompletionSource.getTask();
    }
}

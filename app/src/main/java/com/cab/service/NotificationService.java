package com.cab.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.auth.User;
import com.auth.UserType;
import com.auth.utils.LogInUtils;
import com.cab.app.ApplicationLifecycle;
import com.cab.app.R;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class NotificationService extends FirebaseMessagingService {
    public static final String CHANNEL_ID = "cabify";

    @Override
    public void onNewToken(@NonNull String token) {
        User user = User.getInstance();
        Runnable pendingRunnable =  ()->{
            Map<String, Object> updates = new HashMap<>();
            updates.put("users" + user.getFirebaseUser().getUid() + "/msgToken", token);
            if (user.getUserType() == UserType.DRIVER)
                updates.put("drivers" + user.getFirebaseUser().getUid() + "/msgToken", token);
            FirebaseDatabase.getInstance().getReference().updateChildren(updates).addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w("fetch token", "Updating FCM registration token failed", task.getException());
                    return;
                }
                Log.i("fetch token", "db token updated", task.getException());
            });
        };
        if (user.getFirebaseUser()==null)
            LogInUtils.addResultCallback(user1 -> pendingRunnable.run());
        else pendingRunnable.run();
    }


    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        assert launchIntent != null;
        message.getData().forEach(launchIntent::putExtra);
        if (ApplicationLifecycle.isAppInForeground()) {
            startActivity(launchIntent);
            return;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        startChannel();
        showNotification("Cabify", message.getData().get("title"), pendingIntent);
    }

    @Override
    public void onDeletedMessages() {
        Log.i("messaging service", "onDeletedMessages: ");
    }


    public void startChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Screen Capture Service Channel",
                NotificationManager.IMPORTANCE_HIGH
        );
        NotificationManager manager = getApplicationContext().getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    public void showNotification(String title, String body, PendingIntent intent) {
        NotificationManager manager = getApplicationContext().getSystemService(NotificationManager.class);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(title)
                .setOngoing(true)
                .setWhen(0)
                .setAllowSystemGeneratedContextualActions(true)
                .setContentText(body)
                .setContentIntent(intent);
        Notification notification = builder.build();
        manager.notify(1, notification);
    }
}

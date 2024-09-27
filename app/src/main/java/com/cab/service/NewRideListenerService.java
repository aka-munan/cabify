package com.cab.service;

import static android.app.Notification.EXTRA_NOTIFICATION_ID;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.auth.User;
import com.cab.app.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class NewRideListenerService extends Service {
    private final String CHANNEL_ID = "forground1";
    private DatabaseReference driversReference;
    private User user;
    public NewRideListenerService(){

    }

    @Override
    public void onCreate() {
        createNotificationChannel();
        Notification notification = createNotification();
        startForeground(1, notification);
        driversReference = FirebaseDatabase.getInstance().getReference("drivers");
        user = User.getInstance();
        //set status as active
        driversReference.child(user.getFirebaseUser().getUid() + "/status").setValue("active");
    }

    private Notification createNotification() {
        Intent stopIntent = new Intent(this, NotificationBroadcastReceiver.class);
        stopIntent.setAction("com.cab.app.ActionNotification");
        stopIntent.putExtra(EXTRA_NOTIFICATION_ID, 1);
        PendingIntent intent = PendingIntent.getBroadcast(this, 0,stopIntent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("Location service")
                .setOngoing(true)
                .setFullScreenIntent(intent, true)
                .setAllowSystemGeneratedContextualActions(true)
                .setContentText("Location service required to find new rides")
                .addAction(com.material.login.R.drawable.arrow_back,"stop",intent);
        return builder.build();
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Screen Capture Service Channel",
                NotificationManager.IMPORTANCE_HIGH
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    @Override
    public void onDestroy() {
        //set status as offline
        driversReference.child(user.getFirebaseUser().getUid() + "/status").setValue("offline");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

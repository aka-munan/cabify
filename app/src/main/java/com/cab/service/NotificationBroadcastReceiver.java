package com.cab.service;

import static android.app.Notification.EXTRA_NOTIFICATION_ID;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.auth.User;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

public class NotificationBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), "com.cab.app.ActionNotification")) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.cancel(intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0));
            FirebaseDatabase.getInstance().getReference("drivers").child(User.getInstance().getFirebaseUser().getUid() + "/status").setValue("offline");
        }
    }
}

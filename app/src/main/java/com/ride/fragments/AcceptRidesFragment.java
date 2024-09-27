package com.ride.fragments;

import static android.app.Notification.INTENT_CATEGORY_NOTIFICATION_PREFERENCES;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;

import com.auth.User;
import com.cab.app.MainActivity;
import com.cab.app.R;
import com.cab.app.databinding.AcceptRidesBinding;
import com.cab.service.NotificationBroadcastReceiver;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.core.GeoHash;
import com.google.android.material.sidesheet.SideSheetBehavior;
import com.google.android.material.sidesheet.SideSheetCallback;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.ride.utils.LocationHelper;

import java.util.HashMap;
import java.util.Map;

public class AcceptRidesFragment extends Fragment {
    public static final int NOTIFICATION_ID = 11;
    private final MainActivity mainActivity;
    private AcceptRidesBinding binding;
    private SideSheetBehavior<FrameLayout> behavior;
    private SideSheetCallback callback;

    public AcceptRidesFragment(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = AcceptRidesBinding.inflate(inflater, container, false);
        behavior = SideSheetBehavior.from((FrameLayout) container);
        behavior.addCallback(callback = new SideSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int state) {
                if (state == SideSheetBehavior.STATE_HIDDEN) {
                    getParentFragmentManager().beginTransaction().remove(AcceptRidesFragment.this).commit();
                }
            }

            @Override
            public void onSlide(@NonNull View view, float v) {
            }
        });
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                behavior.hide();
            }
        });
        init();
        return binding.getRoot();
    }


    private void init() {
        binding.toolbar.setNavigationOnClickListener(v -> behavior.hide());
        binding.acceptRides.setOnClickListener(view -> {
            if (!LocationHelper.hasLocationPermission(requireActivity())) {
                mainActivity.resultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                mainActivity.resultCallback = (isGranted) -> {
                    if (!isGranted)
                        LocationHelper.showLocationPermissionUi(mainActivity, mainActivity.snacbar,null);
                };
                return;
            }

            final LocationManager locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
            if (LocationHelper.isLocationEnabled(locationManager)) {
                DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference("drivers");
                LocationHelper.getSingleUpdate(requireActivity(), location -> {
                    Map<String, Object> locData = new HashMap<>();
                    GeoLocation geoLocation = new GeoLocation(location.getLatitude(), location.getLongitude());
                    locData.put("location", location.getLatitude() + "," + location.getLongitude());
                    locData.put("geoHash", new GeoHash(geoLocation).getGeoHashString());
                    driverRef.child(User.getInstance().getFirebaseUser().getUid())
                            .updateChildren(locData).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(requireActivity(), "Congratulation, You will be notified for new rides in your area", Toast.LENGTH_LONG).show();
                                }
                            });

                });
                driverRef.child(User.getInstance().getFirebaseUser().getUid() + "/status").setValue("active");
                Intent stopIntent = new Intent(requireActivity(), NotificationBroadcastReceiver.class);
                stopIntent.setAction("com.cab.app.ActionNotification");
                stopIntent.addCategory(INTENT_CATEGORY_NOTIFICATION_PREFERENCES);
                stopIntent.putExtra(NotificationCompat.EXTRA_NOTIFICATION_ID, 11);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(requireActivity(), 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);
                final String CHANNEL_ID = "cabify";
                NotificationChannel serviceChannel = new NotificationChannel(
                        CHANNEL_ID,
                        "Screen Capture Service Channel",
                        NotificationManager.IMPORTANCE_HIGH
                );
                NotificationManager manager = requireActivity().getSystemService(NotificationManager.class);
                manager.createNotificationChannel(serviceChannel);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(requireActivity(), CHANNEL_ID)
                        .setSmallIcon(R.drawable.logo)
                        .setContentTitle(getString(R.string.app_name))
                        .setOngoing(true)
                        .setAllowSystemGeneratedContextualActions(true)
                        .setContentText("Location service required to find new rides")
                        .setAllowSystemGeneratedContextualActions(false)
                        .setWhen(0)
                        .addAction(com.material.login.R.drawable.arrow_back, "stop", pendingIntent);
                Notification notification = builder.build();
                manager.notify(NOTIFICATION_ID, notification);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                    return;
                if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    mainActivity.resultCallback = (isGranted) -> {
                        if (!isGranted) {
                            showNotifPermissionUi();
                        }
                    };
                    mainActivity.resultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
            } else
                LocationHelper.promptUserToEnableLocation(requireActivity(), false);
        });
    }

    private void showNotifPermissionUi() {
        Snackbar.make(mainActivity.snacbar, "Notification permission denied", Snackbar.LENGTH_LONG)
                .setAction("Settings", view2 -> {
                    Intent settingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", mainActivity.getPackageName(), null));
                    settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    settingsIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    mainActivity.startActivity(settingsIntent);
                }).show();
    }

    private boolean checkLocation() {
        final LocationManager locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
        if (!LocationHelper.isLocationEnabled(locationManager)) {
            LocationHelper.promptUserToEnableLocation(requireActivity(), false);
            return false;
        }
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        behavior.removeCallback(callback);
    }
}

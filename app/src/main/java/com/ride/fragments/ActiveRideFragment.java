package com.ride.fragments;

import android.annotation.SuppressLint;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.webkit.WebViewAssetLoader;

import com.auth.User;
import com.bumptech.glide.Glide;
import com.cab.app.MainActivity;
import com.cab.app.databinding.ActiveRideBinding;
import com.cab.app.databinding.RideFoundBottomSheetBinding;
import com.google.android.gms.tasks.Task;
import com.google.android.material.sidesheet.SideSheetBehavior;
import com.google.android.material.sidesheet.SideSheetCallback;
import com.google.firebase.database.FirebaseDatabase;
import com.ride.RideInstance;
import com.ride.utils.LocationHelper;
import com.ride.utils.RideHelper;
import com.web.LoadWebViewClient;
import com.web.WebInterface;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ActiveRideFragment extends Fragment {
    private final String TAG = ActiveRideFragment.class.getSimpleName();
    private final MainActivity mainActivity;
    private final Handler handler;
    private ActiveRideBinding binding;
    private RideFoundBottomSheetBinding rideDetailsBinding;
    private final List<Runnable> pendingRunnables = new ArrayList<>();
    private String tripId;
    private String passengerId;
    private int status;
    private RideInstance rideInstance;
    private SideSheetBehavior<FrameLayout> behavior;
    private LocationListener locationListener;
    private double[] oldLocation = new double[2];
    private double[] newLocation = new double[2];
    private Runnable drawNavIconRunnable;
    private SideSheetCallback callback;

    public ActiveRideFragment(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        handler = new Handler(Looper.getMainLooper());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ActiveRideBinding.inflate(inflater, container, false);
        behavior = SideSheetBehavior.from(((FrameLayout) container));
        rideDetailsBinding = binding.rideDetails;
        init();
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!pendingRunnables.isEmpty()) {
            for (Runnable pendingRunnable : pendingRunnables) {
                pendingRunnable.run();
                pendingRunnables.remove(pendingRunnable);
            }
        }
        if (locationListener != null)
            LocationHelper.getLocationUpdates(requireActivity(), locationListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (locationListener != null)
            LocationHelper.removeListener(requireActivity(), locationListener);
    }

    public void statusChanged(String tripId, String passengerId, int status) {
        this.tripId = tripId;
        this.passengerId = passengerId;
        this.status = status;
        Runnable code = () -> {
            if (status == RideInstance.DRIVER_ASSIGNED) {
                driverAssigned();
            } else if (status == RideInstance.RIDE_STARTED) {
                if (locationListener != null)
                    LocationHelper.removeListener(requireActivity(), locationListener);
                rideStarted();
            }
        };
        if (isResumed()) {
            code.run();
        } else {
            pendingRunnables.add(code);
        }
    }

    private void rideStarted() {
        Runnable runnable = () -> {
            locationListener = location -> {

                /*listen for location change*/
                if (newLocation[0] > 0)
                    oldLocation = newLocation.clone();
                newLocation[0] = location.getLatitude();
                newLocation[1] = location.getLongitude();
                updateLocationToDb(location.getLatitude() + "," + location.getLongitude());
                if (rideInstance == null || rideInstance.getStatus() != RideInstance.DRIVER_ASSIGNED)
                    return;
                int index = rideInstance.getLocation().lastIndexOf(",");
                drawRoute(location.getLatitude() + "," + location.getLongitude(), rideInstance.getLocation(), "#75FB4C", false);
                drawNavIcon(oldLocation[0], oldLocation[1], location.getLatitude(), location.getLongitude());
                if (LocationHelper.distanceBetween(newLocation[0], newLocation[1], Double.parseDouble(rideInstance.getLocation().substring(0, index)), Double.parseDouble(rideInstance.getLocation().substring(index + 1)), 0, 0) > 100) {
                    Log.i(TAG, "rideStarted: distance is <100 meters");
                    RideHelper.askConfirmation(getContext(), tripId).addOnFailureListener(e -> {
                        Log.e(TAG, "onPageFinished: ", e);
                        Toast.makeText(getContext(), "Failure: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            };
            LocationHelper.getLocationUpdates(requireActivity(), locationListener);
            if (rideInstance == null) displayPrimaryDetails();
        };

        if (rideInstance != null) {
            runnable.run();
        } else {
            FirebaseDatabase.getInstance().getReference("trips").child(passengerId).child(tripId).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            rideInstance = task.getResult().getValue(RideInstance.class);
                            int index = rideInstance.getLocation().lastIndexOf(",");
                            oldLocation[0] = Double.parseDouble(rideInstance.getLocation().substring(0, index));
                            oldLocation[1] = Double.parseDouble(rideInstance.getLocation().substring(index + 1));
                            if (!pendingRunnables.isEmpty() && isVisible()) {
                                for (Runnable pendingRunnable : pendingRunnables) {
                                    pendingRunnable.run();
                                    pendingRunnables.remove(pendingRunnable);
                                }
                            }
                        }
                    });
            pendingRunnables.add(runnable);
        }
    }

    private void init() {
        rideDetailsBinding.acceptBtn.setVisibility(View.GONE);
        rideDetailsBinding.rejectBtn.setVisibility(View.GONE);
        rideDetailsBinding.dragHandle.setVisibility(View.GONE);
        rideDetailsBinding.distanceLayout.setVisibility(View.VISIBLE);
        requireActivity().getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            long lastPressed = 0;

            @Override
            public void handleOnBackPressed() {
                if (!isVisible())
                    return;
                if (binding.content.getVisibility() != View.VISIBLE) {
                    animateView(binding.cardview, binding.content, false);
                    return;
                }
                if (System.currentTimeMillis() - lastPressed < 1000) {
                    //close
                    behavior.hide();
                } else {
                    lastPressed = System.currentTimeMillis();
                    Toast.makeText(requireActivity(), "Press Back Again To Close", Toast.LENGTH_SHORT).show();
                }
            }
        });
        behavior.addCallback(callback = new SideSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int i) {
                if (i == SideSheetBehavior.STATE_HIDDEN) {
                    getParentFragmentManager().beginTransaction().remove(ActiveRideFragment.this).commit();
                    onDestroy();
                }
            }

            @Override
            public void onSlide(@NonNull View view, float v) {
            }
        });
        loadMap();
        binding.getDirectionsBtn.setOnClickListener(view -> animateView(binding.cardview, binding.content, true));
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void loadMap() {
        binding.mapView.getSettings().setJavaScriptEnabled(true);
        binding.mapView.addJavascriptInterface(new WebInterface(null) {

            @JavascriptInterface
            @Override
            public void getOrigin() {
            }
        }, "Android");
        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(requireActivity()))
                .build();
        binding.mapView.setWebViewClient(new LoadWebViewClient(assetLoader) {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (!isVisible())
                    return;
                Runnable drawRoute = () -> drawRoute(rideInstance.getLocation(), rideInstance.getDestination(), "#2854C5", true);
                if (rideInstance != null) {
                    drawRoute.run();
                } else pendingRunnables.add(drawRoute);
                locationListener = location -> {

                    /*listen for location change*/

                    if (newLocation[0] > 0)
                        oldLocation = newLocation.clone();
                    newLocation[0] = location.getLatitude();
                    newLocation[1] = location.getLongitude();
                    drawNavIcon(oldLocation[0], oldLocation[1], newLocation[0], oldLocation[1]);
                    updateLocationToDb(location.getLatitude() + "," + location.getLongitude());
                    if (rideInstance == null || rideInstance.getStatus() != RideInstance.DRIVER_ASSIGNED)
                        return;
                    int index = rideInstance.getLocation().lastIndexOf(",");
                    drawRoute(location.getLatitude() + "," + location.getLongitude(), rideInstance.getLocation(), "#8C1A10", false);
                    drawNavIcon(oldLocation[0], oldLocation[1], location.getLatitude(), location.getLongitude());
                    if (LocationHelper.distanceBetween(newLocation[0], newLocation[1], Double.parseDouble(rideInstance.getLocation().substring(0, index)), Double.parseDouble(rideInstance.getLocation().substring(index + 1)), 0, 0) > 100) {
                        Log.i(TAG, "onPageFinished: distance is <100 meters");
                        RideHelper.askConfirmation(getContext(), tripId).addOnFailureListener(e -> {
                            Log.e(TAG, "onPageFinished: ", e);
                            Toast.makeText(getContext(), "Failure: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                };
                Runnable listenForLocationPrem = new Runnable() {
                    @Override
                    public void run() {
                        if (!LocationHelper.hasLocationPermission(requireActivity())) {
                            LocationHelper.showLocationPermissionUi(mainActivity, mainActivity.snacbar, (isGranted) -> LocationHelper.getLocationUpdates(requireActivity(), locationListener));
                            handler.postDelayed(this, 5000);
                            return;
                        }
                        LocationHelper.removeListener(mainActivity, locationListener);
                    }
                };
                listenForLocationPrem.run();
            }
        });
        binding.mapView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
        binding.mapView.setOnTouchListener((view, event) ->

        {
            if (binding.content.getVisibility() == View.VISIBLE) {
                animateView(binding.cardview, binding.content, true);
                return true;
            }
            return false;
        });
    }

    public void drawRoute(String location, String destination, String strokeColor, boolean isPrimaryRoute) {
        String jsCode = "calculateRouteFromAtoB('" + location + "','" + destination + "','" + strokeColor + "'," + isPrimaryRoute + "," + isPrimaryRoute + ");";
        binding.mapView.evaluateJavascript(jsCode, null);
    }

    private void driverAssigned() {
        FirebaseDatabase.getInstance().getReference("trips").child(passengerId + "/" + tripId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.e(TAG, "driverAssigned: " + task.getResult());
                        rideInstance = task.getResult().getValue(RideInstance.class);
                        int index = rideInstance.getLocation().lastIndexOf(",");
                        oldLocation[0] = Double.parseDouble(rideInstance.getLocation().substring(0, index));
                        oldLocation[1] = Double.parseDouble(rideInstance.getLocation().substring(index + 1));

                        if (!pendingRunnables.isEmpty()) {
                            for (Runnable pendingRunnable : pendingRunnables) {
                                pendingRunnable.run();
                                pendingRunnables.remove(pendingRunnable);
                            }
                        }
                        if (rideDetailsBinding.pickUp.getText().toString().isEmpty()) {
                            displayPrimaryDetails();
                        }
                        if (locationListener != null)
                            return;
                        return;
                    }
                    Log.e(TAG, "driverAssigned: ", task.getException());
                });
    }

    private void displayPrimaryDetails() {
        RideHelper.getRoute(rideInstance.getLocation(), rideInstance.getDestination()).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "displayPrimaryDetails: ", task.getException());
                return;
            }
            if (!isVisible())
                return;
            JSONObject route = task.getResult();
            try {
                route = route.getJSONArray("routes").getJSONObject(0).getJSONArray("sections").getJSONObject(0).getJSONObject("summary");
                rideDetailsBinding.distance2.setText(route.getLong("length") / 1000 + " km");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });
        LocationHelper.getAddressFromCoords(rideInstance.getLocation()).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "displayPrimaryDetails: ", task.getException());
                return;
            }
            if (!isVisible())
                return;
            try {
                JSONObject locationName = task.getResult().getJSONArray("items").getJSONObject(0);
                if (locationName.has("title"))
                    rideDetailsBinding.pickUp.setText(locationName.getString("title"));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            ;

        });
        LocationHelper.getAddressFromCoords(rideInstance.getDestination()).addOnCompleteListener(task -> {

            if (!isVisible())
                return;
            if (!task.isSuccessful()) {
                Log.e(TAG, "displayPrimaryDetails: ", task.getException());
                return;
            }
            requireActivity().runOnUiThread(() -> {
                final JSONObject destinationName;
                try {
                    destinationName = task.getResult().getJSONArray("items").getJSONObject(0);

                    if (destinationName.has("title"))
                        rideDetailsBinding.destination.setText(destinationName.getString("title"));
                    rideDetailsBinding.passengers.setText(String.valueOf(rideInstance.getPassengers()));
                    rideDetailsBinding.fare.setText(rideInstance.getFare() + "₹");
                    rideDetailsBinding.eta.setText(RideInstance.getTime(rideInstance.getEta(), true));

                } catch (JSONException e) {
                    Log.e(TAG, "displayPrimaryDetails: ", e);
                }
            });
        });

        FirebaseDatabase.getInstance().getReference("users").child(passengerId).get()
                .addOnCompleteListener(task -> {
                    if (!isVisible())
                        return;
                    if (task.isSuccessful()) {
                        User passenger = task.getResult().getValue(User.class);
                        Glide.with(requireActivity())
                                .load(passenger.getProfileUrl())
                                .into(rideDetailsBinding.profile);
                        rideDetailsBinding.title.setText(passenger.getUserName());

                        return;
                    }
                    Log.e(TAG, "displayPrimaryDetails: ", task.getException());
                });
    }

    private void drawNavIcon(double lat1, double lng1, double lat2, double lng2) {
        String str = String.format("drawNavIcon(%1$s,%2$s,%3$s,%4$s);", lat1, lng1, lat2, lng2);
        binding.mapView.evaluateJavascript(str, null);
    }

    private Task<Void> updateLocationToDb(String location) {
        return FirebaseDatabase.getInstance().getReference("drivers").child(User.getInstance().getFirebaseUser().getUid()).child("location")
                .setValue(location);
    }

    private void animateView(View mainView, View view, boolean hide) {
        view.animate().translationY(hide ? view.getHeight() : 0)
                .setDuration(300)
                .withStartAction(() -> view.setVisibility(hide ? View.GONE : View.VISIBLE))
                .withEndAction(() -> view.setVisibility(hide ? View.GONE : View.VISIBLE)).start();
        mainView.animate().scaleX(hide ? (float) binding.constraint.getWidth() / mainView.getWidth() : 1).setDuration(300).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        behavior.removeCallback(callback);
    }

    public RideInstance getRideInstance() {
        return rideInstance;
    }

}

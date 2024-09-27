package com.ride.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.auth.User;
import com.bumptech.glide.Glide;
import com.cab.app.R;
import com.cab.app.databinding.RideStatusBottomSheetBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ride.RideInstance;
import com.ride.utils.RideHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class RideStatusFragment extends BottomSheetDialogFragment {
    private static final String TAG = RideStatusFragment.class.getSimpleName();
    private final String tripId;
    private RideInstance rideInstance;
    private RideStatusBottomSheetBinding binding;
    private long distanceFromDriver = 0;
    private Runnable pendingCode;
    private User user;
    private Handler handler;
    private Map<String, Object> driver;
    private DatabaseReference driverLocationRef;
    private ValueEventListener locationListener;

    public RideStatusFragment(String tripId, RideInstance rideInstance) {
        this.tripId = tripId;
        this.rideInstance = rideInstance;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = RideStatusBottomSheetBinding.inflate(inflater, container, false);
        user = User.getInstance();
        init();
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (pendingCode != null) {
            pendingCode.run();
            pendingCode = null;
        }
    }

    private void init() {
        binding.requestedAt.setText("You requested a ride at " + RideInstance.getDate(rideInstance.getDate(), "hh:mm:aa"));
        binding.title.setText(user.getUserName());
        Glide.with(requireActivity()).load(user.getProfileUrl()).into(binding.profile);
        handler = new Handler();
        binding.driverName.setText("Waiting for confermation...");
        handler.post(new Runnable() {
            @Override
            public void run() {
                long diff = System.currentTimeMillis() - rideInstance.getDate();
                Log.i("TAG", "init: " + diff);
                if (diff > 300000L) {
                    binding.cancelBtn.setText("Cancel");
                    binding.cancelBtn.setEnabled(false);
                } else if (diff < 300000L) {
                    binding.cancelBtn.setText("Cancel within " + RideInstance.getTime(300 - diff / 1000, true));
                    handler.postDelayed(this, 60000);
                }
            }
        });
        binding.cancelBtn.setOnClickListener(view -> {
            RideHelper.cancelRide(requireContext(), tripId).addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "init: ", task.getException());
                    return;
                }
                dismiss();
            });
        });
    }

    public void updateRide(RideInstance rideInstance) {
        this.rideInstance = rideInstance;
        if (rideInstance.getStatus() == RideInstance.DRIVER_ASSIGNED || rideInstance.getStatus() == RideInstance.CONFIRM_RIDE)
            driverAssigned();
        else if (rideInstance.getStatus() == RideInstance.RIDE_STARTED) rideStarted();
        else if (rideInstance.getStatus() == RideInstance.RIDE_FINISHED) rideCompleted();
    }

    private void driverAssigned() {
        Runnable code = () -> {
            binding.progressbar1.setProgress(100);
            FirebaseDatabase.getInstance().getReference("drivers").child(rideInstance.getDriverUid()).get().addOnCompleteListener(task -> {
                driver = (Map<String, Object>) task.getResult().getValue();
                showDriverInfo(driver);
            });
        };
        if (!isResumed()) {
            pendingCode = code;
            return;
        }
        code.run();
    }

    private void showDriverInfo(Map driver) {
        RideHelper.getRoute((String) driver.get("location"), rideInstance.getLocation()).addOnCompleteListener(task -> {
            try {
                binding.circle1.setImageResource(R.drawable.circle_filled);
                binding.circle2.setImageResource(R.drawable.circle_filled);
                JSONObject route = task.getResult().getJSONArray("routes").getJSONObject(0).getJSONArray("sections").getJSONObject(0).getJSONObject("summary");
                distanceFromDriver = route.getLong("length");
                binding.driverAssignedText.setText("Driver Name: " + driver.get("uname") + "\nReg. No.: " + driver.get("registration"));

                binding.progressbar2.setProgress(100);
                if (rideInstance.getStatus() == RideInstance.CONFIRM_RIDE) {
                    String str = "Driver Has arrived";
                    binding.driverApproachingText.setText(str);
                    binding.driverName.setText(str);
                    binding.rideStarted.setText("Ride Confirmation");
                    binding.rideStartedText.setText("Please confirm ride if driver is near you!");
                    binding.confirmRideBtn.setVisibility(View.VISIBLE);
                    return;
                }
                String str = "Your driver, " + driver.get("uname") + ", is " + RideInstance.getTime(route.getLong("duration"), true) + " away";
                binding.driverApproachingText.setText(str);
                binding.driverName.setText(str);
                showLocationProgress();
            } catch (Exception e) {
                Log.e(TAG, "showDriverInfo: ", e);
            }
        });

    }

    private void showLocationProgress() {
        if (locationListener != null)
            driverLocationRef.removeEventListener(locationListener);
        driverLocationRef = FirebaseDatabase.getInstance().getReference("drivers").child(rideInstance.getDriverUid()).child("location");
        locationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.i(TAG, "onDataChange: " + snapshot.getValue());
                String driverLocation = snapshot.child("location").getValue(String.class);
                RideHelper.getRoute(driverLocation, rideInstance.getLocation()).addOnCompleteListener(task -> {
                    try {
                        JSONObject route = task.getResult().getJSONArray("routes").getJSONObject(0).getJSONArray("sections").getJSONObject(0).getJSONObject("summary");
                        int difInPercent = (int) (((float) distanceFromDriver / route.getLong("length")) * 90);
                        binding.progressbar3.setProgress(difInPercent);
                        String str = "Your driver, " + driver.get("uname") + ", is " + RideInstance.getTime(route.getLong("duration"), true) + " away";
                        binding.driverApproachingText.setText(str);
                        binding.driverName.setText(str);
                    } catch (Exception e) {
                        Log.e(TAG, "onDataChange: ", e);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        driverLocationRef.addValueEventListener(locationListener);
    }

    private void ShowRideProgress() {
        if (locationListener != null)
            driverLocationRef.removeEventListener(locationListener);
        driverLocationRef = FirebaseDatabase.getInstance().getReference("drivers").child(rideInstance.getDriverUid()).child("location");
        locationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String driverLocation = snapshot.child("location").getValue(String.class);
                RideHelper.getRoute(rideInstance.getLocation(), rideInstance.getDestination()).addOnCompleteListener(task -> {
                    try {
                        JSONObject route = task.getResult().getJSONArray("routes").getJSONObject(0).getJSONArray("sections").getJSONObject(0).getJSONObject("summary");
                        int difInPercent = (int) (((float) rideInstance.getDistance() / route.getLong("length")) * 100);
                        binding.approachedDestinationText.setText("Approaching destination in " + RideInstance.getTime(route.getLong("duration"), false));
                        binding.progressbar4.setProgress(difInPercent);
                        if (route.getLong("length") < 50)
                            RideHelper.rideFinished(requireActivity(), tripId);
                    } catch (JSONException e) {
                        Log.e(TAG, "onDataChange: ", e);
                    }

                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        driverLocationRef.addValueEventListener(locationListener);
    }

    private void rideStarted() {
        Runnable code = () -> {
            binding.progressbar1.setProgress(100);
            binding.progressbar2.setProgress(100);
            binding.progressbar3.setProgress(100);
            if (driver == null)
                FirebaseDatabase.getInstance().getReference("drivers").child(rideInstance.getDriverUid()).get().addOnCompleteListener(task -> {
                    driver = (Map<String, Object>) task.getResult().getValue();
                    showDriverInfo(driver);
                });
            ShowRideProgress();
        };

        if (!isResumed()) {
            pendingCode = code;
            return;
        }
        code.run();
    }

    private void rideCompleted() {
        binding.progressbar1.setProgress(100);
        binding.progressbar2.setProgress(100);
        binding.progressbar3.setProgress(100);
        binding.progressbar4.setProgress(100);
        binding.approachedDestinationText.setText("You have reached your destination");
        if (driver == null)
            return;
        String str = "Your driver, " + driver.get("uname");
        binding.driverApproachingText.setText(str);
        binding.driverName.setText(str);
        binding.cancelBtn.setText("Done");
        binding.cancelBtn.setEnabled(true);
        binding.cancelBtn.setOnClickListener(view -> {
            dismiss();
        });
    }

}

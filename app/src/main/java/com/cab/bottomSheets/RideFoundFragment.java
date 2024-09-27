package com.cab.bottomSheets;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.auth.User;
import com.bumptech.glide.Glide;
import com.cab.app.databinding.RideFoundBottomSheetBinding;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.ride.RideInstance;
import com.ride.utils.LocationHelper;
import com.ride.utils.RideHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class RideFoundFragment extends BottomSheetDialogFragment {
    private final String TAG = RideFoundFragment.class.getSimpleName();
    private final String title;
    private final String tripId;
    private final String passengerUid;
    private RideFoundBottomSheetBinding binding;

    public RideFoundFragment(String title, String tripId, String passengerUid) {
        this.title = title;
        this.tripId = tripId;
        this.passengerUid = passengerUid;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = RideFoundBottomSheetBinding.inflate(inflater, container, false);
        binding.title.setText(title);
        init();
        return binding.getRoot();
    }

    private void init() {
        RideHelper.getUserRideInstance(passengerUid, tripId).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Map result = task.getResult();
                User passenger = (User) result.get("passenger");
                RideInstance rideInstance = (RideInstance) result.get("rideInstance");
                binding.uname.setText(passenger.getUserName() + " • ");
                Glide.with(requireActivity())
                        .load(passenger.getProfileUrl())
                        .into(binding.profile);
                showLocationName(rideInstance);
                binding.passengers.setText(""+rideInstance.getPassengers());
                binding.fare.setText(rideInstance.getFare()+"₹");
                binding.eta.setText(RideInstance.getTime(rideInstance.getEta(),false));
                binding.distance.setText(rideInstance.getDistance() / 1000 + " km");
                binding.acceptBtn.setOnClickListener(view->{
                    view.setEnabled(false);
                    RideHelper.acceptRide(requireActivity(), passengerUid, tripId).addOnCompleteListener(task1 -> {
                        view.setEnabled(true);
                        dismiss();
                        if (task1.isSuccessful()){

                            return;
                        }
                        Log.e(TAG, "init: "+task.getException().getMessage() );
                    });
                });
                binding.rejectBtn.setOnClickListener(view->{
                    dismiss();
                });
                return;
            }
            Log.e(TAG, "init: ", task.getException());
        });
    }

    private void showLocationName(RideInstance rideInstance) {
        LocationHelper.getAddressFromCoords(rideInstance.getLocation()).addOnCompleteListener(task1 -> {
            if (task1.isSuccessful()){
                displayNameInto(task1, binding.pickUp);
            }
        });
        LocationHelper.getAddressFromCoords(rideInstance.getDestination()).addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                displayNameInto(task,binding.destination);
            }
        });
    }
    private void displayNameInto(Task<JSONObject> task, TextView textView){

        try {
            final JSONObject map = (JSONObject) ((JSONArray) task.getResult().get("items")).get(0);
            Log.i(TAG, "displayAddress: " + map);
            if (map.has("title")){
                textView.setText(map.getString("title"));
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}

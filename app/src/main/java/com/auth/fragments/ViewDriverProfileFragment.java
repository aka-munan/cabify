package com.auth.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;

import com.auth.DriverDetails;
import com.auth.User;
import com.auth.utils.Account;
import com.bumptech.glide.Glide;
import com.cab.app.R;
import com.cab.app.databinding.ViewDriverProfileBinding;
import com.google.android.material.badge.ExperimentalBadgeUtils;
import com.google.android.material.sidesheet.SideSheetBehavior;
import com.google.android.material.sidesheet.SideSheetCallback;

import java.util.Objects;

public class ViewDriverProfileFragment extends Fragment {
    private final String TAG = "ViewDriverProfileFrag";
    private ViewDriverProfileBinding binding;
    private User user;
    private DriverDetails driverDetails;
    private Account account;
    private SideSheetBehavior<FrameLayout> behavior;
    private SideSheetCallback callback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        behavior = SideSheetBehavior.from(((FrameLayout) container));
        binding = ViewDriverProfileBinding.inflate(inflater, container, false);
        user = User.getInstance();
        behavior.addCallback(callback=new SideSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int i) {
                if (i == SideSheetBehavior.STATE_HIDDEN) {
                    getParentFragmentManager().beginTransaction().remove(ViewDriverProfileFragment.this).commit();
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
        user.getDriverDetails().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                driverDetails = task.getResult().getValue(DriverDetails.class);
                account=task.getResult().child("payment").getValue(Account.class);
                Log.i(TAG, "init: driver details: " + driverDetails+"\n"+account.toJson());
                if (driverDetails == null)
                    return;
                displayDetails();
            } else {
                Log.e(TAG, "init: ", task.getException());
            }
        });
        Glide.with(getContext()).load(user.getProfileUrl())
                .into(binding.profileView);
        binding.toolbar.setNavigationOnClickListener(view->{
            Objects.requireNonNull(requireActivity()).getOnBackPressedDispatcher().onBackPressed();
        });
    }

    @OptIn(markerClass = ExperimentalBadgeUtils.class)
    private void displayDetails(){
        //dynamic vehicle image
        switch (driverDetails.getTaxiType()){
            case CAR:
                binding.vehicleImg.setImageResource(R.drawable.car);
                break;
            case BIKE:
                break;
            case ELECTRIC_CAR:
                binding.vehicleImg.setImageResource(R.drawable.electric_car);
        }
        //user
        binding.username.setText(user.getUserName());
        binding.contact.setText(user.getContact());
        //driver details
        binding.vehicleName.setText(driverDetails.getVehicleName());
        binding.registrationNo.setText(driverDetails.getRegistration());
        binding.dob.setText(driverDetails.getDob());
        binding.address.setText(driverDetails.getAddress());
        //payment details
        binding.accountName.setText(account.getUserName());
        binding.bankName.setText(account.getBankName());
        binding.acNumber.setText(account.getAcNumber());
        binding.ifsc.setText(account.getIfsc());
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        behavior.removeCallback(callback);
    }
}

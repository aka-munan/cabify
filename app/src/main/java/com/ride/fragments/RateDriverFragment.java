package com.ride.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cab.app.databinding.RateDriverLayoutBinding;
import com.google.android.material.chip.Chip;
import com.google.android.material.sidesheet.SideSheetBehavior;
import com.google.android.material.sidesheet.SideSheetCallback;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.Collections;

public class RateDriverFragment extends Fragment {
    private final String driverId;
    private RateDriverLayoutBinding binding;
    private SideSheetBehavior<FrameLayout> behavior;
    private SideSheetCallback callback;
    private int starIndex;

    public RateDriverFragment(String driverId) {
        this.driverId = driverId;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = RateDriverLayoutBinding.inflate(inflater);
        behavior = SideSheetBehavior.from(((FrameLayout) container));

            for (int i = 0; i < binding.starsGroup.getChildCount(); i++) {
                Chip chip = (Chip) binding.starsGroup.getChildAt(i);
                int checkedIndex = i;
                chip.setOnClickListener(view->{
                    for (int j = 0; j < binding.starsGroup.getChildCount(); j++) {
                        Chip chip2 = (Chip) binding.starsGroup.getChildAt(j);
                        chip2.setChecked(j <= checkedIndex);
                    }
                });
            }
        binding.submitBtn.setOnClickListener(view -> {
            FirebaseDatabase.getInstance().getReference("drivers/" + driverId).child("trips").setValue(ServerValue.increment(starIndex + 1))
                    .addOnCompleteListener(task -> {
                        Toast.makeText(requireActivity(), "Thanks for your efforts", Toast.LENGTH_SHORT).show();
                        behavior.hide();
                    });
        });
        behavior.addCallback(callback = new SideSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int i) {
                if (i == SideSheetBehavior.STATE_HIDDEN) {
                    getParentFragmentManager().beginTransaction().remove(RateDriverFragment.this).commit();
                }
            }

            @Override
            public void onSlide(@NonNull View view, float v) {
            }
        });
        binding.skipBtn.setOnClickListener(view -> {
            behavior.hide();

        });
        return binding.getRoot();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        behavior.removeCallback(callback);
    }
}

package com.ride.fragments;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.auth.User;
import com.cab.app.R;
import com.cab.app.databinding.RideHistoryItemBinding;
import com.cab.app.databinding.RideHistoryLayoutBinding;
import com.google.android.material.sidesheet.SideSheetBehavior;
import com.google.android.material.sidesheet.SideSheetCallback;
import com.google.firebase.database.FirebaseDatabase;
import com.ride.RideInstance;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RideHistoryFragment extends Fragment {
    private final String TAG = RideHistoryFragment.class.getSimpleName();

    private @NonNull RideHistoryLayoutBinding binding;
    private User user;
    private List<RideInstance> dataSet = new ArrayList<>();
    private SideSheetBehavior<FrameLayout> behavior;
    private SideSheetCallback callback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = RideHistoryLayoutBinding.inflate(inflater);
        behavior = SideSheetBehavior.from(((FrameLayout) container));
        behavior.addCallback(callback = new SideSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int i) {
                if (i == SideSheetBehavior.STATE_HIDDEN) {
                    getParentFragmentManager().beginTransaction().remove(RideHistoryFragment.this).commit();
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
        user = User.getInstance();
        init();
        return binding.getRoot();
    }

    private void init() {
        FirebaseDatabase.getInstance().getReference("trips").child(user.getFirebaseUser().getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.hasChildren()){
                        binding.noRidesFound.setVisibility(View.VISIBLE);
                        return;
                    }
                    snapshot.getChildren().forEach(dataSnapshot -> dataSet.add(dataSnapshot.getValue(RideInstance.class)));
                    showData();
                })
                .addOnFailureListener(e -> binding.noRidesFound.setVisibility(View.VISIBLE));
    }

    private void showData() {
        binding.recyclerView.setAdapter(new RecyclerView.Adapter<RideHistoryViewHolder>() {
            @NonNull
            @Override
            public RideHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                @NonNull RideHistoryItemBinding itemBinding = RideHistoryItemBinding.inflate(LayoutInflater.from(parent.getContext()));
                return new RideHistoryViewHolder(itemBinding.getRoot(), itemBinding);
            }

            @Override
            public void onBindViewHolder(@NonNull RideHistoryViewHolder holder, int position) {
                Log.e(TAG, "onBindViewHolder: " + dataSet.get(position));
                RideHistoryItemBinding binding = holder.getItemBinding();
                RideInstance instance = dataSet.get(position);
                binding.location.setText(instance.getLocation());
                binding.destination.setText(instance.getDestination());
                binding.fare.setText("₹ " + instance.getFare());
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(instance.getDate());
                DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
                binding.date.setText(formatter.format(calendar.getTime()) + " • " + RideInstance.getTime(instance.getEta(), false));
                if (instance.getStatus() == RideInstance.RIDE_CANCELED) {
                    binding.statusTxt.setText("Canceled");
                    Drawable drawable = AppCompatResources.getDrawable(requireActivity(), R.drawable.cancel);
                    drawable.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(requireActivity(), R.color.colorError), PorterDuff.Mode.SRC_IN));
                    binding.statusTxt.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
                    binding.statusTxt.setTextColor(requireActivity().getColor(R.color.colorError));
                } else if (instance.getStatus() > RideInstance.RIDE_FINISHED) {
                    binding.statusTxt.setText("Ongoing");
                    binding.statusTxt.setCompoundDrawablesRelative(AppCompatResources.getDrawable(requireActivity(), R.drawable.route), null, null, null);
                    Drawable drawable = AppCompatResources.getDrawable(requireActivity(), R.drawable.route);
                    drawable.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(requireActivity(), R.color.colorPrimary), PorterDuff.Mode.SRC_IN));
                    binding.statusTxt.setCompoundDrawablesWithIntrinsicBounds(drawable,null,null,null);
                    binding.statusTxt.setTextColor(requireActivity().getColor(R.color.colorPrimary));
                }
            }

            @Override
            public int getItemCount() {
                return dataSet.size();
            }
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        behavior.removeCallback(callback);
    }

    private class RideHistoryViewHolder extends RecyclerView.ViewHolder {
        private final com.cab.app.databinding.RideHistoryItemBinding itemBinding;

        public RideHistoryViewHolder(View itemView, RideHistoryItemBinding itemBinding) {
            super(itemView);
            this.itemBinding = itemBinding;
            itemView.post(() -> {
                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) itemView.getLayoutParams();
                params.topMargin = 20;
                itemView.setLayoutParams(params);
            });
        }

        public RideHistoryItemBinding getItemBinding() {
            return itemBinding;
        }
    }

}

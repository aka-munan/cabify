package com.auth.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.auth.Validation;
import com.auth.utils.Account;
import com.cab.app.R;
import com.cab.app.databinding.PaymentProfileBinding;
import com.google.android.material.sidesheet.SideSheetBehavior;
import com.google.android.material.sidesheet.SideSheetCallback;
import com.google.android.material.snackbar.Snackbar;

public class PaymentProfileSetupFragment extends Fragment {
    private PaymentProfileBinding binding;
    private SideSheetBehavior<FrameLayout> behavior;
    private SideSheetCallback callback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = PaymentProfileBinding.inflate(inflater, container, false);
        behavior = SideSheetBehavior.from(((FrameLayout) container));
        init();
        behavior.addCallback(callback=new SideSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int i) {
                if (i == SideSheetBehavior.STATE_HIDDEN) {
                    getParentFragmentManager().beginTransaction().remove(PaymentProfileSetupFragment.this).commit();
                }
            }

            @Override
            public void onSlide(@NonNull View view, float v) {
            }
        });
        //on back pressed
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                behavior.hide();
            }
        });
        return binding.getRoot();
    }

    private void init() {
        binding.toolbar.getMenu().findItem(R.id.done)
                .setOnMenuItemClickListener(item -> {
                    if (validate()) {
                        Account account = new Account(binding.username.getText().toString()
                                , binding.bankName.getText().toString()
                                , binding.acNumber.getText().toString()
                                , binding.ifsc.getText().toString());
                        //validate ande update db from server
                        Validation.updatePaymentDetails(getContext(),account).addOnCompleteListener(task->{
                            if (task.isSuccessful()){
                                Log.e("validation", "success: " );
                                Toast.makeText(getContext(), "You are now ready to accept Rides",Toast.LENGTH_LONG).show();
                                behavior.hide();
                            }else {
                                binding.toolbar.getMenu().findItem(R.id.done).setEnabled(true);
                                Toast.makeText(getContext(), "Request failed, Please try again later", Toast.LENGTH_LONG).show();
                                Log.e("validation", "error: " ,task.getException());
                            }
                        });
                    }
                    return true;
                });
    }

    private boolean validate() {
        if (!binding.username.getText().toString().matches("^[a-z,A-Z\\s]{9,25}$")) {
            binding.username.setError("Please enter a valid Name");
            return false;
        }
        if (!binding.acNumber.getText().toString().matches("^[0-9]{9,20}$")) {
            binding.acNumber.setError("Please enter a valid a/c number");
            return false;
        }
        if (!binding.bankName.getText().toString().matches("^[a-z,A-Z\\s]{10,30}$")) {
            binding.bankName.setError("Please enter a valid Bank Name");
            return false;
        }
        if (!binding.ifsc.getText().toString().matches("^[0-9,A-Z]{9,25}$")) {
            binding.ifsc.setError("Please enter a valid IFSC code");
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

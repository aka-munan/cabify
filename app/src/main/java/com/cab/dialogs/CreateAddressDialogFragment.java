package com.cab.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.cab.app.AddressManager;
import com.cab.app.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.HashMap;
import java.util.Map;

public class CreateAddressDialogFragment extends DialogFragment {

    private Button createBtn;
    private Button cancelBtn;
    private EditText nameTxt;
    private DialogInterface.OnDismissListener onDismissListener;

    @Override
    public void show(@NonNull FragmentManager manager, @Nullable String tag) {
        super.show(manager, tag);
        Dialog dialog = getDialog();

    }


    @SuppressLint("DialogFragmentCallbacksDetector")
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(getContext())
                .setView(R.layout.create_address_dialog)
                .create();
        dialog.setOnShowListener(dialogInterface -> {
            createBtn = dialog.findViewById(R.id.create);
            cancelBtn = dialog.findViewById(R.id.cancel);
            nameTxt = dialog.findViewById(R.id.name);
            dialog.setOnDismissListener(onDismissListener);
            init();
        });
        return dialog;
    }


    private void init() {
        createBtn.setOnClickListener(view -> {
            AddressManager addressManager = new AddressManager(getActivity().getApplicationContext());
            Map<String, String> address = new HashMap<>();
            address.put("name", nameTxt.getText().toString());
            address.put("country", "country =");
            address.put("streetAddr", "address");
            address.put("city", "city");
            address.put("pin", "pin");
            address.put("state", "state");
            addressManager.addAddress(address);
            onDismissListener.onDismiss(null);
            dismiss();
        });
        cancelBtn.setOnClickListener(view -> {
            dismiss();
        });
    }
    public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener){
        this.onDismissListener = onDismissListener;
    }
}

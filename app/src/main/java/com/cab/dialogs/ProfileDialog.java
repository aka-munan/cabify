package com.cab.dialogs;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.auth.User;
import com.auth.UserType;
import com.bumptech.glide.Glide;
import com.cab.app.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public abstract class ProfileDialog {
    private final AppCompatActivity activity;
    private User user;
    private View editProfile;
    private AlertDialog dialog;
    private View logOut;

    public ProfileDialog(AppCompatActivity activity, User user) {
        this.activity = activity;
        this.user = user;
    }

    public AlertDialog getDialog() {
        if (dialog != null)
            return dialog;
        dialog = new MaterialAlertDialogBuilder(activity)
                .setView(R.layout.profile_dialog)
                .create();
        dialog.setOnShowListener(dialogInterface -> {
            user.updateFromDatabase().addOnCompleteListener(task -> {
                user = User.getInstance();
                String[] genders = activity.getResources().getStringArray(R.array.genders);
                TextView nameTxt = dialog.findViewById(R.id.name);
                TextView emailTxt = dialog.findViewById(R.id.email);
                TextView genderTxt = dialog.findViewById(R.id.gender);
                TextView contactTxt = dialog.findViewById(R.id.contact);
                TextView countryCode = dialog.findViewById(R.id.country_code);
                TextView title = dialog.findViewById(R.id.title);
                Button rideHistoryBtn = dialog.findViewById(R.id.history_btn);
                logOut = dialog.findViewById(R.id.logout);
                editProfile = dialog.findViewById(R.id.edit_profile);
                Button registerDriverBtn = dialog.findViewById(R.id.register_driver);
                ImageView profileView = dialog.findViewById(R.id.profileView);
                rideHistoryBtn.setOnClickListener(view->onHistoryBtnClicked(dialog));
                editProfile.setOnClickListener(view -> onEditProfileClick(dialog, view));
                logOut.setOnClickListener(view -> onlogoutClick(dialog, view));
                title.setText("Hello ".concat(user.getUserType().equals(UserType.DRIVER) ? "Driver" : "Passenger"));
                if (user.getUserType().equals(UserType.PENDING_PAYMENT_PROFILE)) {
                    registerDriverBtn.setText("Set-up payment Profile");
                } else {
                    registerDriverBtn.setText(user.getUserType().equals(UserType.DRIVER) ? "View driver profile" : "Register as Driver");
                }
                registerDriverBtn.setOnClickListener(view -> onRegisterAsDriverClicked(dialog, user.getUserType()));
                Glide.with(activity)
                        .load(user.getProfileUrl())
                        .into(profileView);
                //display data
                nameTxt.setText(user.getUserName());
                emailTxt.setText(user.getEmail());
                contactTxt.setText(user.getContact());
                countryCode.setText(user.getCC());
                genderTxt.setText(genders[user.getGender()]);
            });
            dialog.setOnCancelListener(dialogInterface1 -> {

            });
        });
        return dialog;
    }

    public abstract void onEditProfileClick(AlertDialog dialog, View view);

    public abstract void onlogoutClick(AlertDialog dialog, View view);

    public abstract void onRegisterAsDriverClicked(AlertDialog dialog, UserType userType);
    public void onHistoryBtnClicked(AlertDialog dialog){

    }
}

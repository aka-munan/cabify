package com.auth.fragments;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.auth.DriverDetails;
import com.auth.User;
import com.auth.UserType;
import com.auth.Validation;
import com.bumptech.glide.Glide;
import com.cab.app.R;
import com.cab.app.databinding.RegisterAsDriverBinding;
import com.cab.utils.FileUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.sidesheet.SideSheetBehavior;
import com.google.android.material.sidesheet.SideSheetCallback;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Objects;


public class RegisterAsDriverFragment extends Fragment {
    private final String TAG = "RegisterAsDriver";
    private SideSheetBehavior<FrameLayout> behavior;

    private RegisterAsDriverBinding binding;
    private User currentUser, updatedUser;
    private TextView nameTxt, emailTxt, contactTxt, countryCode;
    private Spinner genderSelector;
    private ImageView profileView;
    private String[] genders;
    private DriverDetails driverDetails;
    private String key;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    private PickVisualMediaRequest imageOnlyRequest;
    private SideSheetCallback callback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        behavior = SideSheetBehavior.from(((FrameLayout) container));
        binding = RegisterAsDriverBinding.inflate(inflater, container, false);
        genders = getActivity().getResources().getStringArray(R.array.genders);
        nameTxt = binding.expandedPersonalInfo.findViewById(R.id.name);
        emailTxt = binding.expandedPersonalInfo.findViewById(R.id.email);
        genderSelector = binding.expandedPersonalInfo.findViewById(R.id.gender_selector);
        contactTxt = binding.expandedPersonalInfo.findViewById(R.id.contact);
        countryCode = binding.expandedPersonalInfo.findViewById(R.id.country_code_selector);
        profileView = binding.expandedPersonalInfo.findViewById(R.id.profileView);

        currentUser = User.getInstance();
        updatedUser = new User(currentUser.getUserName(), currentUser.getEmail(), String.valueOf(currentUser.getProfileUrl()), UserType.DRIVER);
        updatedUser.setContact(currentUser.getFullContact());
        updatedUser.setGender(currentUser.getGender());

        driverDetails = new DriverDetails();
        driverDetails.setUid(currentUser.getFirebaseUser().getUid());

        initPersonalInfoLayout();
        initLegalDocLayout();
        initVehicleInfoLayout();
        initConsentLayout();
        behavior.addCallback(callback=new SideSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int i) {
                if (i == SideSheetBehavior.STATE_HIDDEN) {
                    getParentFragmentManager().beginTransaction().remove(RegisterAsDriverFragment.this).commit();
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
        binding.toolbar.setNavigationOnClickListener(view->{
            Objects.requireNonNull(requireActivity()).getOnBackPressedDispatcher().onBackPressed();
        });
        imageOnlyRequest = new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build();
        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                uploadFile(uri).addOnCompleteListener(task -> {
                    String fileUri = task.getResult().toString();
                    driverDetails.put(key,fileUri);
                    switch (key){
                        case DriverDetails.KEY_DL_FRONT:
                            binding.drivingLicenceFront.setIconResource(R.drawable.check);
                            binding.drivingLicenceFront.setIconTint(ColorStateList.valueOf(Color.GREEN));
                            break;
                        case DriverDetails.KEY_DL_BACK:
                            binding.drivingLicenceBack.setIconResource(R.drawable.check);
                            binding.drivingLicenceBack.setIconTint(ColorStateList.valueOf(Color.GREEN));
                            break;
                        case DriverDetails.KEY_AADHAR_FRONT:
                            binding.aadharFront.setIconResource(R.drawable.check);
                            binding.aadharFront.setIconTint(ColorStateList.valueOf(Color.GREEN));
                            break;
                        case DriverDetails.KEY_AADHAR_BACK:
                            binding.aadharBack.setIconResource(R.drawable.check);
                            binding.aadharBack.setIconTint(ColorStateList.valueOf(Color.GREEN));
                            break;
                        case DriverDetails.KEY_RC_URL:
                            binding.rcPhotoBtn.setIconResource(R.drawable.check);
                            binding.rcPhotoBtn.setIconTint(ColorStateList.valueOf(Color.GREEN));
                            break;
                    }
                });
            } else {
                Log.d("PhotoPicker", "No media selected");
            }
        });
        return binding.getRoot();
    }

    private void initPersonalInfoLayout() {
        binding.personalInfoBtn.setOnClickListener(view -> {
            TransitionManager.beginDelayedTransition(binding.parent);
            binding.expandedPersonalInfo.setVisibility(binding.expandedPersonalInfo.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            ((MaterialButton) view).setIconResource(binding.expandedPersonalInfo.getVisibility() == View.VISIBLE ? R.drawable.arrow_drop_up : R.drawable.arrow_drop_down);
        });
        binding.pInfoContinueBtn.setOnClickListener(view -> {
            if (!isPersonalInfoFilled()) {
                Snackbar.make(view, "Please fill all the above fields", Snackbar.LENGTH_SHORT).setAnchorView(binding.snackBar).show();
                return;
            }
            binding.personalInfoBtn.setIconResource(R.drawable.arrow_drop_down);
            binding.expandedPersonalInfo.setVisibility(View.GONE);
            binding.legalDocBtn.performClick();
        });

        binding.dobSelector.setOnClickListener(view -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext());
            datePickerDialog.setOnDateSetListener((datePicker, year, month, day) -> {
                // datePicker.
                String date = String.format("%02d/%02d/%04d", day, month, year);
                binding.dobSelector.setText(date);
            });
            datePickerDialog.show();
        });
        //display contents
        Glide.with(getActivity()).load(currentUser.getProfileUrl()).into(profileView);
        nameTxt.setText(currentUser.getUserName());
        emailTxt.setText(currentUser.getEmail());
        contactTxt.setText(currentUser.getContact());
        countryCode.setText(currentUser.getCC());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.simple_textview, genders);
        adapter.setDropDownViewResource(R.layout.drop_down);
        genderSelector.setAdapter(adapter);
        genderSelector.setSelection(currentUser.getGender(), true);
        ///pick location
        binding.addressBtn.setOnClickListener(view -> {


        });

    }

    private void initLegalDocLayout() {
        binding.legalDocBtn.setOnClickListener(view -> {
            TransitionManager.beginDelayedTransition(binding.parent);
            binding.legalDocLayout.setVisibility(binding.legalDocLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            ((MaterialButton) view).setIconResource(binding.legalDocLayout.getVisibility() == View.VISIBLE ? R.drawable.arrow_drop_up : R.drawable.arrow_drop_down);
        });
        binding.legalDocContinueBtn.setOnClickListener(view -> {
            if (!areLegalDocsUploaded()) {
                Snackbar.make(view, "Please fill all the above fields", Snackbar.LENGTH_SHORT).setAnchorView(binding.snackBar).show();
                return;
            }
            binding.legalDocLayout.setVisibility(View.GONE);
            binding.vehicleInfoBtn.performClick();

        });
        binding.drivingLicenceFront.setOnClickListener(view -> {
            key=DriverDetails.KEY_DL_FRONT;
            pickMedia.launch(imageOnlyRequest);
        });
        binding.drivingLicenceBack.setOnClickListener(view -> {
            key=DriverDetails.KEY_DL_BACK;
            pickMedia.launch(imageOnlyRequest);
        });
        binding.aadharBack.setOnClickListener(view -> {
            key=DriverDetails.KEY_AADHAR_BACK;
            pickMedia.launch(imageOnlyRequest);
        });
        binding.aadharFront.setOnClickListener(view -> {
            key=DriverDetails.KEY_AADHAR_FRONT;
            pickMedia.launch(imageOnlyRequest);
        });
    }

    private void initVehicleInfoLayout() {
        binding.vehicleInfoBtn.setOnClickListener(view -> {
            TransitionManager.beginDelayedTransition(binding.parent);
            binding.vehicleInfoLayout.setVisibility(binding.vehicleInfoLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            ((MaterialButton) view).setIconResource(binding.vehicleInfoLayout.getVisibility() == View.VISIBLE ? R.drawable.arrow_drop_up : R.drawable.arrow_drop_down);
        });
        binding.vInfoContinueBtn.setOnClickListener(view -> {
            if (!isVehicleInfoFilled()) {
                Snackbar.make(view, "Please fill all the above fields", Snackbar.LENGTH_SHORT).setAnchorView(binding.snackBar).show();
                return;
            }
            binding.vehicleInfoLayout.setVisibility(View.GONE);
            binding.consentBtn.performClick();
        });
        binding.rcPhotoBtn.setOnClickListener(view -> {
            key=DriverDetails.KEY_RC_URL;
            pickMedia.launch(imageOnlyRequest);
        });
    }

    private void initConsentLayout() {
        binding.consentBtn.setOnClickListener(view -> {
            TransitionManager.beginDelayedTransition(binding.parent);
            binding.consentLayout.setVisibility(binding.consentLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            ((MaterialButton) view).setIconResource(binding.consentLayout.getVisibility() == View.VISIBLE ? R.drawable.arrow_drop_up : R.drawable.arrow_drop_down);
        });
        binding.agreeContinueBtn.setOnClickListener(view -> {
            if (!isVehicleInfoFilled() || isPersonalInfoFilled() || areLegalDocsUploaded()) {
                Snackbar.make(view, "Please fill all the above fields", Snackbar.LENGTH_SHORT).setAnchorView(binding.snackBar).show();
                return;
            }
            Validation.validateFromServer(getContext(),driverDetails).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    getParentFragmentManager().beginTransaction().replace(R.id.fragment_container,new PaymentProfileSetupFragment()).commit();
                } else {
                    Log.e(TAG, "register user as driver: ", task.getException());
                    Snackbar.make(view, "Failed to get you registered", Snackbar.LENGTH_SHORT).setAnchorView(binding.snackBar).show();
                }
            });
        });
    }

    public boolean isPersonalInfoFilled() {
        updatedUser.setUserName(nameTxt.getText().toString());
        updatedUser.setGender(genderSelector.getSelectedItemPosition());
        updatedUser.setContact(countryCode.getText().toString() + ":" + contactTxt.getText().toString());
        driverDetails.setDob(binding.dobSelector.getText().toString());
        driverDetails.setAddress(binding.addressBtn.getText().toString());
        boolean isPersonalInfoNotFilled = updatedUser.getUserName().trim().isEmpty() || updatedUser.getContact().trim().length() < 10 || driverDetails.getDob().isEmpty() || driverDetails.getAddress().replaceAll("\\D", "").isEmpty();
        return !isPersonalInfoNotFilled;
    }

    public boolean isVehicleInfoFilled() {
        driverDetails.setRegistration(binding.registrationNo.getText().toString());
        return driverDetails.isRCUploaded() || driverDetails.getRegistration().trim().isEmpty();
    }

    private boolean areLegalDocsUploaded() {
        return driverDetails.isAddharUploaded() || driverDetails.isDlUploaded();
    }

    private Task<Uri> uploadFile(Uri uri) {
        try {
            Bitmap bitmap = FileUtils.getBitmapFromUri(getContext(), uri);
            byte[] imageBytes = FileUtils.getBytesFromBitmap(bitmap, 40);
            StorageReference ref = FirebaseStorage.getInstance().getReference("drivers").child(currentUser.getFirebaseUser().getUid() + "/" + key + ".jpg");

            UploadTask uploadTask = ref.putBytes(imageBytes);

            Task<Uri> urlTask = uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "then: ", task.getException());
                }
                // Continue with the task to get the download URL
                return ref.getDownloadUrl();
            });
            return urlTask;
        } catch (Exception e) {
        }
        return null;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        behavior.removeCallback(callback);
    }
}

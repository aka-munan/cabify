package com.auth;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.DrawableUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cab.app.AddressManager;
import com.cab.app.AddressViewAdapter;
import com.cab.app.R;
import com.cab.dialogs.CreateAddressDialogFragment;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EditProfileActivity extends AppCompatActivity {

    private final String TAG = "EditProfileActivity";
    private AppCompatSpinner genderSelector;
    private View newAddress;
    private ImageView profileView;
    private EditText nameTxt, contactTxt;
    MaterialAutoCompleteTextView countryCodeView;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    private RecyclerView addressRecyclerView;
    private User currentUser, updatedUser;
    private Uri profileUpdated;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_profile);

        genderSelector = findViewById(R.id.gender_selector);
        newAddress = findViewById(R.id.new_address);
        profileView = findViewById(R.id.profileView);
        nameTxt = findViewById(R.id.name);
        TextView emailTxt = findViewById(R.id.email);
        contactTxt = findViewById(R.id.contact);
        countryCodeView = findViewById(R.id.country_code_selector);
        addressRecyclerView = findViewById(R.id.address_recycler);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        toolbar.getMenu().findItem(R.id.done).setOnMenuItemClickListener(menuItem -> {
            //push updated things to database
            pushUpdate();
            return true;
        });

        toolbar.setNavigationOnClickListener(view -> finish());

        currentUser = User.getInstance();

        nameTxt.setText(currentUser.getUserName());
        emailTxt.setText(currentUser.getEmail());
        contactTxt.setText(currentUser.getContact() != null ? currentUser.getContact() : "");
        countryCodeView.setText(currentUser.getCC()!=null?currentUser.getCC():"");
        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                profileView.setImageURI(uri);
                profileUpdated = uri;
            } else {
                Log.d("PhotoPicker", "No media selected");
            }
        });

        init();
    }

    private void pushUpdate() {
        updatedUser = new User();
        updatedUser.setUserType(currentUser.getUserType());
        String userName = nameTxt.getText().toString();
        int gender = genderSelector.getSelectedItemPosition();
        String contact = countryCodeView.getText().toString() + ":" + contactTxt.getText().toString();

        UserProfileChangeRequest.Builder changeRequestBuilder = new UserProfileChangeRequest.Builder();
        if (!userName.equals(currentUser.getUserName())) {
            changeRequestBuilder.setDisplayName(userName);
            updatedUser.setUserName(userName);
        }
        if (gender != currentUser.getGender())
            updatedUser.setGender(gender);

        if (!contact.equals(currentUser.getContact()))
            updatedUser.setContact(contact);

        if (profileUpdated != null)
            changeRequestBuilder.setPhotoUri(profileUpdated);


        if (changeRequestBuilder.getDisplayName() != null
                || changeRequestBuilder.getPhotoUri() != null) {
            if (changeRequestBuilder.getPhotoUri() != null) {
                BitmapDrawable bitmapDrawable = ((BitmapDrawable) profileView.getDrawable());
                Bitmap bitmap = bitmapDrawable.getBitmap();
                Log.i(TAG, "pushUpdate: " + bitmap.getHeight() + "x" + bitmap.getWidth());
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.WEBP, 10, stream);
                StorageReference ref = FirebaseStorage.getInstance().getReference("users")
                        .child(currentUser.getFirebaseUser().getUid() + "/profile.jpg");
                UploadTask uploadTask = ref.putBytes(stream.toByteArray());
                Task<Uri> urlTask = uploadTask.continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "then: ", task.getException());
                    }

                    // Continue with the task to get the download URL
                    return ref.getDownloadUrl();
                });
                urlTask.addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        updatedUser.setProfileUrl(task.getResult().toString());
                        changeRequestBuilder.setPhotoUri(task.getResult());
                    }

                    else Log.e(TAG, "pushUpdate: ", task.getException());
                    updateFirebaseAuth(changeRequestBuilder);
                });
            } else updateFirebaseAuth(changeRequestBuilder);


        } else pushToDatabase();

    }

    private void updateFirebaseAuth(UserProfileChangeRequest.Builder changeRequestBuilder) {
        currentUser.getFirebaseUser().updateProfile(changeRequestBuilder.build()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                pushToDatabase();
            } else
                Log.e(TAG, "pushUpdate: ", task.getException());
        });
    }

    private void pushToDatabase() {
        if (!updatedUser.toMap().isEmpty()) {
            FirebaseDatabase.getInstance().getReference("users")
                    .child(currentUser.getFirebaseUser().getUid())
                    .updateChildren(updatedUser.toMap());
        }
        finish();
    }

    private void init() {
        countryCodeView.setOnItemClickListener((adapterView, view, i, l) -> {
            String item = String.valueOf(adapterView.getItemAtPosition(i));
            int startIndex = item.indexOf("+");
            int endIndex = item.indexOf(")");
            String code = item.substring(startIndex, endIndex);
            countryCodeView.setText(code);
            Toast.makeText(EditProfileActivity.this, "" + code, Toast.LENGTH_SHORT).show();

        });

        Glide.with(this).load(currentUser.getProfileUrl())
                .into(profileView);
        String[] genders = getResources().getStringArray(R.array.genders);


        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.simple_textview, genders);
        adapter.setDropDownViewResource(R.layout.drop_down);
        genderSelector.setAdapter(adapter);
        genderSelector.setSelection(currentUser.getGender(), true);
        //add new address
        newAddress.setOnClickListener(view -> {
            CreateAddressDialogFragment dialogFragment = new CreateAddressDialogFragment();
            dialogFragment.setOnDismissListener(dialogInterface -> ((AddressViewAdapter) addressRecyclerView.getAdapter()).updateData());
            dialogFragment.show(getSupportFragmentManager(), "dialog");

        });
        //profile select
        profileView.setOnClickListener(view -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });
        setRecyclerAdapter(addressRecyclerView);
        addressRecyclerView.setLayoutManager(new LinearLayoutManager(EditProfileActivity.this));
    }

    private void setRecyclerAdapter(RecyclerView recyclerView) {
        AddressManager addressManager = new AddressManager(getApplicationContext());
        List<Map<String, String>> addresses = addressManager.getAddresses();
        AddressViewAdapter adapter =
                new AddressViewAdapter(addresses);
        adapter.setAddressManager(addressManager);
        recyclerView.setAdapter(adapter);
    }
}

package com.cab.bottomSheets;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.cab.app.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.slider.Slider;

import java.util.HashMap;
import java.util.Map;

public class BockRideBottomSheet extends BottomSheetDialogFragment {
    public static final String Tag = "ModelBottomSheet";
    private ViewGroup rootView;
    private View bottomSheetView, pickupLocationBtn, dropLocationBtn;
    private MaterialCardView selectedTaxiType;
    private ActivityResultLauncher<String> permissionRequest;
    private boolean waitForPickupLocation;
    private View.OnClickListener locationPickClick;
    private Button bookBtn;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            return rootView;
        }
        rootView = (ViewGroup) inflater.inflate(R.layout.bottom_sheet, container, false);
        //bottomSheetView = rootView.findViewById(R.id.bottom_sheet);
        pickupLocationBtn = rootView.findViewById(R.id.pickup_locationBtn);
        dropLocationBtn = rootView.findViewById(R.id.destination_locationBtn);
         bookBtn = rootView.findViewById(R.id.book_btn);
      //  bottomSheetView.post(initBottomSheet());
        init();
        return rootView;
    }

    private void init() {
        permissionRequest = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                if (waitForPickupLocation) {
                    pickupLocationBtn.performClick();
                } else
                    dropLocationBtn.performClick();
            }
        });
        Slider passengerSlider = rootView.findViewById(R.id.passengerSlider);
        Button addPassenger = rootView.findViewById(R.id.addPassenger);
        TextView passengerNo = rootView.findViewById(R.id.passengerNo);
        addPassenger.setOnClickListener(view -> passengerSlider.setValue(
                passengerSlider.getValue() < passengerSlider.getValueTo() ?
                        passengerSlider.getValue() + 1 : passengerSlider.getValueTo()));
        passengerSlider.addOnChangeListener((slider, v, fromUser) -> {
            passengerNo.setText(String.valueOf((int) v));
        });
        pickupLocationBtn.setOnClickListener(view -> {
            if (!hasLocationPermission()) {
                askLocationPermission();
                return;
            }
            locationPickClick.onClick(view);
            waitForPickupLocation = true;

        });
        dropLocationBtn.setOnClickListener(view -> {
            if (!hasLocationPermission()) {
                askLocationPermission();
                return;
            }
            waitForPickupLocation = false;
            locationPickClick.onClick(view);
        });

    }

    private Runnable initBottomSheet() {
        return () -> {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            if (dialog == null)
                return;
            //add type of taxi items
            ViewGroup taxiItems = rootView.findViewById(R.id.taxi_type_items);
            for (int i = 1; i <= 3; i++) {
                MaterialCardView view = (MaterialCardView) getLayoutInflater().inflate(R.layout.type_taxi_layout, taxiItems, false);
                TextView taxiName = view.findViewById(R.id.taxiName);
                TextView taxiPrice = view.findViewById(R.id.taxiPrice);
                view.setOnClickListener((view2) -> {
                    if (selectedTaxiType != null && selectedTaxiType.isChecked())
                        selectedTaxiType.setChecked(false);
                    view.setChecked(!view.isChecked());
                    selectedTaxiType = (MaterialCardView) view2;

                });
                ImageView icon = view.findViewById(R.id.taxi_icon);
                if (i == 2) {
                    taxiName.setText(R.string.motor_bike);
                    icon.setImageResource(R.drawable.bike);
                } else if (i == 3) {
                    taxiName.setText(R.string.electric_car);
                    icon.setImageResource(R.drawable.electric_car);
                } else {
                    taxiName.setText(R.string.car);
                }
                taxiItems.addView(view);
            }
            selectedTaxiType = (MaterialCardView) taxiItems.getChildAt(0);
            selectedTaxiType.setChecked(true);
        };
    }

    public void locationPicked(Location location) {
        Toast.makeText(getContext(), location.toString(), Toast.LENGTH_SHORT).show();
    }

    public void setLocationPickClickListener(View.OnClickListener listener) {

        this.locationPickClick = listener;
    }

    private void askLocationPermission() {
        //  LocationManager
        permissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

}

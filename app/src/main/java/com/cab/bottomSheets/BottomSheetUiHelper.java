package com.cab.bottomSheets;

import android.Manifest;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.auth.User;
import com.bumptech.glide.Glide;
import com.cab.app.MainActivity;
import com.cab.app.R;
import com.cab.app.databinding.BottomSheetBinding;
import com.cab.app.databinding.RideFoundBottomSheetBinding;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.ride.RideInstance;
import com.ride.utils.LocationHelper;
import com.ride.utils.RideHelper;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BottomSheetUiHelper {
    private static final String TAG = BottomSheetUiHelper.class.getSimpleName();
    private final MainActivity activity;
    private View.OnClickListener locationPickClickListener;
    private boolean waitForPickupLocation, isExpanded = false;
    private Button findDriverBtn;
    List<TaxiType> taxiTypes = new ArrayList<>();
    private String pickupLocation = "", dropLocation = "";
    private Map<String, Long> prices;
    private ViewGroup parent;
    private BottomSheetBinding bookRideLayoutBinding;
    private RideFoundBottomSheetBinding confirmRideLayoutBinding;
    private float baseRideCost;
    private JSONObject route;
    private String dropLocationName, pickupLocationName;

    public BottomSheetUiHelper(MainActivity activity) {
        this.activity = activity;
    }

    public void init(ViewGroup parent, BottomSheetBinding binding) {
        this.parent = parent;
        this.bookRideLayoutBinding = binding;
        findDriverBtn = parent.findViewById(R.id.book_btn);
        confirmRideLayoutBinding = binding.confirmRide;
        setBtnClickListeners();
        displayTaxiTypes();
    }

    private void displayTaxiTypes() {
        //add type of taxi items
        getPricesFromDb().addOnCompleteListener(task -> {
            prices = (Map<String, Long>) task.getResult().getValue();
            ViewGroup taxiItems = bookRideLayoutBinding.taxiTypeItems;
            for (TaxiType taxi : TaxiType.values()) {
                if (taxi == TaxiType.ALL) continue;
                MaterialCardView taxiItem = (MaterialCardView) activity.getLayoutInflater().inflate(R.layout.type_taxi_layout, taxiItems, false);
                TextView taxiName = taxiItem.findViewById(R.id.taxiName);
                TextView taxiPrice = taxiItem.findViewById(R.id.taxiPrice);
                ImageView icon = taxiItem.findViewById(R.id.taxi_icon);

                taxiPrice.setText(String.valueOf(prices.get(taxi.toString())) + "₹/km");
                switch (taxi) {
                    case CAR:
                        taxiName.setText(R.string.car);
                        break;
                    case BIKE:
                        taxiName.setText(R.string.motor_bike);
                        icon.setImageResource(R.drawable.bike);
                        break;
                    case ELECTRIC_CAR:
                        taxiName.setText(R.string.electric_car);
                        icon.setImageResource(R.drawable.electric_car);
                        break;
                }
                taxiItem.setOnClickListener(card -> {
                    if (taxiItem.isChecked()) taxiTypes.remove(taxi);
                    else taxiTypes.add(taxi);
                    taxiItem.setChecked(!taxiItem.isChecked());
                });
                taxiItems.addView(taxiItem);
            }
        });
    }

    private void setBtnClickListeners() {
        findDriverBtn.setOnClickListener(view -> {
            expandBottomSheet();
        });

        bookRideLayoutBinding.bookBtn.setOnClickListener(view -> {
            view.setEnabled(false);
            //bookRide()
            showConfirmRideLayout(view);
        });
        bookRideLayoutBinding.pickupLocationBtn.setOnClickListener(view -> {
            if (!LocationHelper.hasLocationPermission(activity)) {
                activity.resultCallback = (isGranted) -> {
                    if (isGranted) {
                        bookRideLayoutBinding.pickupLocationBtn.performClick();
                    }
                };
                activity.resultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                return;
            }
            locationPickClickListener.onClick(view);
            waitForPickupLocation = true;

        });
        bookRideLayoutBinding.destinationLocationBtn.setOnClickListener(view -> {
            if (!LocationHelper.hasLocationPermission(activity)) {
                activity.resultCallback = (isGranted) -> {
                    if (isGranted) {
                        bookRideLayoutBinding.destinationLocationBtn.performClick();
                    }
                };
                activity.resultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                return;
            }
            waitForPickupLocation = false;
            locationPickClickListener.onClick(view);
        });
        //slider for no. of passenger increment

        bookRideLayoutBinding.addPassenger.setOnClickListener(view -> {
            bookRideLayoutBinding.passengerSlider.setValue(
                    bookRideLayoutBinding.passengerSlider.getValue() < bookRideLayoutBinding.passengerSlider.getValueTo() ?
                            bookRideLayoutBinding.passengerSlider.getValue() + 1 : bookRideLayoutBinding.passengerSlider.getValueTo());
        });
        bookRideLayoutBinding.passengerSlider.addOnChangeListener((slider, v, fromUser) -> {
            bookRideLayoutBinding.passengerNo.setText(String.valueOf((int) v));
        });
    }

    private void showConfirmRideLayout(View view) {
        RideHelper.getRoute(pickupLocation, dropLocation).addOnCompleteListener(task -> {
            view.setEnabled(true);
            if (!task.isSuccessful()) {
                Log.w(TAG, "getRoute: ", task.getException());
                Toast.makeText(activity, "Please try again later", Toast.LENGTH_SHORT).show();
                return;
            }
            try {

                route = task.getResult().getJSONArray("routes").getJSONObject(0).getJSONArray("sections").getJSONObject(0).getJSONObject("summary");
                Log.i(TAG, "locationPicked: route " + route);
                float distance = (float) route.getLong("length") / 1000;
                if (distance < 3 || distance > 60)
                    throw new Exception("Ride is too long or too short");
                baseRideCost = prices.get("CAR") * ((float) route.getLong("length") / 1000);
                TransitionManager.beginDelayedTransition(parent);
                bookRideLayoutBinding.bookRideLayout.setVisibility(View.GONE);
                bookRideLayoutBinding.confirmRideLayout.setVisibility(View.VISIBLE);
                confirmRideLayoutBinding.acceptBtn.setVisibility(View.GONE);
                confirmRideLayoutBinding.rejectBtn.setVisibility(View.GONE);
                confirmRideLayoutBinding.dragHandle.setVisibility(View.GONE);
                initConfirmRideLayout(distance);
            } catch (Exception e) {
                Toast.makeText(activity, e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.w(TAG, "getRoute: ", e);
            }
        });


    }

    public void hideConfirmRideLayout() {
        TransitionManager.beginDelayedTransition(parent);
        bookRideLayoutBinding.bookRideLayout.setVisibility(View.VISIBLE);
        bookRideLayoutBinding.confirmRideLayout.setVisibility(View.GONE);
        bookRideLayoutBinding.bookBtn.setOnClickListener(view -> {
            view.setEnabled(false);
            //bookRide()
            showConfirmRideLayout(view);
        });
        bookRideLayoutBinding.bookBtn.setText("Book Ride");
    }

    private void initConfirmRideLayout(float distance) throws Exception {
        User currentUser = User.getInstance();
        confirmRideLayoutBinding.title.setText("Confirm ride");
        confirmRideLayoutBinding.uname.setHint(currentUser.getUserName() + " • ");
        float totalRideCost = baseRideCost + (bookRideLayoutBinding.passengerSlider.getValue() - 1) * (baseRideCost / 2);
        confirmRideLayoutBinding.fare.setText(Math.round(totalRideCost) + " ₹");
        confirmRideLayoutBinding.distance.setText(Math.round(distance) + "km Ride");
        confirmRideLayoutBinding.passengers.setText(String.valueOf((int) bookRideLayoutBinding.passengerSlider.getValue()));
        confirmRideLayoutBinding.eta.setText(RideInstance.getTime(route.getLong("duration"), false));
        confirmRideLayoutBinding.pickUp.setText(pickupLocationName);
        confirmRideLayoutBinding.destination.setText(dropLocationName);
        Glide.with(confirmRideLayoutBinding.profile)
                .load(currentUser.getProfileUrl()).into(confirmRideLayoutBinding.profile);
        bookRideLayoutBinding.bookBtn.setText("Confirm Ride");
        bookRideLayoutBinding.bookBtn.setOnClickListener((view) -> bookRide());
        bookRideLayoutBinding.cancelBtn.setOnClickListener(view -> hideConfirmRideLayout());
    }

    public void expandBottomSheet() {
        findDriverBtn.setVisibility(View.GONE);
        TransitionManager.beginDelayedTransition(parent);
        bookRideLayoutBinding.getRoot().setVisibility(View.VISIBLE);
        isExpanded = true;
    }

    public void collapseBottomSheet() {
        findDriverBtn.setVisibility(View.VISIBLE);
        bookRideLayoutBinding.getRoot().setVisibility(View.GONE);
        TransitionManager.beginDelayedTransition(parent);
        isExpanded = false;
        parent.requestLayout();
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    private void bookRide() {
        if (pickupLocation.isEmpty() || dropLocation.isEmpty()) {
            Toast.makeText(activity, pickupLocation.isEmpty() ? "Please Select pick-up location" : "Please select drop off location", Toast.LENGTH_LONG).show();
            return;
        }
        bookRideLayoutBinding.bookBtn.setEnabled(false);
        Map<String, Object> data = new HashMap<>();
        data.put("location", pickupLocation);
        data.put("destination", dropLocation);
        data.put("passengers", bookRideLayoutBinding.passengerSlider.getValue());
        data.put("taxiType", taxiTypes.isEmpty() ? TaxiType.ALL.name() : taxiTypes.toString());
        JSONObject jsonData = new JSONObject(data);
        RideHelper.bookRide(findDriverBtn.getContext(), jsonData.toString()).addOnCompleteListener(task -> {
            bookRideLayoutBinding.bookBtn.setEnabled(true);
            try {
                if (!task.isSuccessful()) {
                    Toast.makeText(findDriverBtn.getContext(), "Failure occurred: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                hideConfirmRideLayout();
                collapseBottomSheet();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Task<DataSnapshot> getPricesFromDb() {
        return FirebaseDatabase.getInstance().getReference("prices").get();
    }


    public void locationPicked(Map<String, String> data) {
        if (waitForPickupLocation) {
            pickupLocation = data.get("coords");
            pickupLocationName = data.get("name");
            bookRideLayoutBinding.currentLocationTxt.setText(pickupLocationName);
            waitForPickupLocation = !waitForPickupLocation;
        } else {
            dropLocation = data.get("coords");
            dropLocationName = data.get("name");
            bookRideLayoutBinding.dropLocationTxt.setText(dropLocationName);
        }
        if (pickupLocation==null||dropLocation==null)
            return;
        activity.drawRoute(pickupLocation, dropLocation);
    }


    public void setLocationPickClickListener(View.OnClickListener locationPickClickListener) {
        this.locationPickClickListener = locationPickClickListener;
    }


}

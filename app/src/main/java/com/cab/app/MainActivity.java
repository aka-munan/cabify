package com.cab.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.service.notification.StatusBarNotification;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.webkit.WebViewAssetLoader;

import com.airbnb.lottie.LottieAnimationView;
import com.auth.EditProfileActivity;
import com.auth.LoginActivity;
import com.auth.User;
import com.auth.UserType;
import com.auth.fragments.PaymentProfileSetupFragment;
import com.auth.fragments.RegisterAsDriverFragment;
import com.auth.fragments.ViewDriverProfileFragment;
import com.bumptech.glide.Glide;
import com.cab.app.databinding.BottomSheetBinding;
import com.cab.bottomSheets.BottomSheetUiHelper;
import com.cab.bottomSheets.RideFoundFragment;
import com.cab.dialogs.ProfileDialog;
import com.cab.welcome.WelcomeActivity;
import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;
import com.google.android.material.sidesheet.SideSheetBehavior;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.ride.RideInstance;
import com.ride.SuggestionAdapter;
import com.ride.fragments.AcceptRidesFragment;
import com.ride.fragments.ActiveRideFragment;
import com.ride.fragments.RateDriverFragment;
import com.ride.fragments.RideHistoryFragment;
import com.ride.fragments.RideStatusFragment;
import com.ride.utils.LocationHelper;
import com.ride.utils.ResultCallback;
import com.web.LoadWebViewClient;
import com.web.WebInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private FirebaseAuth firebaseAuth;
    private Button bookBtn;
    private SearchBar searchBar;
    private SearchView searchView;
    private ImageView profileView;
    private User user;
    private Button myLocationBtn;
    private Button pickOnMap;
    private BottomSheetUiHelper bottomSheetUiHelper;
    private FrameLayout fragmentContainer;
    private WebView webView;
    private RecyclerView suggestionRecycler;
    private SuggestionAdapter suggestionAdapter;
    public ResultCallback<Boolean> resultCallback;
    public ActivityResultLauncher<String> resultLauncher;
    public View snacbar;
    private ViewGroup floatingView;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        resultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (resultCallback != null)
                resultCallback.onResult(isGranted);
            resultCallback = null;
        });
        setContentView(R.layout.activity_main);
        bookBtn = findViewById(R.id.book_btn);
        firebaseAuth = FirebaseAuth.getInstance();
        searchBar = findViewById(R.id.search_bar);
        profileView = findViewById(R.id.profileView);
        searchView = findViewById(R.id.search_view);
        myLocationBtn = findViewById(R.id.my_location);
        pickOnMap = findViewById(R.id.pick_on_map);
        fragmentContainer = findViewById(R.id.fragment_container);
        webView = findViewById(R.id.web_view);
        suggestionRecycler = findViewById(R.id.suggestion_recycler);
        snacbar = findViewById(R.id.snack_bar);
        pickOnMap.setOnClickListener(view -> {
            if (bottomSheetUiHelper.isExpanded())
                bottomSheetUiHelper.collapseBottomSheet();
            searchView.hide();
        });
        floatingView = findViewById(R.id.floating_layout);
        if (savedInstanceState != null)
            return;
        if (isLoggedIn()) {
            user = User.getInstance();
            init();
        } else {
            //proceed to welcome/login activity
            launchPage(isNewUser() ? WelcomeActivity.class : LoginActivity.class);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        ((ViewGroup) webView.getParent()).removeView(webView);
        webView.destroy();
        super.onDestroy();
    }


    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        Log.i(TAG, "onNewIntent: " + intent.getExtras());
        if (intent.hasExtra("type") && intent.getStringExtra("type").equals("passengerFound")) {
            String title = intent.getStringExtra("title");
            String tripId = intent.getStringExtra("tripId");
            String passengerUid = intent.getStringExtra("uid");
            new RideFoundFragment(title, tripId, passengerUid).show(getSupportFragmentManager(), "rideFoundFragment");
        }
    }

    private void showFloatingView(RideInstance rideInstance, ViewGroup floatingView) {
        floatingView.setVisibility(View.VISIBLE);
        Button expandBtn = floatingView.findViewById(R.id.expand_btn);
        View expandedLayout = floatingView.findViewById(R.id.expanded_content);
        View background = floatingView.findViewById(R.id.elevated_background);
        TextView fare = floatingView.findViewById(R.id.fare);
        TextView distance = floatingView.findViewById(R.id.distance);
        fare.setText(rideInstance.getFare() + "₹");
        distance.setText(rideInstance.getDistance() / 1000 + " km");
        Handler handler = new Handler(getMainLooper());
        Runnable runnable = () -> hideFloatingView(floatingView);
        handler.postDelayed(runnable, 5000);
        expandBtn.setOnClickListener((view) -> {
            handler.removeCallbacks(runnable);
            TransitionManager.beginDelayedTransition(floatingView);
            view.setVisibility(View.GONE);
            expandedLayout.setVisibility(View.VISIBLE);
            background.animate().alpha(1).setDuration(300).start();
            background.setOnClickListener((view2) -> {
                hideFloatingView(floatingView);
                view.setOnClickListener(null);
            });
            Button continueBtn = floatingView.findViewById(R.id.continue_btn);
            LottieAnimationView animView = floatingView.findViewById(R.id.lottie_view);
            animView.playAnimation();
            continueBtn.setOnClickListener(v -> background.performClick());
        });
    }

    private void hideFloatingView(ViewGroup floatingView) {
        Button expandBtn = floatingView.findViewById(R.id.expand_btn);
        View expandedLayout = floatingView.findViewById(R.id.expanded_content);
        View background = floatingView.findViewById(R.id.elevated_background);
        LottieAnimationView animView = floatingView.findViewById(R.id.lottie_view);
        animView.cancelAnimation();
        TransitionManager.beginDelayedTransition(floatingView);
        expandBtn.setVisibility(View.VISIBLE);
        expandedLayout.setVisibility(View.GONE);
        floatingView.animate().alpha(0).withEndAction(() -> {
            floatingView.setAlpha(1);
            background.setAlpha(0);
            floatingView.setVisibility(View.GONE);
        }).setDuration(300).start();
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void loadMap() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebInterface(this), "Android");
        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        webView.setWebViewClient(new LoadWebViewClient(assetLoader));
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
        webView.setOnTouchListener((view, event) -> {
            if (bottomSheetUiHelper.isExpanded()) {
                bottomSheetUiHelper.collapseBottomSheet();
                return true;
            }
            return false;
        });
    }

    private void init() {
        //showFragment(new RateDriverFragment("EjhO62tDvgVF1mjBmeOGoDsXexJ3"));
        loadMap();
        if (LocationHelper.hasLocationPermission(this))
            getLastLocation();

        myLocationBtn.setOnClickListener(view -> {
            if (!LocationHelper.hasLocationPermission(this)) {
                resultCallback = (isGranted) -> {
                    if (isGranted) {
                        LocationHelper.getSingleUpdate(this, location -> {
                            LocationHelper.displayAddress(MainActivity.this, location.getLatitude() + "," + location.getLongitude());
                            searchView.hide();
                        });
                    }
                };
                resultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                return;
            }
            LocationHelper.getSingleUpdate(this, location -> {
                LocationHelper.displayAddress(MainActivity.this, location.getLatitude() + "," + location.getLongitude());
                searchView.hide();
            });
        });


        ViewGroup bottomSheet = findViewById(R.id.standard_bottom_sheet);
        //prevent passing touch to map view
        bottomSheet.setOnTouchListener((v, event) -> true);
        bottomSheetUiHelper = new BottomSheetUiHelper(this);
        bottomSheetUiHelper.init(bottomSheet, BottomSheetBinding.bind(bottomSheet.findViewById(R.id.expanded_bottom_sheet)));
        bottomSheetUiHelper.setLocationPickClickListener(view -> searchBar.performClick());
        loadProfile(profileView);
        profileView.setOnClickListener(view -> {
            //on click profile
            ProfileDialog profileDialog = new ProfileDialog(MainActivity.this, user) {
                @Override
                public void onEditProfileClick(AlertDialog dialog, View view) {
                    launchPage(EditProfileActivity.class);
                    dialog.dismiss();
                }

                @Override
                public void onlogoutClick(AlertDialog dialog, View view) {
                    firebaseAuth.signOut();
                    launchPage(LoginActivity.class);
                    finish();
                }

                @Override
                public void onRegisterAsDriverClicked(AlertDialog dialog, UserType userType) {
                    Fragment fragment = null;
                    switch (userType) {
                        case PENDING_PAYMENT_PROFILE:
                            fragment = new PaymentProfileSetupFragment();
                            break;
                        case DRIVER:
                            fragment = new ViewDriverProfileFragment();
                            break;
                        default:
                            fragment = new RegisterAsDriverFragment();
                    }
                    showFragment(fragment);
                    dialog.dismiss();
                }

                @Override
                public void onHistoryBtnClicked(AlertDialog dialog) {
                    showFragment(new RideHistoryFragment());
                    dialog.dismiss();
                }
            };
            AlertDialog dialog = profileDialog.getDialog();
            dialog.show();
        });
        suggestionRecycler.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        suggestionAdapter = new SuggestionAdapter(MainActivity.this, new ArrayList<>());
        suggestionRecycler.setAdapter(suggestionAdapter);
        searchView.getEditText().addTextChangedListener(new TextWatcher() {
            Handler handler = new Handler(getMainLooper());
            Runnable getSuggestionsRunnable;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                handler.removeCallbacks(getSuggestionsRunnable);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().isEmpty())
                    return;
                getSuggestionsRunnable = () -> {
                    LocationHelper.getSuggestions(LocationHelper.getMyLastLocation(), s.toString().trim()).addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.e(TAG, "getSuggestions: ", task.getException());
                            return;
                        }
                        try {
                            List<Map<String, String>> data = new ArrayList<>();
                            final JSONArray array = (JSONArray) task.getResult().get("items");
                            for (int i = 0; i < array.length(); i++) {
                                final JSONObject map = (JSONObject) array.get(i);
                                if (!map.has("position"))
                                    return;
                                Map<String, String> item = new HashMap<>();
                                item.put("name", map.getString("title"));
                                JSONObject pos = map.getJSONObject("position");
                                item.put("coords", pos.get("lat") + "," + pos.get("lng"));
                                data.add(item);
                            }
                            suggestionAdapter.updateData(data);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    });
                };
                handler.removeCallbacks(getSuggestionsRunnable);
                handler.postDelayed(getSuggestionsRunnable, 250);
            }
        });
        //listen for accepted rides//for driver only
        Runnable listenForRidesRunnable = () -> {
            Log.i(TAG, "onDataChange: listening for active ride");
            user.listenForRides(new ValueEventListener() {
                final ActiveRideFragment activeRideFragment = new ActiveRideFragment(MainActivity.this);

                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Log.i(TAG, "onDataChange: " + snapshot.getValue());
                    SideSheetBehavior<FrameLayout> behavior = SideSheetBehavior.from(fragmentContainer);
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String tripId = child.getKey();
                        Map<String, Object> rideData = (Map<String, Object>) child.getValue();
                        int status = Integer.parseInt(String.valueOf(rideData.get("status")));
                        activeRideFragment.statusChanged(tripId, (String) rideData.get("passengerUid"), status);
                        if (!activeRideFragment.isVisible()) {
                            LifecycleEventObserver lifecycleObserver = new LifecycleEventObserver() {
                                @Override
                                public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner, @NonNull Lifecycle.Event event) {
                                    if (event != Lifecycle.Event.ON_START)
                                        return;
                                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, activeRideFragment, ActiveRideFragment.class.getSimpleName())
                                            .commit();
                                    behavior.expand();
                                    getLifecycle().removeObserver(this);
                                }
                            };
                            if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                                lifecycleObserver.onStateChanged(MainActivity.this, Lifecycle.Event.ON_START);
                            } else
                                getLifecycle().addObserver(lifecycleObserver);
                        }
                    }
                    if (snapshot.getValue() == null/*ride finished*/) {
                        if (activeRideFragment.isVisible()) {
                            behavior.hide();
                            getSupportFragmentManager().beginTransaction().remove(activeRideFragment);
                        }
                        if (activeRideFragment.isAdded())
                            showFloatingView(activeRideFragment.getRideInstance(), floatingView);
                    }
                }


                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "onCancelled: ", error.toException());
                }
            });
        };
        //check if user has any active ride
        Runnable pendingRunnable = () ->
                user.getActiveRides(new ValueEventListener() {
                    RideStatusFragment rideStatusFragment;
                    RideInstance rideInstance;
                    String tripId;

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.getValue() == null && rideStatusFragment != null && rideStatusFragment.isVisible()) {//ride finished
                            SideSheetBehavior<FrameLayout> behavior = SideSheetBehavior.from(fragmentContainer);
                            showFragment(new RateDriverFragment(rideInstance.getDriverUid()));
                            behavior.expand();
                            getSupportFragmentManager().beginTransaction().remove(rideStatusFragment).commit();
                            bookBtn.setOnClickListener(view -> {
                                bottomSheetUiHelper.expandBottomSheet();
                            });
                            bookBtn.setText("Book Ride");
                            listenForRidesRunnable.run();
                            return;
                        }
                            //check if user is driver nad show accept rides fragment
                            if (!user.isUpdatedFromDb()) {
                                user.updateFromDatabase().addOnCompleteListener(task -> {
                                    if (!task.isSuccessful()) {
                                        return;
                                    }
                                    user = User.getInstance();
                                    if (user.getUserType() == UserType.DRIVER) {
                                        //  showAcceptRidesFragment();
                                        listenForRidesRunnable.run();
                                    }
                                });
                            } else {
                                listenForRidesRunnable.run();
                            }
                        for (DataSnapshot child : snapshot.getChildren()) {//ride found
                            tripId = child.getKey();
                            rideInstance = (child.getValue(RideInstance.class));
                        }
                        if (rideStatusFragment != null && rideStatusFragment.isVisible()) {
                            rideStatusFragment.updateRide(rideInstance);
                            return;
                        }
                        //update ui
                        bookBtn.setOnClickListener(view -> {
                            if (!rideStatusFragment.isVisible())
                                rideStatusFragment.show(getSupportFragmentManager(), "rideStatusFragment");
                        });
                        bookBtn.setText("View Ride Info");
                        LifecycleEventObserver lifecycleObserver = (lifecycleOwner, event) -> {
                            if (event == Lifecycle.Event.ON_START) {
                                rideStatusFragment.show(getSupportFragmentManager(), "rideStatusFragment");
                                rideStatusFragment.updateRide(rideInstance);
                            }
                        };
                        if (rideStatusFragment == null)
                            rideStatusFragment = new RideStatusFragment(tripId, rideInstance);
                        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                            lifecycleObserver.onStateChanged(MainActivity.this, Lifecycle.Event.ON_START);
                        } else
                            getLifecycle().addObserver(lifecycleObserver);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "onCancelled: ",error.toException() );
                    }
                });
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
            pendingRunnable.run();
        else getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner, @NonNull Lifecycle.Event event) {
                if (event != Lifecycle.Event.ON_START)
                    return;
                pendingRunnable.run();
                getLifecycle().removeObserver(this);
            }
        });
    }

    private void showAcceptRidesFragment() {

        StatusBarNotification[] notifications = getSystemService(NotificationManager.class).getActiveNotifications();
        for (StatusBarNotification notification : notifications) {
            if (notification.getId() == AcceptRidesFragment.NOTIFICATION_ID) {
                return;
            }
        }
        Runnable pendingRunnable = () -> showFragment(new AcceptRidesFragment(MainActivity.this));
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
            pendingRunnable.run();
        else
            getLifecycle().addObserver(new LifecycleEventObserver() {
                @Override
                public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner, @NonNull Lifecycle.Event event) {
                    if (event == Lifecycle.Event.ON_START) {
                        pendingRunnable.run();
                        getLifecycle().removeObserver(this);
                    }
                }
            });
    }

    private void showFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment, fragment.getClass().getSimpleName());
        transaction.commit();
        SideSheetBehavior<FrameLayout> behavior = SideSheetBehavior.from(fragmentContainer);
        behavior.expand();
    }

    private void getLastLocation() {
        LocationHelper.getSingleUpdate(this, location -> {
            loadMap();
        });
    }

    public void locationPicked(Map<String, String> data) {
        if (!bottomSheetUiHelper.isExpanded()) {
            bottomSheetUiHelper.expandBottomSheet();
        }
        setMapCenter(data.get("coords"));
        bottomSheetUiHelper.locationPicked(data);
        searchView.hide();
    }

    private void setMapCenter(String center) {
        if (center.isEmpty())
            return;
        String jsCode = "setCenter({lat:" + center.replaceAll(",", ",lng:") + "});";
        webView.evaluateJavascript(jsCode, null);
    }

    public void drawRoute(String location, String destination) {
        String jsCode = "calculateRouteFromAtoB('" + location + "','" + destination + "','#2854C5',true,true);";
        webView.evaluateJavascript(jsCode, null);
    }

    public void setOriginToWebView() {
        getMainExecutor().execute(() -> setMapCenter(LocationHelper.getMyLastLocation()));
    }

    public void loadProfile(ImageView imageView) {
        Glide.with(this)
                .load(user.getProfileUrl())
                .into(imageView);
    }

    private void launchPage(Class page) {
        Intent intent = new Intent();
        intent.setClass(this, page);
        startActivity(intent);
    }

    private boolean isLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    private boolean isNewUser() {
        SharedPreferences preferences = getApplication().getSharedPreferences("user", MODE_PRIVATE);
        return preferences.getBoolean("isNewUser", true);
    }
}
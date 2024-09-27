package com.auth;

import android.net.Uri;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class User {
    private static User userInstance;
    private UserType userType;
    private FirebaseUser firebaseUser;
    private String userName;
    private final String email;
    private String contact;
    private String profileUrl;
    private int gender = 0;//male default
    private Map<String,String> additionalDetails;
    private boolean updatedFromDb;

    public User() {
        this(null, null, null,UserType.PASSENGER);
    }

    public User(String userName, String email, String profileUrl, UserType userType) {
        this(userName, email, profileUrl,userType ,0, null);

    }

    public User(String userName, String email, String profileUrl,UserType userType, int gender, String contact) {
        this(userName, email, profileUrl,userType ,gender, contact,null);
    }
    public User(String userName, String email, String profileUrl,UserType userType, int gender, String contact,Map<String,String> additionalDetails) {
        this.userName = userName;
        this.email = email;
        this.profileUrl = profileUrl;
        this.userType = userType;
        this.gender = gender;
        this.contact = contact;
        this.additionalDetails = additionalDetails;
        this.firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @Exclude
    public static User getInstance() {
        if (userInstance == null) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user==null)
                return userInstance=new User();
            userInstance = new User(user.getDisplayName(), user.getEmail(), String.valueOf(user.getPhotoUrl()),UserType.PASSENGER);
        }
        return userInstance;
    }

    @Exclude
    public Task<DataSnapshot> updateFromDatabase() {
        return FirebaseDatabase.getInstance().getReference("users").child(getFirebaseUser().getUid())
                .get().addOnSuccessListener(task -> {
                    userInstance = task.getValue(User.class);
                    updatedFromDb=true;
                });
    }

    @Exclude
    public Task<DataSnapshot> listenFromDatabase() {
        return FirebaseDatabase.getInstance().getReference("users").child(getFirebaseUser().getUid())
                .get().addOnSuccessListener(task -> userInstance = task.getValue(User.class));
    }

    public Task<DataSnapshot> getDriverDetails(){
        if (userType!=UserType.DRIVER)
            return null;
        return FirebaseDatabase.getInstance().getReference("drivers").child(getFirebaseUser().getUid())
                .get();
    }
    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> userData = new HashMap<>();
        if (userName != null)
            userData.put("userName", userName);
        if (email != null)
            userData.put("email", email);
        if (profileUrl != null)
            userData.put("profileUrl", profileUrl);
        if (userInstance != null && gender != userInstance.getGender())
            userData.put("gender", gender);
        if (userInstance != null && userType != userInstance.getUserType())
            userData.put("userType",userType);
        if (contact != null)
            userData.put("contact", contact);
        return userData;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public String getContact() {
        if (contact==null)
            return null;
        int separateIndex = contact.indexOf(":")+1;
        return contact.substring(separateIndex);
    }
    public String getFullContact() {
        if (contact==null)
            return null;
        return contact;
    }

    public String getCC() {
        if (contact==null)
            return null;
        int separateIndex = contact.indexOf(":");
        return contact.substring(0, separateIndex);
    }
    public void setContact(String contact) {
        this.contact = contact;
    }

    public Uri getProfileUrl() {
        return Uri.parse(profileUrl);
    }

    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    public String getEmail() {
        return email;
    }

    public FirebaseUser getFirebaseUser() {
        if (firebaseUser==null)
            firebaseUser= FirebaseAuth.getInstance().getCurrentUser();
        return firebaseUser;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType=userType;
    }

    public void getActiveRides(ValueEventListener listener){
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("trips").child(getFirebaseUser().getUid());
        Query ridesQuery = userRef.orderByChild("status").startAt(0);
        ridesQuery.addValueEventListener(listener);
    }
    public void listenForRides(ValueEventListener listener){
        if (getUserType()!=UserType.DRIVER)
            return;
        DatabaseReference ridesRef = FirebaseDatabase.getInstance().getReference("drivers").child(getFirebaseUser().getUid()).child("trips");
        Query ridesQuery = ridesRef.orderByChild("status").startAt(0);
        ridesQuery.addValueEventListener(listener);
    }
    public boolean isUpdatedFromDb(){
        return updatedFromDb;
    }

    public Map<String, String> getAdditionalDetails() {
        return additionalDetails;
    }
}

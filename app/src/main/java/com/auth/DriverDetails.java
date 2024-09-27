package com.auth;

import com.cab.bottomSheets.TaxiType;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;

@IgnoreExtraProperties
public class DriverDetails extends HashMap<String, Object> {
    public static final String KEY_DL_FRONT = "dlFront", KEY_DL_BACK = "dlBack", KEY_AADHAR_BACK = "aadharBack", KEY_AADHAR_FRONT = "aadharFront", KEY_RC_URL = "rcurl";
    public DriverDetails() {
    }

    public void setUid(String uid) {
        this.put("uid", uid);
    }

    private void setDLFront(String url) {
        this.put(KEY_DL_FRONT, url);
    }

    public void setDLBack(String url) {
        this.put(KEY_DL_BACK, url);
    }

    public void setAadharBack(String url) {
        this.put(KEY_AADHAR_BACK, url);
    }

    public void setAadharFront(String url) {
        this.put(KEY_AADHAR_FRONT, url);
    }

    public void setRcUrl(String url) {
        this.put(KEY_RC_URL, url);
    }
    public boolean isAddharUploaded() {
        return get("aadharBack") == null
                || get("aadharFront") == null;
    }

    public boolean isDlUploaded() {
        return get("dlFront") == null
                || get("dlBack") == null;
    }

    public boolean isRCUploaded() {
        return get("rcUrl") == null;
    }


    public String getDob() {
        return (String) get("dob");
    }

    public void setDob(String dob) {
        put("dob", dob);
    }

    public String getAddress() {
        return (String) get("address");
    }

    public void setAddress(String address) {
        put("address", address);
    }

    public TaxiType getTaxiType() {
        return (TaxiType) this.get("taxiType");
    }

    public void setTaxiType(TaxiType taxiType) {
        this.put("taxiType", taxiType);
    }
    public String getVehicleName() {
        return (String) get("vehicleName");
    }
    public void setVehicleName(String vehicleName) {
        this.put("vehicleName", vehicleName);
    }
    public String getRegistration() {
        return (String) get("registration");
    }
    public void setRegistration(String registration) {
        this.put("registration", registration);
    }

}

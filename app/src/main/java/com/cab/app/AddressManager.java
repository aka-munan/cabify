package com.cab.app;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.common.util.JsonUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AddressManager {
    private final SharedPreferences preferences;
    private List<Map<String, String>> addresses;
    private final Context context;

    public AddressManager(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences("address", Context.MODE_PRIVATE);
    }

    public List<Map<String, String>> getAddresses() {
        List<Map<String, String>> addresses = new ArrayList<>();
        Set<String> addressSet = preferences.getStringSet("addresses", null);
        if( addressSet == null)
            return null;
        for (String json : addressSet) {
            try {
                addresses.add(jsonToMap(json));
            }catch (JSONException e){
                throw new RuntimeException(e);
            }
        }
        return addresses;
    }

    public void addAddress(Map<String, String> address) {
        SharedPreferences.Editor editor = preferences.edit();
        String addressStr = new JSONObject(address).toString();
        Set<String> addressSet = preferences.getStringSet("addresses", null);
        if (addressSet!=null){
            Set<String> newAddresses = new HashSet<>(addressSet);
            newAddresses.add(addressStr);
            editor.putStringSet("addresses", newAddresses).apply();
            return;
        }
        addressSet = new HashSet<>();
        addressSet.add(addressStr);
        editor.putStringSet("addresses", addressSet).apply();
    }
    private Map<String,String> jsonToMap(String json ) throws JSONException {
        JSONObject jsonObject =new JSONObject(json);
        Map<String,String> map = new HashMap<>();
        for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
            String key = it.next();
            map.put(key, jsonObject.getString(key));
        }
        return map;
    }
    public void removeAddress(Map<String, String> address){
        Set<String> addressSet = preferences.getStringSet("addresses", null);
        Set<String> newAddresses = new HashSet<>(addressSet);
        newAddresses.remove(new JSONObject(address).toString());
        preferences.edit().putStringSet("addresses", newAddresses).apply();
    }
}

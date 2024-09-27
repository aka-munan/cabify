package com.auth.utils;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Account {
    private String bankName, userName,acNumber,ifsc;
    public Account(){
    }
    public Account(String userName,String bankName, String acNumber,String ifsc){

        this.userName = userName;
        this.bankName = bankName;
        this.acNumber = acNumber;
        this.ifsc = ifsc;
    }
    public String getIfsc() {
        return ifsc;
    }

    public void setIfsc(String ifsc) {
        this.ifsc = ifsc;
    }

    public String getAcNumber() {
        return acNumber;
    }

    public void setAcNumber(String acNumber) {
        this.acNumber = acNumber;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }
    public String toJson(){
        JSONObject jsonObject = new JSONObject(toMap());
        return jsonObject.toString();
    }
    private Map<String,String> toMap(){
        Map<String, String> map = new HashMap<>();
        map.put("userName", userName);
        map.put("bankName", bankName);
        map.put("acNumber", acNumber);
        map.put("ifsc", ifsc);
        return map;
    }
}

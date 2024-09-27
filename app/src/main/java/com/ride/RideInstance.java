package com.ride;

import com.google.firebase.database.IgnoreExtraProperties;

import java.text.SimpleDateFormat;
import java.util.Calendar;
@IgnoreExtraProperties
public class RideInstance {
    private long date;
    private String location;
    private String destination;
    private long distance;
    private int eta;
    private int passengers;
    private int status;
    private int fare;
    private String driverUid;
    public static final int RIDE_REQUESTED= 0;
    public static final int DRIVER_ASSIGNED = 1;
    public static final int CONFIRM_RIDE=2;
    public static final int RIDE_STARTED=3;
    public static final int RIDE_FINISHED=-1;
    public static final int RIDE_CANCELED=-2;


    public RideInstance(){

    }
    public RideInstance(long date,String location,String destination,long distance, int eta, int passengers,int status,int fare,String driverUid){
        this.date = date;
        this.location = location;
        this.destination = destination;
        this.distance = distance;
        this.eta = eta;
        this.passengers = passengers;
        this.status = status;
        this.fare = fare;
        this.driverUid = driverUid;
    }

    public long getDate() {
        return date;
    }
    public void setDate(long date){
        this.date = date;
    }
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public long getDistance() {
        return distance;
    }

    public void setDistance(long distance) {
        this.distance = distance;
    }

    public int getEta() {
        return eta;
    }

    public void setEta(int eta) {
        this.eta = eta;
    }

    public int getPassengers() {
        return passengers;
    }

    public void setPassengers(int passengers) {
        this.passengers = passengers;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getFare() {
        return fare;
    }

    public void setFare(int fare) {
        this.fare = fare;
    }

    public static String getTime(long seconds,boolean returnExact) {
        if (seconds < 60)
            return returnExact?"1 min": "less then a minute";
        if (seconds < 60 * 60)
            return seconds / 60 + " mins";
        if (seconds < 60 * 60 * 24) {
            int hours = (int) (seconds / 3600);
            int mins = (int) (seconds % 3600);
            return hours + " hr " + getTime(mins,true);
        }

        return null;
    }
    public static String getDate(long milliSeconds, String dateFormat)
    {
        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }

    public String getDriverUid() {
        return driverUid;
    }
}

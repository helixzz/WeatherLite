package com.example.shinianer.goodweather.util;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.widget.Toast;

/**
 * Created by Shinianer on 2017/5/15.
 */

public class LocationHelper {
    Context mContext;
    private LocationManager locationManager;
    private String locationProvider;

    public LocationHelper(Context mContext) {
        this.mContext = mContext;
    }

    public Location getLocation() {
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        // 获得最好的定位效果
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(false);
        // 使用省电模式
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        //String provider = locationManager.getBestProvider(criteria, true);
        String provider = LocationManager.NETWORK_PROVIDER;


        if (locationManager.isProviderEnabled(provider)) {
            Location location = locationManager.getLastKnownLocation(provider);
            //Toast.makeText(mContext, "Provider" + provider + "is enabled.", Toast.LENGTH_SHORT).show();
            if (location != null) {
                //Toast.makeText(mContext, "location is not null", Toast.LENGTH_SHORT).show();
                double lon = location.getLongitude();
                //Toast.makeText(mContext, "Longitude: " + lon, Toast.LENGTH_SHORT).show();
                double lat = location.getLatitude();
                //Toast.makeText(mContext, "Latitude: " + lat, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, "location is null!!!!", Toast.LENGTH_SHORT).show();
            }

            return location;
        } else {
            Toast.makeText(mContext, "GPS Provider not available.", Toast.LENGTH_SHORT).show();
            return null;
        }

    }


}


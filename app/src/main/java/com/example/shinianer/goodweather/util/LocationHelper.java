package com.example.shinianer.goodweather.util;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.google.gson.Gson;

/**
 * Created by Shinianer on 2017/5/15.
 */

public class LocationHelper {
    Context mContext;
    private LocationManager locationManager;
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
        // 使用省电模式
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        // 查询最合适的 Provider
        String provider = locationManager.getBestProvider(criteria, true);
        // 或者直接使用网络定位
        //String provider = LocationManager.NETWORK_PROVIDER;

        if (locationManager.isProviderEnabled(provider)) {
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                // return TODO;
                Toast.makeText(mContext, "未能调用定位。请检查定位权限是否开启，或是否处于飞行模式。", Toast.LENGTH_LONG).show();
            }
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                storeLastKnownLocation(location);
                return location;
            } else {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    storeLastKnownLocation(location);
                    return location;
                } else {
                    location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                    if (location != null) {
                        storeLastKnownLocation(location);
                        return location;
                    } else {
                        Toast.makeText(mContext, "获取实时位置失败。", Toast.LENGTH_LONG).show();
                        return null;
                    }
                }
            }
        } else {
            Toast.makeText(mContext, "系统定位功能未开启。", Toast.LENGTH_LONG).show();
            return null;
        }

    }

    public void storeLastKnownLocation(Location location) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        String jsonLocation = new Gson().toJson(location);
        editor.putString("mLastLocation", jsonLocation);
        editor.apply();
    }

    public Location getLastKnownLocation() {
        Location location = getLocation();
        if (location != null) {
            return location;
        } else {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            Location storedLocation = null;
            String strLastLocation = sharedPreferences.getString("mLastLocation", null);
            if (strLastLocation != null) {
                if (!strLastLocation.equals(""))
                    storedLocation = new Gson().fromJson(strLastLocation, Location.class);
            }
            if (storedLocation != null) {
                Toast.makeText(mContext, "已读取之前保存的定位信息。", Toast.LENGTH_LONG).show();
                return storedLocation;
            } else {
                Toast.makeText(mContext, "未能读取之前保存的定位信息。", Toast.LENGTH_LONG).show();
                return null;
            }
        }
    }

    public void addLocationListener(LocationListener listener) {
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(mContext, "未能调用定位。请检查定位权限是否开启，或是否处于飞行模式。", Toast.LENGTH_LONG).show();
            return;
        }
        Criteria criteria = new Criteria();
        // 获得最好的定位效果
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        // 使用省电模式
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        // 查询最合适的 Provider
        String provider = locationManager.getBestProvider(criteria, true);
        locationManager.requestLocationUpdates(provider, 60000, 1, listener);
    }

    public void removeLocationListener(LocationListener listener) {
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(mContext, "未能调用定位。请检查定位权限是否开启，或是否处于飞行模式。", Toast.LENGTH_LONG).show();
            return;
        }
        locationManager.removeUpdates(listener);
    }

    public void updateLocation() {
        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                storeLastKnownLocation(location);
                removeLocationListener(this);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        this.addLocationListener(listener);
    }
}


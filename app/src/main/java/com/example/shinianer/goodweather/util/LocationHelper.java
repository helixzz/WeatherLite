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
import android.renderscript.Double2;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.example.shinianer.goodweather.MainActivity;
import com.google.gson.Gson;

/**
 * Created by Shinianer on 2017/5/15.
 */

public class LocationHelper extends MainActivity {
    Context mContext;
    private LocationManager locationManager;

    public LocationHelper(Context mContext) {
        this.mContext = mContext;
    }

    public Location getLocation() {
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        // 检查 App 是否具备定位权限。如果未具备，提示用户，并尝试索取权限。
        if (ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(mContext, "请检查定位权限是否开启，或是否处于飞行模式。", Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission_group.LOCATION}, 124);
        }

        // 枚举三种 Location Provider，从 GPS 开始逐个检查是否开启，并尝试获取位置。
        // 如果没有开启或获取不到位置，则继续尝试下一个，直到全部试完，发出提示并返回空值。
        String[] providers = new String[]{LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER};
        for (String provider : providers) {
            if (locationManager.isProviderEnabled(provider)) {
                Location location = locationManager.getLastKnownLocation(provider);
                if (location != null) {
                    // 成功获取到位置后，存入 SharedPreferences。
                    storeLastKnownLocation(location);
                    return location;
                }
            }
        }
        Toast.makeText(mContext, "获取实时位置失败。", Toast.LENGTH_LONG).show();
        return null;
    }

    public void storeLastKnownLocation(Location location) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (location == null) {
        } else {
            sharedPreferences.edit().putString("LOCATION_LAT", String.valueOf(location.getLatitude())).apply();
            sharedPreferences.edit().putString("LOCATION_LON", String.valueOf(location.getLongitude())).apply();
            sharedPreferences.edit().putString("LOCATION_PROVIDER", location.getProvider()).apply();
        }
        sharedPreferences.edit().apply();
    }

    public Location getLastKnownLocation() {
        Location location = getLocation();
        if (location != null) {
            return location;
        } else {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            Location storedLocation = null;
            String lat = sharedPreferences.getString("LOCATION_LAT", null);
            String lon = sharedPreferences.getString("LOCATION_LON", null);
            String provider = sharedPreferences.getString("LOCATION_PROVIDER", null);
            if (lat != null && lon != null && provider != null) {
                storedLocation = new Location(LocationManager.PASSIVE_PROVIDER);
                storedLocation.setLatitude(Double.parseDouble(lat));
                storedLocation.setLongitude(Double.parseDouble(lon));
                Toast.makeText(mContext, "已读取之前保存的定位信息。", Toast.LENGTH_LONG).show();
            }
            return storedLocation;
        }
    }

    public void addLocationListener(LocationListener listener) {
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        // 检查 App 是否具备定位权限。如果未具备，提示用户，并尝试索取权限。
        if (ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(mContext, "请检查定位权限是否开启，或是否处于飞行模式。", Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission_group.LOCATION}, 124);
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
        if (provider != null) {
            locationManager.requestLocationUpdates(provider, 60000, 1, listener);
        }
        else {
            Toast.makeText(mContext, "请检查定位权限是否开启，或是否处于飞行模式。", Toast.LENGTH_LONG).show();
        }

    }

    public void removeLocationListener(LocationListener listener) {
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        // 检查 App 是否具备定位权限。如果未具备，提示用户，并尝试索取权限。
        if (ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(mContext, "请检查定位权限是否开启，或是否处于飞行模式。", Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission_group.LOCATION}, 124);
            return;
        }

        locationManager.removeUpdates(listener);
    }

    public void updateLocation() {
        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // 当定位信息更新时
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


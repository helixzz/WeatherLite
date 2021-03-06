package com.example.shinianer.goodweather.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Shinianer on 2017/4/20.
 */

public class Weather {
    public String status;
    public Basic basic;
    public AQI aqi;
    public Now now;
    public Suggestion suggestion;
    @SerializedName("daily_forecast")
    public List<Forecast> forecastList;
}

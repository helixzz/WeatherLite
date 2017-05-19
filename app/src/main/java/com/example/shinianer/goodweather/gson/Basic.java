package com.example.shinianer.goodweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Shinianer on 2017/4/20.
 */

public class Basic {
    @SerializedName("city")
    public String cityName;

    @SerializedName("id")
    public String cityId;

    public Update update;

    public class Update {
        @SerializedName("loc")
        public String updateTime;
    }
}

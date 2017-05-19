package com.example.shinianer.goodweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Shinianer on 2017/4/20.
 */

public class Now {
    @SerializedName("tmp")
    public String temperature;

    @SerializedName("cond")
    public More more;

    public class More {
        @SerializedName("txt")
        public String info;
    }
}

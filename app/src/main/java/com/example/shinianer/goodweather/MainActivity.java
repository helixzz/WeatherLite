package com.example.shinianer.goodweather;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.shinianer.goodweather.gson.Forecast;
import com.example.shinianer.goodweather.gson.Weather;
import com.example.shinianer.goodweather.util.HttpUtil;
import com.example.shinianer.goodweather.util.LocationHelper;
import com.example.shinianer.goodweather.util.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    protected SwipeRefreshLayout swipeRefresh;
    protected DrawerLayout drawerLayout;
    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private ImageView bingPicImg;
    private String mCityId;
    private String mLocatingMethod;
    private Button navButton;

    public static String getCityIDByResponseText(String responseText) {
        if (responseText != null) {
            Looper.prepare();
            try {
                // Ugly Code.
                String cityID = "";
                JSONObject obj = new JSONObject(responseText);
                JSONArray arr1 = obj.getJSONArray("HeWeather5");
                JSONObject obj2 = arr1.getJSONObject(0);
                JSONObject obj3 = obj2.getJSONObject("basic");
                cityID = obj3.getString("id");
                return cityID;
            } catch (JSONException e) {
            }
            Looper.loop();
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_main);
        //初始化各控件
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        titleCity = (TextView) findViewById(R.id.title_city);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        aqiText = (TextView) findViewById(R.id.aqi_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        comfortText = (TextView) findViewById(R.id.comfort_text);
        carWashText = (TextView) findViewById(R.id.car_wash_text);
        sportText = (TextView) findViewById(R.id.sport_text);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navButton = (Button) findViewById(R.id.nav_button);

        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);

        String cityID = "";
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String weatherString = prefs.getString("weather", null);
        if (weatherString != null) {
            //有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            mCityId = weather.basic.cityId;
            showWeatherInfo(weather);
        } else {
            //无缓存时去服务器查询天气

            // 得到获取位置的方式（自动定位或用户手选）
            mCityId = prefs.getString("mCityId", "auto");
            if (mCityId.equals("auto")) {
                // Automatic acquire user's location.

            } else {
                // Allow user to select city manually.
                weatherLayout.setVisibility(View.INVISIBLE);
                // mCityId = getIntent().getStringExtra("cityID");
                // Save user's choice to settings cache.
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
                editor.putString("mCityId", mCityId);
                editor.apply();

            }
            if (cityID != "") {
                // 定位成功
                mCityId = cityID;
            } else {
                // 定位失败，手工选择城市
                weatherLayout.setVisibility(View.INVISIBLE);
                // mCityId = getIntent().getStringExtra("mCityId");

            }
            // 使用 mWeatherId 查询天气，并刷新天气界面
            requestWeather(mCityId);
        }

        // 下拉刷新监听器
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                mCityId = prefs.getString("mCityId", "auto");
                Toast.makeText(MainActivity.this, "正在刷新天气信息，当前城市 ID：" + mCityId, Toast.LENGTH_SHORT).show();
                requestWeather(mCityId);
            }
        });
        //获取必应每日一图初始化
        bingPicImg = (ImageView) findViewById(R.id.bing_pic_img);
        String bingPic = prefs.getString("bing_pic", null);
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();
        }

        //调用openDrawer()打开滑动菜单；
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

    }

    //根据天气id请求城市天气信息
    public void requestWeather(String weatherId) {

        if (weatherId.equals("auto")) {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
            editor.putString("mCityId", weatherId);
            editor.apply();
            LocationHelper locationHelper = new LocationHelper(this);
            Location location = locationHelper.getLocation();
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                getCityInfoByLocation(longitude, latitude);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                weatherId = prefs.getString("autoLocatedCityID", null);
                Toast.makeText(this, "自动定位成功，已获取到城市 ID：" + weatherId, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "未能通过自动定位获取城市，请尝试手动选择城市。", Toast.LENGTH_SHORT).show();
            }
        } else {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
            editor.putString("mCityId", weatherId);
            editor.apply();
        }
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=beb135e90b2b44c280a7ecc5a310d98c";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "从接口获取天气信息失败，请重试。", Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
                loadBingPic();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null) {
                            if (weather.status.equals("ok")) {
                                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
                                editor.putString("weather", responseText);
                                editor.apply();
                                showWeatherInfo(weather);
                                Toast.makeText(MainActivity.this, "获取天气信息成功。", Toast.LENGTH_SHORT).show();
                                swipeRefresh.setRefreshing(false);
                            } else {
                                Toast.makeText(MainActivity.this, "从接口获取天气信息失败，请重试。", Toast.LENGTH_SHORT).show();
                                swipeRefresh.setRefreshing(false);
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "从接口获取天气信息失败，请重试。", Toast.LENGTH_SHORT).show();
                            swipeRefresh.setRefreshing(false);
                        }
                    }
                });
            }
        });

    }

    //处理并展示Weather实体类中的数据
    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        SimpleDateFormat updateTimeOriginal = new SimpleDateFormat("yyyy-MM-dd HH:MM");
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:MM");
        String updateTime = "未知";
        try {
            updateTime = dateFormat.format(updateTimeOriginal.parse(weather.basic.update.updateTime));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        titleCity.setText(cityName);
        titleUpdateTime.setText("更新时间 " + updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dateText = (TextView) view.findViewById(R.id.date_text);
            TextView infoText = (TextView) view.findViewById(R.id.info_text);
            TextView maxText = (TextView) view.findViewById(R.id.max_text);
            TextView minText = (TextView) view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }
        if (weather.aqi != null) {
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carWash = "洗车指数：" + weather.suggestion.carWash.info;
        String sport = "运动建议：" + weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        //weatherLayout.setVisibility(View.INVISIBLE);
    }

    //加载必应每日一图
    private void loadBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(MainActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }

    public void getCityInfoByLocation(final double lon, double lat) {
        String apiURL = "https://api.heweather.com/v5/search?city=" + lon + "," + lat + "&key=beb135e90b2b44c280a7ecc5a310d98c";
        HttpUtil.sendOkHttpRequest(apiURL, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "错误：根据位置查询城市信息失败。", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final String autoLocatedCityID = getCityIDByResponseText(responseText);
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
                editor.putString("autoLocatedCityID", autoLocatedCityID);
                editor.apply();
                Toast.makeText(MainActivity.this, "定位成功。", Toast.LENGTH_SHORT).show();
            }
        });
    }

}

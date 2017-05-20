package com.example.shinianer.goodweather;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.example.shinianer.goodweather.gson.Forecast;
import com.example.shinianer.goodweather.gson.Weather;
import com.example.shinianer.goodweather.util.HttpUtil;
import com.example.shinianer.goodweather.util.LocationHelper;
import com.example.shinianer.goodweather.util.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.example.shinianer.goodweather.R.id.bing_pic_img;

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
    private Button navButton;

    public static String getCityIDByResponseText(String responseText) {
        if (responseText != null) {
            Looper.prepare();
            try {
                // Ugly Code.
                String cityID;
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
    protected void onResume() {
        super.onResume();
        LocationHelper locationHelper = new LocationHelper(this);
        locationHelper.updateLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();

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

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mCityId = prefs.getString("mCityId", "auto");
        requestWeather(mCityId);

        // 下拉刷新监听器
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                mCityId = prefs.getString("mCityId", "auto");
//                Toast.makeText(MainActivity.this, "正在刷新天气信息，当前城市 ID：" + mCityId, Toast.LENGTH_SHORT).show();
                requestWeather(mCityId);
                loadUnsplashRandomPicture(false);
            }
        });

        // 初始化图片控件
        bingPicImg = (ImageView) findViewById(bing_pic_img);

        // 显示背景图片
        // 当心：每次旋转屏幕时都会调用 Activity onCreate 方法，导致只要旋转屏幕图片就刷新。
        Bitmap bitmapImage = loadBitmapFromSharedPreferences("backgroundImageBase64");
        if (bitmapImage != null) {
            bingPicImg.setImageBitmap(bitmapImage);
        }
        else {
            loadUnsplashRandomPicture(false);
        }


        // 获取必应每日一图初始化
/*        String bingPic = prefs.getString("bing_pic", null);
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();
        }*/

        //调用openDrawer()打开滑动菜单；
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

    }

    //根据天气id请求城市天气信息
    public void requestWeather(String mCityId) {

        if (mCityId.equals("auto")) {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
            editor.putString("mCityId", mCityId);
            editor.apply();
            LocationHelper locationHelper = new LocationHelper(this);
            Location location = locationHelper.getLastKnownLocation();
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                getCityInfoByLocation(longitude, latitude);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                mCityId = prefs.getString("autoLocatedCityID", null);
                if (mCityId != null) {
                    Toast.makeText(this, "已获取到城市 ID：" + mCityId, Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(this, "查询城市 ID 失败，请检查网络。", Toast.LENGTH_LONG).show();
                    swipeRefresh.setRefreshing(false);
                    return;
                }
            } else {
                Toast.makeText(this, "未能定位，请手动选择城市。", Toast.LENGTH_LONG).show();
                drawerLayout.openDrawer(GravityCompat.START);
                swipeRefresh.setRefreshing(false);
                return;
            }
        } else {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
            editor.putString("mCityId", mCityId);
            editor.apply();
        }
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + mCityId + "&key=beb135e90b2b44c280a7ecc5a310d98c";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "从接口获取天气信息失败，请重试。", Toast.LENGTH_LONG).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
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
//                                Toast.makeText(MainActivity.this, "获取天气信息成功。", Toast.LENGTH_SHORT).show();
                                swipeRefresh.setRefreshing(false);
                            } else {
                                Toast.makeText(MainActivity.this, "未能查询到天气信息…", Toast.LENGTH_SHORT).show();
                                swipeRefresh.setRefreshing(false);
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "未能查询到天气信息…", Toast.LENGTH_SHORT).show();
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
        @SuppressLint("SimpleDateFormat") SimpleDateFormat updateTimeOriginal = new SimpleDateFormat("yyyy-MM-dd HH:MM");
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("HH:MM");
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

    // 获取来自 UnSplash 的随机图片，并存到 SharedPreferences
    public void loadUnsplashRandomPicture(Boolean useCache) {
        Log.i("RandomPicture","Loading Unsplash random picture...");
        ImageView imageView = (ImageView) findViewById(R.id.bing_pic_img);
        final String unsplashUrl="http://source.unsplash.com/random";
        boolean skipMemoryCache;
        DiskCacheStrategy diskCacheStrategy;
        if (useCache) {
            skipMemoryCache = false;
            diskCacheStrategy = DiskCacheStrategy.SOURCE;
        }
        else {
            skipMemoryCache = true;
            diskCacheStrategy = DiskCacheStrategy.NONE;
        }
        Glide.with(getApplicationContext())
                .load(unsplashUrl).skipMemoryCache(skipMemoryCache)
                .diskCacheStrategy(diskCacheStrategy)
                .into(imageView);
    }

    // 存储 Bitmap 图片对象到 SharedPreferences
    public void saveBitMapToSharedPreferences(Bitmap bitmap, String key){
        ByteArrayOutputStream stream=new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] image=stream.toByteArray();
        String stringImage = Base64.encodeToString(image,Base64.URL_SAFE);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
        editor.putString(key, stringImage);
        editor.apply();
    }

    // 从 SharedPreferences 取出图片并解析为 Bitmap
    public Bitmap loadBitmapFromSharedPreferences(String key){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String stringImage = prefs.getString(key, null);
        if (stringImage != null) {
            byte[] image=Base64.decode(stringImage,Base64.URL_SAFE);
            return BitmapFactory.decodeByteArray(image,0,image.length);
        }
        return null;
    }

    public void getCityInfoByLocation(final double lon, double lat) {
        String apiURL = "https://api.heweather.com/v5/search?city=" + lon + "," + lat + "&key=beb135e90b2b44c280a7ecc5a310d98c";
        HttpUtil.sendOkHttpRequest(apiURL, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final String autoLocatedCityID = getCityIDByResponseText(responseText);
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
                editor.putString("autoLocatedCityID", autoLocatedCityID);
                editor.apply();
            }
        });
    }

}

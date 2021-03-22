package com.yjs3408.howistheweather;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.listener.multi.BaseMultiplePermissionsListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getName();

    private ImageButton refreshImageButton;
    private ProgressBar refreshProgressBar;
    private ImageView iconImageView;
    private TextView locationTextView;
    private TextView timestampTextView;
    private TextView tempTextView;
    private TextView tempMinTextView;
    private TextView tempMaxTextView;
    private TextView humidityTextView;
    private TextView pressureTextView;
    private TextView descriptionTextView;
    private ImageButton shareImageButton;

    private Location location;
    private JSONObject weatherData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeComponents();
        registerViewEvents();
        postInitViews();
    }

    private void initializeComponents() {
        refreshImageButton = findViewById(R.id.refresh_image_button);
        refreshProgressBar = findViewById(R.id.refresh_progress_bar);
        iconImageView = findViewById(R.id.icon_image_view);
        locationTextView = findViewById(R.id.location_text_view);
        timestampTextView = findViewById(R.id.timestamp_text_view);
        tempTextView = findViewById(R.id.temp_text_view);
        tempMinTextView = findViewById(R.id.temp_min_text_view);
        tempMaxTextView = findViewById(R.id.temp_max_text_view);
        humidityTextView = findViewById(R.id.humidity_text_view);
        pressureTextView = findViewById(R.id.pressure_text_view);
        descriptionTextView = findViewById(R.id.description_text_view);
        shareImageButton = findViewById(R.id.share_image_button);
    }

    private void registerViewEvents() {
        refreshImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (location != null) {
                    updateWeatherCastData(location.getLatitude(), location.getLongitude());
                } else {
                    updateWeatherCastData(41, 29);
                }
            }
        });
        shareImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = null;
                try {
                    message = String.format("Latitude: %.2f, Longitude: %.2f, Temp:%.2f, Location:%s",
                            location.getLatitude(),
                            location.getLongitude(),
                            weatherData.getJSONObject("main").getDouble("temp"),
                            weatherData.getString("name")
                    );
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_TEXT, message);
                intent.setType("text/plain");
                startActivity(intent);
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void postInitViews() {
        Dexter.withContext(this)
                .withPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new BaseMultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                        if (multiplePermissionsReport.areAllPermissionsGranted()) {
                            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                            String gpsProvider = locationManager.getBestProvider(new Criteria(), false);
                            locationManager.requestLocationUpdates(gpsProvider, 1000, 500, new LocationListener() {
                                @Override
                                public void onLocationChanged(Location location) {
                                    MainActivity.this.location = location;
                                    updateWeatherCastData(location.getLatitude(), location.getLongitude());
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
                            });
                        }
                    }
                }).check();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateWeatherCastData(41, 29);
    }

    private void updateWeatherCastData(double latitude, double longitude) {
        refreshImageButton.setVisibility(View.INVISIBLE);
        refreshProgressBar.setVisibility(View.VISIBLE);
        downloadWeatherCastData(new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                MainActivity.this.weatherData = response;
                try {
                    Glide.with(MainActivity.this).load(String.format("https://openweathermap.org/img/wn/%s@2x.png", response.getJSONArray("weather").getJSONObject(0).getString("icon"))).into(iconImageView);
                    locationTextView.setText(response.getString("name"));
                    timestampTextView.setText(String.format("At %s will be", new SimpleDateFormat("HH:mm").format(new Date())));
                    tempTextView.setText(Math.round(response.getJSONObject("main").getDouble("temp")) + "°");
                    tempMinTextView.setText(response.getJSONObject("main").getInt("temp_min") + "°");
                    tempMaxTextView.setText(response.getJSONObject("main").getInt("temp_max") + "°");
                    humidityTextView.setText(String.valueOf(response.getJSONObject("main").getInt("humidity")));
                    pressureTextView.setText(String.valueOf(response.getJSONObject("main").getInt("pressure")));
                    descriptionTextView.setText(response.getJSONArray("weather").getJSONObject(0).getString("main"));
                    refreshImageButton.setVisibility(View.VISIBLE);
                    refreshProgressBar.setVisibility(View.INVISIBLE);

                    tempTextView.setAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.bounce_animation));
                } catch (JSONException e) {
                    Log.e(TAG, "onResponse: ", e);
                }
            }
        }, latitude, longitude);
    }

    private void downloadWeatherCastData(Response.Listener<JSONObject> listener, double latitude, double longitude) {
        final String url = String.format("https://api.openweathermap.org/data/2.5/weather?lat=%.2f&lon=%.2f&appid=6bd41c1da1996c21b237d7e41e091e5d&units=metric", latitude, longitude);
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, listener,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "onResponse: " + error.getLocalizedMessage());
                    }
                });
        requestQueue.add(request);
    }

}

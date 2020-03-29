package com.example.weatherapp.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weatherapp.R;
import com.example.weatherapp.adapter.CardAdapter;
import com.example.weatherapp.model.DataObject;
import com.example.weatherapp.model.Forecast;
import com.example.weatherapp.service.weatherservice.GenericRequestTask;
import com.example.weatherapp.service.weatherservice.ParseResult;
import com.example.weatherapp.service.weatherservice.TaskOutput;
import com.example.weatherapp.utils.ConnectionDetector;
import com.example.weatherapp.utils.Constants;
import com.example.weatherapp.utils.Utility;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @author vishal kumar
 * @version 1.0
 * @since 06.01.2019
 * <p>
 * Forecast Activity is an controller class to bind weather api data with model and show awesome view.
 */
public class ForecastActivity extends BaseActivity implements LocationListener {

    private static final String TAG = ForecastActivity.class.getSimpleName();

    private LocationManager locationManager;
    ProgressBar progressBar;
    ProgressDialog progressDialog;
    Location location;
    double latitute, longitude;
    Geocoder geocoder;

    //Connection detector class
    private ConnectionDetector connectionDetector;

    private boolean destroyed = false;
    StringBuilder addressStringBuilder;

    List<DataObject> weatherList;
    List<List<DataObject>> daysList;

    List<String> days;
    Set<String> distinctDays;
    CardAdapter cardAdapter;
    CardAdapter cardAdapter2;
    CardAdapter cardAdapter3;
    RecyclerView recyclerViewToday;
    RecyclerView recyclerViewTomorrow;
    RecyclerView recyclerViewLater;
    TabHost host;

    public String recentCityId = "";


    protected static final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 1;

    Toolbar toolbar;
    ImageView imageViewWeatherIcon;

    TextView tvTodayTemperature, tvTodayDescription, tvTodayWind, tvTodayPressure, tvTodayHumidity;
    ConstraintLayout layout;

    private static long back_pressed;

    private LinearLayout errorDisplay;

    private ImageView errorDisplayIcon;
    private TextView errorDisplayText;
    private TextView errorDisplayTryClick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forcast_layout);
        Constants.TEMP_UNIT = " " + getResources().getString(R.string.temp_unit);
        initMember();
        initUi();
        detectLocation();
        host = (TabHost) findViewById(R.id.tabHostT);
        host.setup();

        this.overridePendingTransition(R.anim.left_to_right,
                R.anim.right_to_left);

        //Setting Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.primaryDark_toolBar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setTitle("Weather");

        connectionDetector = new ConnectionDetector(ForecastActivity.this);

        errorDisplay =  findViewById(R.id.ll_errorMain_layout);

        errorDisplayIcon = findViewById(R.id.iv_errorMain_errorIcon);
        errorDisplayText =  findViewById(R.id.tv_errorMain_errorText);
        //errorDisplayTryClick =  findViewById(R.id.tv_errorMain_errorTryAgain);

        Date date = new Date();
        int day = date.getDay();

        //Tab 1
        TabHost.TabSpec spec = host.newTabSpec("Today");
        spec.setContent(R.id.tab1);
        spec.setIndicator("Today");
        host.addTab(spec);

        //Tab 2
        spec = host.newTabSpec("Tomorrow");
        spec.setContent(R.id.tab2);
        spec.setIndicator("Tomorrow");
        host.addTab(spec);

        //Tab 3
        spec = host.newTabSpec("Later");
        spec.setContent(R.id.tab3);
        spec.setIndicator("Later");
        host.addTab(spec);
        recyclerViewToday = findViewById(R.id.my_recycler_view);
        recyclerViewTomorrow = findViewById(R.id.my_recycler_view2);
        recyclerViewLater = findViewById(R.id.my_recycler_view3);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        LinearLayoutManager layoutManager2 = new LinearLayoutManager(this);
        LinearLayoutManager layoutManager3 = new LinearLayoutManager(this);
        recyclerViewToday.setLayoutManager(layoutManager);
        recyclerViewToday.setItemAnimator(new DefaultItemAnimator());

        recyclerViewTomorrow.setLayoutManager(layoutManager2);
        recyclerViewTomorrow.setItemAnimator(new DefaultItemAnimator());

        recyclerViewLater.setLayoutManager(layoutManager3);
        recyclerViewLater.setItemAnimator(new DefaultItemAnimator());


        //getWeather(addressStringBuilder);

        boolean isInternetPresent = connectionDetector.isConnectingToInternet();

        if (isInternetPresent) {

            errorDisplay.setVisibility(View.GONE);
            getWeather(addressStringBuilder);
        }
        else {

            errorDisplay.setVisibility(View.VISIBLE);

            errorDisplayIcon.setImageResource(R.drawable.img_error_internet);
            errorDisplayText.setText(getString(R.string.error_internet));
        }
    }




    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        destroyed = true;

        if (locationManager != null) {
            try {
                locationManager.removeUpdates(ForecastActivity.this);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * This method gets the device current location to call the weather api by default city
     */
    private void detectLocation() {
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                latitute = location.getLatitude();
                longitude = location.getLongitude();
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {
                Toast.makeText(ForecastActivity.this, "Connect to network", Toast.LENGTH_SHORT).show();
            }
        };

        int permissionCheck = ContextCompat.checkSelfPermission(ForecastActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == 0) {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }
        if (location != null) {
            latitute = location.getLatitude();
            longitude = location.getLongitude();
        }

        try {
            geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addressList = geocoder.getFromLocation(latitute, longitude, 1);
            addressStringBuilder = new StringBuilder();
            if (addressList.size() > 0) {
                Address locationAddress = addressList.get(0);
                for (int i = 0; i <= locationAddress.getMaxAddressLineIndex(); i++) {
                    locationAddress.getAddressLine(i);
                    /*remove comment if you subLocality need to be shown*/
                    // addressStringBuilder.append(locationAddress.getSubLocality()).append(",");
                    addressStringBuilder.append(locationAddress.getLocality());
                }
                /*TODO Set the current location to display*/
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menus, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            /*Intent intentHome = new Intent(ForecastActivity.this, HomeActivity.class);
            startActivity(intentHome);*/
            finish();
        }

        switch (item.getItemId()) {
            case R.id.action_search:
                searchByCityName();
                return true;

            case R.id.action_detectLocation:
                getCityByLocation();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    /**
     * This method shows a dialog to enter the city name to search
     */
    private void searchByCityName() {
        androidx.appcompat.app.AlertDialog.Builder alert = new androidx.appcompat.app.AlertDialog.Builder(this);
        alert.setTitle("Enter City Name");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setMaxLines(1);
        input.setSingleLine(true);
        alert.setView(input, 32, 0, 32, 0);
        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String result = input.getText().toString();
                if (!result.isEmpty()) {
                    fetchUpdateOnSearched(result);
                }
            }
        });
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Cancelled
            }
        });
        alert.show();

    }

    public static long saveLastUpdateTime(SharedPreferences sp) {
        Calendar now = Calendar.getInstance();
        sp.edit().putLong("lastUpdate", now.getTimeInMillis()).commit();
        return now.getTimeInMillis();
    }

    /**
     * This method call the getWeather api to searched the weather
     *
     * @param cityName
     */
    private void fetchUpdateOnSearched(String cityName) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(cityName);
        getWeather(stringBuilder);
    }

    void getCityByLocation() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Explanation not needed, since user requests this themmself

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_ACCESS_FINE_LOCATION);
            }

        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(getString(R.string.getting_location));
            progressDialog.setCancelable(false);
            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    try {
                        locationManager.removeUpdates(ForecastActivity.this);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
            });
            progressDialog.show();
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            }
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            }
        } else {
            showLocationSettingsDialog();
        }
    }

    private void showLocationSettingsDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(R.string.location_settings);
        alertDialog.setMessage(R.string.location_settings_message);
        alertDialog.setPositiveButton(R.string.location_settings_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });
        alertDialog.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getCityByLocation();
                }
                return;
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        progressDialog.hide();
        try {
            locationManager.removeUpdates(this);
        } catch (SecurityException e) {
            Log.e("LocationManager", "Error while trying to stop listening for location updates. This is probably a permissions issue", e);
        }
        Log.i("LOCATION (" + location.getProvider().toUpperCase() + ")", location.getLatitude() + ", " + location.getLongitude());
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        new ProvideCityNameTask(this, this, progressDialog).execute("coords", Double.toString(latitude), Double.toString(longitude));

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    /**
     * This method get the Date
     *
     * @param milliTime
     * @return
     */
    private String getDate(Long milliTime) {
        Date currentDate = new Date(milliTime);
        SimpleDateFormat df = new SimpleDateFormat("dd");
        String date = df.format(currentDate);
        return date;
    }

    /**
     * This method call the openwheathermap api by city name and onResponseSuccess bind the data
     * with associated model and set the data to show awesome view to user.
     *
     * @param addressStringBuilder
     */
    private void getWeather(StringBuilder addressStringBuilder) {
        progressDialog.show();
        Call<Forecast> call = Utility.getApis().getWeatherForecastData(addressStringBuilder, Constants.API_KEY, Constants.UNITS);
        call.enqueue(new Callback<Forecast>() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onResponse(Call<Forecast> call, Response<Forecast> response) {
                progressDialog.dismiss();
                if (response.isSuccessful()) {
                    Log.i(TAG, "onResponse: " + response.isSuccessful());
                    weatherList = response.body().getDataObjectList();
                    distinctDays = new LinkedHashSet<>();
                    for (DataObject obj : weatherList) {
                        distinctDays.add(getDate(obj.getDt() * 1000));
                    }
                    Log.i("DISTINCTSIZE", distinctDays.size() + "");

                    days = new ArrayList<>();
                    days.addAll(distinctDays);

                    for (String day : days) {
                        List<DataObject> temp = new ArrayList<>();
                        Log.i("DAY", day);
                        for (DataObject data : weatherList) {
                            Log.i("ELEMENT", getDate(data.getDt() * 1000));
                            if (getDate(data.getDt() * 1000).equals(day)) {
                                Log.i("ADDEDDD", getDate(data.getDt() * 1000));
                                temp.add(data);
                            }
                        }
                        daysList.add(temp);
                    }

                    daysList.get(0).remove(0);

                    Log.i("DAYSLISTSIZE", daysList.size() + "");
                    cardAdapter = new CardAdapter(daysList.get(0));
                    recyclerViewToday.setAdapter(cardAdapter);

                    cardAdapter2 = new CardAdapter(daysList.get(1));
                    recyclerViewTomorrow.setAdapter(cardAdapter2);

                    cardAdapter3 = new CardAdapter(daysList.get(2));
                    recyclerViewLater.setAdapter(cardAdapter3);

                    toolbar.setTitle(response.body().getCity().getName() + ", " + response.body().getCity().getCountry());
                    switch (weatherList.get(0).getWeather().get(0).getIcon()) {
                        case "01d":
                            imageViewWeatherIcon.setImageResource(R.drawable.ic_weather_clear_sky);
                            toolbar.setBackgroundResource(R.color.color_clear_and_sunny);
                            layout.setBackgroundColor(getResources().getColor(R.color.color_clear_and_sunny));
                            break;
                        case "01n":
                            imageViewWeatherIcon.setImageResource(R.drawable.ic_weather_clear_sky);
                            toolbar.setBackgroundResource(R.color.color_clear_and_sunny);
                            layout.setBackgroundColor(getResources().getColor(R.color.color_clear_and_sunny));
                            break;
                        case "02d":
                            imageViewWeatherIcon.setImageResource(R.drawable.ic_weather_few_cloud);
                            toolbar.setBackgroundResource(R.color.color_partly_cloudy);
                            layout.setBackgroundColor(getResources().getColor(R.color.color_partly_cloudy));
                            break;
                        case "02n":
                            imageViewWeatherIcon.setImageResource(R.drawable.ic_weather_few_cloud);
                            toolbar.setBackgroundResource(R.color.color_partly_cloudy);
                            layout.setBackgroundColor(getResources().getColor(R.color.color_partly_cloudy));
                            break;
                        case "03d":
                            imageViewWeatherIcon.setImageResource(R.drawable.ic_weather_scattered_clouds);
                            toolbar.setBackgroundResource(R.color.color_gusty_winds);
                            layout.setBackgroundColor(getResources().getColor(R.color.color_gusty_winds));
                            break;
                        case "03n":
                            imageViewWeatherIcon.setImageResource(R.drawable.ic_weather_scattered_clouds);
                            toolbar.setBackgroundResource(R.color.color_gusty_winds);
                            layout.setBackgroundColor(getResources().getColor(R.color.color_gusty_winds));
                            break;
                        case "04d":
                            imageViewWeatherIcon.setImageResource(R.drawable.ic_weather_broken_clouds);
                            toolbar.setBackgroundResource(R.color.color_cloudy_overnight);
                            layout.setBackgroundColor(getResources().getColor(R.color.color_cloudy_overnight));
                            break;
                        case "04n":
                            imageViewWeatherIcon.setImageResource(R.drawable.ic_weather_broken_clouds);
                            toolbar.setBackgroundResource(R.color.color_cloudy_overnight);
                            layout.setBackgroundColor(getResources().getColor(R.color.color_cloudy_overnight));
                            break;
                        case "09d":
                            imageViewWeatherIcon.setImageResource(R.drawable.ic_weather_shower_rain);
                            toolbar.setBackgroundResource(R.color.color_hail_stroms);
                            layout.setBackgroundColor(getResources().getColor(R.color.color_hail_stroms));
                            break;
                        case "09n":
                            imageViewWeatherIcon.setImageResource(R.drawable.ic_weather_shower_rain);
                            toolbar.setBackgroundResource(R.color.color_hail_stroms);
                            layout.setBackgroundColor(getResources().getColor(R.color.color_hail_stroms));
                            break;
                        case "10d":
                            imageViewWeatherIcon.setImageResource(R.drawable.ic_weather_rain);
                            toolbar.setBackgroundResource(R.color.color_heavy_rain);
                            layout.setBackgroundColor(getResources().getColor(R.color.color_heavy_rain));
                            break;
                        case "10n":
                            imageViewWeatherIcon.setImageResource(R.drawable.ic_weather_rain);
                            toolbar.setBackgroundResource(R.color.color_heavy_rain);
                            layout.setBackgroundColor(getResources().getColor(R.color.color_heavy_rain));
                            break;
                        case "11d":
                            imageViewWeatherIcon.setImageResource(R.drawable.ic_weather_thunderstorm);
                            toolbar.setBackgroundResource(R.color.color_thunderstroms);
                            layout.setBackgroundColor(getResources().getColor(R.color.color_thunderstroms));
                            break;
                        case "11n":
                            imageViewWeatherIcon.setImageResource(R.drawable.ic_weather_thunderstorm);
                            toolbar.setBackgroundResource(R.color.color_thunderstroms);
                            layout.setBackgroundColor(getResources().getColor(R.color.color_thunderstroms));
                            break;
                        case "13d":
                            imageViewWeatherIcon.setImageResource(R.drawable.ic_weather_snow);
                            toolbar.setBackgroundResource(R.color.color_snow);
                            layout.setBackgroundColor(getResources().getColor(R.color.color_snow));
                            break;
                        case "13n":
                            imageViewWeatherIcon.setImageResource(R.drawable.ic_weather_snow);
                            toolbar.setBackgroundResource(R.color.color_snow);
                            layout.setBackgroundColor(getResources().getColor(R.color.color_snow));
                            break;
                        case "15d":
                            imageViewWeatherIcon.setImageResource(R.drawable.ic_weather_mist);
                            toolbar.setBackgroundResource(R.color.color_mix_snow_and_rain);
                            layout.setBackgroundColor(getResources().getColor(R.color.color_mix_snow_and_rain));
                            break;
                        case "15n":
                            imageViewWeatherIcon.setImageResource(R.drawable.ic_weather_mist);
                            toolbar.setBackgroundResource(R.color.color_mix_snow_and_rain);
                            layout.setBackgroundColor(getResources().getColor(R.color.color_mix_snow_and_rain));
                            break;
                    }
                    tvTodayTemperature.setText(weatherList.get(0).getMain().getTemp() + " " + getString(R.string.temp_unit));
                    tvTodayDescription.setText(weatherList.get(0).getWeather().get(0).getDescription());
                    tvTodayWind.setText(getString(R.string.wind_lable) + " " + weatherList.get(0).getWind().getSpeed() + " " + getString(R.string.wind_unit));
                    tvTodayPressure.setText(getString(R.string.pressure_lable) + " " + weatherList.get(0).getMain().getPressure() + " " + getString(R.string.pressure_unit));
                    tvTodayHumidity.setText(getString(R.string.humidity_lable) + " " + weatherList.get(0).getMain().getHumidity() + " " + getString(R.string.humidity_unit));
                }
            }

            @Override
            public void onFailure(Call<Forecast> call, Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "onFailure: " + t.getMessage());
                Toast.makeText(ForecastActivity.this, getString(R.string.msg_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * This method initialize the all ui member variables
     */
    private void initUi() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.progress));
        toolbar = findViewById(R.id.app_toolbar);
        setSupportActionBar(toolbar);
        imageViewWeatherIcon = findViewById(R.id.imageViewWeather);
        tvTodayTemperature = findViewById(R.id.todayTemperature);
        tvTodayDescription = findViewById(R.id.todayDescription);
        tvTodayWind = findViewById(R.id.todayWind);
        tvTodayPressure = findViewById(R.id.todayPressure);
        tvTodayHumidity = findViewById(R.id.todayHumidity);

        layout = findViewById(R.id.layoutWeather);
    }

    /**
     * This method initialize the all member variables
     */
    private void initMember() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        weatherList = new ArrayList<>();
        daysList = new ArrayList<>();
    }



    class ProvideCityNameTask extends GenericRequestTask {

        public ProvideCityNameTask(Context context, ForecastActivity activity, ProgressDialog progressDialog) {
            super(context, activity, progressDialog);
        }

        @Override
        protected void onPreExecute() { /*Nothing*/ }

        @Override
        protected String getAPIName() {
            return "weather";
        }

        @Override
        protected ParseResult parseResponse(String response) {
            Log.i("RESULT", response.toString());
            try {
                JSONObject reader = new JSONObject(response);

                final String code = reader.optString("cod");
                if ("404".equals(code)) {
                    Log.e("Geolocation", "No city found");
                    return ParseResult.CITY_NOT_FOUND;
                }

                saveLocation(reader.getString("id"));

            } catch (JSONException e) {
                Log.e("JSONException Data", response);
                e.printStackTrace();
                return ParseResult.JSON_EXCEPTION;
            }

            return ParseResult.OK;
        }

        @Override
        protected void onPostExecute(TaskOutput output) {
            /* Handle possible errors only */
            handleTaskOutput(output);

            refreshWeather();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void refreshWeather() {
        if (isNetworkAvailable()) {

            getWeather(addressStringBuilder);

        }
        else {
            Snackbar.make(layout, getString(R.string.msg_connection_not_available), Snackbar.LENGTH_LONG).show();
        }
    }

    private void saveLocation(String result) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ForecastActivity.this);
        recentCityId = preferences.getString("cityId", Constants.DEFAULT_CITY_ID);

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("cityId", result);

        editor.commit();
    }
}

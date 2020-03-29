package com.example.weatherapp.utils;

import com.example.weatherapp.model.Forecast;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface Api {

    @GET("forecast")
    Call<Forecast> getWeatherForecastData(@Query("q") StringBuilder cityName, @Query("APPID") String APIKEY, @Query("units") String TempUnit);




}

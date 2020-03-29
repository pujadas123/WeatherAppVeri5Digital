package com.example.weatherapp.utils;

/**
 * @author vishal kumar
 * @version 1.0
 * @since 06.01.2019
 */
public class Utility {
    private static final String TAG = Utility.class.getSimpleName();

    public static Api getApis() {
        return Client.getClient().create(Api.class);
    }
}

package com.example.weatherapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.example.weatherapp.R;

public class SplashActivity extends AppCompatActivity {

    //Declaring Variables
    private static final int SPLASH_DURATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //Hiding keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        //handler for delay
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                //Calling Home activity
                startActivity(new Intent(SplashActivity.this, ForecastActivity.class));
                finish();

            }
        }, SPLASH_DURATION * 1000);
    }
}

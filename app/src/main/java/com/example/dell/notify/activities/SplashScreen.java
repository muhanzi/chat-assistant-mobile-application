package com.example.dell.notify.activities;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.dell.notify.R;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Handler handler=new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // when user is logged in
//                Intent mainActivity = new Intent(SplashScreen.this, MainActivity.class);
//                mainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                startActivity(mainActivity);
//                finish();
                // when user is not logged in
                Intent showFragments = new Intent(SplashScreen.this, ShowFragments.class);
                showFragments.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(showFragments);
                finish();
            }
        },2000);
    }

}

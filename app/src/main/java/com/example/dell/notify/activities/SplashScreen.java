package com.example.dell.notify.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.dell.notify.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashScreen extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        sharedpreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Handler handler=new Handler();
        handler.postDelayed(() -> {
            if(currentUser != null){
                // when user is logged in
                startMainActivity(currentUser);
            }else{
                // when user is not logged in
                Intent showFragments = new Intent(SplashScreen.this, ShowFragments.class);
                showFragments.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(showFragments);
                finish();
            }
        },2000);
    }

    private void startMainActivity(FirebaseUser user){
        // when user is logged in
//        boolean mainActivityIsActive=sharedpreferences.getBoolean("MainActivityIsActive",true);
//        boolean firstStartOfMainActivity=sharedpreferences.getBoolean("firstStartOfMainActivity",true);
//        if(!mainActivityIsActive){
//            // means the MainActivity has been destroyed
//            Intent mainActivity = new Intent(this, MainActivity.class);
//            //mainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); //
//            mainActivity.putExtra("email",user.getDisplayName());
//            mainActivity.putExtra("userID",user.getUid());
//            startActivity(mainActivity);
//            finish();
//        }else if(firstStartOfMainActivity){
//            Intent mainActivity = new Intent(this, MainActivity.class);
//            mainActivity.putExtra("email",user.getDisplayName());
//            mainActivity.putExtra("userID",user.getUid());
//            startActivity(mainActivity);
//            SharedPreferences.Editor editor = sharedpreferences.edit();
//            editor.putBoolean("firstStartOfMainActivity",false);
//            editor.apply();
//            finish();
//        }
//        else{
//            MainActivity mainActivity = MainActivity.instance;
//            mainActivity.bring_main_activity_to_foreground();
//        }
        //
        Intent mainActivity = new Intent(this, MainActivity.class);
        //mainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); // clearing task everytime will also clear the existing tsk of MainActivity // we want MainActivity to be SingleTask
        mainActivity.putExtra("email",user.getDisplayName());
        mainActivity.putExtra("userID",user.getUid());
        startActivity(mainActivity);
        finish();
    }

}

package com.example.dell.notify.activities;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.dell.notify.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashScreen extends AppCompatActivity {

    private FirebaseAuth mAuth;

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
        Intent mainActivity = new Intent(this, MainActivity.class);
        mainActivity.putExtra("email",user.getDisplayName());
        mainActivity.putExtra("userID",user.getUid());
        startActivity(mainActivity);
        finish();
    }

}

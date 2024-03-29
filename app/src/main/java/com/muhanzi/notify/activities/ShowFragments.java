package com.muhanzi.notify.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.muhanzi.notify.R;
import com.muhanzi.notify.fragments.walkthrough_fragments.MyFragmentsAdapter;
import com.muhanzi.notify.fragments.walkthrough_fragments.Page3;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseUser;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

public class ShowFragments extends AppCompatActivity {

    public static ViewPager mypager;
    private MyFragmentsAdapter adapter;
    private TabLayout tabLayout;
    public static ShowFragments instance;
    private final int REQ_CODE_PERMISSIONS_FOR_SMS_AUDIO_CONTACTS = 212;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_fragments);

        mypager= findViewById(R.id.pager);
        tabLayout = findViewById(R.id.tabDots);
        instance = this;
        tabLayout.setupWithViewPager(mypager, true);
        adapter=new MyFragmentsAdapter(getSupportFragmentManager(),1);
        mypager.setAdapter(adapter);
        mypager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) tabLayout.getLayoutParams();
                if(position == 2){
                   params.verticalBias = 0.9f;
                   tabLayout.setLayoutParams(params);
               }else{
                    params.verticalBias = 1.0f;
                    tabLayout.setLayoutParams(params);
               }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQ_CODE_PERMISSIONS_FOR_SMS_AUDIO_CONTACTS:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if(checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                            && checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                            && checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
                            && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            && checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                            ){
                        if(Settings.System.canWrite(this)){
                            allPermissionsGranted();
                            Toast.makeText(this, "permissions allowed", Toast.LENGTH_SHORT).show();  //works fine
                        }
                    }
                }else{
                    Toast.makeText(getApplicationContext(),"permissions denied", Toast.LENGTH_LONG).show();
                }
                break;
            default:
                return;
        }

    }

    private void allPermissionsGranted(){
        Button grant_permissions = Page3.mainview.findViewById(R.id.grant_permission_button);
        grant_permissions.setEnabled(false);
        grant_permissions.setText(R.string.permissions_granted);
        grant_permissions.setBackground(ContextCompat.getDrawable(this,R.drawable.permissions_granted_button));
    }

    public void startMainActivity(FirebaseUser user){
        // when user is logged in
        Intent mainActivity = new Intent(this, MainActivity.class);
        mainActivity.putExtra("email",user.getDisplayName());
        mainActivity.putExtra("userID",user.getUid());
        startActivity(mainActivity);
        finish();
    }

    @Override
    public void onBackPressed() {
        finishAffinity();  // quit the app // clear all activities // and free the allocated memory
        System.exit(0);
    }

}

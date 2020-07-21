package com.example.dell.notify.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;

import com.example.dell.notify.R;
import com.example.dell.notify.fragments.settings_fragments.SettingsFragmentsAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static ViewPager mypager;
    private SettingsFragmentsAdapter adapter;
    private BottomNavigationView navigation;
    private TextView user_email;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

//        user_email = (TextView) navigationView.findViewById(R.id.user_email);
          mAuth = FirebaseAuth.getInstance();
//        FirebaseUser currentUser = mAuth.getCurrentUser();
//        if(currentUser != null){
//            user_email.setText(currentUser.getEmail());
//        }

        mypager= findViewById(R.id.settingsPager);
        adapter=new SettingsFragmentsAdapter(getSupportFragmentManager());
        mypager.setAdapter(adapter);
        mypager.addOnPageChangeListener(viewPagerPageChangeListener);
        navigation = (BottomNavigationView) findViewById(R.id.bottomNavigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.report_issue) {
            //report issue
        } else if (id == R.id.turn_off_spam_filter) {

        } else if (id == R.id.logout) {
            mAuth.signOut();
            Intent splashScreen = new Intent(SettingsActivity.this, SplashScreen.class);
            splashScreen.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); // !!!
            startActivity(splashScreen);
            finish();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = item -> {
                switch (item.getItemId()) {
                    case R.id.navigation_apps:
                        mypager.setCurrentItem(0,true);
                        return true;
                    case R.id.navigation_contacts:
                        mypager.setCurrentItem(1,true);
                        return true;
                    case R.id.navigation_dictionary:
                        mypager.setCurrentItem(2,true);
                        return true;
                }
                return false;
            };

    private ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            switch (position){
                case 0 :
                    navigation.setSelectedItemId(R.id.navigation_apps);
                    break;
                case 1 :
                    navigation.setSelectedItemId(R.id.navigation_contacts);
                    break;
                case 2 :
                    navigation.setSelectedItemId(R.id.navigation_dictionary);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

}

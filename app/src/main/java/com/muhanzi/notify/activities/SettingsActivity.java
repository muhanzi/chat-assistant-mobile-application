package com.muhanzi.notify.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.muhanzi.notify.R;
import com.muhanzi.notify.fragments.settings_fragments.SettingsFragmentsAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

public class SettingsActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static ViewPager mypager;
    private SettingsFragmentsAdapter adapter;
    private BottomNavigationView navigation;
    private TextView user_email;
    private FirebaseAuth mAuth;
    public static SettingsActivity instance;


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
        View headerView = navigationView.getHeaderView(0); // get the header layout // the one passed in the xml --> app:headerLayout // it is the first header view so we get it at position 0
        navigationView.setNavigationItemSelectedListener(this);
        user_email = (TextView) headerView.findViewById(R.id.user_email);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            user_email.setText(currentUser.getEmail());
        }

        instance = this;

        mypager= findViewById(R.id.settingsPager);
        adapter=new SettingsFragmentsAdapter(getSupportFragmentManager(),1);
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

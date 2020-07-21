package com.example.dell.notify.fragments.settings_fragments;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created by DELL on 6/2/2020.
 */


public class SettingsFragmentsAdapter extends FragmentPagerAdapter {

    public SettingsFragmentsAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {

        switch(position){
            case 0 :
                return new SettingsPage1();
            case 1:
                return new SettingsPage2();
            case 2:
                return new SettingsPage3();
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 3;  // because we have 3 fragments
    }



}


package com.muhanzi.notify.fragments.settings_fragments;


import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

/**
 * Created by DELL on 6/2/2020.
 */


public class SettingsFragmentsAdapter extends FragmentPagerAdapter {

    public SettingsFragmentsAdapter(FragmentManager fm,int behavior) {
        super(fm,behavior); // behavior // 1 or 0 // 1 --> means only current fragment is in resume state
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


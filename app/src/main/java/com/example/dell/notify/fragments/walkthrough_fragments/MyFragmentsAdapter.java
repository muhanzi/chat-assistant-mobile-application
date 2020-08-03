package com.example.dell.notify.fragments.walkthrough_fragments;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created by DELL on 6/2/2020.
 */


public class MyFragmentsAdapter extends FragmentPagerAdapter {

    public MyFragmentsAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {

        switch(position){
            case 0 :
                return new Page1();
            case 1:
                return new Page2();
            case 2:
                return new Page3();
            case 3:
                return new Page4();
            case 4:
                return new Page5();
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 5;  // because we have 5 fragments
    }



}


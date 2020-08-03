package com.example.dell.notify.fragments.settings_fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.dell.notify.R;

public class SettingsPage3 extends Fragment {

    public static View mainview;
    private Context context;

    public SettingsPage3() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainview = inflater.inflate(R.layout.fragment_settings_page3, container, false);
        context = getContext();
        return mainview;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

}

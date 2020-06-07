package com.example.dell.notify.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.dell.notify.R;

import static com.example.dell.notify.activities.ShowFragments.mypager;

public class Page5 extends Fragment {

    private ImageView page5_previous;
    private View mainview;

    public Page5() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainview= inflater.inflate(R.layout.fragment_page5, container, false);
        page5_previous= mainview.findViewById(R.id.page5_previous);
        page5_previous.setOnClickListener((view)-> mypager.setCurrentItem(3));
        return mainview;
    }

    /*
    @Override
    public void onStart() {
        super.onStart();
    }
    */
}

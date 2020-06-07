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

public class Page1 extends Fragment {

    private ImageView page1_next;
    private View mainview;

    public Page1() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainview= inflater.inflate(R.layout.fragment_page1, container, false);
        page1_next= mainview.findViewById(R.id.page1_next);
        page1_next.setOnClickListener((view)-> mypager.setCurrentItem(1));
        return mainview;
    }

    /*
    @Override
    public void onStart() {
        super.onStart();
    }
    */
}

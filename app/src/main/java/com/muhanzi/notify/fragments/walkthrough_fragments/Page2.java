package com.muhanzi.notify.fragments.walkthrough_fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.muhanzi.notify.R;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import static com.muhanzi.notify.activities.ShowFragments.mypager;

public class Page2 extends Fragment {

    private ImageView page2_previous,page2_next;
    private View mainview;

    public Page2() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainview= inflater.inflate(R.layout.fragment_page2, container, false);
        page2_previous= mainview.findViewById(R.id.page2_previous);
        page2_next= mainview.findViewById(R.id.page2_next);
        page2_previous.setOnClickListener((view)-> mypager.setCurrentItem(0));
        page2_next.setOnClickListener((view)-> mypager.setCurrentItem(2));
        return mainview;
    }

    /*
    @Override
    public void onStart() {
        super.onStart();
    }
    */
}

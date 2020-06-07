package com.example.dell.notify.fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.dell.notify.R;

import static com.example.dell.notify.activities.ShowFragments.mypager;

public class Page4 extends Fragment {

    private ImageView page4_previous,page4_next;
    private View mainview;
    private ProgressDialog prog;

    public Page4() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainview= inflater.inflate(R.layout.fragment_page4, container, false);
        page4_previous= mainview.findViewById(R.id.page4_previous);
        page4_next= mainview.findViewById(R.id.page4_next);
        prog =new ProgressDialog(getContext());
        page4_previous.setOnClickListener((view)-> mypager.setCurrentItem(2));
        page4_next.setOnClickListener((view)-> mypager.setCurrentItem(4));

//        loginButton.setOnClickListener((view)-> {
//            mypager.setCurrentItem(mypager.getCurrentItem());
//            prog.setMessage("please wait");
//            //prog.setCanceledOnTouchOutside(false);
//            prog.setCancelable(false); // works better // because even onBackPressed() will not cancel the dialog
//            prog.show();
//        });

        return mainview;
    }

    /*
    @Override
    public void onStart() {
        super.onStart();
    }
    */
}

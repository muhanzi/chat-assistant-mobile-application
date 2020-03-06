package com.example.dell.newproject2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by DELL on 1/18/2020.
 */

public class TheBroadcast2 extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
//        Toast.makeText(context,"broadcast is working",Toast.LENGTH_LONG).show();
//        MainActivity.butt3.performClick();
        new MainActivity().afterBootCompleted();
    }
}

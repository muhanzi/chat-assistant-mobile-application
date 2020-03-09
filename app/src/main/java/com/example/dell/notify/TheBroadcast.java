package com.example.dell.notify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by DELL on 11/17/2019.
 */

public class TheBroadcast extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
//        Toast.makeText(context,"broadcast is working",Toast.LENGTH_LONG).show();
//        MainActivity.butt3.performClick();
        new MainActivity().afterBootCompleted();
    }
}

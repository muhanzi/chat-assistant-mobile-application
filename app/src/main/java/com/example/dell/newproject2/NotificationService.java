package com.example.dell.newproject2;

/**
 * Created by DELL on 12/27/2019.
 */

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.support.v4.content.LocalBroadcastManager;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;


public class NotificationService extends NotificationListenerService {

    Context context;
    private boolean STATUS_BAR_READ_ONCE =false;
    //
    private static final int NOTIF_ID = 1;
    private static final String NOTIF_CHANNEL_ID = "channel2";

    @Override
    public void onCreate() {

        super.onCreate();
        context = getApplicationContext();
        createNotificationChannel();
        startforeground();// try
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if(!STATUS_BAR_READ_ONCE){
            readActiveNotifications();
            return;
            // put a delay here // before the app continues with onNotificationPosted()
        }
        String pack = sbn.getPackageName();
        if(pack.equals("android")){
            return;
        }
        String ticker ="";
        if(sbn.getNotification().tickerText !=null) {
            ticker = sbn.getNotification().tickerText.toString();
        }
        Bundle extras = sbn.getNotification().extras;
        //
        String title,text;
        if(extras.getString("android.title") != null && extras.getCharSequence("android.text") != null){
            title = extras.getString("android.title");
            text = extras.getCharSequence("android.text").toString();
            if(title.equals("Notification to repeat") || title.equals("Message from ServiceWithAlarmManager")){
                return;
            }
        }else{
            // title or text is empty
            return;
        }
        //
        int id1 = extras.getInt(Notification.EXTRA_SMALL_ICON);
        Bitmap id = sbn.getNotification().largeIcon;


//        Log.i("Package",pack);
//        Log.i("Ticker",ticker);
//        Log.i("Title",title);
//        Log.i("Text",text);

        Intent msgrcv = new Intent("Msg"); // action --> "Msg"
        msgrcv.putExtra("package", pack);
        msgrcv.putExtra("ticker", ticker);
        msgrcv.putExtra("title", title);
        msgrcv.putExtra("text", text);
        if(id != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            id.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            msgrcv.putExtra("icon",byteArray);
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(msgrcv);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i("Msg","Notification Removed");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
// codes inside this function will be repeated every time
        return START_STICKY;
    }

    private void readActiveNotifications() {
        // read all active notifications // !!!!!!!!!!!!!!!!  from this app  // most of the time it is just one
        StatusBarNotification[] sbn= NotificationService.this.getActiveNotifications();  // will be null because notification listener is not yet connected
        //
        ArrayList<String> all=new ArrayList<>();
        for(StatusBarNotification noti : sbn){
            String pack = noti.getPackageName();
            /*
            String ticker ="";
            if(noti.getNotification().tickerText !=null) {
                ticker = noti.getNotification().tickerText.toString();
            }
            */
            Bundle extras = noti.getNotification().extras;
            String title = extras.getString("android.title");
            String text;
            if(extras.getCharSequence("android.text") != null){
                text = extras.getCharSequence("android.text").toString();
            }else{
                text="text is empty";
            }
            all.add(title+" it says : "+text);
        }
        //
        Intent allNoti = new Intent("allNotifications"); // action --> "allNotifications"
        allNoti.putExtra("notifications", all);
        //
        STATUS_BAR_READ_ONCE=true; // status bar is read already
        // send localbroadcast  action --> "allNotifications"
        LocalBroadcastManager.getInstance(context).sendBroadcast(allNoti);
    }

    ////////////////////////////////////////////////////////////////

    private void startforeground() {
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        // a foreground service always displays a notification in the status bar // so that the user is aware of this service which is always running
        this.startForeground(NOTIF_ID, new NotificationCompat.Builder(this,  //!!! I changed this ---> startForeground(NOTIF_ID, new NotificationCompat.Builder(this,
                NOTIF_CHANNEL_ID)
                .setOngoing(true)
                .setSmallIcon(R.drawable.notif_icon)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Service is running in the background")
                .setContentIntent(pendingIntent)
                .build());
        //
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(getApplicationContext(),TryService.class));
        }else{
            startService(new Intent(getApplicationContext(),TryService.class));
        }
        //
    }


    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name2);
            String description = getString(R.string.channel_description2);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIF_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /*
    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
    }


    @Override
    public void onLowMemory() {
        super.onLowMemory();
        freeMemory();
    }
    */

    public void freeMemory(){
        System.runFinalization();
        Runtime.getRuntime().gc();
        System.gc();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent myServiceIntent = new Intent(this, NotificationService.class);
        myServiceIntent.setPackage(getPackageName());
        startService(myServiceIntent);
        super.onTaskRemoved(rootIntent);
    }

}

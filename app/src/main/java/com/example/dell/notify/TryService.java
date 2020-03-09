package com.example.dell.notify;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

public class TryService extends Service {

    Context context;
    private static final int NOTIF_ID = 4;
    private static final String NOTIF_CHANNEL_ID = "channel3";
    private static final String NOTIF_CHANNEL_ID2 = "channel4";
    @Override
    public void onCreate() {

        super.onCreate();
        context = getApplicationContext();
        createNotificationChannel();
        channelForNotificationsToRepeat();
        startforeground();// try
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //
        final Handler handler=new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try{
                    // because the notification might not yet be posted
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                    notificationManager.cancel(5);
                }catch(Exception e){
                    Log.i("Exception",e.getMessage());
                }
                show_notification_after_one_minute();
                handler.postDelayed(this,60*1000);  // repeat   // #Recursion
            }
        }, 60*1000); // after 120 seconds
        //
        return START_STICKY;
    }

    private void startforeground() {
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        // a foreground service always displays a notification in the status bar // so that the user is aware of this service which is always running
        this.startForeground(NOTIF_ID, new NotificationCompat.Builder(this,  //!!! I changed this ---> startForeground(NOTIF_ID, new NotificationCompat.Builder(this,
                NOTIF_CHANNEL_ID)
                .setOngoing(true)
                .setSmallIcon(R.drawable.notify_icon)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("TryService is running in the background")
                .setContentIntent(pendingIntent)
                .build());
    }


    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name3);
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

    private void show_notification_after_one_minute(){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIF_CHANNEL_ID2)
                .setSmallIcon(R.drawable.notify_icon)
                .setContentTitle("Notification to repeat")
                .setContentText("TryService is working well")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(5, builder.build());
    }

    private void channelForNotificationsToRepeat() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name4);
            String description = getString(R.string.channel_description2);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(NOTIF_CHANNEL_ID2, name, importance);
            channel.setDescription(description);
            channel.setSound(null,null);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent myServiceIntent = new Intent(this, NotificationService.class);
        myServiceIntent.setPackage(getPackageName());
        startService(myServiceIntent);
        super.onTaskRemoved(rootIntent);
    }

}


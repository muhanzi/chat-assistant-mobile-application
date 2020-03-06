package com.example.dell.newproject2;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

public class ServiceWithAlarmManager extends Service {
    Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    public ServiceWithAlarmManager() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try{
            // because the notification might not yet be posted
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.cancel(6);
        }catch(Exception e){
            Log.i("Exception",e.getMessage());
        }
        show_notification();
        return START_STICKY;

    }

    private void show_notification(){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "1") // channel4
                .setSmallIcon(R.drawable.notif_icon)
                .setContentTitle("Message from ServiceWithAlarmManager")
                .setContentText("Notification shown")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(6, builder.build());
    }
}

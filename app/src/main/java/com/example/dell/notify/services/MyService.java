package com.example.dell.notify.services;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.example.dell.notify.R;
import com.example.dell.notify.activities.MainActivity;


public class MyService extends JobService {

    // JobService was introduced in android lollipop
    // it helps us to execute some job not right immediately but when the android system schedules the job to run
    // you may have heavy tasks to do (eg. uploading stuff...) // you decide to run this later
    // it runs on the main thread

    final MainActivity Main=new MainActivity();

    public MyService() {
    }

    // This method is called when the service instance
    // is created
    @Override
    public void onCreate() {
        super.onCreate();
        //keep_sending_notification();   // only when using broadcast receiver
        Log.i("MyService", "myService created");
    }

    // This method is called when the service instance
    // is destroyed
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("MyService", "myService destroyed");
    }

    // This method is called when the scheduled job
    // is started
    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i("MyService", "on start job");
        // do your actual task here
        try{
            // because the notification might not yet be posted
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.cancel(3);  // remove the notification before we use the same notificationID to post notification
        }catch(Exception ex){
            Log.i("Exception",ex.getMessage());
        }
        keep_scheduling_notification(params);
        return true; // true // means it will kick off what is running on the background thread // means it will run on the background thread
    }

    // This method is called when the scheduled job
    // is stopped
    @Override
    public boolean onStopJob(JobParameters params) {
        Log.i("MyService", "on stop job");
        // when the job is stopped by the system
        return true; // true means  that reschedule the job to run later again
    }

    MainActivity myMainActivity;

    public void setUICallback(MainActivity activity) {
        myMainActivity = activity;
    }


    // This method is called when the start command
    // is fired
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Messenger callback = intent.getParcelableExtra("messenger");
        Message m = Message.obtain();
        m.what = 2;
        m.obj = this;
        try {
            callback.send(m);
        } catch (RemoteException e) {
            Log.e("MyService", "Error passing service object back to activity.");
        }
        return START_NOT_STICKY;
    }

    // Method that schedules the job
    public void scheduleJob(JobInfo build) {
        Log.i("MyService","Scheduling job");
        JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(build);
    }

    private void keep_scheduling_notification(JobParameters params){
        if(checkNotificationEnabled()){
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel5")
                    .setSmallIcon(R.drawable.notify_icon)
                    .setColor(getColor(R.color.projectColorCode))
                    .setContentTitle("Notify")
                    .setContentText("Service is running")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(3, builder.build());
            // here let's tell the system that our jo is finished so that it can release the wakelock // save battery life
            jobFinished(params,false); // true // we reschedule the job // so that it will run later again //
        }
    }

    //check notification access setting is enabled or not
    public  boolean checkNotificationEnabled() {
        try{
            if(Settings.Secure.getString(this.getContentResolver(),
                    "enabled_notification_listeners").contains(this.getPackageName())){
                // check if this app is among applications allowed to access notificationListener
                return true;
            } else {
                return false;
            }

        }catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent myServiceIntent = new Intent(this, MyService.class);
        myServiceIntent.putExtra("messenger", new Messenger(Main.myHandler));  // Main.myHandler // may not work
        myServiceIntent.setPackage(getPackageName());
        startService(myServiceIntent);
        super.onTaskRemoved(rootIntent);
    }
}

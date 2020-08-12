package com.muhanzi.notify.services;

/**
 * Created by DELL on 12/27/2019.
 */

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import com.muhanzi.notify.R;
import com.muhanzi.notify.activities.MainActivity;
import com.muhanzi.notify.activities.SplashScreen;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


public class NotificationService extends NotificationListenerService {

    Context context;
    private boolean STATUS_BAR_READ_ONCE =false;
    //
    private static final int NOTIF_ID = 1;
    private static final String NOTIF_CHANNEL_ID = "channel2";
    private TextToSpeech textToSpeech;
    private SharedPreferences sharedpreferences;
    private FirebaseAuth mAuth;
    //
    @Override
    public void onCreate() {  // when notify is allowed to access notifications
        super.onCreate();
        textToSpeech = initializeTextToSpeech();
        context = getApplicationContext();
        sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mAuth = FirebaseAuth.getInstance();
        createNotificationChannel();
        startforeground();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if(!STATUS_BAR_READ_ONCE){
            readActiveNotifications();
            return;
        }
        String packageName = sbn.getPackageName();
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
            if(title.equalsIgnoreCase("chat heads active")){  // messenger stuff // com.facebook.orca
                return;
            }
        }else{
            // title or text is empty
            return;
        }

        Intent msgrcv = new Intent("Msg"); // action --> "Msg"
        msgrcv.putExtra("package", packageName);
        msgrcv.putExtra("ticker", ticker);
        msgrcv.putExtra("title", title);
        msgrcv.putExtra("text", text);
        //
        String sms_package_name=Telephony.Sms.getDefaultSmsPackage(context);
        if(packageName.equals("android") || packageName.equals("com.example.dell.notify") || packageName.equals(sms_package_name)){
            return;
        }

        LocalBroadcastManager.getInstance(context).sendBroadcast(msgrcv);
        // remove the notification from the status bar if app is sms or whatsapp // to avoid the issue of like "2 messages from this person"  // this makes it easy to read messages from whatsapp groups // because whatsapp will be posting them
        if(packageName.equals("com.whatsapp"))
            cancelNotification(sbn.getKey());
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
        // read all active notifications from this app on the status bar // and from others apps
        StatusBarNotification[] sbn= NotificationService.this.getActiveNotifications();  // may be null in case this app has not yet posted any notification on the status bar
        boolean turn_on_notify=sharedpreferences.getBoolean("turn_on_notify",true); // in case sharedpreferences does not provide data, the default value of this boolean we set it to true
        for(StatusBarNotification noti : sbn){
            String packageName = noti.getPackageName();
            Bundle extras = noti.getNotification().extras;

            if(turn_on_notify){
                say_the_text("Notify is now running in the background. Tap to open");
            }
            //
        }
        STATUS_BAR_READ_ONCE=true; // status bar is read already
    }

    ////////////////////////////////////////////////////////////////

    private void startforeground() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

            // a foreground service always displays a notification in the status bar // so that the user is aware of this service which is always running
            this.startForeground(NOTIF_ID, new NotificationCompat.Builder(this,  //!!! I changed this ---> startForeground(NOTIF_ID, new NotificationCompat.Builder(this,
                    NOTIF_CHANNEL_ID)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.notify_icon)
                    .setColor(getColor(R.color.projectColorCode))
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("Notify is running in the background")
                    .setContentIntent(pendingIntent)
                    .build());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(new Intent(getApplicationContext(),TryService.class));
            }else{
                startService(new Intent(getApplicationContext(),TryService.class));
            }
            //
            // start main activity // only if it is not running
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean mainActivityIsActive=sharedpreferences.getBoolean("MainActivityIsActive",true); // in case sharedpreferences does not provide data, the default value of this boolean we set it to true // we assume that the activity is running
            if(!mainActivityIsActive){
                Intent intent = new Intent(this, MainActivity.class);
                intent.setAction(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER); // as if user has clicked on the app to launch it
                startActivity(intent);
                MainActivity mainActivity = MainActivity.instance;
                mainActivity.startNow();
            }
            //
        }else {
            Intent notificationIntent = new Intent(this, SplashScreen.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

            // a foreground service always displays a notification in the status bar // so that the user is aware of this service which is always running
            this.startForeground(NOTIF_ID, new NotificationCompat.Builder(this,  //!!! I changed this ---> startForeground(NOTIF_ID, new NotificationCompat.Builder(this,
                    NOTIF_CHANNEL_ID)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.notify_icon)
                    .setColor(getColor(R.color.projectColorCode))
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("Notify is running in the background")
                    .setContentIntent(pendingIntent)
                    .build());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(new Intent(getApplicationContext(),TryService.class));
            }else{
                startService(new Intent(getApplicationContext(),TryService.class));
            }
        }

    }

    private void say_the_text(String textToSay){
        int speechStatus;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            speechStatus = textToSpeech.speak(textToSay, TextToSpeech.QUEUE_FLUSH, null,null);
        } else {
            speechStatus = textToSpeech.speak(textToSay, TextToSpeech.QUEUE_FLUSH, null);
        }
        if (speechStatus == TextToSpeech.ERROR) {
            Log.e("TTS", "Error in converting Text to Speech!");
        }
    }


    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name2);
            String description = getString(R.string.channel_description2);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(NOTIF_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private TextToSpeech initializeTextToSpeech(){
        TextToSpeech tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    //int ttsLang = textToSpeech.setLanguage(Locale.US);
                    int ttsLang = textToSpeech.setLanguage(Locale.getDefault());
                    textToSpeech.setPitch((float) 1);
                    textToSpeech.setSpeechRate((float) 0.95);
                    if (ttsLang == TextToSpeech.LANG_MISSING_DATA
                            || ttsLang == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "The Language is not supported!");
                    } else {
                        Log.i("TTS", "Language Supported.");
                    }
                    Log.i("TTS", "Initialization success.");
                } else {
                    Toast.makeText(getApplicationContext(), "TTS Initialization failed!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        return tts;
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

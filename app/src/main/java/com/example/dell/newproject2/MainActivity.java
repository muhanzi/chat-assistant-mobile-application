package com.example.dell.newproject2;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements RecognitionListener{

    private Button butt1,butt2,butt4,butt5,butt6;
    public static Button butt3;
    private String phoneNo="0754504768",message="this is a test sms";
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS =0 ;
    private final int REQ_CODE = 100;
    private final int REQ_CODE_FOR_RECORD_AUDIO_PERMISSION = 33;
    private TextToSpeech textToSpeech;
    private String textToSay="this is a test, thank you";
    //
    private SpeechRecognizer mSpeechRecognizer;
    private Intent mSpeechRecognizerIntent;
    //
    private String CHANNEL_ID="1";
    private int notificationId=6; // it will be incrementing
    //
    private final int REQ_CODE_FOR_NOTIFICATION_BIND_SERVICE=9;
    //
    ComponentName myServiceComponent;
    MyService myService;
    Handler myHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            myService = (MyService) msg.obj;
            myService.setUICallback(MainActivity.this);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);  //try this //  worst case scenario
        //
        myServiceComponent = new ComponentName(this, MyService.class);
        Intent myServiceIntent = new Intent(this, MyService.class);
        myServiceIntent.putExtra("messenger", new Messenger(myHandler));
        startService(myServiceIntent);
        //
        setContentView(R.layout.activity_main);
        butt1 =(Button) findViewById(R.id.butt1);
        butt2 =(Button) findViewById(R.id.butt2);
        butt3 =(Button) findViewById(R.id.butt3);
        butt4 =(Button) findViewById(R.id.butt4);
        butt5 =(Button) findViewById(R.id.butt5);
        butt6 =(Button) findViewById(R.id.butt6);
        textToSpeech = initializeTextToSpeech();
        butt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openWhatsApp(view);
            }
        });
        butt2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendSMSMessage();
            }
        });
        butt3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // userStartSpeaking();  // we are not using this button anymore
            }
        });
        butt4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("TTS", "button clicked: " + textToSay);
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
        });

        //
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        //
        // create a recognizer
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);  // this ---> context
        mSpeechRecognizer.setRecognitionListener(this); // this ---> RecognitionListener interface

        //
        butt5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        REQ_CODE_FOR_RECORD_AUDIO_PERMISSION);
            }
        });
        //

        // create notification channel
        createNotificationChannel();
        LocalBroadcastManager.getInstance(this).registerReceiver(allNotificationsReceiver, new IntentFilter("allNotifications"));
        LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, new IntentFilter("Msg"));
        if(checkNotificationEnabled()){
            showNotification();  // Know that a notification can be shown even without the "Notification listener" permission
        }

        keep_sending_notification_after_screen_is_off(); // !!!!!!!!!!!!!!!!!!!!!!!!!!
        keep_broadcast();

        butt6.setOnClickListener(new View.OnClickListener() {  // after enabling notification listener in the settings
            @Override
            public void onClick(View view) {
                startNow();
            }
        });


    }

    private void  startNow(){
        if(checkNotificationEnabled()){
            if(isMyServiceRunning(NotificationService.class)){
                try{
                    unregisterReceiver(allNotificationsReceiver);  // exception may occur in case allNotificationsReceiver wasn't yet registered
                }catch(IllegalArgumentException  ex){
                    Log.i("Exception",ex.getMessage());
                }
                //
                showNotification(); // Know that a notification can be shown even without the "Notification listener" permission
                schedule_the_job(); //  since I added JobScheduler // something better changed
                start_service_with_alarm_manager();
                //check if device is charging
                if(check_if_device_is_charging()){
                    // when phone is charging keep the screen on
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                // register broadcast receiver for phone charger plugged in
                IntentFilter filter = new IntentFilter(Intent.ACTION_POWER_CONNECTED);
                getApplicationContext().registerReceiver(broadcastReceiver_for_charger_plugged, filter);
                // register broadcast receiver for phone charger plugged in
                IntentFilter intentFilter = new IntentFilter(Intent.ACTION_POWER_DISCONNECTED);
                getApplicationContext().registerReceiver(broadcastReceiver_for_charger_unplugged, intentFilter);

                //
            }else{
                // NotificationService is not running
                Toast.makeText(MainActivity.this, "NotificationService is not running", Toast.LENGTH_LONG).show();
                // something like  // try this only and see
                Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                startActivity(intent);
                //
            }
        }else{
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            startActivity(intent);
        }
    }

    private void start_service_with_alarm_manager(){
        Intent intent = new Intent(this, ServiceWithAlarmManager.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
        AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pendingIntent); // comment this
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),5000, pendingIntent); // will be forced up to 60000 millisec
    }

    public boolean check_if_device_is_charging(){
        BatteryManager batteryManager=(BatteryManager) this.getSystemService(Context.BATTERY_SERVICE);
        return batteryManager.isCharging();
    }

    private BroadcastReceiver broadcastReceiver_for_charger_plugged =new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    };

    private BroadcastReceiver broadcastReceiver_for_charger_unplugged =new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    };

    public void sendSMSText(){
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNo, null, message, null, null);
        Toast.makeText(getApplicationContext(), "SMS sent.",Toast.LENGTH_LONG).show();
    }

    protected void sendSMSMessage() {

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.SEND_SMS)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS},
                        MY_PERMISSIONS_REQUEST_SEND_SMS);
            }
        }else{
            sendSMSText();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_SEND_SMS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendSMSText();
                } else {
                    Toast.makeText(getApplicationContext(),"SMS faild, please try again.", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            case REQ_CODE_FOR_RECORD_AUDIO_PERMISSION:  // if permission is allowed then record audio
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                } else {
                  //  Toast.makeText(MainActivity.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
            case REQ_CODE_FOR_NOTIFICATION_BIND_SERVICE:{
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startNow();
                }else {
                    Toast.makeText(getApplicationContext(), "NotificationListener permission denied", Toast.LENGTH_LONG).show();
                }
            }

        }

    }

    public void openWhatsApp(View view){
        try {
            String text = "This is a test";// Replace with your message.

            String toNumber = "256754504768"; // Replace with mobile phone number without +Sign or leading zeros, but with country code
            //Suppose your country is India and your phone number is “xxxxxxxxxx”, then you need to send “91xxxxxxxxxx”.


            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("http://api.whatsapp.com/send?phone="+toNumber +"&text="+text));
            intent.setPackage("com.whatsapp");  // choose whatsapp app
            startActivity(intent);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    // after saying something  // after button3 was clicked and then say something //
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CODE: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    message=result.get(0).toString();
                    if(result.get(0).toString().equals("send message")){  // test a voice command
                        //sendSMSMessage(); // works fine
                        openWhatsApp(new View(getApplicationContext()));
                    }else {
                        Toast.makeText(getApplicationContext(),result.get(0).toString(),Toast.LENGTH_LONG).show();
                    }
                }
                break;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

//        if (textToSpeech != null) {
//            textToSpeech.stop();
//            textToSpeech.shutdown();
//        }
       // unregisterReceiver(allNotificationsReceiver);  // only this receiver

    }

    // for RegisterListener Interface
    @Override
    public void onReadyForSpeech(Bundle bundle) {

    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onRmsChanged(float v) {

    }

    @Override
    public void onBufferReceived(byte[] bytes) {

    }

    @Override
    public void onEndOfSpeech() {

    }

    @Override
    public void onError(int i) {
       // Toast.makeText(getBaseContext(), "error", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList result = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        if(result.get(0).toString().equals("send message")){  // test a voice command
            //sendSMSMessage();
            openWhatsApp(new View(getApplicationContext()));
        }else {
            Toast.makeText(getApplicationContext(),result.get(0).toString(),Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPartialResults(Bundle bundle) {

    }

    @Override
    public void onEvent(int i, Bundle bundle) {

    }
    // ------ end -------

    //  For the LocalBroadcast
    public BroadcastReceiver onNotice= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {  // add it to an arraylist of Strings // then speak one by one
            String pack = intent.getStringExtra("package");
            String title = intent.getStringExtra("title");
            String text = intent.getStringExtra("text");
            int id = intent.getIntExtra("icon",0);  // see the rest of the code in case we need the icon of the notification
           // Toast.makeText(getApplicationContext(),"App: "+pack+", title: "+title+", text: "+text,Toast.LENGTH_LONG).show();
            String sayText=title+" "+text;

            int speechStatus1;
            if(textToSpeech != null){  // I'm doing this because the user may kill "--> onDestroy()"  the mainactivity which initializes the testToSpeech inside the onCreate  // so when our service sends the localBroadcast TextToSpeech will be available
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    speechStatus1 = textToSpeech.speak("you have a new message from "+sayText, TextToSpeech.QUEUE_FLUSH, null,null);
                } else {
                    speechStatus1 = textToSpeech.speak("you have a new message from "+sayText, TextToSpeech.QUEUE_FLUSH, null);
                }

//                new Handler().postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                           // userStartSpeaking();
//                            ActivityCompat.requestPermissions(MainActivity.this,
//                                    new String[]{Manifest.permission.RECORD_AUDIO},
//                                    REQ_CODE_FOR_RECORD_AUDIO_PERMISSION);
//                        }
//                    }, 7000); // after 7 seconds

            }else{
                textToSpeech=initializeTextToSpeech();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    speechStatus1 = textToSpeech.speak("you have a new message from "+sayText, TextToSpeech.QUEUE_FLUSH, null,null);
                } else {
                    speechStatus1 = textToSpeech.speak("you have a new message from "+sayText, TextToSpeech.QUEUE_FLUSH, null);
                }

//                new Handler().postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            ActivityCompat.requestPermissions(MainActivity.this,
//                                    new String[]{Manifest.permission.RECORD_AUDIO},
//                                    REQ_CODE_FOR_RECORD_AUDIO_PERMISSION);
//                        }
//                    }, 7000); // after 7 seconds

            }
            //
            if (speechStatus1 == TextToSpeech.ERROR) {
                Log.e("TTS", "Error in converting Text to Speech!");
            }

        }
    };
    //

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
    //


    private TextToSpeech initializeTextToSpeech(){
        TextToSpeech tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    //int ttsLang = textToSpeech.setLanguage(Locale.US);
                    int ttsLang = textToSpeech.setLanguage(Locale.getDefault());

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


    public void afterBootCompleted(){
        createNotificationChannel();
        //
        if(checkNotificationEnabled()){
            LocalBroadcastManager.getInstance(this).registerReceiver(allNotificationsReceiver, new IntentFilter("allNotifications"));
            showNotification();  // Know that a notification can be shown even without the "Notification listener" permission
        }
    }

    //
    public BroadcastReceiver allNotificationsReceiver= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<String> Notifications= intent.getStringArrayListExtra("notifications");
            //
            for(String notificationText : Notifications){
                int speechStatus1;
                if(textToSpeech != null){  // I'm doing this because the user may kill "--> onDestroy()"  the mainactivity which initializes the testToSpeech inside the onCreate  // so when our service sends the localBroadcast TextToSpeech will be available
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        speechStatus1 = textToSpeech.speak("you have a new message from "+notificationText, TextToSpeech.QUEUE_FLUSH, null,null);
                    } else {
                        speechStatus1 = textToSpeech.speak("you have a new message from "+notificationText, TextToSpeech.QUEUE_FLUSH, null);
                    }
                }else{
                    textToSpeech=initializeTextToSpeech();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        speechStatus1 = textToSpeech.speak("you have a new message from "+notificationText, TextToSpeech.QUEUE_FLUSH, null,null);
                    } else {
                        speechStatus1 = textToSpeech.speak("you have a new message from "+notificationText, TextToSpeech.QUEUE_FLUSH, null);
                    }
                }
                //
                if (speechStatus1 == TextToSpeech.ERROR) {
                    Log.e("TTS", "Error in converting Text to Speech!");
                }
            }

        }
    };
    //
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showNotification(){

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.notif_icon)
                .setContentTitle("Project Notification")
                .setContentText("this text is the content of our notification")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(notificationId, builder.build());
        notificationId++;  // in case u want to post more than one notification // each must have a unique id // from this app
        //
    }

    //
    private  void userStartSpeaking(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Need to speak");
        try {
            startActivityForResult(intent, REQ_CODE);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    "Sorry your device not supported",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // nothing
       // Toast.makeText(this, "this activity should not be stopped", Toast.LENGTH_SHORT).show();
    }

    private void schedule_the_job(){
        JobInfo.Builder builder = new JobInfo.Builder(0, myServiceComponent);
        builder.setRequiresCharging(false);  // the job will run when  the system schedules it // so it doesn't require the device to be charging or not
        builder.setRequiresDeviceIdle(false); // try ---> false  // the device should be in idle mode when it will run
        builder.setPersisted(true); // keep the job alive even if we reboot the device
        builder.setPeriodic(15*60*1000); // each 15 minutes the job will be executed // the interval cannot be less than 15 min // android obliges us to use an interval of 15 minutes or more
        myService.scheduleJob(builder.build());
    }

    private void keep_sending_notification_after_screen_is_off(){
        // broadcastReceiver  // --> // or we can register through the manifest
        BroadcastReceiver br= new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //
//                new Timer().schedule(new TimerTask() {  // this is a timer //  working well // but it runs only one time
//                    @Override
//                    public void run() {
//                        if(checkNotificationEnabled()){
//                            show_notification_after_two_minutes();
//                        }
//                    }
//                },120000);

                // or using handler  // executes something once after a certain delay
                new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if(checkNotificationEnabled()){
;                               final Window window = getWindow();
                                window.addFlags(
//                                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
//                                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON &
                                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
//                                        WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                                );
                                try{
                                    // because the notification might not yet be posted
                                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                                    notificationManager.cancel(2);
                                }catch(Exception e){
                                    Log.i("Exception",e.getMessage());
                                }
                                show_notification_after_two_minutes();
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );
                                    }
                                },5000);
                            }
                        }
                    }, 120000); // after 120 seconds
                //
            }

        };
        // register the broadcast  // with context register
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        getApplicationContext().registerReceiver(br, filter);
    }

    private void keep_broadcast(){
        // broadcastReceiver  // --> // or we can register through the manifest
        BroadcastReceiver br= new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //
//                new Timer().schedule(new TimerTask() {  // this is a timer //  working well // but it runs only one time
//                    @Override
//                    public void run() {
//                        if(checkNotificationEnabled()){
//                            show_notification_after_two_minutes();
//                        }
//                    }
//                },120000);

                // or using handler  // executes something once after a certain delay
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(checkNotificationEnabled()){
                            final Window window = getWindow();
                            window.addFlags(
//                                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
//                                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON &
                                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
//                                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                            );
                            try{
                                // because the notification might not yet be posted
                                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                                notificationManager.cancel(2);
                            }catch(Exception e){
                                Log.i("Exception",e.getMessage());
                            }
                            show_notification_after_two_minutes();
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );
                                }
                            },5000);

                        }
                    }
                }, 120000); // after 120 seconds
                //
            }

        };
        // register the broadcast  // with context register
        IntentFilter filter = new IntentFilter(Intent.ACTION_DREAMING_STARTED);  // just leave it there
        getApplicationContext().registerReceiver(br, filter);
    }

    private void show_notification_after_two_minutes(){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.notif_icon)
                .setContentTitle("Project Notification")
                .setContentText("keep sending notification")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(2, builder.build());
    }

    private boolean isMyServiceRunning(Class<?> MyServiceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MyServiceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}


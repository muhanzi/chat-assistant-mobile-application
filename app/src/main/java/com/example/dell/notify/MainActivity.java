package com.example.dell.notify;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.provider.Telephony;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;


public class MainActivity extends AppCompatActivity implements RecognitionListener{

    private Button button_start_now;
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS =0 ;
    private final int REQ_CODE_FOR_RECORD_AUDIO_PERMISSION = 33;
    private final int REQ_CODE_FOR_RECORD_AUDIO_PERMISSION2 = 34;
    private final int REQ_CODE_FOR_ACCESS_CONTACTS = 27;
    private final int REQ_CODE_PERMISSIONS_FOR_SMS_AUDIO_CONTACTS=55;
    private TextToSpeech textToSpeech;
    //
    private SpeechRecognizer mSpeechRecognizer;
    private Intent mSpeechRecognizerIntent;
    //
    private String CHANNEL_ID="1";
    private int notificationId=6; // it will be incrementing
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
    //
    public static boolean notification_in_process=false;
    private ArrayList<Intent> list_of_notifications =new ArrayList<>();
    //
    private String packageName="",title=""; // packageName--> name of the app from which comes the notification and title --> is the sender of the notification
    private String whatsapp_package_name,sms_package_name,messenger,messenger_lite;
    //
    private Cursor cursor;
    private String sms_to_phone_number="";
    private String contact_name="";
    //
    private AudioManager audioManager;
    //
    private Toolbar tool_bar;
    //
    private String text_to_say="";
    //
    private SharedPreferences sharedpreferences; // stores data in a file
    private Set<String> pending_responses;
    public static MainActivity instance;
    //
    private KeyguardManager keyguardManager;
    //
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //
        tool_bar =(Toolbar) findViewById(R.id.mainActivityAppBar);
        setSupportActionBar(tool_bar);
        getSupportActionBar().setTitle(R.string.appBarTitle);
        //
        //check if device is charging
        sharedpreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String mode=sharedpreferences.getString("mode","");
        if(check_if_device_is_charging() || mode.equals("chatting_mode")){
            // when phone is charging keep the screen on
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        //
        whatsapp_package_name="com.whatsapp";
        sms_package_name= Telephony.Sms.getDefaultSmsPackage(this);
        messenger="com.facebook.orca";
        messenger_lite="com.facebook.mlite";
        //
        myServiceComponent = new ComponentName(this, MyService.class);
        Intent myServiceIntent = new Intent(this, MyService.class);
        myServiceIntent.putExtra("messenger", new Messenger(myHandler));
        startService(myServiceIntent);
        //
        button_start_now =(Button) findViewById(R.id.start_now);
        //
        Set<String> pending_responses_set = sharedpreferences.getStringSet("pending_responses",null); // default value --> null // it means no pending responses
        if(pending_responses_set == null){
            textToSpeech = initializeTextToSpeech();
            textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener);
        }
        //
        create_speech_recognizer();
        //
        audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

        // create notification channel
        createNotificationChannel();
        LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, new IntentFilter("Msg"));
        LocalBroadcastManager.getInstance(this).registerReceiver(onFinishSpeaking, new IntentFilter("Speaking"));
        LocalBroadcastManager.getInstance(this).registerReceiver(pending_responses_finished_receiver, new IntentFilter("pending_responses_finished"));
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver_for_handling_peding_requests, new IntentFilter("handling_pending_responses"));
        if(checkNotificationEnabled()){
            showNotification();  // Know that a notification can be shown even without the "Notification listener" permission
        }

        check_permissions();
        register_broadcasts_for_power_connection();
        // broadcast when user unlock his phone
        register_broadcast_for_phone_unlocked();
        //
        Set<String> empty_set = new HashSet<>();
        pending_responses= sharedpreferences.getStringSet("pending_responses",empty_set); // default value is an empty set // that means there were no pending responses
        //
        keyguardManager = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);

        button_start_now.setOnClickListener(new View.OnClickListener() {  // after enabling notification listener in the settings
            @Override
            public void onClick(View view){
                boolean handling_pending_responses=sharedpreferences.getBoolean("handling_pending_responses",false); // default value --> false
                if(handling_pending_responses){
                    Toast.makeText(MainActivity.this, "Notify has pending responses", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(textToSpeech == null) {  // may be textToSpeech wasn't initialized during onCreate() because notify was handling pending_responses
                    textToSpeech = initializeTextToSpeech();
                    textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener);
                }
                startNow();
            }
        });
        //
        instance=this;

    }

    // the appmenu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.appmenu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.itemSettings:
//                Intent settings=new Intent(MainActivity.this, SettingsActivity.class);
//                startActivity(settings);
                return true;
            case R.id.itemTurnOff:
                // Turn Notify off
                turnOffNotify();
                return true;
            case R.id.itemReportedSpams:
                // open activity to see all reported spams
                return true;
            case R.id.itemChangeMode:
                change_mode();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    //

    private void change_mode(){
        String mode=sharedpreferences.getString("mode","");
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Change Mode ?");
        //
        String message;
        if(mode.equals("chatting_mode")){
            message="you are currently using CHATTING mode";
            dialog.setMessage(message);
            dialog.setPositiveButton("disable chatting", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString("mode","");
                    editor.commit();
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    Toast.makeText(MainActivity.this, "you have just switched to Default mode ", Toast.LENGTH_SHORT).show();
                }
            });
            dialog.setNegativeButton("sms mode", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString("mode","sms_mode");
                    editor.commit();
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // let the screen to go off
                    Toast.makeText(MainActivity.this, "you have just switched to SMS mode ", Toast.LENGTH_SHORT).show();
                }
            });
        }else if(mode.equals("sms_mode")){
            message="you are currently using SMS mode";
            dialog.setMessage(message);
            dialog.setPositiveButton("chatting mode", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString("mode","chatting_mode");
                    editor.commit();
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // works fine
                    Toast.makeText(MainActivity.this, "you have just switched to Chatting mode ", Toast.LENGTH_SHORT).show();
                }
            });
            dialog.setNegativeButton("disable sms", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString("mode","");
                    editor.commit();
                    Toast.makeText(MainActivity.this, "you have just switched to Default mode ", Toast.LENGTH_SHORT).show();
                }
            });
        }else{
            message="you are in DEFAULT mode";
            dialog.setMessage(message);
            dialog.setPositiveButton("chatting mode", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString("mode","chatting_mode");
                    editor.commit();
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // works fine
                    Toast.makeText(MainActivity.this, "you have just switched to Chatting mode ", Toast.LENGTH_SHORT).show();
                }
            });
            dialog.setNegativeButton("sms mode", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString("mode","sms_mode");
                    editor.commit();
                    Toast.makeText(MainActivity.this, "you have just switched to SMS mode ", Toast.LENGTH_SHORT).show();
                }
            });
        }
        //
        dialog.show();
    }

    private void turnOffNotify(){
        // Turn Notify off
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putBoolean("turn_on_notify",false);
        editor.apply();  // will save it in the background
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        button_start_now.setText(R.string.start_now);
        button_start_now.setEnabled(true);
        //
        notification_in_process=false;// processing is stopped
    }

    @Override
    protected void onResume() {
        super.onResume();
        //check if device is charging
        String mode=sharedpreferences.getString("mode","");
        if(check_if_device_is_charging() || mode.equals("chatting_mode")){
            // when phone is charging keep the screen on
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        // broadcasts
        register_broadcast_for_phone_unlocked();
        //
        if(mSpeechRecognizer == null){
            create_speech_recognizer();
        }
        change_sleep_timeout();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //check if device is charging
        String mode=sharedpreferences.getString("mode","");
        if(check_if_device_is_charging() || mode.equals("chatting_mode")){
            // when phone is charging keep the screen on
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        boolean turn_on_notify=sharedpreferences.getBoolean("turn_on_notify",true); // in case sharedpreferences does not provide data, the default value of this boolean we set it to true
        if(isMyServiceRunning(NotificationService.class)){
            if(isMyServiceRunning(ServiceWithAlarmManager.class)){
                if(turn_on_notify){
                    button_start_now.setText(R.string.notify_is_running);
                    button_start_now.setEnabled(false);
                    if(textToSpeech == null) {  // may be textToSpeech wasn't initialized during onCreate() because notify was handling pending_responses
                        textToSpeech = initializeTextToSpeech();
                        textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener);
                    }
                }else{  // all services are running but Notify was turned off
                    button_start_now.setText(R.string.start_now);
                    button_start_now.setEnabled(true);
                }
            }else{
                button_start_now.setText(R.string.start_now);
                button_start_now.setEnabled(true);
            }
        }else{
            button_start_now.setText(R.string.start_now);
            button_start_now.setEnabled(true);
        }
        //
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putBoolean("MainActivityIsActive",true);
        editor.commit();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        //check if device is charging
        if(check_if_device_is_charging()){
            // when phone is charging keep the screen on
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        //
    }

    public void  startNow(){
        if(checkNotificationEnabled()){
            //
            // Turn Notify on  // in case user turned it off // only in that case
            boolean turn_on_notify=sharedpreferences.getBoolean("turn_on_notify",true);
            if(!turn_on_notify){
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putBoolean("turn_on_notify",true);
                editor.commit();
            }
            if(!list_of_notifications.isEmpty()){
                list_of_notifications.clear();  // starting afresh // to avoid endless notifications like during phone call or when downloading
            }
            //
            if(isMyServiceRunning(NotificationService.class)){
                //
                showNotification(); // Know that a notification can be shown even without the "Notification listener" permission
                start_service_with_alarm_manager();
                schedule_the_job(); //  since I added JobScheduler // something better changed
                //
                if(checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED){
                    cursor = getApplicationContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
                }
                //
                button_start_now.setText(R.string.notify_is_running);
                button_start_now.setEnabled(false);
            }else{
                // NotificationService is not running
                Toast.makeText(MainActivity.this, "Notify is not running, please restart notification access", Toast.LENGTH_LONG).show();
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
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),60000, pendingIntent); //5000 // will be forced up to 60000 milliseconds by android system
    }

    public boolean check_if_device_is_charging(){
        BatteryManager batteryManager=(BatteryManager) this.getSystemService(Context.BATTERY_SERVICE);
        return batteryManager.isCharging();
    }

    private void register_broadcasts_for_power_connection(){
        // register broadcast receiver for phone charger plugged in
        IntentFilter filter = new IntentFilter(Intent.ACTION_POWER_CONNECTED);
        getApplicationContext().registerReceiver(broadcastReceiver_for_charger_plugged, filter);
        // register broadcast receiver for phone charger plugged in
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_POWER_DISCONNECTED);
        getApplicationContext().registerReceiver(broadcastReceiver_for_charger_unplugged, intentFilter);
    }

    private BroadcastReceiver broadcastReceiver_for_charger_plugged =new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    };

    public BroadcastReceiver broadcastReceiver_for_charger_unplugged =new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    };

    public void sendSMSText(String message){
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(sms_to_phone_number, null, message, null, null);
        //
        Handler handler=new Handler();
        Toast.makeText(getApplicationContext(), "SMS sent.",Toast.LENGTH_LONG).show();
        int speechStatus = textToSpeech.speak("message sent", TextToSpeech.QUEUE_FLUSH, null, null);
        while(textToSpeech.isSpeaking()){
            Log.i("sms","response");
        }
        if(!textToSpeech.isSpeaking()){
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try{
                        list_of_notifications.remove(0);
                        if(!list_of_notifications.isEmpty()){
                            process_notification(); // process the intent which is now on the position 0
                        }else{
                            audioManager.abandonAudioFocus(audioFocusChangeListener);
                            notification_in_process=false; // after processing all intents inside the arraylist
                        }
                    }catch (IndexOutOfBoundsException ex){
                        Log.e("Exception","IndexOutOfBoundsException index at 0 does not exist //sendSMSText ");
                    }
                }
            },5000); // after 5 seconds proceed with next intent
        }
        //
    }

    protected void sendSMSMessage(String message) {

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.SEND_SMS)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS},
                        MY_PERMISSIONS_REQUEST_SEND_SMS);
            }
        }else{
            if (textToSpeech == null) {  // I'm doing this because the user may kill "--> onDestroy()"  the mainactivity which initializes the testToSpeech inside the onCreate  // so when our service sends the localBroadcast TextToSpeech will be available
                textToSpeech = initializeTextToSpeech();
                textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener);
            }
            sendSMSText(message);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_SEND_SMS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Notify is allowed to send sms", Toast.LENGTH_SHORT).show();
                    // if user allows now the app to send sms // now reprocess the intent at index 0
                    process_notification();
                } else {
                    Toast.makeText(getApplicationContext(),"SMS failed, please try again.", Toast.LENGTH_LONG).show();
                    //
                    try{
                        list_of_notifications.remove(0);
                        if(!list_of_notifications.isEmpty()){
                            process_notification(); // process the intent which is now on the position 0
                        }else{
                            audioManager.abandonAudioFocus(audioFocusChangeListener);
                            notification_in_process=false; // after processing all intents inside the arraylist
                        }
                    }catch (IndexOutOfBoundsException ex){
                        Log.e("Exception","IndexOutOfBoundsException index at 0 does not exist // MY_PERMISSIONS_REQUEST_SEND_SMS ");
                    }
                    //
                }
                break;
            }
            case REQ_CODE_FOR_RECORD_AUDIO_PERMISSION:  // if permission is allowed then record audio
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(SpeechRecognizer.isRecognitionAvailable(this)){ // just try and see
                        mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                    }else{
                        Toast.makeText(this, "you device does not support speech recognition", Toast.LENGTH_LONG).show();
                        remove_intent_after_delay();
                    }
                }else{
                    record_audio_denied();
                }
                break;
            case REQ_CODE_FOR_RECORD_AUDIO_PERMISSION2:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Toast.makeText(this, "permission is granted ok ok", Toast.LENGTH_SHORT).show();
                    Log.i("RecordAudio","permission is granted ok ok");
                }else{
                    //Toast.makeText(this, "permission to record audio is denied", Toast.LENGTH_SHORT).show();
                    Log.i("RecordAudio","permission to record audio is denied");
                }
                break;
            case REQ_CODE_FOR_ACCESS_CONTACTS:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    cursor = getApplicationContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
                    LoadContact loadContact = new LoadContact();
                    loadContact.execute();
                }else{
                    Toast.makeText(getApplicationContext(),"Notify is denied access to phone contacts", Toast.LENGTH_LONG).show();
                    //
                    try{
                        list_of_notifications.remove(0);
                        if(!list_of_notifications.isEmpty()){
                            process_notification(); // process the intent which is now on the position 0
                        }else{
                            audioManager.abandonAudioFocus(audioFocusChangeListener);
                            notification_in_process=false; // after processing all intents inside the arraylist
                        }
                    }catch (IndexOutOfBoundsException ex){
                        Log.e("Exception","IndexOutOfBoundsException index at 0 does not exist // REQ_CODE_FOR_ACCESS_CONTACTS");
                        audioManager.abandonAudioFocus(audioFocusChangeListener);
                        notification_in_process=false;
                    }
                    //
                }
                break;
            case REQ_CODE_PERMISSIONS_FOR_SMS_AUDIO_CONTACTS:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "permissions allowed", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(getApplicationContext(),"permissions denied", Toast.LENGTH_LONG).show();
                }
                break;
            default:
                return;
        }

    }

    private void check_record_audio_permission(){ //permission is denied so many times
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                REQ_CODE_FOR_RECORD_AUDIO_PERMISSION);
    }

    private void check_access_contacts_permission() {
        if(checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, REQ_CODE_FOR_ACCESS_CONTACTS);
        }else{
            // permission is already allowed
            cursor = getApplicationContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
            LoadContact loadContact = new LoadContact();
            loadContact.execute();
        }
    }

    private void check_permissions(){
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.SEND_SMS,Manifest.permission.READ_SMS,Manifest.permission.RECEIVE_SMS,Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_CONTACTS},REQ_CODE_PERMISSIONS_FOR_SMS_AUDIO_CONTACTS);
    }

    private void process_intent_again(){
        Handler handler=new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                process_notification();
                //
            }
        },30000);
    }

    private void send_message_on_whatsapp(final String text){
        if (textToSpeech == null) {  // I'm doing this because the user may kill "--> onDestroy()"  the mainactivity which initializes the testToSpeech inside the onCreate  // so when our service sends the localBroadcast TextToSpeech will be available
            textToSpeech = initializeTextToSpeech();
            textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener);
        }
        final Intent intent=new Intent(Intent.ACTION_VOICE_COMMAND);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        final Handler handler=new Handler();
        handler.postDelayed(new Runnable(){
            @Override
            public void run(){
                int speechStatus = textToSpeech.speak("send whatsapp message to "+title+"  "+text, TextToSpeech.QUEUE_FLUSH, null, null);
                while(textToSpeech.isSpeaking()){  // works well // wait until it finishes talking
                    Log.i("tts","text to speech");
                }
                handler.postDelayed(new Runnable(){
                    @Override
                    public void run(){
                        // bring MainActivity to the foreground // so that notification service stays alive
                        bring_main_activity_to_foreground();
                        //
                        try{
                            textToSpeech.stop();
                            list_of_notifications.remove(0);
                            if(!list_of_notifications.isEmpty()){
                                process_notification(); // process the intent which is now on the position 0
                            }else{
                                audioManager.abandonAudioFocus(audioFocusChangeListener);
                                notification_in_process=false; // after processing all intents inside the arraylist
                            }
                        }catch (IndexOutOfBoundsException ex){
                            Log.e("Exception","IndexOutOfBoundsException index at 0 does not exist // send_message_on_whatsapp() main activity");
                        }
                    }
                },80000); // give google assistant 80 seconds to process
            }
        },3000); // 3 secs// just wait for google assistant to get ready
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        // when the activity is killed
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putBoolean("MainActivityIsActive",false);
        editor.commit();
        mSpeechRecognizer.destroy();  // SpeechRecognizer uses a single instance only
        // when user clicks back button onDestroy() will be called that means we need to destroy the object of SpeechRecognizer so that when the activity launches again the SpeechRecognizer recreates the instance
        // during onPause() SpeechRecognizer will clean up its object // remember onPause() comes before onDestroy()
        // still SpeechRecognizer doesn't work when the activity is created again // !!!!!!!! // try again
    }

    // for RegisterListener Interface
    @Override
    public void onReadyForSpeech(Bundle bundle) {
        notification_in_process=true; // don't read the next intent in the arrayList
    }

    @Override
    public void onBeginningOfSpeech() {
        notification_in_process=true;
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

    /**
     Here's the log statements right before the ERROR_CLIENT is sent to onError
     // because the MainActivity was recreated // the SpeechRecognizer object was cleaned up
     Log.e(TAG, "no selected voice recognition service");
     Log.e(TAG, "bind to recognition service failed");
     Log.e(TAG, "startListening() failed", e);
     Log.e(TAG, "stopListening() failed", e);
     Log.e(TAG, "cancel() failed", e);
     Log.e(TAG, "not connected to the recognition service");
     */

    @Override
    public void onError(int errorCode) {
        empty_variables();
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error"; // fails to record
                //Toast.makeText(this, "RecognitioNListener error: "+message, Toast.LENGTH_SHORT).show();
                Log.e("onError","RecognitioNListener error: "+message);
                remove_intent_after_delay();
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "other Client side error";
                //Toast.makeText(this, "RecognitioNListener error: "+message, Toast.LENGTH_SHORT).show();
                Log.e("onError","RecognitioNListener error: "+message);
                remove_intent_after_delay();
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                //Toast.makeText(this, "RecognitioNListener error: "+message, Toast.LENGTH_SHORT).show();
                Log.e("onError","RecognitioNListener error: "+message);
                remove_intent_after_delay();
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                //Toast.makeText(this, "RecognitioNListener error: "+message, Toast.LENGTH_SHORT).show();
                Log.e("onError","RecognitioNListener error: "+message);
                remove_intent_after_delay(); //
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                //Toast.makeText(this, "RecognitioNListener error: "+message, Toast.LENGTH_SHORT).show();
                Log.e("onError","RecognitioNListener error: "+message);
                process_intent_again(); //
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                //Toast.makeText(this, "RecognitioNListener error: "+message, Toast.LENGTH_SHORT).show();
                Log.e("onError","RecognitioNListener error: "+message);
                process_intent_again(); //
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                //Toast.makeText(this, "RecognitioNListener error: "+message, Toast.LENGTH_SHORT).show();
                Log.e("onError","RecognitioNListener error: "+message);
                process_intent_again(); //
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                //Toast.makeText(this, "RecognitioNListener error: "+message, Toast.LENGTH_SHORT).show();
                Log.e("onError","RecognitioNListener error: "+message);
                process_intent_again(); //
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";  // no response
                //Toast.makeText(this, "RecognitioNListener error: "+message, Toast.LENGTH_SHORT).show();
                Log.e("onError","RecognitioNListener error: "+message);
                remove_intent_after_delay();
                break;
            default:
                message = "Didn't understand, please try again.";
                //Toast.makeText(this, "RecognitioNListener error: "+message, Toast.LENGTH_SHORT).show();
                Log.e("onError","RecognitioNListener error: "+message);
                process_intent_again(); //
                break;
        }
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList result = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String response=result.get(0).toString();
        String mode=sharedpreferences.getString("mode","");
        Handler handler=new Handler();
        if(packageName.equals(whatsapp_package_name)){
            if(isPhoneLocked()){
                if(mode.equals("sms_mode")){
                    sendSMSMessage(response); // send sms
                }else{
                    // either in chatting mode // or no mode // user has not yet activated any mode
                    //
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    pending_responses.add("send whatsapp message to "+title+"  "+response);  // add response to the set
                    editor.putStringSet("pending_responses",pending_responses);  // save the set
                    editor.apply();
                    //
                    text_to_say="your phone is locked, this message will be sent when you unlock it";
                    Speak speak=new Speak();
                    speak.execute();
                    //
                    // save the message in a set of strings inside shared preferences // when phone is unlocked it will send these messages
                }
            }else{
                if(mode.equals("sms_mode")){
                    sendSMSMessage(response); // send sms
                }else{
                    // either in chatting mode // or no mode // user has not yet activated any mode
                    send_message_on_whatsapp(response);// send directly to whatsapp
                }
            }
            //
        }else if(packageName.equals(sms_package_name)){
            sendSMSMessage(response); //
        }else if(packageName.equals(messenger)){
            // reply to messenger
            if(!isPhoneLocked()){
                sendToMessenger(response,messenger);
            }else{
                text_to_say="your phone is locked, please switch to chatting mode to enable notify to send your response on messenger";
                Speak speak=new Speak();
                speak.execute();
            }
            //
        }else if(packageName.equals(messenger_lite)){
            // reply to messenger lite
            if(!isPhoneLocked()){
                sendToMessenger(response,messenger_lite);
            }else{
                text_to_say="your phone is locked, please switch to chatting mode to enable notify to send your response to messenger lite";
                Speak speak=new Speak();
                speak.execute();
            }
            //
        }else{
            //proceed with next intent in the list
            try{
                list_of_notifications.remove(0);
                if(!list_of_notifications.isEmpty()){
                    process_notification(); // process the intent which is now on the position 0
                }else{
                    audioManager.abandonAudioFocus(audioFocusChangeListener);
                    notification_in_process=false; // after processing all intents inside the arraylist
                }
            }catch (IndexOutOfBoundsException ex){
                Log.e("Exception","IndexOutOfBoundsException index at 0 does not exist // onResults() of record audio else branch // not whatsapp ,not sms, not messenger");
            }
            //
        }
    }

    @Override
    public void onPartialResults(Bundle bundle) {

    }

    @Override
    public void onEvent(int i, Bundle bundle) {

    }
    // ------ end -------

    private void remove_intent(){
        try{
            list_of_notifications.remove(0);
            if(!list_of_notifications.isEmpty()){
                process_notification(); // process the intent which is now on the position 0
            }else{
                audioManager.abandonAudioFocus(audioFocusChangeListener);
                notification_in_process=false; // after processing all intents inside the arraylist
            }
        }catch (IndexOutOfBoundsException ex){
            Log.e("Exception","IndexOutOfBoundsException index at 0 does not exist // remove_intent() ");
        }
        //
    }

    //  For the LocalBroadcast
    public BroadcastReceiver onNotice= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean turn_on_notify=sharedpreferences.getBoolean("turn_on_notify",true);
            if(turn_on_notify){  // proceed only if turn_on_notify==true
                list_of_notifications.add(intent);
                if(!notification_in_process){
                    process_notification();
                }
            }
        }
    };
    //

    private void process_notification(){
        boolean turn_on_notify=sharedpreferences.getBoolean("turn_on_notify",true);
        if(!turn_on_notify) {  // if turn_on_notify==false // return
            return;
        }

        if(request_audio_focus()) {
            // Notification in process
            notification_in_process = true;
            //
            contact_name = "";  // the contact name must be fetched every time we process message //intent
            try{
                Intent intent = list_of_notifications.get(0);
                packageName = intent.getStringExtra("package");
                title = intent.getStringExtra("title");
                String text = intent.getStringExtra("text");
                String sayText = title + ": " + text;
                //
                if(packageName.equals(whatsapp_package_name) || packageName.equals(sms_package_name) || packageName.equals(messenger) || packageName.equals(messenger_lite)){
                    Intent speakService=  new Intent(this, SpeakTextService.class);
                    speakService.putExtra("textToSay","you have received  a new message from"+ sayText);
                    speakService.putExtra("type","message");
                    startService(speakService);
                }else{
                    // package name does not require any response // we don't provide response for other apps like browsers,...
                    Intent speakService=  new Intent(this, SpeakTextService.class);
                    speakService.putExtra("textToSay","you have received  a new notification: "+ sayText);
                    speakService.putExtra("type","notification");
                    speakService.putExtra("notification",text);
                    speakService.putExtra("package",packageName);
                    speakService.putExtra("title",title);
                    startService(speakService);
                }
                //
            }catch(IndexOutOfBoundsException ex){
                Log.e("IndexOutOfBounds","exception inside process_notification()");
            }
        }else{
            // audio focus can be denied // for example during phone call
            notification_in_process=false;    // this intent will be processed again when a new intent arrives  // works fine
            //keep the intent in the arrayList
            Log.e("AudioFocus","Audio focus permission is denied");
            Toast.makeText(this, "Audio focus permission is denied", Toast.LENGTH_SHORT).show();
        }
        //
    }

    public BroadcastReceiver onFinishSpeaking= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean handling_pending_responses=sharedpreferences.getBoolean("handling_pending_responses",false); // default value --> false
            if(handling_pending_responses){
                return;
            }
            notification_in_process=true; // but no need for it here
            contact_name=""; // but no need for it here
            Intent speakService=  new Intent(getApplicationContext(), SpeakTextService.class);
            stopService(speakService); // does not cause crashing
            //
            continue_process_notification();
        }
    };

    private void continue_process_notification(){
        if(textToSpeech == null) {  // I'm doing this because the user may kill "--> onDestroy()"  the mainactivity which initializes the testToSpeech inside the onCreate  // so when our service sends the localBroadcast TextToSpeech will be available
            textToSpeech = initializeTextToSpeech();
            textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener);
        }
        //
        if(packageName.equals(whatsapp_package_name)){
            check_access_contacts_permission();
        }else if(packageName.equals(sms_package_name)){
            try {
                sms_to_phone_number=list_of_notifications.get(0).getStringExtra("number");
                if(isValidPhoneNumber()){
                    int speechStatus = textToSpeech.speak(" if you would like to reply, please say your message:", TextToSpeech.QUEUE_FLUSH, null, null);
                    while(textToSpeech.isSpeaking()){  // works well // wait until it finishes talking
                        Log.i("tts","text to speech // SMS Message");
                    }
                    if(!textToSpeech.isSpeaking()) {
                        check_record_audio_permission();
                    }else{
                        textToSpeech.stop();
                        check_record_audio_permission();
                    }
                }else{
                    remove_intent_after_delay();
                }
            }catch (IndexOutOfBoundsException e){
                Log.i("Exception","IndexOutOfBoundsException // continue_process_notification // packageName --> sms");
            }
        }else if(packageName.equals(messenger) || packageName.equals(messenger_lite)){
            int speechStatus = textToSpeech.speak(" if you would like to reply, please say your message:", TextToSpeech.QUEUE_FLUSH, null, null);
            while(textToSpeech.isSpeaking()){  // works well // wait until it finishes talking
                Log.i("tts","text to speech");
            }
            if(!textToSpeech.isSpeaking()) {
                check_record_audio_permission();

            }else{
                textToSpeech.stop();
                check_record_audio_permission();

            }
        }else{
            // package name does not require any response // we don't provide response for other apps like browsers,...
            Handler handler=new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try{
                        list_of_notifications.remove(0);
                        if(!list_of_notifications.isEmpty()){
                            process_notification(); // process the intent which is now on the position 0
                        }else{
                            audioManager.abandonAudioFocus(audioFocusChangeListener);
                            notification_in_process=false; // after processing all intents inside the arraylist
                        }
                    }catch (IndexOutOfBoundsException ex){
                        Log.e("Exception","IndexOutOfBoundsException index at 0 does not exist // continue_processing() // package name does not require any response");
                    }
                }
            },5000);
        }
        //
    }

    // to Handle the Loss of Audio Focus  // audio was playing then we lose audio focus because it is requested by another app
    AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {

        }
    };

    private boolean request_audio_focus(){
        int result = audioManager.requestAudioFocus(audioFocusChangeListener,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return true;
        }
        return false;
    }

    //check notification access setting is enabled or not
    public  boolean checkNotificationEnabled() {
        try{
            if(Settings.Secure.getString(this.getContentResolver(),
                    "enabled_notification_listeners").contains(this.getPackageName())){
                // check if this app is among applications allowed to access notificationListener
                return true;
            }else{
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
                    textToSpeech.setPitch((float) 1); // is the tone
                    textToSpeech.setSpeechRate((float) 0.92);  // speed
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

    private UtteranceProgressListener utteranceProgressListener=new UtteranceProgressListener() {
        @Override
        public void onStart(String s) {

        }

        @Override
        public void onDone(String s) {

        }

        @Override
        public void onError(String s) {
            // to prevent ANR dialog (application not responding)  // when textToSpeech is may be reading a long text or its used many times in a short period
            textToSpeech.stop();  // Interrupts the current utterance (whether played or rendered to file) and discards other utterances in the queue.
            textToSpeech.shutdown(); // Releases the resources used by the TextToSpeech engine.
            Log.e("TTS","onError() inside main activity // during text to speech");
            Toast.makeText(MainActivity.this, "Text to speech encountered an error", Toast.LENGTH_LONG).show();
        }
    };

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
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
                .setSmallIcon(R.drawable.notify_icon)
                .setColor(getColor(R.color.projectColorCode))
                .setContentTitle("Notify")
                .setContentText("your chat assistant")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(notificationId, builder.build());
        notificationId++;  // in case u want to post more than one notification // each must have a unique id // from this app
        //
    }

    @Override
    public void onBackPressed() {
        //to disable back button // just remove  super.onBackPressed();
        Toast.makeText(this, "please use the home button to leave the app", Toast.LENGTH_LONG).show();
        vibrate_phone();
        return;
    }

    private void schedule_the_job(){
        JobInfo.Builder builder = new JobInfo.Builder(0, myServiceComponent);
        builder.setRequiresCharging(false);  // the job will run when  the system schedules it // so it doesn't require the device to be charging or not
        builder.setRequiresDeviceIdle(false); // try ---> false  // the device should be in idle mode when it will run
        builder.setPersisted(true); // keep the job alive even if we reboot the device
        builder.setPeriodic(15*60*1000); // each 15 minutes the job will be executed // the interval cannot be less than 15 min // android obliges us to use an interval of 15 minutes or more
        myService.scheduleJob(builder.build());
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

    //
    class LoadContact extends AsyncTask<Void, Void, Void> {   // to execute tasks on the worker thread
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            // Get Contact list from Phone
            if (cursor.getCount() != 0) {
                try{
                    String name;
                    while (cursor.moveToNext()) {
                        name=cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)).trim().replaceAll("\\s","");
                        Log.e("whatsapp","title is: "+title+" contact: "+name);
                        if(matchContactName(title.trim().replaceAll("\\s",""),name)){
                            Log.e("MessageTitle","message title was found in contacts");
                            contact_name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                            sms_to_phone_number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)).replaceAll("\\s","");
                            break;
                        }else{
                            Log.e("MessageTitle","message title did not match current contact");
                        }
                    }
                }catch(CursorIndexOutOfBoundsException ex){
                    Log.e("cursor","CursorIndexOutOfBoundsException occurred");
                }
            }else {
                Log.e("Cursor ", "Number of contacts is 0");
            }
            //

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //
            if(!contact_name.equals("")){ // means the title was found in our contacts
                if(isValidPhoneNumber()){
                    start_recording();
                }else{
                    // phone number is invalid so we just continue with next intent
                    // or number is saved in the contacts but is it not a valid phone number
                    Log.e("whatsapp","contact found but has invalid phone number");
                    remove_intent_after_delay();
                }
            }else{
                //for example if message comes from a Whatsapp groups // delay for 5 secs then continue with next message // or for example : message from whatsapp 4 messages from 2 chats
                // or it is a whatsapp message but from an unknown number
                Log.e("whatsapp","title was not found in contacts");
                remove_intent_after_delay();
                // !!!! we cannot create a Handler here // because we cannot create handler inside a thread that has not called looper.prepare() // inside Async class // worker thread
            }
            //
        }
    }

    private void start_recording(){
        int speechStatus = textToSpeech.speak(" if you would like to reply, please say your message:", TextToSpeech.QUEUE_FLUSH, null, null);
        while(textToSpeech.isSpeaking()){  // works well // wait until it finishes talking
            Log.i("tts","text to speech // whatsapp // if you would...");
        }
        if(!textToSpeech.isSpeaking()) {
            check_record_audio_permission();
        }else{
            textToSpeech.stop();
            check_record_audio_permission();
        }
    }
    //

    private boolean isValidPhoneNumber(){
        // we remove + sign so that we can filter digits // to avoid bulk sms
        String number=sms_to_phone_number.replace("+",""); // first remove the + sign then verify the left characters
        if(number.length() >= 10 && number.length() <= 12){
            // check if number has letters // or check if it is a bulk sms likely with letters or other special characters
            Log.i("PhoneNumber","current value of phone number is: "+number);
            if(number.matches("[0-9]+")){
                return true;
            }
        }
        //
        return false;
    }

    private Handler handler=new Handler();

    private void remove_intent_after_delay(){
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try{
                    list_of_notifications.remove(0);
                    if(!list_of_notifications.isEmpty()){
                        process_notification(); // process the intent which is now on the position 0
                    }else{
                        audioManager.abandonAudioFocus(audioFocusChangeListener);
                        notification_in_process=false; // after processing all intents inside the arraylist
                    }
                }catch (IndexOutOfBoundsException ex){
                    Log.e("Exception","IndexOutOfBoundsException index at 0 does not exist // remove_intent_after_delay() ");
                }
            }
        },5000); // delay of 5 seconds
    }

    private boolean matchContactName(String message_title, String contact_name){
        boolean characters_are_equal=false;
        Log.e("whatsapp","current  message_title is: "+message_title+" contact_name is: "+contact_name);
        if(!message_title.isEmpty() && !contact_name.isEmpty()){
            Log.e("whatsapp","message_title and contact_name are not empty");
            if(message_title.length() == contact_name.length()){
                Log.e("whatsapp","message_title and contact_name have the same length");
                for(int i=0;i<message_title.length();i++){
                    Character ch1=message_title.charAt(i);
                    Character ch2=contact_name.charAt(i);
                    Log.e("whatsapp","ch1 is: "+ch1+" ch2 is: "+ch2);
                    if(ch1.equals(ch2)){
                        Log.e("whatsapp","ch1 is: and ch2 are equal");
                        characters_are_equal=true;
                    }else{
                        Log.e("whatsapp","ch1 is: and ch2 are different");
                        characters_are_equal=false;
                        break;
                    }
                }
                Log.e("whatsapp","characters_are_equal : "+characters_are_equal);
            }else{
                Log.e("whatsapp","message_title and contact_name have different length");
            }
        }else {
            Log.e("whatsapp","message_title or contact_name / empty ");
        }

        return characters_are_equal;
    }

    //====
    class Speak extends AsyncTask<Void, Void, Void> {   // to execute tasks on the worker thread
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            int speechStatus = textToSpeech.speak(text_to_say, TextToSpeech.QUEUE_FLUSH, null, null);
            //
            while(textToSpeech.isSpeaking()){
                Log.i("tts","text to speech //doInBackground() --> Speak");
            }
            //
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //
            if(!textToSpeech.isSpeaking()) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            list_of_notifications.remove(0);
                            if(!list_of_notifications.isEmpty()){
                                process_notification(); // process the intent which is now on the position 0
                            }else{
                                audioManager.abandonAudioFocus(audioFocusChangeListener);
                                notification_in_process=false; // after processing all intents inside the arraylist
                            }
                        }catch (IndexOutOfBoundsException ex){
                            Log.e("Exception","IndexOutOfBoundsException index at 0 does not exist // Speak async class // if(!textToSpeech.isSpeaking())");
                        }
                    }
                },5000);
            }else{
                textToSpeech.stop();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            list_of_notifications.remove(0);
                            if(!list_of_notifications.isEmpty()){
                                process_notification(); // process the intent which is now on the position 0
                            }else{
                                audioManager.abandonAudioFocus(audioFocusChangeListener);
                                notification_in_process=false; // after processing all intents inside the arraylist
                            }
                        }catch (IndexOutOfBoundsException ex){
                            Log.e("Exception","IndexOutOfBoundsException index at 0 does not exist // Speak async class // if(textToSpeech.isSpeaking())");
                        }
                    }
                },5000);
            }
        }
    }

    private boolean isPhoneLocked(){
        return keyguardManager.isDeviceLocked() || keyguardManager.isKeyguardLocked() || keyguardManager.inKeyguardRestrictedInputMode();
    }

    private void sendToMessenger(String message,String messengerPackageName){
        Handler handler=new Handler();
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, message);
        sendIntent.setType("text/plain");
        sendIntent.setPackage(messengerPackageName);
        startActivity(sendIntent);
        //
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bring_main_activity_to_foreground();
                try {
                    list_of_notifications.remove(0);
                    if(!list_of_notifications.isEmpty()){
                        process_notification(); // process the intent which is now on the position 0
                    }else{
                        audioManager.abandonAudioFocus(audioFocusChangeListener);
                        notification_in_process=false; // after processing all intents inside the arraylist
                    }
                }catch (IndexOutOfBoundsException ex){
                    Log.e("Exception","IndexOutOfBoundsException error occurred // sendToMessenger()");
                }
            }
        },30000);  // after 30 seconds process next intent
    }

    private BroadcastReceiver phoneUnlockedReceiver = new PhoneUnlockedBroadcastReceiver();

    private void register_broadcast_for_phone_unlocked(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(phoneUnlockedReceiver, filter);
    }

    public void bring_main_activity_to_foreground(){
        // move the app to the foreground
        //just to bring back our app to the foreground
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                REQ_CODE_FOR_RECORD_AUDIO_PERMISSION2);
        //
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(phoneUnlockedReceiver);
    }

    BroadcastReceiver pending_responses_finished_receiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(list_of_notifications.isEmpty()){
                notification_in_process=false;
            }else{
                process_notification(); // after handling pending responses // now process the intnet that just arrived in that time of handling pending responses
            }
        }
    };

    BroadcastReceiver receiver_for_handling_peding_requests=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(textToSpeech != null){
                textToSpeech.stop();
            }
        }
    };

    private void vibrate_phone(){
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            if(v != null){
                v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }else{
            //deprecated in API 26
            if(v != null){
                v.vibrate(500);
            }
        }
    }

    private void create_speech_recognizer(){
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                getApplication().getPackageName());
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        // create a recognizer
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);  // this ---> context
        mSpeechRecognizer.setRecognitionListener(this); // this ---> RecognitionListener interface
    }

    private void empty_variables(){
        packageName="";
        title="";
        sms_to_phone_number="";
        contact_name="";
        text_to_say="";
    }

    private void record_audio_denied(){
        empty_variables();
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED){
            // permission is already allowed but due to error that happened with the single instance of SpeechRecognizer when MainActivity was restarted // permission now becomes denied until the connection between mainActivity and SpeechRecognizer instance is established again // the system denies us permission
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try{
                        list_of_notifications.remove(0);
                        if(!list_of_notifications.isEmpty()){
                            process_notification();
                        }else{
                            audioManager.abandonAudioFocus(audioFocusChangeListener);
                            notification_in_process=false; // after processing all intents inside the arraylist
                        }
                    }catch (IndexOutOfBoundsException ex){
                        Log.e("Exception","IndexOutOfBoundsException index at 0 does not exist // record_audio_denied() ");
                    }
                }
            },60000); // 1 min
        }else{
            Toast.makeText(this, "permission to record audio is denied", Toast.LENGTH_SHORT).show();
            remove_intent_after_delay();
        }
    }

    private void change_sleep_timeout(){
        if(Settings.System.canWrite(this)){  // when permission to change settings is allowed
            Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_OFF_TIMEOUT, 120000);
        }else{
            // when permission to write settings is not yet given
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            startActivity(intent);
        }
    }

}

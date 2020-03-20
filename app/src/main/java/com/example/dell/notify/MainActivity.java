package com.example.dell.notify;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
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
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;

public class MainActivity extends AppCompatActivity implements RecognitionListener{

    private Button button_start_now;
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS =0 ;
    private final int REQ_CODE_FOR_RECORD_AUDIO_PERMISSION = 33;
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
    private String packageName,title; // packageName--> name of the app from which comes the notification and title --> is the sender of the notification
    private String whatsapp_package_name,sms_package_name,messenger,messenger_lite;
    //
    private Cursor cursor;
    private String sms_to_phone_number="";
    private String contact_name="";
    //
    private AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //check if device is charging
        if(check_if_device_is_charging()){
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
        setContentView(R.layout.activity_main);
        button_start_now =(Button) findViewById(R.id.start_now);
        textToSpeech = initializeTextToSpeech();
        textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener);
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
        audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

        // create notification channel
        createNotificationChannel();
        LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, new IntentFilter("Msg"));
        if(checkNotificationEnabled()){
            showNotification();  // Know that a notification can be shown even without the "Notification listener" permission
        }

        check_permissions();
        register_broadcasts_for_power_connection();

        button_start_now.setOnClickListener(new View.OnClickListener() {  // after enabling notification listener in the settings
            @Override
            public void onClick(View view) {
                startNow();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //check if device is charging
        if(check_if_device_is_charging()){
            // when phone is charging keep the screen on
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        //
    }

    @Override
    protected void onStart() {
        super.onStart();
        //check if device is charging
        if(check_if_device_is_charging()){
            // when phone is charging keep the screen on
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        if(isMyServiceRunning(NotificationService.class)){
            if(isMyServiceRunning(ServiceWithAlarmManager.class)){
                button_start_now.setText("Notify is running");
                button_start_now.setEnabled(false);
            }else{
                button_start_now.setText("start now");
                button_start_now.setEnabled(true);
            }
        }else{
            button_start_now.setText("start now");
            button_start_now.setEnabled(true);
        }
        //
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

    private void  startNow(){
        if(checkNotificationEnabled()){
            if(isMyServiceRunning(NotificationService.class)){
                //
                showNotification(); // Know that a notification can be shown even without the "Notification listener" permission
                start_service_with_alarm_manager();
                schedule_the_job(); //  since I added JobScheduler // something better changed
                //
                button_start_now.setText("Notify is running");
                button_start_now.setEnabled(false);
            }else{
                // NotificationService is not running
                Toast.makeText(MainActivity.this, "NotificationService is not running, please restart it", Toast.LENGTH_LONG).show();
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
        list_of_notifications.remove(0);
        if(!list_of_notifications.isEmpty()){
            process_notification(list_of_notifications.get(0)); // process the intent which is now on the position 0
        }else{
            audioManager.abandonAudioFocus(audioFocusChangeListener);
            notification_in_process=false; // after processing all intents inside the arraylist
        }
        //
        Toast.makeText(getApplicationContext(), "SMS sent.",Toast.LENGTH_LONG).show();
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
                    process_notification(list_of_notifications.get(0));
                } else {
                    Toast.makeText(getApplicationContext(),"SMS failed, please try again.", Toast.LENGTH_LONG).show();
                    //
                    list_of_notifications.remove(0);
                    if(!list_of_notifications.isEmpty()){
                        process_notification(list_of_notifications.get(0)); // process the intent which is now on the position 0
                    }else{
                        audioManager.abandonAudioFocus(audioFocusChangeListener);
                        notification_in_process=false; // after processing all intents inside the arraylist
                    }
                    //
                }
                break;
            }
            case REQ_CODE_FOR_RECORD_AUDIO_PERMISSION:  // if permission is allowed then record audio
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                }else{
                    Toast.makeText(this, "permission to record audio is denied", Toast.LENGTH_SHORT).show();
                    //
                    list_of_notifications.remove(0);
                    if(!list_of_notifications.isEmpty()){
                        process_notification(list_of_notifications.get(0)); // process the intent which is now on the position 0
                    }else{
                        audioManager.abandonAudioFocus(audioFocusChangeListener);
                        notification_in_process=false; // after processing all intents inside the arraylist
                    }
                    //
                }
                break;
            case REQ_CODE_FOR_ACCESS_CONTACTS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cursor = getApplicationContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
                    LoadContact loadContact = new LoadContact();
                    loadContact.execute();
                } else {
                    Toast.makeText(getApplicationContext(),"Notify is not allowed to access contacts of this phone", Toast.LENGTH_LONG).show();
                    //
                    list_of_notifications.remove(0);
                    if(!list_of_notifications.isEmpty()){
                        process_notification(list_of_notifications.get(0)); // process the intent which is now on the position 0
                    }else{
                        audioManager.abandonAudioFocus(audioFocusChangeListener);
                        notification_in_process=false; // after processing all intents inside the arraylist
                    }
                    //
                }
                break;
            case REQ_CODE_PERMISSIONS_FOR_SMS_AUDIO_CONTACTS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "permissions allowed", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(),"permissions denied", Toast.LENGTH_LONG).show();
                }
                break;
            default:
                return;
        }

    }

    private void check_record_audio_permission(){
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                REQ_CODE_FOR_RECORD_AUDIO_PERMISSION);
    }

    private void check_access_contacts_permission() {
        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, REQ_CODE_FOR_ACCESS_CONTACTS);
            //At this point you wait for callback in onRequestPermissionsResult()
        } else {
            // in case Android version is lesser than 6.0(Marshmallow) it means that permission is already granted.
            // or if Android version is greater than 5.0(lollipop) and permission is already granted t
            cursor = getApplicationContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
            LoadContact loadContact = new LoadContact();
            loadContact.execute();
        }
    }
    
    private void check_permissions(){
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.SEND_SMS,Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_CONTACTS},
                REQ_CODE_PERMISSIONS_FOR_SMS_AUDIO_CONTACTS);
    }

    public void openWhatsApp(String text){  //this opnes whatsapp and user has to click on send button
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("http://api.whatsapp.com/send?phone="+sms_to_phone_number +"&text="+text));
            intent.setPackage("com.whatsapp");  // choose whatsapp app
            startActivity(intent);
            //
            list_of_notifications.remove(0);
            if(!list_of_notifications.isEmpty()){
                process_notification(list_of_notifications.get(0)); // process the intent which is now on the position 0
            }else{
                notification_in_process=false; // after processing all intents inside the arraylist
            }
            //
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void send_message_on_whatsapp(final String text){
        final Intent intent=new Intent(Intent.ACTION_VOICE_COMMAND);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //intent.putExtra("message","send nehemiah how are you doing on whatsapp");
        startActivity(intent);
        final Handler handler=new Handler();
        final String contact= title.replaceAll(" ","");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int speechStatus = textToSpeech.speak("text "+contact+" "+text+" on whatsapp", TextToSpeech.QUEUE_FLUSH, null, null);
                while(textToSpeech.isSpeaking()){  // works well // wait until it finishes talking
                    Log.i("tts","text to speech");
                }
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        remove_intent();
                    }
                },40000); // give google assistant 40 seconds to process
            }
        },2000); // 2 secs// just wait for google assistant to get ready
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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

    @Override
    public void onError(int i) {
        // process next intent in the list
        list_of_notifications.remove(0);
        if(!list_of_notifications.isEmpty()){
            process_notification(list_of_notifications.get(0)); // process the intent which is now on the position 0
        }else{
            audioManager.abandonAudioFocus(audioFocusChangeListener);
            notification_in_process=false; // after processing all intents inside the arraylist
        }
        //
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList result = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String response=result.get(0).toString();
        if(packageName.equals(whatsapp_package_name)){
            //openWhatsApp(response); //
            send_message_on_whatsapp(response);
        }else if(packageName.equals(sms_package_name)){
            sendSMSMessage(response); //
        }else if(packageName.equals(messenger)){
            // reply to messenger  // using intent
            //
            // then remove the intent cause its processing is done
            list_of_notifications.remove(0);
            if(!list_of_notifications.isEmpty()){
                process_notification(list_of_notifications.get(0)); // process the intent which is now on the position 0
            }else{
                audioManager.abandonAudioFocus(audioFocusChangeListener);
                notification_in_process=false; // after processing all intents inside the arraylist
            }
            //
        }else if(packageName.equals(messenger_lite)){
            // reply to messenger lite
            //
            // then remove the intent cause its processing is done
            list_of_notifications.remove(0);
            if(!list_of_notifications.isEmpty()){
                process_notification(list_of_notifications.get(0)); // process the intent which is now on the position 0
            }else{
                audioManager.abandonAudioFocus(audioFocusChangeListener);
                notification_in_process=false; // after processing all intents inside the arraylist
            }
            //
        }else {
            //proceed with next intent in the list
            list_of_notifications.remove(0);
            if(!list_of_notifications.isEmpty()){
                process_notification(list_of_notifications.get(0)); // process the intent which is now on the position 0
            }else{
                audioManager.abandonAudioFocus(audioFocusChangeListener);
                notification_in_process=false; // after processing all intents inside the arraylist
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
        list_of_notifications.remove(0);
        if(!list_of_notifications.isEmpty()){
            process_notification(list_of_notifications.get(0)); // process the intent which is now on the position 0
        }else{
            audioManager.abandonAudioFocus(audioFocusChangeListener);
            notification_in_process=false; // after processing all intents inside the arraylist
        }
        //
    }

    //  For the LocalBroadcast
    public BroadcastReceiver onNotice= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            list_of_notifications.add(intent);
            if(!notification_in_process){
                process_notification(list_of_notifications.get(0));
            }else{
                //nothing
            }
        }
    };
    //

    private void process_notification(Intent intent) {
        Handler handler=new Handler();
        if(request_audio_focus()){
            // Notification in process
            notification_in_process=true;
            //
            contact_name="";  // the contact name must be fetched every time we process message //intent
            //
            packageName = intent.getStringExtra("package");
            title = intent.getStringExtra("title");
            String text = intent.getStringExtra("text");
            String ticker = intent.getStringExtra("ticker");
            int id = intent.getIntExtra("icon", 0);  // see the rest of the code in case we need the icon of the notification
            String sayText = title + " " + text;
            //Toast.makeText(this, "The ticker text is: " + ticker, Toast.LENGTH_LONG).show(); // ticker // has the same value as the title

            int speechStatus,speechStatus1;
            if (textToSpeech == null) {  // I'm doing this because the user may kill "--> onDestroy()"  the mainactivity which initializes the testToSpeech inside the onCreate  // so when our service sends the localBroadcast TextToSpeech will be available
                textToSpeech = initializeTextToSpeech();
                textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener);
            }
            //
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if(packageName.equals(whatsapp_package_name) || packageName.equals(sms_package_name)){
                    //
                    speechStatus = textToSpeech.speak("you have received  a new message from"+ sayText, TextToSpeech.QUEUE_FLUSH, null, null);
                    while(textToSpeech.isSpeaking()){  // works well // if we don't put this while loop above // text to speech will just jump to the next textToSpeech.speak() and leaves the previous one
                        Log.i("tts","text to speech");
                    }
                    //
                    check_access_contacts_permission();
                }else if(packageName.equals(messenger) || packageName.equals(messenger_lite)){
                    //
                    speechStatus = textToSpeech.speak("you have received  a new message from"+ sayText, TextToSpeech.QUEUE_FLUSH, null, null);
                    while(textToSpeech.isSpeaking()){  // works well // if we don't put this while loop above // text to speech will just jump to the next textToSpeech.speak() and leaves the previous one
                        Log.i("tts","text to speech");
                    }
                    //
                    speechStatus1 = textToSpeech.speak(" if you would you like to reply, please say your message:", TextToSpeech.QUEUE_FLUSH, null, null);
                    while(textToSpeech.isSpeaking()){  // works well // wait until it finishes talking
                        Log.i("tts","text to speech");
                    }
                    check_record_audio_permission();
                }else{
                    // package name does not require any response // we don't provide response for other apps like browsers,...
                    speechStatus = textToSpeech.speak("you have received  a new notification: "+ sayText, TextToSpeech.QUEUE_FLUSH, null, null);
                    while(textToSpeech.isSpeaking()){  // works well // wait until it finishes talking
                        Log.i("tts","text to speech");
                    }
                    //  we will have the spam classifier here  // filtering notifications from all other apps and websits
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            list_of_notifications.remove(0);
                            if(!list_of_notifications.isEmpty()){
                                process_notification(list_of_notifications.get(0)); // process the intent which is now on the position 0
                            }else{
                                audioManager.abandonAudioFocus(audioFocusChangeListener);
                                notification_in_process=false; // after processing all intents inside the arraylist
                            }
                            //
                        }
                    },5000);
                    //
                }
                //
            } else {
                // if android version is <6 // just read the message only
                speechStatus = textToSpeech.speak("you have received  a new notification: " + sayText, TextToSpeech.QUEUE_FLUSH, null);
                while(textToSpeech.isSpeaking()){  // works well // wait until it finishes talking
                    Log.i("tts","text to speech");
                }
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        list_of_notifications.remove(0);
                        if(!list_of_notifications.isEmpty()){
                            process_notification(list_of_notifications.get(0)); // process the intent which is now on the position 0
                        }else{
                            audioManager.abandonAudioFocus(audioFocusChangeListener);
                            notification_in_process=false; // after processing all intents inside the arraylist
                        }
                        //
                    }
                },5000);
                //
            }
        }else{
            notification_in_process=false;    // this intent will be processed again when a new intent arrives
            //nothing keep the intent in the arrayList
        }
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

    private UtteranceProgressListener utteranceProgressListener=new UtteranceProgressListener() {
        @Override
        public void onStart(String s) {

        }

        @Override
        public void onDone(String s) {

        }

        @Override
        public void onError(String s) {
            // to prevent ANR dialog (application not responding)
            textToSpeech.stop();
            textToSpeech.shutdown();
            //don't process it again
            //process_notification(list_of_notifications.get(0));  // reprocess it
        }
    };

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
                .setSmallIcon(R.drawable.notify_icon)
                .setContentTitle("Project Notification")
                .setContentText("this text is the content of our notification")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(notificationId, builder.build());
        notificationId++;  // in case u want to post more than one notification // each must have a unique id // from this app
        //
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
            if (cursor != null) {
                if (cursor.getCount() != 0) {
                    while (cursor.moveToNext()) {
                        if(title.equals(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)))){
                            // the title of the message is in our contacts
                            //String id = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                            contact_name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                            sms_to_phone_number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        }
                    }
                }
            } else {
                Log.e("Cursor ", "cursor is null");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(!contact_name.equals("")){ // means the title was found in our contacts
                if(textToSpeech!=null){  // error in saying the message did not occur
                    int speechStatus = textToSpeech.speak(" if you would you like to reply, please say your message:", TextToSpeech.QUEUE_FLUSH, null, null);
                    while(textToSpeech.isSpeaking()){  // works well // wait until it finishes talking
                        Log.i("tts","text to speech");
                    }
                    check_record_audio_permission();
                }
            }else if(contact_name.equals("") && packageName.equals(sms_package_name)){
                // in case of a new number // unknown number
                sms_to_phone_number=title.replaceAll("\\s",""); // remove all spaces
                sms_to_phone_number=sms_to_phone_number.replace("+","");
                if(textToSpeech != null){
                    int speechStatus = textToSpeech.speak(" if you would you like to reply, please say your message:", TextToSpeech.QUEUE_FLUSH, null, null);
                    while(textToSpeech.isSpeaking()){  // works well // wait until it finishes talking
                        Log.i("tts","text to speech");
                    }
                    check_record_audio_permission();
                }
            }else { //for example if message comes from a whatsapp group // delay for 2 secs then continue with next message // or for example : message from whatsapp 4 messages from 2 chats
                Handler handler=new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        list_of_notifications.remove(0);
                        if(!list_of_notifications.isEmpty()){
                            process_notification(list_of_notifications.get(0)); // process the intent which is now on the position 0
                        }else{
                            audioManager.abandonAudioFocus(audioFocusChangeListener);
                            notification_in_process=false; // after processing all intents inside the arraylist
                        }
                        //
                    }
                },2000);
            }
        }
    }
    //

}


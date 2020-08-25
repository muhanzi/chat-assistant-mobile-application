package com.muhanzi.notify.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class SpeakTextService extends Service {

    private TextToSpeech textToSpeech;
    private Context context;
    private String textToSay="";
    private String type="";
    private String packageName="";
    private String title="";
    private String notification="";
    private SharedPreferences sharedpreferences;
    private RequestQueue queue;
    private static final String TAG="volley queue";
    private FirebaseAuth mAuth;
    private FirebaseUser firebaseUser;
    private FirebaseFirestore db;
    private ArrayList<Map> dictionary;

    public SpeakTextService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        textToSpeech=initializeTextToSpeech();
        textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener);
        context=getApplicationContext();
        sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter("pending_responses"));
        // Instantiate the RequestQueue.
        queue = Volley.newRequestQueue(this);
        mAuth = FirebaseAuth.getInstance();
        firebaseUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        getDictionary();
    }

    private void getDictionary() {
        db.collection("users").document(firebaseUser.getUid())
                .collection("dictionary").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                            Map<String, Object> word = new HashMap<>();
                            word.put("abbreviation", document.getString("abbreviation"));
                            word.put("meaning", document.getString("meaning"));
                            dictionary.add(word);
                        }
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                    }
                });
    }

    BroadcastReceiver broadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(textToSpeech != null){
                textToSpeech.stop();
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.e("SpeakTextService","SpeakTextService was started");
        boolean handling_pending_responses=sharedpreferences.getBoolean("handling_pending_responses",false); // default value --> false
        Handler handler=new Handler();
        if(!handling_pending_responses){
            if(textToSpeech == null){
                textToSpeech = initializeTextToSpeech();
                textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener);
            }
            textToSay=intent.getStringExtra("textToSay");
            type=intent.getStringExtra("type");
            if(type.equals("notification")){
                notification=intent.getStringExtra("notification");
                packageName=intent.getStringExtra("package");
                title=intent.getStringExtra("title");
            }else if(type.equals("send_whatsapp_message")){
                handler.postDelayed(() -> {
                    Speak speak=new Speak();
                    speak.execute();
                },3000); // just 3 seconds // as google assistant is getting ready
                return super.onStartCommand(intent, flags, startId);
            }
            //
            handler.postDelayed(() -> {
                Speak speak=new Speak();
                speak.execute();
            },5000); // wait a bit so that Text to Speech finish initialization
        }
        //
        return super.onStartCommand(intent, flags, startId);
    }

    private TextToSpeech initializeTextToSpeech(){
        TextToSpeech tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    //int ttsLang = textToSpeech.setLanguage(Locale.US);
                    int ttsLang = textToSpeech.setLanguage(Locale.getDefault());
                    textToSpeech.setPitch((float) 1);
                    textToSpeech.setSpeechRate((float) 0.92);
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
            Log.e("TTS","onError() error happened while speaking // SpeakTextService");
            // to prevent ANR dialog (application not responding)  // when textToSpeech is may be reading a long text or its used many times in a short period
            textToSpeech.stop();  // Interrupts the current utterance (whether played or rendered to file) and discards other utterances in the queue.
            textToSpeech.shutdown(); // Releases the resources used by the TextToSpeech engine.
            Toast.makeText(getApplicationContext(), "Text to speech encountered an error", Toast.LENGTH_LONG).show();
        }
    };

    class Speak extends AsyncTask<Void, Void, Void> {   // to execute tasks on the worker thread
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            //
            if(!textToSay.equals("")){
                if(!type.equals("send_whatsapp_message")) {
                    for (Map map : dictionary) {
                        textToSay.replaceAll(map.get("abbreviation").toString(), map.get("meaning").toString());
                    }
                }
                int speechStatus = textToSpeech.speak(textToSay, TextToSpeech.QUEUE_FLUSH, null, null);
                while(textToSpeech.isSpeaking()){
                    Log.i("tts","text to speech  // saying the message");
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            boolean handling_pending_responses=sharedpreferences.getBoolean("handling_pending_responses",false); // default value --> false
            String gmail="com.google.android.gm",instagram="com.instagram.android",twitter="com.twitter.android",systemUI="com.android.systemui";
            if(!handling_pending_responses){
                if(!textToSay.equals("")){
                    textToSay="";
                    //
                    if(type.equals("reply") || type.equals("send_whatsapp_message") || type.equals("phone_locked")){
                        if(textToSpeech.isSpeaking()){
                            textToSpeech.stop();
                        }
                        Intent FinishSpeaking = new Intent("Speaking"); // action --> "Speaking"
                        FinishSpeaking.putExtra("type",type);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(FinishSpeaking);
                        return;
                    }
                    // for messages and notifications
                    if(type.equals("message") || packageName.equals(gmail) || packageName.equals(instagram) || packageName.equals(twitter) || packageName.equals(systemUI)){
                        Intent FinishSpeaking = new Intent("Speaking"); // action --> "Speaking"
                        FinishSpeaking.putExtra("type",type);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(FinishSpeaking);
                        // empty variables
                        type="";
                        packageName="";
                        notification="";
                        title="";
                    }else{
                        filter_notification(notification);
                    }
                }
            }
        }
    }


    private void filter_notification(final String notification_text) {

        String url = "https://plino.herokuapp.com/api/v1/classify/";  // we use the plino rest api

        Map<String,String> map = new HashMap<>();
        map.put("email_text",notification_text);

        JSONObject jsonObject = new JSONObject(map);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, jsonObject, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {

                            String email_class = response.getString("email_class"); // spam or ham
                            //String email_text = response.getString("email_text");
                            //int status = response.getInt("status"); // HTTP response codes // 200 for okay
                            if(email_class.equals("spam")){
                                String date,time;
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                                    DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss a");
                                    LocalDateTime currentDate = LocalDateTime.now();
                                    date= currentDate.format(dateFormat);
                                    time= currentDate.format(timeFormat);
                                }else{
                                    Calendar calendar = Calendar.getInstance();
                                    Date currentDate = calendar.getTime();
                                    SimpleDateFormat dateFormat = (SimpleDateFormat) SimpleDateFormat.getDateInstance();
                                    SimpleDateFormat timeFormat = (SimpleDateFormat) SimpleDateFormat.getTimeInstance();
                                    date=dateFormat.format(currentDate);
                                    time=timeFormat.format(currentDate);
                                }
                                Map<String, Object> spam = new HashMap<>();
                                spam.put("spamText", notification_text);
                                spam.put("notificationTitle", title);
                                spam.put("packageName", packageName);
                                spam.put("date", date);
                                spam.put("time", time);
                                db.collection("users").document(firebaseUser.getUid())
                                        .collection("spams").add(spam)
                                        .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentReference> task) {
                                                Intent FinishSpeaking = new Intent("Speaking"); // action --> "Speaking"
                                                FinishSpeaking.putExtra("type",type);
                                                LocalBroadcastManager.getInstance(context).sendBroadcast(FinishSpeaking);
                                                Log.i("SpamFilter","notification looks like a spam: "+notification_text+" Spam from: "+packageName);
                                                if(task.isSuccessful()){
                                                    Toast.makeText(context, "notification looks like a spam: "+notification_text+" Spam from: "+packageName, Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                            }else{
                                // it's a ham
                                Intent FinishSpeaking = new Intent("Speaking"); // action --> "Speaking"
                                FinishSpeaking.putExtra("type",type);
                                LocalBroadcastManager.getInstance(context).sendBroadcast(FinishSpeaking);
                                Log.i("SpamFilter","Ham: "+notification_text);
                            }

                        } catch (JSONException e) {
                            Log.e("JSONException",e.getMessage());
                            //Toast.makeText(context, "JSONException", Toast.LENGTH_SHORT).show();
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error occurred // network, bad request,....
                        Intent FinishSpeaking = new Intent("Speaking"); // action --> "Speaking"
                        FinishSpeaking.putExtra("type",type);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(FinishSpeaking);
                        Log.i("SpamFilter","onErrorResponse: "+error.getMessage());
                       // Toast.makeText(context, "SpamFilter onErrorResponse() "+error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }){
            // the request headers // content-type // tokens,...
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        };

        jsonObjectRequest.setTag(TAG);
        // Add the request to the RequestQueue.
        queue.add(jsonObjectRequest);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(queue != null){
            queue.cancelAll(TAG);
        }

    }
}

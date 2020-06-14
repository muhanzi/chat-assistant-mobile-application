package com.example.dell.notify.services;

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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
            }
            Handler handler=new Handler();
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
                                // we display spams in ListView  // and save it in Firestore when we integrate it
                                // report the spam // packageName,text,title,current date & time
                                Intent FinishSpeaking = new Intent("Speaking"); // action --> "Speaking"
                                FinishSpeaking.putExtra("type",type);
                                LocalBroadcastManager.getInstance(context).sendBroadcast(FinishSpeaking);
                                Log.i("SpamFilter","notification looks like a spam: "+notification_text+" Spam from: "+packageName);
                                Toast.makeText(context, "notification looks like a spam: "+notification_text+" Spam from: "+packageName, Toast.LENGTH_SHORT).show();
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

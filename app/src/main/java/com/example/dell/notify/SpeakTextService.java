package com.example.dell.notify;

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

import java.util.Locale;

public class SpeakTextService extends Service {

    private TextToSpeech textToSpeech;
    private Context context;
    private String textToSay="";
    private SharedPreferences sharedpreferences;

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
            Handler handler=new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Speak speak=new Speak();
                    speak.execute();
                }
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
            if(!handling_pending_responses){
                if(!textToSay.equals("")){
                    textToSay="";
                    Intent FinishSpeaking = new Intent("Speaking"); // action --> "Speaking"
                    LocalBroadcastManager.getInstance(context).sendBroadcast(FinishSpeaking);
                }
            }
        }
    }
}

package com.example.dell.notify;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
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
    private String TTS_finished_up;

    public SpeakTextService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        textToSpeech=initializeTextToSpeech();
        textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener);
        context=getApplicationContext();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (textToSpeech == null) {
            textToSpeech = initializeTextToSpeech();
            textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener);
        }
        //
        textToSay=intent.getStringExtra("textToSay");
        if(TTS_finished_up != null){
            Speak speak=new Speak();
            speak.execute();
        }
        else{
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
                    TTS_finished_up="true"; // setup finished
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
            Toast.makeText(getApplicationContext(), "Text to speech encountered an error", Toast.LENGTH_LONG).show();
            //
            Handler handler=new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Speak speak=new Speak();
                    speak.execute();
                }
            },10000);
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
            int speechStatus = textToSpeech.speak(textToSay, TextToSpeech.QUEUE_FLUSH, null, null);
            //
            while(textToSpeech.isSpeaking()){
                Log.i("tts","text to speech");
            }
            if(!textToSpeech.isSpeaking()) {
                Intent FinishSpeaking = new Intent("Speaking"); // action --> "Speaking"
                LocalBroadcastManager.getInstance(context).sendBroadcast(FinishSpeaking);
            }else{
                textToSpeech.stop();
                Intent FinishSpeaking = new Intent("Speaking"); // action --> "Speaking"
                LocalBroadcastManager.getInstance(context).sendBroadcast(FinishSpeaking);
            }
            //
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

        }
    }
}

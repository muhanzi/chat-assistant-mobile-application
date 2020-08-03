package com.example.dell.notify.services;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class RecognitionService extends Service implements RecognitionListener{

    private SpeechRecognizer mSpeechRecognizer;
    private Intent mSpeechRecognizerIntent;
    //
    private TextToSpeech textToSpeech;
    private String text_to_say="";
    //
    private int ERROR_CODE = 500; // 500 --> is default value

    public RecognitionService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if(mSpeechRecognizer == null){
            create_speech_recognizer();
        }
        //
        if(textToSpeech == null){
            textToSpeech = initializeTextToSpeech();
            textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(mSpeechRecognizer != null) {
            mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
        }else{
            create_speech_recognizer();
            mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     recognition listener methods
     */
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
    public void onError(int errorCode) {
        Speak speak=new Speak();
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                ERROR_CODE=errorCode;
                message = "Audio recording error";
                Log.e("onError","RecognitionListener error: "+message);
                text_to_say="an error occurred, Audio recording error";
                speak.execute();
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                ERROR_CODE=errorCode;
                message = "other Client side error";
                Log.e("onError","RecognitionListener error: "+message);
                text_to_say="an error occurred, Client side error";
                speak.execute();
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                ERROR_CODE=errorCode;
                message = "Insufficient permissions";
                Log.e("onError","RecognitionListener error: "+message);
                text_to_say="an error occurred, some permissions were not granted.";
                speak.execute();
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                ERROR_CODE=errorCode;
                message = "Network error";
                Log.e("onError","RecognitionListener error: "+message);
                text_to_say="a network error occurred. please check your internet connection";
                speak.execute();
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                ERROR_CODE=errorCode;
                message = "Network timeout"; // network issues --> slow
                Log.e("onError","RecognitionListener error: "+message);
                errorSendBroadcast();
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                ERROR_CODE=errorCode;
                message = "No match";
                Log.e("onError","RecognitionListener error: "+message);
                text_to_say="I Didn't understand, please try again.";
                speak.execute();
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                ERROR_CODE=errorCode;
                speechRecognizerIsBusy();
                message = "RecognitionService busy";
                Log.e("onError","RecognitionListener error: "+message);
                text_to_say="An Error occurred, please try again.";
                speak.execute();
                break;
            case SpeechRecognizer.ERROR_SERVER:
                ERROR_CODE=errorCode;
                message = "error from server";
                Log.e("onError","RecognitionListener error: "+message);
                errorSendBroadcast();
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                ERROR_CODE=errorCode;
                message = "No speech input";  // no response
                Log.e("onError","RecognitionListener error: "+message);
                text_to_say="okay, no response";
                speak.execute();
                break;
            default:
                ERROR_CODE=errorCode;
                message = "an unexpected error occurred, please try again.";
                Log.e("onError","RecognitionListener error: "+message);
                text_to_say="an unexpected error occurred";
                speak.execute();
                break;
        }
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList result = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String response=result.get(0).toString();
        // send broadcast
        Intent FinishRecording = new Intent("Recording"); // action --> "Recording"
        FinishRecording.putExtra("output", "results");
        FinishRecording.putExtra("response", response);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(FinishRecording);
    }

    @Override
    public void onPartialResults(Bundle bundle) {

    }

    @Override
    public void onEvent(int i, Bundle bundle) {

    }
    /** ----recognition listener----- */

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mSpeechRecognizer!=null){
            mSpeechRecognizer.stopListening();
            mSpeechRecognizer.cancel();
            mSpeechRecognizer.destroy();
        }
        mSpeechRecognizer=null;
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

    private void speechRecognizerIsBusy(){
        if(mSpeechRecognizer!=null){
            mSpeechRecognizer.cancel();
            mSpeechRecognizer.destroy();
        }
        mSpeechRecognizer=null;
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
                Intent intent = new Intent("Recording"); // action --> "Recording"
                intent.putExtra("output", "error");
                intent.putExtra("errorCode",ERROR_CODE);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }else{
                textToSpeech.stop();
                Intent intent = new Intent("Recording"); // action --> "Recording"
                intent.putExtra("output", "error");
                intent.putExtra("errorCode",ERROR_CODE);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }
        }
    }

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
            Log.e("TTS","onError() inside RecognitionService // during text to speech");
            Toast.makeText(RecognitionService.this, "Text to speech encountered an error", Toast.LENGTH_LONG).show();
        }
    };

    private  void errorSendBroadcast(){
        Intent intent = new Intent("Recording"); // action --> "Recording"
        intent.putExtra("output", "error");
        intent.putExtra("errorCode",ERROR_CODE);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

}

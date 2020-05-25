package com.example.dell.notify;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class RecognitionService extends Service implements RecognitionListener{

    private SpeechRecognizer mSpeechRecognizer;
    private Intent mSpeechRecognizerIntent;

    public RecognitionService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if(mSpeechRecognizer == null){
            create_speech_recognizer();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(mSpeechRecognizer != null){
            mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
        }else{
            //Toast.makeText(this, "record_audio_denied //mSpeechRecognizer == null", Toast.LENGTH_LONG).show();
            Log.e("record_audio_denied","mSpeechRecognizer == null //restart speech recognizer");
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
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error"; // fails to record
                //Toast.makeText(this, "RecognitioNListener error: "+message, Toast.LENGTH_SHORT).show();
                Log.e("onError","RecognitioNListener error: "+message);
                sendBroadcast();
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "other Client side error";
                //Toast.makeText(this, "RecognitioNListener error: "+message, Toast.LENGTH_SHORT).show();
                Log.e("onError","RecognitioNListener error: "+message);
                sendBroadcast();
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                //Toast.makeText(this, "RecognitioNListener error: "+message, Toast.LENGTH_SHORT).show();
                Log.e("onError","RecognitioNListener error: "+message);
                sendBroadcast();
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                //Toast.makeText(this, "RecognitioNListener error: "+message, Toast.LENGTH_SHORT).show();
                Log.e("onError","RecognitioNListener error: "+message);
                sendBroadcast();
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                //Toast.makeText(this, "RecognitioNListener error: "+message, Toast.LENGTH_SHORT).show();
                Log.e("onError","RecognitioNListener error: "+message);
                sendBroadcast(); //
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                //Toast.makeText(this, "RecognitioNListener error: "+message, Toast.LENGTH_SHORT).show();
                Log.e("onError","RecognitioNListener error: "+message);
                sendBroadcast();
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                //Toast.makeText(this, "RecognitioNListener error: "+message, Toast.LENGTH_SHORT).show();
                Log.e("onError","RecognitioNListener error: "+message);
                mSpeechRecognizer.cancel();
                mSpeechRecognizer.destroy();
                mSpeechRecognizer=null;
                sendBroadcast();
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                //Toast.makeText(this, "RecognitioNListener error: "+message, Toast.LENGTH_SHORT).show();
                Log.e("onError","RecognitioNListener error: "+message);
                sendBroadcast(); //
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";  // no response
                //Toast.makeText(this, "RecognitioNListener error: "+message, Toast.LENGTH_SHORT).show();
                Log.e("onError","RecognitioNListener error: "+message);
                sendBroadcast();
                break;
            default:
                message = "an error occurred, please try again.";
                //Toast.makeText(this, "RecognitioNListener error: "+message, Toast.LENGTH_SHORT).show();
                Log.e("onError","RecognitioNListener error: "+message);
                sendBroadcast();
                break;
        }
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList result = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String response=result.get(0).toString();
        Toast.makeText(RecognitionService.this, "RESPONSE: "+response, Toast.LENGTH_SHORT).show();
        // send broadcast
        Intent FinishRecording = new Intent("Recording"); // action --> "Recording"
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

    private void sendBroadcast(){
        Intent FinishRecording = new Intent("Recording"); // action --> "Recording"
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(FinishRecording);
    }

}

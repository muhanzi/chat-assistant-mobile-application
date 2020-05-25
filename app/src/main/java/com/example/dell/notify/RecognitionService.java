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

import java.util.ArrayList;
import java.util.Locale;

public class RecognitionService extends Service implements RecognitionListener{

    private SpeechRecognizer mSpeechRecognizer;
    private Intent mSpeechRecognizerIntent;
    private static boolean first_time_onStop_called = false;

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
        first_time_onStop_called =intent.getBooleanExtra("first_time_onStop_called",false);
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
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                errorOcurredSendBroadcast(SpeechRecognizer.ERROR_AUDIO);
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                errorOcurredSendBroadcast(SpeechRecognizer.ERROR_CLIENT);
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                errorOcurredSendBroadcast(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS);
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                errorOcurredSendBroadcast(SpeechRecognizer.ERROR_NETWORK);
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                errorOcurredSendBroadcast(SpeechRecognizer.ERROR_NETWORK_TIMEOUT);
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                errorOcurredSendBroadcast(SpeechRecognizer.ERROR_NO_MATCH);
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                speechRecognizerIsBusy();
                errorOcurredSendBroadcast(SpeechRecognizer.ERROR_RECOGNIZER_BUSY);
                break;
            case SpeechRecognizer.ERROR_SERVER:
                errorOcurredSendBroadcast(SpeechRecognizer.ERROR_SERVER);
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                errorOcurredSendBroadcast(SpeechRecognizer.ERROR_SPEECH_TIMEOUT);
                break;
            default:
                errorOcurredSendBroadcast(500); // we just use 500 --> as default error code
                break;
        }
    }

    @Override
    public void onResults(Bundle results) {
        if(first_time_onStop_called){
            return;
        }
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

    private void errorOcurredSendBroadcast(int errorCode){
        if(first_time_onStop_called){
            return;
        }
        Intent intent = new Intent("Recording"); // action --> "Recording"
        intent.putExtra("output", "error");
        intent.putExtra("errorCode",errorCode);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

}

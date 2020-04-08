package com.example.dell.notify;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Created by DELL on 4/3/2020.
 */

public class PhoneUnlockedBroadcastReceiver extends BroadcastReceiver {

    private SharedPreferences sharedpreferences;
    private Set<String> pending_responses;
    private List<String> pending_list;
    private TextToSpeech textToSpeech;
    private Context ctx;
    private AudioManager audioManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        //
        ctx=context;
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        //
        sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        pending_responses=sharedpreferences.getStringSet("pending_responses",null);
        pending_list=new ArrayList<>();
        //
        textToSpeech=initializeTextToSpeech();
        textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener);
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        //
        if(keyguardManager.isDeviceLocked() || keyguardManager.isKeyguardLocked() || keyguardManager.inKeyguardRestrictedInputMode()){
            Log.i("Locked","device is locked");
        }else{
            // phone is unlocked
            SharedPreferences.Editor editor = sharedpreferences.edit();
            if(null != pending_responses && !pending_responses.isEmpty()){
                editor.putBoolean("turn_on_notify",false);
                editor.putBoolean("handling_pending_responses",true);
                editor.apply();
                //process the pending whatsapp responses
                pending_list.addAll(pending_responses);
                send_message_on_whatsapp();
            }
        }

    }

    private TextToSpeech initializeTextToSpeech(){
        TextToSpeech tts = new TextToSpeech(ctx, new TextToSpeech.OnInitListener() {
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
                    Toast.makeText(ctx, "TTS Initialization failed!", Toast.LENGTH_SHORT).show();
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
            textToSpeech.stop();  // Interrupts the current utterance (whether played or rendered to file) and discards other utterances in the queue.
            textToSpeech.shutdown(); // Releases the resources used by the TextToSpeech engine.
            Toast.makeText(ctx, "Error occurred while saying the text", Toast.LENGTH_LONG).show();
            //
            Handler handler=new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // abandon audio focus
                    audioManager.abandonAudioFocus(audioFocusChangeListener);
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putBoolean("turn_on_notify",true);
                    editor.putBoolean("handling_pending_responses",false);
                    editor.apply();
                    //
                    SharedPreferences.Editor editor2 = sharedpreferences.edit();
                    editor2.remove("pending_responses");
                    editor2.apply();
                }
            },10000);
        }
    };


    private void send_message_on_whatsapp(){
        if (textToSpeech == null) {
            textToSpeech = initializeTextToSpeech();
            textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener);
        }
        if(request_audio_focus()){
            final Intent intent=new Intent(Intent.ACTION_VOICE_COMMAND);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
            Handler handler=new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Speak speak=new Speak();
                    speak.execute();
                }
            },3000);
        }else{
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putBoolean("turn_on_notify",true);
            editor.putBoolean("handling_pending_responses",false);
            editor.apply();
            Toast.makeText(ctx, "Notify was denied access to audio focus", Toast.LENGTH_LONG).show();
            bring_main_activity_to_foreground();  // but we keep responses in the sharedPreferences // pending_responses
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


    class Speak extends AsyncTask<Void, Void, Void> {   // to execute tasks on the worker thread
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if(!pending_list.isEmpty()){
                String textToSay=pending_list.get(0);
                int speechStatus = textToSpeech.speak(textToSay, TextToSpeech.QUEUE_FLUSH, null, null);
                //
                while(textToSpeech.isSpeaking()){
                    Log.i("tts","text to speech");
                }
                Handler handler=new Handler();
                if(!textToSpeech.isSpeaking()){
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                pending_list.remove(0);
                                if(!pending_list.isEmpty()){
                                    send_message_on_whatsapp();
                                }else{
                                    // abandon audio focus
                                    audioManager.abandonAudioFocus(audioFocusChangeListener);
                                    SharedPreferences.Editor editor = sharedpreferences.edit();
                                    editor.remove("pending_responses");
                                    editor.apply();
                                    //
                                    SharedPreferences.Editor editor2 = sharedpreferences.edit();
                                    editor2.putBoolean("turn_on_notify",true);
                                    editor2.putBoolean("handling_pending_responses",false);
                                    editor2.apply();
                                    bring_main_activity_to_foreground();
                                }
                            }catch(IndexOutOfBoundsException ex){
                                Log.e("Exception","IndexOutOfBoundsException ");
                            }
                    }
                    },80000);
                }else{
                    textToSpeech.stop();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                pending_list.remove(0);
                                if(!pending_list.isEmpty()){
                                    send_message_on_whatsapp();
                                }else{
                                    // abandon audio focus
                                    audioManager.abandonAudioFocus(audioFocusChangeListener);
                                    SharedPreferences.Editor editor = sharedpreferences.edit();
                                    editor.remove("pending_responses");
                                    editor.apply();
                                    //
                                    SharedPreferences.Editor editor2 = sharedpreferences.edit();
                                    editor2.putBoolean("turn_on_notify",true);
                                    editor2.putBoolean("handling_pending_responses",false);
                                    editor2.apply();
                                    bring_main_activity_to_foreground();
                                }
                            }catch(IndexOutOfBoundsException ex){
                                Log.e("Exception","IndexOutOfBoundsException ");
                            }
                        }
                    },80000);
                }
                //
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

        }
    }

    private void bring_main_activity_to_foreground(){
        // move the app to the foreground
        Intent intent = new Intent(ctx, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        ctx.startActivity(intent);
        //
    }

}

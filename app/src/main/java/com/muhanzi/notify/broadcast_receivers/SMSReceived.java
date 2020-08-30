package com.muhanzi.notify.broadcast_receivers;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Objects;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * Created by DELL on 4/7/2020.
 */

public class SMSReceived extends BroadcastReceiver {

    private Cursor cursor;
    private String messageText="",messageFromNumber="",messageTitle="",packageName="";
    private Context ctx;
    private FirebaseAuth mAuth;
    private FirebaseUser firebaseUser;
    private FirebaseFirestore db;
    private ArrayList<String> blockedNumbers;
    private static final String TAG="Load blocked numbers";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
            Toast.makeText(context, "Allow Notify to access phone contacts", Toast.LENGTH_SHORT).show();
            return;
        }
        cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
        packageName=Telephony.Sms.getDefaultSmsPackage(context);
        ctx=context;
        mAuth = FirebaseAuth.getInstance();
        firebaseUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        blockedNumbers = getBlockedNumbers();
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                messageText = smsMessage.getMessageBody(); // message text
                messageFromNumber = smsMessage.getOriginatingAddress().trim().replaceAll("\\s",""); // phone number
                if(isNotBlocked()) {
                    if (isValidPhoneNumber(messageFromNumber)) {
                        RetrieveContacts retrieveContacts = new RetrieveContacts();
                        retrieveContacts.execute();
                    } else {
                        Intent msgrcv = new Intent("Msg"); // action --> "Msg"
                        msgrcv.putExtra("package", packageName);
                        msgrcv.putExtra("title", messageFromNumber); // bulk sms
                        msgrcv.putExtra("text", messageText);
                        msgrcv.putExtra("number", messageFromNumber); // bulk sms
                        LocalBroadcastManager.getInstance(ctx).sendBroadcast(msgrcv);
                    }
                }
            }
        }
        //
    }

    private boolean isValidPhoneNumber(String phoneNumber){
        // we remove + sign so that we can filter digits // to avoid bulk sms
        String number=phoneNumber.replace("+",""); // first remove the + sign then verify the left characters
        if(number.length() >= 10 && number.length() <= 12){
            // check if number has letters // or check if it is a bulk sms likely with letters or other special characters
            if(number.matches("[0-9]+")){
                return true;
            }
        }
        //
        return false;
    }

    class RetrieveContacts extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if(cursor != null){
                if(cursor.getCount() > 0){
                    String contact_number;
                    while(cursor.moveToNext()){
                        contact_number=cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)).trim().replaceAll("\\s","");
                        String name=cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                        if(isValidPhoneNumber(contact_number)){ // the number in our contacts must also be valid
                            if(matchNumbers(contact_number.replace("+",""),messageFromNumber.replace("+",""))){
                                messageTitle=cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                                break;
                            }
                        }
                    }
                }else {
                    Log.i("sms ", "count =0");
                }
            }else {
                Log.i("sms ", "cursor == null ");
            }
            //
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(!messageTitle.equals("")){
                Intent msgrcv = new Intent("Msg"); // action --> "Msg"
                msgrcv.putExtra("package", packageName);
                msgrcv.putExtra("title", messageTitle);
                msgrcv.putExtra("text", messageText);
                msgrcv.putExtra("number", messageFromNumber);
                LocalBroadcastManager.getInstance(ctx).sendBroadcast(msgrcv);
            }else{
                // message is from a new number or unknown number
                Intent msgrcv = new Intent("Msg"); // action --> "Msg"
                msgrcv.putExtra("package", packageName);
                msgrcv.putExtra("title", messageFromNumber); // unknown number
                msgrcv.putExtra("text", messageText);
                msgrcv.putExtra("number", messageFromNumber);
                LocalBroadcastManager.getInstance(ctx).sendBroadcast(msgrcv);
            }
        }

        private boolean matchNumbers (String number1,String number2){
            // we compare the last 9 characters of a phone number // that means we leave out the starting 0 or the +country code
            boolean characters_are_equal=false;
            int number1_length=number1.length(),number2_length=number2.length();  // index starts from 0
            number1=number1.substring(number1_length-9); // get the last 9 digits
            number2=number2.substring(number2_length-9); // get the last 9 digits
            // after substring  length of number1 is 9 and length of number2 is also 9
            for(int i=8;i>=0;i--){ // from index 0 --> 8 // 9 characters
                Character ch1=number1.charAt(i);
                Character ch2=number2.charAt(i);
                if(ch1.equals(ch2)){
                    characters_are_equal=true;
                }else{
                    characters_are_equal=false;
                    break;
                }
            }
            return characters_are_equal;
        }

    }


    private ArrayList<String> getBlockedNumbers() {
        ArrayList<String> numbers = new ArrayList<>();
        db.collection("users").document(firebaseUser.getUid())
                .collection("blockedContacts").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                            numbers.add(document.getString("contactNumber"));
                        }
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                    }
                });
        return numbers;
    }

    private boolean isNotBlocked(){
        boolean isBlocked = false;
        for(String blockednum : blockedNumbers){
            if(blockednum.replaceAll("\\s","").equals(messageFromNumber)){
                isBlocked = true;
                break;
            }
        }
        return !isBlocked;
    }

}

package com.muhanzi.notify.fragments.settings_fragments;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.muhanzi.notify.R;
import com.muhanzi.notify.activities.SettingsActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

public class SettingsPage2 extends Fragment {

    public static View mainview;
    private Context context;
    ListView lstContact;
    CustomContactAdapter adapter;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser firebaseUser;
    private final String TAG = "SettingsPage2";
    private ArrayList<String> blockedContacts;
    private ProgressDialog progressDialog;

    public SettingsPage2() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainview = inflater.inflate(R.layout.fragment_settings_page2, container, false);
        context = getContext();
        lstContact = (ListView) mainview.findViewById(R.id.lstContacts);
        progressBar = (ProgressBar) mainview.findViewById(R.id.progressBarPage2);
        mAuth = FirebaseAuth.getInstance();
        firebaseUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        blockedContacts = new ArrayList<>();
        progressDialog =new ProgressDialog(context);
        progressDialog.setMessage("please wait");
        progressDialog.setCancelable(false);
        progressBar.setVisibility(View.VISIBLE);
        db.collection("users").document(firebaseUser.getUid())
                .collection("blockedContacts").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                //Log.d(TAG, document.getId() + " => " + document.getData());
                                blockedContacts.add(document.getString("contactNumber"));
                            }
                            SettingsActivity.instance.getSupportLoaderManager().initLoader(1,null,loaderCallbacks);
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
        return mainview;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    LoaderManager.LoaderCallbacks loaderCallbacks = new LoaderManager.LoaderCallbacks() {
        @Override
        public Loader onCreateLoader(int id, Bundle args) {
            if(context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(context, "Allow Notify to access phone contacts", Toast.LENGTH_SHORT).show();
                return null;
            }
            Uri CONTACT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            CursorLoader cursorLoader = new CursorLoader(context, CONTACT_URI, null, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC");
            return cursorLoader;
        }

        @Override
        public void onLoadFinished(Loader loader, Object data) {
            if(loader == null || data == null){
                return;
            }
            Cursor cursor = (Cursor) data;
            cursor.moveToFirst();
            adapter = new CustomContactAdapter(context, cursor);
            lstContact.setAdapter(adapter);
            lstContact.setOnItemClickListener((adapterView, view, position, l) -> {
                TextView contactName = (TextView) view.findViewById(R.id.contact_name);
                TextView contactNumber = (TextView) view.findViewById(R.id.contact_number);
                CheckBox checkBox = (CheckBox) view.findViewById(R.id.contact_checkBox);
                boolean checkBoxChecked = checkBox.isChecked();
                if(checkBoxChecked){
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Do you want to block all messages from this contact ?");
                    builder.setPositiveButton("Block", (paramDialogInterface, paramInt) -> {
                        progressDialog.show();
                        Map<String, Object> contact = new HashMap<>();
                        contact.put("contactName", contactName.getText());
                        contact.put("contactNumber", contactNumber.getText());
                        db.collection("users").document(firebaseUser.getUid())
                                .collection("blockedContacts").document(contactNumber.getText().toString()).set(contact)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()){
                                            progressDialog.dismiss();
                                            checkBox.setChecked(false);
                                            Toast.makeText(context, "Messages from this Contact won't be read", Toast.LENGTH_SHORT).show();
                                        }else{
                                            progressDialog.dismiss();
                                            Toast.makeText(context, "An Error Occurred, Try again", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    });
                    builder.setNegativeButton("Cancel", (paramDialogInterface, paramInt) -> Toast.makeText(context, "cancel", Toast.LENGTH_SHORT).show());
                    builder.show();
                }else{
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Do you want Notify to read all messages from this contact ?");
                    builder.setPositiveButton("Unblock", (paramDialogInterface, paramInt) -> {
                        progressDialog.show();
                        db.collection("users").document(firebaseUser.getUid())
                                .collection("blockedContacts").document(contactNumber.getText().toString()).delete()
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()){
                                            progressDialog.dismiss();
                                            checkBox.setChecked(true);
                                            Toast.makeText(context, "Messages from this contact will now be read", Toast.LENGTH_SHORT).show();
                                        }else{
                                            progressDialog.dismiss();
                                            Toast.makeText(context, "An Error Occurred, Try again", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    });
                    builder.setNegativeButton("Cancel", (paramDialogInterface, paramInt) -> Toast.makeText(context, "cancel", Toast.LENGTH_SHORT).show());
                    builder.show();
                }
            });
            progressBar.setVisibility(View.GONE);
        }

        @Override
        public void onLoaderReset(Loader loader) {

        }
    };

    private class CustomContactAdapter extends BaseAdapter {
        Cursor cursor;
        Context mContext;
        LayoutInflater inflater;

        private CustomContactAdapter(Context context, Cursor cursor) {
            mContext = context;
            this.cursor = cursor;
            inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return cursor.getCount();
        }
        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            Holder holder;
            cursor.moveToPosition(position);
            if (view == null) {
                view = inflater.inflate(R.layout.contact_list, parent, false);
                holder = new Holder();
                holder.ContactName = (TextView) view.findViewById(R.id.contact_name);
                holder.ContactNumber = (TextView) view.findViewById(R.id.contact_number);
                holder.ContactCheckBox = (CheckBox) view.findViewById(R.id.contact_checkBox);
                view.setTag(holder);
            } else {
                holder = (Holder) view.getTag();
            }
            holder.ContactName.setText(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)));
            holder.ContactNumber.setText(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
            boolean checked = checkContact(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
            holder.ContactCheckBox.setChecked(checked);
            return view;
        }

        private boolean checkContact(String contactNumber){
            for(String number : blockedContacts){
                if(contactNumber.equals(number)){
                    return false;
                }
            }
            return true;
        }

        class Holder {
            TextView ContactName, ContactNumber;
            CheckBox ContactCheckBox;
        }
    }

}

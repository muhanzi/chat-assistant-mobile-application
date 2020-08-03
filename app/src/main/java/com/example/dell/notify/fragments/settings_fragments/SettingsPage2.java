package com.example.dell.notify.fragments.settings_fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.dell.notify.R;
import com.example.dell.notify.activities.SettingsActivity;

public class SettingsPage2 extends Fragment{

    public static View mainview;
    private Context context;
    ListView lstContact;
    CustomContactAdapter adapter;
    private ProgressBar progressBar;

    public SettingsPage2() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainview = inflater.inflate(R.layout.fragment_settings_page2, container, false);
        context = getContext();
        lstContact = (ListView) mainview.findViewById(R.id.lstContacts);
        progressBar = (ProgressBar) mainview.findViewById(R.id.progressBarPage2);
        SettingsActivity.instance.getSupportLoaderManager().initLoader(1,null,loaderCallbacks);
        return mainview;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    LoaderManager.LoaderCallbacks loaderCallbacks = new LoaderManager.LoaderCallbacks() {
        @Override
        public Loader onCreateLoader(int id, Bundle args) {
            progressBar.setVisibility(View.VISIBLE);
            if(context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(context, "Allow Notify to access phone contacts", Toast.LENGTH_SHORT).show();
                return null;
            }
            Uri CONTACT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            CursorLoader cursorLoader = new CursorLoader(context, CONTACT_URI, null, null, null, null);
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
                CheckBox checkBox = (CheckBox) view.findViewById(R.id.contact_checkBox);
                boolean checkBoxChecked = checkBox.isChecked();
                if(checkBoxChecked){
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Do you want to block all messages from this contact ?");
                    builder.setPositiveButton("Block", (paramDialogInterface, paramInt) -> {
                        // block messages from this contact
                        // save to firestore // // add to the denied contacts
                        checkBox.setChecked(false);
                        Toast.makeText(context, "Messages from this read won't be read", Toast.LENGTH_SHORT).show();
                    });
                    builder.setNegativeButton("Cancel", (paramDialogInterface, paramInt) -> Toast.makeText(context, "cancel", Toast.LENGTH_SHORT).show());
                    builder.show();
                }else{
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Do you want Notify to read all messages from this contact ?");
                    builder.setPositiveButton("Unblock", (paramDialogInterface, paramInt) -> {
                        // unblock messages from this contact
                        // delete from firestore // remove from the denied contacts
                        checkBox.setChecked(true);
                        Toast.makeText(context, "Messages from this contact will be read", Toast.LENGTH_SHORT).show();
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
            boolean checked = true; // check whether this contact name is saved in firestore // set checked to 'false' // among contacts denied ---> first load them in an array inside onCreate()
            holder.ContactCheckBox.setChecked(checked);
            return view;
        }

        class Holder {
            TextView ContactName, ContactNumber;
            CheckBox ContactCheckBox;
        }
    }

}

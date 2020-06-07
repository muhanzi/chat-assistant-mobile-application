package com.example.dell.notify.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.dell.notify.R;
import com.example.dell.notify.activities.ShowFragments;

import static com.example.dell.notify.activities.ShowFragments.mypager;

public class Page3 extends Fragment {

    public static View mainview;
    private TextView sign_up,sign_in;
    private Button grant_permissions;
    private Context context;
    private final int REQ_CODE_PERMISSIONS_FOR_SMS_AUDIO_CONTACTS = 212;

    public Page3() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainview = inflater.inflate(R.layout.fragment_page3, container, false);
        sign_up = mainview.findViewById(R.id.sign_up);
        sign_in = mainview.findViewById(R.id.sign_in);
        grant_permissions = mainview.findViewById(R.id.grant_permission_button);
        context = getContext();
        sign_up.setOnClickListener((view)-> mypager.setCurrentItem(4));
        sign_in.setOnClickListener((view)-> mypager.setCurrentItem(3));
        grant_permissions.setOnClickListener((view)-> {
            check_permissions();
            change_sleep_timeout();
        });
        return mainview;
    }

    private void check_permissions(){
        ActivityCompat.requestPermissions(ShowFragments.instance,
                new String[]{Manifest.permission.SEND_SMS,Manifest.permission.READ_SMS,
                        Manifest.permission.RECEIVE_SMS,Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_CONTACTS}
                        ,REQ_CODE_PERMISSIONS_FOR_SMS_AUDIO_CONTACTS);
    }

    private void change_sleep_timeout(){
        if(Settings.System.canWrite(context)){  // when permission to change settings is allowed
            Settings.System.putInt(context.getContentResolver(),Settings.System.SCREEN_OFF_TIMEOUT, 120000);
        }else{
            // when permission to write settings is not yet given
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            startActivity(intent);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        //
        if(context.checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                ){
            if(Settings.System.canWrite(context)){
                allPermissionsGranted();
            }else {
                grantPermissionsButton();
            }
        }else {
            grantPermissionsButton();
        }
        //
    }

    private void allPermissionsGranted(){
        grant_permissions.setEnabled(false);
        grant_permissions.setText(R.string.permissions_granted);
        grant_permissions.setBackground(ContextCompat.getDrawable(context,R.drawable.permissions_granted_button));
    }

    private void grantPermissionsButton(){
        grant_permissions.setEnabled(true);
        grant_permissions.setText(R.string.grant_permissions);
        grant_permissions.setBackground(ContextCompat.getDrawable(context,R.drawable.grant_permission_button_style));
    }

}

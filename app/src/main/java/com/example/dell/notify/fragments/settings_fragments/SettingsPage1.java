package com.example.dell.notify.fragments.settings_fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.dell.notify.R;

import java.util.ArrayList;
import java.util.List;

public class SettingsPage1 extends Fragment {

    public static View mainview;
    private Context context;
    private List<AppList> installedApps;
    private AppAdapter installedAppAdapter;
    ListView userInstalledApps;

    public SettingsPage1() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainview = inflater.inflate(R.layout.fragment_settings_page1, container, false);
        context = getContext();
        //
        userInstalledApps = (ListView) mainview.findViewById(R.id.installed_app_list);
        installedApps = getInstalledApps();
        installedAppAdapter = new AppAdapter(context, installedApps);
        userInstalledApps.setAdapter(installedAppAdapter);
        userInstalledApps.setOnItemClickListener((adapterView, view, position, l) -> {

            String[] actions = {" Block", " Cancel"};
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Choose Action").setMessage("Do you want to block all notifications from this app ?")
                    .setItems(actions, (dialog, which) -> {
                        if (which==0){
                            // block notifications from this app
                            // save to firestore // // add to the denied apps
                        }
                        if (which==1){
                            // cancel
                        }
                    });
            builder.show();

            /*
            CheckBox checkBox = (CheckBox) view.findViewById(R.id.app_checkBox);
            boolean checkBoxChecked = checkBox.isChecked();
            if(!checkBoxChecked){
                String[] actions = {" Block", " Cancel"};
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Choose Action").setMessage("Do you want to block all notifications from this app ?")
                        .setItems(actions, (dialog, which) -> {
                            if (which==0){
                                // block notifications from this app
                                // save to firestore // // add to the denied apps
                            }
                            if (which==1){
                                // cancel
                            }
                        });
                builder.show();
            }else{
                // !!!!!!!!!!! // use a class variable to store those apps in an array
                // or first check in firestore whether the appName or PackageName is among the apps that are denied
                String[] actions = {" Unblock", " Cancel"};
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Choose Action").setMessage("Do you want Notify to read all notifications from this app ?")
                        .setItems(actions, (dialog, which) -> {
                            if (which==0){
                                // unblock notifications from this app
                                // delete from firestore // remove from the denied apps
                            }
                            if (which==1){
                                // cancel
                            }
                        });
                builder.show();
            }
            */
        });
        //
        return mainview;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    private List<AppList> getInstalledApps() {
        PackageManager packageManager = context.getPackageManager();
        List<AppList> apps = new ArrayList<>();
        List<PackageInfo> packs = packageManager.getInstalledPackages(0);
        //List<PackageInfo> packs = getPackageManager().getInstalledPackages(PackageManager.GET_PERMISSIONS);
        for (int i = 0; i < packs.size(); i++) {
            PackageInfo p = packs.get(i);
            String appName = p.applicationInfo.loadLabel(packageManager).toString();
            Drawable icon = p.applicationInfo.loadIcon(packageManager);
            boolean checked = true; // check whether this package is saved in firestore // among apps denied ---> first load them in an array inside onCreate()
            apps.add(new AppList(appName, icon, checked));
        }
        return apps;
    }

    public class AppAdapter extends BaseAdapter {

        public LayoutInflater layoutInflater;
        public List<AppList> listStorage;

        public AppAdapter(Context context, List<AppList> customizedListView) {
            layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            listStorage = customizedListView;
        }

        @Override
        public int getCount() {
            return listStorage.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder listViewHolder;
            if (convertView == null) {
                listViewHolder = new ViewHolder();
                convertView = layoutInflater.inflate(R.layout.installed_app_list, parent, false);

                listViewHolder.textInListView = (TextView) convertView.findViewById(R.id.list_app_name);
                listViewHolder.imageInListView = (ImageView) convertView.findViewById(R.id.app_icon);
                listViewHolder.checkBoxInListView = (CheckBox) convertView.findViewById(R.id.app_checkBox);
                convertView.setTag(listViewHolder);  //
            } else {
                listViewHolder = (ViewHolder) convertView.getTag();  //
            }
            listViewHolder.textInListView.setText(listStorage.get(position).getName());
            listViewHolder.imageInListView.setImageDrawable(listStorage.get(position).getIcon());
            listViewHolder.checkBoxInListView.setChecked(listStorage.get(position).getChecked());
            //

            return convertView;
        }

        class ViewHolder {
            TextView textInListView;
            ImageView imageInListView;
            CheckBox checkBoxInListView;
        }
    }

    public class AppList {
        private String name;
        Drawable icon;
        private boolean checked;

        public AppList(String name, Drawable icon, boolean checked) {
            this.name = name;
            this.icon = icon;
            this.checked = checked;
        }

        public String getName() {
            return name;
        }

        public Drawable getIcon() {
            return icon;
        }

        public boolean getChecked(){
            return checked;
        }

    }

}

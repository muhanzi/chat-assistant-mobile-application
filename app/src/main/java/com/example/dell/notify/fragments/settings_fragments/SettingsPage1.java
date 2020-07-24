package com.example.dell.notify.fragments.settings_fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.dell.notify.R;

import java.util.ArrayList;
import java.util.List;

public class SettingsPage1 extends Fragment {

    public static View mainview;
    private Context context;
    private List<AppList> installedApps;
    private AppAdapter installedAppAdapter;
    ListView userInstalledApps;
    private ProgressDialog prog;

    public SettingsPage1() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainview = inflater.inflate(R.layout.fragment_settings_page1, container, false);
        context = getContext();
        //
        userInstalledApps = (ListView) mainview.findViewById(R.id.installed_app_list);
        prog =new ProgressDialog(context);
        prog.setMessage("Loading...");
        prog.setCancelable(false);
        LoadApps loadApps = new LoadApps();
        loadApps.execute();
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
        for (int i = 0; i < packs.size(); i++) {
            PackageInfo p = packs.get(i);
            if ((!isSystemPackage(p))) {
                String appName = p.applicationInfo.loadLabel(packageManager).toString();
                Drawable icon = p.applicationInfo.loadIcon(packageManager);
                boolean checked = true; // check whether this package is saved in firestore // set checked to 'false' // among apps denied ---> first load them in an array inside onCreate()
                apps.add(new AppList(appName, icon, checked));
            }
        }
        return apps;
    }

    private boolean isSystemPackage(PackageInfo pkgInfo) {
        return (pkgInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
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

    class LoadApps extends AsyncTask<Void, Void, Void> {   // to execute tasks on the worker thread
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            prog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            //
            installedApps = getInstalledApps();
            //
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //
            installedAppAdapter = new AppAdapter(context, installedApps);
            userInstalledApps.setAdapter(installedAppAdapter);
            userInstalledApps.setOnItemClickListener((adapterView, view, position, l) -> {
                CheckBox checkBox = (CheckBox) view.findViewById(R.id.app_checkBox);
                boolean checkBoxChecked = checkBox.isChecked();
                if(checkBoxChecked){
                    String[] actions = {" Block", " Cancel"};
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Do you want to block all notifications from this app ?");
                    builder.setPositiveButton("Block", (paramDialogInterface, paramInt) -> {
                        // block notifications from this app
                        // save to firestore // // add to the denied apps
                        checkBox.setChecked(false);
                        Toast.makeText(context, "Notifications from this app won't be read", Toast.LENGTH_SHORT).show();
                    });
                    builder.setNegativeButton("Cancel", (paramDialogInterface, paramInt) -> Toast.makeText(context, "cancel", Toast.LENGTH_SHORT).show());
                    builder.show();
                }else{
                    String[] actions = {" Unblock", " Cancel"};
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Do you want Notify to read all notifications from this app ?");
                    builder.setPositiveButton("Unblock", (paramDialogInterface, paramInt) -> {
                        // unblock notifications from this app
                        // delete from firestore // remove from the denied apps
                        checkBox.setChecked(true);
                        Toast.makeText(context, "Notifications from this app will be read", Toast.LENGTH_SHORT).show();
                    });
                    builder.setNegativeButton("Cancel", (paramDialogInterface, paramInt) -> Toast.makeText(context, "cancel", Toast.LENGTH_SHORT).show());
                    builder.show();
                }
            });
            // hide progress dialog
            prog.dismiss();
            //
        }
    }

}

package com.muhanzi.notify.fragments.settings_fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.muhanzi.notify.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

public class SettingsPage1 extends Fragment {

    public static View mainview;
    private Context context;
    private List<AppList> installedApps;
    private AppAdapter installedAppAdapter;
    ListView userInstalledApps;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser firebaseUser;
    private final String TAG = "SettingsPage1";
    private ArrayList<String> blockedApps;
    private ProgressDialog progressDialog;

    public SettingsPage1() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainview = inflater.inflate(R.layout.fragment_settings_page1, container, false);
        context = getContext();
        mAuth = FirebaseAuth.getInstance();
        firebaseUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        blockedApps = new ArrayList<>();
        progressDialog =new ProgressDialog(context);
        progressDialog.setMessage("please wait");
        progressDialog.setCancelable(false);
        userInstalledApps = (ListView) mainview.findViewById(R.id.installed_app_list);
        progressBar = (ProgressBar) mainview.findViewById(R.id.progressBarPage1);
        progressBar.setVisibility(View.VISIBLE);
        db.collection("users").document(firebaseUser.getUid())
                .collection("blockedApps").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                //Log.d(TAG, document.getId() + " => " + document.getData());
                                blockedApps.add(document.getString("appName"));
                            }
                            LoadApps loadApps = new LoadApps();
                            loadApps.execute();
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

    private List<AppList> getInstalledApps() {
        PackageManager packageManager = context.getPackageManager();
        List<AppList> apps = new ArrayList<>();
        List<PackageInfo> packs = packageManager.getInstalledPackages(0);
        for (int i = 0; i < packs.size(); i++) {
            PackageInfo p = packs.get(i);
            if ((!isSystemPackage(p))) {
                String appName = p.applicationInfo.loadLabel(packageManager).toString();
                String packageName = p.applicationInfo.packageName;
                Drawable icon = p.applicationInfo.loadIcon(packageManager);
                apps.add(new AppList(appName, packageName, icon, checkApp(appName)));
            }
        }
        return apps;
    }

    private boolean checkApp(String appName){
        for(String app : blockedApps){
            if(appName.equals(app)){
                return false;
            }
        }
        return true;
    }

    private boolean isSystemPackage(PackageInfo pkgInfo) {
        return (pkgInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    private class AppAdapter extends BaseAdapter {

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

                listViewHolder.appNameTextView = (TextView) convertView.findViewById(R.id.list_app_name);
                listViewHolder.packageNameTextView = (TextView) convertView.findViewById(R.id.packageName);
                listViewHolder.imageInListView = (ImageView) convertView.findViewById(R.id.app_icon);
                listViewHolder.checkBoxInListView = (CheckBox) convertView.findViewById(R.id.app_checkBox);
                convertView.setTag(listViewHolder);  //
            } else {
                listViewHolder = (ViewHolder) convertView.getTag();  //
            }
            listViewHolder.appNameTextView.setText(listStorage.get(position).getName());
            listViewHolder.packageNameTextView.setText(listStorage.get(position).getPackageName());
            listViewHolder.imageInListView.setImageDrawable(listStorage.get(position).getIcon());
            listViewHolder.checkBoxInListView.setChecked(listStorage.get(position).getChecked());
            //

            return convertView;
        }

        class ViewHolder {
            TextView appNameTextView,packageNameTextView;
            ImageView imageInListView;
            CheckBox checkBoxInListView;
        }
    }

    private class AppList {
        private String name,packageName;
        Drawable icon;
        private boolean checked;

        private AppList(String name,String packageName, Drawable icon, boolean checked) {
            this.name = name;
            this.icon = icon;
            this.checked = checked;
            this.packageName = packageName;
        }

        public String getName() {
            return name;
        }

        public String getPackageName() {
            return packageName;
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
        }

        @Override
        protected Void doInBackground(Void... voids) {
            //
            installedApps = getInstalledApps();
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
                TextView app_name = (TextView) view.findViewById(R.id.list_app_name);
                TextView package_name = (TextView) view.findViewById(R.id.packageName);
                boolean checkBoxChecked = checkBox.isChecked();
                if(checkBoxChecked){
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Do you want to block all notifications from this app ?");
                    builder.setPositiveButton("Block", (paramDialogInterface, paramInt) -> {
                        progressDialog.show();
                        Map<String, Object> app = new HashMap<>();
                        app.put("appName", app_name.getText());
                        app.put("packageName", package_name.getText());
                        db.collection("users").document(firebaseUser.getUid())
                                .collection("blockedApps").document(package_name.getText().toString()).set(app)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()){
                                            progressDialog.dismiss();
                                            checkBox.setChecked(false);
                                            Toast.makeText(context, "Notifications from this app won't be read", Toast.LENGTH_SHORT).show();
                                        }else{
                                            progressDialog.dismiss();
                                            Toast.makeText(context, "An Error Occurred, Try again", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                        //
                    });
                    builder.setNegativeButton("Cancel", (paramDialogInterface, paramInt) -> Toast.makeText(context, "cancel", Toast.LENGTH_SHORT).show());
                    builder.show();
                }else{
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Do you want Notify to read all notifications from this app ?");
                    builder.setPositiveButton("Unblock", (paramDialogInterface, paramInt) -> {
                        //
                        progressDialog.show();
                        db.collection("users").document(firebaseUser.getUid())
                                .collection("blockedApps").document(package_name.getText().toString()).delete()
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()){
                                            progressDialog.dismiss();
                                            checkBox.setChecked(true);
                                            Toast.makeText(context, "Notifications from this app will now be read", Toast.LENGTH_SHORT).show();
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
            // hide progress bar
            progressBar.setVisibility(View.GONE);
            //
        }
    }

}

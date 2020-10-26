package com.muhanzi.notify.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.muhanzi.notify.R;

public class ReportedSpams extends AppCompatActivity {

    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private FirebaseAuth mAuth;
    private FirebaseUser firebaseUser;
    private FirebaseFirestore db;
    private FirestoreRecyclerAdapter adapter;
    private LinearLayoutManager linearLayoutManager;
    private Toolbar tool_bar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reported_spams);

        tool_bar =(Toolbar) findViewById(R.id.reportedSpamsAppBar);
        setSupportActionBar(tool_bar);
        getSupportActionBar().setTitle(R.string.spam_report);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mAuth = FirebaseAuth.getInstance();
        firebaseUser = mAuth.getCurrentUser();
        progressBar = (ProgressBar) findViewById(R.id.reported_spams_progress_bar);
        recyclerView = (RecyclerView) findViewById(R.id.reported_spams_recyclerView);
        init();
        getSpams();
    }

    private void init(){
        linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        db = FirebaseFirestore.getInstance();
    }

    private void getSpams(){
        Query query = db.collection("users").document(firebaseUser.getUid()).collection("spams");
        FirestoreRecyclerOptions<Spam> firestoreRecyclerOptions = new FirestoreRecyclerOptions.Builder<Spam>()
                .setQuery(query, Spam.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<Spam, ReportedSpams.SpamHolder>(firestoreRecyclerOptions) {
            @Override
            public void onBindViewHolder(ReportedSpams.SpamHolder holder, int position, Spam model) {
                progressBar.setVisibility(View.GONE);
                holder.title.setText(model.getNotificationTitle());
                holder.text.setText(model.getSpamText());
                holder.dateTime.setText(String.format("%s   %s", model.getDate(), model.getTime()));
                try{
                    PackageManager packageManager = getApplicationContext().getPackageManager();
                    ApplicationInfo appInfo = packageManager.getApplicationInfo(model.getPackageName(), 0);
                    Drawable icon = packageManager.getApplicationIcon(appInfo);
                    String name = packageManager.getApplicationLabel(appInfo).toString();
                    holder.appIcon.setImageDrawable(icon);
                    holder.appName.setText(name);
                }catch(PackageManager.NameNotFoundException e){
                    e.printStackTrace();
                }
                holder.itemView.setOnClickListener(v -> {
                    // todo
                });
            }

            @Override
            public ReportedSpams.SpamHolder onCreateViewHolder(ViewGroup group, int i) {
                View view = LayoutInflater.from(group.getContext())
                        .inflate(R.layout.reported_spam_list_item, group, false);
                return new ReportedSpams.SpamHolder(view);
            }

            @Override
            public void onError(FirebaseFirestoreException e) {
                Log.e("error", e.getMessage());
            }
        };

        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(adapter);
    }



    public class SpamHolder extends RecyclerView.ViewHolder {
        TextView title,text,dateTime,appName;
        ImageView appIcon;

        public SpamHolder(View itemView) {
            super(itemView);
            this.title = itemView.findViewById(R.id.titleText);
            this.text = itemView.findViewById(R.id.spamText);
            this.dateTime = itemView.findViewById(R.id.dateTime);
            this.appName = itemView.findViewById(R.id.appName);
            this.appIcon = itemView.findViewById(R.id.appIcon);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        adapter.stopListening();
    }

}
package com.muhanzi.notify.fragments.settings_fragments;

import android.content.Context;
import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.muhanzi.notify.R;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SettingsPage3 extends Fragment {

    public static View mainview;
    private Context context;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private FirebaseAuth mAuth;
    private FirebaseUser firebaseUser;
    private FirebaseFirestore db;
    private FirestoreRecyclerAdapter adapter;
    private LinearLayoutManager linearLayoutManager;


    public SettingsPage3() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainview = inflater.inflate(R.layout.fragment_settings_page3, container, false);
        context = getContext();
        mAuth = FirebaseAuth.getInstance();
        firebaseUser = mAuth.getCurrentUser();
        progressBar = (ProgressBar) mainview.findViewById(R.id.progressBarPage3);
        recyclerView = (RecyclerView) mainview.findViewById(R.id.dictionary_recyclerView);
        init();
        getFriendList();
        return mainview;
    }

    private void init(){
        linearLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        db = FirebaseFirestore.getInstance();
    }

    private void getFriendList(){
        Query query = db.collection("users").document(firebaseUser.getUid()).collection("dictionary");
        FirestoreRecyclerOptions<Dictionary> firestoreRecyclerOptions = new FirestoreRecyclerOptions.Builder<Dictionary>()
                .setQuery(query, Dictionary.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<Dictionary, DictionaryHolder>(firestoreRecyclerOptions) {
            @Override
            public void onBindViewHolder(DictionaryHolder holder, int position, Dictionary model) {
                progressBar.setVisibility(View.GONE);
                holder.abbreviation.setText(model.getAbbreviation().toUpperCase());
                holder.meaning.setText(model.getMeaning());
                holder.itemView.setOnClickListener(v -> {
                    Snackbar.make(recyclerView, model.getAbbreviation()+" : "+model.getMeaning(), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                });
            }

            @Override
            public DictionaryHolder onCreateViewHolder(ViewGroup group, int i) {
                View view = LayoutInflater.from(group.getContext())
                        .inflate(R.layout.dictionary_list_item, group, false);
                return new DictionaryHolder(view);
            }

            @Override
            public void onError(FirebaseFirestoreException e) {
                Log.e("error", e.getMessage());
            }
        };

        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(adapter);
    }



    public class DictionaryHolder extends RecyclerView.ViewHolder {
        TextView abbreviation,meaning;

        public DictionaryHolder(View itemView) {
            super(itemView);
            this.abbreviation = itemView.findViewById(R.id.abbreviation);
            this.meaning = itemView.findViewById(R.id.meaning);
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

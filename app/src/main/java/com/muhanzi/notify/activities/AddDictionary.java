package com.muhanzi.notify.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.muhanzi.notify.R;

import java.util.HashMap;
import java.util.Map;

public class AddDictionary extends AppCompatActivity {

    private Toolbar tool_bar;
    private EditText abbreviation,meaning;
    private Button delete,save;
    private ProgressDialog progressDialog;
    private FirebaseAuth mAuth;
    private FirebaseUser firebaseUser;
    private String extra_abbreviation,extra_meaning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_dictionary);

        tool_bar =(Toolbar) findViewById(R.id.AddDictionaryAppBar);
        setSupportActionBar(tool_bar);
        getSupportActionBar().setTitle(R.string.editDictionary);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        abbreviation=findViewById(R.id.editTextAbbreviation);
        meaning=findViewById(R.id.editTextMeaning);
        delete=findViewById(R.id.delete_button);
        save=findViewById(R.id.save_button);
        progressDialog =new ProgressDialog(this);
        progressDialog.setMessage("please wait");
        progressDialog.setCancelable(false);
        mAuth = FirebaseAuth.getInstance();
        firebaseUser = mAuth.getCurrentUser();
        Intent intent = getIntent();
        extra_abbreviation = intent.getStringExtra("abbreviation");
        extra_meaning = intent.getStringExtra("meaning");

        if(!extra_abbreviation.isEmpty() && !extra_meaning.isEmpty()){
            abbreviation.setText(extra_abbreviation);
            meaning.setText(extra_meaning);
            save.setText(R.string.update_button_text);
            delete.setEnabled(true);
            save.setOnClickListener(view -> updateDictionary());
        }else{
            save.setText(R.string.save_button_text);
            delete.setEnabled(false);
            save.setOnClickListener(view -> saveToDictionary());
        }

        delete.setOnClickListener(view -> deleteFromDictionary());

    }


    private void saveToDictionary(){
        String abbreviation_text = abbreviation.getText().toString().trim();
        String meaning_text = meaning.getText().toString().trim();
        if(abbreviation_text.isEmpty()){
            Toast.makeText(this, "Abbreviation text is required", Toast.LENGTH_SHORT).show();
            abbreviation.requestFocus();
            return;
        }
        if(meaning_text.isEmpty()){
            Toast.makeText(this, "Meaning text is required", Toast.LENGTH_SHORT).show();
            meaning.requestFocus();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you want to save this Abbreviation to your dictionary ?");
        builder.setPositiveButton("SAVE", (paramDialogInterface, paramInt) -> {
            progressDialog.show();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String documentID = abbreviation_text.toLowerCase().replaceAll(" ","");
            Map<String, Object> word = new HashMap<>();
            word.put("abbreviation", abbreviation_text.toLowerCase());
            word.put("meaning", meaning_text.toLowerCase());
            db.collection("users").document(firebaseUser.getUid())
                    .collection("dictionary").document(documentID)
                    .set(word)
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()){
                            progressDialog.dismiss();
                            Toast.makeText(AddDictionary.this, "Abbreviation saved successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        }else {
                            progressDialog.dismiss();
                            Toast.makeText(AddDictionary.this, "Failed to save the Abbreviation, try again",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        builder.setNegativeButton("CANCEL", (paramDialogInterface, paramInt) -> Toast.makeText(this, "canceled", Toast.LENGTH_SHORT).show());
        builder.show();
    }

    private void updateDictionary(){
        String abbreviation_text = abbreviation.getText().toString().trim();
        String meaning_text = meaning.getText().toString().trim();
        if(abbreviation_text.isEmpty()){
            Toast.makeText(this, "Abbreviation text is required", Toast.LENGTH_SHORT).show();
            abbreviation.requestFocus();
            return;
        }
        if(meaning_text.isEmpty()){
            Toast.makeText(this, "Meaning text is required", Toast.LENGTH_SHORT).show();
            meaning.requestFocus();
            return;
        }

        if(!abbreviation_text.equals(extra_abbreviation)){
            deleteThenCreate(abbreviation_text,meaning_text);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to change this Abbreviation ?");
        builder.setPositiveButton("UPDATE", (paramDialogInterface, paramInt) -> {
            progressDialog.show();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String documentID = abbreviation_text.toLowerCase().replaceAll(" ","");
            Map<String, Object> word = new HashMap<>();
            word.put("abbreviation", abbreviation_text.toLowerCase());
            word.put("meaning", meaning_text.toLowerCase());
            db.collection("users").document(firebaseUser.getUid())
                    .collection("dictionary").document(documentID)
                    .update(word)
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()){
                            progressDialog.dismiss();
                            Toast.makeText(AddDictionary.this, "Abbreviation updated successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        }else {
                            progressDialog.dismiss();
                            Toast.makeText(AddDictionary.this, "Failed to update the Abbreviation, try again",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        builder.setNegativeButton("CANCEL", (paramDialogInterface, paramInt) -> Toast.makeText(this, "canceled", Toast.LENGTH_SHORT).show());
        builder.show();
    }

    private void deleteFromDictionary(){
        String abbreviation_text = abbreviation.getText().toString().trim().toLowerCase();
        String meaning_text = meaning.getText().toString().trim().toLowerCase();
        if(!abbreviation_text.isEmpty() && !meaning_text.isEmpty()){
            if(abbreviation_text.equals(extra_abbreviation)){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Are you sure you want to delete this Abbreviation from your dictionary ?");
                builder.setPositiveButton("DELETE", (paramDialogInterface, paramInt) -> {
                    progressDialog.show();
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    String documentID = abbreviation_text.toLowerCase().replaceAll(" ","");
                    db.collection("users").document(firebaseUser.getUid())
                            .collection("dictionary").document(documentID).delete()
                            .addOnCompleteListener(task -> {
                                if(task.isSuccessful()){
                                    progressDialog.dismiss();
                                    Toast.makeText(AddDictionary.this, "Abbreviation successfully deleted", Toast.LENGTH_SHORT).show();
                                    finish();
                                }else {
                                    progressDialog.dismiss();
                                    Toast.makeText(AddDictionary.this, "Failed to delete the Abbreviation, try again",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                });
                builder.setNegativeButton("CANCEL", (paramDialogInterface, paramInt) -> Toast.makeText(this, "canceled", Toast.LENGTH_SHORT).show());
                builder.show();
            }else{
                Snackbar.make(findViewById(R.id.delete_button), "The Abbreviation is incorrect", Snackbar.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(this, "Fields are empty", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteThenCreate(String abbreviation_text,String meaning_text) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to change this Abbreviation ?");
        builder.setPositiveButton("UPDATE", (paramDialogInterface, paramInt) -> {
            progressDialog.show();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String documentID = extra_abbreviation.toLowerCase().replaceAll(" ","");
            db.collection("users").document(firebaseUser.getUid())
                    .collection("dictionary").document(documentID)
                    .delete()
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()){
                            Map<String, Object> word = new HashMap<>();
                            word.put("abbreviation", abbreviation_text.toLowerCase());
                            word.put("meaning", meaning_text.toLowerCase());
                            db.collection("users").document(firebaseUser.getUid())
                                    .collection("dictionary").document(abbreviation_text.toLowerCase().replaceAll(" ",""))
                                    .set(word)
                                    .addOnCompleteListener(task1 -> {
                                        if(task1.isSuccessful()){
                                            progressDialog.dismiss();
                                            Toast.makeText(AddDictionary.this, "Abbreviation updated successfully", Toast.LENGTH_SHORT).show();
                                            finish();
                                        }else {
                                            progressDialog.dismiss();
                                            Toast.makeText(AddDictionary.this, "Failed to update the Abbreviation, try again",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }else {
                            progressDialog.dismiss();
                            Toast.makeText(AddDictionary.this, "Failed to update the Abbreviation, try again",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        builder.setNegativeButton("CANCEL", (paramDialogInterface, paramInt) -> Toast.makeText(this, "canceled", Toast.LENGTH_SHORT).show());
        builder.show();
    }

}
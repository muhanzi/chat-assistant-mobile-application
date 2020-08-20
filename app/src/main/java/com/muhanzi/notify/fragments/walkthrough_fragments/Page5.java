package com.muhanzi.notify.fragments.walkthrough_fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.muhanzi.notify.R;
import com.muhanzi.notify.activities.ShowFragments;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import static com.muhanzi.notify.activities.ShowFragments.mypager;

public class Page5 extends Fragment {

    private ImageView page5_previous;
    private View mainview;
    private TextInputLayout name,email,password;
    private Button signUp,signUpWithFacebook;
    private ProgressDialog prog;
    private FirebaseAuth mAuth;
    private TextView signIn;
    private final String TAG = "createUserWithEmail";

    public Page5() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainview= inflater.inflate(R.layout.fragment_page5, container, false);
        page5_previous= mainview.findViewById(R.id.page5_previous);
        name= mainview.findViewById(R.id.input_layout_name);
        email= mainview.findViewById(R.id.input_layout_email);
        password= mainview.findViewById(R.id.input_layout_password);
        signUp= mainview.findViewById(R.id.sign_up_button);
        signUpWithFacebook= mainview.findViewById(R.id.sign_up_with_facebook);
        signIn = mainview.findViewById(R.id.already_have_account);
        prog =new ProgressDialog(getContext());
        prog.setMessage("please wait");
        prog.setCancelable(false);
        mAuth = FirebaseAuth.getInstance();
        page5_previous.setOnClickListener((view)-> mypager.setCurrentItem(3));
        signUp.setOnClickListener((view)-> signUp());
        signIn.setOnClickListener(view -> mypager.setCurrentItem(3));
        return mainview;
    }

    private void signUp() {
        prog.show();
        String nametext= name.getEditText().getText().toString();
        String emailtext= email.getEditText().getText().toString();
        String passwordtext= password.getEditText().getText().toString();

        if(nametext.isEmpty()){
            name.setError("name is required");
            prog.dismiss();
            return;
        }else if(emailtext.isEmpty()){
            email.setError("email is required");
            prog.dismiss();
            return;
        }else if(passwordtext.isEmpty()){
            password.setError("password is required");
            prog.dismiss();
            return;
        }

        mAuth.createUserWithEmailAndPassword(emailtext, passwordtext)
                .addOnCompleteListener(getActivity(), task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "createUserWithEmail:success");
                        //
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        Map<String, Object> user = new HashMap<>();
                        user.put("name", nametext);
                        user.put("email", emailtext);
                        db.collection("users").document(firebaseUser.getUid())
                                .set(user)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        String abbreviation = "lol";
                                        String meaning = "laugh out loud";
                                        Map<String, Object> word = new HashMap<>();
                                        word.put("abbreviation", abbreviation);
                                        word.put("meaning", meaning);
                                        db.collection("users").document(firebaseUser.getUid())
                                                .collection("dictionary").document(abbreviation.toLowerCase().replaceAll(" ",""))
                                                .set(word)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if(task.isSuccessful()){
                                                            ShowFragments.instance.startMainActivity(firebaseUser);
                                                        }else {
                                                            prog.dismiss();
                                                            Toast.makeText(getContext(), "Failed to add user dictionary",
                                                                    Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w(TAG, "Error adding document", e);
                                        prog.dismiss();
                                        Toast.makeText(getContext(), "Failed to add user details",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        // If sign in fails, display a message to the user.
                        prog.dismiss();
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(getContext(), "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                    }
                });

    }

    /*
    @Override
    public void onStart() {
        super.onStart();
    }
    */

}

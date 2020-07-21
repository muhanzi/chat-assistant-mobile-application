package com.example.dell.notify.fragments.walkthrough_fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.dell.notify.R;
import com.example.dell.notify.activities.MainActivity;
import com.example.dell.notify.activities.ShowFragments;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import static android.support.constraint.Constraints.TAG;
import static com.example.dell.notify.activities.ShowFragments.mypager;

public class Page5 extends Fragment {

    private ImageView page5_previous;
    private View mainview;
    private TextInputLayout name,email,password;
    private Button signUp,signUpWithFacebook;
    private ProgressDialog prog;
    private FirebaseAuth mAuth;

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
        prog =new ProgressDialog(getContext());
        prog.setMessage("please wait");
        prog.setCancelable(false);
        mAuth = FirebaseAuth.getInstance();
        page5_previous.setOnClickListener((view)-> mypager.setCurrentItem(3));
        signUp.setOnClickListener((view)-> signUp());
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
                        FirebaseUser user = mAuth.getCurrentUser();
                        ShowFragments.instance.startMainActivity(user);
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

package com.example.dell.notify.fragments.walkthrough_fragments;

import android.app.ProgressDialog;
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
import com.example.dell.notify.activities.ShowFragments;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import static android.support.constraint.Constraints.TAG;
import static com.example.dell.notify.activities.ShowFragments.mypager;

public class Page4 extends Fragment {

    private ImageView page4_previous,page4_next;
    private View mainview;
    private ProgressDialog prog;
    private TextInputLayout email,password;
    private Button signIn,signInWithFacebook;
    private FirebaseAuth mAuth;


    public Page4() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainview= inflater.inflate(R.layout.fragment_page4, container, false);
        page4_previous= mainview.findViewById(R.id.page4_previous);
        page4_next= mainview.findViewById(R.id.page4_next);
        email= mainview.findViewById(R.id.input_layout_email);
        password= mainview.findViewById(R.id.input_layout_password);
        signIn= mainview.findViewById(R.id.sign_in_button);
        signInWithFacebook= mainview.findViewById(R.id.sign_in_with_facebook);
        prog =new ProgressDialog(getContext());
        prog.setMessage("please wait");
        prog.setCancelable(false);
        mAuth = FirebaseAuth.getInstance();
        page4_previous.setOnClickListener((view)-> mypager.setCurrentItem(2));
        page4_next.setOnClickListener((view)-> mypager.setCurrentItem(4));
        signIn.setOnClickListener((view -> signIn()));

        return mainview;
    }

    private void signIn() {
        prog.show();
        String emailtext= email.getEditText().getText().toString();
        String passwordtext= password.getEditText().getText().toString();
        if(emailtext.isEmpty()){
            email.setError("email is required");
            prog.dismiss();
            return;
        }else if(passwordtext.isEmpty()){
            password.setError("password is required");
            prog.dismiss();
            return;
        }
        mAuth.signInWithEmailAndPassword(emailtext, passwordtext)
                .addOnCompleteListener(getActivity(), task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        ShowFragments.instance.startMainActivity(user);
                    } else {
                        // If sign in fails, display a message to the user.
                        prog.dismiss();
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
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

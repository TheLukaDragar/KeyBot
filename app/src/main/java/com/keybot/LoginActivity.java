package com.keybot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends Activity {

    private Button googleBtn;

    private GoogleSignInClient mGoogleSignInClient;
    private TextView TextV1;
    private TextView TextV2;
    private FirebaseAuth mAuth;
    private int RC_SIGN_IN = 123;
    private String TAG = "LoginActivity";
    private String username ="";
    private String userId;
    private String userEmail;
    private String userPhoto;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        googleBtn = findViewById(R.id.signInButton);

        TextV1=findViewById(R.id.textViewKeyBot);
        TextV2=findViewById(R.id.textViewlogin);





        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.tokenid))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // [START initialize_auth]
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        updateUI(0);


        googleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                signIn();

            }
        });


    }


    private void updateUI(int action){

        switch (action){
            case 0:

                //default

                TextV1.setVisibility(View.VISIBLE);
                TextV2.setVisibility(View.VISIBLE);
                googleBtn.setVisibility(View.VISIBLE);

                break;


            case 2:
                //sucess
                TextV1.setVisibility(View.VISIBLE);
                TextV1.setText("Welcome back!");
                TextV2.setVisibility(View.GONE);
                googleBtn.setVisibility(View.GONE);





                break;

            case 3:
                //sucess
                TextV1.setVisibility(View.VISIBLE);
                TextV1.setText("Welcome!");
                TextV2.setVisibility(View.GONE);
                googleBtn.setVisibility(View.GONE);





                break;


            default:



        }






    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Snackbar.make(findViewById(R.id.loginconstr), "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                updateUI(0);
                Log.w(TAG, "Google sign in failed", e);
                // ...
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                           boolean isnewuser=  task.getResult().getAdditionalUserInfo().isNewUser();

                           if (isnewuser){
                               updateUI(3);

                           }else {
                               updateUI(2);



                           }

                           addUserToFirebaseDB();

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Snackbar.make(findViewById(R.id.loginconstr), "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                            updateUI(0);
                        }

                        // ...
                    }
                });
    }

    private void addUserToFirebaseDB() {

        FCMHandler fmchandle= new FCMHandler();
        fmchandle.enableFCM();



        userEmail=mAuth.getCurrentUser().getEmail();
        userId=mAuth.getCurrentUser().getUid();
        username=mAuth.getCurrentUser().getDisplayName();
        userPhoto=mAuth.getCurrentUser().getPhotoUrl().toString();






        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Create a new user with a first and last name
        Map<String, Object> user1 = new HashMap<>();
        user1.put("username", username);
        user1.put("email", userEmail);
        user1.put("id", userId);
        user1.put("photo_url", userPhoto);


        db.collection("Users").document(userId)
                .set(user1, SetOptions.merge()).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "DocumentSnapshot successfully written!");

                Intent myIntent = new Intent(LoginActivity.this, MainActivity.class);
                LoginActivity.this.startActivity(myIntent);
                finish();
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        updateUI(0);

                        Snackbar.make(findViewById(R.id.loginconstr), "Authentication Failed.", Snackbar.LENGTH_LONG).show();
                        Log.w(TAG, "Error writing document", e);
                    }
                });




    }




}

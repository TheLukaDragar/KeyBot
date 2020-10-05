package com.keybot;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.RemoteMessage;

public class NotificationActivity extends AppCompatActivity {

    private Activity context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        Bundle extras = getIntent().getExtras();

        Log.d("NotificationActivity", "NotificationActivity - onCreate - extras: " + extras);


        if (FirebaseAuth.getInstance().getCurrentUser()==null){

            Log.d("NotificationActivity", "NotificationActivity - onCreate User not signed in");


            context.finish();
            return;



        }

        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode

        String device_to_use_address = pref.getString("device_to_use_address", null);

        if (device_to_use_address==null || device_to_use_address.length()==0){
            Log.d("NotificationActivity", "NotificationActivity - onCreate no devices saved in device_to_use_address to add them");


            context.finish();
            return;


        }

        if (extras == null) {
            context.finish();
            return;
        }

        RemoteMessage msg = (RemoteMessage) extras.get("msg");

        if (msg == null) {
            context.finish();
            return;
        }

        RemoteMessage.Notification notification = msg.getNotification();

        if (notification == null) {
            context.finish();
            return;
        }

        String dialogMessage;
        try {
            dialogMessage = notification.getBody();
        } catch (Exception e) {
            context.finish();
            return;
        }
        String dialogTitle = notification.getTitle();
        if (dialogTitle == null || dialogTitle.length() == 0) {
            dialogTitle = "";
        }
        String senderUID;
        try {
            senderUID = (String) msg.getData().get("senderUid");
            Log.d("NotificationActivity", "NotificationActivity senderUid=) "+senderUID);

        } catch (Exception e) {
            context.finish();
            return;
        }



        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.myDialog));
        builder.setTitle(dialogTitle);
        builder.setMessage(dialogMessage);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
             addUserToUserList0(senderUID,device_to_use_address);
                dialog.cancel();

            }
        });
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.cancel();
                context.finish();

            }
        });
        AlertDialog alert = builder.create();
        alert.show();

    }

    private void addUserToUserList0(String user_uuid,String device_address){


        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference documentReference= db.collection("Devices").document(device_address);
        documentReference.update("device_users", FieldValue.arrayUnion(user_uuid)).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {


                Toast.makeText(getApplicationContext() , "User is now authorized", Toast.LENGTH_SHORT).show();
                context.finish();
            }
        });








    }

}
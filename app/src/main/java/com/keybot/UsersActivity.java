package com.keybot;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.ShortDynamicLink;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UsersActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();
    HashMap<String, String> saved_device;
    private int active_device;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;

    private ArrayList<HashMap<String, String>> UsersInfo;
    private ArrayList<HashMap<String, String>> UsersInfoSorted;
    private  List<String> device_users ;
    private String dev_admin ;
    private String dev_name ;
    private String dev_pin ;
    private String dev_admin_pin ;
    private RecyclerView.Adapter MyRecyclerViewAdapter_Predmeti;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        saved_device = (HashMap<String, String>)intent.getSerializableExtra("saved_device");
        active_device = intent.getIntExtra("active_device",-1);
        Log.v(TAG, saved_device.get("device_name"));

        setContentView(R.layout.activity_users);

        initToolbar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        swipeRefreshLayout = findViewById(R.id.swipe_to_ref_users);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                GetFromDb();
            }
        });

        recyclerView= findViewById(R.id.users_recycle);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager2 = new LinearLayoutManager(UsersActivity.this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager2);

        FloatingActionButton fab = findViewById(R.id.floatingActionButton);



        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy){
                if (dy > 0)
                    fab.hide();
                else if (dy < 0)
                    fab.show();
            }
        });


        GetFromDb();
    }

    private void GetFromDb() {

        swipeRefreshLayout.setRefreshing(true);

        FirebaseFirestore rootRef = FirebaseFirestore.getInstance();

        DocumentReference docIdRef = rootRef.collection("Devices").document(saved_device.get("device_address"));

        docIdRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {

                        Log.d(TAG, "Document exists!");

                        device_users = (List<String>) document.get("device_users");
                        dev_admin = document.getString("device_admin");
                        dev_name = document.getString("device_name");
                        dev_pin = document.getString("device_pin");
                        dev_admin_pin = document.getString("device_admin_pin");

                        rootRef.collection("Users").whereIn(FieldPath.documentId(), device_users).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {

                                   UsersInfo = new ArrayList<HashMap<String, String>>();
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        Log.d(TAG, document.getId() + " => " + document.getData());
                                        HashMap<String, String> user_info = new HashMap<String, String>();
                                        user_info.put("email",document.get("email").toString());
                                        user_info.put("id",document.get("id").toString());
                                        user_info.put("username",document.get("username").toString());
                                        user_info.put("photo_url",document.get("photo_url").toString());
                                        UsersInfo.add(user_info);



                                    }
                                        int k=0;

                                    UsersInfoSorted = new ArrayList<HashMap<String, String>>();
                                    UsersInfoSorted.addAll(UsersInfo);
                                    for (HashMap<String, String>userinf:UsersInfo){

                                        if(userinf.get("id").equals(dev_admin)){
                                            UsersInfoSorted.remove(userinf);
                                            break;
                                        }
                                        k=k+1;


                                    }
                                    UsersInfoSorted.add(0,UsersInfo.get(k));

                                    MakeRecycleView();
                                } else {
                                    Log.d(TAG, "Error getting documents: ", task.getException());
                                }
                            }
                        });








                    } else {
                        Log.d(TAG, "Document does not exist!");
                        Toast.makeText(getApplicationContext(), "Database Error",
                                Toast.LENGTH_LONG).show();



                    }
                } else {


                    Log.d(TAG, "Failed with: ", task.getException());
                    Toast.makeText(getApplicationContext(), "There is a problem connecting to the database",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

    }


    private void MakeRecycleView() {

        if (device_users!=null&&device_users.size()>0){
            try {
               // MyRecyclerViewAdapter_Predmeti = new MyRecyclerViewAdapter_Predmeti(UsersActivity.this, (UsersActivity.MyRecyclerViewAdapter_Predmeti.MyPredmetListener) this, device_users);
            MyRecyclerViewAdapter_Predmeti = new MyRecyclerViewAdapter_Predmeti(getApplicationContext(),this::onPredmetClicked,UsersInfoSorted,dev_admin);
                recyclerView.setAdapter(MyRecyclerViewAdapter_Predmeti);
                swipeRefreshLayout.setRefreshing(false);

            } catch (Exception e) {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getApplicationContext(), "Error loading with data",
                        Toast.LENGTH_LONG).show();
            }
        }else{
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getApplicationContext(), "There is a problem connecting to the database",
                    Toast.LENGTH_LONG).show();
        }

    }






        private void initToolbar() {
        Toolbar toolbar = (androidx.appcompat.widget.Toolbar) findViewById(R.id.toolbar_users);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(saved_device.get("device_name")+" Users");
    }
    public boolean onOptionsItemSelected(MenuItem item){
        Intent myIntent = new Intent(getApplicationContext(), MainActivity.class);
        startActivityForResult(myIntent, 0);
        finish();
        return true;
    }
    @Override
    public void onBackPressed() {
        Intent myIntent = new Intent(getApplicationContext(), MainActivity.class);
        startActivityForResult(myIntent, 0);
        finish();
    }

    public void AddUser(View view) {
        FirebaseAuth mAurth=FirebaseAuth.getInstance();

        if ( mAurth.getCurrentUser().getUid().equals(dev_admin)){


            String myb = "https://keybotshare.page.link/?link=https://app.switches.com?keybottoconnect%3D"+saved_device.get("device_address")+"&apn=com.keybot&st="+mAurth.getCurrentUser().getDisplayName()+" wants to share a KeyBot"  +"&sd=Open+this+link+and+a+keybot" +
                    "+will+be+added+to+your+devices+list&si=https://www.drupal.org/files/project-images/drupal-addtoany-logo.png";

            Task<ShortDynamicLink> shortLinkTask = FirebaseDynamicLinks.getInstance().createDynamicLink()
                    .setLongLink(Uri.parse(myb))
                    .buildShortDynamicLink()
                    .addOnCompleteListener(this, new OnCompleteListener<ShortDynamicLink>() {
                        @Override
                        public void onComplete(@NonNull Task<ShortDynamicLink> task) {
                            if (task.isSuccessful()) {
                                // Short link created
                                Uri shortLink = task.getResult().getShortLink();
                                Uri flowchartLink = task.getResult().getPreviewLink();

                                String dyn = String.valueOf(shortLink);
                                //Toast.makeText(this,ok, Toast.LENGTH_LONG).show();

                                Intent i = new Intent(Intent.ACTION_SEND);
                                i.setType("text/plain");
                                i.putExtra(Intent.EXTRA_SUBJECT, "Sharing URL");
                                i.putExtra(Intent.EXTRA_TEXT, dyn);
                                startActivity(Intent.createChooser(i, "Share URL"));
                            } else {
                                // Error
                                // ...
                            }
                        }
                    });





        }else{
            Toast.makeText(getApplicationContext(), "You are not permitted to add or remove users",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void onPredmetClicked(int position) {







    }

    public static class MyRecyclerViewAdapter_Predmeti extends RecyclerView.Adapter<MyRecyclerViewAdapter_Predmeti.ViewHolder> {

        private  ArrayList<HashMap<String, String>> mData;
        private LayoutInflater mInflater;
        private MyPredmetListener myPredmetListener;
        private Context context;
        private String dev_admin;


        // data is passed into the constructor
        MyRecyclerViewAdapter_Predmeti(Context context, MyPredmetListener myPredmetListener, ArrayList<HashMap<String, String>> data,String dev_admin) {
            this.mInflater = LayoutInflater.from(context);
            this.mData = data;
            this.myPredmetListener=myPredmetListener;
            this.dev_admin=dev_admin;
            this.context=context;

        }

        // inflates the row layout from xml when needed
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.user_item, parent, false);

            return new ViewHolder(view,myPredmetListener);
        }

        // binds the data to the TextView in each row
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            String name = mData.get(position).get("username");
            String id = mData.get(position).get("id");

            if (id.equals(dev_admin)){

               // name=name+" (Admin)";
                holder.lin.setBackgroundColor(context.getResources().getColor(R.color.admin));
            }


            String email = mData.get(position).get("email");
            String photoUrl = mData.get(position).get("photo_url");

            Glide.with(context)
                    .load(photoUrl)
                    .fitCenter()
                    .circleCrop()
                    .into(holder.userPhoto);


            try {


                holder.name.setText(name);
                holder.email.setText(email);

               // holder.ocene.setText(sb.toString());
                //holder.povprecje.setText("avg: "+povprecje);


            } catch (Exception e) {
                e.printStackTrace();
            }


            Animation fadeIn = new AlphaAnimation(0, 1);
            fadeIn.setInterpolator(new DecelerateInterpolator()); //add this
            fadeIn.setDuration(1000);

          //  holder.cardView.startAnimation(fadeIn);



        }

        // total number of rows
        @Override
        public int getItemCount() {
            return mData.size();
        }


        // stores and recycles views as they are scrolled off screen
        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {


            CardView cardView;
            MyPredmetListener myPredmetListener;
            TextView name;
            TextView email;
            ImageView userPhoto;
            ConstraintLayout lin;




            ViewHolder(View itemView,MyPredmetListener myPredmetListener) {
                super(itemView);
               name = itemView.findViewById(R.id.text_name);
                email=itemView.findViewById(R.id.text_email);
                cardView = itemView.findViewById(R.id.card_predmet);
                userPhoto = itemView.findViewById(R.id.userPhoto);
                lin=itemView.findViewById(R.id.linearLayout);








                this.myPredmetListener=myPredmetListener;


                itemView.setOnClickListener(this);


            }


            @Override
            public void onClick(View v) {
                myPredmetListener.onPredmetClicked(getAdapterPosition());


            }
        }

        private interface MyPredmetListener{
            void onPredmetClicked(int position);


        }


    }


}

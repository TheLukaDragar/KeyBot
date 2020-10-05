package com.keybot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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



public class KeysActivity extends AppCompatActivity {

    private final static String TAG = KeysActivity.class.getSimpleName();
    private SwipeRefreshLayout swipeRefreshLayout;
    private List<String> my_keys;
    private  String user_name;
    private ArrayList<HashMap<String, String>> DeviceInfo;
    private ArrayList<HashMap<String, String>> DeviceInfoSorted;
    private RecyclerView.Adapter MyRecyclerViewAdapter_Predmeti;
    private RecyclerView recyclerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keys);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }
        initToolbar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        recyclerView= findViewById(R.id.keys_recycle);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager2 = new LinearLayoutManager(KeysActivity.this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager2);



        swipeRefreshLayout = findViewById(R.id.swipe_to_ref_keys);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                GetFromDb();
            }
        });

        GetFromDb();
    }

    public void GetFromDb() {

        swipeRefreshLayout.setRefreshing(true);

        FirebaseFirestore rootRef = FirebaseFirestore.getInstance();

        FirebaseUser mAuth = FirebaseAuth.getInstance().getCurrentUser();


        rootRef.collection("Devices").whereArrayContains("device_users", mAuth.getUid()).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {

                    DeviceInfo = new ArrayList<HashMap<String, String>>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Log.d(TAG, document.getId() + " => " + document.getData());
                        HashMap<String, String> device_info = new HashMap<String, String>();
                        device_info.put("device_admin_name", document.getString("device_admin_name"));
                        device_info.put("device_admin", document.getString("device_admin"));
                        device_info.put("device_name", document.get("device_name").toString());
                        device_info.put("is_my_device", "false");
                        device_info.put("device_address",document.getId());

                        if (device_info.get("device_admin")==null){
                            device_info.put("device_admin","No one");
                            device_info.put("device_admin_name","");

                        }


                        DeviceInfo.add(device_info);


                    }
                    int k = 0;

                    if (!DeviceInfo.isEmpty()) {

                        System.out.println(DeviceInfo.toString());


                        DeviceInfoSorted = new ArrayList<HashMap<String, String>>();
                        DeviceInfoSorted.addAll(DeviceInfo);
                        for (HashMap<String, String> devinf : DeviceInfo) {

                            if (devinf.get("device_admin").equals(mAuth.getUid())) {
                                DeviceInfoSorted.remove(devinf);
                                break;
                            }
                            k = k + 1;


                        }
                        if (k < DeviceInfo.size()) {
                            DeviceInfo.get(k).put("is_my_device", "true");
                            DeviceInfoSorted.add(0, DeviceInfo.get(k));//TODO CE NI ADMINA
                        }


                        MakeRecycleView();

                    }else{

                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(getApplicationContext(), "You are not a user of any KeyBot",
                                Toast.LENGTH_LONG).show();





                    }
                } else {
                    swipeRefreshLayout.setRefreshing(false);
                    Log.d(TAG, "Error getting documents: ", task.getException());
                }
            }
        });


    }

    private void MakeRecycleView() {

        if (DeviceInfoSorted!=null&&DeviceInfoSorted.size()>0){
            try {
                // MyRecyclerViewAdapter_Predmeti = new MyRecyclerViewAdapter_Predmeti(UsersActivity.this, (UsersActivity.MyRecyclerViewAdapter_Predmeti.MyPredmetListener) this, device_users);
                MyRecyclerViewAdapter_Predmeti = new KeysActivity.MyRecyclerViewAdapter_Predmeti(this,this::onPredmetClicked,DeviceInfoSorted,user_name);
                recyclerView.setAdapter(MyRecyclerViewAdapter_Predmeti);
                swipeRefreshLayout.setRefreshing(false);

            } catch (Exception e) {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getApplicationContext(), "Error loading with data",
                        Toast.LENGTH_LONG).show();
            }
        }else{
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getApplicationContext(), "You dont have any devices saved",
                    Toast.LENGTH_LONG).show();
        }

    }

    public void onPredmetClicked(int position) {

        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        SharedPreferences.Editor editor =pref.edit();
        editor.putString("device_to_use_address",  DeviceInfoSorted.get(position).get("device_address"));
        editor.commit();

        Toast.makeText(getApplicationContext(), "Now using "+ DeviceInfoSorted.get(position).get("device_name"),
                Toast.LENGTH_LONG).show();








    }






    private void initToolbar() {
        Toolbar toolbar = (androidx.appcompat.widget.Toolbar) findViewById(R.id.toolbar_keys);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle("My KeyBots");
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



    public static class MyRecyclerViewAdapter_Predmeti extends RecyclerView.Adapter<KeysActivity.MyRecyclerViewAdapter_Predmeti.ViewHolder> {

        private  ArrayList<HashMap<String, String>> mData;
        private LayoutInflater mInflater;
        private KeysActivity.MyRecyclerViewAdapter_Predmeti.MyPredmetListener myPredmetListener;
        private Context context;
        private String user_name;




        // data is passed into the constructor
        MyRecyclerViewAdapter_Predmeti(Context context, KeysActivity.MyRecyclerViewAdapter_Predmeti.MyPredmetListener myPredmetListener, ArrayList<HashMap<String, String>> data, String usr_name) {
            this.mInflater = LayoutInflater.from(context);
            this.mData = data;
            this.myPredmetListener=myPredmetListener;
            this.user_name=usr_name;
            this.context=context;


        }

        // inflates the row layout from xml when needed
        @Override
        public KeysActivity.MyRecyclerViewAdapter_Predmeti.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.key_item, parent, false);

            return new KeysActivity.MyRecyclerViewAdapter_Predmeti.ViewHolder(view,myPredmetListener);
        }

        // binds the data to the TextView in each row
        @Override
        public void onBindViewHolder(KeysActivity.MyRecyclerViewAdapter_Predmeti.ViewHolder holder, int position) {

            String name = mData.get(position).get("device_name");
            String usr_name = mData.get(position).get("device_admin_name");
            String is_my_device = mData.get(position).get("is_my_device");

            try {


                holder.device_name.setText(name);
                holder.user_name.setText(usr_name+"'s");

                if (is_my_device.equals("true")){

                    holder.user_name.setText("My");

                }


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
            KeysActivity.MyRecyclerViewAdapter_Predmeti.MyPredmetListener myPredmetListener;
            TextView device_name;
            TextView user_name;
            ConstraintLayout lin;





            ViewHolder(View itemView, KeysActivity.MyRecyclerViewAdapter_Predmeti.MyPredmetListener myPredmetListener) {
                super(itemView);
                device_name = itemView.findViewById(R.id.text_name_device);

                user_name = itemView.findViewById(R.id.text_name_user);

                cardView = itemView.findViewById(R.id.card_predmet_keys);

                lin=itemView.findViewById(R.id.linearLayout_keys);



                this.myPredmetListener=myPredmetListener;

                cardView.setOnClickListener(this);


            }


            @Override
            public void onClick(View v) {
                myPredmetListener.onPredmetClicked(getAdapterPosition());


            }
        }

        private interface MyPredmetListener{
            void onPredmetClicked(int position);


        }

        public HashMap<String, String> getItem(int pos) {
            return mData.get(pos);
        }


    }

}

package com.keybot;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.Layout;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;



public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    Button unlockbutton;
    View constarieddLay;
    TransitionDrawable drawable2;
    TransitionDrawable drawable;
    TextView bigcircle;
    ImageView lockimage;
    TextView deviceName;
    TextView rssiTetView;
    ImageView locationImage;
    BottomNavigationView bottomNavigation;
    EditText editText;
    boolean islocked;
    boolean nodevices=true;


    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics;
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private BluetoothGattCharacteristic Switchchar;
    private BluetoothGattCharacteristic PASSchar;
    public final static UUID UUID_SWITCH =
            UUID.fromString(SampleGattAttributes.SWITCH);
    public final static UUID UUID_PASS =
            UUID.fromString(SampleGattAttributes.PASS);


    private int rssi;
    private int previousRssi;
    private int highestRssi = -128;

    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private int active_device;
    private  HashMap<String,String> saved_device;





    private final ServiceConnection mServiceConnection = new ServiceConnection() {



        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
           // mBluetoothLeService.connect(DEVICE_ADDRESES[DEVICE_TO_CONNECT]);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };




    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(saved_device.get("device_name")+" Connected");
                //updateConnectionState(R.string.connected);
                //invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(saved_device.get("device_name")+" Not Connected");
                notconnectedUI();
                handler.removeCallbacksAndMessages(runnable);
                //updateConnectionState(R.string.disconnected);
                //invalidateOptionsMenu();
               // clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {


                updateConnectionState(saved_device.get("device_name")+" Ready");
                // Show all the supported services and characteristics on the user interface.
               //TODO NO CHARACTERISTICS FOUND ERROR
                loadGattServices(mBluetoothLeService.getSupportedGattServices());

                findPASSChar();
                String a= saved_device.get("device_pin");
                Log.d(TAG, "sending: pin"+a+ " " + Arrays.toString(a.getBytes()));

                PASSchar.setValue(a.getBytes());
                mBluetoothLeService.WriteCharacteristic(PASSchar);


                Log.d(TAG, mGattCharacteristics.toString());



            }
            else if (BluetoothLeService.PASSwritten.equals(action)) {
                updateConnectionState(saved_device.get("device_name")+" Authenticated");

                mBluetoothLeService.setCharacteristicNotification(
                        findSwithChar(), true);

                handler.postDelayed(runnable, 3000);
            }
            else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                updateUI(intent.getStringExtra(BluetoothLeService.SWITCH_DATA));
                updateConnectionState(saved_device.get("device_name")+" Ready");


            }
            else if (BluetoothLeService.DESCRIPTOR_WRITEN.equals(action)) {
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                //Notify enabled reading characteristic to update ui
                mBluetoothLeService.readCharacteristic(findSwithChar());

            }
            else if (BluetoothLeService.ACTION_GATT_RSSI.equals(action)) {

                previousRssi = rssi;
                Log.e(TAG, "Previous RSSI: "+ previousRssi);
                rssi = Integer.valueOf(intent.getStringExtra(BluetoothLeService.RSSI_DATA));

                Log.e(TAG, "Current RSSI: "+ rssi);
                if (highestRssi < rssi)
                    highestRssi = rssi;



                Log.e(TAG, "Higest RSSI: "+ highestRssi);
                Log.e(TAG, "Has rssi changed: "+ hasRssiLevelChanged());


                final int rssiPercent = (int) (100.0f * (127.0f + rssi) / (127.0f + 20.0f));
                //holder.rssi.setImageLevel(rssiPercent);
                rssiTetView.setText(String.valueOf(rssi));
                changelocation();

                //rssiTetView.setText(intent.getStringExtra(BluetoothLeService.RSSI_DATA));


            }

        }
    };
    private Handler handler;
    private Runnable runnable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();


        user = mAuth.getCurrentUser();






        if (user == null) {
            Intent myIntent = new Intent(MainActivity.this, LoginActivity.class);
            MainActivity.this.startActivity(myIntent);
            finish();

        }else {


            SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode

            ArrayList<HashMap<String, String>> offlinedeviceList = getArrayList("devices");

            active_device = pref.getInt("active_device", -1);
            HandleFBDynamicLink();

            if (offlinedeviceList==null&&active_device==-1) {



                Toast.makeText(getApplicationContext(), "No saved devices",
                        Toast.LENGTH_LONG).show();





            } else {

                nodevices=false;
                saved_device = offlinedeviceList.get(active_device);
                Toast.makeText(getApplicationContext(), "Using saved device " + saved_device.get("device_name"),
                        Toast.LENGTH_LONG).show();

                Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
                bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);


            }

            setContentView(R.layout.activity_main);

            bottomNavigation = findViewById(R.id.bottom_navigation);
            bottomNavigation.setOnNavigationItemSelectedListener(navigationItemSelectedListener);
            //openFragment(HomeFragment.newInstance("", ""));




            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_acashmemoreport);
            setSupportActionBar(toolbar);


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
            }

// Show menu icon
            final ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            toolbar.setTitleTextColor(getResources().getColor(R.color.colorWhite));


            lockimage = (ImageView) findViewById(R.id.imageView);


            rssiTetView = findViewById(R.id.TextViewRssi);
            Button rssiButton = findViewById(R.id.button);
            rssiButton.setVisibility(View.GONE);
            //rssiButton.setOnClickListener(new View.OnClickListener() {
            //  @Override
            // public void onClick(View v) {
            //    mBluetoothLeService.readRssi();
            // }
            // });


            locationImage = findViewById(R.id.LocationImage);
            constarieddLay = findViewById(R.id.constrainedLay);

            bigcircle = findViewById(R.id.textViewbigcircle);
            unlockbutton = findViewById(R.id.push_button);

            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(), DeviceScanActivity.class);
                    startActivity(intent);
                    finish();
                }
            });


            deviceName = findViewById(R.id.TextViewDeviceName);


            unlockbutton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (nodevices){

                        final Intent intent = new Intent(getApplicationContext(), DeviceScanActivity.class);
                        startActivity(intent);
                        finish();
                        return;





                    }

                    if (!mConnected) {
                        mBluetoothLeService.connect(saved_device.get("device_address"));

                        return;
                    }

                    if (islocked) {

                        Switchchar.setValue(1, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                        mBluetoothLeService.WriteCharacteristic(Switchchar);
                        // unlockUI();


                    } else {
                        // lockUI();
                        Switchchar.setValue(0, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                        mBluetoothLeService.WriteCharacteristic(Switchchar);

                    }

                }
            });

            if (nodevices){
            nodevicesUI();}else {
                notconnectedUI();
            }
            getRssi();
        }




































    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(saved_device.get("device_address"));
            Log.d(TAG, "Connect request result=" + result);
        }
        handler.postDelayed(runnable,1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        handler.removeCallbacksAndMessages(runnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothLeService!=null) {
            unbindService(mServiceConnection);
        }


        mBluetoothLeService = null;
        if (runnable!=null){
        handler.removeCallbacksAndMessages(runnable);}
    }

    private void updateUI(String stringExtra) {

        if (stringExtra!=null) {
            Log.d(TAG, "BLE SWITCH FEEDBACK: " + stringExtra);
            if (stringExtra.trim().equals("00")) {
                lockUI();
                Log.d(TAG, "lockingUI " + stringExtra);
                islocked=true;


            }
            if (stringExtra.trim().equals("01")) {
                unlockUI();
                Log.d(TAG, "unlockinUI " + stringExtra);
                islocked=false;
            }

        }
    }



    private void unlockUI(){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lockimage.setVisibility(View.VISIBLE);
                constarieddLay.setBackground(getResources().getDrawable(R.drawable.back2));
                bigcircle.setText("UNLOCKED");
                unlockbutton.setText("LOCK");
                bigcircle.setTextColor(getResources().getColor(R.color.backgreen));
                lockimage.setImageDrawable(getResources().getDrawable(R.drawable.unlocked));
                ImageViewCompat.setImageTintList(lockimage, ColorStateList.valueOf(getResources().getColor(R.color.backgreen)));
                //BlinkAndChangeTo(R.color.backgreen);
            }
        });
    }

    private void lockUI(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lockimage.setVisibility(View.VISIBLE);
                constarieddLay.setBackground(getResources().getDrawable(R.drawable.back1));
                bigcircle.setText("LOCKED");
                unlockbutton.setText("UNLOCK");
                bigcircle.setTextColor(getResources().getColor(R.color.backred));
                lockimage.setImageDrawable(getResources().getDrawable(R.drawable.locked));
                ImageViewCompat.setImageTintList(lockimage, ColorStateList.valueOf(getResources().getColor(R.color.backred)));
               // BlinkAndChangeTo(R.color.backred);
            }
        });
    }


    private void notconnectedUI(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceName.setText("Connect to "+ saved_device.get("device_name"));

                constarieddLay.setBackground(getResources().getDrawable(R.drawable.back3));
                lockimage.setImageDrawable(getResources().getDrawable(R.drawable.notconnected));
                ImageViewCompat.setImageTintList(lockimage, ColorStateList.valueOf(getResources().getColor(R.color.backnotconnected)));
                bigcircle.setText("...");
                unlockbutton.setText("CONNECT");
                bigcircle.setTextColor(getResources().getColor(R.color.backnotconnected));
               // BlinkAndChangeTo(R.color.backnotconnected);

            }
        });

    }
    private void nodevicesUI(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceName.setText("No devices saved");
                constarieddLay.setBackground(getResources().getDrawable(R.drawable.back4));
                lockimage.setVisibility(View.GONE);
                bigcircle.setText("");
                unlockbutton.setText("ADD DEVICE");
                bigcircle.setTextColor(getResources().getColor(R.color.backnotconnected));
                // BlinkAndChangeTo(R.color.backnotconnected);

            }
        });

    }
    private void loadingUI(){

    }


    private void updateConnectionState(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceName.setText(status);
            }
        });

    }


    private void loadGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

    }
    private BluetoothGattCharacteristic findSwithChar(){
         Switchchar = null;

        loop:
        for (int i=0;i<mGattCharacteristics.size();i++){

            for (int j=0;j<mGattCharacteristics.get(i).size();j++){

                Switchchar=mGattCharacteristics.get(i).get(j);
                Log.d(TAG, "LOOKING FRO CHARACTERISTIC SWITCH");



                if (Switchchar.getUuid().equals(UUID_SWITCH)&&(Switchchar.getProperties()| BluetoothGattCharacteristic.PROPERTY_WRITE)>0){
                    Log.d(TAG, "FOUND CHARACTERISTIC");
                    break loop;
                }
            }
        }
        return Switchchar;
    }
    private BluetoothGattCharacteristic findPASSChar(){
        PASSchar = null;

        loop:
        for (int i=0;i<mGattCharacteristics.size();i++){

            for (int j=0;j<mGattCharacteristics.get(i).size();j++){

                PASSchar=mGattCharacteristics.get(i).get(j);
                //Log.d(TAG, "LOOKING FRO CHARACTERISTIC SWITCH");



                if (PASSchar.getUuid().equals(UUID_PASS)&&(PASSchar.getProperties()| BluetoothGattCharacteristic.PROPERTY_WRITE)>0){
                   // Log.d(TAG, "FOUND CHARACTERISTIC");
                    break loop;
                }
            }
        }
        return PASSchar;
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.DESCRIPTOR_WRITEN);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_RSSI);
        intentFilter.addAction(BluetoothLeService.PASSwritten);
        return intentFilter;
    }

    private void BlinkAndChangeTo(int color){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Window window = getWindow();
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

                    window.setStatusBarColor(color);



                }

            }
        });

    }





   private boolean hasRssiLevelChanged() {
        final int newLevel =
                -rssi <= 10 ?
                        0 :
                        -rssi <= 28 ?
                                1 :
                                -rssi <= 45 ?
                                        2 :
                                        -rssi <= 65 ?
                                                3 :
                                                4;
        final int oldLevel =
                -previousRssi <= 10 ?
                        0 :
                        -previousRssi <= 28 ?
                                1 :
                                -previousRssi <= 45 ?
                                        2 :
                                        -previousRssi <= 65 ?
                                                3 :
                                                4;
        return newLevel != oldLevel;
    }

    private void changelocation(){

        if (Math.abs(previousRssi-rssi)<5){
            return;
        }

        //final int distance =
                //-rssi <= 10 ?
                      //  0 :
                       // -rssi <= 28 ?
                        //        8 :
                         //       -rssi <= 45 ?
                           //             12 :
                             //           -rssi <= 65 ?
                              //                  16 :
                               //                 30;

        //int distance = (-rssi * 2/3) -20;
        int distance = (int) ((-rssi * 0.8) -40);




        int marginInDp = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, distance, getResources()
                        .getDisplayMetrics());

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) locationImage.getLayoutParams();
        params.topMargin = marginInDp;

    }

    private void getRssi(){
        handler = new Handler();
        runnable = new Runnable() {
            public void run() {
               Log.e(TAG,"Runningg");
               if (mConnected && mBluetoothLeService!=null){
                   mBluetoothLeService.readRssi();
                   handler.postDelayed(runnable, 3000);

               }



            }
        };


    }

    public void openFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    BottomNavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.navigation_keys:

                          ;


                            return true;
                        case R.id.navigation_lock:
                           // openFragment(SmsFragment.newInstance("", ""));
                            return true;
                        case R.id.navigation_users:

                            if (nodevices){
                                Toast.makeText(getApplicationContext(), "No saved devices to view its users",
                                        Toast.LENGTH_LONG).show();


                            }else{
                            Intent myIntent = new Intent(MainActivity.this, UsersActivity.class);
                            myIntent.putExtra("saved_device",saved_device);
                            myIntent.putExtra("active_device",active_device);
                            MainActivity.this.startActivity(myIntent);
                            finish();
                            }
                            //overridePendingTransition(R.anim.up_anim,R.anim.up_anim_don);


                            //openFragment(NotificationFragment.newInstance("", ""));
                            return true;
                    }
                    return false;
                }
            };


    public ArrayList<HashMap<String, String>> getArrayList(String key){
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        Gson gson = new Gson();

        String json = prefs.getString(key, null);
        Type type = new TypeToken<ArrayList<HashMap<String, String>>>() {}.getType();
        return gson.fromJson(json, type);
    }

    private void HandleFBDynamicLink(){
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnSuccessListener(this, new OnSuccessListener<PendingDynamicLinkData>() {
                    @Override
                    public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData) {
                        // Get deep link from result (may be null if no link is found)
                        Uri deepLink = null;
                        if (pendingDynamicLinkData != null) {
                            deepLink = pendingDynamicLinkData.getLink();
                        }
                        final Uri finalDeepLink = deepLink;
                        // if (finalDeepLink==null){
                        //   Toast.makeText(getApplicationContext() , "nilz", Toast.LENGTH_SHORT).show();

                        //  }

                        // If the user isn't signed in and the pending Dynamic Link is
                        // an invitation, sign in the user anonymously, and record the
                        // referrer's UID.
                        //
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                        if(user==null){
                            // Toast.makeText(getApplicationContext() , "you need to log in try again later", Toast.LENGTH_SHORT).show();

                        }
                        if (finalDeepLink != null&&user!=null
                                && finalDeepLink.getBooleanQueryParameter("keybottoconnect", false)) {
                            String keybottoconnect = finalDeepLink.getQueryParameter("keybottoconnect");

                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            mAuth = FirebaseAuth.getInstance();
                            FirebaseUser userin = mAuth.getCurrentUser();

                            Map<String, Object> user1 = new HashMap<>();

                            user1.put("device_users", Arrays.asList(mAuth.getCurrentUser().getUid()));


                           DocumentReference documentReference= db.collection("Devices").document(keybottoconnect);
                            documentReference.update("device_users", FieldValue.arrayUnion(mAuth.getCurrentUser().getUid())).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {

                                    Toast.makeText(getApplicationContext() , "You are added", Toast.LENGTH_SHORT).show();
                                }
                            });
                           // Snackbar snackbar = Snackbar.make(findViewById(R.id.main), "Switch added", 8000);
                            //snackbar.setAction("See it here", new MyUndoListener());
                            //snackbar.show();




                        }
                    }
                });




    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                mAuth.signOut();
                GoogleSignIn.getClient(
                       getApplicationContext(),
                        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                ).signOut();

                Intent myIntent = new Intent(MainActivity.this, LoginActivity.class);
                MainActivity.this.startActivity(myIntent);
                finish();


                break;

        }
        return true;
    }
}



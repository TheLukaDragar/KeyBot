package com.keybot;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.TransitionDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private ProgressBar progressBar;





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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            String channelId  = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW));
        }



        if (user == null) {
            Intent myIntent = new Intent(MainActivity.this, LoginActivity.class);
            MainActivity.this.startActivity(myIntent);
            finish();

        }else {

            Intent i = getIntent();
            Bundle extras = i.getExtras();
            if (extras != null) {
                for (String key : extras.keySet()) {
                    Object value = extras.get(key);
                    Log.d("MainActivty", "Extras received at onCreate:  Key: " + key + " Value: " + value);
                }
                String title = extras.getString("titl");
                String message = extras.getString("bodyy");
                String senderUID = extras.getString("senderUid");
                if ((message!=null && message.length()>0)&&senderUID!=null && senderUID.length()>0) {
                    getIntent().removeExtra("bodyy");
                    showNotificationInADialog(title, message,senderUID);
                }
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


            progressBar=findViewById(R.id.progressBar);






            lockimage = (ImageView) findViewById(R.id.imageView);


            rssiTetView = findViewById(R.id.TextViewRssi);
            Button rssiButton = findViewById(R.id.button);
            rssiButton.setVisibility(View.GONE);



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


            if (isNetworkConnected()) {

                LoadingFromDBUI();
                HandleFBDynamicLink();

                LoadKeysFromDB();

            }else {

                noNetworkUI();
            }






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

            getRssi();


        }


    } @Override
    public void onNewIntent(Intent intent){
        //called when a new intent for this class is created.
        // The main case is when the app was in background, a notification arrives to the tray, and the user touches the notification

        super.onNewIntent(intent);

        if (user!=null) {

            Log.d(TAG, "onNewIntent - starting");
            Bundle extras = intent.getExtras();
            if (extras != null) {
                for (String key : extras.keySet()) {
                    Object value = extras.get(key);
                    Log.d(TAG, "Extras received at onNewIntent:  Key: " + key + " Value: " + value);
                }
                String title = extras.getString("titl");
                String message = extras.getString("bodyy");
                String senderUID = extras.getString("senderUid");
                if ((message != null && message.length() > 0) && senderUID != null && senderUID.length() > 0) {
                    getIntent().removeExtra("bodyy");
                    showNotificationInADialog(title, message, senderUID);
                }
            }
        }
    }



    private void LoadKeysFromDB() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        ArrayList<HashMap<String, String>> offlinedeviceList = new ArrayList<HashMap<String, String>>();


        //offlinedeviceList = getArrayList("devices");

        //active_device = pref.getInt("active_device", -1);




        FirebaseFirestore db = FirebaseFirestore.getInstance();

        CollectionReference citiesRef = db.collection("Devices");

        citiesRef.whereArrayContains("device_users", mAuth.getCurrentUser().getUid()).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {

                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Log.d(TAG, "User is in user list of this device");
                        Log.d(TAG, document.getId() + " => " + document.getData());

                        List<String> device_users = (List<String>) document.get("device_users");
                        String dev_admin = document.getString("device_admin");
                        String dev_admin_name = document.getString("device_admin_name");
                        String dev_name = document.getString("device_name");
                        String dev_pin = document.getString("device_pin");
                        String dev_admin_pin = document.getString("device_admin_pin");

                        HashMap<String, String> offlinedevice = new HashMap<String, String>();
                        offlinedevice.put("device_admin", dev_admin);
                        offlinedevice.put("device_admin_name", dev_admin_name);
                        offlinedevice.put("device_address", document.getId());
                        offlinedevice.put("device_name", dev_name);
                        offlinedevice.put("device_users", String.join(",", device_users));
                        offlinedevice.put("device_pin", dev_pin);//TODO NOT WRITING THIS IN
                        offlinedevice.put("device_admin_pin", dev_admin_pin);


                        offlinedeviceList.add(offlinedevice);
                    }
                    Log.d(TAG, "test" + offlinedeviceList.toString());


                    String device_to_use_address = pref.getString("device_to_use_address", null);
                    System.out.println(device_to_use_address);

                    if (offlinedeviceList.isEmpty()) {


                        Toast.makeText(getApplicationContext(), "No saved devices",
                                Toast.LENGTH_LONG).show();


                    } else {

                        nodevices = false;

                        if (device_to_use_address == null) {
                            device_to_use_address = offlinedeviceList.get(0).get("device_address");
                            SharedPreferences.Editor editor = pref.edit();
                            editor.putString("device_to_use_address", device_to_use_address);
                            editor.commit();


                        } else {

                            active_device = getOfflineDeviceWithAdress(offlinedeviceList, device_to_use_address);

                            if (active_device == -1) {
                                Log.d(TAG, "Recent device was not found or was removed using index 0 ");
                                active_device = 0;
                                SharedPreferences.Editor editor = pref.edit();
                                editor.putString("device_to_use_address", offlinedeviceList.get(0).get("device_address"));
                                editor.commit();

                            }
                        }


                        saved_device = offlinedeviceList.get(active_device);

                       // Snackbar snackbar= Snackbar.make(findViewById(R.id.swipe_to_ref_main), "Using saved device " + saved_device.get("device_name"), Snackbar.LENGTH_LONG);
                       // snackbar.setAnchorView(bottomNavigation);
                       // snackbar.show();
                        Toast.makeText(getApplicationContext(), "Using saved device " + saved_device.get("device_name"),
                                Toast.LENGTH_LONG).show();

                        Intent gattServiceIntent = new Intent(MainActivity.this, BluetoothLeService.class);
                        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);


                    }


                    progressBar.setIndeterminate(false);
                    progressBar.setVisibility(View.GONE);

                    if (nodevices) {
                        nodevicesUI();
                    } else {
                        notconnectedUI();
                    }


                } else {
                    Log.d(TAG, "Error getting documents: ", task.getException());
                }

            }
        });
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
        locationImage.setVisibility(View.VISIBLE);

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
                unlockbutton.setVisibility(View.VISIBLE);
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
                unlockbutton.setVisibility(View.VISIBLE);
                deviceName.setText("Connect to "+ saved_device.get("device_name"));

                constarieddLay.setBackground(getResources().getDrawable(R.drawable.back3));
                lockimage.setImageDrawable(getResources().getDrawable(R.drawable.notconnected));
                ImageViewCompat.setImageTintList(lockimage, ColorStateList.valueOf(getResources().getColor(R.color.backnotconnected)));
                bigcircle.setText("...");
                unlockbutton.setText("CONNECT");
                locationImage.setVisibility(View.GONE);
                bigcircle.setTextColor(getResources().getColor(R.color.backnotconnected));
               // BlinkAndChangeTo(R.color.backnotconnected);

            }
        });

    }

    private void noNetworkUI(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceName.setText("No network available");

                constarieddLay.setBackground(getResources().getDrawable(R.drawable.back3));
                lockimage.setImageDrawable(getResources().getDrawable(R.drawable.nonetwork));
                ImageViewCompat.setImageTintList(lockimage, ColorStateList.valueOf(getResources().getColor(R.color.backnotconnected)));
                bigcircle.setText("");
                unlockbutton.setVisibility(View.GONE);
                locationImage.setVisibility(View.INVISIBLE);
                bigcircle.setTextColor(getResources().getColor(R.color.backnotconnected));
                // BlinkAndChangeTo(R.color.backnotconnected);

            }
        });

    }

    private void LoadingFromDBUI(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setIndeterminate(true);
                deviceName.setText("Synchronizing...");

                constarieddLay.setBackground(getResources().getDrawable(R.drawable.back3));
                lockimage.setImageDrawable(getResources().getDrawable(R.drawable.loadingnetwork));
                ImageViewCompat.setImageTintList(lockimage, ColorStateList.valueOf(getResources().getColor(R.color.backnotconnected)));
                bigcircle.setText("...");
                unlockbutton.setVisibility(View.INVISIBLE);
                locationImage.setVisibility(View.GONE);
                bigcircle.setTextColor(getResources().getColor(R.color.backnotconnected));
                // BlinkAndChangeTo(R.color.backnotconnected);

            }
        });

    }
    private void nodevicesUI(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                unlockbutton.setVisibility(View.VISIBLE);
                deviceName.setText("No devices saved");
                constarieddLay.setBackground(getResources().getDrawable(R.drawable.back4));
                lockimage.setVisibility(View.GONE);
                locationImage.setVisibility(View.GONE);
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

                            Intent myIntent0 = new Intent(MainActivity.this, KeysActivity.class);
                            MainActivity.this.startActivity(myIntent0);
                            finish();


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

                        case R.id.navigation_settings:

                            FirebaseFirestore db = FirebaseFirestore.getInstance();




                            long timestamp = new Date().getTime();
                            long dayTimestamp = getDayTimestamp(timestamp);
                            String body = "Can I use Your Keybot";
                            String ownerUid = mAuth.getCurrentUser().getUid();
                            String userUid =  mAuth.getCurrentUser().getUid();
                            Message message =
                                    new Message(timestamp, -timestamp, dayTimestamp, body, ownerUid, userUid);


                            db.collection("notifications").add(message).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(DocumentReference documentReference) {



                                }
                            });

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
                FCMHandler fmchandle= new FCMHandler();
                fmchandle.disableFCM();


                FirebaseAuth.getInstance().signOut();


                GoogleSignIn.getClient(
                       getApplicationContext(),
                        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                ).signOut();


                SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
                pref.edit().clear().commit();



                Intent myIntent = new Intent(MainActivity.this, LoginActivity.class);
                MainActivity.this.startActivity(myIntent);
                finish();


                break;

        }
        return true;
    }

    public int getOfflineDeviceWithAdress(ArrayList<HashMap<String, String>> offlinedeviceList, String address) {

        int i;

        int ret=-1;


        for (i=0;i<offlinedeviceList.size();i++){

            if (offlinedeviceList.get(i).get("device_address").equals(address)){
                Log.d(TAG,"DEVICE FOUND IN LIST AT INDEX "+ i);
                ret=i;
                break;
            }
        }
        return ret;
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    public static class Message {

        private long timestamp;
        private long negatedTimestamp;
        private long dayTimestamp;
        private String body;
        private String from;
        private String to;

        public Message(long timestamp, long negatedTimestamp, long dayTimestamp, String body, String from, String to) {
            this.timestamp = timestamp;
            this.negatedTimestamp = negatedTimestamp;
            this.dayTimestamp = dayTimestamp;
            this.body = body;
            this.from = from;
            this.to = to;
        }

        public Message() {
        }

        public long getTimestamp() {
            return timestamp;
        }

        public long getNegatedTimestamp() {
            return negatedTimestamp;
        }

        public String getTo() {
            return to;
        }

        public long getDayTimestamp() {
            return dayTimestamp;
        }

        public String getFrom() {
            return from;
        }

        public String getBody() {
            return body;
        }
    }

    public static long getDayTimestamp(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        return calendar.getTimeInMillis();
    }




    private void showNotificationInADialog(String title, String message,String usr_uuid) {

        Log.d(TAG,"BIGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG");

        // show a dialog with the provided title and message
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.myDialog));
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                addUserToUserList(usr_uuid);
                dialog.cancel();

            }
        });
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.cancel();

            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void addUserToUserList(String usr_uuid) {


        FirebaseFirestore db = FirebaseFirestore.getInstance();


        DocumentReference documentReference= db.collection("Devices").document(Objects.requireNonNull(saved_device.get("device_address")));
        documentReference.update("device_users", FieldValue.arrayUnion(usr_uuid)).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {


                Toast.makeText(getApplicationContext() , "User is now authorized", Toast.LENGTH_SHORT).show();
            }
        });
    }
}



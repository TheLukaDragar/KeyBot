package com.keybot;

/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

import static android.content.ContentValues.TAG;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks{
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private  ListView listView;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 20000;

    private static final String[] LOCATION_AND_CONTACTS =
            {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    private static final int RC_LOCATION_CONTACTS_PERM = 124;
    private static final UUID MY_UUID = UUID.fromString("00001815-0000-1000-8000-00805F9B34FB");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.scanactivity);

       // androidx.appcompat.widget.Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarcharacteristic);
        //setSupportActionBar(toolbar);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }




        listView=findViewById(R.id.listView);
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        listView.setAdapter(mLeDeviceListAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {



                final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
                if (device == null) return;



                ArrayList<HashMap<String, String>> offlinedeviceList = getArrayList("devices");


               FirebaseAuth mAuth = FirebaseAuth.getInstance();
               FirebaseUser mAuthCurrentuser = mAuth.getCurrentUser();

               boolean isuserdeviceadmin = false;

               if (offlinedeviceList!=null){
               for (HashMap<String, String> offlinedevice:offlinedeviceList){

                   if (offlinedevice.get("device_admin").equals(mAuthCurrentuser.getUid())&&device.getAddress().equals(offlinedevice.get("device_address"))){
                       Log.d(TAG, "User is admin");
                       isuserdeviceadmin =true;

                   }


               }}



                if (isNetworkConnected()) {
                    FirebaseFirestore rootRef = FirebaseFirestore.getInstance();
                    DocumentReference docIdRef = rootRef.collection("Devices").document(device.getAddress());
                    boolean finalIsuserdeviceadmin = isuserdeviceadmin;
                    docIdRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    Log.d(TAG, "Document exists!");
                                    List<String> device_users = (List<String>) document.get("device_users");
                                    String dev_admin = document.getString("device_admin");
                                    String dev_admin_name = document.getString("device_admin_name");
                                    String dev_name = document.getString("device_name");
                                    String dev_pin = document.getString("device_pin");
                                    String dev_admin_pin = document.getString("device_admin_pin");



                                    if (device_users != null && dev_admin!=null && dev_admin_name!=null  && dev_name!=null &&dev_pin!=null && dev_admin_pin!=null) {

                                        if (dev_admin.isEmpty() || dev_admin_name.isEmpty()){

                                            Log.d(TAG, "Device has no admin ");
                                            saveandconnect(device,true,dev_name,dev_admin,device_users,dev_pin,dev_admin_pin,dev_admin_name,true);
                                        }
                                        else if (isuserinlist(device_users,mAuthCurrentuser.getUid())) {



                                            if (finalIsuserdeviceadmin && dev_admin.equals(mAuthCurrentuser.getUid())){
                                                Log.d(TAG, "User admin and in userlist");
                                                saveandconnect(device,true,dev_name,dev_admin,device_users,dev_pin,dev_admin_pin, dev_admin_name, false);
                                                Toast.makeText(getApplicationContext(), "You are admin",
                                                        Toast.LENGTH_LONG).show();




                                            }
                                            else if (finalIsuserdeviceadmin && !dev_admin.equals(mAuthCurrentuser.getUid())){

                                                Log.d(TAG, "User admin offline but not online,and in userlist");

                                                saveandconnect(device,false,dev_name,dev_admin,device_users,dev_pin,dev_admin_pin, dev_admin_name, false);
                                                Toast.makeText(getApplicationContext(), "You now a user",
                                                        Toast.LENGTH_LONG).show();




                                            }

                                            else{

                                                Log.d(TAG, "User in userlist");

                                                saveandconnect(device,false,dev_name,dev_admin,device_users,dev_pin,dev_admin_pin, dev_admin_name, false);
                                                Toast.makeText(getApplicationContext(), "You are a user",
                                                        Toast.LENGTH_LONG).show();



                                            }


                                            }

                                                else if (dev_admin.equals(mAuthCurrentuser.getUid())){
                                                    Log.d(TAG, "User admin but not in userlist");

                                                saveandconnect(device,true,dev_name,dev_admin,device_users,dev_pin,dev_admin_pin, dev_admin_name, true);

                                                    Toast.makeText(getApplicationContext(), "You are admin",
                                                            Toast.LENGTH_LONG).show();

                                                }
                                                else {

                                                Toast.makeText(getApplicationContext(), "You are not authorized to use this KeyBot",
                                                        Toast.LENGTH_LONG).show();
                                                AskForPermission(dev_admin);

                                            }

                                    }else {
                                        Toast.makeText(getApplicationContext(), "KeyBot exist in database but its not setup correctly",
                                                Toast.LENGTH_LONG).show();



                                    }





                                } else {
                                    Log.d(TAG, "Document does not exist!");
                                    Toast.makeText(getApplicationContext(), "This device is not registered in the database",
                                            Toast.LENGTH_LONG).show();



                                }
                            } else {


                                Log.d(TAG, "Failed with: ", task.getException());
                                Toast.makeText(getApplicationContext(), "There is a problem connecting to the database",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }else{

                    Toast.makeText(getApplicationContext(), "Please connect to the internet to access devices",
                            Toast.LENGTH_LONG).show();


                    if (isuserdeviceadmin){
                        Log.d(TAG, "User admin offline");
                        //




                    }else{

                        Log.d(TAG, "User offline ");
                        //enter pin




                    }

                    //no network





                }



               // final Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                // intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
                //intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                if (mScanning) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mScanning = false;
                }
                SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode

                //isDeviceSaved(device.getName(),device.getAddress(),pref);
               // startActivity(intent);

            }
        });


        //getActionBar().setTitle(R.string.title_devices);
        mHandler = new Handler();

        locationAndContactsTask();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        //check perrmisions
    }

    private void AskForPermission(String device_admin_id) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        long timestamp = new Date().getTime();
        long dayTimestamp = MainActivity.getDayTimestamp(timestamp);
        String body = "Can I use Your Keybot";
        String ownerUid = mAuth.getCurrentUser().getUid();
        String userUid =  device_admin_id;
        MainActivity.Message message =
                new MainActivity.Message(timestamp, -timestamp, dayTimestamp, body, ownerUid, userUid);


        db.collection("notifications").add(message).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {



            }
        });
    }

    private void saveandconnect(BluetoothDevice device, boolean isadmin, String dev_name, String dev_admin, List<String> device_users, String dev_pin, String dev_admin_pin, String dev_admin_name, boolean newdevice) {



        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser mAuthCurrentuser = mAuth.getCurrentUser();


        ArrayList<HashMap<String, String>> offlinedeviceList = getArrayList("devices");
        if (offlinedeviceList==null){offlinedeviceList = new   ArrayList<HashMap<String, String>>(); }

        HashMap<String, String> offlinedevice = new HashMap<String, String>();


        if (newdevice){

            device_users.clear();

        }
        if (!device_users.contains(mAuthCurrentuser.getUid())){

            device_users.add(mAuthCurrentuser.getUid());

        }

        offlinedevice.put("device_admin",dev_admin);
        offlinedevice.put("device_admin_name",dev_admin_name);
        if (isadmin){

            offlinedevice.put("device_admin",mAuthCurrentuser.getUid());
            offlinedevice.put("device_admin_name",mAuthCurrentuser.getDisplayName());




        }



        offlinedevice.put("device_address",device.getAddress());
        offlinedevice.put("device_name",dev_name);
        offlinedevice.put("device_users",String.join(",",device_users));
        offlinedevice.put("device_pin",dev_pin);//TODO NOT WRITING THIS IN
        offlinedevice.put("device_admin_pin",dev_admin_pin);


        int active_device=0;

        if (offlinedeviceList.size()==0){

            offlinedeviceList.add(offlinedevice);
            Log.d(TAG, "list epmpty this is first device ");
        }else {



            int index =  getOfflineDeviceWithAdress(offlinedeviceList,device.getAddress());

            if (index==-1){

                offlinedeviceList.add(offlinedevice);
                Log.d(TAG, "INDEX-1 ");



            }else{

                offlinedeviceList.set(index,offlinedevice);

                active_device=index;
                Log.d(TAG, "INDEX "+index);








            }

        }

        saveArrayList(offlinedeviceList,"devices",active_device);
        Log.d(TAG,offlinedeviceList.toString());


        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Create a new user with a first and last name
        Map<String, Object> user1 = new HashMap<>();
        user1.put("device_admin", offlinedevice.get("device_admin"));
        user1.put("device_admin_name", offlinedevice.get("device_admin_name"));
        user1.put("device_admin_pin", offlinedevice.get("device_admin_pin"));
        user1.put("device_name", offlinedevice.get("device_name"));
        user1.put("device_pin", offlinedevice.get("device_pin"));
        List<String> device_usershelp = Arrays.asList(offlinedevice.get("device_users").split(","));
        Log.d(TAG, device_usershelp.toString());
        user1.put("device_users", device_usershelp);


        db.collection("Devices").document(device.getAddress())
                .set(user1, SetOptions.merge()).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

                SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
                SharedPreferences.Editor editor =pref.edit();
                editor.putString("device_to_use_address",  device.getAddress());
                editor.commit();

                Log.d(TAG, "DocumentSnapshot successfully written!");
                Intent myIntent = new Intent(DeviceScanActivity.this, MainActivity.class);
                DeviceScanActivity.this.startActivity(myIntent);
                finish();




            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {


                        Snackbar.make(findViewById(R.id.loginconstr), "Database write error", Snackbar.LENGTH_LONG).show();
                        Log.w(TAG, "Error writing document", e);
                    }
                });

    }




    private boolean hasLocationAndContactsPermissions() {
        return EasyPermissions.hasPermissions(this, LOCATION_AND_CONTACTS);
    }
    @AfterPermissionGranted(RC_LOCATION_CONTACTS_PERM)
    public void locationAndContactsTask() {
        if (hasLocationAndContactsPermissions()) {
            // Have permissions, do the thing!
            Log.d(TAG, "Permisiions granted");
        } else {
            // Ask for both permissions
            EasyPermissions.requestPermissions(
                    this,
                   "Location permissions are required for Bluetooth operations.",
                    RC_LOCATION_CONTACTS_PERM,
                    LOCATION_AND_CONTACTS);
            //TODO LOAKCIJA MORE BIT ON DA BLE DELA
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());

        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_interminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }
    @Override
    public void onBackPressed() {
        Intent myIntent = new Intent(getApplicationContext(), MainActivity.class);
        startActivityForResult(myIntent, 0);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        listView.setAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }


    public static boolean isIn(String[] arr, String targetValue) {
        return Arrays.asList(arr).contains(targetValue);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            //new UUID[]{MY_UUID}
            //TODO
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            System.out.println("Started Scan");
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.deviceStatus= (TextView) view.findViewById(R.id.device_state);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            final String deviceBOND = String.valueOf(device.getBondState());
            if (deviceName != null && deviceName.length() > 0) {
                viewHolder.deviceName.setText(deviceName);
                viewHolder.deviceStatus.setText(deviceBOND);
            }
            else {
                viewHolder.deviceName.setText(R.string.unknown_device);
            }
            viewHolder.deviceAddress.setText(device.getAddress());
            //TODO RECOGNIZING SHARED DEVICES


            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("Device found: "+device.getAddress());
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceStatus;
        ToggleButton toggleButton;
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    public void saveArrayList(ArrayList<HashMap<String, String>> list, String key, int active_device){
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString(key, json);
        editor.putInt("active_device", active_device);
        editor.commit();     // This line is IMPORTANT !!!
    }

    public ArrayList<HashMap<String, String>> getArrayList(String key){
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        Gson gson = new Gson();

        String json = prefs.getString(key, null);
        Type type = new TypeToken<ArrayList<HashMap<String, String>>>() {}.getType();
        return gson.fromJson(json, type);
    }
    private boolean isuserinlist(List<String> device_users, String uid) {

        for (String user:device_users){
            if (user.equals(uid)) {
                return true;


            }


        }
        return false;




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

}
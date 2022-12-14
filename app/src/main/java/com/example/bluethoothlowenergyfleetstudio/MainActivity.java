package com.example.bluethoothlowenergyfleetstudio;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluethoothlowenergyfleetstudio.Adapters.DeviceItemAdapter;
import com.example.bluethoothlowenergyfleetstudio.Utils.Constants;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements DeviceItemAdapter.OnItemClicked {

    // ui components
    TextView turn_on_description, paired_device_info_text;
    RecyclerView paired_device_recycler_view, available_device_recycler_view;
    Switch switch_button;
    RelativeLayout available_device_container;
    DeviceItemAdapter paired_device_adapter, available_device_adapter;


    BluetoothAdapter bluetoothAdapter;
    Intent btIntent;
    int bluetoothRequestCode;

    Set<BluetoothDevice> pairedDevices, availableDevices = new HashSet<>();
    ArrayList<BluetoothDevice> pairedDevicesArrayList, availableDevicesArrayList;

    ArrayList<Map<String, Object>> deviceRawData = new ArrayList<>();
    ArrayList<JSONObject> deviceRawDataInJson = new ArrayList<>();

    private static final String TAG = "MainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        turn_on_description = findViewById(R.id.turn_on_description);
        paired_device_info_text = findViewById(R.id.paired_device_info_text);
        paired_device_recycler_view = findViewById(R.id.paired_device_recycler_view);
        available_device_recycler_view = findViewById(R.id.available_device_recycler_view);
        switch_button = findViewById(R.id.switch_button);
        available_device_container = findViewById(R.id.available_device_container);


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        btIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

        checkBluetooth();
        switch_button.setOnClickListener(view -> {
            toggleBluetooth();
        });


    }

    private void toggleBluetooth() {
        // check is reversed because this callback is after tapping on switch
        if (!switch_button.isChecked()) {
            // turn off the bluetooth
            Log.d(TAG, "toggleBluetooth: turning bluetooth off");
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter.disable();
                turn_on_description.setText("Turn on bluetooth to see nearby Bluetooth devices");
                available_device_container.setVisibility(View.GONE);
                paired_device_recycler_view.setVisibility(View.GONE);
                paired_device_info_text.setVisibility(View.VISIBLE);
            }
        } else {
            // turn on the bluetooth
            Log.d(TAG, "toggleBluetooth: turning bluetooth on");
            turnOnBluetooth();

        }
    }

    private void turnOnBluetooth() {
        startActivityForResult(btIntent, bluetoothRequestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == bluetoothRequestCode) {
            if (resultCode == RESULT_OK) {
                onBluetoothEnabled();
            } else {
                onBluetoothDisabled();
            }
        }
    }

    private void onBluetoothEnabled() {
        switch_button.setChecked(true);
        turn_on_description.setText("Currently visible to nearby Bluetooth devices");

        Toast.makeText(this, "Bluetooth is enabled.", Toast.LENGTH_SHORT).show();

        paired_device_info_text.setVisibility(View.GONE);
        available_device_container.setVisibility(View.VISIBLE);
        paired_device_recycler_view.setVisibility(View.VISIBLE);

        findAvailableDevices();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            pairedDevices = bluetoothAdapter.getBondedDevices();
            pairedDevicesArrayList = new ArrayList<>();

            if (pairedDevices.size() > 0) {
                for (BluetoothDevice devices :
                        pairedDevices) {
                    if (devices.getName() != null) {
                        pairedDevicesArrayList.add(devices);
                    }

                }
                paired_device_adapter = new DeviceItemAdapter(pairedDevicesArrayList, this, true, MainActivity.this);
                paired_device_recycler_view.setAdapter(paired_device_adapter);
                paired_device_recycler_view.setLayoutManager(new LinearLayoutManager(this));
                for (BluetoothDevice device : pairedDevicesArrayList) {
                    addToRawData(device, true);
                }
            }
        }


    }


    private void addToRawData(BluetoothDevice devices, boolean paired) {
        Map<String, Object> map = new HashMap<>();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            map.put(Constants.DEVICE_NAME, devices.getName());
            map.put(Constants.DEVICE_ADDRESS, devices.getAddress());
            map.put(Constants.DEVICE_TYPE, devices.getBluetoothClass().getDeviceClass());
            map.put(Constants.IS_DEVICE_PAIRED, paired);
        }
        deviceRawData.add(map);
        JSONObject obj = new JSONObject(map);
        deviceRawDataInJson.add(obj);
        if (deviceRawData.size() >= pairedDevicesArrayList.size()) {
            uploadData();
        }
    }

    private void uploadData() {

        Log.d(TAG, "uploadData: " + deviceRawData);
        Log.d(TAG, "uploadData: " + deviceRawDataInJson);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map map = new HashMap<>();
        map.put(Constants.BLUETOOTH_DEVICE,deviceRawData);

        db.collection(Constants.BLUETOOTH_DEVICE).document(Constants.BLUETOOTH_DEVICE)
                .set(map)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "onSuccess: updated data to server");
                    sendNotification("Success","Bluetooth device data has been updated to server");
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "onFailure: " + e.getMessage());
                    sendNotification("Failed","Error occurred during upload.");
                });

    }

    private void sendNotification(String title, String body) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,"2" )
                .setSmallIcon(R.drawable.ic_baseline_bluetooth_24)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name ="bt";
            String description = "bt_notification";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("2", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(2, builder.build());
    }

    private void findAvailableDevices() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter.startDiscovery();
            availableDevicesArrayList = new ArrayList<>();
            IntentFilter btDiscoveryFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(discoveryReceiver, btDiscoveryFilter);
        }
    }

    BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device.getName() != null && !pairedDevicesArrayList.contains(device)) {
                        availableDevicesArrayList.add(device);
                    }

                    available_device_adapter = new DeviceItemAdapter(availableDevicesArrayList, MainActivity.this, false, MainActivity.this);
                    available_device_recycler_view.setAdapter(available_device_adapter);
                    available_device_recycler_view.setLayoutManager(new LinearLayoutManager(MainActivity.this));

                    for (BluetoothDevice devices : availableDevicesArrayList) {
                        addToRawData(devices, false);
                    }
                }
            }
        }
    };

    private void onBluetoothDisabled() {

        Toast.makeText(this, "User canceled bluetooth permission.", Toast.LENGTH_SHORT).show();

//        unregisterReceiver(discoveryReceiver);
        switch_button.setChecked(false);
        turn_on_description.setText("Turn on bluetooth to see nearby Bluetooth devices");
        available_device_container.setVisibility(View.GONE);
        paired_device_info_text.setVisibility(View.VISIBLE);


    }


    private void checkBluetooth() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not found on this device", Toast.LENGTH_SHORT).show();
        } else {
//            makeDeviceDiscoverAble();
            if (bluetoothAdapter.isEnabled()) {
                onBluetoothEnabled();
            } else {
                switch_button.setChecked(false);
                turn_on_description.setText("Turn on bluetooth to see nearby Bluetooth devices");
                available_device_container.setVisibility(View.GONE);
                paired_device_info_text.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onItemClick(int position, boolean isPairedDevice) {
        Log.d(TAG, "onItemClick: " + position + " " + isPairedDevice);
        if (isPairedDevice) {
            //connect to it
            Toast.makeText(this, "connecting...", Toast.LENGTH_SHORT).show();

            BluetoothDevice device = pairedDevicesArrayList.get(position);


            final String PBAP_UUID = "0000112f-0000-1000-8000-00805f9b34fb";

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                if (device.getName() != null) {

                    BluetoothDevice act = bluetoothAdapter.getRemoteDevice(device.getAddress());
                    BluetoothSocket socket;
                    try {
                        socket = act.createInsecureRfcommSocketToServiceRecord(ParcelUuid.fromString(PBAP_UUID).getUuid());

                        device.connectGatt(MainActivity.this, true, callback);

                        socket.connect();
                        Log.d(TAG, "connected to device");
                        Toast.makeText(this, "connected to " + device.getName(), Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "connection failed. May be device is offline.", Toast.LENGTH_LONG).show();

                        Log.e("Connection", "general Error " + e.getMessage());
                        Log.d(TAG, "connectToPocoM3: " + e.getMessage());
                    }

                }
            }
        } else {
            //pair to it

            BluetoothDevice device = availableDevicesArrayList.get(position);
            if (!pairedDevicesArrayList.contains(device)) {
                Toast.makeText(this, "pairing...", Toast.LENGTH_SHORT).show();
                device.createBond();
                pairedDevicesArrayList.add(device);
                paired_device_adapter.notifyDataSetChanged();
                availableDevicesArrayList.remove(device);
                available_device_adapter.notifyDataSetChanged();
            } else {
                availableDevicesArrayList.remove(device);
                available_device_adapter.notifyDataSetChanged();
                Toast.makeText(this, "This device is already paired.", Toast.LENGTH_SHORT).show();
            }
        }


    }

    BluetoothGattCallback callback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG, "onConnectionStateChange: " + newState);

        }
    };

}
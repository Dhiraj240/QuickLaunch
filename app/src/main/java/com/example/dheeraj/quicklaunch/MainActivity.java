package com.example.dheeraj.quicklaunch;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity{
    Button b1,b2,b3,b4;
    private BluetoothAdapter BA;
    private ArrayList<BluetoothDevice> devices;
    private ArrayList<String> allDevices;
    ArrayAdapter<String> devicesListAdapter;
    private BluetoothDevice deviceToConnect;
    ListView lv;

    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_DISCOVERABLE_BT = 0;
    private static final String TAG = "Bluetooth";
    //TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    private BluetoothSocket curBTSocket = null;

    ClientThread connectThread;
    DeviceConnectThread deviceConnectThread;
    ServerConnectThread serverConnectThread;
    public static final String DEV_CONNECTED = "DEVICE_CONNECTED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        b1 = (Button)findViewById(R.id.button);
        b2 = (Button)findViewById(R.id.button2);
        b3 = (Button)findViewById(R.id.button3);
        b4 = (Button)findViewById(R.id.button4);
        lv = (ListView)findViewById(R.id.listView);

        BA = BluetoothAdapter.getDefaultAdapter();
        BA.startDiscovery();
        IntentFilter filter  = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(bReceiver);
        super.onDestroy();
    }

    public void on(View v){
        if(!BA.isEnabled()){
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned On", Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(getApplicationContext(), "Already On", Toast.LENGTH_LONG).show();
        }
        list(v);
        BA.startDiscovery();
        IntentFilter filter  = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bReceiver, filter);
    }

    public void off(View v){
        BA.disable();
        Toast.makeText(getApplicationContext(), "Turned Off", Toast.LENGTH_LONG).show();
    }

    public void visible(View v){
        Intent getVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(getVisible, 0);
    }

    public void list(View v){
        if(devices == null)
            devices = new ArrayList<BluetoothDevice>();
        else
            devices.clear();

        Set<BluetoothDevice>pairedDevices = BA.getBondedDevices();

        //pairedDevices = BA.getBondedDevices();

        ArrayList list = new ArrayList();

        for(BluetoothDevice bt : pairedDevices) {
            list.add(bt.getName());
            devices.add(bt);
        }

        Toast.makeText(getApplicationContext(), "Showing Paired Devices", Toast.LENGTH_LONG).show();

        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
        lv.setAdapter(adapter);
    }

    private final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice curDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int r = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);;
                devices.add(curDevice);
                Log.i("FOUND : ", curDevice.getName() + curDevice.getAddress());
            }
            if(devices.size()>0)
                showMe();
        }
    };

    public void showMe() {
        List<String> tempDevices = new ArrayList<String>();

        for (BluetoothDevice b : devices) {
            String paired = "Paired";
            if (b.getBondState() != 12) {
                paired = "Not Paired";
            }
            tempDevices.add(b.getName() + " - [ " + paired + " ] ");
        }
        if (allDevices == null)
            allDevices = new ArrayList<String>();
        else
            allDevices.clear();

        allDevices.addAll(tempDevices);

        if (devicesListAdapter == null) {
            ListView deviceList = new ListView(this);
            deviceList.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            //final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, allDevices);
            //lv.setAdapter(adapter);

            devicesListAdapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_1, android.R.id.text1, allDevices);
            deviceList.setAdapter(devicesListAdapter);

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle("Paired/UnpPired Devices");
            dialogBuilder.setView(deviceList);
            final AlertDialog alertDialogObject = dialogBuilder.create();
            deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    deviceToConnect = devices.get(position);
                    devicesListAdapter = null;
                    alertDialogObject.dismiss();
                    Log.i("Bluetooth", "Connecting to : " + deviceToConnect.getName());
                    //showMessage("Connecting to : " + deviceToConnect.getName());
                    Toast.makeText(getApplicationContext(), "Connecting to : " + deviceToConnect.getName(), Toast.LENGTH_LONG).show();

                    String address = deviceToConnect.getAddress();
                    BluetoothDevice b = BA.getRemoteDevice(address);
                    try {
                        final BluetoothSocket socket = b.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
                        socket.connect();
                        Intent intent = new Intent(MainActivity.this,Main2Activity.class);
                        startActivity(intent);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //killServerThread();

                    //Connect to the other device which is a server...
                    //connectAsClient();
                }
            });
            alertDialogObject.show();
            alertDialogObject.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    devicesListAdapter = null;
                }
            });
        } else {
            devicesListAdapter.notifyDataSetChanged();
        }
    }

    public void callingShowMe(View v){
        showMe();
    }
    public void connectAsClient() {
        Toast.makeText(getApplicationContext(), "Connecting with online device", Toast.LENGTH_LONG).show();
        //showMessage("Connecting for online Bluetooth devices...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (deviceToConnect != null) {
                    if (connectThread != null) {
                        connectThread.cancel();
                        connectThread = null;
                        //linSendMessage.setVisibility(View.GONE);
                    }
                    connectThread = new ClientThread();
                    curBTSocket = connectThread.connect(BA, deviceToConnect, MY_UUID_SECURE, mHandler);
                    connectThread.start();
                }
            }
        }).start();
    }

    public void killServerThread() {
        if (serverConnectThread != null) {
            serverConnectThread.closeConnection();
            serverConnectThread = null;
            //linSendMessage.setVisibility(View.GONE);
        }
    }

    private void startAsServer() {
        //showMessage("Listening for online Bluetooth devices...");
        Toast.makeText(getApplicationContext(), "Listening for online Bluetooth devices", Toast.LENGTH_LONG).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                serverConnectThread = new ServerConnectThread();
                curBTSocket = serverConnectThread.acceptConnection(BA, MY_UUID_SECURE, mHandler);
            }
        }).start();
    }


    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            byte[] buf = (byte[]) msg.obj;

            switch (msg.what) {

                case Constants.MESSAGE_WRITE:
                    // construct a string from the buffer
                    String writeMessage = new String(buf);
                    Log.i(TAG, "Write Message : " + writeMessage);
                    Toast.makeText(getApplicationContext(), "Message Sent : " + writeMessage, Toast.LENGTH_LONG).show();

                    //showMessage("Message Sent : " + writeMessage);

                    break;
                case Constants.MESSAGE_READ:
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(buf, 0, msg.arg1);
                    Log.i(TAG, "readMessage : " + readMessage);
                    Toast.makeText(getApplicationContext(), "Message Sent : " + readMessage, Toast.LENGTH_LONG).show();
                    //showMessage("Message Received : " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    String mConnectedDeviceName = new String(buf);
                    Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_LONG).show();
    //                showMessage("Connected to " + mConnectedDeviceName);
                    //linSendMessage.setVisibility(View.VISIBLE);
                    //sendMessageToDevice();
                    break;
                case Constants.MESSAGE_SERVER_CONNECTED:
                    //showMessage("CONNECTED");
                    Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_LONG).show();
                    Log.i(TAG, "Connected...");
                    //linSendMessage.setVisibility(View.VISIBLE);
                    break;
            }
        }
    };

}

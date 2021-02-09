package com.example.painelbt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class BluetoothActivity extends AppCompatActivity {
    // GUI Components
    //private TextView mReadBuffer;
    //private Button mScanBtn;
    //private Button mOffBtn;
    //private CheckBox mLED1;

    private TextView mBluetoothStatus;
    private Button mListPairedDevicesBtn;
    private Button mDiscoverBtn;
    private BluetoothAdapter mBTAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private ArrayAdapter<String> mBTArrayAdapter;
    private ListView mDevicesListView;
    private Switch mSwitchButton;
    private TextView mTextViewHeaderListDevice;


    //static private Handler mHandler; // Our main handler that will receive callback notifications
    //private ConnectedThread1 mConnectedThread; // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path

    private static final UUID BTMODULEUUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier


    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    private final static int MESSAGE_READ = 2;      // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status

    //Using Global variable to store information about BT status to be available across all activity
    GlobalClass globalVariable;
    String bluetoothStatus;

    private BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Bundle bundle = intent.getExtras();

            //Create the list of bluetooth devices
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mBTArrayAdapter.notifyDataSetChanged();
            }

            Log.d("BT ACTIVITY RECEIVED", "OK");

            //Receive information if app connected to led panel or not
            if (bundle != null) {
                String deviceName = bundle.getString(MyBluetoothServices.REMOTE_DEVICE_NAME);
                if (deviceName != null) {
                    bluetoothStatus = "Conectado a " + "\"" + deviceName + "\"";
                    mBluetoothStatus.setText(bluetoothStatus);
                    globalVariable.set_btStatus(bluetoothStatus);

                    mBluetoothStatus.setBackgroundColor(ContextCompat.getColor(context, R.color.myPanel_greenOK));
                    //mBluetoothStatus.setBackgroundColor(getResources().getColor(R.color.design_default_color_secondary));
                }

                int failureReason = bundle.getInt(MyBluetoothServices.FAILURE_REASON);
                if (failureReason == 11){
                    bluetoothStatus = "Desconexão remota";
                    mBluetoothStatus.setText(bluetoothStatus);
                    globalVariable.set_btStatus(bluetoothStatus);
                    mBluetoothStatus.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
                }
            }


        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(blReceiver, new IntentFilter(BLUETOOTH_SERVICE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(blReceiver);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        //Using Global variable to store information about BT status to be available across all activity
        globalVariable = (GlobalClass) getApplicationContext();
        bluetoothStatus = globalVariable.get_btStatus();
        mBluetoothStatus = (TextView) findViewById(R.id.bluetoothStatus);
        mBluetoothStatus.setText(bluetoothStatus);



        //mReadBuffer = (TextView) findViewById(R.id.readBuffer);
        //mScanBtn = (Button) findViewById(R.id.scan);
       // mOffBtn = (Button) findViewById(R.id.off);
        mDiscoverBtn = (Button) findViewById(R.id.discover);
        mListPairedDevicesBtn = (Button) findViewById(R.id.PairedBtn);
        //mLED1 = (CheckBox)findViewById(R.id.checkboxLED1);
        mTextViewHeaderListDevice = (TextView) findViewById(R.id.header_deviceListtextView);


        mBTArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio

        mDevicesListView = (ListView) findViewById(R.id.devicesListView);
        mDevicesListView.setAdapter(mBTArrayAdapter); // assign model to view
        mDevicesListView.setOnItemClickListener(mDeviceClickListener);


        mSwitchButton = (Switch) findViewById(R.id.on_off_bluetooth_switch1);
        mSwitchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    bluetoothOn();
                } else {
                    // The toggle is disabled
                    bluetoothOff();
                }
            }
        });


        if (mBTAdapter.isEnabled()) {
            mSwitchButton.setChecked(true);
        } else {
            mSwitchButton.setChecked(false);
        }

//        mHandler = new Handler() {
//            public void handleMessage(android.os.Message msg) {
////                if(msg.what == MESSAGE_READ){
////                    String readMessage = null;
////                    try {
////                        readMessage = new String((byte[]) msg.obj, "UTF-8");
////                    } catch (UnsupportedEncodingException e) {
////                        e.printStackTrace();
////                    }
////                    mReadBuffer.setText(readMessage);
////                }
//
//                if (msg.what == CONNECTING_STATUS) {
//                    if (msg.arg1 == 1)
//                        mBluetoothStatus.setText("Connected to Device: " + (String) (msg.obj));
//                    else
//                        mBluetoothStatus.setText("Connection Failed");
//                }
//            }
//        };


        if (mBTArrayAdapter == null) {
            // Device does not support Bluetooth
            //mBluetoothStatus.setText("Status: Bluetooth not found");
            bluetoothStatus = "Bluetooth não foi encontrado";
            mBluetoothStatus.setText(bluetoothStatus);
            globalVariable.set_btStatus(bluetoothStatus);

            Toast.makeText(getApplicationContext(), "Bluetooth device not found!", Toast.LENGTH_SHORT).show();
        } else {

            mListPairedDevicesBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mTextViewHeaderListDevice.setVisibility(v.VISIBLE);
                    listPairedDevices(v);


                }
            });

            mDiscoverBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mTextViewHeaderListDevice.setVisibility(v.VISIBLE);
                    discover(v);
                }
            });
        }
    } //end onCreate

    /**
     * startService()
     * Here, it is started for the first time the MyBluetoothServices for the first time.
     * In MyBluetoothServices it is done the final part of connection with device.
     * Once started it keep running in background even after close this activity.
     * In other activity is possible to bind to this service and continue to
     * use the interface started here to send and receive data from BT.
     *
     * @param view
     * @param addressPainel (MAC address of remote device)
     */
    public void startService(View view, String addressPainel, String deviceName) {
        Intent intentStartBT = new Intent(this, MyBluetoothServices.class);
        intentStartBT.putExtra("bluetooth_address", addressPainel);
        startService(intentStartBT);
    }



//    private void bluetoothOn(View view) {
//        if (!mBTAdapter.isEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//            mBluetoothStatus.setText("Bluetooth enabled");
//            Toast.makeText(getApplicationContext(), "Bluetooth turned on", Toast.LENGTH_SHORT).show();
//
//        } else {
//            Toast.makeText(getApplicationContext(), "Bluetooth is already on", Toast.LENGTH_SHORT).show();
//        }
//    }
    /**
     * Method bluetoothOn
     * It turn on the bluetooth device.
     */
    private void bluetoothOn() {
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            bluetoothStatus = "Bluetooth não foi encontrado";  //Bluetooth not found
            mBluetoothStatus.setText(bluetoothStatus);
            globalVariable.set_btStatus(bluetoothStatus);
            Toast.makeText(getApplicationContext(), "Bluetooth turned on", Toast.LENGTH_SHORT).show();

        } else {
            Toast.makeText(getApplicationContext(), "Bluetooth is already on", Toast.LENGTH_SHORT).show();
            mBluetoothStatus.setText("Bluetooth está ligado");
        }
    }

    // Enter here after user selects "yes" or "no" to enabling radio
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        // Check which request we're responding to
        super.onActivityResult(requestCode, resultCode, Data);
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                //mBluetoothStatus.setText("Enabled");
                bluetoothStatus = "Bluetooth Habilitado";
                mBluetoothStatus.setText(bluetoothStatus);
                globalVariable.set_btStatus(bluetoothStatus);
            } else {
                //mBluetoothStatus.setText("Disabled");
                bluetoothStatus = "Bluetooth Desabilitado";  //Bluetooth not found
                mBluetoothStatus.setText(bluetoothStatus);
                globalVariable.set_btStatus(bluetoothStatus);
            }
        }
    }


//    private void bluetoothOff(View view) {
//        mBTAdapter.disable(); // turn off
//        mBluetoothStatus.setText("Bluetooth disabled");
//        Toast.makeText(getApplicationContext(), "Bluetooth turned Off", Toast.LENGTH_SHORT).show();
//    }

    /**
     * Method bluetoothOff
     * It turn off the bluetooth device.
     */
    private void bluetoothOff() {
        mBTAdapter.disable(); // turn off
        //mBluetoothStatus.setText("Bluetooth disabled");
        bluetoothStatus = "Bluetooth Desabilitado";
        mBluetoothStatus.setText(bluetoothStatus);
        globalVariable.set_btStatus(bluetoothStatus);
        Toast.makeText(getApplicationContext(), "Bluetooth turned Off", Toast.LENGTH_SHORT).show();
        mBluetoothStatus.setText("Bluetooth está desligado");
    }

    /**
     * Method discover
     * It display in a list the devices discoverable nearby.
     *
     * @param view
     */
    private void discover(View view) {
        // Check if the device is already discovering
        if (mBTAdapter.isDiscovering()) {
            mBTAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(), "Discovery stopped", Toast.LENGTH_SHORT).show();
            mBluetoothStatus.setText("Descoberta foi interrompida");
        } else {
            if (mBTAdapter.isEnabled()) {
                mBTArrayAdapter.clear(); // clear items
                mBTAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
                mBluetoothStatus.setText("Descoberta foi iniciada");
                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            } else {
                Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                mBluetoothStatus.setText("Bluetooth esta desligado");
            }
        }
    }


    //final BroadcastReceiver blReceiver = new BroadcastReceiver() {
//    private BroadcastReceiver blReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            Bundle bundle = intent.getExtras();
//
//            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                // add the name to the list
//                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
//                mBTArrayAdapter.notifyDataSetChanged();
//            }
//
//            //if(action == BLUETOOTH_SERVICE) {
//
//            Log.d("BT ACTIVITY RECEIVED", "OK");
//
//            if (bundle != null) {
//                String deviceName = bundle.getString("remote_device_name");
//                if (deviceName != null) {
//                    mBluetoothStatus.setText("Connected to Device: " + deviceName);
//                }
//            }
//            //}
//
//
//        }
//    };

    /**
     * Method listPairedDevices
     * It list already paired devices.
     *
     * @param view
     */
    private void listPairedDevices(View view) {
        mPairedDevices = mBTAdapter.getBondedDevices();
        if (mBTAdapter.isEnabled()) {
            // put it's one to the adapter

            mBTArrayAdapter.clear(); //keep only unique elements  even after press button Pareados several times.
            for (BluetoothDevice device : mPairedDevices)
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());

            Toast.makeText(getApplicationContext(), "Show Paired Devices", Toast.LENGTH_SHORT).show();
            mBluetoothStatus.setText("Exibindo dispositivos pareados");
        } else {
            Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
            mBluetoothStatus.setText("Bluetooth está desligado");
        }
    }


    /**
     * Instantiate the mDeviceClickListener
     * After Paired Device or Found Devices are listed, any item of list is clickable, then
     * the remote device is selected and connected.
     */
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            if (!mBTAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }

            //mBluetoothStatus.setText("Connecting...");
            bluetoothStatus = "Conectando...";
            mBluetoothStatus.setText(bluetoothStatus);
            globalVariable.set_btStatus(bluetoothStatus);
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0, info.length() - 17);

            //call internal method startService(), which initiate the  MyBluetoothServices
            startService(v, address, name);
            //end


            // Spawn a new thread to avoid blocking the GUI one
//            new Thread()
//            {
//                public void run() {
//                    boolean fail = false;
//
//                    BluetoothDevice device = mBTAdapter.getRemoteDevice(address);
//
//                    try {
//                        mBTSocket = createBluetoothSocket(device);
//                    } catch (IOException e) {
//                        fail = true;
//                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
//                    }
//                    // Establish the Bluetooth socket connection.
//                    try {
//                        mBTSocket.connect();
//                    } catch (IOException e) {
//                        try {
//                            fail = true;
//                            mBTSocket.close();
//                            mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
//                                    .sendToTarget();
//                        } catch (IOException e2) {
//                            //insert code to deal with this
//                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                    if(fail == false) {
//                        //mConnectedThread = new ConnectedThread(mBTSocket);
//
//                        mConnectedThread = new ConnectedThread1(mBTSocket, mHandler);
//                        mConnectedThread.start();
//
//                        mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
//                                .sendToTarget();
//
//                        //set globals to use in another activity
//                        //globalVariable.setmBTSocket(mBTSocket);
//                        //globalVariable.setmHandler(mHandler);
//                    }
//                }
//            }.start();   //end of thread


        } //end onItemClick()
    };    //end instantiate of mDeviceClickListener

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connection with BT device using UUID
    }


}
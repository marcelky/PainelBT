package com.example.painelbt;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: 20/01/2021 Add delete code when press one item of list of msg 
// TODO: 20/01/2021 add Status option in the main screen 
// TODO: 20/01/2021 add feedback of confirmation/fail if command fail to execute in panel 
// FIXME: 20/01/2021 when select too many elements to delete from recyclerview the app crash
// FIXME: 20/01/2021 se o botao exibir mensagens as vezes pode ficar com informacao desatualizada
// TODO: 20/01/2021 add 1 more byte in the control message of BT 0xCC + 0x00 0x00, this is necessary in case ID message to delete exceed the 8bits (256)


 
public class MainActivity extends AppCompatActivity {

    //Configuration for launch second activity configure bluetooth
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String BT_CONF = "com.example.painelbt.BTconfiguration";

    boolean mBounded;
    MyBluetoothServices mServer;

    Switch mSwitchButton;
    TextView mHeaderListMessagesDelete;
    private TextView mBluetoothStatus;
    private MenuItem mMenuItemBluetooth;




    //Test of service using broadcast to communicate with activity
    public String inStream;

    //variables for recyclerView
    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private final LinkedList<MessageInfo> mWordList = new LinkedList<MessageInfo>();


    //Using Global variable to store information about BT status to be available across all activity
    GlobalClass globalVariable;
    String bluetoothStatus;


    private BroadcastReceiver receiver = new BroadcastReceiver() {
        String outStream;
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                String msgPanel = bundle.getString("message");
                String idMsg = bundle.getString("id");
                if((msgPanel !=null) && (idMsg != null)){
                    mWordList.add(new MessageInfo(msgPanel,idMsg));
                    Log.d("DEBUG", msgPanel + "+" + idMsg);
                }
                if (!mWordList.isEmpty())
                    mAdapter.notifyDataSetChanged(); //notify the MyAdapter to refresh list in the recyclerView
            }else {
                Toast.makeText(MainActivity.this, "lista vazia/não foi possível lê-la.",
                        Toast.LENGTH_LONG).show();
            }




                //Handling of data received when ReadMessages from panel
//                inStream = bundle.getString(MyBluetoothServices.INPUTSTREAM);
//                if(inStream !=null) {
//                    mWordList.clear();  //keep only unique elements  even after press button VER several times.
//                    String[] words = inStream.split("\\*\\+\\&");
//                    int wordsLen = words.length;
//
//                    if (((wordsLen%2)==0) && (wordsLen>=2)){
//                        if (words.length > 1)
//                            for (int i = 0; (i < words.length); i = i + 2) {
//                                if (words[i] != null && words[i + 1] != null) {
//                                    mWordList.add(new MainActivity.MessageInfo(words[i], words[i + 1]));
//                                }
//                                Log.d("DEBUG", words[i] + "+" + words[i + 1]);
//                            }
//
//                        if (!mWordList.isEmpty())
//                            mAdapter.notifyDataSetChanged(); //notify the MyAdapter to refresh list in the recyclerView
//
//                    }else {
//                        Toast.makeText(MainActivity.this, "lista vazia/não foi possível lê-la.",Toast.LENGTH_LONG).show();
//                    }
//
//                }
                //end Handling of data received when ReadMessages from panel

                outStream = bundle.getString(MyBluetoothServices.OUTPUTSTREAM);
                if(outStream!=null)
                    Toast.makeText(MainActivity.this,"TX: " + outStream,
                                    Toast.LENGTH_LONG).show();

                int failureReason = bundle.getInt(MyBluetoothServices.FAILURE_REASON);
                if (failureReason == MyBluetoothServices.MessageConstants.MESSAGE_BT_IN_STREAM_DISCONNECTED){
                    bluetoothStatus = "Desconexão remota";
                    mBluetoothStatus.setText(bluetoothStatus);
                    globalVariable.set_btStatus(bluetoothStatus);
                }

            }//onReceive

    };

    /**
     * Register to Broadcast receiver
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onResume() {
        super.onResume();
        //information about inputStream and outputStream
        registerReceiver(receiver, new IntentFilter(MyBluetoothServices.NOTIFICATION));
        //update about bluetooth interface
        registerReceiver(receiver, new IntentFilter(MyBluetoothServices.BLUETOOTH_SERVICE));

        listMessages();

        //this is to ensure the panel exit configuration mode. loop of same message being config.
        sendControlMessageBT("0D0000");

    }

    /**
     * Un-Register to Broadcast receiver
     */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }


    /***********************************************************************************************
     * onCreate()
     *
     * lifecycle callback
     * This is the starting point when this activity is called.
     * @param savedInstanceState
     **********************************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Using Global variable to store information about BT status to be available across all activity
        globalVariable = (GlobalClass) getApplicationContext();
        bluetoothStatus = globalVariable.get_btStatus();
        mBluetoothStatus = (TextView)findViewById(R.id.bluetoothStatus);
        mBluetoothStatus.setText(bluetoothStatus);

        mHeaderListMessagesDelete = (TextView)findViewById(R.id.header_deviceListtextView);
        mMenuItemBluetooth = (MenuItem) findViewById(R.id.bluetooth_item);



        //list the state of togglebutton to show message # or not
        mSwitchButton = (Switch) findViewById(R.id.show_messageNum_switch1);
        mSwitchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    sendControlMessageBT("0C0100");
                } else {
                    // The toggle is disabled
                    sendControlMessageBT("0C0000");
                }
            }
        });

        /******************************************************************************************
         * Setting of recyclerView
         ******************************************************************************************/
        recyclerView = (RecyclerView) findViewById(R.id.msgid_recyclerview);
        // use this setting to
        // improve performance if you know that changes
        // in content do not change the layout size
        // of the RecyclerView
        recyclerView.setHasFixedSize(true);
        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new MyAdapter(mWordList, this);
        recyclerView.setAdapter(mAdapter);

    }//end onCreate()

    /***********************************************************************************************
     * onStart()
     *
     * lifecycle callback
     * Bind to BT service MyBluetoothServices initiated by BluetoothActivity activity.
     ***********************************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onStart() {
        super.onStart();
        Intent mIntent = new Intent(this, MyBluetoothServices.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);

        //this is to ensure the panel exit configuration mode. loop of same message being config.
        sendControlMessageBT("0D0000");

    };

    /***********************************************************************************************
     * onStop()
     *
     * Lifecycle callback
     * Unbind to the BT service
     **********************************************************************************************/
    @Override
    protected void onStop() {
        super.onStop();
        if(mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
    };

    /**
     *
     *
     *
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.mymenu, menu);
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        switch (id){
            case R.id.bluetooth_item:
                Toast.makeText(this, "Bluetooth option",Toast.LENGTH_SHORT).show();
                launchConfigBT();
                break;
            case R.id.reset_item:
                Toast.makeText(this, "Reset",Toast.LENGTH_SHORT).show();
                factoryReset();
                break;
        }
        return true;
    }


    /**
     * launchConfigBT method
     * launch the configuration of bluetooth view in another activity.
     * @param view
     */
    public void launchConfigBT(View view) {
        Log.d(LOG_TAG, "Button clicked!");
        Intent intent = new Intent(this, BluetoothActivity.class);
        startActivity(intent);
    }


    public void launchConfigBT() {
        Log.d(LOG_TAG, "Button clicked!");
        Intent intent = new Intent(this, BluetoothActivity.class);
        startActivity(intent);
    }

    /**
     * Anonymous class to instantiate the mConnection which is binded to service MyBluetoothServices.
     */
    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            //Toast.makeText(MainActivity.this, "Service is disconnected", Toast.LENGTH_SHORT).show();
            mBounded = false;
            mServer = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //Toast.makeText(MainActivity.this, "Service is connected", Toast.LENGTH_SHORT).show();
            mBounded = true;
            MyBluetoothServices.LocalBinder mLocalBinder = (MyBluetoothServices.LocalBinder)service;
            mServer = mLocalBinder.getService()
            ;
        }
    };





    /**
     * sendDataBT
     * Open a new activity where sets the configuration for new message to store in the the panel.
     * @param view
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void sendDataBT(View view) {
        Log.d(LOG_TAG, "Button clicked!");

        sendControlMessageBT("0D0001"); //inform prompt it is time to config

        Intent intentAddMessage = new Intent(this, CreateMessageActivity.class);
        startActivity(intentAddMessage);


    }



    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void sendControlMessageBT(String messageCode){
        if(mServer != null){
            FormatMsg convert = new FormatMsg();
            mServer.sendData(convert.hexToAscii(messageCode));

            //Debug
            String tmp = convert.hexToAscii(messageCode);
            Log.d("DEBUG CtrlMsg", convert.asciiToHex(tmp));
        }
    }


    /**
     * saveConfigRom()
     * Send control message via BT to panel to save the configuration of messages created.
     * @param view
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void saveConfigRom(View view) {
        sendControlMessageBT("0B0000");
    }

//    public void showMessageNum(View view) {
//    }

    /**
     * Send control message via BT to panel to make a factory reset. Basically erase all
     * messages created just keeping the ones hardcoded in the system.
     * @param view
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void factoryReset(View view) {
        sendControlMessageBT("0A0000");
        mSwitchButton.setChecked(false);

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void factoryReset() {
        AlertDialog dialog = new AlertDialog.Builder( this)

                .setTitle("Restaurar a configuração de fábrica ?")
                .setMessage("TODAS AS MENSAGENS DO USUÁRIO SERÃO REMOVIDAS."+"\n"+"Selecione \"Sim\" para continuar e \"Não\" para cancelar.")

                //OK button dialog box
                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //send the command to reset the application.
                        sendControlMessageBT("0A0000");
                        mSwitchButton.setChecked(false);
                    }
                })

                //Cancel button dialog box
                .setNegativeButton("Não", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();













    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void listMessages(View view) {
        //mHeaderListMessagesDelete.setVisibility(view.VISIBLE);
        mWordList.clear();    //clear the array list to prevent duplicate data in recyclerView
        sendControlMessageBT("0F0000");
        Log.d("BT SERVICE", "Delete button pressed");
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void listMessages() {
        //mHeaderListMessagesDelete.setVisibility(view.VISIBLE);
        mWordList.clear();
        sendControlMessageBT("0F0000");
        Log.d("BT SERVICE", "Delete button pressed");
    }

    //support class to use in deleteMessage
    public static class MessageInfo {
        public String msgText;
        public String msgID;
        MessageInfo(String msgReceived, String id){
            msgText = msgReceived;
            //msgID = Integer.parseInt(id);
            msgID = id;
        }
    }

    public static String getOnlyASCII(String raw) {
        Pattern asciiPattern = Pattern.compile("\\p{ASCII}*$");
        Matcher matcher = asciiPattern.matcher(raw);
        String asciiString = null;
        if (matcher.find()) {
            asciiString = matcher.group();
        }
        return asciiString;
    }
}
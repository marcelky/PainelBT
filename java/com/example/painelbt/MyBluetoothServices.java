package com.example.painelbt;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.Vector;


// TODO: 20/01/2021 add a button to disconnect from device
// TODO: 20/01/2021 put the commands in portuguese
public class MyBluetoothServices extends Service {

    private BluetoothAdapter mBluetoothAdapter;
    public static final String B_DEVICE = "MY DEVICE";
    public static final String B_UUID = "00001101-0000-1000-8000-00805f9b34fb";
// 00000000-0000-1000-8000-00805f9b34fb

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private ConnectBtThread mConnectThread;
    private static ConnectedBtThread mConnectedThread;

    private static Handler mHandler = null;
    public static int mState = STATE_NONE;
    public static String deviceName;
    public static BluetoothDevice sDevice = null;
    public Vector<Byte> packData = new Vector<>(2048);

    IBinder mBinder = new LocalBinder();


    //Test of service using broadcast to communicate with activity
    public static final String NOTIFICATION = "com.example.painelbt.inputStream";
    public static final String INPUTSTREAM = "inputstream";
    public static final String OUTPUTSTREAM = "outputstream";
    public static final String REMOTE_DEVICE_NAME = "remote_device_name";
    public static final String FAILURE_REASON = "failure_reason";


    //GlobalClass globalVariable;


    final int handlerState = 0;
    private static Handler btHandler;
    private StringBuilder recDataString = new StringBuilder();


    // Defines several constants used when transmitting messages between the
    // service and the UI.
    public interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;
        public static final int MESSAGE_READ_0x0F = 4;  //Read list of message from panel
        public static final int MESSAGE_READ_0x0D = 5;  //Config message of panel


        public static final int MESSAGE_BT_STATUS = 10;
        public static final int MESSAGE_BT_IN_STREAM_DISCONNECTED = 11;
        // ... (Add other message types here as needed.)
    }


    /**
     * Return the communication channel to the service.
     *
     * @param intent
     * @return mBinder
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Return this instance of LocalService so clients can call public methods
     */
    public class LocalBinder extends Binder {
        MyBluetoothServices getService() {
            return MyBluetoothServices.this;
        }
    }

    public void toast(String mess) {
        Toast.makeText(this, mess, Toast.LENGTH_SHORT).show();
    }

    //private final IBinder mBinder = new LocalBinder();


    /***********************************************************************************************
     * onStartCommand()
     *
     * Callback of service. It is here were the services starts.
     * @param intent
     * @param flags
     * @param startId
     * @return
     **********************************************************************************************/
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String deviceg = intent.getStringExtra("bluetooth_address");
        //String deviceName = intent.getStringExtra("device_name");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        connectToDevice(deviceg);

         /******************************************************************************************
         * Handle to get the message from inputStream bluetooth
         ******************************************************************************************/
        btHandler = new Handler() {

            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            public void handleMessage(android.os.Message msg) {
                Log.d("DEBUG", "handleMessage:" + msg.what );
                String readMessage = (String) msg.obj;   // msg.arg1 = bytes from connect thread

                /**********************************************************************************
                 * inputStream read configuration from panel MESSAGE_READ_0x0F
                 * then this is informed to MainActivity.java via broadcast
                 **********************************************************************************/
                if (msg.what == MessageConstants.MESSAGE_READ_0x0F) {

                    recDataString.append(readMessage);

                    //extract Command code of this message byte[0] and [1]
                    String btOrderCode = recDataString.substring(0,3);
                    recDataString.delete(0,3);  //delete only [0] and [1]
                    FormatMsg cvt = new FormatMsg();

//                    String btOrderCode="";
//                    //test
//                    int endFirstSeparator = recDataString.lastIndexOf("*+&", 7);
//                    if (endFirstSeparator!=-1) {
//                        btOrderCode = recDataString.substring(0, endFirstSeparator);
//                        recDataString.delete(0, endFirstSeparator);
//                    }


                    Log.d("DEBUG", "btOrderCode:" + cvt.asciiToHex(btOrderCode));
                    Log.d("RECORDED", recDataString.toString());


                    //*************************************************
                    String[] words = recDataString.toString().split("\\*\\+\\&");
                    int wordsLen = words.length;

                    //if (((wordsLen % 2) == 0) && (wordsLen >= 2)) {
                    //words[0] = command
                    //words[1] = msg1 and words[2]=msg1_id
                    //words[3] = msg2 and words[3]=msg2_id and so on
                    if ((((wordsLen - 1) % 2) == 0) && (wordsLen >= 3)) {
                        //if (words.length > 1)
                            //for (int i = 0; (i < words.length); i = i + 2) {
                            for (int i = 1; (i < words.length); i = i + 2) {
                                if (words[i] != null && words[i + 1] != null) {
                                    informInStreamArrived(words[i], words[i + 1]);
                                }
                                Log.d("RECORDED", words[i] + "+" + words[i + 1]);
                            }

                    }

                    //*************************************************
                    recDataString.delete(0, recDataString.length());   //clear all string data
                }

                /**********************************************************************************
                 * inputStream response to command MESSAGE_READ_0x0D, configure new message panel
                 * It return the message Id, which can be used delete the message if not ok.
                 **********************************************************************************/
                if (msg.what == MessageConstants.MESSAGE_READ_0x0D) {
                    recDataString.append(readMessage);
                    String[] words = recDataString.toString().split("\\*\\+\\&");

                    if (words.length > 1 ){
                        if(words[1]!=null) {
                            informCreatedMessage(words[1]);
                        }
                    }

                    //*************************************************
                    recDataString.delete(0, recDataString.length());   //clear all string data

                }









                    //message received after write/send a message to remote device
                //then this is informed to MainActivity.java via broadcast
                if (msg.what == MessageConstants.MESSAGE_WRITE) {     //if message is what we want
                    String writeMessage = (String) msg.obj;           // msg.arg1 = bytes from connect thread
                    //recDataString.append(readMessage);

                    Log.d("RECORDED", writeMessage);
                    // Do stuff here with your data, inform MainActivity that new data arrive at InputStream.
                    informOutStreamSent(writeMessage);
                }

                //message received from ConnectBtThread after the remote device is connected
                if (msg.what == MessageConstants.MESSAGE_BT_STATUS) {
                    String remoteDeviceName = (String) msg.obj;
                    updateBluetoothStatus(remoteDeviceName);
                    Log.d("UPDATE BT", remoteDeviceName);

                }

                if (msg.what == MessageConstants.MESSAGE_BT_IN_STREAM_DISCONNECTED) {
                    updataBluetoothStatusFailure(MessageConstants.MESSAGE_BT_IN_STREAM_DISCONNECTED);
                }

            }
        };  //end btHandler


        return START_STICKY;
    }

    /**********************************************************************************************
     * Method to inform via broadcast to activity CreateMessageActivity
     * @param messageId
     **********************************************************************************************/
    private void informCreatedMessage(String messageId){
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra("messageCreatedID", messageId);
        sendBroadcast(intent);
    }


    /**
     * method to inform incomeStream arrived to MainActivity via Broadcast.
     *
     * @param bt_inputStream
     */
//    private void informInStreamArrived(String bt_inputStream) {
//        Intent intent = new Intent(NOTIFICATION);
//        intent.putExtra(INPUTSTREAM, bt_inputStream);
//        sendBroadcast(intent);
//    }


    private void informInStreamArrived(String bt_inputStream, String id) {
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra("message", bt_inputStream);
        intent.putExtra("id", id);
        sendBroadcast(intent);
    }


    /**
     * method to inform incomeStream arrived to MainActivity via Broadcast.
     *
     * @param bt_outputStream
     */
    private void informOutStreamSent(String bt_outputStream) {
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(OUTPUTSTREAM, bt_outputStream);
        sendBroadcast(intent);
    }


    private void updateBluetoothStatus(String remoteDeviceName) {
        Intent intent = new Intent(BLUETOOTH_SERVICE);
        intent.putExtra(REMOTE_DEVICE_NAME, remoteDeviceName);
        sendBroadcast(intent);
        Log.d("BROADCAST BT", remoteDeviceName);
    }

    private void updataBluetoothStatusFailure(int failNumber) {
        Intent intent = new Intent(BLUETOOTH_SERVICE);
        intent.putExtra(FAILURE_REASON, failNumber);
        sendBroadcast(intent);
        Log.d("BROADCAST BT Failure", Integer.toString(failNumber));
    }


    private synchronized void connectToDevice(String macAddress) {
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(macAddress);
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        mConnectThread = new ConnectBtThread(device);
        toast("connecting");
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    private void setState(int state) {
        mState = state;
        if (mHandler != null) {
            // mHandler.obtainMessage();
        }
    }

    public synchronized void stop() {
        setState(STATE_NONE);
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }

        stopSelf();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void sendData(String message) {
        if (mConnectedThread != null) {
            mConnectedThread.write(message.getBytes(StandardCharsets.ISO_8859_1));
            toast("sent data");
        } else {
            Toast.makeText(MyBluetoothServices.this, "Failed to send data", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public boolean stopService(Intent name) {
        setState(STATE_NONE);

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mBluetoothAdapter.cancelDiscovery();
        return super.stopService(name);
    }

/*private synchronized void connected(BluetoothSocket mmSocket){

    if (mConnectThread != null){
        mConnectThread.cancel();
        mConnectThread = null;
    }
    if (mConnectedThread != null){
        mConnectedThread.cancel();
        mConnectedThread = null;
    }

    mConnectedThread = new ConnectedBtThread(mmSocket);
    mConnectedThread.start();


    setState(STATE_CONNECTED);
}*/

    private class ConnectBtThread extends Thread {
        private final BluetoothSocket mSocket;
        private final BluetoothDevice mDevice;

        //constructor
        public ConnectBtThread(BluetoothDevice device) {
            mDevice = device;
            BluetoothSocket socket = null;
            try {
                socket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(B_UUID));
            } catch (IOException e) {
                e.printStackTrace();
            }
            mSocket = socket;

        }

        @Override
        public void run() {
            mBluetoothAdapter.cancelDiscovery();

            try {
                mSocket.connect();
                Log.d("service", "connect thread run method (connected)");
                SharedPreferences pre = getSharedPreferences("BT_NAME", 0);
                pre.edit().putString("bluetooth_connected", mDevice.getName()).apply();

            } catch (IOException e) {

                try {
                    mSocket.close();
                    Log.d("service", "connect thread run method ( close function)");


                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace();
            }
            //connected(mSocket);
            mConnectedThread = new ConnectedBtThread(mSocket);
            mConnectedThread.start();


        }

        public void cancel() {

            try {
                mSocket.close();
                Log.d("service", "connect thread cancel method");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    } //end of class ConnectBtThread


    //Reference: pagina do google
    //https://developer.android.com/guide/topics/connectivity/bluetooth
    private class ConnectedBtThread extends Thread {
        private static final String TAG = "ConnectedBtThread Debug";
        private final BluetoothSocket cSocket;
        private final InputStream inS;
        private final OutputStream outS;
        private byte[] mmBuffer;          // mmBuffer store for the stream

        public ConnectedBtThread(BluetoothSocket socket) {
            cSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //Update status
            String deviceName = cSocket.getRemoteDevice().getName();
            if (deviceName != null) {
                Message readStatus = btHandler.obtainMessage(MessageConstants.MESSAGE_BT_STATUS, (String) deviceName);
                readStatus.sendToTarget();
            }
            //End Update status


            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            inS = tmpIn;
            outS = tmpOut;
        }

        /******************************************************************************************
         * run()
         * This is method is called evertime there is incomeStream in the bluetooth interface.
         ******************************************************************************************/
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void run() {
            mmBuffer = new byte[2048];
            int numBytes = 0;
            StringBuilder readStream = new StringBuilder("");

            char[] endFile = {0xff, 0xff, 0xff, 0xff};
            String eof = String.valueOf(endFile);

            char[] startFile = {0xff, 0x00, 0xff, 0x00};
            String startFileStr = String.valueOf(startFile);

            int messageType = MessageConstants.MESSAGE_READ;


            //keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    if (inS.available() > 3) {
                        numBytes = inS.read(mmBuffer);

                        readStream.append(new String(mmBuffer, 0, numBytes, "ISO-8859-1"));

                        Log.d("DEBUG","readStream: " + readStream.toString());
                        String readStreamHex = new FormatMsg().asciiToHex(readStream.toString());
                        Log.d("DEBUG","readStreamHex: " + readStreamHex);



                        //Condition indicating the response to certain request arrived back
                        if(readStreamHex.contains("FF00FF00")){     //start file sent by panel
//                        if(readStreamHex.contains("F0000") ||               //Resposta para listar mensagens do painel
//                                (readStreamHex.contains("DFF00"))){         //Resposta a config. nova mensagem


                            //condição de contorno para indicar que chegaram todos os dados
                            if (readStreamHex.contains("FFFFFFFF")) { //end of file


                                //cmd configuration of message
                                if(readStreamHex.contains("DFF00")){
                                    messageType =  MessageConstants.MESSAGE_READ_0x0D;
                                    Log.d("DEBUG", "messageType " + "MESSAGE_READ_0x0D");
                                }

                                if(readStreamHex.contains("F0000")){
                                    messageType = MessageConstants.MESSAGE_READ_0x0F;
                                    Log.d("DEBUG", "messageType " + "MESSAGE_READ_0x0F");
                                }


                                /*********************************************************************
                                 * delete the 0xFF 0xFF 0xFF 0xFF at end of income Stream
                                 * panel insert to indicate end of file.
                                 *********************************************************************/
                                int indexEof = readStream.indexOf(eof);
                                if (indexEof >= 0 ){
                                    readStream.delete(indexEof, readStream.toString().length());
                                }
                                Log.d("DEBUG", "readStream after chop EOF " + readStream);

                                /*********************************************************************
                                 * delete the 0xFF 0x00 0xFF 0x00 at beginning of income Stream
                                 * panel insert to indicate start of file.
                                 *********************************************************************/
                                int indexStartFile = readStream.indexOf(startFileStr);
                                if (indexStartFile != -1){
                                    readStream.delete(0, indexStartFile + startFileStr.length());
                                }
                                Log.d("DEBUG", "readStream after chop EOF " + readStream);



                                /*********************************************************************
                                 * send message handler
                                 *********************************************************************/
                                //messageType = MessageConstants.MESSAGE_READ_0x0F;
                                Message readMsg = btHandler.obtainMessage(
                                        messageType, numBytes, -1,
                                        readStream.toString());
                                readMsg.sendToTarget();

                                //readStream.delete(0, readStream.toString().length()-1);
                                readStream.delete(0, readStream.toString().length());
                            }

                        }else{
                            readStream.delete(0, readStream.toString().length());
                        }

                        //wait awhile to read more message in the buffer
                    }else{
                        SystemClock.sleep(5);
                    }

                } catch (IOException e) {
                    Log.d("DEBUG BT PART", "Input stream was disconnected", e);
                    Message readMsg = btHandler.obtainMessage(MessageConstants.MESSAGE_BT_IN_STREAM_DISCONNECTED);
                    readMsg.sendToTarget();
                    break;
                }
            }//end while
        }

        /******************************************************************************************
         * run() => this version was ok, problem to detect the reception after configure the message
         * This is method is called evertime there is incomeStream in the bluetooth interface.
         */
//        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
//        @Override
//        public void run() {
//            mmBuffer = new byte[2048];
//            int numBytes = 0;
//            StringBuilder readStream = new StringBuilder("");
//
//            char[] endFile = {0xff, 0xff, 0xff, 0xff};
//            String eof = String.valueOf(endFile);
//
//            int messageType = MessageConstants.MESSAGE_READ;
//
//
//            //keep listening to the InputStream until an exception occurs
//            while (true) {
//                try {
//                    if (inS.available() > 3) {
//                        numBytes = inS.read(mmBuffer);
//
//                        readStream.append(new String(mmBuffer, 0, numBytes, "ISO-8859-1"));
//
//                        Log.d("DEBUG","readStream: " + readStream.toString());
//                        String readStreamHex = new FormatMsg().asciiToHex(readStream.toString());
//                        Log.d("DEBUG","readStreamHex: " + readStreamHex);
//
//
//                        if(readStreamHex.contains("F0000") ||               //Resposta para listar mensagens do painel
//                                (readStreamHex.contains("DFF00"))){         //Resposta a config. nova mensagem
//
//                            //condição de contorno para indicar que chegaram todos os dados
//                            if (readStreamHex.contains("FFFFFFFF")) {
//
//
//                                //cmd configuration of message
//                                if(readStreamHex.contains("DFF00")){
//                                    messageType =  MessageConstants.MESSAGE_READ_0x0D;
//                                    Log.d("DEBUG", "messageType " + "MESSAGE_READ_0x0D");
//                                }
//
//                                if(readStreamHex.contains("F0000")){
//                                    messageType = MessageConstants.MESSAGE_READ_0x0F;
//                                    Log.d("DEBUG", "messageType " + "MESSAGE_READ_0x0F");
//                                }
//
//
//                                /*********************************************************************
//                                 * delete the 0xFF 0xFF 0xFF 0xFF at end of income Stream
//                                 * panel insert to indicate end of file.
//                                 *********************************************************************/
//                                int indexEof = readStream.indexOf(eof);
//                                if (indexEof >= 0 ){
//                                    readStream.delete(indexEof, readStream.toString().length());
//                                }
//                                Log.d("DEBUG", "readStream after chop EOF " + readStream);
//
//
//
//                                /*********************************************************************
//                                 * send message handler
//                                 *********************************************************************/
//                                //messageType = MessageConstants.MESSAGE_READ_0x0F;
//                                Message readMsg = btHandler.obtainMessage(
//                                        messageType, numBytes, -1,
//                                        readStream.toString());
//                                readMsg.sendToTarget();
//
//                                //readStream.delete(0, readStream.toString().length()-1);
//                                readStream.delete(0, readStream.toString().length());
//                            }
//
//                        }else{
//                            readStream.delete(0, readStream.toString().length());
//                        }
//
//                        //wait awhile to read more message in the buffer
//                    }else{
//                        SystemClock.sleep(20);
//                    }
//
//                } catch (IOException e) {
//                    Log.d("DEBUG BT PART", "Input stream was disconnected", e);
//                    Message readMsg = btHandler.obtainMessage(MessageConstants.MESSAGE_BT_IN_STREAM_DISCONNECTED);
//                    readMsg.sendToTarget();
//                    break;
//                }
//            }//end while
//        }






        /******************************************************************************************
         * Method to send data out to panel via bluetooth interface
         * @param buff
         ******************************************************************************************/

        public void write(byte[] buff) {
            try {
                outS.write(buff);   //THIS IS THE ESSENTIAL PART, THE REMAINING IS NOT.

                // Share the sent message with the UI activity.

                FormatMsg convert = new FormatMsg();
                String outStr = convert.encodeHexString(buff);
                Log.d("RECORDED1", outStr);
                //Log.d("RECORDED1", buff);
                Message writtenMsg = btHandler.obtainMessage(
                        MessageConstants.MESSAGE_WRITE, -1, -1, outStr);

                writtenMsg.sendToTarget();

            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                Message writeErrorMsg =
                        btHandler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                btHandler.sendMessage(writeErrorMsg);
            }
        }

        // Call this method from the main activity to shut down the connection.
        private void cancel() {
            try {
                cSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    } //end of class ConnectedBtThread

    @Override
    public void onDestroy() {
        this.stop();
        super.onDestroy();
    }


} //End of MyBluetoothServices class
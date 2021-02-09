package com.example.painelbt;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.InputType;
import android.text.method.SingleLineTransformationMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

import yuku.ambilwarna.AmbilWarnaDialog;

// TODO: 20/01/2021 camuflar seekbar de acordo com o modo de exibicao do painel
public class CreateMessageActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    TextView mColorTextView;
    EditText mMessagePanel;
    Button mColorButton;
    Button mSendMessageToPanel;
    Button mDeleteButton;
    SeekBar mScrollSpeedSeekBar;
    SeekBar mDurationTextSeekBar;
    SeekBar mBlinkIntervalSeekBar;
    Spinner mSpinnerIcon;
    Spinner mSpinnerIconColor;



    TextInputLayout mTextInput;

    CheckBox mItalic;
    CheckBox mBold;
    CheckBox mMultline;

    public TextView mSpeedScrollTextView;
    private TextView mPresentationDurationTextView;
    private TextView mBlinkTextView;


    //Variables to be used to create the message to panel
    int mDefaultColor;               //color of letters in the panel
    int letterSize;                  //size of letter for panel
    int formatCode;                  //type of visual effect, static, blinking or scroll
    int scrollSpeed;                 //scroll speed
    int durationText;                //duration that the message is presented in seconds
    int blinkInterval;               //interval in seconds between blink of message
    boolean isItalic;                //indicate if the text is italic
    boolean isBold;                  //indicate if the text is bold
    int indexImagem;                 //indice da imagem bmp 0-30, 31 indica sem config de imagem

    String hexValStringMsgId;        //Id created for the message just created

    //boolean isMultline;

    //object to support format message to send via bluetooth
    FormatMsg configMessage = new FormatMsg();

    //variables to control bluetooth connection
    boolean mBounded;
    MyBluetoothServices mServer;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        String outStream;
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                String idMsg = bundle.getString("messageCreatedID");
                Log.d("DEBUG", "Create Message rec ID: " + idMsg);

                if (idMsg != null) {
                    if(idMsg.contains("000000")) {
                        Toast.makeText(getApplicationContext(), "Número máximo de mensagens, 40, foi alcançado. Esta mensagem não foi configurada.", Toast.LENGTH_LONG).show();
                    }else {
                        int string2IntDecimal = Integer.parseInt(idMsg);
                        //convert decimal to hex, with 2 digits. e.g. decimal 16 to 0F hex string (2 digits)
                        hexValStringMsgId = String.format("%04X", string2IntDecimal);

                        mDeleteButton.setEnabled(true);

                        //Test to disable button enviar and enable when the income stream arrive
                        mSendMessageToPanel.setEnabled(true);
                    }
                }
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_message);

        //mColorTextView = (TextView) findViewById(R.id.colorSelected_textView);
        mDefaultColor = ContextCompat.getColor(CreateMessageActivity.this,
                R.color.design_default_color_primary_dark);
        configMessage.color = mDefaultColor;

        mColorButton = (Button) findViewById(R.id.textcolor_button);
        mSendMessageToPanel = (Button) findViewById(R.id.send_message_button);
        mDeleteButton = (Button)findViewById(R.id.delete_button);

        mMessagePanel = (EditText) findViewById(R.id.add_message_editText);

        mTextInput = (TextInputLayout) findViewById(R.id.textContainer);

        mScrollSpeedSeekBar = (SeekBar) findViewById(R.id.speed_seekBar);
        mBlinkIntervalSeekBar = (SeekBar) findViewById(R.id.intervalFlash_seekBar);
        mDurationTextSeekBar = (SeekBar) findViewById(R.id.duration_seekBar);

        mSpeedScrollTextView = (TextView) findViewById(R.id.speedScroll_textView);
        mPresentationDurationTextView = (TextView) findViewById(R.id.presentDuration_textView);
        mBlinkTextView = (TextView) findViewById(R.id.blink_textView);

        mItalic = (CheckBox) findViewById(R.id.italic_checkBox);
        mBold = (CheckBox) findViewById(R.id.bold_checkBox);
        mMultline = (CheckBox) findViewById(R.id.multiline_checkBox);

        mSpinnerIcon = (Spinner) findViewById(R.id.spinner);
        mSpinnerIconColor =(Spinner)findViewById(R.id.spinner_color);

        //Default values for seekbar
        scrollSpeed = 1;
        durationText = 5;

        //at beginning only the seek bar for scroll is enabled
        mScrollSpeedSeekBar.setEnabled(true);
        mScrollSpeedSeekBar.setClickable(true);
        mBlinkIntervalSeekBar.setEnabled(false);
        mBlinkIntervalSeekBar.setClickable(false);
        mDurationTextSeekBar.setEnabled(false);
        mDurationTextSeekBar.setClickable(false);

        //spinner click listener
        //mSpiner.setOnItemClickListener(this);




        /*******************************************************************************************
         * Spinner drop down elementes
         ******************************************************************************************/
        mSpinnerIcon.setOnItemSelectedListener(this);

        ArrayList<String> bmpItem = new ArrayList<String>();
        bmpItem.add("Usar Ícone");
        bmpItem.add("Whatsapp");
        bmpItem.add("Telefone");
        bmpItem.add("Email");
        bmpItem.add("Endereço");
        bmpItem.add("Website");
        bmpItem.add("Localização");
        bmpItem.add("Horários");
        bmpItem.add("Cachorro");
        bmpItem.add("@ arroba");
        bmpItem.add("% porcent.");
        bmpItem.add("Café");
        bmpItem.add("Carro");
        bmpItem.add("Ar-condic.");
        bmpItem.add("Coração");
        bmpItem.add("Estacionamento");
        bmpItem.add("Wifi");
        bmpItem.add("Máscara");
        bmpItem.add("Cool");
        bmpItem.add("Love it");
        bmpItem.add("Smile");
        bmpItem.add("Surpresa");
        bmpItem.add("Lava mão");
        bmpItem.add("Tao");

        mSpinnerIcon.setSelection(0);

        //Creating adapter for spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item, bmpItem);

        //Drop down layout styel - list view with radio button
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        //attaching data adapter to spinner
        mSpinnerIcon.setAdapter(adapter);



        /*******************************************************************************************
        * Spinner drop down icon colors
        ******************************************************************************************/
        mSpinnerIconColor.setOnItemSelectedListener(this);

        ArrayList<String> bmpColorItem = new ArrayList<String>();
        bmpColorItem.add("Cor Ícone");
        bmpColorItem.add("Vermelho");
        bmpColorItem.add("Verde");
        bmpColorItem.add("Azul");
        bmpColorItem.add("Alaranjado");
        bmpColorItem.add("Roxo");
        bmpColorItem.add("Ciano");
        bmpColorItem.add("Amarelo");
        bmpColorItem.add("Branco");



        mSpinnerIconColor.setSelection(0);

        //Creating adapter for spinner
        ArrayAdapter<String> adapterColor = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item, bmpColorItem);

        //Drop down layout style - list view with radio button
        adapterColor.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        //attaching data adapter to spinner
        mSpinnerIconColor.setAdapter(adapterColor);

        /*******************************************************************************************
         * SeekBar for scroll speed
         ******************************************************************************************/
        mScrollSpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            //int scSpeed = 1;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //scSpeed = progress;
                scrollSpeed = progress;
                configMessage.scrollSpeed = (byte) scrollSpeed;
                mSpeedScrollTextView.setText("Velocidade da mensagem: " + scrollSpeed);
                //updateMyTextView("Velocidade da mensagem: ",mSpeedScrollTextView, scrollSpeed);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //write custom code to on start progress
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //tView.setText(pval + "/" + seekBar.getMax());
            }
        });

        /*******************************************************************************************
         * seekbar for Duration of presentation
         ******************************************************************************************/
        mDurationTextSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            //int durText = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //durText = progress;
                durationText = progress;
                configMessage.textDuration = (byte) durationText;
                mPresentationDurationTextView.setText("Duração da mensagem: " + durationText + " s");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //write custom code to on start progress
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //tView.setText(pval + "/" + seekBar.getMax());
            }
        });

        /*******************************************************************************************
         * SeekBar for interval of blink
         ******************************************************************************************/
        mBlinkIntervalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            //int bInterval = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //bInterval = progress;
                blinkInterval = progress;
                configMessage.flashPeriod = (byte) blinkInterval;
                mBlinkTextView.setText("Intervalo On/Off: " + blinkInterval + " s");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //write custom code to on start progress
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //tView.setText(pval + "/" + seekBar.getMax());
            }
        });


        /*******************************************************************************************
         * Message textEdit change listener to show/not show the hint message
         ******************************************************************************************/
        mMessagePanel.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mMessagePanel.setHint("");
                return false;
            }

        });


        mMessagePanel.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    mMessagePanel.setHint("");
                else
                    mMessagePanel.setHint("Digite sua mensagem");
            }
        });


        /*******************************************************************************************
         * Button Enviar, touchListener to prevent to execute the command to fast in case the button
         * receive a burst of press up and down.
         ******************************************************************************************/
//        mSendMessageToPanel.setOnTouchListener(new RepeatListener(3000,
//                2000, new View.OnClickListener() {
//            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
//            @Override
//            public void onClick(View view) {
//
//                mSendMessageToPanel.playSoundEffect(SoundEffectConstants.CLICK);
//                configMessage.messageAsciiFormat = mMessagePanel.getText().toString();
//                configMessage.textLength = (byte) configMessage.messageAsciiFormat.length();
//                //String hexStr = configMessage.assembleMessageSequence();
//
//                if (mServer != null) {
//                    String hexStr = configMessage.assembleMessageSequence();
//                    mServer.sendData(hexStr);
//                }
//
//                //Test to disable button enviar and enable when the income stream arrive
//                mSendMessageToPanel.setEnabled(false);
//
//            }
//        }));

        mSendMessageToPanel.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                mSendMessageToPanel.playSoundEffect(SoundEffectConstants.CLICK);
                configMessage.messageAsciiFormat = mMessagePanel.getText().toString();
                configMessage.textLength = (byte) configMessage.messageAsciiFormat.length();
                //String hexStr = configMessage.assembleMessageSequence();

                if (mServer != null) {
                    String hexStr = configMessage.assembleMessageSequence();
                    mServer.sendData(hexStr);
                }

                //Test to disable button enviar and enable when the income stream arrive
                mSendMessageToPanel.setEnabled(false);
            }

        });


        /******************************************************************************************
         * button delete message
         ******************************************************************************************/
        mDeleteButton.setOnClickListener(new View.OnClickListener(){
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v){
                String deleteCode = "0E";
                String hexStr = deleteCode + hexValStringMsgId;
                sendControlMessageBT(hexStr);
                mDeleteButton.setEnabled(false);
            }


        });





    }//end onCreate()

    public void updateMyTextView(String str, TextView tv, int value) {
        tv.setText("str" + value);
    }


    /**********************************************************************************************
     * This method send the message to panel, before send it convert the string hex -> string ASCII
     * @param messageCode string hex format
     *********************************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void sendControlMessageBT(String messageCode){

        if(mServer != null){
            mServer.sendData(new FormatMsg().hexToAscii(messageCode));
            Log.d("DEBUG CtrlMsg", messageCode);
        }else{
            Log.d("DEBUG CtrlMsg failed", messageCode);
        }
    }



    /**
     * onStart method
     * Bind to BT service MyBluetoothServices initiated by BluetoothActivity activity.
     */
    @Override
    protected void onStart() {
        super.onStart();
        Intent mIntent = new Intent(this, MyBluetoothServices.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);

        //Enter config mode on panel
        //byte[0] 0x0D config message
        //byte[1] 0x00 select loop status
        //byte[2] 0x01 enable loop of config
        //        only the msg config is shown.
        //sendControlMessageBT("0D0001");
    }


    /**
     * onStop method
     * Unbind BT service MyBluetoothServices.
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onStop() {
        super.onStop();
        if (mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }

        //this command disable the configuration mode in the panel and
        //change it to normal presentation
        //byte[0] 0x0D config message
        //byte[1] 0x00 select loop status
        //byte[2] 0x01 enable loop of config
        //        only the msg config is shown.
        sendControlMessageBT("0D0000");  //Exit config mode on panel
    }




    /**********************************************************************************************
     * Implementation of spinner selection of icon
     **********************************************************************************************/
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        switch (parent.getId()) {
            case R.id.spinner:


                // On selecting a spinner item
                if (position > 0) {
                    String item = parent.getItemAtPosition(position).toString();

                    // Showing selected spinner item
                    //Toast.makeText(this, "Selected: " + item, Toast.LENGTH_LONG).show();
                    Toast.makeText(getApplicationContext(), "Selected: " + item + "," + position, Toast.LENGTH_LONG).show();
                    configMessage.bmpImageIndex = (byte) (position - 1);
                    Log.d("DEBUG SPINNER", item);
                } else {
                    Toast.makeText(getApplicationContext(), "Nenhum ícone selecionado", Toast.LENGTH_LONG).show();
                    //configMessage.bmpImageIndex = (byte) 0x1F;
                    configMessage.bmpImageIndex = (byte) 0xFF;
                }
                break;

            case R.id.spinner_color:
                // On selecting a spinner item
                if (position > 0) {
                    String color = parent.getItemAtPosition(position).toString();

                    // Showing selected spinner item
                    //Toast.makeText(this, "Selected: " + item, Toast.LENGTH_LONG).show();
                    Toast.makeText(getApplicationContext(), "Selected: " + color + "," + position, Toast.LENGTH_LONG).show();
                    configMessage.bmpImageColor = (byte) (position - 1);
                    Log.d("DEBUG SPINNER", color);
                } else {
                    Toast.makeText(getApplicationContext(), "Nenhum ícone selecionado", Toast.LENGTH_LONG).show();
                    //configMessage.bmpImageIndex = (byte) 0x1F;
                    configMessage.bmpImageColor = (byte) 0x07;
                }
                break;

        }

    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub


        if (arg0.getId() == R.id.spinner ) {
            configMessage.bmpImageIndex = (byte) 0xFF;
        } else if (arg0.getId() == R.id.spinner_color ) {
            configMessage.bmpImageColor = 0x07; //default color white
        }
    }

    /**********************************************************************************************
     * Implementation of spinner selection of color
     **********************************************************************************************/


    /**********************************************************************************************
     * Implementation of spinner selection of color
     **********************************************************************************************/



    /**
     * Anonymous class to instantiate the mConnection which is binded to service MyBluetoothServices.
     */
    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(CreateMessageActivity.this, "Service is disconnected", Toast.LENGTH_SHORT).show();
            mBounded = false;
            mServer = null;
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Toast.makeText(CreateMessageActivity.this, "Service is connected", Toast.LENGTH_SHORT).show();
            mBounded = true;
            MyBluetoothServices.LocalBinder mLocalBinder = (MyBluetoothServices.LocalBinder) service;
            mServer = mLocalBinder.getService();

            Log.d("DEBUG","onServiceConnected to BluetoothService");

            //Enter config mode on panel
            //byte[0] 0x0D config message
            //byte[1] 0x00 select loop status
            //byte[2] 0x01 enable loop of config
            //        only the msg config is shown.
            sendControlMessageBT("0D0001");
        }
    };


    /**
     * openColorPicker method is called by button Cor do Texto and store the
     * color to be used in the message.
     *
     * @param view
     */
    public void openColorPicker(View view) {
        AmbilWarnaDialog colorPicker = new AmbilWarnaDialog(this, mDefaultColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
            }

            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                mDefaultColor = color;
                //mColorTextView.setBackgroundColor(mDefaultColor);
                mMessagePanel.setTextColor(mDefaultColor);
                configMessage.color = mDefaultColor;
            }
        });
        colorPicker.show();
    }


    /**
     * defineFontSize method
     * Define the size of font to be use for message.
     * In the HW of panel 0 = small font
     * 1 = big font
     *
     * @param view
     */
    public void defineFontSize(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.big_font_radioButton:
                if (checked) {
                    configMessage.textSize = 1; // big letters
                    mMultline.setChecked(false);

                    mMessagePanel.setInputType(InputType.TYPE_CLASS_TEXT);
                    mMessagePanel.setLines(1);
                    mMessagePanel.setTransformationMethod(SingleLineTransformationMethod.getInstance());

                    String trimNewline = String.valueOf(mMessagePanel.getText()).replace("\n", " ");
                    String trimCarrierRet = trimNewline.replace("\r", " ");
                    mMessagePanel.setText(trimCarrierRet);

                    mMultline.setEnabled(false);
                    mItalic.setEnabled(true);
                    mBold.setEnabled(true);


                }
                break;
            case R.id.small_font_radioButton:
                if (checked) {
                    configMessage.textSize = 0; // small letters

                    //unckeck and disable Italic
                    if (mItalic.isChecked())
                        mItalic.setChecked(false);
                    mItalic.setEnabled(false);

                    //uncheck and disable Bold
                    if (mBold.isChecked())
                        mBold.setChecked(false);
                    mBold.setEnabled(false);

                    //enable multiline and check what to configure
                    mMultline.setEnabled(true);

//                    if (mMultline.isChecked())
//                        mMessagePanel.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
//                    else
//                        mMessagePanel.setInputType(InputType.TYPE_CLASS_TEXT);

                }
                break;
        }
    }

    /**
     * onCheckboxClicked method
     * It check if the checkbox for italic and box are checked or not.
     *
     * @param view
     */
    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();

        // Check which checkbox was clicked
        // These italic/bold is only valid for big letter, for small letter this values are
        // ignored.
        switch (view.getId()) {
            case R.id.italic_checkBox:
                if (checked)
                    isItalic = true;

                else
                    isItalic = false;
                break;
            case R.id.bold_checkBox:
                if (checked)
                    isBold = true;
                else
                    isBold = false;
                break;

            case R.id.multiline_checkBox:
                if (checked) {
                    mMessagePanel.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                    mMessagePanel.setLines(2);

                } else {
                    mMessagePanel.setInputType(InputType.TYPE_CLASS_TEXT);
                    mMessagePanel.setLines(1);

                    //mMessagePanel.setTransformationMethod(SingleLineTransformationMethod.getInstance());
                    mMessagePanel.setTransformationMethod(SingleLineTransformationMethod.getInstance());

                    String trimNewline = String.valueOf(mMessagePanel.getText()).replace("\n", " ");
                    String trimCarrierRet = trimNewline.replace("\r", " ");

                    Log.d("DEBUG EditText orig", String.valueOf(mMessagePanel.getText()));
                    Log.d("DEBUG EditText trim", trimCarrierRet);
                    mMessagePanel.setText(trimCarrierRet);


                }

                break;


        }
        if (isItalic) {
            if (isBold) {
                mMessagePanel.setTypeface(null, Typeface.BOLD_ITALIC);
                configMessage.letterFont = 3;
            } else {
                mMessagePanel.setTypeface(null, Typeface.ITALIC);
                configMessage.letterFont = 2;
            }
        } else {
            if (isBold) {
                mMessagePanel.setTypeface(null, Typeface.BOLD);
                configMessage.letterFont = 1;
            } else {
                mMessagePanel.setTypeface(null, Typeface.NORMAL);
                configMessage.letterFont = 0;
            }
        }
    }


    /**
     * defineVisualEffects method
     * Define if the message in the panel will be:
     * scroll             = 1
     * blinking           = 2
     * static or blinking = 3
     *
     * @param view
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void defineVisualEffects(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.scroll_radioButton:
                if (checked) {
                    configMessage.textFormatCode = 1; // scroll text

                    mScrollSpeedSeekBar.setEnabled(true);

                    mBlinkIntervalSeekBar.setEnabled(false);

                    mDurationTextSeekBar.setEnabled(false);

                }
                break;
            case R.id.static_radioButton:
                if (checked) {
                    configMessage.textFormatCode = 3; // static text

                    configMessage.flashPeriod = 0; //this guarantee it will not blink

                    mScrollSpeedSeekBar.setEnabled(false);

                    mBlinkIntervalSeekBar.setEnabled(false);

                    mDurationTextSeekBar.setEnabled(true);

                }
                break;
            case R.id.blink_radioButton:
                if (checked) {
                    configMessage.textFormatCode = 2; // blinking text

                    mScrollSpeedSeekBar.setEnabled(false);

                    mBlinkIntervalSeekBar.setEnabled(true);

                    mDurationTextSeekBar.setEnabled(true);

                }
                break;
        }
    }


    /**
     * sendMessageToPanel method
     * It is called when the button Enviar is pressed, it then sends the configuration command
     * to the panel.
     *
     * @param view
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void sendMessageToPanel(View view) {
        if (mMessagePanel.getText().toString().length() == 0){
            configMessage.messageAsciiFormat = " ";
        }else{
            configMessage.messageAsciiFormat = mMessagePanel.getText().toString();
        }


        //configMessage.messageAsciiFormat = mMessagePanel.getText().toString();
        configMessage.textLength = (byte) configMessage.messageAsciiFormat.length();
        //String hexStr = configMessage.assembleMessageSequence();

        if (mServer != null) {
            String hexStr = configMessage.assembleMessageSequence();
            mServer.sendData(hexStr);

        }


    }


} //end of CreateMessageActivity


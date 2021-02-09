package com.example.painelbt;


import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class FormatMsg {
    public byte[] messageHexFormat = new byte[55];  //3 control byte and 50 byte load
    public String messageAsciiFormat;     //message typed by user in the EditText
    public String messageAsciiBluetooth;  //message to be sent via bluetooth
    private byte btOffSetCtrl = 3; //due to inclusion of 3 control byte in the bluetooth message.


    public byte textSize;    //0 = small font and 1 = big font
    public byte letterFont;  //future use
    public byte panelOrder;  //future use
    public byte scrollSpeed;
    public byte textFormatCode;
    public byte flashPeriod;
    public byte textDuration;
    public int color;
    public byte textLength;

    public byte bmpImageIndex;
    public byte bmpImageColor;

    public int tmpTotalMessageLength;

    //constructor for
    public FormatMsg(){
        initializeMessageHexFormat();
        this.messageAsciiFormat = "Painel MAK";
        this.messageAsciiBluetooth = "";
        this.textLength = (byte)this.messageAsciiFormat.length();
        this.textSize  = 1;
        this.letterFont = 0;
        this.panelOrder = (byte)0xFF;
        this.scrollSpeed = 1;
        this.textFormatCode = 1;
        this.flashPeriod =2;
        this.textDuration = 5;
        this.color = 0xF0F0F0;
        this.bmpImageIndex = 0x1F;  //sem imagem configurada
    }

    public byte getBtOffSetCtrl(){
        return btOffSetCtrl;
    }

    private void initializeMessageHexFormat(){
        for (int i = 0; i < 52; i++){
            messageHexFormat[i] = (byte) 0x00;
        }
    }




    /**
     * asciiToHex method
     * Convert String (ASCII) -> String (hex)
     * It convert an String in ASCII format "ABCDEFGHI..." to its hexadecimal representation
     * @param  asciiStr
     * @return String output
     * e.g. input  "Marcelo"
     *      output "4D 61 72 63 65 6C 6F"
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String asciiToHex(String asciiStr) {
        byte[] byteRep = asciiStr.getBytes(StandardCharsets.ISO_8859_1);
        String hex = String.format("%02X",new BigInteger(1,byteRep));
        return hex;
    }

//    public String asciiToHex(String asciiStr) {
//        char[] chars = asciiStr.toCharArray();
//        StringBuilder hex = new StringBuilder();
//        for (char ch : chars) {
//            hex.append(Integer.toHexString((int) ch));
//        }
//        return hex.toString();
//    }







    /**
     * hexToAscii method
     * Convert String (hex) -> String (ASCII)
     * It convert an String in hex format "0123456789abcdef" using ASCII encoding
     * @param  hexStr String
     * @return  output String
     * e.g. input  "4D 61 72 63 65 6C 6F"
     *      output "Marcelo"
     */
    public  String hexToAscii(String hexStr) {
        StringBuilder output = new StringBuilder("");

        if((hexStr.length()%2 ==0)) {

            for (int i = 0; i < hexStr.length(); i += 2) {
                String str = hexStr.substring(i, i + 2);
                output.append((char) Integer.parseInt(str, 16));
            }
        }
        return output.toString();
    }

    /**
     * byteToHex method
     * Convert byte (hex) -> String (hex)
     * It convert an byte, get it representation in hexadecimal and then convert, to its
     * representation in hexadecimal but in a string.
     * @param  num
     * @return string
     * e.g. input byte = 255 (decimal) or 0xFF (hexadecimal)
     *      output     = "FF"
     */
    public String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }

    /**
     * byteToHex method
     * Convert byte array (hex) -> String of hex (hex)
     * It convert an byte, get it representation in hexadecimal and then convert, to its
     * representation in hexadecimal but in a string.
     * @param  num
     * @return string
     * e.g. input byte array = {255, 255 }(decimal)
     *      output           = "FFFF"
     */
    public String encodeHexString(byte[] byteArray) {
        StringBuffer hexStringBuffer = new StringBuffer();
        for (int i = 0; i < byteArray.length; i++) {
            hexStringBuffer.append(byteToHex(byteArray[i]));
        }
        return hexStringBuffer.toString();
    }







    /**
     * hexToByte method
     * Convert String(hex) -> byte(hex) (it means decimal too)
     * @param hexString
     * @return byte
     * e.g. input String = "FF"
     *      output byte = 255
     */
    public byte hexToByte(String hexString) {
        int firstDigit = toDigit(hexString.charAt(0));
        int secondDigit = toDigit(hexString.charAt(1));
        return (byte) ((firstDigit << 4) + secondDigit);
    }

    //helper method only used in hexToByte()
    private int toDigit(char hexChar) {
        int digit = Character.digit(hexChar, 16);
        if(digit == -1) {
            throw new IllegalArgumentException(
                    "Invalid Hexadecimal Character: "+ hexChar);
        }
        return digit;
    }

    public byte[] decodeHexString(String hexString) {
        if (hexString.length() % 2 == 1) {
            throw new IllegalArgumentException(
                    "Invalid hexadecimal String supplied.");
        }

        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length(); i += 2) {
            bytes[i / 2] = hexToByte(hexString.substring(i, i + 2));
        }
        return bytes;
    }


    /**
     * assembleMessageSequence() method
     * It assembly the parameters to configure a new message to be displayed in the panel.
     * @return String in the format to be sent throught the bluetooth interface.
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String assembleMessageSequence(){
        messageHexFormat[0] = (byte) 0x0D;   //insert message in the panel
        messageHexFormat[1] = (byte) 0xFF;   //insert at last position of panel
        //messageHexFormat[1] = (byte) 0x02;   //insert at last position of panel

        messageHexFormat[2] = (byte) 0x00;   //sent to Panel but not used now

        messageHexFormat[btOffSetCtrl]    = (byte) ((messageHexFormat[btOffSetCtrl] & 0) |
                                                    (((textSize << 5) & 0x20) |
                                                     ((letterFont << 6) & 0xC0) |
                                                      (bmpImageColor & 0x1f)));
        messageHexFormat[btOffSetCtrl +1] = (byte) ((messageHexFormat[btOffSetCtrl + 1] & 0) |
                                                    ((scrollSpeed << 5) & 0xE0) |
                                                     (textFormatCode    & 0x1F));
        messageHexFormat[btOffSetCtrl +2] = (byte) ((messageHexFormat[btOffSetCtrl + 2] & 0)  |
                                                    (((flashPeriod) << 5) & 0xE0) |
                                                     ((textDuration) & 0x1F));
        //messageHexFormat[btOffSetCtrl +3] = (byte) ((messageHexFormat[btOffSetCtrl + 3] & 0)) ;
        messageHexFormat[btOffSetCtrl +3] = (byte) ((messageHexFormat[btOffSetCtrl + 3] & 0) |
                                                     (bmpImageIndex)) ;

        messageHexFormat[btOffSetCtrl +4] = (byte) ((messageHexFormat[btOffSetCtrl + 4] & 0) |
                                                    (color >> 16));  //red color
        messageHexFormat[btOffSetCtrl +5] = (byte) ((messageHexFormat[btOffSetCtrl + 5] & 0) |
                                                    (color >> 8));   //green color
        messageHexFormat[btOffSetCtrl +6] = (byte) ((messageHexFormat[btOffSetCtrl + 6] & 0) |
                                                    (color));        //blue color
        messageHexFormat[btOffSetCtrl +7] = (byte) ((messageHexFormat[btOffSetCtrl + 7] & 0) |
                                                    (textLength));   //blue color

        messageHexFormat[btOffSetCtrl +48] = (byte) 0xFF;
        messageHexFormat[btOffSetCtrl +49] = (byte) 0xFF;

        //convert byte[](hex) to String. e.g. {0x4F, 0x6C, 0xE1} => "4F6CE1" string
        String supportConvertion0 = encodeHexString(messageHexFormat);

        //convert String(Ascii) to String(hex). e.g. "Olá" => "4F6CE1"
        String userMessageHex = asciiToHex(messageAsciiFormat);

        Log.d("DEBUG ctrlAddMess",supportConvertion0);
        Log.d("DEBUG messageAscii",messageAsciiFormat);
        Log.d("DEBUG messageHex", userMessageHex);

        //insert user's typed message in the control array to be sent to panel
        int startIndex = (btOffSetCtrl + 8)*2;  //8 is size of control bytes
        int stopIndex = startIndex + 40*2;            //40 is the size of message show on panel
        StringBuilder builder = new StringBuilder(supportConvertion0);
        builder.replace(startIndex, stopIndex, userMessageHex);            //user message inserted

        messageAsciiBluetooth = hexToAscii(builder.toString());
        return messageAsciiBluetooth;
    }

//    /**
//     * encodeUsingDataTypeConverter method
//     * byte(hex) convert to String(hex values)
//     * @param bytes
//     * @return
//     * e.g. in byte = {0x4F, 0x6C, (byte) 0xE1};
//     *      out String = "4F6CE1"
//     */
//    public String encodeUsingDataTypeConverter(byte[] bytes) {
//        return DatatypeConverter.printHexBinary(bytes);
//    }



//    /**
//     * decodeUsingDataTypeConverter method
//     * convert to String(hex values) to byte(hex)
//     * @param
//     * @return
//     * e.g. out String = "4F6CE1"
//     *      in byte = {0x4F, 0x6C, 0xE1};
//     *
//     */
//    public byte[] decodeUsingDataTypeConverter(String hexString) {
//        return DatatypeConverter.parseHexBinary(hexString);
//    }




}

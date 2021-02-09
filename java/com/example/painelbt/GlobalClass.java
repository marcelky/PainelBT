package com.example.painelbt;
import android.app.Application;

/**
 *  This GlobalClass allow to get/set variables as if globals, which in this way
 *  is available along entire life of application and also available across all activities.
 */
public class GlobalClass extends Application {

    private String btStatus;

    private String streamReadPanel;


    public String get_btStatus(){
        return btStatus;
    }

    public void set_btStatus(String status){
        btStatus = status;
    }

    public String getStreamReadPanel(){ return streamReadPanel; }

    public void concatStreamReadPanel(String incStream){
        streamReadPanel = streamReadPanel + incStream;
    }

    public void initStreamReadPanel() { streamReadPanel = "";}

}

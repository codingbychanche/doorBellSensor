package com.berthold.doorbellsensor.main;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bluetoothconnector.DecodedSensorData;

public class MainViewModel extends ViewModel {

    // Messages
    public static final int MESSAGE_HOME = 0;
    public static final int MESSAGE_OUT = 1;
    public static final int MESSAGE_DO_NOT_DISTURB = 2;

    /**
     * Live data.
     *
     */
     public MutableLiveData<String> btStatusMessage;
     public MutableLiveData<String> getBtStatusMessage(){
     if (btStatusMessage==null)
     btStatusMessage=new MutableLiveData<String>();
     return btStatusMessage;
     }

    public MutableLiveData<String> btErrorMessage;
    public MutableLiveData<String> getBtErrorMessage(){
        if (btErrorMessage==null)
            btErrorMessage=new MutableLiveData<String>();
        return btErrorMessage;
    }

    public MutableLiveData<String> btSucessMessage;
    public MutableLiveData<String> getbtSucessMessage() {
        if (btSucessMessage == null)
            btSucessMessage = new MutableLiveData<String>();
        return btSucessMessage;
    }

    public MutableLiveData<DecodedSensorData> btReceivedData;
    public MutableLiveData<DecodedSensorData> getbtReceivedData() {
        if (btReceivedData == null)
            btReceivedData = new MutableLiveData<DecodedSensorData>();
        return btReceivedData;
    }

    /**
     * @param messageNumber Ranging from 0 to n where 0 is the first message defined inside the array
     *                      of the string resources.
     * @return String containing the selected message.
     */
    public String messageLogic(int messageNumber) {

        String message;

        switch (messageNumber) {
            case MESSAGE_HOME:
                message = "Ich bin daheim";
                break;
            case MESSAGE_OUT:
                message = "Ich bin weg...";
                break;
            case MESSAGE_DO_NOT_DISTURB:
                message = "Ich bin daheim, aber beschaeftigt.....";
                break;
            default:
                message = "-";
        }
        return message;
    }
}
package com.berthold.doorbellsensor.main;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bluetoothconnector.DecodedSensorData;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainViewModel extends ViewModel {

    // Messages
    public static final int MESSAGE_HOME = 0;
    public static final int MESSAGE_OUT = 1;
    public static final int MESSAGE_DO_NOT_DISTURB = 2;

    /**
     * Live data.
     */
    public MutableLiveData<String> btStatusMessage;

    public MutableLiveData<String> getBtStatusMessage() {
        if (btStatusMessage == null)
            btStatusMessage = new MutableLiveData<String>();
        return btStatusMessage;
    }

    public MutableLiveData<String> btErrorMessage;

    public MutableLiveData<String> getBtErrorMessage() {
        if (btErrorMessage == null)
            btErrorMessage = new MutableLiveData<String>();
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

    private ArrayList<String>rangHistotyDataList;
    public MutableLiveData<ArrayList<String>> rangHistoryList;
    public MutableLiveData<ArrayList<String>> getRangHistoryList() {
        //
        // Check if the list contains any data, if so
        // just return it. If not, init it for the first time....
        //
        if (rangHistoryList == null) {
            //
            // Init list for the first time
            //
            Calendar calendar=Calendar.getInstance();
            String time=calendar.getTime().toString();

            rangHistotyDataList=new ArrayList<String>();
            rangHistotyDataList.add("Started at:"+time);
            rangHistoryList = new MutableLiveData<>();
            //
            // This is important, even if there is no active
            // observer, this has to be done to fill the
            // associated live data object with data!
            //
            // If you just want to return an empty list
            // (or any other object) skip this step!
            //
            rangHistoryList.setValue(rangHistotyDataList);
        }
        return rangHistoryList;
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
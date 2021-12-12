package com.berthold.doorbellsensor.main;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bluetoothconnector.BTConnectedInterface;
import com.example.bluetoothconnector.ConnectedThreadReadWriteData;

public class MainViewModel extends ViewModel {

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

    public MutableLiveData<String> btReceivedData;
    public MutableLiveData<String> getbtReceivedData() {
        if (btReceivedData == null)
            btReceivedData = new MutableLiveData<String>();
        return btReceivedData;
    }
}
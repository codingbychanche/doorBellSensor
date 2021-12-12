package com.example.bluetoothconnector;
/*
 * Created by Berthold Fritz
 *
 * This work is licensed under a Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License:
 * https://creativecommons.org/licenses/by-nc-sa/4.0/
 *
 *  Last modified 12/18/18 11:16 PM
 */

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebView;
import android.widget.TextView;

import java.io.IOException;
import java.util.UUID;

public class ConnectThread extends Thread {

    // Debug
    private String tag;

    // BT
    private BTConnectedInterface connectedInterface;
    private BluetoothSocket mSocket = null;
    private BluetoothDevice mDevice;
    private ConnectedThreadReadWriteData connectedThreadReadWriteData;

    // For the HC- 05 we have to use this UUID:
    private UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private String devName;

    private Context c;

    /**
     * Connect Thread
     * <p>
     * This try's to establish a connection to a BT device. The device must already have been bound.
     * If successful this thread starts the connected thread which receives data.
     */
    public ConnectThread(BluetoothDevice mDevice, Context c, BTConnectedInterface connectedInterface) {

        this.mDevice = mDevice;
        this.connectedInterface=connectedInterface;
        tag = getClass().getSimpleName();
        connectedInterface.receiveStatusMessage(tag+" Getting Socket");

        try {
            mSocket = mDevice.createRfcommSocketToServiceRecord(myUUID);
        } catch (IOException e) {
            connectedInterface.receiveErrorMessage(tag+" Could not get socket:"+e.toString());
        }
        this.c=c;
    }

    /*
     * Run!
     */
    public void run() {
        connectedThreadReadWriteData = null;
        try {
           connectedInterface.receiveStatusMessage  (tag+" Trying to connect to device.....");
            mSocket.connect();

            // This reads incoming data from the conneted device...
            connectedThreadReadWriteData = new ConnectedThreadReadWriteData(mSocket, connectedInterface);
            connectedThreadReadWriteData.start();

        } catch (IOException e) {
            connectedInterface.receiveErrorMessage(tag+ "\nKonnte Datenverbindung nicht herstellen. Grund:\n"+e.toString());
            if (connectedThreadReadWriteData != null) connectedThreadReadWriteData.cancel();
        }
        return;
    }

    /***
     * Cancel
     */
    public void cancel() {
       connectedThreadReadWriteData.cancel();
    }
}
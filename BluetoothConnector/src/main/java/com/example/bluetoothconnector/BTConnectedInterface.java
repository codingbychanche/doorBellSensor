package com.example.bluetoothconnector;

/**
 * Interface methods to communicated between app and
 * {@link ConnectThread} / {@link ConnectedThreadReadWriteData}.
 */
public interface BTConnectedInterface {
    void sucess (ConnectedThreadReadWriteData connectedThreadReadWriteData);
    void receiveDataFromBTDevice(String data);
    void receiveErrorMessage(String error);
    void receiveStatusMessage (String status);
}

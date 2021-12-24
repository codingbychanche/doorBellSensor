package com.example.bluetoothconnector;

/**
 * Interface methods to communicated between app and
 * {@link ConnectThread} / {@link ConnectedThreadReadWriteData}.
 */
public interface BTConnectedInterface {
    void sucess (ConnectedThreadReadWriteData connectedThreadReadWriteData);
    void receiveDataFromBTDevice(DecodedSensorData data);
    void receiveErrorMessage(String error);
    void receiveStatusMessage (String status);
}

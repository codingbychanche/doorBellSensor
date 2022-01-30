package com.example.bluetoothconnector;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Berthold on 1/10/19.
 *
 * Bluetooth ic_launcher sensor encodes data into a json structure.
 * This class decodes this data and returns it's contents.
 * If json could not be decoded, it returns an error.
 *
 * Results are written to:
 * @see DecodedSensorData
 *
 */

public class DecodeSensorData {

    public static DecodedSensorData decodeJson(String jsonRecieievedDataFromBluetoothDevice)
    {

        Log.v("JSON>>>>",jsonRecieievedDataFromBluetoothDevice);
        String tag=null;
        DecodedSensorData decodedSensorData=new DecodedSensorData();

        try {
            JSONObject jo = new JSONObject(jsonRecieievedDataFromBluetoothDevice);
            if (jo.has("firmware_version")) decodedSensorData.setFirmwareVersion(jo.getString("firmware_version"));
            if (jo.has("hardware_status")) decodedSensorData.setHardwareStatus(jo.getString("hardware_status"));
            if (jo.has("voltage_status")) decodedSensorData.setVoltageStatus(jo.getString("voltage_status"));
            if (jo.has("temperature_degrees")) decodedSensorData.setTempC(jo.getDouble("temperature_degrees"));
            if (jo.has("temperature_farenheit")) decodedSensorData.setTempF(jo.getDouble("temperature_farenheit"));
            if (jo.has("doorbell_rang")) decodedSensorData.setDoorbellRang(jo.getInt("doorbell_rang"));
            if (jo.has("on_to_off_state")) decodedSensorData.setOnToOffState(jo.getString("on_to_off_state"));
            if (jo.has("off_to_on_state")) decodedSensorData.setOnToOffState(jo.getString("off_to_on_state"));
            if (jo.has("sens_set_state")) decodedSensorData.setOnToOffState(jo.getString("sens_set_state"));
            if (jo.has("off_to_on_state")) decodedSensorData.setOnToOffState(jo.getString("off_to_on_state"));
            if (jo.has("sens_set_state")) decodedSensorData.setSensSetState(jo.getString("sens_set_state"));
            if (jo.has("sens_read")) decodedSensorData.setSensReadData(jo.getLong("sens_read"));
            if (jo.has("sens_set_to")) decodedSensorData.setSensSetTo(jo.getLong("sens_set_to"));
            decodedSensorData.declareDataAsComplete();
        }catch (JSONException e) {
            Log.v(tag,e.toString());
            Log.v("JSON::",e.toString());
            decodedSensorData.declareDataAsIncomplete();
        }
        return decodedSensorData;
    }
}

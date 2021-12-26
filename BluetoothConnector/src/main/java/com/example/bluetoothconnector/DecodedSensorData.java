package com.example.bluetoothconnector;

/**
 * Created by Berthold on 1/10/19.
 * <p>
 * Holds data send from bluetooth ic_launcher sensor.
 */

public class DecodedSensorData {
    private boolean dataIsIncomplete;
    private String firmwareVersion;
    private String hardwareStatus;
    private String voltageStatus;
    private double tempC;
    private double tempF;
    private int doorbellRang;
    private String onToOffState,offToOnState,sensSetState;
    private long sensReadData;

    private long sensSetTo;

    public boolean dataIsIncomplete() {
        return dataIsIncomplete;
    }

    public void declareDataAsIncomplete() {
        this.dataIsIncomplete =true;
    }
    public void declareDataAsComplete(){this.dataIsIncomplete=false;}

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getHardwareStatus() {
        return hardwareStatus;
    }

    public void setHardwareStatus(String hardwareStatus) {
        this.hardwareStatus = hardwareStatus;
    }

    public String getVoltageStatus() {
        return voltageStatus;
    }

    public void setVoltageStatus(String voltageStatus) {
        this.voltageStatus = voltageStatus;
    }

    public double getTempC() {
        return tempC;
    }

    public void setTempC(double tempC) {
        this.tempC = tempC;
    }

    public double getTempF() {
        return tempF;
    }

    public void setTempF(double tempF) {
        this.tempF = tempF;
    }

    public int getDoorbellRang() {
        return doorbellRang;
    }

    public void setDoorbellRang(int doorbellRang) {
        this.doorbellRang = doorbellRang;
    }

    public String getOnToOffState() {
        return onToOffState;
    }

    public void setOnToOffState(String onToOffState) {
        this.onToOffState = onToOffState;
    }

    public String getOffToOnState() {
        return offToOnState;
    }

    public void setOffToOnState(String offToOnState) {
        this.offToOnState = offToOnState;
    }

    public String getSensSetState() {
        return sensSetState;
    }

    public void setSensSetState(String sensSetState) {
        this.sensSetState = sensSetState;
    }

    public long getSensReadData() {
        return sensReadData;
    }

    public void setSensReadData(long sensReadData) {
        this.sensReadData = sensReadData;
    }

    public long getSensSetTo() {
        return sensSetTo;
    }

    public void setSensSetTo(long sensSetTo) {
        this.sensSetTo = sensSetTo;
    }
}


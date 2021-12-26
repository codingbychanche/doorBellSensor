package com.example.bluetoothconnector;

/**
 * Connected Thread
 * <p>
 * When a bluetooth connection has been established, this thread
 * receives a data- stream from the device connected and invokes the
 * callback instance {@link com.example.bluetoothconnector.BTConnectedInterface}
 * to send the received data to the instance(s) implementing this interface.
 * <p>
 * The connected device is represented by it's socket. Socked is obtained and passed by:
 * {@link com.example.bluetoothconnector.ConnectThread}
 */

import android.bluetooth.BluetoothSocket;
import android.text.Html;
import android.text.Spanned;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ConnectedThreadReadWriteData extends Thread {

    // Debug
    private String tag = getClass().getSimpleName();

    // BT
    private final BluetoothSocket mSocket;
    private final InputStream mIs;
    private final OutputStream mOs;
    private float dataChunksActuallySend = 1;
    private float dataChunksArrived = 1;

    // UI
    private TextView connectionStatus;
    private TextView connectionStrength;
    private TextView connectionAnimation;
    private BTConnectedInterface connectedInterface;

    // Just for fun, a nice animation when device is exchanging data
    private int[] beatingHeart = {128150, 128151};
    private int animationPhase;

    /**
     * Connected thread
     *
     * @param socket
     */
    public ConnectedThreadReadWriteData(BluetoothSocket socket, BTConnectedInterface connectedInterface) {
        mSocket = socket;
        this.connectionStatus = connectionStatus;
        this.connectionStrength = connectionStrength;
        this.connectionAnimation = connectionAnimation;
        this.connectedInterface = connectedInterface;

        InputStream tmpIs = null;
        OutputStream tmpOs = null;

        try {
            tmpIs = mSocket.getInputStream();
            tmpOs = mSocket.getOutputStream();
        } catch (IOException e) {
            connectedInterface.receiveErrorMessage(tag + " Data link broken. Reason:" + e.toString());
        }
        mOs = tmpOs;
        mIs = tmpIs;
    }

    /**
     * Read incoming
     */
    public void run() {
        // Read Data
        byte[] inBuffer = new byte[2024];

        connectedInterface.sucess(this);

        int length;

        try {
            while (true) {
                String receivedJsonData = null;
                int readBytes = 0;
                length = mIs.read(inBuffer);
                while (readBytes != length) {
                    receivedJsonData = new String(inBuffer, 0, length);
                    readBytes++;
                }

                // Animate to show, connection is alive!
                animate();

                // Wait before next data chunk arrives.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }

                // Animate to show, connection is alive!
                animate();

                // This part is only for the animation.....
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }

                // OK update display
                dataChunksActuallySend++;
                connectedInterface.receiveDataFromBTDevice(DecodeSensorData.decodeJson(receivedJsonData));
            }

        } catch (IOException e) {
            connectedInterface.receiveErrorMessage(tag + " Connection was interrupted. Reason:" + e.toString());
        }
    }

    /**
     * Send something to the device
     */
    public void send(String dataToSend) {
        try {
            mOs.write(dataToSend.getBytes());
        } catch (IOException e) {
            connectedInterface.receiveErrorMessage(tag + " Could not send....Reason:" + e.toString());
        }
    }

    /**
     * This show a animation in order to give a positive feedback to the user....
     */
    private void animate() {

        String html = "&#" + beatingHeart[animationPhase];

        Spanned htmlText = Html.fromHtml(html);
        //connectedInterface.receiveStatusMessage("" + htmlText);
        if (animationPhase < 1)
            animationPhase++;
        else
            animationPhase = 0;
    }

    /**
     * Closes the connection...
     */
    public void cancel() {
        try {
            if (mIs != null)
                mIs.close();
            if (mOs != null)
                mOs.close();
            if (mSocket != null)
                mSocket.close();

        } catch (IOException e) {
            connectedInterface.receiveErrorMessage(tag + " Error while closing connection. Reason:" + e.toString());
        } finally {
            this.interrupt();
        }
    }
}

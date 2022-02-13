package com.berthold.doorbellsensor.main;
/**
 * This code is executed in background as long as the app is not closed or destroyed.
 */

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;

public class JobServiceAlarmListenerForBTIncomming extends androidx.core.app.JobIntentService {

    static final int JOB_ID = 1000;

    private static boolean notCanceled;

    // Debug
    private String tag;

    // BT
    private static BluetoothSocket mSocket = null;
    private static InputStream mIs;

    // Control
    private Long timeJobWasStarted;
    private int lastState;
    private static final int WAS_ALARM = 1;
    private static final int WAS_NO_ALARM = 2;

    /**
     * This starts the job service (the code inside this classes 'onHandleWork()' method.
     * This is invoked by the {@link com.berthold.doorbellsensor.main.MainActivity} when a connection could be
     * established.
     *
     * @param context
     * @param alarmListenerForBTIncomming
     */
    static public  void doWork(Context context, InputStream tmpIs, Intent alarmListenerForBTIncomming) {
        notCanceled = true;
        mIs = tmpIs;
        enqueueWork(context, JobServiceAlarmListenerForBTIncomming.class, JOB_ID, alarmListenerForBTIncomming);
    }

    /**
     * This listens for incoming connections and starts the alarm...
     *
     * An alarm is detected whenever data is received.
     *
     * todo: Add alarm detection logic matching firmware.....
     *
     * @param intent
     */
    @Override
    protected void onHandleWork(@NonNull Intent intent) {

        lastState = WAS_NO_ALARM;
        timeJobWasStarted=System.currentTimeMillis();

        Intent thisJobIsAliveIntent=new Intent();
        thisJobIsAliveIntent.putExtra("AliveSince", timeJobWasStarted);
        thisJobIsAliveIntent.setAction("com.berthold.servicejobintentservice.ALARM_WATCHER_ALIVE");

        while (notCanceled) {
            Log.v("Logging", "-");
            //
            // Tell all receivers, that we are alive and kicking.....
            //
            sendBroadcast(thisJobIsAliveIntent);

            byte[] packetReceieved = new byte[1024];

            try {
                int bytesAvailable = mIs.available();
                if (bytesAvailable > 0) {
                    packetReceieved = new byte[bytesAvailable];
                    mIs.read(packetReceieved);
                    lastState = WAS_ALARM;
                }
                if (lastState == WAS_ALARM) {

                    String received = new String(packetReceieved, 0, bytesAvailable);
                    Log.v("RECEIVED_", received);

                    Date currentTime = Calendar.getInstance().getTime();

                    //
                    // Notify all receivers that we have an alarm.....
                    //
                    Intent alarmIntent = new Intent();
                    alarmIntent.putExtra("alarmReceived", "Time of Alarm:"+System.currentTimeMillis());
                    alarmIntent.setAction("com.berthold.servicejobintentservice.ALARM_INTENT");
                    sendBroadcast(alarmIntent);

                    //startAlarmActivity("Alarm"); // Option.....

                    lastState = WAS_NO_ALARM;
                }
            } catch (IOException e) {
                cancelThisJob();
            }

            // Just a little bit in order to evaluate correctly if there was an alarm or not..
            // The time span given in ms specifies the time which has to be elapsed before
            // a new alarm can be triggered....
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelThisJob();
    }

    static void cancelThisJob() {
        notCanceled = false;
    }

    /*
     * This starts the activity displaying that an alarm-
     * event was received.
     */
    private void startAlarmActivity(final String message) {


        // I commented that out because it is difficult to predict if
        // this activity is already open or not. This way the activity
        // will be opened all over again each time an alarm was triggered
        // Besides, the activity needed some logic to be able to decide if
        // it was started for the first time or because an alarm was triggered
        // and thus being able to record he event....

        Intent in = new Intent(this, MainActivity.class);
        this.startActivity(in);
    }
}

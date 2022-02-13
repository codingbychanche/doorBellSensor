package com.berthold.doorbellsensor.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.berthold.doorbellsensor.R;
import com.example.bluetoothconnector.BTConnectedInterface;
import com.example.bluetoothconnector.ConnectedThreadReadWriteData;
import com.example.bluetoothconnector.DecodedSensorData;

import org.w3c.dom.Text;

public class FragmentDoorBellWatcher extends Fragment  {

    // Debug
    private String tag;

    // View model
    private MainViewModel mainViewModel;

    // Doorbell count
    private int doorBellRangTimes = 0;

    public static FragmentDoorBellWatcher newInstance() {
        return new FragmentDoorBellWatcher();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_doorbell_watcher, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Debug
        tag = getClass().getSimpleName();
        Long time = System.currentTimeMillis();

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Broadcast receivers.
        //
        // This checks if the job service, checking for incoming alarms is still alive......
        //
        BroadcastReceiver isAlive = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Long timeServiceStarted=intent.getLongExtra("AliveSince",0);
                //mainViewModel.btStatusMessage.postValue(">>Job Service is alive "+timeServiceStarted);
            }
        };
        requireActivity().registerReceiver(isAlive, new IntentFilter("com.berthold.servicejobintentservice.ALARM_WATCHER_ALIVE"));

        //
        // This receiver is invoked when the associated job service detects an alarm.
        //
        BroadcastReceiver alarmReceived = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String timeOfAlarm=intent.getStringExtra("alarmReceived");

                Thread ring=new Thread(new Runnable() {
                    @Override
                    public void run() {

                        //
                        // Alarm....
                        //
                        for (int ringing=0;ringing<5;ringing++){

                            MediaPlayer mpPlayer = MediaPlayer.create(requireActivity(), R.raw.coin);
                            mpPlayer.start();

                            try {
                                Thread.sleep(50);
                            }catch (Exception e){}
                        }

                    }
                });
                ring.start();
            }
        };
        requireActivity().registerReceiver(alarmReceived, new IntentFilter("com.berthold.servicejobintentservice.ALARM_INTENT"));

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // View model and it's observers
        //
        mainViewModel = ViewModelProviders.of(requireActivity()).get(MainViewModel.class);

        //
        // Device was connected successfully....
        //
        final Observer<String> btConnectionSuccess = new Observer<String>() {
            @Override
            public void onChanged(String s) {
                ImageView btSucessIcon = view.findViewById(R.id.connection_status_icon);
                btSucessIcon.setVisibility(View.VISIBLE);
                Log.v("SUCESS_",s);
            }
        };
        mainViewModel.getbtSucessMessage().observe(getActivity(), btConnectionSuccess);

        //
        // BT- receive data from device....
        //
        final Observer<DecodedSensorData> btReceiveDataObserver = new Observer<DecodedSensorData>() {
            @Override
            public void onChanged(@Nullable final DecodedSensorData received) {
                long currentTime = System.currentTimeMillis();
                Log.v(tag, received.getDoorbellRang() + "");

                //
                // Check if we received data, check if is complete and if so
                // display the data received.....
                //
                if (received.dataIsIncomplete()) {
                    Log.v(tag, "Incomplete data received....");
                } else {
                    //
                    // Doorbell counter changed on connected device?
                    //
                    if (received.getDoorbellRang() > doorBellRangTimes) {

                        doorBellRangTimes = received.getDoorbellRang();
                    }

                    if (received.getDoorbellRang()==0) {
                        doorBellRangTimes = received.getDoorbellRang();
                    }

                    TextView dataView = view.findViewById(R.id.message);
                    dataView.setText(received.getDoorbellRang() + "");

                    //
                    // Display other data....
                    //
                    TextView lockView = view.findViewById(R.id.bat_state);
                    lockView.setText(received.getVoltageStatus() + "");

                    TextView isSetView = view.findViewById(R.id.is_set_state);
                    isSetView.setText(received.getSensSetTo() + "");

                    TextView tempView = view.findViewById(R.id.temperature);
                    tempView.setText(received.getTempC() + "");

                    Log.v(tag, "Received:" + received.getDoorbellRang() + "   " + received.getSensSetTo());
                }
            }
        };
        mainViewModel.getbtReceivedData().observe(getActivity(), btReceiveDataObserver);
    }
}
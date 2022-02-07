package com.berthold.doorbellsensor.main;

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

public class FragmentDoorBellWatcher extends Fragment implements BTConnectedInterface {

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

                        // OK, inform user, doorbell has rang......
                        MediaPlayer mpPlayer = MediaPlayer.create(requireActivity(), R.raw.coin);
                        mpPlayer.start();

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

    @Override
    public void sucess(ConnectedThreadReadWriteData connectedThreadReadWriteData) {

    }

    @Override
    public void receiveDataFromBTDevice(DecodedSensorData d) {
        mainViewModel.btReceivedData.postValue(d);
        Log.v("INTERFACETEST", "Got data");
    }

    /**
     * Callback receive status message from connected device.
     */
    @Override
    public void receiveStatusMessage(String status) {
        Log.v(tag, status);
        mainViewModel.btStatusMessage.postValue(status);
    }

    /**
     * Receive error message
     */
    @Override
    public void receiveErrorMessage(String error) {
        Log.v(tag, error);
        mainViewModel.btErrorMessage.postValue(error);
    }
}
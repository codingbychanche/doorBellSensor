package com.berthold.doorbellsensor.main;

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
import android.widget.TextView;

import com.berthold.doorbellsensor.R;
import com.example.bluetoothconnector.DecodedSensorData;

public class FragmentDoorBellWatcher extends Fragment {

    // Debug
    private String tag;

    // View model
    private MainViewModel mainViewModel;


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

        // UI
        TextView dataView=view.findViewById(R.id.message);
        dataView.setText("Hi");

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // View model and it's observers
        //
        mainViewModel= ViewModelProviders.of(requireActivity()).get(MainViewModel.class);

        //
        // BT- receive data
        //
        final Observer<DecodedSensorData> btReceiveDataObserver = new Observer<DecodedSensorData>() {
            @Override
            public void onChanged(@Nullable final DecodedSensorData received) {
                long currentTime = System.currentTimeMillis();
                //connectionHistoryView.append(currentTime + ">>Received:" + received + "\n");
                Log.v(tag,received.getDoorbellRang()+"");
                if (received.dataIsIncomplete()) {
                    Log.v(tag,"Incomplete data received....");
                }else
                    dataView.setText(received.getDoorbellRang());
            }
        };
        mainViewModel.getbtReceivedData().observe(getActivity(), btReceiveDataObserver);
    }
}
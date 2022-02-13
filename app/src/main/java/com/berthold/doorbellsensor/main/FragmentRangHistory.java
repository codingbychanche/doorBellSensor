package com.berthold.doorbellsensor.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.berthold.doorbellsensor.R;
import com.example.bluetoothconnector.DecodedSensorData;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class FragmentRangHistory extends Fragment {

    // Debug
    String tag;

    public static FragmentRangHistory newInstance() {
        return new FragmentRangHistory();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_rang_history, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Debug
        tag = getClass().getSimpleName();
        Long time = System.currentTimeMillis();

        ////////////////////////////////////////////////////////////////////////////////////////////
        // View model and it's observers
        //
        MainViewModel mainViewModel= ViewModelProviders.of(requireActivity()).get(MainViewModel.class);

        //
        // Create custom list adapter for our list showing the rang history
        //
        ListView rangHistoryListView=(ListView)view.findViewById(R.id.rang_history_list);
        ArrayList<String> rangHistoryList=mainViewModel.getRangHistoryList().getValue();
        ArrayAdapter<String> rangHistoryListAdapter =
                new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1, rangHistoryList);
        rangHistoryListView.setAdapter(rangHistoryListAdapter);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Broadcast receivers.
        //
        // This receiver is invoked when the associated job service detects an alarm.
        //
        BroadcastReceiver r = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String timeOfAlarm=intent.getStringExtra("alarmReceived");

                Calendar calendar=Calendar.getInstance();
                String time=calendar.getTime().toString();

                rangHistoryList.add(0,time);
                mainViewModel.rangHistoryList.postValue(rangHistoryList);
                rangHistoryListAdapter.notifyDataSetChanged();
            }
        };
        requireActivity().registerReceiver(r, new IntentFilter("com.berthold.servicejobintentservice.ALARM_INTENT"));
    }
}
package com.berthold.doorbellsensor.main;

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
        Log.v("LISTLIST",rangHistoryListView+"");
        ArrayList<String> rangHistoryList=mainViewModel.getRangHistoryList().getValue();
        Log.v("LISTLIST",rangHistoryList+"");
        ArrayAdapter<String> rangHistoryListAdapter =
                new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1, rangHistoryList);
        rangHistoryListView.setAdapter(rangHistoryListAdapter);

        //
        // BT- receive data from device....
        //
        final Observer<DecodedSensorData> btReceiveDataObserver = new Observer<DecodedSensorData>() {
            @Override
            public void onChanged(@Nullable final DecodedSensorData received) {
                long currentTime = System.currentTimeMillis();
                Log.v(tag,received.getDoorbellRang()+"");

            }
        };
        mainViewModel.getbtReceivedData().observe(getActivity(), btReceiveDataObserver);


    }


}
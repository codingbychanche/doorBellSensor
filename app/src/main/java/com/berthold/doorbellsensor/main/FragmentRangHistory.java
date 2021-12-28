package com.berthold.doorbellsensor.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.berthold.doorbellsensor.R;

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

        return inflater.inflate(R.layout.fragment_doorbell_watcher, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Debug
        tag = getClass().getSimpleName();
        Long time = System.currentTimeMillis();

        // UI
    }

}
package com.Joseph.agroshieldapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.Joseph.agroshieldapp.R;

public class DiagnoseFragment extends Fragment {

    public DiagnoseFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment (create fragment_diagnose.xml)
        return inflater.inflate(R.layout.activity_diagnose_fragmen, container, false);
    }
}

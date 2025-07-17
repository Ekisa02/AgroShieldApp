package com.Joseph.agroshieldapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class WeatherFragment extends Fragment {

    private TextView userFullName;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    public WeatherFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_weather_fragment, container, false);

        userFullName = view.findViewById(R.id.userFullName);
        mAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference("Users"); // Use correct case

        String uid = mAuth.getCurrentUser().getUid();

        userRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    userFullName.setText(name != null ? name : "Name not found");
                } else {
                    userFullName.setText("User not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                userFullName.setText("Error loading name");
            }
        });

        return view;
    }
}

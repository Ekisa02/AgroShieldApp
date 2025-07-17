package com.Joseph.agroshieldapp;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class HomeTabAdapter extends FragmentStateAdapter {
    public HomeTabAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new WeatherFragment();
            case 1: return new DiagnoseFragment();
            case 2: return new HistoryFragment();
            default: return new WeatherFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}

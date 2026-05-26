package com.example.myapplication.screens;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.menu.Menuable;

public class VictimHomeFragment extends Fragment {

    private Menuable menuable;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Menuable) {
            menuable = (Menuable) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        menuable = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_victim_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.victim_sos_button).setOnClickListener(click -> openMenuIndex(2));
        view.findViewById(R.id.victim_bilan_button).setOnClickListener(click -> openMenuIndex(1));
    }

    private void openMenuIndex(int index) {
        if (menuable != null) {
            menuable.onMenuChange(index);
        }
    }
}

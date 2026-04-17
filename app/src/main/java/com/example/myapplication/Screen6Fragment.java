package com.example.myapplication;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class Screen6Fragment extends Fragment {
    private Notifiable notifiable;
    private final int NUM_FRAGMENT = 1;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (requireActivity() instanceof Notifiable) {
            notifiable = (Notifiable) requireActivity();
        }
        else {
            throw new AssertionError("L'activité doit implémenter Notifiable !");
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_screen6, container, false);    // Inflate the layout for this fragment

        return view;
    }

}
package com.example.myapplication;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class Screen1Fragment extends Fragment {
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
        View view =  inflater.inflate(R.layout.fragment_screen1, container, false);    // Inflate the layout for this fragment
        TextView messageHolder = view.findViewById(R.id.message_from_list);

        // recuperer le message envoyé par la list (Adapter)
        Bundle args = getArguments();
        if (args != null) {
            String message = args.getString("message");
            messageHolder.setText(message);
        }

        view.findViewById(R.id.button).setOnClickListener(v -> {       //on prévient l'activité
            notifiable.onClick(NUM_FRAGMENT);
        });

        return view;
    }

}
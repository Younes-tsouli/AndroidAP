package com.example.myapplication;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class Screen3Fragment extends Fragment {
    private Notifiable notifiable;
    private AccidentFactory accidentFactory;

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
        View view =  inflater.inflate(R.layout.fragment_screen3, container, false);    // Inflate the layout for this fragment

        MaterialButtonToggleGroup toggleGroup = view.findViewById(R.id.toggleGroup);
        TextInputLayout layoutTitle = view.findViewById(R.id.layoutTitre);
        TextInputLayout layoutDesc = view.findViewById(R.id.layoutDesc);
        Button validationButton = view.findViewById(R.id.btnEnvoyer);

        validationButton.setOnClickListener(c -> {
            if (toggleGroup.getCheckedButtonId() == R.id.btnUrbain) {
                accidentFactory = new UrbanFactory();
            } else {
                accidentFactory = new HighwayFactory();
            }
            // creer l'incident
            String titre = layoutTitle.getEditText().getText().toString();
            String description = layoutDesc.getEditText().getText().toString();

            Issue issue = accidentFactory.createIssue(titre, description);
            notifiable.onDataChange(3, issue, 0, issue.getSafetyProtocol());
        });

        return view;
    }

}
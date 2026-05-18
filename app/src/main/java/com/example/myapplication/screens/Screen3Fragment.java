package com.example.myapplication.screens;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.myapplication.issue.AccidentFactory;
import com.example.myapplication.factories.HighwayFactory;
import com.example.myapplication.issue.Issue;
import com.example.myapplication.issue.IssueRepository;
import com.example.myapplication.Notifiable;
import com.example.myapplication.R;
import com.example.myapplication.factories.UrbanFactory;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputLayout;

public class Screen3Fragment extends Fragment {
    private Notifiable notifiable;
    private AccidentFactory accidentFactory;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Notifiable) {
            notifiable = (Notifiable) context;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_screen3, container, false);

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
            
            String titre = layoutTitle.getEditText().getText().toString();
            String description = layoutDesc.getEditText().getText().toString();

            // 1. Création via la Factory (qui attache l'EmergencyService)
            Issue issue = accidentFactory.createIssue(titre, description);
            
            // 2. Sauvegarde dans le dépôt central (Singleton)
            IssueRepository.getInstance().addIssue(issue);
            
            Toast.makeText(getContext(), "Incident signalé !", Toast.LENGTH_SHORT).show();

            // 3. Notification à l'activité pour naviguer vers la liste (index 1)
            notifiable.onDataChange(3, issue, 0, "Navigation vers la liste");
        });

        return view;
    }
}

package com.example.myapplication;


import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class Screen2Fragment extends Fragment implements ClickableIssue<Issue> {

    private Notifiable notifiable;
    private List<Issue> myIssues;
    private IssueAdapter adapter;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // On vérifie que l'activité est bien "Notifiable" (le chef d'orchestre)
        if (context instanceof Notifiable) {
            notifiable = (Notifiable) context;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_screen2, container, false);

        // 1. On prépare les données (plus tard, elles pourraient venir d'un Singleton)
        myIssues = new ArrayList<>();
        myIssues.add(new Issue("Fuite d'eau", "Salle 202 - Inondation", android.R.drawable.ic_dialog_alert, 2.0f));
        myIssues.add(new Issue("Panne Réseau", "Plus d'internet au 1er", android.R.drawable.ic_dialog_info, 4.0f));
        myIssues.add(new Issue("Porte bloquée", "Accès parking impossible", android.R.drawable.ic_lock_lock, 1.0f));

        // 2. On récupère la ListView
        ListView listView = view.findViewById(R.id.my_list_view);

        // 3. On crée l'ADAPTER PERSONNALISÉ
        // On lui passe 'this' car notre Fragment implémente ClickableIssue
        adapter = new IssueAdapter(requireContext(), R.layout.item_issue, myIssues, this);
        // 4. On branche
        listView.setAdapter(adapter);

        return view;
    }

    // --- Implémentation de ClickableIssue (Le lien avec l'Adapter) ---

    @Override
    public void onClickItem(List<Issue> items, int itemIndex) {
        // L'Adapter nous dit qu'on a cliqué sur une ligne
        Issue selected = items.get(itemIndex);

        // On prévient l'activité d'afficher le détail (Screen1)
        // actionCode 1 pourrait signifier "Afficher détail"
        notifiable.onDataChange(2, selected, 1, null);
    }

    @Override
    public void onRatingBarChange(int itemIndex, float value, IssueAdapter adapter, List<Issue> items) {
        // L'Adapter nous dit qu'on a changé les étoiles
        Issue issue = items.get(itemIndex);
        issue.setStatus(value); // On met à jour l'objet localement

        // On prévient l'activité que les données ont changé
        // actionCode 2 pourrait signifier "Mise à jour statut"
        notifiable.onDataChange(2, issue, 2, value);

        Log.d("DEBUG", "Nouveau statut pour " + issue.getTitle() + " : " + value);
    }
}
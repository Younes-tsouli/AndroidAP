package com.example.myapplication;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.fragment.app.Fragment;


public class Screen2Fragment extends Fragment {

    // 1. On crée une variable pour stocker l'activité (le chef d'orchestre)
    private Notifiable listener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Notifiable) {
            // On enregistre l'activité dans notre variable 'listener'
            this.listener = (Notifiable) context;
        } else {
            throw new AssertionError("L'activité doit implémenter Notifiable !");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // 2. On gonfle le layout (on crée la vue)
        View view = inflater.inflate(R.layout.fragment_screen2, container, false);

        // 3. ON EST ICI ! C'est le moment de s'occuper de la liste

        // A. On récupère la ListView qui est dans ton fichier XML
        ListView myListView = view.findViewById(R.id.my_list_view);

        // B. On prépare nos données (la liste des incidents)
        String[] incidents = {"Fuite d'eau - Salle 202", "Panne réseau - Bureau 10", "Ampoule grillée - Couloir"};

        // C. On crée l'ADAPTER
        // On lui donne : le contexte, un look par défaut (simple_list_item_1), et nos données
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_list_item_1,
                incidents
        );

        // D. On branche l'Adapter à la ListView
        myListView.setAdapter(adapter);

        // E. On gère le clic sur un élément
        myListView.setOnItemClickListener((parent, viewItem, position, id) -> {
            // On récupère le texte de l'élément cliqué
            String selectedIncident = incidents[position];

            // On appelle la méthode de l'activité via notre interface !
            if (listener != null) {
                listener.onIncidentSelected(selectedIncident);
            }
        });

        return view;
    }
}
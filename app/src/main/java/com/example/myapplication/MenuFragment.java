package com.example.myapplication;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;


public class MenuFragment extends Fragment {
    private Menuable menuable;
    private int currentActivatedIndex = 0;
    private static final String ARG_INDEX = "index";

    private ImageView[] icons = new ImageView[7];   // Tableau des 6 ImageView (une par icône)

    public MenuFragment() {}

    @Override
    public void onAttach(Context context) {    // ① onAttach : vérifier que l'activité implémente Menuable
        super.onAttach(context);
        // on verifie que l'activité porteuse respecte le contrat
        if (requireActivity() instanceof Menuable) {
            menuable = (Menuable) requireActivity();
        }
        else {
            throw new AssertionError("L'activité doit implémenter Menuable !");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);   // Inflate the layout for this fragment

        // ② Récupérer les 6 ImageView
        icons[0] = view.findViewById(R.id.menu_component_0);
        icons[1] = view.findViewById(R.id.menu_component_1);
        icons[2] = view.findViewById(R.id.menu_component_2);
        icons[3] = view.findViewById(R.id.menu_component_3);
        icons[4] = view.findViewById(R.id.menu_component_4);
        icons[5] = view.findViewById(R.id.menu_component_5);
        icons[6] = view.findViewById(R.id.menu_component_6);

        // ③ Lire l'index initial envoyé par l'activité via Bundle
        if (getArguments() != null) {
            currentActivatedIndex = getArguments().getInt(ARG_INDEX, 0);
        }

        // ④ Afficher toutes les icônes (normale ou sélectionnée)
        rafraichirMenu(view);

        // ⑤ Brancher le clic sur chaque icône
        for (int i = 0; i < icons.length; i++) {
            final int index = i;
            icons[i].setOnClickListener(c -> {
                currentActivatedIndex = index;
                rafraichirMenu(view);
                menuable.onMenuChange(currentActivatedIndex);
            });
        }
        return view;
    }

    private void rafraichirMenu(View view) {
        for (int i=0; i<icons.length; i++) {
            String nomImage = "menu" + i + ((i == currentActivatedIndex) ? "_s" : "");

            int ressourceId = getResources().getIdentifier(
                    nomImage,
                    "drawable",
                    view.getContext().getPackageName());

            icons[i].setImageResource(ressourceId);
        }
    }
}
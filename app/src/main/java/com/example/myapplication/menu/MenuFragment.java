package com.example.myapplication.menu;

import android.content.Context;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.myapplication.R;

public class MenuFragment extends Fragment {

    private Menuable menuable;
    private int currentActivatedIndex = 0;
    private static final String ARG_INDEX = "index";

    private LinearLayout[] tabs = new LinearLayout[7];
    private ImageView[] icons = new ImageView[7];

    public MenuFragment() {}

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (requireActivity() instanceof Menuable) {
            menuable = (Menuable) requireActivity();
        } else {
            throw new AssertionError("L'activité doit implémenter Menuable !");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        // Récupérer les tabs (LinearLayout) et les icônes
        tabs[0] = view.findViewById(R.id.cmp0);
        tabs[1] = view.findViewById(R.id.cmp1);
        tabs[2] = view.findViewById(R.id.cmp2);
        tabs[3] = view.findViewById(R.id.cmp3);
        tabs[4] = view.findViewById(R.id.cmp4);
        tabs[5] = view.findViewById(R.id.cmp5);
        tabs[6] = view.findViewById(R.id.cmp6);

        icons[0] = view.findViewById(R.id.menu_component_0);
        icons[1] = view.findViewById(R.id.menu_component_1);
        icons[2] = view.findViewById(R.id.menu_component_2);
        icons[3] = view.findViewById(R.id.menu_component_3);
        icons[4] = view.findViewById(R.id.menu_component_4);
        icons[5] = view.findViewById(R.id.menu_component_5);
        icons[6] = view.findViewById(R.id.menu_component_6);

        // Lire l'index initial
        if (getArguments() != null) {
            currentActivatedIndex = getArguments().getInt(ARG_INDEX, 0);
        }

        rafraichirMenu();

        // Clic sur chaque tab
        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            tabs[i].setOnClickListener(v -> {
                currentActivatedIndex = index;
                rafraichirMenu();
                menuable.onMenuChange(currentActivatedIndex);
            });
        }

        return view;
    }

    private void rafraichirMenu() {
        for (int i = 0; i < tabs.length; i++) {
            boolean actif = (i == currentActivatedIndex);

            // Fond : rouge si actif, blanc sinon
            tabs[i].setBackgroundColor(actif ? 0xFFB71C1C : 0xFFFFFFFF);

            // Tint icône : blanc si actif, gris sinon
            icons[i].setColorFilter(actif ? 0xFFFFFFFF : 0xFF888888);

            // Couleur du label TextView (index 1 dans le LinearLayout)
            ((android.widget.TextView) tabs[i].getChildAt(1))
                    .setTextColor(actif ? 0xFFFFFFFF : 0xFF888888);
        }
    }

    public void setExternalIndex(int i) {
        currentActivatedIndex = i;
        if (getView() != null) {
            rafraichirMenu();
        }
    }
}
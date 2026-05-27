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
import android.widget.TextView;

import com.example.myapplication.ControlActivity;
import com.example.myapplication.R;

public class MenuFragment extends Fragment {

    private Menuable menuable;
    private int currentActivatedIndex = 0;
    private static final String ARG_INDEX = "index";
    private static final String ARG_ROLE = "role";
    private String role = ControlActivity.ROLE_VICTIM;

    private LinearLayout[] tabs = new LinearLayout[5];
    private ImageView[] icons = new ImageView[5];

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
        icons[0] = view.findViewById(R.id.menu_component_0);
        icons[1] = view.findViewById(R.id.menu_component_1);
        icons[2] = view.findViewById(R.id.menu_component_2);
        icons[3] = view.findViewById(R.id.menu_component_3);
        icons[4] = view.findViewById(R.id.menu_component_4);

        if (getArguments() != null) {
            currentActivatedIndex = getArguments().getInt(ARG_INDEX, 0);
            role = getArguments().getString(ARG_ROLE, ControlActivity.ROLE_VICTIM);
        }

        configureMenuForRole();
        if (!isVisibleIndex(currentActivatedIndex)) {
            currentActivatedIndex = ControlActivity.ROLE_RESCUE.equals(role) ? 1 : 0;
        }

        rafraichirMenu();

        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            tabs[i].setOnClickListener(v -> {
                if (!isVisibleIndex(index)) return;
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
            int activeColor = ContextCompat.getColor(requireContext(), R.color.emergency_red);
            int inactiveBackground = ContextCompat.getColor(requireContext(), R.color.app_surface);
            int activeContent = ContextCompat.getColor(requireContext(), R.color.app_on_primary);
            int inactiveContent = ContextCompat.getColor(requireContext(), R.color.nav_inactive);

            tabs[i].setBackgroundColor(actif ? activeColor : inactiveBackground);

            icons[i].setColorFilter(actif ? activeContent : inactiveContent);

            ((TextView) tabs[i].getChildAt(1)).setTextColor(actif ? activeContent : inactiveContent);
        }
    }

    public void setExternalIndex(int i) {
        if (!isVisibleIndex(i)) return;
        currentActivatedIndex = i;
        if (getView() != null) {
            rafraichirMenu();
        }
    }

    private void configureMenuForRole() {
        hideAllTabs();

        if (ControlActivity.ROLE_RESCUE.equals(role)) {
            configureTab(1, R.string.title_summary, R.drawable.ic_menu_clipboard);
            configureTab(3, R.string.menu_map, R.drawable.ic_menu_location);
            configureTab(4, R.string.menu_alerts, R.drawable.ic_menu_alert);
            return;
        }

        configureTab(0, R.string.menu_home, R.drawable.ic_header_home);
        configureTab(1, R.string.menu_signal, R.drawable.ic_menu_alert);
        configureTab(2, R.string.menu_track, R.drawable.ic_menu_clipboard);
        configureTab(3, R.string.menu_map, R.drawable.ic_menu_location);
    }

    private void hideAllTabs() {
        for (LinearLayout tab : tabs) {
            tab.setVisibility(View.GONE);
        }
    }

    private void configureTab(int index, int labelRes, int iconRes) {
        setTabVisible(index, true);
        setTabLabel(index, getString(labelRes));
        setTabIcon(index, iconRes);
    }

    private void setTabVisible(int index, boolean visible) {
        tabs[index].setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void setTabLabel(int index, String label) {
        ((TextView) tabs[index].getChildAt(1)).setText(label);
    }

    private void setTabIcon(int index, int iconRes) {
        icons[index].setImageResource(iconRes);
    }

    private boolean isVisibleIndex(int index) {
        return index >= 0 && index < tabs.length && tabs[index].getVisibility() == View.VISIBLE;
    }
}

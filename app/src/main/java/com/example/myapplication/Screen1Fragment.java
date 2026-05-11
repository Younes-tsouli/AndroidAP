package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class Screen1Fragment extends Fragment {

    private Issue currentIssue;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_screen1, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Récupération de l'incident via le Bundle
        Bundle args = getArguments();
        if (args != null) {
            currentIssue = args.getParcelable("my_incident");
        }

        if (currentIssue == null) return;

        // 2. Liaison avec les vues
        ImageView iconView = view.findViewById(R.id.detail_priority_icon);
        TextView titleView = view.findViewById(R.id.detail_title);
        TextView descView = view.findViewById(R.id.detail_description);
        RatingBar ratingBar = view.findViewById(R.id.detail_rating_status);
        TextView statusLabel = view.findViewById(R.id.detail_status_label);
        TextView safetyView = view.findViewById(R.id.detail_safety_protocol);

        // 3. Affichage des données
        iconView.setImageResource(currentIssue.getPriorityIcon());
        titleView.setText(currentIssue.getTitle());
        descView.setText(currentIssue.getDescription());
        ratingBar.setRating(currentIssue.getStatus().getRating());
        statusLabel.setText("Statut : " + currentIssue.getStatus().name());
        safetyView.setText("Protocole de sécurité :\n" + currentIssue.getSafetyProtocol());

        // 4. Gestion du changement de statut
        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            if (fromUser) {
                Status newStatus = Status.fromRating(rating);
                
                // On met à jour l'objet local
                currentIssue.setStatus(newStatus);
                statusLabel.setText("Statut : " + newStatus.name());

                // IMPORTANT : Puisque l'objet a été "parcelé", il a perdu ses observateurs.
                // On notifie manuellement le service d'urgence pour cette action.
                EmergencyService.getInstance().onStatusChanged(currentIssue);
                
                // Optionnel : On pourrait aussi mettre à jour le Repository si on voulait
                // que le changement persiste dans la liste au retour.
            }
        });
    }
}

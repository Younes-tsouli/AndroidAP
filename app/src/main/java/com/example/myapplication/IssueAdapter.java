package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import java.util.List;

public class IssueAdapter extends ArrayAdapter<Issue> {

    private int layoutResourceId;
    private List<Issue> data;
    private ClickableIssue<Issue> callback; // L'interface pour parler au Fragment

    public IssueAdapter(Context context, int layoutResourceId, List<Issue> data, ClickableIssue<Issue> callback) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.data = data;
        this.callback = callback;
    }

    // Le "ViewHolder" est une petite boîte qui tient les composants d'une ligne
    static class IssueHolder {
        ImageView imgPriority;
        TextView txtTitle;
        TextView txtDescription;
        RatingBar ratingStatus;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        IssueHolder holder;
        View row = convertView;

        // 1. Si la ligne n'existe pas encore, on la crée (Inflation)
        if (row == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new IssueHolder();
            holder.imgPriority = row.findViewById(R.id.item_priority_image);
            holder.txtTitle = row.findViewById(R.id.item_title);
            holder.txtDescription = row.findViewById(R.id.item_description);
            holder.ratingStatus = row.findViewById(R.id.item_status_rating);

            row.setTag(holder); // On range le holder dans la ligne
        } else {
            // Si elle existe, on la recycle (Gain de performance !)
            holder = (IssueHolder) row.getTag();
        }

        // 2. On récupère les données de l'incident actuel
        Issue issue = data.get(position);

        // 3. On remplit les composants avec les vraies infos
        holder.txtTitle.setText(issue.getTitle());
        holder.txtDescription.setText(issue.getDescription());
        holder.imgPriority.setImageResource(issue.getPriorityIcon());
        holder.ratingStatus.setRating(issue.getStatus());

        // 4. GESTION DES CLICS (Interactions demandées)

        // Clic sur toute la ligne -> Détails
        row.setOnClickListener(v -> {
            callback.onClickItem(data, position);
        });

        // Changement de la RatingBar -> Mise à jour du score
        holder.ratingStatus.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser) { // Seulement si c'est l'utilisateur qui a touché
                callback.onRatingBarChange(position, rating, this, data);
            }
        });

        return row;
    }
}
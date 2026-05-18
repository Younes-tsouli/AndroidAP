package com.example.myapplication.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.myapplication.issue.ClickableIssue;
import com.example.myapplication.issue.Issue;
import com.example.myapplication.issue.Status;
import com.example.myapplication.R;

import java.util.List;

public class IssueAdapter extends ArrayAdapter<Issue> {

    private final int layoutResourceId;
    private final List<Issue> data;
    private final ClickableIssue<Issue> callback;

    // Généré automatiquement depuis l'enum Status (ordre déclaration)
    private static final Status[] STATUS_VALUES = Status.values();

    public IssueAdapter(Context context, int layoutResourceId, List<Issue> data, ClickableIssue<Issue> callback) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.data = data;
        this.callback = callback;
    }

    static class IssueHolder {
        ImageView imgPriority;
        TextView  txtTitle;
        TextView  txtDescription;
        Spinner   spinnerStatus;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        IssueHolder holder;
        View row = convertView;

        if (row == null) {
            row = LayoutInflater.from(getContext()).inflate(layoutResourceId, parent, false);

            holder = new IssueHolder();
            holder.imgPriority    = row.findViewById(R.id.alert_icon);
            holder.txtTitle       = row.findViewById(R.id.alert_title);
            holder.txtDescription = row.findViewById(R.id.alert_subtitle);
            holder.spinnerStatus  = row.findViewById(R.id.alert_status_spinner);

            row.setTag(holder);
        } else {
            holder = (IssueHolder) row.getTag();
        }

        Issue issue = data.get(position);

        holder.txtTitle.setText(issue.getTitle());
        holder.txtDescription.setText(issue.getDescription());
        holder.imgPriority.setImageResource(issue.getPriorityIcon());

        // ── Spinner alimenté par Status.values() ──────────────────────────────
        holder.spinnerStatus.setTag(null);

        // Labels affichés = nom de chaque valeur de l'enum
        ArrayAdapter<Status> spinnerAdapter = new ArrayAdapter<Status>(
                getContext(),
                android.R.layout.simple_spinner_item,
                STATUS_VALUES
        ) {
            // Affiche le nom lisible dans la liste déroulante
            @Override
            public View getDropDownView(int pos, View recycled, ViewGroup p) {
                View v = super.getDropDownView(pos, recycled, p);
                ((TextView) v).setText(STATUS_VALUES[pos].name());
                return v;
            }
        };
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        holder.spinnerStatus.setAdapter(spinnerAdapter);

        // Sélection initiale : index de l'enum correspondant au statut de l'issue
        holder.spinnerStatus.setSelection(issue.getStatus().ordinal(), false);
        holder.spinnerStatus.setTag(position);

        // Clic ligne → détails
        row.setOnClickListener(v -> callback.onClickItem(data, position));

        // Changement Spinner → on reuse onRatingBarChange avec getRating() de l'enum
        holder.spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int spinnerPos, long id) {
                if (holder.spinnerStatus.getTag() == null) return;
                int itemPosition = (int) holder.spinnerStatus.getTag();

                // On passe le rating de l'enum sélectionné → Status.fromRating() dans le Fragment
                float newRating = STATUS_VALUES[spinnerPos].getRating();
                callback.onRatingBarChange(itemPosition, newRating, IssueAdapter.this, data);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        return row;
    }
}
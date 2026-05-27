package com.example.myapplication.screens;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.myapplication.ControlActivity;
import com.example.myapplication.NotificationController;
import com.example.myapplication.issue.ClickableIssue;
import com.example.myapplication.issue.Issue;
import com.example.myapplication.issue.IssueRepository;
import com.example.myapplication.Notifiable;
import com.example.myapplication.R;
import com.example.myapplication.issue.Status;
import com.example.myapplication.adapter.IssueAdapter;

import java.util.List;

public class IncidentListFragment extends Fragment implements ClickableIssue<Issue> {

    private Notifiable notifiable;
    private List<Issue> myIssues;
    private IssueAdapter adapter;
    private boolean statusEditable;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Notifiable) {
            notifiable = (Notifiable) context;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_incident_list, container, false);

        // 1. Récupération des données depuis le Singleton Repository
        myIssues = IssueRepository.getInstance().getIssues();

        // 2. Configuration de la ListView
        ListView listView = view.findViewById(R.id.my_list_view);
        TextView countBadge = view.findViewById(R.id.incidents_count_badge);
        TextView emptyMessage = view.findViewById(R.id.empty_incidents_message);
        countBadge.setText(String.valueOf(myIssues.size()));

        String role = getArguments() != null
                ? getArguments().getString(ControlActivity.EXTRA_ROLE, ControlActivity.ROLE_VICTIM)
                : ControlActivity.ROLE_VICTIM;
        statusEditable = ControlActivity.ROLE_RESCUE.equals(role);

        adapter = new IssueAdapter(requireContext(), R.layout.item_alert, myIssues, this, statusEditable);
        listView.setAdapter(adapter);
        boolean hasIssues = !myIssues.isEmpty();
        listView.setVisibility(hasIssues ? View.VISIBLE : View.GONE);
        emptyMessage.setVisibility(hasIssues ? View.GONE : View.VISIBLE);

        return view;
    }

    @Override
    public void onClickItem(List<Issue> items, int itemIndex) {
        Issue selected = items.get(itemIndex);
        notifiable.onDataChange(2, selected, 1, null);
    }

    @Override
    public void onRatingBarChange(int itemIndex, float value, IssueAdapter adapter, List<Issue> items) {
        if (!statusEditable) return;

        Issue issue = items.get(itemIndex);
        Status newStatus = Status.fromRating(value);
        if (issue.getStatus() == newStatus) return;
        
        // C'EST ICI QUE LA MAGIE OPÈRE :
        // Cette ligne modifie l'objet, qui notifie l'EmergencyService automatiquement.
        issue.setStatus(newStatus);
        NotificationController.notifyStatusUpdateForUser(requireContext(), issue);

        notifiable.onDataChange(2, issue, 2, value);
        Log.d("DEBUG", "Nouveau statut pour " + issue.getTitle() + " : " + newStatus);
    }
}

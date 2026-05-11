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

import java.util.List;

public class Screen2Fragment extends Fragment implements ClickableIssue<Issue> {

    private Notifiable notifiable;
    private List<Issue> myIssues;
    private IssueAdapter adapter;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Notifiable) {
            notifiable = (Notifiable) context;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_screen2, container, false);

        // 1. Récupération des données depuis le Singleton Repository
        myIssues = IssueRepository.getInstance().getIssues();

        // 2. Configuration de la ListView
        ListView listView = view.findViewById(R.id.my_list_view);
        adapter = new IssueAdapter(requireContext(), R.layout.item_issue, myIssues, this);
        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onClickItem(List<Issue> items, int itemIndex) {
        Issue selected = items.get(itemIndex);
        notifiable.onDataChange(2, selected, 1, null);
    }

    @Override
    public void onRatingBarChange(int itemIndex, float value, IssueAdapter adapter, List<Issue> items) {
        Issue issue = items.get(itemIndex);
        Status newStatus = Status.fromRating(value);
        
        // C'EST ICI QUE LA MAGIE OPÈRE :
        // Cette ligne modifie l'objet, qui notifie l'EmergencyService automatiquement.
        issue.setStatus(newStatus); 

        notifiable.onDataChange(2, issue, 2, value);
        Log.d("DEBUG", "Nouveau statut pour " + issue.getTitle() + " : " + newStatus);
    }
}

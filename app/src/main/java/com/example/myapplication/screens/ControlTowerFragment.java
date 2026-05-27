package com.example.myapplication.screens;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.myapplication.Notifiable;
import com.example.myapplication.R;
import com.example.myapplication.adapter.IssueAdapter;
import com.example.myapplication.issue.ClickableIssue;
import com.example.myapplication.issue.Issue;
import com.example.myapplication.issue.IssueRepository;
import com.example.myapplication.issue.Status;

import java.util.ArrayList;
import java.util.List;

public class ControlTowerFragment extends Fragment implements ClickableIssue<Issue> {
    private Notifiable notifiable;
    private List<Issue> pendingIssues = new ArrayList<>();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Notifiable) {
            notifiable = (Notifiable) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        notifiable = null;
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_control_tower, container, false);

        ListView listView = view.findViewById(R.id.alerts_list_view);
        View emptyState = view.findViewById(R.id.empty_state_layout);
        TextView countBadge = view.findViewById(R.id.alerts_count_badge);

        pendingIssues = filterIssues(Status.RECEIVED);
        countBadge.setText(String.valueOf(pendingIssues.size()));

        boolean hasAlerts = !pendingIssues.isEmpty();
        listView.setVisibility(hasAlerts ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(hasAlerts ? View.GONE : View.VISIBLE);

        IssueAdapter adapter = new IssueAdapter(
                requireContext(),
                R.layout.item_alert,
                pendingIssues,
                this,
                false
        );
        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onClickItem(List<Issue> items, int itemIndex) {
        if (notifiable == null) return;
        notifiable.onDataChange(4, items.get(itemIndex), 1, null);
    }

    @Override
    public void onRatingBarChange(int itemIndex, float value, IssueAdapter adapter, List<Issue> items) {
        // Les nouvelles alertes se traitent depuis l'ecran de detail.
    }

    private List<Issue> filterIssues(Status status) {
        List<Issue> filtered = new ArrayList<>();
        for (Issue issue : IssueRepository.getInstance().getIssues()) {
            if (issue.getStatus() == status) {
                filtered.add(issue);
            }
        }
        return filtered;
    }
}

package com.example.myapplication.screens;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.myapplication.ControlActivity;
import com.example.myapplication.Notifiable;
import com.example.myapplication.R;
import com.example.myapplication.adapter.IssueAdapter;
import com.example.myapplication.issue.ClickableIssue;
import com.example.myapplication.issue.Issue;
import com.example.myapplication.issue.IssueRepository;
import com.example.myapplication.issue.Status;

import java.util.ArrayList;
import java.util.List;

public class IncidentListFragment extends Fragment implements ClickableIssue<Issue> {
    public static final String ARG_INITIAL_STATUS = "initial_summary_status";
    private static final int ACTION_SUMMARY_TAB_SELECTED = 6;

    private Notifiable notifiable;
    private List<Issue> myIssues = new ArrayList<>();
    private boolean rescueMode;
    private Status selectedSummaryStatus = Status.AID_NOT_SENT;
    private View tabNotSent;
    private View tabSent;
    private View tabFinished;
    private TextView tabNotSentBadge;
    private TextView tabSentBadge;
    private TextView tabFinishedBadge;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_incident_list, container, false);

        ListView listView = view.findViewById(R.id.my_list_view);
        TextView countBadge = view.findViewById(R.id.incidents_count_badge);
        TextView emptyMessage = view.findViewById(R.id.empty_incidents_message);
        TextView sectionTitle = view.findViewById(R.id.active_incidents);
        View summaryPanel = view.findViewById(R.id.summary_content_panel);
        LinearLayout summaryTabs = view.findViewById(R.id.summary_status_tabs);
        tabNotSent = view.findViewById(R.id.summary_tab_not_sent);
        tabSent = view.findViewById(R.id.summary_tab_sent);
        tabFinished = view.findViewById(R.id.summary_tab_finished);
        tabNotSentBadge = view.findViewById(R.id.summary_tab_not_sent_badge);
        tabSentBadge = view.findViewById(R.id.summary_tab_sent_badge);
        tabFinishedBadge = view.findViewById(R.id.summary_tab_finished_badge);

        String role = getArguments() != null
                ? getArguments().getString(ControlActivity.EXTRA_ROLE, ControlActivity.ROLE_VICTIM)
                : ControlActivity.ROLE_VICTIM;
        rescueMode = ControlActivity.ROLE_RESCUE.equals(role);

        if (rescueMode) {
            selectedSummaryStatus = readInitialSummaryStatus();
            sectionTitle.setText(R.string.summary_processed_incidents);
            emptyMessage.setText(R.string.no_summary_incidents);
            summaryTabs.setVisibility(View.VISIBLE);
            updateFolderTabs();
            tabNotSent.setOnClickListener(tab -> selectSummaryTab(
                    Status.AID_NOT_SENT,
                    listView,
                    countBadge,
                    emptyMessage
            ));
            tabSent.setOnClickListener(tab -> selectSummaryTab(
                    Status.AID_SENT,
                    listView,
                    countBadge,
                    emptyMessage
            ));
            tabFinished.setOnClickListener(tab -> selectSummaryTab(
                    Status.RESOLVED,
                    listView,
                    countBadge,
                    emptyMessage
            ));
        } else {
            summaryPanel.setBackground(null);
            summaryPanel.setPadding(0, 0, 0, 0);
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) summaryPanel.getLayoutParams();
            params.setMargins(0, 0, 0, 0);
            summaryPanel.setLayoutParams(params);
        }

        refreshList(listView, countBadge, emptyMessage);
        return view;
    }

    @Override
    public void onClickItem(List<Issue> items, int itemIndex) {
        if (notifiable == null) return;
        notifiable.onDataChange(2, items.get(itemIndex), 1, null);
    }

    @Override
    public void onRatingBarChange(int itemIndex, float value, IssueAdapter adapter, List<Issue> items) {
        // Les changements de decision secours passent par le detail.
    }

    private void refreshList(ListView listView, TextView countBadge, TextView emptyMessage) {
        myIssues = getIssuesForCurrentView();
        countBadge.setText(String.valueOf(myIssues.size()));
        updateSummaryTabBadges();

        IssueAdapter adapter = new IssueAdapter(
                requireContext(),
                R.layout.item_alert,
                myIssues,
                this,
                false
        );
        listView.setAdapter(adapter);

        boolean hasIssues = !myIssues.isEmpty();
        listView.setVisibility(hasIssues ? View.VISIBLE : View.GONE);
        emptyMessage.setVisibility(hasIssues ? View.GONE : View.VISIBLE);
    }

    private List<Issue> getIssuesForCurrentView() {
        List<Issue> issues = IssueRepository.getInstance().getIssues();
        if (!rescueMode) {
            return issues;
        }

        List<Issue> filtered = new ArrayList<>();
        for (Issue issue : issues) {
            if (issue.getStatus() == selectedSummaryStatus) {
                filtered.add(issue);
            }
        }
        return filtered;
    }

    private Status readInitialSummaryStatus() {
        Bundle args = getArguments();
        if (args == null) return Status.AID_NOT_SENT;

        String statusName = args.getString(ARG_INITIAL_STATUS);
        if (statusName == null) return Status.AID_NOT_SENT;

        try {
            Status status = Status.valueOf(statusName);
            if (status == Status.AID_SENT || status == Status.RESOLVED || status == Status.AID_NOT_SENT) {
                return status;
            }
        } catch (IllegalArgumentException ignored) {
            return Status.AID_NOT_SENT;
        }
        return Status.AID_NOT_SENT;
    }

    private void selectSummaryTab(
            Status status,
            ListView listView,
            TextView countBadge,
            TextView emptyMessage
    ) {
        if (selectedSummaryStatus == status) return;
        selectedSummaryStatus = status;
        if (notifiable != null) {
            notifiable.onDataChange(ACTION_SUMMARY_TAB_SELECTED, null, 0, status);
        }
        updateFolderTabs();
        refreshList(listView, countBadge, emptyMessage);
    }

    private void updateFolderTabs() {
        tabNotSent.setSelected(selectedSummaryStatus == Status.AID_NOT_SENT);
        tabSent.setSelected(selectedSummaryStatus == Status.AID_SENT);
        tabFinished.setSelected(selectedSummaryStatus == Status.RESOLVED);
    }

    private void updateSummaryTabBadges() {
        if (!rescueMode || tabNotSentBadge == null || tabSentBadge == null || tabFinishedBadge == null) {
            return;
        }

        int notSentCount = 0;
        int sentCount = 0;
        int finishedCount = 0;

        for (Issue issue : IssueRepository.getInstance().getIssues()) {
            if (issue.getStatus() == Status.AID_NOT_SENT) {
                notSentCount++;
            } else if (issue.getStatus() == Status.AID_SENT) {
                sentCount++;
            } else if (issue.getStatus() == Status.RESOLVED) {
                finishedCount++;
            }
        }

        tabNotSentBadge.setText(String.valueOf(notSentCount));
        tabSentBadge.setText(String.valueOf(sentCount));
        tabFinishedBadge.setText(String.valueOf(finishedCount));
    }
}

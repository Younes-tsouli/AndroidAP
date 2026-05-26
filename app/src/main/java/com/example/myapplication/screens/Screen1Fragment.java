package com.example.myapplication.screens;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.issue.Issue;
import com.example.myapplication.issue.IssueRepository;
import com.example.myapplication.issue.Status;
import com.google.android.material.button.MaterialButtonToggleGroup;

public class Screen1Fragment extends Fragment {

    private Issue currentIssue;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_screen1, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            Issue bundledIssue = args.getParcelable("my_incident");
            if (bundledIssue != null) {
                Issue repositoryIssue = IssueRepository.getInstance().findIssueById(bundledIssue.getId());
                currentIssue = repositoryIssue != null ? repositoryIssue : bundledIssue;
            }
        }

        if (currentIssue == null) return;

        ImageView iconView = view.findViewById(R.id.detail_priority_icon);
        TextView titleView = view.findViewById(R.id.detail_title);
        TextView descView = view.findViewById(R.id.detail_description);
        MaterialButtonToggleGroup statusGroup = view.findViewById(R.id.detail_status_group);
        TextView statusLabel = view.findViewById(R.id.detail_status_label);
        TextView safetyView = view.findViewById(R.id.detail_safety_protocol);

        iconView.setImageResource(currentIssue.getPriorityIcon());
        titleView.setText(currentIssue.getTitle());
        descView.setText(currentIssue.getDescription());
        statusGroup.check(getButtonIdForStatus(currentIssue.getStatus()));
        statusLabel.setText("Statut : " + getStatusLabel(currentIssue.getStatus()));
        safetyView.setText("Protocole de securite :\n" + currentIssue.getSafetyProtocol());

        statusGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            Status newStatus = getStatusForButtonId(checkedId);
            currentIssue.setStatus(newStatus);
            statusLabel.setText("Statut : " + getStatusLabel(newStatus));
        });
    }

    private int getButtonIdForStatus(Status status) {
        switch (status) {
            case AID_SENT:
                return R.id.status_aid_sent;
            case RESOLVED:
                return R.id.status_resolved;
            case RECEIVED:
            default:
                return R.id.status_received;
        }
    }

    private Status getStatusForButtonId(int buttonId) {
        if (buttonId == R.id.status_aid_sent) {
            return Status.AID_SENT;
        }
        if (buttonId == R.id.status_resolved) {
            return Status.RESOLVED;
        }
        return Status.RECEIVED;
    }

    private String getStatusLabel(Status status) {
        switch (status) {
            case AID_SENT:
                return "Secours envoyes";
            case RESOLVED:
                return "Resolu";
            case RECEIVED:
            default:
                return "Recu";
        }
    }
}

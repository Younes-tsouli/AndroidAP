package com.example.myapplication.screens;

import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.myapplication.ControlActivity;
import com.example.myapplication.NotificationController;
import com.example.myapplication.R;
import com.example.myapplication.issue.Issue;
import com.example.myapplication.issue.IssueRepository;
import com.example.myapplication.issue.Status;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.io.File;

public class IncidentDetailFragment extends Fragment {

    private Issue currentIssue;
    private boolean statusEditable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_incident_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            String role = args.getString(ControlActivity.EXTRA_ROLE, ControlActivity.ROLE_VICTIM);
            statusEditable = ControlActivity.ROLE_RESCUE.equals(role);
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
        View photoLabel = view.findViewById(R.id.detail_photo_label);
        View photoCard = view.findViewById(R.id.detail_photo_card);
        ImageView photoView = view.findViewById(R.id.detail_photo);
        TextView statusHint = view.findViewById(R.id.detail_status_hint);
        MaterialButtonToggleGroup statusGroup = view.findViewById(R.id.detail_status_group);
        View statusTimeline = view.findViewById(R.id.detail_status_timeline);
        TextView statusLabel = view.findViewById(R.id.detail_status_label);
        TextView safetyView = view.findViewById(R.id.detail_safety_protocol);

        iconView.setImageResource(currentIssue.getPriorityIcon());
        titleView.setText(currentIssue.getTitle());
        descView.setText(currentIssue.getDescription());
        showIssuePhoto(photoLabel, photoCard, photoView);
        statusLabel.setText(getString(R.string.status_prefix, getStatusLabel(currentIssue.getStatus())));
        safetyView.setText(getString(
                R.string.safety_protocol_prefix,
                getString(currentIssue.getSafetyProtocolResId())
        ));

        if (statusEditable) {
            statusHint.setText(R.string.change_status);
            statusGroup.setVisibility(View.VISIBLE);
            statusTimeline.setVisibility(View.GONE);
            statusGroup.check(getButtonIdForStatus(currentIssue.getStatus()));
            statusGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (!isChecked) return;

                Status newStatus = getStatusForButtonId(checkedId);
                if (currentIssue.getStatus() == newStatus) return;
                currentIssue.setStatus(newStatus);
                statusLabel.setText(getString(R.string.status_prefix, getStatusLabel(newStatus)));
                NotificationController.notifyStatusUpdateForUser(requireContext(), currentIssue);
            });
        } else {
            statusHint.setText(R.string.status_timeline_title);
            statusGroup.setVisibility(View.GONE);
            statusTimeline.setVisibility(View.VISIBLE);
            updateStatusTimeline(view, currentIssue.getStatus());
        }
    }

    private void showIssuePhoto(View photoLabel, View photoCard, ImageView photoView) {
        if (currentIssue == null || !currentIssue.hasPhoto()) return;

        File photo = new File(currentIssue.getPhotoPath());
        if (!photo.exists()) return;

        photoLabel.setVisibility(View.VISIBLE);
        photoCard.setVisibility(View.VISIBLE);
        photoView.setImageURI(Uri.fromFile(photo));
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
            case AID_NOT_SENT:
                return getString(R.string.status_not_sent);
            case AID_SENT:
                return getString(R.string.status_sent);
            case RESOLVED:
                return getString(R.string.status_resolved);
            case RECEIVED:
            default:
                return getString(R.string.status_received);
        }
    }

    private void updateStatusTimeline(View view, Status status) {
        boolean sent = status == Status.AID_SENT || status == Status.RESOLVED;
        boolean resolved = status == Status.RESOLVED;

        updateTimelineStep(
                view.findViewById(R.id.timeline_received_step),
                view.findViewById(R.id.timeline_received_label),
                true
        );
        updateTimelineStep(
                view.findViewById(R.id.timeline_sent_step),
                view.findViewById(R.id.timeline_sent_label),
                sent
        );
        updateTimelineStep(
                view.findViewById(R.id.timeline_resolved_step),
                view.findViewById(R.id.timeline_resolved_label),
                resolved
        );
        updateTimelineArrow(
                view.findViewById(R.id.timeline_arrow_received_sent),
                sent
        );
        updateTimelineArrow(
                view.findViewById(R.id.timeline_arrow_sent_resolved),
                resolved
        );
    }

    private void updateTimelineStep(TextView stepView, TextView labelView, boolean active) {
        int backgroundColor = color(active ? R.color.emergency_red : R.color.app_surface_variant);
        int textColor = color(active ? R.color.app_on_primary : R.color.app_text_muted);
        int labelColor = color(active ? R.color.app_text_primary : R.color.app_text_muted);

        stepView.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        stepView.setTextColor(textColor);
        labelView.setTextColor(labelColor);
        labelView.setTypeface(null, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void updateTimelineArrow(TextView arrowView, boolean active) {
        arrowView.setTextColor(color(active ? R.color.emergency_red : R.color.app_text_disabled));
    }

    private int color(int colorRes) {
        return ContextCompat.getColor(requireContext(), colorRes);
    }
}

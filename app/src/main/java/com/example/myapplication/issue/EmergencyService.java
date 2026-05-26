package com.example.myapplication.issue;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmergencyService implements IssueObserver {
    private static final String TAG = "EmergencyService";
    private static EmergencyService instance;

    private final List<String> alerts = new ArrayList<>();
    private final Map<String, Integer> alertIndexesByIssueId = new HashMap<>();

    private EmergencyService() {}

    public static synchronized EmergencyService getInstance() {
        if (instance == null) {
            instance = new EmergencyService();
        }
        return instance;
    }

    @Override
    public void onStatusChanged(Issue issue) {
        if (issue == null) return;

        String message = buildAlertMessage(issue);
        Log.d(TAG, message);
        saveOrUpdateIssueAlert(issue, message);

        if (issue.getStatus() == Status.AID_SENT) {
            Log.w(TAG, "DEPLOIEMENT DES SECOURS pour: " + issue.getTitle());
        }
    }

    @Override
    public void onPriorityChanged(Issue issue) {
        if (issue == null) return;

        String message = buildAlertMessage(issue);
        Log.d(TAG, message);
        saveOrUpdateIssueAlert(issue, message);
    }

    public List<String> getAlerts() {
        return new ArrayList<>(alerts);
    }

    public void updateAlert(int index, String newRaw) {
        if (index >= 0 && index < alerts.size()) {
            alerts.set(index, newRaw);
        }
    }

    private void saveOrUpdateIssueAlert(Issue issue, String message) {
        Integer existingIndex = alertIndexesByIssueId.get(issue.getId());

        if (existingIndex != null && existingIndex >= 0 && existingIndex < alerts.size()) {
            alerts.set(existingIndex, message);
            return;
        }

        alerts.add(message);
        alertIndexesByIssueId.put(issue.getId(), alerts.size() - 1);
    }

    private String buildAlertMessage(Issue issue) {
        return getSeverityLabel(issue)
                + " - " + issue.getTitle()
                + " - Statut: " + getStatusLabel(issue.getStatus())
                + " - Priorite: " + issue.getPriority().getName();
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

    private String getSeverityLabel(Issue issue) {
        if (issue.getStatus() == Status.RESOLVED) {
            return "NIV.1";
        }

        switch (issue.getPriority()) {
            case CRITICAL:
                return "NIV.3";
            case HIGH:
                return "NIV.2";
            case MEDIUM:
            case LOW:
            default:
                return "NIV.1";
        }
    }
}

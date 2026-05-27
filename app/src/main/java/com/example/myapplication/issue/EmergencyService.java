package com.example.myapplication.issue;

import android.content.Context;
import android.util.Log;

import com.example.myapplication.R;

public class EmergencyService implements IssueObserver {
    private static final String TAG = "EmergencyService";
    private static EmergencyService instance;

    private Context appContext;

    private EmergencyService() {}

    public static synchronized EmergencyService getInstance() {
        if (instance == null) {
            instance = new EmergencyService();
        }
        return instance;
    }

    public void initialize(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
    }

    @Override
    public void onStatusChanged(Issue issue) {
        if (issue == null) return;

        String message = buildAlertMessage(issue);
        Log.d(TAG, message);

        if (issue.getStatus() == Status.AID_SENT) {
            Log.w(TAG, text(R.string.rescue_deployment_log, issue.getTitle()));
        }
    }

    @Override
    public void onPriorityChanged(Issue issue) {
        if (issue == null) return;

        String message = buildAlertMessage(issue);
        Log.d(TAG, message);
    }

    private String buildAlertMessage(Issue issue) {
        return getSeverityLabel(issue)
                + " - " + issue.getTitle()
                + " - " + text(R.string.alert_status_field, getStatusLabel(issue.getStatus()))
                + " - " + text(R.string.alert_priority_field, getPriorityLabel(issue.getPriority()))
                + (issue.hasPhoto() ? " - " + text(R.string.alert_photo_yes_field) : "");
    }

    private String getStatusLabel(Status status) {
        switch (status) {
            case AID_NOT_SENT:
                return text(R.string.status_not_sent);
            case AID_SENT:
                return text(R.string.status_sent);
            case RESOLVED:
                return text(R.string.status_resolved);
            case RECEIVED:
            default:
                return text(R.string.status_received);
        }
    }

    private String getPriorityLabel(Priority priority) {
        switch (priority) {
            case CRITICAL:
                return text(R.string.priority_critical);
            case HIGH:
                return text(R.string.priority_high);
            case MEDIUM:
                return text(R.string.priority_medium);
            case LOW:
            default:
                return text(R.string.priority_low);
        }
    }

    private String getSeverityLabel(Issue issue) {
        if (issue.getStatus() == Status.RESOLVED) {
            return text(R.string.severity_level_1);
        }

        switch (issue.getPriority()) {
            case CRITICAL:
                return text(R.string.severity_level_3);
            case HIGH:
                return text(R.string.severity_level_2);
            case MEDIUM:
            case LOW:
            default:
                return text(R.string.severity_level_1);
        }
    }

    private String text(int resId, Object... args) {
        if (appContext == null) return "";
        return appContext.getString(resId, args);
    }
}

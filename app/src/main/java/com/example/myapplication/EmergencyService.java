package com.example.myapplication;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class EmergencyService implements IssueObserver {
    private static final String TAG = "EmergencyService";
    private static EmergencyService instance;
    
    // Pour le fragment Screen4Fragment plus tard
    private final List<String> alerts = new ArrayList<>();

    private EmergencyService() {}

    public static synchronized EmergencyService getInstance() {
        if (instance == null) {
            instance = new EmergencyService();
        }
        return instance;
    }

    @Override
    public void onStatusChanged(Issue issue) {
        String message = "ALERTE: Incident " + issue.getId() + " [" + issue.getTitle() + "] passé au statut " + issue.getStatus();
        Log.d(TAG, message);
        alerts.add(message);
        
        if (issue.getStatus() == Status.CONFIRMED) {
            Log.w(TAG, "DÉPLOIEMENT DES SECOURS pour: " + issue.getTitle());
        }
    }

    @Override
    public void onPriorityChanged(Issue issue) {
        String message = "INFO: Priorité modifiée pour " + issue.getTitle() + " -> " + issue.getPriority();
        Log.d(TAG, message);
        alerts.add(message);
    }
    
    public List<String> getAlerts() {
        return new ArrayList<>(alerts);
    }
}

package com.example.myapplication;

public class HighwayFactory implements AccidentFactory {
    @Override
    public Issue createIssue(String title, String description) {
        // Sur l'autoroute, la priorité est toujours CRITICAL
        // et le statut initial est REPORTED
        HighwayIssue issue = new HighwayIssue(title, description, Priority.CRITICAL, Status.REPORTED);
        
        // On attache automatiquement le service d'urgence
        issue.addObserver(EmergencyService.getInstance());
        
        return issue;
    }
}

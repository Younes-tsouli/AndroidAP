package com.example.myapplication;

public class UrbanFactory implements AccidentFactory {
    @Override
    public Issue createIssue(String title, String description) {
        // En ville, la priorité est MEDIUM par défaut et le statut REPORTED (rating 1.0f)
        UrbanIssue issue = new UrbanIssue(title, description, Priority.MEDIUM, Status.REPORTED);
        
        // On attache automatiquement le service d'urgence
        issue.addObserver(EmergencyService.getInstance());
        
        return issue;
    }
}

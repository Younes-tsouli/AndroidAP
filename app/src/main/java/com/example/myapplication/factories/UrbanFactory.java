package com.example.myapplication.factories;

import com.example.myapplication.issue.Priority;
import com.example.myapplication.issue.Status;
import com.example.myapplication.issue.UrbanIssue;
import com.example.myapplication.issue.AccidentFactory;
import com.example.myapplication.issue.EmergencyService;
import com.example.myapplication.issue.Issue;

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

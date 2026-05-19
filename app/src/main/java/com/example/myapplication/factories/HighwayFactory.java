package com.example.myapplication.factories;

import com.example.myapplication.issue.HighwayIssue;
import com.example.myapplication.issue.Priority;
import com.example.myapplication.issue.Status;
import com.example.myapplication.issue.AccidentFactory;
import com.example.myapplication.issue.EmergencyService;
import com.example.myapplication.issue.Issue;

public class HighwayFactory implements AccidentFactory {
    @Override
    public Issue createIssue(String title, String description, double longitude, double latitude) {
        HighwayIssue issue = new HighwayIssue(title, description, Priority.CRITICAL, Status.REPORTED,  longitude,  latitude);
        
        // On attache automatiquement le service d'urgence
        issue.addObserver(EmergencyService.getInstance());
        
        return issue;
    }
}

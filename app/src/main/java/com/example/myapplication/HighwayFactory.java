package com.example.myapplication;

public class HighwayFactory implements AccidentFactory {
    @Override
    public Issue createIssue(String title, String description) {
        // Sur l'autoroute, la priorité est toujours CRITICAL (exemple icon d'alerte rouge)
        // et le statut initial est souvent élevé (4.0f)
        return new HighwayIssue(title, description, 0, 4.0f);
    }
}
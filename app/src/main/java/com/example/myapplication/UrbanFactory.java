package com.example.myapplication;

public class UrbanFactory implements AccidentFactory {
    @Override
    public Issue createIssue(String title, String description) {
        // En ville, la priorité est modérée par défaut
        return new UrbanIssue(title, description, 0, 2.0f);
    }
}
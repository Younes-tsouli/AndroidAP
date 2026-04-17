package com.example.myapplication;

public interface Notifiable {
    void onClick(int numFragment);

    // Gardons uniquement celle-là pour les échanges complexes
    void onDataChange(int numFragment, Object object, int actionCode, Object argsAction);

    void onFragmentDisplayed(int fragmentId);
    void onIncidentSelected(Issue issue);
}
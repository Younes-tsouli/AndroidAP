package com.example.myapplication.issue;

public interface AccidentFactory {
    Issue createIssue(String title, String description, double longitude, double latitude);
}

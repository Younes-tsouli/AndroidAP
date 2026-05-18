package com.example.myapplication.issue;

import java.util.ArrayList;
import java.util.List;

public class IssueRepository {
    private static IssueRepository instance;
    private final List<Issue> issues = new ArrayList<>();

    private IssueRepository() {}

    public static synchronized IssueRepository getInstance() {
        if (instance == null) {
            instance = new IssueRepository();
        }
        return instance;
    }

    public void addIssue(Issue issue) {
        issues.add(issue);
    }

    public List<Issue> getIssues() {
        return new ArrayList<>(issues); // Retourne une copie pour la sécurité
    }
}

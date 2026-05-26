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
        if (issue == null || findIssueById(issue.getId()) != null) return;
        issue.addObserver(EmergencyService.getInstance());
        issues.add(issue);
        EmergencyService.getInstance().onStatusChanged(issue);
    }

    public List<Issue> getIssues() {
        return new ArrayList<>(issues); // Retourne une copie pour la sécurité
    }
    public Issue findIssueById(String id) {
        if (id == null) return null;
        for (Issue issue : issues) {
            if (id.equals(issue.getId())) {
                return issue;
            }
        }
        return null;
    }
}

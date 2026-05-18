package com.example.myapplication.issue;

public interface IssueObserver {
    void onStatusChanged(Issue issue);
    void onPriorityChanged(Issue issue);
}

package com.example.myapplication.issue;

public enum Priority {
    LOW("low"), MEDIUM("medium"), HIGH("high"), CRITICAL("critical");

    private String name;

    Priority(String name) {
        this.name = name;
    }
    String getName() {
        return this.name();
    }
}

package com.example.myapplication.issue;

public enum Priority {
    LOW("low"), MEDIUM("medium"), HIGH("high"), CRITICAL("critical");

    private final String name;

    Priority(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
}

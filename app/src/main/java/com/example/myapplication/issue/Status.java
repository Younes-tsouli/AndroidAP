package com.example.myapplication.issue;

public enum Status {
    REPORTED(1.0f),
    CONFIRMED(2.0f),
    ON_SITE(3.0f),
    CLEARING(4.0f),
    RESOLVED(5.0f);

    private final float rating;
    Status(float rating) { this.rating = rating; }
    public float getRating() { return rating; }

    public static Status fromRating(float rating) {
        for (Status s : Status.values()) {
            if (s.getRating() == rating) return s;
        }
        return REPORTED;
    }
}

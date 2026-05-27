package com.example.myapplication.issue;

public enum Status {
    RECEIVED(1.0f),
    AID_NOT_SENT(2.0f),
    AID_SENT(3.0f),
    RESOLVED(4.0f);

    private final float rating;
    Status(float rating) { this.rating = rating; }
    public float getRating() { return rating; }

    public static Status fromRating(float rating) {
        int roundedRating = Math.round(rating);
        for (Status s : Status.values()) {
            if (Math.round(s.getRating()) == roundedRating) return s;
        }
        return RECEIVED;
    }
}

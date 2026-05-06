package com.example.myapplication;

import android.os.Parcel;

public class UrbanIssue extends Issue{

    public static final Creator<UrbanIssue> CREATOR = new Creator<UrbanIssue>() {
        @Override
        public UrbanIssue createFromParcel(Parcel in) {
            return new UrbanIssue(in);
        }

        @Override
        public UrbanIssue[] newArray(int size) {
            return new UrbanIssue[size];
        }
    };

    public UrbanIssue(String title, String description, int priorityIcon, float status) {
        super(title, description, priorityIcon, status);
    }

    protected UrbanIssue(Parcel in) {
        super(in);
    }

    @Override
    public String getSafetyProtocol() {
        return "ne jetter pas des pierres";
    }

}

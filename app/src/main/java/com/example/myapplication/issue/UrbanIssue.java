package com.example.myapplication.issue;

import android.os.Parcel;

import com.example.myapplication.R;

public class UrbanIssue extends Issue {

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

    public UrbanIssue(String title, String description, Priority priority, Status status, double longitude, double latitude) {
        super(title, description, priority, status,  longitude,  latitude);
    }

    protected UrbanIssue(Parcel in) {
        super(in);
    }

    @Override
    public int getSafetyProtocolResId() {
        return R.string.protocol_urban;
    }
}

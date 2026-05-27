package com.example.myapplication.issue;

import android.os.Parcel;

import com.example.myapplication.R;

public class HighwayIssue extends Issue {

    public static final Creator<HighwayIssue> CREATOR = new Creator<HighwayIssue>() {
        @Override
        public HighwayIssue createFromParcel(Parcel in) {
            return new HighwayIssue(in);
        }

        @Override
        public HighwayIssue[] newArray(int size) {
            return new HighwayIssue[size];
        }
    };

    public HighwayIssue(String title, String description, Priority priority, Status status, double longitude, double latitude) {
        super(title, description, priority, status, longitude, latitude );
    }

    protected HighwayIssue(Parcel in) {
        super(in);
    }

    @Override
    public int getSafetyProtocolResId() {
        return R.string.protocol_highway;
    }
}

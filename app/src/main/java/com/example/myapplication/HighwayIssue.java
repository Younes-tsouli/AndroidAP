package com.example.myapplication;

import android.os.Parcel;

public class HighwayIssue extends Issue{

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

    public HighwayIssue(String title, String description, int priorityIcon, float status) {
        super(title, description, priorityIcon, status);
    }

    protected HighwayIssue(Parcel in) {
        super(in);
    }


    @Override
    public String getSafetyProtocol() {
        return "Rester derrière la glissière";
    }
}

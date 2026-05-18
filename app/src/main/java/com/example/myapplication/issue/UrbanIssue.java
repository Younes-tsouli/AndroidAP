package com.example.myapplication.issue;

import android.os.Parcel;

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

    public UrbanIssue(String title, String description, Priority priority, Status status) {
        super(title, description, priority, status);
    }

    protected UrbanIssue(Parcel in) {
        super(in);
    }

    @Override
    public String getSafetyProtocol() {
        return "Enfilez votre gilet de sécurité, balisez la zone avec un triangle à 30 mètres et prévenez les autres usagers.";
    }
}

package com.example.myapplication;

import android.os.Parcel;

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

    public HighwayIssue(String title, String description, Priority priority, Status status) {
        super(title, description, priority, status);
    }

    protected HighwayIssue(Parcel in) {
        super(in);
    }

    @Override
    public String getSafetyProtocol() {
        return "Évacuez immédiatement le véhicule et placez-vous derrière la glissière de sécurité. Ne tentez pas de placer le triangle sur l'autoroute.";
    }
}

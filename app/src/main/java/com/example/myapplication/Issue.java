package com.example.myapplication;

import android.os.Parcel;
import android.os.Parcelable;

public class Issue implements Parcelable {
    private String title;
    private String description;
    private int priorityIcon; // Un ID de ressource (ex: R.drawable.alerte)
    private float status;     // La note de la RatingBar

    // Constructeur pour créer un incident
    public Issue(String title, String description, int priorityIcon, float status) {
        this.title = title;
        this.description = description;
        this.priorityIcon = priorityIcon;
        this.status = status;
    }

    // --- Ici commencent les méthodes Parcelable ---

    // 2. Lire les données dans le même ordre qu'elles ont été écrites
    protected Issue(Parcel in) {
        title = in.readString();
        description = in.readString();
        priorityIcon = in.readInt();
        status = in.readFloat();
    }

    // 3. Le Créateur (Indispensable pour Android)
    public static final Creator<Issue> CREATOR = new Creator<Issue>() {
        @Override
        public Issue createFromParcel(Parcel in) {
            return new Issue(in);
        }

        @Override
        public Issue[] newArray(int size) {
            return new Issue[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    // 4. Écrire les données dans le "carton" (Parcel)
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(description);
        dest.writeInt(priorityIcon);
        dest.writeFloat(status);
    }

    // --- Les Getters et Setters (pour l'Adapter) ---

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getPriorityIcon() { return priorityIcon; }
    public float getStatus() { return status; }
    public void setStatus(float status) { this.status = status; }
}
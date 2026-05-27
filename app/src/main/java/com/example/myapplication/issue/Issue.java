package com.example.myapplication.issue;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.myapplication.R;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class Issue implements Parcelable, IssueObservable {
    private final String id;
    private final String title;
    private final String description;
    private final long timestamp;
    private Priority priority;
    private Status status;
    private double longitude;
    private double latitude;
    private String photoPath;
    
    private transient List<IssueObserver> observers = new ArrayList<>();

    public Issue(String title, String description, Priority priority, Status status, double longitude, double latitude) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.timestamp = System.currentTimeMillis();
        this.priority = priority;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    protected Issue(Parcel in) {
        id = in.readString();
        title = in.readString();
        description = in.readString();
        timestamp = in.readLong();
        priority = Priority.valueOf(in.readString());
        status = Status.valueOf(in.readString());
        longitude = in.readDouble();
        latitude = in.readDouble();
        photoPath = in.readString();
        observers = new ArrayList<>();
        addObserver(EmergencyService.getInstance());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeLong(timestamp);
        dest.writeString(priority.name());
        dest.writeString(status.name());
        dest.writeDouble(longitude);
        dest.writeDouble(latitude);
        dest.writeString(photoPath);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // --- Observable Implementation ---
    @Override
    public void addObserver(IssueObserver observer) {
        if (observers == null) observers = new ArrayList<>();
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(IssueObserver observer) {
        if (observers != null) {
            observers.remove(observer);
        }
    }

    @Override
    public void notifyObservers() {
        notifyStatusObservers();
        notifyPriorityObservers();
    }

    private void notifyStatusObservers() {
        if (observers != null) {
            for (IssueObserver observer : observers) {
                observer.onStatusChanged(this);
            }
        }
    }

    private void notifyPriorityObservers() {
        if (observers != null) {
            for (IssueObserver observer : observers) {
                observer.onPriorityChanged(this);
            }
        }
    }

    // --- Getters and Setters ---
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public long getTimestamp() { return timestamp; }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
    public boolean hasPhoto() { return photoPath != null && !photoPath.isEmpty(); }
    
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) {
        if (this.priority == priority) return;
        this.priority = priority;
        notifyPriorityObservers();
    }

    public Status getStatus() { return status; }
    public void setStatus(Status status) {
        if (this.status == status) return;
        this.status = status;
        notifyStatusObservers();
    }
    
    // Méthode utilitaire pour l'UI
    public int getPriorityIcon() {
        switch (priority) {
            case CRITICAL:
            case HIGH:
                return R.drawable.ic_menu_alert;
            case MEDIUM:
                return R.drawable.emergency;
            default:
                return R.drawable.ic_menu_clipboard;
        }
    }

    public abstract int getSafetyProtocolResId();
}

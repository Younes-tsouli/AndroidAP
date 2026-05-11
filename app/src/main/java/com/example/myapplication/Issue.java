package com.example.myapplication;

import android.os.Parcel;
import android.os.Parcelable;
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
    
    private transient List<IssueObserver> observers = new ArrayList<>();

    public Issue(String title, String description, Priority priority, Status status) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.timestamp = System.currentTimeMillis();
        this.priority = priority;
        this.status = status;
    }

    protected Issue(Parcel in) {
        id = in.readString();
        title = in.readString();
        description = in.readString();
        timestamp = in.readLong();
        priority = Priority.valueOf(in.readString());
        status = Status.valueOf(in.readString());
        observers = new ArrayList<>();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeLong(timestamp);
        dest.writeString(priority.name());
        dest.writeString(status.name());
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
        if (observers != null) {
            for (IssueObserver observer : observers) {
                observer.onStatusChanged(this);
                observer.onPriorityChanged(this);
            }
        }
    }

    // --- Getters and Setters ---
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public long getTimestamp() { return timestamp; }
    
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) {
        this.priority = priority;
        notifyObservers();
    }

    public Status getStatus() { return status; }
    public void setStatus(Status status) {
        this.status = status;
        notifyObservers();
    }
    
    // Méthode utilitaire pour l'UI
    public int getPriorityIcon() {
        switch (priority) {
            case CRITICAL: return android.R.drawable.ic_delete; // A remplacer par tes ressources
            case HIGH: return android.R.drawable.stat_notify_error;
            case MEDIUM: return android.R.drawable.stat_sys_warning;
            default: return android.R.drawable.ic_menu_info_details;
        }
    }

    public abstract String getSafetyProtocol();
}

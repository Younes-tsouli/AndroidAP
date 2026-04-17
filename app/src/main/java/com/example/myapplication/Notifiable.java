package com.example.myapplication;

public interface Notifiable {
    void onClick(int numFragment);
    void onDataChange(int numFragment, Object object);
    void onFragmentDisplayed(int fragmentId);
}

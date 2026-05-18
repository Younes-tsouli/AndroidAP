package com.example.myapplication.issue;

import android.content.Context;

import com.example.myapplication.adapter.IssueAdapter;

import java.util.List;

public interface ClickableIssue<T> {
    void onRatingBarChange(int itemIndex, float value, IssueAdapter adapter, List<T> items);

    void onClickItem(List<T> items, int itemIndex);
    Context getContext();
}

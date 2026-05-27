package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public final class ThemeController {
    private static final String PREFS = "theme_preferences";
    private static final String KEY_NIGHT_MODE = "night_mode";

    private ThemeController() {}

    public static void applySavedMode(Context context) {
        int mode = preferences(context).getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static void setupThemeToggle(AppCompatActivity activity, ImageButton button) {
        if (button == null) return;

        updateThemeButton(activity, button);
        button.setOnClickListener(view -> {
            boolean isNight = isNightMode(activity);
            int nextMode = isNight ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES;
            preferences(activity).edit().putInt(KEY_NIGHT_MODE, nextMode).apply();
            AppCompatDelegate.setDefaultNightMode(nextMode);
        });
    }

    private static void updateThemeButton(Context context, ImageButton button) {
        boolean isNight = isNightMode(context);
        button.setImageResource(isNight ? R.drawable.ic_theme_moon : R.drawable.ic_theme_sun);
        button.setContentDescription(context.getString(
                isNight ? R.string.desc_theme_toggle_dark : R.string.desc_theme_toggle_light
        ));
    }

    private static boolean isNightMode(Context context) {
        int mode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}

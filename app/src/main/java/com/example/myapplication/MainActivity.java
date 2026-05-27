package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.issue.EmergencyService;

public class MainActivity extends AppCompatActivity {
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {}
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeController.applySavedMode(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EmergencyService.getInstance().initialize(getApplicationContext());
        NotificationController.createChannels(this);
        applySystemBarInsets();
        ThemeController.setupThemeToggle(this, findViewById(R.id.theme_button));
        requestNotificationPermissionIfNeeded();

        findViewById(R.id.victim_role_button).setOnClickListener(click ->
                openControlActivity(ControlActivity.ROLE_VICTIM, 0)
        );

        findViewById(R.id.rescue_role_button).setOnClickListener(click ->
                openControlActivity(ControlActivity.ROLE_RESCUE, 1)
        );
    }

    private void openControlActivity(String role, int startIndex) {
        Intent intent = new Intent(this, ControlActivity.class);
        intent.putExtra(ControlActivity.EXTRA_ROLE, role);
        intent.putExtra(ControlActivity.EXTRA_INDEX, startIndex);
        startActivity(intent);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void applySystemBarInsets() {
        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}

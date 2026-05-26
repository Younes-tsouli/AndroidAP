package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
}

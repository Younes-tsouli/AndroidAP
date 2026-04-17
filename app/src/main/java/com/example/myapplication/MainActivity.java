package com.example.myapplication;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ImageView image = findViewById(R.id.animation);
        image.setBackgroundResource(R.drawable.animation1);

        AnimationDrawable animation = (AnimationDrawable)image.getBackground();
        animation.start();

        findViewById(R.id.default_button).setOnClickListener(click ->  {
            Intent i = new Intent(this, ControlActivity.class);
            i.putExtra("index", 0);
            startActivity(i);
        });

        findViewById(R.id.option_button_1).setOnClickListener(click ->  {
            Intent i = new Intent(this, ControlActivity.class);
            i.putExtra("index", 2);
            startActivity(i);
        });
    }
}
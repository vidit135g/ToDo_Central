package com.absolute.todocentral.ui.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.absolute.todocentral.R;
import com.google.android.material.button.MaterialButton;

public class LoadingActivity extends AppCompatActivity {

    MaterialButton btn;
    ImageView piximage;
    ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        btn=findViewById(R.id.pixelbutton);
        progressBar=findViewById(R.id.loading_spinner);
        piximage=findViewById(R.id.ivSplash);

        SharedPreferences pref = getSharedPreferences("ActivityPREF", Context.MODE_PRIVATE);
        if(pref.getBoolean("activity_executed", false)){
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            btn.setOnClickListener(v -> {
                btn.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                new Handler().postDelayed(() -> {
                    Intent i = new Intent(LoadingActivity.this, MainActivity.class);
                    startActivity(i);
                }, 4000);

            });
            SharedPreferences.Editor ed = pref.edit();
            ed.putBoolean("activity_executed", true);
            ed.commit();
        }

    }

    @Override
    protected void onResume() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        super.onResume();
    }
}

package com.absolute.todocentral.ui.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import com.absolute.todocentral.R;

public class LoadingActivity extends AppCompatActivity {

    View btn;
    ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        btn=findViewById(R.id.pixelbutton);
        progressBar=findViewById(R.id.loading_spinner);

        SharedPreferences pref = getSharedPreferences("ActivityPREF", Context.MODE_PRIVATE);
        if(pref.getBoolean("activity_executed", false)){
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            // Straight through. The old four-second wait sat in front of a
            // spinner while nothing loaded.
            btn.setOnClickListener(v -> {
                startActivity(new Intent(LoadingActivity.this, MainActivity.class));
                finish();
            });
            SharedPreferences.Editor ed = pref.edit();
            ed.putBoolean("activity_executed", true);
            ed.commit();
        }

    }

    @Override
    protected void onResume() {
        // The screen no longer goes edge-to-edge behind the system bars: it is
        // a normal page now, and hiding the bars only made the status clock
        // vanish for a moment on launch.
        super.onResume();
    }
}

package com.example.sunwidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

/** Simple launcher screen. The actual sunrise/sunset display is the home-screen widget. */
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button refresh = findViewById(R.id.refresh_button);
        refresh.setOnClickListener(v -> {
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            int[] ids = manager.getAppWidgetIds(new android.content.ComponentName(this, SunWidgetProvider.class));
            new SunWidgetProvider().onUpdate(this, manager, ids);
            ((TextView) findViewById(R.id.status_text)).setText("ویجت به‌روزرسانی شد ✓");
        });
    }
}

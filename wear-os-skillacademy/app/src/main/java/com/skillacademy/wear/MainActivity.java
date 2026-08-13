package com.skillacademy.wear;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int NOTIFICATION_PERMISSION = 1201;

    private TextView statusText;
    private boolean isRound;
    private int padding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestNotificationPermission();

        Intent serviceIntent = new Intent(this, PromoService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        Intent pinIntent = new Intent(this, PinLoginActivity.class);
        pinIntent.putExtra("force_auth", true);
        startActivity(pinIntent);
        finish();
    }

    private View buildContent() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.rgb(16, 25, 35));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(padding, padding, padding, padding);
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        TextView title = text("SkillAcademy", 24, true);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap(0));

        TextView subtitle = text("Promociones Wear OS", 15, false);
        subtitle.setTextColor(Color.rgb(148, 229, 184));
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, matchWrap(6));

        statusText = text("Las promociones aparecen automáticamente cada 10 segundos.", 13, false);
        statusText.setTextColor(Color.rgb(220, 226, 230));
        statusText.setGravity(Gravity.CENTER);
        root.addView(statusText, matchWrap(16));

        Button openWebButton = button("Ver promociones");
        openWebButton.setOnClickListener((view) -> {
            Intent intent = new Intent(this, PinLoginActivity.class);
            intent.putExtra("force_auth", true);
            startActivity(intent);
        });
        root.addView(openWebButton, matchWrap(12));

        Button setupPinButton = button("Configurar PIN");
        setupPinButton.setOnClickListener((view) -> {
            Intent intent = new Intent(this, PinLoginActivity.class);
            startActivity(intent);
        });
        root.addView(setupPinButton, matchWrap(10));

        Button syncButton = button("Sincronizar ahora");
        syncButton.setOnClickListener((view) -> {
            Intent intent = new Intent(this, PromoService.class);
            startService(intent);
            setStatus("Sincronizando promociones...");
        });
        root.addView(syncButton, matchWrap(10));

        return scrollView;
    }

    private TextView text(String value, int size, boolean bold) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(size);
        textView.setLineSpacing(2, 1.08f);
        if (bold) {
            textView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        }
        return textView;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(Color.rgb(33, 163, 102));
        return button;
    }

    private LinearLayout.LayoutParams matchWrap(int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = topMargin;
        return params;
    }

    private void setStatus(String message) {
        statusText.setText(message);
    }

    private int roundPadding() {
        if (!isRound) return 20;
        return (int) (getResources().getDisplayMetrics().widthPixels * 0.14f);
    }

    private boolean canNotify() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !canNotify()) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION);
        }
    }
}
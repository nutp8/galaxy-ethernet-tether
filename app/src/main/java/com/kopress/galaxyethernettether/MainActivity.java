package com.kopress.galaxyethernettether;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    static final String ACTION_TRIGGER = "com.kopress.galaxyethernettether.TRIGGER_TOGGLE";
    static final String PREFS_NAME = "galaxy_ethernet_tether";
    static final String PREF_ACCESSIBILITY_CONSENT = "accessibility_consent";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!DeviceSupport.isSamsungGalaxy()) {
            showUnsupportedDevice();
            return;
        }

        if (hasAccessibilityConsent() && EthernetTetherAccessibilityService.isEnabled(this)) {
            sendBroadcast(new Intent(ACTION_TRIGGER).setPackage(getPackageName()));
            Toast.makeText(this, "Ethernet tethering toggle requested", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showConsentScreen();
    }

    private void showConsentScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        int padding = dp(24);
        root.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(this);
        title.setText("Galaxy Ethernet Tether");
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView body = new TextView(this);
        body.setText("For Samsung Galaxy devices only.\n\nThis app uses Android Accessibility Service to open Samsung Settings and tap the Ethernet tethering switch when Ethernet is connected. It can read the visible Settings screen while doing that action.\n\nThe app does not collect, store, upload, or share screen contents or personal data.");
        body.setTextSize(15);
        body.setGravity(Gravity.CENTER);
        body.setPadding(0, dp(16), 0, dp(20));
        root.addView(body);

        CheckBox consent = new CheckBox(this);
        consent.setText("I understand and allow this accessibility-based automation.");
        consent.setTextSize(14);
        root.addView(consent);

        Button button = new Button(this);
        button.setText("Agree and open Accessibility Settings");
        button.setOnClickListener(v -> {
            if (!consent.isChecked()) {
                Toast.makeText(this, "Please confirm consent first.", Toast.LENGTH_SHORT).show();
                return;
            }
            getPreferences().edit().putBoolean(PREF_ACCESSIBILITY_CONSENT, true).apply();
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        });
        root.addView(button);

        setContentView(root);
    }

    private void showUnsupportedDevice() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        int padding = dp(24);
        root.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(this);
        title.setText("Unsupported device");
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView body = new TextView(this);
        body.setText("This app is built for Samsung Galaxy phones running One UI. It depends on Samsung's Ethernet tethering settings screen and is disabled on this device.\n\nDetected manufacturer: " + Build.MANUFACTURER);
        body.setTextSize(15);
        body.setGravity(Gravity.CENTER);
        body.setPadding(0, dp(16), 0, 0);
        root.addView(body);

        setContentView(root);
    }

    private boolean hasAccessibilityConsent() {
        return getPreferences().getBoolean(PREF_ACCESSIBILITY_CONSENT, false);
    }

    static boolean hasAccessibilityConsent(Context context) {
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(PREF_ACCESSIBILITY_CONSENT, false);
    }

    private SharedPreferences getPreferences() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

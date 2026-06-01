package com.kopress.galaxyethernettether;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Locale;

public class EthernetTetherAccessibilityService extends AccessibilityService {
    private static final String ACTION_TETHER_SETTINGS = "android.settings.TETHER_SETTINGS";
    private static final String SAMSUNG_ETHERNET_STATE_CHANGED = "samsung.net.ethernet.ETH_STATE_CHANGED";
    private static final String[] ETHERNET_TETHER_LABELS = {
            "\uC774\uB354\uB137 \uD14C\uB354\uB9C1",
            "Ethernet tethering",
            "Ethernet Tethering"
    };
    private static final long PENDING_TOGGLE_MS = 20_000L;
    private static final long AUTO_TRIGGER_COOLDOWN_MS = 60_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean lastEthernetPresent;
    private long pendingUntilMs;
    private long lastAutoTriggerMs;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent == null ? "" : intent.getAction();
            if (MainActivity.ACTION_TRIGGER.equals(action)) {
                requestToggle(false);
            } else if (SAMSUNG_ETHERNET_STATE_CHANGED.equals(action)) {
                handler.postDelayed(() -> requestToggle(true), 700);
            }
        }
    };

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            boolean present = isEthernetPresent();
            if (present && !lastEthernetPresent) {
                requestToggle(true);
            }
            lastEthernetPresent = present;
            handler.postDelayed(this, 2_000L);
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        if (!canRunAutomation()) {
            return;
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(MainActivity.ACTION_TRIGGER);
        filter.addAction(SAMSUNG_ETHERNET_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }

        registerEthernetNetworkCallback();
        lastEthernetPresent = isEthernetPresent();
        if (lastEthernetPresent) {
            handler.postDelayed(() -> requestToggle(true), 1_000L);
        }
        handler.postDelayed(pollRunnable, 2_000L);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {
        }
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (RuntimeException ignored) {
            }
        }
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!canRunAutomation()) {
            return;
        }
        if (pendingUntilMs <= System.currentTimeMillis()) {
            return;
        }
        if (event == null || event.getPackageName() == null) {
            return;
        }
        if (!"com.android.settings".contentEquals(event.getPackageName())) {
            return;
        }
        handler.postDelayed(this::tryToggleFromCurrentWindow, 250);
    }

    @Override
    public void onInterrupt() {
    }

    private void requestToggle(boolean automatic) {
        if (!canRunAutomation()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (automatic && now - lastAutoTriggerMs < AUTO_TRIGGER_COOLDOWN_MS) {
            return;
        }
        if (automatic) {
            lastAutoTriggerMs = now;
        }

        pendingUntilMs = now + PENDING_TOGGLE_MS;
        wakeBriefly();

        Intent settingsIntent = new Intent(ACTION_TETHER_SETTINGS);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(settingsIntent);

        handler.postDelayed(this::tryToggleFromCurrentWindow, 800);
        handler.postDelayed(this::tryToggleFromCurrentWindow, 1_800);
        handler.postDelayed(this::tryToggleFromCurrentWindow, 3_000);
    }

    private void tryToggleFromCurrentWindow() {
        if (pendingUntilMs <= System.currentTimeMillis()) {
            return;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return;
        }

        ToggleResult result = findAndToggleEthernetSwitch(root);
        if (result == ToggleResult.DONE) {
            pendingUntilMs = 0;
            Toast.makeText(this, "Ethernet tethering is on", Toast.LENGTH_SHORT).show();
            handler.postDelayed(() -> performGlobalAction(GLOBAL_ACTION_BACK), 900);
        }
    }

    private ToggleResult findAndToggleEthernetSwitch(AccessibilityNodeInfo root) {
        for (String label : ETHERNET_TETHER_LABELS) {
            for (AccessibilityNodeInfo node : root.findAccessibilityNodeInfosByText(label)) {
                ToggleResult result = tryToggleNearLabel(node);
                if (result != ToggleResult.NOT_FOUND) {
                    return result;
                }
            }
        }
        return ToggleResult.NOT_FOUND;
    }

    private ToggleResult tryToggleNearLabel(AccessibilityNodeInfo labelNode) {
        AccessibilityNodeInfo current = labelNode;
        for (int depth = 0; depth < 8 && current != null; depth++) {
            AccessibilityNodeInfo switchNode = findSwitch(current);
            if (switchNode != null) {
                if (!switchNode.isEnabled()) {
                    return ToggleResult.WAIT;
                }
                if (switchNode.isChecked()) {
                    return ToggleResult.DONE;
                }
                if (clickNodeOrParent(switchNode)) {
                    return ToggleResult.DONE;
                }
                return ToggleResult.WAIT;
            }
            current = current.getParent();
        }
        return ToggleResult.NOT_FOUND;
    }

    private AccessibilityNodeInfo findSwitch(AccessibilityNodeInfo node) {
        if (node == null) {
            return null;
        }
        CharSequence className = node.getClassName();
        if (className != null && className.toString().toLowerCase(Locale.US).contains("switch")) {
            return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findSwitch(node.getChild(i));
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private boolean clickNodeOrParent(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        for (int depth = 0; depth < 5 && current != null; depth++) {
            if (current.isClickable() && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private boolean isEthernetPresent() {
        String state = Settings.System.getString(getContentResolver(), "ethernet_state");
        if (state != null) {
            String normalizedState = state.trim().toLowerCase(Locale.US);
            if (normalizedState.equals("connected") || normalizedState.equals("1")) {
                return true;
            }
        }

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if ("eth0".equals(networkInterface.getName()) && networkInterface.isUp()) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void registerEthernetNetworkCallback() {
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return;
        }

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                handler.postDelayed(() -> requestToggle(true), 700);
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                if (networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    handler.postDelayed(() -> requestToggle(true), 700);
                }
            }
        };

        try {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                    .build();
            connectivityManager.registerNetworkCallback(request, networkCallback);
        } catch (RuntimeException ignored) {
        }
    }

    private boolean canRunAutomation() {
        return DeviceSupport.isSamsungGalaxy() && MainActivity.hasAccessibilityConsent(this);
    }

    private void wakeBriefly() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager == null) {
            return;
        }
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "GalaxyEthernetTether:toggle");
        wakeLock.acquire(5_000L);
    }

    static boolean isEnabled(Context context) {
        String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabledServices)) {
            return false;
        }
        ComponentName expected = new ComponentName(
                context,
                EthernetTetherAccessibilityService.class);
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabledServices);
        while (splitter.hasNext()) {
            ComponentName componentName = ComponentName.unflattenFromString(splitter.next());
            if (expected.equals(componentName)) {
                return true;
            }
        }
        return false;
    }

    private enum ToggleResult {
        NOT_FOUND,
        WAIT,
        DONE
    }
}

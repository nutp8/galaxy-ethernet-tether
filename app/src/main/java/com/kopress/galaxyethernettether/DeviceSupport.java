package com.kopress.galaxyethernettether;

import android.os.Build;

final class DeviceSupport {
    private DeviceSupport() {
    }

    static boolean isSamsungGalaxy() {
        return "samsung".equalsIgnoreCase(Build.MANUFACTURER);
    }
}

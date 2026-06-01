# Development Notes

## Build Configuration

- Android Gradle Plugin: 8.5.2
- Compile SDK: 35
- Minimum SDK: 26
- Target SDK: 35
- Application ID: `com.kopress.galaxyethernettether`

## Core Flow

1. The user opens the app.
2. The app blocks unsupported non-Samsung devices.
3. The user accepts the Accessibility Service disclosure.
4. Android Accessibility settings are opened.
5. After the service is enabled, Ethernet detection can trigger automation.
6. The service opens Samsung tethering settings and toggles Ethernet tethering.

## Verification

Run:

```bash
./gradlew assembleDebug
./gradlew bundleRelease
```

Manual device verification is still required because Samsung settings screens
vary by device, Android version, region, and One UI version.

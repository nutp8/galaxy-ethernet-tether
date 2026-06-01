# Galaxy Ethernet Tether

Galaxy Ethernet Tether is a small Android utility for Samsung Galaxy / One UI
devices. When a USB Ethernet adapter is connected, the app can open Samsung's
`Mobile Hotspot and Tethering` settings screen and turn on the `Ethernet
tethering` switch through an Android Accessibility Service.

This app is intentionally narrow:

- Samsung Galaxy / One UI devices only
- local, on-device automation only
- no analytics
- no network calls
- no collection, storage, upload, or sharing of screen contents or personal data

## Why Accessibility Service Is Used

Android does not expose a public third-party API for toggling Samsung Ethernet
tethering directly. This app uses Android Accessibility Service only after the
user gives in-app consent and enables the service in Android settings.

The service is used to:

1. detect a connected Ethernet interface,
2. open Samsung tethering settings,
3. find the Ethernet tethering switch,
4. tap the switch if it is available and currently off,
5. return from Settings.

The app does not use Accessibility Service for general screen monitoring,
content collection, user profiling, advertising, or analytics.

## Device Requirements

- Samsung Galaxy phone or tablet running One UI
- Android 8.0 or newer
- USB Ethernet adapter supported by the device
- User-enabled Accessibility Service permission

Non-Samsung devices are blocked in-app because the automation depends on
Samsung's settings screen and Samsung-specific Ethernet behavior.

## Repository Layout

- `app/`: Android application module
- `app/src/main/java/...`: app and Accessibility Service source
- `app/src/main/res/`: strings, styles, and accessibility service config
- `gradle/`: Gradle wrapper files
- `build.gradle.kts`, `settings.gradle.kts`: project build configuration
- `docs/`: public development notes

Generated build outputs are intentionally ignored.

## Build

Clone the repository:

```bash
git clone https://github.com/handicop/galaxy-ethernet-tether.git
cd galaxy-ethernet-tether
```

Build a debug APK:

```bash
./gradlew assembleDebug
```

The debug APK will be created at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Build a release app bundle:

```bash
./gradlew bundleRelease
```

The release bundle will be created at:

```text
app/build/outputs/bundle/release/app-release.aab
```

Release signing is not included in this repository. Configure signing locally
or in your own CI/CD environment.

## Install And Use

1. Build and install the debug APK on a Samsung Galaxy device.
2. Open `Galaxy Ethernet Tether`.
3. Read the Accessibility Service disclosure.
4. Check the consent box.
5. Tap `Agree and open Accessibility Settings`.
6. Enable `Galaxy Ethernet Tether` in Android Accessibility settings.
7. Connect a supported USB Ethernet adapter.
8. Open the app or wait for Ethernet detection to trigger automation.

If the app cannot find the Ethernet tethering switch, open Samsung Settings and
verify that `Mobile Hotspot and Tethering` contains an `Ethernet tethering`
option on your device and firmware version.

## Privacy

See [PRIVACY.md](PRIVACY.md).

## Security

See [SECURITY.md](SECURITY.md).

## License

No license has been selected yet. Do not assume redistribution or commercial
use rights until a license is added.

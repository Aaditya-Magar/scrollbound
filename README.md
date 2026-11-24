# Scrollbound

Scrollbound is an Android habit tracker for short videos. The first development build includes guided setup, a local YouTube Shorts counter, a configurable daily cap, and optional multi-pulse feedback.

## Build locally

Use JDK 17 or later and Android SDK Platform 36. Set `ANDROID_HOME` or create a local `local.properties` containing `sdk.dir`.

```sh
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Tracking and privacy

Read the tracking disclosure in setup before enabling Scrollbound under Android Accessibility settings. The service observes YouTube UI events and requires a visible Shorts feed and title candidate. It discards UI text after computing a temporary in-memory fingerprint. Daily totals, cap preferences, and onboarding answers stay in local app storage. The development build uses no account, network permission, or telemetry.

Use the vibration preview on a physical phone to feel the cap alert. The detector is experimental. YouTube can change its interface identifiers across versions and experiments. History, export and interventions beyond cap vibration are planned.

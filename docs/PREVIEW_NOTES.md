# Hyperpixelacity 0.1.0 preview

Download **Hyperpixelacity-0.1.0-preview.apk** below and open it on Android 10 or newer with OpenGL ES 3.0. This is the same debug-signed APK delivered in ChatGPT, not a rebuilt binary. No account or network connection is needed in the installed app.

Includes offline two-hand tracking, ten procedural effects, gesture controls, silent camera/effects recording, local playback and sharing, favorites and effect adjustment controls.

## Verification
Build, lint, eight JVM gesture tests, two Android emulator tests, APK signature verification and 16 KB ZIP alignment checks passed in [build 36391916611](https://github.com/syedsuban070/SUPERNATURAL-HAND-GESTURES/actions/runs/36391916611).

Source commit: `336a646938b7ec21b9694bba82df19cb76bd88e1`.

SHA-256: `ae8dd36afbfddb7da7913a6c8cbcdc28c44414c7fbde089243e563429e1aa8b1`.

## Preview limitations
Real-hand tracking, camera alignment, saved composite videos and thermal behavior still need physical-phone validation. Depth/finger occlusion, audio, trim and two-hand object rotation are not implemented. This is not a production or Play Store release.

See the attached installation guide, device QA checklist, third-party notices and test reports. Current main includes the standard Gradle wrapper and subsequent documentation; this release tag identifies the APK's exact source commit. If a later debug APK cannot update this test build due to a different signing key, uninstall the earlier app first (settings are removed).

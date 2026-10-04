# HYPERPIXELACITY
**Create cinematic powers with your hands.**

An offline native Android camera studio for fingertip trails, energy orbs and gesture-controlled effects. Built for Evidence Of One.

## Download
[Download the APK and related files](https://github.com/syedsuban070/SUPERNATURAL-HAND-GESTURES/releases/tag/v0.2.1-preview).

## Current status
A compiled development preview. CI build, lint, 18 JVM tests, three Android emulator tests, APK signing and alignment verification pass. It is not yet physically device-tested or production-ready. Build results and test APKs appear under [Actions](https://github.com/syedsuban070/SUPERNATURAL-HAND-GESTURES/actions).

## Camera and gesture fixes in 0.2.1
Camera flips reuse the model, compositor and attached preview. Gesture acquisition now tolerates slower tracking and either-hand pinch. Rendering no longer hides every result older than 200 ms. Improved crystal, orb and circle shaders with cheaper trail rendering. [Repair notes](docs/UPDATE_0_2_1.md).

## New in 0.2
Adaptive landmark filtering, safer gesture resets, refined trails and orbs, illustrated effect cards, Focus controls, three quick looks and live tracking diagnostics. [Details](docs/UPDATE_0_2.md).

## Implemented
- Kotlin, Compose Material 3, Hilt and local DataStore settings.
- CameraX front/rear preview and local MediaPipe hand tracking.
- Index trails, palm effects, charged two-hand orbs, flick projectiles and pinch-following crystals.
- Ten original procedural OpenGL ES 3.0 effect styles.
- A camera SurfaceProcessor feeding both preview and video, so the encoder receives the composite.
- Silent recording (up to 60 seconds), countdown, local playback, share, rename and delete.
- Quiet charcoal/ivory controls; effect tuning, favorites, quality and reduced motion.
- No account, Internet permission, microphone permission, advertising or cloud inference.

## Build
Requirements: Java 17, Android SDK 37.0 and Build Tools 36.0.0, Python 3. Initial dependency/model downloads require internet; the installed app does not.

```sh
sdkmanager 'platforms;android-37.0' 'build-tools;36.0.0'
python3 tools/download_model.py
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The checked-in standard Gradle wrapper pins Gradle 9.3.1. On Windows use gradlew.bat. No signing key is committed; this command produces a development APK, not a store release.

## Use
Allow camera. Choose an effect and follow its gesture guide. Record, then open **My clips**. Files are stored under Movies/Hyperpixelacity. For an unsupported camera stream combination, live effects remain available with recording disabled and an explanation.

## Limits
This preview does not implement depth/finger occlusion, audio, trim, two-hand object rotation, or arbitrary AI-generated models. A 2D flick approximates throwing. See [device QA and known limits](docs/DEVICE_QA.md), [delivery status](docs/DELIVERY_STATUS.md), and [initial architecture](docs/PHASE_0_ARCHITECTURE.md).

Visual effects are fictional digital effects. The owner authorized ongoing development without approval gates.

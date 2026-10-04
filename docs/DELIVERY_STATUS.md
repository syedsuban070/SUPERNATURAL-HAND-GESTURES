# Delivery status

Owner approval: all development phases authorized on 28 September 2026; no per-phase permission gates remain.

## Implemented source
Native Kotlin/Compose/Hilt project; permission and privacy onboarding; CameraX front/rear studio; MediaPipe two-hand LIVE_STREAM adapter; pure Kotlin gesture timing/smoothing; 10 procedural GL effect styles; shared SurfaceProcessor for preview/video; silent scoped-storage video recording; local playback/share/rename/delete; DataStore settings, favorites and effect adjustment preset; adaptive landscape controls; eight gesture regression tests.

## Verification
The Kotlin application compiles, eight gesture JVM tests pass, and Android lint passes. Two Android 15 x86_64 emulator tests pass: the actual GLES3 fragment shader compiles and the bundled MediaPipe model runs LIVE_STREAM inference on a blank frame without Internet permission. The full workflow passed: https://github.com/syedsuban070/SUPERNATURAL-HAND-GESTURES/actions/runs/36391916611 . APK signature verification and 16 KB ZIP alignment checks passed, and onboarding launched on the emulator. These checks do not validate real hands, camera alignment or composited recordings on physical hardware.
Android 37 is published as platforms;android-37.0. Build configuration explicitly selects minor API level 0. The temporary SDK 36 experiment was rejected by the modern UI dependencies and has been removed.

See DEVICE_QA.md for unverified device behavior and preview limitations. Optional ARCore/depth and richer object manipulation remain unimplemented. No production-readiness claim.

## Delivered preview
Source commit: `336a646938b7ec21b9694bba82df19cb76bd88e1`.

APK SHA-256: `ae8dd36afbfddb7da7913a6c8cbcdc28c44414c7fbde089243e563429e1aa8b1`.

Universal debug APK: 100,110,271 bytes, including the offline model and native architectures. APK is available in the successful workflow artifact Hyperpixelacity-debug-APK. Later documentation/wrapper-only commits do not change this binary.

## Motion Studio 0.2 preview
Published at https://github.com/syedsuban070/SUPERNATURAL-HAND-GESTURES/releases/tag/v0.2.0-preview . Build 37200598283 passed compilation, lint, 13 JVM tests, two emulator tests, APK signature and ZIP alignment verification.

APK source commit: `65055e02e66d3c7bbca4b93c58c82ad1e4be6549`.

APK SHA-256: `7e3850f3b9e2823887a65019e66d308565189732fc96f71b540c524359f1aa51`.

Camera/real-hand/thermal QA is still unverified on physical phones. See UPDATE_0_2.md. Subsequent CI-only commits strengthen launch verification without changing the published APK.

## Camera and gesture repair 0.2.1
Published: https://github.com/syedsuban070/SUPERNATURAL-HAND-GESTURES/releases/tag/v0.2.1-preview .

Build 37219437410 passed compilation, lint, 18 JVM regression tests, 3 emulator tests (including repeated front/back rebinding with one model initialization), rendered onboarding, signature and alignment verification.

Source commit: `886e48772e2fdb80043ab28012ba676944ef44b2`.
APK SHA-256: `27c5e07be9487784934c6673ba4642097dc59a232954f9e523e697e177acc6d0`.

These are emulator/regression results, not a real-phone speed or gesture-success benchmark. See UPDATE_0_2_1.md for changes and limits.

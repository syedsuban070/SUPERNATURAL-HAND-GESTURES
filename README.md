# HYPERPIXELACITY
**Create cinematic powers with your hands.**

An offline-first native Android camera studio planned for fingertip trails, palm effects, energy orbs and gesture-controlled objects.

## Current status
**Phase 0: technical discovery and architecture.**
This repository currently contains planning documents only. No Android application, build verification, or APK is available yet.

Read [the technical decisions, UI direction, dependency candidates, risks and phased acceptance criteria](docs/PHASE_0_ARCHITECTURE.md).

## Intended experience
- Local hand tracking using a bundled MediaPipe model.
- Custom OpenGL effects composed with the camera before video encoding.
- Quiet charcoal/ivory UI, restrained accents and accessible controls.
- No login, paid cloud API or uploaded camera frames for the core studio.
- Optional depth features only when device support and integration are verified.

## Implementation direction
Kotlin · Compose Material 3 · Hilt · CameraX · MediaPipe · OpenGL ES 3.0 · DataStore.
Proposed minimum: Android 10 (API 29). Target: Android 17 (API 37).
Dependency candidates are documented; the combined toolchain is not yet compilation-verified.

## Delivery gate
The owner requested approval between phases. Phase 1 starts after Phase 0 approval.
Current execution host has Java 17 but no detected Android SDK or Gradle installation, and a direct SDK download connection timed out. A functioning Android build environment is required before an APK can be delivered.

No build commands apply to this documentation-only commit. Planned commands and device QA gates are in the architecture document.

Visual effects are fictional digital effects.


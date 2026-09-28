# Third-party notices and asset provenance

The app links Kotlin, kotlinx.coroutines, AndroidX, Dagger/Hilt and Google MediaPipe. Their project licenses/notices must be retained in distributions as applicable. No proprietary VFX SDK is used. GLSL effects in this repository are project-authored.

- AndroidX: https://android.googlesource.com/platform/frameworks/support/ (Apache-2.0)
- Kotlin: https://github.com/JetBrains/kotlin (Apache-2.0)
- Coroutines: https://github.com/Kotlin/kotlinx.coroutines (Apache-2.0)
- Dagger/Hilt: https://github.com/google/dagger (Apache-2.0)
- MediaPipe runtime: https://github.com/google-ai-edge/mediapipe (Apache-2.0; retain transitive notices)
- JUnit (test only): https://github.com/junit-team/junit4 (EPL-1.0)

## Hand model
Build-time source: https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task
SHA-256: fbc2a30080c3c557093b5ddfc334698132eb341044ccee322ccf8bcf3607cde1
Guide: https://developers.google.com/edge/mediapipe/solutions/vision/hand_landmarker

Downloaded and verified only during development/build; packaged locally in the APK. The model is not downloaded by the app. Model-specific redistribution terms need to be checked separately from the runtime license before a commercial/store release; this file does not assign a license to Google's model weights.

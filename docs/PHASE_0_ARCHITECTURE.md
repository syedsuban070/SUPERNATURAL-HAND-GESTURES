# HYPERPIXELACITY — Phase 0 technical decision record
Create cinematic powers with your hands.

Date: 28 September 2026 (Asia/Karachi).
Status: architecture proposal published for owner approval. No application code, APK, build success, or device performance is claimed.

## Objective and acceptance
Choose an offline native Android architecture, define recording before implementation, verify available stable dependency candidates, expose unknowns, and establish measurable delivery gates.

Phase 0 deliverables: this document and README.md. No dependencies are installed in this phase.
Acceptance: architecture, UI direction, camera ownership, recording fallback, dependency evidence, risks, and phase gates are reviewable. Full dependency compatibility remains a Phase 1 compilation gate; documentation alone cannot prove it.

## Product and scope
Package: com.hyperpixelacity.app. Kotlin, Compose Material 3, Hilt, Coroutines/Flow, CameraX, MediaPipe Hand Landmarker, OpenGL ES 3.0.
Core use requires no server, account, subscriptions, paid effect SDK, or real-time generative model. Bundle the trained model and original effect assets at build time. Development dependency downloads still need internet.
Ship progressively: first buildable foundation; then tracking; then a trail and orb; then remaining catalog; then optional occlusion and final recording/export verification. Do not advertise unimplemented effects.
No perfect finger occlusion, universal 60 FPS, or accurate physical forward-distance measurement is promised.

## Verified baseline and remaining resolution work
Sources were checked on the date above. These are proposed pins, not a generated dependency lockfile.

| Component | Candidate | Purpose and status |
| --- | --- | --- |
| Android minSdk | 29 | Product choice: Android 10+ simplifies scoped storage. MediaPipe platform floor is 24; our selected floor is deliberately higher. |
| compileSdk / targetSdk | 37 / 37 | Android 17 stable official announcement [1]. |
| JDK | 17 | Available locally; compatible with selected AGP [2]. |
| AGP | 9.1.1 | Official table supports API 37 [2]; selected stable baseline, not a claim of newest AGP. |
| Gradle wrapper | 9.3.1 | Paired with AGP by official table [2]. Wrapper checksum must be verified when downloaded. |
| Kotlin | AGP built-in 2.2.10 baseline | AGP table [2]. Verify compatibility with resolved libraries; do not apply the legacy Android Kotlin plugin redundantly. |
| Compose BOM | 2026.09.00 | Official BOM example [3]. Use matching Compose compiler plugin for the actual Kotlin compiler; verify metadata before retaining this combination. |
| CameraX | 1.6.2 | Official stable release for core, camera2, lifecycle, view, video [4]. |
| Hilt | 2.60.1 | Official setup [5]; KSP2 integration needs compilation verification. |
| KSP | 2.3.9 candidate | Official Dagger KSP example [6]; exact AGP/Kotlin compatibility still a build gate. |
| DataStore Preferences | 1.2.1 | Official stable release [7]; settings/favorites. |
| Navigation Compose | 2.10.2 candidate | AndroidX stable table [8]; foundation dependency resolution required. |
| MediaPipe tasks-vision | 0.10.35 candidate | Official release exists [9]; verify Android Maven artifact, native ABIs, model license, and 16 KB alignment before pinning in Phase 3. |
| Lifecycle, activity-compose, coroutines | Resolve before Phase 1 implementation | Select explicit stable compatible versions, record POM/metadata and dependency tree. Do not use placeholders in build files. |
| ARCore / Media3 Transformer | Deferred dependency selection | Optional depth / trim; verify stable releases immediately before integration. |
| Room / Filament | Not added | No database or physically based renderer needed for initial scope. |

If modern UI artifacts require newer Kotlin metadata, update Kotlin/compiler coherently using the official compatibility table, then recompile. Never silently substitute a random version or suppress compatibility checks. Check release dates against project date and avoid alpha artifacts.
Produce libs.versions.toml, Gradle verification metadata, dependency locks where supported, third-party notices, and model SHA-256 once actual artifacts resolve. GitHub release tags alone do not establish Maven availability.

## Rendering decision
| Approach | Advantages | Costs | Decision |
| --- | --- | --- | --- |
| Custom OpenGL ES 3.0 / GLSL | Direct particles, trails, mesh primitives, compositing and predictable GPU ownership | We own buffers, shader validation, EGL recovery and pooling | Selected |
| Google Filament | Android renderer; Apache-2.0; suitable for physically based object scenes [10] | Another renderer lifecycle and material pipeline for an emissive-effects-first app | Reconsider only for richer 3D object requirements |

First visuals use procedural meshes/textures: crystal, rings, shield, orb, ribbon trails. Low tier uses analytic glow; Balanced uses half-resolution glow buffer; High may use two blur passes. All tiers cap overdraw and particles. Visual settings never control unbounded allocations.

## Camera and recording architecture
Use CameraX Preview + ImageAnalysis, and VideoCapture when recording is implemented. Compose controls are separate from the rendered scene.
Implement CameraEffect with PREVIEW | VIDEO_CAPTURE targets and a custom SurfaceProcessor [11,12]. The processor receives the camera texture, composites camera plus VFX, and draws processed output to the CameraX-provided surfaces. CameraX Recorder handles encoded video output [13]. This is the selected native production API route; custom effects still require implementation and physical-device proof.

One GL thread owns EGL, input SurfaceTexture, FBOs, shaders and output surfaces. Build one logical scene snapshot per camera timestamp; reuse it across preview and recording. Apply each output's crop/rotation transform separately. Release surfaces only in response to ownership/lifecycle completion, not immediately after submission.
UI buttons must never enter the recorded image. A deliberate mirrored-export preference determines export mirroring; preview mirroring alone must not flip gesture coordinates or silently change video.
Lock output dimensions at recording start; adapt UI on resize. Finalize the clip before switching camera. Initially finalize when backgrounded rather than implement background recording.

Tracking pipeline: ImageAnalysis KEEP_ONLY_LATEST, bounded conversion buffers, one in-flight inference; close ImageProxy in all branches. Never recycle the input while the asynchronous task may own it. Drop stale work. Use frame timestamps mapped to a common monotonic timeline; callbacks preserve the originating frame time.
Use a short bounded history of gesture snapshots. Match snapshots to frame timestamps; cap interpolation/prediction. Start with a 100 ms freshness target and stop new emissions after 150 ms of stale tracking (tuning values, not measurements).
The preview may refresh effects at display cadence using the most recent camera image; encoded frames advance according to the chosen recording frame rate.

Recording recovery:
1. Negotiate supported SDR H.264/MP4 at 720p/30 initially; 1080p only when supported.
2. If setup fails, retry at lower supported resolution and a compatible AVC encoder, preserving compositing.
3. A custom EGL-to-MediaCodec input surface / MediaMuxer backend is a contingency, not an implemented fallback. Add only if the CameraX path fails a documented requirement.
4. If all encoders fail, retain live effects and explain that recording is unavailable. Never silently save a raw camera-only video.
5. Clean incomplete MediaStore entries; report finalization failures honestly. Recover already finalized clips after app restart.

## Tracking and gesture design
Hand domain: stable session track ID, handedness plus its separate score, 21 normalized image landmarks, optional hand-local world landmarks, timestamp, wrist/tips/palm center, estimated palm normal, velocity, validity and visibility state.
MediaPipe supplies image/world landmarks and handedness; world landmarks are centered on each hand [14]. They are not a shared room coordinate system. Never subtract two independent hand-local origins to claim real inter-hand distance.
Tracking confidence is nullable if the API does not expose it. Handedness confidence must not be renamed tracking confidence. Derive a separately named quality estimate from temporal consistency, completeness and geometry.
Associate hands by spatial continuity plus handedness; handedness alone is insufficient during crossing. Reset velocity on identity changes. Compute geometry in aspect-correct image space, not raw normalized x/y distance.

Pure Kotlin state engines:
- Smooth landmarks with time-aware EMA first; measure lag before considering One Euro.
- Pinch: normalize thumb-index separation by palm width. Initial candidate engage ratio 0.25 for 90 ms; release ratio 0.38 for 70 ms. Require valid geometry and debounce both directions.
- Open palm: joint extension, fingertip distance and palm orientation; require stable visible landmarks for 120 ms. Missing data cancels candidate.
- Trail: stable index extension starts sampling; bounded ring buffer, minimum spatial separation, time-based fade. Lost tracking breaks the strip instead of drawing a bridge.
- Orb: IDLE → HAND_CANDIDATE → CHARGING → FORMED → HELD; EXPANDING is a bounded size-change substate. Candidate 150 ms, charge 450 ms; midpoint and radius from image-space palms; facing-palm check and hysteresis. Tune these values using recordings.
- Throw: only a formed/held object can launch; require coherent velocity across several samples, release evidence and 800 ms cooldown. Image-space flick is reliable initial interaction; apparent hand-size change can assist a forward-throw heuristic but is not metric depth.
- THROWING → PROJECTILE_ACTIVE → COOLDOWN → IDLE uses simulation time. A projectile remains independent of hand loss.
- LOST_TRACKING: suspend new actions immediately; short visual fade; clear holds after 250 ms. Reacquisition requires fresh dwell and cannot auto-throw.
Every transition stores reason, time, validity requirements, threshold/dwell/hysteresis, cancellation and reset behavior in a transition table. Debug logging contains state names/timing only, never images or stored landmarks.
Pinch one object to move it. Two simultaneous pinches set relative scale/rotation against captured anchors. Avoid arbitrary orientation jumps and clamp scale.

Tests: variable frame rates, threshold oscillation, delayed/out-of-order timestamps, zero-size palms, NaN coordinates, crossing hands, brief loss, long loss, reacquisition, cooldown, left/right mirror transforms and no accidental throws. Fixtures are synthetic or explicitly consented, never silently recorded.

## Effects and UI
EffectDefinition: stable id, display name resource, category, supported binding, core/glow colors, texture asset IDs, base scale, palm scaling, max particles, cooldown/lifetime, throw support, optional local sound and minimum quality tier. Validate presets and clamp all numbers.
Planned catalog: White Light Trail; Blue Plasma Orb; Orange Fire Orb; Purple Cosmic Sphere; Cyan Lightning Hand; Golden Magic Circle; Ice Crystal Summon; Dark Portal; Energy Shield; Simple Summoned Crystal. Styles are original procedural effects, with no franchise names.
Gesture selection is limited to supported bindings. Undo/reset and local presets; no locked paid cards.

UI direction overrides the pasted neon-menu brief:
- Warm ivory #F5F3EF, charcoal #17191C, quiet gray surfaces, restrained sage accent #8CADA0. No neon borders or flashing navigation.
- System typography, clear hierarchy, generous spacing, 48 dp minimum controls; contrast checked during implementation.
- Camera opens to a small labeled tracking chip, single effect carousel, large record button, camera flip and library access. Advanced settings open in a bottom sheet.
- Compact phones use bottom sheets; landscape/tablets use side panels. Respect insets, cutouts, large text and resizable windows.
- Onboarding: local processing/privacy, hand gesture cards, handedness preference, camera permission when entering Studio.
- Library: categories and favorites. Editor: color, intensity, size, density, glow, trail length, supported gesture and reset/save.
- Preview/history: local playback, title, save/share/delete. Export trim is deferred until a verified Media3 path exists.
- Settings: quality, mirror, haptics, optional sound, reduced motion, countdown 0/3/5, debug, privacy and cache cleanup.
- English strings in resources first; Urdu resources and RTL support fit the same structure.
- Status uses words/icons as well as color. Effects warning: “Fictional digital effects.”
- No microphone permission until audio recording exists. Silent recording is the initial recording milestone; pause/resume and trim explicitly deferred.

## Occlusion and AR
OcclusionProvider exposes None, person mask, or depth plus timestamp and coordinate transform. None is the universal default.
CameraX and ARCore must not independently compete for the camera. Official ARCore shared camera documentation is Camera2-specific [15].
Phase 7 must prototype a separate optional rear-camera ARCore session, pausing/unbinding CameraX first, adapting AR frames to the same tracking/render domains. Preserve CameraX mode when ARCore unavailable. Do not present this as a trivial flag.
Check installation/support and isDepthModeSupported for the selected session; missing depth is normal [16]. Optional AR services may need installation/update online; base studio must work on first launch in airplane mode with bundled assets.
Person segmentation is only a coarse silhouette option. It does not provide reliable gaps between individual fingers; thin edges, hair, motion, lighting and latency degrade it. Test benefit against inference cost before adding a second model.
Capability checks and None fallback can ship before real depth integration; label status accurately.

## Packages and ownership
Start with :app plus a pure Kotlin :core:gestures module for JVM tests, using package boundaries below. Split more Gradle modules only when ownership/build times justify them.
app/src/main/java/com/hyperpixelacity/app/
- core/common: clocks, errors, debug logger
- core/model: HandFrame, EffectDefinition, capabilities, recording results
- core/ui: theme, accessibility components
- core/camera: CameraController and CameraX adapter
- core/tracking: HandTrackingEngine and MediaPipe adapter
- core/graphics: EffectEngine, EffectRenderer, GL resources
- core/ar: OcclusionProvider and optional session owner
- core/media: RecordingController and export handling
- core/storage: EffectPresetRepository, AppSettingsRepository
- feature/onboarding, studio, effects, editor, recordings, settings

DeviceCapabilityProvider reports GL/codec/camera/AR facts. Hilt wires dependencies; ViewModels expose immutable state and commands. Computer vision stays outside Compose; EGL stays outside ViewModels. DataStore stores settings, versioned local JSON stores presets, and a small file index tracks owned recordings; no Room until queries justify it.

## Budget, privacy and performance gates
No paid runtime dependencies or cloud backend. No INTERNET permission for base studio; inspect merged manifests/transitive dependencies. Public source does not mean publicly uploaded camera media.
Inspect SDK licenses separately from downloadable model licenses. Include notices and original assets only. Verify native libraries on 16 KB page-size Android devices.
Write clips using scoped storage, publish completed entries, share content URIs with temporary grants, and delete only user-selected owned recordings. Camera processing stops outside foreground. No analytics implementation beyond a no-op abstraction.

Performance targets (not measured):
Low: 720p/30 output, tracking 15–20 Hz, 128 particles, analytic glow.
Balanced: 720p/30, tracking 24 Hz, 384 particles, reduced glow buffer.
High: supported 1080p/30 initially, tracking up to 30 Hz, 768 particles; 60 FPS preview only after profiling.
Quality drops under sustained frame-time/thermal pressure with hysteresis. Bounded queues and pooled arrays/particles prevent growth. Test a 10-minute studio session and repeated 60-second clips; profile memory, frame p95, temperature and battery on real hardware.

## Risk register
| Risk | Severity | Mitigation / exit gate |
| --- | --- | --- |
| Build host lacks SDK/Gradle; download timed out | Blocking for local APK | Use working SDK host or verified GitHub Actions build; never claim a generated APK without artifact |
| Dependency/compiler matrix unresolved | High | Resolve pinned artifacts, build Hilt/Compose sample, capture dependency report before feature work |
| GL preview/export differ | Critical | Encode a moving test marker plus effect; decode frames and compare position, crop, mirroring and timestamps |
| Device camera stream combination fails | High | Lower resolution/use-case load; test actual target phones |
| Hand overlap/low light | High | Dwell, identity association, fade/reset; visible guidance |
| Forward throw ambiguity | High | Clearly labeled flick interaction; calibrated heuristic only |
| ARCore camera conflict/depth limitations | High | Separate optional camera owner; None fallback |
| Native ABI or 16 KB incompatibility | High | Inspect AAR/APK and test native load on physical devices |
| Heat, battery and GC pauses | High | Quality caps, buffer pools, sustained-session profiling |
| Encoder/storage interruption | High | Lower-quality retry, finalization/error states, incomplete-entry cleanup |
| Model license/offline availability | High | Bundle verified model + hash + license; airplane-mode first-run test |

## Phases and approval gates
0: This research/architecture record. Await explicit approval before Phase 1, as requested.
1: Gradle wrapper/catalog, manifest, Application/Hilt, MainActivity, Compose navigation/theme, permission/onboarding and CI. Gate: assembled debug APK + lint/test results; otherwise report blocked.
2: CameraController/CameraX studio. Gate: both cameras, denial/regrant, resize, background/reopen.
3: bundled Hand Landmarker and debug overlay. Gate: offline 0/1/2 hands, rotation/mirror, model errors.
4: pure gesture engines and sequence tests. Gate: stable thresholds, loss handling, no repeat throws.
5: GL compositor, trail/orb/pool/tiering. Gate: aligned effects and an early composited recording feasibility spike before investing in all effects.
6: remaining catalog/editor/presets. Gate: each card demonstrates its actual effect and survives preset reload.
7: optional AR/depth prototype and fallback. Gate: no competing camera ownership; real supported-device evidence.
8: complete recording, preview, storage/share/delete, errors. Gate: decoded MP4 has camera AND VFX, no controls, valid duration/rotation, offline.
9: profiling/privacy/native compatibility/release instructions. Gate: physical-device QA evidence and signed build verification.
At each phase list actual file paths, dependencies, risks, complete committed source, build/test commands and QA outcomes. Never label unrun tests passed.

## Build/run and manual QA
Phase 0 is Markdown only: there is no gradlew or installable project yet. No build/run command applies to the current commit.
Planned Phase 1 commands after the wrapper/project exist:
    ./gradlew --version
    ./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
    adb install -r app/build/outputs/apk/debug/app-debug.apk
Later gesture-module gate:
    ./gradlew :core:gestures:test
Later device gate:
    ./gradlew :app:connectedDebugAndroidTest
Windows uses gradlew.bat. Release signing stays outside git; signing secrets must never be committed.

Phase 0 checks performed: repository resolved and confirmed empty; write permission available; official docs reviewed; Java 17 found; Gradle, sdkmanager and adb absent from PATH; SDK root variables absent; direct SDK repository connection timed out.
Not performed: dependency resolution, compilation, model execution, camera tests, encoding, instrumentation or performance tests.
Review checklist: accept calm UI; accept Android 10+ baseline; accept approximate forward throw; accept optional/deferred depth; accept silent initial recording; acknowledge build-host blocker. These are design review items, not device QA passes.

## Official references
[1] https://developer.android.com/blog/posts/android-17-is-here
[2] https://developer.android.com/build/releases/agp-9-1-0-release-notes
[3] https://developer.android.com/develop/ui/compose/bom
[4] https://developer.android.com/jetpack/androidx/releases/camera
[5] https://dagger.dev/hilt/gradle-setup.html
[6] https://dagger.dev/dev-guide/ksp.html
[7] https://developer.android.com/jetpack/androidx/releases/datastore
[8] https://developer.android.com/jetpack/androidx/versions
[9] https://github.com/google-ai-edge/mediapipe/releases/tag/v0.10.35
[10] https://github.com/google/filament
[11] https://developer.android.com/reference/androidx/camera/core/CameraEffect
[12] https://developer.android.com/reference/androidx/camera/core/SurfaceProcessor
[13] https://developer.android.com/media/camera/camerax/video-capture
[14] https://developers.google.com/edge/mediapipe/solutions/vision/hand_landmarker/android
[15] https://developers.google.com/ar/develop/java/camera-sharing
[16] https://developers.google.com/ar/develop/java/depth/developer-guide
[17] https://developers.google.com/edge/mediapipe/solutions/setup_android


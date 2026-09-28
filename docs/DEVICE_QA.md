# Device acceptance checklist

These checks are NOT marked passed until executed on a physical phone. An APK build is not camera/VFX validation.

## Core path
1. Fresh install in airplane mode. Open onboarding, grant camera, show each hand. Model must load without a download.
2. Move index around all four image corners; effect must remain attached. Repeat selfie/rear, mirror on/off, portrait/landscape.
3. Pinch for less than 90 ms: no activation. Hold: crystal appears. Release clearly: disappears. Lose hand: no phantom object.
4. Open two palms for at least 600 ms: orb forms; move palms apart/together. Flick: one projectile, then cooldown. Crossing hands must not cause a launch.
5. Run each effect in Low/Balanced/High. Watch for freezes, graphic errors and excessive heat. Low reduces video quality to SD when supported.
6. Record a 10-second clip while drawing. Play the saved MP4 outside the app: camera AND effect visible, no controls, no black frames, correct orientation/mirror.
7. Record a 60-second clip. Verify automatic finalization, duration, playback, rename, share and delete.
8. Background while recording, rotate, deny/regrant permission, switch camera, retry after a camera conflict. No stuck record button or camera-in-use leak.
9. Low storage: error is visible and no broken file remains in gallery.
10. Ten-minute studio session and repeated clips: record frame times, memory, thermal state and battery. No universal FPS claim is made.

## Known preview limitations
- No ARCore session or depth/person/finger occlusion. Standard compositing is explicit.
- Two-hand orb uses image-space geometry. Forward throw is a flick heuristic, not physical depth measurement.
- Crystal can follow a pinch; two-hand object rotation/scaling is not implemented.
- No audio, pause/resume, trim, haptics, rune recognition, or arbitrary AI object generation.
- One active local adjustment preset; no named preset collection yet.
- No measured FPS overlay or automatic thermal adaptation yet.
- Settings/localization structure exists, but visible strings are English and not all are extracted to resources yet.
- MediaPipe inference uses bounded single-frame processing but still allocates bitmaps; device profiling is needed before further pooling optimization.
- Release signing, model redistribution review and store publication are separate from the test APK.

Do not label this preview production-ready until the above gates pass and limitations are resolved or explicitly accepted as product scope.

# 0.2.1 — Camera and gesture repair preview

Fixes based on reported slow camera flips and unreliable gestures:
- Rebind camera use cases while keeping one hand model and one compositor alive across flips. Ignore stale results from the old camera. Disable repeated Flip taps while switching and show an 8-second recovery message if frames do not arrive.
- Track first analysis-frame arrival after each switch in the debug display. This is not a physical-device benchmark or display latency measurement.
- Use raw landmarks for gesture decisions and smoothed landmarks for visual placement. Pinch has wider start/release hysteresis, a short dwell, palm-length normalization and either-hand selection.
- Accept three extended fingers for palm gestures. Slow inference up to 1.2 seconds between results no longer resets gesture acquisition on every update; no-hand results still reset immediately.
- Renderer expires results by time since receipt (800 ms), not capture age; the previous 200 ms cutoff could hide every effect on a slow CPU. This can keep an old position visible for up to 800 ms during a stall.
- Model timestamp continuity is independent of camera timestamp changes. Detection/presence/tracking thresholds use 0.5.
- Trail shader runs glow exponentials after segment selection instead of inside every segment iteration, with bounding rejection. Detailed crystal highlights, orb veins and circle engravings; no new assets or cloud services.

Verification: CI compiles/lints, runs gesture and filter regression tests, validates shader/model on an emulator, and repeatedly flips front/back cameras while asserting model initialization happens only once. Hardware camera switching speed, user gesture accuracy and visual/recording quality still require real-phone testing. No universal first-attempt or instant-flip guarantee.

Android 10+, GLES3. Debug-signed preview: uninstall an earlier preview if Android rejects the update because of its signing certificate; this removes app settings.

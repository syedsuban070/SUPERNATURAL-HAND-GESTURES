# 0.2.0 — Motion Studio preview

## Changes
- Speed-adaptive One Euro landmark filtering, implemented in pure Kotlin. Low-speed jitter suppression with a wider cutoff for fast gestures.
- A tracking gap longer than 250 ms resets held gestures; effect changes cannot inherit a charged orb. Fully visible landmarks are required for open-palm effects.
- Trails sample over their selected lifespan with bounded storage; resumed drawing cannot connect across a long pause. Shader brightness no longer accumulates once for every overlapping segment.
- Smoothed orb scale, layered animated energy bands and emissive highlight compression.
- Original local vector effect cards; Soft, Studio and Epic adjustment shortcuts; Focus mode hides secondary studio controls.
- Contextual gesture guidance, charge bar and measured tracking-update rate / preprocessing-plus-inference duration in debug mode. These values are not render FPS or tracking confidence.

## Verification scope
CI gates: compilation, lint, 13 pure Kotlin regression tests, actual GLES3 shader compilation and bundled offline model inference on an Android emulator, signing and alignment checks. Physical device tracking, output alignment, recording and thermal validation remain required. No perfect occlusion, VR spatial tracking or generative AI capability is claimed.

## Method reference
Original implementation of the speed-adaptive filtering method described by Casiez, Roussel and Vogel (CHI 2012): https://gery.casiez.net/publications/CHI2012-casiez.pdf . No new dependency or cloud service.

## Installation
Android 10+ and GLES3 required. Preview is debug-signed. A different CI signing key can require uninstalling 0.1 first, removing app settings. This update is not store-signed.

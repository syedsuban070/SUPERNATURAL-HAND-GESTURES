# Test APK and release signing

## Development preview
The CI artifact is a debug-signed APK for testing, not a Play Store release. The application ID is com.hyperpixelacity.app. Minimum Android version is 10 (API 29), and OpenGL ES 3.0 is required.

Only install APKs from this repository or the delivered file. On Android, allow installation from the app used to open the APK when prompted by the operating system. This is a device security choice, not a permission the app can grant itself.

A later debug build can have a different test signing key on a fresh CI runner. If Android refuses to update it, uninstall the earlier test build before reinstalling; this removes app settings but shared-storage recordings normally remain. A production release must retain one privately managed signing identity.

## Release build
Do not commit keystores, passwords, local.properties or signing secrets. Build the unsigned release after the device QA gate passes:

```sh
python3 tools/download_model.py
./gradlew :app:assembleRelease
```

Generate a private signing key with the JDK keytool, keep backups outside the repository, and use Android Build Tools apksigner to sign app/build/outputs/apk/release/app-release-unsigned.apk. Provide passwords interactively, never in source or logs. Verify the signed APK with apksigner verify and zipalign. For Play distribution use a signed AAB and Play App Signing; no Play publication has been performed.

## Before public/store release
- Complete docs/DEVICE_QA.md on actual Android 10+ phones, including a modern 16 KB page-size device.
- Review every bundled model and dependency's redistribution notices.
- Verify permission declarations and privacy disclosures against the final merged manifest.
- Keep the APK/AAB hash, source commit, dependency versions, build logs and test reports together.
- Raise versionCode for every update and preserve signing identity.
- Resolve or explicitly scope out all preview limitations before describing the application as production-ready.

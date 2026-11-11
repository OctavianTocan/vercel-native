# Android builds and releases

This app owns its delivery pipeline. Inventory supplies its existing account/session and the protected shared downloads page; it does not publish this app.

## Build locally

Install JDK 21, Python 3 and Android SDK 37 with build-tools 37.0.0. `.mise.toml` pins the JDK; `android/gradlew` pins Gradle. Set `ANDROID_HOME` to your SDK, then run:

```sh
cd android
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease --no-daemon --max-workers=2
```

The debug APK is `android/app/build/outputs/apk/debug/app-debug.apk`. It uses `com.octaviantocan.vercelnative.debug`, cannot replace the signed release, and has production self-updating disabled. Password account sign-in uses the existing server. Google sign-in on a development build additionally needs its own debug-package/certificate registration; this initial setup registers the release app only.

## Release changes

For an Android runtime/build change, add `android/changes/YYYY-MM-DD-description.md` with a `## YYYY-MM-DD` heading and `- ` release notes. Do not assign a version manually. `python3 -B deploy/release.py changelog` combines the seed changelog and dated notes in main's first-parent arrival order. A runtime change without a version increase fails CI.

`Android verify` runs release-tool tests, Kotlin unit tests, a debug build and release lint. Every job uses the repository-scoped `vercel-linux` runner on `mini-002-netcup`. Fork pull requests do not execute on this persistent runner. Verification has no release secrets.

After successful verification of an own-repository main push, `Android release` checks out that exact commit, validates the signing certificate, builds and signs the APK, and publishes it through S3 with pinned dependencies from `deploy/requirements.txt`. Manual dispatch is restricted to main and repeats verification. Production releases are serialized.

The publisher uploads the versioned APK, signs in with the existing automated-test account, downloads that candidate through Inventory's protected route, and verifies its size, SHA-256, application ID, version and signing certificate. Only then does it back up and conditionally replace the active manifest using its original ETag. It reads back the active feed and APK again. `--if-needed` verifies an already-published identical version without replacing its bytes.

## Identities and secrets

- Release package: `com.octaviantocan.vercelnative`.
- Release key alias: `vercel-native`; keep this signing identity for every update.
- Certificate SHA-256: `55c8a6ff4be043eb3e9797ef52724016d9f4ee8bcd099501e13f2d179cf98155`.
- Certificate SHA-1: `E1:2E:53:36:F7:A8:E5:DB:58:62:E0:7B:DD:C9:38:1A:11:02:77:03`.
- Google Android client: `Vercel Native Android (release)`, in the existing `mymind-replica` project, registered for this package and SHA-1. The app uses Inventory's existing Web client ID as its server audience.
- Private R2 bucket: `vercel-native`; public development access is disabled. Its publisher credential is Object Read & Write for this bucket only.
- GitHub environment: `production`, main deployment branch only. Encrypted secrets: `ANDROID_RELEASE_KEYSTORE_BASE64`, `ANDROID_RELEASE_PASSWORD`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, `TEST_USER_NAME`, `TEST_USER_PASSWORD`.
- Repository variables: `ANDROID_RELEASE_CERT_SHA256`, `ANDROID_GOOGLE_WEB_CLIENT_ID`, `CLOUDFLARE_ACCOUNT_ID`. Client IDs and certificate fingerprints are public identifiers.

The canonical signing key and password are owner-only files on mini-002 under `~/.config/vercel-native/`. GitHub keeps encrypted CI copies. The one-time R2 credential was transferred directly to encrypted CI secrets and an owner-only local configuration file. Do not copy any of these values into this repository, logs or APKs. Reuse the existing key when restoring CI; generating a replacement prevents updates to installed copies.

## Download and recovery

Shared page: <https://mymind.octaviantocan.com/download>. Feed: `/download/vercel/android.json`. Latest download: `/download/vercel/latest.apk`. Versioned files: `/download/vercel/vercel-native-X.Y.Z.apk`. All use the existing Inventory sign-in gate and session, with no new authentication scopes or account system.

The independent bucket stores `manifest.json`, its previous value as `previous-manifest.json`, and versioned APKs. A failure before promotion leaves the current feed in place. Keep previous APKs. To recover a bad release, prefer publishing a corrected release with a higher versionCode and the same signing key: Android cannot install an older code over an already-updated app. Restoring the previous manifest can stop offering a bad candidate to older installations, but it cannot downgrade an installed app. Verify the previous feed and APK before an authorized restoration.

Updates are mandatory after the app learns of a newer release. The offer survives process restarts and failed requests. The updater checks on resume and with Android WorkManager, verifies package size/hash and opens Android's installer. Android controls installation confirmation and permission to install apps from this source. Google sign-in and the full installation/update round trip require verification on the owner's phone.

## Runner operations

Use `tailscale ssh octaviantocan@mini-002-netcup`. The repository-specific service is `actions.runner.OctavianTocan-vercel-native.vercel-linux-1.service`, installed in `/opt/actions-runner-vercel-1`, running as unprivileged `vercelgha`. Existing Inventory and other project runners remain separate.

Inspect the service and exact GitHub release run when a build fails. Keep release secrets away from PR verification and untrusted fork code. Re-run `Android release` on main after a transient failure; the publisher verifies existing release metadata before deciding whether an upload is needed.

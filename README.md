# Vercel Native

A native Kotlin/Jetpack Compose Android app that will bring Vercel’s signed-in dashboard to a phone. This is an independent, unofficial project.

The first milestone is the delivery setup: a small native shell, Inventory account sign-in for protected downloads, mandatory in-app updates, and an independent signed release pipeline. Dashboard implementation comes later.

## Download

[Shared downloads page](https://mymind.octaviantocan.com/download) — sign in with your existing Inventory account. The Vercel entry is being connected during initial setup.

## Build

Use JDK 21 and Android SDK/build-tools 37. The Gradle wrapper and Kotlin/Compose versions are pinned.

```sh
cd android
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease --no-daemon --max-workers=2
```

Debug builds use a separate package ID and do not install production updates. Release builds require the project’s signing key and environment configuration; keys and APKs are never committed.

## Delivery

CI runs on the self-hosted `vercel-linux` runner on mini-002-netcup. Verification runs without signing or publication secrets. A successful verified main commit is signed and published by this repository’s own release job. Public fork code is not executed on the persistent worker.

This repository owns its signing identity, private R2 bucket, APKs, release feed and publisher. Inventory’s existing service only provides the shared generic downloads page, sign-in and authenticated serving route. Its publisher and release manifest do not manage this app.

Updates are mandatory. The app checks on resume and in the background, verifies downloads, and uses Android’s installer. A release offered to the installed app must be installed before continuing.

## Development decisions

- [Agent instructions](AGENTS.md)
- [Behavior contracts](CONTRACTS)
- [Decisions](docs/decisions.md)
- [Release notes](android/CHANGELOG.md)
- [Build, CI, signing and recovery guide](docs/android-releases.md)
- [Reused code provenance](docs/provenance.md)

Add dated notes under `android/changes/` for app runtime changes. Versions are assigned from main’s merge history. This project contains no Inventory library data, private Git history, private AI-kit dependency or application backend.

## License

[MIT](LICENSE). Third-party libraries retain their own licenses.

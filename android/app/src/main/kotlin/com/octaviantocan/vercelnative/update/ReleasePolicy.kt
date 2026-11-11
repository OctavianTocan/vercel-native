package com.octaviantocan.vercelnative.update

/** Rejects another app's feed, unsafe paths and malformed package descriptions before downloading. */
fun validateRelease(release: ReleaseInfo) {
    require(release.applicationId == "com.octaviantocan.vercelnative") { "The update belongs to another app" }
    require(release.versionName.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+"))) { "Invalid update version" }
    require(release.versionCode > 0 && release.bytes in 1..250_000_000L) { "Invalid update size or version" }
    require(release.sha256.matches(Regex("[a-fA-F0-9]{64}"))) { "Invalid update checksum" }
    require(release.url == "/download/vercel/vercel-native-${release.versionName}.apk") { "Invalid update address" }
}

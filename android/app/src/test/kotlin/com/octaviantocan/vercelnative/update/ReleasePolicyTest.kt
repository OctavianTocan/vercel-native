package com.octaviantocan.vercelnative.update

import org.junit.Assert.*
import org.junit.Test

class ReleasePolicyTest {
    private fun release() = ReleaseInfo("com.octaviantocan.vercelnative", "0.1.1", 2, "/download/vercel/vercel-native-0.1.1.apk", 12, "a".repeat(64))
    @Test fun acceptsThisAppsImmutablePackage() { validateRelease(release()) }
    @Test fun rejectsInventoryFeed() { rejected(release().copy(applicationId = "com.octaviantocan.mymind")) }
    @Test fun rejectsExternalOrUnrelatedDownload() {
        rejected(release().copy(url = "https://example.com/steal.apk"))
        rejected(release().copy(url = "/download/mymind.apk"))
        rejected(release().copy(url = "/download/vercel/../secret.apk"))
    }
    @Test fun rejectsInvalidSizeOrHash() {
        rejected(release().copy(bytes = 0))
        rejected(release().copy(sha256 = "bad"))
    }
    @Test fun comparesVersionCodesWithoutDowngrading() {
        assertTrue(isNewer(2, 1)); assertFalse(isNewer(1, 1)); assertFalse(isNewer(1, 2))
    }
    private fun rejected(value: ReleaseInfo) {
        assertThrows(IllegalArgumentException::class.java) { validateRelease(value) }
    }
}

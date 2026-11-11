package com.octaviantocan.vercelnative.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Hands a verified package to Android. */
interface ApkInstaller {
    /**
     * Starts the install of [file]; Android reports the outcome to [InstallStatusReceiver].
     *
     * @param file Verified package.
     * @param profile Whether to install the package's Baseline Profile beside it, so Android compiles at install.
     * @param beforeCommit Called with the session's id before it is committed, since Android can answer at once.
     * @throws java.io.IOException when the session cannot be written or committed.
     */
    suspend fun install(file: File, profile: Boolean, beforeCommit: (Int) -> Unit)
}

/** [ApkInstaller] that commits a `PackageInstaller` session which asks for no user action where Android allows it. */
class SessionApkInstaller(private val context: Context) : ApkInstaller {
    override suspend fun install(file: File, profile: Boolean, beforeCommit: (Int) -> Unit) = withContext(Dispatchers.IO) {
        val installer = context.packageManager.packageInstaller
        installer.mySessions.forEach { runCatching { installer.abandonSession(it.sessionId) } }
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(file.length())
            setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            file.inputStream().use { input ->
                session.openWrite(SESSION_FILE, 0, file.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }
            // With its profile beside it, Android compiles the app's hot code while installing, not overnight.
            (if (profile) dexMetadata(file) else null)?.let { metadata ->
                session.openWrite(PROFILE_FILE, 0, metadata.size.toLong()).use { output ->
                    output.write(metadata)
                    session.fsync(output)
                }
            }
            beforeCommit(id)
            val callback = Intent(context, InstallStatusReceiver::class.java).setPackage(context.packageName)
                .putExtra(PackageInstaller.EXTRA_SESSION_ID, id)
            val pending = PendingIntent.getBroadcast(context, id, callback, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
            session.commit(pending.intentSender)
        }
    }

    private companion object {
        const val SESSION_FILE = "vercel-native.apk"

        /** Named after [SESSION_FILE], as Android pairs a package with its dex metadata by name. */
        const val PROFILE_FILE = "vercel-native.dm"
    }
}

/**
 * The dex metadata Android compiles a package's hot code from at install: the Baseline Profile the build put in [apk]
 * (`assets/dexopt/baseline.prof` and `.profm`) as `primary.prof` and `primary.profm` in a zip, the form of
 * `adb install-multiple app.apk app.dm`.
 *
 * @param apk The package to install.
 * @returns The `.dm` file's bytes, or null when the package carries no profile or cannot be read.
 */
internal fun dexMetadata(apk: File): ByteArray? = runCatching {
    ZipFile(apk).use { zip ->
        val profile = zip.getEntry("assets/dexopt/baseline.prof") ?: return null
        val meta = zip.getEntry("assets/dexopt/baseline.profm") ?: return null
        ByteArrayOutputStream().also { bytes ->
            ZipOutputStream(bytes).use { out ->
                for ((name, entry) in listOf("primary.prof" to profile, "primary.profm" to meta)) {
                    out.putNextEntry(ZipEntry(name))
                    zip.getInputStream(entry).use { it.copyTo(out) }
                    out.closeEntry()
                }
            }
        }.toByteArray()
    }
}.getOrNull()

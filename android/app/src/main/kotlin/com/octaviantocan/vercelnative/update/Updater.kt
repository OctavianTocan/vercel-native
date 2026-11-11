package com.octaviantocan.vercelnative.update

import android.content.Intent
import android.content.pm.PackageInstaller
import com.octaviantocan.vercelnative.data.AppJson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * Looks for a newer release, downloads and verifies it, and hands it to Android to install. The screens, the
 * background worker and the install receiver all use the one instance, and read its [state].
 *
 * @param client HTTP client that adds the app token.
 * @param downloadsUrl Origin of the downloads server.
 * @param installedCode `versionCode` of the running app.
 * @param directory Folder for downloaded packages; created when missing.
 * @param installer Starts the install of a verified package.
 * @param prefs Persistent facts such as the time of the last check.
 * @param clock Current time in milliseconds.
 * @param io Dispatcher for network and file work.
 */
class Updater(
    private val client: OkHttpClient,
    private val downloadsUrl: String,
    private val installedCode: Int,
    private val directory: File,
    private val installer: ApkInstaller,
    private val prefs: UpdatePrefs,
    private val clock: () -> Long = System::currentTimeMillis,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutableState = MutableStateFlow<UpdateState>(UpdateState.None)
    private val mutableOffer = MutableStateFlow(restoreOffer())
    private val lock = Mutex()

    /**
     * The newer release the server offers, or null when there is none. While it is set the app shows only the update
     * screen. It starts as the last offer the phone heard of, so the screen is there from the first frame of a start,
     * before the server answers again; a failed check keeps it and a server with nothing newer takes it away.
     */
    val offer: StateFlow<ReleaseInfo?> = mutableOffer

    /** What the updater is doing. */
    val state: StateFlow<UpdateState> = mutableState

    private val mutablePrompt = MutableStateFlow<Intent?>(null)

    /**
     * Android's install prompt while it waits for the user's tap, or null. The update screen opens it from the visible
     * activity, and offers it again when the user comes back without finishing it.
     */
    val prompt: StateFlow<Intent?> = mutablePrompt

    private val mutablePromptRequest = MutableStateFlow(0)

    /** Changes each time Android sends a new prompt, so the update screen opens it once. */
    val promptRequest: StateFlow<Int> = mutablePromptRequest

    // The session the last install committed, and the package it installs. Android reports every session it ends,
    // including ones a retry abandoned, so a status for any other session is stale.
    @Volatile
    private var session: Int? = null

    @Volatile
    private var installing: UpdateState.Ready? = null

    /** True while the app is on screen; set by the activity. */
    @Volatile
    var foreground: Boolean = false

    /** @return The release the current state is about, or null. */
    fun currentRelease(): ReleaseInfo? = when (val now = mutableState.value) {
        is UpdateState.Downloading -> now.release
        is UpdateState.Ready -> now.release
        is UpdateState.Installing -> now.release
        is UpdateState.Failed -> now.release
        else -> null
    }

    /**
     * Checks only when the last check is older than five minutes, so coming back to the app often does not ask the
     * server as often.
     *
     * @return The state after the call.
     */
    suspend fun checkIfStale(): UpdateState =
        if (clock() - prefs.lastCheckMs > STALE_MS) check() else mutableState.value

    /**
     * Asks the server for the newest release and downloads it when it is newer than the installed app. A second call
     * waits for the first.
     *
     * @return The state after the check: up to date, ready, installing or failed.
     */
    suspend fun check(): UpdateState = lock.withLock {
        if (mutableState.value is UpdateState.Installing) return@withLock mutableState.value
        val before = mutableState.value
        if (before !is UpdateState.Ready) mutableState.value = UpdateState.Checking
        val release = try {
            fetchRelease()
        } catch (error: IOException) {
            return@withLock fail(before, error.message?.takeIf { error is UpdateException } ?: "No connection to the server", technical(error))
        }
        prefs.lastCheckMs = clock()
        if (release == null || !isNewer(release.versionCode, installedCode)) {
            saveOffer(null)
            withContext(io) { directory.deleteRecursively() }
            mutableState.value = UpdateState.UpToDate
            return@withLock mutableState.value
        }
        saveOffer(release)
        try {
            val file = download(release)
            mutableState.value = UpdateState.Ready(release, file)
        } catch (error: IOException) {
            mutableState.value = UpdateState.Failed(error.message?.takeIf { error is UpdateException } ?: "Download interrupted", release, technical(error))
        }
        mutableState.value
    }

    /** Set once Android fails an install, so later installs in this process leave the Baseline Profile out. */
    private var profileRefused = false

    /** Starts the install of the verified package; nothing happens unless the state is ready. */
    suspend fun install() {
        val ready = mutableState.value as? UpdateState.Ready ?: return
        mutableState.value = UpdateState.Installing(ready.release)
        mutablePrompt.value = null
        installing = ready
        try {
            installer.install(ready.file, profile = !profileRefused) { session = it }
        } catch (error: IOException) {
            mutableState.value = UpdateState.Failed("Could not start the install: ${error.message ?: "unknown error"}", ready.release, technical(error))
        } catch (error: SecurityException) {
            mutableState.value = UpdateState.Failed("Android refused the install: ${error.message ?: "not allowed"}", ready.release, technical(error))
        }
    }

    /**
     * Applies the status Android reported for the committed session.
     *
     * @param status A `PackageInstaller.STATUS_*` value.
     * @param message Android's explanation, or null.
     * @param confirm The system prompt's intent for `STATUS_PENDING_USER_ACTION`, or null.
     * @param sessionId The session the status is about, or -1 when unknown.
     * @return What the receiver does next.
     */
    fun onInstallStatus(status: Int, message: String?, confirm: Intent?, sessionId: Int = -1): StatusAction {
        val current = session
        if (sessionId != -1 && current != null && sessionId != current) return StatusAction.Stale
        val release = currentRelease()
        return when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> when {
                confirm == null -> {
                    mutableState.value = UpdateState.Failed("Android asked for a confirmation without a prompt", release, statusDetail(status, message))
                    StatusAction.None
                }
                else -> {
                    mutablePrompt.value = confirm
                    mutablePromptRequest.value += 1
                    if (foreground) StatusAction.None else StatusAction.NotifyPrompt(confirm)
                }
            }
            PackageInstaller.STATUS_SUCCESS -> StatusAction.None.also { mutablePrompt.value = null }
            // Saying no in Android's prompt is a choice, not a failure: the package stays and Update is offered again.
            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                mutablePrompt.value = null
                val ready = installing
                mutableState.value = if (ready != null && ready.file.isFile) ready else UpdateState.Failed(failureText(status, message), release, statusDetail(status, message))
                StatusAction.None
            }
            else -> {
                mutablePrompt.value = null
                // Should Android ever refuse a package because of its profile, the next try installs it alone.
                profileRefused = true
                mutableState.value = UpdateState.Failed(failureText(status, message), release, statusDetail(status, message))
                StatusAction.None
            }
        }
    }

    /** The exception's class and message, for the failure's technical detail. */
    private fun technical(error: Throwable): String = listOfNotNull(error::class.java.name, error.message).joinToString(": ")

    /** Android's status code by name with its own message, for the failure's technical detail. */
    private fun statusDetail(status: Int, message: String?): String {
        val name = STATUS_NAMES[status] ?: "STATUS_$status"
        return "PackageInstaller $name ($status)" + (message?.takeIf { it.isNotBlank() }?.let { "; Android said: $it" } ?: "")
    }

    private fun failureText(status: Int, message: String?): String = when (status) {
        PackageInstaller.STATUS_FAILURE_ABORTED -> "The install was cancelled"
        PackageInstaller.STATUS_FAILURE_CONFLICT -> "Android found a conflict with the installed app"
        PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "This version does not fit this phone"
        PackageInstaller.STATUS_FAILURE_INVALID -> "Android found the package invalid"
        PackageInstaller.STATUS_FAILURE_STORAGE -> "Not enough storage to install"
        PackageInstaller.STATUS_FAILURE_BLOCKED -> "Android blocked the install"
        else -> message?.takeIf { it.isNotBlank() } ?: "The install failed"
    }

    private fun restoreOffer(): ReleaseInfo? {
        val saved = prefs.offerJson ?: return null
        val release = runCatching { AppJson.decodeFromString(ReleaseInfo.serializer(), saved) }.getOrNull()
        if (release == null || !isNewer(release.versionCode, installedCode)) {
            prefs.offerJson = null
            return null
        }
        return release
    }

    private fun saveOffer(release: ReleaseInfo?) {
        mutableOffer.value = release
        prefs.offerJson = release?.let { AppJson.encodeToString(ReleaseInfo.serializer(), it) }
    }

    private fun fail(before: UpdateState, reason: String, detail: String): UpdateState {
        mutableState.value = if (before is UpdateState.Ready) before else UpdateState.Failed(reason, detail = detail)
        return mutableState.value
    }

    private suspend fun fetchRelease(): ReleaseInfo? = withContext(io) {
        val request = Request.Builder().url(downloadsUrl.trimEnd('/') + "/download/vercel/android.json").get().build()
        client.newCall(request).execute().use { response ->
            when {
                response.code == 404 -> throw UpdateException("The update feed is not available yet")
                response.code == 401 -> throw UpdateException("Signed out of the downloads server")
                !response.isSuccessful -> throw UpdateException("The server answered ${response.code}")
                else -> try {
                    AppJson.decodeFromString(ReleaseInfo.serializer(), response.body.string()).also { validateRelease(it) }
                } catch (_: IllegalArgumentException) {
                    throw UpdateException("The release description is unreadable")
                }
            }
        }
    }

    private suspend fun download(release: ReleaseInfo): File = withContext(io) {
        directory.mkdirs()
        val target = File(directory, "vercel-native-${release.versionCode}.apk")
        directory.listFiles()?.filter { it.name != target.name }?.forEach { it.delete() }
        if (target.isFile && matches(target, release)) return@withContext target
        target.delete()
        val part = File(directory, target.name + ".part")
        val url = downloadsUrl.trimEnd('/').toHttpUrl().resolve(release.url) ?: throw UpdateException("The package address is invalid")
        mutableState.value = UpdateState.Downloading(release, 0f)
        client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
            if (!response.isSuccessful) throw UpdateException("The package download answered ${response.code}")
            val digest = MessageDigest.getInstance("SHA-256")
            var received = 0L
            var shown = 0f
            part.outputStream().use { out ->
                val buffer = ByteArray(BUFFER)
                val source = response.body.byteStream()
                while (true) {
                    val read = source.read(buffer)
                    if (read < 0) break
                    out.write(buffer, 0, read)
                    digest.update(buffer, 0, read)
                    received += read
                    if (received > release.bytes) throw UpdateException("The package is larger than its release description")
                    val progress = if (release.bytes > 0) (received.toFloat() / release.bytes).coerceIn(0f, 1f) else 0f
                    if (progress - shown >= PROGRESS_STEP) {
                        shown = progress
                        mutableState.value = UpdateState.Downloading(release, progress)
                    }
                }
            }
            val hex = digest.digest().joinToString("") { "%02x".format(it) }
            if (received != release.bytes || hex != release.sha256.lowercase()) {
                part.delete()
                val what = if (received != release.bytes) "size" else "checksum"
                throw UpdateException("The downloaded package failed its $what check")
            }
        }
        if (!part.renameTo(target)) throw UpdateException("The downloaded package could not be saved")
        target
    }

    private fun matches(file: File, release: ReleaseInfo): Boolean {
        if (file.length() != release.bytes) return false
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) } == release.sha256.lowercase()
    }

    private class UpdateException(message: String) : IOException(message)

    private companion object {
        const val STALE_MS = 5 * 60 * 1000L
        const val BUFFER = 64 * 1024
        const val PROGRESS_STEP = 0.01f
        val STATUS_NAMES = mapOf(
            PackageInstaller.STATUS_FAILURE to "STATUS_FAILURE",
            PackageInstaller.STATUS_FAILURE_ABORTED to "STATUS_FAILURE_ABORTED",
            PackageInstaller.STATUS_FAILURE_BLOCKED to "STATUS_FAILURE_BLOCKED",
            PackageInstaller.STATUS_FAILURE_CONFLICT to "STATUS_FAILURE_CONFLICT",
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE to "STATUS_FAILURE_INCOMPATIBLE",
            PackageInstaller.STATUS_FAILURE_INVALID to "STATUS_FAILURE_INVALID",
            PackageInstaller.STATUS_FAILURE_STORAGE to "STATUS_FAILURE_STORAGE",
            PackageInstaller.STATUS_PENDING_USER_ACTION to "STATUS_PENDING_USER_ACTION",
        )
    }
}

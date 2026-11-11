package com.octaviantocan.vercelnative.update

import kotlinx.serialization.Serializable
import java.io.File

/**
 * One past version in the release description.
 *
 * @property versionName Dotted version.
 * @property versionCode Build number.
 * @property date Release date as `YYYY-MM-DD`.
 * @property notes The version's changelog lines.
 */
@Serializable
data class ReleaseHistory(val versionName: String, val versionCode: Int, val date: String = "", val notes: List<String> = emptyList())

/**
 * The newest published Android package, as `GET /download/android.json` describes it.
 *
 * @property versionName Dotted version.
 * @property versionCode Build number; the package is an update when it is higher than the installed one.
 * @property url Path of the package on the downloads server.
 * @property bytes Exact size of the package.
 * @property sha256 Lower-case hex SHA-256 of the package.
 * @property built When the package was built.
 * @property notes This version's changelog lines.
 * @property history Every version's notes, newest first.
 */
@Serializable
data class ReleaseInfo(
    val applicationId: String,
    val versionName: String,
    val versionCode: Int,
    val url: String,
    val bytes: Long,
    val sha256: String,
    val built: String = "",
    val notes: List<String> = emptyList(),
    val history: List<ReleaseHistory> = emptyList(),
)

/** What the updater is doing, shared by the screens, the worker and the install receiver. */
sealed interface UpdateState {
    /** No check has finished yet in this process. */
    data object None : UpdateState

    /** The installed version is the newest. */
    data object UpToDate : UpdateState

    /** Asking the server for the newest release. */
    data object Checking : UpdateState

    /**
     * Downloading [release].
     *
     * @property release The release being downloaded.
     * @property progress Fraction from 0 to 1.
     */
    data class Downloading(val release: ReleaseInfo, val progress: Float) : UpdateState

    /**
     * [release] is downloaded and verified.
     *
     * @property release The release.
     * @property file The verified package.
     */
    data class Ready(val release: ReleaseInfo, val file: File) : UpdateState

    /**
     * Android is installing [release].
     *
     * @property release The release.
     */
    data class Installing(val release: ReleaseInfo) : UpdateState

    /**
     * The last check, download or install failed.
     *
     * @property reason Short wording of the cause.
     * @property release The release involved, or null when the check itself failed.
     * @property detail The technical cause behind [reason] (exception or Android's status and message), for a feedback
     *   report; empty when there is nothing more to say.
     */
    data class Failed(val reason: String, val release: ReleaseInfo? = null, val detail: String = "") : UpdateState
}

/**
 * Whether a published version replaces the installed one.
 *
 * @param published The published `versionCode`.
 * @param installed The installed `versionCode`.
 * @return True when [published] is higher.
 */
fun isNewer(published: Int, installed: Int): Boolean = published > installed

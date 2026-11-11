package com.octaviantocan.vercelnative.update

import android.content.Context
import android.content.SharedPreferences

/** The updater's small persistent facts. */
class UpdatePrefs(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE))

    /** Wall-clock time of the last finished check, or 0 when none. */
    var lastCheckMs: Long
        get() = prefs.getLong(LAST_CHECK, 0L)
        set(value) = prefs.edit().putLong(LAST_CHECK, value).apply()

    /** The `versionCode` whose "What's new" sheet was already handled, or null on a fresh install. */
    var shownChangelogCode: Int?
        get() = if (prefs.contains(SHOWN_CODE)) prefs.getInt(SHOWN_CODE, 0) else null
        set(value) {
            val editor = prefs.edit()
            if (value == null) editor.remove(SHOWN_CODE) else editor.putInt(SHOWN_CODE, value)
            editor.apply()
        }

    /** JSON of the newest release the server last offered over the installed app, or null when none. */
    var offerJson: String?
        get() = prefs.getString(OFFER, null)
        set(value) {
            val editor = prefs.edit()
            if (value == null) editor.remove(OFFER) else editor.putString(OFFER, value)
            editor.apply()
        }

    /** The highest `versionCode` the user was already told about in a notification, or 0. */
    var notifiedCode: Int
        get() = prefs.getInt(NOTIFIED_CODE, 0)
        set(value) = prefs.edit().putInt(NOTIFIED_CODE, value).apply()

    /** True once the notification permission was asked for. */
    var askedNotifications: Boolean
        get() = prefs.getBoolean(ASKED_NOTIFICATIONS, false)
        set(value) = prefs.edit().putBoolean(ASKED_NOTIFICATIONS, value).apply()

    private companion object {
        const val NAME = "updates"
        const val LAST_CHECK = "lastCheckMs"
        const val SHOWN_CODE = "shownChangelogCode"
        const val ASKED_NOTIFICATIONS = "askedNotifications"
        const val OFFER = "offer"
        const val NOTIFIED_CODE = "notifiedCode"
    }
}

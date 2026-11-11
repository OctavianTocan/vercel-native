package com.octaviantocan.vercelnative.update

import android.app.*
import android.content.*
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat
import com.octaviantocan.vercelnative.*

sealed interface StatusAction {
    data object None : StatusAction
    data object Stale : StatusAction
    data class NotifyPrompt(val intent: Intent) : StatusAction
}

class InstallStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as VercelApp
        val action = app.updater.onInstallStatus(
            intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE),
            intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE),
            IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java),
            intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1))
        if (action is StatusAction.NotifyPrompt) UpdateNotifications.ready(context)
    }
}

object UpdateNotifications {
    fun notification(context: Context, text: String): Notification {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("updates", "App updates", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 1, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(context, "updates").setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Vercel Native update").setContentText(text).setContentIntent(open).build()
    }
    fun ready(context: Context) {
        if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        context.getSystemService(NotificationManager::class.java).notify(1, notification(context, "Open the app to finish updating."))
    }
}

/** Keeps a user-started update alive if the activity leaves the foreground. */
class UpdateService : Service() {
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(2, UpdateNotifications.notification(this, "Downloading and installing the update."))
        return START_NOT_STICKY
    }
}

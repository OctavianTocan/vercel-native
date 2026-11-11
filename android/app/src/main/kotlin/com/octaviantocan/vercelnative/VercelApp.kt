package com.octaviantocan.vercelnative

import android.app.Application
import android.content.Context
import androidx.work.*
import com.octaviantocan.vercelnative.data.DownloadAccount
import com.octaviantocan.vercelnative.update.*
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.TimeUnit

class VercelApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    lateinit var account: DownloadAccount
    lateinit var updater: Updater
    override fun onCreate() {
        super.onCreate()
        account = DownloadAccount(this)
        updater = Updater(account.http, BuildConfig.DOWNLOADS_ORIGIN, BuildConfig.VERSION_CODE,
            File(cacheDir, "updates"), SessionApkInstaller(this), UpdatePrefs(this))
        scope.launch { account.session.restore() }
        if (!BuildConfig.DEBUG) {
            val work = PeriodicWorkRequestBuilder<UpdateCheckWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork("vercel-native-updates", ExistingPeriodicWorkPolicy.KEEP, work)
        }
    }
}

/** Downloads in the background; installation begins from the mandatory screen when the app opens. */
class UpdateCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as VercelApp
        app.account.session.restore()
        if (app.account.session.token == null || BuildConfig.DEBUG) return Result.success()
        val result = app.updater.check()
        if (app.updater.offer.value != null) UpdateNotifications.ready(app)
        return if (result is UpdateState.Failed) Result.retry() else Result.success()
    }
}

package org.example.flipweather

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class RefreshWorker(c: Context, p: WorkerParameters) : Worker(c, p) {
    override fun doWork(): Result {
        var ok = true
        for (l in Store.locations(applicationContext)) {
            try { Store.putWx(applicationContext, l, Weather.fetch(l)) } catch (e: Exception) { ok = false }
        }
        WeatherWidget.render(applicationContext)   // also refreshes battery
        return if (ok) Result.success() else Result.retry()
    }

    companion object {
        private val net = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        fun schedule(c: Context) {
            WorkManager.getInstance(c).enqueueUniquePeriodicWork(
                "periodic", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequest.Builder(RefreshWorker::class.java, 15, TimeUnit.MINUTES).setConstraints(net).build()
            )
        }
        fun now(c: Context) {
            WorkManager.getInstance(c).enqueueUniqueWork(
                "now", ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequest.Builder(RefreshWorker::class.java).setConstraints(net).build()
            )
        }
    }
}

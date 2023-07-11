package com.nll.store.update

import android.content.Context
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nll.store.api.StoreApiManager
import com.nll.store.log.CLog
import java.text.DateFormat
import java.util.concurrent.TimeUnit

class PeriodicUpdateCheckWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (CLog.isDebug()) {
            CLog.log(logTag, "PeriodicUpdateCheckWorker run @ ${DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(System.currentTimeMillis())}")
        }

        StoreApiManager.getInstance(applicationContext).checkUpdates()

        return Result.success()
    }

companion object{
    private val logTag = "PeriodicUpdateCheckWorker"
    fun enqueueUpdateCheck(context: Context){

        /**
         * Prevent java.lang.IllegalStateException: WorkManager is not initialized properly
         * We somehow get here while WorkManager is not initialized since targeting Android 14
         * TODO May be investigate later
         */
        val isWorkManagerInitialized = WorkManager.isInitialized()
        if (CLog.isDebug()) {
            CLog.log(logTag, "isWorkManagerInitialized: $isWorkManagerInitialized")
        }
        if(!isWorkManagerInitialized){
            WorkManager.initialize(context, Configuration.Builder().build())
        }



        val tag = "periodic-update-check"
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicDeleteWorkRequest = PeriodicWorkRequest.Builder(PeriodicUpdateCheckWorker::class.java, 24, TimeUnit.HOURS).apply {
            addTag(tag)
            setConstraints(constraints)
            setInitialDelay(24, TimeUnit.HOURS)
        }.build()



        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(tag, ExistingPeriodicWorkPolicy.KEEP, periodicDeleteWorkRequest)
    }
}
}
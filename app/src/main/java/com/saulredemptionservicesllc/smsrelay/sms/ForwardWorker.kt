package com.saulredemptionservicesllc.smsrelay.sms

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.saulredemptionservicesllc.smsrelay.data.Status
import com.saulredemptionservicesllc.smsrelay.data.Store
import java.util.concurrent.TimeUnit

/** Re-sends a forward that failed (e.g. no signal), with increasing delays. */
class ForwardWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val id = inputData.getString(KEY_LOG_ID) ?: return Result.success()
        val store = Store.get(applicationContext)
        val entry = store.findLog(id) ?: return Result.success()
        if (entry.status != Status.RETRYING) return Result.success()
        val next = store.updateLog(id) { it.copy(status = Status.SENDING, attempts = it.attempts + 1) }
            ?: return Result.success()
        Forwarder.send(applicationContext, next)
        return Result.success()
    }

    companion object {
        private const val KEY_LOG_ID = "log_id"

        fun scheduleRetry(context: Context, logId: String, attemptsSoFar: Int) {
            val delayMinutes = if (attemptsSoFar <= 1) 1L else 5L
            val request = OneTimeWorkRequestBuilder<ForwardWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setInputData(workDataOf(KEY_LOG_ID to logId))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("retry-$logId", ExistingWorkPolicy.REPLACE, request)
        }
    }
}

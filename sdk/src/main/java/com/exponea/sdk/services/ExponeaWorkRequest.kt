package com.exponea.sdk.services

import android.os.Looper
import androidx.work.Worker
import com.exponea.sdk.Exponea
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.util.Logger
import java.util.*
import java.util.concurrent.CountDownLatch

class ExponeaWorkRequest : Worker() {
    companion object {
        const val KEY_CONFIG_INPUT = "KeyConfigInput"
    }

    override fun doWork(): Result {
        Logger.d(this, "doWork -> Starting...")
        val countDownLatch = CountDownLatch(1)
        val config = ExponeaConfigRepository.get(applicationContext) ?: return Result.FAILURE

        if (!Exponea.isInitialized) {
            Looper.prepare()
            Exponea.init(applicationContext, config)
        }

        try {
            Exponea.component.sessionManager.trackSessionEnd()
            Exponea.component.flushManager.onFlushFinishListener = {
                Logger.d(this, "doWork -> Finished")
                countDownLatch.countDown()
            }

            Logger.d(this, "doWork -> Starting flushing data")
            Exponea.component.flushManager.flushData()

            try {
                countDownLatch.await()
            } catch (e: InterruptedException) {
                Logger.e(this, "doWork -> countDownLatch was interrupted", e)
                return Result.FAILURE
            }

            Logger.d(this, "doWork -> Success!")

            return Result.SUCCESS
        } catch (e: Exception) {
            return Result.FAILURE
        }
    }
}
package com.example.inventory

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.inventory.data.AppContainer
import com.example.inventory.data.AppDataContainer
import java.util.concurrent.TimeUnit

class InventoryApplication : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)

        //-- バックグラウンドでの通知定期更新ジョブを登録 --//
        setupPeriodicTaskUpdate()
    }

    private fun setupPeriodicTaskUpdate() {
        val repeatingRequest = PeriodicWorkRequestBuilder<UpdateTaskCountWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "UpdateTaskCountWork",
            ExistingPeriodicWorkPolicy.KEEP,
            repeatingRequest
        )
    }
}

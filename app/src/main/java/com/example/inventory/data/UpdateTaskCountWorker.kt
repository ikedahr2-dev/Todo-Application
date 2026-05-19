package com.example.inventory

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.inventory.data.InventoryDatabase

class UpdateTaskCountWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            // 1. データベースのインスタンスを取得
            val database = InventoryDatabase.getDatabase(context)
            val dao = database.scheduleDao()

            // 2. 現在の時刻をミリ秒で取得
            val currentTime = System.currentTimeMillis()

            // 3. 期限が過ぎた未完了タスクの数を数える
            val overdueCount = dao.getOverdueIncompleteTaskCount(currentTime)

            // 4. NotificationHelperの関数を呼び出して通知を更新
            updateOngoingTaskCountNotification(context, overdueCount)

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}

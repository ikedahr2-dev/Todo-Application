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

            // 3. 💡【修正】件数ではなく「期限切れ未完了タスクの一覧データ」を取得する
            val overdueTasks = dao.getOverdueIncompleteTasks(currentTime)

            // 4. 💡【修正】引数を overdueTasks に変更し、データ一覧を丸ごと手渡す
            updateOngoingTaskCountNotification(context, overdueTasks)

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}

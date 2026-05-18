package com.example.inventory

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import com.example.inventory.data.InventoryDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TodoAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        // 💡 1番用：もし「スマホの起動が完了した」という電波だったら
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // バックグラウンドでデータベースからデータを読み込む（非同期処理）
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Codelabの標準構造からデータベースと全タスクデータを取得
                    val database = InventoryDatabase.getDatabase(context)
                    // ★ご自身のDaoの関数名（例: getAllSchedules() や getAllItems()）に合わせてください
                    val allItems = database.scheduleDao().getAllSchedule().first()

                    val currentTime = System.currentTimeMillis()

                    for (item in allItems) {
                        // 予定の日付・時間文字列をミリ秒に変換
                        // ★ご自身のデータ構造の変数名（例: item.dateString や item.time）に合わせてください
                        val taskTimeMillis = convertDateTimeToMillis(item.date, item.time)

                        // 5分前を計算した時間が「現在より未来」のものだけを自動で再予約
                        if (taskTimeMillis != null && (taskTimeMillis - (5 * 60 * 1000)) > currentTime) {
                            scheduleTodoAlarm(
                                context = context,
                                taskId = item.id, // ★ご自身のID名（例: item.itemId）に合わせてください
                                taskTitle = item.text, // ★ご自身のタイトル名（例: item.text）に合わせてください
                                taskTimeMillis = taskTimeMillis
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return // 再起動の処理が終わったらここで処理を抜ける
        }

        // 🌟 通知の「完了」ボタンが押されたときのバックグラウンド処理
        val action = intent.action
        val taskId = intent.getIntExtra("TODO_ID", -1)

        if (action == "com.example.inventory.ACTION_COMPLETE_TASK" && taskId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = InventoryDatabase.getDatabase(context)
                    val dao = database.scheduleDao()

                    // 💡【ここを修正】フリーズを防ぐため、全体リストを1回だけ取得し、その中から対象を探す
                    val allItems = dao.getAllSchedule().first()
                    val currentSchedule = allItems.find { it.id == taskId }

                    if (currentSchedule != null) {
                        // isCompleted フラグを true (完了) にしてデータベースを更新
                        val updated = currentSchedule.copy(isCompleted = true)
                        dao.update(updated)

                        // データベースの最新データから未完了数を数え直す（今完了にしたタスクは除外して数える）
                        val uncompletedCount = allItems.count {
                            if (it.id == taskId) false else !it.isCompleted
                        }

                        // 常駐通知の残り件数を最新の数字に更新する
                        updateOngoingTaskCountNotification(
                            context = context,
                            uncompletedCount = uncompletedCount
                        )

                        // アラーム設定自体もキャンセルして消す
                        cancelTodoAlarm(context, taskId)

                        // タップされた通知バナー自体を通知欄から消去する
                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(taskId)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return // 完了ボタンの処理が終わったらここで抜ける
        }

        // 💡 通常の「時間（5分前）になった」ときの通知処理（これまでのコード）
        val title = intent.getStringExtra("TODO_TITLE") ?: "タスクの通知"
        val content = intent.getStringExtra("TODO_CONTENT") ?: "5分前です"
        val notificationId = intent.getIntExtra("TODO_ID", 1)

        sendTodoNotification(context, notificationId, title, content)
    }
}

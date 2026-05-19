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

        // もし「スマホの起動が完了した」という電波だったら
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = InventoryDatabase.getDatabase(context)
                    val allItems = database.scheduleDao().getAllSchedule().first()
                    val currentTime = System.currentTimeMillis()

                    // 未来のアラームの再予約ループだけを残します
                    for (item in allItems) {
                        val taskTimeMillis = convertDateTimeToMillis(item.date, item.time)

                        // 5分前を計算した時間が「現在より未来」かつ「まだ完了していない」ものだけを自動で再予約
                        if (taskTimeMillis != null &&
                            (taskTimeMillis - (5 * 60 * 1000)) > currentTime &&
                            !item.isCompleted
                        ) {
                            scheduleTodoAlarm(
                                context = context,
                                taskId = item.id,
                                taskTitle = item.text,
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

        // 通知の「完了」ボタンが押されたときのバックグラウンド処理
        val action = intent.action
        val taskId = intent.getIntExtra("TODO_ID", -1)

        if (action == "com.example.inventory.ACTION_COMPLETE_TASK" && taskId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = InventoryDatabase.getDatabase(context)
                    val dao = database.scheduleDao()

                    // フリーズを防ぐため、全体リストを1回だけ取得し、その中から対象を探す
                    val allItems = dao.getAllSchedule().first()
                    val currentSchedule = allItems.find { it.id == taskId }

                    if (currentSchedule != null) {
                        // isCompleted フラグを true (完了) にしてデータベースを更新
                        val updated = currentSchedule.copy(isCompleted = true)
                        dao.update(updated)

                        // 💡【修正】全件取り直し＆カウントを廃止し、期限切れの未完了数だけを高速に取得
                        val currentTime = System.currentTimeMillis()
                        val overdueCount = dao.getOverdueIncompleteTaskCount(currentTime)

                        // 常駐通知の残り件数を最新の正しい数字に更新する
                        updateOngoingTaskCountNotification(
                            context = context,
                            uncompletedCount = overdueCount // 期限切れのカウント数を手渡す
                        )

                        // アラーム設定自体もキャンセルして消す（変数名を currentSchedule に修正）
                        cancelTodoAlarm(
                            context = context,
                            taskId = taskId,
                            taskTitle = currentSchedule.text
                        )

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

        // 通常の「時間（5分前）になった」ときの通知処理
        val title = intent.getStringExtra("TODO_TITLE") ?: "タスクの通知"
        val content = intent.getStringExtra("TODO_CONTENT") ?: "5分前です"
        val notificationId = intent.getIntExtra("TODO_ID", 1)

        sendTodoNotification(context, notificationId, title, content)
    }
}

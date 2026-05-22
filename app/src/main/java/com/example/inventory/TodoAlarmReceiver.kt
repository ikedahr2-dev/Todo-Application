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
                        val reminderMinutes = item.reminderMinutes // 💡 データベースから通知時間を取得

                        // 💡 通知なし(-1)の場合はスキップ
                        if (reminderMinutes >= 0 && taskTimeMillis != null) {
                            // 設定された分数前を計算した時間が「現在より未来」かつ「まだ完了していない」ものだけ再予約
                            val alarmTimeMillis = taskTimeMillis - (reminderMinutes * 60 * 1000L)

                            if (alarmTimeMillis > currentTime && !item.isCompleted) {
                                scheduleTodoAlarm(
                                    context = context,
                                    taskId = item.id,
                                    taskTitle = item.text,
                                    taskTimeMillis = taskTimeMillis,
                                    reminderMinutes = reminderMinutes // 💡 引数を追加
                                )
                            }
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

        // 確実にIDを取得できるように、キー名を統一して取り出します
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

                        // 件数ではなく、新しく作った「期限切れ未完了タスクの一覧データ」を取得する
                        val currentTime = System.currentTimeMillis()
                        val overdueTasks = dao.getOverdueIncompleteTasks(currentTime)

                        // 名前を引数に合わせて overdueTasks としてデータ一覧を丸ごと手渡す
                        updateOngoingTaskCountNotification(
                            context = context,
                            overdueTasks = overdueTasks
                        )

                        // アラーム設定自体もキャンセルして消す
                        cancelTodoAlarm(
                            context = context,
                            taskId = taskId,
                            taskTitle = currentSchedule.text,
                            reminderMinutes = currentSchedule.reminderMinutes // 💡 引数を追加
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

        // 通常の「時間になった」ときの通知処理
        val title = intent.getStringExtra("TODO_TITLE") ?: "タスクの通知"
        val content = intent.getStringExtra("TODO_CONTENT") ?: "時間です"

        val notificationId = intent.getIntExtra("TODO_ID", -1)

        if (notificationId != -1) {
            sendTodoNotification(context, notificationId, title, content)
        }
    }
}
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

        // スマホの起動が完了した時のアラーム再予約
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = InventoryDatabase.getDatabase(context)
                    val allItems = database.scheduleDao().getAllSchedule().first()
                    val currentTime = System.currentTimeMillis()

                    for (item in allItems) {
                        val taskTimeMillis = convertDateTimeToMillis(item.date, item.time)
                        val reminderMinutes = item.reminderMinutes

                        // 💡 9999の判定を無くし、未完了タスクなら一律で再予約をかけます
                        if (reminderMinutes >= 0 && taskTimeMillis != null) {
                            val alarmTimeMillis = taskTimeMillis - (reminderMinutes * 60 * 1000L)

                            if (alarmTimeMillis > currentTime && !item.isCompleted) {
                                scheduleTodoAlarm(
                                    context = context,
                                    taskId = item.id,
                                    taskTitle = item.text,
                                    taskTimeMillis = taskTimeMillis,
                                    reminderMinutes = reminderMinutes
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return
        }

        val action = intent.action

        val taskId = intent.getIntExtra("TODO_ID", -1)
        val pendingTaskId = intent.getIntExtra("TODO_PENDING_ID", -1)
        val endTaskId = intent.getIntExtra("TODO_END_ID", -1)
        val endPendingTaskId = intent.getIntExtra("TODO_END_PENDING_ID", -1)

        // 💡 追加：通知の「開始」ボタンが押されたときのバックグラウンド処理（開始時間ぴったりにチェックを入れる）
        if (action == "com.example.inventory.ACTION_START_TASK") {
            val actualTaskId = when {
                taskId != -1 -> taskId
                pendingTaskId != -1 -> pendingTaskId
                endTaskId != -1 -> endTaskId - 100000
                endPendingTaskId != -1 -> endPendingTaskId - 100000
                else -> -1
            }

            if (actualTaskId != -1) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val database = InventoryDatabase.getDatabase(context)
                        val dao = database.scheduleDao()

                        val allItems = dao.getAllSchedule().first()
                        val currentSchedule = allItems.find { it.id == actualTaskId }

                        if (currentSchedule != null) {
                            // 開始時間ぴったりとして、開始チェックボックス（isCompleted）をtrue（完了）にする
                            val updated = currentSchedule.copy(isCompleted = true)
                            dao.update(updated)

                            val currentTime = System.currentTimeMillis()
                            val overdueTasks = dao.getOverdueIncompleteTasks(currentTime)

                            updateOngoingTaskCountNotification(
                                context = context,
                                overdueTasks = overdueTasks
                            )

                            // バナー通知の消去（開始ボタンを押したため、そのバナーを消す）
                            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.cancel(actualTaskId)
                            notificationManager.cancel(actualTaskId + 200000) // pending通知用など
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                return
            }
        }

        // 通知の「完了」ボタンが押されたときのバックグラウンド処理
        if (action == "com.example.inventory.ACTION_COMPLETE_TASK") {

            val actualTaskId = when {
                taskId != -1 -> taskId
                pendingTaskId != -1 -> pendingTaskId
                endTaskId != -1 -> endTaskId - 100000
                endPendingTaskId != -1 -> endPendingTaskId - 100000
                else -> -1
            }

            if (actualTaskId != -1) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val database = InventoryDatabase.getDatabase(context)
                        val dao = database.scheduleDao()

                        val allItems = dao.getAllSchedule().first()
                        val currentSchedule = allItems.find { it.id == actualTaskId }

                        if (currentSchedule != null) {
                            // 💡 修正：開始通知ボタン(pendingTaskId)であっても、終了通知ボタンであっても、
                            // 内部数値を9999に化けさせず、その場で即座にタスクを「完了(true)」状態に更新します！
                            val updated = currentSchedule.copy(isCompleted = true)
                            dao.update(updated)

                            val currentTime = System.currentTimeMillis()
                            val overdueTasks = dao.getOverdueIncompleteTasks(currentTime)

                            updateOngoingTaskCountNotification(
                                context = context,
                                overdueTasks = overdueTasks
                            )

                            // 完了したのでアラーム予約をキャンセル
                            cancelTodoAlarm(
                                context = context,
                                taskId = actualTaskId,
                                taskTitle = currentSchedule.text,
                                reminderMinutes = currentSchedule.reminderMinutes
                            )

                            // バナー消去
                            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.cancel(actualTaskId)
                            notificationManager.cancel(actualTaskId + 100000)
                            notificationManager.cancel(actualTaskId + 200000)
                            notificationManager.cancel(actualTaskId + 300000)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                return
            }
        }

        // 通常の「時間になった」ときの通知処理
        val title = intent.getStringExtra("TODO_TITLE") ?: "タスクの通知"
        val content = intent.getStringExtra("TODO_CONTENT") ?: "時間です"

        if (endTaskId != -1) {
            sendTodoEndNotification(context, endTaskId, title, content)
        } else if (taskId != -1) {
            sendTodoNotification(context, taskId, title, content)
        }
    }
}
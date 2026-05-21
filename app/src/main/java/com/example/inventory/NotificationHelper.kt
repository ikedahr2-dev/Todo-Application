package com.example.inventory

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale

// 1. 通知を表示する関数
fun sendTodoNotification(context: Context, notificationId: Int, title: String, content: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    val activityIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    val activityPendingIntent = PendingIntent.getActivity(
        context,
        notificationId,
        activityIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val completeIntent = Intent(context, TodoAlarmReceiver::class.java).apply {
        action = "com.example.inventory.ACTION_COMPLETE_TASK"
        putExtra("TODO_ID", notificationId)
    }

    val completePendingIntent = PendingIntent.getBroadcast(
        context,
        notificationId,
        completeIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(context, "todo_notifications")
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle(title)
        .setContentText(content)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setContentIntent(activityPendingIntent)
        .setAutoCancel(true)
        .addAction(
            android.R.drawable.ic_secure,
            "完了",
            completePendingIntent
        )

    try {
        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    } catch (e: SecurityException) {
        e.printStackTrace()
    }
}

// 2. アラームを予約する関数（💡 reminderMinutes を追加）
fun scheduleTodoAlarm(context: Context, taskId: Int, taskTitle: String, taskTimeMillis: Long, reminderMinutes: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (!alarmManager.canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            context.startActivity(intent)
            return
        }
    }

    // 💡 通知なし(-1)の場合はアラームをセットせずに終了する
    if (reminderMinutes < 0) return

    // 💡 0分なら「時間です」、それ以外なら「XX分前」と表示を切り替える
    val displayTitle = if (reminderMinutes == 0) "タスクの時間です" else "【${reminderMinutes}分前】タスクの時間です"

    val intent = Intent(context, TodoAlarmReceiver::class.java).apply {
        putExtra("TODO_TITLE", displayTitle)
        putExtra("TODO_CONTENT", taskTitle)
        putExtra("TODO_ID", taskId)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        taskId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // 💡 設定された分数（reminderMinutes）を引き算する
    val alarmTimeMillis = taskTimeMillis - (reminderMinutes * 60 * 1000L)

    if (alarmTimeMillis > System.currentTimeMillis()) {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            alarmTimeMillis,
            pendingIntent
        )
    }
}

// 3. 日時を変換する関数
fun convertDateTimeToMillis(dateString: String, timeString: String): Long? {
    return try {
        val dateTimeString = "$dateString $timeString"
        val formatPattern = if (dateString.contains("/")) "yyyy/MM/dd HH:mm" else "yyyy-MM-dd HH:mm"
        val sdf = SimpleDateFormat(formatPattern, Locale.getDefault())
        val date = sdf.parse(dateTimeString)
        date?.time
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// 4. アラームをキャンセルする関数（💡 reminderMinutes を追加）
fun cancelTodoAlarm(context: Context, taskId: Int, taskTitle: String, reminderMinutes: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // 💡 予約時と同じタイトルを生成してキャンセルする
    val displayTitle = if (reminderMinutes == 0) "タスクの時間です" else "【${reminderMinutes}分前】タスクの時間です"

    val intent = Intent(context, TodoAlarmReceiver::class.java).apply {
        putExtra("TODO_TITLE", displayTitle)
        putExtra("TODO_CONTENT", taskTitle)
        putExtra("TODO_ID", taskId)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        taskId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    alarmManager.cancel(pendingIntent)
    pendingIntent.cancel()
}

// 常駐通知の関数
fun updateOngoingTaskCountNotification(context: Context, overdueTasks: List<com.example.inventory.data.Schedule>) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "ToDoリストの状況通知"
        val descriptionText = "期限が過ぎた未完了タスクの数を常駐表示します"
        val importance = android.app.NotificationManager.IMPORTANCE_LOW
        val channel = android.app.NotificationChannel("ongoing_status", name, importance).apply {
            description = descriptionText
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    val ONGOING_NOTIFICATION_ID = 9999

    val activityIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val activityPendingIntent = PendingIntent.getActivity(
        context,
        ONGOING_NOTIFICATION_ID,
        activityIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val uncompletedCount = overdueTasks.size

    val contentText = if (uncompletedCount > 0) {
        "期限切れのタスクがあります"
    } else {
        "期限切れのタスクはありません"
    }

    val inboxStyle = NotificationCompat.InboxStyle()
        .setBigContentTitle("未完了のタスク一覧")
        .setSummaryText("残り ${uncompletedCount} 件")

    for (task in overdueTasks.take(6)) {
        inboxStyle.addLine("・${task.text}")
    }
    if (uncompletedCount > 6) {
        inboxStyle.addLine("他、${uncompletedCount - 6} 件のタスクがあります")
    }

    val builder = NotificationCompat.Builder(context, "ongoing_status")
        .setSmallIcon(android.R.drawable.ic_menu_agenda)
        .setContentTitle("ToDoリストの状況")
        .setContentText(contentText)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .setContentIntent(activityPendingIntent)
        .setAutoCancel(false)

    if (uncompletedCount > 0) {
        builder.setStyle(inboxStyle)
    }

    try {
        NotificationManagerCompat.from(context).notify(ONGOING_NOTIFICATION_ID, builder.build())
    } catch (e: SecurityException) {
        e.printStackTrace()
    }
}
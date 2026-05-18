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

// 1. 通知を表示する関数（引数に notificationId を追加して重複を防止）
fun sendTodoNotification(context: Context, notificationId: Int, title: String, content: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    // 💡【2番の追加コード】タップしたときに起動する画面（MainActivity）を指定
    val activityIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    // 通知に埋め込むための特別なインテントを作成
    val activityPendingIntent = PendingIntent.getActivity(
        context,
        notificationId, // タスクごとのIDをここにも使う
        activityIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // ★【ここを追加】通知の「完了」ボタンを押したときに Receiver を呼び出すためのインテント
    val completeIntent = Intent(context, TodoAlarmReceiver::class.java).apply {
        action = "com.example.inventory.ACTION_COMPLETE_TASK"
        putExtra("TODO_ID", notificationId) // タスクIDを渡す
    }

    val completePendingIntent = PendingIntent.getBroadcast(
        context,
        notificationId, // ボタン専用の識別ID
        completeIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(context, "todo_notifications")
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle(title)
        .setContentText(content)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setContentIntent(activityPendingIntent) //タップした時の動きを登録
        .setAutoCancel(true) //タップされたら自動的に通知を消す
        // ★【ここを追加】通知に「完了」ボタンを設置
        .addAction(
            android.R.drawable.ic_secure, // チェックマークのアイコン
            "完了",                               // ボタンのテキスト
            completePendingIntent                 // 押したときに送る電波
        )

    try {
        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    } catch (e: SecurityException) {
        e.printStackTrace()
    }
}
// 2. アラームを予約する関数（引数に taskId を追加して同じ時間の通知が消えるのを防止）
fun scheduleTodoAlarm(context: Context, taskId: Int, taskTitle: String, taskTimeMillis: Long) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (!alarmManager.canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            context.startActivity(intent)
            return
        }
    }

    val intent = Intent(context, TodoAlarmReceiver::class.java).apply {
        putExtra("TODO_TITLE", "【5分前】タスクの時間です")
        putExtra("TODO_CONTENT", taskTitle)
        putExtra("TODO_ID", taskId)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        taskId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val alarmTimeMillis = taskTimeMillis - (5 * 60 * 1000)

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

// 4. アラームをキャンセルする関数（★3番の機能を追加）
fun cancelTodoAlarm(context: Context, taskId: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, TodoAlarmReceiver::class.java)

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        taskId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
}
//未完了タスク数を常駐通知として表示・更新する関数
fun updateOngoingTaskCountNotification(context: Context, uncompletedCount: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    // 常駐通知専用のID（固定値。5分前アラームのIDと被らない数字）
    val ONGOING_NOTIFICATION_ID = 9999

    // 通知をタップしたときにアプリを開く設定
    val activityIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val activityPendingIntent = PendingIntent.getActivity(
        context,
        ONGOING_NOTIFICATION_ID,
        activityIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // 表示するメッセージの切り替え
    val contentText = if (uncompletedCount > 0) {
        "未完了のタスクが残り ${uncompletedCount} 件あります"
    } else {
        "すべてのタスクが完了しました！"
    }

    val builder = NotificationCompat.Builder(context, "ongoing_status")
        .setSmallIcon(android.R.drawable.ic_menu_agenda)
        .setContentTitle("ToDoリストの状況")
        .setContentText(contentText)
        .setPriority(NotificationCompat.PRIORITY_LOW) // 常駐なので音やバナーで邪魔しない低優先度
        .setOngoing(true) // ★超重要：ユーザーがスワイプしても消せないように常駐させる
        .setContentIntent(activityPendingIntent)
        .setAutoCancel(false)

    try {
        NotificationManagerCompat.from(context).notify(ONGOING_NOTIFICATION_ID, builder.build())
    } catch (e: SecurityException) {
        e.printStackTrace()
    }
}

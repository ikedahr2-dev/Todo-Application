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
fun sendTodoNotification(context: Context, title: String, content: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    val builder = NotificationCompat.Builder(context, "todo_notifications")
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle(title)
        .setContentText(content)
        // ★ここから下の2行を変更・追加します
        .setPriority(NotificationCompat.PRIORITY_MAX) // 優先度を最高に
        .setDefaults(NotificationCompat.DEFAULT_ALL)   // 音とバイブを強制有効化
        .setAutoCancel(true)

    try {
        NotificationManagerCompat.from(context).notify(1, builder.build())
    } catch (e: SecurityException) {
        e.printStackTrace()
    }
}


// 2. アラームを予約する関数
fun scheduleTodoAlarm(context: Context, taskTitle: String, taskTimeMillis: Long) {
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
    }

    val requestCode = taskTimeMillis.hashCode()
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // 予定日時から5分（300,000ミリ秒）を引き算する
    val alarmTimeMillis = taskTimeMillis - (5 * 60 * 1000)

    if (alarmTimeMillis > System.currentTimeMillis()) {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            alarmTimeMillis,
            pendingIntent
        )
    }
}

// 3. 日時を変換する関数（消えていたので復活させます）
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

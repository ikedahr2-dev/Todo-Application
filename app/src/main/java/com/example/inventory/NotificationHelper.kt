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

    // タップしたときに起動する画面（MainActivity）を指定
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

    // 通知の「完了」ボタンを押したときに Receiver を呼び出すためのインテント
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
        // 通知に「完了」ボタンを設置
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
        putExtra("TODO_CONTENT", taskTitle) // これが消去側にも絶対に必要です
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

// 4. アラームをキャンセルする関数（予約時とインテントの中身を完全に一致させる修正）
fun cancelTodoAlarm(context: Context, taskId: Int, taskTitle: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // 予約時（scheduleTodoAlarm）のインテントと、中身の「引き出しの数や文字」を完全に同一にします
    val intent = Intent(context, TodoAlarmReceiver::class.java).apply {
        putExtra("TODO_TITLE", "【5分前】タスクの時間です")
        putExtra("TODO_CONTENT", taskTitle) // ★ここにも同じ文字を詰めることで、OSが同一アラームだと認識します
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

// 引数を件数（Int）から、タスクデータ一覧（List<Schedule>）を受け取る形に変更
fun updateOngoingTaskCountNotification(context: Context, overdueTasks: List<com.example.inventory.data.Schedule>) {
    // ----チャンネルの作成（Android 8.0以上で必須） ----
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "ToDoリストの状況通知"
        val descriptionText = "期限が過ぎた未完了タスクの数を常駐表示します"
        val importance = android.app.NotificationManager.IMPORTANCE_LOW // 音を鳴らさない
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

    val uncompletedCount = overdueTasks.size

    // 表示する通常メッセージの切り替え
    val contentText = if (uncompletedCount > 0) {
        "期限切れのタスクがあります"
    } else {
        "期限切れのタスクはありません"
    }

    // 矢印を押して展開したとき用の「箇条書きスタイル」を作成
    val inboxStyle = NotificationCompat.InboxStyle()
        .setBigContentTitle("未完了のタスク一覧") // 展開時のタイトル
        .setSummaryText("残り ${uncompletedCount} 件") // 右下のサブテキスト

    // タスク一覧から名前を1件ずつ取り出して箇条書き（・タスク名）として追加（最大6件程度）
    for (task in overdueTasks.take(6)) {
        inboxStyle.addLine("・${task.text}")
    }
    // もし7件以上あれば「他◯件」と表示
    if (uncompletedCount > 6) {
        inboxStyle.addLine("他、${uncompletedCount - 6} 件のタスクがあります")
    }

    val builder = NotificationCompat.Builder(context, "ongoing_status")
        .setSmallIcon(android.R.drawable.ic_menu_agenda)
        .setContentTitle("ToDoリストの状況")
        .setContentText(contentText)
        .setPriority(NotificationCompat.PRIORITY_LOW) // 常駐なので音やバナーで邪魔しない低優先度
        .setOngoing(true) // ★超重要：ユーザーがスワイプしても消せないように常駐させる
        .setContentIntent(activityPendingIntent)
        .setAutoCancel(false)

    // 箇条書きが1件以上ある場合のみ、スワイプ展開用のスタイルを設定する
    if (uncompletedCount > 0) {
        builder.setStyle(inboxStyle)
    }

    try {
        NotificationManagerCompat.from(context).notify(ONGOING_NOTIFICATION_ID, builder.build())
    } catch (e: SecurityException) {
        e.printStackTrace()
    }
}

package com.example.inventory

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TodoAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 時間（5分前）になったらここが自動で実行されます
        val title = intent.getStringExtra("TODO_TITLE") ?: "タスクの通知"
        val content = intent.getStringExtra("TODO_CONTENT") ?: "5分前です"

        // NotificationHelperに定義してある通知関数を呼び出します
        sendTodoNotification(context, title, content)
    }
}

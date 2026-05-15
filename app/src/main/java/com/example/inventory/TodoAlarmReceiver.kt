package com.example.inventory

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TodoAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("TODO_TITLE") ?: "タスクの通知"
        val content = intent.getStringExtra("TODO_CONTENT") ?: "5分前です"

        // ★アラーム予約時に込めたタスクID（無ければデフォルトで1）を取り出す
        val notificationId = intent.getIntExtra("TODO_ID", 1)

        // ★引数に notificationId を追加して呼び出す
        sendTodoNotification(context, notificationId, title, content)
    }
}//

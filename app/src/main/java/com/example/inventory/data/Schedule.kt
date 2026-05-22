package com.example.inventory.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val text: String,
    val time: String,
    val date: String,
    val endTime: String = "",

    val data: Long = System.currentTimeMillis(), //日付の更新
    val category: String = "",
    val detail: String = "",

    // デフォルトは未完了（false）にしてる
    val isCompleted: Boolean = false,

    // 💡 追加：何分前に通知するか（デフォルトは5分前。通知なしの場合は -1 を想定）
    val reminderMinutes: Int = 5
)
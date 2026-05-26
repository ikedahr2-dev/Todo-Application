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

    // 何分前に通知するか（デフォルトは5分前。通知なしの場合は -1 を想定）
    val reminderMinutes: Int = 5,

    // 終了チェックボックス専用のフラグを追加（既存の機能を壊さず、後ろに追記しました）
    val isEndCompleted: Boolean = false
)
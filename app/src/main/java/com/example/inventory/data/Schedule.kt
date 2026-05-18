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

    val data: Long = System.currentTimeMillis(), //日付の更新
    val category: String = "",

    //デフォルトは未完了（false）にしてる
    val isCompleted: Boolean = false

)
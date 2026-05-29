package com.example.inventory.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game")
data class Game(
    @PrimaryKey
    val id: Int = 1,
    val waterStoredPercent: Int = 0, // 蓄えられた水分量
    val currentLevel: Int = 0,       // 現在のレベル
    val givenWaterCount: Int = 0,    // 現在のレベルであげた水の回数
    val currentHeightLayer: Int = 0  // 現在の高さ
)
package com.example.inventory.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow


// 仮

@Dao
interface GameDao {
// ---------- 現在のゲームデータの取得 ---------- //

    @Query("SELECT * FROM game LIMIT 1")
    fun getGameStatus(): Flow<Game?>

// ---------- ゲームデータの新規登録 ---------- //

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: Game)

// ---------- ゲームデータの更新 ---------- //

    @Update
    suspend fun updateGame(game: Game)
}
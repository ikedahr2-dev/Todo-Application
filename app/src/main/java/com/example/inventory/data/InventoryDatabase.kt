package com.example.inventory.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// 🌟 変更ポイント①: entitiesに「Game::class」を追加して、versionを「2」に上げる！
@Database(entities = [Schedule::class, Game::class], version = 2, exportSchema = false)
abstract class InventoryDatabase : RoomDatabase() {

    //-- Dao呼び出し --//
    abstract fun scheduleDao(): ScheduleDao

    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var Instance: InventoryDatabase? = null

        fun getDatabase(context: Context): InventoryDatabase {

            //-- インスタンスなかったら作成 --//
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, InventoryDatabase::class.java, "schedule_database")
                    .fallbackToDestructiveMigration() // 開発中にテーブルを増やした時のエラーを防ぐ
                    .build().also { Instance = it }   // 作ったインスタンスをInstanceに保存　使いまわし可
            }
        }
    }
}


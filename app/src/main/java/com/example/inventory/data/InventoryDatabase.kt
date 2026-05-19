package com.example.inventory.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Schedule::class], version = 1, exportSchema = false)
abstract class InventoryDatabase : RoomDatabase() {

    //-- Dao呼び出し --//
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        @Volatile
        private var Instance: InventoryDatabase? = null

        fun getDatabase(context: Context): InventoryDatabase {

            //-- インスタンスなかったら作成 --//
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, InventoryDatabase::class.java, "schedule_database")
                    .build().also { Instance = it }   // 作ったインスタンスをInstanceに保存　使いまわし可
            }
        }
    }
}


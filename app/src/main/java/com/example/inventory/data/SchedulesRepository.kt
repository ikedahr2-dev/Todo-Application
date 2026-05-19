package com.example.inventory.data

import kotlinx.coroutines.flow.Flow

interface SchedulesRepository {

    //-- 全取得 --//
    fun getAllSchedulesStream(): Flow<List<Schedule>>

    //-- idが一致するscheduleを取得 --//
    fun getScheduleStream(id: Int): Flow<Schedule?>

    //-- 保存 --//
    suspend fun insertSchedule(schedule: Schedule)

    //-- 削除 --//
    suspend fun deleteSchedule(schedule: Schedule)

    //-- 更新 --//
    suspend fun updateSchedule(schedule: Schedule)

    //--期限切れの未完了タスク数を取得 --//
    suspend fun getOverdueIncompleteTaskCount(currentTime: Long): Int

}
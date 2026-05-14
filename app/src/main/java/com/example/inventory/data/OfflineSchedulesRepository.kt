package com.example.inventory.data

import kotlinx.coroutines.flow.Flow

class OfflineSchedulesRepository(private val scheduleDao: ScheduleDao) : SchedulesRepository {

    //-- 全取得 --//
    override fun getAllSchedulesStream(): Flow<List<Schedule>> = scheduleDao.getAllSchedule()

    //-- idが一致するscheduleを取得 --//
    override fun getScheduleStream(id: Int): Flow<Schedule?> = scheduleDao.getSchedule(id)

    //-- 保存 --//
    override suspend fun insertSchedule(schedule: Schedule) = scheduleDao.insert(schedule)

    //-- 削除 --//
    override suspend fun deleteSchedule(schedule: Schedule) = scheduleDao.delete(schedule)

    //-- 更新 --//
    override suspend fun updateSchedule(schedule: Schedule) = scheduleDao.update(schedule)
}

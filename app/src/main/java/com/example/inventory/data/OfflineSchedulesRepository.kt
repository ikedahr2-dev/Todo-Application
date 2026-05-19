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

    //--Daoの関数を呼び出してリポジトリ経由で件数を取得 --//
    override suspend fun getOverdueIncompleteTaskCount(currentTime: Long): Int =
        scheduleDao.getOverdueIncompleteTaskCount(currentTime)

    // 💡【ここを追加】Daoの関数を呼び出してリポジトリ経由でデータ一覧を取得
    override suspend fun getOverdueIncompleteTasks(currentTime: Long): List<Schedule> =
        scheduleDao.getOverdueIncompleteTasks(currentTime)
}

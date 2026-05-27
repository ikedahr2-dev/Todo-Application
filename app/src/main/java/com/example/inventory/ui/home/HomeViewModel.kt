package com.example.inventory.ui.home

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.app.Application
import android.content.Context
import com.example.inventory.cancelTodoAlarm
import com.example.inventory.updateOngoingTaskCountNotification
import com.example.inventory.scheduleTodoAlarm
import com.example.inventory.scheduleTodoEndAlarm
import com.example.inventory.convertDateTimeToMillis
import com.example.inventory.data.Schedule
import com.example.inventory.data.SchedulesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val scheduleList: List<Schedule> = listOf(),
    val showDatePicker: Boolean = false,
    val showInputBox: Boolean = false,
    val editingItem: Schedule? = null,
    val selectedFilterCategory: String = "",
    val selectedEditCategory: String = "",
    val searchQuery: String = "",
    val categoriesList: List<String> = listOf("すべて", "仕事", "プライベート", "その他"),
    //現在蓄えられている水分量の状態
    val waterStoredPercent: Int = 0
)

class HomeViewModel(
    private val schedulesRepository: SchedulesRepository,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())

    val uiState: StateFlow<HomeUiState> = schedulesRepository.getAllSchedulesStream()
        .combine(_uiState) { dbList, currentUiState ->
            val query = currentUiState.searchQuery.trim()
            val filteredList = dbList.filter { schedule ->
                if (query.isBlank()) {
                    true
                } else {
                    schedule.text.contains(query, ignoreCase = true) ||
                            schedule.date.contains(query, ignoreCase = true) ||
                            schedule.time.contains(query, ignoreCase = true)
                }
            }
            currentUiState.copy(scheduleList = filteredList)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            initialValue = HomeUiState()
        )

    private var _editingItem = mutableStateOf<Schedule?>(null)
    val editingItem: State<Schedule?> = _editingItem

    //すでにリアルタイム加算が処理されたタスクごとの経過ミリ秒数をデバイスに記録する用
    private val processedTimePrefs = application.getSharedPreferences("game_processed_time", Context.MODE_PRIVATE)

    init {
        refreshOngoingNotification()
        loadCategories()
        loadWaterData()
    }

    private fun loadCategories() {
        val sharedPreferences = application.getSharedPreferences("app_categories", Context.MODE_PRIVATE)
        val savedCategoriesStr = sharedPreferences.getString("categories_key", null)
        if (!savedCategoriesStr.isNullOrBlank()) {
            val list = savedCategoriesStr.split(",")
            _uiState.update { it.copy(categoriesList = list) }
        }
    }

    private fun loadWaterData() {
        val sharedPreferences = application.getSharedPreferences("game_data", Context.MODE_PRIVATE)
        val savedWater = sharedPreferences.getInt("water_percent_key", 0)
        _uiState.update { it.copy(waterStoredPercent = savedWater) }
    }

    private fun saveWaterToDevice(water: Int) {
        val sharedPreferences = application.getSharedPreferences("game_data", Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt("water_percent_key", water).apply()
    }

    fun addCategory(newCategory: String) {
        if (newCategory.isNotBlank() && !_uiState.value.categoriesList.contains(newCategory)) {
            val updatedList = _uiState.value.categoriesList + newCategory
            saveCategoriesToDevice(updatedList)
            _uiState.update { it.copy(categoriesList = updatedList) }
            onSelectFilterCategory(newCategory)
        }
    }

    fun deleteCategory(category: String) {
        if (category != "すべて") {
            val updatedList = _uiState.value.categoriesList.filter { it != category }
            saveCategoriesToDevice(updatedList)
            _uiState.update { it.copy(categoriesList = updatedList) }
            if (_uiState.value.selectedFilterCategory == category) {
                onSelectFilterCategory("")
            }
        }
    }

    private fun saveCategoriesToDevice(list: List<String>) {
        val sharedPreferences = application.getSharedPreferences("app_categories", Context.MODE_PRIVATE)
        val categoriesStr = list.joinToString(",")
        sharedPreferences.edit().putString("categories_key", categoriesStr).apply()
    }

    private fun refreshOngoingNotification() {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            val overdueTasks = schedulesRepository.getOverdueIncompleteTasks(currentTime)
            updateOngoingTaskCountNotification(context = application.applicationContext, overdueTasks = overdueTasks)
        }
    }

    fun onSelectFilterCategory(category: String) { _uiState.update { it.copy(selectedFilterCategory = category) } }
    fun onSelectEditCategory(category: String) { _uiState.update { it.copy(selectedEditCategory = category) } }

    fun onAddClick() {
        _editingItem.value = null
        _uiState.update {
            it.copy(showInputBox = true, editingItem = null, selectedEditCategory = "")
        }
    }

    fun onDismissInputBox() {
        _editingItem.value = null
        _uiState.update { it.copy(showInputBox = false) }
    }

    fun addText(text: String, date: String, time: String, endTime: String, category: String, detail: String, reminderMinutes: Int, endReminderMinutes: Int) {
        viewModelScope.launch {
            val targetEndTime = endTime.ifBlank { time }
            val taskTimeMillis = convertDateTimeToMillis(date, targetEndTime) ?: System.currentTimeMillis()

            val finalReminderMinutes = if (reminderMinutes == 9999) 5 else reminderMinutes

            val newSchedule = Schedule(
                text = text,
                date = date,
                time = time,
                endTime = endTime,
                category = category,
                detail = detail,
                data = taskTimeMillis,
                reminderMinutes = finalReminderMinutes
            )
            schedulesRepository.insertSchedule(newSchedule)

            val savedList = schedulesRepository.getAllSchedulesStream().first()
            val savedItem = savedList.find { it.text == text && it.date == date && it.time == time }

            if (savedItem != null) {
                val alarmTimeMillis = convertDateTimeToMillis(date, time)
                if (alarmTimeMillis != null && alarmTimeMillis > System.currentTimeMillis()) {
                    scheduleTodoAlarm(context = application.applicationContext, taskId = savedItem.id, taskTitle = text, taskTimeMillis = alarmTimeMillis, reminderMinutes = finalReminderMinutes)
                }

                if (endTime.isNotBlank()) {
                    val endAlarmTimeMillis = convertDateTimeToMillis(date, endTime)
                    if (endAlarmTimeMillis != null && endAlarmTimeMillis > System.currentTimeMillis()) {
                        scheduleTodoEndAlarm(context = application.applicationContext, taskId = savedItem.id, taskTitle = text, endTimeMillis = endAlarmTimeMillis)
                    }
                }
            }

            onDismissInputBox()
            refreshOngoingNotification()
        }
    }

    fun updateItem(schedule: Schedule, newText: String, newDate: String, newTime: String, newEndTime: String, newCategory: String, newDetail: String, newReminderMinutes: Int, newEndReminderMinutes: Int) {
        viewModelScope.launch {
            val targetEndTime = newEndTime.ifBlank { newTime }
            val taskTimeMillis = convertDateTimeToMillis(newDate, targetEndTime) ?: System.currentTimeMillis()

            val finalReminderMinutes = if (newReminderMinutes == 9999) 5 else newReminderMinutes

            val updatedSchedule = schedule.copy(
                text = newText,
                date = newDate,
                time = newTime,
                endTime = newEndTime,
                category = newCategory,
                detail = newDetail,
                data = taskTimeMillis,
                reminderMinutes = finalReminderMinutes
            )
            schedulesRepository.updateSchedule(updatedSchedule)

            cancelTodoAlarm(context = application.applicationContext, taskId = schedule.id, taskTitle = schedule.text, reminderMinutes = schedule.reminderMinutes)

            val alarmTimeMillis = convertDateTimeToMillis(newDate, newTime)
            if (alarmTimeMillis != null && alarmTimeMillis > System.currentTimeMillis()) {
                scheduleTodoAlarm(context = application.applicationContext, taskId = schedule.id, taskTitle = newText, taskTimeMillis = alarmTimeMillis, reminderMinutes = finalReminderMinutes)
            }

            if (newEndTime.isNotBlank()) {
                val endAlarmTimeMillis = convertDateTimeToMillis(newDate, newEndTime)
                if (endAlarmTimeMillis != null && endAlarmTimeMillis > System.currentTimeMillis()) {
                    scheduleTodoEndAlarm(context = application.applicationContext, taskId = schedule.id, taskTitle = newText, endTimeMillis = endAlarmTimeMillis)
                }
            }

            onDismissInputBox()
            refreshOngoingNotification()
        }
    }

    fun onEditSavedItem(schedule: Schedule) {
        _editingItem.value = schedule
        _uiState.update {
            it.copy(showInputBox = true, editingItem = schedule, selectedEditCategory = schedule.category)
        }
    }

    fun deleteItem(schedule: Schedule) {
        viewModelScope.launch {
            schedulesRepository.deleteSchedule(schedule)
            cancelTodoAlarm(context = application.applicationContext, taskId = schedule.id, taskTitle = schedule.text, reminderMinutes = schedule.reminderMinutes)
            onDismissInputBox()
            refreshOngoingNotification()
        }
    }

    // 💡 注目：現在進行中のタスクの時間をリアルタイムに計測して水分に変換するコア関数
    // HomeScreen のタイマー周期（5秒ごと等）に合わせて随時呼び出されます。
    fun trackActiveSchedulesRealTime(currentTimeMillis: Long) {
        viewModelScope.launch {
            val allSchedules = schedulesRepository.getAllSchedulesStream().first()
            var addedPercentTotal = 0.0

            allSchedules.forEach { schedule ->
                val startMillis = convertDateTimeToMillis(schedule.date, schedule.time)
                val endMillis = convertDateTimeToMillis(schedule.date, schedule.endTime ?: "")

                //タスクが現在実行時間内にあるか判定
                if (startMillis != null && endMillis != null && currentTimeMillis >= startMillis) {
                    //計測対象は「タスク終了時刻」または「現在の時刻」のいずれか早い方まで
                    val upperLimit = minOf(currentTimeMillis, endMillis)
                    val activeDurationMillis = upperLimit - startMillis

                    if (activeDurationMillis > 0) {
                        val key = "task_progress_${schedule.id}"
                        val alreadyProcessedMillis = processedTimePrefs.getLong(key, 0L)

                        //前回計測時からの純粋な「差分ミリ秒（進捗した時間）」を算出
                        val deltaMillis = activeDurationMillis - alreadyProcessedMillis

                        if (deltaMillis > 0) {
                            //1時間(60分 = 3,600,000ミリ秒)を100%とした時のリアルタイム水分量比率を細かく計算
                            val deltaPercent = (deltaMillis.toDouble() / 3600000.0) * 100.0
                            addedPercentTotal += deltaPercent

                            //このタスクの現在の消化時間を記録保存
                            processedTimePrefs.edit().putLong(key, activeDurationMillis).apply()
                        }
                    }
                }
            }

            if (addedPercentTotal > 0.0) {
                //端数を維持しながら現在の水分パーセンテージにリアルタイム反映して永続保存
                val currentStored = _uiState.value.waterStoredPercent
                val newWaterPercent = (currentStored + addedPercentTotal).toInt()
                saveWaterToDevice(newWaterPercent)

                _uiState.update { it.copy(waterStoredPercent = newWaterPercent) }
            }
        }
    }

    //一括削除時はタスクデータを消去するだけで、水分はリアルタイムでチャージ済みのため加算は行いません
    fun deleteCompletedSchedules() {
        viewModelScope.launch {
            val allSchedules = schedulesRepository.getAllSchedulesStream().first()
            val fullyCompletedList = allSchedules.filter { it.isEndCompleted }

            fullyCompletedList.forEach { schedule ->
                schedulesRepository.deleteSchedule(schedule)
                cancelTodoAlarm(context = application.applicationContext, taskId = schedule.id, taskTitle = schedule.text, reminderMinutes = schedule.reminderMinutes)
                //用済みのタスク計測キャッシュをクリーンアップ
                processedTimePrefs.edit().remove("task_progress_${schedule.id}").apply()
            }

            refreshOngoingNotification()
        }
    }

    //開始チェックボックス（1つ目）用
    fun toggleScheduleStatus(schedule: Schedule, isChecked: Boolean) {
        viewModelScope.launch {
            val updatedSchedule = schedule.copy(isCompleted = isChecked)
            schedulesRepository.updateSchedule(updatedSchedule)

            kotlinx.coroutines.delay(150)

            if (isChecked) {
                cancelTodoAlarm(context = application.applicationContext, taskId = schedule.id, taskTitle = schedule.text, reminderMinutes = schedule.reminderMinutes)
            } else {
                val fallbackReminderMinutes = if (schedule.reminderMinutes == 9999) 5 else schedule.reminderMinutes

                val alarmTimeMillis = convertDateTimeToMillis(schedule.date, schedule.time)
                if (alarmTimeMillis != null && alarmTimeMillis > System.currentTimeMillis()) {
                    scheduleTodoAlarm(context = application.applicationContext, taskId = schedule.id, taskTitle = schedule.text, taskTimeMillis = alarmTimeMillis, reminderMinutes = fallbackReminderMinutes)
                }

                val targetEndTime = schedule.endTime ?: ""
                if (targetEndTime.isNotBlank()) {
                    val endAlarmTimeMillis = convertDateTimeToMillis(schedule.date, targetEndTime)
                    if (endAlarmTimeMillis != null && endAlarmTimeMillis > System.currentTimeMillis()) {
                        scheduleTodoEndAlarm(context = application.applicationContext, taskId = schedule.id, taskTitle = schedule.text, endTimeMillis = endAlarmTimeMillis)
                    }
                }
            }
            refreshOngoingNotification()
        }
    }

    //終了チェックボックス（2つ目）専用の更新関数
    fun toggleScheduleEndStatus(schedule: Schedule, isChecked: Boolean) {
        viewModelScope.launch {
            val updatedSchedule = schedule.copy(isEndCompleted = isChecked)
            schedulesRepository.updateSchedule(updatedSchedule)

            kotlinx.coroutines.delay(150)
            refreshOngoingNotification()
        }
    }

    fun updateWaterStoredPercent(newPercent: Int) {
        viewModelScope.launch {
            saveWaterToDevice(newPercent)
            _uiState.update { it.copy(waterStoredPercent = newPercent) }
        }
    }

    fun updateSearchQuery(query: String) { _uiState.update { it.copy(searchQuery = query) } }

    companion object { private const val TIMEOUT_MILLIS = 5_000L }
}
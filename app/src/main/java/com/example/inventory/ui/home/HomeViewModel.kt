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
    val categoriesList: List<String> = listOf("すべて", "仕事", "プライベート", "その他")
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

    init {
        refreshOngoingNotification()
        loadCategories()
    }

    private fun loadCategories() {
        val sharedPreferences = application.getSharedPreferences("app_categories", Context.MODE_PRIVATE)
        val savedCategoriesStr = sharedPreferences.getString("categories_key", null)
        if (!savedCategoriesStr.isNullOrBlank()) {
            val list = savedCategoriesStr.split(",")
            _uiState.update { it.copy(categoriesList = list) }
        }
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

    // 💡 修正: 新規追加処理
    fun addText(text: String, date: String, time: String, endTime: String, category: String, detail: String, reminderMinutes: Int, endReminderMinutes: Int) {
        viewModelScope.launch {
            val targetEndTime = endTime.ifBlank { time }
            val taskTimeMillis = convertDateTimeToMillis(date, targetEndTime) ?: System.currentTimeMillis()

            // 💡 画面から渡ってきた選択分数（デフォルト5分）を確実に適用
            // 万が一ここが不正な値（9999など）で入ってきた場合の防衛策として、初期作成時は強制的に5分前をセットします
            val finalReminderMinutes = if (reminderMinutes == 9999) 5 else reminderMinutes

            val newSchedule = Schedule(
                text = text,
                date = date,
                time = time,
                endTime = endTime,
                category = category,
                detail = detail,
                data = taskTimeMillis,
                reminderMinutes = finalReminderMinutes // 💡 5分前（または選択値）を確実に保存
            )
            schedulesRepository.insertSchedule(newSchedule)

            val savedList = schedulesRepository.getAllSchedulesStream().first()
            val savedItem = savedList.find { it.text == text && it.date == date && it.time == time }

            if (savedItem != null) {
                // 1. 開始アラーム（5分前など）の予約
                val alarmTimeMillis = convertDateTimeToMillis(date, time)
                if (alarmTimeMillis != null && alarmTimeMillis > System.currentTimeMillis()) {
                    scheduleTodoAlarm(context = application.applicationContext, taskId = savedItem.id, taskTitle = text, taskTimeMillis = alarmTimeMillis, reminderMinutes = finalReminderMinutes)
                }

                // 2. 終了アラーム（時間ぴったり固定）の予約
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

    fun onEditSavedItem(schedule: Schedule) {
        _editingItem.value = schedule
        _uiState.update {
            it.copy(showInputBox = true, editingItem = schedule, selectedEditCategory = schedule.category)
        }
    }

    // 💡 修正: 編集更新処理
    fun updateItem(schedule: Schedule, newText: String, newDate: String, newTime: String, newEndTime: String, newCategory: String, newDetail: String, newReminderMinutes: Int, newEndReminderMinutes: Int) {
        viewModelScope.launch {
            val targetEndTime = newEndTime.ifBlank { newTime }
            val taskTimeMillis = convertDateTimeToMillis(newDate, targetEndTime) ?: System.currentTimeMillis()

            // 💡 編集更新時も、画面の入力値（9999以外）を正しくマッピング
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

            // 古いアラームをキャンセル
            cancelTodoAlarm(context = application.applicationContext, taskId = schedule.id, taskTitle = schedule.text, reminderMinutes = schedule.reminderMinutes)

            // 1. 新しい開始前通知アラームの予約
            val alarmTimeMillis = convertDateTimeToMillis(newDate, newTime)
            if (alarmTimeMillis != null && alarmTimeMillis > System.currentTimeMillis()) {
                scheduleTodoAlarm(context = application.applicationContext, taskId = schedule.id, taskTitle = newText, taskTimeMillis = alarmTimeMillis, reminderMinutes = finalReminderMinutes)
            }

            // 2. 新しい終了アラーム（時間ぴったり固定）の予約
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

    fun deleteItem(schedule: Schedule) {
        viewModelScope.launch {
            schedulesRepository.deleteSchedule(schedule)
            cancelTodoAlarm(context = application.applicationContext, taskId = schedule.id, taskTitle = schedule.text, reminderMinutes = schedule.reminderMinutes)
            onDismissInputBox()
            refreshOngoingNotification()
        }
    }

    fun deleteCompletedSchedules() {
        viewModelScope.launch {
            val completedList = uiState.value.scheduleList.filter { it.isCompleted }
            completedList.forEach { schedule ->
                schedulesRepository.deleteSchedule(schedule)
                cancelTodoAlarm(context = application.applicationContext, taskId = schedule.id, taskTitle = schedule.text, reminderMinutes = schedule.reminderMinutes)
            }
            refreshOngoingNotification()
        }
    }

    fun toggleScheduleStatus(schedule: Schedule, isChecked: Boolean) {
        viewModelScope.launch {
            val updatedSchedule = schedule.copy(isCompleted = isChecked)
            schedulesRepository.updateSchedule(updatedSchedule)

            kotlinx.coroutines.delay(150)

            if (isChecked) {
                cancelTodoAlarm(context = application.applicationContext, taskId = schedule.id, taskTitle = schedule.text, reminderMinutes = schedule.reminderMinutes)
            } else {
                // 💡 チェックを外して未完了に戻した時、古い保留用フラグ(9999)だった場合はデフォルトの5分前に安全復帰させます
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

    fun updateSearchQuery(query: String) { _uiState.update { it.copy(searchQuery = query) } }

    companion object { private const val TIMEOUT_MILLIS = 5_000L }
}
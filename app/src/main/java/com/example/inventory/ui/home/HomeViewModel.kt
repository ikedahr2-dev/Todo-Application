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

    // 💡 引数に reminderMinutes を追加
    fun addText(text: String, date: String, time: String, endTime: String, category: String, detail: String, reminderMinutes: Int) {
        viewModelScope.launch {
            val targetEndTime = endTime.ifBlank { time }
            val taskTimeMillis = convertDateTimeToMillis(date, targetEndTime) ?: System.currentTimeMillis()

            val newSchedule = Schedule(
                text = text,
                date = date,
                time = time,
                endTime = endTime,
                category = category,
                detail = detail,
                data = taskTimeMillis,
                reminderMinutes = reminderMinutes // 💡 追加
            )
            schedulesRepository.insertSchedule(newSchedule)

            val savedList = schedulesRepository.getAllSchedulesStream().first()
            val savedItem = savedList.find { it.text == text && it.date == date && it.time == time }

            val alarmTimeMillis = convertDateTimeToMillis(date, time)
            if (savedItem != null && alarmTimeMillis != null && alarmTimeMillis > System.currentTimeMillis()) {
                // 💡 引数に reminderMinutes を引き渡す
                scheduleTodoAlarm(context = application.applicationContext, taskId = savedItem.id, taskTitle = text, taskTimeMillis = alarmTimeMillis, reminderMinutes = reminderMinutes)
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

    // 💡 引数に newReminderMinutes を追加
    fun updateItem(schedule: Schedule, newText: String, newDate: String, newTime: String, newEndTime: String, newCategory: String, newDetail: String, newReminderMinutes: Int) {
        viewModelScope.launch {
            val targetEndTime = newEndTime.ifBlank { newTime }
            val taskTimeMillis = convertDateTimeToMillis(newDate, targetEndTime) ?: System.currentTimeMillis()

            val updatedSchedule = schedule.copy(
                text = newText,
                date = newDate,
                time = newTime,
                endTime = newEndTime,
                category = newCategory,
                detail = newDetail,
                data = taskTimeMillis,
                reminderMinutes = newReminderMinutes // 💡 追加
            )
            schedulesRepository.updateSchedule(updatedSchedule)

            // 💡 変更前の古い通知設定を使って、一度アラームをキャンセルする
            cancelTodoAlarm(context = application.applicationContext, taskId = schedule.id, taskTitle = schedule.text, reminderMinutes = schedule.reminderMinutes)

            val alarmTimeMillis = convertDateTimeToMillis(newDate, newTime)
            if (alarmTimeMillis != null && alarmTimeMillis > System.currentTimeMillis()) {
                // 💡 新しい通知設定でアラームを再予約する
                scheduleTodoAlarm(context = application.applicationContext, taskId = schedule.id, taskTitle = newText, taskTimeMillis = alarmTimeMillis, reminderMinutes = newReminderMinutes)
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
                val alarmTimeMillis = convertDateTimeToMillis(schedule.date, schedule.time)
                if (alarmTimeMillis != null && alarmTimeMillis > System.currentTimeMillis()) {
                    scheduleTodoAlarm(context = application.applicationContext, taskId = schedule.id, taskTitle = schedule.text, taskTimeMillis = alarmTimeMillis, reminderMinutes = schedule.reminderMinutes)
                }
            }
            refreshOngoingNotification()
        }
    }

    fun updateSearchQuery(query: String) { _uiState.update { it.copy(searchQuery = query) } }

    companion object { private const val TIMEOUT_MILLIS = 5_000L }
}
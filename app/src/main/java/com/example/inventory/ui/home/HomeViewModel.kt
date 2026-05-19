package com.example.inventory.ui.home

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.app.Application
import com.example.inventory.cancelTodoAlarm
import com.example.inventory.updateOngoingTaskCountNotification
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
    val searchQuery: String = ""
)

class HomeViewModel(
    private val schedulesRepository: SchedulesRepository,
    private val application: Application //アプリのContextを取得するために追加
) : ViewModel() {

    // 1. ダイアログなどの「UI状態」を管理する
    private val _uiState = MutableStateFlow(HomeUiState())

    // 2. DBのデータとUI状態を「ガッチャンコ」して HomeScreen に流す
    val uiState: StateFlow<HomeUiState> = schedulesRepository.getAllSchedulesStream()
        .combine(_uiState) { dbList, currentUiState ->

            // 💡【修正】ここでの古い一斉通知コードを削除しました。
            // これによりバックグラウンド定期更新（WorkManager）との競合（数値のチカチカ）を防ぎます。

            val filteredList =
                dbList.filter { schedule ->
                    val query = currentUiState.searchQuery.trim()
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

    // 💡【共通処理を追加】期限切れの未完了タスク数だけを数えて通知を最新にする関数
    private fun refreshOngoingNotification() {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            val overdueCount = schedulesRepository.getOverdueIncompleteTaskCount(currentTime)
            updateOngoingTaskCountNotification(
                context = application.applicationContext,
                uncompletedCount = overdueCount
            )
        }
    }

    fun onSelectFilterCategory(category: String) {
        _uiState.update {
            it.copy(selectedFilterCategory = category)
        }
    }

    fun onSelectEditCategory(category: String) {
        _uiState.update {
            it.copy(selectedEditCategory = category)
        }
    }

    fun onAddClick() {
        _editingItem.value = null
        _uiState.update {
            it.copy(
                showInputBox = true,
                editingItem = null,
                selectedEditCategory = ""
            )
        }
    }

    fun onDismissInputBox() {
        _editingItem.value = null
        _uiState.update { it.copy(showInputBox = false) }
    }

    fun addText(text: String, date: String, time: String, category: String) {
        viewModelScope.launch {
            val newSchedule = Schedule(text = text, date = date, time = time, category = category)
            schedulesRepository.insertSchedule(newSchedule)
            onDismissInputBox()
            // 💡タスク追加時に通知をリアルタイム更新
            refreshOngoingNotification()
        }
    }

    fun onEditSavedItem(schedule: Schedule) {
        _editingItem.value = schedule
        _uiState.update {
            it.copy(
                showInputBox = true,
                editingItem = schedule,
                selectedEditCategory = schedule.category
            )
        }
    }

    fun updateItem(schedule: Schedule, newText: String, newDate: String, newTime: String, newCategory: String) {
        viewModelScope.launch {
            val updatedSchedule = schedule.copy(text = newText, date = newDate, time = newTime, category = newCategory)
            schedulesRepository.updateSchedule(updatedSchedule)
            onDismissInputBox()
            // 💡タスク編集時に通知をリアルタイム更新
            refreshOngoingNotification()
        }
    }

    fun deleteItem(schedule: Schedule) {
        viewModelScope.launch {
            schedulesRepository.deleteSchedule(schedule)
            onDismissInputBox()
            // 💡タスク削除時に通知をリアルタイム更新
            refreshOngoingNotification()
        }
    }

    fun deleteCompletedSchedules() {
        viewModelScope.launch {
            val completedList = uiState.value.scheduleList.filter { it.isCompleted }

            completedList.forEach { schedule ->
                schedulesRepository.deleteSchedule(schedule)

                cancelTodoAlarm(
                    context = application.applicationContext,
                    taskId = schedule.id,
                    taskTitle = schedule.text
                )
            }
            // 💡一括削除後に古い全件カウントを廃止し、最新の期限切れ数を通知に反映
            refreshOngoingNotification()
        }
    }

    // チェックボックスの切り替え処理
    fun toggleScheduleStatus(schedule: Schedule, isChecked: Boolean) {
        viewModelScope.launch {
            val updatedSchedule = schedule.copy(isCompleted = isChecked)
            schedulesRepository.updateSchedule(updatedSchedule)

            kotlinx.coroutines.delay(150)

            if (isChecked) {
                cancelTodoAlarm(
                    context = application.applicationContext,
                    taskId = schedule.id,
                    taskTitle = schedule.text
                )
            }
            // 💡チェック切り替え後に古い全件カウントを廃止し、最新の期限切れ数を通知に反映
            refreshOngoingNotification()
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update {
            it.copy(searchQuery = query)
        }
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

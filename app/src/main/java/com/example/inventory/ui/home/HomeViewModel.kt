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
    val selectedCategory: String = "",
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

            // 「trueではないもの（＝未完了）」だけ数えるように書き換えます
            val uncompletedCount = dbList.count { it.isCompleted == false }

            updateOngoingTaskCountNotification(
                context = application.applicationContext,
                uncompletedCount = uncompletedCount
            )

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

    fun onSelectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }
    fun onAddClick() {
        _editingItem.value = null
        _uiState.update { it.copy(
            showInputBox = true,
            editingItem = null,
        ) }
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
        }
    }

    fun onEditSavedItem(schedule: Schedule) {
        _editingItem.value = schedule
        _uiState.update { it.copy(
            showInputBox = true,
            editingItem = schedule,
            selectedCategory = schedule.category
        ) }
    }

    fun updateItem(schedule: Schedule, newText: String, newDate: String, newTime: String, newCategory: String) {
        viewModelScope.launch {
            val updatedSchedule = schedule.copy(text = newText, date = newDate, time = newTime, category = newCategory)
            schedulesRepository.updateSchedule(updatedSchedule)
            onDismissInputBox()
        }
    }

    fun deleteItem(schedule: Schedule) {
        viewModelScope.launch {
            schedulesRepository.deleteSchedule(schedule)
            onDismissInputBox()
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

            val uncompletedCount = uiState.value.scheduleList.count { !it.isCompleted }
            updateOngoingTaskCountNotification(
                context = application.applicationContext,
                uncompletedCount = uncompletedCount
            )
        }
    }

    // チェックボックスの切り替え処理
    fun toggleScheduleStatus(schedule: Schedule, isChecked: Boolean) {
        viewModelScope.launch {
            // 1. 完了状態を書き換えたデータを作成
            val updatedSchedule = schedule.copy(isCompleted = isChecked)

            // 2. 先にデータベースを更新して確定させる
            schedulesRepository.updateSchedule(updatedSchedule)

            // 3. データベースの更新が反映されるまで少し待機（順序を上に移動）
            kotlinx.coroutines.delay(150)

            // 4. データベース書き換え完了後にアラームを安全に消去する
            // 画面側の自動登録ロジックが動いた後に、上から上書きして確実にアラームを消し去ります
            if (isChecked) {
                // ★引数に schedule.text を追記して、予約時と全く同じタイトル情報を手渡します
                cancelTodoAlarm(
                    context = application.applicationContext,
                    taskId = schedule.id,
                    taskTitle = schedule.text
                )
            }

            // 5. 最新のリストから未完了タスクだけをカウントして常駐通知を更新
            val currentList = uiState.value.scheduleList
            val uncompletedCount = currentList.count { !it.isCompleted }
            updateOngoingTaskCountNotification(
                context = application.applicationContext,
                uncompletedCount = uncompletedCount
            )
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
package com.example.inventory.ui.home

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.app.Application
import com.example.inventory.cancelTodoAlarm
import com.example.inventory.updateOngoingTaskCountNotification
import com.example.inventory.scheduleTodoAlarm // 💡5分前アラームの予約関数をインポート
import com.example.inventory.convertDateTimeToMillis // 💡日時文字列のミリ秒変換関数をインポート
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

    // 💡【追加】アプリが起動した瞬間に15分待たずに今すぐ通知を強制更新する初期化処理
    init {
        refreshOngoingNotification()
    }

    // 💡【共通処理】期限切れの未完了タスク数だけを数えて通知を最新にする関数
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

    // 🟢 タスク追加（5分前アラーム自動予約付きに修正）
    fun addText(text: String, date: String, time: String, category: String, detail: String) {
        viewModelScope.launch {
            // 💡 選択された日時をミリ秒に変換
            val taskTimeMillis = convertDateTimeToMillis(date, time) ?: System.currentTimeMillis()

            // 💡 変換したミリ秒（taskTimeMillis）をdataフィールドにしっかり格納して保存
            val newSchedule = Schedule(
                text = text,
                date = date,
                time = time,
                category = category,
                detail = detail,
                data = taskTimeMillis // ⭕ これで未来と過去の判定が正しくなります
            )
            schedulesRepository.insertSchedule(newSchedule)

            // 💡 保存直後、DBが自動生成した本物のID（確定した背番号）を特定してアラームをセットする
            val savedList = schedulesRepository.getAllSchedulesStream().first()
            val savedItem = savedList.find { it.text == text && it.date == date && it.time == time }

            if (savedItem != null && taskTimeMillis > System.currentTimeMillis()) {
                scheduleTodoAlarm(
                    context = application.applicationContext,
                    taskId = savedItem.id, // ⭕ ズレのない本物のIDを使用
                    taskTitle = text,
                    taskTimeMillis = taskTimeMillis
                )
            }

            onDismissInputBox()
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

    // 🟢 タスク編集（5分前アラーム自動再予約付きに修正）
    fun updateItem(schedule: Schedule, newText: String, newDate: String, newTime: String, newCategory: String, newDetail: String) {
        viewModelScope.launch {
            // 💡 新しく選択された日時をミリ秒に変換
            val taskTimeMillis = convertDateTimeToMillis(newDate, newTime) ?: System.currentTimeMillis()

            val updatedSchedule = schedule.copy(
                text = newText,
                date = newDate,
                time = newTime,
                category = newCategory,
                detail = newDetail,
                data = taskTimeMillis // ⭕ 更新された日時を正確に上書き
            )
            schedulesRepository.updateSchedule(updatedSchedule)

            // 💡 一度古いアラームを安全にキャンセルし、新日時でアラームを再予約
            cancelTodoAlarm(
                context = application.applicationContext,
                taskId = schedule.id,
                taskTitle = schedule.text
            )

            if (taskTimeMillis > System.currentTimeMillis()) {
                scheduleTodoAlarm(
                    context = application.applicationContext,
                    taskId = schedule.id, // ⭕ 既存の本物のIDを引き継ぐ
                    taskTitle = newText,
                    taskTimeMillis = taskTimeMillis
                )
            }

            onDismissInputBox()
            refreshOngoingNotification()
        }
    }

    fun deleteItem(schedule: Schedule) {
        viewModelScope.launch {
            schedulesRepository.deleteSchedule(schedule)

            // 💡 タスク削除時にも連動してアラームを完全に消去する
            cancelTodoAlarm(
                context = application.applicationContext,
                taskId = schedule.id,
                taskTitle = schedule.text
            )

            onDismissInputBox()
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
            } else {
                // 💡 チェックを外して未完了に戻した場合は、5分前アラームを再度自動予約する
                val taskTimeMillis = convertDateTimeToMillis(schedule.date, schedule.time)
                if (taskTimeMillis != null && taskTimeMillis > System.currentTimeMillis()) {
                    scheduleTodoAlarm(
                        context = application.applicationContext,
                        taskId = schedule.id,
                        taskTitle = schedule.text,
                        taskTimeMillis = taskTimeMillis
                    )
                }
            }
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

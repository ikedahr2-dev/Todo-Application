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

    // 1. 【ここがキモ】ダイアログなどの「UI状態」を管理する
    private val _uiState = MutableStateFlow(HomeUiState())

    // 2. 【ここがキモ】DBのデータとUI状態を「ガッチャンコ」して HomeScreen に流す
    val uiState: StateFlow<HomeUiState> = schedulesRepository.getAllSchedulesStream()
        .combine(_uiState) { dbList, currentUiState ->

            // ★ここを確実に「trueではないもの（＝未完了）」だけ数えるように書き換えます
            val uncompletedCount = dbList.count { it.isCompleted == false }

            updateOngoingTaskCountNotification(
                context = application.applicationContext,
                uncompletedCount = uncompletedCount
            )

            val filteredList =
                if (currentUiState.searchQuery.isBlank()) {
                    dbList
                } else {
                    dbList.filter { schedule ->
                        schedule.text.contains(
                            currentUiState.searchQuery,
                            ignoreCase = true
                        )
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
            selectedCategory = ""
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
    //チェックボックスの切り替え処理
    // チェックボックスの切り替え処理
    fun toggleScheduleStatus(schedule: Schedule, isChecked: Boolean) {
        viewModelScope.launch {
            // 1. データベース上の完了状態を更新
            val updatedSchedule = schedule.copy(isCompleted = isChecked)
            schedulesRepository.updateSchedule(updatedSchedule)

            // 2. もし完了（チェックON）にされたら、5分前アラームをキャンセルする
            if (isChecked) {
                cancelTodoAlarm(context = application.applicationContext, taskId = schedule.id)
            }

            // ★【ここを修正】一瞬だけデータベースの書き換え完了を待つ（100ミリ秒）
            kotlinx.coroutines.delay(100)

            // 3. 最新のリストから「isCompleted == false」の未完了タスクだけを正しくカウントする
            val currentList = uiState.value.scheduleList
            val uncompletedCount = currentList.count { !it.isCompleted }

            // 4. 正しい件数で常駐通知を更新
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
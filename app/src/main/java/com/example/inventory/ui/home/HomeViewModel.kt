package com.example.inventory.ui.home

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inventory.data.Schedule
import com.example.inventory.data.SchedulesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val scheduleList: List<Schedule> = listOf(),
    val showDatePicker: Boolean = false,
    val showInputBox: Boolean = false,
    val editingItem: Schedule? = null
)

class HomeViewModel(
    private val schedulesRepository: SchedulesRepository
) : ViewModel() {

    // 1. 【ここがキモ】ダイアログなどの「UI状態」を管理する
    private val _uiState = MutableStateFlow(HomeUiState())

    // 2. 【ここがキモ】DBのデータとUI状態を「ガッチャンコ」して HomeScreen に流す
    val uiState: StateFlow<HomeUiState> = schedulesRepository.getAllSchedulesStream()
        .combine(_uiState) { dbList, currentUiState ->
            currentUiState.copy(scheduleList = dbList)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            initialValue = HomeUiState()
        )

    private var _editingItem = mutableStateOf<Schedule?>(null)
    val editingItem: State<Schedule?> = _editingItem

    fun onAddClick() {
        _editingItem.value = null
        _uiState.update { it.copy(
            showInputBox = true,
            editingItem = null) }
    }

    fun onDismissInputBox() {
        _editingItem.value = null
        _uiState.update { it.copy(showInputBox = false) }
    }

    fun addText(text: String, date: String, time: String) {
        viewModelScope.launch {
            val newSchedule = Schedule(text = text, date = date, time = time)
            schedulesRepository.insertSchedule(newSchedule)
            onDismissInputBox()
        }
    }

    fun onEditSavedItem(schedule: Schedule) {
        _editingItem.value = schedule
        _uiState.update { it.copy(
            showInputBox = true,
            editingItem = schedule) }
    }

    fun updateItem(schedule: Schedule, newText: String, newDate: String, newTime: String) {
        viewModelScope.launch {
            val updatedSchedule = schedule.copy(text = newText, date = newDate, time = newTime)
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

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}
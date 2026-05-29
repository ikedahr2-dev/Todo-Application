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
import com.example.inventory.data.Game
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
    val waterStoredPercent: Int = 0
)

class HomeViewModel(
    private val schedulesRepository: SchedulesRepository,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())

    // ----- Game ----- //
    val gameUiState: StateFlow<Game?> = schedulesRepository.getGameStatusStream()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

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

    private val processedTasksPrefs = application.getSharedPreferences("game_processed_tasks", Context.MODE_PRIVATE)

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
                reminderMinutes = finalReminderMinutes,
                actualStartMillis = null // 新規作成時は未計測
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
            processedTasksPrefs.edit().remove("task_done_${schedule.id}").apply()
            onDismissInputBox()
            refreshOngoingNotification()
        }
    }

    // バックアップ用＆自動完了タスクの回収ロジック
    fun calculateWaterFromCompletedTasks() {
        viewModelScope.launch {
            val allSchedules = schedulesRepository.getAllSchedulesStream().first()
            var addedPercentTotal = 0
            val currentTime = System.currentTimeMillis()

            allSchedules.forEach { schedule ->
                val key = "task_done_${schedule.id}"
                val isAlreadyProcessed = processedTasksPrefs.getBoolean(key, false)

                val taskEndTimeMillis = convertDateTimeToMillis(schedule.date, schedule.endTime.ifBlank { schedule.time })
                val isPastEndTime = taskEndTimeMillis != null && currentTime >= taskEndTimeMillis

                // 手動で両方埋まった、または時間が過ぎて自動的に完了した未チャージタスク
                if ((schedule.isCompleted && schedule.isEndCompleted) || (isPastEndTime && !isAlreadyProcessed)) {
                    if (!isAlreadyProcessed) {
                        // 開始時刻の特定（フライングロックされた開始時間、または通常の予定開始時間）
                        val startMillis = schedule.actualStartMillis
                            ?: convertDateTimeToMillis(schedule.date, schedule.time)

                        // 終了時刻の特定（手動チェックなら現在時刻、時間超過自動完了なら予定終了時刻）
                        val endMillis = if (schedule.isEndCompleted) currentTime else taskEndTimeMillis

                        if (startMillis != null && endMillis != null && endMillis > startMillis) {
                            val diffMinutes = (endMillis - startMillis) / (1000 * 60)
                            val chargedPercent = ((diffMinutes.toDouble() / 60.0) * 100.0).toInt()
                            addedPercentTotal += chargedPercent
                        }
                        processedTasksPrefs.edit().putBoolean(key, true).apply()
                    }
                }
                // チェックボックスが外された場合の減算処理
                else if (!schedule.isCompleted || !schedule.isEndCompleted) {
                    if (processedTasksPrefs.getBoolean(key, false)) {
                        val startMillis = schedule.actualStartMillis ?: convertDateTimeToMillis(schedule.date, schedule.time)
                        val endMillis = convertDateTimeToMillis(schedule.date, schedule.endTime.ifBlank { schedule.time })
                        if (startMillis != null && endMillis != null && endMillis > startMillis) {
                            val diffMinutes = (endMillis - startMillis) / (1000 * 60)
                            val chargedPercent = ((diffMinutes.toDouble() / 60.0) * 100.0).toInt()
                            addedPercentTotal -= chargedPercent
                        }
                        processedTasksPrefs.edit().remove(key).apply()
                    }
                }
            }

            if (addedPercentTotal != 0) {
                val currentStored = _uiState.value.waterStoredPercent
                val newWaterPercent = (currentStored + addedPercentTotal).coerceIn(0, 2400)
                saveWaterToDevice(newWaterPercent)
                _uiState.update { it.copy(waterStoredPercent = newWaterPercent) }
            }
        }
    }

    fun deleteCompletedSchedules() {
        viewModelScope.launch {
            val allSchedules = schedulesRepository.getAllSchedulesStream().first()
            val fullyCompletedList = allSchedules.filter { it.isEndCompleted }

            fullyCompletedList.forEach { schedule ->
                schedulesRepository.deleteSchedule(schedule)
                cancelTodoAlarm(context = application.applicationContext, taskId = schedule.id, taskTitle = schedule.text, reminderMinutes = schedule.reminderMinutes)
                processedTasksPrefs.edit().remove("task_done_${schedule.id}").apply()
            }
            refreshOngoingNotification()
        }
    }

    // 1つ目のチェックボックス（開始）の更新関数
    fun toggleScheduleStatus(schedule: Schedule, isChecked: Boolean) {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            val planStartTimeMillis = convertDateTimeToMillis(schedule.date, schedule.time) ?: currentTime

            // 💡 どんなに早くチェックしても予定開始時刻(planStartTimeMillis)より前なら予定時刻をスタートとする（フライング防止）
            val actualStart = if (isChecked) {
                java.lang.Long.max(planStartTimeMillis, currentTime)
            } else {
                null
            }

            val updatedSchedule = schedule.copy(
                isCompleted = isChecked,
                actualStartMillis = actualStart
            )
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
                val targetEndTime = schedule.endTime
                if (targetEndTime.isNotBlank()) {
                    val endAlarmTimeMillis = convertDateTimeToMillis(schedule.date, targetEndTime)
                    if (endAlarmTimeMillis != null && endAlarmTimeMillis > System.currentTimeMillis()) {
                        scheduleTodoEndAlarm(context = application.applicationContext, taskId = schedule.id, taskTitle = schedule.text, endTimeMillis = endAlarmTimeMillis)
                    }
                }
            }
            calculateWaterFromCompletedTasks()
            refreshOngoingNotification()
        }
    }

    // 2つ目のチェックボックス（終了）の更新関数
    fun toggleScheduleEndStatus(schedule: Schedule, isChecked: Boolean) {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()

            val updatedSchedule = schedule.copy(isEndCompleted = isChecked)
            schedulesRepository.updateSchedule(updatedSchedule)

            // 💡 リアルタイムでのチャージ処理
            if (isChecked) {
                val key = "task_done_${schedule.id}"
                val isAlreadyProcessed = processedTasksPrefs.getBoolean(key, false)

                if (!isAlreadyProcessed) {
                    val startMillis = schedule.actualStartMillis
                        ?: convertDateTimeToMillis(schedule.date, schedule.time)
                        ?: currentTime

                    val diffMinutes = ((currentTime - startMillis) / (1000 * 60)).coerceAtLeast(0)
                    val chargedPercent = ((diffMinutes.toDouble() / 60.0) * 100.0).toInt()

                    if (chargedPercent > 0) {
                        val currentStored = _uiState.value.waterStoredPercent
                        val newWaterPercent = (currentStored + chargedPercent).coerceIn(0, 2400)
                        saveWaterToDevice(newWaterPercent)
                        _uiState.update { it.copy(waterStoredPercent = newWaterPercent) }
                        processedTasksPrefs.edit().putBoolean(key, true).apply()
                    }
                }
            } else {
                val key = "task_done_${schedule.id}"
                processedTasksPrefs.edit().remove(key).apply()
            }

            kotlinx.coroutines.delay(150)
            calculateWaterFromCompletedTasks()
            refreshOngoingNotification()
        }
    }

    fun updateWaterStoredPercent(newPercent: Int) {
        viewModelScope.launch {
            val boundedPercent = newPercent.coerceIn(0, 2400)
            saveWaterToDevice(boundedPercent)
            _uiState.update { it.copy(waterStoredPercent = boundedPercent) }
        }
    }

    fun updateSearchQuery(query: String) { _uiState.update { it.copy(searchQuery = query) } }

    companion object { private const val TIMEOUT_MILLIS = 5_000L }

// ---------- Game ---------- //
    fun waterTree() {
        val currentGame = gameUiState.value

        viewModelScope.launch {
            // データが無いときは、最初は「水0%」で初期登録
            if (currentGame == null) {
                schedulesRepository.insertGameStatus(
                    Game(
                        id = 1,
                        waterStoredPercent = 0,
                        currentLevel = 0,
                        givenWaterCount = 0,
                        currentHeightLayer = 0
                    )
                )
                return@launch
            }

            val currentHeightLayer = currentGame.currentHeightLayer
            val currentLevel = currentGame.currentLevel
            val givenWaterCount = currentGame.givenWaterCount
            val waterStoredPercent = currentGame.waterStoredPercent   // 【取得】蓄えた水

            if (waterStoredPercent >= 100 && currentLevel < 14) {
                var newLevel = currentLevel
                var newCount = givenWaterCount + 1
                var newLayer = currentHeightLayer

                val required = getRequiredCount(newLevel)

                if (newCount >= required) {
                    newLevel++
                    newCount = 0

                    when (newLevel) {
                        6 -> newLayer = 1
                        8 -> newLayer = 2
                        10 -> newLayer = 3
                        12 -> newLayer = 4
                    }
                }

                schedulesRepository.insertGameStatus(
                    Game(
                        id = 1,
                        waterStoredPercent = waterStoredPercent - 100,
                        currentLevel = newLevel,
                        givenWaterCount = newCount,
                        currentHeightLayer = newLayer
                    )
                )
            }
        }
    }

    // GameScreenから移動した関数
    private fun getRequiredCount(level: Int): Int {
        return when (level) {
            0 -> 3     //Lv.0 -> Lv.1
            1 -> 18    //Lv.1 -> Lv.2
            2 -> 39    //Lv.2 -> Lv.3
            3 -> 52    //Lv.3 -> Lv.4
            4 -> 80    //Lv.4 -> Lv.5
            5 -> 120   //Lv.5 -> Lv.6
            6 -> 160   //Lv.6 -> Lv.7
            7 -> 250   //Lv.7 -> Lv.8
            8 -> 380   //Lv.8 -> Lv.9
            9 -> 470   //Lv.9 -> Lv.10
            10 -> 630  //Lv.10 -> Lv.11
            11 -> 670  //Lv.11 -> Lv.12
            12 -> 852  //Lv.12 -> Lv.13
            13 -> 1000 //Lv.13 -> Lv.14 (Max)
            else -> 0
        }
    }

    // ----- 階層管理 ----- //
    fun changeLayer(isUp: Boolean) {
        // 現在のゲームデータを取得
        val currentGame = gameUiState.value ?: Game()
        // 今いる位置の取得
        val currentLayer = currentGame.currentHeightLayer

        viewModelScope.launch {
            // 階層ごとにcurrentHeightLayerだけを+-1して、DBに保存
            schedulesRepository.updateGameStatus(
                currentGame.copy(
                    currentHeightLayer = if (isUp) currentLayer + 1 else currentLayer - 1
                )
            )
        }
    }

    // ----- 木の初期化(水以外) ----- //

    fun resetGameKeepWater() {
        // 現在のゲームデータの取得
        val currentGame = gameUiState.value ?: return // もしデータがなければ何もしない

        // 現在の蓄えられた水の取得
        val currentWater = currentGame.waterStoredPercent

        viewModelScope.launch {
            // idと水だけを引き継ぎ、それ以外を「0」に初期化してDBをUpdateする！
            schedulesRepository.insertGameStatus(
                Game(
                    id = 1,                            // idそのまま
                    waterStoredPercent = currentWater, // 水そのまま
                    currentLevel = 0,                  // レベル初期化
                    givenWaterCount = 0,               // 水あげた回数初期化
                    currentHeightLayer = 0             // 階層初期化
                )
            )
        }
    }
}
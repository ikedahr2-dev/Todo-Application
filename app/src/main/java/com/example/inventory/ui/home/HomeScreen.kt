package com.example.inventory.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inventory.R
import com.example.inventory.convertDateTimeToMillis
import com.example.inventory.data.Schedule
import com.example.inventory.scheduleTodoAlarm
import com.example.inventory.ui.AppViewModelProvider
import com.example.inventory.ui.navigation.NavigationDestination
import com.example.inventory.ui.theme.md_theme_light_primary
import com.example.inventory.updateOngoingTaskCountNotification
import java.text.SimpleDateFormat
import java.util.Locale

object HomeDestination : NavigationDestination {
    override val route = "home"
    override val titleRes = R.string.app_name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigateToItemEntry: () -> Unit,
    navigateToItemUpdate: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val today = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(java.util.Date())
    val context = LocalContext.current
    //リストに変化があったら自動で常駐通知の件数を更新する
    LaunchedEffect(uiState.scheduleList) {
        // 現在のリストの全件数を未完了数としてカウント（もし完了フラグ等があれば filter { !it.isCompleted } などにしてください）
        val uncompletedCount = uiState.scheduleList.size
        updateOngoingTaskCountNotification(context, uncompletedCount)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var showCalendar by remember { mutableStateOf(false) }
    val selectedCategory = uiState.selectedCategory ?: ""
    Scaffold(
        //modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        bottomBar = {
            ViewToggleButton(
                onListClick = { showCalendar = false; /*selectedDate = "onListClick"*/ },
                onCalendarClick = { showCalendar = true }
            )
        },
        floatingActionButton = {
            if (!showCalendar) {
                FloatingActionButton(
                    onClick = {
                        selectedDate = ""
                        selectedTime = ""
                        viewModel.onAddClick()
                    },
                    shape = CircleShape,
                    containerColor = Color.White,
                    contentColor = md_theme_light_primary,
                    modifier = Modifier
                        .offset(y = (-15).dp)
                        .size(75.dp)
                        .border(BorderStroke(1.5.dp, md_theme_light_primary), CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (showCalendar) {
                LaunchedEffect(Unit) {
                    selectedDate = ""
                }
                Box(modifier = Modifier.padding(innerPadding)) {
                    CalendarScreen(
                        scheduleList = uiState.scheduleList,
                        selectedDate = selectedDate,
                        onDateSelected = { date -> selectedDate = date },
                        onCalendarItemClick = { schedule ->
                            selectedDate = schedule.date
                            selectedTime = schedule.time
                            viewModel.onEditSavedItem(schedule)
                        },
                        onAddClick = {
                            if (selectedDate.isNotBlank()) {
                                selectedTime = ""
                                viewModel.onAddClick()
                            }
                        }
                    )
                }
            } else {
                HomeBody(
                    scheduleList = uiState.scheduleList,
                    onEditItem = { schedule ->
                        // 編集時に日付と時間を同期
                        selectedDate = schedule.date
                        selectedTime = schedule.time
                        viewModel.onEditSavedItem(schedule)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            // 入力ダイアログ
            if (uiState.showInputBox) {
                ScheduleInputDialog(
                    initialText = uiState.editingItem?.text ?: "",
                    onDismiss = { viewModel.onDismissInputBox() },
                    onSave = { text, date, time, category ->
                        val item = uiState.editingItem
                        if (item != null) {
                            viewModel.updateItem(item, text, date, time, category)
                        } else {
                            viewModel.addText(text, date, time, category)
                        }

                        // アラーム予約ロジック
                        val taskTimeMillis = convertDateTimeToMillis(date, time)
                        if (taskTimeMillis != null) {
                            val idForAlarm = item?.id ?: taskTimeMillis.hashCode()
                            scheduleTodoAlarm(
                                context = context,
                                taskId = idForAlarm,
                                taskTitle = text,
                                taskTimeMillis = taskTimeMillis
                            )
                        }
                        viewModel.onDismissInputBox()
                    },
                    onDelete = uiState.editingItem?.let { item ->
                        { viewModel.deleteItem(item) }
                    },
                    onSelectDate = { showDatePicker = true },
                    onSelectTime = { showTimePicker = true },
                    selectedDate = selectedDate,
                    selectedTime = selectedTime,

                )
            }

            // DatePicker
            if (showDatePicker) {
                val datePickerState = rememberDatePickerState()
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            selectedDate = datePickerState.selectedDateMillis?.let {
                                SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(java.util.Date(it))
                            } ?: selectedDate
                            showDatePicker = false
                        }) { Text(stringResource(R.string.enter)) }
                    }
                ) { DatePicker(state = datePickerState) }
            }

            // TimePicker
            if (showTimePicker) {
                val timePickerState = rememberTimePickerState()
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            selectedTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                            showTimePicker = false
                        }) { Text(stringResource(R.string.enter)) }
                    },
                    dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) } },
                    text = { TimePicker(state = timePickerState) }
                )
            }
        }
    }
}

@Composable
private fun HomeBody(
    scheduleList: List<Schedule>,
    onEditItem: (Schedule) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    // 日付ごとにグループ化
    val groupedSchedules = scheduleList
        .sortedWith(compareBy<Schedule> { it.date }.thenBy { it.time })
        .groupBy { it.date }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        if (scheduleList.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.no_item_description),
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        } else {
            groupedSchedules.forEach { (date, schedules) ->
                // --- 日付の見出し (例: // 2026/05/15 の予定) ---
                item {
                    Text(
                        text = "// $date の予定",
                        color = md_theme_light_primary,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }

                // --- その日の予定リスト ---
                items(schedules) { schedule ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(1.5.dp, md_theme_light_primary, RoundedCornerShape(8.dp))
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .clickable { onEditItem(schedule) }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // ★【ここを追加】完了・未完了のチェックボックス
                            Checkbox(
                                checked = schedule.isCompleted,
                                onCheckedChange = { isChecked ->
                                    // 先ほど ViewModel に追加した関数を呼び出す
                                    viewModel.toggleScheduleStatus(schedule, isChecked)
                                },
                                modifier = Modifier.padding(end = 8.dp) // 文字との間に少し隙間を空ける
                            )

                            // 【元からあるテキスト】完了時は打ち消し線を引くようにアレンジ
                            Text(
                                text = schedule.text,
                                fontSize = 24.sp,
                                modifier = Modifier.weight(1f),
                                style = androidx.compose.ui.text.TextStyle(
                                    textDecoration = if (schedule.isCompleted) {
                                        androidx.compose.ui.text.style.TextDecoration.LineThrough
                                    } else {
                                        androidx.compose.ui.text.style.TextDecoration.None
                                    }
                                )
                            )

                            // 【元からあるテキスト】時間
                            Text(text = schedule.time, fontSize = 20.sp, color = Color.DarkGray)
                        }
                    }
                }
            }

            // スクロール用余白
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun ViewToggleButton(onListClick: () -> Unit, onCalendarClick: () -> Unit) {
    BottomAppBar(containerColor = MaterialTheme.colorScheme.surface) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onListClick, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.small) { Text("リスト") }
            Button(onClick = onCalendarClick, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.small) { Text("カレンダー") }
        }
    }
}
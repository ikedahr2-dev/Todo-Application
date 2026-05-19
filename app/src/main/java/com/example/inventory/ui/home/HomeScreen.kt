package com.example.inventory.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inventory.R
import com.example.inventory.convertDateTimeToMillis
import com.example.inventory.data.Schedule
import com.example.inventory.scheduleTodoAlarm
import com.example.inventory.ui.AppViewModelProvider
import com.example.inventory.ui.navigation.NavigationDestination
import com.example.inventory.ui.theme.md_theme_light_primary
import com.example.inventory.updateOngoingTaskCountNotification
import com.example.inventory.cancelTodoAlarm
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.lazy.LazyListScope
import com.example.inventory.ui.theme.md_theme_dark_time
import com.example.inventory.ui.theme.md_theme_light_time
import kotlin.collections.component1
import kotlin.collections.component2
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
    val context = LocalContext.current

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var showCalendar by remember { mutableStateOf(false) }
    val selectedFilterCategory = uiState.selectedFilterCategory
    val selectedEditCategory = uiState.selectedEditCategory

    Scaffold(
        bottomBar = {
            ViewToggleButton(
                onListClick = { showCalendar = false; },
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
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .offset(y = (-15).dp)
                        .size(75.dp)
                        .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary), CircleShape)
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
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    val selectedCategoryTab =
                        if (selectedFilterCategory.isBlank()) {
                            "すべて"
                        } else {
                            selectedFilterCategory
                        }

                    val categories = listOf("すべて", "仕事", "プライベート", "その他")

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { category ->
                            FilterChip(
                                selected = (category == selectedCategoryTab),
                                onClick = {
                                    if (category == "すべて") {
                                        viewModel.onSelectFilterCategory("")
                                    } else {
                                        viewModel.onSelectFilterCategory(category)
                                    }
                                },
                                label = {
                                    Text(text = category, fontSize = 16.sp)
                                },
                                modifier = Modifier
                                    .padding(vertical = 16.dp)
                                    .height(40.dp),
                                shape = CircleShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,

                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    // 内部処理用のデータリストの分離処理
                    val uncompletedSchedules = uiState.scheduleList.filter { !it.isCompleted }
                    val completedSchedules = uiState.scheduleList.filter { it.isCompleted }

                    val groupedUncompleted = uncompletedSchedules
                        .sortedWith(compareBy<Schedule> { it.date }.thenBy { it.time })
                        .groupBy { it.date }

                    val strokeColor = MaterialTheme.colorScheme.primary
                    val interactionSource = remember { MutableInteractionSource() }

                    BasicTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        interactionSource = interactionSource,
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp)
                            .drawWithContent {
                                drawContent()
                                val stroke = Stroke(
                                    width = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(
                                        intervals = floatArrayOf(12.dp.toPx(), 8.dp.toPx()),
                                        phase = 0f
                                    )
                                )
                                drawRoundRect(
                                    color = strokeColor,
                                    style = stroke,
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                                )
                            },
                        decorationBox = { innerTextField ->
                            OutlinedTextFieldDefaults.DecorationBox(
                                value = uiState.searchQuery,
                                innerTextField = innerTextField,
                                enabled = true,
                                singleLine = true,
                                visualTransformation = VisualTransformation.None,
                                interactionSource = interactionSource,
                                placeholder = {
                                    Text(
                                        text = "予定を検索",
                                        color = strokeColor.copy(alpha = 0.6f),
                                        fontSize = 16.sp
                                    )
                                },
                                contentPadding = PaddingValues(
                                    horizontal = 12.dp,
                                    vertical = 10.dp
                                ),
                                container = {}
                            )
                        }
                    )

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp,
                            top = 0.dp
                        )
                    ) {

                        if (uiState.scheduleList.isEmpty()) {

                            item {
                                Text(
                                    text = stringResource(R.string.no_item_description),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 32.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }

                        } else {

                            if (
                                selectedFilterCategory != "仕事" &&
                                selectedFilterCategory != "プライベート" &&
                                selectedFilterCategory != "その他"
                            ) {

                                scheduleMainList(
                                    groupedUncompleted = groupedUncompleted,
                                    completedSchedules = completedSchedules,
                                    viewModel = viewModel,
                                    onEditSavedItem = { schedule ->
                                        selectedDate = schedule.date
                                        selectedTime = schedule.time
                                        viewModel.onEditSavedItem(schedule)
                                    }
                                )

                            } else {

                                val filteredSchedules =
                                    uiState.scheduleList.filter {
                                        it.category == selectedFilterCategory
                                    }

                                val filteredUncompletedGroup = filteredSchedules
                                    .filter { !it.isCompleted }
                                    .sortedWith(compareBy<Schedule> { it.date }.thenBy { it.time })
                                    .groupBy { it.date }

                                val filteredCompletedList =
                                    filteredSchedules.filter { it.isCompleted }

                                scheduleMainList(
                                    groupedUncompleted = filteredUncompletedGroup,
                                    completedSchedules = filteredCompletedList,
                                    viewModel = viewModel,
                                    onEditSavedItem = { schedule ->
                                        selectedDate = schedule.date
                                        selectedTime = schedule.time
                                        viewModel.onEditSavedItem(schedule)
                                    }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(100.dp))
                            }
                        }
                    }
                }
            }

            // 入力ダイアログ
            if (uiState.showInputBox) {
                ScheduleInputDialog(
                    initialText = uiState.editingItem?.text ?: "",
                    initialDetail = uiState.editingItem?.detail ?: "",
                    onDismiss = { viewModel.onDismissInputBox() },
                    onSave = { text, date, time, category, detail ->
                        val item = uiState.editingItem
                        if (item != null) {
                            viewModel.updateItem(item, text, date, time, category,detail)
                        } else {
                            viewModel.addText(text, date, time, category,detail)
                        }

                        // アラーム予約ロジック
                        val taskTimeMillis = convertDateTimeToMillis(date, time)
                        if (taskTimeMillis != null) {
                            val idForAlarm = item?.id ?: taskTimeMillis.hashCode()
                            // item?.isCompleted == true の時は予約しない（ガードをかける）
                            if (item?.isCompleted != true) {
                                scheduleTodoAlarm(
                                    context = context,
                                    taskId = idForAlarm,
                                    taskTitle = text,
                                    taskTimeMillis = taskTimeMillis
                                )
                            } else {
                                // 💡 すでに完了しているタスクを編集・保存した場合は、念のため古いアラームを確実に消去しておく
                                cancelTodoAlarm(
                                    context = context,
                                    taskId = idForAlarm,
                                    taskTitle = text
                                )
                            }
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
                    selectedCategory = selectedEditCategory,
                    onSelectCategory = {
                        viewModel.onSelectEditCategory(it)
                    }
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

// アイテム行コンポーネント (共通再利用部品として最下部に配置)
@Composable
private fun ScheduleItemRow(
    schedule: Schedule,
    onEditItem: (Schedule) -> Unit,
    viewModel: HomeViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.5.dp, md_theme_light_primary, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .clickable { onEditItem(schedule) }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = schedule.isCompleted,
                onCheckedChange = { isChecked ->
                    viewModel.toggleScheduleStatus(schedule, isChecked)
                },
                modifier = Modifier.padding(end = 8.dp)
            )

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

            Text(text = schedule.time, fontSize = 20.sp, color = if (isSystemInDarkTheme()) md_theme_dark_time else md_theme_light_time)
        }
    }
}

// ---------- 未完・完了 ---------- //
private fun LazyListScope.scheduleMainList(
    groupedUncompleted: Map<String, List<Schedule>>,
    completedSchedules: List<Schedule>,
    viewModel: HomeViewModel,
    onEditSavedItem: (Schedule) -> Unit
) {

    groupedUncompleted.forEach { (date, schedules) ->
        item {
            Text(
                text = "$date の予定",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
        }

        items(schedules) { schedule ->
            ScheduleItemRow(
                schedule = schedule,
                onEditItem = { onEditSavedItem(schedule) },
                viewModel = viewModel
            )
        }
    }

    if (completedSchedules.isNotEmpty()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "完了した予定", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)

                Button(
                    onClick = { viewModel.deleteCompletedSchedules() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD72323)),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("完了した予定を一括削除", fontSize = 12.sp, color = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        items(completedSchedules) { schedule ->
            ScheduleItemRow(
                schedule = schedule,
                onEditItem = { onEditSavedItem(schedule) },
                viewModel = viewModel
            )
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


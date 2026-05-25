package com.example.inventory.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inventory.R
import com.example.inventory.convertDateTimeToMillis
import com.example.inventory.data.Schedule
import com.example.inventory.ui.AppViewModelProvider
import com.example.inventory.ui.navigation.NavigationDestination
import com.example.inventory.ui.theme.md_theme_light_primary
import com.example.inventory.ui.theme.md_theme_dark_time
import com.example.inventory.ui.theme.md_theme_light_time
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HomeDestination : NavigationDestination {
    override val route = "home"
    override val titleRes = R.string.app_name
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    var showEndTimePicker by remember { mutableStateOf(false) }

    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var selectedEndTime by remember { mutableStateOf("") }

    var currentScreenMode by remember { mutableStateOf(0) }

    val selectedFilterCategory = uiState.selectedFilterCategory
    val selectedEditCategory = uiState.selectedEditCategory
    val dynamicCategories = uiState.categoriesList

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryText by remember { mutableStateOf("") }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }

    // 💡 追加：長押しで削除しようとしているタスクを保持する変数
    var scheduleToDelete by remember { mutableStateOf<Schedule?>(null) }

    val selectedCategoryTab = if (selectedFilterCategory.isBlank()) "すべて" else selectedFilterCategory

    Scaffold(
        bottomBar = {
            ViewToggleButton(
                currentMode = currentScreenMode,
                onModeChange = { currentScreenMode = it }
            )
        },
        floatingActionButton = {
            if (currentScreenMode == 0) {
                FloatingActionButton(
                    onClick = {
                        selectedDate = ""
                        selectedTime = ""
                        selectedEndTime = ""
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
            when (currentScreenMode) {
                1 -> {
                    LaunchedEffect(Unit) { selectedDate = "" }
                    Box(modifier = Modifier.padding(innerPadding)) {
                        CalendarScreen(
                            scheduleList = uiState.scheduleList,
                            categories = dynamicCategories,
                            selectedCategory = if (selectedFilterCategory.isBlank()) "すべて" else selectedFilterCategory,
                            onCategorySelected = { category ->
                                if (category == "すべて") viewModel.onSelectFilterCategory("") else viewModel.onSelectFilterCategory(category)
                            },
                            selectedDate = selectedDate,
                            onDateSelected = { date -> selectedDate = date },
                            onCalendarItemClick = { schedule ->
                                selectedDate = schedule.date
                                selectedTime = schedule.time
                                selectedEndTime = schedule.endTime ?: ""
                                viewModel.onEditSavedItem(schedule)
                            },
                            onAddClick = {
                                if (selectedDate.isNotBlank()) {
                                    selectedTime = ""
                                    selectedEndTime = ""
                                    viewModel.onAddClick()
                                }
                            },
                            viewModel = viewModel
                        )
                    }
                }
                2 -> {
                    if (selectedDate.isBlank()) {
                        selectedDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
                    }
                    Box(modifier = Modifier.padding(innerPadding)) {
                        TimelineScreen(
                            scheduleList = uiState.scheduleList,
                            selectedDate = selectedDate,
                            onDateChange = { newDate -> selectedDate = newDate },
                            viewModel = viewModel,
                            onTimelineItemClick = { schedule ->
                                selectedDate = schedule.date
                                selectedTime = schedule.time
                                selectedEndTime = schedule.endTime ?: ""
                                viewModel.onEditSavedItem(schedule)
                            }
                        )
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(dynamicCategories) { category ->
                                val isSelected = category == selectedCategoryTab
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .padding(vertical = 16.dp)
                                        .height(40.dp)
                                        .combinedClickable(
                                            onClick = {
                                                if (category == "すべて") viewModel.onSelectFilterCategory("") else viewModel.onSelectFilterCategory(category)
                                            },
                                            onLongClick = { if (category != "すべて") categoryToDelete = category }
                                        )
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                                        Text(text = category, fontSize = 16.sp)
                                    }
                                }
                            }
                            item {
                                Surface(
                                    onClick = { showAddCategoryDialog = true },
                                    modifier = Modifier.padding(vertical = 16.dp).size(40.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = "カテゴリーを追加", modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }

                        val searchedSchedules = uiState.scheduleList.filter {
                            uiState.searchQuery.isBlank() || it.text.contains(uiState.searchQuery, ignoreCase = true) || (it.detail?.contains(uiState.searchQuery, ignoreCase = true) ?: false)
                        }

                        val filteredSchedules = if (selectedFilterCategory.isBlank()) searchedSchedules else searchedSchedules.filter { it.category == selectedFilterCategory }
                        val uncompletedSchedules = filteredSchedules.filter { !it.isCompleted }
                        val completedSchedules = filteredSchedules.filter { it.isCompleted }

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
                                        pathEffect = PathEffect.dashPathEffect(intervals = floatArrayOf(12.dp.toPx(), 8.dp.toPx()), phase = 0f)
                                    )
                                    drawRoundRect(color = strokeColor, style = stroke, cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
                                },
                            decorationBox = { innerTextField ->
                                OutlinedTextFieldDefaults.DecorationBox(
                                    value = uiState.searchQuery,
                                    innerTextField = innerTextField,
                                    enabled = true,
                                    singleLine = true,
                                    visualTransformation = VisualTransformation.None,
                                    interactionSource = interactionSource,
                                    placeholder = { Text(text = "予定を検索", color = strokeColor.copy(alpha = 0.6f), fontSize = 16.sp) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    container = {}
                                )
                            }
                        )

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 0.dp)
                        ) {
                            if (filteredSchedules.isEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(R.string.no_item_description),
                                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            } else {
                                scheduleMainList(
                                    groupedUncompleted = groupedUncompleted,
                                    completedSchedules = completedSchedules,
                                    viewModel = viewModel,
                                    onEditSavedItem = { schedule ->
                                        selectedDate = schedule.date
                                        selectedTime = schedule.time
                                        selectedEndTime = schedule.endTime ?: ""
                                        viewModel.onEditSavedItem(schedule)
                                    },
                                    // 💡 追加：長押しされたら削除変数にセット
                                    onDeleteSavedItem = { schedule -> scheduleToDelete = schedule }
                                )

                                item { Spacer(modifier = Modifier.height(100.dp)) }
                            }
                        }
                    }
                }
            }

            if (uiState.showInputBox) {
                ScheduleInputDialog(
                    initialText = uiState.editingItem?.text ?: "",
                    initialDetail = uiState.editingItem?.detail ?: "",
                    initialReminderMinutes = uiState.editingItem?.reminderMinutes ?: 5,
                    onDismiss = { viewModel.onDismissInputBox() },
                    onSave = { text, date, time, endTime, category, detail, reminderMinutes ->
                        val item = uiState.editingItem
                        if (item != null) {
                            viewModel.updateItem(item, text, date, time, endTime, category, detail, reminderMinutes)
                        } else {
                            viewModel.addText(text, date, time, endTime, category, detail, reminderMinutes)
                        }
                        viewModel.onDismissInputBox()
                    },
                    onDelete = uiState.editingItem?.let { item -> { viewModel.deleteItem(item) } },
                    onSelectDate = { showDatePicker = true },
                    onSelectTime = { showTimePicker = true },
                    onSelectEndTime = { showEndTimePicker = true },
                    selectedDate = selectedDate,
                    selectedTime = selectedTime,
                    selectedEndTime = selectedEndTime,
                    selectedCategory = selectedEditCategory,
                    onSelectCategory = { viewModel.onSelectEditCategory(it) },
                    categories = dynamicCategories
                )
            }

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

            if (showEndTimePicker) {
                val timePickerState = rememberTimePickerState()
                AlertDialog(
                    onDismissRequest = { showEndTimePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            selectedEndTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                            showEndTimePicker = false
                        }) { Text(stringResource(R.string.enter)) }
                    },
                    dismissButton = { TextButton(onClick = { showEndTimePicker = false }) { Text(stringResource(R.string.cancel)) } },
                    text = { TimePicker(state = timePickerState) }
                )
            }
        }
    }

    // 💡 追加：タスク削除の確認ダイアログ
    scheduleToDelete?.let { schedule ->
        AlertDialog(
            onDismissRequest = { scheduleToDelete = null },
            title = { Text("予定の削除") },
            text = { Text("予定「${schedule.text}」を削除しますか？") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteItem(schedule)
                        scheduleToDelete = null // 削除したらダイアログを閉じる
                    }
                ) { Text("削除", color = MaterialTheme.colorScheme.onError) }
            },
            dismissButton = { TextButton(onClick = { scheduleToDelete = null }) { Text("キャンセル") } }
        )
    }

    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("新しいカテゴリーを追加") },
            text = { OutlinedTextField(value = newCategoryText, onValueChange = { newCategoryText = it }, label = { Text("カテゴリー名") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                Button(onClick = {
                    if (newCategoryText.isNotBlank()) {
                        viewModel.addCategory(newCategoryText)
                        newCategoryText = ""
                        showAddCategoryDialog = false
                    }
                }) { Text("追加") }
            },
            dismissButton = { TextButton(onClick = { showAddCategoryDialog = false }) { Text("キャンセル") } }
        )
    }

    categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("カテゴリーの削除") },
            text = { Text("カテゴリー「$category」を削除しますか？") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteCategory(category)
                        categoryToDelete = null
                    }
                ) { Text("削除", color = MaterialTheme.colorScheme.onError) }
            },
            dismissButton = { TextButton(onClick = { categoryToDelete = null }) { Text("キャンセル") } }
        )
    }
}

// 💡 onDeleteItem の引数を追加し、長押し（combinedClickable）に対応
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScheduleItemRow(
    schedule: Schedule,
    onEditItem: (Schedule) -> Unit,
    onDeleteItem: (Schedule) -> Unit, // 💡 追加
    viewModel: HomeViewModel
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val arrowRotationDegree by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "ArrowAnimation"
    )

    val currentTime = System.currentTimeMillis()
    val taskTimeMillis = convertDateTimeToMillis(schedule.date, schedule.endTime)
    val isOverdue = taskTimeMillis != null && taskTimeMillis < currentTime && !schedule.isCompleted

    val isDark = isSystemInDarkTheme()
    val borderColor = if (isOverdue) Color(0xFF94403E) else md_theme_light_primary
    val backgroundColor = if (isOverdue && isDark) MaterialTheme.colorScheme.surfaceVariant
    else if (isOverdue) Color(0xFFFFEBEE)
    else MaterialTheme.colorScheme.surfaceVariant

    fun formatTo12Hour(timeStr: String?): String {
        if (timeStr.isNullOrBlank()) return "未設定"
        val timeParts = timeStr.split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: return timeStr
        val amPmSystem = if (hour < 12) "午前" else "午後"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val minute = timeParts.getOrNull(1) ?: "00"
        return "$amPmSystem ${String.format("%02d", displayHour)}:$minute"
    }

    val displayStartTime = formatTo12Hour(schedule.time)
    val displayEndTime = formatTo12Hour(schedule.endTime)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
            .background(backgroundColor, RoundedCornerShape(8.dp))
            // 💡 clickable から combinedClickable に変更（長押し対応）
            .combinedClickable(
                onClick = { expanded = !expanded },
                onLongClick = { onDeleteItem(schedule) }
            )
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = schedule.isCompleted,
                onCheckedChange = { isChecked -> viewModel.toggleScheduleStatus(schedule, isChecked) },
                modifier = Modifier.padding(end = 8.dp)
            )

            Text(
                text = schedule.text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                style = androidx.compose.ui.text.TextStyle(textDecoration = if (schedule.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None)
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp)) {
                val timeColor = if (isSystemInDarkTheme()) md_theme_dark_time else md_theme_light_time
                Text(text = displayStartTime, fontSize = 14.sp, color = timeColor)
                Text(text = " ➔ ", fontSize = 12.sp, color = timeColor.copy(alpha = 0.6f))
                Text(text = " ${displayEndTime}", fontSize = 14.sp, color = timeColor)
            }

            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp).rotate(arrowRotationDegree))
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, start = 8.dp, end = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Divider(modifier = Modifier.padding(bottom = 6.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

                val displayDate_day = if (!schedule.date.isNullOrBlank()) "${schedule.date} " else "未設定"
                Text(text = "📅 日  付: $displayDate_day", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "⏰ 時  間: $displayStartTime ～ $displayEndTime", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                val reminderText = when (schedule.reminderMinutes) {
                    -1 -> "通知なし"
                    0 -> "時間ピッタリ"
                    else -> "${schedule.reminderMinutes}分前"
                }
                Text(text = "🔔 通  知: $reminderText", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                val displayDetail = if (!schedule.detail.isNullOrBlank()) schedule.detail else "なし"
                Text(text = "📝 メ  モ: $displayDetail", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val displayCategory = if (!schedule.category.isNullOrBlank()) schedule.category else "なし"
                Text(text = "🏷️ カテゴリ: $displayCategory", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onEditItem(schedule) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)) {
                        Text("編集する", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// 💡 onDeleteSavedItem の引数を追加
private fun LazyListScope.scheduleMainList(
    groupedUncompleted: Map<String, List<Schedule>>,
    completedSchedules: List<Schedule>,
    viewModel: HomeViewModel,
    onEditSavedItem: (Schedule) -> Unit,
    onDeleteSavedItem: (Schedule) -> Unit // 💡 追加
) {
    groupedUncompleted.forEach { (date, schedules) ->
        item { Text(text = "$date の予定", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)) }
        items(schedules) { schedule ->
            ScheduleItemRow(schedule = schedule, onEditItem = { onEditSavedItem(schedule) }, onDeleteItem = { onDeleteSavedItem(schedule) }, viewModel = viewModel)
        }
    }

    if (completedSchedules.isNotEmpty()) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
        items(completedSchedules) { schedule ->
            ScheduleItemRow(schedule = schedule, onEditItem = { onEditSavedItem(schedule) }, onDeleteItem = { onDeleteSavedItem(schedule) }, viewModel = viewModel)
        }
    }
}

@Composable
fun ViewToggleButton(currentMode: Int, onModeChange: (Int) -> Unit) {
    BottomAppBar(containerColor = MaterialTheme.colorScheme.surface) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(onClick = { onModeChange(0) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(4.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = if (currentMode == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, contentColor = if (currentMode == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)) { Text("リスト", fontSize = 14.sp) }
            Button(onClick = { onModeChange(1) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(4.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = if (currentMode == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, contentColor = if (currentMode == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)) { Text("カレンダー", fontSize = 14.sp) }
            Button(onClick = { onModeChange(2) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(4.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = if (currentMode == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, contentColor = if (currentMode == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)) { Text("タイムライン", fontSize = 14.sp) }
        }
    }
}
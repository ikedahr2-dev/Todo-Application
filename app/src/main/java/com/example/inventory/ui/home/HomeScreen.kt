package com.example.inventory.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.colorspace.WhitePoint

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
        //修正：リストの全件数（size）ではなく、まだ完了していない(isCompletedがfalseの)タスクだけを正しく数える
        val uncompletedCount = uiState.scheduleList.count { !it.isCompleted }
        updateOngoingTaskCountNotification(context, uncompletedCount)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var showCalendar by remember { mutableStateOf(false) }
    val selectedCategory = uiState.selectedCategory ?: ""
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
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {

                    var selectedCategoryTab by remember { mutableStateOf("すべて") }
                    val categories = listOf("すべて", "仕事", "プライベート", "その他")

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ){
                        items(categories) { category ->

                            FilterChip(
                                selected = (category == selectedCategoryTab),
                                onClick = { selectedCategoryTab = category },
                                label = {
                                    Text(
                                        text = category,
                                        fontSize = 16.sp
                                    )
                                },
                                modifier = Modifier
                                    .padding(vertical = 16.dp)
                                    .height(40.dp),
                                shape = CircleShape,   // 形
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }


                    val strokeColor = md_theme_light_primary
                    val interactionSource = remember { MutableInteractionSource() }

                    BasicTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        interactionSource = interactionSource,
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 0.dp)
                            // 点線枠を描画するカスタム Modifier
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
                                    horizontal = 12.dp, // 左右の隙間（好みに応じて 4.dp などに減らしてもOK）
                                    vertical = 6.dp     // 上下の隙間（ここを減らすと枠と文字がギュッと縮まります）
                                ),
                                container = {
                                    // デフォルトのコンテナ枠線を表示させないために何も描画しない
                                }
                            )
                        }
                    )

                    HomeBody(
                        scheduleList = uiState.scheduleList,
                        onEditItem = { schedule ->
                            selectedDate = schedule.date
                            selectedTime = schedule.time
                            viewModel.onEditSavedItem(schedule)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
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
                    selectedCategory = selectedCategory,
                    onSelectCategory = { viewModel.onSelectCategory(it) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeBody(
    scheduleList: List<Schedule>,
    onEditItem: (Schedule) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val groupedSchedules = scheduleList
        .sortedWith(compareBy<Schedule> { it.date }.thenBy { it.time })
        .groupBy { it.date }

    Column(
        modifier = modifier.fillMaxSize()
    ) {

        // ----- 予定閲覧 ----- //

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 0.dp)
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
                    item {
                        Text(
                            text = "$date の予定",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                    }

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

                                Text(text = schedule.time, fontSize = 20.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
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
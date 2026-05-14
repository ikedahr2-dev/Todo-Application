package com.example.inventory.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inventory.R
import com.example.inventory.data.Schedule // Schedule型を使用
import com.example.inventory.ui.AppViewModelProvider
import com.example.inventory.ui.navigation.NavigationDestination
import com.example.inventory.ui.theme.md_theme_light_primary
import java.text.SimpleDateFormat

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
    // 【修正】Factoryを渡してDBリポジトリを使えるようにする
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var showCalendar by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        bottomBar = {
            ViewToggleButton(
                onListClick = { showCalendar = false },
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
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.item_entry_title)
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (showCalendar) {
                CalendarScreen(
                    onDateSelected = { date ->
                        selectedDate = date
                        selectedTime = ""
                        viewModel.onAddClick()
                    }
                )
            } else {
                // 【修正】scheduleListのみを渡すように変更
                HomeBody(
                    scheduleList = uiState.scheduleList,
                    onItemClick = navigateToItemUpdate,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = innerPadding
                )
            }

            // 【修正】入力ダイアログ（editingItemもSchedule型になっている前提）
            if (uiState.showInputBox) {
                ScheduleInputDialog(
                    onDismiss = { viewModel.onDismissInputBox() },
                    onSave = { text, date, time ->
                        val editingItem = viewModel.editingItem.value
                        if (editingItem != null) {
                            viewModel.updateItem(editingItem, text, date, time)
                        } else {
                            viewModel.addText(text, date, time)
                        }
                        viewModel.onDismissInputBox()
                    },
                    onDelete = viewModel.editingItem.value?.let { item ->
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
                            val millis = datePickerState.selectedDateMillis
                            selectedDate = millis?.let {
                                SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())
                                    .format(java.util.Date(it))
                            } ?: ""
                            showDatePicker = false
                        }) {
                            Text(stringResource(R.string.enter))
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            // TimePicker
            if (showTimePicker) {
                val timePickerState = rememberTimePickerState(initialHour = 0, initialMinute = 0)
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            selectedTime = String.format(
                                "%02d:%02d",
                                timePickerState.hour,
                                timePickerState.minute
                            )
                            showTimePicker = false
                        }) {
                            Text(stringResource(R.string.enter))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                    text = {
                        Column {
                            TimePicker(state = timePickerState)
                        }
                    },
                    properties = DialogProperties(
                        dismissOnBackPress = false,
                        dismissOnClickOutside = false
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeBody(
    scheduleList: List<Schedule>, // 【重要】Schedule型のみに統合
    onItemClick: (Int) -> Unit,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var selectedCategory by remember { mutableStateOf("すべて") }
    val categories = listOf("すべて", "仕事", "プライベート", "その他")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(contentPadding),
    ) {
        // カテゴリナビゲーションバー
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                FilterChip(
                    selected = (category == selectedCategory),
                    onClick = { selectedCategory = category },
                    label = { Text(category) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = md_theme_light_primary,
                        selectedLabelColor = Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = md_theme_light_primary
                    )
                )
            }
        }

        // フィルタリング処理
        val filteredList = if (selectedCategory == "すべて") {
            scheduleList
        } else {
            scheduleList.filter { it.category == selectedCategory }
        }

        // スケジュール表示エリア
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            if (filteredList.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_item_description),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                )
            } else {
                // DBから取得した予定を表示
                filteredList.forEach { schedule ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = Color.Gray,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                            .clickable { viewModel.onEditSavedItem(schedule) }
                            .padding(12.dp) // 内側の余白
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "${schedule.date}                             ${schedule.time}\n${schedule.text}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.onEditSavedItem(schedule)
                                }
                                .padding(vertical = 4.dp),
                            textAlign = TextAlign.Start,
                            fontSize = 23.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ViewToggleButton(
    onListClick: () -> Unit,
    onCalendarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onListClick,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small
            ) {
                Text("リスト")
            }
            Button(
                onClick = onCalendarClick,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small
            ) {
                Text("カレンダー")
            }
        }
    }
}
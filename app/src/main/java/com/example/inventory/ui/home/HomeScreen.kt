package com.example.inventory.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inventory.R
import com.example.inventory.data.Item
import com.example.inventory.ui.navigation.NavigationDestination
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.graphics.Color
import com.example.inventory.ui.theme.md_theme_light_primary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimeInput

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
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") } //Calendarで選択した日付をここに保存

    var selectedCalendarDate by remember { mutableStateOf("") }

    var showCalendar by remember { mutableStateOf(false)} // カレンダー画面を表示するか

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
        },
        bottomBar = {
            ViewToggleButton(
                onListClick = { showCalendar = false },
                onCalendarClick = { showCalendar = true }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                FloatingActionButton(
                    onClick = { viewModel.onAddClick() },
                    shape = CircleShape,   // 丸
                    containerColor = Color.White,   // 中を白
                    contentColor = md_theme_light_primary,   // アイコン色
                    modifier = Modifier   // 位置・サイズ・枠
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
    ){ innerPadding ->

        Box(modifier = Modifier.fillMaxSize()) {

            if (showCalendar) {
                CalendarScreen(
                    onDateSelected = { date ->

                        //ここで日付受け取る
                        selectedDate = date

                        //入力ダイアログ開く
                        viewModel.onAddClick()
                    }
                )
            } else {
                HomeBody(
                    itemList = uiState.itemList,
                    onItemClick = navigateToItemUpdate,
                    savedItems = uiState.savedItems,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = innerPadding
                )
            }

            //入力ダイアログ
            if (uiState.showInputBox) {
                ScheduleInputDialog(
                    onDismiss = { viewModel.onDismissInputBox() },
                    onSave = { text, date, time ->
                        viewModel.addText(text, date, time)
                    },
                    onSelectDate = { showDatePicker = true },
                    onSelectTime = { showTimePicker = true },
                    selectedDate = selectedDate,
                    selectedTime = selectedTime
                )
            }

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            val millis = datePickerState.selectedDateMillis
                            selectedDate = millis?.let {
                                java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())
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

            if (showTimePicker) {
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
                            Text(stringResource(R.string.no_item_cancel))
                        }
                    },
                    text = {
                        Column {
                            TimePicker(state = timePickerState)
                        }
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeBody(
    itemList: List<Item>,
    onItemClick: (Int) -> Unit,
    savedItems: List<ScheduleItem>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {

    var selectedCategory by remember { mutableStateOf("すべて") }
    // この先増えるなら格納先作って呼び出し、何個かで固定なら直で書けばいいかなって感じ^-^
    val categories = listOf("すべて", "仕事", "プライベート", "その他")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(contentPadding), // 余白
    ) {

        // ----- ナビゲーションバー ----- //

        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxWidth()
        )
        {
            items(categories) { category ->
                FilterChip(
                    selected = (category == selectedCategory), // 選択
                    onClick = { selectedCategory = category },
                    label = { Text(category) },
                    shape = CircleShape,   // 形
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            savedItems.forEach { item ->
                Text(
                    text = "・${item.date} ${item.time} ${item.text}",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    fontSize = 16.sp
                )
            }
        }
        val filteredList = if (selectedCategory == "すべて") {
            itemList
        } else {
            itemList.filter { it.category == selectedCategory }
        }

        if (itemList.isEmpty() && savedItems.isEmpty()) {
            Text(
                text = stringResource(R.string.no_item_description),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(contentPadding),
            )
        } else {
            InventoryList(
                itemList = filteredList,
                onItemClick = { onItemClick(it.id) },
                contentPadding = contentPadding,
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.padding_small))
            )
        }
    }
}

@Composable
private fun InventoryList(
    itemList: List<Item>,
    onItemClick: (Item) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding
    ) {
        items(items = itemList, key = { it.id }) { item ->
            InventoryItem(item = item,
                modifier = Modifier
                    .padding(dimensionResource(id = R.dimen.padding_small))
                    .clickable { onItemClick(item) })
        }
    }
}

@Composable
private fun InventoryItem(
    item: Item, modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(id = R.dimen.padding_large)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_small))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.weight(1f))
            }
            Text(
                text = stringResource(R.string.in_stock, item.quantity),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
@Composable
fun ViewToggleButton(
    onListClick: () -> Unit,
    onCalendarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.Button(
                onClick = onListClick,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small
            ) {
                Text("リスト")
            }
            androidx.compose.material3.Button(
                onClick = onCalendarClick,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small
            ) {
                Text("カレンダー")
            }
        }
    }
}
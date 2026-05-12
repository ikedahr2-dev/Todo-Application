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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inventory.InventoryTopAppBar
import com.example.inventory.R
import com.example.inventory.data.Item
import com.example.inventory.ui.item.formatedPrice
import com.example.inventory.ui.navigation.NavigationDestination
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp


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
    //カレンダーの状態管理
    val datePickerState = rememberDatePickerState()
    var text by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
        },
        bottomBar = {
            ViewToggleButton(
                onListClick = { /* 現在地なので何もしない */ },
                onCalendarClick = { viewModel.onCalenderClick() }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                FloatingActionButton(
                    onClick = { viewModel.onAddClick() },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.offset(y = (-30).dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.item_entry_title)
                    )
                }
            }
        },
    ) { innerPadding ->

        if (uiState.showDatePicker) {
            DatePickerDialog(
                onDismissRequest = {
                    viewModel.onDismissDatePicker()
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.onDismissDatePicker()
                        }
                    ) {
                        Text(stringResource(R.string.no_item_OK))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.onDismissDatePicker()
                        }
                    ) {
                        Text(stringResource(R.string.no_item_cancel))
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        var text by remember { mutableStateOf("") }

        Box(modifier = Modifier.fillMaxSize()) {

            //保存したテキスト表示
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {

                HomeBody(
                    itemList = uiState.itemList,
                    onItemClick = navigateToItemUpdate,
                    savedTexts = uiState.savedTexts,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = innerPadding,
                )
            }

            //入力ボックス
            if (uiState.showInputBox) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember {
                                androidx.compose.foundation.interaction.MutableInteractionSource()
                            }
                        ) {
                            viewModel.onDismissInputBox()
                        }
                ) {

                    Column(
                        modifier = Modifier
                            .align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            label = { Text(stringResource(R.string.stay_schedule)) },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                        )

                        androidx.compose.material3.Button(
                            onClick = {
                                viewModel.addText(text)
                                text = ""
                                viewModel.onDismissInputBox()
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(stringResource(R.string.enter))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeBody(
    itemList: List<Item>,
    onItemClick: (Int) -> Unit,
    savedTexts: List<String>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
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
                    selected = (category == "すべて"), // すべてが選択された状態
                    onClick = {  },
                    label = { Text(category) },
                    shape = androidx.compose.foundation.shape.CircleShape,   // 形
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = androidx.compose.ui.graphics.Color(0xFF86AF91), 
                        selectedLabelColor = androidx.compose.ui.graphics.Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = androidx.compose.ui.graphics.Color(0xFF86AF91)
                    )
                )
            }
        }

        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            savedTexts.forEach { text ->
                Text(
                    text = "・$text",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    fontSize = 25.sp,
                    textAlign = TextAlign.Start
                )
            }
        }

        if (itemList.isEmpty() && savedTexts.isEmpty()) {
            Text(
                text = stringResource(R.string.no_item_description),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(contentPadding),
            )
        } else {
            InventoryList(
                itemList = itemList,
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
                Text(
                    text = item.formatedPrice(),
                    style = MaterialTheme.typography.titleMedium
                )
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
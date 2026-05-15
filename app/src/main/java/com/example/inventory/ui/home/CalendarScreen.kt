package com.example.inventory.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventory.R
import com.example.inventory.data.Schedule
import com.example.inventory.ui.theme.md_theme_light_primary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    scheduleList: List<Schedule>,
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    onCalendarItemClick: (Schedule) -> Unit,
    onAddClick: () -> Unit
) {
    val state = rememberDatePickerState()

    // ✨ ここがポイント！カレンダーの選択が変わるたびに HomeScreen へ報告する
    LaunchedEffect(state.selectedDateMillis) {
        state.selectedDateMillis?.let { millis ->
            val date = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                .format(Date(millis))
            onDateSelected(date) // 👈 これでタップした瞬間に HomeScreen の selectedDate が更新される！
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. カレンダー本体
        DatePicker(
            state = state,
            modifier = Modifier.fillMaxWidth()
        )

        // 2. 「yyyy/mm/dd」を表示するエリア
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween, // 👈 左右に振り分ける
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左側：選択中の日付を表示（ボタンじゃなくてただのテキスト）
            // CalendarScreen.kt のテキスト部分
            Text(
                text = selectedDate.ifEmpty { "日付を選択してください" }, // 未選択時のガイド
                fontSize = 24.sp, // 少しサイズ調整
                color = md_theme_light_primary,
                modifier = Modifier.padding(start = 8.dp)
            )

            Button(
                onClick = { onAddClick() },
                // ★ここが重要：selectedDate が空じゃない時だけボタンを有効にする
                enabled = selectedDate.isNotBlank(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("予定を入力")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. 予定リスト（LazyColumn）
        val dailySchedules = scheduleList.filter { it.date == selectedDate }
            .sortedBy { it.time }   //昇順

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(dailySchedules) { schedule ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp) // カード同士の隙間
                        .border(
                            width = 1.5.dp,
                            color = md_theme_light_primary,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .background(
                            Color.White,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            onCalendarItemClick(schedule)
                        }
                        .padding(12.dp) // カードの中の余白
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = schedule.text, fontSize = 28.sp)
                            Text(text = schedule.time, fontSize = 24.sp)
                        }
                    }
                }
            }
        }

    }
}
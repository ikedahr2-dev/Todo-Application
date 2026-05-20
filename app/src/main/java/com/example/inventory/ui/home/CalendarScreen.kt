package com.example.inventory.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventory.R
import com.example.inventory.data.Schedule
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    scheduleList: List<Schedule>,
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    onCalendarItemClick: (Schedule) -> Unit,
    onAddClick: () -> Unit
) {
    val state = rememberDatePickerState()

    // カレンダーの選択が変わるたびに HomeScreen へ報告する
    LaunchedEffect(state.selectedDateMillis) {
        state.selectedDateMillis?.let { millis ->
            val date = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                .format(Date(millis))
            onDateSelected(date)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. カレンダー本体
        DatePicker(
            state = state,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { category: String ->
                FilterChip(
                    selected = (category == selectedCategory),
                    onClick = { onCategorySelected(category) },
                    label = { Text(text = category, fontSize = 14.sp) }
                )
            }
        }

        // 2. 「yyyy/mm/dd」を表示するエリア
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedDate.ifEmpty { "日付を選択" },
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )

            Button(
                onClick = { onAddClick() },
                enabled = selectedDate.isNotBlank(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("予定を入力")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. 予定リスト（LazyColumn）
        val dailySchedules = scheduleList
            .filter { it.date == selectedDate }
            .filter { selectedCategory == "すべて" || it.category == selectedCategory }
            .sortedBy { it.time }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(dailySchedules, key = { it.id }) { schedule ->

                // アイテムごとの開閉状態管理（スクロール時も状態保持）
                var expanded by rememberSaveable(key = schedule.id.toString()) { mutableStateOf(false) }

                // 矢印回転アニメーション
                val arrowRotationDegree by animateFloatAsState(
                    targetValue = if (expanded) 180f else 0f,
                    label = "ArrowAnimation"
                )

                val displayFormattedTime = if (!schedule.time.isNullOrBlank()) {
                    val timeParts = schedule.time.split(":")
                    val hour = timeParts.getOrNull(0)?.toIntOrNull()
                    if (hour != null) {
                        val amPmSystem = if (hour < 12) "午前" else "午後"
                        val displayHour = when {
                            hour == 0 -> 12
                            hour > 12 -> hour - 12
                            else -> hour
                        }
                        val minute = timeParts.getOrNull(1) ?: "00"
                        "$amPmSystem ${String.format("%02d", displayHour)}:$minute"
                    } else {
                        schedule.time
                    }
                } else {
                    "未設定"
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .background(
                            Color.White,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { expanded = !expanded } // タップでアコーディオン開閉
                        .padding(12.dp)
                ) {
                    // 1行目：タイトル、時間、矢印アイコン
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = schedule.text,
                            fontSize = 28.sp,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = displayFormattedTime,
                            fontSize = 24.sp,
                            modifier = Modifier.padding(end = 4.dp)
                        )

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (expanded) "閉じる" else "詳細を開く",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(28.dp)
                                .rotate(arrowRotationDegree)
                        )
                    }

                    // 2行目以降：詳細アコーディオンエリア
                    AnimatedVisibility(visible = expanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, start = 4.dp, end = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 区切り線
                            Divider(
                                modifier = Modifier.padding(bottom = 6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )

                            // 📅 日付の日本語変換
                            val formattedDate = if (!schedule.date.isNullOrBlank()) {
                                val parts = schedule.date.split("/")
                                if (parts.size == 3) {
                                    "${parts[0]}年${parts[1]}月${parts[2]}日"
                                } else {
                                    schedule.date
                                }
                            } else {
                                "未設定"
                            }
                            Text(text = "📅 日付け: $formattedDate", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            //時間
                            Text(text = "⏰ 時間: $displayFormattedTime", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            //メモ（詳細）
                            val displayDetail = if (!schedule.detail.isNullOrBlank()) schedule.detail else "詳細テキストはありません。"
                            Text(text = "📝 メモ: $displayDetail", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            //タグ
                            val displayCategory = if (!schedule.category.isNullOrBlank()) schedule.category else "なし"
                            Text(text = "🏷️ タグ: $displayCategory", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(8.dp))

                            // 右下の「編集する」ボタン
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { onCalendarItemClick(schedule) }, // 元々のタップ処理をここに移譲
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("編集する", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
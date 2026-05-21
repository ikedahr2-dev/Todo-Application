package com.example.inventory.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape

// カレンダー表示とスケジュール確認を行う画面構成
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
    onAddClick: () -> Unit,
    viewModel: HomeViewModel // タスクの完了状態（チェックボックス）を更新するために使用
) {
    // カレンダーの選択状態を管理するオブジェクト
    val state = rememberDatePickerState()

    // カレンダーの日付がタップされてミリ秒（millis）が変わったときに動く処理
    LaunchedEffect(state.selectedDateMillis) {
        state.selectedDateMillis?.let { millis ->
            // ミリ秒を "yyyy/MM/dd" 形式の文字列に変換して親画面に通知
            val date = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                .format(Date(millis))
            onDateSelected(date)
        }
    }

    // 24時間表記（例: "13:00"）を「午後 01:00」のような12時間表記に変換する関数
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

    // 画面全体の縦並びレイアウト
    Column(modifier = Modifier.fillMaxSize()) {
        // Material3標準のスクロール可能なカレンダー表示
        DatePicker(
            state = state,
            modifier = Modifier.fillMaxWidth()
        )

        // カテゴリ切り替え用の横スクロールチップ一覧
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),             horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { category: String ->
                val isSelected = (category == selectedCategory)

                Surface(
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .height(40.dp)
                        .clickable { onCategorySelected(category) }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(text = category, fontSize = 16.sp)
                    }
                }
            }
        }

        // 選択された日付のテキストと「予定を入力」ボタンの並び
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

            // 日付が選ばれているとき（空じゃないとき）だけ押せる新規入力ボタン
            Button(
                onClick = { onAddClick() },
                enabled = selectedDate.isNotBlank(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("予定を入力")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 全データから「選ばれた日付」かつ「選ばれたカテゴリ」に一致するものを抽出して時間順に並び替え
        val dailySchedules = scheduleList
            .filter { it.date == selectedDate }
            .filter { selectedCategory == "すべて" || it.category == selectedCategory }
            .sortedBy { it.time }

        // 抽出されたスケジュールカードを縦に並べるリスト領域
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(dailySchedules, key = { it.id }) { schedule ->
                // 各カードごとに詳細エリア（アコーディオン）の開閉状態を保持
                var expanded by rememberSaveable(key = schedule.id.toString()) { mutableStateOf(false) }

                // 開閉矢印がぬるっと回転するアニメーションの設定
                val arrowRotationDegree by animateFloatAsState(
                    targetValue = if (expanded) 180f else 0f,
                    label = "ArrowAnimation"
                )

                // 開始・終了時刻をそれぞれ12時間表記に変換
                val displayStartTime = formatTo12Hour(schedule.time)
                val displayEndTime = formatTo12Hour(schedule.endTime)

                // スケジュールカードの外枠と背景の定義
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .clickable { expanded = !expanded } // タップでアコーディオン開閉
                        .padding(12.dp)
                ) {
                    // 1行目：チェックボックス、タイトル、時間帯、矢印を横一列に配置
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // スケジュールの完了/未完了を切り替えるチェックボックス
                        Checkbox(
                            checked = schedule.isCompleted,
                            onCheckedChange = { isChecked -> viewModel.toggleScheduleStatus(schedule, isChecked) },
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // スケジュールのタイトル（完了時は文字の真ん中に打ち消し線を追加）
                        Text(
                            text = schedule.text,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                            style = androidx.compose.ui.text.TextStyle(
                                textDecoration = if (schedule.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
                            )
                        )

                        // 予定の「開始時間 ➔ 終了時間」を横並びで表示する領域
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Text(text = displayStartTime, fontSize = 14.sp, color = Color.Gray)
                            Text(text = " ➔ ", fontSize = 12.sp, color = Color.Gray.copy(alpha = 0.6f))
                            Text(text = displayEndTime, fontSize = 14.sp, color = Color.Gray)
                        }

                        // 開閉状態に合わせて回転する下向き矢印アイコン
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (expanded) "閉じる" else "メモを開く",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(26.dp)
                                .rotate(arrowRotationDegree)
                        )
                    }

                    // タップした時だけぬるっと広がる詳細エリア（アコーディオン）
                    AnimatedVisibility(visible = expanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, start = 4.dp, end = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // タイトル行と詳細情報のエリアを隔てる細い区切り線
                            Divider(
                                modifier = Modifier.padding(bottom = 6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )

                            // 日本語形式（〇〇年〇月〇日）に整形して日付を表示
                            val formattedDate = if (!schedule.date.isNullOrBlank()) {
                                val parts = schedule.date.split("/")
                                if (parts.size == 3) "${parts[0]}年${parts[1]}月${parts[2]}日" else schedule.date
                            } else {
                                "未設定"
                            }
                            Text(text = "📅 日　　付: $formattedDate", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            // 時間の範囲表示
                            Text(text = "⏰ 時　　間: $displayStartTime ～ $displayEndTime", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            // メモ内容の表示
                            val displayDetail = if (!schedule.detail.isNullOrBlank()) schedule.detail else "なし"
                            Text(text = "📝 メ　　モ: $displayDetail", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            // カテゴリ名の表示
                            val displayCategory = if (!schedule.category.isNullOrBlank()) schedule.category else "なし"
                            Text(text = "🏷️ カテゴリ: $displayCategory", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(8.dp))

                            // 「編集する」ボタンを右端に寄せて配置
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(
                                    onClick = { onCalendarItemClick(schedule) }, // タップでHomeScreen側の編集ポップアップ要求へ飛ばす
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
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
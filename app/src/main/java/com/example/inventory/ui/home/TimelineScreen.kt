package com.example.inventory.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventory.convertDateTimeToMillis
import com.example.inventory.data.Schedule
import com.example.inventory.ui.theme.md_theme_dark_time
import com.example.inventory.ui.theme.md_theme_light_primary
import com.example.inventory.ui.theme.md_theme_light_time
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// 【時間軸表示画面】1日のスケジュールを24時間の縦型タイムライン形式で一覧表示するコンポーザブル
@Composable//
fun TimelineScreen(
    scheduleList: List<Schedule>,
    onTimelineItemClick: (Schedule) -> Unit,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    // 今日の日付を "yyyy/MM/dd" 形式で取得
    val todayDate = remember {
        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
    }

    // 今日の予定だけを抽出し、時間順に並び替え
    val todaysSchedules = remember(scheduleList) {
        scheduleList.filter { it.date == todayDate }.sortedBy { it.time }
    }

    // 00:00 から 23:00 までの24時間分の時間枠のリストを作成
    val timeSlots = remember { (0..23).map { String.format("%02d:00", it) } }

    // 主コンテナの構築
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // 上部に「今日の日付（タイムライン）」の見出しを表示
        Text(
            text = "$todayDate のタイムライン",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // 24時間分のスクロールリスト（縦軸のタイムラインをレンダリング）
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(timeSlots) { slotTime ->
                // この時間枠（例: 09:00 〜 09:59）に開始する予定をフィルタリング
                val slotHour = slotTime.split(":")[0]
                val matchSchedules = todaysSchedules.filter {
                    it.time.startsWith("$slotHour:")
                }

                // タイムラインの1行分（左に時間、右に予定カード）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min) // 左右のセルの高さを連動させる（線を引き伸ばすために必須）
                ) {
                    // 左側：時間表示エリア
                    Column(
                        modifier = Modifier
                            .width(60.dp)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = slotTime,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.padding(end = 8.dp, top = 4.dp)
                        )
                    }

                    // 中央：タイムラインの縦線とドットを描画する領域
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        // 24時間を繋ぐ縦線
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        )
                        // 予定がある時間枠には目印の丸（インジケータードット）を表示
                        if (matchSchedules.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .size(10.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        }
                    }

                    // 右側：予定カード表示エリア
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (matchSchedules.isEmpty()) {
                            // 予定がない時間は、高さを確保するための空スペースを置く（等間隔な時間枠の表現）
                            Spacer(modifier = Modifier.height(40.dp))
                        } else {
                            // その時間帯にある予定をすべてカードとして描画
                            matchSchedules.forEach { schedule ->
                                TimelineItemCard(
                                    schedule = schedule,
                                    onEditItem = { onTimelineItemClick(schedule) },
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 【タイムライン専用カード】既存のScheduleItemRowの仕様・デザインを完全流用
@Composable
private fun TimelineItemCard(
    schedule: Schedule,
    onEditItem: (Schedule) -> Unit,
    viewModel: HomeViewModel
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    // 詳細展開矢印の回転アニメーション設定
    val arrowRotationDegree by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "ArrowAnimation"
    )

    // 期限切れ（過去の時刻）かつ 未完了かどうかの判定を行うロジック
    val currentTime = System.currentTimeMillis()
    val taskTimeMillis = convertDateTimeToMillis(schedule.date, schedule.time)
    val isOverdue = taskTimeMillis != null && taskTimeMillis < currentTime && !schedule.isCompleted

    // テーマ設定と状態（期限切れかどうか）に基づいた背景・枠線の色決定
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isOverdue) Color(0xFF94403E) else md_theme_light_primary
    val backgroundColor = if (isOverdue && isDark) MaterialTheme.colorScheme.surfaceVariant
    else if (isOverdue) Color(0xFFFFEBEE)
    else MaterialTheme.colorScheme.surfaceVariant

    // 午前/午後 表記への変換ロジック
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

    // 各スケジュール項目カードの外枠レイアウト
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded } // タップで開閉
            .padding(12.dp)
    ) {
        // カードの1行目：チェックボックス、タスク名、時刻、展開矢印
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = schedule.isCompleted,
                onCheckedChange = { isChecked ->
                    viewModel.toggleScheduleStatus(schedule, isChecked) // チェック切り替え処理
                },
                modifier = Modifier.padding(end = 8.dp)
            )

            Text(
                text = schedule.text,
                fontSize = 20.sp, // タイムライン用に少しだけコンパクトに調整
                modifier = Modifier.weight(1f),
                style = androidx.compose.ui.text.TextStyle(
                    textDecoration = if (schedule.isCompleted) {
                        androidx.compose.ui.text.style.TextDecoration.LineThrough // 完了時に打ち消し線
                    } else {
                        androidx.compose.ui.text.style.TextDecoration.None
                    }
                )
            )

            Text(
                text = displayFormattedTime,
                fontSize = 16.sp,
                color = if (isSystemInDarkTheme()) md_theme_dark_time else md_theme_light_time,
                modifier = Modifier.padding(end = 4.dp)
            )

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "閉じる" else "詳細を開く",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(arrowRotationDegree)
            )
        }

        // 2行目以降：タップ時に展開される詳細情報エリア
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 8.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 区切り線の描画
                Divider(
                    modifier = Modifier.padding(bottom = 6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )

                val displayDate_day = if (!schedule.date.isNullOrBlank()) "${schedule.date} " else "未設定"
                Text(text = "📅 日　　付: $displayDate_day", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "⏰ 時　　間: $displayFormattedTime", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                val displayDetail = if (!schedule.detail.isNullOrBlank()) schedule.detail else ""
                Text(text = "📝 メ  　  モ: $displayDetail", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                val displayCategory = if (!schedule.category.isNullOrBlank()) schedule.category else "なし"
                Text(text = "🏷️ カテゴリ: $displayCategory", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(8.dp))

                // 「編集する」ボタンの配置（タップでダイアログを呼び出す）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { onEditItem(schedule) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("編集する", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
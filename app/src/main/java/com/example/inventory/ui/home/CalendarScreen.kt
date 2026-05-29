package com.example.inventory.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventory.data.Schedule
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import com.example.inventory.convertDateTimeToMillis
import com.example.inventory.ui.theme.md_theme_light_primary
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    viewModel: HomeViewModel
) {
    // カレンダー画面用の長押し削除、完了確認の状態変数（既存のまま）
    var scheduleToDelete by remember { mutableStateOf<Schedule?>(null) }
    var schedulePendingCheck by remember { mutableStateOf<CheckConfirmationState?>(null) }

    // 💡 初期日付が空なら今日の予定を自動選択
    LaunchedEffect(Unit) {
        if (selectedDate.isBlank()) {
            val today = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
            onDateSelected(today)
        }
    }

    // 💡 今日を中心とした前後2週間（合計29日間）の日付リストを動的に生成
    val dateList = remember {
        val list = mutableListOf<Date>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -14) // 14日前から開始
        for (i in 0 until 29) {
            list.add(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val weekScrollState = rememberScrollState()
    val dayOfWeekFormatter = SimpleDateFormat("E", Locale.JAPANESE) // "月", "火"
    val dayFormatter = SimpleDateFormat("d", Locale.getDefault())     // "28", "29"
    val fullDateFormatter = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

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

    Column(modifier = Modifier.fillMaxSize()) {

        // 💡 省スペースな横スクロールの1週間バー
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(weekScrollState)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dateList.forEach { date ->
                val dateString = fullDateFormatter.format(date)
                val isSelected = dateString == selectedDate

                Column(
                    modifier = Modifier
                        .width(50.dp)
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onDateSelected(dateString) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayOfWeekFormatter.format(date),
                        fontSize = 12.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dayFormatter.format(date),
                        fontSize = 16.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ----- カテゴリー一覧（既存のまま） -----
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { category: String ->
                val isSelected = (category == selectedCategory)
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.height(40.dp).clickable { onCategorySelected(category) }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(text = category, fontSize = 16.sp)
                    }
                }
            }
        }

        // ----- 日付表示＆入力ボタン（既存のまま） -----
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = selectedDate.ifEmpty { "日付を選択" }, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
            Button(onClick = { onAddClick() }, enabled = selectedDate.isNotBlank(), shape = RoundedCornerShape(8.dp)) {
                Text("予定を入力")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ----- スケジュールリストエリア（既存のまま） -----
        val dailySchedules = scheduleList
            .filter { it.date == selectedDate }
            .filter { selectedCategory == "すべて" || it.category == selectedCategory }
            .sortedBy { it.time }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(dailySchedules, key = { it.id }) { schedule ->
                var expanded by rememberSaveable(key = schedule.id.toString()) { mutableStateOf(false) }
                val arrowRotationDegree by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "ArrowAnimation")

                val currentTime = System.currentTimeMillis()
                val startTimeMillis = convertDateTimeToMillis(schedule.date, schedule.time)
                val taskEndTimeMillis = convertDateTimeToMillis(schedule.date, schedule.endTime)

                val fiveMinutesMillis = 5 * 60 * 1000
                val isWithinFiveMinutesBeforeStart = startTimeMillis != null &&
                        currentTime < startTimeMillis &&
                        currentTime >= (startTimeMillis - fiveMinutesMillis)

                val isTooEarlyForManualCheck = startTimeMillis != null && currentTime < startTimeMillis && !isWithinFiveMinutesBeforeStart
                val isOverdue = taskEndTimeMillis != null && taskEndTimeMillis < currentTime && !schedule.isEndCompleted

                val isPastEndTime = taskEndTimeMillis != null && currentTime >= taskEndTimeMillis
                val isPastStartTime = startTimeMillis != null && currentTime >= startTimeMillis

                val isNotificationPendingCompleted = schedule.reminderMinutes == 9999 && isPastStartTime
                val isVisualCompleted = schedule.isCompleted || isNotificationPendingCompleted
                val isEndVisualCompleted = isPastEndTime || schedule.isEndCompleted

                val shouldShowStrikeThrough = isEndVisualCompleted

                val isDark = isSystemInDarkTheme()
                val borderColor = if (isOverdue) Color(0xFF94403E) else md_theme_light_primary
                val backgroundColor = if (isOverdue && isDark) MaterialTheme.colorScheme.surfaceVariant
                else if (isOverdue) Color(0xFFFFEBEE)
                else MaterialTheme.colorScheme.surfaceVariant

                val displayStartTime = formatTo12Hour(schedule.time)
                val displayEndTime = formatTo12Hour(schedule.endTime)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(BorderStroke(1.5.dp, borderColor), RoundedCornerShape(8.dp))
                        .background(backgroundColor, RoundedCornerShape(8.dp))
                        .combinedClickable(
                            onClick = { expanded = !expanded },
                            onLongClick = { scheduleToDelete = schedule }
                        )
                        .padding(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isVisualCompleted,
                            onCheckedChange = { isChecked ->
                                if (isChecked && isTooEarlyForManualCheck && !schedule.isCompleted) {
                                    schedulePendingCheck = CheckConfirmationState(schedule, isChecked, isEndTimeTarget = false)
                                } else {
                                    viewModel.toggleScheduleStatus(schedule, isChecked)
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))

                        Checkbox(
                            checked = schedule.isEndCompleted,
                            onCheckedChange = { isChecked ->
                                if (isChecked && !isPastEndTime) {
                                    schedulePendingCheck = CheckConfirmationState(schedule, isChecked, isEndTimeTarget = true)
                                } else {
                                    viewModel.toggleScheduleEndStatus(schedule, isChecked)
                                }
                            },
                            modifier = Modifier.size(28.dp),
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.secondary,
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = schedule.text,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                            style = androidx.compose.ui.text.TextStyle(
                                textDecoration = if (shouldShowStrikeThrough) {
                                    androidx.compose.ui.text.style.TextDecoration.LineThrough
                                } else {
                                    androidx.compose.ui.text.style.TextDecoration.None
                                }
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp)) {
                            Text(text = displayStartTime, fontSize = 14.sp, color = Color.Gray)
                            Text(text = " ➔ ", fontSize = 12.sp, color = Color.Gray.copy(alpha = 0.6f))
                            Text(text = " ${displayEndTime}", fontSize = 14.sp, color = Color.Gray)
                        }
                        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp).rotate(arrowRotationDegree))
                    }

                    AnimatedVisibility(visible = expanded) {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, start = 4.dp, end = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Divider(modifier = Modifier.padding(bottom = 6.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            val formattedDate = if (!schedule.date.isNullOrBlank()) {
                                val parts = schedule.date.split("/")
                                if (parts.size == 3) "${parts[0]}年${parts[1]}月${parts[2]}日" else schedule.date
                            } else { "未設定" }
                            Text(text = "📅 日  付: $formattedDate", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "⏰ 時  間: $displayStartTime ～ $displayEndTime", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            val reminderText = when (schedule.reminderMinutes) {
                                -1 -> "通知なし"
                                0 -> "時間ピッタリ"
                                9999 -> "通知で完了済み（開始待機）"
                                else -> "${schedule.reminderMinutes}分前"
                            }
                            Text(text = "🔔 通  知: $reminderText", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            val displayDetail = if (!schedule.detail.isNullOrBlank()) schedule.detail else "なし"
                            Text(text = "📝 メ  モ: $displayDetail", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val displayCategory = if (!schedule.category.isNullOrBlank()) schedule.category else "なし"
                            Text(text = "🏷️ カテゴリ: $displayCategory", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { onCalendarItemClick(schedule) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)) {
                                    Text("編集する", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 早期完了確認ダイアログ
    schedulePendingCheck?.let { config ->
        val titleText = if (config.isEndTimeTarget) "予定終了前の完了確認" else "予定開始前の完了確認"
        val bodyText = if (config.isEndTimeTarget) {
            "「${config.schedule.text}」は終了時間前ですが、完了にしてもよろしいですか？"
        } else {
            "「${config.schedule.text}」は開始時間前ですが、完了にしてもよろしいですか？"
        }

        AlertDialog(
            onDismissRequest = { schedulePendingCheck = null },
            title = { Text(titleText) },
            text = {
                Column {
                    Text(bodyText)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "⚠️1度チェックをつけると取り消しできません",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (config.isEndTimeTarget) {
                            viewModel.toggleScheduleEndStatus(config.schedule, config.isChecked)
                        } else {
                            viewModel.toggleScheduleStatus(config.schedule, config.isChecked)
                        }
                        schedulePendingCheck = null
                    }
                ) { Text("はい") }
            },
            dismissButton = {
                TextButton(onClick = { schedulePendingCheck = null }) { Text("キャンセル") }
            }
        )
    }

    // 💡 修正点：Material 3 の正しいカラーパレット形式（.colorScheme.error）に直しました！
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
                        scheduleToDelete = null
                    }
                ) { Text("削除", color = MaterialTheme.colorScheme.onError) }
            },
            dismissButton = { TextButton(onClick = { scheduleToDelete = null }) { Text("キャンセル") } }
        )
    }
}
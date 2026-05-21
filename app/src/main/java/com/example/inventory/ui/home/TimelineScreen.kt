package com.example.inventory.ui.home

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventory.convertDateTimeToMillis
import com.example.inventory.data.Schedule
import com.example.inventory.ui.theme.md_theme_dark_time
import com.example.inventory.ui.theme.md_theme_light_primary
import com.example.inventory.ui.theme.md_theme_light_time
import java.util.Calendar

@Composable
fun TimelineScreen(
    scheduleList: List<Schedule>,
    selectedDate: String,
    onDateChange: (String) -> Unit,
    onTimelineItemClick: (Schedule) -> Unit,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todaysSchedules = remember(scheduleList, selectedDate) {
        scheduleList.filter { it.date == selectedDate }
    }
    val todaysIncompleteTasks = remember(todaysSchedules) {
        todaysSchedules.filter { !it.isCompleted }
    }
    val totalCount = todaysSchedules.size
    val completedCount = todaysSchedules.count { it.isCompleted }
    val scrollState = rememberScrollState()

    // 予定がないときの、標準の5分マスのコンパクトな横幅
    val defaultStepWidth = 15.dp

    // 全288個の5分マスの幅を個別に動的決定するマップ
    val dynamicFiveMinuteWidths = remember(todaysSchedules, scheduleList) {
        val widthMap = IntArray(288) { defaultStepWidth.value.toInt() }

        // 1. 各5分マスのそれぞれの地点において、重なっている予定の「最大文字数」を事前集計する
        val maxCharCounts = IntArray(288) { 0 }
        todaysSchedules.forEach { schedule ->
            try {
                val startParts = schedule.time.split(":")
                val startH = startParts.getOrNull(0)?.toIntOrNull() ?: 0
                val startM = startParts.getOrNull(1)?.toIntOrNull() ?: 0
                val startBlockIndex = (startH * 12) + (startM / 5)

                val endParts = schedule.endTime.split(":")
                val endH = endParts.getOrNull(0)?.toIntOrNull() ?: startH
                val endM = endParts.getOrNull(1)?.toIntOrNull() ?: (startM + 5)
                val endBlockIndex = ((endH * 12) + (endM / 5)).coerceAtMost(288)

                for (i in startBlockIndex until endBlockIndex) {
                    if (i in 0..287) {
                        maxCharCounts[i] = maxCharCounts[i].coerceAtLeast(schedule.text.length)
                    }
                }
            } catch (e: Exception) {}
        }

        // 2. 集計した「最大文字数」を基準に、10分なら2分割、15分なら3分割して各マスを均等に引き延ばす
        todaysSchedules.forEach { schedule ->
            try {
                val startParts = schedule.time.split(":")
                val startH = startParts.getOrNull(0)?.toIntOrNull() ?: 0
                val startM = startParts.getOrNull(1)?.toIntOrNull() ?: 0
                val startBlockIndex = (startH * 12) + (startM / 5)

                val endParts = schedule.endTime.split(":")
                val endH = endParts.getOrNull(0)?.toIntOrNull() ?: startH
                val endM = endParts.getOrNull(1)?.toIntOrNull() ?: (startM + 5)
                val endBlockIndex = ((endH * 12) + (endM / 5)).coerceAtMost(288)

                val blockCount = endBlockIndex - startBlockIndex

                if (blockCount > 0) {
                    var maxCharsInThisRange = 0
                    for (i in startBlockIndex until endBlockIndex) {
                        if (i in 0..287) {
                            maxCharsInThisRange = maxCharsInThisRange.coerceAtLeast(maxCharCounts[i])
                        }
                    }

                    val requiredTotalWidth = (maxCharsInThisRange * 18) + 110
                    val originalTotalWidth = defaultStepWidth.value.toInt() * blockCount
                    val delta = requiredTotalWidth - originalTotalWidth

                    val extraWidthPerBlock = if (delta > 0) delta / blockCount else 0
                    val finalStepWidth = defaultStepWidth.value.toInt() + extraWidthPerBlock

                    for (i in startBlockIndex until endBlockIndex) {
                        if (i in 0..287) {
                            widthMap[i] = widthMap[i].coerceAtLeast(finalStepWidth)
                        }
                    }
                }
            } catch (e: Exception) {
                // パース失敗時はデフォルト維持
            }
        }
        widthMap
    }

    // 任意の時間位置までの正確な横オフセット距離（dp）を算出する関数
    fun getOffsetUpToBlock(targetTime: Float): Dp {
        val totalMinutes = (targetTime * 60f).toInt()
        val targetBlockCount = totalMinutes / 5
        val remainderMinutes = totalMinutes % 5

        var accumulatedDp = 0.dp
        for (i in 0 until targetBlockCount) {
            if (i in 0..287) {
                accumulatedDp += dynamicFiveMinuteWidths[i].dp
            }
        }
        if (targetBlockCount in 0..287) {
            val currentBlockWidth = dynamicFiveMinuteWidths[targetBlockCount].dp
            accumulatedDp += currentBlockWidth * (remainderMinutes / 5f)
        }
        return accumulatedDp
    }

    // 各1時間の総横幅を算出
    val hourWidths = remember(dynamicFiveMinuteWidths) {
        IntArray(24) { hour ->
            var sum = 0
            for (m in 0 until 12) {
                sum += dynamicFiveMinuteWidths[(hour * 12) + m]
            }
            sum
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // 上部エリア
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "$selectedDate のタイムライン", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            IconButton(
                onClick = {
                    val calendar = Calendar.getInstance()
                    try {
                        val dateParts = selectedDate.split("/")
                        calendar.set(dateParts[0].toInt(), dateParts[1].toInt() - 1, dateParts[2].toInt())
                    } catch (e: Exception) {}
                    DatePickerDialog(context, { _, y, m, d -> onDateChange(String.format("%04d/%02d/%02d", y, m + 1, d)) }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                }
            ) {
                Icon(imageVector = Icons.Default.DateRange, contentDescription = "日付を変更", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // 横スクロールタイムラインエリア
        Box(
            modifier = Modifier.fillMaxWidth().weight(4f).horizontalScroll(scrollState)
        ) {
            // ベースのタイムライン横線
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).padding(top = 35.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)))

            // 背景グリッドと目盛り文字の位置ズレを完全に防ぐ一体型コンテナ構造
            Row(modifier = Modifier.fillMaxHeight()) {
                (0..23).forEach { hour ->
                    val currentHourWidth = hourWidths[hour].dp

                    Box(
                        modifier = Modifier
                            .width(currentHourWidth)
                            .fillMaxHeight()
                    ) {
                        // 1. 背景の5分マスグリッド線
                        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Start) {
                            for (i in 0 until 12) {
                                val blockIndex = (hour * 12) + i
                                val stepWidth = dynamicFiveMinuteWidths[blockIndex].dp
                                val isExpanded = stepWidth > (defaultStepWidth + 2.dp)

                                Box(
                                    modifier = Modifier
                                        .width(stepWidth)
                                        .fillMaxHeight()
                                        .border(
                                            width = if (i == 11) 1.dp else 0.4.dp,
                                            color = MaterialTheme.colorScheme.primary.copy(
                                                alpha = if (isExpanded) 0.3f else if (i == 11) 0.2f else 0.06f
                                            )
                                        )
                                )
                            }
                        }

                        // 2. メイン目盛り（XX:00）
                        Box(
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Column(
                                modifier = Modifier.wrapContentWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = String.format("%02d:00", hour),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.offset(x = 0.dp)
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Box(modifier = Modifier.size(7.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape))
                            }
                        }

                        // 3. 途中から広がる5分枠用の補助時間（XX:XX）の配置
                        // 💡 【修正点】すべての5分を出すのではなく、登録されている予定の開始時間と終了時間にだけ文字を絞り込む
                        var currentAccumulatedWidth = 0.dp
                        for (m in 0 until 12) {
                            val blockIndex = (hour * 12) + m
                            val stepWidth = dynamicFiveMinuteWidths[blockIndex].dp
                            val minuteValue = m * 5
                            val timeString = String.format("%02d:%02d", hour, minuteValue)

                            // 💡 いずれかの予定の「開始時刻」または「終了時刻」と完全一致するか判定
                            val isMatchTime = todaysSchedules.any { it.time == timeString || it.endTime == timeString }

                            if (isMatchTime && minuteValue != 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .offset(x = currentAccumulatedWidth)
                                ) {
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text(
                                            text = timeString,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                        Spacer(modifier = Modifier.height(19.dp))
                                        Box(modifier = Modifier.size(5.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                    }
                                }
                            }
                            currentAccumulatedWidth += stepWidth
                        }
                    }
                }

                // 最終24:00の線用目盛り
                Box(modifier = Modifier.width(60.dp), contentAlignment = Alignment.TopStart) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(text = "24:00", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(18.dp))
                        Box(modifier = Modifier.size(7.dp).background(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), shape = CircleShape))
                    }
                }
            }

            // 予定カードレイアウト領域
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(top = 55.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(todaysSchedules) { schedule ->
                        val startParts = schedule.time.split(":")
                        val startHour = startParts.getOrNull(0)?.toFloatOrNull() ?: 0f
                        val startMinute = startParts.getOrNull(1)?.toFloatOrNull() ?: 0f
                        val startPosition = startHour + (startMinute / 60f)

                        val endParts = schedule.endTime.split(":")
                        val endHour = endParts.getOrNull(0)?.toIntOrNull() ?: startHour.toInt()
                        val endMinute = endParts.getOrNull(1)?.toFloatOrNull() ?: 0f
                        val endPosition = endHour + (endMinute / 60f)

                        val leftOffset = getOffsetUpToBlock(startPosition)
                        val cardWidth = getOffsetUpToBlock(endPosition) - leftOffset

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Spacer(modifier = Modifier.width(leftOffset))
                            HorizontalTimelineCard(
                                schedule = schedule,
                                cardWidth = cardWidth,
                                onEditItem = { onTimelineItemClick(schedule) },
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }

        // 振り返りエリアの境界線
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            Text(text = "振り返りエリア", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), modifier = Modifier.padding(horizontal = 8.dp))
            Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        }

        // 下半分：振り返りダッシュボードエリア
        Column(
            modifier = Modifier.fillMaxWidth().weight(5f).padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "☑️", fontSize = 16.sp, modifier = Modifier.padding(end = 6.dp))
                        Text(text = "未完了 TODO", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    if (todaysIncompleteTasks.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "この日の未完了タスクはありません！✨", fontSize = 13.sp, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(todaysIncompleteTasks) { task ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().clickable { onTimelineItemClick(task) }.padding(vertical = 2.dp)
                                ) {
                                    Checkbox(checked = task.isCompleted, onCheckedChange = { isChecked -> viewModel.toggleScheduleStatus(task, isChecked) }, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = task.text, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(text = task.time, fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📊", fontSize = 16.sp, modifier = Modifier.padding(end = 6.dp))
                            Text(text = "カテゴリ別進捗", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(text = "達成度: $completedCount/$totalCount", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    val categoriesWithTasks = todaysSchedules.groupBy { if (it.category.isBlank()) "未分類" else it.category }
                    if (categoriesWithTasks.isEmpty()) {
                        Text(text = "データがありません", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            categoriesWithTasks.forEach { (categoryName, list) ->
                                val catTotal = list.size
                                val catCompleted = list.count { it.isCompleted }
                                val progressFactor = if (catTotal > 0) catCompleted.toFloat() / catTotal.toFloat() else 0f
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = categoryName, fontSize = 13.sp)
                                        Text(text = "${(progressFactor * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(progress = progressFactor, modifier = Modifier.fillMaxWidth().height(6.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HorizontalTimelineCard(
    schedule: Schedule,
    cardWidth: androidx.compose.ui.unit.Dp,
    onEditItem: (Schedule) -> Unit,
    viewModel: HomeViewModel
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val arrowRotationDegree by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "ArrowAnimation")

    val currentTime = System.currentTimeMillis()
    val taskTimeMillis = convertDateTimeToMillis(schedule.date, schedule.time)
    val isOverdue = taskTimeMillis != null && taskTimeMillis < currentTime && !schedule.isCompleted

    val isDark = isSystemInDarkTheme()
    val borderColor = if (isOverdue) Color(0xFF94403E) else md_theme_light_primary
    val backgroundColor = if (isOverdue && isDark) MaterialTheme.colorScheme.surfaceVariant
    else if (isOverdue) Color(0xFFFFEBEE)
    else MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier
            .width(cardWidth)
            .border(BorderStroke(1.5.dp, borderColor), RoundedCornerShape(8.dp))
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded }
            .padding(vertical = 6.dp, horizontal = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = schedule.isCompleted, onCheckedChange = { isChecked -> viewModel.toggleScheduleStatus(schedule, isChecked) }, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = schedule.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = androidx.compose.ui.text.TextStyle(
                        textDecoration = if (schedule.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
                    )
                )
            }

            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp).rotate(arrowRotationDegree))
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                Text(text = "📅 日　　付: ${schedule.date}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                //Text(text = "📌 予　　定: ${schedule.text}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "📝 メ　　モ: ${schedule.detail.ifEmpty { "なし" }}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "🏷️ カテゴリ: ${schedule.category.ifEmpty { "なし" }}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onEditItem(schedule) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp), modifier = Modifier.height(28.dp)) {
                        Text("編集する", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
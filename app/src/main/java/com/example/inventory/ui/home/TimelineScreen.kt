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

// タイムライン全体の画面構成
@Composable
fun TimelineScreen(
    scheduleList: List<Schedule>,
    selectedDate: String, // 💡 外部（HomeScreenなど）から現在選択中の日付を受け取る
    onDateChange: (String) -> Unit, // 💡 日付が変更されたことを外部に伝えるイベント
    onTimelineItemClick: (Schedule) -> Unit,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    // Androidのシステムコンテキストを取得（日付変更用ダイアログの表示に必須）
    val context = LocalContext.current

    // 全予定の中から「選択された日付」のものだけを抽出し保持
    val todaysSchedules = remember(scheduleList, selectedDate) {
        scheduleList.filter { it.date == selectedDate }
    }

    // 下部のTODOエリア用に「選択された日付の未完了タスク」だけを抽出
    val todaysIncompleteTasks = remember(todaysSchedules) {
        todaysSchedules.filter { !it.isCompleted }
    }

    // 進捗グラフ用に全体の達成度を計算
    val totalCount = todaysSchedules.size
    val completedCount = todaysSchedules.count { it.isCompleted }

    // 横方向のスクロール位置を管理する状態
    val scrollState = rememberScrollState()

    // タイムラインの1時間あたりの横幅の基準サイズ(1時間=100dp)
    val hourWidth = 100.dp

    // カレンダー上部に並べる00:00〜24:00までの目盛り用文字リスト
    val timeLabels = remember { (0..24).map { String.format("%02d:00", it) } }

    // 画面全体の縦並びレイアウト
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // 💡 上部エリア：日付タイトルと日付変更ボタンを横並びにするレイアウト
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 画面上部の一番大きなタイトル（選択中の日付を表示）
            Text(
                text = "$selectedDate のタイムライン",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // 💡 カレンダーアイコンのボタン（タップでシステムのカレンダーダイアログを起動）
            IconButton(
                onClick = {
                    val calendar = Calendar.getInstance()
                    try {
                        // 現在画面に出て選択されている日付文字列を解析してカレンダーの初期位置にする
                        val dateParts = selectedDate.split("/")
                        val year = dateParts[0].toInt()
                        val month = dateParts[1].toInt() - 1 // Calendarの月は0〜11の仕様
                        val day = dateParts[2].toInt()
                        calendar.set(year, month, day)
                    } catch (e: Exception) {
                        // 解析が万が一失敗した場合は、現在の標準状態（今日）のまま進行させる
                    }

                    // Android標準の日付選択ダイアログを表示
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            // 選択完了時、"yyyy/MM/dd" 形式に整形して親コンポーザブルへ通知
                            val newDate = String.format("%04d/%02d/%02d", year, month + 1, dayOfMonth)
                            onDateChange(newDate)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "日付を変更",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 上半分：内部が横スクロールするタイムラインコンテナ領域(重みを4にしてバランス調整)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(4f)
                .horizontalScroll(scrollState)
        ) {
            // 背景を左から右へ貫く、時間軸のベースとなる薄い横線
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .padding(top = 30.dp) // 時間文字のすぐ下に重なるように位置を微調整
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            )

            // 目盛りレイアウトと予定リストを縦に並べるコンテナ
            Column(modifier = Modifier.fillMaxHeight()) {

                // 1.時間の目盛り行（数字と丸ドット）
                Row(modifier = Modifier.height(60.dp)) {
                    timeLabels.forEach { label ->
                        // 各時間ごとに決められた横幅(100dp)の枠を確保
                        Box(
                            modifier = Modifier.width(hourWidth),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // 目盛りの時間文字(例:06:00)
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                // 横線の上に重なる、時間基準点の小さな丸ポチ
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            }
                        }
                    }
                }

                // 2.予定カードを被らないように上から縦に並べるリスト領域
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(todaysSchedules) { schedule ->
                        // 開始時刻を数値の位置に変換(例:"06:30"➔6.5)
                        val startParts = schedule.time.split(":")
                        val startHour = startParts.getOrNull(0)?.toFloatOrNull() ?: 0f
                        val startMinute = startParts.getOrNull(1)?.toFloatOrNull() ?: 0f
                        val startPosition = startHour + (startMinute / 60f)

                        // 終了時刻を数値の位置に変換(例:"08:45"➔8.75)
                        val endParts = schedule.endTime.split(":")
                        val endHour = endParts.getOrNull(0)?.toFloatOrNull() ?: (startPosition + 1f)
                        val endMinute = endParts.getOrNull(1)?.toFloatOrNull() ?: 0f
                        val endPosition = endHour + (endMinute / 60f)

                        // 予定が何時間分あるか、引き算で長さ（倍率）を割り出す
                        val duration = if (endPosition > startPosition) endPosition - startPosition else 1.0f

                        // ドットの中心にぴったり合わせるため、目盛り幅の半分(50dp)を右へずらす補正を計算に組み込む
                        val leftOffset = (hourWidth * startPosition) + (hourWidth / 2f)
                        // カード自体の横幅を計算(例:2時間分なら100dp×2=200dp)
                        val cardWidth = hourWidth * duration

                        // カードを適切な横位置に配置するための1行レイアウト
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // カードの左側に、ドットの中心位置までの「目に見えない透明な隙間」を作る
                            Spacer(modifier = Modifier.width(leftOffset))

                            // 横長に引き伸ばされた予定カード本体を配置
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

        // 中央：振り返りエリアの区切り文字
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            Text(
                text = "振り返りエリア",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        }

        // 下半分：新設した「振り返りダッシュボードエリア」(重みを5にして広めに確保)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(5f)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 3.未完了のタスク
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
                        // 未完了のリストだけをスクロールリストで下に並べる
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(todaysIncompleteTasks) { task ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().clickable { onTimelineItemClick(task) }.padding(vertical = 2.dp)
                                ) {
                                    Checkbox(
                                        checked = task.isCompleted,
                                        onCheckedChange = { isChecked -> viewModel.toggleScheduleStatus(task, isChecked) },
                                        modifier = Modifier.size(28.dp)
                                    )
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

            // 4.「カテゴリ別進捗」カードセクション
            Card(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📊", fontSize = 16.sp, modifier = Modifier.padding(end = 6.dp))
                            Text(text = "カテゴリ別進捗", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        // 全体の達成状況数をテキスト表示
                        Text(text = "達成度: $completedCount/$totalCount", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // 今日のタスクをカテゴリ（仕事、プライベートなど）ごとに分類して進捗バーを生成
                    val categoriesWithTasks = todaysSchedules.groupBy { if (it.category.isBlank()) "未分類" else it.category }

                    if (categoriesWithTasks.isEmpty()) {
                        Text(text = "データがありません", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            categoriesWithTasks.forEach { (categoryName, list) ->
                                val catTotal = list.size
                                val catCompleted = list.count { it.isCompleted }
                                val progressFactor = if (catTotal > 0) catCompleted.toFloat() / catTotal.toFloat() else 0f
                                val percentage = (progressFactor * 100).toInt()

                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = categoryName, fontSize = 13.sp)
                                        Text(text = "$percentage%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // 進捗を視覚化する進捗バー
                                    LinearProgressIndicator(
                                        progress = progressFactor,
                                        modifier = Modifier.fillMaxWidth().height(6.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// タイムライン上に配置される横長予定カードのデザイン構成
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

    // 24時間表記を「午前/午後 XX:XX」の形式に変換するロジック
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
        return "$amPmSystem${String.format("%02d", displayHour)}:$minute"
    }

    val displayStartTime = formatTo12Hour(schedule.time)
    val displayEndTime = formatTo12Hour(schedule.endTime)

    Column(
        modifier = Modifier
            .width(cardWidth)
            .border(BorderStroke(1.5.dp, borderColor), RoundedCornerShape(8.dp))
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded }
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(
                checked = schedule.isCompleted,
                onCheckedChange = { isChecked -> viewModel.toggleScheduleStatus(schedule, isChecked) },
                modifier = Modifier.size(24.dp).padding(end = 4.dp)
            )
            Text(
                text = schedule.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                style = androidx.compose.ui.text.TextStyle(
                    textDecoration = if (schedule.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
                )
            )
            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp).rotate(arrowRotationDegree))
        }

        // 午前・午後を含めた時間帯をカード内に表示
        Text(
            text = "$displayStartTime 〜 $displayEndTime",
            fontSize = 10.sp,
            color = if (isDark) md_theme_dark_time else md_theme_light_time,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                Text(text = "📅 日　　付: ${schedule.date}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "⏰ 時　　間: $displayStartTime 〜 $displayEndTime", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
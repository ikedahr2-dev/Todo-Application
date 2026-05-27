package com.example.inventory.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventory.R

import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleInputDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, Int, Int) -> Unit,
    onDelete: (() -> Unit)? = null,
    onSelectDate: () -> Unit,
    onSelectTime: () -> Unit,
    onSelectEndTime: () -> Unit,
    onSelectCategory: (String) -> Unit,
    selectedDate: String,
    selectedTime: String,
    selectedEndTime: String,
    selectedCategory: String,
    categories: List<String>,
    initialText: String = "",
    initialDetail: String = "",
    initialReminderMinutes: Int = 5
) {

    var text by remember(initialText) { mutableStateOf(initialText) }
    var detail by remember(initialDetail) { mutableStateOf(initialDetail) }

    val safeInitialMinutes = if (initialReminderMinutes == 9999 || initialReminderMinutes < -1) 5 else initialReminderMinutes
    var selectedReminderMinutes by remember(safeInitialMinutes) { mutableStateOf(safeInitialMinutes) }

    var showError by remember { mutableStateOf(false) }

    // 💡 既存機能：削除せずにしっかりと残してあります
    fun convertTo12HourLabel(timeStr: String): String {
        if (timeStr.isBlank()) return ""
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

    val isTimeInvalid = selectedTime.isNotBlank() &&
            selectedEndTime.isNotBlank() &&
            selectedEndTime <= selectedTime

    val isFormFilled = text.isNotBlank() &&
            selectedDate.isNotBlank() &&
            selectedTime.isNotBlank() &&
            selectedEndTime.isNotBlank()

    val isFormValid = isFormFilled && !isTimeInvalid

    AlertDialog(
        onDismissRequest = { },
        confirmButton = {
            Button(
                onClick = {
                    if (isFormValid) {
                        val finalMinutes = if (selectedReminderMinutes == 9999) 5 else selectedReminderMinutes
                        onSave(text, selectedDate, selectedTime, selectedEndTime, selectedCategory, detail, finalMinutes, 0)
                        onDismiss()
                    } else {
                        showError = true
                    }
                }
            ) { Text(stringResource(R.string.enter)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
        text = {
            Column {
                var categoryExpanded by remember { mutableStateOf(false) }
                var reminderExpanded by remember { mutableStateOf(false) }

                // スケジュール名入力欄
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        if (isFormValid) showError = false
                    },
                    label = {
                        Text(
                            text = androidx.compose.ui.text.buildAnnotatedString {
                                withStyle(style = androidx.compose.ui.text.SpanStyle(color = androidx.compose.ui.graphics.Color.Red, fontWeight = FontWeight.Bold)) {
                                    append("*")
                                }
                                append(stringResource(R.string.stay_schedule))
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // メモ入力欄
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text("メモ") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ----- 日付選択 -----
                Text(
                    text = androidx.compose.ui.text.buildAnnotatedString {
                        withStyle(style = androidx.compose.ui.text.SpanStyle(color = androidx.compose.ui.graphics.Color.Red, fontWeight = FontWeight.Bold)) {
                            append("*")
                        }
                        append("予定日")
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Button(onClick = onSelectDate, modifier = Modifier.fillMaxWidth()) {
                    Text(if (selectedDate.isEmpty()) stringResource(R.string.date_enter) else selectedDate)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ----- 時間設定（開始・終了） -----
                Text(
                    text = androidx.compose.ui.text.buildAnnotatedString {
                        withStyle(style = androidx.compose.ui.text.SpanStyle(color = androidx.compose.ui.graphics.Color.Red, fontWeight = FontWeight.Bold)) {
                            append("*")
                        }
                        append("時間設定")
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSelectTime,
                        modifier = Modifier.weight(1f)
                    ) {
                        // 💡 変更：12時間表記に変換せず、24時間表記の値をそのまま表示するようにしました
                        Text(if (selectedTime.isEmpty()) stringResource(R.string.StartTime_enter) else selectedTime)
                    }
                    Button(
                        onClick = onSelectEndTime,
                        modifier = Modifier.weight(1f)
                    ) {
                        // 💡 変更：12時間表記に変換せず、24時間表記の値をそのまま表示するようにしました
                        Text(if (selectedEndTime.isEmpty()) stringResource(R.string.EndTime_enter) else selectedEndTime)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ----- 開始時間の通知設定 -----
                Text(text = "開始時間の通知設定", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

                val reminderLabel = when (selectedReminderMinutes) {
                    -1 -> "通知なし"
                    0 -> "時間ピッタリ"
                    9999 -> "5分前"
                    else -> "${selectedReminderMinutes}分前"
                }

                val reminderOptions = listOf(
                    Pair("通知なし", -1),
                    Pair("時間ピッタリ", 0),
                    Pair("5分前", 5),
                    Pair("10分前", 10),
                    Pair("15分前", 15),
                    Pair("30分前", 30),
                    Pair("1時間前", 60)
                )

                ExposedDropdownMenuBox(
                    expanded = reminderExpanded,
                    onExpandedChange = { reminderExpanded = !reminderExpanded }
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        readOnly = true,
                        value = reminderLabel,
                        onValueChange = {},
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reminderExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = reminderExpanded,
                        onDismissRequest = { reminderExpanded = false }
                    ) {
                        reminderOptions.forEach { (label, minutes) ->
                            DropdownMenuItem(
                                text = { Text(text = label) },
                                onClick = {
                                    selectedReminderMinutes = minutes
                                    reminderExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ----- カテゴリプルダウン -----
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        readOnly = true,
                        value = selectedCategory,
                        onValueChange = {},
                        label = { Text("カテゴリー") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.filter { it != "すべて" }.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(text = category) },
                                onClick = {
                                    onSelectCategory(category)
                                    categoryExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                if (showError || isTimeInvalid) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isTimeInvalid) "終了時間は開始時間より後の時刻にしてください" else "すべて入力・選択してください",
                        color = androidx.compose.ui.graphics.Color.Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    )
}
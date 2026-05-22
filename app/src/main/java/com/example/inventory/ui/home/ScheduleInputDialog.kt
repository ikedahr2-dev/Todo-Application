package com.example.inventory.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight

// スケジュール入力・編集を行うポップアップダイアログ
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleInputDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit,
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
    initialDetail: String = ""
) {

    // タイトルとメモの入力を管理する状態（初期値が渡されたら自動更新）
    var text by remember(initialText) { mutableStateOf(initialText) }
    var detail by remember(initialDetail) { mutableStateOf(initialDetail) }

    // エラーメッセージの表示状態を管理するフラグ
    var showError by remember { mutableStateOf(false) }

    // 24時間表記を「午前/午後 XX:XX」の形式に変換するダイアログ内ヘルパー関数
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

    // 終了時刻が開始時刻よりも前（または同じ）になっていないか判定するフラグ
    val isTimeInvalid = selectedTime.isNotBlank() &&
            selectedEndTime.isNotBlank() &&
            selectedEndTime <= selectedTime

    // すべての必須項目（タイトル、日付、開始、終了）が埋まっているかチェック
    val isFormFilled = text.isNotBlank() &&
            selectedDate.isNotBlank() &&
            selectedTime.isNotBlank() &&
            selectedEndTime.isNotBlank()

    // フォーム全体が有効か（正しく入力され、かつ時間の逆転がないか）
    val isFormValid = isFormFilled && !isTimeInvalid

    AlertDialog(
        onDismissRequest = { },
        // 右下の「確定（保存）」ボタン
        confirmButton = {
            Button(
                onClick = {
                    if (isFormValid) {
                        // 入力が正しい場合のみ、データを引き渡してダイアログを閉じる
                        onSave(text, selectedDate, selectedTime, selectedEndTime, selectedCategory, detail)
                        onDismiss()
                    } else {
                        // 未入力や時間逆転がある場合はエラーを表示
                        showError = true
                    }
                }
            ) { Text(stringResource(R.string.enter)) }
        },
        // 左下の「削除」および「キャンセル」ボタン
        dismissButton = {
            Row {
                // 既存の予定を編集しているとき（onDeleteがヌルでないとき）だけ削除ボタンを出現させる
                /*if (onDelete != null) {
                    TextButton(
                        onClick = {
                            onDelete()
                            onDismiss()
                        }
                    ) { Text(stringResource(R.string.delete)) }
                }*/
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
        // ダイアログの中身（入力フォーム一覧）
        text = {
            Column {
                // カテゴリ選択用のメニュー開閉フラグ
                var expanded by remember { mutableStateOf(false) }

                // スケジュール名入力欄
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        if (isFormValid) showError = false
                    },
                    label = { Text(stringResource(R.string.stay_schedule)) }
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
                Text(text = "*予定日", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Button(onClick = onSelectDate, modifier = Modifier.fillMaxWidth()) {
                    Text(if (selectedDate.isEmpty()) stringResource(R.string.date_enter) else selectedDate)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ----- 時間設定（開始・終了） -----
                Text(text = "*時間設定", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 開始時間設定ボタン（午前/午後表記を適用）
                    Button(
                        onClick = onSelectTime,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (selectedTime.isEmpty())
                                stringResource(R.string.StartTime_enter)
                            else
                                convertTo12HourLabel(selectedTime)
                        )
                    }
                    // 終了時間設定ボタン（午前/午後表記を適用）
                    Button(
                        onClick = onSelectEndTime,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (selectedEndTime.isEmpty())
                                stringResource(R.string.EndTime_enter)
                            else
                                convertTo12HourLabel(selectedEndTime)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ----- カテゴリプルダウン -----
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor(),
                        readOnly = true,
                        value = selectedCategory,
                        onValueChange = {},
                        label = { Text("カテゴリー") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )

                    // 選択項目の中身一覧
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        // フィルター用の「すべて」を除外したリストをメニューに表示
                        categories.filter { it != "すべて" }.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(text = category) },
                                onClick = {
                                    onSelectCategory(category)
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                // エラーメッセージの出し分け表示
                if (showError || isTimeInvalid) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isTimeInvalid) {
                            "終了時間は開始時間より後の時刻にしてください"
                        } else {
                            "すべて入力・選択してください"
                        },
                        color = androidx.compose.ui.graphics.Color.Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    )
}
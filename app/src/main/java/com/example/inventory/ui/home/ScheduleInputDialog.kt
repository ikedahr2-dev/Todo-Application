package com.example.inventory.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.example.inventory.R

@Composable
fun ScheduleInputDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    onDelete: (() -> Unit)? = null,
    onSelectDate: () -> Unit,
    onSelectTime: () -> Unit,
    selectedDate: String,
    selectedTime: String,
    //dialogProperties: Any
) {

    var text by remember {
        mutableStateOf("")
    }

    val isFormValid = text.isNotBlank() && selectedDate.isNotBlank() && selectedTime.isNotBlank() //入力チェック

    AlertDialog(
        onDismissRequest = { },
        confirmButton = {
            Button(
                onClick = {
                    onSave(text, selectedDate, selectedTime)
                    onDismiss()
                },
                enabled = isFormValid //有効/無効を制御
            ) {
                Text(stringResource(R.string.enter))
            }
        },
        dismissButton = {
            Row {
                //削除ボタン（編集時のみ）
                if (onDelete != null) {
                    TextButton(
                        onClick = {
                            onDelete()
                            onDismiss()
                        }
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                }

                TextButton(
                    onClick = onDismiss
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
        text = {
            Column {

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.stay_schedule)) }
                )

                Button(onClick = onSelectDate) {
                    Text(
                        if (selectedDate.isEmpty()) stringResource(R.string.date_enter)
                        else selectedDate
                    )
                }

                Button(onClick = onSelectTime) {
                    Text(
                        if (selectedTime.isEmpty()) stringResource(R.string.time_enter)
                        else selectedTime
                    )
                }

                if (!isFormValid) { //もし未入力だった場合のエラーメッセージ
                    Text(
                        text = "日付・時間・予定をすべて入力してください",
                        color = androidx.compose.ui.graphics.Color.Red,
                        fontSize = 13.sp
                    )
                }
            }
        }
    )
}

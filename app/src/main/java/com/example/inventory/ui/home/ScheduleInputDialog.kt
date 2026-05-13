package com.example.inventory.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.example.inventory.R

@Composable
fun ScheduleInputDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    onSelectDate: () -> Unit,
    onSelectTime: () -> Unit,
    selectedDate: String,
    selectedTime: String
) {

    var text by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {

                    //予定が入力されてないなら閉じる
                    if (text.isBlank()) {
                        onDismiss()
                        return@Button
                    }


                    onSave(text, selectedDate, selectedTime)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.enter))
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
                    Text(if (selectedDate.isEmpty()) stringResource(R.string.date_enter) else selectedDate)
                }

                Button(onClick = onSelectTime) {
                    Text(if (selectedTime.isEmpty()) stringResource(R.string.time_enter) else selectedTime)
                }
            }
        }
    )
}
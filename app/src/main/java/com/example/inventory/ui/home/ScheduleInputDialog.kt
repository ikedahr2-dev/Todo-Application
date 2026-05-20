package com.example.inventory.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth // Modifier.fillMaxWidth() に必須のインポート
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier // Modifier自体に必須のインポート
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.example.inventory.R

// カテゴリPullDown
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleInputDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit,
    onDelete: (() -> Unit)? = null,
    onSelectDate: () -> Unit,
    onSelectTime: () -> Unit,
    onSelectCategory: (String) -> Unit,
    selectedDate: String,
    selectedTime: String,
    selectedCategory: String,
    categories: List<String>,
    initialText: String = "",
    initialDetail: String = ""
) {

    var text by remember(initialText) {
        mutableStateOf(initialText)
    }

    var detail by remember(initialDetail){
        mutableStateOf(initialDetail)
    }

    // エラーフラグ
    var showError by remember { mutableStateOf(false) }

    // 入力チェック
    val isFormValid = text.isNotBlank() && selectedDate.isNotBlank() && selectedTime.isNotBlank()

// ---------- バック ---------- //

    AlertDialog(
        onDismissRequest = { },
        confirmButton = {
            Button(
                onClick = {
                    // 保存出来たら閉じる
                    if (isFormValid) {
                        onSave(text, selectedDate, selectedTime, selectedCategory, detail)
                        onDismiss()
                        // 未入力
                    } else {
                        showError = true
                    }
                },
                enabled = true
            ) {
                Text(stringResource(R.string.enter))
            }
        },
        dismissButton = {
            Row {
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

// ---------- フロント ---------- //

        text = {
            Column {

                var expanded by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        if (isFormValid) showError = false
                    },
                    label = { Text(stringResource(R.string.stay_schedule)) }
                )

                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text("詳細") },
                    modifier = Modifier.fillMaxWidth() // 💡これでエラーが消えます
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

                // ----- カテゴリプルダウン ----- //

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

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
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

                // showErrorがtrueのときエラー表示
                if (showError) {
                    Text(
                        text = "すべて入力・選択してください",
                        color = androidx.compose.ui.graphics.Color.Red,
                        fontSize = 13.sp
                    )
                }
            }
        }
    )
}
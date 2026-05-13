package com.example.inventory.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.inventory.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onDateSelected: (String) -> Unit
) {
    val state = rememberDatePickerState()

    Column(modifier = Modifier.fillMaxSize()) {

        //カレンダー本体
        DatePicker(state = state)

        //日付確定ボタン
        Button(
            onClick = {

                //ここで日付変換
                val millis = state.selectedDateMillis
                val date = millis?.let {
                    SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                        .format(Date(it))
                } ?: ""

                //HomeScreenへ渡す
                if (date.isNotEmpty()) {
                    onDateSelected(date)
                }
            }
        ) {
            Text(stringResource(R.string.all_stay_schedule))
        }
    }
}
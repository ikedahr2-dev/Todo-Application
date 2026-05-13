/*
 * Copyright (C) 2023 The Android Open Source Project
 * ... (ライセンス表記は省略可)
 */

package com.example.inventory.ui.home

import androidx.lifecycle.ViewModel
import com.example.inventory.data.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ホーム画面のUI状態を保持するデータクラス
 */
data class HomeUiState(
    val itemList: List<Item> = listOf(),
    val showDatePicker: Boolean = false, // カレンダー表示用のフラグ
    val showInputBox: Boolean = false,   //テキストボックス表示用のフラグ
    val savedItems: List<ScheduleItem> = emptyList()
)

data class ScheduleItem(
    val text: String,
    val date: String,
    val time: String
)
/**
 * Roomデータベースからすべてのアイテムを取得するためのViewModel
 */
class HomeViewModel : ViewModel() {

    // UIの状態を管理するMutableStateFlow
    private val _uiState = MutableStateFlow(HomeUiState())

    // HomeScreenから参照される読み取り専用のStateFlow
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    //カレンダー表示
    fun onCalenderClick() {
        _uiState.update { it.copy(showDatePicker = true) }
    }

    //テキストボックス表示
    fun onAddClick() {
        _uiState.update { it.copy(showInputBox = true) }
    }


    //カレンダーを閉じる
    fun onDismissDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }

    //テキストボックスを閉じる
    fun onDismissInputBox() {
        _uiState.update { it.copy(showInputBox = false) }
    }

    fun addText(
        text: String,
        date: String,
        time: String
    ) {
        _uiState.update {
            it.copy(
                savedItems = it.savedItems + ScheduleItem(
                    text = text,
                    date = date,
                    time = time
                )
            )
        }
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}